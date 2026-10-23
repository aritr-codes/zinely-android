package com.aritr.zinely.core.render

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ART-2, Never #19 (ADR-136 decision 3): a redraw may not grow a piece beyond the ink box it shipped
 * with, because a saved zine places the piece by its box and a bigger drawing would overlap neighbours
 * the author never saw it touch. The literals are the 34 shipped ink boxes (min/max of every flattened
 * outline point, unit square); the current box of each must lie inside its old one.
 */
class SupplyRedrawBoundsTest {
    private data class Box(val x0: Double, val y0: Double, val x1: Double, val y1: Double)

    private val shipped: Map<String, Box> = mapOf(
        "tape.torn" to Box(0.0, 0.0, 1.0, 1.0),
        "fix.corner" to Box(0.0, 0.0, 1.0, 1.0),
        "fix.staple" to Box(0.0, 0.3, 1.0, 1.0),
        "fix.clip" to Box(0.06, 0.0, 0.88, 1.0),
        "tape.masking" to Box(0.0, 0.125, 1.0, 0.875),
        "fix.stitch" to Box(0.20833333333333334, 0.0, 0.7916666666666666, 1.0),
        "fix.grommet" to Box(0.0, 0.0, 1.0, 1.0),
        "fix.pushpin" to Box(0.20833333333333334, 0.08333333333333333, 0.7916666666666666, 1.0),
        "mark.asterisk" to Box(0.0, 0.0245, 1.0, 0.9755),
        "mark.arrow" to Box(0.0, 0.06, 1.0, 0.94),
        "mark.halftone" to Box(0.0, 0.0, 1.0, 1.0),
        "mark.registration" to Box(0.0, 0.0, 1.0, 1.0),
        "mark.hand" to Box(0.0, 0.20833333333333334, 1.0, 0.75),
        "mark.crop" to Box(0.0, 0.0, 1.0, 1.0),
        "mark.bar" to Box(0.0, 0.16666666666666666, 1.0, 0.8333333333333334),
        "mark.scan" to Box(0.0, 0.0, 1.0, 1.0),
        "mark.perf" to Box(0.0, 0.4166666666666667, 1.0, 0.5833333333333334),
        "mark.burst" to Box(0.0, 0.0, 1.0, 1.0),
        "paper.strip" to Box(0.0, 0.16, 1.0, 0.84),
        "paper.window" to Box(0.0, 0.0, 1.0, 1.0),
        "paper.tag" to Box(0.0, 0.0, 1.0, 1.0),
        "paper.underline" to Box(0.0, 0.41, 1.0, 0.68),
        "paper.stub" to Box(0.0, 0.125, 1.0, 0.875),
        "paper.stamp" to Box(0.0, 0.0, 1.0, 1.0),
        "paper.deckle" to Box(0.0, 0.0, 1.0, 1.0),
        "paper.hole" to Box(0.0, 0.0, 1.0, 1.0),
        "paper.dogear" to Box(0.08333333333333333, 0.08333333333333333, 0.9166666666666666, 0.9166666666666666),
        "paper.border" to Box(0.0, 0.0, 1.0, 1.0),
        "paper.round" to Box(0.0, 0.0, 1.0, 1.0),
        "shape.rect" to Box(0.0, 0.0, 1.0, 1.0),
        "shape.circle" to Box(0.0, 0.0, 1.0, 1.0),
        "shape.triangle" to Box(0.0, 0.0, 1.0, 1.0),
        "shape.rule" to Box(0.0, 0.4, 1.0, 0.6),
        "shape.ring" to Box(0.0, 0.0, 1.0, 1.0),
    )

    private fun inkBox(o: SupplyOutline): Box {
        val pts = o.polygons().flatten()
        return Box(pts.minOf { it.x }, pts.minOf { it.y }, pts.maxOf { it.x }, pts.maxOf { it.y })
    }

    @Test
    fun `given the 34 shipped boxes, when listed, then they are exactly the baseline ids`() {
        val baseline = javaClass.getResource("/supply-id-baseline.txt")!!.readText().lines().filter { it.isNotBlank() }
        assertTrue(shipped.keys == baseline.toSet(), "literal boxes and baseline ids differ")
    }

    @Test
    fun `given each baseline id, when its current ink box is measured, then it lies inside the shipped box`() {
        val eps = 1e-9
        for ((id, old) in shipped) {
            val now = inkBox(checkNotNull(SupplyCatalog.OUTLINES[id]) { "$id has no outline" })
            assertTrue(
                now.x0 >= old.x0 - eps && now.y0 >= old.y0 - eps && now.x1 <= old.x1 + eps && now.y1 <= old.y1 + eps,
                "$id grew beyond its shipped ink box: now $now, shipped $old (Never #19)",
            )
        }
    }
}
