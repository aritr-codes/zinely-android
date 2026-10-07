package com.aritr.zinely.data.android

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.DocumentRepository
import com.aritr.zinely.core.data.repository.FolderChange
import com.aritr.zinely.core.data.repository.ProjectShelfEntry
import com.aritr.zinely.core.data.repository.errorOrNull
import com.aritr.zinely.core.data.repository.getOrNull
import com.aritr.zinely.core.data.storage.AtomicFileStore
import com.aritr.zinely.core.data.storage.FileSystemOps
import com.aritr.zinely.core.data.storage.NioFileSystemOps
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.data.android.room.ProjectDao
import com.aritr.zinely.data.android.room.ProjectEntity
import com.aritr.zinely.data.android.room.ZinelyDatabase
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertArrayEquals
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

/**
 * Shelf folders in storage ([ADR-125](docs/DECISIONS.md#adr-125) rules 12 to 18). A folder is the name its
 * zines carry in `meta.json`, so these tests read that file, and the index, directly: what the repository
 * returns is a claim, the files are the fact. Same harness as [RoomProjectRepositoryTest].
 */
@RunWith(RobolectricTestRunner::class)
class RoomProjectRepositoryFoldersTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var root: Path
    private lateinit var store: AtomicFileStore
    private lateinit var documents: DocumentRepositoryImpl
    private lateinit var db: ZinelyDatabase

    private var now = 1_000L
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
        store: AtomicFileStore = this.store,
        dao: ProjectDao = db.projectDao(),
        documents: DocumentRepository = this.documents,
        sessionGate: ProjectSessionGate = ProjectSessionGate { true },
    ): RoomProjectRepository = RoomProjectRepository(
        rootDir = root,
        dao = dao,
        documents = documents,
        store = store,
        sessionGate = sessionGate,
        io = Dispatchers.Unconfined,
        clock = { now },
        newId = { "p${nextId++}" },
    )

    // ---- every writer of meta.json keeps the folder (rule 12) --------------------------------------

    @Test
    fun `a zine made in a folder carries it from its first metadata, and the shelf says so`() = runTest {
        val repo = repo()

        val made = repo.createProject("Lisbon", ZineFormat.SINGLE_SHEET_8, PaperSize.A4, folder = "  Trips\n").getOrNull()!!

        assertEquals("Trips", metaOnDisk(made.id).folder)
        val entry = repo.observeShelfProjects().first().single() as ProjectShelfEntry.Available
        assertEquals("Trips", entry.folder)
        assertEquals(made.id, entry.id)
    }

    @Test
    fun `a zine made in an existing folder takes that folder's spelling`() = runTest {
        val repo = repo()
        create(repo, "Lisbon", "Trips")

        val second = create(repo, "Porto", "TRIPS")

        assertEquals("Trips", metaOnDisk(second).folder)
    }

    @Test
    fun `a zine made on My Shelf has the same metadata bytes as before folders existed`() = runTest {
        val repo = repo()

        val plain = create(repo, "Plain", null)
        val blank = create(repo, "Blank", "   ")
        val shelf = create(repo, "Shelf", "my shelf")

        listOf(plain, blank, shelf).forEach { id ->
            assertNull(metaOnDisk(id).folder)
            assertFalse(Files.readString(metaFile(id)).contains("folder"))
        }
        // The shape a build without the field wrote, byte for byte.
        assertEquals(
            """{"title":"T","createdAtEpochMs":1,"coverSurface":"S","coverStamp":"C"}""",
            Json.encodeToString(ProjectMeta.serializer(), ProjectMeta("T", 1L, "S", "C")),
        )
        assertEquals(
            """{"title":"T","createdAtEpochMs":1}""",
            Json.encodeToString(ProjectMeta.serializer(), ProjectMeta("T", 1L)),
        )
    }

    @Test
    fun `renaming a zine does not take it out of its folder`() = runTest {
        val repo = repo()
        val id = create(repo, "Lisbon", "Trips")

        assertTrue(repo.renameProject(id, "Lisbon, again") is DataResult.Success)

        assertEquals("Lisbon, again", metaOnDisk(id).title)
        assertEquals("Trips", metaOnDisk(id).folder)
    }

    @Test
    fun `a copy is put beside its original`() = runTest {
        val repo = repo()
        val inFolder = create(repo, "Lisbon", "Trips")
        val onShelf = create(repo, "Loose", null)

        val copyInFolder = repo.duplicateProject(inFolder).getOrNull()!!.id
        val copyOnShelf = repo.duplicateProject(onShelf).getOrNull()!!.id

        assertEquals("Trips", metaOnDisk(copyInFolder).folder)
        assertNull(metaOnDisk(copyOnShelf).folder)
    }

    @Test
    fun `giving an old zine its cover keeps its folder`() = runTest {
        // A sidecar with a folder but no cover: the legacy cover backfill rewrites it on first presentation.
        documents.save("old", validDoc())
        Files.write(metaFile("old"), """{"title":"Old","createdAtEpochMs":5,"folder":"Trips"}""".toByteArray())

        repo().observeProjects().first()

        val meta = metaOnDisk("old")
        assertTrue(meta.coverSurface != null && meta.coverStamp != null)
        assertEquals("Trips", meta.folder)
    }

    // ---- reading a name the app did not write (rule 13) --------------------------------------------

    @Test
    fun `a folder value that is not text is My Shelf, and the rest of the file still reads`() = runTest {
        documents.save("odd", validDoc())
        Files.write(
            metaFile("odd"),
            """{"title":"Kept","createdAtEpochMs":5,"coverSurface":"x","coverStamp":"y","folder":{"name":"Trips"}}""".toByteArray(),
        )

        val entry = repo().observeShelfProjects().first().single()

        assertEquals("Kept", entry.title)
        assertNull(entry.folder)
    }

    @Test
    fun `a hand-edited name is cleaned before the shelf sees it`() = runTest {
        handWritten("a", """"  Trips\n"""")
        handWritten("b", """"MY SHELF"""")
        handWritten("c", """"${"x".repeat(60)}"""")

        val shelf = repo().observeShelfProjects().first().associate { it.id to it.folder }

        assertEquals(mapOf("a" to "Trips", "b" to null, "c" to "x".repeat(40)), shelf)
    }

    @Test
    fun `a zine that will not open still shows in its folder`() = runTest {
        val repo = repo()
        val id = create(repo, "Lisbon", "Trips")
        Files.write(documentFile(id), "{ not a document".toByteArray())

        val entry = repo.observeShelfProjects().first().single() as ProjectShelfEntry.Unavailable

        assertEquals("Trips", entry.folder)
    }

    // ---- moving one zine (rule 17) -----------------------------------------------------------------

    @Test
    fun `moving a zine writes its metadata and nothing else`() = runTest {
        val dao = WriteCountingDao(db.projectDao())
        val loads = LoadCountingDocuments(documents)
        val repo = repo(dao = dao, documents = loads)
        val older = create(repo, "Older", null)
        now = 2_000L
        val newer = create(repo, "Newer", null)
        repo.observeProjects().first()
        val documentBytes = Files.readAllBytes(documentFile(older))
        val documentTime = Files.getLastModifiedTime(documentFile(older))
        val rowBefore = db.projectDao().findById(older)
        val orderBefore = repo.observeShelfProjects().first().map { it.id }
        dao.writes = 0
        loads.loads = 0
        now = 9_000L

        assertTrue(repo.moveProject(older, "Trips") is DataResult.Success)

        assertEquals("Trips", metaOnDisk(older).folder)
        assertEquals(0, dao.writes) // the index is not written
        assertEquals(0, loads.loads) // the document is not opened
        assertArrayEquals(documentBytes, Files.readAllBytes(documentFile(older)))
        assertEquals(documentTime, Files.getLastModifiedTime(documentFile(older)))
        assertEquals(rowBefore, db.projectDao().findById(older)) // not made "newer"
        assertEquals(listOf(newer, older), orderBefore)
        assertEquals(orderBefore, repo.observeShelfProjects().first().map { it.id })
        assertEquals(mapOf(older to "Trips", newer to null), shelfFolders(repo))
    }

    @Test
    fun `moving a zine into a folder takes its spelling, and moving it out leaves no trace`() = runTest {
        val repo = repo()
        create(repo, "Lisbon", "Trips")
        val loose = create(repo, "Porto", null)

        assertTrue(repo.moveProject(loose, "tRIPS") is DataResult.Success)
        assertEquals("Trips", metaOnDisk(loose).folder)

        assertTrue(repo.moveProject(loose, "My Shelf") is DataResult.Success)
        assertNull(metaOnDisk(loose).folder)
        assertFalse(Files.readString(metaFile(loose)).contains("folder"))

        assertTrue(repo.moveProject(loose, "Trips") is DataResult.Success)
        assertTrue(repo.moveProject(loose, null) is DataResult.Success)
        assertNull(metaOnDisk(loose).folder)
    }

    @Test
    fun `a folder's only zine can change the folder's capitals`() = runTest {
        val repo = repo()
        val id = create(repo, "Lisbon", "Trips")

        assertTrue(repo.moveProject(id, "TRIPS") is DataResult.Success)

        assertEquals("TRIPS", metaOnDisk(id).folder)
    }

    @Test
    fun `moving a zine to where it already is changes no byte`() = runTest {
        val repo = repo()
        val id = create(repo, "Lisbon", "Trips")
        val before = Files.readAllBytes(metaFile(id))
        val time = Files.getLastModifiedTime(metaFile(id))

        assertTrue(repo.moveProject(id, "Trips") is DataResult.Success)

        assertArrayEquals(before, Files.readAllBytes(metaFile(id)))
        assertEquals(time, Files.getLastModifiedTime(metaFile(id)))
    }

    @Test
    fun `a zine that will not open can still be moved`() = runTest {
        val repo = repo()
        val id = create(repo, "Lisbon", null)
        val garbage = "{ not a document".toByteArray()
        Files.write(documentFile(id), garbage)

        assertTrue(repo.moveProject(id, "Trips") is DataResult.Success)

        assertEquals("Trips", metaOnDisk(id).folder)
        assertArrayEquals(garbage, Files.readAllBytes(documentFile(id)))
    }

    @Test
    fun `a zine with missing or unreadable metadata is not moved, and nothing is written over`() = runTest {
        documents.save("bare", validDoc())
        documents.save("torn", validDoc())
        val garbage = "not json".toByteArray()
        Files.write(metaFile("torn"), garbage)
        val repo = repo()

        val bare = repo.moveProject("bare", "Trips")
        val torn = repo.moveProject("torn", "Trips")

        assertTrue(bare.errorOrNull() is DataError.Corrupt)
        assertTrue(torn.errorOrNull() is DataError.Corrupt)
        assertFalse(Files.exists(metaFile("bare")))
        assertArrayEquals(garbage, Files.readAllBytes(metaFile("torn")))
    }

    @Test
    fun `moving a zine that does not exist is not found`() = runTest {
        assertTrue(repo().moveProject("nobody", "Trips").errorOrNull() is DataError.NotFound)
        assertFalse(Files.exists(root.resolve("projects/nobody")))
    }

    @Test
    fun `a zine being edited is not moved`() = runTest {
        val id = create(repo(), "Lisbon", null)
        val gate = RecordingGate(open = false)

        val result = repo(sessionGate = gate).moveProject(id, "Trips")

        assertTrue(result.errorOrNull() is DataError.Busy)
        assertEquals(listOf(id), gate.askedIds)
        assertNull(metaOnDisk(id).folder)
    }

    @Test
    fun `a move that cannot be written fails and leaves the zine where it was`() = runTest {
        val id = create(repo(), "Lisbon", "Trips")

        val result = repo(store = metaWriteFailingStore { true }).moveProject(id, "Journeys")

        assertTrue(result.errorOrNull() is DataError.Io)
        assertEquals("Trips", metaOnDisk(id).folder)
    }

    // ---- renaming and unpacking a folder (rules 17 and 18) -----------------------------------------

    @Test
    fun `renaming a folder renames it for every zine in it and for no other`() = runTest {
        val dao = WriteCountingDao(db.projectDao())
        val loads = LoadCountingDocuments(documents)
        val repo = repo(dao = dao, documents = loads)
        val a = create(repo, "Lisbon", "Trips")
        val b = create(repo, "Porto", "Trips")
        val other = create(repo, "Moths", "Club")
        val loose = create(repo, "Loose", null)
        repo.observeProjects().first()
        val rows = listOf(a, b, other, loose).map { db.projectDao().findById(it) }
        dao.writes = 0
        loads.loads = 0
        now = 9_000L

        val change = repo.renameFolder("trips", "Journeys").getOrNull()!!

        assertEquals(FolderChange("Journeys", listOf(a, b)), change)
        assertTrue(change.complete)
        assertEquals(listOf("Journeys", "Journeys", "Club", null), listOf(a, b, other, loose).map { metaOnDisk(it).folder })
        assertEquals(0, dao.writes)
        assertEquals(0, loads.loads)
        assertEquals(rows, listOf(a, b, other, loose).map { db.projectDao().findById(it) })
    }

    @Test
    fun `renaming a folder can change only its capitals`() = runTest {
        val repo = repo()
        val a = create(repo, "Lisbon", "Trips")
        val b = create(repo, "Porto", "Trips")

        val change = repo.renameFolder("Trips", "TRIPS").getOrNull()!!

        assertEquals(FolderChange("TRIPS", listOf(a, b)), change)
        assertEquals(listOf("TRIPS", "TRIPS"), listOf(a, b).map { metaOnDisk(it).folder })
    }

    @Test
    fun `zines renamed into another folder's name join it in its spelling`() = runTest {
        val repo = repo()
        val a = create(repo, "Lisbon", "Trips")
        val kept = create(repo, "Moths", "Club")

        val change = repo.renameFolder("Trips", "CLUB").getOrNull()!!

        assertEquals(FolderChange("Club", listOf(a)), change)
        assertEquals(listOf("Club", "Club"), listOf(a, kept).map { metaOnDisk(it).folder })
    }

    @Test
    fun `a folder cannot be renamed to nothing or to My Shelf`() = runTest {
        val repo = repo()
        val a = create(repo, "Lisbon", "Trips")

        assertTrue(repo.renameFolder("Trips", "   ").errorOrNull() is DataError.Invalid)
        assertTrue(repo.renameFolder("Trips", "my shelf").errorOrNull() is DataError.Invalid)

        assertEquals("Trips", metaOnDisk(a).folder)
    }

    @Test
    fun `renaming or unpacking a folder no zine is in does nothing and succeeds`() = runTest {
        val repo = repo()
        val a = create(repo, "Lisbon", "Trips")

        assertEquals(FolderChange("Journeys", emptyList()), repo.renameFolder("Nowhere", "Journeys").getOrNull())
        assertEquals(FolderChange(null, emptyList()), repo.unpackFolder("Nowhere").getOrNull())
        assertEquals(FolderChange(null, emptyList()), repo.unpackFolder("My Shelf").getOrNull())

        assertEquals("Trips", metaOnDisk(a).folder)
    }

    @Test
    fun `unpacking a folder puts its zines on My Shelf and leaves the others`() = runTest {
        val repo = repo()
        val a = create(repo, "Lisbon", "Trips")
        val b = create(repo, "Porto", "Trips")
        val other = create(repo, "Moths", "Club")

        val change = repo.unpackFolder("TRIPS").getOrNull()!!

        assertEquals(FolderChange(null, listOf(a, b)), change)
        assertEquals(listOf(null, null, "Club"), listOf(a, b, other).map { metaOnDisk(it).folder })
        assertFalse(Files.readString(metaFile(a)).contains("folder"))
        assertEquals(setOf("Club", null), shelfFolders(repo).values.toSet())
    }

    @Test
    fun `a rename that stops partway says which zines are left, and running it again finishes it`() = runTest {
        val good = repo()
        val a = create(good, "Lisbon", "Trips")
        val b = create(good, "Porto", "Trips")
        val c = create(good, "Faro", "Trips")

        val partial = repo(store = metaWriteFailingStore { it == b }).renameFolder("Trips", "Journeys").getOrNull()!!

        assertEquals(FolderChange("Journeys", changedIds = listOf(a, c), failedIds = listOf(b)), partial)
        assertFalse(partial.complete)
        assertEquals(listOf("Journeys", "Trips", "Journeys"), listOf(a, b, c).map { metaOnDisk(it).folder })

        val finished = good.renameFolder("Trips", "Journeys").getOrNull()!!

        assertEquals(FolderChange("Journeys", listOf(b)), finished)
        assertEquals(listOf("Journeys", "Journeys", "Journeys"), listOf(a, b, c).map { metaOnDisk(it).folder })
    }

    @Test
    fun `an unpack that stops partway is finished by running it again`() = runTest {
        val good = repo()
        val a = create(good, "Lisbon", "Trips")
        val b = create(good, "Porto", "Trips")

        val partial = repo(store = metaWriteFailingStore { it == a }).unpackFolder("Trips").getOrNull()!!

        assertEquals(FolderChange(null, changedIds = listOf(b), failedIds = listOf(a)), partial)
        assertEquals(listOf("Trips", null), listOf(a, b).map { metaOnDisk(it).folder })

        assertEquals(FolderChange(null, listOf(a)), good.unpackFolder("Trips").getOrNull())
        assertEquals(listOf(null, null), listOf(a, b).map { metaOnDisk(it).folder })
    }

    @Test
    fun `a folder with a zine being edited is not renamed or unpacked at all`() = runTest {
        val good = repo()
        val a = create(good, "Lisbon", "Trips")
        val b = create(good, "Porto", "Trips")
        create(good, "Moths", "Club")
        val gate = RecordingGate(open = false)
        val busy = repo(sessionGate = gate)

        assertTrue(busy.renameFolder("Trips", "Journeys").errorOrNull() is DataError.Busy)
        assertTrue(busy.unpackFolder("Trips").errorOrNull() is DataError.Busy)

        assertEquals(listOf("Trips", "Trips"), listOf(a, b).map { metaOnDisk(it).folder })
        assertTrue(gate.askedIds.all { it == a || it == b }) // only the folder's own zines are waited for

        val open = RecordingGate(open = true)
        assertTrue(repo(sessionGate = open).renameFolder("Trips", "Journeys") is DataResult.Success)
        assertEquals(listOf(a, b), open.askedIds)
    }

    // ---- the Shelf lists again after a folder operation (rule 14) ----------------------------------

    @Test
    fun `the shelf lists again after each folder operation, though the index was not written`() = runBlocking {
        val dao = WriteCountingDao(db.projectDao())
        val repo = repo(dao = dao)
        val a = create(repo, "Lisbon", null)
        val b = create(repo, "Porto", null)
        val seen = Channel<Map<String, String?>>(Channel.UNLIMITED)
        val watching = launch(Dispatchers.Default) {
            repo.observeShelfProjects().collect { shelf -> seen.send(shelf.associate { it.id to it.folder }) }
        }
        suspend fun shelfBecomes(expected: Map<String, String?>) = withTimeout(10_000) {
            while (seen.receive() != expected) Unit
        }
        try {
            shelfBecomes(mapOf(a to null, b to null))
            dao.writes = 0

            repo.moveProject(a, "Trips")
            shelfBecomes(mapOf(a to "Trips", b to null))

            repo.moveProject(b, "Trips")
            shelfBecomes(mapOf(a to "Trips", b to "Trips"))

            repo.renameFolder("Trips", "Journeys")
            shelfBecomes(mapOf(a to "Journeys", b to "Journeys"))

            repo.unpackFolder("Journeys")
            shelfBecomes(mapOf(a to null, b to null))

            assertEquals(0, dao.writes)
        } finally {
            watching.cancel()
        }
    }

    // ---- helpers ---------------------------------------------------------------------------------

    private suspend fun create(repo: RoomProjectRepository, title: String, folder: String?): String =
        repo.createProject(title, ZineFormat.SINGLE_SHEET_8, PaperSize.A4, folder).getOrNull()!!.id

    private suspend fun shelfFolders(repo: RoomProjectRepository): Map<String, String?> =
        repo.observeShelfProjects().first().associate { it.id to it.folder }

    /** A zine whose `meta.json` was written by hand; [folderJson] is the raw JSON value of its folder. */
    private suspend fun handWritten(id: String, folderJson: String) {
        documents.save(id, validDoc())
        Files.write(
            metaFile(id),
            """{"title":"$id","createdAtEpochMs":5,"coverSurface":"x","coverStamp":"y","folder":$folderJson}""".toByteArray(),
        )
    }

    private fun metaOnDisk(id: String): ProjectMeta =
        Json.decodeFromString(ProjectMeta.serializer(), Files.readString(metaFile(id)))

    private fun validDoc(): ZineDocument = ZineDocument(
        format = ZineFormat.SINGLE_SHEET_8,
        paperSize = PaperSize.LETTER,
        pages = (0 until ZineFormat.SINGLE_SHEET_8.pageCount).map { Page(index = it, role = PageRole.INTERIOR) },
    )

    private fun documentFile(id: String): Path = root.resolve("projects/$id/document.json")
    private fun metaFile(id: String): Path = root.resolve("projects/$id/meta.json")

    private class RecordingGate(private val open: Boolean) : ProjectSessionGate {
        val askedIds = mutableListOf<String>()
        override suspend fun awaitNoSession(projectId: String): Boolean {
            askedIds += projectId
            return open
        }
    }

    /** A store that cannot commit the `meta.json` of the zines [fails] picks; everything else is real. */
    private fun metaWriteFailingStore(fails: (projectId: String) -> Boolean): AtomicFileStore = AtomicFileStore(
        object : FileSystemOps by NioFileSystemOps {
            override fun atomicReplace(source: Path, replacing: Path) {
                if (replacing.fileName.toString() == "meta.json" && fails(replacing.parent.fileName.toString())) {
                    throw IOException("meta write blocked")
                }
                NioFileSystemOps.atomicReplace(source, replacing)
            }
        },
    )

    /** Counts every write to the index; reads go straight through. */
    private class WriteCountingDao(private val delegate: ProjectDao) : ProjectDao by delegate {
        var writes = 0
        override suspend fun upsert(project: ProjectEntity) {
            writes++
            delegate.upsert(project)
        }

        override suspend fun deleteById(id: String) {
            writes++
            delegate.deleteById(id)
        }
    }

    /** Counts every time a document is opened. */
    private class LoadCountingDocuments(private val delegate: DocumentRepository) : DocumentRepository by delegate {
        var loads = 0
        override suspend fun load(projectId: String): DataResult<ZineDocument> {
            loads++
            return delegate.load(projectId)
        }
    }
}
