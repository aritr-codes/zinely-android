package com.aritr.zinely.feature.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.ink.InkColourName
import com.aritr.zinely.core.editor.ink.InkHex
import com.aritr.zinely.core.editor.ink.InkHueWord
import com.aritr.zinely.core.editor.ink.InkNameForm
import com.aritr.zinely.core.editor.ink.InkTone
import com.aritr.zinely.core.editor.ink.colourName
import com.aritr.zinely.ui.theme.ZinelyContentInks

/** The words for a colour name (prototype `colourName`'s return). `when`s over the enums, so a new value is a compile error, not a blank. */
internal fun inkColourWords(name: InkColourName): String {
    val tone = when (name.tone) {
        InkTone.NONE -> null
        InkTone.DARK -> Copy.InkName.DARK
        InkTone.LIGHT -> Copy.InkName.LIGHT
    }
    return when (name.form) {
        InkNameForm.NEAR_BLACK -> Copy.InkName.NEAR_BLACK
        InkNameForm.WHITE -> Copy.InkName.WHITE
        InkNameForm.GREY -> Copy.InkName.toned(tone, Copy.InkName.GREY)
        InkNameForm.TINTED_GREY -> Copy.InkName.toned(tone, Copy.InkName.tintedGrey(hueWord(name.word)))
        InkNameForm.HUE -> Copy.InkName.toned(tone, hueWord(name.word))
    }
}

private fun hueWord(word: InkHueWord?): String =
    when (requireNotNull(word) { "a hue or tinted-grey name carries a hue word" }) {
        InkHueWord.RED -> Copy.InkName.RED
        InkHueWord.ORANGE -> Copy.InkName.ORANGE
        InkHueWord.YELLOW -> Copy.InkName.YELLOW
        InkHueWord.YELLOW_GREEN -> Copy.InkName.YELLOW_GREEN
        InkHueWord.GREEN -> Copy.InkName.GREEN
        InkHueWord.TEAL -> Copy.InkName.TEAL
        InkHueWord.CYAN -> Copy.InkName.CYAN
        InkHueWord.BLUE -> Copy.InkName.BLUE
        InkHueWord.VIOLET -> Copy.InkName.VIOLET
        InkHueWord.PURPLE -> Copy.InkName.PURPLE
        InkHueWord.MAGENTA -> Copy.InkName.MAGENTA
        InkHueWord.PINK -> Copy.InkName.PINK
        InkHueWord.BROWN -> Copy.InkName.BROWN
        InkHueWord.BEIGE -> Copy.InkName.BEIGE
    }

private fun Color.inkHex() = InkHex("%06X".format(java.util.Locale.ROOT, toArgb() and 0xFFFFFF))

/**
 * Prototype `inkName`: a built-in's own name when [hex] is one of the target's built-ins (a text target
 * is not offered the paper tints, so for text a tint's hex is only a colour), else the colour words.
 * Maker inks come before neutrals, as the prototype's `BUILT` does; `Ink` is in both with the same name.
 */
internal fun inkName(hex: InkHex, inks: ZinelyContentInks, kind: BenchVerbKind): String {
    inks.makerInks.firstOrNull { it.value.inkHex() == hex }?.let { return benchInkName(it.id) }
    inks.neutrals.firstOrNull { it.value.inkHex() == hex }?.let { return benchInkName(it.id) }
    if (kind != BenchVerbKind.TEXT) {
        inks.paperTints.firstOrNull { it.value.inkHex() == hex }?.let { return benchInkName(it.id) }
    }
    return inkColourWords(colourName(hex))
}
