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

    /** Shoelace over a subpath's on-curve points — zero means the contour encloses nothing. */
    private fun Subpath.polygonArea(): Double {
        val pts = listOf(start) + segments.map { it.to }
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

    @Test
    fun `given every catalogue outline, when its extent is measured, then it fills the square on an axis`() {
        // A supply authored small inside its square would land smaller than the maker asked for, and
        // the maker would have no way to tell the outline from the box. So both axes must have real
        // extent and at least one must run edge to edge.
        //
        // Only *one* axis, because `shape.rule` is deliberately a centred band with air above and
        // below (see SupplyCatalog.RULE) — the margin is the whole difference between a rule and a
        // rectangle. Requiring both axes would forbid the one supply that has a reason to say no.
        for ((id, outline) in SupplyCatalog.OUTLINES) {
            val pts = outline.points()
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
            assertEquals(
                expectedSpan,
                maxOf(spanX, spanY),
                1e-9,
                "$id fills neither axis of its square — it would render smaller than its own box",
            )
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
                // still enclose nothing. Measured on the on-curve points, so a curved contour is
                // under-counted, never over-counted — the assertion stays conservative.
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
    fun `given the frozen expanded cabinet, when catalogue keys are read, then all thirty two are authored`() {
        assertEquals(
            Copy.Supplies.NAMES.keys,
            SupplyCatalog.OUTLINES.keys,
            "the frozen expanded vocabulary and the authored catalogue must be the same cabinet",
        )
        for (id in Copy.Supplies.NAMES.keys) assertNotNull(SupplyCatalog.outlineOf(id), "$id has no mark")
    }

    @Test
    fun `given the multi-subpath outlines, when their subpaths are compared, then none overlaps another`() {
        // §4.1 rule 4 — "free of winding-dependent self-intersection" — is called an authoring rule
        // no assertion can see, and in general that is true. But the specific way it breaks here IS
        // visible: under an even-odd fill two overlapping subpaths cancel to a HOLE rather than
        // uniting, so a registration mark drawn as a full plus laid across a ring would punch a gap
        // exactly where the mark is densest, and two kissing halftone dots would notch each other.
        //
        // Bounding boxes, not exact geometry: a box test can only be conservative for convex parts
        // like these (every subpath here is a rectangle, a triangle or a circle), so a pass is a real
        // pass. It cannot see a concave near-miss, which is why rule 4 still needs a reviewer.
        for ((id, outline) in SupplyCatalog.OUTLINES) {
            if (outline.subpaths.size < 2) continue
            val boxes = outline.subpaths.map { sub ->
                val pts = listOf(sub.start) + sub.segments.flatMap { it.points() }
                listOf(pts.minOf { it.x }, pts.minOf { it.y }, pts.maxOf { it.x }, pts.maxOf { it.y })
            }
            for (i in boxes.indices) {
                for (j in i + 1 until boxes.size) {
                    val (aMinX, aMinY, aMaxX, aMaxY) = boxes[i]
                    val b = boxes[j]
                    // Nesting is the one legitimate overlap: a ring, a window and a photo corner are
                    // *defined* by an inner subpath sitting wholly inside an outer one. That is the
                    // hole even-odd is for. What must never happen is a partial crossing.
                    //
                    // ⚠ **The two branches are not equally strong, and the weaker one is named here
                    // rather than papered over.** Disjoint bounding boxes really do imply disjoint
                    // shapes, so that branch is conservative and a pass is a real pass. *Nested*
                    // bounding boxes do **not** imply a nested shape — `fix.corner` is two triangles,
                    // and an inner triangle poking through the outer hypotenuse would still have a
                    // contained bbox and would still pass here. Rule 4 keeps its reviewer; what this
                    // test buys is that the mistake anyone actually makes — a crossbar laid across a
                    // ring — cannot reach the drawer.
                    val nested = (aMinX <= b[0] && aMinY <= b[1] && aMaxX >= b[2] && aMaxY >= b[3]) ||
                        (b[0] <= aMinX && b[1] <= aMinY && b[2] >= aMaxX && b[3] >= aMaxY)
                    val disjoint = aMaxX <= b[0] || b[2] <= aMinX || aMaxY <= b[1] || b[3] <= aMinY
                    assertTrue(
                        nested || disjoint,
                        "$id: subpaths $i and $j partially overlap — even-odd will cancel them to a " +
                            "hole where the mark should be solid",
                    )
                }
            }
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
