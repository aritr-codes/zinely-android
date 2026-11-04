package com.aritr.zinely.core.data.serialization

import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.Element
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.TextElement
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

    /** Wire the real contiguous migrator registry; this is the app-facing constructor. */
    public constructor() : this(DocumentMigrations(MIGRATORS))

    override val format: PersistedFormat = PersistedFormat.JSON

    private val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = true
        // ADR-147: a null (absent) description writes no key, so an undescribed zine is byte for byte a v3 file.
        // Every later field must be nullable and absent when unused, or its bump gives that property up.
        explicitNulls = false
        prettyPrint = false
    }

    override fun serialize(document: ZineDocument): String {
        // ADR-147 decision 3: a file says the OLDEST version that can read it, not the newest this build knows.
        // 3 for a plain zine (never lower), 4 when any photo or Art piece holds a description. Each later
        // bump adds its own line here when the document uses what it added.
        val stamp = if (holdsDescription(document)) DESCRIPTION_SCHEMA_VERSION else MIN_WRITTEN_SCHEMA_VERSION
        val written = withoutEmptyText(document)
        val normalized = if (written.schemaVersion == stamp) written else written.copy(schemaVersion = stamp)
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

    private fun holdsDescription(document: ZineDocument): Boolean =
        document.pages.any { page ->
            page.elements.any { (it as? ImageElement)?.description != null || (it as? DecorElement)?.description != null }
        }

    /**
     * Drops every [TextElement] whose text is exactly `""`, on write and on read (#155).
     *
     * **Invariant: an empty text box is never on disk.** `Add > Text` places a box with `text = ""` and opens its
     * edit session; the words arrive only on commit, and a session ending empty removes the box
     * (`EditorReducer.endTextSession`). The placement is an undo step, so it autosaves mid-session, and a `.bak`
     * repair or a kill then reopened an invisible "Empty text" element. Filtering on write keeps it off disk;
     * filtering on read heals files already written. Exactly `""`, not blank: no build commits whitespace either
     * (Done on it deletes the box), but whitespace on disk did not come from the placeholder, so it is kept.
     * Returns [document] itself when there is nothing to drop, so a clean zine writes the same bytes as before.
     */
    private fun withoutEmptyText(document: ZineDocument): ZineDocument {
        if (document.pages.none { page -> page.elements.any(::isEmptyText) }) return document
        return document.copy(pages = document.pages.map { page -> page.copy(elements = page.elements.filterNot(::isEmptyText)) })
    }

    private fun isEmptyText(element: Element): Boolean = element is TextElement && element.text.isEmpty()

    private fun decode(obj: JsonObject): ZineDocument =
        try {
            withoutEmptyText(json.decodeFromJsonElement(ZineDocument.serializer(), obj))
        } catch (e: SerializationException) {
            throw DocumentDecodeException("Document JSON does not match the current schema", e)
        } catch (e: IllegalArgumentException) {
            throw DocumentDecodeException("Document JSON does not match the current schema", e)
        }

    private companion object {
        /** Top-level key carrying the [PersistedFormat] marker on disk. */
        const val ENCODING_KEY = "_encoding"

        /** The lowest version the writer ever stamps (ADR-147): a plain zine is not written as v1 or v2. */
        const val MIN_WRITTEN_SCHEMA_VERSION = 3

        /** The version a document holding a photo or Art description is stamped (ADR-147). */
        const val DESCRIPTION_SCHEMA_VERSION = 4

        /**
         * The ordered JSON-tree migrator chain. If a non-JSON format is ever adopted, migration moves
         * to per-version typed snapshots ([ADR-020]).
         *
         * **v1 → v2 and v2 → v3 are identity steps, on purpose.** `DecorElement` (ADR-105) changes nothing about
         * an existing document — no field moved, no field was renamed, and no v1 document contains a
         * `decor` element, so there is genuinely nothing to transform. SUPPLIES-SPEC §2.1 concluded
         * from that that no version bump was needed; [D-029's 2026-08-16 ruling]
         * (../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-029-ruling-2026-08-16) Q5 overrules
         * it, because the bump is not about *migrating forward* — it is about failing *backward*
         * honestly, via `NewerSchemaVersionException`, when a build predating decor meets a document
         * carrying an unknown sealed discriminator it cannot parse at all.
         *
         * The migrator is required by mechanism, not by content: [DocumentMigrations] demands a
         * contiguous `vN -> vN+1` chain and throws [MissingMigratorException] on a gap, so without
         * this step every already-saved v1 zine would refuse to open. The v2→v3 step has the same
         * forward shape: new flip Booleans default false, but the version bump makes pre-flip builds
         * refuse a v3 document before a tolerant decode/save can erase active flags (ADR-113).
         */
        val MIGRATORS: List<DocumentMigrator> = listOf(
            IdentityMigrator(from = 1),
            IdentityMigrator(from = 2),
            // v3 -> v4 (ADR-147): `description` is nullable and absent in a v3 file, so there is nothing to do.
            IdentityMigrator(from = 3),
        )

        /** A structural no-op step, for a version bump that exists purely to gate old readers out. */
        private class IdentityMigrator(from: Int) : DocumentMigrator {
            override val fromVersion: Int = from
            override val toVersion: Int = from + 1
            // `DocumentMigrations` stamps `schemaVersion` itself after each step; migrators must not.
            override fun migrate(input: JsonObject): JsonObject = input
        }
    }
}
