package com.aritr.zinely.feature.editor

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import com.aritr.zinely.ui.theme.ZinelyV2Dimens
import com.aritr.zinely.core.editor.SnapAxis
import com.aritr.zinely.core.editor.SnapGuide
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtSize
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
 * nothing. A `VERTICAL` guide runs the page's height at a constant device-x; `HORIZONTAL` the page's width
 * at a constant device-y — both less the frozen 8dp at each end (see the V2 re-skin note below).
 *
 * ### V2 re-skin (C1, ADR-089 row 1.10)
 *
 * The frozen Bench draws this line as `.guide{background:var(--matcha)}` revealed at
 * `.guide.show{opacity:.6}`, 1px wide, and **inset 8px from the page's top and bottom**
 * (`.guide.v{top:8px;bottom:8px}` — `v2-bench.html:124-126`). Only the appearance changed here: the
 * trigger is still the existing `:core:editor` `Snap`/`LiveSnap`, and a `SnapGuides` still only ever
 * draws guides that fired this frame, so the `.show` opacity is the resting default.
 *
 * ### V2.1 re-skin (P2, [ADR-102 §12.9](../../../../../../../../docs/DECISIONS.md#adr-102-p2-marks))
 *
 * `.guideV{border-left:1.5px dashed var(--butter)}` at `opacity:.85` (`v21-bench.html:189-190`). Three
 * changes: `matcha` → `butter`, solid → **dashed**, and 1dp → 1.5dp. The end-inset and the page-derived
 * geometry above are unchanged.
 *
 * ⚠ **37596 `butter` at .85 alpha composites to 1.20:1 on the sheet's paper, and that is a ruled,
 * measured acceptance** — not an oversight and not a value to "fix" by darkening. The direct
 * `butter/paper` pairing is 1.24:1, and V2.1's own spec says butter carries *"no
 * action, no text, and no state alone"*. A snap guide is arguably a state, which made this a
 * freeze-versus-spec conflict ([§12.6 row 4](../../../../../../../../docs/DECISIONS.md#adr-102-p1-corrections)).
 * The owner ruled on 2026-08-13 that the guide is **decorative** in 1.4.11's sense: it reports an
 * alignment the user can watch happening to their own element, during their own drag, and it is
 * redundant with the element's visible position. Worth knowing what that costs: V2's `matcha` guide was
 * 2.18:1 — also a failure — so this is not a regression from a compliant state. **There has never been
 * one.** If [D-064](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md) later rules that the floor
 * binds decorative marks, this line and the keep-clear cue move together, and neither frozen hue
 * survives at any alpha.
 *
 * The frozen file draws only the **vertical** guide, because that is the only one its prototype ever
 * fires. The engine produces both axes, so the 8dp end-inset is applied symmetrically — a horizontal
 * guide stops 8dp short of the left and right edges. Stated rather than assumed: the alternative,
 * insetting one axis and letting the other run full-bleed, would look like a bug on the first
 * horizontal snap.
 *
 * **The inset is measured from the page, not from this canvas** (review RF3). In the frozen file
 * `.guide` is a child of `.page`, so `top:8px` insets from the *sheet*. This canvas is the whole
 * editor area, and those two rectangles used to be near enough the same thing because the page was
 * anchored at the top-left and roughly filled it — until C1 centred the page, which is exactly the
 * change that made them differ. Measuring from the canvas would run a horizontal guide across the
 * desk on either side of the paper. The page rect is therefore derived from the same seam every
 * sibling reads, so the line ends where the sheet does.
 *
 * @param guides the lines that fired this frame; empty ⇒ nothing drawn.
 * @param screenPxPerPt device px per point — MUST match the sibling [PagePreview].
 * @param pageOffset page-space pan applied before the screen scale — MUST match [PagePreview].
 * @param pageSizePt the sheet, in points — the rectangle [endInset] is measured from.
 * @param modifier sized identically to the sibling [PagePreview] so the device-px positions align.
 * @param color the guide stroke colour; defaults to the frozen `--butter` token at `.85`, read through
 *   [ZinelyTheme.v21Colors] so it takes the on-paper value inside the sheet island in both themes.
 * @param endInset how far the line stops short of the **page's** edges — the frozen 8px.
 */
@Composable
public fun SnapGuides(
    guides: List<SnapGuide>,
    screenPxPerPt: Float,
    pageOffset: PtPoint,
    pageSizePt: PtSize,
    modifier: Modifier = Modifier,
    color: Color = ZinelyTheme.v21Colors.butter.copy(alpha = BenchStudio.GUIDE_ALPHA),
    endInset: Dp = BenchStudio.GuideEndInset,
) {
    Canvas(modifier = modifier.testTag(SnapGuidesTestTag)) {
        if (guides.isEmpty()) return@Canvas
        val strokePx = BenchStudio.PageBorder.toPx()
        // The frozen `dashed`, on the same reasoning [BenchStudio.KeepClearDash] records for the other
        // dashed mark: CSS names the style and no length, so this reproduces what the prototype's own
        // renderer draws rather than inventing a number the freeze does not contain.
        val dashPx = BenchStudio.KeepClearDash.toPx()
        val dash = PathEffect.dashPathEffect(floatArrayOf(dashPx, dashPx))
        val inset = endInset.toPx()
        val toDevice = ExportScale.previewPageToDevice(screenPxPerPt.toDouble(), pageOffset)
        // The sheet's own rectangle in device px, through the same seam the render uses — the frozen
        // `.guide` is a child of `.page`, so this is what the 8px is measured from.
        val pageTopLeft = toDevice.map(PtPoint(0.0, 0.0))
        val pageBottomRight = toDevice.map(PtPoint(pageSizePt.width, pageSizePt.height))
        val left = pageTopLeft.x.toFloat() + inset
        val top = pageTopLeft.y.toFloat() + inset
        val right = pageBottomRight.x.toFloat() - inset
        val bottom = pageBottomRight.y.toFloat() - inset
        for (g in guides) {
            // Map a point ON the line; the off-axis coordinate is irrelevant (the span comes from the page).
            val mapped = toDevice.map(PtPoint(g.positionPt, g.positionPt))
            when (g.axis) {
                SnapAxis.VERTICAL -> {
                    if (bottom <= top) continue
                    drawLine(color, Offset(mapped.x.toFloat(), top), Offset(mapped.x.toFloat(), bottom), strokeWidth = strokePx, pathEffect = dash)
                }
                SnapAxis.HORIZONTAL -> {
                    if (right <= left) continue
                    drawLine(color, Offset(left, mapped.y.toFloat()), Offset(right, mapped.y.toFloat()), strokeWidth = strokePx, pathEffect = dash)
                }
            }
        }
    }
}
