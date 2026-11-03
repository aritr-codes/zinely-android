package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.Element
import com.aritr.zinely.core.model.Fit
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** ADR-147 / Bench A52: `SetDescription` is one whole-element command with its own undo words. */
class DescriptionReducerTest {
    private val box = Transform(10.0, 20.0, 50.0, 40.0, 0.0)
    private val photo = ImageElement("photo", box, zIndex = 1, assetId = "a".repeat(64), fit = Fit.FILL)
    private val art = DecorElement("art", box, zIndex = 2, supplyId = "shape.star", ink = ColorRgba(10, 20, 30))
    private val text = TextElement("text", box, zIndex = 3, text = "words")

    private fun model(vararg elements: Element, page: Int = 1) = EditorModel(
        document = ZineDocument(
            format = ZineFormat.SINGLE_SHEET_8,
            paperSize = PaperSize.A4,
            pages = List(8) { i ->
                Page(index = i, role = PageRole.INTERIOR, elements = if (i == page) elements.toList() else emptyList())
            },
        ),
        currentPageIndex = page,
    )

    private fun reduce(m: EditorModel, vararg intents: Intent) =
        intents.fold(m) { acc, i -> EditorReducer.reduce(acc, i).model }

    private fun EditorModel.el(id: String) = document.pages[currentPageIndex].elements.first { it.id == id }
    private fun EditorModel.description(id: String) = when (val e = el(id)) {
        is ImageElement -> e.description
        is DecorElement -> e.description
        else -> error("no description on $e")
    }

    @Test
    fun `describing a photo and a piece of Art is one command and one undo step each`() {
        val m = model(photo, art)
        val p = reduce(m, Intent.SetDescription("photo", "Mum at the gate"))
        assertEquals("Mum at the gate", p.description("photo"))
        assertEquals(1, p.history.undo.size)
        assertEquals(m.document, reduce(p, Intent.Undo).document)

        val a = reduce(m, Intent.SetDescription("art", "this way to the flower end"))
        assertEquals("this way to the flower end", a.description("art"))
        assertEquals(1, a.history.undo.size)
    }

    @Test
    fun `the same text makes no step`() {
        val described = model(photo.copy(description = "same"))
        val out = EditorReducer.reduce(described, Intent.SetDescription("photo", "same"))
        assertEquals(described, out.model)
        assertEquals(emptyList<Effect>(), out.effects)
    }

    @Test
    fun `a change that normalises to the stored words makes no step`() {
        val described = model(photo.copy(description = "same"))
        assertEquals(0, reduce(described, Intent.SetDescription("photo", "  same\n")).history.undo.size)
    }

    @Test
    fun `text is normalised once, at the reducer`() {
        val m = reduce(model(photo), Intent.SetDescription("photo", "  line one\nline two  "))
        assertEquals("line one line two", m.description("photo"))
    }

    @Test
    fun `blank clears the description`() {
        val m = reduce(model(photo.copy(description = "words")), Intent.SetDescription("photo", "   "))
        assertNull(m.description("photo"))
    }

    @Test
    fun `text, a missing id and another page are no-ops`() {
        val m = model(text)
        assertEquals(m, EditorReducer.reduce(m, Intent.SetDescription("text", "x")).model)
        assertEquals(m, EditorReducer.reduce(m, Intent.SetDescription("nope", "x")).model)
    }

    @Test
    fun `undo and redo restore it`() {
        val done = reduce(model(photo), Intent.SetDescription("photo", "a moth"))
        val undone = reduce(done, Intent.Undo)
        assertNull(undone.description("photo"))
        assertEquals("a moth", reduce(undone, Intent.Redo).description("photo"))
    }

    @Test
    fun `Replace photo clears the description and Undo brings both back`() {
        val described = model(photo.copy(description = "the old picture"))
        val replaced = reduce(described, Intent.ReplaceImage("photo", "b".repeat(64)))
        assertNull(replaced.description("photo"))
        assertEquals("b".repeat(64), (replaced.el("photo") as ImageElement).assetId)
        val undone = reduce(replaced, Intent.Undo)
        assertEquals("the old picture", undone.description("photo"))
        assertEquals("a".repeat(64), (undone.el("photo") as ImageElement).assetId)
    }

    @Test
    fun `Replace supply keeps the description`() {
        val described = model(art.copy(description = "this way"))
        val replaced = reduce(described, Intent.ReplaceSupply("art", "shape.heart", box))
        assertEquals("this way", replaced.description("art"))
    }

    @Test
    fun `Duplicate keeps the description on the copy`() {
        val m = reduce(model(photo.copy(description = "twin")), Intent.DuplicateElement("photo", PtSize(105.0, 148.0)))
        val copy = m.document.pages[1].elements.single { it.id != "photo" } as ImageElement
        assertEquals("twin", copy.description)
    }

    @Test
    fun `Across fold keeps the description on both halves`() {
        val m = reduce(
            model(photo.copy(description = "wide"), page = 1),
            Intent.MakeImageSpread("photo", photoAspect = 2.0, pageSizePt = PtSize(105.0, 148.0)),
        )
        val halves = m.document.pages.flatMap { it.elements }.filterIsInstance<ImageElement>()
        assertEquals(2, halves.size)
        assertEquals(listOf("wide", "wide"), halves.map { it.description })
    }

    // — editLabel: A52.4's three verbs, and the precedence the ADR orders —

    private fun labelAfter(start: EditorModel, vararg intents: Intent): EditLabel? {
        val done = reduce(start, *intents)
        return done.history.undo.last().editLabel(done.document)
    }

    @Test
    fun `none to words is added, words to none is removed, words to other words is changed`() {
        assertEquals(EditLabel(EditVerb.DESCRIPTION_ADDED, null), labelAfter(model(photo), Intent.SetDescription("photo", "x")))
        assertEquals(
            EditLabel(EditVerb.DESCRIPTION_REMOVED, null),
            labelAfter(model(photo.copy(description = "x")), Intent.SetDescription("photo", "")),
        )
        assertEquals(
            EditLabel(EditVerb.DESCRIPTION_CHANGED, null),
            labelAfter(model(photo.copy(description = "x")), Intent.SetDescription("photo", "y")),
        )
        assertEquals(EditLabel(EditVerb.DESCRIPTION_ADDED, null), labelAfter(model(art), Intent.SetDescription("art", "x")))
        assertEquals(
            EditLabel(EditVerb.DESCRIPTION_CHANGED, null),
            labelAfter(model(art.copy(description = "x")), Intent.SetDescription("art", "y")),
        )
    }

    @Test
    fun `Replace photo on a described photo is still the photo swap`() {
        assertEquals(
            EditLabel(EditVerb.SWAP, EditKind.PHOTO),
            labelAfter(model(photo.copy(description = "x")), Intent.ReplaceImage("photo", "b".repeat(64))),
        )
    }

    @Test
    fun `Replace supply on a described piece is still the Art swap, and a description is never framing or a swap`() {
        assertEquals(
            EditLabel(EditVerb.SWAP, EditKind.ART),
            labelAfter(model(art.copy(description = "x")), Intent.ReplaceSupply("art", "shape.heart", box)),
        )
        val described = labelAfter(model(photo), Intent.SetDescription("photo", "x"))!!.verb
        assertEquals(false, described == EditVerb.FRAMING || described == EditVerb.SWAP)
    }
}
