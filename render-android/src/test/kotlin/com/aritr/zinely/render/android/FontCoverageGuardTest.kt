package com.aritr.zinely.render.android

import com.aritr.zinely.core.model.Script
import com.aritr.zinely.core.model.SupportedScripts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * **The promise-keeping guard (F3 Increment 2).**
 *
 * `:core:model` promises which scripts v1 renders; this asserts the bundled font files can actually
 * deliver that promise, measured from each TTF's own `cmap`. If these two ever disagree the app is
 * promising something it cannot print, which surfaces as blank glyphs on paper — the Article 5 violation
 * DoD 4 exists to prevent.
 *
 * This is the test that must fail *loudly* when a font is swapped or a script is added to the ratified
 * set without the glyphs to back it.
 */
@RunWith(RobolectricTestRunner::class)
class FontCoverageGuardTest {

    private val assets = RuntimeEnvironment.getApplication().assets

    @Test
    fun everyBundledFamilyCoversTheRatifiedScripts() {
        val incomplete = FontCoverage.incompleteFamilies(assets)

        assertTrue(
            "bundled fonts must cover every ratified script; missing: " +
                incomplete.joinToString { r ->
                    "${r.familyName} lacks ${r.missing.size} code points " +
                        r.missing.take(12).joinToString(prefix = "[", postfix = "]") { "U+%04X".format(it) }
                },
            incomplete.isEmpty(),
        )
    }

    @Test
    fun theGuardCoversEveryScriptTheAppPromises() {
        // Inter owns the alphabetic subset. Emoji are verified against their separate bundled font by
        // EmojiRenderingInstrumentedTest on the real Android graphics/PDF stack (ADR-112).
        assertEquals(
            SupportedScripts.BUNDLED_SCRIPTS - Script.EMOJI,
            FontCoverage.guardedScripts,
        )
    }

    @Test
    fun theRequiredSetActuallySpansTheThreeScripts() {
        val required = FontCoverage.requiredCodePoints()

        // A guard that probed only ASCII would pass trivially while proving nothing about Cyrillic or
        // Greek — so assert the probe set itself reaches all three ratified scripts.
        assertTrue("Latin", required.any { SupportedScripts.scriptOf(it) == Script.LATIN })
        assertTrue("Cyrillic", required.any { SupportedScripts.scriptOf(it) == Script.CYRILLIC })
        assertTrue("Greek", required.any { SupportedScripts.scriptOf(it) == Script.GREEK })
        assertTrue("meaningful size", required.size > 300)
    }

    @Test
    fun intersRequiredSetIsWhatItWasBeforeVoices() {
        // ADR-126 stop condition: the per-family guard must not change what Inter is held to.
        assertEquals(FontCoverage.requiredCodePoints(), FontCoverage.requiredCodePoints(DocumentFontRegistry.INTER))
        assertEquals(FontCoverage.requiredCodePoints(), FontCoverage.requiredCodePoints("sans-serif"))
        // A family this build does not know is drawn in Inter, so it is held to Inter's set.
        assertEquals(FontCoverage.requiredCodePoints(), FontCoverage.requiredCodePoints("Averia Sans Libre"))
    }

    @Test
    fun booksRequiredSetIsTheLatinPartLessItsOneRecordedGap() {
        val all = FontCoverage.requiredCodePoints()
        val book = FontCoverage.requiredCodePoints(DocumentFontRegistry.FRAUNCES)
        val latin = all.filter { SupportedScripts.scriptOf(it) == Script.LATIN }.toSet()

        assertEquals(latin - 0x017F, book)
        assertTrue("the gap is a real member of the Latin set", 0x017F in latin)
        assertTrue("no Greek", book.none { SupportedScripts.scriptOf(it) == Script.GREEK })
        assertTrue("no Cyrillic", book.none { SupportedScripts.scriptOf(it) == Script.CYRILLIC })
        assertTrue("meaningful size", book.size > 250)
    }

    @Test
    fun theScriptTableAndTheBookFilesAgree() {
        // The other direction of the promise: Book declares no Greek and no Cyrillic BECAUSE its four
        // faces hold none. If a later Fraunces gained them, this is the prompt to widen the table.
        val book = DocumentFontRegistry.Bundled.resolve(DocumentFontRegistry.FRAUNCES)
        for (asset in listOf(book.regularAsset, book.boldAsset, book.italicAsset, book.boldItalicAsset)) {
            val covered = CmapCoverage.coveredCodePoints(assets.open(asset).use { it.readBytes() })
            assertTrue("$asset holds Greek", (0x0370..0x03FF).none { it in covered })
            assertTrue("$asset holds Cyrillic", (0x0400..0x04FF).none { it in covered })
            assertFalse("$asset holds the long s it is recorded as lacking", 0x017F in covered)
        }
    }

    @Test
    fun theGuardWouldCatchBookDeclaringAScriptItsFilesLack() {
        // What "the script table and the font files disagree" looks like: hold Book to Inter's set.
        val report = FontCoverage.report(
            assets = assets,
            family = DocumentFontRegistry.Bundled.resolve(DocumentFontRegistry.FRAUNCES),
            required = FontCoverage.requiredCodePoints(),
        )

        assertFalse(report.isComplete)
        assertTrue(0x03B1 in report.missing) // α
        assertTrue(0x0436 in report.missing) // ж
    }

    @Test
    fun theGuardDetectsAGenuineGap() {
        // Prove the guard can FAIL. A code point no text font carries (U+10FFFD, a private-use plane
        // character) must be reported missing — otherwise a green result would mean nothing.
        val report = FontCoverage.report(
            assets = assets,
            family = DocumentFontRegistry.Bundled.resolve(DocumentFontRegistry.INTER),
            required = setOf('A'.code, 0x10FFFD),
        )

        assertFalse(report.isComplete)
        assertEquals(listOf(0x10FFFD), report.missing)
    }

    @Test
    fun coverageIsMeasuredFromTheFontFileNotTheSystemFallbackChain() {
        // `Paint.hasGlyph` would walk the device's fallback chain and report true for glyphs the bundled
        // font lacks — the exact preview-vs-export drift the bundled-font policy removes. Reading the
        // cmap must therefore report a CJK ideograph as absent from Inter even though a device font has
        // it. (Inter is a Latin/Greek/Cyrillic face; it carries no Han.)
        val report = FontCoverage.report(
            assets = assets,
            family = DocumentFontRegistry.Bundled.resolve(DocumentFontRegistry.INTER),
            required = setOf(0x4E16), // 世
        )

        assertFalse("Inter must not claim Han coverage", report.isComplete)
    }
}
