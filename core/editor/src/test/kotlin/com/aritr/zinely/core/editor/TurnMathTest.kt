package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.Transform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** A48.3–A48.6 (ADR-143 decisions 1, 2, 4): the pure turn helper, case by case from the frozen HTML. */
class TurnMathTest {

    @Test
    fun `normalise lands in the half-open turn from -180 to 180`() {
        assertEquals(180.0, TurnMath.normalise(180.0))
        assertEquals(180.0, TurnMath.normalise(-180.0))
        assertEquals(15.0, TurnMath.normalise(375.0))
        assertEquals(0.0, TurnMath.normalise(360.0))
        assertEquals(0.0, TurnMath.normalise(-360.0))
        assertEquals(-30.0, TurnMath.normalise(-30.0))
        assertEquals(-90.0, TurnMath.normalise(270.0))
        // never a negative zero: the readout writes "0°", not "-0°"
        assertFalse(1.0 / TurnMath.normalise(-360.0) < 0)
        assertFalse(1.0 / TurnMath.normalise(-720.0) < 0)
    }

    @Test
    fun `snap writes the quarter in r's own turns, 5 inclusive (A48 4)`() {
        assertEquals(360.0, TurnMath.snap(358.0))
        assertEquals(-90.0, TurnMath.snap(-92.0))
        assertEquals(0.0, TurnMath.snap(5.0))
        assertEquals(5.01, TurnMath.snap(5.01))
        assertEquals(90.0, TurnMath.snap(87.0))
        assertEquals(30.0, TurnMath.snap(30.0))
        assertFalse(1.0 / TurnMath.snap(-3.0) < 0, "snap to 0 is +0")
    }

    @Test
    fun `upright is n = 0 within 1e-6, so 360 is upright (A48 6)`() {
        assertTrue(TurnMath.isUpright(0.0))
        assertTrue(TurnMath.isUpright(360.0))
        assertTrue(TurnMath.isUpright(-720.0))
        assertTrue(TurnMath.isUpright(5e-7))
        assertFalse(TurnMath.isUpright(1.5))
        assertFalse(TurnMath.isUpright(180.0))
    }

    @Test
    fun `uprightDelta is the shortest way, 180 goes counterclockwise (A48 6)`() {
        assertEquals(-15.0, TurnMath.uprightDelta(375.0)) // 375 -> 360
        assertEquals(30.0, TurnMath.uprightDelta(-30.0)) // -30 -> 0
        assertEquals(-180.0, TurnMath.uprightDelta(180.0)) // 180 -> 0
        assertEquals(-1.5, TurnMath.uprightDelta(1.5))
        assertTrue(TurnMath.isUpright(375.0 + TurnMath.uprightDelta(375.0)))
    }

    @Test
    fun `knob hysteresis - from 3, a 0_5 drag stays at 3_5, never 0`() {
        var knob = KnobSnap.startingAt(3.0)
        knob = knob.next(3.5)
        assertEquals(3.5, knob.angleFor(3.5))
        knob = knob.next(2.0)
        assertEquals(2.0, knob.angleFor(2.0))
    }

    @Test
    fun `knob hysteresis - from 3, past 8 and back to 2 snaps to 0`() {
        var knob = KnobSnap.startingAt(3.0)
        knob = knob.next(8.5)
        assertEquals(8.5, knob.angleFor(8.5))
        knob = knob.next(2.0)
        assertEquals(0.0, knob.angleFor(2.0))
    }

    @Test
    fun `a drag that starts on a quarter, or outside every zone, snaps at once`() {
        val onQuarter = KnobSnap.startingAt(0.0).next(3.0)
        assertEquals(0.0, onQuarter.angleFor(3.0))
        val outside = KnobSnap.startingAt(30.0).next(88.0)
        assertEquals(90.0, outside.angleFor(88.0))
        val at360 = KnobSnap.startingAt(360.0).next(363.0)
        assertEquals(360.0, at360.angleFor(363.0))
    }

    @Test
    fun `sameTransform is the tolerant back-where-it-began test (A48 3, A48 9)`() {
        val t = Transform(10.0, 20.0, 30.0, 40.0, 0.0)
        assertTrue(TurnMath.sameTransform(t, t.copy(rotationDegrees = 360.0)))
        assertTrue(TurnMath.sameTransform(t, t.copy(xPt = 10.0 + 1e-12, widthPt = 30.0 * 1.1 / 1.1)))
        assertTrue(TurnMath.sameTransform(t.copy(rotationDegrees = 15.0), t.copy(rotationDegrees = -345.0)))
        assertFalse(TurnMath.sameTransform(t, t.copy(rotationDegrees = 1.0)))
        assertFalse(TurnMath.sameTransform(t, t.copy(yPt = 20.001)))
    }
}
