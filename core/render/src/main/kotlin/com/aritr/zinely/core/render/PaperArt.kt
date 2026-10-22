package com.aritr.zinely.core.render

import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.render.ArtGeometry.closed
import com.aritr.zinely.core.render.ArtGeometry.p24

/**
 * The *Cut paper* family's outlines (moved verbatim out of [SupplyCatalog]). One object per family keeps each static
 * initialiser under the JVM's 64 KB cap (ADR-139); see `SupplyCatalogClinitHeadroomTest`.
 */
internal object PaperArt {

    /** How much of the window frame is border, per edge. */
    internal const val WINDOW_BORDER: Double = 0.14

    /**
     * A cut-out window: the full square with a smaller square removed.
     *
     * The one outline in the catalogue whose *point* is the hole, so it is also the one that would
     * break first if a backend ever dropped the even-odd fill rule — the second subpath would fill
     * solid and the window would become a rectangle. `SupplyOutlineRingTest` already pins that the
     * fill rule reaches the backend; this is the supply that would show it to a maker.
     */
    internal val WINDOW: SupplyOutline = SupplyOutline(
        closed(
            PtPoint(0.0, 0.0), PtPoint(1.0, 0.0), PtPoint(1.0, 1.0), PtPoint(0.0, 1.0),
        ).subpaths +
            closed(
                PtPoint(WINDOW_BORDER, WINDOW_BORDER),
                PtPoint(1.0 - WINDOW_BORDER, WINDOW_BORDER),
                PtPoint(1.0 - WINDOW_BORDER, 1.0 - WINDOW_BORDER),
                PtPoint(WINDOW_BORDER, 1.0 - WINDOW_BORDER),
            ).subpaths,
    )

    /**
     * A cut label with a spoken tail — §4's *"cut label/speech tag"*, drawn as the label rather than as
     * a comic balloon. One polygon: a rectangle across the top five-eighths, with a tail dropped from
     * the lower-left third. Kept angular because the family is **Cut paper**: this is scissors work,
     * not a drawn bubble.
     *
     * ⚠ **The tail's apex sits left of its own base, and that is what makes it read as a tail.** The
     * first draft put the apex at `x = 0.22` between a base of `0.20…0.42` — a near-isoceles spike
     * hanging straight down beside a wide empty notch, which reads as a torn corner, not as speech.
     * It passed every assertion in `SupplyCatalogTest`: the span was right, the area was right, and
     * being one polygon there was nothing to overlap. **It was caught by rendering it and looking.**
     * That is the whole argument for the pixel coverage this outline had none of.
     */
    internal val SPEECH_TAG: SupplyOutline = closed(
        PtPoint(0.0, 0.0), PtPoint(1.0, 0.0), PtPoint(1.0, 0.62), PtPoint(0.44, 0.62),
        PtPoint(0.10, 1.00), PtPoint(0.20, 0.62), PtPoint(0.0, 0.62),
    )

    /** Torn paper: the same irregular edge voice opened into a broad strip of material. */
    internal val TORN_STRIP: SupplyOutline = closed(
        PtPoint(0.00, 0.23), PtPoint(0.08, 0.18), PtPoint(0.18, 0.24),
        PtPoint(0.31, 0.16), PtPoint(0.43, 0.22), PtPoint(0.56, 0.17),
        PtPoint(0.70, 0.25), PtPoint(0.84, 0.18), PtPoint(1.00, 0.23),
        PtPoint(1.00, 0.76), PtPoint(0.88, 0.82), PtPoint(0.76, 0.75),
        PtPoint(0.63, 0.84), PtPoint(0.49, 0.78), PtPoint(0.34, 0.83),
        PtPoint(0.20, 0.75), PtPoint(0.08, 0.82), PtPoint(0.00, 0.77),
    )

    /** Marker underline: the tear rhythm reduced to a quick, uneven swipe across the middle. */
    internal val MARKER_UNDERLINE: SupplyOutline = closed(
        PtPoint(0.00, 0.46), PtPoint(0.13, 0.42), PtPoint(0.28, 0.48),
        PtPoint(0.46, 0.43), PtPoint(0.64, 0.47), PtPoint(0.81, 0.41),
        PtPoint(1.00, 0.45), PtPoint(1.00, 0.62), PtPoint(0.87, 0.67),
        PtPoint(0.73, 0.61), PtPoint(0.58, 0.67), PtPoint(0.41, 0.63),
        PtPoint(0.26, 0.68), PtPoint(0.12, 0.62), PtPoint(0.00, 0.67),
    )

    internal val TICKET_STUB: SupplyOutline = SupplyOutline(listOf(Subpath(
        start = p24(0, 3),
        segments = listOf(
            Segment.LineTo(p24(24, 3)), Segment.LineTo(p24(24, 9)),
            Segment.CubicTo(p24(20, 9), p24(20, 15), p24(24, 15)),
            Segment.LineTo(p24(24, 21)), Segment.LineTo(p24(0, 21)), Segment.LineTo(p24(0, 15)),
            Segment.CubicTo(p24(4, 15), p24(4, 9), p24(0, 9)),
        ),
    )))

    internal val POSTAGE_STAMP: SupplyOutline = closed(
        p24(2, 0), p24(4, 2), p24(6, 0), p24(8, 2), p24(10, 0), p24(12, 2),
        p24(14, 0), p24(16, 2), p24(18, 0), p24(20, 2), p24(22, 0), p24(24, 2),
        p24(22, 4), p24(24, 6), p24(22, 8), p24(24, 10), p24(22, 12), p24(24, 14),
        p24(22, 16), p24(24, 18), p24(22, 20), p24(24, 22), p24(22, 24), p24(20, 22),
        p24(18, 24), p24(16, 22), p24(14, 24), p24(12, 22), p24(10, 24), p24(8, 22),
        p24(6, 24), p24(4, 22), p24(2, 24), p24(0, 22), p24(2, 20), p24(0, 18),
        p24(2, 16), p24(0, 14), p24(2, 12), p24(0, 10), p24(2, 8), p24(0, 6),
        p24(2, 4), p24(0, 2),
    )

    internal val DECKLE_EDGE: SupplyOutline = closed(
        p24(1, 1), p24(5, 0), p24(8, 2), p24(12, 0), p24(16, 1), p24(20, 0),
        p24(24, 2), p24(23, 6), p24(24, 10), p24(22, 14), p24(24, 18), p24(23, 23),
        p24(19, 24), p24(15, 22), p24(11, 24), p24(7, 23), p24(3, 24), p24(0, 21),
        p24(2, 17), p24(0, 13), p24(1, 9), p24(0, 5),
    )

    internal val TORN_HOLE: SupplyOutline = SupplyOutline(
        closed(
            p24(2, 4), p24(7, 0), p24(12, 2), p24(17, 0), p24(23, 5), p24(22, 10),
            p24(24, 15), p24(20, 22), p24(14, 21), p24(9, 24), p24(3, 20), p24(1, 14), p24(0, 9),
        ).subpaths + closed(
            p24(7, 8), p24(6, 13), p24(9, 18), p24(14, 17), p24(18, 14), p24(17, 9), p24(13, 6),
        ).subpaths,
    )

    internal val FOLDED_CORNER: SupplyOutline = closed(
        p24(2, 2), p24(17, 2), p24(22, 7), p24(22, 22), p24(2, 22),
    )

    // =============================================================================================
    // Two hand-cut frames — v21-bench.html A32 (decision gate Q6, ADR-107 R1a).
    //
    // Both were authored from scratch as coordinates for Zinely, on the 24-unit grid. No reference art
    // was traced, opened or consulted, and they carry no third-party licence. Each is ONE outline: an
    // outer contour and one hole, straight segments, even-odd, nothing overlapping. The coordinates are
    // A32's, point for point — `BenchArtSheetParityTest` goes red if one is moved, smoothed or rounded.

    /** Snipped border: a thin border cut with scissors, three straight snips to a side. */
    internal val SNIPPED_BORDER: SupplyOutline = SupplyOutline(
        closed(
            p24(0.3, 0.6), p24(8.5, 0), p24(16, 0.5), p24(23.6, 0.2), p24(24, 7.5), p24(23.5, 15),
            p24(23.9, 23.5), p24(15.5, 24), p24(7, 23.6), p24(0.5, 23.9), p24(0, 16.5), p24(0.4, 8),
        ).subpaths + closed(
            p24(2.4, 2.6), p24(9, 2.2), p24(16.5, 2.5), p24(21.7, 2.3), p24(21.9, 8), p24(21.5, 15.5),
            p24(21.8, 21.6), p24(15, 21.9), p24(7.5, 21.6), p24(2.5, 21.8), p24(2.2, 16), p24(2.5, 8.5),
        ).subpaths,
    )

    /** Round window: a square of paper with a round hole of fourteen snips. */
    internal val ROUND_WINDOW: SupplyOutline = SupplyOutline(
        closed(
            p24(0, 0.5), p24(23.6, 0), p24(24, 23.4), p24(0.6, 24),
        ).subpaths + closed(
            p24(20, 12.2), p24(19.2, 15.5), p24(17.4, 17.9), p24(14.6, 19.6), p24(11.6, 19.9),
            p24(6.1, 17.3), p24(4.4, 14.6), p24(4.1, 11.6), p24(4.8, 8.6), p24(6.6, 6.1),
            p24(9.4, 4.6), p24(12.4, 4.1), p24(17.8, 6.5), p24(19.4, 9.1),
        ).subpaths,
    )

    /** This family's outlines keyed by `supplyId`, in `Copy.Supplies.BY_FAMILY` order. */
    internal val OUTLINES: Map<String, SupplyOutline> = linkedMapOf(
        "paper.strip" to TORN_STRIP,
        "paper.window" to WINDOW,
        "paper.tag" to SPEECH_TAG,
        "paper.underline" to MARKER_UNDERLINE,
        "paper.stub" to TICKET_STUB,
        "paper.stamp" to POSTAGE_STAMP,
        "paper.deckle" to DECKLE_EDGE,
        "paper.hole" to TORN_HOLE,
        "paper.dogear" to FOLDED_CORNER,
        "paper.border" to SNIPPED_BORDER,
        "paper.round" to ROUND_WINDOW,
    )
}
