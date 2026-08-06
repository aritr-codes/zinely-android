package com.aritr.zinely.feature.editor.a11y

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.feature.editor.BenchStyleRow
import com.aritr.zinely.ui.a11y.platformNode
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The C3 style row on the **platform** accessibility tree — the tier
 * [ReframeControlsRolePlatformA11yTest] exists for, applied to the row ADR-093 rows 3.4–3.7 add.
 *
 * Three of this row's four controls are drawn-and-inert (OD-9), which is the precise shape of the defect
 * this project has already shipped once: `ReframeControls.ZoomButton` passed `assertIsNotEnabled` against
 * Compose's **merged** tree while telling the platform `enabled = true`, so TalkBack invited a tap on a
 * control that could not act. A merged-tree assertion cannot catch that, by construction — only
 * `AccessibilityNodeInfo` can, and that is what [platformNode] reads.
 *
 * So the inert chips are asserted `isEnabled = false` **and** `isClickable = false` here. The second is the
 * one that matters most: `disabled()` alone still leaves a node an accessibility service may offer to
 * activate, and the chips carry no `clickable` modifier precisely so the platform node reports neither.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BenchStyleRowPlatformA11yTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun render() {
        composeRule.setContent {
            ZinelyTheme {
                BenchStyleRow(visible = true, inkSwatch = Color.Black, onDone = {})
            }
        }
        composeRule.waitForIdle()
    }

    private fun assertInert(label: String) {
        val node = composeRule.onNodeWithContentDescription(label).platformNode(composeRule.activity)
        assertEquals("$label must carry Role.Button to the platform tree", "android.widget.Button", node.className)
        assertFalse("$label is drawn-and-inert (OD-9) and must report DISABLED to the platform", node.isEnabled)
        assertFalse("$label must not be offered as clickable — nothing happens if it is activated", node.isClickable)
    }

    @Test
    fun the_three_inert_chips_report_disabled_and_unclickable_to_the_platform() {
        render()
        assertInert(Copy.BenchVerbs.FONT)
        assertInert(Copy.BenchVerbs.SIZE)
        assertInert(Copy.BenchVerbs.INK)
    }

    @Test
    fun done_is_the_one_live_control_and_the_platform_can_activate_it() {
        render()
        val done = composeRule.onNodeWithContentDescription(Copy.EditText.DONE).platformNode(composeRule.activity)
        assertEquals("android.widget.Button", done.className)
        assertTrue("Done is the row's only live control and must be enabled to the platform", done.isEnabled)
        assertTrue("Done must be activatable by an accessibility service, not only by touch", done.isClickable)
    }
}
