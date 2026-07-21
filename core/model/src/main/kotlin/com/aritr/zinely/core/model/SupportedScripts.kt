package com.aritr.zinely.core.model

/**
 * A named script the app either supports or knowingly does not.
 *
 * Naming a script we *don't* support is deliberate: the out-of-scope entries exist so the app can say
 * "Arabic isn't supported yet" instead of "some characters won't print", which is the difference
 * between a kind refusal and a shrug.
 */
public enum class Script(public val displayName: String) {
    LATIN("Latin"),
    CYRILLIC("Cyrillic"),
    GREEK("Greek"),

    /**
     * Cyrillic beyond the bundled coverage (U+0500–U+052F: Komi, Abkhaz and other extended letters).
     * Named separately from [CYRILLIC] so a refusal can be specific — the user typed Cyrillic, and it is
     * *this* Cyrillic we cannot print — rather than confusingly claiming Cyrillic is unsupported.
     */
    CYRILLIC_EXTENDED("Extended Cyrillic"),

    /** Polytonic Greek (U+1F00–U+1FFF), beyond the bundled coverage. Named for the same reason. */
    GREEK_EXTENDED("Polytonic Greek"),
    ARABIC("Arabic"),
    HEBREW("Hebrew"),
    DEVANAGARI("Devanagari"),
    BENGALI("Bengali"),
    TAMIL("Tamil"),
    THAI("Thai"),
    HAN("Chinese"),
    HIRAGANA("Japanese"),
    KATAKANA("Japanese"),
    HANGUL("Korean"),
    EMOJI("Emoji"),
    OTHER("Unsupported"),
}

/**
 * **The ratified v1 script set** ([ZINELY V1](../../../../../../../../docs/zinely-v1.md) §5) and the
 * classifier that decides which script a character belongs to.
 *
 * Pure Kotlin with zero Android dependencies, because the same answer must be reachable from the editor
 * (to warn at typing time), from validation, and from tests — and because "which script is this
 * character" is a property of Unicode, not of a device.
 *
 * **Why a ratified set at all.** DoD 4 promises that every character in the supported set renders on all
 * four surfaces and every character outside it is *flagged at typing time, kindly, before any work is
 * lost*. That promise is only keepable if "supported" is a closed, stated set rather than whatever the
 * bundled font happens to contain. This object is that set; [BUNDLED_SCRIPTS] is the promise, and glyph
 * coverage of the actual font files is verified separately against it.
 *
 * **Scope is honest, not aspirational.** RTL scripts (Arabic, Hebrew), Indic scripts and CJK are
 * **out of scope for v1** and are classified here precisely so they can be named when refused. They are
 * excluded because each needs shaping, bidirectional layout, or a font an order of magnitude larger than
 * the whole app — not because they are unimportant.
 */
public object SupportedScripts {

    /** The scripts v1 promises to render on all four surfaces. */
    public val BUNDLED_SCRIPTS: Set<Script> = setOf(Script.LATIN, Script.CYRILLIC, Script.GREEK)

    /**
     * Characters that carry no script of their own — spaces, digits, punctuation, symbols, and control
     * characters. They are always considered supported: they are shared across scripts, and flagging a
     * space or a full stop as "unsupported" would be noise that teaches the user to ignore the warning.
     */
    public fun isScriptNeutral(codePoint: Int): Boolean = when {
        codePoint == '\n'.code || codePoint == '\r'.code || codePoint == '\t'.code -> true
        codePoint in 0x20..0x40 -> true // space, digits, common punctuation and symbols
        codePoint in 0x5B..0x60 -> true // [ \ ] ^ _ `
        codePoint in 0x7B..0x7E -> true // { | } ~
        codePoint in 0x00A0..0x00BF -> true // Latin-1 punctuation/symbols (£, ©, °, «, »…)
        codePoint == 0x00D7 || codePoint == 0x00F7 -> true // × ÷
        codePoint in 0x2000..0x206F -> true // general punctuation (dashes, quotes, ellipsis, bullet)
        codePoint in 0x20A0..0x20BF -> true // currency symbols
        codePoint in 0x2100..0x214F -> true // letterlike symbols (™, №…)
        else -> false
    }

    /**
     * The [Script] [codePoint] belongs to. Script-neutral characters report [Script.LATIN] — callers
     * that care about the distinction should ask [isScriptNeutral] first; callers that only ask "is this
     * supported" get the right answer either way.
     *
     * Ranges are deliberately coarse: this classifies well enough to *name what is unsupported*, which
     * is all DoD 4 requires. It is not a Unicode script database.
     */
    public fun scriptOf(codePoint: Int): Script = when {
        isScriptNeutral(codePoint) -> Script.LATIN
        codePoint in 0x0041..0x005A || codePoint in 0x0061..0x007A -> Script.LATIN // A-Z a-z
        // Latin-1 letters, Latin Extended-A/B, IPA Extensions (U+0250..U+02AF — every character there is
        // named "LATIN SMALL LETTER …" and carries Script=Latin per UAX#24), and the spacing modifier
        // letters (ʼ ʻ ˆ ˇ) that Latin orthographies use.
        codePoint in 0x00C0..0x02FF -> Script.LATIN
        // **Combining marks are inherited, never their own script.** `é` typed in decomposed form
        // (NFD — what iOS and macOS keyboards routinely produce) is `e` + U+0301, and classifying the
        // mark as unsupported would warn a user that ordinary French cannot be printed. A warning that
        // fires on legitimate text is worse than no warning: it teaches people to dismiss it, which
        // costs us the one moment DoD 4 exists to protect. Treated as Latin because the ratified set is
        // Latin/Greek/Cyrillic — every script whose marks these are — and because the mark alone is
        // never what makes a run unprintable; the base character it attaches to already decides that.
        codePoint in 0x0300..0x036F -> Script.LATIN
        codePoint in 0x1E00..0x1EFF -> Script.LATIN // Latin Extended Additional (Vietnamese)
        // **Greek and Cyrillic are promised only where the bundled fonts deliver.** These ranges are
        // narrower than the Unicode blocks on purpose: promising a block the font does not cover is how
        // a character reaches paper blank with no warning — the DoD 4 failure this file exists to
        // prevent. Excluded and NOT promised: Cyrillic Supplement (U+0500–U+052F, the Komi/Abkhaz
        // letters — 47 assigned code points, none in Inter) and polytonic Greek Extended (U+1F00–U+1FFF).
        // Those classify as their own script below, so a user typing them is warned rather than
        // silently failed. Widening the promise means bundling glyphs first, and the coverage guard is
        // what proves it.
        codePoint in 0x0370..0x03FF -> Script.GREEK
        codePoint in 0x0400..0x04FF -> Script.CYRILLIC
        // Beyond the promise but still correctly *named*, so a refusal can say which script it is.
        codePoint in 0x0500..0x052F -> Script.CYRILLIC_EXTENDED
        codePoint in 0x1F00..0x1FFF -> Script.GREEK_EXTENDED
        codePoint in 0x0590..0x05FF -> Script.HEBREW
        codePoint in 0x0600..0x06FF || codePoint in 0x0750..0x077F -> Script.ARABIC
        codePoint in 0x0900..0x097F -> Script.DEVANAGARI
        codePoint in 0x0980..0x09FF -> Script.BENGALI
        codePoint in 0x0B80..0x0BFF -> Script.TAMIL
        codePoint in 0x0E00..0x0E7F -> Script.THAI
        codePoint in 0x3040..0x309F -> Script.HIRAGANA
        codePoint in 0x30A0..0x30FF -> Script.KATAKANA
        codePoint in 0xAC00..0xD7AF || codePoint in 0x1100..0x11FF -> Script.HANGUL
        codePoint in 0x4E00..0x9FFF || codePoint in 0x3400..0x4DBF -> Script.HAN
        isEmoji(codePoint) -> Script.EMOJI
        else -> Script.OTHER
    }

    /** Whether v1 promises to render [codePoint]. */
    public fun isSupported(codePoint: Int): Boolean = scriptOf(codePoint) in BUNDLED_SCRIPTS

    /**
     * Emoji are called out separately from [Script.OTHER] because they fail for a *different reason* and
     * deserve a different explanation: colour emoji (CBDT/COLR) cannot be embedded in a PDF at all
     * ([ADR-001](../../../../../../../../docs/DECISIONS.md#adr-001)), so this is a limit of print, not a
     * missing font.
     */
    private fun isEmoji(codePoint: Int): Boolean =
        codePoint in 0x1F300..0x1FAFF ||
            codePoint in 0x1F000..0x1F2FF ||
            codePoint in 0x2600..0x27BF ||
            codePoint == 0x203C || codePoint == 0x2049 ||
            codePoint in 0xFE00..0xFE0F // variation selectors (the emoji-presentation marker)
}
