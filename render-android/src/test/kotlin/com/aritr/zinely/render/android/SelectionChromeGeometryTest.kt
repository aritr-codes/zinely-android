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

    /**
     * ADR-091 row 2.3 — the frozen `.sel` `inset:-7px` is **7 device px**, so it must not change with the
     * zoom. Measured at two scales; at `screenPxPerPt = 2` the box doubles and the inset does not.
     *
     * Mutation: inset 7 → 0 collapses both expectations onto the un-inflated box.
     */
    @Test
    fun inset_is_seven_device_px_at_every_zoom() {
        val t = Transform(0.0, 0.0, 30.0, 10.0, rotationDegrees = 0.0)
        val off = PtPoint(0.0, 0.0)

        val at1 = SelectionChromeGeometry.outlineDevicePx(t, 1.0, off, inflateDevicePx = 7.0)
        assertPt(PtPoint(-7.0, -7.0), at1[0])
        assertPt(PtPoint(37.0, 17.0), at1[2])

        val at2 = SelectionChromeGeometry.outlineDevicePx(t, 2.0, off, inflateDevicePx = 7.0)
        // The box is now 60×20 device px; the outline still stands off by exactly 7, not 14.
        assertPt(PtPoint(-7.0, -7.0), at2[0])
        assertPt(PtPoint(67.0, 27.0), at2[2])
    }

    /**
     * The reason the inflation happens in the element's **local** frame: a rotated element's outline must
     * stay parallel to the element, not to the screen. Inflating an axis-aligned bounding box would leave
     * a 45° photo inside a diamond-shaped gap that is 7px at the corners and much wider at the edges.
     *
     * At 90° the box's own axes swap, so the inflated corner is checked against the rotated geometry: each
     * inflated corner must sit exactly `7·√2` from its un-inflated counterpart (7 along each local axis),
     * at every angle.
     *
     * Mutation: inflate after the rotation with the wrong axes and the 45° case drifts off `7·√2`.
     */
    @Test
    fun inset_follows_the_element_s_own_axes_at_every_rotation() {
        val off = PtPoint(0.0, 0.0)
        val expected = 7.0 * kotlin.math.sqrt(2.0)
        for (deg in listOf(0.0, 30.0, 45.0, 90.0, 137.0, 270.0)) {
            val t = Transform(10.0, 20.0, 40.0, 24.0, rotationDegrees = deg)
            val plain = SelectionChromeGeometry.outlineDevicePx(t, 1.0, off)
            val inflated = SelectionChromeGeometry.outlineDevicePx(t, 1.0, off, inflateDevicePx = 7.0)
            for (i in 0 until 4) {
                val d = kotlin.math.hypot(inflated[i].x - plain[i].x, inflated[i].y - plain[i].y)
                assertEquals("corner $i at $deg° stands off by 7 along both local axes", expected, d, 1e-6)
            }
        }
    }

    /** The default is the box itself — the punch-out and the handles read it, and must not be inflated. */
    @Test
    fun inset_defaults_to_zero() {
        val t = Transform(10.0, 20.0, 40.0, 24.0, rotationDegrees = 33.0)
        val off = PtPoint(0.0, 0.0)
        val default = SelectionChromeGeometry.outlineDevicePx(t, 1.0, off)
        val explicit = SelectionChromeGeometry.outlineDevicePx(t, 1.0, off, inflateDevicePx = 0.0)
        for (i in 0 until 4) assertPt(default[i], explicit[i])
    }
}
