package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM unit proof of the [EditorStore] shell (spike §10.3) — no Compose, no Robolectric. The store is
 * a thin serialising shell over the frozen pure reducer, so the tests verify the *wiring* Codex reviewed:
 * synchronous fold into [uiState], effect forwarding, the main-thread FIFO mailbox under re-entrancy, and
 * the synchronous [Intent.BeginTransform] token contract the gesture layer relies on.
 */
class EditorStoreTest {

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    /** Records every effect it is handed; optionally re-dispatches a follow-up intent inline (re-entrancy). */
    private class RecordingRunner(
        private val onEffect: (Effect, (Intent) -> Unit) -> Unit = { _, _ -> },
    ) : EditorEffectRunner {
        val effects = mutableListOf<Effect>()
        override fun run(effect: Effect, dispatch: (Intent) -> Unit) {
            effects += effect
            onEffect(effect, dispatch)
        }
    }

    private fun model(): EditorModel = EditorModel(
        document = ZineDocument(
            format = ZineFormat.SINGLE_SHEET_8,
            paperSize = PaperSize.LETTER,
            pages = listOf(Page(index = 0, role = PageRole.INTERIOR)),
        ),
    )

    private fun store(runner: EditorEffectRunner) =
        EditorStore(model(), scope, dispatcher, runner)

    private fun box() = Transform(xPt = 10.0, yPt = 10.0, widthPt = 20.0, heightPt = 20.0)

    @Test
    fun select_updatesUiState_withoutAutosave() {
        val runner = RecordingRunner()
        val store = store(runner)

        store.dispatch(Intent.PlaceText(box(), "hi"))
        val placedId = store.uiState.value.selection.single()
        store.dispatch(Intent.Select(null))
        store.dispatch(Intent.Select(placedId))

        assertEquals(setOf(placedId), store.uiState.value.selection)
        // PlaceText emitted exactly one Autosave; the two Selects emitted none (no document mutation).
        assertEquals(1, runner.effects.count { it is Effect.Autosave })
    }

    @Test
    fun placeText_addsElement_andRequestsAutosave() {
        val runner = RecordingRunner()
        val store = store(runner)

        store.dispatch(Intent.PlaceText(box(), "hello"))

        val page = store.uiState.value.document.pages[0]
        assertEquals(1, page.elements.size)
        assertEquals("hello", (page.elements[0] as com.aritr.zinely.core.model.TextElement).text)
        assertTrue(runner.effects.any { it is Effect.Autosave })
        assertTrue(store.uiState.value.canUndo)
    }

    @Test
    fun requestAddImage_forwardsPickAndDecodeEffect() {
        val runner = RecordingRunner()
        val store = store(runner)

        store.dispatch(Intent.RequestAddImage)

        assertEquals(listOf(Effect.PickAndDecodeImage), runner.effects)
        assertTrue(store.uiState.value.document.pages[0].elements.isEmpty())
    }

    @Test
    fun mailbox_serialisesSynchronousReentrantDispatch() {
        // A runner that, the moment it sees the PlaceText autosave, re-enters the store's PUBLIC
        // synchronous dispatch with ClearSelection (the worst case Codex flagged: an effect calling
        // dispatch mid-reduction). The mailbox must enqueue it and drain it AFTER the current reduction
        // completes — never a nested/recursive reduction. Final state: element placed, selection cleared,
        // and (crucially) no corruption from re-entry.
        lateinit var store: EditorStore
        var reentered = false
        val runner = RecordingRunner { effect, _ ->
            if (effect is Effect.Autosave && !reentered) {
                reentered = true
                store.dispatch(Intent.ClearSelection) // synchronous re-entry into the running drain
            }
        }
        store = store(runner)

        store.dispatch(Intent.PlaceText(box(), "x"))

        assertEquals(1, store.uiState.value.document.pages[0].elements.size)
        assertTrue("re-entrant ClearSelection must have drained", store.uiState.value.selection.isEmpty())
    }

    @Test
    fun beginTransform_tokenReadableSynchronously_thenCommitApplies() {
        val runner = RecordingRunner()
        val store = store(runner)
        store.dispatch(Intent.PlaceText(box(), "t"))
        val id = store.uiState.value.selection.single()

        // BeginTransform reduces synchronously, so the gesture layer can read the session token the
        // instant dispatch returns (spike §5).
        store.dispatch(Intent.BeginTransform(setOf(id)))
        val interaction = store.uiState.value.interaction
        assertTrue(interaction is Interaction.Transforming)
        val token = (interaction as Interaction.Transforming).token

        val moved = box().copy(xPt = 100.0, yPt = 100.0)
        store.dispatch(Intent.CommitTransform(mapOf(id to moved), token))

        val committed = store.uiState.value.document.pages[0].elements.single().transform
        assertEquals(100.0, committed.xPt, 0.0)
        assertTrue(store.uiState.value.interaction is Interaction.Idle)
    }

    @Test
    fun staleCommitTransform_wrongToken_isNoOp() {
        val runner = RecordingRunner()
        val store = store(runner)
        store.dispatch(Intent.PlaceText(box(), "t"))
        val id = store.uiState.value.selection.single()
        store.dispatch(Intent.BeginTransform(setOf(id)))

        val moved = box().copy(xPt = 999.0)
        store.dispatch(Intent.CommitTransform(mapOf(id to moved), token = -1L))

        val committed = store.uiState.value.document.pages[0].elements.single().transform
        assertEquals(10.0, committed.xPt, 0.0) // unchanged — stale token rejected by the reducer
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun postDispatch_followUpIntent_appliesAfterMainConfinement() {
        // A runner that uses the supplied (main-confined) dispatch to post a follow-up from outside the
        // current drain — it should run when the scheduler advances, not inline.
        val runner = RecordingRunner { effect, dispatch ->
            if (effect is Effect.PickAndDecodeImage) {
                scope.launch { dispatch(Intent.PlaceText(Transform(0.0, 0.0, 5.0, 5.0), "late")) }
            }
        }
        val store = store(runner)

        store.dispatch(Intent.RequestAddImage)
        assertTrue("follow-up must not run inline", store.uiState.value.document.pages[0].elements.isEmpty())

        scope.runCurrent()
        assertEquals(1, store.uiState.value.document.pages[0].elements.size)
    }

    @Test
    fun unknownSelection_clearsToNull() {
        val runner = RecordingRunner()
        val store = store(runner)
        store.dispatch(Intent.Select(null))
        assertNull(store.uiState.value.selection.firstOrNull())
    }
}
