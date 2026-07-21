package com.aritr.zinely.render.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [DocumentFontRegistry] contract (F3): family resolution is explicit, total, and deterministic — the
 * property that lets every surface agree on what a document's `fontFamily` means. No Android; plain JVM.
 * Given-When-Then.
 */
class DocumentFontRegistryTest {

    private fun family(name: String) = DocumentFontFamily(
        name = name,
        regularAsset = "fonts/$name-Regular.ttf",
        boldAsset = "fonts/$name-Bold.ttf",
        italicAsset = "fonts/$name-Italic.ttf",
        boldItalicAsset = "fonts/$name-BoldItalic.ttf",
    )

    private fun registry(vararg names: String, default: String = names.first()) =
        DocumentFontRegistry(names.map(::family), default)

    @Test
    fun `a registered family resolves to itself`() {
        val r = registry("Inter", "Fraunces")

        assertEquals("Fraunces", r.resolve("Fraunces").name)
        assertTrue(r.isRegistered("Fraunces"))
    }

    @Test
    fun `an unregistered family resolves to the default rather than failing`() {
        val r = registry("Inter", "Fraunces", default = "Inter")

        // A document authored against a font this build does not carry must still render readable text —
        // never a crash, and never a device font whose metrics vary per phone.
        assertEquals("Inter", r.resolve("Comic Sans").name)
        assertSame(r.defaultFamily, r.resolve("Comic Sans"))
    }

    @Test
    fun `substitution is observable, not silent`() {
        val r = registry("Inter", default = "Inter")

        // resolve() alone cannot distinguish "matched" from "fell back" — both return a usable family.
        // isRegistered is what lets a caller be honest with the user about an unavailable font.
        assertTrue(r.isRegistered("Inter"))
        assertFalse(r.isRegistered("Fraunces"))
        assertEquals(r.resolve("Inter").name, r.resolve("Fraunces").name)
    }

    @Test
    fun `matching ignores case and surrounding whitespace but nothing more`() {
        val r = registry("Inter")

        assertTrue(r.isRegistered("inter"))
        assertTrue(r.isRegistered("  INTER  "))
        // Deliberately not fuzzy: a near-miss silently landing on the wrong family is the failure this
        // registry exists to remove, so anything beyond case/trim is an unregistered name.
        assertFalse(r.isRegistered("Inter Tight"))
        assertFalse(r.isRegistered("Int er"))
    }

    @Test
    fun `families are exposed in declaration order`() {
        val r = registry("Inter", "Fraunces", "Georgia")

        assertEquals(listOf("Inter", "Fraunces", "Georgia"), r.families.map { it.name })
    }

    @Test
    fun `an unregistered default is rejected at construction`() {
        // Fail where it is a wiring bug, not part-way through rendering a zine.
        val ex = assertThrows(IllegalArgumentException::class.java) {
            DocumentFontRegistry(listOf(family("Inter")), defaultFamilyName = "Fraunces")
        }
        assertTrue(ex.message!!.contains("not registered"))
    }

    @Test
    fun `duplicate family names are rejected at construction`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            DocumentFontRegistry(listOf(family("Inter"), family("inter")), defaultFamilyName = "Inter")
        }
        assertTrue(ex.message!!.contains("Duplicate") || ex.message!!.contains("duplicate"))
    }

    @Test
    fun `an empty registry is rejected at construction`() {
        assertThrows(IllegalArgumentException::class.java) {
            DocumentFontRegistry(emptyList(), defaultFamilyName = "Inter")
        }
    }

    @Test
    fun `the bundled registry declares exactly what the render module carries`() {
        val bundled = DocumentFontRegistry.Bundled

        // One family today. This asserts what is BUNDLED, not what the registry supports: expanding the
        // set is the designer's font/preset curation, and this test is the tripwire that makes adding a
        // family a deliberate act rather than a silent one.
        assertEquals(listOf(DocumentFontRegistry.INTER), bundled.families.map { it.name })
        assertEquals(DocumentFontRegistry.INTER, bundled.defaultFamily.name)

        val inter = bundled.resolve(DocumentFontRegistry.INTER)
        assertEquals("fonts/Inter-Regular.ttf", inter.regularAsset)
        assertEquals("fonts/Inter-Bold.ttf", inter.boldAsset)
        assertEquals("fonts/Inter-Italic.ttf", inter.italicAsset)
        assertEquals("fonts/Inter-BoldItalic.ttf", inter.boldItalicAsset)
    }
}
