package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * ADR-123 on the semantics tree: an undo or redo raises the one Bench snack, its message is the one
 * live-region node that carries the line, it has no button, and an identical repeat is a new node.
 *
 * Structural only. It shows what the platform is handed, not what TalkBack says; that is the owner's listen.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NamedUndoSnackTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val effects = mutableListOf<Effect>()

    private fun store(): EditorStore = EditorStore(
        EditorModel(
            document = ZineDocument(
                format = ZineFormat.SINGLE_SHEET_8,
                paperSize = PaperSize.LETTER,
                pages = List(8) { Page(index = it, role = PageRole.INTERIOR) },
            ),
        ),
        CoroutineScope(Dispatchers.Unconfined),
        Dispatchers.Unconfined,
        object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) {
                effects += effect
            }
        },
    )

    private fun setScreen(store: EditorStore) {
        composeRule.setContent {
            ZinelyTheme { EditorScreen(store = store, pageSizePt = PtSize(100.0, 100.0), modifier = Modifier.size(360.dp, 720.dp)) }
        }
        composeRule.waitForIdle()
    }

    private fun place(store: EditorStore) = store.dispatch(Intent.PlaceText(Transform(20.0, 60.0, 60.0, 18.0), "hi"))

    private fun press(tag: String) {
        composeRule.onNodeWithTag(tag).performClick()
        composeRule.mainClock.advanceTimeBy(BenchSnackMillis + 100L)
        composeRule.waitForIdle()
    }

    /** Every live-region node on screen that says [line]. */
    private fun speakers(line: String) = composeRule.onAllNodes(
        SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion) and
            SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(line)),
        useUnmergedTree = true,
    ).fetchSemanticsNodes()

    @Test
    fun undo_names_what_came_back_in_the_one_snack_with_no_button() {
        val store = store()
        setScreen(store)
        place(store)
        composeRule.waitForIdle()
        press(BenchBarUndoTag)

        composeRule.onNodeWithTag(BenchSnackTestTag).assertIsDisplayed()
        assertEquals(1, composeRule.onAllNodesWithTag(BenchSnackTestTag).fetchSemanticsNodes().size)
        assertEquals("one live region carries the whole line", 1, speakers("Text taken off").size)
        composeRule.onNodeWithTag(BenchSnackActionTestTag).assertDoesNotExist()
        assertEquals("nothing else announces it", emptyList<Effect>(), effects.filterNot { it is Effect.Autosave })
    }

    @Test
    fun redo_says_its_forward_line_in_the_same_snack() {
        val store = store()
        setScreen(store)
        place(store)
        composeRule.waitForIdle()
        press(BenchBarUndoTag)
        press(BenchBarRedoTag)

        assertEquals(1, speakers("Text added").size)
        assertEquals(0, speakers("Text taken off").size)
        assertEquals(1, composeRule.onAllNodesWithTag(BenchSnackTestTag).fetchSemanticsNodes().size)
        composeRule.onNodeWithTag(BenchSnackActionTestTag).assertDoesNotExist()
    }

    @Test
    fun a_step_that_changes_page_carries_the_page_in_the_same_line() {
        val store = store()
        setScreen(store)
        place(store)
        store.dispatch(Intent.GoToPage(3))
        composeRule.waitForIdle()
        press(BenchBarUndoTag)

        assertEquals(0, store.uiState.value.currentPageIndex)
        assertEquals(1, speakers("Text taken off, page 1").size)
        assertEquals(0, speakers("Text taken off").size)
    }

    @Test
    fun an_identical_repeat_is_a_new_snack_node() {
        val store = store()
        setScreen(store)
        place(store)
        place(store)
        composeRule.waitForIdle()

        press(BenchBarUndoTag)
        val first = speakers("Text taken off").single().id
        press(BenchBarUndoTag)
        val second = speakers("Text taken off").single().id
        assertNotEquals("the same line must arrive on a fresh node, or a live region stays silent", first, second)
    }

    @Test
    fun the_snack_stands_for_the_frozen_window_then_leaves() {
        val store = store()
        setScreen(store)
        place(store)
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        press(BenchBarUndoTag)
        composeRule.mainClock.advanceTimeBy(2800L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchSnackTestTag).assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(800L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchSnackTestTag).assertDoesNotExist()
    }

    @Test
    fun an_undo_snack_rises_in_from_below_like_the_delete_snack() {
        // The step keys a fresh BenchSnack whose progress starts at 0, so it must still play A26's entrance
        // (the frozen 8dp rise) rather than appear already at rest.
        val store = store()
        setScreen(store)
        place(store)
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag(BenchBarUndoTag).performClick()
        repeat(4) {
            if (composeRule.onAllNodesWithTag(BenchSnackTestTag).fetchSemanticsNodes().isEmpty()) {
                composeRule.mainClock.advanceTimeByFrame()
                composeRule.waitForIdle()
            }
        }
        val entering = composeRule.onNodeWithTag(BenchSnackTestTag).fetchSemanticsNode().boundsInRoot
        composeRule.mainClock.advanceTimeBy(BenchSnackMillis + 200L)
        composeRule.waitForIdle()
        val resting = composeRule.onNodeWithTag(BenchSnackTestTag).fetchSemanticsNode().boundsInRoot

        val rise = entering.top - resting.top
        val eightDp = with(composeRule.density) { 8.dp.toPx() }
        assertTrue("the snack must enter from below: entering=$entering resting=$resting", rise > eightDp / 2)
        assertTrue("...and by no more than the frozen 8dp: $rise", rise <= eightDp + 0.5f)
    }

    @Test
    fun a_redo_soon_after_an_undo_gets_its_own_full_window() {
        // The undo's hide timer must not take the redo's line down early.
        val store = store()
        setScreen(store)
        place(store)
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        press(BenchBarUndoTag)
        composeRule.mainClock.advanceTimeBy(2000L)
        press(BenchBarRedoTag)
        composeRule.mainClock.advanceTimeBy(2800L)
        composeRule.waitForIdle()
        assertEquals(1, speakers("Text added").size)
        composeRule.mainClock.advanceTimeBy(800L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchSnackTestTag).assertDoesNotExist()
    }
}
