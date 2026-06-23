package com.aritr.zinely.data.android

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.storage.DocumentSnapshotProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Lifecycle ownership for one open project's autosave (ADR-026 §1/§3, PR-A Step 6). Owns exactly one
 * [ProjectAutosaveHandle] — created eagerly and reused for the whole project-open lifetime — and binds
 * it to an Android [Lifecycle]: ON_PAUSE (primary) / ON_STOP (backstop) trigger a flush, and
 * [closeProject]/[dispose] run an idempotent flush-then-cancel teardown.
 *
 * **Composition over the frozen core (ADR-025).** The binder adds the interruptibility policy the
 * coordinator deliberately leaves out: the teardown flush + the release wait run under
 * [NonCancellable] so that once teardown begins it always finishes — even if [autosaveScope] is
 * cancelled mid-flight — and a [binderMutex] makes the teardown flush the final, non-concurrent write.
 *
 * **Failure routing (ADR-026 §5).** Background (debounced) failures already reach the [SaveFailureSink]
 * via the factory's collector; the binder additionally routes its *own* caller-owned flushes — the
 * lifecycle flush and the teardown flush — to the same sink (the editor has no other place to learn of
 * them). Foreground/direct [ProjectAutosaveHandle.flushNow] callers are unaffected.
 *
 * **Threading.** [observe]/[dispose] are main-thread (lifecycle) calls; flush work is never run on the
 * lifecycle thread — it is dispatched onto [autosaveScope] (the io dispatcher).
 */
public class EditorAutosaveBinder(
    factory: AutosaveCoordinatorFactory,
    private val projectId: String,
    snapshotProvider: DocumentSnapshotProvider,
    private val autosaveScope: CoroutineScope,
    private val failureSink: SaveFailureSink,
) {
    /** The single per-project handle (ADR-026 §2). Created once; never re-created across rotations. */
    private val handle: ProjectAutosaveHandle = factory.create(projectId, snapshotProvider)

    /** Serializes the lifecycle flush and the teardown flush so they never write concurrently. */
    private val binderMutex = Mutex()

    private val closeLock = Any()
    private var closeJob: Job? = null

    /**
     * Set synchronously the instant [closeProject] is invoked — before the async teardown runs — so a
     * lifecycle event or [markDirty] arriving afterwards is inert (ADR-026 §3, no resurrection). Also
     * re-checked under [binderMutex] so a flush already queued when teardown begins becomes a no-op.
     */
    @Volatile
    private var closed = false

    private var observedLifecycle: Lifecycle? = null
    private var observer: LifecycleEventObserver? = null

    /**
     * Observe [lifecycle], flushing on ON_PAUSE/ON_STOP. Replaces any prior observation (a config-change
     * rotation re-observes the new owner) so the single [handle] is never driven by two lifecycles.
     */
    public fun observe(lifecycle: Lifecycle) {
        detachObserver()
        if (closed) return // a closed binder owns no live coordinator; never re-attach (ADR-026 §3)
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> flushFromLifecycle()
                else -> Unit
            }
        }
        observer = obs
        observedLifecycle = lifecycle
        lifecycle.addObserver(obs)
    }

    /** Signal an edit (debounced save). Inert once [closeProject]/[dispose] has begun teardown. */
    public fun markDirty() {
        if (closed) return
        handle.markDirty()
    }

    /**
     * Lifecycle (ON_PAUSE/ON_STOP) flush. Dispatched onto [autosaveScope] so it never blocks the
     * lifecycle thread; a clean coordinator makes a redundant event a no-op (ON_STOP after a clean
     * ON_PAUSE saves nothing). Suppressed once closed (checked both before launch and under the mutex).
     */
    private fun flushFromLifecycle() {
        if (closed) return
        autosaveScope.launch {
            binderMutex.withLock {
                if (closed) return@withLock
                reportIfFailure(handle.flushNow())
            }
        }
    }

    /**
     * Idempotent teardown (ADR-026 §1/§3): returns the same [Job] on every call. The flush-then-cancel
     * ([ProjectAutosaveHandle.close]) and the [ProjectAutosaveHandle.awaitReleased] wait run under
     * [NonCancellable], so once begun the teardown always completes even if [autosaveScope] is cancelled
     * mid-flight; joining the returned job therefore guarantees the project id has left the factory
     * registry (the reopen contract). Teardown flush failures are routed to the [SaveFailureSink].
     */
    public fun closeProject(): Job = synchronized(closeLock) {
        closeJob ?: run {
            closed = true
            detachObserver() // release the lifecycle reference on explicit close, not only via dispose()
            autosaveScope.launch {
                withContext(NonCancellable) {
                    binderMutex.withLock { reportIfFailure(handle.close()) }
                    handle.awaitReleased()
                }
            }.also { closeJob = it }
        }
    }

    /** Tear the project down (ADR-026 §1). [closeProject] now owns observer detachment. */
    public fun dispose() {
        closeProject()
    }

    private fun detachObserver() {
        observer?.let { prior -> observedLifecycle?.removeObserver(prior) }
        observer = null
        observedLifecycle = null
    }

    private fun reportIfFailure(result: DataResult<Unit>) {
        if (result is DataResult.Failure) failureSink.report(projectId, result.error)
    }
}
