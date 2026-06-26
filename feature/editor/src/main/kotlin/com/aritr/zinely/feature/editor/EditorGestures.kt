package com.aritr.zinely.feature.editor

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.PI
import kotlin.math.abs
import com.aritr.zinely.core.editor.EditorUiState
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.editor.LiveTransform
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.render.android.ExportScale

/**
 * The S4 editor gesture layer (ADR-029 §5). Translates raw Compose pointer input over the page into the
 * **same** MVI intents the a11y contextbar uses, plus the off-reducer live-preview accumulator
 * ([LiveTransform]) that drives the selection chrome's `graphicsLayer{}` during a drag/pinch/rotate.
 *
 * Two cooperating detectors on the page surface:
 *  - **Tap layer** — `detectTapGestures`: **long-press** → [Intent.SelectAt] (hit-test runs in the pure
 *    reducer), **double-tap** → [onDoubleTap] (the text-edit-session seam; its begin-edit intent lands in
 *    the text-session step).
 *  - **Transform layer** — a hand-rolled begin/update/commit loop (the internals of
 *    `detectTransformGestures`, opened up so the gesture *end* is observable). The first real
 *    pan/zoom/rotation frame dispatches [Intent.BeginTransform] and reads the session **token**
 *    synchronously (the [EditorStore] mailbox guarantees the reduction completed before `dispatch`
 *    returns — the §5.1 contract). Subsequent frames fold into one [LiveTransform] pushed to [onPreview];
 *    on lift, the accumulator **bakes** each member's snapshot `before` (§5.2) into [Intent.CommitTransform]
 *    — exactly one undo step ([R5.3]).
 *
 * **No geometry of its own:** px↔page conversion is [ExportScale.previewDeviceToPage] (the inverse of the
 * preview seam [PagePreview] renders with), and the bake is [LiveTransform.bake] — both proven pure. Group
 * selection bakes each member about its own centre (MVP selects one; the true group-bbox-centre transform
 * of §5.5 is the additive multi-select extension).
 *
 * @param screenPxPerPt device px per point at the current preview scale; must match [PagePreview].
 * @param pageOffset page-space pan applied before the screen scale; must match [PagePreview].
 * @param currentState reads the live [EditorUiState] snapshot (e.g. `{ store.uiState.value }`) — used to
 *   read the session token after [Intent.BeginTransform] and the snapshot `before` map at commit.
 * @param dispatch forwards an [Intent] into the store.
 * @param onPreview receives the live accumulator each frame (drives the chrome `graphicsLayer{}`), and
 *   `null` when a gesture ends so the preview resets.
 * @param onDoubleTap the page-space point of a double-tap (text-edit seam).
 */
public fun Modifier.editorTransformGestures(
    screenPxPerPt: Float,
    pageOffset: PtPoint,
    currentState: () -> EditorUiState,
    dispatch: (Intent) -> Unit,
    onPreview: (LiveTransform?) -> Unit,
    onDoubleTap: (PtPoint) -> Unit = {},
): Modifier {
    fun toPage(pos: Offset): PtPoint = ExportScale.previewDeviceToPage(
        screenPxPerPt = screenPxPerPt.toDouble(),
        pageOffset = pageOffset,
        devicePx = PtPoint(pos.x.toDouble(), pos.y.toDouble()),
    )

    return this
        .pointerInput(screenPxPerPt, pageOffset) {
            detectTapGestures(
                onLongPress = { pos -> dispatch(Intent.SelectAt(toPage(pos))) },
                onDoubleTap = { pos -> onDoubleTap(toPage(pos)) },
            )
        }
        .pointerInput(screenPxPerPt, pageOffset) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                // Target the selection as it stood when the finger landed; the only way to reach a transform
                // is a drag that crosses slop BEFORE long-press fires, so this also equals the slop-cross
                // selection — capturing it here just makes the "what am I moving" contract explicit (Codex Q3).
                val selectionAtDown = currentState().selection
                var live = LiveTransform()
                var token: Long? = null
                var canceled = false
                var pastSlop = false
                var firstEvent = true
                val touchSlop = viewConfiguration.touchSlop
                // Pre-slop motion is accumulated CUMULATIVELY across frames (matching detectTransformGestures):
                // an interpolated swipe arrives as many sub-slop steps, so a per-frame test would never cross.
                var slopZoom = 1f
                var slopRotation = 0f
                var slopPan = Offset.Zero

                do {
                    val event: PointerEvent = awaitPointerEvent()
                    // Ownership coordination with the cooperating tap layer (Codex fix): the tap layer consumes
                    // the *first down* while it waits out the long-press/double-tap window — that one is ignored.
                    // Any LATER external consumption means the tap layer claimed the gesture (a long-press fired)
                    // or some ancestor took over: abandon a not-yet-started transform, cancel a live one.
                    if (!firstEvent && event.changes.any { it.isConsumed }) {
                        if (token != null) canceled = true
                        break
                    }
                    firstEvent = false

                    val zoom = event.calculateZoom()
                    val rotation = event.calculateRotation()
                    val pan = event.calculatePan()

                    // Touch-slop startup gate (the real detectTransformGestures threshold): a motionless
                    // long-press never crosses it, so it stays a tap for the tap layer — and sub-slop jitter
                    // never opens a spurious one-tap-is-one-undo-step session (R5.3).
                    if (!pastSlop) {
                        slopZoom *= zoom
                        slopRotation += rotation
                        slopPan += pan
                        val centroidSize = event.calculateCentroidSize(useCurrent = false)
                        val zoomMotion = abs(1f - slopZoom) * centroidSize
                        val rotationMotion = abs(slopRotation * (PI / 180.0).toFloat() * centroidSize)
                        val panMotion = slopPan.getDistance()
                        if (zoomMotion > touchSlop || rotationMotion > touchSlop || panMotion > touchSlop) {
                            pastSlop = true
                        }
                    }

                    if (pastSlop) {
                        if (token == null) {
                            // Open the session and read the token synchronously (the §5.1 mailbox contract).
                            dispatch(Intent.BeginTransform(selectionAtDown))
                            val itx = currentState().interaction
                            if (itx !is Interaction.Transforming) break // nothing selected/transformable
                            token = itx.token
                        }
                        live = live.accumulate(
                            panXpx = pan.x.toDouble(),
                            panYpx = pan.y.toDouble(),
                            zoomFactor = zoom.toDouble(),
                            rotationDelta = rotation.toDouble(),
                        )
                        onPreview(live)
                        event.changes.forEach { c: PointerInputChange -> if (c.positionChanged()) c.consume() }
                    }
                } while (event.changes.any { it.pressed })

                if (token != null) {
                    val tx = currentState().interaction
                    val mine = tx is Interaction.Transforming && tx.token == token
                    when {
                        mine && !canceled -> {
                            val before = (tx as Interaction.Transforming).before
                            val after = before.mapValues { (_, t) -> live.bake(t, screenPxPerPt.toDouble()) }
                            dispatch(Intent.CommitTransform(after, token))
                        }
                        // Our session is still open but the pointer was canceled mid-gesture — discard it.
                        mine -> dispatch(Intent.CancelTransform)
                        // else: a newer gesture/selection replaced our session — leave it untouched.
                    }
                    onPreview(null)
                }
            }
        }
}
