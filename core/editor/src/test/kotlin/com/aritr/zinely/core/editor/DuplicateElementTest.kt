package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.Fit
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextAlign
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.TextStyle
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DuplicateElementTest {
    private val pageSize = PtSize(100.0, 100.0)

    private fun model(vararg elements: com.aritr.zinely.core.model.Element): EditorModel = EditorModel(
        document = ZineDocument(
            format = ZineFormat.SINGLE_SHEET_8,
            paperSize = PaperSize.A4,
            pages = listOf(Page(0, PageRole.INTERIOR, elements = elements.toList())),
        ),
    )

    @Test
    fun `duplicate text preserves authored content, offsets it, selects it, and is one undo step`() {
        val style = TextStyle(sizePt = 18.0, align = TextAlign.CENTER, bold = true)
        val source = TextElement("el-4", Transform(10.0, 14.0, 30.0, 20.0, 7.0), 3, "Hello", style)

        val result = EditorReducer.reduce(model(source), Intent.DuplicateElement(source.id, pageSize))
        val copy = result.model.document.pages.single().elements.last() as TextElement

        assertEquals("el-5", copy.id)
        assertEquals(Transform(22.0, 26.0, 30.0, 20.0, 7.0), copy.transform)
        assertEquals(source.text, copy.text)
        assertEquals(source.style, copy.style)
        assertEquals(4, copy.zIndex)
        assertEquals(setOf(copy.id), result.model.selection)
        assertTrue(result.model.history.undo.single() is PlaceCommand)
        assertTrue(result.effects.single() is Effect.Autosave)

        val undone = EditorReducer.reduce(result.model, Intent.Undo).model
        assertEquals(listOf(source), undone.document.pages.single().elements)
        assertTrue(undone.selection.isEmpty())
        val redone = EditorReducer.reduce(undone, Intent.Redo).model
        assertEquals(copy, redone.document.pages.single().elements.last())
    }

    @Test
    fun `duplicate photo reverses at the page edge and keeps the shared asset and framing`() {
        val source = ImageElement(
            id = "photo",
            transform = Transform(80.0, 75.0, 20.0, 25.0, -4.0),
            zIndex = 8,
            assetId = "a".repeat(64),
            crop = Crop(0.1, 0.2, 0.8, 0.9),
            fit = Fit.FILL,
            copier = true,
        )

        val result = EditorReducer.reduce(model(source), Intent.DuplicateElement(source.id, pageSize)).model
        val copy = result.document.pages.single().elements.last() as ImageElement

        assertEquals(Transform(68.0, 63.0, 20.0, 25.0, -4.0), copy.transform)
        assertEquals(source.assetId, copy.assetId)
        assertEquals(source.crop, copy.crop)
        assertEquals(source.fit, copy.fit)
        assertEquals(source.copier, copy.copier)
        assertNotEquals(source.id, copy.id)
    }

    @Test
    fun `duplicate art keeps its supply ink and mirror and leaves a page-sized axis aligned`() {
        val source = DecorElement(
            id = "decor",
            transform = Transform(0.0, 35.0, 100.0, 20.0),
            zIndex = 1,
            supplyId = "mark.arrow",
            ink = ColorRgba(142, 149, 70),
            mirrored = true,
        )

        val result = EditorReducer.reduce(model(source), Intent.DuplicateElement(source.id, pageSize)).model
        val copy = result.document.pages.single().elements.last() as DecorElement

        assertEquals(0.0, copy.transform.xPt)
        assertEquals(47.0, copy.transform.yPt)
        assertEquals(source.supplyId, copy.supplyId)
        assertEquals(source.ink, copy.ink)
        assertTrue(copy.mirrored)
    }

    @Test
    fun `duplicate rejects a missing element without history or autosave`() {
        val source = TextElement("text", Transform(10.0, 10.0, 20.0, 20.0), text = "Hi")
        val start = model(source)

        val missing = EditorReducer.reduce(start, Intent.DuplicateElement("missing", pageSize))

        assertEquals(start, missing.model)
        assertTrue(missing.effects.isEmpty())
    }

    @Test
    fun `blank text is unfinished content and is not duplicated`() {
        val source = TextElement("blank", Transform(10.0, 10.0, 20.0, 20.0), text = "   ")
        val start = model(source)

        val result = EditorReducer.reduce(start, Intent.DuplicateElement(source.id, pageSize))

        assertEquals(start, result.model)
        assertTrue(result.effects.isEmpty())
    }
}
