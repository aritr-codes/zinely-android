package com.aritr.zinely.data.android.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Queries over the `projects` index (ADR-042). `Flow` for the observed list; `suspend` one-shots. */
@Dao
internal interface ProjectDao {

    /** Newest-first by the metadata-op timestamp; display recency is refined at the repository. */
    @Query("SELECT * FROM projects ORDER BY updatedAtEpochMs DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun findById(id: String): ProjectEntity?

    /** The indexed id set, for the reconcile scan's disk↔index diff. */
    @Query("SELECT id FROM projects")
    suspend fun ids(): List<String>

    @Upsert
    suspend fun upsert(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: String)
}
