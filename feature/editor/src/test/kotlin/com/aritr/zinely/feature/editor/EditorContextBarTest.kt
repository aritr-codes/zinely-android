package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The visible a11y contextbar (ADR-029 §6, WCAG 2.5.7): each button dispatches the SAME reducer intent as
 * the gesture/custom-action twins, so we assert the document mutates correctly against a real
 * [EditorStore]. Robolectric NATIVE, same tier as [EditorGesturesTest].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorContextBarTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val scope = CoroutineScope(Dispatchers.Unconfined)

    private fun storeWithSelectedText(): EditorStore {
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
        store.dispatch(Intent.PlaceText(Transform(40.0, 40.0, 20.0, 20.0), "hi")) // auto-selects
        return store
    }

    private fun setBar(store: EditorStore) {
        composeRule.setContent {
            MaterialTheme {
                val state by store.uiState.collectAsState()
                EditorContextBar(selection = state.selection, dispatch = store::dispatch)
            }
        }
    }

    @Test
    fun moveRight_nudges_the_selected_element_by_one_step() {
        val store = storeWithSelectedText()
        val id = store.uiState.value.selection.single()
        setBar(store)

        composeRule.onNodeWithContentDescription("Move right").performClick()
        composeRule.waitForIdle()

        val t = store.uiState.value.document.pages[0].elements.single { it.id == id }.transform
        assertEquals(40.0 + EditorA11y.NUDGE_STEP_PT, t.xPt, 1e-9)
        assertEquals(40.0, t.yPt, 1e-9)
    }

    @Test
    fun makeLarger_then_rotate_each_commit_one_step() {
        val store = storeWithSelectedText()
        val id = store.uiState.value.selection.single()
        setBar(store)

        composeRule.onNodeWithContentDescription("Make larger").performClick()
        composeRule.onNodeWithContentDescription("Rotate clockwise").performClick()
        composeRule.waitForIdle()

        val t = store.uiState.value.document.pages[0].elements.single { it.id == id }.transform
        // Centre-anchored scale by 1.1 grows 20 → 22; rotation adds one 15° step.
        assertEquals(22.0, t.widthPt, 1e-6)
        assertEquals(EditorA11y.ROTATE_STEP_DEGREES, t.rotationDegrees, 1e-9)
    }

    @Test
    fun the_bar_is_present_for_a_selection_and_absent_without_one() {
        val store = storeWithSelectedText()
        setBar(store)
        composeRule.onNodeWithTag(EditorContextBarTestTag).assertIsDisplayed()

        store.dispatch(Intent.ClearSelection)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorContextBarTestTag).assertDoesNotExist()
    }
}
