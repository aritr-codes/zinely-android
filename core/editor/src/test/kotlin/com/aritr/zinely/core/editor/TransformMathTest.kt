package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.Transform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Transform baking (ADR-029 §5.2): fold an accumulated gesture delta {pan, zoom, rotation} into a
 * committed [Transform]. Centre-anchored (pinch / a11y ScaleBy): uniform scale + rotation about the
 * element centre, pan in page space. Pure.
 */
class TransformMathTest {

    private val tol = 1e-9
    private fun assertTransform(expected: Transform, actual: Transform) {
        assertEquals(expected.xPt, actual.xPt, tol, "x")
        assertEquals(expected.yPt, actual.yPt, tol, "y")
        assertEquals(expected.widthPt, actual.widthPt, tol, "w")
        assertEquals(expected.heightPt, actual.heightPt, tol, "h")
        assertEquals(expected.rotationDegrees, actual.rotationDegrees, tol, "rot")
    }

    private val base = Transform(xPt = 100.0, yPt = 100.0, widthPt = 80.0, heightPt = 40.0, rotationDegrees = 0.0)
    private fun centre(t: Transform) = PtPoint(t.xPt + t.widthPt / 2.0, t.yPt + t.heightPt / 2.0)

    @Test
    fun `pure pan shifts centre, leaves size and rotation`() {
        val out = TransformMath.bakeCentreAnchored(base, panPt = PtPoint(10.0, -5.0), zoom = 1.0, rotationDeltaDeg = 0.0)
        assertTransform(Transform(110.0, 95.0, 80.0, 40.0, 0.0), out)
    }

    @Test
    fun `pure uniform scale keeps centre fixed and scales size`() {
        val out = TransformMath.bakeCentreAnchored(base, panPt = PtPoint(0.0, 0.0), zoom = 2.0, rotationDeltaDeg = 0.0)
        // centre (140,120) unchanged; size doubles; x/y re-anchored to centre - half-size = (60, 80).
        assertEquals(centre(base).x, centre(out).x, tol)
        assertEquals(centre(base).y, centre(out).y, tol)
        assertTransform(Transform(60.0, 80.0, 160.0, 80.0, 0.0), out)
    }

    @Test
    fun `rotation delta is additive about the centre`() {
        val rotated = base.copy(rotationDegrees = 30.0)
        val out = TransformMath.bakeCentreAnchored(rotated, panPt = PtPoint(0.0, 0.0), zoom = 1.5, rotationDeltaDeg = 45.0)
        assertEquals(75.0, out.rotationDegrees, tol)
        // centre still fixed under scale+rotate about centre.
        assertEquals(centre(rotated).x, centre(out).x, tol)
        assertEquals(centre(rotated).y, centre(out).y, tol)
    }

    // page-space position of an element-local unit corner (±1,±1), via centre + R(rot)·(lx*w/2, ly*h/2).
    private fun cornerPage(t: Transform, lx: Double, ly: Double): PtPoint {
        val off = com.aritr.zinely.core.model.AffineTransform2D.rotateDeg(t.rotationDegrees)
            .map(PtPoint(lx * t.widthPt / 2.0, ly * t.heightPt / 2.0))
        return PtPoint(t.xPt + t.widthPt / 2.0 + off.x, t.yPt + t.heightPt / 2.0 + off.y)
    }

    @Test
    fun `unrotated corner resize holds the opposite corner and sets new size`() {
        // anchor top-left (-1,-1) fixed at (100,100); drag bottom-right (+1,+1) to (300,300).
        val out = TransformMath.bakeHandleResize(
            base, anchorLocal = PtPoint(-1.0, -1.0), movingLocal = PtPoint(1.0, 1.0), dragPagePt = PtPoint(300.0, 300.0),
        )
        assertTransform(Transform(100.0, 100.0, 200.0, 200.0, 0.0), out)
        // anchor corner unmoved.
        val a = cornerPage(out, -1.0, -1.0)
        assertEquals(100.0, a.x, tol); assertEquals(100.0, a.y, tol)
    }

    @Test
    fun `rotated corner resize keeps the opposite corner fixed in page space`() {
        val rotated = base.copy(rotationDegrees = 90.0)
        val anchorBefore = cornerPage(rotated, -1.0, -1.0)   // opposite-corner page position to preserve
        val out = TransformMath.bakeHandleResize(
            rotated, anchorLocal = PtPoint(-1.0, -1.0), movingLocal = PtPoint(1.0, 1.0),
            dragPagePt = PtPoint(220.0, 260.0),
        )
        assertEquals(90.0, out.rotationDegrees, tol)
        val anchorAfter = cornerPage(out, -1.0, -1.0)
        assertEquals(anchorBefore.x, anchorAfter.x, 1e-6)
        assertEquals(anchorBefore.y, anchorAfter.y, 1e-6)
    }

    @Test
    fun `edge handle resizes one axis only`() {
        // right-edge: anchor left edge (-1,0), moving right edge (+1,0); height unchanged.
        val out = TransformMath.bakeHandleResize(
            base, anchorLocal = PtPoint(-1.0, 0.0), movingLocal = PtPoint(1.0, 0.0), dragPagePt = PtPoint(260.0, 120.0),
        )
        assertEquals(40.0, out.heightPt, tol) // height untouched
        assertEquals(160.0, out.widthPt, tol) // 260 - leftEdgeX(100)
    }
}
