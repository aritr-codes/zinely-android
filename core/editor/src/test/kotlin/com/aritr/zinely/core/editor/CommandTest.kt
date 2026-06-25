package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.Element
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Undo commands (ADR-029 §4): each [Command] carries a field-level memento and round-trips
 * `invertOn(applyTo(doc)) == doc`. Pure.
 */
class CommandTest {

    private fun txt(id: String, x: Double = 0.0, z: Int = 0, text: String = "x") =
        TextElement(id = id, transform = Transform(x, 0.0, 10.0, 10.0), zIndex = z, text = text)

    private fun doc(vararg els: TextElement) = ZineDocument(
        format = ZineFormat.SINGLE_SHEET_8,
        paperSize = PaperSize.LETTER,
        pages = listOf(Page(index = 0, role = PageRole.FRONT_COVER, elements = els.toList())),
    )

    private fun elements(d: ZineDocument): List<Element> = d.pages[0].elements
    private fun ids(d: ZineDocument) = elements(d).map { it.id }

    private fun assertRoundTrip(base: ZineDocument, cmd: Command) {
        val applied = cmd.applyTo(base)
        assertEquals(base, cmd.invertOn(applied), "invertOn(applyTo(doc)) must equal doc")
    }

    @Test
    fun `TransformCommand applies after and inverts to before`() {
        val base = doc(txt("a"))
        val after = Transform(99.0, 5.0, 20.0, 20.0, 45.0)
        val cmd = TransformCommand(
            pageIndex = 0,
            before = mapOf("a" to txt("a").transform),
            after = mapOf("a" to after),
        )
        assertEquals(after, (elements(cmd.applyTo(base)).single() as TextElement).transform)
        assertRoundTrip(base, cmd)
    }

    @Test
    fun `PlaceCommand appends and inverts by removing the id`() {
        val base = doc(txt("a"))
        val cmd = PlaceCommand(pageIndex = 0, element = txt("b", z = 1))
        assertEquals(listOf("a", "b"), ids(cmd.applyTo(base)))
        assertRoundTrip(base, cmd)
    }

    @Test
    fun `DeleteCommand removes by id and re-inserts at original index on invert`() {
        val base = doc(txt("a"), txt("b"), txt("c"))
        val cmd = DeleteCommand(pageIndex = 0, removed = listOf(1 to txt("b")))
        assertEquals(listOf("a", "c"), ids(cmd.applyTo(base)))
        assertRoundTrip(base, cmd)
    }

    @Test
    fun `ReorderCommand sets afterZ and inverts to beforeZ`() {
        val base = doc(txt("a", z = 0), txt("b", z = 1))
        val cmd = ReorderCommand(pageIndex = 0, beforeZ = mapOf("a" to 0, "b" to 1), afterZ = mapOf("a" to 1, "b" to 0))
        val applied = cmd.applyTo(base)
        assertEquals(mapOf("a" to 1, "b" to 0), elements(applied).associate { it.id to it.zIndex })
        assertRoundTrip(base, cmd)
    }

    @Test
    fun `EditTextCommand replaces text and inverts`() {
        val base = doc(txt("a", text = "old"))
        val cmd = EditTextCommand(pageIndex = 0, id = "a", before = txt("a", text = "old"), after = txt("a", text = "new"))
        assertEquals("new", (elements(cmd.applyTo(base)).single() as TextElement).text)
        assertRoundTrip(base, cmd)
    }

    @Test
    fun `AddPageCommand inserts a page and renumbers indices, invert removes`() {
        val base = doc(txt("a"))
        val newPage = Page(index = -1, role = PageRole.INTERIOR, elements = emptyList())
        val cmd = AddPageCommand(page = newPage, atIndex = 0)
        val applied = cmd.applyTo(base)
        assertEquals(2, applied.pages.size)
        assertEquals(listOf(0, 1), applied.pages.map { it.index }) // renumbered
        assertRoundTrip(base, cmd)
    }

    @Test
    fun `DeletePageCommand removes a page and renumbers, invert restores`() {
        val twoPage = doc(txt("a")).let {
            it.copy(pages = it.pages + Page(index = 1, role = PageRole.INTERIOR, elements = listOf(txt("b"))))
        }
        val cmd = DeletePageCommand(page = twoPage.pages[1], atIndex = 1, priorSelection = setOf("b"))
        assertEquals(1, cmd.applyTo(twoPage).pages.size)
        assertRoundTrip(twoPage, cmd)
    }
}
