package com.aritr.zinely.data.android

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.DocumentRepository
import com.aritr.zinely.core.data.storage.AutosaveConfig
import com.aritr.zinely.core.data.storage.DocumentSnapshotProvider
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract for the ADR-026 / PR-A Step 6 [EditorAutosaveBinder]: the lifecycle-ownership layer that
 * owns one [ProjectAutosaveHandle] for the open project, flushes on ON_PAUSE/ON_STOP, and runs an
 * idempotent, [kotlinx.coroutines.NonCancellable] flush-then-cancel teardown that gates re-create on
 * release. Pure composition over the frozen coordinator + the Step 5 factory/handle.
 *
 * Tests run on the virtual clock (a shared [TestCoroutineScheduler]); the binder's autosave scope and
 * io dispatcher are a [StandardTestDispatcher] over it, so `runCurrent`/`advanceUntilIdle` drive every
 * launched flush. Lifecycle events are delivered through a hand-written [FakeLifecycle] (the real
 * pure-JVM [Lifecycle] base), avoiding any device/Robolectric dependency.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditorAutosaveBinderTest {

    // --- Lifecycle flush: ON_PAUSE primary, ON_STOP backstop ---

    @Test
    fun `ON_PAUSE flushes the pending edit immediately`() = runTest {
        val f = BinderFixture(testScheduler)
        val binder = f.binder()
        val lifecycle = FakeLifecycle()
        binder.observe(lifecycle)

        binder.markDirty()
        lifecycle.emit(Lifecycle.Event.ON_PAUSE)
        runCurrent() // dispatch the launched flush WITHOUT elapsing the 1s debounce

        assertEquals(listOf("proj1" to doc(1)), f.repo.saves)
    }

    @Test
    fun `ON_STOP flushes the pending edit`() = runTest {
        val f = BinderFixture(testScheduler)
        val binder = f.binder()
        val lifecycle = FakeLifecycle()
        binder.observe(lifecycle)

        binder.markDirty()
        lifecycle.emit(Lifecycle.Event.ON_STOP)
        runCurrent()

        assertEquals(listOf("proj1" to doc(1)), f.repo.saves)
    }

    @Test
    fun `ON_STOP after a clean ON_PAUSE is a no-op`() = runTest {
        val f = BinderFixture(testScheduler)
        val binder = f.binder()
        val lifecycle = FakeLifecycle()
        binder.observe(lifecycle)

        binder.markDirty()
        lifecycle.emit(Lifecycle.Event.ON_PAUSE)
        runCurrent() // save #1 (coordinator now clean)
        lifecycle.emit(Lifecycle.Event.ON_STOP)
        runCurrent() // clean → coordinator dedups → no second save

        assertEquals(1, f.repo.saves.size)
    }

    // --- markDirty routing ---

    @Test
    fun `markDirty routes the edit through the handle and autosaves`() = runTest {
        val f = BinderFixture(testScheduler)
        val binder = f.binder()

        binder.markDirty()
        advanceUntilIdle() // debounce elapses → background save

        assertEquals(listOf("proj1" to doc(1)), f.repo.saves)
    }

    // --- Failure routing + silent-recovery clear: outcomes reach the sink via the coordinator listener (ADR-037) ---

    @Test
    fun `a lifecycle flush failure reaches the sink (via the coordinator listener)`() = runTest {
        val f = BinderFixture(testScheduler)
        f.repo.result = { DataResult.Failure(DataError.Io("boom")) }
        val binder = f.binder()
        val lifecycle = FakeLifecycle()
        binder.observe(lifecycle)

        binder.markDirty()
        lifecycle.emit(Lifecycle.Event.ON_PAUSE)
        runCurrent()

        // The binder no longer reports directly; the flush outcome drives the sink through the listener.
        val failure = f.sink.failures.value.getValue("proj1")
        assertEquals(DataError.Io("boom"), failure.error)
    }

    @Test
    fun `a lifecycle flush success clears a prior failure (silent recovery)`() = runTest {
        val f = BinderFixture(testScheduler)
        f.repo.result = { DataResult.Failure(DataError.Io("boom")) }
        val binder = f.binder()
        val lifecycle = FakeLifecycle()
        binder.observe(lifecycle)

        binder.markDirty()
        advanceUntilIdle() // background save fails → sink reports
        assertEquals(1, f.sink.failures.value.getValue("proj1").failureCount)

        f.repo.result = { DataResult.Success(Unit) }
        binder.markDirty()
        lifecycle.emit(Lifecycle.Event.ON_PAUSE)
        runCurrent() // lifecycle flush succeeds → listener clears the project

        assertNull(f.sink.failures.value["proj1"]) // banner would auto-dismiss
    }

    @Test
    fun `a teardown flush success clears a prior failure (no stale-on-reopen)`() = runTest {
        val f = BinderFixture(testScheduler)
        f.repo.result = { DataResult.Failure(DataError.Io("boom")) }
        val binder = f.binder()

        binder.markDirty()
        advanceUntilIdle() // background save fails → sink reports
        assertEquals(1, f.sink.failures.value.getValue("proj1").failureCount)

        f.repo.result = { DataResult.Success(Unit) }
        binder.markDirty()
        binder.closeProject() // teardown flush succeeds under NonCancellable → listener clears BEFORE cancel
        advanceUntilIdle()

        assertNull(f.sink.failures.value["proj1"]) // cleared synchronously, never lost to teardown
    }

    @Test
    fun `a teardown flush failure is reported exactly once`() = runTest {
        val f = BinderFixture(testScheduler)
        f.repo.result = { DataResult.Failure(DataError.Io("boom")) }
        val binder = f.binder()

        binder.markDirty()
        binder.closeProject() // teardown flush fails → reported once via the listener (no double-count)
        advanceUntilIdle()

        assertEquals(1, f.sink.failures.value.getValue("proj1").failureCount)
    }

    // --- Teardown: idempotent, exactly one flush ---

    @Test
    fun `closeProject is idempotent and returns the same Job`() = runTest {
        val f = BinderFixture(testScheduler)
        val binder = f.binder()

        val j1 = binder.closeProject()
        val j2 = binder.closeProject()

        assertSame(j1, j2)
        advanceUntilIdle()
    }

    @Test
    fun `closeProject performs exactly one teardown flush even when called twice`() = runTest {
        val f = BinderFixture(testScheduler)
        val binder = f.binder()

        binder.markDirty()
        binder.closeProject()
        binder.closeProject() // memoized → no second teardown
        advanceUntilIdle()

        assertEquals(1, f.repo.saves.size)
    }

    @Test
    fun `a lifecycle flush racing teardown yields a single non-concurrent write`() = runTest {
        val f = BinderFixture(testScheduler)
        val binder = f.binder()
        val lifecycle = FakeLifecycle()
        binder.observe(lifecycle)

        binder.markDirty()
        lifecycle.emit(Lifecycle.Event.ON_PAUSE) // flush launched (queued)
        binder.closeProject()                    // closed=true synchronously; teardown launched
        advanceUntilIdle()

        // The lifecycle flush is suppressed by the closed-guard; the teardown flush is the final write.
        assertEquals(1, f.repo.saves.size)
    }

    // --- Closed semantics: no resurrection ---

    @Test
    fun `lifecycle events after close produce no further writes`() = runTest {
        val f = BinderFixture(testScheduler)
        val binder = f.binder()
        val lifecycle = FakeLifecycle()
        binder.observe(lifecycle)

        binder.markDirty()
        binder.closeProject()
        advanceUntilIdle() // teardown flush → save #1
        val afterClose = f.repo.saves.size

        lifecycle.emit(Lifecycle.Event.ON_PAUSE)
        lifecycle.emit(Lifecycle.Event.ON_STOP)
        advanceUntilIdle()

        assertEquals(afterClose, f.repo.saves.size)
    }

    @Test
    fun `markDirty after close produces no further writes`() = runTest {
        val f = BinderFixture(testScheduler)
        val binder = f.binder()

        binder.markDirty()
        binder.closeProject()
        advanceUntilIdle()
        val afterClose = f.repo.saves.size

        binder.markDirty()
        advanceUntilIdle()

        assertEquals(afterClose, f.repo.saves.size)
    }

    // --- NonCancellable teardown ---

    @Test
    fun `teardown flush completes even if its coroutine is cancelled after entering NonCancellable`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val scope = CoroutineScope(dispatcher + SupervisorJob())
            val repo = GatedRepository()
            val sink = InMemorySaveFailureSink()
            val factory = AutosaveCoordinatorFactory(scope, dispatcher, repo, sink, AutosaveConfig())
            val binder = EditorAutosaveBinder(factory, "proj1", { doc(1) }, scope)

            binder.markDirty()
            val job = binder.closeProject()
            runCurrent() // teardown enters NonCancellable, flushNow suspends on the gate (no time elapsed)
            assertTrue(repo.saves.isEmpty())

            job.cancel()             // cancel AFTER entry — NonCancellable must ignore it
            repo.gate.complete(Unit) // release the gated save
            advanceUntilIdle()

            assertEquals(listOf("proj1" to doc(1)), repo.saves) // flush completed despite the cancel
        }

    @Test
    fun `closeProject on an already-cancelled scope does not throw and skips the teardown flush`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val scope = CoroutineScope(dispatcher + SupervisorJob())
            val repo = RecordingRepository()
            val sink = InMemorySaveFailureSink()
            val factory = AutosaveCoordinatorFactory(scope, dispatcher, repo, sink, AutosaveConfig())
            val binder = EditorAutosaveBinder(factory, "proj1", { doc(1) }, scope)

            binder.markDirty()
            scope.cancel() // autosave scope dies before teardown is launched
            advanceUntilIdle()

            val job = binder.closeProject() // must not throw
            advanceUntilIdle()

            assertTrue(job.isCompleted)
            assertTrue(repo.saves.isEmpty()) // body never ran → no teardown flush
        }

    // --- Reopen contract ---

    @Test
    fun `reopen of the same project succeeds after closeProject join`() = runTest {
        val f = BinderFixture(testScheduler)
        val binder = f.binder()

        binder.markDirty()
        binder.closeProject().join() // teardown: close + awaitReleased → registry slot freed

        f.factory.create("proj1") { doc(2) } // re-create accepted (no throw)
    }

    // --- Observer replacement / single-handle rotation ---

    @Test
    fun `re-observe replaces the prior lifecycle observer`() = runTest {
        val f = BinderFixture(testScheduler)
        val binder = f.binder()
        val first = FakeLifecycle()
        val second = FakeLifecycle()

        binder.observe(first)
        assertEquals(1, first.observerCount)

        binder.observe(second)
        assertEquals(0, first.observerCount) // detached from the old lifecycle
        assertEquals(1, second.observerCount)
    }

    @Test
    fun `rotation preserves a single handle owning the project id`() = runTest {
        val f = BinderFixture(testScheduler)
        val binder = f.binder()
        val first = FakeLifecycle()
        val second = FakeLifecycle()

        binder.observe(first)
        binder.observe(second) // config-change rotation; must NOT create a second handle

        binder.markDirty()
        second.emit(Lifecycle.Event.ON_PAUSE)
        runCurrent()
        assertEquals(listOf("proj1" to doc(1)), f.repo.saves) // same coordinator saved once

        // The project id is still singly owned: a direct create for it must be rejected.
        assertThrows(IllegalStateException::class.java) { f.factory.create("proj1") { doc(2) } }
    }

    @Test
    fun `closeProject detaches the observer`() = runTest {
        val f = BinderFixture(testScheduler)
        val binder = f.binder()
        val lifecycle = FakeLifecycle()
        binder.observe(lifecycle)
        assertEquals(1, lifecycle.observerCount)

        binder.closeProject() // explicit close must release the lifecycle reference, not only dispose()

        assertEquals(0, lifecycle.observerCount)
        advanceUntilIdle()
    }

    @Test
    fun `observe after closeProject does not attach a new observer`() = runTest {
        val f = BinderFixture(testScheduler)
        val binder = f.binder()
        binder.closeProject()
        advanceUntilIdle()

        val lifecycle = FakeLifecycle()
        binder.observe(lifecycle) // a closed binder must not re-attach

        assertEquals(0, lifecycle.observerCount)
    }

    // --- dispose ---

    @Test
    fun `dispose detaches the observer and tears the project down`() = runTest {
        val f = BinderFixture(testScheduler)
        val binder = f.binder()
        val lifecycle = FakeLifecycle()
        binder.observe(lifecycle)

        binder.markDirty()
        binder.dispose()
        advanceUntilIdle()

        assertEquals(0, lifecycle.observerCount) // observer detached
        assertEquals(1, f.repo.saves.size)       // teardown flushed the pending edit

        binder.markDirty()
        lifecycle.emit(Lifecycle.Event.ON_PAUSE)
        advanceUntilIdle()
        assertEquals(1, f.repo.saves.size) // inert after dispose
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
 * A binder wired to the virtual clock. [scope] stands in for the app-lifetime `@AutosaveScope` (a
 * [SupervisorJob] with no parent in the test job tree) and is shared by the factory and the binder so
 * `advanceUntilIdle` drives both the coordinators and the binder's launched flush/teardown coroutines.
 */
private class BinderFixture(scheduler: TestCoroutineScheduler) {
    val dispatcher: CoroutineDispatcher = StandardTestDispatcher(scheduler)
    val scope: CoroutineScope = CoroutineScope(dispatcher + SupervisorJob())
    val repo = RecordingRepository()
    val sink = InMemorySaveFailureSink()
    val factory = AutosaveCoordinatorFactory(scope, dispatcher, repo, sink, AutosaveConfig())

    fun binder(
        projectId: String = "proj1",
        provider: DocumentSnapshotProvider = DocumentSnapshotProvider { doc(1) },
    ): EditorAutosaveBinder = EditorAutosaveBinder(factory, projectId, provider, scope)
}

/** Records every save with its project id; [result] decides each call's outcome (default success). */
private class RecordingRepository(
    var result: (String) -> DataResult<Unit> = { DataResult.Success(Unit) },
) : DocumentRepository {
    val saves = mutableListOf<Pair<String, ZineDocument>>()

    override suspend fun load(projectId: String): DataResult<ZineDocument> =
        error("load is not exercised by the binder")

    override suspend fun save(projectId: String, document: ZineDocument): DataResult<Unit> {
        saves += projectId to document
        return result(projectId)
    }
}

/** Save suspends on [gate] until it is completed, giving tests a deterministic mid-flush pause point. */
private class GatedRepository : DocumentRepository {
    val saves = mutableListOf<Pair<String, ZineDocument>>()
    val gate = CompletableDeferred<Unit>()

    override suspend fun load(projectId: String): DataResult<ZineDocument> =
        error("load is not exercised by the binder")

    override suspend fun save(projectId: String, document: ZineDocument): DataResult<Unit> {
        gate.await()
        saves += projectId to document
        return DataResult.Success(Unit)
    }
}

/**
 * Hand-written [Lifecycle] over the pure-JVM lifecycle-common base: records its [LifecycleEventObserver]s
 * and lets a test [emit] events synchronously. No [androidx.lifecycle.LifecycleRegistry] (and thus no
 * main-thread/Robolectric requirement) is involved.
 */
private class FakeLifecycle : Lifecycle(), LifecycleOwner {
    private val observers = mutableListOf<LifecycleEventObserver>()

    override val lifecycle: Lifecycle get() = this
    override val currentState: State get() = State.RESUMED
    val observerCount: Int get() = observers.size

    override fun addObserver(observer: LifecycleObserver) {
        observers += observer as LifecycleEventObserver
    }

    override fun removeObserver(observer: LifecycleObserver) {
        observers.remove(observer)
    }

    fun emit(event: Event) {
        observers.toList().forEach { it.onStateChanged(this, event) }
    }
}
