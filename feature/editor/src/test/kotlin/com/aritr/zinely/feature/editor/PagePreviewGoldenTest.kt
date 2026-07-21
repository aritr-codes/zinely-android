package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextAlign
import com.aritr.zinely.core.model.TextStyle
import com.aritr.zinely.core.render.DrawCommand
import com.aritr.zinely.core.render.DrawImage
import com.aritr.zinely.core.render.DrawTextBox
import com.aritr.zinely.core.render.FillRect
import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.Fit
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import kotlin.math.roundToInt

/**
 * **Editor-preview goldens (F4 Increment 1) — the fourth surface's committed reference images.**
 *
 * DoD 3 requires all four surfaces to render from the same display list and be golden-verified. The
 * page-image surface has had committed goldens since [ADR-028] G6 (`RasterGoldenTest`); the editor
 * preview had **none** — `PagePreviewParityTest` proves host-vs-replay pixel equality but commits no
 * reference image, so a change that moved *both* paths identically would pass unnoticed. These goldens
 * close that: they are a fixed picture of what the editor is supposed to look like, not a comparison
 * between two things that could drift together.
 *
 * **The cases mirror `RasterGoldenTest` deliberately** — same tapes, same scales, same labels, same
 * tolerances. That is what makes "same display list, four surfaces" checkable by a human: the preview
 * golden and the raster golden for a given case are pictures of the same page, and a reviewer can hold
 * them side by side. The two suites are *not* diffed automatically (the Compose host paints on a themed
 * window at its own placement, so the rasters are not byte-comparable) — [PagePreviewParityTest] owns
 * that proof, and these own the regression baseline.
 *
 * **Capture path** — the same one every `:feature:editor` golden uses, for the reasons recorded in
 * [ComposeHostRaster]: draw the laid-out decor `View` straight onto a `Canvas(bitmap)`, crop to the
 * host's actual placed bounds, then hand the bitmap to Roborazzi. `captureToImage()` times out under
 * Robolectric NATIVE, and `captureRoboImage` on a *node* is a no-op outside the roborazzi tasks.
 *
 * Recorded and verified on the pinned CI runner (`ubuntu-24.04`, `TZ=UTC`, `sdk=34`, density 1.0);
 * these PNGs are only valid against that baseline, and a re-record is a deliberate reviewed act.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PagePreviewGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"

        /** The same scales `RasterGoldenTest` uses, so the two surfaces' goldens are comparable. */
        const val SCREEN = 2.5 // typical editor screen density
        const val ZOOM = 1.7 // a fractional editor zoom (catches sub-integer placement)

        val RED = ColorRgba(255, 0, 0, 255)
        val GREEN = ColorRgba(0, 255, 0, 255)
        val BLUE = ColorRgba(0, 0, 255, 255)
        val YELLOW = ColorRgba(255, 255, 0, 255)

        /** Geometric fills must diff at zero tolerance — a moved edge is a real bug. */
        fun exact() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0f),
        )

        /** AA edges (rotated fills, glyph edges, the placeholder cross) jitter a few pixels run to run. */
        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }

    private fun bg(sheet: PtSize) = FillRect(PtRect(0.0, 0.0, sheet.width, sheet.height), ColorRgba.WHITE)

    /**
     * Renders [tape] through the **Compose host** at [s] px/pt and returns the host's own pixels.
     *
     * The size assertion is load-bearing: if the host were not placed at exactly the page's device
     * extent, the crop would silently capture the wrong region and the golden would encode that
     * mistake permanently.
     */
    private fun previewBitmap(tape: List<DrawCommand>, sheet: PtSize, s: Double): Bitmap {
        val w = (sheet.width * s).roundToInt()
        val h = (sheet.height * s).roundToInt()
        assertEquals(
            "test display density must be 1.0 for dp==px sizing; got ${composeRule.density.density}",
            1.0f, composeRule.density.density, 0.0001f,
        )

        composeRule.setContent {
            ZinelyTheme {
                PagePreview(
                    tape = tape,
                    sheet = sheet,
                    screenPxPerPt = s.toFloat(),
                    pageOffset = PtPoint(0.0, 0.0),
                    modifier = Modifier.size(w.dp, h.dp),
                )
            }
        }
        composeRule.waitForIdle()

        val bounds = composeRule.onNodeWithTag(PagePreviewTestTag).fetchSemanticsNode().boundsInRoot
        assertEquals("host placed width != page device px", w, bounds.width.roundToInt())
        assertEquals("host placed height != page device px", h, bounds.height.roundToInt())

        val full = composeRule.activity.window.decorView.rasterizeToBitmap()
        return Bitmap.createBitmap(
            full,
            bounds.left.roundToInt(), bounds.top.roundToInt(),
            bounds.width.roundToInt(), bounds.height.roundToInt(),
        )
    }

    /** Device pixel under a page-local point at scale [s]. */
    private fun Bitmap.pixelAt(pageX: Double, pageY: Double, s: Double): Int =
        getPixel(
            (pageX * s).toInt().coerceIn(0, width - 1),
            (pageY * s).toInt().coerceIn(0, height - 1),
        )

    private fun quadrantTape(sheet: PtSize) = listOf(
        bg(sheet),
        FillRect(PtRect(0.0, 0.0, 36.0, 36.0), RED),
        FillRect(PtRect(36.0, 0.0, 36.0, 36.0), GREEN),
        FillRect(PtRect(0.0, 36.0, 36.0, 36.0), BLUE),
        FillRect(PtRect(36.0, 36.0, 36.0, 36.0), YELLOW),
    )

    /**
     * Asserts quadrant placement, then commits the picture. The assertion runs first on purpose: a
     * golden of a wrong render is worse than no golden, because it makes the wrongness the reference.
     *
     * **One scale per test method.** `ComposeTestRule.setContent` may be called only once per test
     * ("has already set content"), so the multi-scale `for` loop `RasterGoldenTest` uses — which draws
     * straight to a bitmap with no Compose rule — cannot be mirrored here. Each scale gets its own test.
     */
    private fun captureQuadrants(label: String, s: Double) {
        val sheet = PtSize(72.0, 72.0)
        val bmp = previewBitmap(quadrantTape(sheet), sheet, s)
        assertEquals("TL red @$label", Color.RED, bmp.pixelAt(18.0, 18.0, s))
        assertEquals("TR green @$label", Color.GREEN, bmp.pixelAt(54.0, 18.0, s))
        assertEquals("BL blue @$label", Color.BLUE, bmp.pixelAt(18.0, 54.0, s))
        assertEquals("BR yellow @$label", Color.YELLOW, bmp.pixelAt(54.0, 54.0, s))
        bmp.captureRoboImage("$GOLDEN_DIR/preview_fills_quadrants_$label.png", exact())
    }

    @Test
    fun fillQuadrants_atScreenScale() = captureQuadrants("screen", SCREEN)

    /** A fractional zoom — catches sub-integer placement the integral screen scale would hide. */
    @Test
    fun fillQuadrants_atFractionalZoom() = captureQuadrants("zoom", ZOOM)

    @Test
    fun zOverlap_lastDrawnWins() {
        val sheet = PtSize(72.0, 72.0)
        val tape = listOf(
            bg(sheet),
            FillRect(PtRect(8.0, 8.0, 40.0, 40.0), RED), // back
            FillRect(PtRect(24.0, 24.0, 40.0, 40.0), BLUE), // front, overlaps the red
        )
        val bmp = previewBitmap(tape, sheet, SCREEN)
        assertEquals("overlap is the front colour", Color.BLUE, bmp.pixelAt(40.0, 40.0, SCREEN))
        assertEquals("red-only region", Color.RED, bmp.pixelAt(12.0, 12.0, SCREEN))
        assertEquals("uncovered background", Color.WHITE, bmp.pixelAt(68.0, 4.0, SCREEN))
        bmp.captureRoboImage("$GOLDEN_DIR/preview_z_overlap_screen.png", exact())
    }

    @Test
    fun rotatedFill_clipsToItsOwnRotatedFrame() {
        // The load-bearing ADR-028 §3.1 claim, goldened on the preview surface: localClip is applied
        // AFTER concat(localToPage), so the clip rotates with the element rather than staying axis-aligned.
        val sheet = PtSize(72.0, 72.0)
        // Same construction as RasterGoldenTest's rotated case, so the two surfaces' goldens are
        // pictures of the same page rather than of two different rotations.
        val rotateAboutCentre = AffineTransform2D.translate(36.0, 36.0)
            .times(AffineTransform2D.rotateDeg(30.0))
            .times(AffineTransform2D.translate(-36.0, -36.0))
        val tape = listOf(
            bg(sheet),
            FillRect(
                rect = PtRect(0.0, 0.0, 72.0, 72.0), // overflows the clip on every side
                color = RED,
                localToPage = rotateAboutCentre,
                localClip = PtRect(16.0, 16.0, 40.0, 40.0),
            ),
        )
        val bmp = previewBitmap(tape, sheet, SCREEN)
        assertEquals("clip interior is filled", Color.RED, bmp.pixelAt(36.0, 36.0, SCREEN))
        assertEquals("outside the rotated clip stays background", Color.WHITE, bmp.pixelAt(4.0, 36.0, SCREEN))
        bmp.captureRoboImage("$GOLDEN_DIR/preview_rotated_clip_screen.png", aa())
    }

    @Test
    fun text_wrapsAndAligns() {
        // Text is the highest-risk divergence between preview and export (Compose text vs Canvas text is
        // the classic trap, ADR-028 §4) — so the preview surface carries its own text reference.
        val sheet = PtSize(120.0, 72.0)
        val tape = listOf(
            bg(sheet),
            DrawTextBox(
                text = "Hand folded zines print sharp",
                style = TextStyle(fontFamily = "Inter", sizePt = 11.0, color = ColorRgba.BLACK),
                boxWidthPt = 100.0,
                boxHeightPt = 52.0,
                localToPage = AffineTransform2D.translate(10.0, 10.0),
                localClip = PtRect(0.0, 0.0, 100.0, 52.0),
            ),
        )
        val bmp = previewBitmap(tape, sheet, SCREEN)
        bmp.captureRoboImage("$GOLDEN_DIR/preview_text_wrap_screen.png", aa())
    }

    @Test
    fun text_centreAligned() {
        val sheet = PtSize(120.0, 72.0)
        val tape = listOf(
            bg(sheet),
            DrawTextBox(
                text = "Fold here",
                style = TextStyle(
                    fontFamily = "Inter",
                    sizePt = 14.0,
                    color = ColorRgba.BLACK,
                    align = TextAlign.CENTER,
                    bold = true,
                ),
                boxWidthPt = 100.0,
                boxHeightPt = 28.0,
                localToPage = AffineTransform2D.translate(10.0, 24.0),
                localClip = PtRect(0.0, 0.0, 100.0, 28.0),
            ),
        )
        val bmp = previewBitmap(tape, sheet, SCREEN)
        bmp.captureRoboImage("$GOLDEN_DIR/preview_text_centre_screen.png", aa())
    }

    @Test
    fun missingAsset_paintsPlaceholder() {
        // The defined missing-asset placeholder (ADR-028 §5.4) must look the same in the editor as it
        // does on paper — a user who sees the broken-image cross should get it in the export too.
        val sheet = PtSize(72.0, 72.0)
        val tape = listOf(
            bg(sheet),
            DrawImage(
                assetId = "absent",
                box = PtRect(12.0, 12.0, 48.0, 48.0),
                crop = Crop.FULL,
                fit = Fit.FIT,
                localToPage = AffineTransform2D.identity(),
                localClip = null,
            ),
        )
        val bmp = previewBitmap(tape, sheet, SCREEN)
        bmp.captureRoboImage("$GOLDEN_DIR/preview_placeholder_screen.png", aa())
    }
}
