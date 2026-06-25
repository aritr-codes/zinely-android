package com.aritr.zinely.render.android

/**
 * The MVP character set the bundled font(s) must be **self-covering** for (ADR-028 §4.2, risk R2/R12).
 * Every code point here must resolve to a glyph **within the bundled family** — never via Android's
 * device-variable system fallback (which `Typeface.CustomFallbackBuilder` cannot force below API 29,
 * [ADR-024](../DECISIONS.md#adr-024)). [FontCoverageTest] asserts this against each TTF's `cmap`, so a
 * future font swap/subset that drops a glyph fails the build instead of silently shifting metrics.
 *
 * Scope is English-first Latin: printable ASCII, the Latin-1 letters (accented forms for names and
 * loanwords), common currency, and the punctuation/symbols a zine actually uses (curly quotes, en/em
 * dashes, ellipsis, bullet, ©/®/™). Extended scripts, emoji, and CJK are explicitly out of MVP scope
 * (custom-font import is V2, [ROADMAP](../ROADMAP.md)) and are excluded from committed goldens.
 */
internal object MvpCharset {

    val codePoints: Set<Int> = buildSet {
        addAll(0x20..0x7E)                          // printable ASCII
        addAll((0x00C0..0x00FF) - 0x00D7 - 0x00F7)  // Latin-1 letters (minus × ÷)
        addAll(listOf(0x00A3, 0x00A2, 0x00A5, 0x20AC)) // £ ¢ ¥ €
        // © ® ™ ° § ¶ † ‡ ·
        addAll(listOf(0x00A9, 0x00AE, 0x2122, 0x00B0, 0x00A7, 0x00B6, 0x2020, 0x2021, 0x00B7))
        // – — ‘ ’ “ ” … •
        addAll(listOf(0x2013, 0x2014, 0x2018, 0x2019, 0x201C, 0x201D, 0x2026, 0x2022))
    }
}
