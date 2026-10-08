package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.DocumentVoice
import com.aritr.zinely.core.model.Script
import com.aritr.zinely.core.model.SupportedScripts
import com.aritr.zinely.core.model.TextStyle

/**
 * Why something in the Font row cannot be used, or what the row has to say
 * (frozen `v21-typebar.html` A29 rule 8). The words live in `Copy.Type`; this is only the meaning.
 */
public sealed interface FontReason {
    /** The text's family is one this build does not have (A29 rule 10). It is drawn in Plain for now. */
    public data object UnknownFont : FontReason

    /** The text holds letters of a script Book does not set. At least one of the two is true. */
    public data class BookLacksScript(val greek: Boolean, val cyrillic: Boolean) : FontReason

    /** The text is smaller than Book's minimum print size. */
    public data object BookNeedsSize : FontReason

    /** A Book text is at Book's smallest size, so Smaller is refused (A29 rule 9). */
    public data object BookAtFloor : FontReason
}

/**
 * The Font row's whole state for one text, derived and never stored (A29: "derived from the state; never
 * stored, never needs a tap").
 *
 * @property voice the chosen voice, or `null` for a family this build does not know: then neither word is
 *   chosen (A29 rule 10).
 * @property bookBlock why Book cannot be chosen now, or `null` when it can be, or already is. A tap on Book
 *   says this reason even while [line] is showing a different one.
 * @property smallerBlocked a Book text at or below Book's minimum, above the ramp's own bottom: Smaller is
 *   drawn unavailable and refused, with [FontReason.BookAtFloor].
 * @property line the one reason line under the row, or `null` for none. One shows, in A29 rule 8's order:
 *   the unknown font, then the script, then the size.
 */
public data class FontRow(
    val voice: DocumentVoice?,
    val bookBlock: FontReason?,
    val smallerBlocked: Boolean,
    val line: FontReason?,
)

/**
 * Derive the Font row for [text] set in [style].
 *
 * [rampMinPt] is the size stepper's own bottom (the surface owns the ramp): at that size Smaller is truly
 * unavailable for every text, so Book's floor has nothing to add there.
 *
 * Only Greek and Cyrillic are named. A script no voice sets (Hebrew, say) is not a reason to refuse Book:
 * Plain cannot print it either, and the typing-time notice already says so.
 */
public fun fontRow(text: String, style: TextStyle, rampMinPt: Double): FontRow {
    val voice = DocumentVoice.of(style.fontFamily)
    val bookBlock = if (voice == DocumentVoice.BOOK) null else bookBlock(text, style.sizePt)
    val smallerBlocked = voice == DocumentVoice.BOOK &&
        style.sizePt <= DocumentVoice.BOOK_MIN_SIZE_PT && style.sizePt > rampMinPt
    val line = when {
        voice == null -> FontReason.UnknownFont
        smallerBlocked -> FontReason.BookAtFloor
        else -> bookBlock
    }
    return FontRow(voice, bookBlock, smallerBlocked, line)
}

private fun bookBlock(text: String, sizePt: Double): FontReason? {
    var greek = false
    var cyrillic = false
    var i = 0
    while (i < text.length) {
        val cp = text.codePointAt(i)
        when (SupportedScripts.scriptOf(cp)) {
            Script.GREEK, Script.GREEK_EXTENDED -> greek = true
            Script.CYRILLIC, Script.CYRILLIC_EXTENDED -> cyrillic = true
            else -> Unit
        }
        i += Character.charCount(cp)
    }
    return when {
        greek || cyrillic -> FontReason.BookLacksScript(greek, cyrillic)
        sizePt < DocumentVoice.BOOK_MIN_SIZE_PT -> FontReason.BookNeedsSize
        else -> null
    }
}
