package com.aritr.zinely.render.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.render.DrawCommand
import kotlin.math.ceil

/**
 * The shelf-thumbnail canvas provider (S6.4, ADR-045): a third thin provider over the shared
 * [CanvasReplayer], beside [RasterPageRenderer] (300 DPI export) and [PdfPageRenderer]. One page's
 * tape is replayed onto a paper-white bitmap whose longest edge is [render]'s `longestEdgePx` —
 * the visual CTM and the image decode resolution are the same thumbnail scale (points→pixels),
 * so a thumbnail is a miniature of the export by construction (ADR-027/028 structural parity).
 *
 * @param replayer shared across providers so text/image config is identical in every path.
 */
public class ThumbnailRenderer(private val replayer: CanvasReplayer) {

    /**
     * Renders [tape] for one logical page onto a freshly allocated paper-white `ARGB_8888` bitmap
     * and returns it; the caller owns the bitmap. The scale is `longestEdgePx / max(page dims)`,
     * so the longer page edge lands at exactly [longestEdgePx] and the shorter keeps the aspect
     * (`ceil`, clamped ≥ 1 px — the [ExportScale.pxExtent] rule at thumbnail scale).
     */
    public fun render(tape: List<DrawCommand>, pageSizePt: PtSize, longestEdgePx: Int): Bitmap {
        require(longestEdgePx > 0) { "longestEdgePx must be positive, was $longestEdgePx" }
        val scale = longestEdgePx / maxOf(pageSizePt.width, pageSizePt.height)
        val bitmap = Bitmap.createBitmap(
            ceil(pageSizePt.width * scale).toInt().coerceAtLeast(1),
            ceil(pageSizePt.height * scale).toInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        // Paper: transparent reads as a broken card on the shelf (the SheetComposer.writePng rationale).
        canvas.drawColor(Color.WHITE)
        replayer.replay(
            canvas = canvas,
            tape = tape,
            pageToDevice = ExportScale.uniformScale(scale),
            pageClip = PtRect(0.0, 0.0, pageSizePt.width, pageSizePt.height),
            decodePxPerPt = scale,
        )
        return bitmap
    }
}
