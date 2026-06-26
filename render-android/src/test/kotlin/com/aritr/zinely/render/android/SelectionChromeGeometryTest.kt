package com.aritr.zinely.render.android

import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.Transform
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-JVM proof of [SelectionChromeGeometry] — the chrome corners must land on the rendered box at
 * every rotation, using the same preview seam + rotation sign as `SceneRenderer.localToPage`.
 */
class SelectionChromeGeometryTest {

    private fun assertPt(expected: PtPoint, actual: PtPoint) {
        assertEquals("x", expected.x, actual.x, 1e-9)
        assertEquals("y", expected.y, actual.y, 1e-9)
    }

    @Test
    fun axisAligned_box_maps_corners_to_scaled_device_rect() {
        // 20×20 pt box at (40,40); centre (50,50). s=2, no offset.
        val corners = SelectionChromeGeometry.outlineDevicePx(
            Transform(40.0, 40.0, 20.0, 20.0, rotationDegrees = 0.0),
            screenPxPerPt = 2.0,
            pageOffset = PtPoint(0.0, 0.0),
        )
        // TL,TR,BR,BL in page pt: (40,40),(60,40),(60,60),(40,60) → ×2 px.
        assertPt(PtPoint(80.0, 80.0), corners[0])
        assertPt(PtPoint(120.0, 80.0), corners[1])
        assertPt(PtPoint(120.0, 120.0), corners[2])
        assertPt(PtPoint(80.0, 120.0), corners[3])
    }

    @Test
    fun pageOffset_is_applied_before_scale() {
        // previewPageToDevice = scale(s)·translate(offset): devicePx = (pagePt + offset) * s.
        val corners = SelectionChromeGeometry.outlineDevicePx(
            Transform(40.0, 40.0, 20.0, 20.0, rotationDegrees = 0.0),
            screenPxPerPt = 2.0,
            pageOffset = PtPoint(10.0, -4.0),
        )
        // TL page (40,40) → (40+10, 40-4)=(50,36) → ×2 = (100,72).
        assertPt(PtPoint(100.0, 72.0), corners[0])
    }

    @Test
    fun clockwise_90deg_rotates_corner_listing_about_centre() {
        // rotateDeg(90) about origin maps (x,y)→(-y,x) (matches SceneRenderer.localToPage sign).
        val corners = SelectionChromeGeometry.outlineDevicePx(
            Transform(40.0, 40.0, 20.0, 20.0, rotationDegrees = 90.0),
            screenPxPerPt = 1.0,
            pageOffset = PtPoint(0.0, 0.0),
        )
        // local TL(-10,-10)→(10,-10)+centre(50,50)=(60,40); TR(10,-10)→(10,10)=(60,60);
        // BR(10,10)→(-10,10)=(40,60); BL(-10,10)→(-10,-10)=(40,40). s=1.
        assertPt(PtPoint(60.0, 40.0), corners[0])
        assertPt(PtPoint(60.0, 60.0), corners[1])
        assertPt(PtPoint(40.0, 60.0), corners[2])
        assertPt(PtPoint(40.0, 40.0), corners[3])
    }

    @Test
    fun handle_positions_match_corners_and_edge_midpoints() {
        val t = Transform(40.0, 40.0, 20.0, 20.0, rotationDegrees = 0.0)
        val s = 2.0
        val off = PtPoint(0.0, 0.0)
        val corners = SelectionChromeGeometry.outlineDevicePx(t, s, off)
        // Corner handle local (1,1) == BR == corners[2]; edge (1,0) == right-edge midpoint.
        assertPt(corners[2], SelectionChromeGeometry.handleDevicePx(t, PtPoint(1.0, 1.0), s, off))
        assertPt(corners[0], SelectionChromeGeometry.handleDevicePx(t, PtPoint(-1.0, -1.0), s, off))
        assertPt(PtPoint(120.0, 100.0), SelectionChromeGeometry.handleDevicePx(t, PtPoint(1.0, 0.0), s, off)) // right edge
        assertPt(PtPoint(100.0, 80.0), SelectionChromeGeometry.handleDevicePx(t, PtPoint(0.0, -1.0), s, off))  // top edge
    }

    @Test
    fun non_square_box_keeps_width_height_distinct() {
        val corners = SelectionChromeGeometry.outlineDevicePx(
            Transform(0.0, 0.0, 30.0, 10.0, rotationDegrees = 0.0),
            screenPxPerPt = 1.0,
            pageOffset = PtPoint(0.0, 0.0),
        )
        assertPt(PtPoint(0.0, 0.0), corners[0])
        assertPt(PtPoint(30.0, 0.0), corners[1])
        assertPt(PtPoint(30.0, 10.0), corners[2])
        assertPt(PtPoint(0.0, 10.0), corners[3])
    }
}
