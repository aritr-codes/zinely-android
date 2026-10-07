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

    /**
     * The Library shelf's list, newest-first by convention, emitted again on every change.
     *
     * This is a wider projection than [observeProjects]: it keeps locally-present projects visible even
     * when their authoritative `document.json` is corrupt or from a newer schema, so the shelf can still
     * offer the safe actions without pretending the zine opens normally.
     */
    public fun observeShelfProjects(): Flow<List<ProjectShelfEntry>>

    /** Metadata for one project, or [DataError.NotFound]. */
    public suspend fun getProject(id: String): DataResult<ProjectSummary>

    /**
     * Create a new, empty project of the given format and return its summary.
     *
     * [folder] is the Shelf folder it is made in, or `null` for My Shelf (ADR-125 rule 8). It is written with
     * the zine's first metadata, in the spelling that folder already has.
     */
    public suspend fun createProject(
        title: String,
        format: ZineFormat,
        paperSize: PaperSize,
        folder: String? = null,
    ): DataResult<ProjectSummary>

    /** Rename a project; the document is untouched. */
    public suspend fun renameProject(id: String, title: String): DataResult<Unit>

    /**
     * Duplicate a project: a new id with a copy of the **document**, **referencing the same content
     * hashes**. Assets are globally shared and content-addressed ([ADR-022]) — the blobs are never
     * copied per project; the duplicate simply adds new live roots over the existing assets.
     * The copy is put beside its original: in the same Shelf folder, or on My Shelf (ADR-125 rule 8).
     */
    public suspend fun duplicateProject(id: String): DataResult<ProjectSummary>

    /** Delete a project's metadata and document; orphaned assets are reclaimed by GC ([ADR-022]). */
    public suspend fun deleteProject(id: String): DataResult<Unit>

    // ---- Shelf folders (ADR-125) -----------------------------------------------------------------
    //
    // A folder is the name its zines carry, so there is nothing to create or delete: a folder exists while a
    // zine carries its name. The three operations below share four properties (rule 17). They write a zine's
    // metadata and nothing else, so a zine that will not open can still be moved. They do not make a zine
    // "newer", so arranging the Shelf never reorders it. They find a folder's zines in the same files the
    // Shelf reads. They never write over metadata that is missing or cannot be read.

    /**
     * Put the zine [id] in [folder], or on My Shelf when [folder] is `null` or stands for no folder
     * ([FolderNames.clean]). Joining an existing folder takes that folder's spelling. A zine already there is
     * left untouched and the call succeeds.
     *
     * Fails with [DataError.NotFound] when there is no such zine and [DataError.Corrupt] when its metadata is
     * missing or cannot be read; that zine stays where it was.
     */
    public suspend fun moveProject(id: String, folder: String?): DataResult<Unit>

    /**
     * Give every zine in the folder [from] the name [to]. Safe to run again: it moves every zine still
     * carrying [from], so a rename that stopped partway is finished by the same call (rule 18). If other zines
     * already carry [to], the renamed zines join them and take their spelling; refusing another folder's name
     * is the caller's rule, not this method's. Changing only the capitals is a rename like any other.
     *
     * Fails with [DataError.Invalid] when [to] stands for no folder. Otherwise succeeds with what was done,
     * which may be partial ([FolderChange.complete]).
     */
    public suspend fun renameFolder(from: String, to: String): DataResult<FolderChange>

    /**
     * Take every zine out of the folder [name] and put it on My Shelf; with no zine left the folder is gone.
     * Safe to run again, and may be partial like [renameFolder].
     */
    public suspend fun unpackFolder(name: String): DataResult<FolderChange>
}
