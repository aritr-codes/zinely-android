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

    /**
     * An outline transcribed from a frozen sheet's SVG path (v21-bench.html A39, ADR-139), in the sheet's own
     * 24-unit numbers: `m`, `l`, `c` and `z` are the absolute `M`, `L`, `C` and `Z`, each divided by 24 here
     * so the source reads exactly like the frozen row it came from. `BenchArtSheetParityTest` holds every
     * transcription to its row.
     *
     * Inline so each call is a flat run of statements (no lambda class per outline, no deep call chain).
     */
    internal inline fun path(draw: PathBuilder.() -> Unit): SupplyOutline = PathBuilder().apply(draw).build()

    /** The pen behind [path]. A subpath ends at `z()` or at the next `m()`; closure is implicit (see [Subpath]). */
    internal class PathBuilder {
        private val subpaths = mutableListOf<Subpath>()
        private val segments = mutableListOf<Segment>()
        private var start: PtPoint? = null

        private fun p(x: Double, y: Double) = PtPoint(x / 24.0, y / 24.0)

        fun m(x: Double, y: Double) {
            z()
            start = p(x, y)
        }

        fun l(x: Double, y: Double) {
            segments += Segment.LineTo(p(x, y))
        }

        fun c(x1: Double, y1: Double, x2: Double, y2: Double, x: Double, y: Double) {
            segments += Segment.CubicTo(p(x1, y1), p(x2, y2), p(x, y))
        }

        fun z() {
            val from = start ?: return
            subpaths += Subpath(from, segments.toList())
            segments.clear()
            start = null
        }

        fun build(): SupplyOutline {
            z()
            return SupplyOutline(subpaths.toList())
        }
    }
}
