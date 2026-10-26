package com.aritr.zinely.core.editor.ink

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The structured column of the prototype NAME_TABLE (docs/design/mockups/v21-ink-mixer.html).
 * The expected words for the same rows are asserted in Task 5, so a word typo and a branch error
 * fail different tests.
 */
internal val NAME_TABLE: List<Pair<String, InkColourName>> = listOf(
    "505955" to n(InkTone.DARK, InkNameForm.GREY),
    "2A251E" to n(InkTone.NONE, InkNameForm.NEAR_BLACK),
    "000000" to n(InkTone.NONE, InkNameForm.NEAR_BLACK),
    "FFFFFF" to n(InkTone.NONE, InkNameForm.WHITE),
    "808080" to n(InkTone.NONE, InkNameForm.GREY),
    "FF0000" to n(InkTone.NONE, InkNameForm.HUE, InkHueWord.RED),
    "B0503F" to n(InkTone.NONE, InkNameForm.HUE, InkHueWord.RED),
    "A52A2A" to n(InkTone.NONE, InkNameForm.HUE, InkHueWord.RED),
    "6B3E1E" to n(InkTone.DARK, InkNameForm.HUE, InkHueWord.BROWN),
    "D19A3C" to n(InkTone.NONE, InkNameForm.HUE, InkHueWord.ORANGE),
    "E7B53C" to n(InkTone.NONE, InkNameForm.HUE, InkHueWord.YELLOW),
    "FFFF00" to n(InkTone.NONE, InkNameForm.HUE, InkHueWord.YELLOW),
    "7C8A3F" to n(InkTone.NONE, InkNameForm.HUE, InkHueWord.YELLOW_GREEN),
    "3E5E3A" to n(InkTone.DARK, InkNameForm.TINTED_GREY, InkHueWord.GREEN),
    "00FF00" to n(InkTone.NONE, InkNameForm.HUE, InkHueWord.GREEN),
    "008080" to n(InkTone.DARK, InkNameForm.HUE, InkHueWord.CYAN),
    "00FFFF" to n(InkTone.NONE, InkNameForm.HUE, InkHueWord.CYAN),
    "57B0A9" to n(InkTone.NONE, InkNameForm.HUE, InkHueWord.CYAN),
    "2E6F95" to n(InkTone.NONE, InkNameForm.HUE, InkHueWord.BLUE),
    "6E86C9" to n(InkTone.NONE, InkNameForm.HUE, InkHueWord.BLUE),
    "0000FF" to n(InkTone.NONE, InkNameForm.HUE, InkHueWord.BLUE),
    "8A5A9B" to n(InkTone.NONE, InkNameForm.HUE, InkHueWord.PURPLE),
    "7A3B69" to n(InkTone.NONE, InkNameForm.HUE, InkHueWord.MAGENTA),
    "FF00FF" to n(InkTone.NONE, InkNameForm.HUE, InkHueWord.MAGENTA),
    "E27F89" to n(InkTone.NONE, InkNameForm.HUE, InkHueWord.PINK),
    "FFC0CB" to n(InkTone.LIGHT, InkNameForm.HUE, InkHueWord.PINK),
    "F2D7A6" to n(InkTone.LIGHT, InkNameForm.HUE, InkHueWord.ORANGE),
    "B7AD93" to n(InkTone.NONE, InkNameForm.TINTED_GREY, InkHueWord.BEIGE),
    "8C8269" to n(InkTone.NONE, InkNameForm.TINTED_GREY, InkHueWord.BROWN),
    "5B5347" to n(InkTone.DARK, InkNameForm.TINTED_GREY, InkHueWord.BROWN),
    "F1E9D6" to n(InkTone.LIGHT, InkNameForm.TINTED_GREY, InkHueWord.BEIGE),
    "DDE9EE" to n(InkTone.LIGHT, InkNameForm.TINTED_GREY, InkHueWord.CYAN),
    "C0392B" to n(InkTone.NONE, InkNameForm.HUE, InkHueWord.RED),
    "1F7A5C" to n(InkTone.DARK, InkNameForm.HUE, InkHueWord.TEAL),
    "D4B000" to n(InkTone.NONE, InkNameForm.HUE, InkHueWord.YELLOW),
    "8C1C13" to n(InkTone.DARK, InkNameForm.HUE, InkHueWord.RED),
    "E8A87C" to n(InkTone.NONE, InkNameForm.HUE, InkHueWord.ORANGE),
    "6B4E71" to n(InkTone.NONE, InkNameForm.TINTED_GREY, InkHueWord.PURPLE),
)

private fun n(tone: InkTone, form: InkNameForm, word: InkHueWord? = null) = InkColourName(tone, form, word)

class InkColourNameTest {
    @Test fun `table has the prototype 38 rows`() = assertEquals(38, NAME_TABLE.size)

    @Test fun `given each table hex when named then the structured form matches`() {
        val bad = NAME_TABLE.filter { (hex, want) -> colourName(InkHex(hex)) != want }
            .map { (hex, want) -> "$hex want $want got ${colourName(InkHex(hex))}" }
        assertEquals(emptyList<String>(), bad)
    }
}
