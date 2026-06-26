package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.editor.EditorUiState
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.ResizeHandle
import com.aritr.zinely.core.editor.TransformMath
import com.aritr.zinely.core.editor.resizeByHandle
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.render.android.ExportScale
import com.aritr.zinely.render.android.SelectionChromeGeometry
import kotlin.math.roundToInt

/** Stable test-tag prefix; a handle's node tag is `"$ResizeHandleTagPrefix${handle.name}"`. */
public const val ResizeHandleTagPrefix: String = "resize-handle-"

/**
 * The S4 resize handles (ADR-029 §5.2/§5.3): eight draggable handles on the selected element's box.
 * Each handle holds its **opposite** handle fixed in page space ([ResizeHandle.opposite]) and resizes via
 * [TransformMath.resizeByHandle] — corners both axes, edges one. Rotation is unchanged by a resize.
 *
 * Like [editorTransformGestures] this opens a reducer transform session on first drag (`BeginTransform`,
 * token read synchronously), drives [onResize] with the directly-baked override each frame (fed to
 * [EditorPagePreview]'s `resizeOverride`), and commits one [Intent.CommitTransform] on release — one drag
 * = one undo step (R5.3). A handle's drag is self-contained and **consumes** its pointers, so it never
 * races the page pan/pinch layer beneath it.
 *
 * **A11y (WCAG 2.5.8 ≥48dp):** each handle's hit area is a 48dp box; the visual dot is smaller. The
 * dragging alternative (2.5.7) is the a11y contextbar's `ScaleBy` stepper — a later increment. MVP selects
 * one element; nothing is drawn when the selection is empty or not a single element.
 *
 * @param currentState reads the live [EditorUiState] (token after `BeginTransform`, `before` at commit).
 * @param dispatch forwards an [Intent] into the store.
 * @param onResize receives the live override map each frame (drives `resizeOverride`), `null` on end.
 * @param modifier sized identically to the sibling [PagePreview]/[SelectionChrome] so handle device-px
 *   positions align.
 * @param color handle fill; defaults to the theme `primary`.
 */
@Composable
public fun ResizeHandles(
    uiState: EditorUiState,
    currentState: () -> EditorUiState,
    dispatch: (Intent) -> Unit,
    onResize: (Map<String, Transform>?) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val selectedId = uiState.selection.singleOrNull() ?: return
    val element = uiState.document.pages[uiState.currentPageIndex].elements.firstOrNull { it.id == selectedId } ?: return
    val transform = element.transform
    val screenPxPerPt = uiState.view.screenPxPerPt.toDouble()
    val pageOffset = uiState.view.pageOffset
    val density = LocalDensity.current
    val hitSizePx = with(density) { 48.dp.toPx() }

    Box(modifier = modifier) {
        for (handle in ResizeHandle.entries) {
            val center = SelectionChromeGeometry.handleDevicePx(transform, handle.local, screenPxPerPt, pageOffset)
            HandleTarget(
                handle = handle,
                centerPx = center,
                hitSizePx = hitSizePx,
                color = color,
                onDrag = { dragDevicePx, activeToken ->
                    // Only bake/preview if OUR session is still the live one (Codex rec): a concurrent
                    // interaction that replaced ours has a different token — skip, so no stale preview
                    // frame shows, and the reducer rejects the eventual commit anyway.
                    val itx = currentState().interaction
                    if (itx is Interaction.Transforming && itx.token == activeToken) {
                        val before = itx.before[selectedId] ?: return@HandleTarget null
                        val dragPagePt = ExportScale.previewDeviceToPage(
                            screenPxPerPt, pageOffset, PtPoint(dragDevicePx.x.toDouble(), dragDevicePx.y.toDouble()),
                        )
                        val after = TransformMath.resizeByHandle(before, handle, dragPagePt)
                        onResize(mapOf(selectedId to after))
                        after
                    } else {
                        null
                    }
                },
                beginSession = {
                    dispatch(Intent.BeginTransform(setOf(selectedId)))
                    (currentState().interaction as? Interaction.Transforming)?.token
                },
                commitSession = { after, token ->
                    dispatch(Intent.CommitTransform(mapOf(selectedId to after), token))
                    onResize(null)
                },
                cancelSession = { token ->
                    dispatch(Intent.CancelTransform(token))
                    onResize(null)
                },
            )
        }
    }
}

/**
 * One 48dp handle hit-target placed (centred) on [centerPx], drawing a small dot. Owns the per-handle drag
 * loop: [beginSession] on first drag → token; [onDrag] each move with the handle's accumulated device-px
 * position → the live-baked transform; [commitSession]/[cancelSession] on end/cancel.
 */
@Composable
private fun HandleTarget(
    handle: ResizeHandle,
    centerPx: PtPoint,
    hitSizePx: Float,
    color: Color,
    onDrag: (Offset, Long) -> Transform?,
    beginSession: () -> Long?,
    commitSession: (Transform, Long) -> Unit,
    cancelSession: (Long) -> Unit,
) {
    val half = hitSizePx / 2f
    Box(
        modifier = Modifier
            // Place the hit box so its centre lands on the handle point (top-left = centre − half).
            .offset {
                IntOffset(
                    x = (centerPx.x.toFloat() - half).roundToInt(),
                    y = (centerPx.y.toFloat() - half).roundToInt(),
                )
            }
            .size(48.dp)
            .testTag("$ResizeHandleTagPrefix${handle.name}")
            .pointerInput(handle, centerPx) {
                var token: Long? = null
                var cur = Offset(centerPx.x.toFloat(), centerPx.y.toFloat())
                var last: Transform? = null
                detectDragGestures(
                    onDragStart = {
                        cur = Offset(centerPx.x.toFloat(), centerPx.y.toFloat())
                        last = null
                        token = beginSession()
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        val t = token
                        if (t != null) {
                            cur += amount
                            last = onDrag(cur, t)
                        }
                    },
                    onDragEnd = {
                        val t = token
                        val after = last
                        if (t != null && after != null) commitSession(after, t) else if (t != null) cancelSession(t)
                        token = null
                    },
                    onDragCancel = {
                        val t = token
                        if (t != null) cancelSession(t)
                        token = null
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(12.dp)
                .background(color, CircleShape),
        )
    }
}
