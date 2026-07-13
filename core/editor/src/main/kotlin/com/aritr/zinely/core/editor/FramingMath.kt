package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.Crop

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
}
