package com.aritr.zinely.feature.library

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/** A47.2, A47.4, A47.6, A47.10: the choice itself, without a screen. */
class ShelfSelectionTest {

    @Test
    fun `starting chooses that one zine, which is the last touched`() {
        assertEquals(ShelfSelection(listOf("a"), null, "a"), ShelfSelection.start("a", null))
    }

    @Test
    fun `a tap chooses a zine or lets it go, in the order chosen`() {
        val two = ShelfSelection.start("a", null).toggle("b")!!
        assertEquals(listOf("a", "b"), two.ids)
        assertEquals("b", two.last)
        val back = two.toggle("a")!!
        assertEquals(listOf("b"), back.ids)
        assertEquals("a", back.last)
    }

    @Test
    fun `letting go of the last zine stops choosing`() {
        assertNull(ShelfSelection.start("a", null).toggle("a"))
    }

    @Test
    fun `a zine that is gone is dropped, and none left stops choosing`() {
        val s = ShelfSelection(listOf("a", "b"), "Family", "b")
        assertEquals(listOf("b"), s.prune("Family", setOf("b", "c"))!!.ids)
        assertSame(s, s.prune("Family", setOf("a", "b")))
        assertNull(s.prune("Family", setOf("c")))
    }

    @Test
    fun `another view ends the choice`() {
        val s = ShelfSelection(listOf("a"), null, "a")
        assertNull(s.prune("Family", setOf("a")))
        assertNull(ShelfSelection(listOf("a"), "Family", "a").prune(null, setOf("a")))
    }

    @Test
    fun `the choice survives being saved, on My Shelf and in a folder`() {
        val scope = SaverScope { true }
        listOf(ShelfSelection(listOf("a", "b"), null, "b"), ShelfSelection(listOf("a"), "Family", "a"), null).forEach { s ->
            val saved = with(ShelfSelection.Saver) { scope.save(s) }!!
            assertEquals(s, ShelfSelection.Saver.restore(saved))
        }
    }
}
