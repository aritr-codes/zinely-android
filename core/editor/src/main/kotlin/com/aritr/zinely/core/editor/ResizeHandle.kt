package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.Transform

/**
 * The eight selection resize handles (ADR-029 §5.3), each identified by its position in the element's
 * **local unit frame** `(±1, ±1)` — corners at `(±1, ±1)`, edge midpoints at `(±1, 0)` / `(0, ±1)`.
 * The frame is pre-rotation (element-local), so a handle keeps its identity as the box rotates.
 *
 * A handle drag holds the **opposite** handle fixed in page space ([opposite]); corners resize both axes,
 * edges resize one (the axis the handle's local coordinate is non-zero on). This is the input-dependent
 * anchor of §5.3 — distinct from the centre-anchored pinch/`ScaleBy`.
 */
public enum class ResizeHandle(public val local: PtPoint) {
    TOP_LEFT(PtPoint(-1.0, -1.0)),
    TOP(PtPoint(0.0, -1.0)),
    TOP_RIGHT(PtPoint(1.0, -1.0)),
    LEFT(PtPoint(-1.0, 0.0)),
    RIGHT(PtPoint(1.0, 0.0)),
    BOTTOM_LEFT(PtPoint(-1.0, 1.0)),
    BOTTOM(PtPoint(0.0, 1.0)),
    BOTTOM_RIGHT(PtPoint(1.0, 1.0));

    /**
     * The anchor handle held fixed during a drag of this one — the mirror through the box centre.
     * Written as `0.0 - v` (not unary minus) so a `0` axis stays `+0.0`, never `-0.0` (which would break
     * `PtPoint` value equality without changing the IEEE arithmetic).
     */
    public val opposite: PtPoint get() = PtPoint(0.0 - local.x, 0.0 - local.y)
}

/**
 * Bake a handle drag (ADR-029 §5.2/§5.3): resize [before] so [handle] tracks [dragPagePt] while the
 * opposite handle stays fixed in page space. A thin, total adapter over [TransformMath.bakeHandleResize]
 * that wires the handle's [ResizeHandle.local]/[ResizeHandle.opposite] frame — kept pure so the gesture
 * layer only decodes pointer events into a [dragPagePt].
 */
public fun TransformMath.resizeByHandle(before: Transform, handle: ResizeHandle, dragPagePt: PtPoint): Transform =
    bakeHandleResize(
        before = before,
        anchorLocal = handle.opposite,
        movingLocal = handle.local,
        dragPagePt = dragPagePt,
    )
