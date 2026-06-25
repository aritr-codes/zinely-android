package com.aritr.zinely.render.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.Fit
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.render.DrawCommand
import com.aritr.zinely.core.render.DrawImage
import com.aritr.zinely.core.render.FillRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.ceil

/**
 * Real-PNG image fidelity for the shared [ImageBlitter]/[CanvasReplayer] (ADR-028 §5, §7.3) — the cases
 * that the headless [RasterGoldenTest] cannot prove because Robolectric 4.16.1's
 * `BitmapRegionDecoder.decodeRegion` returns a blank bitmap (no native region decode). This runs
 * **instrumented** on a real device, where `android.graphics`' codecs decode the committed master for
 * real, exercising the crop-aware region decode that is the actual thing under test.
 *
 * Same authored-not-headless-CI split as the PDF write path ([PdfExportInstrumentedTest]) and
 * `:data-android`'s `Os.fsync` durability tests: it is **compile-checked** in the SDK CI job
 * (`compileDebugAndroidTestKotlin`) so it can't bitrot, and executed once an emulator/connected job
 * exists. Pixel assertions (not Roborazzi goldens) keep it consistent with [PdfExportInstrumentedTest];
 * golden recording would need a pinned device image the project does not yet run.
 *
 *  1. FIT letterboxes the square master inside a landscape box (white pillarbox at the sides).
 *  2. FILL covers the whole box, overflow clipped to the box.
 *  3. A tiny crop from the large master at 300 DPI resolves real detail (crop-aware region decode,
 *     §5.1 — not starved/grey).
 *  4. A rotated FILL image clips to its own rotated frame (the page corners stay background).
 */
@RunWith(AndroidJUnit4::class)
class ImageReplayInstrumentedTest {

    private companion object {
        const val MASTER = "image-master"
        const val FIXTURE = "fixtures/image_master_1024.png"
        val SCREEN = 2.5
        val EXPORT = ExportScale.EXPORT_PX_PER_PT
    }

    /** Fresh, independent stream per call from the androidTest APK assets (the two-open contract). */
    private val assetBytes = AssetBytesSource { id ->
        if (id == MASTER) {
            InstrumentationRegistry.getInstrumentation().context.assets.open(FIXTURE)
        } else {
            null
        }
    }
    private val replayer = CanvasReplayer(imageBlitter = ImageBlitter(assetBytes))

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

    private fun Bitmap.pixelAt(pageX: Double, pageY: Double, s: Double): Int =
        getPixel((pageX * s).toInt().coerceIn(0, width - 1), (pageY * s).toInt().coerceIn(0, height - 1))

    private fun bg(sheet: PtSize) = FillRect(PtRect(0.0, 0.0, sheet.width, sheet.height), ColorRgba.WHITE)

    private fun image(
        box: PtRect,
        fit: Fit,
        crop: Crop = Crop.FULL,
        localToPage: AffineTransform2D = AffineTransform2D.identity(),
    ) = DrawImage(MASTER, crop, fit, box, localToPage, localClip = box)

    @Test
    fun imageFit_letterboxesInsideBox() {
        val sheet = PtSize(72.0, 48.0)
        val tape = listOf(bg(sheet), image(PtRect(0.0, 0.0, 72.0, 48.0), Fit.FIT))
        for (s in listOf(SCREEN, EXPORT)) {
            val bmp = render(tape, sheet, s)
            assertEquals("left pillarbox stays background", Color.WHITE, bmp.pixelAt(3.0, 24.0, s))
            assertNotEquals("image covers the centre", Color.WHITE, bmp.pixelAt(30.0, 20.0, s))
        }
    }

    @Test
    fun imageFill_coversBoxAndClipsOverflow() {
        val sheet = PtSize(72.0, 48.0)
        val tape = listOf(bg(sheet), image(PtRect(0.0, 0.0, 72.0, 48.0), Fit.FILL))
        for (s in listOf(SCREEN, EXPORT)) {
            val bmp = render(tape, sheet, s)
            assertNotEquals("TL covered", Color.WHITE, bmp.pixelAt(2.0, 2.0, s))
            assertNotEquals("TR covered", Color.WHITE, bmp.pixelAt(69.0, 2.0, s))
            assertNotEquals("BL covered", Color.WHITE, bmp.pixelAt(2.0, 45.0, s))
            assertNotEquals("BR covered", Color.WHITE, bmp.pixelAt(69.0, 45.0, s))
        }
    }

    @Test
    fun imageTinyCrop_fromLargeMaster_at300Dpi() {
        val sheet = PtSize(72.0, 72.0)
        val tape = listOf(
            bg(sheet),
            image(PtRect(0.0, 0.0, 72.0, 72.0), Fit.FILL, crop = Crop(0.0, 0.0, 0.12, 0.12)),
        )
        val bmp = render(tape, sheet, EXPORT)
        val p = bmp.pixelAt(20.0, 20.0, EXPORT)
        assertTrue(
            "tiny crop resolves the master's red region (not starved/grey): $p",
            Color.red(p) > 120 && Color.red(p) > Color.green(p) && Color.red(p) > Color.blue(p),
        )
    }

    @Test
    fun rotatedImage_clipsToItsOwnRotatedFrame() {
        val sheet = PtSize(72.0, 72.0)
        val rotateAboutCentre = AffineTransform2D.translate(20.0, 20.0)
            .times(AffineTransform2D.rotateDeg(30.0))
            .times(AffineTransform2D.translate(-20.0, -20.0))
        val localToPage = AffineTransform2D.translate(16.0, 16.0).times(rotateAboutCentre)
        val tape = listOf(bg(sheet), image(PtRect(0.0, 0.0, 40.0, 40.0), Fit.FILL, localToPage = localToPage))
        val bmp = render(tape, sheet, EXPORT)
        assertEquals("corner outside the rotated rect stays background", Color.WHITE, bmp.pixelAt(2.0, 2.0, EXPORT))
        assertNotEquals("the rotated element covers the page centre", Color.WHITE, bmp.pixelAt(36.0, 36.0, EXPORT))
    }
}
