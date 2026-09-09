package com.aritr.zinely.render.android

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.Fit
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextStyle
import com.aritr.zinely.core.render.DrawCommand
import com.aritr.zinely.core.render.DrawImage
import com.aritr.zinely.core.render.DrawShape
import com.aritr.zinely.core.render.DrawTextBox
import com.aritr.zinely.core.render.FillRect
import com.aritr.zinely.core.render.Segment
import com.aritr.zinely.core.render.Subpath
import com.aritr.zinely.core.render.SupplyOutline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * **The print-PDF surface's verification stream (F4 Increment 2).**
 *
 * DoD 3 requires all four surfaces to render from the same display list and be verified. Three surfaces
 * are covered headlessly — page images by `RasterGoldenTest`, editor preview by `PagePreviewGoldenTest`,
 * and host-vs-replay by `PagePreviewParityTest`. **PDF cannot join them**: `PdfDocument` throws
 * `IllegalStateException: document is closed!` under Robolectric `graphicsMode=NATIVE`, measured
 * directly during F4's feasibility investigation, so the print surface is verified **instrumented, on a
 * device** — a founder decision, taken because the objective is verification of production output, not
 * uniformity of mechanism.
 *
 * **What this adds over [PdfExportInstrumentedTest].** That test proves the PDF *exists* and agrees with
 * the raster path at **one interior pixel of one flat fill** — it says so itself ("the cross-canvas
 * parity seed; full multi-scale goldens are G6"). A single pixel cannot catch a shifted rectangle, a
 * rotated clip applied in the wrong frame, or text laid out with different metrics. This test compares
 * **whole rasters** across the same case set the other surfaces use, so the print surface is verified to
 * the same standard rather than merely sampled.
 *
 * **What the whole-raster fraction does and does not do.** The two paths run different rasterisers —
 * `PdfRenderer` (PDF Skia) versus `Canvas(Bitmap)` (Bitmap Skia) — so anti-aliased edges legitimately
 * differ by a pixel of coverage. [EDGE_TOLERANCE] is an **AA-noise bar**, nothing more: a whole-page
 * fraction is a poor detector of placement, because translating a 36 pt quadrant by a full point at
 * 300 DPI changes only ~1.4 % of the page — under the bar. Placement is therefore caught by the
 * **absolute colour probes** in each case, which are chosen to sit either side of an edge, not by the
 * fraction. (An earlier revision of this file claimed the fraction caught a 1 pt error; it does not,
 * and the probes were added because it does not.)
 *
 * **A parity metric is blind to shared bugs, by construction.** Both surfaces replay through the *same*
 * `CanvasReplayer`, so a defect in the shared path moves both rasters identically and the fraction stays
 * near zero. Only the absolute assertions can catch that class — which is why every case below pins real
 * colours at real coordinates rather than relying on the diff.
 *
 * **Text is measured in its own ink region, not across the whole page** — the lesson `PagePreviewParityTest`
 * records: ink is a tiny fraction of a sheet, so a whole-frame differing-fraction dilutes a real font or
 * metrics divergence below any useful bar.
 *
 * Runs on a device; compile-checked in CI (`:render-android:compileDebugAndroidTestKotlin`) so it cannot
 * bitrot, and executed as part of the instrumented verification stream.
 */
@RunWith(AndroidJUnit4::class)
class PdfSurfaceParityInstrumentedTest {

    private val assets get() =
        ApplicationProvider.getApplicationContext<android.content.Context>().assets
    private val cacheDir get() =
        ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir

    private val replayer by lazy { CanvasReplayer(BundledFontResolver(assets)) }
    private val pdf by lazy { PdfPageRenderer(replayer) }
    private val raster by lazy { RasterPageRenderer(replayer) }
    private val rasterizer = PdfRasterizer()

    private val sheet = PtSize(72.0, 72.0)

    private companion object {
        /** Interiors of flat fills must be identical; this bar allows only AA edge coverage to move. */
        const val EDGE_TOLERANCE = 0.03

        /**
         * Per-channel delta below which two pixels count as the same ink. Antialiased glyph coverage
         * legitimately differs between Bitmap Skia (`hinting = HINTING_ON`, subpixel text) and the vector
         * PDF rasterised through `RENDER_MODE_FOR_PRINT`; a shape or placement difference does not hide
         * under it, because a moved glyph puts ink where the other surface has paper — a full-scale delta.
         */
        const val CHANNEL_TOLERANCE = 48

        /**
         * Bound on materially-differing ink, as a fraction of the ink drawn. **Calibrated on device**
         * (SM-A176B, Android 16), not authored:
         *
         * | measurement | value |
         * |---|---|
         * | PDF vs raster, same font | **0.276** |
         * | bundled-vs-default font swap (discriminator) | **1.171** |
         *
         * `0.55` sits ~2× above the observed agreement and ~2× below a genuine font swap — a 4.2×
         * separation between "the two rasterisers disagree about coverage" and "the two surfaces drew
         * different glyphs". [text_matchesAcrossSurfaces_measuredOverItsOwnInk] re-proves that separation
         * on every run, so the bar cannot quietly become meaningless.
         *
         * An earlier revision compared pixels *exactly* and measured 0.746 — not a defect, just the wrong
         * metric for antialiased text across two rasterisers.
         */
        const val TEXT_INK_TOLERANCE = 0.55

        /** Ink bounding boxes may differ by this many px per edge (observed: 1). */
        const val BBOX_TOLERANCE_PX = 3

        /** Ratio of the surfaces' ink masses (observed: 1.137 — PDF AA is softer, so it inks more). */
        const val MASS_RATIO_TOLERANCE = 1.35

        val RED = ColorRgba(255, 0, 0, 255)
        val BLUE = ColorRgba(0, 0, 255, 255)
        val WHITE = ColorRgba.WHITE

        /** A row needs this much ink to count as a text line rather than an AA fringe. */
        const val MIN_ROW_INK = 3
    }

    /** The two surfaces' rasters for one tape, at identical dimensions ([ExportScale.pxExtent]). */
    private fun bothSurfaces(tape: List<DrawCommand>): Pair<Bitmap, Bitmap> {
        val rasterBmp = raster.render(tape, sheet)
        val pdfBmp = rasterizer.rasterize(pdf.render(tape, sheet), cacheDir = cacheDir)
        assertEquals("surfaces must rasterise to equal width", rasterBmp.width, pdfBmp.width)
        assertEquals("surfaces must rasterise to equal height", rasterBmp.height, pdfBmp.height)
        return rasterBmp to pdfBmp
    }

    private fun differingFraction(a: Bitmap, b: Bitmap): Double {
        var differing = 0
        for (y in 0 until a.height) {
            for (x in 0 until a.width) if (a.getPixel(x, y) != b.getPixel(x, y)) differing++
        }
        return differing.toDouble() / (a.width * a.height)
    }

    /** Bounding box of non-white pixels as `[l,t,r,b]` — where the ink actually sits. */
    private fun inkBoundsOf(b: Bitmap): IntArray {
        var l = b.width; var t = b.height; var r = -1; var bo = -1
        for (y in 0 until b.height) for (x in 0 until b.width) {
            if (b.getPixel(x, y) != Color.WHITE) {
                if (x < l) l = x; if (x > r) r = x; if (y < t) t = y; if (y > bo) bo = y
            }
        }
        return intArrayOf(l, t, r, bo)
    }

    /** Bounding box of non-white pixels as "l,t,r,b" — where the ink actually sits. */
    private fun inkBounds(b: Bitmap): String {
        var l = b.width; var t = b.height; var r = -1; var bo = -1
        for (y in 0 until b.height) for (x in 0 until b.width) {
            if (b.getPixel(x, y) != Color.WHITE) {
                if (x < l) l = x; if (x > r) r = x; if (y < t) t = y; if (y > bo) bo = y
            }
        }
        return "$l,$t,$r,$bo"
    }

    /** Ink-relative difference counting only pixels whose channels differ by more than [tol]. */
    private fun tolerantInkRatio(a: Bitmap, b: Bitmap, tol: Int): Double {
        var differing = 0
        for (y in 0 until a.height) for (x in 0 until a.width) {
            val pa = a.getPixel(x, y); val pb = b.getPixel(x, y)
            val d = maxOf(
                Math.abs(Color.red(pa) - Color.red(pb)),
                Math.abs(Color.green(pa) - Color.green(pb)),
                Math.abs(Color.blue(pa) - Color.blue(pb)),
            )
            if (d > tol) differing++
        }
        return differing.toDouble() / maxOf(inkCount(a), inkCount(b), 1).toDouble()
    }

    /** Count of non-white pixels — used to prove a region actually carries ink before comparing it. */
    private fun inkCount(b: Bitmap): Int {
        var n = 0
        for (y in 0 until b.height) {
            for (x in 0 until b.width) if (b.getPixel(x, y) != Color.WHITE) n++
        }
        return n
    }

    private fun bg() = FillRect(PtRect(0.0, 0.0, sheet.width, sheet.height), WHITE)

    /**
     * A real asymmetric PNG proves the reflection matrices survive both Android replay surfaces.
     * Absolute quadrant probes catch a shared wrong-axis/wrong-order bug; the fraction catches a
     * raster/PDF disagreement. This is device-only because both real region decode and PdfDocument are.
     */
    @Test
    fun flippedImage_horizontalVerticalAndCombined_matchRasterAndPdf() {
        val master = "flip-parity-master"
        val source = AssetBytesSource { id ->
            if (id == master) {
                InstrumentationRegistry.getInstrumentation().context.assets
                    .open("fixtures/image_master_1024.png")
            } else {
                null
            }
        }
        val imageReplayer = CanvasReplayer(imageBlitter = ImageBlitter(source))
        val imageRaster = RasterPageRenderer(imageReplayer)
        val imagePdf = PdfPageRenderer(imageReplayer)
        val box = PtRect(0.0, 0.0, sheet.width, sheet.height)

        fun surfaces(localToPage: AffineTransform2D): Pair<Bitmap, Bitmap> {
            val tape = listOf(
                bg(),
                DrawImage(master, Crop.FULL, Fit.FILL, box, localToPage, localClip = box),
            )
            val rasterBmp = imageRaster.render(tape, sheet)
            val pdfBmp = rasterizer.rasterize(imagePdf.render(tape, sheet), cacheDir = cacheDir)
            assertEquals(rasterBmp.width, pdfBmp.width)
            assertEquals(rasterBmp.height, pdfBmp.height)
            return rasterBmp to pdfBmp
        }

        fun assertColour(label: String, expected: Int, actual: Int) {
            val delta = maxOf(
                Math.abs(Color.red(expected) - Color.red(actual)),
                Math.abs(Color.green(expected) - Color.green(actual)),
                Math.abs(Color.blue(expected) - Color.blue(actual)),
            )
            assertTrue("$label expected $expected but was $actual (channel delta $delta)", delta <= 12)
        }

        fun quadrantColours(bitmap: Bitmap): List<Int> {
            val x = listOf(bitmap.width / 4, bitmap.width * 3 / 4)
            val y = listOf(bitmap.height / 4, bitmap.height * 3 / 4)
            return listOf(
                bitmap.getPixel(x[0], y[0]),
                bitmap.getPixel(x[1], y[0]),
                bitmap.getPixel(x[0], y[1]),
                bitmap.getPixel(x[1], y[1]),
            )
        }

        fun assertQuadrants(label: String, bitmap: Bitmap, colours: List<Int>) {
            quadrantColours(bitmap).zip(colours).forEachIndexed { index, (actual, expected) ->
                assertColour("$label quadrant $index", expected, actual)
            }
        }

        val (identityRaster, identityPdf) = surfaces(AffineTransform2D.identity())
        val sourceColours = quadrantColours(identityRaster)
        assertEquals("fixture must keep four asymmetric quadrants", 4, sourceColours.distinct().size)
        assertQuadrants("identity PDF", identityPdf, sourceColours)

        val cases = listOf(
            "horizontal" to (
                AffineTransform2D.translate(sheet.width, 0.0)
                    .times(AffineTransform2D.scale(-1.0, 1.0)) to
                    listOf(sourceColours[1], sourceColours[0], sourceColours[3], sourceColours[2])
                ),
            "vertical" to (
                AffineTransform2D.translate(0.0, sheet.height)
                    .times(AffineTransform2D.scale(1.0, -1.0)) to
                    listOf(sourceColours[2], sourceColours[3], sourceColours[0], sourceColours[1])
                ),
            "combined" to (
                AffineTransform2D.translate(sheet.width, sheet.height)
                    .times(AffineTransform2D.scale(-1.0, -1.0)) to
                    listOf(sourceColours[3], sourceColours[2], sourceColours[1], sourceColours[0])
                ),
        )

        cases.forEach { (label, case) ->
            val (matrix, colours) = case
            val (rasterBmp, pdfBmp) = surfaces(matrix)
            assertQuadrants("$label raster", rasterBmp, colours)
            assertQuadrants("$label PDF", pdfBmp, colours)
            val fraction = differingFraction(rasterBmp, pdfBmp)
            assertTrue(
                "$label PDF and raster differ over ${"%.4f".format(fraction)} of the page",
                fraction <= EDGE_TOLERANCE,
            )
        }
    }

    /**
     * **The hole test on the print surface** (SUPPLIES-SPEC §3.2, §3.5) — the PDF twin of
     * `ShapeReplayTest.holeTest_sameWoundRing_leavesItsCentreUnfilled`, which can only prove the raster
     * surface because `PdfDocument` does not run under Robolectric (see this file's header).
     *
     * A ring whose inner and outer contours wind the **same** direction is the only geometry whose
     * rendering differs between even-odd and non-zero, so this case fails on a dropped `Path.fillType`
     * — **and**, uniquely here, on a silent raster fallback: if SkPDF gave up on vectors and rasterised
     * the shape, the rasterised-back PDF would not agree with the raster surface to this tolerance.
     *
     * ⚠ It does **not** catch a missing scale fold or a wrong composition order, though an earlier draft
     * of this comment claimed both. Like its raster twin it builds `localToPage` by hand and never routes
     * through `SceneRenderer`, so the fold is not on its path; mutation testing on the twin proved the
     * claim false. Those two live in `SceneRendererDecorTest`.
     *
     * Note what the parity *fraction* cannot do for this case, per this file's own warning: both
     * surfaces replay through the same `CanvasReplayer`, so a dropped fill rule closes the hole on both
     * and the fraction stays near zero. The **absolute** centre probe is the assertion that matters.
     */
    @Test
    fun supplyRing_holeSurvivesOnBothSurfaces() {
        fun square(x: Double, y: Double, size: Double) = Subpath(
            start = PtPoint(x, y),
            segments = listOf(
                Segment.LineTo(PtPoint(x + size, y)),
                Segment.LineTo(PtPoint(x + size, y + size)),
                Segment.LineTo(PtPoint(x, y + size)),
            ),
        )
        // Same-wound: both contours clockwise. Opposite winding has a hole under BOTH rules — it would
        // pass whichever rule the backend used and prove nothing.
        val ring = SupplyOutline(listOf(square(0.0, 0.0, 1.0), square(0.25, 0.25, 0.5)))
        val tape = listOf(
            bg(),
            DrawShape(
                outline = ring,
                ink = RED,
                // The §3.4.1 fold, by hand: without the scale term this is a 1pt x 1pt speck.
                localToPage = AffineTransform2D.translate(12.0, 12.0)
                    .times(AffineTransform2D.scale(48.0, 48.0)),
            ),
        )
        val (r, p) = bothSurfaces(tape)

        // Page point -> pixel, both surfaces rasterise at identical dimensions (asserted in bothSurfaces).
        fun px(v: Double) = (v / sheet.width * r.width).toInt()

        assertEquals("raster hole is paper", Color.WHITE, r.getPixel(px(36.0), px(36.0)))
        assertEquals("PDF hole is paper", Color.WHITE, p.getPixel(px(36.0), px(36.0)))
        assertEquals("raster wall is inked", Color.RED, r.getPixel(px(36.0), px(15.0)))
        assertEquals("PDF wall is inked", Color.RED, p.getPixel(px(36.0), px(15.0)))
        assertEquals("raster outside is paper", Color.WHITE, r.getPixel(px(4.0), px(4.0)))
        assertEquals("PDF outside is paper", Color.WHITE, p.getPixel(px(4.0), px(4.0)))

        val fraction = differingFraction(r, p)
        Log.i("F4MEASURED", "supply ring: fraction=${"%.5f".format(fraction)} (bar $EDGE_TOLERANCE)")
        assertTrue(
            "PDF and raster supply rings differ over ${"%.4f".format(fraction)} of the page " +
                "(> $EDGE_TOLERANCE) — more than an antialiasing difference, and a raster fallback " +
                "in the PDF device would look exactly like this",
            fraction <= EDGE_TOLERANCE,
        )
    }

    @Test
    fun fillQuadrants_matchAcrossSurfaces() {
        val tape = listOf(
            bg(),
            FillRect(PtRect(0.0, 0.0, 36.0, 36.0), RED),
            FillRect(PtRect(36.0, 36.0, 36.0, 36.0), BLUE),
        )
        val (r, p) = bothSurfaces(tape)

        // Interiors are exact — a quadrant that moved would change these outright.
        val q = r.width / 4
        assertEquals("raster TL interior", Color.RED, r.getPixel(q, q))
        assertEquals("PDF TL interior", Color.RED, p.getPixel(q, q))
        assertEquals("raster BR interior", Color.BLUE, r.getPixel(3 * q, 3 * q))
        assertEquals("PDF BR interior", Color.BLUE, p.getPixel(3 * q, 3 * q))

        val fraction = differingFraction(r, p)
        Log.i("F4MEASURED", "fills: fraction=${"%.5f".format(fraction)} (bar $EDGE_TOLERANCE)")
        assertTrue(
            "PDF and raster surfaces differ over ${"%.4f".format(fraction)} of the page (> $EDGE_TOLERANCE)",
            fraction <= EDGE_TOLERANCE,
        )
    }

    @Test
    fun zOverlap_lastDrawnWins_onBothSurfaces() {
        val tape = listOf(
            bg(),
            FillRect(PtRect(8.0, 8.0, 40.0, 40.0), RED), // back
            FillRect(PtRect(24.0, 24.0, 40.0, 40.0), BLUE), // front
        )
        val (r, p) = bothSurfaces(tape)
        val s = ExportScale.EXPORT_PX_PER_PT

        // Draw ORDER, not just presence: the overlap must be the front colour on both surfaces.
        val ox = (40.0 * s).toInt()
        assertEquals("raster overlap is front", Color.BLUE, r.getPixel(ox, ox))
        assertEquals("PDF overlap is front", Color.BLUE, p.getPixel(ox, ox))

        val fraction = differingFraction(r, p)
        Log.i("F4MEASURED", "zOverlap: fraction=${"%.5f".format(fraction)} (bar $EDGE_TOLERANCE)")
        assertTrue("z-overlap differs over ${"%.4f".format(fraction)}", fraction <= EDGE_TOLERANCE)
    }

    @Test
    fun rotatedClip_appliesInItsOwnFrame_onBothSurfaces() {
        // ADR-028 §3.1: localClip is applied AFTER concat(localToPage), so it rotates with the element.
        // If the PDF canvas applied them in the other order the clip would stay axis-aligned — a defect
        // one interior pixel could never see, because the centre is filled under both orderings.
        val rotateAboutCentre = AffineTransform2D.translate(36.0, 36.0)
            .times(AffineTransform2D.rotateDeg(30.0))
            .times(AffineTransform2D.translate(-36.0, -36.0))
        val tape = listOf(
            bg(),
            FillRect(
                rect = PtRect(0.0, 0.0, 72.0, 72.0),
                color = RED,
                localToPage = rotateAboutCentre,
                localClip = PtRect(16.0, 16.0, 40.0, 40.0),
            ),
        )
        val (r, p) = bothSurfaces(tape)
        val s = ExportScale.EXPORT_PX_PER_PT

        assertEquals("raster clip interior", Color.RED, r.getPixel((36.0 * s).toInt(), (36.0 * s).toInt()))
        assertEquals("PDF clip interior", Color.RED, p.getPixel((36.0 * s).toInt(), (36.0 * s).toInt()))

        // **The discriminating probe.** (18,18) pt is INSIDE the axis-aligned rect [16,56]² but OUTSIDE
        // the same rect once rotated 30° about the centre (its local coords are ≈(11.4, 29.4), left of
        // the clip's x=16 edge). So it is RED if the clip were applied before the rotation and WHITE if
        // applied after — which is the ADR-028 §3.1 ordering this case exists to pin.
        //
        // A point far outside both shapes (an earlier revision probed (4,36) pt) is white under BOTH
        // orderings and proves only that *some* clip exists. The probe has to straddle the two shapes.
        val dx = (18.0 * s).toInt()
        assertEquals("raster: clip must rotate with the element", Color.WHITE, r.getPixel(dx, dx))
        assertEquals("PDF: clip must rotate with the element", Color.WHITE, p.getPixel(dx, dx))

        val fraction = differingFraction(r, p)
        Log.i("F4MEASURED", "rotatedClip: fraction=${"%.5f".format(fraction)} (bar $EDGE_TOLERANCE)")
        assertTrue("rotated clip differs over ${"%.4f".format(fraction)}", fraction <= EDGE_TOLERANCE)
    }

    @Test
    fun text_matchesAcrossSurfaces_measuredOverItsOwnInk() {
        // Text is the highest-risk divergence between canvases (ADR-028 §4): PDF keeps it vector via
        // StaticLayout.draw while the raster path rasterises glyphs, so identical layout is the thing
        // being proven. Both go through the SAME SharedTextLayout, so wrapping and metrics must agree.
        val boxX = 8.0
        val boxY = 12.0
        val boxW = 56.0
        val boxH = 44.0
        val tape = listOf(
            bg(),
            DrawTextBox(
                text = "Hand folded zines print sharp",
                style = TextStyle(fontFamily = "Inter", sizePt = 8.0, color = ColorRgba.BLACK),
                boxWidthPt = boxW,
                boxHeightPt = boxH,
                localToPage = AffineTransform2D.translate(boxX, boxY),
                localClip = PtRect(0.0, 0.0, boxW, boxH),
            ),
        )
        val (r, p) = bothSurfaces(tape)
        val s = ExportScale.EXPORT_PX_PER_PT

        val rx = (boxX * s).toInt()
        val ry = (boxY * s).toInt()
        val rw = (boxW * s).toInt().coerceAtMost(r.width - rx)
        val rh = (boxH * s).toInt().coerceAtMost(r.height - ry)
        val rInk = Bitmap.createBitmap(r, rx, ry, rw, rh)
        val pInk = Bitmap.createBitmap(p, rx, ry, rw, rh)

        // Sanity before comparison: two blank crops would "agree" perfectly and prove nothing.
        assertTrue("raster produced no text ink", inkCount(rInk) > 50)
        assertTrue("PDF produced no text ink", inkCount(pInk) > 50)

        // Line breaking must be identical: same ink rows means the same number of lines in the same
        // places. This catches a wrapping divergence that a coverage-only threshold would absorb.
        // Line breaking must be identical — the same number of inked bands. This catches a wrapping
        // divergence that a coverage-only bound would absorb entirely.
        assertEquals(
            "the two surfaces broke the text into a different number of lines",
            inkBandCount(rInk), inkBandCount(pInk),
        )

        // **Discriminator first — prove the bar can fail before trusting that it passed.** Render the
        // same text through the system default resolver instead of the bundled one: a genuine font swap
        // must exceed the bar, or the bar is too loose to detect one and this test fails loudly asking
        // for better text rather than passing vacuously. This is the idiom `PagePreviewParityTest` uses.
        val defaultReplayer = CanvasReplayer(FontResolver.Default)
        val defaultBmp = RasterPageRenderer(defaultReplayer).render(tape, sheet)
        val defaultInk = Bitmap.createBitmap(defaultBmp, rx, ry, rw, rh)
        val swapRatio = tolerantInkRatio(rInk, defaultInk, CHANNEL_TOLERANCE)
        assertTrue(
            "a bundled-vs-default font swap changes only ${"%.3f".format(swapRatio)} of the ink " +
                "(<= $TEXT_INK_TOLERANCE); this test cannot tell the resolvers apart, so choose text or a " +
                "size that renders differently in Inter vs the system default",
            swapRatio > TEXT_INK_TOLERANCE,
        )

        // **Placement and extent — the assertions that actually catch a metrics divergence.** If the two
        // surfaces laid the text out differently the ink would start or end somewhere else; coverage
        // tolerance cannot absorb that, because a moved glyph inks paper on one surface only.
        val rb = inkBoundsOf(rInk)
        val pb = inkBoundsOf(pInk)
        for (i in 0..3) {
            assertTrue(
                "text ink bounding boxes disagree by more than $BBOX_TOLERANCE_PX px " +
                    "(raster=${rb.toList()} pdf=${pb.toList()})",
                Math.abs(rb[i] - pb[i]) <= BBOX_TOLERANCE_PX,
            )
        }
        val massRatio = maxOf(inkCount(rInk), inkCount(pInk)).toDouble() /
            maxOf(minOf(inkCount(rInk), inkCount(pInk)), 1)
        assertTrue(
            "one surface inked ${"%.2f".format(massRatio)}× the other (> $MASS_RATIO_TOLERANCE) — " +
                "that is more than an antialiasing difference",
            massRatio <= MASS_RATIO_TOLERANCE,
        )

        // Primary bound: of the ink actually drawn, how much disagrees MATERIALLY between the surfaces.
        val inkRatio = tolerantInkRatio(rInk, pInk, CHANNEL_TOLERANCE)
        // Measured values are printed so a future calibration can read them off a run rather than
        // guessing, and so a threshold drifting toward its bar is visible before it fails.
        Log.i("F4MEASURED",
            "text: inkRaster=${inkCount(rInk)} inkPdf=${inkCount(pInk)} " +
                "bands=${inkBandCount(rInk)}/${inkBandCount(pInk)} " +
                "inkRatio=${"%.4f".format(inkRatio)} (bar $TEXT_INK_TOLERANCE) " +
                "swapRatio=${"%.4f".format(swapRatio)} " +
                "bboxRaster=${inkBounds(rInk)} bboxPdf=${inkBounds(pInk)} " +
                "tolerant16=${"%.4f".format(tolerantInkRatio(rInk, pInk, 16))} " +
                "tolerant48=${"%.4f".format(tolerantInkRatio(rInk, pInk, 48))} " +
                "swapTolerant48=${"%.4f".format(tolerantInkRatio(rInk, defaultInk, 48))} " +
                "massRatio=${"%.4f".format(maxOf(inkCount(rInk), inkCount(pInk)).toDouble() / minOf(inkCount(rInk), inkCount(pInk)))}",
        )
        assertTrue(
            "${"%.3f".format(inkRatio)} of the text ink differs between surfaces (> $TEXT_INK_TOLERANCE) — " +
                "glyph coverage may differ between rasterisers, but layout must not",
            inkRatio <= TEXT_INK_TOLERANCE,
        )
    }

    /**
     * The number of **contiguous inked bands** — i.e. text lines — which is the layout signature this
     * case actually cares about.
     *
     * Calibrated against a real device run (F4 instrumented validation). Counting inked *rows* instead
     * measured 91 on the raster surface and 92 on the PDF surface: a one-row delta where a genuine
     * wrapping change would move a whole band (~33 rows at 8 pt / 300 DPI). That single row is a band
     * edge crossing [MIN_ROW_INK] on one rasteriser and not the other — precisely the AA-coverage
     * difference this file grants elsewhere. Counting bands measures lines rather than pixels, so it
     * still moves the instant a line appears, disappears, or merges, and is immune to a fringe row.
     */
    private fun inkBandCount(b: Bitmap): Int {
        var bands = 0
        var inBand = false
        for (y in 0 until b.height) {
            var n = 0
            for (x in 0 until b.width) if (b.getPixel(x, y) != Color.WHITE) n++
            val inked = n >= MIN_ROW_INK
            if (inked && !inBand) bands++
            inBand = inked
        }
        return bands
    }
}
