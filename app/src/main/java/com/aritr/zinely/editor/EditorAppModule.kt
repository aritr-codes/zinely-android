package com.aritr.zinely.editor

import android.content.Context
import com.aritr.zinely.core.data.asset.AssetStore
import com.aritr.zinely.core.data.storage.FileAssetStore
import com.aritr.zinely.core.data.storage.FileSystemOps
import com.aritr.zinely.core.imposition.Imposer
import com.aritr.zinely.core.imposition.SingleSheet8Imposer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.io.File
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
 * The global content-addressed asset directory `filesDir/assets` (ADR-022/ADR-031 §6). One provider,
 * shared by the writer (`FileAssetStore`, Inc 2b) and the render reader (`FileAssetBytesSource`), so
 * the store root is defined in exactly one place. Qualified because a bare `File` is ambiguous.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class AssetsDir

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

    /** `filesDir/assets` — the content-addressed master store root (created lazily by the writer). */
    @Provides
    @Singleton
    @AssetsDir
    fun provideAssetsDir(@ApplicationContext context: Context): File =
        File(context.filesDir, "assets")

    /**
     * The content-addressed import store (ADR-031 §1) over `filesDir/assets`, wired with the **real**
     * [FileSystemOps] (the `:data-android` `AndroidFileSystemOps` with true `Os.fsync` dir flush — Codex
     * Inc-2a obs), not the pure `Nio` default.
     */
    @Provides
    @Singleton
    fun provideAssetStore(@AssetsDir assetsDir: File, fs: FileSystemOps): AssetStore =
        FileAssetStore(rootDir = assetsDir.toPath(), fs = fs)

    /** The [ADR-023] import-master decoder (ADR-031 §4); needs the app `ContentResolver` for picked Uris. */
    @Provides
    @Singleton
    fun provideImportMasterDecoder(@ApplicationContext context: Context): ImportMasterDecoder =
        ImportMasterDecoder(context.contentResolver)
}
