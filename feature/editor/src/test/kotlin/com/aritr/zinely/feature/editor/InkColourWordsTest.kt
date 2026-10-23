package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.editor.ink.InkHex
import com.aritr.zinely.core.editor.ink.InkTone
import com.aritr.zinely.core.editor.ink.InkHueWord
import com.aritr.zinely.core.editor.ink.InkNameForm
import com.aritr.zinely.core.editor.ink.InkColourName
import com.aritr.zinely.core.editor.ink.colourName
import com.aritr.zinely.ui.theme.zinelyContentInks
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The prototype's 38-row `NAME_TABLE` (`v21-ink-mixer.html`), expected words verbatim. The structured column
 * is `:core:editor`'s `InkColourNameTest`; this one is the join, so a word typo and a branch error fail
 * different tests.
 */
class InkColourWordsTest {
    private val table = listOf(
        "505955" to "dark grey",
        "2A251E" to "near-black",
        "000000" to "near-black",
        "FFFFFF" to "white",
        "808080" to "grey",
        "FF0000" to "red",
        "B0503F" to "red",
        "A52A2A" to "red",
        "6B3E1E" to "dark brown",
        "D19A3C" to "orange",
        "E7B53C" to "yellow",
        "FFFF00" to "yellow",
        "7C8A3F" to "yellow-green",
        "3E5E3A" to "dark green-grey",
        "00FF00" to "green",
        "008080" to "dark cyan",
        "00FFFF" to "cyan",
        "57B0A9" to "cyan",
        "2E6F95" to "blue",
        "6E86C9" to "blue",
        "0000FF" to "blue",
        "8A5A9B" to "purple",
        "7A3B69" to "magenta",
        "FF00FF" to "magenta",
        "E27F89" to "pink",
        "FFC0CB" to "light pink",
        "F2D7A6" to "light orange",
        "B7AD93" to "beige-grey",
        "8C8269" to "brown-grey",
        "5B5347" to "dark brown-grey",
        "F1E9D6" to "light beige-grey",
        "DDE9EE" to "light cyan-grey",
        "C0392B" to "red",
        "1F7A5C" to "dark teal",
        "D4B000" to "yellow",
        "8C1C13" to "dark red",
        "E8A87C" to "orange",
        "6B4E71" to "purple-grey",
    )

    @Test fun `table has the prototype 38 rows`() = assertEquals(38, table.size)

    @Test fun `given each table hex when worded then the words match the prototype`() {
        val bad = table.filter { (hex, want) -> inkColourWords(colourName(InkHex(hex))) != want }
            .map { (hex, want) -> "$hex want $want got ${inkColourWords(colourName(InkHex(hex)))}" }
        assertEquals(emptyList<String>(), bad)
    }

    @Test fun `every hue word has its own words, so a new enum value cannot be blank`() {
        InkHueWord.entries.forEach { w ->
            val words = inkColourWords(InkColourName(InkTone.NONE, InkNameForm.HUE, w))
            assertEquals(false, words.isBlank())
        }
    }

    private val inks = zinelyContentInks()

    @Test fun `given a built-in hex when named then the built-in's own name wins`() {
        assertEquals("Matcha", inkName(InkHex("7C8A3F"), inks, BenchVerbKind.TEXT))
        assertEquals("Slate", inkName(InkHex("5B5347"), inks, BenchVerbKind.TEXT))
    }

    @Test fun `given a paper tint hex then only a target that takes paper names it`() {
        assertEquals("light beige-grey", inkName(InkHex("F1E9D6"), inks, BenchVerbKind.TEXT))
        assertEquals("Cream", inkName(InkHex("F1E9D6"), inks, BenchVerbKind.DECOR))
    }

    @Test fun `given a hex that is no built-in then the colour words name it`() {
        assertEquals("dark teal", inkName(InkHex("1F7A5C"), inks, BenchVerbKind.DECOR))
    }
}
