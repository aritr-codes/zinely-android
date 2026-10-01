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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

/**
 * ADR-123 on the semantics tree and at the platform boundary. The snack's message is one live-region node
 * that is never replaced; a forward snack (the delete) reaches it as a description added to it. An undo or
 * redo raises the same snack with no button, but its line is spoken once through the host's announcement
 * drain, an identical repeat included, and the live region stays silent for it.
 *
 * Structural only. It shows what the platform is handed, not what TalkBack says; that is the owner's listen.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NamedUndoSnackTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val effects = mutableListOf<Effect>()

    /** What the host's announcement drain was asked to speak, in order. */
    private val announced = mutableListOf<String>()

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
            ZinelyTheme {
                EditorScreen(
                    store = store,
                    pageSizePt = PtSize(100.0, 100.0),
                    modifier = Modifier.size(360.dp, 720.dp),
                    onHistoryAnnounce = { announced += it },
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun place(store: EditorStore) = store.dispatch(Intent.PlaceText(Transform(20.0, 60.0, 60.0, 18.0), "hi"))

    private fun press(tag: String) {
        composeRule.onNodeWithTag(tag).performClick()
        composeRule.mainClock.advanceTimeBy(BenchSnackMillis + 100L)
        composeRule.waitForIdle()
    }

    private fun described(line: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(line))

    /** Every live-region node on screen that says [line]. */
    private fun speakers(line: String) = composeRule.onAllNodes(
        SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion) and described(line),
        useUnmergedTree = true,
    ).fetchSemanticsNodes()

    /** Every node that carries [line] for a finger without being a live region: the pill, for an undo line. */
    private fun shown(line: String) = composeRule.onAllNodes(
        SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion) and described(line),
        useUnmergedTree = true,
    ).fetchSemanticsNodes()

    /** The snack's one live-region node. It is in the tree whether or not a snack is up. */
    private fun voice() = composeRule.onAllNodesWithTag(BenchSnackVoiceTestTag, useUnmergedTree = true)
        .fetchSemanticsNodes().single()

    /** Exactly what that node's description is; null while it has none. */
    private fun said(): String? = voice().config.getOrNull(SemanticsProperties.ContentDescription)?.single()

    /** Report accessibility as on, as with TalkBack running: Compose builds node infos and sends events only then. */
    private fun turnAccessibilityOn() {
        val manager = composeRule.activity.getSystemService(AccessibilityManager::class.java)
        shadowOf(manager).setEnabled(true)
        shadowOf(manager).setEnabledAccessibilityServiceList(listOf(AccessibilityServiceInfo()))
        shadowOf(manager).setTouchExplorationEnabled(true)
    }

    @Test
    fun the_voice_is_in_the_tree_before_any_snack_and_says_nothing() {
        turnAccessibilityOn()
        setScreen(store())

        assertTrue(voice().config.contains(SemanticsProperties.LiveRegion))
        assertEquals("no description at all: even an empty one is a TalkBack focus stop", null, said())
        assertTrue("one pixel, so it covers almost nothing beneath it", voice().boundsInRoot.let { it.width <= 1f && it.height <= 1f })
        assertTrue("...but not empty, or the platform tree drops it", !voice().boundsInRoot.isEmpty)
        composeRule.onNodeWithTag(BenchSnackTestTag).assertDoesNotExist()

        // The platform's own node, which is what TalkBack reads. A zero alpha would hide it here.
        val content = composeRule.activity.findViewById<ViewGroup>(android.R.id.content)
        val composeView = (content.getChildAt(0) as ViewGroup).getChildAt(0)
        val info = checkNotNull(composeView.accessibilityNodeProvider.createAccessibilityNodeInfo(voice().id))
        assertTrue("the standing node must be visible to the user", info.isVisibleToUser)
        assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE, info.liveRegion)
        assertEquals(null, info.contentDescription)
        assertTrue("and no one can land on it", !info.isScreenReaderFocusable && !info.isFocusable)
    }

    @Test
    fun the_delete_speaks_on_the_standing_node_and_undo_and_redo_through_the_drain() {
        val store = store()
        setScreen(store)
        val node = voice().id
        place(store)
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false

        // A forward snack: the live region says it, on the node that was already there. The drain is not used.
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.DELETE}").performClick()
        composeRule.mainClock.advanceTimeBy(BenchDeleteFadeMillis + BenchSnackMillis + 100L)
        composeRule.waitForIdle()
        assertEquals("the delete snack keeps its words", "Text deleted.", said())
        assertEquals(node, voice().id)
        assertEquals(1, speakers("Text deleted.").size)
        assertEquals(0, shown("Text deleted.").size)
        assertEquals(emptyList<String>(), announced)
        // Its Undo is a sibling of the message, outside the live region.
        val action = composeRule.onNodeWithTag(BenchSnackActionTestTag, useUnmergedTree = true).fetchSemanticsNode()
        assertTrue(!action.config.contains(SemanticsProperties.LiveRegion))
        assertEquals(voice().parent?.id, action.parent?.id)
        assertTrue(voice().children.isEmpty())

        // An undo: the drain says it once, the live region says nothing, and the pill still carries the words.
        press(BenchBarUndoTag)
        assertEquals(listOf("Text put back"), announced)
        assertEquals("the live region must not say it as well", null, said())
        assertEquals(
            composeRule.onNodeWithTag(BenchSnackTestTag, useUnmergedTree = true).fetchSemanticsNode().id,
            shown("Text put back").single().id,
        )
        assertEquals(node, voice().id)

        press(BenchBarRedoTag)
        assertEquals(listOf("Text put back", "Text removed"), announced)
        assertEquals(null, said())
        assertEquals(1, shown("Text removed").size)
        assertEquals(node, voice().id)

        composeRule.mainClock.advanceTimeBy(BenchSnackDeleteMillis + BenchSnackMillis + 100L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchSnackTestTag).assertDoesNotExist()
        assertEquals(null, said())
        assertEquals(0, shown("Text removed").size)
        assertEquals(node, voice().id)
        assertEquals("no effect announces any of it", emptyList<Effect>(), effects.filterNot { it is Effect.Autosave })
    }

    @Test
    fun a_forward_snack_right_after_an_undo_speaks_for_itself_again() {
        // An undo line is quiet in the live region; the next forward snack must not inherit that.
        val store = store()
        setScreen(store)
        place(store)
        place(store)
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        press(BenchBarUndoTag)
        assertEquals(null, said())

        store.dispatch(Intent.Select(store.uiState.value.document.pages[0].elements.single().id))
        composeRule.mainClock.advanceTimeBy(200L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.DELETE}").performClick()
        composeRule.mainClock.advanceTimeBy(BenchDeleteFadeMillis + BenchSnackMillis + 100L)
        composeRule.waitForIdle()
        assertEquals("Text deleted.", said())
        assertEquals(listOf("Text taken off"), announced)
    }

    @Test
    fun the_platform_hears_the_delete_from_the_live_region_and_each_undo_from_the_drain_only() {
        // What TalkBack is actually sent. Compose reports a description added to a node it already knew as
        // CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION carrying the words. For an undo line that event must not
        // come from the live region, or the line would be spoken twice.
        turnAccessibilityOn()
        val fromVoice = mutableListOf<String>()
        composeRule.activity.findViewById<View>(android.R.id.content).accessibilityDelegate =
            object : View.AccessibilityDelegate() {
                override fun onRequestSendAccessibilityEvent(host: ViewGroup, child: View, event: AccessibilityEvent): Boolean {
                    if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                        event.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION &&
                        shadowOf(event).virtualDescendantId == voice().id
                    ) {
                        fromVoice += event.contentDescription?.toString().orEmpty()
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
            // Long enough for Compose's accessibility checks, some of which ride a 100 ms loop.
            repeat(4) {
                composeRule.mainClock.advanceTimeBy(150L)
                composeRule.waitForIdle()
            }
        }
        settle()
        fromVoice.clear()

        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.DELETE}").performClick()
        composeRule.mainClock.advanceTimeBy(BenchDeleteFadeMillis + 50L)
        settle()
        assertEquals("the delete snack, from the live region", listOf("Text deleted."), fromVoice.filter { it.isNotBlank() })
        assertEquals(emptyList<String>(), announced)

        fromVoice.clear()
        press(BenchBarUndoTag)
        settle()
        press(BenchBarUndoTag)
        settle()
        press(BenchBarUndoTag)
        settle()
        assertEquals("the live region says no undo line", emptyList<String>(), fromVoice.filter { it.isNotBlank() })
        assertEquals(
            "three undos, the last two the same words, each spoken once through the drain",
            listOf("Text put back", "Text taken off", "Text taken off"),
            announced,
        )
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
        assertEquals("one speaker: the drain, once", listOf("Text taken off"), announced)
        assertEquals("...and not the live region as well", null, said())
        assertEquals("the pill still carries the whole line", 1, shown("Text taken off").size)
        composeRule.onNodeWithTag(BenchSnackActionTestTag).assertDoesNotExist()
        assertEquals("no effect announces it", emptyList<Effect>(), effects.filterNot { it is Effect.Autosave })
    }

    @Test
    fun redo_says_its_forward_line_in_the_same_snack() {
        val store = store()
        setScreen(store)
        place(store)
        composeRule.waitForIdle()
        press(BenchBarUndoTag)
        press(BenchBarRedoTag)

        assertEquals(listOf("Text taken off", "Text added"), announced)
        assertEquals(null, said())
        assertEquals(1, shown("Text added").size)
        assertEquals(0, shown("Text taken off").size)
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
        assertEquals("the page rides in the one spoken line", listOf("Text taken off, page 1"), announced)
        assertEquals(null, said())
        assertEquals(1, shown("Text taken off, page 1").size)
    }

    @Test
    fun an_identical_repeat_is_spoken_again() {
        // The device gate: TalkBack read a live region's line once and stayed silent on the same line again.
        val store = store()
        setScreen(store)
        place(store)
        place(store)
        composeRule.waitForIdle()
        val node = voice().id

        press(BenchBarUndoTag)
        press(BenchBarUndoTag)
        assertEquals(listOf("Text taken off", "Text taken off"), announced)
        assertEquals(1, shown("Text taken off").size)
        assertEquals(null, said())
        assertEquals(node, voice().id)
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
        assertEquals(1, shown("Text added").size)
        composeRule.mainClock.advanceTimeBy(800L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchSnackTestTag).assertDoesNotExist()
    }
}
