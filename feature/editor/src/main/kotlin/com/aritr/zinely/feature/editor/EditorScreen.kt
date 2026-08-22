package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntOffset
import androidx.activity.compose.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aritr.zinely.ui.theme.rememberReduceMotion
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.EditorUiState
import com.aritr.zinely.core.editor.FramingMath
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.editor.LiveTransform
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.TextCoverage
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.TextStyle
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.render.android.AssetBytesSource
import com.aritr.zinely.render.android.readImageIntrinsics
import com.aritr.zinely.ui.theme.LocalZinelyV2Colors
import com.aritr.zinely.ui.theme.LocalZinelyV21Colors
import com.aritr.zinely.ui.theme.ZinelyMakerInkId
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
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

    // The open text session's live script coverage (ADR-070). Fed by EditTextSession on the seed and on
    // every keystroke, reset to Covered on its dispose — so this holds only the *current* session's draft
    // coverage. The notice is additionally gated on `editing` below, so a stale value can never outlive
    // the session that produced it.
    var editCoverage by remember { mutableStateOf(TextCoverage.Covered) }

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
    // ----- C4 (ADR-094) -------------------------------------------------------------------
    // The Add chooser (rows 4.4a-4.4d). Open/closed only: the sheet itself is ZSheet, per OD-21.
    var addChooserOpen by remember { mutableStateOf(false) }

    // The Art sheet — the frozen `openArt()` (ADR-105 step S7). It is a *second* state rather than a mode
    // of `addChooserOpen` because the two are separate Compose `Dialog`s where the freeze had one `#sheet`
    // whose `innerHTML` was swapped. Both feed `benchStateOf` below: `openArt()` captions this surface as
    // a *variant of the Adding narration*, not as a state of its own, so the Bench is in `Adding` for
    // either sheet — a bar that returned to `Rest` behind an open cabinet would be the C9 invariant
    // failing in the one place nobody had opened yet. (The caption itself is not quoted here: C9's
    // narration guard scans comments too, and rightly.)
    //
    // **It carries its purpose rather than a second flag.** The same sheet now serves two verbs — Add ▸ Art
    // places a new supply, and a selected supply's `Replace` swaps that one — and the tile handler has to
    // know which. A `Boolean` plus an `artReplaceTarget: String?` beside it would be two states that can
    // disagree, which is exactly the shape [D-091](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-091)
    // cost this package a review cycle over. One nullable value, and the purpose is unconstructibly wrong.
    var artSheetFor by remember { mutableStateOf<BenchArtPurpose?>(null) }
    val artSheetOpen = artSheetFor != null

    // ----- C5 (ADR-095) -------------------------------------------------------------------
    // The page grid is *summoned*, never default (row 5.11a): this flag is the whole of its existence,
    // and while it is false the overlay composes nothing at all.
    var pageGridOpen by remember { mutableStateOf(false) }
    // Back stands the grid down instead of leaving the editor. The frozen HTML cannot specify this — a
    // prototype has no back button — but on Android an overlay you summoned is an overlay Back dismisses,
    // and the grid consumes every touch beneath it, so without this its only exits are `Done` and a cell.
    BackHandler(enabled = pageGridOpen) { pageGridOpen = false }

    // The frozen soft delete (rows 4.13-4.14). `del()` fades the element and shows a snack with
    // Undo; `undo()` restores it. The product already has a real undo stack, so the snack's Undo
    // dispatches Intent.Undo rather than carrying a private copy of the deleted element — one
    // history, one restore path, and the bar's Undo lights up for free because canUndo is already
    // true. The fade runs BEFORE the dispatch, because a dispatched delete has nothing left to fade.
    var deletingId by remember { mutableStateOf<String?>(null) }
    var deleteProgress by remember { mutableFloatStateOf(0f) }
    var snackVisible by remember { mutableStateOf(false) }
    var snackMessage by remember { mutableStateOf("") }
    // Which snack is up. The delete snack carries `Undo`; the ink snack (C6, row 4.15) carries no
    // button at all and stands for 1600ms rather than 3200 — the frozen `applyInk` builds it that way
    // because an ink is one undoable command the bar's own Undo already reverses.
    var snackAction by remember { mutableStateOf<String?>(null) }
    val c4Scope = rememberCoroutineScope()
    // One job, cancelled on re-entry: two deletes in quick succession must not leave the first
    // coroutine to clear `snackVisible` out from under the second, which is how a snackbar ends up
    // dismissing itself 200ms after it appears.
    val deleteJob = remember { arrayOfNulls<Job>(1) }

    // Frozen `del()` (`v2-bench.html:620-629`), in the order the freeze performs it: fade, then
    // remove, then say so. Row 4.13 asserts the three together because any one alone reads as a
    // different interaction - a fade with no snack is a disappearance, a snack with no fade is a
    // report about something that already vanished.
    // C9 row 9.2a: the frozen rule's selector is `*`, so this fade honours it like every other V2 animation.
    // It was the one call site in the Bench that did not — it read `tween(BenchDeleteFadeMillis)` flat, and
    // under a reduced-motion preference the element still faded for 200ms before vanishing. Hoisted out of
    // the lambda because `softDelete` runs in a coroutine, where a CompositionLocal cannot be read.
    val deleteFadeMillis = ZinelyTheme.v2Motion.durationMillis(BenchDeleteFadeMillis)
    val softDelete: (Set<String>) -> Unit = { ids ->
        val id = ids.firstOrNull()
        if (id != null) {
            val label = benchDeleteLabel(currentState().document.pages, id)
            deleteJob[0]?.cancel()
            deleteJob[0] = c4Scope.launch {
                deletingId = id
                animate(0f, 1f, animationSpec = tween(deleteFadeMillis)) { v, _ ->
                    deleteProgress = v
                }
                dispatch(Intent.Delete(ids))
                deletingId = null
                deleteProgress = 0f
                snackMessage = benchDeletedMessage(label)
                snackAction = UndoActionLabel
                snackVisible = true
                delay(BenchSnackDeleteMillis)
                snackVisible = false
            }
        }
    }

    /**
     * The ink a supply lands in — the maker palette's own `Ink` (`#2A251E`).
     *
     * ⚠ **SUPPLIES-SPEC is silent on the placement ink and this is an implementation reading owed a
     * ruling.** §0 O-A settles which *palette* may tint decor (the content palette, all three bands) and
     * says nothing about which swatch a first placement uses. `Ink` is chosen because it is the one swatch
     * that reads as a mark rather than as a choice: the alternatives are a coloured ink the maker did not
     * pick (a compositional decision the app made, which §5.1 forbids in the tilt case for exactly this
     * reason) or a paper tint, which §5's own text calls *"pale-on-pale … a legitimate riso result"* — fine
     * as a maker's choice, an invisible first placement as a default.
     *
     * Read from [ZinelyContentInks] rather than written as a literal so the value cannot drift from the
     * palette, and so a `ColorRgba` constant does not appear in a second place.
     */
    val supplyInk = ZinelyTheme.contentInks[ZinelyMakerInkId.Ink].value.toColorRgba()

    // Frozen `applyInk()` (`v2-bench.html:699-704`), in the order the freeze performs it: set the ink,
    // then say which one. The other two writes it makes are already delivered here by unidirectional
    // data flow rather than by this lambda — `$('editSw').style.background` is `BenchStyleRow`'s own
    // `inkSwatch`, which reads the element's live style, and `flashSaved()` is the `.status` chip C4
    // moved the autosave reassurance into, which the document change raises by itself.
    //
    // One `Intent.StyleText` per tap: an immediate-commit style change, so each ink is one undoable
    // command and the buttonless snack is honest — the bar's Undo is right there.
    /**
     * Frozen `openArt()`'s tile handler (`v21-bench.html:887-889`), in the order the freeze performs it:
     * close the sheet, put the supply on the page, say so.
     *
     * `selectByKind('decor')` is not transcribed as a third statement because
     * [Intent.PlaceSupply][com.aritr.zinely.core.editor.Intent.PlaceSupply] already auto-selects what it
     * placed, exactly as `PlaceText` does — the freeze's own select is the reducer's, not a second one.
     *
     * The frozen toast is `undoable=true`, so the snack carries `Undo`; one placement is one
     * [PlaceCommand][com.aritr.zinely.core.editor.PlaceCommand], so that button is honest. It shares
     * `deleteJob[0]` with the delete and ink snacks for the same reason they share it: one snack slot, so a
     * newer message always cancels the older one's dismissal timer rather than being cut short by it.
     */
    val placeSupply: (String) -> Unit = { supplyId ->
        // Read the purpose BEFORE closing the sheet — closing is what clears it.
        val purpose = artSheetFor
        artSheetFor = null
        when (purpose) {
            // ⚠ `null -> Unit`, deliberately NOT folded in with `Place`. The Replace arm below argues that a
            // missing target must drop the verb "rather than placing a stray supply, which is what a
            // `?: place` fallback would quietly do" — and `null, Place ->` was exactly that fallback one
            // level up, in the same `when`. Independent review caught the code disagreeing with its own
            // comment. Unreachable today (the sheet only fires `onPick` while visible, which needs a
            // non-null purpose), and it costs one line to keep it unreachable *and* harmless.
            null -> Unit
            // Add ▸ Art: a new element at page centre, at its family's size (§5, §5.1, §5.2).
            BenchArtPurpose.Place -> {
                dispatch(Intent.PlaceSupply(supplyId, supplyInk, benchSupplyPlacement(supplyId, pageSizePt)))
                deleteJob[0]?.cancel()
                deleteJob[0] = c4Scope.launch {
                    snackMessage = Copy.Snack.PLACED
                    snackAction = UndoActionLabel
                    snackVisible = true
                    delay(BenchSnackDeleteMillis)
                    snackVisible = false
                }
            }
            // §8 `Replace supply`. The transform is computed from the OUTGOING element, so the swap needs
            // it in hand — a target that has since been deleted resolves to null and the verb is dropped
            // rather than placing a stray supply, which is what a `?: place` fallback would quietly do.
            is BenchArtPurpose.Replace -> {
                val current = uiState.document.pages.getOrNull(uiState.currentPageIndex)
                    ?.elements?.firstOrNull { it.id == purpose.id } as? DecorElement
                if (current != null) {
                    dispatch(
                        Intent.ReplaceSupply(
                            id = purpose.id,
                            supplyId = supplyId,
                            // The owner's ruling: the incoming family's scale, the outgoing one's place.
                            transform = benchSupplyReplacement(supplyId, pageSizePt, current.transform),
                        ),
                    )
                }
                // No snack. The other three decor verbs narrate because their result is easy to miss —
                // a placement lands at page centre, a delete removes the thing you were looking at, an ink
                // is one property of many. A replacement redraws the selected mark in place, under the
                // maker's own eyes, with the selection chrome still around it. A snack here would be the
                // app announcing what the maker just watched happen.
            }
        }
    }

    /**
     * One popover, two verbs — the ink swatch has to reach whichever kind of element is selected.
     *
     * ⚠ **This lambda used to dispatch [Intent.StyleText] for whatever was selected.** That was correct only
     * while decor's `Ink` verb was inert: `StyleText` resolves its target with `as? TextElement`, so the
     * moment a supply could open this popover, every tap would have been a **silent** no-op — the swatch
     * highlights, the snack says the ink was applied, and the mark on the page does not change. Nothing
     * would have thrown and no test that checks the dispatch alone would have gone red.
     *
     * The `when` is exhaustive on purpose. A fourth element kind must come here and declare whether it has
     * an ink, rather than inheriting text's verb by falling through.
     */
    val applyInk: (String, Color) -> Unit = { name, color ->
        val id = uiState.selection.singleOrNull()
        val element = id?.let { selected ->
            uiState.document.pages.getOrNull(uiState.currentPageIndex)
                ?.elements?.firstOrNull { it.id == selected }
        }
        if (element != null) {
            when (element) {
                is TextElement -> dispatch(Intent.StyleText(element.id, color = color.toColorRgba()))
                is DecorElement -> dispatch(Intent.InkSupply(element.id, color.toColorRgba()))
                // A photo's colour is the photocopier's job (`ToggleCopier`), not an ink. The frozen PHOTO
                // verb set carries no `Ink`, so this arm is unreachable today — written out rather than
                // left to an `else` so it stays unreachable *visibly*.
                is ImageElement -> Unit
            }
            deleteJob[0]?.cancel()
            deleteJob[0] = c4Scope.launch {
                snackMessage = Copy.BenchInk.applied(name)
                snackAction = null
                snackVisible = true
                delay(BenchSnackInkMillis)
                snackVisible = false
            }
        }
    }

    val bratioOf = { el: ImageElement -> el.transform.widthPt / el.transform.heightPt }
    val adjustDraft = { d: FramingDraft? -> reframeDraft = d; reframeAdjusted = true }
    // What the open draft can still change ([ReframeAbilities]) — computed once and used for BOTH the
    // stepper pill's enabled state and the verbs below. One truth, so a control painted unavailable is also
    // refused by the hardware keyboard, and no announcement can claim a move that did not happen (the old
    // nudge said "Moved left" whether or not anything moved). `NONE` until the photo's aspect resolves,
    // which is the same inert state M7-01 already required.
    val reframeAbilities = reframing?.let { rf ->
        val d = reframeDraft
        val pr = reframePratio
        if (d != null && pr != null) Framing.abilities(d, pr, bratioOf(rf.before)) else null
    } ?: ReframeAbilities.NONE

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
    // Back stands the panel down instead of leaving the editor — the same rule the page grid and the ink
    // popover already follow, and the same reason: a prototype has no Back, so the freeze cannot specify
    // it, and an overlay you summoned is one Android expects Back to dismiss.
    //
    // This one was measured before it was written, and the omission was worse than an inconsistency. With
    // the panel open on a device, Back fell through to the editor's own handler and returned the user to
    // the shelf — the entire editing context discarded in one press, from a surface opened to change a
    // font size (BETA-UX-REVIEW.md F-15, reproduced twice on SM-A176B / Android 16).
    //
    // It costs most for the people with the fewest alternatives: TalkBack maps swipe-down-then-left to
    // Back, so a screen-reader user performing the one canonical "get me out of here" gesture left the
    // editor, with nothing announced to warn them.
    //
    // Closing the panel must NOT deselect. The selection is what the panel is styling, and a maker who
    // puts the panel away is not done with the element — `styleTarget` is derived from the selection, so
    // clearing the selection here would close the panel twice over and lose the user's place.
    BackHandler(enabled = typeBarOpen) { typeBarOpen = false }

    // ----- C6 (ADR-096) -------------------------------------------------------------------
    // The frozen ink popover is summoned by the `Ink` verb and by nothing else — `.inkpop` has no rest
    // state in the freeze either (`openInk` is its only `add('show')`). Surface-only state, like
    // `typeBarOpen`: the reducer neither knows nor needs to know a popover is open.
    // ⚠ **The id of the element that summoned it, not a boolean.** It was `Boolean`, and a `LaunchedEffect`
    // keyed on the selected id enforced "the popover belongs to the element that summoned it" by clearing
    // the flag whenever the selection changed. That effect and any *caller* that opens the popover while
    // also changing the selection are in a race the caller always loses — the flag is set, the selection
    // change relaunches the effect, and the effect clears it on the next composition.
    //
    // Independent review demonstrated it with a probe: §8's `Change ink` accessibility action selects the
    // supply and then opens the popover, so **on its primary path — TalkBack focus, which is not selection —
    // it opened nothing at all.** The visible `Ink` verb was unaffected only because the element it opens on
    // is already the selection, which is exactly the case where the effect's key does not change.
    //
    // Holding the id makes the rule structural rather than reactive: the popover is visible **iff** the
    // element that summoned it is still the one selected. There is no window in which the two disagree,
    // and no effect to race. This is the same move the package made for the stranded-popover defect —
    // derive the state instead of guarding the flag.
    var inkPopoverFor by remember { mutableStateOf<String?>(null) }

    // ── D-039: who is presenting a capability right now ────────────────────────────────────────────
    //
    // The owner's ruling keeps BOTH bars (OD-11, ADR-029 §6) and forbids showing the same action twice at
    // the same moment, resolving it by **assigning responsibilities**:
    //
    //   the frozen `.ctx` bar  →  the ELEMENT verbs   (Edit · Font · Size · Ink · Delete · Reframe · Replace)
    //   EditorContextBar       →  the TRANSFORM verbs (move ×4 · scale ×2 · rotate ×2 · order ×2)
    //
    // which is the split ADR-029 §6 already justifies: the transform bar exists because *drag* has no
    // single-pointer twin, and move/scale/rotate/order are the verbs that argument covers. `Delete` and
    // `Style` are not — they are element verbs the frozen bar now presents, with a word rather than a
    // glyph, so while it is up those two stand down and the on-canvas Reframe chip stands down with them.
    // Nothing is removed: each yields only while another visible control offers the same capability, and
    // every one of them returns the moment the frozen bar does not (an open session, an open Type bar, a
    // Reframe, a multi-selection, or a kind with no frozen verb set).
    //
    // Hoisted this high because the three sites it governs are in three different scopes: the chip inside
    // the sheet island, the frozen bar inside the canvas, the transform bar below both.
    val ctxElement = uiState.selection.singleOrNull()?.let { id ->
        uiState.document.pages[uiState.currentPageIndex].elements.firstOrNull { it.id == id }
    }
    val ctxKind = ctxElement?.let { benchVerbKindOf(it) }
    // The element `.inkpop` can actually recolour, resolved ONCE for the three sites that need it —
    // the `Ink` verb's routing, the popover's own visibility, and the F-5 clearance term. See
    // [benchInkTargetOf] for why the routing has to consult it rather than opening unconditionally.
    val inkTarget = benchInkTargetOf(ctxElement)
    // ⚠ **The popover is open only if it is also on screen.** Four other controls read this state — the
    // frozen bar stands down for it, `Done` disables, the bar's caption changes, and the edit pan clears
    // it — and every one of them used the raw flag, which is what let one bad `open` produce a screen with
    // no bar, no popover and a disabled `Done`. Deriving it once means a flag that cannot be honoured
    // simply is not honoured anywhere: the stranded state stops being guarded and becomes unconstructible.
    // The guard in `onVerb` below stays as well — belt and braces on a defect the spec has already had to
    // write down once (SUPPLIES-SPEC §10.1, S7).
    val inkPopoverVisible = inkPopoverFor != null && inkPopoverFor == ctxElement?.id && inkTarget != null
    // Back stands it down rather than leaving the editor, exactly as it does for the page grid, and for
    // the same reason: a prototype has no Back, so the freeze cannot specify this, and an overlay you
    // summoned is one Android expects Back to dismiss. Enabled on the *derived* visibility, so Back can
    // never be captured by a popover that is not on screen.
    BackHandler(enabled = inkPopoverVisible) { inkPopoverFor = null }
    // C9 row 9.1: the Bench's four states, derived in one place. The `!EditingText` term spelled out here
    // is subsumed by `benchState == Selected`; `ctxKind != null` stays, because it asks a different
    // question (a single element, of a kind the freeze gives verbs to). So the one behavioural change is
    // the term the freeze writes and this call site did not: `showSheet` removes `.ctx`
    // (`v2-bench.html:847`). Nothing regressed by adding it — the Add chooser is a Dialog, so the bar it
    // now suppresses was already behind another window; the point is that the rule lives in the model
    // rather than being delegated to a platform accident. (`showDelete = !ctxVisible` below therefore
    // offers Delete on the transform bar while the chooser is up — also behind the Dialog. That "behind a
    // Dialog ⇒ unreachable" premise is exactly the merged-semantics assumption ADR-059/CI-26 distrusts,
    // so it is on the device Pass 1 platform-tree checklist rather than settled by this comment.)
    // `addChooserOpen || artSheetOpen`: the Art sheet's frozen caption is a variant of the Adding one, so
    // both sheets are the same Bench state. See `artSheetOpen`'s declaration.
    val benchState = benchStateOf(uiState.selection, uiState.interaction, addChooserOpen || artSheetOpen)
    val ctxVisible = benchState == BenchState.Selected &&
        ctxKind != null &&
        reframing == null &&
        !typeBarOpen &&
        // The freeze's own swap: `openInk` runs `ctx.classList.remove('show')` and `inkClose` restores
        // it (`v2-bench.html:692`, `:697`). Two floating cards share this 12dp inset, so one of them is
        // always the one that is up.
        !inkPopoverVisible

    // The popover belongs to the element that summoned it. Any change of that element — a reselect, a
    // deselect, a page change, a delete — stands it down, which is the freeze doing the same at four
    // separate sites (`:621`, `:628`, `:649`, `:712`).
    //
    // ✅ **The `LaunchedEffect` that used to enforce this is deleted.** `inkPopoverVisible` above compares
    // the summoning id against the live selection every composition, so the rule now holds by construction:
    // a stale popover cannot be visible for even one frame, and — unlike the effect — the rule cannot be
    // lost in a race with a caller that opens the popover and changes the selection in the same act.
    // Applying an ink still leaves it open, because that changes the element and not its id.
    //
    // ⚠ Re-adding that effect is the one-line way to reintroduce the defect, and it was verified as such:
    // with `LaunchedEffect(ctxElement?.id) { inkPopoverFor = null }` restored, exactly one test goes red —
    // `the_change_ink_a11y_action_opens_the_popover_when_the_supply_was_NOT_already_selected` — while the
    // already-selected twin stays green, reproducing the original pass/fail split precisely. **Do not
    // reintroduce it.** The comparison in `inkPopoverVisible` is what enforces this rule now.

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
            sayReframe(Copy.Editor.REFRAMING_PHOTO)
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
            // Speak the outcome (bench: "Framing saved." vs "Framing unchanged.") — literally the same
            // predicate the reducer uses to decide whether a command is recorded, not a second copy of it.
            // This comment used to claim they were "the same comparison" while they were two hand-written
            // `==` tests; they agreed only by being wrong identically, and told a TalkBack user their
            // framing was saved when nothing had been touched (D-097).
            sayReframe(reframeOutcomeLine(after, rf.before))
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
            // Two separate questions, and answering only the first is what let the phantom survive:
            //   · CAN this axis move at all? — that is [reframeAbilities], and it drives the button's
            //     enabled state, which must stay stable per axis rather than flickering off at each edge.
            //   · DID this particular step move anything? — only the resulting draft can say. A photo
            //     already pressed against the clamp has a live axis and a dead step, so the ability alone
            //     would still announce "Moved right" for a photo that did not move.
            // The announcement follows the second question; the paint follows the first.
            val allowed =
                if (dx != 0) reframeAbilities.panHorizontally else reframeAbilities.panVertically
            val nudged = Framing.nudged(d, dx, dy, pr, br)
            // `allowed` is still required: Framing.nudged does not know about fit, so in Whole photo it
            // would happily move a pan the commit then discards.
            if (allowed && nudged != d) {
                adjustDraft(nudged)
                sayReframe(
                    when {
                        dx < 0 -> Copy.Editor.MOVED_LEFT; dx > 0 -> Copy.Editor.MOVED_RIGHT; dy < 0 -> Copy.Editor.MOVED_UP; else -> Copy.Editor.MOVED_DOWN
                    },
                )
            } else {
                sayReframe(if (d.fit == FrameFit.WHOLE) WholePhotoInertLine else Copy.Editor.NO_ROOM_TO_MOVE)
            }
        }
        Unit
    }
    val reframeZoom = { factor: Double ->
        val rf = reframing
        val d = reframeDraft
        val pr = reframePratio
        if (rf != null && d != null && pr != null) {
            val br = bratioOf(rf.before)
            val zoomingIn = factor > 1.0
            if (if (zoomingIn) reframeAbilities.zoomIn else reframeAbilities.zoomOut) {
                val nd = Framing.clampPan(Framing.zoomed(d, factor), pr, br)
                adjustDraft(nd)
                sayReframe(Copy.Reframe.zoomPercentAnnouncement((nd.zoom * 100).roundToInt()))
            } else {
                // Same as the nudge: the button is disabled, the keystroke is not, so it gets a reason
                // instead of a repeated "Zoom 100 percent" that sounds like a stuck control.
                sayReframe(
                    when {
                        d.fit == FrameFit.WHOLE -> WholePhotoInertLine
                        zoomingIn -> Copy.Editor.ALREADY_LARGEST_ZOOM
                        else -> Copy.Editor.ALREADY_SMALLEST_ZOOM
                    },
                )
            }
        }
        Unit
    }
    val reframeSetFit = { f: FrameFit ->
        val d = reframeDraft
        if (d != null && reframePratio != null) {
            adjustDraft(applyFit(d, f))
            sayReframe(
                if (f == FrameFit.FILL) {
                    Copy.Editor.FILLING_THE_FRAME
                } else {
                    Copy.Editor.SHOWING_WHOLE_PHOTO
                },
            )
        }
        Unit
    }
    val reframeReset = {
        if (reframePratio != null) {
            adjustDraft(Framing.DEFAULT_FILL)
            sayReframe(Copy.Editor.FRAMING_RESET)
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
            if (reframeReadable == true) sayReframe(Copy.Editor.REFRAMING_CANCELLED)
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

    // The element under an open text session, or null. Hoisted to the screen root because C3 needs it on
    // both sides of the layout: inside the canvas (to suppress it from the render tape and to place the
    // in-place field on it) and outside it (to seed the style row's ink swatch, and to raise the row at
    // all). Null while a delete races the session closed; every consumer then draws nothing, and the
    // session's own token guard no-ops the trailing commit.
    val editingTextSession = uiState.interaction as? Interaction.EditingText
    val editingElement = editingTextSession?.let { session ->
        uiState.document.pages[uiState.currentPageIndex].elements
            .firstOrNull { it.id == session.id } as? TextElement
    }
    // Text editing is a surface inside the editor, not a navigation destination. Own Back at the screen
    // boundary where it can outrank the NavHost: a handler inside BasicTextField loses that race on real
    // Samsung firmware once the IME has consumed its own Back. The first press may hide the keyboard; the
    // next discards the draft and returns to the selected page instead of leaving for the shelf.
    // CancelText also coalesces a freshly placed blank box away, so opening Text and backing out is clean.
    BackHandler(enabled = editingElement != null) {
        editingTextSession?.let { dispatch(Intent.CancelText(it.id, it.token)) }
    }
    // The frozen editing row's Done ends the session by clearing focus — see its call site for why that,
    // and not a dispatch, is the correct end (the draft is feature-ephemeral).
    val focusManager = LocalFocusManager.current

    // D-043 / OD-16, 2026-08-03: the amended pan is `min(96dp, slack + clearance)`, and the clearance term
    // needs two window-space edges that live on opposite sides of this layout — where the `.kbstack` docks,
    // and where the canvas starts. Both are reported by layout rather than derived from insets and a height
    // constant, because the row's height moves with the font scale and the canvas's top moves with the top
    // bar; a constant would be right on the device it was measured on and quietly wrong everywhere else.
    // `NaN` until the first layout pass, which the consumer reads as "no lift yet" rather than guessing.
    var styleRowDockedTopPx by remember { mutableFloatStateOf(Float.NaN) }
    // F-5: the ink popover docks in the same place, over the same page, and until now only the row's
    // occlusion was paid for. A second edge rather than one shared variable, because the two panels report
    // independently and a single slot would be written by whichever laid out last — including the hidden one.
    var inkPopoverDockedTopPx by remember { mutableFloatStateOf(Float.NaN) }
    var canvasTopPx by remember { mutableFloatStateOf(Float.NaN) }

    // The frozen `.kbstack` is anchored to the PHONE (`left/right/bottom:0; z-index:35`, `:259`) — not to
    // the canvas — so the editing row must overlay the whole screen, above the supply tray, rather than
    // dock at the canvas's bottom edge. It rode the canvas in the first cut of C3 and the screen-level
    // golden caught it floating in the middle of the screen with the tray still showing beneath.
    Box(modifier = modifier) {
    Column(
        modifier = Modifier
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
            }
            // The studio ground and the grain over the whole screen. Last in the chain so it draws beneath
            // the Column's children and the grain lays over all of them, which is what
            // `.phone::after{z-index:90}` does in the frozen file (`v21-bench.html:162`; V2 said 60).
            .benchStudioGround(),
    ) {
        // C4 row 4.9: the frozen `.status` strip, the first thing in the phone (`v2-bench.html:390`
        // sits directly inside `.phone`, above `.canvasArea`). It carries the autosave chip that
        // `EditorSavedConfirmation` used to float over the canvas - one message, one presentation,
        // per OD-14 - and its left slot is deliberately empty; see [BenchStatusStrip].
        BenchStatusStrip(savedVisible = savedVisible && !saveErrorVisible)

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
                    // The one nav action the Bench offers, and it was the last silent control on this
                    // screen. The Library's equivalent — "Make a zine" — has always ticked.
                    onClick = benchTap(action = onPreview),
                    // Room-level nav action, in V2.1's `leafText` — the action colour as text.
                    //
                    // **Converted by P1 although this control is P6's, because P1 broke it.** It was V1
                    // `coral` (#E76F51) as bare text with no fill, which measured 2.77:1 on the old
                    // `paper` ground — already under AA. Moving the ground to `bench` took it to
                    // **2.18:1**, below even the 3:1 large-text floor, and it ships. A package may not
                    // darken the room under a control and leave the control where it fell.
                    //
                    // `leafText` measures 4.88:1 light and 9.38:1 dark on `bench`. It is read from the
                    // ROOM here without an opt-out because this control sits OUTSIDE the island
                    // altogether — it is above the canvas, not over the sheet.
                    colors = ButtonDefaults.textButtonColors(contentColor = ZinelyTheme.v21Colors.leafText),
                    modifier = Modifier
                        .testTag(EditorPreviewActionTestTag)
                        .semantics { contentDescription = Copy.Editor.PREVIEW },
                ) { Text(Copy.Editor.PREVIEW_LABEL) }
            }
        }
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                // D-045, 2026-08-03: the canvas clips to its own bounds, as the frozen
                // `.canvasArea{overflow:hidden}` (`v2-bench.html:171`) has always said and this host never
                // implemented. It cost nothing while the pan was zero and everything once C3 made it real:
                // on device the panned sheet painted straight over the top bar, leaving `Preview` invisible
                // and — read off the platform `AccessibilityNodeInfo` tree, which still gave it its full
                // bounds and `clickable=true` — pressable. A control you cannot see and can press is the
                // defect; the paper over the chrome is only how you notice it.
                //
                // It lands in the same change as the OD-16 clamp on purpose, and the order is not
                // arbitrary: clipping alone would have converted D-043 from a defect you can see into one
                // you cannot (the over-lifted element would be cut off rather than drawn on the chrome),
                // and clamping alone leaves the paper on the top bar whenever clearance genuinely needs
                // the full 96dp. Either one shipped by itself makes the other worse.
                .clipToBounds()
                .onGloballyPositioned { canvasTopPx = it.positionInWindow().y }
                .testTag(EditorCanvasTestTag),
        ) {
            // Fit the whole page into the measured canvas (contain), top-left anchored (pan stays zero for
            // the MVP host; true centring/zoom is a follow-up). The scale is the single px-per-point source.
            val widthPx = constraints.maxWidth.toFloat()
            // **The canvas is not as tall as it measures.** `BenchContextBar` floats over the sheet at this
            // Box's bottom edge, so the height the page may actually occupy is the measured height less the
            // band that bar sits in — see [BenchContextBarReservedHeightDp] for why that band is reserved
            // always rather than only while the bar is up. P2's device Pass 2 (finding U3) found the bar
            // covering the keep-clear boundary's bottom edge and the folio; this is the fix, and it is a
            // *fit* change rather than new chrome, so nothing frozen moves.
            val reservedBottomPx = with(LocalDensity.current) { BenchContextBarReservedHeightDp.toPx() }
            val heightPx = (constraints.maxHeight.toFloat() - reservedBottomPx).coerceAtLeast(0f)
            val scale: Float = remember(widthPx, heightPx, pageSizePt) {
                benchCanvasFitScale(widthPx, heightPx, pageSizePt)
            }
            val interaction = uiState.interaction
            // The SAME predicate the style row is raised on (`editingElement != null`), deliberately, and
            // not the looser `interaction is EditingText`. They differ in exactly one state — a delete
            // racing the session closed leaves the interaction open with no element — and under the looser
            // form the page would stay lifted 96dp while the row slid away beneath it, with nothing on
            // screen to explain either. One predicate, so the two cannot disagree.
            val editing = editingElement != null

            // Push the measured scale into the model so every layer shares it (idempotent — the reducer
            // no-ops an equal view). Deferred until no gesture/edit session is open (Codex RF3): a viewport
            // change re-keys the gesture `pointerInput(screenPxPerPt, …)`, restarting it mid-drag with no
            // cleanup — which would strand a `Transforming` session and a non-null `live`. Re-runs when the
            // interaction returns to Idle, so the latest scale is applied the instant the gesture ends.
            val idle = interaction is Interaction.Idle
            // C1 row 1.3 — `.canvasArea{display:flex;align-items:center;justify-content:center}`. The
            // sheet is centred in the residual height instead of pinned to the top-left as the MVP host
            // left it.
            //
            // Centring is expressed as the **viewport's** `pageOffset`, not as a `contentAlignment` on
            // the canvas Box, and that choice is load-bearing. Every layer over this canvas — the render
            // tape, the snap guides, the selection chrome, the gesture surface, the resize handles, the
            // Reframe overlay and its affordance chip — is `fillMaxSize()` and maps page points through
            // `ExportScale.previewPageToDevice(screenPxPerPt, pageOffset)`, i.e.
            // `devicePx = (pagePt + pageOffset) × scale`. Moving the paper with a layout alignment would
            // have moved the paper *only*, and left every one of those layers drawing at the old origin:
            // the exact divergence the paper-backing comment below was written about. Moving the shared
            // offset moves all of them at once, because there is one seam and they all already read it.
            //
            // Pan is therefore real from here, which retires the "harmless while the MVP host pins pan at
            // zero" caveat below — the paper backing now applies the offset too.
            val centreOffset = remember(widthPx, heightPx, scale, pageSizePt) {
                if (scale <= 0f) {
                    PtPoint(0.0, 0.0)
                } else {
                    PtPoint(
                        x = ((widthPx / scale - pageSizePt.width) / 2.0).coerceAtLeast(0.0),
                        y = ((heightPx / scale - pageSizePt.height) / 2.0).coerceAtLeast(0.0),
                    )
                }
            }
            LaunchedEffect(scale, centreOffset, idle) {
                if (idle) dispatch(Intent.SetViewport(scale, centreOffset))
            }

            // D-035: the canvas stack is the artifact, so it is a light-theme island — the sheet and
            // everything drawn on it keep paper-coloured surroundings at night, while the room around
            // this Box (bar, tray, page strip, context bar) goes on dimming. See [BenchSheetIsland].
            // Read BEFORE the island opens, so the chrome that floats over the sheet can be drawn in the
            // room's palette rather than the sheet's — see the `.ctx` bar below.
            val roomColors = ZinelyTheme.v2Colors
            // The V2.1 half of the same read. P1 made [BenchSheetIsland] provide BOTH palettes, so an
            // opt-out that restored only the V2 one would hand back the room for `v2Colors` and keep the
            // LIT sheet palette for `v21Colors` — chrome drawn half in the room and half on the paper.
            // No live defect when this was written (all three opt-out subtrees still read `v2Colors`),
            // and that is exactly why it is fixed now: P2–P4 convert those subtrees, and the failure
            // would arrive as light ink on a dark bar with every light-palette test passing. D-035's
            // defect, one palette later.
            val roomColors21 = ZinelyTheme.v21Colors
            BenchSheetIsland(modifier = Modifier.fillMaxSize()) {
                // The page footprint reads as paper — the frozen `--paper` sheet (bench.html `.panel`),
                // instead of the bare desk showing through. Purely a host backing UNDER the render: the
                // SceneRenderer contract
                // is untouched (a document background still paints over it; Background.None now shows
                // paper, matching export onto a white sheet). Top-left anchored like the render itself.
                //
                // Sized from `uiState.view.screenPxPerPt` — the SAME viewport every content layer reads —
                // and deliberately NOT from the locally measured `scale`. Those two are not always equal:
                // the push above is deferred while a gesture or text session is open, so a canvas resize
                // mid-session (most obviously the soft keyboard opening under an inline edit) moved the
                // paper immediately and left the render behind, until the session ended. Both now lag
                // together, which is invisible, instead of diverging, which reads as the app losing the
                // page. Same shape as BenchPageNav's thumbs, where one scale drives both the output
                // box and the render and they cannot disagree.
                //
                // The remaining coupling is `pageOffset`: the render translates by it, this backing does
                // not. Harmless while the MVP host pins pan at zero (both read the same zero) — but the
                // day pan becomes real, this offset must be applied here too or the two diverge again.
                val density = LocalDensity.current
                val paperScale = uiState.view.screenPxPerPt
                val paperOffset = uiState.view.pageOffset
                val paperWidth = with(density) { (pageSizePt.width * paperScale).toFloat().toDp() }
                val paperHeight = with(density) { (pageSizePt.height * paperScale).toFloat().toDp() }
                // Both read from `uiState.view`, never from the locally measured `scale`/`centreOffset`:
                // the viewport push is deferred while a gesture or text session is open, so a mid-session
                // canvas resize must move the paper and the render *together*, late, rather than moving
                // one of them immediately. Same lockstep the paperScale comment above describes — now
                // extended to the offset, which centring made load-bearing.
                val paperX = with(density) { (paperOffset.x * paperScale).toFloat().toDp() }
                val paperY = with(density) { (paperOffset.y * paperScale).toFloat().toDp() }
                // C3 (ADR-093 rows 3.1, 3.1a, 3.3): entering a text session pans the WHOLE page as a
                // rigid body — frozen `edit()` sets `pageWrap.style.transform=translateY(-96px)` (`:551`)
                // and `endEdit()` settles it back to `translateY(0)` (`:558`).
                //
                // The wrapper is a `graphicsLayer` translation over the page layers, and that is the
                // whole point of row 3.1: nothing inside re-lays-out, so every element keeps identical
                // page-space bounds and the render, the chrome, the gesture surface and the semantics
                // mirror cannot drift apart mid-pan. A layout offset would have moved them at different
                // times. It wraps the page layers only — the context bar, the Type bar and the room
                // notices are siblings of `.pageWrap` in the freeze too, and do not ride it.
                // D-043 / OD-16, 2026-08-03 — the frozen file was amended first and this transcribes the
                // amendment: −96 is the **maximum** lift, spent as `slack + clearance`. The rule itself is
                // the pure [benchEditPanMagnitudeDp]; everything here is the measurement it consumes,
                // taken from the same `uiState.view` geometry the paper and the render read, so the pan
                // cannot be computed against a viewport the page is not actually drawn at.
                //
                // Rest geometry only: `paperY` is the un-panned band above the sheet and the element's
                // bottom is its page-space bottom, so the target is a function of where things are BEFORE
                // the gesture. It cannot chase the animation it drives.
                //
                // F-5, 2026-08-16: **two panels dock here, and the rule was only ever applied to one.**
                // `BenchInkPopover` replaces the context bar in the same bottom inset and covers the same
                // page — so a maker picking ink could not see the type they were colouring. Nothing about
                // the rule changes; only which panel is the occluder, and which element must clear it. The
                // editing row wins when both could apply.
                //
                // ⚠ That state is **reachable**, and an earlier draft of this comment claimed it was not.
                // `ctxVisible` hides the *bar*, but a session also starts from a tap that re-hits the
                // already-sole-selected element (`EditorGestures.kt`) and from the `Edit text` custom action
                // (`EditorA11y.kt`) — neither goes through the bar. The row is the correct occluder there:
                // it docks below the popover and the typed line is what must clear. Pre-existing to F-5
                // (C6 could produce it too), and flagged for a device look rather than ruled on here.
                val occludingPanelTopPx = if (editing) styleRowDockedTopPx else inkPopoverDockedTopPx
                // The element the panel is about: the one under the session, or — with the popover up —
                // the selected text it is recolouring. This is the SAME `inkTarget` the popover's own call
                // site reads, hoisted beside `ctxElement`; one binding, so they cannot name different
                // elements and the clearance cannot be computed for an element the popover is not about.
                val occludedElement = editingElement
                    ?: inkTarget.takeIf { inkPopoverVisible }
                val panTargetDp: Dp = if (occludedElement == null) {
                    0.dp
                } else {
                    val occluderTopCanvasPx = occludingPanelTopPx - canvasTopPx
                    val edited = occludedElement
                    // Before the first layout pass there is no measurement, so there is no lift. One frame
                    // late is invisible; guessing the literal here would reinstate D-043 for that frame.
                    if (occluderTopCanvasPx.isNaN()) {
                        0.dp
                    } else {
                        val bottomPx =
                            ((edited.transform.yPt + edited.transform.heightPt + paperOffset.y) * paperScale)
                                .toFloat()
                        with(density) {
                            -benchEditPanMagnitudeDp(
                                maxPanDp = -BenchEditPanDp.value,
                                slackAboveDp = paperY.value,
                                elementBottomDp = bottomPx.toDp().value,
                                occluderTopDp = occluderTopCanvasPx.toDp().value,
                            ).dp
                        }
                    }
                }
                val panDp by animateDpAsState(
                    targetValue = panTargetDp,
                    animationSpec = ZinelyTheme.v2Motion.settle(BenchEditPanMillis),
                    label = "bench-edit-pan",
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = panDp.toPx() },
                ) {
                    // C1 (ADR-089 rows 1.5-1.9, 1.11): the bare paper rectangle becomes the frozen sheet —
                    // its hairline edge, radius, two-layer shadow and grain — and carries the two marks the
                    // frozen page carries: the keep-clear boundary and the page number. This box IS the
                    // canonical page geometry per D-033, which is why the furniture is nested inside it
                    // rather than positioned against the canvas.
                    Box(
                        modifier = Modifier
                            .offset(x = paperX, y = paperY)
                            .size(paperWidth, paperHeight)
                            .benchPageSurface()
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
                        // ADR-093 row 3.11: the in-place field below is the only drawing of this element
                        // while the session is open, so the tape must not paint it a second time.
                        hiddenElementId = editingElement?.id,
                        // C4 row 4.13: the leaving element, and how far through leaving it is.
                        deleting = deletingId?.let { it to deleteProgress },
                    )
                    // **Above the wash, and the device is why.** The furniture used to be nested inside the
                    // sheet box above, which put it *under* [EditorPagePreview]'s focus scrim. The freeze
                    // dims `.el:not(.selected)` (`v21-bench.html:207`) — it dims *elements*; Compose
                    // implements the dim as one composite bounded to the page rect ([BenchFocusScrim], and
                    // it stays a composite for the reasons recorded there), so it also washed every mark on
                    // the sheet. That divergence is invisible in a screenshot test, which reads nominal
                    // alphas, and it halved the marks on glass: P2's device pass measured the cue at an
                    // effective **.42** rather than `.85`, and the warning — the one mark ADR-102 §12.9
                    // rests on clearing 3:1 — at **1.82:1** rather than 3.66:1. (`.425` is what the model
                    // predicts; `.42` is what the screen gave. In a comment whose whole subject is correct
                    // arithmetic losing to glass, the two must not be written as the same number — a review
                    // caught this one saying `.425`.)
                    //
                    // The cue's trigger *was* the wash's trigger, so it was never once seen undimmed.
                    // OD-48 has since split the two — the cue fires on a crossing, the wash on a
                    // selection — but this Box must still sit above the wash. The reason changed with
                    // OD-49 and the conclusion did not: it is no longer "the cue only fires during a
                    // gesture, and a gesture implies the wash", because a crossing selection now warns
                    // with no gesture at all. It is that a **crossing requires a selection**, and a
                    // selection is the wash's own trigger. The z-order is what keeps the one accessible
                    // mark at its measured 3.51:1.
                    //
                    // Geometry is unchanged: the same `paperX/paperY/paperWidth/paperHeight` the sheet uses,
                    // so D-033 still holds — the furniture is still positioned by the page box, it is simply
                    // no longer painted beneath the wash.
                    Box(
                        modifier = Modifier
                            .offset(x = paperX, y = paperY)
                            .size(paperWidth, paperHeight),
                    ) {
                        BenchKeepClear(
                            // D-032 as **amended by OD-49**: still transient, but transient with the
                            // *selection* rather than with the gesture. `live`/`resizeOverride` answer while
                            // a gesture runs; `selection` answers every other frame. The paragraph that
                            // stood here — *"the warning cannot outlive the act, which is the whole of the
                            // ruling"* — is the half the device falsified, and a review found it still
                            // quoted as current one line above the argument that discards it.
                            // Nothing is persisted either way: no reducer state backs this.
                            //
                            // The resolve below duplicates the one EditorPagePreview makes this frame.
                            // That is deliberate rather than plumbed: it is a pure call on identical
                            // inputs, so the two agree by construction, and hoisting the result out of a
                            // sibling composable would mean writing state during composition to read it
                            // one frame stale — a worse trade for a few rect operations.
                            warn = BenchStudio.keepClearWarn(
                                page = uiState.document.pages[uiState.currentPageIndex],
                                interaction = uiState.interaction,
                                live = live,
                                resizeOverride = resizeOverride,
                                screenPxPerPt = uiState.view.screenPxPerPt,
                                pageSizePt = pageSizePt,
                                // OD-49: with no gesture in flight this is what the warning answers for —
                                // the selection's committed geometry. An empty set warns about nothing,
                                // which is how an unheld page stays silent.
                                selection = uiState.selection,
                            ),
                            // ⚠ There is no `focusing` argument any more. OD-48 made the crossing the
                            // cue's only trigger, so `keepClearWarn` above is now the whole predicate —
                            // and the selection state the freeze used to reveal on is not consulted here
                            // at all. The wash and the cue no longer answer the same question.
                            panelWidthPt = pageSizePt.width,
                        )
                        BenchPageNumber(
                            pageNumber = uiState.currentPageIndex + 1,
                            pageCount = uiState.document.pages.size,
                            // `.pagenum{right:9px;bottom:6px}` — V2.1 moved the folio to the sheet's
                            // foot. The insets travel with the composable; only the corner is here.
                            modifier = Modifier.align(Alignment.BottomEnd),
                        )
                    }
                    // ADR-093 rows 3.8/3.11: the text is edited ON the page, at its own box, inside the
                    // panned wrapper so it rides the pan rigidly with everything else. This replaces the
                    // detached bottom sheet the editor shipped with — see [BenchEditingSurface].
                    if (interaction is Interaction.EditingText && editingElement != null) {
                        BenchEditingSurface(
                            session = interaction,
                            element = editingElement,
                            dispatch = dispatch,
                            screenPxPerPt = uiState.view.screenPxPerPt,
                            pageOffset = uiState.view.pageOffset,
                            modifier = Modifier.align(Alignment.TopStart),
                            onCoverageChanged = { editCoverage = it },
                        )
                    }
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
                    // `&& !ctxVisible` is D-039: while the frozen bar is up it presents `Reframe` as a labelled
                    // verb, so the chip would be the same offer twice on one screen — the exact pair a
                    // first-time user read as a malfunction in C2b's Pass 2. It returns whenever the bar is not
                    // up, which is every case ADR-053 RF2 built it for except the one the bar now covers.
                    if (reframing == null && !editing && selectedImage != null && !ctxVisible) {
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
                            // C4 row 4.13: TalkBack's Delete takes the same reversible path the visible
                            // verb does — the fade, the snack, and its Undo. Without this the element
                            // simply vanished for a screen-reader user while a sighted user got an undo.
                            onDelete = { softDelete(setOf(it)) },
                            // §8's `Change ink`, taking the SAME path the visible `Ink` verb takes below:
                            // name the element the popover is for. The id comes from the action rather than
                            // from the selection precisely because `EditorA11y` selects the supply in the
                            // same act — under the old boolean, that selection change was what silently
                            // closed the popover this callback had just opened.
                            onChangeInk = { id -> inkPopoverFor = id },
                            // §8's `Replace supply`, taking the same path the visible verb takes: name the
                            // element the cabinet is being opened for.
                            onReplaceSupply = { id -> artSheetFor = BenchArtPurpose.Replace(id) },
                            // OD-49's non-visual half. The drawn warning needs a gesture or a selection;
                            // this needs neither, because a maker reading the page by touch should be able
                            // to discover that a box is off the edge without first selecting it.
                            pageSizePt = pageSizePt,
                        )
                    }
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
                    // Centred on the **paper**, not on the canvas. `Modifier.align(Alignment.Center)`
                    // centres on the canvas Box — but the paper is top-left anchored and, on a portrait
                    // page, narrower than the canvas, so the invitation sat to the right of the sheet and
                    // its lines ran off the screen edge. Copy that straddles the paper's edge reads as the
                    // app having lost track of where the page is, which is why an undo that merely revealed
                    // this was reported as undo corrupting the layout. A paper-sized box also *constrains*
                    // the text, so the lines wrap to the sheet instead of being clipped by the display.
                    //
                    // C1: the sheet is no longer top-left anchored, so a paper-*sized* box at the canvas
                    // origin is no longer a paper-*placed* one. It takes the same `paperX`/`paperY` the
                    // sheet does, from the same viewport offset — which is the point of putting centring
                    // in `pageOffset` rather than in a layout alignment: there is one number to follow,
                    // and following it is what keeps this overlay on the paper it is about.
                    Box(
                        modifier = Modifier
                            .offset(x = paperX, y = paperY)
                            .size(paperWidth, paperHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        EditorEmptyState(
                            firstPage = currentPage.role == PageRole.FRONT_COVER || currentPage.index == 0,
                        )
                    }
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
                            // The hint is composed after the page gesture surface, but that surface is
                            // `fillMaxSize()` over the WHOLE canvas — so wherever the two overlap, the one
                            // control this notice has is competing with a full-screen tap handler whose miss
                            // branch deselects. Measured: an injected tap on `Got it` at a 400dp host does not
                            // reach its handler and the selection is cleared instead, while the same button's
                            // semantics action works and the same tap works at 800dp.
                            .zIndex(1f)
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
                // C4 row 4.10: retired. The autosave reassurance now lives in the frozen `.status`
                // strip at the top of the screen, which is where the freeze puts it. The capability is
                // unchanged - same signal, same 1600ms window - so OD-11 holds; only the presentation
                // moved, and leaving both would have been OD-14's defect.

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

                // The live unsupported-character notice (ADR-070) — the honesty half of the typography
                // work: it names any script the document renderer can't print so nothing reaches paper
                // blank without warning. Shown only while a text session is open (its coverage is live)
                // and only when no save failure is up: a real "couldn't save" outranks it and they share
                // the TopCenter slot, so gating on `!saveErrorVisible` keeps them from ever stacking.
                // Passing Covered when gated collapses the notice via its own AnimatedVisibility.
                EditorCoverageNotice(
                    coverage = if (editing && !saveErrorVisible) editCoverage else TextCoverage.Covered,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp),
                )

                // The frozen contextual verb bar — .ctx (ADR-092). ADDED beside EditorContextBar, never
                // in place of it: OD-11 ruled the frozen bar additive, because the shipped one is the
                // WCAG 2.5.7 single-pointer path (ADR-029 §6) and a parity phase does not remove one.
                //
                // It hides while the Type bar is open, and that is the freeze's own pattern rather than
                // an invention: opening the ink popover runs ctx.classList.remove('show') and closing it
                // restores the bar (v2-bench.html:582, :520). Two floating cards at the same canvas edge
                // would otherwise stack.
                // The bar is composed inside the sheet island for its geometry — it floats at the canvas's
                // bottom edge — but it is NOT part of the sheet, so it must not inherit the sheet's
                // palette. [BenchStudio.sheetIsland] re-declares exactly eight tokens, and `ink` is one of
                // them: drawn under the island the bar took the *light* ink onto its own *room* `--sheet`
                // fill, which in dark theme is dark-on-dark — measured at 1.05:1 on a device, an invisible
                // toolbar that every Robolectric test passed because they all run the light palette. The
                // ruling was already written at the island's own comment above ("the room around this Box
                // — bar, tray, page strip, context bar — goes on dimming", D-035); this restores it.
                //
                // The alignment modifier is built out here because `align` needs the BoxScope receiver,
                // which the provider's lambda does not carry.
                val ctxModifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                CompositionLocalProvider(
                    LocalZinelyV2Colors provides roomColors,
                    LocalZinelyV21Colors provides roomColors21,
                ) {
                BenchContextBar(
                    visible = ctxVisible,
                    // `styleable` is the same test the Style control already applies (ADR-055): a text box
                    // the reducer would refuse to style must not be offered Size or Ink (D-040).
                    verbs = ctxKind?.let {
                        benchContextVerbs(
                            it,
                            // ⚠ The note that stood here said this was *"harmless today — the DECOR verb set
                            // does not consult it — which is exactly the shape S7′ calls silent."* Widening
                            // `inkTarget` to include decor turned that latent shape into a **compile error**,
                            // which is the outcome the S7′ survey predicted for every correct-by-accident
                            // site once decor's Ink was enabled. It is now written out per kind.
                            //
                            // `false`, not `true`, for the kinds that do not consult it: if some future verb
                            // set ever does read `styleable`, the failure should be a control that is *off*
                            // — visible and recoverable — rather than one that is on and lies about what it
                            // can do. Only TEXT reads it today.
                            styleable = when (val selected = ctxElement) {
                                is TextElement -> selected.text.isNotBlank()
                                is DecorElement, is ImageElement, null -> false
                            },
                            // The toggle reads its state from the document, never from local UI state —
                            // so Undo, a page change and a reload all move the announced state with it.
                            copierOn = (ctxElement as? ImageElement)?.copier ?: false,
                        )
                    }.orEmpty(),
                    onVerb = { verb ->
                        val id = ctxElement?.id
                        when (verb.label) {
                            Copy.BenchVerbs.EDIT -> if (id != null) dispatch(Intent.BeginEditText(id))
                            // OD-9 routed Size to the shipped Type bar, and it stays there.
                            Copy.BenchVerbs.SIZE -> typeBarOpen = true
                            // C6 (ADR-096 row 6.1): Ink now opens the frozen `.inkpop`, which is what
                            // the freeze binds it to. Until this package it borrowed the Type bar,
                            // because `.inkpop` was outside C2b's fence and blocked on D-028 — recorded
                            // then as a temporary route, closed now. The Type bar keeps its own ink row:
                            // OD-11 makes the frozen surface additive, and its five inks are the only
                            // place `Coral`, `Teal` and `Blue` remain reachable.
                            // ⚠ Guarded, not unconditional — SUPPLIES-SPEC §10.1's S7 row. `true` here
                            // opened the popover for ANY selected kind, and `.inkpop` is text-only, so a
                            // decor selection produced a state with no bar (`ctxVisible` carries
                            // `!inkPopoverOpen`), no popover, `Done` disabled and the bar's caption already
                            // switched to DONE_AFTER_INK: the verb row vanished with nothing in its place.
                            // The spec calls this out by name and says the fix is the **routing**, not the
                            // verb — so the verb stays disabled and this stops depending on that.
                            // `takeIf` keeps the old guard's meaning — never summon a popover with nothing to
                        // recolour — while naming which element it is for.
                        Copy.BenchVerbs.INK -> inkPopoverFor = ctxElement?.id?.takeIf { inkTarget != null }
                            Copy.BenchVerbs.REFRAME -> if (id != null) dispatch(Intent.BeginReframe(id))
                            // X3b (ADR-106): a toggle, so tapping it again is the undo the user reaches
                            // for first — and Undo is the one they reach for second. Both work.
                            Copy.BenchVerbs.COPIER -> if (id != null) dispatch(Intent.ToggleCopier(id))
                            Copy.BenchVerbs.DELETE -> softDelete(uiState.selection)
                            // §8 `Replace supply` — the Art sheet, re-summoned with a target instead of a
                            // blank page. Guarded on the element actually being decor: the frozen PHOTO row
                            // carries a `Replace` of its own that stays disabled ([D-038], an owner
                            // question), and if that one is ever enabled it must not fall into this arm and
                            // offer a maker paper stamps as replacements for their photograph.
                            Copy.BenchVerbs.REPLACE ->
                                if (ctxElement is DecorElement) artSheetFor = BenchArtPurpose.Replace(ctxElement.id)
                            // Font ships disabled and never arrives here (ADR-092 §1(c)).
                            else -> Unit
                        }
                    },
                    modifier = ctxModifier,
                )

                // C6 rows 6.1-6.14: the frozen `.inkpop` (`v2-bench.html:377-390`, markup `:506`).
                //
                // It sits in the same 12dp inset the bar above it does, because the freeze gives them the
                // same three offsets — and it *replaces* that bar rather than stacking on it, which is
                // `openInk`/`inkClose`'s own behaviour and is why `ctxVisible` carries `!inkPopoverOpen`.
                //
                // Room palette, inside the same provider as the bar: this is chrome over the artifact,
                // not the artifact (D-035). Drawn under the sheet island it would take the island's light
                // `ink` onto the room's `sheet` — the exact 1.05:1 defect C2b measured on a device.
                //
                // ⚠ **No longer text-only.** This read `BenchVerbKind.TEXT` as a literal while decor's `Ink`
                // verb was inert, and `benchInkBands` has carried a working `PHOTO, DECOR ->` arm the whole
                // time — live code on an unreachable path. Passing `ctxKind` is what connects the two; the
                // literal would have silently served a supply the *text* bands, which is a different palette
                // and not the one the frozen decor branch specifies.
                //
                // `?: BenchVerbKind.TEXT` is unreachable rather than a default: `inkPopoverVisible` already
                // requires a non-null `inkTarget`, and an ink target implies a `ctxKind`. It exists because
                // `ctxKind` is nullable at the type level and this is the arm that must not invent a palette.
                BenchInkPopover(
                    visible = inkPopoverVisible,
                    bands = benchInkBands(ZinelyTheme.contentInks, ctxKind ?: BenchVerbKind.TEXT),
                    presets = benchInkPresets(ZinelyTheme.contentInks),
                    // The element's OWN ink, not the last tap: the ring survives undo, a page change and
                    // a reselect, and an ink applied from the Type bar (Coral, Teal, Blue — in no frozen
                    // band) correctly rings nothing rather than ringing something stale. Via
                    // [benchInkColorOf] so a supply's `ink` and a text box's `style.color` reach it through
                    // one binding — the same reason the target itself does.
                    selected = benchInkColorOf(inkTarget)?.toComposeColor(),
                    inkCount = benchInkCount(uiState.document.pages),
                    onPick = { swatch -> applyInk(swatch.name, swatch.value) },
                    // OD-24: the recipe's PRIMARY ink, and the snack says the recipe's name — which is
                    // what the frozen `applyInk(c, PRESETS[i][0])` passes.
                    onPreset = { preset -> applyInk(preset.name, preset.applied.value) },
                    onDone = { inkPopoverFor = null },
                    // F-5: the same clearance term the editing row feeds, from the panel that replaces it.
                    onDockedTopChanged = { inkPopoverDockedTopPx = it },
                    // The frozen stacking order is explicit and this is the only place it can be
                    // expressed: `.ctx` is `z-index:30` (`:357`), `.snack` is `38` (`:444`) and
                    // `.inkpop` is `42` (`:377`) — the popover sits ABOVE the snack. `BenchSnack` is
                    // composed after this call, so without a zIndex the snack drew over the card and
                    // covered the `.inkuse` note; measured on device. Raising the popover rather than
                    // reordering the composition keeps the snack above the verb bar, which is the other
                    // half of the frozen order.
                    modifier = ctxModifier.zIndex(1f),
                )
                }

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

                // C4 rows 4.11-4.12: the frozen `.snack` (`v2-bench.html:361-364`, markup `:443`).
                //
                // **Inside the canvas, not on the screen.** The markup at `:443` sits within
                // `.canvasArea` (`:392`), and `.canvasArea` declares `position:relative` (`:194`) — so the
                // frozen `bottom:12px` resolves against the *canvas*, which ends above the page strip and
                // the bar. The first cut of C4 put this in the screen's root Box and justified it with the
                // claim that `.snack` is positioned against `.phone`; that claim was false, and the test
                // that locked it encoded the wrong geometry as frozen. Independent review caught it by
                // reading the HTML rather than the ADR. Anchored here, the confirmation appears over the
                // artifact it is about, and it still takes no layout height — so it cannot resize the
                // sheet, which is the half of the old reasoning that was true.
                BenchSnack(
                    visible = snackVisible,
                    message = snackMessage,
                    // Null for the ink snack (row 4.15 / C6): the frozen `applyInk` hides the button.
                    actionLabel = snackAction,
                    onAction = {
                        deleteJob[0]?.cancel()
                        snackVisible = false
                        dispatch(Intent.Undo)
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                    // D-089 / frozen Bench A8: placement raises both surfaces. Keep Undo and the newly
                    // selected element's verbs reachable by stacking the snack one complete bar footprint
                    // above the shared floor; its own 12dp inset becomes the required clear gap.
                    bottomClearance = if (ctxVisible) {
                        BenchContextBarReservedHeightDp + BenchSnackStackRotationAllowance
                    } else {
                        0.dp
                    },
                )

            }
        }

        // C5 rows 5.1-5.10: the frozen `.navrow` — the grid button and the filmstrip of little sheets,
        // replacing V1's tilted cards. Every page of the document stays reachable exactly as before; the
        // capability is untouched and only the paint changed (OD-9, D-009).
        //
        // It is emitted HERE, between the canvas and the bar, because that is the frozen stacking order:
        // `v2-bench.html:481` opens `.navrow` and `:488` opens `.bar`, both in `.phone`'s normal flow, so
        // the sheets sit *above* Undo/Redo/Add/Done. C5 first shipped them the other way round and this
        // ADR's own device checklist described the frozen order while the build did the opposite — caught
        // by independent review, not by a test, because nothing asserted the two rows' relative order.
        // `SurfaceTraversalOrderTest` now does.
        //
        // Each thumb mini-renders its page through the SAME render path the canvas uses — which after
        // OD-22 is the specification, not a divergence from it: the frozen `.pthumb i` rules were deleted
        // from `v2-bench.html` because a live miniature is the only way to see another page without
        // going there. Reads pages / current / size / defaults from the same hoisted state and dispatches
        // Intent.GoToPage; the reducer clears selection + returns to Idle on the switch. Threads the
        // host's imageBytes so a thumb's images match the canvas.
        BenchPageNav(
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
            onOpenGrid = { pageGridOpen = true },
            modifier = Modifier.fillMaxWidth(),
            imageBytes = imageBytes,
        )

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
                // The bar paints exactly what the verbs will accept — same value, one source.
                abilities = reframeAbilities,
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
            BenchBottomBar(
                canUndo = uiState.canUndo,
                canRedo = uiState.canRedo,
                // Row 4.8a: withheld for the whole of a text session, because C3's style-row chip
                // owns "finish" while one is open and two visible Dones is OD-14's defect.
                //
                // F-6 extends the SAME rule to the ink popover rather than writing a second one — but on
                // the finding's real cause, not its stated one. ⚠ The device disproves the *visual* claim:
                // the popover's Done is a `--leaf` pill, this bar's is a dark stroked ✓, and the second
                // green pill beside it is `+ Add`. What a dump does show is two nodes named exactly
                // "Done" — `TextView[text=Done]` inside the card and `Button[content-desc=Done]` here —
                // which is OD-14's defect stated in the channel that cannot dress its way out of it.
                // The condition already existed and the popover simply sat outside it — so the fix is the
                // missing term, not a new mechanism. Whatever owns "finish" right now is the only control
                // allowed to say so.
                doneEnabled = editingElement == null && !inkPopoverVisible,
                // F-1's rule reaching the control F-6 just gave a second reason to be dim. Derived from the
                // same two terms above and in the same order, so the reason cannot name a state the button
                // is not in — the text session is checked first because it is the one that also hides the
                // popover's route in.
                doneUnavailableBecause = when {
                    editingElement != null -> Copy.BenchBar.DONE_AFTER_TEXT
                    inkPopoverVisible -> Copy.BenchBar.DONE_AFTER_INK
                    else -> null
                },
                // Undoing from the bar during the delete window takes the snack down with it. Without
                // this the snack kept standing after its own delete had already been reversed, still
                // offering `Undo` — and a second press would have taken back whatever preceded the
                // delete. One reversal, one affordance. (Independent review, C4.)
                onUndo = {
                    deleteJob[0]?.cancel()
                    snackVisible = false
                    dispatch(Intent.Undo)
                },
                onRedo = { dispatch(Intent.Redo) },
                onAdd = { addChooserOpen = true },
                // Row 4.8b: the frozen `deselect()` branch. `SelectAt` with a miss reduces to
                // ClearSelection's exact state - the same one line C2a used for tap-to-dismiss, so
                // there is one deselect path and not two that can drift.
                onDone = { dispatch(Intent.ClearSelection) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // C4 rows 4.4a-4.4d: the frozen Add chooser, all three rows as of ADR-105 S7. A Dialog, so where
        // it is declared does not affect layout.
        BenchAddChooser(
            visible = addChooserOpen,
            onDismiss = { addChooserOpen = false },
            onAddText = { addTextAndEdit(pageSizePt, dispatch) },
            onAddPhoto = { dispatch(Intent.RequestAddImage) },
            onAddArt = { artSheetFor = BenchArtPurpose.Place },
        )

        // The frozen `openArt()` cabinet (ADR-105 S7). `onPick` is only ever called for one of the four
        // authored supplies — an unauthored tile carries no click at all, so `BenchArtSheet` is where
        // "inert stays inert" is enforced, and `placeSupply` is not asked to re-check it.
        BenchArtSheet(
            visible = artSheetOpen,
            onDismiss = { artSheetFor = null },
            onPick = placeSupply,
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
                //
                // D-039 deliberately does NOT touch this one. `Size` and `Ink` on the frozen bar open the
                // same Type bar, so it is tempting to call Style a third door onto one room — but the
                // ruling is about *identical actions presented twice*, and the evidence Pass 2 produced was
                // two controls wearing the same word (`Delete`, `Reframe`). "Text style" is a different
                // offer that happens to share a destination, so standing it down is beyond the minimum the
                // ruling asks for. (An earlier draft of this comment also claimed withholding Style would
                // strand the open panel with no toggle. Review showed that is false — Style would only be
                // withheld while `ctxVisible`, which requires `!typeBarOpen`, i.e. only while the panel is
                // already closed. The sentence is gone: a wrong invariant in a comment is worse than none.)
                onStyle = styleTarget?.let { { typeBarOpen = !typeBarOpen } },
                styleOpen = typeBarOpen,
                // Same rule for Delete, the one verb the two bars have always shared (ADR-092 row 2.13d).
                showDelete = !ctxVisible,
            )
        }
    }
        // Frozen `.kbstack` — `left/right/bottom:0; z-index:35` on the phone (`:259`), so it overlays the
        // supply tray rather than sitting above it in the flow. The row itself carries `imePadding()`, so
        // on a real device it rides directly on top of the system keyboard; the frozen `.kb` beneath it is
        // the prototype's drawn stand-in for that keyboard and is deliberately not ported.
        //
        // Room palette, for the same reason the context bar carries the same provider: this is chrome, not
        // artifact, and the sheet island re-declares `ink` — which once put light ink on a dark room fill
        // and produced an invisible toolbar that every light-palette Robolectric test passed (D-035).
        CompositionLocalProvider(
                    LocalZinelyV2Colors provides ZinelyTheme.v2Colors,
                    LocalZinelyV21Colors provides ZinelyTheme.v21Colors,
                ) {
            BenchStyleRow(
                visible = editingElement != null,
                // Row 3.9: seeded from the element's own computed colour (`v2-bench.html:553`), so the
                // swatch reports the maker's ink rather than a theme default.
                inkSwatch = editingElement?.style?.color
                    ?.let { Color(it.r, it.g, it.b, it.a) }
                    ?: ZinelyTheme.v2Colors.ink,
                // Frozen `#doneEdit → endEdit()` (`:563`). It clears focus rather than dispatching, and
                // that is deliberate: the draft lives inside the session composable (feature-ephemeral by
                // ADR-029 §5.6, and never reaches the store until commit), so the only correct way to end
                // it from outside is the same focus-loss path a tap-away already takes. Dispatching an end
                // would close the session first and the field's dispose-commit would then be rejected by
                // its own token guard — silently dropping the draft. One draft, one commit, one undo entry.
                onDone = { focusManager.clearFocus() },
                // D-043 / OD-16: the pan's clearance term. See [benchEditPanMagnitudeDp].
                onDockedTopChanged = { styleRowDockedTopPx = it },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        // C5 rows 5.11-5.15: the frozen `.pgrid` — **and it is no longer inside the canvas.**
        //
        // ⚠ This is the THIRD reading of where this panel lives, and the first two were both wrong in the
        // same way: they answered from the CSS rule and not from the markup. V2's `.pgrid` was
        // `position:absolute; inset:0` *inside* `.canvasArea`, so C5 correctly covered the canvas only
        // after an earlier cut had made it a full-screen Dialog on the false claim that the freeze said
        // `fixed`. **V2.1 moved the element.** `v21-bench.html:585` declares `.pgrid` as a direct child of
        // `.phone` — `.canvasArea` closes at `:530` — alongside `.scrim` (`:571`), `.sheet` (`:572`) and
        // `.snack` (`:570`), and its rule is now `left:0;right:0;bottom:0` with `translateY(103%)`. It is
        // a bottom sheet, so its bottom edge is the SCREEN's bottom edge; anchored to the canvas it would
        // have risen to a stop mid-screen with the filmstrip and bar still lit beneath it, which reads as
        // a panel that failed to open rather than as a sheet. Verified against the markup nesting rather
        // than the rule, which is what both earlier readings skipped.
        //
        // Emitted after the style row and before nothing: the frozen z-order is `.kbstack` 40, `.scrim`
        // 50, `.pgrid` 54, `.snack` 60, and in Compose paint order is declaration order.
        //
        // Room palette: this is chrome over the artifact, not the artifact (D-035). Read from the theme
        // directly rather than from the canvas's `roomColors`, which is scoped inside the island's host —
        // out here the theme's own palette IS the room's.
        CompositionLocalProvider(
            LocalZinelyV2Colors provides ZinelyTheme.v2Colors,
            LocalZinelyV21Colors provides ZinelyTheme.v21Colors,
        ) {
            BenchPageGrid(
                visible = pageGridOpen,
                pages = uiState.document.pages,
                currentPageIndex = uiState.currentPageIndex,
                // Choosing a page here does what choosing one on the strip does, and then stands the grid
                // down — a picker that stayed open after picking would be a panel, which is what
                // "summoned, never default" refuses.
                onSelectPage = { idx ->
                    if (reframing != null) commitReframe()
                    dispatch(Intent.GoToPage(idx))
                    pageGridOpen = false
                },
                onDismiss = { pageGridOpen = false },
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

/**
 * The line spoken when an adjustment is asked for in "Whole photo", where pan and zoom do nothing.
 *
 * **Wording written here, not sourced from a frozen spec — replaceable.** It exists because gating the
 * verbs (so the keyboard refuses what the disabled buttons refuse) would otherwise make the keystroke
 * *silent*, which is a worse accessibility outcome than the phantom "Moved left" it replaces. It names the
 * way out rather than only the refusal, so a screen-reader user is not left guessing why the pad went
 * quiet. Same for the three zoom-limit lines at their call site.
 */
internal const val WholePhotoInertLine: String = Copy.Editor.WHOLE_PHOTO_INERT

private fun applyFit(draft: FramingDraft, fit: FrameFit): FramingDraft = when (fit) {
    FrameFit.WHOLE -> draft.copy(fit = FrameFit.WHOLE, zoom = Framing.MIN_ZOOM, panX = 0.0, panY = 0.0)
    FrameFit.FILL -> draft.copy(fit = FrameFit.FILL)
}

/**
 * "Add words" from the empty state: place an **empty** text box centered on the page and **open its
 * edit session immediately**, so the beginner goes straight to typing — no committed placeholder
 * sentence, and no reliance on the hidden double-tap-to-edit affordance (Codex UX finding). Composed
 * The reducer owns the whole act through [Intent.PlaceTextAndEdit]: it mints the id, places the box and
 * opens the matching session in one reduction. Keeping those steps atomic matters when another element
 * is already selected — no UI-side state read can accidentally reopen that older selection.
 */
internal fun addTextAndEdit(
    pageSizePt: PtSize,
    dispatch: (Intent) -> Unit,
) {
    dispatch(Intent.PlaceTextAndEdit(centeredTextBox(pageSizePt)))
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
