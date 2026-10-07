package com.aritr.zinely.feature.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineCoverRecipe
import com.aritr.zinely.feature.editor.BenchSnack
import com.aritr.zinely.feature.editor.BenchSnackInsetBottom
import com.aritr.zinely.feature.editor.FolderSnackAction
import com.aritr.zinely.feature.editor.HomeShelfEvent
import com.aritr.zinely.feature.editor.ShelfCreateSheet
import com.aritr.zinely.feature.editor.ShelfRenameSheet
import com.aritr.zinely.feature.editor.homeDeletedMessage
import com.aritr.zinely.ui.components.ZSnackbar
import com.aritr.zinely.ui.components.ZToast
import com.aritr.zinely.ui.theme.ZinelyHaptic
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Grain
import com.aritr.zinely.ui.theme.rememberZinelyV21GrainBrush
import com.aritr.zinely.ui.theme.zinelyV21Grain
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Locale

/** The screen itself — `.phone`, the desk everything else stands on. */
public const val ZineLibraryTestTag: String = "zine-library"

/**
 * `.shelf` as **this screen places it**, which is not the same claim as B2's own tag.
 *
 * The screen owes two facts about the shelf that B2 cannot state alone: that it runs *under* the dock
 * rather than stopping above it, and that it is absent entirely in the error state. Both are assertions
 * about the placed region, so the region needs a handle here.
 */
internal const val ZineLibraryShelfTestTag: String = "zine-library-shelf"

/**
 * One zine as the V2 Library shows it: the name on its cover, the cover it was assigned, and the line the
 * shelf withholds until the sheet asks for it.
 *
 * **The identity is the id, not the position.** B2 keyed its grid by position and said so, because a list
 * that cannot reorder has no other identity; B5 brings real projects, and with them the stable key that
 * makes a rename, a duplicate and a delete land on the zine they were aimed at.
 *
 * @property subtitle `data-sub` — paper and recency, e.g. `"A4 · Edited 2 days ago"`. Composed by the data
 *   layer, never by a cover: the frozen file shows five example values and defines no thresholds, so the
 *   rule lives with the timestamps ([ADR-086](docs/DECISIONS.md#adr-086) row 8).
 * @property cover the persisted [ZineCoverRecipe] — assigned **once**, at creation, and stored
 *   ([D-017](docs/design/V2-SPEC-DEFECTS.md#d-017-ruling)). Non-null here: a shelf cannot draw an object
 *   with no cover, and the one path that can leave a project unassigned (a backfill whose write failed)
 *   is resolved before this type is built.
 * @property folder the folder this zine is in, or `null` on My Shelf
 *   ([ADR-125](docs/DECISIONS.md#adr-125)). Every zine of one folder carries **the same spelling** here:
 *   the host settles it, so the screen groups by plain equality.
 */
public data class LibraryZine(
    val id: String,
    val title: String,
    val subtitle: String,
    val cover: ZineCoverRecipe,
    val unavailableReason: String? = null,
    val folder: String? = null,
)

/**
 * The four states the Library can be in — and they are **four**, not a shelf plus two booleans.
 *
 * The frozen prototype has six hard-coded zines and therefore only ever shows one of these; the other
 * three were added by the [D-024 amendment](docs/design/V2-SPEC-DEFECTS.md#d-024-ruling) and the
 * [D-025 ruling](docs/design/V2-SPEC-DEFECTS.md#d-025-ruling). Modelling them as one closed type rather
 * than as `loading`/`error`/`storeEmpty` flags (V1's shape) is deliberate: the frozen CSS is a set of
 * **mutually exclusive** `body.is-*` rules, and a flag triple can express `loading && error`, which the
 * design cannot. The impossible states are impossible here rather than merely untested.
 */
public sealed interface LibraryShelfState {
    /** The store has not answered yet — `body.is-loading`. */
    public data object Loading : LibraryShelfState

    /** The shelf could not be *read*. Nothing was lost — `body.is-error`. */
    public data object Error : LibraryShelfState

    /** The **store** is empty, which is the invitation — `body.is-empty`. */
    public data object Empty : LibraryShelfState

    /** Zines, in the order the repository gave them. No sort is applied here; see [ZineLibraryScreen]. */
    public data class Content(val zines: List<LibraryZine>) : LibraryShelfState
}

/** Which of the screen's own surfaces is open over the shelf. */
private sealed interface LibrarySheet {
    /** The paper chooser — the existing creation flow ([ADR-047](docs/DECISIONS.md#adr-047)). */
    data object Create : LibrarySheet

    /** The whole-library backup / restore chooser. */
    data object KeepSafe : LibrarySheet

    /** B3's action sheet, for one zine. */
    data class Actions(val zineId: String) : LibrarySheet

    /** The existing rename input, raised by the sheet's Rename row. */
    data class Rename(val zineId: String, val title: String) : LibrarySheet

    /** A28: where one zine can go. */
    data class Move(val zineId: String) : LibrarySheet

    /** A28: naming a new folder for one zine. [fromChooser] is where Cancel leads back to (A28.14). */
    data class NewFolder(val zineId: String, val fromChooser: Boolean) : LibrarySheet

    /** A28: one folder's own sheet. */
    data class Folder(val name: String) : LibrarySheet

    /** A28: renaming one folder. */
    data class RenameFolder(val name: String) : LibrarySheet
}

/** Where focus should go once the Shelf shows what a folder action did (A28, *Focus*). */
private sealed interface FocusWish {
    /** Into a folder: the control that leads back out. */
    data object Back : FocusWish

    /** Out of a folder: that folder's pile. */
    data class Pile(val folder: String) : FocusWish

    /** After an action: the tile that now holds this zine, once the zine is in [folder]. */
    data class Zine(val id: String, val folder: String?) : FocusWish
}

/** What the undo snackbar is waiting to be told: `true` = the user pressed Undo. V1's shape, reused. */
private class UndoRequest(val id: String, val message: String, val outcome: CompletableDeferred<Boolean>)

/**
 * The frozen Library — `v2-library.html`, the whole screen (B5, [ADR-086](docs/DECISIONS.md#adr-086)).
 *
 * ```css
 * .phone{position:relative;background:var(--desk)}
 * ```
 *
 * This is the **integration** package: B1 drew the cover, B2 the shelf, B3 the gestures and the sheet, B4
 * the empty state and the dock — each against a frozen file that specifies it completely. What was left
 * is everything a prototype does not contain: real projects, the four states, and where each of the seven
 * actions goes. The screen answers the Library's one question,
 * *["which zine do I want?"](CLAUDE.md#product-principle-every-screen-answers-the-users-current-question)*,
 * and paints the desk the earlier packages deliberately left unpainted.
 *
 * ### The desk is this composable's, and nobody else's
 *
 * `.shelf`, `.empty`, `.fail` and `.dock` all declare **no background**; the ground belongs to
 * `.phone{background:var(--desk)}`, the app window. B1's cover and B2's shelf each recorded that they owe
 * their desk to whatever places them, and this is that place.
 *
 * ### Every action hands over to an existing flow; the Library adds no destination of its own
 *
 * That sentence is the [D-025 ruling](docs/design/V2-SPEC-DEFECTS.md#d-025-ruling) and it is the reason
 * V1's paper chooser, rename field and undo snackbar appear on a V2 surface. The seam is
 * [ADR-080](docs/DECISIONS.md#adr-080)'s migration architecture working as designed — accepted and
 * recorded as a Known Limitation in ADR-086, not drift.
 *
 * **Share & export is the one that is easy to get wrong.** It must push the editor **and then** the Proof,
 * because `ProofRoute` resolves the *shared* editor ViewModel from the editor's live back-stack entry (the
 * [ADR-026](docs/DECISIONS.md#adr-026) single-writer seam); a direct navigate to the Proof throws at
 * runtime. This screen therefore reports the intent and the destination owns the two pushes.
 *
 * ### The dock stands in all four states
 *
 * `.dock` sits outside `.empty` and outside `.fail`, and **no state rule targets it** — verified against
 * the frozen file rather than inferred. The ruling states why: *"the dock is part of the workspace rather
 * than the loaded content."* So a user whose shelf failed to open can still start a zine, and the empty
 * state's only exit is never hidden by a slow read. This is the one composition fact of the screen that a
 * "does it look right" review cannot catch in three of its four states.
 *
 * ### The order is the repository's, untouched
 *
 * The frozen file states **no** sort — its own six zines run 2 days · today · 5 days · 1 week · 2 weeks ·
 * 3 weeks, which is not sorted by anything — and `ProjectRepository.observeProjects()` documents its
 * newest-first contract. So the list passes straight through. V1's sort control was dropped by owner
 * ruling ([ADR-081](docs/DECISIONS.md#adr-081)); re-deriving an order here would be inventing design out
 * of the freeze's silence, which [D-020](docs/design/V2-SPEC-DEFECTS.md#d-020-ruling) forbids by name.
 *
 * ### The loading debounce is not here, and that is a decision
 *
 * A short delay before the placeholders appear, so a fast read does not flash them, is **implementation
 * behaviour rather than design**: the canonical HTML can neither express nor verify a timing threshold, so
 * encoding one there would make the design authoritative over something it cannot check
 * ([D-024's ruling](docs/design/V2-SPEC-DEFECTS.md#d-024-ruling)). The state machine is asserted in unit
 * tests; the threshold is a device Pass 1 question — *does a fast read flash?* — and lives with whoever
 * produces [LibraryShelfState.Loading], not with the screen that draws it.
 *
 * @param state which of the four the shelf is in. One value, not three flags — see [LibraryShelfState].
 * @param events one-shot shelf events (undo prompts, warm failure messages), each consumed exactly once.
 * @param onOpenZine a cover was tapped, or *Open on the bench* was chosen → the editor.
 * @param onShareExport *Share & export* → the editor **and then** the Proof. See above.
 * @param onStartZine the paper was chosen → the existing creation flow. The second value is the open
 *   folder, or `null` on My Shelf: a zine made inside a folder lands in it (A28.6).
 * @param onMoveZine `(id, folder)` — a zine goes into a folder, a new one or an existing one, or back to My
 *   Shelf (`null`). The answer comes back as a [HomeShelfEvent.FolderSnack], as it does for the next two.
 * @param onRenameFolder `(from, to)`.
 * @param onUnpackFolder *Take the zines out*.
 * @param onFolderSnackAction the folder snack's Undo or Try again was pressed.
 * @param checkFolderName what a typed folder name stands for. The host's, because the rules live where the
 *   names are stored.
 * @param onRenameZine `(id, newTitle)` — trimming and the blank-is-not-a-rename rule belong to the flow.
 * @param onDuplicateZine a copy of the content, with a **new** cover
 *   ([D-026](docs/design/V2-SPEC-DEFECTS.md#d-026-ruling)) — which the store does, not this screen.
 * @param onDeleteZine hide the zine and open the undo window. The store is not called yet.
 * @param onDeleteUndo the window closed with Undo pressed.
 * @param onDeleteCommit the window closed without it — only now is the zine really deleted.
 * @param onRetry the shelf failed to open; ask the store again.
 */
@Composable
public fun ZineLibraryScreen(
    state: LibraryShelfState,
    events: Flow<HomeShelfEvent>,
    backupRestoreState: LibraryBackupRestoreUiState?,
    lastBackup: LibraryLastBackup?,
    onOpenZine: (String) -> Unit,
    onShareExport: (String) -> Unit,
    onStartZine: (PaperSize, String?) -> Unit,
    onRenameZine: (String, String) -> Unit,
    onDuplicateZine: (String) -> Unit,
    onDeleteZine: (String) -> Unit,
    onDeleteUndo: (String) -> Unit,
    onDeleteCommit: (String) -> Unit,
    onRetry: () -> Unit,
    onStartBackup: () -> Unit,
    onStartRestore: () -> Unit,
    onDismissBackupRestore: () -> Unit,
    onCancelBackupRestore: () -> Unit,
    onRetryBackupRestore: () -> Unit,
    preferredPaper: PaperSize = PaperSize.A4,
    appVersion: String = "",
    onPreferredPaperChange: (PaperSize) -> Unit = {},
    onMoveZine: (String, String?) -> Unit = { _, _ -> },
    onRenameFolder: (String, String) -> Unit = { _, _ -> },
    onUnpackFolder: (String) -> Unit = {},
    onFolderSnackAction: () -> Unit = {},
    checkFolderName: (String) -> FolderNameVerdict = ::plainFolderNameVerdict,
    modifier: Modifier = Modifier,
) {
    val colors = ZinelyTheme.v21Colors
    val haptics = ZinelyTheme.haptics

    var openSheet by remember { mutableStateOf<LibrarySheet?>(null) }
    var undo by remember { mutableStateOf<UndoRequest?>(null) }
    var toast by remember { mutableStateOf<Pair<String, CompletableDeferred<Unit>>?>(null) }
    val backupActionFocusRequester = remember { FocusRequester() }
    val colophonActionFocusRequester = remember { FocusRequester() }
    var restoreBackupActionFocus by remember { mutableStateOf(false) }
    var showColophon by rememberSaveable { mutableStateOf(false) }
    var restoreColophonActionFocus by remember { mutableStateOf(false) }
    var createPaperSnapshot by remember { mutableStateOf(preferredPaper) }

    // A28. The open folder is saved, so it survives a trip to the Bench even if Android stopped the app
    // meanwhile; a fresh start has nothing saved and opens My Shelf (A28.9).
    var openFolder by rememberSaveable { mutableStateOf<String?>(null) }
    val shelfGrid = rememberLazyGridState()
    val folderGrid = rememberLazyGridState()
    val shelfFocus = remember { ShelfFocus() }
    var focusWish by remember { mutableStateOf<FocusWish?>(null) }
    var focusTile by remember { mutableIntStateOf(-1) }
    // The folder that was open when a folder action was confirmed: Undo goes back to it.
    var folderAtAction by remember { mutableStateOf<String?>(null) }
    var reopenFolder by remember { mutableStateOf<String?>(null) }
    // The snack keeps its last line while it is down, so the line does not blank as it leaves.
    var snack by remember { mutableStateOf(HomeShelfEvent.FolderSnack("")) }
    var snackUp by remember { mutableStateOf(false) }
    var snackStep by remember { mutableIntStateOf(0) }
    var snackHeld by remember { mutableStateOf(false) }
    var dockHeight by remember { mutableIntStateOf(0) }

    // The collector outlives recompositions; always call the latest handlers.
    val currentUndo by rememberUpdatedState(onDeleteUndo)
    val currentCommit by rememberUpdatedState(onDeleteCommit)

    LaunchedEffect(backupRestoreState) {
        if (backupRestoreState != null) openSheet = null
    }

    LaunchedEffect(backupRestoreState, state) {
        if (backupRestoreState != null) {
            restoreBackupActionFocus = true
        } else if (
            restoreBackupActionFocus &&
            (state is LibraryShelfState.Content || state is LibraryShelfState.Empty)
        ) {
            // Dialogs contain focus while visible. Ask for it back only after Compose has removed the
            // modal surface, so keyboard and accessibility users return to the action that opened it.
            backupActionFocusRequester.requestFocus()
            restoreBackupActionFocus = false
        }
    }

    LaunchedEffect(showColophon, state) {
        if (!showColophon && restoreColophonActionFocus &&
            (state is LibraryShelfState.Content || state is LibraryShelfState.Empty)
        ) {
            colophonActionFocusRequester.requestFocus()
            restoreColophonActionFocus = false
        }
    }

    // V1's collector, reused whole rather than re-derived — including the two things about it that are
    // not obvious and were paid for once already: it *suspends* per event, so two quick deletes queue
    // instead of the second snackbar silently committing the first; and the `finally` commits an
    // unresolved outcome, so a rotation cannot strand a zine that is hidden from the shelf but still in
    // the store with no undo window left. "Reuse the existing delete flow" is reuse of this, not of a
    // delete call ([D-025](docs/design/V2-SPEC-DEFECTS.md#d-025-ruling)).
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
                        if (!outcome.isCompleted) currentCommit(event.id)
                    }
                }

                is HomeShelfEvent.Message -> {
                    val gone = CompletableDeferred<Unit>()
                    toast = event.text to gone
                    gone.await()
                    toast = null
                }

                // Not awaited: a newer folder snack replaces this one (A28.12), it does not queue behind it.
                is HomeShelfEvent.FolderSnack -> {
                    snack = event
                    snackStep++
                    snackUp = true
                    event.zineId?.let { focusWish = FocusWish.Zine(it, event.folder) }
                }
            }
        }
    }

    val zines = (state as? LibraryShelfState.Content)?.zines.orEmpty()
    val folders = shelfFolders(zines)
    val tiles = shelfTiles(zines, openFolder)

    // A28.3: a folder exists only while it holds a zine, so an open folder whose last zine left or was
    // deleted returns to My Shelf. Only once the Shelf has answered: while it is still loading there are
    // no zines to judge by, and a folder restored after Android stopped the app must not be dropped.
    LaunchedEffect(state, openFolder) {
        if (state is LibraryShelfState.Content && openFolder != null && openFolder !in folders) openFolder = null
    }
    // Undo puts the maker back in the folder the action was taken from, once that folder exists again.
    LaunchedEffect(reopenFolder, folders) {
        val folder = reopenFolder ?: return@LaunchedEffect
        if (folder in folders) {
            openFolder = folder
            reopenFolder = null
        }
    }
    // A28.13: the snack goes away when a sheet is raised, and its action goes with it.
    LaunchedEffect(openSheet) {
        if (openSheet != null) {
            snackUp = false
            focusWish = null
            reopenFolder = null
        }
    }
    // A28.13: at least four seconds, longer if the phone's accessibility timeout asks, and for as long as
    // a finger or focus is on it.
    val accessibility = LocalAccessibilityManager.current
    LaunchedEffect(snackUp, snackStep) {
        if (!snackUp) return@LaunchedEffect
        val stay = accessibility?.calculateRecommendedTimeoutMillis(
            originalTimeoutMillis = FolderSnackMillis,
            containsText = true,
            containsControls = snack.action != null,
        ) ?: FolderSnackMillis
        do {
            delay(stay)
        } while (snackHeld)
        snackUp = false
    }
    // A28, *Focus*. A zine's wish waits until the Shelf shows the zine where the action put it; before
    // that the tile at its place is still the old one. A tile that is not on screen has no requester
    // attached, and then the heading takes focus, as the frozen page has it.
    LaunchedEffect(focusWish, tiles) {
        val wish = focusWish ?: return@LaunchedEffect
        val index = when (wish) {
            FocusWish.Back -> -1
            is FocusWish.Pile -> tiles.indexOfFirst { it is ShelfTile.Pile && it.name == wish.folder }
            is FocusWish.Zine -> {
                if (zines.firstOrNull { it.id == wish.id }?.let { it.folder == wish.folder } != true) return@LaunchedEffect
                tiles.indexOfFirst { tile ->
                    when (tile) {
                        is ShelfTile.Zine -> tile.zine.id == wish.id
                        is ShelfTile.Pile -> tile.zines.any { it.id == wish.id }
                    }
                }
            }
        }
        focusTile = index
        withFrameNanos { }
        focusWish = null
        val first = if (wish == FocusWish.Back) shelfFocus.back else if (index >= 0) shelfFocus.tile else shelfFocus.heading
        runCatching { first.requestFocus() }.onFailure { runCatching { shelfFocus.heading.requestFocus() } }
    }

    // ADR-125 rule 11: a folder action finishes a waiting delete first, so its Undo is gone by the time the
    // folder snack shows. The host does the finishing; this closes the delete's own snack.
    val confirmFolderAction = { then: () -> Unit ->
        undo?.outcome?.complete(false)
        folderAtAction = openFolder
        openSheet = null
        then()
    }
    // Into a folder: from the top, with focus on the control that leads back out.
    val scope = rememberCoroutineScope()
    val enterFolder = { name: String ->
        openFolder = name
        focusWish = FocusWish.Back
        scope.launch { folderGrid.scrollToItem(0) }
        Unit
    }

    if (showColophon) {
        Box(modifier.fillMaxSize()) {
            ColophonScreen(
                preferredPaper = preferredPaper,
                appVersion = appVersion,
                onPreferredPaperChange = onPreferredPaperChange,
                onBackToShelf = {
                    restoreColophonActionFocus = true
                    showColophon = false
                },
                modifier = Modifier.fillMaxSize(),
            )
            toast?.let { (text, gone) ->
                ZToast(
                    message = text,
                    onTimeout = { gone.complete(Unit) },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = TransientBottomInset),
                )
            }
        }
        return
    }

    // A28.9: inside a folder, system Back returns to My Shelf, which is where it was left.
    val leaveFolder = {
        openFolder?.let { focusWish = FocusWish.Pile(it) }
        openFolder = null
    }
    BackHandler(enabled = openFolder != null, onBack = leaveFolder)

    Box(
        modifier
            .testTag(ZineLibraryTestTag)
            .fillMaxSize()
            // `.phone{background:var(--desk)}` — the ground the shelf and the dock both leave to their
            // caller.
            .background(colors.desk),
        // NOTE: no `windowInsetsPadding` here, deliberately. `.dock` transcribes
        // `calc(22px + env(safe-area-inset-bottom))` as a *consuming* pad, so a second consumer on this
        // root would silently double-count the inset — the risk ADR-084 decision 4 named in terms. The
        // absence is structural and is asserted as an absence.
    ) {
        when (state) {
            // `body.is-loading` hides `.zine` and shows four `.ph` — inside the shelf, under the same
            // heading. It does **not** hide `.shelf` itself, and it explicitly hides `.empty`: a slow
            // read that rendered the invitation would tell a user with twelve zines that they have none.
            is LibraryShelfState.Loading -> ZineShelf(
                zines = emptyList(),
                onOpen = {},
                onActions = {},
                modifier = Modifier.fillMaxSize().testTag(ZineLibraryShelfTestTag),
                placeholders = LoadingPlaceholderCount,
            )

            // `body.is-error{.shelf:none;.empty:none;.fail:flex}` — the shelf is gone, not merely empty.
            // Retry answers the hand like every other control on this screen; the failure surface was
            // the one that did not.
            is LibraryShelfState.Error ->
                ZineShelfFail({ haptics.perform(ZinelyHaptic.Tick); onRetry() }, Modifier.fillMaxSize())

            // `body.is-empty{.shelf:none;.empty:flex}`. The shelf and the invitation are alternatives,
            // and the half that matters is the *absence*: a screen that renders both looks correct in a
            // screenshot of either one.
            is LibraryShelfState.Empty -> ZineShelfEmpty(Modifier.fillMaxSize())

            // My Shelf and an open folder are two grids, each with its own scroll position, so going back
            // finds My Shelf where it was left (A28.9).
            is LibraryShelfState.Content -> key(openFolder != null) {
                ZineShelf(
                    zines = tiles.map { tile ->
                        when (tile) {
                            is ShelfTile.Zine -> ZineShelfItem(tile.zine.title, tile.zine.cover, tile.zine.subtitle)
                            is ShelfTile.Pile -> ZineShelfItem(
                                title = tile.name,
                                recipe = tile.zines.first().cover,
                                subtitle = pluralZineCount(tile.zines.size),
                                pile = tile.zines.map { it.cover },
                            )
                        }
                    },
                    onOpen = { index ->
                        when (val tile = tiles.getOrNull(index)) {
                            is ShelfTile.Zine ->
                                if (tile.zine.unavailableReason == null) {
                                    onOpenZine(tile.zine.id)
                                } else {
                                    openSheet = LibrarySheet.Actions(tile.zine.id)
                                }
                            is ShelfTile.Pile -> enterFolder(tile.name)
                            null -> Unit
                        }
                    },
                    onActions = { index ->
                        when (val tile = tiles.getOrNull(index)) {
                            is ShelfTile.Zine -> openSheet = LibrarySheet.Actions(tile.zine.id)
                            is ShelfTile.Pile -> openSheet = LibrarySheet.Folder(tile.name)
                            null -> Unit
                        }
                    },
                    modifier = Modifier.fillMaxSize().testTag(ZineLibraryShelfTestTag),
                    // A28.5: beside My Shelf, every zine, in a folder or not; inside a folder, its own.
                    count = if (openFolder == null) zines.size else tiles.size,
                    folder = openFolder,
                    onBack = { haptics.perform(ZinelyHaptic.Tick); leaveFolder() },
                    state = if (openFolder == null) shelfGrid else folderGrid,
                    focus = shelfFocus,
                    focusTile = focusTile,
                )
            }
        }

        // `.grainy::before` — the desk's own paper tooth: `background-size:160px 160px`,
        // `mix-blend-mode:soft-light`, `opacity:.55`. Chrome grain, not paper grain: it blends with
        // what is already painted beneath rather than darkening it like ink.
        //
        // **A sibling here, and its position is the transcription.** `z-index:2` puts it above the
        // shelf (`z-index:auto`) and below the dock (`40`) and the sheet (`46`) — so the covers wear
        // the desk's tooth and the chrome above them does not. Written as a modifier on the root Box it
        // would paint after every child, grain the dock's gradient, and look almost right.
        //
        // `.phone::after` is a SECOND grain layer in the frozen file, at `z-index:60` over everything.
        // That one is the prototype's *studio* ground — the simulated device sitting on a page — and is
        // excluded on the same reasoning that excluded `--stage` from the palette.
        Box(
            Modifier
                .fillMaxSize()
                .zinelyV21Grain(
                    rememberZinelyV21GrainBrush(),
                    ZinelyV21Grain.BakedAlpha * DeskGrainOpacity,
                    ZinelyV21Grain.ChromeBlend,
                ),
        )

        // `.dock{position:absolute;left:0;right:0;bottom:0}` — over the shelf, which scrolls under it and
        // clears it with bottom padding of its own (188dp on `.shelf`, 206dp on `.empty`; the failure
        // surface keeps its established 150dp because it has no secondary action),
        // each grown by the safe area — see [zineDockClearance]). In a `Column` after the shelf it would
        // look identical at rest and steal that space at every other moment.
        ZineDock(
            onStart = {
                haptics.perform(ZinelyHaptic.Tick)
                createPaperSnapshot = preferredPaper
                openSheet = LibrarySheet.Create
            },
            secondaryActions = when (state) {
                is LibraryShelfState.Content, is LibraryShelfState.Empty -> listOf(
                    ZineDockSecondaryAction(
                        label = if (state is LibraryShelfState.Empty || zines.isEmpty()) {
                            Copy.LibraryBackup.BRING_BACK
                        } else {
                            Copy.LibraryBackup.BACKUPS
                        },
                        onClick = {
                            haptics.perform(ZinelyHaptic.Tick)
                            restoreBackupActionFocus = true
                            openSheet = LibrarySheet.KeepSafe
                        },
                        focusRequester = backupActionFocusRequester,
                    ),
                    ZineDockSecondaryAction(
                        label = Copy.Colophon.ACTION,
                        onClick = {
                            haptics.perform(ZinelyHaptic.Tick)
                            restoreColophonActionFocus = true
                            showColophon = true
                        },
                        focusRequester = colophonActionFocusRequester,
                    ),
                )
                else -> emptyList()
            },
            modifier = Modifier.align(Alignment.BottomCenter).onSizeChanged { dockHeight = it.height },
        )

        // `.snack` — the Shelf's own, for folder actions (A28.12, A28.13): *the Bench's support-paper
        // snack, same paint*, so it is that composable, with the two things the Shelf's rule changes.
        // 8dp above the dock at any text size, so the dock is measured, not assumed.
        BenchSnack(
            visible = snackUp,
            message = snack.message,
            actionLabel = when (snack.action) {
                FolderSnackAction.Undo -> Copy.Shelf.UNDO
                FolderSnackAction.TryAgain -> Copy.Folders.TRY_AGAIN
                null -> null
            },
            onAction = {
                snackUp = false
                if (snack.action == FolderSnackAction.Undo) reopenFolder = folderAtAction
                onFolderSnackAction()
            },
            colors = colors,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onFocusChanged { snackHeld = it.hasFocus }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        snackHeld = true
                        waitForUpOrCancellation(PointerEventPass.Initial)
                        snackHeld = false
                    }
                },
            bottomClearance = with(LocalDensity.current) { dockHeight.toDp() } + FolderSnackGap - BenchSnackInsetBottom,
            step = snackStep,
            shape = FolderSnackShape,
            actionMinHeight = FolderSnackActionMinHeight,
        )

        // Above the dock, never under the thumb that made them — V1's placement, reused with its chrome.
        undo?.let { request ->
            ZSnackbar(
                message = request.message,
                actionLabel = Copy.Shelf.UNDO,
                onAction = { haptics.perform(ZinelyHaptic.Tick); request.outcome.complete(true) },
                onTimeout = { request.outcome.complete(false) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = TransientBottomInset),
            )
        }
        toast?.let { (text, gone) ->
            ZToast(
                message = text,
                onTimeout = { gone.complete(Unit) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = TransientBottomInset),
            )
        }
    }

    // The existing creation flow: choosing the paper *is* the create action (ADR-047).
    ShelfCreateSheet(
        visible = openSheet is LibrarySheet.Create,
        preferredPaper = createPaperSnapshot,
        onDismiss = { openSheet = null },
        onChoosePaper = { paper ->
            openSheet = null
            onStartZine(paper, openFolder)
        },
    )

    KeepSafeSheet(
        visible = openSheet is LibrarySheet.KeepSafe,
        canBackup = zines.isNotEmpty(),
        lastBackup = lastBackup,
        onDismiss = { openSheet = null },
        onHidden = {
            if (backupRestoreState == null && restoreBackupActionFocus) {
                backupActionFocusRequester.requestFocus()
                restoreBackupActionFocus = false
            }
        },
        onSaveBackup = {
            openSheet = null
            onStartBackup()
        },
        onRestoreBackup = {
            openSheet = null
            onStartRestore()
        },
    )

    val actionTarget = (openSheet as? LibrarySheet.Actions)
        ?.let { open -> zines.firstOrNull { it.id == open.zineId } }

    // B3's sheet reports the choice and holds still — dismissal is decided "alongside the flow it
    // triggers", which is here. Every row dismisses first: each of the five leads somewhere else, and a
    // sheet left standing over a pushed route is the one shape none of them wants.
    ZineActionSheet(
        target = actionTarget?.let {
            ZineActionTarget(
                title = it.title,
                subtitle = it.subtitle,
                unavailableReason = it.unavailableReason,
                inFolder = it.folder != null,
            )
        },
        onDismiss = { openSheet = null },
        onAction = { action ->
            val zine = actionTarget ?: return@ZineActionSheet
            if (
                zine.unavailableReason != null &&
                (action == ZineAction.Open ||
                    action == ZineAction.ShareExport ||
                    action == ZineAction.Duplicate)
            ) {
                return@ZineActionSheet
            }
            // Every row answers the hand, not only the two that change the shelf. Three of these five
            // buzzed and three did not, which reads as the quiet ones having failed. `Tick` for the rows
            // that lead somewhere; `Snap`/`Boundary` below stay as they are, because duplicating and
            // deleting are a different kind of event and the finger should be able to tell.
            when (action) {
                ZineAction.Open -> { haptics.perform(ZinelyHaptic.Tick); openSheet = null; onOpenZine(zine.id) }
                ZineAction.ShareExport -> {
                    haptics.perform(ZinelyHaptic.Tick)
                    openSheet = null
                    onShareExport(zine.id)
                }
                // Rename replaces the sheet with the existing input rather than closing to nothing: the
                // field is the reason the row was pressed.
                ZineAction.Rename -> {
                    haptics.perform(ZinelyHaptic.Tick)
                    openSheet = LibrarySheet.Rename(zine.id, zine.title)
                }
                ZineAction.Duplicate -> {
                    haptics.perform(ZinelyHaptic.Snap)
                    openSheet = null
                    onDuplicateZine(zine.id)
                }
                // A28.14: the first folder skips the chooser. With no folder to choose, the row goes
                // straight to naming one.
                ZineAction.Move -> {
                    haptics.perform(ZinelyHaptic.Tick)
                    openSheet = if (folders.isEmpty()) {
                        LibrarySheet.NewFolder(zine.id, fromChooser = false)
                    } else {
                        LibrarySheet.Move(zine.id)
                    }
                }
                ZineAction.Delete -> {
                    haptics.perform(ZinelyHaptic.Boundary)
                    openSheet = null
                    onDeleteZine(zine.id)
                }
            }
        },
    )

    val renaming = openSheet as? LibrarySheet.Rename
    ShelfRenameSheet(
        visible = renaming != null,
        title = renaming?.title.orEmpty(),
        onDismiss = { openSheet = null },
        onRename = { newTitle -> renaming?.let { onRenameZine(it.zineId, newTitle) } },
    )

    // ---- A28: the three folder sheets ---------------------------------------------------------------
    val moving = (openSheet as? LibrarySheet.Move)?.let { open -> zines.firstOrNull { it.id == open.zineId } }
    FolderMoveSheet(
        target = moving?.let { FolderMoveTarget(it.title, it.folder, folders) },
        onMove = { folder ->
            moving?.let { zine ->
                haptics.perform(ZinelyHaptic.Snap)
                confirmFolderAction { onMoveZine(zine.id, folder) }
            }
        },
        onNewFolder = {
            moving?.let { zine ->
                haptics.perform(ZinelyHaptic.Tick)
                openSheet = LibrarySheet.NewFolder(zine.id, fromChooser = true)
            }
        },
        onDismiss = { openSheet = null },
    )

    val naming = openSheet as? LibrarySheet.NewFolder
    val namingFor = naming?.let { open -> zines.firstOrNull { it.id == open.zineId } }
    val renamingFolder = (openSheet as? LibrarySheet.RenameFolder)?.name?.takeIf { it in folders }
    FolderNameSheet(
        target = when {
            namingFor != null -> FolderNameTarget(zineTitle = namingFor.title, zineFolder = namingFor.folder)
            renamingFolder != null -> FolderNameTarget(renaming = renamingFolder, count = folders.getValue(renamingFolder))
            else -> null
        },
        keys = folders.keys.associateBy { name ->
            (checkFolderName(name) as? FolderNameVerdict.Name)?.key ?: name
        },
        check = checkFolderName,
        onGo = { name ->
            when {
                namingFor != null -> confirmFolderAction { onMoveZine(namingFor.id, name) }
                renamingFolder != null -> confirmFolderAction { onRenameFolder(renamingFolder, name) }
            }
        },
        // A28.14: Cancel returns to the chooser when the sheet came from the chooser.
        onCancel = {
            openSheet = if (naming != null && naming.fromChooser) LibrarySheet.Move(naming.zineId) else null
        },
        onDismiss = { openSheet = null },
    )

    val folderSheet = (openSheet as? LibrarySheet.Folder)?.name?.takeIf { it in folders }
    FolderActionsSheet(
        target = folderSheet?.let { FolderActionsTarget(it, folders.getValue(it)) },
        onOpen = {
            folderSheet?.let { name ->
                haptics.perform(ZinelyHaptic.Tick)
                openSheet = null
                enterFolder(name)
            }
        },
        onRename = {
            folderSheet?.let { name ->
                haptics.perform(ZinelyHaptic.Tick)
                openSheet = LibrarySheet.RenameFolder(name)
            }
        },
        onUnpack = {
            folderSheet?.let { name ->
                haptics.perform(ZinelyHaptic.Snap)
                confirmFolderAction { onUnpackFolder(name) }
            }
        },
        onDismiss = { openSheet = null },
    )

    LibraryBackupRestoreStateSheet(
        state = backupRestoreState,
        onDismiss = onDismissBackupRestore,
        onCancel = onCancelBackupRestore,
        onRetry = onRetryBackupRestore,
    )
}

/**
 * `<div class="ph"></div>` × 4 (`:184`) — two rows of the two-column grid, which is what the shelf looks
 * like with something on it. Frozen, not chosen: a different count is a different amount of shelf.
 */
private const val LoadingPlaceholderCount = 4

/**
 * `.snackbar`/`.toast{bottom:96px}` — V1's transient surfaces at V1's offset, arriving with the flow they
 * belong to. Not a frozen V2 value: the V2 file has no snackbar, because a prototype with six hard-coded
 * zines never deletes one.
 */
private val TransientBottomInset = 96.dp

/** `.grainy::before{opacity:.55}` — the effective strength is this times the tile's baked `.42`. */
private const val DeskGrainOpacity = 0.55f

/**
 * `.snack{border-radius:var(--br-lg)}`, `.snack button{min-height:48px}`, 8px above the dock, and the four
 * seconds it stays at least (A28.13).
 */
private val FolderSnackShape = RoundedCornerShape(ZinelyV21Dimens.radiusLg)
private val FolderSnackActionMinHeight = 48.dp
private val FolderSnackGap = 8.dp
private const val FolderSnackMillis = 4000L

/**
 * What a typed folder name stands for when no host says: trimmed, compared ignoring case. The frozen page's
 * own rule (`fold`), enough for a preview or a test of this screen; the app passes the real one.
 */
internal fun plainFolderNameVerdict(typed: String): FolderNameVerdict {
    val whole = typed.trim()
    // The key is of the name that is kept, so a long name and its own first forty characters are one folder.
    val name = whole.take(PlainFolderNameLength).trim()
    val key = name.lowercase(Locale.ROOT)
    return when {
        name.isEmpty() -> FolderNameVerdict.Blank
        key == Copy.Folders.MY_SHELF.lowercase(Locale.ROOT) -> FolderNameVerdict.MyShelf
        else -> FolderNameVerdict.Name(name, key, cut = whole.length > PlainFolderNameLength)
    }
}

private const val PlainFolderNameLength = 40
