package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.ui.components.ZSnackbar
import com.aritr.zinely.ui.components.ZToast
import com.aritr.zinely.ui.theme.ZinelyHaptic
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow

/** Test tag on the shelf grid (the Content state's objects). */
public const val HomeShelfTestTag: String = "home-shelf"

/** Test tag on the loading skeleton. */
public const val HomeLoadingTestTag: String = "home-loading"

/** Test tag on the empty-shelf invitation container. */
public const val HomeEmptyStateTestTag: String = "home-empty-state"

/** `.empty h2` — the invitation, which teaches rather than reports (`shelf.html:399`). */
public const val HomeEmptyHeadline: String = "Make your first little zine."

/** Test tags on the rename row inside the action sheet (`.rename input`, `.rename .save`). */
public const val HomeRenameFieldTestTag: String = "home-rename-field"
public const val HomeRenameConfirmTestTag: String = "home-rename-confirm"

/** Test tag on the Start-a-zine paper chooser (`#createSheet`, S7.1/ADR-047). */
public const val HomePaperChooserTestTag: String = "home-paper-chooser"

/** Test tag for one paper choice inside the chooser. */
public fun homePaperChoiceTestTag(paperSize: PaperSize): String =
    "home-paper-choice-${paperSize.name}"

/** Test tag for one zine card on the shelf, keyed by its stable project [id]. */
public fun homeCardTestTag(id: String): String = "home-card-$id"

/** Test tag for a card's `⋯` affordance. */
public fun homeCardMenuTestTag(id: String): String = "home-card-menu-$id"

/** `Deleted “X”` — `$("#snackText").textContent` (`shelf.html:723`). */
public fun homeDeletedMessage(title: String): String = "Deleted “$title”"

/**
 * One zine on the shelf — the feature-local UI model, carrying only what the card shows
 * (ADR-043). The host ViewModel maps `ProjectSummary` → this, so this module stays free of
 * `:core:data`; [id] is the project id the tap hands back for `EditorRoute(id)` navigation.
 */
public data class HomeZineCard(
    val id: String,
    val title: String,
    val formatLabel: String,
    val editedLabel: String,
    /**
     * The rendered page-1 thumbnail (ADR-045).
     *
     * **Not rendered.** The frozen Shelf prints a generated riso cover per zine instead of a page
     * preview — an owner decision, taken with the pipeline left in place. The producer, its ADR and
     * its tests are untouched; nothing on this surface reads this field. Retiring or reviving the
     * pipeline is an M6 owner decision.
     */
    val thumbnail: ImageBitmap? = null,
)

/**
 * One-shot shelf event from the host to the screen (ADR-044 §3/§5) — feature-local so the module
 * stays free of `:core:data`. Queued events, not observable state: each is consumed exactly once
 * by the screen's serialising loop, so multiple deletes queue behind one another and recomposition
 * can't replay one.
 */
public sealed interface HomeShelfEvent {
    /**
     * A delete was requested and the card is already hidden: show the undo snackbar for [title];
     * Undo → `onDeleteUndo(id)`, dismiss/timeout → `onDeleteCommit(id)` (the actual store delete).
     */
    public data class DeletePrompt(val id: String, val title: String) : HomeShelfEvent

    /** A warm, transient message (a failed mutation). */
    public data class Message(val text: String) : HomeShelfEvent
}

/**
 * Which sheet is open. **At most one, ever** — `show()` in the frozen spec calls `closeSheets(true)`
 * before opening, and marks every other sheet plus the whole app behind the scrim `inert`. Modelling
 * the open sheet as one nullable value rather than three booleans makes that invariant unstateable
 * otherwise, which is the point: two open sheets is not a bug to be fixed, it is a state that cannot
 * be written down.
 */
private sealed interface ShelfSheet {
    data object Create : ShelfSheet
    data object Sort : ShelfSheet
    data class Actions(val cardId: String) : ShelfSheet
}

/** What the snackbar is waiting to be told: `true` = the user pressed Undo. */
private class UndoRequest(val id: String, val message: String, val outcome: CompletableDeferred<Boolean>)

/**
 * The Home · "My zines" shelf — a drawer of small printed objects, not a list of files.
 *
 * Stateless with respect to the store: it renders the [cards] it is given and hands every mutation
 * back. Search and sort are *view* state and live here, because nothing outside this screen has an
 * opinion about them; [SHELF_TOOLS_THRESHOLD] decides whether they are offered at all.
 *
 * [storeEmpty] is the honest empty signal (ADR-044 §3): the invitation shows only when the STORE is
 * empty — a shelf filtered to zero visible [cards] by pending undoable deletes is a zero-card shelf,
 * never the invitation. A shelf filtered to zero by a *search* is neither: it is the search miss.
 */
@Composable
public fun HomeScreen(
    loading: Boolean,
    storeEmpty: Boolean,
    cards: List<HomeZineCard>,
    events: Flow<HomeShelfEvent>,
    onOpenZine: (String) -> Unit,
    onStartZine: (PaperSize) -> Unit,
    onRenameZine: (String, String) -> Unit,
    onDuplicateZine: (String) -> Unit,
    onDeleteZine: (String) -> Unit,
    onDeleteUndo: (String) -> Unit,
    onDeleteCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** The shelf could not be *read*. No zine was lost; [onRetry] simply asks the store again. */
    error: Boolean = false,
    onRetry: () -> Unit = {},
) {
    val haptics = ZinelyTheme.haptics

    var openSheet by remember { mutableStateOf<ShelfSheet?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(ShelfSort.Recent) }

    var undo by remember { mutableStateOf<UndoRequest?>(null) }
    var toast by remember { mutableStateOf<Pair<String, CompletableDeferred<Unit>>?>(null) }

    // The collector outlives recompositions; always call the latest handlers.
    val currentUndo by rememberUpdatedState(onDeleteUndo)
    val currentCommit by rememberUpdatedState(onDeleteCommit)

    // One collector, and it *suspends* on each event until that event's surface is finished with.
    // Two deletes in quick succession therefore queue: the second snackbar never overwrites the
    // first, which would silently commit a delete the user still had a window to undo.
    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is HomeShelfEvent.DeletePrompt -> {
                    val outcome = CompletableDeferred<Boolean>()
                    undo = UndoRequest(event.id, homeDeletedMessage(event.title), outcome)
                    try {
                        if (outcome.await()) currentUndo(event.id) else currentCommit(event.id)
                    } finally {
                        undo = null
                        // A rotation disposes this composition and cancels the `await` above. The
                        // card is already hidden, so leaving the outcome unresolved would strand the
                        // zine: gone from the shelf, still in the store, with no window left to undo.
                        // Ending the window early *is* the answer ADR-046 §4 already gives for the
                        // sibling case (navigating away commits); the pending delete resolves the
                        // same way here, on the ViewModel's scope, which outlives the Activity.
                        if (!outcome.isCompleted) currentCommit(event.id)
                    }
                }

                is HomeShelfEvent.Message -> {
                    val gone = CompletableDeferred<Unit>()
                    toast = event.text to gone
                    gone.await()
                    toast = null
                }
            }
        }
    }

    val shown = remember(cards, query, sort) { shelfVisibleCards(cards, query, sort) }
    val searchable = cards.size >= SHELF_TOOLS_THRESHOLD
    val showTools = searchable && !loading && !storeEmpty && !error
    val showHead = !loading && !storeEmpty && !error

    BoxWithConstraints(modifier.fillMaxSize().background(ZinelyTheme.colors.desk)) {
        val shelfWidth = maxWidth
        val wide = shelfWidth >= 820.dp
        Column(Modifier.fillMaxSize()) {
            ShelfAppBar()
            if (showTools) {
                ShelfTools(
                    query = query,
                    onQueryChange = { query = it },
                    sortLabel = sort.buttonLabel,
                    onSortClick = { haptics.perform(ZinelyHaptic.Tick); openSheet = ShelfSheet.Sort },
                )
            }
            if (showHead) ShelfHead(cards.size)

            Box(Modifier.fillMaxWidth().weight(1f)) {
                when {
                    // An unreadable shelf must never flash the empty invitation, and a skeleton over
                    // a failed read would hold still forever.
                    error -> ShelfErrorState(onRetry, Modifier.fillMaxSize())

                    loading -> ShelfLoadingSkeleton(
                        columns = shelfColumns(shelfWidth),
                        roomy = wide,
                        modifier = Modifier.fillMaxSize(),
                    )

                    storeEmpty -> ShelfEmptyState(Modifier.fillMaxSize())

                    // `if(many && q && list.length===0)` — a search that matched nothing is not an
                    // empty shelf. The zines are all still there; the name isn't.
                    searchable && query.isNotBlank() && shown.isEmpty() ->
                        ShelfSearchMiss(Modifier.align(Alignment.TopCenter))

                    else -> ShelfGrid(
                        cards = shown,
                        onOpenZine = onOpenZine,
                        onZineActions = { id ->
                            haptics.perform(ZinelyHaptic.Tick)
                            openSheet = ShelfSheet.Actions(id)
                        },
                        modifier = Modifier.fillMaxSize().testTag(HomeShelfTestTag),
                    )
                }
            }
        }

        // `.dock{ position:sticky }` floats over the scroll (whose 112dp bottom padding clears it).
        // `dock.classList.toggle("hide", err)` — no starting a new zine on a shelf that can't be read.
        if (!error) {
            ShelfDock(
                onStart = { haptics.perform(ZinelyHaptic.Tick); openSheet = ShelfSheet.Create },
                wide = wide,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        // `.snackbar`/`.toast{ bottom:96px }` — above the dock, never under the thumb that made them.
        undo?.let { request ->
            ZSnackbar(
                message = request.message,
                actionLabel = "Undo",
                onAction = { haptics.perform(ZinelyHaptic.Tick); request.outcome.complete(true) },
                onTimeout = { request.outcome.complete(false) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp),
            )
        }
        toast?.let { (text, gone) ->
            ZToast(
                message = text,
                onTimeout = { gone.complete(Unit) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp),
            )
        }
    }

    ShelfCreateSheet(
        visible = openSheet is ShelfSheet.Create,
        onDismiss = { openSheet = null },
        onChoosePaper = { paper ->
            openSheet = null
            onStartZine(paper)
        },
    )

    ShelfSortSheet(
        visible = openSheet is ShelfSheet.Sort,
        selected = sort,
        onDismiss = { openSheet = null },
        onSelect = { sort = it },
    )

    // The card outlives its sheet's exit slide: nulling it on dismiss would hard-cut the animation.
    var actionCard by remember { mutableStateOf<HomeZineCard?>(null) }
    val target = (openSheet as? ShelfSheet.Actions)?.let { open -> cards.firstOrNull { it.id == open.cardId } }
    LaunchedEffect(target) { if (target != null) actionCard = target }
    ShelfActionSheet(
        visible = target != null,
        card = actionCard,
        onDismiss = { openSheet = null },
        onOpen = onOpenZine,
        onRename = onRenameZine,
        onDuplicate = onDuplicateZine,
        onDelete = onDeleteZine,
    )
}

/**
 * `sorted(list)` then `list.filter(...)` — the shelf's view of its own objects.
 *
 * **A conflict with the frozen spec, resolved in the spec's favour where it can be.** The prototype
 * sorts on a `d` date field that no repository row exposes to this screen; the card carries a
 * *recency label*, not a timestamp. But the repository's contract (ADR-042 §7) is that
 * `observeProjects()` emits newest-first, so:
 *
 * - `Recent` is the given order, passed through untouched — which is exactly what the spec's
 *   `y.d.localeCompare(x.d)` computes.
 * - `Oldest` is that order reversed — likewise exactly `x.d.localeCompare(y.d)`.
 * - `Name` sorts by title, case-insensitively, as `localeCompare` does.
 *
 * No date is re-derived here and no ordering is invented; the spec's three orders are reproduced
 * from the one ordering the repository is contractually required to provide.
 */
internal fun shelfVisibleCards(
    cards: List<HomeZineCard>,
    query: String,
    sort: ShelfSort,
): List<HomeZineCard> {
    val sorted = when (sort) {
        ShelfSort.Recent -> cards
        ShelfSort.Oldest -> cards.asReversed()
        ShelfSort.Name -> cards.sortedBy { it.title.lowercase() }
    }
    val needle = query.trim().lowercase()
    // `if(many && q)` — a shelf too small to offer search is never filtered by a stale query.
    if (needle.isEmpty() || cards.size < SHELF_TOOLS_THRESHOLD) return sorted
    return sorted.filter { it.title.lowercase().contains(needle) }
}
