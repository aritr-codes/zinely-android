package com.aritr.zinely.core.data.serialization

import com.aritr.zinely.core.model.CURRENT_SCHEMA_VERSION
import com.aritr.zinely.core.model.ZineDocument
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * `kotlinx.serialization` JSON implementation of [DocumentSerializer] ([ADR-020]).
 *
 * - **discriminator** `type` for the sealed `Element`/`Background` trees;
 * - **`ignoreUnknownKeys`** so a document written by a newer app version still loads (forward
 *   tolerance, S2 spike §3/§6);
 * - **`encodeDefaults`** so `schemaVersion` and defaulted fields are always present on disk;
 * - decode runs [DocumentMigrations] **before** typed decoding, so an older document becomes a
 *   current-shaped [ZineDocument].
 *
 * Pure and Android-free: it takes text in and gives text out. File I/O, atomic rename, and the
 * `.bak`/recovery contract ([ADR-021]) live in the S2B file data source, not here.
 */
public class JsonDocumentSerializer(
    private val migrations: DocumentMigrations = DocumentMigrations(emptyList()),
) : DocumentSerializer {

    private val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    override fun serialize(document: ZineDocument): String {
        val normalized =
            if (document.schemaVersion == CURRENT_SCHEMA_VERSION) document
            else document.copy(schemaVersion = CURRENT_SCHEMA_VERSION)
        return json.encodeToString(ZineDocument.serializer(), normalized)
    }

    override fun deserialize(text: String): ZineDocument {
        val migrated = migrations.migrate(parse(text))
        return decode(migrated)
    }

    private fun parse(text: String): JsonObject {
        val element = try {
            json.parseToJsonElement(text)
        } catch (e: SerializationException) {
            throw DocumentDecodeException("Document is not valid JSON", e)
        }
        return element as? JsonObject
            ?: throw DocumentDecodeException("Document root is not a JSON object")
    }

    private fun decode(obj: JsonObject): ZineDocument =
        try {
            json.decodeFromJsonElement(ZineDocument.serializer(), obj)
        } catch (e: SerializationException) {
            throw DocumentDecodeException("Document JSON does not match the current schema", e)
        } catch (e: IllegalArgumentException) {
            throw DocumentDecodeException("Document JSON does not match the current schema", e)
        }
}
