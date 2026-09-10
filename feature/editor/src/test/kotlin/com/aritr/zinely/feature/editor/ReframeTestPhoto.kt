package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.aritr.zinely.render.android.AssetBytesSource
import com.aritr.zinely.render.android.ImageIntrinsics
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
 * composition still fails the test. That environment residue is D-101, distinct from #57's now-deterministic
 * measurable-but-undisplayable fixture. The honest fallback is that a red run here has always been an
 * environment failure, never once a product defect.
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
 * A photo that is **measurable but not displayable**, established directly through the loader seam.
 * No stream is consumed, so the precondition cannot change with coroutine scheduling or consumer order.
 */
internal fun reframeTestPhotoMeasurableOnlyLoader(
    widthPx: Int = 250,
    heightPx: Int = 200,
): ReframePhotoLoader = ReframePhotoLoader { _, _ ->
    ReframePhoto(intrinsic = ImageIntrinsics(widthPx, heightPx), decoded = null)
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
