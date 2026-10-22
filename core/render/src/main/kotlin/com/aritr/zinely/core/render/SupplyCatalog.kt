package com.aritr.zinely.core.render

/**
 * The drawer: `supplyId` → the authored outline that draws it (SUPPLIES-SPEC §3.4, §4.1).
 *
 * Outlines are **code, not data**. A supply needs no I/O to resolve — unlike [DrawImage]'s `assetId`,
 * which stays unresolved on the tape because resolving it means reading bytes — so the catalogue is a
 * plain pure map and `buildScene` can emit a self-contained [DrawShape] without any backend gaining a
 * resolver. The whole supply system is therefore unit-testable in pure JVM, with no device and no
 * fixture files.
 *
 * ### The frozen cabinet is complete — 16 of 16
 *
 * Twelve outlines are derivable elementary geometry. The remaining four deliberately carry Zinely's
 * authored hand: one irregular tear voice serves `tape.torn`, `paper.strip` and `paper.underline`, while
 * `fix.clip` is drawn as the closed ribbon around a wire that a fill-only renderer requires. Their
 * silhouettes were ruled in `v21-bench.html` A11 before being transcribed here.
 *
 * ⚠ **The split was originally drawn at 4/12 along family lines, and that was wrong.** *Cut shapes*
 * looked like the derivable family because it was authored first; in fact derivability cuts across
 * all four families, and the eight added in the second package came from three of them. A step that
 * mixes "needs a house style" with "is a rectangle" always looks blocked by its hardest quarter
 * (SUPPLIES-SPEC §10).
 *
 * Unknown ids remain **explicit** rather than guessed: [outlineOf] returns `null` for a misspelled or
 * newer-schema `supplyId`, so it draws nothing at the render boundary — never a crash or invented shape.
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
 * **All thirty-four outlines below were authored from scratch, in this package (since ADR-139 split across `ShapeArt`, `MarkArt`, `PaperArt` and `TapeArt`), by writing coordinates. No
 * reference art was traced, opened or consulted; they are elementary geometry — squares, inscribed
 * circles, polygons written vertex by vertex, and one star polygon evaluated at two radii — and they
 * carry no third-party licence. They are covered by the repository licence.** §4.1 requires one attestation per supply and there is
 * no colophon surface yet (X11 is unbuilt), so it lives here, next to the coordinates it is about; it
 * moves to the colophon when there is one.
 */
public object SupplyCatalog {

    /**
     * Every authored outline, keyed by the `supplyId` written into saved documents, in
     * `Copy.Supplies.BY_FAMILY` order: Tape & fixings, Stamps & marks, Cut paper, Cut shapes (ADR-139).
     * One object per family so no static initialiser nears the 64 KB class-file cap.
     */
    public val OUTLINES: Map<String, SupplyOutline> = linkedMapOf<String, SupplyOutline>().apply {
        putAll(TapeArt.OUTLINES)
        putAll(MarkArt.OUTLINES)
        putAll(PaperArt.OUTLINES)
        putAll(ShapeArt.OUTLINES)
    }

    /** The outline for [supplyId], or `null` when it is not authored yet (or not a supply at all). */
    public fun outlineOf(supplyId: String): SupplyOutline? = OUTLINES[supplyId]
}
