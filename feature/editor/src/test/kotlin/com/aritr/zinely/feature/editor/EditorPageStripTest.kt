package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
 * The page navigator (docs/design/editor-visual-direction.md §4): tapping a card must dispatch the
 * SAME `Intent.GoToPage` the reducer already supports, against a real [EditorStore] — so we assert the
 * model's `currentPageIndex` actually moves. This is the functional unlock: every page reachable, not
 * just page 0. Robolectric NATIVE, same tier as [EditorContextBarTest].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorPageStripTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val scope = CoroutineScope(Dispatchers.Unconfined)

    private fun eightPageStore(): EditorStore {
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        val pages = (0 until ZineFormat.SINGLE_SHEET_8.pageCount)
            .map { Page(index = it, role = PageRole.INTERIOR) }
        return EditorStore(
            EditorModel(
                document = ZineDocument(
                    format = ZineFormat.SINGLE_SHEET_8,
                    paperSize = PaperSize.LETTER,
                    pages = pages,
                ),
            ),
            scope, Dispatchers.Unconfined, runner,
        )
    }

    private fun setStrip(store: EditorStore) {
        composeRule.setContent {
            MaterialTheme {
                val state by store.uiState.collectAsState()
                EditorPageStrip(
                    pageCount = state.document.pages.size,
                    currentPageIndex = state.currentPageIndex,
                    pageHasContent = { state.document.pages[it].elements.isNotEmpty() },
                    onSelectPage = { store.dispatch(Intent.GoToPage(it)) },
                )
            }
        }
    }

    @Test
    fun renders_one_card_per_page_with_page_one_current_initially() {
        val store = eightPageStore()
        setStrip(store)

        for (n in 1..8) composeRule.onNodeWithTag(editorPageCardTag(n)).assertExists()
        composeRule.onNodeWithTag(editorPageCardTag(1)).assertIsSelected()
    }

    @Test
    fun tapping_a_card_navigates_to_that_page_via_GoToPage() {
        val store = eightPageStore()
        setStrip(store)

        composeRule.onNodeWithTag(editorPageCardTag(5)).performClick()
        composeRule.waitForIdle()

        assertEquals(4, store.uiState.value.currentPageIndex)
        composeRule.onNodeWithTag(editorPageCardTag(5)).assertIsSelected()
    }
}
