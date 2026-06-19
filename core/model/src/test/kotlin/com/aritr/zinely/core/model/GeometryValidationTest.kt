package com.aritr.zinely.core.model

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** Invalid geometry must fail at construction, not leak into the engine/SVG. */
class GeometryValidationTest {
    @Test
    fun `point rejects non-finite coordinates`() {
        assertThrows(IllegalArgumentException::class.java) { PtPoint(Double.NaN, 0.0) }
        assertThrows(IllegalArgumentException::class.java) { PtPoint(0.0, Double.POSITIVE_INFINITY) }
    }

    @Test
    fun `size rejects non-finite and negative dimensions`() {
        assertThrows(IllegalArgumentException::class.java) { PtSize(-1.0, 10.0) }
        assertThrows(IllegalArgumentException::class.java) { PtSize(10.0, Double.NaN) }
    }

    @Test
    fun `rect rejects non-finite values and negative size`() {
        assertThrows(IllegalArgumentException::class.java) { PtRect(0.0, 0.0, -5.0, 10.0) }
        assertThrows(IllegalArgumentException::class.java) { PtRect(Double.NaN, 0.0, 5.0, 10.0) }
        assertThrows(IllegalArgumentException::class.java) { PtRect(0.0, 0.0, 5.0, -0.001) }
    }

    @Test
    fun `valid geometry constructs without throwing`() {
        PtPoint(0.0, 0.0)
        PtSize(0.0, 0.0)
        PtRect(0.0, 0.0, 0.0, 0.0)
        PtLine(PtPoint(0.0, 0.0), PtPoint(1.0, 1.0))
    }
}
