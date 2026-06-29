package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The S4 host [EditorScreen] (ADR-029 §5/§6): proves the *assembly* — the measured canvas feeds the model
 * viewport, the visible contextbar tracks selection, the accessible mirror is wired, and an open text
 * session raises the edit overlay. Robolectric NATIVE, the same tier as the per-layer tests; the layers'
 * own behaviour is covered by their dedicated suites.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val pageSizePt = PtSize(100.0, 100.0)

    private fun store(): EditorStore {
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

    private fun setScreen(store: EditorStore) {
        composeRule.setContent {
            MaterialTheme {
                EditorScreen(store = store, pageSizePt = pageSizePt, modifier = Modifier.size(300.dp, 400.dp))
            }
        }
    }

    @Test
    fun measuring_the_canvas_pushes_a_real_viewport_into_the_model() {
        val store = store()
        setScreen(store)
        composeRule.waitForIdle()
        // The host fit a 100×100pt page into a 300×400px canvas, so the scale is well above the 1f default —
        // proving SetViewport flowed from the measured canvas into the shared ViewState.
        assertTrue(store.uiState.value.view.screenPxPerPt > 1f)
    }

    @Test
    fun the_contextbar_tracks_selection() {
        val store = store()
        store.dispatch(Intent.PlaceText(Transform(20.0, 20.0, 20.0, 20.0), "hi")) // auto-selects
        setScreen(store)
        composeRule.onNodeWithTag(EditorContextBarTestTag).assertIsDisplayed()

        store.dispatch(Intent.ClearSelection)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorContextBarTestTag).assertDoesNotExist()
    }

    @Test
    fun the_accessible_element_mirror_is_wired() {
        val store = store()
        store.dispatch(Intent.PlaceText(Transform(20.0, 20.0, 20.0, 20.0), "hi"))
        val id = store.uiState.value.selection.single()
        setScreen(store)
        composeRule.onNodeWithTag("$ElementNodeTagPrefix$id").assertExists()
    }

    @Test
    fun a_text_session_gates_the_canvas_so_resize_handles_disappear() {
        // Regression for the lost-draft race (Codex RF1): while editing, the gesture surface + handles must
        // be inert, or a stray long-press/double-tap replaces EditingText and the draft is dropped. A
        // single-selected element shows handles; opening its text session must remove them.
        val store = store()
        store.dispatch(Intent.PlaceText(Transform(20.0, 20.0, 20.0, 20.0), "hi"))
        val id = store.uiState.value.selection.single()
        setScreen(store)
        composeRule.onNodeWithTag("${ResizeHandleTagPrefix}TOP_LEFT").assertExists()

        store.dispatch(Intent.BeginEditText(id))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("${ResizeHandleTagPrefix}TOP_LEFT").assertDoesNotExist()
    }

    @Test
    fun the_supply_tray_add_words_drives_the_live_host_add_text_path() {
        // Wiring proof at the host seam: the EditorSupplyTray is assembled inside EditorScreen (not tested in
        // isolation here), so tapping its "Add words" supply must run the host's addTextAndEdit — place an
        // empty text element on the current page, select it, and open its edit session.
        val store = store()
        setScreen(store)
        composeRule.waitForIdle()
        assertTrue(store.uiState.value.document.pages[0].elements.isEmpty())

        composeRule.onNodeWithTag(SupplyAddWordsTag).performClick()
        composeRule.waitForIdle()

        val page = store.uiState.value.document.pages[0]
        assertTrue(page.elements.size == 1)
        assertTrue(store.uiState.value.selection.size == 1)
        assertTrue(store.uiState.value.interaction is Interaction.EditingText)
        composeRule.onNodeWithTag(EditTextSessionTestTag).assertIsDisplayed()
    }

    @Test
    fun a_move_resize_hint_appears_once_an_element_is_selected() {
        // Discoverability teach: once a placed element is single-selected (handles visible, Idle), the
        // one-time hint floats in to say the moves are drag/pinch — the gestures that have no other twin.
        val store = store()
        store.dispatch(Intent.PlaceText(Transform(20.0, 20.0, 20.0, 20.0), "hi")) // auto-selects, Idle
        setScreen(store)
        composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).assertIsDisplayed()
    }

    @Test
    fun the_move_resize_hint_stays_hidden_with_no_selection() {
        // Relevance: nothing selected → no handles → no gesture to teach → no hint clutter.
        val store = store()
        setScreen(store)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).assertDoesNotExist()
    }

    @Test
    fun tapping_got_it_dismisses_the_move_resize_hint_for_the_session() {
        // Easy, touch-safe dismissal; screen-local one-time — re-selecting must not bring it back.
        val store = store()
        store.dispatch(Intent.PlaceText(Transform(20.0, 20.0, 20.0, 20.0), "hi"))
        val id = store.uiState.value.selection.single()
        setScreen(store)
        composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).assertIsDisplayed()

        composeRule.onNodeWithTag(MoveResizeHintDismissTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).assertDoesNotExist()

        // Re-select the same element: still dismissed (one-time, not per-selection nagging).
        store.dispatch(Intent.ClearSelection)
        composeRule.waitForIdle()
        store.dispatch(Intent.Select(id))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).assertDoesNotExist()
    }

    @Test
    fun opening_a_text_session_replaces_the_hint_with_the_edit_overlay() {
        // The hint never blocks editing: opening a text session yields the hint and raises the overlay.
        val store = store()
        store.dispatch(Intent.PlaceText(Transform(20.0, 20.0, 20.0, 20.0), "hi"))
        val id = store.uiState.value.selection.single()
        setScreen(store)
        composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).assertIsDisplayed()

        store.dispatch(Intent.BeginEditText(id))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(EditTextSessionTestTag).assertIsDisplayed()
    }

    @Test
    fun an_open_text_session_raises_the_edit_overlay() {
        val store = store()
        store.dispatch(Intent.PlaceText(Transform(20.0, 20.0, 20.0, 20.0), "hi"))
        val id = store.uiState.value.selection.single()
        setScreen(store)
        composeRule.onNodeWithTag(EditTextSessionTestTag).assertDoesNotExist()

        store.dispatch(Intent.BeginEditText(id))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditTextSessionTestTag).assertIsDisplayed()
    }
}
