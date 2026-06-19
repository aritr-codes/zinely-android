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
}
