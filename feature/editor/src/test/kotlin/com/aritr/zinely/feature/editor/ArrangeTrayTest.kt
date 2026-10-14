package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Arrange tray (Bench A33, ADR-129; WCAG 2.5.7): each button dispatches the SAME reducer intent as the
 * gesture and custom-action twins, so the document is asserted against a real [EditorStore].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArrangeTrayTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val scope = CoroutineScope(Dispatchers.Unconfined)

    private val allControls = listOf(
        Copy.A11y.MOVE_LEFT, Copy.A11y.MOVE_UP, Copy.A11y.MOVE_DOWN, Copy.A11y.MOVE_RIGHT,
        Copy.A11y.MAKE_SMALLER, Copy.A11y.MAKE_LARGER,
        Copy.A11y.ROTATE_COUNTERCLOCKWISE, Copy.A11y.ROTATE_CLOCKWISE,
        Copy.A11y.SEND_BACKWARD, Copy.A11y.BRING_FORWARD,
    )

    private fun storeWithTwoTexts(): EditorStore {
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        val store = EditorStore(
            EditorModel(
                document = ZineDocument(
                    format = ZineFormat.SINGLE_SHEET_8,
                    paperSize = PaperSize.LETTER,
                    pages = listOf(Page(index = 0, role = PageRole.INTERIOR)),
                ),
            ),
            scope, Dispatchers.Unconfined, runner,
        )
        store.dispatch(Intent.PlaceText(Transform(10.0, 10.0, 20.0, 20.0), "under"))
        store.dispatch(Intent.PlaceText(Transform(40.0, 40.0, 20.0, 20.0), "hi")) // auto-selects, topmost
        return store
    }

    private fun setTray(store: EditorStore, onDone: () -> Unit = {}) {
        composeRule.setContent {
            ZinelyTheme {
                val state by store.uiState.collectAsState()
                ArrangeTray(
                    visible = true,
                    elementId = state.selection.singleOrNull(),
                    dispatch = store::dispatch,
                    onDone = onDone,
                    firstFocusRequester = remember { FocusRequester() },
                )
            }
        }
    }

    private fun selected(store: EditorStore) = store.uiState.value.let { s ->
        s.document.pages[0].elements.single { it.id == s.selection.single() }
    }

    @Test
    fun each_move_button_nudges_one_step_its_own_way() {
        val store = storeWithTwoTexts()
        setTray(store)
        mapOf(
            Copy.A11y.MOVE_RIGHT to (EditorA11y.NUDGE_STEP_PT to 0.0),
            Copy.A11y.MOVE_DOWN to (EditorA11y.NUDGE_STEP_PT to EditorA11y.NUDGE_STEP_PT),
            Copy.A11y.MOVE_LEFT to (0.0 to EditorA11y.NUDGE_STEP_PT),
            Copy.A11y.MOVE_UP to (0.0 to 0.0),
        ).forEach { (label, offset) ->
            composeRule.onNodeWithContentDescription(label).performClick()
            composeRule.waitForIdle()
            val t = selected(store).transform
            assertEquals(label, 40.0 + offset.first, t.xPt, 1e-9)
            assertEquals(label, 40.0 + offset.second, t.yPt, 1e-9)
        }
    }

    @Test
    fun size_and_turn_each_commit_one_step_and_undo_one_at_a_time() {
        val store = storeWithTwoTexts()
        setTray(store)

        composeRule.onNodeWithContentDescription(Copy.A11y.MAKE_LARGER).performClick()
        composeRule.onNodeWithContentDescription(Copy.A11y.ROTATE_CLOCKWISE).performClick()
        composeRule.waitForIdle()
        selected(store).transform.let {
            // Centre-anchored scale by 1.1 grows 20 → 22; rotation adds one 15° step.
            assertEquals(22.0, it.widthPt, 1e-6)
            assertEquals(EditorA11y.ROTATE_STEP_DEGREES, it.rotationDegrees, 1e-9)
        }

        store.dispatch(Intent.Undo)
        assertEquals(0.0, selected(store).transform.rotationDegrees, 1e-9)
        assertEquals(22.0, selected(store).transform.widthPt, 1e-6)

        composeRule.onNodeWithContentDescription(Copy.A11y.MAKE_SMALLER).performClick()
        composeRule.onNodeWithContentDescription(Copy.A11y.ROTATE_COUNTERCLOCKWISE).performClick()
        composeRule.waitForIdle()
        assertEquals(20.0, selected(store).transform.widthPt, 1e-6)
        assertEquals(-EditorA11y.ROTATE_STEP_DEGREES, selected(store).transform.rotationDegrees, 1e-9)
    }

    @Test
    fun layer_buttons_restack_the_selected_piece() {
        val store = storeWithTwoTexts()
        val id = store.uiState.value.selection.single()
        setTray(store)
        // Reorder rewrites zIndex, not list position, so paint order is the sorted-by-zIndex position.
        fun indexOfSelected() = store.uiState.value.document.pages[0].elements
            .sortedBy { it.zIndex }.indexOfFirst { it.id == id }
        assertEquals(1, indexOfSelected())

        composeRule.onNodeWithContentDescription(Copy.A11y.SEND_BACKWARD).performClick()
        composeRule.waitForIdle()
        assertEquals(0, indexOfSelected())

        composeRule.onNodeWithContentDescription(Copy.A11y.BRING_FORWARD).performClick()
        composeRule.waitForIdle()
        assertEquals(1, indexOfSelected())
    }

    @Test
    fun done_reports_and_the_tray_offers_no_delete_and_no_text_style() {
        val store = storeWithTwoTexts()
        var done = 0
        setTray(store, onDone = { done++ })

        composeRule.onNodeWithTag(ArrangeDoneTestTag).performClick()
        assertEquals(1, done)
        // Delete lives on the verb bar; the Text verb opens the text card. Neither is repeated here.
        composeRule.onNodeWithContentDescription(Copy.A11y.DELETE).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Text style").assertDoesNotExist()
    }

    @Test
    // The narrowest supported phone. `assertIsDisplayed`, not `assertExists`: existing in the tree is what
    // the old bar's off-screen controls did while it scrolled (F-2).
    @Config(qualifiers = "w360dp-h740dp")
    fun every_control_is_displayed_at_full_target_size_on_a_narrow_phone() {
        val store = storeWithTwoTexts()
        setTray(store)
        val minTarget = 48f * composeRule.density.density
        allControls.forEach { label ->
            val node = composeRule.onNodeWithContentDescription(label)
            node.assertIsDisplayed()
            val bounds = node.fetchSemanticsNode().touchBoundsInRoot
            assertTrue("$label must keep 48dp — got $bounds", bounds.width >= minTarget - 1.5f)
            assertTrue("$label must keep 48dp — got $bounds", bounds.height >= minTarget - 1.5f)
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h740dp")
    fun on_a_narrow_phone_move_and_size_share_a_line_and_turn_and_layer_take_the_next() {
        // A33's layout: Move and Size on the first line, Turn and Layer on the second.
        val store = storeWithTwoTexts()
        setTray(store)
        fun top(label: String) = composeRule.onNodeWithContentDescription(label).fetchSemanticsNode().boundsInRoot.top
        assertEquals(top(Copy.A11y.MOVE_LEFT), top(Copy.A11y.MAKE_LARGER), 1f)
        assertEquals(top(Copy.A11y.ROTATE_CLOCKWISE), top(Copy.A11y.BRING_FORWARD), 1f)
        assertTrue(top(Copy.A11y.ROTATE_CLOCKWISE) > top(Copy.A11y.MOVE_LEFT))
    }
}
