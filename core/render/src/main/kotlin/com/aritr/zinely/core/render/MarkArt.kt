package com.aritr.zinely.core.render

import com.aritr.zinely.core.render.ArtGeometry.bar24
import com.aritr.zinely.core.render.ArtGeometry.bar
import com.aritr.zinely.core.render.ArtGeometry.circle
import com.aritr.zinely.core.render.ArtGeometry.closed
import com.aritr.zinely.core.render.ArtGeometry.p24
import com.aritr.zinely.core.render.ArtGeometry.path

/**
 * The *Stamps & marks* family's outlines: the 38 of v21-bench.html A39 ([ADR-139]), in the sheet's order.
 * The family is split across this object and [MarkArtPart2] (from Colour bar on) so each static initialiser
 * stays well under the JVM's 64 KB cap (R-A5); see `SupplyCatalogClinitHeadroomTest`. [OUTLINES] holds both.
 *
 * Six shipped drawings are kept as the owner picked them (Halftone, Crosshair, Crop marks, Copier streak,
 * Tear line, Starburst) and stay in their original form below. Every other outline is transcribed from its
 * A39 row with [path], in the row's own 24-unit numbers; `BenchArtSheetParityTest` holds each one to its row.
 * Four of them redraw a shipped id (Star, Arrow, Pointing hand, Colour bar), each inside that id's old ink
 * box (Never #19, `SupplyRedrawBoundsTest`). Provenance per piece is `SupplyProvenanceCatalog`.
 *
 * [ADR-139]: ../../../../../../../../docs/DECISIONS.md#adr-139
 */
internal object MarkArt {

    // =============================================================================================
    // Kept as shipped (SUPPLIES-SPEC §4.3 / ADR-107 R4)
    //
    // The family's other four first drawings (Star, Arrow, Pointing hand, Colour bar) are now A39 redraws, below.
    //
    // ⚠ **Even-odd is the binding constraint on every outline below, and it is the one that bites.**
    // §4.1 rule 3 fills even-odd, so two overlapping subpaths cancel to a HOLE rather than uniting.
    // Nothing here may be drawn as overlapping parts: the registration arms stop short of the ring rather
    // than crossing it; the halftone dots are spaced so no two touch. `SupplyCatalogTest` samples every
    // outline for even-odd = non-zero (same-direction rings declared).
    //
    // ### Attestation (§4.1, the definition of done)
    //
    // These six were authored from scratch, in this file, by writing coordinates or by elementary
    // construction from a named centre and radius. **No reference art was traced, opened or consulted.**
    // The halftone grid is a fixed 4×4 lattice with a declared radius ramp. None of it is procedural in the
    // §5 sense — every constant is written down here and identical on every device, for every maker, forever.

    /** Half the thickness of a registration arm, as a fraction of the unit square. */
    internal const val REG_ARM_HALF: Double = 0.035

    /** Where a registration arm stops — on the ring's outer edge, so the two meet without crossing. */
    internal const val REG_RING_OUTER: Double = 0.25

    internal const val REG_RING_INNER: Double = 0.19

    /**
     * The printer's registration target: a four-armed crosshair with a centred ring — one of the two
     * supplies §4 calls out as naming the **process** rather than pointing at anything.
     *
     * ⚠ **The arms stop where the ring begins, so nothing overlaps.** The obvious drawing — a full-width
     * plus laid across a ring — is wrong here: under even-odd every crossing punches a hole exactly where
     * the mark is densest. So each arm runs from its edge inward and stops on [REG_RING_OUTER]; the ring
     * is a proper annulus (a second, smaller subpath is the hole); and the centre stays empty. Meeting
     * rather than crossing is geometry, not styling.
     *
     * ⚠ **How this revision came about, recorded so the A5 question is auditable rather than asserted.**
     * The ring was widened *after* comparing the rendered outline against the Art sheet's tile glyph in a
     * golden diff. That is the trigger, and stating it is the point: an A5 finding whose prompt is
     * unrecorded cannot be re-examined by anyone but its author. The glyph was used as a **check that the
     * mark reads as the thing it names**, never as the source of a coordinate — which is the line A5 draws.
     * A reader who thinks that line was crossed has, in this sentence, everything needed to say so.
     *
     * ⚠ **This was deliberately not a copy of the tile glyph, and the divergence was required — until the
     * glyph stopped existing.** A5 ruled that `BenchArtGlyphs` *depicted* the supplies and "must not become
     * their source"; that glyph was a 24-unit **stroked** icon whose crosshair ran straight through its
     * circle — a drawing this renderer cannot make, because [DrawShape] is fill-only and a crossing cancels
     * under even-odd. **The glyph set was deleted on 2026-08-20 (amendment A7, [D-093]) and the tile now
     * renders this outline**, so the divergence this paragraph defends is gone: there is no second drawing
     * left to diverge from. The reasoning is kept because it is *why* the mark is authored as it is — the
     * arms stop at the ring because even-odd would cancel a crossing, not because a glyph once disagreed. The first draft
     * here answered that by shrinking the ring to a 0.085 dot, which was even-odd-safe and read as a
     * crosshair with a speck rather than as a registration target. The ring is the *subject* of the mark,
     * so it takes a quarter of the box and the arms yield to it.
     */
    internal val REGISTRATION: SupplyOutline = SupplyOutline(
        bar(0.0, 0.5 - REG_ARM_HALF, 0.5 - REG_RING_OUTER, 0.5 + REG_ARM_HALF) +
            bar(0.5 + REG_RING_OUTER, 0.5 - REG_ARM_HALF, 1.0, 0.5 + REG_ARM_HALF) +
            bar(0.5 - REG_ARM_HALF, 0.0, 0.5 + REG_ARM_HALF, 0.5 - REG_RING_OUTER) +
            bar(0.5 - REG_ARM_HALF, 0.5 + REG_RING_OUTER, 0.5 + REG_ARM_HALF, 1.0) +
            circle(0.5, 0.5, REG_RING_OUTER) +
            circle(0.5, 0.5, REG_RING_INNER),
    )

    /** The largest halftone dot's radius — also its first centre, so its edge sits on the box. */
    internal const val HALFTONE_R_MAX: Double = 0.11

    /** How much each step along the lattice diagonal takes off the radius. */
    internal const val HALFTONE_R_STEP: Double = 0.012

    /**
     * The lattice pitch, **derived** so the far column's outer edge lands exactly on 1.0.
     *
     * Three gaps of [HALFTONE_PITCH] separate four columns; the first centre sits at [HALFTONE_R_MAX]
     * and the last dot on the top row has radius `HALFTONE_R_MAX - 3 * HALFTONE_R_STEP`. Writing the
     * arithmetic instead of the number means editing the ramp cannot silently un-fill the square.
     */
    internal const val HALFTONE_PITCH: Double =
        (1.0 - HALFTONE_R_MAX - (HALFTONE_R_MAX - 3 * HALFTONE_R_STEP)) / 3.0

    /**
     * The authored tone ramp: densest at the top-left, thinning along the diagonal.
     *
     * ⚠ **Guarded, because this is the one constant here that fails silently.** A ramp steep enough to
     * drive the far corner to zero or below still produces in-square points and still passes
     * `polygonArea() > 1e-6` — the shoelace takes an absolute value, so an inside-out dot measures the
     * same as a right-way-out one. The result would be an invisible or inverted corner dot that every
     * assertion blesses. [HALFTONE_PITCH] is derived so it cannot go quietly wrong; this makes the ramp
     * match.
     */
    internal fun halftoneRadius(diagonal: Int): Double =
        (HALFTONE_R_MAX - HALFTONE_R_STEP * diagonal).also {
            require(it > 0.0) { "halftone ramp is too steep: cell $diagonal has radius $it" }
        }

    /**
     * A halftone cell — the second **process** mark, and the one whose whole meaning is that a printed
     * grey is not grey but a lattice of solid dots at varying size.
     *
     * A fixed 4×4 lattice with the radius ramping down along the diagonal, so the cluster reads as a
     * tone gradient rather than a polka dot. **The ramp is authored, not computed from a hash or a
     * seed** — §5 bans procedural variation of an outline, and four authored tears beating infinite
     * generated ones is the same argument as one authored screen beating infinite generated ones.
     * Every constant is written down, so this lattice is identical on every device forever.
     *
     * Two invariants hold it together, and [HALFTONE_PITCH] is derived from the first rather than
     * guessed at: the biggest dot's outer edge sits on x=0 and the far column's outer edge on x=1
     * (so the mark fills its box), while the pitch stays wider than twice the largest radius (so no
     * two dots touch, which even-odd requires — a kissing pair would cancel to a notch).
     *
     * ⚠ **Open, and deliberately not settled here: the tile glyph draws a scatter, this draws a lattice.**
     * ⚠ The Art sheet's glyph for this mark was **seven** circles at irregular centres while the mark itself
     * is **sixteen** on a lattice. That was `BenchArtGlyphs`, deleted 2026-08-20: the tile renders this
     * outline now, so the disagreement is not fixed but *unrepresentable*. The reasoning it rested on — A5 permits
     * the divergence — the glyph *depicts* the supply and "must not become their source" — but a maker taps
     * a loose cluster and gets a regular grid, and only a first-time reader can say whether that reads as a
     * broken promise. The lattice is kept because **a halftone screen is a lattice**: that regularity is
     * the whole content of the mark, and an irregular scatter is stippling, which is a different process.
     * The counter-argument is real and belongs to Pass 2, not to this file — §5 wants marks that feel
     * authored rather than procedurally generic, and a perfect gradient lattice is the most generated-
     * looking thing in the catalogue. Booked as a reading, not answered by a constant.
     */
    internal val HALFTONE: SupplyOutline = SupplyOutline(
        (0 until 4).flatMap { row ->
            (0 until 4).flatMap { col ->
                circle(
                    cx = HALFTONE_R_MAX + col * HALFTONE_PITCH,
                    cy = HALFTONE_R_MAX + row * HALFTONE_PITCH,
                    r = halftoneRadius(row + col),
                )
            }
        },
    )

    internal val CROP_MARKS: SupplyOutline = SupplyOutline(
        closed(
            p24(0, 0), p24(9, 0), p24(9, 2), p24(2, 2), p24(2, 9), p24(0, 9),
        ).subpaths + closed(
            p24(15, 0), p24(24, 0), p24(24, 9), p24(22, 9), p24(22, 2), p24(15, 2),
        ).subpaths + closed(
            p24(0, 15), p24(2, 15), p24(2, 22), p24(9, 22), p24(9, 24), p24(0, 24),
        ).subpaths + closed(
            p24(22, 15), p24(24, 15), p24(24, 24), p24(15, 24), p24(15, 22), p24(22, 22),
        ).subpaths,
    )

    internal val COPIER_STREAK: SupplyOutline = SupplyOutline(
        closed(p24(2, 0), p24(5, 0), p24(4, 24), p24(0, 24)).subpaths +
            closed(p24(9, 0), p24(11, 0), p24(13, 24), p24(8, 24)).subpaths +
            closed(p24(17, 0), p24(22, 0), p24(24, 24), p24(19, 24)).subpaths,
    )

    internal val PERFORATION: SupplyOutline = SupplyOutline(
        bar24(0, 10, 3, 14) + bar24(5, 10, 8, 14) + bar24(10, 10, 13, 14) +
            bar24(15, 10, 18, 14) + bar24(20, 10, 24, 14),
    )

    internal val STARBURST: SupplyOutline = closed(
        p24(12, 0), p24(14, 7), p24(19, 2), p24(18, 9), p24(24, 8), p24(19, 12),
        p24(24, 16), p24(18, 15), p24(19, 22), p24(14, 17), p24(12, 24), p24(10, 17),
        p24(5, 22), p24(6, 15), p24(0, 16), p24(5, 12), p24(0, 8), p24(6, 9),
        p24(5, 2), p24(10, 7),
    )

    // =============================================================================================
    // Transcribed from v21-bench.html A39 (ART_A39), in sheet order; Colour bar on is in MarkArtPart2.

    /** Star (`mark.asterisk`): A39, drawn in-house, redraws the shipped drawing, fitted move +0, +0.04. */
    internal val STAR: SupplyOutline = path {
        m(12.0, 0.59); l(14.93, 8.43); l(23.3, 8.8); l(16.75, 14.01); l(18.98, 22.08); l(12.0, 17.46)
        l(5.02, 22.08); l(7.25, 14.01); l(0.7, 8.8); l(9.07, 8.43); z()
    }

    /** Star, hand-cut (`mark.asteriskb`): Doodle Icons by Khushmeen Sidhu, imported. */
    internal val MARK_ASTERISKB: SupplyOutline = path {
        m(11.89, 17.36); l(4.77, 22.56); c(4.59, 22.69, 4.46, 22.87, 4.3, 23.01)
        c(4.15, 23.13, 4.02, 23.18, 3.89, 23.18); c(3.68, 23.18, 3.49, 23.02, 3.46, 22.71); l(3.44, 22.47)
        l(3.49, 22.11); c(3.62, 21.21, 3.96, 20.34, 4.27, 19.48); l(6.21, 14.11)
        c(4.55, 12.6, 2.98, 11.06, 1.46, 9.47); c(1.25, 9.25, 1.03, 9.03, 0.94, 8.74); l(0.86, 8.47)
        c(0.81, 8.3, 1.0, 8.15, 1.26, 8.13); l(2.35, 8.06); l(6.62, 8.11); l(8.23, 8.05); l(11.11, 0.79)
        c(11.19, 0.59, 11.36, 0.5, 11.52, 0.5); c(11.66, 0.5, 11.82, 0.56, 11.91, 0.73); l(12.16, 1.22)
        l(13.74, 5.11); c(14.05, 5.87, 14.46, 6.58, 14.83, 7.33); l(20.5, 7.22); l(22.5, 7.24)
        c(22.75, 7.24, 23.02, 7.29, 23.08, 7.55); l(23.14, 7.8); l(23.12, 7.92)
        c(23.1, 8.04, 23.03, 8.15, 22.93, 8.24); l(21.37, 9.71); l(17.08, 13.48); l(16.69, 13.85); l(16.75, 14.08)
        c(17.4, 16.58, 18.56, 18.88, 19.03, 21.41); l(19.23, 22.49); l(19.24, 22.66)
        c(19.27, 23.09, 19.03, 23.21, 18.53, 23.25); l(18.26, 23.27); c(17.63, 23.32, 17.22, 22.9, 16.78, 22.51)
        c(15.47, 21.34, 14.2, 20.14, 13.07, 18.79); z()
    }

    /** Asterisk (`mark.asteriskc`): Tabler Icons, imported. */
    internal val MARK_ASTERISKC: SupplyOutline = path {
        m(13.25, 2.0); l(13.25, 9.86); l(20.28, 5.91); c(20.45, 5.81, 20.65, 5.76, 20.86, 5.76)
        c(21.3, 5.76, 21.75, 5.99, 21.98, 6.39); c(22.08, 6.56, 22.13, 6.77, 22.13, 6.97)
        c(22.13, 7.41, 21.9, 7.87, 21.5, 8.09); l(14.55, 12.0); l(21.5, 15.91)
        c(21.9, 16.13, 22.13, 16.59, 22.13, 17.03); c(22.13, 17.23, 22.08, 17.44, 21.98, 17.61)
        c(21.75, 18.01, 21.3, 18.24, 20.86, 18.24); c(20.65, 18.24, 20.45, 18.19, 20.28, 18.09); l(13.25, 14.14)
        l(13.25, 22.0); c(13.25, 22.66, 12.66, 23.25, 12.0, 23.25); c(11.35, 23.25, 10.75, 22.66, 10.75, 22.0)
        l(10.75, 14.14); l(3.72, 18.09); c(3.55, 18.19, 3.35, 18.24, 3.14, 18.24)
        c(2.7, 18.24, 2.25, 18.01, 2.02, 17.61); c(1.92, 17.44, 1.87, 17.23, 1.87, 17.03)
        c(1.87, 16.59, 2.1, 16.13, 2.5, 15.91); l(9.45, 12.0); l(2.5, 8.09); c(2.1, 7.87, 1.87, 7.41, 1.87, 6.97)
        c(1.87, 6.77, 1.92, 6.56, 2.02, 6.39); c(2.25, 5.99, 2.7, 5.76, 3.14, 5.76)
        c(3.35, 5.76, 3.55, 5.81, 3.72, 5.91); l(10.75, 9.86); l(10.75, 2.0)
        c(10.75, 1.34, 11.34, 0.75, 12.0, 0.75); c(12.66, 0.75, 13.25, 1.34, 13.25, 2.0); z()
    }

    /** Arrow (`mark.arrow`): A39, drawn in-house, redraws the shipped drawing. */
    internal val ARROW: SupplyOutline = path {
        m(1.75, 17.71); c(1.06, 17.71, 0.5, 17.15, 0.5, 16.46); c(0.5, 15.77, 1.06, 15.21, 1.75, 15.21)
        c(8.11, 15.21, 14.06, 13.02, 18.84, 8.97); l(14.45, 9.2); c(13.7, 9.24, 13.13, 8.67, 13.13, 7.95)
        c(13.13, 7.29, 13.62, 6.74, 14.32, 6.7); l(22.1, 6.29); c(22.86, 6.25, 23.46, 6.85, 23.42, 7.61)
        l(23.01, 15.4); c(22.98, 16.03, 22.43, 16.58, 21.76, 16.58); c(21.05, 16.58, 20.47, 16.01, 20.51, 15.27)
        l(20.76, 10.62); c(15.52, 15.13, 8.67, 17.71, 1.75, 17.71); z()
    }

    /** Arrow, pen (`mark.arrowb`): A34, drawn in-house. */
    internal val MARK_ARROWB: SupplyOutline = path {
        m(1.3, 14.3); c(5.5, 14.2, 10.0, 11.9, 14.4, 10.34); l(11.92, 5.72); l(22.78, 9.54); l(15.28, 18.28)
        l(15.12, 13.04); c(10.6, 14.6, 5.8, 16.9, 1.3, 16.9); c(0.6, 16.9, 0.6, 14.3, 1.3, 14.3); z()
    }

    /** Block arrow (`mark.arrowc`): A39, drawn in-house. */
    internal val MARK_ARROWC: SupplyOutline = path {
        m(0.5, 8.55); c(0.5, 8.38, 0.63, 8.25, 0.8, 8.25); l(11.6, 8.25); l(11.6, 2.26)
        c(11.6, 2.08, 11.75, 1.96, 11.9, 1.96); c(11.97, 1.96, 12.02, 1.96, 12.1, 2.03); l(23.5, 12.0)
        l(12.1, 21.97); c(12.02, 22.04, 11.97, 22.04, 11.9, 22.04); c(11.75, 22.04, 11.6, 21.92, 11.6, 21.74)
        l(11.6, 15.75); l(0.8, 15.75); c(0.63, 15.75, 0.5, 15.62, 0.5, 15.45); z()
    }

    /** Arrow, brushed (`mark.arrowd`): Doodle Icons by Khushmeen Sidhu, imported. */
    internal val MARK_ARROWD: SupplyOutline = path {
        m(10.94, 12.63); l(3.18, 12.56); c(2.74, 12.56, 2.2, 12.52, 1.58, 12.46)
        c(1.04, 12.4, 0.52, 11.95, 0.6, 11.45); l(0.64, 11.2); c(0.7, 10.83, 1.08, 10.54, 1.45, 10.48)
        l(2.03, 10.39); l(5.33, 10.21); l(12.06, 10.16); l(17.44, 10.18); l(17.27, 10.09)
        c(16.57, 9.72, 15.76, 9.36, 14.93, 8.86); l(12.7, 7.51); l(12.25, 7.19)
        c(11.97, 6.99, 11.8, 6.62, 11.78, 6.38); l(11.77, 6.22); c(11.74, 5.77, 12.14, 5.45, 12.56, 5.38)
        l(12.81, 5.34); c(12.99, 5.31, 13.17, 5.39, 13.34, 5.47); l(14.45, 6.01); l(22.09, 10.0)
        c(22.88, 10.41, 23.28, 10.75, 23.35, 11.23); l(23.4, 11.58); c(23.47, 12.07, 23.14, 12.6, 22.51, 13.16)
        l(21.43, 14.12); l(18.96, 16.04); l(16.37, 18.02); c(16.12, 18.21, 15.83, 18.38, 15.54, 18.53)
        c(15.39, 18.61, 15.23, 18.65, 15.06, 18.65); c(14.82, 18.65, 14.58, 18.57, 14.35, 18.4)
        c(14.06, 18.18, 13.8, 17.73, 13.87, 17.35); l(13.93, 17.02); c(14.0, 16.62, 14.3, 16.31, 14.74, 15.93)
        l(18.69, 12.56); l(11.13, 12.6); z()
    }

    /** Arrow, wobbly (`mark.arrowe`): Doodle Icons by Oreo Design, imported. */
    internal val MARK_ARROWE: SupplyOutline = path {
        m(1.78, 10.75); c(5.06, 10.75, 8.38, 10.89, 11.75, 10.86); l(19.29, 10.8); l(18.25, 9.8); l(15.02, 6.76)
        c(14.77, 6.52, 14.64, 6.18, 14.64, 5.84); c(14.64, 5.53, 14.75, 5.22, 14.96, 4.99)
        c(15.2, 4.74, 15.54, 4.61, 15.88, 4.61); c(16.19, 4.61, 16.5, 4.72, 16.73, 4.93); l(19.98, 7.99)
        l(23.15, 11.13); c(23.38, 11.35, 23.48, 11.68, 23.48, 12.0); c(23.48, 12.32, 23.38, 12.65, 23.15, 12.87)
        l(19.98, 16.01); l(16.73, 19.07); c(16.5, 19.28, 16.19, 19.39, 15.88, 19.39)
        c(15.54, 19.39, 15.2, 19.26, 14.96, 19.01); c(14.75, 18.78, 14.64, 18.47, 14.64, 18.16)
        c(14.64, 17.82, 14.77, 17.48, 15.02, 17.24); l(19.19, 13.3); l(11.76, 13.36)
        c(8.37, 13.39, 5.02, 13.25, 1.72, 13.25); c(1.07, 13.25, 0.5, 12.63, 0.5, 11.97)
        c(0.5, 11.32, 1.12, 10.75, 1.78, 10.75); z()
    }

    /** Halftone screen (`mark.halftoneb`): A39, drawn in-house. */
    internal val MARK_HALFTONEB: SupplyOutline = path {
        m(5.08, 3.19); c(5.08, 4.24, 4.24, 5.08, 3.19, 5.08); c(2.15, 5.08, 1.3, 4.24, 1.3, 3.19)
        c(1.3, 2.15, 2.15, 1.3, 3.19, 1.3); c(4.24, 1.3, 5.08, 2.15, 5.08, 3.19); z()
        m(8.72, 6.95); c(8.72, 7.93, 7.93, 8.72, 6.95, 8.72); c(5.98, 8.72, 5.19, 7.93, 5.19, 6.95)
        c(5.19, 5.98, 5.98, 5.19, 6.95, 5.19); c(7.93, 5.19, 8.72, 5.98, 8.72, 6.95); z()
        m(12.48, 3.19); c(12.48, 4.17, 11.69, 4.96, 10.71, 4.96); c(9.74, 4.96, 8.95, 4.17, 8.95, 3.19)
        c(8.95, 2.22, 9.74, 1.43, 10.71, 1.43); c(11.69, 1.43, 12.48, 2.22, 12.48, 3.19); z()
        m(4.96, 10.71); c(4.96, 11.69, 4.17, 12.48, 3.19, 12.48); c(2.22, 12.48, 1.43, 11.69, 1.43, 10.71)
        c(1.43, 9.74, 2.22, 8.95, 3.19, 8.95); c(4.17, 8.95, 4.96, 9.74, 4.96, 10.71); z()
        m(8.59, 14.47); c(8.59, 15.38, 7.86, 16.11, 6.95, 16.11); c(6.05, 16.11, 5.32, 15.38, 5.32, 14.47)
        c(5.32, 13.57, 6.05, 12.84, 6.95, 12.84); c(7.86, 12.84, 8.59, 13.57, 8.59, 14.47); z()
        m(19.87, 3.19); c(19.87, 4.1, 19.14, 4.83, 18.23, 4.83); c(17.33, 4.83, 16.6, 4.1, 16.6, 3.19)
        c(16.6, 2.29, 17.33, 1.56, 18.23, 1.56); c(19.14, 1.56, 19.87, 2.29, 19.87, 3.19); z()
        m(12.35, 10.71); c(12.35, 11.62, 11.62, 12.35, 10.71, 12.35); c(9.81, 12.35, 9.08, 11.62, 9.08, 10.71)
        c(9.08, 9.81, 9.81, 9.08, 10.71, 9.08); c(11.62, 9.08, 12.35, 9.81, 12.35, 10.71); z()
        m(16.11, 6.95); c(16.11, 7.86, 15.38, 8.59, 14.47, 8.59); c(13.57, 8.59, 12.84, 7.86, 12.84, 6.95)
        c(12.84, 6.05, 13.57, 5.32, 14.47, 5.32); c(15.38, 5.32, 16.11, 6.05, 16.11, 6.95); z()
        m(4.83, 18.23); c(4.83, 19.14, 4.1, 19.87, 3.19, 19.87); c(2.29, 19.87, 1.56, 19.14, 1.56, 18.23)
        c(1.56, 17.33, 2.29, 16.6, 3.19, 16.6); c(4.1, 16.6, 4.83, 17.33, 4.83, 18.23); z()
        m(12.22, 18.23); c(12.22, 19.07, 11.55, 19.74, 10.71, 19.74); c(9.88, 19.74, 9.21, 19.07, 9.21, 18.23)
        c(9.21, 17.4, 9.88, 16.73, 10.71, 16.73); c(11.55, 16.73, 12.22, 17.4, 12.22, 18.23); z()
        m(23.5, 6.95); c(23.5, 7.79, 22.83, 8.46, 21.99, 8.46); c(21.16, 8.46, 20.49, 7.79, 20.49, 6.95)
        c(20.49, 6.12, 21.16, 5.45, 21.99, 5.45); c(22.83, 5.45, 23.5, 6.12, 23.5, 6.95); z()
        m(19.74, 10.71); c(19.74, 11.55, 19.07, 12.22, 18.23, 12.22); c(17.4, 12.22, 16.73, 11.55, 16.73, 10.71)
        c(16.73, 9.88, 17.4, 9.21, 18.23, 9.21); c(19.07, 9.21, 19.74, 9.88, 19.74, 10.71); z()
        m(15.98, 14.47); c(15.98, 15.31, 15.31, 15.98, 14.47, 15.98); c(13.64, 15.98, 12.97, 15.31, 12.97, 14.47)
        c(12.97, 13.64, 13.64, 12.97, 14.47, 12.97); c(15.31, 12.97, 15.98, 13.64, 15.98, 14.47); z()
        m(8.46, 21.99); c(8.46, 22.83, 7.79, 23.5, 6.95, 23.5); c(6.12, 23.5, 5.45, 22.83, 5.45, 21.99)
        c(5.45, 21.16, 6.12, 20.49, 6.95, 20.49); c(7.79, 20.49, 8.46, 21.16, 8.46, 21.99); z()
        m(23.37, 14.47); c(23.37, 15.24, 22.76, 15.85, 21.99, 15.85); c(21.23, 15.85, 20.62, 15.24, 20.62, 14.47)
        c(20.62, 13.71, 21.23, 13.1, 21.99, 13.1); c(22.76, 13.1, 23.37, 13.71, 23.37, 14.47); z()
        m(19.61, 18.23); c(19.61, 19.0, 19.0, 19.61, 18.23, 19.61); c(17.47, 19.61, 16.86, 19.0, 16.86, 18.23)
        c(16.86, 17.47, 17.47, 16.86, 18.23, 16.86); c(19.0, 16.86, 19.61, 17.47, 19.61, 18.23); z()
        m(15.85, 21.99); c(15.85, 22.76, 15.24, 23.37, 14.47, 23.37); c(13.71, 23.37, 13.1, 22.76, 13.1, 21.99)
        c(13.1, 21.23, 13.71, 20.62, 14.47, 20.62); c(15.24, 20.62, 15.85, 21.23, 15.85, 21.99); z()
        m(23.24, 21.99); c(23.24, 22.68, 22.68, 23.24, 21.99, 23.24); c(21.3, 23.24, 20.74, 22.68, 20.74, 21.99)
        c(20.74, 21.3, 21.3, 20.74, 21.99, 20.74); c(22.68, 20.74, 23.24, 21.3, 23.24, 21.99); z()
    }

    /** Bold crosshair (`mark.registrationb`): A34, drawn in-house. */
    internal val MARK_REGISTRATIONB: SupplyOutline = path {
        m(10.75, 0.75); l(13.25, 0.75); l(13.25, 4.86); c(16.26, 5.39, 18.61, 7.74, 19.14, 10.75); l(23.25, 10.75)
        l(23.25, 13.25); l(19.14, 13.25); c(18.61, 16.26, 16.26, 18.61, 13.25, 19.14); l(13.25, 23.25)
        l(10.75, 23.25); l(10.75, 19.14); c(7.74, 18.61, 5.39, 16.26, 4.86, 13.25); l(0.75, 13.25); l(0.75, 10.75)
        l(4.86, 10.75); c(5.39, 7.74, 7.74, 5.39, 10.75, 4.86); z()
        m(16.58, 10.75); c(16.14, 9.13, 14.87, 7.86, 13.25, 7.42); l(13.25, 10.75); z()
        m(13.25, 13.25); l(13.25, 16.58); c(14.87, 16.14, 16.14, 14.87, 16.58, 13.25); z()
        m(7.42, 13.25); c(7.86, 14.87, 9.13, 16.14, 10.75, 16.58); l(10.75, 13.25); z()
        m(10.75, 10.75); l(10.75, 7.42); c(9.13, 7.86, 7.86, 9.13, 7.42, 10.75); z()
    }

    /** Fine crosshair (`mark.registrationc`): A39, drawn in-house. */
    internal val MARK_REGISTRATIONC: SupplyOutline = path {
        m(0.5, 11.55); c(0.5, 11.38, 0.63, 11.25, 0.8, 11.25); l(3.53, 11.25)
        c(3.89, 7.15, 7.15, 3.89, 11.25, 3.53); l(11.25, 0.8); c(11.25, 0.63, 11.38, 0.5, 11.55, 0.5); l(12.45, 0.5)
        c(12.62, 0.5, 12.75, 0.63, 12.75, 0.8); l(12.75, 3.53); c(16.85, 3.89, 20.11, 7.15, 20.47, 11.25)
        l(23.2, 11.25); c(23.37, 11.25, 23.5, 11.38, 23.5, 11.55); l(23.5, 12.45)
        c(23.5, 12.62, 23.37, 12.75, 23.2, 12.75); l(20.47, 12.75); c(20.11, 16.85, 16.85, 20.11, 12.75, 20.47)
        l(12.75, 23.2); c(12.75, 23.37, 12.62, 23.5, 12.45, 23.5); l(11.55, 23.5)
        c(11.38, 23.5, 11.25, 23.37, 11.25, 23.2); l(11.25, 20.47); c(7.15, 20.11, 3.89, 16.85, 3.53, 12.75)
        l(0.8, 12.75); c(0.63, 12.75, 0.5, 12.62, 0.5, 12.45); z()
        m(11.25, 5.04); c(7.98, 5.39, 5.39, 7.98, 5.04, 11.25); l(11.25, 11.25); z()
        m(12.75, 18.96); c(16.02, 18.61, 18.61, 16.02, 18.96, 12.75); l(12.75, 12.75); z()
        m(12.75, 11.25); l(18.96, 11.25); c(18.61, 7.98, 16.02, 5.39, 12.75, 5.04); z()
        m(11.25, 12.75); l(5.04, 12.75); c(5.39, 16.02, 7.98, 18.61, 11.25, 18.96); z()
    }

    /**
     * Pointing hand (`mark.hand`): A34, drawn in-house, redraws the shipped drawing, fitted ×0.965, move +0, -1.835.
     */
    internal val POINTING_HAND: SupplyOutline = path {
        m(0.99, 5.0); l(4.37, 5.29); l(4.27, 17.64); l(0.89, 17.35); z()
        m(6.3, 6.35); c(8.13, 5.39, 10.54, 5.29, 12.57, 5.58); l(21.06, 5.68)
        c(22.99, 5.68, 22.99, 9.34, 21.06, 9.34); l(14.88, 9.25); c(16.72, 9.34, 16.91, 12.14, 15.08, 12.24)
        c(16.62, 12.43, 16.62, 14.94, 14.88, 15.04); c(16.04, 15.23, 15.95, 17.55, 14.31, 17.64)
        c(11.51, 18.22, 8.52, 18.12, 6.3, 17.16); z()
    }

    /** Cuffed hand (`mark.handb`): A39, drawn in-house. */
    internal val MARK_HANDB: SupplyOutline = path {
        m(0.75, 4.05); c(0.75, 3.88, 0.88, 3.75, 1.05, 3.75); l(4.7, 3.75); c(4.87, 3.75, 5.0, 3.88, 5.0, 4.05)
        l(5.0, 6.1); c(7.29, 5.08, 9.53, 4.4, 10.6, 4.4); l(21.4, 4.4); c(22.28, 4.4, 23.0, 5.12, 23.0, 6.0)
        c(23.0, 6.88, 22.28, 7.6, 21.4, 7.6); l(16.7, 7.6); c(17.65, 7.88, 18.3, 8.69, 18.3, 9.6)
        c(18.3, 10.76, 17.27, 11.7, 16.0, 11.7); c(15.88, 11.7, 15.76, 11.69, 15.64, 11.67)
        c(16.0, 11.97, 16.2, 12.4, 16.2, 12.85); c(16.2, 13.51, 15.77, 14.09, 15.13, 14.31)
        c(15.46, 14.6, 15.65, 15.02, 15.65, 15.45); c(15.65, 16.14, 15.17, 16.75, 14.48, 16.94)
        c(14.78, 17.22, 14.95, 17.6, 14.95, 18.0); c(14.95, 18.83, 14.23, 19.5, 13.35, 19.5)
        c(13.16, 19.5, 13.0, 19.48, 12.8, 19.41); c(10.29, 18.55, 7.65, 18.21, 5.0, 18.3); l(5.0, 19.95)
        c(5.0, 20.12, 4.87, 20.25, 4.7, 20.25); l(1.05, 20.25); c(0.88, 20.25, 0.75, 20.12, 0.75, 19.95); z()
    }

    /** Drawn hand (`mark.handc`): Doodle Icons by Khushmeen Sidhu, imported. */
    internal val MARK_HANDC: SupplyOutline = path {
        m(3.42, 19.52); c(2.26, 18.73, 1.42, 17.57, 1.01, 16.22); c(0.76, 15.39, 0.61, 14.57, 0.61, 13.77)
        c(0.61, 12.07, 1.2, 10.45, 2.37, 8.91); c(2.6, 8.61, 2.88, 8.34, 3.21, 8.14)
        c(3.99, 7.66, 4.79, 7.19, 5.56, 6.79); l(6.55, 6.28); c(7.14, 5.97, 7.74, 5.69, 8.3, 5.32)
        c(8.8, 4.99, 9.22, 4.57, 9.66, 4.17); l(10.17, 3.7); l(10.76, 3.1); c(11.07, 2.78, 11.47, 2.67, 11.87, 2.67)
        c(12.11, 2.67, 12.36, 2.72, 12.62, 2.82); c(13.2, 3.04, 13.53, 3.52, 13.58, 4.24); l(13.61, 4.72)
        c(13.67, 5.69, 13.33, 6.66, 12.74, 7.44); l(12.48, 7.78); l(15.84, 7.72); l(20.41, 7.8); l(20.57, 7.79)
        l(21.18, 7.84); c(21.61, 7.88, 22.03, 7.97, 22.44, 8.11); c(22.99, 8.3, 23.38, 8.78, 23.38, 9.43)
        l(23.38, 9.56); c(23.38, 10.02, 23.12, 10.39, 22.6, 10.66); c(22.13, 10.9, 21.66, 11.13, 21.17, 11.21)
        c(20.16, 11.38, 19.12, 11.51, 18.12, 11.58); l(15.59, 11.76); c(15.85, 12.17, 16.08, 12.53, 16.04, 12.95)
        l(16.01, 13.23); c(15.95, 13.8, 15.53, 14.13, 15.13, 14.4); c(15.47, 14.98, 15.65, 15.46, 15.65, 15.89)
        c(15.65, 16.56, 15.23, 17.13, 14.33, 17.79); l(14.45, 18.06); c(14.55, 18.29, 14.69, 18.5, 14.71, 18.75)
        l(14.75, 19.15); c(14.83, 19.94, 14.29, 20.62, 13.52, 20.91); c(12.95, 21.13, 12.35, 21.29, 11.75, 21.31)
        l(10.95, 21.33); c(9.43, 21.37, 7.82, 21.11, 5.96, 20.66); c(5.05, 20.44, 4.19, 20.05, 3.42, 19.52); z()
    }

    /** Corner marks (`mark.cropb`): A34, drawn in-house. */
    internal val MARK_CROPB: SupplyOutline = path {
        m(0.5, 5.25); l(4.15, 5.25); l(4.15, 7.75); l(0.5, 7.75); z()
        m(5.25, 0.5); l(7.75, 0.5); l(7.75, 4.15); l(5.25, 4.15); z()
        m(19.85, 5.25); l(23.5, 5.25); l(23.5, 7.75); l(19.85, 7.75); z()
        m(16.25, 0.5); l(18.75, 0.5); l(18.75, 4.15); l(16.25, 4.15); z()
        m(0.5, 16.25); l(4.15, 16.25); l(4.15, 18.75); l(0.5, 18.75); z()
        m(5.25, 19.85); l(7.75, 19.85); l(7.75, 23.5); l(5.25, 23.5); z()
        m(19.85, 16.25); l(23.5, 16.25); l(23.5, 18.75); l(19.85, 18.75); z()
        m(16.25, 19.85); l(18.75, 19.85); l(18.75, 23.5); l(16.25, 23.5); z()
    }

    /** Long trim ticks (`mark.cropc`): A39, drawn in-house. */
    internal val MARK_CROPC: SupplyOutline = path {
        m(0.5, 7.8); c(0.5, 7.63, 0.63, 7.5, 0.8, 7.5); l(5.95, 7.5); c(6.12, 7.5, 6.25, 7.63, 6.25, 7.8)
        l(6.25, 8.7); c(6.25, 8.87, 6.12, 9.0, 5.95, 9.0); l(0.8, 9.0); c(0.63, 9.0, 0.5, 8.87, 0.5, 8.7); z()
        m(17.75, 7.8); c(17.75, 7.63, 17.88, 7.5, 18.05, 7.5); l(23.2, 7.5); c(23.37, 7.5, 23.5, 7.63, 23.5, 7.8)
        l(23.5, 8.7); c(23.5, 8.87, 23.37, 9.0, 23.2, 9.0); l(18.05, 9.0); c(17.88, 9.0, 17.75, 8.87, 17.75, 8.7)
        z()
        m(15.0, 0.8); c(15.0, 0.63, 15.13, 0.5, 15.3, 0.5); l(16.2, 0.5); c(16.37, 0.5, 16.5, 0.63, 16.5, 0.8)
        l(16.5, 5.95); c(16.5, 6.12, 16.37, 6.25, 16.2, 6.25); l(15.3, 6.25); c(15.13, 6.25, 15.0, 6.12, 15.0, 5.95)
        z()
        m(7.5, 0.8); c(7.5, 0.63, 7.63, 0.5, 7.8, 0.5); l(8.7, 0.5); c(8.87, 0.5, 9.0, 0.63, 9.0, 0.8); l(9.0, 5.95)
        c(9.0, 6.12, 8.87, 6.25, 8.7, 6.25); l(7.8, 6.25); c(7.63, 6.25, 7.5, 6.12, 7.5, 5.95); z()
        m(15.0, 18.05); c(15.0, 17.88, 15.13, 17.75, 15.3, 17.75); l(16.2, 17.75)
        c(16.37, 17.75, 16.5, 17.88, 16.5, 18.05); l(16.5, 23.2); c(16.5, 23.37, 16.37, 23.5, 16.2, 23.5)
        l(15.3, 23.5); c(15.13, 23.5, 15.0, 23.37, 15.0, 23.2); z()
        m(7.5, 18.05); c(7.5, 17.88, 7.63, 17.75, 7.8, 17.75); l(8.7, 17.75); c(8.87, 17.75, 9.0, 17.88, 9.0, 18.05)
        l(9.0, 23.2); c(9.0, 23.37, 8.87, 23.5, 8.7, 23.5); l(7.8, 23.5); c(7.63, 23.5, 7.5, 23.37, 7.5, 23.2); z()
        m(0.5, 15.3); c(0.5, 15.13, 0.63, 15.0, 0.8, 15.0); l(5.95, 15.0); c(6.12, 15.0, 6.25, 15.13, 6.25, 15.3)
        l(6.25, 16.2); c(6.25, 16.37, 6.12, 16.5, 5.95, 16.5); l(0.8, 16.5); c(0.63, 16.5, 0.5, 16.37, 0.5, 16.2)
        z()
        m(17.75, 15.3); c(17.75, 15.13, 17.88, 15.0, 18.05, 15.0); l(23.2, 15.0)
        c(23.37, 15.0, 23.5, 15.13, 23.5, 15.3); l(23.5, 16.2); c(23.5, 16.37, 23.37, 16.5, 23.2, 16.5)
        l(18.05, 16.5); c(17.88, 16.5, 17.75, 16.37, 17.75, 16.2); z()
    }

    /** This family's outlines keyed by `supplyId`, in `Copy.Supplies.BY_FAMILY` order (v21-bench.html A39). */
    internal val OUTLINES: Map<String, SupplyOutline> = linkedMapOf(
        "mark.asterisk" to STAR,
        "mark.asteriskb" to MARK_ASTERISKB,
        "mark.asteriskc" to MARK_ASTERISKC,
        "mark.arrow" to ARROW,
        "mark.arrowb" to MARK_ARROWB,
        "mark.arrowc" to MARK_ARROWC,
        "mark.arrowd" to MARK_ARROWD,
        "mark.arrowe" to MARK_ARROWE,
        "mark.halftone" to HALFTONE,
        "mark.halftoneb" to MARK_HALFTONEB,
        "mark.registration" to REGISTRATION,
        "mark.registrationb" to MARK_REGISTRATIONB,
        "mark.registrationc" to MARK_REGISTRATIONC,
        "mark.hand" to POINTING_HAND,
        "mark.handb" to MARK_HANDB,
        "mark.handc" to MARK_HANDC,
        "mark.crop" to CROP_MARKS,
        "mark.cropb" to MARK_CROPB,
        "mark.cropc" to MARK_CROPC,
        "mark.bar" to MarkArtPart2.COLOUR_BAR,
        "mark.barb" to MarkArtPart2.MARK_BARB,
        "mark.scan" to COPIER_STREAK,
        "mark.scanb" to MarkArtPart2.MARK_SCANB,
        "mark.scanc" to MarkArtPart2.MARK_SCANC,
        "mark.perf" to PERFORATION,
        "mark.perfb" to MarkArtPart2.MARK_PERFB,
        "mark.perfc" to MarkArtPart2.MARK_PERFC,
        "mark.burst" to STARBURST,
        "mark.burstb" to MarkArtPart2.MARK_BURSTB,
        "mark.burstc" to MarkArtPart2.MARK_BURSTC,
        "mark.burstd" to MarkArtPart2.MARK_BURSTD,
        "mark.sparkle" to MarkArtPart2.MARK_SPARKLE,
        "mark.cancel" to MarkArtPart2.MARK_CANCEL,
        "mark.heart" to MarkArtPart2.MARK_HEART,
        "mark.tick" to MarkArtPart2.MARK_TICK,
        "mark.cross" to MarkArtPart2.MARK_CROSS,
        "mark.scissors" to MarkArtPart2.MARK_SCISSORS,
        "mark.squiggle" to MarkArtPart2.MARK_SQUIGGLE,
    )
}
