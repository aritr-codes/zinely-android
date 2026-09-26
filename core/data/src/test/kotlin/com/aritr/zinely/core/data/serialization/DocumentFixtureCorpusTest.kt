package com.aritr.zinely.core.data.serialization

import com.aritr.zinely.core.data.validation.DefaultDocumentValidator
import com.aritr.zinely.core.model.Background
import com.aritr.zinely.core.model.CURRENT_SCHEMA_VERSION
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Element
import com.aritr.zinely.core.model.Fit
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.TextAlign
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.TextStyle
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.security.MessageDigest

/**
 * The frozen document corpus (1.x plan §4 F3): one fictional zine as each schema version wrote it,
 * checked in under `src/test/resources/fixtures/`. Every future build must decode all of them to the
 * same document. Unlike the round-trip tests, these bytes cannot move with the code.
 *
 * - `document-v3.json` was written by [JsonDocumentSerializer] at `5f7707a` (0.9.0-beta.5).
 * - `document-v2.json` and `document-v1.json` are that output with the fields their versions predate
 *   removed: v2 has no flip fields (ADR-113); v1 also has no `copier` (ADR-106) and no decor
 *   (ADR-105). Checked against the `v0.9.0-beta.2` and `v0.9.0-beta.1` model sources; the serializer
 *   configuration and enum wire names are unchanged since beta.1.
 *
 * The expected documents spell out every field an older file lacks, so a changed default fails here —
 * the half the schema-shape guard in `:core:model` cannot see.
 *
 * Fixtures are frozen. A failure is a compatibility break to fix in code; never regenerate or edit a
 * fixture to pass. A new schema version adds a new file beside these.
 */
class DocumentFixtureCorpusTest {

    private val serializer = JsonDocumentSerializer()

    /**
     * One expected document per pinned fixture. Each entry spells out every field its file lacks, so a
     * changed default fails. A fixture pinned in [FIXTURE_SHA256] without an entry here fails the
     * per-fixture decode check; it is never merely listed.
     */
    private val expectedByVersion: Map<Int, () -> ZineDocument> = mapOf(
        // v1 predates copier (ADR-106), decor (ADR-105) and the flips (ADR-113).
        1 to {
            expectedV3().mapElements { element ->
                when (element) {
                    is ImageElement -> element.copy(copier = false, flippedHorizontally = false, flippedVertically = false)
                    is DecorElement -> null
                    else -> element
                }
            }
        },
        // v2 predates the flips (ADR-113).
        2 to {
            expectedV3().mapElements { element ->
                when (element) {
                    is ImageElement -> element.copy(flippedHorizontally = false, flippedVertically = false)
                    is DecorElement -> element.copy(flippedVertically = false)
                    else -> element
                }
            }
        },
        3 to ::expectedV3,
    )

    @Test
    fun `every schema version has a frozen fixture and an expected document`() {
        assertEquals((1..CURRENT_SCHEMA_VERSION).toSet(), FIXTURE_SHA256.keys) {
            "A schema bump needs document-v$CURRENT_SCHEMA_VERSION.json in the corpus, pinned and decoded here."
        }
        assertEquals(FIXTURE_SHA256.keys, expectedByVersion.keys) {
            "Every pinned fixture needs an expected document, and every expected document a pinned fixture."
        }
    }

    @Test
    fun `the current writer still writes the pinned bytes of the current version`() {
        // Freezes the encode side too (key order, defaults on disk, the format marker). A schema bump
        // fails here on purpose rather than skipping: the encode check must move to the new version.
        assertEquals(WRITER_PINNED_VERSION, CURRENT_SCHEMA_VERSION) {
            "CURRENT_SCHEMA_VERSION is $CURRENT_SCHEMA_VERSION, but the writer-byte check still pins " +
                "document-v$WRITER_PINNED_VERSION.json. Add document-v$CURRENT_SCHEMA_VERSION.json as written by " +
                "the new serializer, pin its SHA-256 in FIXTURE_SHA256, add its expected document to " +
                "expectedByVersion, then set WRITER_PINNED_VERSION to $CURRENT_SCHEMA_VERSION. Keep every older fixture."
        }
        val expected = expectedByVersion.getValue(WRITER_PINNED_VERSION)()
        assertEquals(fixtureBytes(WRITER_PINNED_VERSION).decodeToString(), serializer.serialize(expected))
    }

    /** One check per pinned fixture, generated from [FIXTURE_SHA256] itself. */
    @TestFactory
    fun `every pinned fixture decodes to its expected document`(): List<DynamicTest> =
        FIXTURE_SHA256.keys.sorted().map { version ->
            dynamicTest("document-v$version.json") {
                val expected = checkNotNull(expectedByVersion[version]) {
                    "document-v$version.json is pinned but has no expected document, so it would never be checked."
                }
                assertDecodes(version, expected())
            }
        }

    @Test
    fun `fixture files are the frozen bytes`() {
        FIXTURE_SHA256.forEach { (version, hash) ->
            assertEquals(hash, sha256(fixtureBytes(version))) {
                "document-v$version.json changed. Fixtures are frozen: add a new file, never edit one."
            }
        }
    }

    private fun assertDecodes(version: Int, expected: ZineDocument) {
        val text = fixtureBytes(version).decodeToString()
        assertTrue(text.contains("\"schemaVersion\":$version,")) { "document-v$version.json is not a v$version file" }

        val decoded = serializer.deserialize(text)

        assertEquals(expected, decoded)
        val validation = DefaultDocumentValidator().validate(decoded)
        assertTrue(validation.isValid) { validation.toString() }
    }

    private fun fixtureBytes(version: Int): ByteArray =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/document-v$version.json")) {
            "missing fixtures/document-v$version.json"
        }.use { it.readBytes() }

    private fun ZineDocument.mapElements(transform: (Element) -> Element?): ZineDocument =
        copy(pages = pages.map { page -> page.copy(elements = page.elements.mapNotNull(transform)) })

    /** Every field explicit, including defaults: an older fixture's missing field must decode to exactly this. */
    private fun expectedV3(): ZineDocument {
        val ink = ColorRgba(20, 20, 20, 255)
        fun page(index: Int, elements: List<Element> = emptyList(), background: Background = Background.None) = Page(
            index = index,
            role = when (index) {
                0 -> PageRole.FRONT_COVER
                7 -> PageRole.BACK_COVER
                else -> PageRole.INTERIOR
            },
            background = background,
            elements = elements,
        )
        return ZineDocument(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            format = ZineFormat.SINGLE_SHEET_8,
            paperSize = PaperSize.A4,
            defaults = DocumentDefaults(
                textStyle = TextStyle("serif", 14.0, ink, TextAlign.START, bold = false, italic = false),
                background = Background.None,
            ),
            pages = listOf(
                page(
                    0,
                    background = Background.Solid(ColorRgba(250, 240, 220, 255)),
                    elements = listOf(
                        ImageElement(
                            id = "cover-photo",
                            transform = Transform(18.0, 24.0, 150.0, 200.0, 0.0),
                            zIndex = 0,
                            assetId = PHOTO_A,
                            crop = Crop(0.1, 0.0, 0.9, 1.0),
                            fit = Fit.FILL,
                            copier = true,
                            flippedHorizontally = true,
                            flippedVertically = false,
                        ),
                        TextElement(
                            id = "cover-title",
                            transform = Transform(18.0, 232.0, 150.0, 32.0, -4.0),
                            zIndex = 1,
                            text = "Moth Club Bulletin",
                            style = TextStyle(
                                "serif", 22.0, ColorRgba(120, 30, 40, 255), TextAlign.CENTER, bold = true, italic = false,
                            ),
                        ),
                    ),
                ),
                page(1),
                page(2),
                page(
                    3,
                    elements = listOf(
                        ImageElement(
                            id = "lamp-photo",
                            transform = Transform(20.0, 30.0, 160.0, 100.0, 0.0),
                            zIndex = 0,
                            assetId = PHOTO_B,
                            crop = Crop(0.0, 0.0, 1.0, 1.0),
                            fit = Fit.FIT,
                            copier = false,
                            flippedHorizontally = false,
                            flippedVertically = true,
                        ),
                        TextElement(
                            id = "lamp-caption",
                            transform = Transform(20.0, 140.0, 160.0, 40.0, 0.0),
                            zIndex = 1,
                            text = "Found near the porch light, 9 pm.",
                            style = TextStyle("sans-serif", 11.0, ink, TextAlign.START, bold = false, italic = true),
                        ),
                        DecorElement(
                            id = "lamp-tape",
                            transform = Transform(60.0, 22.0, 80.0, 18.0, 8.0),
                            zIndex = 2,
                            supplyId = "tape.torn",
                            ink = ColorRgba(200, 40, 90, 255),
                            mirrored = true,
                            flippedVertically = true,
                        ),
                    ),
                ),
                page(4),
                page(5),
                page(6),
                page(
                    7,
                    elements = listOf(
                        TextElement(
                            id = "back-note",
                            transform = Transform(20.0, 240.0, 160.0, 24.0, 0.0),
                            zIndex = 0,
                            text = "Fold it, cut it, pass it on.",
                            style = TextStyle("serif", 12.0, ink, TextAlign.END, bold = false, italic = false),
                        ),
                    ),
                ),
            ),
        )
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

    private companion object {
        /** The version whose fixture the current writer must reproduce byte for byte; moved on each bump. */
        const val WRITER_PINNED_VERSION = 3

        /** SHA-256 of the two fixture JPEGs, which live in the `:core:data-storage` library archive. */
        const val PHOTO_A = "380704824c8bf2b6f7f69cca9eae828fee7ff44b2328820e49eec362c674ed3a"
        const val PHOTO_B = "8f914e9d17abc02fbccbf33f384481a28e1ccb9dd642f3b4cf0e5b1ddd3f3219"

        /** The same three hashes are pinned by `LibraryBackupFixtureTest`: the archive carries these bytes. */
        val FIXTURE_SHA256 = mapOf(
            1 to "b0e82a1a512ae724930f7b32d81213dca927f0e9f1b3233791e20618253da91e",
            2 to "3ba7370f44123cf9dde809ee83dc4e951d20e19e2cdc4502197ed3fe1346ed24",
            3 to "bba2f22d633bf0e3da9d9b72559bcfa0a937f459309dcb79b92a4e68fab13ae9",
        )
    }
}
