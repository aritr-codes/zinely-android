package com.aritr.zinely.data.android

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.ProjectShelfEntry
import com.aritr.zinely.core.data.repository.RestoredDetails
import com.aritr.zinely.core.data.repository.getOrNull
import com.aritr.zinely.core.data.storage.AtomicFileStore
import com.aritr.zinely.core.data.storage.FileSystemOps
import com.aritr.zinely.core.data.storage.NioFileSystemOps
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.data.android.room.ZinelyDatabase
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
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

/**
 * [ADR-134](docs/DECISIONS.md#adr-134): a `meta.json` put back from its earlier copy is told once. The repair
 * signal comes from the store's sealed read result, the waiting note is kept beside the zine's files, and the Shelf
 * lists it with the zine. Real [AtomicFileStore] and [DocumentRepositoryImpl] over a temp dir, Room in memory.
 */
@RunWith(RobolectricTestRunner::class)
class RoomProjectRepositoryRestoredNoteTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var root: Path
    private lateinit var db: ZinelyDatabase
    private var now = 1_000L
    private var nextId = 1

    @Before
    fun setUp() {
        root = tmp.root.toPath()
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ZinelyDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    /** A new repository over the same files and index: a new process. */
    private fun repo(store: AtomicFileStore = AtomicFileStore()): RoomProjectRepository = process(store).first

    /**
     * A new process, wired as `DataModule` wires it: one [RepairNotes] shared by the project repository and the
     * document repository the Bench reads through ([ADR-140](docs/DECISIONS.md#adr-140) decision 4).
     */
    private fun process(store: AtomicFileStore = AtomicFileStore()): Pair<RoomProjectRepository, DocumentRepositoryImpl> {
        val notes = RepairNotes(root, store)
        val documents = DocumentRepositoryImpl(rootDir = root, store = AtomicFileStore(), onPagesRestored = notes::addPages)
        return RoomProjectRepository(
            rootDir = root,
            dao = db.projectDao(),
            documents = documents,
            store = store,
            sessionGate = { true },
            io = Dispatchers.Unconfined,
            clock = { now++ },
            newId = { "p${nextId++}" },
            notes = notes,
        ) to documents
    }

    private fun docFile(id: String): Path = root.resolve("projects/$id/document.json")

    /** Saved a second time, so its page file has an earlier copy, then that page file damaged. */
    private suspend fun damagePages(id: String) {
        val documents = DocumentRepositoryImpl(rootDir = root, store = AtomicFileStore())
        documents.save(id, documents.load(id).getOrNull()!!)
        Files.write(docFile(id), "{".toByteArray())
    }

    private val pagesOnly = RestoredDetails(previousTitle = null, details = false, pages = true)

    /** A store on which an atomic replace of a file called [name] fails, as on a full disk. */
    private fun failing(name: String): AtomicFileStore = AtomicFileStore(
        object : FileSystemOps by NioFileSystemOps {
            override fun atomicReplace(source: Path, replacing: Path) {
                if (replacing.fileName.toString() == name) throw IOException("disk full")
                NioFileSystemOps.atomicReplace(source, replacing)
            }
        },
    )

    private fun metaFile(id: String): Path = root.resolve("projects/$id/meta.json")
    private fun noteFile(id: String): Path = root.resolve("projects/$id/restored-note.json")

    /** Made as "Letters home", renamed "Letters to Gran", then its meta.json damaged with the app stopped. */
    private suspend fun renamedThenDamaged(): String {
        val id = repo().createProject("Letters home", ZineFormat.SINGLE_SHEET_8, PaperSize.A4).getOrNull()!!.id
        repo().renameProject(id, "Letters to Gran") // meta.json.bak now holds "Letters home"
        Files.write(metaFile(id), """{"title":"\x"}""".toByteArray())
        return id
    }

    private suspend fun RoomProjectRepository.shelf(): List<ProjectShelfEntry> = observeShelfProjects().first()

    @Test
    fun `a repaired read raises one waiting note with the title the Shelf showed, and a clean read none`() = runTest {
        val clean = repo().createProject("Riso tests", ZineFormat.SINGLE_SHEET_8, PaperSize.A4).getOrNull()!!.id
        val id = renamedThenDamaged()

        val shelf = repo().shelf()

        val repaired = shelf.single { it.id == id }
        assertEquals("the tile shows the repaired file", "Letters home", repaired.title)
        assertEquals(RestoredDetails(previousTitle = "Letters to Gran"), repaired.restored)
        assertNull("a clean read raises nothing", shelf.single { it.id == clean }.restored)
        assertEquals(1, shelf.count { it.restored != null })
        assertTrue("kept beside the zine's files", Files.exists(noteFile(id)))
        // Read again: the file is repaired now, so the same one note, not a second.
        assertEquals(listOf(id), repo().shelf().filter { it.restored != null }.map { it.id })
    }

    @Test
    fun `the note survives a restart and Got it clears it for good`() = runTest {
        val id = renamedThenDamaged()
        repo().shelf()

        val restarted = repo()
        assertEquals("Letters to Gran", restarted.shelf().single().restored?.previousTitle)

        assertTrue(restarted.dismissRestoredDetails(listOf(id)) is DataResult.Success)
        assertNull(restarted.shelf().single().restored)
        assertFalse(Files.exists(noteFile(id)))
        assertNull(repo().shelf().single().restored)
    }

    @Test
    fun `a repair whose write fails is told once, and the lone backup read later raises nothing after Got it`() = runTest {
        val id = renamedThenDamaged()
        val diskFull = repo(store = failing("meta.json"))

        val first = diskFull.shelf().single()
        assertEquals("Letters home", first.title)
        assertEquals("Letters to Gran", first.restored?.previousTitle)
        assertFalse("the damaged file is quarantined", Files.exists(metaFile(id)))
        diskFull.dismissRestoredDetails(listOf(id))

        // Every later read takes the lone .bak again (still failing to put it back): the same repair, not a new one.
        assertNull(diskFull.shelf().single().restored)
        assertNull(repo(store = failing("meta.json")).shelf().single().restored)
    }

    @Test
    fun `when the waiting note cannot be written it still shows for this run`() = runTest {
        val id = renamedThenDamaged()
        val noRoom = repo(store = failing("restored-note.json"))

        assertEquals("Letters to Gran", noRoom.shelf().single().restored?.previousTitle)
        assertFalse(Files.exists(noteFile(id)))
        // Only its survival across a restart is lost.
        assertNull(repo().shelf().single().restored)
    }

    @Test
    fun `a repair found after the start scan re-syncs the row, so the tile and the note agree`() = runTest {
        val repo = repo()
        val id = repo.createProject("Letters home", ZineFormat.SINGLE_SHEET_8, PaperSize.A4).getOrNull()!!.id
        repo.renameProject(id, "Letters to Gran")
        assertEquals("Letters to Gran", repo.shelf().single().title) // reconciled: no later scan on this instance

        Files.write(metaFile(id), """{"title":"\x"}""".toByteArray())
        val entry = repo.shelf().single()

        assertEquals("Letters home", entry.title)
        assertEquals("Letters to Gran", entry.restored?.previousTitle)
        assertEquals("the index follows the file", "Letters home", db.projectDao().findById(id)!!.title)
        assertEquals("Letters home", repo.getProject(id).getOrNull()!!.title)
    }

    @Test
    fun `renaming or moving a zine takes it out of the note, and deleting it removes its marker`() = runTest {
        val renamed = renamedThenDamaged()
        repo().shelf()
        assertTrue(repo().renameProject(renamed, "Letters to Gran") is DataResult.Success)
        assertNull(repo().shelf().single { it.id == renamed }.restored)

        val moved = repo().createProject("Coffee log", ZineFormat.SINGLE_SHEET_8, PaperSize.A4).getOrNull()!!.id
        repo().renameProject(moved, "Coffee diary")
        Files.write(metaFile(moved), "{".toByteArray())
        assertTrue(repo().shelf().single { it.id == moved }.restored != null)
        assertTrue(repo().moveProject(moved, "Kitchen") is DataResult.Success)
        assertNull(repo().shelf().single { it.id == moved }.restored)

        val deleted = repo().createProject("Tiny poems", ZineFormat.SINGLE_SHEET_8, PaperSize.A4).getOrNull()!!.id
        repo().renameProject(deleted, "Small poems")
        Files.write(metaFile(deleted), "{".toByteArray())
        val repo = repo()
        assertTrue(repo.shelf().single { it.id == deleted }.restored != null)
        assertTrue(repo.deleteProject(deleted) is DataResult.Success)
        assertTrue(repo.shelf().none { it.id == deleted })
    }

    /** Makes the marker's `.bak` impossible to delete: a directory that is not empty. */
    private fun blockMarkerBackup(id: String) {
        val bak = root.resolve("projects/$id/restored-note.json.bak")
        Files.deleteIfExists(bak)
        Files.createDirectories(bak.resolve("stuck"))
    }

    @Test
    fun `a Got it that cannot finish clearing reports failure and keeps the note for next start`() = runTest {
        val id = renamedThenDamaged()
        repo().shelf()
        blockMarkerBackup(id)

        assertTrue(repo().dismissRestoredDetails(listOf(id)) is DataResult.Failure)

        // The main marker went second, so it is still there: the note is not silently lost.
        assertTrue(Files.exists(noteFile(id)))
        assertEquals("Letters to Gran", repo().shelf().single().restored?.previousTitle)
    }

    @Test
    fun `a rename that committed reports success even when its marker cannot be cleared`() = runTest {
        val id = renamedThenDamaged()
        repo().shelf()
        blockMarkerBackup(id)

        assertTrue(repo().renameProject(id, "Letters to Gran") is DataResult.Success)

        assertEquals("Letters to Gran", repo().getProject(id).getOrNull()!!.title)
        assertTrue("left on disk, told again", Files.exists(noteFile(id)))
    }

    @Test
    fun `a duplicate does not inherit its original's waiting note`() = runTest {
        val id = renamedThenDamaged()
        val repo = repo()
        repo.shelf()

        val copy = repo.duplicateProject(id).getOrNull()!!.id

        assertTrue(repo.shelf().single { it.id == id }.restored != null)
        assertNull(repo.shelf().single { it.id == copy }.restored)
    }

    // ---- ADR-140: a page file put back from its earlier copy joins the same note.

    @Test
    fun pages_repair_writes_a_pages_note() = runTest {
        // Given a zine whose page file is damaged beside a good earlier copy
        val id = repo().createProject("Coffee log", ZineFormat.SINGLE_SHEET_8, PaperSize.A4).getOrNull()!!.id
        damagePages(id)

        // When the Shelf lists it
        val entry = repo().shelf().single()

        // Then it opens, and one pages note waits, kept beside its files
        assertTrue(entry is ProjectShelfEntry.Available)
        assertEquals(pagesOnly, entry.restored)
        assertTrue(Files.exists(noteFile(id)))
        assertEquals("it survives a restart", pagesOnly, repo().shelf().single().restored)
    }

    @Test
    fun `a details and a pages repair of one zine make one note, keeping the old name`() = runTest {
        val id = renamedThenDamaged()
        damagePages(id)

        val entry = repo().shelf().single()

        assertEquals(RestoredDetails(previousTitle = "Letters to Gran", details = true, pages = true), entry.restored)
        assertEquals(entry.restored, repo().shelf().single().restored)
    }

    @Test
    fun `a pages repair after a details repair already waiting adds to that note`() = runTest {
        val id = renamedThenDamaged()
        repo().shelf()
        damagePages(id)

        assertEquals(
            RestoredDetails(previousTitle = "Letters to Gran", details = true, pages = true),
            repo().shelf().single().restored,
        )
    }

    @Test
    fun `a marker written before ADR-140 still reads as a details repair with its old name`() = runTest {
        val id = repo().createProject("Letters home", ZineFormat.SINGLE_SHEET_8, PaperSize.A4).getOrNull()!!.id
        Files.write(noteFile(id), """{"previousTitle":"Letters to Gran"}""".toByteArray())

        assertEquals(RestoredDetails(previousTitle = "Letters to Gran"), repo().shelf().single().restored)
    }

    @Test
    fun `renaming or moving clears the details part only, and Got it clears both (A46_5)`() = runTest {
        val renamed = renamedThenDamaged()
        damagePages(renamed)
        repo().shelf()
        assertTrue(repo().renameProject(renamed, "Letters to Gran") is DataResult.Success)
        assertEquals(pagesOnly, repo().shelf().single { it.id == renamed }.restored)
        // A pages-only zine renamed again keeps its note: a rename acts on its name, not its pages.
        assertTrue(repo().renameProject(renamed, "Letters") is DataResult.Success)
        assertEquals(pagesOnly, repo().shelf().single { it.id == renamed }.restored)

        val moved = repo().createProject("Coffee log", ZineFormat.SINGLE_SHEET_8, PaperSize.A4).getOrNull()!!.id
        repo().renameProject(moved, "Coffee diary")
        Files.write(metaFile(moved), "{".toByteArray())
        damagePages(moved)
        repo().shelf()
        assertTrue(repo().moveProject(moved, "Kitchen") is DataResult.Success)
        assertEquals(pagesOnly, repo().shelf().single { it.id == moved }.restored)

        assertTrue(repo().dismissRestoredDetails(listOf(renamed, moved)) is DataResult.Success)
        assertTrue(repo().shelf().all { it.restored == null })
        assertFalse(Files.exists(noteFile(renamed)))
    }

    @Test
    fun `a pages repair reported for a zine already deleted brings back no directory`() = runTest {
        // Given a zine deleted while the Bench was still reading its page file
        val notes = RepairNotes(root, AtomicFileStore())

        // When that read's repair is reported
        notes.addPages("gone")

        // Then nothing is written where the zine was
        assertFalse(Files.exists(root.resolve("projects/gone")))
    }

    @Test
    fun `a pages repair found when the Bench opens a zine is on the Shelf without a restart (R-W2)`() = runTest {
        // Given My Shelf has drawn its zines, none waiting
        val (repo, bench) = process()
        val id = repo.createProject("Coffee log", ZineFormat.SINGLE_SHEET_8, PaperSize.A4).getOrNull()!!.id
        val listings = Channel<List<ProjectShelfEntry>>(Channel.UNLIMITED)
        val shelf = backgroundScope.launch(Dispatchers.Default) { repo.observeShelfProjects().collect(listings::send) }
        assertNull(listings.receive().single().restored)

        // When the page file is found damaged by the Bench, which reads with no project repository on its path
        damagePages(id)
        assertTrue(bench.load(id) is DataResult.Success)

        // Then the Shelf lists again, and the note is there
        var latest = listings.receive()
        while (latest.single().restored == null) latest = listings.receive()
        assertEquals(pagesOnly, latest.single().restored)
        shelf.cancel()
    }
}
