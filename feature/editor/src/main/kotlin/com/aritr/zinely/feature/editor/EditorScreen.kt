package com.aritr.zinely.feature.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aritr.zinely.core.editor.EditorUiState
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.editor.LiveTransform
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.render.android.AssetBytesSource
import kotlin.math.min

/** Test tag on the editor canvas Box (the measured, gesture-bearing area). */
public const val EditorCanvasTestTag: String = "editor-canvas"

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
 */
@Composable
public fun EditorScreen(
    store: EditorStore,
    pageSizePt: PtSize,
    modifier: Modifier = Modifier,
    imageBytes: AssetBytesSource = EmptyAssetBytes,
    moveResizeHintSeen: Boolean? = false,
    onMoveResizeHintSeen: () -> Unit = {},
) {
    val uiState by store.uiState.collectAsStateWithLifecycle()
    val dispatch: (Intent) -> Unit = store::dispatch
    val currentState = { store.uiState.value }

    // Feature-ephemeral gesture accumulators — the live pan/pinch frame and the handle-resize override.
    // They never reach the reducer; only the baked CommitTransform does (§5.1). Handle drags consume their
    // pointers, so at most one of the two is non-null at a time; the preview prioritises resizeOverride.
    var live by remember { mutableStateOf<LiveTransform?>(null) }
    var resizeOverride by remember { mutableStateOf<Map<String, Transform>?>(null) }

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

    Column(modifier = modifier) {
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
                EditorPagePreview(
                    uiState = uiState,
                    defaults = uiState.document.defaults,
                    pageSizePt = pageSizePt,
                    live = live,
                    modifier = Modifier.fillMaxSize(),
                    resizeOverride = resizeOverride,
                    imageBytes = imageBytes,
                )
                // The page gesture surface and resize handles are inert while a text session is open (Codex
                // RF1): otherwise a stray long-press/double-tap replaces `EditingText`, and the session's
                // onDispose commits a now-stale token → the draft is silently dropped. The handles also yield
                // to an in-flight page drag (`live != null`) so the two never run concurrent sessions (RF2).
                if (!editing) {
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
                                onDoubleTap = { pagePoint -> dispatch(Intent.BeginEditTextAt(pagePoint)) },
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
                ElementSemanticsLayer(
                    uiState = uiState,
                    dispatch = dispatch,
                    modifier = Modifier.fillMaxSize(),
                )

                // First-run invitation: when the current page is blank and no text session is open, a
                // blank sheet reads as a void — so we overlay the cozy empty state (DESIGN-LANGUAGE §8/§9).
                // It is **invitation-only** ([ADR-033](../DECISIONS.md#adr-033)): the add actions live
                // solely in the persistent supply tray below, so "Add a photo" / "Add words" never appear
                // twice at once. The overlay just invites + points to the shelf; it disappears the instant
                // the page gets an element. Non-interactive, so touches fall through to the gesture surface.
                val currentPageEmpty = uiState.document.pages[uiState.currentPageIndex].elements.isEmpty()
                if (currentPageEmpty && !editing) {
                    EditorEmptyState(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                // One-time move/resize hint: the moment a placed element is single-selected (handles up,
                // not editing) we float in the gentle "drag to move · pinch to resize" note — those two
                // gestures have no discrete-control twin, so a beginner can miss them. It is non-blocking
                // (declares no pointerInput; touches fall through to the gesture surface) and one-time per
                // screen. Sits below the edit overlay so a text session always wins the top of the canvas.
                // Also gate on no in-flight gesture so the hint is gone the same frame a drag begins
                // (the LaunchedEffect makes that dismissal stick); avoids a one-frame overlap.
                val gesturing = live != null || resizeOverride != null
                val showMoveResizeHint =
                    !editing && !gesturing && uiState.selection.size == 1 &&
                        !moveResizeHintDismissed && moveResizeHintSeen == false
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
                            color = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
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
        EditorSupplyTray(
            canUndo = uiState.canUndo,
            canRedo = uiState.canRedo,
            onAddPhoto = { dispatch(Intent.RequestAddImage) },
            onAddText = { addTextAndEdit(pageSizePt, currentState, dispatch) },
            onUndo = { dispatch(Intent.Undo) },
            onRedo = { dispatch(Intent.Redo) },
            modifier = Modifier.fillMaxWidth(),
        )

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
            onSelectPage = { dispatch(Intent.GoToPage(it)) },
            modifier = Modifier.fillMaxWidth(),
            imageBytes = imageBytes,
        )

        EditorContextBar(
            selection = uiState.selection,
            dispatch = dispatch,
            modifier = Modifier.fillMaxWidth(),
        )
    }
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
