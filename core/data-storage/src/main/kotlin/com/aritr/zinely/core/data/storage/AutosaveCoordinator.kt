package com.aritr.zinely.core.data.storage

import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.model.ZineDocument
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Persists one document snapshot. The coordinator composes over this seam rather than calling
 * [AtomicFileStore] directly, so production wiring reuses the repository's serialization, validation,
 * and [DataError] mapping: `DocumentSaver { doc -> repository.save(projectId, doc) }` (ADR-021,
 * `DocumentRepository` KDoc).
 */
public fun interface DocumentSaver {
    public suspend fun save(document: ZineDocument): DataResult<Unit>
}

/**
 * Supplies the current document at save time. Pulled by the coordinator when a save starts (not at
 * [AutosaveCoordinator.markDirty]), so the latest committed editor state is what gets persisted.
 * Returns an immutable [ZineDocument]; later editor mutations produce new instances.
 */
public fun interface DocumentSnapshotProvider {
    public fun snapshot(): ZineDocument
}

/**
 * Autosave cadence (ADR-021). [debounce] coalesces a burst of edits into one save; [maxLatency] is
 * the hard cap measured from the first unsaved edit, so a continuous edit still persists on time.
 */
public data class AutosaveConfig(
    val debounce: Duration = 1.seconds,
    val maxLatency: Duration = 5.seconds,
)

/**
 * Debounced, single-writer autosave coordinator (ADR-021 / ADR-009). [markDirty] pushes a change
 * signal; the coordinator waits for [AutosaveConfig.debounce] of quiescence (bounded by
 * [AutosaveConfig.maxLatency] from the first edit) and then saves the latest snapshot through
 * [DocumentSaver]. Exactly one save runs at a time; the most recent snapshot wins; a dirty edit
 * clears only after a successful save of that edit.
 *
 * Pure JVM and Android-free (ADR-025): timers use coroutine [withTimeoutOrNull] on the injected
 * [dispatcher], so they run under the virtual test clock. Lifecycle ownership stays with the caller
 * (`:data-android` / S4 drives [markDirty] / [flushNow] / [cancel]); the coordinator never observes
 * Android lifecycle itself.
 *
 * @param scope caller-owned scope; the coordinator launches its save loop on a child [SupervisorJob]
 *   of this scope, so the caller's cancellation cancels the coordinator (and [cancel] disposes the
 *   coordinator without touching the caller's scope).
 */
public class AutosaveCoordinator(
    private val saver: DocumentSaver,
    private val snapshotProvider: DocumentSnapshotProvider,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
    private val config: AutosaveConfig = AutosaveConfig(),
) {
    /** Child of the caller's job: cancelled by caller-scope cancellation or by [cancel]; never cancels the caller. */
    private val job = SupervisorJob(scope.coroutineContext[Job])
    private val coordinatorScope = CoroutineScope(scope.coroutineContext + job + dispatcher)

    /** Monotonic edit counter ([markDirty]) vs. the counter value captured by the last successful save. */
    private val dirtyGen = AtomicLong(0L)
    private val savedGen = AtomicLong(0L)
    private fun isDirty(): Boolean = dirtyGen.get() != savedGen.get()

    /** Conflated wake-up signal: a burst of edits collapses to a single pending tick. */
    private val ticks = Channel<Unit>(Channel.CONFLATED)

    /** Serializes every actual save so the background loop and [flushNow] never write concurrently. */
    private val writeMutex = Mutex()

    private val _failures = MutableSharedFlow<DataError>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Background (debounced) save failures. [flushNow] returns its failure directly instead. */
    public val failures: Flow<DataError> = _failures.asSharedFlow()

    init {
        coordinatorScope.launch { consume() }
    }

    /** Push signal: record that the document changed. Non-blocking; the actual save is debounced. */
    public fun markDirty() {
        dirtyGen.incrementAndGet()
        ticks.trySend(Unit)
    }

    /**
     * Save the latest dirty state now, bypassing the debounce/cap timers. Awaits any in-flight save
     * first and re-saves if an edit arrived while waiting, so on a [DataResult.Success] return no
     * known dirty edit remains. A no-op (returns success) when already clean. Concurrent callers
     * serialize on the single writer. Used for ON_STOP, editor exit, and save-before-export.
     */
    public suspend fun flushNow(): DataResult<Unit> = drain(loopUntilClean = true)

    /** Dispose the coordinator's save loop and timers without cancelling the caller's scope. */
    public fun cancel() {
        job.cancel()
    }

    /**
     * Background loop: one debounced + capped save per dirty burst.
     *
     * The cap bounds only the *debounce wait*; [drain] runs OUTSIDE [withTimeoutOrNull] so a slow
     * write is never cancelled mid-flight. Because there is a single writer (ADR-021), an edit that
     * lands while a save is in flight cannot be persisted until that save releases the writer, then
     * starts a fresh window. So the "≤[AutosaveConfig.maxLatency] from the first unsaved edit"
     * guarantee holds whenever no save is already running, and otherwise is deferred by at most the
     * in-flight save's duration plus one debounce — bounded in practice because saves are fast local
     * atomic writes (ADR-021/ADR-009). A hard sub-cap guarantee under pathologically slow storage
     * would need a separate cap timer independent of the writer; deferred as it is not required here.
     */
    private suspend fun consume() {
        for (signal in ticks) {
            if (!isDirty()) continue // spurious wake-up (e.g. a tick left by markDirty after cancel)
            // Wait for [debounce] of quiescence, bounded by [maxLatency] from this first unsaved edit.
            withTimeoutOrNull(config.maxLatency) {
                while (withTimeoutOrNull(config.debounce) { ticks.receive() } != null) {
                    // a new edit arrived inside the debounce gap → reset and keep waiting
                }
            }
            val result = drain(loopUntilClean = false)
            if (result is DataResult.Failure) _failures.tryEmit(result.error)
        }
    }

    /**
     * Persist the latest dirty snapshot under the single-writer lock. Pulls the snapshot at save
     * start (latest wins) and advances [savedGen] only on success (a failure stays dirty/retryable).
     * When [loopUntilClean], keeps saving until no newer edit remains (used by [flushNow]); the
     * background path saves once and lets a fresh tick reschedule any edit made during the save.
     */
    private suspend fun drain(loopUntilClean: Boolean): DataResult<Unit> = writeMutex.withLock {
        var last: DataResult<Unit> = DataResult.Success(Unit)
        while (isDirty()) {
            val gen = dirtyGen.get()
            val result = try {
                saver.save(snapshotProvider.snapshot()) // snapshot pulled at save start (latest wins)
            } catch (cancellation: CancellationException) {
                throw cancellation // structured cancellation is never an autosave failure
            } catch (e: Exception) {
                // catch Exception, not Throwable: never swallow fatal VM/programmer Errors
                // (OutOfMemoryError, StackOverflowError, AssertionError) into a recoverable DataError
                DataResult.Failure(DataError.Unknown(e.message ?: "autosave failed", e))
            }
            last = result
            if (result is DataResult.Success) {
                savedGen.set(gen) // clear the edit we captured (a newer edit re-dirties via dirtyGen)
                if (!loopUntilClean) break
            } else {
                break // leave savedGen behind so the edit stays dirty and retryable
            }
        }
        last
    }
}
