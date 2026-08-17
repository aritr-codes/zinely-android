package com.aritr.zinely.core.render

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * X3b, the photocopier filter (ADR-106). Every expectation here is **hand-computed** from the
 * published Floyd–Steinberg weights, not captured from the implementation — a golden recorded from
 * the code under test asserts only that the code has not changed, which is the failure mode
 * [docs/DECISIONS.md ADR-083] already paid for once.
 */
class PhotocopierTest {

    // — the diffusion itself —

    @Test
    fun `error carries right into the next sample`() {
        // Given two mid-grey samples in a row.
        // 100 < 128 ⇒ ink(0), error 100; right neighbour gains 100*7/16 = 43 ⇒ 143.
        // 143 >= 128 ⇒ paper(255).
        val out = floydSteinbergDither(intArrayOf(100, 100), width = 2, height = 1)
        assertArrayEquals(intArrayOf(0, 255), out)
    }

    @Test
    fun `the rightward weight is seven sixteenths and nothing else`() {
        // Two cases that bracket the numerator from both sides — one alone cannot. Every case here
        // starts with 100, which quantises to ink and diffuses an error of exactly 100 rightward, so
        // the neighbour receives `100*w/16` and its threshold crossing reads `w` directly.
        //
        //   w :  5   6   7   8   9
        //   +:  31  37  43  50  56
        //
        // Lower bracket — the neighbour reaches paper only from 7 up: 90+43 = 133 ≥ 128.
        assertArrayEquals(intArrayOf(0, 255), floydSteinbergDither(intArrayOf(100, 90), width = 2, height = 1))
        // Upper bracket — it must still be ink at 7 and would be paper from 8 up: 84+43 = 127 < 128.
        assertArrayEquals(intArrayOf(0, 0), floydSteinbergDither(intArrayOf(100, 84), width = 2, height = 1))
        // Both together fail for every numerator except 7. The first version of this test asserted only
        // one case and claimed to pin the number; review showed 6 through 16 all passed it. Recorded
        // because the miss is the interesting part: a hand-computed expectation is only as sharp as the
        // case you happened to pick, and "I worked it out by hand" is not the same as "it discriminates".
    }

    @Test
    fun `a flat mid-grey square diffuses into a checkerboard`() {
        // 2x2 of exactly 128, worked through by hand:
        //   (0,0) 128 -> paper, err -128: right += -56 = 72; (0,1) += -40 = 88; (1,1) += -8 = 120
        //   (1,0)  72 -> ink,   err  +72:                    (0,1) += 13 = 101; (1,1) += 22 = 142
        //   (0,1) 101 -> ink,   err +101: right += 44 = 186
        //   (1,1) 186 -> paper
        val out = floydSteinbergDither(IntArray(4) { 128 }, width = 2, height = 2)
        assertArrayEquals(intArrayOf(255, 0, 0, 255), out)
    }

    @Test
    fun `pure black and pure white survive untouched`() {
        // The filter must not invent grain in flat art: zero error means nothing to diffuse.
        assertArrayEquals(IntArray(9) { 0 }, floydSteinbergDither(IntArray(9) { 0 }, 3, 3))
        assertArrayEquals(IntArray(9) { 255 }, floydSteinbergDither(IntArray(9) { 255 }, 3, 3))
    }

    @Test
    fun `the threshold is exclusive below and inclusive at 128`() {
        assertArrayEquals(intArrayOf(0), floydSteinbergDither(intArrayOf(127), 1, 1))
        assertArrayEquals(intArrayOf(255), floydSteinbergDither(intArrayOf(128), 1, 1))
    }

    @Test
    fun `the input array is not modified`() {
        val input = intArrayOf(100, 100)
        floydSteinbergDither(input, 2, 1)
        assertArrayEquals(intArrayOf(100, 100), input)
    }

    @Test
    fun `a mismatched grid is rejected rather than read out of bounds`() {
        assertThrows(IllegalArgumentException::class.java) { floydSteinbergDither(intArrayOf(1, 2, 3), 2, 2) }
        assertThrows(IllegalArgumentException::class.java) { floydSteinbergDither(IntArray(0), 0, 0) }
    }

    /**
     * The property that makes it a *photocopier* and not a threshold: total ink is conserved. A
     * uniform 25% grey over a large grid must come out roughly 25% ink — error diffusion's whole
     * claim. A plain threshold would give 100% ink and pass every exact test above.
     */
    @Test
    fun `mean brightness is preserved across the grid`() {
        val out = floydSteinbergDither(IntArray(64 * 64) { 64 }, 64, 64)
        val mean = out.sum().toDouble() / out.size
        assertTrue(mean in 58.0..70.0, "mean was $mean, expected ~64")
    }

    // — luma —

    @Test
    fun `luma is Rec 601 over the packed channels and ignores alpha`() {
        assertEquals(0, lumaOf(0xFF000000.toInt()))
        assertEquals(255, lumaOf(0xFFFFFFFF.toInt()))
        assertEquals(255, lumaOf(0x00FFFFFF)) // alpha 0, still white
        // pure red: 299*255/1000 = 76
        assertEquals(76, lumaOf(0xFFFF0000.toInt()))
        // pure green: 587*255/1000 = 149
        assertEquals(149, lumaOf(0xFF00FF00.toInt()))
        // pure blue: 114*255/1000 = 29
        assertEquals(29, lumaOf(0xFF0000FF.toInt()))
    }

    @Test
    fun `photocopy emits only opaque black and opaque white`() {
        val out = photocopy(IntArray(2) { 0xFF808080.toInt() }, 2, 1)
        assertTrue(out.all { it == 0xFF000000.toInt() || it == 0xFFFFFFFF.toInt() }, out.joinToString())
    }

    // — the grid law: the reason preview == export —

    @Test
    fun `the dot grid is derived from points and not from the decode density`() {
        // 144pt (2 inches) at 150dpi ⇒ 300 dots, whatever the source resolution is — as long as the
        // source has them. The screen decode and the 300dpi export decode therefore agree.
        assertEquals(300, copierGridSize(extentPt = 144.0, sourcePx = 4000))
        assertEquals(300, copierGridSize(extentPt = 144.0, sourcePx = 300))
    }

    @Test
    fun `the grid never upsamples past the pixels actually available`() {
        assertEquals(120, copierGridSize(extentPt = 144.0, sourcePx = 120))
    }

    @Test
    fun `a degenerate destination still yields one dot`() {
        assertEquals(1, copierGridSize(extentPt = 0.0, sourcePx = 500))
        assertEquals(1, copierGridSize(extentPt = -3.0, sourcePx = 500))
        assertEquals(1, copierGridSize(extentPt = Double.NaN, sourcePx = 500))
        assertEquals(1, copierGridSize(extentPt = 0.1, sourcePx = 500)) // rounds to 0, clamped up
    }
}
