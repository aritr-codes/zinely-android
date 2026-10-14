package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
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
 * Golden net for the [ArrangeTray] (Bench A33, ADR-129), light and dark, at the 360dp width A33 lays out
 * for: Move and Size on the first line, Turn and Layer on the second.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class ArrangeTrayGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val HOST_TAG = "arrangeTrayGoldenHost"

        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }

    private var deskArgb = 0

    private fun capture(name: String, darkTheme: Boolean) {
        composeRule.setContent {
            ZinelyTheme(darkTheme = darkTheme) {
                deskArgb = ZinelyTheme.colors.desk.toArgb()
                Box(
                    Modifier
                        .testTag(HOST_TAG)
                        .width(360.dp)
                        .background(ZinelyTheme.colors.desk),
                ) {
                    ArrangeTray(
                        visible = true,
                        elementId = "t1",
                        dispatch = {},
                        onDone = {},
                        firstFocusRequester = remember { FocusRequester() },
                    )
                }
            }
        }
        composeRule.waitForIdle()
        // Non-vacuity: the tray's own node must be present, so a blanked tray fails here.
        composeRule.onNodeWithTag(ArrangeTrayTestTag).assertExists()
        val bounds = composeRule.onNodeWithTag(HOST_TAG).fetchSemanticsNode().boundsInRoot
        val bmp: Bitmap = cropToBounds(composeRule.activity.window.decorView.rasterizeToBitmap(), bounds)
        assertTrue("the host desk did not paint ($name)", bmp.pixelCountOf(deskArgb) > 100)
        bmp.captureRoboImage("$GOLDEN_DIR/$name.png", aa())
    }

    @Test
    fun arrange_tray_light() = capture("arrange_tray_light", darkTheme = false)

    @Test
    fun arrange_tray_dark() = capture("arrange_tray_dark", darkTheme = true)
}
