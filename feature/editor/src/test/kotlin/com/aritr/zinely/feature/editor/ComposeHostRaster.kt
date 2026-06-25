package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View

/**
 * Headless rasterizer for the Compose host's view tree — the deterministic, mode-independent pixel
 * readback the S4 parity gate is built on.
 *
 * **Why not `captureToImage()` / `captureRoboImage()` (the derisk lesson, see [ComposeCanvasProbeTest]):**
 *  - compose-ui-test's `captureToImage()` routes through `WindowCapture.forceRedraw`, which under
 *    Robolectric `graphicsMode=NATIVE` never signals "drawn" → `ComposeTimeoutException` after 2 s.
 *  - Roborazzi's `captureRoboImage(path)` is a deliberate **no-op** under a plain `testDebugUnitTest`
 *    (it only rasterises when `-Proborazzi.test.record/verify` is set), so it cannot back a
 *    behavioural assertion without writing a golden.
 *
 * Drawing the laid-out host [View] straight onto a `Canvas(bitmap)` is synchronous, headless, and is
 * the same primitive Roborazzi uses internally — it just skips the window-redraw handshake. The host
 * composable is sized to the page in px, so its raster occupies the top-left `[0,w)×[0,h)` of the
 * decor view, pixel-aligned with the direct-replay reference bitmap.
 */
internal fun View.rasterizeToBitmap(): Bitmap {
    val w = width.coerceAtLeast(1)
    val h = height.coerceAtLeast(1)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    draw(Canvas(bmp))
    return bmp
}
