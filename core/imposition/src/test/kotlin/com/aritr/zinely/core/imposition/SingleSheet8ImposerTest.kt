package com.aritr.zinely.core.imposition

import com.aritr.zinely.core.model.GridCell
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Rotation
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.core.model.approxEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class SingleSheet8ImposerTest {
    private val imposer = SingleSheet8Imposer()
    private val eps = 1e-6

    private fun layout(p: PaperSize) = imposer.layout(ZineFormat.SINGLE_SHEET_8, p)
    private fun ImpositionLayout.page(n: Int) = panels.first { it.bookletPage == n }

    @Test
    fun `supports only the single-sheet 8 format with the canonical convention`() {
        assertEquals(setOf(ZineFormat.SINGLE_SHEET_8), imposer.supportedFormats)
        assertEquals("TOP_ROW_ROTATED", imposer.convention.name)
    }

    @Test
    fun `produces exactly 8 panels, one per booklet page`() {
        val l = layout(PaperSize.LETTER)
        assertEquals(8, l.panels.size)
        assertEquals((1..8).toSet(), l.panels.map { it.bookletPage }.toSet())
    }

    @Test
    fun `panel bounds tile the sheet per the cell grid (Letter)`() {
        val l = layout(PaperSize.LETTER) // landscape 792 x 612 => pw=198, ph=306
        assertTrue(l.page(5).bounds.approxEquals(PtRect(0.0, 0.0, 198.0, 306.0)))   // (0,0)
        assertTrue(l.page(1).bounds.approxEquals(PtRect(594.0, 306.0, 198.0, 306.0))) // (1,3)
        assertTrue(l.page(8).bounds.approxEquals(PtRect(396.0, 306.0, 198.0, 306.0))) // (1,2)
    }

    @Test
    fun `panel bounds derive from paper for A4 too`() {
        val l = layout(PaperSize.A4) // landscape 841.89 x 595.276 => pw=210.4725, ph=297.638
        val pw = 841.890 / 4.0
        val ph = 595.276 / 2.0
        assertTrue(l.page(5).bounds.approxEquals(PtRect(0.0, 0.0, pw, ph), 1e-3))
        assertTrue(l.page(1).bounds.approxEquals(PtRect(3 * pw, ph, pw, ph), 1e-3))
    }

    @Test
    fun `panelIndex is the row-major physical id`() {
        val l = layout(PaperSize.LETTER)
        assertEquals(0, l.page(5).panelIndex)  // cell (0,0) -> 0
        assertEquals(7, l.page(1).panelIndex)  // cell (1,3) -> 7
        assertEquals(6, l.page(8).panelIndex)  // cell (1,2) -> 6
    }

    @Test
    fun `upright panel contentToSheet just translates content into its cell`() {
        val l = layout(PaperSize.LETTER)
        val p1 = l.page(1) // upright, bounds (594,306,198,306)
        assertEquals(Rotation.NONE, p1.rotation)
        // local top-left -> cell top-left; local bottom-right -> cell bottom-right
        assertPoint(594.0, 306.0, p1.contentToSheet.map(PtPoint(0.0, 0.0)))
        assertPoint(792.0, 612.0, p1.contentToSheet.map(PtPoint(198.0, 306.0)))
    }

    @Test
    fun `rotated panel contentToSheet half-turns content about the panel center`() {
        val l = layout(PaperSize.LETTER)
        val p5 = l.page(5) // 180-degree, bounds (0,0,198,306)
        assertEquals(Rotation.HALF, p5.rotation)
        // content top-left lands at the panel's bottom-right; bottom-right lands at top-left
        assertPoint(198.0, 306.0, p5.contentToSheet.map(PtPoint(0.0, 0.0)))
        assertPoint(0.0, 0.0, p5.contentToSheet.map(PtPoint(198.0, 306.0)))
        // center maps to the cell center
        assertPoint(99.0, 153.0, p5.contentToSheet.map(PtPoint(99.0, 153.0)))
    }

    @Test
    fun `rotated panel in a non-origin cell maps content top-left to the cell bottom-right`() {
        val l = layout(PaperSize.LETTER)
        val p2 = l.page(2) // 180, cell (0,3) -> bounds (594,0,198,306)
        assertPoint(792.0, 306.0, p2.contentToSheet.map(PtPoint(0.0, 0.0)))
    }

    @Test
    fun `emits four fold lines on the cell boundaries and one cut on the center fold`() {
        val l = layout(PaperSize.LETTER) // W=792, H=612
        assertEquals(setOf("H-center", "V-quarter-1", "V-center", "V-quarter-3"), l.foldLines.map { it.id }.toSet())
        val hCenter = l.foldLines.first { it.id == "H-center" }
        assertEquals(FoldAxis.HORIZONTAL, hCenter.axis)
        assertPoint(0.0, 306.0, hCenter.line.start)
        assertPoint(792.0, 306.0, hCenter.line.end)

        assertEquals(1, l.cutLines.size)
        val cut = l.cutLines.first()
        assertEquals("H-center", cut.onFoldId)
        assertPoint(198.0, 306.0, cut.line.start) // W/4, H/2
        assertPoint(594.0, 306.0, cut.line.end)   // 3W/4, H/2
    }

    @Test
    fun `safe and clip local bounds honor the inset`() {
        val l = imposer.layout(ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER, safeAreaInsetPt = 17.0)
        val p = l.page(1)
        assertEquals(17.0, l.safeAreaInsetPt, eps)
        assertTrue(p.panelLocalBounds.approxEquals(PtRect(0.0, 0.0, 198.0, 306.0)))
        assertTrue(p.clipLocalBounds.approxEquals(PtRect(0.0, 0.0, 198.0, 306.0)))
        assertTrue(p.safeLocalBounds.approxEquals(PtRect(17.0, 17.0, 164.0, 272.0)))
    }

    @Test
    fun `rejects an inset too large for the panel`() {
        assertThrows(IllegalArgumentException::class.java) {
            imposer.layout(ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER, safeAreaInsetPt = 200.0)
        }
    }

    @Test
    fun `rejects a negative inset`() {
        assertThrows(IllegalArgumentException::class.java) {
            imposer.layout(ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER, safeAreaInsetPt = -1.0)
        }
    }

    @Test
    fun `is deterministic`() {
        assertEquals(layout(PaperSize.LETTER), layout(PaperSize.LETTER))
        assertEquals(layout(PaperSize.A4), layout(PaperSize.A4))
    }

    @Test
    fun `a named paper imposes exactly as its portrait sheet does`() {
        for (paper in PaperSize.entries) {
            assertEquals(layout(paper), imposer.layout(ZineFormat.SINGLE_SHEET_8, paper.portrait), "$paper")
        }
    }

    // ADR-133 chose US Legal and A5 as the next sizes. They are not in [PaperSize] yet (adding them is a
    // schema change), so the engine is proved on their sheets here, by size.

    @Test
    fun `US Legal imposes 252 x 306 pt pages and validates clean`() {
        val l = imposer.layout(ZineFormat.SINGLE_SHEET_8, LEGAL) // landscape 1008 x 612
        assertTrue(l.page(1).panelLocalBounds.approxEquals(PtRect(0.0, 0.0, 252.0, 306.0)))
        assertTrue(l.page(1).safeLocalBounds.approxEquals(PtRect(17.0, 17.0, 218.0, 272.0)))
        assertEquals(emptyList<ValidationIssue>(), LayoutValidator().validate(l))
    }

    @Test
    fun `A5 imposes the A4 page scaled by one over root two and validates clean`() {
        val a5 = imposer.layout(ZineFormat.SINGLE_SHEET_8, A5).page(1).panelLocalBounds
        assertTrue(a5.approxEquals(PtRect(0.0, 0.0, 595.276 / 4, 419.528 / 2))) // 148.819 x 209.764
        val a4 = layout(PaperSize.A4).page(1).panelLocalBounds
        // ISO 216: A5 is A4 halved, so it keeps A4's shape. Within 1 pt, not exactly: the standard rounds
        // A5's short side down to 148 mm from half of A4's 297 mm, which moves the page height by 0.7 pt.
        assertEquals(a4.width / sqrt(2.0), a5.width, 1.0)
        assertEquals(a4.height / sqrt(2.0), a5.height, 1.0)
        assertEquals(emptyList<ValidationIssue>(), LayoutValidator().validate(imposer.layout(ZineFormat.SINGLE_SHEET_8, A5)))
    }

    @Test
    fun `rejects a sheet with a zero side`() {
        for (bad in listOf(PtSize(0.0, 792.0), PtSize(612.0, 0.0))) {
            assertThrows(IllegalArgumentException::class.java, { imposer.layout(ZineFormat.SINGLE_SHEET_8, bad) }, "$bad")
        }
    }

    private fun assertPoint(ex: Double, ey: Double, p: PtPoint) {
        assertEquals(ex, p.x, eps, "x")
        assertEquals(ey, p.y, eps, "y")
    }

    private companion object {
        /** US Legal, 8.5 × 14 in. */
        val LEGAL = PtSize(width = 612.0, height = 1008.0)

        /** A5, 148 × 210 mm. */
        val A5 = PtSize(width = 419.528, height = 595.276)
    }
}
