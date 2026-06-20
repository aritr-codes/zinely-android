package com.aritr.zinely.core.data.serialization

import com.aritr.zinely.core.model.CURRENT_SCHEMA_VERSION
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.Page
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
        val v1Json = serializer.serialize(sample(schemaVersion = 1))
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

    @Test
    fun `a legacy payload without an encoding marker is accepted as implicit json`() {
        val legacy = "{\"schemaVersion\":1,\"format\":\"single_sheet_8\",\"paperSize\":\"a4\",\"pages\":[]}"
        val decoded = serializer.deserialize(legacy)
        assertEquals(PaperSize.A4, decoded.paperSize)
    }
}
