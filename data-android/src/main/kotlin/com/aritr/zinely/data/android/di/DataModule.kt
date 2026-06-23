package com.aritr.zinely.data.android.di

import android.content.Context
import com.aritr.zinely.core.data.repository.DocumentRepository
import com.aritr.zinely.core.data.storage.AtomicFileStore
import com.aritr.zinely.core.data.storage.FileSystemOps
import com.aritr.zinely.data.android.AndroidFileSystemOps
import com.aritr.zinely.data.android.AutosaveCoordinatorFactory
import com.aritr.zinely.data.android.DocumentRepositoryImpl
import com.aritr.zinely.data.android.InMemorySaveFailureSink
import com.aritr.zinely.data.android.SaveFailureSink
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

/**
 * Binds the frozen `:data-android` adapters into the Hilt graph (PR-A Step 7, design §6–§9). Pure
 * wiring: every binding is `@Provides` constructing a frozen adapter with its real constructor, so no
 * adapter gains a DI annotation (design §11/§12) and no behavior changes.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DataModule {

    @Provides
    @Singleton
    fun provideFileSystemOps(): FileSystemOps = AndroidFileSystemOps()

    @Provides
    @Singleton
    fun provideAtomicFileStore(fs: FileSystemOps): AtomicFileStore = AtomicFileStore(fs)

    /**
     * The file-only repository over **app-private internal storage** (ADR-025/ADR-026): `rootDir` is
     * `filesDir` and nothing else. [store] is an explicit graph parameter (one shared instance), not
     * built inline.
     */
    @Provides
    @Singleton
    fun provideDocumentRepository(
        @ApplicationContext context: Context,
        store: AtomicFileStore,
    ): DocumentRepository =
        DocumentRepositoryImpl(rootDir = context.filesDir.toPath(), store = store)

    @Provides
    @Singleton
    fun provideSaveFailureSink(): SaveFailureSink = InMemorySaveFailureSink()

    @Provides
    @Singleton
    fun provideAutosaveCoordinatorFactory(
        @AutosaveScope scope: CoroutineScope,
        @IoDispatcher io: CoroutineDispatcher,
        repository: DocumentRepository,
        failureSink: SaveFailureSink,
    ): AutosaveCoordinatorFactory =
        // config left at the frozen AutosaveConfig() default — making it tunable is out of Step 7.
        AutosaveCoordinatorFactory(scope, io, repository, failureSink)
}
