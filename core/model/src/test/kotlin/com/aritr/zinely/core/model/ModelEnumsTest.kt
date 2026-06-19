package com.aritr.zinely.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ModelEnumsTest {
    @Test
    fun `single-sheet 8 format is 8 pages on a 2x4 grid`() {
        val f = ZineFormat.SINGLE_SHEET_8
        assertEquals(8, f.pageCount)
        assertEquals(2, f.rows)
        assertEquals(4, f.cols)
    }

    @Test
    fun `rotation degrees are 0 and 180`() {
        assertEquals(0, Rotation.NONE.degrees)
        assertEquals(180, Rotation.HALF.degrees)
    }

    @Test
    fun `grid cell holds row and col`() {
        val c = GridCell(row = 1, col = 3)
        assertEquals(1, c.row)
        assertEquals(3, c.col)
    }

    @Test
    fun `page roles enumerate cover and interior`() {
        assertEquals(3, PageRole.entries.size)
    }
}
