package com.aritr.zinely.render.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.render.FillRect
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * G2 conformance for [CanvasReplayer] (ADR-028, spike §7.2). Two checks that JVM geometry alone
 * (ADR-027) cannot make:
 *
 *  1. **Clip-replay order** — every command must replay as the exact quad
 *     `save → concat(localToPage) → clip(localClip) → draw → restore`, nested inside one page-level
 *     `save → concat(pageToDevice) → clip(pageClip) … restore`. A wrong order (e.g. clip before
 *     concat, or a missing restore) silently corrupts every later command.
 *  2. **Matrix conformance** — the `AffineTransform2D → android.graphics.Matrix` conversion plus the
 *     `concat` order must compose to `pageToDevice × localToPage` (ADR-027 clause 2, column-vector).
 *     This catches a transposed/swapped matrix or a pre/post-concat inversion before it reaches a
 *     pixel golden (Codex Recommended, §7.2 / §12.1).
 *
 * Runs under `LEGACY` graphics (overriding the module's NATIVE default): these are pure call-sequence
 * and matrix-math assertions needing no real Skia, and a [RecordingCanvas] subclass reliably observes
 * the calls in LEGACY (the spike's R3 caveat — a recording spy can be bypassed under NATIVE).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class CanvasReplayerConformanceTest {

    private val red = ColorRgba(255, 0, 0)
    private val pageClip = PtRect(0.0, 0.0, 100.0, 100.0)

    /** A [Canvas] that records the replay op sequence and every concatenated [Matrix]. */
    private class RecordingCanvas(bitmap: Bitmap) : Canvas(bitmap) {
        val ops = mutableListOf<String>()
        val concats = mutableListOf<Matrix>()
        /** Save counts returned, in call order; and the targets passed to restoreToCount. */
        val saveCounts = mutableListOf<Int>()
        val restoreArgs = mutableListOf<Int>()

        override fun save(): Int {
            ops.add("save")
            val count = super.save()
            saveCounts.add(count)
            return count
        }

        override fun concat(matrix: Matrix?) {
            ops.add("concat")
            if (matrix != null) concats.add(Matrix(matrix))
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

        override fun restoreToCount(saveCount: Int) {
            ops.add("restore")
            restoreArgs.add(saveCount)
            super.restoreToCount(saveCount)
        }
    }

    @Test
    fun replaysTheClipQuadInOrderWithOnePageScope() {
        val canvas = RecordingCanvas(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))
        val tape = listOf(
            FillRect(
                rect = PtRect(0.0, 0.0, 4.0, 4.0),
                color = red,
                localToPage = AffineTransform2D.translate(10.0, 20.0),
                localClip = PtRect(0.0, 0.0, 4.0, 4.0),
            ),
        )

        CanvasReplayer.replay(
            canvas = canvas,
            tape = tape,
            pageToDevice = AffineTransform2D.identity(),
            pageClip = pageClip,
            decodePxPerPt = 1.0,
        )

        // page: save, concat(pageToDevice), clip(pageClip)
        // cmd:  save, concat(localToPage), clip(localClip), drawRect, restore
        // page: restore
        assertEquals(
            listOf(
                "save", "concat", "clipRect",
                "save", "concat", "clipRect", "drawRect", "restore",
                "restore",
            ),
            canvas.ops,
        )
        // Exactly one page scope + one command scope, and restores target those exact save counts in
        // LIFO order (command restored to its own count, then page) — proves balanced nesting, not just
        // a plausible op name sequence (Codex Recommended).
        assertEquals(2, canvas.saveCounts.size)
        assertEquals(canvas.saveCounts.reversed(), canvas.restoreArgs)
    }

    @Test
    fun omitsTheLocalClipWhenNull() {
        val canvas = RecordingCanvas(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))
        val tape = listOf(
            FillRect(
                rect = PtRect(0.0, 0.0, 4.0, 4.0),
                color = red,
                localToPage = AffineTransform2D.identity(),
                localClip = null,
            ),
        )

        CanvasReplayer.replay(canvas, tape, AffineTransform2D.identity(), pageClip, 1.0)

        // No "clipRect" between the command's "concat" and "drawRect".
        assertEquals(
            listOf("save", "concat", "clipRect", "save", "concat", "drawRect", "restore", "restore"),
            canvas.ops,
        )
    }

    @Test
    fun composedMatrixEqualsPageToDeviceTimesLocalToPage() {
        // pageToDevice = scale 2 + translate(5,7); localToPage = 90° rotation + translate(10,0).
        val pageToDevice = AffineTransform2D(2.0, 0.0, 0.0, 2.0, 5.0, 7.0)
        val localToPage = AffineTransform2D(0.0, 1.0, -1.0, 0.0, 10.0, 0.0)
        val expected = pageToDevice.times(localToPage)

        val canvas = RecordingCanvas(Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888))
        CanvasReplayer.replay(
            canvas = canvas,
            tape = listOf(
                FillRect(PtRect(0.0, 0.0, 1.0, 1.0), red, localToPage = localToPage, localClip = null),
            ),
            pageToDevice = pageToDevice,
            pageClip = pageClip,
            decodePxPerPt = 1.0,
        )

        // Canvas CTM after concat(pageToDevice) then concat(localToPage) is pageMatrix * localMatrix.
        // Reproduce by left-to-right preConcat of the recorded matrices (acc = acc * m).
        val acc = Matrix()
        for (m in canvas.concats) acc.preConcat(m)

        for (p in listOf(PtPoint(0.0, 0.0), PtPoint(1.0, 0.0), PtPoint(0.0, 1.0), PtPoint(3.0, 5.0))) {
            val pts = floatArrayOf(p.x.toFloat(), p.y.toFloat())
            acc.mapPoints(pts)
            val exp = expected.map(p)
            assertEquals(exp.x, pts[0].toDouble(), 1e-3)
            assertEquals(exp.y, pts[1].toDouble(), 1e-3)
        }
    }
}
