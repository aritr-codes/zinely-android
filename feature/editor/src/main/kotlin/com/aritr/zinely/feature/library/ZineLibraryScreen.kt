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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

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

    /**
     * Zines, in the order the repository gave them. No sort is applied here; see [ZineLibraryScreen].
     *
     * @property restored the zines whose details went back to their earlier copy and wait to be told, in Shelf
     *   order ([ADR-134](docs/DECISIONS.md#adr-134)). My Shelf shows one note for all of them (A37).
     */
    public data class Content(
        val zines: List<LibraryZine>,
        val restored: List<RestoredZine> = emptyList(),
    ) : LibraryShelfState
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

    /** A47.7: where several chosen zines can go. A choice of one uses [Move]. */
    data class MoveMany(val zineIds: List<String>) : LibrarySheet

    /** A47.7: naming a new folder for several chosen zines. */
    data class NewFolderMany(val zineIds: List<String>, val fromChooser: Boolean) : LibrarySheet
}

/**
 * Keeps [LibrarySheet] across Activity recreation. A hand-written [Saver] rather than `@Parcelize`:
 * no module here applies the parcelize plugin (W1 R-W8). One tag, then the case's own fields.
 */
private val LibrarySheetSaver: Saver<LibrarySheet?, Any> = Saver(
    save = { sheet ->
        when (sheet) {
            null -> emptyList<Any>()
            LibrarySheet.Create -> listOf("create")
            LibrarySheet.KeepSafe -> listOf("keepSafe")
            is LibrarySheet.Actions -> listOf("actions", sheet.zineId)
            is LibrarySheet.Rename -> listOf("rename", sheet.zineId, sheet.title)
            is LibrarySheet.Move -> listOf("move", sheet.zineId)
            is LibrarySheet.NewFolder -> listOf("newFolder", sheet.zineId, sheet.fromChooser)
            is LibrarySheet.Folder -> listOf("folder", sheet.name)
            is LibrarySheet.RenameFolder -> listOf("renameFolder", sheet.name)
            is LibrarySheet.MoveMany -> listOf<Any>("moveMany") + sheet.zineIds
            is LibrarySheet.NewFolderMany -> listOf<Any>("newFolderMany", sheet.fromChooser) + sheet.zineIds
        }
    },
    restore = { saved ->
        // Bad or stale data closes the sheet rather than crashing the Shelf on start.
        runCatching {
            val f = (saved as List<*>)
            when (f.firstOrNull()) {
                "create" -> LibrarySheet.Create
                "keepSafe" -> LibrarySheet.KeepSafe
                "actions" -> LibrarySheet.Actions(f[1] as String)
                "rename" -> LibrarySheet.Rename(f[1] as String, f[2] as String)
                "move" -> LibrarySheet.Move(f[1] as String)
                "newFolder" -> LibrarySheet.NewFolder(f[1] as String, f[2] as Boolean)
                "folder" -> LibrarySheet.Folder(f[1] as String)
                "renameFolder" -> LibrarySheet.RenameFolder(f[1] as String)
                "moveMany" -> LibrarySheet.MoveMany(f.drop(1).map { it as String })
                "newFolderMany" -> LibrarySheet.NewFolderMany(f.drop(2).map { it as String }, f[1] as Boolean)
                else -> null
            }
        }.getOrNull()
    },
)

/** Where focus should go once the Shelf shows what a folder action did (A28, *Focus*). */
private sealed interface FocusWish {
    /** Into a folder: the control that leads back out. */
    data object Back : FocusWish

    /** Out of a folder: that folder's pile. */
    data class Pile(val folder: String) : FocusWish

    /** After an action: the tile that now holds this zine, once the zine is in [folder]. */
    data class Zine(val id: String, val folder: String?) : FocusWish

    /** The heading: after several zines are deleted, or when choosing stops with no zine to return to (A47.12). */
    data object Heading : FocusWish
}

/**
 * Whether the restored-details note is announced (A37.10): only when it **arrives** on a My Shelf that has already
 * drawn its zines in this visit. Back from the Bench, out of a folder or the About page, the note is there as the
 * Shelf draws and is read in order.
 *
 * Plain fields, not state, on purpose: nothing recomposes for them. [applied] runs after each applied composition
 * (a `SideEffect`), so [armed] means "the last My Shelf put on screen had no note". The composition in which the
 * note first appears on an armed Shelf leaves one announcement [pending]; the note [take]s it once, as it is first
 * composed, so a note scrolled away and back, or recomposed with new words, never takes a second one.
 *
 * ponytail: the take happens when the note's lazy cell first composes. A note that arrives while My Shelf is
 * scrolled past its first row is therefore announced when the maker scrolls back up and it comes into view, not
 * at the moment the store found the repair. Taking it at screen level would need the grid's visible range read
 * in composition; do that only if the device pass finds the late announcement confusing.
 */
private class NoteArrival {
    private var armed = false
    private var pending = false

    fun compose(noteUp: Boolean) {
        if (!noteUp) pending = false else if (armed) pending = true
    }

    fun applied(onMyShelf: Boolean, noteUp: Boolean) {
        armed = onMyShelf && !noteUp
    }

    fun take(): Boolean = pending.also { pending = false }
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
 * @param onDismissRestored *Got it* on the restored-details note, with every zine it names (A37.5).
 * @param onDeleteZines several zines deleted at once (A47.8): hide them and open one undo window.
 * @param onDeleteUndoMany that window closed with Undo pressed.
 * @param onDeleteCommitMany it closed without: each zine is now deleted on its own (A47.9).
 * @param onMoveZines `(ids, folder)`: several zines moved as one folder action with one Undo (A47.7).
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
    isDeleteWaiting: (String) -> Boolean,
    checkFolderName: (String) -> FolderNameVerdict,
    modifier: Modifier = Modifier,
    onDismissRestored: (List<String>) -> Unit = {},
    onDeleteZines: (List<String>) -> Unit = {},
    onDeleteUndoMany: (List<String>) -> Unit = {},
    onDeleteCommitMany: (List<String>) -> Unit = {},
    onMoveZines: (List<String>, String?) -> Unit = { _, _ -> },
) {
    val colors = ZinelyTheme.v21Colors
    val haptics = ZinelyTheme.haptics

    // Saved across recreation (rotation, dark-mode toggle): a half-typed name must not vanish with its sheet.
    var openSheet by rememberSaveable(stateSaver = LibrarySheetSaver) { mutableStateOf<LibrarySheet?>(null) }
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
    // A47.10: the zines being chosen, saved exactly as the open folder is, and never written to the zines' storage.
    var selection by rememberSaveable(stateSaver = ShelfSelection.Saver) { mutableStateOf<ShelfSelection?>(null) }
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
    val currentDeleteWaiting by rememberUpdatedState(isDeleteWaiting)
    val currentUndoMany by rememberUpdatedState(onDeleteUndoMany)
    val currentCommitMany by rememberUpdatedState(onDeleteCommitMany)

    LaunchedEffect(backupRestoreState) {
        if (backupRestoreState != null) openSheet = null
    }

    LaunchedEffect(backupRestoreState, state) {
        if (backupRestoreState != null) {
            restoreBackupActionFocus = true
        } else if (
            restoreBackupActionFocus &&
            (state is LibraryShelfState.Content || state is LibraryShelfState.Empty || state is LibraryShelfState.Error)
        ) {
            // Dialogs contain focus while visible. Ask for it back only after Compose has removed the
            // modal surface, so keyboard and accessibility users return to the action that opened it.
            backupActionFocusRequester.requestFocus()
            restoreBackupActionFocus = false
        }
    }

    LaunchedEffect(showColophon, state) {
        if (!showColophon && restoreColophonActionFocus &&
            (state is LibraryShelfState.Content || state is LibraryShelfState.Empty || state is LibraryShelfState.Error)
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
                // Prompts queue. One whose delete a folder action has finished meanwhile (rule 11) is not
                // shown: its Undo would be a button that does nothing.
                is HomeShelfEvent.DeletePrompt -> {
                    if (!currentDeleteWaiting(event.id)) return@collect
                    val outcome = CompletableDeferred<Boolean>()
                    undo = UndoRequest(event.id, homeDeletedMessage(event.title), outcome)
                    try {
                        if (outcome.await()) currentUndo(event.id) else currentCommit(event.id)
                    } finally {
                        undo = null
                        if (!outcome.isCompleted) currentCommit(event.id)
                    }
                }

                // A47.8: one window for the whole set. The ids a folder action has finished meanwhile are
                // dropped; the rest share the one Undo.
                is HomeShelfEvent.DeleteBatchPrompt -> {
                    val ids = event.ids.filter { currentDeleteWaiting(it) }
                    if (ids.isEmpty()) return@collect
                    val outcome = CompletableDeferred<Boolean>()
                    undo = UndoRequest(ids.first(), event.message, outcome)
                    try {
                        if (outcome.await()) currentUndoMany(ids) else currentCommitMany(ids)
                    } finally {
                        undo = null
                        if (!outcome.isCompleted) currentCommitMany(ids)
                    }
                }

                is HomeShelfEvent.Message -> {
                    val gone = CompletableDeferred<Unit>()
                    // The toast stands where the folder snack stands, so it waits for the snack to leave.
                    // Lowering the snack instead was the first answer, and it took the snack's Undo with
                    // it: a failure notice that happened to arrive inside those four seconds cost the
                    // maker the one way back from a move they had just made.
                    //
                    // The wait holds this collector, as a showing toast always has, so whatever arrives
                    // meanwhile comes after the notice. It ends when the snack times out, when its action
                    // is taken, or when a sheet is raised; while a finger or focus holds the snack up, the
                    // notice goes on waiting, because the snack is still where it would stand.
                    snapshotFlow { snackUp }.first { !it }
                    toast = event.text to gone
                    gone.await()
                    toast = null
                }

                // Not awaited: a newer folder snack replaces this one (A28.12), it does not queue behind it.
                // It does come after a notice that is showing or waiting above: those hold the collector.
                is HomeShelfEvent.FolderSnack -> {
                    snack = event
                    snackStep++
                    snackUp = true
                    event.zineId?.let { focusWish = FocusWish.Zine(it, event.folder) }
                }

                // A duplicate's copy: the same wish a folder snack makes, without the snack.
                is HomeShelfEvent.Reveal -> focusWish = FocusWish.Zine(event.zineId, event.folder)
            }
        }
    }

    val zines = (state as? LibraryShelfState.Content)?.zines.orEmpty()
    val folders = shelfFolders(zines)
    val tiles = shelfTiles(zines, openFolder)

    // A47.6, A47.10: only the zines of the view being shown can be chosen. A chosen zine that has gone is dropped,
    // and a choice made in another view (a folder opened or left) is over. Judged as the Shelf draws, so a gone
    // zine is never shown chosen for a frame; not while it is still loading, when there are no zines to judge by.
    val choice = if (state is LibraryShelfState.Loading) {
        null
    } else {
        selection?.prune(openFolder, tiles.mapNotNull { (it as? ShelfTile.Zine)?.zine?.id })
    }
    if (state !is LibraryShelfState.Loading) SideEffect { if (choice != selection) selection = choice }
    val choosing = choice != null

    // A37: the restored-details note, on My Shelf only (A37.2). It is ANNOUNCED only when it arrives on a My Shelf
    // that has already drawn its zines in this visit (A37.10). See [NoteArrival].
    val restored = (state as? LibraryShelfState.Content)?.restored.orEmpty()
    val noteUp = restored.isNotEmpty() && openFolder == null
    val onMyShelf = state is LibraryShelfState.Content && openFolder == null && !showColophon
    val arrival = remember { NoteArrival() }
    arrival.compose(noteUp)
    SideEffect { arrival.applied(onMyShelf, noteUp) }

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
            FocusWish.Back, FocusWish.Heading -> -1
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
    // folder snack shows. The host does the finishing, of this delete and of any whose prompt is still
    // queued; this closes the snack that is up. The collector then reports the commit as it does for a
    // snack that timed out, and the host knows it has that one in hand.
    val confirmFolderAction = { then: () -> Unit ->
        undo?.outcome?.complete(false)
        folderAtAction = openFolder
        openSheet = null
        // A47.4: finishing an action stops choosing. Only the dock's Move reaches here while choosing.
        selection = null
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

    // ---- A47: choosing several zines ---------------------------------------------------------------
    // Starting keeps focus where it was, on the zine just chosen (A47.12), and puts the snack away.
    val startChoosing = { id: String ->
        snackUp = false
        selection = ShelfSelection.start(id, openFolder)
    }
    // ✕ and Back: focus to the zine touched last, else the heading (A47.12). Leaving changes nothing.
    val stopChoosing = {
        val last = choice?.last
        selection = null
        focusWish = if (last != null && zines.any { it.id == last }) FocusWish.Zine(last, openFolder) else FocusWish.Heading
    }
    val toggleChosen = { id: String ->
        val next = choice?.toggle(id)
        selection = next
        // Letting go of the last zine stops choosing, with focus on that zine (A47.4, A47.12).
        if (next == null) focusWish = FocusWish.Zine(id, openFolder)
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
                key(gone) {
                    ZToast(
                        message = text,
                        onTimeout = { gone.complete(Unit) },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = TransientBottomInset),
                    )
                }
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
    // A47.4: registered after the folder's, so it is asked first: inside a folder, Back stops choosing and a
    // second Back leaves the folder (A28.9).
    BackHandler(enabled = choosing, onBack = stopChoosing)

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
                ZineShelfFail(
                    { haptics.perform(ZinelyHaptic.Tick); onRetry() },
                    Modifier.fillMaxSize(),
                    with(LocalDensity.current) { dockHeight.toDp() },
                )

            // `body.is-empty{.shelf:none;.empty:flex}`. The shelf and the invitation are alternatives,
            // and the half that matters is the *absence*: a screen that renders both looks correct in a
            // screenshot of either one.
            is LibraryShelfState.Empty -> ZineShelfEmpty(Modifier.fillMaxSize(), with(LocalDensity.current) { dockHeight.toDp() })

            // My Shelf and an open folder are two grids, each with its own scroll position, so going back
            // finds My Shelf where it was left (A28.9).
            is LibraryShelfState.Content -> key(openFolder != null) {
                ZineShelf(
                    zines = tiles.map { tile ->
                        when (tile) {
                            is ShelfTile.Zine -> ZineShelfItem(
                                title = tile.zine.title,
                                recipe = tile.zine.cover,
                                subtitle = tile.zine.subtitle,
                                choosing = choosing,
                                chosen = choice?.ids?.contains(tile.zine.id) == true,
                            )
                            is ShelfTile.Pile -> ZineShelfItem(
                                title = tile.name,
                                recipe = tile.zines.first().cover,
                                subtitle = pluralZineCount(tile.zines.size),
                                pile = tile.zines.map { it.cover },
                                choosing = choosing,
                            )
                        }
                    },
                    onOpen = { index ->
                        val tile = tiles.getOrNull(index)
                        // A47.2, A47.6: while choosing, a tap chooses a zine, and a folder only says why it cannot be.
                        if (choosing) {
                            when (tile) {
                                is ShelfTile.Zine -> toggleChosen(tile.zine.id)
                                is ShelfTile.Pile -> {
                                    snack = HomeShelfEvent.FolderSnack(Copy.Select.OPEN_FOLDER_FIRST)
                                    snackStep++
                                    snackUp = true
                                }
                                null -> Unit
                            }
                            return@ZineShelf
                        }
                        when (tile) {
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
                    onSelect = { index -> (tiles.getOrNull(index) as? ShelfTile.Zine)?.let { startChoosing(it.zine.id) } },
                    choosing = choosing,
                    // `.a47-on .restored{display:none}`: the note waits until choosing is over.
                    note = if (!noteUp || choosing) {
                        null
                    } else {
                        {
                            val (title, body) = restored.noteWords()
                            ShelfRestoredNote(
                                title = title,
                                body = body,
                                announce = remember { arrival.take() },
                                onGotIt = {
                                    haptics.perform(ZinelyHaptic.Tick)
                                    onDismissRestored(restored.map { it.id })
                                    // A37.5: focus goes to the "My Shelf" heading, brought on screen if the note
                                    // was long enough to push it off.
                                    scope.launch {
                                        if (runCatching { shelfFocus.heading.requestFocus() }.isFailure) {
                                            shelfGrid.scrollToItem(0)
                                            withFrameNanos { }
                                            runCatching { shelfFocus.heading.requestFocus() }
                                        }
                                    }
                                },
                            )
                        }
                    },
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

        // A47.3: `.a47-bar{z-index:30}` — over the shelf and its grain, under the dock and the sheets.
        choice?.let { ShelfChoosingBar(count = it.ids.size, onStop = stopChoosing, modifier = Modifier.align(Alignment.TopCenter)) }

        // `.dock{position:absolute;left:0;right:0;bottom:0}` — over the shelf, which scrolls under it and
        // clears it with bottom padding of its own (188dp on `.shelf`, 206dp on `.empty`; the failure
        // surface is cleared by the measured dock height),
        // each grown by the safe area — see [zineDockClearance]). In a `Column` after the shelf it would
        // look identical at rest and steal that space at every other moment.
        ZineDock(
            onStart = {
                haptics.perform(ZinelyHaptic.Tick)
                createPaperSnapshot = preferredPaper
                openSheet = LibrarySheet.Create
            },
            secondaryActions = when (state) {
                is LibraryShelfState.Content, is LibraryShelfState.Empty, is LibraryShelfState.Error -> listOf(
                    ZineDockSecondaryAction(
                        // `zines.isEmpty()` in Error means "could not read", not "empty": the frozen
                        // `v21-library.html` says "Backups" there (D-145), and the sheet opens restore-only.
                        label = if (state is LibraryShelfState.Empty || (state is LibraryShelfState.Content && zines.isEmpty())) {
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
            choosing = choice?.let { chosen ->
                val several = pluralZineCount(chosen.ids.size)
                ZineDockChoosing(
                    moveLabel = if (openFolder == null) Copy.Folders.MOVE_TO_A_FOLDER else Copy.Folders.MOVE_SOMEWHERE_ELSE,
                    moveName = if (openFolder == null) Copy.Select.moveToAFolder(several) else Copy.Select.moveSomewhereElse(several),
                    deleteName = Copy.Select.delete(several),
                    // A47.7. A choice of one is the single move, words and all; A28.14's first folder skips the chooser.
                    onMove = {
                        haptics.perform(ZinelyHaptic.Tick)
                        val ids = chosen.ids
                        openSheet = when {
                            ids.size == 1 && folders.isEmpty() -> LibrarySheet.NewFolder(ids.single(), fromChooser = false)
                            ids.size == 1 -> LibrarySheet.Move(ids.single())
                            folders.isEmpty() -> LibrarySheet.NewFolderMany(ids, fromChooser = false)
                            else -> LibrarySheet.MoveMany(ids)
                        }
                    },
                    // A47.8: no question first, as for one zine; focus goes to the heading (A47.12).
                    onDelete = {
                        haptics.perform(ZinelyHaptic.Boundary)
                        val ids = chosen.ids
                        selection = null
                        focusWish = FocusWish.Heading
                        if (ids.size == 1) onDeleteZine(ids.single()) else onDeleteZines(ids)
                    },
                )
            },
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
                // Undo goes back to where the action was taken: My Shelf at once, a folder once it is there again.
                if (snack.action == FolderSnackAction.Undo) {
                    if (folderAtAction == null) openFolder = null else reopenFolder = folderAtAction
                }
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

        // Above the dock, never under the thumb that made them. Measured, not V1's fixed 96dp: the dock
        // is taller than that now, and the bar sat on top of Make a zine.
        val aboveDock = with(LocalDensity.current) { dockHeight.toDp() } + FolderSnackGap
        undo?.let { request -> key(request) {
            ZSnackbar(
                message = request.message,
                actionLabel = Copy.Shelf.UNDO,
                onAction = { haptics.perform(ZinelyHaptic.Tick); request.outcome.complete(true) },
                onTimeout = { request.outcome.complete(false) },
                modifier = Modifier.align(Alignment.BottomCenter)
                    .padding(start = TransientSideInset, end = TransientSideInset, bottom = aboveDock),
            )
        } }
        toast?.let { (text, gone) -> key(gone) {
            ZToast(
                message = text,
                onTimeout = { gone.complete(Unit) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = aboveDock),
            )
        } }
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

    // In Error `zines` is empty because the read failed, so this opens restore-only. That is acceptable:
    // restore only adds, never replaces ([ADR-121](docs/DECISIONS.md#adr-121)); D-145.
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
                // A47.1: the way in that can be seen.
                ZineAction.Select -> {
                    haptics.perform(ZinelyHaptic.Tick)
                    openSheet = null
                    startChoosing(zine.id)
                    // A47.12: focus on the zine just chosen. The sheet's window would hand it back to the `⋯`,
                    // which is gone while choosing. (A long press needs none: focus is already on the tile.)
                    focusWish = FocusWish.Zine(zine.id, zine.folder)
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
    // A47.7: the chooser for a set, which is the zines still shown of those chosen. Its title is their count.
    // A set that has shrunk to one zine while open is that zine's chooser, in A28's words (#166).
    val shownOfMany = (openSheet as? LibrarySheet.MoveMany)?.zineIds?.filter { id -> zines.any { it.id == id } }
        ?.takeIf { it.isNotEmpty() }
    val moving = (openSheet as? LibrarySheet.Move)?.let { open -> zines.firstOrNull { it.id == open.zineId } }
        ?: shownOfMany?.singleOrNull()?.let { id -> zines.firstOrNull { it.id == id } }
    val movingMany = shownOfMany?.takeIf { it.size > 1 }
    FolderMoveSheet(
        target = when {
            moving != null -> FolderMoveTarget(moving.title, moving.folder, folders)
            movingMany != null -> pluralZineCount(movingMany.size).let { several ->
                FolderMoveTarget(several, openFolder, folders, paneTitle = Copy.Select.movePane(several))
            }
            else -> null
        },
        onMove = { folder ->
            haptics.perform(ZinelyHaptic.Snap)
            moving?.let { zine -> confirmFolderAction { onMoveZine(zine.id, folder) } }
            movingMany?.let { ids -> confirmFolderAction { onMoveZines(ids, folder) } }
        },
        onNewFolder = {
            haptics.perform(ZinelyHaptic.Tick)
            moving?.let { zine -> openSheet = LibrarySheet.NewFolder(zine.id, fromChooser = true) }
            movingMany?.let { ids -> openSheet = LibrarySheet.NewFolderMany(ids, fromChooser = true) }
        },
        onDismiss = { openSheet = null },
    )

    val naming = openSheet as? LibrarySheet.NewFolder
    val namingFor = naming?.let { open -> zines.firstOrNull { it.id == open.zineId } }
    val namingMany = openSheet as? LibrarySheet.NewFolderMany
    val namingForMany = namingMany?.zineIds?.filter { id -> zines.any { it.id == id } }?.takeIf { it.isNotEmpty() }
    val renamingFolder = (openSheet as? LibrarySheet.RenameFolder)?.name?.takeIf { it in folders }
    FolderNameSheet(
        target = when {
            namingFor != null -> FolderNameTarget(zineTitle = namingFor.title, zineFolder = namingFor.folder)
            namingForMany != null && namingForMany.size == 1 ->
                zines.firstOrNull { it.id == namingForMany.single() }
                    ?.let { FolderNameTarget(zineTitle = it.title, zineFolder = it.folder) }
            namingForMany != null -> FolderNameTarget(zineFolder = openFolder, zines = pluralZineCount(namingForMany.size))
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
                namingForMany != null -> confirmFolderAction { onMoveZines(namingForMany, name) }
                renamingFolder != null -> confirmFolderAction { onRenameFolder(renamingFolder, name) }
            }
        },
        // A28.14: Cancel returns to the chooser when the sheet came from the chooser.
        onCancel = {
            openSheet = when {
                naming != null && naming.fromChooser -> LibrarySheet.Move(naming.zineId)
                namingMany != null && namingMany.fromChooser -> LibrarySheet.MoveMany(namingMany.zineIds)
                else -> null
            }
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
 * zines never deletes one. Only for a screen with no dock (the colophon); over the shelf the bars are
 * placed from the measured dock.
 */
private val TransientBottomInset = 96.dp

/** The folder snack's own side inset (`A28.13`), so the two bars line up. */
private val TransientSideInset = 14.dp

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
