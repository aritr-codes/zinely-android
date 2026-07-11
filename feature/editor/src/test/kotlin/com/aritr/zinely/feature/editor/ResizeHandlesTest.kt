package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
}
