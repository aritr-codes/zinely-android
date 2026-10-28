package com.aritr.zinely.core.copy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Choosing several zines: [Copy.Select]'s words against the frozen `v21-library.html` A47 (ADR-142), including the
 * prototype's `a47Names` list rule (up to four in full; past four, three and "and {k} more").
 */
class SelectCopyTest {

    private val one = listOf("A")
    private val two = listOf("A", "B")
    private val four = listOf("A", "B", "C", "D")
    private val five = listOf("A", "B", "C", "D", "E")

    @Test
    fun `notDeleted names one, two, four and five zines`() {
        assertEquals("“A” wasn’t deleted. It’s back on your shelf; try again in a moment.", Copy.Select.notDeleted(one))
        assertEquals(
            "2 zines weren’t deleted: “A” and “B”. They’re back on your shelf; try again in a moment.",
            Copy.Select.notDeleted(two),
        )
        assertEquals(
            "4 zines weren’t deleted: “A”, “B”, “C” and “D”. They’re back on your shelf; try again in a moment.",
            Copy.Select.notDeleted(four),
        )
        assertEquals(
            "5 zines weren’t deleted: “A”, “B”, “C” and 2 more. They’re back on your shelf; try again in a moment.",
            Copy.Select.notDeleted(five),
        )
    }

    @Test
    fun `notMoved names one, two, four and five zines, after the line or alone`() {
        val line = "Moved 2 zines to “Family”"
        assertEquals("$line. “A” didn’t move; try again in a moment.", Copy.Select.notMoved(line, one))
        assertEquals("“A” and “B” didn’t move; try again in a moment.", Copy.Select.notMoved(null, two))
        assertEquals("“A”, “B”, “C” and “D” didn’t move; try again in a moment.", Copy.Select.notMoved(null, four))
        assertEquals("$line. “A”, “B”, “C” and 2 more didn’t move; try again in a moment.", Copy.Select.notMoved(line, five))
    }

    @Test
    fun `naming no zine is a programming error, not an empty sentence`() {
        assertThrows<IllegalArgumentException> { Copy.Select.notDeleted(emptyList()) }
        assertThrows<IllegalArgumentException> { Copy.Select.notMoved(null, emptyList()) }
    }

    @Test
    fun `the snack lines for a set`() {
        assertEquals("Moved 3 zines to “Family”", Copy.Select.movedTo("3 zines", "Family"))
        assertEquals("Made “Family” with 3 zines", Copy.Select.madeWith("Family", "3 zines"))
        assertEquals("Moved 3 zines to My Shelf", Copy.Select.movedToMyShelf("3 zines"))
        assertEquals("3 zines are back in “Family”", Copy.Select.areBackIn("3 zines", "Family"))
        assertEquals("3 zines are back on My Shelf", Copy.Select.areBackOnMyShelf("3 zines"))
        assertEquals("Deleted 3 zines", Copy.Select.deleted("3 zines"))
    }

    @Test
    fun `R6 and R7 - a partial Undo into a folder, and the chooser's pane title`() {
        assertEquals("Some zines didn’t go back. They’re still in “Family”, nothing is lost", Copy.Select.notAllBack("Family"))
        assertEquals("Move 3 zines", Copy.Select.movePane("3 zines"))
    }

    @Test
    fun `the bar, the dock and the name sheet`() {
        assertEquals("3 selected", Copy.Select.selectedCount(3))
        assertEquals("Move 3 zines to a folder", Copy.Select.moveToAFolder("3 zines"))
        assertEquals("Move 3 zines somewhere else", Copy.Select.moveSomewhereElse("3 zines"))
        assertEquals("Delete 3 zines", Copy.Select.delete("3 zines"))
        assertEquals("For 3 zines", Copy.Select.forZines("3 zines"))
        assertEquals("They’re already in “Family”.", Copy.Select.alreadyIn("Family"))
        assertEquals("You already have “Family”. These zines will join it.", Copy.Select.willJoin("Family"))
    }
}
