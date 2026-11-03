package com.aritr.zinely.core.editor

import java.text.BreakIterator

/**
 * What a photo's or Art piece's description may be
 * ([ADR-147](../../../../../../../../docs/DECISIONS.md#adr-147) decision 2, Bench A52.3). Pure text rules; the
 * reducer owns [normalise] and the Describe field owns [edit].
 *
 * Invariant: **what is stored has passed [normalise] once, at the reducer**; the 250-code-point cap is the
 * field's *edit filter* and never a rule on stored data, so a longer description (a future build's, or a
 * hand-edited file's) is never cut by opening and closing the sheet.
 */
public object DescriptionText {
    /** The cap, in code points, enforced while typing or pasting. */
    public const val MAX_CODE_POINTS: Int = 250

    /**
     * [raw] as it is stored: control characters and line or paragraph separators become spaces (one for each),
     * both ends are trimmed, and a blank result is `null` (no description). Nothing in the middle is touched.
     */
    public fun normalise(raw: String?): String? =
        raw?.let(::spaceControls)?.trim()?.takeIf { it.isNotEmpty() }

    /**
     * The field's value after an edit. [opening] is the text the sheet opened with, [next] what the keyboard or
     * a paste produced. Control characters become spaces; then, as with the rename cap
     * ([ADR-136](../../../../../../../../docs/DECISIONS.md#adr-136) W2a item 3), the length is limited **on growth
     * only**: the limit is the larger of [MAX_CODE_POINTS] and the opening's own length, and the cut falls on a
     * grapheme boundary so an emoji or a combined mark goes in whole or not at all.
     */
    public fun edit(opening: String, next: String): String {
        val limit = maxOf(MAX_CODE_POINTS, opening.codePointCount(0, opening.length))
        return cap(spaceControls(next), limit)
    }

    private fun spaceControls(s: String): String {
        if (s.none(::isControl)) return s
        return buildString(s.length) { s.forEach { append(if (isControl(it)) ' ' else it) } }
    }

    private fun isControl(c: Char): Boolean = when (Character.getType(c).toByte()) {
        Character.CONTROL, Character.LINE_SEPARATOR, Character.PARAGRAPH_SEPARATOR -> true
        else -> false
    }

    /** Cut [s] at or below [limit] code points on a grapheme-cluster boundary. */
    private fun cap(s: String, limit: Int): String {
        if (s.codePointCount(0, s.length) <= limit) return s
        val raw = s.offsetByCodePoints(0, limit)
        val boundary = BreakIterator.getCharacterInstance().apply { setText(s) }
        val end = if (boundary.isBoundary(raw)) raw else boundary.preceding(raw)
        return s.substring(0, end)
    }
}
