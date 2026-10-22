package com.aritr.zinely.core.editor.ink

import kotlin.math.max
import kotlin.math.min

public enum class InkHueWord { RED, ORANGE, YELLOW, YELLOW_GREEN, GREEN, TEAL, CYAN, BLUE, VIOLET, PURPLE, MAGENTA, PINK, BROWN, BEIGE }

public enum class InkTone { NONE, DARK, LIGHT }

public enum class InkNameForm { NEAR_BLACK, WHITE, GREY, TINTED_GREY, HUE }

/**
 * A colour's name as structure, not words: `:core:editor` carries no user-facing text (ADR-060, ADR-138 §8).
 * Task 5 joins this into words in `:core:copy`.
 */
public data class InkColourName(val tone: InkTone, val form: InkNameForm, val word: InkHueWord?)

/** Prototype `HUE_BANDS`: the first band whose limit is above the hue names it. */
public val HUE_BANDS: List<Pair<Int, InkHueWord>> = listOf(
    12 to InkHueWord.RED, 40 to InkHueWord.ORANGE, 62 to InkHueWord.YELLOW, 85 to InkHueWord.YELLOW_GREEN,
    160 to InkHueWord.GREEN, 175 to InkHueWord.TEAL, 200 to InkHueWord.CYAN, 250 to InkHueWord.BLUE,
    275 to InkHueWord.VIOLET, 300 to InkHueWord.PURPLE, 330 to InkHueWord.MAGENTA, 355 to InkHueWord.PINK,
    360 to InkHueWord.RED,
)

public fun hueName(h: Double): InkHueWord = HUE_BANDS.first { h < it.first }.second

/**
 * Prototype `colourName`, line for line. Names judge grey by chroma (max-min), not HSL saturation,
 * which balloons at the light and dark ends.
 */
public fun colourName(hex: InkHex): InkColourName {
    val (r, g, b) = rgb01(hex)
    val mx = max(r, max(g, b))
    val mn = min(r, min(g, b))
    val c = mx - mn
    val l = (mx + mn) / 2
    if (l < .18 && c < .12) return InkColourName(InkTone.NONE, InkNameForm.NEAR_BLACK, null)
    if (l > .95 && c < .06) return InkColourName(InkTone.NONE, InkNameForm.WHITE, null)
    val tone = if (l < .35) InkTone.DARK else if (l > .72) InkTone.LIGHT else InkTone.NONE
    if (c < .06) return InkColourName(tone, InkNameForm.GREY, null)
    val h = hueOf(r, g, b)!! // c >= .06 so the chroma is not 0
    val hn = hueName(h)
    val warm = h < 62 || h >= 355
    if (c < .18) {
        val word = if (warm) (if (l > .5) InkHueWord.BEIGE else InkHueWord.BROWN) else hn
        return InkColourName(tone, InkNameForm.TINTED_GREY, word)
    }
    if (hn == InkHueWord.ORANGE && l < .45) return InkColourName(tone, InkNameForm.HUE, InkHueWord.BROWN)
    return InkColourName(tone, InkNameForm.HUE, hn)
}
