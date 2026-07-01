package com.aritr.zinely.render.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.render.DrawCommand
import java.io.IOException
import java.io.OutputStream
import kotlin.math.roundToInt

/**
 * One booklet page placed on the physical sheet: its already-rendered [tape] (page-local point
 * [DrawCommand]s from `SceneRenderer`), the imposition [contentToSheet] affine that lands it in its
 * panel cell (rotation included), and the panel [clip] in sheet-local points.
 *
 * Deliberately expressed only in `:core:model` / `:core:render` types — the `Imposer` runs a layer up
 * (in `:app`), so `:render-android` keeps its single production edge on `:core:render` (ADR-039 §1).
 */
public data class SheetPanel(
    val contentToSheet: AffineTransform2D,
    val clip: PtRect,
    val tape: List<DrawCommand>,
)

/**
 * Composites the imposed panels of a single physical sheet onto **one** surface — the multi-panel
 * export product path [ADR-028] implied ("one replayer, loop the panels") but never named
 * ([ADR-039](../../../../../../../../docs/DECISIONS.md#adr-039)). It is **not** a parallel draw model:
 * every pixel/point goes through the same injected [replayer] the preview and the single-panel golden
 * renderers use, so `preview == export` stays structural (ADR-006).
 *
 * Two targets, one loop each:
 *  - **PDF** ([writePdf]) — a single-page `android.graphics.pdf.PdfDocument` sized in PostScript points;
 *    text stays vector (`StaticLayout.draw`, [ADR-001]).
 *  - **PNG** ([writePng]) — a single `ARGB_8888` sheet bitmap at the 300 DPI export resolution
 *    ([ADR-011]), painted paper-white first, encoded, then recycled.
 *
 * Both write straight into the caller's [OutputStream] (ADR-039 §2) rather than returning a `ByteArray`,
 * so a full encoded copy never stacks on top of the ~33 MB sheet bitmap; the caller guards
 * `OutOfMemoryError` around the call.
 *
 * The [overlay] is a sheet-space tape (identity placement) drawn **after** the panels — fold/cut guides
 * today, the [ADR-012] calibration ruler once a sheet margin exists (ADR-039 deferral).
 */
public class SheetComposer(private val replayer: CanvasReplayer) {

    /** Renders [panels] + [overlay] onto a one-page PDF sized `round(w) x round(h)` points and writes it. */
    public fun writePdf(
        sheetPt: PtSize,
        panels: List<SheetPanel>,
        overlay: List<DrawCommand>,
        out: OutputStream,
    ) {
        val document = PdfDocument()
        // ponytail: ~6 lines of PdfDocument scaffolding shared with PdfPageRenderer; duplicated rather
        // than reshaping that proven single-panel golden harness (ADR-039 alternatives).
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(
                sheetPt.width.roundToInt(),
                sheetPt.height.roundToInt(),
                /* pageNumber = */ 1,
            ).create()
            val page = document.startPage(pageInfo)
            try {
                for (panel in panels) {
                    replayer.replay(
                        canvas = page.canvas,
                        tape = panel.tape,
                        pageToDevice = ExportScale.pdfPageToDevice(panel.contentToSheet),
                        pageClip = panel.clip,
                        decodePxPerPt = ExportScale.EXPORT_PX_PER_PT,
                    )
                }
                replaySheetOverlay(page.canvas, sheetPt, overlay, ExportScale.pdfPageToDevice(SHEET_SPACE))
            } finally {
                document.finishPage(page)
            }
            document.writeTo(out)
        } finally {
            document.close()
        }
    }

    /** Renders [panels] + [overlay] onto a paper-white `ARGB_8888` sheet bitmap @300 DPI and writes PNG bytes. */
    public fun writePng(
        sheetPt: PtSize,
        panels: List<SheetPanel>,
        overlay: List<DrawCommand>,
        out: OutputStream,
    ) {
        val bitmap = Bitmap.createBitmap(
            ExportScale.pxExtent(sheetPt.width),
            ExportScale.pxExtent(sheetPt.height),
            Bitmap.Config.ARGB_8888,
        )
        try {
            val canvas = Canvas(bitmap)
            // Paper: an ARGB sheet is transparent by default, which some viewers render black and which
            // reads wrong as "a picture of my zine". Fill white so the PNG is a sheet of paper.
            canvas.drawColor(Color.WHITE)
            for (panel in panels) {
                replayer.replay(
                    canvas = canvas,
                    tape = panel.tape,
                    pageToDevice = ExportScale.rasterPageToDevice(panel.contentToSheet),
                    pageClip = panel.clip,
                    decodePxPerPt = ExportScale.EXPORT_PX_PER_PT,
                )
            }
            replaySheetOverlay(canvas, sheetPt, overlay, ExportScale.rasterPageToDevice(SHEET_SPACE))
            // compress() returns false on encode failure — surfacing it prevents shipping a truncated PNG
            // as if it succeeded (Codex).
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, /* quality (lossless) = */ 100, out)) {
                throw IOException("PNG encode failed for a ${bitmap.width}×${bitmap.height} sheet")
            }
        } finally {
            bitmap.recycle()
        }
    }

    /** Draws the sheet-space [overlay] (identity placement) clipped to the whole sheet, if non-empty. */
    private fun replaySheetOverlay(
        canvas: Canvas,
        sheetPt: PtSize,
        overlay: List<DrawCommand>,
        pageToDevice: AffineTransform2D,
    ) {
        if (overlay.isEmpty()) return
        replayer.replay(
            canvas = canvas,
            tape = overlay,
            pageToDevice = pageToDevice,
            pageClip = PtRect(0.0, 0.0, sheetPt.width, sheetPt.height),
            decodePxPerPt = ExportScale.EXPORT_PX_PER_PT,
        )
    }

    private companion object {
        /** Overlay commands are authored in sheet points already, so their placement is identity. */
        val SHEET_SPACE: AffineTransform2D = AffineTransform2D.identity()
    }
}
