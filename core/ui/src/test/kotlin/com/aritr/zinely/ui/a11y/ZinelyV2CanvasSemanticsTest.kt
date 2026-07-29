package com.aritr.zinely.ui.a11y

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The canvas node geometry (A8), as a pure suite: no composition, no density, no Robolectric.
 *
 * This is the arithmetic that decides where a screen-reader user's focus rectangle lands over a canvas.
 * It is invisible to every other kind of test the repository runs — a Roborazzi golden cannot photograph a
 * focus rectangle, and a semantics assertion checks that a node exists, not that it sits on the thing it
 * names. So it is asserted here directly, at each of the boundaries where it could plausibly be wrong.
 */
class ZinelyV2CanvasSemanticsTest {

    private val min = 48f

    @Test
    fun `a large axis-aligned box is bounded exactly, with no inflation`() {
        val rect = zinelyV2CanvasNodeBounds(
            listOf(Offset(100f, 200f), Offset(300f, 200f), Offset(300f, 500f), Offset(100f, 500f)),
            min,
        )!!
        assertEquals(100, rect.leftPx)
        assertEquals(200, rect.topPx)
        assertEquals(200f, rect.widthPx, 0f)
        assertEquals(300f, rect.heightPx, 0f)
    }

    @Test
    fun `a rotated quad is bounded by its four corners, not by its unrotated extent`() {
        // A 100x100 square rotated 45 degrees about (200, 200): its corners lie on the axes through the
        // centre at radius 100/sqrt(2) ~= 70.71, so the AABB is ~141.42 on a side, NOT 100.
        val r = 100f / kotlin.math.sqrt(2f)
        val rect = zinelyV2CanvasNodeBounds(
            listOf(
                Offset(200f, 200f - r), Offset(200f + r, 200f),
                Offset(200f, 200f + r), Offset(200f - r, 200f),
            ),
            min,
        )!!
        assertEquals(2 * r, rect.widthPx, 1e-3f)
        assertEquals(2 * r, rect.heightPx, 1e-3f)
        assertEquals(200f - r, rect.leftPx.toFloat(), 1f)
        assertEquals(200f - r, rect.topPx.toFloat(), 1f)
    }

    @Test
    fun `a tiny element is inflated to the minimum on both axes`() {
        val rect = zinelyV2CanvasNodeBounds(
            listOf(Offset(500f, 500f), Offset(504f, 500f), Offset(504f, 506f), Offset(500f, 506f)),
            min,
        )!!
        assertEquals(min, rect.widthPx, 0f)
        assertEquals(min, rect.heightPx, 0f)
    }

    @Test
    fun `inflation is centred on the element, not grown from its top-left corner`() {
        // The load-bearing property: an inflated node must stay concentric with the mark it names.
        // Growing from the top-left would leave leftPx/topPx at 500/500 and both assertions below fail.
        val corners = listOf(Offset(500f, 500f), Offset(504f, 500f), Offset(504f, 506f), Offset(500f, 506f))
        val rect = zinelyV2CanvasNodeBounds(corners, min)!!
        assertEquals("node centre X must equal element centre X", 502f, rect.leftPx + rect.widthPx / 2f, 0.5f)
        assertEquals("node centre Y must equal element centre Y", 503f, rect.topPx + rect.heightPx / 2f, 0.5f)
    }

    @Test
    fun `an element short on one axis only is inflated on that axis alone`() {
        // A wide, thin banner: the width is already past the floor and must not be touched.
        val rect = zinelyV2CanvasNodeBounds(
            listOf(Offset(0f, 0f), Offset(400f, 0f), Offset(400f, 10f), Offset(0f, 10f)),
            min,
        )!!
        assertEquals(400f, rect.widthPx, 0f)
        assertEquals(min, rect.heightPx, 0f)
    }

    @Test
    fun `a single point is placeable and opens to a full target centred on itself`() {
        val rect = zinelyV2CanvasNodeBounds(listOf(Offset(80f, 90f)), min)!!
        assertEquals(min, rect.widthPx, 0f)
        assertEquals(min, rect.heightPx, 0f)
        assertEquals(80f, rect.leftPx + rect.widthPx / 2f, 0.5f)
        assertEquals(90f, rect.topPx + rect.heightPx / 2f, 0.5f)
    }

    @Test
    fun `no corners means no node`() {
        assertNull(zinelyV2CanvasNodeBounds(emptyList(), min))
    }

    @Test
    fun `negative coordinates survive - an element panned off the top-left is still bounded`() {
        // The canvas pans, so device-px bounds legitimately go negative. roundToInt must not clamp.
        val rect = zinelyV2CanvasNodeBounds(
            listOf(Offset(-300f, -200f), Offset(-100f, -200f), Offset(-100f, -50f), Offset(-300f, -50f)),
            min,
        )!!
        assertEquals(-300, rect.leftPx)
        assertEquals(-200, rect.topPx)
        assertEquals(200f, rect.widthPx, 0f)
        assertEquals(150f, rect.heightPx, 0f)
    }

    @Test
    fun `the minimum side is the WCAG 2-5-8 platform floor`() {
        assertEquals(48f, ZinelyV2CanvasNodeMinSide.value, 0f)
    }

    @Test
    fun `the node tag prefix is stable`() {
        // Tests and any future device-dump recipe address nodes by this prefix; changing it silently
        // would break every locator without failing a compile.
        assertEquals("zinely-v2-canvas-node-", ZinelyV2CanvasNodeTagPrefix)
        assertTrue(ZinelyV2CanvasNodeTagPrefix.endsWith("-"))
    }
}
