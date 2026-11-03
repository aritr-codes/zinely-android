package com.aritr.zinely.core.editor

import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.StringLength
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** ADR-147 decision 2 / Bench A52.3: what a description is allowed to be, as pure text rules. */
class DescriptionTextTest {

    private fun cps(s: String) = s.codePointCount(0, s.length)

    @Test
    fun `control characters and line separators become spaces`() {
        assertEquals("a b", DescriptionText.normalise("a\nb"))
        assertEquals("a b", DescriptionText.normalise("a\rb"))
        assertEquals("a  b", DescriptionText.normalise("a\r\nb"))
        assertEquals("a b", DescriptionText.normalise("a\tb"))
        assertEquals("a b", DescriptionText.normalise("a\u0007b"))
        assertEquals("a b", DescriptionText.normalise("a b"))
        assertEquals("a b", DescriptionText.normalise("a b"))
    }

    @Test
    fun `both ends are trimmed and the middle is untouched`() {
        assertEquals("Mum at the gate", DescriptionText.normalise("  \n Mum at the gate \t "))
        assertEquals("two  spaces", DescriptionText.normalise("two  spaces"))
    }

    @Test
    fun `blank and spaces-only are no description`() {
        assertNull(DescriptionText.normalise(null))
        assertNull(DescriptionText.normalise(""))
        assertNull(DescriptionText.normalise("   "))
        assertNull(DescriptionText.normalise("\n\t "))
    }

    @Test
    fun `a stored description longer than the cap is not cut by normalising`() {
        val long = "x".repeat(300)
        assertEquals(long, DescriptionText.normalise(long))
    }

    @Test
    fun `typing stops at 250 code points`() {
        val full = "a".repeat(250)
        assertEquals(full, DescriptionText.edit(opening = "", next = full))
        assertEquals(full, DescriptionText.edit(opening = "", next = full + "b"))
    }

    @Test
    fun `a paste is cut at the cap`() {
        assertEquals("a".repeat(250), DescriptionText.edit("", "a".repeat(400)))
    }

    @Test
    fun `a family emoji at the cap goes whole or not at all`() {
        val family = "👨‍👩‍👧" // man ZWJ woman ZWJ girl: 5 code points
        val starts = "a".repeat(248)
        // 248 + 5 would pass 250: the whole cluster is left out, never cut through.
        assertEquals(starts, DescriptionText.edit("", starts + family))
        val roomy = "a".repeat(245)
        assertEquals(roomy + family, DescriptionText.edit("", roomy + family))
    }

    @Test
    fun `a combined mark is kept with its letter or dropped with it`() {
        val e = "é" // e + combining acute
        val text = "a".repeat(249) + e
        assertEquals("a".repeat(249), DescriptionText.edit("", text))
    }

    @Test
    fun `growth only - a stored 300 survives an unchanged close and refuses growth`() {
        val stored = "y".repeat(300)
        assertEquals(stored, DescriptionText.edit(opening = stored, next = stored))
        assertEquals(stored, DescriptionText.edit(opening = stored, next = stored + "z"))
        // Deleting then retyping within the opening length is allowed.
        val shorter = stored.dropLast(10)
        assertEquals(shorter, DescriptionText.edit(opening = stored, next = shorter))
        assertEquals(shorter + "q", DescriptionText.edit(opening = stored, next = shorter + "q"))
    }

    @Test
    fun `typed control characters become spaces as they are typed`() {
        assertEquals("a b", DescriptionText.edit("", "a\nb"))
    }

    @Property
    fun `edit never exceeds the larger of the cap and the opening, and is idempotent`(
        @ForAll @StringLength(max = 600) opening: String,
        @ForAll @StringLength(max = 600) next: String,
    ) {
        val out = DescriptionText.edit(opening, next)
        assertTrue(cps(out) <= maxOf(DescriptionText.MAX_CODE_POINTS, cps(opening)))
        assertEquals(out, DescriptionText.edit(opening, out))
    }

    @Property
    fun `normalise is idempotent`(@ForAll @StringLength(max = 400) raw: String) {
        val once = DescriptionText.normalise(raw)
        assertEquals(once, DescriptionText.normalise(once))
    }
}
