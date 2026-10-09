package com.aritr.zinely.render.android

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **No production replayer draws with the default resolver (ADR-126; spike §4.2).**
 *
 * [CanvasReplayer]'s `fontResolver` defaults to [FontResolver.Default], which knows no bundled face: a
 * replayer built without one would draw a Book text in the platform's sans and never fail. With one
 * voice that was a wrong-looking Inter; with two it is a different font on one surface only.
 *
 * A source scan, because the claim is about every construction site in every module and no single
 * module's classpath sees them all. Plain JVM.
 */
class ProductionReplayerFontGuardTest {

    private val construction = Regex("""(?<![A-Za-z.])CanvasReplayer\(""")

    /** Every production construction, as `path` to the call's own argument text. */
    private val root = File("..").canonicalFile
    private val sites: List<Pair<String, String>> = root.walkTopDown()
        .onEnter { it == root || (it.name != "build" && !it.name.startsWith(".")) }
        .filter { it.extension == "kt" && it.invariantSeparatorsPath.contains("/src/main/") }
        .filterNot { it.name == "CanvasReplayer.kt" }
        .flatMap { file ->
            val src = file.readText()
            construction.findAll(src).map { m ->
                file.relativeTo(root).invariantSeparatorsPath to src.substring(m.range.last, src.indexOf(')', m.range.last) + 1)
            }
        }
        .toList()

    @Test
    fun `the scan sees the export and the preview, and nothing it does not know`() {
        // Bench, page strip and Read all draw through PagePreview; export draws through ZineExporter. A
        // third site is a new surface and must be added here deliberately.
        assertEquals(
            setOf(
                "app/src/main/java/com/aritr/zinely/export/ZineExporter.kt",
                "feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/PagePreview.kt",
            ),
            sites.map { it.first }.toSet(),
        )
    }

    @Test
    fun `every production replayer is handed the bundled resolver`() {
        assertTrue("no construction site was found", sites.isNotEmpty())
        for ((path, args) in sites) {
            assertTrue(
                "$path builds a CanvasReplayer without the bundled fonts: $args",
                Regex("""fontResolver\s*=\s*(BundledFontResolver\(|previewFontResolver\()""").containsMatchIn(args),
            )
        }
        val seam = File("../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/PagePreview.kt").readText()
        assertTrue(
            "previewFontResolver no longer returns the bundled resolver",
            seam.contains("fun previewFontResolver(assets: AssetManager): FontResolver = BundledFontResolver(assets)"),
        )
    }
}
