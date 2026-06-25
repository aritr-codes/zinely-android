package com.aritr.zinely.render.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode

/**
 * Derisk probe (ADR-028, the G6a discipline: never assume what Robolectric NATIVE supports). Before
 * investing in headless text goldens, this proves the two things they depend on:
 *  1. a **bundled asset** [Typeface] actually rasterises real glyphs under `graphicsMode=NATIVE`
 *     (`BitmapRegionDecoder` and `PdfDocument` were both found unsupported — fonts must be checked too);
 *  2. it renders **differently** from the system default, i.e. the bundled Inter is genuinely used and
 *     not silently swapped for a device font.
 *
 * If this fails in CI, headless text goldens are not viable and text fidelity moves to an instrumented
 * `androidTest` (the image/PDF split); the [FontCoverageTest] cmap guard stays headless regardless.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FontRenderingProbeTest {

    private fun renderGlyphs(typeface: Typeface): Bitmap {
        val bmp = Bitmap.createBitmap(360, 60, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 40f
            color = Color.BLACK
        }
        // A pangram-ish spread so the bundled-vs-system shape difference is robust, not one-glyph-brittle.
        canvas.drawText("Hamburgefonstiv", 8f, 44f, paint)
        return bmp
    }

    private fun Bitmap.inkPixels(): Int {
        var n = 0
        for (y in 0 until height) for (x in 0 until width) {
            if (getPixel(x, y) != Color.WHITE) n++
        }
        return n
    }

    @Test
    fun nativeRastersBundledAssetTypefaceDistinctFromSystem() {
        val assets = RuntimeEnvironment.getApplication().assets
        val inter = Typeface.createFromAsset(assets, "fonts/Inter-Regular.ttf")

        val interBmp = renderGlyphs(inter)
        val systemBmp = renderGlyphs(Typeface.DEFAULT)

        // BOTH must be non-blank, else "differing" could pass for the wrong reason (e.g. one side blank).
        assertTrue("bundled Inter must rasterise real glyphs under NATIVE", interBmp.inkPixels() > 50)
        assertTrue("system default must also rasterise (sanity)", systemBmp.inkPixels() > 50)

        // Different typeface ⇒ at least some pixels differ; identical rasters would mean Inter was
        // silently replaced by the system font (the parity hazard this whole tier exists to remove).
        var differing = 0
        for (y in 0 until interBmp.height) for (x in 0 until interBmp.width) {
            if (interBmp.getPixel(x, y) != systemBmp.getPixel(x, y)) differing++
        }
        assertTrue("bundled Inter must render differently from the system default", differing > 20)
    }
}
