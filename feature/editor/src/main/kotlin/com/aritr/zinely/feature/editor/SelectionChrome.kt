package com.aritr.zinely.feature.editor

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.render.android.SelectionChromeGeometry
import com.aritr.zinely.ui.theme.ZinelyTheme

/** Test tag on the chrome Canvas. */
public const val SelectionChromeTestTag: String = "selection-chrome"

/**
 * The S4 selection chrome (ADR-029 §5, selection-chrome increment): a thin, draw-only overlay that
 * strokes the rotated outline of each selected element's box. It carries **no geometry of its own** —
 * every corner comes from [SelectionChromeGeometry.outlineDevicePx] (the same preview seam + rotation
 * sign as the renderer), so the outline sits exactly on the rendered box at any rotation.
 *
 * It is drawn in **screen space with a constant stroke width** — NOT inside a scaling `graphicsLayer`
 * (Codex review). During a live transform the caller passes the **live-baked** [transforms] (see
 * [com.aritr.zinely.core.editor.LivePreview]), so the outline tracks the gesture without the stroke
 * fattening under zoom or snapping back at commit.
 *
 * Non-text contrast (WCAG 1.4.11 ≥3:1): the stroke defaults to the frozen `--coral-strong` token — the
 * `box-shadow:0 0 0 2px var(--coral-strong)` the Bench spec strokes the selected block with (bench.html
 * `.block.sel`). Resize handles, snap guides, and the a11y contextbar land in the following increments;
 * this step is the selection boundary.
 *
 * @param transforms the (live-baked) committed boxes to outline; empty ⇒ nothing drawn.
 * @param screenPxPerPt device px per point — MUST match the sibling [PagePreview].
 * @param pageOffset page-space pan applied before the screen scale — MUST match [PagePreview].
 * @param modifier sizing applied by the caller; size it identically to the sibling [PagePreview] so the
 *   device-px corners align.
 * @param color the outline stroke colour; defaults to the frozen `--coral-strong` token.
 */
@Composable
public fun SelectionChrome(
    transforms: List<Transform>,
    screenPxPerPt: Float,
    pageOffset: PtPoint,
    modifier: Modifier = Modifier,
    color: Color = ZinelyTheme.colors.coralStrong,
) {
    Canvas(modifier = modifier.testTag(SelectionChromeTestTag)) {
        val strokePx = 2.dp.toPx()
        for (t in transforms) {
            val corners = SelectionChromeGeometry.outlineDevicePx(t, screenPxPerPt.toDouble(), pageOffset)
            if (corners.size != 4) continue
            val path = Path().apply {
                moveTo(corners[0].x.toFloat(), corners[0].y.toFloat())
                for (i in 1 until corners.size) lineTo(corners[i].x.toFloat(), corners[i].y.toFloat())
                close()
            }
            drawPath(path = path, color = color, style = Stroke(width = strokePx))
        }
    }
}
