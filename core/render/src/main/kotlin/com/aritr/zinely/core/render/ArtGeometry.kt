package com.aritr.zinely.core.render

import com.aritr.zinely.core.model.PtPoint

/** Shared construction helpers for the family art objects (moved verbatim out of [SupplyCatalog]). */
internal object ArtGeometry {

    /** The circle's Bézier constant `4/3·(√2 − 1)`, scaled by the unit circle's radius of `0.5`. */
    internal const val KAPPA: Double = 0.5522847498307936 * 0.5

    /** One axis-aligned rectangle as a single closed subpath, from (x0,y0) to (x1,y1). */
    internal fun bar(x0: Double, y0: Double, x1: Double, y1: Double): List<Subpath> = closed(
        PtPoint(x0, y0), PtPoint(x1, y0), PtPoint(x1, y1), PtPoint(x0, y1),
    ).subpaths

    internal fun p24(x: Number, y: Number): PtPoint = PtPoint(x.toDouble() / 24.0, y.toDouble() / 24.0)

    internal fun bar24(x0: Number, y0: Number, x1: Number, y1: Number): List<Subpath> =
        bar(x0.toDouble() / 24.0, y0.toDouble() / 24.0, x1.toDouble() / 24.0, y1.toDouble() / 24.0)

    /**
     * One circle of radius [r] about ([cx],[cy]), as the same four-cubic approximation [ShapeArt.CIRCLE] uses.
     *
     * [KAPPA] is already folded by half for the unit-square circle, so it is doubled back out here and
     * re-scaled by [r] — the alternative is a second constant that means the same thing.
     */
    internal fun circle(cx: Double, cy: Double, r: Double): List<Subpath> {
        val k = KAPPA * 2.0 * r
        return listOf(
            Subpath(
                start = PtPoint(cx, cy - r),
                segments = listOf(
                    Segment.CubicTo(PtPoint(cx + k, cy - r), PtPoint(cx + r, cy - k), PtPoint(cx + r, cy)),
                    Segment.CubicTo(PtPoint(cx + r, cy + k), PtPoint(cx + k, cy + r), PtPoint(cx, cy + r)),
                    Segment.CubicTo(PtPoint(cx - k, cy + r), PtPoint(cx - r, cy + k), PtPoint(cx - r, cy)),
                    Segment.CubicTo(PtPoint(cx - r, cy - k), PtPoint(cx - k, cy - r), PtPoint(cx, cy - r)),
                ),
            ),
        )
    }

    /** Counter-clockwise partner used by A16's explicitly authored even-odd ring paths. */
    internal fun circleReverse(cx: Double, cy: Double, r: Double): List<Subpath> {
        val k = KAPPA * 2.0 * r
        return listOf(
            Subpath(
                start = PtPoint(cx, cy - r),
                segments = listOf(
                    Segment.CubicTo(PtPoint(cx - k, cy - r), PtPoint(cx - r, cy - k), PtPoint(cx - r, cy)),
                    Segment.CubicTo(PtPoint(cx - r, cy + k), PtPoint(cx - k, cy + r), PtPoint(cx, cy + r)),
                    Segment.CubicTo(PtPoint(cx + k, cy + r), PtPoint(cx + r, cy + k), PtPoint(cx + r, cy)),
                    Segment.CubicTo(PtPoint(cx + r, cy - k), PtPoint(cx + k, cy - r), PtPoint(cx, cy - r)),
                ),
            ),
        )
    }

    /** One closed subpath of straight segments through [points], in order. */
    internal fun closed(vararg points: PtPoint): SupplyOutline =
        SupplyOutline(listOf(Subpath(points.first(), points.drop(1).map { Segment.LineTo(it) })))
}
