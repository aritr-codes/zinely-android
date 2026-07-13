package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.Fit
import com.aritr.zinely.core.model.ImageElement

/**
 * Which way the photo fills its fixed frame in a Reframe session (ADR-053, frozen bench.html
 * `.fitseg`): [FILL] crops-to-cover (edges may be trimmed), [WHOLE] contains (margins may appear).
 * A **feature-layer** enum distinct from the model's [Fit]: the persisted representation of a panned/
 * zoomed Fill is `Fit.FIT` + a box-aspect crop (see [FramingDraft.toImage]), so FILL is not 1:1 with
 * `Fit.FILL` — keeping them separate types prevents that conflation.
 */
public enum class FrameFit { FILL, WHOLE }

/**
 * The **ephemeral** Reframe working state (ADR-029 §5.1): fit + zoom + pan, held in the feature layer for
 * the life of one [com.aritr.zinely.core.editor.Interaction.Reframing] session and never written to the
 * document until [Intent.CommitReframe][com.aritr.zinely.core.editor.Intent.CommitReframe]. It is the
 * image twin of the transform layer's live accumulator: the frozen bench keeps exactly `{ox, oy, scale,
 * fit}` here (`reframe.before`), pans/zooms it live, and bakes it on Done.
 *
 * [pan] is the crop rectangle's **centre offset from the image centre**, in image-fraction units
 * (`[-0.5, 0.5]` before clamping), so `(0, 0)` is centred. [zoom] is `1.0` (whole cover rect) up to
 * [Framing.MAX_ZOOM]. For [FrameFit.WHOLE] pan/zoom are ignored (contain is always the whole photo,
 * centred) — matching the bench, where choosing "Whole photo" re-centres to a clean baseline.
 */
public data class FramingDraft(
    val fit: FrameFit,
    val zoom: Double,
    val panX: Double,
    val panY: Double,
)

/**
 * Pure, platform-free framing math (ADR-053) shared by the Reframe **preview overlay** and the **commit**
 * so `preview == commit` by construction: the same [FramingDraft] resolves to the same crop the overlay
 * paints and the reducer persists.
 *
 * The load-bearing idea: the model renders a crop through [Fit], and `Fit.FILL` always *re-centres* its
 * aspect sub-rect — so it cannot encode an off-centre pan. A Fill that has been panned/zoomed is therefore
 * persisted as **`Fit.FIT` over a box-aspect crop rectangle** (FIT with a matching-aspect crop fills the
 * box with no letterbox, at exactly the panned position). Only the *untouched, centred* Fill stays
 * `Fit.FILL` + full crop, so it is byte-identical to a freshly placed photo and IF1's no-op commit guard
 * fires. "Whole photo" is `Fit.FIT` + full crop (honest letterbox).
 */
public object Framing {
    public const val MIN_ZOOM: Double = 1.0
    public const val MAX_ZOOM: Double = 4.0

    /** Discrete zoom stepper factor (frozen bench `rfZoom(1.15)`); nudge step is a fraction of the image. */
    public const val ZOOM_STEP: Double = 1.15
    public const val NUDGE_FRACTION: Double = 0.05

    /** The placement default / clean baseline: centred cover, whole-cover zoom. */
    public val DEFAULT_FILL: FramingDraft = FramingDraft(FrameFit.FILL, MIN_ZOOM, 0.0, 0.0)

    private const val EPS: Double = 1e-6

    /**
     * The Fill **cover** crop extent at [zoom] `1` for a photo of aspect [pratio] (`w/h`) in a frame of
     * aspect [bratio]: the maximal box-aspect rectangle centred in the image. Returns `(width, height)`
     * as image fractions; the rectangle always has the box aspect, so `Fit.FIT` over it never letterboxes.
     */
    public fun coverExtent(pratio: Double, bratio: Double): Pair<Double, Double> =
        if (pratio >= bratio) (bratio / pratio) to 1.0 else 1.0 to (pratio / bratio)

    /**
     * Resolve [draft] to the box-aspect crop rectangle it currently frames (used by the overlay and the
     * commit for [FrameFit.FILL]). The rect is shrunk by `1/zoom`, translated by the (clamped) pan, and
     * guaranteed inside `[0, 1]²` so Fill never reveals a gap.
     */
    public fun resolveCrop(draft: FramingDraft, pratio: Double, bratio: Double): Crop {
        val (cw0, ch0) = coverExtent(pratio, bratio)
        val cw = cw0 / draft.zoom
        val ch = ch0 / draft.zoom
        val cx = (0.5 + draft.panX).coerceIn(cw / 2.0, 1.0 - cw / 2.0)
        val cy = (0.5 + draft.panY).coerceIn(ch / 2.0, 1.0 - ch / 2.0)
        return Crop(left = cx - cw / 2.0, top = cy - ch / 2.0, right = cx + cw / 2.0, bottom = cy + ch / 2.0)
    }

    /**
     * Bake [draft] onto [before] for [Intent.CommitReframe][com.aritr.zinely.core.editor.Intent.CommitReframe]:
     * only `crop`/`fit` are set (the reducer keeps the rest of `before`). Untouched centred Fill → the
     * `Fit.FILL` + full-crop baseline (so the commit is a no-op); panned/zoomed Fill → `Fit.FIT` + the
     * resolved box-aspect crop; Whole → `Fit.FIT` + full crop.
     */
    public fun toImage(
        before: ImageElement,
        draft: FramingDraft,
        pratio: Double,
        bratio: Double,
    ): ImageElement = when {
        draft.fit == FrameFit.WHOLE -> before.copy(crop = Crop.FULL, fit = Fit.FIT)
        isBaseline(draft) -> before.copy(crop = Crop.FULL, fit = Fit.FILL)
        else -> before.copy(crop = resolveCrop(draft, pratio, bratio), fit = Fit.FIT)
    }

    /**
     * Reconstruct the working [FramingDraft] from a photo's persisted framing when a session opens, so
     * reframing continues from the current look rather than snapping to a baseline. The inverse of
     * [toImage] for the representations it produces; any other legacy crop is best-effort mapped to a Fill
     * pan/zoom (the commit re-derives from the draft, and clamping keeps it valid).
     */
    public fun seedDraft(image: ImageElement, pratio: Double, bratio: Double): FramingDraft {
        val c = image.crop
        val full = c.left <= EPS && c.top <= EPS && c.right >= 1.0 - EPS && c.bottom >= 1.0 - EPS
        return when {
            image.fit == Fit.FIT && full -> FramingDraft(FrameFit.WHOLE, MIN_ZOOM, 0.0, 0.0)
            full -> DEFAULT_FILL
            else -> {
                val (cw0, _) = coverExtent(pratio, bratio)
                val cw = (c.right - c.left).coerceAtLeast(EPS)
                val zoom = (cw0 / cw).coerceIn(MIN_ZOOM, MAX_ZOOM)
                val cx = (c.left + c.right) / 2.0
                val cy = (c.top + c.bottom) / 2.0
                clampPan(FramingDraft(FrameFit.FILL, zoom, cx - 0.5, cy - 0.5), pratio, bratio)
            }
        }
    }

    /** Clamp a Fill zoom to `[MIN_ZOOM, MAX_ZOOM]`; a no-op basis for [zoomed]/[nudged]. */
    public fun clampZoom(zoom: Double): Double = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)

    /** Multiply the zoom by [factor] (the +/- steppers and pinch), clamped. Whole ignores zoom. */
    public fun zoomed(draft: FramingDraft, factor: Double): FramingDraft =
        draft.copy(zoom = clampZoom(draft.zoom * factor))

    /**
     * Clamp the pan so the resolved crop stays wholly inside the image (Fill never gaps). Derived from the
     * current zoom's crop extent; a wider (less-zoomed) crop has a smaller pan range.
     */
    public fun clampPan(draft: FramingDraft, pratio: Double, bratio: Double): FramingDraft {
        val (cw0, ch0) = coverExtent(pratio, bratio)
        val cw = cw0 / draft.zoom
        val ch = ch0 / draft.zoom
        val maxX = ((1.0 - cw) / 2.0).coerceAtLeast(0.0)
        val maxY = ((1.0 - ch) / 2.0).coerceAtLeast(0.0)
        return draft.copy(panX = draft.panX.coerceIn(-maxX, maxX), panY = draft.panY.coerceIn(-maxY, maxY))
    }

    /** Translate the pan by fractions of the image (drag), clamped so Fill can't reveal a gap. */
    public fun panned(draft: FramingDraft, dxFraction: Double, dyFraction: Double, pratio: Double, bratio: Double): FramingDraft =
        clampPan(draft.copy(panX = draft.panX + dxFraction, panY = draft.panY + dyFraction), pratio, bratio)

    /** One discrete nudge step (the a11y move pad); `(dx, dy)` in `{-1, 0, 1}`. */
    public fun nudged(draft: FramingDraft, dx: Int, dy: Int, pratio: Double, bratio: Double): FramingDraft =
        panned(draft, dx * NUDGE_FRACTION, dy * NUDGE_FRACTION, pratio, bratio)

    /** True when [draft] is the untouched centred-Fill baseline (→ a no-op commit). */
    private fun isBaseline(draft: FramingDraft): Boolean =
        draft.fit == FrameFit.FILL &&
            kotlin.math.abs(draft.zoom - MIN_ZOOM) <= EPS &&
            kotlin.math.abs(draft.panX) <= EPS &&
            kotlin.math.abs(draft.panY) <= EPS
}
