package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.Element
import com.aritr.zinely.core.model.Fit
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * ADR-123 / A26: every command a Bench control raises yields the label A26's table names. Each case is
 * built by the real intent, so the command under test is the one the product records, then labelled the
 * way `stepHistory` labels it (from the document before the step).
 */
class EditLabelTest {
    private val box = Transform(10.0, 20.0, 50.0, 40.0, 0.0)
    private val photo = ImageElement("photo", box, zIndex = 1, assetId = "a".repeat(64), fit = Fit.FILL)
    private val art = DecorElement("art", box, zIndex = 2, supplyId = "shape.star", ink = ColorRgba(10, 20, 30))
    private val text = TextElement("text", box, zIndex = 3, text = "words")

    private fun model(vararg elements: Element, page: Int = 1) = EditorModel(
        document = ZineDocument(
            format = ZineFormat.SINGLE_SHEET_8,
            paperSize = PaperSize.A4,
            pages = List(8) { i ->
                Page(index = i, role = PageRole.INTERIOR, elements = if (i == page) elements.toList() else emptyList())
            },
        ),
        currentPageIndex = page,
    )

    /** The label the last committed command carries, read as an undo reads it. */
    private fun labelAfter(start: EditorModel, vararg intents: Intent): EditLabel? {
        val done = intents.fold(start) { m, i -> EditorReducer.reduce(m, i).model }
        return done.history.undo.last().editLabel(done.document)
    }

    @Test
    fun `delete names each kind and counts several`() {
        assertEquals(EditLabel(EditVerb.DELETE, EditKind.TEXT), labelAfter(model(text), Intent.Delete(setOf("text"))))
        assertEquals(EditLabel(EditVerb.DELETE, EditKind.PHOTO), labelAfter(model(photo), Intent.Delete(setOf("photo"))))
        assertEquals(EditLabel(EditVerb.DELETE, EditKind.ART), labelAfter(model(art), Intent.Delete(setOf("art"))))
        assertEquals(3, labelAfter(model(text, photo, art), Intent.Delete(setOf("text", "photo", "art")))!!.count)
    }

    @Test
    fun `add and duplicate are one physical result (D1 = 1a)`() {
        val placedArt = labelAfter(model(), Intent.PlaceSupply("shape.star", ColorRgba(0, 0, 0), box))
        assertEquals(EditLabel(EditVerb.PLACE, EditKind.ART), placedArt)
        assertEquals(EditLabel(EditVerb.PLACE, EditKind.TEXT), labelAfter(model(), Intent.PlaceText(box, "hi")))
        assertEquals(EditLabel(EditVerb.PLACE, EditKind.PHOTO), labelAfter(model(), Intent.CommitAddImage(photo)))
        val duplicated = labelAfter(model(photo), Intent.DuplicateElement("photo", PtSize(105.0, 148.0)))
        assertEquals(EditLabel(EditVerb.PLACE, EditKind.PHOTO), duplicated)
    }

    @Test
    fun `add text is two steps, empty box then words`() {
        val placed = EditorReducer.reduce(model(), Intent.PlaceTextAndEdit(box)).model
        val id = placed.selection.single()
        val session = placed.interaction as Interaction.EditingText
        val typed = EditorReducer.reduce(
            placed,
            Intent.CommitText(id, TextElement(id, box, text = "hello"), session.token),
        ).model
        val (place, edit) = typed.history.undo
        assertEquals(EditLabel(EditVerb.PLACE, EditKind.EMPTY_BOX), place.editLabel(typed.document))
        assertEquals(EditLabel(EditVerb.WORDS_FROM_EMPTY, null), edit.editLabel(typed.document))
    }

    @Test
    fun `move, resize and turn come from the transform's own diff`() {
        val selected = EditorReducer.reduce(model(photo), Intent.Select("photo")).model
        assertEquals(EditVerb.MOVE, labelAfter(selected, Intent.Nudge(PtPoint(3.0, 0.0)))!!.verb)
        assertEquals(EditVerb.RESIZE, labelAfter(selected, Intent.ScaleBy(1.5))!!.verb)
        assertEquals(EditVerb.TURN, labelAfter(selected, Intent.RotateBy(15.0))!!.verb)
        assertEquals(EditKind.PHOTO, labelAfter(selected, Intent.Nudge(PtPoint(3.0, 0.0)))!!.kind)
    }

    @Test
    fun `restack is one direction-free line`() {
        assertEquals(
            EditLabel(EditVerb.RESTACK, null),
            labelAfter(model(photo, art), Intent.Reorder("photo", ReorderOp.TO_FRONT)),
        )
    }

    @Test
    fun `words, text style, and words over style`() {
        assertEquals(EditVerb.TEXT_STYLE, labelAfter(model(text), Intent.StyleText("text", bold = true))!!.verb)
        val edited = EditTextCommand(1, "text", text, text.copy(text = "other", style = text.style.copy(bold = true)))
        assertEquals(EditLabel(EditVerb.WORDS, null), edited.editLabel(model(text).document))
    }

    @Test
    fun `photo replace, copier, flips and framing`() {
        assertEquals(EditLabel(EditVerb.SWAP, EditKind.PHOTO), labelAfter(model(photo), Intent.ReplaceImage("photo", "b".repeat(64))))
        assertEquals(EditVerb.COPIER_ON, labelAfter(model(photo), Intent.ToggleCopier("photo"))!!.verb)
        assertEquals(EditVerb.COPIER_OFF, labelAfter(model(photo.copy(copier = true)), Intent.ToggleCopier("photo"))!!.verb)
        assertEquals(EditVerb.FLIP_LEFT_RIGHT_ON, labelAfter(model(photo), Intent.ToggleFlip("photo", FlipAxis.HORIZONTAL))!!.verb)
        assertEquals(EditVerb.FLIP_TOP_BOTTOM_ON, labelAfter(model(photo), Intent.ToggleFlip("photo", FlipAxis.VERTICAL))!!.verb)
        val flipped = photo.copy(flippedHorizontally = true, flippedVertically = true)
        assertEquals(EditVerb.FLIP_LEFT_RIGHT_OFF, labelAfter(model(flipped), Intent.ToggleFlip("photo", FlipAxis.HORIZONTAL))!!.verb)
        assertEquals(EditVerb.FLIP_TOP_BOTTOM_OFF, labelAfter(model(flipped), Intent.ToggleFlip("photo", FlipAxis.VERTICAL))!!.verb)
    }

    @Test
    fun `a reframe and a reset are both framing (D2 = 2a)`() {
        val cropped = photo.copy(crop = Crop(0.1, 0.1, 0.9, 0.9))
        assertEquals(EditLabel(EditVerb.FRAMING, null), labelAfter(model(cropped), Intent.ResetFraming("photo")))
        // A manual reframe that happens to land on the default look is still a reframe, not a reset.
        val backToDefault = EditImageCommand(1, "photo", cropped, cropped.copy(crop = Crop.FULL, fit = Fit.FILL))
        assertEquals(EditLabel(EditVerb.FRAMING, null), backToDefault.editLabel(model(cropped).document))
    }

    @Test
    fun `art replace outranks its refit, then ink and art flips`() {
        val swapped = labelAfter(model(art), Intent.ReplaceSupply("art", "tape.torn", box.copy(widthPt = 90.0)))
        assertEquals(EditLabel(EditVerb.SWAP, EditKind.ART), swapped)
        assertEquals(EditLabel(EditVerb.INK, null), labelAfter(model(art), Intent.InkSupply("art", ColorRgba(0, 128, 128))))
        assertEquals(EditVerb.FLIP_LEFT_RIGHT_ON, labelAfter(model(art), Intent.ToggleFlip("art", FlipAxis.HORIZONTAL))!!.verb)
        assertEquals(EditVerb.FLIP_TOP_BOTTOM_ON, labelAfter(model(art), Intent.ToggleFlip("art", FlipAxis.VERTICAL))!!.verb)
    }

    @Test
    fun `replacing an art piece with the same supply is still a swap, never ink`() {
        // Replace refits the box even when the maker picks the piece it already is (Review R1).
        val same = labelAfter(model(art), Intent.ReplaceSupply("art", art.supplyId, box.copy(widthPt = 90.0)))
        assertEquals(EditLabel(EditVerb.SWAP, EditKind.ART), same)
    }

    @Test
    fun `a twin that changes nothing records no step to mislabel`() {
        // Make smaller at the minimum size used to commit a no-op labelled "moved" (Review I3).
        val selected = EditorReducer.reduce(model(photo), Intent.Select("photo")).model
        assertEquals(selected.history, EditorReducer.reduce(selected, Intent.RotateBy(0.0)).model.history)
    }

    @Test
    fun `across fold is a spread of a photo`() {
        val spread = labelAfter(model(photo), Intent.MakeImageSpread("photo", 1.5, PtSize(105.0, 148.0)))
        assertEquals(EditLabel(EditVerb.SPREAD, EditKind.PHOTO), spread)
    }

    @Test
    fun `page commands have no line`() {
        assertNull(AddPageCommand(Page(index = 8, role = PageRole.INTERIOR), 8).editLabel(model().document))
        assertNull(DeletePageCommand(Page(index = 1, role = PageRole.INTERIOR), 1, emptySet()).editLabel(model().document))
    }

    // --- D4 = 4c boundaries: strict `>` against 2% and 2°, magnitudes, the largest change wins ---------

    private fun verb(before: Transform, after: Transform) = transformVerb(mapOf("a" to before), mapOf("a" to after))
    private val base = Transform(0.0, 0.0, 100.0, 50.0, 0.0)

    @Test
    fun `size at exactly two percent is a move and just over is a resize`() {
        assertEquals(EditVerb.MOVE, verb(base, base.copy(widthPt = 102.0)))
        assertEquals(EditVerb.RESIZE, verb(base, base.copy(widthPt = 102.01)))
        assertEquals(EditVerb.MOVE, verb(base, base.copy(heightPt = 51.0)))
        assertEquals(EditVerb.RESIZE, verb(base, base.copy(heightPt = 51.01)))
    }

    @Test
    fun `turn at exactly two degrees is a move and just over is a turn`() {
        assertEquals(EditVerb.MOVE, verb(base, base.copy(rotationDegrees = 2.0)))
        assertEquals(EditVerb.TURN, verb(base, base.copy(rotationDegrees = 2.01)))
    }

    @Test
    fun `a shrink and a counter-clockwise turn count by magnitude`() {
        assertEquals(EditVerb.RESIZE, verb(base, base.copy(widthPt = 50.0)))
        assertEquals(EditVerb.TURN, verb(base, base.copy(rotationDegrees = -10.0)))
        // Shortest way round: 359° to 1° is a 2° slip, not 358°.
        assertEquals(EditVerb.MOVE, verb(base.copy(rotationDegrees = 359.0), base.copy(rotationDegrees = 1.0)))
        assertEquals(EditVerb.TURN, verb(base.copy(rotationDegrees = 350.0), base.copy(rotationDegrees = 5.0)))
    }

    @Test
    fun `size relative to the pre-action box`() {
        // 100 -> 98 is exactly 2% of the pre-action 100 (a slip), but 2.04% of the post-action 98.
        assertEquals(EditVerb.MOVE, verb(base, base.copy(widthPt = 98.0)))
    }

    @Test
    fun `precedence - size beats turn, turn beats a size slip, two slips move`() {
        assertEquals(EditVerb.RESIZE, verb(base, base.copy(widthPt = 110.0, rotationDegrees = 30.0)))
        assertEquals(EditVerb.TURN, verb(base, base.copy(widthPt = 101.0, rotationDegrees = 30.0)))
        assertEquals(EditVerb.MOVE, verb(base, base.copy(xPt = 40.0, widthPt = 101.5, rotationDegrees = 1.5)))
    }

    @Test
    fun `several selected - the largest qualifying change of any of them`() {
        val before = mapOf("a" to base, "b" to base)
        assertEquals(
            EditVerb.RESIZE,
            transformVerb(before, mapOf("a" to base.copy(rotationDegrees = 20.0), "b" to base.copy(widthPt = 105.0))),
        )
        assertEquals(
            EditVerb.TURN,
            transformVerb(before, mapOf("a" to base.copy(widthPt = 101.0), "b" to base.copy(rotationDegrees = 5.0))),
        )
        assertEquals(
            EditVerb.MOVE,
            transformVerb(before, mapOf("a" to base.copy(xPt = 9.0), "b" to base.copy(widthPt = 101.0, rotationDegrees = 1.0))),
        )
    }
}
