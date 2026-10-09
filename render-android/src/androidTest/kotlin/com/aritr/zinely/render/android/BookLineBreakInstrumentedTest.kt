package com.aritr.zinely.render.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.TextStyle
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * **Book breaks its lines in the same places on every Android this app supports (ADR-126; Brief 02).**
 *
 * Android 7's text shaper is older than a current phone's. One fixed, made-up paragraph is laid out in
 * each of the four Book faces through [SharedTextLayout], the one layout path every surface uses, and
 * the character offset at which each line ends is held to the values recorded below. Offsets, not
 * pixels, so there is no tolerance: a kerning pair or an "fi" the old shaper measures differently moves
 * a break and fails here.
 *
 * Instrumented on purpose. A Robolectric run would measure the host's shaper, and the claim is about the
 * phone's. Plain is included as the control: it has shipped on Android 7 since the first beta.
 *
 * The recorded values are [REFERENCE]. Each run also logs its own (`ZINELY_BOOK_BREAKS`) and writes the
 * drawn paragraphs to the test app's files directory, so the letter shapes can be read by eye from two
 * devices side by side.
 */
@RunWith(AndroidJUnit4::class)
class BookLineBreakInstrumentedTest {

    private companion object {
        /** Holds h, m, n, s, an ampersand, an "fi" and an "fl", as Brief 02 asks. Made up. */
        const val PARAGRAPH =
            "The first fig fell on the flagstones & Hamish the fishmonger, munching, swept the floor " +
                "of his shop on Saffron Lane; nine months on, the same shelf still smells of thyme."
        const val SIZE_PT = 12.0
        const val BOX_WIDTH_PT = 180.0

        /** label to (family, bold, italic). */
        val FACES = listOf(
            "book-regular" to Triple("Fraunces", false, false),
            "book-bold" to Triple("Fraunces", true, false),
            "book-italic" to Triple("Fraunces", false, true),
            "book-bolditalic" to Triple("Fraunces", true, true),
            "plain-regular" to Triple("sans-serif", false, false),
            "plain-italic" to Triple("sans-serif", false, true),
        )

        /**
         * Line-end offsets recorded on the reference device (Samsung SM-A176B, Android 16, API 36) on
         * 2026-10-08. The last offset of each face is the paragraph's length.
         */
        val REFERENCE: Map<String, List<Int>> = mapOf(
            "book-regular" to listOf(26, 50, 78, 111, 142, 170),
            "book-bold" to listOf(26, 50, 78, 103, 129, 161, 170),
            "book-italic" to listOf(37, 62, 95, 122, 154, 170),
            "book-bolditalic" to listOf(26, 50, 78, 111, 137, 164, 170),
            "plain-regular" to listOf(26, 50, 78, 111, 137, 164, 170),
            "plain-italic" to listOf(26, 50, 78, 111, 137, 164, 170),
        )
    }

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun layout(face: Triple<String, Boolean, Boolean>) = SharedTextLayout.build(
        text = PARAGRAPH,
        style = TextStyle(fontFamily = face.first, sizePt = SIZE_PT, color = ColorRgba.BLACK, bold = face.second, italic = face.third),
        boxWidthPt = BOX_WIDTH_PT,
        fontResolver = BundledFontResolver(context.assets),
    )

    @Test
    fun everyFaceBreaksItsLinesWhereTheReferenceDoes() {
        val measured = FACES.associate { (label, face) ->
            val l = layout(face)
            label to (0 until l.lineCount).map { l.getLineEnd(it) }
        }
        measured.forEach { (label, ends) ->
            Log.i("ZINELY_BOOK_BREAKS", "api=${Build.VERSION.SDK_INT} \"$label\" to listOf(${ends.joinToString()}),")
        }
        assertEquals("line-end offsets on API ${Build.VERSION.SDK_INT}", REFERENCE, measured)
    }

    @Test
    fun drawsEachFaceForReadingByEye() {
        // 3 px per layout unit of a point is plenty to read a letter shape: 180pt -> 540px wide.
        val scale = 3f / SharedTextLayout.LAYOUT_SCALE
        val layouts = FACES.map { (label, face) -> label to layout(face) }
        val gap = 24
        val height = layouts.sumOf { (it.second.height * scale).toInt() + gap } + gap
        val bmp = Bitmap.createBitmap((BOX_WIDTH_PT * 3).toInt() + 2 * gap, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp).apply { drawColor(Color.WHITE) }
        var y = gap.toFloat()
        layouts.forEach { (_, l) ->
            canvas.save()
            canvas.translate(gap.toFloat(), y)
            canvas.scale(scale, scale)
            l.draw(canvas)
            canvas.restore()
            y += l.height * scale + gap
        }
        val out = File(context.filesDir, "book-faces-api${Build.VERSION.SDK_INT}.png")
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        Log.i("ZINELY_BOOK_BREAKS", "wrote ${out.absolutePath}")
        // Not blank: something other than paper was drawn for every face.
        var ink = 0
        for (yy in 0 until bmp.height step 2) for (xx in 0 until bmp.width step 2) if (bmp.getPixel(xx, yy) != Color.WHITE) ink++
        assertTrue("the paragraphs drew almost nothing ($ink sampled pixels)", ink > 2000)
    }
}
