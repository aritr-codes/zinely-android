package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.StepSource
import com.aritr.zinely.core.model.Fit
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import kotlin.math.cos
import kotlin.math.sin

/**
 * Bench A48's turn knob ([TurnHandle], ADR-143 decision 1) through a real [EditorStore].
 *
 * Surface 200×200 dp at `screenPxPerPt = 2` (1 dp == 1 px under Robolectric's mdpi) ⇒ a 100×100 pt page. The
 * piece (40,20,20,20) pt is the box (80,40)–(120,80) px with its centre at (100,60); its knob hangs 54 below
 * the bottom edge at (100,134), 74 px from the centre, and its 48 px hit box is (76,110)–(124,158).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TurnHandleTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val box = Transform(40.0, 20.0, 20.0, 20.0)
    private val centre = Offset(100f, 60f)
    private val radius = 74f
    private val hitTopLeft = Offset(76f, 110f)

    private class Harness(val store: EditorStore) {
        val intents = mutableListOf<Intent>()
        var lastTurn: Map<String, Transform>? = null
        fun dispatch(i: Intent) { intents += i; store.dispatch(i) }
    }

    private fun newStore(rotation: Double = 0.0): EditorStore {
        val photo = ImageElement("p", box.copy(rotationDegrees = rotation), zIndex = 0, assetId = "a".repeat(64), fit = Fit.FILL)
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        return EditorStore(
            EditorModel(
                document = ZineDocument(
                    format = ZineFormat.SINGLE_SHEET_8,
                    paperSize = PaperSize.LETTER,
                    pages = listOf(Page(index = 0, role = PageRole.INTERIOR, elements = listOf(photo))),
                ),
                selection = setOf("p"),
                view = com.aritr.zinely.core.editor.ViewState(screenPxPerPt = 2f),
            ),
            scope, Dispatchers.Unconfined, runner,
        )
    }

    private fun show(h: Harness, obstacles: List<Rect> = emptyList()) {
        composeRule.setContent {
            ZinelyTheme {
                val state by h.store.uiState.collectAsState()
                TurnHandle(
                    uiState = state,
                    currentState = { h.store.uiState.value },
                    dispatch = h::dispatch,
                    onTurn = { h.lastTurn = it },
                    modifier = Modifier.size(200.dp, 200.dp),
                    obstaclesInWindow = obstacles,
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun photo(h: Harness) = h.store.uiState.value.document.pages[0].elements.single().transform

    /** The finger at angle [deg] round the centre (90 = straight down, where the knob starts), in the hit box's frame. */
    private fun at(deg: Double): Offset {
        val r = Math.toRadians(deg)
        return Offset(centre.x + radius * cos(r).toFloat(), centre.y + radius * sin(r).toFloat()) - hitTopLeft
    }

    /** Drag the knob round by [sweep] degrees in 2° steps, starting on it. */
    private fun TouchInjectionScope.turnBy(sweep: Double, startAt: Double = 90.0) {
        down(at(startAt))
        val steps = (kotlin.math.abs(sweep) / 2.0).toInt().coerceAtLeast(1)
        for (i in 1..steps) moveTo(at(startAt + sweep * i / steps))
    }

    @Test
    fun a_knob_drag_is_one_undo_step_that_writes_the_angle_alone() {
        val h = Harness(newStore())
        show(h)
        composeRule.onNodeWithTag(TurnHandleTestTag).performTouchInput { turnBy(40.0); up() }
        composeRule.waitForIdle()

        assertEquals(1, h.intents.count { it is Intent.BeginTransform })
        assertEquals(1, h.intents.count { it is Intent.CommitTransform })
        val t = photo(h)
        // x, y, width and height bit-identical: copy, not bake.
        assertEquals(box, t.copy(rotationDegrees = 0.0))
        assertEquals(40.0, t.rotationDegrees, 1.0)
        assertTrue(h.store.uiState.value.interaction is Interaction.Idle)
        assertNull("the preview is cleared on release", h.lastTurn)
        h.store.dispatch(Intent.Undo)
        assertEquals("one Undo takes the whole drag back", box, photo(h))
    }

    @Test
    fun the_knob_snaps_to_a_quarter_turn_exactly() {
        val h = Harness(newStore())
        show(h)
        composeRule.onNodeWithTag(TurnHandleTestTag).performTouchInput { turnBy(87.0); up() }
        composeRule.waitForIdle()
        assertEquals(90.0, photo(h).rotationDegrees, 0.0)
    }

    @Test
    fun a_full_turn_records_no_step() {
        val h = Harness(newStore())
        show(h)
        val revision = h.store.uiState.value.historyRevision
        composeRule.onNodeWithTag(TurnHandleTestTag).performTouchInput { turnBy(360.0); up() }
        composeRule.waitForIdle()
        assertEquals(box, photo(h))
        assertEquals(revision, h.store.uiState.value.historyRevision)
    }

    @Test
    fun no_jump_on_touch_a_touch_inside_the_slop_changes_nothing() {
        val h = Harness(newStore())
        show(h)
        composeRule.onNodeWithTag(TurnHandleTestTag).performTouchInput {
            down(center)
            moveBy(Offset(2f, 0f))
            up()
        }
        composeRule.waitForIdle()
        assertTrue(h.intents.none { it is Intent.BeginTransform })
        assertEquals(box, photo(h))
    }

    @Test
    fun a_half_pixel_jitter_on_a_3_degree_piece_changes_nothing_and_records_no_step() {
        val h = Harness(newStore(rotation = 3.0))
        show(h)
        val revision = h.store.uiState.value.historyRevision
        composeRule.onNodeWithTag(TurnHandleTestTag).performTouchInput {
            down(center)
            moveBy(Offset(0.5f, 0f))
            moveBy(Offset(-0.5f, 0f))
            up()
        }
        composeRule.waitForIdle()
        assertEquals(3.0, photo(h).rotationDegrees, 0.0)
        assertEquals(revision, h.store.uiState.value.historyRevision)
    }

    @Test
    fun a_mostly_radial_drag_past_slop_on_a_3_degree_piece_does_not_straighten_it() {
        val h = Harness(newStore(rotation = 3.0))
        show(h)
        composeRule.onNodeWithTag(TurnHandleTestTag).performTouchInput {
            down(center)
            moveBy(Offset(0f, 20f)) // Well past the slop, and straight away from the piece: almost no sweep.
            up()
        }
        composeRule.waitForIdle()
        assertTrue("snapped straight: ${photo(h).rotationDegrees}", photo(h).rotationDegrees > 1.0)
    }

    @Test
    fun the_knob_stands_aside_for_a_session_already_open() {
        val h = Harness(newStore())
        show(h)
        h.store.dispatch(Intent.BeginTransform(setOf("p")))
        val session = h.store.uiState.value.interaction
        composeRule.onNodeWithTag(TurnHandleTestTag).performTouchInput { turnBy(40.0); up() }
        composeRule.waitForIdle()
        assertTrue(h.intents.none { it is Intent.BeginTransform })
        assertEquals(session, h.store.uiState.value.interaction)
    }

    @Test
    fun chrome_arriving_under_a_resting_finger_does_not_move_the_knob_before_the_drag() {
        val h = Harness(newStore())
        var obstacles by mutableStateOf(emptyList<Rect>())
        composeRule.setContent {
            ZinelyTheme {
                val state by h.store.uiState.collectAsState()
                TurnHandle(
                    uiState = state,
                    currentState = { h.store.uiState.value },
                    dispatch = h::dispatch,
                    onTurn = { h.lastTurn = it },
                    modifier = Modifier.size(200.dp, 200.dp),
                    obstaclesInWindow = obstacles,
                )
            }
        }
        composeRule.waitForIdle()
        val before = composeRule.onNodeWithTag(TurnHandleTestTag).fetchSemanticsNode().boundsInRoot
        composeRule.onNodeWithTag(TurnHandleTestTag).performTouchInput { down(center) }
        composeRule.waitForIdle()
        obstacles = listOf(Rect(0f, 100f, 200f, 200f)) // Covers the knob's place below the piece.
        composeRule.waitForIdle()
        assertEquals(before, composeRule.onNodeWithTag(TurnHandleTestTag).fetchSemanticsNode().boundsInRoot)
        composeRule.onNodeWithTag(TurnHandleTestTag).performTouchInput { up() }
    }

    @Test
    fun the_readout_stays_clear_of_chrome_above_the_piece() {
        val h = Harness(newStore())
        val bar = Rect(0f, 0f, 200f, 30f)
        show(h, obstacles = listOf(bar))
        composeRule.onNodeWithTag(TurnHandleTestTag).performTouchInput { turnBy(30.0) }
        composeRule.waitForIdle()
        val pill = composeRule.onNodeWithTag(TurnReadoutTestTag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("pill at ${pill.top}", pill.top >= bar.bottom)
        composeRule.onNodeWithTag(TurnHandleTestTag).performTouchInput { up() }
    }

    @Test
    fun the_readout_shows_only_while_a_drag_is_live() {
        val h = Harness(newStore())
        show(h)
        composeRule.onNodeWithTag(TurnReadoutTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(TurnHandleTestTag).performTouchInput { turnBy(30.0) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TurnReadoutTestTag, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(TurnHandleTestTag).performTouchInput { up() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TurnReadoutTestTag, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun the_knob_is_not_a_clickable_node_in_the_accessibility_tree() {
        val h = Harness(newStore())
        show(h)
        composeRule.onNodeWithTag(TurnHandleTestTag, useUnmergedTree = true)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
            .assert(SemanticsMatcher.keyNotDefined(androidx.compose.ui.semantics.SemanticsProperties.ContentDescription))
    }

    @Test
    fun the_knob_is_hidden_in_reframe() {
        val reframing = Harness(newStore())
        reframing.store.dispatch(Intent.BeginReframe("p"))
        show(reframing)
        composeRule.onNodeWithTag(TurnHandleTestTag).assertDoesNotExist()
    }

    @Test
    fun the_knob_is_hidden_while_words_are_typed() {
        val store = newStore()
        store.dispatch(Intent.PlaceTextAndEdit(Transform(40.0, 20.0, 20.0, 20.0)))
        assertTrue(store.uiState.value.interaction is Interaction.EditingText)
        show(Harness(store))
        composeRule.onNodeWithTag(TurnHandleTestTag).assertDoesNotExist()
    }

    @Test
    fun the_knob_is_hidden_with_no_selection() {
        val store = newStore()
        store.dispatch(Intent.ClearSelection)
        show(Harness(store))
        composeRule.onNodeWithTag(TurnHandleTestTag).assertDoesNotExist()
    }

    @Test
    fun chrome_under_the_knob_with_no_room_above_means_the_knob_is_not_drawn() {
        // Below, the hit box (110..158) meets the chrome; above, the disc would sit 54 beyond the top edge
        // (y 40) at y −14, off the canvas. Neither side fits, so A48.2's one fallback: not drawn.
        val h = Harness(newStore())
        show(h, obstacles = listOf(Rect(0f, 100f, 200f, 200f)))
        composeRule.onNodeWithTag(TurnHandleTestTag).assertDoesNotExist()
    }

    // --- pure helpers ---------------------------------------------------------------------------------

    @Test
    fun the_knob_sits_54_beyond_the_bottom_edge_on_the_turned_normal() {
        val c = turnKnobCentrePx(box, 2.0, PtPoint(0.0, 0.0), 54f, top = false)
        assertEquals(100f, c.x, 1e-3f)
        assertEquals(134f, c.y, 1e-3f)
        // Turned 90° clockwise, "down" points to screen-left.
        val turned = turnKnobCentrePx(box.copy(rotationDegrees = 90.0), 2.0, PtPoint(0.0, 0.0), 54f, top = false)
        assertEquals(100f - 20f - 54f, turned.x, 1e-3f)
        assertEquals(60f, turned.y, 1e-3f)
        val up = turnKnobCentrePx(box, 2.0, PtPoint(0.0, 0.0), 54f, top = true)
        assertEquals(40f - 54f, up.y, 1e-3f)
    }

    @Test
    fun one_fallback_rule_below_then_above_then_not_drawn() {
        val visible = Rect(0f, 0f, 200f, 200f)
        val below = Rect(Offset(100f, 134f), 24f)
        val above = Rect(Offset(100f, 30f), 24f)
        assertEquals(false, turnKnobTop(below, above, visible, emptyList()))
        assertEquals(true, turnKnobTop(below, above, visible, listOf(Rect(0f, 150f, 200f, 200f))))
        assertNull(turnKnobTop(below, above, visible, listOf(Rect(0f, 150f, 200f, 200f), Rect(0f, 0f, 200f, 20f))))
        assertNull("off the visible canvas", turnKnobTop(below, above, Rect(0f, 40f, 200f, 150f), emptyList()))
    }

    @Test
    fun the_readout_is_whole_degrees_normalised_with_a_true_minus() {
        assertEquals("15°", turnReadout(375.0))
        assertEquals("−30°", turnReadout(-30.0))
        assertEquals("180°", turnReadout(-180.0))
        assertEquals("0°", turnReadout(360.0))
        assertEquals("90°", turnReadout(89.6))
    }

    @Test
    fun the_swept_angle_unwraps_across_the_back() {
        val c = Offset(0f, 0f)
        fun p(deg: Double) = Offset(cos(Math.toRadians(deg)).toFloat(), sin(Math.toRadians(deg)).toFloat())
        // 170° to 190° (written −170°) is +20 clockwise, not −340.
        assertEquals(20.0, sweptDegrees(p(170.0), p(-170.0), c), 1e-3)
        assertEquals(-20.0, sweptDegrees(p(-170.0), p(170.0), c), 1e-3)
    }

    // --- A48.6: the element's own Straighten action ------------------------------------------------------

    @Test
    fun straighten_is_an_element_action_only_while_tilted_and_sends_the_other_source() {
        val tilted = ImageElement("p", box.copy(rotationDegrees = -30.0), zIndex = 0, assetId = "a".repeat(64))
        val sent = mutableListOf<Intent>()
        val action = EditorA11y.elementCustomActions(tilted, { sent += it }).single { it.label == Copy.A11y.STRAIGHTEN }
        action.action()
        assertEquals(Intent.RotateBy(30.0, StepSource.OTHER), sent.last())
        listOf(0.0, 360.0).forEach { r ->
            val upright = tilted.copy(transform = box.copy(rotationDegrees = r))
            assertFalse(EditorA11y.elementCustomActions(upright, {}).any { it.label == Copy.A11y.STRAIGHTEN })
        }
    }
}
