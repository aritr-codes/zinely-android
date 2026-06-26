package com.aritr.zinely.editor

import com.aritr.zinely.core.imposition.Imposer
import com.aritr.zinely.core.imposition.SingleSheet8Imposer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * The main (UI) [CoroutineDispatcher] (ADR-030 §2). Qualified for the same reason `:data-android`
 * qualifies [com.aritr.zinely.data.android.di.IoDispatcher]: `CoroutineDispatcher` is a framework type
 * with several legitimate bindings, so an unqualified one would be ambiguous in the graph.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class MainDispatcher

/**
 * App-layer DI for the editor host (ADR-030 §2). Supplies the two collaborators the editor wiring needs
 * that `:data-android` does not already provide: the main dispatcher (the store's main-thread contract)
 * and the imposition engine (the single source of `pageSizePt`). Pure wiring — no behavior.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object EditorAppModule {

    /**
     * `Dispatchers.Main.immediate` — the store's `dispatch` is main-thread-only by contract; `.immediate`
     * avoids a re-post when already on the main thread (so a synchronous dispatch stays synchronous).
     */
    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main.immediate

    /** The single-sheet 8-page imposer (the only format the MVP supports). Pure and stateless. */
    @Provides
    @Singleton
    fun provideImposer(): Imposer = SingleSheet8Imposer()
}
