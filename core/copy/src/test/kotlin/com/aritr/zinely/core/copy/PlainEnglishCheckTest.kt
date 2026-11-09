package com.aritr.zinely.core.copy

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * The ADR-151 local check: a **heuristic** for the plain-English sentence and word rules in VOICE.md §2a, run over the
 * prose literals in `Copy.kt`. It is not an ASD-STE100 compliance check and nothing a user reads may say so.
 *
 * It flags a sentence over [MAX_WORDS] words, a semicolon, a word or phrase from [BANNED], and an -ing word that is not in
 * [ING_ALLOWED]. Copy that broke a rule before ADR-151 is listed in `plain-english-baseline.txt` (one finding a line).
 *
 * Limits: it reads source text, so a sentence assembled at run time from pieces (`lead + where + "."`) is checked piece by
 * piece, and it cannot tell an instruction (20 words) from a description (25). Review those by hand.
 *
 * Invariant (a ratchet): a finding that is not in the baseline fails the test, and a baseline line that no longer matches a
 * finding also fails, so the baseline only shrinks. A rewrite that fixes a string must delete its line in the same change.
 */
class PlainEnglishCheckTest {

    private companion object {
        /** ADR-151 decision 1: 20 for an instruction, 25 for a description. A heuristic cannot tell them apart, so 25. */
        const val MAX_WORDS = 25

        /** A literal is prose when it has a lowercase-space-lowercase run (the C9 detector, CopyNoProseLiteralTest). */
        val PROSE = Regex("[a-z] [a-z]")
        val LITERAL = Regex("\"((?:[^\"\\\\]|\\\\.)*)\"")
        val COMMENT_LINE = Regex("^\\s*(\\*|//|/\\*).*")
        val TEMPLATE = Regex("\\$\\{[^}]*}|\\$[A-Za-z_][A-Za-z0-9_]*")

        /** Hand-written (ADR-151 decision 7): idioms, phrasal verbs with a one-word form, and filler. */
        val BANNED: List<String> = listOf(
            "grab", "free some up", "out of space", "take a look", "check out", "set up", "a bunch", "kind of", "sort of",
            "in order to", "utilize", "simply", "basically", "on the fly", "figure out", "make sure to",
        )

        /** -ing words that are not -ing forms of a verb, or are glossary nouns. */
        val ING_ALLOWED: Set<String> = setOf(
            "thing", "things", "nothing", "something", "anything", "everything", "string", "strings", "bring", "ring", "rings",
            "spring", "during", "morning", "evening", "ceiling", "wing", "king", "sing", "sting", "swing",
        )
        val ING = Regex("\\b([a-z]+ing)\\b")
    }

    private fun repoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").isFile) return dir
            dir = dir.parentFile
        }
        error("repo root not found from ${System.getProperty("user.dir")}")
    }

    /** Every finding in `Copy.kt`, as "rule | sentence", in file order, without duplicates. */
    private fun findings(): List<String> {
        val src = File(repoRoot(), "core/copy/src/main/kotlin/com/aritr/zinely/core/copy/Copy.kt")
        val out = LinkedHashSet<String>()
        // A sentence is often split over lines as `"first part " +` / `"second part"`. Join adjacent literals first, so the
        // length rule sees the whole sentence (review 3, R7). Comment lines are dropped before the join.
        val joined = src.readLines().filterNot { COMMENT_LINE.matches(it) }.joinToString("\n")
            .replace(Regex("\"\\s*\\+\\s*\\n?\\s*\""), "")
        joined.lines().forEach { line ->
            LITERAL.findAll(line).map { it.groupValues[1] }.filter { PROSE.containsMatchIn(it) }.forEach { lit ->
                val text = TEMPLATE.replace(lit, "X").replace("\\n", " ").replace("\\\"", "\"")
                text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }.forEach { s ->
                    val lower = s.lowercase()
                    val words = s.split(Regex("\\s+")).count { it.any(Char::isLetterOrDigit) }
                    if (words > MAX_WORDS) out += "length $words | $s"
                    if (';' in s) out += "semicolon | $s"
                    BANNED.filter { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(lower) }.forEach { out += "banned '$it' | $s" }
                    ING.findAll(lower).map { it.groupValues[1] }.filterNot { it in ING_ALLOWED }.distinct()
                        .forEach { out += "ing '$it' | $s" }
                }
            }
        }
        return out.toList()
    }

    private fun baseline(): List<String> =
        javaClass.getResource("/plain-english-baseline.txt")!!.readText().lines().map { it.trimEnd() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }

    @Test
    fun `given Copy, when checked, then no new plain-English finding appears`() {
        val known = baseline().toSet()
        val fresh = findings().filterNot { it in known }
        assertTrue(fresh.isEmpty()) {
            "New copy breaks the ADR-151 rules (VOICE.md §2a). Rewrite it; do not add it to the baseline:\n" +
                fresh.joinToString("\n")
        }
    }

    @Test
    fun `given the baseline, when checked, then every line still matches a finding`() {
        val now = findings().toSet()
        val stale = baseline().filterNot { it in now }
        assertTrue(stale.isEmpty()) {
            "These baseline lines no longer match Copy.kt. The copy was fixed or changed: delete the lines:\n" +
                stale.joinToString("\n")
        }
    }
}
