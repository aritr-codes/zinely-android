package com.aritr.zinely.home

import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.ProjectRepository
import com.aritr.zinely.core.data.repository.ProjectSummary
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineFormat
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

    /** Read-only shelf fake: scripts [observeProjects]; every mutation is an unused-path error. */
    private class FakeProjectRepository : ProjectRepository {
        val projects = MutableSharedFlow<List<ProjectSummary>>(replay = 1)

        override fun observeProjects(): Flow<List<ProjectSummary>> = projects

        override suspend fun getProject(id: String): DataResult<ProjectSummary> =
            error("not used by the read-only shelf")

        override suspend fun createProject(
            title: String,
            format: ZineFormat,
            paperSize: PaperSize,
        ): DataResult<ProjectSummary> = error("no mutation UI in S6.2 (ADR-042 invariant)")

        override suspend fun renameProject(id: String, title: String): DataResult<Unit> =
            error("no mutation UI in S6.2 (ADR-042 invariant)")

        override suspend fun duplicateProject(id: String): DataResult<ProjectSummary> =
            error("no mutation UI in S6.2 (ADR-042 invariant)")

        override suspend fun deleteProject(id: String): DataResult<Unit> =
            error("no mutation UI in S6.2 (ADR-042 invariant)")
    }

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeProjectRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeProjectRepository()
    }

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
        val viewModel = HomeViewModel(repository)

        val job = launch(Dispatchers.Main) { viewModel.state.collect {} }
        assertEquals(HomeUiState.Loading, viewModel.state.value)
        job.cancel()
    }

    @Test
    fun `an empty store is the Empty shelf, never a zero-card Content`() = runTest {
        val viewModel = HomeViewModel(repository)

        val job = launch(Dispatchers.Main) { viewModel.state.collect {} }
        repository.projects.emit(emptyList())

        assertEquals(HomeUiState.Empty, viewModel.state.value)
        job.cancel()
    }

    @Test
    fun `projects become warm cards - id, title, format and recency labels`() = runTest {
        val fiveMinutesAgo = System.currentTimeMillis() - 5 * 60_000L
        val viewModel = HomeViewModel(repository)

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
        val viewModel = HomeViewModel(repository)

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
        val viewModel = HomeViewModel(repository)

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
        val viewModel = HomeViewModel(repository)

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
