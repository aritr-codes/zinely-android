package com.aritr.zinely.core.render

import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.Fit
import com.aritr.zinely.core.model.PtRect

/**
 * The resolved image blit: which fraction of the source to sample and where it lands.
 *
 * - [srcFraction] — a rect in **normalised 0..1 image space** (of the *full* image) to sample.
 * - [destRect] — where it draws in **element-local points** (within the box `(0,0,w,h)`).
 */
public data class ImageBlit(val srcFraction: PtRect, val destRect: PtRect)

/**
 * The shared, pure fit/crop math for images (seam A, ADR-027). Lives in `:core:render` and is
 * invoked by **both** Android backends with the intrinsic pixel size each reads from its own
 * `inJustDecodeBounds` of the canonical import-master bytes (the single source of truth) — so the
 * geometry cannot diverge between preview and export.
 *
 * The aspect that matters is the **cropped** source, not the raw image: with crop fractions
 * `cw = right-left`, `ch = bottom-top`, the effective source aspect is `(cw·iw)/(ch·ih)`.
 *
 * - [Fit.FIT] letterboxes the cropped region inside the box (sampling exactly the crop).
 * - [Fit.FILL] covers the box, sampling a centred sub-rect of the crop whose aspect matches the box.
 */
public fun computeImageBlit(
    intrinsicWidthPx: Int,
    intrinsicHeightPx: Int,
    crop: Crop,
    fit: Fit,
    boxWidthPt: Double,
    boxHeightPt: Double,
): ImageBlit {
    require(intrinsicWidthPx > 0 && intrinsicHeightPx > 0) {
        "intrinsic dimensions must be > 0, were ${intrinsicWidthPx}x$intrinsicHeightPx"
    }
    require(boxWidthPt > 0.0 && boxHeightPt > 0.0 && boxWidthPt.isFinite() && boxHeightPt.isFinite()) {
        "box dimensions must be finite and > 0, were ${boxWidthPt}x$boxHeightPt"
    }
    // computeImageBlit is public API; fail fast on an unvalidated crop (the document validator
    // normally enforces this) — a zero/negative extent would otherwise divide by zero.
    require(crop.left >= 0.0 && crop.right <= 1.0 && crop.left < crop.right) {
        "crop horizontal range must satisfy 0 <= left < right <= 1, was [${crop.left}, ${crop.right}]"
    }
    require(crop.top >= 0.0 && crop.bottom <= 1.0 && crop.top < crop.bottom) {
        "crop vertical range must satisfy 0 <= top < bottom <= 1, was [${crop.top}, ${crop.bottom}]"
    }

    val iw = intrinsicWidthPx.toDouble()
    val ih = intrinsicHeightPx.toDouble()
    val cw = crop.right - crop.left          // crop width as a fraction of the full image
    val ch = crop.bottom - crop.top          // crop height as a fraction of the full image

    val croppedAspect = (cw * iw) / (ch * ih)
    val boxAspect = boxWidthPt / boxHeightPt

    return when (fit) {
        Fit.FIT -> {
            // Sample exactly the crop; scale to fit inside the box, preserving aspect, centred.
            val src = PtRect(crop.left, crop.top, cw, ch)
            val dest = if (croppedAspect > boxAspect) {
                val destH = boxWidthPt / croppedAspect
                PtRect(0.0, (boxHeightPt - destH) / 2.0, boxWidthPt, destH)
            } else {
                val destW = boxHeightPt * croppedAspect
                PtRect((boxWidthPt - destW) / 2.0, 0.0, destW, boxHeightPt)
            }
            ImageBlit(srcFraction = src, destRect = dest)
        }
        Fit.FILL -> {
            // Cover the whole box; sample a centred sub-rect of the crop with the box's aspect.
            val dest = PtRect(0.0, 0.0, boxWidthPt, boxHeightPt)
            val src = if (croppedAspect > boxAspect) {
                // Crop too wide → keep crop height, narrow the width, centre horizontally.
                val sfw = boxAspect * ch * ih / iw
                PtRect(crop.left + (cw - sfw) / 2.0, crop.top, sfw, ch)
            } else {
                // Crop too tall → keep crop width, shorten the height, centre vertically.
                val sfh = cw * iw / (boxAspect * ih)
                PtRect(crop.left, crop.top + (ch - sfh) / 2.0, cw, sfh)
            }
            ImageBlit(srcFraction = src, destRect = dest)
        }
    }
}
