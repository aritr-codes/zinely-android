package com.aritr.zinely.core.render

import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.render.ArtGeometry.bar24
import com.aritr.zinely.core.render.ArtGeometry.bar
import com.aritr.zinely.core.render.ArtGeometry.circle
import com.aritr.zinely.core.render.ArtGeometry.circleReverse
import com.aritr.zinely.core.render.ArtGeometry.closed
import com.aritr.zinely.core.render.ArtGeometry.p24

/**
 * The *Tape & fixings* family's outlines (moved verbatim out of [SupplyCatalog]). One object per family keeps each static
 * initialiser under the JVM's 64 KB cap (ADR-139); see `SupplyCatalogClinitHeadroomTest`.
 */
internal object TapeArt {

    /**
     * A staple, seen flat: a crossbar with two legs turned down at its ends.
     *
     * **One polygon, eight points** — not a bar plus two legs, for the even-odd reason in this
     * header comment in `MarkArt`. Proportions are a real staple's: the crown is most of the width, the legs are
     * about a third of the height, and the wire is thin enough to read as wire rather than as a
     * bracket.
     */
    internal val STAPLE: SupplyOutline = closed(
        PtPoint(0.0, 0.30), PtPoint(1.0, 0.30), PtPoint(1.0, 1.00), PtPoint(0.82, 1.00),
        PtPoint(0.82, 0.48), PtPoint(0.18, 0.48), PtPoint(0.18, 1.00), PtPoint(0.0, 1.00),
    )

    /**
     * A photo corner: the triangular pocket a print slides into, with the pocket cut out.
     *
     * Two subpaths and therefore a hole, exactly like [PaperArt.WINDOW] — the inner triangle is the opening the
     * photograph's corner goes behind, and without it the supply is a solid triangle already served by
     * `shape.triangle`.
     */
    internal val PHOTO_CORNER: SupplyOutline = SupplyOutline(
        closed(PtPoint(0.0, 0.0), PtPoint(1.0, 1.0), PtPoint(0.0, 1.0)).subpaths +
            closed(PtPoint(0.16, 0.42), PtPoint(0.58, 0.84), PtPoint(0.16, 0.84)).subpaths,
    )

    // =============================================================================================
    // The authored hand — v21-bench.html A11
    //
    // These four silhouettes were authored from scratch as coordinates for Zinely. No reference art
    // was traced, opened or consulted. They are deterministic, carry no third-party licence, and use
    // only the existing unit-square, fill-only, even-odd outline contract.

    /** Torn tape: clean long edges with two irregular ends, made long by its existing placement box. */
    internal val TORN_TAPE: SupplyOutline = closed(
        PtPoint(0.00, 0.18), PtPoint(0.06, 0.08), PtPoint(0.02, 0.00),
        PtPoint(0.97, 0.00), PtPoint(0.93, 0.10), PtPoint(1.00, 0.20),
        PtPoint(0.95, 0.31), PtPoint(1.00, 0.43), PtPoint(0.94, 0.56),
        PtPoint(0.99, 0.70), PtPoint(0.93, 0.84), PtPoint(0.98, 1.00),
        PtPoint(0.02, 1.00), PtPoint(0.07, 0.88), PtPoint(0.00, 0.76),
        PtPoint(0.05, 0.63), PtPoint(0.01, 0.49), PtPoint(0.06, 0.34),
    )

    /**
     * A paper clip as one closed ribbon around its doubled wire path. The contour never crosses itself,
     * so even-odd fill produces a solid wire without asking any backend to grow a stroke feature.
     */
    internal val PAPER_CLIP: SupplyOutline = closed(
        PtPoint(0.45, 0.18), PtPoint(0.45, 0.62), PtPoint(0.40, 0.68),
        PtPoint(0.34, 0.68), PtPoint(0.30, 0.62), PtPoint(0.30, 0.22),
        PtPoint(0.36, 0.10), PtPoint(0.50, 0.00), PtPoint(0.70, 0.10),
        PtPoint(0.84, 0.22), PtPoint(0.88, 0.76), PtPoint(0.76, 0.90),
        PtPoint(0.60, 1.00), PtPoint(0.22, 1.00), PtPoint(0.10, 0.88),
        PtPoint(0.06, 0.76), PtPoint(0.06, 0.34), PtPoint(0.16, 0.34),
        PtPoint(0.16, 0.72), PtPoint(0.20, 0.80), PtPoint(0.28, 0.84),
        PtPoint(0.58, 0.84), PtPoint(0.68, 0.78), PtPoint(0.72, 0.70),
        PtPoint(0.72, 0.26), PtPoint(0.66, 0.18), PtPoint(0.58, 0.14),
        PtPoint(0.50, 0.14),
    )

    // =============================================================================================
    // ADR-107 first expansion — sixteen print-native materials, authored from A15/A16's ruled set.

    internal val MASKING_TAPE: SupplyOutline = closed(
        p24(0, 3), p24(24, 3), p24(24, 21), p24(0, 21),
    )

    internal val SADDLE_STITCH: SupplyOutline = SupplyOutline(
        bar24(5, 0, 8, 24) + bar24(16, 0, 19, 24),
    )

    internal val EYELET: SupplyOutline = SupplyOutline(
        circle(0.5, 0.5, 0.5) + circleReverse(0.5, 0.5, 7.0 / 24.0),
    )

    internal val PUSH_PIN: SupplyOutline = closed(
        p24(5, 2), p24(19, 2), p24(17, 8), p24(14, 10), p24(14, 17),
        p24(12, 24), p24(10, 17), p24(10, 10), p24(7, 8),
    )

    /** This family's outlines keyed by `supplyId`, in `Copy.Supplies.BY_FAMILY` order. */
    internal val OUTLINES: Map<String, SupplyOutline> = linkedMapOf(
        "tape.torn" to TORN_TAPE,
        "fix.corner" to PHOTO_CORNER,
        "fix.staple" to STAPLE,
        "fix.clip" to PAPER_CLIP,
        "tape.masking" to MASKING_TAPE,
        "fix.stitch" to SADDLE_STITCH,
        "fix.grommet" to EYELET,
        "fix.pushpin" to PUSH_PIN,
    )
}
