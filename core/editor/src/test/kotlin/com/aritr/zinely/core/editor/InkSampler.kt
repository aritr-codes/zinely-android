package com.aritr.zinely.core.editor

import com.aritr.zinely.core.render.SupplyInk

/**
 * Probe points for a holed supply, **generated from the same ink the renderer draws** (via
 * [SupplyInk.distancePt]) instead of written by hand, so a redraw that moves a hole moves the probes
 * with it (ART-4). Unit-square points, origin top-left.
 *
 * - [ink]: a cell whose eight neighbours are ink too (clear of the boundary, so no probe sits on an edge).
 * - [bare]: up to three cells from *enclosed* bare regions (a hole, not the outside), likewise interior.
 */
internal object InkSampler {
    private const val N = 50

    class Probes(val ink: Pair<Double, Double>, val bare: List<Pair<Double, Double>>)

    private fun inked(id: String, x: Int, y: Int) =
        SupplyInk.distancePt(id, ((x + 0.5) / N - 0.5) * 100.0, ((y + 0.5) / N - 0.5) * 100.0, 100.0, 100.0, false, false) == 0.0

    fun probes(id: String): Probes {
        val ink = Array(N) { y -> BooleanArray(N) { x -> inked(id, x, y) } }
        fun inside(x: Int, y: Int) = x in 0 until N && y in 0 until N
        fun interior(x: Int, y: Int, want: Boolean) =
            (-1..1).all { dy -> (-1..1).all { dx -> inside(x + dx, y + dy) && ink[y + dy][x + dx] == want } }
        // Outside = bare cells connected to the box edge.
        val outside = Array(N) { BooleanArray(N) }
        val stack = ArrayDeque<Int>()
        fun push(x: Int, y: Int) { if (!ink[y][x] && !outside[y][x]) { outside[y][x] = true; stack.add(y * N + x) } }
        for (i in 0 until N) { push(i, 0); push(i, N - 1); push(0, i); push(N - 1, i) }
        while (stack.isNotEmpty()) {
            val c = stack.removeLast(); val x = c % N; val y = c / N
            for ((dx, dy) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) if (inside(x + dx, y + dy)) push(x + dx, y + dy)
        }
        fun centre(x: Int, y: Int) = (x + 0.5) / N to (y + 0.5) / N
        val inkCell = (0 until N * N).map { it % N to it / N }.firstOrNull { (x, y) -> interior(x, y, true) }
            ?: error("$id: no interior ink cell at ${N}x$N; raise N")
        val holeCells = (0 until N * N).map { it % N to it / N }
            .filter { (x, y) -> !ink[y][x] && !outside[y][x] && interior(x, y, false) }
        check(holeCells.isNotEmpty()) { "$id is in IN_SCOPE but the sampler finds no enclosed hole" }
        val picked = listOf(holeCells.first(), holeCells[holeCells.size / 2], holeCells.last()).distinct()
        return Probes(centre(inkCell.first, inkCell.second), picked.map { centre(it.first, it.second) })
    }
}
