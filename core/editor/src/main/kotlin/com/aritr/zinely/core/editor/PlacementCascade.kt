package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform

/**
 * Offset item [index] in a placement sequence of [count] from [base], bounded by [pageSizePt].
 *
 * The first item remains at [base]. Later items step diagonally by [CASCADE_STEP_PT], capped at
 * `room / (count - 1)` on each axis so the final item remains fully on the page. This is deliberately
 * separate from default-placement geometry: callers decide whether an insertion should cascade.
 */
public fun cascadedPlacement(base: Transform, index: Int, count: Int, pageSizePt: PtSize): Transform {
    if (index <= 0) return base
    val spans = (count - 1).coerceAtLeast(1)
    val roomX = (pageSizePt.width - base.widthPt - base.xPt).coerceAtLeast(0.0)
    val roomY = (pageSizePt.height - base.heightPt - base.yPt).coerceAtLeast(0.0)
    return base.copy(
        xPt = base.xPt + minOf(CASCADE_STEP_PT, roomX / spans) * index,
        yPt = base.yPt + minOf(CASCADE_STEP_PT, roomY / spans) * index,
    )
}

/** ~4 mm at 72 dpi: visibly a second sheet, not a misalignment. */
private const val CASCADE_STEP_PT = 12.0
