package com.aritr.zinely.feature.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.ReorderOp
import com.aritr.zinely.core.model.PtPoint

/** Test tag on the contextbar surface; absent from the tree when there's no selection. */
public const val EditorContextBarTestTag: String = "editor-context-bar"

/**
 * The visible single-pointer transform controls (ADR-029 §6, WCAG 2.5.7) — the on-screen twin of the
 * gesture layer. Shown only when [selection] is non-empty; each control is a ≥48dp [IconButton] that
 * dispatches the **same** reducer intent the gesture commit and the per-element custom actions use
 * ([EditorA11y] step sizes), so the touch, a11y-action, and visible-button paths are one code path —
 * each press is one undo step.
 *
 * Nudge/scale/rotate act on the current selection (the reducer reads `model.selection`); reorder and
 * delete are id-scoped, so those are shown only for a **single** selected element. The bar pads the
 * navigation-bar inset for edge-to-edge (M3); the glyphs are decorative ([clearAndSetSemantics] sets the
 * spoken label on the button, not its text).
 *
 * @param selection the current selection; empty ⇒ nothing rendered.
 * @param dispatch forwards an [Intent] into the store.
 * @param modifier sizing/placement applied by the host.
 */
@Composable
public fun EditorContextBar(
    selection: Set<String>,
    dispatch: (Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selection.isEmpty()) return
    val singleId = selection.singleOrNull()

    Surface(
        modifier = modifier.testTag(EditorContextBarTestTag),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            BarButton("‹", "Move left") { dispatch(Intent.Nudge(PtPoint(-EditorA11y.NUDGE_STEP_PT, 0.0))) }
            BarButton("›", "Move right") { dispatch(Intent.Nudge(PtPoint(EditorA11y.NUDGE_STEP_PT, 0.0))) }
            BarButton("ʌ", "Move up") { dispatch(Intent.Nudge(PtPoint(0.0, -EditorA11y.NUDGE_STEP_PT))) }
            BarButton("v", "Move down") { dispatch(Intent.Nudge(PtPoint(0.0, EditorA11y.NUDGE_STEP_PT))) }
            BarButton("+", "Make larger") { dispatch(Intent.ScaleBy(EditorA11y.SCALE_STEP_FACTOR)) }
            BarButton("−", "Make smaller") { dispatch(Intent.ScaleBy(1.0 / EditorA11y.SCALE_STEP_FACTOR)) }
            BarButton("↻", "Rotate clockwise") { dispatch(Intent.RotateBy(EditorA11y.ROTATE_STEP_DEGREES)) }
            BarButton("↺", "Rotate counterclockwise") { dispatch(Intent.RotateBy(-EditorA11y.ROTATE_STEP_DEGREES)) }
            if (singleId != null) {
                BarButton("⤒", "Bring forward") { dispatch(Intent.Reorder(singleId, ReorderOp.BRING_FORWARD)) }
                BarButton("⤓", "Send backward") { dispatch(Intent.Reorder(singleId, ReorderOp.SEND_BACKWARD)) }
            }
            BarButton("⌫", "Delete") { dispatch(Intent.Delete(selection)) }
        }
    }
}

/** One ≥48dp control: a decorative [glyph] with the spoken [description] as the button's a11y label. */
@Composable
private fun BarButton(glyph: String, description: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .testTag("$EditorContextBarTestTag-$description")
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            },
    ) {
        Text(glyph, style = MaterialTheme.typography.titleMedium)
    }
}
