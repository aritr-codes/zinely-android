package com.aritr.zinely.data.android.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aritr.zinely.core.editor.ART_FAVOURITES_CAP
import com.aritr.zinely.core.editor.ArtShortcuts
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataStoreArtShortcutsStoreTest {

    private class FakeDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
        private val state = MutableStateFlow(initial)
        var writes = 0
            private set
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            if (updated != state.value) writes++
            state.value = updated
            return updated
        }
    }

    private val key = stringPreferencesKey("art_shortcuts")
    private fun storedWith(json: String) = FakeDataStore(mutablePreferencesOf(key to json))

    @Test
    fun given_empty_store_when_read_then_empty() = runTest {
        assertEquals(ArtShortcuts(), DataStoreArtShortcutsStore(FakeDataStore()).shortcuts.first())
    }

    @Test
    fun given_a_pick_and_a_star_when_a_new_store_reads_the_same_file_then_they_are_back() = runTest {
        val fake = FakeDataStore()
        DataStoreArtShortcutsStore(fake).update { it.picked("shape.circle").favourite("shape.circle", true) }

        // A second adapter over the same file stands for the next process.
        val after = DataStoreArtShortcutsStore(fake).shortcuts.first()

        assertEquals(listOf("shape.circle"), after.recent)
        assertEquals(listOf("shape.circle"), after.favourites)
        assertTrue(fake.data.first()[key]!!.contains("\"v\":1"))
    }

    @Test
    fun given_oversize_stored_lists_when_read_then_bounded() = runTest {
        val recent = (1..20).joinToString(",") { "\"r$it\"" }
        val favs = (1..100).joinToString(",") { "\"f$it\"" }
        val read = DataStoreArtShortcutsStore(storedWith("""{"v":1,"recent":[$recent],"favourites":[$favs]}""")).shortcuts.first()

        assertEquals(6, read.recent.size)
        assertEquals(ART_FAVOURITES_CAP, read.favourites.size)
    }

    @Test
    fun given_garbage_when_read_then_empty_and_next_update_writes_valid_v1() = runTest {
        val fake = storedWith("{not json")
        val store = DataStoreArtShortcutsStore(fake)

        assertEquals(ArtShortcuts(), store.shortcuts.first())
        store.update { it.picked("a") }

        assertEquals(listOf("a"), store.shortcuts.first().recent)
    }

    @Test
    fun given_newer_version_when_update_then_raw_value_untouched() = runTest {
        val raw = """{"v":2,"recent":["a"],"favourites":[],"future":true}"""
        val fake = storedWith(raw)
        val store = DataStoreArtShortcutsStore(fake)

        assertEquals(ArtShortcuts(), store.shortcuts.first())
        store.update { it.picked("b") }

        assertEquals(raw, fake.data.first()[key])
        assertEquals(0, fake.writes)
    }

    @Test
    fun given_no_op_transform_then_no_write() = runTest {
        val fake = FakeDataStore()
        DataStoreArtShortcutsStore(fake).update { it }
        assertEquals(0, fake.writes)
    }
}
