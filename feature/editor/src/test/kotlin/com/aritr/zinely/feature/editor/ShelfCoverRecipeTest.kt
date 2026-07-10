package com.aritr.zinely.feature.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The frozen Shelf's generated cover recipe (`shelf.html` `hash`/`recipe`). Gradle runs unit tests
 * with the module directory as cwd, so the spec cannot be read at test time — the expectations below
 * are hand-derived from the spec's own seed titles and pinned, exactly as `ZinelyTokensTest` pins the
 * token literals. Drift fails the build.
 */
class ShelfCoverRecipeTest {

    @Test
    fun `hash sums utf-16 code units, matching the spec's charCodeAt loop`() {
        // "Corner Store Poems" = 1722, "Tuesday" = 735 (sum of char codes, no wrap at these sizes).
        assertEquals(1722, shelfCoverHash("Corner Store Poems"))
        assertEquals(735, shelfCoverHash("Tuesday"))
        assertEquals(0, shelfCoverHash(""))
    }

    @Test
    fun `a seed title prints its frozen recipe`() {
        // h=1722: 1722%4=2 -> Tone/Stamp; 1722%3==0 -> accent ACCENT[(1722>>2)%4] = ACCENT[2] = Teal;
        // 1722%2==0 -> the default paper stock.
        assertEquals(
            ShelfCoverRecipe(ShelfArchetype.Tone, ShelfInk.Stamp, ShelfInk.Teal, usePaper2 = false),
            shelfCoverRecipe("Corner Store Poems"),
        )
        // h=735: 735%4=3 -> Split/Yellow; 735%3==0 -> ACCENT[(735>>2)%4] = ACCENT[3] = Stamp;
        // 735%2==1 -> the second paper stock.
        assertEquals(
            ShelfCoverRecipe(ShelfArchetype.Split, ShelfInk.Yellow, ShelfInk.Stamp, usePaper2 = true),
            shelfCoverRecipe("Tuesday"),
        )
    }

    @Test
    fun `a single-ink cover has no accent`() {
        // "Small Machines" = 1345, which is not a multiple of 3, so the spec leaves --ink2 unset.
        assertEquals(1345, shelfCoverHash("Small Machines"))
        assertNull(shelfCoverRecipe("Small Machines").accent)
    }

    @Test
    fun `archetype and dominant ink share one index, as the spec writes them`() {
        val titles = listOf("Overgrown", "Bus Window", "Paper Teeth", "Marginalia", "Analog Ghosts")
        for (title in titles) {
            val recipe = shelfCoverRecipe(title)
            assertEquals(
                "archetype/ink disagreed for \"$title\"",
                recipe.archetype.ordinal,
                shelfCoverHash(title) % 4,
            )
            assertEquals(recipe.archetype.ordinal, recipe.field.ordinal)
        }
    }

    @Test
    fun `the recipe is stable per title, so a zine keeps its look under any sort`() {
        assertEquals(shelfCoverRecipe("Notes on Rain"), shelfCoverRecipe("Notes on Rain"))
        // ...and is not merely a constant.
        assertNotEquals(shelfCoverRecipe("Notes on Rain"), shelfCoverRecipe("Tuesday"))
    }

    @Test
    fun `a title long enough to overflow the 32-bit sum still yields a valid recipe`() {
        // The spec's `|0` wraps to signed 32-bit and then takes Math.abs; a negative index would
        // crash the lookup, so the hash floors at zero.
        val huge = "￿".repeat(40_000)
        val h = shelfCoverHash(huge)
        assertEquals(true, h >= 0)
        shelfCoverRecipe(huge) // must not throw
    }
}
