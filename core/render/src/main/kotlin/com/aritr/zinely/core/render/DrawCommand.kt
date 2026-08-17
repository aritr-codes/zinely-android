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
 *
 * ⚠ **"Points" is a statement about *page* space, not about every command's local space.** It held
 * for all three original commands because their [localToPage] carries no scale, so local units *were*
 * points. [DrawShape] breaks that coincidence: its outline is authored in a unit square and the size
 * lives in the transform (SUPPLIES-SPEC §3.4.1). A command whose local space is not points therefore
 * cannot carry a points-valued [localClip] — see [DrawShape.localClip], which is fixed at `null`.
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
    /**
     * The photocopier filter (X3b, ADR-106). Carried on the tape rather than baked into the asset so
     * the one replayer applies it identically on all four surfaces — see [photocopy] and
     * [copierGridSize] for why the dot grid is derived from [box] in points and not from the decode
     * density. Trailing and defaulted: an existing tape construction stays valid.
     */
    val copier: Boolean = false,
) : DrawCommand

/**
 * One placed supply: a filled, single-colour [outline] (SUPPLIES-SPEC §3.3).
 *
 * **Fill only, even-odd** — see [SupplyOutline] for the authoring invariant the fill rule exists to
 * serve. There is no stroke surface here, so there is nothing about caps, joins or miters for the
 * four backends to disagree on.
 *
 * The outline is resolved (from [SupplyCatalog]) rather than referenced by id, which is where this
 * command deliberately differs from [DrawImage]: an outline is code, so resolving it costs no I/O and
 * the tape can stay self-contained without any backend gaining a resolver dependency (§3.4).
 *
 * **[localToPage] carries the size.** The outline is authored in a unit square, so the fold is
 * `translate(x,y) · [T(c)·R(deg)·T(-c)] · scale(w,h) · mirror?` (§3.4.1) — without that `scale` term
 * every supply would render 1pt × 1pt. The scale is non-uniform in general; keeping a stamp square is
 * an **editor** constraint, not a render one.
 *
 * **Emitted and painted** (P3). `SceneRenderer` emits this for a `DecorElement` whose `supplyId` has an
 * authored outline, and `CanvasReplayer` draws it with its own anti-aliased `Paint` and
 * `Path.FillType.EVEN_ODD`. A supply with no authored outline yet emits nothing — twelve of the sixteen
 * are still unauthored, and that is a `null` from `SupplyCatalog.outlineOf`, never a validation failure.
 *
 * The command was defined a package *before* anything could paint it, so that the one thing all four
 * surfaces share was settled before any surface had an opinion about it.
 */
public data class DrawShape(
    val outline: SupplyOutline,
    val ink: ColorRgba,
    override val localToPage: AffineTransform2D,
) : DrawCommand {
    /**
     * Always `null`, and **not a constructor parameter** — the clip is inexpressible rather than
     * merely discouraged.
     *
     * Every other command's local space is points, so a points-valued clip means what it says. This
     * one's local space is the unit square, so a `PtRect(0,0,w,h)` clip — the value the other two
     * element commands pass, and the value anyone would copy — would clip the supply to a `w × h`
     * sliver of a shape that is only 1 × 1 wide. There is nothing to clip anyway: an outline is
     * authored inside its own square (SUPPLIES-SPEC §4.1) and so cannot overflow the box the way a
     * FILL-fitted photo can.
     */
    override val localClip: PtRect? get() = null
}

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
