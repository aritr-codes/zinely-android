package com.aritr.zinely.data.android.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class DataStoreBackupRecordStoreTest {

    private class FakeDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            transform(state.value).also { state.value = it }
    }

    @Test
    fun `no backup ever saved reads as none`() = runTest {
        assertEquals(null, DataStoreBackupRecordStore(FakeDataStore()).lastBackup.first())
    }

    @Test
    fun `a saved backup round-trips with and without a file name`() = runTest {
        val store = DataStoreBackupRecordStore(FakeDataStore())

        store.recordBackup(BackupRecord(1_000L, "zinely-backup-2026-09-12.zine"))
        assertEquals(BackupRecord(1_000L, "zinely-backup-2026-09-12.zine"), store.lastBackup.first())

        // A later save the provider named nothing for must not inherit the older name.
        store.recordBackup(BackupRecord(2_000L, null))
        assertEquals(BackupRecord(2_000L, null), store.lastBackup.first())
    }

    @Test
    fun `a partial backup's counts round-trip, and a later complete one doesn't inherit them`() = runTest {
        val store = DataStoreBackupRecordStore(FakeDataStore())

        store.recordBackup(BackupRecord(1_000L, "a.zine", savedCount = 5, totalCount = 6))
        assertEquals(BackupRecord(1_000L, "a.zine", 5, 6), store.lastBackup.first())

        store.recordBackup(BackupRecord(2_000L, "b.zine"))
        assertEquals(BackupRecord(2_000L, "b.zine"), store.lastBackup.first())
    }

    @Test
    fun `an unreadable preferences file reads as none`() = runTest {
        val broken = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException("corrupt") }
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                error("unused")
        }
        assertEquals(null, DataStoreBackupRecordStore(broken).lastBackup.first())
    }
}
