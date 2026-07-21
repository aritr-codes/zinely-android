package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import com.aritr.zinely.render.android.AssetBytesSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * A real, decodable photo for the Reframe surface tests.
 *
 * Reframe sessions are inert unless the photo is actually on screen (M7-01): the overlay reports an aspect
 * only when the master both measures and decodes, and the host gates every adjustment verb on that report.
 * Before M7-01 these tests ran on the empty byte source and leaned on the overlay's
 * "no decode ⇒ use the frame aspect" fallback — the mechanism INV-01 identified as the way a commit could
 * bake a crop against a photo nobody had seen. That fallback still exists for the *drawing* path; what
 * changed is that it can no longer reach a commit, which is exactly why these sessions now need a real
 * photo. Supplying real bytes exercises the shipping path instead of a fixture shortcut.
 *
 * Callers should size the photo to their element's box aspect. Every Reframe test element is `100×80`
 * (aspect `1.25`), so [reframeTestPhoto]'s default `250×200` keeps `pratio == bratio` and leaves each
 * test's expected crop values exactly as they were.
 *
 * The bytes are encoded **once**; each `open` hands back a fresh stream over them, matching the
 * `AssetBytesSource` contract (a fresh, independent stream at byte 0).
 */
internal fun reframeTestPhoto(widthPx: Int = 250, heightPx: Int = 200): AssetBytesSource {
    val bytes = reframeTestPhotoBytes(widthPx, heightPx)
    return AssetBytesSource { ByteArrayInputStream(bytes) }
}

/**
 * A photo that is **measurable but not displayable**: the first `open` yields the bytes, every later one
 * yields nothing. Since `ReframeOverlay` reads bounds before pixels, the overlay lands in exactly the
 * state INV-01 described — aspect known, pixels absent — and the session must go inert.
 *
 * **What this fixture proves.** It reproduces the *state* faithfully, which is what the session-level and
 * accessibility tests assert behaviour in. It does not attempt to pin *how* the aspect was obtained — since
 * [ADR-056][com.aritr.zinely.render.android.readImageIntrinsics] the overlay and the renderer call one
 * shared function, so there is no second implementation to drift from. That is why the seam was shared
 * rather than tested: four fixtures were tried and none could distinguish a header read from a pixel
 * decode on this decoder (truncated bytes decode to a partial bitmap; a one-shot source observes only one
 * call; an `IDAT`-less PNG is rejected outright; a corrupt-`IDAT` PNG still yields a bitmap).
 */
internal fun reframeTestPhotoMeasurableOnly(widthPx: Int = 250, heightPx: Int = 200): AssetBytesSource {
    val bytes = reframeTestPhotoBytes(widthPx, heightPx)
    var served = false
    return AssetBytesSource { if (served) null else ByteArrayInputStream(bytes).also { served = true } }
}

private fun reframeTestPhotoBytes(widthPx: Int, heightPx: Int): ByteArray {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val bytes = ByteArrayOutputStream()
        .also { bitmap.compress(Bitmap.CompressFormat.PNG, /* quality (lossless) = */ 100, it) }
        .toByteArray()
    bitmap.recycle()
    return bytes
}
