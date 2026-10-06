package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.toUiState
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.Element
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [benchTapIntent] through a holed piece (ADR-124, test 16). Pure: the decision, not the delivery —
 * tap delivery is a device-verification item, as [benchTapIntent]'s own KDoc explains.
 */
class TapThroughIntentTest {

    private val words = TextElement("words", Transform(0.0, 0.0, 200.0, 200.0), zIndex = 0, text = "x")
    private val window = DecorElement("window", Transform(50.0, 50.0, 100.0, 100.0), 1, "paper.window", ColorRgba(0, 0, 0))
    private val hole = PtPoint(100.0, 100.0)
    private val band = PtPoint(55.0, 100.0)

    private fun state(selection: Set<String>, vararg els: Element) = EditorModel(
        document = ZineDocument(
            format = ZineFormat.SINGLE_SHEET_8,
            paperSize = PaperSize.LETTER,
            pages = listOf(Page(index = 0, role = PageRole.INTERIOR, elements = els.toList())),
        ),
        selection = selection,
    ).toUiState()

    @Test
    fun a_tap_through_a_hole_onto_words_that_are_already_selected_begins_editing_them() {
        assertEquals(Intent.BeginEditText("words"), benchTapIntent(state(setOf("words"), words, window), hole, 4.0))
    }

    @Test
    fun every_other_tap_is_a_SelectAt_carrying_the_tolerance_it_was_given() {
        // Words under the hole, not yet selected; the band, with the words selected; and blank paper.
        assertEquals(Intent.SelectAt(hole, 4.0), benchTapIntent(state(emptySet(), words, window), hole, 4.0))
        assertEquals(Intent.SelectAt(band, 4.0), benchTapIntent(state(setOf("words"), words, window), band, 4.0))
        assertEquals(Intent.SelectAt(hole, 4.0), benchTapIntent(state(emptySet(), window), hole, 4.0))
    }
}
