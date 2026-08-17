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
 * ### ⚠ This catalogue is incomplete, on purpose — 4 of 16
 *
 * Only the **Cut shapes** family is authored here:
 *
 * | Authored (4) | Owed to a designer (12) |
 * |---|---|
 * | `shape.rect` · `shape.circle` · `shape.triangle` · `shape.rule` | `tape.torn` · `fix.corner` · `fix.staple` · `fix.clip` · `mark.asterisk` · `mark.arrow` · `mark.halftone` · `mark.registration` · `paper.strip` · `paper.window` · `paper.tag` · `paper.underline` |
 *
 * The four here are scissor-clean geometry — they are *derivable*, which is exactly why they can be
 * written by an engineer without inventing a house style. The other twelve are torn, stamped and
 * hand-drawn: they carry the product's hand, and drawing them is design work with an attestation
 * attached (§4.1 — authored from scratch, no reference art traced). They land as their own package.
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
 * That all four ids authored below happen to share the `shape.` prefix is a coincidence of which
 * family was cheapest to author first.
 *
 * ### Attestation (§4.1, the definition of done)
 *
 * **All four outlines below were authored from scratch, in this file, by writing coordinates. No
 * reference art was traced, opened or consulted; they are elementary geometry — a square, an
 * inscribed circle, an isosceles triangle and a centred bar — and they carry no third-party licence.
 * They are covered by the repository licence.** §4.1 requires one attestation per supply and there is
 * no colophon surface yet (X11 is unbuilt), so it lives here, next to the coordinates it is about; it
 * moves to the colophon when there is one. The twelve still owed each need their own, written by
 * whoever draws them — which is the point of the rule, since these four are the only ones nobody
 * *could* have traced.
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

    /** Every authored outline, keyed by the `supplyId` written into saved documents. */
    public val OUTLINES: Map<String, SupplyOutline> = linkedMapOf(
        "shape.rect" to RECT,
        "shape.circle" to CIRCLE,
        "shape.triangle" to TRIANGLE,
        "shape.rule" to RULE,
    )

    /** The outline for [supplyId], or `null` when it is not authored yet (or not a supply at all). */
    public fun outlineOf(supplyId: String): SupplyOutline? = OUTLINES[supplyId]

    /** One closed subpath of straight segments through [points], in order. */
    private fun closed(vararg points: PtPoint): SupplyOutline =
        SupplyOutline(listOf(Subpath(points.first(), points.drop(1).map { Segment.LineTo(it) })))
}
