package com.aritr.zinely.data.android.prefs

import com.aritr.zinely.core.editor.ink.InkLibrary
import com.aritr.zinely.core.editor.ink.InkLibraryEdit
import kotlinx.coroutines.flow.Flow

/**
 * My inks and My palettes (ADR-138): app-level preferences, never part of a document and not in backups
 * (an ADR-138 Known Limitation). [update] returns the before and after values so the screen can offer
 * Undo by restoring `before`.
 */
public interface InkLibraryStore {
    /** The current library, always inside [InkLibrary.sanitized]'s invariants. */
    public val library: Flow<InkLibrary>

    /** Applies [transform] to the stored library and persists the result only if it changed. */
    public suspend fun update(transform: (InkLibrary) -> InkLibrary): InkLibraryEdit
}
