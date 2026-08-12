package com.aritr.zinely.feature.editor

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.render.android.SelectionChromeGeometry
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens

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
 * ### V2.1 re-skin (ADR-102 P1) — the ring is drawn, not lit
 *
 * The frozen `.el .ring` is `inset:-6px; border:1.6px **dashed** var(--ink); border-radius:var(--br-sm)`
 * (`v21-bench.html:145`), and the freeze's own banner states the rule it serves: **selection is a
 * hand-drawn dashed ring in `ink`, never a system box.** So the outline is drawn 6 device px outside the
 * box, 1.6dp thick, dashed, in `ink`.
 *
 * It replaced V2's `1.5px solid var(--matcha)` at `inset:-7px`, which had itself replaced a 2dp
 * `--coral-strong` stroke on the box edge. **`matcha` has no V2.1 successor on this surface** — ADR-102
 * §3's table mapped it to `leaf`, and nothing in the frozen page uses `leaf` for chrome
 * ([§12.1](../../../../../../../../docs/DECISIONS.md#adr-102-island-v21)).
 *
 * **The contrast claim gets stronger, not weaker.** `ink` on `paper` is the palette's AA-critical body
 * pairing and measures far above `matcha`'s 5.20:1; because [BenchSheetIsland] lights `ink` and `paper`
 * together in both themes, that figure holds at night as well as by day. WCAG 1.4.11 sets no minimum
 * stroke width, so 1.5dp → 1.6dp is neutral against it — and **dashing does not weaken it either**: a
 * dash is an absence of the mark, not a lower-contrast mark, and the segments that are drawn are drawn
 * at full ink.
 *
 * ⚠ **The ring is guaranteed only over paper.** Where it crosses the user's own photograph no token can
 * promise a ratio — that is the constraint [BenchHandleMark]'s halo exists for, and the ring has no
 * equivalent because it is a closed figure whose far side is almost always on paper. Recorded so the
 * difference between the two marks is a decision rather than an inconsistency.
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
 * @param color the outline stroke colour; defaults to the frozen `--ink` token. Read through
 *   [ZinelyTheme.v21Colors] so that inside the sheet island it is the on-paper value in both themes.
 * @param alpha the frozen `.el .ring{transition:opacity .15s}` fade. The caller drives
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
    color: Color = ZinelyTheme.v21Colors.ink,
    alpha: Float = 1f,
) {
    if (alpha <= 0f) return
    Canvas(modifier = modifier.testTag(SelectionChromeTestTag)) {
        val strokePx = SelectionOutlineStrokeDp.toPx()
        val insetPx = SelectionOutlineInsetDp.toPx()
        val radiusPx = SelectionOutlineRadiusDp.toPx()
        val dashPx = SelectionOutlineDashDp.toPx()
        val dash = PathEffect.dashPathEffect(floatArrayOf(dashPx, dashPx))
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
                style = Stroke(width = strokePx, pathEffect = dash),
            )
        }
    }
}

/** Frozen `.el .ring` `border:1.6px` (`v21-bench.html:145`). */
internal val SelectionOutlineStrokeDp = 1.6.dp

/** Frozen `.el .ring` `inset:-6px` — outward, in screen space (`v21-bench.html:145`). */
internal val SelectionOutlineInsetDp = 6.dp

/** Frozen `.el .ring` `border-radius:var(--br-sm)` (`v21-bench.html:146`). */
internal val SelectionOutlineRadiusDp = ZinelyV21Dimens.radiusSm

/**
 * The dash period. CSS names `dashed` and specifies **no** length, so the frozen file has no number to
 * transcribe; every browser picks its own. Chrome — the renderer the prototype is read in — draws a
 * 1.6px dashed border at roughly `2 × width` on, `2 × width` off.
 *
 * Recorded rather than silently chosen, and expressed against the stroke rather than as a bare `3.2.dp`,
 * so that a future change to the ring's weight keeps the dash in proportion instead of quietly turning
 * it into a dotted line. [BenchStudio.KeepClearDash] records the same reasoning for the other dashed
 * mark on this surface, and reached a different number from a different stroke width — which is the
 * point.
 */
internal val SelectionOutlineDashDp = SelectionOutlineStrokeDp * 2f

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
