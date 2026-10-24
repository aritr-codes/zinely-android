package com.aritr.zinely.core.copy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

/**
 * The restored-details note's words against the frozen `v21-library.html` A37.3, form by form, including the
 * prototype's own five-zine probe output (ADR-134 / D-120 RI-4).
 */
class RestoredCopyTest {

    private val body1 = "A small file that holds its name, cover and folder was damaged, so Zinely used the copy " +
        "from before the last change to them."
    private val bodyN = "Small files that hold their names, covers and folders were damaged, so Zinely used the " +
        "copies from before the last changes to them."

    @Test
    fun `one zine whose name went back`() {
        assertEquals(
            "“Letters to Gran” is called “Letters home” again" to "$body1 You can rename it again.",
            Copy.Restored.note(listOf("Letters home" to "Letters to Gran")),
        )
    }

    @Test
    fun `one zine whose name did not go back, or is not known, takes the plain form`() {
        val plain = "Zinely repaired “Coffee log”" to body1
        assertEquals(plain, Copy.Restored.note(listOf("Coffee log" to null)))
        assertEquals(plain, Copy.Restored.note(listOf("Coffee log" to "Coffee log")))
    }

    @Test
    fun `two to four zines are listed in full`() {
        assertEquals(
            "Zinely repaired 2 zines" to "“Letters home” (was “Letters to Gran”) and “Riso tests”. $bodyN",
            Copy.Restored.note(listOf("Letters home" to "Letters to Gran", "Riso tests" to null)),
        )
        assertEquals(
            "“A”, “B”, “C” and “D”. $bodyN",
            Copy.Restored.note(listOf("A" to null, "B" to null, "C" to null, "D" to null)).second,
        )
    }

    @Test
    fun `past four, three names and the rest counted, never one`() {
        val five = listOf(
            "Letters home" to "Letters to Gran",
            "Riso tests" to null,
            "Mum’s garden" to null,
            "Tiny poems" to null,
            "Coffee log" to null,
        )
        assertEquals(
            "Zinely repaired 5 zines" to
                "“Letters home” (was “Letters to Gran”), “Riso tests”, “Mum’s garden” and 2 more. $bodyN",
            Copy.Restored.note(five),
        )
    }

    @Test
    fun `the words never say backup, restore, error, corrupt or metadata (A37_4)`() {
        val words = listOf(
            Copy.Restored.note(listOf("X" to "Y")),
            Copy.Restored.note(listOf("X" to null)),
            Copy.Restored.note(List(6) { "Z$it" to null }),
        ).flatMap { it.toList() } + Copy.Restored.GOT_IT
        for (banned in listOf("backup", "restor", "error", "corrupt", "metadata", ".json")) {
            assertFalse(words.any { it.contains(banned, ignoreCase = true) }) { "says $banned" }
        }
    }

    // ---- A46.3 (ADR-140): a page file put back from its earlier copy. Literals transcribed from the frozen page.

    private val pagesBody = "A page file was damaged, so Zinely used the copy from just before your last few " +
        "seconds of changes."
    private val bothBody = "$body1 A page file was damaged too, so its pages are from just before your last few " +
        "seconds of changes."
    private val pagesN = "Their page files were damaged, so Zinely used the copies from just before your last few " +
        "seconds of changes."
    private val mixedN = "Some of their files were damaged, so Zinely used the copies it kept. Names, covers and " +
        "folders are from before the last change to them; pages are from just before your last few seconds of changes."

    private fun pages(now: String) = Copy.Restored.Zine(now, was = null, details = false, pages = true)

    @Test
    fun `one zine, pages only, is named plainly, whatever name its details once had`() {
        assertEquals("Zinely repaired “Coffee log”" to pagesBody, Copy.Restored.noteFor(listOf(pages("Coffee log"))))
        // {was} belongs to a details repair only (A46.3).
        assertEquals(
            "Zinely repaired “Coffee log”" to pagesBody,
            Copy.Restored.noteFor(listOf(Copy.Restored.Zine("Coffee log", "Old", details = false, pages = true))),
        )
    }

    @Test
    fun `one zine, details and pages, takes the A37 title and both sentences`() {
        assertEquals(
            "“Letters to Gran” is called “Letters home” again" to "$bothBody You can rename it again.",
            Copy.Restored.noteFor(listOf(Copy.Restored.Zine("Letters home", "Letters to Gran", pages = true))),
        )
        assertEquals(
            "Zinely repaired “Coffee log”" to bothBody,
            Copy.Restored.noteFor(listOf(Copy.Restored.Zine("Coffee log", null, pages = true))),
        )
    }

    @Test
    fun `several zines, all pages only, and mixed`() {
        assertEquals(
            "Zinely repaired 2 zines" to "“Coffee log” and “Riso tests”. $pagesN",
            Copy.Restored.noteFor(listOf(pages("Coffee log"), pages("Riso tests"))),
        )
        assertEquals(
            "Zinely repaired 3 zines" to "“Letters home” (was “Letters to Gran”), “Riso tests” and “Coffee log”. $mixedN",
            Copy.Restored.noteFor(
                listOf(
                    Copy.Restored.Zine("Letters home", "Letters to Gran"),
                    pages("Riso tests"),
                    Copy.Restored.Zine("Coffee log", "Coffee diary", details = false, pages = true),
                ),
            ),
        )
    }

    @Test
    fun `no pages repair at all is A37_3 unchanged`() {
        val pairs = listOf("Letters home" to "Letters to Gran", "Riso tests" to null)
        assertEquals(
            Copy.Restored.note(pairs),
            Copy.Restored.noteFor(pairs.map { (now, was) -> Copy.Restored.Zine(now, was) }),
        )
    }

    @Test
    fun `the A46 words never say backup, restore, error, corrupt or metadata (A46_4)`() {
        val words = listOf(
            Copy.Restored.noteFor(listOf(pages("X"))),
            Copy.Restored.noteFor(listOf(Copy.Restored.Zine("X", "Y", pages = true))),
            Copy.Restored.noteFor(List(6) { pages("Z$it") }),
            Copy.Restored.noteFor(listOf(pages("X"), Copy.Restored.Zine("Y", "W"))),
        ).flatMap { it.toList() }
        for (banned in listOf("backup", "restor", "error", "corrupt", "metadata", ".json")) {
            assertFalse(words.any { it.contains(banned, ignoreCase = true) }) { "says $banned" }
        }
    }

    @Test
    fun `TalkBack reads the title and body as one sentence pair`() {
        assertEquals("Zinely repaired “X”. Body.", Copy.Restored.spoken("Zinely repaired “X”", "Body."))
    }
}
