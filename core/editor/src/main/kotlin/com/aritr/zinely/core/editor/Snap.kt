package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import kotlin.math.abs

/** A render-only alignment guide: a `VERTICAL` line at a constant x, or `HORIZONTAL` at a constant y. */
public enum class SnapAxis { VERTICAL, HORIZONTAL }

/** One active snap line (drawn during a drag, never stored in history). */
public data class SnapGuide(val axis: SnapAxis, val positionPt: Double)

/** The snapped box plus the guides that fired. */
public data class SnapResult(val adjusted: PtRect, val guides: List<SnapGuide>)

/**
 * Pure snapping (ADR-029 §5.4). Candidate lines come from the page edges/centre and every other element's
 * edges/centres; the moving box's nearest anchor (left/centre/right, top/centre/bottom) snaps to the
 * closest candidate within [PtSize]-space [thresholdPt] (the caller passes `≈ 8px / screenPxPerPt`).
 * Each axis snaps independently. Guides are render-only.
 */
public object Snap {

    public fun snap(moving: PtRect, others: List<PtRect>, pageSize: PtSize, thresholdPt: Double): SnapResult {
        val xLines = buildList {
            add(0.0); add(pageSize.width / 2.0); add(pageSize.width)
            for (o in others) { add(o.x); add(o.centerX); add(o.right) }
        }
        val yLines = buildList {
            add(0.0); add(pageSize.height / 2.0); add(pageSize.height)
            for (o in others) { add(o.y); add(o.centerY); add(o.bottom) }
        }
        val dx = bestSnap(listOf(moving.x, moving.centerX, moving.right), xLines, thresholdPt)
        val dy = bestSnap(listOf(moving.y, moving.centerY, moving.bottom), yLines, thresholdPt)

        val guides = buildList {
            dx?.let { add(SnapGuide(SnapAxis.VERTICAL, it.line)) }
            dy?.let { add(SnapGuide(SnapAxis.HORIZONTAL, it.line)) }
        }
        return SnapResult(
            adjusted = moving.copy(x = moving.x + (dx?.delta ?: 0.0), y = moving.y + (dy?.delta ?: 0.0)),
            guides = guides,
        )
    }

    private data class Hit(val delta: Double, val line: Double)

    /** The smallest within-threshold shift aligning any [anchors] entry to any [lines] entry, or null. */
    private fun bestSnap(anchors: List<Double>, lines: List<Double>, threshold: Double): Hit? {
        var best: Hit? = null
        for (a in anchors) for (l in lines) {
            val d = l - a
            if (abs(d) <= threshold && (best == null || abs(d) < abs(best.delta))) best = Hit(d, l)
        }
        return best
    }
}
