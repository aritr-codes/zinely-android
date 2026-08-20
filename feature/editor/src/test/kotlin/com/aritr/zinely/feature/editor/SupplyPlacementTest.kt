package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.PtSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `Given a fixing, When its family is read, Then it came from the copy and not from its id prefix`() {
        // THE trap this file exists to keep shut. Five prefixes carry four families: `fix.corner` is in
        // *Tape & fixings*, and a `substringBefore('.')` implementation throws on it instead.
        //
        // ⚠ This test used to go further and assert that a fixing lands at the SAME size as tape. It was
        // green, and it was pinning [D-092]: `fix.corner` is authored 1:1 and inherited tape's 4.5:1, so it
        // landed as a sliver over half the page wide with its pocket reduced to a slit. Family membership
        // and sizing were conflated here exactly as they were in the implementation — the test agreed with
        // the code because it was written from it. The membership half is what this test was for; it stays.
        assertEquals(Copy.Supplies.TAPE_AND_FIXINGS, benchSupplyFamily("fix.corner"))
        assertEquals(Copy.Supplies.TAPE_AND_FIXINGS, benchSupplyFamily("tape.torn"))
        assertEquals(Copy.Supplies.TAPE_AND_FIXINGS, benchSupplyFamily("fix.staple"))
    }

    @Test
    fun `Given a photo corner, When it is placed, Then it lands compact and square, not as tape`() {
        // [D-092], the defect this package closes, asserted as the two properties the device pass named:
        // the corner is not a sliver, and it is not half the page wide.
        val corner = benchSupplyPlacement("fix.corner", page)
        val tape = benchSupplyPlacement("tape.torn", page)

        assertEquals("a fixing is authored 1:1 and must land 1:1", corner.widthPt, corner.heightPt, 0.001)
        assertTrue("a photo corner must not land as tape does", corner.widthPt < tape.widthPt)
        assertTrue("a photo corner must not be half the page wide", corner.widthPt < page.width / 3.0)
        // Not vacuous in the other direction: tape must still land long, so the split moved the fixings
        // rather than flattening the family.
        assertTrue("tape must still land long", tape.widthPt > tape.heightPt * 2.0)
    }

    @Test
    fun `Given the three fixings, When they are placed, Then all three share one sizing constant`() {
        // The split is per *sizing group*, not per supply — a staple, a corner and a clip are the same kind
        // of compact object, and giving each its own number would be per-supply sizing with extra steps,
        // which is what §5.2 exists to refuse.
        val sizes = BenchFixings.map { benchSupplyPlacement(it, page) }.map { it.widthPt to it.heightPt }
        assertEquals("the fixings must share one constant", 1, sizes.toSet().size)
        // ⚠ Tied to the COPY LAYER, not to a count. `assertEquals(3, BenchFixings.size)` was the first
        // version and it is exactly the hole D-092 came through: a fifth member added to the family in
        // `Copy.Supplies` would silently inherit tape's 4.5:1 and reproduce the defect, with the suite
        // green. The set must be the family minus its tape.
        val family = Copy.Supplies.BY_FAMILY.getValue(Copy.Supplies.TAPE_AND_FIXINGS).keys
        assertEquals(
            "every member of Tape & fixings must be either tape or an enumerated fixing",
            family,
            BenchFixings + "tape.torn",
        )
    }

    @Test
    fun `Given the fixings key, When the families are read, Then it is not one of them`() {
        // It is a sizing key, not a fifth family: a fifth family would put a fifth heading on the Art sheet.
        assertFalse(
            "the fixings sizing key must never be mistaken for a family",
            BenchFixingsSizingKey in Copy.Supplies.BY_FAMILY.keys,
        )
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

    // ── benchSupplyReplacement — the owner's 2026-08-17 swap ruling ────────────────────────────────
    //
    // ⚠ These exist because the function shipped with **none**, and independent review proved it by
    // mutation: replacing the body with an origin-anchored, rotation-dropping version left the entire
    // suite BUILD SUCCESSFUL. The ruling this package was built to implement was asserted only in a KDoc.
    // Each test below is written against a specific mutation that previously passed.

    @Test
    fun `Given a swap between families, When replaced, Then the centre is preserved and the size is the new family's`() {
        // Mutation this kills: `xPt = current.xPt` (origin-anchored). With an aspect change the origin and
        // the centre disagree, so anchoring the wrong one walks the mark across the page on every swap.
        //
        // ⚠ The incoming id is `mark.asterisk`, NOT `mark.star` — the supply *named* "Star" carries the id
        // `asterisk` (Copy.kt's second naming departure). The first draft of this test wrote `mark.star`,
        // derived from the name, and the `require` in `benchSupplySizePt` threw. That loud failure is the
        // design working: a silent fallback size would have sized an unknown supply and passed.
        val outgoing = benchSupplyPlacement("tape.torn", page)
        val outgoingCentreX = outgoing.xPt + outgoing.widthPt / 2.0
        val outgoingCentreY = outgoing.yPt + outgoing.heightPt / 2.0

        val swapped = benchSupplyReplacement("mark.asterisk", page, outgoing)

        assertEquals(outgoingCentreX, swapped.xPt + swapped.widthPt / 2.0, 1e-9)
        assertEquals(outgoingCentreY, swapped.yPt + swapped.heightPt / 2.0, 1e-9)
        // …and the size is genuinely the INCOMING family's, not the outgoing one's.
        val freshStar = benchSupplyPlacement("mark.asterisk", page)
        assertEquals(freshStar.widthPt, swapped.widthPt, 1e-9)
        assertEquals(freshStar.heightPt, swapped.heightPt, 1e-9)
        // Guard the guard: this pair of supplies must actually differ in size, or the test proves nothing.
        assertNotEquals(outgoing.widthPt, swapped.widthPt, 1e-9)
    }

    @Test
    fun `Given a rotated supply, When replaced, Then the maker's rotation survives`() {
        // Mutation this kills: `rotationDegrees = 0.0`. §5.1 fixes the angle a supply ARRIVES at; it does
        // not entitle a swap to straighten one the maker turned.
        val rotated = benchSupplyPlacement("shape.rect", page).copy(rotationDegrees = 37.5)

        val swapped = benchSupplyReplacement("tape.torn", page, rotated)

        assertEquals(37.5, swapped.rotationDegrees, 1e-9)
    }

    @Test
    fun `Given any supply, When replaced, Then its size equals a fresh placement of the same supply`() {
        // Mutation this kills: preserving the outgoing transform wholesale. This is the ruling stated as an
        // invariant over the WHOLE cabinet rather than one example — "a replacement takes the incoming
        // family's scale" means exactly "the size a fresh placement would have given it".
        val outgoing = benchSupplyPlacement("tape.torn", page).copy(rotationDegrees = 12.0)
        Copy.Supplies.NAMES.keys.forEach { supplyId ->
            val swapped = benchSupplyReplacement(supplyId, page, outgoing)
            val fresh = benchSupplyPlacement(supplyId, page)
            assertEquals("$supplyId width", fresh.widthPt, swapped.widthPt, 1e-9)
            assertEquals("$supplyId height", fresh.heightPt, swapped.heightPt, 1e-9)
        }
    }

    @Test
    fun `Given a page that forces the clamp, When replaced, Then the clamp still applies`() {
        // The clamp lives in the extracted size helper, so both callers must get it. A replacement that
        // skipped it could hand a maker a supply taller than the page it sits on.
        val wide = PtSize(400.0, 40.0)
        val outgoing = benchSupplyPlacement("shape.rect", wide)

        val swapped = benchSupplyReplacement("tape.torn", wide, outgoing)

        assertTrue("the clamp must bound a replacement too", swapped.heightPt <= wide.height * 0.6 + 1e-9)
        assertTrue("and preserve the family's proportion", swapped.widthPt > swapped.heightPt * 2.0)
    }

    @Test
    fun `Given the same supply at the same size, When replaced by itself, Then the transform is unchanged`() {
        // The reducer's no-op short-circuit compares elements, so this is what makes "replace a rect with a
        // rect" push no undo entry. If this ever returns a different transform, that guard stops working
        // and the undo stack fills with entries that undo to the same picture.
        val current = benchSupplyPlacement("shape.rect", page)

        assertEquals(current, benchSupplyReplacement("shape.rect", page, current))
    }

    @Test
    fun `Given an id the cabinet does not hold, When it replaces one, Then it fails loudly too`() {
        assertThrows(IllegalArgumentException::class.java) {
            benchSupplyReplacement("shape.pentagon", page, benchSupplyPlacement("shape.rect", page))
        }
    }
}
