package com.aritr.zinely.render.android

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **The Book faces are upstream's own files, byte for byte (ADR-126 decision 4).**
 *
 * `tools/build-document-fonts.py` is the one record of the four SHA-256 pins. This reads the pins out of
 * that script and holds the files in `assets/fonts/` to them, so a face that is re-instanced, subset,
 * renamed or swapped (any of which would make it a Modified Version under the OFL, and would move every
 * Book golden) fails here and names the file.
 *
 * Plain JVM: it reads the source tree, not the merged assets, because the claim is about what is
 * committed.
 */
class DocumentFontPinTest {

    private val pins: Map<String, String> =
        Regex(""""(Fraunces9pt-[A-Za-z]+\.ttf)":\s*\(\s*"[^"]+",\s*"([0-9a-f]{64})"\)""")
            .findAll(File("../tools/build-document-fonts.py").readText())
            .associate { it.groupValues[1] to it.groupValues[2] }

    @Test
    fun `the script pins exactly the four faces the registry names for Book`() {
        val book = DocumentFontRegistry.Bundled.resolve(DocumentFontRegistry.FRAUNCES)
        val registered = listOf(book.regularAsset, book.boldAsset, book.italicAsset, book.boldItalicAsset)
            .map { it.substringAfterLast('/') }

        assertEquals(4, registered.distinct().size)
        assertEquals(registered.toSet(), pins.keys)
    }

    @Test
    fun `every bundled Book face equals its pinned hash`() {
        assertTrue("no pins were read from the script", pins.isNotEmpty())
        for ((name, pinned) in pins) {
            val bytes = File("src/main/assets/fonts/$name").readBytes()
            val actual = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            assertEquals("$name is not the pinned upstream file", pinned, actual)
        }
    }
}
