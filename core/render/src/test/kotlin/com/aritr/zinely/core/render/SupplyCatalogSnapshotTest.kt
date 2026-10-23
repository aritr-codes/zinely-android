package com.aritr.zinely.core.render

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * ART-1: the catalogue split into per-family objects is a pure move. The resource is `OUTLINES` as it
 * stood before the split (34 ids, every coordinate exact via `Double.toString`); it is a frozen baseline,
 * so a drawing change must be a deliberate edit to it, never a regeneration.
 */
class SupplyCatalogSnapshotTest {
    private fun lines(text: String) = text.trim().lines().sorted()

    @Test
    fun `given the pre-split snapshot, when OUTLINES is serialised, then every id and coordinate is identical`() {
        val baseline = javaClass.getResource("/supply-outlines-v0.txt")!!.readText()
        // Restricted to the shipped ids (supply-id-baseline.txt) so later families can add ids without editing this.
        val ids = javaClass.getResource("/supply-id-baseline.txt")!!.readText().lines().filter { it.isNotBlank() }.toSet()
        assertEquals(lines(baseline), lines(SupplyCatalogSnapshot.serialize(SupplyCatalog.OUTLINES.filterKeys { it in ids })))
    }
}
