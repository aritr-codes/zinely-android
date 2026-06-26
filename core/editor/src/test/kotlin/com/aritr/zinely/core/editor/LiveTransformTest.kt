package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.Transform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The pure half of the gesture pipeline (ADR-029 §5.1/§5.2). [LiveTransform] is the off-reducer
 * accumulator a `detectTransformGestures` loop feeds: per-frame `{pan(px), zoomFactor, rotation}`
 * fold into one ephemeral `{panXpx, panYpx, zoom, rotationDeg}` that drives `graphicsLayer{}` live,
 * then bakes to a committed page-space [Transform] on gesture end. No Compose, no reducer — so the
 * frame math (accumulation, the F2 live min-size clamp, the px→pt pan conversion at bake) is proven
 * in pure JVM, leaving only event decoding in the Compose layer.
 */
class LiveTransformTest {

    private val tol = 1e-9
    private val base = Transform(xPt = 100.0, yPt = 100.0, widthPt = 80.0, heightPt = 40.0, rotationDegrees = 0.0)
    private fun centre(t: Transform) = Pair(t.xPt + t.widthPt / 2.0, t.yPt + t.heightPt / 2.0)

    @Test
    fun `identity accumulates to no-op and bakes to the original box`() {
        val out = LiveTransform().bake(base, screenPxPerPt = 2.0)
        assertEquals(base.xPt, out.xPt, tol)
        assertEquals(base.yPt, out.yPt, tol)
        assertEquals(base.widthPt, out.widthPt, tol)
        assertEquals(base.heightPt, out.heightPt, tol)
        assertEquals(base.rotationDegrees, out.rotationDegrees, tol)
    }

    @Test
    fun `frames accumulate additively for pan and rotation, multiplicatively for zoom`() {
        val live = LiveTransform()
            .accumulate(panXpx = 10.0, panYpx = 4.0, zoomFactor = 2.0, rotationDelta = 15.0)
            .accumulate(panXpx = -4.0, panYpx = 2.0, zoomFactor = 1.5, rotationDelta = 5.0)
        assertEquals(6.0, live.panXpx, tol)
        assertEquals(6.0, live.panYpx, tol)
        assertEquals(3.0, live.zoom, tol)        // 2.0 * 1.5
        assertEquals(20.0, live.rotationDeg, tol) // 15 + 5
    }

    @Test
    fun `bake converts accumulated pixel pan into page points via screenPxPerPt`() {
        // 20px pan at 2 px/pt = 10pt page-space shift of the centre; size and rotation unchanged.
        val out = LiveTransform().accumulate(20.0, -10.0, 1.0, 0.0).bake(base, screenPxPerPt = 2.0)
        assertEquals(centre(base).first + 10.0, centre(out).first, tol)
        assertEquals(centre(base).second - 5.0, centre(out).second, tol)
        assertEquals(80.0, out.widthPt, tol)
    }

    @Test
    fun `bake uniform-scales about the stable centre`() {
        val out = LiveTransform().accumulate(0.0, 0.0, 2.0, 0.0).bake(base, screenPxPerPt = 1.0)
        assertEquals(centre(base).first, centre(out).first, tol)
        assertEquals(centre(base).second, centre(out).second, tol)
        assertEquals(160.0, out.widthPt, tol)
        assertEquals(80.0, out.heightPt, tol)
    }

    @Test
    fun `bake adds the rotation delta about the centre`() {
        val rotated = base.copy(rotationDegrees = 30.0)
        val out = LiveTransform().accumulate(0.0, 0.0, 1.0, 45.0).bake(rotated, screenPxPerPt = 1.0)
        assertEquals(75.0, out.rotationDegrees, tol)
        assertEquals(centre(rotated).first, centre(out).first, tol)
    }

    // — F2: live min-size clamp — the preview scale never collapses the box below MIN_SIZE_PT —

    @Test
    fun `clampedZoom floors the live scale so width never drops below MIN_SIZE_PT`() {
        // height is the binding axis (40pt): minZoom = 1/40; a 0.001 pinch would otherwise vanish it.
        val live = LiveTransform().accumulate(0.0, 0.0, 0.001, 0.0)
        val z = live.clampedZoom(base)
        assertEquals(TransformMath.MIN_SIZE_PT / base.heightPt, z, tol)
        assertTrue(base.widthPt * z >= TransformMath.MIN_SIZE_PT - tol)
        assertTrue(base.heightPt * z >= TransformMath.MIN_SIZE_PT - tol)
    }

    @Test
    fun `clampedZoom leaves an enlarging gesture untouched`() {
        val live = LiveTransform().accumulate(0.0, 0.0, 3.0, 0.0)
        assertEquals(3.0, live.clampedZoom(base), tol)
    }

    @Test
    fun `bake clamps a collapsing pinch to the same floor the live preview shows`() {
        val live = LiveTransform().accumulate(0.0, 0.0, 0.0001, 0.0)
        val out = live.bake(base, screenPxPerPt = 1.0)
        // committed size equals before * clampedZoom (preview == commit parity).
        assertEquals(base.widthPt * live.clampedZoom(base), out.widthPt, tol)
        assertEquals(base.heightPt * live.clampedZoom(base), out.heightPt, tol)
        assertEquals(centre(base).first, centre(out).first, tol) // centre still stable
    }
}
