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

// A HUE or TINTED_GREY name always carries a hue word (InkColourName), so requireNotNull cannot fire.
internal fun hueWord(word: InkHueWord?): String =
    when (requireNotNull(word)) {
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

/** The six-digit hex of an opaque Compose colour (alpha dropped), as the ink library stores it. */
internal fun Color.inkHex(): InkHex = InkHex("%06X".format(java.util.Locale.ROOT, toArgb() and 0xFFFFFF))

/**
 * Prototype `inkName`: a built-in's own name (maker inks, neutrals, then the paper tints — `[...BUILT,
 * ...TINTS]`), else the colour words. A tint is named by its tint for **every** target, text included
 * (INK addendum R-I1), so "Ink · Cream. Pale in print: …" names what the maker tapped even where the tray
 * does not offer tints. `Ink` is in both maker inks and neutrals with the same name.
 *
 * [kind] does not change the answer since R-I1; it stays in the signature (R-I2) so a target-specific
 * rule can return without touching every caller.
 */
@Suppress("UNUSED_PARAMETER")
internal fun inkName(hex: InkHex, inks: ZinelyContentInks, kind: BenchVerbKind): String {
    inks.makerInks.firstOrNull { it.value.inkHex() == hex }?.let { return benchInkName(it.id) }
    inks.neutrals.firstOrNull { it.value.inkHex() == hex }?.let { return benchInkName(it.id) }
    inks.paperTints.firstOrNull { it.value.inkHex() == hex }?.let { return benchInkName(it.id) }
    return inkColourWords(colourName(hex))
}
