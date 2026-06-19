package com.aritr.zinely.core.imposition

import com.aritr.zinely.core.model.GridCell
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.Rotation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The canonical single-sheet 8-page convention is the verified oracle from
 * docs/RESEARCH.md R1.2 (NASA/Chandra + Cambridge). These tests pin it as data.
 */
class ConventionSpecTest {
    private val spec = SingleSheet8.TOP_ROW_ROTATED

    @Test
    fun `cell mapping matches the R1_2 oracle`() {
        assertEquals(GridCell(1, 3), spec.cellOf[1]) // front cover
        assertEquals(GridCell(0, 3), spec.cellOf[2])
        assertEquals(GridCell(0, 2), spec.cellOf[3])
        assertEquals(GridCell(0, 1), spec.cellOf[4])
        assertEquals(GridCell(0, 0), spec.cellOf[5])
        assertEquals(GridCell(1, 0), spec.cellOf[6])
        assertEquals(GridCell(1, 1), spec.cellOf[7])
        assertEquals(GridCell(1, 2), spec.cellOf[8]) // back cover
    }

    @Test
    fun `pages 2 to 5 are rotated 180 and pages 1,6,7,8 are upright`() {
        setOf(2, 3, 4, 5).forEach { assertEquals(Rotation.HALF, spec.rotationOf[it], "page $it") }
        setOf(1, 6, 7, 8).forEach { assertEquals(Rotation.NONE, spec.rotationOf[it], "page $it") }
    }

    @Test
    fun `front cover is page 1, back cover is page 8, rest interior`() {
        assertEquals(PageRole.FRONT_COVER, spec.roleOf[1])
        assertEquals(PageRole.BACK_COVER, spec.roleOf[8])
        setOf(2, 3, 4, 5, 6, 7).forEach { assertEquals(PageRole.INTERIOR, spec.roleOf[it], "page $it") }
    }

    @Test
    fun `spec is a bijection of all 8 pages onto 8 distinct cells`() {
        assertEquals((1..8).toSet(), spec.cellOf.keys)
        assertEquals(8, spec.cellOf.values.toSet().size)
        assertEquals((1..8).toSet(), spec.rotationOf.keys)
        assertEquals((1..8).toSet(), spec.roleOf.keys)
    }
}
