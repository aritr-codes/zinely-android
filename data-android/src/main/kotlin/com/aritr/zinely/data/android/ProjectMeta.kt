package com.aritr.zinely.data.android

import com.aritr.zinely.core.data.repository.LenientFolderNameSerializer
import kotlinx.serialization.Serializable

/**
 * The per-project `projects/<id>/meta.json` sidecar — the **source of truth** for the shelf-only
 * metadata that is *not* derivable from `document.json` (ADR-042: title, createdAt). Written
 * atomically via [com.aritr.zinely.core.data.storage.AtomicFileStore], so it inherits the `.bak`
 * recovery semantics. The Room `projects` table only *indexes* this file; if they disagree, the
 * file wins. A **missing** sidecar is backfilled with fallbacks; a **present-but-unreadable** one
 * is never overwritten (it is the only copy of this metadata — bytes are left for repair).
 */
@Serializable
internal data class ProjectMeta(
    val title: String,
    val createdAtEpochMs: Long,
    /**
     * The persisted cover ([D-017](docs/design/V2-SPEC-DEFECTS.md#d-017-ruling)), stored as the enum
     * **names** rather than the types themselves so the sidecar stays a plain, forward-readable JSON
     * document — the same choice the Room index makes for `format`/`paperSize`.
     *
     * Both default to `null` and that default is load-bearing twice over: it is what lets an existing
     * `meta.json` written before B5 decode without error, and it is the signal that a project is
     * **legacy** and owed a cover on first presentation. An unrecognised name decodes to a non-null
     * string that maps to `null` — treated exactly like a missing one, so a future rename of an enum
     * constant degrades to a re-draw rather than to a crash.
     */
    val coverSurface: String? = null,
    val coverStamp: String? = null,
    /**
     * The Shelf folder this zine is in, or `null` for My Shelf ([ADR-125](docs/DECISIONS.md#adr-125) rule 12).
     * A folder is not a thing on disk; it is this name, carried by each of its zines.
     *
     * Invariant: **every writer of this file carries the field across**, because each rewrite is wholesale
     * and a field left out is destroyed. The `null` default is not encoded, so a zine that was never put in a
     * folder has a byte-identical `meta.json` to one written before the field existed. Read leniently (a
     * value that is not a string is no folder, and the rest of the file still reads) and always passed
     * through `FolderNames.clean` before use.
     */
    @Serializable(with = LenientFolderNameSerializer::class)
    val folder: String? = null,
)
