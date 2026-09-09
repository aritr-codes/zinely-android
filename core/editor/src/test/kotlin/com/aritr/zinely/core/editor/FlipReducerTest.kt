package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.Element
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FlipReducerTest {
    private val transform = Transform(10.0, 20.0, 30.0, 40.0, 15.0)

    private fun photo(id: String = "photo") = ImageElement(
        id = id,
        transform = transform,
        zIndex = 4,
        assetId = "a".repeat(64),
    )

    private fun art(id: String = "art") = DecorElement(
        id = id,
        transform = transform,
        zIndex = 5,
        supplyId = "shape.star",
        ink = ColorRgba(10, 20, 30),
    )

    private fun text(id: String = "text") = TextElement(id, transform, text = "words")

    private fun model(vararg elements: Element): EditorModel = EditorModel(
        document = ZineDocument(
            format = ZineFormat.SINGLE_SHEET_8,
            paperSize = PaperSize.A4,
            pages = listOf(
                Page(0, PageRole.INTERIOR, elements = elements.toList()),
                Page(1, PageRole.INTERIOR, elements = listOf(photo("off-page"))),
            ),
        ),
        selection = setOf(elements.first().id),
    )

    @Test
    fun `photo axes toggle independently with one command and autosave each`() {
        val horizontal = EditorReducer.reduce(model(photo()), Intent.ToggleFlip("photo", FlipAxis.HORIZONTAL))
        val h = horizontal.model.document.pages[0].elements.single() as ImageElement
        assertTrue(h.flippedHorizontally)
        assertFalse(h.flippedVertically)
        assertEquals(setOf("photo"), horizontal.model.selection)
        assertTrue(horizontal.model.history.undo.single() is EditImageCommand)
        assertEquals(1, horizontal.effects.count { it is Effect.Autosave })

        val vertical = EditorReducer.reduce(horizontal.model, Intent.ToggleFlip("photo", FlipAxis.VERTICAL))
        val both = vertical.model.document.pages[0].elements.single() as ImageElement
        assertTrue(both.flippedHorizontally)
        assertTrue(both.flippedVertically)
        assertEquals(2, vertical.model.history.undo.size)
    }

    @Test
    fun `art horizontal uses historical mirror and vertical uses the new field`() {
        val horizontal = EditorReducer.reduce(model(art()), Intent.ToggleFlip("art", FlipAxis.HORIZONTAL))
        val h = horizontal.model.document.pages[0].elements.single() as DecorElement
        assertTrue(h.mirrored)
        assertFalse(h.flippedVertically)
        assertTrue(horizontal.model.history.undo.single() is EditDecorCommand)

        val vertical = EditorReducer.reduce(horizontal.model, Intent.ToggleFlip("art", FlipAxis.VERTICAL))
        val both = vertical.model.document.pages[0].elements.single() as DecorElement
        assertTrue(both.mirrored)
        assertTrue(both.flippedVertically)
    }

    @Test
    fun `tapping an active axis removes it and undo redo restore exact state`() {
        val start = model(photo().copy(flippedHorizontally = true))
        val removed = EditorReducer.reduce(start, Intent.ToggleFlip("photo", FlipAxis.HORIZONTAL)).model
        assertFalse((removed.document.pages[0].elements.single() as ImageElement).flippedHorizontally)

        val undone = EditorReducer.reduce(removed, Intent.Undo).model
        assertTrue((undone.document.pages[0].elements.single() as ImageElement).flippedHorizontally)
        assertEquals(start.document, undone.document)

        val redone = EditorReducer.reduce(undone, Intent.Redo).model
        assertFalse((redone.document.pages[0].elements.single() as ImageElement).flippedHorizontally)
    }

    @Test
    fun `text missing and off-page targets are no-ops`() {
        val start = model(text())
        for (id in listOf("text", "missing", "off-page")) {
            val result = EditorReducer.reduce(start, Intent.ToggleFlip(id, FlipAxis.VERTICAL))
            assertEquals(start, result.model, id)
            assertTrue(result.effects.isEmpty(), id)
        }
    }

    @Test
    fun `reframe commit changes only framing and preserves both photo flips`() {
        val source = photo().copy(flippedHorizontally = true, flippedVertically = true)
        val begun = EditorReducer.reduce(model(source), Intent.BeginReframe("photo")).model
        val session = begun.interaction as Interaction.Reframing
        val draft = source.copy(
            crop = Crop(0.1, 0.2, 0.9, 0.8),
            flippedHorizontally = false,
            flippedVertically = false,
        )
        val committed = EditorReducer.reduce(
            begun,
            Intent.CommitReframe("photo", draft, session.token),
        ).model.document.pages[0].elements.single() as ImageElement

        assertEquals(Crop(0.1, 0.2, 0.9, 0.8), committed.crop)
        assertTrue(committed.flippedHorizontally)
        assertTrue(committed.flippedVertically)
    }
}
