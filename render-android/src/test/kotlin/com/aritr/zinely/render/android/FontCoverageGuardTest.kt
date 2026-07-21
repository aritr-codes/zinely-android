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
        // If a script is added to BUNDLED_SCRIPTS, this fails until the guard probes it too — so the
        // promise cannot silently outrun what is verified.
        assertEquals(SupportedScripts.BUNDLED_SCRIPTS, FontCoverage.guardedScripts)
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
