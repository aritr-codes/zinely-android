package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.Fit
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [FramingMath.sameFraming] — the single predicate behind *"did the maker actually change anything?"*,
 * asked by [EditorReducer]'s `CommitReframe` (record a command and autosave?) and by
 * `EditorScreen.commitReframe` (say *"Framing saved."* or *"Framing unchanged."*?).
 *
 * ### What is tested where
 *
 * This file tests **the predicate**: its tolerance, from both sides. The *round trip* that motivated it
 * lives in `feature:editor` (`Framing.seedDraft` → `toImage`) and is walked against the real functions by
 * `ReframeRoundTripTest` there. An earlier version of this file re-implemented that round trip locally so
 * the table could live here; that made it a test of a copy, which a later edit to `Framing` could have
 * left green while re-opening the defect. The table moved rather than being duplicated.
 *
 * ### What is being defended
 *
 * [D-097](../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-097): both callers compared [Crop]'s
 * `Double`s with `==`, and the crop arriving at a commit has been round-tripped through a zoom, so it
 * lands 1–7 ULP away. **56 % of untouched Reframe sessions announced a save that did not happen** and
 * pushed an undo step for it.
 *
 * ⚠ **The tolerance is the whole design, so it is tested from both ends.** Too tight re-opens D-097; too
 * loose silently swallows a real edit — a worse bug, and a quieter one.
 */
class FramingSameFramingTest {

    @Test
    fun `given identical framings, when compared, then they are the same`() {
        val c = Crop(0.25, 0.10, 0.75, 0.90)
        assertTrue(FramingMath.sameFraming(c, Fit.FIT, c, Fit.FIT))
    }

    @Test
    fun `given a difference at the floor - ULP drift - when compared, then it is the same framing`() {
        // The measured worst case of the real round trip is ~1.55e-15 (~7 ULP). This is an order above
        // that and still absorbed, which is the margin the constant claims.
        val a = Crop(0.25, 0.10, 0.75, 0.90)
        val b = Crop(0.25 + 1e-14, 0.10 - 1e-14, 0.75 + 1e-14, 0.90 - 1e-14)
        assertTrue(a != b, "the fixture must actually differ, or this proves nothing")
        assertTrue(FramingMath.sameFraming(a, Fit.FIT, b, Fit.FIT))
    }

    @Test
    fun `given a difference at the ceiling - the finest real drag - when compared, then it is NOT the same`() {
        // `ReframeOverlay` converts a drag as `fx = -pan.x * (cw0 / zoom) / frameWpx`. The worst realistic
        // case — a 10:1 panorama at MAX_ZOOM on a large dense frame — is ~1.6e-5. This asserts the
        // predicate cannot swallow it. ⚠ It is NOT "one pixel over the overlay width": an earlier
        // derivation used that and was wrong by two orders.
        val a = Crop(0.25, 0.10, 0.75, 0.90)
        val finestDrag = 1.6e-5
        assertFalse(FramingMath.sameFraming(a, Fit.FIT, a.copy(left = a.left + finestDrag), Fit.FIT))
        // And the constant's own edge, so it is pinned rather than merely "large enough".
        assertFalse(FramingMath.sameFraming(a, Fit.FIT, a.copy(left = a.left + 1e-8), Fit.FIT))
        assertTrue(FramingMath.sameFraming(a, Fit.FIT, a.copy(left = a.left + 1e-10), Fit.FIT))
    }

    @Test
    fun `given a difference on any single edge, when compared, then it is NOT the same framing`() {
        // Each edge is checked, so a predicate that forgot one cannot pass.
        val a = Crop(0.25, 0.10, 0.75, 0.90)
        val d = 1e-3
        assertFalse(FramingMath.sameFraming(a, Fit.FIT, a.copy(left = a.left + d), Fit.FIT))
        assertFalse(FramingMath.sameFraming(a, Fit.FIT, a.copy(top = a.top + d), Fit.FIT))
        assertFalse(FramingMath.sameFraming(a, Fit.FIT, a.copy(right = a.right + d), Fit.FIT))
        assertFalse(FramingMath.sameFraming(a, Fit.FIT, a.copy(bottom = a.bottom + d), Fit.FIT))
    }

    @Test
    fun `given an equal crop but a different fit, when compared, then it is NOT the same framing`() {
        // `Fit` is an enum carrying no rounding, and FILL→FIT at an equal crop changes what is drawn.
        // If this ever loosened, "Whole photo" on an already-full crop would go silent.
        val c = Crop(0.0, 0.0, 1.0, 1.0)
        assertFalse(FramingMath.sameFraming(c, Fit.FILL, c, Fit.FIT))
        assertTrue(FramingMath.sameFraming(c, Fit.FIT, c, Fit.FIT))
    }
}
