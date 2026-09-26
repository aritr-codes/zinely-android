package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.Element
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * The reading order of a page's elements for a screen reader: the canvas clause of
 * [ZINELY-DESIGN-SYSTEM §4.5](../../../../../../../../../../docs/ZINELY-DESIGN-SYSTEM.md#45-reading-order),
 * recorded as [ADR-119](../../../../../../../../../../docs/DECISIONS.md#adr-119). The rule and its threshold are
 * stated there, once; this file implements them and does not restate why.
 *
 * Independent of `zIndex` and of list order by construction: neither is read. Restacking and Make-spread
 * change only those, so they cannot move a node.
 *
 * **Invariant — a procedure, not a `Comparator`.** "Shares a row with" is not transitive, so a pairwise
 * comparator built on it breaks `sortedWith`'s contract (and can throw). Rows are formed first; only then
 * is anything sorted, and every sort below is by plain numbers plus the id.
 */
public object SpatialOrder {

    /** The §4.5 large-element threshold, as a fraction of the page height. */
    public const val LARGE_ELEMENT_HEIGHT_FRACTION: Double = 0.6

    /**
     * [elements] in reading order.
     *
     * @param pageHeightPt the page's height in points. Pass [Double.POSITIVE_INFINITY] when it is unknown:
     *   no element is then "large", and the order is plain rows.
     */
    public fun order(elements: List<Element>, pageHeightPt: Double): List<Element> {
        val boxes = elements.map { Box(it) }
        val limit = LARGE_ELEMENT_HEIGHT_FRACTION * pageHeightPt
        val (large, rest) = boxes.partition { it.height > limit }

        val result = large.sortedWith(byTopLeftId).mapTo(ArrayList(elements.size)) { it.element }
        val unassigned = rest.sortedWith(byTopLeftId).toMutableList()
        while (unassigned.isNotEmpty()) {
            val opener = unassigned.first()
            val row = unassigned.filter { it === opener || it.centerY >= opener.top && it.centerY <= opener.bottom }
            unassigned.removeAll(row)
            row.sortedWith(byLeftId).mapTo(result) { it.element }
        }
        return result
    }

    /** An element's rotated, axis-aligned bounds in page points (rotation is about the box centre). */
    private class Box(val element: Element) {
        val top: Double
        val bottom: Double
        val left: Double
        val centerY: Double
        val height: Double get() = bottom - top

        init {
            val t = element.transform
            val rad = t.rotationDegrees * PI / 180.0
            val c = abs(cos(rad))
            val s = abs(sin(rad))
            val halfW = (t.widthPt * c + t.heightPt * s) / 2.0
            val halfH = (t.widthPt * s + t.heightPt * c) / 2.0
            centerY = t.yPt + t.heightPt / 2.0
            top = centerY - halfH
            bottom = centerY + halfH
            left = t.xPt + t.widthPt / 2.0 - halfW
        }
    }

    private val byTopLeftId = compareBy<Box>({ it.top }, { it.left }, { it.element.id })
    private val byLeftId = compareBy<Box>({ it.left }, { it.element.id })
}
