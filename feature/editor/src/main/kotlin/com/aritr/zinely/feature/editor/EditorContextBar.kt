package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
 * navigation-bar inset for edge-to-edge (M3).
 *
 * **Visual (docs/design/editor-visual-direction.md §3–4).** The bar is a "desk" band (`background`) that
 * matches the page strip and supply tray; each control is a small **stamped paper craft-chip**
 * (`surface`/`onSurface`, rounded, a slight deterministic handmade tilt) carrying a real Material icon
 * rather than a unicode placeholder glyph — so the last unstyled editor chrome reads as designed
 * supplies, not productivity-template text. The chip and tilt are decorative: the icon is hidden from
 * the a11y tree and [clearAndSetSemantics] puts the spoken label on the (axis-aligned, ≥48dp) button.
 *
 * The control row **scrolls horizontally** (same pattern as [EditorPageStrip]): with up to eleven ≥48dp
 * controls the set overflows a narrow phone, so scrolling keeps every control reachable without shrinking
 * any target below 48dp (DESIGN-RULES 1, 7). Scrolling changes layout only — no action, intent, gating,
 * or semantic label changes.
 *
 * @param selection the current selection; empty ⇒ nothing rendered.
 * @param dispatch forwards an [Intent] into the store.
 * @param modifier sizing/placement applied by the host.
 */
// The directional/rotation glyphs use the non-AutoMirrored Filled icons on purpose: these controls are
// spatial (page-space "left" is screen-left in any layout direction), so RTL auto-mirroring would point
// the arrow the wrong way. The glyph is decorative regardless (cleared from the a11y tree), so the
// deprecation hint toward AutoMirrored does not apply here.
@Suppress("DEPRECATION")
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
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarButton(Icons.Filled.KeyboardArrowLeft, "Move left", -2.5f) { dispatch(Intent.Nudge(PtPoint(-EditorA11y.NUDGE_STEP_PT, 0.0))) }
            BarButton(Icons.Filled.KeyboardArrowRight, "Move right", 2f) { dispatch(Intent.Nudge(PtPoint(EditorA11y.NUDGE_STEP_PT, 0.0))) }
            BarButton(Icons.Filled.KeyboardArrowUp, "Move up", -2f) { dispatch(Intent.Nudge(PtPoint(0.0, -EditorA11y.NUDGE_STEP_PT))) }
            BarButton(Icons.Filled.KeyboardArrowDown, "Move down", 2.5f) { dispatch(Intent.Nudge(PtPoint(0.0, EditorA11y.NUDGE_STEP_PT))) }
            BarButton(Icons.Filled.Add, "Make larger", -1.5f) { dispatch(Intent.ScaleBy(EditorA11y.SCALE_STEP_FACTOR)) }
            BarButton(Icons.Filled.Remove, "Make smaller", 1.5f) { dispatch(Intent.ScaleBy(1.0 / EditorA11y.SCALE_STEP_FACTOR)) }
            BarButton(Icons.Filled.RotateRight, "Rotate clockwise", -2f) { dispatch(Intent.RotateBy(EditorA11y.ROTATE_STEP_DEGREES)) }
            BarButton(Icons.Filled.RotateLeft, "Rotate counterclockwise", 2f) { dispatch(Intent.RotateBy(-EditorA11y.ROTATE_STEP_DEGREES)) }
            if (singleId != null) {
                BarButton(Icons.Filled.FlipToFront, "Bring forward", -1.5f) { dispatch(Intent.Reorder(singleId, ReorderOp.BRING_FORWARD)) }
                BarButton(Icons.Filled.FlipToBack, "Send backward", 1.5f) { dispatch(Intent.Reorder(singleId, ReorderOp.SEND_BACKWARD)) }
            }
            BarButton(Icons.Filled.Delete, "Delete", -2.5f) { dispatch(Intent.Delete(selection)) }
        }
    }
}

/**
 * One ≥48dp control: a stamped paper craft-chip carrying a decorative [icon] with the spoken
 * [description] as the button's a11y label. The chip + [tilt] sit *inside* the [IconButton], so the
 * touch target stays the standard axis-aligned 48dp box while only the paper stamp leans.
 */
@Composable
private fun BarButton(icon: ImageVector, description: String, tilt: Float, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .testTag("$EditorContextBarTestTag-$description")
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            },
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer { rotationZ = tilt }
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
