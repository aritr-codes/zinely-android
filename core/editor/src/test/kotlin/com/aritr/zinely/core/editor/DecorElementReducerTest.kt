package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.Element
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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * `DecorElement` through the MVI reducer (ADR-105 / SUPPLIES-SPEC §2).
 *
 * **Why this file exists as its own suite.** `EditorReducer.kt` carries ten type-switch sites — the
 * largest concentration anywhere, and one that SUPPLIES-SPEC §10 omits from both S2′ and S7′
 * (corrected by D-029's 2026-08-16 ruling). Only one of the ten is an exhaustive `when`; the other
 * nine are `as?` casts, so **the compiler cannot find them**. A supply that silently refused to move,
 * restack or delete would produce no build error and no red test — it would just be a mark on the page
 * the maker cannot touch. These tests are the only thing standing where the compiler cannot.
 *
 * Two claims are asserted throughout, and the split is the invariant:
 *  - **type-agnostic verbs treat decor exactly as they treat a photo** (move, resize, rotate, restack,
 *    delete, undo);
 *  - **type-specific verbs are no-ops on decor, without throwing and without mutating**.
 *
 * ⚠ **The second claim has been narrowed since P1.** It read *"Replace-supply and Change-ink are S7 and do
 * not exist yet"*. **Change ink now exists** ([Intent.InkSupply]), so decor has a type-specific verb that
 * genuinely mutates, and the no-op claim now covers only *photo-only and text-only* verbs plus Replace
 * supply. The ink tests below assert the opposite direction as well — that `InkSupply` is itself a no-op on
 * a text box and a photo — so the two halves of the invariant are checked against each other rather than
 * each being trusted alone.
 */
class DecorElementReducerTest {

    private fun decor(id: String, x: Double = 0.0, z: Int = 0, supplyId: String = "tape.torn") =
        DecorElement(
            id = id,
            transform = Transform(x, 0.0, 100.0, 40.0),
            zIndex = z,
            supplyId = supplyId,
            ink = ColorRgba(200, 40, 90),
        )

    private fun txt(id: String, z: Int = 0) =
        TextElement(id = id, transform = Transform(0.0, 0.0, 10.0, 10.0), zIndex = z, text = "x")

    private fun modelOf(vararg els: Element): EditorModel = EditorModel(
        document = ZineDocument(
            format = ZineFormat.SINGLE_SHEET_8,
            paperSize = PaperSize.LETTER,
            pages = listOf(Page(index = 0, role = PageRole.FRONT_COVER, elements = els.toList())),
        ),
    )

    private fun els(m: EditorModel) = m.document.pages[0].elements
    private fun theDecor(m: EditorModel) = els(m).filterIsInstance<DecorElement>().single()

    /** Everything a supply carries beyond geometry — nothing a transform verb may disturb. */
    private fun identityOf(d: DecorElement) = Triple(d.supplyId, d.ink, d.mirrored)

    // — type-agnostic verbs: decor is a first-class citizen —

    @Test
    fun `given a selected supply, when nudged, then it moves and keeps its supplyId, ink and mirror`() {
        val start = modelOf(decor("d1")).copy(selection = setOf("d1"))
        val before = theDecor(start)

        val r = EditorReducer.reduce(start, Intent.Nudge(PtPoint(4.0, -4.0)))

        val after = theDecor(r.model)
        assertEquals(4.0, after.transform.xPt)
        assertEquals(-4.0, after.transform.yPt)
        // The point of the whole seam: geometry changed, identity did not.
        assertEquals(identityOf(before), identityOf(after))
        assertEquals(1, r.model.history.undo.size)
        assertTrue(r.effects.any { it is Effect.Autosave })
    }

    @Test
    fun `given a selected supply, when scaled and rotated, then both apply and one command each`() {
        val start = modelOf(decor("d1")).copy(selection = setOf("d1"))

        val scaled = EditorReducer.reduce(start, Intent.ScaleBy(2.0)).model
        assertEquals(200.0, theDecor(scaled).transform.widthPt)
        assertEquals(80.0, theDecor(scaled).transform.heightPt)

        val rotated = EditorReducer.reduce(scaled, Intent.RotateBy(15.0)).model
        assertEquals(15.0, theDecor(rotated).transform.rotationDegrees)
        assertEquals(2, rotated.history.undo.size)
    }

    @Test
    fun `given a transform session on a supply, when committed, then the new transform is stored`() {
        val start = modelOf(decor("d1"))
        val begun = EditorReducer.reduce(start, Intent.BeginTransform(setOf("d1"))).model
        val token = (begun.interaction as Interaction.Transforming).token
        val after = Transform(99.0, 9.0, 40.0, 40.0, 30.0)

        val r = EditorReducer.reduce(begun, Intent.CommitTransform(mapOf("d1" to after), token))

        assertEquals(after, theDecor(r.model).transform)
        assertEquals("tape.torn", theDecor(r.model).supplyId)
    }

    @Test
    fun `given a supply under a photo, when brought to front, then the z-order actually changes`() {
        val start = modelOf(decor("d1", z = 0), txt("t1", z = 1))

        val r = EditorReducer.reduce(start, Intent.Reorder("d1", ReorderOp.TO_FRONT))

        val zByIndex = els(r.model).associate { it.id to it.zIndex }
        assertTrue(
            zByIndex.getValue("d1") > zByIndex.getValue("t1"),
            "decor must restack above the text box, got $zByIndex",
        )
        assertTrue(r.effects.any { it is Effect.Autosave })
    }

    @Test
    fun `given a supply, when deleted then undone, then it comes back byte-identical`() {
        val start = modelOf(decor("d1"), txt("t1")).copy(selection = setOf("d1"))
        val original = theDecor(start)

        val deleted = EditorReducer.reduce(start, Intent.Delete(setOf("d1"))).model
        assertTrue(els(deleted).none { it.id == "d1" })

        val undone = EditorReducer.reduce(deleted, Intent.Undo).model
        assertEquals(original, theDecor(undone))
    }

    @Test
    fun `given a supply on the page, when hit-tested at its box, then it is selectable`() {
        // A supply that could not be selected could not be reached by any verb above.
        val r = EditorReducer.reduce(modelOf(decor("d1")), Intent.SelectAt(PtPoint(50.0, 20.0)))
        assertEquals(setOf("d1"), r.model.selection)
    }

    // — Change ink (SUPPLIES-SPEC §8): decor's first type-specific verb to actually do something —

    @Test
    fun `given a supply, when inked, then only the ink changes and it is one undo step`() {
        val start = modelOf(decor("d1")).copy(selection = setOf("d1"))
        val before = theDecor(start)
        val teal = ColorRgba(0, 128, 128)

        val r = EditorReducer.reduce(start, Intent.InkSupply("d1", teal))

        val after = theDecor(r.model)
        assertEquals(teal, after.ink)
        // The mirror of `identityOf`: this verb changes ink and NOTHING else — not geometry, not the
        // catalogue key, not the mirror flag. A recolour that silently reset the supplyId would still
        // pass an `assertEquals(teal, after.ink)` on its own.
        assertEquals(before.transform, after.transform)
        assertEquals(before.supplyId, after.supplyId)
        assertEquals(before.mirrored, after.mirrored)
        assertEquals(before.zIndex, after.zIndex)
        assertEquals(1, r.model.history.undo.size)
        assertTrue(r.effects.any { it is Effect.Autosave })
    }

    @Test
    fun `given an inked supply, when undone, then the previous ink comes back exactly`() {
        val start = modelOf(decor("d1")).copy(selection = setOf("d1"))
        val original = theDecor(start).ink

        val inked = EditorReducer.reduce(start, Intent.InkSupply("d1", ColorRgba(0, 128, 128))).model
        val undone = EditorReducer.reduce(inked, Intent.Undo).model

        assertEquals(original, theDecor(undone).ink)
        assertTrue(theDecor(undone).ink != ColorRgba(0, 128, 128))
    }

    @Test
    fun `re-picking the ink a supply already has pushes no undo entry`() {
        // Without the `after == el` short-circuit this passes an ink assertion perfectly while quietly
        // filling the undo stack with entries that undo to the same picture — the maker taps Undo and
        // nothing appears to happen. That is the failure this test exists for, not the colour.
        val start = modelOf(decor("d1")).copy(selection = setOf("d1"))
        val same = theDecor(start).ink

        val r = EditorReducer.reduce(start, Intent.InkSupply("d1", same))

        assertTrue(r.model.history.undo.isEmpty(), "an ink that changes nothing must not be undoable")
        assertTrue(r.effects.none { it is Effect.Autosave }, "nor may it autosave")
        assertSame(start.document, r.model.document)
    }

    @Test
    fun `InkSupply is a no-op on a text box, a photo, and an id that is not there`() {
        // InkSupply resolves with `as? DecorElement`, so it is exactly as silent for the types it does
        // not own as the photo/text verbs are for decor. Asserted in both directions on one page.
        val photo = ImageElement(
            id = "i1",
            transform = Transform(0.0, 0.0, 10.0, 10.0),
            zIndex = 2,
            assetId = "a".repeat(64),
        )
        val start = modelOf(decor("d1"), txt("t1"), photo)

        listOf("t1", "i1", "nope").forEach { id ->
            val r = EditorReducer.reduce(start, Intent.InkSupply(id, ColorRgba(0, 128, 128)))
            assertEquals(start.document, r.model.document, "InkSupply($id) must not mutate the document")
            assertTrue(r.model.history.undo.isEmpty(), "InkSupply($id) must not push an undo step")
        }
    }

    // — Replace supply (SUPPLIES-SPEC §8): decor's second type-specific verb —

    @Test
    fun `given a supply, when replaced, then the outline and geometry change but identity does not`() {
        val start = modelOf(decor("d1")).copy(selection = setOf("d1"))
        val before = theDecor(start)
        val newTransform = Transform(5.0, 7.0, 60.0, 60.0)

        val r = EditorReducer.reduce(
            start,
            Intent.ReplaceSupply("d1", supplyId = "shape.rect", transform = newTransform),
        )

        val after = theDecor(r.model)
        assertEquals("shape.rect", after.supplyId)
        assertEquals(newTransform, after.transform)
        // The whole contract of a swap: the element is the SAME element wearing a different outline.
        // Ink especially — the maker chose that colour for this spot, not for that shape.
        assertEquals(before.id, after.id)
        assertEquals(before.ink, after.ink)
        assertEquals(before.mirrored, after.mirrored)
        assertEquals(before.zIndex, after.zIndex)
        assertEquals(1, r.model.history.undo.size)
        assertTrue(r.effects.any { it is Effect.Autosave })
    }

    @Test
    fun `given a replaced supply, when undone, then the original outline and geometry both come back`() {
        val start = modelOf(decor("d1", supplyId = "tape.torn")).copy(selection = setOf("d1"))
        val before = theDecor(start)

        val swapped = EditorReducer.reduce(
            start,
            Intent.ReplaceSupply("d1", "shape.rect", Transform(5.0, 7.0, 60.0, 60.0)),
        ).model
        val undone = theDecor(EditorReducer.reduce(swapped, Intent.Undo).model)

        // Both halves, because the command carries one memento for the pair: a revert that restored the
        // outline but kept the new family's size would leave a torn tape in a square box.
        assertEquals("tape.torn", undone.supplyId)
        assertEquals(before.transform, undone.transform)
    }

    @Test
    fun `replacing a supply with itself at the same size pushes no undo entry`() {
        val start = modelOf(decor("d1", supplyId = "shape.rect")).copy(selection = setOf("d1"))
        val same = theDecor(start)

        val r = EditorReducer.reduce(
            start,
            Intent.ReplaceSupply("d1", supplyId = same.supplyId, transform = same.transform),
        )

        assertTrue(r.model.history.undo.isEmpty(), "a swap that changes nothing must not be undoable")
        assertSame(start.document, r.model.document)
    }

    @Test
    fun `ReplaceSupply is a no-op on a text box, a photo, and an id that is not there`() {
        val photo = ImageElement(
            id = "i1",
            transform = Transform(0.0, 0.0, 10.0, 10.0),
            zIndex = 2,
            assetId = "a".repeat(64),
        )
        val start = modelOf(decor("d1"), txt("t1"), photo)

        listOf("t1", "i1", "nope").forEach { id ->
            val r = EditorReducer.reduce(
                start,
                Intent.ReplaceSupply(id, "shape.rect", Transform(0.0, 0.0, 20.0, 20.0)),
            )
            assertEquals(start.document, r.model.document, "ReplaceSupply($id) must not mutate")
            assertTrue(r.model.history.undo.isEmpty(), "ReplaceSupply($id) must not push an undo step")
        }
    }

    // — type-specific verbs: a documented, non-throwing no-op —

    @Test
    fun `given a supply, when a photo-only or text-only verb is dispatched, then nothing happens`() {
        val start = modelOf(decor("d1")).copy(selection = setOf("d1"))
        val intents = listOf(
            Intent.BeginEditText("d1"),
            Intent.BeginReframe("d1"),
            Intent.ResetFraming("d1"),
            Intent.ToggleCopier("d1"),
            Intent.ReplaceImage("d1", "b".repeat(64)),
            Intent.StyleText("d1", sizePt = 40.0),
        )

        intents.forEach { intent ->
            val r = EditorReducer.reduce(start, intent)
            assertEquals(start.document, r.model.document, "$intent must not mutate the document")
            assertTrue(r.model.history.undo.isEmpty(), "$intent must not push an undo step")
            assertTrue(r.effects.none { it is Effect.Autosave }, "$intent must not autosave")
            assertTrue(r.model.interaction is Interaction.Idle, "$intent must not open a session")
        }
    }

    @Test
    fun `given a supply, when double-tapped, then it is a named no-op and not an unhandled case`() {
        // The `DoubleTapAt` arm is the ONE exhaustive site in this file. It is written as an explicit
        // `is DecorElement ->` rather than left to the `else`, so the decision is visible in the source.
        val start = modelOf(decor("d1"))
        val r = EditorReducer.reduce(start, Intent.DoubleTapAt(PtPoint(50.0, 20.0)))
        assertSame(start.document, r.model.document)
        assertTrue(r.model.interaction is Interaction.Idle)
    }

    @Test
    fun `a supply coexists with a photo and a text box on one page without disturbing either`() {
        val photo = ImageElement(
            id = "i1",
            transform = Transform(0.0, 0.0, 10.0, 10.0),
            zIndex = 2,
            assetId = "a".repeat(64),
        )
        val start = modelOf(decor("d1", z = 0), txt("t1", z = 1), photo).copy(selection = setOf("t1"))

        val r = EditorReducer.reduce(start, Intent.Nudge(PtPoint(4.0, 0.0)))

        assertEquals(4.0, els(r.model).single { it.id == "t1" }.transform.xPt)
        assertEquals(0.0, theDecor(r.model).transform.xPt)
        assertNotNull(els(r.model).single { it.id == "i1" })
    }
}
