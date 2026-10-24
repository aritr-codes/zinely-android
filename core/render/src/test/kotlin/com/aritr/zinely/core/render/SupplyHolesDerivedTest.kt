package com.aritr.zinely.core.render

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * ART-4: the holed set is a **named list** (SUPPLIES-SPEC H9, ADR-139), and this test is what keeps the
 * list honest against the drawings. It flood-fills each outline's sampled [isInked] grid; a bare region
 * that cannot reach the box edge is a hole. For every outline present in this build, the ids that have a
 * hole must be exactly [SupplyInk.IN_SCOPE], less the exclusions the owner ruled, which are named here
 * with their reason and nowhere else.
 */
class SupplyHolesDerivedTest {

    /**
     * Ids that have a hole by geometry but are out of [SupplyInk.IN_SCOPE] by ruling, with the reason.
     * Empty today: `mark.crop` (decision gate Q3) has no enclosed region, so it needs no exclusion. An
     * entry here is a reviewed act, and the second test fails if it stops being true.
     */
    private val excludedByRuling: Map<String, String> = emptyMap()

    private val n = 100

    /** Smallest enclosed bare region that counts as a hole, in grid cells (0.04% of the box). */
    private val minHoleCells = 4

    private fun hasHole(o: SupplyOutline): Boolean {
        val bare = Array(n) { y -> BooleanArray(n) { x -> !o.isInked((x + 0.5) / n, (y + 0.5) / n) } }
        val seen = Array(n) { BooleanArray(n) }
        fun fill(sx: Int, sy: Int): Int {
            var cells = 0
            val stack = ArrayDeque<Int>().apply { add(sy * n + sx) }
            seen[sy][sx] = true
            while (stack.isNotEmpty()) {
                val c = stack.removeLast(); val x = c % n; val y = c / n
                cells++
                for ((dx, dy) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                    val nx = x + dx; val ny = y + dy
                    if (nx in 0 until n && ny in 0 until n && bare[ny][nx] && !seen[ny][nx]) {
                        seen[ny][nx] = true; stack.add(ny * n + nx)
                    }
                }
            }
            return cells
        }
        // The outside first: every bare region touching the box edge.
        for (i in 0 until n) for ((x, y) in listOf(i to 0, i to n - 1, 0 to i, n - 1 to i)) {
            if (bare[y][x] && !seen[y][x]) fill(x, y)
        }
        for (y in 0 until n) for (x in 0 until n) {
            if (bare[y][x] && !seen[y][x] && fill(x, y) >= minHoleCells) return true
        }
        return false
    }

    @Test
    fun `given every outline present, when sampled for holes, then the holed ids are exactly IN_SCOPE less the rulings`() {
        val holed = SupplyCatalog.OUTLINES.filterValues { hasHole(it) }.keys
        assertEquals(
            SupplyInk.IN_SCOPE.sorted(),
            (holed - excludedByRuling.keys).sorted(),
            "a piece with a hole must be named in SupplyInk.IN_SCOPE (SUPPLIES-SPEC H9), or excluded here by ruling",
        )
    }

    @Test
    fun `given each ruled exclusion, then it really has a hole and really is out of scope`() {
        for (id in excludedByRuling.keys) {
            assertEquals(true, hasHole(SupplyCatalog.OUTLINES.getValue(id)), "$id: the exclusion is stale (no hole)")
            assertEquals(false, id in SupplyInk.IN_SCOPE, "$id is excluded by ruling but listed")
        }
    }

    @Test
    fun `given every IN_SCOPE id, then it has an outline in this build`() {
        for (id in SupplyInk.IN_SCOPE) assertEquals(true, id in SupplyCatalog.OUTLINES, "$id has no outline")
    }
}
