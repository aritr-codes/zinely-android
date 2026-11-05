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
 * A pick shows at once; if the store refuses it, the colour goes back to what `meta.json` still says, so the
 * screen never claims a paper the zine does not keep.
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
        val before = colour.value
        if (picked == before) return
        userPicked = true
        colour.value = picked
        viewModelScope.launch {
            if (projectRepository.setPaperColour(projectId, picked) is DataResult.Failure) colour.value = before
        }
    }
}
