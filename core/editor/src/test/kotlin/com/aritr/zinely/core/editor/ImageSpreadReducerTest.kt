package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.Fit
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.TextStyle
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ImageSpreadReducerTest {
    private val pageSize = PtSize(105.0, 148.0)

    private fun image(id: String = "photo") = ImageElement(
        id = id,
        transform = Transform(12.0, 18.0, 50.0, 40.0, 7.0),
        zIndex = 4,
        assetId = "a".repeat(64),
        fit = Fit.FILL,
        copier = true,
    )

    private fun text(id: String, z: Int) = TextElement(
        id = id,
        transform = Transform(10.0, 10.0, 30.0, 20.0),
        zIndex = z,
        text = id,
        style = TextStyle(color = ColorRgba.BLACK),
    )

    private fun model(sourcePage: Int, source: ImageElement = image()): EditorModel {
        val pages = List(8) { index ->
            Page(
                index = index,
                role = when (index) {
                    0 -> PageRole.FRONT_COVER
                    7 -> PageRole.BACK_COVER
                    else -> PageRole.INTERIOR
                },
                elements = when (index) {
                    sourcePage -> listOf(text("source-copy", 2), source)
                    imageSpreadPair(sourcePage)?.partnerPageIndex -> listOf(text("partner-copy", 3))
                    else -> emptyList()
                },
            )
        }
        return EditorModel(
            document = ZineDocument(
                format = ZineFormat.SINGLE_SHEET_8,
                paperSize = PaperSize.A4,
                pages = pages,
            ),
            currentPageIndex = sourcePage,
            selection = setOf(source.id),
        )
    }

    @Test
    fun `fixed booklet table contains only the four physical pairs`() {
        val expected = listOf(7, 2, 1, 4, 3, 6, 5, 0)
        assertEquals(expected, (0..7).map { imageSpreadPair(it)?.partnerPageIndex })
        assertEquals(listOf(false, true, false, true, false, true, false, true),
            (0..7).map { imageSpreadPair(it)?.sourceIsLeft })
        assertEquals(null, imageSpreadPair(-1))
        assertEquals(null, imageSpreadPair(8))
    }

    @Test
    fun `spread crops are complementary page-aspect halves`() {
        val (left, right) = imageSpreadCrops(photoAspect = 16.0 / 9.0, pageAspect = 105.0 / 148.0)!!
        assertEquals(0.5, left.right)
        assertEquals(0.5, right.left)
        assertEquals(left.top, right.top)
        assertEquals(left.bottom, right.bottom)
        val photoAspect = 16.0 / 9.0
        assertEquals(105.0 / 148.0, (left.right - left.left) * photoAspect / (left.bottom - left.top), 1e-12)
        assertEquals(105.0 / 148.0, (right.right - right.left) * photoAspect / (right.bottom - right.top), 1e-12)
    }

    @Test
    fun `one action writes shared full-page halves behind existing content and one undo restores both pages`() {
        val before = model(sourcePage = 1)
        val reduction = EditorReducer.reduce(
            before,
            Intent.MakeImageSpread("photo", photoAspect = 16.0 / 9.0, pageSizePt = pageSize),
        )
        val after = reduction.model
        val left = after.document.pages[1].elements.filterIsInstance<ImageElement>().single()
        val right = after.document.pages[2].elements.filterIsInstance<ImageElement>().single()

        assertEquals("photo", left.id)
        assertNotEquals(left.id, right.id)
        assertEquals(left.assetId, right.assetId)
        assertTrue(left.copier && right.copier)
        assertEquals(Transform(0.0, 0.0, 105.0, 148.0), left.transform)
        assertEquals(left.transform, right.transform)
        assertEquals(Fit.FIT, left.fit)
        assertEquals(Fit.FIT, right.fit)
        assertEquals(0.5, left.crop.right)
        assertEquals(0.5, right.crop.left)
        assertTrue(left.zIndex < after.document.pages[1].elements.first { it.id == "source-copy" }.zIndex)
        assertTrue(right.zIndex < after.document.pages[2].elements.first { it.id == "partner-copy" }.zIndex)
        assertEquals(1, after.history.undo.size)
        assertEquals(1, reduction.effects.filterIsInstance<Effect.Autosave>().size)

        val undone = EditorReducer.reduce(after, Intent.Undo).model
        assertEquals(before.document, undone.document)
        val redone = EditorReducer.reduce(undone, Intent.Redo).model
        assertEquals(after.document, redone.document)
    }

    @Test
    fun `outside wrap puts page eight on the left and page one on the right`() {
        val start = model(sourcePage = 0)
        val after = EditorReducer.reduce(
            start,
            Intent.MakeImageSpread("photo", photoAspect = 2.0, pageSizePt = pageSize),
        ).model
        val pageOne = after.document.pages[0].elements.filterIsInstance<ImageElement>().single()
        val pageEight = after.document.pages[7].elements.filterIsInstance<ImageElement>().single()
        assertEquals(0.5, pageEight.crop.right)
        assertEquals(0.5, pageOne.crop.left)
    }

    @Test
    fun `invalid aspect missing image and missing partner are no-ops`() {
        val start = model(sourcePage = 3)
        assertEquals(start, EditorReducer.reduce(start, Intent.MakeImageSpread("missing", 1.5, pageSize)).model)
        assertEquals(start, EditorReducer.reduce(start, Intent.MakeImageSpread("photo", Double.NaN, pageSize)).model)
        val short = start.copy(document = start.document.copy(pages = start.document.pages.take(4)))
        assertEquals(short, EditorReducer.reduce(short, Intent.MakeImageSpread("photo", 1.5, pageSize)).model)
    }
}
