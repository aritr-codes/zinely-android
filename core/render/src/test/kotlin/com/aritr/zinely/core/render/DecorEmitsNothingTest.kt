package com.aritr.zinely.core.render

import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * `DecorElement` in the scene renderer — asserting the **absence** of a command, on purpose.
 *
 * Package P1 is the model seam; the command that draws a supply (`DrawShape`) needs `SupplyOutline`,
 * `SupplyCatalog` and the unit-square fold (SUPPLIES-SPEC §3.3/§3.4.1), which are package P2. Until
 * then a supply draws nothing, and that is a *decision* — so it is pinned here rather than left to be
 * discovered as a bug. **When P2 lands these assertions must be inverted, not deleted.**
 *
 * The second test is the one that matters more: emitting nothing must not disturb the tape around it.
 * The tape is what all four surfaces (canvas, proof, PDF, PNG) share, so a skipped element that also
 * shifted its neighbours would be wrong in four places at once.
 */
class DecorEmitsNothingTest {

    private val pageSize = PtSize(612.0, 792.0)

    private fun decor(id: String, z: Int) = DecorElement(
        id = id,
        transform = Transform(10.0, 10.0, 100.0, 40.0),
        zIndex = z,
        supplyId = "tape.torn",
        ink = ColorRgba(200, 40, 90),
    )

    private fun text(id: String, z: Int) =
        TextElement(id = id, transform = Transform(0.0, 0.0, 50.0, 12.0), zIndex = z, text = id)

    private fun page(vararg els: com.aritr.zinely.core.model.Element) =
        Page(index = 0, role = PageRole.FRONT_COVER, elements = els.toList())

    @Test
    fun `given a page of only supplies, when the scene is built, then the tape is empty`() {
        val scene = SceneRenderer.buildScene(page(decor("d1", 0), decor("d2", 1)), pageSize, DocumentDefaults())
        assertTrue(scene.commands.isEmpty(), "P1 draws decor as nothing; got ${scene.commands}")
    }

    @Test
    fun `given supplies interleaved with text, when the scene is built, then only the text is emitted, in order`() {
        val scene = SceneRenderer.buildScene(
            page(text("t-back", 0), decor("d-mid", 1), text("t-front", 2)),
            pageSize,
            DocumentDefaults(),
        )
        val texts = scene.commands.filterIsInstance<DrawTextBox>().map { it.text }
        assertEquals(listOf("t-back", "t-front"), texts)
        assertEquals(2, scene.commands.size, "no placeholder command may be emitted for decor")
    }
}
