package com.aritr.zinely.feature.editor

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

/**
 * ADR-123 on the semantics tree: an undo or redo raises the one Bench snack, its message is the one
 * live-region node that carries the line, it has no button, and that node is never replaced: every line,
 * the delete snack's and an identical repeat included, reaches it as a change to its description.
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

    /** Every live-region node on screen that says [line]. A repeat's trailing space is not part of the words. */
    private fun speakers(line: String) = composeRule.onAllNodes(
        SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion) and
            SemanticsMatcher("says \"$line\"") {
                it.config.getOrNull(SemanticsProperties.ContentDescription)?.singleOrNull()?.trimEnd() == line
            },
        useUnmergedTree = true,
    ).fetchSemanticsNodes()

    /** The snack's one live-region node. It is in the tree whether or not a snack is up. */
    private fun voice() = composeRule.onAllNodesWithTag(BenchSnackVoiceTestTag, useUnmergedTree = true)
        .fetchSemanticsNodes().single()

    /** Exactly what the platform is handed as that node's description. */
    private fun said(): String = voice().config[SemanticsProperties.ContentDescription].single()

    @Test
    fun the_voice_is_in_the_tree_before_any_snack_and_says_nothing() {
        setScreen(store())

        assertTrue(voice().config.contains(SemanticsProperties.LiveRegion))
        assertEquals("", said())
        assertTrue("one pixel, so it covers nothing beneath it", voice().boundsInRoot.let { it.width <= 1f && it.height <= 1f })
        assertTrue("...but not empty, or the platform tree drops it", !voice().boundsInRoot.isEmpty)
        composeRule.onNodeWithTag(BenchSnackTestTag).assertDoesNotExist()
    }

    @Test
    fun one_node_says_the_delete_the_undo_and_the_redo_and_is_never_replaced() {
        // The first device gate: a live-region node that appears already holding its line is not read out.
        // Every line must reach the node that was there before it, as a change to its description.
        val store = store()
        setScreen(store)
        val node = voice().id
        place(store)
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.DELETE}").performClick()
        composeRule.mainClock.advanceTimeBy(BenchDeleteFadeMillis + BenchSnackMillis + 100L)
        composeRule.waitForIdle()
        assertEquals("the delete snack keeps its words", "Text deleted.", said())
        assertEquals(node, voice().id)
        assertEquals(1, speakers("Text deleted.").size)
        // Its Undo is a sibling of the message, outside the live region.
        val action = composeRule.onNodeWithTag(BenchSnackActionTestTag, useUnmergedTree = true).fetchSemanticsNode()
        assertTrue(!action.config.contains(SemanticsProperties.LiveRegion))
        assertEquals(voice().parent?.id, action.parent?.id)
        assertTrue(voice().children.isEmpty())

        press(BenchBarUndoTag)
        assertEquals("Text put back", said().trimEnd())
        assertEquals(node, voice().id)
        assertEquals(1, speakers("Text put back").size)
        assertEquals(0, speakers("Text deleted.").size)

        press(BenchBarRedoTag)
        assertEquals("Text removed", said().trimEnd())
        assertEquals(node, voice().id)

        composeRule.mainClock.advanceTimeBy(BenchSnackDeleteMillis + BenchSnackMillis + 100L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchSnackTestTag).assertDoesNotExist()
        assertEquals("down again, it says nothing, so the next line is a change", "", said())
        assertEquals(node, voice().id)
        assertEquals("nothing else announces any of it", emptyList<Effect>(), effects.filterNot { it is Effect.Autosave })
    }

    @Test
    fun the_platform_is_told_each_line_as_a_change_to_the_description() {
        // What TalkBack is actually sent. Compose reports a changed description on a node it already knew as
        // CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION carrying the words; a node that appears holding its words
        // gets only a subtree change from an ancestor, which is what the first device gate heard as silence.
        val manager = composeRule.activity.getSystemService(AccessibilityManager::class.java)
        shadowOf(manager).setEnabled(true)
        shadowOf(manager).setEnabledAccessibilityServiceList(listOf(AccessibilityServiceInfo()))
        shadowOf(manager).setTouchExplorationEnabled(true)
        val described = mutableListOf<String>()
        composeRule.activity.findViewById<View>(android.R.id.content).accessibilityDelegate =
            object : View.AccessibilityDelegate() {
                override fun onRequestSendAccessibilityEvent(host: ViewGroup, child: View, event: AccessibilityEvent): Boolean {
                    if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                        event.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION
                    ) {
                        described += event.contentDescription?.toString().orEmpty()
                    }
                    return true
                }
            }
        val store = store()
        setScreen(store)
        place(store)
        place(store)
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        fun settle() {
            // Compose batches accessibility events on a 100 ms loop.
            repeat(4) {
                composeRule.mainClock.advanceTimeBy(150L)
                composeRule.waitForIdle()
            }
        }
        settle()
        described.clear()

        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.DELETE}").performClick()
        composeRule.mainClock.advanceTimeBy(BenchDeleteFadeMillis + 50L)
        settle()
        assertEquals("the delete snack", listOf("Text deleted."), described.filter { it.isNotBlank() })

        described.clear()
        press(BenchBarUndoTag)
        settle()
        press(BenchBarUndoTag)
        settle()
        press(BenchBarUndoTag)
        settle()
        assertEquals(
            "three undos, the last two the same words, each told to the platform once",
            listOf("Text put back", "Text taken off", "Text taken off"),
            described.filter { it.isNotBlank() }.map { it.trimEnd() },
        )
    }

    @Test
    fun a_spoken_line_always_differs_from_the_one_before_it() {
        val line = "Text taken off"
        assertEquals("", benchSnackSpoken(visible = false, message = line, step = 3))
        for (step in 0..5) {
            val now = benchSnackSpoken(visible = true, message = line, step = step)
            assertEquals(line, now.trimEnd())
            assertNotEquals(now, benchSnackSpoken(visible = true, message = line, step = step + 1))
        }
    }

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
    fun an_identical_repeat_is_a_new_update_on_the_same_node() {
        val store = store()
        setScreen(store)
        place(store)
        place(store)
        composeRule.waitForIdle()

        press(BenchBarUndoTag)
        val first = speakers("Text taken off").single().id
        val firstSaid = said()
        press(BenchBarUndoTag)
        val second = speakers("Text taken off").single().id
        assertEquals("the node is kept: a new one is not read out", first, second)
        assertNotEquals("the same words must still be a change, or a live region stays silent", firstSaid, said())
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
