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
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtSize
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

    private fun setLayer(store: EditorStore, pageSizePt: PtSize? = null) {
        composeRule.setContent {
            MaterialTheme {
                ElementSemanticsLayer(
                    uiState = store.uiState.value, // snapshot for layout; the custom actions read the live store
                    dispatch = store::dispatch,
                    modifier = Modifier.size(200.dp, 200.dp),
                    pageSizePt = pageSizePt,
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

    /**
     * **OD-49's non-visual half.** The drawn warning is the only thing that ever said *"this will not
     * print"*, and P2b's device pass found it says nothing to the platform: 133 nodes in the
     * `AccessibilityNodeInfo` tree, not one mentioning the printer's reach. That mattered most on the
     * nudge path, which exists for people who cannot drag.
     *
     * Asserted through `stateDescription` rather than an announcement because the platform speaks a state
     * change on the focused node **and** re-reads it on every later focus — a maker who nudges past the
     * edge hears it happen, and one who arrives afterwards can still find out. Both directions are
     * checked: a state that is always present would be a nag, which is the failure D-032 named.
     */
    @Test
    fun an_element_inside_the_reach_carries_no_state_and_keeps_the_platforms_own() {
        val store = store()
        val id = store.uiState.value.selection.single()
        // 100×100pt page, 17pt inset; the element sits at x=40 and clears it.
        setLayer(store, PtSize(100.0, 100.0))
        val clear = composeRule.onNodeWithTag("$ElementNodeTagPrefix$id").fetchSemanticsNode()
        assertEquals(
            "an element well inside the reach must set no state of its own — Compose's `Selected` / " +
                "`Not selected` is then left to reach the platform, which for Role.Button is the only " +
                "way selection is spoken at all",
            null,
            clear.config.getOrElseNullable(SemanticsProperties.StateDescription) { null },
        )
    }

    @Test
    fun an_element_nudged_across_the_boundary_gains_the_spoken_state() {
        val store = store()
        val id = store.uiState.value.selection.single()
        // The device case, reproduced through the a11y path itself: nudges, not a drag. 4pt a step, from
        // x=40 to below the 17pt inset — and nothing about a nudge is ever "in flight".
        repeat(7) { store.dispatch(Intent.Nudge(PtPoint(-EditorA11y.NUDGE_STEP_PT, 0.0))) }
        assertTrue(
            "precondition: the nudges actually moved it across",
            store.uiState.value.document.pages[0].elements.single().transform.xPt < 17.0,
        )
        setLayer(store, PtSize(100.0, 100.0))
        val node = composeRule.onNodeWithTag("$ElementNodeTagPrefix$id").fetchSemanticsNode()
        assertEquals(
            // Carries "Selected" with it: an explicit state description replaces the platform's own, and
            // for Role.Button that default is the only channel selection ever reaches TalkBack through.
            Copy.A11y.outsidePrintReachState(selected = true),
            node.config.getOrElseNullable(SemanticsProperties.StateDescription) { null },
        )
        assertTrue(
            "the warning must not cost the maker the selection word",
            node.config.getOrElseNullable(SemanticsProperties.StateDescription) { null }
                ?.contains("Selected") == true,
        )
    }

    /**
     * The drawn mark needs a selection; the spoken one does not. A maker walking the page by touch meets
     * elements they have never selected, and *"this will be cut off"* is exactly what they are walking to
     * find out — so the state is stated regardless, with the platform's own selection word carried along.
     */
    @Test
    fun an_unselected_element_outside_the_reach_still_says_so_and_still_says_not_selected() {
        val store = store()
        val id = store.uiState.value.selection.single()
        repeat(7) { store.dispatch(Intent.Nudge(PtPoint(-EditorA11y.NUDGE_STEP_PT, 0.0))) }
        store.dispatch(Intent.ClearSelection)
        assertTrue("precondition: nothing is selected", store.uiState.value.selection.isEmpty())

        setLayer(store, PtSize(100.0, 100.0))
        val node = composeRule.onNodeWithTag("$ElementNodeTagPrefix$id").fetchSemanticsNode()
        assertEquals(
            Copy.A11y.outsidePrintReachState(selected = false),
            node.config.getOrElseNullable(SemanticsProperties.StateDescription) { null },
        )
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
