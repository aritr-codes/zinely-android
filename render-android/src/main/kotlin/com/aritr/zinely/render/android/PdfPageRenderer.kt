package com.aritr.zinely.render.android

import android.graphics.pdf.PdfDocument
import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.render.DrawCommand
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

/**
 * The **PDF** export canvas provider (ADR-028 §3.2, "Export PDF"): the second thin provider over the
 * shared [CanvasReplayer]. The `PdfDocument` page is a **PostScript-point** surface, so its `PageInfo`
 * is sized in paper points and the visual CTM is [ExportScale.pdfPageToDevice] — `contentToSheet`
 * **unscaled** (no 300/72). The image-decode resolution is still `300/72`, passed **separately** to
 * `replay` (Required-fix #1). Text stays vector because the replayer issues it via `StaticLayout.draw`
 * onto the page canvas, never `drawBitmap` ([ADR-001], §4.3) — proven structurally at G6.
 *
 * @param replayer shared with the raster provider so both export paths are one draw path.
 */
public class PdfPageRenderer(private val replayer: CanvasReplayer) {

    /**
     * Renders [tape] for one logical page into a single-page PDF and returns the written bytes.
     *
     * @param sheetPt page surface in points; `PageInfo` is `round(width) × round(height)` points
     *   (Letter `612×792`, A4 `595×842` are already integers).
     * @param contentToSheet panel/page placement (identity for a full-sheet single page), passed through
     *   unscaled.
     * @param pageClip page-local clip in points (default = the full sheet).
     */
    public fun render(
        tape: List<DrawCommand>,
        sheetPt: PtSize,
        contentToSheet: AffineTransform2D = AffineTransform2D.identity(),
        pageClip: PtRect = PtRect(0.0, 0.0, sheetPt.width, sheetPt.height),
    ): ByteArray {
        val document = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(
                sheetPt.width.roundToInt(),
                sheetPt.height.roundToInt(),
                /* pageNumber = */ 1,
            ).create()
            val page = document.startPage(pageInfo)
            try {
                replayer.replay(
                    canvas = page.canvas,
                    tape = tape,
                    pageToDevice = ExportScale.pdfPageToDevice(contentToSheet),
                    pageClip = pageClip,
                    decodePxPerPt = ExportScale.EXPORT_PX_PER_PT,
                )
            } finally {
                document.finishPage(page)
            }
            return ByteArrayOutputStream().also { document.writeTo(it) }.toByteArray()
        } finally {
            document.close()
        }
    }
}
