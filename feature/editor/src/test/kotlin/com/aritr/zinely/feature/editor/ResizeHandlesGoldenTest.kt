package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.ResizeHandle
import com.aritr.zinely.core.editor.ViewState
import com.aritr.zinely.core.editor.toUiState
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
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
import org.robolectric.annotation.GraphicsMode

/**
 * **CI-25** golden net for [ResizeHandles], light + dark (roadmap §C1). Like [SelectionChromeGoldenTest]
 * this is an on-canvas overlay drawn in Compose screen space (constant handle geometry, not part of the
 * `:core:render` tape), so it is hosted over a page-sized box with one selected element. No `@Config`
 * qualifier: the default Robolectric density is 1.0 (dp == px), so a 200dp box is 200px and the element's
 * eight handles — at `screenPxPerPt = 2`, page (40,40)-(60,60) ⇒ device (80,80)-(120,120) — sit centred and
 * fully in frame, mirroring [ResizeHandlesTest].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ResizeHandlesGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val HOST_TAG = "resizeHandlesGoldenHost"

        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }

    private val uiState = EditorModel(
        document = ZineDocument(
            format = ZineFormat.SINGLE_SHEET_8,
            paperSize = PaperSize.LETTER,
            pages = listOf(
                Page(
                    index = 0,
                    role = PageRole.INTERIOR,
                    elements = listOf(
                        TextElement(id = "t1", transform = Transform(40.0, 40.0, 20.0, 20.0, 0.0), text = "Zine"),
                    ),
                ),
            ),
        ),
        selection = setOf("t1"),
        view = ViewState(screenPxPerPt = 2f),
    ).toUiState()

    private var deskArgb = 0

    private fun host(darkTheme: Boolean) {
        composeRule.setContent {
            ZinelyTheme(darkTheme = darkTheme) {
                deskArgb = ZinelyTheme.colors.desk.toArgb()
                Box(
                    Modifier
                        .testTag(HOST_TAG)
                        .size(200.dp)
                        .background(ZinelyTheme.colors.desk),
                ) {
                    ResizeHandles(
                        uiState = uiState,
                        currentState = { uiState },
                        dispatch = {},
                        onResize = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun hostBitmap(): Bitmap {
        val bounds = composeRule.onNodeWithTag(HOST_TAG).fetchSemanticsNode().boundsInRoot
        val full = composeRule.activity.window.decorView.rasterizeToBitmap()
        return cropToBounds(full, bounds)
    }

    private fun capture(name: String, darkTheme: Boolean) {
        host(darkTheme)
        // The handles are actually composed (structural non-vacuity: a handle node must exist)...
        composeRule.onNodeWithTag("$ResizeHandleTagPrefix${ResizeHandle.BOTTOM_RIGHT.name}").assertExists()
        val bmp = hostBitmap()
        // ...and the desk ground painted behind them (a blank capture leaves none).
        assertTrue(
            "the desk did not paint behind the resize handles ($name)",
            bmp.pixelCountOf(deskArgb) > 100,
        )
        bmp.captureRoboImage("$GOLDEN_DIR/$name.png", aa())
    }

    @Test
    fun resize_handles_light() = capture("resize_handles_light", darkTheme = false)

    @Test
    fun resize_handles_dark() = capture("resize_handles_dark", darkTheme = true)
}
