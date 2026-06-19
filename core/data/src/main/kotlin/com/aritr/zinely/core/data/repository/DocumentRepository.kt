package com.aritr.zinely.core.data.repository

import com.aritr.zinely.core.model.ZineDocument

/**
 * Reads and writes the **content** of a project — its [ZineDocument] tree (S2 spike §1). The editor
 * (S4) talks to this, never to the serializer or filesystem directly. The S2B implementation owns
 * the atomic-save / `.bak` / recovery contract ([ADR-021]); the autosave debounce/single-writer
 * coordinator composes over [save].
 */
public interface DocumentRepository {
    /**
     * Load the current-shaped document for [projectId]. Implementations validate on the way out and
     * fail only on ERROR-severity issues (`ValidationResult.isValid == false` → [DataError.Invalid]);
     * WARNING-severity issues never block a load/save.
     */
    public suspend fun load(projectId: String): DataResult<ZineDocument>

    /** Persist [document] for [projectId] atomically (last-write-wins within the single writer). */
    public suspend fun save(projectId: String, document: ZineDocument): DataResult<Unit>
}
