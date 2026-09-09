package com.aritr.zinely.feature.editor.a11y

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.feature.editor.BenchStyleRow
import com.aritr.zinely.feature.editor.BenchStyleRowTestTag
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
 * The C3 style row on the **platform** accessibility tree — the tier
 * [ReframeControlsRolePlatformA11yTest] exists for, applied to the row ADR-093 rows 3.4–3.7 add.
 *
 * D-108 removes the fake Font / Size buttons and makes Ink match the already-wired HTML. Compose merged-tree
 * checks are insufficient for this contract: the row once passed them while a platform node still reported
 * the wrong enabled/clickable state. [platformNode] therefore proves both remaining actions are real Android
 * buttons that an accessibility service can activate.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BenchStyleRowPlatformA11yTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun render() {
        composeRule.setContent {
            ZinelyTheme {
                BenchStyleRow(visible = true, inkSwatch = Color.Black, onInk = {}, onDone = {})
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun ink_is_a_real_platform_button() {
        render()
        val ink = composeRule.onNodeWithContentDescription(Copy.BenchVerbs.INK)
            .platformNode(composeRule.activity)
        assertEquals("android.widget.Button", ink.className)
        assertTrue("Ink must be enabled to the platform", ink.isEnabled)
        assertTrue("Ink must be activatable by an accessibility service", ink.isClickable)
    }

    @Test
    fun done_remains_a_live_platform_control() {
        render()
        val done = composeRule.onNodeWithContentDescription(Copy.EditText.DONE).platformNode(composeRule.activity)
        assertEquals("android.widget.Button", done.className)
        assertTrue("Done must be enabled to the platform", done.isEnabled)
        assertTrue("Done must be activatable by an accessibility service, not only by touch", done.isClickable)
    }

    @Test
    fun ink_and_done_remain_inside_the_row_at_maximum_font_scale() {
        composeRule.setContent {
            ZinelyTheme {
                val base = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 2f)) {
                    BenchStyleRow(
                        visible = true,
                        inkSwatch = Color.Black,
                        onInk = {},
                        onDone = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()

        val row = composeRule.onNodeWithTag(BenchStyleRowTestTag).fetchSemanticsNode().boundsInRoot
        val ink = composeRule.onNodeWithContentDescription(Copy.BenchVerbs.INK)
            .fetchSemanticsNode().boundsInRoot
        val done = composeRule.onNodeWithContentDescription(Copy.EditText.DONE)
            .fetchSemanticsNode().boundsInRoot
        assertTrue("Ink must stay inside the leading row edge at 2.0 font scale", ink.left >= row.left)
        assertTrue("Done must stay inside the trailing row edge at 2.0 font scale", done.right <= row.right)
    }
}
