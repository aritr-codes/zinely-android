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
 * The **B1** empty-frame golden (M5, [ADR-051]; batch spec §B1.6) — the frozen 3-act scaffold at rest
 * on the Sheet act, light + dark. Same two-proof shape as [ZComponentGoldenTest]: a behavioural pixel
 * assertion (mode-independent, green under a plain unit run — the desk ground and the coral primary
 * must actually paint) plus a committed Roborazzi golden recorded on the pinned CI image.
 *
 * Captures only the frame — the act bodies are empty in B1 (Act 1/2/3 content lands in B2–B4, each with
 * its own golden). Rendered at a phone width so the topbar + creases + action-bar chrome match `proof.html`.
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
                    ProofScreen(zineName = "Corner Store Poems", onBack = {})
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
        // The desk (#E7DECE) is the frame's ground.
        assertTrue(
            "light desk did not paint in the proof scaffold",
            bmp.countColour(Color(0xFFE7DECE).toArgb()) > 1000,
        )
        // The one coral action ("Print setup", --coral-strong) must actually paint.
        assertTrue(
            "coral-strong primary did not paint in the light scaffold",
            bmp.countColour(Color(0xFFC64E34).toArgb()) > 200,
        )
        bmp.captureRoboImage("$GOLDEN_DIR/proof_scaffold_light.png", aa())
    }

    @Test
    fun proof_scaffold_dark() {
        val bmp = scaffoldBitmap(darkTheme = true)
        // Dark desk (#201F1E) is the ground; the coral CTA is identical across themes.
        assertTrue(
            "dark desk did not paint in the proof scaffold",
            bmp.countColour(Color(0xFF201F1E).toArgb()) > 1000,
        )
        assertTrue(
            "coral-strong primary did not paint in the dark scaffold",
            bmp.countColour(Color(0xFFC64E34).toArgb()) > 200,
        )
        bmp.captureRoboImage("$GOLDEN_DIR/proof_scaffold_dark.png", aa())
    }
}
