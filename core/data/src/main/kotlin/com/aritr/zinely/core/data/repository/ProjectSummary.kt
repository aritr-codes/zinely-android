package com.aritr.zinely.core.data.repository

import com.aritr.zinely.core.model.PaperSize
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
)
