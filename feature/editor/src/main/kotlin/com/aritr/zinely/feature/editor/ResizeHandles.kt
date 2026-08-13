package com.aritr.zinely.feature.editor

import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * **V2.1 re-skin (ADR-102 P1):** the frozen `.hnd` is a **9px rounded square** (`border-radius:2px`),
 * `--paper` filled, `border:1.6px solid var(--ink)` (`v21-bench.html:198-199`). It replaced V2's 13dp
 * `--matcha`-bordered circle, which had itself replaced a 15dp `--coral-strong` rounded square from V1.
 *
 * ### The halo is kept, and the freeze does not have one
 *
 * `v21-bench.html` specifies no `box-shadow` on `.hnd`. **It is kept anyway**, ruled as a deliberate
 * departure at [ADR-102 §12.6 row 5](../../../../../../../../docs/DECISIONS.md#adr-102-p1-corrections).
 *
 * The reason is the one thing about this mark that no palette can fix: a ring and a page number sit on
 * paper, where `ink`-on-`paper` is measured and guaranteed — but **a handle sits on the user's
 * photograph**, and there is no token that can promise a ratio against an image the product has never
 * seen. [V2-BENCH-IA-INTERACTION.md §C.4](../../../../../../../../docs/design/V2-BENCH-IA-INTERACTION.md)
 * requires handles to *"hold **3:1** over any user photo"* through a dual-tone stroke, and the white ring
 * is the tone that does it. Dropping it to match the CSS would trade a measured accessibility guarantee
 * for fidelity, and the owner's standing priority order puts accessibility **above** fidelity to the
 * freeze — which is the same order that made the freeze *"strong input and constraints, but not sacred."*
 *
 * This is the same family as the handle **count** below: the freeze draws four, this draws eight.
 *
 * **The freeze draws four handles (`tl/tr/bl/br`); this draws eight, and that is deliberate.** The four
 * edge handles resize a **single axis** — capability the corners cannot express. Deleting them to match
 * the CSS would remove a shipped editor capability during a parity phase, which owner ruling **OD-11**
 * forbids; the frozen vocabulary is additive. See [ADR-091 §1(b)](../../../../../../../../docs/DECISIONS.md#adr-091).
 *
 * @param color the handle's border colour; defaults to the frozen `--ink` token. Read through
 *   [ZinelyTheme.v21Colors] so that inside the sheet island it is the on-paper value in both themes.
 */
/**
 * Where a handle's **drawn mark** sits relative to its hit target: [ringOffsetPx] outward on each axis the
 * handle touches, turned with the element.
 *
 * Extracted as a pure function for one reason — **the composable had no way to assert that the mark moved
 * at all.** After the 2026-08-12 ruling ([ADR-102 §12.8](../../../../../../../../docs/DECISIONS.md#adr-102-p1-handles))
 * the suite proved the CSS declares 5.5px, that the constant equals the derived CSS value, and that the hit
 * target does *not* move — and then set `HandleRingOffsetDp` to zero would still have passed every one of
 * them, with only the goldens objecting, in the same change that re-recorded the goldens. A review caught
 * that the guard had been built on the wrong side of the change. This is the right side.
 *
 * [ResizeHandle.local] is the unit-frame corner/edge — `(±1,±1)` for corners, `(±1,0)`/`(0,±1)` for edges —
 * so its sign is the outward direction and its **zero** is what makes an edge handle stay centred on its
 * edge. That reproduces the frozen rules exactly: `.hnd.tl{left:-10px;top:-10px}` on a 9px `border-box`
 * mark centres at `-10 + 9/2 = -5.5` per axis, and `.hnd.t{left:calc(50% - 4.5px);top:-10px}` is 5.5 out on
 * the bound axis and centred on the free one (`v21-bench.html:201-206`).
 *
 * Note the corner figure is **5.5 per axis**, i.e. 7.78 along the diagonal — prose calling it "5.5 out along
 * the diagonal" is loose, and the CSS is per-axis.
 */
internal fun handleMarkOffsetPx(handle: ResizeHandle, ringOffsetPx: Float, rotationDegrees: Double): Offset {
    val ox = handle.local.x.toFloat() * ringOffsetPx
    val oy = handle.local.y.toFloat() * ringOffsetPx
    // `handleDevicePx` already rotates the POINT; this rotates the offset from it, with the same matrix
    // (`AffineTransform2D.rotateDeg`: x·cos − y·sin, x·sin + y·cos) so the marks stay on the turned ring.
    val r = Math.toRadians(rotationDegrees)
    val c = cos(r).toFloat()
    val s = sin(r).toFloat()
    return Offset(ox * c - oy * s, ox * s + oy * c)
}

@Composable
public fun ResizeHandles(
    uiState: EditorUiState,
    currentState: () -> EditorUiState,
    dispatch: (Intent) -> Unit,
    onResize: (Map<String, Transform>?) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = ZinelyTheme.v21Colors.ink,
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
    val ringOffsetPx = with(density) { HandleRingOffsetDp.toPx() }

    Box(modifier = modifier) {
        for (handle in ResizeHandle.entries) {
            val center = SelectionChromeGeometry.handleDevicePx(transform, handle.local, screenPxPerPt, pageOffset)
            HandleTarget(
                handle = handle,
                centerPx = center,
                markOffsetPx = handleMarkOffsetPx(handle, ringOffsetPx, transform.rotationDegrees),
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
 * One 48dp handle hit-target placed (centred) on [centerPx], drawing the frozen handle mark — a 9dp
 * paper-filled **rounded square** with a 1.6dp [color] border (`v21-bench.html:198-199`) and the
 * retained 1.5dp white halo — displaced by [markOffsetPx] so it sits on the ring.
 *
 * ### [centerPx] and [markOffsetPx] are separated on purpose, and the separation is load-bearing
 *
 * [centerPx] is the element's **geometric** corner/edge point, and it does three jobs: it places this hit
 * box, it keys the [pointerInput], and — the dangerous one — it **seeds the drag accumulator** at
 * `onDragStart`, which `TransformMath.resizeByHandle` reads as *the corner's new position*. Displace
 * [centerPx] and every resize gains a 5.5dp bias on frame one, silently: the drag anchor assertions in
 * `ResizeHandlesTest` are `> 30.0`-style inequalities that such a bias sails through.
 *
 * So the mark moves and the point does not. The trade, stated rather than discovered later: **the 48dp
 * target stays centred on the corner, not on the mark it draws.** They differ by 5.5dp, well inside a
 * target 5.3× the mark's size, and the alternative is a wrong resize.
 */
@Composable
private fun HandleTarget(
    handle: ResizeHandle,
    centerPx: PtPoint,
    markOffsetPx: Offset,
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
        BenchHandleMark(
            color = color,
            modifier = Modifier.offset { IntOffset(markOffsetPx.x.roundToInt(), markOffsetPx.y.roundToInt()) },
        )
    }
}

/**
 * The frozen handle mark (`v21-bench.html:198-199`): a 9px `--paper`-filled **rounded square**, radius
 * 2px, with a 1.6px [color] border — plus the retained halo, which the freeze does not specify.
 *
 * The freeze sets `*{box-sizing:border-box}` (`:94`), so 9px is the **outer** size and the border eats
 * into it; the halo is a 1.5px ring *outside* that, hence the 12dp envelope.
 *
 * The halo is not decoration — it is the layer that keeps the mark visible on a dark photo, where `ink`
 * alone cannot be guaranteed 3:1 ([IA §C.4](../../../../../../../../docs/design/V2-BENCH-IA-INTERACTION.md)).
 * Its ring shape follows the mark's, so on a rounded square it is a rounded square: a circular halo
 * around a square mark would show four bright ears at the corners.
 *
 * Extracted from the hit target so the appearance can be composed and measured on its own, without a
 * drag loop, an `EditorUiState` or a selection.
 */
@Composable
internal fun BenchHandleMark(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(HandleHaloDiameterDp)
            .background(HandleHaloColor, HandleHaloShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(HandleDiameterDp)
                .background(ZinelyTheme.v21Colors.paper, HandleShape)
                .border(HandleBorderDp, color, HandleShape),
        )
    }
}

/** Frozen `.hnd` `width/height:9px` — outer size, `box-sizing:border-box` (`v21-bench.html:198`). */
internal val HandleDiameterDp = 9.dp

/**
 * How far the drawn mark sits **outside** the element box, on each axis it touches.
 *
 * `.hnd.tl{left:-10px}` on a 9px border-box mark ⇒ centre at `-10 + 9/2 = -5.5px`, and the edge handles'
 * `calc(50% - 4.5px)` is the same statement on the free axis (`v21-bench.html:201-206`). The ring's stroke
 * centre is at `-6 + 1.6/2 = -5.2px`, so the marks are threaded **on** the ring, 0.3px off it — one figure.
 *
 * ⚠ This is a **correction, not a re-skin value.** Compose drew the marks at offset 0 — on the box corner,
 * 5.5dp inside the freeze — because [ADR-091](../../../../../../../../docs/DECISIONS.md#adr-091) row 2.6
 * asserted the frozen offset was *"satisfied by construction"*. That arithmetic was false in V2 as well
 * (`-10 + 13/2 = -3.5`, not 0), and "satisfied by construction" is a phrase that ends testing, so nothing
 * ever failed. The four edge handles' placement is an owner ruling of 2026-08-12 (the freeze had never
 * specified it) recorded in the same amendment banner that adds them to the frozen file.
 */
internal val HandleRingOffsetDp = 5.5.dp

/** Frozen `.hnd` `border:1.6px solid var(--ink)` (`v21-bench.html:198`). */
internal val HandleBorderDp = 1.6.dp

/** Frozen `.hnd` `border-radius:2px` — a written literal, not a `--br-*` step (`v21-bench.html:199`). */
internal val HandleRadiusDp = 2.dp

internal val HandleShape: RoundedCornerShape = RoundedCornerShape(HandleRadiusDp)

/**
 * The halo's width, retained from V2 (`v2-bench.html:296`, `0 0 0 1.5px`) under
 * [ADR-102 §12.6 row 5](../../../../../../../../docs/DECISIONS.md#adr-102-p1-corrections) — a ring
 * outside the 9px mark, so 9 + 1.5 × 2 = 12dp.
 */
internal val HandleHaloWidthDp = 1.5.dp
internal val HandleHaloDiameterDp = HandleDiameterDp + HandleHaloWidthDp * 2f

/**
 * The halo's corner. CSS grows an outset ring's radius **additively** (Backgrounds §7.1.1), so a 1.5px
 * ring around a 2px corner has a 3.5px outer radius — not 2px, and not the proportional growth a naive
 * larger-box-same-shape would give. [Modifier.zinelyV21Frame] records the same arithmetic and the review
 * that caught it being wrong.
 */
internal val HandleHaloShape: RoundedCornerShape = RoundedCornerShape(HandleRadiusDp + HandleHaloWidthDp)

/** Retained halo colour `rgba(255,255,255,.7)` (`v2-bench.html:296`). */
internal val HandleHaloColor: Color = Color.White.copy(alpha = 0.7f)
