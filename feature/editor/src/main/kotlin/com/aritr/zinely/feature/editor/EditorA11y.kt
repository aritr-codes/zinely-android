package com.aritr.zinely.feature.editor

import androidx.compose.ui.semantics.CustomAccessibilityAction
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.ReorderOp
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
        is TextElement -> if (element.text.isBlank()) "Empty text" else "Text: ${element.text}"
        is ImageElement -> "Image"
    }

    /**
     * The custom actions exposed on a single element's semantic node. Each transform action **selects the
     * element first** (so the selection-scoped reducer intent acts on the right element even when focus
     * lands via TalkBack without a prior tap), then dispatches the twin intent. Reorder/delete are
     * id-scoped already. The visible contextbar reuses the same intents on the current selection.
     */
    public fun elementCustomActions(element: Element, dispatch: (Intent) -> Unit): List<CustomAccessibilityAction> {
        val id = element.id
        fun selectThen(action: () -> Unit): Boolean { dispatch(Intent.Select(id)); action(); return true }
        return buildList {
            // Editing text is the primary action for a text box (the a11y twin of the double-tap seam).
            if (element is TextElement) add(CustomAccessibilityAction("Edit text") { dispatch(Intent.BeginEditText(id)); true })
            add(CustomAccessibilityAction("Move left") { selectThen { dispatch(Intent.Nudge(PtPoint(-NUDGE_STEP_PT, 0.0))) } })
            add(CustomAccessibilityAction("Move right") { selectThen { dispatch(Intent.Nudge(PtPoint(NUDGE_STEP_PT, 0.0))) } })
            add(CustomAccessibilityAction("Move up") { selectThen { dispatch(Intent.Nudge(PtPoint(0.0, -NUDGE_STEP_PT))) } })
            add(CustomAccessibilityAction("Move down") { selectThen { dispatch(Intent.Nudge(PtPoint(0.0, NUDGE_STEP_PT))) } })
            add(CustomAccessibilityAction("Make larger") { selectThen { dispatch(Intent.ScaleBy(SCALE_STEP_FACTOR)) } })
            add(CustomAccessibilityAction("Make smaller") { selectThen { dispatch(Intent.ScaleBy(1.0 / SCALE_STEP_FACTOR)) } })
            add(CustomAccessibilityAction("Rotate clockwise") { selectThen { dispatch(Intent.RotateBy(ROTATE_STEP_DEGREES)) } })
            add(CustomAccessibilityAction("Rotate counterclockwise") { selectThen { dispatch(Intent.RotateBy(-ROTATE_STEP_DEGREES)) } })
            add(CustomAccessibilityAction("Bring forward") { dispatch(Intent.Reorder(id, ReorderOp.BRING_FORWARD)); true })
            add(CustomAccessibilityAction("Send backward") { dispatch(Intent.Reorder(id, ReorderOp.SEND_BACKWARD)); true })
            add(CustomAccessibilityAction("Delete") { dispatch(Intent.Delete(setOf(id))); true })
        }
    }
}
