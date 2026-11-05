package com.aritr.zinely.data.android

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.DocumentRepository
import com.aritr.zinely.core.data.repository.PaperColour
import com.aritr.zinely.core.data.repository.ProjectShelfEntry
import com.aritr.zinely.core.data.repository.errorOrNull
import com.aritr.zinely.core.data.repository.getOrNull
import com.aritr.zinely.core.data.storage.AtomicFileStore
import com.aritr.zinely.core.data.storage.ZineLibraryBackupStager
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.data.android.room.ProjectDao
import com.aritr.zinely.data.android.room.ProjectEntity
import com.aritr.zinely.data.android.room.ZinelyDatabase
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
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
 * Paper colour in `meta.json` ([ADR-150](docs/DECISIONS.md#adr-150) decision 3). The files are the fact: these
 * tests read `meta.json` and the index directly. Same harness as [RoomProjectRepositoryFoldersTest].
 */
@RunWith(RobolectricTestRunner::class)
class RoomProjectRepositoryPaperColourTest {

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
        appVersion = "test-version",
    )

    @Test
    fun `every id round-trips through meta json, and White writes no key`() = runTest {
        val repo = repo()
        val id = create(repo)
        assertFalse(Files.readString(metaFile(id)).contains("paperColour")) // created on White

        PaperColour.entries.forEach { colour ->
            assertTrue(repo.setPaperColour(id, colour) is DataResult.Success)
            assertEquals(colour.storedId, metaOnDisk(id).paperColour)
            assertEquals(colour, repo.getPaperColour(id))
            assertEquals(colour, shelfEntry(repo, id).paperColour)
        }
        assertTrue(Files.readString(metaFile(id)).contains("\"paperColour\":\"grey\""))
        assertTrue(repo.setPaperColour(id, PaperColour.WHITE) is DataResult.Success)
        assertFalse(Files.readString(metaFile(id)).contains("paperColour")) // White removed it
        assertEquals(
            """{"title":"T","createdAtEpochMs":1}""",
            Json.encodeToString(ProjectMeta.serializer(), ProjectMeta("T", 1L)),
        )
    }

    @Test
    fun `choosing a paper is not an edit - meta json only, recency and order untouched`() = runTest {
        val dao = WriteCountingDao(db.projectDao())
        val loads = LoadCountingDocuments(documents)
        val repo = repo(dao = dao, documents = loads)
        val older = create(repo)
        now = 2_000L
        val newer = create(repo)
        val orderBefore = repo.observeShelfProjects().first().map { it.id }
        val documentBytes = Files.readAllBytes(documentFile(older))
        val documentTime = Files.getLastModifiedTime(documentFile(older))
        val rowBefore = db.projectDao().findById(older)
        dao.writes = 0
        loads.loads = 0
        now = 9_000L

        assertTrue(repo.setPaperColour(older, PaperColour.PASTEL_BLUE) is DataResult.Success)

        assertEquals("pastel-blue", metaOnDisk(older).paperColour)
        assertEquals(0, dao.writes)
        assertEquals(0, loads.loads)
        assertArrayEquals(documentBytes, Files.readAllBytes(documentFile(older)))
        assertEquals(documentTime, Files.getLastModifiedTime(documentFile(older)))
        assertEquals(rowBefore, db.projectDao().findById(older))
        assertEquals(listOf(newer, older), orderBefore)
        assertEquals(orderBefore, repo.observeShelfProjects().first().map { it.id })
    }

    @Test
    fun `the paper can be chosen while the zine is open, from Print details`() = runTest {
        val repo = repo(sessionGate = ProjectSessionGate { false })
        documents.save("open", validDoc())
        Files.write(metaFile("open"), """{"title":"Open","createdAtEpochMs":5,"coverSurface":"x","coverStamp":"y"}""".toByteArray())

        assertTrue(repo.setPaperColour("open", PaperColour.IVORY) is DataResult.Success)
        assertEquals("ivory", metaOnDisk("open").paperColour)
    }

    @Test
    fun `no zine or unreadable metadata is refused and nothing is written`() = runTest {
        val repo = repo()
        assertTrue(repo.setPaperColour("missing", PaperColour.GREY).errorOrNull() is DataError.NotFound)
        documents.save("bad", validDoc())
        Files.write(metaFile("bad"), "{ not json".toByteArray())
        assertTrue(repo.setPaperColour("bad", PaperColour.GREY).errorOrNull() is DataError.Corrupt)
        assertEquals("{ not json", Files.readString(metaFile("bad")))
        assertEquals(PaperColour.WHITE, repo.getPaperColour("bad"))
    }

    @Test
    fun `an unknown id is kept on disk by every writer and drawn and named as White`() = runTest {
        documents.save("later", validDoc())
        Files.write(
            metaFile("later"),
            """{"title":"Later","createdAtEpochMs":5,"folder":"Trips","paperColour":"kraft"}""".toByteArray(),
        )
        val repo = repo()

        // The legacy cover backfill (`withCover` / `.copy`) rewrites it on first presentation.
        assertEquals(PaperColour.WHITE, shelfEntry(repo, "later").paperColour)
        assertEquals("kraft", metaOnDisk("later").paperColour)
        assertTrue(metaOnDisk("later").coverSurface != null)

        assertTrue(repo.renameProject("later", "Later still") is DataResult.Success)
        assertEquals("kraft", metaOnDisk("later").paperColour)
        assertTrue(repo.moveProject("later", "Elsewhere") is DataResult.Success)
        assertEquals("kraft", metaOnDisk("later").paperColour)
        assertTrue(repo.renameFolder("Elsewhere", "Away") is DataResult.Success)
        assertEquals("kraft", metaOnDisk("later").paperColour)
        assertTrue(repo.unpackFolder("Away") is DataResult.Success)
        assertEquals("kraft", metaOnDisk("later").paperColour)
        val copy = repo.duplicateProject("later").getOrNull()!!.id
        assertEquals("kraft", metaOnDisk(copy).paperColour)
        assertEquals(PaperColour.WHITE, repo.getPaperColour("later"))
    }

    @Test
    fun `a non-string paper colour is White and the rest of the file still reads`() = runTest {
        documents.save("odd", validDoc())
        Files.write(
            metaFile("odd"),
            """{"title":"Kept","createdAtEpochMs":5,"coverSurface":"x","coverStamp":"y","paperColour":{"id":"ivory"}}""".toByteArray(),
        )
        val entry = repo().observeShelfProjects().first().single()
        assertEquals("Kept", entry.title)
        assertEquals(PaperColour.WHITE, entry.paperColour)
    }

    @Test
    fun `a duplicate keeps its original's paper and a zine that will not open still shows it`() = runTest {
        val repo = repo()
        val id = create(repo)
        repo.setPaperColour(id, PaperColour.PASTEL_PINK)
        val copy = repo.duplicateProject(id).getOrNull()!!.id
        assertEquals("pastel-pink", metaOnDisk(copy).paperColour)

        Files.write(documentFile(id), "{ not a document".toByteArray())
        val entry = repo.observeShelfProjects().first().first { it.id == id } as ProjectShelfEntry.Unavailable
        assertEquals(PaperColour.PASTEL_PINK, entry.paperColour)
    }

    @Test
    fun `a backup carries the paper colour and the sheet, and restore puts them back`() = runTest {
        val repo = repo()
        val legal = repo.createProject("Legal", ZineFormat.SINGLE_SHEET_8, PaperSize.LEGAL).getOrNull()!!.id
        val plain = repo.createProject("Plain", ZineFormat.SINGLE_SHEET_8, PaperSize.A4).getOrNull()!!.id
        repo.setPaperColour(legal, PaperColour.PASTEL_YELLOW)
        val archive = root.resolve("library.zine")
        assertTrue(repo.createLibraryBackup(archive) is DataResult.Success)

        ZineLibraryBackupStager().stage(archive, root.resolve("verify-stage")).use { staged ->
            val entries = staged.manifest.projects.associateBy { it.sourceProjectId }
            assertEquals(PaperSize.LETTER, entries.getValue(legal).paperSize)
            assertEquals("legal", entries.getValue(legal).sheet)
            assertEquals("pastel-yellow", entries.getValue(legal).paperColour)
            assertNull(entries.getValue(plain).sheet)
            assertNull(entries.getValue(plain).paperColour)
            assertEquals(5, entries.getValue(legal).documentSchemaVersion)
            assertEquals(3, entries.getValue(plain).documentSchemaVersion)
        }

        assertTrue(repo.deleteProject(legal) is DataResult.Success)
        assertTrue(repo.deleteProject(plain) is DataResult.Success)
        assertTrue(repo.restoreLibrary(archive) { true } is DataResult.Success)

        val shelf = repo.observeShelfProjects().first().associateBy { it.title }
        assertEquals(PaperColour.PASTEL_YELLOW, shelf.getValue("Legal").paperColour)
        assertEquals(PaperSize.LEGAL, shelf.getValue("Legal").paperSize)
        assertEquals(PaperColour.WHITE, shelf.getValue("Plain").paperColour)
        assertFalse(Files.readString(metaFile(shelf.getValue("Plain").id)).contains("paperColour"))
    }

    // ---- helpers ---------------------------------------------------------------------------------

    private suspend fun create(repo: RoomProjectRepository): String =
        repo.createProject("Zine", ZineFormat.SINGLE_SHEET_8, PaperSize.A4).getOrNull()!!.id

    private suspend fun shelfEntry(repo: RoomProjectRepository, id: String): ProjectShelfEntry =
        repo.observeShelfProjects().first().first { it.id == id }

    private fun metaOnDisk(id: String): ProjectMeta =
        Json.decodeFromString(ProjectMeta.serializer(), Files.readString(metaFile(id)))

    private fun validDoc(): ZineDocument = ZineDocument(
        format = ZineFormat.SINGLE_SHEET_8,
        paperSize = PaperSize.LETTER,
        pages = (0 until ZineFormat.SINGLE_SHEET_8.pageCount).map { Page(index = it, role = PageRole.INTERIOR) },
    )

    private fun documentFile(id: String): Path = root.resolve("projects/$id/document.json")
    private fun metaFile(id: String): Path = root.resolve("projects/$id/meta.json")

    private class WriteCountingDao(private val delegate: ProjectDao) : ProjectDao by delegate {
        var writes = 0
        override suspend fun upsert(project: ProjectEntity) {
            writes++
            delegate.upsert(project)
        }
    }

    private class LoadCountingDocuments(private val delegate: DocumentRepository) : DocumentRepository by delegate {
        var loads = 0
        override suspend fun load(projectId: String): DataResult<ZineDocument> {
            loads++
            return delegate.load(projectId)
        }
    }
}
