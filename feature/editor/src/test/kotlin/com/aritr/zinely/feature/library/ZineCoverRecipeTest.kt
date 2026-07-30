package com.aritr.zinely.feature.library

import androidx.compose.ui.graphics.Color
import com.aritr.zinely.ui.theme.zinelyContentInks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The cover recipe: [ZineCoverSurface] and [ZineCoverStamp], and the palette each surface resolves to.
 *
 * Pure JVM — no Robolectric. `Color` is a value class over a `ULong`, so the palette assertions need no
 * Android runtime, which keeps the frozen-hex checks in the fast suite where a wrong ink fails in
 * milliseconds.
 *
 * **What this file does not test.** B1 shipped no function that assigns a [ZineCoverRecipe] to a zine —
 * [D-017](docs/design/V2-SPEC-DEFECTS.md)'s ruling ("assign once at creation, persist, never derive from
 * the title") is a property of a create-and-store path, and B1 has neither: [ZineCover] takes a recipe,
 * it does not compute one. An earlier draft shipped an assigner here (`newZineCoverRecipe(random)`)
 * guarded by a ~40-line reflection scan that tried to prove no function in this package maps a `String`
 * to a cover. Independent review found the guard could not hold the ruling regardless of how it was
 * written: it filtered on parameter *type*, so a title-derived **seed** at a call site
 * (`newZineCoverRecipe(Random(title.hashCode()))`) satisfies every version of it while still letting the
 * title reach the cover. A signature check cannot decide an information-flow property. So the assigner —
 * and the guard — are deleted; both land in **B5**, next to the persisted field the ruling requires, where
 * there is an actual call site whose only input can be checked directly instead of enumerated against.
 */
class ZineCoverRecipeTest {

    // -----------------------------------------------------------------------------------------
    // The palette — the frozen hexes, resolved only from the content namespace
    // -----------------------------------------------------------------------------------------

    @Test
    fun `each surface resolves to its frozen fill, title colour and band`() {
        val inks = zinelyContentInks()
        // `v2-library.html:79-84`, verbatim.
        val expected = mapOf(
            ZineCoverSurface.MatchaInk to Triple(0xFF7C8A3F, 0xFFF7F2E7, 0xFF4E5A26),
            ZineCoverSurface.TealInk to Triple(0xFF47857B, 0xFFF7F2E7, 0xFF2E574E),
            ZineCoverSurface.StrawberryInk to Triple(0xFFE27F89, 0xFF4A211F, 0xFFC05863),
            ZineCoverSurface.OchreInk to Triple(0xFFD19A3C, 0xFF3A2A0E, 0xFFA9741F),
            // The paper stock's bands are cover-ink *fills*, not those inks' darker band cuts.
            ZineCoverSurface.PaperMatchaBand to Triple(0xFFF1EBDA, 0xFF2A251E, 0xFF7C8A3F),
            ZineCoverSurface.PaperStrawberryBand to Triple(0xFFF1EBDA, 0xFF2A251E, 0xFFE27F89),
        )
        for ((surface, hexes) in expected) {
            val palette = surface.palette(inks)
            val (fill, onFill, band) = hexes
            assertEquals("$surface fill", Color(fill), palette.fill)
            assertEquals("$surface title colour", Color(onFill), palette.onFill)
            assertEquals("$surface band", Color(band), palette.band)
        }
        assertEquals("every frozen surface must be covered", ZineCoverSurface.entries.size, expected.size)
    }

    @Test
    fun `the band is always darker than the stock it prints on`() {
        // The band multiplies onto the paper. A band lighter than its fill would still *draw* — multiply
        // would simply do almost nothing — and the cover would silently lose its one printed mark.
        val inks = zinelyContentInks()
        for (surface in ZineCoverSurface.entries) {
            val palette = surface.palette(inks)
            assertTrue(
                "$surface: band ${palette.band} must be darker than fill ${palette.fill}",
                palette.band.luminance() < palette.fill.luminance(),
            )
        }
    }

    /** Plain relative brightness — enough to order two colours, and no WCAG claim is being made here. */
    private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

    @Test
    fun `each stamp maps to its own frozen mark`() {
        val icons = ZineCoverStamp.entries.map { it.icon() }
        assertEquals("no two stamps may draw the same mark", icons.size, icons.toSet().size)
        assertEquals("Sun stamp", "StampSun", ZineCoverStamp.Sun.icon().name)
        assertEquals("Letter stamp", "StampLetter", ZineCoverStamp.Letter.icon().name)
        assertEquals("Waves stamp", "StampWaves", ZineCoverStamp.Waves.icon().name)
        assertEquals("Sprig stamp", "StampSprig", ZineCoverStamp.Sprig.icon().name)
        assertEquals("Star stamp", "StampStar", ZineCoverStamp.Star.icon().name)
        assertEquals("Face stamp", "StampFace", ZineCoverStamp.Face.icon().name)
    }

    @Test
    fun `every stamp paints itself, so no cover call site invents a stroke weight`() {
        // The seven artwork marks carry `stroke-width:1.6` in their own markup, which is why the cover
        // can build them without passing a paint. If that ever stops being true, `toImageVector` throws
        // at run time on a real shelf — so it is asserted here instead.
        for (stamp in ZineCoverStamp.entries) {
            assertTrue("${stamp.icon().name} must carry its own frozen paint", stamp.icon().frozenPaint != null)
        }
    }
}
