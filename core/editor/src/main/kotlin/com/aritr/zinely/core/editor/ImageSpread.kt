package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.Crop

/** One fixed reading-order mini-zine pair. [sourceIsLeft] is page-local, independent of sheet rotation. */
internal data class ImageSpreadPair(val partnerPageIndex: Int, val sourceIsLeft: Boolean)

/**
 * The four physical pairs for an eight-page one-sheet zine (ADR-109). This is deliberately a table:
 * sheet-adjacent 3|4 and 7|8 are opposite sides of a leaf, not spreads.
 */
internal fun imageSpreadPair(pageIndex: Int): ImageSpreadPair? = when (pageIndex) {
    0 -> ImageSpreadPair(partnerPageIndex = 7, sourceIsLeft = false) // 8 | 1 outside wrap
    1 -> ImageSpreadPair(partnerPageIndex = 2, sourceIsLeft = true)  // 2 | 3
    2 -> ImageSpreadPair(partnerPageIndex = 1, sourceIsLeft = false)
    3 -> ImageSpreadPair(partnerPageIndex = 4, sourceIsLeft = true)  // 4 | 5
    4 -> ImageSpreadPair(partnerPageIndex = 3, sourceIsLeft = false)
    5 -> ImageSpreadPair(partnerPageIndex = 6, sourceIsLeft = true)  // 6 | 7
    6 -> ImageSpreadPair(partnerPageIndex = 5, sourceIsLeft = false)
    7 -> ImageSpreadPair(partnerPageIndex = 0, sourceIsLeft = true)
    else -> null
}

/**
 * Complementary crops for a centred cover crop at twice the page width. The pair tiles at exactly
 * `x = 0.5` in source-image coordinates and each half has the page aspect, so `Fit.FIT` fills a full
 * page without letterboxing.
 */
internal fun imageSpreadCrops(photoAspect: Double, pageAspect: Double): Pair<Crop, Crop>? {
    if (!photoAspect.isFinite() || photoAspect <= 0.0 || !pageAspect.isFinite() || pageAspect <= 0.0) {
        return null
    }
    val spreadAspect = pageAspect * 2.0
    val cropWidth: Double
    val cropHeight: Double
    if (photoAspect >= spreadAspect) {
        cropWidth = spreadAspect / photoAspect
        cropHeight = 1.0
    } else {
        cropWidth = 1.0
        cropHeight = photoAspect / spreadAspect
    }
    val left = 0.5 - cropWidth / 2.0
    val top = 0.5 - cropHeight / 2.0
    val right = 0.5 + cropWidth / 2.0
    val bottom = 0.5 + cropHeight / 2.0
    return Crop(left, top, 0.5, bottom) to Crop(0.5, top, right, bottom)
}
