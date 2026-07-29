package com.aritr.zinely.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the V2 motion contract to the DESIGN-FROZEN trilogy.
 *
 * Like [ZinelyV2DimensTest], several assertions re-derive from the frozen HTML rather than from
 * transcribed constants — because the load-bearing claims here are again claims about *absence*
 * (no duration tokens, no ladder) and about *divergence* (three different reduced-motion rules),
 * neither of which can be pinned by transcribing a value.
 */
class ZinelyV2MotionTest {

    private val mockups = File("../../docs/design/mockups")

    private fun frozen(name: String): String {
        val f = File(mockups, name)
        assertTrue("expected to run with :core:ui as the working directory — missing $f", f.isFile)
        val text = f.readText()
        assertTrue("$name should contain a <style> block", text.contains("</style>"))
        return text.substringBefore("</style>")
    }

    private val library = frozen("v2-library.html")
    private val bench = frozen("v2-bench.html")
    private val proof = frozen("v2-proof.html")
    private val all = listOf(library, bench, proof)

    // -- the two easings --------------------------------------------------------------------------

    @Test
    fun `settle is the frozen arrival curve`() {
        assertTrue("--settle:cubic-bezier(.05,.7,.1,1)", bench.contains("--settle:cubic-bezier(.05,.7,.1,1)"))
        assertTrue("--settle:cubic-bezier(.05,.7,.1,1)", proof.contains("--settle:cubic-bezier(.05,.7,.1,1)"))
        assertEquals(CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f), ZinelyV2Settle)
    }

    @Test
    fun `standard is the frozen informational curve`() {
        assertTrue("--standard:cubic-bezier(.2,0,0,1)", bench.contains("--standard:cubic-bezier(.2,0,0,1)"))
        assertTrue("--standard:cubic-bezier(.2,0,0,1)", proof.contains("--standard:cubic-bezier(.2,0,0,1)"))
        assertEquals(CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f), ZinelyV2Standard)
    }

    @Test
    fun `the two easings are the only motion tokens the trilogy declares`() {
        // No --fast, no --base, no --duration-*: V1 had duration tokens, V2 has none. If one ever
        // appears, the "durations live at the component" reading has been overtaken by the design.
        val durationTokens = all.sumOf { css ->
            Regex("""--(?:fast|base|slow|duration[\w-]*|dur[\w-]*)\s*:""").findAll(css).count()
        }
        assertEquals("V2 declares no duration token", 0, durationTokens)
    }

    @Test
    fun `there is no duration ladder to publish`() {
        val durations = all.flatMap { css ->
            Regex("""(?:transition|animation)[^;}]*?([\d.]+)s(?![\w-])""").findAll(css)
                .map { it.groupValues[1] }.toList()
        }.toSet()
        assertTrue("expected many bespoke durations, found ${durations.size}: $durations", durations.size >= 10)
    }

    // -- D-011: the Library predates the easing tokens ---------------------------------------------

    @Test
    fun `D-011 is still open — the Library declares neither easing token and uses a third curve`() {
        assertFalse("the Library declares no --settle", library.contains("--settle"))
        assertFalse("the Library declares no --standard", library.contains("--standard"))
        assertTrue(
            "the Library's sheet uses a curve found nowhere else in V2",
            library.contains("cubic-bezier(.2,.8,.2,1)"),
        )
        // And that curve is genuinely unique to it — not a third shared easing that simply went
        // untokenised. If it appears in another file, this is no longer a Library-only staleness.
        assertEquals(
            "cubic-bezier(.2,.8,.2,1) appears only in the Library",
            1,
            all.count { it.contains("cubic-bezier(.2,.8,.2,1)") },
        )
    }

    // -- D-012: three reduced-motion policies -------------------------------------------------------

    @Test
    fun `D-012 is still open — each frozen file writes a different reduced-motion rule`() {
        val rules = all.map { css ->
            Regex("""@media\s*\(prefers-reduced-motion:\s*reduce\)\s*\{([^}]*\}[^}]*)\}""")
                .find(css)?.groupValues?.get(1)?.replace(Regex("""\s+"""), "")
        }
        assertTrue("every file must specify a reduced-motion rule", rules.all { it != null })
        assertEquals("and all three must be different — that is the defect", 3, rules.toSet().size)

        // The distinction the Compose policy is built on: only the Bench disables animation outright,
        // and the Bench is the only file with a looping animation (the caret's `blink … infinite`).
        assertTrue("bench disables animation rather than collapsing it", bench.contains("animation:none"))
        assertTrue("bench is the file with an infinite animation", bench.contains("blink 1.05s steps(1) infinite"))
        assertFalse("the proof collapses animation duration instead", proof.contains("animation:none"))
    }

    // -- the policy -------------------------------------------------------------------------------

    @Test
    fun `one-shot motion collapses to zero under reduced motion`() {
        val reduced = ZinelyV2Motion(reduceMotion = true)
        assertEquals(0, reduced.durationMillis(340))
        assertEquals(0, reduced.settle<Float>(340).durationMillis)
        assertEquals(0, reduced.standard<Float>(200).durationMillis)
    }

    @Test
    fun `continuous motion does not run at all under reduced motion, rather than running at zero`() {
        // The distinction this class exists for: a one-shot at 0ms arrives instantly; a repeating
        // animation at 0ms is an unbounded loop with no delay, which strobes. Reduced motion is in
        // part a photosensitivity setting, so that is the failure that actually hurts.
        assertFalse(ZinelyV2Motion(reduceMotion = true).allowsContinuousMotion)
        assertTrue(ZinelyV2Motion(reduceMotion = false).allowsContinuousMotion)
    }

    @Test
    fun `frozen durations and easings pass through untouched when motion is allowed`() {
        val motion = ZinelyV2Motion(reduceMotion = false)
        assertEquals(340, motion.durationMillis(340))
        assertEquals(ZinelyV2Settle, motion.settle<Float>(340).easing)
        assertEquals(ZinelyV2Standard, motion.standard<Float>(200).easing)
        assertEquals(200, motion.standard<Float>(200).durationMillis)
    }
}
