package com.aritr.zinely.feature.editor.a11y

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.feature.editor.EditorPageStrip
import com.aritr.zinely.feature.editor.editorPageCardTag
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * CI-29 (`stateDescription`) + CI-30 (`Role`) for the [EditorPageStrip] page picker, asserted where
 * production sets them (EditorPageStrip.kt:147, 152).
 *
 * Each page card publishes a `stateDescription` — "Current page" / "Not selected" — so TalkBack speaks the
 * selection state. That announcement had no assertion before (CI-29); a re-skin could have dropped it
 * silently. It is pinned here on the merged semantics tree.
 *
 * **Role.Tab and the platform tree (CI-30).** The cards are `Role.Tab`. Unlike `Role.Button` /
 * `RadioButton` / `Checkbox` — which Compose surfaces to the platform as a distinct
 * `AccessibilityNodeInfo.className` (`android.widget.Button`, …) — `Role.Tab` is carried by the platform
 * node's **`roleDescription`** ("Tab"), and its `className` stays `android.widget.TextView`. The CI-26
 * [PlatformA11yNode] snapshot captures class / clickable / enabled / bounds / contentDescription (the
 * enabled-bit defect it exists for) and does **not** read `roleDescription`; and the card is a merged
 * multi-child `selectable`, which does not faithfully carry its derived attributes onto the platform node
 * in the JVM harness (the same merged/platform divergence documented in [ZButtonPlatformA11yTest]). The
 * authoritative Tab-role assertion is therefore the merged-tree `SemanticsProperties.Role` check below; the
 * on-device platform tree is covered by the mandatory device passes.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorPageStripA11yTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val pageSizePt = PtSize(100.0, 130.0)

    /** Three empty sheets (blank-page thumbnail path — no image decode), page 1 current. */
    private fun renderStrip(currentIndex: Int = 0) {
        val pages = (0 until 3).map { Page(index = it, role = PageRole.INTERIOR, elements = emptyList()) }
        composeRule.setContent {
            ZinelyTheme {
                EditorPageStrip(
                    pages = pages,
                    currentPageIndex = currentIndex,
                    pageSizePt = pageSizePt,
                    defaults = DocumentDefaults(),
                    onSelectPage = {},
                )
            }
        }
    }

    private fun hasStateDescription(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    private fun hasRole(role: Role) =
        SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    @Test
    fun the_current_card_announces_current_page_and_others_announce_not_selected() {
        renderStrip(currentIndex = 0)
        composeRule.onNodeWithTag(editorPageCardTag(1)).assert(hasStateDescription("Current page"))
        composeRule.onNodeWithTag(editorPageCardTag(2)).assert(hasStateDescription("Not selected"))
        composeRule.onNodeWithTag(editorPageCardTag(3)).assert(hasStateDescription("Not selected"))
    }

    @Test
    fun each_card_carries_the_tab_role() {
        renderStrip(currentIndex = 0)
        // Role.Tab is carried by the platform node's roleDescription, which the CI-26 harness does not
        // snapshot (and, being a merged multi-child `selectable`, the card does not faithfully carry its
        // derived attributes onto the platform node in the JVM harness anyway — the same merged/platform
        // divergence documented in ZButtonPlatformA11yTest). The merged-tree Role is therefore the
        // authoritative Tab-role assertion; the on-device platform tree is covered by the device passes.
        composeRule.onNodeWithTag(editorPageCardTag(1)).assert(hasRole(Role.Tab))
        composeRule.onNodeWithTag(editorPageCardTag(2)).assert(hasRole(Role.Tab))
    }
}
