package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.ViewState
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The accessible element overlay (ADR-029 §6, WCAG 2.4.7/2.5.7): one focusable node per element carrying
 * Select + the single-pointer transform/reorder/delete custom actions. Asserts a node exists per element,
 * reports its selected state, and that invoking a custom action drives the same reducer intent (one undo
 * step) against a real [EditorStore]. Robolectric NATIVE.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ElementSemanticsLayerTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val scope = CoroutineScope(Dispatchers.Unconfined)

    private fun store(): EditorStore {
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        val s = EditorStore(
            EditorModel(
                document = ZineDocument(
                    format = ZineFormat.SINGLE_SHEET_8,
                    paperSize = PaperSize.LETTER,
                    pages = listOf(Page(index = 0, role = PageRole.INTERIOR)),
                ),
                view = ViewState(screenPxPerPt = 2f),
            ),
            scope, Dispatchers.Unconfined, runner,
        )
        s.dispatch(Intent.PlaceText(Transform(40.0, 40.0, 20.0, 20.0), "hi")) // auto-selects
        return s
    }

    private fun setLayer(store: EditorStore) {
        composeRule.setContent {
            MaterialTheme {
                ElementSemanticsLayer(
                    uiState = store.uiState.value, // snapshot for layout; the custom actions read the live store
                    dispatch = store::dispatch,
                    modifier = Modifier.size(200.dp, 200.dp),
                )
            }
        }
    }

    @Test
    fun a_node_exists_per_element_and_reports_selected() {
        val store = store()
        val id = store.uiState.value.selection.single()
        setLayer(store)

        val node = composeRule.onNodeWithTag("$ElementNodeTagPrefix$id").fetchSemanticsNode()
        // The element is auto-selected by PlaceText; the selectable node carries Selected state.
        assertEquals(true, node.config[SemanticsProperties.Selected])
        assertNotNull(node.config[SemanticsProperties.ContentDescription])
    }

    @Test
    fun invoking_the_make_larger_custom_action_scales_the_element() {
        val store = store()
        val id = store.uiState.value.selection.single()
        setLayer(store)

        val node = composeRule.onNodeWithTag("$ElementNodeTagPrefix$id").fetchSemanticsNode()
        val actions = node.config[SemanticsActions.CustomActions]
        val makeLarger = actions.first { it.label == "Make larger" }
        composeRule.runOnUiThread { makeLarger.action() }
        composeRule.waitForIdle()

        val t = store.uiState.value.document.pages[0].elements.single { it.id == id }.transform
        assertEquals(22.0, t.widthPt, 1e-6) // 20 × 1.1, centre-anchored
    }

    @Test
    fun invoking_delete_custom_action_removes_the_element() {
        val store = store()
        val id = store.uiState.value.selection.single()
        setLayer(store)

        val node = composeRule.onNodeWithTag("$ElementNodeTagPrefix$id").fetchSemanticsNode()
        val actions = node.config[SemanticsActions.CustomActions]
        val delete = actions.first { it.label == "Delete" }
        composeRule.runOnUiThread { delete.action() }
        composeRule.waitForIdle()

        assertTrue(store.uiState.value.document.pages[0].elements.none { it.id == id })
    }
}
