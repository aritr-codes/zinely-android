package com.aritr.zinely.feature.editor.a11y

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.ui.a11y.platformNode
import com.aritr.zinely.ui.components.ZIconButton
import com.aritr.zinely.ui.components.ZPrimaryButton
import com.aritr.zinely.ui.components.ZPrimaryButtonMetrics
import com.aritr.zinely.ui.components.ZStampButton
import com.aritr.zinely.ui.components.ZToolButton
import com.aritr.zinely.ui.components.ZToolButtonMetrics
import com.aritr.zinely.ui.components.zinelyControl
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * CI-93 (disabled / `enabled = false`) on the **platform** `AccessibilityNodeInfo` tree, for the button
 * vocabulary [com.aritr.zinely.ui.components] declares the design system with ([ZPrimaryButton],
 * [ZStampButton], [ZIconButton], [ZToolButton], and the [zinelyControl] modifier). Each sets an `enabled`
 * flag on its `clickable`; this asserts the **enabled bit an accessibility service actually reads** — the
 * exact bit `f4faaa4` lied about — in both states, via the CI-26 [platformNode] harness.
 *
 * `assertIsEnabled` / `assertIsNotEnabled` (used elsewhere) prove the *merged* semantics tree; TalkBack
 * reads the platform tree, and the two disagreed in `f4faaa4`. Here the disabled node is verified to report
 * `enabled = false` on the platform node — where a screen reader consumes it. This is the assertion the
 * CI-93 inject→revert proof pair flips.
 *
 * ## Role, and a reported divergence (do NOT read this as a green Role check)
 *
 * These primitives also set `role = Role.Button`, asserted here on the **merged** tree. They are **not**
 * asserted as `android.widget.Button` on the platform tree, because they do not surface that way: a
 * `Role.Button` set via `clickable()` on a control that **merges child content** (every Z* button wraps a
 * `BasicText`/icon) reaches the platform `AccessibilityNodeInfo` as `className = android.view.View` with a
 * null `roleDescription` — the Role-derived class is dropped for merged nodes.
 *
 * The rule is **leaf/cleared vs merged, and it is identical for every role**: on the platform tree a Role's
 * class survives only on a leaf node or one carrying `clearAndSetSemantics`; a role on a node that merges
 * child content collapses to `android.view.View` — `Role.Button`, `Role.RadioButton`, and `Role.Checkbox`
 * alike (a merged RadioButton and a merged Checkbox collapse exactly as a merged Button does). The only
 * platform-tree Role assertions in this milestone that hold are therefore on descendant-*clearing* controls
 * — Reframe's `clearAndSetSemantics` cells surface `android.widget.Button`
 * ([ReframeControlsRolePlatformA11yTest]). The Type-bar tests assert Role on the **merged** tree only (never
 * the platform className), because their controls merge a glyph child.
 *
 * This is a genuine merged-vs-platform divergence in the same class as `f4faaa4`, surfaced by CI-30/§11.3
 * ("the platform's tree is the truth"). It is **reported, not fixed** here (STOP-condition: a production
 * change is a separate, independently reviewed item). The merged-tree Role assertion below documents the
 * *intent*; the platform-tree assertion that carries weight for these controls is the enabled bit.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ZButtonPlatformA11yTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    /** CI-93 platform-tree check: the enabled twin is enabled, the disabled twin is disabled — to the platform. */
    private fun assertEnabledBitOnPlatform(onTag: String, offTag: String) {
        val on = composeRule.onNodeWithTag(onTag).platformNode(composeRule.activity)
        assertTrue("enabled control must be enabled to the platform", on.isEnabled)
        val off = composeRule.onNodeWithTag(offTag).platformNode(composeRule.activity)
        assertFalse("disabled control must be disabled to the platform (the f4faaa4 bit)", off.isEnabled)
    }

    private fun SemanticsNodeInteraction.assertMergedButtonRole() = assert(hasRole(Role.Button))

    @Test
    fun z_primary_button_enabled_bit_reaches_the_platform_tree() {
        composeRule.setContent {
            ZinelyTheme {
                Column {
                    ZPrimaryButton("On", {}, ZPrimaryButtonMetrics.Proof, enabled = true, modifier = Modifier.testTag("primary-on"))
                    ZPrimaryButton("Off", {}, ZPrimaryButtonMetrics.Proof, enabled = false, modifier = Modifier.testTag("primary-off"))
                }
            }
        }
        assertEnabledBitOnPlatform("primary-on", "primary-off")
        composeRule.onNodeWithTag("primary-on").assertMergedButtonRole()
    }

    @Test
    fun z_stamp_button_enabled_bit_reaches_the_platform_tree() {
        composeRule.setContent {
            ZinelyTheme {
                Column {
                    ZStampButton("On", {}, enabled = true, modifier = Modifier.testTag("stamp-on"))
                    ZStampButton("Off", {}, enabled = false, modifier = Modifier.testTag("stamp-off"))
                }
            }
        }
        assertEnabledBitOnPlatform("stamp-on", "stamp-off")
        composeRule.onNodeWithTag("stamp-on").assertMergedButtonRole()
    }

    @Test
    fun z_icon_button_enabled_bit_reaches_the_platform_tree() {
        composeRule.setContent {
            ZinelyTheme {
                Column {
                    ZIconButton({}, "Undo", enabled = true, modifier = Modifier.testTag("icon-on")) { Box(Modifier.size(18.dp)) }
                    ZIconButton({}, "Redo", enabled = false, modifier = Modifier.testTag("icon-off")) { Box(Modifier.size(18.dp)) }
                }
            }
        }
        assertEnabledBitOnPlatform("icon-on", "icon-off")
        composeRule.onNodeWithTag("icon-on").assertMergedButtonRole()
    }

    @Test
    fun z_tool_button_enabled_bit_reaches_the_platform_tree() {
        composeRule.setContent {
            ZinelyTheme {
                Column {
                    ZToolButton({}, ZToolButtonMetrics.BenchTool, text = "On", enabled = true, modifier = Modifier.testTag("tool-on"))
                    ZToolButton({}, ZToolButtonMetrics.BenchTool, text = "Off", enabled = false, modifier = Modifier.testTag("tool-off"))
                }
            }
        }
        assertEnabledBitOnPlatform("tool-on", "tool-off")
        composeRule.onNodeWithTag("tool-on").assertMergedButtonRole()
    }

    @Test
    fun zinely_control_enabled_bit_reaches_the_platform_tree() {
        composeRule.setContent {
            ZinelyTheme {
                Column {
                    Text("on", Modifier.testTag("control-on").size(48.dp).zinelyControl(label = "Open", enabled = true, onClick = {}))
                    Text("off", Modifier.testTag("control-off").size(48.dp).zinelyControl(label = "Blocked", enabled = false, onClick = {}))
                }
            }
        }
        assertEnabledBitOnPlatform("control-on", "control-off")
        composeRule.onNodeWithTag("control-on").assertMergedButtonRole()
    }
}
