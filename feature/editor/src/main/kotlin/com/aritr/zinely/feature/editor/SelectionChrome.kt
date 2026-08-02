package com.aritr.zinely.feature.editor

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
 * **C2a re-skin (ADR-091 row 2.3):** the frozen `.sel` is `inset:-7px; border:1.5px solid var(--matcha);
 * border-radius:6px` (v2-bench.html `:155`) — so the outline is drawn 7 device px **outside** the box,
 * 1.5dp thick, in `--matcha`, with rounded corners. It replaced a 2dp `--coral-strong` stroke sitting
 * directly on the box edge. That re-skin does not weaken the WCAG 1.4.11 claim it inherits, it improves
 * it: on the sheet (`--paper #F7F2E7`) `--coral-strong` measures 4.11:1 and `--matcha` **5.20:1**, and
 * because [BenchSheetIsland] makes the sheet a light-theme island ([ADR-090](docs/DECISIONS.md#adr-090)),
 * 5.20:1 is the figure in **both** themes where the old one held only in light. 1.4.11 sets no minimum
 * stroke width, so 2dp → 1.5dp costs nothing against it.
 *
 * The inset and the radius are applied to the **rotated** quad, not to an axis-aligned bounding box: the
 * inflation happens in the element's local frame inside [SelectionChromeGeometry.outlineDevicePx], and the
 * corner arcs are built along the quad's own edges. A turned photo therefore keeps a parallel outline —
 * something the frozen CSS cannot express, and a capability the shipped chrome already had.
 *
 * @param transforms the (live-baked) committed boxes to outline; empty ⇒ nothing drawn.
 * @param screenPxPerPt device px per point — MUST match the sibling [PagePreview].
 * @param pageOffset page-space pan applied before the screen scale — MUST match [PagePreview].
 * @param modifier sizing applied by the caller; size it identically to the sibling [PagePreview] so the
 *   device-px corners align.
 * @param color the outline stroke colour; defaults to the frozen `--matcha` token. Read through
 *   [ZinelyTheme.v2Colors] so that inside the sheet island it is the on-paper value in both themes.
 * @param alpha the frozen `.sel{transition:opacity .12s}` fade (v2-bench.html `:155`). The caller drives
 *   it and keeps passing the *last* selection's boxes while it runs down to zero — otherwise there is
 *   nothing left to fade and the outline snaps out, which is the half of a transition that is easy to
 *   forget and impossible to see in a still.
 */
@Composable
public fun SelectionChrome(
    transforms: List<Transform>,
    screenPxPerPt: Float,
    pageOffset: PtPoint,
    modifier: Modifier = Modifier,
    color: Color = ZinelyTheme.v2Colors.matcha,
    alpha: Float = 1f,
) {
    if (alpha <= 0f) return
    Canvas(modifier = modifier.testTag(SelectionChromeTestTag)) {
        val strokePx = SelectionOutlineStrokeDp.toPx()
        val insetPx = SelectionOutlineInsetDp.toPx()
        val radiusPx = SelectionOutlineRadiusDp.toPx()
        for (t in transforms) {
            val corners = SelectionChromeGeometry.outlineDevicePx(
                t = t,
                screenPxPerPt = screenPxPerPt.toDouble(),
                pageOffset = pageOffset,
                inflateDevicePx = insetPx.toDouble(),
            )
            if (corners.size != 4) continue
            drawPath(
                path = roundedQuadPath(corners, radiusPx),
                color = color,
                alpha = alpha,
                style = Stroke(width = strokePx),
            )
        }
    }
}

/** Frozen `.sel` `border:1.5px` (v2-bench.html `:155`). */
internal val SelectionOutlineStrokeDp = 1.5.dp

/** Frozen `.sel` `inset:-7px` — outward, in screen space (v2-bench.html `:155`). */
internal val SelectionOutlineInsetDp = 7.dp

/** Frozen `.sel` `border-radius:6px` (v2-bench.html `:155`). */
internal val SelectionOutlineRadiusDp = 6.dp

/**
 * The frozen `border-radius:6px` on an arbitrarily **rotated** quad. `RoundRect` cannot express this —
 * it is axis-aligned — so each corner is cut back by [radiusPx] along both of its own edges and bridged
 * by a quadratic through the original corner point. On an unrotated box this is pixel-equivalent to a
 * rounded rect; on a rotated one it is the only reading that keeps the outline parallel to the element.
 *
 * The cut is clamped to half the shorter adjacent edge, so a box smaller than the radius degenerates to
 * a plain quad instead of folding its corners inside out.
 */
internal fun roundedQuadPath(corners: List<PtPoint>, radiusPx: Float): Path {
    val n = corners.size
    val pts = corners.map { Offset(it.x.toFloat(), it.y.toFloat()) }
    val path = Path()
    // Per corner: the point `radius` back along the incoming edge, and `radius` forward along the outgoing
    // one. Both are clamped by their own edge's half-length so adjacent corners can never overrun.
    fun towards(from: Offset, to: Offset): Offset {
        val d = to - from
        val len = kotlin.math.hypot(d.x, d.y)
        if (len <= 0f) return from
        val r = kotlin.math.min(radiusPx, len / 2f)
        return from + d * (r / len)
    }
    for (i in 0 until n) {
        val prev = pts[(i + n - 1) % n]
        val cur = pts[i]
        val next = pts[(i + 1) % n]
        val entry = towards(cur, prev)
        val exit = towards(cur, next)
        if (i == 0) path.moveTo(entry.x, entry.y) else path.lineTo(entry.x, entry.y)
        path.quadraticTo(cur.x, cur.y, exit.x, exit.y)
    }
    path.close()
    return path
}
