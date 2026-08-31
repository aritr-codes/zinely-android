package com.aritr.zinely.core.render

import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.Background
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Element
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform

/**
 * The semantic, resolved intermediate for one page: the page size plus the ordered, flat
 * [commands] tape. Kept as a named seam (ADR-027) between [SceneRenderer.buildScene] and
 * [SceneRenderer.emit] so callers that need the page size alongside the tape have it.
 */
public data class Scene(val pageSizePt: PtSize, val commands: List<DrawCommand>)

/**
 * Pure scene renderer (S3, ADR-027): turns one document [Page] into an ordered, flat list of
 * self-contained [DrawCommand]s in page-local **points**. A pure function of `(Page, defaults)`
 * alone — no asset resolver, no I/O, no Android. Depends only on `:core:model`.
 *
 * Both the editor preview (S4) and PDF/image export (S5) consume the identical tape; the only
 * difference is the page→device matrix each backend pre-concats, which is why preview == export
 * holds by construction ([docs/DECISIONS.md ADR-006]).
 */
public object SceneRenderer {

    /** [buildScene] then [emit] — the convenience composite. */
    public fun render(page: Page, pageSizePt: PtSize, defaults: DocumentDefaults): List<DrawCommand> =
        emit(buildScene(page, pageSizePt, defaults))

    /** Resolve a page into the ordered draw tape (background first, then elements back-to-front). */
    public fun buildScene(page: Page, pageSizePt: PtSize, defaults: DocumentDefaults): Scene {
        val commands = buildList {
            effectiveBackground(page.background, defaults.background)?.let { color ->
                add(FillRect(rect = PtRect(0.0, 0.0, pageSizePt.width, pageSizePt.height), color = color))
            }
            // Stable sort: equal zIndex keeps author (list) order; emitted back-to-front (painter's).
            for (element in page.elements.sortedBy { it.zIndex }) {
                // `null` = a supply whose outline is not authored yet; see [toCommand].
                element.toCommand()?.let { add(it) }
            }
        }
        return Scene(pageSizePt, commands)
    }

    public fun emit(scene: Scene): List<DrawCommand> = scene.commands

    /**
     * One element → one draw command, or `null` for a supply whose outline is not authored yet.
     *
     * Every *element kind* now emits — P3 armed the last one. The remaining `null` is narrower and
     * permanent in shape: an unauthored, misspelled or newer-schema `supplyId` has no outline, and
     * §2.2 puts that check here at the render boundary rather than in the document validator so it
     * draws nothing instead of making the zine refuse to open.
     *
     * `null` is not an error path and not a fallthrough — the `when` below is exhaustive, so a fourth
     * element kind would still be a compile error here.
     */
    private fun Element.toCommand(): DrawCommand? {
        val box = PtRect(0.0, 0.0, transform.widthPt, transform.heightPt)
        val localToPage = localToPage(transform)
        return when (this) {
            is TextElement -> DrawTextBox(
                text = text,
                style = style, // verbatim — no document-default fold (ADR-027)
                boxWidthPt = transform.widthPt,
                boxHeightPt = transform.heightPt,
                localToPage = localToPage,
                localClip = box,
            )
            is ImageElement -> DrawImage(
                assetId = assetId,
                crop = crop,
                fit = fit,
                box = box,
                localToPage = localBoxFold(
                    localToPage = localToPage,
                    transform = transform,
                    flippedHorizontally = flippedHorizontally,
                    flippedVertically = flippedVertically,
                ),
                localClip = box,
                copier = copier,
            )
            // P3: a supply now emits. `null` survives here for exactly one reason — an **unauthored**
            // `supplyId`. Twelve of the sixteen are still owed to a designer, and §2.2 rules that
            // catalogue membership is checked at this boundary rather than in the document validator,
            // precisely so an unknown id draws nothing instead of refusing to open the zine. A
            // misspelled or newer-schema id lands here too, and takes the same silent-but-safe route.
            is DecorElement -> SupplyCatalog.outlineOf(supplyId)?.let { outline ->
                DrawShape(
                    outline = outline,
                    ink = ink,
                    localToPage = unitSquareFold(
                        localToPage = localToPage,
                        transform = transform,
                        flippedHorizontally = mirrored,
                        flippedVertically = flippedVertically,
                    ),
                )
            }
        }
    }

    /**
     * Reflection about the unit square's own vertical centre line, `x → 1 - x`
     * (`translate(1,0) · scale(-1,1)`). Applied in **unit** space, i.e. innermost, so it mirrors the
     * drawing rather than the placement: a mirrored supply occupies the identical page box.
     */
    private val MIRROR_X_IN_UNIT_SQUARE = AffineTransform2D(-1.0, 0.0, 0.0, 1.0, 1.0, 0.0)
    private val MIRROR_Y_IN_UNIT_SQUARE = AffineTransform2D(1.0, 0.0, 0.0, -1.0, 0.0, 1.0)

    /**
     * The unit-square fold (SUPPLIES-SPEC §3.4.1) — the term [localToPage] alone cannot supply.
     *
     * `translate(x,y) · [T(c)·R(deg)·T(-c)] · scale(w,h) · mirror?`, right-to-left in application
     * order: mirror the drawing, blow the unit square up to the element's box, then place and rotate
     * it exactly as every other element is placed. Without the `scale` term a supply renders
     * **1pt × 1pt** — [localToPage] is translate·rotate and carries no scale at all, which is the
     * defect §3.4.1 was written about.
     *
     * The scale is **non-uniform** whenever `w != h`, and that is legal here: stretching tape is what
     * tape does. Keeping a stamp square is an *editor* concern (§3.4.1), never a render one — the tape
     * stays dumb, so nothing below inspects the `supplyId` to decide.
     *
     * ⚠ This said *"editor **constraint**"*, which read as a description of shipped behaviour. It is not:
     * no aspect lock exists, so a stamp can be stretched into an oval today
     * ([D-100](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-100)). The render-side reasoning
     * is unaffected — this fold is correct either way — but the claim about the editor was not true.
     */
    private fun unitSquareFold(
        localToPage: AffineTransform2D,
        transform: Transform,
        flippedHorizontally: Boolean,
        flippedVertically: Boolean,
    ): AffineTransform2D {
        var folded = localToPage.times(AffineTransform2D.scale(transform.widthPt, transform.heightPt))
        if (flippedHorizontally) folded = folded.times(MIRROR_X_IN_UNIT_SQUARE)
        if (flippedVertically) folded = folded.times(MIRROR_Y_IN_UNIT_SQUARE)
        return folded
    }

    /**
     * Reflect photo-local point coordinates inside the unchanged element box. The reflection is the
     * innermost term, so rotation remains about the same page-space centre and crop remains a source
     * rectangle decision carried by [DrawImage] (ADR-113).
     */
    private fun localBoxFold(
        localToPage: AffineTransform2D,
        transform: Transform,
        flippedHorizontally: Boolean,
        flippedVertically: Boolean,
    ): AffineTransform2D {
        var folded = localToPage
        if (flippedHorizontally) {
            folded = folded.times(
                AffineTransform2D(-1.0, 0.0, 0.0, 1.0, transform.widthPt, 0.0),
            )
        }
        if (flippedVertically) {
            folded = folded.times(
                AffineTransform2D(1.0, 0.0, 0.0, -1.0, 0.0, transform.heightPt),
            )
        }
        return folded
    }

    /** Page background wins; `None` falls back to the document default; `None`/`None` ⇒ no fill. */
    private fun effectiveBackground(page: Background, default: Background): ColorRgba? = when (page) {
        is Background.Solid -> page.color
        Background.None -> when (default) {
            is Background.Solid -> default.color
            Background.None -> null
        }
    }

    /**
     * Element-local → page affine: place the box at `(x, y)` and rotate clockwise about its centre.
     * `translate(x,y) × [T(c) · R(deg) · T(-c)]` — emitting the matrix (not `(x,y,deg)`) means the
     * backend never re-derives rotation, the same contract imposition uses for `contentToSheet`.
     */
    private fun localToPage(t: Transform): AffineTransform2D {
        val cx = t.widthPt / 2.0
        val cy = t.heightPt / 2.0
        val rotateAboutCenter = AffineTransform2D.translate(cx, cy)
            .times(AffineTransform2D.rotateDeg(t.rotationDegrees))
            .times(AffineTransform2D.translate(-cx, -cy))
        return AffineTransform2D.translate(t.xPt, t.yPt).times(rotateAboutCenter)
    }
}
