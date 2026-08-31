package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.FlipAxis
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FlipTrayTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var inputMode: InputModeManager

    private val photo = ImageElement(
        id = "photo",
        transform = Transform(0.0, 0.0, 40.0, 30.0),
        assetId = "a".repeat(64),
        flippedVertically = true,
    )

    @Test
    fun `tray exposes two immediate toggles and focuses left-right on open`() {
        val toggles = mutableListOf<FlipAxis>()
        val visible = mutableStateOf(false)
        val element = mutableStateOf(photo)
        composeRule.setContent {
            ZinelyTheme {
                inputMode = LocalInputModeManager.current
                val firstFocusRequester = remember { FocusRequester() }
                Box(Modifier.fillMaxSize()) {
                    FlipTray(
                        visible = visible.value,
                        element = element.value,
                        onToggle = { axis ->
                            toggles += axis
                            if (axis == FlipAxis.HORIZONTAL) {
                                element.value = element.value.copy(
                                    flippedHorizontally = !element.value.flippedHorizontally,
                                )
                            }
                        },
                        onDone = {},
                        firstFocusRequester = firstFocusRequester,
                    )
                }
            }
        }
        composeRule.runOnUiThread {
            inputMode.requestInputMode(InputMode.Keyboard)
            visible.value = true
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(FlipTrayTestTag)
            .assert(
                androidx.compose.ui.test.SemanticsMatcher.expectValue(
                    SemanticsProperties.PaneTitle,
                    Copy.Editor.FLIP,
                ),
            )
        composeRule.onNodeWithTag(selectionCueTag(FlipLeftRightTestTag), useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithTag(selectionCueTag(FlipTopBottomTestTag), useUnmergedTree = true).assertExists()
        val leftRightBounds = composeRule.onNodeWithTag(FlipLeftRightTestTag).getUnclippedBoundsInRoot()
        val leftRightHeight = leftRightBounds.bottom - leftRightBounds.top
        assertTrue(leftRightHeight.value >= 56f)
        assertTrue(leftRightHeight.value <= 57f)
        composeRule.onNodeWithTag(FlipLeftRightTestTag).assertIsOff().assertIsFocused().performClick()
        composeRule.onNodeWithTag(selectionCueTag(FlipLeftRightTestTag), useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(FlipTopBottomTestTag).assertIsOn()
        composeRule.onNodeWithTag(FlipDoneTestTag)
            .assertContentDescriptionEquals(Copy.Editor.DONE)
        val doneBounds = composeRule.onNodeWithTag(FlipDoneTestTag).getUnclippedBoundsInRoot()
        val doneHeight = doneBounds.bottom - doneBounds.top
        assertTrue(doneHeight.value >= 48f)
        assertTrue(doneHeight.value <= 56f)
        assertEquals(listOf(FlipAxis.HORIZONTAL), toggles)
    }

    @Test
    fun `hidden tray publishes no controls`() {
        composeRule.setContent {
            ZinelyTheme {
                val firstFocusRequester = remember { FocusRequester() }
                FlipTray(
                    visible = false,
                    element = photo,
                    onToggle = {},
                    onDone = {},
                    firstFocusRequester = firstFocusRequester,
                )
            }
        }
        composeRule.onAllNodesWithTag(FlipTrayTestTag).assertCountEquals(0)
        composeRule.onAllNodesWithTag(FlipLeftRightTestTag).assertCountEquals(0)
    }
}
