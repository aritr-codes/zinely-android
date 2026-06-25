package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.Transform
import kotlin.math.abs

/**
 * Pure transform-baking math (ADR-029 §5.2). A live gesture accumulates an ephemeral
 * `{pan, zoom, rotation}` off the reducer (driven through `graphicsLayer{}`); on commit it is **baked**
 * into the committed decomposed [Transform], from which `:core:render` derives the matrix (no stored
 * matrix, no drift — R9.4). Two anchor modes, matching the design's input-dependent anchors (§5.3).
 */
public object TransformMath {

    /**
     * Centre-anchored bake — the **pinch** and a11y `ScaleBy`/`RotateBy`/`Nudge` path. Uniform scale and
     * rotation pivot on the element centre (which therefore stays fixed under scale/rotate); [panPt] is a
     * page-space translation of that centre. Uniform scale and centre-rotation commute, so this is correct
     * for any starting `rotationDegrees`.
     */
    public fun bakeCentreAnchored(
        before: Transform,
        panPt: PtPoint,
        zoom: Double,
        rotationDeltaDeg: Double,
    ): Transform {
        val cx = before.xPt + before.widthPt / 2.0 + panPt.x
        val cy = before.yPt + before.heightPt / 2.0 + panPt.y
        val w = before.widthPt * zoom
        val h = before.heightPt * zoom
        return Transform(
            xPt = cx - w / 2.0,
            yPt = cy - h / 2.0,
            widthPt = w,
            heightPt = h,
            rotationDegrees = before.rotationDegrees + rotationDeltaDeg,
        )
    }

    /**
     * Anchored handle-resize bake (§5.2). A corner/edge handle drag holds its **opposite anchor** fixed in
     * **page space** while the box resizes; on a rotated element the centre therefore moves. Rotation is
     * unchanged by a resize.
     *
     * [anchorLocal] / [movingLocal] are the fixed and dragged corners/edges in the element-local unit frame
     * `(±1, ±1)` — e.g. anchor `(-1,-1)` top-left fixed, moving `(+1,+1)` bottom-right dragged (corner); or
     * anchor `(-1,0)` left edge, moving `(+1,0)` right edge (1-axis). [dragPagePt] is the moving handle's new
     * page-space position. Only axes that actually move (`movingLocal != anchorLocal` on that axis) resize.
     */
    public fun bakeHandleResize(
        before: Transform,
        anchorLocal: PtPoint,
        movingLocal: PtPoint,
        dragPagePt: PtPoint,
    ): Transform {
        val rot = before.rotationDegrees
        val toPage = AffineTransform2D.rotateDeg(rot)
        val toLocal = AffineTransform2D.rotateDeg(-rot)

        // The fixed anchor's page-space position (invariant through the resize).
        val c0x = before.xPt + before.widthPt / 2.0
        val c0y = before.yPt + before.heightPt / 2.0
        val anchorOffset0 = toPage.map(PtPoint(anchorLocal.x * before.widthPt / 2.0, anchorLocal.y * before.heightPt / 2.0))
        val anchorPage = PtPoint(c0x + anchorOffset0.x, c0y + anchorOffset0.y)

        // New half-extents from the drag, expressed in the element's local axes.
        val dragLocal = toLocal.map(PtPoint(dragPagePt.x - anchorPage.x, dragPagePt.y - anchorPage.y))
        val w = if (movingLocal.x != anchorLocal.x) abs(dragLocal.x) else before.widthPt
        val h = if (movingLocal.y != anchorLocal.y) abs(dragLocal.y) else before.heightPt

        // Solve the new centre so the anchor stays at anchorPage.
        val anchorOffset1 = toPage.map(PtPoint(anchorLocal.x * w / 2.0, anchorLocal.y * h / 2.0))
        val cx = anchorPage.x - anchorOffset1.x
        val cy = anchorPage.y - anchorOffset1.y
        return Transform(
            xPt = cx - w / 2.0,
            yPt = cy - h / 2.0,
            widthPt = w,
            heightPt = h,
            rotationDegrees = rot,
        )
    }
}
