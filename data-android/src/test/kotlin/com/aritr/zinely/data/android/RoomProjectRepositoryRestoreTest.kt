package com.aritr.zinely.data.android

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aritr.zinely.core.data.asset.AssetEntry
import com.aritr.zinely.core.data.asset.CURRENT_LIBRARY_BACKUP_VERSION
import com.aritr.zinely.core.data.asset.LIBRARY_BACKUP_KIND
import com.aritr.zinely.core.data.asset.ZineBackupProjectEntry
import com.aritr.zinely.core.data.asset.ZineLibraryBackupManifest
import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.errorOrNull
import com.aritr.zinely.core.data.repository.getOrNull
import com.aritr.zinely.core.data.serialization.JsonDocumentSerializer
import com.aritr.zinely.core.data.storage.AtomicFileStore
import com.aritr.zinely.core.data.storage.FileSystemOps
import com.aritr.zinely.core.data.storage.NioFileSystemOps
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.data.android.room.ProjectDao
import com.aritr.zinely.data.android.room.ProjectEntity
import com.aritr.zinely.data.android.room.ZinelyDatabase
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomProjectRepositoryRestoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var root: Path
    private lateinit var store: AtomicFileStore
    private lateinit var documents: DocumentRepositoryImpl
    private lateinit var db: ZinelyDatabase
    private var nextId = 1

    @Before
    fun setUp() {
        root = tmp.root.toPath()
        store = AtomicFileStore()
        documents = DocumentRepositoryImpl(rootDir = root, store = store)
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ZinelyDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun repo(
        dao: ProjectDao = db.projectDao(),
        libraryWriterGate: LibraryWriterGate = LibraryWriterGate { LibraryWriterLease {} },
        fs: FileSystemOps = NioFileSystemOps,
    ): RoomProjectRepository = RoomProjectRepository(
        rootDir = root,
        dao = dao,
        documents = documents,
        store = store,
        sessionGate = ProjectSessionGate { true },
        libraryWriterGate = libraryWriterGate,
        fs = fs,
        io = Dispatchers.Unconfined,
        newId = { "p${nextId++}" },
    )

    @Test
    fun `successful restore remaps a colliding id, preserves timestamps, and deduplicates a shared asset`() = runTest {
        val repository = repo()
        val existing = repository.createProject("Existing", ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER).getOrNull()!!.id
        val sharedBytes = "shared-image".encodeToByteArray()
        val sharedHash = sha256(sharedBytes)
        val archive = writeArchive(
            projects = listOf(
                BackupProjectFixture(existing, "Collision title", 10L, 30L, document(sharedHash)),
                BackupProjectFixture(
                    "incoming",
                    "Fresh title",
                    20L,
                    40L,
                    document(sharedHash),
                    coverSurface = "FUTURE_SURFACE",
                ),
            ),
            assets = mapOf(sharedHash to sharedBytes),
        )

        val receipt = repository.restoreLibrary(archive).getOrNull()!!

        assertEquals(listOf(existing, "incoming"), receipt.projects.map { it.sourceProjectId })
        assertEquals(listOf("p2", "incoming"), receipt.projects.map { it.project.id })
        assertEquals("Collision title", receipt.projects.first().project.title)
        assertEquals(10L, metaOnDisk("p2").createdAtEpochMs)
        assertEquals(20L, metaOnDisk("incoming").createdAtEpochMs)
        assertEquals(30L, Files.getLastModifiedTime(documentFile("p2")).toMillis())
        assertEquals(40L, Files.getLastModifiedTime(documentFile("incoming")).toMillis())
        assertEquals(document(sharedHash), documents.load("p2").getOrNull())
        assertEquals(document(sharedHash), documents.load("incoming").getOrNull())
        assertTrue(Files.isRegularFile(root.resolve("assets").resolve(sharedHash)))
        val assetFiles = Files.list(root.resolve("assets")).use { stream ->
            stream.filter { Files.isRegularFile(it) }.count()
        }
        assertEquals(1L, assetFiles)
        assertNull(receipt.projects.single { it.sourceProjectId == "incoming" }.project.cover)
        assertEquals("FUTURE_SURFACE", metaOnDisk("incoming").coverSurface)
        assertNull(metaOnDisk("incoming").coverStamp)
    }

    @Test
    fun `invalid archive leaves existing projects untouched and writes no restored project`() = runTest {
        val repository = repo()
        val existing = repository.createProject("Keep", ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER).getOrNull()!!.id
        val assetBytes = "missing".encodeToByteArray()
        val assetHash = sha256(assetBytes)
        val archive = writeArchive(
            projects = listOf(BackupProjectFixture("broken", "Broken", 1L, 2L, document(assetHash))),
            assets = mapOf(assetHash to assetBytes),
            omittedEntries = setOf("assets/$assetHash"),
        )

        val result = repository.restoreLibrary(archive)

        assertTrue(result.errorOrNull() is DataError.Corrupt)
        assertEquals(listOf(existing), repository.observeProjects().first().map { it.id })
        assertFalse(Files.exists(root.resolve("projects").resolve("broken")))
        assertFalse(Files.exists(root.resolve("assets").resolve(assetHash)))
    }

    @Test
    fun `recovered stale rows are dropped before restore id allocation so reused ids get fresh metadata`() = runTest {
        documents.save("reuse", document())
        db.projectDao().upsert(
            ProjectEntity(
                id = "reuse",
                title = "Stale row",
                format = ZineFormat.SINGLE_SHEET_8.name,
                paperSize = PaperSize.LETTER.name,
                createdAtEpochMs = 1L,
                updatedAtEpochMs = 1L,
                documentSchemaVersion = 2,
            ),
        )
        writePendingRestoreJournal("reuse")
        val archive = writeArchive(
            projects = listOf(BackupProjectFixture("reuse", "Fresh restore", 10L, 25L, document())),
        )

        val receipt = repo().restoreLibrary(archive).getOrNull()!!

        assertEquals("reuse", receipt.projects.single().project.id)
        assertEquals("Fresh restore", receipt.projects.single().project.title)
        assertEquals("Fresh restore", repo().getProject("reuse").getOrNull()!!.title)
    }

    @Test
    fun `pending restore is recovered before a shelf read can reconcile transitional files`() = runTest {
        documents.save("interrupted", document())
        db.projectDao().upsert(
            ProjectEntity(
                id = "interrupted",
                title = "Transitional",
                format = ZineFormat.SINGLE_SHEET_8.name,
                paperSize = PaperSize.LETTER.name,
                createdAtEpochMs = 1L,
                updatedAtEpochMs = 1L,
                documentSchemaVersion = 2,
            ),
        )
        writePendingRestoreJournal("interrupted")

        val repository = repo()

        assertTrue(repository.observeProjects().first().isEmpty())
        assertFalse(Files.exists(root.resolve("projects/interrupted")))
        assertFalse(Files.exists(root.resolve(".library-restore/pending-library-restore.v1")))
        assertTrue(repository.observeProjects().first().isEmpty())
    }

    @Test
    fun `restore succeeds even when an unrelated corrupt project is already on disk`() = runTest {
        val badDir = root.resolve("projects").resolve("bad")
        Files.createDirectories(badDir)
        Files.write(badDir.resolve("document.json"), "{ not a document".encodeToByteArray())
        val archive = writeArchive(
            projects = listOf(BackupProjectFixture("good", "Healthy", 5L, 15L, document())),
        )

        val receipt = repo().restoreLibrary(archive).getOrNull()!!

        assertEquals(listOf("good"), receipt.projects.map { it.project.id })
        assertNull(db.projectDao().findById("bad"))
        assertEquals("Healthy", repo().getProject("good").getOrNull()!!.title)
    }

    @Test
    fun `restore returns Busy when the library writer gate is not available`() = runTest {
        val archive = writeArchive(
            projects = listOf(BackupProjectFixture("good", "Healthy", 5L, 15L, document())),
        )

        val result = repo(libraryWriterGate = LibraryWriterGate { null }).restoreLibrary(archive)

        assertTrue(result.errorOrNull() is DataError.Busy)
    }

    @Test
    fun `real autosave registry excludes restore and editor sessions in both directions`() = runTest {
        val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        val autosaveFactory = AutosaveCoordinatorFactory(
            autosaveScope = scope,
            ioDispatcher = Dispatchers.Unconfined,
            repository = documents,
            failureSink = InMemorySaveFailureSink(),
        )
        val gate = AutosaveLibraryWriterGate(autosaveFactory)
        val archive = writeArchive(
            projects = listOf(BackupProjectFixture("incoming", "Incoming", 5L, 15L, document())),
        )
        val editor = autosaveFactory.create("open-editor") { document() }

        assertTrue(repo(libraryWriterGate = gate).restoreLibrary(archive).errorOrNull() is DataError.Busy)

        editor.cancel()
        editor.awaitReleased()
        gate.tryAcquire()!!.use {
            val blocked = runCatching { autosaveFactory.create("new-editor") { document() } }.exceptionOrNull()
            assertTrue(blocked is IllegalStateException)
        }
        scope.cancel()
    }

    @Test
    fun `Room failure after authoritative commit is healed by a later reconcile`() = runTest {
        val archive = writeArchive(
            projects = listOf(BackupProjectFixture("recoverable", "Recoverable", 5L, 15L, document())),
        )
        val failingDao = FailFirstUpsertProjectDao(db.projectDao())

        val failed = repo(dao = failingDao).restoreLibrary(archive)

        assertTrue(failed.errorOrNull() is DataError.Io)
        assertTrue(Files.isRegularFile(documentFile("recoverable")))
        assertTrue(Files.isRegularFile(root.resolve("projects/recoverable/meta.json")))

        val recovered = repo().getProject("recoverable").getOrNull()
        assertEquals("Recoverable", recovered?.title)
        assertEquals("Recoverable", db.projectDao().findById("recoverable")?.title)
    }

    private fun document(assetHash: String? = null): ZineDocument {
        val pages = (0 until 8).map { index ->
            val role = when (index) {
                0 -> PageRole.FRONT_COVER
                7 -> PageRole.BACK_COVER
                else -> PageRole.INTERIOR
            }
            val elements = if (index == 0 && assetHash != null) {
                listOf(
                    ImageElement(
                        id = "image-$index",
                        transform = Transform(0.0, 0.0, 100.0, 100.0),
                        assetId = assetHash,
                    ),
                )
            } else {
                emptyList()
            }
            Page(index = index, role = role, elements = elements)
        }
        return ZineDocument(format = ZineFormat.SINGLE_SHEET_8, paperSize = PaperSize.LETTER, pages = pages)
    }

    private fun writeArchive(
        projects: List<BackupProjectFixture>,
        assets: Map<String, ByteArray> = emptyMap(),
        omittedEntries: Set<String> = emptySet(),
    ): Path {
        val serializer = JsonDocumentSerializer()
        val documentsById = projects.associate { it.sourceProjectId to serializer.serialize(it.document).encodeToByteArray() }
        val manifest = ZineLibraryBackupManifest(
            packageVersion = CURRENT_LIBRARY_BACKUP_VERSION,
            kind = LIBRARY_BACKUP_KIND,
            appVersion = "test",
            createdAtEpochMs = 99L,
            projects = projects.map { project ->
                val bytes = documentsById.getValue(project.sourceProjectId)
                ZineBackupProjectEntry(
                    sourceProjectId = project.sourceProjectId,
                    title = project.title,
                    format = project.document.format,
                    paperSize = project.document.paperSize,
                    createdAtEpochMs = project.createdAtEpochMs,
                    updatedAtEpochMs = project.updatedAtEpochMs,
                    documentSchemaVersion = project.document.schemaVersion,
                    documentPath = "projects/${project.sourceProjectId}/document.json",
                    documentSha256 = sha256(bytes),
                    documentByteCount = bytes.size.toLong(),
                    assetHashes = project.document.pages.flatMap { page ->
                        page.elements.filterIsInstance<ImageElement>().map { it.assetId }
                    }.distinct(),
                    coverSurface = project.coverSurface,
                    coverStamp = project.coverStamp,
                )
            },
            assets = assets.map { (hash, bytes) -> AssetEntry(hash, "image/jpeg", 32, 32, bytes.size.toLong()) },
        )
        val archive = Files.createTempFile(root, "restore-", ".zine")
        val entries = linkedMapOf<String, ByteArray>()
        entries["manifest.json"] =
            Json.encodeToString(ZineLibraryBackupManifest.serializer(), manifest).encodeToByteArray()
        entries.putAll(documentsById.mapKeys { (id, _) -> "projects/$id/document.json" })
        entries.putAll(assets.mapKeys { (hash, _) -> "assets/$hash" })
        ZipOutputStream(Files.newOutputStream(archive, StandardOpenOption.WRITE)).use { zip ->
            entries.filterKeys { it !in omittedEntries }.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return archive
    }

    private fun writePendingRestoreJournal(vararg projectIds: String) {
        val journal = root.resolve(".library-restore").resolve("pending-library-restore.v1")
        Files.createDirectories(journal.parent)
        val body = buildString {
            appendLine("ZINELY_LIBRARY_RESTORE_V1")
            appendLine("transaction=restore-pending")
            projectIds.forEach { append("project=").appendLine(it) }
        }
        Files.write(journal, body.encodeToByteArray())
    }

    private fun documentFile(id: String): Path = root.resolve("projects").resolve(id).resolve("document.json")

    private fun metaOnDisk(id: String): ProjectMeta =
        Json.decodeFromString(ProjectMeta.serializer(), Files.readString(root.resolve("projects").resolve(id).resolve("meta.json")))

    private fun sha256(bytes: ByteArray): String =
        java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private data class BackupProjectFixture(
        val sourceProjectId: String,
        val title: String,
        val createdAtEpochMs: Long,
        val updatedAtEpochMs: Long,
        val document: ZineDocument,
        val coverSurface: String? = null,
        val coverStamp: String? = null,
    )

    private class FailFirstUpsertProjectDao(
        private val delegate: ProjectDao,
    ) : ProjectDao {
        private var failed = false

        override fun observeAll(): Flow<List<ProjectEntity>> = delegate.observeAll()

        override suspend fun findById(id: String): ProjectEntity? = delegate.findById(id)

        override suspend fun ids(): List<String> = delegate.ids()

        override suspend fun upsert(project: ProjectEntity) {
            if (!failed) {
                failed = true
                throw IOException("injected Room failure after file commit")
            }
            delegate.upsert(project)
        }

        override suspend fun deleteById(id: String): Unit = delegate.deleteById(id)
    }
}
