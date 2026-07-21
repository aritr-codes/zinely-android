package com.aritr.zinely.render.android

import android.content.res.AssetManager
import com.aritr.zinely.core.model.Script

/**
 * What a bundled font file actually covers, measured from the file itself.
 *
 * @param missing the requested code points the font does not map, in ascending order.
 */
public data class FontCoverageReport(
    public val familyName: String,
    public val assetPath: String,
    public val missing: List<Int>,
) {
    public val isComplete: Boolean get() = missing.isEmpty()
}

/**
 * **Registry completeness (F3 Increment 2).** Answers whether the fonts a
 * [DocumentFontRegistry] declares can actually keep the promise
 * [com.aritr.zinely.core.model.SupportedScripts.BUNDLED_SCRIPTS] makes.
 *
 * Two different questions are deliberately kept apart, because conflating them is how a promise rots:
 *  - *"Did the user type a supported script?"* — a **product** question, answered purely and instantly
 *    at typing time by `analyzeTextCoverage` in `:core:model`.
 *  - *"Can the bundled TTF render it?"* — a **file** question, answered here by parsing the font's own
 *    `cmap`.
 *
 * If the second ever answers "no" for a script the first calls supported, the app is promising something
 * it cannot deliver — a DoD 4 violation that would surface as blank glyphs on paper. That is exactly the
 * failure this class exists to make loud and early, and it is why the check is production code rather
 * than a test helper: a build that bundles a font can carry the means to verify it.
 *
 * **Why the font's own `cmap` and not `Paint.hasGlyph`.** `hasGlyph` walks the **whole system fallback
 * chain**, so it happily reports `true` for a glyph the bundled font lacks but a device font supplies —
 * which is precisely the preview-vs-export drift the bundled-font policy exists to remove (ADR-028
 * §4.2). Parsing the file answers "does *this* TTF cover it?", which is the only answer that survives
 * being exported to a PDF on someone else's phone.
 */
public object FontCoverage {

    /**
     * **The core alphabets of the ratified scripts** — every letter a reader of English, another
     * European Latin language, Russian or modern Greek would actually type, plus the punctuation and
     * symbols the editor itself produces. Verified against **all four faces**, since a character that
     * renders regular but blanks in bold is still a blank on paper.
     *
     * **This is a probe set, not the whole promise, and the difference is stated rather than hidden.**
     * `SupportedScripts` classifies whole Unicode ranges as Latin/Greek/Cyrillic; the bundled Inter
     * faces do not carry every assigned code point in those ranges (some combining marks, IPA letters,
     * letterlike symbols and archaic Greek are absent). Demanding all of them would fail on a font that
     * is entirely fit for purpose, while demanding too few proves nothing — an earlier revision of this
     * set omitted the Cyrillic Supplement block entirely and would not have noticed 47 promised-but-
     * absent characters. The line drawn here is *the alphabets themselves*: if any of these were
     * missing, the ratified script set would be a false claim.
     *
     * The residue — promised-by-classification but absent from the font — is a known, recorded gap; see
     * the coverage guard test and [ARCHITECTURE.md](../../../../../../../../docs/ARCHITECTURE.md) §5.
     */
    public fun requiredCodePoints(): Set<Int> = buildSet {
        addAll(0x20..0x7E) // printable ASCII
        addAll((0x00C0..0x00FF) - 0x00D7 - 0x00F7) // Latin-1 letters
        addAll(0x0100..0x017F) // Latin Extended-A — the European diacritics
        // Greek: the modern monotonic alphabet, accented forms included.
        add(0x0386); addAll(0x0388..0x038A); add(0x038C); addAll(0x038E..0x03A1); addAll(0x03A3..0x03CE)
        // Cyrillic: the modern Russian alphabet plus Ё/ё.
        addAll(0x0410..0x044F); addAll(listOf(0x0401, 0x0451))
        // Punctuation and symbols the editor's own text handling can produce.
        addAll(listOf(0x2013, 0x2014, 0x2018, 0x2019, 0x201C, 0x201D, 0x2026, 0x2022))
        addAll(listOf(0x00A3, 0x20AC, 0x00A9, 0x00AE, 0x00B0, 0x00A7))
    }.filter { it !in UNPRINTABLE }.toSet()

    /**
     * Code points inside the probed ranges that Unicode leaves unassigned or formally deprecates.
     * Excluded explicitly, and only with a reason, so the guard fails on *real* gaps rather than on
     * slots no well-built font is expected to fill.
     *
     * Every entry is deliberate:
     *  - Greek unassigned slots interleaved through `U+0370..U+03CE`.
     *  - **U+0149** (LATIN SMALL LETTER N PRECEDED BY APOSTROPHE) — **deprecated** since Unicode 5.2,
     *    carrying a compatibility decomposition to `U+02BC U+006E`. Inter's omission is correct
     *    behaviour, not a coverage gap; text using it normalises (NFKC) to characters that *are*
     *    covered. Found by this guard on its first run, which is the guard working as intended.
     */
    private val UNPRINTABLE: Set<Int> = setOf(0x0149)

    /**
     * Measure a family against [required], across **every face it declares**.
     *
     * All four faces are checked because bold and italic ship (ADR-055) and a character that renders in
     * regular but blanks in bold is still a blank on paper. A code point is reported missing if *any*
     * face lacks it, and [assetPath] names the first face that does — so a failure points at the file
     * to fix rather than at the family in the abstract.
     */
    public fun report(
        assets: AssetManager,
        family: DocumentFontFamily,
        required: Set<Int> = requiredCodePoints(),
    ): FontCoverageReport {
        val faces = listOf(
            family.regularAsset,
            family.boldAsset,
            family.italicAsset,
            family.boldItalicAsset,
        ).distinct()

        var firstIncomplete: String? = null
        val missing = sortedSetOf<Int>()
        for (asset in faces) {
            val covered = CmapCoverage.coveredCodePoints(assets.open(asset).use { it.readBytes() })
            val gaps = required.filter { it !in covered }
            if (gaps.isNotEmpty() && firstIncomplete == null) firstIncomplete = asset
            missing.addAll(gaps)
        }

        return FontCoverageReport(
            familyName = family.name,
            assetPath = firstIncomplete ?: family.regularAsset,
            missing = missing.toList(),
        )
    }

    /** Measure every family in [registry]; an empty result means the registry keeps its promise. */
    public fun incompleteFamilies(
        assets: AssetManager,
        registry: DocumentFontRegistry = DocumentFontRegistry.Bundled,
        required: Set<Int> = requiredCodePoints(),
    ): List<FontCoverageReport> =
        registry.families.map { report(assets, it, required) }.filter { !it.isComplete }

    /** The ratified scripts this guard covers, for reporting alongside a failure. */
    public val guardedScripts: Set<Script> =
        setOf(Script.LATIN, Script.CYRILLIC, Script.GREEK)
}
