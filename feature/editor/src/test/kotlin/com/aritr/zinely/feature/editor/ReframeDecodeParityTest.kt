package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import com.aritr.zinely.render.android.AssetBytesSource
import com.aritr.zinely.render.android.readImageIntrinsics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * **The decode-asymmetry proof (M7-01).** The Reframe overlay reads the photo's aspect with a header-only
 * decode, the same way `ImageBlitter` does, rather than from the full-resolution bitmap it paints.
 *
 * That split is the whole fix. INV-01 found that deriving the aspect from the pixel decode made the
 * overlay strictly less robust than the renderer: a master the renderer could measure was one the overlay
 * could fail on, and on failure the overlay silently fell back to the box ratio while the renderer used
 * the true one — so the committed crop letterboxed content the overlay had never displayed.
 *
 * A real out-of-memory decode cannot be provoked deterministically in a test, so the asymmetry is
 * exercised with its honest analogue: **truncated bytes**, where the header is intact and the pixel data
 * is not. That is the same shape of failure — measurable but not decodable — and it is the case that used
 * to poison the aspect.
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
        val source = reframeTestPhotoMeasurableOnly(widthPx = 800, heightPx = 200)

        val bounds = readImageIntrinsics(source, "a") // the renderer's method: header only, no pixels
        val pixels = decodePhoto(source, "a") // the overlay's own decode, which has nothing to lift

        assertNotNull("the size must be known from the header alone", bounds)
        assertEquals(800, bounds!!.widthPx)
        assertEquals(200, bounds.heightPx)
        assertEquals(4.0, bounds.aspect, 1e-9)
        assertNull("pixels must not be recoverable", pixels)
    }
}
