package com.aritr.zinely.core.render

import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

/**
 * The **tape** half of P3 (SUPPLIES-SPEC §3.5): what `SceneRenderer` emits for a `DecorElement`.
 *
 * §3.5 calls the emitted tape "the stronger test, and exactly the thing shared by all four surfaces" —
 * a transform asserted here is the same transform preview, PNG, PDF and the imposed sheet replay, so a
 * fold defect caught here is caught for all four at once and without a rasteriser's opinion in the way.
 *
 * Every assertion is written as **where a unit-square corner lands in page points**, not as matrix
 * coefficients. The coefficients are an implementation of the fold; the corners are the contract, and a
 * refactor that reassociates the multiplication should not be able to fail this file.
 */
class SceneRendererDecorTest {

    private val pageSize = PtSize(200.0, 300.0)
    private val defaults = DocumentDefaults()
    private val ink = ColorRgba(200, 40, 90)

    private fun decor(
        supplyId: String = "shape.rect",
        transform: Transform = Transform(10.0, 20.0, 40.0, 25.0),
        mirrored: Boolean = false,
    ) = DecorElement(
        id = "d1",
        transform = transform,
        zIndex = 0,
        supplyId = supplyId,
        ink = ink,
        mirrored = mirrored,
    )

    private fun tape(element: DecorElement) =
        SceneRenderer.render(Page(0, PageRole.INTERIOR, elements = listOf(element)), pageSize, defaults)

    private fun assertPoint(expX: Double, expY: Double, actual: PtPoint, eps: Double = 1e-9) {
        assertTrue(abs(expX - actual.x) <= eps, "x: expected $expX got ${actual.x}")
        assertTrue(abs(expY - actual.y) <= eps, "y: expected $expY got ${actual.y}")
    }

    @Test
    fun `given an authored supply, when rendered, then one DrawShape carries the catalogue outline`() {
        val cmds = tape(decor())

        assertEquals(1, cmds.size)
        val shape = cmds[0] as DrawShape
        assertSame(SupplyCatalog.outlineOf("shape.rect"), shape.outline)
        assertEquals(ink, shape.ink)
        // §3.2 constraint 1: the local space here is the unit square, so a points-valued clip would crop
        // the supply to a 1x1pt sliver. The command has no setter for it — this pins the invariant.
        assertNull(shape.localClip)
    }

    @Test
    fun `given a non-square element, when folded, then the unit square lands on the element box`() {
        // The §3.4.1 defect this exists to catch: without the scale term every supply renders 1pt x 1pt,
        // so (1,1) would land at (11,21) instead of the box's far corner. 40 x 25 is deliberately
        // non-uniform — `uniformScale()` has nothing true to say about this transform.
        val m = (tape(decor())[0] as DrawShape).localToPage

        assertPoint(10.0, 20.0, m.map(PtPoint(0.0, 0.0)))
        assertPoint(50.0, 45.0, m.map(PtPoint(1.0, 1.0)))
        assertPoint(30.0, 32.5, m.map(PtPoint(0.5, 0.5)))
    }

    @Test
    fun `given a rotated element, when folded, then the box rotates about its own centre`() {
        val m = (tape(decor(transform = Transform(10.0, 20.0, 40.0, 20.0, 90.0)))[0] as DrawShape).localToPage

        // Centre (30,30) is invariant under a rotation about itself; the corners swing around it. The
        // 40x20 box comes to occupy x 20..40, y 10..50 — width and height traded, which is the whole
        // observable consequence of a quarter turn and would not happen if the scale folded after it.
        assertPoint(30.0, 30.0, m.map(PtPoint(0.5, 0.5)), eps = 1e-9)
        assertPoint(40.0, 10.0, m.map(PtPoint(0.0, 0.0)), eps = 1e-9)
        assertPoint(20.0, 50.0, m.map(PtPoint(1.0, 1.0)), eps = 1e-9)
    }

    @Test
    fun `given mirrored, when folded, then the drawing reflects but the page box does not move`() {
        val plain = (tape(decor())[0] as DrawShape).localToPage
        val m = (tape(decor(mirrored = true))[0] as DrawShape).localToPage

        // x -> 1 - x in unit space: the two ends of the outline trade places...
        assertPoint(50.0, 20.0, m.map(PtPoint(0.0, 0.0)))
        assertPoint(10.0, 45.0, m.map(PtPoint(1.0, 1.0)))
        // ...while the occupied box is unchanged, which is what makes this a reflection of the *mark*
        // rather than of the placement. A mirrored supply must not jump across the page.
        val corners = listOf(PtPoint(0.0, 0.0), PtPoint(1.0, 0.0), PtPoint(1.0, 1.0), PtPoint(0.0, 1.0))
        fun bounds(t: com.aritr.zinely.core.model.AffineTransform2D) =
            corners.map { t.map(it) }.let { ps ->
                listOf(ps.minOf { it.x }, ps.minOf { it.y }, ps.maxOf { it.x }, ps.maxOf { it.y })
            }
        assertEquals(bounds(plain), bounds(m))
    }

    @Test
    fun `given an unauthored supplyId, when rendered, then nothing is emitted and nothing throws`() {
        // Twelve of the sixteen are still owed to a designer, and §2.2 puts this check here rather than
        // in the document validator precisely so an unknown id cannot make a zine refuse to open.
        assertTrue(tape(decor(supplyId = "tape.torn")).isEmpty())
        assertTrue(tape(decor(supplyId = "shape.rectangle")).isEmpty())
    }
}
