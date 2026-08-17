package com.aritr.zinely.core.render

import com.aritr.zinely.core.model.PtPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Can the representation express a **hole**, wound the way the fill rule is actually tested?
 *
 * This is a design test, not a catalogue test. None of P2's four supplies has a hole — but four of the
 * twelve still owed (`paper.window`, `fix.corner`, `mark.registration`, `mark.asterisk` in some
 * drawings) do, and even-odd exists in the design precisely so a hand-drawn hole works without the
 * author having to reason about winding direction (SUPPLIES-SPEC §3.3, §4.1).
 *
 * The winding matters to the *test*, not to the drawing. A ring whose inner and outer contours run in
 * **opposite** directions has a hole under even-odd **and** under non-zero — so it passes whichever
 * rule the backend actually used and proves nothing. A ring wound the **same** direction is the only
 * shape whose rendering differs between the two rules, which makes it the one geometry that can catch
 * a dropped `Path.fillType`. If the type could not express it, that would be a design defect rather
 * than a testing inconvenience — so it is pinned here, before P3 needs it.
 */
class SupplyOutlineRingTest {

    /** Shoelace signed area over a closed polygon of [Segment.LineTo]s. Sign = winding direction. */
    private fun Subpath.signedArea(): Double {
        val pts = listOf(start) + segments.map { it.to }
        return pts.indices.sumOf { i ->
            val a = pts[i]
            val b = pts[(i + 1) % pts.size] // implicit closure
            a.x * b.y - b.x * a.y
        } / 2.0
    }

    /** A square from its top-left corner, wound clockwise when [size] > 0. */
    private fun square(x: Double, y: Double, size: Double) = Subpath(
        start = PtPoint(x, y),
        segments = listOf(
            Segment.LineTo(PtPoint(x + size, y)),
            Segment.LineTo(PtPoint(x + size, y + size)),
            Segment.LineTo(PtPoint(x, y + size)),
        ),
    )

    @Test
    fun `given two same-wound squares, when built as one outline, then the ring is expressible`() {
        val ring = SupplyOutline(listOf(square(0.0, 0.0, 1.0), square(0.25, 0.25, 0.5)))

        assertEquals(2, ring.subpaths.size)
        val outer = ring.subpaths[0].signedArea()
        val inner = ring.subpaths[1].signedArea()
        assertTrue(outer > 0.0 && inner > 0.0, "both contours must wind the same way, got $outer / $inner")
        assertTrue(
            kotlin.math.abs(inner) < kotlin.math.abs(outer),
            "the hole must sit inside the wall for the fill rule to be the only thing under test",
        )
    }

    @Test
    fun `given the same ring, when the fill rules are compared, then only even-odd leaves a hole`() {
        // The property that makes this geometry the right probe, asserted on the geometry itself so it
        // survives independently of any backend: the centre is inside BOTH contours, so it is crossed
        // twice (even ⇒ empty under even-odd) and wound twice the same way (⇒ filled under non-zero).
        val ring = SupplyOutline(listOf(square(0.0, 0.0, 1.0), square(0.25, 0.25, 0.5)))
        val centre = PtPoint(0.5, 0.5)

        val crossings = ring.subpaths.count { sub ->
            val pts = listOf(sub.start) + sub.segments.map { it.to }
            centre.x > pts.minOf { it.x } && centre.x < pts.maxOf { it.x } &&
                centre.y > pts.minOf { it.y } && centre.y < pts.maxOf { it.y }
        }
        assertEquals(2, crossings, "the centre must be enclosed by both contours or the probe is blunt")
        assertEquals(0, crossings % 2, "even ⇒ even-odd leaves it empty")
        val windingSum = ring.subpaths.map { it.signedArea() }.count { it > 0.0 }
        assertEquals(2, windingSum, "same-direction ⇒ non-zero fills it; that is the difference under test")
    }
}
