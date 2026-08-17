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
 * An **unauthored** `supplyId` in the scene renderer — asserting the absence of a command, on purpose.
 *
 * This file was `DecorEmitsNothingTest`, which pinned P1/P2's decision that *every* supply drew
 * nothing. P3 armed the renderer, and these two tests kept passing — because the fixture happens to use
 * `tape.torn`, one of the **twelve outlines still owed to a designer**. That is a narrower and more
 * durable claim than the one the file's name and KDoc were making, so it is renamed to the claim it
 * actually tests rather than deleted with the era that wrote it.
 *
 * The narrower claim is load-bearing on its own: §2.2 rules that catalogue membership is checked at the
 * render boundary and **not** in the document validator, precisely so an unknown, misspelled or
 * newer-schema `supplyId` draws nothing instead of making a zine refuse to open.
 *
 * The second test is the one that matters more, and it now runs with an **authored** supply: a supply
 * that skipped, or one that landed, must not disturb its neighbours' order on the tape. The tape is
 * what all four surfaces (canvas, proof, PDF, PNG) share, so a shifted neighbour is wrong four times.
 */
class DecorUnauthoredSupplyTest {

    private val pageSize = PtSize(612.0, 792.0)

    private fun decor(id: String, z: Int, supplyId: String = "tape.torn") = DecorElement(
        id = id,
        transform = Transform(10.0, 10.0, 100.0, 40.0),
        zIndex = z,
        supplyId = supplyId,
        ink = ColorRgba(200, 40, 90),
    )

    private fun text(id: String, z: Int) =
        TextElement(id = id, transform = Transform(0.0, 0.0, 50.0, 12.0), zIndex = z, text = id)

    private fun page(vararg els: com.aritr.zinely.core.model.Element) =
        Page(index = 0, role = PageRole.FRONT_COVER, elements = els.toList())

    @Test
    fun `given a page of only unauthored supplies, when the scene is built, then the tape is empty`() {
        val scene = SceneRenderer.buildScene(page(decor("d1", 0), decor("d2", 1)), pageSize, DocumentDefaults())
        assertTrue(scene.commands.isEmpty(), "an unauthored supply draws nothing; got ${scene.commands}")
    }

    @Test
    fun `given supplies interleaved with text, when the scene is built, then z-order is preserved`() {
        val scene = SceneRenderer.buildScene(
            page(
                text("t-back", 0),
                decor("d-unauthored", 1),
                decor("d-authored", 2, supplyId = "shape.rect"),
                text("t-front", 3),
            ),
            pageSize,
            DocumentDefaults(),
        )

        // The unauthored one leaves no gap and no placeholder; the authored one sits at its own z.
        assertEquals(3, scene.commands.size, "no placeholder command may be emitted for a missing outline")
        assertEquals(listOf("t-back"), scene.commands.take(1).filterIsInstance<DrawTextBox>().map { it.text })
        assertTrue(scene.commands[1] is DrawShape, "the authored supply belongs between the two texts")
        assertEquals("t-front", (scene.commands[2] as DrawTextBox).text)
    }
}
