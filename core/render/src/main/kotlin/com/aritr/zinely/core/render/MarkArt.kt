package com.aritr.zinely.core.render

import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.render.ArtGeometry.bar24
import com.aritr.zinely.core.render.ArtGeometry.bar
import com.aritr.zinely.core.render.ArtGeometry.circle
import com.aritr.zinely.core.render.ArtGeometry.closed
import com.aritr.zinely.core.render.ArtGeometry.p24

/**
 * The *Stamps & marks* family's outlines (moved verbatim out of [SupplyCatalog]). One object per family keeps each static
 * initialiser under the JVM's 64 KB cap (ADR-139); see `SupplyCatalogClinitHeadroomTest`.
 */
internal object MarkArt {

    // =============================================================================================
    // The derivable eight — SUPPLIES-SPEC §4.3 / ADR-107 R4
    //
    // §10.1 recorded the remaining twelve as "gated on O-B/O-C". **That was stale**: §0 closes all four
    // escalated calls and says so in as many words ("Nothing here is left open"). What actually gated
    // them was a designer's hand, which is a resource, not a ruling — and *Cut shapes* already proved a
    // quarter of the set was not design work at all. These eight are the same finding applied again.
    //
    // ⚠ **Even-odd is the binding constraint on every outline below, and it is the one that bites.**
    // §4.1 rule 3 fills even-odd, so two overlapping subpaths cancel to a HOLE rather than uniting.
    // Nothing here may be drawn as overlapping parts: a plus is ONE twelve-point polygon, never two
    // crossed bars; the registration arms stop short of the ring rather than crossing it; the halftone
    // dots are spaced so no two touch. Rule 4 ("free of winding-dependent self-intersection") is
    // explicitly an authoring rule no assertion can see, so this is a review obligation, not a test.
    //
    // ### Attestation (§4.1, the definition of done)
    //
    // All eight were authored from scratch, in this file, by writing coordinates or by elementary
    // construction from a named centre and radius. **No reference art was traced, opened or consulted.**
    // The star's ten vertices are a regular 5/2 star polygon evaluated at r=0.5 and r=0.2; the halftone
    // grid is a fixed 4×4 lattice with a declared radius ramp. None of it is procedural in the §5 sense
    // — every constant is written down here and identical on every device, for every maker, forever.

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

    /**
     * The supply §4 writes as *"star/asterisk"* and `Copy.Supplies` names **Star**.
     *
     * **The recipe, stated so it reproduces these constants:** ten vertices, alternating outer radius
     * 0.5 and inner radius 0.2 about the centre, one every 36° with the first at twelve o'clock; then
     * normalise so the **wide axis spans exactly 1.0**, and centre the result in the square. Rounded
     * to four places, which is what lands the extremes on exactly `0.0000` and `1.0000`.
     *
     * ⚠ **Ten vertices, not five — this is deliberately not a `{5/2}` star polygon.** A `{5/2}` is five
     * points joined every second one; it self-intersects, and under an even-odd fill its core pentagon
     * renders **hollow**. What is authored here is the simple ten-vertex concave decagon that traces a
     * pentagram's *outline*, which fills solid under any winding rule. Anyone who "simplifies" this
     * back to five points ships a hollow star, and no assertion in `SupplyCatalogTest` would see it.
     *
     * ⚠ The normalisation is not decoration either. A star inscribed in a circle does not fill its
     * square — the points at 4 and 8 o'clock stop short of the top and bottom — so an unnormalised
     * star lands ~5 % smaller than the maker's box in every direction, with nothing on screen to
     * explain why. `SupplyCatalogTest` pins this for every entry; the star is what discovered it.
     *
     *
     * ⚠ The name and the id disagree by design: this is `mark.asterisk`, and `Copy.Supplies.NAMES`
     * wins ([D-083](../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-083-ruling)). Do not "fix"
     * the id to match the drawn name; saved documents carry it.
     */
    internal val STAR: SupplyOutline = closed(
        PtPoint(0.5000, 0.0245), PtPoint(0.6236, 0.3801), PtPoint(1.0000, 0.3877),
        PtPoint(0.7000, 0.6152), PtPoint(0.8090, 0.9755), PtPoint(0.5000, 0.7605),
        PtPoint(0.1910, 0.9755), PtPoint(0.3000, 0.6152), PtPoint(0.0000, 0.3877),
        PtPoint(0.3764, 0.3801),
    )

    /**
     * An arrow pointing right, as one seven-point polygon: a shaft across the middle third and a head
     * that reaches the full height. Right rather than up because the maker has **rotate**, and a
     * horizontal arrow is the one that reads correctly under a 0° landing (§5.1).
     */
    internal val ARROW: SupplyOutline = closed(
        PtPoint(0.0, 0.34), PtPoint(0.58, 0.34), PtPoint(0.58, 0.06), PtPoint(1.0, 0.50),
        PtPoint(0.58, 0.94), PtPoint(0.58, 0.66), PtPoint(0.0, 0.66),
    )

    internal val POINTING_HAND: SupplyOutline = closed(
        p24(0, 9), p24(10, 9), p24(10, 5), p24(13, 5), p24(13, 8), p24(16, 6),
        p24(18, 8), p24(21, 8), p24(24, 11), p24(21, 14), p24(18, 14), p24(16, 16),
        p24(13, 14), p24(13, 18), p24(10, 18), p24(10, 14), p24(0, 14),
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

    internal val COLOUR_BAR: SupplyOutline = SupplyOutline(
        bar24(0, 4, 5, 20) + bar24(6.5, 4, 11.5, 20) +
            bar24(13, 4, 18, 20) + bar24(19.5, 4, 24, 20),
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

    /** This family's outlines keyed by `supplyId`, in `Copy.Supplies.BY_FAMILY` order. */
    internal val OUTLINES: Map<String, SupplyOutline> = linkedMapOf(
        "mark.asterisk" to STAR,
        "mark.arrow" to ARROW,
        "mark.halftone" to HALFTONE,
        "mark.registration" to REGISTRATION,
        "mark.hand" to POINTING_HAND,
        "mark.crop" to CROP_MARKS,
        "mark.bar" to COLOUR_BAR,
        "mark.scan" to COPIER_STREAK,
        "mark.perf" to PERFORATION,
        "mark.burst" to STARBURST,
    )
}
