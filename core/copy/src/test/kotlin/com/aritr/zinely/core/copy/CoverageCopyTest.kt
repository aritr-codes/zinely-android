package com.aritr.zinely.core.copy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [Copy.Coverage.unsupported] — the unsupported-character notice line (ADR-070; VOICE §Errors). Pins the
 * English list grammar (`A` · `A and B` · `A, B and C`) and the two invariants the design requires: the
 * line **names the script** (specific refusal, not "some characters") and **reassures nothing is lost**.
 *
 * Pure JVM: `:core:copy` is Android-free, so the notice's wording is verifiable without a device.
 */
class CoverageCopyTest {

    @Test
    fun `one script names it directly`() {
        val line = Copy.Coverage.unsupported(listOf("Bengali"))
        assertEquals(
            "Bengali characters can’t print yet — but they’re saved with your zine, so nothing’s lost.",
            line,
        )
    }

    @Test
    fun `two scripts join with and`() {
        val line = Copy.Coverage.unsupported(listOf("Bengali", "Tamil"))
        assertTrue(line.startsWith("Bengali and Tamil characters"), "two-script grammar: $line")
    }

    @Test
    fun `three or more scripts use an oxford-free serial list`() {
        val line = Copy.Coverage.unsupported(listOf("Bengali", "Tamil", "Thai"))
        assertTrue(line.startsWith("Bengali, Tamil and Thai characters"), "serial-list grammar: $line")
    }

    @Test
    fun `every line names the safety net`() {
        // The one non-negotiable of ADR-070: whatever the scripts, the user is told the text is kept.
        listOf(
            listOf("Bengali"),
            listOf("Bengali", "Tamil"),
            listOf("Emoji", "Arabic", "Hebrew"),
        ).forEach { scripts ->
            val line = Copy.Coverage.unsupported(scripts)
            assertTrue(line.contains("saved") && line.contains("nothing’s lost"), "missing reassurance: $line")
        }
    }

    @Test
    fun `empty list degrades to a grammatical sentence`() {
        // Defensive: the notice is hidden when fully covered, so this never renders — but if it ever did,
        // the sentence must still parse rather than reading "  characters can’t print".
        val line = Copy.Coverage.unsupported(emptyList())
        assertTrue(line.startsWith("Some characters"), "empty-list fallback: $line")
    }
}
