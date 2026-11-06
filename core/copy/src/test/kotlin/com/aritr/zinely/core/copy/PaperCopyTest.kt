package com.aritr.zinely.core.copy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * A55 (plain paper names) and A56 (the paper pile) words against the frozen `v21-paper-names.html` and
 * `v21-paper-pile.html` (ADR-152). Each string is written out in full AND looked up in its HTML, so a typo in `Copy.kt`
 * and its test cannot agree, and a string that drifts from the frozen spec fails here.
 */
class PaperCopyTest {

    private val paper = Copy.Paper
    private val colour = Copy.PaperColour

    private fun repoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").isFile) return dir
            dir = dir.parentFile
        }
        error("repo root not found from ${System.getProperty("user.dir")}")
    }

    private fun html(name: String) = File(repoRoot(), "docs/design/mockups/$name").readText()
    private val names by lazy { html("v21-paper-names.html") }
    private val pile by lazy { html("v21-paper-pile.html") }

    /** The HTML writes plain spaces; `Copy.kt` keeps a number with its "×" and unit by no-break spaces (A35.10). */
    private fun String.plain() = replace(' ', ' ')

    private fun assertIn(spec: String, text: String) =
        assertTrue(spec.contains(text.plain())) { "not in the frozen HTML: \"${text.plain()}\"" }

    @Test
    fun `given A55's words table, when the plain names and sizes are read, then they match it`() {
        assertEquals("printer paper, most countries", paper.A4_PLAIN)
        assertEquals("printer paper, US and Canada", paper.LETTER_PLAIN)
        assertEquals("long paper", paper.LEGAL_PLAIN)
        assertEquals("small paper", paper.A5_PLAIN)
        assertEquals("210 × 297 mm · pages 74 × 105 mm", paper.A4_SIZES.plain())
        assertEquals("8.5 × 11 in · pages 2.75 × 4.25 in", paper.LETTER_SIZES.plain())
        assertEquals("8.5 × 14 in · pages 3.5 × 4.25 in", paper.LEGAL_SIZES.plain())
        assertEquals("148 × 210 mm · pages 52.5 × 74 mm", paper.A5_SIZES.plain())
        listOf(
            paper.A4_PLAIN, paper.LETTER_PLAIN, paper.LEGAL_PLAIN, paper.A5_PLAIN,
            paper.A4_SIZES, paper.LETTER_SIZES, paper.LEGAL_SIZES, paper.A5_SIZES,
        ).forEach { assertIn(names, "'$it'") }
    }

    @Test
    fun `given the sizes lines, then every number keeps its times sign and unit with no-break spaces`() {
        listOf(paper.A4_SIZES, paper.LETTER_SIZES, paper.LEGAL_SIZES, paper.A5_SIZES).forEach {
            assertTrue(" × " !in it && Regex("\\d (mm|in)").find(it) == null, it)
        }
    }

    @Test
    fun `given US Legal and A5, then the only-on line and A55_4's spoken rows match the HTML`() {
        assertEquals("Use US Legal paper only. Make sure that your printer can use it.", paper.onlyOn(paper.LEGAL))
        assertEquals("Use A5 paper only. Make sure that your printer can use it.", paper.onlyOn(paper.A5))
        assertIn(names, paper.onlyOn(paper.LEGAL))
        assertIn(names, paper.onlyOn(paper.A5))
        assertEquals(
            "US Legal, long paper, 8.5 by 14 inches, pages 3.5 by 4.25 inches. Use US Legal paper only. " +
                "Make sure that your printer can use it.",
            paper.LEGAL_SPOKEN,
        )
        assertIn(names, paper.LEGAL_SPOKEN)
        // The HTML builds the other three from its R list (`speak`): stamp, plain, say, then the line.
        assertEquals("A4, printer paper, most countries, 210 by 297 millimetres, pages 74 by 105 millimetres.", paper.A4_SPOKEN)
        assertEquals("US Letter, printer paper, US and Canada, 8.5 by 11 inches, pages 2.75 by 4.25 inches.", paper.LETTER_SPOKEN)
        assertEquals(
            "A5, small paper, 148 by 210 millimetres, pages 52.5 by 74 millimetres. Use A5 paper only. " +
                "Make sure that your printer can use it.",
            paper.A5_SPOKEN,
        )
        assertIn(names, "say:'210 by 297 millimetres, pages 74 by 105 millimetres'")
        assertIn(names, "say:'8.5 by 11 inches, pages 2.75 by 4.25 inches'")
        assertIn(names, "say:'148 by 210 millimetres, pages 52.5 by 74 millimetres'")
    }

    @Test
    fun `given the Start sheet and Print details, then the sub-line, foot and A55_6 sentence match the HTML`() {
        assertEquals(
            "Choose the paper that is in your printer. You cannot change the paper of a zine after you start it.",
            Copy.Shelf.CHOOSE_PAPER_SUB,
        )
        assertEquals("Zinely makes eight pages from one folded sheet.", Copy.Shelf.EIGHT_PAGES_FROM_SHEET)
        assertEquals(
            "You made this zine for US Legal paper. Use US Legal paper only. Put US Legal paper in your printer before you print.",
            paper.printsOnlyOn(paper.LEGAL),
        )
        assertIn(names, Copy.Shelf.CHOOSE_PAPER_SUB)
        assertIn(names, Copy.Shelf.EIGHT_PAGES_FROM_SHEET)
        assertIn(names, paper.printsOnlyOn(paper.LEGAL))
    }

    @Test
    fun `given A56, then the chooser's words match the HTML`() {
        assertEquals(
            "Choose the paper that you will put in your printer. Zinely shows the colour on the screen only. " +
                "The PDF has only the ink.",
            colour.SUB,
        )
        assertEquals(
            "Home printers have no white ink, so this list has no dark or bright paper. " +
                "The colours on the screen are approximately correct.",
            colour.FOOT,
        )
        assertEquals("Done", colour.DONE)
        assertEquals("Chosen", colour.CHOSEN)
        assertEquals("Pastel orange", colour.PASTEL_ORANGE)
        assertEquals("Pastel purple", colour.PASTEL_PURPLE)
        assertEquals("Light brown", colour.LIGHT_BROWN)
        assertEquals("A sample page on pastel orange paper", colour.sampleLabel("pastel orange", isWhite = false))
        assertEquals("A sample page on white paper, plain printer paper", colour.sampleLabel("white", isWhite = true))
        assertEquals(
            "Put pastel yellow paper in your printer. The PDF has only the ink, so the colour comes from your paper.",
            colour.loadNote("pastel yellow"),
        )
        listOf(colour.SUB, colour.FOOT, ">Done<", ">Chosen<", "'Pastel orange'", "'Pastel purple'", "'Light brown'", "'A sample page on '",
            "', plain printer paper'", "The PDF has only the ink, so the colour comes from your paper.")
            .forEach { assertIn(pile, it) }
    }
}
