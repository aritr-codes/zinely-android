package com.aritr.zinely.core.data.serialization

import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.CURRENT_SCHEMA_VERSION
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.TextAlign
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.TextStyle
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * The [DocumentSerializer] is the single (de)serialization boundary ([ADR-020]): it always writes
 * the current schema version, decodes tolerantly (`ignoreUnknownKeys`), and runs migration before
 * decoding so an older document opens as a current-shaped [ZineDocument] (S2 spike §3/§6).
 */
class JsonDocumentSerializerTest {

    private val serializer: DocumentSerializer = JsonDocumentSerializer()

    private fun sample(schemaVersion: Int = CURRENT_SCHEMA_VERSION): ZineDocument = ZineDocument(
        schemaVersion = schemaVersion,
        format = ZineFormat.SINGLE_SHEET_8,
        paperSize = PaperSize.LETTER,
        pages = listOf(
            Page(
                index = 0,
                role = PageRole.FRONT_COVER,
                elements = listOf(
                    ImageElement(
                        id = "img-1",
                        transform = Transform(0.0, 0.0, 100.0, 100.0),
                        assetId = "f".repeat(64),
                    ),
                ),
            ),
        ),
    )

    @Test
    fun `serialize then deserialize round-trips`() {
        val doc = sample()
        assertEquals(doc, serializer.deserialize(serializer.serialize(doc)))
    }

    @Test
    fun `photo and art flip combinations round-trip in schema v3`() {
        val elements = buildList {
            for (horizontal in listOf(false, true)) for (vertical in listOf(false, true)) {
                add(
                    ImageElement(
                        id = "image-$horizontal-$vertical",
                        transform = Transform(1.0, 2.0, 30.0, 40.0),
                        assetId = "a".repeat(64),
                        flippedHorizontally = horizontal,
                        flippedVertically = vertical,
                    ),
                )
                add(
                    DecorElement(
                        id = "decor-$horizontal-$vertical",
                        transform = Transform(1.0, 2.0, 30.0, 40.0),
                        supplyId = "shape.star",
                        ink = ColorRgba.BLACK,
                        mirrored = horizontal,
                        flippedVertically = vertical,
                    ),
                )
            }
        }
        val doc = sample().copy(pages = listOf(Page(0, PageRole.INTERIOR, elements = elements)))

        assertEquals(doc, serializer.deserialize(serializer.serialize(doc)))
    }

    @Test
    fun `v2 payload migrates with flip defaults false and preserves historical art mirror`() {
        val v3 = sample().copy(
            pages = listOf(
                Page(
                    0,
                    PageRole.INTERIOR,
                    elements = listOf(
                        DecorElement(
                            id = "old-art",
                            transform = Transform(0.0, 0.0, 20.0, 20.0),
                            supplyId = "shape.star",
                            ink = ColorRgba.BLACK,
                            mirrored = true,
                        ),
                    ),
                ),
            ),
        )
        val v2 = serializer.serialize(v3)
            .replace("\"schemaVersion\":3", "\"schemaVersion\":2")
            .replace(",\"flippedHorizontally\":false", "")
            .replace(",\"flippedVertically\":false", "")

        val decoded = serializer.deserialize(v2)
        val art = decoded.pages.single().elements.single() as DecorElement
        assertEquals(CURRENT_SCHEMA_VERSION, decoded.schemaVersion)
        assertTrue(art.mirrored)
        assertEquals(false, art.flippedVertically)
    }

    @Test
    fun `a fully-styled text element round-trips through JSON (FR-3, ADR-055)`() {
        val doc = ZineDocument(
            format = ZineFormat.SINGLE_SHEET_8,
            paperSize = PaperSize.LETTER,
            pages = listOf(
                Page(
                    index = 0,
                    role = PageRole.INTERIOR,
                    elements = listOf(
                        TextElement(
                            id = "t-1",
                            transform = Transform(1.0, 2.0, 30.0, 40.0),
                            zIndex = 5,
                            text = "styled",
                            style = TextStyle(
                                fontFamily = "serif",
                                sizePt = 18.0,
                                color = ColorRgba(10, 20, 30, 200),
                                align = TextAlign.END,
                                bold = true,
                                italic = true,
                            ),
                        ),
                    ),
                ),
            ),
        )
        assertEquals(doc, serializer.deserialize(serializer.serialize(doc)))
    }

    @Test
    fun `an existing text payload without align keeps the historical start fallback`() {
        val doc = ZineDocument(
            format = ZineFormat.SINGLE_SHEET_8,
            paperSize = PaperSize.LETTER,
            pages = listOf(
                Page(
                    index = 0,
                    role = PageRole.INTERIOR,
                    elements = listOf(
                        TextElement(
                            id = "legacy-text",
                            transform = Transform(1.0, 2.0, 30.0, 40.0),
                            text = "already here",
                        ),
                    ),
                ),
            ),
        )
        val encoded = serializer.serialize(doc)
        assertTrue(encoded.contains("\"align\":\"start\""), encoded)

        val legacyWithoutAlign = encoded.replace("\"align\":\"start\",", "")
        assertTrue(!legacyWithoutAlign.contains("\"align\""), legacyWithoutAlign)
        val decoded = serializer.deserialize(legacyWithoutAlign)
        val text = decoded.pages.single().elements.single() as TextElement

        assertEquals(TextAlign.START, text.style.align)
    }

    @Test
    fun `serialize always writes the current schema version`() {
        val stale = sample(schemaVersion = 0)
        val decoded = serializer.deserialize(serializer.serialize(stale))
        assertEquals(CURRENT_SCHEMA_VERSION, decoded.schemaVersion)
    }

    @Test
    fun `deserialize tolerates unknown future keys`() {
        val withFutureKey = serializer.serialize(sample()).replaceFirst("{", "{\"futureOnlyField\":42,")
        val decoded = serializer.deserialize(withFutureKey)
        assertEquals(sample(), decoded)
    }

    @Test
    fun `deserialize runs migration before decoding`() {
        // Synthetic migrator that renames a legacy doc-level key the current model does not have;
        // proves the JSON passes through migration (and version-stamping) before decode.
        val migrator = object : DocumentMigrator {
            override val fromVersion = 1
            override val toVersion = 2
            override fun migrate(input: JsonObject): JsonObject = buildJsonObject {
                input.forEach { (k, v) -> put(k, v) }
                put("migratedMarker", true)
            }
        }
        val migrating = JsonDocumentSerializer(DocumentMigrations(listOf(migrator), targetVersion = 2))
        // Hand-rewrite the stamped version rather than asking `serialize` for a v1 payload: since the
        // Versioned bumps (ADR-105/113) mean `serialize` always normalises to CURRENT_SCHEMA_VERSION,
        // so a v1 request would come back current and this migrator would never run — the assertion would
        // still pass and
        // prove nothing.
        // ADR-147: an undescribed sample is written as v3, so this needs a described one to be stamped current (4).
        val described = sample().copy(
            pages = sample().pages.map { page ->
                page.copy(elements = page.elements.map { (it as ImageElement).copy(description = "A photo") })
            },
        )
        val v1Json = serializer.serialize(described)
            .replace("\"schemaVersion\":$CURRENT_SCHEMA_VERSION", "\"schemaVersion\":1")
        val decoded = migrating.deserialize(v1Json)
        assertEquals(2, decoded.schemaVersion)
    }

    @Test
    fun `malformed json fails with a typed serialization error`() {
        assertThrows<DocumentSerializationException> { serializer.deserialize("{ this is not json") }
    }

    @Test
    fun `a document missing schemaVersion is rejected`() {
        assertThrows<MissingSchemaVersionException> {
            serializer.deserialize("{\"format\":\"single_sheet_8\",\"paperSize\":\"letter\",\"pages\":[]}")
        }
    }

    @Test
    fun `serialized output is self-describing with schemaVersion and discriminators`() {
        val text = serializer.serialize(sample())
        assertTrue(text.contains("\"schemaVersion\""), text)
        assertTrue(text.contains("\"type\":\"image\""), text)
    }

    @Test
    fun `the json serializer reports the json persisted format`() {
        assertEquals(PersistedFormat.JSON, serializer.format)
    }

    @Test
    fun `serialized payload carries an explicit format marker`() {
        val text = serializer.serialize(sample())
        assertTrue(text.contains("\"_encoding\":\"json\""), text)
    }

    @Test
    fun `a structural rename migration preserves the value through decode`() {
        // Legacy v1 stored the paper under "paper"; v2 renames it to "paperSize". The value must
        // survive into the typed model — this is why JSON-tree migration beats tolerant-decode fixup.
        val rename = object : DocumentMigrator {
            override val fromVersion = 1
            override val toVersion = 2
            override fun migrate(input: JsonObject): JsonObject = buildJsonObject {
                input.forEach { (k, v) -> if (k != "paper") put(k, v) }
                input["paper"]?.let { put("paperSize", it) }
            }
        }
        val migrating = JsonDocumentSerializer(DocumentMigrations(listOf(rename), targetVersion = 2))
        val legacy = "{\"schemaVersion\":1,\"format\":\"single_sheet_8\",\"paper\":\"a4\"}"
        val decoded = migrating.deserialize(legacy)
        assertEquals(PaperSize.A4, decoded.paperSize)
        assertEquals(2, decoded.schemaVersion)
    }

    @Test
    fun `a document newer than the current schema is refused`() {
        val newer = "{\"schemaVersion\":${CURRENT_SCHEMA_VERSION + 1},\"format\":\"single_sheet_8\",\"paperSize\":\"a4\"}"
        assertThrows<NewerSchemaVersionException> { serializer.deserialize(newer) }
    }

    @Test
    fun `the stamped json marker round-trips through deserialize`() {
        // Proves the marker the serializer writes is the one it accepts on read.
        val doc = sample()
        assertEquals(doc, serializer.deserialize(serializer.serialize(doc)))
    }

    @Test
    fun `a payload declaring a non-json persisted format is refused`() {
        val foreign = "{\"_encoding\":\"protobuf\",\"schemaVersion\":1,\"format\":\"single_sheet_8\",\"paperSize\":\"a4\"}"
        val ex = assertThrows<UnsupportedFormatException> { serializer.deserialize(foreign) }
        assertEquals(PersistedFormat.JSON.wire, ex.expected)
    }

    @Test
    fun `a non-string encoding marker is refused`() {
        val bad = "{\"_encoding\":42,\"schemaVersion\":1,\"format\":\"single_sheet_8\",\"paperSize\":\"a4\"}"
        assertThrows<UnsupportedFormatException> { serializer.deserialize(bad) }
    }

    // #155: Add > Text places a box with text "" and autosaves it while the maker is still typing, so a repair
    // to that `.bak` (or a kill mid-session) used to reopen an invisible "Empty text" element.
    private fun withTexts(vararg texts: String): ZineDocument = sample().copy(
        pages = listOf(
            Page(
                index = 0,
                role = PageRole.INTERIOR,
                elements = texts.mapIndexed { i, body ->
                    TextElement(id = "t-$i", transform = Transform(0.0, 0.0, 10.0, 10.0), zIndex = i, text = body)
                },
            ),
        ),
    )

    @Test
    fun `an empty text box is never written`() {
        val written = serializer.serialize(withTexts("kept", "", "also kept"))

        assertTrue("\"t-1\"" !in written, written)
        val kept = withTexts("kept", "", "also kept").pages[0].elements.filter { it.id != "t-1" }
        assertEquals(kept, serializer.deserialize(written).pages.single().elements)
    }

    @Test
    fun `an empty text box already on disk is dropped on load`() {
        // A file an older build wrote: emptied by hand, since this writer no longer can.
        val onDisk = serializer.serialize(withTexts("@EMPTY@", "hi")).replace("\"@EMPTY@\"", "\"\"")
        assertTrue("\"text\":\"\"" in onDisk, onDisk)

        val ids = serializer.deserialize(onDisk).pages.single().elements.map { it.id }

        assertEquals(listOf("t-1"), ids)
    }

    @Test
    fun `whitespace-only text is not empty and survives both ways`() {
        // Only "" is the in-session placeholder. No current build commits whitespace (Done on it deletes the box),
        // so a whitespace-only text on disk came from somewhere that meant it; it is not ours to delete.
        val doc = withTexts(" ", "\n")
        assertEquals(doc, serializer.deserialize(serializer.serialize(doc)))
    }

    @Test
    fun `a legacy payload without an encoding marker is accepted as implicit json`() {
        val legacy = "{\"schemaVersion\":1,\"format\":\"single_sheet_8\",\"paperSize\":\"a4\",\"pages\":[]}"
        val decoded = serializer.deserialize(legacy)
        assertEquals(PaperSize.A4, decoded.paperSize)
    }
}
