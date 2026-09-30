package com.aritr.zinely.feature.editor.a11y

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.feature.editor.BenchContextBar
import com.aritr.zinely.feature.editor.BenchVerbKind
import com.aritr.zinely.feature.editor.benchContextVerbs
import com.aritr.zinely.ui.a11y.platformNode
import com.aritr.zinely.ui.a11y.platformTraversalStops
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/** Guards Replace labels and ADR-115 selected-text platform order, roles, and blank-text guards. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BenchContextBarPlatformA11yTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val fired = mutableListOf<String>()

    private fun render(kind: BenchVerbKind, styleable: Boolean = true) {
        composeRule.setContent {
            ZinelyTheme {
                Box(Modifier.fillMaxSize()) {
                    BenchContextBar(
                        visible = true,
                        verbs = benchContextVerbs(kind, styleable = styleable),
                        onVerb = { fired += it.label },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun assertLiveButton(label: String) {
        val node = composeRule.onNodeWithContentDescription(label)
            .platformNode(composeRule.activity)
        assertEquals("android.widget.Button", node.className)
        assertEquals(label, node.contentDescription?.toString())
        assertTrue("$label must be enabled to the platform", node.isEnabled)
        assertTrue("$label must be clickable to an accessibility service", node.isClickable)
    }

    @Test
    fun authored_text_exposes_exactly_five_live_actions_in_order_and_dispatches_them() {
        render(BenchVerbKind.TEXT)
        val expected = listOf("Edit", "Size", "Ink", "Duplicate", "Delete")
        assertEquals(expected, platformTraversalStops(composeRule.activity).map { it.label })
        composeRule.onNodeWithContentDescription(Copy.BenchVerbs.FONT).assertDoesNotExist()
        for (label in expected) {
            assertLiveButton(label)
            composeRule.onNodeWithContentDescription(label).performClick()
            composeRule.waitForIdle()
        }
        assertEquals(expected, fired)
    }

    @Test
    fun blank_text_keeps_only_its_three_actionable_guards_and_no_font_stop() {
        render(BenchVerbKind.TEXT, styleable = false)
        composeRule.onNodeWithContentDescription(Copy.BenchVerbs.FONT).assertDoesNotExist()
        assertLiveButton(Copy.BenchVerbs.EDIT)
        assertLiveButton(Copy.BenchVerbs.DELETE)
        for (label in listOf(Copy.BenchVerbs.SIZE, Copy.BenchVerbs.INK, Copy.BenchVerbs.DUPLICATE)) {
            val node = composeRule.onNodeWithContentDescription(label).platformNode(composeRule.activity)
            assertFalse(node.isEnabled)
            assertFalse(node.isClickable)
            assertEquals(Copy.BenchVerbs.TYPE_FIRST, node.stateDescription?.toString())
        }
    }

    @Test
    fun photo_replace_names_its_target_on_the_platform() {
        render(BenchVerbKind.PHOTO)
        assertLiveButton(Copy.A11y.REPLACE_PHOTO)
    }

    @Test
    fun decor_replace_keeps_its_distinct_supply_name_on_the_platform() {
        render(BenchVerbKind.DECOR)
        assertLiveButton(Copy.A11y.REPLACE_SUPPLY)
    }
}
