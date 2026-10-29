package com.aritr.zinely.data.android.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aritr.zinely.core.editor.ArtShortcuts
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import java.io.IOException

/**
 * Preferences DataStore adapter for [ArtShortcutsStore]. Modelled on [DataStoreInkLibraryStore].
 *
 * Invariants:
 * - Every read and write goes through [ArtShortcuts.sanitized], so damaged data cannot break the caps.
 * - **Downgrade-safe.** A stored `v` that is present but not [VERSION] reads as empty and [update] then writes
 *   nothing, so an older build never overwrites a newer build's shortcuts.
 * - Undecodable data reads as empty and the next write replaces it. Ids unknown to the catalogue are not
 *   pruned here (the store has no catalogue); the sheet drops them on read.
 */
public class DataStoreArtShortcutsStore(
    private val dataStore: DataStore<Preferences>,
) : ArtShortcutsStore {

    override val shortcuts: Flow<ArtShortcuts> =
        dataStore.data
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .map { decode(it[KEY]).value }

    override suspend fun update(transform: (ArtShortcuts) -> ArtShortcuts) {
        dataStore.edit { preferences ->
            val current = decode(preferences[KEY])
            if (current.newer) return@edit
            val next = transform(current.value).sanitized()
            if (next != current.value) preferences[KEY] = JSON.encodeToString(Dto(VERSION, next.recent, next.favourites))
        }
    }

    private class Decoded(val value: ArtShortcuts, val newer: Boolean)

    private fun decode(raw: String?): Decoded {
        if (raw == null) return Decoded(ArtShortcuts(), false)
        return try {
            val v = (JSON.parseToJsonElement(raw) as? JsonObject)?.get("v")
            if (v != null && (v as? JsonPrimitive)?.intOrNull != VERSION) {
                Decoded(ArtShortcuts(), true)
            } else {
                val dto = JSON.decodeFromString<Dto>(raw)
                Decoded(ArtShortcuts(dto.recent, dto.favourites).sanitized(), false)
            }
        } catch (_: SerializationException) {
            Decoded(ArtShortcuts(), false)
        } catch (_: IllegalArgumentException) {
            Decoded(ArtShortcuts(), false)
        }
    }

    @Serializable
    private class Dto(val v: Int, val recent: List<String> = emptyList(), val favourites: List<String> = emptyList())

    private companion object {
        /** Stable app-preference key; changing it would lose every maker's shortcuts. */
        val KEY = stringPreferencesKey("art_shortcuts")
        const val VERSION = 1
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
