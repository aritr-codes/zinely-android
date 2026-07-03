package com.aritr.zinely.home

import androidx.compose.ui.graphics.ImageBitmap
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.render.DrawCommand
import java.io.OutputStream
import java.nio.file.Path

/**
 * The shelf's thumbnail seam (S6.4, ADR-045): give me the current page-1 thumbnail for a project,
 * or `null` when one cannot exist right now (unreadable document, deleted project, render
 * failure) — the card shows a warm paper placeholder, the shelf never breaks. Pull-based and
 * self-invalidating (the producer stamps its cache with the document's mtime), so callers just
 * ask again on every shelf emission; a fresh thumbnail is a cheap stat + memory hit.
 */
internal fun interface ShelfThumbnails {
    suspend fun ensure(projectId: String): ImageBitmap?
}

/**
 * The raster half of the producer, seamed off so [ShelfThumbnailProducer]'s cache/staleness logic
 * stays plain-JVM-testable (`Bitmap` needs Robolectric; the real implementation's pixels are
 * proven in `:render-android`'s NATIVE lane).
 */
internal interface ThumbnailRaster {

    /**
     * Renders [tape] at thumbnail scale, writes it to [out] as PNG, and returns the decoded
     * bitmap for immediate display — `null` on any render/encode failure.
     */
    fun renderPng(tape: List<DrawCommand>, pageSizePt: PtSize, out: OutputStream): ImageBitmap?

    /** Decodes a cached thumbnail PNG, or `null` when the bytes are unreadable. */
    fun decodePng(file: Path): ImageBitmap?
}
