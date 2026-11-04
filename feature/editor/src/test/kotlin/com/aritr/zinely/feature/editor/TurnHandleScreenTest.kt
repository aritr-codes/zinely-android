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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
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
import org.junit.Assert.assertTrue
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

    /** Twist two fingers by 40° round [c], starting at the root-space points [a] and [b]. */
    private fun twist(c: Offset, a: Offset, b: Offset, step: Int = 4) {
        fun turned(p: Offset, deg: Double): Offset {
            val r = Math.toRadians(deg)
            val v = p - c
            return c + Offset((v.x * cos(r) - v.y * sin(r)).toFloat(), (v.x * sin(r) + v.y * cos(r)).toFloat())
        }
        composeRule.onRoot().performTouchInput {
            down(0, a)
            down(1, b)
            for (d in step..40 step step) {
                updatePointerTo(0, turned(a, d.toDouble()))
                updatePointerTo(1, turned(b, d.toDouble()))
                move()
            }
            up(1)
            up(0)
        }
        composeRule.waitForIdle()
    }

    private fun EditorStore.piece() = uiState.value.document.pages[0].elements.single().transform

    /** The piece's centre on screen, in root space. */
    private fun pieceCentre(store: EditorStore, box: Transform): Offset {
        val view = store.uiState.value.view
        val canvas = composeRule.onNodeWithTag(EditorCanvasTestTag).fetchSemanticsNode().boundsInRoot
        return Offset(
            canvas.left + ((box.xPt + box.widthPt / 2 + view.pageOffset.x) * view.screenPxPerPt).toFloat(),
            canvas.top + ((box.yPt + box.heightPt / 2 + view.pageOffset.y) * view.screenPxPerPt).toFloat(),
        )
    }

    private val tinyPiece = Transform(45.0, 60.0, 8.0, 8.0)

    @Test
    fun two_fingers_on_blank_paper_turn_the_selected_piece() {
        val store = storeWith(tinyPiece)
        show(store)
        val c = pieceCentre(store, tinyPiece)
        val up = 150f * composeRule.density.density
        twist(c, c + Offset(-100f, -up), c + Offset(100f, -up))
        assertEquals(40.0, store.piece().rotationDegrees, 1.0)
        assertEquals(tinyPiece.widthPt, store.piece().widthPt, 0.01)
    }

    @Test
    fun two_fingers_on_a_small_piece_turn_it_though_they_land_on_its_handles() {
        val store = storeWith(tinyPiece)
        show(store)
        val c = pieceCentre(store, tinyPiece)
        val d = 12f * composeRule.density.density
        twist(c, c + Offset(-d, -d), c + Offset(d, d))
        assertEquals(40.0, store.piece().rotationDegrees, 1.0)
        assertEquals("not a resize", tinyPiece.widthPt, store.piece().widthPt, 0.01)
    }

    @Test
    fun one_finger_on_a_handle_and_one_on_the_paper_turn_the_piece_and_do_not_resize_it() {
        val store = storeWith(midPage)
        show(store)
        val handle = composeRule.onNodeWithTag("$ResizeHandleTagPrefix${ResizeHandle.BOTTOM_RIGHT.name}").fetchSemanticsNode().boundsInRoot
        val c = pieceCentre(store, midPage)
        twist(c, handle.center, c - (handle.center - c) * 3f)
        assertEquals(40.0, store.piece().rotationDegrees, 1.0)
        assertEquals("not a resize", midPage.widthPt, store.piece().widthPt, 0.01)
    }

    @Test
    fun one_finger_on_the_knob_and_one_on_the_paper_are_a_twist_and_one_undo() {
        val store = storeWith(midPage)
        show(store)
        val knob = composeRule.onNodeWithTag(TurnHandleTestTag).fetchSemanticsNode().boundsInRoot
        val c = pieceCentre(store, midPage)
        twist(c, knob.center, c - (knob.center - c))
        assertEquals(40.0, store.piece().rotationDegrees, 1.0)
        store.dispatch(Intent.Undo)
        assertEquals("one Undo", midPage, store.piece())
    }

    @Test
    fun a_fast_twist_from_the_knob_is_not_lost_to_the_page_gesture() {
        val store = storeWith(midPage)
        show(store)
        val knob = composeRule.onNodeWithTag(TurnHandleTestTag).fetchSemanticsNode().boundsInRoot
        val c = pieceCentre(store, midPage)
        twist(c, knob.center, c - (knob.center - c), step = 8)
        assertEquals(40.0, store.piece().rotationDegrees, 1.0)
    }

    @Test
    fun a_third_finger_on_the_paper_does_not_end_a_twist() {
        val store = storeWith(tinyPiece)
        show(store)
        val c = pieceCentre(store, tinyPiece)
        val r = 40f * composeRule.density.density
        fun at(deg: Double, radius: Float = r) = c + Offset(
            radius * cos(Math.toRadians(deg)).toFloat(),
            radius * sin(Math.toRadians(deg)).toFloat(),
        )
        composeRule.onRoot().performTouchInput {
            down(0, at(0.0))
            down(1, at(180.0))
            for (d in 4..20 step 4) {
                updatePointerTo(0, at(d.toDouble()))
                updatePointerTo(1, at(180.0 + d))
                move()
            }
            down(2, c + Offset(0f, -150f * composeRule.density.density))
            updatePointerTo(2, c + Offset(40f * composeRule.density.density, -150f * composeRule.density.density))
            for (d in 24..40 step 4) {
                updatePointerTo(0, at(d.toDouble()))
                updatePointerTo(1, at(180.0 + d))
                move()
            }
            up(2)
            up(1)
            up(0)
        }
        composeRule.waitForIdle()
        assertTrue("still a turn, was ${store.piece().rotationDegrees}", store.piece().rotationDegrees > 15.0)
    }

    @Test
    fun a_second_finger_on_the_paper_during_a_handle_drag_changes_nothing_but_the_resize() {
        val store = storeWith(midPage)
        show(store)
        val handle = composeRule.onNodeWithTag("$ResizeHandleTagPrefix${ResizeHandle.BOTTOM_RIGHT.name}").fetchSemanticsNode().boundsInRoot
        val c = pieceCentre(store, midPage)
        val paper = c + Offset(0f, -150f * composeRule.density.density)
        composeRule.onRoot().performTouchInput {
            down(0, handle.center)
            moveTo(0, handle.center + Offset(30f, 30f))
            down(1, paper)
            for (i in 1..5) {
                updatePointerTo(0, handle.center + Offset(30f + 5f * i, 30f + 5f * i))
                updatePointerTo(1, paper + Offset(20f * i, 0f))
                move()
            }
            up(1)
            up(0)
        }
        composeRule.waitForIdle()
        assertEquals(0.0, store.piece().rotationDegrees, 0.0)
        assertTrue("resized", store.piece().widthPt > midPage.widthPt)
        store.dispatch(Intent.Undo)
        assertEquals("one Undo", midPage, store.piece())
    }

    @Test
    fun a_second_finger_joining_a_body_drag_turns_the_piece_too() {
        val store = storeWith(midPage)
        show(store)
        val c = pieceCentre(store, midPage)
        val r = 100f * composeRule.density.density
        fun at(deg: Double) = c + Offset(r * cos(Math.toRadians(deg)).toFloat(), r * sin(Math.toRadians(deg)).toFloat())
        composeRule.onRoot().performTouchInput {
            down(0, c)
            moveTo(0, c + Offset(40f, 0f))
            down(1, at(180.0))
            for (d in 4..40 step 4) {
                updatePointerTo(1, at(180.0 + d))
                move()
            }
            up(1)
            up(0)
        }
        composeRule.waitForIdle()
        assertTrue("turned, was ${store.piece().rotationDegrees}", store.piece().rotationDegrees > 5.0)
        store.dispatch(Intent.Undo)
        assertEquals("one Undo", midPage, store.piece())
    }

    @Test
    fun a_tap_on_blank_paper_after_a_twist_still_deselects() {
        val store = storeWith(tinyPiece)
        show(store)
        val c = pieceCentre(store, tinyPiece)
        val d = 12f * composeRule.density.density
        twist(c, c + Offset(-d, -d), c + Offset(d, d))
        assertEquals(1, store.uiState.value.selection.size)
        composeRule.onRoot().performTouchInput { click(c + Offset(0f, -150f * composeRule.density.density)) }
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        assertEquals(0, store.uiState.value.selection.size)
    }

    @Test
    fun two_resting_fingers_that_wobble_a_little_change_nothing() {
        val store = storeWith(tinyPiece)
        show(store)
        val c = pieceCentre(store, tinyPiece)
        val dp = composeRule.density.density
        val a = c + Offset(-30f * dp, 0f)
        val b = c + Offset(30f * dp, 0f)
        composeRule.onRoot().performTouchInput {
            down(0, a)
            down(1, b)
            // 1.5dp each way: the fingers' spread changes by 5% and their line by under 3 degrees.
            updatePointerTo(0, a + Offset(-1.5f * dp, 0f))
            updatePointerTo(1, b + Offset(1.5f * dp, 0f))
            move()
            updatePointerTo(0, a + Offset(0f, 1.5f * dp))
            updatePointerTo(1, b + Offset(0f, -1.5f * dp))
            move()
            up(1)
            up(0)
        }
        composeRule.waitForIdle()
        assertEquals(tinyPiece, store.piece())
    }

    @Test
    fun a_two_finger_drag_does_not_keep_a_stray_little_turn() {
        val store = storeWith(midPage)
        show(store)
        val c = pieceCentre(store, midPage)
        val dp = composeRule.density.density
        val a = c + Offset(-100f * dp, 0f)
        val b = c + Offset(100f * dp, 0f)
        composeRule.onRoot().performTouchInput {
            down(0, a)
            down(1, b)
            // Under the slop, their line tips by about 2 degrees; then both fingers slide.
            updatePointerTo(0, a + Offset(2f * dp, -3.5f * dp))
            updatePointerTo(1, b + Offset(2f * dp, 3.5f * dp))
            move()
            for (i in 1..4) {
                updatePointerTo(0, a + Offset(20f * dp * i, -3.5f * dp))
                updatePointerTo(1, b + Offset(20f * dp * i, 3.5f * dp))
                move()
            }
            up(1)
            up(0)
        }
        composeRule.waitForIdle()
        assertEquals(0.0, store.piece().rotationDegrees, 0.0)
        assertTrue("moved", store.piece().xPt > midPage.xPt)
    }

    @Test
    fun two_fingers_resting_on_a_small_piece_change_nothing_and_keep_the_selection() {
        val store = storeWith(tinyPiece)
        show(store)
        val c = pieceCentre(store, tinyPiece)
        val d = 12f * composeRule.density.density
        composeRule.onRoot().performTouchInput {
            down(0, c + Offset(-d, -d))
            down(1, c + Offset(d, d))
            up(1)
            up(0)
        }
        composeRule.waitForIdle()
        assertEquals(tinyPiece, store.piece())
        assertEquals(1, store.uiState.value.selection.size)
    }

    @Test
    fun one_finger_on_a_handle_still_resizes() {
        val store = storeWith(midPage)
        show(store)
        composeRule.onNodeWithTag("$ResizeHandleTagPrefix${ResizeHandle.BOTTOM_RIGHT.name}").performTouchInput {
            down(center)
            moveBy(Offset(30f, 30f))
            up()
        }
        composeRule.waitForIdle()
        assertTrue(store.piece().widthPt > midPage.widthPt)
        assertEquals(0.0, store.piece().rotationDegrees, 0.0)
    }

    /** Narrower than a finger on this canvas: a piece the handles' boxes used to cover. */
    private val narrowPiece = Transform(45.0, 60.0, 5.0, 16.0)

    private fun markOf(store: EditorStore, box: Transform, sx: Float, sy: Float): Offset {
        val scale = store.uiState.value.view.screenPxPerPt
        val out = 5.5f * composeRule.density.density
        return pieceCentre(store, box) + Offset(sx * (box.widthPt.toFloat() / 2 * scale + out), sy * (box.heightPt.toFloat() / 2 * scale + out))
    }

    @Test
    fun a_drag_from_the_middle_of_a_small_piece_moves_it_and_does_not_resize_it() {
        val store = storeWith(narrowPiece)
        show(store)
        val c = pieceCentre(store, narrowPiece)
        composeRule.onRoot().performTouchInput {
            down(c)
            moveBy(Offset(30f, 0f))
            moveBy(Offset(30f, 30f))
            up()
        }
        composeRule.waitForIdle()
        assertTrue("moved", store.piece().xPt > narrowPiece.xPt + 1.0)
        assertEquals("not resized", narrowPiece.widthPt, store.piece().widthPt, 0.001)
        assertEquals("not resized", narrowPiece.heightPt, store.piece().heightPt, 0.001)
    }

    @Test
    fun a_double_tap_on_the_middle_of_a_small_piece_opens_it() {
        val store = storeWith(narrowPiece)
        show(store)
        val c = pieceCentre(store, narrowPiece)
        composeRule.onRoot().performTouchInput { doubleClick(c) }
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        assertTrue("was ${store.uiState.value.interaction}", store.uiState.value.interaction is Interaction.EditingText)
    }

    @Test
    fun a_drag_from_each_mark_of_a_small_piece_still_resizes_it() {
        val cases = listOf(1f to 0f, -1f to 0f, 0f to 1f, 0f to -1f, 1f to 1f, -1f to -1f, 1f to -1f, -1f to 1f)
        val store = storeWith(narrowPiece)
        show(store)
        for ((sx, sy) in cases) {
            val before = store.piece()
            val mark = markOf(store, before, sx, sy)
            composeRule.onRoot().performTouchInput {
                down(mark)
                moveBy(Offset(sx * 25f, sy * 25f))
                moveBy(Offset(sx * 25f, sy * 25f))
                up()
            }
            composeRule.waitForIdle()
            val after = store.piece()
            assertEquals("mark $sx,$sy changes the width", sx != 0f, after.widthPt > before.widthPt)
            assertEquals("mark $sx,$sy changes the height", sy != 0f, after.heightPt > before.heightPt)
            // The drag starts from the geometric point, not from the box that stepped outward: no more than the finger moved.
            val moved = 50.0 / store.uiState.value.view.screenPxPerPt + 0.01
            assertTrue("mark $sx,$sy added no more than the finger moved", after.widthPt - before.widthPt <= moved && after.heightPt - before.heightPt <= moved)
            store.dispatch(Intent.Undo)
        }
    }

    /** Holds the tap tests to what they claim: the point is inside the left handle's box, not on the page beside it. */
    private fun assertInLeftBox(at: Offset) {
        val box = composeRule.onNodeWithTag("$ResizeHandleTagPrefix${ResizeHandle.LEFT.name}").fetchSemanticsNode().boundsInRoot
        assertTrue("$at in $box", box.contains(at))
    }

    /** A neighbour left of [narrowPiece], its middle inside the narrow piece's left box but well away from its mark. */
    private val neighbour = Transform(33.0, 60.0, 5.0, 16.0)

    @Test
    fun a_tap_on_a_neighbour_inside_a_small_pieces_handle_box_picks_the_neighbour() {
        val store = storeWith(neighbour)
        store.dispatch(Intent.PlaceText(narrowPiece, "B")) // auto-selects the narrow piece
        val neighbourId = store.uiState.value.document.pages[0].elements.first().id
        show(store)
        val at = pieceCentre(store, neighbour)
        assertInLeftBox(at)
        composeRule.onRoot().performTouchInput { click(at) }
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        assertEquals(setOf(neighbourId), store.uiState.value.selection)
    }

    @Test
    fun a_tap_on_blank_paper_inside_a_small_pieces_handle_box_deselects() {
        val store = storeWith(narrowPiece)
        show(store)
        val scale = store.uiState.value.view.screenPxPerPt
        // 30dp left of the left edge: past the 24 the box used to reach, inside the 48 it reaches now.
        val at = pieceCentre(store, narrowPiece) - Offset(narrowPiece.widthPt.toFloat() / 2 * scale + 30f * composeRule.density.density, 0f)
        assertInLeftBox(at)
        composeRule.onRoot().performTouchInput { click(at) }
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        assertEquals(emptySet<String>(), store.uiState.value.selection)
    }

    @Test
    fun a_tap_on_a_handles_mark_keeps_the_selection_and_the_size() {
        val store = storeWith(narrowPiece)
        show(store)
        for ((sx, sy) in listOf(-1f to 0f, 1f to 1f)) {
            composeRule.onRoot().performTouchInput { click(markOf(store, narrowPiece, sx, sy)) }
            composeRule.mainClock.advanceTimeBy(1000)
            composeRule.waitForIdle()
            assertEquals("mark $sx,$sy", 1, store.uiState.value.selection.size)
            assertEquals("mark $sx,$sy", narrowPiece, store.piece())
        }
    }

    @Test
    fun a_turned_piece_is_typed_into_turned() {
        val box = Transform(30.0, 60.0, 40.0, 12.0, 90.0)
        val store = storeWith(box)
        show(store)
        store.dispatch(Intent.BeginEditText(store.uiState.value.selection.single()))
        composeRule.waitForIdle()
        val field = composeRule.onNodeWithTag(EditTextSessionTestTag).fetchSemanticsNode().boundsInRoot
        assertTrue("a 40 x 12 box turned 90 stands upright: ${field.width} x ${field.height}", field.height > field.width * 2)
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
    fun the_first_run_hint_waits_over_a_piece_at_the_top_and_the_knob_stays() {
        // A piece at the very top of the page. Until Bench A54 the hint lay over it and took the knob below it;
        // now the hint waits for room above a piece, so it is not drawn and leaves no stale bounds to hide the knob.
        val store = storeWith(Transform(40.0, 0.0, 20.0, 4.0))
        show(store, hintSeen = false)
        composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).assertIsNotDisplayed()
        composeRule.onNodeWithTag(TurnHandleTestTag).assertExists()
    }

    @Test
    fun a_hint_that_stops_fitting_gives_the_knob_back() {
        // Low enough for the hint, then moved to the top without a gesture (as Arrange's buttons move it): the
        // hint now waits, and the bounds it had when drawn must not keep hiding the knob.
        val store = storeWith(Transform(40.0, 100.0, 20.0, 4.0))
        show(store, hintSeen = false)
        composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).assertIsDisplayed()
        store.dispatch(Intent.Nudge(PtPoint(0.0, -100.0)))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).assertIsNotDisplayed()
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
