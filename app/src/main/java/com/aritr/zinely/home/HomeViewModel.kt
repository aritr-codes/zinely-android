package com.aritr.zinely.home

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.ProjectRepository
import com.aritr.zinely.core.data.repository.ProjectSummary
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.feature.editor.HomeShelfEvent
import com.aritr.zinely.feature.editor.HomeZineCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
 * The Home · "My zines" shelf (read shelf: ADR-043; actions: ADR-044; MVVM — ADR-005 scoped MVI to
 * the editor). Maps the [ProjectRepository]'s newest-first [ProjectSummary] stream to warm
 * [HomeZineCard]s — **order passed through untouched** (the ADR-042 §7 repository contract, never
 * re-derived here). Recency labels are computed at emission and go stale until the next one —
 * accepted for a shelf you just navigated to (fresh subscription = fresh labels via
 * WhileSubscribed); a ticking clock is not this slice's problem.
 *
 * S6.3 actions (ADR-044): create with warm defaults; rename/duplicate delegating to the store
 * (which enforces the open-editor exclusion and answers [DataError.Busy]); delete as a deferred
 * commit — the card hides immediately, a queued [HomeShelfEvent.DeletePrompt] drives one undo
 * snackbar, and only its dismissal calls [ProjectRepository.deleteProject]. A failed commit
 * unhides the card: the shelf never lies about what was deleted. [HomeUiState.Empty] means the
 * STORE is empty; a shelf filtered to zero by pending deletes stays a zero-card
 * [HomeUiState.Content] (the invitation would be dishonest while a delete is still reversible).
 */
@HiltViewModel
internal class HomeViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val shelfThumbnails: ShelfThumbnails,
) : ViewModel() {

    /** Ids hidden from the shelf while their undo window is open (ADR-044 §3). */
    private val pendingDeletes = MutableStateFlow<Set<String>>(emptySet())

    /** Delivered page-1 thumbnails by project id (ADR-045); absent = warm placeholder on the card. */
    private val thumbnails = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())

    /** Ids with an ensure() in flight — one request per id at a time; emissions re-ask when done. */
    private val thumbnailsInFlight = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    /** Queued one-shot events; the buffer absorbs emissions while the screen is between collects. */
    private val eventQueue = Channel<HomeShelfEvent>(Channel.BUFFERED)

    /** One-shot shelf events (undo prompts, warm failure messages), each consumed exactly once. */
    val events: Flow<HomeShelfEvent> = eventQueue.receiveAsFlow()

    /** Queued one-shot "open this project" navigation events (ADR-046 §5) — consumed by the nav
     * destination (navigation is a destination-layer concern), never by [HomeScreen] itself. */
    private val openQueue = Channel<String>(Channel.BUFFERED)

    /**
     * One-shot open-project ids; the destination collects and navigates `EditorRoute(id)`. Each
     * (re)collection first **discards anything buffered while nobody was collecting** (Codex
     * implementation-review Required Fix): an open that landed after the shelf left composition —
     * e.g. a slow create finishing behind an already-taken card tap — must never auto-navigate on
     * return. Navigation is a fresh user action; a dropped stale open costs one re-tap, data-safe.
     */
    val openEvents: Flow<String> = flow {
        while (openQueue.tryReceive().isSuccess) {
            // discard stale opens buffered between shelf visits
        }
        emitAll(openQueue.receiveAsFlow())
    }

    /** The in-flight create (ADR-046 §5 single-flight): taps during it are no-ops. */
    private var createJob: Job? = null

    val state: StateFlow<HomeUiState> =
        combine(
            projectRepository.observeProjects(),
            pendingDeletes,
            thumbnails,
        ) { projects, pending, thumbs ->
            if (projects.isEmpty()) {
                HomeUiState.Empty
            } else {
                val now = System.currentTimeMillis()
                // Prune pending ids the store no longer knows (committed deletes it caught up with):
                // this VM is process-lifetime since the ADR-046 re-root, so "stale ids are inert"
                // is no longer enough — they must not accumulate forever (Codex). The update
                // re-triggers this combine once; the second pass is a no-op and it converges.
                val projectIds = projects.mapTo(HashSet()) { it.id }
                if (pending.any { it !in projectIds }) {
                    pendingDeletes.update { current -> current.filterTo(mutableSetOf()) { it in projectIds } }
                }
                val visible = projects.filterNot { it.id in pending }
                // Ask for every visible card's thumbnail on every emission (ADR-045 §2): the
                // producer's mtime stamp makes a fresh ask a cheap no-op, delivery lands in
                // [thumbnails] which re-runs this combine, and identical results change nothing —
                // the loop converges. Hidden pending-delete cards are never asked for.
                visible.forEach { requestThumbnail(it.id) }
                HomeUiState.Content(visible.map { it.toCard(now, thumbs[it.id]) })
            }
        // WhileSubscribed(0), not 5_000 (ADR-046 §6): every return to the shelf re-collects the
        // upstream, so max(row, doc mtime) recency and thumbnails re-derive after an editor round-trip
        // — the store emits nothing on autosave mtime changes, so a warm subscription shows stale
        // cards on exactly the most common flow. stateIn keeps the last value: no Loading flash.
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), HomeUiState.Loading)

    /** Launch one ensure() per id at a time; the map updates only when the bitmap actually changes. */
    private fun requestThumbnail(id: String) {
        if (!thumbnailsInFlight.add(id)) return
        viewModelScope.launch {
            try {
                when (val bitmap = shelfThumbnails.ensure(id)) {
                    null -> thumbnails.update { if (id in it) it - id else it }
                    else -> thumbnails.update { if (it[id] === bitmap) it else it + (id to bitmap) }
                }
            } finally {
                thumbnailsInFlight.remove(id)
            }
        }
    }

    /**
     * Open a card's project: commit any pending deletes first (leaving the shelf is a snackbar
     * dismissal — ADR-046 §4), then hand the id to the destination as a one-shot open event. A hidden
     * pending-delete card is untappable, so [id] can never itself be pending.
     */
    fun openZine(id: String) {
        viewModelScope.launch {
            commitPendingDeletesNow()
            openQueue.send(id)
        }
    }

    /**
     * "Start a zine": create with the warm defaults, then navigate into the new zine via the same
     * one-shot open path (ADR-046 §5). Single-flight — a tap while a create is in flight is a no-op
     * (an unguarded double-tap would mint two projects and two navigations); pending deletes commit
     * first (ADR-046 §4). Create failure keeps the warm message and emits no open event.
     */
    fun startZine() {
        if (createJob?.isActive == true) return
        createJob = viewModelScope.launch {
            commitPendingDeletesNow()
            when (val result =
                projectRepository.createProject(DEFAULT_NEW_TITLE, ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER)
            ) {
                is DataResult.Success -> openQueue.send(result.value.id)
                is DataResult.Failure -> eventQueue.send(HomeShelfEvent.Message(result.error.warmMessage()))
            }
        }
    }

    /** Rename with the ADR-044 §4 normalisation: trimmed; blank keeps the existing name. */
    fun rename(id: String, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { projectRepository.renameProject(id, trimmed).sendMessageOnFailure() }
    }

    fun duplicate(id: String) {
        viewModelScope.launch { projectRepository.duplicateProject(id).sendMessageOnFailure() }
    }

    /**
     * Hide the card and prompt for undo — the store is untouched until [commitDelete]. Ignored when
     * [id] is not a visible card (double-tap, or already pending).
     */
    fun delete(id: String) {
        val cards = (state.value as? HomeUiState.Content)?.cards ?: return
        val title = cards.firstOrNull { it.id == id }?.title ?: return
        pendingDeletes.update { it + id }
        eventQueue.trySend(HomeShelfEvent.DeletePrompt(id, title))
    }

    /** Undo within the window: unhide; the store was never called. */
    fun undoDelete(id: String) {
        pendingDeletes.update { it - id }
    }

    /**
     * The undo window closed: perform the store delete. On success the id STAYS in
     * [pendingDeletes] — unhiding here would flash the deleted card back for the window between
     * `deleteProject` returning and [ProjectRepository.observeProjects] re-emitting; once the flow
     * emits the shorter list the filter is a no-op (a stale id over a fresh-UUID store is inert).
     * Only a failed commit unhides + messages: the card is still real, and the shelf never lies.
     */
    fun commitDelete(id: String) {
        viewModelScope.launch { performCommit(id) }
    }

    /** The one commit path [commitDelete] and [commitPendingDeletesNow] share. */
    private suspend fun performCommit(id: String) {
        when (val result = projectRepository.deleteProject(id)) {
            is DataResult.Success -> Unit // stay hidden; the store flow removes the card
            is DataResult.Failure -> {
                pendingDeletes.update { it - id } // the card is still real — show it again
                eventQueue.send(HomeShelfEvent.Message(result.error.warmMessage()))
            }
        }
    }

    /**
     * Commit every pending delete before leaving the shelf (ADR-046 §4): navigating away cancels the
     * snackbar collector, so neither undo nor commit would ever run — this awaits the commits the
     * dismissal would have made. A failed commit rolls back visibly through [performCommit] (unhide +
     * warm message, seen on return) and **never blocks** the requested open/create.
     */
    private suspend fun commitPendingDeletesNow() {
        pendingDeletes.value.forEach { performCommit(it) }
    }

    private suspend fun DataResult<*>.sendMessageOnFailure() {
        if (this is DataResult.Failure) eventQueue.send(HomeShelfEvent.Message(error.warmMessage()))
    }
}

/** Warm failure copy (VOICE): [DataError.Busy] is "still saving", never a scary failure. */
private fun DataError.warmMessage(): String = when (this) {
    is DataError.Busy -> BUSY_MESSAGE
    else -> GENERIC_FAILURE_MESSAGE
}

/** Default title for a shelf-created zine — aligned with the store's adoption fallback (ADR-044 §4). */
internal const val DEFAULT_NEW_TITLE: String = "My zine"

/** The ADR-044 §1 gate refused: an editor session is still live/releasing. Retry-shaped, warm. */
internal const val BUSY_MESSAGE: String = "That zine is still saving — try again in a moment."

/** Any other mutation failure (VOICE: warm, recoverable, no jargon). */
internal const val GENERIC_FAILURE_MESSAGE: String = "That didn't work — try again?"

/** `ProjectSummary` → the card the shelf shows: only display data past this point (ADR-043/045). */
internal fun ProjectSummary.toCard(
    nowEpochMs: Long,
    thumbnail: ImageBitmap? = null,
): HomeZineCard = HomeZineCard(
    id = id,
    title = title,
    formatLabel = "${format.shelfLabel()} · ${paperSize.shelfLabel()}",
    editedLabel = editedLabel(updatedAtEpochMs, nowEpochMs),
    thumbnail = thumbnail,
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
