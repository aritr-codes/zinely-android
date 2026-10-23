package com.aritr.zinely.core.render

import com.aritr.zinely.core.copy.Copy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ART-2: the catalogue is **add-only**. A saved zine stores `supplyId`; removing or renaming one orphans
 * every zine that used it. `supply-id-baseline.txt` is the 34 ids that shipped; appending a new id never
 * touches it, removing or renaming one fails here.
 */
class SupplyIdBaselineTest {
    /** Mirrors `DefaultDocumentValidator.SUPPLY_ID` (`:core:data` is not a dependency of this module). */
    private val validatorRegex = Regex("""^[a-z]+\.[a-z]+$""")

    private val baseline: List<String> =
        checkNotNull(javaClass.getResource("/supply-id-baseline.txt")) { "baseline resource missing" }
            .readText().lines().map { it.trim() }.filter { it.isNotEmpty() }

    @Test
    fun `given the shipped baseline, when counted, then it is the 34 ids that shipped and they are distinct`() {
        assertEquals(34, baseline.size)
        assertEquals(baseline.size, baseline.toSet().size)
    }

    @Test
    fun `given every baseline id, when looked up, then it still has an outline, a name and tags`() {
        for (id in baseline) {
            assertTrue(id in SupplyCatalog.OUTLINES, "baseline id $id lost its outline (renamed or removed?)")
            assertTrue(id in Copy.Supplies.NAMES, "baseline id $id lost its name")
            assertTrue(id in Copy.Supplies.TAGS, "baseline id $id lost its tags")
        }
    }

    @Test
    fun `given every catalogue id, when matched against the validator regex, then all match`() {
        for (id in SupplyCatalog.OUTLINES.keys + Copy.Supplies.NAMES.keys) {
            assertTrue(validatorRegex.matches(id), "id '$id' would be rejected by DefaultDocumentValidator")
        }
    }
}
