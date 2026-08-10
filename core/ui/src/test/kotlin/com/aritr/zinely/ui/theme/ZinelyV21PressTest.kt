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
    fun `every tier holds the literals transcribed from the corpus`() {
        // All four pinned, not just Hero. A review found the arithmetic test below passing for
        // Raised(5,3,2) — self-consistency is not measurement, and three quarters of this class was
        // guarding nothing. These twelve numbers are the transcription; everything else is derived.
        assertEquals(Triple(4.dp, 2.dp, 1.dp), ZinelyV21Press.Hero.triple())
        assertEquals(Triple(3.dp, 2.dp, 1.dp), ZinelyV21Press.Raised.triple())
        assertEquals(Triple(2.dp, 2.dp, 0.dp), ZinelyV21Press.Flat.triple())
        assertEquals(Triple(2.dp, 1.dp, 1.dp), ZinelyV21Press.Inline.triple())
    }

    private fun ZinelyV21Press.triple() = Triple(rest, travel, pressed)

    @Test
    fun `Hero does not follow pressed equals rest minus travel, and the other three do`() {
        // Recorded as a fact about the numbers, NOT as intent. An earlier version of this test called
        // Hero a deliberate expressive choice; the corpus does not support that reading — every Hero
        // and Raised :active rule writes the identical `translate(2px,2px); box-shadow:1px 1px 0`,
        // which shows one reused pressed value, not a per-tier decision.
        val hero = ZinelyV21Press.Hero
        assertNotEquals(hero.rest - hero.travel, hero.pressed)
        for (tier in listOf(ZinelyV21Press.Raised, ZinelyV21Press.Flat, ZinelyV21Press.Inline)) {
            assertEquals("$tier", tier.rest - tier.travel, tier.pressed)
        }
    }

    @Test
    fun `every non-Flat tier presses to the same 1dp, whatever it rests at`() {
        // The actual pattern in the corpus, stated as the assertion. Hero rests 4 and Raised rests 3;
        // both land on 1. This is what the numbers show and all they show.
        for (tier in listOf(ZinelyV21Press.Hero, ZinelyV21Press.Raised, ZinelyV21Press.Inline)) {
            assertEquals("$tier", 1.dp, tier.pressed)
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
        //
        // Asked by reflection, not by listing the four constants: a review pointed out that the
        // previous version asserted four named constants were four distinct constants, which cannot
        // fail when a fifth is added. This one can.
        //
        // Java reflection rather than `::class.members` — kotlin-reflect is not on the unit-test
        // classpath, and adding a dependency so one assertion can read prettier is not a trade worth
        // making. The getters are `getHero()`-shaped, so the name is recovered from the accessor.
        val declared = ZinelyV21Press.Companion::class.java.declaredMethods
            .filter { it.returnType == ZinelyV21Press::class.java && it.parameterCount == 0 }
            .map { it.name.removePrefix("get") }
        assertEquals("tiers are a transcription, not a ladder to extend: $declared", 4, declared.size)
        assertEquals(setOf("Hero", "Raised", "Flat", "Inline"), declared.toSet())
    }
}
