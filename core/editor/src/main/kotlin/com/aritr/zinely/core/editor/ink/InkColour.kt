package com.aritr.zinely.core.editor.ink

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/** Hue in degrees [0, 360), saturation and value in [0, 1]. */
public data class Hsv(val h: Double, val s: Double, val v: Double)

/**
 * Port of the prototype's pure helpers (`docs/design/mockups/v21-ink-mixer.html`, `<script>`:
 * `hsvToHex`, `hueOf`, `hexToHsv`, `lum`, `contrastOnWhite`). Kotlin `%` is a truncated remainder like JS.
 */
public fun hsvToHex(h: Double, s: Double, v: Double): InkHex {
    // Unlike the prototype this never throws: a drag that overshoots the strip hands in out-of-range or NaN
    // values, so wrap the hue and clamp s, v and every channel.
    val hue = if (h.isFinite()) ((h % 360) + 360) % 360 else 0.0
    val sat = if (s.isNaN()) 0.0 else s.coerceIn(0.0, 1.0)
    val value = if (v.isNaN()) 0.0 else v.coerceIn(0.0, 1.0)
    fun f(n: Int): Int {
        val k = (n + hue / 60) % 6
        return ((value - value * sat * max(0.0, min(min(k, 4 - k), 1.0))) * 255).roundToInt().coerceIn(0, 255)
    }
    return InkHex("%02X%02X%02X".format(java.util.Locale.ROOT, f(5), f(3), f(1)))
}

/** Hue of an rgb triple in 0..1, or `null` when the chroma is 0 (a grey has no hue). */
public fun hueOf(r: Double, g: Double, b: Double): Double? {
    val mx = max(r, max(g, b))
    val d = mx - min(r, min(g, b))
    if (d == 0.0) return null
    val h = when (mx) {
        r -> ((g - b) / d) % 6
        g -> (b - r) / d + 2
        else -> (r - g) / d + 4
    }
    return (h * 60 + 360) % 360
}

internal fun rgb01(hex: InkHex): Triple<Double, Double, Double> =
    hex.toColorRgba().let { Triple(it.r / 255.0, it.g / 255.0, it.b / 255.0) }

/** A grey has no hue, so the strip keeps the last one ([keepHue]) instead of snapping to red. */
public fun hexToHsv(hex: InkHex, keepHue: Double?): Hsv {
    val (r, g, b) = rgb01(hex)
    val mx = max(r, max(g, b))
    val mn = min(r, min(g, b))
    return Hsv(hueOf(r, g, b) ?: (keepHue ?: 0.0), if (mx != 0.0) (mx - mn) / mx else 0.0, mx)
}

public fun luminance(hex: InkHex): Double {
    val (r, g, b) = rgb01(hex)
    fun lin(c: Double) = if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    return lin(r) * 0.2126 + lin(g) * 0.7152 + lin(b) * 0.0722
}

public fun contrastOnWhite(hex: InkHex): Double = 1.05 / (luminance(hex) + 0.05)

/** Below this contrast on white an ink is "pale" (shows the pale note). */
public const val PALE_BELOW: Double = 3.0

public fun isPale(hex: InkHex): Boolean = contrastOnWhite(hex) < PALE_BELOW
