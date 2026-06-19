package com.aritr.zinely.core.model

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Shared approximate-equality used by tests and (Phase 3) validation. */
class ToleranceTest {
    @Test
    fun `default tolerance is small and positive`() {
        assertTrue(GeometryTolerance.DEFAULT > 0.0)
        assertTrue(GeometryTolerance.DEFAULT <= 1e-6)
    }

    @Test
    fun `doubles within tolerance are approximately equal`() {
        assertTrue(1.0.approxEquals(1.0 + 1e-12))
        assertFalse(1.0.approxEquals(1.0 + 1e-3))
    }

    @Test
    fun `points, rects and lines compare within tolerance`() {
        assertTrue(PtPoint(1.0, 2.0).approxEquals(PtPoint(1.0 + 1e-12, 2.0)))
        assertFalse(PtPoint(1.0, 2.0).approxEquals(PtPoint(1.5, 2.0)))

        assertTrue(PtRect(0.0, 0.0, 10.0, 5.0).approxEquals(PtRect(0.0, 1e-12, 10.0, 5.0)))
        assertFalse(PtRect(0.0, 0.0, 10.0, 5.0).approxEquals(PtRect(0.0, 0.0, 10.0, 6.0)))

        assertTrue(
            PtLine(PtPoint(0.0, 0.0), PtPoint(3.0, 4.0))
                .approxEquals(PtLine(PtPoint(0.0, 0.0), PtPoint(3.0 + 1e-12, 4.0))),
        )
    }

    @Test
    fun `affine transforms compare within tolerance`() {
        val t = AffineTransform2D.translate(5.0, 7.0)
        assertTrue(t.approxEquals(AffineTransform2D(1.0, 0.0, 0.0, 1.0, 5.0 + 1e-12, 7.0)))
        assertFalse(t.approxEquals(AffineTransform2D.translate(5.0, 8.0)))
    }
}
