package com.aritr.zinely.core.model

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A 2D affine transform `[a c e ; b d f ; 0 0 1]` mapping a point `(x, y)` to
 * `(a*x + c*y + e, b*x + d*y + f)`. Column-vector convention, matching SVG and
 * `android.graphics.Matrix`, so a consumer can apply it directly without re-deriving math.
 *
 * This is the authoritative transform a [PanelPlacement] carries: content authored upright
 * in panel-local space is mapped to sheet space by [PanelPlacement.contentToSheet].
 */
public data class AffineTransform2D(
    val a: Double,
    val b: Double,
    val c: Double,
    val d: Double,
    val e: Double,
    val f: Double,
) {
    /** Maps a point through this transform. */
    public fun map(p: PtPoint): PtPoint =
        PtPoint(a * p.x + c * p.y + e, b * p.x + d * p.y + f)

    /** Matrix product `this · other` — i.e. apply [other] first, then `this`. */
    public fun times(other: AffineTransform2D): AffineTransform2D =
        AffineTransform2D(
            a = a * other.a + c * other.b,
            b = b * other.a + d * other.b,
            c = a * other.c + c * other.d,
            d = b * other.c + d * other.d,
            e = a * other.e + c * other.f + e,
            f = b * other.e + d * other.f + f,
        )

    /** The transform equivalent to applying `this` first, then [next]. */
    public fun then(next: AffineTransform2D): AffineTransform2D = next.times(this)

    public companion object {
        public fun identity(): AffineTransform2D =
            AffineTransform2D(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)

        public fun translate(tx: Double, ty: Double): AffineTransform2D =
            AffineTransform2D(1.0, 0.0, 0.0, 1.0, tx, ty)

        /**
         * An **exact** 180° rotation about `(cx, cy)`: `(x, y) → (2cx - x, 2cy - y)`.
         * Exact (no trigonometric residue) because the imposition only ever needs half-turns;
         * this keeps panel transforms deterministic and round-trip-clean.
         */
        public fun halfTurnAbout(cx: Double, cy: Double): AffineTransform2D =
            AffineTransform2D(-1.0, 0.0, 0.0, -1.0, 2.0 * cx, 2.0 * cy)

        /** General rotation by [deg] degrees about the origin (trigonometric; carries float residue). */
        public fun rotateDeg(deg: Double): AffineTransform2D {
            val r = deg * PI / 180.0
            val cosr = cos(r)
            val sinr = sin(r)
            return AffineTransform2D(cosr, sinr, -sinr, cosr, 0.0, 0.0)
        }
    }
}
