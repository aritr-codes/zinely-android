package com.aritr.zinely.home

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.ProjectRepository
import com.aritr.zinely.core.data.repository.ProjectSummary
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.feature.editor.HomeShelfEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the S6.2 read-only Home shelf ViewModel (ADR-043). Given-When-Then; a hand fake
 * stands in for the [ProjectRepository] so the Loading→Empty/Content mapping and the warm card
 * labels are verified with no Android / Hilt. The fake's flow is **cold-until-emitted** (a replaying
 * SharedFlow, not a StateFlow), so the Loading-first assertion is real, not incidental (Codex).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    /**
     * Recording shelf fake (S6.3, ADR-044): scripts [observeProjects] via a cold-until-emitted
     * replaying SharedFlow and records every mutation; per-call results are scriptable so failure
     * paths (generic, [DataError.Busy]) are testable.
     */
    private class FakeProjectRepository : ProjectRepository {
        val projects = MutableSharedFlow<List<ProjectSummary>>(replay = 1)

        val created = mutableListOf<Triple<String, ZineFormat, PaperSize>>()
        val renamed = mutableListOf<Pair<String, String>>()
        val duplicated = mutableListOf<String>()
        val deleted = mutableListOf<String>()

        /** How many times the shelf stream has been (re)collected — the ADR-046 §6 freshness proof. */
        var observeCollections = 0

        /** When set, [createProject] suspends on it — lets a test hold a create in flight (§5 single-flight). */
        var createGate: CompletableDeferred<Unit>? = null

        var createResult: () -> DataResult<ProjectSummary> =
            { error("script createResult when the test asserts on it") }
        var renameResult: () -> DataResult<Unit> = { DataResult.Success(Unit) }
        var duplicateResult: () -> DataResult<ProjectSummary> =
            { error("script duplicateResult when the test asserts on it") }
        var deleteResult: () -> DataResult<Unit> = { DataResult.Success(Unit) }

        /** When set, the *next* collection of [observeProjects] throws instead of emitting. */
        var observeFailure: Throwable? = null

        override fun observeProjects(): Flow<List<ProjectSummary>> = flow {
            observeCollections++
            observeFailure?.let { failure ->
                observeFailure = null // one scripted failure: a retry must be able to succeed
                throw failure
            }
            emitAll(projects)
        }

        override suspend fun getProject(id: String): DataResult<ProjectSummary> =
            error("not used by the shelf")

        override suspend fun createProject(
            title: String,
            format: ZineFormat,
            paperSize: PaperSize,
        ): DataResult<ProjectSummary> {
            created += Triple(title, format, paperSize)
            createGate?.await()
            return createResult()
        }

        override suspend fun renameProject(id: String, title: String): DataResult<Unit> {
            renamed += id to title
            return renameResult()
        }

        override suspend fun duplicateProject(id: String): DataResult<ProjectSummary> {
            duplicated += id
            return duplicateResult()
        }

        override suspend fun deleteProject(id: String): DataResult<Unit> {
            deleted += id
            return deleteResult()
        }
    }

    /**
     * Recording thumbnail fake (S6.4, ADR-045): scripts [ensure] per id and records every ask, so
     * the "hidden cards are never asked for" and delivery-mapping assertions are direct.
     */
    private class FakeShelfThumbnails : ShelfThumbnails {
        val asked = mutableListOf<String>()
        var bitmaps: Map<String, ImageBitmap> = emptyMap()

        override suspend fun ensure(projectId: String): ImageBitmap? {
            asked += projectId
            return bitmaps[projectId]
        }
    }

    /** A JVM-pure [ImageBitmap] — the VM never reads pixels, it only carries the reference. */
    private class FakeImageBitmap : ImageBitmap {
        override val width: Int = 1
        override val height: Int = 1
        override val config: ImageBitmapConfig = ImageBitmapConfig.Argb8888
        override val hasAlpha: Boolean = false
        override val colorSpace: ColorSpace = ColorSpaces.Srgb
        override fun prepareToDraw() = Unit
        override fun readPixels(
            buffer: IntArray,
            startX: Int,
            startY: Int,
            width: Int,
            height: Int,
            bufferOffset: Int,
            stride: Int,
        ) = Unit
    }

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeProjectRepository
    private lateinit var thumbnails: FakeShelfThumbnails

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeProjectRepository()
        thumbnails = FakeShelfThumbnails()
    }

    private fun viewModel() = HomeViewModel(repository, thumbnails)

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun summary(
        id: String,
        title: String,
        updatedAtEpochMs: Long,
        paperSize: PaperSize = PaperSize.LETTER,
    ) = ProjectSummary(
        id = id,
        title = title,
        format = ZineFormat.SINGLE_SHEET_8,
        paperSize = paperSize,
        createdAtEpochMs = updatedAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs,
        documentSchemaVersion = 1,
    )

    @Test
    fun `the shelf is Loading until the store first answers`() = runTest {
        val viewModel = viewModel()

        val job = launch(Dispatchers.Main) { viewModel.state.collect {} }
        assertEquals(HomeUiState.Loading, viewModel.state.value)
        job.cancel()
    }

    @Test
    fun `a store that cannot be read is the Error shelf, not an empty one`() = runTest {
        repository.observeFailure = IllegalStateException("the store is unreadable")
        val viewModel = viewModel()

        val job = launch(Dispatchers.Main) { viewModel.state.collect {} }

        // Never Empty: an unreadable shelf must not invite you to make your first zine.
        assertEquals(HomeUiState.Error, viewModel.state.value)
        job.cancel()
    }

    @Test
    fun `retry re-subscribes and a shelf that reads on the second ask recovers`() = runTest {
        repository.observeFailure = IllegalStateException("the store is unreadable")
        val viewModel = viewModel()

        val job = launch(Dispatchers.Main) { viewModel.state.collect {} }
        assertEquals(HomeUiState.Error, viewModel.state.value)
        assertEquals(1, repository.observeCollections)

        // When retried, a *fresh* collection is made — the thrown flow is dead and cannot be revived
        viewModel.retry()
        assertEquals(2, repository.observeCollections)
        repository.projects.emit(listOf(summary("p1", "Notes on Rain", updatedAtEpochMs = 0L)))

        val cards = (viewModel.state.value as HomeUiState.Content).cards
        assertEquals(listOf("Notes on Rain"), cards.map { it.title })
        job.cancel()
    }

    /**
     * A retry holds the error until the store answers. It must NOT re-arm Loading: this flow is
     * re-collected on every return to the shelf (ADR-046 §6), so a per-subscription Loading emission
     * would wipe the cached Content and flash the skeleton on the app's most common flow.
     */
    @Test
    fun `retry holds the error until the store answers, never flashing Loading`() = runTest {
        repository.observeFailure = IllegalStateException("the store is unreadable")
        val viewModel = viewModel()

        val job = launch(Dispatchers.Main) { viewModel.state.collect {} }
        assertEquals(HomeUiState.Error, viewModel.state.value)

        viewModel.retry()
        // The second ask is in flight; the store has not answered it yet.
        assertEquals(HomeUiState.Error, viewModel.state.value)
        job.cancel()
    }

    @Test
    fun `an empty store is the Empty shelf, never a zero-card Content`() = runTest {
        val viewModel = viewModel()

        val job = launch(Dispatchers.Main) { viewModel.state.collect {} }
        repository.projects.emit(emptyList())

        assertEquals(HomeUiState.Empty, viewModel.state.value)
        job.cancel()
    }

    @Test
    fun `projects become warm cards - id, title, format and recency labels`() = runTest {
        val fiveMinutesAgo = System.currentTimeMillis() - 5 * 60_000L
        val viewModel = viewModel()

        val job = launch(Dispatchers.Main) { viewModel.state.collect {} }
        repository.projects.emit(
            listOf(summary(id = "z1", title = "My first zine", updatedAtEpochMs = fiveMinutesAgo)),
        )

        val content = viewModel.state.value as HomeUiState.Content
        val card = content.cards.single()
        assertEquals("z1", card.id)
        assertEquals("My first zine", card.title)
        assertEquals("8-page mini · Letter", card.formatLabel)
        assertEquals("Edited 5 minutes ago", card.editedLabel)
        job.cancel()
    }

    @Test
    fun `A4 zines say so on their card`() = runTest {
        val viewModel = viewModel()

        val job = launch(Dispatchers.Main) { viewModel.state.collect {} }
        repository.projects.emit(
            listOf(
                summary(
                    id = "z2",
                    title = "Trip notes",
                    updatedAtEpochMs = System.currentTimeMillis(),
                    paperSize = PaperSize.A4,
                ),
            ),
        )

        val content = viewModel.state.value as HomeUiState.Content
        assertEquals("8-page mini · A4", content.cards.single().formatLabel)
        job.cancel()
    }

    @Test
    fun `the shelf re-renders when the store changes`() = runTest {
        val now = System.currentTimeMillis()
        val viewModel = viewModel()

        val job = launch(Dispatchers.Main) { viewModel.state.collect {} }
        repository.projects.emit(listOf(summary("z1", "One", now)))
        assertEquals(1, (viewModel.state.value as HomeUiState.Content).cards.size)

        repository.projects.emit(listOf(summary("z2", "Two", now), summary("z1", "One", now)))

        val cards = (viewModel.state.value as HomeUiState.Content).cards
        assertEquals(listOf("z2", "z1"), cards.map { it.id })
        job.cancel()
    }

    @Test
    fun `cards keep the repository's order - the VM never re-sorts`() = runTest {
        // Newest-first is the ProjectRepository contract (ADR-042 §7); the VM must pass it
        // through untouched rather than duplicate the ordering logic (Codex).
        val now = System.currentTimeMillis()
        val viewModel = viewModel()

        val job = launch(Dispatchers.Main) { viewModel.state.collect {} }
        repository.projects.emit(
            listOf(
                summary("newest", "Newest", now),
                summary("older", "Older", now - 90_000L),
                summary("oldest", "Oldest", now - 86_400_000L),
            ),
        )

        val cards = (viewModel.state.value as HomeUiState.Content).cards
        assertEquals(listOf("newest", "older", "oldest"), cards.map { it.id })
        job.cancel()
    }

    // --- S6.3 shelf actions (ADR-044) ---

    @Test
    fun `Start a zine creates with the warm defaults`() = runTest {
        // Given
        val viewModel = viewModel()
        repository.createResult = { DataResult.Success(summary("new", "My zine", 0L)) }

        // When
        viewModel.startZine(PaperSize.LETTER)

        // Then — "My zine", the only format, and the bootstrap-matching paper (ADR-044 §4)
        assertEquals(
            listOf(Triple("My zine", ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER)),
            repository.created,
        )
    }

    @Test
    fun `Start a zine creates on the chosen paper`() = runTest {
        // Given — S7.1: the shelf asks which paper before creating (A4 printers exist)
        val viewModel = viewModel()
        repository.createResult = { DataResult.Success(summary("new", "My zine", 0L)) }

        // When the person picks A4
        viewModel.startZine(PaperSize.A4)

        // Then the project is created on A4, not the old hardcoded Letter
        assertEquals(
            listOf(Triple("My zine", ZineFormat.SINGLE_SHEET_8, PaperSize.A4)),
            repository.created,
        )
    }

    @Test
    fun `a failed create surfaces the warm generic message`() = runTest {
        // Given
        val viewModel = viewModel()
        repository.createResult = { DataResult.Failure(DataError.Io("disk")) }
        val events = mutableListOf<HomeShelfEvent>()
        val eventsJob = launch(Dispatchers.Main) { viewModel.events.collect { events += it } }

        // When
        viewModel.startZine(PaperSize.LETTER)

        // Then
        assertEquals(listOf<HomeShelfEvent>(HomeShelfEvent.Message(GENERIC_FAILURE_MESSAGE)), events)
        eventsJob.cancel()
    }

    @Test
    fun `rename trims the title before it reaches the store`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.rename("z1", "  Trip notes  ")

        // Then
        assertEquals(listOf("z1" to "Trip notes"), repository.renamed)
    }

    @Test
    fun `a blank rename keeps the existing name - no store call`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.rename("z1", "   ")

        // Then
        assertTrue(repository.renamed.isEmpty())
    }

    @Test
    fun `duplicate delegates to the store`() = runTest {
        // Given
        val viewModel = viewModel()
        repository.duplicateResult = { DataResult.Success(summary("copy", "One copy", 0L)) }

        // When
        viewModel.duplicate("z1")

        // Then
        assertEquals(listOf("z1"), repository.duplicated)
    }

    @Test
    fun `a Busy refusal reads as still-saving, not as a failure`() = runTest {
        // Given — the ADR-044 §1 gate refused: an editor session is still live/releasing
        val viewModel = viewModel()
        repository.duplicateResult = { DataResult.Failure(DataError.Busy("live session")) }
        val events = mutableListOf<HomeShelfEvent>()
        val eventsJob = launch(Dispatchers.Main) { viewModel.events.collect { events += it } }

        // When
        viewModel.duplicate("z1")

        // Then
        assertEquals(listOf<HomeShelfEvent>(HomeShelfEvent.Message(BUSY_MESSAGE)), events)
        eventsJob.cancel()
    }

    @Test
    fun `delete hides the card immediately and prompts for undo - no store call yet`() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val viewModel = viewModel()
        val stateJob = launch(Dispatchers.Main) { viewModel.state.collect {} }
        val events = mutableListOf<HomeShelfEvent>()
        val eventsJob = launch(Dispatchers.Main) { viewModel.events.collect { events += it } }
        repository.projects.emit(listOf(summary("z1", "Zine one", now), summary("z2", "Zine two", now)))

        // When
        viewModel.delete("z1")

        // Then — card hidden, one prompt with the title, nothing deleted in the store
        assertEquals(listOf("z2"), (viewModel.state.value as HomeUiState.Content).cards.map { it.id })
        assertEquals(listOf<HomeShelfEvent>(HomeShelfEvent.DeletePrompt("z1", "Zine one")), events)
        assertTrue(repository.deleted.isEmpty())
        stateJob.cancel()
        eventsJob.cancel()
    }

    @Test
    fun `a second delete of the same card does not prompt twice`() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val viewModel = viewModel()
        val stateJob = launch(Dispatchers.Main) { viewModel.state.collect {} }
        val events = mutableListOf<HomeShelfEvent>()
        val eventsJob = launch(Dispatchers.Main) { viewModel.events.collect { events += it } }
        repository.projects.emit(listOf(summary("z1", "One", now)))

        // When — the card is already hidden, so a second tap can't see it; guard anyway
        viewModel.delete("z1")
        viewModel.delete("z1")

        // Then
        assertEquals(1, events.size)
        stateJob.cancel()
        eventsJob.cancel()
    }

    @Test
    fun `undo unhides the card without touching the store`() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val viewModel = viewModel()
        val stateJob = launch(Dispatchers.Main) { viewModel.state.collect {} }
        repository.projects.emit(listOf(summary("z1", "One", now)))
        viewModel.delete("z1")

        // When
        viewModel.undoDelete("z1")

        // Then
        assertEquals(listOf("z1"), (viewModel.state.value as HomeUiState.Content).cards.map { it.id })
        assertTrue(repository.deleted.isEmpty())
        stateJob.cancel()
    }

    @Test
    fun `commit performs the store delete`() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val viewModel = viewModel()
        val stateJob = launch(Dispatchers.Main) { viewModel.state.collect {} }
        repository.projects.emit(listOf(summary("z1", "One", now)))
        viewModel.delete("z1")

        // When
        viewModel.commitDelete("z1")

        // Then
        assertEquals(listOf("z1"), repository.deleted)
        stateJob.cancel()
    }

    @Test
    fun `a successful commit keeps the card hidden until the store emits the shorter list`() = runTest {
        // Given — unhiding on success would flash the deleted card back for the window between
        // deleteProject returning and observeProjects() re-emitting (reviewer Required Fix)
        val now = System.currentTimeMillis()
        val viewModel = viewModel()
        val stateJob = launch(Dispatchers.Main) { viewModel.state.collect {} }
        repository.projects.emit(listOf(summary("z1", "One", now), summary("z2", "Two", now)))
        viewModel.delete("z1")

        // When — the store delete succeeds but the flow has NOT re-emitted yet
        viewModel.commitDelete("z1")

        // Then — the card stays hidden
        assertEquals(listOf("z2"), (viewModel.state.value as HomeUiState.Content).cards.map { it.id })

        // and when the store catches up, the shelf simply reflects it
        repository.projects.emit(listOf(summary("z2", "Two", now)))
        assertEquals(listOf("z2"), (viewModel.state.value as HomeUiState.Content).cards.map { it.id })
        stateJob.cancel()
    }

    @Test
    fun `a failed commit unhides the card and says so - the shelf never lies`() = runTest {
        // Given (Codex: deleteProject is not infallible)
        val now = System.currentTimeMillis()
        val viewModel = viewModel()
        repository.deleteResult = { DataResult.Failure(DataError.Io("unindex failed")) }
        val stateJob = launch(Dispatchers.Main) { viewModel.state.collect {} }
        val events = mutableListOf<HomeShelfEvent>()
        val eventsJob = launch(Dispatchers.Main) { viewModel.events.collect { events += it } }
        repository.projects.emit(listOf(summary("z1", "One", now)))
        viewModel.delete("z1")

        // When
        viewModel.commitDelete("z1")

        // Then — card back on the shelf + a warm failure message after the prompt
        assertEquals(listOf("z1"), (viewModel.state.value as HomeUiState.Content).cards.map { it.id })
        assertEquals(HomeShelfEvent.Message(GENERIC_FAILURE_MESSAGE), events.last())
        stateJob.cancel()
        eventsJob.cancel()
    }

    @Test
    fun `hiding every card is a zero-card Content, never the Empty invitation`() = runTest {
        // Given — Empty means the STORE is empty; a pending delete is still reversible (Codex)
        val now = System.currentTimeMillis()
        val viewModel = viewModel()
        val stateJob = launch(Dispatchers.Main) { viewModel.state.collect {} }
        repository.projects.emit(listOf(summary("z1", "One", now)))

        // When
        viewModel.delete("z1")

        // Then
        val state = viewModel.state.value
        assertTrue(state is HomeUiState.Content && state.cards.isEmpty())
        stateJob.cancel()
    }

    // --- S6.4 shelf thumbnails (ADR-045) ---

    @Test
    fun `a delivered thumbnail rides its card`() = runTest {
        // Given a thumbnail the producer can deliver for z1
        val bitmap = FakeImageBitmap()
        thumbnails.bitmaps = mapOf("z1" to bitmap)
        val viewModel = viewModel()
        val job = launch(Dispatchers.Main) { viewModel.state.collect {} }

        // When the store emits the project
        repository.projects.emit(listOf(summary("z1", "One", System.currentTimeMillis())))

        // Then the card carries exactly that bitmap
        val card = (viewModel.state.value as HomeUiState.Content).cards.single()
        assertSame(bitmap, card.thumbnail)
        job.cancel()
    }

    @Test
    fun `no thumbnail is a null card slot - the warm placeholder, never a broken shelf`() = runTest {
        // Given a producer with nothing to give (unreadable document, render failure)
        val viewModel = viewModel()
        val job = launch(Dispatchers.Main) { viewModel.state.collect {} }

        // When
        repository.projects.emit(listOf(summary("z1", "One", System.currentTimeMillis())))

        // Then
        assertNull((viewModel.state.value as HomeUiState.Content).cards.single().thumbnail)
        assertTrue("the shelf still asked" , thumbnails.asked.contains("z1"))
        job.cancel()
    }

    @Test
    fun `every visible card is asked for - hidden pending-delete cards are not`() = runTest {
        // Given two zines on the shelf
        val now = System.currentTimeMillis()
        val viewModel = viewModel()
        val job = launch(Dispatchers.Main) { viewModel.state.collect {} }
        repository.projects.emit(listOf(summary("z1", "One", now), summary("z2", "Two", now)))
        assertTrue(thumbnails.asked.containsAll(listOf("z1", "z2")))

        // When z1 is hidden by a pending delete and the shelf re-emits
        viewModel.delete("z1")
        thumbnails.asked.clear()
        repository.projects.emit(listOf(summary("z1", "One", now), summary("z2", "Two", now)))

        // Then only the visible card is asked for (ADR-045 §2 / ADR-044 pending-delete interplay)
        assertTrue(thumbnails.asked.contains("z2"))
        assertTrue("hidden card must not be asked for", "z1" !in thumbnails.asked)
        job.cancel()
    }

    // --- S6.5 nav re-root (ADR-046) ---

    @Test
    fun `Start a zine emits exactly one open event with the created id`() = runTest {
        // Given
        val viewModel = viewModel()
        repository.createResult = { DataResult.Success(summary("new-id", "My zine", 0L)) }
        val opened = mutableListOf<String>()
        val openJob = launch(Dispatchers.Main) { viewModel.openEvents.collect { opened += it } }

        // When
        viewModel.startZine(PaperSize.LETTER)

        // Then — create → navigate (ADR-046 §5): the destination collects this and pushes EditorRoute
        assertEquals(listOf("new-id"), opened)
        openJob.cancel()
    }

    @Test
    fun `a failed create emits no open event - the warm message only`() = runTest {
        // Given
        val viewModel = viewModel()
        repository.createResult = { DataResult.Failure(DataError.Io("disk")) }
        val opened = mutableListOf<String>()
        val openJob = launch(Dispatchers.Main) { viewModel.openEvents.collect { opened += it } }
        val events = mutableListOf<HomeShelfEvent>()
        val eventsJob = launch(Dispatchers.Main) { viewModel.events.collect { events += it } }

        // When
        viewModel.startZine(PaperSize.LETTER)

        // Then
        assertTrue(opened.isEmpty())
        assertEquals(listOf<HomeShelfEvent>(HomeShelfEvent.Message(GENERIC_FAILURE_MESSAGE)), events)
        openJob.cancel()
        eventsJob.cancel()
    }

    @Test
    fun `Start a zine is single-flight - taps during an in-flight create are no-ops`() = runTest {
        // Given a create the test holds in flight (ADR-046 §5, Codex RF3: an unguarded double-tap
        // mints two projects and two navigations)
        val viewModel = viewModel()
        val gate = CompletableDeferred<Unit>()
        repository.createGate = gate
        repository.createResult = { DataResult.Success(summary("only-one", "My zine", 0L)) }
        val opened = mutableListOf<String>()
        val openJob = launch(Dispatchers.Main) { viewModel.openEvents.collect { opened += it } }

        // When — a rapid second (and third) tap while the first create is still in flight
        viewModel.startZine(PaperSize.LETTER)
        viewModel.startZine(PaperSize.LETTER)
        viewModel.startZine(PaperSize.LETTER)
        gate.complete(Unit)

        // Then — one project, one open event
        assertEquals(1, repository.created.size)
        assertEquals(listOf("only-one"), opened)
        openJob.cancel()
    }

    @Test
    fun `opening a card emits its open event`() = runTest {
        // Given
        val viewModel = viewModel()
        val opened = mutableListOf<String>()
        val openJob = launch(Dispatchers.Main) { viewModel.openEvents.collect { opened += it } }

        // When
        viewModel.openZine("z1")

        // Then
        assertEquals(listOf("z1"), opened)
        openJob.cancel()
    }

    @Test
    fun `leaving the shelf commits pending deletes before the open event`() = runTest {
        // Given a pending undoable delete (ADR-046 §4: leaving the shelf IS the snackbar dismissal —
        // otherwise the cancelled snackbar collector strands the card hidden forever, Codex RF1)
        val now = System.currentTimeMillis()
        val viewModel = viewModel()
        val stateJob = launch(Dispatchers.Main) { viewModel.state.collect {} }
        repository.projects.emit(listOf(summary("z1", "One", now), summary("z2", "Two", now)))
        viewModel.delete("z1")
        // Snapshot the committed deletes at the moment the open event is delivered — order, not just presence.
        val deletesAtOpen = mutableListOf<List<String>>()
        val openJob = launch(Dispatchers.Main) {
            viewModel.openEvents.collect { deletesAtOpen += repository.deleted.toList() }
        }

        // When — tapping the other card
        viewModel.openZine("z2")

        // Then — z1's delete was committed BEFORE the navigation event went out
        assertEquals(listOf(listOf("z1")), deletesAtOpen)
        stateJob.cancel()
        openJob.cancel()
    }

    @Test
    fun `Start a zine also commits pending deletes first`() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val viewModel = viewModel()
        val stateJob = launch(Dispatchers.Main) { viewModel.state.collect {} }
        repository.projects.emit(listOf(summary("z1", "One", now)))
        viewModel.delete("z1")
        repository.createResult = { DataResult.Success(summary("new-id", "My zine", 0L)) }

        // When
        viewModel.startZine(PaperSize.LETTER)

        // Then
        assertEquals(listOf("z1"), repository.deleted)
        stateJob.cancel()
    }

    @Test
    fun `an unrelated pending delete's commit failure unhides and messages but never blocks the open`() = runTest {
        // Given (ADR-046 §4, Codex round 2: the failed delete rolls back visibly; navigation proceeds)
        val now = System.currentTimeMillis()
        val viewModel = viewModel()
        repository.deleteResult = { DataResult.Failure(DataError.Io("unindex failed")) }
        val stateJob = launch(Dispatchers.Main) { viewModel.state.collect {} }
        val events = mutableListOf<HomeShelfEvent>()
        val eventsJob = launch(Dispatchers.Main) { viewModel.events.collect { events += it } }
        val opened = mutableListOf<String>()
        val openJob = launch(Dispatchers.Main) { viewModel.openEvents.collect { opened += it } }
        repository.projects.emit(listOf(summary("z1", "One", now), summary("z2", "Two", now)))
        viewModel.delete("z1")

        // When
        viewModel.openZine("z2")

        // Then — the open went out, the failed delete is back on the shelf with its warm message
        assertEquals(listOf("z2"), opened)
        assertEquals(
            listOf("z1", "z2"),
            (viewModel.state.value as HomeUiState.Content).cards.map { it.id }.sorted(),
        )
        assertEquals(HomeShelfEvent.Message(GENERIC_FAILURE_MESSAGE), events.last())
        stateJob.cancel()
        eventsJob.cancel()
        openJob.cancel()
    }

    @Test
    fun `an open buffered while nobody collected never fires on shelf return`() = runTest {
        // Given an open that landed while the shelf was not collecting (e.g. a slow create finishing
        // after the user already tapped into another editor) — Codex implementation-review RF: a
        // stale open must never re-navigate on return; navigation is a FRESH user action.
        val viewModel = viewModel()
        viewModel.openZine("stale")

        // When the shelf destination comes back and re-subscribes
        val opened = mutableListOf<String>()
        val openJob = launch(Dispatchers.Main) { viewModel.openEvents.collect { opened += it } }

        // Then the stale open is discarded, and a fresh tap still opens normally
        assertTrue(opened.isEmpty())
        viewModel.openZine("fresh")
        assertEquals(listOf("fresh"), opened)
        openJob.cancel()
    }

    @Test
    fun `committed pending ids are pruned once the store emission drops them`() = runTest {
        // Given a committed delete the store has caught up with (Codex implementation-review Rec:
        // the Home VM is process-lifetime now — stale hidden ids must not accumulate forever)
        val now = System.currentTimeMillis()
        val viewModel = viewModel()
        val stateJob = launch(Dispatchers.Main) { viewModel.state.collect {} }
        repository.projects.emit(listOf(summary("z1", "One", now), summary("z2", "Two", now)))
        viewModel.delete("z1")
        viewModel.commitDelete("z1")
        repository.projects.emit(listOf(summary("z2", "Two", now)))

        // When the same id later reappears in the store
        repository.projects.emit(listOf(summary("z1", "One again", now), summary("z2", "Two", now)))

        // Then it is visible — the stale pending id was pruned, not left filtering forever
        assertEquals(
            listOf("z1", "z2"),
            (viewModel.state.value as HomeUiState.Content).cards.map { it.id }.sorted(),
        )
        stateJob.cancel()
    }

    @Test
    fun `returning to the shelf re-collects the store - fresh labels and thumbnails`() = runTest {
        // Given a first shelf visit (ADR-046 §6: WhileSubscribed(0) — a warm 5 s window used to keep
        // the stale upstream alive across the most common edit → back round-trip, ADR-045 §6)
        val viewModel = viewModel()
        val firstVisit = launch(Dispatchers.Main) { viewModel.state.collect {} }
        repository.projects.emit(listOf(summary("z1", "One", System.currentTimeMillis())))
        assertEquals(1, repository.observeCollections)

        // When the shelf is left (collection stops) and immediately returned to — NO virtual time passes
        firstVisit.cancel()
        val secondVisit = launch(Dispatchers.Main) { viewModel.state.collect {} }

        // Then the upstream was re-collected: the store is re-read, labels/thumbnails re-derived
        assertEquals(2, repository.observeCollections)
        secondVisit.cancel()
    }

    // --- the recency label, a pure function ---

    @Test
    fun `edited label speaks human - just now, minutes, hours, yesterday, days`() {
        val now = 1_000_000_000_000L
        assertEquals("Edited just now", editedLabel(now - 30_000L, now))
        assertEquals("Edited 1 minute ago", editedLabel(now - 60_000L, now))
        assertEquals("Edited 59 minutes ago", editedLabel(now - 59 * 60_000L, now))
        assertEquals("Edited 1 hour ago", editedLabel(now - 60 * 60_000L, now))
        assertEquals("Edited 23 hours ago", editedLabel(now - 23 * 3_600_000L, now))
        assertEquals("Edited yesterday", editedLabel(now - 24 * 3_600_000L, now))
        assertEquals("Edited yesterday", editedLabel(now - 47 * 3_600_000L, now))
        assertEquals("Edited 2 days ago", editedLabel(now - 48 * 3_600_000L, now))
        assertEquals("Edited 30 days ago", editedLabel(now - 30 * 86_400_000L, now))
    }

    @Test
    fun `a future timestamp (clock skew) is just now, never negative time`() {
        val now = 1_000_000_000_000L
        assertEquals("Edited just now", editedLabel(now + 60_000L, now))
    }

    @Test
    fun `singular and plural read correctly`() {
        val now = 1_000_000_000_000L
        assertTrue(editedLabel(now - 2 * 60_000L, now).contains("2 minutes"))
        assertTrue(editedLabel(now - 2 * 3_600_000L, now).contains("2 hours"))
        assertTrue(editedLabel(now - 3 * 86_400_000L, now).contains("3 days"))
    }
}
