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
 * `.dp` does not trip the gate -- see [sanitizeKotlin]. `import`/`package` declaration lines are
 * skipped ([isScannableCodeLine]) -- an `import ...unit.dp` line ends in `.dp` but constructs nothing.
 *
 * ## Gradle up-to-date caveat (by design; enforcement is CI)
 * The enrolment file lives at the repository root and is read from the filesystem at runtime, so it is
 * **not** a declared Gradle input of `testDebugUnitTest`. A purely local, warm-cache build that changes
 * *only* the enrolment file may therefore see this task reported UP-TO-DATE and skip it. This does not
 * weaken the gate: (a) the intended action -- a package joins the list **in the same commit that
 * migrates its sources** -- edits production `.kt` files, which invalidates compilation and reruns the
 * test; and (b) CI runners are fresh, so `testDebugUnitTest` has no prior outputs and always runs (the
 * roadmap requires the check "exists and runs in CI"). Locally, force a check with `--rerun-tasks`.
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

    /**
     * A provenance marker: the evidence a literal offers for where its value came from.
     *
     * [OD-29](../../../../../../../../../docs/DECISIONS.md) settled the rule this gate enforces —
     * *"every value is traceable to the frozen V2 source"*, **not** *"no literal appears"* — and
     * delegated the mechanism to D0. Two forms are accepted, and they are the only two, because a
     * marker has to answer **where did this value come from?** rather than merely assert approval:
     *
     *  - a **frozen address** — `v2-bench.html:351`, `v2-proof.html:155`, `v2-library.html:101`.
     *    This is the frozen trilogy declaring the value (`:root`) or writing it at that use site.
     *  - a **governed reference** — `ADR-074`, `OD-45`, `D-007`. An accepted ADR or owner ruling
     *    that governs the value, which is how a component-level literal that D-006 and D-007
     *    deliberately keep out of a token stays compliant without a token to move to.
     *
     * `// approved`, `// legacy`, `// intentional` are **not** markers and never will be: they name
     * no source, so they cannot be checked against one.
     */
    private val provenanceMarkers = listOf(
        Regex("""\bv2-(bench|proof|library)\.html:\d+"""),
        Regex("""\b(ADR|OD|D)-\d+\b"""),
    )

    private fun hasProvenance(line: String): Boolean =
        provenanceMarkers.any { it.containsMatchIn(line) }

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
            violations += scan(file.readText()).map { (line, rule, text) ->
                Violation(file, line, rule, text)
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

    /**
     * The mechanism itself, proved on fixtures rather than on the repository — the enrolment list is
     * empty, so the live scan asserts nothing about the *verdict*, only about the *plumbing*. These
     * cases are the four OD-29 distinguishes, plus the near-misses that keep the marker honest.
     */
    @Test
    fun `an untraceable literal fails and a literal that names its frozen source passes`() {
        // (A) forbidden: a value with no provenance at all. The V1 paper hex is the live example --
        //     it is a V1 carry-over with no frozen V2 source, exactly OD-29's non-compliant class.
        val untraceable = scan("val c = Color(0xFFF4EFE6)")
        assertEquals(1, untraceable.size)
        assertEquals("Color( literal", untraceable.single().rule)

        // (C) traceable: the frozen trilogy writes this value at that address.
        assertTrue(
            "a frozen address on the same line must satisfy the gate",
            scan("val fill = Color(0xFF6E7F58) // v2-proof.html:158 .btn-save background:var(--matcha)").isEmpty(),
        )
        assertTrue(
            "a frozen address in the comment block above must satisfy the gate",
            scan(
                """
                // Bound edge, transcribed from the frozen bench pages.
                // v2-bench.html:275
                val edge = 6.dp
                """.trimIndent(),
            ).isEmpty(),
        )

        // (B) governed: D-007 keeps spacing at component level and publishes no scale to move it
        //     to, so the literal is compliant where it stands once it names the ruling.
        assertTrue(
            "an accepted ruling must satisfy the gate",
            scan("val pad = 16.dp // D-007 -- spacing stays per-component, no V2 scale exists").isEmpty(),
        )
        assertTrue(
            "an ADR reference must satisfy the gate",
            scan("val shape = RoundedCornerShape(12.dp) // ADR-074 -- layer primitive, no ladder").isEmpty(),
        )

        // (D) unknown provenance: syntactically fine, says nothing about where the value came from.
        //     OD-29 forbids a silent pass here, so this must fail exactly as (A) does.
        assertEquals(1, scan("val pad = 16.dp // intentional").size)
        assertEquals(1, scan("val pad = 16.dp // approved by review").size)
        assertEquals(1, scan("val pad = 16.dp // legacy, do not change").size)
        assertEquals(1, scan("val pad = 16.dp // TODO tokenise this").size)

        // A marker cut off from its literal by a blank line is not annotating it.
        assertEquals(
            1,
            scan(
                """
                // v2-bench.html:275

                val edge = 6.dp
                """.trimIndent(),
            ).size,
        )
        // ...and a marker *below* the literal does not reach back up to it.
        assertEquals(
            1,
            scan(
                """
                val edge = 6.dp
                // v2-bench.html:275
                """.trimIndent(),
            ).size,
        )
        // A marker inside a string is not a comment and must not count.
        assertEquals(1, scan("""val pad = 16.dp; val s = "v2-bench.html:275"""").size)
    }

    /**
     * Enrolment parsing. The list is the *defined term*, so a line that cannot be read as a
     * fully-qualified package name must fail loudly rather than be silently dropped -- a typo that
     * parses to nothing would silently un-enrol a package that its commit believed it had enrolled.
     */
    @Test
    fun `enrolment parsing keeps package names, ignores comments, and rejects malformed entries`() {
        assertEquals(
            setOf("com.aritr.zinely.ui.components", "com.aritr.zinely.ui.a11y"),
            parseEnrolmentLines(
                listOf(
                    "# a comment",
                    "",
                    "com.aritr.zinely.ui.components",
                    "  com.aritr.zinely.ui.a11y   # trailing comment",
                ),
            ),
        )
        for (bad in listOf("com.aritr..components", "com.aritr.zinely.ui components", "Com Aritr", ".leading", "trailing.")) {
            val e = runCatching { parseEnrolmentLines(listOf(bad)) }.exceptionOrNull()
            assertTrue("malformed entry must be rejected clearly: $bad", e != null)
            assertTrue("the message must name the offending entry: $bad", e!!.message!!.contains(bad.trim()))
        }
    }

    /** The committed list must itself parse — a malformed entry would silently narrow the gate. */
    @Test
    fun `the committed enrolment list parses`() {
        val enrolled = parseEnrolment(File(repoRoot(), ENROLMENT_PATH))
        assertTrue(
            "Enrolment is empty until a package migrates; D1 enrols com.aritr.zinely.ui.components " +
                "in the commit that re-skins it (ADR-080 Decision 2). Got: $enrolled",
            enrolled.all { it.startsWith("com.aritr.zinely.") },
        )
    }

    // --- the mechanism -------------------------------------------------------------------------

    /** One untraceable literal: 1-based line, which form it was, and the offending source line. */
    private data class Finding(val line: Int, val rule: String, val text: String)

    /**
     * The gate, as OD-29 defines it: a **narrowed literal gate combined with a provenance
     * annotation**, which are two of the four forms that ruling permits.
     *
     * The four literal forms are still detected exactly as before — that half is unchanged, and
     * deliberately so; what changed is the verdict. A detected literal is:
     *
     *  - **compliant** when it carries a provenance marker, either trailing on its own line or in
     *    the contiguous comment block immediately above it (see [hasProvenance]). This is the case
     *    [D-006] and [D-007] create and that a presence-based gate could not express: a value the
     *    V2 design deliberately keeps at component level, with **no published scale to move it to**,
     *    is compliant where it stands once it says where it came from;
     *  - **non-compliant** when it carries none. That covers both a value invented or carried over
     *    from V1 without a frozen source, and a value whose provenance the mechanism simply cannot
     *    determine. OD-29 binds these together on purpose: *"where an automated check cannot decide
     *    traceability it must **not silently pass** — the package records the untraceable value."*
     *    Undecidable and forbidden therefore share one outcome, and that outcome is failure.
     *
     * There is no allowlist, no exemption list and no per-package escape hatch. The only way a
     * literal passes is by naming its source in the source file, where a reviewer reads it.
     */
    private fun scan(src: String): List<Finding> {
        val raw = src.lines()
        val sanitized = sanitizeKotlin(src).lines()
        val findings = mutableListOf<Finding>()
        sanitized.forEachIndexed { idx, line ->
            if (!isScannableCodeLine(line)) return@forEachIndexed
            val hit = rules.firstOrNull { it.regex.containsMatchIn(line) } ?: return@forEachIndexed
            if (provenanceInScope(raw, idx)) return@forEachIndexed
            findings += Finding(idx + 1, hit.label, raw[idx].trim())
        }
        return findings
    }

    /**
     * Provenance is read from the **raw** source, because a marker lives in a comment and
     * [sanitizeKotlin] blanks comments before the rules ever see them.
     *
     * Two placements are in scope, and only two: trailing on the literal's own line, or in the
     * contiguous run of comment lines directly above it. A blank line ends the run — a marker
     * separated from its literal by empty space is not annotating it.
     */
    private fun provenanceInScope(raw: List<String>, idx: Int): Boolean {
        if (idx < raw.size && hasProvenance(trailingComment(raw[idx]))) return true
        var i = idx - 1
        while (i >= 0) {
            val t = raw[i].trim()
            if (t.isEmpty()) return false
            if (!t.startsWith("//") && !t.startsWith("*") && !t.startsWith("/*")) return false
            if (hasProvenance(t)) return true
            i--
        }
        return false
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

    private fun parseEnrolment(file: File): Set<String> = parseEnrolmentLines(file.readLines())

    /**
     * The comment part of a line, or `""` if it has none.
     *
     * Only a **comment** can carry provenance. A `//` inside a string literal opens no comment, and
     * a marker that is merely string *content* annotates nothing — so the scan walks the line
     * tracking quotes rather than taking the first `//` it sees. Without this, a source file could
     * satisfy the gate by containing the text of an address it does not transcribe.
     */
    private fun trailingComment(line: String): String {
        var i = 0
        var inString = false
        var inChar = false
        while (i < line.length) {
            val c = line[i]
            when {
                c == '\\' && (inString || inChar) -> i++
                c == '"' && !inChar -> inString = !inString
                c == '\'' && !inString -> inChar = !inChar
                c == '/' && i + 1 < line.length && line[i + 1] == '/' && !inString && !inChar ->
                    return line.substring(i)
                c == '/' && i + 1 < line.length && line[i + 1] == '*' && !inString && !inChar ->
                    return line.substring(i)
            }
            i++
        }
        return ""
    }

    /** A fully-qualified Kotlin package name: dot-separated identifiers, no empty segment. */
    private val packageNameRegex = Regex("""^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)*$""")

    /**
     * Blank lines and `#` comments are ignored; everything else must be a package name. An
     * unparseable entry **throws** rather than being dropped: silently ignoring it would leave a
     * package the commit believed it had enrolled unscanned, which is the one failure mode this
     * gate cannot afford.
     */
    private fun parseEnrolmentLines(lines: List<String>): Set<String> =
        lines.map { it.substringBefore('#').trim() }
            .filter { it.isNotEmpty() }
            .onEach { entry ->
                require(packageNameRegex.matches(entry)) {
                    "Malformed entry in $ENROLMENT_PATH: \"$entry\" is not a fully-qualified Kotlin " +
                        "package name. One package per line; blank lines and '#' comments are ignored."
                }
            }
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
