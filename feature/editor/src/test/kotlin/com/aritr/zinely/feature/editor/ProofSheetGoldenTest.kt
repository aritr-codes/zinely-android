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
 * The **B2** Act 1 golden (M5, [ADR-051]; batch spec §B2.5) — the imposed sheet at rest on the Sheet
 * act: paper, dead-band, engine-ordered cells, creases, the one coral cut + label, legend, cover cards.
 * Same two-proof shape as [ProofScaffoldGoldenTest]: a behavioural pixel assertion (mode-independent —
 * the paper ground and the coral cut must actually paint) plus a committed Roborazzi golden recorded on
 * the pinned CI image, at phone + tablet width, light + dark.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProofSheetGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }

    private fun sheetBitmap(darkTheme: Boolean, w: Dp, h: Dp): Bitmap {
        composeRule.setContent {
            ZinelyTheme(darkTheme = darkTheme) {
                Box(Modifier.size(w, h)) {
                    ProofScreen(zineName = "Corner Store Poems", onBack = {})
                }
            }
        }
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

    private fun assertSheetPaints(bmp: Bitmap, paper: Color) {
        // The paper sheet is the imposed-sheet ground.
        assertTrue("paper sheet did not paint", bmp.countColour(paper.toArgb()) > 1000)
        // The one coral cut + "ONE CUT" label (--coral-strong) must actually paint.
        assertTrue("coral cut did not paint", bmp.countColour(Color(0xFFC64E34).toArgb()) > 50)
    }

    @Test
    fun proof_sheet_light_phone() {
        val bmp = sheetBitmap(darkTheme = false, w = 420.dp, h = 820.dp)
        assertSheetPaints(bmp, Color(0xFFF4EFE6))
        bmp.captureRoboImage("$GOLDEN_DIR/proof_sheet_light_phone.png", aa())
    }

    @Test
    fun proof_sheet_dark_phone() {
        val bmp = sheetBitmap(darkTheme = true, w = 420.dp, h = 820.dp)
        assertSheetPaints(bmp, Color(0xFFEDE6D9))
        bmp.captureRoboImage("$GOLDEN_DIR/proof_sheet_dark_phone.png", aa())
    }

    @Test
    fun proof_sheet_light_tablet() {
        val bmp = sheetBitmap(darkTheme = false, w = 820.dp, h = 1100.dp)
        assertSheetPaints(bmp, Color(0xFFF4EFE6))
        bmp.captureRoboImage("$GOLDEN_DIR/proof_sheet_light_tablet.png", aa())
    }

    @Test
    fun proof_sheet_dark_tablet() {
        val bmp = sheetBitmap(darkTheme = true, w = 820.dp, h = 1100.dp)
        assertSheetPaints(bmp, Color(0xFFEDE6D9))
        bmp.captureRoboImage("$GOLDEN_DIR/proof_sheet_dark_tablet.png", aa())
    }
}
