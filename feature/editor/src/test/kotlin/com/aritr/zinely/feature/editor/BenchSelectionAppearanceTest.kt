package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.zinelyV2LightColors
import com.aritr.zinely.ui.theme.zinelyV21LightColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The frozen `.sel` outline and `.handle` mark, measured as **drawn pixels** rather than as the constants
 * that produced them ([ADR-091](../../../../../../../../../docs/DECISIONS.md#adr-091) rows 2.3–2.6).
 *
 * The values under test all changed in C2a — `--coral-strong`/2dp/square became `--matcha`/1.5dp/circle —
 * and the reason they could change safely is a **contrast** claim, not a colour preference. So the
 * contrast is what the first test asserts, computed here rather than restated from the ADR.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BenchSelectionAppearanceTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val HOST = "appearance-host"

        /** Neither paper, matcha nor white — so every layer the frozen handle stacks is distinguishable. */
        val BACKDROP = Color(0xFF102030)
    }

    /** The host, cropped out of the rasterised window — `rasterizeToBitmap` is a `View` extension. */
    private fun hostBitmap(): Bitmap {
        composeRule.waitForIdle()
        val full = composeRule.activity.window.decorView.rasterizeToBitmap()
        val r = composeRule.onNodeWithTag(HOST).fetchSemanticsNode().boundsInWindow
        return Bitmap.createBitmap(full, r.left.toInt(), r.top.toInt(), r.width.toInt(), r.height.toInt())
    }

    private fun lum(argb: Int): Double {
        fun ch(shift: Int): Double {
            val s = ((argb shr shift) and 0xFF) / 255.0
            return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * ch(16) + 0.7152 * ch(8) + 0.0722 * ch(0)
    }

    private fun contrast(a: Int, b: Int): Double {
        val la = lum(a)
        val lb = lum(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    /**
     * **The claim that licensed the re-skin.** `SelectionChrome`'s KDoc has always asserted WCAG 1.4.11
     * (non-text contrast ≥3:1) and C2a changed both the hue and the width it asserted that with. This
     * recomputes it from the tokens the product actually resolves — not from the ADR's table — and holds
     * it in **both** themes, which is only true because [BenchSheetIsland] made the sheet a light-theme
     * island (ADR-090 / OD-12). Before that amendment the dark-theme figure was a different number.
     *
     * Mutation: point the outline back at a token that fails on paper (e.g. `--paper-edge`, 1.11:1) and
     * this fails while every geometry test stays green.
     */
    @Test
    fun `the outline holds WCAG 1_4_11 against the sheet, in both themes`() {
        val onPaper = zinelyV2LightColors()
        val c = contrast(onPaper.matcha.toArgb(), onPaper.paper.toArgb())
        assertTrue(
            "the frozen --matcha outline reaches only ${"%.2f".format(c)}:1 on the sheet",
            c >= 3.0,
        )
        // The island is what makes one measurement answer for both themes: the on-paper tokens a dark-theme
        // editor resolves inside the sheet ARE these. If that ever stops being true, this equality breaks
        // before any contrast figure does.
        val darkRoomOnPaper = BenchStudio.sheetIsland(com.aritr.zinely.ui.theme.zinelyV2DarkColors())
        assertEquals("dark-theme on-paper matcha == light-theme matcha", onPaper.matcha, darkRoomOnPaper.matcha)
        assertEquals("dark-theme on-paper paper == light-theme paper", onPaper.paper, darkRoomOnPaper.paper)
    }

    /**
     * Row 2.3 — the outline is drawn **outside** the element's box. Composed at a known transform, the
     * pixel on the box edge must be clear of the stroke and the stroke must be found beyond it.
     *
     * Mutation: `SelectionOutlineInsetDp` 7 → 0 puts the stroke on the edge itself and the first assertion
     * (edge is clean) fails.
     */
    @Test
    fun `the outline stands off the element's edge rather than sitting on it`() {
        val t = Transform(40.0, 40.0, 60.0, 60.0, rotationDegrees = 0.0)
        composeRule.setContent {
            ZinelyTheme(darkTheme = false) {
                BenchSheetIsland(modifier = Modifier.size(200.dp).testTag(HOST)) {
                    Box(Modifier.fillMaxSize().background(ZinelyTheme.v2Colors.paper))
                    SelectionChrome(
                        transforms = listOf(t),
                        screenPxPerPt = 1f,
                        pageOffset = PtPoint(0.0, 0.0),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        val b: Bitmap = hostBitmap()
        val paper = zinelyV2LightColors().paper.toArgb()
        val midY = 70

        // On the box's left edge (x = 40): nothing drawn there any more.
        assertEquals("the box edge itself carries no stroke", paper, b.getPixel(40, midY))
        // Seven px outside it: the stroke. Scanned across a small band because a 1.5dp stroke straddles
        // pixel boundaries and asserting one exact column would be asserting the rasteriser, not the spec.
        val band = (31..35).map { b.getPixel(it, midY) }
        assertTrue(
            "no stroke found 7px outside the box — saw ${band.map { Integer.toHexString(it) }}",
            band.any { it != paper },
        )
    }

    /**
     * The mark is a **rounded square** in `ink`, and the halo is still there. Both are what changed in
     * V2.1, and both are what a constants test would have missed.
     *
     * ### The shape assertion inverted, and that is the point
     *
     * V2's mark was a 13dp **circle** and the old version of this test proved it by sampling the diagonal
     * where a square would still paint. V2.1's `.hnd` is a 9px square with `border-radius:2px`
     * (`v21-bench.html:148-149`), so the same sample now proves the opposite thing, and the test would
     * have gone green in the wrong direction if it had only had its numbers updated.
     *
     * ### The halo is retained against the freeze, deliberately
     *
     * `v21-bench.html` specifies no `box-shadow` on `.hnd`. It is kept under
     * [ADR-102 §12.6 row 5](../../../../../../../../../docs/DECISIONS.md#adr-102-p1-corrections), because
     * a handle sits on the **user's photograph** where no token can promise a ratio, and IA §C.4 requires
     * a dual-tone stroke holding 3:1 over any image. This test is the only assertion that fails if a
     * later "fidelity" pass deletes it.
     */
    @Test
    @Config(qualifiers = "xhdpi")
    fun `the handle mark is a rounded square carrying the retained halo`() {
        composeRule.setContent {
            ZinelyTheme(darkTheme = false) {
                // The mark alone, over a colour that is neither paper, ink nor white — so every layer
                // the frozen handle stacks is distinguishable from the backdrop it covers.
                Box(
                    modifier = Modifier.size(40.dp).background(BACKDROP).testTag(HOST),
                    contentAlignment = Alignment.Center,
                ) {
                    BenchHandleMark(color = ZinelyTheme.v21Colors.ink)
                }
            }
        }
        val b: Bitmap = hostBitmap()
        val cx = b.width / 2
        val cy = b.height / 2

        val paper = zinelyV21LightColors().paper.toArgb()
        assertEquals("the mark's centre is --paper filled", paper, b.getPixel(cx, cy))

        // The halo band runs from the mark's edge (9dp ÷ 2) to the envelope's (12dp ÷ 2) — only 1.5dp
        // wide, which at density 1 is a 1.5px band every sample of which is anti-aliased against one of
        // its two neighbours. Hence @Config(xhdpi): at 2× the band is 3px and its middle is clean. The
        // radius is derived from the measured bitmap rather than hard-coded, so the sample follows.
        // Sampled on an AXIS, where a square's edge and a circle's are furthest from disagreeing.
        val pxPerDp = b.width / 40f
        val haloMidRadius =
            Math.round((HandleDiameterDp.value / 2 + HandleHaloDiameterDp.value / 2) / 2 * pxPerDp)
        // ...is inside the 16dp halo envelope but outside the 13dp mark ⇒ the halo, and
        // the halo is translucent, so the pixel that lands is `rgba(255,255,255,.7)` composited over the
        // backdrop. Asserting that exact composite rather than "looks whitish" is what makes the frozen
        // .7 load-bearing: at .5 or 1.0 this fails, and a brightness threshold would pass at both.
        val haloPixel = b.getPixel(cx + haloMidRadius, cy)
        val expected = (0..2).map { i ->
            val shift = 16 - 8 * i
            val backdrop = (BACKDROP.toArgb() shr shift) and 0xFF
            Math.round(0.7f * 255 + 0.3f * backdrop)
        }
        val actual = listOf((haloPixel shr 16) and 0xFF, (haloPixel shr 8) and 0xFF, haloPixel and 0xFF)
        val worst = expected.zip(actual).maxOf { (e, a) -> Math.abs(e - a) }
        assertTrue(
            "the halo is not rgba(255,255,255,.7) over the backdrop — expected $expected, got $actual",
            worst <= 2,
        )

        // Is the **mark** square? Sampled on its diagonal at 3.5dp from centre — the one place the two
        // candidate shapes disagree, and the assertion runs the opposite way to V2's.
        //
        //  - The frozen 9dp rounded square (radius 2) COVERS (3.5, 3.5): its corner arc is centred at
        //    (2.5, 2.5) and the point is 1.41 from that centre, between the arc's inner radius (2 − 1.6
        //    = 0.4) and its outer (2) — so the pixel is the **border**, i.e. `ink`.
        //  - A 9dp circle has radius 4.5, and (3.5, 3.5) is 4.95 away — OUTSIDE it, in the halo.
        //
        // So this fails if the mark reverts to a circle, and it fails if the border colour reverts to
        // anything but ink. Sampling the halo envelope's corner instead would prove nothing: the halo
        // follows the mark's shape, which is exactly how an earlier draft of this test passed with the
        // wrong shape deliberately reinstated.
        val diag = Math.round(3.5f * pxPerDp)
        val onDiagonal = b.getPixel(cx + diag, cy + diag)
        // Near-match, not equality: a 1.6dp arc lands on fractional coordinates and anti-aliases against
        // both of its neighbours, so a diagonal sample may never be a fully-covered ink pixel. The rest of
        // the suite moved to ±24 for exactly this reason; the discriminator survives it, because the two
        // candidates here are nowhere near 24 apart. V2.1 light `ink` is `#33261C`; the halo composites to
        // near-white, and the backdrop `#102030` sits 35 away on its worst channel.
        val ink = zinelyV21LightColors().ink.toArgb()
        val inkDelta = (0..2).maxOf { i ->
            val shift = 16 - 8 * i
            Math.abs(((onDiagonal shr shift) and 0xFF) - ((ink shr shift) and 0xFF))
        }
        assertTrue(
            "the mark is not the frozen rounded square in ink — its diagonal at 3.5dp should be border, " +
                "saw #${Integer.toHexString(onDiagonal)} against #${Integer.toHexString(ink)}",
            inkDelta <= 24,
        )
        assertNotEquals(
            "a circle would leave the diagonal in the halo — the mark has reverted to V2's shape",
            haloPixel,
            onDiagonal,
        )
    }
}
