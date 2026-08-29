package com.aritr.zinely.home

import android.net.Uri
import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.ProjectShelfEntry
import com.aritr.zinely.core.data.repository.ProjectRepository
import com.aritr.zinely.core.data.repository.ProjectSummary
import com.aritr.zinely.core.data.repository.ProjectUnavailableReason
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.data.android.LibraryBackupReceipt
import com.aritr.zinely.data.android.LibraryRestoreReceipt
import com.aritr.zinely.data.android.LibrarySafTransport
import com.aritr.zinely.data.android.prefs.PreferredPaperStore
import com.aritr.zinely.data.android.RestoredProject
import com.aritr.zinely.feature.editor.HomeShelfEvent
import com.aritr.zinely.feature.library.LibraryBackupRestoreFailureKind
import com.aritr.zinely.feature.library.LibraryBackupRestoreMode
import com.aritr.zinely.feature.library.LibraryBackupRestoreUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for the S6.2 read-only Home shelf ViewModel (ADR-043). Given-When-Then; a hand fake
 * stands in for the [ProjectRepository] so the Loading→Empty/Content mapping and the warm card
 * labels are verified with no Android / Hilt. The fake's flow is **cold-until-emitted** (a replaying
 * SharedFlow, not a StateFlow), so the Loading-first assertion is real, not incidental (Codex).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
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
            emitAll(projects)
        }

        var shelfProjection: (List<ProjectSummary>) -> List<ProjectShelfEntry> =
            { items -> items.map(ProjectShelfEntry::Available) }

        override fun observeShelfProjects(): Flow<List<ProjectShelfEntry>> = flow {
            observeCollections++
            observeFailure?.let { failure ->
                observeFailure = null // one scripted failure: a retry must be able to succeed
                throw failure
            }
            emitAll(projects.map(shelfProjection))
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

    private class FakeLibrarySafTransport : LibrarySafTransport {
        val backupDestinations = mutableListOf<Uri>()
        val restoreSources = mutableListOf<Uri>()

        var backupGate: CompletableDeferred<Unit>? = null
        var restoreGate: CompletableDeferred<Unit>? = null
        var backupResult: DataResult<LibraryBackupReceipt> = DataResult.Success(
            LibraryBackupReceipt(projectCount = 2, assetCount = 3, archiveByteCount = 1024L),
        )
        var restoreResult: DataResult<LibraryRestoreReceipt> = DataResult.Success(
            LibraryRestoreReceipt(projects = emptyList()),
        )

        override suspend fun backupTo(destination: Uri): DataResult<LibraryBackupReceipt> {
            backupDestinations += destination
            backupGate?.await()
            return backupResult
        }

        override suspend fun restoreFrom(source: Uri): DataResult<LibraryRestoreReceipt> {
            restoreSources += source
            restoreGate?.await()
            return restoreResult
        }
    }

    private class FakePreferredPaperStore : PreferredPaperStore {
        private val paper = MutableStateFlow(PaperSize.A4)
        override val preferredPaperSize: Flow<PaperSize> = paper
        override suspend fun setPreferredPaperSize(paperSize: PaperSize) {
            paper.value = paperSize
        }
    }

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeProjectRepository
    private lateinit var transport: FakeLibrarySafTransport
    private lateinit var preferredPaperStore: FakePreferredPaperStore

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeProjectRepository()
        transport = FakeLibrarySafTransport()
        preferredPaperStore = FakePreferredPaperStore()
    }

    private fun viewModel() = HomeViewModel(repository, transport, preferredPaperStore)

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
    fun `preferred paper is exposed and persisted without creating a zine`() = runTest {
        val viewModel = viewModel()

        assertEquals(PaperSize.A4, viewModel.preferredPaper.value)
        viewModel.setPreferredPaper(PaperSize.LETTER)

        assertEquals(PaperSize.LETTER, viewModel.preferredPaper.value)
        assertTrue(repository.created.isEmpty())
    }

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
        // See LibraryZineMappingTest: the shelf's paper label now comes from `Copy.Paper`, where the
        // size is "US Letter".
        assertEquals("8-page mini · US Letter", card.formatLabel)
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
    fun `an unavailable zine stays visible while the healthy card path excludes it`() = runTest {
        val now = System.currentTimeMillis()
        val viewModel = viewModel()
        repository.shelfProjection = { items ->
            listOf(
                ProjectShelfEntry.Available(items.first()),
                ProjectShelfEntry.Unavailable(
                    id = "z2",
                    title = "Broken one",
                    paperSize = PaperSize.A4,
                    updatedAtEpochMs = now,
                    cover = null,
                    reason = ProjectUnavailableReason.CORRUPT,
                ),
            )
        }

        val job = launch(Dispatchers.Main) { viewModel.state.collect {} }
        repository.projects.emit(
            listOf(
                summary("z1", "Healthy one", now),
                summary("z2", "Broken one", now, paperSize = PaperSize.A4),
            ),
        )

        val content = viewModel.state.value as HomeUiState.Content
        assertEquals(listOf("z1"), content.cards.map { it.id })
        assertEquals(listOf("z1", "z2"), content.zines.map { it.id })
        assertEquals(Copy.Shelf.UNAVAILABLE_DAMAGED, content.zines.last().unavailableReason)
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
    fun `flush pending deletes commits each hidden zine`() = runTest {
        val now = System.currentTimeMillis()
        val viewModel = viewModel()
        val stateJob = launch(Dispatchers.Main) { viewModel.state.collect {} }
        repository.projects.emit(listOf(summary("z1", "One", now), summary("z2", "Two", now)))
        viewModel.delete("z1")
        viewModel.delete("z2")

        viewModel.flushPendingDeletes()

        assertEquals(listOf("z1", "z2"), repository.deleted)
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
    fun `returning to the shelf re-collects the store - fresh labels`() = runTest {
        // Given a first shelf visit (ADR-046 §6: WhileSubscribed(0) — a warm 5 s window used to keep
        // the stale upstream alive across the most common edit → back round-trip, ADR-045 §6)
        val viewModel = viewModel()
        val firstVisit = launch(Dispatchers.Main) { viewModel.state.collect {} }
        repository.projects.emit(listOf(summary("z1", "One", System.currentTimeMillis())))
        assertEquals(1, repository.observeCollections)

        // When the shelf is left (collection stops) and immediately returned to — NO virtual time passes
        firstVisit.cancel()
        val secondVisit = launch(Dispatchers.Main) { viewModel.state.collect {} }

        // Then the upstream was re-collected: the store is re-read and labels re-derived
        assertEquals(2, repository.observeCollections)
        secondVisit.cancel()
    }

    // --- library backup / restore orchestration ---

    @Test
    fun `backup asks for a user-owned destination only when the visible shelf has a zine`() = runTest {
        val viewModel = viewModel()
        val stateJob = launch(Dispatchers.Main) { viewModel.state.collect {} }
        val requests = mutableListOf<LibraryBackupRestorePickerRequest>()
        val requestJob = launch(Dispatchers.Main) {
            viewModel.backupRestorePickerRequests.collect { requests += it }
        }

        repository.projects.emit(emptyList())
        viewModel.startBackup()
        assertTrue(requests.isEmpty())

        repository.projects.emit(listOf(summary("z1", "One", System.currentTimeMillis())))
        viewModel.startBackup()

        val request = requests.single() as LibraryBackupRestorePickerRequest.Backup
        assertTrue(request.suggestedName.startsWith("zinely-backup-"))
        assertTrue(request.suggestedName.endsWith(".zine"))
        assertEquals(null, viewModel.backupRestoreState.value)
        stateJob.cancel()
        requestJob.cancel()
    }

    @Test
    fun `restore can begin from an empty shelf and picker cancellation is silent`() = runTest {
        val viewModel = viewModel()
        val requests = mutableListOf<LibraryBackupRestorePickerRequest>()
        val requestJob = launch(Dispatchers.Main) {
            viewModel.backupRestorePickerRequests.collect { requests += it }
        }
        val events = mutableListOf<HomeShelfEvent>()
        val eventJob = launch(Dispatchers.Main) { viewModel.events.collect { events += it } }

        viewModel.startRestore()
        assertEquals(listOf(LibraryBackupRestorePickerRequest.Restore), requests)

        viewModel.restorePicked(null)
        assertEquals(null, viewModel.backupRestoreState.value)
        assertTrue(transport.restoreSources.isEmpty())
        assertTrue(events.isEmpty())

        // Null clears the pending-picker guard; a later intentional tap still opens the picker.
        viewModel.startRestore()
        assertEquals(2, requests.size)
        requestJob.cancel()
        eventJob.cancel()
    }

    @Test
    fun `picker request is single-flight until its callback clears the pending gate`() = runTest {
        val viewModel = viewModel()
        val stateJob = launch(Dispatchers.Main) { viewModel.state.collect {} }
        repository.projects.emit(listOf(summary("z1", "One", System.currentTimeMillis())))
        val requests = mutableListOf<LibraryBackupRestorePickerRequest>()
        val requestJob = launch(Dispatchers.Main) {
            viewModel.backupRestorePickerRequests.collect { requests += it }
        }

        // The first request owns the system picker. Rapid same-mode and cross-mode taps cannot open
        // competing pickers before Android returns a result.
        viewModel.startRestore()
        viewModel.startRestore()
        viewModel.startBackup()
        assertEquals(listOf(LibraryBackupRestorePickerRequest.Restore), requests)

        // A cancelled picker is still a callback and releases the guard for the next intentional tap.
        viewModel.restorePicked(null)
        viewModel.startBackup()
        assertEquals(2, requests.size)
        assertTrue(requests.last() is LibraryBackupRestorePickerRequest.Backup)
        stateJob.cancel()
        requestJob.cancel()
    }

    @Test
    fun `picker launch failure clears the gate and offers a useful retry state`() = runTest {
        val viewModel = viewModel()
        val requests = mutableListOf<LibraryBackupRestorePickerRequest>()
        val requestJob = launch(Dispatchers.Main) {
            viewModel.backupRestorePickerRequests.collect { requests += it }
        }

        viewModel.startRestore()
        assertEquals(listOf(LibraryBackupRestorePickerRequest.Restore), requests)

        viewModel.backupRestorePickerFailed(LibraryBackupRestoreMode.Restore)
        assertEquals(
            LibraryBackupRestoreUiState.Failed(
                mode = LibraryBackupRestoreMode.Restore,
                kind = LibraryBackupRestoreFailureKind.ReadFailed,
            ),
            viewModel.backupRestoreState.value,
        )

        viewModel.retryBackupRestore()
        assertEquals(2, requests.size)
        requestJob.cancel()
    }

    @Test
    fun `successful backup reports what was saved`() = runTest {
        val viewModel = viewModel()
        val uri = Uri.parse("content://zinely-tests/backup")
        transport.backupResult = DataResult.Success(
            LibraryBackupReceipt(projectCount = 4, assetCount = 7, archiveByteCount = 4096L),
        )

        viewModel.backupPicked(uri)

        assertEquals(listOf(uri), transport.backupDestinations)
        assertEquals(
            LibraryBackupRestoreUiState.BackupSaved(projectCount = 4, assetCount = 7),
            viewModel.backupRestoreState.value,
        )
    }

    @Test
    fun `successful additive restore reports the restored project count`() = runTest {
        val viewModel = viewModel()
        val uri = Uri.parse("content://zinely-tests/restore")
        val restored = summary("restored", "Returned zine", 0L)
        transport.restoreResult = DataResult.Success(
            LibraryRestoreReceipt(
                projects = listOf(RestoredProject(sourceProjectId = "source-id", project = restored)),
            ),
        )

        viewModel.restorePicked(uri)

        assertEquals(listOf(uri), transport.restoreSources)
        assertEquals(
            LibraryBackupRestoreUiState.RestoreAdded(restoredProjectCount = 1),
            viewModel.backupRestoreState.value,
        )
    }

    @Test
    fun `restore failures map repository detail to stable product error families`() = runTest {
        val cases = listOf(
            DataError.Corrupt("tampered") to LibraryBackupRestoreFailureKind.Damaged,
            DataError.SchemaTooNew(documentVersion = 3, supportedVersion = 2) to
                LibraryBackupRestoreFailureKind.NewerAppNeeded,
            DataError.OutOfSpace("full") to LibraryBackupRestoreFailureKind.NotEnoughSpace,
            DataError.Busy("writer active") to LibraryBackupRestoreFailureKind.Busy,
            DataError.Io("provider failed") to LibraryBackupRestoreFailureKind.ReadFailed,
            DataError.Unknown("unexpected") to LibraryBackupRestoreFailureKind.Generic,
        )
        val viewModel = viewModel()

        cases.forEachIndexed { index, (error, expectedKind) ->
            transport.restoreResult = DataResult.Failure(error)
            viewModel.restorePicked(Uri.parse("content://zinely-tests/failure-$index"))
            assertEquals(
                LibraryBackupRestoreUiState.Failed(
                    mode = LibraryBackupRestoreMode.Restore,
                    kind = expectedKind,
                ),
                viewModel.backupRestoreState.value,
            )
            viewModel.dismissBackupRestoreSurface()
        }
    }

    @Test
    fun `backup IO failure is described as save failure`() = runTest {
        val viewModel = viewModel()
        transport.backupResult = DataResult.Failure(DataError.Io("provider failed"))

        viewModel.backupPicked(Uri.parse("content://zinely-tests/unwritable"))

        assertEquals(
            LibraryBackupRestoreUiState.Failed(
                mode = LibraryBackupRestoreMode.Backup,
                kind = LibraryBackupRestoreFailureKind.SaveFailed,
            ),
            viewModel.backupRestoreState.value,
        )
    }

    @Test
    fun `a running transfer is single-flight and explicit cancellation clears it`() = runTest {
        val viewModel = viewModel()
        val gate = CompletableDeferred<Unit>()
        transport.backupGate = gate
        val events = mutableListOf<HomeShelfEvent>()
        val eventJob = launch(Dispatchers.Main) { viewModel.events.collect { events += it } }

        viewModel.backupPicked(Uri.parse("content://zinely-tests/slow-backup"))
        assertEquals(
            LibraryBackupRestoreUiState.Running(LibraryBackupRestoreMode.Backup),
            viewModel.backupRestoreState.value,
        )

        viewModel.backupPicked(Uri.parse("content://zinely-tests/second-backup"))
        viewModel.restorePicked(Uri.parse("content://zinely-tests/restore-during-backup"))
        assertEquals(1, transport.backupDestinations.size)
        assertTrue(transport.restoreSources.isEmpty())

        viewModel.cancelBackupRestore()
        assertEquals(null, viewModel.backupRestoreState.value)
        assertEquals(
            listOf<HomeShelfEvent>(HomeShelfEvent.Message("Backup cancelled.")),
            events,
        )
        eventJob.cancel()
    }

    @Test
    fun `retry after a restore failure asks for the file again and can succeed`() = runTest {
        val viewModel = viewModel()
        val requests = mutableListOf<LibraryBackupRestorePickerRequest>()
        val requestJob = launch(Dispatchers.Main) {
            viewModel.backupRestorePickerRequests.collect { requests += it }
        }
        transport.restoreResult = DataResult.Failure(DataError.Corrupt("damaged"))
        viewModel.restorePicked(Uri.parse("content://zinely-tests/damaged"))
        assertTrue(viewModel.backupRestoreState.value is LibraryBackupRestoreUiState.Failed)

        viewModel.retryBackupRestore()
        assertEquals(listOf(LibraryBackupRestorePickerRequest.Restore), requests)

        transport.restoreResult = DataResult.Success(LibraryRestoreReceipt(emptyList()))
        viewModel.restorePicked(Uri.parse("content://zinely-tests/good"))
        assertEquals(
            LibraryBackupRestoreUiState.RestoreAdded(restoredProjectCount = 0),
            viewModel.backupRestoreState.value,
        )
        requestJob.cancel()
    }

    @Test
    fun `restore commits pending deletes before opening the picker`() = runTest {
        val now = System.currentTimeMillis()
        val viewModel = viewModel()
        val stateJob = launch(Dispatchers.Main) { viewModel.state.collect {} }
        repository.projects.emit(listOf(summary("z1", "One", now)))
        viewModel.delete("z1")
        val deletesAtPicker = mutableListOf<List<String>>()
        val requestJob = launch(Dispatchers.Main) {
            viewModel.backupRestorePickerRequests.collect {
                deletesAtPicker += repository.deleted.toList()
            }
        }

        viewModel.startRestore()

        assertEquals(listOf(listOf("z1")), deletesAtPicker)
        stateJob.cancel()
        requestJob.cancel()
    }

    @Test
    fun `a failed pending delete prevents restore picker from seeing a dishonest shelf`() = runTest {
        val now = System.currentTimeMillis()
        val viewModel = viewModel()
        val stateJob = launch(Dispatchers.Main) { viewModel.state.collect {} }
        repository.projects.emit(listOf(summary("z1", "One", now)))
        repository.deleteResult = { DataResult.Failure(DataError.Io("delete failed")) }
        viewModel.delete("z1")
        val requests = mutableListOf<LibraryBackupRestorePickerRequest>()
        val requestJob = launch(Dispatchers.Main) {
            viewModel.backupRestorePickerRequests.collect { requests += it }
        }

        viewModel.startRestore()

        assertTrue(requests.isEmpty())
        assertEquals(listOf("z1"), (viewModel.state.value as HomeUiState.Content).cards.map { it.id })
        stateJob.cancel()
        requestJob.cancel()
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
