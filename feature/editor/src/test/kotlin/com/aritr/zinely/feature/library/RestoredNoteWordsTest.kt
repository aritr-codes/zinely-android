package com.aritr.zinely.feature.library

import com.aritr.zinely.core.model.ZineCoverRecipe
import com.aritr.zinely.core.model.ZineCoverStamp
import com.aritr.zinely.core.model.ZineCoverSurface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which zines share a name, and what the note then says (`v21-library.html` A37.3.3, issue #145): counted from the
 * Shelf's own zines, in Shelf order, in the zine's own place. The words themselves are `RestoredCopyTest`'s.
 */
class RestoredNoteWordsTest {

    private fun shelf(vararg titleAndFolder: Pair<String, String?>): List<LibraryZine> =
        titleAndFolder.mapIndexed { i, (title, folder) ->
            LibraryZine("z$i", title, "A4", ZineCoverRecipe(ZineCoverSurface.MatchaInk, ZineCoverStamp.Sun), folder = folder)
        }

    private fun words(shelf: List<LibraryZine>, vararg ids: Pair<String, String?>) =
        ids.map { (id, was) -> RestoredZine(id, shelf.first { it.id == id }.title, was) }.noteWords(shelf).second

    @Test
    fun `a name carried once says nothing extra`() {
        val s = shelf("Notes" to null, "Riso tests" to null)
        assertTrue(words(s, "z0" to null, "z1" to null).startsWith("“Notes” and “Riso tests”. "))
    }

    @Test
    fun `a shared name is counted in Shelf order, whatever order the note lists them in`() {
        val s = shelf("Notes" to null, "Riso tests" to null, "Notes" to null, "Notes" to null)
        // z3 went back, so the note lists it first; its place is still 3 of 3.
        assertTrue(
            words(s, "z0" to null, "z3" to "Trip notes").startsWith(
                "“Notes” (was “Trip notes”, 3 of 3 with this name) and “Notes” (1 of 3 with this name). ",
            ),
        )
    }

    @Test
    fun `a zine in a folder names its folder, and counts only inside it`() {
        val s = shelf("Notes" to "Trips", "Notes" to "Trips", "Notes" to null)
        assertTrue(
            words(s, "z1" to null, "z2" to null).startsWith(
                "“Notes” (in “Trips”, 2 of 2 with this name) and “Notes” (on My Shelf). ",
            ),
        )
    }

    @Test
    fun `sharers spread over two places each say where, even when alone in theirs`() {
        val s = shelf("Notes" to null, "Notes" to "Trips")
        assertTrue(words(s, "z0" to null, "z1" to null).startsWith("“Notes” (on My Shelf) and “Notes” (in “Trips”). "))
    }

    @Test
    fun `names are compared without their leading and trailing spaces, but not their case`() {
        val s = shelf("Notes " to null, "Notes" to null, "notes" to null)
        assertTrue(words(s, "z0" to null, "z2" to null).startsWith("“Notes ” (1 of 2 with this name) and “notes”. "))
    }

    @Test
    fun `a restored zine the Shelf does not list takes the plain form`() {
        val s = shelf("Notes" to null)
        val r = listOf(RestoredZine("gone", "Notes", null), RestoredZine("z0", "Notes", null))
        assertTrue(r.noteWords(s).second.startsWith("“Notes” and “Notes”. "))
    }

    @Test
    fun `one zine with a shared name gets the sentence`() {
        val s = shelf("Notes" to null, "Notes" to null)
        val (title, body) = listOf(RestoredZine("z1", "Notes", "Trip notes")).noteWords(s)
        assertEquals("“Trip notes” is called “Notes” again", title)
        assertTrue(body.contains(" More than one zine has this name; this is number 2 of 2, counting from the top of the shelf."))
    }
}
