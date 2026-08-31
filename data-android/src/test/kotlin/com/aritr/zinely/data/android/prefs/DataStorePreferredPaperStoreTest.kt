package com.aritr.zinely.data.android.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aritr.zinely.core.model.PaperSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class DataStorePreferredPaperStoreTest {

    private class FakeDataStore(
        initial: Preferences = emptyPreferences(),
    ) : DataStore<Preferences> {
        private val state = MutableStateFlow(initial)
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

    @Test
    fun missing_preference_defaults_to_a4() = runTest {
        val store = DataStorePreferredPaperStore(FakeDataStore())

        assertEquals(PaperSize.A4, store.preferredPaperSize.first())
    }

    @Test
    fun chosen_paper_round_trips_and_can_be_changed() = runTest {
        val store = DataStorePreferredPaperStore(FakeDataStore())

        store.setPreferredPaperSize(PaperSize.LETTER)
        assertEquals(PaperSize.LETTER, store.preferredPaperSize.first())

        store.setPreferredPaperSize(PaperSize.A4)
        assertEquals(PaperSize.A4, store.preferredPaperSize.first())
    }

    @Test
    fun persisted_values_use_stable_wire_names() = runTest {
        val fake = FakeDataStore()
        val store = DataStorePreferredPaperStore(fake)

        store.setPreferredPaperSize(PaperSize.LETTER)
        assertEquals("letter", fake.data.first()[stringPreferencesKey("preferred_paper_size")])

        store.setPreferredPaperSize(PaperSize.A4)
        assertEquals("a4", fake.data.first()[stringPreferencesKey("preferred_paper_size")])
    }

    @Test
    fun unknown_persisted_value_falls_back_to_a4() = runTest {
        val preferences = mutablePreferencesOf(stringPreferencesKey("preferred_paper_size") to "legal")
        val store = DataStorePreferredPaperStore(FakeDataStore(preferences))

        assertEquals(PaperSize.A4, store.preferredPaperSize.first())
    }

    @Test
    fun io_read_failure_falls_back_to_a4() = runTest {
        val failing = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException("corrupt") }
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                throw UnsupportedOperationException("not needed")
        }
        val store = DataStorePreferredPaperStore(failing)

        assertEquals(PaperSize.A4, store.preferredPaperSize.first())
    }

    @Test
    fun non_io_read_failure_is_not_hidden() = runTest {
        val failing = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IllegalStateException("bug") }
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                throw UnsupportedOperationException("not needed")
        }
        val store = DataStorePreferredPaperStore(failing)

        try {
            store.preferredPaperSize.first()
            throw AssertionError("expected the non-IO error to propagate")
        } catch (expected: IllegalStateException) {
            assertEquals("bug", expected.message)
        }
    }
}
