package com.aritr.zinely.core.render

import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.PtPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The catalogue and the authoring invariant (SUPPLIES-SPEC §4.1).
 *
 * Two of the four §4.1 rules are mechanically checkable and are checked here for **every** entry, so
 * they hold for the twelve outlines still owed to a designer as well as for the four authored now:
 * the unit square, and the closed-ring/fill-only structure. Even-odd correctness and the absence of
 * winding-dependent self-intersection are authoring rules no assertion can see — which is the reason
 * §4.1 requires reviewed Kotlin source rather than a data file.
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
        // Rule 1 is a constructor guard, so it covers the twelve outlines nobody has drawn yet. This
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
            assertEquals(
                1.0,
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
    fun `given the catalogue, when the four families are consulted, then only Cut shapes is authored`() {
        // Read the family from BY_FAMILY, never from the id prefix: five prefixes carry four families.
        val cutShapes = Copy.Supplies.BY_FAMILY.getValue(Copy.Supplies.CUT_SHAPES).keys
        assertEquals(cutShapes, SupplyCatalog.OUTLINES.keys, "P2 authors the Cut shapes family, exactly")
    }

    @Test
    fun `given the twelve unauthored supplies, when an outline is asked for, then it is null`() {
        // The incompleteness is explicit: an unauthored supply draws nothing and never guesses.
        val unauthored = Copy.Supplies.NAMES.keys - SupplyCatalog.OUTLINES.keys
        assertEquals(12, unauthored.size, "16 supplies ship; 4 are authored")
        for (id in unauthored) assertNull(SupplyCatalog.outlineOf(id), "$id must not resolve to a guess")
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
