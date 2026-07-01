package com.aritr.zinely.render.android

import android.graphics.BitmapFactory
import android.graphics.Color
import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.render.DrawCommand
import com.aritr.zinely.core.render.FillRect
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * NATIVE (real Skia) conformance for the multi-panel [SheetComposer] PNG path (ADR-039 §1). The shipped
 * `RasterPageRenderer` proves a *single* panel; this proves the 8-up product path — that N panels each
 * land in their own sheet cell, a half-turn panel is rotated into its cell, and the sheet-space overlay
 * paints in sheet coordinates. The PDF path (which Robolectric NATIVE cannot generate) is proven
 * instrumented in `PdfExportInstrumentedTest`.
 */
@RunWith(RobolectricTestRunner::class)
class SheetComposerTest {

    private val composer = SheetComposer(CanvasReplayer())
    private val scale = ExportScale.EXPORT_PX_PER_PT // 300/72

    /** Decodes [SheetComposer.writePng] output back to a bitmap for pixel assertions. */
    private fun composePng(sheetPt: PtSize, panels: List<SheetPanel>, overlay: List<DrawCommand> = emptyList()) =
        ByteArrayOutputStream().also { composer.writePng(sheetPt, panels, overlay, it) }.toByteArray()
            .let { BitmapFactory.decodeByteArray(it, 0, it.size) }

    private fun px(pt: Double): Int = (pt * scale).toInt()

    private fun fullFill(color: ColorRgba, size: Double = 72.0): List<DrawCommand> =
        listOf(FillRect(rect = PtRect(0.0, 0.0, size, size), color = color))

    @Test
    fun sheetIsSizedAtExportResolution() {
        val bitmap = composePng(PtSize(144.0, 72.0), emptyList())
        assertEquals(ExportScale.pxExtent(144.0), bitmap.width)  // 600
        assertEquals(ExportScale.pxExtent(72.0), bitmap.height)  // 300
    }

    @Test
    fun compositesEachPanelIntoItsOwnCell() {
        // Two 72×72 panels side by side on a 144×72 sheet: left = identity, right = translate(72,0).
        val sheet = PtSize(144.0, 72.0)
        val panels = listOf(
            SheetPanel(AffineTransform2D.identity(), PtRect(0.0, 0.0, 72.0, 72.0), fullFill(ColorRgba(255, 0, 0, 255))),
            SheetPanel(AffineTransform2D.translate(72.0, 0.0), PtRect(0.0, 0.0, 72.0, 72.0), fullFill(ColorRgba(0, 0, 255, 255))),
        )
        val bitmap = composePng(sheet, panels)
        assertEquals("left cell is panel A", Color.RED, bitmap.getPixel(px(36.0), px(36.0)))
        assertEquals("right cell is panel B", Color.BLUE, bitmap.getPixel(px(108.0), px(36.0)))
    }

    @Test
    fun halfTurnPanelIsRotatedIntoItsCell() {
        // One 72×72 cell at the origin, content half-turned about the panel centre (the top-row flip).
        // A fill of the panel-local TOP-LEFT quadrant must land in the cell's BOTTOM-RIGHT after 180°.
        val sheet = PtSize(72.0, 72.0)
        val topLeftQuadrant = listOf(
            FillRect(rect = PtRect(0.0, 0.0, 36.0, 36.0), color = ColorRgba(0, 180, 0, 255)),
        )
        val panels = listOf(
            SheetPanel(AffineTransform2D.halfTurnAbout(36.0, 36.0), PtRect(0.0, 0.0, 72.0, 72.0), topLeftQuadrant),
        )
        val bitmap = composePng(sheet, panels)
        assertEquals("rotated into bottom-right", Color.rgb(0, 180, 0), bitmap.getPixel(px(54.0), px(54.0)))
        assertEquals("top-left is bare paper", Color.WHITE, bitmap.getPixel(px(18.0), px(18.0)))
    }

    @Test
    fun overlayPaintsInSheetSpace() {
        // No panels; a sheet-space overlay fill at an absolute sheet rectangle paints there.
        val sheet = PtSize(144.0, 72.0)
        val overlay = listOf(
            FillRect(rect = PtRect(96.0, 0.0, 144.0, 72.0), color = ColorRgba(0, 0, 0, 255)),
        )
        val bitmap = composePng(sheet, panels = emptyList(), overlay = overlay)
        assertEquals("overlay region", Color.BLACK, bitmap.getPixel(px(120.0), px(36.0)))
        assertEquals("outside overlay is paper", Color.WHITE, bitmap.getPixel(px(24.0), px(36.0)))
    }
}
