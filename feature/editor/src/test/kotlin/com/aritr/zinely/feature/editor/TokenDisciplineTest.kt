package com.aritr.zinely.feature.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * CI-27 -- the repository's FIRST static gate on design-token discipline (milestone C1).
 *
 * The repository has **no lint, no detekt, no ktlint, no spotless**; `explicitApi()` in nine modules
 * is the only other static gate. This is a **pure-JVM unit test** (no new Gradle dependency, no lint
 * plugin) that scans the production (`src/main`) Kotlin sources of every **enrolled** package and
 * fails the build on any of the four raw literal forms the design system forbids outside the token
 * layer:
 *
 *   1. `.dp`                    -- a raw density-pixel spacing/size literal
 *   2. `.sp`                    -- a raw scaled-pixel type-size literal
 *   3. `Color(`                 -- a raw colour constructor outside `ZinelyColors.kt`
 *   4. `RoundedCornerShape(`    -- a raw radius literal outside the shape tokens
 *
 * Authority: [docs/ZINELY-DESIGN-SYSTEM.md] Section 13 (spacing / radius boxes);
 * [docs/V1-IMPLEMENTATION-ROADMAP.md] Sections 10.1-10.2; inventory item CI-27.
 *
 * ## "Enrolled package" is a defined term
 * A package is enrolled when its fully-qualified Kotlin package name appears on a non-comment line in
 * the committed enrolment file [config/token-enrolment.txt]. The check is scoped by that committed
 * list, **not by file type** -- which is exactly why it survives the palette: `ZinelyColors.kt` (34
 * legitimate `Color(` literals) and `Theme.kt`'s legacy scheme (9 more) live in
 * `com.aritr.zinely.ui.theme`, a package that is simply never enrolled.
 *
 * ## Enrolment <-> migration coupling
 * A package joins the enrolment list in the **same commit that migrates it** off raw literals. At C1
 * the list is intentionally **empty**: nothing has been migrated, so nothing can be enrolled without
 * failing this test. The deliverable of CI-27 is the *mechanism* and the *defined term*, not coverage;
 * later milestones (C3a/C6/C7...) enrol packages as they migrate. See the file header for the full
 * contract.
 *
 * Matching is by **exact** package name; sub-packages are not pulled in transitively (each enrols on
 * its own line). Comments and string-literal contents are ignored, so KDoc that merely *mentions*
 * `.dp` does not trip the gate -- see [sanitizeKotlin].
 */
class TokenDisciplineTest {

    /** One forbidden literal form: a human name and the regex that finds it in *sanitized* source. */
    private data class Rule(val label: String, val regex: Regex)

    private val rules = listOf(
        // `\.dp\b` / `\.sp\b`: a `.dp`/`.sp` property access whose token ends on a word boundary, so
        // `.dpi` or an identifier ending in `sp` is not matched.
        Rule(".dp literal", Regex("""\.dp\b""")),
        Rule(".sp literal", Regex("""\.sp\b""")),
        // `\bColor\s*\(`: the `Color(` constructor. The leading `\b` means `MyColor(` (no word
        // boundary before `Color`) is not matched, and `ColorFilter(` is not matched because `Color`
        // is not immediately followed by `(`.
        Rule("Color( literal", Regex("""\bColor\s*\(""")),
        Rule("RoundedCornerShape( literal", Regex("""\bRoundedCornerShape\s*\(""")),
    )

    private data class Violation(val file: File, val line: Int, val rule: String, val text: String)

    @Test
    fun `no raw dp, sp, Color or RoundedCornerShape literal survives in any enrolled package`() {
        val repoRoot = repoRoot()
        val enrolmentFile = File(repoRoot, ENROLMENT_PATH)
        assertTrue(
            "The enrolment list must be a committed file at $ENROLMENT_PATH -- it defines the term " +
                "\"enrolled package\". Looked under repo root: ${repoRoot.absolutePath}",
            enrolmentFile.isFile,
        )

        val enrolled = parseEnrolment(enrolmentFile)

        val violations = mutableListOf<Violation>()
        for (file in productionKotlinSources(repoRoot)) {
            val pkg = packageOf(file) ?: continue
            if (pkg !in enrolled) continue
            val sanitized = sanitizeKotlin(file.readText())
            sanitized.lineSequence().forEachIndexed { idx, line ->
                if (!isScannableCodeLine(line)) return@forEachIndexed
                for (rule in rules) {
                    if (rule.regex.containsMatchIn(line)) {
                        violations += Violation(file, idx + 1, rule.label, line.trim())
                    }
                }
            }
        }

        assertTrue(
            buildString {
                append("Design-token discipline (CI-27) failed: ")
                append(violations.size)
                append(" raw literal(s) found in enrolled package(s).\n")
                append("Every enrolled package must use the design tokens, not raw literals. ")
                append("Either migrate the literal onto a token, or -- if the package was enrolled ")
                append("prematurely -- remove it from $ENROLMENT_PATH.\n\n")
                violations.forEach { v ->
                    append("  ")
                    append(v.file.relativeTo(repoRoot).path.replace('\\', '/'))
                    append(':')
                    append(v.line)
                    append("  [")
                    append(v.rule)
                    append("]  ")
                    append(v.text)
                    append('\n')
                }
            },
            violations.isEmpty(),
        )
    }

    /**
     * Intrinsic proof of the mechanism, independent of what the enrolment list currently contains
     * (it is empty at C1). Asserts each of the four forms is detected in live code, and that the
     * detector does **not** fire on the same tokens when they appear only in comments or strings.
     */
    @Test
    fun `the detector catches each forbidden form and ignores comments and strings`() {
        fun hits(src: String): Set<String> {
            val sanitized = sanitizeKotlin(src)
            return rules.filter { it.regex.containsMatchIn(sanitized) }.map { it.label }.toSet()
        }

        // Live code: every form must be caught.
        assertEquals(setOf(".dp literal"), hits("val p = Modifier.padding(16.dp)"))
        assertEquals(setOf(".sp literal"), hits("val s = 14.sp"))
        assertEquals(setOf("Color( literal"), hits("val c = Color(0xFFC64E34)"))
        assertEquals(setOf("RoundedCornerShape( literal"), hits("val r = RoundedCornerShape(8)"))
        assertEquals(
            setOf(".dp literal", "Color( literal"),
            hits("Box(Modifier.size(4.dp).background(Color(0xFF000000)))"),
        )

        // Comments and strings: nothing must be caught.
        assertTrue("line comment must be ignored", hits("// spacing was 16.dp before tokens").isEmpty())
        assertTrue("block comment must be ignored", hits("/* uses Color(0x..) historically */").isEmpty())
        assertTrue("kdoc must be ignored", hits("/** Padding of 8.dp, radius RoundedCornerShape(4). */").isEmpty())
        assertTrue("string literal must be ignored", hits("""val label = "16.dp"""").isEmpty())

        // Near-misses that must NOT be flagged (word-boundary / qualifier discipline).
        assertTrue(".dpi must not match .dp", hits("val x = screen.dpi").isEmpty())
        assertTrue("ColorFilter must not match Color(", hits("val f = ColorFilter.tint(c)").isEmpty())
        assertTrue("MyColor( must not match Color(", hits("val c = MyColor(1, 2, 3)").isEmpty())

        // import/package declarations are skipped before rules ever run, even though `.dp`/`.sp`
        // appear at the end of the unit imports.
        assertTrue(isScannableCodeLine("    Modifier.padding(16.dp)"))
        assertTrue("import must be skipped", !isScannableCodeLine("import androidx.compose.ui.unit.dp"))
        assertTrue("import must be skipped", !isScannableCodeLine("import androidx.compose.ui.unit.sp"))
        assertTrue("package must be skipped", !isScannableCodeLine("package com.aritr.zinely.ui.components"))
    }

    // --- helpers -------------------------------------------------------------------------------

    /**
     * `import`/`package` declarations are not usage literals: an `import androidx.compose.ui.unit.dp`
     * line ends in `.dp` but constructs nothing. Excluding them keeps the gate to real code only.
     */
    private fun isScannableCodeLine(line: String): Boolean {
        val t = line.trimStart()
        return !t.startsWith("import ") && !t.startsWith("package ")
    }

    private fun parseEnrolment(file: File): Set<String> =
        file.readLines()
            .map { it.substringBefore('#').trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    private fun productionKotlinSources(repoRoot: File): Sequence<File> =
        repoRoot.walkTopDown()
            .onEnter { dir -> dir.name != "build" && dir.name != ".git" && dir.name != ".claude" }
            .filter { it.isFile && it.extension == "kt" }
            .filter { file ->
                // Only production sources: `.../src/main/...`. Normalise separators for Windows.
                file.absolutePath.replace('\\', '/').contains("/src/main/")
            }

    private val packageRegex = Regex("""^\s*package\s+([\w.]+)""")

    private fun packageOf(file: File): String? {
        file.useLines { lines ->
            for (line in lines) {
                val m = packageRegex.find(line)
                if (m != null) return m.groupValues[1]
            }
        }
        return null
    }

    private fun repoRoot(): File {
        val start: String = System.getProperty("user.dir") ?: "."
        var dir: File? = File(start).absoluteFile
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").isFile) return dir
            dir = dir.parentFile
        }
        error("Could not locate repository root (no settings.gradle.kts above $start)")
    }

    /**
     * Blanks out Kotlin comments and string-literal contents so the literal-form regexes only ever
     * see real code. Newlines are preserved in place, so line numbers reported to a developer stay
     * accurate. Handled: `//` line comments, `/* */` (and `/** */`) block comments, `"..."` and
     * `"""..."""` strings, and `'.'` char literals (so a `"` inside a char literal cannot desync the
     * string scanner).
     */
    private fun sanitizeKotlin(src: String): String {
        val out = StringBuilder(src.length)
        var i = 0
        val n = src.length
        fun peek(offset: Int): Char = if (i + offset < n) src[i + offset] else ' '
        fun blank(c: Char) = out.append(if (c == '\n') '\n' else ' ')
        while (i < n) {
            val c = src[i]
            when {
                c == '/' && peek(1) == '/' -> {
                    while (i < n && src[i] != '\n') { blank(src[i]); i++ }
                }
                c == '/' && peek(1) == '*' -> {
                    out.append("  "); i += 2
                    while (i < n && !(src[i] == '*' && peek(1) == '/')) { blank(src[i]); i++ }
                    if (i < n) { out.append("  "); i += 2 } // consume closing */
                }
                c == '"' && peek(1) == '"' && peek(2) == '"' -> {
                    out.append("   "); i += 3
                    while (i < n && !(src[i] == '"' && peek(1) == '"' && peek(2) == '"')) { blank(src[i]); i++ }
                    if (i < n) { out.append("   "); i += 3 }
                }
                c == '"' -> {
                    blank(c); i++
                    while (i < n && src[i] != '"') {
                        if (src[i] == '\\' && i + 1 < n) { out.append("  "); i += 2 }
                        else { blank(src[i]); i++ }
                    }
                    if (i < n) { blank(src[i]); i++ }
                }
                c == '\'' -> {
                    blank(c); i++
                    while (i < n && src[i] != '\'') {
                        if (src[i] == '\\' && i + 1 < n) { out.append("  "); i += 2 }
                        else { blank(src[i]); i++ }
                    }
                    if (i < n) { blank(src[i]); i++ }
                }
                else -> { out.append(c); i++ }
            }
        }
        return out.toString()
    }

    private companion object {
        /** Path, relative to the repository root, of the committed enrolment list (the defined term). */
        const val ENROLMENT_PATH = "config/token-enrolment.txt"
    }
}
