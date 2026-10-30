package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.editor.EditorModel
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
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The knob with the eight handles (Bench A48, ADR-143): light, dark, turned, on a small piece, and flipped
 * above a piece at the foot of the page. Composes the same two overlays as [EditorScreen], without the page,
 * so a knob that vanishes or moves cannot hide inside a whole-screen picture's 2% threshold.
 * 1 dp == 1 px, as in [ResizeHandlesGoldenTest]; 200×280 dp at `screenPxPerPt = 2` is a 100×140 pt page.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h740dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TurnKnobGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val HOST_TAG = "turnKnobGoldenHost"

        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }

    private fun capture(name: String, box: Transform, darkTheme: Boolean = false) {
        val uiState = EditorModel(
            document = ZineDocument(
                format = ZineFormat.SINGLE_SHEET_8,
                paperSize = PaperSize.LETTER,
                pages = listOf(
                    Page(index = 0, role = PageRole.INTERIOR, elements = listOf(TextElement(id = "t1", transform = box, text = "Zine"))),
                ),
            ),
            selection = setOf("t1"),
            view = ViewState(screenPxPerPt = 2f),
        ).toUiState()
        var deskArgb = 0
        composeRule.setContent {
            ZinelyTheme(darkTheme = darkTheme) {
                deskArgb = ZinelyTheme.colors.desk.toArgb()
                Box(Modifier.testTag(HOST_TAG).size(200.dp, 280.dp).background(ZinelyTheme.colors.desk)) {
                    ResizeHandles(
                        uiState = uiState,
                        currentState = { uiState },
                        dispatch = {},
                        onResize = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                    TurnHandle(
                        uiState = uiState,
                        currentState = { uiState },
                        dispatch = {},
                        onTurn = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TurnHandleTestTag).assertExists()
        val bounds = composeRule.onNodeWithTag(HOST_TAG).fetchSemanticsNode().boundsInRoot
        val bmp = cropToBounds(composeRule.activity.window.decorView.rasterizeToBitmap(), bounds)
        assertTrue("the desk did not paint ($name)", bmp.pixelCountOf(deskArgb) > 100)
        bmp.captureRoboImage("$GOLDEN_DIR/$name.png", aa())
    }

    private val mid = Transform(30.0, 40.0, 40.0, 30.0)

    @Test
    fun turn_knob_light() = capture("turn_knob_light", mid)

    @Test
    fun turn_knob_dark() = capture("turn_knob_dark", mid, darkTheme = true)

    @Test
    fun turn_knob_turned() = capture("turn_knob_turned", mid.copy(rotationDegrees = 45.0))

    @Test
    fun turn_knob_small_piece() = capture("turn_knob_small_piece", Transform(45.0, 40.0, 10.0, 10.0))

    @Test
    fun turn_knob_flipped() = capture("turn_knob_flipped", Transform(30.0, 115.0, 40.0, 20.0))
}
