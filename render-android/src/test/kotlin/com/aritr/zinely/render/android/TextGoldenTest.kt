package com.aritr.zinely.render.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextAlign
import com.aritr.zinely.core.model.TextStyle
import com.aritr.zinely.core.render.DrawCommand
import com.aritr.zinely.core.render.DrawTextBox
import com.aritr.zinely.core.render.FillRect
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode
import kotlin.math.ceil

/**
 * G6b — headless **text** parity goldens (ADR-028 §7.3, §4). The bundled-Inter [BundledFontResolver]
 * feeds the shared point-space [SharedTextLayout]/[CanvasReplayer], so wrapping, alignment, weight, and
 * the box-overflow clip are proven to rasterise identically under Robolectric NATIVE (the
 * [FontRenderingProbeTest] established that NATIVE renders the bundled typeface). Each case = a
 * mode-independent behavioural assertion + a committed Roborazzi golden.
 *
 * Glyph coverage is guarded separately by [FontCoverageTest] (cmap, not fallback). PDF vector-text
 * structure stays an instrumented concern (no `PdfDocument` under NATIVE).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TextGoldenTest {

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val SCREEN = 2.5
        val EXPORT = ExportScale.EXPORT_PX_PER_PT

        // Text is all AA edges; a small committed threshold absorbs sub-pixel jitter on the pinned image.
        fun text() = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.03f))
    }

    private val replayer = CanvasReplayer(
        fontResolver = BundledFontResolver(RuntimeEnvironment.getApplication().assets),
    )

    private fun style(sizePt: Double, align: TextAlign = TextAlign.START, bold: Boolean = false, italic: Boolean = false) =
        TextStyle(fontFamily = "sans-serif", sizePt = sizePt, color = ColorRgba.BLACK, align = align, bold = bold, italic = italic)

    private fun tape(sheet: PtSize, box: PtRect, text: String, style: TextStyle): List<DrawCommand> = listOf(
        FillRect(PtRect(0.0, 0.0, sheet.width, sheet.height), ColorRgba.WHITE),
        DrawTextBox(
            text = text,
            style = style,
            boxWidthPt = box.width,
            boxHeightPt = box.height,
            localToPage = AffineTransform2D.translate(box.x, box.y),
            localClip = PtRect(0.0, 0.0, box.width, box.height),
        ),
    )

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

    /** Non-white pixel count inside a page-local rect at scale [s]. */
    private fun Bitmap.inkIn(rect: PtRect, s: Double): Int {
        var n = 0
        val x0 = (rect.x * s).toInt().coerceIn(0, width - 1)
        val y0 = (rect.y * s).toInt().coerceIn(0, height - 1)
        val x1 = (rect.right * s).toInt().coerceIn(x0, width)
        val y1 = (rect.bottom * s).toInt().coerceIn(y0, height)
        for (y in y0 until y1) for (x in x0 until x1) if (getPixel(x, y) != Color.WHITE) n++
        return n
    }

    /** Leftmost inked device-x inside a page-local rect, or -1 if blank. */
    private fun Bitmap.leftmostInkX(rect: PtRect, s: Double): Int {
        val x0 = (rect.x * s).toInt().coerceIn(0, width - 1)
        val y0 = (rect.y * s).toInt().coerceIn(0, height - 1)
        val x1 = (rect.right * s).toInt().coerceIn(x0, width)
        val y1 = (rect.bottom * s).toInt().coerceIn(y0, height)
        for (x in x0 until x1) for (y in y0 until y1) if (getPixel(x, y) != Color.WHITE) return x
        return -1
    }

    @Test
    fun wraps_acrossScales() {
        val sheet = PtSize(96.0, 72.0)
        val box = PtRect(8.0, 8.0, 80.0, 56.0)
        val t = tape(sheet, box, "The quick brown fox jumps over the lazy dog", style(13.0))
        for ((label, s) in listOf("screen" to SCREEN, "export" to EXPORT)) {
            val bmp = render(t, sheet, s)
            assertTrue("text renders @$label", bmp.inkIn(box, s) > 50)
            // Wrapped to ≥2 lines ⇒ the lower half of the box also has ink.
            assertTrue("text wrapped to multiple lines @$label", bmp.inkIn(PtRect(8.0, 36.0, 80.0, 28.0), s) > 10)
            bmp.captureRoboImage("$GOLDEN_DIR/text_wrap_$label.png", text())
        }
    }

    @Test
    fun alignment_movesTextLeftToRight() {
        val sheet = PtSize(96.0, 40.0)
        val box = PtRect(8.0, 6.0, 80.0, 28.0)
        val lefts = mutableMapOf<String, Int>()
        for (align in listOf(TextAlign.START, TextAlign.CENTER, TextAlign.END)) {
            val bmp = render(tape(sheet, box, "Title", style(22.0, align = align)), sheet, EXPORT)
            assertTrue("aligned text renders ($align)", bmp.inkIn(box, EXPORT) > 50)
            lefts[align.name] = bmp.leftmostInkX(box, EXPORT)
            bmp.captureRoboImage("$GOLDEN_DIR/text_align_${align.name.lowercase()}_export.png", text())
        }
        assertTrue("START left of CENTER left of END", lefts["START"]!! < lefts["CENTER"]!! && lefts["CENTER"]!! < lefts["END"]!!)
    }

    @Test
    fun bold_isHeavierThanRegular() {
        val sheet = PtSize(96.0, 40.0)
        val box = PtRect(8.0, 6.0, 80.0, 28.0)
        val regular = render(tape(sheet, box, "Weight", style(22.0)), sheet, EXPORT)
        val bold = render(tape(sheet, box, "Weight", style(22.0, bold = true)), sheet, EXPORT)
        val regularInk = regular.inkIn(box, EXPORT)
        val boldInk = bold.inkIn(box, EXPORT)
        assertTrue("both render", regularInk > 50 && boldInk > 50)
        assertTrue("bold lays down more ink than regular ($boldInk vs $regularInk)", boldInk > regularInk)
        regular.captureRoboImage("$GOLDEN_DIR/text_regular_export.png", text())
        bold.captureRoboImage("$GOLDEN_DIR/text_bold_export.png", text())
    }

    @Test
    fun italic_renders() {
        val sheet = PtSize(96.0, 40.0)
        val box = PtRect(8.0, 6.0, 80.0, 28.0)
        val bmp = render(tape(sheet, box, "Italic", style(22.0, italic = true)), sheet, EXPORT)
        assertTrue("italic renders", bmp.inkIn(box, EXPORT) > 50)
        bmp.captureRoboImage("$GOLDEN_DIR/text_italic_export.png", text())
    }

    @Test
    fun overflow_isClippedToBox() {
        // Three lines of text into a 16pt-tall box → only the top is visible; the rest is clipped by
        // the box (localClip), NOT truncated by maxLines — so nothing renders below the box bottom.
        val sheet = PtSize(96.0, 64.0)
        val box = PtRect(8.0, 8.0, 80.0, 16.0)
        val bmp = render(tape(sheet, box, "Alpha Bravo Charlie Delta Echo Foxtrot Golf", style(12.0)), sheet, EXPORT)
        assertTrue("top line is visible", bmp.inkIn(box, EXPORT) > 30)
        assertEquals("nothing renders below the clipped box", 0, bmp.inkIn(PtRect(8.0, 24.0, 80.0, 20.0), EXPORT))
        bmp.captureRoboImage("$GOLDEN_DIR/text_overflow_clip_export.png", text())
    }
}
