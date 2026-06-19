package com.aritr.zinely.core.model

import kotlin.math.hypot

/**
 * Geometry primitives for the zine document model. All values are in **points (1/72")**,
 * the device-independent unit shared by the document model, imposition, and PDF export.
 * Pure data; no Android dependencies.
 */

/** A size in points. Width and height must be finite and non-negative. */
public data class PtSize(val width: Double, val height: Double) {
    init {
        require(width.isFinite() && width >= 0.0) { "width must be finite and >= 0, was $width" }
        require(height.isFinite() && height >= 0.0) { "height must be finite and >= 0, was $height" }
    }
}

/** A point in points, origin top-left, +x right, +y down. Coordinates must be finite. */
public data class PtPoint(val x: Double, val y: Double) {
    init {
        require(x.isFinite()) { "x must be finite, was $x" }
        require(y.isFinite()) { "y must be finite, was $y" }
    }
}

/** An axis-aligned rectangle in points, anchored at its top-left corner. */
public data class PtRect(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
) {
    init {
        require(x.isFinite() && y.isFinite()) { "rect origin must be finite, was ($x, $y)" }
        require(width.isFinite() && width >= 0.0) { "width must be finite and >= 0, was $width" }
        require(height.isFinite() && height >= 0.0) { "height must be finite and >= 0, was $height" }
    }

    public val right: Double get() = x + width
    public val bottom: Double get() = y + height
    public val centerX: Double get() = x + width / 2.0
    public val centerY: Double get() = y + height / 2.0
    public val center: PtPoint get() = PtPoint(centerX, centerY)
    public val area: Double get() = width * height

    /** Inclusive containment test (boundary counts as inside). */
    public fun contains(p: PtPoint): Boolean =
        p.x >= x && p.x <= right && p.y >= y && p.y <= bottom
}

/** A line segment in points. */
public data class PtLine(val start: PtPoint, val end: PtPoint) {
    public val length: Double get() = hypot(end.x - start.x, end.y - start.y)
}
