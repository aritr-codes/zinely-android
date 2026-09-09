package com.aritr.zinely.data.android.prefs

import com.aritr.zinely.core.model.PaperSize
import kotlinx.coroutines.flow.Flow

/**
 * Local install preference for the paper offered when making a new zine.
 *
 * This is app state rather than project content: changing it never rewrites an existing document. A
 * missing or unreadable preference resolves to [PaperSize.A4], while callers can observe subsequent
 * changes through [preferredPaperSize].
 */
public interface PreferredPaperStore {
    /** The preferred paper for newly created zines. */
    public val preferredPaperSize: Flow<PaperSize>

    /** Atomically persist [paperSize] as the preference for future zines. */
    public suspend fun setPreferredPaperSize(paperSize: PaperSize)
}
