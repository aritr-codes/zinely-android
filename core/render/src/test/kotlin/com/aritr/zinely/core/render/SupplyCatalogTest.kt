package com.aritr.zinely.core.render

import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.PtPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The catalogue and the authoring invariant (SUPPLIES-SPEC §4.1).
 *
 * Three of the four §4.1 rules are mechanically checkable and are checked here for **every** entry, so
 * they hold for every outline in the frozen sixteen:
 * the unit square, the closed-ring/fill-only structure, and strict crossings between straight edges.
 * Curved even-odd correctness remains an authoring rule — which is the reason §4.1 still requires
 * reviewed Kotlin source rather than a data file.
 *
 * The cross-check against `Copy.Supplies` is the one that would catch the expensive mistake: a
 * catalogue key that is not a shipped supply id draws a shape no picker can ever offer, and a typo
 * (`shape.rectangle`) fails silently in exactly that way.
 */
class SupplyCatalogTest {

    private fun SupplyOutline.points(): List<PtPoint> =
        subpaths.flatMap { sub -> listOf(sub.start) + sub.segments.flatMap { it.points() } }

    /** Shoelace over a subpath's flattened polygon (cubics sampled) — zero means the contour encloses nothing. */
    private fun Subpath.polygonArea(): Double {
        val pts = SupplyOutline(listOf(this)).polygons().single()
        return kotlin.math.abs(
            pts.indices.sumOf { i ->
                val a = pts[i]
                val b = pts[(i + 1) % pts.size] // implicit closure
                a.x * b.y - b.x * a.y
            } / 2.0,
        )
    }

    // — §4.1 rule 1: the unit square, now enforced by the type —

    @Test
    fun `given a point outside the unit square, when a subpath is built, then construction fails`() {
        // Rule 1 is a constructor guard, so it covers the outlines nobody has drawn yet. This
        // test is what stops someone "fixing" a future overshoot by loosening the guard silently.
        assertThrows(IllegalArgumentException::class.java) {
            Subpath(PtPoint(0.0, 0.0), listOf(Segment.LineTo(PtPoint(1.5, 0.5)), Segment.LineTo(PtPoint(1.0, 1.0))))
        }
        // A cubic bulges toward its controls, so an out-of-square control point is an out-of-square curve.
        assertThrows(IllegalArgumentException::class.java) {
            Subpath(
                PtPoint(0.0, 0.0),
                listOf(
                    Segment.CubicTo(PtPoint(0.5, -0.2), PtPoint(1.0, 0.0), PtPoint(1.0, 1.0)),
                    Segment.LineTo(PtPoint(0.0, 1.0)),
                ),
            )
        }
    }

    /**
     * The 17 shipped drawings the owner kept as picked in v21-bench.html A39 (ADR-139). They predate H4's
     * margin, so they still fill their square on an axis; every other outline is drawn to H4.
     */
    private val keptAsShipped = setOf(
        "fix.stitch", "fix.grommet", "fix.pushpin",
        "mark.halftone", "mark.registration", "mark.crop", "mark.scan", "mark.perf", "mark.burst",
        "paper.window", "paper.stub", "paper.stamp", "paper.deckle", "paper.hole", "paper.dogear",
        "paper.border", "paper.round",
    )

    @Test
    fun `given every kept shipped outline, when its extent is measured, then it fills the square on an axis`() {
        // A supply authored small inside its square would land smaller than the maker asked for, and
        // the maker would have no way to tell the outline from the box. Only *one* axis, because a wide
        // piece (Perforation, Ticket stub) has air above and below by design.
        for (id in keptAsShipped) {
            val pts = SupplyCatalog.outlineOf(id)!!.points()
            val spanX = pts.maxOf { it.x } - pts.minOf { it.x }
            val spanY = pts.maxOf { it.y } - pts.minOf { it.y }
            assertTrue(spanX > 0.0 && spanY > 0.0, "$id is degenerate on an axis (${spanX}x$spanY)")
            // A16 deliberately freezes two inset marks: Push pin starts below the tile edge (2..24),
            // and Folded corner is an inset scrap (2..22). Their visible margin is content, not an
            // accidentally undersized authoring box; source-level parity pins the exact frozen bounds.
            val expectedSpan = when (id) {
                "fix.pushpin" -> 22.0 / 24.0
                "paper.dogear" -> 20.0 / 24.0
                else -> 1.0
            }
            assertEquals(expectedSpan, maxOf(spanX, spanY), 1e-9, "$id fills neither axis of its square")
        }
    }

    /**
     * Shipped ids whose A39 redraw lands with its family's change (R-A7 (5)); until then they keep the shipped
     * drawing. ⚠ Temporary: empty once the four family changes are in, and then deleted.
     */
    private val redrawPending = setOf(
        "mark.asterisk", "mark.arrow", "mark.hand", "mark.bar",
        "paper.strip", "paper.tag", "paper.underline",
        "shape.rect", "shape.circle", "shape.triangle", "shape.rule", "shape.ring",
    )

    @Test
    fun `given every outline drawn to H4, when its extent is measured, then it sits in the margin at its class size`() {
        // SUPPLIES-SPEC H4 (G3): nothing crosses the 0.5-unit margin, control points included, and each
        // piece is as large as its class allows: square about 21 units across, round 22.5, points and line
        // 23. No class is smaller than about 21, so no piece drawn to H4 spans less than 20 of its 24 units.
        val margin = 0.5 / 24.0
        for ((id, outline) in SupplyCatalog.OUTLINES) {
            if (id in keptAsShipped || id in redrawPending) continue
            val pts = outline.points()
            val minX = pts.minOf { it.x }
            val minY = pts.minOf { it.y }
            val maxX = pts.maxOf { it.x }
            val maxY = pts.maxOf { it.y }
            assertTrue(
                minOf(minX, minY) >= margin - 1e-9 && maxOf(maxX, maxY) <= 1.0 - margin + 1e-9,
                "$id crosses H4's 0.5-unit margin ($minX, $minY)..($maxX, $maxY)",
            )
            val span = maxOf(maxX - minX, maxY - minY) * 24.0
            assertTrue(span >= 20.0 - 1e-9, "$id spans $span of 24 units, under every H4 class size")
        }
    }

    // — §4.1 rule 2: closed rings, never strokes —

    @Test
    fun `given every catalogue outline, when its subpaths are read, then each encloses an area`() {
        for ((id, outline) in SupplyCatalog.OUTLINES) {
            assertTrue(outline.subpaths.isNotEmpty(), "$id has no subpath")
            for (sub in outline.subpaths) {
                // Two, not three: closure back to `start` is implicit, so a triangle is `start` plus
                // two `LineTo`s. One segment plus its closure is a doubled-back line, which fills
                // nothing — a stroke someone hoped would show up, which §4.1 rule 2 forbids.
                assertTrue(
                    sub.segments.size >= 2,
                    "$id has a subpath of ${sub.segments.size} segment(s) — with implicit closure that fills nothing",
                )
                // …and a segment count is not an area: three collinear points pass the check above and
                // still enclose nothing. Measured on the flattened curve, so a dot drawn as two cubics
                // (whose on-curve points are collinear) is measured as the disc it is.
                assertTrue(
                    sub.polygonArea() > 1e-6,
                    "$id has a subpath enclosing no area (${sub.polygonArea()}) — that is a stroke, not a fill",
                )
            }
        }
    }

    // — the id contract —

    @Test
    fun `given the catalogue, when its keys are read, then every one is a shipped supply id`() {
        // Copy.Supplies is the single source of truth for supply ids (ADR-105 S6).
        val shipped = Copy.Supplies.NAMES.keys
        for (id in SupplyCatalog.OUTLINES.keys) {
            assertTrue(id in shipped, "$id is not a supply — the catalogue cannot draw what no picker offers")
        }
    }

    @Test
    fun `given the catalogue, when the four families are consulted, then every family is part-authored`() {
        // Read the family from BY_FAMILY, never from the id prefix: five prefixes carry four families.
        //
        // This assertion is the inverse of the one it replaces. P2 authored exactly Cut shapes and
        // the test pinned that equality — which quietly encoded the wrong theory, that derivability
        // ran along family lines. It does not: the derivable eight came from three families, and
        // Cut shapes is simply the one that happens to be finished. What is worth pinning now is
        // that no family is wholly absent from the drawer, because a family with nothing in it is a
        // heading over an empty shelf on a shipped surface (D-086).
        for (family in Copy.Supplies.BY_FAMILY.keys) {
            val ids = Copy.Supplies.BY_FAMILY.getValue(family).keys
            assertTrue(
                ids.any { it in SupplyCatalog.OUTLINES },
                "the $family family has no authored outline at all — its heading would sit over nothing",
            )
        }
    }

    @Test
    fun `given the frozen expanded cabinet, when catalogue keys are read, then every named supply is authored`() {
        assertEquals(
            Copy.Supplies.NAMES.keys,
            SupplyCatalog.OUTLINES.keys,
            "the frozen expanded vocabulary and the authored catalogue must be the same cabinet",
        )
        for (id in Copy.Supplies.NAMES.keys) assertNotNull(SupplyCatalog.outlineOf(id), "$id has no mark")
    }

    /**
     * The four shipped holed drawings kept as picked whose hole runs the same way as their paper (A39: they
     * "still fill differently under non-zero, which is harmless: the renderer fills even-odd").
     * `SupplyOutlineRingTest` pins why a same-direction ring must stay expressible.
     */
    private val sameDirectionRings = setOf("mark.registration", "paper.window", "paper.border", "paper.round")

    /** Winding number of [polygons] about ([x], [y]), by a ray cast to `+x`. */
    private fun winding(polygons: List<List<PtPoint>>, x: Double, y: Double): Int {
        var w = 0
        for (poly in polygons) for (i in poly.indices) {
            val a = poly[i]
            val b = poly[(i + 1) % poly.size]
            if ((a.y <= y) != (b.y <= y) && a.x + (y - a.y) / (b.y - a.y) * (b.x - a.x) > x) {
                w += if (b.y > a.y) 1 else -1
            }
        }
        return w
    }

    /** How many of an [n]×[n] grid's samples (offset off the 0.01 lattice) fill differently under the two rules. */
    private fun disagreements(outline: SupplyOutline, n: Int): Int {
        val polygons = outline.polygons()
        return (0 until n * n).count { k ->
            val x = (k % n + 0.437) / n
            val y = (k / n + 0.391) / n
            (winding(polygons, x, y) != 0) != outline.isInked(x, y)
        }
    }

    @Test
    fun `given every outline, when sampled, then even-odd and non-zero fill the same points`() {
        // §4.1 rule 4 — "free of winding-dependent self-intersection". Where the two rules agree, overlapping
        // subpaths (a crossbar laid across a ring, two kissing dots) cannot cancel to a hole the drawing did
        // not mean, and a backend that dropped the fill rule still draws the same piece. This replaces a
        // bounding-box overlap heuristic that could not see a concave or curved near-miss.
        for ((id, outline) in SupplyCatalog.OUTLINES) {
            if (id in sameDirectionRings) continue
            val disagree = disagreements(outline, 160)
            assertEquals(0, disagree, "$id: even-odd and non-zero disagree at $disagree of ${160 * 160} samples")
        }
    }

    @Test
    fun `given the same-direction rings, then non-zero really would fill them differently`() {
        for (id in sameDirectionRings) {
            assertTrue(
                disagreements(SupplyCatalog.outlineOf(id)!!, 100) > 0,
                "$id is listed as a same-direction ring but non-zero agrees with even-odd; drop it from the list",
            )
        }
    }

    @Test
    fun `given a straight edged outline, when each contour is checked, then no edge crosses another`() {
        for ((id, outline) in SupplyCatalog.OUTLINES) {
            for ((subpathIndex, subpath) in outline.subpaths.withIndex()) {
                if (subpath.segments.any { it !is Segment.LineTo }) continue
                val vertices = listOf(subpath.start) + subpath.segments.map { it.to }
                val edges = vertices.indices.map { i -> vertices[i] to vertices[(i + 1) % vertices.size] }

                for (first in edges.indices) {
                    for (second in first + 1 until edges.size) {
                        val adjacent = second == first + 1 || (first == 0 && second == edges.lastIndex)
                        if (adjacent) continue
                        assertTrue(
                            !edges[first].intersects(edges[second]),
                            "$id subpath $subpathIndex crosses or overlaps itself at edges $first and $second",
                        )
                    }
                }
            }
        }
    }

    private fun Pair<PtPoint, PtPoint>.intersects(other: Pair<PtPoint, PtPoint>): Boolean {
        fun side(a: PtPoint, b: PtPoint, point: PtPoint): Double =
            (b.x - a.x) * (point.y - a.y) - (b.y - a.y) * (point.x - a.x)

        fun onSegment(a: PtPoint, b: PtPoint, point: PtPoint): Boolean =
            point.x >= minOf(a.x, b.x) - 1e-12 && point.x <= maxOf(a.x, b.x) + 1e-12 &&
                point.y >= minOf(a.y, b.y) - 1e-12 && point.y <= maxOf(a.y, b.y) + 1e-12

        val a = side(first, second, other.first)
        val b = side(first, second, other.second)
        val c = side(other.first, other.second, first)
        val d = side(other.first, other.second, second)
        if (a * b < -1e-12 && c * d < -1e-12) return true
        return (kotlin.math.abs(a) <= 1e-12 && onSegment(first, second, other.first)) ||
            (kotlin.math.abs(b) <= 1e-12 && onSegment(first, second, other.second)) ||
            (kotlin.math.abs(c) <= 1e-12 && onSegment(other.first, other.second, first)) ||
            (kotlin.math.abs(d) <= 1e-12 && onSegment(other.first, other.second, second))
    }

    // — the two hand-cut frames (v21-bench.html A32) —

    /** Unit-square probes for one frame: points on its band, and points in its hole. */
    private fun assertIsOneFrame(id: String, band: List<Pair<Double, Double>>, hole: List<Pair<Double, Double>>) {
        val outline = SupplyCatalog.outlineOf(id)!!
        assertEquals(2, outline.subpaths.size, "$id is one outer contour and one hole")
        assertTrue(
            outline.subpaths.all { sub -> sub.segments.all { it is Segment.LineTo } },
            "$id is cut with scissors: straight segments only",
        )
        // Closed and inside the unit square are the type's own guards; spanning the box on BOTH axes is
        // this piece's: a frame that stopped short of an edge would leave a gap around what it frames.
        val pts = outline.points()
        assertEquals(0.0, pts.minOf { it.x }, 1e-12, "$id left edge")
        assertEquals(1.0, pts.maxOf { it.x }, 1e-12, "$id right edge")
        assertEquals(0.0, pts.minOf { it.y }, 1e-12, "$id top edge")
        assertEquals(1.0, pts.maxOf { it.y }, 1e-12, "$id bottom edge")

        // Even-odd with nothing overlapping: the hole lies wholly inside the outer contour. Every hole
        // vertex is inside it and no hole edge touches an outer edge, so the two never cross.
        val (outer, inner) = outline.subpaths.map { sub -> listOf(sub.start) + sub.segments.map { it.to } }
        val outerOnly = SupplyOutline(listOf(outline.subpaths[0]))
        for (p in inner) assertTrue(outerOnly.isInked(p.x, p.y), "$id: hole vertex $p is outside the paper")
        fun edges(v: List<PtPoint>) = v.indices.map { v[it] to v[(it + 1) % v.size] }
        for (a in edges(outer)) for (b in edges(inner)) {
            assertTrue(!a.intersects(b), "$id: the hole's edge $b crosses the paper's edge $a")
        }

        for ((u, v) in band) assertTrue(outline.isInked(u, v), "$id must be paper at ($u, $v)")
        for ((u, v) in hole) assertTrue(!outline.isInked(u, v), "$id must be open at ($u, $v)")
    }

    @Test
    fun `given the snipped border, when it is filled, then a thin band surrounds one open middle`() {
        assertIsOneFrame(
            "paper.border",
            // The middle of each side's band, and two corners on the diagonal.
            band = listOf(0.05 to 0.5, 0.95 to 0.5, 0.5 to 0.05, 0.5 to 0.95, 0.06 to 0.06, 0.94 to 0.94),
            // The centre, and just inside the band on each side: the hole is nearly the whole box.
            hole = listOf(0.5 to 0.5, 0.15 to 0.5, 0.85 to 0.5, 0.5 to 0.15, 0.5 to 0.85),
        )
    }

    @Test
    fun `given the round window, when it is filled, then the corners are paper and the middle is open`() {
        assertIsOneFrame(
            "paper.round",
            // The four corners are what make it a square of paper and not a ring, plus two sides' bands.
            band = listOf(0.08 to 0.08, 0.92 to 0.08, 0.92 to 0.92, 0.08 to 0.92, 0.05 to 0.5, 0.5 to 0.95),
            hole = listOf(0.5 to 0.5, 0.25 to 0.5, 0.75 to 0.5, 0.5 to 0.25, 0.5 to 0.75),
        )
    }

    @Test
    fun `given an id that is not a supply at all, when an outline is asked for, then it is null`() {
        assertNull(SupplyCatalog.outlineOf("shape.rectangle")) // the plausible typo
        assertNull(SupplyCatalog.outlineOf(""))
    }

    // — the shapes themselves —

    @Test
    fun `given the rule, when compared with the rectangle, then it is a centred band and not the square`() {
        val rule = SupplyCatalog.outlineOf("shape.rule")!!.points()
        val rect = SupplyCatalog.outlineOf("shape.rect")!!.points()
        assertTrue(rule != rect, "a rule that equals a rectangle differs only by default size")
        // Vertically centred: the band's top and bottom are equidistant from the square's middle.
        val top = rule.minOf { it.y }
        val bottom = rule.maxOf { it.y }
        assertEquals(0.5, (top + bottom) / 2.0, 1e-9)
        assertTrue(top > 0.0 && bottom < 1.0, "the rule needs air above and below")
    }

    @Test
    fun `given the circle, when its four cubics are read, then they meet at the cardinal points`() {
        val sub = SupplyCatalog.outlineOf("shape.circle")!!.subpaths.single()
        assertEquals(4, sub.segments.size)
        assertTrue(sub.segments.all { it is Segment.CubicTo }, "a circle of lines is a polygon")
        assertEquals(
            listOf(PtPoint(1.0, 0.5), PtPoint(0.5, 1.0), PtPoint(0.0, 0.5), PtPoint(0.5, 0.0)),
            sub.segments.map { it.to },
        )
        assertEquals(sub.start, sub.segments.last().to, "the last cubic returns to the start")
    }

    // — the command —

    @Test
    fun `given a shape command, when its clip is read, then it is null, whatever the placement`() {
        // A points-valued clip would shrink a unit-square outline to a sliver; the field is not a
        // constructor parameter, so this is a regression guard on someone re-adding one.
        val cmd = DrawShape(
            outline = SupplyCatalog.outlineOf("shape.rect")!!,
            ink = ColorRgba(0, 0, 0),
            localToPage = AffineTransform2D.translate(10.0, 10.0).times(AffineTransform2D.scale(40.0, 25.0)),
        )
        assertNull(cmd.localClip)
    }

    // — the type's own guards —

    @Test
    fun `given an empty outline or a subpath with no segments, when built, then construction fails`() {
        assertThrows(IllegalArgumentException::class.java) { SupplyOutline(emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { Subpath(PtPoint(0.0, 0.0), emptyList()) }
    }
}
