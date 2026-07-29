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
import com.aritr.zinely.core.model.Script
import com.aritr.zinely.core.model.TextCoverage
import com.aritr.zinely.ui.golden.cropToBounds
import com.aritr.zinely.ui.golden.pixelCountOf
import com.aritr.zinely.ui.golden.rasterizeToBitmap
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
 * Golden net for [EditorCoverageNotice], light + dark (ADR-070; roadmap §C1; the frozen
 * [EditorSaveFailureGoldenTest] two-proof shape it mirrors). Captured with `reduceMotion = true`, so the
 * [androidx.compose.animation.AnimatedVisibility] fade is skipped and the notice is fully opaque at
 * capture — the static end-state the golden must pin. Baselines are recorded on the pinned CI image only.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class EditorCoverageNoticeGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val HOST_TAG = "editorCoverageNoticeGoldenHost"

        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }

    private var deskArgb = 0

    private fun host(darkTheme: Boolean, content: @Composable () -> Unit) {
        composeRule.setContent {
            ZinelyTheme(darkTheme = darkTheme) {
                deskArgb = ZinelyTheme.colors.desk.toArgb()
                Box(
                    Modifier
                        .testTag(HOST_TAG)
                        .background(ZinelyTheme.colors.desk)
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
        // Non-vacuity (the real guard): the notice's own node must be present. A blanked component in a
        // later re-record fails here — a desk-pixel count on a desk-backed host would still pass.
        composeRule.onNodeWithTag(EditorCoverageNoticeTestTag).assertExists()
        val bmp = hostBitmap()
        // Secondary sanity: the host raster is non-empty (the desk ground painted).
        assertTrue(
            "the host desk did not paint ($name)",
            bmp.pixelCountOf(deskArgb) > 100,
        )
        bmp.captureRoboImage("$GOLDEN_DIR/$name.png", aa())
    }

    @Composable
    private fun notice() = EditorCoverageNotice(
        // Two scripts, so the golden pins the list grammar ("Bengali and Tamil …") as well as the pill.
        coverage = TextCoverage(
            unsupportedScripts = listOf(Script.BENGALI, Script.TAMIL),
            sampleCharacters = listOf("অ", "அ"),
            unsupportedCount = 2,
        ),
        reduceMotion = true,
    )

    @Test
    fun editor_coverage_notice_light() =
        capture("editor_coverage_notice_light", darkTheme = false) { notice() }

    @Test
    fun editor_coverage_notice_dark() =
        capture("editor_coverage_notice_dark", darkTheme = true) { notice() }
}
