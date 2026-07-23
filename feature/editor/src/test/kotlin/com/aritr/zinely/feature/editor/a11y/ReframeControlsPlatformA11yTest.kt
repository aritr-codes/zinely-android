package com.aritr.zinely.feature.editor.a11y

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.aritr.zinely.feature.editor.FrameFit
import com.aritr.zinely.feature.editor.ReframeControls
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * CI-26 demonstrating test — assert a real control's state as the PLATFORM accessibility tree reports it.
 *
 * The Reframe zoom-in stepper ([ReframeControls] `ZoomButton`, spoken "Zoom in") is the exact control class
 * that shipped the `f4faaa4` enabled-state defect: green against Compose's merged semantics, wrong to
 * TalkBack. This test reads the platform [android.view.accessibility.AccessibilityNodeInfo] Compose hands an
 * accessibility service — via [platformNode] — and asserts the attributes the service actually consumes:
 * the enabled bit, the button class, the click affordance, and a laid-out hit-rect.
 *
 * It is the consumer that proves the [PlatformAccessibilityTree] harness runs in the existing `:feature:editor`
 * Robolectric JVM suite and reads the platform tree rather than the merged semantics tree.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReframeControlsPlatformA11yTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun renderControls() {
        composeRule.setContent {
            ZinelyTheme {
                ReframeControls(
                    fit = FrameFit.FILL,
                    zoomPercent = 100,
                    onFit = {},
                    onNudge = { _, _ -> },
                    onZoom = {},
                    onReset = {},
                    onCancel = {},
                    onDone = {},
                )
            }
        }
    }

    @Test
    fun the_zoom_in_stepper_is_enabled_in_the_platform_accessibility_tree() {
        renderControls()

        val node = composeRule
            .onNodeWithContentDescription("Zoom in")
            .platformNode(composeRule.activity)

        // Sanity: we read the node we think we read (identity via the spoken label).
        assertEquals("Zoom in", node.contentDescription)

        // The CI-26 core assertion — the ENABLED bit the platform tree exposes to TalkBack. This is the
        // attribute that lied in f4faaa4; the injected-defect proof flips exactly this value.
        assertTrue("platform tree must report the zoom-in stepper ENABLED", node.isEnabled)

        // Role.Button surfaces to the platform as a Button; a clickable control advertises its click; and a
        // laid-out control reports a positive-area hit-rect. All four are the fields the harness exposes.
        assertEquals("android.widget.Button", node.className)
        assertTrue("platform tree must report the stepper clickable", node.isClickable)
        assertTrue("platform tree must report a laid-out hit-rect", node.hasNonEmptyBounds)
    }
}
