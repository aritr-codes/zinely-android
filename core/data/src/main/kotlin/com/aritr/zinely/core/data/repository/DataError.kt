package com.aritr.zinely.core.data.repository

import com.aritr.zinely.core.data.validation.ValidationIssue

/**
 * The domain error taxonomy a repository maps platform failures to (ARCHITECTURE §9). Deliberately
 * **offline-only**: there is no `Network` case — the privacy invariant forbids networking ([PRD §5]).
 * The cause (when present) is the original platform exception, kept for logging but never surfaced
 * raw to UI.
 */
public sealed interface DataError {
    /** No project/document exists for [id]. */
    public data class NotFound(val id: String) : DataError

    /** A persisted file/database read or write failed (IO, SQLite). */
    public data class Io(val message: String, val cause: Throwable? = null) : DataError

    /** A stored document could not be decoded — malformed/corrupt bytes. */
    public data class Corrupt(val message: String, val cause: Throwable? = null) : DataError

    /** A document failed validation; carries the structured issues. */
    public data class Invalid(val issues: List<ValidationIssue>) : DataError

    /** The stored document is newer than this build supports (forward-incompatible). */
    public data class SchemaTooNew(val documentVersion: Int, val supportedVersion: Int) : DataError

    /** An unclassified failure; [cause] retained for diagnostics. */
    public data class Unknown(val message: String, val cause: Throwable? = null) : DataError
}
