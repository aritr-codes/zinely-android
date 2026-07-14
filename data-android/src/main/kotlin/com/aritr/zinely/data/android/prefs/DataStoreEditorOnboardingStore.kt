package com.aritr.zinely.data.android.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * [EditorOnboardingStore] backed by a Preferences [DataStore] (ADR-032). A thin adapter: it maps one
 * boolean key, applies the canonical DataStore read-robustness pattern, and does nothing else — so the
 * storage boundary stays trivially reviewable.
 *
 * **Read robustness.** A failed read of the on-disk store (an [IOException] from a corrupt/half-written
 * preferences file) is recovered to [emptyPreferences] rather than propagated, so the flag reads `false`
 * (hint shows) instead of crashing the editor. Any other throwable is rethrown (a programming error must
 * not be swallowed — sealed-Result discipline applies to *expected* failures, not bugs).
 *
 * **Write.** [markMoveResizeHintSeen] sets the key to `true`; DataStore's [edit] is itself atomic and
 * serialised, so concurrent or repeated calls converge (idempotent). A failed *write* ([IOException] from
 * the on-disk store) is **swallowed** — this is a best-effort, non-critical hint flag, and the VM launches
 * the write fire-and-forget, so an uncaught [IOException] would otherwise reach the default coroutine
 * handler and crash the app (Codex RF2). The only consequence of a dropped write is that the hint may
 * re-teach once on the next launch; non-IO throwables (programming errors) still propagate.
 *
 * @param dataStore the process-singleton preferences store (one per file — provided by Hilt, ADR-032).
 */
public class DataStoreEditorOnboardingStore(
    private val dataStore: DataStore<Preferences>,
) : EditorOnboardingStore {

    override val moveResizeHintSeen: Flow<Boolean> =
        dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { prefs -> prefs[MOVE_RESIZE_HINT_SEEN] ?: false }

    override suspend fun markMoveResizeHintSeen() {
        try {
            dataStore.edit { prefs -> prefs[MOVE_RESIZE_HINT_SEEN] = true }
        } catch (_: IOException) {
            // Best-effort, non-critical flag — drop a failed write rather than crash the fire-and-forget
            // VM coroutine. Worst case: the hint re-teaches once next launch. Non-IO errors still throw.
        }
    }

    override val reframeCoachSeen: Flow<Boolean> =
        dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { prefs -> prefs[REFRAME_COACH_SEEN] ?: false }

    override suspend fun markReframeCoachSeen() {
        try {
            dataStore.edit { prefs -> prefs[REFRAME_COACH_SEEN] = true }
        } catch (_: IOException) {
            // Best-effort, non-critical flag (same policy as the move/resize hint). Worst case: the coach
            // re-teaches once next launch.
        }
    }

    private companion object {
        /** The "move/resize hint already seen" flag. Stable key — renaming it would re-show the hint. */
        val MOVE_RESIZE_HINT_SEEN = booleanPreferencesKey("move_resize_hint_seen")

        /** The "Reframe coach-mark already taught" flag. Stable key — renaming it would re-teach it. */
        val REFRAME_COACH_SEEN = booleanPreferencesKey("reframe_coach_seen")
    }
}
