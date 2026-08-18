package com.aritr.zinely.feature.editor

import androidx.compose.ui.semantics.CustomAccessibilityAction
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.ReorderOp
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.Element
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.TextElement

/**
 * The editor's accessibility vocabulary (ADR-029 §6, WCAG 2.5.7). Every gesture transform has a
 * single-pointer twin here that dispatches the **same** reducer intent the gesture commit does
 * ([Intent.Nudge]/[Intent.ScaleBy]/[Intent.RotateBy] → one `TransformCommand` = one undo step), so the
 * touch and a11y paths can never diverge. The step sizes are the discrete increments the contextbar
 * buttons and the per-element custom actions both use.
 */
public object EditorA11y {

    /** One nudge step in points — a visible-but-fine move (≈ 1/18"); the arrow buttons/actions use it. */
    public const val NUDGE_STEP_PT: Double = 4.0

    /** One scale step as a multiplicative factor; "smaller" uses its reciprocal. Centre-anchored (§5.3). */
    public const val SCALE_STEP_FACTOR: Double = 1.1

    /** One rotation step in degrees (clockwise positive, matching the model/renderer). */
    public const val ROTATE_STEP_DEGREES: Double = 15.0

    /** A short spoken label for an element's semantic node (TalkBack reads this on focus). */
    public fun label(element: Element): String = when (element) {
        is TextElement -> if (element.text.isBlank()) Copy.A11y.EMPTY_TEXT else Copy.A11y.textLabel(element.text)
        is ImageElement -> Copy.A11y.PHOTO
        is DecorElement -> decorLabel(element.supplyId)
    }

    /**
     * A spoken name for a supply — the authored name from [Copy.Supplies], never derived from the id.
     *
     * **This function used to split the id and speak the halves**, so `shape.rect` was read aloud as
     * *"Rect shape"* and `fix.corner` as *"Corner fix"* — sixteen labels in a vocabulary no maker uses,
     * one of which ("fix") is not even a noun. Its KDoc excused that on the grounds that the real names
     * were being written elsewhere in the same cycle. They landed; the excuse expired; this is the
     * replacement that KDoc promised.
     *
     * The id prefix is **not** the family, which is the trap that made the old derivation wrong twice
     * over: five prefixes (`tape` · `fix` · `mark` · `paper` · `shape`) carry four families, because
     * *Tape & fixings* is one tape plus three fixings. [Copy.Supplies] is the only place that mapping
     * is correct.
     *
     * Still **not** the final §8 string, and the remaining gap is honest: §8 specifies
     * `"<supply name>, <size>, <colour name>"` — e.g. *"Star, medium, berry"*. The name is now real;
     * size has no vocabulary anywhere yet, and the colour name needs a `ColorRgba` →
     * `ZinelyMakerInkId` reverse lookup against `:core:ui`'s palette. Both are S7.
     *
     * Pure and total: any string in, a non-empty string out. An id outside the sixteen is a document
     * the validator would reject, and a semantic node with an empty label is worse than a vague one.
     */
    internal fun decorLabel(supplyId: String): String =
        Copy.Supplies.NAMES[supplyId] ?: DECOR_LABEL_FALLBACK

    /**
     * Spoken only for a `supplyId` outside the authored sixteen — which the validator already flags
     * (`decor.supplyId.blank`), so reaching it means a document that should not have loaded.
     */
    internal const val DECOR_LABEL_FALLBACK: String = "Supply"

    /**
     * The custom actions exposed on a single element's semantic node. Each transform action **selects the
     * element first** (so the selection-scoped reducer intent acts on the right element even when focus
     * lands via TalkBack without a prior tap), then dispatches the twin intent. Reorder/delete are
     * id-scoped already. The visible contextbar reuses the same intents on the current selection.
     */
    public fun elementCustomActions(
        element: Element,
        dispatch: (Intent) -> Unit,
        onDelete: (String) -> Unit = { dispatch(Intent.Delete(setOf(it))) },
        /**
         * Opens the ink popover on a supply — SUPPLIES-SPEC §8's `Change ink`. A callback rather than an
         * [Intent] because the popover is **screen state, not document state**: there is no intent to open
         * it, exactly as there is none for the visible `Ink` verb, which flips the same flag.
         *
         * **`null`, not a no-op lambda.** It was `= {}`, documented as making the action "simply absent for
         * any host with no popover to open" — which it did not do: the action was added unconditionally for
         * decor and the default merely made it *inert*, so a default host advertised a dead action. That is
         * the exact failure this function's decor branch spent all of P1 refusing to commit, reintroduced by
         * a default value. Independent review caught the gap between the sentence and the code.
         *
         * Nullable makes the sentence true: no callback, no action. Two test hosts
         * (`KeepClearPlatformStateTest`, `ElementSemanticsLayerTest`) take the default and correctly see
         * nothing.
         */
        onChangeInk: ((String) -> Unit)? = null,
        /**
         * Opens the Art cabinet on a supply — SUPPLIES-SPEC §8's `Replace supply`. A callback for the same
         * reason [onChangeInk] is one: the sheet is screen state, and the visible verb flips exactly this.
         *
         * `null` (the default) withholds the action entirely.
         */
        onReplaceSupply: ((String) -> Unit)? = null,
    ): List<CustomAccessibilityAction> {
        val id = element.id
        fun selectThen(action: () -> Unit): Boolean { dispatch(Intent.Select(id)); action(); return true }
        return buildList {
            // Editing text is the primary action for a text box (the a11y twin of the double-tap seam).
            if (element is TextElement) add(CustomAccessibilityAction(Copy.A11y.EDIT_TEXT) { dispatch(Intent.BeginEditText(id)); true })
            // Reframe is the photo's primary in-place action (ADR-053, the a11y twin of the double-tap →
            // Reframe seam and the affordance chip); Reset framing is the one-shot revert-to-placement
            // (bench block-menu "Reset framing" → Intent.ResetFraming, one undoable command). Both are the
            // authoritative Switch Access / TalkBack path — they drive the same reducer intents as the
            // visual affordances. (Replace-picture defers to IF4: it needs a pick-for-replace effect.)
            if (element is ImageElement) {
                add(CustomAccessibilityAction(Copy.A11y.REFRAME_PHOTO) { dispatch(Intent.BeginReframe(id)); true })
                add(CustomAccessibilityAction(Copy.A11y.RESET_FRAMING) { dispatch(Intent.ResetFraming(id)); true })
            }
            // ✅ **Both of decor's §8 actions now exist, and §8's table of 13 is met.** P1's note here said
            // decor got none, because each wanted a `Copy.A11y` string and a reducer intent that did not
            // exist. Both now have both — and, the condition P1 actually set, both now *do* something:
            // `Change ink` opens the ink popover ([Intent.InkSupply]); `Replace supply` opens the Art
            // cabinet on this element ([Intent.ReplaceSupply]).
            //
            // ⚠ Each is gated on its own callback being non-null, not merely on the element being decor.
            // A host with no popover and no cabinet must advertise **neither** — an action that dispatches
            // nothing costs a blind maker a gesture to discover the same emptiness a sighted maker sees
            // greyed out at a glance, and that argument is why both were withheld for a whole phase.
            //
            // With these two, decor's 11 shared actions become 13: a supply can be moved, resized, rotated,
            // restacked, deleted, recoloured and swapped entirely by TalkBack.
            if (element is DecorElement) {
                if (onChangeInk != null) {
                    add(CustomAccessibilityAction(Copy.A11y.CHANGE_INK) { selectThen { onChangeInk(id) } })
                }
                if (onReplaceSupply != null) {
                    add(CustomAccessibilityAction(Copy.A11y.REPLACE_SUPPLY) { selectThen { onReplaceSupply(id) } })
                }
            }
            add(CustomAccessibilityAction(Copy.A11y.MOVE_LEFT) { selectThen { dispatch(Intent.Nudge(PtPoint(-NUDGE_STEP_PT, 0.0))) } })
            add(CustomAccessibilityAction(Copy.A11y.MOVE_RIGHT) { selectThen { dispatch(Intent.Nudge(PtPoint(NUDGE_STEP_PT, 0.0))) } })
            add(CustomAccessibilityAction(Copy.A11y.MOVE_UP) { selectThen { dispatch(Intent.Nudge(PtPoint(0.0, -NUDGE_STEP_PT))) } })
            add(CustomAccessibilityAction(Copy.A11y.MOVE_DOWN) { selectThen { dispatch(Intent.Nudge(PtPoint(0.0, NUDGE_STEP_PT))) } })
            add(CustomAccessibilityAction(Copy.A11y.MAKE_LARGER) { selectThen { dispatch(Intent.ScaleBy(SCALE_STEP_FACTOR)) } })
            add(CustomAccessibilityAction(Copy.A11y.MAKE_SMALLER) { selectThen { dispatch(Intent.ScaleBy(1.0 / SCALE_STEP_FACTOR)) } })
            add(CustomAccessibilityAction(Copy.A11y.ROTATE_CLOCKWISE) { selectThen { dispatch(Intent.RotateBy(ROTATE_STEP_DEGREES)) } })
            add(CustomAccessibilityAction(Copy.A11y.ROTATE_COUNTERCLOCKWISE) { selectThen { dispatch(Intent.RotateBy(-ROTATE_STEP_DEGREES)) } })
            add(CustomAccessibilityAction(Copy.A11y.BRING_FORWARD) { dispatch(Intent.Reorder(id, ReorderOp.BRING_FORWARD)); true })
            add(CustomAccessibilityAction(Copy.A11y.SEND_BACKWARD) { dispatch(Intent.Reorder(id, ReorderOp.SEND_BACKWARD)); true })
            // Routed through the host's [onDelete] rather than dispatching `Intent.Delete` directly, so
            // the accessibility path gets the **same reversible delete** the visible verb does: the fade,
            // the 3200ms snack and its `Undo` ([ADR-094](../DECISIONS.md#adr-094) row 4.13). Dispatching
            // here made the element simply vanish for a TalkBack user while a sighted user got an undo
            // affordance — the reversal capability present on one path and absent on the other. The
            // default keeps every existing caller behaving exactly as before.
            add(CustomAccessibilityAction(Copy.A11y.DELETE) { onDelete(id); true })
        }
    }
}
