package com.aritr.zinely.render.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.aritr.zinely.core.render.DrawCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * G1 scaffold smoke test (ADR-028, spike §7.1) — NOT a render test; the [CanvasReplayer] arrives at G2.
 *
 * It proves the two things G1 must establish before any logic lands:
 *  1. Robolectric `graphicsMode=NATIVE` actually rasterises real pixels on this CI runner — the
 *     load-bearing prerequisite for every later preview==export golden. A legacy-graphics runner would
 *     return a blank/placeholder bitmap and silently invalidate the whole fidelity tier.
 *  2. The single production edge `api(:core:render)` resolves on the test classpath, so G2+ can replay
 *     the [DrawCommand] tape without a module-wiring surprise.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RenderAndroidScaffoldTest {

    @Test
    fun nativeCanvasRasterisesRealPixels() {
        val bmp = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.WHITE)
            val paint = Paint().apply {
                color = Color.RED
                isAntiAlias = false
            }
            // Fill the interior; under NATIVE Skia this writes a real red raster, under legacy graphics
            // the pixel stays unwritten — so this assertion is the actual NATIVE gate.
            canvas.drawRect(1f, 1f, 7f, 7f, paint)
            assertEquals(Color.RED, bmp.getPixel(4, 4))
            assertEquals(Color.WHITE, bmp.getPixel(0, 0))
        } finally {
            bmp.recycle()
        }
    }

    @Test
    fun coreRenderTapeTypeIsOnTheApiClasspath() {
        // Compile-time + runtime proof that api(:core:render) resolves here; no replay logic yet (G2+).
        val tape: List<DrawCommand> = emptyList()
        assertTrue(tape.isEmpty())
    }
}
