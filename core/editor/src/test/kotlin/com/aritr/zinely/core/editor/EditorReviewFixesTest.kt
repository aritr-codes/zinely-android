package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Fixes from the PR #20 max-effort review: reducer robustness (stale sessions, non-invertible commits,
 * id minting), DeletePage index/selection correctness, degenerate-size clamping, and Snap input guards.
 */
class EditorReviewFixesTest {

    private fun txt(id: String, x: Double = 0.0, z: Int = 0, text: String = "x") =
        TextElement(id = id, transform = Transform(x, 0.0, 10.0, 10.0), zIndex = z, text = text)

    private fun pageOf(idx: Int, vararg els: TextElement) =
        Page(index = idx, role = if (idx == 0) PageRole.FRONT_COVER else PageRole.INTERIOR, elements = els.toList())

    private fun model(pages: List<Page>, current: Int = 0, selection: Set<String> = emptySet()) = EditorModel(
        document = ZineDocument(format = ZineFormat.SINGLE_SHEET_8, paperSize = PaperSize.LETTER, pages = pages),
        currentPageIndex = current,
        selection = selection,
    )

    private fun els(m: EditorModel) = m.document.pages[m.currentPageIndex].elements

    @Test
    fun `DeletePage before current shifts currentPageIndex down by one`() {
        val m = model(listOf(pageOf(0), pageOf(1), pageOf(2, txt("c")), pageOf(3)), current = 2)
        val r = EditorReducer.reduce(m, Intent.DeletePage(0))
        // page formerly at 2 (holding c) is now at index 1; current must follow it.
        assertEquals(1, r.model.currentPageIndex)
        assertEquals(listOf("c"), r.model.document.pages[r.model.currentPageIndex].elements.map { it.id })
    }

    @Test
    fun `DeletePage clears selection and ends any transform session`() {
        val m = model(listOf(pageOf(0, txt("a")), pageOf(1)), current = 0, selection = setOf("a"))
            .let { EditorReducer.reduce(it, Intent.BeginTransform(setOf("a"))).model }
        assertTrue(m.interaction is Interaction.Transforming)
        val r = EditorReducer.reduce(m, Intent.DeletePage(1))
        assertTrue(r.model.selection.isEmpty())
        assertTrue(r.model.interaction is Interaction.Idle)
    }

    @Test
    fun `GoToPage clears selection and ends any transform session`() {
        val m = model(listOf(pageOf(0, txt("a")), pageOf(1)), current = 0, selection = setOf("a"))
            .let { EditorReducer.reduce(it, Intent.BeginTransform(setOf("a"))).model }
        val r = EditorReducer.reduce(m, Intent.GoToPage(1))
        assertTrue(r.model.selection.isEmpty())
        assertTrue(r.model.interaction is Interaction.Idle)
    }

    @Test
    fun `CommitTransform ignores ids absent from the begin snapshot so undo fully restores`() {
        val start = model(listOf(pageOf(0, txt("a"), txt("b", x = 50.0))), selection = setOf("a"))
        val begun = EditorReducer.reduce(start, Intent.BeginTransform(setOf("a"))).model
        val token = (begun.interaction as Interaction.Transforming).token
        val foreign = mapOf("a" to Transform(99.0, 9.0, 20.0, 20.0), "b" to Transform(0.0, 0.0, 1.0, 1.0))
        val r = EditorReducer.reduce(begun, Intent.CommitTransform(foreign, token))
        // b must be untouched (it was never in the session).
        val b = els(r.model).first { it.id == "b" } as TextElement
        assertEquals(50.0, b.transform.xPt, 1e-9)
        // undo restores a exactly.
        val undone = EditorReducer.reduce(r.model, Intent.Undo).model
        assertEquals(start.document, undone.document)
    }

    @Test
    fun `CommitText normalizes the committed element id to the target id`() {
        val start = model(listOf(pageOf(0, txt("a", text = "old"))), selection = setOf("a"))
        val r = EditorReducer.reduce(start, Intent.CommitText("a", TextElement("WRONG", txt("a").transform, 0, "new")))
        val out = els(r.model).single() as TextElement
        assertEquals("a", out.id)
        assertEquals("new", out.text)
    }

    @Test
    fun `CommitAddImage mints a reducer id and advances nextToken`() {
        val start = model(listOf(pageOf(0, txt("a"))))
        val img = ImageElement(id = "ignored", transform = Transform(0.0, 0.0, 10.0, 10.0), assetId = "sha")
        val r = EditorReducer.reduce(start, Intent.CommitAddImage(img))
        val placed = els(r.model).last()
        assertEquals("el-${start.nextToken}", placed.id)
        assertEquals(start.nextToken + 1, r.model.nextToken)
        assertEquals(setOf(placed.id), r.model.selection)
    }

    @Test
    fun `ScaleBy to zero clamps to a positive minimum size so the element stays hittable`() {
        val start = model(listOf(pageOf(0, txt("a"))), selection = setOf("a"))
        val r = EditorReducer.reduce(start, Intent.ScaleBy(0.0))
        val t = (els(r.model).single() as TextElement).transform
        assertTrue(t.widthPt > 0.0 && t.heightPt > 0.0, "size must stay positive")
        // and it can still be hit at its centre
        val page = r.model.document.pages[0]
        assertNotNull(HitTest.topmostAt(page, PtPoint(t.xPt + t.widthPt / 2, t.yPt + t.heightPt / 2)))
    }

    @Test
    fun `bakeHandleResize clamps a collapsed drag to a positive minimum size`() {
        val base = Transform(0.0, 0.0, 100.0, 100.0)
        // drag bottom-right onto the top-left anchor (0,0) → would be zero area.
        val out = TransformMath.bakeHandleResize(base, PtPoint(-1.0, -1.0), PtPoint(1.0, 1.0), PtPoint(0.0, 0.0))
        assertTrue(out.widthPt > 0.0 && out.heightPt > 0.0)
    }

    @Test
    fun `Snap with a non-finite threshold is a safe no-op`() {
        val moving = PtRect(3.0, 300.0, 50.0, 20.0)
        val r = Snap.snap(moving, emptyList(), PtSize(600.0, 800.0), thresholdPt = Double.POSITIVE_INFINITY)
        assertEquals(moving, r.adjusted)
        assertTrue(r.guides.isEmpty())
    }

    @Test
    fun `Undo of DeletePage restores the prior selection`() {
        val m = model(listOf(pageOf(0, txt("a")), pageOf(1, txt("b"))), current = 1, selection = setOf("b"))
        val deleted = EditorReducer.reduce(m, Intent.DeletePage(1)).model
        assertFalse("b" in deleted.selection)
        val undone = EditorReducer.reduce(deleted, Intent.Undo).model
        assertEquals(setOf("b"), undone.selection)
    }
}
