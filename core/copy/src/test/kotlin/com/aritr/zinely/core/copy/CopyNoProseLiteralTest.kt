package com.aritr.zinely.core.copy

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.io.File
import java.util.regex.Pattern

/**
 * The C9 guard ([ADR-060]): **no user-facing prose literal survives in the enrolled UI sources.** Every
 * such string must live in [Copy]. This is the mechanical embodiment of [§13]'s "copy is from VOICE"
 * box — previously hundreds of human comparisons per review, now one pure-JVM assertion.
 *
 * It reproduces the inventory's own detector exactly ([V1-CONFORMANCE-INVENTORY] CI-81): after dropping
 * comment lines, a "prose literal" is a double-quoted string containing a lowercase-space-lowercase run
 * (`"[^"]*[a-z] [a-z][^"]*"`). The count that literal filter reports on the enrolled tree must be zero,
 * apart from a tiny, explicitly-justified [ALLOWED] set of **non-VOICE internal strings** (assertion
 * messages a user never sees).
 *
 * Reading source files from a pure-Kotlin module is deliberate: `:core:copy` has no Android dependency,
 * so this runs in the Android-free `core-tests` job. The enrolled files exist on disk regardless of
 * whether `:app`/`:feature:editor` are included in the Gradle build (the `ZINELY_CORE_ONLY` gate only
 * drops the modules, not the checkout).
 */
class CopyNoProseLiteralTest {

    private companion object {
        /** The inventory's filter: a quoted literal with a lowercase-space-lowercase run. */
        val PROSE: Pattern = Pattern.compile("\"[^\"]*[a-z] [a-z][^\"]*\"")

        /** Lines that are wholly a comment (star, double-slash, or slash-star after leading whitespace) — dropped, per the filter. */
        val COMMENT_LINE: Pattern = Pattern.compile("^\\s*(\\*|//|/\\*).*")

        /**
         * The only prose literals permitted to remain in the enrolled tree: **internal, non-user-facing**
         * strings that are not VOICE copy. Each must be justified here. Keep this list minimal — a new
         * entry is a claim that a string is invisible to users, and the reviewer will check it.
         */
        val ALLOWED: Set<String> = setOf(
            // `EditorStore.check{ … }` debug invariant (Codex D2) — a developer assertion message, never
            // rendered or announced. Not copy; deliberately left in place.
            "Autosave effect document is not the freshly-reduced document",
        )

        /** Enrolled roots/files, relative to the repo root — the §C9 "Editor, Shelf and Proof sources; nav host". */
        val ENROLLED_DIRS: List<String> = listOf(
            "feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor",
        )
        val ENROLLED_FILES: List<String> = listOf(
            "app/src/main/java/com/aritr/zinely/editor/ZinelyNavHost.kt",
        )
    }

    private fun repoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").isFile) return dir
            dir = dir.parentFile
        }
        fail<Unit>("Could not locate the repo root (no settings.gradle.kts found walking up from ${System.getProperty("user.dir")})")
        error("unreachable")
    }

    private fun enrolledFiles(root: File): List<File> {
        val fromDirs = ENROLLED_DIRS.flatMap { rel ->
            val dir = File(root, rel)
            assertTrue(dir.isDirectory, "Enrolled dir missing: ${dir.path}")
            dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        }
        val fromFiles = ENROLLED_FILES.map { rel ->
            File(root, rel).also { assertTrue(it.isFile, "Enrolled file missing: ${it.path}") }
        }
        return fromDirs + fromFiles
    }

    /** Each entry: file → the offending prose literals it still contains (excluding the allowlist). */
    private fun offenders(file: File): List<String> {
        val found = mutableListOf<String>()
        file.readText().lineSequence().forEach { line ->
            if (COMMENT_LINE.matcher(line).matches()) return@forEach
            val m = PROSE.matcher(line)
            while (m.find()) {
                val literal = m.group()
                val inner = literal.substring(1, literal.length - 1)
                if (inner !in ALLOWED) found += literal
            }
        }
        return found
    }

    @Test
    fun `no user-facing prose literal survives outside the Copy object`() {
        val root = repoRoot()
        val files = enrolledFiles(root)
        assertTrue(files.isNotEmpty(), "No enrolled files were scanned — the guard would be vacuous")

        val violations = files
            .associateWith { offenders(it) }
            .filterValues { it.isNotEmpty() }

        if (violations.isNotEmpty()) {
            val report = violations.entries.joinToString("\n") { (file, lits) ->
                "  ${file.relativeTo(root).path}:\n" + lits.joinToString("\n") { "    $it" }
            }
            fail<Unit>(
                "User-facing prose literals must live in Copy (ADR-060 / CI-81). " +
                    "Move these to com.aritr.zinely.core.copy.Copy (or, if genuinely non-VOICE internal, " +
                    "add to CopyNoProseLiteralTest.ALLOWED with justification):\n$report",
            )
        }
    }

    @Test
    fun `the guard actually detects a planted prose literal (anti-vacuity)`() {
        // Confidence that the detector is not a no-op: a planted two-word lowercase literal must match,
        // and a hyphenated single token / glyph / allowlisted string must not.
        fun hits(src: String): Int {
            var n = 0
            src.lineSequence().forEach { line ->
                if (COMMENT_LINE.matcher(line).matches()) return@forEach
                val m = PROSE.matcher(line)
                while (m.find()) if (m.group().substring(1, m.group().length - 1) !in ALLOWED) n++
            }
            return n
        }
        assertTrue(hits("val x = \"drag to move\"") == 1, "detector missed a plain prose literal")
        assertTrue(hits("val t = \"proof-screen\"") == 0, "detector flagged a hyphenated test tag")
        assertTrue(hits("val g = \"✿\"") == 0, "detector flagged a decorative glyph")
        assertTrue(
            hits("check(x){ \"Autosave effect document is not the freshly-reduced document\" }") == 0,
            "detector did not honour the allowlist",
        )
    }
}
