package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.Element
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The image Reframe session + replace/reset one-shots + placement default (ADR-053): a session begins/commits/
 * cancels like the text-edit + transform sessions — one session/one-shot is exactly one [EditImageCommand]; a
 * stale [token] is rejected; a commit takes only crop/fit and clamps it; the placement default is `Fit.FILL`
 * applied at placement time; existing documents are never rewritten. Pure, Given-When-Then.
 */
class ImageFramingSessionTest {

    private fun img(
        id: String,
        assetId: String = "sha-$id",
        crop: Crop = Crop.FULL,
        fit: Fit = Fit.FILL,
        transform: Transform = Transform(0.0, 0.0, 10.0, 10.0),
        z: Int = 0,
    ) = ImageElement(id = id, transform = transform, zIndex = z, assetId = assetId, crop = crop, fit = fit)

    private fun model(vararg els: Element, selection: Set<String> = emptySet()) = EditorModel(
        document = ZineDocument(
            format = ZineFormat.SINGLE_SHEET_8,
            paperSize = PaperSize.LETTER,
            pages = listOf(Page(index = 0, role = PageRole.INTERIOR, elements = els.toList())),
        ),
        selection = selection,
    )

    private fun els(m: EditorModel) = m.document.pages[0].elements
    private fun image(m: EditorModel, id: String) = els(m).first { it.id == id } as ImageElement

    private fun begin(m: EditorModel, id: String): Pair<EditorModel, Long> {
        val r = EditorReducer.reduce(m, Intent.BeginReframe(id))
        return r.model to (r.model.interaction as Interaction.Reframing).token
    }

    // — begin —

    @Test
    fun `BeginReframe opens a session selecting the image and advancing the token, no autosave`() {
        val start = model(img("i"))
        val r = EditorReducer.reduce(start, Intent.BeginReframe("i"))
        val rx = r.model.interaction as Interaction.Reframing
        assertEquals("i", rx.id)
        assertEquals(image(start, "i"), rx.before)
        assertEquals(start.nextToken, rx.token)
        assertEquals(start.nextToken + 1, r.model.nextToken)
        assertEquals(setOf("i"), r.model.selection)
        assertTrue(r.effects.none { it is Effect.Autosave }, "opening a session is not a mutation")
    }

    @Test
    fun `BeginReframe on a missing or non-image id is a no-op`() {
        val start = model(img("i"), TextElement("t", Transform(0.0, 0.0, 10.0, 10.0), 1, "hi"))
        assertEquals(Interaction.Idle, EditorReducer.reduce(start, Intent.BeginReframe("nope")).model.interaction)
        assertEquals(Interaction.Idle, EditorReducer.reduce(start, Intent.BeginReframe("t")).model.interaction)
    }

    // — double-tap retarget by type (ADR-053 §4), tested both ways —

    @Test
    fun `DoubleTapAt opens Reframing on an image hit, EditingText on a text hit, and no-op on empty space`() {
        val imageModel = model(img("i")) // 10x10 image at origin
        assertTrue(
            EditorReducer.reduce(imageModel, Intent.DoubleTapAt(PtPoint(5.0, 5.0))).model.interaction is Interaction.Reframing,
        )
        val textModel = model(TextElement("t", Transform(0.0, 0.0, 10.0, 10.0), 0, "hi"))
        assertTrue(
            EditorReducer.reduce(textModel, Intent.DoubleTapAt(PtPoint(5.0, 5.0))).model.interaction is Interaction.EditingText,
        )
        assertEquals(Interaction.Idle, EditorReducer.reduce(imageModel, Intent.DoubleTapAt(PtPoint(500.0, 500.0))).model.interaction)
    }

    // — commit —

    @Test
    fun `a full session commits exactly one EditImageCommand and one undo restores`() {
        val start = model(img("i", crop = Crop.FULL, fit = Fit.FILL))
        val (begun, token) = begin(start, "i")
        val draft = image(start, "i").copy(crop = Crop(0.1, 0.1, 0.7, 0.7), fit = Fit.FIT)
        val r = EditorReducer.reduce(begun, Intent.CommitReframe("i", draft, token))
        assertEquals(Crop(0.1, 0.1, 0.7, 0.7), image(r.model, "i").crop)
        assertEquals(Fit.FIT, image(r.model, "i").fit)
        assertEquals(Interaction.Idle, r.model.interaction)
        assertEquals(1, r.model.history.undo.size)
        assertTrue(r.model.history.undo.single() is EditImageCommand)
        assertTrue(r.effects.any { it is Effect.Autosave })
        assertEquals(start.document, EditorReducer.reduce(r.model, Intent.Undo).model.document)
    }

    @Test
    fun `a stale token is rejected — the document and session are untouched`() {
        val start = model(img("i"))
        val (begun, token) = begin(start, "i")
        val draft = image(start, "i").copy(crop = Crop(0.2, 0.2, 0.8, 0.8))
        val r = EditorReducer.reduce(begun, Intent.CommitReframe("i", draft, token + 1))
        assertEquals(Crop.FULL, image(r.model, "i").crop)
        assertTrue(r.model.interaction is Interaction.Reframing, "session stays open for the real commit")
        assertTrue(r.effects.none { it is Effect.Autosave })
    }

    @Test
    fun `a no-op commit (draft framing equals before) closes the session without a command`() {
        val start = model(img("i", crop = Crop(0.1, 0.1, 0.9, 0.9), fit = Fit.FIT))
        val (begun, token) = begin(start, "i")
        val r = EditorReducer.reduce(begun, Intent.CommitReframe("i", image(start, "i"), token))
        assertEquals(Interaction.Idle, r.model.interaction)
        assertEquals(begun.document, r.model.document)
        assertTrue(r.model.history.undo.isEmpty())
        assertTrue(r.effects.none { it is Effect.Autosave }, "no change ⇒ no autosave / no undo entry")
    }

    @Test
    fun `commit takes only crop and fit — a malformed draft cannot swap the photo or move the element`() {
        val start = model(img("i", assetId = "sha-i", z = 3))
        val (begun, token) = begin(start, "i")
        val malformed = image(start, "i").copy(
            assetId = "EVIL", transform = Transform(999.0, 999.0, 1.0, 1.0), zIndex = 42,
            crop = Crop(0.2, 0.2, 0.8, 0.8), fit = Fit.FIT,
        )
        val out = image(EditorReducer.reduce(begun, Intent.CommitReframe("i", malformed, token)).model, "i")
        assertEquals("sha-i", out.assetId)                                 // photo not swapped
        assertEquals(Transform(0.0, 0.0, 10.0, 10.0), out.transform)       // element not moved
        assertEquals(3, out.zIndex)                                        // z preserved
        assertEquals(Crop(0.2, 0.2, 0.8, 0.8), out.crop)                   // framing applied
        assertEquals(Fit.FIT, out.fit)
    }

    @Test
    fun `commit clamps an out-of-range draft crop so the stored crop is always renderable`() {
        val start = model(img("i"))
        val (begun, token) = begin(start, "i")
        val overshoot = image(start, "i").copy(crop = Crop(-0.5, -0.5, 1.5, 1.5))
        val out = image(EditorReducer.reduce(begun, Intent.CommitReframe("i", overshoot, token)).model, "i")
        assertEquals(FramingMath.clampCrop(Crop(-0.5, -0.5, 1.5, 1.5)), out.crop)
        assertEquals(Crop.FULL, out.crop) // fully out-of-range ⇒ full crop
    }

    @Test
    fun `committing after the element was deleted mid-session just closes, pushing no command`() {
        val start = model(img("i"))
        val (begun, token) = begin(start, "i")
        val deleted = EditorReducer.reduce(begun, Intent.Delete(setOf("i"))).model
        val r = EditorReducer.reduce(deleted, Intent.CommitReframe("i", img("i").copy(crop = Crop(0.2, 0.2, 0.8, 0.8)), token))
        assertEquals(Interaction.Idle, r.model.interaction)
        assertTrue(els(r.model).none { it.id == "i" }, "the deleted element is not resurrected")
        assertTrue(r.effects.none { it is Effect.Autosave }, "no spurious autosave / history entry")
        // The only undo step is the Delete itself — no phantom EditImageCommand on top.
        assertTrue(r.model.history.undo.single() is DeleteCommand)
    }

    // — cancel —

    @Test
    fun `CancelReframe discards the preview, keeping the element and emitting no autosave`() {
        val start = model(img("i", crop = Crop(0.1, 0.1, 0.9, 0.9)))
        val (begun, token) = begin(start, "i")
        val r = EditorReducer.reduce(begun, Intent.CancelReframe(token))
        assertEquals(Interaction.Idle, r.model.interaction)
        assertEquals(begun.document, r.model.document)
        assertTrue(r.effects.none { it is Effect.Autosave })
    }

    @Test
    fun `CancelReframe with a stale token or no session is a no-op that leaves a live session open`() {
        val start = model(img("i"))
        assertEquals(start, EditorReducer.reduce(start, Intent.CancelReframe(99L)).model)
        val (begun, token) = begin(start, "i")
        assertTrue(EditorReducer.reduce(begun, Intent.CancelReframe(token + 1)).model.interaction is Interaction.Reframing)
    }

    // — replace (preserves framing, ADR-053 §6) —

    @Test
    fun `ReplaceImage swaps only the bytes, preserving framing, one undo restores`() {
        val start = model(img("i", assetId = "old", crop = Crop(0.2, 0.2, 0.8, 0.8), fit = Fit.FIT, z = 2))
        val r = EditorReducer.reduce(start, Intent.ReplaceImage("i", "new"))
        val out = image(r.model, "i")
        assertEquals("new", out.assetId)
        assertEquals(Crop(0.2, 0.2, 0.8, 0.8), out.crop) // framing preserved
        assertEquals(Fit.FIT, out.fit)
        assertEquals(2, out.zIndex)
        assertTrue(r.effects.any { it is Effect.Autosave })
        assertEquals(start.document, EditorReducer.reduce(r.model, Intent.Undo).model.document)
    }

    @Test
    fun `ReplaceImage with the same assetId or a missing id is a no-op`() {
        val start = model(img("i", assetId = "same"))
        assertEquals(start.document, EditorReducer.reduce(start, Intent.ReplaceImage("i", "same")).model.document)
        assertTrue(EditorReducer.reduce(start, Intent.ReplaceImage("i", "same")).effects.none { it is Effect.Autosave })
        assertEquals(start.document, EditorReducer.reduce(start, Intent.ReplaceImage("nope", "x")).model.document)
    }

    // — reset (ADR-053 §6) —

    @Test
    fun `ResetFraming returns the photo to the Fit-FILL full-crop default, one undo restores`() {
        val start = model(img("i", crop = Crop(0.2, 0.2, 0.8, 0.8), fit = Fit.FIT))
        val r = EditorReducer.reduce(start, Intent.ResetFraming("i"))
        val out = image(r.model, "i")
        assertEquals(Crop.FULL, out.crop)
        assertEquals(Fit.FILL, out.fit)
        assertTrue(r.effects.any { it is Effect.Autosave })
        assertEquals(start.document, EditorReducer.reduce(r.model, Intent.Undo).model.document)
    }

    @Test
    fun `ResetFraming on a photo already at the default, or a missing id, is a no-op`() {
        val start = model(img("i", crop = Crop.FULL, fit = Fit.FILL))
        assertEquals(start.document, EditorReducer.reduce(start, Intent.ResetFraming("i")).model.document)
        assertTrue(EditorReducer.reduce(start, Intent.ResetFraming("i")).effects.none { it is Effect.Autosave })
        assertEquals(start.document, EditorReducer.reduce(start, Intent.ResetFraming("nope")).model.document)
    }

    // — placement default + migration (ADR-053 §2/§3) —

    @Test
    fun `CommitAddImage places a new photo at the Fit-FILL default regardless of the supplied fit`() {
        val start = model()
        // Feature supplies an element with the model default (FIT); the reducer must force FILL at placement.
        val supplied = ImageElement(id = "ignored", transform = Transform(0.0, 0.0, 10.0, 10.0), assetId = "sha", fit = Fit.FIT)
        val placed = image(EditorReducer.reduce(start, Intent.CommitAddImage(supplied)).model, "el-${start.nextToken}")
        assertEquals(Fit.FILL, placed.fit)
        assertEquals(Crop.FULL, placed.crop) // full crop
    }

    @Test
    fun `an existing Fit-FIT photo is never rewritten by placing a new photo (migration = new placements only)`() {
        val legacy = img("legacy", fit = Fit.FIT, crop = Crop(0.0, 0.1, 1.0, 0.9))
        val start = model(legacy)
        val supplied = ImageElement(id = "x", transform = Transform(5.0, 5.0, 10.0, 10.0), assetId = "sha2")
        val after = EditorReducer.reduce(start, Intent.CommitAddImage(supplied)).model
        // The pre-existing document element is byte-identical; only the NEW element got the FILL default.
        assertEquals(legacy, image(after, "legacy"))
        assertEquals(Fit.FIT, image(after, "legacy").fit)
        assertTrue(els(after).any { it.id != "legacy" && (it as ImageElement).fit == Fit.FILL })
    }

    @Test
    fun `EditImageCommand is invertible for reframe, replace, and reset`() {
        val before = img("i", assetId = "a", crop = Crop(0.1, 0.1, 0.9, 0.9), fit = Fit.FILL)
        val doc = model(before).document
        val cases = listOf(
            before.copy(crop = Crop(0.2, 0.3, 0.7, 0.6), fit = Fit.FIT), // reframe
            before.copy(assetId = "b"),                                  // replace
            before.copy(crop = Crop.FULL, fit = Fit.FILL),               // reset
        )
        for (after in cases) {
            val cmd = EditImageCommand(0, "i", before, after)
            assertEquals(after, cmd.applyTo(doc).pages[0].elements.single())
            assertEquals(doc, cmd.invertOn(cmd.applyTo(doc)), "invert(apply(doc)) != doc for after=$after")
        }
    }

    @Test
    fun `leaving the page ends an open Reframe session`() {
        val start = model(img("i")).copy(
            document = ZineDocument(
                format = ZineFormat.SINGLE_SHEET_8,
                paperSize = PaperSize.LETTER,
                pages = listOf(
                    Page(0, PageRole.INTERIOR, elements = listOf(img("i"))),
                    Page(1, PageRole.INTERIOR),
                ),
            ),
        )
        val (begun, _) = begin(start, "i")
        assertTrue(begun.interaction is Interaction.Reframing)
        val moved = EditorReducer.reduce(begun, Intent.GoToPage(1)).model
        assertEquals(Interaction.Idle, moved.interaction)
    }
}
