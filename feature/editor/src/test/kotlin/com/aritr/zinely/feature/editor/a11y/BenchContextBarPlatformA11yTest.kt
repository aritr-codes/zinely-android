package com.aritr.zinely.feature.editor.a11y

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.feature.editor.BenchContextBar
import com.aritr.zinely.feature.editor.BenchVerbKind
import com.aritr.zinely.feature.editor.benchContextVerbs
import com.aritr.zinely.ui.a11y.platformNode
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/** Guards the platform labels for the two different actions that visibly read `Replace`. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BenchContextBarPlatformA11yTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun render(kind: BenchVerbKind) {
        composeRule.setContent {
            ZinelyTheme {
                Box(Modifier.fillMaxSize()) {
                    BenchContextBar(
                        visible = true,
                        verbs = benchContextVerbs(kind),
                        onVerb = {},
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
