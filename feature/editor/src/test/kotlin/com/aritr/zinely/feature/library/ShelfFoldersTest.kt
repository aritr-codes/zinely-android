package com.aritr.zinely.feature.library

import com.aritr.zinely.core.model.ZineCoverRecipe
import com.aritr.zinely.core.model.ZineCoverStamp
import com.aritr.zinely.core.model.ZineCoverSurface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What stands on the Shelf, and what the name sheet answers (`v21-library.html` A28,
 * [ADR-125](docs/DECISIONS.md#adr-125)). Pure: lists and strings in, lists and strings out.
 *
 * The expected words are written out here from the frozen file, not read from `Copy`, so a changed string
 * fails here and is not agreed with.
 */
class ShelfFoldersTest {

    private fun zine(id: String, folder: String? = null) = LibraryZine(
        id = id,
        title = id,
        subtitle = "A4 · today",
        cover = ZineCoverRecipe(ZineCoverSurface.MatchaInk, ZineCoverStamp.Sun),
        folder = folder,
    )

    /** The frozen page's own seeded Shelf (`seed(true)`), newest first. */
    private val seeded = listOf(
        zine("Sunday market", "For the stall"),
        zine("Letters home", "Family"),
        zine("Riso tests"),
        zine("Mum's garden", "Family"),
        zine("Tiny poems", "For the stall"),
        zine("Coffee log", "For the stall"),
    )

    private fun List<ShelfTile>.names() = map {
        when (it) {
            is ShelfTile.Zine -> it.zine.id
            is ShelfTile.Pile -> "[${it.name}: ${it.zines.joinToString { z -> z.id }}]"
        }
    }

    @Test
    fun `with no folder made the Shelf is its zines, in the order given`() {
        val zines = listOf(zine("a"), zine("b"), zine("c"))
        assertEquals(listOf("a", "b", "c"), shelfTiles(zines, openFolder = null).names())
        assertTrue(shelfFolders(zines).isEmpty())
    }

    @Test
    fun `a folder is one pile, standing where its newest zine would stand`() {
        // A28.4. Six zines, three tiles: each pile sits at its first (newest) zine and holds the rest,
        // still newest first.
        assertEquals(
            listOf(
                "[For the stall: Sunday market, Tiny poems, Coffee log]",
                "[Family: Letters home, Mum's garden]",
                "Riso tests",
            ),
            shelfTiles(seeded, openFolder = null).names(),
        )
    }

    @Test
    fun `inside a folder the tiles are that folder's zines, newest first`() {
        assertEquals(listOf("Letters home", "Mum's garden"), shelfTiles(seeded, openFolder = "Family").names())
        // A folder with nothing in it is nothing: the screen reads this as "go back to My Shelf".
        assertTrue(shelfTiles(seeded, openFolder = "Gone").isEmpty())
    }

    @Test
    fun `the folders are counted from the zines shown, in the order their piles stand`() {
        assertEquals(listOf("For the stall" to 3, "Family" to 2), shelfFolders(seeded).toList())
    }

    // ---- the name sheet (A28.10) -------------------------------------------------------------------

    private val keys = mapOf("family" to "Family", "for the stall" to "For the stall")
    private fun name(typed: String) = plainFolderNameVerdict(typed)

    @Test
    fun `New folder - a new name makes a folder`() {
        val answer = newFolderAnswer(name("  Trips "), keys, current = null)
        assertEquals("Make folder", answer.button)
        assertTrue(answer.enabled)
        assertEquals("", answer.hint)
        assertEquals("the name is trimmed", "Trips", answer.name)
    }

    @Test
    fun `New folder - a name that exists joins that folder and takes its spelling`() {
        val answer = newFolderAnswer(name("FAMILY"), keys, current = null)
        assertEquals("Move to “Family”", answer.button)
        assertTrue(answer.enabled)
        assertEquals("You already have “Family”. This zine will join it.", answer.hint)
        assertEquals("the first spelling is kept", "Family", answer.name)
    }

    @Test
    fun `New folder - the folder the zine is already in is not a move`() {
        val answer = newFolderAnswer(name("family"), keys, current = "Family")
        assertFalse(answer.enabled)
        assertEquals("It’s already in “Family”.", answer.hint)
    }

    @Test
    fun `New folder - nothing typed, and My Shelf, cannot be made`() {
        val blank = newFolderAnswer(name("   "), keys, current = null)
        assertEquals("Make folder", blank.button)
        assertFalse(blank.enabled)
        assertEquals("", blank.hint)
        assertNull(blank.name)

        val shelf = newFolderAnswer(name("my shelf"), keys, current = null)
        assertEquals("Make folder", shelf.button)
        assertFalse(shelf.enabled)
        assertEquals("“My Shelf” is where loose zines sit. Try another name.", shelf.hint)
    }

    @Test
    fun `Rename folder - another folder's name is refused`() {
        val answer = renameFolderAnswer(name("for the STALL"), keys, renaming = "Family")
        assertEquals("Rename", answer.button)
        assertFalse(answer.enabled)
        assertEquals("You already have a folder called “For the stall”.", answer.hint)
    }

    @Test
    fun `Rename folder - changing only the capitals of its own name is a rename like any other`() {
        val answer = renameFolderAnswer(name("FAMILY"), keys, renaming = "Family")
        assertTrue(answer.enabled)
        assertEquals("", answer.hint)
        assertEquals("FAMILY", answer.name)
    }

    @Test
    fun `Rename folder - its own name unchanged, nothing typed, and My Shelf, are nothing to do`() {
        assertFalse(renameFolderAnswer(name("Family"), keys, renaming = "Family").enabled)
        assertFalse("spaces round it are not a change", renameFolderAnswer(name(" Family "), keys, renaming = "Family").enabled)
        assertFalse(renameFolderAnswer(name(""), keys, renaming = "Family").enabled)
        val shelf = renameFolderAnswer(name("My Shelf"), keys, renaming = "Family")
        assertEquals("Rename", shelf.button)
        assertFalse(shelf.enabled)
        assertEquals("“My Shelf” is where loose zines sit. Try another name.", shelf.hint)
    }

    @Test
    fun `the stand-in name rule trims, ignores case, refuses My Shelf and keeps forty characters`() {
        assertEquals(FolderNameVerdict.Blank, plainFolderNameVerdict(" \t "))
        assertEquals(FolderNameVerdict.MyShelf, plainFolderNameVerdict(" MY shelf "))
        assertEquals(FolderNameVerdict.Name("Trips", "trips"), plainFolderNameVerdict(" Trips "))
        val long = plainFolderNameVerdict("a".repeat(41)) as FolderNameVerdict.Name
        assertEquals(40, long.name.length)
        assertTrue(long.cut)
    }
}
