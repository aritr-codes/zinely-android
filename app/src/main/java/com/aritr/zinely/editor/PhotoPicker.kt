package com.aritr.zinely.editor

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * A one-in-flight rendezvous between the suspend import pipeline and the Compose-hosted system picker
 * (ADR-031 §5, Codex RF2). The pipeline calls [await] (suspending until a result); the Compose layer
 * owns the `ActivityResultLauncher`, [bind]s its launch action, and [deliver]s the callback result.
 *
 * Generic over the result type [R] purely so the concurrency/lifecycle rules below are unit-testable on
 * the JVM without `android.net.Uri`; production uses `PhotoPicker<Uri>`.
 *
 * **Lifecycle rules (Codex RF2), all under one lock so result/cancel races can't double-resume:**
 *  - [await] with **no launcher bound** resumes `null` immediately — never hangs.
 *  - [await] while **another pick is pending** resumes `null` (single-flight: the first continuation is
 *    never overwritten).
 *  - [unbind] clears only the launch hook and **keeps** any in-flight pick: an ordinary composition
 *    disposal (rotation / activity recreation) must NOT cancel a pick, because the VM (and so this
 *    picker + its pending continuation) survives, the re-composed host re-[bind]s a fresh launcher, and
 *    `ActivityResultRegistry` redelivers the result to it → [deliver] resolves the still-pending await
 *    (Inc-2b Codex RF2). Real teardown is handled by `viewModelScope` cancellation, which fires the
 *    awaiting coroutine's `invokeOnCancellation` and clears the slot — so dropping the resume here can't
 *    strand the pipeline.
 *  - cancellation of the awaiting coroutine clears the pending slot **only if it is still that await's**
 *    (identity-checked — a cancelled A must never clear a newer B's pending; Inc-2b Codex RF1).
 *  - [deliver] atomically takes-then-resumes, so a concurrent cancel can't resume twice.
 */
public class PhotoPicker<R> {

    private val lock = Any()
    private var pending: ((R?) -> Unit)? = null
    private var launch: (() -> Unit)? = null

    /** Bind the Compose launcher's action (called on the main thread from a `DisposableEffect`). */
    public fun bind(onLaunch: () -> Unit) {
        synchronized(lock) { launch = onLaunch }
    }

    /**
     * Unbind the launcher on composition dispose. Clears **only** the launch hook (releasing the captured
     * launcher/Activity); a pick in flight is intentionally kept so a rotation/redelivery still resolves
     * it (see the class note). Teardown of a stranded pick is `viewModelScope`'s job, not this method's.
     */
    public fun unbind() {
        synchronized(lock) { launch = null }
    }

    /** Deliver the picker callback's result (or `null` for cancel) to the in-flight [await]. */
    public fun deliver(result: R?) {
        val resume: ((R?) -> Unit)?
        synchronized(lock) {
            resume = pending
            pending = null
        }
        resume?.invoke(result)
    }

    /**
     * Launch the picker and suspend until [deliver] (or coroutine cancellation). Returns the picked [R],
     * or `null` for cancel / not-bound / a concurrent pick already pending. **Call on the main thread** —
     * it invokes the bound launch action, and `ActivityResultLauncher.launch` is main-thread-only (RF1).
     */
    public suspend fun await(): R? = suspendCancellableCoroutine { cont ->
        // Per-await resumer: its identity is the slot key, so cancellation/launch-failure cleanup below
        // can only ever clear THIS await's pending, never a newer one (Inc-2b Codex RF1).
        val resumer: (R?) -> Unit = { result -> if (cont.isActive) cont.resume(result) }
        val toLaunch: (() -> Unit)?
        synchronized(lock) {
            when {
                launch == null -> { cont.resume(null); return@suspendCancellableCoroutine }
                pending != null -> { cont.resume(null); return@suspendCancellableCoroutine }
                else -> {
                    pending = resumer
                    toLaunch = launch
                }
            }
        }
        cont.invokeOnCancellation {
            synchronized(lock) { if (pending === resumer) pending = null }
        }
        // If launching throws (launcher detached / lifecycle), don't poison the bridge: clear our pending
        // and resume null rather than leave it stuck (Inc-2b Codex Rec).
        try {
            toLaunch?.invoke()
        } catch (_: Throwable) {
            synchronized(lock) { if (pending === resumer) pending = null }
            if (cont.isActive) cont.resume(null)
        }
    }
}
