package com.aritr.zinely.render.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.render.FillRect
import com.aritr.zinely.core.render.SceneRenderer
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import kotlin.math.ceil

/**
 * The two hand-cut frames (`v21-bench.html` A32) reaching pixels through the one replayer: each frame in
 * two inks, flipped and unflipped, at the screen and the export scale.
 *
 * One sheet per frame, four cells: ink one as cut, ink one mirrored, ink two flipped top to bottom, ink
 * two flipped both ways. The route is `Page -> SceneRenderer -> CanvasReplayer`, the same one the Bench,
 * page nav, Read and the PDF take, so this is the fold and the fill rule together.
 *
 * As in [RasterGoldenTest], each case carries pixel assertions that run under a plain unit-test run, and
 * a golden that only `verifyRoborazziDebug` compares.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FrameGoldenTest {

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val SCREEN = 2.5
        val EXPORT = ExportScale.EXPORT_PX_PER_PT

        const val CELL = 60.0
        val sheet = PtSize(144.0, 144.0)

        val BERRY = ColorRgba(200, 40, 90, 255)
        val SEA = ColorRgba(40, 80, 160, 255)

        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }

    private data class Cell(val x: Double, val y: Double, val ink: ColorRgba, val mirrored: Boolean, val flipped: Boolean)

    private val cells = listOf(
        Cell(8.0, 8.0, BERRY, mirrored = false, flipped = false),
        Cell(76.0, 8.0, BERRY, mirrored = true, flipped = false),
        Cell(8.0, 76.0, SEA, mirrored = false, flipped = true),
        Cell(76.0, 76.0, SEA, mirrored = true, flipped = true),
    )

    private val replayer = CanvasReplayer()

    private fun render(supplyId: String, s: Double): Bitmap {
        val page = Page(
            index = 0,
            role = PageRole.INTERIOR,
            elements = cells.mapIndexed { i, c ->
                DecorElement("f$i", Transform(c.x, c.y, CELL, CELL), i, supplyId, c.ink, c.mirrored, c.flipped)
            },
        )
        val tape = listOf(FillRect(PtRect(0.0, 0.0, sheet.width, sheet.height), ColorRgba.WHITE)) +
            SceneRenderer.render(page, sheet, DocumentDefaults())
        val bmp = Bitmap.createBitmap(
            ceil(sheet.width * s).toInt(),
            ceil(sheet.height * s).toInt(),
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

    /** The pixel under unit point ([u], [v]) of [cell]'s box. */
    private fun Bitmap.at(cell: Cell, u: Double, v: Double, s: Double): Int =
        getPixel(((cell.x + CELL * u) * s).toInt(), ((cell.y + CELL * v) * s).toInt())

    /** A row of [cell]'s pixels at unit height [v], read left to right. */
    private fun Bitmap.row(cell: Cell, v: Double, s: Double): List<Int> {
        val n = 120
        return (0 until n).map { i -> at(cell, (i + 0.5) / n, v, s) }
    }

    private fun capture(supplyId: String, label: String, band: Double) {
        for ((scale, s) in listOf("screen" to SCREEN, "export" to EXPORT)) {
            val bmp = render(supplyId, s)
            for (c in cells) {
                val ink = Color.argb(255, c.ink.r, c.ink.g, c.ink.b)
                assertEquals("$supplyId hole @$scale", Color.WHITE, bmp.at(c, 0.5, 0.5, s))
                // Every side has a band, so these hold whichever way the piece is flipped.
                assertEquals("$supplyId left band @$scale", ink, bmp.at(c, band, 0.5, s))
                assertEquals("$supplyId right band @$scale", ink, bmp.at(c, 1.0 - band, 0.5, s))
                assertEquals("$supplyId top band @$scale", ink, bmp.at(c, 0.5, band, s))
                assertEquals("$supplyId bottom band @$scale", ink, bmp.at(c, 0.5, 1.0 - band, s))
            }
            // The cut is uneven, so a flip must show: the mirrored cell is not the unflipped one. Sampled
            // across the top edge, where the snips are.
            val (asCut, mirrored) = cells
            val edge = 0.012
            assertFalse(
                "$supplyId mirrored equals unflipped @$scale: the flip did nothing, or the cut is symmetric",
                bmp.row(asCut, edge, s) == bmp.row(mirrored, edge, s),
            )
            assertFalse(
                "$supplyId has no ink along its top edge @$scale, so the flip check above is vacuous",
                bmp.row(asCut, edge, s).all { it == Color.WHITE },
            )
            bmp.captureRoboImage("$GOLDEN_DIR/frame_${label}_$scale.png", aa())
        }
    }

    @Test
    fun snippedBorder_twoInks_flippedAndUnflipped() = capture("paper.border", "snipped_border", band = 0.04)

    @Test
    fun roundWindow_twoInks_flippedAndUnflipped() = capture("paper.round", "round_window", band = 0.06)
}
