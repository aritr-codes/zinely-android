package com.aritr.zinely.data.android.room

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The app-private, local-only Room database (ADR-042). Holds nothing but the rebuildable `projects`
 * index — the ADR-022 asset index table is deferred with the GC. Open-time corruption is handled by
 * SQLite's default error handler (drops the DB file), after which the reconcile scan rebuilds every
 * row from the per-project files; `exportSchema = true` with the schema dir checked in per ADR-042.
 */
@Database(entities = [ProjectEntity::class], version = 1, exportSchema = true)
internal abstract class ZinelyDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
}
