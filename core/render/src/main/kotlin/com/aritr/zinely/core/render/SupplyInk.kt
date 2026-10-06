package com.aritr.zinely.core.render

import com.aritr.zinely.core.model.PtPoint
import kotlin.math.hypot

/**
 * Where a supply's **ink** is, as a pure question about the outline the renderer draws
 * ([ADR-124](../../../../../../../../docs/DECISIONS.md#adr-124)).
 *
 * The geometry is [SupplyCatalog.outlineOf] and nothing else: no mask, no bitmap, no second table of
 * hit shapes. A drawn hole and a hit hole can therefore disagree only if the arithmetic here is wrong,
 * and `SupplyInkTest` pins it against `SceneRenderer`'s own emitted fold.
 */
public object SupplyInk {

    /**
     * The pieces whose hit area is their ink rather than their box. **A list, not a rule**: a piece is in
     * scope because it is named here in a reviewed change, never because its outline happens to have a
     * second subpath. `mark.crop` is out by the owner's ruling (decision gate Q3).
     */
    public val IN_SCOPE: Set<String> = setOf(
        "paper.window",
        "paper.hole",
        "shape.ring",
        "fix.grommet",
        "fix.corner",
        "mark.registration",
    )

    /**
     * The distance in **points** from a point to [supplyId]'s ink, or `null` when the caller must use the
     * element's box instead.
     *
     * ([localX], [localY]) is in the element's own un-rotated frame, origin at the box centre — what is
     * left after subtracting the centre and un-rotating. This applies the exact inverse of the rest of the
     * render fold (`scale(w, h) · mirrorX? · mirrorY?`, `SceneRenderer.unitSquareFold`).
     *
     * - `0.0` — the point is on the ink (inside it under even-odd, or exactly on its boundary).
     * - positive — the shortest distance to the flattened boundary, measured after scaling the chords by
     *   ([widthPt], [heightPt]), so it stays in points on a piece stretched to a non-square box.
     * - `null` — [supplyId] is not in [IN_SCOPE], has no outline in this build, or the box has a zero,
     *   negative or non-finite side.
     */
    public fun distancePt(
        supplyId: String,
        localX: Double,
        localY: Double,
        widthPt: Double,
        heightPt: Double,
        mirrored: Boolean,
        flippedVertically: Boolean,
    ): Double? {
        if (supplyId !in IN_SCOPE) return null
        if (!(widthPt > 0.0 && heightPt > 0.0 && widthPt.isFinite() && heightPt.isFinite())) return null
        val polygons = SupplyCatalog.outlineOf(supplyId)?.polygons() ?: return null

        val u = ((localX + widthPt / 2.0) / widthPt).let { if (mirrored) 1.0 - it else it }
        val v = ((localY + heightPt / 2.0) / heightPt).let { if (flippedVertically) 1.0 - it else it }
        if (isInked(polygons, u, v)) return 0.0

        // Mirroring is an isometry of the scaled box, so measuring from the mirrored point to the authored
        // chords is the same distance as from the real point to the mirrored ink.
        val px = u * widthPt
        val py = v * heightPt
        var nearest = Double.POSITIVE_INFINITY
        for (poly in polygons) {
            for (i in poly.indices) {
                val a = poly[i]
                val b = poly[(i + 1) % poly.size] // the closing edge is an edge
                val d = segmentDistance(px, py, a.x * widthPt, a.y * heightPt, b.x * widthPt, b.y * heightPt)
                if (d < nearest) nearest = d
            }
        }
        return nearest
    }

    private fun segmentDistance(px: Double, py: Double, ax: Double, ay: Double, bx: Double, by: Double): Double {
        val dx = bx - ax
        val dy = by - ay
        val lengthSquared = dx * dx + dy * dy
        val t = if (lengthSquared == 0.0) 0.0 else (((px - ax) * dx + (py - ay) * dy) / lengthSquared).coerceIn(0.0, 1.0)
        return hypot(px - (ax + t * dx), py - (ay + t * dy))
    }
}

/** Cubic flattening steps. 64 keeps the worst chord error well under a probe's clearance. */
internal const val FLATTEN: Int = 64

/** Every subpath as a closed polygon, cubics flattened. Closure back to `start` is implicit. */
internal fun SupplyOutline.polygons(): List<List<PtPoint>> = subpaths.map { sub ->
    val pts = mutableListOf(sub.start)
    var from = sub.start
    for (seg in sub.segments) {
        when (seg) {
            is Segment.LineTo -> pts += seg.to
            is Segment.CubicTo -> {
                for (i in 1..FLATTEN) {
                    val t = i.toDouble() / FLATTEN
                    val u = 1.0 - t
                    pts += PtPoint(
                        u * u * u * from.x + 3 * u * u * t * seg.c1.x + 3 * u * t * t * seg.c2.x + t * t * t * seg.to.x,
                        u * u * u * from.y + 3 * u * u * t * seg.c1.y + 3 * u * t * t * seg.c2.y + t * t * t * seg.to.y,
                    )
                }
            }
        }
        from = seg.to
    }
    pts
}

/**
 * Is ([x], [y]) inked, under the **even-odd** rule SUPPLIES-SPEC §4.1 rule 3 specifies?
 *
 * A horizontal ray cast to `+x`, counting crossings across every subpath at once — which is what makes
 * this even-odd rather than per-subpath containment, and therefore the only version of the question
 * that can catch a cancelling overlap.
 */
internal fun SupplyOutline.isInked(x: Double, y: Double): Boolean = isInked(polygons(), x, y)

private fun isInked(polygons: List<List<PtPoint>>, x: Double, y: Double): Boolean {
    var crossings = 0
    for (poly in polygons) {
        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[(i + 1) % poly.size]
            if ((a.y <= y && b.y > y) || (b.y <= y && a.y > y)) {
                val t = (y - a.y) / (b.y - a.y)
                if (a.x + t * (b.x - a.x) > x) crossings++
            }
        }
    }
    return crossings % 2 == 1
}
