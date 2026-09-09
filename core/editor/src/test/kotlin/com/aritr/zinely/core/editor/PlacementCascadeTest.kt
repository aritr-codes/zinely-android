package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlacementCascadeTest {

    private val page = PtSize(width = 300.0, height = 400.0)
    private val base = Transform(
        xPt = 80.0,
        yPt = 120.0,
        widthPt = 100.0,
        heightPt = 80.0,
        rotationDegrees = 7.0,
    )

    @Test
    fun `first item keeps the exact base transform`() {
        assertEquals(base, cascadedPlacement(base, index = 0, count = 5, pageSizePt = page))
    }

    @Test
    fun `later item advances one twelve-point step per index when room allows`() {
        val placed = cascadedPlacement(base, index = 2, count = 3, pageSizePt = page)

        assertEquals(base.xPt + 24.0, placed.xPt, 0.0)
        assertEquals(base.yPt + 24.0, placed.yPt, 0.0)
        assertEquals(base.widthPt, placed.widthPt, 0.0)
        assertEquals(base.heightPt, placed.heightPt, 0.0)
        assertEquals(base.rotationDegrees, placed.rotationDegrees, 0.0)
    }

    @Test
    fun `step is capped independently on each axis by remaining room`() {
        val nearEdge = Transform(
            xPt = 194.0,
            yPt = 317.0,
            widthPt = 100.0,
            heightPt = 80.0,
        )

        val second = cascadedPlacement(nearEdge, index = 1, count = 4, pageSizePt = page)
        val last = cascadedPlacement(nearEdge, index = 3, count = 4, pageSizePt = page)

        assertEquals(196.0, second.xPt, 0.0)
        assertEquals(318.0, second.yPt, 0.0)
        assertEquals(200.0, last.xPt, 0.0)
        assertEquals(320.0, last.yPt, 0.0)
    }

    @Test
    fun `high final index remains wholly inside the page`() {
        val count = 10_001
        val placed = cascadedPlacement(base, index = count - 1, count = count, pageSizePt = page)

        assertTrue(placed.xPt >= 0.0)
        assertTrue(placed.yPt >= 0.0)
        assertTrue(placed.xPt + placed.widthPt <= page.width)
        assertTrue(placed.yPt + placed.heightPt <= page.height)
        assertEquals(page.width, placed.xPt + placed.widthPt, 1e-9)
        assertEquals(page.height, placed.yPt + placed.heightPt, 1e-9)
    }
}
