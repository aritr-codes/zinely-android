package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.imposition.SingleSheet8
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Proof's imposed sheet (Act 1) must show the CANONICAL single-sheet-8 order — the checkpoint
 * caught it drifting (5·4·3·6 / 8·1·2·7) from the engine's verified convention (docs/RESEARCH.md
 * R1.2, `SingleSheet8.TOP_ROW_ROTATED`). Pure JVM: asserts the derivation, no composition needed.
 *
 * M5 B2 ([ADR-051]) relocated `decorativeImpositionRows` from the Export screen into the Proof and
 * extended this guard with the frozen-grid ↔ engine equivalence: `proof.html`'s corrected illustrative
 * `LAYOUT=[4,3,2,1,5,6,7,0]` (cell → panel, page = panel + 1) must equal the engine's cell order, so
 * the frozen illustration and the Compose sheet can never diverge from the imposition ([ADR-050]).
 */
class DecorativeImpositionOrderTest {

    @Test
    fun `flattened cell order equals the frozen proof-html LAYOUT`() {
        // Given the corrected frozen illustrative grid LAYOUT=[4,3,2,1,5,6,7,0] (cell -> 0-based panel)
        // When mapped to 1-based pages (page = panel + 1): 5 4 3 2 · 6 7 8 1
        val frozenLayoutPages = listOf(4, 3, 2, 1, 5, 6, 7, 0).map { it + 1 }

        // Then the engine-derived sheet, read cell-major, matches the frozen illustration exactly.
        val engineOrder = decorativeImpositionRows().flatten().map { it.pageNumber }
        assertEquals(frozenLayoutPages, engineOrder)
    }

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
