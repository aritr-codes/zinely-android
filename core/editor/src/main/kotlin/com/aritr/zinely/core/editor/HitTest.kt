package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.Element
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.render.SupplyInk
import kotlin.math.abs

/**
 * Pure hit-testing over a document [Page] (ADR-029 §5.4). No matrix inverse is needed because the
 * model is **decomposed**: a touch is mapped into an element's local frame by subtracting the box
 * centre and un-rotating by `-rotationDegrees` (reusing [AffineTransform2D.rotateDeg], so the sign
 * matches `:core:render`'s `SceneRenderer.localToPage` exactly), then AABB-tested against `±w/2, ±h/2`.
 *
 * Iteration order mirrors the renderer's **stable** draw order: the renderer draws by ascending
 * `(zIndex, listIndex)`, so the topmost element is the one with the **greatest** `(zIndex, listIndex)`.
 *
 * ### Six holed pieces are their ink, not their box (ADR-124)
 *
 * For the pieces [SupplyInk] names, the hit area is the drawn ink; every other element keeps its box.
 * [topmostAt] resolves in three passes, each topmost-first, and the first pass that finds an element
 * answers: **drawn**, then **near**, then **box**. Passes 2 and 3 can only ever return an in-scope piece —
 * any other element whose box holds the point was already found in pass 1 — so no text box, photo or
 * ordinary piece gains or loses a tap through them.
 *
 * [SupplyInk.distancePt] is the **only** thing this module takes from `:core:render`.
 */
public object HitTest {

    /**
     * The id of the topmost element under page-local [pt], or `null` if the point hits nothing.
     *
     * [tolerancePt] is how far, in page points, a tap may land off an in-scope piece's ink and still pick
     * it up **when nothing contains the tap exactly**. It is a yes-or-no test per piece: among pieces
     * within it, stacking order decides, never distance. At `0.0`, or if it is not finite, there is no
     * near pass.
     */
    public fun topmostAt(page: Page, pt: PtPoint, tolerancePt: Double = 0.0): String? {
        val topmostFirst = page.elements
            .withIndex()
            // Topmost first: greatest zIndex, then greatest list index (later-drawn wins ties).
            .sortedWith(compareByDescending<IndexedValue<Element>> { it.value.zIndex }.thenByDescending { it.index })
            .map { it.value }

        // 1. Drawn: ink for an in-scope piece, the box for everything else.
        topmostFirst.firstOrNull { el -> inkDistance(el, pt)?.let { it == 0.0 } ?: contains(el, pt) }
            ?.let { return it.id }
        // 2. Near: in-scope pieces only. A null distance is never a candidate. A non-finite tolerance is
        // no tolerance: an infinite one would hand every miss to the topmost holed piece on the page.
        if (tolerancePt > 0.0 && tolerancePt.isFinite()) {
            topmostFirst.firstOrNull { el -> inkDistance(el, pt)?.let { it <= tolerancePt } == true }
                ?.let { return it.id }
        }
        // 3. Box: today's test, which is why a holed piece on blank paper stays selectable through its hole.
        return topmostFirst.firstOrNull { contains(it, pt) }?.id
    }

    /** True if page-local [pt] lies within [element]'s (possibly rotated) box. */
    public fun contains(element: Element, pt: PtPoint): Boolean {
        val t = element.transform
        val local = toLocal(t, pt)
        return abs(local.x) <= t.widthPt / 2.0 && abs(local.y) <= t.heightPt / 2.0
    }

    /** [pt] in the element's own un-rotated frame, origin at the box centre. */
    private fun toLocal(t: Transform, pt: PtPoint): PtPoint {
        val cx = t.xPt + t.widthPt / 2.0
        val cy = t.yPt + t.heightPt / 2.0
        return AffineTransform2D.rotateDeg(-t.rotationDegrees).map(PtPoint(pt.x - cx, pt.y - cy))
    }

    /** Points from [pt] to [element]'s ink; `null` means "this element is hit by its box". */
    private fun inkDistance(element: Element, pt: PtPoint): Double? {
        if (element !is DecorElement) return null
        val t = element.transform
        val local = toLocal(t, pt)
        return SupplyInk.distancePt(
            supplyId = element.supplyId,
            localX = local.x,
            localY = local.y,
            widthPt = t.widthPt,
            heightPt = t.heightPt,
            mirrored = element.mirrored,
            flippedVertically = element.flippedVertically,
        )
    }
}
