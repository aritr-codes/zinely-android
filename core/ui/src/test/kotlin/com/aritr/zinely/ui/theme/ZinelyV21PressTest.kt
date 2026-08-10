package com.aritr.zinely.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the four press tiers against the frozen corpus.
 *
 * These are counts of `:active` rules in `v21-*.html`, not derived values, so the failure this class
 * guards is the one that already happened once: a **plausible formula quietly replacing a measurement**.
 * V21-SPEC states the hero case as though it were the rule, and a first draft of [ZinelyV21Dimens]
 * published it as `pressTravel` / `hardShadowPressed` for everything. Three quarters of the corpus
 * disagrees.
 */
class ZinelyV21PressTest {

    @Test
    fun `the hero tier is the corpus figure, not the arithmetic one`() {
        // The one that matters. `pressed = rest - travel` would give 2dp here; the corpus says 1dp,
        // so the screen's single primary action sheds more shadow than it travels and reads as going
        // further down. If this ever equals rest - travel, a formula has eaten the design.
        val hero = ZinelyV21Press.Hero
        assertEquals(4.dp, hero.rest)
        assertEquals(2.dp, hero.travel)
        assertEquals(1.dp, hero.pressed)
        assertNotEquals("Hero is the deliberate exception to pressed = rest - travel", hero.rest - hero.travel, hero.pressed)
    }

    @Test
    fun `the other three tiers do follow pressed equals rest minus travel`() {
        for (tier in listOf(ZinelyV21Press.Raised, ZinelyV21Press.Flat, ZinelyV21Press.Inline)) {
            assertEquals("$tier", tier.rest - tier.travel, tier.pressed)
        }
    }

    @Test
    fun `Flat presses flush to the surface`() {
        // Chips, tiles and icon buttons lose their shadow entirely while held. Zero is a state here,
        // and zinelyV21HardShadow draws nothing for it — that is the design, not a skipped draw.
        assertEquals(0.dp, ZinelyV21Press.Flat.pressed)
    }

    @Test
    fun `Inline halves the travel because it sits inside another surface`() {
        assertEquals(1.dp, ZinelyV21Press.Inline.travel)
        assertEquals(ZinelyV21Press.Flat.rest, ZinelyV21Press.Inline.rest)
    }

    @Test
    fun `only the hero tier uses the tokenised offset`() {
        // --hard is the one depth the corpus names; 3 and 2 are literals at their use sites.
        assertEquals(ZinelyV21Dimens.hardShadow, ZinelyV21Press.Hero.rest)
        for (tier in listOf(ZinelyV21Press.Raised, ZinelyV21Press.Flat, ZinelyV21Press.Inline)) {
            assertNotEquals(ZinelyV21Dimens.hardShadow, tier.rest)
        }
    }

    @Test
    fun `every tier is shallower pressed than at rest, and travels toward the surface`() {
        for (tier in listOf(ZinelyV21Press.Hero, ZinelyV21Press.Raised, ZinelyV21Press.Flat, ZinelyV21Press.Inline)) {
            assertTrue("$tier must shed shadow when pressed", tier.pressed < tier.rest)
            assertTrue("$tier must travel", tier.travel > 0.dp)
            assertEquals(tier.rest, tier.offset(isPressed = false))
            assertEquals(tier.pressed, tier.offset(isPressed = true))
        }
    }

    @Test
    fun `there are exactly four tiers`() {
        // A fifth would have no corpus behind it. Interpolating one is the fabrication D-006/D-007
        // both refused; if a surface needs a depth that is not here, the HTML states it first.
        val tiers = setOf(
            ZinelyV21Press.Hero,
            ZinelyV21Press.Raised,
            ZinelyV21Press.Flat,
            ZinelyV21Press.Inline,
        )
        assertEquals(4, tiers.size)
    }
}
