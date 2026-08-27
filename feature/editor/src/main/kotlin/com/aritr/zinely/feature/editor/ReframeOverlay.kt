package com.aritr.zinely.feature.editor

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.render.android.AssetBytesSource
import com.aritr.zinely.render.android.readImageIntrinsics
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Scrim
import kotlin.math.roundToInt

/** Test tag on the Reframe preview overlay canvas. */
public const val ReframeOverlayTestTag: String = "reframe-overlay"

/**
 * The Reframe preview overlay (ADR-053; V2.1 paint per ADR-102 §12.16, `v21-reframe.html` `.frame`,
 * superseding the V1 `bench.html .reframing` this file was first written against): the photo pans/zooms **inside a
 * fixed frame**, its cropped-away overflow shown dimmed (the "the picture moves" teach), with a rule-of-
 * thirds window over the frame. It is a **preview-only** layer — it paints the decoded photo through the
 * ephemeral [draft] and never mutates the document; the reducer sees only the baked [Intent.CommitReframe]
 * ([com.aritr.zinely.core.editor.Intent.CommitReframe]) on Done.
 *
 * The frame is the element's on-page box mapped to device px (`(pagePt + pageOffset)·screenPxPerPt`), with
 * the element rotation applied about its centre — the same placement the renderer uses, so the frame sits
 * on the rendered box. The photo is drawn so its resolved crop rect ([Framing.resolveCrop]) maps onto the
 * frame; the canvas is unclipped, so the rest of the photo spills out and the scrim dims it.
 *
 * Gestures retarget the PHOTO (bench's one-grammar rule): drag pans, pinch zooms — both routed through the
 * pure [Framing] helpers so they clamp (Fill never gaps) and match the commit. [FrameFit.WHOLE] shows the
 * whole photo contained and ignores pan/zoom (bench parity).
 *
 * @param element the reframing photo (the session `before`; supplies assetId + the frame box).
 * @param draft the ephemeral working fit/zoom/pan.
 * @param onAspect reports the photo's intrinsic aspect (`w/h`) up so the host can seed/commit with the
 *   true ratio. Fires **only when the photo is both measurable and displayable**, so it doubles as the
 *   host's "the user may adjust" signal (M7-01): while it has not fired, the frame is inert and the
 *   session commits the element unchanged. One signal, so the two gates cannot drift apart.
 * @param onDraft receives a gesture-updated [draft] (host stores it; never the reducer).
 */
@Composable
public fun ReframeOverlay(
    element: ImageElement,
    draft: FramingDraft,
    screenPxPerPt: Float,
    pageOffset: PtPoint,
    imageBytes: AssetBytesSource,
    onAspect: (Double) -> Unit,
    onDraft: (FramingDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    // V2.1 (ADR-102 §12.16, `v21-reframe.html` `.frame`). What changed is paint, not geometry:
    //
    //  * the **boundary** was `coralStrong` at 2dp solid — the last coral on the Bench, and a system
    //    rectangle drawn over the user's photograph. The spec draws it by hand instead: *"THE BOUNDARY IS
    //    A DRAWN LINE, NOT A SYSTEM RECTANGLE. Dashed ink, the same hand as the selection ring — because
    //    it means the same thing: this is the edge you are working to."* So it is literally the ring's
    //    hand: [SelectionOutlineStrokeDp] and [SelectionOutlineDashDp], the same `1.6px dashed var(--ink)`
    //    declaration, read from the same constants rather than re-transcribed into a second pair.
    //  * the **scrim** was `ZinelyTheme.colors.scrim` (V1). The 37596 palette derives one invariant wash
    //    (`v21-bench.html:359`, `rgba(39,39,15,.44)`), hoisted to [ZinelyV21Scrim]. Read it from *there*
    //    and not from `BenchGridScrimColor`, which is
    //    what this first did: that constant is documented as a **modal backdrop**, and this dim is a
    //    permanent crop dimmer that never animates to full. Same value, different job — so darkening the
    //    modal scrim must not silently change how much of the user's cropped-away photo stays visible.
    //  * the **frame ground** is new: `.frame{background:var(--desk-edge)}`. It is what shows through in
    //    [FrameFit.WHOLE], where the contained photo letterboxes — previously the page's own paper, so the
    //    letterbox bars were invisible and the frame lost its edges wherever the photo did not reach.
    val colors = ZinelyTheme.v21Colors
    val scrim = ZinelyV21Scrim
    val boundary = colors.ink
    val frameGround = colors.deskEdge
    // ⚠ The thirds lie **over the user's photograph**, and stay a hardcoded white on purpose.
    //
    // This was briefly changed to follow a theme token, on the reasoning that a token-derived guide keeps
    // the room's voice. A review killed it and was right: the photograph does not change with the app's
    // theme, and no palette token can guarantee contrast over an arbitrary photograph.
    //
    // No token can promise a ratio over a photograph — the constraint [SelectionChrome] records for the
    // ring — and the repo already has one answer to that problem: [HandleHaloColor], a hardcoded
    // `Color.White` at .7 behind the resize marks, for exactly this reason. This is the same answer at a
    // lighter weight.
    val thirds = Color.White
    val bratio = element.transform.widthPt / element.transform.heightPt

    // Aspect and pixels come from two DIFFERENT decodes, deliberately (M7-01).
    //
    // **Aspect** comes from the shared [readImageIntrinsics] seam (ADR-056) — the *same function* the
    // renderer's `ImageBlitter` calls, not a second implementation that happens to agree. The committed
    // crop is resolved against `pratio` while the page resolves the same geometry against the intrinsic
    // size; deriving them separately is what broke `preview == export` before (INV-01 failure mode 3),
    // because the overlay's full-resolution decode could fail where a header read cannot, and its
    // fallback to the box ratio then disagreed with the renderer's true one.
    val intrinsic = remember(element.assetId, imageBytes) { readImageIntrinsics(imageBytes, element.assetId) }
    // **Pixels** are what the user actually frames against; a missing/undecodable asset (the default
    // EmptyAssetBytes, or a TOCTOU delete) yields null.
    val decoded: DecodedPhoto? = remember(element.assetId, imageBytes) { decodePhoto(imageBytes, element.assetId) }
    val pratio = intrinsic?.aspect ?: bratio
    // The frame is adjustable ONLY while the photo is genuinely on screen. Framing blind is what produced
    // the divergence above: the controls stayed live, the draft moved, and the commit baked a crop against
    // a photo the user could not see. `onAspect` is therefore the single signal for BOTH "the true aspect
    // is known" and "the user may adjust", so the host's gate and this one cannot drift apart.
    val framable = intrinsic != null && decoded != null
    LaunchedEffect(intrinsic, decoded) { if (intrinsic != null && decoded != null) onAspect(intrinsic.aspect) }

    val latestDraft by rememberUpdatedState(draft)

    Canvas(
        modifier = modifier
            .testTag(ReframeOverlayTestTag)
            .pointerInput(element.id, screenPxPerPt, framable) {
                detectTransformGestures { _, pan, zoomChange, _ ->
                    var d = latestDraft
                    // No photo on screen ⇒ no framing. Gestures are inert rather than silently moving a
                    // crop the user cannot see (M7-01); the host gates its keyboard/button verbs the same way.
                    if (!framable) return@detectTransformGestures
                    if (d.fit != FrameFit.FILL) return@detectTransformGestures // Whole is static (bench parity)
                    if (zoomChange != 1f) d = Framing.zoomed(d, zoomChange.toDouble())
                    // Device px → image-fraction: the frame width shows `coverW/zoom` of the image, so
                    // one px is that many fractions; dragging the photo right moves the crop left.
                    val frameWpx = element.transform.widthPt * screenPxPerPt
                    val frameHpx = element.transform.heightPt * screenPxPerPt
                    val (cw0, ch0) = Framing.coverExtent(pratio, bratio)
                    val fx = reframePanFraction(
                        panPx = pan.x,
                        coverExtent = cw0,
                        zoom = d.zoom,
                        framePx = frameWpx,
                        flipped = element.flippedHorizontally,
                    )
                    val fy = reframePanFraction(
                        panPx = pan.y,
                        coverExtent = ch0,
                        zoom = d.zoom,
                        framePx = frameHpx,
                        flipped = element.flippedVertically,
                    )
                    onDraft(Framing.panned(d, fx, fy, pratio, bratio))
                }
            },
    ) {
        val frame = frameRectPx(element, screenPxPerPt.toDouble(), pageOffset)
        val center = frame.center
        val frameRadius = CornerRadius(ZinelyV21Dimens.radiusXs.toPx(), ZinelyV21Dimens.radiusXs.toPx())
        rotate(degrees = element.transform.rotationDegrees.toFloat(), pivot = center) {
            // 0) `.frame{background:var(--desk-edge)}` — the frame's own ground, under the photo. Seen only
            // where the photo does not reach it (WHOLE letterboxes; a mid-drag FILL cannot gap).
            drawRoundRect(frameGround, topLeft = frame.topLeft, size = frame.size, cornerRadius = frameRadius)
            // 1) The movable photo (unclipped: overflow spills out to be dimmed by the scrim).
            // Gated on `framable`, not merely on `decoded`: without a measured intrinsic the aspect is the
            // box-ratio fallback, and painting the photo through it would show a distorted picture the page
            // will never render. Unreachable in practice (a master that decodes has a readable header), but
            // drawing nothing beats drawing a lie (Observation O-1).
            val dst = if (framable) photoDestPx(draft, frame, pratio, bratio) else null
            if (dst != null && decoded != null && dst.width >= 1f && dst.height >= 1f) {
                withTransform({
                    scale(
                        scaleX = if (element.flippedHorizontally) -1f else 1f,
                        scaleY = if (element.flippedVertically) -1f else 1f,
                        pivot = center,
                    )
                }) {
                    drawImage(
                        image = decoded.bitmap,
                        dstOffset = IntOffset(dst.left.roundToInt(), dst.top.roundToInt()),
                        dstSize = IntSize(dst.width.roundToInt(), dst.height.roundToInt()),
                    )
                    // The dim belongs to the cropped-away photo, so it reflects with that photo too.
                    for (r in photoScrimRectsPx(dst, frame)) {
                        drawRect(scrim, topLeft = r.topLeft, size = r.size)
                    }
                }
            }
            // 2) Scrim the PHOTO'S OVERFLOW ONLY — `dst - frame`, never the stage.
            //
            // ⚠ INVARIANT (ADR-090 / OD-12: *"the artifact does not dim; the room around it may"*). This
            // used to paint four rects sized `max(size)*2` — i.e. everything outside the frame, to the
            // edges of the stage and past them. On a device that dimmed the user's cream paper sheet to a
            // muddy taupe the moment Reframe opened, and every other element on it: the rule exactly
            // inverted, the artifact dimmed and the room untouched. Verified on hardware, not theorised.
            //
            // The scrim's job is narrow and worth keeping: make the frame read as *the kept region* by
            // showing the spilled part of the photo, dimmed, so the "the picture moves" teach shows what
            // is being cropped away. That needs the photo's overflow and nothing else. So the rects are
            // bounded to [dst] — no photo (unmeasurable/undecodable) or no overflow (WHOLE letterboxes
            // inside the frame) means no scrim paints at all.
            //
            // ⚠ Accepted deviation, from the rounded frame — and note it is now only about CORNERS. The
            // spec's `.frame{overflow:hidden}` is a prototype convenience, not the specified behaviour
            // (`v21-reframe.html`, amendment 2026-08-15): unclipped-and-dimmed IS the spec. What still
            // deviates is that these rects are square while the frame is rounded, so in each 4dp corner arc
            // a sliver of photo paints outside the ground and outside the boundary, undimmed. Clipping the
            // photo to a rounded rect and drawing it twice would close it, and is not worth 4dp of corner.
            // Drawn with the photo above so reflection cannot leave the overflow dim on the old side.
            // 3) The drawn boundary — `.frame::after{border:1.6px dashed var(--ink);border-radius:var(--br-xs)}`.
            val dashPx = SelectionOutlineDashDp.toPx()
            drawRoundRect(
                boundary,
                topLeft = frame.topLeft,
                size = frame.size,
                cornerRadius = frameRadius,
                style = Stroke(
                    width = SelectionOutlineStrokeDp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, dashPx)),
                ),
            )
            // 4) The rule-of-thirds window. ⚠ OPEN (owner call): the V2.1 Reframe spec has no thirds grid
            // — it is V1 chrome the re-skin retinted rather than re-decided, because deleting a guide is a
            // redesign and this change is a transcription. Recorded in ADR-102 §12.16 as a question.
            val third = thirds.copy(alpha = 0.34f)
            val tw = 1.dp.toPx()
            for (i in 1..2) {
                val x = frame.left + frame.width * i / 3f
                drawLine(third, Offset(x, frame.top), Offset(x, frame.bottom), strokeWidth = tw)
                val y = frame.top + frame.height * i / 3f
                drawLine(third, Offset(frame.left, y), Offset(frame.right, y), strokeWidth = tw)
            }
        }
    }
}

/** Screen-directional drag converted into crop-fraction movement for one possibly reflected axis. */
internal fun reframePanFraction(
    panPx: Float,
    coverExtent: Double,
    zoom: Double,
    framePx: Double,
    flipped: Boolean,
): Double {
    if (framePx <= 0.0 || !coverExtent.isFinite() || !zoom.isFinite() || zoom <= 0.0) return 0.0
    val unflipped = -panPx * (coverExtent / zoom) / framePx
    return if (flipped) -unflipped else unflipped
}

/** A decoded photo + its aspect (`w/h`). */
internal class DecodedPhoto(val bitmap: ImageBitmap, val widthPx: Int, val heightPx: Int) {
    val aspect: Double get() = widthPx.toDouble() / heightPx.toDouble()
}

/** Decode the master bytes to an [ImageBitmap]; null if absent/undecodable (treated as missing). */
internal fun decodePhoto(source: AssetBytesSource, assetId: String): DecodedPhoto? {
    val stream = source.open(assetId) ?: return null
    val bmp = runCatching { stream.use { BitmapFactory.decodeStream(it) } }.getOrNull() ?: return null
    if (bmp.width <= 0 || bmp.height <= 0) return null
    return DecodedPhoto(bmp.asImageBitmap(), bmp.width, bmp.height)
}

/** The element's on-page box in device px (axis-aligned, pre-rotation; the caller rotates about its centre). */
private fun frameRectPx(element: ImageElement, screenPxPerPt: Double, pageOffset: PtPoint): Rect {
    val left = ((element.transform.xPt + pageOffset.x) * screenPxPerPt).toFloat()
    val top = ((element.transform.yPt + pageOffset.y) * screenPxPerPt).toFloat()
    val w = (element.transform.widthPt * screenPxPerPt).toFloat()
    val h = (element.transform.heightPt * screenPxPerPt).toFloat()
    return Rect(left, top, left + w, top + h)
}

/**
 * The set difference `dst - frame`: the parts of the drawn photo that spill outside the frame window, as up
 * to four non-overlapping rects (above / below / left / right of the window), each clipped to [dst].
 *
 * ⚠ INVARIANT (ADR-090 / OD-12): **nothing here may fall outside [dst]**. The scrim dims the user's
 * cropped-away photo; it must never dim the page the photo sits on. Clamping the window's edges into
 * [dst]'s range is what guarantees it — it also makes every returned rect non-negative, and returns an
 * empty list when the photo does not overflow at all (WHOLE letterboxes *inside* the frame, so there is no
 * overflow to dim and no scrim should paint).
 */
internal fun photoScrimRectsPx(dst: Rect, frame: Rect): List<Rect> {
    if (dst.isEmpty) return emptyList() // degenerate photo rect: nothing drawn, so nothing to dim
    val l = frame.left.coerceIn(dst.left, dst.right)
    val r = frame.right.coerceIn(dst.left, dst.right)
    val t = frame.top.coerceIn(dst.top, dst.bottom)
    val b = frame.bottom.coerceIn(dst.top, dst.bottom)
    return listOf(
        Rect(dst.left, dst.top, dst.right, t), // above
        Rect(dst.left, b, dst.right, dst.bottom), // below
        Rect(dst.left, t, l, b), // left
        Rect(r, t, dst.right, b), // right
    ).filter { it.width > 0f && it.height > 0f }
}

/**
 * Where to draw the whole photo so the [draft]'s framing lands inside [frame]. FILL maps the resolved crop
 * rect onto the frame (so the crop fills it, the rest overflows); WHOLE contains the whole photo centred.
 */
internal fun photoDestPx(draft: FramingDraft, frame: Rect, pratio: Double, bratio: Double): Rect {
    if (draft.fit == FrameFit.WHOLE) {
        // Contain: fit the whole photo inside the frame, preserving aspect, centred.
        val dstW: Float
        val dstH: Float
        if (pratio >= bratio) { dstW = frame.width; dstH = (frame.width / pratio).toFloat() }
        else { dstH = frame.height; dstW = (frame.height * pratio).toFloat() }
        val left = frame.left + (frame.width - dstW) / 2f
        val top = frame.top + (frame.height - dstH) / 2f
        return Rect(left, top, left + dstW, top + dstH)
    }
    val crop = Framing.resolveCrop(draft, pratio, bratio)
    val cw = (crop.right - crop.left).toFloat()
    val ch = (crop.bottom - crop.top).toFloat()
    val fullW = frame.width / cw
    val fullH = frame.height / ch
    val left = frame.left - crop.left.toFloat() * fullW
    val top = frame.top - crop.top.toFloat() * fullH
    return Rect(left, top, left + fullW, top + fullH)
}
