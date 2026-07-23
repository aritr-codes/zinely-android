package com.aritr.zinely.feature.editor.a11y

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.aritr.zinely.feature.editor.EditorContextBar
import com.aritr.zinely.feature.editor.EditorContextBarTestTag
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * CI-29 (`stateDescription`) + CI-30 (`Role`) for the [EditorContextBar], asserted where production sets
 * them (EditorContextBar.kt:168-169).
 *
 * The bar's "Text style" control is a **disclosure**: it publishes a `stateDescription` of "Showing" /
 * "Hidden" so a screen-reader user knows what the tap did. That announcement (CI-29) had **no** assertion
 * before — a re-skin (C6 restyles this file) could drop it with every test still green. Here it is pinned:
 * both the state and `Role.Button` are asserted on the **merged** semantics tree (see the Role paragraph
 * below for why the platform node is not the vehicle here), because Role is what TalkBack announces the
 * control *as*.
 *
 * `stateDescription` is asserted on the merged semantics tree rather than through [PlatformA11yNode]: the
 * CI-26 harness snapshots class / clickable / enabled / bounds / contentDescription — the enabled-bit
 * defect it was built for — and does not capture `getStateDescription()`. The merged value is the source
 * Compose synthesises the platform node's state from, so pinning it there is the faithful CI-29 assertion
 * available without extending the frozen harness.
 *
 * **Role** is asserted on the merged tree here too. Each [EditorContextBar] control is a Material
 * `IconButton` wrapped in `clearAndSetSemantics { role = Role.Button }` inside a **horizontally scrolled**
 * row; the platform `AccessibilityNodeInfo` a control produces is position-dependent (a control scrolled
 * off-screen materialises an empty platform node), so a stable per-control platform-class assertion is not
 * reliable in the JVM harness — the on-device platform tree is covered by the mandatory device passes. The
 * only platform-tree Role assertions that hold in this milestone are on descendant-*clearing* controls:
 * [ReframeControlsRolePlatformA11yTest] (Button). (The Type-bar radios/checkboxes assert Role on the
 * **merged** tree, not the platform className — their controls merge a glyph child, which collapses any
 * role's platform class to `android.view.View`; see [ZButtonPlatformA11yTest].)
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorContextBarA11yTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun barTag(description: String) = "$EditorContextBarTestTag-$description"

    private fun renderBar(styleOpen: Boolean) {
        composeRule.setContent {
            ZinelyTheme {
                EditorContextBar(
                    selection = setOf("only-text"),
                    dispatch = {},
                    onStyle = {},
                    styleOpen = styleOpen,
                )
            }
        }
    }

    private fun hasStateDescription(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    @Test
    fun the_text_style_disclosure_announces_showing_when_the_type_bar_is_open() {
        renderBar(styleOpen = true)
        composeRule.onNodeWithTag(barTag("Text style")).assert(hasStateDescription("Showing"))
    }

    @Test
    fun the_text_style_disclosure_announces_hidden_when_the_type_bar_is_closed() {
        renderBar(styleOpen = false)
        composeRule.onNodeWithTag(barTag("Text style")).assert(hasStateDescription("Hidden"))
    }

    @Test
    fun context_bar_controls_carry_the_button_role() {
        renderBar(styleOpen = false)
        // A plain transform control and the disclosure both carry Role.Button (merged tree — see class KDoc
        // for why the scrolling bar's platform node is asserted on-device rather than here).
        composeRule.onNodeWithTag(barTag("Move left")).assert(hasRole(Role.Button))
        composeRule.onNodeWithTag(barTag("Text style")).assert(hasRole(Role.Button))
    }

    private fun hasRole(role: Role) =
        SemanticsMatcher.expectValue(SemanticsProperties.Role, role)
}
