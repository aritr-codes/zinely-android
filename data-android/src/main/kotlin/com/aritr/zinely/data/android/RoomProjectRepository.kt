package com.aritr.zinely.data.android

import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.DocumentRepository
import com.aritr.zinely.core.data.repository.ProjectRepository
import com.aritr.zinely.core.data.repository.ProjectSummary
import com.aritr.zinely.core.data.storage.AtomicFileStore
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.data.android.room.ProjectDao
import com.aritr.zinely.data.android.room.ProjectEntity
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import kotlin.streams.toList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * The Room-backed [ProjectRepository] (ADR-042). The **files are the source of truth and the
 * transaction commit point**: `document.json` for content/format/paperSize/schemaVersion (ADR-003)
 * and the `meta.json` sidecar for title/createdAt; the Room `projects` table is a **rebuildable
 * index** derived from them. Every mutation commits its file state first and then re-derives the
 * affected row through the single [syncRowFromDisk] path — the same derivation the [reconcile] scan
 * uses to adopt pre-Room projects (including the S4 `"default"` seed) and to drop rows whose files
 * are gone. A returned failure cleans up partial files (no adoptable orphans from failed calls);
 * crash windows heal at the next reconcile. An index write that fails *after* a committed file
 * change surfaces [DataError.Io] and flags a re-reconcile, so the index converges to file truth.
 *
 * Display recency: [observeProjects] emits `updatedAtEpochMs = max(row, document mtime)` — an
 * autosave is a durable file write, so its mtime is the recency signal with zero coupling to the
 * autosave stack (valid under the current nav assumption that the shelf and an editor are never
 * simultaneously active; ADR-042).
 *
 * Invariants recorded in ADR-042: project mutations MUST NOT run against a project with an open
 * editor session (enforced by the S6.3 shelf layer via the autosave binder's single-writer
 * registry); `"default"` stays the ADR-030 §4 bootstrap-reserved id until S6.5 moves the start
 * destination — the app shell re-seeds it on next boot if deleted at this level.
 */
internal class RoomProjectRepository(
    rootDir: Path,
    private val dao: ProjectDao,
    private val documents: DocumentRepository,
    private val store: AtomicFileStore,
    private val io: CoroutineDispatcher,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ProjectRepository {

    private val paths = ProjectPaths(rootDir)
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    /** Cleared whenever the index may have diverged from committed file truth. */
    @Volatile
    private var reconciled = false

    override fun observeProjects(): Flow<List<ProjectSummary>> =
        dao.observeAll()
            .onStart { mutex.withLock { ensureReconciledLocked() } }
            .map { rows -> rows.mapNotNull(::toSummary).sortedByDescending { it.updatedAtEpochMs } }
            .flowOn(io)

    override suspend fun getProject(id: String): DataResult<ProjectSummary> = withContext(io) {
        mutex.withLock { ensureReconciledLocked() }
        val row = try {
            dao.findById(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return@withContext failure(DataError.Io("failed to query project '$id'", e))
        }
        row?.let(::toSummary)?.let { DataResult.Success(it) } ?: failure(DataError.NotFound(id))
    }

    override suspend fun createProject(
        title: String,
        format: ZineFormat,
        paperSize: PaperSize,
    ): DataResult<ProjectSummary> = withContext(io) {
        mutex.withLock {
            ensureReconciledLocked()
            val id = newId()
            when (val saved = documents.save(id, blankDocument(format, paperSize))) {
                is DataResult.Failure -> return@withLock DataResult.Failure(saved.error)
                is DataResult.Success -> Unit
            }
            val now = clock()
            try {
                writeMeta(id, ProjectMeta(title = title, createdAtEpochMs = now))
            } catch (e: IOException) {
                // A returned failure must leave no adoptable orphan (ADR-042 / Codex RF2).
                cleanupProjectFiles(id)
                return@withLock failure(DataError.Io("failed to write project metadata for '$id'", e))
            }
            syncRowFromDisk(id, updatedAtEpochMs = now)
        }
    }

    override suspend fun renameProject(id: String, title: String): DataResult<Unit> = withContext(io) {
        mutex.withLock {
            ensureReconciledLocked()
            val docFile = paths.documentFile(id)
                ?: return@withLock failure(DataError.NotFound(id))
            if (!Files.isRegularFile(docFile)) return@withLock failure(DataError.NotFound(id))
            val createdAt = readMetaOrNull(id)?.createdAtEpochMs ?: fileMtimeOrNull(docFile) ?: clock()
            try {
                // The atomic meta rewrite is the commit; the row below is derived.
                writeMeta(id, ProjectMeta(title = title, createdAtEpochMs = createdAt))
            } catch (e: IOException) {
                return@withLock failure(DataError.Io("failed to write project metadata for '$id'", e))
            }
            when (val synced = syncRowFromDisk(id, updatedAtEpochMs = clock())) {
                is DataResult.Failure -> DataResult.Failure(synced.error)
                is DataResult.Success -> DataResult.Success(Unit)
            }
        }
    }

    override suspend fun duplicateProject(id: String): DataResult<ProjectSummary> = withContext(io) {
        mutex.withLock {
            ensureReconciledLocked()
            val source = when (val loaded = documents.load(id)) {
                is DataResult.Failure -> return@withLock DataResult.Failure(loaded.error)
                is DataResult.Success -> loaded.value
            }
            val sourceTitle = readMetaOrNull(id)?.title ?: DEFAULT_TITLE
            val copyId = newId()
            // Same document ⇒ same referenced content hashes: the duplicate is a new live root over
            // the shared blobs, never a byte copy (ADR-022).
            when (val saved = documents.save(copyId, source)) {
                is DataResult.Failure -> return@withLock DataResult.Failure(saved.error)
                is DataResult.Success -> Unit
            }
            val now = clock()
            try {
                writeMeta(copyId, ProjectMeta(title = "$sourceTitle copy", createdAtEpochMs = now))
            } catch (e: IOException) {
                cleanupProjectFiles(copyId)
                return@withLock failure(DataError.Io("failed to write project metadata for '$copyId'", e))
            }
            syncRowFromDisk(copyId, updatedAtEpochMs = now)
        }
    }

    override suspend fun deleteProject(id: String): DataResult<Unit> = withContext(io) {
        mutex.withLock {
            ensureReconciledLocked()
            // An unsafe id can never name a project; deleting it is a no-op success (idempotent).
            val dir = paths.projectDir(id) ?: return@withLock DataResult.Success(Unit)
            try {
                // document.json first — its disappearance is the commit point that releases the
                // project's GC roots (ADR-022); the rest of the dir is cleanup.
                Files.deleteIfExists(dir.resolve(ProjectPaths.DOCUMENT_FILE))
                deleteRecursively(dir)
            } catch (e: IOException) {
                return@withLock failure(DataError.Io("failed to delete project '$id'", e))
            }
            try {
                dao.deleteById(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                reconciled = false // files committed; the index converges at the next reconcile
                return@withLock failure(DataError.Io("failed to unindex project '$id'", e))
            }
            DataResult.Success(Unit)
        }
    }

    // ---- the single files→row derivation path ---------------------------------------------------

    /**
     * Derive the row for [id] from its files — used by every mutation *and* the reconcile scan, so
     * there is exactly one derivation. [updatedAtEpochMs] is the metadata-op timestamp (mutations
     * pass the op clock; adoption passes the document mtime). An unreadable document is a Failure
     * (adoption skips it; bytes untouched — ADR-042 known limitation).
     */
    private suspend fun syncRowFromDisk(id: String, updatedAtEpochMs: Long): DataResult<ProjectSummary> {
        val docFile = paths.documentFile(id) ?: return failure(DataError.NotFound(id))
        val document = when (val loaded = documents.load(id)) {
            is DataResult.Failure -> return DataResult.Failure(loaded.error)
            is DataResult.Success -> loaded.value
        }
        val meta = readMetaOrBackfill(id, docFile)
        val entity = ProjectEntity(
            id = id,
            title = meta.title,
            format = document.format.name,
            paperSize = document.paperSize.name,
            createdAtEpochMs = meta.createdAtEpochMs,
            updatedAtEpochMs = updatedAtEpochMs,
            documentSchemaVersion = document.schemaVersion,
        )
        try {
            dao.upsert(entity)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reconciled = false // file truth is committed; re-derive the index on next use
            return failure(DataError.Io("failed to index project '$id'", e))
        }
        return DataResult.Success(
            ProjectSummary(
                id = id,
                title = meta.title,
                format = document.format,
                paperSize = document.paperSize,
                createdAtEpochMs = meta.createdAtEpochMs,
                updatedAtEpochMs = max(updatedAtEpochMs, fileMtimeOrNull(docFile) ?: 0L),
                documentSchemaVersion = document.schemaVersion,
            ),
        )
    }

    /**
     * Reconcile the index against the on-disk project set (ADR-042 seeding: adopts pre-Room
     * projects — the S4 `"default"` seed becomes an ordinary row — and drops rows without files).
     * Idempotent and re-runnable; repair, not a gate: a failed scan leaves [reconciled] false so the
     * next repository use retries, while the current call proceeds against the existing index
     * (files stay the truth regardless).
     */
    private suspend fun ensureReconciledLocked() {
        if (reconciled) return
        try {
            val onDisk = listProjectIds()
            val indexed = dao.ids().toSet()
            for (id in onDisk) {
                if (id !in indexed) {
                    val docFile = paths.documentFile(id) ?: continue
                    // Unreadable documents (Corrupt/SchemaTooNew) return Failure here and are
                    // skipped — invisible to the shelf, bytes left for a future repair path.
                    syncRowFromDisk(id, updatedAtEpochMs = fileMtimeOrNull(docFile) ?: clock())
                }
            }
            val onDiskSet = onDisk.toSet()
            for (id in indexed) {
                if (id !in onDiskSet) dao.deleteById(id)
            }
            reconciled = true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Deliberate degrade-not-fail: the observed list falls back to the current index and
            // the scan retries on the next repository use (reconciled stays false).
        }
    }

    /** Ids of `projects/` children that pass the whitelist and contain a document.json. */
    private fun listProjectIds(): List<String> {
        val root = paths.projectsRoot
        if (!Files.isDirectory(root)) return emptyList()
        Files.list(root).use { stream ->
            return stream
                .filter { Files.isDirectory(it) }
                .map { it.fileName.toString() }
                .filter { ProjectPaths.PROJECT_ID.matches(it) }
                .filter { id -> paths.documentFile(id)?.let(Files::isRegularFile) == true }
                .toList()
        }
    }

    // ---- meta.json sidecar -----------------------------------------------------------------------

    private fun readMetaOrNull(id: String): ProjectMeta? {
        val metaFile = paths.metaFile(id) ?: return null
        val bytes = try {
            // The decode predicate makes a corrupt primary fall back to its .bak (AtomicFileStore).
            store.read(metaFile) { decodeMeta(it); true }
        } catch (e: IOException) {
            null
        } ?: return null
        return try {
            decodeMeta(bytes)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Read the sidecar; a **missing** one is backfilled with fallbacks (title, createdAt = document
     * mtime — not scan time, so adoption never rewrites history). A **present-but-unreadable** one
     * is never overwritten: the row gets the fallback title while the bytes stay for repair.
     */
    private fun readMetaOrBackfill(id: String, docFile: Path): ProjectMeta {
        readMetaOrNull(id)?.let { return it }
        val fallback = ProjectMeta(
            title = DEFAULT_TITLE,
            createdAtEpochMs = fileMtimeOrNull(docFile) ?: clock(),
        )
        val metaFile = paths.metaFile(id) ?: return fallback
        if (!Files.exists(metaFile)) {
            try {
                writeMeta(id, fallback)
            } catch (_: IOException) {
                // The row is still built from the fallback; the backfill retries on a later scan.
            }
        }
        return fallback
    }

    private fun writeMeta(id: String, meta: ProjectMeta) {
        val metaFile = paths.metaFile(id) ?: throw IOException("unsafe project id '$id'")
        store.write(metaFile, json.encodeToString(ProjectMeta.serializer(), meta).encodeToByteArray())
    }

    private fun decodeMeta(bytes: ByteArray): ProjectMeta =
        json.decodeFromString(ProjectMeta.serializer(), bytes.decodeToString())

    // ---- helpers ---------------------------------------------------------------------------------

    private fun toSummary(entity: ProjectEntity): ProjectSummary? {
        val format = ZineFormat.entries.firstOrNull { it.name == entity.format } ?: return null
        val paperSize = PaperSize.entries.firstOrNull { it.name == entity.paperSize } ?: return null
        val docMtime = paths.documentFile(entity.id)?.let(::fileMtimeOrNull) ?: 0L
        return ProjectSummary(
            id = entity.id,
            title = entity.title,
            format = format,
            paperSize = paperSize,
            createdAtEpochMs = entity.createdAtEpochMs,
            updatedAtEpochMs = max(entity.updatedAtEpochMs, docMtime),
            documentSchemaVersion = entity.documentSchemaVersion,
        )
    }

    private fun blankDocument(format: ZineFormat, paperSize: PaperSize): ZineDocument = ZineDocument(
        format = format,
        paperSize = paperSize,
        // Every page INTERIOR: role is unconstrained for SINGLE_SHEET_8 (matches EditorBootstrap's seed).
        pages = (0 until format.pageCount).map { Page(index = it, role = PageRole.INTERIOR) },
    )

    /** Best-effort removal of a failed create/duplicate's partial files (never throws). */
    private fun cleanupProjectFiles(id: String) {
        val dir = paths.projectDir(id) ?: return
        try {
            deleteRecursively(dir)
        } catch (_: IOException) {
            // Cleanup is best-effort; leftovers are adopted or ignored by the next reconcile.
        }
    }

    private fun deleteRecursively(dir: Path) {
        if (!Files.exists(dir)) return
        Files.walk(dir).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    private fun fileMtimeOrNull(path: Path): Long? = try {
        Files.getLastModifiedTime(path).toMillis()
    } catch (_: IOException) {
        null
    }

    private fun failure(error: DataError): DataResult.Failure = DataResult.Failure(error)

    private companion object {
        /** Fallback title for adopted projects with no readable sidecar. */
        const val DEFAULT_TITLE = "My zine"
    }
}
