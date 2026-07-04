package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.imposition.SingleSheet8
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Export screen's decorative sheet must show the CANONICAL single-sheet-8 order — the checkpoint
 * caught it drifting (5·4·3·6 / 8·1·2·7) from the engine's verified convention (docs/RESEARCH.md
 * R1.2, `SingleSheet8.TOP_ROW_ROTATED`). Pure JVM: asserts the derivation, no composition needed.
 */
class DecorativeImpositionOrderTest {

    @Test
    fun `decorative rows match the canonical TOP_ROW_ROTATED convention`() {
        // Given the canonical engine convention (the default spec)
        // When the decorative rows are derived
        val rows = decorativeImpositionRows()

        // Then the picture shows exactly the engine's order: top 5 4 3 2, bottom 6 7 8 1
        assertEquals(
            listOf(listOf(5, 4, 3, 2), listOf(6, 7, 8, 1)),
            rows.map { row -> row.map { it.pageNumber } },
        )
    }

    @Test
    fun `top row is flipped and bottom row is upright, per the convention's rotations`() {
        // Given the canonical engine convention
        // When the decorative rows are derived
        val rows = decorativeImpositionRows(SingleSheet8.TOP_ROW_ROTATED)

        // Then rotation follows the convention: the whole top row upside-down, the bottom upright
        assertTrue(rows[0].all { it.flipped })
        assertFalse(rows[1].any { it.flipped })
    }
}
