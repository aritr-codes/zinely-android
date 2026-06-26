package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The race-safe text-edit session (ADR-029 §5.6, D5): begin/commit/cancel like a drag — one session is one
 * [EditTextCommand]; a stale [token] is rejected; an empty result never leaks an empty `TextElement`. Pure.
 */
class TextEditSessionTest {

    private fun txt(id: String, text: String = "old") =
        TextElement(id = id, transform = Transform(0.0, 0.0, 10.0, 10.0), zIndex = 0, text = text)

    private fun model(vararg els: TextElement, selection: Set<String> = emptySet()) = EditorModel(
        document = ZineDocument(
            format = ZineFormat.SINGLE_SHEET_8,
            paperSize = PaperSize.LETTER,
            pages = listOf(Page(index = 0, role = PageRole.INTERIOR, elements = els.toList())),
        ),
        selection = selection,
    )

    private fun els(m: EditorModel) = m.document.pages[0].elements

    private fun begin(m: EditorModel, id: String): Pair<EditorModel, Long> {
        val r = EditorReducer.reduce(m, Intent.BeginEditText(id))
        val tx = r.model.interaction as Interaction.EditingText
        return r.model to tx.token
    }

    @Test
    fun `BeginEditText opens a session selecting the element and advancing the token`() {
        val start = model(txt("a"))
        val r = EditorReducer.reduce(start, Intent.BeginEditText("a"))
        val tx = r.model.interaction as Interaction.EditingText
        assertEquals("a", tx.id)
        assertEquals(start.nextToken, tx.token)
        assertEquals(start.nextToken + 1, r.model.nextToken)
        assertEquals(setOf("a"), r.model.selection)
        assertTrue(r.effects.none { it is Effect.Autosave }, "opening a session is not a mutation")
    }

    @Test
    fun `BeginEditText on a missing or non-text id is a no-op`() {
        val start = model(txt("a"))
        assertEquals(Interaction.Idle, EditorReducer.reduce(start, Intent.BeginEditText("nope")).model.interaction)
    }

    @Test
    fun `BeginEditTextAt opens a session on the topmost text hit, but ignores an image hit`() {
        val text = txt("t", text = "hi") // 10×10 at origin
        val start = model(text)
        val hit = EditorReducer.reduce(start, Intent.BeginEditTextAt(PtPoint(5.0, 5.0))).model
        assertEquals("t", (hit.interaction as Interaction.EditingText).id)

        val img = ImageElement(id = "i", transform = Transform(0.0, 0.0, 10.0, 10.0), assetId = "sha")
        val imgModel = EditorModel(
            document = ZineDocument(
                format = ZineFormat.SINGLE_SHEET_8,
                paperSize = PaperSize.LETTER,
                pages = listOf(Page(index = 0, role = PageRole.INTERIOR, elements = listOf(img))),
            ),
        )
        assertEquals(Interaction.Idle, EditorReducer.reduce(imgModel, Intent.BeginEditTextAt(PtPoint(5.0, 5.0))).model.interaction)
        // A miss (empty space) is also a no-op.
        assertEquals(Interaction.Idle, EditorReducer.reduce(start, Intent.BeginEditTextAt(PtPoint(500.0, 500.0))).model.interaction)
    }

    @Test
    fun `a full session commits exactly one EditTextCommand and one undo restores`() {
        val start = model(txt("a", text = "old"), selection = setOf("a"))
        val (begun, token) = begin(start, "a")
        val r = EditorReducer.reduce(begun, Intent.CommitText("a", txt("a", text = "new"), token))
        assertEquals("new", (els(r.model).single() as TextElement).text)
        assertEquals(Interaction.Idle, r.model.interaction)
        assertTrue(r.effects.any { it is Effect.Autosave })
        val undone = EditorReducer.reduce(r.model, Intent.Undo).model
        assertEquals(start.document, undone.document)
    }

    @Test
    fun `a stale token is rejected — the document and session are untouched`() {
        val start = model(txt("a", text = "old"), selection = setOf("a"))
        val (begun, token) = begin(start, "a")
        val r = EditorReducer.reduce(begun, Intent.CommitText("a", txt("a", text = "new"), token + 1))
        assertEquals("old", (els(r.model).single() as TextElement).text)
        assertTrue(r.model.interaction is Interaction.EditingText, "session stays open for the real commit")
        assertTrue(r.effects.none { it is Effect.Autosave })
    }

    @Test
    fun `a no-op commit (draft equals before) closes the session without a command`() {
        val start = model(txt("a", text = "same"), selection = setOf("a"))
        val (begun, token) = begin(start, "a")
        val r = EditorReducer.reduce(begun, Intent.CommitText("a", txt("a", text = "same"), token))
        assertEquals(Interaction.Idle, r.model.interaction)
        assertEquals(begun.document, r.model.document)
        assertTrue(r.effects.none { it is Effect.Autosave }, "no change ⇒ no autosave / no undo entry")
        assertTrue(r.model.history.undo.isEmpty())
    }

    @Test
    fun `committing empty over an existing box deletes it, one undo restores`() {
        val start = model(txt("a", text = "keep"), selection = setOf("a"))
        val (begun, token) = begin(start, "a")
        val r = EditorReducer.reduce(begun, Intent.CommitText("a", txt("a", text = "   "), token))
        assertTrue(els(r.model).none { it.id == "a" }, "an existing box cleared to blank is deleted")
        assertEquals(Interaction.Idle, r.model.interaction)
        assertTrue(r.effects.any { it is Effect.Autosave })
        assertTrue("a" !in r.model.selection)
        val undone = EditorReducer.reduce(r.model, Intent.Undo).model
        assertEquals(start.document, undone.document)
    }

    @Test
    fun `place then cancel an empty box coalesces away — no undo cruft`() {
        // The "add text" flow: place an EMPTY text box (PlaceCommand), open it, dismiss without typing.
        val start = model(selection = emptySet())
        val placed = EditorReducer.reduce(start, Intent.PlaceText(Transform(0.0, 0.0, 10.0, 10.0), "")).model
        val id = placed.selection.single()
        val (begun, token) = begin(placed, id)
        val r = EditorReducer.reduce(begun, Intent.CancelText(id, token))
        assertTrue(els(r.model).none { it.id == id }, "the freshly-placed empty box is removed")
        // Coalesced: the placement was undone+popped, so the undo stack is back to empty (no resurrect-empty).
        assertEquals(start.document, r.model.document)
        assertTrue(r.model.history.undo.isEmpty(), "no PlaceCommand left to undo")
        assertTrue(r.effects.any { it is Effect.Autosave })
    }

    @Test
    fun `committing non-blank keeps before's geometry, ignoring UI-supplied transform`() {
        val start = model(txt("a", text = "old"), selection = setOf("a"))
        val (begun, token) = begin(start, "a")
        // A malformed `after` carrying a moved transform must NOT move the element — only text changes.
        val malformed = TextElement("a", Transform(999.0, 999.0, 1.0, 1.0), 7, "new")
        val r = EditorReducer.reduce(begun, Intent.CommitText("a", malformed, token))
        val out = els(r.model).single() as TextElement
        assertEquals("new", out.text)
        assertEquals(Transform(0.0, 0.0, 10.0, 10.0), out.transform) // before's geometry preserved
        assertEquals(0, out.zIndex)
    }

    @Test
    fun `CancelText on an existing box discards the draft, keeping the box`() {
        val existing = model(txt("b", text = "hi"), selection = setOf("b"))
        val (begun, token) = begin(existing, "b")
        val r = EditorReducer.reduce(begun, Intent.CancelText("b", token))
        assertEquals("hi", (els(r.model).single() as TextElement).text)
        assertEquals(Interaction.Idle, r.model.interaction)
        assertTrue(r.effects.none { it is Effect.Autosave }, "no change ⇒ no autosave")
    }

    @Test
    fun `CancelText with a stale token or no session is a no-op`() {
        val start = model(txt("a"))
        assertEquals(start, EditorReducer.reduce(start, Intent.CancelText("a", 99L)).model)
        val (begun, token) = begin(model(txt("a"), selection = setOf("a")), "a")
        // wrong token ⇒ session stays open
        val r = EditorReducer.reduce(begun, Intent.CancelText("a", token + 1))
        assertTrue(r.model.interaction is Interaction.EditingText)
    }
}
