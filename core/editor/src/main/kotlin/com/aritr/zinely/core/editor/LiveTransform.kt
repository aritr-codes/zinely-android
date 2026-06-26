package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.Transform

/**
 * The off-reducer accumulator for one live transform gesture (ADR-029 §5.1/§5.2). A
 * `detectTransformGestures` loop folds each frame's incremental `{pan(px), zoomFactor, rotation}` into
 * this single ephemeral value via [accumulate]; the Compose layer reads it straight into
 * `graphicsLayer { translationX/Y = panXpx/panYpx; scaleX/Y = clampedZoom; rotationZ = rotationDeg }`
 * so live frames hit the **draw phase only** and never reach the reducer ([R5.3] coalescing — one
 * gesture is one undo step). On gesture end [bake] folds the accumulated delta into the committed
 * page-space [Transform] the reducer stores.
 *
 * Pan is carried in **pixels** (the unit `graphicsLayer.translation` and `PointerEvent.calculatePan`
 * both speak) and converted to page points only at [bake], using `screenPxPerPt`. Zoom is the running
 * product of per-frame factors; rotation the running sum of per-frame degrees.
 *
 * **F2 live min-size clamp.** A pinch can drive `zoom → 0`. [bake] is safe (it delegates to
 * [TransformMath.bakeCentreAnchored], which floors size at [TransformMath.MIN_SIZE_PT]), but the live
 * `graphicsLayer` scale would still collapse the element to an invisible, un-grabbable sliver mid-gesture.
 * [clampedZoom] floors the *preview* scale at the same point-size floor, so what the user sees during the
 * gesture matches what [bake] commits.
 */
public data class LiveTransform(
    val panXpx: Double = 0.0,
    val panYpx: Double = 0.0,
    val zoom: Double = 1.0,
    val rotationDeg: Double = 0.0,
) {

    /** Fold one gesture frame in: pan adds (px), zoom multiplies, rotation adds (degrees). */
    public fun accumulate(
        panXpx: Double,
        panYpx: Double,
        zoomFactor: Double,
        rotationDelta: Double,
    ): LiveTransform = LiveTransform(
        panXpx = this.panXpx + panXpx,
        panYpx = this.panYpx + panYpx,
        zoom = this.zoom * zoomFactor,
        rotationDeg = this.rotationDeg + rotationDelta,
    )

    /**
     * The live `graphicsLayer` scale, floored so neither axis of [before] shrinks below
     * [TransformMath.MIN_SIZE_PT] (F2). Only shrinking gestures are affected; enlarging passes through.
     */
    public fun clampedZoom(before: Transform): Double {
        val minZoom = maxOf(
            TransformMath.MIN_SIZE_PT / before.widthPt,
            TransformMath.MIN_SIZE_PT / before.heightPt,
        )
        return zoom.coerceAtLeast(minZoom)
    }

    /**
     * Bake the accumulated delta into the committed [Transform] (§5.2, centre-anchored). Pixel pan is
     * converted to page points via [screenPxPerPt]; the clamped zoom keeps commit == last previewed frame.
     */
    public fun bake(before: Transform, screenPxPerPt: Double): Transform =
        TransformMath.bakeCentreAnchored(
            before = before,
            panPt = PtPoint(panXpx / screenPxPerPt, panYpx / screenPxPerPt),
            zoom = clampedZoom(before),
            rotationDeltaDeg = rotationDeg,
        )
}
