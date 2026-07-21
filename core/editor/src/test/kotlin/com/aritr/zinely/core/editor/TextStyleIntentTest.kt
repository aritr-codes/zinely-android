package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtPoint
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

/**
 * Immediate-commit text styling (FR-3, ADR-055): [Intent.StyleText] patches individual [TextStyle]
 * fields via one undoable [EditTextCommand], preserving every untouched field (incl. `fontFamily`),
 * the element's text/geometry/id/zIndex, and doing nothing for a blank/absent element. Pure.
 */
class TextStyleIntentTest {

    private fun txt(id: String, text: String = "hi", style: TextStyle = TextStyle()) =
        TextElement(id = id, transform = Transform(1.0, 2.0, 10.0, 20.0), zIndex = 3, text = text, style = style)

    private fun model(vararg els: TextElement) = EditorModel(
        document = ZineDocument(
            format = ZineFormat.SINGLE_SHEET_8,
            paperSize = PaperSize.LETTER,
            pages = listOf(Page(index = 0, role = PageRole.INTERIOR, elements = els.toList())),
        ),
        selection = els.map { it.id }.toSet(),
    )

    private fun el(m: EditorModel, id: String) = m.document.pages[0].elements.first { it.id == id } as TextElement

    @Test
    fun `StyleText updates each individual style field`() {
        val start = model(txt("a"))
        assertEquals(20.0, el(styleOf(start, Intent.StyleText("a", sizePt = 20.0)), "a").style.sizePt)
        assertEquals(ColorRgba.WHITE, el(styleOf(start, Intent.StyleText("a", color = ColorRgba.WHITE)), "a").style.color)
        assertEquals(TextAlign.CENTER, el(styleOf(start, Intent.StyleText("a", align = TextAlign.CENTER)), "a").style.align)
        assertTrue(el(styleOf(start, Intent.StyleText("a", bold = true)), "a").style.bold)
        assertTrue(el(styleOf(start, Intent.StyleText("a", italic = true)), "a").style.italic)
    }

    @Test
    fun `StyleText preserves every untouched style field`() {
        val styled = TextStyle(sizePt = 18.0, color = ColorRgba.WHITE, align = TextAlign.END, bold = true, italic = true)
        val start = model(txt("a", style = styled))
        // Patch only bold=false; every other field must survive.
        val after = el(styleOf(start, Intent.StyleText("a", bold = false)), "a").style
        assertEquals(styled.copy(bold = false), after)
    }

    @Test
    fun `StyleText never resets fontFamily`() {
        val start = model(txt("a", style = TextStyle(fontFamily = "serif")))
        val after = el(styleOf(start, Intent.StyleText("a", sizePt = 30.0, bold = true)), "a").style
        assertEquals("serif", after.fontFamily)
    }

    @Test
    fun `StyleText preserves text, geometry, id and zIndex`() {
        val start = model(txt("a", text = "keep me"))
        val out = el(styleOf(start, Intent.StyleText("a", bold = true)), "a")
        assertEquals("keep me", out.text)
        assertEquals(Transform(1.0, 2.0, 10.0, 20.0), out.transform)
        assertEquals("a", out.id)
        assertEquals(3, out.zIndex)
    }

    @Test
    fun `a StyleText change commits exactly one undoable command`() {
        val start = model(txt("a"))
        val r = EditorReducer.reduce(start, Intent.StyleText("a", bold = true))
        assertEquals(1, r.model.history.undo.size)
        assertTrue(r.model.history.undo.single() is EditTextCommand)
        assertTrue(r.effects.any { it is Effect.Autosave })
        // One undo restores the original document exactly.
        val undone = EditorReducer.reduce(r.model, Intent.Undo).model
        assertEquals(start.document, undone.document)
    }

    @Test
    fun `undo then redo re-applies the style`() {
        val start = model(txt("a"))
        val styled = EditorReducer.reduce(start, Intent.StyleText("a", sizePt = 24.0)).model
        val undone = EditorReducer.reduce(styled, Intent.Undo).model
        assertEquals(start.document, undone.document)
        val redone = EditorReducer.reduce(undone, Intent.Redo).model
        assertEquals(24.0, el(redone, "a").style.sizePt)
    }

    @Test
    fun `a no-op style change (equal to before) pushes no command`() {
        val start = model(txt("a", style = TextStyle(bold = true)))
        val r = EditorReducer.reduce(start, Intent.StyleText("a", bold = true))
        assertEquals(start.document, r.model.document)
        assertTrue(r.model.history.undo.isEmpty())
        assertTrue(r.effects.none { it is Effect.Autosave })
    }

    @Test
    fun `StyleText on a missing or non-text id is a no-op`() {
        val img = ImageElement(id = "i", transform = Transform(0.0, 0.0, 10.0, 10.0), assetId = "sha")
        val start = EditorModel(
            document = ZineDocument(
                format = ZineFormat.SINGLE_SHEET_8,
                paperSize = PaperSize.LETTER,
                pages = listOf(Page(index = 0, role = PageRole.INTERIOR, elements = listOf(img))),
            ),
        )
        assertEquals(start, EditorReducer.reduce(start, Intent.StyleText("nope", bold = true)).model)
        assertEquals(start, EditorReducer.reduce(start, Intent.StyleText("i", bold = true)).model)
    }

    @Test
    fun `StyleText on a blank text box is a no-op (defends fresh-blank-place coalescing, ADR-055)`() {
        val start = model(txt("a", text = "   "))
        val r = EditorReducer.reduce(start, Intent.StyleText("a", bold = true, sizePt = 40.0))
        assertEquals(start.document, r.model.document)
        assertTrue(r.model.history.undo.isEmpty())
        assertTrue(r.effects.none { it is Effect.Autosave })
    }

    @Test
    fun `place then style a blank box then dismiss still coalesces to no undo cruft`() {
        // The regression the blank-guard protects: place empty box, try to style it, dismiss without typing.
        val start = model()
        val placed = EditorReducer.reduce(start, Intent.PlaceText(Transform(0.0, 0.0, 10.0, 10.0), "")).model
        val id = placed.selection.single()
        val styled = EditorReducer.reduce(placed, Intent.StyleText(id, bold = true)).model // must be a no-op
        val begun = EditorReducer.reduce(styled, Intent.BeginEditText(id))
        val token = (begun.model.interaction as Interaction.EditingText).token
        val dismissed = EditorReducer.reduce(begun.model, Intent.CancelText(id, token)).model
        assertEquals(start.document, dismissed.document)
        assertTrue(dismissed.history.undo.isEmpty(), "placement coalesced away — no EditTextCommand cruft")
    }

    @Test
    fun `a committed style survives a subsequent text-edit session`() {
        // Style, then run a text-edit session that changes only text (the draft carries current style).
        val start = model(txt("a", text = "old"))
        val styled = EditorReducer.reduce(start, Intent.StyleText("a", bold = true, sizePt = 22.0)).model
        val begun = EditorReducer.reduce(styled, Intent.BeginEditText("a"))
        val token = (begun.model.interaction as Interaction.EditingText).token
        val draft = el(styled, "a").copy(text = "new") // UI supplies the current (styled) element with new text
        val committed = EditorReducer.reduce(begun.model, Intent.CommitText("a", draft, token)).model
        val out = el(committed, "a")
        assertEquals("new", out.text)
        assertTrue(out.style.bold)
        assertEquals(22.0, out.style.sizePt)
        assertNotEquals(TextStyle(), out.style)
    }

    /** Reduce [intent] against [start] and return the next model (styling always immediate-commits). */
    private fun styleOf(start: EditorModel, intent: Intent.StyleText) = EditorReducer.reduce(start, intent).model
}
