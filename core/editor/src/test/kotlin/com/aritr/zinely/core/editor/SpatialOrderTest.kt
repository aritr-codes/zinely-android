package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.Element
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Combinators
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * [SpatialOrder] — the §4.5 canvas clause (ADR-119). Fixtures pin the rule's cases; the properties pin
 * what must hold for any page: a permutation, deterministic, and blind to `zIndex` and list order.
 */
class SpatialOrderTest {

    private val pageH = 300.0

    private fun el(id: String, x: Double, y: Double, w: Double, h: Double, rot: Double = 0.0, z: Int = 0): Element =
        TextElement(id = id, transform = Transform(x, y, w, h, rot), zIndex = z, text = id)

    private fun ids(vararg els: Element, pageHeightPt: Double = pageH) =
        SpatialOrder.order(els.toList(), pageHeightPt).map { it.id }

    // ---- Fixtures ----------------------------------------------------------------------------------

    @Test
    fun `a row reads left to right, rows read top to bottom`() {
        // Declared in reverse of reading order on purpose.
        val out = ids(
            el("d", 110.0, 110.0, 50.0, 30.0),
            el("c", 10.0, 112.0, 50.0, 30.0),
            el("b", 110.0, 12.0, 50.0, 30.0),
            el("a", 10.0, 10.0, 50.0, 30.0),
        )
        assertEquals(listOf("a", "b", "c", "d"), out)
    }

    @Test
    fun `a row member is judged by its vertical centre against the opener's span`() {
        // `b` starts below `a`'s top but its centre (y=35) lies inside a's span [10, 50]; `c`'s centre (70)
        // does not, so it opens the next row even though it is further left.
        val out = ids(
            el("c", 0.0, 60.0, 20.0, 20.0),
            el("b", 100.0, 25.0, 20.0, 20.0),
            el("a", 50.0, 10.0, 20.0, 40.0),
        )
        assertEquals(listOf("a", "b", "c"), out)
    }

    @Test
    fun `columns whose tops do not share a row interleave, row by row`() {
        // The brief's stated limit: row-major is predictable, not a column reader.
        val out = ids(
            el("L1", 10.0, 10.0, 80.0, 20.0),
            el("L2", 10.0, 50.0, 80.0, 20.0),
            el("R1", 110.0, 30.0, 80.0, 20.0),
        )
        assertEquals(listOf("L1", "R1", "L2"), out)
    }

    @Test
    fun `rotation is judged on the rotated bounds`() {
        // A 150x20 bar turned 90° about its centre (75, 20) stands from y=-55 to y=95 (150 tall, under the
        // guard), left edge 65. It opens the first row, and the box at y=70 (centre 75) falls inside that
        // span and reads before it by left edge.
        val out = ids(
            el("bar", 0.0, 10.0, 150.0, 20.0, rot = 90.0),
            el("box", 0.0, 70.0, 10.0, 10.0),
        )
        assertEquals(listOf("box", "bar"), out)
        // Unrotated, the bar is a short banner at the top and the box is its own later row.
        assertEquals(listOf("bar", "box"), ids(el("bar", 0.0, 10.0, 150.0, 20.0), el("box", 0.0, 70.0, 10.0, 10.0)))
    }

    @Test
    fun `a tall element is read first and does not swallow the page`() {
        val out = ids(
            el("caption", 10.0, 250.0, 80.0, 20.0),
            el("title", 10.0, 10.0, 80.0, 20.0),
            el("photo", 0.0, 0.0, 200.0, pageH), // full page: taller than 0.6 × H
        )
        assertEquals(listOf("photo", "title", "caption"), out)
    }

    @Test
    fun `the guard is height only - a full-width short banner opens an ordinary row`() {
        val out = ids(
            el("body", 10.0, 100.0, 80.0, 20.0),
            el("banner", 0.0, 40.0, 200.0, 30.0),
            el("kicker", 150.0, 5.0, 40.0, 20.0),
        )
        assertEquals(listOf("kicker", "banner", "body"), out)
    }

    @Test
    fun `several large elements read by top, then left, then id`() {
        val out = ids(
            el("y", 100.0, 0.0, 50.0, 250.0),
            el("x", 0.0, 0.0, 50.0, 250.0),
            el("w", 0.0, 0.0, 50.0, 250.0),
            el("v", 0.0, 20.0, 50.0, 250.0),
        )
        assertEquals(listOf("w", "x", "y", "v"), out)
    }

    @Test
    fun `exactly 0_6 of the page height is not large`() {
        val out = ids(el("b", 50.0, 0.0, 10.0, 10.0), el("a", 0.0, 100.0, 10.0, 0.6 * pageH))
        assertEquals(listOf("b", "a"), out)
    }

    @Test
    fun `an unknown page height disables the guard`() {
        val title = el("title", 0.0, 10.0, 80.0, 20.0)
        val photo = el("photo", 100.0, 0.0, 50.0, pageH)
        // Guarded, the full-height photo is ground and reads first despite standing to the right…
        assertEquals(listOf("photo", "title"), ids(title, photo))
        // …unguarded, it merely opens the only row, and both read by left edge.
        assertEquals(listOf("title", "photo"), ids(title, photo, pageHeightPt = Double.POSITIVE_INFINITY))
    }

    @Test
    fun `ties break by id, never by zIndex`() {
        val a = el("a", 10.0, 10.0, 20.0, 20.0, z = 9)
        val b = el("b", 10.0, 10.0, 20.0, 20.0, z = 0)
        assertEquals(listOf("a", "b"), ids(b, a))
        assertEquals(listOf("a", "b"), ids(a, b))
    }

    @Test
    fun `restack then spread - the reducer's z and list changes cannot reach the reading order`() {
        val size = PtSize(200.0, pageH)
        val photo = ImageElement(id = "p", transform = Transform(20.0, 150.0, 60.0, 40.0), assetId = "a".repeat(64))
        val words = TextElement(id = "t", transform = Transform(20.0, 20.0, 100.0, 30.0), text = "title")
        val note = TextElement(id = "n", transform = Transform(20.0, 20.0, 100.0, 30.0), text = "note")
        // Pages 2 | 3 (indices 1 and 2) are a spread pair; the photo starts on index 1, left of it.
        val pages = List(8) { i ->
            Page(
                index = i,
                role = PageRole.INTERIOR,
                elements = when (i) {
                    1 -> listOf(photo, words)
                    2 -> listOf(note)
                    else -> emptyList()
                },
            )
        }
        val start = EditorModel(
            document = ZineDocument(format = ZineFormat.SINGLE_SHEET_8, paperSize = PaperSize.LETTER, pages = pages),
            currentPageIndex = 1,
        )
        fun order(m: EditorModel, page: Int) =
            SpatialOrder.order(m.document.pages[page].elements, size.height).map { it.id }

        assertEquals(listOf("t", "p"), order(start, 1))
        val restacked = EditorReducer.reduce(start, Intent.Reorder("t", ReorderOp.TO_BACK)).model
        assertEquals(listOf("t", "p"), order(restacked, 1), "a restack changes zIndex only")

        val spread = EditorReducer.reduce(restacked, Intent.MakeImageSpread("p", 2.0, size)).model
        // The source became the full-page left half, sent to the back: large, so read first, as the ground.
        assertEquals(listOf("p", "t"), order(spread, 1))
        // The partner half was appended with the lowest zIndex on the facing page; it too reads first there.
        val partner = spread.document.pages[2].elements.single { it.id != "n" }
        assertEquals(listOf(partner.id, "n"), order(spread, 2))
        assertEquals("n", spread.document.pages[2].elements.first().id, "precondition: list order disagrees")
    }

    // ---- Properties ---------------------------------------------------------------------------------

    @Property
    fun `the order is a permutation of the input`(@ForAll("pages") els: List<Element>) {
        val out = SpatialOrder.order(els, pageH)
        assertEquals(els.map { it.id }.sorted(), out.map { it.id }.sorted())
    }

    @Property
    fun `list order does not matter`(@ForAll("pages") els: List<Element>) {
        assertEquals(SpatialOrder.order(els, pageH), SpatialOrder.order(els.reversed(), pageH))
        assertEquals(SpatialOrder.order(els, pageH), SpatialOrder.order(els.shuffled(java.util.Random(7)), pageH))
    }

    @Property
    fun `zIndex does not matter`(@ForAll("pages") els: List<Element>) {
        val restacked = els.mapIndexed { i, e -> (e as TextElement).copy(zIndex = els.size - i) }
        assertEquals(SpatialOrder.order(els, pageH).map { it.id }, SpatialOrder.order(restacked, pageH).map { it.id })
    }

    @Property
    fun `every large element is read before every other element`(@ForAll("pages") els: List<Element>) {
        val out = SpatialOrder.order(els, pageH)
        val isLarge = out.map { e ->
            val t = e.transform
            val r = t.rotationDegrees * kotlin.math.PI / 180.0
            t.widthPt * kotlin.math.abs(kotlin.math.sin(r)) + t.heightPt * kotlin.math.abs(kotlin.math.cos(r)) >
                SpatialOrder.LARGE_ELEMENT_HEIGHT_FRACTION * pageH
        }
        assertEquals(isLarge.sortedDescending(), isLarge, "a large element came after an ordinary one")
    }

    @Property
    fun `the order is deterministic`(@ForAll("pages") els: List<Element>) {
        assertEquals(SpatialOrder.order(els, pageH), SpatialOrder.order(els, pageH))
    }

    @Provide
    fun pages(): Arbitrary<List<Element>> {
        val coord = Arbitraries.doubles().between(-50.0, 250.0).ofScale(0)
        val size = Arbitraries.doubles().between(1.0, 320.0).ofScale(0)
        val rot = Arbitraries.of(0.0, 0.0, 15.0, 90.0, -45.0, 180.0)
        val z = Arbitraries.integers().between(-3, 3)
        val box = Combinators.combine(coord, coord, size, size, rot, z).`as` { x, y, w, h, r, zi ->
            Transform(x, y, w, h, r) to zi
        }
        // Coarse integer coordinates on purpose: they make exact ties (and so the id tie-break) common.
        return box.list().ofMinSize(0).ofMaxSize(12).map { list ->
            list.mapIndexed { i, (t, zi) -> TextElement(id = "e$i", transform = t, zIndex = zi, text = "e$i") }
        }
    }
}
