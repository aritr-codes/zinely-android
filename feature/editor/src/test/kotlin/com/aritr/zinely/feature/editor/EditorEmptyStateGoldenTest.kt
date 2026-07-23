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
 * **CI-25** golden net for [EditorEmptyState], light + dark (roadmap §C1; the two-proof shape of the frozen
 * [TypeBarGoldenTest]). `captureRoboImage` is a no-op under a plain `testDebugUnitTest`; the desk
 * non-vacuity assertion (below) runs green then, so a golden can never be recorded off a blank capture, and
 * `:feature:editor:recordRoborazziDebug` on the pinned CI image (`record-goldens.yml`) produces the PNG.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class EditorEmptyStateGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val HOST_TAG = "editorEmptyStateGoldenHost"

        // The committed AA tolerance ([TypeBarGoldenTest].aa()): the invitation's tilted sticker corners are
        // AA edges that jitter a fraction of a pixel run-to-run.
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
        val bmp = hostBitmap()
        // Non-vacuity: the desk ground must actually paint behind the surface (a blank capture leaves none).
        assertTrue(
            "the desk did not paint behind the empty state ($name)",
            bmp.pixelCountOf(deskArgb) > 100,
        )
        bmp.captureRoboImage("$GOLDEN_DIR/$name.png", aa())
    }

    @Test
    fun editor_empty_state_light() =
        capture("editor_empty_state_light", darkTheme = false) { EditorEmptyState() }

    @Test
    fun editor_empty_state_dark() =
        capture("editor_empty_state_dark", darkTheme = true) { EditorEmptyState() }
}
