package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.Fit
import kotlin.math.abs

/**
 * Pure framing helpers (ADR-053). This is the **model-space** safety clamp: it guarantees any crop the
 * reducer persists satisfies the invariant `computeImageBlit` ([ADR-027](../render)) requires —
 * `0 <= left < right <= 1`, `0 <= top < bottom <= 1` — so a reframe commit can never store a crop that
 * would crash the renderer at draw.
 *
 * It is deliberately NOT the screen-space offset-percent/zoom basis: mapping a gesture's pan% + zoom to a
 * crop rectangle is HTML-frozen design + Milestone IF2 feature work (ADR-053 non-goals). This helper only
 * makes the *result* renderable, wherever the mapping came from.
 */
public object FramingMath {

    /**
     * Clamp [crop] into the renderable range. Each edge is first coerced to `[0, 1]`; if an axis is then
     * degenerate or inverted (`left >= right` / `top >= bottom`), that axis falls back to the full extent
     * `[0, 1]` — a whole-axis crop is always valid and is the least-surprising recovery from a bad input.
     * Idempotent: `clampCrop(clampCrop(c)) == clampCrop(c)`, and a valid crop is returned unchanged.
     */
    public fun clampCrop(crop: Crop): Crop {
        val left = crop.left.coerceIn(0.0, 1.0)
        val right = crop.right.coerceIn(0.0, 1.0)
        val top = crop.top.coerceIn(0.0, 1.0)
        val bottom = crop.bottom.coerceIn(0.0, 1.0)
        val (l, r) = if (left < right) left to right else 0.0 to 1.0
        val (t, b) = if (top < bottom) top to bottom else 0.0 to 1.0
        return Crop(left = l, top = t, right = r, bottom = b)
    }

    /**
     * How far apart two crop edges may be and still be *the same framing*, as an image fraction.
     *
     * Sized against both ends deliberately, because [D-097](../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-097)
     * was caused by leaving the equivalent number unstated (at `0`). **Both bounds are measured, not
     * estimated** — an earlier draft of this KDoc estimated the ceiling from the wrong quantity and was
     * wrong by two orders, which is the same species of mistake as the defect itself:
     *  - **Floor — the drift being absorbed: worst measured `1.55e-15` (~7 ULP).** `Framing.seedDraft`
     *    recovers a zoom from a persisted crop and `resolveCrop` recovers the width back from that zoom;
     *    dividing by a quotient does not return the numerator, and `ch0 / zoom` re-rounds through a
     *    reciprocal. Measured over 405 aspect × zoom × pan combinations against the real code paths.
     *    `1e-9` sits **~5.8 orders above** it, with zero escapes observed.
     *  - **Ceiling — the finest *deliberate* change: ~`1.6e-5`.** ⚠ This is **not** "one pixel over the
     *    overlay's width". A drag converts through `ReframeOverlay`'s
     *    `fx = -pan.x * (cw0 / zoom) / frameWpx` — the *element frame* in **device px**, scaled by the
     *    crop extent — so zoom (÷`MAX_ZOOM`), photo aspect (`cw0 = bratio/pratio`, ÷14 on a 10:1
     *    panorama) and screen density all shrink it. Worst realistic case is a panorama at `zoom = 4` on
     *    a large dense frame. `1e-9` sits **~4 orders below** it; reaching it would need a photo of about
     *    175,000:1. The a11y nudge (`0.05`) and the zoom stepper (`×1.15`) are coarser still.
     *
     * Four orders of clearance at the tight end is the point: it cannot mask an edit, and it cannot be
     * defeated by rounding.
     */
    public const val FRAMING_EPS: Double = 1e-9

    /**
     * Is `(a, aFit)` the same framing as `(b, bFit)` — i.e. did the maker actually change anything?
     *
     * **The one place that decides this, for both callers that must agree.** `EditorReducer`'s
     * `CommitReframe` uses it to decide whether an undoable command and an autosave are recorded, and
     * `EditorScreen.commitReframe` uses it to choose between *"Framing saved."* and *"Framing unchanged."*.
     * Those two were already documented as using "the same comparison" and were both `==` on
     * [Crop]'s `Double`s — so they agreed only by being wrong identically, and 56 % of untouched Reframe
     * sessions announced a save that did not happen ([D-097]).
     *
     * ⚠ **[Fit] is compared exactly and must be** — it is an enum, it carries no rounding, and
     * `FILL`→`FIT` at an equal crop is a real change in what gets drawn.
     */
    public fun sameFraming(a: Crop, aFit: Fit, b: Crop, bFit: Fit): Boolean =
        aFit == bFit &&
            abs(a.left - b.left) <= FRAMING_EPS &&
            abs(a.top - b.top) <= FRAMING_EPS &&
            abs(a.right - b.right) <= FRAMING_EPS &&
            abs(a.bottom - b.bottom) <= FRAMING_EPS
}
