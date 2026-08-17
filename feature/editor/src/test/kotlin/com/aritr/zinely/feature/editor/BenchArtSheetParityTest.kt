package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.copy.Copy
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the Art sheet against the frozen file it was transcribed from — `openArt()`'s `SUP` array
 * (`v21-bench.html:848-854`), as amended by **A5**.
 *
 * This is the same instrument `ZinelyV2IconsTest` uses on the V2 icon set and for the same reason: a
 * transcription that is only *checked* once drifts the first time either side is edited. It re-extracts the
 * sixteen entries from the HTML at run time and compares ids, order, spoken names and geometry against the
 * Kotlin, in both directions, so the two cannot disagree without a red test.
 *
 * ⚠ **It reads the mockup at a relative path, not a resource.** The frozen file is documentation and is not
 * packaged; the path is the one `ZinelyV2IconsTest` already uses, one level deeper.
 */
class BenchArtSheetParityTest {

    private val frozen = File("../../docs/design/mockups/v21-bench.html").readText()

    /**
     * The frozen `SUP` entries as `id → (name, inner SVG markup)`, in file order.
     *
     * `SUP` is a JS array literal of three-element arrays; the third element is either a quoted string of
     * SVG children or the bare identifier `DECOR.star`, which is resolved here from its own declaration
     * (`:641`) rather than hard-coded — the freeze reuses that glyph deliberately, and a test that inlined
     * it would keep passing if the freeze changed the star.
     */
    private fun frozenSupplies(): List<Triple<String, String, String>> {
        val body = frozen.substringAfter("function openArt(){").substringBefore("\$('sheet').innerHTML")
        val star = Regex("""star:'([^']*)'""").find(frozen)!!.groupValues[1]
        return Regex("""\['([a-z]+\.[a-z]+)','([^']*)',(?:'([^']*)'|(DECOR\.star))]""")
            .findAll(body)
            .map { m ->
                val markup = m.groupValues[3].ifEmpty { star }
                Triple(m.groupValues[1], m.groupValues[2], markup)
            }
            .toList()
    }

    /** The `d`/`cx,cy,r`/`x,y,w,h` of each child, in the notation both sides are compared in. */
    private fun frozenGeometry(markup: String): List<String> =
        Regex("""<(path|circle|rect)\s*([^>]*?)/?>""").findAll(markup).map { m ->
            val at = Regex("""([\w-]+)=['"]([^'"]*)['"]""").findAll(m.groupValues[2])
                .associate { it.groupValues[1] to it.groupValues[2] }
            when (m.groupValues[1]) {
                "path" -> "path:${at.getValue("d")}"
                "circle" -> "circle:${at.getValue("cx")},${at.getValue("cy")},${at.getValue("r")}"
                else -> "rect:${at["x"] ?: "0"},${at["y"] ?: "0"}," +
                    "${at.getValue("width")},${at.getValue("height")},${at["rx"] ?: "0"}"
            }
        }.toList()

    /** The Kotlin side, in the same notation. Floats are printed the way the HTML writes them. */
    private fun kotlinGeometry(supplyId: String): List<String> =
        BenchArtGlyphs.getValue(supplyId).map { shape ->
            fun n(v: Float) = if (v == v.toInt().toFloat()) "${v.toInt()}" else "$v"
            when (shape) {
                is com.aritr.zinely.ui.theme.ZinelyV2IconShape.Path -> "path:${shape.data}"
                is com.aritr.zinely.ui.theme.ZinelyV2IconShape.Circle ->
                    "circle:${n(shape.cx)},${n(shape.cy)},${n(shape.r)}"
                is com.aritr.zinely.ui.theme.ZinelyV2IconShape.Rect ->
                    "rect:${n(shape.x)},${n(shape.y)},${n(shape.width)},${n(shape.height)},${n(shape.rx)}"
            }
        }

    @Test
    fun the_frozen_sheet_offers_sixteen_supplies_and_the_extractor_found_them_all() {
        // Non-vacuity first: every assertion below is over this list, and a regex that silently matched
        // nothing would make the whole file pass while asserting about an empty set.
        assertEquals("the extractor did not find the frozen sixteen", 16, frozenSupplies().size)
    }

    @Test
    fun the_ids_and_their_order_are_the_frozen_ones() {
        assertEquals(
            "the sheet's supply order is SUPPLIES-SPEC §4's, frozen — position is the only way a maker " +
                "finds a supply twice on a surface with no search and no sort",
            frozenSupplies().map { it.first },
            Copy.Supplies.BY_FAMILY.values.flatMap { it.keys },
        )
    }

    @Test
    fun every_spoken_name_matches_the_copy_layer() {
        // A5's reconciliation ran in both directions and ended with the copy winning; this is the assertion
        // that keeps it won. Five of the sixteen names depart from §4's prose, each for a documented reason.
        assertEquals(
            frozenSupplies().associate { it.first to it.second },
            frozenSupplies().associate { it.first to Copy.Supplies.NAMES.getValue(it.first) },
        )
    }

    @Test
    fun every_tile_glyph_is_the_frozen_geometry() {
        val frozenIds = frozenSupplies().map { it.first }.toSet()
        // Both directions: a glyph in the Kotlin that the freeze does not draw is as much a defect as a
        // missing one, and only the second of those fails a per-id loop.
        assertEquals("the Kotlin draws a different set of supplies than the freeze", frozenIds, BenchArtGlyphs.keys)
        for ((id, _, markup) in frozenSupplies()) {
            assertEquals("$id's glyph has drifted from the frozen file", frozenGeometry(markup), kotlinGeometry(id))
        }
    }

    // ── the transcribed MEASUREMENTS, read out of the frozen CSS ──────────────────────────────────────
    //
    // Everything above pins the sheet's *content*. These pin its numbers, and they exist because nothing
    // else can: the goldens were recorded from this implementation, so they pin regression rather than
    // correctness, and at `changeThreshold = 0.02f` a dropped letter-spacing or a 1.7→1.8 stroke across
    // sixteen thin outlines moves far less than 2 % of the frame. "Transcribed from the freeze" is only a
    // checkable claim if something compares it to the freeze.

    /** One `--gap-*` / `--br-*` custom property, in px, from the file's own `:root`. */
    private fun token(name: String): Float =
        Regex("""$name\s*:\s*([\d.]+)px""").find(frozen)!!.groupValues[1].toFloat()

    /** The declaration block of one CSS rule, whitespace-normalised. */
    private fun rule(selector: String): String {
        val at = frozen.indexOf("\n$selector{")
        require(at >= 0) { "$selector is no longer in the frozen file" }
        return frozen.substring(frozen.indexOf('{', at) + 1, frozen.indexOf('}', at))
            .replace(Regex("""\s+"""), " ").trim()
    }

    /** A single declaration's value out of a rule — `.tile svg` `stroke-width` → `1.7`. */
    private fun decl(selector: String, property: String): String =
        Regex("""(?:^|;)\s*$property\s*:\s*([^;]+)""").find(rule(selector))!!.groupValues[1].trim()

    @Test
    fun the_grid_and_tile_geometry_is_the_frozen_geometry() {
        assertEquals("`.grid{grid-template-columns}`", "repeat(4,1fr)", decl(".grid", "grid-template-columns"))
        assertEquals("`.grid{gap:var(--gap-sm)}`", "var(--gap-sm)", decl(".grid", "gap"))
        assertEquals(token("--gap-sm"), BenchArtGridGap.value, 0f)
        assertEquals(4, BenchArtGridColumns)

        // `.tile{aspect-ratio:1}` — the square is what makes a 4-column grid a cabinet rather than a list.
        assertEquals("1", decl(".tile", "aspect-ratio"))
        assertEquals("var(--br-sm)", decl(".tile", "border-radius"))
        assertEquals(token("--br-sm"), com.aritr.zinely.ui.theme.ZinelyV21Dimens.radiusSm.value, 0f)
        assertTrue("`.tile{border:1.5px solid var(--ink)}`", decl(".tile", "border").startsWith("1.5px"))
        assertEquals(1.5f, BenchArtTileBorder.value, 0f)
        // `box-shadow:2px 2px 0 var(--ink-line)` is ZinelyV21Press.Flat's resting offset; `:active` moves
        // the tile 2px and sheds the shadow entirely, which is Flat's travel and its pressed offset.
        assertTrue("`.tile{box-shadow:2px 2px 0 …}`", decl(".tile", "box-shadow").startsWith("2px 2px 0"))
        assertEquals(2f, com.aritr.zinely.ui.theme.ZinelyV21Press.Flat.rest.value, 0f)
        assertEquals(2f, com.aritr.zinely.ui.theme.ZinelyV21Press.Flat.travel.value, 0f)
        assertEquals(0f, com.aritr.zinely.ui.theme.ZinelyV21Press.Flat.pressed.value, 0f)
    }

    @Test
    fun the_glyph_is_drawn_at_the_frozen_size_and_weight() {
        assertEquals("60%", decl(".tile svg", "width"))
        assertEquals("60%", decl(".tile svg", "height"))
        assertEquals(0.6f, BenchArtGlyphFraction, 0f)
        assertEquals("1.7", decl(".tile svg", "stroke-width"))
        assertEquals(1.7f, BenchArtGlyphStroke, 0f)
        assertEquals("none", decl(".tile svg", "fill"))
        assertEquals("round", decl(".tile svg", "stroke-linecap"))
        assertEquals("round", decl(".tile svg", "stroke-linejoin"))
    }

    @Test
    fun the_section_heading_is_the_frozen_lbl() {
        // .62rem at the prototype's 16px root. The conversion is stated here rather than trusted to a
        // comment, because the sp value is the thing that ships.
        assertEquals(".62rem", decl(".lbl", "font-size"))
        assertEquals(0.62f * 16f, BenchArtLabelSize.value, 0.001f)
        assertEquals("700", decl(".lbl", "font-weight"))
        assertEquals(".12em", decl(".lbl", "letter-spacing"))
        assertEquals(0.12f, BenchArtLabelTracking.value, 0f)
        assertEquals("uppercase", decl(".lbl", "text-transform"))
        // `margin:var(--gap-md) 0 var(--gap-sm)` — 12 above, 8 below.
        assertEquals("var(--gap-md) 0 var(--gap-sm)", decl(".lbl", "margin"))
        assertEquals(token("--gap-md"), BenchArtLabelSpaceAbove.value, 0f)
        assertEquals(token("--gap-sm"), BenchArtLabelSpaceBelow.value, 0f)
    }

    @Test
    fun the_tint_and_tilt_cycle_is_the_frozen_one_including_which_rule_wins_at_position_twelve() {
        // Position 12 matches BOTH `3n` and `4n`; CSS resolves it by source order, so `4n` — declared
        // second — wins. Asserted as an ordering fact about the file, because the Kotlin encodes it as a
        // `%4`-before-`%3` branch and a reader who reverses those two lines produces a berry tile at
        // position 12 and nowhere else. That is a one-tile difference in sixteen.
        assertTrue(
            "the freeze no longer declares 3n before 4n — the tint cycle's precedence has moved",
            frozen.indexOf(".tile:nth-child(3n)") < frozen.indexOf(".tile:nth-child(4n)"),
        )
        assertEquals("-1.6deg", decl(".tile:nth-child(3n)", "--tt"))
        assertEquals("1.4deg", decl(".tile:nth-child(4n)", "--tt"))
        assertEquals("var(--berry-tint)", decl(".tile:nth-child(3n)", "background"))
        assertEquals("var(--butter-tint)", decl(".tile:nth-child(4n)", "background"))
        assertEquals("var(--jam-text)", decl(".tile:nth-child(3n)", "color"))
        assertEquals("var(--ink-soft)", decl(".tile:nth-child(4n)", "color"))
        // The Kotlin side of the same cycle: index 0 = leaf/0deg, 1 = berry/-1.6, 2 = butter/+1.4, and
        // the branch order is what puts position 12 on the butter arm.
        assertEquals(0f, BenchArtTilt[0], 0f)
        assertEquals(-1.6f, BenchArtTilt[1], 0f)
        assertEquals(1.4f, BenchArtTilt[2], 0f)
        assertEquals("position 1 is the base leaf tile", 0, benchArtTintIndex(1))
        assertEquals("position 2 is the base leaf tile", 0, benchArtTintIndex(2))
        assertEquals("position 3 is 3n — berry", 1, benchArtTintIndex(3))
        assertEquals("position 4 is 4n — butter", 2, benchArtTintIndex(4))
        assertEquals("position 12 matches both, and 4n is declared later so it wins", 2, benchArtTintIndex(12))
        // …and the base, which is what positions 1 and 2 of every row wear.
        assertEquals("var(--leaf-tint)", decl(".tile", "background"))
        assertEquals("var(--leaf-text)", decl(".tile", "color"))
    }

    @Test
    fun the_frozen_sheet_still_has_no_chip_row_no_filter_and_no_search() {
        // A5 removed the chips and ADR-104 removed the search box, both by ruling. This asserts the *freeze*
        // still says so — if someone re-adds either upstream, this implementation is out of date and should
        // fail loudly rather than silently become the thing that diverges.
        val art = frozen.substringAfter("function openArt(){").substringBefore("function showSheet")
        assertFalse("the frozen Art sheet has grown a chip row again", art.contains("class=\"chips\""))
        assertFalse("the frozen Art sheet has grown a search field again", art.contains("class=\"search\""))
        assertTrue("the frozen Art sheet no longer shelves under .lbl headings", art.contains("class=\"lbl\""))
    }
}
