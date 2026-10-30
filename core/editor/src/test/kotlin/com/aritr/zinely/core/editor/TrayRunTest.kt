package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.Fit
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
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A48.9 / ADR-143 decision 6: consecutive tray-sourced taps in one Arrange group on one piece are one undo
 * step; anything else ends the run. Every case below is one of A48's BUILD TEST CASES or ADR-143's
 * Consequences, built with the real intents.
 */
class TrayRunTest {
    private val box = Transform(10.0, 20.0, 50.0, 40.0, 0.0)
    private val photo = ImageElement("photo", box, zIndex = 1, assetId = "a".repeat(64), fit = Fit.FILL)
    private val other = TextElement("text", Transform(100.0, 100.0, 20.0, 20.0), zIndex = 2, text = "x")

    private val selected: EditorModel = EditorModel(
        document = ZineDocument(
            format = ZineFormat.SINGLE_SHEET_8,
            paperSize = PaperSize.A4,
            pages = List(2) { i ->
                Page(index = i, role = PageRole.INTERIOR, elements = if (i == 0) listOf(photo, other) else emptyList())
            },
        ),
        selection = setOf("photo"),
    )

    private val tray = StepSource.TRAY
    private val right = Intent.Nudge(PtPoint(4.0, 0.0), tray)
    private val left = Intent.Nudge(PtPoint(-4.0, 0.0), tray)
    private val larger = Intent.ScaleBy(1.1, tray)
    private val smaller = Intent.ScaleBy(1.0 / 1.1, tray)
    private val clockwise = Intent.RotateBy(15.0, tray)

    private fun run(start: EditorModel, vararg intents: Intent): EditorModel =
        intents.fold(start) { m, i -> EditorReducer.reduce(m, i).model }

    private fun EditorModel.photoT() = document.pages[0].elements.first { it.id == "photo" }.transform

    /** A model with one undone edit waiting on Redo, so a run's "Redo as it was" is observable. */
    private fun withRedo(): EditorModel {
        val placed = run(selected, Intent.PlaceText(Transform(0.0, 0.0, 5.0, 5.0), "r"), Intent.Undo)
        return placed.copy(selection = setOf("photo"))
    }

    @Test
    fun `a run of three Move taps is one step, labelled moved, and one Undo puts all three back`() {
        val m = run(selected, right, right, right)
        assertEquals(1, m.history.undo.size)
        assertEquals(box.xPt + 12.0, m.photoT().xPt)
        assertEquals(EditVerb.MOVE, m.history.undo.last().editLabel(m.document)!!.verb)
        assertEquals(box, run(m, Intent.Undo).photoT())
    }

    @Test
    fun `Move left then Move right is no step, before exact, Redo kept`() {
        val start = withRedo()
        val m = run(start, left, right)
        assertEquals(start.history.undo, m.history.undo)
        assertEquals(start.history.redo, m.history.redo)
        assertEquals(box, m.photoT())
        assertNull(m.trayRun)
    }

    @Test
    fun `SIZE THERE-AND-BACK - larger then smaller is no step, before restored exactly, Redo as before`() {
        val start = withRedo()
        val grown = run(start, larger)
        assertTrue(grown.history.redo.isEmpty(), "the first tap commits like any tap and clears Redo")
        val m = run(grown, smaller)
        assertEquals(box, m.photoT()) // exactly: not the bakeCentreAnchored drift
        assertEquals(start.history, m.history)
    }

    @Test
    fun `a Turn run of 24 taps (360) is no step and before is restored exactly, not 360`() {
        val start = withRedo()
        val m = (1..24).fold(start) { acc, _ -> run(acc, clockwise) }
        assertEquals(0.0, m.photoT().rotationDegrees)
        assertEquals(box, m.photoT())
        assertEquals(start.history, m.history)
    }

    @Test
    fun `extending a run and removing one each emit Autosave of the resulting document`() {
        val first = EditorReducer.reduce(selected, right)
        val second = EditorReducer.reduce(first.model, right)
        assertEquals(listOf(Effect.Autosave(second.model.document)), second.effects)
        val back = EditorReducer.reduce(run(second.model, left), left)
        assertEquals(selected.document, back.model.document)
        assertEquals(listOf(Effect.Autosave(selected.document)), back.effects)
    }

    @Test
    fun `A41_2 - the history revision moves on every merge and on the removal`() {
        var m = run(selected, right)
        for (tap in listOf(right, left, left)) {
            val next = EditorReducer.reduce(m, tap).model
            assertNotEquals(m.historyRevision, next.historyRevision)
            m = next
        }
    }

    @Test
    fun `a TalkBack element action with the tray open is its own step and ends the run`() {
        val m = run(selected, right, Intent.Nudge(PtPoint(4.0, 0.0)), right)
        assertEquals(3, m.history.undo.size)
    }

    @Test
    fun `Straighten (RotateBy, non-tray) is its own step and ends a Turn run`() {
        val m = run(selected, clockwise, Intent.RotateBy(-15.0), clockwise)
        assertEquals(3, m.history.undo.size)
    }

    @Test
    fun `another group, a Layer tap, a selection change and the tray closing each end a run`() {
        assertEquals(2, run(selected, right, clockwise).history.undo.size)
        assertEquals(3, run(selected, right, Intent.Reorder("photo", ReorderOp.TO_FRONT), right).history.undo.size)
        assertEquals(2, run(selected, right, Intent.Select("text"), Intent.Select("photo"), right).history.undo.size)
        assertEquals(2, run(selected, right, Intent.TrayClosed, right).history.undo.size)
        // A tap on another piece while the tray stays open: the run never follows the selection.
        assertEquals(2, run(selected, right, Intent.Select("text"), right).history.undo.size)
    }

    @Test
    fun `a page change ends a run`() {
        val m = run(selected, right, Intent.GoToPage(1), Intent.GoToPage(0), Intent.Select("photo"), right)
        assertEquals(2, m.history.undo.size)
    }

    @Test
    fun `Undo then Redo of a run's step, then a same-group tap, is a new step (identity guard)`() {
        val m = run(selected, right, right, Intent.Undo, Intent.Redo, right)
        assertEquals(2, m.history.undo.size)
    }

    @Test
    fun `a BeginTransform that records nothing still ends the run`() {
        val begun = run(selected, right, Intent.BeginTransform(setOf("photo")))
        val token = (begun.interaction as Interaction.Transforming).token
        val m = run(begun, Intent.CancelTransform(token), right)
        assertEquals(2, m.history.undo.size)
    }

    @Test
    fun `the run marker holds the piece, the group, the run's own command and the Redo before it`() {
        val start = withRedo()
        val m = run(start, right, right)
        val marker = m.trayRun!!
        assertEquals("photo", marker.id)
        assertEquals(TrayGroup.MOVE, marker.group)
        assertSame(m.history.undo.last(), marker.command)
        assertEquals(start.history.redo, marker.redoBefore)
    }

    @Test
    fun `a 0 to 360 knob drag records no step and keeps Redo`() {
        val start = withRedo()
        val begun = run(start, Intent.BeginTransform(setOf("photo")))
        val token = (begun.interaction as Interaction.Transforming).token
        val r = EditorReducer.reduce(begun, Intent.CommitTransform(mapOf("photo" to box.copy(rotationDegrees = 360.0)), token))
        assertEquals(start.history, r.model.history)
        assertEquals(box, r.model.photoT())
        assertTrue(r.effects.none { it is Effect.Autosave })
    }

    @Test
    fun `a knob commit leaves x, y, width and height bit-identical`() {
        val odd = box.copy(xPt = 10.1, yPt = 0.3, widthPt = 33.3, heightPt = 7.7)
        val start = selected.copy(
            document = selected.document.copy(
                pages = selected.document.pages.mapIndexed { i, p ->
                    if (i == 0) p.copy(elements = listOf(photo.copy(transform = odd), other)) else p
                },
            ),
        )
        val begun = run(start, Intent.BeginTransform(setOf("photo")))
        val token = (begun.interaction as Interaction.Transforming).token
        val m = run(begun, Intent.CommitTransform(mapOf("photo" to odd.copy(rotationDegrees = 37.0)), token))
        assertEquals(odd.copy(rotationDegrees = 37.0), m.photoT())
        assertEquals(EditVerb.TURN, m.history.undo.last().editLabel(m.document)!!.verb)
    }
}
