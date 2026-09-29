package com.aritr.zinely.data.android.prefs

import kotlinx.coroutines.flow.Flow

/**
 * The last whole-library backup this phone saved: when, the file name if the provider reported one, and how many of
 * how many zines it holds (ADR-122 §4; counts only — names would go stale). `null` counts are a record from before
 * step 1b, read as complete. Partial ⇔ [savedCount] < [totalCount].
 */
public data class BackupRecord(
    val savedAtEpochMs: Long,
    val fileName: String?,
    val savedCount: Int? = null,
    val totalCount: Int? = null,
)

/**
 * Device state, never document or archive content (Brief 01 "Data model implications", ADR-120): a
 * restored backup must not claim another phone's backup time. Written only when a backup is saved;
 * failure, cancel and restore leave it alone.
 */
public interface BackupRecordStore {
    /** The last saved backup, or null when none was ever saved (or the record can't be read). */
    public val lastBackup: Flow<BackupRecord?>

    public suspend fun recordBackup(record: BackupRecord)
}
