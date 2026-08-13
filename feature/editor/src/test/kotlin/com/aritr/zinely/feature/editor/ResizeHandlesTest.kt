package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.ResizeHandle
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
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
import org.robolectric.annotation.GraphicsMode

/**
 * Robolectric NATIVE proof of the resize-handle wiring ([ResizeHandles]): dragging the bottom-right corner
 * opens exactly one transform session, holds the opposite (top-left) corner fixed in page space, grows the
 * box, and commits one baked `CommitTransform`. The opposite-anchor bake math itself is proven pure in
 * `ResizeHandleTest` / `TransformMathTest`; this asserts the gesture decodes into the right intents.
 *
 * Surface 200×200 dp at `screenPxPerPt = 2` (1 dp == 1 px) ⇒ a 100×100 pt page. The element (40,40,20,20)
 * has its BR handle at page (60,60) ⇒ device (120,120).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ResizeHandlesTest {

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
            view = com.aritr.zinely.core.editor.ViewState(screenPxPerPt = pxPerPt),
        )
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        return EditorStore(model, scope, Dispatchers.Unconfined, runner)
    }

    private class Harness(val store: EditorStore) {
        val intents = mutableListOf<Intent>()
        var lastResizeWasNull = false
        fun dispatch(i: Intent) { intents += i; store.dispatch(i) }
    }

    @Test
    fun dragBottomRight_opensOneSession_growsBox_holdsTopLeft_commitsOnce() {
        val store = newStore()
        store.dispatch(Intent.PlaceText(Transform(40.0, 40.0, 20.0, 20.0), "t"))
        val id = store.uiState.value.selection.single()
        val h = Harness(store)

        composeRule.setContent {
            ZinelyTheme {
                ResizeHandles(
                    uiState = store.uiState.value,
                    currentState = { store.uiState.value },
                    dispatch = h::dispatch,
                    onResize = { h.lastResizeWasNull = it == null },
                    modifier = Modifier.size(200.dp, 200.dp),
                )
            }
        }

        composeRule.onNodeWithTag("$ResizeHandleTagPrefix${ResizeHandle.BOTTOM_RIGHT.name}").performTouchInput {
            down(center)
            moveBy(Offset(15f, 15f))
            moveBy(Offset(15f, 15f))
            moveBy(Offset(15f, 15f))
            up()
        }
        composeRule.waitForIdle()

        assertEquals(1, h.intents.count { it is Intent.BeginTransform })
        assertEquals(1, h.intents.count { it is Intent.CommitTransform })
        val resized = store.uiState.value.document.pages[0].elements.single { it.id == id }.transform
        // Opposite (top-left) corner held fixed in page space; box grew on both axes.
        assertEquals(40.0, resized.xPt, 1e-6)
        assertEquals(40.0, resized.yPt, 1e-6)
        assertTrue("width grew", resized.widthPt > 30.0)
        assertTrue("height grew", resized.heightPt > 30.0)
        assertEquals(0.0, resized.rotationDegrees, 1e-6)
        assertTrue(store.uiState.value.interaction is Interaction.Idle)
        assertTrue(h.lastResizeWasNull)
    }

    /**
     * **The mark moves; and it moves the way the frozen CSS says, at every rotation.**
     *
     * The companion to the test below, and the one a review had to ask for: that test proves the *hit
     * target* stayed put, which `HandleRingOffsetDp = 0.dp` also satisfies. Nothing asserted the drawn mark
     * had moved at all — the only witness was a golden re-recorded in the same change.
     *
     * Values from `v21-bench.html:201-206`, not from the constant: `.hnd.tl{left:-10px}` on a 9px
     * `border-box` mark ⇒ `-5.5` per axis; `.hnd.t{left:calc(50% - 4.5px);top:-10px}` ⇒ 5.5 out on the
     * bound axis, **0** on the free one.
     */
    @Test
    fun `the mark is displaced outward per axis, zero on an edge handle's free axis, and turns with the box`() {
        val px = 5.5f

        val tl = handleMarkOffsetPx(ResizeHandle.TOP_LEFT, px, 0.0)
        assertEquals(-5.5f, tl.x, 1e-4f)
        assertEquals(-5.5f, tl.y, 1e-4f)
        val br = handleMarkOffsetPx(ResizeHandle.BOTTOM_RIGHT, px, 0.0)
        assertEquals(5.5f, br.x, 1e-4f)
        assertEquals(5.5f, br.y, 1e-4f)

        // The edge handles are the case the naive "offset along the diagonal" reading gets wrong.
        val top = handleMarkOffsetPx(ResizeHandle.TOP, px, 0.0)
        assertEquals("an edge handle stays centred on its edge", 0f, top.x, 1e-4f)
        assertEquals(-5.5f, top.y, 1e-4f)
        val right = handleMarkOffsetPx(ResizeHandle.RIGHT, px, 0.0)
        assertEquals(5.5f, right.x, 1e-4f)
        assertEquals("an edge handle stays centred on its edge", 0f, right.y, 1e-4f)

        // Rotation: at 90° the top handle's outward normal points along +x. A transposed matrix sends it
        // to -x and this fails; dropping the rotation entirely leaves it on -y and this fails.
        val turned = handleMarkOffsetPx(ResizeHandle.TOP, px, 90.0)
        assertEquals(5.5f, turned.x, 1e-3f)
        assertEquals(0f, turned.y, 1e-3f)

        // …and the displacement is rigid: turning changes direction, never length.
        val d = handleMarkOffsetPx(ResizeHandle.TOP_LEFT, px, 37.0)
        assertEquals(
            "rotation must preserve the mark's distance from its target",
            kotlin.math.hypot(5.5f, 5.5f).toDouble(), kotlin.math.hypot(d.x, d.y).toDouble(), 1e-3,
        )
    }

    /**
     * **The drag must produce the box the drag distance implies — exactly, not merely "bigger".**
     *
     * This is the test the codebase did not have, and its absence is what made the 2026-08-12 handle
     * work dangerous. `centerPx` does three jobs: it places the 48dp hit box, it keys the `pointerInput`,
     * and it **seeds the drag accumulator** that `TransformMath.resizeByHandle` reads as *the corner's
     * new position*. Moving the drawn mark onto the frozen ring is one plausible edit away from moving
     * that seed — and the assertion above is `widthPt > 30.0`, which a 5.5dp bias sails straight through.
     *
     * ### It asserts the hit box's position, not a drag's outcome — and that is deliberate
     *
     * Two gesture-shaped versions of this test were written and both were wrong instruments. An absolute
     * width after a 45px drag pins the platform's **touch slop** (`detectDragGestures` swallows it before
     * the first `onDrag`, so 45px of finger is ~34px of drag) and calls it geometry. An out-and-back round
     * trip cancels the slop but not the re-seed: the second gesture re-reads the *moved* corner, so the
     * arithmetic compounds rather than cancelling.
     *
     * The seed, the `pointerInput` key and the hit box are **the same value**. So the invariant is
     * asserted where it is exact and needs no gesture at all: the 48dp target's centre must sit on the
     * element's geometric corner. If a future edit displaces `centerPx` to chase the drawn mark onto the
     * ring, this fails immediately and by name — which is the failure the codebase could not previously
     * produce.
     *
     * The box is 20×20pt at (40,40) with `screenPxPerPt` = 2, so its bottom-right corner is at device
     * (120,120) and the 48dp target spans ±24dp about it.
     */
    @Test
    fun theHandleTargetStaysCentredOnTheGeometricCorner_notOnTheDrawnMark() {
        val store = newStore()
        store.dispatch(Intent.PlaceText(Transform(40.0, 40.0, 20.0, 20.0), "t"))
        val h = Harness(store)

        composeRule.setContent {
            ZinelyTheme {
                ResizeHandles(
                    uiState = store.uiState.value,
                    currentState = { store.uiState.value },
                    dispatch = h::dispatch,
                    onResize = { h.lastResizeWasNull = it == null },
                    modifier = Modifier.size(300.dp, 300.dp),
                )
            }
        }
        composeRule.waitForIdle()

        val root = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        for ((handle, corner) in mapOf(
            ResizeHandle.TOP_LEFT to (40.0 to 40.0),
            ResizeHandle.BOTTOM_RIGHT to (60.0 to 60.0),
            ResizeHandle.TOP_RIGHT to (60.0 to 40.0),
            ResizeHandle.BOTTOM_LEFT to (40.0 to 60.0),
        )) {
            val b = composeRule.onNodeWithTag("$ResizeHandleTagPrefix${handle.name}")
                .fetchSemanticsNode().boundsInRoot
            val cx = (b.left + b.right) / 2f - root.left
            val cy = (b.top + b.bottom) / 2f - root.top
            assertEquals(
                "$handle's TARGET must stay on the box corner — the drawn mark moves, the point does not",
                (corner.first * pxPerPt).toFloat(), cx, 1.5f,
            )
            assertEquals(
                "$handle's TARGET must stay on the box corner — the drawn mark moves, the point does not",
                (corner.second * pxPerPt).toFloat(), cy, 1.5f,
            )
        }
    }
}
