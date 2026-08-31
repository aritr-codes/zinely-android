package com.aritr.zinely.data.android

import android.graphics.BitmapFactory
import java.io.IOException
import java.nio.file.Path

internal data class LibraryAssetMetadata(
    val mimeType: String,
    val widthPx: Int,
    val heightPx: Int,
)

internal fun interface LibraryAssetMetadataReader {
    fun read(path: Path): LibraryAssetMetadata
}

internal object AndroidLibraryAssetMetadataReader : LibraryAssetMetadataReader {
    override fun read(path: Path): LibraryAssetMetadata {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path.toString(), options)
        val mime = options.outMimeType ?: throw IOException("asset is not a readable image")
        if (options.outWidth <= 0 || options.outHeight <= 0) throw IOException("asset dimensions are invalid")
        return LibraryAssetMetadata(mime, options.outWidth, options.outHeight)
    }
}
