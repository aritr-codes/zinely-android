package com.aritr.zinely.data.android.prefs

import kotlinx.coroutines.flow.Flow

/** The last whole-library backup this phone saved: when, and the file name if the provider reported one. */
public data class BackupRecord(
    val savedAtEpochMs: Long,
    val fileName: String?,
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
