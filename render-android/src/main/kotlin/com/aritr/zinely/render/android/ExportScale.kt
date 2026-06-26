package com.aritr.zinely.render.android

import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtSize
import kotlin.math.ceil

/**
 * Export/preview scale constants and the page→device transforms shared by the canvas providers
 * (ADR-028 §3.2). The two export targets **decouple the visual CTM from the image-decode resolution**
 * (Codex Required-fix #1):
 *  - **Raster** (Bitmap): the visual CTM scales points→pixels at [EXPORT_PX_PER_PT]; decode at the same.
 *  - **PDF** (PdfDocument): the visual CTM is points→points — [pdfPageToDevice] leaves `contentToSheet`
 *    unscaled because the page surface is already in PostScript points; the decode resolution is still
 *    [EXPORT_PX_PER_PT], supplied **separately** to `replay`, never inferred from the points-valued PDF
 *    canvas (the bug Codex flagged — baking 300/72 in would oversize every coordinate ~4.17×).
 *
 * [pxExtent] is the single point→pixel sizing rule, so the raster renderer and the PDF rasteriser
 * produce **identical** bitmap dimensions — a precondition of the §7.3 parity diff.
 */
public object ExportScale {

    /** Print resolution (ADR-011 / ADR-023): 300 DPI. */
    public const val EXPORT_DPI: Double = 300.0

    /** PostScript points per inch. */
    public const val POINTS_PER_INCH: Double = 72.0

    /** Device pixels per point at the export resolution = `300/72` ≈ 4.1667. */
    public val EXPORT_PX_PER_PT: Double = EXPORT_DPI / POINTS_PER_INCH

    /**
     * Pixel extent for a point length at the export resolution — `ceil`, clamped to ≥ 1px. Multiplies
     * before dividing to avoid float drift (`72 × 300 / 72` is exactly `300`). Shared by [RasterPageRenderer]
     * and [PdfRasterizer] so both yield equal bitmap sizes.
     */
    public fun pxExtent(pt: Double): Int =
        ceil(pt * EXPORT_DPI / POINTS_PER_INCH).toInt().coerceAtLeast(1)

    /** A uniform scale transform `s·I`. */
    public fun uniformScale(s: Double): AffineTransform2D =
        AffineTransform2D(s, 0.0, 0.0, s, 0.0, 0.0)

    /**
     * Raster export visual CTM: points→pixels = `scale(300/72) · contentToSheet` — placement applied
     * first ([AffineTransform2D.times] applies its argument first), then the DPI scale, because the
     * bitmap surface is pixels.
     */
    public fun rasterPageToDevice(contentToSheet: AffineTransform2D): AffineTransform2D =
        uniformScale(EXPORT_PX_PER_PT).times(contentToSheet)

    /**
     * PDF export visual CTM: points→points = `contentToSheet` unchanged. The PDF page surface is already
     * in points, so **no** DPI scale is applied here (Required-fix #1).
     */
    public fun pdfPageToDevice(contentToSheet: AffineTransform2D): AffineTransform2D =
        contentToSheet

    /**
     * Preview seam (ADR-028 §2.4 / §6) — the raw-`Canvas` page→device transform the S4 Compose host
     * concats: `scale(screenPxPerPt) · translate(pageOffset)` (offset applied first). Exposed here so the
     * host stays a thin `drawIntoCanvas` bridge with no geometry of its own and `:render-android` stays
     * Compose-free.
     */
    public fun previewPageToDevice(screenPxPerPt: Double, pageOffset: PtPoint): AffineTransform2D =
        uniformScale(screenPxPerPt).times(AffineTransform2D.translate(pageOffset.x, pageOffset.y))

    /**
     * Inverse of [previewPageToDevice] — maps a device pixel back to its page point, the conversion the
     * S4 gesture layer needs to turn a touch (px) into a page-space hit point for `SelectAt`/snapping.
     * Algebraic inverse of `scale(s) · translate(offset)`: `pagePt = devicePx / s − offset`. Kept beside
     * its forward twin so the preview seam owns **both** directions and they cannot drift; the round-trip
     * is asserted in `ExportScaleTest`.
     */
    public fun previewDeviceToPage(screenPxPerPt: Double, pageOffset: PtPoint, devicePx: PtPoint): PtPoint =
        PtPoint(
            x = devicePx.x / screenPxPerPt - pageOffset.x,
            y = devicePx.y / screenPxPerPt - pageOffset.y,
        )
}

/** Named paper sizes in points (ADR-028 §3.2). The MVP home-print targets ([ADR-011]). */
public enum class PaperSize(public val sizePt: PtSize) {
    LETTER(PtSize(612.0, 792.0)),
    A4(PtSize(595.0, 842.0)),
}
