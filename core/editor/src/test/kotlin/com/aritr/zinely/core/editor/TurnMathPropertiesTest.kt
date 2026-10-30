package com.aritr.zinely.core.editor

import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.DoubleRange
import net.jqwik.api.constraints.IntRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.math.abs

/** ADR-143 Consequences: the rotation helper's properties (idempotent, |snap(r) - r| <= 5, exact, 360 = 0). */
class TurnMathPropertiesTest {

    @Property
    fun `normalise lands in the half-open turn and keeps the angle mod 360`(
        @ForAll @DoubleRange(min = -5000.0, max = 5000.0) r: Double,
    ) {
        val n = TurnMath.normalise(r)
        assertTrue(n > -180.0 && n <= 180.0, "$r -> $n")
        val diff = abs((r - n) / 360.0 - Math.rint((r - n) / 360.0))
        assertTrue(diff < 1e-9, "$r -> $n is not a whole number of turns away")
    }

    @Property
    fun `normalise is idempotent`(@ForAll @DoubleRange(min = -5000.0, max = 5000.0) r: Double) {
        assertEquals(TurnMath.normalise(r), TurnMath.normalise(TurnMath.normalise(r)))
    }

    @Property
    fun `snap moves at most 5, is idempotent, and a snapped value is an exact quarter`(
        @ForAll @DoubleRange(min = -5000.0, max = 5000.0) r: Double,
    ) {
        val s = TurnMath.snap(r)
        assertTrue(abs(s - r) <= TurnMath.SNAP_DEGREES)
        assertEquals(s, TurnMath.snap(s))
        if (s != r) assertTrue(TurnMath.isOnQuarter(s), "$r snapped to $s")
    }

    @Property
    fun `whole turns are upright, and straightening always lands upright`(
        @ForAll @IntRange(min = -20, max = 20) turns: Int,
        @ForAll @DoubleRange(min = -5000.0, max = 5000.0) r: Double,
    ) {
        assertTrue(TurnMath.isUpright(turns * 360.0))
        assertTrue(TurnMath.isUpright(r + TurnMath.uprightDelta(r)))
        assertTrue(abs(TurnMath.uprightDelta(r)) <= 180.0)
    }

    @Property
    fun `an armed knob never rests within 5 of a quarter, other than on it`(
        @ForAll @DoubleRange(min = -720.0, max = 720.0) start: Double,
        @ForAll @DoubleRange(min = -720.0, max = 720.0) raw: Double,
    ) {
        val knob = KnobSnap.startingAt(start).next(raw)
        val a = knob.angleFor(raw)
        val q = TurnMath.nearestQuarter(a)
        val inZone = abs(a - q) <= TurnMath.SNAP_DEGREES && a != q
        // Only an unarmed knob (a tilt the drag started from and never left) may rest there.
        if (inZone) assertTrue(!knob.armed && abs(raw - TurnMath.nearestQuarter(start)) <= TurnMath.SNAP_DEGREES)
    }
}
