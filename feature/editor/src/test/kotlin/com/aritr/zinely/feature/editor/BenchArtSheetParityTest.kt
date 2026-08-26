package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.render.Segment
import com.aritr.zinely.core.render.SupplyCatalog
import java.io.File
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source-level parity with the owner-approved A16 Art amendment. */
class BenchArtSheetParityTest {

    private data class FrozenSupply(
        val family: String,
        val id: String,
        val name: String,
        val path: String,
    )

    private val frozen = File("../../docs/design/mockups/v21-bench.html").readText()
    private val a16 = frozen.substringAfter("A16 ITERATION 3").substringBefore("</html>")

    private fun frozenSupplies(): List<FrozenSupply> {
        val original = Regex("""\['([a-z]+\.[a-z]+)','([^']*)','<path d="([^"]*)"/>'\]""")
            .findAll(frozen.substringAfter("const SUP=[").substringBefore("];"))
            .mapIndexed { index, match ->
                FrozenSupply(
                    family = Copy.Supplies.BY_FAMILY.keys.elementAt(index / 4),
                    id = match.groupValues[1],
                    name = match.groupValues[2],
                    path = match.groupValues[3],
                )
            }
            .toList()
        val expansion = Regex("""\['([^']+)','([^']+)','([^']+)','<path d="([^"]*)"/>'\]""")
            .findAll(frozen.substringAfter("const ART_FIRST_WAVE=[").substringBefore("];"))
            .map { match ->
                FrozenSupply(
                    family = match.groupValues[1],
                    id = match.groupValues[2],
                    name = match.groupValues[3],
                    path = match.groupValues[4],
                )
            }
            .toList()
        return Copy.Supplies.BY_FAMILY.keys.flatMap { family ->
            (original + expansion).filter { it.family == family }
        }
    }

    private fun cataloguePath(supplyId: String): String {
        val outline = SupplyCatalog.outlineOf(supplyId) ?: error("$supplyId has no authored outline")
        fun n(value: Double): String = String.format(Locale.ROOT, "%.2f", value * VIEWBOX)
            .trimEnd('0').trimEnd('.').ifEmpty { "0" }
        return outline.subpaths.joinToString(" ") { subpath ->
            val head = "M${n(subpath.start.x)} ${n(subpath.start.y)}"
            val body = subpath.segments.joinToString(" ") { segment ->
                when (segment) {
                    is Segment.LineTo -> "L${n(segment.to.x)} ${n(segment.to.y)}"
                    is Segment.CubicTo -> "C${n(segment.c1.x)} ${n(segment.c1.y)} " +
                        "${n(segment.c2.x)} ${n(segment.c2.y)} ${n(segment.to.x)} ${n(segment.to.y)}"
                }
            }
            "$head $body Z"
        }
    }

    /** Canonicalises the frozen SVG's H/V shorthand to the catalogue's absolute M/L/C notation. */
    private fun normalisePath(path: String): List<String> {
        val tokens = Regex("""[MLHVCZ]|-?\d+(?:\.\d+)?""").findAll(path).map { it.value }.toList()
        val output = mutableListOf<String>()
        var index = 0
        var command = ""
        var x = 0.0
        var y = 0.0
        fun number(): Double = tokens[index++].toDouble()
        fun n(value: Double): String = String.format(Locale.ROOT, "%.2f", value)
            .trimEnd('0').trimEnd('.').ifEmpty { "0" }
        while (index < tokens.size) {
            if (tokens[index].length == 1 && tokens[index][0].isLetter()) command = tokens[index++]
            when (command) {
                "M", "L" -> {
                    x = number()
                    y = number()
                    output += "$command${n(x)} ${n(y)}"
                    if (command == "M") command = "L"
                }
                "H" -> {
                    x = number()
                    output += "L${n(x)} ${n(y)}"
                }
                "V" -> {
                    y = number()
                    output += "L${n(x)} ${n(y)}"
                }
                "C" -> {
                    val c1x = number()
                    val c1y = number()
                    val c2x = number()
                    val c2y = number()
                    x = number()
                    y = number()
                    output += "C${n(c1x)} ${n(c1y)} ${n(c2x)} ${n(c2y)} ${n(x)} ${n(y)}"
                }
                "Z" -> {
                    output += "Z"
                    command = ""
                }
                else -> error("Unsupported frozen SVG command near ${tokens.getOrNull(index)}")
            }
        }
        return output
    }

    @Test
    fun a16_is_the_frozen_source_and_not_an_unapproved_iteration() {
        assertTrue(a16.contains("DESIGN FROZEN 2026-08-26"))
        assertTrue(a16.contains("production source of truth for the Compose implementation"))
    }

    @Test
    fun all_thirty_two_ids_names_families_order_and_geometry_match_the_freeze() {
        val expected = frozenSupplies()
        val actual = Copy.Supplies.BY_FAMILY.flatMap { (family, supplies) ->
            supplies.map { (id, name) -> Triple(family, id, name) }
        }
        assertEquals(32, expected.size)
        assertEquals(expected.map { Triple(it.family, it.id, it.name) }, actual)
        expected.forEach { supply ->
            assertEquals(supply.id, normalisePath(supply.path), normalisePath(cataloguePath(supply.id)))
        }
    }

    @Test
    fun layout_measurements_match_the_frozen_material_cards() {
        assertTrue(a16.contains("grid-template-columns:repeat(3,minmax(0,1fr))"))
        assertTrue(a16.contains("grid-auto-columns:104px"))
        assertTrue(a16.contains("height:134px"))
        assertTrue(a16.contains("width:48px;height:48px"))
        assertEquals(3, BenchArtGridColumns)
        assertEquals(104f, BenchArtRailCardWidth.value, 0f)
        assertEquals(134f, BenchArtCardHeight.value, 0f)
        assertEquals(46f, BenchArtGlyphSize.value, 0f)
        assertEquals(12f, BenchArtGridGap.value, 0f)
    }

    @Test
    fun search_families_favourites_and_empty_state_keep_the_frozen_contract() {
        assertTrue(a16.contains("search.placeholder='${Copy.BenchArt.SEARCH_HINT}'"))
        assertTrue(frozen.contains("const ART_FAVOURITES=new Set()"))
        assertTrue(a16.contains("aria-live=\"polite\""))
        assertTrue(frozen.contains("favouriteEmpty.textContent='${Copy.BenchArt.FAVOURITES_EMPTY}'"))
        assertTrue(frozen.contains("<b>${Copy.BenchArt.NO_RESULTS}</b>"))
        assertTrue(frozen.contains("<span>${Copy.BenchArt.NO_RESULTS_HINT}</span>"))
        assertTrue(frozen.contains(">${Copy.BenchArt.SHOW_ALL}</button>"))
        assertEquals(4, Copy.Supplies.BY_FAMILY.size)
    }

    @Test
    fun family_material_tilts_match_a16() {
        assertEquals(-0.55f, BenchArtTilt[0], 0f)
        assertEquals(0.45f, BenchArtTilt[1], 0f)
        assertEquals(-0.35f, BenchArtTilt[2], 0f)
        assertEquals(0.35f, BenchArtTilt[3], 0f)
        Copy.Supplies.BY_FAMILY.values.forEachIndexed { index, supplies ->
            supplies.keys.forEach { id -> assertEquals(index, benchArtFamilyIndex(id)) }
        }
    }

    private companion object {
        const val VIEWBOX = 24.0
    }
}
