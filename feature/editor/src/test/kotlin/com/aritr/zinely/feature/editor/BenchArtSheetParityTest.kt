package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.render.Segment
import com.aritr.zinely.core.render.SupplyCatalog
import java.io.File
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source-level parity with the owner-approved A16 Art amendment, A32's two frames, and A39's final list (ADR-139). */
class BenchArtSheetParityTest {

    private data class FrozenSupply(
        val family: String,
        val id: String,
        val name: String,
        val path: String,
    )

    private val frozen = File("../../docs/design/mockups/v21-bench.html").readText()
    private val a16 = frozen.substringAfter("A16 ITERATION 3").substringBefore("</html>")

    /** The A16/A32 sheet as shipped: 34 pieces in the order the sheet draws them. */
    private fun shippedSupplies(): List<FrozenSupply> {
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
        // A32 keeps its two frames in a list of their own and pushes it onto ART_FIRST_WAVE when the page
        // runs, so the sheet's order is the first wave and then the frames.
        val expansion = listOf("const ART_FIRST_WAVE=[", "const ART_D5_FRAMES=[").flatMap { list -> rowsOf(list) }
        return Copy.Supplies.BY_FAMILY.keys.flatMap { family ->
            (original + expansion).filter { it.family == family }
        }
    }

    private fun rowsOf(list: String): List<FrozenSupply> =
        Regex("""\['([^']+)','([^']+)','([^']+)','<path d="([^"]*)"/>'\]""")
            .findAll(frozen.substringAfter(list).substringBefore("];"))
            .map { match ->
                FrozenSupply(
                    family = match.groupValues[1],
                    id = match.groupValues[2],
                    name = match.groupValues[3],
                    path = match.groupValues[4],
                )
            }
            .toList()

    /** v21-bench.html A39 (ADR-139): the final 124, four fields per row, in `Copy.Supplies.BY_FAMILY` order. */
    private val a39: List<FrozenSupply> by lazy { rowsOf("const ART_A39=[") }

    /**
     * The families wired to A39 so far, one per change (R-A7 (5)); the rest still match the shipped sheet.
     * ⚠ Temporary while the four family commits land: the last of them removes the shipped half.
     */
    private val wiredToA39 = setOf(Copy.Supplies.TAPE_AND_FIXINGS, Copy.Supplies.STAMPS_AND_MARKS, Copy.Supplies.CUT_PAPER)

    private fun frozenSupplies(): List<FrozenSupply> = Copy.Supplies.BY_FAMILY.keys.flatMap { family ->
        (if (family in wiredToA39) a39 else shippedSupplies()).filter { it.family == family }
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
    fun a39_is_the_frozen_final_list_of_124_in_four_families() {
        assertEquals(124, a39.size)
        assertEquals(a39.size, a39.map { it.id }.toSet().size)
        assertEquals(
            listOf(24, 38, 44, 18),
            Copy.Supplies.BY_FAMILY.keys.map { family -> a39.count { it.family == family } },
        )
        assertEquals(Copy.Supplies.BY_FAMILY.keys.toList(), a39.map { it.family }.distinct())
    }

    @Test
    fun ids_names_families_order_and_geometry_match_the_freeze() {
        val expected = frozenSupplies()
        val actual = Copy.Supplies.BY_FAMILY.flatMap { (family, supplies) ->
            supplies.map { (id, name) -> Triple(family, id, name) }
        }
        assertEquals(expected.map { Triple(it.family, it.id, it.name) }, actual)
        expected.forEach { supply ->
            assertEquals(supply.id, normalisePath(supply.path), normalisePath(cataloguePath(supply.id)))
        }
    }

    @Test
    fun the_two_frames_are_a32s_frozen_and_pushed_onto_the_sheet() {
        assertTrue(frozen.contains("A32 · 2026-10-12 · FROZEN by the owner's ruling of that date"))
        val a32 = frozen.substringAfter("A32 · 2026-10-12 · FROZEN")
        assertTrue(a32.contains("ART_FIRST_WAVE.push(...ART_D5_FRAMES);"))
        assertTrue(a32.contains("Object.assign(ART_TAGS,ART_D5_TAGS);"))
        val frames = rowsOf("const ART_D5_FRAMES=[")
        assertEquals(
            listOf(
                Triple(Copy.Supplies.CUT_PAPER, "paper.border", "Snipped border"),
                Triple(Copy.Supplies.CUT_PAPER, "paper.round", "Round window"),
            ),
            frames.map { Triple(it.family, it.id, it.name) },
        )
        frames.forEach { frame ->
            assertTrue(
                "${frame.id} tags",
                a32.contains("'${frame.id}':'${Copy.Supplies.TAGS.getValue(frame.id)}'"),
            )
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
