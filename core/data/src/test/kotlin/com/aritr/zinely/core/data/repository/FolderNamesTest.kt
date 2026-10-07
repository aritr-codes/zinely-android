package com.aritr.zinely.core.data.repository

import java.util.Locale
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
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
