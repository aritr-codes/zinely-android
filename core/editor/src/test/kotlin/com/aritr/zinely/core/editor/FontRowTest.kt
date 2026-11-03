package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.DocumentVoice
import com.aritr.zinely.core.model.TextStyle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The Font row's derived state (frozen `v21-typebar.html` A29 rules 7 to 10 and A29H; ADR-126, ADR-TBD-W9).
 * Each case is one of the prototype's states, or an edge its script decides. Pure. Given-When-Then.
 */
class FontRowTest {
    private val rampMin = 10.0
    private val min = DocumentVoice.BOOK_MIN_SIZE_PT
    private val latin = "Saturday, and nothing planned"
    private val greek = "$latin. Καλημέρα"
    private val both = listOf(DocumentVoice.BOOK, DocumentVoice.HAND)

    private fun row(text: String = latin, family: String = "sans-serif", sizePt: Double = 16.0) =
        fontRow(text, TextStyle(fontFamily = family, sizePt = sizePt), rampMin)

    private fun lacks(voices: List<DocumentVoice>, stays: DocumentVoice, greek: Boolean = true, cyrillic: Boolean = false) =
        FontReason.LacksScript(voices, stays, greek, cyrillic)

    @Test
    fun `a Plain text Book and Hand can set, nothing to say`() {
        assertEquals(FontRow(DocumentVoice.PLAIN, blocks = emptyMap(), smallerBlocked = false, line = null), row())
        assertEquals(DocumentVoice.PLAIN, row(family = "Inter").voice)
    }

    @Test
    fun `a Book text and a Hand text above the minimum, nothing to say`() {
        assertEquals(FontRow(DocumentVoice.BOOK, emptyMap(), smallerBlocked = false, line = null), row(family = "Fraunces"))
        assertEquals(FontRow(DocumentVoice.HAND, emptyMap(), smallerBlocked = false, line = null), row(family = "Shantell Sans"))
    }

    @Test
    fun `Greek, Cyrillic or both rule Book and Hand out together, and the line names both`() {
        assertEquals(lacks(both, DocumentVoice.PLAIN), row(greek).line)
        assertEquals(lacks(both, DocumentVoice.PLAIN, greek = false, cyrillic = true), row("Привет").line)
        assertEquals(lacks(both, DocumentVoice.PLAIN, greek = true, cyrillic = true), row("Καλημέρα Привет").line)
        // The letters beyond Plain's own promise are still Greek and Cyrillic to a maker.
        assertEquals(lacks(both, DocumentVoice.PLAIN), row("ἀ").line)
        assertEquals(lacks(both, DocumentVoice.PLAIN, greek = false, cyrillic = true), row("Ԁ").line)
    }

    @Test
    fun `a tap on one quiet word gets that word's own reason`() {
        val r = row(greek)

        assertEquals(setOf(DocumentVoice.BOOK, DocumentVoice.HAND), r.blocks.keys)
        assertEquals(lacks(listOf(DocumentVoice.BOOK), DocumentVoice.PLAIN), r.blocks.getValue(DocumentVoice.BOOK))
        assertEquals(lacks(listOf(DocumentVoice.HAND), DocumentVoice.PLAIN), r.blocks.getValue(DocumentVoice.HAND))
    }

    @Test
    fun `in a Book text that holds Greek, only Hand is quiet and the text stays Book`() {
        val r = row(greek, family = "Fraunces")

        assertEquals(setOf(DocumentVoice.HAND), r.blocks.keys)
        assertEquals(lacks(listOf(DocumentVoice.HAND), DocumentVoice.BOOK), r.line)
        assertNull(r.blocks[DocumentVoice.BOOK]) // the chosen word is never shown unavailable
    }

    @Test
    fun `in a Hand text that holds Cyrillic, only Book is quiet and the text stays Hand`() {
        val r = row("Привет", family = "Shantell Sans")

        assertEquals(setOf(DocumentVoice.BOOK), r.blocks.keys)
        assertEquals(lacks(listOf(DocumentVoice.BOOK), DocumentVoice.HAND, greek = false, cyrillic = true), r.line)
    }

    @Test
    fun `a script no voice sets is not a reason to refuse Book or Hand`() {
        assertTrue(row("hello שלום 你好 🙂").blocks.isEmpty())
    }

    @Test
    fun `below the minimum both need size, at the minimum neither does`() {
        assertEquals(FontReason.NeedsSize(both), row(sizePt = 10.0).line)
        assertEquals(FontReason.NeedsSize(both), row(sizePt = 11.9).line)
        assertEquals(FontReason.NeedsSize(listOf(DocumentVoice.HAND)), row(sizePt = 11.9).blocks.getValue(DocumentVoice.HAND))
        assertTrue(row(sizePt = min).blocks.isEmpty())
        assertTrue(row(sizePt = DocumentVoice.HAND_MIN_SIZE_PT).blocks.isEmpty())
    }

    @Test
    fun `each voice has its own minimum, equal today`() {
        assertEquals(12.0, DocumentVoice.BOOK.minSizePt)
        assertEquals(12.0, DocumentVoice.HAND.minSizePt)
        assertNull(DocumentVoice.PLAIN.minSizePt)
    }

    @Test
    fun `the script outranks the size`() {
        assertEquals(lacks(both, DocumentVoice.PLAIN), row(greek, sizePt = 10.0).line)
    }

    @Test
    fun `a Hand text under the minimum keeps Hand chosen and tells Book alone what it needs`() {
        val r = row(family = "Shantell Sans", sizePt = 11.0)

        assertEquals(DocumentVoice.HAND, r.voice)
        assertEquals(setOf(DocumentVoice.BOOK), r.blocks.keys)
        assertEquals(FontReason.NeedsSize(listOf(DocumentVoice.BOOK)), r.blocks.getValue(DocumentVoice.BOOK))
        assertEquals(FontReason.AtFloor(DocumentVoice.HAND), r.line) // Smaller is the thing to say first
    }

    @Test
    fun `a Book or Hand text at the minimum cannot go smaller, and says so`() {
        for (voice in listOf(DocumentVoice.BOOK, DocumentVoice.HAND)) {
            val floor = row(family = voice.familyName, sizePt = min)

            assertTrue(floor.smallerBlocked)
            assertEquals(FontReason.AtFloor(voice), floor.line)
            assertNull(floor.blocks[voice])
            assertFalse(row(family = voice.familyName, sizePt = 14.0).smallerBlocked)
        }
        // A Plain text at the same size steps down freely.
        assertFalse(row(sizePt = min).smallerBlocked)
    }

    @Test
    fun `a text already below the minimum is drawn as it is - Smaller refused, nothing changed for it`() {
        // Made on another build, or before a later ruling. Between the ramp's bottom and the minimum:
        for (voice in listOf(DocumentVoice.BOOK, DocumentVoice.HAND)) {
            val between = row(family = voice.familyName, sizePt = 11.0)
            assertTrue(between.smallerBlocked)
            assertEquals(voice, between.voice)
            // At the ramp's own bottom Smaller is truly unavailable for every text; the floor adds nothing.
            val bottom = row(family = voice.familyName, sizePt = rampMin)
            assertFalse(bottom.smallerBlocked)
        }
    }

    @Test
    fun `an unknown font - no word chosen, and its line shows ahead of any other reason`() {
        val unknown = row(family = "Averia Sans Libre")
        assertNull(unknown.voice)
        assertEquals(FontReason.UnknownFont, unknown.line)
        assertTrue(unknown.blocks.isEmpty())
        assertFalse(unknown.smallerBlocked)

        // Book and Hand also ruled out: the unknown-font line still shows; a tap on a word says its reason.
        val ruledOut = row(greek, family = "Averia Sans Libre", sizePt = 10.0)
        assertEquals(FontReason.UnknownFont, ruledOut.line)
        assertEquals(lacks(listOf(DocumentVoice.BOOK), DocumentVoice.PLAIN), ruledOut.blocks.getValue(DocumentVoice.BOOK))
        assertEquals(lacks(listOf(DocumentVoice.HAND), DocumentVoice.PLAIN), ruledOut.blocks.getValue(DocumentVoice.HAND))
    }

    @Test
    fun `if the printed page brings the minimum down to the ramp's bottom, both size states never happen`() {
        // Not a second code path: with rampMin == minimum the arithmetic already yields nothing.
        val atRampBottom = fontRow(latin, TextStyle(fontFamily = "Shantell Sans", sizePt = min), rampMinPt = min)
        assertFalse(atRampBottom.smallerBlocked)
        assertTrue(fontRow(latin, TextStyle(sizePt = min), rampMinPt = min).blocks.isEmpty())
    }
}
