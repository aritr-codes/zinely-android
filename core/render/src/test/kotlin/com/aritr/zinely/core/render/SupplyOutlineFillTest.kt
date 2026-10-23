package com.aritr.zinely.core.render

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * **What each authored outline actually looks like** — asserted by filling it, not by measuring it.
 *
 * ### Why this file exists
 *
 * [SupplyCatalogTest] checks *structure*: every outline spans its square, every subpath encloses an area,
 * no two subpaths partially overlap. All of that can hold while the mark on the page is wrong. Three
 * defects found in one afternoon of authoring make the case, and none of them was visible to a structural
 * assertion:
 *
 * 1. **`paper.tag`'s tail was a spike beside an empty notch** — it read as a torn corner rather than as
 *    speech. One polygon, correct span, correct area, nothing to overlap. Caught only by rendering it.
 * 2. **`mark.asterisk` would render hollow** if anyone "simplified" its ten-vertex outline back to the
 *    five-vertex `{5/2}` star polygon it traces. A `{5/2}` self-intersects, and under even-odd the core
 *    pentagon empties. Same span, same subpath count, same everything this suite could see.
 * 3. **`mark.registration` drawn the obvious way** — a full-width plus laid across a ring — punches a hole
 *    exactly where the mark is densest, because even-odd cancels the crossing.
 *
 * So this file asks the only question that catches those: **is this point solid, or is it paper?**
 *
 * ### Why probes rather than a golden
 *
 * A Roborazzi golden would be the other honest answer, and it costs more than it is worth here: goldens
 * must be recorded on the pinned CI image to mean anything, they gate on a 2 % pixel threshold that a
 * single wrong tail would slip under, and a re-record blesses a regression as smoothly as a fix. A named
 * probe cannot be blessed by accident — it says *"the middle of the star is solid"*, and if that stops
 * being true someone has to read the sentence and disagree with it in writing.
 *
 * ⚠ **This does not replace a reviewer.** §4.1 rule 4 is an authoring rule, and a mark can be solid in
 * every place probed here and still be ugly. What these assertions pin is that the *topology* survives —
 * holes where holes belong, ink where ink belongs — which is the part that fails silently.
 */
class SupplyOutlineFillTest {

    // The flattening and the even-odd ray cast this file probes with (`polygons`, `isInked`) are the
    // production ones in `SupplyInk.kt` — promoted from here by ADR-124 so that the hit test and these
    // probes cannot hold two opinions of the same outline.

    private fun outline(id: String): SupplyOutline =
        requireNotNull(SupplyCatalog.outlineOf(id)) { "$id has no authored outline — this test is about the authored ones" }

    /** Asserts [id] is inked at every point in [inked] and bare at every point in [bare]. */
    private fun probe(
        id: String,
        inked: List<Pair<Double, Double>>,
        bare: List<Pair<Double, Double>>,
    ) {
        val o = outline(id)
        for ((x, y) in inked) {
            assertTrue(o.isInked(x, y), "$id must be INKED at ($x, $y)")
        }
        for ((x, y) in bare) {
            assertFalse(o.isInked(x, y), "$id must be BARE at ($x, $y)")
        }
    }

    // — the rule the whole file rests on —

    @Test
    fun `given a known ring, when it is filled even-odd, then the probe sees the hole`() {
        // Calibration. If this fails, every assertion below is meaningless rather than wrong, so it is
        // asserted first and against a shape whose answer nobody has to trust the catalogue for.
        val ring = outline("paper.window")
        assertTrue(ring.isInked(0.05, 0.5), "the probe must find ink in a wall")
        assertFalse(ring.isInked(0.5, 0.5), "the probe must find paper in a hole — if it cannot, it cannot see any hole")
    }

    // — the three defects that motivated this file —

    @Test
    fun `given the star, when its middle is probed, then it is solid and not a hollow pentagram`() {
        // The `{5/2}` trap: five vertices joined every second one traces the same silhouette and empties
        // its own core under even-odd. Mutation: replace STAR's ten points with the five outer ones and
        // this line goes red while every assertion in SupplyCatalogTest stays green.
        probe(
            "mark.asterisk",
            inked = listOf(0.5 to 0.5, 0.5 to 0.10, 0.5 to 0.30),
            bare = listOf(0.03 to 0.03, 0.97 to 0.97, 0.5 to 0.99),
        )
    }

    @Test
    fun `given the speech tag, when the tail and the notch are probed, then only the tail is inked`() {
        // The defect this caught: a tail so narrow, and so nearly vertical, that the mark read as a torn
        // corner. The apex sits LEFT of its own base, which is what makes it a tail rather than a spike —
        // so the probe is placed where a leaning tail has ink and an upright one does not.
        probe(
            "paper.tag",
            // A39's tag (ADR-139): base 5.4..11.6 at y 16, apex (3.4, 22.6), so (5.76, 19.2) is in the leaning tail.
            inked = listOf(0.5 to 0.30, 0.24 to 0.80),
            bare = listOf(0.70 to 0.85, 0.05 to 0.90, 0.95 to 0.95),
        )
    }

    @Test
    fun `given the registration mark, when its arms and ring are probed, then the crossing did not cancel`() {
        // Four arms, an annulus, and a deliberately empty centre. The failure this guards is the one the
        // obvious drawing produces: arms laid ACROSS the ring cancel under even-odd, so the ring's band
        // goes bare exactly where an arm passes through it. Mutation: extend an arm to 0.5 and the
        // `0.5 to 0.28` probe (the ring band, directly under the top arm) goes bare.
        probe(
            "mark.registration",
            inked = listOf(0.05 to 0.5, 0.95 to 0.5, 0.5 to 0.05, 0.5 to 0.28, 0.28 to 0.5),
            bare = listOf(0.5 to 0.5, 0.15 to 0.15, 0.85 to 0.85),
        )
    }

    // — the rest of the authored twelve, so nothing added here is unobserved —

    @Test
    fun `given the halftone lattice, when a dot and a gap are probed, then the dots are separate`() {
        // The largest dot is at (0.11, 0.11); the gap on the diagonal between the first two dots is the
        // tightest clearance in the mark, so it is the point that would first go solid if the pitch were
        // ever narrowed into an overlap — which even-odd would then render as a notch, not a merge.
        probe(
            "mark.halftone",
            inked = listOf(0.11 to 0.11, 0.382 to 0.11, 0.926 to 0.926),
            bare = listOf(0.246 to 0.11, 0.5 to 0.99, 0.99 to 0.5),
        )
    }

    @Test
    fun `given the arrow, when its shaft and head are probed, then it points right`() {
        // Right, not up: the maker has rotate, and §5.1 lands every supply at 0°. If this ever reads as
        // an up arrow the default landing is wrong for every maker who does not think to rotate it.
        // A39's arrow (ADR-139) sweeps from the left edge up to a head at the top right.
        probe(
            "mark.arrow",
            inked = listOf(0.0625 to 0.686, 0.667 to 0.329, 0.908 to 0.5),
            bare = listOf(0.10 to 0.15, 0.10 to 0.85, 0.5 to 0.3, 0.95 to 0.95),
        )
    }

    @Test
    fun `given the photo corner, when the pocket is probed, then it is a pocket and not a triangle`() {
        // Without its hole this supply is `shape.triangle`, which already ships. The hole IS the supply.
        probe(
            "fix.corner",
            // The A39 corner (ADR-139): a pocket in the top-left, open along the hypotenuse.
            inked = listOf(0.06 to 0.3, 0.5 to 0.06),
            bare = listOf(0.25 to 0.25, 0.9 to 0.9),
        )
    }

    @Test
    fun `given the staple, when between its legs is probed, then the crown is not a solid block`() {
        probe(
            "fix.staple",
            // The A39 staple (ADR-139): crown at 7.2..9.7 of 24, legs down to 18.95.
            inked = listOf(0.5 to 0.35, 0.09 to 0.6, 0.92 to 0.6),
            bare = listOf(0.5 to 0.6, 0.5 to 0.1),
        )
    }

    // — non-vacuity —

    @Test
    fun `given every authored outline, when its square is sampled, then it is neither blank nor solid`() {
        // A blanket floor under all twelve, including the four this file does not probe by name. An
        // outline that fills its whole square is indistinguishable from its own bounding box; one that
        // fills almost nothing renders as a speck the maker cannot see. `shape.rect` is the one supply
        // that IS its box, so it is the stated exception rather than a loosened bound.
        for ((id, o) in SupplyCatalog.OUTLINES) {
            var inked = 0
            val n = 60
            for (iy in 0 until n) {
                for (ix in 0 until n) {
                    if (o.isInked((ix + 0.5) / n, (iy + 0.5) / n)) inked++
                }
            }
            val coverage = inked.toDouble() / (n * n)
            // ponytail: one stated exception, not a lowered floor: A39's fine perforation is a frozen row of
            // pinpricks (true area 0.038 of its square; this 60-grid samples 0.041) and reads as a line.
            val floor = if (id == "mark.perfc") 0.035 else 0.05
            assertTrue(coverage > floor, "$id inks only ${"%.3f".format(coverage)} of its square — it would read as a speck")
            if (id == "shape.rect") {
                assertEquals(1.0, coverage, 1e-9, "shape.rect is its own box, by definition")
            } else {
                assertTrue(coverage < 0.98, "$id inks ${"%.3f".format(coverage)} — it is indistinguishable from its box")
            }
        }
    }
}
