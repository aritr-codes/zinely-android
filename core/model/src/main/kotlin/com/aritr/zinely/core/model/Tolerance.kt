package com.aritr.zinely.core.model

import kotlin.math.abs

/**
 * Shared floating-point tolerance for geometry comparisons. Used by tests and by the
 * imposition validation layer so equality is consistent across the codebase — never
 * compare points/rects/transforms for exact equality (A4 dimensions are irrational in points).
 */
public object GeometryTolerance {
    public const val DEFAULT: Double = 1e-9
}

public fun Double.approxEquals(other: Double, eps: Double = GeometryTolerance.DEFAULT): Boolean =
    abs(this - other) <= eps

public fun PtPoint.approxEquals(other: PtPoint, eps: Double = GeometryTolerance.DEFAULT): Boolean =
    x.approxEquals(other.x, eps) && y.approxEquals(other.y, eps)

public fun PtSize.approxEquals(other: PtSize, eps: Double = GeometryTolerance.DEFAULT): Boolean =
    width.approxEquals(other.width, eps) && height.approxEquals(other.height, eps)

public fun PtRect.approxEquals(other: PtRect, eps: Double = GeometryTolerance.DEFAULT): Boolean =
    x.approxEquals(other.x, eps) &&
        y.approxEquals(other.y, eps) &&
        width.approxEquals(other.width, eps) &&
        height.approxEquals(other.height, eps)

public fun PtLine.approxEquals(other: PtLine, eps: Double = GeometryTolerance.DEFAULT): Boolean =
    start.approxEquals(other.start, eps) && end.approxEquals(other.end, eps)

public fun AffineTransform2D.approxEquals(other: AffineTransform2D, eps: Double = GeometryTolerance.DEFAULT): Boolean =
    a.approxEquals(other.a, eps) &&
        b.approxEquals(other.b, eps) &&
        c.approxEquals(other.c, eps) &&
        d.approxEquals(other.d, eps) &&
        e.approxEquals(other.e, eps) &&
        f.approxEquals(other.f, eps)
