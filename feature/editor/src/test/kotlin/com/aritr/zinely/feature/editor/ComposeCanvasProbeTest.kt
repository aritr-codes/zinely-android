package com.aritr.zinely.feature.editor

import android.graphics.Color
import android.graphics.Paint
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * **DERISK PROBE (G5/G6a discipline — never assume the headless path works; spike §2.4 / §7.1).**
 *
 * Before the real preview-host parity test is trusted, prove the load-bearing primitive it depends
 * on: that Robolectric `graphicsMode=NATIVE` can rasterise a **Compose** `Canvas` composable whose
 * body draws through `drawIntoCanvas { it.nativeCanvas }` — the *exact* bridge [PagePreview] uses —
 * and that the resulting pixels are readable headless.
 *
 * **What the probe actually discovered (recorded so the parity test's design is not re-litigated):**
 * the two "obvious" capture APIs do **not** work headless here —
 *  - compose-ui-test's `captureToImage()` routes through `WindowCapture.forceRedraw`, which never
 *    signals "drawn" under NATIVE → `ComposeTimeoutException` after 2 s;
 *  - Roborazzi's `captureRoboImage(path)` is a no-op under a plain `testDebugUnitTest` (writes a
 *    0-byte PNG; only rasterises under `-Proborazzi.test.record/verify`).
 *
 * The path that **does** work headless is drawing the laid-out host `View` straight onto a
 * `Canvas(bitmap)` ([rasterizeToBitmap]) — the same primitive Roborazzi uses internally, minus the
 * window-redraw handshake. This probe proves that path carries the exact pixels `nativeCanvas` drew;
 * if it did not, the parity test would have to move to `src/androidTest` and run on a device.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ComposeCanvasProbeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun nativeCanvasInComposeCanvas_rasterisesRealPixelsHeadless() {
        val redFill = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = false
            color = Color.RED
        }

        composeRule.setContent {
            Canvas(modifier = Modifier.size(40.dp)) {
                drawIntoCanvas { canvas ->
                    // The same primitive PagePreview relies on: draw onto the raw android Canvas.
                    canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, redFill)
                }
            }
        }
        composeRule.waitForIdle()

        val bmp = composeRule.activity.window.decorView.rasterizeToBitmap()

        // The probe's verdict: a real, non-blank raster that carries the colour we drew. The 40dp
        // Canvas sits at the decor view's top-left (mdpi: dp == px), so (20,20) is inside it.
        assertTrue("captured raster is degenerate (${bmp.width}x${bmp.height})", bmp.width > 0 && bmp.height > 0)
        val inside = bmp.getPixel(20, 20)
        assertEquals(
            "pixel inside the Canvas is not the red the nativeCanvas drew — headless Compose Canvas raster is blank/wrong",
            Color.RED, inside,
        )
    }
}
