package com.aritr.zinely.data.android

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
)
