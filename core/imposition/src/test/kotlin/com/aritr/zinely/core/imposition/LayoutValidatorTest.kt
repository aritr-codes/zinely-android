package com.aritr.zinely.core.imposition

import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.GridCell
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtLine
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.ZineFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LayoutValidatorTest {
    private val validator = LayoutValidator()
    private val valid = SingleSheet8Imposer().layout(ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER)

    private fun codes(l: ImpositionLayout) = validator.validate(l).map { it.code }.toSet()

    /** Replace the panel for [page] with [f] applied. */
    private fun mutate(page: Int, f: (PanelPlacement) -> PanelPlacement): ImpositionLayout =
        valid.copy(panels = valid.panels.map { if (it.bookletPage == page) f(it) else it })

    @Test
    fun `a freshly imposed layout is valid`() {
        assertEquals(emptyList<ValidationIssue>(), validator.validate(valid))
    }

    @Test
    fun `flags duplicate booklet pages`() {
        val broken = mutate(2) { it.copy(bookletPage = 1) }
        assertTrue(ValidationCodes.PAGE_NOT_BIJECTIVE in codes(broken))
    }

    @Test
    fun `flags duplicate cells`() {
        val broken = mutate(2) { it.copy(cell = GridCell(1, 3)) } // same cell as page 1
        assertTrue(ValidationCodes.CELL_NOT_BIJECTIVE in codes(broken))
    }

    @Test
    fun `flags a cell outside the grid`() {
        val broken = mutate(2) { it.copy(cell = GridCell(5, 9)) }
        assertTrue(ValidationCodes.CELL_OUT_OF_GRID in codes(broken))
    }

    @Test
    fun `flags a panel outside the sheet`() {
        val broken = mutate(2) { it.copy(bounds = it.bounds.copy(x = 100000.0)) }
        assertTrue(ValidationCodes.PANEL_OUT_OF_BOUNDS in codes(broken))
    }

    @Test
    fun `flags overlapping panels`() {
        // Move page 2 onto page 1's cell region without touching its declared cell.
        val target = valid.panels.first { it.bookletPage == 1 }.bounds
        val broken = mutate(2) { it.copy(bounds = target) }
        assertTrue(ValidationCodes.PANEL_OVERLAP in codes(broken))
    }

    @Test
    fun `flags a contentToSheet transform inconsistent with bounds and rotation`() {
        val broken = mutate(1) { it.copy(contentToSheet = AffineTransform2D.identity()) }
        assertTrue(ValidationCodes.TRANSFORM_BOUNDS_MISMATCH in codes(broken))
    }

    @Test
    fun `flags a safe area not contained in the panel`() {
        val broken = mutate(1) { it.copy(safeLocalBounds = PtRect(-5.0, -5.0, 1000.0, 1000.0)) }
        assertTrue(ValidationCodes.SAFE_NOT_IN_PANEL in codes(broken))
    }

    @Test
    fun `flags a missing front cover`() {
        val broken = mutate(1) { it.copy(role = PageRole.INTERIOR) }
        assertTrue(ValidationCodes.COVER_COUNT in codes(broken))
    }

    @Test
    fun `flags a cut referencing a non-existent fold`() {
        val broken = valid.copy(cutLines = valid.cutLines.map { it.copy(onFoldId = "nope") })
        assertTrue(ValidationCodes.CUT_FOLD_MISSING in codes(broken))
    }

    @Test
    fun `flags a cut that does not lie on its referenced fold`() {
        val off = valid.cutLines.first().copy(line = PtLine(PtPoint(10.0, 10.0), PtPoint(20.0, 20.0)))
        val broken = valid.copy(cutLines = listOf(off))
        assertTrue(ValidationCodes.CUT_NOT_ON_FOLD in codes(broken))
    }

    @Test
    fun `flags a wrong panel count`() {
        val broken = valid.copy(panels = valid.panels.drop(1))
        assertTrue(ValidationCodes.PANEL_COUNT in codes(broken))
    }

    @Test
    fun `flags panels that leave a gap (do not tile the sheet)`() {
        val broken = mutate(2) { it.copy(bounds = it.bounds.copy(width = it.bounds.width / 2)) }
        assertTrue(ValidationCodes.PANELS_DONT_TILE in codes(broken))
    }

    @Test
    fun `flags duplicate fold ids`() {
        val dup = valid.foldLines.first()
        val broken = valid.copy(foldLines = valid.foldLines + dup)
        assertTrue(ValidationCodes.FOLD_ID_DUPLICATE in codes(broken))
    }

    @Test
    fun `flags a page set that is not exactly 1 through N`() {
        val broken = mutate(1) { it.copy(bookletPage = 0) } // unique but missing page 1
        assertTrue(ValidationCodes.PAGE_NOT_BIJECTIVE in codes(broken))
    }

    @Test
    fun `flags a transform with a correct bounding box but wrong orientation`() {
        // page 5 is a HALF panel at the origin cell; an identity transform has the right bbox
        // but the wrong orientation. An independent (non-circular) check must catch it.
        val broken = mutate(5) { it.copy(contentToSheet = AffineTransform2D.identity()) }
        assertTrue(ValidationCodes.TRANSFORM_BOUNDS_MISMATCH in codes(broken))
    }

    @Test
    fun `flags a safe area whose margins are smaller than the inset`() {
        val broken = mutate(1) { it.copy(safeLocalBounds = it.panelLocalBounds) } // zero margins < inset
        assertTrue(ValidationCodes.SAFE_NOT_IN_PANEL in codes(broken))
    }

    @Test
    fun `flags a wrong panelIndex`() {
        val broken = mutate(1) { it.copy(panelIndex = 99) }
        assertTrue(ValidationCodes.PANEL_INDEX_INVALID in codes(broken))
    }

    @Test
    fun `flags panelLocalBounds that are not the origin-anchored panel size`() {
        val broken = mutate(1) { it.copy(panelLocalBounds = PtRect(0.0, 0.0, 1.0, 1.0)) }
        assertTrue(ValidationCodes.PANEL_LOCAL_MISMATCH in codes(broken))
    }

    @Test
    fun `flags a clip area outside the panel`() {
        val broken = mutate(1) { it.copy(clipLocalBounds = PtRect(-5.0, -5.0, 1000.0, 1000.0)) }
        assertTrue(ValidationCodes.CLIP_NOT_IN_PANEL in codes(broken))
    }

    @Test
    fun `flags a fold line outside the sheet`() {
        val broken = valid.copy(
            foldLines = valid.foldLines.map {
                if (it.id == "H-center") it.copy(line = PtLine(PtPoint(-10.0, 306.0), PtPoint(802.0, 306.0))) else it
            },
        )
        assertTrue(ValidationCodes.FOLD_OUT_OF_BOUNDS in codes(broken))
    }

    @Test
    fun `flags a degenerate fold line`() {
        val broken = valid.copy(
            foldLines = valid.foldLines.map {
                if (it.id == "V-center") it.copy(line = PtLine(PtPoint(396.0, 100.0), PtPoint(396.0, 100.0))) else it
            },
        )
        assertTrue(ValidationCodes.FOLD_DEGENERATE in codes(broken))
    }

    @Test
    fun `flags a fold whose axis disagrees with its geometry`() {
        val broken = valid.copy(
            foldLines = valid.foldLines.map {
                if (it.id == "H-center") it.copy(axis = FoldAxis.VERTICAL) else it
            },
        )
        assertTrue(ValidationCodes.FOLD_AXIS_MISMATCH in codes(broken))
    }

    @Test
    fun `flags a degenerate cut line`() {
        // zero-length, but sitting on the H-center fold so CUT_NOT_ON_FOLD does not mask it
        val degenerate = valid.cutLines.first().copy(line = PtLine(PtPoint(300.0, 306.0), PtPoint(300.0, 306.0)))
        val broken = valid.copy(cutLines = listOf(degenerate))
        assertTrue(ValidationCodes.CUT_DEGENERATE in codes(broken))
    }

    @Test
    fun `issues are returned in a deterministic order`() {
        val broken = mutate(2) { it.copy(bookletPage = 1) }
        val a = validator.validate(broken)
        val b = validator.validate(broken)
        assertEquals(a, b)
    }

    @Test
    fun `issues carry a category and severity`() {
        val broken = mutate(2) { it.copy(bookletPage = 1) }
        val issue = validator.validate(broken).first { it.code == ValidationCodes.PAGE_NOT_BIJECTIVE }
        assertEquals(IssueCategory.TOPOLOGY, issue.category)
        assertEquals(Severity.ERROR, issue.severity)
    }
}
