package com.aritr.zinely.data.android

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aritr.zinely.core.data.repository.DataError
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
import java.nio.file.attribute.FileTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
 * The Room-backed [com.aritr.zinely.core.data.repository.ProjectRepository] (ADR-042). Runs Room on
 * the JVM via Robolectric (`inMemoryDatabaseBuilder`, no emulator) against the REAL
 * [DocumentRepositoryImpl] + [AtomicFileStore] over a temp dir, so the files-are-truth /
 * Room-is-index split is exercised end to end, mirroring [DocumentRepositoryImplTest]'s style.
 */
@RunWith(RobolectricTestRunner::class)
class RoomProjectRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var root: Path
    private lateinit var store: AtomicFileStore
    private lateinit var documents: DocumentRepositoryImpl
    private lateinit var db: ZinelyDatabase

    /** Controllable metadata-op clock; document mtimes stay real wall-clock (files are files). */
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
    ): RoomProjectRepository = RoomProjectRepository(
        rootDir = root,
        dao = dao,
        documents = documents,
        store = store,
        io = Dispatchers.Unconfined,
        clock = { now },
        newId = { "p${nextId++}" },
    )

    /**
     * A store whose atomic commit fails for `meta.json` targets only (the same [FileSystemOps]
     * fault-injection seam [DocumentRepositoryImplTest] uses). `documents` keeps its own good store,
     * so the document (file truth) commits and only the sidecar write fails.
     */
    private fun metaWriteFailingStore(): AtomicFileStore = AtomicFileStore(
        object : FileSystemOps by NioFileSystemOps {
            override fun atomicReplace(source: Path, replacing: Path) {
                if (replacing.fileName.toString() == "meta.json") throw IOException("meta write blocked")
                NioFileSystemOps.atomicReplace(source, replacing)
            }
        },
    )

    /** Delegates to the real Room DAO but throws on the next [upsert] — the post-file-commit index failure. */
    private class FailNextUpsertDao(private val delegate: ProjectDao) : ProjectDao by delegate {
        var failNextUpsert = false
        override suspend fun upsert(project: ProjectEntity) {
            if (failNextUpsert) {
                failNextUpsert = false
                throw IllegalStateException("index write blocked")
            }
            delegate.upsert(project)
        }
    }

    private fun validDoc(
        format: ZineFormat = ZineFormat.SINGLE_SHEET_8,
        paperSize: PaperSize = PaperSize.LETTER,
    ): ZineDocument = ZineDocument(
        format = format,
        paperSize = paperSize,
        pages = (0 until format.pageCount).map { Page(index = it, role = PageRole.INTERIOR) },
    )

    private fun documentFile(id: String): Path = root.resolve("projects/$id/document.json")
    private fun metaFile(id: String): Path = root.resolve("projects/$id/meta.json")

    // ---- reconcile / adoption ------------------------------------------------------------------

    @Test
    fun `given an empty store, when observing, then the list is empty`() = runTest {
        // When
        val projects = repo().observeProjects().first()

        // Then
        assertTrue(projects.isEmpty())
    }

    @Test
    fun `given an existing on-disk default project, when observing, then it is adopted as a row`() = runTest {
        // Given — the S4 seed exactly as EditorBootstrap leaves it
        documents.save("default", validDoc())

        // When
        val projects = repo().observeProjects().first()

        // Then — adopted with fallback title, document-derived format/paper, mtime-derived createdAt
        val adopted = projects.single()
        assertEquals("default", adopted.id)
        assertEquals("My zine", adopted.title)
        assertEquals(ZineFormat.SINGLE_SHEET_8, adopted.format)
        assertEquals(PaperSize.LETTER, adopted.paperSize)
        assertEquals(Files.getLastModifiedTime(documentFile("default")).toMillis(), adopted.createdAtEpochMs)
        // and the meta.json sidecar is backfilled
        assertTrue(Files.isRegularFile(metaFile("default")))
    }

    @Test
    fun `given a stale row without backing files, when observing, then the row is dropped`() = runTest {
        // Given — an index row with no on-disk project (files are the source of truth)
        db.projectDao().upsert(
            ProjectEntity(
                id = "ghost", title = "Ghost", format = "SINGLE_SHEET_8", paperSize = "LETTER",
                createdAtEpochMs = 1L, updatedAtEpochMs = 1L, documentSchemaVersion = 1,
            ),
        )

        // When
        val projects = repo().observeProjects().first()

        // Then
        assertTrue(projects.isEmpty())
    }

    @Test
    fun `given a corrupt document on disk, when reconciling, then the project is skipped and bytes untouched`() = runTest {
        // Given
        val dir = root.resolve("projects/bad")
        Files.createDirectories(dir)
        val garbage = "{ not a document".toByteArray()
        Files.write(dir.resolve("document.json"), garbage)

        // When
        val projects = repo().observeProjects().first()

        // Then — invisible (ADR-042 known limitation), bytes left for a future repair path
        assertTrue(projects.isEmpty())
        assertTrue(garbage.contentEquals(Files.readAllBytes(dir.resolve("document.json"))))
    }

    @Test
    fun `given a newer-schema document on disk, when reconciling, then the project is skipped and bytes untouched`() = runTest {
        // Given — well-formed JSON declaring a schema this build does not support (SchemaTooNew, not Corrupt)
        val dir = root.resolve("projects/toonew")
        Files.createDirectories(dir)
        val payload = """{"schemaVersion":2}""".toByteArray()
        Files.write(dir.resolve("document.json"), payload)

        // When
        val projects = repo().observeProjects().first()

        // Then — same skip as corruption (ADR-042 §4): invisible, bytes left for a future repair path
        assertTrue(projects.isEmpty())
        assertTrue(payload.contentEquals(Files.readAllBytes(dir.resolve("document.json"))))
    }

    @Test
    fun `given an unreadable meta sidecar, when adopting, then fallback title is used and the file is not overwritten`() = runTest {
        // Given — a valid document but corrupt meta.json (the only copy of Tier-C metadata)
        documents.save("m1", validDoc())
        val garbage = "not json".toByteArray()
        Files.write(metaFile("m1"), garbage)

        // When
        val adopted = repo().observeProjects().first().single()

        // Then
        assertEquals("My zine", adopted.title)
        assertTrue(garbage.contentEquals(Files.readAllBytes(metaFile("m1"))))
    }

    // ---- create --------------------------------------------------------------------------------

    @Test
    fun `given create, then the project appears in the observed list with its metadata`() = runTest {
        // When
        val created = repo().createProject("My Zine", ZineFormat.SINGLE_SHEET_8, PaperSize.A4)

        // Then
        val summary = created.getOrNull()!!
        assertEquals("p1", summary.id)
        assertEquals("My Zine", summary.title)
        assertEquals(ZineFormat.SINGLE_SHEET_8, summary.format)
        assertEquals(PaperSize.A4, summary.paperSize)
        assertEquals(now, summary.createdAtEpochMs)
        assertEquals(listOf("p1"), repo().observeProjects().first().map { it.id })
    }

    @Test
    fun `given create, then a loadable blank document with the format page count is persisted`() = runTest {
        // When
        val id = repo().createProject("Z", ZineFormat.SINGLE_SHEET_8, PaperSize.A4).getOrNull()!!.id

        // Then — the document (source of truth) exists, validates, and carries the requested shape
        val document = documents.load(id).getOrNull()!!
        assertEquals(PaperSize.A4, document.paperSize)
        assertEquals(ZineFormat.SINGLE_SHEET_8.pageCount, document.pages.size)
        assertTrue(Files.isRegularFile(metaFile(id)))
    }

    // ---- getProject ----------------------------------------------------------------------------

    @Test
    fun `given a created project, when getting it, then its summary round-trips`() = runTest {
        // Given
        val repo = repo()
        val id = repo.createProject("G", ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER).getOrNull()!!.id

        // When / Then
        assertEquals("G", repo.getProject(id).getOrNull()!!.title)
    }

    @Test
    fun `given a missing project, when getting it, then NotFound`() = runTest {
        assertEquals(DataError.NotFound("nope"), repo().getProject("nope").errorOrNull())
    }

    // ---- rename --------------------------------------------------------------------------------

    @Test
    fun `given rename, then the title updates and createdAt is preserved`() = runTest {
        // Given
        val repo = repo()
        val id = repo.createProject("Old", ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER).getOrNull()!!.id
        val createdAt = now
        now += 10

        // When
        val renamed = repo.renameProject(id, "New")

        // Then
        assertTrue(renamed.isSuccess)
        val summary = repo.getProject(id).getOrNull()!!
        assertEquals("New", summary.title)
        assertEquals(createdAt, summary.createdAtEpochMs)
    }

    @Test
    fun `given rename of a missing project, then NotFound`() = runTest {
        assertEquals(DataError.NotFound("nope"), repo().renameProject("nope", "X").errorOrNull())
    }

    // ---- duplicate -----------------------------------------------------------------------------

    @Test
    fun `given duplicate, then the copy shares document content under a new id and the original is untouched`() = runTest {
        // Given
        val repo = repo()
        val src = repo.createProject("Orig", ZineFormat.SINGLE_SHEET_8, PaperSize.A4).getOrNull()!!.id

        // When
        val copy = repo.duplicateProject(src).getOrNull()!!

        // Then — new id, derived title, identical document (same content hashes by construction, ADR-022)
        assertTrue(copy.id != src)
        assertEquals("Orig copy", copy.title)
        assertEquals(documents.load(src).getOrNull(), documents.load(copy.id).getOrNull())
        assertEquals("Orig", repo.getProject(src).getOrNull()!!.title)
        assertEquals(2, repo.observeProjects().first().size)
    }

    @Test
    fun `given duplicate of a missing project, then NotFound`() = runTest {
        assertEquals(DataError.NotFound("nope"), repo().duplicateProject("nope").errorOrNull())
    }

    // ---- delete --------------------------------------------------------------------------------

    @Test
    fun `given delete, then the row and all project files are gone`() = runTest {
        // Given
        val repo = repo()
        val id = repo.createProject("D", ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER).getOrNull()!!.id

        // When
        val deleted = repo.deleteProject(id)

        // Then — document gone (releases the project's GC roots, ADR-022), dir gone, row gone
        assertTrue(deleted.isSuccess)
        assertFalse(Files.exists(root.resolve("projects/$id")))
        assertTrue(repo.observeProjects().first().isEmpty())
        assertEquals(DataError.NotFound(id), repo.getProject(id).errorOrNull())
    }

    @Test
    fun `given delete of an already-deleted project, then delete is idempotent`() = runTest {
        // Given
        val repo = repo()
        val id = repo.createProject("D", ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER).getOrNull()!!.id
        repo.deleteProject(id)

        // When / Then
        assertTrue(repo.deleteProject(id).isSuccess)
    }

    // ---- returned-failure cleanup + index divergence (ADR-042 §5) -------------------------------

    @Test
    fun `given the meta write fails, when creating, then Io is returned and no partial files or row remain`() = runTest {
        // When — the document commits, then the sidecar write fails
        val result = repo(store = metaWriteFailingStore())
            .createProject("X", ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER)

        // Then — a returned failure leaves no adoptable orphan: files cleaned up, nothing indexed
        assertTrue("expected Io, got $result", result.errorOrNull() is DataError.Io)
        assertFalse(Files.exists(root.resolve("projects/p1")))
        assertTrue(repo().observeProjects().first().isEmpty())
    }

    @Test
    fun `given the meta write fails, when duplicating, then Io is returned and the copy leaves no partial files`() = runTest {
        // Given — a healthy source project
        val src = repo().createProject("Orig", ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER).getOrNull()!!.id

        // When — the copy's document commits, then its sidecar write fails
        val result = repo(store = metaWriteFailingStore()).duplicateProject(src)

        // Then — copy cleaned up (id p2), source untouched
        assertTrue("expected Io, got $result", result.errorOrNull() is DataError.Io)
        assertFalse(Files.exists(root.resolve("projects/p2")))
        assertEquals(listOf(src), repo().observeProjects().first().map { it.id })
        assertEquals("Orig", repo().getProject(src).getOrNull()!!.title)
    }

    @Test
    fun `given the index write fails after the files commit, then Io is returned and the next reconcile re-derives the row`() = runTest {
        // Given — a reconciled repo whose next DAO upsert fails
        val dao = FailNextUpsertDao(db.projectDao())
        val repo = repo(dao = dao)
        repo.observeProjects().first() // reconcile the empty store first, so create hits only the upsert
        dao.failNextUpsert = true

        // When
        val result = repo.createProject("T", ZineFormat.SINGLE_SHEET_8, PaperSize.A4)

        // Then — Io surfaced; file truth stayed committed but no row was indexed
        assertTrue("expected Io, got $result", result.errorOrNull() is DataError.Io)
        assertTrue(Files.isRegularFile(documentFile("p1")))
        assertTrue(Files.isRegularFile(metaFile("p1")))
        assertNull(db.projectDao().findById("p1"))

        // and the flagged re-reconcile converges the index to file truth (title/createdAt from disk)
        val adopted = repo.observeProjects().first().single()
        assertEquals("p1", adopted.id)
        assertEquals("T", adopted.title)
        assertEquals(now, adopted.createdAtEpochMs)
    }

    // ---- ordering ------------------------------------------------------------------------------

    @Test
    fun `given a document saved after creation, when observing, then recency follows the document mtime`() = runTest {
        // Given — p1 then p2 by metadata clock; then p1's DOCUMENT is edited later than everything
        val repo = repo()
        val first = repo.createProject("A", ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER).getOrNull()!!.id
        now += 10
        val second = repo.createProject("B", ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER).getOrNull()!!.id
        val editedAt = System.currentTimeMillis() + 100_000
        Files.setLastModifiedTime(documentFile(first), FileTime.fromMillis(editedAt))

        // When
        val projects = repo.observeProjects().first()

        // Then — newest-first by max(row.updatedAt, document mtime): an autosaved document is
        // durable file truth, so its mtime wins without coupling autosave to the metadata layer
        assertEquals(listOf(first, second), projects.map { it.id })
        assertEquals(editedAt, projects.first().updatedAtEpochMs)
    }
}
