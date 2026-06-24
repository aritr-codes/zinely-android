package com.aritr.zinely.core.render

import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.Fit
import com.aritr.zinely.core.model.PtRect
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ComputeImageBlitTest {

    private val full = Crop.FULL

    private fun assertRect(exp: PtRect, act: PtRect, eps: Double = 1e-9) {
        assertTrue(kotlin.math.abs(exp.x - act.x) <= eps, "x exp ${exp.x} got ${act.x}")
        assertTrue(kotlin.math.abs(exp.y - act.y) <= eps, "y exp ${exp.y} got ${act.y}")
        assertTrue(kotlin.math.abs(exp.width - act.width) <= eps, "w exp ${exp.width} got ${act.width}")
        assertTrue(kotlin.math.abs(exp.height - act.height) <= eps, "h exp ${exp.height} got ${act.height}")
    }

    @Test
    fun `FIT letterboxes a wide image inside a square box, centered vertically`() {
        // 2:1 image into a 1:1 box → full width, half height, centered (y inset = 25)
        val blit = computeImageBlit(200, 100, full, Fit.FIT, 100.0, 100.0)
        assertRect(PtRect(0.0, 0.0, 1.0, 1.0), blit.srcFraction)
        assertRect(PtRect(0.0, 25.0, 100.0, 50.0), blit.destRect)
    }

    @Test
    fun `FIT pillarboxes a tall image inside a square box, centered horizontally`() {
        // 1:2 image into 1:1 box → full height, half width, x inset = 25
        val blit = computeImageBlit(100, 200, full, Fit.FIT, 100.0, 100.0)
        assertRect(PtRect(0.0, 0.0, 1.0, 1.0), blit.srcFraction)
        assertRect(PtRect(25.0, 0.0, 50.0, 100.0), blit.destRect)
    }

    @Test
    fun `FILL covers the box by cropping the wide image horizontally`() {
        // 2:1 image, 1:1 box → dest = full box; sample centered half-width of the image
        val blit = computeImageBlit(200, 100, full, Fit.FILL, 100.0, 100.0)
        assertRect(PtRect(0.0, 0.0, 100.0, 100.0), blit.destRect)
        assertRect(PtRect(0.25, 0.0, 0.5, 1.0), blit.srcFraction)
    }

    @Test
    fun `FILL covers the box by cropping the tall image vertically`() {
        // 1:1 image, 2:1 box → dest = full box; sample centered half-height
        val blit = computeImageBlit(100, 100, full, Fit.FILL, 80.0, 40.0)
        assertRect(PtRect(0.0, 0.0, 80.0, 40.0), blit.destRect)
        assertRect(PtRect(0.0, 0.25, 1.0, 0.5), blit.srcFraction)
    }

    @Test
    fun `FIT uses the cropped aspect, not the raw image aspect`() {
        // square image, crop to left half (0.5 wide) → cropped aspect 0.5:1 → pillarbox in 1:1 box
        val crop = Crop(left = 0.0, top = 0.0, right = 0.5, bottom = 1.0)
        val blit = computeImageBlit(100, 100, crop, Fit.FIT, 100.0, 100.0)
        assertRect(PtRect(0.0, 0.0, 0.5, 1.0), blit.srcFraction) // samples exactly the crop
        assertRect(PtRect(25.0, 0.0, 50.0, 100.0), blit.destRect) // half-width, centered
    }

    @Test
    fun `FILL composes the sample within the crop window`() {
        // crop horizontal band 0.1..0.9 (cw=0.8) of a square image, FILL a wide 2:1 box
        // cropped aspect = 0.8 < 2.0 → too tall → keep width 0.8, shrink height to 0.4, center in crop
        val crop = Crop(left = 0.1, top = 0.0, right = 0.9, bottom = 1.0)
        val blit = computeImageBlit(100, 100, crop, Fit.FILL, 80.0, 40.0)
        assertRect(PtRect(0.0, 0.0, 80.0, 40.0), blit.destRect)
        assertRect(PtRect(0.1, 0.3, 0.8, 0.4), blit.srcFraction)
    }

    @Test
    fun `extreme aspect stays finite`() {
        val blit = computeImageBlit(1000, 1, full, Fit.FIT, 10.0, 10.0)
        assertTrue(blit.destRect.width.isFinite() && blit.destRect.height.isFinite())
        assertEquals(10.0, blit.destRect.width, 1e-9)
        assertEquals(0.01, blit.destRect.height, 1e-9)
    }

    @Test
    fun `rejects non-positive intrinsic dimensions`() {
        assertThrows(IllegalArgumentException::class.java) {
            computeImageBlit(0, 100, full, Fit.FIT, 10.0, 10.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            computeImageBlit(100, -1, full, Fit.FIT, 10.0, 10.0)
        }
    }

    @Test
    fun `rejects a degenerate or out-of-range crop`() {
        // zero-width crop (right == left) → division by zero in the aspect
        assertThrows(IllegalArgumentException::class.java) {
            computeImageBlit(100, 100, Crop(0.5, 0.0, 0.5, 1.0), Fit.FIT, 10.0, 10.0)
        }
        // out of the unit square
        assertThrows(IllegalArgumentException::class.java) {
            computeImageBlit(100, 100, Crop(-0.1, 0.0, 1.0, 1.0), Fit.FILL, 10.0, 10.0)
        }
        // inverted vertical crop (bottom < top)
        assertThrows(IllegalArgumentException::class.java) {
            computeImageBlit(100, 100, Crop(0.0, 0.8, 1.0, 0.2), Fit.FIT, 10.0, 10.0)
        }
    }

    @Test
    fun `rejects non-positive or non-finite box dimensions`() {
        assertThrows(IllegalArgumentException::class.java) {
            computeImageBlit(100, 100, full, Fit.FIT, 0.0, 10.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            computeImageBlit(100, 100, full, Fit.FIT, 10.0, Double.POSITIVE_INFINITY)
        }
    }
}
