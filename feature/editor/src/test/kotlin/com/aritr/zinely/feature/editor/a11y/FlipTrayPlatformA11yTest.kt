package com.aritr.zinely.feature.editor.a11y

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.FlipAxis
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.feature.editor.FlipTray
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
 * The Flip tray's visible labels sit inside [FlipTray]'s toggleable containers. Compose's merged tree
 * made those words look reachable while the physical Samsung tree exposed two unnamed checkable Views.
 * Assert the Android [android.view.accessibility.AccessibilityNodeInfo] instead: a label, toggle state,
 * click affordance, and a 48dp hit target must all arrive at the service together.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FlipTrayPlatformA11yTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun render() {
        composeRule.setContent {
            ZinelyTheme {
                Box(Modifier.fillMaxSize()) {
                    FlipTray(
                        visible = true,
                        element = ImageElement(
                            id = "photo",
                            transform = Transform(0.0, 0.0, 40.0, 30.0),
                            assetId = "a".repeat(64),
                            flippedVertically = true,
                        ),
                        onToggle = {},
                        onDone = {},
                        firstFocusRequester = remember { FocusRequester() },
                    )
                }
            }
        }
    }

    @Test
    fun flip_axes_are_named_checkable_platform_controls_with_48dp_targets() {
        render()
        val horizontal = composeRule
            .onNodeWithContentDescription(Copy.A11y.FLIP_LEFT_RIGHT)
            .platformNode(composeRule.activity)
        val vertical = composeRule
            .onNodeWithContentDescription(Copy.A11y.FLIP_TOP_BOTTOM)
            .platformNode(composeRule.activity)
        val floor = with(composeRule.density) { 48.dp.toPx() }

        assertEquals(Copy.A11y.FLIP_LEFT_RIGHT, horizontal.contentDescription)
        assertEquals(Copy.A11y.FLIP_TOP_BOTTOM, vertical.contentDescription)
        assertTrue(horizontal.isEnabled && horizontal.isClickable && horizontal.isCheckable)
        assertTrue(vertical.isEnabled && vertical.isClickable && vertical.isCheckable)
        assertFalse(horizontal.isChecked)
        assertTrue(vertical.isChecked)
        listOf(horizontal, vertical).forEach { node ->
            assertTrue(
                "Flip target is ${node.boundsInScreen.width()}×${node.boundsInScreen.height()}px, under 48dp",
                node.boundsInScreen.width() >= floor - 1f && node.boundsInScreen.height() >= floor - 1f,
            )
        }
    }
}
