package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Hit-testing (ADR-029 §5.4): inverse-transform the touch into element-local space (subtract centre,
 * un-rotate by `-rotationDegrees`), AABB-test, iterate `(zIndex desc, listIndex desc)` to mirror the
 * renderer's stable draw order, first hit wins. Pure — no Android, no I/O.
 */
class HitTestTest {

    private fun box(id: String, x: Double, y: Double, w: Double, h: Double, z: Int, rot: Double = 0.0) =
        TextElement(id = id, transform = Transform(x, y, w, h, rot), zIndex = z, text = "x")

    private fun page(vararg els: TextElement) =
        Page(index = 0, role = PageRole.INTERIOR, elements = els.toList())

    @Test
    fun `point inside an unrotated element hits it`() {
        val p = page(box("a", x = 10.0, y = 10.0, w = 100.0, h = 50.0, z = 0))
        assertEquals("a", HitTest.topmostAt(p, PtPoint(50.0, 30.0)))
    }

    @Test
    fun `point outside every element misses`() {
        val p = page(box("a", x = 10.0, y = 10.0, w = 100.0, h = 50.0, z = 0))
        assertNull(HitTest.topmostAt(p, PtPoint(500.0, 500.0)))
    }

    @Test
    fun `overlapping elements - greater zIndex wins`() {
        val p = page(
            box("under", x = 0.0, y = 0.0, w = 100.0, h = 100.0, z = 0),
            box("over", x = 0.0, y = 0.0, w = 100.0, h = 100.0, z = 5),
        )
        assertEquals("over", HitTest.topmostAt(p, PtPoint(50.0, 50.0)))
    }

    @Test
    fun `equal zIndex - later in list wins (mirrors renderer stable draw order)`() {
        val p = page(
            box("first", x = 0.0, y = 0.0, w = 100.0, h = 100.0, z = 0),
            box("second", x = 0.0, y = 0.0, w = 100.0, h = 100.0, z = 0),
        )
        assertEquals("second", HitTest.topmostAt(p, PtPoint(50.0, 50.0)))
    }

    @Test
    fun `rotated element - hit point inside the rotated box but outside the AABB still hits`() {
        // A 100x20 bar centred at (50,50), rotated 90° → occupies a tall 20x100 region.
        // (50, 90) is outside the unrotated AABB (y: 40..60) but inside the rotated bar.
        val p = page(box("bar", x = 0.0, y = 40.0, w = 100.0, h = 20.0, z = 0, rot = 90.0))
        assertEquals("bar", HitTest.topmostAt(p, PtPoint(50.0, 90.0)))
        // A point on the unrotated long axis is now outside the rotated bar.
        assertNull(HitTest.topmostAt(p, PtPoint(95.0, 50.0)))
    }
}
