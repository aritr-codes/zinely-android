package com.aritr.zinely.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AffineTransform2DTest {
    private val eps = 1e-9

    private fun assertPoint(ex: Double, ey: Double, p: PtPoint) {
        assertEquals(ex, p.x, eps, "x")
        assertEquals(ey, p.y, eps, "y")
    }

    @Test
    fun `identity maps a point to itself`() {
        assertPoint(3.0, 7.0, AffineTransform2D.identity().map(PtPoint(3.0, 7.0)))
    }

    @Test
    fun `translate offsets a point`() {
        val t = AffineTransform2D.translate(10.0, -5.0)
        assertPoint(13.0, 2.0, t.map(PtPoint(3.0, 7.0)))
    }

    @Test
    fun `half turn about a center maps each corner to the opposite corner exactly`() {
        // panel rect (0,0,100,200) → center (50,100)
        val t = AffineTransform2D.halfTurnAbout(50.0, 100.0)
        assertPoint(50.0, 100.0, t.map(PtPoint(50.0, 100.0))) // center is fixed
        assertPoint(100.0, 200.0, t.map(PtPoint(0.0, 0.0)))   // top-left → bottom-right
        assertPoint(0.0, 0.0, t.map(PtPoint(100.0, 200.0)))   // bottom-right → top-left
    }

    @Test
    fun `half turn is exact (no floating-point residue) and self-inverse`() {
        val t = AffineTransform2D.halfTurnAbout(50.0, 100.0)
        // exact: component c and b are precisely zero (unlike a trig 180 rotation)
        assertEquals(0.0, t.b)
        assertEquals(0.0, t.c)
        val p = PtPoint(12.0, 34.0)
        assertPoint(p.x, p.y, t.map(t.map(p)))
    }

    @Test
    fun `then applies this first, then the next transform`() {
        val a = AffineTransform2D.translate(1.0, 0.0)
        val b = AffineTransform2D.halfTurnAbout(0.0, 0.0) // (x,y) → (-x,-y)
        // apply a then b to (0,0): a→(1,0), b→(-1,0)
        assertPoint(-1.0, 0.0, a.then(b).map(PtPoint(0.0, 0.0)))
    }

    @Test
    fun `rotateDeg 90 about origin maps unit-x to unit-y`() {
        assertPoint(0.0, 1.0, AffineTransform2D.rotateDeg(90.0).map(PtPoint(1.0, 0.0)))
    }

    // — scale (SUPPLIES-SPEC §3.4.1) — the factory that did not exist, and without which every supply
    // would render 1pt × 1pt because `localToPage` has translate and rotate but no scale term.

    @Test
    fun `given a unit square, when scaled non-uniformly, then each axis takes its own factor`() {
        val t = AffineTransform2D.scale(120.0, 40.0)
        assertPoint(0.0, 0.0, t.map(PtPoint(0.0, 0.0)))
        assertPoint(120.0, 40.0, t.map(PtPoint(1.0, 1.0)))
        // The whole point of two factors: a uniform-scale API would put 120 (or 40) on both axes here.
        assertPoint(60.0, 40.0, t.map(PtPoint(0.5, 1.0)))
    }

    @Test
    fun `scale is about the origin, so it moves an off-origin point`() {
        assertPoint(6.0, 21.0, AffineTransform2D.scale(2.0, 3.0).map(PtPoint(3.0, 7.0)))
    }

    @Test
    fun `scale by one is the identity and a negative factor reflects`() {
        assertEquals(AffineTransform2D.identity(), AffineTransform2D.scale(1.0, 1.0))
        // §2's `mirrored` does NOT use this (Transform has no scale term and the validator rejects
        // non-positive size) — but the matrix itself must still reflect, since the unit-space mirror in
        // the supplies fold is an x-reflection composed here.
        assertPoint(-4.0, 5.0, AffineTransform2D.scale(-1.0, 1.0).map(PtPoint(4.0, 5.0)))
    }

    @Test
    fun `composition order matters - scale then translate is not translate then scale`() {
        val s = AffineTransform2D.scale(2.0, 2.0)
        val t = AffineTransform2D.translate(10.0, 0.0)
        // scale first, then translate: (1,0) → (2,0) → (12,0)
        assertPoint(12.0, 0.0, s.then(t).map(PtPoint(1.0, 0.0)))
        // translate first, then scale: (1,0) → (11,0) → (22,0)
        assertPoint(22.0, 0.0, t.then(s).map(PtPoint(1.0, 0.0)))
    }

    @Test
    fun `the supplies fold places a unit-square outline into its element box`() {
        // SUPPLIES-SPEC §3.4.1: translate(x,y) · [T(c)·R(deg)·T(-c)] · scale(w,h), at 0° rotation.
        val w = 120.0
        val h = 40.0
        val fold = AffineTransform2D.translate(30.0, 50.0).times(AffineTransform2D.scale(w, h))
        assertPoint(30.0, 50.0, fold.map(PtPoint(0.0, 0.0)))       // unit top-left → box top-left
        assertPoint(150.0, 90.0, fold.map(PtPoint(1.0, 1.0)))      // unit bottom-right → box bottom-right
        assertPoint(90.0, 70.0, fold.map(PtPoint(0.5, 0.5)))       // unit centre → box centre
    }

    @Test
    fun `times pins every component of the matrix product (apply other first)`() {
        val a = AffineTransform2D(2.0, 0.0, 0.0, 3.0, 1.0, 1.0) // scale(2,3) then translate(1,1)
        val b = AffineTransform2D.translate(5.0, 7.0)
        // a.times(b) applies b first then a: (x,y) -> (2(x+5)+1, 3(y+7)+1) = (2x+11, 3y+22)
        assertEquals(AffineTransform2D(2.0, 0.0, 0.0, 3.0, 11.0, 22.0), a.times(b))
        assertPoint(11.0, 22.0, a.times(b).map(PtPoint(0.0, 0.0)))
    }
}
