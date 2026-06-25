package com.aritr.zinely.render.android

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * The §4.2 glyph-coverage guard (ADR-028, risk R2/R12): every bundled Inter style must be
 * **self-covering** for the [MvpCharset] so no device-variable system fallback is ever reached for
 * in-scope text. Asserted against each TTF's `cmap` directly ([CmapReader]) — not `Paint.hasGlyph`,
 * which would falsely pass via the system fallback chain. A font swap/subset that drops an MVP glyph
 * fails the build here, before it can silently shift wrapping in a golden.
 *
 * Pure byte parsing (no rasterisation), so it is robust regardless of Robolectric's native-graphics
 * gaps; it only needs the merged library assets, which `isIncludeAndroidResources` puts on the
 * unit-test classpath.
 */
@RunWith(RobolectricTestRunner::class)
class FontCoverageTest {

    private val bundledStyles = listOf(
        "fonts/Inter-Regular.ttf",
        "fonts/Inter-Bold.ttf",
        "fonts/Inter-Italic.ttf",
        "fonts/Inter-BoldItalic.ttf",
    )

    @Test
    fun everyBundledStyleSelfCoversTheMvpCharset() {
        val assets = RuntimeEnvironment.getApplication().assets
        for (path in bundledStyles) {
            val bytes = assets.open(path).use { it.readBytes() }
            val covered = CmapReader.coveredCodePoints(bytes)
            val missing = MvpCharset.codePoints.filter { it !in covered }
                .sorted()
                .map { "U+%04X".format(it) }
            assertTrue("$path is missing MVP glyphs: $missing", missing.isEmpty())
        }
    }
}
