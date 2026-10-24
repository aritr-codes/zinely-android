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
 * ### The cabinet: 124 outlines in four families (ADR-139)
 *
 * The owner's A39 picks (v21-bench.html, 2026-10-22): 17 shipped drawings kept as picked, 17 shipped ids
 * redrawn inside their old ink boxes (Never #19), and 90 new ids. Each family's outlines live in its own
 * object, transcribed from its A39 row; `BenchArtSheetParityTest` holds every one to that row.
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
 * ### Provenance (§4.1, ADR-104, ADR-139)
 *
 * Every outline records where it came from in `SupplyProvenanceCatalog`: drawn in-house, a redraw of a
 * shipped id, or imported from a permissively licensed set (with its upstream file's sha256 and the change
 * made). Imported sets are credited in Licences & credits, never in the PDF. No outline is parsed from SVG
 * at runtime: each is reviewed Kotlin source.
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
