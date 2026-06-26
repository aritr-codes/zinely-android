package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Live snap (ADR-029 §5.4): bake the gesture (§5.2) then snap the single-select, non-rotated box to the
 * page/other-element candidate lines. Same inputs as the gesture commit ⇒ preview == commit. F1 skips
 * rotated; multi-select bakes without snapping. Pure.
 */
class LiveSnapTest {

    private val pageSize = PtSize(600.0, 800.0)
    private val tol = 1e-9

    private fun img(id: String, t: Transform) = ImageElement(id = id, transform = t, zIndex = 0, assetId = "a")

    private fun page(vararg els: ImageElement) =
        Page(index = 0, role = PageRole.INTERIOR, elements = els.toList())

    /** A pan that lands the box 3pt from the page's left edge — within the 8px@1× threshold. */
    @Test
    fun `pan within threshold snaps the baked box to the page edge and emits a guide`() {
        val before = Transform(xPt = 100.0, yPt = 300.0, widthPt = 50.0, heightPt = 20.0)
        val sel = setOf("e")
        // panXpx = -97 at 1px/pt ⇒ baked x = 3, snaps to 0.
        val live = LiveTransform(panXpx = -97.0)
        val r = LiveSnap.resolve(
            page = page(img("e", before)),
            selection = sel,
            before = mapOf("e" to before),
            live = live,
            screenPxPerPt = 1.0,
            pageSize = pageSize,
            thresholdPt = LiveSnap.thresholdPt(1.0),
        )
        assertEquals(0.0, r.transforms.getValue("e").xPt, tol)
        assertTrue(r.guides.any { it.axis == SnapAxis.VERTICAL && it.positionPt == 0.0 })
    }

    @Test
    fun `bake equals commit — resolve transforms match the raw bake plus snap`() {
        val before = Transform(xPt = 100.0, yPt = 300.0, widthPt = 50.0, heightPt = 20.0)
        val live = LiveTransform(panXpx = -97.0)
        val r = LiveSnap.resolve(
            page = page(img("e", before)),
            selection = setOf("e"),
            before = mapOf("e" to before),
            live = live,
            screenPxPerPt = 1.0,
            pageSize = pageSize,
            thresholdPt = LiveSnap.thresholdPt(1.0),
        )
        // The committed transform IS r.transforms — the gesture layer reuses this exact map.
        val baked = live.bake(before, 1.0)
        assertEquals(baked.copy(xPt = 0.0), r.transforms.getValue("e"))
    }

    @Test
    fun `F1 — a rotated element is baked but never snapped, no guides`() {
        val before = Transform(xPt = 100.0, yPt = 300.0, widthPt = 50.0, heightPt = 20.0, rotationDegrees = 30.0)
        val live = LiveTransform(panXpx = -97.0)
        val r = LiveSnap.resolve(
            page = page(img("e", before)),
            selection = setOf("e"),
            before = mapOf("e" to before),
            live = live,
            screenPxPerPt = 1.0,
            pageSize = pageSize,
            thresholdPt = LiveSnap.thresholdPt(1.0),
        )
        assertEquals(live.bake(before, 1.0), r.transforms.getValue("e"))
        assertTrue(r.guides.isEmpty())
    }

    @Test
    fun `multi-select bakes every member without snapping`() {
        val a = Transform(xPt = 3.0, yPt = 300.0, widthPt = 50.0, heightPt = 20.0)
        val b = Transform(xPt = 200.0, yPt = 400.0, widthPt = 40.0, heightPt = 40.0)
        val live = LiveTransform()
        val r = LiveSnap.resolve(
            page = page(img("a", a), img("b", b)),
            selection = setOf("a", "b"),
            before = mapOf("a" to a, "b" to b),
            live = live,
            screenPxPerPt = 1.0,
            pageSize = pageSize,
            thresholdPt = LiveSnap.thresholdPt(1.0),
        )
        // a sits 3pt off the left edge but multi-select never snaps.
        assertEquals(3.0, r.transforms.getValue("a").xPt, tol)
        assertTrue(r.guides.isEmpty())
    }

    @Test
    fun `a non-positive scale degrades to no movement and no snap`() {
        val before = Transform(xPt = 3.0, yPt = 300.0, widthPt = 50.0, heightPt = 20.0)
        val r = LiveSnap.resolve(
            page = page(img("e", before)),
            selection = setOf("e"),
            before = mapOf("e" to before),
            live = LiveTransform(panXpx = 40.0),
            screenPxPerPt = 0.0,
            pageSize = pageSize,
            thresholdPt = LiveSnap.thresholdPt(0.0),
        )
        // No NaN/snap poisons the commit; the box stays exactly where it was.
        assertEquals(before, r.transforms.getValue("e"))
        assertTrue(r.guides.isEmpty())
    }

    @Test
    fun `a rotated neighbour is not a snap candidate`() {
        // moving's right edge (153) sits 3pt from the rotated neighbour's left edge (150) — but rotated
        // neighbours are excluded, and no page line is within range, so nothing snaps.
        val moving = Transform(xPt = 103.0, yPt = 500.0, widthPt = 50.0, heightPt = 20.0)
        val rotated = Transform(xPt = 150.0, yPt = 0.0, widthPt = 40.0, heightPt = 40.0, rotationDegrees = 20.0)
        val r = LiveSnap.resolve(
            page = page(img("m", moving), img("o", rotated)),
            selection = setOf("m"),
            before = mapOf("m" to moving),
            live = LiveTransform(),
            screenPxPerPt = 1.0,
            pageSize = pageSize,
            thresholdPt = LiveSnap.thresholdPt(1.0),
        )
        assertEquals(103.0, r.transforms.getValue("m").xPt, tol)
        assertTrue(r.guides.isEmpty())
    }

    @Test
    fun `candidate lines come from other elements, not the moving one`() {
        val moving = Transform(xPt = 122.0, yPt = 500.0, widthPt = 50.0, heightPt = 20.0) // centerX 147
        val other = Transform(xPt = 100.0, yPt = 0.0, widthPt = 100.0, heightPt = 10.0) // centerX 150
        val live = LiveTransform()
        val r = LiveSnap.resolve(
            page = page(img("m", moving), img("o", other)),
            selection = setOf("m"),
            before = mapOf("m" to moving),
            live = live,
            screenPxPerPt = 1.0,
            pageSize = pageSize,
            thresholdPt = LiveSnap.thresholdPt(1.0),
        )
        val snapped = r.transforms.getValue("m")
        assertEquals(150.0, snapped.xPt + snapped.widthPt / 2.0, tol)
    }
}
