package com.aritr.zinely.data.android.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row of the `projects` metadata index (ADR-042). This is a **rebuildable index** over the
 * per-project files — `document.json` (content, format, paperSize, schemaVersion; ADR-003) and
 * `meta.json` (title, createdAt) — never an authority: every row is reconstructible by the
 * reconcile scan, and file truth always wins. Enums are stored by name; timestamps are epoch ms.
 * `updatedAtEpochMs` records the last *metadata operation* — display recency additionally folds in
 * the document file's mtime at read time (see `RoomProjectRepository`).
 */
@Entity(tableName = "projects", indices = [Index("updatedAtEpochMs")])
internal data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val format: String,
    val paperSize: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val documentSchemaVersion: Int,
)
