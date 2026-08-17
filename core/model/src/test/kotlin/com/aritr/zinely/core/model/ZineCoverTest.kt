package com.aritr.zinely.core.model

import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The cover **assigner** — `newZineCoverRecipe` ([ADR-086](docs/DECISIONS.md#adr-086) rows 10, 11).
 *
 * ### Why this file exists, and why it did not exist a moment ago
 *
 * B1 shipped the cover with **no assigner at all**: independent review found that no reflection guard
 * could hold *"never derived from the title"* against a function with no caller to check, so the assigner
 * and its guard were both deferred to the package that would give it one. B5 landed the assigner and, on
 * the first pass, not these tests — and the mid-package adversarial review proved the gap by applying row
 * 11's own planned mutation, `stamp = ZineCoverStamp.entries[surface.ordinal]`, and watching **all four
 * module suites stay green**. Every neighbouring test compares whole recipes for *inequality*, and a
 * one-axis assigner satisfies every one of them.
 *
 * That is the ninth consecutive package to produce an assertion blind to the defect class it claimed to
 * gate — with the distinction that here the assertion was **absent** while its table row read ✅. Hence
 * [ADR-087](docs/DECISIONS.md#adr-087): a row terminates on an artifact, never on an intention.
 *
 * ### Seeded, not statistical
 *
 * Every draw below is over a pinned [Random] seed, so a failure is a defect rather than a bad afternoon.
 * The claims are structural — *both axes move*, and *neither axis is a function of the other* — which is
 * what "vary independently" means and what a correlated assigner actually breaks.
 */
class ZineCoverTest {

    @Test
    fun `both axes take more than one value over a seeded run`() {
        val draws = draws(SAMPLE, seed = 11)

        // The floor: an assigner that returned a constant on either axis would still produce covers, and
        // would still pass every "a cover exists" assertion in the programme.
        assertTrue(
            draws.map { it.surface }.distinct().size >= 2,
            "every zine got the same surface — the surface axis is not being drawn",
        )
        assertTrue(
            draws.map { it.stamp }.distinct().size >= 2,
            "every zine got the same stamp — the stamp axis is not being drawn",
        )
    }

    @Test
    fun `the stamp is not a function of the surface`() {
        // **This is row 11's assertion**, and it is the one the review's mutation walked straight through.
        // `stamp = entries[surface.ordinal]` still yields many distinct *recipes*, so inequality tests are
        // blind to it; what it destroys is the independence — under it, one surface never appears with two
        // different stamps. A single such pair refutes the functional dependency outright.
        val bySurface = draws(SAMPLE, seed = 29).groupBy({ it.surface }, { it.stamp })
        val varying = bySurface.filterValues { it.distinct().size >= 2 }

        assertTrue(
            varying.isNotEmpty(),
            "no surface was ever drawn with two different stamps — the stamp is derived from the surface",
        )
    }

    @Test
    fun `the surface is not a function of the stamp`() {
        // The mirror of the row's mutation, which is just as available and just as invisible to an
        // inequality test. Independence is symmetric; asserting it in one direction only would be half a
        // claim, and the half left out is the one nobody writes.
        val byStamp = draws(SAMPLE, seed = 97).groupBy({ it.stamp }, { it.surface })

        assertTrue(
            byStamp.filterValues { it.distinct().size >= 2 }.isNotEmpty(),
            "no stamp was ever drawn with two different surfaces — the surface is derived from the stamp",
        )
    }

    @Test
    fun `every surface and every stamp is reachable`() {
        // A palette with an unreachable member is a design that quietly shipped fewer covers than it drew
        // — `entries.size - 1` in an index expression, and nothing else in the suite would notice.
        val draws = draws(REACH_SAMPLE, seed = 5)
        assertEquals(
            ZineCoverSurface.entries.toSet(),
            draws.map { it.surface }.toSet(),
            "some surfaces can never be assigned",
        )
        assertEquals(
            ZineCoverStamp.entries.toSet(),
            draws.map { it.stamp }.toSet(),
            "some stamps can never be assigned",
        )
    }

    @Test
    fun `the same seed draws the same recipes`() {
        // Not a property of the design — a property of the *seam*. The assigner takes its entropy as a
        // parameter, which is what lets every persistence test above it pin a run; a hidden internal
        // source would make those tests unrepeatable while looking identical here.
        assertEquals(draws(SAMPLE, seed = 3), draws(SAMPLE, seed = 3))
    }

    private fun draws(count: Int, seed: Int): List<ZineCoverRecipe> {
        val random = Random(seed)
        return List(count) { newZineCoverRecipe(random) }
    }

    private companion object {
        /** Enough draws that independence is decidable, few enough that a failure is readable. */
        const val SAMPLE = 200

        /** 6 × 6 cells: coupon-collector reach is comfortable well inside this. */
        const val REACH_SAMPLE = 600
    }
}
