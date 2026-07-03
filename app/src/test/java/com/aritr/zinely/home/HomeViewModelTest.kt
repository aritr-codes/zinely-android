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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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

        var createResult: () -> DataResult<ProjectSummary> =
            { error("script createResult when the test asserts on it") }
        var renameResult: () -> DataResult<Unit> = { DataResult.Success(Unit) }
        var duplicateResult: () -> DataResult<ProjectSummary> =
            { error("script duplicateResult when the test asserts on it") }
        var deleteResult: () -> DataResult<Unit> = { DataResult.Success(Unit) }

        override fun observeProjects(): Flow<List<ProjectSummary>> = projects

        override suspend fun getProject(id: String): DataResult<ProjectSummary> =
            error("not used by the shelf")

        override suspend fun createProject(
            title: String,
            format: ZineFormat,
            paperSize: PaperSize,
        ): DataResult<ProjectSummary> {
            created += Triple(title, format, paperSize)
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
        viewModel.startZine()

        // Then — "My zine", the only format, and the bootstrap-matching paper (ADR-044 §4)
        assertEquals(
            listOf(Triple("My zine", ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER)),
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
        viewModel.startZine()

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
