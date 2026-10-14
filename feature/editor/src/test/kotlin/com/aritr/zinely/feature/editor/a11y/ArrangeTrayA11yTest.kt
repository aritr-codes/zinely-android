package com.aritr.zinely.feature.editor.a11y

import androidx.activity.ComponentActivity
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.feature.editor.ArrangeTray
import com.aritr.zinely.ui.a11y.PlatformA11yNode
import com.aritr.zinely.ui.a11y.platformNode
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The Arrange tray's controls on the **platform** `AccessibilityNodeInfo` tree — the tree TalkBack and
 * Switch Access read (CI-26, ADR-059).
 *
 * The first assertion is the one that caught a shipped defect on the bar these controls came from:
 * `clearAndSetSemantics` deletes the `OnClick` action `IconButton` installed, and the platform derives
 * `isClickable` from that action alone, so every control reported `clickable=false` to TalkBack on
 * SM-A176B / Android 16 while still working under a finger.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArrangeTrayA11yTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val allControls = listOf(
        Copy.A11y.MOVE_LEFT, Copy.A11y.MOVE_UP, Copy.A11y.MOVE_DOWN, Copy.A11y.MOVE_RIGHT,
        Copy.A11y.MAKE_SMALLER, Copy.A11y.MAKE_LARGER,
        Copy.A11y.ROTATE_COUNTERCLOCKWISE, Copy.A11y.ROTATE_CLOCKWISE,
        Copy.A11y.SEND_BACKWARD, Copy.A11y.BRING_FORWARD,
        Copy.Editor.DONE,
    )

    private fun renderTray() {
        composeRule.setContent {
            ZinelyTheme {
                ArrangeTray(
                    visible = true,
                    elementId = "only",
                    dispatch = {},
                    onDone = {},
                    firstFocusRequester = remember { FocusRequester() },
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun node(label: String): PlatformA11yNode =
        composeRule.onNodeWithContentDescription(label).platformNode(composeRule.activity)

    @Test
    fun every_control_is_a_named_clickable_enabled_button_on_the_platform_tree() {
        renderTray()
        allControls.forEach { label ->
            val n = node(label)
            assertEquals("$label must carry Role.Button to the platform tree", "android.widget.Button", n.className)
            assertEquals(label, n.contentDescription)
            assertTrue("$label must be activatable by an accessibility service, not only by touch", n.isClickable)
            assertTrue("$label must be enabled", n.isEnabled)
            // A nudge has no state to report; an announcement on it would be noise.
            assertEquals(null, n.stateDescription)
        }
    }

    @Test
    fun every_control_reports_a_forty_eight_dp_touch_target_to_the_platform() {
        renderTray()
        // 1px tolerance: odd-width content puts both edges on a half-pixel that `getBoundsInScreen` rounds in
        // opposite directions (DEVICE-VERIFICATION.md §5).
        val floor = with(composeRule.density) { 48.dp.toPx() }
        allControls.forEach { label ->
            val bounds = node(label).boundsInScreen
            assertTrue(
                "$label reports a ${bounds.width()}×${bounds.height()}px hit-rect, under the ${floor}px floor",
                bounds.width() >= floor - 1f && bounds.height() >= floor - 1f,
            )
        }
    }

    @Test
    fun the_four_group_names_are_headings() {
        renderTray()
        listOf(Copy.Editor.ARRANGE_MOVE, Copy.Editor.ARRANGE_SIZE, Copy.Editor.ARRANGE_TURN, Copy.Editor.ARRANGE_LAYER)
            .forEach { name ->
                assertTrue(
                    "$name must be a heading on the platform tree",
                    composeRule.onNodeWithText(name).platformNode(composeRule.activity).isHeading,
                )
            }
    }
}
