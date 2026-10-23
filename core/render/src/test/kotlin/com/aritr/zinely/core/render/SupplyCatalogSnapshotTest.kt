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

    /**
     * The shipped ids whose drawing ADR-139 changes (ADR-132 *As built*, v21-bench.html A39), wired one family
     * per change: these are held by `SupplyRedrawBoundsTest` (old ink box) and `BenchArtSheetParityTest` (A39
     * row) instead. Every other shipped id must still draw exactly as it did. Grows to the 17 redraws, no more.
     */
    private val redrawn = setOf("tape.torn", "fix.corner", "fix.staple", "fix.clip", "tape.masking",
        "mark.asterisk", "mark.arrow", "mark.hand", "mark.bar",
        "paper.strip", "paper.tag", "paper.underline",
    )

    @Test
    fun `given the pre-split snapshot, when OUTLINES is serialised, then every id and coordinate is identical`() {
        val baseline = javaClass.getResource("/supply-outlines-v0.txt")!!.readText()
        // Restricted to the shipped ids (supply-id-baseline.txt) so later families can add ids without editing this.
        val ids = javaClass.getResource("/supply-id-baseline.txt")!!.readText().lines().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        assertEquals(34, ids.size, "the baseline is the 34 shipped ids")
        val kept = ids - redrawn
        val baselineKept = lines(baseline).filter { line -> line.substringBefore("|") in kept }
        assertEquals(baselineKept, lines(SupplyCatalogSnapshot.serialize(SupplyCatalog.OUTLINES.filterKeys { it in kept })))
    }
}
