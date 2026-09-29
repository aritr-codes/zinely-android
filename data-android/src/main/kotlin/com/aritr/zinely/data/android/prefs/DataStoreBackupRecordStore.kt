package com.aritr.zinely.data.android.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
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
                preferences[SAVED_AT]?.let {
                    BackupRecord(it, preferences[FILE_NAME], preferences[SAVED_COUNT], preferences[TOTAL_COUNT])
                }
            }

    override suspend fun recordBackup(record: BackupRecord) {
        dataStore.edit { preferences ->
            preferences[SAVED_AT] = record.savedAtEpochMs
            // A later backup must not inherit the previous one's name or counts.
            preferences.putOrRemove(FILE_NAME, record.fileName)
            preferences.putOrRemove(SAVED_COUNT, record.savedCount)
            preferences.putOrRemove(TOTAL_COUNT, record.totalCount)
        }
    }

    private fun <T> MutablePreferences.putOrRemove(key: Preferences.Key<T>, value: T?) {
        if (value == null) remove(key) else set(key, value)
    }

    private companion object {
        /** Stable keys; renaming one would forget the maker's last backup. */
        val SAVED_AT = longPreferencesKey("last_backup_saved_at_epoch_ms")
        val FILE_NAME = stringPreferencesKey("last_backup_file_name")
        val SAVED_COUNT = intPreferencesKey("last_backup_saved_count")
        val TOTAL_COUNT = intPreferencesKey("last_backup_total_count")
    }
}
