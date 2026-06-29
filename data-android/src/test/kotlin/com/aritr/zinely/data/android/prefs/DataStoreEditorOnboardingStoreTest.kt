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

/**
 * The storage-boundary contract for [DataStoreEditorOnboardingStore] (ADR-032), driven against a fake
 * [DataStore] — pure JVM, no Android/Robolectric (DataStore's `data`/`updateData` is an interface, so the
 * adapter's mapping + read-robustness are unit-testable without a real preferences file). The real
 * file-backed store is exercised by the app graph at runtime; here we pin the adapter's own logic.
 */
class DataStoreEditorOnboardingStoreTest {

    /** Minimal in-memory [DataStore]: `edit{}` flows through [updateData] exactly as the real one. */
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
    fun an_untouched_store_reports_the_hint_unseen() = runTest {
        val store = DataStoreEditorOnboardingStore(FakeDataStore())
        assertEquals(false, store.moveResizeHintSeen.first())
    }

    @Test
    fun marking_seen_is_observed_as_true() = runTest {
        val fake = FakeDataStore()
        val store = DataStoreEditorOnboardingStore(fake)

        store.markMoveResizeHintSeen()

        assertEquals(true, store.moveResizeHintSeen.first())
    }

    @Test
    fun marking_seen_twice_stays_true_and_does_not_throw() = runTest {
        val store = DataStoreEditorOnboardingStore(FakeDataStore())

        store.markMoveResizeHintSeen()
        store.markMoveResizeHintSeen() // idempotent

        assertEquals(true, store.moveResizeHintSeen.first())
    }

    @Test
    fun a_failed_write_is_swallowed_rather_than_crashing() = runTest {
        // The VM launches the write fire-and-forget; an uncaught IOException would reach the default
        // handler and crash. A best-effort hint flag must degrade quietly (Codex RF2).
        val failingWrite = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = MutableStateFlow(emptyPreferences())
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                throw IOException("disk full")
        }
        val store = DataStoreEditorOnboardingStore(failingWrite)

        store.markMoveResizeHintSeen() // must not throw
    }

    @Test
    fun a_corrupt_read_degrades_to_unseen_rather_than_crashing() = runTest {
        // The canonical DataStore robustness path: an IOException reading the on-disk store recovers to
        // empty prefs (hint shows) instead of propagating and crashing the editor.
        val throwing = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException("corrupt prefs") }
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                throw UnsupportedOperationException("not needed")
        }
        val store = DataStoreEditorOnboardingStore(throwing)

        assertEquals(false, store.moveResizeHintSeen.first())
    }

    @Test
    fun a_non_io_read_error_is_not_swallowed() = runTest {
        // Read-robustness is scoped to expected IO failures; a programming error must surface, not hide.
        val buggy = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IllegalStateException("bug") }
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                throw UnsupportedOperationException("not needed")
        }
        val store = DataStoreEditorOnboardingStore(buggy)

        try {
            store.moveResizeHintSeen.first()
            throw AssertionError("expected the non-IO error to propagate")
        } catch (expected: IllegalStateException) {
            assertEquals("bug", expected.message)
        }
    }
}
