package com.aritr.zinely.render.android

import android.graphics.Color
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.render.FillRect
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * G5 conformance for the raster export provider (ADR-028 §3.2, "Export image"). Proves a page renders:
 * the bitmap is sized at the export resolution (points × 300/72) and the shared [CanvasReplayer] draws
 * the tape into it. Runs under the module-default `graphicsMode=NATIVE` so the fill is real Skia pixels.
 */
@RunWith(RobolectricTestRunner::class)
class RasterPageRendererTest {

    private val renderer = RasterPageRenderer(CanvasReplayer())

    @Test
    fun rendersPageAtExportPixelDimensions() {
        val bitmap = renderer.render(emptyList(), PtSize(72.0, 144.0))
        assertEquals(300, bitmap.width)   // 72 pt × 300/72
        assertEquals(600, bitmap.height)  // 144 pt × 300/72
    }

    @Test
    fun fillsPageWithCommandColor() {
        val sheet = PtSize(72.0, 72.0)
        val tape = listOf(
            FillRect(rect = PtRect(0.0, 0.0, 72.0, 72.0), color = ColorRgba(255, 0, 0, 255)),
        )
        val bitmap = renderer.render(tape, sheet)
        assertEquals(Color.RED, bitmap.getPixel(bitmap.width / 2, bitmap.height / 2))
    }
}
