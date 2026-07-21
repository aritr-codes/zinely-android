package com.aritr.zinely.render.android

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.render.DrawImage
import com.aritr.zinely.core.render.ImageBlit
import com.aritr.zinely.core.render.computeImageBlit
import kotlin.math.roundToInt

/**
 * The single, shared image draw path (ADR-028 §5, ADR-006). Every canvas provider blits through **one**
 * `ImageBlitter`, so there is no per-backend fit/crop math to diverge: the pure [computeImageBlit]
 * ([`:core:render`][com.aritr.zinely.core.render.computeImageBlit]) is the sole source of
 * `(srcFraction, destRect)`; the blitter only turns that into pixels.
 *
 * Per draw: bounds-decode the canonical master ([AssetBytesSource], open #1) for the intrinsic px →
 * [computeImageBlit] → **crop-aware, resolution-aware** decode of the visible region (open #2) →
 * `drawBitmap` into the point-space `destRect`. A missing asset or a failed decode (TOCTOU — render
 * cannot detect absence) paints a defined [placeholder][drawPlaceholder], never crashing or drawing
 * nothing (§5.4). Both opens use a **fresh stream** and are closed; the bitmap is `recycle()`d at once.
 */
public class ImageBlitter(private val assetBytes: AssetBytesSource) {

    /** Pinned image paint (§4.1): bilinear filter on, no dither; anti-aliased edges. */
    private val imagePaint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        isDither = false
    }

    /**
     * @param decodePxPerPt page-local-point → device-pixel density (§3.2: `300/72` for export, screen
     *   scale for preview) — the **separate** decode resolution, never inferred from the PDF canvas CTM.
     * @param localScale the element-local → page-local linear scale (the scale part of `localToPage`),
     *   so a scaled element still decodes at its true on-page footprint.
     */
    public fun draw(canvas: Canvas, command: DrawImage, decodePxPerPt: Double, localScale: Double) {
        val bounds = decodeBounds(command.assetId)
        if (bounds == null) {
            drawPlaceholder(canvas, command.box)
            return
        }
        val (intrinsicW, intrinsicH) = bounds
        val blit = computeImageBlit(
            intrinsicWidthPx = intrinsicW,
            intrinsicHeightPx = intrinsicH,
            crop = command.crop,
            fit = command.fit,
            boxWidthPt = command.box.width,
            boxHeightPt = command.box.height,
        )
        val bitmap = decodeVisibleRegion(command.assetId, intrinsicW, intrinsicH, blit, decodePxPerPt, localScale)
        if (bitmap == null) {
            drawPlaceholder(canvas, command.box)
            return
        }
        try {
            canvas.drawBitmap(bitmap, null, blit.destRect.toRectF(), imagePaint)
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Open #1: the intrinsic px (the ground truth, seam A) — via the shared [readImageIntrinsics]
     * (ADR-056), which the editor's Reframe overlay also calls, so no surface can resolve framing against
     * a different intrinsic than the one drawn here.
     */
    private fun decodeBounds(assetId: String): Pair<Int, Int>? =
        readImageIntrinsics(assetBytes, assetId)?.let { it.widthPx to it.heightPx }

    /**
     * Open #2: decode the visible source region `srcFraction × intrinsic` to roughly the device px the
     * destination occupies (`destRect × decodePxPerPt × localScale`). Region decode samples only the
     * crop, so a small crop is not starved; if [BitmapRegionDecoder] cannot handle the format, fall back
     * to a whole-image decode sized so the *region* still yields the needed px (inflate by
     * `1/srcFraction`), then sub-rect it (§5.1, Required-fix #2).
     */
    private fun decodeVisibleRegion(
        assetId: String,
        intrinsicW: Int,
        intrinsicH: Int,
        blit: ImageBlit,
        decodePxPerPt: Double,
        localScale: Double,
    ): Bitmap? {
        val region = blit.srcFraction.toMasterRegion(intrinsicW, intrinsicH)
        val regionWidthPx = region.width()

        // Device px the destination occupies → the region (which fills the dest) must carry that many.
        val destPxW = blit.destRect.width * decodePxPerPt * localScale
        val regionSample = inSampleSizeFor(regionWidthPx, destPxW)

        // Open #2 — region decode. A `null` stream is MISSING (no fallback, per the AssetBytesSource
        // contract); only a decoder *failure* on a VALID stream falls through to the whole-image path
        // (Codex Required-fix). `use {}` closes the stream even when decodeRegion throws.
        val regionStream = assetBytes.open(assetId) ?: return null
        val regionBitmap = regionStream.use { stream ->
            runCatching {
                @Suppress("DEPRECATION") // newInstance(stream, shareable) is the minSdk-24 API.
                BitmapRegionDecoder.newInstance(stream, false)?.let { decoder ->
                    try {
                        decoder.decodeRegion(region, decodeOptions(regionSample))
                    } finally {
                        decoder.recycle()
                    }
                }
            }.getOrNull()
        }
        if (regionBitmap != null) return regionBitmap

        // Region decoder unsupported/failed on a valid stream → whole-image fallback (open #3), sized so
        // the REGION still yields destPxW px (inflate by 1/srcFraction).
        val srcFractionW = blit.srcFraction.width.coerceAtLeast(MIN_FRACTION)
        val fullSample = inSampleSizeFor(intrinsicW, destPxW / srcFractionW)
        return decodeWholeThenCrop(assetId, region, fullSample)
    }

    private fun decodeWholeThenCrop(assetId: String, region: Rect, sampleSize: Int): Bitmap? {
        val stream = assetBytes.open(assetId) ?: return null // null open ⇒ missing
        val whole = runCatching {
            stream.use { BitmapFactory.decodeStream(it, null, decodeOptions(sampleSize)) }
        }.getOrNull() ?: return null
        return runCatching {
            val left = (region.left / sampleSize).coerceIn(0, whole.width - 1)
            val top = (region.top / sampleSize).coerceIn(0, whole.height - 1)
            val right = (region.right / sampleSize).coerceIn(left + 1, whole.width)
            val bottom = (region.bottom / sampleSize).coerceIn(top + 1, whole.height)
            val cropped = Bitmap.createBitmap(whole, left, top, right - left, bottom - top)
            // createBitmap may return `whole` itself when the sub-rect is the full image; only recycle a copy.
            if (cropped !== whole) whole.recycle()
            cropped
        }.getOrElse {
            whole.recycle()
            null
        }
    }

    private fun decodeOptions(sampleSize: Int): BitmapFactory.Options =
        BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888 // ADR-011
        }

    /** Largest power-of-two sample size keeping the decoded extent ≥ [needPx]. */
    private fun inSampleSizeFor(havePx: Int, needPx: Double): Int {
        if (needPx <= 0.0 || havePx <= 0) return 1
        var sample = 1
        while (havePx / (sample * 2) >= needPx) sample *= 2
        return sample
    }

    /** Defined, deterministic missing-asset placeholder: neutral fill + border + broken-image cross (§5.4). */
    private fun drawPlaceholder(canvas: Canvas, box: PtRect) {
        val l = box.x.toFloat()
        val t = box.y.toFloat()
        val r = box.right.toFloat()
        val b = box.bottom.toFloat()
        canvas.drawRect(l, t, r, b, placeholderFill)
        canvas.drawRect(l, t, r, b, placeholderStroke)
        canvas.drawLine(l, t, r, b, placeholderStroke)
        canvas.drawLine(l, b, r, t, placeholderStroke)
    }

    private val placeholderFill = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = false
        color = PLACEHOLDER_FILL_ARGB
    }
    private val placeholderStroke = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeWidth = 1f // point space; the canvas matrix scales it
        color = PLACEHOLDER_STROKE_ARGB
    }

    private companion object {
        /** Guards a divide-by-zero on a degenerate crop fraction. */
        const val MIN_FRACTION = 1e-6

        const val PLACEHOLDER_FILL_ARGB = 0xFFE0E0E0.toInt()
        const val PLACEHOLDER_STROKE_ARGB = 0xFF9E9E9E.toInt()
    }
}

/** The crop's source region in master pixels, clamped to a ≥ 1px extent inside the image bounds. */
private fun PtRect.toMasterRegion(intrinsicW: Int, intrinsicH: Int): Rect {
    val l = (x * intrinsicW).roundToInt().coerceIn(0, intrinsicW - 1)
    val t = (y * intrinsicH).roundToInt().coerceIn(0, intrinsicH - 1)
    val r = (right * intrinsicW).roundToInt().coerceIn(l + 1, intrinsicW)
    val b = (bottom * intrinsicH).roundToInt().coerceIn(t + 1, intrinsicH)
    return Rect(l, t, r, b)
}

private fun PtRect.toRectF(): RectF = RectF(x.toFloat(), y.toFloat(), right.toFloat(), bottom.toFloat())
