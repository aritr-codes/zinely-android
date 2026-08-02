package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
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
        saveError: SaveErrorKind? = null,
        onDismissSaveError: () -> Unit = {},
        pageSizePt: PtSize = this.pageSizePt,
    ) {
        composeRule.setContent {
            ZinelyTheme {
                EditorScreen(
                    store = store,
                    pageSizePt = pageSizePt,
                    modifier = Modifier.size(300.dp, 400.dp),
                    moveResizeHintSeen = moveResizeHintSeen,
                    onMoveResizeHintSeen = onMoveResizeHintSeen,
                    savedSignals = savedSignals,
                    saveError = saveError,
                    onDismissSaveError = onDismissSaveError,
                )
            }
        }
    }

    @Test
    fun paper_surface_backs_the_page_render() {
        // Given a mounted editor
        setScreen(store())
        composeRule.waitForIdle()
        // Then the page-footprint paper backing is present under the render (the canvas page must
        // read as paper like Preview/export/thumbnails, never the bare desk).
        composeRule.onNodeWithTag(EditorPaperSurfaceTestTag).assertExists()
    }

    /**
     * D2 — the paper backing and the render must never be drawn at two different scales.
     *
     * The viewport push is deliberately deferred until the interaction is `Idle` (a mid-gesture re-key
     * strands the session), and every content layer reads `view.screenPxPerPt`. The paper used to be sized
     * from the freshly measured local scale instead, so any canvas resize during a session — the soft
     * keyboard opening under an inline edit is the everyday one — moved the sheet and left its contents
     * behind until the session ended. Pinning the paper to the *shared* viewport is what makes them lag
     * together rather than diverge.
     */
    @Test
    fun the_paper_is_sized_from_the_viewport_the_content_reads() {
        val store = store()
        setScreen(store)
        composeRule.waitForIdle()

        val spp = store.uiState.value.view.screenPxPerPt
        val paper = composeRule.onNodeWithTag(EditorPaperSurfaceTestTag).fetchSemanticsNode().boundsInRoot
        assertEquals((pageSizePt.width * spp).toFloat(), paper.width, 1f)
        assertEquals((pageSizePt.height * spp).toFloat(), paper.height, 1f)
    }

    /**
     * The one that actually tests D2 — and the reason it is worth its length.
     *
     * At rest the two scales are equal **by construction**, so the assertion above passes whichever of
     * them the paper is sized from: a review mutation-tested it and it stayed green against the original
     * defect. The divergence only exists while the interaction is *not* Idle, because that is exactly when
     * the viewport push is withheld. So this test has to get into that state and resize the canvas — which
     * is the everyday case, the soft keyboard opening under an inline text edit.
     */
    @Test
    fun a_canvas_resize_during_a_session_moves_neither_the_paper_nor_the_content() {
        val store = store()
        store.dispatch(Intent.PlaceText(Transform(20.0, 20.0, 40.0, 20.0), "hi")) // auto-selects
        val id = store.uiState.value.selection.single()

        // A portrait page in a comfortably wide host, so the *height* is what the fit is limited by — then
        // changing the host height certainly changes the fitted scale, which is what this test needs to be
        // true for the mutation to be caught.
        val portraitPage = PtSize(50.0, 100.0)
        var canvasHeight by mutableStateOf(400.dp)
        composeRule.setContent {
            ZinelyTheme {
                EditorScreen(
                    store = store,
                    pageSizePt = portraitPage,
                    modifier = Modifier.size(300.dp, canvasHeight),
                    moveResizeHintSeen = true,
                    onMoveResizeHintSeen = {},
                    savedSignals = emptyFlow(),
                )
            }
        }
        composeRule.waitForIdle()

        // Open a text session: from here the host deliberately stops pushing the measured scale into the
        // model, because re-keying the gesture pointerInput mid-session would strand it.
        store.dispatch(Intent.BeginEditText(id))
        composeRule.waitForIdle()
        assertTrue("session open", store.uiState.value.interaction is Interaction.EditingText)
        val frozen = store.uiState.value.view.screenPxPerPt

        // The host resizes under the open session. Grown rather than shrunk, deliberately: shrinking far
        // enough to force a scale change collapses the weighted canvas toward zero, and every node's
        // clipped bounds go to zero with it — which fails the assertion for the wrong reason.
        canvasHeight = 700.dp
        composeRule.waitForIdle()

        assertEquals("the viewport is deliberately frozen mid-session", frozen, store.uiState.value.view.screenPxPerPt)
        val canvas = composeRule.onNodeWithTag(EditorCanvasTestTag).fetchSemanticsNode().boundsInRoot
        assertTrue(
            "the canvas must actually have grown, or the mutation cannot be caught: $canvas",
            canvas.height > portraitPage.height * frozen + 1f,
        )
        val paper = composeRule.onNodeWithTag(EditorPaperSurfaceTestTag).fetchSemanticsNode().boundsInRoot
        assertEquals(
            "the paper must freeze with the content it backs, not follow the new canvas",
            (portraitPage.width * frozen).toFloat(),
            paper.width,
            1f,
        )
    }

    /**
     * D1 — the blank-page invitation belongs on the sheet, not on the desk around it.
     *
     * It was placed with `Modifier.align(Alignment.Center)`, which centres on the *canvas*; the paper is
     * top-left anchored and, for a portrait page, much narrower. The copy therefore sat to the right of the
     * sheet with its lines running off the display — which reads as the app having lost track of the page,
     * and is why an undo that merely revealed a blank page was reported as undo corrupting the layout.
     */
    @Test
    fun the_blank_page_invitation_stays_within_the_paper() {
        // A portrait page in a 300×400dp host: the paper is materially narrower than the canvas, so
        // canvas-centred and paper-centred are far apart. With a square page they would coincide and this
        // test would prove nothing.
        setScreen(store(), pageSizePt = PtSize(50.0, 100.0))
        composeRule.waitForIdle()

        val paper = composeRule.onNodeWithTag(EditorPaperSurfaceTestTag).fetchSemanticsNode().boundsInRoot
        val invitation = composeRule.onNodeWithTag(EditorEmptyStateTestTag).fetchSemanticsNode().boundsInRoot

        assertTrue("paper is narrower than the canvas, or this proves nothing", paper.width < 300f)
        assertTrue(
            "the invitation ran off the paper: paper=$paper invitation=$invitation",
            invitation.left >= paper.left - 1f && invitation.right <= paper.right + 1f,
        )
        assertEquals("centred on the paper", paper.center.x, invitation.center.x, 1f)
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
    fun no_save_failure_means_no_failure_banner() {
        // Quiet by default: with no reported failure, the editor shows no "couldn't save" chrome.
        val store = store()
        setScreen(store, saveError = null)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorSaveFailureTestTag).assertDoesNotExist()
    }

    @Test
    fun a_save_failure_shows_the_warm_failure_banner() {
        // ADR-035: a reported autosave failure surfaces the honest, warm "couldn't save" banner.
        val store = store()
        setScreen(store, saveError = SaveErrorKind.Generic)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorSaveFailureTestTag).assertIsDisplayed()
        composeRule.onNodeWithText(SaveFailureText, substring = true).assertIsDisplayed()
    }

    @Test
    fun a_storage_failure_shows_the_out_of_space_copy() {
        // ADR-036: a probe-classified out-of-space failure shows the storage-specific line, not the generic
        // one — the honest, actionable "low on storage" guidance, keyed by the feature-local kind.
        val store = store()
        setScreen(store, saveError = SaveErrorKind.OutOfSpace)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorSaveFailureTestTag).assertIsDisplayed()
        composeRule.onNodeWithText(SaveFailureOutOfSpaceText, substring = true).assertIsDisplayed()
        // It must NOT show the generic line when the failure is specifically storage exhaustion.
        composeRule.onNodeWithText(SaveFailureText, substring = true).assertDoesNotExist()
    }

    @Test
    fun a_save_failure_suppresses_the_optimistic_saved_confirmation() {
        // Honesty precedence (ADR-035): "Saved ✨" fires at mark-dirty (optimistic, ADR-034). While a real
        // failure is known, the editor must NOT also flash "Saved" — the failure banner wins, the chip yields.
        val signals = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val store = store()
        composeRule.mainClock.autoAdvance = false
        setScreen(store, savedSignals = signals, saveError = SaveErrorKind.Generic)
        composeRule.waitForIdle()

        signals.tryEmit(Unit)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        // A save was "scheduled", but with a failure on record the chip stays hidden; the banner shows.
        composeRule.onNodeWithTag(EditorSavedConfirmationTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(EditorSaveFailureTestTag).assertIsDisplayed()
    }

    @Test
    fun a_save_failure_suppresses_the_move_resize_hint() {
        // Competing-chrome precedence (ADR-035): the one-time teaching hint yields to the more important
        // honest failure banner at the top of the canvas; selecting an element raises the hint, but a
        // known failure keeps it hidden and shows the banner instead.
        val store = store()
        store.dispatch(Intent.PlaceText(Transform(20.0, 20.0, 20.0, 20.0), "hi")) // auto-selects → hint
        setScreen(store, saveError = SaveErrorKind.Generic)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(EditorSaveFailureTestTag).assertIsDisplayed()
    }

    @Test
    fun dismissing_a_failure_does_not_resurrect_a_stale_saved_chip() {
        // Honesty regression (Codex Required Fix): "Saved ✨" runs an independent 1600ms window. If a save
        // signal lit the chip, then a failure appears (chip yields to the banner), then the user dismisses
        // the failure BEFORE that window elapses, the chip must NOT pop back — there is no success evidence.
        val signals = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val store = store()
        val errorVisible = androidx.compose.runtime.mutableStateOf(false)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ZinelyTheme {
                EditorScreen(
                    store = store,
                    pageSizePt = pageSizePt,
                    modifier = Modifier.size(300.dp, 400.dp),
                    savedSignals = signals,
                    saveError = if (errorVisible.value) SaveErrorKind.Generic else null,
                    onDismissSaveError = { errorVisible.value = false },
                )
            }
        }
        composeRule.waitForIdle()

        // A save signal lights the chip.
        signals.tryEmit(Unit)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorSavedConfirmationTestTag).assertIsDisplayed()

        // A failure appears mid-window: chip yields to the banner (past its fade-out).
        errorVisible.value = true
        composeRule.mainClock.advanceTimeBy(500L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorSavedConfirmationTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(EditorSaveFailureTestTag).assertIsDisplayed()

        // Dismiss the failure while the original 1600ms window would still be open: the chip must stay
        // gone — the failure cancelled its timer, only a new save signal could re-light it.
        errorVisible.value = false
        composeRule.mainClock.advanceTimeBy(500L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorSavedConfirmationTestTag).assertDoesNotExist()
    }

    @Test
    fun tapping_got_it_on_the_failure_banner_invokes_the_dismiss_callback() {
        // The host forwards dismissal to the app (which clears the sink, ADR-026 §5) — the host owns no
        // failure state of its own, so it just wires the callback through.
        var dismissed = false
        val store = store()
        setScreen(store, saveError = SaveErrorKind.Generic, onDismissSaveError = { dismissed = true })
        composeRule.onNodeWithTag(EditorSaveFailureTestTag).assertIsDisplayed()

        composeRule.onNodeWithTag(SaveFailureDismissTag).performClick()
        composeRule.waitForIdle()
        assertTrue(dismissed)
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

    // --- ADR-070: the live unsupported-character coverage notice, host gating ---

    /** U+0985 BENGALI LETTER A — out of the bundled set, built from its code point (encoding-safe). */
    private val bengaliA = String(Character.toChars(0x0985))

    @Test
    fun an_unsupported_character_in_the_open_session_raises_the_coverage_notice() {
        // The honesty seam (ADR-070): opening a session on text the renderer can't print must surface the
        // notice at the top of the canvas, so the character can't vanish to paper without a warning.
        val store = store()
        store.dispatch(Intent.PlaceText(Transform(20.0, 20.0, 20.0, 20.0), bengaliA))
        val id = store.uiState.value.selection.single()
        setScreen(store)
        composeRule.onNodeWithTag(EditorCoverageNoticeTestTag).assertDoesNotExist()

        store.dispatch(Intent.BeginEditText(id))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorCoverageNoticeTestTag).assertIsDisplayed()
    }

    @Test
    fun the_coverage_notice_stays_hidden_with_no_open_session() {
        // The notice is a status of the *current draft*: with no text session open there is no draft to
        // warn about, so it must be absent even though the canvas is live.
        val store = store()
        setScreen(store)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorCoverageNoticeTestTag).assertDoesNotExist()
    }

    @Test
    fun a_save_failure_suppresses_the_coverage_notice() {
        // Precedence (ADR-070): a real "couldn't save" is more urgent than a render-coverage warning, and
        // both live in the TopCenter slot — so while the failure banner is up the coverage notice yields.
        val store = store()
        store.dispatch(Intent.PlaceText(Transform(20.0, 20.0, 20.0, 20.0), bengaliA))
        val id = store.uiState.value.selection.single()
        store.dispatch(Intent.BeginEditText(id))
        setScreen(store, saveError = SaveErrorKind.Generic)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(EditorSaveFailureTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(EditorCoverageNoticeTestTag).assertDoesNotExist()
    }

    // ── C2b — the frozen `.ctx` verb bar, as assembled (ADR-092) ───────────────────────────────────

    private fun selectedText(): EditorStore = store().also {
        it.dispatch(Intent.PlaceText(Transform(20.0, 20.0, 20.0, 20.0), "Hi"))
    }

    @Test
    fun the_verb_bar_follows_the_selection_and_yields_to_an_open_session() {
        val store = selectedText()
        setScreen(store)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.EDIT}").assertIsDisplayed()

        // ADR-092 §3: the freeze itself hides `.ctx` while a session owns the element (v2-bench.html:516).
        store.dispatch(Intent.BeginEditText(store.uiState.value.selection.single()))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.EDIT}").assertDoesNotExist()
    }

    /**
     * The one that would have caught it.
     *
     * The verb bar floats at the canvas's bottom edge, so it is composed **inside** [BenchSheetIsland] —
     * and the island re-declares eight tokens from the light palette, `ink` among them, so that the
     * artifact does not dim at night (D-035/OD-12). The bar took that light `ink` while keeping the room's
     * dark `--sheet` for its own fill: dark ink on a dark card, measured at **1.05:1** on a device. Every
     * unit test passed, because Robolectric's default qualifiers are the *light* palette, where the two
     * sources happen to agree. Only `qualifiers = "night"` can tell them apart.
     *
     * Asserted as contrast rather than as a colour, because the defect is not "the wrong token" — it is
     * "a control the user cannot see", and that is what should fail.
     */
    @Test
    @Config(qualifiers = "night")
    fun the_verb_bar_is_legible_at_night() {
        setScreen(selectedText())
        composeRule.waitForIdle()
        val bar = composeRule.onNodeWithTag(BenchContextBarTestTag).fetchSemanticsNode().boundsInWindow
        val bmp = composeRule.activity.window.decorView.rasterizeToBitmap()

        // The bar's own fill, taken from a gap between two verbs rather than from a glyph.
        val fill = bmp.getPixel(bar.left.toInt() + 2, bar.center.y.toInt())
        val edit = composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.EDIT}")
            .fetchSemanticsNode().boundsInWindow
        var widest = 0
        for (y in edit.top.toInt() + 2 until edit.bottom.toInt() - 2) {
            for (x in edit.left.toInt() + 2 until edit.right.toInt() - 2) {
                val p = bmp.getPixel(x, y)
                val d = listOf(0, 8, 16).sumOf { s ->
                    kotlin.math.abs(((p shr s) and 0xFF) - ((fill shr s) and 0xFF))
                }
                if (d > widest) widest = d
            }
        }
        // The defect measured 7 across all three channels combined. Correct dark theme is cream on near
        // black — several hundred. 150 sits far from both, so this is not a threshold tuned to today's
        // pixels; it is the gap between "readable" and "not drawn at all".
        assertTrue("the verb label must be legible against the bar it sits on (was $widest)", widest > 150)
    }

    /**
     * The second one the device found, and the one no single package could have predicted.
     *
     * C2a made a tap outside the selection dismiss it (OD-13); C2b floated a card over the canvas. Their
     * intersection is the card's own dead space — 8dp of padding and 6dp between each pair of verbs —
     * which passed the tap straight through to the canvas underneath. Aiming at the toolbar and missing by
     * three density-independent pixels therefore deselected the element and took the toolbar away with it:
     * measured on hardware, four times out of four. Neither package is wrong on its own, which is exactly
     * why this is asserted at the assembly and not in either component's own suite.
     */
    @Test
    fun a_tap_that_lands_on_the_bar_but_misses_a_verb_keeps_the_selection() {
        val store = selectedText()
        setScreen(store)
        composeRule.waitForIdle()
        val bar = composeRule.onNodeWithTag(BenchContextBarTestTag).fetchSemanticsNode().boundsInWindow
        val edit = composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.EDIT}")
            .fetchSemanticsNode().boundsInWindow
        // The card's top-left padding: inside the bar, above and left of the first verb — the frozen 8dp.
        val x = ((bar.left + edit.left) / 2f) - bar.left
        val y = ((bar.top + edit.top) / 2f) - bar.top
        composeRule.onNodeWithTag(BenchContextBarTestTag).performTouchInput { click(Offset(x, y)) }
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()

        assertEquals(1, store.uiState.value.selection.size)
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.EDIT}").assertExists()
    }

    // ── D-039 — one capability, one visible presentation at a time ─────────────────────────────────

    /**
     * The assertion that replaced its own opposite.
     *
     * C2b shipped with a test that *pinned* two `Delete` controls as the priced cost of OD-11's additive,
     * and device Pass 2 found that price too high: the same verb offered twice on one screen reads as a
     * malfunction to a first-time user. The owner's D-039 ruling keeps both bars and assigns them
     * responsibilities instead — element verbs to the frozen bar, transform verbs to the shipped one — so
     * the count that used to be 2 must now be exactly 1.
     */
    @Test
    fun only_one_control_offers_Delete_while_the_frozen_bar_is_up() {
        setScreen(selectedText())
        composeRule.waitForIdle()
        assertEquals(
            1,
            composeRule.onAllNodesWithContentDescription(Copy.BenchVerbs.DELETE).fetchSemanticsNodes().size,
        )
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.DELETE}").assertExists()
        composeRule.onNodeWithTag("$EditorContextBarTestTag-${Copy.A11y.DELETE}").assertDoesNotExist()
    }

    /**
     * The other half, and the one that keeps the ruling honest: *"no functionality is removed."* The
     * transform bar's Delete is withheld only while another visible control offers it. Open the Type bar
     * and the frozen bar stands down — so Delete must come straight back, or this would be a capability
     * lost rather than a presentation assigned.
     */
    @Test
    fun the_transform_bar_takes_Delete_back_the_moment_the_frozen_bar_stands_down() {
        val store = selectedText()
        setScreen(store)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.SIZE}").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.DELETE}").assertDoesNotExist()
        composeRule.onNodeWithTag("$EditorContextBarTestTag-${Copy.A11y.DELETE}").assertExists()
        assertEquals(
            1,
            composeRule.onAllNodesWithContentDescription(Copy.A11y.DELETE).fetchSemanticsNodes().size,
        )
    }

    private fun selectedPhoto(): EditorStore = store().also {
        it.dispatch(
            Intent.CommitAddImage(
                ImageElement(id = "photo", transform = Transform(20.0, 20.0, 40.0, 30.0), assetId = "a"),
            ),
        )
    }

    /**
     * The pair a first-time user actually met on the device: `Reframe` on the photo and `Reframe` in the
     * bar, 150px apart, at the same moment. The written note was *"did I do something wrong?"* — so the
     * chip yields to the bar, which says the same word with a label rather than over the artwork.
     */
    @Test
    fun the_on_canvas_Reframe_chip_yields_to_the_bar_that_offers_the_same_verb() {
        setScreen(selectedPhoto())
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.REFRAME}").assertExists()
        composeRule.onNodeWithTag(ReframeChipTestTag).assertDoesNotExist()
        // One Delete for a photo too — the frozen bar's.
        assertEquals(
            1,
            composeRule.onAllNodesWithContentDescription(Copy.BenchVerbs.DELETE).fetchSemanticsNodes().size,
        )
    }

    /**
     * The ruling protects the transform verbs absolutely — they are what ADR-029 §6 exists for, being the
     * verbs `drag` has no single-pointer twin for. Whatever the frozen bar is doing, all ten stay put.
     */
    /**
     * D-040 — the dead end review found, which no test covered and Pass 2 would have.
     *
     * `benchVerbKindOf` keys on element *type*, so a still-blank box got the full text set. Tapping `Size`
     * set `typeBarOpen`, which hid the frozen bar (its own `!typeBarOpen` term) while the Type bar declined
     * to appear (`styleTarget` is null for a blank box) — and the reset effect is keyed on `styleTarget?.id`,
     * still null, so it never re-ran. The bar stayed gone across later selections too. Now Size and Ink are
     * inert there, in the class OD-9 already established for `Font`, so the tap cannot happen.
     */
    @Test
    fun a_still_blank_text_box_is_offered_neither_Size_nor_Ink() {
        val store = store()
        store.dispatch(Intent.PlaceText(Transform(20.0, 20.0, 20.0, 20.0), "   "))
        setScreen(store)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.SIZE}").assertIsNotEnabled()
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.INK}").assertIsNotEnabled()
        // Delete stays live — a blank box is exactly the one you most want to get rid of.
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.DELETE}").assertIsEnabled()
    }

    @Test
    fun the_ten_transform_verbs_are_never_withheld() {
        setScreen(selectedPhoto())
        composeRule.waitForIdle()
        listOf(
            Copy.A11y.MOVE_LEFT, Copy.A11y.MOVE_RIGHT, Copy.A11y.MOVE_UP, Copy.A11y.MOVE_DOWN,
            Copy.A11y.MAKE_LARGER, Copy.A11y.MAKE_SMALLER,
            Copy.A11y.ROTATE_CLOCKWISE, Copy.A11y.ROTATE_COUNTERCLOCKWISE,
            Copy.A11y.BRING_FORWARD, Copy.A11y.SEND_BACKWARD,
        ).forEach { composeRule.onNodeWithTag("$EditorContextBarTestTag-$it").assertExists() }
    }

}
