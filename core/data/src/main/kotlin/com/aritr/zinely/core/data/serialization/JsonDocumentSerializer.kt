package com.aritr.zinely.core.data.serialization

import com.aritr.zinely.core.model.CURRENT_SCHEMA_VERSION
import com.aritr.zinely.core.model.ZineDocument
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * `kotlinx.serialization` JSON implementation of [DocumentSerializer] ([ADR-020]).
 *
 * - **discriminator** `type` for the sealed `Element`/`Background` trees;
 * - **`ignoreUnknownKeys`** so a document with extra additive keys still loads (forward tolerance,
 *   S2 spike §3/§6);
 * - **`encodeDefaults`** so `schemaVersion` and defaulted fields are always present on disk;
 * - a top-level **format marker** (`_encoding`) is stamped on write so the persisted payload is
 *   self-identifying — format detection is explicit, never heuristic (Codex review 2026-06-19);
 * - decode runs JSON-tree [DocumentMigrations] (internal) **before** typed decoding, so an older
 *   document becomes a current-shaped [ZineDocument], and a **newer** document is refused.
 *
 * Pure and Android-free: text in, text out. File I/O, atomic rename, and the `.bak`/recovery
 * contract ([ADR-021]) live in the S2B file data source, not here.
 */
public class JsonDocumentSerializer internal constructor(
    private val migrations: DocumentMigrations,
) : DocumentSerializer {

    /** Wire the real (currently empty) migrator registry; this is the app-facing constructor. */
    public constructor() : this(DocumentMigrations(MIGRATORS))

    override val format: PersistedFormat = PersistedFormat.JSON

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
        val body = json.encodeToJsonElement(ZineDocument.serializer(), normalized).jsonObject
        val stamped = buildJsonObject {
            put(ENCODING_KEY, format.wire)
            body.forEach { (key, value) -> put(key, value) }
        }
        return json.encodeToString(JsonObject.serializer(), stamped)
    }

    override fun deserialize(text: String): ZineDocument {
        val root = parse(text)
        checkFormat(root)
        return decode(migrations.migrate(root))
    }

    /**
     * Enforce the format marker before version migration. A missing marker is accepted as legacy
     * implicit-JSON (documents written before the marker existed, or hand-authored); a marker that
     * names any other format — or is not a string — is refused. This is what makes format detection
     * serializer-owned and non-heuristic rather than a cosmetic field ([ADR-020] amendment).
     */
    private fun checkFormat(root: JsonObject) {
        val marker = root[ENCODING_KEY] ?: return
        val wire = (marker as? JsonPrimitive)?.takeIf { it.isString }?.content
        if (wire != format.wire) {
            throw UnsupportedFormatException(found = marker.toString(), expected = format.wire)
        }
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

    private companion object {
        /** Top-level key carrying the [PersistedFormat] marker on disk. */
        const val ENCODING_KEY = "_encoding"

        /**
         * The ordered JSON-tree migrator chain. Empty at schema v1 (no history yet). When the first
         * structural `v1 -> v2` change lands, add the migrator here and a golden fixture test; if a
         * non-JSON format is ever adopted, migration moves to per-version typed snapshots ([ADR-020]).
         */
        val MIGRATORS: List<DocumentMigrator> = emptyList()
    }
}
