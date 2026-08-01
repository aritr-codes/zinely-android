package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * **What is drawn on the page must be readable on the page** — in both themes, as rendered, not as
 * intended.
 *
 * ### Why this test exists, and why a golden could not do its job
 *
 * C1 ([ADR-090](../../../../../../../../../docs/DECISIONS.md#adr-090)) repainted the editor's sheet with
 * the **V2** `paper` token and left [EditorEmptyState] painting the **V1** `ink` token. The two palettes
 * disagree about dark deliberately: V1 keeps the sheet *lit* in dark (`paper` `#EDE6D9`) so a near-black
 * `ink` (`#23201C`) is correct on it, while V2 *dims* the sheet (`paper` `#2F2A22`) and answers it with a
 * warm-cream `ink` (`#ECE4D3`). Each system is coherent alone. Mixed, the dark theme drew `#23201C` on
 * `#2F2A22` — roughly **1.1:1**, an invitation headline the user simply cannot see.
 *
 * **Every existing check passed.** The unit tests assert tokens against the frozen CSS, and both tokens
 * were individually right. `editor_screen_dark.png` was **re-recorded by C1 with the defect already in
 * it**, so it verified green against itself — the corpus already knows this failure as
 * *goldens-in-record-mode-aren't-assertions*. It was found by a **device Pass 2**, by looking at the
 * screen.
 *
 * A token-level assertion cannot replace that: comparing V2 `ink` to V2 `paper` is a statement about a
 * pairing that was never broken, and it would have stayed green throughout the defect. The only assertion
 * that fails when this recurs is one about the **pixels that actually land** — which composable read
 * which palette is then irrelevant, because the probe measures the result.
 *
 * ### How the probe reads
 *
 * The invitation is drawn over the real page surface, rasterised, cropped to the headline's own box, and
 * reduced to two numbers whose WCAG contrast is the screen's actual legibility: the sheet and the ink.
 * *Which* two pixels those are is the whole difficulty, and [inkOnPaperContrast] carries the reasoning —
 * two earlier drafts of this probe chose them wrongly and passed with the defect deliberately reinstated.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorPageLegibilityProbeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun host(darkTheme: Boolean, content: @Composable () -> Unit) {
        composeRule.setContent {
            ZinelyTheme(darkTheme = darkTheme) {
                Box(
                    modifier = Modifier.size(PAGE_W.dp, PAGE_H.dp).benchPageSurface(),
                    contentAlignment = Alignment.Center,
                ) { content() }
            }
        }
        composeRule.waitForIdle()
    }

    private fun hostBitmap(): Bitmap {
        val view = composeRule.activity.window.decorView
        composeRule.waitForIdle()
        return view.rasterizeToBitmap()
    }

    /** WCAG relative luminance of a packed ARGB pixel. */
    private fun lum(argb: Int): Double {
        fun ch(v: Int): Double {
            val s = v / 255.0
            return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * ch((argb shr 16) and 0xFF) +
            0.7152 * ch((argb shr 8) and 0xFF) +
            0.0722 * ch(argb and 0xFF)
    }

    private fun contrast(a: Double, b: Double): Double {
        val hi = maxOf(a, b)
        val lo = minOf(a, b)
        return (hi + 0.05) / (lo + 0.05)
    }

    /**
     * The strongest contrast the pixels of **[text]'s own box** reach against the sheet under them.
     *
     * **Two earlier cuts of this probe passed with the defect deliberately reinstated**, each because
     * something other than the text supplied the contrast:
     *
     * 1. Reading the page as a whole, the decorative sticker cluster — an opaque blob, perfectly visible
     *    in either theme — answered the assertion. Fixed by cropping to the headline's own bounds.
     * 2. Reading the *extreme* pixel in that crop, the headline's trailing **emoji** answered it: emoji
     *    rasterise in their own colours (a bright `#FFF5A1` here), which no theme token controls.
     *
     * So neither the modal pixel alone nor the extreme pixel alone is the ink. The sheet is the **modal**
     * pixel (paper outweighs glyphs in any line box), and the ink is the **modal non-sheet** pixel — the
     * glyph core, since one text colour repeats across every letter while an emoji's colours are spread
     * thin across a gradient. Anti-aliasing only moves ink *towards* paper, so the modal ink pixel is the
     * glyph's true colour and not an edge artefact. Grain is excluded by [GRAIN_TOLERANCE].
     */
    private fun inkOnPaperContrast(bmp: Bitmap, text: String): Double {
        val b = composeRule.onNodeWithText(text).fetchSemanticsNode().boundsInWindow
        val x0 = b.left.toInt().coerceIn(0, bmp.width - 1)
        val y0 = b.top.toInt().coerceIn(0, bmp.height - 1)
        val x1 = b.right.toInt().coerceIn(x0 + 1, bmp.width)
        val y1 = b.bottom.toInt().coerceIn(y0 + 1, bmp.height)

        val counts = HashMap<Int, Int>()
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                val c = bmp.getPixel(x, y) or (0xFF shl 24)
                counts[c] = (counts[c] ?: 0) + 1
            }
        }
        val paper = counts.maxByOrNull { it.value }!!.key
        val paperLum = lum(paper)
        fun dist(a: Int, b: Int): Int = maxOf(
            Math.abs(((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)),
            Math.abs(((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)),
            Math.abs((a and 0xFF) - (b and 0xFF)),
        )
        // No ink at all in the headline's own box means it stopped drawing — still a legibility failure, and
        // 1.0 reports it as one. Throwing here would swap the measured message for a KotlinNullPointerException.
        val ink = counts.filterKeys { dist(it, paper) > GRAIN_TOLERANCE }.maxByOrNull { it.value }?.key
            ?: return 1.0
        return contrast(lum(ink), paperLum)
    }

    /**
     * The light theme could not have caught the C1 defect — V1 and V2 light inks are both dark on a light
     * sheet, so the mixed pairing stayed legible and this test passes with the defect reinstated. It is here
     * as a guard against a light-theme regression, not as a mirror of the dark one.
     */
    @Test
    fun `the blank-page invitation is legible on the sheet in the light theme`() {
        host(darkTheme = false) { EditorEmptyState() }
        val c = inkOnPaperContrast(hostBitmap(), FirstPageInvitationHeadline)
        assertTrue("light: the invitation reaches only ${"%.2f".format(c)}:1 against the sheet", c >= 4.5)
    }

    @Test
    fun `the blank-page invitation is legible on the sheet in the dark theme`() {
        // THE regression. Before the fix this measured ~1.1:1 while every golden and unit test stayed green.
        host(darkTheme = true) { EditorEmptyState() }
        val c = inkOnPaperContrast(hostBitmap(), FirstPageInvitationHeadline)
        assertTrue("dark: the invitation reaches only ${"%.2f".format(c)}:1 against the sheet", c >= 4.5)
    }

    private companion object {
        /**
         * The frozen `.page` box, `229×324`, as the owner amended it under
         * [D-033](../../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-033-amendment). Transcribed rather
         * than derived because this probe only needs *a* realistic sheet to draw on — it measures colour, not
         * geometry, and `BenchStudioSurfaceTest` is where the number is asserted against the frozen file.
         */
        const val PAGE_W = 229
        const val PAGE_H = 324

        /**
         * How far a pixel may sit from the modal sheet colour and still be *sheet*. The baked grain
         * ([BenchStudio.PAGE_GRAIN_ALPHA]) dithers the paper by a few levels per channel, so the sheet is
         * not one colour but a cloud of them — measured here at ±3; 8 leaves headroom without reaching any
         * glyph, the nearest of which (V1 `ink` on V2 dark `paper`, the defect itself) is 12 away.
         */
        const val GRAIN_TOLERANCE = 8
    }
}
