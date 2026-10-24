package com.aritr.zinely.core.render

import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.render.ArtGeometry.closed
import com.aritr.zinely.core.render.ArtGeometry.p24
import com.aritr.zinely.core.render.ArtGeometry.path

/**
 * The *Cut paper* family's outlines: the 44 of v21-bench.html A39 ([ADR-139]), in the sheet's order.
 * The family is split across this object and [PaperArtPart2] (from Deckle edge, rough on) so each static
 * initialiser stays well under the JVM's 64 KB cap (R-A5); see `SupplyCatalogClinitHeadroomTest`.
 * [OUTLINES] holds both.
 *
 * Eight shipped drawings are kept as the owner picked them (Window frame, Ticket stub, Postage stamp,
 * Deckle edge, Torn hole, Folded corner, Snipped border, Round window) and stay in their original form
 * below. Every other outline is transcribed from its A39 row with [path], in the row's own 24-unit
 * numbers; `BenchArtSheetParityTest` holds each one to its row. Three of them redraw a shipped id (Torn
 * strip, Speech tag, Marker underline), each inside that id's old ink box (Never #19,
 * `SupplyRedrawBoundsTest`). Provenance per piece is `SupplyProvenanceCatalog`.
 *
 * [ADR-139]: ../../../../../../../../docs/DECISIONS.md#adr-139
 */
internal object PaperArt {

    // =============================================================================================
    // Kept as shipped (ADR-107).

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

    // =============================================================================================
    // Transcribed from v21-bench.html A39 (ART_A39), in sheet order; Deckle edge, rough on is in PaperArtPart2.

    /** Torn strip (`paper.strip`): A39, drawn in-house, redraws the shipped drawing. */
    internal val TORN_STRIP: SupplyOutline = path {
        m(0.82, 6.32); l(1.48, 6.35); l(2.0, 6.94); l(2.84, 6.84); l(4.01, 6.83); l(4.47, 6.3); l(5.25, 6.0)
        l(6.29, 6.52); l(6.87, 6.5); l(7.78, 7.03); l(9.08, 6.81); l(9.8, 6.51); l(10.32, 6.32); l(11.42, 6.67)
        l(12.07, 6.15); l(12.92, 5.91); l(13.37, 6.04); l(14.61, 6.26); l(15.39, 5.86); l(15.97, 5.71)
        l(16.95, 5.5); l(17.66, 5.96); l(18.83, 5.78); l(19.35, 5.65); l(20.2, 5.14); l(20.72, 5.29); l(21.95, 5.25)
        l(22.67, 6.06); l(23.28, 6.24); c(23.45, 6.29, 23.5, 6.39, 23.5, 6.53); l(23.5, 17.43)
        c(23.5, 17.58, 23.4, 17.71, 23.23, 17.73); l(22.33, 17.83); l(21.55, 18.03); l(20.96, 17.93)
        l(19.93, 17.13); l(19.08, 17.13); l(18.56, 17.42); l(17.33, 18.58); l(16.68, 18.87); l(15.77, 18.48)
        l(15.31, 18.96); l(14.21, 18.63); l(13.49, 18.38); l(12.9, 17.91); l(11.47, 18.45); l(10.82, 18.78)
        l(9.85, 18.76); l(9.33, 17.93); l(8.48, 17.94); l(7.31, 18.01); l(6.86, 18.69); l(6.08, 19.21)
        l(5.04, 18.39); l(4.45, 18.29); l(3.54, 17.52); l(2.24, 17.58); l(1.53, 17.95); l(1.01, 17.9)
        l(0.69, 17.77); c(0.59, 17.73, 0.5, 17.62, 0.5, 17.5); l(0.5, 6.62); c(0.5, 6.44, 0.62, 6.31, 0.82, 6.32)
        z()
    }

    /** Wavy torn strip (`paper.stripb`): A34, drawn in-house. */
    internal val PAPER_STRIPB: SupplyOutline = path {
        m(0.6, 6.4); l(1.52, 7.01); l(2.86, 5.07); l(3.48, 5.76); l(5.2, 6.38); l(6.18, 5.7); l(7.71, 6.46)
        l(8.47, 5.25); l(10.48, 5.73); l(11.57, 5.19); l(12.27, 5.12); l(14.11, 6.65); l(15.27, 5.27)
        l(16.19, 5.84); l(17.76, 4.97); l(19.1, 6.11); l(19.87, 5.18); l(21.8, 6.22); l(22.77, 5.01); l(23.4, 5.7)
        l(23.5, 18.0); l(22.51, 18.45); l(21.08, 17.05); l(20.39, 17.58); l(18.71, 18.59); l(17.59, 17.6)
        l(16.7, 18.13); l(14.47, 16.83); l(13.5, 18.21); l(11.99, 17.89); l(11.21, 18.41); l(9.88, 17.4)
        l(8.1, 18.02); l(7.38, 17.21); l(6.21, 18.66); l(4.6, 18.4); l(3.7, 18.12); l(2.31, 18.59); l(0.5, 18.5)
        z()
    }

    /** Torn edge strip (`paper.stripc`): A39, drawn in-house. */
    internal val PAPER_STRIPC: SupplyOutline = path {
        m(0.8, 7.2); l(23.2, 7.2); c(23.37, 7.2, 23.5, 7.33, 23.5, 7.5); l(23.5, 16.56)
        c(23.5, 16.7, 23.44, 16.82, 23.27, 16.86); l(22.98, 16.93); l(21.75, 18.15); l(21.09, 18.33)
        l(20.19, 17.95); l(19.73, 18.39); l(18.62, 18.01); l(17.91, 17.79); l(17.32, 17.32); l(15.89, 17.82)
        l(15.24, 18.06); l(14.27, 18.04); l(13.75, 17.24); l(12.9, 17.28); l(11.73, 17.28); l(11.28, 17.93)
        l(10.5, 18.35); l(9.46, 17.57); l(8.88, 17.49); l(7.96, 16.72); l(6.66, 16.79); l(5.95, 17.14)
        l(5.43, 17.31); l(4.32, 16.69); l(3.68, 17.21); l(2.83, 17.44); l(2.38, 17.15); l(1.14, 16.73)
        l(0.83, 16.77); c(0.67, 16.79, 0.5, 16.64, 0.5, 16.47); l(0.5, 7.5); c(0.5, 7.33, 0.63, 7.2, 0.8, 7.2); z()
    }

    /** Window frame, cut (`paper.windowb`): A34, drawn in-house. */
    internal val PAPER_WINDOWB: SupplyOutline = path {
        m(0.5, 0.7); l(12.2, 0.5); l(23.4, 0.6); l(23.5, 12.3); l(23.3, 23.4); l(11.8, 23.5); l(0.6, 23.3)
        l(0.7, 11.6); z()
        m(5.6, 18.5); l(18.5, 18.3); l(18.4, 5.5); l(5.5, 5.7); z()
    }

    /** Narrow frame (`paper.windowc`): A39, drawn in-house. */
    internal val PAPER_WINDOWC: SupplyOutline = path {
        m(1.8, 1.5); l(22.2, 1.5); c(22.37, 1.5, 22.5, 1.63, 22.5, 1.8); l(22.5, 22.2)
        c(22.5, 22.37, 22.37, 22.5, 22.2, 22.5); l(1.8, 22.5); c(1.63, 22.5, 1.5, 22.37, 1.5, 22.2); l(1.5, 1.8)
        c(1.5, 1.63, 1.63, 1.5, 1.8, 1.5); z()
        m(6.5, 6.5); l(6.5, 16.5); l(17.5, 16.5); l(17.5, 6.5); z()
    }

    /** Hand-cut window (`paper.cutwindow`): Doodle Icons by Khushmeen Sidhu, imported. */
    internal val PAPER_CUTWINDOW: SupplyOutline = path {
        m(1.82, 12.2); l(1.99, 8.55); l(2.02, 7.56); l(2.05, 6.66); l(2.06, 6.4); l(2.09, 5.49)
        c(2.1, 5.31, 2.1, 5.15, 2.11, 4.95); l(2.14, 4.42); l(2.17, 4.08); c(2.23, 3.42, 2.59, 2.92, 3.16, 2.77)
        c(3.35, 2.72, 3.63, 2.65, 3.77, 2.61); c(4.19, 2.49, 4.45, 2.45, 4.78, 2.43)
        c(6.27, 2.34, 7.88, 2.28, 10.04, 2.22); l(11.16, 2.19); l(12.36, 2.15); l(13.57, 2.12)
        c(14.48, 2.1, 15.29, 2.1, 16.01, 2.12); c(16.64, 2.14, 17.21, 2.16, 17.91, 2.2)
        c(18.22, 2.22, 18.5, 2.25, 18.84, 2.28); l(19.77, 2.36); l(20.84, 2.45)
        c(21.87, 2.54, 22.54, 3.3, 22.5, 4.34); l(22.47, 5.03); l(22.32, 9.26); l(22.15, 13.18); l(22.0, 16.57)
        l(21.94, 17.72); l(21.9, 18.53); c(21.86, 19.24, 21.89, 19.62, 21.75, 20.13)
        c(21.58, 20.75, 21.16, 21.16, 20.53, 21.3); c(20.3, 21.35, 20.08, 21.38, 19.76, 21.38)
        c(18.99, 21.38, 18.13, 21.4, 17.17, 21.42); l(14.57, 21.49); l(13.61, 21.52); l(10.3, 21.64); l(10.0, 21.65)
        l(4.67, 21.86); l(3.84, 21.89); c(3.47, 21.9, 3.01, 21.89, 2.66, 21.77)
        c(1.9, 21.51, 1.48, 20.87, 1.5, 19.93); c(1.51, 19.37, 1.53, 18.75, 1.56, 18.09); l(1.63, 16.25)
        l(1.84, 12.44); c(1.84, 12.36, 1.82, 12.28, 1.82, 12.2); z()
        m(18.79, 18.37); l(18.81, 18.11); c(18.85, 17.64, 18.88, 17.06, 18.92, 16.36)
        c(19.0, 15.01, 19.08, 13.46, 19.15, 11.71); c(19.22, 9.96, 19.26, 8.46, 19.29, 7.21)
        c(19.3, 6.58, 19.3, 6.07, 19.3, 5.68); c(19.23, 5.68, 19.15, 5.67, 19.09, 5.67); l(17.72, 5.58)
        l(16.91, 5.52); c(16.61, 5.5, 16.35, 5.48, 16.11, 5.47); c(15.53, 5.44, 15.01, 5.43, 14.55, 5.43)
        c(13.79, 5.43, 12.95, 5.45, 12.0, 5.48); l(9.44, 5.57); l(7.38, 5.64); l(4.95, 5.73); l(4.94, 6.01)
        l(4.66, 18.82); z()
    }

    /** Speech tag (`paper.tag`): A39, drawn in-house, redraws the shipped drawing. */
    internal val SPEECH_TAG: SupplyOutline = path {
        m(1.8, 2.0); l(22.2, 2.0); c(22.37, 2.0, 22.5, 2.13, 22.5, 2.3); l(22.5, 15.7)
        c(22.5, 15.87, 22.37, 16.0, 22.2, 16.0); l(11.6, 16.0); l(3.4, 22.6); l(5.4, 16.0); l(1.8, 16.0)
        c(1.63, 16.0, 1.5, 15.87, 1.5, 15.7); l(1.5, 2.3); c(1.5, 2.13, 1.63, 2.0, 1.8, 2.0); z()
    }

    /** Speech tag, cut (`paper.tagb`): A34, drawn in-house. */
    internal val PAPER_TAGB: SupplyOutline = path {
        m(0.6, 1.2); l(12.3, 0.8); l(23.4, 1.0); l(23.5, 16.4); l(11.4, 16.6); l(4.6, 23.3); l(6.3, 16.7)
        l(0.5, 16.5); z()
    }

    /** Square balloon (`paper.chat`): Doodle Icons by Oreo Design, imported. */
    internal val PAPER_CHAT: SupplyOutline = path {
        m(2.47, 3.35); c(5.57, 3.01, 8.93, 2.84, 12.3, 2.85); c(15.52, 2.86, 18.66, 3.03, 21.42, 3.35)
        c(21.94, 3.41, 22.39, 3.84, 22.49, 4.35); c(22.84, 6.08, 23.01, 8.0, 23.0, 10.09)
        c(22.99, 11.93, 22.84, 13.82, 22.56, 15.55); c(22.48, 16.07, 22.03, 16.51, 21.5, 16.58)
        c(19.2, 16.89, 16.66, 17.05, 13.89, 17.08); c(11.74, 17.1, 9.54, 17.04, 7.29, 16.89)
        c(6.05, 18.44, 4.85, 19.71, 3.54, 20.85); c(3.14, 21.19, 2.51, 21.24, 2.07, 20.96)
        c(1.69, 20.72, 1.46, 20.28, 1.49, 19.83); c(1.6, 18.02, 1.52, 16.6, 1.37, 15.53)
        c(1.11, 13.78, 0.99, 11.9, 1.0, 9.89); c(1.01, 8.01, 1.13, 6.18, 1.37, 4.42)
        c(1.44, 3.88, 1.92, 3.41, 2.47, 3.35); z()
    }

    /**
     * Marker underline (`paper.underline`): A39, drawn in-house, redraws the shipped drawing, fitted ×0.919, move +0, +0.443.
     */
    internal val MARKER_UNDERLINE: SupplyOutline = path {
        m(3.45, 12.94); c(3.58, 12.94, 3.71, 12.94, 3.84, 12.94); c(9.91, 12.94, 15.94, 11.9, 21.66, 9.86)
        c(21.84, 9.79, 22.02, 9.92, 22.02, 10.11); c(22.02, 10.15, 22.05, 10.13, 22.0, 10.23); l(20.28, 13.92)
        c(20.23, 14.02, 20.18, 14.05, 20.11, 14.07); c(15.17, 15.3, 10.18, 16.32, 5.09, 16.32)
        c(4.1, 16.32, 3.13, 16.29, 2.15, 16.23); c(2.01, 16.23, 1.89, 16.11, 1.89, 15.96)
        c(1.89, 15.91, 1.87, 15.94, 1.92, 15.84); l(3.19, 13.1); c(3.25, 12.98, 3.34, 12.94, 3.45, 12.94); z()
    }

    /** Round underline (`paper.underlineb`): A34, drawn in-house. */
    internal val PAPER_UNDERLINEB: SupplyOutline = path {
        m(1.9, 10.4); c(7.0, 8.4, 15.0, 11.6, 22.2, 9.5); c(23.4, 9.3, 23.5, 12.8, 22.3, 13.0)
        c(15.0, 15.2, 7.5, 12.2, 1.7, 14.2); c(0.6, 14.5, 0.6, 10.6, 1.9, 10.4); z()
    }

    /** Torn-end stub (`paper.stubb`): A34, drawn in-house. */
    internal val PAPER_STUBB: SupplyOutline = path {
        m(0.6, 4.2); l(21.6, 3.9); l(21.84, 4.99); l(21.46, 5.7); l(21.6, 7.3); l(21.86, 8.49); l(21.77, 10.6)
        l(21.65, 11.5); l(22.45, 12.88); l(21.97, 13.49); l(21.44, 15.21); l(21.86, 16.2); l(21.73, 17.7)
        l(22.41, 18.48); l(22.0, 20.1); l(0.5, 19.9); l(0.5, 15.0); c(2.16, 15.0, 3.5, 13.66, 3.5, 12.0)
        c(3.5, 10.34, 2.16, 9.0, 0.5, 9.0); l(0.6, 4.2); z()
    }

    /** Torn-off stub (`paper.stubc`): A39, drawn in-house. */
    internal val PAPER_STUBC: SupplyOutline = path {
        m(2.52, 5.25); l(14.82, 5.25); c(14.97, 5.25, 15.1, 5.36, 15.12, 5.51)
        c(15.2, 6.08, 15.52, 6.57, 16.0, 6.87); c(16.08, 6.92, 16.15, 6.88, 16.14, 7.13); l(16.13, 7.49)
        l(15.85, 8.45); l(15.89, 9.23); l(15.92, 9.71); l(16.2, 10.85); l(16.2, 11.45); l(16.11, 12.29)
        l(16.24, 12.71); l(16.1, 13.73); l(16.07, 14.39); l(15.93, 14.93); l(16.12, 16.25); l(16.14, 16.87)
        c(16.15, 17.1, 16.09, 17.08, 16.0, 17.13); c(15.51, 17.4, 15.2, 17.93, 15.12, 18.49)
        c(15.1, 18.63, 14.97, 18.75, 14.82, 18.75); l(2.52, 18.75); c(2.38, 18.75, 2.25, 18.64, 2.23, 18.5)
        c(2.14, 17.87, 1.63, 17.38, 1.0, 17.27); c(0.83, 17.24, 0.75, 17.12, 0.75, 16.98); l(0.75, 7.02)
        c(0.75, 6.88, 0.86, 6.75, 1.0, 6.73); c(1.63, 6.64, 2.12, 6.13, 2.23, 5.5)
        c(2.26, 5.33, 2.38, 5.25, 2.52, 5.25); z()
        m(19.18, 5.25); l(21.48, 5.25); c(21.62, 5.25, 21.75, 5.36, 21.77, 5.5)
        c(21.86, 6.13, 22.37, 6.62, 23.0, 6.73); c(23.17, 6.76, 23.25, 6.88, 23.25, 7.02); l(23.25, 16.98)
        c(23.25, 17.12, 23.14, 17.25, 23.0, 17.27); c(22.37, 17.36, 21.88, 17.87, 21.77, 18.5)
        c(21.74, 18.67, 21.62, 18.75, 21.48, 18.75); l(19.18, 18.75); c(19.03, 18.75, 18.9, 18.64, 18.88, 18.49)
        c(18.8, 17.92, 18.48, 17.42, 17.99, 17.13); c(17.89, 17.07, 17.85, 17.11, 17.84, 16.88); l(17.82, 16.25)
        l(17.63, 14.93); l(17.77, 14.39); l(17.8, 13.73); l(17.94, 12.71); l(17.81, 12.29); l(17.9, 11.45)
        l(17.9, 10.85); l(17.62, 9.71); l(17.59, 9.23); l(17.55, 8.45); l(17.83, 7.49); l(17.84, 7.12)
        c(17.85, 6.86, 17.9, 6.93, 17.99, 6.87); c(18.47, 6.55, 18.8, 6.07, 18.88, 5.51)
        c(18.9, 5.37, 19.03, 5.25, 19.18, 5.25); z()
    }

    /** Entry ticket (`paper.ticket`): Fluent Emoji (High Contrast), imported. */
    internal val PAPER_TICKET: SupplyOutline = path {
        m(19.35, 4.1); c(20.23, 4.1, 21.04, 4.78, 21.18, 5.65); c(21.24, 6.04, 21.55, 6.34, 21.94, 6.39)
        c(22.8, 6.5, 23.5, 7.28, 23.5, 8.15); l(23.5, 9.25); c(23.5, 9.68, 23.35, 10.08, 23.08, 10.4)
        c(23.35, 10.72, 23.5, 11.13, 23.5, 11.55); l(23.5, 12.45); c(23.5, 12.88, 23.35, 13.29, 23.07, 13.61)
        c(23.34, 13.93, 23.49, 14.33, 23.49, 14.75); l(23.49, 15.88); c(23.49, 16.74, 22.8, 17.52, 21.95, 17.63)
        c(21.57, 17.68, 21.25, 17.97, 21.19, 18.35); c(21.05, 19.21, 20.25, 19.9, 19.37, 19.9); l(4.63, 19.9)
        c(3.75, 19.9, 2.96, 19.22, 2.81, 18.35); c(2.75, 17.98, 2.44, 17.68, 2.06, 17.63)
        c(1.2, 17.52, 0.5, 16.74, 0.5, 15.87); l(0.5, 14.75); c(0.5, 14.33, 0.65, 13.92, 0.92, 13.6)
        c(0.65, 13.28, 0.5, 12.87, 0.5, 12.45); l(0.5, 11.55); c(0.5, 11.13, 0.65, 10.72, 0.92, 10.4)
        c(0.65, 10.08, 0.5, 9.67, 0.5, 9.25); l(0.5, 8.14); c(0.5, 7.28, 1.19, 6.49, 2.04, 6.38)
        c(2.42, 6.33, 2.74, 6.05, 2.81, 5.67); c(2.96, 4.8, 3.75, 4.1, 4.63, 4.1); z()
    }

    /** Bitten stamp (`paper.stampb`): A34, drawn in-house. */
    internal val PAPER_STAMPB: SupplyOutline = path {
        m(1.0, 1.0); l(3.0, 1.0); c(3.0, 1.55, 3.45, 2.0, 4.0, 2.0); c(4.55, 2.0, 5.0, 1.55, 5.0, 1.0); l(7.0, 1.0)
        c(7.0, 1.55, 7.45, 2.0, 8.0, 2.0); c(8.55, 2.0, 9.0, 1.55, 9.0, 1.0); l(11.0, 1.0)
        c(11.0, 1.55, 11.45, 2.0, 12.0, 2.0); c(12.55, 2.0, 13.0, 1.55, 13.0, 1.0); l(15.0, 1.0)
        c(15.0, 1.55, 15.45, 2.0, 16.0, 2.0); c(16.55, 2.0, 17.0, 1.55, 17.0, 1.0); l(19.0, 1.0)
        c(19.0, 1.55, 19.45, 2.0, 20.0, 2.0); c(20.55, 2.0, 21.0, 1.55, 21.0, 1.0); l(23.0, 1.0); l(23.0, 3.0)
        c(22.45, 3.0, 22.0, 3.45, 22.0, 4.0); c(22.0, 4.55, 22.45, 5.0, 23.0, 5.0); l(23.0, 7.0)
        c(22.45, 7.0, 22.0, 7.45, 22.0, 8.0); c(22.0, 8.55, 22.45, 9.0, 23.0, 9.0); l(23.0, 11.0)
        c(22.45, 11.0, 22.0, 11.45, 22.0, 12.0); c(22.0, 12.55, 22.45, 13.0, 23.0, 13.0); l(23.0, 15.0)
        c(22.45, 15.0, 22.0, 15.45, 22.0, 16.0); c(22.0, 16.55, 22.45, 17.0, 23.0, 17.0); l(23.0, 19.0)
        c(22.45, 19.0, 22.0, 19.45, 22.0, 20.0); c(22.0, 20.55, 22.45, 21.0, 23.0, 21.0); l(23.0, 23.0)
        l(21.0, 23.0); c(21.0, 22.45, 20.55, 22.0, 20.0, 22.0); c(19.45, 22.0, 19.0, 22.45, 19.0, 23.0)
        l(17.0, 23.0); c(17.0, 22.45, 16.55, 22.0, 16.0, 22.0); c(15.45, 22.0, 15.0, 22.45, 15.0, 23.0)
        l(13.0, 23.0); c(13.0, 22.45, 12.55, 22.0, 12.0, 22.0); c(11.45, 22.0, 11.0, 22.45, 11.0, 23.0)
        l(9.0, 23.0); c(9.0, 22.45, 8.55, 22.0, 8.0, 22.0); c(7.45, 22.0, 7.0, 22.45, 7.0, 23.0); l(5.0, 23.0)
        c(5.0, 22.45, 4.55, 22.0, 4.0, 22.0); c(3.45, 22.0, 3.0, 22.45, 3.0, 23.0); l(1.0, 23.0); l(1.0, 21.0)
        c(1.55, 21.0, 2.0, 20.55, 2.0, 20.0); c(2.0, 19.45, 1.55, 19.0, 1.0, 19.0); l(1.0, 17.0)
        c(1.55, 17.0, 2.0, 16.55, 2.0, 16.0); c(2.0, 15.45, 1.55, 15.0, 1.0, 15.0); l(1.0, 13.0)
        c(1.55, 13.0, 2.0, 12.55, 2.0, 12.0); c(2.0, 11.45, 1.55, 11.0, 1.0, 11.0); l(1.0, 9.0)
        c(1.55, 9.0, 2.0, 8.55, 2.0, 8.0); c(2.0, 7.45, 1.55, 7.0, 1.0, 7.0); l(1.0, 5.0)
        c(1.55, 5.0, 2.0, 4.55, 2.0, 4.0); c(2.0, 3.45, 1.55, 3.0, 1.0, 3.0); z()
    }

    /** Corner bites (`paper.stampc`): A39, drawn in-house. */
    internal val PAPER_STAMPC: SupplyOutline = path {
        m(3.44, 2.27); c(3.85, 2.18, 4.18, 1.85, 4.27, 1.44); c(4.3, 1.3, 4.43, 1.2, 4.57, 1.2); l(6.23, 1.2)
        c(6.37, 1.2, 6.5, 1.3, 6.53, 1.44); c(6.64, 1.94, 7.08, 2.3, 7.6, 2.3); c(8.12, 2.3, 8.56, 1.94, 8.67, 1.44)
        c(8.7, 1.3, 8.83, 1.2, 8.97, 1.2); l(10.63, 1.2); c(10.77, 1.2, 10.9, 1.3, 10.93, 1.44)
        c(11.04, 1.94, 11.48, 2.3, 12.0, 2.3); c(12.52, 2.3, 12.96, 1.94, 13.07, 1.44)
        c(13.1, 1.3, 13.23, 1.2, 13.37, 1.2); l(15.03, 1.2); c(15.17, 1.2, 15.3, 1.3, 15.33, 1.44)
        c(15.44, 1.94, 15.88, 2.3, 16.4, 2.3); c(16.92, 2.3, 17.36, 1.94, 17.47, 1.44)
        c(17.5, 1.3, 17.63, 1.2, 17.77, 1.2); l(19.43, 1.2); c(19.57, 1.2, 19.7, 1.3, 19.73, 1.44)
        c(19.82, 1.85, 20.15, 2.18, 20.56, 2.27); c(20.7, 2.3, 20.8, 2.43, 20.8, 2.57); l(20.8, 4.15)
        c(20.8, 4.29, 20.7, 4.42, 20.56, 4.45); c(20.06, 4.56, 19.7, 5.0, 19.7, 5.52)
        c(19.7, 6.04, 20.06, 6.48, 20.56, 6.59); c(20.7, 6.62, 20.8, 6.75, 20.8, 6.89); l(20.8, 8.47)
        c(20.8, 8.61, 20.7, 8.74, 20.56, 8.77); c(20.06, 8.88, 19.7, 9.32, 19.7, 9.84)
        c(19.7, 10.36, 20.06, 10.8, 20.56, 10.91); c(20.7, 10.94, 20.8, 11.07, 20.8, 11.21); l(20.8, 12.79)
        c(20.8, 12.93, 20.7, 13.06, 20.56, 13.09); c(20.06, 13.2, 19.7, 13.64, 19.7, 14.16)
        c(19.7, 14.68, 20.06, 15.12, 20.56, 15.23); c(20.7, 15.26, 20.8, 15.39, 20.8, 15.53); l(20.8, 17.11)
        c(20.8, 17.25, 20.7, 17.38, 20.56, 17.41); c(20.06, 17.52, 19.7, 17.96, 19.7, 18.48)
        c(19.7, 19.0, 20.06, 19.44, 20.56, 19.55); c(20.7, 19.58, 20.8, 19.71, 20.8, 19.85); l(20.8, 21.43)
        c(20.8, 21.57, 20.7, 21.7, 20.56, 21.73); c(20.15, 21.82, 19.82, 22.15, 19.73, 22.56)
        c(19.7, 22.7, 19.57, 22.8, 19.43, 22.8); l(17.77, 22.8); c(17.63, 22.8, 17.5, 22.7, 17.47, 22.56)
        c(17.36, 22.06, 16.92, 21.7, 16.4, 21.7); c(15.88, 21.7, 15.44, 22.06, 15.33, 22.56)
        c(15.3, 22.7, 15.17, 22.8, 15.03, 22.8); l(13.37, 22.8); c(13.23, 22.8, 13.1, 22.7, 13.07, 22.56)
        c(12.96, 22.06, 12.52, 21.7, 12.0, 21.7); c(11.48, 21.7, 11.04, 22.06, 10.93, 22.56)
        c(10.9, 22.7, 10.77, 22.8, 10.63, 22.8); l(8.97, 22.8); c(8.83, 22.8, 8.7, 22.7, 8.67, 22.56)
        c(8.56, 22.06, 8.12, 21.7, 7.6, 21.7); c(7.08, 21.7, 6.64, 22.06, 6.53, 22.56)
        c(6.5, 22.7, 6.37, 22.8, 6.23, 22.8); l(4.57, 22.8); c(4.43, 22.8, 4.3, 22.7, 4.27, 22.56)
        c(4.18, 22.15, 3.85, 21.82, 3.44, 21.73); c(3.3, 21.7, 3.2, 21.57, 3.2, 21.43); l(3.2, 19.85)
        c(3.2, 19.71, 3.3, 19.58, 3.44, 19.55); c(3.94, 19.44, 4.3, 19.0, 4.3, 18.48)
        c(4.3, 17.96, 3.94, 17.52, 3.44, 17.41); c(3.3, 17.38, 3.2, 17.25, 3.2, 17.11); l(3.2, 15.53)
        c(3.2, 15.39, 3.3, 15.26, 3.44, 15.23); c(3.94, 15.12, 4.3, 14.68, 4.3, 14.16)
        c(4.3, 13.64, 3.94, 13.2, 3.44, 13.09); c(3.3, 13.06, 3.2, 12.93, 3.2, 12.79); l(3.2, 11.21)
        c(3.2, 11.07, 3.3, 10.94, 3.44, 10.91); c(3.94, 10.8, 4.3, 10.36, 4.3, 9.84)
        c(4.3, 9.32, 3.94, 8.88, 3.44, 8.77); c(3.3, 8.74, 3.2, 8.61, 3.2, 8.47); l(3.2, 6.89)
        c(3.2, 6.75, 3.3, 6.62, 3.44, 6.59); c(3.94, 6.48, 4.3, 6.04, 4.3, 5.52)
        c(4.3, 5.0, 3.94, 4.56, 3.44, 4.45); c(3.3, 4.42, 3.2, 4.29, 3.2, 4.15); l(3.2, 2.57)
        c(3.2, 2.43, 3.3, 2.3, 3.44, 2.27); z()
    }

    /** This family's outlines keyed by `supplyId`, in `Copy.Supplies.BY_FAMILY` order (v21-bench.html A39). */
    internal val OUTLINES: Map<String, SupplyOutline> = linkedMapOf(
        "paper.strip" to TORN_STRIP,
        "paper.stripb" to PAPER_STRIPB,
        "paper.stripc" to PAPER_STRIPC,
        "paper.window" to WINDOW,
        "paper.windowb" to PAPER_WINDOWB,
        "paper.windowc" to PAPER_WINDOWC,
        "paper.cutwindow" to PAPER_CUTWINDOW,
        "paper.tag" to SPEECH_TAG,
        "paper.tagb" to PAPER_TAGB,
        "paper.chat" to PAPER_CHAT,
        "paper.underline" to MARKER_UNDERLINE,
        "paper.underlineb" to PAPER_UNDERLINEB,
        "paper.stub" to TICKET_STUB,
        "paper.stubb" to PAPER_STUBB,
        "paper.stubc" to PAPER_STUBC,
        "paper.ticket" to PAPER_TICKET,
        "paper.stamp" to POSTAGE_STAMP,
        "paper.stampb" to PAPER_STAMPB,
        "paper.stampc" to PAPER_STAMPC,
        "paper.deckle" to DECKLE_EDGE,
        "paper.deckleb" to PaperArtPart2.PAPER_DECKLEB,
        "paper.decklec" to PaperArtPart2.PAPER_DECKLEC,
        "paper.hole" to TORN_HOLE,
        "paper.holeb" to PaperArtPart2.PAPER_HOLEB,
        "paper.holec" to PaperArtPart2.PAPER_HOLEC,
        "paper.holed" to PaperArtPart2.PAPER_HOLED,
        "paper.dogear" to FOLDED_CORNER,
        "paper.dogearb" to PaperArtPart2.PAPER_DOGEARB,
        "paper.dogearc" to PaperArtPart2.PAPER_DOGEARC,
        "paper.page" to PaperArtPart2.PAPER_PAGE,
        "paper.sticky" to PaperArtPart2.PAPER_STICKY,
        "paper.border" to SNIPPED_BORDER,
        "paper.borderb" to PaperArtPart2.PAPER_BORDERB,
        "paper.borderc" to PaperArtPart2.PAPER_BORDERC,
        "paper.borderd" to PaperArtPart2.PAPER_BORDERD,
        "paper.round" to ROUND_WINDOW,
        "paper.roundb" to PaperArtPart2.PAPER_ROUNDB,
        "paper.roundc" to PaperArtPart2.PAPER_ROUNDC,
        "paper.balloon" to PaperArtPart2.PAPER_BALLOON,
        "paper.thought" to PaperArtPart2.PAPER_THOUGHT,
        "paper.shout" to PaperArtPart2.PAPER_SHOUT,
        "paper.bookmark" to PaperArtPart2.PAPER_BOOKMARK,
        "paper.ribbon" to PaperArtPart2.PAPER_RIBBON,
        "paper.gifttag" to PaperArtPart2.PAPER_GIFTTAG,
    )
}
