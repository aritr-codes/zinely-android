package com.aritr.zinely.home

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Durable intent for the shelf's short undo window.
 *
 * The project files remain authoritative. This marker only says that a delete the maker already requested
 * still needs to reach that authoritative store if Android kills the process before the snackbar settles.
 * SharedPreferences `commit()` is deliberate: the marker must be on disk before the card disappears.
 */
internal interface PendingDeleteStore {
    fun pendingIds(): Set<String>

    fun add(id: String): Boolean

    fun remove(id: String): Boolean
}

@Singleton
internal class SharedPreferencesPendingDeleteStore @Inject constructor(
    @ApplicationContext context: Context,
) : PendingDeleteStore {
    private val preferences = context.getSharedPreferences(StoreName, Context.MODE_PRIVATE)

    @Synchronized
    override fun pendingIds(): Set<String> =
        preferences.getStringSet(PendingIdsKey, emptySet()).orEmpty().toSet()

    @Synchronized
    override fun add(id: String): Boolean = write(pendingIds() + id)

    @Synchronized
    override fun remove(id: String): Boolean = write(pendingIds() - id)

    private fun write(ids: Set<String>): Boolean =
        preferences.edit().putStringSet(PendingIdsKey, ids).commit()

    private companion object {
        const val StoreName = "zinely_pending_deletes"
        const val PendingIdsKey = "project_ids"
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PendingDeleteModule {
    @Binds
    abstract fun bindPendingDeleteStore(
        implementation: SharedPreferencesPendingDeleteStore,
    ): PendingDeleteStore
}
