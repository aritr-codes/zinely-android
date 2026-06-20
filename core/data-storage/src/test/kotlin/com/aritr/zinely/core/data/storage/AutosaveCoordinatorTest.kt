package com.aritr.zinely.core.data.storage

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * FROZEN design + test contract for the autosave coordinator (ADR-021), written before the
 * implementation as a disabled test list (the TDD "write the test names first" step). The next S2B
 * slice turns each test green one at a time (RED -> GREEN). Reviewed by Codex (GO WITH FIXES, all
 * fixes adopted); this file is the authoritative contract so a future session needs no chat history.
 *
 * ## Frozen API (`:core:data-storage`, pure JVM — ADR-025)
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
 * ) {
 *     public fun markDirty()                        // push: signal a change after the reducer commits
 *     public suspend fun flushNow(): DataResult<Unit> // ON_STOP / editor-exit / save-before-export
 *     public val failures: Flow<DataError>          // background (debounced) save failures, observable
 *     public fun cancel()                           // dispose internal timers/jobs
 * }
 * ```
 * Production wiring binds the saver to the repository: `DocumentSaver { doc -> repository.save(projectId, doc) }`
 * — so serialization, validation, and DataError mapping are reused, not re-implemented (DocumentRepository KDoc).
 *
 * ## Invariants (ADR-021 / ADR-009)
 * - One save at a time per coordinator (single writer); saves never interleave.
 * - Latest snapshot wins; snapshot is pulled at save start, not at markDirty().
 * - Dirty clears ONLY after a successful save of the latest known dirty state.
 * - flushNow() success ⇒ no known dirty edit remains unsaved at return time.
 * - Background failures are observable via [failures]; failures keep the coordinator dirty/retryable.
 * - CancellationException (external scope cancellation) propagates; it is never mapped to DataError.
 * - Android lifecycle ownership lives in the caller (`:data-android` / S4), not here.
 */
class AutosaveCoordinatorTest {

    // --- Debounce + max-latency cap (ADR-021 cadence) ---

    @Test
    @Disabled("autosave impl slice — debounce 1s coalescing (ADR-021)")
    fun `debounce coalesces a burst of edits into a single save`() = Unit

    @Test
    @Disabled("autosave impl slice — 5s cap from the FIRST unsaved edit, not the latest (ADR-021)")
    fun `max-latency cap fires 5s from the first unsaved edit during a long continuous edit`() = Unit

    @Test
    @Disabled("autosave impl slice — latest-wins cancellation (ADR-021)")
    fun `a superseded debounce is cancelled and the latest snapshot wins`() = Unit

    // --- Snapshot timing + immutability (pull-at-save) ---

    @Test
    @Disabled("autosave impl slice — snapshot pulled at save time, not at markDirty (ADR-021)")
    fun `the snapshot provider is called at save time, not at markDirty`() = Unit

    @Test
    @Disabled("autosave impl slice — saved snapshot is immutable vs later edits (ADR-021)")
    fun `the saved snapshot is immutable relative to later editor mutations`() = Unit

    // --- Single writer ---

    @Test
    @Disabled("autosave impl slice — single-writer serialization (ADR-021)")
    fun `saves never interleave for one coordinator`() = Unit

    // --- flushNow semantics (ON_STOP / editor exit) ---

    @Test
    @Disabled("autosave impl slice — no-op flush when clean (ADR-021)")
    fun `flushNow with no pending dirty state returns success without saving`() = Unit

    @Test
    @Disabled("autosave impl slice — flushNow cancels pending timers and saves now (ADR-021)")
    fun `flushNow cancels pending debounce and cap timers and saves immediately`() = Unit

    @Test
    @Disabled("autosave impl slice — flushNow awaits the in-flight save (ADR-021)")
    fun `flushNow waits for an in-flight save to complete`() = Unit

    @Test
    @Disabled("autosave impl slice — flushNow re-saves edits arriving during the in-flight save (ADR-021)")
    fun `flushNow performs a second save when dirtied while awaiting the in-flight save`() = Unit

    @Test
    @Disabled("autosave impl slice — concurrent flushNow coalesces (ADR-021)")
    fun `concurrent flushNow calls serialize and observe a consistent result`() = Unit

    // --- Failure + dirty-state + cancellation semantics ---

    @Test
    @Disabled("autosave impl slice — typed error, never a raw exception (DataResult)")
    fun `a save failure surfaces a DataError, never a raw platform exception`() = Unit

    @Test
    @Disabled("autosave impl slice — failure keeps dirty/retryable (ADR-021 / ADR-009)")
    fun `a save failure leaves the coordinator dirty and retryable`() = Unit

    @Test
    @Disabled("autosave impl slice — background failure observable via failures flow (ADR-009)")
    fun `a debounced background save failure is observable on the failures flow`() = Unit

    @Test
    @Disabled("autosave impl slice — cancellation is not an error (structured concurrency)")
    fun `external scope cancellation is not converted into a DataError`() = Unit
}
