package com.aritr.zinely.data.android.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The app-private, local-only Room database (ADR-042). Holds nothing but the rebuildable `projects`
 * index — the ADR-022 asset index table is deferred with the GC. Open-time corruption is handled by
 * SQLite's default error handler (drops the DB file), after which the reconcile scan rebuilds every
 * row from the per-project files; `exportSchema = true` with the schema dir checked in per ADR-042.
 */
@Database(entities = [ProjectEntity::class], version = 2, exportSchema = true)
internal abstract class ZinelyDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
}

/**
 * v1 → v2 (B5): the persisted cover columns ([D-017](docs/design/V2-SPEC-DEFECTS.md#d-017-ruling)).
 *
 * **Additive and non-destructive**, and deliberately not `fallbackToDestructiveMigration()`. The index
 * *is* rebuildable (ADR-042) and dropping it would be recoverable, so destruction was a real candidate
 * — it is rejected because it would make this and every later schema change silently destructive, and
 * because a dropped table forces a full reconcile scan of every project on the next launch for what is
 * a two-column addition.
 *
 * Both columns are added `NULL`, which is exactly right: every row that exists at migration time
 * predates the field, and `NULL` is precisely what marks a project **legacy** and owed a cover on first
 * presentation ([D-026](docs/design/V2-SPEC-DEFECTS.md#d-026-ruling)). The migration therefore stores no
 * covers itself — assignment is the repository's job, once, at the moment the zine is first presented,
 * so that the assignment and its persistence happen in the same place for legacy and new projects alike.
 */
internal val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE projects ADD COLUMN coverSurface TEXT")
        db.execSQL("ALTER TABLE projects ADD COLUMN coverStamp TEXT")
    }
}
