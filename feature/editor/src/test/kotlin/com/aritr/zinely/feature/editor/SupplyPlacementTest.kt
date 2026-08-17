package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.PtSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Where a supply lands and at what size — SUPPLIES-SPEC §5 / §5.1 / §5.2, as implemented by
 * [benchSupplyPlacement].
 *
 * Pure JUnit, no Robolectric: the function is platform-free, and a device harness would only make these
 * assertions slower and less trustworthy.
 */
class SupplyPlacementTest {

    /** Roughly an eighth of Letter, portrait — the shape the product actually makes. */
    private val page = PtSize(153.0, 198.0)

    @Test
    fun `Given any supply, When it is placed, Then it is centred on the page and flat`() {
        for (id in Copy.Supplies.NAMES.keys) {
            val t = benchSupplyPlacement(id, page)
            assertEquals("$id must be centred horizontally", page.width / 2.0, t.xPt + t.widthPt / 2.0, 1e-9)
            assertEquals("$id must be centred vertically", page.height / 2.0, t.yPt + t.heightPt / 2.0, 1e-9)
            // §5.1 — the tilt clause is withdrawn. A deterministic-hash tilt would show up here as a
            // non-zero angle on some ids and not others, which is exactly the defect §5.1 describes.
            assertEquals("$id must land flat", 0.0, t.rotationDegrees, 0.0)
        }
    }

    @Test
    fun `Given any supply, When it is placed, Then it fits on the page with room to compose`() {
        for (id in Copy.Supplies.NAMES.keys) {
            val t = benchSupplyPlacement(id, page)
            assertTrue("$id must have a positive box", t.widthPt > 0.0 && t.heightPt > 0.0)
            assertTrue("$id must start inside the page", t.xPt >= 0.0 && t.yPt >= 0.0)
            assertTrue("$id must end inside the page", t.xPt + t.widthPt <= page.width + 1e-9)
            assertTrue("$id must end inside the page", t.yPt + t.heightPt <= page.height + 1e-9)
        }
    }

    @Test
    fun `Given the four families, When their supplies are placed, Then each family has its own size`() {
        // §5.2's ruling, asserted as the property it states rather than as four literals: the four defaults
        // are distinct. Pinning the numbers here would make this test a copy of the constants it checks —
        // and they are an implementation reading awaiting a ruling, so they are the wrong thing to freeze.
        val areas = Copy.Supplies.BY_FAMILY.mapValues { (_, supplies) ->
            val t = benchSupplyPlacement(supplies.keys.first(), page)
            t.widthPt * t.heightPt
        }
        assertEquals(4, areas.size)
        assertEquals("each family must land at its own size", 4, areas.values.toSet().size)
    }

    @Test
    fun `Given tape and a stamp, When both are placed, Then tape lands long and the stamp lands small`() {
        // §5.2's two unambiguous adjectives. (Its third — "a rule lands wide" — names `shape.rule`, which
        // is not a family, so a per-family constant cannot express it. That mismatch is flagged in
        // SupplyPlacement.kt's header and is owed a ruling; it is deliberately NOT asserted here.)
        val tape = benchSupplyPlacement("tape.torn", page)
        val stamp = benchSupplyPlacement("mark.asterisk", page)

        assertTrue("tape must land long", tape.widthPt > tape.heightPt * 2.0)
        assertTrue("a stamp must land small", stamp.widthPt * stamp.heightPt < tape.widthPt * tape.heightPt)
        assertTrue("a stamp must be a fraction of the page", stamp.widthPt < page.width / 4.0)
    }

    @Test
    fun `Given the straight rule, When it is placed, Then it lands wide and not as its family's square`() {
        // §5.2's third adjective, and the only one of the three a maker can currently reach — `Cut shapes`
        // is the sole family with authored outlines. A per-family-only reading landed it in `shape.rect`'s
        // box; independent review showed that made the spec's own example the one thing S7 got wrong.
        val rule = benchSupplyPlacement("shape.rule", page)
        val rect = benchSupplyPlacement("shape.rect", page)

        assertTrue("a rule must land wide", rule.widthPt > rule.heightPt * 3.0)
        assertTrue("…wider than its family-mate", rule.widthPt > rect.widthPt)
        assertNotEquals(rect, rule)
        // …and it is still centred and flat, i.e. the override changes the size and nothing else.
        assertEquals(page.width / 2.0, rule.xPt + rule.widthPt / 2.0, 1e-9)
        assertEquals(0.0, rule.rotationDegrees, 0.0)
    }

    @Test
    fun `Given the exception list, When it is read, Then it holds only the supply the spec names`() {
        // The override exists because §5.2 names `shape.rule` by name. A second entry would be the
        // per-family rule quietly giving up, so the count is pinned rather than left to drift.
        assertEquals(setOf("shape.rule"), BenchSupplyOverrides.keys)
    }

    @Test
    fun `Given a fixing, When it is placed, Then its family came from the copy and not from its id prefix`() {
        // THE trap this file exists to keep shut. Five prefixes carry four families: `fix.corner` is in
        // *Tape & fixings*, so its default must equal `tape.torn`'s and must NOT be some `fix` family's.
        // A `substringBefore('.')` implementation throws on `fix.corner` instead — this test goes red.
        assertEquals(Copy.Supplies.TAPE_AND_FIXINGS, benchSupplyFamily("fix.corner"))
        assertEquals(Copy.Supplies.TAPE_AND_FIXINGS, benchSupplyFamily("tape.torn"))
        assertEquals(
            benchSupplyPlacement("tape.torn", page),
            benchSupplyPlacement("fix.corner", page),
        )
        // …and a different family really is different, so the assertion above is not vacuous.
        assertNotEquals(
            benchSupplyPlacement("tape.torn", page),
            benchSupplyPlacement("shape.rect", page),
        )
    }

    @Test
    fun `Given every one of the sixteen, When its family is read, Then the copy layer names one`() {
        for (id in Copy.Supplies.NAMES.keys) {
            assertTrue("$id must belong to a family", benchSupplyFamily(id) != null)
        }
    }

    @Test
    fun `Given a page of another size, When a supply is placed, Then the size scales with the page`() {
        val big = benchSupplyPlacement("shape.rect", PtSize(306.0, 396.0))
        val small = benchSupplyPlacement("shape.rect", page)
        assertEquals(2.0, big.widthPt / small.widthPt, 1e-9)
    }

    @Test
    fun `Given a page wider than it is tall, When tape is placed, Then it is clamped and stays tape-shaped`() {
        val wide = PtSize(400.0, 40.0)
        val t = benchSupplyPlacement("tape.torn", wide)
        assertTrue("the clamp must keep it on the page", t.heightPt <= wide.height * 0.6 + 1e-9)
        assertTrue("the clamp must preserve the family's proportion", t.widthPt > t.heightPt * 2.0)
        assertEquals(wide.width / 2.0, t.xPt + t.widthPt / 2.0, 1e-9)
    }

    @Test
    fun `Given an id the cabinet does not hold, When it is placed, Then it fails loudly`() {
        // A silent fallback size would be the app inventing craft knowledge it does not have.
        assertThrows(IllegalArgumentException::class.java) {
            benchSupplyPlacement("shape.pentagon", page)
        }
    }
}
