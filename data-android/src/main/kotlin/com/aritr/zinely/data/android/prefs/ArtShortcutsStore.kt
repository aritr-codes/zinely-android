package com.aritr.zinely.data.android.prefs

import com.aritr.zinely.core.editor.ArtShortcuts
import kotlinx.coroutines.flow.Flow

/**
 * The Art sheet's Recent and Favourites (piece ids only), so they survive process death and restarts.
 *
 * Privacy and backup: an app-level preference on the device, never part of a document, never sent anywhere,
 * and in no backup: `android:allowBackup=false` plus the exclude-all `backup_rules` / `data_extraction_rules`
 * (the same position as My inks, ADR-138) and not in Zinely's own backup file. Losing it costs a maker six
 * shortcuts and some stars, so it is a Known Limitation, not a gap to close.
 */
public interface ArtShortcutsStore {
    /** The current shortcuts, always inside [ArtShortcuts.sanitized]'s bounds. */
    public val shortcuts: Flow<ArtShortcuts>

    /** Applies [transform] to the stored value and persists the result only if it changed. */
    public suspend fun update(transform: (ArtShortcuts) -> ArtShortcuts)
}
