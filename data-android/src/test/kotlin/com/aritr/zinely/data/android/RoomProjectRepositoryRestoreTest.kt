package com.aritr.zinely.data.android

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aritr.zinely.core.data.asset.AssetEntry
import com.aritr.zinely.core.data.asset.CURRENT_LIBRARY_BACKUP_VERSION
import com.aritr.zinely.core.data.asset.LIBRARY_BACKUP_KIND
import com.aritr.zinely.core.data.asset.ZineBackupOmission
import com.aritr.zinely.core.data.asset.ZineBackupProjectEntry
import com.aritr.zinely.core.data.asset.ZineLibraryBackupManifest
import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.errorOrNull
import com.aritr.zinely.core.data.repository.getOrNull
import com.aritr.zinely.core.data.serialization.JsonDocumentSerializer
import com.aritr.zinely.core.data.storage.AtomicFileStore
import com.aritr.zinely.core.data.storage.FileSystemOps
import com.aritr.zinely.core.data.storage.NioFileSystemOps
import com.aritr.zinely.core.data.storage.ZineBackupWriteLimits
import com.aritr.zinely.core.data.storage.ZineLibraryBackupStager
import com.aritr.zinely.core.data.storage.ZineLibraryBackupWriter
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
import java.io.ByteArrayInputStream
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
import kotlin.streams.toList
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
        assetMetadataReader: LibraryAssetMetadataReader = LibraryAssetMetadataReader {
            LibraryAssetMetadata("image/jpeg", 32, 32)
        },
        backupWriter: ZineLibraryBackupWriter = ZineLibraryBackupWriter(),
        usableBytes: (Path) -> Long = { Long.MAX_VALUE },
        restoreStager: ZineLibraryBackupStager = ZineLibraryBackupStager(),
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
        appVersion = "test-version",
        assetMetadataReader = assetMetadataReader,
        backupWriter = backupWriter,
        usableBytes = usableBytes,
        restoreStager = restoreStager,
    )

    @Test
    fun `backup snapshots real files and one shared asset into a restorable archive`() = runTest {
        val repository = repo()
        val first = repository.createProject("First", ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER).getOrNull()!!
        val second = repository.createProject("Second", ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER).getOrNull()!!
        val assetBytes = "one shared import master".encodeToByteArray()
        val assetHash = sha256(assetBytes)
        Files.createDirectories(root.resolve("assets"))
        Files.write(root.resolve("assets").resolve(assetHash), assetBytes)
        assertTrue(documents.save(first.id, document(assetHash)).getOrNull() != null)
        assertTrue(documents.save(second.id, document(assetHash)).getOrNull() != null)
        val archive = root.resolve("library.zine")

        val receipt = repository.createLibraryBackup(archive).getOrNull()!!

        assertEquals(2, receipt.projectCount)
        assertEquals(1, receipt.assetCount)
        assertEquals(Files.size(archive), receipt.archiveByteCount)
        ZineLibraryBackupStager().stage(archive, root.resolve("verify-stage")).use { staged ->
            assertEquals(listOf(first.id, second.id), staged.projects.map { it.manifestEntry.sourceProjectId })
            assertEquals(setOf(assetHash), staged.assets.keys)
            assertEquals("test-version", staged.manifest.appVersion)
        }
    }

    @Test
    fun `backup refuses an active writer without creating an archive`() = runTest {
        val repository = repo(libraryWriterGate = LibraryWriterGate { null })
        val archive = root.resolve("busy.zine")

        val result = repository.createLibraryBackup(archive)

        assertTrue(result.errorOrNull() is DataError.Busy)
        assertFalse(Files.exists(archive))
    }

    @Test
    fun `a library whose only zine has a poisoned photo saves nothing and writes no file`() = runTest {
        val repository = repo()
        val project = repository.createProject("Poisoned", ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER).getOrNull()!!
        val declaredHash = sha256("expected".encodeToByteArray())
        Files.createDirectories(root.resolve("assets"))
        Files.write(root.resolve("assets").resolve(declaredHash), "different".encodeToByteArray())
        assertTrue(documents.save(project.id, document(declaredHash)).getOrNull() != null)
        val archive = root.resolve("poisoned.zine")

        val receipt = repository.createLibraryBackup(archive).getOrNull()!!

        assertEquals(0, receipt.projectCount)
        assertEquals(1, receipt.totalCount)
        assertEquals(listOf(ZineBackupOmission("Poisoned", ZineBackupOmission.PHOTO)), receipt.omitted)
        assertFalse(Files.exists(archive))
    }

    // ---- ADR-122: skip-and-list backups ------------------------------------------------------------

    @Test
    fun `one poisoned photo leaves out only its zine, and the archive says so`() = runTest {
        val repository = repo()
        val good = saved(repository, "Good", photo("good-photo"))
        saved(repository, "Poisoned", poisonedPhoto())
        val archive = root.resolve("partial.zine")

        val receipt = repository.createLibraryBackup(archive).getOrNull()!!

        assertEquals(1, receipt.projectCount)
        assertEquals(2, receipt.totalCount)
        assertEquals(listOf(ZineBackupOmission("Poisoned", ZineBackupOmission.PHOTO)), receipt.omitted)
        assertFalse(receipt.omittedOffShelf)
        ZineLibraryBackupStager().stage(archive, root.resolve("verify")).use { staged ->
            assertEquals(listOf(good), staged.projects.map { it.manifestEntry.sourceProjectId })
            assertEquals(setOf(sha256("good-photo".encodeToByteArray())), staged.assets.keys) // exact closure
            assertEquals(receipt.omitted, staged.manifest.omitted)
        }
    }

    @Test
    fun `a poisoned photo shared by two zines leaves out both, each named`() = runTest {
        val repository = repo()
        val shared = poisonedPhoto()
        saved(repository, "Moth Club Bulletin", shared)
        saved(repository, "Riso tests", shared)
        saved(repository, "Healthy")

        val archive = root.resolve("shared.zine")

        val receipt = repository.createLibraryBackup(archive).getOrNull()!!

        assertEquals(1, receipt.projectCount)
        assertEquals(
            setOf("Moth Club Bulletin", "Riso tests"),
            receipt.omitted.map { it.title }.toSet(),
        )
        assertTrue(receipt.omitted.all { it.reason == ZineBackupOmission.PHOTO })
        ZineLibraryBackupStager().stage(archive, root.resolve("verify")).use { staged ->
            assertTrue("the poisoned photo never enters the archive", staged.assets.isEmpty())
        }
    }

    @Test
    fun `a photo that changes after its check is left out by the writer backstop and the archive rebuilt`() = runTest {
        val changing = photo("changes-under-us")
        val writer = ZineLibraryBackupWriter(
            openSource = { source ->
                if (source.fileName.toString() == changing) ByteArrayInputStream("tampered".encodeToByteArray())
                else Files.newInputStream(source)
            },
        )
        val repository = repo(backupWriter = writer)
        val kept = saved(repository, "Kept")
        saved(repository, "Changed", changing)
        val archive = root.resolve("out").resolve("backstop.zine")

        val receipt = repository.createLibraryBackup(archive).getOrNull()!!

        assertEquals(listOf(ZineBackupOmission("Changed", ZineBackupOmission.PHOTO)), receipt.omitted)
        assertEquals(listOf(archive.fileName.toString()), Files.list(archive.parent).use { it.map { p -> p.fileName.toString() }.toList() })
        ZineLibraryBackupStager().stage(archive, root.resolve("verify")).use { staged ->
            assertEquals(listOf(kept), staged.projects.map { it.manifestEntry.sourceProjectId })
            assertTrue(staged.assets.isEmpty())
        }
    }

    @Test
    fun `a photo read that fails once is retried, and one that keeps failing is left out`() = runTest {
        var failures = 1
        val flaky = LibraryAssetMetadataReader {
            if (failures-- > 0) throw IOException("I/O error") else LibraryAssetMetadata("image/jpeg", 32, 32)
        }
        val repository = repo(assetMetadataReader = flaky)
        saved(repository, "Flaky", photo("flaky"))

        assertEquals(1, repository.createLibraryBackup(root.resolve("once.zine")).getOrNull()!!.projectCount)

        failures = 2
        val twice = repository.createLibraryBackup(root.resolve("twice.zine")).getOrNull()!!
        assertEquals(listOf(ZineBackupOmission("Flaky", ZineBackupOmission.PHOTO)), twice.omitted)
    }

    @Test
    fun `a photo the package validator refuses leaves out its zine`() = runTest {
        val repository = repo(
            assetMetadataReader = { path ->
                if (path.fileName.toString() == sha256("huge".encodeToByteArray())) LibraryAssetMetadata("image/jpeg", 9_000, 32)
                else LibraryAssetMetadata("image/png", 32, 32)
            },
        )
        saved(repository, "Too big", photo("huge"))
        saved(repository, "Not a jpeg", photo("png"))
        saved(repository, "Fine")

        val receipt = repository.createLibraryBackup(root.resolve("validated.zine")).getOrNull()!!

        assertEquals(1, receipt.projectCount)
        assertEquals(setOf("Too big", "Not a jpeg"), receipt.omitted.map { it.title }.toSet())
        assertTrue(receipt.omitted.all { it.reason == ZineBackupOmission.PHOTO })
    }

    @Test
    fun `a zine the package validator refuses is left out as unreadable`() = runTest {
        val repository = repo()
        val odd = saved(repository, "Odd times")
        Files.write(
            root.resolve("projects/$odd/meta.json"),
            Json.encodeToString(ProjectMeta.serializer(), ProjectMeta(title = "Odd times", createdAtEpochMs = -5L)).encodeToByteArray(),
        )
        saved(repository, "Fine")

        val receipt = repository.createLibraryBackup(root.resolve("times.zine")).getOrNull()!!

        assertEquals(listOf(ZineBackupOmission("Odd times", ZineBackupOmission.UNREADABLE)), receipt.omitted)
    }

    @Test
    fun `unreadable, newer and nameless zines are left out with their reasons and names`() = runTest {
        val repository = repo()
        val broken = saved(repository, "Broken")
        val newer = saved(repository, "From the future")
        val nameless = saved(repository, "Lost its name")
        saved(repository, "Fine")
        overwriteDocument(broken, "{ not a document")
        overwriteDocument(newer, "{\"schemaVersion\":99,\"format\":\"single_sheet_8\",\"paperSize\":\"letter\",\"pages\":[]}")
        Files.write(root.resolve("projects/$nameless/meta.json"), "not json".encodeToByteArray())

        val receipt = repository.createLibraryBackup(root.resolve("mixed.zine")).getOrNull()!!

        assertEquals(1, receipt.projectCount)
        assertEquals(
            setOf(
                ZineBackupOmission("Broken", ZineBackupOmission.UNREADABLE),
                ZineBackupOmission("From the future", ZineBackupOmission.NEWER_VERSION),
                // No readable meta.json: named from its shelf row (ADR-122 §4).
                ZineBackupOmission("Lost its name", ZineBackupOmission.UNREADABLE),
            ),
            receipt.omitted.toSet(),
        )
        assertFalse(receipt.omittedOffShelf) // corrupt and newer zines show as unavailable; the nameless one has a row
    }

    @Test
    fun `a left-out zine with no shelf row is flagged off the shelf`() = runTest {
        val seed = repo()
        val hidden = saved(seed, "Hidden", poisonedPhoto())
        db.projectDao().deleteById(hidden)
        val repository = repo(dao = NoRowDao(db.projectDao(), hidden))

        val receipt = repository.createLibraryBackup(root.resolve("hidden.zine")).getOrNull()!!

        assertEquals(listOf(ZineBackupOmission("Hidden", ZineBackupOmission.PHOTO)), receipt.omitted)
        assertEquals(setOf(ZineBackupOmission.PHOTO), receipt.offShelfReasons)
    }

    @Test
    fun `a failure writing the private archive fails the whole backup and leaves nothing out`() = runTest {
        val repository = repo()
        saved(repository, "Fine")
        val notADirectory = root.resolve("occupied").also { Files.write(it, byteArrayOf(1)) }

        val result = repository.createLibraryBackup(notADirectory.resolve("backup.zine"))

        assertTrue(result.errorOrNull() is DataError.Io)
    }

    /** ADR-122 §2: a transient document read is not one bad zine; leaving it out would make the backup silently partial. */
    @Test
    fun `a document the writer can't read fails the whole backup and leaves nothing out`() = runTest {
        val writer = ZineLibraryBackupWriter(
            openSource = { source ->
                if (source.fileName.toString() == "document.json") throw IOException("read failed") else Files.newInputStream(source)
            },
        )
        val repository = repo(backupWriter = writer)
        saved(repository, "One")
        saved(repository, "Two")
        val archive = root.resolve("out").resolve("read-fails.zine")

        val result = repository.createLibraryBackup(archive)

        assertTrue("got $result", result.errorOrNull() is DataError.Io)
        assertFalse(Files.exists(archive))
    }

    /**
     * ADR-122 §5: a backup may list as many left-out zines as the project limit and no more. Past it the whole backup
     * fails as a limit and nothing is written, never a file that restore would read back short. The limit is injected
     * small here; the writer tests cover the real 10,000 boundary.
     */
    @Test
    fun `more left-out zines than a backup can list fails the whole backup as a limit and writes nothing`() = runTest {
        val repository = repo(backupWriter = ZineLibraryBackupWriter(ZineBackupWriteLimits(maximumProjects = 2)))
        saved(repository, "Fine")
        overwriteDocument(saved(repository, "Broken 1"), "{ not a document")
        overwriteDocument(saved(repository, "Broken 2"), "{ not a document")
        val atTheLimit = root.resolve("out").resolve("two-left-out.zine")

        assertEquals(2, repository.createLibraryBackup(atTheLimit).getOrNull()!!.omitted.size)

        overwriteDocument(saved(repository, "Broken 3"), "{ not a document")
        val overTheLimit = root.resolve("out").resolve("three-left-out.zine")

        val result = repository.createLibraryBackup(overTheLimit)

        assertTrue("got $result", result.errorOrNull() is DataError.LimitExceeded)
        assertEquals(listOf(atTheLimit.fileName.toString()), Files.list(overTheLimit.parent).use { it.map { p -> p.fileName.toString() }.toList() })
    }

    @Test
    fun `a changed document the writer catches is rebuilt without it`() = runTest {
        var tampered = false
        val writer = ZineLibraryBackupWriter(
            openSource = { source ->
                if (!tampered && source.toString().contains("changing")) {
                    tampered = true
                    ByteArrayInputStream("{}".encodeToByteArray())
                } else {
                    Files.newInputStream(source)
                }
            },
        )
        documents.save("changing", document())
        Files.write(root.resolve("projects/changing/meta.json"), Json.encodeToString(ProjectMeta.serializer(), ProjectMeta("Changing", 1L)).encodeToByteArray())
        val repository = repo(backupWriter = writer)
        saved(repository, "Steady")

        val receipt = repository.createLibraryBackup(root.resolve("doc.zine")).getOrNull()!!

        assertEquals(listOf(ZineBackupOmission("Changing", ZineBackupOmission.UNREADABLE)), receipt.omitted)
        assertEquals(1, receipt.projectCount)
    }

    // ---- ADR-122 R1-R3 and ADR-121: restore reports what happened -----------------------------------

    @Test
    fun `a full disk while staging is out of space, never a damaged backup`() = runTest {
        Files.write(root.resolve(".library-restore"), byteArrayOf(1)) // staging can't be created here
        val archive = writeArchive(projects = listOf(BackupProjectFixture("a", "A", 1L, 2L, document())))

        val full = repo(usableBytes = { 1_024L }).restoreLibrary(archive).errorOrNull()
        val notFull = repo(usableBytes = { Long.MAX_VALUE }).restoreLibrary(archive).errorOrNull()

        assertTrue("got $full", full is DataError.OutOfSpace)
        assertTrue("got $notFull", notFull is DataError.Unknown)
    }

    /**
     * ADR-122 R1, review RF-1: the disk fills mid-copy. The stager deletes its partial copy before the repository sees
     * the failure, so a probe then would read the freed space; the stager's own measurement, taken first, decides.
     */
    @Test
    fun `a disk that fills mid-copy is out of space although clean-up freed the space`() = runTest {
        val archive = writeArchive(projects = listOf(BackupProjectFixture("a", "A", 1L, 2L, document())))
        fun fillsUp(usableAtFailure: Long) = ZineLibraryBackupStager(
            openStagingFile = { target ->
                object : java.io.OutputStream() {
                    init { Files.createFile(target) }
                    override fun write(b: Int): Unit = throw IOException("No space left on device")
                    override fun write(b: ByteArray, off: Int, len: Int): Unit = throw IOException("No space left on device")
                }
            },
            usableSpace = { usableAtFailure },
        )

        // After clean-up the disk has room again: only the measurement at the failure can tell.
        val full = repo(usableBytes = { Long.MAX_VALUE }, restoreStager = fillsUp(1_024L)).restoreLibrary(archive).errorOrNull()
        val notFull = repo(usableBytes = { 1_024L }, restoreStager = fillsUp(Long.MAX_VALUE)).restoreLibrary(archive).errorOrNull()

        assertTrue("got $full", full is DataError.OutOfSpace)
        assertTrue("got $notFull", notFull is DataError.Unknown)
        assertTrue("nothing written", !Files.exists(root.resolve("projects")) || projectDirectories().isEmpty())
    }

    @Test
    fun `a Cancel that wins before the commit starts adds nothing`() = runTest {
        val archive = writeArchive(projects = listOf(BackupProjectFixture("incoming", "Incoming", 1L, 2L, document())))

        val thrown = runCatching { repo().restoreLibrary(archive) { false } }.exceptionOrNull()

        assertTrue("got $thrown", thrown is kotlinx.coroutines.CancellationException)
        assertFalse(Files.exists(root.resolve("projects/incoming")))
        assertNull(db.projectDao().findById("incoming"))
    }

    @Test
    fun `once the commit starts the restore reports what it added`() = runTest {
        val archive = writeArchive(projects = listOf(BackupProjectFixture("incoming", "Incoming", 1L, 2L, document())))
        var hookCalls = 0

        val receipt = repo().restoreLibrary(archive) { hookCalls++; true }.getOrNull()!!

        assertEquals(1, hookCalls)
        assertEquals(1, receipt.addedCount)
        assertTrue(receipt.shelfUpToDate)
    }

    @Test
    fun `the same backup restored twice adds nothing the second time and never commits`() = runTest {
        val archive = writeArchive(
            projects = listOf(
                BackupProjectFixture("a", "Poems", 1L, 2L, document()),
                BackupProjectFixture("b", "Maps", 1L, 2L, document(sha256("x".encodeToByteArray())).copy(paperSize = PaperSize.A4)),
            ),
            assets = mapOf(sha256("x".encodeToByteArray()) to "x".encodeToByteArray()),
        )
        assertEquals(2, repo().restoreLibrary(archive).getOrNull()!!.addedCount)
        val before = projectDirectories()
        var hookCalls = 0

        val second = repo().restoreLibrary(archive) { hookCalls++; true }.getOrNull()!!

        assertEquals(0, second.addedCount)
        assertEquals(2, second.alreadyHereCount)
        assertTrue(second.shelfUpToDate)
        assertEquals(0, hookCalls)
        assertEquals(before, projectDirectories())
        assertEquals(2, db.projectDao().ids().size)
        assertFalse(Files.exists(root.resolve(".library-restore/pending-library-restore.v1")))
    }

    @Test
    fun `a mixed archive adds only what is new, and a changed version with a clashing id gets a new id`() = runTest {
        val repository = repo()
        val poems = repository.createProject("Poems", ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER).getOrNull()!!.id
        val edited = document(sha256("new".encodeToByteArray()))
        val archive = writeArchive(
            projects = listOf(
                BackupProjectFixture(poems, "Poems", 1L, 2L, document(sha256("new".encodeToByteArray()))), // changed
                BackupProjectFixture("maps", "Maps", 1L, 2L, document()), // new
            ),
            assets = mapOf(sha256("new".encodeToByteArray()) to "new".encodeToByteArray()),
        )
        // The shelf copy of "Poems" is the blank document: a changed version.
        assertTrue(documents.load(poems).getOrNull() != edited)

        val receipt = repository.restoreLibrary(archive).getOrNull()!!

        assertEquals(2, receipt.addedCount)
        assertEquals(0, receipt.alreadyHereCount)
        val poemsCopy = receipt.projects.single { it.sourceProjectId == poems }.project.id
        assertTrue(poemsCopy != poems)
        assertEquals(edited, documents.load(poemsCopy).getOrNull())
    }

    @Test
    fun `equal content in older bytes is already here`() = runTest {
        documents.save("poems", document())
        Files.write(root.resolve("projects/poems/meta.json"), Json.encodeToString(ProjectMeta.serializer(), ProjectMeta("Poems", 1L)).encodeToByteArray())
        // Same content, different bytes: reordered keys and whitespace.
        val archive = writeArchive(
            projects = listOf(BackupProjectFixture("other-id", "Poems", 1L, 2L, document())),
            documentText = { text -> " " + text },
        )

        val receipt = repo().restoreLibrary(archive).getOrNull()!!

        assertEquals(0, receipt.addedCount)
        assertEquals(1, receipt.alreadyHereCount)
    }

    @Test
    fun `an unreadable shelf zine never absorbs a match`() = runTest {
        // Same title, readable meta.json, a Room row, but a document that can't be decoded and bytes that differ:
        // matching must add (doubt adds), never treat it as the backup's zine.
        val repository = repo()
        val shelf = saved(repository, "My zine")
        overwriteDocument(shelf, "{ not a document")
        val archive = writeArchive(projects = listOf(BackupProjectFixture("b", "My zine", 1L, 2L, document())))

        val receipt = repository.restoreLibrary(archive).getOrNull()!!

        assertEquals(1, receipt.addedCount)
        assertEquals(0, receipt.alreadyHereCount)
    }

    @Test
    fun `a partial archive's omissions come back clamped, and a malformed list is ignored`() = runTest {
        val long = "x".repeat(300)
        val archive = writeArchive(
            projects = listOf(BackupProjectFixture("a", "A", 1L, 2L, document())),
            omitted = List(60) { ZineBackupOmission(if (it == 0) long else "Zine $it", ZineBackupOmission.PHOTO) },
        )

        val receipt = repo().restoreLibrary(archive).getOrNull()!!

        assertEquals(60, receipt.omitted.size)
        assertEquals(MAX_OMISSION_TITLE_CHARS, receipt.omitted.first().title!!.length)
        assertEquals("Zine 49", receipt.omitted[49].title)
        assertNull(receipt.omitted[50].title)

        val malformed = writeArchive(
            projects = listOf(BackupProjectFixture("b", "B", 1L, 2L, document())),
            manifestText = { it.dropLast(1) + ",\"omitted\":5}" },
        )
        val restored = repo().restoreLibrary(malformed).getOrNull()!!
        assertEquals(1, restored.addedCount)
        assertTrue(restored.omitted.isEmpty())
    }

    // ---- ADR-125 rule 15: a backup carries folder names --------------------------------------------

    @Test
    fun `a backup carries each zine's folder, and restoring it puts the zines back in them`() = runTest {
        val repository = repo()
        val a = repository.createProject("Lisbon", ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER, "Trips").getOrNull()!!.id
        val b = repository.createProject("Porto", ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER, "Trips").getOrNull()!!.id
        val c = repository.createProject("Loose", ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER).getOrNull()!!.id
        val archive = root.resolve("folders.zine")

        repository.createLibraryBackup(archive).getOrNull()!!

        ZineLibraryBackupStager().stage(archive, root.resolve("verify")).use { staged ->
            assertEquals(2, staged.manifest.packageVersion)
            assertEquals(
                mapOf(a to "Trips", b to "Trips", c to null),
                staged.projects.associate { it.manifestEntry.sourceProjectId to it.manifestEntry.folder },
            )
        }
        listOf(a, b, c).forEach { assertTrue(repository.deleteProject(it).getOrNull() != null) }

        val receipt = repository.restoreLibrary(archive).getOrNull()!!

        assertEquals(3, receipt.addedCount)
        assertEquals(
            mapOf("Lisbon" to "Trips", "Porto" to "Trips", "Loose" to null),
            receipt.projects.associate { it.project.title to metaOnDisk(it.project.id).folder },
        )
        assertEquals(
            mapOf("Lisbon" to "Trips", "Porto" to "Trips", "Loose" to null),
            repository.observeShelfProjects().first().associate { it.title to it.folder },
        )
    }

    @Test
    fun `a restored zine joins the shelf's folder of the same name, in the shelf's spelling`() = runTest {
        val repository = repo()
        repository.createProject("Lisbon", ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER, "Trips").getOrNull()!!
        val archive = writeArchive(
            projects = listOf(
                BackupProjectFixture("porto", "Porto", 1L, 2L, document(), folder = "TRIPS"),
                BackupProjectFixture("moths", "Moths", 1L, 2L, document(), folder = "club"),
                BackupProjectFixture("bats", "Bats", 1L, 2L, document(), folder = "CLUB"),
            ),
        )

        repository.restoreLibrary(archive).getOrNull()!!

        assertEquals("Trips", metaOnDisk("porto").folder)
        // Zines arriving together into a folder the shelf does not have agree on one spelling.
        assertEquals("club", metaOnDisk("moths").folder)
        assertEquals("club", metaOnDisk("bats").folder)
    }

    @Test
    fun `a zine that is already here is not moved by a restore`() = runTest {
        documents.save("poems", document())
        Files.write(
            root.resolve("projects/poems/meta.json"),
            Json.encodeToString(ProjectMeta.serializer(), ProjectMeta("Poems", 1L, folder = "Mine")).encodeToByteArray(),
        )
        documents.save("loose", document())
        Files.write(
            root.resolve("projects/loose/meta.json"),
            Json.encodeToString(ProjectMeta.serializer(), ProjectMeta("Loose", 1L)).encodeToByteArray(),
        )
        val archive = writeArchive(
            projects = listOf(
                BackupProjectFixture("other-id", "Poems", 1L, 2L, document(), folder = "Theirs"),
                BackupProjectFixture("another-id", "Loose", 1L, 2L, document(), folder = "Theirs"),
            ),
        )

        val receipt = repo().restoreLibrary(archive).getOrNull()!!

        assertEquals(0, receipt.addedCount)
        assertEquals(2, receipt.alreadyHereCount)
        assertEquals("Mine", metaOnDisk("poems").folder)
        assertNull(metaOnDisk("loose").folder)
    }

    @Test
    fun `a backup's folder names are cleaned like any other, and a bad one never refuses the restore`() = runTest {
        val archive = writeArchive(
            projects = listOf(
                BackupProjectFixture("a", "Shelf", 1L, 2L, document(), folder = " my shelf "),
                BackupProjectFixture("b", "Long", 1L, 2L, document(), folder = "x".repeat(60) + "\n"),
                BackupProjectFixture("c", "Typed", 1L, 2L, document(), folder = "REPLACED"),
            ),
            manifestText = { text ->
                assertTrue(text.contains(""""folder":"REPLACED""""))
                text.replace(""""folder":"REPLACED"""", """"folder":{"name":"Trips"},"fromAFutureZinely":[1]""")
            },
        )

        val receipt = repo().restoreLibrary(archive).getOrNull()!!

        assertEquals(3, receipt.addedCount)
        assertNull(metaOnDisk("a").folder)
        assertEquals("x".repeat(40), metaOnDisk("b").folder)
        assertNull(metaOnDisk("c").folder)
    }

    private suspend fun saved(repository: RoomProjectRepository, title: String, photoHash: String? = null): String {
        val id = repository.createProject(title, ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER).getOrNull()!!.id
        assertTrue(documents.save(id, document(photoHash)).getOrNull() != null)
        return id
    }

    /** A photo file whose bytes hash to its name; returns the hash. */
    private fun photo(content: String): String {
        val bytes = content.encodeToByteArray()
        val hash = sha256(bytes)
        Files.createDirectories(root.resolve("assets"))
        Files.write(root.resolve("assets").resolve(hash), bytes)
        return hash
    }

    /** A photo file whose bytes do not hash to its name. */
    private fun poisonedPhoto(): String {
        val hash = sha256("expected".encodeToByteArray())
        Files.createDirectories(root.resolve("assets"))
        Files.write(root.resolve("assets").resolve(hash), "different".encodeToByteArray())
        return hash
    }

    /** Replaces a zine's document and removes any backup copy, so the load can't recover it. */
    private fun overwriteDocument(id: String, text: String) {
        val dir = root.resolve("projects").resolve(id)
        Files.list(dir).use { files -> files.filter { it.fileName.toString().startsWith("document.json.") }.toList() }
            .forEach(Files::delete)
        Files.write(dir.resolve("document.json"), text.encodeToByteArray())
    }

    private fun projectDirectories(): Set<String> =
        Files.list(root.resolve("projects")).use { dirs -> dirs.map { it.fileName.toString() }.toList().toSet() }

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
    fun `future backup version is reported distinctly and leaves the library untouched`() = runTest {
        val repository = repo()
        val existing = repository.createProject("Keep", ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER).getOrNull()!!.id
        val archive = writeArchive(
            projects = listOf(BackupProjectFixture("future", "From the future", 1L, 2L, document())),
            packageVersion = CURRENT_LIBRARY_BACKUP_VERSION + 1,
        )

        val result = repository.restoreLibrary(archive)

        val error = result.errorOrNull()
        assertTrue("expected SchemaTooNew, got $error", error is DataError.SchemaTooNew)
        assertEquals(CURRENT_LIBRARY_BACKUP_VERSION + 1, (error as DataError.SchemaTooNew).documentVersion)
        assertEquals(CURRENT_LIBRARY_BACKUP_VERSION, error.supportedVersion)
        assertEquals(listOf(existing), repository.observeProjects().first().map { it.id })
        assertFalse(Files.exists(root.resolve("projects").resolve("future")))
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

        val lagging = repo(dao = failingDao).restoreLibrary(archive).getOrNull()!!

        // R3: the zine is committed, so the restore succeeded; only the shelf index lags.
        assertEquals(1, lagging.addedCount)
        assertFalse(lagging.shelfUpToDate)
        assertTrue(lagging.projects.isEmpty())
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
        packageVersion: Int = CURRENT_LIBRARY_BACKUP_VERSION,
        omitted: List<ZineBackupOmission> = emptyList(),
        manifestText: (String) -> String = { it },
        documentText: (String) -> String = { it },
    ): Path {
        val serializer = JsonDocumentSerializer()
        val documentsById = projects.associate {
            it.sourceProjectId to documentText(serializer.serialize(it.document)).encodeToByteArray()
        }
        val manifest = ZineLibraryBackupManifest(
            packageVersion = packageVersion,
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
                    folder = project.folder,
                )
            },
            assets = assets.map { (hash, bytes) -> AssetEntry(hash, "image/jpeg", 32, 32, bytes.size.toLong()) },
            omitted = omitted,
        )
        val archive = Files.createTempFile(tmp.newFolder().toPath(), "restore-", ".zine")
        val entries = linkedMapOf<String, ByteArray>()
        entries["manifest.json"] =
            manifestText(Json.encodeToString(ZineLibraryBackupManifest.serializer(), manifest)).encodeToByteArray()
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
        val folder: String? = null,
    )

    /** A shelf index that never holds a row for [hiddenId]: the zine exists only on disk. */
    private class NoRowDao(private val delegate: ProjectDao, private val hiddenId: String) : ProjectDao by delegate {
        override suspend fun findById(id: String): ProjectEntity? = if (id == hiddenId) null else delegate.findById(id)
        override suspend fun upsert(project: ProjectEntity) {
            if (project.id == hiddenId) throw IOException("injected: never indexed") else delegate.upsert(project)
        }
    }

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
