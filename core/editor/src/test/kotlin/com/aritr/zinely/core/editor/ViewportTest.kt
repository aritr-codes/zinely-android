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
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [Intent.SetViewport] is display-only (ADR-029 §5): the host feeds the measured canvas scale into the
 * model so the gesture commit and the preview render share one [ViewState], but it must never autosave,
 * enter history, or disturb selection/interaction. Pure.
 */
class ViewportTest {

    private fun model(
        selection: Set<String> = emptySet(),
        interaction: Interaction = Interaction.Idle,
    ) = EditorModel(
        document = ZineDocument(
            format = ZineFormat.SINGLE_SHEET_8,
            paperSize = PaperSize.LETTER,
            pages = listOf(
                Page(
                    index = 0,
                    role = PageRole.INTERIOR,
                    elements = listOf(TextElement("a", Transform(0.0, 0.0, 10.0, 10.0), 0, "hi")),
                ),
            ),
        ),
        selection = selection,
        interaction = interaction,
    )

    @Test
    fun `SetViewport updates the view but emits no autosave and no history`() {
        val start = model()
        val r = EditorReducer.reduce(start, Intent.SetViewport(3.5f, PtPoint(4.0, 6.0)))
        assertEquals(ViewState(3.5f, PtPoint(4.0, 6.0)), r.model.view)
        assertTrue(r.effects.isEmpty(), "viewport is display-only ⇒ no effects")
        assertEquals(start.history, r.model.history)
        assertEquals(start.nextToken, r.model.nextToken, "no id/token consumed")
        assertEquals(start.document, r.model.document)
    }

    @Test
    fun `SetViewport leaves selection and an open interaction untouched`() {
        val tx = Interaction.Transforming(0, setOf("a"), mapOf("a" to Transform(0.0, 0.0, 10.0, 10.0)), 7)
        val start = model(selection = setOf("a"), interaction = tx)
        val r = EditorReducer.reduce(start, Intent.SetViewport(2.0f, PtPoint(0.0, 0.0)))
        assertEquals(setOf("a"), r.model.selection)
        assertEquals(tx, r.model.interaction, "a viewport change mid-gesture can't disturb the session")
    }

    @Test
    fun `an equal SetViewport is a no-op returning the same model`() {
        val start = model().copy(view = ViewState(2.0f, PtPoint(1.0, 1.0)))
        val r = EditorReducer.reduce(start, Intent.SetViewport(2.0f, PtPoint(1.0, 1.0)))
        assertSame(start, r.model, "idempotent — equal view yields the identical model instance")
    }
}
