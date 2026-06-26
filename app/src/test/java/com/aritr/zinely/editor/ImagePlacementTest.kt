package com.aritr.zinely.editor

import com.aritr.zinely.core.model.PtSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [defaultImagePlacement] (ADR-031 §5): a centered, aspect-correct box bounded to a
 * fraction of the page. Given-When-Then; pure geometry, no Android.
 */
class ImagePlacementTest {

    private val page = PtSize(width = 612.0, height = 792.0) // a Letter-ish panel
    private val frac = 0.6
    private val eps = 1e-6

    @Test
    fun `a landscape image is width-bounded, centered, and keeps its aspect`() {
        val t = defaultImagePlacement(masterWidthPx = 2000, masterHeightPx = 1000, pageSizePt = page)

        assertEquals(page.width * frac, t.widthPt, eps) // hits the width bound
        assertEquals(2.0, t.widthPt / t.heightPt, eps) // aspect preserved
        assertEquals((page.width - t.widthPt) / 2.0, t.xPt, eps) // centered x
        assertEquals((page.height - t.heightPt) / 2.0, t.yPt, eps) // centered y
        assertEquals(0.0, t.rotationDegrees, eps)
    }

    @Test
    fun `a tall image is height-bounded and stays within the page`() {
        val t = defaultImagePlacement(masterWidthPx = 1000, masterHeightPx = 2000, pageSizePt = page)

        assertEquals(page.height * frac, t.heightPt, eps) // hits the height bound
        assertEquals(0.5, t.widthPt / t.heightPt, eps)
        assertTrue("box fits horizontally", t.xPt >= 0.0 && t.xPt + t.widthPt <= page.width)
        assertTrue("box fits vertically", t.yPt >= 0.0 && t.yPt + t.heightPt <= page.height)
    }

    @Test
    fun `degenerate zero-height master falls back to a square (no divide-by-zero)`() {
        val t = defaultImagePlacement(masterWidthPx = 100, masterHeightPx = 0, pageSizePt = page)

        assertEquals(1.0, t.widthPt / t.heightPt, eps) // aspect defaulted to 1
        assertTrue(t.widthPt > 0.0 && t.heightPt > 0.0)
    }
}
