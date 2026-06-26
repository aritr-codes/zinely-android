package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.Transform

/**
 * Pure live-preview projection (ADR-029 §5, S4 selection-chrome increment). Produces the page the editor
 * **renders during an open transform gesture** by baking the ephemeral [LiveTransform] into the selected
 * elements' [Transform]s — through the **same** [LiveTransform.bake] the gesture's `CommitTransform` uses.
 *
 * This is the chosen live-preview mechanism (Codex review, selection-chrome increment): instead of a
 * `graphicsLayer` over a cached layer, the editor re-renders the whole page in document order via the
 * normal `SceneRenderer` → `PagePreview` path, with only the selected transforms replaced. So:
 *  - **preview == commit by construction** — the live frame is the same render path as the baked commit
 *    (no text-relayout / clip / stroke divergence a scaled raster layer would show);
 *  - **z-order is correct** — the moving element keeps its real `(zIndex, listIndex)` slot;
 *  - **the clamp matches** — [LiveTransform.bake] already floors zoom at `MIN_SIZE_PT` per element;
 *  - **multi-select is exact** — each member bakes about its own centre, exactly as the commit does.
 *
 * Per-frame re-render of a small zine page is cheap (pure point math + tape replay); decoded image bytes
 * are cached in the replayer by asset key, not re-decoded per frame.
 */
public object LivePreview {

    /**
     * The page to render this frame: every element whose id is in [before] gets its transform replaced
     * by `live.bake(before[id], screenPxPerPt)`; all others are untouched. Element list order (hence
     * z-order and the draw tape) is preserved. [before] is the reducer-owned pre-gesture snapshot from
     * [Interaction.Transforming] — never a live re-read of selection (D3).
     */
    public fun apply(
        page: Page,
        before: Map<String, Transform>,
        live: LiveTransform,
        screenPxPerPt: Double,
    ): Page {
        if (before.isEmpty()) return page
        val baked = before.mapValues { (_, t) -> live.bake(t, screenPxPerPt) }
        return page.copy(
            elements = page.elements.map { element ->
                val newTransform = baked[element.id] ?: return@map element
                element.withTransform(newTransform)
            },
        )
    }
}
