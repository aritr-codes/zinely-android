package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Snapping (ADR-029 §5.4): candidate lines from page edges/centre + other elements' edges/centres; snap a
 * moving box's nearest anchor within a points threshold; guides are render-only. Pure.
 */
class SnapTest {

    private val page = PtSize(600.0, 800.0)
    private val tol = 1e-9

    @Test
    fun `left edge within threshold snaps x to the page left`() {
        val moving = PtRect(x = 3.0, y = 300.0, width = 50.0, height = 20.0)
        val r = Snap.snap(moving, others = emptyList(), pageSize = page, thresholdPt = 8.0)
        assertEquals(0.0, r.adjusted.x, tol)
        assertTrue(r.guides.any { it.axis == SnapAxis.VERTICAL && it.positionPt == 0.0 })
    }

    @Test
    fun `centre aligns to another element centre`() {
        val other = PtRect(x = 100.0, y = 0.0, width = 100.0, height = 10.0) // centerX = 150
        val moving = PtRect(x = 122.0, y = 400.0, width = 50.0, height = 20.0) // centerX = 147 → 3 from 150
        val r = Snap.snap(moving, others = listOf(other), pageSize = page, thresholdPt = 8.0)
        assertEquals(150.0, r.adjusted.centerX, tol)
    }

    @Test
    fun `nothing within threshold leaves the rect unchanged with no guides`() {
        val moving = PtRect(x = 250.0, y = 333.0, width = 33.0, height = 17.0)
        val r = Snap.snap(moving, others = emptyList(), pageSize = page, thresholdPt = 8.0)
        assertEquals(moving, r.adjusted)
        assertTrue(r.guides.isEmpty())
    }

    @Test
    fun `x and y snap independently in one pass`() {
        // x: left near 0; y: top near page centre (400).
        val moving = PtRect(x = 5.0, y = 404.0, width = 40.0, height = 40.0)
        val r = Snap.snap(moving, others = emptyList(), pageSize = page, thresholdPt = 8.0)
        assertEquals(0.0, r.adjusted.x, tol)
        assertEquals(400.0, r.adjusted.y, tol)
        assertEquals(2, r.guides.size)
    }
}
