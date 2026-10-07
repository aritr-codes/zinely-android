package com.aritr.zinely.core.data.repository

import java.text.Normalizer
import java.time.Duration
import java.util.Locale
import java.util.Random
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** What a Shelf folder's name is (ADR-125 rule 13). */
class FolderNamesTest {

    // ---- clean: what is a name at all ------------------------------------------------------------

    @Test
    fun `nothing, blank and invisible-only names are no folder`() {
        assertNull(FolderNames.clean(null))
        assertNull(FolderNames.clean(""))
        assertNull(FolderNames.clean("   "))
        assertNull(FolderNames.clean("\n\t\r"))
        assertNull(FolderNames.clean("  ")) // no-break space, em space
        assertNull(FolderNames.clean("​‍﻿")) // zero-width characters only
    }

    @Test
    fun `My Shelf is never a folder, typed or read from a file`() {
        assertNull(FolderNames.clean("My Shelf"))
        assertNull(FolderNames.clean("  my shelf "))
        assertNull(FolderNames.clean("MY SHELF"))
        assertNull(FolderNames.clean("My\n Shelf")) // the line break is removed first
        assertEquals("My Shelf 2", FolderNames.clean("My Shelf 2"))
        assertEquals("My  Shelf", FolderNames.clean("My  Shelf")) // inner spaces are the maker's own
    }

    @Test
    fun `a name is trimmed and loses control characters and line breaks`() {
        assertEquals("Trips", FolderNames.clean("  Trips\n"))
        assertEquals("Trips", FolderNames.clean("Tr\u0000ip\u0007s"))
        assertEquals("Field notes", FolderNames.clean("Field  notes "))
        assertEquals("ab", FolderNames.clean("a\uD83Db")) // an unpaired surrogate cannot be shown
    }

    @Test
    fun `a name is stored composed, so two ways of typing one letter are one name`() {
        assertEquals("Café", FolderNames.clean("Café"))
        assertEquals(FolderNames.clean("Café"), FolderNames.clean("Café"))
    }

    // ---- clean: the cut at 40 --------------------------------------------------------------------

    @Test
    fun `a name is cut at forty characters`() {
        assertEquals("a".repeat(40), FolderNames.clean("a".repeat(40)))
        assertEquals("a".repeat(40), FolderNames.clean("a".repeat(41)))
        assertEquals("a".repeat(39), FolderNames.clean("a".repeat(39) + " bbb")) // no space left at the end
    }

    @Test
    fun `the cut never splits a character a reader sees as one`() {
        val lead = "a".repeat(39)
        val family = "👩‍👧‍👦" // woman, girl, boy joined
        val thumb = "👍🏽" // thumbs up with a skin tone
        val india = "🇮🇳"
        val france = "🇫🇷"
        val keycap = "1️⃣"

        assertEquals(lead + family, FolderNames.clean(lead + family + "b"))
        assertEquals(lead + thumb, FolderNames.clean(lead + thumb + "b"))
        assertEquals(lead + india, FolderNames.clean(lead + india + france))
        assertEquals(lead + keycap, FolderNames.clean(lead + keycap + "b"))
        // A mark with no composed form stays with its letter.
        assertEquals(lead + "q́", FolderNames.clean(lead + "q́" + "b"))
        // Forty emoji are forty characters, however many code units they take.
        assertEquals(thumb.repeat(40), FolderNames.clean(thumb.repeat(45)))
        assertEquals(india.repeat(40), FolderNames.clean(india.repeat(41)))
    }

    @Test
    fun `consonants joined by a virama, and a letter with its non-joiner, are not cut apart`() {
        val lead = "a".repeat(39)
        val ksha = "\u0915\u094D\u0937" // Devanagari k + virama + ss, drawn as one
        val kshaJoined = "\u0915\u094D\u200D\u0937"
        val meemNonJoiner = "\u0645\u200C"

        assertEquals(lead + ksha, FolderNames.clean(lead + ksha + "b"))
        assertEquals(lead + kshaJoined, FolderNames.clean(lead + kshaJoined + "b"))
        assertEquals(lead + meemNonJoiner, FolderNames.clean(lead + meemNonJoiner + "b"))
    }

    @Test
    fun `no name is longer than the Shelf can show, however it is built`() {
        // Letters glued by joiners are forty letters, not one endless character.
        val glued = FolderNames.clean("x\u200D".repeat(500))!!
        assertEquals("x\u200D".repeat(40), glued)
        // One letter under a thousand marks is left out whole: it is never cut through.
        assertNull(FolderNames.clean("a" + "\u0301".repeat(5000)))
        assertEquals("b", FolderNames.clean("b" + "q" + "\u0301".repeat(5000)))
        // Forty of the longest ordinary emoji stop at the unit limit, on a boundary.
        val family = "\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66"
        assertEquals(family.repeat(20), FolderNames.clean(family.repeat(40)))
        assertEquals(FolderNames.MAX_UNITS, family.repeat(20).length)
    }

    @Test
    fun `a huge value from a file is cleaned quickly, and a pair split by the reading limit is dropped`() {
        val hostile = "\u0301\u0316".repeat(2_000_000)
        assertTimeoutPreemptively(Duration.ofSeconds(5)) { FolderNames.clean("Trips$hostile") }
        // 1023 removed characters, then an emoji whose second half is past the limit: the half is not kept.
        assertNull(FolderNames.clean("\u200B".repeat(1023) + "\uD83D\uDC4D"))
        assertEquals("\uD83D\uDC4D", FolderNames.clean("\u200B".repeat(1022) + "\uD83D\uDC4D"))
    }

    @Test
    fun `the commonest invisible characters are removed, and none of them can smuggle in My Shelf`() {
        assertEquals("Trips", FolderNames.clean("Tri\u200Bps\u00AD\uFEFF"))
        assertEquals("Trips", FolderNames.clean("\u202ETrips\u2060"))
        assertNull(FolderNames.clean("My Shelf\u200B"))
        assertNull(FolderNames.clean("My Shelf\u200D"))
        assertNull(FolderNames.clean("My\u200C Shelf\uFE0F"))
        assertNull(FolderNames.clean("My\u00A0Shelf"))
        assertNull(FolderNames.clean("MY\u2003SHELF\u034F"))
        listOf("My Shelf\u3164", "My Shelf\uFFA0", "My\u2800Shelf", "My Shelf\u2065", "My Shelf\uDB40\uDC80")
            .forEach { assertNull(FolderNames.clean(it), it.map { c -> c.code.toString(16) }.toString()) }
        // Hidden characters dropped anywhere in it, in any number, never get it through.
        val hidden = intArrayOf(0x200B, 0x200C, 0x200D, 0xFE0F, 0x034F, 0x00AD, 0x2065, 0xE0080, 0xE0061, 0x202E)
        val random = Random(9)
        repeat(20_000) {
            val name = buildString {
                for (c in if (random.nextBoolean()) "My Shelf" else "MY SHELF") {
                    repeat(random.nextInt(3)) { appendCodePoint(hidden[random.nextInt(hidden.size)]) }
                    append(c)
                }
                repeat(random.nextInt(3)) { appendCodePoint(hidden[random.nextInt(hidden.size)]) }
            }
            assertNull(FolderNames.clean(name), name.map { c -> c.code.toString(16) }.toString())
        }
        // Known and left: a stray joiner inside any other name still makes a different folder.
        assertFalse(FolderNames.same(FolderNames.clean("Trips\u200D"), "Trips"))
        assertTrue(FolderNames.same(FolderNames.clean("Trips\u200B"), "trips"))
    }

    @Test
    fun `a name of marks or blank-looking characters alone is no folder`() {
        listOf("\u3164", "\u2800", "\uFE0F", "\u034F", "\u0301", "\u115F\u1160", "\u200D\u200C")
            .forEach { assertNull(FolderNames.clean(it), it.map { c -> c.code.toString(16) }.toString()) }
        assertEquals("\u2764\uFE0F", FolderNames.clean("\u2764\uFE0F")) // a heart is something to see
        // An emoji newer than this machine's tables is still a name: someone typed it, and a newer phone draws it.
        (0x1FA00..0x1FAFF).firstOrNull { Character.getType(it).toByte() == Character.UNASSIGNED }?.let { newer ->
            val unknown = String(Character.toChars(newer))
            assertEquals(unknown, FolderNames.clean(unknown))
        }
        // A code point Unicode reserves as ignorable draws nothing, assigned or not.
        assertNull(FolderNames.clean(String(Character.toChars(0xE0080))))
        assertNull(FolderNames.clean("\u2065"))
        // Tamil's mark does not join, so its letters are counted one by one.
        assertEquals("\u0B95\u0BCD".repeat(40), FolderNames.clean("\u0B95\u0BCD".repeat(80)))
    }

    /** Rule 13's invariant, over names built from the characters that have broken it before. */
    @Test
    fun `every cleaned name is bounded, composed, trimmed and unchanged by cleaning again`() {
        val pool = intArrayOf(
            'a'.code, 'B'.code, ' '.code, '1'.code, 0x00A0, 0x0301, 0x0316, 0x00E9, 0x0131, 0x0130, 0x200B, 0x200C, 0x200D,
            0xFE0F, 0x20E3, 0x1F44D, 0x1F3FD, 0x1F1EE, 0x1F1F3, 0x1F469, 0xE0067, 0xE007F, 0x0915, 0x094D, 0x0937,
            0x1100, 0x119E, 0x0600, 0x3164, 0x2800, 0x0009, 0x000A, 0x2028, 0xD83D, 0xFF9E, 0x00AD, 0x03A3, 0x00DF,
        )
        val random = Random(125)
        repeat(200_000) {
            val raw = buildString { repeat(random.nextInt(120)) { appendCodePoint(pool[random.nextInt(pool.size)]) } }
            val once = FolderNames.clean(raw) ?: return@repeat
            val shown = raw.map { c -> c.code.toString(16) }.toString()
            assertEquals(once, FolderNames.clean(once), shown)
            assertTrue(once.length <= FolderNames.MAX_UNITS, shown)
            assertEquals(Normalizer.normalize(once, Normalizer.Form.NFC), once, shown)
            assertEquals(once.trim(), once, shown)
            assertFalse(FolderNames.key(once.replace('\u00A0', ' ')) == "my shelf", shown)
        }
    }

    @Test
    fun `cleaning a cleaned name changes nothing`() {
        listOf(
            "  Trips\n",
            "Café",
            "a".repeat(39) + " bbb",
            "a".repeat(39) + "👩‍👧‍👦b",
            "a".repeat(39) + "🇮🇳🇫🇷",
            "👍🏽".repeat(45),
            "My Shelf 2",
        ).forEach { raw ->
            val once = FolderNames.clean(raw)
            assertEquals(once, FolderNames.clean(once), raw)
        }
    }

    // ---- key, same, display ----------------------------------------------------------------------

    @Test
    fun `capitals and composition do not make a different folder`() {
        assertTrue(FolderNames.same("Trips", "TRIPS"))
        assertTrue(FolderNames.same("Café", "CAFÉ"))
        assertTrue(FolderNames.same(null, null))
        assertFalse(FolderNames.same(null, "Trips"))
        assertFalse(FolderNames.same("Trips", "Trip"))
    }

    @Test
    fun `a phone set to Turkish groups the same zines as any other`() {
        val before = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            // Turkish lower-cases I to a dotless i; the folder rule must not.
            assertEquals("diary", FolderNames.key("DIARY"))
            assertTrue(FolderNames.same("DIARY", "diary"))
            assertNull(FolderNames.clean("MY SHELF"))
            // A dotless i is its own letter everywhere.
            assertFalse(FolderNames.same("dıary", "diary"))
        } finally {
            Locale.setDefault(before)
        }
    }

    @Test
    fun `a folder whose zines disagree shows the spelling that sorts first`() {
        assertEquals("Trips", FolderNames.display(listOf("trips", "Trips", "tRIPS")))
        assertEquals("TRIPS", FolderNames.display(listOf("Trips", "TRIPS")))
        assertNull(FolderNames.display(emptyList()))
        // By code point, not by UTF-16 unit: U+FF41 sorts before U+1F44D.
        assertEquals("ａ", FolderNames.display(listOf("👍", "ａ")))
    }

    // ---- reading a name from a file --------------------------------------------------------------

    @Test
    fun `a folder value that is not text is no folder, never an error`() {
        fun read(json: String) = Json.decodeFromString(LenientFolderNameSerializer, json)

        assertEquals("Trips", read("\"Trips\""))
        assertNull(read("null"))
        assertNull(read("7"))
        assertNull(read("true"))
        assertNull(read("[\"Trips\"]"))
        assertNull(read("{\"name\":\"Trips\"}"))
    }

    @Test
    fun `a folder change is complete only when nothing failed`() {
        assertTrue(FolderChange("Trips", listOf("a")).complete)
        assertFalse(FolderChange("Trips", listOf("a"), failedIds = listOf("b")).complete)
    }
}
