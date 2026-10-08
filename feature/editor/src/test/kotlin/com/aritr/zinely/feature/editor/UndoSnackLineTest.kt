package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.editor.EditKind
import com.aritr.zinely.core.editor.EditLabel
import com.aritr.zinely.core.editor.EditVerb
import com.aritr.zinely.core.editor.Effect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * ADR-123: the words are A26's, row by row (`v21-bench.html` A26 `ROWS`), and a redo never says the undo line.
 * Literals on purpose: a test that spends the `Copy.Undo` constants it checks cannot see a changed word.
 */
class UndoSnackLineTest {
    private fun line(verb: EditVerb, kind: EditKind? = null, count: Int = 1, redo: Boolean = false, landed: Int? = null, page: Int = 1) =
        undoSnackLine(Effect.HistoryStepped(EditLabel(verb, kind, count), isRedo = redo, landedOnPage = landed), page)

    /** [verb, kind, count] -> [undo line, redo line], exactly as A26's table renders them. */
    private val rows = listOf(
        Triple(EditVerb.DELETE, EditKind.TEXT, 1) to ("Text put back" to "Text removed"),
        Triple(EditVerb.DELETE, EditKind.PHOTO, 1) to ("Photo put back" to "Photo removed"),
        Triple(EditVerb.DELETE, EditKind.ART, 1) to ("Art piece put back" to "Art piece removed"),
        Triple(EditVerb.DELETE, EditKind.PHOTO, 3) to ("3 things put back" to "3 things removed"),
        Triple(EditVerb.PLACE, EditKind.TEXT, 1) to ("Text taken off" to "Text added"),
        Triple(EditVerb.PLACE, EditKind.PHOTO, 1) to ("Photo taken off" to "Photo added"),
        Triple(EditVerb.PLACE, EditKind.ART, 1) to ("Art piece taken off" to "Art piece added"),
        Triple(EditVerb.PLACE, EditKind.PHOTO, 3) to ("3 things taken off" to "3 things added"),
        Triple(EditVerb.PLACE, EditKind.EMPTY_BOX, 1) to ("Empty box taken off" to "Empty box added"),
        Triple(EditVerb.MOVE, EditKind.PHOTO, 1) to ("Photo moved back" to "Photo moved"),
        Triple(EditVerb.MOVE, EditKind.TEXT, 3) to ("3 things moved back" to "3 things moved"),
        Triple(EditVerb.RESIZE, EditKind.PHOTO, 1) to ("Photo resized back" to "Photo resized"),
        Triple(EditVerb.RESIZE, EditKind.ART, 1) to ("Art piece resized back" to "Art piece resized"),
        Triple(EditVerb.TURN, EditKind.TEXT, 1) to ("Text turned back" to "Text turned"),
        Triple(EditVerb.RESTACK, null, 1) to ("Stacking put back" to "Stacking changed"),
        Triple(EditVerb.WORDS, null, 1) to ("Words put back" to "Words changed"),
        Triple(EditVerb.WORDS_FROM_EMPTY, null, 1) to ("Words taken off" to "Words added"),
        Triple(EditVerb.TEXT_STYLE, null, 1) to ("Text style put back" to "Text style changed"),
        Triple(EditVerb.FONT, null, 1) to ("Font put back" to "Font changed"),
        Triple(EditVerb.SWAP, EditKind.PHOTO, 1) to ("Photo swapped back" to "Photo swapped"),
        Triple(EditVerb.SWAP, EditKind.ART, 1) to ("Art piece swapped back" to "Art piece swapped"),
        Triple(EditVerb.COPIER_ON, null, 1) to ("Copier taken off" to "Copier added"),
        Triple(EditVerb.COPIER_OFF, null, 1) to ("Copier put back" to "Copier removed"),
        Triple(EditVerb.FLIP_LEFT_RIGHT_ON, null, 1) to ("Left-right flip taken off" to "Left-right flip added"),
        Triple(EditVerb.FLIP_LEFT_RIGHT_OFF, null, 1) to ("Left-right flip put back" to "Left-right flip removed"),
        Triple(EditVerb.FLIP_TOP_BOTTOM_ON, null, 1) to ("Top-bottom flip taken off" to "Top-bottom flip added"),
        Triple(EditVerb.FLIP_TOP_BOTTOM_OFF, null, 1) to ("Top-bottom flip put back" to "Top-bottom flip removed"),
        Triple(EditVerb.INK, null, 1) to ("Ink put back" to "Ink changed"),
        Triple(EditVerb.FRAMING, null, 1) to ("Framing put back" to "Framing changed"),
        Triple(EditVerb.SPREAD, EditKind.PHOTO, 1) to ("Photo back on one page" to "Photo runs across pages 2 & 3"),
    )

    @Test
    fun every_a26_row_says_its_undo_line_and_its_own_forward_line() {
        for ((label, words) in rows) {
            val (verb, kind, count) = label
            val (undo, redo) = words
            assertEquals("$label undo", undo, line(verb, kind, count))
            assertEquals("$label redo", redo, line(verb, kind, count, redo = true))
            assertNotEquals("$label redo must not repeat the undo line", undo, redo)
        }
    }

    @Test
    fun every_verb_has_a_row() {
        assertEquals(EditVerb.entries.toSet(), rows.map { it.first.first }.toSet())
    }

    @Test
    fun no_page_change_means_no_clause() {
        assertEquals("Photo put back", line(EditVerb.DELETE, EditKind.PHOTO, landed = null, page = 2))
    }

    @Test
    fun a_page_change_appends_the_page_landed_on() {
        assertEquals("Photo put back, page 3", line(EditVerb.DELETE, EditKind.PHOTO, landed = 2, page = 2))
        assertEquals("Top-bottom flip taken off, page 8", line(EditVerb.FLIP_TOP_BOTTOM_ON, landed = 7, page = 7))
        assertEquals("Photo removed, page 3", line(EditVerb.DELETE, EditKind.PHOTO, redo = true, landed = 2, page = 2))
    }

    @Test
    fun across_fold_undo_takes_the_clause_only_when_it_changes_page() {
        assertEquals("Photo back on one page", line(EditVerb.SPREAD, EditKind.PHOTO, page = 2))
        assertEquals("Photo back on one page, page 3", line(EditVerb.SPREAD, EditKind.PHOTO, landed = 2, page = 2))
    }

    @Test
    fun across_fold_redo_names_its_pages_and_never_takes_a_clause() {
        assertEquals("Photo runs across pages 2 & 3", line(EditVerb.SPREAD, EditKind.PHOTO, redo = true, landed = 2, page = 2))
        assertEquals("Photo runs across pages 8 & 1", line(EditVerb.SPREAD, EditKind.PHOTO, redo = true, landed = 0, page = 0))
        assertEquals("Photo runs across pages 6 & 7", line(EditVerb.SPREAD, EditKind.PHOTO, redo = true, page = 5))
    }
}
