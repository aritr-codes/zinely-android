package com.aritr.zinely.render.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextStyle
import com.aritr.zinely.core.render.DrawCommand
import com.aritr.zinely.core.render.DrawTextBox
import com.aritr.zinely.core.render.FillRect
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode
import kotlin.math.ceil

/**
 * **Document voices on the page (ADR-126): Plain and Book, each regular, bold, italic and bold italic.**
 *
 * [TextGoldenTest]'s harness: the bundled resolver feeding the one [CanvasReplayer], at the editor's
 * screen scale and at [ExportScale.EXPORT_PX_PER_PT], which is the raster the export writes (PDF vector
 * text itself is an instrumented concern, as [TextGoldenTest] records). Each golden holds one voice's four
 * faces, one per line, so a face that is missing, swapped or synthesised is visible in one picture.
 *
 * The behavioural half runs under a plain unit run: the four faces of a voice are four different
 * rasters, Book is not Plain, and a family this build does not know is Plain to the pixel.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VoiceGoldenTest {

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val SCREEN = 2.5
        val EXPORT = ExportScale.EXPORT_PX_PER_PT
        const val LINE = "Saturday, and nothing planned"
        val SHEET = PtSize(216.0, 112.0)

        /** regular, bold, italic, bold italic: (bold, italic), top to bottom. */
        val FACES = listOf(false to false, true to false, false to true, true to true)

        fun text() = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f))
    }

    private val replayer = CanvasReplayer(
        fontResolver = BundledFontResolver(RuntimeEnvironment.getApplication().assets),
    )

    private fun box(row: Int) = PtRect(8.0, 8.0 + row * 24.0, 200.0, 22.0)

    private fun tape(family: String, faces: List<Pair<Boolean, Boolean>> = FACES): List<DrawCommand> =
        listOf<DrawCommand>(FillRect(PtRect(0.0, 0.0, SHEET.width, SHEET.height), ColorRgba.WHITE)) +
            faces.mapIndexed { row, (bold, italic) ->
                val b = box(row)
                DrawTextBox(
                    text = LINE,
                    style = TextStyle(fontFamily = family, sizePt = 12.0, color = ColorRgba.BLACK, bold = bold, italic = italic),
                    boxWidthPt = b.width,
                    boxHeightPt = b.height,
                    localToPage = AffineTransform2D.translate(b.x, b.y),
                    localClip = PtRect(0.0, 0.0, b.width, b.height),
                )
            }

    private fun render(tape: List<DrawCommand>, s: Double): Bitmap {
        val bmp = Bitmap.createBitmap(ceil(SHEET.width * s).toInt(), ceil(SHEET.height * s).toInt(), Bitmap.Config.ARGB_8888)
        replayer.replay(
            canvas = Canvas(bmp),
            tape = tape,
            pageToDevice = ExportScale.previewPageToDevice(s, PtPoint(0.0, 0.0)),
            pageClip = PtRect(0.0, 0.0, SHEET.width, SHEET.height),
            decodePxPerPt = s,
        )
        return bmp
    }

    /** One face's line alone, on the first row, so two faces are compared like for like. */
    private fun face(family: String, bold: Boolean, italic: Boolean) = render(tape(family, listOf(bold to italic)), EXPORT)

    private fun Bitmap.ink(): Int {
        var n = 0
        for (y in 0 until height) for (x in 0 until width) if (getPixel(x, y) != Color.WHITE) n++
        return n
    }

    @Test
    fun plain_fourFaces_acrossScales() = capture("plain", "sans-serif")

    @Test
    fun book_fourFaces_acrossScales() = capture("book", "Fraunces")

    private fun capture(label: String, family: String) {
        for ((scale, s) in listOf("screen" to SCREEN, "export" to EXPORT)) {
            val bmp = render(tape(family), s)
            assertTrue("$label renders @$scale", bmp.ink() > 500)
            bmp.captureRoboImage("$GOLDEN_DIR/voice_${label}_faces_$scale.png", text())
        }
    }

    @Test
    fun each_voice_draws_four_different_faces_and_Book_is_not_Plain() {
        for (family in listOf("sans-serif", "Fraunces")) {
            val rasters = FACES.map { (bold, italic) -> face(family, bold, italic) }
            for (i in rasters.indices) for (j in i + 1 until rasters.size) {
                assertFalse("$family: faces ${FACES[i]} and ${FACES[j]} drew the same pixels", rasters[i].sameAs(rasters[j]))
            }
        }
        FACES.forEach { (bold, italic) ->
            assertFalse(
                "Book drew Plain's pixels (bold=$bold italic=$italic)",
                face("Fraunces", bold, italic).sameAs(face("sans-serif", bold, italic)),
            )
        }
    }

    @Test
    fun a_family_this_build_does_not_know_is_drawn_as_Plain_to_the_pixel() {
        // The older-build worst case in miniature, and the unknown-font state's promise ("shown in
        // Plain"): same faces, same line breaks, every style.
        assertTrue(render(tape("Averia Sans Libre"), EXPORT).sameAs(render(tape("sans-serif"), EXPORT)))
        // "Inter" by name is Plain too.
        assertTrue(render(tape("Inter"), EXPORT).sameAs(render(tape("sans-serif"), EXPORT)))
    }
}
