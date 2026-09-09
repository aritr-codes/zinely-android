package com.aritr.zinely.ui.theme

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the V2 icon set against the frozen HTML it was transcribed from.
 *
 * The central test is [every icon is byte-identical to the frozen source, geometry and paint together]:
 * it re-extracts every 24×24 icon from the three frozen files and asserts set equality with the Kotlin,
 * in both directions. It makes the two incapable of drifting apart.
 *
 * **It compares paint as well as geometry, and the first version did not.** That version passed while
 * `StampFace`'s pupils were flipped from filled to hollow, and it passed while [ZinelyV2Icons.Favourite]
 * was modelled as a stroked outline when the design fills it — because it discarded every styling
 * attribute before comparing. Geometry was never the likely defect; paint was, and it was the one thing
 * the test could not see. The comparison notation below therefore carries `fill`, `stroke`,
 * `stroke-linecap` and `stroke-linejoin` alongside the coordinates.
 */
class ZinelyV2IconsTest {

    private val mockups = File("../../docs/design/mockups")
    private val files = listOf("v2-library.html", "v2-bench.html", "v2-proof.html")

    private fun html(name: String) = File(mockups, name).readText()
    private fun css(name: String) = html(name).substringBefore("</style>")
    private fun body(name: String) = html(name).substringAfter("</style>")

    /**
     * Every 24×24 icon in a frozen file, as (svg attributes, inner markup).
     *
     * Includes the markup the page *injects* — `startAnim()` swaps two bars into `#animIcon` and
     * `stopAnim()` swaps the triangle back. Those have no `<svg>` of their own, and missing them is how
     * [ZinelyV2Icons.Pause] went untranscribed on the first pass.
     */
    private fun icons(name: String): List<Pair<String, String>> {
        val statik = Regex("""<svg([^>]*)>(.*?)</svg>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(body(name))
            .filter { it.groupValues[1].contains("0 0 24 24") }
            .map { it.groupValues[1] to it.groupValues[2] }
        val injected = Regex("""innerHTML\s*=\s*'(<(?:rect|path|circle)[^']*)'""")
            .findAll(body(name))
            .map { "" to it.groupValues[1] }
        return (statik + injected)
            .map { (a, i) -> a to i.replace(Regex("""\s+"""), " ").trim() }
            .toList()
    }

    /**
     * The frozen inner markup, reduced to the notation both sides are compared in.
     *
     * `<g>` carries presentation that **inherits**, so its attributes are pushed onto its children —
     * `StampSun` wraps its eight rays in `<g stroke-linecap="round">` and they are round-capped whether
     * or not each path says so. An extractor that ignored the group would report the Kotlin (which
     * flattens correctly) as the mismatch.
     */
    private fun frozenShapes(inner: String): List<String> {
        val group = Regex("""<g\s*([^>]*?)>""").find(inner)
        val inherited = group?.let {
            Regex("""([\w-]+)=['"]([^'"]*)['"]""").findAll(it.groupValues[1])
                .associate { a -> a.groupValues[1] to a.groupValues[2] }
        }.orEmpty()
        val groupRange = group?.let { it.range.last + 1 until (inner.indexOf("</g>").takeIf { i -> i >= 0 } ?: inner.length) }

        return Regex("""<(path|circle|rect)\s*([^>]*?)/?>""").findAll(inner).map { m ->
            val own = Regex("""([\w-]+)=['"]([^'"]*)['"]""").findAll(m.groupValues[2])
                .associate { it.groupValues[1] to it.groupValues[2] }
            val at = if (groupRange != null && m.range.first in groupRange) inherited + own else own
            val geometry = when (m.groupValues[1]) {
                "path" -> "path:${at.getValue("d")}"
                "circle" -> "circle:${at.getValue("cx")},${at.getValue("cy")},${at.getValue("r")}"
                else -> "rect:${at["x"] ?: "0"},${at["y"] ?: "0"}," +
                    "${at.getValue("width")},${at.getValue("height")},${at["rx"] ?: "0"}"
            }
            geometry + style(
                filled = at["fill"] == "currentColor" && at["stroke"] == "none",
                cap = at["stroke-linecap"],
                join = at["stroke-linejoin"],
            )
        }.toList()
    }

    /** The Kotlin side, in the same notation. Floats are formatted the way the frozen files write them. */
    private fun kotlinShapes(icon: ZinelyV2Icon): List<String> = icon.shapes.map { s ->
        val geometry = when (s) {
            is ZinelyV2IconShape.Path -> "path:${s.data}"
            is ZinelyV2IconShape.Circle -> "circle:${f(s.cx)},${f(s.cy)},${f(s.r)}"
            is ZinelyV2IconShape.Rect ->
                "rect:${f(s.x)},${f(s.y)},${f(s.width)},${f(s.height)},${f(s.rx)}"
        }
        geometry + style(
            filled = s.paint == ZinelyV2IconPaint.Fill,
            cap = s.cap?.let { if (it == StrokeCapRound) "round" else it.toString() },
            join = s.join?.let { if (it == StrokeJoinRound) "round" else it.toString() },
        )
    }

    private fun style(filled: Boolean, cap: String?, join: String?) = buildString {
        if (filled) append("|fill")
        cap?.let { append("|cap=$it") }
        join?.let { append("|join=$it") }
    }

    /** `4.2f` -> `4.2`, `12f` -> `12`, `0.7f` -> `.7` — the SVG spelling. */
    private fun f(v: Float): String {
        val t = if (v == v.toInt().toFloat()) v.toInt().toString() else v.toString()
        return if (t.startsWith("0.")) t.substring(1) else t
    }

    @Test
    fun `every icon is byte-identical to the frozen source, geometry and paint together`() {
        assertTrue("expected to run with :core:ui as the working directory", mockups.isDirectory)
        val trilogy = files.flatMap { icons(it) }.map { frozenShapes(it.second) }.toSet()
        // A15 installs the collage through `icon.innerHTML=svg('...')`, rather than a literal `<svg>`;
        // scrape that amendment seam explicitly so the test still compares Kotlin to the frozen bytes.
        val v21Amendments = Regex("""icon\.innerHTML=svg\('([^']+)'\)""")
            .findAll(html("v21-bench.html"))
            .map { frozenShapes(it.groupValues[1]) }
            .filter { it == kotlinShapes(ZinelyV2Icons.Collage) }
            .toSet()
        assertEquals("37 distinct marks across the trilogy", 37, trilogy.size)
        assertEquals("A15/A16 contributes the frozen collage mark once", 1, v21Amendments.size)
        val frozen = trilogy + v21Amendments

        ZinelyV2Icons.All.forEach { icon ->
            assertTrue(
                "${icon.name} does not match any frozen icon.\n  kotlin: ${kotlinShapes(icon)}",
                kotlinShapes(icon) in frozen,
            )
        }
        assertEquals(
            "every frozen mark is transcribed, and nothing extra",
            frozen,
            ZinelyV2Icons.All.map { kotlinShapes(it) }.toSet(),
        )
    }

    @Test
    fun `the set is 37 trilogy marks plus the V21 collage amendment`() {
        assertEquals(38, ZinelyV2Icons.All.size)
        assertEquals(38, ZinelyV2Icons.All.map { it.name }.toSet().size)
        assertEquals("43 placements in the frozen trilogy", 43, files.sumOf { icons(it).size })
        // Tick x4, ChevronLeft x2, Close x2, and Play x2 — the second being stopAnim() restoring it.
        val byGeometry = files.flatMap { icons(it) }.groupBy { frozenShapes(it.second) }
        assertEquals("four marks appear more than once", 4, byGeometry.count { it.value.size > 1 })
        assertEquals("six surplus placements", 6, 42 - 36)
    }

    @Test
    fun `only artwork states its own stroke, which is why paint lives at the call site`() {
        // The load-bearing observation behind the whole API shape, asserted rather than asserted-about.
        // If a future freeze gives UI icons their own weights, this fails and ZinelyV2Icon should
        // become a plain ImageVector.
        val selfStyled = files.flatMap { icons(it) }.count { it.first.contains("stroke-width") }
        assertEquals("only the seven artwork marks style themselves in markup", 7, selfStyled)

        val artwork = ZinelyV2Icons.All.filter { it.frozenPaint is ZinelyV2IconPaint.Stroke }
        assertEquals(7, artwork.size)
        artwork.forEach {
            assertEquals(
                "all artwork is drawn at 1.6",
                1.6f,
                (it.frozenPaint as ZinelyV2IconPaint.Stroke).width,
                0f,
            )
        }
        // The three whose sole container states a complete paint, and nothing else.
        assertEquals(
            setOf("Favourite", "Play", "Pause"),
            ZinelyV2Icons.All.filter { it.frozenPaint != null && it.frozenPaint !is ZinelyV2IconPaint.Stroke }
                .map { it.name }.toSet(),
        )
        assertEquals("the remaining 28 take their paint from the call site", 28,
            ZinelyV2Icons.All.count { it.frozenPaint == null })
    }

    @Test
    fun `artwork carries the exact paint its own markup states, cap and join included`() {
        // The parity test above compares the *inner* markup. An icon's own <svg> attributes are where
        // artwork states its stroke — and leaving them uncompared is how a dropped `stroke-linecap`
        // survived a full green suite during review. That is the same blind spot that produced this
        // package's NO-GO, one level up, so it gets its own assertion rather than a wider net.
        val bySvg = files.flatMap { icons(it) }.associate { (attrs, inner) -> frozenShapes(inner) to attrs }

        val artwork = ZinelyV2Icons.All.filter { it.frozenPaint is ZinelyV2IconPaint.Stroke }
        assertEquals("seven marks style themselves", 7, artwork.size)
        artwork.forEach { icon ->
            val attrs = bySvg.getValue(kotlinShapes(icon))
            fun attr(name: String) = Regex("""$name=['"]([\w.]+)""").find(attrs)?.groupValues?.get(1)
            val expected = ZinelyV2IconPaint.Stroke(
                width = attr("stroke-width")!!.toFloat(),
                cap = if (attr("stroke-linecap") == "round") StrokeCapRound else StrokeCapButt,
                join = if (attr("stroke-linejoin") == "round") StrokeJoinRound else StrokeJoinMiter,
            )
            assertEquals("${icon.name} must carry exactly what its markup states", expected, icon.frozenPaint)
        }
    }

    @Test
    fun `the three non-stroked marks match the container CSS they came from`() {
        // These were modelled wrong in the first draft — as ordinary stroked outlines — because the
        // markup says nothing and only the container CSS does. Pin them to that CSS.
        val bench = css("v2-bench.html")
        assertTrue(
            "the favourites star is filled with ochre and has no stroke",
            bench.contains("fill:var(--ochre,var(--ink-ochre))") &&
                Regex("""\.rail \.rl svg\{[^}]*}""").find(bench)!!.value.let { "stroke" !in it },
        )
        assertEquals(ZinelyV2IconPaint.Fill, ZinelyV2Icons.Favourite.frozenPaint)

        val proof = css("v2-proof.html")
        val toggle = Regex("""\.animtoggle svg\{([^}]*)}""").find(proof)!!.groupValues[1]
        assertTrue("the play toggle both fills and strokes", "fill:var(--ink-soft)" in toggle)
        assertTrue("...at weight 1", "stroke-width:1;" in toggle)
        assertEquals(ZinelyV2IconPaint.FillAndStroke(1f), ZinelyV2Icons.Play.frozenPaint)
        assertEquals(ZinelyV2IconPaint.FillAndStroke(1f), ZinelyV2Icons.Pause.frozenPaint)
    }

    @Test
    fun `the call sites spread stroke weight across eight values and size across eleven`() {
        // The numbers the KDoc uses to argue that a baked-stroke asset cannot express this design.
        val rules = files.flatMap { f ->
            Regex("""([^{}]*)\{([^{}]*)}""").findAll(css(f))
                .map { it.groupValues[1].replace(Regex("""\s+"""), " ").trim() to it.groupValues[2] }
                .filter { (sel, _) -> Regex("""[\s.#]svg\b|^svg\b""").containsMatchIn(sel) }
        }
        val widths = rules.mapNotNull { Regex("""stroke-width\s*:\s*([\d.]+)""").find(it.second)?.groupValues?.get(1) }
        val sizes = rules.mapNotNull { Regex("""width\s*:\s*(\d+)px""").find(it.second)?.groupValues?.get(1) }
        assertEquals(setOf("1", "1.7", "1.8", "1.9", "2", "2.2", "2.4", "2.6"), widths.toSet())
        assertEquals(
            setOf("11", "12", "13", "15", "16", "17", "18", "19", "20", "22", "26"),
            sizes.toSet(),
        )
        assertTrue("24px is the authoring grid, never a rendered size", "24" !in sizes)
    }

    @Test
    fun `an icon with no frozen paint refuses to build without the call site's`() {
        val e = runCatching { ZinelyV2Icons.Undo.toImageVector() }.exceptionOrNull()
        assertNotNull("building without a paint must fail loudly", e)
        assertTrue(e is IllegalArgumentException)
        assertTrue(
            "the message should say where the paint comes from, not just that it is missing",
            e!!.message!!.contains("call site"),
        )
        // Anything the design paints unambiguously builds unaided.
        assertEquals("StampSun", ZinelyV2Icons.StampSun.toImageVector().name)
        assertEquals("Favourite", ZinelyV2Icons.Favourite.toImageVector().name)
        assertEquals("Pause", ZinelyV2Icons.Pause.toImageVector().name)
    }

    @Test
    fun `the built vector carries the call site's paint onto every shape`() {
        val vector = ZinelyV2Icons.Search.toImageVector(
            paint = ZinelyV2IconPaint.Stroke(1.7f, StrokeCapRound, StrokeJoinRound),
        )
        assertEquals(24f, vector.viewportWidth, 0f)
        assertEquals(24f, vector.viewportHeight, 0f)
        assertEquals("Search draws a circle and a handle", 2, vector.root.count())
    }

    @Test
    fun `a filled shape is filled and not stroked, and both together is both`() {
        assertNull("Fill contributes no stroke", ZinelyV2IconPaint.Fill.strokeWidth)
        assertEquals(1f, ZinelyV2IconPaint.FillAndStroke(1f).strokeWidth)
        assertTrue("FillAndStroke fills", ZinelyV2IconPaint.FillAndStroke(1f).fills)
        assertTrue("Fill fills", ZinelyV2IconPaint.Fill.fills)
        assertTrue("a plain stroke does not fill", !ZinelyV2IconPaint.Stroke(2f).fills)
    }

    @Test
    fun `circle and rect convert to path data that lands on the right bounds`() {
        // The only geometry this file authors rather than transcribes, checked against the bounds the
        // SVG primitive is defined to have.
        assertEquals(
            "M4.0,11.0 a7.0,7.0 0 1,0 14.0,0 a7.0,7.0 0 1,0 -14.0,0 Z",
            ZinelyV2IconShape.Circle(11f, 11f, 7f).pathData(),
        )
        val rounded = ZinelyV2IconShape.Rect(3f, 3f, 7f, 8f, 1f).pathData()
        assertTrue("starts inset by the corner radius", rounded.startsWith("M4.0,3.0 h5.0 "))
        assertTrue("closes", rounded.endsWith("Z"))
        assertEquals(
            "M0.0,0.0 h10.0 v10.0 h-10.0 Z",
            ZinelyV2IconShape.Rect(0f, 0f, 10f, 10f).pathData(),
        )
    }

    @Test
    fun `D-015 is still open — two concepts are each drawn twice, differently`() {
        assertEquals(
            "two distinct chevron-right geometries",
            2,
            setOf(ZinelyV2Icons.ChevronRight, ZinelyV2Icons.ChevronRightBand).map { kotlinShapes(it) }.toSet().size,
        )
        assertEquals(
            "two distinct check geometries",
            2,
            setOf(ZinelyV2Icons.Tick, ZinelyV2Icons.Done).map { kotlinShapes(it) }.toSet().size,
        )
        // Only ChevronRight mirrors ChevronLeft — the argument that ChevronRightBand is the stray one.
        // If a future freeze unifies them, this fails and D-015 has been answered.
        assertEquals("path:M9 5l7 7-7 7", kotlinShapes(ZinelyV2Icons.ChevronRight).single())
        assertEquals("path:M15 5l-7 7 7 7", kotlinShapes(ZinelyV2Icons.ChevronLeft).single())
    }

    private companion object {
        val StrokeCapRound = androidx.compose.ui.graphics.StrokeCap.Round
        val StrokeCapButt = androidx.compose.ui.graphics.StrokeCap.Butt
        val StrokeJoinRound = androidx.compose.ui.graphics.StrokeJoin.Round
        val StrokeJoinMiter = androidx.compose.ui.graphics.StrokeJoin.Miter
    }
}
