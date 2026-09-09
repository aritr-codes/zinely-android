package com.aritr.zinely.feature.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class ReframeFlipTest {
    @Test
    fun `reflected axes invert crop deltas so visible drag direction stays screen-directional`() {
        val ordinary = reframePanFraction(
            panPx = 20f,
            coverExtent = 1.5,
            zoom = 2.0,
            framePx = 100.0,
            flipped = false,
        )
        val reflected = reframePanFraction(
            panPx = 20f,
            coverExtent = 1.5,
            zoom = 2.0,
            framePx = 100.0,
            flipped = true,
        )
        assertEquals(-0.15, ordinary, 1e-9)
        assertEquals(0.15, reflected, 1e-9)
    }

    @Test
    fun `degenerate frame produces no crop movement`() {
        assertEquals(
            0.0,
            reframePanFraction(20f, coverExtent = 1.5, zoom = 2.0, framePx = 0.0, flipped = true),
            0.0,
        )
    }
}
