package com.aritr.zinely.data.android.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Preferences DataStore adapter for [BackupRecordStore], on the app's one preferences file. */
public class DataStoreBackupRecordStore(
    private val dataStore: DataStore<Preferences>,
) : BackupRecordStore {

    override val lastBackup: Flow<BackupRecord?> =
        dataStore.data
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .map { preferences ->
                preferences[SAVED_AT]?.let { BackupRecord(it, preferences[FILE_NAME]) }
            }

    override suspend fun recordBackup(record: BackupRecord) {
        dataStore.edit { preferences ->
            preferences[SAVED_AT] = record.savedAtEpochMs
            // A later backup without a reported name must not inherit the previous file's name.
            if (record.fileName == null) preferences.remove(FILE_NAME) else preferences[FILE_NAME] = record.fileName
        }
    }

    private companion object {
        /** Stable keys; renaming one would forget the maker's last backup. */
        val SAVED_AT = longPreferencesKey("last_backup_saved_at_epoch_ms")
        val FILE_NAME = stringPreferencesKey("last_backup_file_name")
    }
}
