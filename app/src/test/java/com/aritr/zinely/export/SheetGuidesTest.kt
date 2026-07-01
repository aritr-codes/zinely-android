package com.aritr.zinely.export

import com.aritr.zinely.core.imposition.SingleSheet8Imposer
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.core.render.FillRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-JVM check that the guide overlay maps the imposition's fold/cut lines to thin sheet-space rects. */
class SheetGuidesTest {

    private val layout = SingleSheet8Imposer().layout(ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER)

    @Test
    fun overlayHasOneRectPerFoldAndCutLine() {
        val overlay = SheetGuides.overlay(layout)
        assertEquals(layout.foldLines.size + layout.cutLines.size, overlay.size)
        assertTrue("all guides are fills", overlay.all { it is FillRect })
    }

    @Test
    fun verticalFoldCollapsesToAThinRect() {
        // The V-center fold runs top→bottom, so its rect is thickness-wide and ≈ sheet-tall.
        val overlay = SheetGuides.overlay(layout)
        val tallest = overlay.filterIsInstance<FillRect>().maxBy { it.rect.height }
        assertTrue("a vertical fold is thin", tallest.rect.width < 2.0)
        assertTrue("and spans the sheet height", tallest.rect.height >= layout.sheet.height)
    }
}
