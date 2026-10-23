package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The pure MVI reducer (ADR-029 §2): `reduce(model, intent) -> Reduction(model, effects)`. No I/O —
 * autosave/image-decode are returned as [Effect]s. Autosave is emitted ONLY by document-mutating intents
 * (Codex required-fix #5); stale transform commits are rejected by token (required-fix #1).
 */
class EditorReducerTest {

    private fun txt(id: String, x: Double = 0.0, z: Int = 0, text: String = "x") =
        TextElement(id = id, transform = Transform(x, 0.0, 10.0, 10.0), zIndex = z, text = text)

    private fun modelOf(vararg els: TextElement): EditorModel = EditorModel(
        document = ZineDocument(
            format = ZineFormat.SINGLE_SHEET_8,
            paperSize = PaperSize.LETTER,
            pages = listOf(Page(index = 0, role = PageRole.FRONT_COVER, elements = els.toList())),
        ),
    )

    private fun firstPageEls(m: EditorModel) = m.document.pages[0].elements

    @Test
    fun `Select sets selection and emits NO autosave`() {
        val r = EditorReducer.reduce(modelOf(txt("a")), Intent.Select("a"))
        assertEquals(setOf("a"), r.model.selection)
        assertTrue(r.effects.none { it is Effect.Autosave }, "selection must not trigger autosave")
    }

    @Test
    fun `SelectAt hit-tests and selects the topmost element`() {
        val r = EditorReducer.reduce(modelOf(txt("a")), Intent.SelectAt(PtPoint(5.0, 5.0)))
        assertEquals(setOf("a"), r.model.selection)
    }

    @Test
    fun `PlaceText appends, selects it, and emits autosave`() {
        val r = EditorReducer.reduce(modelOf(txt("a")), Intent.PlaceText(Transform(20.0, 20.0, 30.0, 10.0), "hi"))
        assertEquals(2, firstPageEls(r.model).size)
        assertTrue(r.effects.any { it is Effect.Autosave })
        assertEquals(1, r.model.selection.size)
    }

    @Test
    fun `BeginTransform then CommitTransform records ONE TransformCommand and emits autosave`() {
        val start = modelOf(txt("a"))
        val begun = EditorReducer.reduce(start, Intent.BeginTransform(setOf("a"))).model
        assertTrue(begun.interaction is Interaction.Transforming)
        val token = (begun.interaction as Interaction.Transforming).token
        val after = Transform(99.0, 9.0, 40.0, 40.0, 30.0)
        val r = EditorReducer.reduce(begun, Intent.CommitTransform(mapOf("a" to after), token))
        assertEquals(after, (firstPageEls(r.model).single() as TextElement).transform)
        assertEquals(1, r.model.history.undo.size)
        assertTrue(r.model.interaction is Interaction.Idle)
        assertTrue(r.effects.any { it is Effect.Autosave })
    }

    @Test
    fun `CommitTransform that ends where it began closes the session without an undo step`() {
        // Given one undone edit waiting on the redo stack
        val start = modelOf(txt("a"))
        val placed = EditorReducer.reduce(start, Intent.PlaceText(Transform(20.0, 20.0, 30.0, 10.0), "hi")).model
        val undone = EditorReducer.reduce(placed, Intent.Undo).model
        val begun = EditorReducer.reduce(undone, Intent.BeginTransform(setOf("a"))).model
        val token = (begun.interaction as Interaction.Transforming).token

        // When a drag returns the element exactly to where it started
        val r = EditorReducer.reduce(begun, Intent.CommitTransform(mapOf("a" to txt("a").transform), token))

        // Then nothing is recorded, nothing is saved, and Redo survives
        assertTrue(r.model.interaction is Interaction.Idle)
        assertEquals(undone.history, r.model.history)
        assertTrue(r.effects.none { it is Effect.Autosave })
    }

    @Test
    fun `CommitTransform with a stale token is a no-op`() {
        val begun = EditorReducer.reduce(modelOf(txt("a")), Intent.BeginTransform(setOf("a"))).model
        val r = EditorReducer.reduce(begun, Intent.CommitTransform(mapOf("a" to Transform(1.0, 1.0, 1.0, 1.0)), token = 999_999))
        assertEquals(begun.document, r.model.document) // unchanged
        assertTrue(r.model.history.undo.isEmpty())
    }

    @Test
    fun `CancelTransform idles its own session but a stale-token cancel is a no-op`() {
        val begun = EditorReducer.reduce(modelOf(txt("a")), Intent.BeginTransform(setOf("a"))).model
        val token = (begun.interaction as Interaction.Transforming).token
        // A stale cancel (a newer session would carry a different token) must NOT wipe the live session.
        val stale = EditorReducer.reduce(begun, Intent.CancelTransform(token + 1))
        assertEquals(begun.interaction, stale.model.interaction)
        // The matching cancel idles it and discards the preview (no command, no autosave).
        val r = EditorReducer.reduce(begun, Intent.CancelTransform(token))
        assertTrue(r.model.interaction is Interaction.Idle)
        assertTrue(r.model.history.undo.isEmpty())
        assertTrue(r.effects.none { it is Effect.Autosave })
    }

    @Test
    fun `Delete removes the element and clears it from selection`() {
        val start = modelOf(txt("a"), txt("b")).copy(selection = setOf("a"))
        val r = EditorReducer.reduce(start, Intent.Delete(setOf("a")))
        assertEquals(listOf("b"), firstPageEls(r.model).map { it.id })
        assertFalse("a" in r.model.selection)
        assertEquals(1, r.model.history.undo.size)
    }

    @Test
    fun `Undo reverts the last command and Redo re-applies it`() {
        val start = modelOf(txt("a"))
        val placed = EditorReducer.reduce(start, Intent.PlaceText(Transform(0.0, 0.0, 5.0, 5.0), "z")).model
        assertEquals(2, firstPageEls(placed).size)
        val undone = EditorReducer.reduce(placed, Intent.Undo).model
        assertEquals(1, firstPageEls(undone).size)
        assertTrue(undone.history.undo.isEmpty())
        val redone = EditorReducer.reduce(undone, Intent.Redo).model
        assertEquals(2, firstPageEls(redone).size)
    }

    @Test
    fun `a new committing intent clears the redo stack`() {
        val start = modelOf(txt("a"))
        val placed = EditorReducer.reduce(start, Intent.PlaceText(Transform(0.0, 0.0, 5.0, 5.0), "z")).model
        val undone = EditorReducer.reduce(placed, Intent.Undo).model
        assertTrue(undone.history.redo.isNotEmpty())
        val afterNew = EditorReducer.reduce(undone, Intent.PlaceText(Transform(1.0, 1.0, 5.0, 5.0), "q")).model
        assertTrue(afterNew.history.redo.isEmpty())
    }

    @Test
    fun `Reorder BringForward changes zIndex and records a ReorderCommand`() {
        val start = modelOf(txt("a", z = 0), txt("b", z = 1))
        val r = EditorReducer.reduce(start, Intent.Reorder("a", ReorderOp.BRING_FORWARD))
        assertEquals(mapOf("a" to 1, "b" to 0), firstPageEls(r.model).associate { it.id to it.zIndex })
        assertTrue(r.model.history.undo.single() is ReorderCommand)
    }

    @Test
    fun `Nudge translates the selected element via the same commit path`() {
        val start = modelOf(txt("a", x = 10.0)).copy(selection = setOf("a"))
        val r = EditorReducer.reduce(start, Intent.Nudge(PtPoint(4.0, 0.0)))
        assertEquals(14.0, (firstPageEls(r.model).single() as TextElement).transform.xPt, 1e-9)
        assertTrue(r.model.history.undo.single() is TransformCommand)
        assertTrue(r.effects.any { it is Effect.Autosave })
    }

    @Test
    fun `RequestAddImage emits PickAndDecodeImage and no autosave`() {
        val r = EditorReducer.reduce(modelOf(txt("a")), Intent.RequestAddImage)
        assertTrue(r.effects.any { it is Effect.PickAndDecodeImage })
        assertTrue(r.effects.none { it is Effect.Autosave })
    }

    @Test
    fun `GoToPage changes the current page and emits no autosave`() {
        val twoPage = modelOf(txt("a")).let {
            it.copy(document = it.document.copy(pages = it.document.pages + Page(1, PageRole.INTERIOR)))
        }
        val r = EditorReducer.reduce(twoPage, Intent.GoToPage(1))
        assertEquals(1, r.model.currentPageIndex)
        assertTrue(r.effects.none { it is Effect.Autosave })
    }

    // W1 / D-141: the counter a snack's Undo is tied to. Screen state, not document state.
    @Test
    fun history_revision_moves_on_every_push_undo_and_redo() {
        val start = modelOf(txt("a"))
        assertEquals(0L, start.toUiState().historyRevision)
        val placed = EditorReducer.reduce(start, Intent.PlaceText(Transform(20.0, 20.0, 30.0, 10.0), "hi")).model
        assertEquals(1L, placed.toUiState().historyRevision, "a push moves it")
        val selected = EditorReducer.reduce(placed, Intent.Select("a")).model
        assertEquals(1L, selected.toUiState().historyRevision, "a selection is no history step")
        val undone = EditorReducer.reduce(selected, Intent.Undo).model
        assertEquals(2L, undone.toUiState().historyRevision, "an undo moves it")
        val redone = EditorReducer.reduce(undone, Intent.Redo).model
        assertEquals(3L, redone.toUiState().historyRevision, "a redo moves it")
        // Undo then a new edit: the undo depth repeats, the revision does not.
        val undoneAgain = EditorReducer.reduce(redone, Intent.Undo).model
        val edited = EditorReducer.reduce(undoneAgain, Intent.PlaceText(Transform(0.0, 40.0, 30.0, 10.0), "yo")).model
        assertEquals(redone.history.undo.size, edited.history.undo.size)
        assertTrue(edited.toUiState().historyRevision > redone.toUiState().historyRevision)
        // An empty undo stack: nothing happens, nothing moves.
        assertEquals(0L, EditorReducer.reduce(start, Intent.Undo).model.toUiState().historyRevision)
    }

    @Test
    fun history_revision_moves_when_a_fresh_blank_box_is_dropped() {
        val placed = EditorReducer.reduce(modelOf(), Intent.PlaceTextAndEdit(Transform(0.0, 0.0, 30.0, 10.0))).model
        val session = placed.interaction as Interaction.EditingText
        val dropped = EditorReducer.reduce(placed, Intent.CancelText(session.id, session.token)).model
        assertTrue(dropped.history.undo.isEmpty())
        assertTrue(dropped.toUiState().historyRevision > placed.toUiState().historyRevision)
    }
}
