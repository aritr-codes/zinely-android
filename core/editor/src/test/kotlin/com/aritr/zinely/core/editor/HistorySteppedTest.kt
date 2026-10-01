package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.Fit
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** ADR-123: each undo/redo emits exactly one [Effect.HistoryStepped], and nothing else speaks. */
class HistorySteppedTest {
    private fun photo(id: String) = ImageElement(id, Transform(10.0, 20.0, 50.0, 40.0), assetId = "a".repeat(64), fit = Fit.FILL)

    private val start = EditorModel(
        document = ZineDocument(
            format = ZineFormat.SINGLE_SHEET_8,
            paperSize = PaperSize.A4,
            pages = List(8) { i ->
                Page(index = i, role = PageRole.INTERIOR, elements = if (i == 2) listOf(photo("p"), photo("q")) else emptyList())
            },
        ),
        currentPageIndex = 2,
    )

    private fun reduce(m: EditorModel, vararg intents: Intent) = intents.fold(m) { acc, i -> EditorReducer.reduce(acc, i).model }

    private val photoBack = EditLabel(EditVerb.DELETE, EditKind.PHOTO)

    @Test
    fun `undo emits one HistoryStepped after the autosave, and nothing else`() {
        val deleted = reduce(start, Intent.Delete(setOf("p")))
        val r = EditorReducer.reduce(deleted, Intent.Undo)
        assertEquals(
            listOf(Effect.Autosave(r.model.document), Effect.HistoryStepped(photoBack, isRedo = false, landedOnPage = null)),
            r.effects,
        )
    }

    @Test
    fun `redo emits the same label marked as a redo`() {
        val undone = reduce(start, Intent.Delete(setOf("p")), Intent.Undo)
        val r = EditorReducer.reduce(undone, Intent.Redo)
        assertEquals(listOf(Effect.HistoryStepped(photoBack, isRedo = true, landedOnPage = null)), r.effects.drop(1))
    }

    @Test
    fun `nothing left to undo or redo emits nothing`() {
        assertTrue(EditorReducer.reduce(start, Intent.Undo).effects.isEmpty())
        assertTrue(EditorReducer.reduce(start, Intent.Redo).effects.isEmpty())
    }

    @Test
    fun `a step that changes page carries the page it landed on, read after the step`() {
        val away = reduce(start, Intent.Delete(setOf("p")), Intent.GoToPage(5))
        val undo = EditorReducer.reduce(away, Intent.Undo)
        assertEquals(2, undo.model.currentPageIndex)
        assertEquals(2, (undo.effects.last() as Effect.HistoryStepped).landedOnPage)

        val awayAgain = reduce(undo.model, Intent.GoToPage(6))
        val redo = EditorReducer.reduce(awayAgain, Intent.Redo)
        assertEquals(Effect.HistoryStepped(photoBack, isRedo = true, landedOnPage = 2), redo.effects.last())
    }

    @Test
    fun `across fold lands on its source page from the partner page`() {
        val spread = reduce(start, Intent.MakeImageSpread("p", 1.5, PtSize(105.0, 148.0)), Intent.GoToPage(1))
        val undo = EditorReducer.reduce(spread, Intent.Undo)
        assertEquals(
            Effect.HistoryStepped(EditLabel(EditVerb.SPREAD, EditKind.PHOTO), isRedo = false, landedOnPage = 2),
            undo.effects.last(),
        )
        val redoHere = EditorReducer.reduce(undo.model, Intent.Redo)
        assertEquals(null, (redoHere.effects.last() as Effect.HistoryStepped).landedOnPage)
    }

    @Test
    fun `identical steps each emit their own event`() {
        val twice = reduce(start, Intent.Delete(setOf("p")), Intent.Delete(setOf("q")))
        val first = EditorReducer.reduce(twice, Intent.Undo)
        val second = EditorReducer.reduce(first.model, Intent.Undo)
        assertEquals(first.effects.last(), second.effects.last())
        assertEquals(1, second.effects.filterIsInstance<Effect.HistoryStepped>().size)
    }

    @Test
    fun `history is untouched by labelling - undo then redo restores the same stacks`() {
        val deleted = reduce(start, Intent.Delete(setOf("p")))
        val round = reduce(deleted, Intent.Undo, Intent.Redo)
        assertEquals(deleted.history, round.history)
        assertEquals(deleted.document, round.document)
    }

    @Test
    fun `page commands step without a snack`() {
        val added = reduce(start, Intent.AddPage)
        val r = EditorReducer.reduce(added, Intent.Undo)
        assertTrue(r.effects.none { it is Effect.HistoryStepped })
    }
}
