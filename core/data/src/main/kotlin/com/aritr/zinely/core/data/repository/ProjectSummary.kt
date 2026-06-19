package com.aritr.zinely.core.data.repository

import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineFormat

/**
 * Queryable project metadata for the project list — the **derived cache** view (Room-backed in S2B),
 * never the source of truth (the document is). Timestamps are epoch milliseconds; the pure core does
 * not read a clock, so callers supply them ([ADR-003], S2 spike §2).
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
