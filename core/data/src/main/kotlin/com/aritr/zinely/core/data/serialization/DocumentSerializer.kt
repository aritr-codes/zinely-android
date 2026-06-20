package com.aritr.zinely.core.data.serialization

import com.aritr.zinely.core.model.ZineDocument

/**
 * Base type for any failure crossing the document (de)serialization boundary. Repositories catch
 * this one family and map it to a `DataError` (S2 spike §1); no raw `kotlinx` exception escapes.
 */
public sealed class DocumentSerializationException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/** The input was not a decodable document: malformed JSON, wrong root shape, or bad enum/field. */
public class DocumentDecodeException(message: String, cause: Throwable? = null) :
    DocumentSerializationException(message, cause)

/**
 * The payload declares a persisted [PersistedFormat] that this serializer does not read. Format
 * detection is explicit and serializer-owned: a serializer refuses a payload whose marker is not its
 * own ([ADR-020] amendment), rather than guessing.
 */
public class UnsupportedFormatException(public val found: String, public val expected: String) :
    DocumentSerializationException("Document declares persisted format $found; this serializer reads '$expected'")

/**
 * The **persisted wire format** of a document, distinct from the document's own `ZineFormat`. The
 * serializer stamps its format into the payload and is the sole owner of format/version detection,
 * so a future Protobuf format ([ADR-020] escape hatch) is selected by an explicit marker, never by
 * heuristics (Codex review 2026-06-19). Only JSON exists today.
 */
public enum class PersistedFormat(public val wire: String) {
    JSON("json"),
}

/**
 * The single boundary that turns a [ZineDocument] into persisted text and back ([ADR-020]). Every
 * (de)serialization in the app routes through this interface, so the wire format stays swappable
 * (JSON now; Protobuf is the future escape hatch behind the same contract — S2 spike §3, O2).
 *
 * The serializer **owns format and schema-version detection**: callers hand it a payload and get a
 * [ZineDocument] back, with no caller-side "is this JSON or Protobuf?" branching.
 */
public interface DocumentSerializer {
    /** The wire format this serializer reads and writes. */
    public val format: PersistedFormat

    /** Encode [document] as text at the **current** schema version, stamped with [format]. */
    public fun serialize(document: ZineDocument): String

    /**
     * Decode [text] into a current-shaped [ZineDocument]: migrate older versions up first, tolerate
     * unknown (additive) keys, and **refuse** a document newer than this build supports. Throws a
     * [DocumentSerializationException] on any failure.
     */
    public fun deserialize(text: String): ZineDocument
}
