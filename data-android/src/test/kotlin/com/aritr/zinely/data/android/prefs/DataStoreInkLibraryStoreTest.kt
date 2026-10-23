package com.aritr.zinely.data.android.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aritr.zinely.core.editor.ink.InkHex
import com.aritr.zinely.core.editor.ink.InkLibrary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataStoreInkLibraryStoreTest {

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

    private val key = stringPreferencesKey("ink_library")
    private val teal = InkHex("2A9D8F")

    private fun storedWith(json: String) = FakeDataStore(mutablePreferencesOf(key to json))

    @Test
    fun given_empty_store_when_read_then_empty() = runTest {
        assertEquals(InkLibrary.EMPTY, DataStoreInkLibraryStore(FakeDataStore()).library.first())
    }

    @Test
    fun given_use_when_read_then_contains_hex_and_edit_reports_before_and_after() = runTest {
        val store = DataStoreInkLibraryStore(FakeDataStore())

        val edit = store.update { it.use(teal).library }

        assertEquals(InkLibrary.EMPTY, edit.before)
        assertNotNull(edit.after.kept(teal))
        assertTrue(edit.changed)
        assertEquals(edit.after, store.library.first())
    }

    @Test
    fun given_garbage_json_when_read_then_empty_and_next_update_writes_valid_v1() = runTest {
        val fake = storedWith("{not json")
        val store = DataStoreInkLibraryStore(fake)

        assertEquals(InkLibrary.EMPTY, store.library.first())
        store.update { it.use(teal).library }

        assertTrue(fake.data.first()[key]!!.contains("\"v\":1"))
        assertNotNull(store.library.first().kept(teal))
    }

    @Test
    fun given_newer_version_when_read_then_empty_and_update_leaves_raw_value_untouched() = runTest {
        val raw = """{"v":2,"inks":[{"hex":"2A9D8F","used":1}],"palettes":[],"clock":1,"nextId":1,"open":null,"future":true}"""
        val fake = storedWith(raw)
        val store = DataStoreInkLibraryStore(fake)

        assertEquals(InkLibrary.EMPTY, store.library.first())
        val edit = store.update { it.use(teal).library }

        assertFalse(edit.changed)
        assertEquals(InkLibrary.EMPTY, edit.after)
        assertEquals(raw, fake.data.first()[key])
        assertEquals(0, fake.writes)
    }

    @Test
    fun given_no_op_transform_when_update_then_unchanged_and_no_write() = runTest {
        val fake = FakeDataStore()
        val edit = DataStoreInkLibraryStore(fake).update { it }

        assertFalse(edit.changed)
        assertEquals(0, fake.writes)
    }

    @Test
    fun given_twelve_stored_inks_when_read_then_nine() = runTest {
        val inks = (1..12).joinToString(",") { """{"hex":"%06X","used":$it}""".format(it * 1000) }
        val store = DataStoreInkLibraryStore(
            storedWith("""{"v":1,"inks":[$inks],"palettes":[],"clock":12,"nextId":1,"open":null}"""),
        )

        assertEquals(9, store.library.first().myInks.size)
    }

    @Test
    fun given_a_write_then_raw_value_has_v1_under_stable_key() = runTest {
        val fake = FakeDataStore()
        DataStoreInkLibraryStore(fake).update { it.use(teal).library }

        val raw = fake.data.first()[stringPreferencesKey("ink_library")]!!
        assertTrue(raw, raw.contains("\"v\":1"))
    }

    @Test
    fun given_two_sequential_updates_then_second_before_is_first_after() = runTest {
        val store = DataStoreInkLibraryStore(FakeDataStore())

        val first = store.update { it.use(teal).library }
        val second = store.update { it.use(InkHex("E76F51")).library }

        assertEquals(first.after, second.before)
        assertEquals(2, second.after.myInks.size)
    }

    @Test
    fun given_palette_and_open_id_when_round_trip_then_equal() = runTest {
        val store = DataStoreInkLibraryStore(FakeDataStore())
        val edit = store.update {
            (it.addPalette("Sea", listOf(teal, InkHex("E76F51"))) as com.aritr.zinely.core.editor.ink.PaletteResult.Done).library
        }

        assertEquals(edit.after, store.library.first())
    }

    @Test
    fun given_one_bad_hex_among_good_when_read_then_only_that_ink_is_dropped() = runTest {
        val store = DataStoreInkLibraryStore(
            storedWith(
                """{"v":1,"inks":[{"hex":"2a9d8f","used":1},{"hex":"E76F51","used":2}],"palettes":[],"clock":2,"nextId":1,"open":null}""",
            ),
        )

        assertEquals(listOf(InkHex("E76F51")), store.library.first().myInks.map { it.hex })
    }

    @Test
    fun given_bad_hex_in_one_palette_when_read_then_other_palette_survives() = runTest {
        val store = DataStoreInkLibraryStore(
            storedWith(
                """{"v":1,"inks":[],"palettes":[{"id":1,"name":"Bad","inks":["zzzzzz","E76F51"]},{"id":2,"name":"Good","inks":["2A9D8F","E76F51"]}],"clock":0,"nextId":3,"open":null}""",
            ),
        )

        assertEquals(listOf("Good"), store.library.first().palettes.map { it.name })
    }

    @Test
    fun given_unreadable_or_non_current_version_when_update_then_raw_value_untouched() = runTest {
        for (v in listOf("0", "-1", "\"2\"", "2.5", "null")) {
            val raw = """{"v":$v,"inks":[{"hex":"2A9D8F","used":1}],"palettes":[],"clock":1,"nextId":1,"open":null}"""
            val fake = storedWith(raw)

            val edit = DataStoreInkLibraryStore(fake).update { it.use(teal).library }

            assertFalse(v, edit.changed)
            assertEquals(v, raw, fake.data.first()[key])
        }
    }
}
