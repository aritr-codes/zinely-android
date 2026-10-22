package com.aritr.zinely.core.render

import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.render.ArtGeometry.bar
import com.aritr.zinely.core.render.ArtGeometry.circle
import com.aritr.zinely.core.render.ArtGeometry.circleReverse
import com.aritr.zinely.core.render.ArtGeometry.closed
import com.aritr.zinely.core.render.ArtGeometry.KAPPA

/**
 * The *Cut shapes* family's outlines (moved verbatim out of [SupplyCatalog]). One object per family keeps each static
 * initialiser under the JVM's 64 KB cap (ADR-139); see `SupplyCatalogClinitHeadroomTest`.
 */
internal object ShapeArt {

    /** Half the rule's authored thickness, as a fraction of the unit square — see [RULE]. */
    internal const val RULE_HALF_THICKNESS: Double = 0.1

    /** The full unit square. Scaled by the fold to the element's own box, so a rectangle *is* its box. */
    internal val RECT: SupplyOutline = closed(
        PtPoint(0.0, 0.0), PtPoint(1.0, 0.0), PtPoint(1.0, 1.0), PtPoint(0.0, 1.0),
    )

    /**
     * The inscribed circle, as the four-cubic approximation every vector format uses. Starts at the
     * top and runs clockwise (+y is down), which is the same handedness as the rest of the geometry —
     * irrelevant to an even-odd fill, and one less thing to be surprised by when reading a diff.
     */
    internal val CIRCLE: SupplyOutline = SupplyOutline(
        listOf(
            Subpath(
                start = PtPoint(0.5, 0.0),
                segments = listOf(
                    Segment.CubicTo(PtPoint(0.5 + KAPPA, 0.0), PtPoint(1.0, 0.5 - KAPPA), PtPoint(1.0, 0.5)),
                    Segment.CubicTo(PtPoint(1.0, 0.5 + KAPPA), PtPoint(0.5 + KAPPA, 1.0), PtPoint(0.5, 1.0)),
                    Segment.CubicTo(PtPoint(0.5 - KAPPA, 1.0), PtPoint(0.0, 0.5 + KAPPA), PtPoint(0.0, 0.5)),
                    Segment.CubicTo(PtPoint(0.0, 0.5 - KAPPA), PtPoint(0.5 - KAPPA, 0.0), PtPoint(0.5, 0.0)),
                ),
            ),
        ),
    )

    /** Isosceles, apex centred on the top edge, base on the bottom — the triangle a maker draws. */
    internal val TRIANGLE: SupplyOutline = closed(
        PtPoint(0.5, 0.0), PtPoint(1.0, 1.0), PtPoint(0.0, 1.0),
    )

    /**
     * A bar across the full width, vertically centred, occupying the middle fifth of the square.
     *
     * The margin above and below is the whole difference between a rule and a [RECT] — without it the
     * two outlines would be identical and only their default size would differ, which is a difference
     * a maker cannot see once they resize. It also gives the divider air inside its own selection box,
     * which is what a divider is *for*.
     */
    internal val RULE: SupplyOutline = closed(
        PtPoint(0.0, 0.5 - RULE_HALF_THICKNESS),
        PtPoint(1.0, 0.5 - RULE_HALF_THICKNESS),
        PtPoint(1.0, 0.5 + RULE_HALF_THICKNESS),
        PtPoint(0.0, 0.5 + RULE_HALF_THICKNESS),
    )

    internal val RING: SupplyOutline = SupplyOutline(
        circle(0.5, 0.5, 0.5) + circleReverse(0.5, 0.5, 7.0 / 24.0),
    )

    /** This family's outlines keyed by `supplyId`, in `Copy.Supplies.BY_FAMILY` order. */
    internal val OUTLINES: Map<String, SupplyOutline> = linkedMapOf(
        "shape.rect" to RECT,
        "shape.circle" to CIRCLE,
        "shape.triangle" to TRIANGLE,
        "shape.rule" to RULE,
        "shape.ring" to RING,
    )
}
