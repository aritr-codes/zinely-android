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
import com.aritr.zinely.core.render.COPIER_DOTS_PER_POINT
import com.aritr.zinely.core.render.copierGridSize
import com.aritr.zinely.core.render.photocopy
import kotlin.math.roundToInt

/**
 * The single, shared image draw path (ADR-028 §5, ADR-006). Every canvas provider blits through **one**
 * `ImageBlitter`, so there is no per-backend fit/crop math to diverge: the pure [computeImageBlit]
 * ([`:core:render`][com.aritr.zinely.core.render.computeImageBlit]) is the sole source of
 * `(srcFraction, destRect)`; the blitter only turns that into pixels.
 *
 * Without a cache (the default): bounds-decode the canonical master ([AssetBytesSource], open #1) for
 * the intrinsic px → [computeImageBlit] → **crop-aware, resolution-aware** decode of the visible region
 * (open #2) → `drawBitmap` into the point-space `destRect`. A missing asset or failed decode paints a
 * defined [placeholder][drawPlaceholder], never crashing or drawing nothing (§5.4). Both opens use a
 * fresh stream and are closed; the bitmap is recycled at once.
 *
 * An interactive preview may supply [maxCacheBytes]. Successful intrinsics and non-copier region decodes
 * are then retained in a bounded LRU until [close]; failed reads are never cached. Export leaves the
 * budget at zero, preserving its one-draw ownership and memory behaviour.
 */
public class ImageBlitter(
    private val assetBytes: AssetBytesSource,
    private val maxCacheBytes: Long = 0L,
) : AutoCloseable {

    init {
        require(maxCacheBytes >= 0L) { "maxCacheBytes must be non-negative" }
    }

    private data class BitmapCacheKey(
        val assetId: String,
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
        val sampleSize: Int,
    )

    private data class DecodedBitmap(val bitmap: Bitmap, val cached: Boolean)

    private val intrinsicsCache = LinkedHashMap<String, Pair<Int, Int>>(INTRINSICS_CACHE_CAPACITY, 0.75f, true)
    private val bitmapCache = LinkedHashMap<BitmapCacheKey, Bitmap>(8, 0.75f, true)
    private var cachedBitmapBytes = 0L

    internal val cachedBitmapCount: Int get() = bitmapCache.size

    /** Pinned image paint (§4.1): bilinear filter on, no dither; anti-aliased edges. */
    private val imagePaint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        isDither = false
    }

    /** Photocopier paint: every smoothing switch **off**, so a dithered dot magnifies to a hard dot. */
    private val copierPaint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
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
        // ⚠ A copier photo decodes at the FILTER's density, not the surface's — this line is what makes
        // preview == export true rather than nearly true. Review found the first version broken here: with
        // the surface density, the editor (screen px/pt) and the exporter (300/72) fed the dither two
        // differently-resampled sets of pixels, and error diffusion turns any difference into a different
        // dot pattern. Pinning the decode to `COPIER_DOTS_PER_POINT` makes `inSampleSizeFor` a function of
        // the master and the page alone, so both surfaces dither byte-identical input. It also stops the
        // export path decoding at 300dpi only to throw most of it away.
        val decodeDensity = if (command.copier) COPIER_DOTS_PER_POINT else decodePxPerPt
        val decoded = decodeVisibleRegion(
            assetId = command.assetId,
            intrinsicW = intrinsicW,
            intrinsicH = intrinsicH,
            blit = blit,
            decodePxPerPt = decodeDensity,
            localScale = localScale,
            allowCache = !command.copier,
        )
        if (decoded == null) {
            drawPlaceholder(canvas, command.box)
            return
        }
        val bitmap = decoded.bitmap
        val drawn = if (command.copier) photocopied(bitmap, blit, localScale) else bitmap
        try {
            // Filtering off is right when the dot grid is being MAGNIFIED and wrong when it is being
            // minified — a page thumbnail draws a ~300-dot halftone into ~30px, where nearest-neighbour
            // is moiré and nothing else (review finding). So the hard-dot paint is used only when the
            // destination has at least a device pixel per dot.
            val destPx = blit.destRect.width * decodePxPerPt * localScale
            val hardDots = command.copier && drawn.width <= destPx
            canvas.drawBitmap(drawn, null, blit.destRect.toRectF(), if (hardDots) copierPaint else imagePaint)
        } finally {
            if (drawn !== bitmap || !decoded.cached) drawn.recycle()
            if (drawn !== bitmap) bitmap.recycle()
        }
    }

    /**
     * The photocopier filter (X3b, ADR-106) — the **only** Android half of it: resample to the pure
     * dot grid, hand the pixels to [photocopy], hand back a 1-bit-valued bitmap. All of the algorithm
     * and all of the sizing law live in `:core:render`.
     *
     * The grid comes from `destRect × localScale` in **page points**, never from the decoded bitmap,
     * which is what makes the editor's dots and the exporter's dots the same physical size (ADR-006).
     * The magnification back up to `destRect` is left to `drawBitmap` under [copierPaint], whose
     * filtering is off — a bilinear magnify would grey the dots back into the halftone's opposite.
     *
     * Falls back to the unfiltered bitmap if allocation fails, because a photo is better than a hole.
     */
    private fun photocopied(source: Bitmap, blit: ImageBlit, localScale: Double): Bitmap {
        val gridW = copierGridSize(blit.destRect.width * localScale, source.width)
        val gridH = copierGridSize(blit.destRect.height * localScale, source.height)
        // `runCatching` here deliberately catches Error too, OutOfMemoryError included: a second full-size
        // allocation is exactly where a low-memory device gives out, and an unfiltered photo is a better
        // answer than a crashed export. The one place this file bends "never swallow", and it says so.
        return runCatching {
            val small = Bitmap.createScaledBitmap(source, gridW, gridH, true)
            val pixels = try {
                IntArray(gridW * gridH).also { small.getPixels(it, 0, gridW, 0, 0, gridW, gridH) }
            } finally {
                // `finally`, not a trailing call: `createScaledBitmap` returns `source` itself when no
                // scaling is needed, and a throw inside getPixels would otherwise leak the copy.
                if (small !== source) small.recycle()
            }
            Bitmap.createBitmap(photocopy(pixels, gridW, gridH), gridW, gridH, Bitmap.Config.ARGB_8888)
        }.getOrDefault(source)
    }

    /**
     * Open #1: the intrinsic px (the ground truth, seam A) — via the shared [readImageIntrinsics]
     * (ADR-056), which the editor's Reframe overlay also calls, so no surface can resolve framing against
     * a different intrinsic than the one drawn here.
     */
    private fun decodeBounds(assetId: String): Pair<Int, Int>? {
        if (maxCacheBytes > 0L) intrinsicsCache[assetId]?.let { return it }
        val decoded = readImageIntrinsics(assetBytes, assetId)?.let { it.widthPx to it.heightPx } ?: return null
        if (maxCacheBytes > 0L) {
            intrinsicsCache[assetId] = decoded
            while (intrinsicsCache.size > INTRINSICS_CACHE_CAPACITY) {
                intrinsicsCache.entries.iterator().run {
                    next()
                    remove()
                }
            }
        }
        return decoded
    }

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
        allowCache: Boolean,
    ): DecodedBitmap? {
        val region = blit.srcFraction.toMasterRegion(intrinsicW, intrinsicH)
        val regionWidthPx = region.width()

        // Device px the destination occupies → the region (which fills the dest) must carry that many.
        val destPxW = blit.destRect.width * decodePxPerPt * localScale
        val regionSample = inSampleSizeFor(regionWidthPx, destPxW)
        val cacheKey = BitmapCacheKey(
            assetId = assetId,
            left = region.left,
            top = region.top,
            right = region.right,
            bottom = region.bottom,
            sampleSize = regionSample,
        )
        if (allowCache && maxCacheBytes > 0L) {
            bitmapCache[cacheKey]?.takeUnless(Bitmap::isRecycled)?.let { return DecodedBitmap(it, cached = true) }
        }

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
        if (regionBitmap != null) {
            return DecodedBitmap(regionBitmap, cacheIfEligible(cacheKey, regionBitmap, allowCache))
        }

        // Region decoder unsupported/failed on a valid stream → whole-image fallback (open #3), sized so
        // the REGION still yields destPxW px (inflate by 1/srcFraction).
        val srcFractionW = blit.srcFraction.width.coerceAtLeast(MIN_FRACTION)
        val fullSample = inSampleSizeFor(intrinsicW, destPxW / srcFractionW)
        return decodeWholeThenCrop(assetId, region, fullSample)?.let { bitmap ->
            // The fallback may need a different whole-image sample to preserve the requested region
            // resolution, but its returned pixels still represent this exact region request.
            DecodedBitmap(bitmap, cacheIfEligible(cacheKey, bitmap, allowCache))
        }
    }

    private fun cacheIfEligible(key: BitmapCacheKey, bitmap: Bitmap, allowCache: Boolean): Boolean {
        if (!allowCache || maxCacheBytes == 0L) return false
        val bytes = bitmap.allocationByteCount.toLong()
        if (bytes > maxCacheBytes) return false

        bitmapCache.remove(key)?.let { previous ->
            cachedBitmapBytes -= previous.allocationByteCount.toLong()
            if (previous !== bitmap && !previous.isRecycled) previous.recycle()
        }
        bitmapCache[key] = bitmap
        cachedBitmapBytes += bytes
        while (cachedBitmapBytes > maxCacheBytes && bitmapCache.isNotEmpty()) {
            val eldest = bitmapCache.entries.iterator().run {
                val entry = next()
                remove()
                entry.value
            }
            cachedBitmapBytes -= eldest.allocationByteCount.toLong()
            if (!eldest.isRecycled) eldest.recycle()
        }
        return true
    }

    /** Releases preview-owned decoded pixels. Export callers use the default zero-byte cache. */
    override fun close() {
        bitmapCache.values.forEach { if (!it.isRecycled) it.recycle() }
        bitmapCache.clear()
        cachedBitmapBytes = 0L
        intrinsicsCache.clear()
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

        const val INTRINSICS_CACHE_CAPACITY = 128

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
