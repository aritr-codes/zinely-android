package com.aritr.zinely.render.android

import android.graphics.Color
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.render.DrawCommand
import com.aritr.zinely.core.render.FillRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * G5 conformance for the PDF export provider + rasterise-back harness (ADR-028 §3.2 / §7.3 layer 1).
 * Proves the G5 exit bar — **both providers render a page; the PDF rasterises back** — and seeds the
 * raster-vs-PDF parity proof the G6 goldens complete:
 *  1. [PdfPageRenderer] emits a valid one-page PDF (`%PDF-` header).
 *  2. [PdfRasterizer] renders that PDF back to a bitmap at the same export dimensions as the raster
 *     path, via an explicit points→px@300/72 matrix (Required-fix #1/B — never diff `PdfDocument.canvas`
 *     against a bitmap directly).
 *  3. For a geometric fill the two paths agree at an interior pixel (exact — no AA tolerance needed),
 *     which is the cross-canvas isolation seed (§7.3 layer 1).
 *
 * `graphicsMode=NATIVE` routes both `PdfDocument` (write) and `PdfRenderer` (read) through real Skia.
 */
@RunWith(RobolectricTestRunner::class)
class PdfExportConformanceTest {

    private val replayer = CanvasReplayer()
    private val pdf = PdfPageRenderer(replayer)
    private val raster = RasterPageRenderer(replayer)
    private val rasterizer = PdfRasterizer()
    private val sheet = PtSize(72.0, 72.0)

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
        val bitmap = rasterizer.rasterize(pdf.render(redFillTape(), sheet))
        assertEquals(300, bitmap.width)   // 72 pt × 300/72
        assertEquals(300, bitmap.height)
    }

    @Test
    fun rasterAndPdfPathsAgreeForGeometricFill() {
        val tape = redFillTape()
        val rasterBitmap = raster.render(tape, sheet)
        val pdfBitmap = rasterizer.rasterize(pdf.render(tape, sheet))

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
