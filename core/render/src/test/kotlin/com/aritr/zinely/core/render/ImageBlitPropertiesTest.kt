package com.aritr.zinely.core.render

import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.Fit
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Combinators
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import net.jqwik.api.constraints.DoubleRange
import net.jqwik.api.constraints.IntRange
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Property invariants for [computeImageBlit] across a wide input space (jqwik), complementing the
 * hand-picked oracles in [ComputeImageBlitTest].
 */
class ImageBlitPropertiesTest {
    private val eps = 1e-9

    /** Valid crops: 0 ≤ left < right ≤ 1 and 0 ≤ top < bottom ≤ 1, with a minimum extent. */
    @Provide
    fun crops(): Arbitrary<Crop> {
        val coord = Arbitraries.doubles().between(0.0, 1.0)
        val pair = Combinators.combine(coord, coord)
        return Combinators.combine(pair.`as` { a, b -> doubleArrayOf(a, b) }, pair.`as` { a, b -> doubleArrayOf(a, b) })
            .`as` { x, y ->
                val l = minOf(x[0], x[1]); val r = maxOf(x[0], x[1])
                val t = minOf(y[0], y[1]); val b = maxOf(y[0], y[1])
                Crop(left = l, top = t, right = r, bottom = b)
            }
            .filter { it.right - it.left >= 0.05 && it.bottom - it.top >= 0.05 }
    }

    @Property
    fun `FILL samples within the crop window and the unit square`(
        @ForAll @IntRange(min = 1, max = 10_000) iw: Int,
        @ForAll @IntRange(min = 1, max = 10_000) ih: Int,
        @ForAll @DoubleRange(min = 1.0, max = 2000.0) boxW: Double,
        @ForAll @DoubleRange(min = 1.0, max = 2000.0) boxH: Double,
        @ForAll("crops") crop: Crop,
    ) {
        val blit = computeImageBlit(iw, ih, crop, Fit.FILL, boxW, boxH)
        val s = blit.srcFraction
        // dest covers the full box
        assertTrue(blit.destRect.width == boxW && blit.destRect.height == boxH)
        // sample stays inside the crop window (and therefore inside [0,1])
        assertTrue(s.x >= crop.left - eps, "src left ${s.x} < crop ${crop.left}")
        assertTrue(s.y >= crop.top - eps, "src top ${s.y} < crop ${crop.top}")
        assertTrue(s.x + s.width <= crop.right + eps, "src right ${s.x + s.width} > crop ${crop.right}")
        assertTrue(s.y + s.height <= crop.bottom + eps, "src bottom ${s.y + s.height} > crop ${crop.bottom}")
    }

    @Property
    fun `FIT keeps dest inside the box and samples exactly the crop`(
        @ForAll @IntRange(min = 1, max = 10_000) iw: Int,
        @ForAll @IntRange(min = 1, max = 10_000) ih: Int,
        @ForAll @DoubleRange(min = 1.0, max = 2000.0) boxW: Double,
        @ForAll @DoubleRange(min = 1.0, max = 2000.0) boxH: Double,
        @ForAll("crops") crop: Crop,
    ) {
        val blit = computeImageBlit(iw, ih, crop, Fit.FIT, boxW, boxH)
        val d = blit.destRect
        // dest fits inside the box
        assertTrue(d.x >= -eps && d.y >= -eps, "dest origin negative: $d")
        assertTrue(d.x + d.width <= boxW + eps, "dest right ${d.x + d.width} > $boxW")
        assertTrue(d.y + d.height <= boxH + eps, "dest bottom ${d.y + d.height} > $boxH")
        // FIT samples exactly the crop rect
        assertTrue(absEq(blit.srcFraction.x, crop.left) && absEq(blit.srcFraction.y, crop.top))
        assertTrue(absEq(blit.srcFraction.width, crop.right - crop.left))
        assertTrue(absEq(blit.srcFraction.height, crop.bottom - crop.top))
    }

    private fun absEq(a: Double, b: Double) = kotlin.math.abs(a - b) <= 1e-9
}
