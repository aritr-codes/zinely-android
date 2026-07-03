package com.aritr.zinely.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.render.DrawCommand
import com.aritr.zinely.render.android.ThumbnailRenderer
import java.io.OutputStream
import java.nio.file.Path

/**
 * The real raster half of the shelf-thumbnail producer (S6.4, ADR-045): `:render-android`'s
 * [ThumbnailRenderer] (the proven shared-replayer path — thumbnail == miniature export) plus PNG
 * encode/decode. Pixel correctness is owned by `ThumbnailRendererTest` in `:render-android`'s
 * Robolectric NATIVE lane; this class is compile-checked glue.
 */
internal class AndroidThumbnailRaster(
    private val renderer: ThumbnailRenderer,
    private val longestEdgePx: Int,
) : ThumbnailRaster {

    override fun renderPng(tape: List<DrawCommand>, pageSizePt: PtSize, out: OutputStream): ImageBitmap? {
        val bitmap = try {
            renderer.render(tape, pageSizePt, longestEdgePx)
        } catch (_: RuntimeException) {
            return null // a broken tape must degrade to the placeholder, never break the shelf
        }
        // compress() returning false means a truncated PNG; the producer deletes it (SheetComposer
        // precedent). The bitmap is NOT recycled on success — the returned ImageBitmap wraps it.
        return if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
            bitmap.asImageBitmap()
        } else {
            bitmap.recycle()
            null
        }
    }

    override fun decodePng(file: Path): ImageBitmap? =
        BitmapFactory.decodeFile(file.toString())?.asImageBitmap()
}
