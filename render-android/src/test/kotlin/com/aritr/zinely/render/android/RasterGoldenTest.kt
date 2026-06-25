package com.aritr.zinely.render.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.Fit
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.aritr.zinely.core.render.DrawCommand
import com.aritr.zinely.core.render.DrawImage
import com.aritr.zinely.core.render.FillRect
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import kotlin.math.ceil

/**
 * G6a — the S3-closing **raster** parity proof for everything Robolectric `graphicsMode=NATIVE` can
 * truly rasterise from `android.graphics` primitives (ADR-028 §7.3): geometric fills, z-overlap, the
 * **rotated-clip order** (§3.1, the load-bearing replay claim), and the missing-asset placeholder. The
 * single [CanvasReplayer] is exercised at **three realistic scales** — screen density, a fractional
 * editor zoom, and the 300 DPI export — over those cases. Each carries **two proofs**:
 *
 *  1. **Behavioural pixel assertions** — deterministic, mode-independent (the real TDD red/green; they
 *     run under a plain `testDebugUnitTest`).
 *  2. A committed **Roborazzi golden** — [captureRoboImage] writes/reads the PNG at an explicit
 *     module-relative path, so `recordRoborazziDebug` (pinned CI image) produces it and
 *     `verifyRoborazziDebug` gates later runs against pixel drift. It is a no-op under a plain
 *     unit-test run, so the headless job stays green until the goldens are committed.
 *
 * **What is deliberately NOT here.** Real-PNG image fidelity (FIT/FILL/crop/rotation) cannot run
 * headless: in Robolectric 4.16.1 `BitmapRegionDecoder.decodeRegion` returns a blank bitmap (no native
 * region decode), and the crop-aware region decode is the very thing under test. Those cases live in
 * [ImageReplayInstrumentedTest] (real `android.graphics` codecs, compile-checked in CI, run on a
 * device) — the same authored-not-headless-CI split as the PDF write path (G5). Text + the §4.2
 * bundled-font map are G6b (they need a committed licence-clear TTF).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RasterGoldenTest {

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"

        // Realistic page→device scales (px per point), spanning the §7.3 promise.
        const val SCREEN = 2.5     // typical editor screen density
        const val ZOOM = 1.7       // a fractional editor zoom (catches sub-integer placement)
        val EXPORT = ExportScale.EXPORT_PX_PER_PT // 300/72 ≈ 4.1667 print export

        const val MISSING = "no-such-asset" // resolves to null → placeholder

        // Pure colours so geometric fills diff at exactly zero tolerance.
        val RED = ColorRgba(255, 0, 0, 255)
        val GREEN = ColorRgba(0, 255, 0, 255)
        val BLUE = ColorRgba(0, 0, 255, 255)
        val YELLOW = ColorRgba(255, 255, 0, 255)

        const val PLACEHOLDER_FILL = 0xFFE0E0E0.toInt() // ImageBlitter.PLACEHOLDER_FILL_ARGB (§5.4)

        fun exact() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0f),
        )

        // AA edges (rotated fills, placeholder cross) can jitter a small fraction of pixels run-to-run;
        // a tiny committed threshold absorbs that without masking placement bugs (Codex §7.3, no hShift).
        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }

    // The placeholder case is the only image command here, and it resolves to a MISSING asset (no
    // decode), so the source always returns null — real-PNG decode is exercised on device, not here.
    private val replayer = CanvasReplayer(imageBlitter = ImageBlitter(AssetBytesSource { null }))

    /** Renders [tape] onto a fresh ARGB_8888 bitmap at a uniform [s] px/pt (preview seam, offset 0). */
    private fun render(tape: List<DrawCommand>, sheet: PtSize, s: Double): Bitmap {
        val bmp = Bitmap.createBitmap(
            ceil(sheet.width * s).toInt().coerceAtLeast(1),
            ceil(sheet.height * s).toInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        replayer.replay(
            canvas = Canvas(bmp),
            tape = tape,
            pageToDevice = ExportScale.previewPageToDevice(s, PtPoint(0.0, 0.0)),
            pageClip = PtRect(0.0, 0.0, sheet.width, sheet.height),
            decodePxPerPt = s,
        )
        return bmp
    }

    /** Device pixel under a page-local point at scale [s]. */
    private fun Bitmap.pixelAt(pageX: Double, pageY: Double, s: Double): Int =
        getPixel(
            (pageX * s).toInt().coerceIn(0, width - 1),
            (pageY * s).toInt().coerceIn(0, height - 1),
        )

    private fun bg(sheet: PtSize) = FillRect(PtRect(0.0, 0.0, sheet.width, sheet.height), ColorRgba.WHITE)

    @Test
    fun fillQuadrants_acrossScales() {
        val sheet = PtSize(72.0, 72.0)
        val tape = listOf(
            bg(sheet),
            FillRect(PtRect(0.0, 0.0, 36.0, 36.0), RED),
            FillRect(PtRect(36.0, 0.0, 36.0, 36.0), GREEN),
            FillRect(PtRect(0.0, 36.0, 36.0, 36.0), BLUE),
            FillRect(PtRect(36.0, 36.0, 36.0, 36.0), YELLOW),
        )
        for ((label, s) in listOf("screen" to SCREEN, "zoom" to ZOOM, "export" to EXPORT)) {
            val bmp = render(tape, sheet, s)
            assertEquals("TL red @$label", Color.RED, bmp.pixelAt(18.0, 18.0, s))
            assertEquals("TR green @$label", Color.GREEN, bmp.pixelAt(54.0, 18.0, s))
            assertEquals("BL blue @$label", Color.BLUE, bmp.pixelAt(18.0, 54.0, s))
            assertEquals("BR yellow @$label", Color.YELLOW, bmp.pixelAt(54.0, 54.0, s))
            bmp.captureRoboImage("$GOLDEN_DIR/fills_quadrants_$label.png", exact())
        }
    }

    @Test
    fun zOverlap_lastDrawnWins() {
        val sheet = PtSize(72.0, 72.0)
        val tape = listOf(
            bg(sheet),
            FillRect(PtRect(8.0, 8.0, 40.0, 40.0), RED),    // back
            FillRect(PtRect(24.0, 24.0, 40.0, 40.0), BLUE), // front, overlaps the red
        )
        for ((label, s) in listOf("screen" to SCREEN, "export" to EXPORT)) {
            val bmp = render(tape, sheet, s)
            assertEquals("overlap is the front colour @$label", Color.BLUE, bmp.pixelAt(40.0, 40.0, s))
            assertEquals("red-only region @$label", Color.RED, bmp.pixelAt(12.0, 12.0, s))
            assertEquals("uncovered background @$label", Color.WHITE, bmp.pixelAt(68.0, 4.0, s))
            bmp.captureRoboImage("$GOLDEN_DIR/z_overlap_$label.png", exact())
        }
    }

    @Test
    fun rotatedFill_clipsToItsOwnRotatedFrame() {
        // The load-bearing §3.1 claim: localClip is applied AFTER concat(localToPage), so it rotates
        // with the element. A full-sheet RED fill is clipped to a 40pt local square and rotated 30°
        // about the page centre → the visible red is a *rotated* square; the page edges stay white.
        val sheet = PtSize(72.0, 72.0)
        val rotateAboutCentre = AffineTransform2D.translate(36.0, 36.0)
            .times(AffineTransform2D.rotateDeg(30.0))
            .times(AffineTransform2D.translate(-36.0, -36.0))
        val tape = listOf(
            bg(sheet),
            FillRect(
                rect = PtRect(0.0, 0.0, 72.0, 72.0), // overflows the clip on every side
                color = RED,
                localToPage = rotateAboutCentre,
                localClip = PtRect(16.0, 16.0, 40.0, 40.0), // centred on (36,36); half-diagonal ≈ 28.3pt
            ),
        )
        for ((label, s) in listOf("screen" to SCREEN, "export" to EXPORT)) {
            val bmp = render(tape, sheet, s)
            assertEquals("clip interior is filled @$label", Color.RED, bmp.pixelAt(36.0, 36.0, s))
            // (4,36) is 32pt from the centre — beyond the rotated square's reach → background survives.
            assertEquals("outside the rotated clip stays background @$label", Color.WHITE, bmp.pixelAt(4.0, 36.0, s))
            bmp.captureRoboImage("$GOLDEN_DIR/rotated_clip_$label.png", aa())
        }
    }

    @Test
    fun missingAsset_paintsPlaceholder() {
        val sheet = PtSize(72.0, 72.0)
        val placeholder = DrawImage(
            assetId = MISSING,
            crop = Crop.FULL,
            fit = Fit.FILL,
            box = PtRect(12.0, 12.0, 48.0, 48.0),
            localToPage = AffineTransform2D.identity(),
            localClip = PtRect(12.0, 12.0, 48.0, 48.0),
        )
        val tape = listOf(bg(sheet), placeholder)
        for ((label, s) in listOf("screen" to SCREEN, "export" to EXPORT)) {
            val bmp = render(tape, sheet, s)
            // (20,40) is off both diagonals of the broken-image cross → the flat placeholder fill.
            assertEquals("placeholder neutral fill @$label", PLACEHOLDER_FILL, bmp.pixelAt(20.0, 40.0, s))
            assertEquals("outside the box stays background @$label", Color.WHITE, bmp.pixelAt(4.0, 4.0, s))
            bmp.captureRoboImage("$GOLDEN_DIR/placeholder_$label.png", aa())
        }
    }
}
