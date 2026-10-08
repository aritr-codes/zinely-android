package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.PaperSize

/**
 * What the shelf hands across its boundary — the two model types and the handful of test tags that
 * outlived the V1 screen they were declared on.
 *
 * `HomeScreen.kt`, `ShelfCard.kt` and `ShelfStates.kt` were the V1 shelf: ~50 KB of composables that
 * `ZinelyNavHost` stopped routing to when the V2.1 Library ([com.aritr.zinely.feature.library.ZineLibraryScreen])
 * took the surface. They were deleted rather than left as a second, silently-diverging answer to *"which
 * zine do I want?"* — but the delete was not clean, and this file is why: eight of their declarations were
 * never V1-specific. [HomeZineCard] and [HomeShelfEvent] are the contract `HomeViewModel` speaks to *any*
 * shelf, and the tags below are read by [ShelfSheets], [com.aritr.zinely.feature.library.ZineShelf] and
 * [com.aritr.zinely.feature.library.ZineLibraryScreen], all of which are live.
 *
 * ⚠ **The `home`/`Home` prefixes are kept deliberately.** They are stale — there is no Home screen any
 * more — but they name test tags that a dozen live assertions match on as strings, and renaming a tag is a
 * behavioural change dressed as tidying. The rename belongs in its own change, with the tests, not
 * smuggled into a deletion.
 */

/** `.empty h2` — the invitation, which teaches rather than reports (`shelf.html:399`). */
public const val HomeEmptyHeadline: String = Copy.Shelf.HOME_EMPTY_HEADLINE

/** Test tags on the rename row inside the action sheet (`.rename input`, `.rename .save`). */
public const val HomeRenameFieldTestTag: String = "home-rename-field"
public const val HomeRenameConfirmTestTag: String = "home-rename-confirm"

/** Test tag on the Start-a-zine paper chooser (`#createSheet`, S7.1/ADR-047). */
public const val HomePaperChooserTestTag: String = "home-paper-chooser"

/** Test tag for one paper choice inside the chooser. */
public fun homePaperChoiceTestTag(paperSize: PaperSize): String =
    "home-paper-choice-${paperSize.name}"

/** Test tag for the drawn sheet inside one paper choice — the thing whose scale is asserted. */
public fun homePaperStockTestTag(paperSize: PaperSize): String =
    "home-paper-stock-${paperSize.name}"

/** Test tag for one zine card on the shelf, keyed by its stable project [id]. */
public fun homeCardTestTag(id: String): String = "home-card-$id"

/** `Deleted “X”` — `$("#snackText").textContent` (`shelf.html:723`). */
public fun homeDeletedMessage(title: String): String = Copy.Shelf.deletedMessage(title)

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

    /**
     * A folder action ended (`v21-library.html` A28.12, [ADR-125](docs/DECISIONS.md#adr-125)): raise the
     * Shelf's snack with [message]. Unlike the two above it does not hold the queue: a newer one replaces it.
     *
     * @property action what the snack offers, or `null` for none (a rename, and the line after an undo). The
     *   host remembers what it does; the screen only reports that it was pressed.
     * @property zineId the zine focus should follow, or `null` to leave focus where it is.
     * @property folder where that zine is now (`null` is My Shelf), so the screen can wait for the Shelf to
     *   show it there before moving focus.
     */
    public data class FolderSnack(
        val message: String,
        val action: FolderSnackAction? = null,
        val zineId: String? = null,
        val folder: String? = null,
    ) : HomeShelfEvent
}

/** The one thing a folder snack can offer. */
public enum class FolderSnackAction {
    /** Take the move, the new folder or the take-out back. */
    Undo,

    /** A rename or a take-out stopped partway (rule 18): run it again to finish it. */
    TryAgain,
}
