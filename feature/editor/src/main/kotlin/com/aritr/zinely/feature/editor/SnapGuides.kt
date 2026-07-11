package com.aritr.zinely.feature.editor

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.editor.SnapAxis
import com.aritr.zinely.core.editor.SnapGuide
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.render.android.ExportScale
import com.aritr.zinely.ui.theme.ZinelyTheme

/** Test tag on the snap-guide Canvas. */
public const val SnapGuidesTestTag: String = "snap-guides"

/**
 * The S4 live snap guides (ADR-029 §5.4): a draw-only overlay that strokes the alignment lines that fired
 * for the current gesture frame. Like [SelectionChrome] it draws in **screen space with a constant stroke
 * width** (never inside a scaling `graphicsLayer`) and carries no geometry of its own — each guide's
 * page-point position is mapped through the same preview seam ([ExportScale.previewPageToDevice]) the
 * renderer uses, so the line sits exactly where the snapped edge lands.
 *
 * Guides are **render-only** — produced fresh each frame by
 * [com.aritr.zinely.core.editor.LiveSnap.resolve] and never stored in history. An empty list draws
 * nothing. A `VERTICAL` guide is a full-height line at a constant device-x; `HORIZONTAL` a full-width line
 * at a constant device-y.
 *
 * @param guides the lines that fired this frame; empty ⇒ nothing drawn.
 * @param screenPxPerPt device px per point — MUST match the sibling [PagePreview].
 * @param pageOffset page-space pan applied before the screen scale — MUST match [PagePreview].
 * @param modifier sized identically to the sibling [PagePreview] so the device-px positions align.
 * @param color the guide stroke colour; defaults to the frozen `--teal` token at the spec's fired-guide
 *   opacity — bench.html strokes `.page .guide{ background:var(--teal) }` and reveals the firing line at
 *   `.guide.on{ opacity:.8 }`. A `SnapGuides` only ever draws guides that fired this frame (the `.on`
 *   state), so the default carries that `.8`. It reads distinctly from the coral selection chrome.
 */
@Composable
public fun SnapGuides(
    guides: List<SnapGuide>,
    screenPxPerPt: Float,
    pageOffset: PtPoint,
    modifier: Modifier = Modifier,
    color: Color = ZinelyTheme.colors.teal.copy(alpha = 0.8f),
) {
    Canvas(modifier = modifier.testTag(SnapGuidesTestTag)) {
        if (guides.isEmpty()) return@Canvas
        val strokePx = 1.dp.toPx()
        val toDevice = ExportScale.previewPageToDevice(screenPxPerPt.toDouble(), pageOffset)
        for (g in guides) {
            // Map a point ON the line; the off-axis coordinate is irrelevant (we span the whole canvas).
            val mapped = toDevice.map(PtPoint(g.positionPt, g.positionPt))
            when (g.axis) {
                SnapAxis.VERTICAL -> {
                    val x = mapped.x.toFloat()
                    drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = strokePx)
                }
                SnapAxis.HORIZONTAL -> {
                    val y = mapped.y.toFloat()
                    drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = strokePx)
                }
            }
        }
    }
}
