package com.aritr.zinely.core.copy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Pins [Copy.Supplies] against [SUPPLIES-SPEC §4/§6/§8](../../../../../../../docs/design/SUPPLIES-SPEC.md).
 *
 * Four things are checkable here and nothing else is: the vocabulary is complete and non-colliding, the
 * ids are well-formed, and the families are the four the spec names. What a supply *looks like* is S5's
 * problem and cannot be asserted from a string.
 */
class SuppliesCopyTest {

    private companion object {
        /** §2.2's validator rule, restated where the ids are authored so the two cannot drift. */
        val SUPPLY_ID = Regex("^[a-z]+\\.[a-z]+$")

        /** §4's `supplyId` list, verbatim, in frozen order. */
        val SPEC_IDS = listOf(
            "tape.torn", "fix.corner", "fix.staple", "fix.clip",
            "mark.asterisk", "mark.arrow", "mark.halftone", "mark.registration",
            "paper.strip", "paper.window", "paper.tag", "paper.underline",
            "shape.rect", "shape.circle", "shape.triangle", "shape.rule",
        )

        /**
         * Every user-visible string that already means something else and that a supply name must not
         * become — the 19 swatch names (`BenchInkPopover.benchInkName`, all three bands) and the verbs on
         * the bar the decor context bar itself will use.
         *
         * `Ink` is in here twice over, which is §8's whole warning: it names a maker ink, a neutral, and
         * the verb that opens the popover. A supply called `Ink` would announce identically to all three.
         */
        val TAKEN: Map<String, String> = mapOf(
            Copy.BenchInk.MATCHA to "swatch", Copy.BenchInk.FOREST to "swatch",
            Copy.BenchInk.STRAWBERRY to "swatch", Copy.BenchInk.BRICK to "swatch",
            Copy.BenchInk.SUNFLOWER to "swatch", Copy.BenchInk.OCHRE to "swatch",
            Copy.BenchInk.AQUA to "swatch", Copy.BenchInk.CORNFLOWER to "swatch",
            Copy.BenchInk.PLUM to "swatch", Copy.BenchInk.INK to "swatch",
            Copy.BenchInk.CREAM to "swatch", Copy.BenchInk.BLUSH to "swatch",
            Copy.BenchInk.SKY to "swatch", Copy.BenchInk.SAGE to "swatch",
            Copy.BenchInk.KRAFT to "swatch", Copy.BenchInk.SLATE to "swatch",
            Copy.BenchInk.STONE to "swatch", Copy.BenchInk.FOG to "swatch",
            Copy.BenchVerbs.INK to "bench verb", Copy.BenchVerbs.REPLACE to "bench verb",
            Copy.BenchVerbs.DELETE to "bench verb", Copy.BenchVerbs.EDIT to "bench verb",
            Copy.BenchVerbs.SIZE to "bench verb", Copy.BenchVerbs.FONT to "bench verb",
            Copy.BenchVerbs.REFRAME to "bench verb", Copy.BenchVerbs.COPIER to "bench verb",
            Copy.AddChooser.TEXT_TITLE to "Add row", Copy.AddChooser.PHOTO_TITLE to "Add row",
        )
    }

    @Test
    fun `the sixteen supplies are present, named, and mutually distinct`() {
        val names = Copy.Supplies.NAMES.values.toList()
        assertEquals(16, Copy.Supplies.NAMES.size, "SUPPLIES-SPEC §4 names sixteen supplies")
        names.forEach { assertTrue(it.isNotBlank(), "A supply with no name cannot be spoken or drawn") }
        assertEquals(
            names.size, names.toSet().size,
            "Two supplies share a name — TalkBack cannot tell them apart: " +
                names.groupBy { it }.filterValues { it.size > 1 }.keys,
        )
    }

    @Test
    fun `no supply name collides with a string that already means something else`() {
        // §8: `Ink` is the collision the spec warns about, and the resolution is that no supply takes it.
        val collisions = Copy.Supplies.NAMES
            .filterValues { it in TAKEN }
            .map { (id, name) -> "$id is called \"$name\", which is already a ${TAKEN[name]}" }
        assertTrue(collisions.isEmpty(), "Supply names collide with shipped copy (SUPPLIES-SPEC §8): $collisions")

        // The named half of the same assertion, so a future rename to "Ink" fails here by name, not by
        // set membership — this is the sentence a reader of the failure needs.
        assertTrue(
            Copy.Supplies.NAMES.values.none { it == Copy.BenchInk.INK },
            "No supply may be called \"${Copy.BenchInk.INK}\" — that word already names a maker ink, a " +
                "neutral, and the context-bar verb (SUPPLIES-SPEC §8)",
        )
    }

    @Test
    fun `every supply id is well-formed, distinct, and exactly the set the spec lists`() {
        val ids = Copy.Supplies.NAMES.keys.toList()
        ids.forEach {
            assertTrue(SUPPLY_ID.matches(it), "supplyId \"$it\" does not match ${SUPPLY_ID.pattern} (§2.2)")
        }
        assertEquals(ids.size, ids.toSet().size, "Duplicate supplyId: ${ids.groupBy { it }.filterValues { g -> g.size > 1 }.keys}")
        assertEquals(SPEC_IDS, ids, "The shipped ids are not SUPPLIES-SPEC §4's, in §4's order")
    }

    @Test
    fun `the four families are the spec's four, with four supplies each`() {
        assertEquals(
            listOf(
                Copy.Supplies.TAPE_AND_FIXINGS,
                Copy.Supplies.STAMPS_AND_MARKS,
                Copy.Supplies.CUT_PAPER,
                Copy.Supplies.CUT_SHAPES,
            ),
            Copy.Supplies.BY_FAMILY.keys.toList(),
            "SUPPLIES-SPEC §4 freezes four families (v21-bench.html:819)",
        )
        Copy.Supplies.BY_FAMILY.forEach { (family, supplies) ->
            assertEquals(4, supplies.size, "§4's table gives $family exactly four supplies")
        }
    }
}
