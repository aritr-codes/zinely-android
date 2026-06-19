package com.aritr.zinely.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GeometryTest {
    private val eps = 1e-9

    @Test
    fun `size holds width and height`() {
        val s = PtSize(2.0, 3.0)
        assertEquals(2.0, s.width, eps)
        assertEquals(3.0, s.height, eps)
    }

    @Test
    fun `rect exposes derived edges, center and area`() {
        val r = PtRect(10.0, 20.0, 100.0, 50.0)
        assertEquals(110.0, r.right, eps)
        assertEquals(70.0, r.bottom, eps)
        assertEquals(60.0, r.centerX, eps)
        assertEquals(45.0, r.centerY, eps)
        assertEquals(PtPoint(60.0, 45.0), r.center)
        assertEquals(5000.0, r.area, eps)
    }

    @Test
    fun `rect contains points inside and on the boundary, excludes outside`() {
        val r = PtRect(0.0, 0.0, 10.0, 10.0)
        assertTrue(r.contains(PtPoint(5.0, 5.0)))
        assertTrue(r.contains(PtPoint(0.0, 0.0)))
        assertTrue(r.contains(PtPoint(10.0, 10.0)))
        assertFalse(r.contains(PtPoint(10.001, 5.0)))
        assertFalse(r.contains(PtPoint(-0.001, 5.0)))
    }

    @Test
    fun `line length is the euclidean distance`() {
        val l = PtLine(PtPoint(0.0, 0.0), PtPoint(3.0, 4.0))
        assertEquals(5.0, l.length, eps)
    }
}
