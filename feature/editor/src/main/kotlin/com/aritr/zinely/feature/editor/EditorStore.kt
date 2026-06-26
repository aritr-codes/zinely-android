package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.EditorReducer
import com.aritr.zinely.core.editor.EditorUiState
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.toUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The feature-layer MVI store (spike §1/§2, §10.3 — Codex-reconciled). It owns the single source of
 * truth — the reducer's [EditorModel] — and exposes a history-free [EditorUiState] to Compose. It is a
 * thin shell over the **pure** [EditorReducer]: every [Intent] folds synchronously into a new model, and
 * the resulting [Effect]s are handed to an [EditorEffectRunner] (autosave / image decode / a11y announce).
 *
 * **Threading & re-entrancy (Codex D1).** [dispatch] is **main-thread-only by contract** (the UI and the
 * gesture layer always call it on the main thread), which keeps the model free of cross-thread mutation.
 * Dispatch is serialised through a main-thread FIFO **mailbox**: if an effect runner re-enters with a
 * follow-up intent while a reduction is in flight, the intent is enqueued and drained after the current
 * one — never a nested/recursive reduction (the bug Codex flagged). The runner receives a [postDispatch]
 * callback that confines a background-thread follow-up (e.g. [Intent.CommitAddImage] from the io decode
 * pipeline) back onto the main thread before it touches the mailbox.
 *
 * Live gesture frames never reach here — they drive `graphicsLayer{}` ephemerally and only the baked
 * [Intent.CommitTransform] is dispatched (spike §5.1). Because [Intent.BeginTransform] reduces
 * synchronously, the session token is readable from [uiState] the instant [dispatch] returns (spike §5).
 */
public class EditorStore(
    initial: EditorModel,
    private val scope: CoroutineScope,
    private val mainDispatcher: CoroutineDispatcher,
    private val effectRunner: EditorEffectRunner,
) {
    /** The reducer's full model (history included) — never exposed; only fed back into [EditorReducer]. */
    private val model = MutableStateFlow(initial)

    private val _uiState = MutableStateFlow(initial.toUiState())

    /** The history-free projection Compose collects (`collectAsStateWithLifecycle`). */
    public val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val mailbox = ArrayDeque<Intent>()
    private var draining = false

    /** Re-entry path handed to effect runners: confine a background follow-up onto the main thread. */
    private val postDispatch: (Intent) -> Unit = { intent ->
        scope.launch(mainDispatcher) { drain(intent) }
    }

    /** Fold [intent] into the model and run its effects. Main-thread only (see the class threading note). */
    public fun dispatch(intent: Intent): Unit = drain(intent)

    /**
     * Enqueue [intent] and, unless a drain is already running on this (main) thread, drain the mailbox to
     * empty — running each reduction and its effects in FIFO order. Only ever entered on the main thread
     * (public [dispatch] by contract; [postDispatch] confines background re-entries), so the unsynchronised
     * [mailbox]/[draining]/[model] access is safe.
     */
    private fun drain(intent: Intent) {
        mailbox.addLast(intent)
        if (draining) return
        draining = true
        try {
            while (true) {
                val next = mailbox.removeFirstOrNull() ?: break
                val reduction = EditorReducer.reduce(model.value, next)
                model.value = reduction.model
                _uiState.value = reduction.model.toUiState()
                for (effect in reduction.effects) {
                    // Debug invariant (Codex D2): the Autosave payload is the just-committed document, so the
                    // pull-based binder and the effect payload can never disagree; we save via the pull, the
                    // payload is only a marker.
                    if (effect is Effect.Autosave) check(effect.document === reduction.model.document) {
                        "Autosave effect document is not the freshly-reduced document"
                    }
                    effectRunner.run(effect, postDispatch)
                }
            }
        } finally {
            draining = false
        }
    }
}
