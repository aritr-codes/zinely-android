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
            "tape.masking", "fix.stitch", "fix.grommet", "fix.pushpin",
            "mark.asterisk", "mark.arrow", "mark.halftone", "mark.registration",
            "mark.hand", "mark.crop", "mark.bar", "mark.scan", "mark.perf", "mark.burst",
            "paper.strip", "paper.window", "paper.tag", "paper.underline",
            "paper.stub", "paper.stamp", "paper.deckle", "paper.hole", "paper.dogear",
            "paper.border", "paper.round",
            "shape.rect", "shape.circle", "shape.triangle", "shape.rule",
            "shape.ring",
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
            Copy.Type.ROW_SIZE to "text card row", Copy.BenchVerbs.FONT to "bench verb",
            Copy.BenchVerbs.REFRAME to "bench verb", Copy.BenchVerbs.COPIER to "bench verb",
            Copy.AddChooser.TEXT_TITLE to "Add row and bench verb", Copy.AddChooser.PHOTO_TITLE to "Add row",
        )
    }

    @Test
    fun `the first expanded cabinet is present, named, and mutually distinct`() {
        val names = Copy.Supplies.NAMES.values.toList()
        assertEquals(
            Copy.Supplies.BY_FAMILY.values.sumOf { it.size }, Copy.Supplies.NAMES.size,
            "a supply id is filed under exactly one family",
        )
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
        // Ids are add-only (ADR-139): every shipped id is still here, and A39's new ids sit after their
        // subject's holder, so the shipped ids keep §4's relative order.
        assertEquals(SPEC_IDS, ids.filter { it in SPEC_IDS }, "The shipped ids are not SUPPLIES-SPEC §4's, in §4's order")
    }

    @Test
    fun `the four families stay stable while their curated counts grow`() {
        assertEquals(
            listOf(
                Copy.Supplies.TAPE_AND_FIXINGS,
                Copy.Supplies.STAMPS_AND_MARKS,
                Copy.Supplies.CUT_PAPER,
                Copy.Supplies.CUT_SHAPES,
            ),
            Copy.Supplies.BY_FAMILY.keys.toList(),
            "SUPPLIES-SPEC §4 freezes four families (v21-bench.html:831)",
        )
        // v21-bench.html A39's 124 (ADR-139): 24, 38, 44 and 18.
        assertEquals(listOf(24, 38, 44, 18), Copy.Supplies.BY_FAMILY.values.map { it.size })
    }

    @Test
    fun `search is local deterministic and matches names plus curated tags`() {
        assertEquals(
            listOf(
                "mark.asterisk", "mark.asteriskb", "mark.asteriskc",
                "mark.burst", "mark.burstb", "mark.burstc", "mark.burstd", "mark.sparkle",
            ),
            Copy.Supplies.matchingIds("star"),
        )
        assertEquals(listOf("mark.hand", "mark.handb", "mark.handc"), Copy.Supplies.matchingIds("manicule"))
        assertEquals(
            listOf("tape.torn", "tape.tornb", "tape.masking", "tape.maskingb", "tape.plaster"),
            Copy.Supplies.matchingIds("tape", Copy.Supplies.TAPE_AND_FIXINGS),
        )
        assertTrue(Copy.Supplies.matchingIds("not in this cabinet").isEmpty())
        // A rule's variants are found with it (A39); a search for "rule" finds the three dividers and nothing else.
        assertEquals(listOf("shape.rule", "shape.ruleb", "shape.rulec"), Copy.Supplies.matchingIds("rule"))
        // The two frames (v21-bench.html A32) and their A39 variants are found by name and by tag, in the
        // cabinet's own order.
        assertEquals(listOf("paper.border", "paper.borderb", "paper.borderc"), Copy.Supplies.matchingIds("snipped"))
        assertEquals(listOf("paper.round", "paper.roundb", "paper.roundc"), Copy.Supplies.matchingIds("porthole"))
        assertEquals(
            listOf(
                "paper.window", "paper.windowb", "paper.windowc", "paper.cutwindow", "paper.holed",
                "paper.border", "paper.borderb", "paper.borderc", "paper.borderd",
                "paper.round", "paper.roundb", "paper.roundc",
            ),
            Copy.Supplies.matchingIds("frame"),
        )
    }

    @Test
    fun `the two frames keep their ruled order after Folded corner`() {
        // A32 closed Cut paper with them; A39 (ADR-139) adds variants and new subjects around them, so the
        // ruling that survives is their order among the shipped subjects.
        val cutPaper = Copy.Supplies.BY_FAMILY.getValue(Copy.Supplies.CUT_PAPER)
        val ruled = listOf("paper.dogear" to "Folded corner", "paper.border" to "Snipped border", "paper.round" to "Round window")
        assertEquals(ruled, cutPaper.entries.filter { it.key in ruled.toMap() }.map { it.key to it.value })
    }

    @Test
    fun `every supply has searchable tags`() {
        assertEquals(Copy.Supplies.NAMES.keys, Copy.Supplies.TAGS.keys)
    }

    @Test
    fun `a piece renamed to fit its tile is still found by every word of its old name`() {
        // Given the 48 A39 names as frozen on 2026-10-22, before D-132 shortened them to fit the A16 tile
        val oldNames = mapOf(
            "tape.tornb" to "Torn tape, long tear",
            "fix.cornerb" to "Photo corner, sharp",
            "fix.clipb" to "Paper clip, narrow",
            "fix.clipc" to "Paper clip, slanted wire",
            "tape.maskingb" to "Masking tape, wide",
            "fix.stitchb" to "Saddle stitch, thread",
            "fix.stitchc" to "Saddle stitch, across fold",
            "fix.pushpinc" to "Push pin, upright",
            "fix.pushpind" to "Push pin, drawn",
            "fix.pushpine" to "Push pin, drawn tilted",
            "fix.needle" to "Needle and thread",
            "mark.halftoneb" to "Halftone dots, screen",
            "mark.registration" to "Registration cross",
            "mark.registrationb" to "Registration cross, bold",
            "mark.registrationc" to "Registration cross, fine",
            "mark.handb" to "Pointing hand, cuffed",
            "mark.handc" to "Pointing hand, drawn",
            "mark.cropb" to "Crop marks, outside trim",
            "mark.cropc" to "Crop marks, long ticks",
            "mark.barb" to "Colour bar, tone patches",
            "mark.scanb" to "Copier streak, toner",
            "mark.scanc" to "Copier streak, specks",
            "mark.perf" to "Perforation",
            "mark.perfb" to "Perforation, slits",
            "mark.perfc" to "Perforation, fine",
            "paper.stripb" to "Torn strip, wavy",
            "paper.windowc" to "Window frame, narrow",
            "paper.chat" to "Speech balloon (square)",
            "paper.underlineb" to "Marker underline, round",
            "paper.stubb" to "Ticket stub, torn end",
            "paper.stubc" to "Ticket stub, torn off",
            "paper.ticket" to "Admission ticket",
            "paper.stampb" to "Postage stamp, bites",
            "paper.stampc" to "Postage stamp, corner bites",
            "paper.deckleb" to "Deckle edge, rough",
            "paper.holeb" to "Torn hole, ragged",
            "paper.holec" to "Torn hole, off-round",
            "paper.dogearb" to "Folded corner, flap",
            "paper.dogearc" to "Folded corner, portrait",
            "paper.page" to "Dog-eared page",
            "paper.borderb" to "Snipped border, band",
            "paper.borderc" to "Snipped border, fine snips",
            "paper.roundb" to "Round window, smooth",
            "paper.roundc" to "Round window, narrow",
            "paper.balloon" to "Speech balloon (round)",
            "paper.ribbon" to "Bookmark ribbon (brush)",
            "shape.rectb" to "Rectangle, landscape",
            "shape.cloudb" to "Three-lobe cloud",
        )
        // When each old word is searched; Then the renamed piece is among the results
        val lost = oldNames.flatMap { (id, old) ->
            Regex("[a-z][a-z-]*").findAll(old.lowercase()).map { it.value }.filter { it != "and" }
                .filterNot { id in Copy.Supplies.matchingIds(it) }.map { "$id: $it" }.toList()
        }
        assertEquals(48, oldNames.size)
        assertTrue(oldNames.all { (id, old) -> Copy.Supplies.NAMES.getValue(id) != old }, "D-132 renamed all 48")
        assertTrue(lost.isEmpty(), "old words no longer found: $lost")
    }
}
