package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aritr.zinely.ui.theme.rememberReduceMotion
import com.aritr.zinely.core.editor.EditorUiState
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.editor.LiveTransform
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.TextStyle
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.render.android.AssetBytesSource
import com.aritr.zinely.render.android.readImageIntrinsics
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext

/** Test tag on the editor canvas Box (the measured, gesture-bearing area). */
public const val EditorCanvasTestTag: String = "editor-canvas"

/** Test tag on the paper surface under the page render (the page-footprint paper backing). */
public const val EditorPaperSurfaceTestTag: String = "editor-paper"

/** Test tag on the top "Preview" entry point (shown only when the host provides an `onPreview`). */
public const val EditorPreviewActionTestTag: String = "editor-preview-action"


/**
 * The S4 editor host (ADR-029 §5, §6) — the screen that assembles every interaction layer over one
 * [EditorStore]. It is the seam the per-increment components were built against: it owns the **ephemeral**
 * gesture state ([live] / [resizeOverride], feature-layer per the §5.1 contract — never in the reducer),
 * measures the canvas, and feeds the resulting scale back into the model so every layer shares one
 * [com.aritr.zinely.core.editor.ViewState].
 *
 * **Single viewport source.** The page is fit into the measured canvas; the host dispatches
 * [Intent.SetViewport] (display-only — no autosave, no history) so the gesture commit (`LiveSnap` over
 * `screenPxPerPt`) and the preview render agree exactly (preview == commit). [pageSizePt] is hoisted —
 * imposition owns the panel size; this host does not derive it.
 *
 * **Layer stack** (bottom → top), siblings sized identically so device-px coordinates align:
 *  1. [EditorPagePreview] — the decorative page render + snap guides + selection chrome (no pointer input).
 *  2. The gesture surface — [editorTransformGestures]: long-press select, double-tap → [Intent.BeginEditTextAt],
 *     pan/pinch/rotate → live preview + one [Intent.CommitTransform].
 *  3. [ResizeHandles] — eight opposite-anchor handles (single selection); they consume their own pointers,
 *     so they sit above the gesture surface and win the hit-test without racing it.
 *  4. [ElementSemanticsLayer] — the accessible mirror (semantics-only, consumes nothing).
 *  5. The [EditTextSession] overlay when an [Interaction.EditingText] is open (IME-padded sheet).
 * The [EditorContextBar] (the visible 2.5.7 single-pointer twins) sits below the canvas, shown on selection.
 *
 * Stateless beyond the two gesture accumulators: [store] is hoisted (constructed with its effect runner at
 * the DI/app layer). The model is collected with `collectAsStateWithLifecycle`; the gesture/handle layers
 * read the *latest* snapshot synchronously via `{ store.uiState.value }` (the token-read contract, §5.1).
 *
 * @param store the editor MVI store (its `uiState` is the single source of truth).
 * @param pageSizePt the edited page/panel size in points; hoisted from imposition (also the page clip).
 * @param modifier sizing/placement for the whole screen.
 * @param imageBytes import-master byte source for image elements; defaults to the missing-asset placeholder.
 * @param moveResizeHintSeen the persisted "already seen the one-time move/resize hint" gate (ADR-032),
 *   hoisted from the app/VM over the local preferences store, as a **load-aware tri-state**: `null` =
 *   not yet loaded, `false` = loaded & unseen, `true` = loaded & seen. The hint shows only on `false`, so
 *   a not-yet-loaded (`null`) flag can never flash it; and persistence on discovery fires unless the flag
 *   is *positively* `true`, so a first gesture during the load window still records the hint as seen
 *   (avoids re-teaching next launch). Defaults to `false` (loaded-unseen) for tests.
 * @param onMoveResizeHintSeen invoked when the hint is dismissed — via "Got it" or via discovery (a live
 *   drag/resize) — so the host can persist the flag. Idempotent on the store side. Defaults to a no-op.
 * @param savedSignals the autosave-event stream (ADR-034): each emission is one autosave *scheduled*
 *   (mark-dirty), raised by the existing `Effect.Autosave` path (runner → app `SharedFlow`) — it signals
 *   that work is being saved, not that a write has completed. The host surfaces the transient "Saved ✨"
 *   reassurance per emission (optimistic, not a completion receipt — ADR-034), coalescing a burst into one
 *   visible window. Defaults to an empty flow (no confirmation) for previews/tests.
 * @param saveError the kind of unresolved autosave failure currently known for this project, or `null`
 *   when there is none (ADR-035/[ADR-036](../DECISIONS.md#adr-036), hoisted from the app over the
 *   `SaveFailureSink` of ADR-026 §5; the app maps `DataError` → a feature-local [SaveErrorKind] so this
 *   layer stays free of core/data-android types). When non-null the host shows the warm
 *   [EditorSaveFailure] banner (copy keyed by the kind) **and** suppresses the optimistic "Saved ✨" chip
 *   and the move/resize hint — the editor must not claim a save it knows failed. Defaults to `null`.
 * @param onDismissSaveError invoked when the user taps the failure banner's "Got it" — the app clears
 *   the failure from the sink. Defaults to a no-op.
 * @param onRetrySaveError invoked when the user taps the failure banner's "Try now" ([ADR-038](../DECISIONS.md#adr-038))
 *   — the app forces an immediate save; the outcome flows through the ADR-037 path (clears on success,
 *   re-reports on failure). Defaults to a no-op.
 * @param onStyleAnnounce speaks a discrete Type-bar style change (FR-3, [ADR-055](../DECISIONS.md#adr-055),
 *   WCAG 4.1.3). Same contract and same host drain as [onReframeAnnounce] — a separate parameter only so
 *   the two surfaces stay independently testable. Defaults to a no-op (previews/tests).
 * @param onPreview invoked by the "Preview" entry point to open the unified Proof surface (M5,
 *   [ADR-051](../DECISIONS.md#adr-051) — the reader's-booklet PreviewScreen it once opened is retired,
 *   superseded by the imposed-sheet-first Proof). `null` (the default) hides the affordance entirely, so a
 *   screen without a proof destination (previews/tests) is unchanged; the app passes a navigate action.
 */
@Composable
public fun EditorScreen(
    store: EditorStore,
    pageSizePt: PtSize,
    modifier: Modifier = Modifier,
    imageBytes: AssetBytesSource = EmptyAssetBytes,
    moveResizeHintSeen: Boolean? = false,
    onMoveResizeHintSeen: () -> Unit = {},
    reframeCoachSeen: Boolean? = false,
    onReframeCoachSeen: () -> Unit = {},
    onReframeAnnounce: (String) -> Unit = {},
    onStyleAnnounce: (String) -> Unit = {},
    savedSignals: Flow<Unit> = emptyFlow(),
    saveError: SaveErrorKind? = null,
    onDismissSaveError: () -> Unit = {},
    onRetrySaveError: () -> Unit = {},
    onPreview: (() -> Unit)? = null,
) {
    val uiState by store.uiState.collectAsStateWithLifecycle()
    val dispatch: (Intent) -> Unit = store::dispatch
    val currentState = { store.uiState.value }

    // A known save failure (ADR-035) gates the optimistic "Saved ✨" + the move/resize hint and raises the
    // banner. The presence flag drives all the suppression below; the kind only selects the banner copy.
    val saveErrorVisible = saveError != null
    // Retain the last shown kind so the copy stays put through the banner's exit fade (when `saveError`
    // returns to null on dismissal, `saveErrorVisible` flips false but the text must not flip too).
    var lastSaveErrorKind by remember { mutableStateOf(SaveErrorKind.Generic) }
    if (saveError != null) lastSaveErrorKind = saveError

    // Transient "Saved ✨" reassurance (ADR-034). Driven solely by the existing autosave event stream — no
    // new save logic. `collectLatest` coalesces a burst of saves (e.g. several quick commits) into one
    // visible window: each new save cancels the prior dismissal timer and restarts it, so the chip stays
    // up once rather than flickering, and TalkBack's polite live region announces once per appearance.
    var savedVisible by remember { mutableStateOf(false) }
    LaunchedEffect(savedSignals, saveErrorVisible) {
        // Clear any stuck state first: if the source flow is ever swapped mid-window, the prior collector
        // is cancelled before its dismissal timer fires, so reset on (re)subscribe (Codex review #3).
        // Keying on saveErrorVisible also closes an honesty hole (ADR-035, Codex Required Fix): a known
        // failure cancels the in-flight "Saved" window (savedVisible → false) and stops collecting, so the
        // chip can't resurrect from a stale timer when the user dismisses the banner. Only a *new* save
        // signal after the failure clears re-lights it.
        savedVisible = false
        if (saveErrorVisible) return@LaunchedEffect
        savedSignals.collectLatest {
            savedVisible = true
            delay(SavedConfirmationVisibleMs)
            savedVisible = false
        }
    }

    // Feature-ephemeral gesture accumulators — the live pan/pinch frame and the handle-resize override.
    // They never reach the reducer; only the baked CommitTransform does (§5.1). Handle drags consume their
    // pointers, so at most one of the two is non-null at a time; the preview prioritises resizeOverride.
    var live by remember { mutableStateOf<LiveTransform?>(null) }
    var resizeOverride by remember { mutableStateOf<Map<String, Transform>?>(null) }

    // The same feature-ephemeral idiom for a settling Type-bar size burst (ADR-055): the in-flight style
    // the canvas paints while the 400ms settle coalesces the undo entry. Like the two above it never
    // reaches the reducer — only the one Intent.StyleText on settle does — and the Type bar clears it on
    // dispose, so it cannot outlive the bar that owns it.
    var styleOverride by remember { mutableStateOf<Map<String, TextStyle>?>(null) }

    // Feature-ephemeral Reframe session state (ADR-053 §5.1): the fit/zoom/pan draft + the decoded photo
    // aspect, held here for the life of one Interaction.Reframing and baked to the document only on Done
    // (Intent.CommitReframe) — the reducer never sees the live pan/zoom, exactly like the transform `live`.
    val reframing = uiState.interaction as? Interaction.Reframing
    var reframeDraft by remember { mutableStateOf<FramingDraft?>(null) }
    var reframePratio by remember { mutableStateOf<Double?>(null) }
    // Whether this session's photo can be read at all: `null` until resolved, then true/false. Reframe
    // chrome is presented **only** on `true` (M7-01 / RF-4). Gating presentation rather than cancelling
    // after it is what makes "do not enter Reframe for an undisplayable photo" literal: an unreadable
    // photo never reaches `true`, so no overlay and no controls are ever composed — where previously the
    // session rendered for the duration of the readability read before being cancelled.
    var reframeReadable by remember { mutableStateOf<Boolean?>(null) }
    var reframeAdjusted by remember { mutableStateOf(false) }
    // The Reframe screen-reader announcements (bench `#rfLive` / `rfSay`): every discrete adjustment, fit
    // change, and session end speaks (WCAG 4.1.3). Routed through [onReframeAnnounce] to the host's
    // `announceForAccessibility` drain — the SAME channel the reducer's selection/undo announcements use.
    // The platform re-announces even identical consecutive text (so a repeated ← nudge is never silent,
    // Review finding #1) and leaves no lingering live-region node to become a stale focus stop (#2).
    val latestAnnounce by rememberUpdatedState(onReframeAnnounce)
    val sayReframe = { msg: String -> latestAnnounce(msg) }
    // The editor's hardware-keyboard receiver (installed on the root Column below), shared by the two
    // grammars that need one: the Reframe session (bench: arrows nudge · +/− zoom · Enter saves · Esc
    // cancels) and the FR-3 bold/italic shortcuts (ADR-055 §4). Focus is requested when either becomes
    // live, so keystrokes route here without a prior tap.
    val editorKeyFocus = remember { FocusRequester() }
    val reduceMotion = rememberReduceMotion()
    val bratioOf = { el: ImageElement -> el.transform.widthPt / el.transform.heightPt }
    val adjustDraft = { d: FramingDraft? -> reframeDraft = d; reframeAdjusted = true }

    // ── FR-3 Text styling (ADR-055) ────────────────────────────────────────────────────────────────
    // The one text box the Type bar can act on, or null. Derived from the store every recomposition —
    // never cached — so undo/redo and any document change re-sync the open bar automatically. The
    // guards mirror the reducer's own StyleText no-ops (absent / not text / blank), so the surface
    // never offers a control the reducer would silently refuse; the inline editor owns styling of its
    // own session, so Style stands down while one is open (ADR-055 §4).
    val styleTarget = uiState.selection.singleOrNull()
        ?.let { id -> uiState.document.pages[uiState.currentPageIndex].elements.firstOrNull { it.id == id } }
        ?.let { it as? TextElement }
        ?.takeIf { it.text.isNotBlank() && uiState.interaction !is Interaction.EditingText }
    // Type-bar visibility is surface-only state (a disclosure flag, not a styling draft): the bar is
    // non-modal and the reducer neither knows nor needs to know it is open.
    var typeBarOpen by remember { mutableStateOf(false) }
    // Any change of the styleable element closes the bar (ADR-055 §3: "a selection change to a non-text
    // or empty element closes the Type bar"). Keyed on the id, so committing a style through the bar —
    // which changes the element but not its id — leaves it open, exactly as the frozen prototype does.
    // Entering an inline edit session nulls `styleTarget` too, so the bar closes for the session and
    // reopens closed on exit — the same key, one rule.
    //
    // The same effect hands the canvas the keyboard whenever a styleable box becomes the target, so
    // Ctrl/Cmd+B works on a fresh selection without a prior tap on the canvas (the Reframe session does
    // exactly this at its own entry). It cannot fight the inline editor for focus: `styleTarget` is null
    // for the whole of an EditingText session, which is when the text field owns focus.
    LaunchedEffect(styleTarget?.id) {
        typeBarOpen = false
        if (styleTarget != null) runCatching { editorKeyFocus.requestFocus() }
    }
    // Style announcements ride the host's existing announceForAccessibility drain — the same channel
    // Reframe and the reducer's selection/undo announcements use (no second live-region mechanism).
    val latestStyleAnnounce by rememberUpdatedState(onStyleAnnounce)
    val sayStyle = { msg: String -> latestStyleAnnounce(msg) }
    val styleBuzz = rememberStyleBuzz()

    // Open/refresh the draft when a session begins (keyed on token): seed from the current framing so
    // reframing continues from the present look, then re-seed once the true photo aspect decodes — unless
    // the user already adjusted the draft. Clears when the session ends.
    LaunchedEffect(reframing?.token) {
        val rf = reframing
        reframeAdjusted = false
        reframePratio = null
        reframeReadable = null // nothing is presented until this resolves true
        reframeDraft = rf?.let { Framing.seedDraft(it.before, bratioOf(it.before), bratioOf(it.before)) }
        if (rf != null) {
            // **Refuse a session we cannot honour (M7-01 / RF-4, founder Choice 1).** A photo whose
            // intrinsic size cannot be read is a photo that cannot be framed, so the session is declined
            // rather than opened inert.
            //
            // Refusal lives here, at the surface, rather than in the reducer: `Intent.DoubleTapAt` opens
            // the session inside the pure `:core:editor` reducer, which cannot read asset bytes without
            // breaking core purity. Doing it here also covers all three entry paths — double-tap, the
            // Reframe chip, and the a11y custom action — under one rule. It runs *before* the entry
            // announcement, so a refused session never announces that it started.
            //
            // This catches the common cases (absent / corrupt master) cheaply, with a header read that
            // allocates no pixels. It cannot predict a pixel decode that fails later, so the in-session
            // gate below remains as the second layer for that rarer case.
            //
            // **Keyboard ownership is claimed BEFORE the read (M7-01-R1).** Focus is keystroke *routing*,
            // not presentation: the receiver is the root Column, which is composed either way, and every
            // reframe verb it serves is independently gated (adjustments on `reframePratio`, chrome on
            // `reframeReadable`), so nothing becomes reachable early. Claiming it after the read left a
            // window where the session existed but keystrokes went nowhere — Escape pressed immediately
            // on entry was simply lost, because `withContext(Dispatchers.IO)` suspends past the point
            // Compose considers the effect idle. Escape during the window now cancels, which is exactly
            // what the user asked for.
            runCatching { editorKeyFocus.requestFocus() }
            val measurable = withContext(Dispatchers.IO) {
                readImageIntrinsics(imageBytes, rf.before.assetId)
            }
            if (measurable == null) {
                if (ReframeUnavailableAnnouncement.isNotBlank()) sayReframe(ReframeUnavailableAnnouncement)
                dispatch(Intent.CancelReframe(rf.token))
                return@LaunchedEffect
            }
            // Readable: release the chrome. Nothing above this line has presented anything, so a refused
            // session is never seen — the session exists in the reducer for as long as this read takes,
            // but it has no surface until here.
            reframeReadable = true
            // Announce entry. The coach-mark has now done its teaching job (bench `taughtReframe = true`),
            // so persist it unless already positively seen. (The keyboard was claimed before the read.)
            sayReframe(
                "Reframing photo. Drag to reposition, pinch to zoom, or use the on-screen " +
                    "move and zoom controls. Done saves, Cancel discards.",
            )
            if (reframeCoachSeen != true) onReframeCoachSeen()
        }
    }
    LaunchedEffect(reframing?.token, reframePratio) {
        val rf = reframing
        val pr = reframePratio
        if (rf != null && pr != null && !reframeAdjusted) reframeDraft = Framing.seedDraft(rf.before, pr, bratioOf(rf.before))
    }

    // Bake the current draft to the document (Done, page-switch, or backgrounding) via the token-gated
    // Intent.CommitReframe — reading the LATEST draft/aspect (they change every gesture frame). A stale
    // token (after cancel/new session) is rejected by the reducer, so an over-fire is a safe no-op.
    val latestReframe by rememberUpdatedState(reframing)
    val latestDraft by rememberUpdatedState(reframeDraft)
    val latestPratio by rememberUpdatedState(reframePratio)
    val commitReframe = {
        val rf = latestReframe
        val d = latestDraft
        if (rf != null && d != null) {
            val br = bratioOf(rf.before)
            val pr = latestPratio
            // A null aspect means the overlay never displayed this photo, so the session was inert and
            // there is nothing to bake. Committing `before` unchanged still ends the session through the
            // same token-gated intent, and makes it impossible for a blind session to rewrite framing —
            // the divergence INV-01 found was exactly a crop baked against a photo nobody could see.
            val after = if (pr != null) Framing.toImage(rf.before, d, pr, br) else rf.before
            // Speak the outcome (bench: "Framing saved." vs "Framing unchanged.") — the same crop/fit
            // comparison the reducer uses to decide whether a command is recorded.
            sayReframe(
                if (after.crop != rf.before.crop || after.fit != rf.before.fit) "Framing saved." else "Framing unchanged.",
            )
            dispatch(Intent.CommitReframe(rf.id, after, rf.token))
        }
    }

    // The Reframe adjustment verbs, shared by the on-screen controls AND the hardware keyboard so the two
    // paths can never diverge (they mutate the same ephemeral draft and speak the same live-region line).
    // Every adjustment verb is gated on `reframePratio`, which the overlay reports only when the photo is
    // both measurable and displayed (M7-01). Until it arrives the frame is inert — keyboard, on-screen
    // controls, and pointer gestures alike — so no draft can ever be built against a photo the user cannot
    // see, and `reframePratio` is guaranteed non-null wherever the commit resolves geometry.
    val reframeNudge = { dx: Int, dy: Int ->
        val rf = reframing
        val d = reframeDraft
        val pr = reframePratio
        if (rf != null && d != null && pr != null) {
            val br = bratioOf(rf.before)
            adjustDraft(Framing.nudged(d, dx, dy, pr, br))
            sayReframe(
                when {
                    dx < 0 -> "Moved left"; dx > 0 -> "Moved right"; dy < 0 -> "Moved up"; else -> "Moved down"
                },
            )
        }
        Unit
    }
    val reframeZoom = { factor: Double ->
        val rf = reframing
        val d = reframeDraft
        val pr = reframePratio
        if (rf != null && d != null && pr != null) {
            val br = bratioOf(rf.before)
            val nd = Framing.clampPan(Framing.zoomed(d, factor), pr, br)
            adjustDraft(nd)
            sayReframe("Zoom ${(nd.zoom * 100).roundToInt()} percent")
        }
        Unit
    }
    val reframeSetFit = { f: FrameFit ->
        val d = reframeDraft
        if (d != null && reframePratio != null) {
            adjustDraft(applyFit(d, f))
            sayReframe(
                if (f == FrameFit.FILL) {
                    "Filling the frame. Edges may be cropped."
                } else {
                    "Showing the whole photo. Margins may appear on paper."
                },
            )
        }
        Unit
    }
    val reframeReset = {
        if (reframePratio != null) {
            adjustDraft(Framing.DEFAULT_FILL)
            sayReframe("Framing reset. Cancel to undo.")
        }
        Unit
    }
    val reframeCancel = {
        val rf = reframing
        if (rf != null) {
            // Announce only a session the user was actually told had started (M7-01-R1). Escape is live
            // from the instant the session exists — before the readability read resolves — so a cancel
            // can land while the entry line has not been spoken. Saying "Reframing cancelled." then
            // would report the end of something a screen-reader user never heard begin. The cancel
            // itself still happens; only the announcement is conditioned on the entry announcement.
            if (reframeReadable == true) sayReframe("Reframing cancelled.")
            dispatch(Intent.CancelReframe(rf.token))
        }
        Unit
    }

    // Durability force-commit (ADR-009, mirroring EditTextSession): backgrounding the editor flushes the
    // open framing so the autosave the commit emits runs before the process can be killed.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, reframing?.token) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_PAUSE) commitReframe() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // One-time move/resize hint, now persisted **across sessions** (ADR-032): [moveResizeHintSeen] is the
    // load-aware across-install gate (null=loading / false=unseen / true=seen) hoisted from the app's
    // preferences store, [moveResizeHintDismissed] is the within-session latch. The hint shows only when
    // the gate is loaded-unseen and the latch is clear. A live drag/resize counts as discovery (discovery
    // *is* dismissal) and dismisses + persists it too.
    var moveResizeHintDismissed by remember { mutableStateOf(false) }
    LaunchedEffect(live != null || resizeOverride != null) {
        if ((live != null || resizeOverride != null) && !moveResizeHintDismissed) {
            moveResizeHintDismissed = true
            // Persist unless the flag is *positively* already-seen. `null` (still loading) persists too, so
            // a first gesture before the value loads is recorded (Codex RF1) — the write is idempotent, so
            // the only cost is one redundant set on the rare pre-load discovery; later loaded-true sessions
            // (the common case) skip the write entirely.
            if (moveResizeHintSeen != true) onMoveResizeHintSeen()
        }
    }

    Column(
        modifier = modifier
            // The editor's keyboard receiver, serving the two grammars that need one: the Reframe session
            // (bench: reframe owns the keyboard) and the FR-3 bold/italic shortcuts. Every branch routes
            // through the SAME shared verbs the on-screen controls use, so keyboard and touch never diverge.
            //
            // It sits on the WHOLE screen, not on the canvas, for the reason the prototype binds its
            // handler to `document`: focus moves around inside the editor — tapping the Style hat with a
            // mouse focuses that button — and a canvas-scoped receiver would go silently dead the moment
            // focus left the canvas subtree, which is precisely when a hardware-keyboard user is working.
            // As an ancestor of every control, its *preview* pass runs before the focused node whatever
            // holds focus.
            .focusRequester(editorKeyFocus)
            // focusTarget, NOT focusable(). Two deliberate differences, both wanted here:
            //  · No focus SEMANTICS — `focusable` installs a `focused` property + requestFocus action,
            //    i.e. exactly the stray TalkBack stop that the old Reframe-only gate existed to avoid.
            //    `focusTarget` is the focus half without the semantics half, so the receiver can be
            //    permanent (a shortcut is live most of the time, not just during a Reframe session)
            //    without adding a screen-reader stop.
            //  · Focusability.Always rather than `focusable`'s SystemDefined (keyboard-input-mode only),
            //    so the receiver holds focus in touch mode too — which is what keeps the shortcut alive
            //    across a selection change. The cost is one unindicated Tab stop on the editor root.
            .focusTarget()
            .onPreviewKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (reframing == null) {
                    // Industry-standard bold/italic (bench: `meta && b|i` while a text block is selected),
                    // ADR-055 §4. `styleTarget` IS the specified precondition — a single, non-blank text
                    // box, no inline edit session open — so the "suppressed inside the inline editor" rule
                    // is structural here rather than a second check that could drift: an open session nulls
                    // the target and this branch falls through to the focused text field. Returning true
                    // consumes the event (bench's `preventDefault`); the fall-through cannot inject rich
                    // text either way, because the Compose editor is plain-text — hence no Ctrl+U arm
                    // (bench needs one only to stop `contenteditable` from writing a `<u>`).
                    val target = styleTarget
                    if (target != null && (ev.isCtrlPressed || ev.isMetaPressed)) {
                        when (ev.key) {
                            Key.B -> { toggleBold(target, dispatch, sayStyle, styleBuzz); return@onPreviewKeyEvent true }
                            Key.I -> { toggleItalic(target, dispatch, sayStyle, styleBuzz); return@onPreviewKeyEvent true }
                            else -> Unit
                        }
                    }
                    return@onPreviewKeyEvent false
                }
                val step = if (ev.isShiftPressed) 3 else 1
                when (ev.key) {
                    Key.Escape -> { reframeCancel(); true }
                    Key.Enter, Key.NumPadEnter -> { commitReframe(); true }
                    Key.DirectionLeft -> { reframeNudge(-step, 0); true }
                    Key.DirectionRight -> { reframeNudge(step, 0); true }
                    Key.DirectionUp -> { reframeNudge(0, -step); true }
                    Key.DirectionDown -> { reframeNudge(0, step); true }
                    Key.Plus, Key.Equals -> { reframeZoom(Framing.ZOOM_STEP); true }
                    Key.Minus -> { reframeZoom(1.0 / Framing.ZOOM_STEP); true }
                    else -> false
                }
            },
    ) {
        // The "Preview" entry to the unified Proof surface (M5, ADR-051). A quiet top-end nav
        // action (not a thumb-zone craft supply — it advances the journey, it doesn't place content);
        // shown only when the host supplies a destination, so the editor's tested layout is unchanged
        // without one.
        if (onPreview != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = onPreview,
                    // Desk-level nav action — the coral accent sourced from the frozen `--coral` token
                    // (the migrated Legacy `primary` role, byte-identical) instead of the Material default.
                    colors = ButtonDefaults.textButtonColors(contentColor = ZinelyTheme.colors.coral),
                    modifier = Modifier
                        .testTag(EditorPreviewActionTestTag)
                        .semantics { contentDescription = "Preview" },
                ) { Text("Preview  ›") }
            }
        }
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag(EditorCanvasTestTag),
        ) {
            // Fit the whole page into the measured canvas (contain), top-left anchored (pan stays zero for
            // the MVP host; true centring/zoom is a follow-up). The scale is the single px-per-point source.
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()
            val scale: Float = remember(widthPx, heightPx, pageSizePt) {
                if (pageSizePt.width <= 0.0 || pageSizePt.height <= 0.0 || widthPx <= 0f || heightPx <= 0f) {
                    1f
                } else {
                    min(widthPx / pageSizePt.width, heightPx / pageSizePt.height).toFloat()
                }
            }
            val interaction = uiState.interaction
            val editing = interaction is Interaction.EditingText

            // Push the measured scale into the model so every layer shares it (idempotent — the reducer
            // no-ops an equal view). Deferred until no gesture/edit session is open (Codex RF3): a viewport
            // change re-keys the gesture `pointerInput(screenPxPerPt, …)`, restarting it mid-drag with no
            // cleanup — which would strand a `Transforming` session and a non-null `live`. Re-runs when the
            // interaction returns to Idle, so the latest scale is applied the instant the gesture ends.
            val idle = interaction is Interaction.Idle
            LaunchedEffect(scale, idle) {
                if (idle) dispatch(Intent.SetViewport(scale, PtPoint(0.0, 0.0)))
            }

            Box(modifier = Modifier.fillMaxSize()) {
                // The page footprint reads as paper — the frozen `--paper` sheet (bench.html `.panel`),
                // instead of the bare desk showing through. Purely a host backing UNDER the render: the
                // SceneRenderer contract
                // is untouched (a document background still paints over it; Background.None now shows
                // paper, matching export onto a white sheet). Sized to the page at the fitted scale,
                // top-left anchored like the render itself (pan is zero in the MVP host).
                val density = LocalDensity.current
                val paperWidth = with(density) { (pageSizePt.width * scale).toFloat().toDp() }
                val paperHeight = with(density) { (pageSizePt.height * scale).toFloat().toDp() }
                Box(
                    modifier = Modifier
                        .size(paperWidth, paperHeight)
                        .background(ZinelyTheme.colors.paper)
                        .testTag(EditorPaperSurfaceTestTag),
                )
                EditorPagePreview(
                    uiState = uiState,
                    defaults = uiState.document.defaults,
                    pageSizePt = pageSizePt,
                    live = live,
                    modifier = Modifier.fillMaxSize(),
                    resizeOverride = resizeOverride,
                    styleOverride = styleOverride,
                    imageBytes = imageBytes,
                )
                // The page gesture surface and resize handles are inert while a text session is open (Codex
                // RF1): otherwise a stray long-press/double-tap replaces `EditingText`, and the session's
                // onDispose commits a now-stale token → the draft is silently dropped. The handles also yield
                // to an in-flight page drag (`live != null`) so the two never run concurrent sessions (RF2).
                // The page gesture surface + handles are inert while a text session OR a Reframe session is
                // open: during reframe the ReframeOverlay owns pointer input (drag pans / pinch zooms the
                // photo), and a stray page long-press/double-tap must not replace the session.
                if (!editing && reframing == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .editorTransformGestures(
                                screenPxPerPt = uiState.view.screenPxPerPt,
                                pageOffset = uiState.view.pageOffset,
                                pageSizePt = pageSizePt,
                                currentState = currentState,
                                dispatch = dispatch,
                                onPreview = { live = it },
                                // The single double-tap seam (ADR-053 §4): the reducer retargets by the
                                // topmost element type — text → inline edit, image → Reframe.
                                onDoubleTap = { pagePoint -> dispatch(Intent.DoubleTapAt(pagePoint)) },
                            ),
                    )
                    if (live == null) {
                        ResizeHandles(
                            uiState = uiState,
                            currentState = currentState,
                            dispatch = dispatch,
                            onResize = { resizeOverride = it },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                // The Reframe preview overlay (ADR-053): the movable photo layer + scrim + rule-of-thirds,
                // driven by the ephemeral draft. Only while a session is open and its photo still exists (a
                // delete races it closed; the token guard then no-ops any trailing commit).
                val reframeEl = reframing?.let { rf ->
                    uiState.document.pages[uiState.currentPageIndex].elements.firstOrNull { it.id == rf.id } as? ImageElement
                }
                val currentDraft = reframeDraft
                if (reframeEl != null && currentDraft != null && reframeReadable == true) {
                    ReframeOverlay(
                        element = reframeEl,
                        draft = currentDraft,
                        screenPxPerPt = uiState.view.screenPxPerPt,
                        pageOffset = uiState.view.pageOffset,
                        imageBytes = imageBytes,
                        onAspect = { reframePratio = it },
                        onDraft = { adjustDraft(it) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // The persistent "Reframe" affordance chip (RF2): a single-selected photo, not editing/
                // reframing, always advertises that it can be reframed. Positioned just below the photo box.
                val selectedImage = uiState.selection.singleOrNull()?.let { id ->
                    uiState.document.pages[uiState.currentPageIndex].elements.firstOrNull { it.id == id } as? ImageElement
                }
                if (reframing == null && !editing && selectedImage != null) {
                    val spp = uiState.view.screenPxPerPt
                    val off = uiState.view.pageOffset
                    val chipX = (selectedImage.transform.xPt + selectedImage.transform.widthPt / 2.0 + off.x) * spp
                    val chipY = (selectedImage.transform.yPt + selectedImage.transform.heightPt + off.y) * spp
                    ReframeAffordanceChip(
                        onClick = { dispatch(Intent.BeginReframe(selectedImage.id)) },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset { IntOffset((chipX - 44.dp.toPx()).roundToInt(), (chipY - 36.dp.toPx()).roundToInt()) },
                        // First-run coach-mark: pulse only when the flag is loaded-unseen AND motion is
                        // allowed (WCAG 2.3.3). `null` (still loading) or seen ⇒ no pulse, never a flash.
                        teach = reframeCoachSeen == false && !reduceMotion,
                    )
                }
                // The accessible element mirror is suppressed during a Reframe session: its custom actions
                // (move/scale/rotate/delete) act on the underlying photo's geometry, which the reframe
                // commit rebuilds from `before` — so a mid-reframe element action would be silently
                // reverted. The Reframe controls carry the a11y path (nudge/zoom/fit/reset) while it's open,
                // matching how the page gesture surface is already inert here.
                if (reframing == null) {
                    ElementSemanticsLayer(
                        uiState = uiState,
                        dispatch = dispatch,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // First-run invitation: when the current page is blank and no text session is open, a
                // blank sheet reads as a void — so we overlay the cozy empty state (DESIGN-LANGUAGE §8/§9).
                // It is **invitation-only** ([ADR-033](../DECISIONS.md#adr-033)): the add actions live
                // solely in the persistent supply tray below, so "Add a photo" / "Add words" never appear
                // twice at once. The overlay just invites + points to the shelf; it disappears the instant
                // the page gets an element. Non-interactive, so touches fall through to the gesture surface.
                val currentPage = uiState.document.pages[uiState.currentPageIndex]
                val currentPageEmpty = currentPage.elements.isEmpty()
                if (currentPageEmpty && !editing) {
                    // First page keeps the warm welcome; a later blank page gets the lighter "fresh page"
                    // line (VOICE empty states). Only the headline differs — same invitation-only overlay.
                    // "First" is the page's own identity (front cover, or index 0), not just the cursor, so
                    // it stays correct if roles ever diverge from list position (Codex review); today's
                    // all-INTERIOR docs fall back to index 0.
                    EditorEmptyState(
                        modifier = Modifier.align(Alignment.Center),
                        firstPage = currentPage.role == PageRole.FRONT_COVER || currentPage.index == 0,
                    )
                }

                // One-time move/resize hint: the moment a placed element is single-selected (handles up,
                // not editing) we float in the gentle "drag to move · pinch to resize" note — those two
                // gestures have no discrete-control twin, so a beginner can miss them. It is non-blocking
                // (declares no pointerInput; touches fall through to the gesture surface) and one-time per
                // screen. Sits below the edit overlay so a text session always wins the top of the canvas.
                // Also gate on no in-flight gesture so the hint is gone the same frame a drag begins
                // (the LaunchedEffect makes that dismissal stick); avoids a one-frame overlap.
                // A known save failure (ADR-035) outranks the teaching hint at the top region: an
                // honest "couldn't save" must win over a one-time tip, so the hint yields while it shows
                // (it returns on the next eligible selection once the failure is dismissed/cleared).
                val gesturing = live != null || resizeOverride != null
                val showMoveResizeHint =
                    !editing && !gesturing && uiState.selection.size == 1 &&
                        !moveResizeHintDismissed && moveResizeHintSeen == false && !saveErrorVisible
                if (showMoveResizeHint) {
                    EditorMoveResizeHint(
                        onDismiss = {
                            moveResizeHintDismissed = true
                            onMoveResizeHintSeen()
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp),
                    )
                }

                // The transient "Saved ✨" autosave reassurance (ADR-034). Pinned to the top-*end* corner —
                // a quiet, non-thumb-zone spot well clear of the supply tray's primary actions below
                // (DESIGN-RULES R3/R7). It **yields to the move/resize hint**: the first element placement
                // both selects (raising the hint at TopCenter, up to 320dp wide) and autosaves, so on a
                // phone-width canvas a TopEnd chip could overlap the centered hint (Codex review #2). The
                // teaching hint wins; the chip simply skips that one window. Passive (no pointer input);
                // it fades itself out after the transient window.
                EditorSavedConfirmation(
                    visible = savedVisible && !showMoveResizeHint && !saveErrorVisible,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp),
                )

                // The warm save-failure banner (ADR-035) — the honest correction to the optimistic
                // "Saved ✨". Reuses the existing app-scoped SaveFailureSink (ADR-026 §5); no second save
                // system. Persistent (until "Got it" / project switch), unlike the transient chip, and
                // pinned TopCenter at the top of the canvas — well clear of the thumb-zone supply tray
                // (DESIGN-RULES R3/R7). It takes precedence over the chip + hint, both gated off above.
                EditorSaveFailure(
                    visible = saveErrorVisible,
                    onDismiss = onDismissSaveError,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp),
                    onRetry = onRetrySaveError,
                    kind = lastSaveErrorKind,
                )

                // The Type bar (FR-3, ADR-055, bench `.typebar`): a floating card pinned to the bottom of
                // the canvas — bench `position:absolute; bottom:calc(74px + safe-area)`, i.e. it FLOATS
                // OVER the page, above the bottom chrome. It is an overlay here for the same reason it is
                // absolutely positioned there: at four rows it is ~240dp tall, and putting it in the
                // bottom Column's flow steals that height from the weight(1f) canvas — on a short screen
                // the canvas collapses toward zero and the measure→Intent.SetView→remeasure scale
                // feedback never settles (an infinite recomposition, caught by TypeBarTest).
                //
                // It holds no styling state — it reads [styleTarget]'s live style and dispatches
                // Intent.StyleText — so an undo/redo that restores a different style re-syncs it for free
                // on the next recomposition (ADR-055 §3). Reveals without motion, deliberately: no
                // enter/exit animation to gate on reduced motion (ADR-055 §4).
                if (styleTarget != null && typeBarOpen && reframing == null) {
                    TypeBar(
                        element = styleTarget,
                        dispatch = dispatch,
                        onAnnounce = sayStyle,
                        onPreview = { styleOverride = it },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            // bench `.typebar{max-width:calc(100% - 24px)}` — the frozen cap, which the
                            // first port dropped. On a centred max-content card a symmetric 12dp padding
                            // IS that cap: it lowers the incoming max constraint, which is what the card's
                            // `width(IntrinsicSize.Max)` clamps against. Without it the bar has no floor
                            // under it on a narrow screen or a large font scale.
                            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                    )
                }

                // The text-edit overlay: only while a session is open and its element still exists (a delete
                // races it closed; the session's onDispose/token guard then no-ops the trailing commit).
                if (interaction is Interaction.EditingText) {
                    val editing = uiState.document.pages[uiState.currentPageIndex].elements
                        .firstOrNull { it.id == interaction.id } as? TextElement
                    if (editing != null) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .imePadding(),
                            // The text-edit overlay is a paper input panel — frozen `--paper`/`--ink`
                            // (bench edits in-place on the panel; this bottom sheet is the Compose host's
                            // input surface), off the abused Material `surface`/`onSurface` roles.
                            color = ZinelyTheme.colors.paper,
                            contentColor = ZinelyTheme.colors.ink,
                            tonalElevation = 3.dp,
                        ) {
                            EditTextSession(
                                session = interaction,
                                element = editing,
                                dispatch = dispatch,
                            )
                        }
                    }
                }
            }
        }

        // The supply tray: the visible shelf of craft supplies that replaces the app's lone "Add image"
        // FAB. Every primary action lives here in the thumb zone — add a photo (the old FAB's
        // Intent.RequestAddImage), add words (the empty-state add-text behavior), and undo/redo bound to
        // the real canUndo/canRedo so a disabled supply is visibly inert, not a dead tap.
        // Reframe swaps its chrome in over the supply tray + context bar (bench `toolbar[data-mode="reframe"]`).
        // Gated on `reframeReadable == true` alongside the overlay, so the controls and the photo layer
        // appear together or not at all — a refused session shows neither (M7-01 / RF-4).
        if (reframing != null && reframeReadable == true) {
            ReframeControls(
                fit = reframeDraft?.fit ?: FrameFit.FILL,
                zoomPercent = ((reframeDraft?.zoom ?: 1.0) * 100).roundToInt(),
                // Same shared verbs the keyboard uses (they announce + mutate the one draft) — parity by
                // construction, not by two copies of the math.
                onFit = reframeSetFit,
                onNudge = reframeNudge,
                onZoom = reframeZoom,
                onReset = reframeReset,
                onCancel = reframeCancel,
                onDone = { commitReframe() },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            EditorSupplyTray(
                canUndo = uiState.canUndo,
                canRedo = uiState.canRedo,
                onAddPhoto = { dispatch(Intent.RequestAddImage) },
                onAddText = { addTextAndEdit(pageSizePt, currentState, dispatch) },
                onUndo = { dispatch(Intent.Undo) },
                onRedo = { dispatch(Intent.Redo) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // The page navigator: makes all pages of the SINGLE_SHEET_8 document reachable (before this
        // only page 0 was). Each card mini-renders its page through the SAME render path the canvas
        // uses, so it reads as a real workbench; reads pages / current / size / defaults from the same
        // hoisted state and dispatches Intent.GoToPage; the reducer clears selection + returns to Idle
        // on the switch. Threads the host's imageBytes so a card's images match the canvas.
        EditorPageStrip(
            pages = uiState.document.pages,
            currentPageIndex = uiState.currentPageIndex,
            pageSizePt = pageSizePt,
            defaults = uiState.document.defaults,
            // Leaving the panel commits the open framing first (bench: never strand a session on an
            // off-screen photo), then navigates.
            onSelectPage = { idx ->
                if (reframing != null) commitReframe()
                dispatch(Intent.GoToPage(idx))
            },
            modifier = Modifier.fillMaxWidth(),
            imageBytes = imageBytes,
        )

        // The transform context bar is hidden during a Reframe session — the Reframe controls take over.
        // "During a session" means a *presented* one: while readability is still resolving, and for a
        // session that is refused outright, the bar stays put so the editor is visually untouched by a
        // session the user never sees (M7-01 / RF-4).
        if (reframing == null || reframeReadable != true) {
            EditorContextBar(
                selection = uiState.selection,
                dispatch = dispatch,
                modifier = Modifier.fillMaxWidth(),
                // Style is offered only where it can act (FR-3, ADR-055): a single, non-blank text box,
                // outside an inline edit session. Anything else — a photo, a multi-selection, a
                // still-blank box the reducer would refuse anyway — gets the bar exactly as before.
                onStyle = styleTarget?.let { { typeBarOpen = !typeBarOpen } },
                styleOpen = typeBarOpen,
            )
        }
    }
}

/**
 * Apply a fit choice to the working [FramingDraft] (bench `setFit`): choosing "Whole photo" re-centres to
 * a clean baseline (zoom 1, no pan); choosing "Fill" keeps the current pan/zoom.
 */
/**
 * **FOUNDER-OWNED COPY — AWAITING WORDING (M7-01 / RF-4).**
 *
 * The line spoken when Reframe is declined because the photo cannot be read. Deliberately empty: M7-01
 * was not authorised to invent user-facing text, and the founder is supplying it separately. While it is
 * empty the refusal is silent — which is a *known, temporary* Article 5 gap, not the intended end state:
 * declining without saying why is honest about the framing but not about the reason.
 *
 * Replace the empty string with the founder's wording; no other change is needed, as the refusal path
 * already speaks it through the same `announceForAccessibility` drain every other Reframe line uses.
 */
internal const val ReframeUnavailableAnnouncement: String = ""

private fun applyFit(draft: FramingDraft, fit: FrameFit): FramingDraft = when (fit) {
    FrameFit.WHOLE -> draft.copy(fit = FrameFit.WHOLE, zoom = Framing.MIN_ZOOM, panX = 0.0, panY = 0.0)
    FrameFit.FILL -> draft.copy(fit = FrameFit.FILL)
}

/**
 * "Add words" from the empty state: place an **empty** text box centered on the page and **open its
 * edit session immediately**, so the beginner goes straight to typing — no committed placeholder
 * sentence, and no reliance on the hidden double-tap-to-edit affordance (Codex UX finding). Composed
 * from existing intents: [Intent.PlaceText] reduces synchronously and selects the new element, so its
 * id is readable from [currentState] the instant `dispatch` returns (see `EditorStore` threading note),
 * and we hand it to [Intent.BeginEditText].
 *
 * Follow-up (tracked, not this slice): make this a single reducer-owned intent so a cancelled brand-new
 * empty text is removed and place+edit collapse into one undo step.
 */
internal fun addTextAndEdit(
    pageSizePt: PtSize,
    currentState: () -> EditorUiState,
    dispatch: (Intent) -> Unit,
) {
    dispatch(Intent.PlaceText(centeredTextBox(pageSizePt), ""))
    currentState().selection.singleOrNull()?.let { dispatch(Intent.BeginEditText(it)) }
}

/** A text box centered on the page (points) for a newly added text element. */
private fun centeredTextBox(page: PtSize): Transform {
    val w = page.width * 0.7
    val h = page.height * 0.16
    return Transform(
        xPt = (page.width - w) / 2.0,
        yPt = (page.height - h) / 2.0,
        widthPt = w,
        heightPt = h,
    )
}
