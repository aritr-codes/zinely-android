package com.aritr.zinely.data.android.di

import android.content.Context
import androidx.room.Room
import com.aritr.zinely.core.data.repository.DocumentRepository
import com.aritr.zinely.core.data.repository.ProjectRepository
import com.aritr.zinely.core.data.storage.AtomicFileStore
import com.aritr.zinely.core.data.storage.FileSystemOps
import com.aritr.zinely.data.android.AndroidFileSystemOps
import com.aritr.zinely.data.android.AutosaveCoordinatorFactory
import com.aritr.zinely.data.android.AutosaveSessionGate
import com.aritr.zinely.data.android.DocumentRepositoryImpl
import com.aritr.zinely.data.android.InMemorySaveFailureSink
import com.aritr.zinely.data.android.ProjectDocumentLayout
import com.aritr.zinely.data.android.RoomProjectRepository
import com.aritr.zinely.data.android.SaveFailureSink
import com.aritr.zinely.data.android.projectDocumentLayout
import com.aritr.zinely.data.android.room.ZinelyDatabase
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

    /**
     * S6.1 (ADR-042): the local-only, app-private Room DB holding the rebuildable `projects` index.
     * No network, no analytics; already excluded from cloud backup/D2D by ADR-030 §7. Open-time
     * corruption falls to SQLite's default error handler (drops the DB), after which the
     * repository's reconcile scan rebuilds every row from the per-project files.
     */
    @Provides
    @Singleton
    fun provideZinelyDatabase(@ApplicationContext context: Context): ZinelyDatabase =
        Room.databaseBuilder(context, ZinelyDatabase::class.java, "zinely.db").build()

    /**
     * S6.1 (ADR-042): the Room-backed [ProjectRepository] over the same app-private `rootDir` as
     * the document store — files are the source of truth, Room is the derived index.
     */
    @Provides
    @Singleton
    fun provideProjectRepository(
        @ApplicationContext context: Context,
        database: ZinelyDatabase,
        documents: DocumentRepository,
        store: AtomicFileStore,
        autosaveFactory: AutosaveCoordinatorFactory,
        @IoDispatcher io: CoroutineDispatcher,
    ): ProjectRepository = RoomProjectRepository(
        rootDir = context.filesDir.toPath(),
        dao = database.projectDao(),
        documents = documents,
        store = store,
        // ADR-044 §1: mutations are refused (DataError.Busy) while the target id has a live
        // editor autosave session in the single-writer registry.
        sessionGate = AutosaveSessionGate(autosaveFactory),
        io = io,
    )

    /**
     * S6.4 (ADR-045): the narrow document-file window the shelf-thumbnail producer stats for its
     * invalidation stamp — same `rootDir` as the stores, same ProjectPaths chokepoint.
     */
    @Provides
    @Singleton
    fun provideProjectDocumentLayout(@ApplicationContext context: Context): ProjectDocumentLayout =
        projectDocumentLayout(context.filesDir.toPath())

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
