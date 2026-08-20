package com.aritr.zinely.core.render

import com.aritr.zinely.core.model.PtPoint

/**
 * The drawer: `supplyId` → the authored outline that draws it (SUPPLIES-SPEC §3.4, §4.1).
 *
 * Outlines are **code, not data**. A supply needs no I/O to resolve — unlike [DrawImage]'s `assetId`,
 * which stays unresolved on the tape because resolving it means reading bytes — so the catalogue is a
 * plain pure map and `buildScene` can emit a self-contained [DrawShape] without any backend gaining a
 * resolver. The whole supply system is therefore unit-testable in pure JVM, with no device and no
 * fixture files.
 *
 * ### ⚠ This catalogue is incomplete, on purpose — 12 of 16
 *
 * | Authored (12) | Owed to a designer (4) |
 * |---|---|
 * | `shape.rect` · `shape.circle` · `shape.triangle` · `shape.rule` · `mark.registration` · `mark.halftone` · `mark.asterisk` · `mark.arrow` · `paper.window` · `paper.tag` · `fix.staple` · `fix.corner` | `tape.torn` · `paper.strip` · `paper.underline` · `fix.clip` |
 *
 * The twelve here are **derivable** — geometry an engineer can write without inventing a house style.
 * The four still owed are not, and the reason each resists is recorded next to the helpers below
 * rather than left as a gap: three need an authored *tear*, and a paper clip is a wire object in a
 * fill-only renderer.
 *
 * ⚠ **The split was originally drawn at 4/12 along family lines, and that was wrong.** *Cut shapes*
 * looked like the derivable family because it was authored first; in fact derivability cuts across
 * all four families, and the eight added in the second package came from three of them. A step that
 * mixes "needs a house style" with "is a rectangle" always looks blocked by its hardest quarter
 * (SUPPLIES-SPEC §10).
 *
 * The incompleteness is **explicit** rather than implicit: [outlineOf] returns `null` for a supply
 * that has no outline yet, so an unauthored (or misspelled, or newer-schema) `supplyId` draws nothing
 * and is reported by the layer that knows the catalogue — never a crash, and never a guessed shape.
 * §2.2 rules that catalogue membership is checked here at the render boundary, not in the document
 * validator, precisely so an unknown id cannot make a zine refuse to open.
 *
 * ### The id prefix is not the family
 *
 * Five prefixes (`tape`, `fix`, `mark`, `paper`, `shape`) carry four families, because *Tape &
 * fixings* holds one tape and three fixings. Nothing here reads a family, and nothing here should
 * start: the family lives in `Copy.Supplies.BY_FAMILY`, never in `supplyId.substringBefore('.')`.
 * The ids authored below span four prefixes across three families, which is the plainest evidence for
 * the rule: `mark.asterisk` is filed under *Stamps & marks*, `paper.window` under *Cut paper*, and
 * `fix.staple` under *Tape & fixings* — but nothing here knows that, and nothing here needs to.
 *
 * ### Attestation (§4.1, the definition of done)
 *
 * **All twelve outlines below were authored from scratch, in this file, by writing coordinates. No
 * reference art was traced, opened or consulted; they are elementary geometry — squares, inscribed
 * circles, polygons written vertex by vertex, and one star polygon evaluated at two radii — and they
 * carry no third-party licence. They are covered by the repository licence.** §4.1 requires one attestation per supply and there is
 * no colophon surface yet (X11 is unbuilt), so it lives here, next to the coordinates it is about; it
 * moves to the colophon when there is one. The four still owed each need their own, written by
 * whoever draws them — which is the point of the rule, since the derivable ones are precisely those
 * nobody *could* have traced. The second package's attestation sits with its own coordinates below.
 */
public object SupplyCatalog {

    /** The circle's Bézier constant `4/3·(√2 − 1)`, scaled by the unit circle's radius of `0.5`. */
    private const val KAPPA: Double = 0.5522847498307936 * 0.5

    /** Half the rule's authored thickness, as a fraction of the unit square — see [RULE]. */
    private const val RULE_HALF_THICKNESS: Double = 0.1

    /** The full unit square. Scaled by the fold to the element's own box, so a rectangle *is* its box. */
    private val RECT: SupplyOutline = closed(
        PtPoint(0.0, 0.0), PtPoint(1.0, 0.0), PtPoint(1.0, 1.0), PtPoint(0.0, 1.0),
    )

    /**
     * The inscribed circle, as the four-cubic approximation every vector format uses. Starts at the
     * top and runs clockwise (+y is down), which is the same handedness as the rest of the geometry —
     * irrelevant to an even-odd fill, and one less thing to be surprised by when reading a diff.
     */
    private val CIRCLE: SupplyOutline = SupplyOutline(
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
    private val TRIANGLE: SupplyOutline = closed(
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
    private val RULE: SupplyOutline = closed(
        PtPoint(0.0, 0.5 - RULE_HALF_THICKNESS),
        PtPoint(1.0, 0.5 - RULE_HALF_THICKNESS),
        PtPoint(1.0, 0.5 + RULE_HALF_THICKNESS),
        PtPoint(0.0, 0.5 + RULE_HALF_THICKNESS),
    )

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
    private const val REG_ARM_HALF: Double = 0.035

    /** Where a registration arm stops — on the ring's outer edge, so the two meet without crossing. */
    private const val REG_RING_OUTER: Double = 0.25

    private const val REG_RING_INNER: Double = 0.19

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
    private val REGISTRATION: SupplyOutline = SupplyOutline(
        bar(0.0, 0.5 - REG_ARM_HALF, 0.5 - REG_RING_OUTER, 0.5 + REG_ARM_HALF) +
            bar(0.5 + REG_RING_OUTER, 0.5 - REG_ARM_HALF, 1.0, 0.5 + REG_ARM_HALF) +
            bar(0.5 - REG_ARM_HALF, 0.0, 0.5 + REG_ARM_HALF, 0.5 - REG_RING_OUTER) +
            bar(0.5 - REG_ARM_HALF, 0.5 + REG_RING_OUTER, 0.5 + REG_ARM_HALF, 1.0) +
            circle(0.5, 0.5, REG_RING_OUTER) +
            circle(0.5, 0.5, REG_RING_INNER),
    )

    /** The largest halftone dot's radius — also its first centre, so its edge sits on the box. */
    private const val HALFTONE_R_MAX: Double = 0.11

    /** How much each step along the lattice diagonal takes off the radius. */
    private const val HALFTONE_R_STEP: Double = 0.012

    /**
     * The lattice pitch, **derived** so the far column's outer edge lands exactly on 1.0.
     *
     * Three gaps of [HALFTONE_PITCH] separate four columns; the first centre sits at [HALFTONE_R_MAX]
     * and the last dot on the top row has radius `HALFTONE_R_MAX - 3 * HALFTONE_R_STEP`. Writing the
     * arithmetic instead of the number means editing the ramp cannot silently un-fill the square.
     */
    private const val HALFTONE_PITCH: Double =
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
    private fun halftoneRadius(diagonal: Int): Double =
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
    private val HALFTONE: SupplyOutline = SupplyOutline(
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

    /** How much of the window frame is border, per edge. */
    private const val WINDOW_BORDER: Double = 0.14

    /**
     * A cut-out window: the full square with a smaller square removed.
     *
     * The one outline in the catalogue whose *point* is the hole, so it is also the one that would
     * break first if a backend ever dropped the even-odd fill rule — the second subpath would fill
     * solid and the window would become a rectangle. `SupplyOutlineRingTest` already pins that the
     * fill rule reaches the backend; this is the supply that would show it to a maker.
     */
    private val WINDOW: SupplyOutline = SupplyOutline(
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
     * A staple, seen flat: a crossbar with two legs turned down at its ends.
     *
     * **One polygon, eight points** — not a bar plus two legs, for the even-odd reason in this
     * section's header. Proportions are a real staple's: the crown is most of the width, the legs are
     * about a third of the height, and the wire is thin enough to read as wire rather than as a
     * bracket.
     */
    private val STAPLE: SupplyOutline = closed(
        PtPoint(0.0, 0.30), PtPoint(1.0, 0.30), PtPoint(1.0, 1.00), PtPoint(0.82, 1.00),
        PtPoint(0.82, 0.48), PtPoint(0.18, 0.48), PtPoint(0.18, 1.00), PtPoint(0.0, 1.00),
    )

    /**
     * A photo corner: the triangular pocket a print slides into, with the pocket cut out.
     *
     * Two subpaths and therefore a hole, exactly like [WINDOW] — the inner triangle is the opening the
     * photograph's corner goes behind, and without it the supply is a solid triangle already served by
     * `shape.triangle`.
     */
    private val PHOTO_CORNER: SupplyOutline = SupplyOutline(
        closed(PtPoint(0.0, 0.0), PtPoint(1.0, 1.0), PtPoint(0.0, 1.0)).subpaths +
            closed(PtPoint(0.16, 0.42), PtPoint(0.58, 0.84), PtPoint(0.16, 0.84)).subpaths,
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
    private val SPEECH_TAG: SupplyOutline = closed(
        PtPoint(0.0, 0.0), PtPoint(1.0, 0.0), PtPoint(1.0, 0.62), PtPoint(0.44, 0.62),
        PtPoint(0.10, 1.00), PtPoint(0.20, 0.62), PtPoint(0.0, 0.62),
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
    private val STAR: SupplyOutline = closed(
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
    private val ARROW: SupplyOutline = closed(
        PtPoint(0.0, 0.34), PtPoint(0.58, 0.34), PtPoint(0.58, 0.06), PtPoint(1.0, 0.50),
        PtPoint(0.58, 0.94), PtPoint(0.58, 0.66), PtPoint(0.0, 0.66),
    )

    /** Every authored outline, keyed by the `supplyId` written into saved documents. */
    public val OUTLINES: Map<String, SupplyOutline> = linkedMapOf(
        "shape.rect" to RECT,
        "shape.circle" to CIRCLE,
        "shape.triangle" to TRIANGLE,
        "shape.rule" to RULE,
        // The derivable eight (SUPPLIES-SPEC §4.3). Four of the sixteen remain unauthored and are
        // listed with their reason at this file's foot — they are not forgotten, they need a hand.
        "mark.registration" to REGISTRATION,
        "mark.halftone" to HALFTONE,
        "mark.asterisk" to STAR,
        "mark.arrow" to ARROW,
        "paper.window" to WINDOW,
        "paper.tag" to SPEECH_TAG,
        "fix.staple" to STAPLE,
        "fix.corner" to PHOTO_CORNER,
    )

    /** The outline for [supplyId], or `null` when it is not authored yet (or not a supply at all). */
    public fun outlineOf(supplyId: String): SupplyOutline? = OUTLINES[supplyId]


    /** One axis-aligned rectangle as a single closed subpath, from (x0,y0) to (x1,y1). */
    private fun bar(x0: Double, y0: Double, x1: Double, y1: Double): List<Subpath> = closed(
        PtPoint(x0, y0), PtPoint(x1, y0), PtPoint(x1, y1), PtPoint(x0, y1),
    ).subpaths

    /**
     * One circle of radius [r] about ([cx],[cy]), as the same four-cubic approximation [CIRCLE] uses.
     *
     * [KAPPA] is already folded by half for the unit-square circle, so it is doubled back out here and
     * re-scaled by [r] — the alternative is a second constant that means the same thing.
     */
    private fun circle(cx: Double, cy: Double, r: Double): List<Subpath> {
        val k = KAPPA * 2.0 * r
        return listOf(
            Subpath(
                start = PtPoint(cx, cy - r),
                segments = listOf(
                    Segment.CubicTo(PtPoint(cx + k, cy - r), PtPoint(cx + r, cy - k), PtPoint(cx + r, cy)),
                    Segment.CubicTo(PtPoint(cx + r, cy + k), PtPoint(cx + k, cy + r), PtPoint(cx, cy + r)),
                    Segment.CubicTo(PtPoint(cx - k, cy + r), PtPoint(cx - r, cy + k), PtPoint(cx - r, cy)),
                    Segment.CubicTo(PtPoint(cx - r, cy - k), PtPoint(cx - k, cy - r), PtPoint(cx, cy - r)),
                ),
            ),
        )
    }

    // ### The four still unauthored, and why each needs a hand rather than a constant
    //
    // `tape.torn` · `paper.strip` · `paper.underline` — **a torn or drawn edge is the house style**.
    // §5 bans procedural variation of an outline outright ("no randomise the tear"), so a tear cannot be
    // generated; it has to be drawn once, by someone, and drawn well. These are the real S5 work, and
    // they are ONE commission rather than three: the same authored tear serves all three supplies.
    //
    // `fix.clip` — **a paper clip is a wire object, and wire is a stroke.** §4.1 rule 2 is fill-only, so
    // a clip must be authored as the closed ribbon *around* the wire: a long, doubled, self-parallel
    // outline whose two ends nest. That is genuine draughtsmanship, not elementary geometry, and it is
    // the one row of SUPPLIES-SPEC §4.3's "derivable" claim that did not survive contact with the
    // renderer. Recorded here rather than quietly dropped, because the same trap waits for the safety
    // pin and the rubber band in §4.3's proposed set.
    //
    // (A `//` block, not KDoc: it documents an ABSENCE, so there is no declaration for it to attach to.
    // As `/** */` it bound silently to whatever happened to be declared next and appeared in no output.)

    /** One closed subpath of straight segments through [points], in order. */
    private fun closed(vararg points: PtPoint): SupplyOutline =
        SupplyOutline(listOf(Subpath(points.first(), points.drop(1).map { Segment.LineTo(it) })))
}
