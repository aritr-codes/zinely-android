package com.aritr.zinely.feature.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **What an N-page one-sheet zine physically opens to** — [bookletLeaves], the pure half of ADR-101 P5.
 *
 * The frozen prototype writes the eight-page answer down as a literal (`SPREADS`). Deriving it instead is
 * only worth doing if the derivation is pinned, and the thing worth pinning is not the arithmetic — it is
 * the **physical claim**: the cover is a free sheet bound on its left, the back cover is a free sheet bound
 * on its right, and everything between them faces a partner with the spine between them. Get the side
 * wrong and the gutter, the corner radii and the turn's pivot are all wrong together, silently, in a way a
 * screenshot at one page count will not show.
 *
 * A plain JVM test on purpose: no Robolectric, no Compose. The model has no framework in it, which is the
 * point of extracting it.
 */
class BookletLeavesTest {

    /** The frozen table, verbatim: `[[null,1],[2,3],[4,5],[6,7],[8,null]]`. */
    @Test
    fun `eight pages reproduce the frozen SPREADS table exactly`() {
        val leaves = bookletLeaves(8)

        assertEquals(
            listOf(
                ReadLeaf(1, spineOnLeft = true, solo = true),
                ReadLeaf(2, spineOnLeft = false, solo = false),
                ReadLeaf(3, spineOnLeft = true, solo = false),
                ReadLeaf(4, spineOnLeft = false, solo = false),
                ReadLeaf(5, spineOnLeft = true, solo = false),
                ReadLeaf(6, spineOnLeft = false, solo = false),
                ReadLeaf(7, spineOnLeft = true, solo = false),
                ReadLeaf(8, spineOnLeft = false, solo = true),
            ),
            leaves,
        )
    }

    /**
     * **Reading order is the list order, and that is the simplification.** The prototype tracks a
     * `(spread, side)` pair and can represent positions that are not pages; a flat list cannot, so turning
     * a page is `index ± 1` and no clamping arithmetic is duplicated at the two edges.
     */
    @Test
    fun `every page appears exactly once, in reading order, whatever the count`() {
        for (count in 1..40) {
            val numbers = bookletLeaves(count).map { it.pageNumber }
            assertEquals("page count $count", (1..count).toList(), numbers)
        }
    }

    /**
     * The physical invariant, stated once and checked at every size: **facing leaves are bound towards each
     * other.** A left-hand page's spine is on its right and its partner's is on its left, so the two
     * gutters meet in the middle. Any leaf that faces nothing is `solo` and draws neither.
     */
    @Test
    fun `facing leaves are bound towards each other and free sheets face nothing`() {
        for (count in 2..40) {
            val leaves = bookletLeaves(count)
            val first = leaves.first()
            val last = leaves.last()

            assertTrue("cover is a free sheet at $count", first.solo && first.spineOnLeft)
            assertTrue("back cover is a free sheet at $count", last.solo && !last.spineOnLeft)

            leaves.filter { !it.solo }.chunked(2).forEach { pair ->
                assertEquals("interior leaves come in facing pairs at $count", 2, pair.size)
                assertTrue("the left leaf of a pair is bound on its right", !pair[0].spineOnLeft)
                assertTrue("the right leaf of a pair is bound on its left", pair[1].spineOnLeft)
                assertEquals("a pair is consecutive", pair[0].pageNumber + 1, pair[1].pageNumber)
            }
        }
    }

    /**
     * Odd counts cannot reach a printed zine — every format seeds an even sheet — but they reach unit tests
     * and in-progress documents, and a model that throws or mis-pairs there is a crash waiting for the
     * first format that changes. The middle pairs simply run out: the last interior leaf faces nothing.
     */
    @Test
    fun `an odd page count leaves the last interior leaf unfaced rather than mis-paired`() {
        val leaves = bookletLeaves(7)

        assertEquals((1..7).toList(), leaves.map { it.pageNumber })
        // 1 alone · 2|3 · 4|5 · 6 alone · 7 alone.
        assertTrue("page 6 has no partner", leaves[5].solo)
        assertTrue("page 7 is the back cover", leaves[6].solo)
    }

    @Test
    fun `degenerate counts do not throw`() {
        assertEquals(emptyList<ReadLeaf>(), bookletLeaves(0))
        assertEquals(emptyList<ReadLeaf>(), bookletLeaves(-3))
        assertEquals(listOf(ReadLeaf(1, spineOnLeft = true, solo = true)), bookletLeaves(1))
        // Two pages are a cover and a back, both free sheets, nothing bound.
        assertTrue(bookletLeaves(2).all { it.solo })
    }
}
