package com.aritr.zinely.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The two document voices and what each can set (ADR-126). Pure JVM. Given-When-Then.
 */
class DocumentVoiceTest {

    @Test
    fun `sans-serif and Inter are both Plain, Fraunces is Book`() {
        assertEquals(DocumentVoice.PLAIN, DocumentVoice.of("sans-serif"))
        assertEquals(DocumentVoice.PLAIN, DocumentVoice.of("Inter"))
        assertEquals(DocumentVoice.BOOK, DocumentVoice.of("Fraunces"))
        // The font registry matches trimmed and case-insensitively; the voice table must agree with it.
        assertEquals(DocumentVoice.PLAIN, DocumentVoice.of("  INTER "))
        assertEquals(DocumentVoice.BOOK, DocumentVoice.of("fraunces"))
    }

    @Test
    fun `a family this build does not know is no voice, and is drawn as Plain`() {
        assertNull(DocumentVoice.of("Averia Sans Libre"))
        assertNull(DocumentVoice.of(""))
        assertEquals(DocumentVoice.PLAIN, DocumentVoice.drawnAs("Averia Sans Libre"))
        assertEquals(DocumentVoice.BOOK, DocumentVoice.drawnAs("Fraunces"))
    }

    @Test
    fun `choosing Plain writes what every text has always held`() {
        // A text returned to Plain must be byte-identical to one never touched (ADR-126 decision 1).
        assertEquals(TextStyle().fontFamily, DocumentVoice.PLAIN.familyName)
        assertEquals("Fraunces", DocumentVoice.BOOK.familyName)
    }

    @Test
    fun `Plain sets exactly what was promised before voices, Book sets Latin only`() {
        assertEquals(SupportedScripts.BUNDLED_SCRIPTS, DocumentVoice.PLAIN.scripts)
        assertEquals(setOf(Script.LATIN, Script.EMOJI), DocumentVoice.BOOK.scripts)
        // Plain sets every script Book sets, so going back to Plain is always possible.
        assertTrue(DocumentVoice.PLAIN.scripts.containsAll(DocumentVoice.BOOK.scripts))
    }

    @Test
    fun `the minimum print size for Book is one named value`() {
        assertEquals(12.0, DocumentVoice.BOOK_MIN_SIZE_PT)
    }

    // ---- analyzeTextCoverage(text, family) ----

    private val greek = "Καλημέρα"
    private val cyrillic = "Привет"

    @Test
    fun `Greek and Cyrillic in a Book text are reported, by name`() {
        val coverage = analyzeTextCoverage("Saturday. $greek, $cyrillic", "Fraunces")

        assertFalse(coverage.isFullyCovered)
        assertEquals(listOf(Script.GREEK, Script.CYRILLIC), coverage.unsupportedScripts)
        assertEquals(greek.length + cyrillic.length, coverage.unsupportedCount)
    }

    @Test
    fun `in Plain they raise nothing, with or without the family named`() {
        for (plain in listOf("sans-serif", "Inter")) {
            assertTrue(analyzeTextCoverage("$greek $cyrillic", plain).isFullyCovered)
        }
        assertEquals(analyzeTextCoverage("$greek $cyrillic"), analyzeTextCoverage("$greek $cyrillic", "sans-serif"))
    }

    @Test
    fun `an unknown family is checked as Plain, because Inter draws it`() {
        assertTrue(analyzeTextCoverage("$greek $cyrillic", "Averia Sans Libre").isFullyCovered)
        assertEquals(analyzeTextCoverage("שלום"), analyzeTextCoverage("שלום", "Averia Sans Libre"))
    }

    @Test
    fun `a script no voice sets is still reported in Plain exactly as before`() {
        val before = analyzeTextCoverage("hello שלום 你好")

        assertEquals(listOf(Script.HEBREW, Script.HAN), before.unsupportedScripts)
        assertEquals(before, analyzeTextCoverage("hello שלום 你好", "Inter"))
        // …and in Book too, alongside nothing else.
        assertEquals(before, analyzeTextCoverage("hello שלום 你好", "Fraunces"))
    }

    @Test
    fun `line breaks, digits, punctuation and emoji raise nothing in either voice`() {
        val neutral = "Line one\nLine two\t0123456789 — “quoted” … £5 🙂 ❤️"
        for (family in listOf("sans-serif", "Inter", "Fraunces", "Averia Sans Libre")) {
            assertTrue(analyzeTextCoverage(neutral, family).isFullyCovered, family)
        }
    }

    @Test
    fun `Latin with its accents is covered in Book`() {
        assertTrue(analyzeTextCoverage("Zażółć gęślą jaźń — Ångström café naïve", "Fraunces").isFullyCovered)
    }
}
