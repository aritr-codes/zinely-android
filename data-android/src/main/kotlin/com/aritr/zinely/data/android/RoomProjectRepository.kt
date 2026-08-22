package com.aritr.zinely.data.android

import com.aritr.zinely.core.data.asset.AssetEntry
import com.aritr.zinely.core.data.asset.CURRENT_LIBRARY_BACKUP_VERSION
import com.aritr.zinely.core.data.asset.LIBRARY_BACKUP_KIND
import com.aritr.zinely.core.data.asset.ZineBackupProjectEntry
import com.aritr.zinely.core.data.asset.ZineLibraryBackupManifest
import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.DocumentRepository
import com.aritr.zinely.core.data.repository.ProjectRepository
import com.aritr.zinely.core.data.repository.ProjectSummary
import com.aritr.zinely.core.data.storage.AtomicFileStore
import com.aritr.zinely.core.data.storage.AdditiveLibraryRestoreCommitter
import com.aritr.zinely.core.data.storage.FileSystemOps
import com.aritr.zinely.core.data.storage.NioFileSystemOps
import com.aritr.zinely.core.data.storage.PreparedLibraryRestore
import com.aritr.zinely.core.data.storage.PreparedRestoreAsset
import com.aritr.zinely.core.data.storage.PreparedRestoreProject
import com.aritr.zinely.core.data.storage.RestoreProjectIdAllocator
import com.aritr.zinely.core.data.storage.StagedZineLibraryBackup
import com.aritr.zinely.core.data.storage.ZineBackupStagingException
import com.aritr.zinely.core.data.storage.ZineLibraryBackupStager
import com.aritr.zinely.core.data.storage.ZineLibraryBackupWriter
import com.aritr.zinely.core.data.storage.ZineBackupWritingException
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineCoverRecipe
import com.aritr.zinely.core.model.ZineCoverStamp
import com.aritr.zinely.core.model.ZineCoverSurface
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.core.model.newZineCoverRecipe
import com.aritr.zinely.data.android.room.ProjectDao
import com.aritr.zinely.data.android.room.ProjectEntity
import java.io.IOException
import java.io.BufferedInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import kotlin.random.Random
import kotlin.streams.toList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
 * editor session — enforced HERE (ADR-044 §1, stronger than the shelf-layer enforcement ADR-042
 * assigned): every id-targeted mutation awaits [sessionGate] before taking the mutex and refuses
 * with [DataError.Busy] if a session is still live at the gate's bound. `"default"` stays the
 * ADR-030 §4 bootstrap-reserved id until S6.5 moves the start destination — the app shell re-seeds
 * it on next boot if deleted at this level.
 */
internal class RoomProjectRepository(
    rootDir: Path,
    private val dao: ProjectDao,
    private val documents: DocumentRepository,
    private val store: AtomicFileStore,
    private val sessionGate: ProjectSessionGate,
    private val libraryWriterGate: LibraryWriterGate = LibraryWriterGate { LibraryWriterLease {} },
    private val fs: FileSystemOps = NioFileSystemOps,
    private val io: CoroutineDispatcher,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val appVersion: String = "unknown",
    private val assetMetadataReader: LibraryAssetMetadataReader = AndroidLibraryAssetMetadataReader,
    /**
     * Entropy for the cover assigner ([D-017](docs/design/V2-SPEC-DEFECTS.md#d-017-ruling)). Injected
     * for the same reason [clock] and [newId] are — so a test can pin the draw — and, unlike them,
     * because it is the *only* input the assignment has. Nothing derived from a title, an id or a
     * neighbour reaches it, which is the ruling stated as a signature.
     */
    private val random: Random = Random.Default,
) : ProjectRepository, LibraryRestoreRepository, LibraryBackupRepository {

    private val libraryRoot = rootDir.toAbsolutePath().normalize()
    private val paths = ProjectPaths(libraryRoot)
    private val restoreWorkDir = libraryRoot.resolve(RESTORE_WORK_DIRECTORY)
    private val restoreCommitter = AdditiveLibraryRestoreCommitter(
        liveProjectsDir = paths.projectsRoot,
        liveAssetsDir = libraryRoot.resolve(ASSETS_DIRECTORY),
        restoreWorkDir = restoreWorkDir,
        fs = fs,
    )
    private val restoreStager = ZineLibraryBackupStager()
    private val backupWriter = ZineLibraryBackupWriter()
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    /** Cleared whenever the index may have diverged from committed file truth. */
    @Volatile
    private var reconciled = false

    override fun observeProjects(): Flow<List<ProjectSummary>> =
        dao.observeAll()
            .onStart {
                mutex.withLock {
                    if (ensureReconciledLocked() is DataResult.Failure) {
                        throw IOException("library recovery failed before Room reconciliation")
                    }
                }
            }
            .map { rows -> rows.mapNotNull(::toSummary).sortedByDescending { it.updatedAtEpochMs } }
            .flowOn(io)

    override suspend fun getProject(id: String): DataResult<ProjectSummary> = withContext(io) {
        mutex.withLock { ensureReconciledLocked() }.let { result ->
            if (result is DataResult.Failure) return@withContext result
        }
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
            when (val ready = ensureReconciledLocked()) {
                is DataResult.Failure -> return@withLock ready
                is DataResult.Success -> Unit
            }
            val id = newId()
            when (val saved = documents.save(id, blankDocument(format, paperSize))) {
                is DataResult.Failure -> return@withLock DataResult.Failure(saved.error)
                is DataResult.Success -> Unit
            }
            val now = clock()
            try {
                // D-017: the cover is assigned ONCE, here, at creation — and persisted in the same
                // atomic write as the title, so a zine can never exist without its visual identity.
                // `title` is in scope but is deliberately not passed to the assigner; the draw takes
                // entropy and nothing else, which is the property `two zines with the same title get
                // independently drawn covers` asserts.
                writeMeta(
                    id,
                    ProjectMeta(title = title, createdAtEpochMs = now).withCover(newZineCoverRecipe(random)),
                )
            } catch (e: IOException) {
                // A returned failure must leave no adoptable orphan (ADR-042 / Codex RF2).
                cleanupProjectFiles(id)
                return@withLock failure(DataError.Io("failed to write project metadata for '$id'", e))
            }
            syncRowFromDisk(id, updatedAtEpochMs = now)
        }
    }

    override suspend fun renameProject(id: String, title: String): DataResult<Unit> = withContext(io) {
        sessionBusy(id)?.let { return@withContext it }
        mutex.withLock {
            when (val ready = ensureReconciledLocked()) {
                is DataResult.Failure -> return@withLock ready
                is DataResult.Success -> Unit
            }
            val docFile = paths.documentFile(id)
                ?: return@withLock failure(DataError.NotFound(id))
            if (!Files.isRegularFile(docFile)) return@withLock failure(DataError.NotFound(id))
            val existing = readMetaOrNull(id)
            val createdAt = existing?.createdAtEpochMs ?: fileMtimeOrNull(docFile) ?: clock()
            try {
                // The atomic meta rewrite is the commit; the row below is derived.
                //
                // The rewrite is WHOLESALE, so every field not carried across is destroyed. D-017 is
                // explicit that "a physical object should retain its identity across renames", which
                // makes dropping the cover here a silent identity change — the exact defect the ruling
                // was written against, disguised as an unrelated edit. `createdAt` was already carried
                // for the same reason; the cover joins it.
                writeMeta(
                    id,
                    ProjectMeta(
                        title = title,
                        createdAtEpochMs = createdAt,
                        coverSurface = existing?.coverSurface,
                        coverStamp = existing?.coverStamp,
                    ),
                )
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
        // The SOURCE is gated: a live session's unflushed edits would make the copy silently stale.
        sessionBusy(id)?.let { return@withContext it }
        mutex.withLock {
            when (val ready = ensureReconciledLocked()) {
                is DataResult.Failure -> return@withLock ready
                is DataResult.Success -> Unit
            }
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
                // D-026: "duplicate content, not visual identity" — the copy DRAWS ITS OWN cover
                // rather than inheriting the source's. Two identical covers would be indistinguishable
                // on a covers-only shelf whose one question is "which zine is mine?", and ADR-083 moved
                // every distinguishing detail into the action sheet, so the cover is all there is.
                writeMeta(
                    copyId,
                    ProjectMeta(title = "$sourceTitle copy", createdAtEpochMs = now)
                        .withCover(newZineCoverRecipe(random)),
                )
            } catch (e: IOException) {
                cleanupProjectFiles(copyId)
                return@withLock failure(DataError.Io("failed to write project metadata for '$copyId'", e))
            }
            syncRowFromDisk(copyId, updatedAtEpochMs = now)
        }
    }

    override suspend fun deleteProject(id: String): DataResult<Unit> = withContext(io) {
        sessionBusy(id)?.let { return@withContext it }
        mutex.withLock {
            when (val ready = ensureReconciledLocked()) {
                is DataResult.Failure -> return@withLock ready
                is DataResult.Success -> Unit
            }
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

    /**
     * Restore a fully validated v2 archive additively under the same writer ownership and repository
     * mutex as ordinary project mutations. No live path is touched until staging and preparation are
     * complete; once commit starts, commit plus Room reconciliation are non-cancellable.
     */
    override suspend fun restoreLibrary(archive: Path): DataResult<LibraryRestoreReceipt> = withContext(io) {
        val lease = libraryWriterGate.tryAcquire()
            ?: return@withContext failure(DataError.Busy("the library has an active editor or restore"))
        lease.use {
            mutex.withLock {
                when (val recovered = recoverInterruptedRestoreLocked()) {
                    is DataResult.Failure -> return@withLock recovered
                    is DataResult.Success -> Unit
                }
                // Recovery can leave stale rows for project ids whose directories were just removed. A
                // new restore must reconcile those rows away before it chooses collision-free ids, or a
                // reused on-disk id could inherit stale Room metadata from the interrupted transaction.
                reconciled = false
                when (val indexed = reconcileLocked(requiredProjectIds = emptySet(), strictIo = true)) {
                    is DataResult.Failure -> return@withLock indexed
                    is DataResult.Success -> Unit
                }

                val staged = try {
                    restoreStager.stage(archive, restoreWorkDir)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (invalid: ZineBackupStagingException) {
                    if (invalid.reason == ZineBackupStagingException.Reason.FUTURE_VERSION) {
                        return@withLock failure(
                            DataError.SchemaTooNew(
                                documentVersion = invalid.encounteredVersion ?: Int.MAX_VALUE,
                                supportedVersion = invalid.supportedVersion ?: 0,
                            ),
                        )
                    }
                    return@withLock failure(DataError.Corrupt(invalid.message ?: "invalid backup", invalid))
                } catch (failure: Exception) {
                    return@withLock failure(DataError.Io("failed to stage library backup", failure))
                }

                staged.use { verified ->
                    val prepared = try {
                        prepareRestore(verified)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        return@withLock failure(DataError.Io("failed to prepare library restore", failure))
                    }

                    withContext(NonCancellable) {
                        try {
                            restoreCommitter.commit(prepared.restore)
                        } catch (failure: Exception) {
                            return@withContext failure(DataError.Io("failed to commit library restore", failure))
                        }
                        reconciled = false
                        when (
                            val indexed = reconcileLocked(
                                requiredProjectIds = prepared.ids.map { it.second }.toSet(),
                                strictIo = true,
                            )
                        ) {
                            is DataResult.Failure -> return@withContext indexed
                            is DataResult.Success -> Unit
                        }
                        val restored = ArrayList<RestoredProject>(prepared.ids.size)
                        for ((sourceId, localId) in prepared.ids) {
                            val row = try {
                                dao.findById(localId)
                            } catch (failure: Exception) {
                                return@withContext failure(
                                    DataError.Io("failed to read restored project '$localId'", failure),
                                )
                            }
                            val summary = row?.let(::toSummary)
                                ?: return@withContext failure(
                                    DataError.Io("restored project '$localId' was not reconciled"),
                                )
                            restored += RestoredProject(sourceId, summary)
                        }
                        DataResult.Success(LibraryRestoreReceipt(restored))
                    }
                }
            }
        }
    }

    /**
     * Freeze files-as-truth under the same library-wide lease used by restore, then stream one
     * self-validating archive to a unique private destination. Room is reconciled first but is not
     * used as backup authority.
     */
    override suspend fun createLibraryBackup(destination: Path): DataResult<LibraryBackupReceipt> = withContext(io) {
        val lease = libraryWriterGate.tryAcquire()
            ?: return@withContext failure(DataError.Busy("the library has an active editor or backup"))
        lease.use {
            mutex.withLock {
                when (val recovered = recoverInterruptedRestoreLocked()) {
                    is DataResult.Failure -> return@withLock recovered
                    is DataResult.Success -> Unit
                }
                reconciled = false
                when (val indexed = reconcileLocked(requiredProjectIds = emptySet(), strictIo = true)) {
                    is DataResult.Failure -> return@withLock indexed
                    is DataResult.Success -> Unit
                }

                val projectIds = listProjectIds().sorted()
                if (projectIds.isEmpty()) return@withLock failure(DataError.NotFound("library"))

                val documentSources = linkedMapOf<String, Path>()
                val projectEntries = ArrayList<ZineBackupProjectEntry>(projectIds.size)
                val referencedAssets = linkedSetOf<String>()
                for (id in projectIds) {
                    currentCoroutineContext().ensureActive()
                    val documentFile = paths.documentFile(id)
                        ?: return@withLock failure(DataError.Corrupt("project '$id' has an unsafe path"))
                    val meta = readMetaOrNull(id)
                        ?: return@withLock failure(DataError.Corrupt("project '$id' metadata is unreadable"))
                    val document = when (val loaded = documents.load(id)) {
                        is DataResult.Failure -> return@withLock loaded
                        is DataResult.Success -> loaded.value
                    }
                    val assetHashes = document.pages.asSequence()
                        .flatMap { it.elements.asSequence() }
                        .filterIsInstance<ImageElement>()
                        .mapTo(linkedSetOf()) { it.assetId }
                    referencedAssets += assetHashes
                    val documentByteCount = try {
                        Files.size(documentFile)
                    } catch (failure: IOException) {
                        return@withLock failure(DataError.Io("couldn't read project '$id' for backup", failure))
                    }
                    val rawSchemaVersion = try {
                        json.parseToJsonElement(Files.readString(documentFile)).jsonObject["schemaVersion"]
                            ?.jsonPrimitive?.intOrNull
                    } catch (failure: Exception) {
                        return@withLock failure(DataError.Corrupt("project '$id' document is malformed", failure))
                    } ?: return@withLock failure(DataError.Corrupt("project '$id' has no document schema version"))
                    val documentHash = try {
                        sha256(documentFile)
                    } catch (failure: IOException) {
                        return@withLock failure(DataError.Io("couldn't hash project '$id' for backup", failure))
                    }
                    projectEntries += ZineBackupProjectEntry(
                        sourceProjectId = id,
                        title = meta.title,
                        format = document.format,
                        paperSize = document.paperSize,
                        createdAtEpochMs = meta.createdAtEpochMs,
                        updatedAtEpochMs = fileMtimeOrNull(documentFile) ?: clock(),
                        documentSchemaVersion = rawSchemaVersion,
                        documentPath = "projects/$id/document.json",
                        documentSha256 = documentHash,
                        documentByteCount = documentByteCount,
                        assetHashes = assetHashes.toList(),
                        coverSurface = meta.coverSurface,
                        coverStamp = meta.coverStamp,
                    )
                    documentSources[id] = documentFile
                }

                val assetSources = linkedMapOf<String, Path>()
                val assetEntries = ArrayList<AssetEntry>(referencedAssets.size)
                for (hash in referencedAssets.sorted()) {
                    currentCoroutineContext().ensureActive()
                    val path = libraryRoot.resolve(ASSETS_DIRECTORY).resolve(hash)
                    if (!Files.isRegularFile(path)) {
                        return@withLock failure(DataError.Corrupt("project asset '$hash' is missing"))
                    }
                    val metadata = try {
                        assetMetadataReader.read(path)
                    } catch (failure: Exception) {
                        return@withLock failure(DataError.Corrupt("project asset '$hash' is not a readable image", failure))
                    }
                    val byteCount = try {
                        Files.size(path)
                    } catch (failure: IOException) {
                        return@withLock failure(DataError.Io("couldn't read project asset '$hash'", failure))
                    }
                    assetEntries += AssetEntry(hash, metadata.mimeType, metadata.widthPx, metadata.heightPx, byteCount)
                    assetSources[hash] = path
                }

                val manifest = ZineLibraryBackupManifest(
                    packageVersion = CURRENT_LIBRARY_BACKUP_VERSION,
                    kind = LIBRARY_BACKUP_KIND,
                    appVersion = appVersion.ifBlank { "unknown" },
                    createdAtEpochMs = clock(),
                    projects = projectEntries,
                    assets = assetEntries,
                )
                val archiveByteCount = try {
                    backupWriter.write(manifest, documentSources, assetSources, destination)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (invalid: ZineBackupWritingException) {
                    val error = when (invalid.reason) {
                        ZineBackupWritingException.Reason.IO_FAILURE,
                        ZineBackupWritingException.Reason.DESTINATION_EXISTS,
                        ZineBackupWritingException.Reason.SOURCE_UNAVAILABLE,
                        -> DataError.Io("couldn't create the private library backup", invalid)
                        else -> DataError.Corrupt("the local library could not be backed up safely", invalid)
                    }
                    return@withLock failure(error)
                }
                DataResult.Success(
                    LibraryBackupReceipt(
                        projectCount = projectEntries.size,
                        assetCount = assetEntries.size,
                        archiveByteCount = archiveByteCount,
                    ),
                )
            }
        }
    }

    /**
     * ADR-044 §1: await [sessionGate] BEFORE the repository mutex (a gated wait inside the lock would
     * stall unrelated ops) and refuse with [DataError.Busy] while [id] has a live editor session.
     * `create` never calls this — a fresh UUID can have no session. TOCTOU after the gate passes is
     * accepted under the nav invariants (shelf and editor never simultaneously active; ADR-044).
     */
    private suspend fun sessionBusy(id: String): DataResult.Failure? =
        if (sessionGate.awaitNoSession(id)) null
        else failure(DataError.Busy("project '$id' has a live editor session"))

    // ---- library restore -----------------------------------------------------------------------

    private suspend fun prepareRestore(staged: StagedZineLibraryBackup): PreparedAndroidRestore {
        val existingIds = listProjectIds().toSet()
        val localIds = RestoreProjectIdAllocator.allocate(
            sourceProjectIds = staged.projects.map { it.manifestEntry.sourceProjectId },
            existingProjectIds = existingIds,
            mintId = newId,
        )
        val preparedProjectsRoot = staged.root.resolve(PREPARED_PROJECTS_DIRECTORY)
        Files.createDirectories(preparedProjectsRoot)

        val preparedProjects = ArrayList<PreparedRestoreProject>(staged.projects.size)
        val idPairs = ArrayList<Pair<String, String>>(staged.projects.size)
        staged.projects.zip(localIds).forEach { (project, localId) ->
            currentCoroutineContext().ensureActive()
            val entry = project.manifestEntry
            val projectDir = preparedProjectsRoot.resolve(localId)
            Files.createDirectory(projectDir)
            val documentFile = projectDir.resolve(ProjectPaths.DOCUMENT_FILE)
            Files.copy(project.documentPath, documentFile)
            Files.setLastModifiedTime(documentFile, FileTime.fromMillis(entry.updatedAtEpochMs))

            val meta = ProjectMeta(
                title = entry.title,
                createdAtEpochMs = entry.createdAtEpochMs,
                coverSurface = entry.coverSurface,
                coverStamp = entry.coverStamp,
            )
            store.write(
                projectDir.resolve(ProjectPaths.META_FILE),
                json.encodeToString(ProjectMeta.serializer(), meta).encodeToByteArray(),
            )
            preparedProjects += PreparedRestoreProject(localId, projectDir)
            idPairs += entry.sourceProjectId to localId
        }

        return PreparedAndroidRestore(
            restore = PreparedLibraryRestore(
                transactionId = UUID.randomUUID().toString(),
                projects = preparedProjects,
                assets = staged.assets.map { (hash, path) -> PreparedRestoreAsset(hash, path) },
            ),
            ids = idPairs,
        )
    }

    /** Recovery is fail-closed and always precedes any files-to-Room scan. */
    private fun recoverInterruptedRestoreLocked(): DataResult<Unit> = try {
        if (restoreCommitter.recoverInterruptedCommit()) reconciled = false
        DataResult.Success(Unit)
    } catch (failure: Exception) {
        failure(DataError.Io("failed to recover an interrupted library restore", failure))
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
            // The index mirrors the sidecar, which is the authority (ADR-042). Because this is the
            // SINGLE files→row derivation — every mutation and the reconcile scan both land here — the
            // legacy backfill inside readMetaOrBackfill reaches the index for free, and a project can
            // never end up with a cover in one and not the other.
            coverSurface = meta.coverSurface,
            coverStamp = meta.coverStamp,
        )
        try {
            dao.upsert(entity)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reconciled = false // file truth is committed; re-derive the index on next use
            return failure(DataError.Io("failed to index project '$id'", e))
        }
        // Derived from the row that was just written, NOT assembled a second time by hand. This used to
        // be a parallel ProjectSummary(...) construction and it silently dropped the cover the moment
        // the field was added — a create returned a coverless zine while the index and the sidecar both
        // held one. Two construction sites for one projection is the defect; one site is the fix.
        return toSummary(entity)?.let { DataResult.Success(it) }
            // Unreachable by construction: format/paperSize were written from the document's own enums
            // a few lines above, so they always parse back. Kept total rather than asserted.
            ?: failure(DataError.Io("failed to project indexed row for '$id'"))
    }

    /**
     * Reconcile the index against the on-disk project set (ADR-042 seeding: adopts pre-Room
     * projects — the S4 `"default"` seed becomes an ordinary row — and drops rows without files).
     * Idempotent and re-runnable; repair, not a gate: a failed scan leaves [reconciled] false so the
     * next repository use retries, while the current call proceeds against the existing index
     * (files stay the truth regardless).
     */
    private suspend fun ensureReconciledLocked(): DataResult<Unit> {
        when (val recovered = recoverInterruptedRestoreLocked()) {
            is DataResult.Failure -> return recovered
            is DataResult.Success -> Unit
        }
        if (reconciled) return DataResult.Success(Unit)
        return reconcileLocked(requiredProjectIds = emptySet(), strictIo = false)
    }

    /**
     * Rebuild Room from authoritative files. Ordinary shelf reads retain ADR-042's degrade-and-retry
     * behavior; restore names the exact [requiredProjectIds] that must become visible before it may
     * report success. Unrelated corrupt projects remain the existing "skip and retry later" limitation.
     */
    private suspend fun reconcileLocked(
        requiredProjectIds: Set<String>,
        strictIo: Boolean,
    ): DataResult<Unit> {
        var fullyReconciled = true
        try {
            val onDisk = listProjectIds()
            val indexed = dao.ids().toSet()
            for (id in onDisk) {
                if (id !in indexed) {
                    val docFile = paths.documentFile(id) ?: continue
                    // Unreadable documents (Corrupt/SchemaTooNew) return Failure here and are
                    // skipped — invisible to the shelf, bytes left for a future repair path.
                    when (val synced = syncRowFromDisk(id, updatedAtEpochMs = fileMtimeOrNull(docFile) ?: clock())) {
                        is DataResult.Failure -> {
                            fullyReconciled = false
                            if (id in requiredProjectIds) return synced
                        }
                        is DataResult.Success -> Unit
                    }
                }
            }
            val onDiskSet = onDisk.toSet()
            for (id in indexed) {
                if (id !in onDiskSet) dao.deleteById(id)
            }
            reconciled = fullyReconciled
            return DataResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (failure: Exception) {
            // Deliberate degrade-not-fail: the observed list falls back to the current index and
            // the scan retries on the next repository use (reconciled stays false).
            reconciled = false
            return if (strictIo) {
                failure(DataError.Io("failed to reconcile restored projects", failure))
            } else {
                DataResult.Success(Unit)
            }
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
     *
     * **The cover follows the same rule as [backfillCoverIfLegacy], and for the same reason.** An
     * adopted project is only given one when the sidecar that would hold it is actually written; an
     * unwritable *or* unreadable sidecar yields a **coverless** meta rather than a cover that lives
     * only in this process. The index is rebuildable ([ADR-042](docs/DECISIONS.md#adr-042)), so a
     * cover kept in the row and nowhere else is repainted the next time the index is rebuilt — the
     * identity flicker D-017 forbids, arriving one function away from where it was closed.
     */
    private fun readMetaOrBackfill(id: String, docFile: Path): ProjectMeta {
        readMetaOrNull(id)?.let { return backfillCoverIfLegacy(id, it) }
        val fallback = ProjectMeta(
            title = DEFAULT_TITLE,
            createdAtEpochMs = fileMtimeOrNull(docFile) ?: clock(),
        )
        val metaFile = paths.metaFile(id) ?: return fallback
        // Present-but-unreadable: the bytes stay for repair, so nothing is written — and therefore no
        // cover is assigned either, because assigning one here would redraw it on every single read.
        if (Files.exists(metaFile)) return fallback
        val assigned = fallback.withCover(newZineCoverRecipe(random))
        return try {
            writeMeta(id, assigned)
            assigned
        } catch (_: IOException) {
            // The row is still built from the fallback; the backfill retries on a later scan.
            fallback
        }
    }

    /**
     * The **legacy cover backfill** ([D-026](docs/design/V2-SPEC-DEFECTS.md#d-026-ruling)):
     *
     * > *"Legacy zines receive a cover on first presentation. The assigned cover is then persisted."*
     *
     * A readable sidecar with no cover belongs to a zine created before the field existed. It is given
     * one **here**, once, and the assignment is written straight back — so the very next read finds it
     * stored and this function returns early. That "once" is the whole contract: assign-on-every-read
     * and assign-on-first-read are indistinguishable in a single render and differ in every subsequent
     * one, which is why the test that matters reads twice.
     *
     * **A failed write must not fabricate an identity.** If persistence fails the meta is returned
     * *unchanged* — still coverless — rather than carrying a cover that exists only in memory. The zine
     * then draws no persisted cover this session and is offered the backfill again next time, which is
     * the honest degradation: a cover that would silently differ on every launch is worse than one that
     * is visibly not yet assigned, and it would break D-017's identity guarantee while appearing to
     * satisfy it.
     */
    private fun backfillCoverIfLegacy(id: String, meta: ProjectMeta): ProjectMeta {
        if (meta.coverRecipe() != null) return meta
        // A v2 backup may deliberately carry partial or future cover names. Those are degraded
        // metadata, not a legacy sidecar: preserve the raw identity and show no cover rather than
        // silently replacing it with a new local draw. Only the old both-fields-absent shape is
        // eligible for D-026's first-presentation backfill.
        if (meta.coverSurface != null || meta.coverStamp != null) return meta
        val assigned = meta.withCover(newZineCoverRecipe(random))
        return try {
            writeMeta(id, assigned)
            assigned
        } catch (_: IOException) {
            meta
        }
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
            cover = entity.coverRecipe(),
        )
    }

    /**
     * The indexed cover, or `null` when this project is **legacy** — created before the field existed,
     * so it has never been assigned one ([D-026](docs/design/V2-SPEC-DEFECTS.md#d-026-ruling)).
     *
     * An **unrecognised** name is treated exactly like a missing one. That matters: it means renaming an
     * enum constant in a future release degrades to a re-draw rather than to a crash on a user's shelf,
     * and it keeps this function total.
     */
    private fun ProjectEntity.coverRecipe(): ZineCoverRecipe? {
        val surface = ZineCoverSurface.entries.firstOrNull { it.name == coverSurface } ?: return null
        val stamp = ZineCoverStamp.entries.firstOrNull { it.name == coverStamp } ?: return null
        return ZineCoverRecipe(surface, stamp)
    }

    /** The sidecar's cover, by the same total mapping the index uses. */
    private fun ProjectMeta.coverRecipe(): ZineCoverRecipe? {
        val surface = ZineCoverSurface.entries.firstOrNull { it.name == coverSurface } ?: return null
        val stamp = ZineCoverStamp.entries.firstOrNull { it.name == coverStamp } ?: return null
        return ZineCoverRecipe(surface, stamp)
    }

    /** Stamp a recipe onto a sidecar record. The one place meta learns a cover. */
    private fun ProjectMeta.withCover(recipe: ZineCoverRecipe): ProjectMeta =
        copy(coverSurface = recipe.surface.name, coverStamp = recipe.stamp.name)

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

    private data class PreparedAndroidRestore(
        val restore: PreparedLibraryRestore,
        val ids: List<Pair<String, String>>,
    )

    private companion object {
        /** Fallback title for adopted projects with no readable sidecar. */
        const val DEFAULT_TITLE = "My zine"
        const val ASSETS_DIRECTORY = "assets"
        const val RESTORE_WORK_DIRECTORY = ".library-restore"
        const val PREPARED_PROJECTS_DIRECTORY = "prepared-projects"
    }

    private suspend fun sha256(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        BufferedInputStream(Files.newInputStream(path), buffer.size).use { input ->
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
