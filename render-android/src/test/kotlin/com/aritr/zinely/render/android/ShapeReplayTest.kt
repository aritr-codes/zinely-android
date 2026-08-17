package com.aritr.zinely.render.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.render.DrawCommand
import com.aritr.zinely.core.render.DrawShape
import com.aritr.zinely.core.render.FillRect
import com.aritr.zinely.core.render.Segment
import com.aritr.zinely.core.render.SceneRenderer
import com.aritr.zinely.core.render.Subpath
import com.aritr.zinely.core.render.SupplyCatalog
import com.aritr.zinely.core.render.SupplyOutline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import kotlin.math.ceil

/**
 * P3 — a supply reaching **pixels** through the one replayer (ADR-028), and the checks that can tell
 * the difference between drawing it and drawing something that merely looks plausible.
 *
 * This file replaces `ShapeReplayDrawsNothingTest`, whose assertions P2 asked to be **inverted, not
 * deleted**, when the arm was armed. The scope-balance case survives verbatim in spirit: this is the
 * replayer shared by preview, PNG, PDF and the imposed sheet, so an unbalanced save here would corrupt
 * every later command on four surfaces at once.
 *
 * ### The hole test is the load-bearing one (SUPPLIES-SPEC §3.2, §3.5)
 *
 * [holeTest_sameWoundRing_leavesItsCentreUnfilled] is the check the spike chose over diffing PDF
 * operators, and it is worth being precise about *why* one assertion is allowed to carry this much.
 * A ring whose inner and outer contours wind the **same** direction is the only geometry whose
 * rendering differs between even-odd and non-zero; a ring wound in opposite directions has a hole
 * under both rules and would pass whichever one the backend actually used. What this single case fails
 * on:
 *
 *  - a dropped `Path.fillType` — the hole fills, and it fills *identically on all four surfaces*, which
 *    is exactly the defect cross-surface parity testing is blind to by construction. **Verified by
 *    mutation**: flipping `EVEN_ODD` to `WINDING` turns this test and its discriminator twin red.
 *  - a silent raster fallback on the PDF surface — but only in the instrumented twin,
 *    `PdfSurfaceParityInstrumentedTest`, which has never executed (see below).
 *
 * ⚠ **What it does NOT catch, corrected after a review falsified the first version of this comment.**
 * An earlier draft claimed this test also fails on a missing scale fold and on a wrong composition
 * order. **It does not, and mutation proved it**: deleting the scale term and reversing the composition
 * order each left this test green. The reason is structural — this test builds `localToPage` by hand
 * (`translate(12,12) · scale(48,48)`) and never routes through [SceneRenderer], so the fold is not on
 * its path at all. Those two mutations are caught by `sceneRendererFold_putsAnAuthoredSupplyOnItsOwnBox`
 * and the three fold cases in `SceneRendererDecorTest`.
 *
 * No coverage was missing; only the attribution was wrong. It is corrected rather than quietly deleted
 * because a test whose comment overstates its reach is worse than one with no comment: the next person
 * to touch the fold would have believed this test was watching them.
 *
 * ### Which surface this actually proves
 *
 * The raster one. Robolectric `graphicsMode=NATIVE` rasterises through real `android.graphics`, so
 * these are genuine pixels and run headless. **The PDF surface is not proved here** — `PdfDocument`
 * cannot run under Robolectric at all (see `PdfSurfaceParityInstrumentedTest`'s header), so its hole
 * test is instrumented, compile-checked in CI, and has to run on a device before the print half of
 * §3.2 stops being an argument.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShapeReplayTest {

    private companion object {
        const val SCALE = 4.0 // px per point — enough that a 1pt sliver would be visible if it happened
        val INK = ColorRgba(200, 40, 90, 255)
        val INK_ARGB = Color.argb(255, 200, 40, 90)
        val sheet = PtSize(72.0, 72.0)
    }

    private val replayer = CanvasReplayer()

    /** A square from its top-left corner in unit space, wound clockwise (+y down) when [size] > 0. */
    private fun square(x: Double, y: Double, size: Double) = Subpath(
        start = PtPoint(x, y),
        segments = listOf(
            Segment.LineTo(PtPoint(x + size, y)),
            Segment.LineTo(PtPoint(x + size, y + size)),
            Segment.LineTo(PtPoint(x, y + size)),
        ),
    )

    /**
     * The probe geometry: outer and inner contours wound the **same** direction, so the centre is
     * enclosed twice — empty under even-odd, filled under non-zero. `SupplyOutlineRingTest` pins that
     * property on the geometry itself; this file pins that the pixels honour it.
     */
    private fun ring() = SupplyOutline(listOf(square(0.0, 0.0, 1.0), square(0.25, 0.25, 0.5)))

    private fun render(tape: List<DrawCommand>): Bitmap {
        val bmp = Bitmap.createBitmap(
            ceil(sheet.width * SCALE).toInt(),
            ceil(sheet.height * SCALE).toInt(),
            Bitmap.Config.ARGB_8888,
        )
        replayer.replay(
            canvas = Canvas(bmp),
            tape = tape,
            pageToDevice = ExportScale.previewPageToDevice(SCALE, PtPoint(0.0, 0.0)),
            pageClip = PtRect(0.0, 0.0, sheet.width, sheet.height),
            decodePxPerPt = SCALE,
        )
        return bmp
    }

    private fun Bitmap.at(pageX: Double, pageY: Double): Int =
        getPixel((pageX * SCALE).toInt().coerceIn(0, width - 1), (pageY * SCALE).toInt().coerceIn(0, height - 1))

    private fun paper() = FillRect(PtRect(0.0, 0.0, sheet.width, sheet.height), ColorRgba.WHITE)

    @Test
    fun holeTest_sameWoundRing_leavesItsCentreUnfilled() {
        // Placed at (12,12), 48 x 48: outer wall spans page 12..60, the hole spans 24..48.
        val shape = DrawShape(
            outline = ring(),
            ink = INK,
            localToPage = AffineTransform2D.translate(12.0, 12.0)
                .times(AffineTransform2D.scale(48.0, 48.0)),
        )
        val bmp = render(listOf(paper(), shape))

        // The whole point. Under non-zero this pixel is INK; under even-odd it is paper.
        assertEquals("the hole must stay paper — a dropped even-odd fill rule fills it", Color.WHITE, bmp.at(36.0, 36.0))
        // Just inside the outer wall, on all four sides, so a shifted or squashed ring cannot pass by
        // leaving the centre white for the wrong reason (e.g. drawing nothing at all).
        assertEquals("wall, top", INK_ARGB, bmp.at(36.0, 15.0))
        assertEquals("wall, bottom", INK_ARGB, bmp.at(36.0, 57.0))
        assertEquals("wall, left", INK_ARGB, bmp.at(15.0, 36.0))
        assertEquals("wall, right", INK_ARGB, bmp.at(57.0, 36.0))
        // And outside the ring is untouched paper — the supply stays inside its own box.
        assertEquals("outside the box", Color.WHITE, bmp.at(4.0, 4.0))
    }

    @Test
    fun holeTest_isBlindWithoutTheFillRule_soPinTheDiscriminator() {
        // Guards the guard. If `toPath()` ever stopped setting EVEN_ODD, the assertion above would fail
        // — but only if a WINDING fill of this exact geometry really does close the hole. Pin that here
        // rather than assume it, so a future reader can see the hole test is not vacuous.
        val path = ring().toPath()
        assertEquals(Path.FillType.EVEN_ODD, path.fillType)

        val bmp = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint().apply { style = Paint.Style.FILL; color = INK_ARGB }
        canvas.scale(48f, 48f)
        path.fillType = Path.FillType.WINDING
        canvas.drawPath(path, paint)

        assertNotEquals(
            "a WINDING fill of a same-wound ring must close the hole, or the hole test proves nothing",
            0,
            bmp.getPixel(24, 24),
        )
    }

    @Test
    fun sceneRendererFold_putsAnAuthoredSupplyOnItsOwnBox() {
        // End-to-end: Page -> SceneRenderer -> the one replayer. `shape.rect` is the full unit square,
        // so the ink footprint IS the element box — which makes a missing scale term (§3.4.1) a
        // 1pt x 1pt speck at the origin rather than a filled 40 x 25 box, visible in every probe below.
        val decor = DecorElement(
            id = "d1",
            transform = Transform(xPt = 16.0, yPt = 24.0, widthPt = 40.0, heightPt = 25.0),
            zIndex = 1,
            supplyId = "shape.rect",
            ink = INK,
        )
        val tape = SceneRenderer.render(
            Page(0, PageRole.INTERIOR, elements = listOf(decor)),
            sheet,
            DocumentDefaults(),
        )
        val bmp = render(listOf(paper()) + tape)

        assertEquals("centre of the box", INK_ARGB, bmp.at(36.0, 36.0))
        assertEquals("inside the top-left corner", INK_ARGB, bmp.at(18.0, 26.0))
        assertEquals("inside the bottom-right corner", INK_ARGB, bmp.at(54.0, 47.0))
        // Beyond the box on the axis the element is *shorter* in: catches a uniform scale that used the
        // width for both terms, which would spill 15pt past the bottom edge.
        assertEquals("below the box", Color.WHITE, bmp.at(36.0, 54.0))
        assertEquals("right of the box", Color.WHITE, bmp.at(60.0, 36.0))
    }

    @Test
    fun aCurvedSupplyIsAntiAliased_andTheSharedFillPaintIsNot() {
        // §3.5's trade-off, asserted rather than trusted: the supply paint has AA on (a torn or curved
        // edge is jagged without it) while `fillPaint` keeps AA off so geometric fills diff at zero
        // tolerance. Proved by a partially-covered edge pixel: an AA'd circle edge yields a blend, an
        // aliased rect edge never does.
        val circle = DrawShape(
            outline = SupplyCatalog.outlineOf("shape.circle")!!,
            ink = INK,
            localToPage = AffineTransform2D.translate(12.0, 12.0).times(AffineTransform2D.scale(48.0, 48.0)),
        )
        val bmp = render(listOf(paper(), circle))

        // Walk the diagonal out of the circle; between solid ink and solid paper there must be a blend.
        val blended = (0 until 200).map { i ->
            val t = 12.0 + 24.0 + 24.0 * (i / 200.0) / kotlin.math.sqrt(2.0)
            bmp.at(t, t)
        }.any { it != INK_ARGB && it != Color.WHITE && it != 0 }
        assertEquals("a curved supply edge must be anti-aliased (§3.5)", true, blended)
    }

    @Test
    fun theSharedFillPaintStaysAliased_soGeometricFillsKeepDiffingAtZeroTolerance() {
        // The other half of §3.5's trade-off, and it was NOT tested until a review mutated
        // `fillPaint.isAntiAlias` to `true` and watched the whole suite stay green. The clause lived only
        // in a test NAME. Roborazzi's goldens would have caught it eventually, but a golden reports "the
        // pixels moved", not "the shared paint changed" — and the two are a long way apart at 3am.
        //
        // A ROTATED rect is what makes this observable: an axis-aligned edge lands on whole pixels and
        // looks identical either way, so the obvious version of this test would have passed under the
        // mutation too.
        val rotated = FillRect(
            rect = PtRect(0.0, 0.0, 32.0, 32.0),
            color = ColorRgba(200, 40, 90, 255),
            localToPage = AffineTransform2D.translate(20.0, 20.0).times(AffineTransform2D.rotateDeg(30.0)),
        )
        val bmp = render(listOf(paper(), rotated))

        val blended = (0 until 400).map { i ->
            val t = 4.0 + 64.0 * (i / 400.0)
            bmp.at(t, t)
        }.any { it != INK_ARGB && it != Color.WHITE && it != 0 }
        assertEquals("the shared fillPaint must stay aliased (§3.5)", false, blended)
    }

    // ---- The scope contract, carried over from ShapeReplayDrawsNothingTest --------------------

    private class RecordingCanvas(bitmap: Bitmap) : Canvas(bitmap) {
        val ops = mutableListOf<String>()
        val saveCounts = mutableListOf<Int>()
        val restoreArgs = mutableListOf<Int>()

        override fun save(): Int {
            ops.add("save")
            return super.save().also { saveCounts.add(it) }
        }

        override fun concat(matrix: Matrix?) {
            ops.add("concat")
            super.concat(matrix)
        }

        override fun clipRect(left: Float, top: Float, right: Float, bottom: Float): Boolean {
            ops.add("clipRect")
            return super.clipRect(left, top, right, bottom)
        }

        override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
            ops.add("drawRect")
            super.drawRect(left, top, right, bottom, paint)
        }

        override fun drawPath(path: Path, paint: Paint) {
            ops.add("drawPath")
            super.drawPath(path, paint)
        }

        override fun restoreToCount(saveCount: Int) {
            ops.add("restore")
            restoreArgs.add(saveCount)
            super.restoreToCount(saveCount)
        }
    }

    private fun triangle() = DrawShape(
        outline = SupplyCatalog.outlineOf("shape.triangle")!!,
        ink = INK,
        localToPage = AffineTransform2D.translate(10.0, 10.0).times(AffineTransform2D.scale(40.0, 25.0)),
    )

    @Test
    fun `given a tape of only shapes, when replayed, then exactly one drawPath runs inside the quad`() {
        val canvas = RecordingCanvas(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))

        replayer.replay(canvas, listOf(triangle()), AffineTransform2D.identity(), PtRect(0.0, 0.0, 100.0, 100.0), 1.0)

        // No `clipRect` for the command: DrawShape.localClip is fixed at null (§3.2 constraint 1), so
        // the quad's clip step is skipped — a points-valued clip would crop a unit outline to a sliver.
        assertEquals(
            listOf("save", "concat", "clipRect", "save", "concat", "drawPath", "restore", "restore"),
            canvas.ops,
        )
    }

    @Test
    fun `given a shape between two fills, when replayed, then both fills still draw and scopes balance`() {
        val canvas = RecordingCanvas(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))
        val fill = FillRect(PtRect(0.0, 0.0, 4.0, 4.0), ColorRgba(255, 0, 0))

        replayer.replay(
            canvas, listOf(fill, triangle(), fill),
            AffineTransform2D.identity(), PtRect(0.0, 0.0, 100.0, 100.0), 1.0,
        )

        assertEquals(2, canvas.ops.count { it == "drawRect" })
        assertEquals(1, canvas.ops.count { it == "drawPath" })
        assertEquals(4, canvas.saveCounts.size) // one page scope + three command scopes
        assertEquals(canvas.saveCounts.reversed(), canvas.restoreArgs) // strict LIFO, nothing leaked
    }
}
