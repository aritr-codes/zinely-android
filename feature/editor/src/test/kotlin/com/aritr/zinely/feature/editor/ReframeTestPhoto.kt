package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.aritr.zinely.render.android.AssetBytesSource
import org.junit.Assume.assumeTrue
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
    assumeFullImageDecodeAvailable()
    val bytes = reframeTestPhotoBytes(widthPx, heightPx)
    return AssetBytesSource { ByteArrayInputStream(bytes) }
}

/**
 * Skip the calling test when the runtime cannot decode full image **pixels**.
 *
 * Robolectric NATIVE on the Linux CI image has no full-image decoder: `BitmapFactory.decodeStream`
 * returns null and the runtime prints *"Failed to create image decoder with message 'unimplemented'"*.
 * The header-only path ([com.aritr.zinely.render.android.readImageIntrinsics], `inJustDecodeBounds`)
 * and `BitmapRegionDecoder` — which is why `:render-android`'s own image goldens pass there — both
 * work; only the whole-image decode `ReframeOverlay.decodePhoto` needs is missing. On the Windows dev
 * machine and on a real device it works.
 *
 * That is not a cosmetic gap for this surface. Since M7-01 the frame is adjustable **only while the
 * photo is genuinely on screen** (`framable = intrinsic != null && decoded != null`), so with no
 * decoder every Reframe session is inert, the controls never mount, and a test asserting framing
 * behaviour would be asserting against a surface the environment cannot draw. Skipping says that;
 * a green run would not.
 *
 * The check is a live probe rather than an OS/property check, so these tests start running again by
 * themselves the day the decoder appears — a skip that cannot silently outlive its cause.
 *
 * **This guard is dormant insurance, not an active skip — and the distinction matters.** The CI
 * failures that prompted it were not caused by a missing decoder but by an *exhausted* one: each test
 * instance re-encoded its own PNG and the probe added another decode, and past some threshold the
 * runtime stopped being able to create a decoder at all — which is why the failing set rotated between
 * runs instead of holding still. Encoding the default photo and the probe once per JVM removed that
 * pressure, and CI now runs every Reframe suite green with `skipped="0"`. The guard remains because a
 * decode-less runtime is still a real possibility and silently asserting nothing would be worse, but
 * it is currently firing nowhere: CI coverage of the Reframe surface is intact, not waived.
 */
internal fun assumeFullImageDecodeAvailable() {
    assumeTrue(
        "skipped: this runtime cannot decode full image pixels (BitmapFactory.decodeStream returned " +
            "null). The Reframe surface is inert without pixels on screen (M7-01), so there is nothing " +
            "here to assert. Runs locally and on device; see assumeFullImageDecodeAvailable.",
        fullImageDecodeAvailable,
    )
}

/**
 * Probed **once per JVM**, not once per test.
 *
 * The CI decoder is not merely absent, it is *exhaustible*: with a probe per test the failing set
 * rotated between runs — the probe would succeed, then the decoder would die partway through the same
 * test and the surface would silently fail to mount. Every extra encode/decode made that likelier, so
 * the probe pays its native cost a single time and every later caller reads the cached answer.
 */
private val fullImageDecodeAvailable: Boolean by lazy {
    val probe = runCatching {
        BitmapFactory.decodeStream(ByteArrayInputStream(reframeTestPhotoBytes(4, 4)))
    }.getOrNull()
    probe?.recycle()
    probe != null
}

/**
 * The default photo's bytes, encoded once per JVM for the same reason as [fullImageDecodeAvailable]:
 * every test instance used to re-encode its own copy. Non-default sizes still encode on demand — they
 * are rare, and caching them would need a key for a handful of callers.
 */
private val defaultPhotoBytes: ByteArray by lazy { encodePhoto(DEFAULT_WIDTH_PX, DEFAULT_HEIGHT_PX) }

private const val DEFAULT_WIDTH_PX = 250
private const val DEFAULT_HEIGHT_PX = 200

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
    // Guarded too, though this fixture deliberately fails its *second* open: the test still drives the
    // mounted controls to prove they are inert, and without a decoder the surrounding surface never
    // reaches the state whose inertness is the point.
    assumeFullImageDecodeAvailable()
    val bytes = reframeTestPhotoBytes(widthPx, heightPx)
    var served = false
    return AssetBytesSource { if (served) null else ByteArrayInputStream(bytes).also { served = true } }
}

private fun reframeTestPhotoBytes(widthPx: Int, heightPx: Int): ByteArray =
    if (widthPx == DEFAULT_WIDTH_PX && heightPx == DEFAULT_HEIGHT_PX) defaultPhotoBytes
    else encodePhoto(widthPx, heightPx)

private fun encodePhoto(widthPx: Int, heightPx: Int): ByteArray {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val bytes = ByteArrayOutputStream()
        .also { bitmap.compress(Bitmap.CompressFormat.PNG, /* quality (lossless) = */ 100, it) }
        .toByteArray()
    bitmap.recycle()
    return bytes
}
