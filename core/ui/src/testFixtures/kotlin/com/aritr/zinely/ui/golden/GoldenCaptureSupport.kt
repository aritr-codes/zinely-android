package com.aritr.zinely.ui.golden

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import kotlin.math.roundToInt

/**
 * Shared capture primitives for the **CI-25** Editor component goldens (roadmap §C1 — the screenshot net
 * before the C6 migration; docs/V1-CONFORMANCE-INVENTORY.md CI-25).
 *
 * These mirror the crop + pixel-read the frozen [TypeBarGoldenTest] / [SelectionChromeGoldenTest] inline:
 * `captureToImage()`/`captureRoboImage()` are not usable for a behavioural read headless under Robolectric
 * NATIVE (see [ComposeHostRaster] / [ComposeCanvasProbeTest]), so a golden host draws the laid-out decor
 * view ([rasterizeToBitmap]) and crops to a tagged node's placed bounds. Extracted here (a pure, unit-safe
 * helper — house convention) only to keep the eleven new golden files free of a copy-pasted crop loop; the
 * per-component fixture, theme, and non-vacuity assertion stay in each test.
 */

/** Crop [full] to [bounds] (root px), clamped inside the bitmap. */
public fun cropToBounds(full: Bitmap, bounds: Rect): Bitmap {
    val x = bounds.left.roundToInt().coerceAtLeast(0)
    val y = bounds.top.roundToInt().coerceAtLeast(0)
    val w = bounds.width.roundToInt().coerceAtMost(full.width - x).coerceAtLeast(1)
    val h = bounds.height.roundToInt().coerceAtMost(full.height - y).coerceAtLeast(1)
    return Bitmap.createBitmap(full, x, y, w, h)
}

/** Count of pixels exactly equal to [argb] — the flat-colour non-vacuity proof (pre-AA blend). */
public fun Bitmap.pixelCountOf(argb: Int): Int {
    var n = 0
    for (yy in 0 until height) for (xx in 0 until width) if (getPixel(xx, yy) == argb) n++
    return n
}
