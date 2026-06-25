package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.DoubleRange
import net.jqwik.api.constraints.IntRange
import net.jqwik.api.constraints.Size
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Property-based invariants for the editor core (jqwik): undo round-trips, z-order stays a total order,
 * and hit-test agrees with the renderer's rotation sign across the input space (ADR-029 §8).
 */
class EditorPropertiesTest {

    private fun docWith(el: TextElement) = ZineDocument(
        format = ZineFormat.SINGLE_SHEET_8,
        paperSize = PaperSize.LETTER,
        pages = listOf(Page(index = 0, role = PageRole.FRONT_COVER, elements = listOf(el))),
    )

    @Property
    fun `TransformCommand always round-trips`(
        @ForAll @DoubleRange(min = -500.0, max = 500.0) x: Double,
        @ForAll @DoubleRange(min = 1.0, max = 400.0) w: Double,
        @ForAll @DoubleRange(min = -180.0, max = 180.0) rot: Double,
    ) {
        val base = docWith(TextElement("a", Transform(0.0, 0.0, 10.0, 10.0), 0, "t"))
        val after = Transform(x, x, w, w, rot)
        val cmd = TransformCommand(0, mapOf("a" to base.pages[0].elements[0].transform), mapOf("a" to after))
        assertEquals(base, cmd.invertOn(cmd.applyTo(base)))
    }

    @Property
    fun `normalize always yields a dense 0 until n-1 permutation`(
        @ForAll @Size(min = 1, max = 8) @IntRange(min = -50, max = 50) zs: List<Int>,
    ) {
        val els = zs.mapIndexed { i, z -> TextElement("e$i", Transform(0.0, 0.0, 5.0, 5.0), z, "x") }
        val out = ZOrder.normalize(Page(0, PageRole.INTERIOR, elements = els))
        assertEquals((0 until els.size).toSet(), out.elements.map { it.zIndex }.toSet())
    }

    @Property
    fun `the element centre always hits regardless of rotation`(
        @ForAll @DoubleRange(min = -300.0, max = 300.0) x: Double,
        @ForAll @DoubleRange(min = -300.0, max = 300.0) y: Double,
        @ForAll @DoubleRange(min = 2.0, max = 200.0) w: Double,
        @ForAll @DoubleRange(min = 2.0, max = 200.0) h: Double,
        @ForAll @DoubleRange(min = -360.0, max = 360.0) rot: Double,
    ) {
        val t = Transform(x, y, w, h, rot)
        val page = Page(0, PageRole.INTERIOR, elements = listOf(TextElement("a", t, 0, "x")))
        val centre = PtPoint(x + w / 2.0, y + h / 2.0)
        assertEquals("a", HitTest.topmostAt(page, centre))
    }

    @Property
    fun `a corner maps inside the rotated box via the renderer's forward transform`(
        @ForAll @DoubleRange(min = 4.0, max = 200.0) w: Double,
        @ForAll @DoubleRange(min = 4.0, max = 200.0) h: Double,
        @ForAll @DoubleRange(min = -180.0, max = 180.0) rot: Double,
    ) {
        // Forward-map the top-left corner (renderer convention) and confirm hit-test (the inverse) agrees.
        val t = Transform(100.0, 100.0, w, h, rot)
        val el = TextElement("a", t, 0, "x")
        val cx = 100.0 + w / 2.0; val cy = 100.0 + h / 2.0
        val cornerPage = AffineTransform2D.rotateDeg(rot).map(PtPoint(-w / 2.0 + 0.01, -h / 2.0 + 0.01))
        assertTrue(HitTest.contains(el, PtPoint(cx + cornerPage.x, cy + cornerPage.y)))
    }
}
