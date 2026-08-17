package com.aritr.zinely.core.render

import kotlin.math.roundToInt

/**
 * The photocopier filter (X3b) — 1-bit Floyd–Steinberg error diffusion over a **downscaled** bitmap,
 * exactly as specified by [ZINE-DIRECTION.md X3b](../../../../../../../../docs/design/ZINE-DIRECTION.md)
 * and [PRODUCT-DIRECTION.md §280](../../../../../../../../docs/design/PRODUCT-DIRECTION.md), and sequenced
 * ahead of the supply set by [ADR-105 D-4](../../../../../../../../docs/DECISIONS.md#adr-105).
 * Pure Kotlin, no Android, no dependency — the platform seam only hands it pixels
 * ([ADR-106](../../../../../../../../docs/DECISIONS.md#adr-106)).
 *
 * ### Why the grid is measured in points, not pixels
 *
 * The editor decodes at screen density and the exporter at `300/72`, so dithering the decoded bitmap
 * directly would give the *same photo* a fine grain on paper and a coarse one on screen — breaking the
 * one promise the render tape exists to keep (preview == export, ADR-006). The dot grid is therefore
 * derived from the destination's size in **page points** ([copierGridSize]), which is identical on every
 * surface; the backend then magnifies the small 1-bit bitmap with filtering **off**, so one dithered
 * dot is one physical dot of the same size in both places.
 *
 * The grid is necessary and **not sufficient**, which review had to point out: `copierGridSize` clamps to
 * the pixels available, and two surfaces resampling from different decodes produce different samples even
 * at an equal grid. Both leaks close in the backend, where a copier photo decodes at
 * [COPIER_DOTS_PER_POINT] rather than at the surface's own density — see `ImageBlitter.draw`.
 *
 * `150` dpi is a decided constant, not a frozen one — no HTML specification names a dot size. The
 * question is filed as [D-082](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-082) for the owner.
 */
public const val COPIER_DOTS_PER_INCH: Int = 150

/** Dots per page point — points are 1/72", so this is the grid density the tape works in. */
public const val COPIER_DOTS_PER_POINT: Double = COPIER_DOTS_PER_INCH / 72.0

/** The 1-bit threshold: a diffused sample at or above this prints ink, below it prints paper. */
internal const val COPIER_THRESHOLD: Int = 128

/**
 * The dot-grid extent for a destination [extentPt] page points long, never exceeding the [sourcePx]
 * actually available (upsampling before dithering would invent detail) and never below one dot.
 */
public fun copierGridSize(extentPt: Double, sourcePx: Int): Int {
    require(sourcePx >= 1) { "sourcePx must be >= 1, was $sourcePx" }
    if (!extentPt.isFinite() || extentPt <= 0.0) return 1
    return (extentPt * COPIER_DOTS_PER_POINT).roundToInt().coerceIn(1, sourcePx)
}

/** Rec.601 luma of a packed ARGB pixel, 0..255. Alpha is ignored — a zine page has no transparency. */
public fun lumaOf(argb: Int): Int {
    val r = (argb ushr 16) and 0xFF
    val g = (argb ushr 8) and 0xFF
    val b = argb and 0xFF
    return (299 * r + 587 * g + 114 * b) / 1000
}

/**
 * Classic Floyd–Steinberg error diffusion over row-major [luma] samples (0..255), left-to-right,
 * top-to-bottom. Returns a new array of `0` (ink) or `255` (paper) — [luma] is not modified.
 *
 * The quantisation error travels to the four not-yet-visited neighbours in the published 7/3/5/1
 * sixteenths. Arithmetic is integer and truncates toward zero — a *definition*, not a shortcut: it is
 * what lets both surfaces produce byte-identical output from identical input rather than merely
 * similar output. (Truncation biases the result ~3% toward ink — a 64×64 field of `64` comes back
 * with a mean of 61.9, not 64.0. Deterministic, and in character for a copier.)
 *
 * ⚠ Identical output needs identical *input*, which is the backend's job, not this function's: see
 * `ImageBlitter.draw`, where a copier photo decodes at [COPIER_DOTS_PER_POINT] instead of the
 * surface's own density. Without that, the editor and the exporter resample differently and error
 * diffusion amplifies the difference into a visibly different dot pattern.
 */
public fun floydSteinbergDither(luma: IntArray, width: Int, height: Int): IntArray {
    require(width >= 1 && height >= 1) { "dither grid must be >= 1x1, was ${width}x$height" }
    require(luma.size == width * height) { "luma size ${luma.size} != ${width}x$height" }

    val buffer = luma.copyOf()
    for (y in 0 until height) {
        val row = y * width
        for (x in 0 until width) {
            val i = row + x
            val old = buffer[i]
            val new = if (old < COPIER_THRESHOLD) 0 else 255
            buffer[i] = new
            val error = old - new
            if (error == 0) continue
            val hasRight = x + 1 < width
            val hasBelow = y + 1 < height
            if (hasRight) buffer[i + 1] += error * 7 / 16
            if (hasBelow) {
                val below = i + width
                if (x > 0) buffer[below - 1] += error * 3 / 16
                buffer[below] += error * 5 / 16
                if (hasRight) buffer[below + 1] += error * 1 / 16
            }
        }
    }
    return buffer
}

/**
 * The whole filter over packed ARGB pixels: luma → dither → opaque black-or-white ARGB. The caller
 * has already resampled to the dot grid ([copierGridSize]); this never resizes.
 */
public fun photocopy(argb: IntArray, width: Int, height: Int): IntArray {
    val dithered = floydSteinbergDither(IntArray(argb.size) { lumaOf(argb[it]) }, width, height)
    return IntArray(dithered.size) { if (dithered[it] == 0) BLACK_ARGB else WHITE_ARGB }
}

private const val BLACK_ARGB: Int = 0xFF000000.toInt()
private const val WHITE_ARGB: Int = 0xFFFFFFFF.toInt()
