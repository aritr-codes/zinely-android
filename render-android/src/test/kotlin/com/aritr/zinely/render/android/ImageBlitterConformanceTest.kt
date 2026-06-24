package com.aritr.zinely.render.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.Fit
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.render.DrawImage
import com.aritr.zinely.core.render.computeImageBlit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * G4 conformance for [ImageBlitter] (ADR-028 §7.2 / §5). Asserts the contract — not pixel fidelity
 * (that is the G6 goldens):
 *  1. **`computeImageBlit` routing** — the blitted `destRect` is exactly the pure function's output,
 *     never hand-rolled (§5.2).
 *  2. **Two-open seam** — a successful draw opens the [AssetBytesSource] more than once (bounds, then
 *     decode), each a fresh stream (§5.3).
 *  3. **Missing-asset placeholder** — a `null` on the first OR second open paints the defined
 *     placeholder (fill + border + cross), never a `drawBitmap`, never a crash (§5.4).
 *
 * Runs under the module-default `graphicsMode=NATIVE` so a real in-memory PNG decodes to its true
 * intrinsic size (the routing assertion needs the same intrinsic the blitter reads).
 */
@RunWith(RobolectricTestRunner::class)
class ImageBlitterConformanceTest {

    private val box = PtRect(0.0, 0.0, 50.0, 50.0)
    private val exportDecodePxPerPt = 300.0 / 72.0

    /** A real PNG of known intrinsic size — so `decodeBounds` reports it deterministically. */
    private fun pngBytes(width: Int, height: Int): ByteArray {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.BLUE)
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        bmp.recycle()
        return out.toByteArray()
    }

    private class RecordingCanvas(bitmap: Bitmap) : Canvas(bitmap) {
        var drawBitmapCalls = 0
        var drawRectCalls = 0
        var drawLineCalls = 0
        val bitmapDests = mutableListOf<RectF>()

        override fun drawBitmap(bitmap: Bitmap, src: Rect?, dst: RectF, paint: Paint?) {
            drawBitmapCalls++
            bitmapDests.add(RectF(dst))
            super.drawBitmap(bitmap, src, dst, paint)
        }

        override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
            drawRectCalls++
            super.drawRect(left, top, right, bottom, paint)
        }

        override fun drawLine(startX: Float, startY: Float, stopX: Float, stopY: Float, paint: Paint) {
            drawLineCalls++
            super.drawLine(startX, startY, stopX, stopY, paint)
        }
    }

    private fun replayImage(source: AssetBytesSource, crop: Crop, fit: Fit): RecordingCanvas {
        val canvas = RecordingCanvas(Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888))
        val replayer = CanvasReplayer(imageBlitter = ImageBlitter(source))
        val tape = listOf(
            DrawImage(
                assetId = "asset-1",
                crop = crop,
                fit = fit,
                box = box,
                localToPage = AffineTransform2D.identity(),
                localClip = box,
            ),
        )
        replayer.replay(canvas, tape, AffineTransform2D.identity(), box, exportDecodePxPerPt)
        return canvas
    }

    @Test
    fun routesDestRectThroughComputeImageBlit() {
        val png = pngBytes(100, 60)
        var opens = 0
        val source = AssetBytesSource { opens++; ByteArrayInputStream(png) } // fresh stream each call

        val canvas = replayImage(source, crop = Crop.FULL, fit = Fit.FIT)

        assertEquals(1, canvas.drawBitmapCalls)
        val expected = computeImageBlit(100, 60, Crop.FULL, Fit.FIT, box.width, box.height).destRect
        val dst = canvas.bitmapDests.single()
        assertEquals(expected.x, dst.left.toDouble(), 1e-3)
        assertEquals(expected.y, dst.top.toDouble(), 1e-3)
        assertEquals(expected.right, dst.right.toDouble(), 1e-3)
        assertEquals(expected.bottom, dst.bottom.toDouble(), 1e-3)
        // Bounds pass + at least one decode pass — each a fresh stream (§5.3).
        assertTrue("expected >= 2 opens (bounds + decode), was $opens", opens >= 2)
    }

    @Test
    fun paintsPlaceholderWhenAssetMissingOnFirstOpen() {
        val source = AssetBytesSource { null }

        val canvas = replayImage(source, crop = Crop.FULL, fit = Fit.FIT)

        assertEquals("no bitmap for a missing asset", 0, canvas.drawBitmapCalls)
        assertTrue("placeholder fill + border", canvas.drawRectCalls >= 1)
        assertTrue("broken-image cross", canvas.drawLineCalls >= 1)
    }

    @Test
    fun paintsPlaceholderWhenSecondOpenFails() {
        val png = pngBytes(40, 40)
        var opens = 0
        // Bounds succeeds (open #1) but every decode re-open fails → MISSING, not a half-decoded draw.
        val source = AssetBytesSource { opens++; if (opens == 1) ByteArrayInputStream(png) else null }

        val canvas = replayImage(source, crop = Crop.FULL, fit = Fit.FIT)

        assertEquals(0, canvas.drawBitmapCalls)
        assertTrue("placeholder painted", canvas.drawRectCalls >= 1)
    }

    @Test
    fun nullOnSecondOpenIsMissingEvenIfAThirdOpenWouldSucceed() {
        val png = pngBytes(40, 40)
        var opens = 0
        // open #1 (bounds) ok; open #2 (region) null ⇒ MISSING; a (would-be) open #3 must NOT be tried.
        val source = AssetBytesSource { opens++; if (opens == 2) null else ByteArrayInputStream(png) }

        val canvas = replayImage(source, crop = Crop.FULL, fit = Fit.FIT)

        assertEquals("no draw for a null second open", 0, canvas.drawBitmapCalls)
        assertTrue("placeholder painted", canvas.drawRectCalls >= 1)
        // Stopped at the null second open — never fell through to a third (the AssetBytesSource contract).
        assertEquals(2, opens)
    }
}
