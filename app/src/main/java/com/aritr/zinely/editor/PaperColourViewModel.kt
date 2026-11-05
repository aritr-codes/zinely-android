package com.aritr.zinely.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.PaperColour
import com.aritr.zinely.core.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The open zine's paper colour (AW6, [ADR-150](../../../../../../../docs/DECISIONS.md#adr-150)), for the Bench and
 * the Proof. Scoped to the editor's back-stack entry, as [EditorViewModel] is, so the Proof (which resolves against
 * that entry) and the Bench read and write ONE value.
 *
 * Kept out of [EditorViewModel] on purpose: the colour is not part of the document (AW6.10: `meta.json`, not
 * `document.json`; no undo step; no recency change), so it never passes through the single-writer store.
 *
 * A pick shows at once; if the store refuses it, `meta.json` is read again and shown, so the screen never claims a
 * paper the zine does not keep. The refusal is silent (ponytail: no message yet; add one with the Bench's error
 * surface if a write ever fails in the field).
 */
@HiltViewModel
internal class PaperColourViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
) : ViewModel() {

    private val projectId: String = savedStateHandle.toRoute<EditorRoute>().projectId

    private val colour = MutableStateFlow(PaperColour.WHITE)

    /** White until `meta.json` is read, which draws no layer: the safe direction for a first frame. */
    val paperColour: StateFlow<PaperColour> = colour.asStateFlow()

    /** Declared before `init`, which reads it: a pick made before the read returned is newer than the read. */
    private var userPicked = false

    init {
        viewModelScope.launch {
            val stored = projectRepository.getPaperColour(projectId)
            if (!userPicked) colour.value = stored
        }
    }

    fun setPaperColour(picked: PaperColour) {
        if (picked == colour.value) return
        userPicked = true
        colour.value = picked
        viewModelScope.launch {
            if (projectRepository.setPaperColour(projectId, picked) is DataResult.Failure) {
                // Re-read rather than restore the value before the pick: that may be the first frame's White, or a
                // pick a later write has since replaced. A later pick still on screen keeps its own write's outcome.
                val stored = projectRepository.getPaperColour(projectId)
                if (colour.value == picked) colour.value = stored
            }
        }
    }
}
