package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.DocumentVoice
import com.aritr.zinely.core.model.TextStyle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The Font row's derived state (frozen `v21-typebar.html` A29 rules 7 to 10; ADR-126). Each case is one
 * of the prototype's states, or an edge its script decides. Pure. Given-When-Then.
 */
class FontRowTest {
    private val rampMin = 10.0
    private val min = DocumentVoice.BOOK_MIN_SIZE_PT
    private val latin = "Saturday, and nothing planned"
    private val greek = "$latin. Καλημέρα"

    private fun row(text: String = latin, family: String = "sans-serif", sizePt: Double = 16.0) =
        fontRow(text, TextStyle(fontFamily = family, sizePt = sizePt), rampMin)

    @Test
    fun `a Plain text Book can set, nothing to say`() {
        assertEquals(FontRow(DocumentVoice.PLAIN, bookBlock = null, smallerBlocked = false, line = null), row())
        assertEquals(DocumentVoice.PLAIN, row(family = "Inter").voice)
    }

    @Test
    fun `a Book text above the minimum, nothing to say`() {
        assertEquals(FontRow(DocumentVoice.BOOK, bookBlock = null, smallerBlocked = false, line = null), row(family = "Fraunces"))
    }

    @Test
    fun `Greek, Cyrillic or both rule Book out, and the reason names what was found`() {
        assertEquals(FontReason.BookLacksScript(greek = true, cyrillic = false), row(greek).bookBlock)
        assertEquals(FontReason.BookLacksScript(greek = false, cyrillic = true), row("Привет").bookBlock)
        assertEquals(FontReason.BookLacksScript(greek = true, cyrillic = true), row("Καλημέρα Привет").bookBlock)
        // The letters beyond Plain's own promise are still Greek and Cyrillic to a maker.
        assertEquals(FontReason.BookLacksScript(greek = true, cyrillic = false), row("ἀ").bookBlock)
        assertEquals(FontReason.BookLacksScript(greek = false, cyrillic = true), row("Ԁ").bookBlock)
        assertEquals(row(greek).bookBlock, row(greek).line)
    }

    @Test
    fun `a script no voice sets is not a reason to refuse Book`() {
        assertNull(row("hello שלום 你好 🙂").bookBlock)
    }

    @Test
    fun `below the minimum Book needs size, at the minimum it does not`() {
        assertEquals(FontReason.BookNeedsSize, row(sizePt = 10.0).bookBlock)
        assertEquals(FontReason.BookNeedsSize, row(sizePt = 11.9).line)
        assertNull(row(sizePt = min).bookBlock)
    }

    @Test
    fun `the script outranks the size`() {
        assertEquals(FontReason.BookLacksScript(greek = true, cyrillic = false), row(greek, sizePt = 10.0).line)
    }

    @Test
    fun `a Book text at the minimum cannot go smaller, and says so`() {
        val floor = row(family = "Fraunces", sizePt = min)

        assertTrue(floor.smallerBlocked)
        assertEquals(FontReason.BookAtFloor, floor.line)
        assertNull(floor.bookBlock) // the chosen word is never shown unavailable
        assertFalse(row(family = "Fraunces", sizePt = 14.0).smallerBlocked)
        // A Plain text at the same size steps down freely.
        assertFalse(row(sizePt = min).smallerBlocked)
    }

    @Test
    fun `a Book text already below the minimum is drawn as it is - Smaller refused, nothing changed for it`() {
        // Made on another build, or before a later ruling. Between the ramp's bottom and the minimum:
        val between = row(family = "Fraunces", sizePt = 11.0)
        assertTrue(between.smallerBlocked)
        assertEquals(DocumentVoice.BOOK, between.voice)
        // At the ramp's own bottom Smaller is truly unavailable for every text; Book's floor adds nothing.
        val bottom = row(family = "Fraunces", sizePt = rampMin)
        assertFalse(bottom.smallerBlocked)
        assertNull(bottom.line)
    }

    @Test
    fun `an unknown font - neither word chosen, and its line shows ahead of Book's own reason`() {
        val unknown = row(family = "Averia Sans Libre")
        assertNull(unknown.voice)
        assertEquals(FontReason.UnknownFont, unknown.line)
        assertNull(unknown.bookBlock)
        assertFalse(unknown.smallerBlocked)

        // Book also ruled out: the unknown-font line still shows; a tap on Book says Book's reason.
        val both = row(greek, family = "Averia Sans Libre", sizePt = 10.0)
        assertEquals(FontReason.UnknownFont, both.line)
        assertEquals(FontReason.BookLacksScript(greek = true, cyrillic = false), both.bookBlock)
    }

    @Test
    fun `if the printed page brings the minimum down to the ramp's bottom, both size states never happen`() {
        // Not a second code path: with rampMin == minimum the arithmetic already yields nothing.
        val atRampBottom = fontRow(latin, TextStyle(fontFamily = "Fraunces", sizePt = min), rampMinPt = min)
        assertFalse(atRampBottom.smallerBlocked)
        assertNull(fontRow(latin, TextStyle(sizePt = min), rampMinPt = min).bookBlock)
    }
}
