package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import kotlin.math.abs

/** The baked-and-snapped transforms for a live gesture frame plus the render-only guides that fired. */
public data class LiveSnapResult(
    val transforms: Map<String, Transform>,
    val guides: List<SnapGuide>,
)

/**
 * The single source of truth that turns an open gesture into the frame the editor both **previews** and
 * **commits** (ADR-029 §5.4). It bakes each selected member's snapshot through [LiveTransform.bake] (the
 * §5.2 centre-anchored bake), then — for the single-select, non-rotated case — snaps the moving box to the
 * page/other-element candidate lines via the pure [Snap]. Because the preview layer ([EditorPagePreview])
 * and the gesture commit ([Modifier.editorTransformGestures]) call this with identical inputs, the
 * previewed frame equals the committed transform by construction (preview == commit), and the snap is
 * baked into the commit, never just shown ([§5.4](../../../../../../../docs/spikes/s4-editor-mvi.md)).
 *
 * **F1 — skip rotated.** Snapping a rotated element would align its *pre-rotation* AABB edges, which no
 * longer touch the visible box; so when the baked transform is rotated (`rotationDegrees != 0`) the box is
 * baked but never snapped and no guide fires. **Multi-select** (§5.5, post-MVP) likewise bakes without
 * snapping — group-bbox snapping is the additive extension. Both degrade to plain bake, not to nothing.
 */
public object LiveSnap {

    /** The snap pull radius in device px (§5.4); `8px` ≈ a fingertip's worth of slack at 1× preview. */
    public const val SNAP_THRESHOLD_PX: Double = 8.0

    /** Below this, a rotation reads as axis-aligned — float bake noise must not defeat F1 (Codex rec). */
    private const val ROTATION_EPSILON_DEG: Double = 1e-6

    /**
     * The zoom-adjusted threshold in **points** the preview and commit must share (§5.4): `8px` converted
     * through the current preview scale. A non-positive/non-finite [screenPxPerPt] yields a non-finite
     * threshold, which [Snap.snap] treats as "no snapping".
     */
    public fun thresholdPt(screenPxPerPt: Double): Double = SNAP_THRESHOLD_PX / screenPxPerPt

    public fun resolve(
        page: Page,
        selection: Set<String>,
        before: Map<String, Transform>,
        live: LiveTransform,
        screenPxPerPt: Double,
        pageSize: PtSize,
        thresholdPt: Double,
    ): LiveSnapResult {
        // A non-finite/non-positive scale has no sensible preview; baking would divide by zero / spread NaN
        // into the document (Codex rec #6). Degrade to "no movement, no snap" — never a poisoned commit.
        if (!screenPxPerPt.isFinite() || screenPxPerPt <= 0.0) return LiveSnapResult(before, emptyList())

        val baked = before.mapValues { (_, t) -> live.bake(t, screenPxPerPt) }
        // MVP selects one (§5.5); F1 skips rotated. Either way: bake, don't snap.
        val id = selection.singleOrNull() ?: return LiveSnapResult(baked, emptyList())
        val t = baked[id] ?: return LiveSnapResult(baked, emptyList())
        if (abs(t.rotationDegrees) > ROTATION_EPSILON_DEG) return LiveSnapResult(baked, emptyList())

        val moving = PtRect(t.xPt, t.yPt, t.widthPt, t.heightPt)
        // Candidate lines come from un-rotated neighbours only: a rotated element's stored x/y/w/h is its
        // pre-rotation box, whose edges no longer touch the rendered shape — snapping to them aligns to an
        // invisible line (Codex rec #3). Exclude both the selection and any rotated neighbour.
        val others = page.elements
            .filter { it.id !in selection && abs(it.transform.rotationDegrees) <= ROTATION_EPSILON_DEG }
            .map { it.transform.let { o -> PtRect(o.xPt, o.yPt, o.widthPt, o.heightPt) } }
        val snap = Snap.snap(moving, others, pageSize, thresholdPt)
        val snapped = t.copy(xPt = snap.adjusted.x, yPt = snap.adjusted.y)
        // Keep every baked member; override only the snapped one (robust if `before` ever carries extras).
        return LiveSnapResult(baked + (id to snapped), snap.guides)
    }
}
