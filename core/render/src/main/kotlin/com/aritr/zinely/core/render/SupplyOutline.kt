package com.aritr.zinely.core.render

import com.aritr.zinely.core.model.PtPoint

/**
 * The geometry of one supply — an authored mark from the Zinely cabinet, drawn once as code and
 * reused at every size (SUPPLIES-SPEC §3.3, §4.1).
 *
 * ### The representation, and why this one
 *
 * A list of closed subpaths, each a start point plus straight and cubic segments. Not a rect/circle
 * union (nine of the sixteen are torn or hand-drawn and have no algebra), not an SVG path string (a
 * string is a parser and a parser is a failure mode), not `android.graphics.Path` (this module is
 * pure Kotlin and the outline must be readable in a JVM unit test on all four surfaces' behalf).
 * Cubics only: every quadratic is a cubic, and one segment kind fewer is one fold fewer everywhere.
 *
 * ### The authoring invariant (SUPPLIES-SPEC §4.1 — binding)
 *
 * Every outline in [SupplyCatalog] is:
 *
 *  1. **Drawn in the unit square** — every coordinate, control points included, lies in `0.0..1.0`.
 *     The element's `Transform` supplies the real size through the `scale(w, h)` term of the fold
 *     (§3.4.1); an outline that leaves the square would render outside its own selection box.
 *  2. **Fill-only** — a supply that reads as an outline is authored as a closed ring, never stroked.
 *     There is no width, cap, join or miter here to get wrong, and none is coming.
 *  3. **Even-odd** — holes work without authoring winding direction correctly, which matters because
 *     these are drawn by hand rather than generated. A hole is a second subpath, and its direction is
 *     deliberately free: `SupplyOutlineRingTest` pins that a **same**-direction ring is expressible,
 *     because that is the only geometry whose rendering differs between even-odd and non-zero and so
 *     the only one that can catch a backend that dropped the fill rule.
 *  4. **Free of winding-dependent self-intersection** — a shape whose fill differs between even-odd
 *     and non-zero is not authored; it is a bug that would render differently the day any backend
 *     disagreed about the rule.
 *
 * Rule 1 is enforced **by construction** below, so it is total: it holds for the twelve outlines still
 * owed to a designer, and for any outline built outside the catalogue, without anyone remembering to
 * add a test. Rule 2 is checked structurally by `SupplyCatalogTest`. Rules 3 and 4 are **authoring
 * rules no assertion can see** — which is exactly why §4.1 requires these to ship as reviewed Kotlin
 * source rather than as a data file or an import.
 *
 * ### Closure is implicit
 *
 * A subpath is always closed. Filling an open path fills its implicit closure anyway, so a `closed`
 * flag would be a field you could set to `false` and change nothing — a lie with a setter.
 */
public data class SupplyOutline(val subpaths: List<Subpath>) {
    init {
        require(subpaths.isNotEmpty()) { "a supply outline needs at least one subpath" }
    }
}

/** One closed contour: where the pen lands, then where it goes. Closure back to [start] is implicit. */
public data class Subpath(val start: PtPoint, val segments: List<Segment>) {
    init {
        require(segments.isNotEmpty()) { "a subpath needs at least one segment; a point encloses no area" }
        // Rule 1, enforced rather than asserted. Control points count: a cubic bulges toward them, so a
        // control point outside the square is a curve outside the square.
        for (p in listOf(start) + segments.flatMap { it.points() }) {
            require(p.x in 0.0..1.0 && p.y in 0.0..1.0) {
                "a supply outline is authored in the unit square (SUPPLIES-SPEC §4.1); $p is outside it"
            }
        }
    }
}

/** A step of a [Subpath]. Cubics only — every quadratic is expressible as one. */
public sealed interface Segment {
    /** The point this segment ends at. */
    public val to: PtPoint

    /** A straight line to [to]. */
    public data class LineTo(override val to: PtPoint) : Segment

    /** A cubic Bézier to [to] with control points [c1] and [c2]. */
    public data class CubicTo(val c1: PtPoint, val c2: PtPoint, override val to: PtPoint) : Segment

    /** Every point this segment names, control points included — see [Subpath]'s unit-square guard. */
    public fun points(): List<PtPoint> = when (this) {
        is LineTo -> listOf(to)
        is CubicTo -> listOf(c1, c2, to)
    }
}
