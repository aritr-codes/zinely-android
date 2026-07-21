package com.aritr.zinely.render.android

import android.graphics.BitmapFactory

/** A master image's intrinsic pixel size, read without decoding pixels. */
public data class ImageIntrinsics(val widthPx: Int, val heightPx: Int) {
    /** The image's own aspect (`w/h`). */
    public val aspect: Double get() = widthPx.toDouble() / heightPx.toDouble()
}

/**
 * **The single intrinsic-size seam (ADR-056).** Reads a master's dimensions with `inJustDecodeBounds` —
 * allocating no pixels — and is the *only* way any surface may learn how big a photo is.
 *
 * Framing geometry is resolved against this number on both sides of `preview == export`: the renderer
 * feeds it to [computeImageBlit][com.aritr.zinely.core.render.computeImageBlit], and the editor's Reframe
 * overlay resolves the crop it paints against the same value. Two implementations that merely *agree*
 * are not enough — [ADR-053](../../../../../../../../docs/DECISIONS.md#adr-053)'s overlay once derived the
 * aspect from a full-resolution decode instead, which is strictly less robust (it can exhaust memory where
 * a header read cannot). When it failed the overlay silently fell back to the frame's ratio while the
 * renderer used the true one, and a commit could bake a crop that letterboxed content the overlay had
 * never displayed. Sharing one function makes that class of divergence unrepresentable rather than merely
 * absent (INV-01 → M7-01).
 *
 * **Invariant: nothing here may allocate pixels.** The pixel-free property is what makes this callable
 * from a surface that must not fail where the renderer succeeds. Any future work (EXIF orientation,
 * sampling) belongs *inside* this function so both callers inherit it at once — that is the whole point
 * of the seam.
 *
 * @return the intrinsic size, or `null` if the asset is absent, unreadable, or corrupt.
 */
public fun readImageIntrinsics(assetBytes: AssetBytesSource, assetId: String): ImageIntrinsics? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val stream = assetBytes.open(assetId) ?: return null // null open ⇒ missing
    // A hostile/IO-failing stream must not crash the caller — swallow and treat as unreadable.
    runCatching { stream.use { BitmapFactory.decodeStream(it, null, options) } }
    return if (options.outWidth > 0 && options.outHeight > 0) {
        ImageIntrinsics(options.outWidth, options.outHeight)
    } else {
        null // unreadable/corrupt ⇒ missing
    }
}
