package com.aritr.zinely.render.android

import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtSize
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * G5 unit spec for [ExportScale] (ADR-028 §3.2 / §6) — the export/preview transforms and the shared
 * pixel-extent rule. Pure JVM (no Skia), so it runs in the Android-SDK unit job without Robolectric.
 * The decoupling proved here is the Codex Required-fix #1 contract: the **raster** CTM scales
 * points→pixels, while the **PDF** CTM leaves points untouched (the PDF surface is already points).
 */
class ExportScaleTest {

    @Test
    fun pxExtentRoundsUpAtExportResolution() {
        assertEquals("72 pt → 300 px @300 DPI", 300, ExportScale.pxExtent(72.0))
        assertEquals("Letter width 612 pt → 2550 px", 2550, ExportScale.pxExtent(612.0))
        assertEquals("a zero/degenerate extent never yields a 0-px bitmap", 1, ExportScale.pxExtent(0.0))
    }

    @Test
    fun rasterCtmScalesPointsToPixels() {
        val device = ExportScale.rasterPageToDevice(AffineTransform2D.identity()).map(PtPoint(72.0, 0.0))
        assertEquals(300.0, device.x, 1e-6)
    }

    @Test
    fun pdfCtmLeavesPointsUnscaled() {
        // PDF Required-fix #1: the page surface is already in points — no 300/72 may be baked in.
        val device = ExportScale.pdfPageToDevice(AffineTransform2D.identity()).map(PtPoint(72.0, 0.0))
        assertEquals(72.0, device.x, 1e-6)
    }

    @Test
    fun previewCtmAppliesOffsetThenScale() {
        // Seam for the S4 Compose host (§2.4): scale(screenPxPerPt) · translate(offset).
        val ctm = ExportScale.previewPageToDevice(screenPxPerPt = 2.0, pageOffset = PtPoint(10.0, 5.0))
        val device = ctm.map(PtPoint(0.0, 0.0))
        assertEquals(20.0, device.x, 1e-6)
        assertEquals(10.0, device.y, 1e-6)
    }

    @Test
    fun previewDeviceToPageInvertsPageToDevice() {
        // The gesture seam (§5.4): a touch in px must map back to the exact page point, round-tripping
        // through previewPageToDevice. Picked a non-trivial scale + offset.
        val s = 2.5
        val offset = PtPoint(10.0, -4.0)
        val pagePt = PtPoint(33.0, 21.0)
        val device = ExportScale.previewPageToDevice(s, offset).map(pagePt)
        val back = ExportScale.previewDeviceToPage(s, offset, device)
        assertEquals(pagePt.x, back.x, 1e-6)
        assertEquals(pagePt.y, back.y, 1e-6)
    }

    @Test
    fun paperSizesMatchSpec() {
        assertEquals(PtSize(612.0, 792.0), PaperSize.LETTER.sizePt)
        assertEquals(PtSize(595.0, 842.0), PaperSize.A4.sizePt)
    }
}
