package com.aritr.zinely.core.imposition

import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.Rotation
import com.aritr.zinely.core.model.ZineFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

/** Edge cases called out in the spike and the Codex reviews. */
class ImpositionEdgeCaseTest {
    private val imposer = SingleSheet8Imposer()
    private val validator = LayoutValidator()
    private val eps = 1e-6

    @Test
    fun `inset just below the half-panel limit is accepted and validates clean`() {
        // Letter landscape min panel dim = 198 (pw); limit is 2*inset < 198 => inset < 99.
        val layout = imposer.layout(ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER, safeAreaInsetPt = 98.9)
        assertTrue(validator.validate(layout).isEmpty())
    }

    @Test
    fun `inset exactly at the half-panel limit is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            imposer.layout(ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER, safeAreaInsetPt = 99.0)
        }
    }

    @Test
    fun `for every panel the content-local corners map onto its bounds`() {
        for (paper in PaperSize.entries) {
            val layout = imposer.layout(ZineFormat.SINGLE_SHEET_8, paper)
            for (p in layout.panels) {
                val pl = p.panelLocalBounds
                val corners = listOf(
                    PtPoint(pl.x, pl.y), PtPoint(pl.right, pl.y),
                    PtPoint(pl.x, pl.bottom), PtPoint(pl.right, pl.bottom),
                ).map { p.contentToSheet.map(it) }
                val minX = corners.minOf { it.x }
                val maxX = corners.maxOf { it.x }
                val minY = corners.minOf { it.y }
                val maxY = corners.maxOf { it.y }
                assertTrue(abs(minX - p.bounds.x) <= eps && abs(minY - p.bounds.y) <= eps, "min corner $paper page ${p.bookletPage}")
                assertTrue(abs(maxX - p.bounds.right) <= eps && abs(maxY - p.bounds.bottom) <= eps, "max corner $paper page ${p.bookletPage}")
            }
        }
    }

    @Test
    fun `panels exactly tile the sheet with no overlap`() {
        for (paper in PaperSize.entries) {
            val layout = imposer.layout(ZineFormat.SINGLE_SHEET_8, paper)
            val sheetArea = layout.sheet.width * layout.sheet.height
            assertEquals(sheetArea, layout.panels.sumOf { it.bounds.area }, sheetArea * 1e-9)
            val b = layout.panels.map { it.bounds }
            for (i in b.indices) for (j in i + 1 until b.size) {
                val overlap = b[i].x < b[j].right - eps && b[j].x < b[i].right - eps &&
                    b[i].y < b[j].bottom - eps && b[j].y < b[i].bottom - eps
                assertTrue(!overlap, "panels $i,$j overlap on $paper")
            }
        }
    }

    @Test
    fun `all fold and cut endpoints lie within the sheet`() {
        for (paper in PaperSize.entries) {
            val layout = imposer.layout(ZineFormat.SINGLE_SHEET_8, paper)
            val w = layout.sheet.width
            val h = layout.sheet.height
            val pts = layout.foldLines.flatMap { listOf(it.line.start, it.line.end) } +
                layout.cutLines.flatMap { listOf(it.line.start, it.line.end) }
            for (pt in pts) {
                assertTrue(pt.x in -eps..(w + eps) && pt.y in -eps..(h + eps), "endpoint $pt off sheet $paper")
            }
        }
    }

    @Test
    fun `the convention is exhaustively realized in the layout`() {
        val layout = imposer.layout(ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER)
        val spec = SingleSheet8.TOP_ROW_ROTATED
        assertEquals(8, layout.panels.size)
        assertEquals((0..7).toSet(), layout.panels.map { it.cell.row * 4 + it.cell.col }.toSet())
        for (p in layout.panels) {
            assertEquals(spec.cellOf.getValue(p.bookletPage), p.cell, "cell page ${p.bookletPage}")
            assertEquals(spec.rotationOf.getValue(p.bookletPage), p.rotation, "rotation page ${p.bookletPage}")
            assertEquals(spec.roleOf.getValue(p.bookletPage), p.role, "role page ${p.bookletPage}")
        }
        assertEquals(1, layout.panels.count { it.role == PageRole.FRONT_COVER })
        assertEquals(1, layout.panels.count { it.role == PageRole.BACK_COVER })
        assertEquals(Rotation.NONE, layout.panels.first { it.bookletPage == 1 }.rotation)
    }

    @Test
    fun `engine output validates clean across a range of insets and papers`() {
        for (paper in PaperSize.entries) {
            for (inset in listOf(0.0, 6.0, 17.0, 50.0, 90.0)) {
                val layout = imposer.layout(ZineFormat.SINGLE_SHEET_8, paper, inset)
                assertEquals(emptyList<ValidationIssue>(), validator.validate(layout), "inset=$inset paper=$paper")
            }
        }
    }
}
