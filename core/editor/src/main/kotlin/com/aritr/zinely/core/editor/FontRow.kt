package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.DocumentVoice
import com.aritr.zinely.core.model.Script
import com.aritr.zinely.core.model.SupportedScripts
import com.aritr.zinely.core.model.TextStyle

/**
 * Why something in the Font row cannot be used, or what the row has to say
 * (frozen `v21-typebar.html` A29 rule 8, and A29H for the third word). The words live in `Copy.Type`; this
 * is only the meaning.
 *
 * Book and Hand are the voices that can be refused (they set Latin only and have a smallest size). A reason
 * names **every** voice it speaks for, so one line can say "Book and Hand have no Greek letters" (A29H.7).
 */
public sealed interface FontReason {
    /** The text's family is one this build does not have (A29 rule 10). It is drawn in Plain for now. */
    public data object UnknownFont : FontReason

    /**
     * The text holds letters of a script [voices] do not set; at least one of [greek], [cyrillic] is true.
     * [stays] is the font the text is in, which the line says it stays in.
     */
    public data class LacksScript(
        val voices: List<DocumentVoice>,
        val stays: DocumentVoice,
        val greek: Boolean,
        val cyrillic: Boolean,
    ) : FontReason

    /** The text is smaller than the minimum print size of every voice in [voices]. */
    public data class NeedsSize(val voices: List<DocumentVoice>) : FontReason

    /** A [voice] text is at its smallest size, so Smaller is refused (A29 rule 9, A29H.8). */
    public data class AtFloor(val voice: DocumentVoice) : FontReason
}

/**
 * The Font row's whole state for one text, derived and never stored (A29: "derived from the state; never
 * stored, never needs a tap").
 *
 * @property voice the chosen voice, or `null` for a family this build does not know: then no word is
 *   chosen (A29 rule 10).
 * @property blocks why each voice that is not chosen cannot be chosen now, one reason **for that word
 *   alone**; a voice that can be chosen, or already is, is absent. A tap on a word says its entry even while
 *   [line] is showing a different reason.
 * @property smallerBlocked a text of a voice that has a minimum, at or below it and above the ramp's own
 *   bottom: Smaller is drawn unavailable and refused, with [FontReason.AtFloor].
 * @property line the one reason line under the row, or `null` for none. One shows, in A29 rule 8's order:
 *   the unknown font, then the script, then the size; it names every word it speaks for.
 */
public data class FontRow(
    val voice: DocumentVoice?,
    val blocks: Map<DocumentVoice, FontReason>,
    val smallerBlocked: Boolean,
    val line: FontReason?,
)

/**
 * Derive the Font row for [text] set in [style].
 *
 * [rampMinPt] is the size stepper's own bottom (the surface owns the ramp): at that size Smaller is truly
 * unavailable for every text, so a voice's floor has nothing to add there.
 *
 * Only Greek and Cyrillic are named. A script no voice sets (Hebrew, say) is not a reason to refuse a voice:
 * Plain cannot print it either, and the typing-time notice already says so.
 */
public fun fontRow(text: String, style: TextStyle, rampMinPt: Double): FontRow {
    val voice = DocumentVoice.of(style.fontFamily)
    val scripts = scriptsIn(text)
    val blocks = DocumentVoice.entries
        .filter { it.minSizePt != null && it != voice }
        .mapNotNull { v -> blockFor(v, scripts, style.sizePt, stays = voice ?: DocumentVoice.PLAIN)?.let { v to it } }
        .toMap()
    val floor = voice?.minSizePt
    val smallerBlocked = floor != null && style.sizePt <= floor && style.sizePt > rampMinPt
    val line = when {
        voice == null -> FontReason.UnknownFont
        smallerBlocked -> FontReason.AtFloor(voice)
        else -> combined(blocks.values)
    }
    return FontRow(voice, blocks, smallerBlocked, line)
}

private class Found(val greek: Boolean, val cyrillic: Boolean) {
    val any: Boolean get() = greek || cyrillic
}

private fun scriptsIn(text: String): Found {
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
    return Found(greek, cyrillic)
}

private fun blockFor(voice: DocumentVoice, found: Found, sizePt: Double, stays: DocumentVoice): FontReason? = when {
    found.any -> FontReason.LacksScript(listOf(voice), stays, found.greek, found.cyrillic)
    sizePt < voice.minSizePt!! -> FontReason.NeedsSize(listOf(voice))
    else -> null
}

/** The script reasons win over the size ones (A29 rule 8); each is merged into one that names all its voices. */
private fun combined(reasons: Collection<FontReason>): FontReason? {
    val script = reasons.filterIsInstance<FontReason.LacksScript>()
    if (script.isNotEmpty()) return script.first().copy(voices = script.flatMap { it.voices })
    val size = reasons.filterIsInstance<FontReason.NeedsSize>()
    return if (size.isEmpty()) null else FontReason.NeedsSize(size.flatMap { it.voices })
}
