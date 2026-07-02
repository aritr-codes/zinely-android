package com.aritr.zinely.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aritr.zinely.core.data.repository.ProjectRepository
import com.aritr.zinely.core.data.repository.ProjectSummary
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.feature.editor.HomeZineCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * What the Home shelf shows (ADR-043). [Loading] until the project store first answers (so the
 * empty invitation never flashes); [Empty] is a real state, distinct from a zero-card [Content].
 */
internal sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Empty : HomeUiState
    data class Content(val cards: List<HomeZineCard>) : HomeUiState
}

/**
 * The S6.2 read-only Home · "My zines" shelf (ADR-043; MVVM — ADR-005 scoped MVI to the editor,
 * and this screen only observes). Maps the [ProjectRepository]'s newest-first
 * [ProjectSummary] stream to warm [HomeZineCard]s — **order passed through untouched** (ordering
 * is the ADR-042 §7 repository contract, never re-derived here). Mutations are S6.3; this VM
 * holds no reference to any mutating call. Recency labels are computed at emission and go stale
 * until the next one — accepted for a shelf you just navigated to (fresh subscription = fresh
 * labels via WhileSubscribed); a ticking clock is not this slice's problem.
 */
@HiltViewModel
internal class HomeViewModel @Inject constructor(
    projectRepository: ProjectRepository,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = projectRepository.observeProjects()
        .map { projects ->
            if (projects.isEmpty()) {
                HomeUiState.Empty
            } else {
                val now = System.currentTimeMillis()
                HomeUiState.Content(projects.map { it.toCard(now) })
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)
}

/** `ProjectSummary` → the card the shelf shows: only display strings past this point (ADR-043). */
internal fun ProjectSummary.toCard(nowEpochMs: Long): HomeZineCard = HomeZineCard(
    id = id,
    title = title,
    formatLabel = "${format.shelfLabel()} · ${paperSize.shelfLabel()}",
    editedLabel = editedLabel(updatedAtEpochMs, nowEpochMs),
)

/** Warm, jargon-free format name (never the enum's SCREAMING_SNAKE identity). */
private fun ZineFormat.shelfLabel(): String = when (this) {
    ZineFormat.SINGLE_SHEET_8 -> "8-page mini"
}

/** Paper-size name as people say it. */
private fun PaperSize.shelfLabel(): String = when (this) {
    PaperSize.LETTER -> "Letter"
    PaperSize.A4 -> "A4"
}

/**
 * The human recency line for a card: "Edited just now / N minutes / N hours ago / yesterday /
 * N days ago". Pure — `now` is a parameter, so tests need no clock seam (Codex). A future
 * timestamp (clock skew) clamps to "just now"; never negative time.
 */
internal fun editedLabel(updatedAtEpochMs: Long, nowEpochMs: Long): String {
    val elapsedMs = (nowEpochMs - updatedAtEpochMs).coerceAtLeast(0L)
    val minutes = elapsedMs / 60_000L
    val hours = elapsedMs / 3_600_000L
    val days = elapsedMs / 86_400_000L
    return when {
        minutes < 1 -> "Edited just now"
        hours < 1 -> "Edited $minutes ${plural(minutes, "minute")} ago"
        days < 1 -> "Edited $hours ${plural(hours, "hour")} ago"
        days < 2 -> "Edited yesterday"
        else -> "Edited $days days ago"
    }
}

private fun plural(n: Long, unit: String): String = if (n == 1L) unit else "${unit}s"
