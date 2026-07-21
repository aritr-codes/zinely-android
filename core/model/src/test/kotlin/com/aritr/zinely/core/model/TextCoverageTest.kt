package com.aritr.zinely.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Input-time coverage detection (DoD 4, [ZINELY V1] §7 blocker #1): the app must flag characters it
 * cannot print **at typing time, kindly, before any work is lost** — and must name what it cannot print
 * rather than gesturing at "some characters". Pure JVM, no Android. Given-When-Then.
 */
class TextCoverageTest {

    @Test
    fun `the ratified script set is fully covered`() {
        // V1 §5 ratified Latin/Latin-Ext, Cyrillic and Greek — all three must pass cleanly.
        assertTrue(analyzeTextCoverage("The quick brown fox").isFullyCovered)
        assertTrue(analyzeTextCoverage("Ångström café naïve — Æøå").isFullyCovered)
        assertTrue(analyzeTextCoverage("Привет, мир").isFullyCovered)
        assertTrue(analyzeTextCoverage("Καλημέρα κόσμε").isFullyCovered)
        assertTrue(analyzeTextCoverage("Zażółć gęślą jaźń").isFullyCovered) // Latin Extended-A
    }

    @Test
    fun `empty text is covered`() {
        assertTrue(analyzeTextCoverage("").isFullyCovered)
        assertEquals(TextCoverage.Covered, analyzeTextCoverage(""))
    }

    @Test
    fun `script-neutral characters never trip the warning`() {
        // Flagging a space, a digit or a full stop would be noise that teaches the user to ignore the
        // warning entirely — the failure mode worse than not warning at all.
        assertTrue(analyzeTextCoverage(" \n\t0123456789").isFullyCovered)
        assertTrue(analyzeTextCoverage("!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~").isFullyCovered)
        assertTrue(analyzeTextCoverage("£ € © ® ° « » — – … • “ ” ‘ ’").isFullyCovered)
        assertTrue(analyzeTextCoverage("2 × 3 ÷ 4").isFullyCovered)
    }

    @Test
    fun `decomposed accents are covered - a false positive here would poison the warning`() {
        // `é` in NFD is `e` + U+0301 COMBINING ACUTE, which is what iOS and macOS keyboards routinely
        // produce. Flagging it would tell a user that ordinary French cannot be printed — and a warning
        // that fires on legitimate text teaches people to dismiss it, costing us the one moment DoD 4
        // exists to protect. Caught during F3 Increment 2; combining marks were previously unclassified.
        assertTrue(analyzeTextCoverage("café").isFullyCovered) // café, decomposed
        assertTrue(analyzeTextCoverage("naïve").isFullyCovered) // naïve, decomposed
        assertTrue(analyzeTextCoverage("François").isFullyCovered) // François, decomposed
        // Composed and decomposed forms must agree — the user cannot tell them apart.
        assertEquals(
            analyzeTextCoverage("café").isFullyCovered,
            analyzeTextCoverage("café").isFullyCovered,
        )
        // Greek and Cyrillic take combining marks too.
        assertTrue(analyzeTextCoverage("ά").isFullyCovered) // ά decomposed
    }

    @Test
    fun `latin-adjacent letters and modifiers are covered`() {
        assertTrue(analyzeTextCoverage("ə").isFullyCovered) // U+0259 schwa, Latin Extended-B
        assertTrue(analyzeTextCoverage("ʼn").isFullyCovered) // U+02BC modifier apostrophe + n
        assertTrue(analyzeTextCoverage("Việt Nam").isFullyCovered) // Latin Extended Additional
        assertTrue(analyzeTextCoverage("soft­hyphen").isFullyCovered)
        assertTrue(analyzeTextCoverage("non breaking").isFullyCovered)
    }

    @Test
    fun `out-of-scope scripts are detected and named`() {
        // Naming the script is the whole point: "Arabic isn't supported yet" is a kind refusal;
        // "some characters won't print" is a shrug.
        val arabic = analyzeTextCoverage("hello مرحبا")
        assertFalse(arabic.isFullyCovered)
        assertEquals(listOf(Script.ARABIC), arabic.unsupportedScripts)

        assertEquals(listOf(Script.HAN), analyzeTextCoverage("hello 世界").unsupportedScripts)
        assertEquals(listOf(Script.DEVANAGARI), analyzeTextCoverage("नमस्ते").unsupportedScripts)
        assertEquals(listOf(Script.HEBREW), analyzeTextCoverage("שלום").unsupportedScripts)
        assertEquals(listOf(Script.THAI), analyzeTextCoverage("สวัสดี").unsupportedScripts)
        assertEquals(listOf(Script.HANGUL), analyzeTextCoverage("안녕하세요").unsupportedScripts)
    }

    @Test
    fun `emoji are reported separately because they fail for a different reason`() {
        // Colour emoji cannot be embedded in a PDF at all (ADR-001) — a limit of print, not a missing
        // font, so the explanation a user needs is different.
        val result = analyzeTextCoverage("party 🎉")
        assertFalse(result.isFullyCovered)
        assertEquals(listOf(Script.EMOJI), result.unsupportedScripts)
    }

    @Test
    fun `an astral character counts once, not twice`() {
        // 🎉 is a surrogate pair: iterating chars rather than code points would double-count it and
        // report a nonsense "2 characters" to the user.
        assertEquals(1, analyzeTextCoverage("🎉").unsupportedCount)
        assertEquals(listOf("🎉"), analyzeTextCoverage("🎉").sampleCharacters)
    }

    @Test
    fun `multiple scripts are reported in first-appearance order`() {
        val result = analyzeTextCoverage("a 世 ب 世")
        assertEquals(listOf(Script.HAN, Script.ARABIC), result.unsupportedScripts)
        assertEquals(3, result.unsupportedCount) // 世 twice + ب
    }

    @Test
    fun `samples are deduplicated and bounded`() {
        val result = analyzeTextCoverage("世界你好朋友们再见吧")

        assertTrue(result.sampleCharacters.size <= TextCoverage.MAX_SAMPLES)
        assertEquals(result.sampleCharacters.distinct(), result.sampleCharacters)
        // The full count is still honest even though the samples are capped.
        assertEquals(10, result.unsupportedCount)
    }

    @Test
    fun `mixed supported and unsupported text reports only the unsupported part`() {
        val result = analyzeTextCoverage("Hello Привет Καλημέρα 世界")

        assertFalse(result.isFullyCovered)
        assertEquals(listOf(Script.HAN), result.unsupportedScripts)
        assertEquals(2, result.unsupportedCount)
    }

    @Test
    fun `the promise stops where the bundled fonts do`() {
        // Cyrillic Supplement (Komi/Abkhaz) and polytonic Greek are NOT promised: 47 of the 48 assigned
        // Cyrillic Supplement code points are absent from every bundled Inter face, so classifying them
        // supported would send a character to paper blank with no warning — the DoD 4 failure this file
        // exists to prevent. They are still NAMED, so the refusal can say which script it is.
        val komi = analyzeTextCoverage("Ԁԁ") // U+0500 U+0501
        assertFalse(komi.isFullyCovered)
        assertEquals(listOf(Script.CYRILLIC_EXTENDED), komi.unsupportedScripts)

        val polytonic = analyzeTextCoverage("ἀρχή") // U+1F00 polytonic alpha
        assertFalse(polytonic.isFullyCovered)
        assertEquals(listOf(Script.GREEK_EXTENDED), polytonic.unsupportedScripts)

        // ...while the modern alphabets they extend remain fully supported.
        assertTrue(analyzeTextCoverage("Привет").isFullyCovered)
        assertTrue(analyzeTextCoverage("Καλημέρα").isFullyCovered)
    }

    @Test
    fun `script classification matches the ratified set exactly`() {
        assertEquals(setOf(Script.LATIN, Script.CYRILLIC, Script.GREEK), SupportedScripts.BUNDLED_SCRIPTS)
        assertTrue(SupportedScripts.isSupported('A'.code))
        assertTrue(SupportedScripts.isSupported('Ж'.code))
        assertTrue(SupportedScripts.isSupported('Ω'.code))
        assertFalse(SupportedScripts.isSupported(0x4E16)) // 世
        assertFalse(SupportedScripts.isSupported(0x0627)) // ا
    }
}
