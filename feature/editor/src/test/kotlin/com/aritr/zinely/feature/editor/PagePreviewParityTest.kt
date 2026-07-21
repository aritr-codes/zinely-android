package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.Fit
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextAlign
import com.aritr.zinely.core.model.TextStyle
import com.aritr.zinely.core.render.DrawCommand
import com.aritr.zinely.core.render.DrawImage
import com.aritr.zinely.core.render.DrawTextBox
import com.aritr.zinely.core.render.FillRect
import com.aritr.zinely.render.android.AssetBytesSource
import com.aritr.zinely.render.android.BundledFontResolver
import com.aritr.zinely.render.android.CanvasReplayer
import com.aritr.zinely.render.android.ExportScale
import com.aritr.zinely.render.android.FontResolver
import com.aritr.zinely.render.android.ImageBlitter
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * **S4 preview-host parity proof (ADR-028 §2.4 / §7.3, spike §2.4 "Codex Required-fix C").**
 *
 * The S3 spike proved the raw [CanvasReplayer] is canvas- and scale-invariant in `:render-android`,
 * but deferred the **Compose** preview pixels to S4: Compose adds its own density handling, parent
 * canvas matrix/clip state, and possible `graphicsLayer`/alpha compositing — any of which could break
 * `preview == export`. Each test here renders the SAME tape through
 *
 *  1. [PagePreview] (the Compose host, rasterised headless via [rasterizeToBitmap] then cropped to the
 *     host's ACTUAL placed pixel bounds — see [hostPageBitmap]; [ComposeCanvasProbeTest] records why
 *     neither `captureToImage()` nor `captureRoboImage()` works under a plain headless run), and
 *  2. a direct [CanvasReplayer.replay] onto a plain [Bitmap] at the SAME scale/offset/resolver,
 *
 * and asserts the two rasters are pixel-equal. The gates are graduated (Codex fix #2): **pure fills
 * and the missing-image placeholder must match EXACTLY** (`differing == 0`); only the AA-edge cases
 * (rotated clip, glyph edges) use `changeThreshold = 0.02f` — exactly `RasterGoldenTest.aa()`'s
 * tolerance. A looser bar on flat fills would mask a real host-introduced placement bug.
 *
 * The §4.2 bundled-font wiring is proven by an actual TEXT render (Codex fix #3): host text must match
 * a direct replay built with [BundledFontResolver] and must NOT match one built with
 * [FontResolver.Default] — so the test fails if the host ever routes text through the wrong resolver.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PagePreviewParityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val SCREEN_PX_PER_PT = 2.5f

        val RED = ColorRgba(255, 0, 0, 255)
        val BLUE = ColorRgba(0, 0, 255, 255)
        val WHITE = ColorRgba(255, 255, 255, 255)

        // Matches RasterGoldenTest.aa(): the committed AA tolerance (fraction of pixels allowed to differ).
        const val AA_THRESHOLD = 0.02
        const val MISSING_ASSET = "no-such-asset" // resolves to null → placeholder

        // The text box ([textTape]) in page points — also the region the text-wiring proof crops to.
        const val TEXT_BOX_X_PT = 6.0
        const val TEXT_BOX_Y_PT = 10.0
        const val TEXT_BOX_W_PT = 64.0
        const val TEXT_BOX_H_PT = 40.0
    }

    private val sheet = PtSize(72.0, 72.0)
    private val offset = PtPoint(0.0, 0.0)
    private val assets get() = ApplicationProvider.getApplicationContext<android.content.Context>().assets

    private fun devicePx(): Pair<Int, Int> =
        ceil(sheet.width * SCREEN_PX_PER_PT).toInt().coerceAtLeast(1) to
            ceil(sheet.height * SCREEN_PX_PER_PT).toInt().coerceAtLeast(1)

    // ---- tapes -------------------------------------------------------------------------------------

    /** Pure axis-aligned fills only — every pixel is flat colour, so host==direct must be EXACT. */
    private fun fillsOnlyTape(): List<DrawCommand> = listOf(
        FillRect(PtRect(0.0, 0.0, sheet.width, sheet.height), WHITE),
        FillRect(PtRect(8.0, 8.0, 40.0, 40.0), RED),
        FillRect(PtRect(40.0, 40.0, 64.0, 64.0), BLUE),
    )

    /** A rotated, locally-clipped fill — its clip edges are anti-aliased, hence the AA tolerance. */
    private fun rotatedClipTape(): List<DrawCommand> {
        val rotateAboutCentre = AffineTransform2D.translate(36.0, 36.0)
            .times(AffineTransform2D.rotateDeg(30.0))
            .times(AffineTransform2D.translate(-36.0, -36.0))
        return listOf(
            FillRect(PtRect(0.0, 0.0, sheet.width, sheet.height), WHITE),
            FillRect(
                rect = PtRect(0.0, 0.0, 72.0, 72.0),
                color = BLUE,
                localToPage = rotateAboutCentre,
                localClip = PtRect(20.0, 20.0, 32.0, 32.0),
            ),
        )
    }

    /** A text box (MVP charset) — glyph edges are AA, so host==bundled-direct uses the AA tolerance. */
    private fun textTape(): List<DrawCommand> = listOf(
        FillRect(PtRect(0.0, 0.0, sheet.width, sheet.height), WHITE),
        DrawTextBox(
            text = "Zine 42",
            style = TextStyle(
                fontFamily = "sans-serif",
                sizePt = 14.0,
                color = ColorRgba.BLACK,
                align = TextAlign.START,
                bold = false,
                italic = false,
            ),
            boxWidthPt = TEXT_BOX_W_PT,
            boxHeightPt = TEXT_BOX_H_PT,
            localToPage = AffineTransform2D.translate(TEXT_BOX_X_PT, TEXT_BOX_Y_PT),
            localClip = PtRect(0.0, 0.0, TEXT_BOX_W_PT, TEXT_BOX_H_PT),
        ),
    )

    /** A DrawImage of a MISSING asset → the deterministic placeholder (flat fill + cross), no decode. */
    private fun missingImageTape(): List<DrawCommand> = listOf(
        FillRect(PtRect(0.0, 0.0, sheet.width, sheet.height), WHITE),
        DrawImage(
            assetId = MISSING_ASSET,
            crop = Crop.FULL,
            fit = Fit.FILL,
            box = PtRect(12.0, 12.0, 48.0, 48.0),
            localToPage = AffineTransform2D.identity(),
            localClip = PtRect(12.0, 12.0, 48.0, 48.0),
        ),
    )

    // ---- reference (export-side) raster ------------------------------------------------------------

    /** The export-side reference: the raw replayer onto a plain bitmap (RasterGoldenTest's render()). */
    private fun directReplayBitmap(
        tape: List<DrawCommand>,
        fontResolver: FontResolver = previewFontResolver(assets),
        imageBytes: AssetBytesSource = AssetBytesSource { null },
    ): Bitmap {
        val (w, h) = devicePx()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        CanvasReplayer(fontResolver = fontResolver, imageBlitter = ImageBlitter(imageBytes)).replay(
            canvas = Canvas(bmp),
            tape = tape,
            pageToDevice = ExportScale.previewPageToDevice(SCREEN_PX_PER_PT.toDouble(), offset),
            pageClip = PtRect(0.0, 0.0, sheet.width, sheet.height),
            decodePxPerPt = SCREEN_PX_PER_PT.toDouble(),
        )
        return bmp
    }

    // ---- host-side raster (robust crop) ------------------------------------------------------------

    /**
     * Drive [PagePreview] sized to the page in px, draw the laid-out decor view, then crop to the
     * host's **actual** placed bounds (fetched from the tagged node) — not a blind `(0,0,w,h)`.
     * Asserts (a) density == 1.0 so `dp == px` holds (else the proof silently weakens), and (b) the
     * placed pixel size equals the expected device px (else Compose sized/placed the host wrongly).
     */
    private fun hostPageBitmap(
        tape: List<DrawCommand>,
        imageBytes: AssetBytesSource = AssetBytesSource { null },
    ): Bitmap {
        val (w, h) = devicePx()
        // dp == px only at density 1.0; assert it rather than assume it (Codex fix #1).
        assertEquals(
            "test display density must be 1.0 for dp==px sizing; got ${composeRule.density.density}",
            1.0f, composeRule.density.density, 0.0001f,
        )

        composeRule.setContent {
            PagePreview(
                tape = tape,
                sheet = sheet,
                screenPxPerPt = SCREEN_PX_PER_PT,
                pageOffset = offset,
                modifier = Modifier.size(w.dp, h.dp),
                imageBytes = imageBytes,
            )
        }
        composeRule.waitForIdle()

        // The host's ACTUAL placed pixel rect (Codex fix #1) — fail loudly if Compose moved/resized it.
        val bounds = composeRule.onNodeWithTag(PagePreviewTestTag).fetchSemanticsNode().boundsInRoot
        val bx = bounds.left.roundToInt()
        val by = bounds.top.roundToInt()
        val bw = bounds.width.roundToInt()
        val bh = bounds.height.roundToInt()
        assertEquals("host placed width != expected device px", w, bw)
        assertEquals("host placed height != expected device px", h, bh)

        val full = composeRule.activity.window.decorView.rasterizeToBitmap()
        require(bx >= 0 && by >= 0 && bx + bw <= full.width && by + bh <= full.height) {
            "host bounds ($bx,$by,${bx + bw},${by + bh}) fall outside decor view ${full.width}x${full.height}"
        }
        return Bitmap.createBitmap(full, bx, by, bw, bh)
    }

    // ---- comparison --------------------------------------------------------------------------------

    /** Fraction of differing pixels between two same-size bitmaps. */
    private fun differingFraction(a: Bitmap, b: Bitmap): Double {
        assertEquals("raster widths differ", a.width, b.width)
        assertEquals("raster heights differ", a.height, b.height)
        var differing = 0
        for (y in 0 until a.height) {
            for (x in 0 until a.width) {
                if (a.getPixel(x, y) != b.getPixel(x, y)) differing++
            }
        }
        return differing.toDouble() / (a.width * a.height)
    }

    // ---- tests -------------------------------------------------------------------------------------

    @Test
    fun fillsOnly_host_matchesDirectReplay_EXACTLY() {
        val reference = directReplayBitmap(fillsOnlyTape())
        val host = hostPageBitmap(fillsOnlyTape())
        val fraction = differingFraction(host, reference)
        assertEquals(
            "pure fills must match the raw replayer EXACTLY (${"%.4f".format(fraction)} differ); the " +
                "Compose layer must add ZERO geometry — all of it lives in ExportScale/CanvasReplayer.",
            0.0, fraction, 0.0,
        )
        // Positive content check so the proof is not vacuous on a blank pair.
        fun px(x: Double, y: Double) =
            host.getPixel((x * SCREEN_PX_PER_PT).toInt(), (y * SCREEN_PX_PER_PT).toInt())
        assertEquals("RED fill region", Color.RED, px(20.0, 20.0))
        assertEquals("BLUE fill region", Color.BLUE, px(52.0, 52.0))
        assertEquals("WHITE background", Color.WHITE, px(68.0, 4.0))
    }

    @Test
    fun rotatedClip_host_matchesDirectReplay_atAaTolerance() {
        val reference = directReplayBitmap(rotatedClipTape())
        val host = hostPageBitmap(rotatedClipTape())
        val fraction = differingFraction(host, reference)
        assertTrue(
            "rotated-clip host drifted from the raw replayer: ${"%.4f".format(fraction)} of pixels " +
                "differ (> $AA_THRESHOLD).",
            fraction <= AA_THRESHOLD,
        )
        // The rotated clip's interior must be the front colour in BOTH paths (non-vacuous).
        val cx = (36.0 * SCREEN_PX_PER_PT).toInt()
        val cy = (36.0 * SCREEN_PX_PER_PT).toInt()
        assertEquals("rotated clip interior", Color.BLUE, host.getPixel(cx, cy))
        assertEquals("rotated clip interior matches reference", reference.getPixel(cx, cy), host.getPixel(cx, cy))
    }

    @Test
    fun text_host_routesThroughBundledFontResolver_notDefault() {
        val host = hostPageBitmap(textTape())
        val bundledDirect = directReplayBitmap(textTape(), fontResolver = BundledFontResolver(assets))
        val defaultDirect = directReplayBitmap(textTape(), fontResolver = FontResolver.Default)

        // Compare ONLY within the text box's device-px region, NOT the whole 72pt sheet (review #19,
        // finding #1). Text ink is a tiny fraction of the full frame, so a whole-frame differing-fraction
        // dilutes the font signal below any useful bar — a host wrongly using FontResolver.Default could
        // still pass an absolute whole-frame 0.02. The textTape box is localToPage=translate(6,10), 64x40pt
        // → px region (15,25)..(175,125) at 2.5 px/pt; cropping there concentrates the glyph signal.
        val rx = (TEXT_BOX_X_PT * SCREEN_PX_PER_PT).toInt()
        val ry = (TEXT_BOX_Y_PT * SCREEN_PX_PER_PT).toInt()
        val rw = (TEXT_BOX_W_PT * SCREEN_PX_PER_PT).toInt()
        val rh = (TEXT_BOX_H_PT * SCREEN_PX_PER_PT).toInt()
        val hostRegion = Bitmap.createBitmap(host, rx, ry, rw, rh)
        val bundledRegion = Bitmap.createBitmap(bundledDirect, rx, ry, rw, rh)
        val defaultRegion = Bitmap.createBitmap(defaultDirect, rx, ry, rw, rh)

        // Sanity: text actually rendered in the region (not two blank crops).
        assertTrue("text host produced no ink in the text region", inkCount(hostRegion) > 20)

        val hostVsBundled = differingFraction(hostRegion, bundledRegion)
        val bundledVsDefault = differingFraction(bundledRegion, defaultRegion)

        // (1) DISCRIMINATOR: the chosen text must render differently in Inter vs the system default by
        // MORE than the host's own AA tolerance — otherwise the proof below cannot tell a bundled host
        // from a Default one, so the test fails loudly asking for distinguishing text rather than passing
        // vacuously. (This is the teeth the old `bundledVsDefault > 0.0` lacked.)
        assertTrue(
            "bundled vs Default fonts differ by only ${"%.4f".format(bundledVsDefault)} of the text region " +
                "(<= $AA_THRESHOLD); the test cannot distinguish the resolvers — choose text/size that " +
                "renders differently in Inter vs the system default.",
            bundledVsDefault > AA_THRESHOLD,
        )

        // (2) PRIMARY proof (review #19, finding #1): host text matches the BUNDLED-resolver replay within
        // the AA tolerance. Because (1) guarantees bundledVsDefault > AA_THRESHOLD, a host that wrongly used
        // FontResolver.Default would have hostVsBundled ≈ bundledVsDefault > AA_THRESHOLD and FAIL here —
        // so this now genuinely proves PagePreview injects BundledFontResolver, not Default.
        assertTrue(
            "host text drifted from the BUNDLED-font replay (${"%.4f".format(hostVsBundled)} of the text " +
                "region differ > $AA_THRESHOLD) — PagePreview must route text through BundledFontResolver.",
            hostVsBundled <= AA_THRESHOLD,
        )

        // Cheap belt-and-braces: the seam type is exactly BundledFontResolver.
        assertTrue("seam is not BundledFontResolver", previewFontResolver(assets) is BundledFontResolver)
    }

    @Test
    fun missingImage_host_paintsPlaceholder_matchingDirectReplay_EXACTLY() {
        // Default null image source on BOTH sides → the deterministic placeholder; no DrawImage crash
        // (Codex fix #4: the host wires an ImageBlitter, so an image tape paints the placeholder).
        val reference = directReplayBitmap(missingImageTape())
        val host = hostPageBitmap(missingImageTape())
        val fraction = differingFraction(host, reference)
        assertEquals(
            "missing-image placeholder must match the raw replayer EXACTLY (${"%.4f".format(fraction)} differ)",
            0.0, fraction, 0.0,
        )
        // Off-diagonal placeholder fill is flat → exact (PLACEHOLDER_FILL_ARGB = 0xFFE0E0E0).
        val px = host.getPixel((20.0 * SCREEN_PX_PER_PT).toInt(), (40.0 * SCREEN_PX_PER_PT).toInt())
        assertEquals("placeholder neutral fill", 0xFFE0E0E0.toInt(), px)
    }

    @Test
    fun host_injectsBundledFontResolver_notDefault_typeCheck() {
        // Cheap extra (NOT the sole proof — see text_host_routesThroughBundledFontResolver_notDefault).
        assertTrue(
            "preview host must inject BundledFontResolver, not FontResolver.Default (ADR-028 §4.2)",
            previewFontResolver(assets) is BundledFontResolver,
        )
    }

    /**
     * A **styled** block — bold + italic + centered + coloured + a non-default ramp size — the FR-3 case
     * ADR-055 §5 names: "a Roborazzi golden of a styled block … renders identically in preview and in the
     * imposed PDF/PNG (single `CanvasReplayer` path)".
     *
     * Every field the Type bar can write is off its default at once, deliberately: a per-attribute sweep
     * would pass while the *combination* dropped one (Compose's own text stack has shipped
     * bold-lost-under-italic-synthesis bugs), and this tape is the whole styling surface in one frame.
     * 24pt is the ramp value the [TypeBarGoldenTest] card is pinned at; Teal is the frozen `.tyinks` token.
     */
    private fun styledTape(): List<DrawCommand> = listOf(
        FillRect(PtRect(0.0, 0.0, sheet.width, sheet.height), WHITE),
        DrawTextBox(
            text = "Zine 42",
            style = TextStyle(
                fontFamily = "sans-serif",
                sizePt = 24.0,
                color = ColorRgba(0x2A, 0x9D, 0x8F),
                align = TextAlign.CENTER,
                bold = true,
                italic = true,
            ),
            boxWidthPt = TEXT_BOX_W_PT,
            boxHeightPt = TEXT_BOX_H_PT,
            localToPage = AffineTransform2D.translate(TEXT_BOX_X_PT, TEXT_BOX_Y_PT),
            localClip = PtRect(0.0, 0.0, TEXT_BOX_W_PT, TEXT_BOX_H_PT),
        ),
    )

    @Test
    fun styledBlock_previewMatchesExportReplay_andGoldens() {
        // ADR-055 §5 "Preview == export parity". Preview and export are the SAME CanvasReplayer over the
        // SAME tape, so this is a proof that no styling attribute leaks in on the host side only (or is
        // dropped on the export side only) — the failure mode a preview-only screenshot cannot see.
        val reference = directReplayBitmap(styledTape())
        val host = hostPageBitmap(styledTape())

        // Crop to the text box's device-px region, as the sibling font proof does: styled glyph ink is a
        // tiny fraction of the 72pt sheet, and a whole-frame differing-fraction dilutes it below any
        // useful bar.
        val rx = (TEXT_BOX_X_PT * SCREEN_PX_PER_PT).toInt()
        val ry = (TEXT_BOX_Y_PT * SCREEN_PX_PER_PT).toInt()
        val rw = (TEXT_BOX_W_PT * SCREEN_PX_PER_PT).toInt()
        val rh = (TEXT_BOX_H_PT * SCREEN_PX_PER_PT).toInt()
        val hostRegion = Bitmap.createBitmap(host, rx, ry, rw, rh)
        val referenceRegion = Bitmap.createBitmap(reference, rx, ry, rw, rh)

        // Non-vacuity: styled text actually rendered (not two blank crops agreeing perfectly).
        assertTrue("styled host produced no ink in the text region", inkCount(hostRegion) > 20)

        // DISCRIMINATOR: the styling must actually change pixels vs the unstyled tape — otherwise a
        // replayer that silently ignored bold/italic/colour/size/align would make the parity proof below
        // vacuous (both sides identically unstyled). Fails loudly rather than passing empty.
        val unstyledRegion = Bitmap.createBitmap(directReplayBitmap(textTape()), rx, ry, rw, rh)
        assertTrue(
            "the styled tape renders the same as the unstyled one (" +
                "${"%.4f".format(differingFraction(referenceRegion, unstyledRegion))} of the region differ" +
                "); the replayer is ignoring the style, so this parity proof would be vacuous.",
            differingFraction(referenceRegion, unstyledRegion) > AA_THRESHOLD,
        )

        // PRIMARY: the styled preview matches the export-side replay within the committed AA tolerance
        // (glyph edges are AA; the teal ink is dark and saturated, so its edges blend run-to-run).
        val fraction = differingFraction(hostRegion, referenceRegion)
        assertTrue(
            "styled preview drifted from the export replay (${"%.4f".format(fraction)} of the text region " +
                "differ > $AA_THRESHOLD) — preview and export must be one CanvasReplayer path.",
            fraction <= AA_THRESHOLD,
        )

        // The committed golden (ADR-055 §5). A no-op under a plain unit run; recorded on the pinned CI
        // image (`record-goldens.yml`) and gated by `verifyRoborazziDebug`.
        hostRegion.captureRoboImage(
            "$GOLDEN_DIR/styled_block_preview.png",
            RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f)),
        )
    }

    /** Non-white pixel count — a cheap "did anything render" guard. */
    private fun inkCount(bmp: Bitmap): Int {
        var n = 0
        for (y in 0 until bmp.height) for (x in 0 until bmp.width) if (bmp.getPixel(x, y) != Color.WHITE) n++
        return n
    }
}
