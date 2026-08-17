package com.aritr.zinely.core.data.repository

import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineCoverRecipe
import com.aritr.zinely.core.model.ZineFormat

/**
 * Queryable project metadata for the project list — the **derived index** view (Room-backed,
 * [ADR-042]), never an authority: the source of truth is the per-project **files** — `document.json`
 * for format/paperSize/schemaVersion ([ADR-003]) and the `meta.json` sidecar for title/createdAt
 * (shelf-only metadata the document cannot carry). On disagreement, files win and the index is
 * rebuilt. Timestamps are epoch milliseconds; the pure core does not read a clock, so callers
 * supply them (S2 spike §2). `updatedAtEpochMs` is display recency: max(last metadata operation,
 * document file mtime) at read time ([ADR-042] §7).
 */
public data class ProjectSummary(
    val id: String,
    val title: String,
    val format: ZineFormat,
    val paperSize: PaperSize,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val documentSchemaVersion: Int,
    /**
     * The zine's persisted cover — its **visual identity**, assigned once and never re-derived
     * ([D-017](docs/design/V2-SPEC-DEFECTS.md#d-017-ruling)). Like every other field here this is the
     * *index* view; `meta.json` is the authority.
     *
     * `null` means **not yet assigned**, which is only true of a project that predates the field — one
     * created before it existed, or one adopted from disk by the reconcile scan. Such a project receives
     * a cover on first presentation and persists it immediately
     * ([D-026](docs/design/V2-SPEC-DEFECTS.md#d-026-ruling)), after which it is indistinguishable from
     * one assigned at creation. It therefore stays `null` across reads in exactly one circumstance: the
     * sidecar that would hold the assignment could not be written (or is present but unreadable, and so
     * must not be overwritten). That is a **degraded state, not a lie** — the alternative, returning a
     * cover the disk does not carry, is repainted on the next index rebuild.
     */
    val cover: ZineCoverRecipe? = null,
)
