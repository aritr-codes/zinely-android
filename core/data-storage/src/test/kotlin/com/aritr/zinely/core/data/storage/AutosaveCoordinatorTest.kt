package com.aritr.zinely.core.data.storage

import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

/**
 * Behavioural contract for the autosave coordinator (ADR-021), driven TDD against the frozen design
 * (Codex: DESIGN APPROVED — BEGIN IMPLEMENTATION). The API below is authoritative so a future
 * session needs no chat history; do not change it without a fresh review.
 *
 * ## API (`:core:data-storage`, pure JVM — ADR-025)
 * ```
 * public fun interface DocumentSaver {            // composes over DocumentRepository.save (NOT AtomicFileStore)
 *     public suspend fun save(document: ZineDocument): DataResult<Unit>
 * }
 * public fun interface DocumentSnapshotProvider {  // pulled at save time, returns the current immutable document
 *     public fun snapshot(): ZineDocument
 * }
 * public data class AutosaveConfig(
 *     val debounce: Duration = 1.seconds,          // ADR-021 debounce
 *     val maxLatency: Duration = 5.seconds,        // ADR-021 hard cap, from the first unsaved edit
 * )
 * public class AutosaveCoordinator(
 *     saver: DocumentSaver,
 *     snapshotProvider: DocumentSnapshotProvider,
 *     scope: CoroutineScope,                        // caller-owned (lifecycle stays outside this module)
 *     dispatcher: CoroutineDispatcher,             // injected; test uses a test dispatcher/scheduler
 *     config: AutosaveConfig = AutosaveConfig(),
 *     outcomeListener: (DataResult<Unit>) -> Unit = {}, // ADR-037: synchronous per-attempt outcome seam
 * ) {
 *     public fun markDirty()                        // push: signal a change after the reducer commits
 *     public suspend fun flushNow(): DataResult<Unit> // ON_STOP / editor-exit / save-before-export
 *     public fun cancel()                           // dispose internal timers/jobs
 * }
 * ```
 * Production wiring binds the saver to the repository: `DocumentSaver { doc -> repository.save(projectId, doc) }`.
 *
 * ## Invariants (ADR-021 / ADR-009 / ADR-037)
 * - One save at a time per coordinator (single writer); saves never interleave.
 * - Latest snapshot wins; snapshot is pulled at save start, not at markDirty().
 * - Dirty clears ONLY after a successful save of the latest known dirty state.
 * - flushNow() success ⇒ no known dirty edit remains unsaved at return time.
 * - Every completed save attempt (background **and** flushNow) notifies [outcomeListener] exactly once,
 *   under the writer lock, in durable-write order; on success after the save generation advances.
 *   Failures keep the coordinator dirty/retryable. Listener faults are isolated from the writer.
 * - CancellationException (external scope cancellation) propagates; it is never mapped to DataError.
 * - Android lifecycle ownership lives in the caller (`:data-android` / S4), not here.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AutosaveCoordinatorTest {

    // --- Debounce + max-latency cap (ADR-021 cadence) ---

    @Test
    fun `debounce coalesces a burst of edits into a single save`() = runTest {
        val saver = RecordingSaver()
        val coordinator = coordinator(saver, { doc(1) })

        coordinator.markDirty(); runCurrent()
        advanceTimeBy(300L); coordinator.markDirty(); runCurrent()
        advanceTimeBy(300L); coordinator.markDirty(); runCurrent()

        advanceTimeBy(300L); runCurrent() // 300ms since the last edit, still inside the 1s debounce
        assertEquals(0, saver.callCount)

        advanceUntilIdle() // debounce elapses
        assertEquals(1, saver.callCount)
    }

    @Test
    fun `max-latency cap fires 5s from the first unsaved edit during a long continuous edit`() = runTest {
        val saver = RecordingSaver()
        val coordinator = coordinator(saver, { doc(1) })

        coordinator.markDirty(); runCurrent() // first edit at t=0; cap window t0..t5000
        repeat(4) { advanceTimeBy(800L); coordinator.markDirty(); runCurrent() } // edits at 800..3200
        assertEquals(0, saver.callCount) // t=3200 < cap; debounce keeps resetting → no save yet

        repeat(2) { advanceTimeBy(800L); coordinator.markDirty(); runCurrent() } // edits at 4000, 4800
        advanceTimeBy(400L); runCurrent() // advance to t=5200, past the 5s cap
        assertEquals(1, saver.callCount) // cap fired at t=5000 even though edits never quiesced
        assertEquals(doc(1), saver.saved.single())
    }

    @Test
    fun `a superseded debounce is cancelled and the latest snapshot wins`() = runTest {
        var current = doc(1)
        val saver = RecordingSaver()
        val coordinator = coordinator(saver, { current })

        coordinator.markDirty(); runCurrent()
        advanceTimeBy(500L); runCurrent()
        current = doc(2)
        coordinator.markDirty(); runCurrent()

        advanceUntilIdle()
        assertEquals(1, saver.callCount)
        assertEquals(doc(2), saver.saved.single())
    }

    // --- Snapshot timing + immutability (pull-at-save) ---

    @Test
    fun `the snapshot provider is called at save time, not at markDirty`() = runTest {
        var snapshotCalls = 0
        val provider = DocumentSnapshotProvider { snapshotCalls++; doc(1) }
        val saver = RecordingSaver()
        val coordinator = coordinator(saver, provider)

        coordinator.markDirty(); runCurrent()
        assertEquals(0, snapshotCalls)

        advanceUntilIdle()
        assertEquals(1, snapshotCalls)
        assertEquals(1, saver.callCount)
    }

    @Test
    fun `the saved snapshot is immutable relative to later editor mutations`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val saved = mutableListOf<ZineDocument>()
        var current = doc(1)
        val saver = DocumentSaver { d -> saved += d; gate.await(); DataResult.Success(Unit) }
        val coordinator = coordinator(saver, { current })

        coordinator.markDirty()
        advanceUntilIdle() // save starts, captures doc(1), then awaits the gate
        current = doc(2) // editor mutates AFTER the snapshot was pulled
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf(doc(1)), saved)
    }

    // --- Single writer ---

    @Test
    fun `saves never interleave for one coordinator`() = runTest {
        val release = Channel<Unit>(Channel.UNLIMITED)
        var inFlight = 0
        var maxConcurrent = 0
        var current = doc(1)
        val saver = DocumentSaver { _ ->
            inFlight++
            if (inFlight > maxConcurrent) maxConcurrent = inFlight
            release.receive()
            inFlight--
            DataResult.Success(Unit)
        }
        val coordinator = coordinator(saver, { current })

        coordinator.markDirty()
        advanceUntilIdle() // save #1 starts and holds the write lock awaiting release
        assertEquals(1, inFlight)

        current = doc(2)
        coordinator.markDirty()
        advanceUntilIdle() // save #2 cannot start while #1 holds the writer
        assertEquals(1, inFlight)

        release.send(Unit); advanceUntilIdle() // #1 completes; #2 (debounced) then runs
        release.send(Unit); advanceUntilIdle() // #2 completes
        assertEquals(1, maxConcurrent)
    }

    // --- flushNow semantics (ON_STOP / editor exit) ---

    @Test
    fun `flushNow with no pending dirty state returns success without saving`() = runTest {
        val saver = RecordingSaver()
        val coordinator = coordinator(saver, { doc(1) })

        val result = coordinator.flushNow()

        assertTrue(result is DataResult.Success)
        assertEquals(0, saver.callCount)
    }

    @Test
    fun `flushNow cancels pending debounce and cap timers and saves immediately`() = runTest {
        val saver = RecordingSaver()
        val coordinator = coordinator(saver, { doc(1) })

        coordinator.markDirty(); runCurrent()
        assertEquals(0, saver.callCount) // debounce pending, not yet saved

        val result = coordinator.flushNow()
        assertTrue(result is DataResult.Success)
        assertEquals(1, saver.callCount) // saved immediately, without waiting for the 1s debounce

        advanceUntilIdle()
        assertEquals(1, saver.callCount) // the superseded pending timer no-ops
    }

    @Test
    fun `flushNow waits for an in-flight save to complete`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val saver = DocumentSaver { _ -> calls++; gate.await(); DataResult.Success(Unit) }
        val coordinator = coordinator(saver, { doc(1) })

        coordinator.markDirty()
        advanceUntilIdle() // background save in flight, holds the writer
        assertEquals(1, calls)

        val flush = async { coordinator.flushNow() }
        runCurrent()
        assertFalse(flush.isCompleted) // blocked on the in-flight writer

        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(flush.isCompleted)
        assertTrue(flush.await() is DataResult.Success)
    }

    @Test
    fun `flushNow performs a second save when dirtied while awaiting the in-flight save`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var current = doc(1)
        val saved = mutableListOf<ZineDocument>()
        val saver = DocumentSaver { d ->
            saved += d
            if (saved.size == 1) gate.await() // hold only the first (in-flight) save
            DataResult.Success(Unit)
        }
        val coordinator = coordinator(saver, { current })

        coordinator.markDirty()
        advanceUntilIdle() // save #1 (doc1) in flight, awaiting the gate
        val flush = async { coordinator.flushNow() }
        runCurrent()
        current = doc(2)
        coordinator.markDirty() // dirtied while flushNow awaits the in-flight save
        runCurrent()

        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(flush.await() is DataResult.Success)
        assertEquals(listOf(doc(1), doc(2)), saved) // second save persists the latest snapshot
    }

    @Test
    fun `concurrent flushNow calls serialize and observe a consistent result`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val saver = DocumentSaver { _ -> calls++; gate.await(); DataResult.Success(Unit) }
        val coordinator = coordinator(saver, { doc(1) })

        coordinator.markDirty()
        val f1 = async { coordinator.flushNow() }
        val f2 = async { coordinator.flushNow() }
        advanceUntilIdle() // one save in flight, the other queued on the writer

        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(f1.await() is DataResult.Success)
        assertTrue(f2.await() is DataResult.Success)
        assertEquals(1, calls) // the single dirty state is saved exactly once
    }

    // --- Failure + dirty-state + cancellation semantics ---

    @Test
    fun `a save failure surfaces a DataError, never a raw platform exception`() = runTest {
        val saver = DocumentSaver { _ -> throw IOException("disk full") }
        val coordinator = coordinator(saver, { doc(1) })

        coordinator.markDirty()
        val result = coordinator.flushNow() // must not propagate the IOException

        assertTrue(result is DataResult.Failure)
    }

    @Test
    fun `a save failure leaves the coordinator dirty and retryable`() = runTest {
        var calls = 0
        var failNext = true
        val saver = DocumentSaver { _ ->
            calls++
            if (failNext) DataResult.Failure(DataError.Io("transient")) else DataResult.Success(Unit)
        }
        val coordinator = coordinator(saver, { doc(1) })

        coordinator.markDirty()
        assertTrue(coordinator.flushNow() is DataResult.Failure)

        failNext = false
        assertTrue(coordinator.flushNow() is DataResult.Success)
        assertEquals(2, calls) // retried because the failed edit stayed dirty
    }

    @Test
    fun `a debounced background save failure is delivered to the outcome listener`() = runTest {
        val outcomes = mutableListOf<DataResult<Unit>>()
        val saver = DocumentSaver { _ -> DataResult.Failure(DataError.Io("boom")) }
        val coordinator = coordinator(saver, { doc(1) }, outcomeListener = { outcomes += it })

        coordinator.markDirty()
        advanceUntilIdle() // debounce fires → background save fails → notifies the listener

        assertEquals(listOf<DataResult<Unit>>(DataResult.Failure(DataError.Io("boom"))), outcomes)
    }

    @Test
    fun `external scope cancellation is not converted into a DataError`() = runTest {
        val extScope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
        val outcomes = mutableListOf<DataResult<Unit>>()
        val gate = CompletableDeferred<Unit>()
        val saver = DocumentSaver { _ -> gate.await(); DataResult.Success(Unit) }
        val coordinator = AutosaveCoordinator(
            saver, { doc(1) }, extScope, StandardTestDispatcher(testScheduler),
            outcomeListener = { outcomes += it },
        )

        coordinator.markDirty()
        advanceUntilIdle() // background save in flight, awaiting the gate
        extScope.cancel() // external cancellation mid-save
        advanceUntilIdle()

        // CancellationException is not mapped to a DataError → no Failure outcome is emitted.
        assertTrue(outcomes.none { it is DataResult.Failure })
    }

    // --- ADR-037: synchronous outcome listener ---

    @Test
    fun `a successful save notifies the listener with Success`() = runTest {
        val outcomes = mutableListOf<DataResult<Unit>>()
        val coordinator = coordinator(RecordingSaver(), { doc(1) }, outcomeListener = { outcomes += it })

        coordinator.markDirty()
        advanceUntilIdle()

        assertEquals(listOf<DataResult<Unit>>(DataResult.Success(Unit)), outcomes)
    }

    @Test
    fun `a successful save notifies once and leaves the coordinator clean`() = runTest {
        // Observable contract: exactly one Success notification, and afterward the edit is persisted —
        // a redundant flushNow is a no-op (no second repository call). The stricter invariant that the
        // listener fires *after* savedGen advances within the lock is enforced by drain()'s structure
        // (the success branch sets savedGen, then calls notifyOutcome) and is not separately observable
        // through the public API without a production-only test hook (which TDD forbids).
        val saver = RecordingSaver()
        val outcomes = mutableListOf<DataResult<Unit>>()
        val coordinator = coordinator(saver, { doc(1) }, outcomeListener = { outcomes += it })

        coordinator.markDirty()
        advanceUntilIdle()
        assertEquals(listOf<DataResult<Unit>>(DataResult.Success(Unit)), outcomes)

        val redundant = coordinator.flushNow() // clean → no-op
        assertTrue(redundant is DataResult.Success)
        assertEquals(1, saver.callCount) // nothing left to save: the edit was already persisted
    }

    @Test
    fun `a listener fault does not corrupt the writer and the next save still runs`() = runTest {
        val saver = RecordingSaver()
        var current = doc(1)
        val coordinator = coordinator(saver, { current }, outcomeListener = {
            throw IllegalStateException("observer blew up")
        })

        coordinator.markDirty()
        advanceUntilIdle()
        assertEquals(1, saver.callCount) // first save completed despite the throwing listener

        current = doc(2)
        coordinator.markDirty()
        advanceUntilIdle()
        assertEquals(2, saver.callCount) // writer is not poisoned: the next save still runs
        assertEquals(listOf(doc(1), doc(2)), saver.saved)
    }

    @Test
    fun `the default no-op listener preserves existing behaviour`() = runTest {
        // Back-compat: a coordinator built without the seam saves exactly as before.
        val saver = RecordingSaver()
        val coordinator = coordinator(saver, { doc(1) })

        coordinator.markDirty()
        advanceUntilIdle()

        assertEquals(1, saver.callCount)
    }

    @Test
    fun `bursty saves deliver exactly one ordered outcome per completed save with no loss`() = runTest {
        val outcomes = mutableListOf<DataResult<Unit>>()
        val saver = RecordingSaver()
        var current = doc(0)
        val coordinator = coordinator(saver, { current }, outcomeListener = { outcomes += it })

        // Five discrete edit→flush cycles: each flushNow forces exactly one completed save.
        repeat(5) { i ->
            current = doc(i + 1)
            coordinator.markDirty()
            coordinator.flushNow()
        }
        advanceUntilIdle()

        assertEquals(5, saver.callCount) // five completed saves
        assertEquals(saver.callCount, outcomes.size) // exactly one outcome per completed save — none lost
        assertTrue(outcomes.all { it is DataResult.Success })
    }
}

// --- Test fixtures -------------------------------------------------------------------------------

/** Distinct, immutable documents keyed by [marker] (data-class equality distinguishes them). */
private fun doc(marker: Int): ZineDocument =
    ZineDocument(
        format = ZineFormat.SINGLE_SHEET_8,
        paperSize = PaperSize.LETTER,
        pages = listOf(Page(index = marker, role = PageRole.INTERIOR)),
    )

/**
 * A coordinator on the test's virtual clock. It runs on an independent foreground scope (a
 * [StandardTestDispatcher] with its own [Job], not [TestScope.backgroundScope]) so `advanceUntilIdle`
 * drives its debounce/cap timers — background-scope delays are *not* advanced by `advanceUntilIdle`.
 * The scope has no parent in the test's job tree, so the runner neither waits on it nor flags a leak.
 */
private fun TestScope.coordinator(
    saver: DocumentSaver,
    snapshot: DocumentSnapshotProvider,
    config: AutosaveConfig = AutosaveConfig(),
    outcomeListener: (DataResult<Unit>) -> Unit = {},
): AutosaveCoordinator {
    val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
    return AutosaveCoordinator(
        saver, snapshot, scope, StandardTestDispatcher(testScheduler), config, outcomeListener,
    )
}

/** Records every saved document; returns success unless a [behavior] override is supplied. */
private class RecordingSaver(
    private val behavior: suspend (ZineDocument) -> DataResult<Unit> = { DataResult.Success(Unit) },
) : DocumentSaver {
    val saved = mutableListOf<ZineDocument>()
    val callCount: Int get() = saved.size
    override suspend fun save(document: ZineDocument): DataResult<Unit> {
        saved += document
        return behavior(document)
    }
}
