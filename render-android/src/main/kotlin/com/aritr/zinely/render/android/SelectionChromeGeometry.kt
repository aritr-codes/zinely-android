package com.aritr.zinely.render.android

import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.Transform

/**
 * Pure selection-chrome geometry (ADR-029 §5, S4 selection-chrome increment). Maps a committed
 * (possibly rotated) element [Transform] into the **device-pixel** corner points the Compose chrome
 * strokes, using the **same** preview seam ([ExportScale.previewPageToDevice]) and the **same**
 * rotation sign as [com.aritr.zinely.core.render.SceneRenderer]'s `localToPage` — so the outline sits
 * exactly on the rendered box at every rotation (no independent geometry, no drift).
 *
 * Why here and not in `:feature:editor`: this is pure point math over [ExportScale]; keeping it in
 * `:render-android` lets it be unit-tested on the JVM (no Compose/Robolectric) and keeps the Compose
 * `SelectionChrome` a thin draw-only wrapper. During a live transform the caller passes the **live-baked**
 * `Transform` (the gesture preview already bakes through [com.aritr.zinely.core.editor.LiveTransform]),
 * so the chrome tracks the finger in screen space with a constant stroke — never scaled inside a
 * `graphicsLayer` (Codex review, selection-chrome increment).
 */
public object SelectionChromeGeometry {

    /**
     * The four device-px corners of [t]'s box, clockwise from the rotated **top-left**:
     * `[TL, TR, BR, BL]`. Local corners `(±w/2, ±h/2)` are rotated clockwise about the box centre by
     * `rotationDegrees` (matching `SceneRenderer.localToPage`), translated to the page-space centre,
     * then mapped to device px by [ExportScale.previewPageToDevice].
     */
    public fun outlineDevicePx(t: Transform, screenPxPerPt: Double, pageOffset: PtPoint): List<PtPoint> {
        val hw = t.widthPt / 2.0
        val hh = t.heightPt / 2.0
        // Clockwise from top-left in element-local axes (+x right, +y down).
        val localCorners = listOf(
            PtPoint(-hw, -hh), // TL
            PtPoint(hw, -hh),  // TR
            PtPoint(hw, hh),   // BR
            PtPoint(-hw, hh),  // BL
        )
        val mapLocal = localMapper(t, screenPxPerPt, pageOffset)
        return localCorners.map(mapLocal)
    }

    /**
     * The device-px position of one resize handle, given its **local unit-frame** coordinate `(±1, ±1)`
     * (see `ResizeHandle.local` in `:core:editor`): scaled to the half-extents `(lx·w/2, ly·h/2)`, rotated
     * about the centre by `+rot`, then mapped to device px — so the handle sits on the rendered box edge.
     */
    public fun handleDevicePx(t: Transform, local: PtPoint, screenPxPerPt: Double, pageOffset: PtPoint): PtPoint =
        localMapper(t, screenPxPerPt, pageOffset)(PtPoint(local.x * t.widthPt / 2.0, local.y * t.heightPt / 2.0))

    /** Maps an element-local offset from the box centre to device px (rotate about centre → +centre → device). */
    private fun localMapper(t: Transform, screenPxPerPt: Double, pageOffset: PtPoint): (PtPoint) -> PtPoint {
        val cx = t.xPt + t.widthPt / 2.0
        val cy = t.yPt + t.heightPt / 2.0
        val rotate = AffineTransform2D.rotateDeg(t.rotationDegrees)
        val toDevice = ExportScale.previewPageToDevice(screenPxPerPt, pageOffset)
        return { localOffset ->
            val rotated = rotate.map(localOffset)
            toDevice.map(PtPoint(cx + rotated.x, cy + rotated.y))
        }
    }
}
