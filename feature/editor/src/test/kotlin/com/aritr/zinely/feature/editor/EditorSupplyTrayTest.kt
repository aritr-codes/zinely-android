package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import com.aritr.zinely.ui.theme.ZinelyTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.feature.editor.a11y.platformNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The supply tray (docs/design/editor-visual-direction.md §4 "tool tray"; mockup
 * docs/design/mockups/supply-tray.html) is the visible replacement for the lone "Add image" FAB.
 * Two contracts must hold: (1) every supply dispatches the SAME existing intent/behavior, and
 * (2) undo/redo enabled state mirrors the real store (`canUndo`/`canRedo`) — nothing hidden, no
 * dead control. Robolectric NATIVE, same tier as [EditorPageStripTest].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorSupplyTrayTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val scope = CoroutineScope(Dispatchers.Unconfined)

    private fun onePageStore(): EditorStore {
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        return EditorStore(
            EditorModel(
                document = ZineDocument(
                    format = ZineFormat.SINGLE_SHEET_8,
                    paperSize = PaperSize.LETTER,
                    pages = listOf(Page(index = 0, role = PageRole.INTERIOR)),
                ),
            ),
            scope, Dispatchers.Unconfined, runner,
        )
    }

    @Test
    fun each_enabled_supply_invokes_its_callback() {
        var photo = 0
        var words = 0
        var undo = 0
        var redo = 0
        composeRule.setContent {
            ZinelyTheme {
                EditorSupplyTray(
                    canUndo = true,
                    canRedo = true,
                    onAddPhoto = { photo++ },
                    onAddText = { words++ },
                    onUndo = { undo++ },
                    onRedo = { redo++ },
                )
            }
        }

        composeRule.onNodeWithTag(SupplyAddPhotoTag).performClick()
        composeRule.onNodeWithTag(SupplyAddWordsTag).performClick()
        composeRule.onNodeWithTag(SupplyUndoTag).performClick()
        composeRule.onNodeWithTag(SupplyRedoTag).performClick()

        assertEquals(1, photo)
        assertEquals(1, words)
        assertEquals(1, undo)
        assertEquals(1, redo)
    }

    @Test
    fun redo_is_disabled_when_cannot_redo() {
        composeRule.setContent {
            ZinelyTheme {
                EditorSupplyTray(
                    canUndo = true,
                    canRedo = false,
                    onAddPhoto = {}, onAddText = {}, onUndo = {}, onRedo = {},
                )
            }
        }

        // Undo is live (has a click action); Redo reports disabled, so a tap cannot fire it.
        composeRule.onNodeWithTag(SupplyUndoTag).assertIsEnabled()
        composeRule.onNodeWithTag(SupplyUndoTag).assertHasClickAction()
        composeRule.onNodeWithTag(SupplyRedoTag).assertIsNotEnabled()
    }

    /**
     * CI-93 on the **platform** tree: `assertIsNotEnabled` above proves the *merged* semantics tree — but
     * TalkBack reads the platform `AccessibilityNodeInfo`, and those disagreed in `f4faaa4`. A disabled
     * supply must report `enabled = false` on the tree the screen reader actually walks. This asserts the
     * disabled bit where it counts, and is the assertion the CI-93 inject→revert proof pair flips.
     *
     * (Role is asserted on the merged tree. A supply's `Role.Button` is set via `clickable()` over merged
     * child content, so it surfaces to the platform as `className = android.view.View`, not
     * `android.widget.Button` — the reported CI-30 merged-vs-platform divergence; see
     * [com.aritr.zinely.feature.editor.a11y.ZButtonPlatformA11yTest].)
     */
    @Test
    fun disabled_redo_reports_disabled_on_the_platform_tree() {
        composeRule.setContent {
            ZinelyTheme {
                EditorSupplyTray(
                    canUndo = true,
                    canRedo = false,
                    onAddPhoto = {}, onAddText = {}, onUndo = {}, onRedo = {},
                )
            }
        }

        // Role.Button (merged tree) — the intent.
        composeRule.onNodeWithTag(SupplyRedoTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))

        // The disabled bit on the platform tree — where f4faaa4 lied.
        val redo = composeRule.onNodeWithTag(SupplyRedoTag).platformNode(composeRule.activity)
        assertFalse("a disabled Redo supply must be disabled to the platform (f4faaa4 class)", redo.isEnabled)

        // The live Undo supply is the enabled twin on the same tree.
        val undo = composeRule.onNodeWithTag(SupplyUndoTag).platformNode(composeRule.activity)
        assertTrue("an enabled Undo supply must be enabled to the platform", undo.isEnabled)
    }

    @Test
    fun undo_enabled_state_tracks_store_and_undo_reverts_the_change() {
        val store = onePageStore()
        composeRule.setContent {
            ZinelyTheme {
                val state by store.uiState.collectAsState()
                EditorSupplyTray(
                    canUndo = state.canUndo,
                    canRedo = state.canRedo,
                    onAddPhoto = {},
                    onAddText = {},
                    onUndo = { store.dispatch(Intent.Undo) },
                    onRedo = { store.dispatch(Intent.Redo) },
                )
            }
        }

        // Nothing done yet → no history → Undo inert.
        composeRule.onNodeWithTag(SupplyUndoTag).assertIsNotEnabled()

        // Make one undoable change; the tray must light Undo up.
        composeRule.runOnUiThread {
            store.dispatch(Intent.PlaceText(Transform(10.0, 10.0, 30.0, 10.0), ""))
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(SupplyUndoTag).assertIsEnabled()

        val before = store.uiState.value.document.pages[0].elements.size
        composeRule.onNodeWithTag(SupplyUndoTag).performClick()
        composeRule.waitForIdle()

        assertEquals(before - 1, store.uiState.value.document.pages[0].elements.size)
    }

    @Test
    fun the_tray_offers_a_supplies_section_label_for_orientation() {
        // Orientation polish (ADR-033 follow-up): a quiet "Supplies" heading names the shelf, so a
        // screen-reader user lands on a section landmark before traversing the four supply actions
        // (DESIGN-RULES 9). It is a `heading()` so TalkBack announces it as orientation, not a button.
        composeRule.setContent {
            ZinelyTheme {
                EditorSupplyTray(
                    canUndo = false,
                    canRedo = false,
                    onAddPhoto = {}, onAddText = {}, onUndo = {}, onRedo = {},
                )
            }
        }
        composeRule.onNodeWithText(TraySectionLabel).assertIsDisplayed()
        composeRule.onNodeWithText(TraySectionLabel)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }
}
