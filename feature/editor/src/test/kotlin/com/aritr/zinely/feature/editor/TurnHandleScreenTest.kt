package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.ResizeHandle
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.cos
import kotlin.math.sin

/**
 * The knob's assembly on the real [EditorScreen] (A48.2, A48.7): what only the host knows is whether chrome
 * covers it. The per-gesture rules are [TurnHandleTest]'s.
 */
@RunWith(RobolectricTestRunner::class)
// Robolectric defaults to 320x470, where the open tray leaves the knob no room at all (A48.2 then hides it).
@Config(qualifiers = "w360dp-h740dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TurnHandleScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val pageSizePt = PtSize(100.0, 140.0)

    private fun storeWith(box: Transform): EditorStore {
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
        store.dispatch(Intent.PlaceText(box, "hi")) // auto-selects
        return store
    }

    private fun show(store: EditorStore, hintSeen: Boolean = true) {
        composeRule.setContent {
            ZinelyTheme {
                EditorScreen(
                    store = store,
                    pageSizePt = pageSizePt,
                    modifier = Modifier.size(360.dp, 740.dp),
                    moveResizeHintSeen = hintSeen,
                    onMoveResizeHintSeen = {},
                )
            }
        }
        composeRule.waitForIdle()
    }

    private val midPage = Transform(40.0, 40.0, 20.0, 20.0)

    @Test
    fun a_selected_piece_has_a_knob_and_a_drag_through_the_screen_is_one_undo_step() {
        val store = storeWith(midPage)
        show(store)
        val hit = composeRule.onNodeWithTag(TurnHandleTestTag).fetchSemanticsNode().boundsInRoot
        val scale = store.uiState.value.view.screenPxPerPt.toDouble()
        val knobToCentre = (10.0 * scale + 54.0 * composeRule.density.density).toFloat()
        composeRule.onNodeWithTag(TurnHandleTestTag).performTouchInput {
            val c = Offset(hit.width / 2f, hit.height / 2f - knobToCentre)
            fun at(deg: Double) = Offset(
                c.x + knobToCentre * cos(Math.toRadians(deg)).toFloat(),
                c.y + knobToCentre * sin(Math.toRadians(deg)).toFloat(),
            )
            down(at(90.0))
            for (d in 92..130 step 2) moveTo(at(d.toDouble()))
            up()
        }
        composeRule.waitForIdle()
        val t = store.uiState.value.document.pages[0].elements.single().transform
        assertEquals(40.0, t.rotationDegrees, 1.0)
        assertEquals(midPage, t.copy(rotationDegrees = 0.0))
        store.dispatch(Intent.Undo)
        assertEquals("one Undo", midPage, store.uiState.value.document.pages[0].elements.single().transform)
    }

    @Test
    fun a_second_finger_on_the_page_during_a_knob_drag_changes_nothing_but_the_turn() {
        val store = storeWith(midPage)
        show(store)
        val hit = composeRule.onNodeWithTag(TurnHandleTestTag).fetchSemanticsNode().boundsInRoot
        val scale = store.uiState.value.view.screenPxPerPt.toDouble()
        val knobToCentre = (10.0 * scale + 54.0 * composeRule.density.density).toFloat()
        composeRule.onNodeWithTag(TurnHandleTestTag).performTouchInput {
            val c = Offset(hit.width / 2f, hit.height / 2f - knobToCentre)
            fun at(deg: Double) = Offset(
                c.x + knobToCentre * cos(Math.toRadians(deg)).toFloat(),
                c.y + knobToCentre * sin(Math.toRadians(deg)).toFloat(),
            )
            down(0, at(90.0))
            for (d in 92..110 step 2) moveTo(0, at(d.toDouble()))
            // A finger on the piece itself, pinching and dragging: on the page surface it would begin a transform.
            down(1, c)
            moveTo(1, c + Offset(60f, 60f))
            for (d in 112..130 step 2) moveTo(0, at(d.toDouble()))
            up(1)
            up(0)
        }
        composeRule.waitForIdle()
        val t = store.uiState.value.document.pages[0].elements.single().transform
        assertEquals(40.0, t.rotationDegrees, 1.0)
        assertEquals(midPage, t.copy(rotationDegrees = 0.0))
        store.dispatch(Intent.Undo)
        assertEquals("one Undo", midPage, store.uiState.value.document.pages[0].elements.single().transform)
    }

    @Test
    fun a_knob_finger_and_a_page_finger_crossing_slop_in_one_event_leave_nothing_stuck() {
        val store = storeWith(midPage)
        show(store)
        val hit = composeRule.onNodeWithTag(TurnHandleTestTag).fetchSemanticsNode().boundsInRoot
        val scale = store.uiState.value.view.screenPxPerPt.toDouble()
        val knobToCentre = (10.0 * scale + 54.0 * composeRule.density.density).toFloat()
        composeRule.onNodeWithTag(TurnHandleTestTag).performTouchInput {
            val c = Offset(hit.width / 2f, hit.height / 2f - knobToCentre)
            fun at(deg: Double) = Offset(
                c.x + knobToCentre * cos(Math.toRadians(deg)).toFloat(),
                c.y + knobToCentre * sin(Math.toRadians(deg)).toFloat(),
            )
            down(0, at(90.0))
            down(1, c)
            // Both cross the slop in the one event, before any recomposition can take a surface away.
            updatePointerTo(0, at(120.0))
            updatePointerTo(1, c + Offset(40f, 40f))
            move()
        }
        composeRule.waitForIdle() // The recomposition that used to strand a surface's session.
        composeRule.onRoot().performTouchInput {
            up(1)
            up(0)
        }
        composeRule.waitForIdle()
        assertEquals(Interaction.Idle, store.uiState.value.interaction)
        composeRule.onNodeWithTag(TurnHandleTestTag).assertExists()
        composeRule.onNodeWithTag("$ResizeHandleTagPrefix${ResizeHandle.BOTTOM_RIGHT.name}").assertExists()
    }

    @Test
    fun a_second_finger_tapping_blank_paper_during_a_knob_drag_does_not_deselect() {
        val store = storeWith(midPage)
        show(store)
        val selection = store.uiState.value.selection
        val hit = composeRule.onNodeWithTag(TurnHandleTestTag).fetchSemanticsNode().boundsInRoot
        val scale = store.uiState.value.view.screenPxPerPt.toDouble()
        val knobToCentre = (10.0 * scale + 54.0 * composeRule.density.density).toFloat()
        composeRule.onNodeWithTag(TurnHandleTestTag).performTouchInput {
            val c = Offset(hit.width / 2f, hit.height / 2f - knobToCentre)
            fun at(deg: Double) = Offset(
                c.x + knobToCentre * cos(Math.toRadians(deg)).toFloat(),
                c.y + knobToCentre * sin(Math.toRadians(deg)).toFloat(),
            )
            down(0, at(90.0))
            for (d in 92..110 step 2) moveTo(0, at(d.toDouble()))
            down(1, c + Offset(0f, -70f * composeRule.density.density))
            up(1)
            advanceEventTime(1000)
            for (d in 112..130 step 2) moveTo(0, at(d.toDouble()))
            up(0)
        }
        composeRule.waitForIdle()
        assertEquals(selection, store.uiState.value.selection)
        assertEquals(40.0, store.uiState.value.document.pages[0].elements.single().transform.rotationDegrees, 1.0)
    }

    @Test
    fun a_handle_whose_gesture_restarts_mid_drag_leaves_no_session_and_the_knob_comes_back() {
        val store = storeWith(midPage)
        show(store)
        composeRule.onNodeWithTag("$ResizeHandleTagPrefix${ResizeHandle.BOTTOM_RIGHT.name}").performTouchInput {
            down(center)
            moveBy(Offset(30f, 30f))
        }
        composeRule.waitForIdle()
        assertEquals(Interaction.Transforming::class, store.uiState.value.interaction::class)
        // A second finger's tray tap does this: the moved piece changes the handle's pointerInput key.
        store.dispatch(Intent.Nudge(PtPoint(5.0, 0.0)))
        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { up() }
        composeRule.waitForIdle()
        assertEquals(Interaction.Idle, store.uiState.value.interaction)
        composeRule.onNodeWithTag(TurnHandleTestTag).assertExists()
    }

    @Test
    fun the_knob_stays_out_from_under_the_type_bar() {
        val store = storeWith(Transform(30.0, 60.0, 40.0, 12.0))
        show(store)
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.TEXT}").performClick()
        composeRule.waitForIdle()
        val bar = composeRule.onNodeWithTag("type-bar").fetchSemanticsNode().boundsInRoot
        val knobs = composeRule.onAllNodesWithTag(TurnHandleTestTag).fetchSemanticsNodes()
        knobs.forEach { assertFalse("the knob's hit box is under the bar", it.boundsInRoot.overlaps(bar)) }
    }

    @Test
    fun the_knob_goes_while_words_are_typed_and_comes_back_after() {
        val store = storeWith(midPage)
        show(store)
        composeRule.onNodeWithTag(TurnHandleTestTag).assertExists()
        store.dispatch(Intent.BeginEditText(store.uiState.value.selection.single()))
        composeRule.waitForIdle()
        val session = store.uiState.value.interaction as Interaction.EditingText
        composeRule.onNodeWithTag(TurnHandleTestTag).assertDoesNotExist()
        store.dispatch(Intent.CancelText(session.id, session.token))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TurnHandleTestTag).assertExists()
    }

    @Test
    fun the_first_run_hint_on_a_piece_at_the_top_means_no_knob_until_got_it() {
        // A piece at the very top of the page: its knob below lands on the hint, and above is off the canvas.
        val store = storeWith(Transform(40.0, 0.0, 20.0, 4.0))
        show(store, hintSeen = false)
        composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).assertExists()
        composeRule.onNodeWithTag(TurnHandleTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(MoveResizeHintDismissTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TurnHandleTestTag).assertExists()
    }

    @Test
    fun the_knob_stays_with_the_arrange_tray_open_and_the_tray_has_straighten() {
        val store = storeWith(Transform(40.0, 10.0, 20.0, 20.0))
        show(store)
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.ARRANGE}").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(Copy.A11y.STRAIGHTEN).assertExists()
        composeRule.onNodeWithTag(TurnHandleTestTag).assertExists()
    }

    @Test
    fun a_piece_at_the_foot_of_the_page_flips_its_knob_above_clear_of_the_verb_bar() {
        val box = Transform(40.0, 120.0, 20.0, 18.0)
        val store = storeWith(box)
        show(store)
        val hit = composeRule.onNodeWithTag(TurnHandleTestTag).fetchSemanticsNode().boundsInRoot
        val view = store.uiState.value.view
        val canvasTop = composeRule.onNodeWithTag(EditorCanvasTestTag).fetchSemanticsNode().boundsInRoot.top
        val pieceTop = canvasTop + ((box.yPt + view.pageOffset.y) * view.screenPxPerPt).toFloat()
        val density = composeRule.density.density
        assertEquals("the knob hangs 54dp above the top edge", pieceTop - 54f * density, (hit.top + hit.bottom) / 2f, 2f)
    }
}
