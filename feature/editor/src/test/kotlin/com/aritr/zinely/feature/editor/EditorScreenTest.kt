package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The S4 host [EditorScreen] (ADR-029 §5/§6): proves the *assembly* — the measured canvas feeds the model
 * viewport, the visible contextbar tracks selection, the accessible mirror is wired, and an open text
 * session raises the edit overlay. Robolectric NATIVE, the same tier as the per-layer tests; the layers'
 * own behaviour is covered by their dedicated suites.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val pageSizePt = PtSize(100.0, 100.0)

    private fun store(): EditorStore {
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        return EditorStore(
            EditorModel(
                document = ZineDocument(
                    format = ZineFormat.SINGLE_SHEET_8,
                    paperSize = PaperSize.LETTER,
                    pages = listOf(Page(index = 0, role = PageRole.INTERIOR)),
                ),
            ),
            scope, Dispatchers.Unconfined, runner,
        )
    }

    private fun twoPageStore(): EditorStore {
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        return EditorStore(
            EditorModel(
                document = ZineDocument(
                    format = ZineFormat.SINGLE_SHEET_8,
                    paperSize = PaperSize.LETTER,
                    pages = listOf(
                        Page(index = 0, role = PageRole.INTERIOR),
                        Page(index = 1, role = PageRole.INTERIOR),
                    ),
                ),
            ),
            scope, Dispatchers.Unconfined, runner,
        )
    }

    private fun setScreen(
        store: EditorStore,
        moveResizeHintSeen: Boolean? = false,
        onMoveResizeHintSeen: () -> Unit = {},
        savedSignals: Flow<Unit> = emptyFlow(),
    ) {
        composeRule.setContent {
            MaterialTheme {
                EditorScreen(
                    store = store,
                    pageSizePt = pageSizePt,
                    modifier = Modifier.size(300.dp, 400.dp),
                    moveResizeHintSeen = moveResizeHintSeen,
                    onMoveResizeHintSeen = onMoveResizeHintSeen,
                    savedSignals = savedSignals,
                )
            }
        }
    }

    @Test
    fun measuring_the_canvas_pushes_a_real_viewport_into_the_model() {
        val store = store()
        setScreen(store)
        composeRule.waitForIdle()
        // The host fit a 100×100pt page into a 300×400px canvas, so the scale is well above the 1f default —
        // proving SetViewport flowed from the measured canvas into the shared ViewState.
        assertTrue(store.uiState.value.view.screenPxPerPt > 1f)
    }

    @Test
    fun the_contextbar_tracks_selection() {
        val store = store()
        store.dispatch(Intent.PlaceText(Transform(20.0, 20.0, 20.0, 20.0), "hi")) // auto-selects
        setScreen(store)
        composeRule.onNodeWithTag(EditorContextBarTestTag).assertIsDisplayed()

        store.dispatch(Intent.ClearSelection)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorContextBarTestTag).assertDoesNotExist()
    }

    @Test
    fun the_accessible_element_mirror_is_wired() {
        val store = store()
        store.dispatch(Intent.PlaceText(Transform(20.0, 20.0, 20.0, 20.0), "hi"))
        val id = store.uiState.value.selection.single()
        setScreen(store)
        composeRule.onNodeWithTag("$ElementNodeTagPrefix$id").assertExists()
    }

    @Test
    fun a_text_session_gates_the_canvas_so_resize_handles_disappear() {
        // Regression for the lost-draft race (Codex RF1): while editing, the gesture surface + handles must
        // be inert, or a stray long-press/double-tap replaces EditingText and the draft is dropped. A
        // single-selected element shows handles; opening its text session must remove them.
        val store = store()
        store.dispatch(Intent.PlaceText(Transform(20.0, 20.0, 20.0, 20.0), "hi"))
        val id = store.uiState.value.selection.single()
        setScreen(store)
        composeRule.onNodeWithTag("${ResizeHandleTagPrefix}TOP_LEFT").assertExists()

        store.dispatch(Intent.BeginEditText(id))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("${ResizeHandleTagPrefix}TOP_LEFT").assertDoesNotExist()
    }

    @Test
    fun the_supply_tray_add_words_drives_the_live_host_add_text_path() {
        // Wiring proof at the host seam: the EditorSupplyTray is assembled inside EditorScreen (not tested in
        // isolation here), so tapping its "Add words" supply must run the host's addTextAndEdit — place an
        // empty text element on the current page, select it, and open its edit session.
        val store = store()
        setScreen(store)
        composeRule.waitForIdle()
        assertTrue(store.uiState.value.document.pages[0].elements.isEmpty())

        composeRule.onNodeWithTag(SupplyAddWordsTag).performClick()
        composeRule.waitForIdle()

        val page = store.uiState.value.document.pages[0]
        assertTrue(page.elements.size == 1)
        assertTrue(store.uiState.value.selection.size == 1)
        assertTrue(store.uiState.value.interaction is Interaction.EditingText)
        composeRule.onNodeWithTag(EditTextSessionTestTag).assertIsDisplayed()
    }

    @Test
    fun on_a_blank_page_the_add_actions_are_not_duplicated_the_tray_owns_them() {
        // ADR-033 de-dup: a blank page raises the invitation overlay AND the persistent tray. The overlay
        // is invitation-only (no buttons), so each add action exists exactly once on screen — in the tray
        // (the thumb-zone home, DESIGN-RULES 3/7). Guards against re-adding buttons to the empty state.
        val store = store()
        setScreen(store)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorEmptyStateTestTag).assertIsDisplayed()

        // Exactly one "Add a photo" / "Add words" affordance — the tray's, not a second in the overlay.
        assertTrue(
            composeRule.onAllNodesWithText(AddPhotoActionLabel, substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes().size == 1,
        )
        assertTrue(
            composeRule.onAllNodesWithText(AddWordsActionLabel, substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes().size == 1,
        )
        composeRule.onNodeWithTag(SupplyAddPhotoTag).assertIsDisplayed()
        composeRule.onNodeWithTag(SupplyAddWordsTag).assertIsDisplayed()
    }

    @Test
    fun the_empty_state_copy_follows_the_current_page_position() {
        // VOICE empty states: the host threads the current page position so page 0 gets the welcoming
        // line and any later blank page gets the lighter "fresh page" variant. The overlay stays
        // invitation-only either way (tray owns the actions); only the headline changes.
        val store = twoPageStore()
        setScreen(store)
        composeRule.waitForIdle()
        composeRule.onNodeWithText(FirstPageInvitationHeadline, substring = true).assertIsDisplayed()

        store.dispatch(Intent.GoToPage(1))
        composeRule.waitForIdle()
        composeRule.onNodeWithText(LaterPageInvitationHeadline, substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(FirstPageInvitationHeadline, substring = true).assertDoesNotExist()
    }

    @Test
    fun a_front_cover_page_gets_the_welcoming_line() {
        // The "first page" signal is the page's identity, not just the cursor: a role-typed FRONT_COVER
        // gets the warm welcome, so future role-aware documents don't regress to the "fresh page" line.
        val store = EditorStore(
            EditorModel(
                document = ZineDocument(
                    format = ZineFormat.SINGLE_SHEET_8,
                    paperSize = PaperSize.LETTER,
                    pages = listOf(
                        Page(index = 0, role = PageRole.FRONT_COVER),
                        Page(index = 1, role = PageRole.INTERIOR),
                    ),
                ),
            ),
            scope, Dispatchers.Unconfined,
            object : EditorEffectRunner {
                override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
            },
        )
        setScreen(store)
        composeRule.waitForIdle()
        composeRule.onNodeWithText(FirstPageInvitationHeadline, substring = true).assertIsDisplayed()
    }

    @Test
    fun a_move_resize_hint_appears_once_an_element_is_selected() {
        // Discoverability teach: once a placed element is single-selected (handles visible, Idle), the
        // one-time hint floats in to say the moves are drag/pinch — the gestures that have no other twin.
        val store = store()
        store.dispatch(Intent.PlaceText(Transform(20.0, 20.0, 20.0, 20.0), "hi")) // auto-selects, Idle
        setScreen(store)
        composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).assertIsDisplayed()
    }

    @Test
    fun the_move_resize_hint_stays_hidden_with_no_selection() {
        // Relevance: nothing selected → no handles → no gesture to teach → no hint clutter.
        val store = store()
        setScreen(store)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).assertDoesNotExist()
    }

    @Test
    fun tapping_got_it_dismisses_the_move_resize_hint_for_the_session() {
        // Easy, touch-safe dismissal; screen-local one-time — re-selecting must not bring it back.
        val store = store()
        store.dispatch(Intent.PlaceText(Transform(20.0, 20.0, 20.0, 20.0), "hi"))
        val id = store.uiState.value.selection.single()
        setScreen(store)
        composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).assertIsDisplayed()

        composeRule.onNodeWithTag(MoveResizeHintDismissTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).assertDoesNotExist()

        // Re-select the same element: still dismissed (one-time, not per-selection nagging).
        store.dispatch(Intent.ClearSelection)
        composeRule.waitForIdle()
        store.dispatch(Intent.Select(id))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).assertDoesNotExist()
    }

    @Test
    fun a_persisted_seen_flag_suppresses_the_hint_across_sessions() {
        // ADR-032: a relaunch where the store already recorded "seen" must NOT re-teach. With the gate
        // true, selecting an element shows no hint — the across-sessions promise, distinct from the
        // session-local "Got it" latch.
        val store = store()
        store.dispatch(Intent.PlaceText(Transform(20.0, 20.0, 20.0, 20.0), "hi")) // auto-selects
        setScreen(store, moveResizeHintSeen = true)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).assertDoesNotExist()
    }

    @Test
    fun the_hint_stays_hidden_while_the_persisted_flag_is_still_loading() {
        // ADR-032 flash-avoidance: a null gate (value not yet loaded) must NOT show the hint, so it can't
        // flash before the persisted state is known. It becomes eligible only once a real `false` arrives.
        val store = store()
        store.dispatch(Intent.PlaceText(Transform(20.0, 20.0, 20.0, 20.0), "hi")) // auto-selects
        setScreen(store, moveResizeHintSeen = null)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).assertDoesNotExist()
    }

    @Test
    fun dismissing_the_hint_reports_it_seen_for_persistence() {
        // The "Got it" tap must drive the persistence callback (the host's only write seam, ADR-032), not
        // just the session latch — otherwise the flag never survives the process.
        var reportedSeen = false
        val store = store()
        store.dispatch(Intent.PlaceText(Transform(20.0, 20.0, 20.0, 20.0), "hi"))
        setScreen(store, onMoveResizeHintSeen = { reportedSeen = true })
        composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).assertIsDisplayed()

        composeRule.onNodeWithTag(MoveResizeHintDismissTag).performClick()
        composeRule.waitForIdle()

        assertTrue(reportedSeen)
        composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).assertDoesNotExist()
    }

    @Test
    fun opening_a_text_session_replaces_the_hint_with_the_edit_overlay() {
        // The hint never blocks editing: opening a text session yields the hint and raises the overlay.
        val store = store()
        store.dispatch(Intent.PlaceText(Transform(20.0, 20.0, 20.0, 20.0), "hi"))
        val id = store.uiState.value.selection.single()
        setScreen(store)
        composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).assertIsDisplayed()

        store.dispatch(Intent.BeginEditText(id))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(EditTextSessionTestTag).assertIsDisplayed()
    }

    @Test
    fun the_saved_confirmation_is_hidden_until_a_save_signal() {
        // Quiet by default: with no autosave event, the editor shows no "Saved" chrome — it only appears
        // in response to a real save signal (VOICE: earned, not constant).
        val store = store()
        setScreen(store, savedSignals = MutableSharedFlow())
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorSavedConfirmationTestTag).assertDoesNotExist()
    }

    @Test
    fun a_save_signal_shows_the_saved_confirmation() {
        // The host subscribes to the autosave/persist signal stream and surfaces the transient "Saved ✨"
        // reassurance when a save event arrives — driven by the existing path, not a second save system.
        val signals = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val store = store()
        composeRule.mainClock.autoAdvance = false
        setScreen(store, savedSignals = signals)
        composeRule.waitForIdle() // the collector is now subscribed (replay=0 SharedFlow)
        composeRule.onNodeWithTag(EditorSavedConfirmationTestTag).assertDoesNotExist()

        signals.tryEmit(Unit)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorSavedConfirmationTestTag).assertIsDisplayed()
    }

    @Test
    fun the_saved_confirmation_auto_dismisses_after_the_transient_window() {
        // Non-blocking and transient: it fades itself out after the window — it never lingers or competes
        // with the tray. Clock is hand-advanced so the dismissal is deterministic, not wall-clock racy.
        val signals = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val store = store()
        composeRule.mainClock.autoAdvance = false
        setScreen(store, savedSignals = signals)
        composeRule.waitForIdle()

        signals.tryEmit(Unit)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorSavedConfirmationTestTag).assertIsDisplayed()

        // Past the visible window + the fade-out: the chip removes itself.
        composeRule.mainClock.advanceTimeBy(SavedConfirmationVisibleMs + 1000L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorSavedConfirmationTestTag).assertDoesNotExist()
    }

    @Test
    fun the_saved_confirmation_yields_while_the_move_resize_hint_is_visible() {
        // Competing-chrome guard (Codex review #2): placing the first element BOTH selects it (raising the
        // move/resize hint at TopCenter, up to 320dp wide) and autosaves. On a narrow canvas a TopEnd
        // "Saved" chip would overlap the centered hint, so the chip yields — the teaching hint wins, the
        // chip simply skips that window.
        val signals = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val store = store()
        store.dispatch(Intent.PlaceText(Transform(20.0, 20.0, 20.0, 20.0), "hi")) // auto-selects → hint
        composeRule.mainClock.autoAdvance = false
        setScreen(store, savedSignals = signals)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).assertIsDisplayed()

        signals.tryEmit(Unit)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        // Save fired, but the chip stays hidden because the hint owns the top of the canvas.
        composeRule.onNodeWithTag(EditorSavedConfirmationTestTag).assertDoesNotExist()
    }

    @Test
    fun an_open_text_session_raises_the_edit_overlay() {
        val store = store()
        store.dispatch(Intent.PlaceText(Transform(20.0, 20.0, 20.0, 20.0), "hi"))
        val id = store.uiState.value.selection.single()
        setScreen(store)
        composeRule.onNodeWithTag(EditTextSessionTestTag).assertDoesNotExist()

        store.dispatch(Intent.BeginEditText(id))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditTextSessionTestTag).assertIsDisplayed()
    }
}
