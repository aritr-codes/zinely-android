package com.aritr.zinely.data.android.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aritr.zinely.core.editor.ink.InkHex
import com.aritr.zinely.core.editor.ink.InkLibrary
import com.aritr.zinely.core.editor.ink.InkLibraryEdit
import com.aritr.zinely.core.editor.ink.InkPalette
import com.aritr.zinely.core.editor.ink.MyInk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import java.io.IOException

/**
 * Preferences DataStore adapter for [InkLibraryStore] (ADR-138).
 *
 * Invariants:
 * - Every read goes through [InkLibrary.sanitized], so a hand-edited or damaged value cannot break the caps.
 * - **Downgrade-safe.** A stored `v` that is present but not [VERSION] (newer, or unreadable) reads as [InkLibrary.EMPTY], and [update] then
 *   writes nothing and returns an unchanged edit, so an older build never overwrites a newer build's inks.
 * - One bad hex drops only that ink or palette. Wholly undecodable data is already lost; the next write replaces
 *   it (refusing would leave the feature dead).
 */
public class DataStoreInkLibraryStore(
    private val dataStore: DataStore<Preferences>,
) : InkLibraryStore {

    override val library: Flow<InkLibrary> =
        dataStore.data
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .map { decode(it[KEY]).library }

    override suspend fun update(transform: (InkLibrary) -> InkLibrary): InkLibraryEdit {
        var edit = InkLibraryEdit(InkLibrary.EMPTY, InkLibrary.EMPTY)
        dataStore.edit { preferences ->
            val current = decode(preferences[KEY])
            if (current.newer) return@edit
            val next = transform(current.library).sanitized()
            edit = InkLibraryEdit(current.library, next)
            if (edit.changed) preferences[KEY] = JSON.encodeToString(next.toDto())
        }
        return edit
    }

    private class Decoded(val library: InkLibrary, val newer: Boolean)

    private fun decode(raw: String?): Decoded {
        if (raw == null) return Decoded(InkLibrary.EMPTY, false)
        return try {
            // A "v" that is present but is not exactly 1 (newer, 0, negative, a string) is a format we do
            // not know: read as empty and never overwrite it. A missing "v" is plain garbage.
            val v = (JSON.parseToJsonElement(raw) as? JsonObject)?.get("v")
            if (v != null && (v as? JsonPrimitive)?.intOrNull != VERSION) {
                Decoded(InkLibrary.EMPTY, true)
            } else {
                Decoded(JSON.decodeFromString<LibraryDto>(raw).toLibrary().sanitized(), false)
            }
        } catch (_: SerializationException) {
            Decoded(InkLibrary.EMPTY, false)
        } catch (_: IllegalArgumentException) {
            Decoded(InkLibrary.EMPTY, false)
        }
    }

    @Serializable
    private class InkDto(val hex: String, val used: Long)

    @Serializable
    private class PaletteDto(val id: Long, val name: String, val inks: List<String>)

    @Serializable
    private class LibraryDto(
        val v: Int,
        val inks: List<InkDto> = emptyList(),
        val palettes: List<PaletteDto> = emptyList(),
        val clock: Long = 0,
        val nextId: Long = 1,
        val open: Long? = null,
    ) {
        fun toLibrary(): InkLibrary = InkLibrary(
            // One bad hex drops only its own ink or palette, never the rest of the library.
            myInks = inks.mapNotNull { i -> hexOrNull(i.hex)?.let { MyInk(it, i.used) } },
            palettes = palettes.mapNotNull { p ->
                val hexes = p.inks.map(::hexOrNull)
                if (hexes.any { it == null }) null else InkPalette(p.id, p.name, hexes.filterNotNull())
            },
            clock = clock,
            nextPaletteId = nextId,
            openPaletteId = open,
        )
    }

    private fun InkLibrary.toDto() = LibraryDto(
        v = VERSION,
        inks = myInks.map { InkDto(it.hex.value, it.used) },
        palettes = palettes.map { p -> PaletteDto(p.id, p.name, p.inks.map { it.value }) },
        clock = clock,
        nextId = nextPaletteId,
        open = openPaletteId,
    )

    private companion object {
        /** Stable app-preference key; changing it would lose every maker's inks. */
        val KEY = stringPreferencesKey("ink_library")
        const val VERSION = 1
        val JSON = Json { ignoreUnknownKeys = true }
    }
}

private fun hexOrNull(value: String): InkHex? =
    try { InkHex(value) } catch (_: IllegalArgumentException) { null }
