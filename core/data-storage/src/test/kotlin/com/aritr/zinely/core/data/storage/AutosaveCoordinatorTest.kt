package com.aritr.zinely.core.data.storage

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * TDD test plan for the autosave coordinator (ADR-021), written before the implementation as a
 * disabled contract list. The next S2B slice turns each test green one at a time (RED -> GREEN),
 * implementing the coordinator over an injected [kotlinx.coroutines.CoroutineDispatcher] + test
 * clock/scheduler and routing every save through [AtomicFileStore.write] (single writer).
 *
 * Scope reminder (ADR-025): pure-JVM only. The Android lifecycle (`ON_STOP`) trigger and the
 * ViewModel wiring live in `:data-android` / S4; here `flushNow()` models the synchronous flush the
 * lifecycle owner will call.
 *
 * The coordinator's exact construction shape (push vs. pull immutable-snapshot supply, error model)
 * is a design decision for that slice and must go through the project's design -> Codex-review ->
 * implement workflow before these stubs are fleshed out.
 */
class AutosaveCoordinatorTest {

    @Test
    @Disabled("S2B autosave slice — debounce 1s coalescing (ADR-021)")
    fun `debounces a burst of edits into a single save`() = Unit

    @Test
    @Disabled("S2B autosave slice — 5s hard max-latency cap (ADR-021)")
    fun `force-flushes at the max-latency cap during a long continuous edit`() = Unit

    @Test
    @Disabled("S2B autosave slice — latest-wins cancellation (ADR-021)")
    fun `a superseded debounce is cancelled and the latest snapshot wins`() = Unit

    @Test
    @Disabled("S2B autosave slice — flushNow awaits the in-flight save (ADR-021)")
    fun `flushNow awaits the in-flight save for ON_STOP and editor exit`() = Unit

    @Test
    @Disabled("S2B autosave slice — single-writer serialization (ADR-021)")
    fun `saves never interleave — one writer per project`() = Unit

    @Test
    @Disabled("S2B autosave slice — immutable snapshot (ADR-021)")
    fun `saves an immutable snapshot decoupled from ongoing edits`() = Unit

    @Test
    @Disabled("S2B autosave slice — typed error mapping (ADR-021 / DataResult)")
    fun `a save failure is surfaced as a DataResult error, not a raw exception`() = Unit

    @Test
    @Disabled("S2B autosave slice — no-op flush (ADR-021)")
    fun `flushNow with no pending edits is a no-op`() = Unit
}
