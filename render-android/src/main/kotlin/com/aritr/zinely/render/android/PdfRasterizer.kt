package com.aritr.zinely.render.android

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File

/**
 * Rasterises a written PDF back to a bitmap (ADR-028 §7.3 layer 1, Codex Required-fix #1/B) — the test
 * harness that makes the vector PDF **pixel-comparable** to the raster export. A `PdfDocument.canvas`
 * cannot be diffed against a bitmap directly (it is vector, in points), so the parity proof writes the
 * PDF, reopens it through [PdfRenderer], and renders each page with an **explicit** points→px@300/72
 * matrix onto an `ARGB_8888` bitmap sized by the same [ExportScale.pxExtent] rule the raster path uses —
 * so equal output size isolates canvas-implementation divergence (PDF Skia vs Bitmap Skia) from any
 * coordinate-model effect.
 *
 * Not part of the export product path; it exists for the G6 raster-vs-PDF golden diffs. [PdfRenderer]
 * needs a **seekable** descriptor, so the bytes are staged to a temp file that is deleted afterward.
 */
public class PdfRasterizer {

    /**
     * Renders page [pageIndex] of [pdfBytes] to a fresh `ARGB_8888` bitmap at the export resolution.
     *
     * @param cacheDir directory for the seekable staging file; pass an app cache dir on-device (where
     *   `java.io.tmpdir` is not guaranteed writable). Defaults to the JVM temp dir.
     */
    public fun rasterize(pdfBytes: ByteArray, pageIndex: Int = 0, cacheDir: File? = null): Bitmap {
        val file = File.createTempFile("zinely-export", ".pdf", cacheDir)
        try {
            file.writeBytes(pdfBytes)
            // PdfRenderer takes ownership of the descriptor and closes it on close(); own-and-guard so we
            // never double-close it (the nested-`use` trap) yet never leak it if the constructor throws.
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = try {
                PdfRenderer(descriptor)
            } catch (t: Throwable) {
                descriptor.close()
                throw t
            }
            renderer.use { pages ->
                pages.openPage(pageIndex).use { page ->
                    // page.width/height are in points (1/72"); size the raster by the shared rule.
                    val bitmap = Bitmap.createBitmap(
                        ExportScale.pxExtent(page.width.toDouble()),
                        ExportScale.pxExtent(page.height.toDouble()),
                        Bitmap.Config.ARGB_8888,
                    )
                    val pointsToPixels = Matrix().apply {
                        val s = ExportScale.EXPORT_PX_PER_PT.toFloat()
                        setScale(s, s)
                    }
                    page.render(bitmap, null, pointsToPixels, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    return bitmap
                }
            }
        } finally {
            file.delete()
        }
    }
}
