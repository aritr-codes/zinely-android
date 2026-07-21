package com.aritr.zinely.core.model

/**
 * What a piece of text contains that v1 cannot print.
 *
 * @param unsupportedScripts the out-of-scope scripts present, in first-appearance order — so a message
 *   can *name* them ("Arabic isn't supported yet") rather than gesture vaguely at "some characters".
 * @param sampleCharacters up to [TextCoverage.MAX_SAMPLES] of the offending characters, in
 *   first-appearance order, for showing the user exactly which ones.
 * @param unsupportedCount how many characters are affected in total.
 */
public data class TextCoverage(
    public val unsupportedScripts: List<Script>,
    public val sampleCharacters: List<String>,
    public val unsupportedCount: Int,
) {
    /** Whether every character is in the ratified script set — i.e. the text will print as typed. */
    public val isFullyCovered: Boolean get() = unsupportedCount == 0

    public companion object {
        /** Enough to show the user what is wrong without turning a warning into a wall of text. */
        public const val MAX_SAMPLES: Int = 6

        /** The result for text that prints exactly as typed. */
        public val Covered: TextCoverage = TextCoverage(emptyList(), emptyList(), 0)
    }
}

/**
 * **Input-time coverage detection (DoD 4, [ZINELY V1](../../../../../../../../docs/zinely-v1.md) §7
 * blocker #1).** Reports which characters in [text] fall outside the ratified script set
 * ([SupportedScripts.BUNDLED_SCRIPTS]).
 *
 * This is the *honesty* half of the typography work: silent blank text is the standing violation of
 * Article 5, and DoD 4 requires unsupported characters be "flagged at typing time, kindly, **before any
 * work is lost**". Answering at typing time is why this is pure, allocation-light, and lives in
 * `:core:model` — the editor can call it on every keystroke without touching a font, a canvas, or a
 * device.
 *
 * **Deliberately not a glyph check.** It answers "did the user type a script v1 promised to support?",
 * not "does the bundled TTF contain this exact glyph". Those are different questions with different
 * owners: the promise is a product decision (this function), while the font's ability to keep it is
 * verified against the real font files in the render module's coverage guard. Asking the font at typing
 * time would also be the wrong answer — it would let the warning drift silently every time a font is
 * swapped, and it cannot run without an Android `AssetManager`.
 *
 * Surrogate pairs are handled by iterating code points, so an emoji or a CJK ideograph outside the BMP
 * counts once, not twice.
 */
public fun analyzeTextCoverage(text: String): TextCoverage {
    if (text.isEmpty()) return TextCoverage.Covered

    val scripts = LinkedHashSet<Script>()
    val samples = LinkedHashSet<String>()
    var count = 0
    var i = 0
    while (i < text.length) {
        val cp = text.codePointAt(i)
        val width = Character.charCount(cp)
        if (!SupportedScripts.isSupported(cp)) {
            count++
            scripts += SupportedScripts.scriptOf(cp)
            if (samples.size < TextCoverage.MAX_SAMPLES) samples += text.substring(i, i + width)
        }
        i += width
    }

    return if (count == 0) {
        TextCoverage.Covered
    } else {
        TextCoverage(
            unsupportedScripts = scripts.toList(),
            sampleCharacters = samples.toList(),
            unsupportedCount = count,
        )
    }
}
