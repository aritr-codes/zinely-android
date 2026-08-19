package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * `Intent.PlaceSupply` — taking a supply out of the drawer and putting it on the page (SUPPLIES-SPEC §5,
 * ADR-105 step S7).
 *
 * Every fixture here uses `shape.circle`, one of the **four authored** supplies. That is deliberate: a test
 * about decor whose fixture names an unauthored id can pass for the wrong reason, because an unauthored
 * supply draws nothing and half of what one might assert is then vacuous. Absence is never the point below,
 * so absence is never in a fixture.
 */
class PlaceSupplyTest {

    private val ink = ColorRgba(0x2A, 0x25, 0x1E)
    private val box = Transform(10.0, 20.0, 30.0, 40.0)

    private fun modelOf(vararg els: TextElement): EditorModel = EditorModel(
        document = ZineDocument(
            format = ZineFormat.SINGLE_SHEET_8,
            paperSize = PaperSize.LETTER,
            pages = listOf(
                Page(index = 0, role = PageRole.FRONT_COVER, elements = els.toList()),
                Page(index = 1, role = PageRole.INTERIOR),
            ),
        ),
    )

    private fun txt(id: String, z: Int = 0) =
        TextElement(id = id, transform = Transform(0.0, 0.0, 10.0, 10.0), zIndex = z, text = "x")

    private fun place(model: EditorModel, supplyId: String = "shape.circle") =
        EditorReducer.reduce(model, Intent.PlaceSupply(supplyId, ink, box))

    private fun decorOn(model: EditorModel, page: Int = 0) =
        model.document.pages[page].elements.filterIsInstance<DecorElement>()

    @Test
    fun `Given an empty page, When a supply is placed, Then a DecorElement carries the id, ink and box verbatim`() {
        val r = place(modelOf())

        val placed = decorOn(r.model).single()
        assertEquals("shape.circle", placed.supplyId)
        assertEquals(ink, placed.ink)
        assertEquals(box, placed.transform)
        // §5.1: a supply lands flat. The reducer must not add a tilt of its own on top of the caller's box.
        assertEquals(0.0, placed.transform.rotationDegrees)
    }

    @Test
    fun `Given a page with elements, When a supply is placed, Then it lands on top and is selected`() {
        val r = place(modelOf(txt("a", z = 3), txt("b", z = 7)))

        val placed = decorOn(r.model).single()
        assertTrue(placed.zIndex > 7, "a placed supply must land above every element already there")
        assertEquals(setOf(placed.id), r.model.selection)
        assertEquals(3, r.model.document.pages[0].elements.size, "the existing elements are untouched")
    }

    @Test
    fun `Given a placement, When it is reduced, Then exactly one autosave is emitted`() {
        val r = place(modelOf())

        assertEquals(1, r.effects.filterIsInstance<Effect.Autosave>().size)
    }

    @Test
    fun `Given a placement, When Undo runs, Then the supply is gone and Redo brings it back`() {
        val placed = place(modelOf(txt("a"))).model
        assertEquals(1, decorOn(placed).size)
        // One user act ⇒ one undoable command, like every other placement.
        assertEquals(1, placed.history.undo.size)

        val undone = EditorReducer.reduce(placed, Intent.Undo).model
        assertTrue(decorOn(undone).isEmpty(), "undo must remove the placed supply")
        assertTrue(undone.selection.isEmpty(), "a selection may not outlive the element it points at")

        val redone = EditorReducer.reduce(undone, Intent.Redo).model
        assertEquals(1, decorOn(redone).size)
        assertEquals("shape.circle", decorOn(redone).single().supplyId)
    }

    @Test
    fun `Given two placements, When both are reduced, Then their ids differ`() {
        // The id is minted reducer-side from `nextToken` — a collision would make `PlaceCommand.invertOn`
        // delete BOTH matches, which is the trap `CommitAddImage`'s comment already names.
        val once = place(modelOf()).model
        val twice = place(once).model

        val ids = decorOn(twice).map { it.id }
        assertEquals(2, ids.size)
        assertNotEquals(ids[0], ids[1])
    }

    @Test
    fun `Given the second page is current, When a supply is placed, Then it lands on that page`() {
        val onPageTwo = EditorReducer.reduce(modelOf(txt("a")), Intent.GoToPage(1)).model

        val r = place(onPageTwo)

        assertTrue(decorOn(r.model, page = 0).isEmpty(), "page 1 must be untouched")
        assertEquals(1, decorOn(r.model, page = 1).size)
    }

    @Test
    fun `Given a placed supply, When a type-agnostic verb runs, Then decor behaves exactly as a photo does`() {
        // The reducer's own claim about decor (its KDoc): move/restack/delete never name an element type.
        // Asserted here rather than trusted, because all three go through `as?`-free paths that a wrong
        // change would break silently.
        val placed = place(modelOf()).model
        val id = decorOn(placed).single().id

        val nudged = EditorReducer.reduce(placed, Intent.Nudge(com.aritr.zinely.core.model.PtPoint(5.0, 0.0))).model
        assertEquals(box.xPt + 5.0, decorOn(nudged).single().transform.xPt)

        val deleted = EditorReducer.reduce(nudged, Intent.Delete(setOf(id))).model
        assertTrue(decorOn(deleted).isEmpty())
    }
}
