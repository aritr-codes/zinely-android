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
 * The **B3** Act 2 golden (M5, [ADR-051]/[ADR-052]; batch spec §B3.5) — the honest print recipe at rest
 * on the Print act: the four recipe rows (Scale/Orientation warn-styled, Paper + Change, Single-sided),
 * the single-sided note, and the two-button export row (Save PDF · Share; the frozen Print button is
 * dropped per ADR-052). Same two-proof shape as the sibling goldens: a behavioural pixel assertion (the
 * field cards + the coral emphasis must actually paint) plus a committed Roborazzi golden, phone +
 * tablet, light + dark. The paper/share `ZSheet`s are Dialog-hosted (invisible to the decor-view
 * rasterizer, per the M1 ZSheet golden note), so this captures the recipe at rest, sheets closed.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProofPrintGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }

    private fun printBitmap(darkTheme: Boolean, w: Dp, h: Dp): Bitmap {
        composeRule.setContent {
            ZinelyTheme(darkTheme = darkTheme) {
                Box(Modifier.size(w, h)) {
                    ProofScreen(zineName = "Corner Store Poems", onBack = {})
                }
            }
        }
        composeRule.onNodeWithTag(ProofPrimaryTestTag).performClick() // Sheet → Print
        composeRule.waitForIdle()
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

    private fun assertRecipePaints(bmp: Bitmap, field: Color, coralText: Color) {
        // The recipe rows are --field cards.
        assertTrue("recipe field cards did not paint", bmp.countColour(field.toArgb()) > 1000)
        // The warn emphasis ("100% · Actual size", "Landscape") + "Change" speak coral-text.
        assertTrue("coral-text emphasis did not paint", bmp.countColour(coralText.toArgb()) > 40)
    }

    @Test
    fun proof_print_light_phone() {
        val bmp = printBitmap(darkTheme = false, w = 420.dp, h = 820.dp)
        assertRecipePaints(bmp, field = Color(0xFFFBF8F1), coralText = Color(0xFFA63C22))
        bmp.captureRoboImage("$GOLDEN_DIR/proof_print_light_phone.png", aa())
    }

    @Test
    fun proof_print_dark_phone() {
        val bmp = printBitmap(darkTheme = true, w = 420.dp, h = 820.dp)
        assertRecipePaints(bmp, field = Color(0xFF2B2A28), coralText = Color(0xFFE76F51))
        bmp.captureRoboImage("$GOLDEN_DIR/proof_print_dark_phone.png", aa())
    }

    @Test
    fun proof_print_light_tablet() {
        val bmp = printBitmap(darkTheme = false, w = 820.dp, h = 1100.dp)
        assertRecipePaints(bmp, field = Color(0xFFFBF8F1), coralText = Color(0xFFA63C22))
        bmp.captureRoboImage("$GOLDEN_DIR/proof_print_light_tablet.png", aa())
    }

    @Test
    fun proof_print_dark_tablet() {
        val bmp = printBitmap(darkTheme = true, w = 820.dp, h = 1100.dp)
        assertRecipePaints(bmp, field = Color(0xFF2B2A28), coralText = Color(0xFFE76F51))
        bmp.captureRoboImage("$GOLDEN_DIR/proof_print_dark_tablet.png", aa())
    }
}
