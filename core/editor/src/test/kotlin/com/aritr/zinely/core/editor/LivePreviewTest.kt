package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.TextStyle
import com.aritr.zinely.core.model.Transform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/**
 * Pure-JVM proof that [LivePreview.apply] bakes the live gesture into exactly the selected elements via
 * the same [LiveTransform.bake] the commit uses (preview == commit), leaving order and others untouched.
 */
class LivePreviewTest {

    private fun page(vararg elements: TextElement) = Page(index = 0, role = PageRole.INTERIOR, elements = elements.toList())

    private fun text(id: String, t: Transform, z: Int = 0) = TextElement(id = id, transform = t, zIndex = z, text = id)

    @Test
    fun bakes_only_selected_element_via_same_bake_as_commit() {
        val a = text("a", Transform(40.0, 40.0, 20.0, 20.0))
        val b = text("b", Transform(0.0, 0.0, 10.0, 10.0))
        val live = LiveTransform().accumulate(panXpx = 20.0, panYpx = 0.0, zoomFactor = 1.0, rotationDelta = 0.0)
        val s = 2.0

        val out = LivePreview.apply(page(a, b), mapOf("a" to a.transform), live, s)

        // "a" moved by 20px / 2 = 10pt right; "b" untouched; order preserved.
        assertEquals(live.bake(a.transform, s), out.elements[0].transform)
        assertEquals(50.0, out.elements[0].transform.xPt, 1e-9)
        assertSame(b, out.elements[1])
        assertEquals(listOf("a", "b"), out.elements.map { it.id })
    }

    @Test
    fun empty_before_returns_page_unchanged() {
        val p = page(text("a", Transform(1.0, 2.0, 3.0, 4.0)))
        assertSame(p, LivePreview.apply(p, emptyMap(), LiveTransform(), 2.0))
    }

    @Test
    fun preview_equals_commit_application() {
        // The commit (TransformCommand) applies after = live.bake(before) the same way; assert parity.
        val a = text("a", Transform(40.0, 40.0, 20.0, 20.0, rotationDegrees = 30.0))
        val live = LiveTransform().accumulate(panXpx = 8.0, panYpx = -4.0, zoomFactor = 1.5, rotationDelta = 15.0)
        val s = 2.5
        val before = mapOf("a" to a.transform)

        val previewTransform = LivePreview.apply(page(a), before, live, s).elements[0].transform
        val committedAfter = before.mapValues { (_, t) -> live.bake(t, s) }["a"]
        assertEquals(committedAfter, previewTransform)
    }

    // --- applyStyleOverride (ADR-055): the settling size burst's canvas half ---

    @Test
    fun style_override_replaces_only_the_listed_text_element() {
        val a = text("a", Transform(0.0, 0.0, 10.0, 10.0))
        val b = text("b", Transform(0.0, 0.0, 10.0, 10.0))
        val bigger = a.style.copy(sizePt = 48.0)

        val out = LivePreview.applyStyleOverride(page(a, b), mapOf("a" to bigger))

        assertEquals(bigger, (out.elements[0] as TextElement).style)
        assertSame(b, out.elements[1])
        assertEquals(listOf("a", "b"), out.elements.map { it.id })
        // The override is style-only: the box the selection chrome draws must not move.
        assertEquals(a.transform, out.elements[0].transform)
    }

    @Test
    fun style_override_leaves_non_text_elements_alone() {
        // An id collision between an image and an override must be a no-op, not a crash: the Type bar
        // only ever targets a text box, but the projection must not assume it.
        val image = ImageElement(id = "a", transform = Transform(0.0, 0.0, 10.0, 10.0), assetId = "sha")
        val p = Page(index = 0, role = PageRole.INTERIOR, elements = listOf(image))

        val out = LivePreview.applyStyleOverride(p, mapOf("a" to TextStyle(sizePt = 48.0)))

        assertSame(image, out.elements[0])
    }

    @Test
    fun empty_style_override_returns_page_unchanged() {
        val p = page(text("a", Transform(1.0, 2.0, 3.0, 4.0)))
        assertSame(p, LivePreview.applyStyleOverride(p, emptyMap()))
    }
}
