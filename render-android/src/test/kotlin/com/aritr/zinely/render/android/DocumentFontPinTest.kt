package com.aritr.zinely.render.android

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **The Book faces are upstream's own files, byte for byte (ADR-126 decision 4); the Hand faces are the
 * ones `--hand` builds from the pinned release (ADR-148 decision 4).**
 *
 * `tools/build-document-fonts.py` is the one record of the eight SHA-256 pins. This reads the pins out of
 * that script and holds the files in `assets/fonts/` to them, so a face that is re-instanced, subset,
 * renamed or swapped by hand (any of which would move every Book or Hand golden, and for Book would make it
 * a Modified Version under the OFL) fails here and names the file.
 *
 * Plain JVM: it reads the source tree, not the merged assets, because the claim is about what is
 * committed.
 */
class DocumentFontPinTest {

    private val pins: Map<String, String> =
        Regex(""""((?:Fraunces9pt|ShantellSans)-[A-Za-z]+\.ttf)":\s*\(\s*"[^"]+",\s*"([0-9a-f]{64})"\)""")
            .findAll(File("../tools/build-document-fonts.py").readText())
            .associate { it.groupValues[1] to it.groupValues[2] }

    @Test
    fun `the script pins exactly the faces the registry names for Book and for Hand`() {
        val registered = listOf(DocumentFontRegistry.FRAUNCES, DocumentFontRegistry.SHANTELL_SANS).flatMap { name ->
            val f = DocumentFontRegistry.Bundled.resolve(name)
            listOf(f.regularAsset, f.boldAsset, f.italicAsset, f.boldItalicAsset).map { it.substringAfterLast('/') }
        }

        assertEquals(8, registered.distinct().size)
        assertEquals(registered.toSet(), pins.keys)
    }

    @Test
    fun `every bundled Book and Hand face equals its pinned hash`() {
        assertTrue("no pins were read from the script", pins.isNotEmpty())
        for ((name, pinned) in pins) {
            val bytes = File("src/main/assets/fonts/$name").readBytes()
            val actual = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            assertEquals("$name is not the pinned file", pinned, actual)
        }
    }
}
