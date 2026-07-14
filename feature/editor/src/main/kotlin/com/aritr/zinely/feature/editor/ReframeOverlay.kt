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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.render.android.AssetBytesSource
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlin.math.roundToInt

/** Test tag on the Reframe preview overlay canvas. */
public const val ReframeOverlayTestTag: String = "reframe-overlay"

/**
 * The Reframe preview overlay (ADR-053, frozen bench.html `.reframing`): the photo pans/zooms **inside a
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
 * @param onAspect reports the decoded photo aspect (`w/h`) up so the host can seed/commit with the true
 *   ratio; fires once per successful decode.
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
    val scrim = ZinelyTheme.colors.scrim
    val outline = ZinelyTheme.colors.coralStrong
    val bratio = element.transform.widthPt / element.transform.heightPt

    // Decode the canonical master to an ImageBitmap once per (asset, source). A missing/undecodable asset
    // (the default EmptyAssetBytes, or a TOCTOU delete) yields null → the frame + scrim still draw so the
    // controls remain usable; the render path shows its own placeholder for the committed result.
    val decoded: DecodedPhoto? = remember(element.assetId, imageBytes) { decodePhoto(imageBytes, element.assetId) }
    val pratio = decoded?.aspect ?: bratio
    LaunchedEffect(decoded) { if (decoded != null) onAspect(decoded.aspect) }

    val latestDraft by rememberUpdatedState(draft)

    Canvas(
        modifier = modifier
            .testTag(ReframeOverlayTestTag)
            .pointerInput(element.id, screenPxPerPt) {
                detectTransformGestures { _, pan, zoomChange, _ ->
                    var d = latestDraft
                    if (d.fit != FrameFit.FILL) return@detectTransformGestures // Whole is static (bench parity)
                    if (zoomChange != 1f) d = Framing.zoomed(d, zoomChange.toDouble())
                    // Device px → image-fraction: the frame width shows `coverW/zoom` of the image, so
                    // one px is that many fractions; dragging the photo right moves the crop left.
                    val frameWpx = element.transform.widthPt * screenPxPerPt
                    val frameHpx = element.transform.heightPt * screenPxPerPt
                    val (cw0, ch0) = Framing.coverExtent(pratio, bratio)
                    val fx = if (frameWpx > 0f) -pan.x * (cw0 / d.zoom) / frameWpx else 0.0
                    val fy = if (frameHpx > 0f) -pan.y * (ch0 / d.zoom) / frameHpx else 0.0
                    onDraft(Framing.panned(d, fx, fy, pratio, bratio))
                }
            },
    ) {
        val frame = frameRectPx(element, screenPxPerPt.toDouble(), pageOffset)
        val center = frame.center
        rotate(degrees = element.transform.rotationDegrees.toFloat(), pivot = center) {
            // 1) The movable photo (unclipped: overflow spills out to be dimmed by the scrim).
            if (decoded != null) {
                val dst = photoDestPx(draft, frame, decoded, pratio, bratio)
                if (dst.width >= 1f && dst.height >= 1f) {
                    drawImage(
                        image = decoded.bitmap,
                        dstOffset = IntOffset(dst.left.roundToInt(), dst.top.roundToInt()),
                        dstSize = IntSize(dst.width.roundToInt(), dst.height.roundToInt()),
                    )
                }
            }
            // 2) Scrim everything except the frame window (four rects; extended well past the stage so the
            // rotated case still covers). Dims the overflow so the frame reads as the kept region.
            val big = maxOf(size.width, size.height) * 2f
            drawRect(scrim, topLeft = Offset(-big, -big), size = Size(big * 2f, frame.top + big)) // above
            drawRect(scrim, topLeft = Offset(-big, frame.bottom), size = Size(big * 2f, big)) // below
            drawRect(scrim, topLeft = Offset(-big, frame.top), size = Size(frame.left + big, frame.height)) // left
            drawRect(scrim, topLeft = Offset(frame.right, frame.top), size = Size(big, frame.height)) // right
            // 3) The frame outline + rule-of-thirds window.
            drawRect(outline, topLeft = frame.topLeft, size = frame.size, style = Stroke(width = 2.dp.toPx()))
            val third = Color.White.copy(alpha = 0.34f)
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
 * Where to draw the whole photo so the [draft]'s framing lands inside [frame]. FILL maps the resolved crop
 * rect onto the frame (so the crop fills it, the rest overflows); WHOLE contains the whole photo centred.
 */
private fun photoDestPx(draft: FramingDraft, frame: Rect, photo: DecodedPhoto, pratio: Double, bratio: Double): Rect {
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
