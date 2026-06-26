package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.ImageElement
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The feature-layer side-effect boundary for the editor (ADR-029 §2.2, spike §10.3). The pure reducer
 * only **declares** [Effect]s; the [EditorStore] hands each to an [EditorEffectRunner] that performs the
 * I/O off the reducer. The three collaborators are abstracted as seams so `:feature:editor` stays off
 * `:data-android` (the S2B autosave binder is adapted to [AutosaveSink] at the DI/app layer) and off any
 * concrete image source (wired in a later S4 step).
 */
public interface EditorEffectRunner {
    /**
     * Perform [effect]. [dispatch] feeds **follow-up** intents back into the store (e.g. a decoded image
     * → [Intent.CommitAddImage]); it is already confined to the store's dispatch thread, so a runner may
     * call it from any coroutine/thread safely (the store's mailbox serialises the re-entry).
     */
    public fun run(effect: Effect, dispatch: (Intent) -> Unit)
}

/**
 * Autosave seam — the editor's view of the S2B [EditorAutosaveBinder][com.aritr.zinely…] `markDirty()`.
 * Pull-based: the binder owns debounce/conflation/timing and pulls the latest document snapshot itself
 * (Codex required-fix #5), so this seam only signals "the document changed" — it never carries bytes.
 */
public fun interface AutosaveSink {
    public fun markDirty()
}

/** Accessibility live-region seam (WCAG 4.1.3); the app adapts it to an `AccessibilityManager` announce. */
public fun interface Announcer {
    public fun announce(text: String)
}

/** The outcome of one image pick→decode→store attempt (spike §2.2, Codex rec #6 — never a silent no-op). */
public sealed interface ImagePickResult {
    /** Decoded + stored. The reducer re-mints the element id, so [element]'s id is a placeholder only. */
    public data class Success(val element: ImageElement) : ImagePickResult

    /** The user cancelled the picker — no element, no announcement (the expected, non-error exit). */
    public data object Cancelled : ImagePickResult

    /** Pick/decode/store failed (unsupported type, decode error, OOM) — surfaced to the user, never swallowed. */
    public data class Failure(val message: String) : ImagePickResult
}

/**
 * The pick→decode→AssetStore pipeline seam (spike §2.2). The real implementation (system photo picker →
 * decode → content-addressed asset store, ADR-022/023) is injected by a later S4 step; until then the
 * store wires [UnavailableImagePipeline].
 */
public fun interface ImagePickDecodePipeline {
    public suspend fun pickAndDecode(): ImagePickResult
}

/**
 * The placeholder image pipeline for the store-only S4 step: image import has no source wired yet, so it
 * reports a user-visible [ImagePickResult.Failure] rather than silently no-opping — exercising the whole
 * failure→[Announcer] path end-to-end before the real source lands.
 */
public object UnavailableImagePipeline : ImagePickDecodePipeline {
    override suspend fun pickAndDecode(): ImagePickResult =
        ImagePickResult.Failure("Adding images isn’t available yet")
}

/**
 * The production [EditorEffectRunner] (spike §10.3, Codex-reconciled). It routes each [Effect]:
 *  - [Effect.Autosave] → [AutosaveSink.markDirty] (pull-based; binder owns timing — required-fix #5);
 *  - [Effect.PickAndDecodeImage] → launch the [ImagePickDecodePipeline] on [io], then on success dispatch
 *    [Intent.CommitAddImage] (the reducer mints the id), on failure announce, on cancel do nothing;
 *  - [Effect.Announce] → [Announcer.announce].
 *
 * Background results ([io] pipeline) are routed back to the main thread before touching the store or the
 * a11y announcer: the follow-up [Intent.CommitAddImage] goes through the supplied (main-confined)
 * `dispatch`, and the failure announce is posted onto [main] explicitly.
 */
public class DefaultEditorEffectRunner(
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher,
    private val main: CoroutineDispatcher,
    private val autosave: AutosaveSink,
    private val imagePipeline: ImagePickDecodePipeline,
    private val announcer: Announcer,
) : EditorEffectRunner {

    override fun run(effect: Effect, dispatch: (Intent) -> Unit) {
        when (effect) {
            is Effect.Autosave -> autosave.markDirty()
            is Effect.Announce -> announcer.announce(effect.text)
            Effect.PickAndDecodeImage -> launchImagePick(dispatch)
        }
    }

    private fun launchImagePick(dispatch: (Intent) -> Unit) {
        scope.launch(io) {
            when (val result = imagePipeline.pickAndDecode()) {
                is ImagePickResult.Success -> dispatch(Intent.CommitAddImage(result.element))
                is ImagePickResult.Failure -> withContext(main) { announcer.announce(result.message) }
                ImagePickResult.Cancelled -> Unit
            }
        }
    }
}
