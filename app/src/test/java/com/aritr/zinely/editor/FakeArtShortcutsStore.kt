package com.aritr.zinely.editor

import com.aritr.zinely.core.editor.ArtShortcuts
import com.aritr.zinely.data.android.prefs.ArtShortcutsStore
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [ArtShortcutsStore] for ViewModel tests that construct the seam but do not drive it. */
class FakeArtShortcutsStore : ArtShortcutsStore {
    val state = MutableStateFlow(ArtShortcuts())
    override val shortcuts = state
    override suspend fun update(transform: (ArtShortcuts) -> ArtShortcuts) {
        state.value = transform(state.value).sanitized()
    }
}
