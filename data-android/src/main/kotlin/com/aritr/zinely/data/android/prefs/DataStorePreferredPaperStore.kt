package com.aritr.zinely.data.android.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aritr.zinely.core.model.PaperSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Preferences DataStore adapter for [PreferredPaperStore]. */
public class DataStorePreferredPaperStore(
    private val dataStore: DataStore<Preferences>,
) : PreferredPaperStore {

    override val preferredPaperSize: Flow<PaperSize> =
        dataStore.data
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .map { preferences -> decode(preferences[PREFERRED_PAPER_SIZE]) }

    override suspend fun setPreferredPaperSize(paperSize: PaperSize) {
        dataStore.edit { preferences ->
            preferences[PREFERRED_PAPER_SIZE] = encode(paperSize)
        }
    }

    private companion object {
        /** Stable app-preference key; changing it would reset the user's paper choice. */
        val PREFERRED_PAPER_SIZE = stringPreferencesKey("preferred_paper_size")

        private const val A4_VALUE = "a4"
        private const val LETTER_VALUE = "letter"

        fun encode(paperSize: PaperSize): String = when (paperSize) {
            PaperSize.A4 -> A4_VALUE
            PaperSize.LETTER -> LETTER_VALUE
        }

        fun decode(value: String?): PaperSize = when (value) {
            LETTER_VALUE -> PaperSize.LETTER
            A4_VALUE -> PaperSize.A4
            else -> PaperSize.A4
        }
    }
}
