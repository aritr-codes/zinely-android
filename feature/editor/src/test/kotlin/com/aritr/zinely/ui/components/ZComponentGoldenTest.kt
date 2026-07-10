package com.aritr.zinely.ui.components

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.feature.editor.rasterizeToBitmap
import com.aritr.zinely.ui.theme.LocalZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyMotion
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
 * M1 component goldens, light + dark — the same two-proof shape as [SelectionChromeGoldenTest]:
 * a behavioural pixel assertion (mode-independent, green under a plain unit run) plus a committed
 * Roborazzi golden (recorded by `recordRoborazziDebug`, gated by `verifyRoborazziDebug`).
 *
 * The gallery composes every M1 component in a plain host — including [ZSheetSurface], which is
 * goldened here precisely because the real [ZSheet] lives in a Dialog window that the decor-view
 * capture cannot see (pre-M1 review, Required Fix 4); ZSheet *behavior* is covered in
 * [ZComponentsTest]. The sweep is captured in its reduced-motion (static wash) state — the
 * animated state never settles for a deterministic frame.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ZComponentGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val GALLERY_TAG = "zComponentGallery"
        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }

    @Composable
    private fun Gallery() {
        val desk = ZinelyTheme.colors.desk
        Column(
            modifier = Modifier
                .testTag(GALLERY_TAG)
                .width(420.dp)
                .background(desk)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ZPrimaryButton(text = "Start a zine", onClick = {}, metrics = ZPrimaryButtonMetrics.Shelf)
            ZPrimaryButton(text = "Proof it", onClick = {}, metrics = ZPrimaryButtonMetrics.Bench)
            ZPrimaryButton(
                text = "Save as PDF", onClick = {},
                metrics = ZPrimaryButtonMetrics.Proof, fill = ZPrimaryFill.Stamp,
            )
            ZStampButton(text = "Try again", onClick = {})
            ZIconButton(onClick = {}, contentDescription = "Undo") { tint -> CheckGlyph(tint) }
            ZIconButton(onClick = {}, contentDescription = "Redo", enabled = false) { tint -> CheckGlyph(tint) }
            ZToolButton(onClick = {}, metrics = ZToolButtonMetrics.BenchTool, text = "Text")
            ZToolButton(onClick = {}, metrics = ZToolButtonMetrics.ProofGhost, text = "Fold guide")
            ZMenuItem("Duplicate", onClick = {})
            ZMenuItem("Delete", onClick = {}, danger = true)
            ZMenuItem("Newest first", onClick = {}, selected = true, selectedStyle = ZSelectedStyle.WeightAndCheck)
            ZMenuItem("A4", onClick = {}, selected = true, selectedStyle = ZSelectedStyle.Coral, subLabel = "210 × 297 mm")
            ZSheetSurface(title = "Paper size", sub = "Match your printer.") {
                ZMenuItem("Letter", onClick = {}, selected = false, selectedStyle = ZSelectedStyle.Coral)
            }
            ZSnackbar(message = "Zine deleted", actionLabel = "Undo", onAction = {}, onTimeout = {})
            ZToast(message = "Saved", onTimeout = {})
            ZStatusPane(
                title = "Couldn't open your shelf",
                body = "Your zines are safe on this device.",
                badgeBackground = ZinelyTheme.colors.coralStrong.copy(alpha = 0.14f),
                badgeContent = ZinelyTheme.colors.coralText,
                badgeIcon = { tint -> CheckGlyph(tint) },
                cta = { ZStampButton(text = "Try again", onClick = {}) },
            )
            ZPaperSurface(boundEdgeWidth = 7.dp, boundEdgeAlpha = 0.14f) { Box(Modifier.size(120.dp, 160.dp)) }
            // Static reduced-motion wash — the only deterministic sweep frame.
            CompositionLocalProvider(LocalZinelyMotion provides ZinelyMotion(reduceMotion = true)) {
                Box(
                    Modifier
                        .size(120.dp, 40.dp)
                        .background(ZinelyTheme.colors.paper2)
                        .zinelySweep(),
                )
            }
            ZTextField(value = "My first zine", onValueChange = {})
        }
    }

    private fun galleryBitmap(darkTheme: Boolean): Bitmap {
        composeRule.setContent { ZinelyTheme(darkTheme = darkTheme) { Gallery() } }
        composeRule.waitForIdle()
        val bounds = composeRule.onNodeWithTag(GALLERY_TAG).fetchSemanticsNode().boundsInRoot
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
    fun component_gallery_light() {
        val bmp = galleryBitmap(darkTheme = false)
        // Behavioural: the coral primary fill must actually paint (frozen --coral-strong).
        assertTrue(
            "coral-strong did not paint in the light gallery",
            bmp.countColour(Color(0xFFC64E34).toArgb()) > 200,
        )
        // And the stamp fill (buttons + snackbar).
        assertTrue(
            "stamp did not paint in the light gallery",
            bmp.countColour(Color(0xFF264653).toArgb()) > 200,
        )
        bmp.captureRoboImage("$GOLDEN_DIR/z_components_light.png", aa())
    }

    @Test
    fun component_gallery_dark() {
        val bmp = galleryBitmap(darkTheme = true)
        // Dark desk (#201F1E) must be the gallery ground; coral CTA identical in dark.
        assertTrue(
            "dark desk did not paint in the dark gallery",
            bmp.countColour(Color(0xFF201F1E).toArgb()) > 1000,
        )
        assertTrue(
            "coral-strong did not paint in the dark gallery",
            bmp.countColour(Color(0xFFC64E34).toArgb()) > 200,
        )
        bmp.captureRoboImage("$GOLDEN_DIR/z_components_dark.png", aa())
    }
}
