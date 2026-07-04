package com.aritr.zinely.editor

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.IOException

/** A decoded, normalised import-master: the JPEG [bytes] plus its pixel dimensions (for placement). */
public data class MasterImage(val bytes: ByteArray, val widthPx: Int, val heightPx: Int) {
    // ByteArray needs structural equals/hashCode to satisfy the data-class contract meaningfully.
    override fun equals(other: Any?): Boolean =
        this === other || (other is MasterImage && widthPx == other.widthPx &&
            heightPx == other.heightPx && bytes.contentEquals(other.bytes))

    override fun hashCode(): Int = (widthPx * 31 + heightPx) * 31 + bytes.contentHashCode()
}

/**
 * Produces the [ADR-023] import-master from a picked image [Uri] (ADR-031 §4): a **sampled** decode
 * (never the unsampled original — OOM guard, Codex RF4), EXIF orientation **consumed** into the pixels
 * (including the mirrored orientations), the longest edge capped to [MAX_EDGE_PX], re-encoded as
 * **JPEG quality-90**. Re-encoding from a `Bitmap` writes no EXIF, so the orientation tag and original
 * geotags/metadata are dropped (the master is upright + privacy-clean).
 *
 * Returns `null` on any failure (unreadable/unsupported/corrupt/`OutOfMemoryError`); the caller maps
 * that to a user-visible import failure rather than crashing.
 *
 * Android (Bitmap) work — runs on an IO dispatcher (the caller confines it); covered headlessly by
 * `ImportMasterDecoderTest` (Robolectric NATIVE real-Skia + shadow resolver, S7.0).
 */
public class ImportMasterDecoder(private val contentResolver: ContentResolver) {

    public fun decodeToMaster(uri: Uri): MasterImage? {
        return try {
            val (srcW, srcH) = readBounds(uri) ?: return null
            if (srcW <= 0 || srcH <= 0) return null

            val sample = sampleSizeForMemory(srcW, srcH)
            val decoded = decodeSampled(uri, sample) ?: return null
            // From here, `decoded` MUST be recycled on every exit (success, failure, OOM) — Codex RF3.
            var normalised: Bitmap? = null
            try {
                val orientation = readOrientation(uri)
                // normalise returns a NEW bitmap when orientation/scale apply, or the source if identity.
                normalised = normalise(decoded, orientation)
                val out = ByteArrayOutputStream()
                // A false compress wrote nothing usable → treat as a decode failure, never a partial master.
                if (!normalised.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) return null
                MasterImage(out.toByteArray(), normalised.width, normalised.height)
            } finally {
                val n = normalised
                if (n != null && n !== decoded) n.recycle()
                decoded.recycle()
            }
        } catch (_: OutOfMemoryError) {
            null // a too-large source ⇒ import Failure, never a crash (Codex RF4)
        } catch (_: IOException) {
            null
        } catch (_: RuntimeException) {
            null // BitmapFactory / region decoder hostile-input failures
        }
    }

    private fun readBounds(uri: Uri): Pair<Int, Int>? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // A bounds-only decodeStream returns null BY CONTRACT (it only fills opts) — the null check
        // must apply to the stream, never the decode result (S7.0: `?.use{decode} ?: return null`
        // made every on-device import fail).
        val stream = contentResolver.openInputStream(uri) ?: return null
        stream.use { BitmapFactory.decodeStream(it, null, opts) }
        return if (opts.outWidth > 0 && opts.outHeight > 0) opts.outWidth to opts.outHeight else null
    }

    private fun decodeSampled(uri: Uri, sample: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    private fun readOrientation(uri: Uri): Int =
        contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

    /**
     * Apply [orientation] (rotate + mirror) and a longest-edge ≤ [MAX_EDGE_PX] scale in **one** matrix,
     * so at most one extra bitmap is allocated. Returns the source unchanged only when the matrix is the
     * identity (orientation normal AND already within the cap).
     */
    private fun normalise(src: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
            else -> Unit // ORIENTATION_NORMAL / UNDEFINED
        }
        // Longest-edge cap (the oriented longest equals the un-oriented longest — a 90° swap keeps max).
        val longest = maxOf(src.width, src.height)
        if (longest > MAX_EDGE_PX) {
            val scale = MAX_EDGE_PX.toFloat() / longest
            matrix.postScale(scale, scale)
        }
        return if (matrix.isIdentity) src
        else Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    /**
     * Largest power-of-two [BitmapFactory.Options.inSampleSize] keeping the decoded pixel count within
     * [MAX_DECODE_PIXELS], so a 50 MP source is never decoded whole (OOM). The decoded longest edge is
     * then capped to [MAX_EDGE_PX] by [normalise]; this only bounds the *transient* decode.
     */
    private fun sampleSizeForMemory(w: Int, h: Int): Int {
        var sample = 1
        while (w.toLong() / sample * (h.toLong() / sample) > MAX_DECODE_PIXELS) sample *= 2
        return sample
    }

    private companion object {
        const val MAX_EDGE_PX = 4096
        const val JPEG_QUALITY = 90
        // ~24 MP transient decode cap (≤ ~96 MB ARGB_8888) — lets a 5000px source decode at full sample
        // and scale to 4096, while forcing a ≥50 MP source to sample down first.
        const val MAX_DECODE_PIXELS = 24_000_000L
    }
}
