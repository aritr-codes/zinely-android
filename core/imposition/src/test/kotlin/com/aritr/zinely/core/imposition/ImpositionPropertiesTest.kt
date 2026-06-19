package com.aritr.zinely.core.imposition

import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.ZineFormat
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import net.jqwik.api.constraints.DoubleRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.math.abs

/**
 * Property-based invariants (jqwik). These assert the engine and the affine algebra hold across a
 * wide input space, not just the hand-picked examples in the unit tests.
 */
class ImpositionPropertiesTest {
    private val imposer = SingleSheet8Imposer()
    private val validator = LayoutValidator()
    private val eps = 1e-6

    @Provide
    fun papers(): Arbitrary<PaperSize> = Arbitraries.of(PaperSize.LETTER, PaperSize.A4)

    // --- Engine invariants -----------------------------------------------------------------------

    @Property
    fun `engine output always validates clean`(
        @ForAll("papers") paper: PaperSize,
        @ForAll @DoubleRange(min = 0.0, max = 95.0) inset: Double,
    ) {
        val layout = imposer.layout(ZineFormat.SINGLE_SHEET_8, paper, inset)
        assertEquals(emptyList<ValidationIssue>(), validator.validate(layout))
    }

    @Property
    fun `panels always tile the sheet area`(@ForAll("papers") paper: PaperSize) {
        val layout = imposer.layout(ZineFormat.SINGLE_SHEET_8, paper)
        val sheetArea = layout.sheet.width * layout.sheet.height
        assertEquals(sheetArea, layout.panels.sumOf { it.bounds.area }, sheetArea * 1e-9)
    }

    @Property
    fun `contentToSheet maps panel-local corners onto bounds`(@ForAll("papers") paper: PaperSize) {
        val layout = imposer.layout(ZineFormat.SINGLE_SHEET_8, paper)
        for (p in layout.panels) {
            val pl = p.panelLocalBounds
            val xs = listOf(pl.x, pl.right).flatMap { x -> listOf(pl.y, pl.bottom).map { y -> p.contentToSheet.map(PtPoint(x, y)) } }
            assertTrue(abs(xs.minOf { it.x } - p.bounds.x) <= eps)
            assertTrue(abs(xs.maxOf { it.x } - p.bounds.right) <= eps)
            assertTrue(abs(xs.minOf { it.y } - p.bounds.y) <= eps)
            assertTrue(abs(xs.maxOf { it.y } - p.bounds.bottom) <= eps)
        }
    }

    // --- Affine algebra --------------------------------------------------------------------------

    @Property
    fun `half turn about any center is self-inverse`(
        @ForAll @DoubleRange(min = -1000.0, max = 1000.0) cx: Double,
        @ForAll @DoubleRange(min = -1000.0, max = 1000.0) cy: Double,
        @ForAll @DoubleRange(min = -1000.0, max = 1000.0) px: Double,
        @ForAll @DoubleRange(min = -1000.0, max = 1000.0) py: Double,
    ) {
        val t = AffineTransform2D.halfTurnAbout(cx, cy)
        val p = PtPoint(px, py)
        val back = t.map(t.map(p))
        assertTrue(abs(back.x - p.x) <= 1e-9 && abs(back.y - p.y) <= 1e-9)
    }

    @Property
    fun `translations compose additively`(
        @ForAll @DoubleRange(min = -500.0, max = 500.0) ax: Double,
        @ForAll @DoubleRange(min = -500.0, max = 500.0) ay: Double,
        @ForAll @DoubleRange(min = -500.0, max = 500.0) bx: Double,
        @ForAll @DoubleRange(min = -500.0, max = 500.0) by: Double,
        @ForAll @DoubleRange(min = -500.0, max = 500.0) px: Double,
        @ForAll @DoubleRange(min = -500.0, max = 500.0) py: Double,
    ) {
        val composed = AffineTransform2D.translate(ax, ay).then(AffineTransform2D.translate(bx, by))
        val out = composed.map(PtPoint(px, py))
        assertTrue(abs(out.x - (px + ax + bx)) <= 1e-9 && abs(out.y - (py + ay + by)) <= 1e-9)
    }

    @Property
    fun `then applies this first then next (a then b equals b after a)`(
        @ForAll @DoubleRange(min = -3.0, max = 3.0) a0: Double,
        @ForAll @DoubleRange(min = -3.0, max = 3.0) a1: Double,
        @ForAll @DoubleRange(min = -3.0, max = 3.0) a2: Double,
        @ForAll @DoubleRange(min = -3.0, max = 3.0) a3: Double,
        @ForAll @DoubleRange(min = -50.0, max = 50.0) a4: Double,
        @ForAll @DoubleRange(min = -50.0, max = 50.0) a5: Double,
        @ForAll @DoubleRange(min = -3.0, max = 3.0) b0: Double,
        @ForAll @DoubleRange(min = -3.0, max = 3.0) b1: Double,
        @ForAll @DoubleRange(min = -3.0, max = 3.0) b2: Double,
        @ForAll @DoubleRange(min = -3.0, max = 3.0) b3: Double,
        @ForAll @DoubleRange(min = -50.0, max = 50.0) b4: Double,
        @ForAll @DoubleRange(min = -50.0, max = 50.0) b5: Double,
        @ForAll @DoubleRange(min = -50.0, max = 50.0) px: Double,
        @ForAll @DoubleRange(min = -50.0, max = 50.0) py: Double,
    ) {
        val a = AffineTransform2D(a0, a1, a2, a3, a4, a5)
        val b = AffineTransform2D(b0, b1, b2, b3, b4, b5)
        val p = PtPoint(px, py)
        val viaThen = a.then(b).map(p) // apply a first, then b
        val viaCompose = b.map(a.map(p))
        assertTrue(abs(viaThen.x - viaCompose.x) <= 1e-6 && abs(viaThen.y - viaCompose.y) <= 1e-6)
    }

    @Property
    fun `rotating by 90 degrees four times is the identity`(
        @ForAll @DoubleRange(min = -1000.0, max = 1000.0) px: Double,
        @ForAll @DoubleRange(min = -1000.0, max = 1000.0) py: Double,
    ) {
        val q = AffineTransform2D.rotateDeg(90.0)
        val full = q.then(q).then(q).then(q)
        val out = full.map(PtPoint(px, py))
        assertTrue(abs(out.x - px) <= 1e-6 && abs(out.y - py) <= 1e-6)
    }
}
