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
 * **CI-25** golden net for [EditorMoveResizeHint], light + dark (roadmap §C1; the frozen [TypeBarGoldenTest]
 * two-proof shape). The one-time support scrap is static text (no motion); both generic and photo copy are
 * captured because the latter adds the Reframe/crop distinction.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class EditorMoveResizeHintGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val HOST_TAG = "editorMoveResizeHintGoldenHost"

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
        // Non-vacuity (the real guard): the component's own node must be present. A blanked component
        // in a later re-record fails here — a desk-pixel count on a desk-backed host would still pass.
        composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).assertExists()
        val hint = composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).fetchSemanticsNode().boundsInRoot
        val dismiss = composeRule.onNodeWithTag(MoveResizeHintDismissTag).fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Got it must remain inside the support scrap ($name): hint=$hint dismiss=$dismiss",
            dismiss.left >= hint.left && dismiss.top >= hint.top &&
                dismiss.right <= hint.right && dismiss.bottom <= hint.bottom,
        )
        val bmp = hostBitmap()
        // Secondary sanity: the host raster is non-empty (the desk ground painted).
        assertTrue(
            "the host desk did not paint ($name)",
            bmp.pixelCountOf(deskArgb) > 100,
        )
        bmp.captureRoboImage("$GOLDEN_DIR/$name.png", aa())
    }

    @Test
    fun editor_move_resize_hint_light() =
        capture("editor_move_resize_hint_light", darkTheme = false) {
            EditorMoveResizeHint(onDismiss = {})
        }

    @Test
    fun editor_move_resize_hint_dark() =
        capture("editor_move_resize_hint_dark", darkTheme = true) {
            EditorMoveResizeHint(onDismiss = {})
        }

    @Test
    fun editor_photo_move_resize_hint_light() =
        capture("editor_photo_move_resize_hint_light", darkTheme = false) {
            EditorMoveResizeHint(photo = true, onDismiss = {})
        }

    @Test
    fun editor_photo_move_resize_hint_dark() =
        capture("editor_photo_move_resize_hint_dark", darkTheme = true) {
            EditorMoveResizeHint(photo = true, onDismiss = {})
        }
}
