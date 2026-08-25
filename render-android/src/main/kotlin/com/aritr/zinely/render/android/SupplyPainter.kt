package com.aritr.zinely.render.android

import android.graphics.Canvas
import android.graphics.Paint
import com.aritr.zinely.core.render.SupplyOutline

/**
 * [DrawShape][com.aritr.zinely.core.render.DrawShape]'s paint, as a factory rather than as one field.
 *
 * Extracted so [CanvasReplayer] and [SupplyPainter] cannot drift apart: anti-aliasing on, no dither, FILL
 * (SUPPLIES-SPEC §3.5). The fill *rule* travels with the path — `SupplyOutline.toPath()` sets `EVEN_ODD` —
 * so a caller cannot get the paint right and the holes wrong.
 */
internal fun newShapePaint(): Paint = Paint().apply {
    style = Paint.Style.FILL
    isAntiAlias = true
    isDither = false
}

/**
 * Draws an authored supply outline **outside a page tape** — for a chooser tile, a thumbnail, a chip:
 * anywhere the mark must be shown at a size that is not a placement.
 *
 * ### Why this exists rather than a second path builder
 *
 * The Art sheet used to draw its own hand-authored 24-unit icons beside the catalogue, and they disagreed
 * with it: sixteen tiles were **stroked outlines** while every placement is a **fill**, `mark.halftone`'s
 * tile drew seven dots for a mark that has sixteen, and two earlier glyph corrections had already been
 * needed for the same reason. That is [D-093](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-093),
 * and the ruling on it is structural: **a tile cannot mispredict a mark it is.** So this shares the one
 * `toPath()` and the one paint recipe with [CanvasReplayer] — the tile becomes a fifth surface through the
 * *same* seam, not a fifth implementation of it.
 *
 * ⚠ It is deliberately **not** `CanvasReplayer.replay`. A tape carries page geometry — the §3.4.1 fold, the
 * page clip, a font resolver, an image blitter — and a tile has none of those: it is one outline in a box.
 * Routing a tile through the full replayer would mean inventing a one-command page and a font resolver to
 * ignore, which costs a bundled-font load per tile and says something false about what a tile is.
 *
 * Instances are cheap but hold one mutable [Paint]; construct one per drawing surface (`remember {}` in
 * Compose) rather than sharing a singleton across threads.
 */
public class SupplyPainter {

    private val paint = newShapePaint()
    private val pathCache = SupplyPathCache()

    /**
     * Fills [outline] into [canvas], stretched from its authored unit square to [widthPx] × [heightPx],
     * in [colorArgb].
     *
     * The scale is non-uniform whenever the box is not square — the same latitude the page fold has, and
     * for the same reason: the outline is authored in a unit square and the box carries the size. A caller
     * that wants the mark undistorted passes a square box; nothing here decides that for it.
     */
    public fun drawUnitSquare(
        canvas: Canvas,
        outline: SupplyOutline,
        colorArgb: Int,
        widthPx: Float,
        heightPx: Float,
    ) {
        paint.color = colorArgb
        val saved = canvas.save()
        canvas.scale(widthPx, heightPx)
        canvas.drawPath(pathCache.pathFor(outline), paint)
        canvas.restoreToCount(saved)
    }
}
