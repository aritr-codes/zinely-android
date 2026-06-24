package com.aritr.zinely.core.render

import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.Background
import com.aritr.zinely.core.model.ColorRgba
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
                add(element.toCommand())
            }
        }
        return Scene(pageSizePt, commands)
    }

    public fun emit(scene: Scene): List<DrawCommand> = scene.commands

    private fun Element.toCommand(): DrawCommand {
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
                localToPage = localToPage,
                localClip = box,
            )
        }
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
