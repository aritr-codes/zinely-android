package com.aritr.zinely.core.render

import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.Fit
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.TextStyle

/**
 * One entry in the flat, ordered draw tape `:core:render` produces (ADR-027). Commands are
 * **self-contained** — each carries its own [localToPage] transform and optional [localClip] — so a
 * backend replays each as a single `save → concat(localToPage) → clip(localClip) → draw → restore`
 * with no push/pop stack to keep balanced. All coordinates are page-local **points**; the backend
 * supplies the page→device scale.
 */
public sealed interface DrawCommand {
    /** Element-local → page-space affine (points → points). Identity for page-level fills. */
    public val localToPage: AffineTransform2D

    /** Clip in this command's **local** space, applied after [localToPage]. `null` ⇒ no clip. */
    public val localClip: PtRect?
}

/** A solid fill of [rect] (local space) — page background or, later, element backgrounds. */
public data class FillRect(
    val rect: PtRect,
    val color: ColorRgba,
    override val localToPage: AffineTransform2D = AffineTransform2D.identity(),
    override val localClip: PtRect? = null,
) : DrawCommand

/**
 * A content-addressed image as **intent** — no bytes, no intrinsic size, no resolved rects (seam A,
 * ADR-027). [box] is the element-local placement `(0,0,w,h)`; [crop]/[fit] are the model semantics.
 * The backend decodes [assetId] (`inJustDecodeBounds` → intrinsic, the ground truth), calls the
 * shared pure [computeImageBlit], decodes-to-target (ADR-011) and blits; on decode failure it paints
 * the defined missing-asset placeholder. [localClip] is [box] so FILL/cover overflow is clipped.
 */
public data class DrawImage(
    val assetId: String,
    val crop: Crop,
    val fit: Fit,
    val box: PtRect,
    override val localToPage: AffineTransform2D,
    override val localClip: PtRect?,
) : DrawCommand

/**
 * Text **layout intent** — not laid-out glyphs (ADR-027). The backend builds a `StaticLayout` from
 * these fields via the shared `SharedTextLayout`, laid out in point space with the canvas matrix
 * applying device scale (so wrapping is resolution-independent). [style] is the model's verbatim
 * [TextStyle] (its fields are non-optional, so no document-default fold happens here).
 */
public data class DrawTextBox(
    val text: String,
    val style: TextStyle,
    val boxWidthPt: Double,
    val boxHeightPt: Double,
    override val localToPage: AffineTransform2D,
    override val localClip: PtRect?,
) : DrawCommand
