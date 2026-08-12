package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import kotlin.math.roundToInt
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The Proof surface frame at rest, light + dark: the desk ground and the band's commit action must
 * actually paint. Two proofs in one, as [ZComponentGoldenTest] — a mode-independent pixel assertion that
 * runs green under a plain unit run, plus a Roborazzi golden recorded on the pinned CI image.
 *
 * **The baseline is current again.** For the length of [ADR-101](../../../../../../../docs/DECISIONS.md#adr-101)
 * P1–P5 the two PNGs deliberately showed a screen that no longer existed — the three-act climb, its progress
 * creases and a coral *"Print setup"* — and this header carried the notice, because a green golden test
 * whose baseline depicts a retired screen reads as coverage and is not. P6 re-recorded them against the
 * finished surface, so the notice is retired with the debt: **`proof_scaffold_{light,dark}.png` depict what
 * ships.**
 *
 * The asserted fill moved with the design, twice, and the second move is the one to remember. Until P2 the
 * band's one action was `coralStrong`; P2 made it a single prominent action in `stamp`; P6's sweep makes it
 * `leaf` — and unlike `stamp`, **`leaf` is not the same colour in both themes**, so the light and dark tests
 * assert different greens rather than sharing a constant. Asserting a colour the scene cannot contain is how
 * a pixel test starts lying, and so is asserting one constant for two scenes that differ.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProofScaffoldGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }

    private fun scaffoldBitmap(darkTheme: Boolean): Bitmap {
        composeRule.setContent {
            ZinelyTheme(darkTheme = darkTheme) {
                Box(Modifier.size(420.dp, 820.dp)) {
                    ProofScreen(onBack = {}, startDrawer = ProofDrawer.None)
                }
            }
        }
        composeRule.waitForIdle()
        val bounds = composeRule.onNodeWithTag(ProofScreenTestTag).fetchSemanticsNode().boundsInRoot
        val full = composeRule.activity.window.decorView.rasterizeToBitmap()
        val x = bounds.left.roundToInt().coerceAtLeast(0)
        val y = bounds.top.roundToInt().coerceAtLeast(0)
        val w = bounds.width.roundToInt().coerceAtMost(full.width - x)
        val h = bounds.height.roundToInt().coerceAtMost(full.height - y)
        return Bitmap.createBitmap(full, x, y, w, h)
    }

    private fun Bitmap.countColour(argb: Int): Int {
        var n = 0
        for (yy in 0 until height) for (xx in 0 until width) if (getPixel(xx, yy) == argb) n++
        return n
    }

    @Test
    fun proof_scaffold_light() {
        val bmp = scaffoldBitmap(darkTheme = false)
        // The V2.1 desk (#FBE9CE) is the frame's ground.
        assertTrue(
            "light desk did not paint in the proof scaffold",
            bmp.countColour(Color(0xFFFBE9CE).toArgb()) > 1000,
        )
        // The band's commit action ("Save PDF") and the `.ready` tick are `leaf` now — the IOU this
        // file's own header wrote against P6.
        assertTrue(
            "leaf commit action did not paint in the light scaffold",
            bmp.countColour(Color(0xFF4E7A3C).toArgb()) > 200,
        )
        bmp.captureRoboImage("$GOLDEN_DIR/proof_scaffold_light.png", aa())
    }

    @Test
    fun proof_scaffold_dark() {
        val bmp = scaffoldBitmap(darkTheme = true)
        // The V2.1 dark desk (#241E18). Unlike V1's `stamp`, `leaf` is **not** identical across themes —
        // it lightens to #8FAE6B so a fill stays legible on a dark desk — so this pair asserts two
        // different greens on purpose.
        assertTrue(
            "dark desk did not paint in the proof scaffold",
            bmp.countColour(Color(0xFF241E18).toArgb()) > 1000,
        )
        assertTrue(
            "leaf commit action did not paint in the dark scaffold",
            bmp.countColour(Color(0xFF8FAE6B).toArgb()) > 200,
        )
        bmp.captureRoboImage("$GOLDEN_DIR/proof_scaffold_dark.png", aa())
    }
}
