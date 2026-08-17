package com.aritr.zinely.core.copy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The indefinite article in [Copy.Proof.paperChangedResave].
 *
 * The band read *"save again to get **a A4**-sized PDF"* on a device — the article was a literal in a
 * sentence that interpolates its noun, so it could only ever be right for one of the two paper names.
 * Nothing caught it: it is grammatically invisible to every assertion that checks a string *contains* a
 * paper name, which is what the copy tests do.
 *
 * These cases are therefore written against the rendered sentence, not against the helper, because the
 * sentence is the thing that was wrong.
 */
class PaperArticleTest {

    @Test
    fun `A4 takes an, because it is said ay-four`() {
        // Given the paper has just been changed to A4
        val notice = Copy.Proof.paperChangedResave(Copy.Paper.A4, Copy.Paper.LETTER)

        // Then the sentence reads "an A4-sized PDF"
        assertTrue(notice.contains("an A4-sized PDF"), notice)
        assertTrue(!notice.contains("a A4-sized"), "must not read \"a A4\"")
    }

    @Test
    fun `US Letter takes a, despite starting with a vowel LETTER`() {
        // Given the paper has just been changed to US Letter
        val notice = Copy.Proof.paperChangedResave(Copy.Paper.LETTER, Copy.Paper.A4)

        // Then the sentence reads "a US Letter-sized PDF" — English picks by sound ("you-ess"), which is
        // why no vowel-letter rule could serve both names and the exceptions are enumerated instead.
        assertTrue(notice.contains("a US Letter-sized PDF"), notice)
        assertTrue(!notice.contains("an US Letter"), "must not read \"an US Letter\"")
    }

    /**
     * The ceiling, stated as a test: every shipped paper name is either listed as taking *an* or is
     * deliberately taking *a*. A third size added without a thought here fails this, which is the point —
     * the enumeration is only honest while someone maintains it.
     */
    @Test
    fun `every shipped paper name has a considered article`() {
        val shipped = listOf(Copy.Paper.A4, Copy.Paper.LETTER)

        assertEquals(listOf("an", "a"), shipped.map { Copy.Proof.article(it) })
        assertTrue(
            shipped.containsAll(Copy.Proof.TAKES_AN),
            "TAKES_AN lists a name that is not a shipped paper size",
        )
    }
}
