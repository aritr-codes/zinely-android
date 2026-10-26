package com.aritr.zinely.core.copy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * A40 "Mix an ink / My inks / palettes" words, one example each, against `v21-ink-mixer.html` and bench
 * `#a40`/`#a40r2`. Written out in full on purpose: a typo in `Copy.kt` and its test would otherwise agree.
 */
class InkCopyTest {
    private val ink = Copy.BenchInk
    private val mix = Copy.InkMix
    private val word = Copy.InkName

    @Test fun `tray constants`() {
        assertEquals("My palettes", ink.MY_PALETTES)
        assertEquals("+ New palette", ink.NEW_PALETTE_ACTION)
        assertEquals("New palette", ink.NEW_PALETTE)
        assertEquals("My inks", ink.MY_INKS)
        assertEquals("Built-in inks", ink.BUILT_IN_INKS)
        assertEquals("Starter palettes", ink.STARTER_PALETTES)
        assertEquals("3 to copy", ink.STARTER_COUNT)
        assertEquals("Save a copy", ink.SAVE_A_COPY)
        assertEquals("Inks that go together, one tap away.", ink.PALETTES_EMPTY_LEAD)
        assertEquals(" Make one, or copy a starter below.", ink.PALETTES_EMPTY_REST)
        assertEquals("Your palettes", ink.YOUR_PALETTES)
        assertEquals(", selected", ink.SELECTED_SUFFIX)
        assertEquals("Mix", ink.MIX)
        assertEquals("Mix an ink", ink.MIX_AN_INK)
        assertEquals(ink.MIX_AN_INK, mix.TITLE)
    }

    @Test fun `tray functions`() {
        assertEquals("3 of 9", ink.paletteCount(3))
        assertEquals("7 of 9", ink.inkCount(7))
        assertEquals("More for Warm vintage", ink.moreFor("Warm vintage"))
        assertEquals("Warm vintage, 4 inks", ink.paletteGroup("Warm vintage", 4))
        assertEquals("Matcha, from Warm vintage", ink.palettePot("Matcha", "Warm vintage"))
        assertEquals("My ink, dark teal, 1 F 7 A 5 C", ink.myInkLabel("dark teal", "1 F 7 A 5 C"))
        assertEquals("dark teal, 1 F 7 A 5 C", ink.pickInkLabel("dark teal", "1 F 7 A 5 C"))
    }

    @Test fun `menus`() {
        assertEquals("Remove from My inks", ink.REMOVE_FROM_MY_INKS)
        assertEquals("Edit", ink.EDIT)
        assertEquals("Duplicate", ink.DUPLICATE)
        assertEquals("Delete", ink.DELETE)
    }

    @Test fun `edit mode`() {
        assertEquals("Edit palette", ink.EDIT_PALETTE)
        assertEquals("Palette name", ink.PALETTE_NAME)
        assertEquals("My palette", ink.DEFAULT_PALETTE_NAME)
        assertEquals(" copy", ink.COPY_SUFFIX)
        assertEquals("Inks in this palette", ink.INKS_IN_PALETTE)
        assertEquals("Tap inks below to add them", ink.TAP_INKS_TO_ADD)
        assertEquals("Was in this palette", ink.WAS_IN_PALETTE)
        assertEquals("Cancel", ink.CANCEL)
        assertEquals("Save palette", ink.SAVE_PALETTE)
        assertEquals("Remove matcha from palette", ink.removeFromPalette("matcha"))
        assertEquals("Give it a name, like \"Warm vintage\".", ink.NAME_EMPTY_HELP)
        assertEquals("You have another palette called this. That's fine.", ink.NAME_DUPLICATE_HELP)
    }

    @Test fun `pickedCount covers none, one, middle and the cap`() {
        assertEquals("Pick 2 to 6 inks. 0 picked.", ink.pickedCount(0))
        assertEquals("Pick 2 to 6 inks. 1 picked.", ink.pickedCount(1))
        assertEquals("2 picked. Up to 6.", ink.pickedCount(2))
        assertEquals("5 picked. Up to 6.", ink.pickedCount(5))
        assertEquals("6 picked \u2014 that's the most a palette holds.", ink.pickedCount(6))
    }

    @Test fun `snacks`() {
        assertEquals("Ink \u00b7 Matcha", ink.applied("Matcha"))
        assertEquals("Ink \u00b7 light cyan-grey. Pale in print: words may be hard to read.", ink.appliedPale("light cyan-grey"))
        assertEquals("Removed \u00b7 dark teal. Pages and palettes using it keep it.", ink.removedInk("dark teal"))
        assertEquals("My palettes holds 9. Delete one to make room.", ink.PALETTES_FULL)
        assertEquals("Duplicated \u00b7 Warm vintage", ink.duplicated("Warm vintage"))
        assertEquals("Deleted \u00b7 Warm vintage. Its inks stay.", ink.deleted("Warm vintage"))
        assertEquals("Saved to My palettes \u00b7 Warm zine", ink.savedCopy("Warm zine"))
        assertEquals("A palette holds 6. Remove one first.", ink.PALETTE_HOLDS_SIX)
        assertEquals("Palette not saved", ink.PALETTE_NOT_SAVED)
        assertEquals("Pick at least 2 inks.", ink.PICK_AT_LEAST_TWO)
        assertEquals("Saved \u00b7 Warm vintage", ink.saved("Warm vintage"))
        assertEquals("New palette \u00b7 Warm vintage", ink.newPalette("Warm vintage"))
        assertEquals("Mixed \u00b7 dark teal, added to the palette and My inks", ink.mixedAdded("dark teal"))
        assertEquals("Mixed \u00b7 dark teal, already in this palette", ink.mixedAlreadyIn("dark teal"))
        assertEquals("Mixed \u00b7 dark teal, kept in My inks (the palette is full)", ink.mixedKept("dark teal"))
    }

    @Test fun `mixer`() {
        assertEquals("Preview on white paper", mix.PREVIEW_LABEL)
        assertEquals("Reads clearly on white paper", mix.READS_CLEARLY)
        assertEquals("Pale in print.", mix.PALE_LEAD)
        assertEquals(" Words may be hard to read.", mix.PALE_TEXT)
        assertEquals(" Fine for a soft mark or a tint.", mix.PALE_ART)
        assertEquals("Hex colour", mix.HEX_LABEL)
        assertEquals("Use 0\u20139 and A\u2013F only. Still showing #1F7A5C.", mix.badHex("1F7A5C"))
        assertEquals("Keep typing, or tap Done for a 3-letter code. Still showing #1F7A5C.", mix.keepTyping("1F7A5C"))
        assertEquals("Colour strength", mix.COLOUR_STRENGTH)
        assertEquals("Lightness", mix.LIGHTNESS)
        assertEquals("Hue", mix.HUE)
        assertEquals("dark teal, 62% colour", mix.strengthValue("dark teal", 62))
        assertEquals("dark teal, 48% light", mix.lightValue("dark teal", 48))
        assertEquals("Greyer \u2190 \u2192 More colour", mix.SQUARE_HINT_X)
        assertEquals("\u2191 Lighter \u00b7 \u2193 Darker", mix.SQUARE_HINT_Y)
        assertEquals("Use this ink", mix.USE_THIS_INK)
        assertEquals("Add to palette", mix.ADD_TO_PALETTE)
        assertEquals("Save to My inks", mix.SAVE_TO_MY_INKS)
        assertEquals(
            "My inks is full. This replaces dark teal, used longest ago. Your pages keep it.",
            mix.fullNote("dark teal"),
        )
        assertEquals("Last stop", mix.PREVIEW_SAMPLE)
    }

    @Test fun `colour words`() {
        assertEquals(listOf("near-black", "white", "dark", "light", "grey"), listOf(word.NEAR_BLACK, word.WHITE, word.DARK, word.LIGHT, word.GREY))
        assertEquals(
            listOf("red", "orange", "yellow", "yellow-green", "green", "teal", "cyan", "blue", "violet", "purple", "magenta", "pink"),
            listOf(word.RED, word.ORANGE, word.YELLOW, word.YELLOW_GREEN, word.GREEN, word.TEAL, word.CYAN, word.BLUE, word.VIOLET, word.PURPLE, word.MAGENTA, word.PINK),
        )
        assertEquals("brown", word.BROWN)
        assertEquals("beige", word.BEIGE)
        assertEquals("dark teal", word.toned(word.DARK, "teal"))
        assertEquals("teal", word.toned(null, "teal"))
        assertEquals("brown-grey", word.tintedGrey("brown"))
    }
}
