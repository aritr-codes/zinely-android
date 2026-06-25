package com.aritr.zinely.render.android

import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * [BundledFontResolver] contract (ADR-028 §4.2): the four `(bold, italic)` combinations select four
 * distinct bundled styles, and each style is loaded once and reused (so layout is stable across calls).
 * Glyph coverage is proven separately in [FontCoverageTest]; native rendering of these typefaces in the
 * proof environment is proven in [FontRenderingProbeTest].
 */
@RunWith(RobolectricTestRunner::class)
class BundledFontResolverTest {

    private val resolver = BundledFontResolver(RuntimeEnvironment.getApplication().assets)

    @Test
    fun selectsADistinctStylePerBoldItalicCombination() {
        val regular = resolver.resolve("sans-serif", bold = false, italic = false)
        val bold = resolver.resolve("sans-serif", bold = true, italic = false)
        val italic = resolver.resolve("sans-serif", bold = false, italic = true)
        val boldItalic = resolver.resolve("sans-serif", bold = true, italic = true)

        val all = listOf(regular, bold, italic, boldItalic)
        for (i in all.indices) {
            for (j in i + 1 until all.size) {
                assertNotSame("styles $i and $j must be different Typefaces", all[i], all[j])
            }
        }
    }

    @Test
    fun reusesTheSameTypefaceInstancePerStyle() {
        // Stable identity → identical metrics on every layout build (no per-call reload drift).
        assertSame(
            resolver.resolve("sans-serif", bold = true, italic = false),
            resolver.resolve("sans-serif", bold = true, italic = false),
        )
    }

    @Test
    fun mapsEveryFamilyNameToTheBundledDefault() {
        // MVP bundles one family, so a known and an unknown family resolve to the same style instance.
        assertSame(
            resolver.resolve("sans-serif", bold = false, italic = false),
            resolver.resolve("Totally Unknown Family", bold = false, italic = false),
        )
    }
}
