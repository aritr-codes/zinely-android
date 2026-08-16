package com.aritr.zinely.render.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.render.DrawShape
import com.aritr.zinely.core.render.FillRect
import com.aritr.zinely.core.render.SupplyCatalog
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * `DrawShape` in the one replayer — asserting the **absence** of a draw, on purpose.
 *
 * Package P2 adds the command, the outline type and the catalogue; arming the replayer is P3, and it
 * needs an anti-aliased `Paint` and an even-odd `Path` (SUPPLIES-SPEC §3.5). Until then a supply
 * replays as nothing, and that is a *decision* — pinned here rather than left to be discovered as a
 * bug. **When P3 lands these assertions must be inverted, not deleted.**
 *
 * The second test is the one that matters more: drawing nothing must not disturb the commands around
 * it. This replayer is shared by preview, PNG, PDF and the imposed sheet (ADR-028), so an unbalanced
 * save scope here would corrupt every later command on four surfaces at once.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class ShapeReplayDrawsNothingTest {

    private val replayer = CanvasReplayer()
    private val pageClip = PtRect(0.0, 0.0, 100.0, 100.0)

    private fun shape() = DrawShape(
        outline = SupplyCatalog.outlineOf("shape.triangle")!!,
        ink = ColorRgba(200, 40, 90),
        // The unit-square fold (§3.4.1): without the scale term a supply renders 1pt x 1pt. Non-uniform
        // on purpose — 40 x 25 is a legal placement, and `uniformScale()` has nothing to say about it.
        localToPage = AffineTransform2D.translate(10.0, 10.0)
            .times(AffineTransform2D.scale(40.0, 25.0)),
    )

    /** Records every drawing call the replayer could plausibly make, plus the save/restore scopes. */
    private class RecordingCanvas(bitmap: Bitmap) : Canvas(bitmap) {
        val ops = mutableListOf<String>()
        val saveCounts = mutableListOf<Int>()
        val restoreArgs = mutableListOf<Int>()

        override fun save(): Int {
            ops.add("save")
            return super.save().also { saveCounts.add(it) }
        }

        override fun concat(matrix: Matrix?) {
            ops.add("concat")
            super.concat(matrix)
        }

        override fun clipRect(left: Float, top: Float, right: Float, bottom: Float): Boolean {
            ops.add("clipRect")
            return super.clipRect(left, top, right, bottom)
        }

        override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
            ops.add("drawRect")
            super.drawRect(left, top, right, bottom, paint)
        }

        override fun drawPath(path: Path, paint: Paint) {
            ops.add("drawPath")
            super.drawPath(path, paint)
        }

        override fun drawBitmap(bitmap: Bitmap, left: Float, top: Float, paint: Paint?) {
            ops.add("drawBitmap")
            super.drawBitmap(bitmap, left, top, paint)
        }

        override fun restoreToCount(saveCount: Int) {
            ops.add("restore")
            restoreArgs.add(saveCount)
            super.restoreToCount(saveCount)
        }
    }

    @Test
    fun `given a tape of only shapes, when replayed, then the canvas is never drawn to`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = RecordingCanvas(bitmap)

        replayer.replay(canvas, listOf(shape()), AffineTransform2D.identity(), pageClip, 1.0)

        // The scope quad still runs — only the draw is absent.
        assertEquals(listOf("save", "concat", "clipRect", "save", "concat", "restore", "restore"), canvas.ops)
        // And the pixels agree. The op list above is a whitelist of the calls this spy overrides, so a
        // P3 arm reaching for drawOval/drawCircle/drawColor would slip past it; an untouched bitmap
        // cannot be slipped past. Kept alongside the op list because they fail differently: the ops say
        // *what* was called, the pixels say *whether anything landed*.
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        assertEquals("a supply must not paint a single pixel yet", 0, pixels.count { it != 0 })
    }

    @Test
    fun `given a shape between two fills, when replayed, then both fills still draw and scopes balance`() {
        val canvas = RecordingCanvas(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))
        val fill = FillRect(
            rect = PtRect(0.0, 0.0, 4.0, 4.0),
            color = ColorRgba(255, 0, 0),
            localToPage = AffineTransform2D.identity(),
            localClip = null,
        )

        replayer.replay(canvas, listOf(fill, shape(), fill), AffineTransform2D.identity(), pageClip, 1.0)

        assertEquals(2, canvas.ops.count { it == "drawRect" })
        assertEquals(4, canvas.saveCounts.size) // one page scope + three command scopes
        assertEquals(canvas.saveCounts.reversed(), canvas.restoreArgs) // strict LIFO, nothing leaked
    }
}
