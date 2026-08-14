package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.ui.golden.cropToBounds
import com.aritr.zinely.ui.golden.pixelCountOf
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.golden.topRowRiseOf
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * **CI-25** golden net for [EditorSaveFailure], light + dark (roadmap §C1; the frozen [TypeBarGoldenTest]
 * two-proof shape). Captured `visible = true` with `reduceMotion = true`, so the [androidx.compose.animation.AnimatedVisibility]
 * fade is skipped and the banner is fully opaque at capture — the static end-state the golden must pin.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class EditorSaveFailureGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val HOST_TAG = "editorSaveFailureGoldenHost"

        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }

    private var deskArgb = 0
    private var inkArgb = 0

    private fun host(darkTheme: Boolean, content: @Composable () -> Unit) {
        composeRule.setContent {
            ZinelyTheme(darkTheme = darkTheme) {
                // ⚠ The host desk is **V2.1's**, not `ZinelyTheme.colors.desk`. The component inside went
                // V2.1 in ADR-102 P6a while this host stayed V2, so the recorded raster was a frame in
                // one design language around a component in another — a picture depicting neither corpus.
                // Caught by review, not by the suite: the only non-image assertion here counted the host
                // background, which the wrong desk satisfies exactly as well as the right one.
                deskArgb = ZinelyTheme.v21Colors.desk.toArgb()
                inkArgb = ZinelyTheme.v21Colors.ink.toArgb()
                Box(
                    Modifier
                        .testTag(HOST_TAG)
                        .background(ZinelyTheme.v21Colors.desk)
                        .padding(16.dp),
                ) { content() }
            }
        }
        composeRule.waitForIdle()
    }

    private fun hostBitmap(): Bitmap {
        val bounds = composeRule.onNodeWithTag(HOST_TAG).fetchSemanticsNode().boundsInRoot
        val full = composeRule.activity.window.decorView.rasterizeToBitmap()
        return cropToBounds(full, bounds)
    }

    private fun capture(name: String, darkTheme: Boolean, content: @Composable () -> Unit) {
        host(darkTheme, content)
        // Non-vacuity (the real guard): the component's own node must be present. A blanked component
        // in a later re-record fails here — a desk-pixel count on a desk-backed host would still pass.
        composeRule.onNodeWithTag(EditorSaveFailureTestTag).assertExists()
        val bmp = hostBitmap()
        // Secondary sanity: the host raster is non-empty (the desk ground painted).
        assertTrue(
            "the host desk did not paint ($name)",
            bmp.pixelCountOf(deskArgb) > 100,
        )
        // ⚠ **The raster alone asserts nothing** — a golden records whatever it is shown, and until this
        // block existed the desk count above was the only claim in the file, satisfied by the host's own
        // background whatever the component did. So the two properties that actually distinguish the V2.1
        // banner from its V2 ancestor are read off the pixels, in both themes:
        //
        //  1. `.snack{background:var(--ink)}` — the banner's ground is the corpus's INK, on both themes.
        //     V2's notice was a `paper`/`desk` card, so this is the byte that flips with the re-skin, and
        //     it is the one a re-record would silently accept.
        //  2. `.snack{transform:rotate(-.6deg)}` — the settled lean, read as the **rise across the
        //     banner's own width**: the ink ground starts lower on one side than the other, and it cannot
        //     for an untilted pill. A corner-pixel probe was written first and thrown away — the banner is
        //     `radiusPill`, so its bounding-box corner is desk with the lean and without it, and the
        //     `graphicsLayer` rotation does not move layout bounds either. That test would have passed on
        //     a flat banner: the same "cannot fail for the right reason" defect this batch was fixing.
        val inkPixels = bmp.pixelCountOf(inkArgb)
        assertTrue(
            "the banner's ground is `--ink` — $inkPixels px found ($name)",
            inkPixels > 500,
        )
        val rise = bmp.topRowRiseOf(inkArgb)
        assertTrue(
            "the banner leans — ink top rises ${rise}px across its width ($name)",
            rise >= 4,
        )
        bmp.captureRoboImage("$GOLDEN_DIR/$name.png", aa())
    }

    @Composable
    private fun banner() = EditorSaveFailure(
        visible = true,
        onDismiss = {},
        onRetry = {},
        reduceMotion = true,
    )

    @Test
    fun editor_save_failure_light() =
        capture("editor_save_failure_light", darkTheme = false) { banner() }

    @Test
    fun editor_save_failure_dark() =
        capture("editor_save_failure_dark", darkTheme = true) { banner() }
}
