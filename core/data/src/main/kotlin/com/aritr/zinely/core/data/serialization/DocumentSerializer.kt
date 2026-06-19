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
 * The single boundary that turns a [ZineDocument] into persisted text and back ([ADR-020]). Every
 * (de)serialization in the app routes through this interface, so the wire format stays swappable
 * (JSON now; Protobuf is the future escape hatch behind the same contract — S2 spike §3, O2).
 */
public interface DocumentSerializer {
    /** Encode [document] as text at the **current** schema version. */
    public fun serialize(document: ZineDocument): String

    /**
     * Decode [text] into a current-shaped [ZineDocument]: migrate older versions first, tolerate
     * unknown (newer) keys. Throws a [DocumentSerializationException] on any failure.
     */
    public fun deserialize(text: String): ZineDocument
}
