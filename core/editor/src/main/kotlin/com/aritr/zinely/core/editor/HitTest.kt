package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.Element
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PtPoint
import kotlin.math.abs

/**
 * Pure hit-testing over a document [Page] (ADR-029 §5.4). No matrix inverse is needed because the
 * model is **decomposed**: a touch is mapped into an element's local frame by subtracting the box
 * centre and un-rotating by `-rotationDegrees` (reusing [AffineTransform2D.rotateDeg], so the sign
 * matches `:core:render`'s `SceneRenderer.localToPage` exactly), then AABB-tested against `±w/2, ±h/2`.
 *
 * Iteration order mirrors the renderer's **stable** draw order: the renderer draws by ascending
 * `(zIndex, listIndex)`, so the topmost element is the one with the **greatest** `(zIndex, listIndex)`.
 */
public object HitTest {

    /** The id of the topmost element under page-local [pt], or `null` if the point hits nothing. */
    public fun topmostAt(page: Page, pt: PtPoint): String? =
        page.elements
            .withIndex()
            // Topmost first: greatest zIndex, then greatest list index (later-drawn wins ties).
            .sortedWith(compareByDescending<IndexedValue<Element>> { it.value.zIndex }.thenByDescending { it.index })
            .firstOrNull { contains(it.value, pt) }
            ?.value?.id

    /** True if page-local [pt] lies within [element]'s (possibly rotated) box. */
    public fun contains(element: Element, pt: PtPoint): Boolean {
        val t = element.transform
        val cx = t.xPt + t.widthPt / 2.0
        val cy = t.yPt + t.heightPt / 2.0
        val local = AffineTransform2D.rotateDeg(-t.rotationDegrees).map(PtPoint(pt.x - cx, pt.y - cy))
        return abs(local.x) <= t.widthPt / 2.0 && abs(local.y) <= t.heightPt / 2.0
    }
}
