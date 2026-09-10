package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import com.aritr.zinely.render.android.AssetBytesSource
import com.aritr.zinely.render.android.readImageIntrinsics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * **The atomic-load parity proof (M7-01 / issues #56 and #57).** Reframe resolves geometry through the
 * renderer-shared header seam, decodes only a display-bounded bitmap, and publishes both outcomes as one
 * immutable result. A deterministic loader supplies the rare measurable-but-undisplayable state directly;
 * no assertion depends on stream-consumer order or a platform decoder accepting particular corrupt bytes.
 */
@RunWith(RobolectricTestRunner::class)
class ReframeDecodeParityTest {

    private fun pngBytes(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            .toByteArray()
            .also { bitmap.recycle() }
    }

    private fun sourceOf(bytes: ByteArray?): AssetBytesSource =
        AssetBytesSource { bytes?.let { ByteArrayInputStream(it) } }

    @Test
    fun `bounds decode reports the true intrinsic size`() {
        val bounds = readImageIntrinsics(sourceOf(pngBytes(400, 250)), "a")

        assertNotNull(bounds)
        assertEquals(400, bounds!!.widthPx)
        assertEquals(250, bounds.heightPx)
        assertEquals(1.6, bounds.aspect, 1e-9)
    }

    @Test
    fun `a missing asset yields no bounds`() {
        assertNull(readImageIntrinsics(sourceOf(null), "a"))
    }

    @Test
    fun `corrupt bytes yield no bounds`() {
        assertNull(readImageIntrinsics(sourceOf(ByteArray(64) { 0x7F }), "a"))
    }

    /**
     * The measurable-but-undisplayable state: aspect known, pixels absent.
     *
     * Both halves are asserted together because it is their *combination* that closes the divergence:
     * bounds returning a size keeps the committed geometry honest, and pixels returning null is what makes
     * the frame inert so nothing is committed blind.
     *
     * **On what this does and does not pin.** The fixture reproduces the *state* faithfully, which is what
     * these assertions are about. It deliberately does not try to pin *how* the aspect was obtained —
     * that is no longer a test's job. Since ADR-056 the overlay and the renderer call the **same**
     * [readImageIntrinsics], so there is no second implementation to drift from and nothing for a test to
     * catch: the guarantee is structural. (Earlier revisions of M7-01 tried to assert it and could not —
     * four fixtures failed against a lenient decoder — which is precisely why sharing the seam, rather
     * than testing two copies, was the right resolution.)
     */
    @Test
    fun `a measurable but undisplayable master yields the true aspect and no pixels`() {
        val loaded = reframeTestPhotoMeasurableOnlyLoader(widthPx = 800, heightPx = 200)
            .load(sourceOf(null), "a")
        val bounds = loaded.intrinsic
        val pixels = loaded.decoded

        assertNotNull("the size must be known from the header alone", bounds)
        assertEquals(800, bounds!!.widthPx)
        assertEquals(200, bounds.heightPx)
        assertEquals(4.0, bounds.aspect, 1e-9)
        assertNull("pixels must not be recoverable", pixels)
    }

    @Test
    fun `production loading preserves master geometry and bounds preview pixels`() {
        val loaded = ProductionReframePhotoLoader.load(sourceOf(pngBytes(4096, 16)), "a")

        assertEquals(4096, loaded.intrinsic?.widthPx)
        assertEquals(16, loaded.intrinsic?.heightPx)
        assertNotNull(loaded.decoded)
        assertTrue(loaded.decoded!!.widthPx <= ReframePreviewMaxEdgePx)
        assertTrue(loaded.decoded!!.heightPx <= ReframePreviewMaxEdgePx)
    }

    @Test
    fun `preview sampling changes only after the display bound`() {
        assertEquals(1, reframePreviewSampleSize(800, 600))
        assertEquals(1, reframePreviewSampleSize(2048, 16))
        assertEquals(2, reframePreviewSampleSize(2049, 16))
        assertEquals(2, reframePreviewSampleSize(4096, 4096))
        assertEquals(4, reframePreviewSampleSize(4097, 16))
    }
}
