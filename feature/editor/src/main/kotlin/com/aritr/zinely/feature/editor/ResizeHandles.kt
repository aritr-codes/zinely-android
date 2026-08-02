package com.aritr.zinely.feature.editor

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.aritr.zinely.ui.theme.ZinelyTheme
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
 * **C2a re-skin (ADR-091 rows 2.5–2.6a):** the frozen `.handle` is a **13px circle**, `--paper` filled,
 * `border:2px solid var(--matcha)`, haloed by `box-shadow:0 0 0 1.5px rgba(255,255,255,.7)`
 * (v2-bench.html `:157-158`). It replaced a 15dp `--coral-strong`-bordered rounded *square* transcribed
 * from the V1 bench. The halo is not decoration: `--matcha` on `--paper` is guaranteed only against the
 * sheet, and a handle sits **on the user's photo**, where no token can be guaranteed. The white ring is
 * [V2-BENCH-IA-INTERACTION.md §C.4](../../../../../../../../docs/design/V2-BENCH-IA-INTERACTION.md)'s
 * dual-tone, and it is what keeps the mark visible over a dark image.
 *
 * **The freeze draws four handles (`tl/tr/bl/br`); this draws eight, and that is deliberate.** The four
 * edge handles resize a **single axis** — capability the corners cannot express. Deleting them to match
 * the CSS would remove a shipped editor capability during a parity phase, which owner ruling **OD-11**
 * forbids; the frozen vocabulary is additive. See [ADR-091 §1(b)](../../../../../../../../docs/DECISIONS.md#adr-091).
 *
 * @param color the handle's border colour; defaults to the frozen `--matcha` token. Read through
 *   [ZinelyTheme.v2Colors] so that inside the sheet island it is the on-paper value in both themes.
 */
@Composable
public fun ResizeHandles(
    uiState: EditorUiState,
    currentState: () -> EditorUiState,
    dispatch: (Intent) -> Unit,
    onResize: (Map<String, Transform>?) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = ZinelyTheme.v2Colors.matcha,
) {
    // The frozen `.handle{transition:opacity .12s}` with `.el.selected .handle{opacity:1}` (v2-bench.html
    // `:156-157`). Like the outline, fading out needs the geometry to outlive the selection, so the last
    // selected box is retained until the fade completes.
    val live = uiState.selection.singleOrNull()?.let { id ->
        uiState.document.pages[uiState.currentPageIndex].elements.firstOrNull { it.id == id }?.let { id to it.transform }
    }
    var last by remember { mutableStateOf(live) }
    if (live != null) last = live
    val alpha by animateFloatAsState(
        targetValue = if (live != null) 1f else 0f,
        animationSpec = tween(if (ZinelyTheme.motion.reduceMotion) 0 else BenchChromeFadeMillis),
        label = "bench-handle-fade",
    )
    if (alpha <= 0f) return
    val (selectedId, transform) = live ?: last ?: return
    // A handle on its way out is a picture, not a control: it must not accept a drag for a selection that
    // no longer exists. `live == null` is exactly that window.
    val interactive = live != null
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
                alpha = alpha,
                interactive = interactive,
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
 * One 48dp handle hit-target placed (centred) on [centerPx], drawing the frozen handle mark — a 13dp
 * paper-filled **circle** with a 2dp [color] border and a 1.5dp white halo (v2-bench.html `:157-158`).
 * Owns the per-handle drag loop: [beginSession] on first drag → token; [onDrag] each move with the handle's
 * accumulated device-px position → the live-baked transform; [commitSession]/[cancelSession] on end/cancel.
 */
@Composable
private fun HandleTarget(
    handle: ResizeHandle,
    centerPx: PtPoint,
    hitSizePx: Float,
    color: Color,
    alpha: Float,
    interactive: Boolean,
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
            .alpha(alpha)
            .then(if (!interactive) Modifier else Modifier.pointerInput(handle, centerPx) {
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
            },),
        contentAlignment = Alignment.Center,
    ) {
        BenchHandleMark(color = color)
    }
}

/**
 * The frozen handle mark (v2-bench.html `:157-158`, ADR-091 row 2.5): a 13px `--paper`-filled **circle**
 * with a 2px [color] border, haloed by `box-shadow:0 0 0 1.5px rgba(255,255,255,.7)`.
 *
 * The freeze sets `*{box-sizing:border-box}` (`:107`), so 13px is the **outer** diameter and the border
 * eats into it; the halo is a 1.5px ring *outside* that, hence the 16dp envelope. The halo is not
 * decoration — it is the layer that keeps the mark visible on a dark photo, where `--matcha` alone cannot
 * be guaranteed 3:1 ([IA §C.4](../../../../../../../../docs/design/V2-BENCH-IA-INTERACTION.md)).
 *
 * Extracted from the hit target so the appearance can be composed and measured on its own, without a
 * drag loop, an `EditorUiState` or a selection.
 */
@Composable
internal fun BenchHandleMark(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(HandleHaloDiameterDp)
            .background(HandleHaloColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(HandleDiameterDp)
                .background(ZinelyTheme.v2Colors.paper, CircleShape)
                .border(HandleBorderDp, color, CircleShape),
        )
    }
}

/** Frozen `.handle` `width/height:13px` — outer diameter, `box-sizing:border-box` (v2-bench.html `:157`). */
internal val HandleDiameterDp = 13.dp

/** Frozen `.handle` `border:2px solid var(--matcha)` (v2-bench.html `:157`). */
internal val HandleBorderDp = 2.dp

/** Frozen `.handle` halo `0 0 0 1.5px` — a ring outside the 13px mark, so 13 + 1.5 × 2 = 16dp. */
internal val HandleHaloDiameterDp = 16.dp

/** Frozen `.handle` halo colour `rgba(255,255,255,.7)` (v2-bench.html `:158`). */
internal val HandleHaloColor: Color = Color.White.copy(alpha = 0.7f)
