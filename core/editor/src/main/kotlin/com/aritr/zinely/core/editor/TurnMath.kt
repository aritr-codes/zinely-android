package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.Transform
import kotlin.math.abs
import kotlin.math.floor

/**
 * The turn rules of Bench A48 ([ADR-143](../../../../../../../../docs/DECISIONS.md#adr-143)): normalising,
 * the knob's quarter-turn snap, upright, and the tolerant "back where it began" test.
 *
 * **Invariant: stored angles are never normalised** (ADR-143 decision 8). Everything here is for display and
 * comparison; a snap writes `q` in the angle's own turns (358 → 360, never 0), and Straighten writes
 * `r + uprightDelta(r)` (375 → 360).
 */
public object TurnMath {
    /** A48.4: a knob angle within this many degrees (inclusive) of a quarter turn is written as that quarter. */
    public const val SNAP_DEGREES: Double = 5.0
    public const val QUARTER_DEGREES: Double = 90.0

    /** A48.3 / A48.6 / A48.9: the one tolerance, in pt for position and size and in degrees for the angle. */
    public const val EPSILON: Double = 1e-6

    /** [degrees] in (−180, 180], never −0 (so the readout never writes "−0°"). */
    public fun normalise(degrees: Double): Double {
        var n = degrees.mod(360.0)
        if (n > 180.0) n -= 360.0
        return n + 0.0 // −0.0 + 0.0 == +0.0
    }

    /** n = 0 within [EPSILON]: 360 and 720 are upright. */
    public fun isUpright(degrees: Double): Boolean = abs(normalise(degrees)) <= EPSILON

    /** Straighten's turn (A48.6): the shortest way to upright; 180 goes counterclockwise (the half-open range). */
    public fun uprightDelta(degrees: Double): Double = -normalise(degrees)

    /** The multiple of 90 nearest [degrees], in its own turns (358 → 360, −92 → −90). Halves round up, as JS does. */
    public fun nearestQuarter(degrees: Double): Double =
        QUARTER_DEGREES * floor(degrees / QUARTER_DEGREES + 0.5) + 0.0

    /** A48.4: within [SNAP_DEGREES] (inclusive) of a quarter, that quarter exactly; otherwise [degrees]. */
    public fun snap(degrees: Double): Double {
        val q = nearestQuarter(degrees)
        return if (abs(degrees - q) <= SNAP_DEGREES) q else degrees
    }

    /** The readout's leaf state (A48.5): exactly on a quarter turn. */
    public fun isOnQuarter(degrees: Double): Boolean = degrees % QUARTER_DEGREES == 0.0

    /**
     * "Back where it began" (A48.3, A48.9): every position and size field within [EPSILON] pt, the angle
     * within [EPSILON]° after normalising mod 360. A caller that finds this true restores `before` exactly.
     */
    public fun sameTransform(a: Transform, b: Transform): Boolean =
        abs(a.xPt - b.xPt) <= EPSILON && abs(a.yPt - b.yPt) <= EPSILON &&
            abs(a.widthPt - b.widthPt) <= EPSILON && abs(a.heightPt - b.heightPt) <= EPSILON &&
            abs(normalise(a.rotationDegrees - b.rotationDegrees)) <= EPSILON
}

/**
 * The knob's snap for one drag, with A48.4's hysteresis: a drag that starts within [TurnMath.SNAP_DEGREES]
 * of a quarter turn but not on it is not [armed] until the angle has once left that quarter's zone, so
 * touching the knob of a saved 3° piece never straightens it. Feed every raw angle through [next] before
 * [angleFor].
 */
public data class KnobSnap(val startQuarter: Double, val armed: Boolean) {
    public fun next(raw: Double): KnobSnap =
        if (armed || abs(raw - startQuarter) <= TurnMath.SNAP_DEGREES) this else copy(armed = true)

    public fun angleFor(raw: Double): Double = if (armed) TurnMath.snap(raw) else raw

    public companion object {
        public fun startingAt(degrees: Double): KnobSnap {
            val q = TurnMath.nearestQuarter(degrees)
            return KnobSnap(q, armed = !(abs(degrees - q) <= TurnMath.SNAP_DEGREES && degrees != q))
        }
    }
}
