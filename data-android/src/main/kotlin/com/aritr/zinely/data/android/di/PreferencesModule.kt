package com.aritr.zinely.data.android.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.aritr.zinely.data.android.prefs.DataStoreEditorOnboardingStore
import com.aritr.zinely.data.android.prefs.EditorOnboardingStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * The local preferences store wiring (ADR-032). Provides the single Preferences [DataStore] for the
 * editor's onboarding flags and binds the [EditorOnboardingStore] seam over it.
 *
 * **Single instance.** DataStore throws if a second instance is created for the same file in one process,
 * so the store is `@Singleton`; Hilt enforces the one-per-process invariant for us.
 *
 * **Scope.** The store's internal IO runs on a dedicated [CoroutineScope] carrying the app's
 * [IoDispatcher] + a [SupervisorJob] — application-lifetime (the file outlives any one screen), so it is
 * never cancelled with an editor/VM scope. App-private internal storage only (ADR-025/-026); local-only,
 * no network — privacy invariant intact.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object PreferencesModule {

    /** The file name for the editor onboarding preferences (DataStore appends `.preferences_pb`). */
    private const val EDITOR_ONBOARDING_STORE = "editor_onboarding"

    @Provides
    @Singleton
    fun provideEditorOnboardingDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher io: CoroutineDispatcher,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(io + SupervisorJob()),
            produceFile = { context.preferencesDataStoreFile(EDITOR_ONBOARDING_STORE) },
        )

    @Provides
    @Singleton
    fun provideEditorOnboardingStore(
        dataStore: DataStore<Preferences>,
    ): EditorOnboardingStore = DataStoreEditorOnboardingStore(dataStore)
}
