package com.aritr.zinely.editor

import com.aritr.zinely.core.data.serialization.JsonDocumentSerializer
import com.aritr.zinely.core.data.validation.DefaultDocumentValidator
import com.aritr.zinely.core.editor.HitTest
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.core.render.DrawShape
import com.aritr.zinely.core.render.SceneRenderer
import com.aritr.zinely.core.render.SupplyCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What a build does with a piece it has never heard of (Brief 05, *Data model implications*).
 *
 * A zine made with a newer build's supplies reaches an older build through a library-backup restore or a
 * downgrade. The two hand-cut frames (`v21-bench.html` A32) are the first ids added since supplies
 * shipped, so every build before them is that older build. The claim, end to end across the four modules
 * it crosses: the zine **loads**, the piece **draws nothing**, its id is **kept on save**, and it is
 * **still selectable** by its box.
 *
 * The id here is one no build knows, standing in for a frame on a build that predates it. It lives in
 * `:app` because this is the only test source set that sees the serializer, the validator, the renderer
 * and the hit test at once.
 */
class UnknownSupplyCompatibilityTest {

    private val unknownId = "future.ribbon"

    private val piece = DecorElement(
        id = "decor-1",
        transform = Transform(xPt = 50.0, yPt = 50.0, widthPt = 100.0, heightPt = 100.0),
        zIndex = 1,
        supplyId = unknownId,
        ink = ColorRgba(200, 40, 90),
        mirrored = true,
    )

    private val photo = ImageElement(
        id = "photo-1",
        transform = Transform(xPt = 0.0, yPt = 0.0, widthPt = 200.0, heightPt = 200.0),
        zIndex = 0,
        assetId = "a".repeat(64),
    )

    private fun document(): ZineDocument = ZineDocument(
        format = ZineFormat.SINGLE_SHEET_8,
        paperSize = PaperSize.LETTER,
        pages = (0 until 8).map { i ->
            val role = when (i) {
                0 -> PageRole.FRONT_COVER
                7 -> PageRole.BACK_COVER
                else -> PageRole.INTERIOR
            }
            Page(index = i, role = role, elements = if (i == 0) listOf(photo, piece) else emptyList())
        },
    )

    @Test
    fun `a zine with a piece this build does not know loads, draws nothing for it, keeps it and can select it`() {
        val serializer = JsonDocumentSerializer()
        assertNull("the premise: this build has no outline for the id", SupplyCatalog.outlineOf(unknownId))

        // Loads: it decodes, and the validator does not refuse it.
        val saved = serializer.serialize(document())
        val loaded = serializer.deserialize(saved)
        assertTrue(DefaultDocumentValidator().validate(loaded).isValid)
        val page = loaded.pages[0]
        assertEquals(piece, page.elements.single { it.id == "decor-1" })

        // Draws nothing: the photo is on the tape and no shape is.
        val tape = SceneRenderer.render(page, PtSize(200.0, 200.0), DocumentDefaults())
        assertEquals(1, tape.size)
        assertTrue("an unknown piece must not draw", tape.none { it is DrawShape })

        // Keeps it: a second save writes the id, and every other field of the piece, back unchanged.
        val resaved = serializer.serialize(loaded)
        assertEquals(saved, resaved)
        assertTrue(resaved.contains("\"supplyId\":\"$unknownId\""))

        // Still selectable, by its box, which is why it takes the tap from the photo underneath.
        assertEquals("decor-1", HitTest.topmostAt(page, PtPoint(100.0, 100.0)))
        assertEquals("photo-1", HitTest.topmostAt(page, PtPoint(20.0, 20.0)))
    }

    @Test
    fun `the two frames are known to this build, so the case above is about older builds`() {
        for (id in listOf("paper.border", "paper.round")) {
            val frame = piece.copy(supplyId = id)
            val page = Page(index = 0, role = PageRole.INTERIOR, elements = listOf(photo, frame))
            val tape = SceneRenderer.render(page, PtSize(200.0, 200.0), DocumentDefaults())
            assertEquals("$id draws one shape over the photo", 1, tape.count { it is DrawShape })
            // And the hole is not the box: the same centre tap now reaches the photo.
            assertEquals("photo-1", HitTest.topmostAt(page, PtPoint(100.0, 100.0)))
        }
    }
}
