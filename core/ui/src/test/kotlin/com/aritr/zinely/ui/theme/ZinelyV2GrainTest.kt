package com.aritr.zinely.ui.theme

import java.io.File
import java.security.MessageDigest
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Pins the paper grain — the tile asset itself, and the frozen parameters it was generated from.
 *
 * The unusual thing here is that **the asset is the deliverable**, so the tests read the PNG rather
 * than any Kotlin constant. A grain tile can be wrong in ways a hash alone would not explain — it can
 * drift off its midpoint, lose its alpha channel, or carry a corrupted edge — and each of those is a
 * material defect that would only surface as "the paper looks odd" months later. So the hash pins
 * *identity* and the measurements pin *correctness*, and the two failures read differently: a changed
 * hash with intact measurements is a regenerated tile, while a failed measurement is a broken one.
 *
 * **What these tests deliberately do not check: seamlessness.** An earlier version asserted it by
 * comparing the wrap-edge pixel delta against the interior delta. That test **could not fail** — pure
 * white noise passed it, as did the tile with a column corrupted — because at `baseFrequency` 0.9 the
 * lattice cell is ~1.1px, adjacent pixels are effectively uncorrelated (measured neighbour r ≈ −0.07),
 * and no statistic computed on this PNG distinguishes a stitched tile from an unstitched one.
 * `stitchTiles` is verified where it is checkable — at the committed generator, `tools/grain/gen_grain.py`,
 * which asserts that the turbulence *function* is continuous across the tile boundary (1.8e-6 in the
 * function's own units, against 0.38 with stitching off) and that its control discriminates. Note those
 * are function units, not pixel deltas. That verification is recorded there and in ADR-076 rather than
 * imitated here by an assertion that always passes.
 */
class ZinelyV2GrainTest {

    private val tile = File("src/main/res/drawable-nodpi/zinely_v2_grain.png")
    private val mockups = File("../../docs/design/mockups")

    private fun frozen(name: String) = File(mockups, name).readText().substringBefore("</style>")

    /** Every CSS rule body in the frozen file that actually paints the grain. */
    private fun grainRules(name: String): List<String> =
        Regex("""[^{}]*\{([^{}]*)}""").findAll(frozen(name))
            .map { it.groupValues[1] }
            .filter { it.contains("var(--grain)") }
            .toList()

    @Test
    fun `the tile is the exact asset its provenance claims`() {
        assertTrue("expected to run with :core:ui as the working directory — missing $tile", tile.isFile)
        val sha = MessageDigest.getInstance("SHA-256").digest(tile.readBytes())
            .joinToString("") { "%02x".format(it) }
        // Generated from the SVG 1.1 normative feTurbulence reference code at the frozen parameters:
        // fractalNoise, baseFrequency .9, numOctaves 2, stitchTiles stitch, seed 0, 140x140, then
        // feColorMatrix saturate 0, then linearRGB -> sRGB. Regenerating with the same inputs
        // reproduces this byte for byte; a different hash means the material changed and should read
        // that way in the diff.
        assertEquals("91da701d2e25d75a9e9744ae833944d8c66e235c6887a17e3169322f307d3d1e", sha)
    }

    @Test
    fun `the tile is 140 square, matching the frozen source`() {
        val img = ImageIO.read(tile)
        assertEquals(140, img.width)
        assertEquals(140, img.height)
        // The Library percent-encodes its data URI, so match the substring all three agree on.
        listOf("v2-library.html", "v2-bench.html", "v2-proof.html").forEach {
            assertTrue("$it authors the noise at 140x140", frozen(it).contains("140' height='140'"))
        }
    }

    @Test
    fun `the tile carries its own alpha, because saturate leaves the alpha channel alone`() {
        // feColorMatrix type="saturate" acts on RGB only, so feTurbulence's fourth channel survives
        // as per-pixel alpha. A tile flattened to opaque would blend visibly differently.
        val img = ImageIO.read(tile)
        assertTrue("the tile must have an alpha channel", img.colorModel.hasAlpha())
        val alphas = (0 until img.height).flatMap { y ->
            (0 until img.width).map { x -> (img.getRGB(x, y) ushr 24) and 0xFF }
        }
        assertTrue("alpha must vary per pixel, not be a constant", alphas.toSet().size > 32)
        // Alpha is never colour-managed, so unlike luma it sits on the *linear* midpoint.
        assertEquals("alpha centres on 0.5", 127.5, alphas.average(), 2.0)
    }

    @Test
    fun `the noise is grey and centred on the sRGB encoding of linear mid-grey`() {
        val img = ImageIO.read(tile)
        var sum = 0L
        var count = 0
        for (y in 0 until img.height) {
            for (x in 0 until img.width) {
                val p = img.getRGB(x, y)
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                assertEquals("saturate 0 means R == G == B at ($x,$y)", r, g)
                assertEquals("saturate 0 means R == G == B at ($x,$y)", g, b)
                sum += r
                count++
            }
        }
        val mean = sum.toDouble() / count
        // fractalNoise maps turbulence to (t+1)/2, so a large sample centres on 0.5 — but in
        // *linear* light, because `color-interpolation-filters` defaults to linearRGB and the frozen
        // files do not override it. Encoded to sRGB that is 0.735, i.e. ~187. A tile centred on 128
        // would be ~45% too dark and, since soft-light lightens above the midpoint and darkens below
        // it, would invert the material's effect on paper rather than merely weakening it.
        val srgbMid = 255.0 * (1.055 * Math.pow(0.5, 1 / 2.4) - 0.055)
        assertEquals("expected the tile to centre on sRGB(linear 0.5)", srgbMid, mean, 2.0)
    }

    @Test
    fun `no row or column is degenerate, which is what a corrupted edge would look like`() {
        // The failure this can actually catch: a clipped, blanked or duplicated edge line — the way
        // a mis-generated or mis-cropped tile shows up. Deliberately NOT a seam test; see the class
        // KDoc for why a seam is not detectable from this PNG at all.
        val img = ImageIO.read(tile)
        fun lum(x: Int, y: Int) = (img.getRGB(x, y) shr 16) and 0xFF
        val cols = (0 until img.width).map { x -> (0 until img.height).map { lum(x, it) } }
        val rows = (0 until img.height).map { y -> (0 until img.width).map { lum(it, y) } }

        (cols + rows).forEachIndexed { i, line ->
            val mean = line.average()
            val spread = line.max() - line.min()
            assertTrue("line $i is flat (spread $spread) — a blanked or clipped edge", spread > 40)
            assertTrue("line $i drifts off the tile's midpoint (mean $mean)", abs(mean - 186.8) < 8.0)
        }
        // Duplication is the other way an edge goes wrong, and a per-line statistic cannot see it,
        // so it gets its own assertion rather than a mention in the message above.
        listOf(cols, rows).forEach { lines ->
            lines.zipWithNext().forEachIndexed { i, (a, b) ->
                assertTrue("lines $i and ${i + 1} are identical — a duplicated edge", a != b)
            }
        }
    }

    @Test
    fun `every frozen grain use blends soft-light, which is why only one blend mode is modelled`() {
        // Scoped to rules that actually draw the grain. V2 blends one other thing — `.band` at
        // v2-library.html:67, the cover's colour band, on `multiply` — and that is a cover component
        // for Phase B, not a property of the paper material. An earlier draft of this test read every
        // blend declaration in the trilogy and failed on exactly that.
        val blends = listOf("v2-library.html", "v2-bench.html", "v2-proof.html")
            .flatMap { grainRules(it) }
            .mapNotNull { Regex("""[\w-]*blend-mode\s*:\s*([\w-]+)""").find(it)?.groupValues?.get(1) }
        // 3 Library (.cover, .sheet-ill, .book-ill) + 3 Bench + 4 Proof.
        assertEquals("all ten frozen grain-drawing rules", 10, blends.size)
        assertEquals("soft-light is the only way V2 blends grain", setOf("soft-light"), blends.toSet())
    }

    @Test
    fun `the frozen tile sizes are six values on no ladder, which is why none is tokenised`() {
        val sizes = listOf("v2-library.html", "v2-bench.html", "v2-proof.html")
            .flatMap { grainRules(it) }
            .mapNotNull { Regex("""background-size\s*:\s*(\d+)px""").find(it)?.groupValues?.get(1) }
            .map { it.toInt() }
        assertEquals(10, sizes.size)
        assertEquals(setOf(70, 90, 120, 140, 150, 180), sizes.toSet())
        // The D-007 ruling in one assertion: if these ever fall onto a shared scale, the case for
        // leaving them at the call site weakens and this test should be the thing that says so.
        assertTrue("no common ratio — these are per-surface choices", sizes.toSet().size == 6)
    }

    @Test
    fun `D-013 is still open — the two grain definitions bake different alpha`() {
        // The Bench and Proof bake opacity .5 into the SVG rect; the Library bakes none. Everything
        // downstream — the 4-to-7x effective-strength gap between a Library cover and a Bench page —
        // follows from this one difference, so it is the thing to watch rather than the CSS opacities.
        assertTrue("bench bakes .5", frozen("v2-bench.html").contains("filter='url(%23n)' opacity='.5'"))
        assertTrue("proof bakes .5", frozen("v2-proof.html").contains("filter='url(%23n)' opacity='.5'"))
        val library = frozen("v2-library.html")
        assertTrue("the library declares grain", library.contains("--grain:"))
        // Fires for two of the three possible rulings (Library gains .5, or Bench/Proof lose theirs).
        // It cannot fire for the third — "the Library is deliberate, change nothing" — which is a
        // known limit of this tripwire, not an oversight: that ruling closes D-013 by decision alone
        // and must be closed in the register by hand.
        assertTrue(
            "the library's rect carries no opacity — if it gains one, D-013 has been answered",
            !library.contains("opacity='.5'") && !library.contains("opacity%3D"),
        )
    }
}
