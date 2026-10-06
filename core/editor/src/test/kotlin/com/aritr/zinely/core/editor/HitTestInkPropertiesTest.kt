package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Element
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.render.DrawShape
import com.aritr.zinely.core.render.SceneRenderer
import com.aritr.zinely.core.render.SupplyInk
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Combinators
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import net.jqwik.api.constraints.DoubleRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.math.abs

/**
 * ADR-124's two properties: the hit hole is the hole the renderer draws (13), and the three-pass hit
 * test differs from the box-only one it replaced only where the ADR says it may (14).
 */
class HitTestInkPropertiesTest {

    private fun page(els: List<Element>) = Page(index = 0, role = PageRole.INTERIOR, elements = els)

    // — 13: the hit hole is the drawn hole —

    @Property
    fun `a photo corner is hit where the renderer draws it, at any rotation, flip and box`(
        @ForAll @DoubleRange(min = -180.0, max = 180.0) rot: Double,
        @ForAll mirrored: Boolean,
        @ForAll flipped: Boolean,
        @ForAll @DoubleRange(min = 20.0, max = 200.0) w: Double,
        @ForAll @DoubleRange(min = 20.0, max = 200.0) h: Double,
    ) {
        val corner = DecorElement(
            "corner", Transform(300.0, 300.0, w, h, rot), 1, "fix.corner", ColorRgba(0, 0, 0), mirrored, flipped,
        )
        val photo = ImageElement("photo", Transform(0.0, 0.0, 800.0, 800.0), 0, assetId = "sha")
        val p = page(listOf(photo, corner))
        // The transform the renderer ACTUALLY emits for this element — not a second derivation of it.
        val fold = SceneRenderer.buildScene(p, PtSize(800.0, 800.0), DocumentDefaults())
            .commands.filterIsInstance<DrawShape>().single().localToPage

        for ((u, v) in listOf(0.06 to 0.9, 0.5 to 0.95)) {
            assertEquals("corner", HitTest.topmostAt(p, fold.map(PtPoint(u, v))), "ink at unit ($u, $v)")
        }
        // The pocket, and the half of the box that was never ink.
        for ((u, v) in listOf(0.25 to 0.7, 0.9 to 0.2)) {
            assertEquals("photo", HitTest.topmostAt(p, fold.map(PtPoint(u, v))), "bare paper at unit ($u, $v)")
        }
    }

    // — 14: what changed, and what may not —

    /** `HitTest.topmostAt` as it stood before ADR-124, kept here as the oracle. */
    private fun boxOnly(page: Page, pt: PtPoint): String? =
        topmostFirst(page).firstOrNull { el ->
            val t = el.transform
            val cx = t.xPt + t.widthPt / 2.0
            val cy = t.yPt + t.heightPt / 2.0
            val local = AffineTransform2D.rotateDeg(-t.rotationDegrees).map(PtPoint(pt.x - cx, pt.y - cy))
            abs(local.x) <= t.widthPt / 2.0 && abs(local.y) <= t.heightPt / 2.0
        }?.id

    private fun topmostFirst(page: Page): List<Element> = page.elements.withIndex()
        .sortedWith(compareByDescending<IndexedValue<Element>> { it.value.zIndex }.thenByDescending { it.index })
        .map { it.value }

    private fun isInScope(el: Element?) = el is DecorElement && el.supplyId in SupplyInk.IN_SCOPE

    private val outOfScope = listOf("mark.crop", "shape.rect", "fix.staple", "mark.halftone", "zzz.unknown")

    private fun transforms(): Arbitrary<Transform> = Combinators.combine(
        Arbitraries.doubles().between(0.0, 200.0),
        Arbitraries.doubles().between(0.0, 200.0),
        Arbitraries.doubles().between(10.0, 150.0),
        Arbitraries.doubles().between(10.0, 150.0),
        Arbitraries.of(0.0, 0.0, 90.0, 33.0, -120.0, 180.0),
    ).`as` { x, y, w, h, rot -> Transform(x, y, w, h, rot) }

    /** A lazily-id'd element: kind 0 photo, 1 words, otherwise a piece with [supplyIds]. */
    private fun elements(supplyIds: List<String>): Arbitrary<(Int) -> Element> = Combinators.combine(
        Arbitraries.integers().between(0, 5),
        transforms(),
        Arbitraries.integers().between(0, 3),
        Arbitraries.of(supplyIds),
        Arbitraries.of(true, false),
        Arbitraries.of(true, false),
    ).`as` { kind, t, z, supply, mirrored, flipped ->
        { i: Int ->
            when (kind) {
                0 -> ImageElement("e$i", t, z, assetId = "sha")
                1 -> TextElement("e$i", t, z, text = "x")
                else -> DecorElement("e$i", t, z, supply, ColorRgba(0, 0, 0), mirrored, flipped)
            }
        }
    }

    private fun pages(supplyIds: List<String>): Arbitrary<Page> =
        elements(supplyIds).list().ofMinSize(1).ofMaxSize(6).map { makers -> page(makers.mapIndexed { i, make -> make(i) }) }

    @Provide
    fun anyPage(): Arbitrary<Page> = pages(SupplyInk.IN_SCOPE.toList() + outOfScope)

    @Provide
    fun pageWithoutHoledPieces(): Arbitrary<Page> = pages(outOfScope)

    @Provide
    fun tolerance(): Arbitrary<Double> = Arbitraries.of(0.0, 8.0)

    @Property(tries = 5000)
    fun `the answer differs from the box-only test only where ADR-124 says it may`(
        @ForAll("anyPage") p: Page,
        @ForAll @DoubleRange(min = -20.0, max = 370.0) x: Double,
        @ForAll @DoubleRange(min = -20.0, max = 370.0) y: Double,
        @ForAll("tolerance") tolerancePt: Double,
    ) {
        val pt = PtPoint(x, y)
        val before = boxOnly(p, pt)
        val after = HitTest.topmostAt(p, pt, tolerancePt)
        val order = topmostFirst(p)
        val beforeEl = order.firstOrNull { it.id == before }
        val afterEl = order.firstOrNull { it.id == after }

        // A tap that selected something still selects something.
        if (before != null) assertNotNull(after, "$before was hit by its box and is now nothing")
        if (after == before) return

        // It differs only when the present answer is a holed piece, or nothing.
        assertTrue(before == null || isInScope(beforeEl), "$before is not a holed piece, and lost the tap to $after")
        // …and then the new answer is a holed piece, or the element beneath the one that gave the tap up.
        assertTrue(
            isInScope(afterEl) || (beforeEl != null && order.indexOf(afterEl) > order.indexOf(beforeEl)),
            "$after took the tap from $before and is neither a holed piece nor beneath it",
        )
        // Whatever is beneath was reached exactly: its own box holds the point.
        if (!isInScope(afterEl)) assertTrue(HitTest.contains(afterEl!!, pt), "$after does not contain the tap")
        // Without a tolerance nothing is gained from blank paper.
        if (tolerancePt == 0.0) assertNotNull(before, "tolerance 0 turned a miss into $after")
    }

    @Property(tries = 2000)
    fun `on a page with no holed piece the two tests always agree`(
        @ForAll("pageWithoutHoledPieces") p: Page,
        @ForAll @DoubleRange(min = -20.0, max = 370.0) x: Double,
        @ForAll @DoubleRange(min = -20.0, max = 370.0) y: Double,
        @ForAll("tolerance") tolerancePt: Double,
    ) {
        val pt = PtPoint(x, y)
        assertEquals(boxOnly(p, pt), HitTest.topmostAt(p, pt, tolerancePt))
    }
}
