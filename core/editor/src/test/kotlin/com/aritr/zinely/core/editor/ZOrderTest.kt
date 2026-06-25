package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Z-order invariant (ADR-029 §3, Codex required-fix #4): on load the editor normalises a page to dense
 * unique `zIndex` `0..n-1` derived from the visual order `(zIndex, listIndex)`, so reorder is well-defined
 * even when a persisted document carried duplicate/sparse `zIndex`.
 */
class ZOrderTest {

    private fun el(id: String, z: Int) =
        TextElement(id = id, transform = Transform(0.0, 0.0, 10.0, 10.0), zIndex = z, text = "x")

    private fun page(vararg els: TextElement) = Page(index = 0, role = PageRole.INTERIOR, elements = els.toList())

    private fun zById(page: Page): Map<String, Int> = page.elements.associate { it.id to it.zIndex }

    @Test
    fun `normalize assigns dense unique zIndex from visual order, ties broken by list order`() {
        // [a=5, b=5, c=2]; visual order (z,idx) = c(2), a(5,0), b(5,1) → ranks c=0, a=1, b=2.
        val out = ZOrder.normalize(page(el("a", 5), el("b", 5), el("c", 2)))
        assertEquals(mapOf("a" to 1, "b" to 2, "c" to 0), zById(out))
    }

    @Test
    fun `normalize is idempotent on an already-dense page`() {
        val once = ZOrder.normalize(page(el("a", 5), el("b", 5), el("c", 2)))
        assertEquals(zById(once), zById(ZOrder.normalize(once)))
    }

    @Test
    fun `bringForward swaps with the next-higher neighbour`() {
        val p = ZOrder.normalize(page(el("a", 0), el("b", 1), el("c", 2))) // ranks a0 b1 c2
        val out = ZOrder.reorder(p, "a", ReorderOp.BRING_FORWARD)
        assertEquals(mapOf("a" to 1, "b" to 0, "c" to 2), zById(out))
    }

    @Test
    fun `toFront makes the element the highest`() {
        val p = ZOrder.normalize(page(el("a", 0), el("b", 1), el("c", 2)))
        val out = ZOrder.reorder(p, "a", ReorderOp.TO_FRONT)
        assertEquals(2, zById(out)["a"])
        assertEquals(setOf(0, 1, 2), out.elements.map { it.zIndex }.toSet()) // still a total order
    }

    @Test
    fun `bringForward on the topmost element is a no-op`() {
        val p = ZOrder.normalize(page(el("a", 0), el("b", 1), el("c", 2)))
        val out = ZOrder.reorder(p, "c", ReorderOp.BRING_FORWARD)
        assertEquals(zById(p), zById(out))
    }
}
