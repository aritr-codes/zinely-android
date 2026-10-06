package com.aritr.zinely.core.render

import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [SupplyInk.distancePt] — the one function `:core:editor` takes from this module (ADR-124 §7).
 *
 * Probes are named unit-square points, as in [SupplyOutlineFillTest], and for the same reason: a
 * sentence like *"the centre of the ring is not ink"* cannot be blessed by accident.
 */
class SupplyInkTest {

    /** One in-scope piece: a point on its ink, and points in its hole or bare paper. */
    private data class Probe(val id: String, val ink: List<Pair<Double, Double>>, val bare: List<Pair<Double, Double>>)

    private val six = listOf(
        // The left band is ink only because the HOLE's implicit closing edge (u = 0.14) is counted.
        Probe("paper.window", ink = listOf(0.05 to 0.5, 0.5 to 0.05), bare = listOf(0.5 to 0.5)),
        Probe("paper.hole", ink = listOf(0.125 to 0.5), bare = listOf(0.5 to 0.5)),
        Probe("shape.ring", ink = listOf(0.1 to 0.5, 0.5 to 0.9), bare = listOf(0.5 to 0.5)),
        Probe("fix.grommet", ink = listOf(0.1 to 0.5, 0.5 to 0.9), bare = listOf(0.5 to 0.5)),
        // (0.06, 0.6) is level with the pocket, so it is ink only if the pocket's closing edge
        // (u = 0.16) is counted; without it the ray crosses twice and the band reads as bare.
        Probe("fix.corner", ink = listOf(0.06 to 0.6, 0.06 to 0.9, 0.5 to 0.95), bare = listOf(0.25 to 0.7, 0.9 to 0.2)),
        Probe("mark.registration", ink = listOf(0.05 to 0.5, 0.5 to 0.28), bare = listOf(0.5 to 0.5, 0.15 to 0.15)),
    )

    /** Distance at unit point ([u], [v]) of an un-rotated, un-flipped [w]×[h] piece. */
    private fun at(
        id: String,
        u: Double,
        v: Double,
        w: Double = 100.0,
        h: Double = 100.0,
        mirrored: Boolean = false,
        flippedVertically: Boolean = false,
    ): Double? = SupplyInk.distancePt(id, (u - 0.5) * w, (v - 0.5) * h, w, h, mirrored, flippedVertically)

    @Test
    fun `given each of the six, when its ink is probed, then the distance is zero, and positive in its hole`() {
        for (p in six) {
            for ((u, v) in p.ink) assertEquals(0.0, at(p.id, u, v), "${p.id} must be ink at ($u, $v)")
            for ((u, v) in p.bare) assertTrue(at(p.id, u, v)!! > 0.0, "${p.id} must be bare at ($u, $v)")
        }
    }

    @Test
    fun `given the scope, then it is exactly the six, each has an outline, and everything else is null`() {
        assertEquals(
            setOf("paper.window", "paper.hole", "shape.ring", "fix.grommet", "fix.corner", "mark.registration"),
            SupplyInk.IN_SCOPE,
        )
        assertEquals(six.map { it.id }.toSet(), SupplyInk.IN_SCOPE, "every in-scope piece is probed above")
        for (id in SupplyInk.IN_SCOPE) assertNotNull(SupplyCatalog.outlineOf(id), "$id has no outline")

        // Crop marks are out by ruling; the rest are out by not being named. Their centres are bare
        // paper for several of them, which is the case a rule that guessed from the outline would catch.
        for (id in SupplyCatalog.OUTLINES.keys - SupplyInk.IN_SCOPE) {
            assertNull(at(id, 0.5, 0.5), "$id is out of scope and must be hit by its box")
        }
        assertNull(at("mark.crop", 0.5, 0.5))
        assertNull(at("zzz.unknown", 0.5, 0.5))
    }

    @Test
    fun `given a degenerate box, then the piece falls back to its box`() {
        for (bad in listOf(0.0, -10.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertNull(SupplyInk.distancePt("paper.window", 0.0, 0.0, bad, 100.0, false, false), "width $bad")
            assertNull(SupplyInk.distancePt("paper.window", 0.0, 0.0, 100.0, bad, false, false), "height $bad")
        }
    }

    @Test
    fun `given a point on the outline itself, then the answer is deterministic and it counts as ink`() {
        // A vertex, a horizontal edge, the box edge and the four corners of the unit square: every one is
        // on the window's boundary, so whichever side the ray cast puts it, the distance is zero.
        val onTheWindow = listOf(
            0.14 to 0.14, 0.86 to 0.86, // vertices of the hole
            0.5 to 0.14, 0.5 to 0.86, // horizontal edges of the hole
            0.5 to 0.0, 0.5 to 1.0, 0.0 to 0.5, 1.0 to 0.5, // the box edge
            0.0 to 0.0, 1.0 to 0.0, 1.0 to 1.0, 0.0 to 1.0, // the box corners
        )
        for ((u, v) in onTheWindow) {
            assertEquals(0.0, at("paper.window", u, v)!!, 1e-9, "the window's boundary at ($u, $v)")
        }
        // A ring leaves its box's corners bare: (√2/2 − 1/2) of a 100 pt box.
        for ((u, v) in listOf(0.0 to 0.0, 1.0 to 0.0, 1.0 to 1.0, 0.0 to 1.0)) {
            assertEquals(20.71, at("shape.ring", u, v)!!, 0.01, "the ring's box corner ($u, $v)")
        }
    }

    @Test
    fun `given a point beside a closing edge, then the distance is measured to that edge`() {
        // The window's left side (u = 0) and the corner's left side are both implicit closing edges. A
        // distance loop that skipped them would measure to the nearest authored edge instead.
        assertEquals(5.0, at("paper.window", -0.05, 0.5)!!, 1e-9, "5 pt left of the window")
        assertEquals(5.0, at("fix.corner", -0.05, 0.6)!!, 1e-9, "5 pt left of the corner")
    }

    @Test
    fun `given a ring stretched to a 2 to 1 box, when a point is 3 pt off the ink, then the distance is 3 pt`() {
        val w = 200.0
        val h = 100.0
        fun local(x: Double, y: Double) = SupplyInk.distancePt("shape.ring", x, y, w, h, false, false)!!
        assertEquals(3.0, local(-w / 2 - 3.0, 0.0), 0.01, "left of the ring")
        assertEquals(3.0, local(w / 2 + 3.0, 0.0), 0.01, "right of the ring")
        assertEquals(3.0, local(0.0, -h / 2 - 3.0), 0.01, "above the ring")
        assertEquals(3.0, local(0.0, h / 2 + 3.0), 0.01, "below the ring")
        // …and inside the hole, 3 pt short of its edge on each axis (the hole is 7/12 of each side).
        assertEquals(3.0, local(w * 7.0 / 24.0 - 3.0, 0.0), 0.01, "inside the hole, along x")
        assertEquals(3.0, local(0.0, h * 7.0 / 24.0 - 3.0), 0.01, "inside the hole, along y")
    }

    @Test
    fun `given a thin registration arm, when a point moves off it, then the distance has no gaps`() {
        // The case eight samples on a circle fail (ADR-124, difference 1): the arm is 0.07 of the box,
        // about 2.4 pt at this size, thinner than the gaps between samples. Distance has none.
        val size = 34.0
        val tolerance = 8.0
        val armTop = -0.035 * size
        val x = (0.05 - 0.5) * size // on the left arm, well clear of the ring and the top arm
        var off = 0.0
        while (off <= 9.0) {
            val d = SupplyInk.distancePt("mark.registration", x, armTop - off, size, size, false, false)!!
            assertEquals(off, d, 1e-9, "$off pt above the left arm")
            assertEquals(off <= tolerance, d <= tolerance + 1e-9, "$off pt above the arm, against an 8 pt tolerance")
            off += 0.125
        }
    }

    @Test
    fun `given a mirrored or flipped corner, then its ink moves and its box does not`() {
        // (0.06, 0.9) is ink; its mirror image (0.94, 0.9) and its flip (0.06, 0.1)… the flip is the left
        // band and stays ink, so the flip is probed with the bottom band instead.
        assertEquals(0.0, at("fix.corner", 0.94, 0.9, mirrored = true))
        assertTrue(at("fix.corner", 0.94, 0.9)!! > 0.0)
        assertTrue(at("fix.corner", 0.06, 0.9, mirrored = true)!! > 0.0)

        assertEquals(0.0, at("fix.corner", 0.5, 0.05, flippedVertically = true))
        assertTrue(at("fix.corner", 0.5, 0.05)!! > 0.0)
        assertTrue(at("fix.corner", 0.5, 0.95, flippedVertically = true)!! > 0.0)
    }

    @Test
    fun `given the transform the renderer emits, when a drawn point is mapped back, then ink is ink`() {
        // The hit hole is the drawn hole: unit points go OUT through the fold `SceneRenderer` actually
        // emits, come back through the inverse this module documents (subtract the centre, un-rotate, then
        // `distancePt`), and must agree with the fill the replayer applies to that same outline.
        // The grid is offset so no sample sits on an edge (the corner's hypotenuse is u = v).
        val n = 25
        for (id in SupplyInk.IN_SCOPE) {
            for (mirrored in listOf(false, true)) for (flipped in listOf(false, true)) {
                val t = Transform(xPt = 37.0, yPt = 61.0, widthPt = 130.0, heightPt = 70.0, rotationDegrees = 33.0)
                val piece = DecorElement("p", t, 0, id, ColorRgba(0, 0, 0), mirrored, flipped)
                val shape = SceneRenderer
                    .buildScene(Page(0, PageRole.INTERIOR, elements = listOf(piece)), PtSize(400.0, 400.0), DocumentDefaults())
                    .commands.filterIsInstance<DrawShape>().single()
                val unRotate = AffineTransform2D.rotateDeg(-t.rotationDegrees)
                for (iy in 0 until n) for (ix in 0 until n) {
                    val u = (ix + 0.37) / n
                    val v = (iy + 0.61) / n
                    val page = shape.localToPage.map(PtPoint(u, v))
                    val local = unRotate.map(
                        PtPoint(page.x - (t.xPt + t.widthPt / 2.0), page.y - (t.yPt + t.heightPt / 2.0)),
                    )
                    val d = SupplyInk.distancePt(id, local.x, local.y, t.widthPt, t.heightPt, mirrored, flipped)!!
                    assertEquals(
                        shape.outline.isInked(u, v),
                        d == 0.0,
                        "$id mirrored=$mirrored flipped=$flipped at unit ($u, $v): distance $d",
                    )
                }
            }
        }
    }
}
