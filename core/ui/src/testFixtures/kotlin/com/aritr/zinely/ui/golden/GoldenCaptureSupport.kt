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

/**
 * Vertical rise, in px, of [argb]'s topmost row between the left and right quarter of the span it
 * occupies — the raster read of a **tilt**.
 *
 * A corner-pixel probe cannot do this job: the V2.1 surfaces that lean are `radiusPill`, so a bounding-box
 * corner is ground with the lean and without it, and Compose applies the lean through `graphicsLayer`,
 * which does not move layout bounds at all. Reading the rise across the shape's own width is the smallest
 * measurement that a flat shape actually fails (a `-.6deg` lean across ~800px is ~8px of rise).
 *
 * Returns 0 when [argb] is absent or occupies fewer than four columns.
 */
public fun Bitmap.topRowRiseOf(argb: Int): Int {
    fun topRow(x: Int): Int {
        for (y in 0 until height) if (getPixel(x, y) == argb) return y
        return -1
    }
    var first = -1
    var last = -1
    for (x in 0 until width) if (topRow(x) >= 0) { if (first < 0) first = x; last = x }
    if (first < 0 || last - first < 4) return 0
    val span = last - first
    val left = topRow(first + span / 4)
    val right = topRow(last - span / 4)
    if (left < 0 || right < 0) return 0
    return kotlin.math.abs(left - right)
}

/** Count of pixels exactly equal to [argb] — the flat-colour non-vacuity proof (pre-AA blend). */
public fun Bitmap.pixelCountOf(argb: Int): Int {
    var n = 0
    for (yy in 0 until height) for (xx in 0 until width) if (getPixel(xx, yy) == argb) n++
    return n
}
