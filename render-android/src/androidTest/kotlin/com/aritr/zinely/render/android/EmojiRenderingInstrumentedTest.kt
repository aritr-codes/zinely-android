package com.aritr.zinely.render.android

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Debug
import android.text.Spanned
import android.text.style.ReplacementSpan
import android.util.Log
import androidx.emoji2.text.EmojiCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextStyle
import com.aritr.zinely.core.render.DrawCommand
import com.aritr.zinely.core.render.DrawTextBox
import com.aritr.zinely.core.render.FillRect
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Real-Skia proof for ADR-112's bundled, offline emoji renderer. */
@RunWith(AndroidJUnit4::class)
class EmojiRenderingInstrumentedTest {
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val sheet = PtSize(72.0, 72.0)
    private val replayer by lazy { CanvasReplayer(BundledFontResolver(context.assets)) }
    private val raster by lazy { RasterPageRenderer(replayer) }
    private val pdf by lazy { PdfPageRenderer(replayer) }
    private val rasterizer = PdfRasterizer()

    @Before
    fun awaitBundledFont() {
        val pssBeforeKb = Debug.getPss()
        val startedNs = System.nanoTime()
        EmojiRendering.initialize(context)
        val compat = EmojiCompat.get()
        if (compat.loadState != EmojiCompat.LOAD_STATE_SUCCEEDED) {
            val ready = CountDownLatch(1)
            compat.registerInitCallback(object : EmojiCompat.InitCallback() {
                override fun onInitialized() = ready.countDown()
                override fun onFailed(throwable: Throwable?) = ready.countDown()
            })
            assertTrue("bundled emoji metadata did not load", ready.await(10, TimeUnit.SECONDS))
        }
        assertEquals(EmojiCompat.LOAD_STATE_SUCCEEDED, compat.loadState)
        val loadMs = (System.nanoTime() - startedNs) / 1_000_000.0
        val pssDeltaKb = Debug.getPss() - pssBeforeKb
        Log.i("ZINELY_EMOJI", "fontLoadMs=${"%.2f".format(loadMs)} pssDeltaKb=$pssDeltaKb")
        assertTrue("bundled emoji load added an unbounded ${pssDeltaKb}KiB", pssDeltaKb < 32 * 1024)
    }

    @Test
    fun launchCorpusUsesBundledSpansAndPrintsAtSupportedSizes() {
        val corpus = listOf("🙂", "👍🏽", "👩🏽‍🎨", "👨‍👩‍👧‍👦", "🇮🇳", "1️⃣", "❤️", "🐦‍🔥")
        corpus.forEach { emoji ->
            val processed = EmojiRendering.process("A $emoji Z")
            assertTrue("$emoji must be processed as styled text", processed is Spanned)
            val spans = (processed as Spanned).getSpans(0, processed.length, ReplacementSpan::class.java)
            assertTrue("$emoji must use the bundled Emoji span", spans.isNotEmpty())
        }

        val text = "Paper, ink, photos — and words."
        assertEquals(text, EmojiRendering.process(text).toString())

        var priorRasterInk = 0
        var priorPdfInk = 0
        listOf(10.0, 24.0, 48.0).forEach { sizePt ->
            val tape = emojiTape(sizePt)
            val rasterBitmap = raster.render(tape, sheet)
            val pdfBitmap = rasterizer.rasterize(pdf.render(tape, sheet), cacheDir = context.cacheDir)
            val rasterInk = rasterBitmap.nonWhitePixels()
            val pdfInk = pdfBitmap.nonWhitePixels()
            assertTrue("$sizePt pt emoji must rasterise", rasterInk > 20)
            assertTrue("$sizePt pt emoji must print to PDF", pdfInk > 20)
            assertTrue("raster emoji must grow at $sizePt pt", rasterInk > priorRasterInk)
            assertTrue("PDF emoji must grow at $sizePt pt", pdfInk > priorPdfInk)
            priorRasterInk = rasterInk
            priorPdfInk = pdfInk
        }

        val plainTape = textTape("Paper and ink", 24.0)
        val emojiTape = textTape("Paper 🙂 ink", 24.0)
        raster.render(plainTape, sheet)
        raster.render(emojiTape, sheet)
        val plainMs = averageRasterMs(plainTape)
        val emojiMs = averageRasterMs(emojiTape)
        Log.i("ZINELY_EMOJI", "raster24PlainMs=${"%.3f".format(plainMs)} raster24EmojiMs=${"%.3f".format(emojiMs)}")
        assertTrue("24 pt emoji raster averaged ${emojiMs}ms", emojiMs < 500.0)
        assertTrue("emoji raster cost is unbounded ($emojiMs ms vs $plainMs ms)", emojiMs < maxOf(plainMs * 20.0, 50.0))
    }

    private fun emojiTape(sizePt: Double): List<DrawCommand> = listOf(
        FillRect(PtRect(0.0, 0.0, 72.0, 72.0), ColorRgba.WHITE),
        DrawTextBox(
            text = "🙂",
            style = TextStyle(fontFamily = "Inter", sizePt = sizePt, color = ColorRgba.BLACK),
            boxWidthPt = 64.0,
            boxHeightPt = 64.0,
            localToPage = AffineTransform2D.translate(4.0, 4.0),
            localClip = PtRect(0.0, 0.0, 64.0, 64.0),
        ),
    )

    private fun textTape(text: String, sizePt: Double): List<DrawCommand> = listOf(
        FillRect(PtRect(0.0, 0.0, 72.0, 72.0), ColorRgba.WHITE),
        DrawTextBox(
            text = text,
            style = TextStyle(fontFamily = "Inter", sizePt = sizePt, color = ColorRgba.BLACK),
            boxWidthPt = 64.0,
            boxHeightPt = 64.0,
            localToPage = AffineTransform2D.translate(4.0, 4.0),
            localClip = PtRect(0.0, 0.0, 64.0, 64.0),
        ),
    )

    private fun averageRasterMs(tape: List<DrawCommand>): Double {
        val startedNs = System.nanoTime()
        repeat(10) { raster.render(tape, sheet) }
        return (System.nanoTime() - startedNs) / 10_000_000.0
    }

    private fun Bitmap.nonWhitePixels(): Int {
        var count = 0
        for (y in 0 until height) for (x in 0 until width) {
            if (getPixel(x, y) != Color.WHITE) count++
        }
        return count
    }
}
