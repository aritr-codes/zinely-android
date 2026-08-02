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
import org.junit.Assert.assertEquals
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
     * Row 2.5 — the mark is a **circle**, and the halo is present. Both are what changed; both are what a
     * constants test would have missed. The corner of the mark's bounding box must be halo/paper (a circle
     * does not reach its corners) while its centre carries the paper fill.
     *
     * Mutation: `CircleShape` → the old `RoundedCornerShape(4.dp)` fills the corner with the border colour
     * and the first assertion fails.
     */
    @Test
    @Config(qualifiers = "xhdpi")
    fun `the handle mark is a circle carrying the frozen halo`() {
        composeRule.setContent {
            ZinelyTheme(darkTheme = false) {
                // The mark alone, over a colour that is neither paper, matcha nor white — so every layer
                // the frozen handle stacks is distinguishable from the backdrop it covers.
                Box(
                    modifier = Modifier.size(40.dp).background(BACKDROP).testTag(HOST),
                    contentAlignment = Alignment.Center,
                ) {
                    BenchHandleMark(color = ZinelyTheme.v2Colors.matcha)
                }
            }
        }
        val b: Bitmap = hostBitmap()
        val cx = b.width / 2
        val cy = b.height / 2

        val paper = zinelyV2LightColors().paper.toArgb()
        assertEquals("the mark's centre is --paper filled", paper, b.getPixel(cx, cy))

        // The halo band runs from the mark's edge (13dp ÷ 2) to the envelope's (16dp ÷ 2) — only 1.5dp
        // wide, which at density 1 is a 1.5px band every sample of which is anti-aliased against one of
        // its two neighbours. Hence @Config(xhdpi): at 2× the band is 3px and its middle is clean. The
        // radius is derived from the measured bitmap rather than hard-coded, so the sample follows.
        val pxPerDp = b.width / 40f
        val haloMidRadius = Math.round((13f / 2 + 16f / 2) / 2 * pxPerDp)
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

        // Is the **mark** round? Sampled on its diagonal at 5dp from centre — the one place the two
        // candidate shapes disagree. A 13dp circle has radius 6.5, and (5,5) is 7.07 away, so it falls
        // OUTSIDE the mark, in the halo. The old 13dp rounded square (corner radius 4) still covers that
        // point — its corner arc is centred at (2.5,2.5) and (5,5) is only 3.54 from it, inside — so it
        // would paint matcha there. Sampling the halo envelope's corner instead would prove nothing: the
        // halo is round under either mark, which is exactly how an earlier draft of this test passed with
        // the square deliberately reinstated.
        val diag = Math.round(5f * pxPerDp)
        val onDiagonal = b.getPixel(cx + diag, cy + diag)
        assertEquals(
            "the mark is not a circle — its diagonal at 5dp is painted, so a square still covers it",
            haloPixel,
            onDiagonal,
        )
    }
}
