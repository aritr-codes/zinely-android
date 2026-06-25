package com.aritr.zinely.render.android

import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.render.DrawCommand
import com.aritr.zinely.core.render.FillRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * G5 exit-bar proof for the PDF export provider + rasterise-back harness (ADR-028 §3.2 / §7.3 layer 1):
 * **both providers render a page; the PDF rasterises back.** This runs **instrumented** (real device /
 * emulator) because Robolectric `graphicsMode=NATIVE` rasterises `Bitmap`/`Canvas` but cannot generate a
 * `PdfDocument` ("document is closed!" on a fresh page) — the raster provider is proven headless in
 * [RasterPageRendererTest]; the real `android.graphics.pdf` stack is proven here (ADR-028 risk R1/Q3).
 * Same authored-not-headless-CI split as `:data-android`'s `Os.fsync` durability tests — it is
 * compile-checked in the SDK CI job and executed on a device.
 *
 *  1. [PdfPageRenderer] emits a valid one-page PDF (`%PDF-` header).
 *  2. [PdfRasterizer] renders it back to a bitmap at the same export dimensions as the raster path, via
 *     an explicit points→px@300/72 matrix (Required-fix #1/B — never diff `PdfDocument.canvas` directly).
 *  3. For a geometric fill the two paths agree at an interior pixel (exact — the cross-canvas parity
 *     seed; full multi-scale goldens are G6).
 */
@RunWith(AndroidJUnit4::class)
class PdfExportInstrumentedTest {

    private val replayer = CanvasReplayer()
    private val pdf = PdfPageRenderer(replayer)
    private val raster = RasterPageRenderer(replayer)
    private val rasterizer = PdfRasterizer()
    private val sheet = PtSize(72.0, 72.0)

    /** App cache dir — `java.io.tmpdir` is not guaranteed writable on-device. */
    private val cacheDir get() = ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir

    private fun redFillTape(): List<DrawCommand> = listOf(
        FillRect(rect = PtRect(0.0, 0.0, 72.0, 72.0), color = ColorRgba(255, 0, 0, 255)),
    )

    @Test
    fun producesSinglePagePdfBytes() {
        val bytes = pdf.render(redFillTape(), sheet)
        assertTrue("non-empty PDF", bytes.size > 4)
        assertEquals("PDF header", "%PDF-", String(bytes.copyOf(5), Charsets.US_ASCII))
    }

    @Test
    fun pdfRasterisesBackToExportDimensions() {
        val bitmap = rasterizer.rasterize(pdf.render(redFillTape(), sheet), cacheDir = cacheDir)
        assertEquals(300, bitmap.width)   // 72 pt × 300/72
        assertEquals(300, bitmap.height)
    }

    @Test
    fun rasterAndPdfPathsAgreeForGeometricFill() {
        val tape = redFillTape()
        val rasterBitmap = raster.render(tape, sheet)
        val pdfBitmap = rasterizer.rasterize(pdf.render(tape, sheet), cacheDir = cacheDir)

        assertEquals("equal output size per scale (§7.3)", rasterBitmap.width, pdfBitmap.width)
        assertEquals(rasterBitmap.height, pdfBitmap.height)

        val cx = rasterBitmap.width / 2
        val cy = rasterBitmap.height / 2
        assertEquals("raster interior is the fill color", Color.RED, rasterBitmap.getPixel(cx, cy))
        assertEquals(
            "PDF interior pixel matches raster (cross-canvas parity seed)",
            rasterBitmap.getPixel(cx, cy),
            pdfBitmap.getPixel(cx, cy),
        )
    }
}
