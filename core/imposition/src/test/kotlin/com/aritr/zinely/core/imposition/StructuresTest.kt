package com.aritr.zinely.core.imposition

import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.GridCell
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtLine
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.Rotation
import com.aritr.zinely.core.model.ZineFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** The layout structures must wire together and round-trip their fields. */
class StructuresTest {
    @Test
    fun `imposition layout holds panels, folds and cuts with a cut bound to a fold`() {
        val panel = PanelPlacement(
            panelIndex = 0,
            bookletPage = 1,
            role = PageRole.FRONT_COVER,
            cell = GridCell(1, 3),
            bounds = PtRect(0.0, 0.0, 10.0, 10.0),
            rotation = Rotation.NONE,
            panelLocalBounds = PtRect(0.0, 0.0, 10.0, 10.0),
            safeLocalBounds = PtRect(1.0, 1.0, 8.0, 8.0),
            clipLocalBounds = PtRect(0.0, 0.0, 10.0, 10.0),
            contentToSheet = AffineTransform2D.identity(),
        )
        val fold = FoldLine("H-center", PtLine(PtPoint(0.0, 5.0), PtPoint(40.0, 5.0)), FoldAxis.HORIZONTAL)
        val cut = CutLine(PtLine(PtPoint(10.0, 5.0), PtPoint(30.0, 5.0)), onFoldId = "H-center")

        val layout = ImpositionLayout(
            format = ZineFormat.SINGLE_SHEET_8,
            paper = PaperSize.LETTER,
            sheet = PaperSize.LETTER.landscape(),
            conventionName = SingleSheet8.TOP_ROW_ROTATED.name,
            panels = listOf(panel),
            foldLines = listOf(fold),
            cutLines = listOf(cut),
            safeAreaInsetPt = 17.0,
        )

        assertEquals(1, layout.panels.size)
        assertEquals(1, layout.bookletPageOf(panel))
        assertEquals("H-center", layout.cutLines.first().onFoldId)
        assertEquals(FoldAxis.HORIZONTAL, layout.foldLines.first().axis)
        assertEquals(FoldType.UNSPECIFIED, layout.foldLines.first().type)
    }
}

private fun ImpositionLayout.bookletPageOf(p: PanelPlacement): Int =
    panels.first { it.panelIndex == p.panelIndex }.bookletPage
