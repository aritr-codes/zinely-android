package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.Fit
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Transform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The pure Reframe framing math (ADR-053): [Framing] maps the ephemeral [FramingDraft] to the crop the
 * overlay paints and the commit persists — so `preview == commit`. No Android; plain JVM. Given-When-Then.
 */
class FramingDraftTest {

    private fun image(crop: Crop = Crop.FULL, fit: Fit = Fit.FILL) =
        ImageElement(id = "img", transform = Transform(0.0, 0.0, 100.0, 100.0), assetId = "a", crop = crop, fit = fit)

    // pratio = 2 (wide photo), bratio = 1 (square frame): the cover crop is full-height, half-width, centred.
    private val pratio = 2.0
    private val bratio = 1.0

    @Test
    fun `cover extent is the maximal box-aspect rect centred in the image`() {
        assertEquals(0.5 to 1.0, Framing.coverExtent(pratio = 2.0, bratio = 1.0))
        assertEquals(1.0 to 0.5, Framing.coverExtent(pratio = 1.0, bratio = 2.0))
        assertEquals(1.0 to 1.0, Framing.coverExtent(pratio = 1.0, bratio = 1.0))
    }

    @Test
    fun `default fill resolves to the centred cover crop`() {
        val c = Framing.resolveCrop(Framing.DEFAULT_FILL, pratio, bratio)
        assertCrop(Crop(0.25, 0.0, 0.75, 1.0), c)
    }

    @Test
    fun `untouched centred fill commits as the FILL full-crop baseline (a no-op commit)`() {
        val after = Framing.toImage(image(), Framing.DEFAULT_FILL, pratio, bratio)
        assertEquals(Crop.FULL, after.crop)
        assertEquals(Fit.FILL, after.fit)
    }

    @Test
    fun `whole photo commits as FIT over the full crop regardless of pan or zoom`() {
        val draft = FramingDraft(FrameFit.WHOLE, zoom = 3.0, panX = 0.2, panY = -0.1)
        val after = Framing.toImage(image(), draft, pratio, bratio)
        assertEquals(Crop.FULL, after.crop)
        assertEquals(Fit.FIT, after.fit)
    }

    @Test
    fun `a zoomed fill commits as FIT over a shrunk centred box-aspect crop`() {
        val draft = Framing.zoomed(Framing.DEFAULT_FILL, 2.0)
        assertEquals(2.0, draft.zoom, 1e-9)
        val after = Framing.toImage(image(), draft, pratio, bratio)
        assertEquals(Fit.FIT, after.fit)
        assertCrop(Crop(0.375, 0.25, 0.625, 0.75), after.crop)
    }

    @Test
    fun `pan is clamped so fill never reveals a gap`() {
        // Push the pan far past the edge; the resolved crop must stay flush inside the image.
        val draft = Framing.panned(Framing.DEFAULT_FILL, dxFraction = 5.0, dyFraction = 0.0, pratio, bratio)
        assertEquals(0.25, draft.panX, 1e-9) // maxX at zoom 1 = (1 - 0.5)/2
        val c = Framing.resolveCrop(draft, pratio, bratio)
        assertCrop(Crop(0.5, 0.0, 1.0, 1.0), c)
    }

    @Test
    fun `zoom is clamped to the 1x - 4x range`() {
        assertEquals(4.0, Framing.zoomed(FramingDraft(FrameFit.FILL, 3.5, 0.0, 0.0), 2.0).zoom, 1e-9)
        assertEquals(1.0, Framing.zoomed(FramingDraft(FrameFit.FILL, 1.2, 0.0, 0.0), 0.1).zoom, 1e-9)
    }

    @Test
    fun `seedDraft round-trips the representations toImage produces`() {
        // FILL + full crop → the centred-fill baseline.
        assertEquals(Framing.DEFAULT_FILL, Framing.seedDraft(image(Crop.FULL, Fit.FILL), pratio, bratio))
        // FIT + full crop → Whole photo.
        assertEquals(FrameFit.WHOLE, Framing.seedDraft(image(Crop.FULL, Fit.FIT), pratio, bratio).fit)
        // FIT + a box-aspect sub-crop → the Fill zoom/pan that produced it.
        val zoomed = Framing.zoomed(Framing.DEFAULT_FILL, 2.0)
        val committed = Framing.toImage(image(), zoomed, pratio, bratio)
        val seeded = Framing.seedDraft(committed, pratio, bratio)
        assertEquals(FrameFit.FILL, seeded.fit)
        assertEquals(2.0, seeded.zoom, 1e-6)
        assertEquals(0.0, seeded.panX, 1e-6)
        assertEquals(0.0, seeded.panY, 1e-6)
    }

    @Test
    fun `property - every resolved crop satisfies the computeImageBlit precondition`() {
        // The renderer requires 0 <= left < right <= 1 and 0 <= top < bottom <= 1 for any draft.
        val ratios = listOf(0.4, 0.85, 1.0, 1.5, 2.5)
        val zooms = listOf(1.0, 1.3, 2.0, 4.0)
        val pans = listOf(-9.0, -0.3, 0.0, 0.3, 9.0)
        for (pr in ratios) for (br in ratios) for (z in zooms) for (px in pans) for (py in pans) {
            val draft = Framing.clampPan(FramingDraft(FrameFit.FILL, z, px, py), pr, br)
            val c = Framing.resolveCrop(draft, pr, br)
            assertTrue("horiz $pr/$br z$z pan($px,$py) -> $c", c.left in 0.0..1.0 && c.right in 0.0..1.0 && c.left < c.right)
            assertTrue("vert $pr/$br z$z pan($px,$py) -> $c", c.top in 0.0..1.0 && c.bottom in 0.0..1.0 && c.top < c.bottom)
        }
    }

    private fun assertCrop(expected: Crop, actual: Crop) {
        assertEquals("left", expected.left, actual.left, 1e-9)
        assertEquals("top", expected.top, actual.top, 1e-9)
        assertEquals("right", expected.right, actual.right, 1e-9)
        assertEquals("bottom", expected.bottom, actual.bottom, 1e-9)
    }
}
