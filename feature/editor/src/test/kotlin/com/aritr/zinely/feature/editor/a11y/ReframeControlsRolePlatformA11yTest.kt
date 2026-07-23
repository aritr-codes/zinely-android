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
 * CI-30 (`Role`) for the [ReframeControls] surface on the **platform** accessibility tree (CI-26
 * [platformNode]). Every Reframe control sets `role = Role.Button` (ReframeControls.kt:196, 243, 281, 317,
 * 336, 361, 399); the sibling demo [ReframeControlsPlatformA11yTest] pins the one "Zoom in" stepper (the
 * `f4faaa4` control). This suite extends that to the rest of the band — nudge pad, both zoom steps, the fit
 * segments, Reset, Cancel and Done — so every control TalkBack announces as a Button is proven to carry
 * `android.widget.Button` to the platform, not just to Compose's merged tree.
 *
 * `ReframeControls` is rendered standalone (no photo), so it does **not** touch the Robolectric NATIVE
 * image decoder that flakes in the full-screen [com.aritr.zinely.feature.editor.ReframeA11yTest].
 *
 * **CI-93 note (a finding, reported not fixed):** `ReframeControls` has **no** `enabled = false` path in
 * the current tree — every control's `clickable` uses the default `enabled = true`. The zoom stepper that
 * shipped the `f4faaa4` enabled-bit lie no longer carries a disabled state at all; the demo asserts its
 * *enabled* bit. There is therefore nothing to assert for CI-93's disabled clause on this surface.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReframeControlsRolePlatformA11yTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun render() {
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

    private fun assertPlatformButton(spokenLabel: String) {
        val node = composeRule.onNodeWithContentDescription(spokenLabel).platformNode(composeRule.activity)
        assertEquals("$spokenLabel must carry Role.Button to the platform tree", "android.widget.Button", node.className)
        assertTrue("$spokenLabel must be enabled to the platform", node.isEnabled)
    }

    @Test
    fun every_reframe_control_reports_button_role_on_the_platform_tree() {
        render()

        // Nudge pad (4 cross cells).
        assertPlatformButton("Move photo up")
        assertPlatformButton("Move photo left")
        assertPlatformButton("Move photo right")
        assertPlatformButton("Move photo down")

        // Zoom steppers.
        assertPlatformButton("Zoom in")
        assertPlatformButton("Zoom out")

        // Fit segments, Reset, Cancel, Done.
        assertPlatformButton("Fill")
        assertPlatformButton("Whole photo")
        assertPlatformButton("Reset framing")
        assertPlatformButton("Cancel reframing")
        assertPlatformButton("Done reframing")
    }
}
