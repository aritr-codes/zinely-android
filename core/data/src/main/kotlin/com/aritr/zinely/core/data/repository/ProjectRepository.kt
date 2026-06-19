package com.aritr.zinely.core.data.repository

import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineFormat
import kotlinx.coroutines.flow.Flow

/**
 * Manages project **metadata** and lifecycle — create/open/duplicate/rename/delete and the
 * observable project list (S2 spike §1/§2). The list is a [Flow] so the UI updates reactively
 * (MVVM, `collectAsStateWithLifecycle`); one-shot operations are `suspend` and return a typed
 * [DataResult]. Content lives behind [DocumentRepository].
 */
public interface ProjectRepository {
    /** The project list, newest-first by convention, emitted again on every change. */
    public fun observeProjects(): Flow<List<ProjectSummary>>

    /** Metadata for one project, or [DataError.NotFound]. */
    public suspend fun getProject(id: String): DataResult<ProjectSummary>

    /** Create a new, empty project of the given format and return its summary. */
    public suspend fun createProject(
        title: String,
        format: ZineFormat,
        paperSize: PaperSize,
    ): DataResult<ProjectSummary>

    /** Rename a project; the document is untouched. */
    public suspend fun renameProject(id: String, title: String): DataResult<Unit>

    /** Deep-copy a project (new id, copied document + assets) and return the new summary. */
    public suspend fun duplicateProject(id: String): DataResult<ProjectSummary>

    /** Delete a project's metadata and document; orphaned assets are reclaimed by GC ([ADR-022]). */
    public suspend fun deleteProject(id: String): DataResult<Unit>
}
