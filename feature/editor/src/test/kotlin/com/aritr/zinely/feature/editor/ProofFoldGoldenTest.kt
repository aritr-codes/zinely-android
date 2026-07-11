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
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import kotlin.math.roundToInt

/**
 * The **B4** Act 3 golden (M5, [ADR-051]; batch spec §B4.5) — the signature climax. Two captures: the
 * five-step fold **guide** at rest on step 1 (the schematic diagram + the live-region caption + the
 * prev/dots/next nav), and the **finished book** at the end of the staged reveal (cover shut, book
 * settled, words + shelf-line present). The finished state is also the reduced-motion end state (every
 * beat collapsed to its final value), so this doubles as the reduced-motion golden. Same two-proof shape
 * as the sibling goldens: a behavioural pixel assertion (the paper artefact must actually paint) plus a
 * committed Roborazzi golden, phone + tablet, light + dark.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProofFoldGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }

    private fun mount(darkTheme: Boolean, w: Dp, h: Dp) {
        composeRule.setContent {
            ZinelyTheme(darkTheme = darkTheme) {
                Box(Modifier.size(w, h)) {
                    ProofScreen(zineName = "Corner Store Poems", onBack = {})
                }
            }
        }
        composeRule.onNodeWithTag(ProofPrimaryTestTag).performClick() // Sheet → Print
        composeRule.onNodeWithTag(ProofPrimaryTestTag).performClick() // Print → Fold
        composeRule.waitForIdle()
    }

    private fun crop(): Bitmap {
        val bounds = composeRule.onNodeWithTag(ProofScreenTestTag).fetchSemanticsNode().boundsInRoot
        val full = composeRule.activity.window.decorView.rasterizeToBitmap()
        val x = bounds.left.roundToInt().coerceAtLeast(0)
        val y = bounds.top.roundToInt().coerceAtLeast(0)
        val bw = bounds.width.roundToInt().coerceAtMost(full.width - x)
        val bh = bounds.height.roundToInt().coerceAtMost(full.height - y)
        return Bitmap.createBitmap(full, x, y, bw, bh)
    }

    private fun Bitmap.countColour(argb: Int): Int {
        var n = 0
        for (yy in 0 until height) for (xx in 0 until width) if (getPixel(xx, yy) == argb) n++
        return n
    }

    private fun guideBitmap(darkTheme: Boolean, w: Dp, h: Dp): Bitmap {
        mount(darkTheme, w, h)
        return crop()
    }

    private fun finishedBitmap(darkTheme: Boolean, w: Dp, h: Dp): Bitmap {
        // Force reduced motion so the reveal collapses to its complete end state at once: Robolectric's
        // clock does not auto-advance the climax `delay()` beats, so the full-motion capture would freeze
        // mid-reveal. This end state IS the reduced-motion golden (book closed + words + shelf-line, per
        // the frozen reduced-motion `finishFold`).
        android.provider.Settings.Global.putFloat(
            org.robolectric.RuntimeEnvironment.getApplication().contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )
        mount(darkTheme, w, h)
        repeat(4) { composeRule.onNodeWithTag(ProofStepNextTestTag).performScrollTo().performClick() }
        composeRule.onNodeWithTag(ProofPrimaryTestTag).performClick() // finish
        composeRule.waitForIdle()
        return crop()
    }

    // ---- the fold guide (step 1) --------------------------------------------------------------

    @Test
    fun proof_fold_light_phone() {
        val bmp = guideBitmap(darkTheme = false, w = 420.dp, h = 820.dp)
        // The step-1 diagram draws the paper sheet (the guide actually rendered).
        assertTrue("fold diagram sheet did not paint", bmp.countColour(Color(0xFFF4EFE6).toArgb()) > 500)
        bmp.captureRoboImage("$GOLDEN_DIR/proof_fold_light_phone.png", aa())
    }

    @Test
    fun proof_fold_dark_phone() {
        val bmp = guideBitmap(darkTheme = true, w = 420.dp, h = 820.dp)
        assertTrue("fold diagram sheet did not paint", bmp.countColour(Color(0xFFEDE6D9).toArgb()) > 500)
        bmp.captureRoboImage("$GOLDEN_DIR/proof_fold_dark_phone.png", aa())
    }

    @Test
    fun proof_fold_light_tablet() {
        val bmp = guideBitmap(darkTheme = false, w = 820.dp, h = 1100.dp)
        assertTrue("fold diagram sheet did not paint", bmp.countColour(Color(0xFFF4EFE6).toArgb()) > 500)
        bmp.captureRoboImage("$GOLDEN_DIR/proof_fold_light_tablet.png", aa())
    }

    @Test
    fun proof_fold_dark_tablet() {
        val bmp = guideBitmap(darkTheme = true, w = 820.dp, h = 1100.dp)
        assertTrue("fold diagram sheet did not paint", bmp.countColour(Color(0xFFEDE6D9).toArgb()) > 500)
        bmp.captureRoboImage("$GOLDEN_DIR/proof_fold_dark_tablet.png", aa())
    }

    // ---- the finished book (the climax end state = the reduced-motion state) ------------------

    @Test
    fun proof_finished_light_phone() {
        val bmp = finishedBitmap(darkTheme = false, w = 420.dp, h = 820.dp)
        // The book cover (paper) has become a book.
        assertTrue("finished book cover did not paint", bmp.countColour(Color(0xFFF4EFE6).toArgb()) > 1000)
        bmp.captureRoboImage("$GOLDEN_DIR/proof_finished_light_phone.png", aa())
    }

    @Test
    fun proof_finished_dark_phone() {
        val bmp = finishedBitmap(darkTheme = true, w = 420.dp, h = 820.dp)
        assertTrue("finished book cover did not paint", bmp.countColour(Color(0xFFEDE6D9).toArgb()) > 1000)
        bmp.captureRoboImage("$GOLDEN_DIR/proof_finished_dark_phone.png", aa())
    }
}
