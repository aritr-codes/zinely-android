package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.Transform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Pure-JVM proof that [resizeByHandle] wires the opposite-anchor frame correctly over [TransformMath]. */
class ResizeHandleTest {

    private fun assertTransform(expected: Transform, actual: Transform) {
        assertEquals(expected.xPt, actual.xPt, 1e-9)
        assertEquals(expected.yPt, actual.yPt, 1e-9)
        assertEquals(expected.widthPt, actual.widthPt, 1e-9)
        assertEquals(expected.heightPt, actual.heightPt, 1e-9)
        assertEquals(expected.rotationDegrees, actual.rotationDegrees, 1e-9)
    }

    @Test
    fun opposite_is_mirror_through_centre() {
        assertEquals(PtPoint(1.0, 1.0), ResizeHandle.TOP_LEFT.opposite)
        assertEquals(PtPoint(-1.0, 0.0), ResizeHandle.RIGHT.opposite)
    }

    @Test
    fun corner_drag_holds_opposite_corner_fixed() {
        // 10×10 at origin; drag BOTTOM_RIGHT to (20,20) → TL stays (0,0), box becomes 20×20.
        val after = TransformMath.resizeByHandle(
            before = Transform(0.0, 0.0, 10.0, 10.0),
            handle = ResizeHandle.BOTTOM_RIGHT,
            dragPagePt = PtPoint(20.0, 20.0),
        )
        assertTransform(Transform(0.0, 0.0, 20.0, 20.0), after)
    }

    @Test
    fun edge_drag_resizes_one_axis_only() {
        // RIGHT edge dragged to x=20 → width 20, height unchanged, left edge (x=0) fixed.
        val after = TransformMath.resizeByHandle(
            before = Transform(0.0, 0.0, 10.0, 10.0),
            handle = ResizeHandle.RIGHT,
            dragPagePt = PtPoint(20.0, 5.0),
        )
        assertTransform(Transform(0.0, 0.0, 20.0, 10.0), after)
    }

    @Test
    fun resize_never_changes_rotation() {
        val after = TransformMath.resizeByHandle(
            before = Transform(0.0, 0.0, 10.0, 10.0, rotationDegrees = 30.0),
            handle = ResizeHandle.BOTTOM_RIGHT,
            dragPagePt = PtPoint(15.0, 15.0),
        )
        assertEquals(30.0, after.rotationDegrees, 1e-9)
    }
}
