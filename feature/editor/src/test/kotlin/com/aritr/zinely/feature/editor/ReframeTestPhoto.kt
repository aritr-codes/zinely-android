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
 * **This guard is load-bearing on CI, and how often it fires is worth watching.** The failures that
 * prompted it come from a decoder that stops working for a bounded window and then recovers (see
 * [fullImageDecodeAvailable] for the two wrong diagnoses that preceded that one). When a Reframe test
 * starts inside such a window it now skips, which is honest — the environment cannot draw the surface,
 * so there is nothing to assert — rather than failing and reading like a product regression.
 *
 * The cost is real: a skip is absent coverage wearing a green tick. Check the `skipped` count on a CI
 * run before trusting a green Reframe suite, and if it stops being occasional, the environment has got
 * worse and this guard is hiding it. Locally and on device the decoder always works and this never fires.
 */
internal fun assumeFullImageDecodeAvailable() {
    assumeTrue(
        "skipped: this runtime cannot decode full image pixels right now (BitmapFactory.decodeStream " +
            "returned null). The Reframe surface is inert without pixels on screen (M7-01), so there is " +
            "nothing here to assert. Runs locally and on device; see assumeFullImageDecodeAvailable.",
        fullImageDecodeAvailable(),
    )
}

/**
 * Probed **per call**, deliberately — and this is the third answer to the same question, so the two
 * wrong ones are kept here rather than quietly replaced.
 *
 * 1. *"The decoder is absent on Linux CI."* Wrong: most tests decode fine on that image.
 * 2. *"The decoder is exhaustible."* Wrong, and this is what made the probe a cached `by lazy` in the
 *    first place. Exhaustion predicts that once the decoder dies everything after it dies too; the runs
 *    show one to three failures with three hundred passes, including later tests that decode. A resource
 *    that ran out does not come back inside the same JVM. This one does.
 * 3. What the evidence actually supports: the decoder fails for a **bounded window** and then recovers.
 *    That explains the rotating failure set (the window lands somewhere different each run), the small
 *    clusters, and why a fresh JVM always starts healthy.
 *
 * Against a transient window, a cached probe is exactly the wrong shape: it answers once, at whatever
 * moment the first Reframe test happened to run, and every later test inherits that stale answer — so a
 * test unlucky enough to execute inside a dead window fails instead of skipping, which is the CI red this
 * kept producing. Probing per call costs one 4×4 decode and lets each test ask about *its own* moment.
 *
 * This narrows the window rather than closing it: a decoder that dies between this probe and the
 * composition still fails the test. That residue is [#57][https://github.com/aritr-codes/zinely-android/issues/57]'s
 * neighbourhood, not something to solve during a release freeze — and the honest fallback is that a red
 * run here has always been an environment failure, never once a product defect.
 */
private fun fullImageDecodeAvailable(): Boolean {
    val probe = runCatching {
        BitmapFactory.decodeStream(ByteArrayInputStream(probeBytes))
    }.getOrNull()
    probe?.recycle()
    return probe != null
}

/** The probe's 4×4 PNG, encoded once — the *encode* has never been the flaky half. */
private val probeBytes: ByteArray by lazy { encodePhoto(4, 4) }

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
