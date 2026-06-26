package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.Interaction
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Instrumented-style proof of the gesture **wiring** ([editorTransformGestures], ADR-029 §5), run
 * headless via Robolectric NATIVE + compose-ui-test input injection — the same tier
 * [PagePreviewParityTest] uses. The pure frame/bake math lives in `LiveTransformTest`
 * (`:core:editor`); this asserts only that real pointer input over the page produces the right
 * intents against a real [EditorStore]: a drag opens one transform session and commits one baked
 * `CommitTransform`, a long-press emits `SelectAt`, and a still finger never opens a session.
 *
 * Surface is 200×200 dp at `screenPxPerPt = 2` ⇒ a 100×100 pt page (1 dp == 1 px under the default
 * test density), so a 40-px dry swipe is a +20 pt page-space move.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorGesturesTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val pxPerPt = 2f
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    private fun newStore(): EditorStore {
        val model = EditorModel(
            document = ZineDocument(
                format = ZineFormat.SINGLE_SHEET_8,
                paperSize = PaperSize.LETTER,
                pages = listOf(Page(index = 0, role = PageRole.INTERIOR)),
            ),
        )
        // Unconfined runner is irrelevant here (no Autosave timing under test); the store drains synchronously.
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        return EditorStore(model, scope, Dispatchers.Unconfined, runner)
    }

    private class Harness(val store: EditorStore) {
        val intents = mutableListOf<Intent>()
        var lastPreviewWasNull = false
        fun dispatch(i: Intent) { intents += i; store.dispatch(i) }
    }

    private fun setContent(store: EditorStore): Harness {
        val h = Harness(store)
        composeRule.setContent {
            Box(
                Modifier
                    .size(200.dp, 200.dp)
                    .testTag("surface")
                    .editorTransformGestures(
                        screenPxPerPt = pxPerPt,
                        pageOffset = PtPoint(0.0, 0.0),
                        pageSizePt = PtSize(100.0, 100.0),
                        currentState = { store.uiState.value },
                        dispatch = h::dispatch,
                        onPreview = { h.lastPreviewWasNull = it == null },
                    ),
            )
        }
        return h
    }

    @Test
    fun drag_opensOneTransformSession_andCommitsOneBakedMove() {
        val store = newStore()
        // Place + auto-select one element at (40,40) 20×20 pt.
        store.dispatch(Intent.PlaceText(Transform(40.0, 40.0, 20.0, 20.0), "t"))
        val id = store.uiState.value.selection.single()
        val h = setContent(store)

        composeRule.onNodeWithTag("surface").performTouchInput {
            swipeRight(startX = centerX, endX = centerX + 80f, durationMillis = 200)
        }
        composeRule.waitForIdle()

        // Exactly one BeginTransform and one CommitTransform reached the store (one gesture = one undo step).
        assertEquals(1, h.intents.count { it is Intent.BeginTransform })
        assertEquals(1, h.intents.count { it is Intent.CommitTransform })
        // Rightward drag moves the box right (exact px→pt is LiveTransformTest's job; touch-slop eats a few
        // px of startup so we assert direction + that size/rotation/centre-Y are untouched by a pure pan).
        val moved = store.uiState.value.document.pages[0].elements.single { it.id == id }.transform
        assertTrue("box moved right", moved.xPt > 50.0)
        assertEquals(40.0, moved.yPt, 6.0)
        assertEquals(20.0, moved.widthPt, 1e-6)
        assertEquals(0.0, moved.rotationDegrees, 1e-6)
        // Session closed and preview reset.
        assertTrue(store.uiState.value.interaction is Interaction.Idle)
        assertTrue(h.lastPreviewWasNull)
    }

    @Test
    fun drag_towardPageEdge_commitsTheSnappedTransform() {
        val store = newStore()
        // 20×20 box at (40,40). A single moveBy applies its FULL pan (the slop-crossing frame is also
        // accumulated), so +76px at 2px/pt is a deterministic +38pt move: right edge lands at 98 — within
        // the 8px/2 = 4pt snap threshold of the page's right edge (100). The commit snaps right→100, x→80
        // (vs the un-snapped 78), proving the snap is baked into the commit (preview == commit, §5.4).
        store.dispatch(Intent.PlaceText(Transform(40.0, 40.0, 20.0, 20.0), "t"))
        val id = store.uiState.value.selection.single()
        val h = setContent(store)

        composeRule.onNodeWithTag("surface").performTouchInput {
            down(center)
            moveBy(Offset(76f, 0f))
            up()
        }
        composeRule.waitForIdle()

        val moved = store.uiState.value.document.pages[0].elements.single { it.id == id }.transform
        assertEquals(100.0, moved.xPt + moved.widthPt, 1e-6)
        assertEquals(80.0, moved.xPt, 1e-6)
        assertTrue(store.uiState.value.interaction is Interaction.Idle)
    }

    @Test
    fun longPress_dispatchesSelectAt_inPagePoints() {
        val store = newStore()
        val h = setContent(store)

        composeRule.onNodeWithTag("surface").performTouchInput { longClick(Offset(100f, 100f)) }
        composeRule.waitForIdle()

        val selectAt = h.intents.filterIsInstance<Intent.SelectAt>().single()
        // (100,100) px at 2 px/pt, zero offset → (50,50) pt.
        assertEquals(50.0, selectAt.pagePoint.x, 1e-6)
        assertEquals(50.0, selectAt.pagePoint.y, 1e-6)
    }

    @Test
    fun stillFinger_neverOpensATransformSession() {
        val store = newStore()
        store.dispatch(Intent.PlaceText(Transform(40.0, 40.0, 20.0, 20.0), "t"))
        val h = setContent(store)

        // A long-press is a held, motionless pointer — it must stay a tap, never a transform.
        composeRule.onNodeWithTag("surface").performTouchInput { longClick(Offset(100f, 100f)) }
        composeRule.waitForIdle()

        assertNull(h.intents.firstOrNull { it is Intent.BeginTransform })
        assertTrue(store.uiState.value.interaction is Interaction.Idle)
    }
}
