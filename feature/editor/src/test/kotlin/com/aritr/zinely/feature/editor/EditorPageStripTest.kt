package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
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
    private val pageSizePt = PtSize(100.0, 130.0)

    private fun eightPageStore(): EditorStore {
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        // Page 1 holds an element (so its thumbnail renders content); the rest are empty sheets.
        val pages = (0 until ZineFormat.SINGLE_SHEET_8.pageCount).map { i ->
            val elements = if (i == 0) {
                listOf(TextElement(id = "t1", transform = Transform(10.0, 10.0, 40.0, 20.0), text = "hi"))
            } else {
                emptyList()
            }
            Page(index = i, role = PageRole.INTERIOR, elements = elements)
        }
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
                    pages = state.document.pages,
                    currentPageIndex = state.currentPageIndex,
                    pageSizePt = pageSizePt,
                    defaults = state.document.defaults,
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
    fun every_card_renders_a_live_page_thumbnail() {
        // The visual upgrade: each card hosts a mini-render of its page (the SceneRenderer→PagePreview
        // path), content pages and empty pages alike. Asserting the per-card thumb node exists proves the
        // render path is wired for all eight without crashing — the page-1 element exercises a content
        // tape, the rest exercise the blank-sheet path.
        val store = eightPageStore()
        setStrip(store)

        // The thumbnail node is decorative — it merges into the card's selectable (Role.Tab) semantics,
        // so it lives in the unmerged tree (intended: the card owns the a11y label, not the mini-render).
        for (n in 1..8) {
            composeRule.onNodeWithTag(editorPageThumbTag(n), useUnmergedTree = true).assertExists()
        }
    }

    @Test
    fun cards_keep_the_frozen_page_picker_a11y_and_target_contract() {
        // Behavior+a11y are frozen across the mini-render restyle: each card stays a "Page N" picker
        // entry (TalkBack label, by content description) on a ≥48dp target — the thumbnail is decorative
        // and must not have eroded the navigation contract.
        val store = eightPageStore()
        setStrip(store)

        for (n in 1..8) {
            composeRule.onNodeWithContentDescription("Page $n")
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
        }
        // Selected-state reporting still tracks the current page.
        composeRule.onNodeWithContentDescription("Page 1").assertIsSelected()
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
