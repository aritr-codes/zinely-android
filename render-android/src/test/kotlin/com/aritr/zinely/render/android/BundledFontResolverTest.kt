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
    fun anUnregisteredFamilyFallsBackToTheRegistryDefault() {
        // Renamed at F3: this assertion used to be titled "maps every family name to the bundled
        // default", which described the pre-registry behaviour of discarding `fontFamily` outright. The
        // assertion itself is unchanged and still holds — but now for the right reason. Both names below
        // are unregistered, so both resolve to the registry's default family.
        assertSame(
            resolver.resolve("sans-serif", bold = false, italic = false),
            resolver.resolve("Totally Unknown Family", bold = false, italic = false),
        )
    }

    @Test
    fun resolvesPerFamilyThroughTheRegistry() {
        // Family routing must be real, not incidental. With only Inter bundled, a same-assets second
        // family would prove nothing, so the probe family points at a DIFFERENT face of the same TTF set:
        // if `fontFamily` were still discarded, both calls below would return the same Typeface.
        val probe = DocumentFontRegistry(
            families = listOf(
                DocumentFontRegistry.Bundled.resolve(DocumentFontRegistry.INTER),
                DocumentFontFamily(
                    name = "ProbeFamily",
                    regularAsset = "fonts/Inter-Bold.ttf",
                    boldAsset = "fonts/Inter-Bold.ttf",
                    italicAsset = "fonts/Inter-BoldItalic.ttf",
                    boldItalicAsset = "fonts/Inter-BoldItalic.ttf",
                ),
            ),
            defaultFamilyName = DocumentFontRegistry.INTER,
        )
        val r = BundledFontResolver(RuntimeEnvironment.getApplication().assets, probe)

        assertNotSame(
            "the two families must not resolve to the same face",
            r.resolve(DocumentFontRegistry.INTER, bold = false, italic = false),
            r.resolve("ProbeFamily", bold = false, italic = false),
        )
        // ...and the probe's regular face IS Inter-Bold, proving the asset came from the family row.
        assertSame(
            r.resolve("ProbeFamily", bold = false, italic = false),
            r.resolve(DocumentFontRegistry.INTER, bold = true, italic = false),
        )
    }
}
