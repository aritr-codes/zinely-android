package com.aritr.zinely.data.android

import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.DocumentRepository
import com.aritr.zinely.core.data.storage.AutosaveConfig
import com.aritr.zinely.core.data.storage.AutosaveCoordinator
import com.aritr.zinely.core.data.storage.DocumentSaver
import com.aritr.zinely.core.data.storage.DocumentSnapshotProvider
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

/**
 * Assembles and owns one autosave unit per project (ADR-026 §2, PR-A Step 5). Each [create] wires a
 * frozen [AutosaveCoordinator] to the repository ([DocumentSaver] `{ repository.save(projectId, it) }`)
 * and to the application-scoped [SaveFailureSink], returning a [ProjectAutosaveHandle] whose lifetime
 * is a per-project child [SupervisorJob] of [autosaveScope].
 *
 * **Ownership boundary (ADR-021 single writer / ADR-026 one-coordinator-per-project):** the factory
 * keeps an active-project registry so at most one live coordinator can ever target a given document
 * path. A duplicate [create] for an already-active id fails fast; the id is released — exactly once,
 * via the project [Job]'s completion — whether teardown came from [ProjectAutosaveHandle.close],
 * [ProjectAutosaveHandle.cancel], or cancellation of [autosaveScope] itself.
 *
 * **Composition only (ADR-025):** the factory adds no durability or interruptibility semantics of its
 * own. Lifecycle teardown policy and the `NonCancellable` orchestration of the teardown flush stay
 * with the lifecycle binder (Step 6, ADR-026 §1/§3); [ProjectAutosaveHandle.close] here is a thin
 * flush-then-cancel composition over the frozen coordinator.
 */
public class AutosaveCoordinatorFactory(
    private val autosaveScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val repository: DocumentRepository,
    private val failureSink: SaveFailureSink,
    private val config: AutosaveConfig = AutosaveConfig(),
) {
    /** Parent of every project job; cancelling it tears down all coordinators (ADR-026 §2). */
    private val parentJob: Job = requireNotNull(autosaveScope.coroutineContext[Job]) {
        "autosaveScope must contain a Job so project scopes are cancelled when it is"
    }

    private val lock = Any()

    /** Active project id → its handle, guarded by [lock]. The registry IS the single-writer guard. */
    private val active = HashMap<String, ProjectAutosaveHandle>()

    /**
     * Start autosaving [projectId], pulling the document via [snapshotProvider] at each save. Fails
     * fast with [IllegalStateException] if [autosaveScope] is already cancelled or if [projectId] is
     * already active — the caller (the lifecycle binder) must [ProjectAutosaveHandle.close] or
     * [ProjectAutosaveHandle.cancel] the prior handle before re-creating one for the same project.
     *
     * **Release is asynchronous:** a prior handle's id is freed only when its project [Job] *completes*
     * (via `invokeOnCompletion`), which happens after — not at — the `close()`/`cancel()` call returns,
     * once the coordinator finishes cancelling on [ioDispatcher]. So an immediate re-create
     * of the same id on the heels of a teardown can still observe it as active and throw; the lifecycle
     * binder (Step 6) owns ordering teardown completion before re-creation.
     */
    public fun create(
        projectId: String,
        snapshotProvider: DocumentSnapshotProvider,
    ): ProjectAutosaveHandle = synchronized(lock) {
        check(parentJob.isActive) { "autosave scope is no longer active" }
        check(projectId !in active) { "autosave already active for project '$projectId'" }
        RegisteredHandle(projectId, snapshotProvider).also { handle ->
            active[projectId] = handle
            // Attach AFTER inserting: if the project job is born cancelled (parent cancelled in the
            // tiny window after the isActive check), completion fires now and evicts this entry rather
            // than no-op'ing before the insert and leaking a dead handle.
            handle.attachCleanup()
        }
    }

    /** Remove [handle] iff it is still the registered owner of [projectId] (never clobber a successor). */
    private fun unregister(projectId: String, handle: ProjectAutosaveHandle) {
        synchronized(lock) {
            if (active[projectId] === handle) active.remove(projectId)
        }
    }

    private inner class RegisteredHandle(
        private val projectId: String,
        snapshotProvider: DocumentSnapshotProvider,
    ) : ProjectAutosaveHandle {

        /** Per-project lifetime: child of [parentJob], so parent cancellation tears this down too. */
        private val projectJob = SupervisorJob(parentJob)

        /**
         * Release-completion signal (ADR-026 reopen contract). Parentless on purpose: no scope can
         * cancel it, only the factory completes it — and only *after* [unregister] has run (see
         * [attachCleanup]). Callers observe it through [awaitReleased] alone; they hold no handle that
         * could complete or cancel it, so [awaitReleased] returning is a happens-before for the id
         * having left the registry.
         */
        private val releaseSignal: CompletableJob = Job()
        private val projectScope =
            CoroutineScope(autosaveScope.coroutineContext + projectJob + ioDispatcher)

        private val coordinator = AutosaveCoordinator(
            saver = DocumentSaver { repository.save(projectId, it) },
            snapshotProvider = snapshotProvider,
            scope = projectScope,
            dispatcher = ioDispatcher,
            config = config,
            // ADR-037: the factory is the SOLE SaveFailureSink feeder. The coordinator notifies this
            // listener once per completed save — background AND caller-flush (flushNow/close) alike, in
            // durable-write order, synchronously under the writer lock — so a durable success clears the
            // project's failure and a failure reports it. No lossy SharedFlow, no second routing path:
            // `clear` no-ops on an absent key, so the common all-success case never churns the sink.
            outcomeListener = { result ->
                when (result) {
                    is DataResult.Success -> failureSink.clear(projectId)
                    is DataResult.Failure -> failureSink.report(projectId, result.error)
                }
            },
        )

        /**
         * Wired by the factory after registry insert; on job completion unregisters exactly once and
         * *then* completes [releaseSignal]. The order is load-bearing: anyone resumed by
         * [awaitReleased] is guaranteed the id has already left the registry.
         */
        fun attachCleanup() {
            projectJob.invokeOnCompletion {
                unregister(projectId, this)
                releaseSignal.complete()
            }
        }

        override fun markDirty(): Unit = coordinator.markDirty()

        override suspend fun flushNow(): DataResult<Unit> = coordinator.flushNow()

        override suspend fun close(): DataResult<Unit> =
            try {
                flushNow() // ADR-026 §1 flush-then-cancel; no NonCancellable here (Step 6 owns that)
            } finally {
                cancel()
            }

        override fun cancel() {
            coordinator.cancel() // dispose the coordinator's loop (its job is a child of projectJob)
            projectJob.cancel() // tear down the project scope + complete the job → invokeOnCompletion
        }

        override suspend fun awaitReleased() {
            releaseSignal.join() // resumes only after invokeOnCompletion ran unregister + complete
        }
    }
}

/**
 * A live per-project autosave unit produced by [AutosaveCoordinatorFactory]. Narrow by design: push
 * edits with [markDirty], force a save with [flushNow], and tear down with [close] (graceful,
 * flush-then-cancel) or [cancel] (hard, no flush). [flushNow]/[close] also return the save result to
 * the caller, but **every** completed save — background, [flushNow], and [close] — additionally drives
 * the application-scoped [SaveFailureSink] through the coordinator's synchronous outcome listener
 * (ADR-037): a durable success clears the project's failure, a failure reports it. The factory is the
 * sole sink feeder; callers no longer route outcomes themselves.
 */
public interface ProjectAutosaveHandle {
    /** Signal that the document changed; the actual save is debounced (ADR-021). */
    public fun markDirty()

    /** Persist the latest dirty state now, returning the result to the caller. The outcome also drives
     * the [SaveFailureSink] via the coordinator's outcome listener (ADR-037). */
    public suspend fun flushNow(): DataResult<Unit>

    /**
     * Flush the pending state, then tear down. Returns the flush result. (ADR-026 §1 close path.)
     * The project id is released asynchronously when the project job completes (see [cancel]).
     */
    public suspend fun close(): DataResult<Unit>

    /**
     * Hard teardown without flushing. Idempotent. Releases the project id back to the factory
     * **asynchronously** — on project-job completion, after this call returns — so a same-id re-create
     * issued immediately afterward may still see the id as active. Use [awaitReleased] to order a
     * re-create after the release.
     */
    public fun cancel()

    /**
     * Suspend until this handle's project id has been released from the factory registry (ADR-026
     * reopen contract). Returning establishes a happens-before: the registry no longer maps this id
     * to this handle, so a subsequent [create][AutosaveCoordinatorFactory.create] for the same id will
     * not be rejected *on account of this handle*. (It may still be rejected if another owner created
     * the id in the interim, or if the autosave scope has since been cancelled — release frees only
     * this handle's claim.)
     *
     * **Await-only:** the caller cannot complete or cancel the underlying signal — there is no handle
     * to it but this method. Cancelling the awaiting coroutine cancels only the wait; the signal is
     * untouched and still completes once teardown finishes. Idempotent and re-entrant: it returns
     * immediately once released, and never completes while the handle is still live.
     */
    public suspend fun awaitReleased()
}
