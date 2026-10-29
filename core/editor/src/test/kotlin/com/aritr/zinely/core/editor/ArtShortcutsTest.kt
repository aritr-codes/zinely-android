package com.aritr.zinely.core.editor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArtShortcutsTest {

    @Test
    fun given_a_seventh_pick_then_oldest_recent_drops_and_a_repeat_moves_to_front() {
        var s = ArtShortcuts()
        for (i in 1..7) s = s.picked("p$i")
        assertEquals(listOf("p7", "p6", "p5", "p4", "p3", "p2"), s.recent)

        assertEquals(listOf("p3", "p7", "p6", "p5", "p4", "p2"), s.picked("p3").recent)
    }

    @Test
    fun given_favourites_beyond_the_cap_then_oldest_is_dropped_and_unfavourite_removes() {
        var s = ArtShortcuts()
        for (i in 1..(ART_FAVOURITES_CAP + 1)) s = s.favourite("p$i", true)
        assertEquals(ART_FAVOURITES_CAP, s.favourites.size)
        assertEquals("p2", s.favourites.first())

        assertEquals(false, "p3" in s.favourite("p3", false).favourites)
    }

    @Test
    fun given_unknown_blank_oversize_and_duplicate_ids_when_sanitized_then_dropped() {
        val s = ArtShortcuts(
            recent = listOf("a", "gone", "", "a", "x".repeat(65)),
            favourites = listOf("gone", "b", "b"),
        ).sanitized(known = setOf("a", "b"))

        assertEquals(listOf("a"), s.recent)
        assertEquals(listOf("b"), s.favourites)
    }
}
