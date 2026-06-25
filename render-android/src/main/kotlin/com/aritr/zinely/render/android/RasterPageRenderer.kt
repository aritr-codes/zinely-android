package com.aritr.zinely.render.android

import android.graphics.Bitmap
import android.graphics.Canvas
import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.render.DrawCommand

/**
 * The **raster** export canvas provider (ADR-028 §3, "Export image"): one of the two thin providers
 * over the shared [CanvasReplayer]. Allocates an `ARGB_8888` sheet ([ADR-011]) at the export resolution
 * and replays the tape onto a `Canvas(bitmap)` with the points→pixels visual CTM
 * ([ExportScale.rasterPageToDevice]) and `decodePxPerPt = 300/72`. The provider owns only *which* canvas
 * and *which* matrix — every pixel is drawn by the one replayer (structural parity, ADR-006).
 *
 * @param replayer shared across providers so text/image config is identical in both paths.
 */
public class RasterPageRenderer(private val replayer: CanvasReplayer) {

    /**
     * Renders [tape] for one logical page onto a freshly allocated bitmap and returns it. The caller
     * owns the bitmap (and must `recycle()` it after encoding, per the one-sheet-at-a-time policy, R7).
     *
     * @param sheetPt the page surface in points; the bitmap is `pxExtent(width) × pxExtent(height)`.
     * @param contentToSheet panel/page placement (identity for a full-sheet single page); composed under
     *   the DPI scale by [ExportScale.rasterPageToDevice].
     * @param pageClip page-local clip in points (default = the full sheet, valid for identity placement);
     *   the export caller supplies the panel's local bounds for multi-up sheets.
     */
    public fun render(
        tape: List<DrawCommand>,
        sheetPt: PtSize,
        contentToSheet: AffineTransform2D = AffineTransform2D.identity(),
        pageClip: PtRect = PtRect(0.0, 0.0, sheetPt.width, sheetPt.height),
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(
            ExportScale.pxExtent(sheetPt.width),
            ExportScale.pxExtent(sheetPt.height),
            Bitmap.Config.ARGB_8888,
        )
        replayer.replay(
            canvas = Canvas(bitmap),
            tape = tape,
            pageToDevice = ExportScale.rasterPageToDevice(contentToSheet),
            pageClip = pageClip,
            decodePxPerPt = ExportScale.EXPORT_PX_PER_PT,
        )
        return bitmap
    }
}
