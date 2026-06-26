package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
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
