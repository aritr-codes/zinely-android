package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.Crop
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [FramingMath.clampCrop] is the model-space safety clamp (ADR-053): whatever crop a reframe produces, the
 * clamped result must always satisfy the `computeImageBlit` ([ADR-027]) invariant so a commit can never
 * persist a crop that crashes the renderer. Given-When-Then, pure.
 */
class FramingMathTest {

    @Test
    fun `a valid crop is returned equal to itself`() {
        val valid = Crop(0.1, 0.2, 0.8, 0.9)
        assertEquals(valid, FramingMath.clampCrop(valid))
    }

    @Test
    fun `Crop FULL is unchanged`() {
        assertEquals(Crop.FULL, FramingMath.clampCrop(Crop.FULL))
    }

    @Test
    fun `out-of-range edges are coerced into 0_1`() {
        val out = Crop(left = -0.3, top = -1.0, right = 1.4, bottom = 2.0)
        assertEquals(Crop(0.0, 0.0, 1.0, 1.0), FramingMath.clampCrop(out))
    }

    @Test
    fun `an inverted or degenerate axis falls back to the full extent`() {
        // left >= right on the horizontal axis, valid vertical axis is kept.
        assertEquals(Crop(0.0, 0.2, 1.0, 0.9), FramingMath.clampCrop(Crop(0.8, 0.2, 0.3, 0.9)))
        // zero-width axis (left == right) also degenerate ⇒ full.
        assertEquals(Crop(0.0, 0.2, 1.0, 0.9), FramingMath.clampCrop(Crop(0.5, 0.2, 0.5, 0.9)))
    }

    @Test
    fun `a NaN edge collapses only its own axis to full`() {
        val r = FramingMath.clampCrop(Crop(left = Double.NaN, top = 0.2, right = 0.6, bottom = 0.9))
        assertEquals(Crop(0.0, 0.2, 1.0, 0.9), r)
    }

    @Test
    fun `clampCrop is idempotent`() {
        for (c in tricky) {
            val once = FramingMath.clampCrop(c)
            assertEquals(once, FramingMath.clampCrop(once), "not idempotent for $c")
        }
    }

    @Test
    fun `property - every clamped crop satisfies the computeImageBlit precondition`() {
        // The renderer (ADR-027 computeImageBlit) requires 0 <= left < right <= 1 and 0 <= top < bottom <= 1.
        // :core:editor depends only on :core:model, so we assert that invariant directly here.
        for (c in tricky) {
            val k = FramingMath.clampCrop(c)
            assertTrue(k.left in 0.0..1.0 && k.right in 0.0..1.0 && k.left < k.right, "horiz invalid: $c ⇒ $k")
            assertTrue(k.top in 0.0..1.0 && k.bottom in 0.0..1.0 && k.top < k.bottom, "vert invalid: $c ⇒ $k")
        }
    }

    private companion object {
        // A deterministic grid of edge values: negatives, >1, inverted, degenerate, NaN, and normal.
        private val values = listOf(-1.0, -0.01, 0.0, 0.3, 0.5, 0.7, 1.0, 1.01, 2.0, Double.NaN)
        val tricky: List<Crop> = buildList {
            for (l in values) for (r in values) for (t in listOf(0.0, 0.4, 0.9)) for (b in listOf(0.1, 0.6, 1.2))
                add(Crop(l, t, r, b))
        }
    }
}
