package com.aritr.zinely.data.android

import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.DocumentRepository
import com.aritr.zinely.core.data.storage.AutosaveConfig
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Contract for the ADR-026 / PR-A Step 5 [AutosaveCoordinatorFactory]: the ownership boundary that
 * assembles one autosave unit per project (coordinator + repository saver + failure→sink collector)
 * and enforces a single active coordinator per project id (ADR-021 single writer). This is pure
 * composition over the frozen [AutosaveCoordinator][com.aritr.zinely.core.data.storage.AutosaveCoordinator];
 * lifecycle teardown policy and the NonCancellable orchestration stay in Step 6.
 *
 * Tests run on the virtual clock (a shared [TestCoroutineScheduler]) like the coordinator's own
 * suite: the factory's `@AutosaveScope` and io dispatcher are a [StandardTestDispatcher] over the
 * test scheduler, so `advanceUntilIdle` drives every debounce/cap timer and failure delivery.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AutosaveCoordinatorFactoryTest {

    // --- Composition: saves route through the repository with the bound project id ---

    @Test
    fun `markDirty autosaves the provider snapshot through the repository for the bound project`() =
        runTest {
            val f = Fixture(testScheduler)
            val handle = f.factory().create("proj1") { doc(1) }

            handle.markDirty()
            advanceUntilIdle() // debounce elapses → background save runs

            assertEquals(listOf("proj1" to doc(1)), f.repo.saves)
        }

    @Test
    fun `a background save failure is reported to the sink under the project id`() = runTest {
        val f = Fixture(testScheduler)
        f.repo.result = { DataResult.Failure(DataError.Io("boom")) }
        val handle = f.factory().create("proj1") { doc(1) }

        handle.markDirty()
        advanceUntilIdle()

        val failure = f.sink.failures.value.getValue("proj1")
        assertEquals(DataError.Io("boom"), failure.error)
        assertEquals(1, failure.failureCount)
    }

    @Test
    fun `two projects route saves and failures independently`() = runTest {
        val f = Fixture(testScheduler)
        f.repo.result = { id -> if (id == "p2") DataResult.Failure(DataError.Io("x")) else DataResult.Success(Unit) }
        val factory = f.factory()

        factory.create("p1") { doc(1) }.markDirty()
        factory.create("p2") { doc(2) }.markDirty()
        advanceUntilIdle()

        assertTrue(f.repo.saves.contains("p1" to doc(1)))
        assertTrue(f.repo.saves.contains("p2" to doc(2)))
        assertNull(f.sink.failures.value["p1"]) // p1 succeeded → no failure recorded
        assertEquals(DataError.Io("x"), f.sink.failures.value.getValue("p2").error)
    }

    // --- flushNow: returned to caller, never reported to the sink ---

    @Test
    fun `flushNow persists the dirty snapshot synchronously and returns the repository result`() =
        runTest {
            val f = Fixture(testScheduler)
            val handle = f.factory().create("proj1") { doc(1) }

            handle.markDirty()
            val result = handle.flushNow() // saves immediately, without advancing the debounce

            assertTrue(result.isSuccess)
            assertEquals(listOf("proj1" to doc(1)), f.repo.saves)
        }

    @Test
    fun `flushNow failure is returned to the caller and not reported to the sink`() = runTest {
        val f = Fixture(testScheduler)
        f.repo.result = { DataResult.Failure(DataError.Io("disk full")) }
        val handle = f.factory().create("proj1") { doc(1) }

        handle.markDirty()
        val result = handle.flushNow() // no advanceUntilIdle: only the direct flush path runs

        assertTrue(result.isFailure)
        assertNull(f.sink.failures.value["proj1"]) // flushNow failures never traverse `failures`
    }

    // --- Ownership boundary: one active coordinator per project ---

    @Test
    fun `create rejects a duplicate active project id`() = runTest {
        val f = Fixture(testScheduler)
        val factory = f.factory()
        factory.create("proj1") { doc(1) }

        assertThrows(IllegalStateException::class.java) {
            factory.create("proj1") { doc(2) }
        }
    }

    @Test
    fun `concurrent creates for one id yield exactly one active handle`() {
        // The registry is the single-writer guard (ADR-021); under contention the synchronized
        // check-and-insert must admit exactly one winner and reject the rest. Not a runTest: real
        // threads race create(). The scope's StandardTestDispatcher is never advanced, so the
        // per-handle collector coroutines stay parked — only the registry serialization is exercised.
        val dispatcher = StandardTestDispatcher(TestCoroutineScheduler())
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val factory =
            AutosaveCoordinatorFactory(scope, dispatcher, FakeRepository(), InMemorySaveFailureSink())

        val threads = 16
        val pool = Executors.newFixedThreadPool(threads)
        val start = CountDownLatch(1)
        val done = CountDownLatch(threads)
        val successes = AtomicInteger(0)
        val rejections = AtomicInteger(0)
        repeat(threads) {
            pool.execute {
                start.await()
                try {
                    factory.create("proj1") { doc(1) }
                    successes.incrementAndGet()
                } catch (_: IllegalStateException) {
                    rejections.incrementAndGet()
                } finally {
                    done.countDown()
                }
            }
        }
        start.countDown()
        assertTrue(done.await(30, TimeUnit.SECONDS))
        pool.shutdown()

        assertEquals(1, successes.get())
        assertEquals(threads - 1, rejections.get())
        scope.cancel()
    }

    @Test
    fun `cancel releases the project id`() = runTest {
        val f = Fixture(testScheduler)
        val factory = f.factory()
        val handle = factory.create("proj1") { doc(1) }

        handle.cancel()
        advanceUntilIdle() // project job completes → invokeOnCompletion unregisters

        // No longer a duplicate: a fresh create for the same id must not throw.
        factory.create("proj1") { doc(2) }
    }

    @Test
    fun `a handle created after cancel autosaves normally`() = runTest {
        val f = Fixture(testScheduler)
        val factory = f.factory()
        factory.create("proj1") { doc(1) }.cancel()
        advanceUntilIdle()

        val reborn = factory.create("proj1") { doc(2) }
        reborn.markDirty()
        advanceUntilIdle()

        assertEquals(listOf("proj1" to doc(2)), f.repo.saves)
    }

    // --- Teardown ---

    @Test
    fun `parent scope cancellation tears down the project coordinator`() = runTest {
        val f = Fixture(testScheduler)
        val handle = f.factory().create("proj1") { doc(1) }

        f.parent.cancel() // @AutosaveScope dies
        advanceUntilIdle()

        handle.markDirty()
        advanceUntilIdle()

        assertTrue(f.repo.saves.isEmpty()) // the torn-down coordinator no longer saves
    }

    @Test
    fun `failure collection stops after the handle is cancelled`() = runTest {
        val f = Fixture(testScheduler)
        f.repo.result = { DataResult.Failure(DataError.Io("boom")) }
        val handle = f.factory().create("proj1") { doc(1) }

        handle.markDirty()
        advanceUntilIdle()
        assertEquals(1, f.sink.failures.value.getValue("proj1").failureCount)

        handle.cancel()
        advanceUntilIdle()

        handle.markDirty() // after cancel: collector is gone
        advanceUntilIdle()
        assertEquals(1, f.sink.failures.value.getValue("proj1").failureCount) // unchanged
    }

    @Test
    fun `close flushes the pending snapshot then tears the handle down`() = runTest {
        val f = Fixture(testScheduler)
        val factory = f.factory()
        val handle = factory.create("proj1") { doc(1) }

        handle.markDirty()
        val result = handle.close()

        assertTrue(result.isSuccess)
        assertEquals(listOf("proj1" to doc(1)), f.repo.saves) // flushed before teardown
        advanceUntilIdle()

        handle.markDirty() // closed handle is inert
        advanceUntilIdle()
        assertEquals(1, f.repo.saves.size)

        factory.create("proj1") { doc(2) } // id was released by close → no throw
    }

    @Test
    fun `create after parent scope cancellation fails fast`() = runTest {
        val f = Fixture(testScheduler)
        val factory = f.factory()
        f.parent.cancel()
        advanceUntilIdle()

        assertThrows(IllegalStateException::class.java) {
            factory.create("proj1") { doc(1) }
        }
    }

    // --- Release-completion contract (awaitReleased) ---

    @Test
    fun `awaitReleased completes after close and frees the id`() = runTest {
        val f = Fixture(testScheduler)
        val factory = f.factory()
        val handle = factory.create("proj1") { doc(1) }

        handle.close()
        handle.awaitReleased() // suspends until project job completes + unregister has run

        factory.create("proj1") { doc(2) } // id is released → no throw
    }

    @Test
    fun `awaitReleased completes after cancel and frees the id`() = runTest {
        val f = Fixture(testScheduler)
        val factory = f.factory()
        val handle = factory.create("proj1") { doc(1) }

        handle.cancel()
        handle.awaitReleased()

        factory.create("proj1") { doc(2) } // id is released → no throw
    }

    @Test
    fun `awaitReleased completes after parent scope cancellation`() = runTest {
        val f = Fixture(testScheduler)
        val handle = f.factory().create("proj1") { doc(1) }

        f.parent.cancel() // @AutosaveScope dies → project job (its child) completes

        handle.awaitReleased() // returns once the project job completes and unregister has run
    }

    @Test
    fun `create for the same id fails before release completes`() = runTest {
        val f = Fixture(testScheduler)
        val factory = f.factory()
        val handle = factory.create("proj1") { doc(1) }

        handle.cancel() // teardown initiated; release is async (project job not yet complete)

        // No advance/await: the project job's collector child has not been dispatched, so the job
        // is still cancelling (not completed) → unregister has not run → the id is still active.
        assertThrows(IllegalStateException::class.java) {
            factory.create("proj1") { doc(2) }
        }
    }

    @Test
    fun `create for the same id succeeds after awaiting release`() = runTest {
        val f = Fixture(testScheduler)
        val factory = f.factory()
        val handle = factory.create("proj1") { doc(1) }

        handle.cancel()
        handle.awaitReleased() // gate on release completion → unregister guaranteed to have run

        val reborn = factory.create("proj1") { doc(2) } // accepted now
        reborn.markDirty()
        advanceUntilIdle()
        assertEquals(listOf("proj1" to doc(2)), f.repo.saves)
    }

    @Test
    fun `factory construction requires a scope carrying a Job`() {
        val jobless = object : CoroutineScope {
            override val coroutineContext = StandardTestDispatcher()
        }
        assertThrows(IllegalArgumentException::class.java) {
            AutosaveCoordinatorFactory(
                autosaveScope = jobless,
                ioDispatcher = StandardTestDispatcher(),
                repository = FakeRepository(),
                failureSink = InMemorySaveFailureSink(),
            )
        }
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
 * A factory wired to the virtual clock. [parent] stands in for the app-lifetime `@AutosaveScope`
 * (a [SupervisorJob] with no parent in the test's job tree, so the runner neither awaits nor flags
 * the coordinators' forever-loops); [dispatcher] is the injected io dispatcher over the same
 * scheduler so `advanceUntilIdle` drives the coordinators.
 */
private class Fixture(scheduler: TestCoroutineScheduler) {
    val dispatcher: CoroutineDispatcher = StandardTestDispatcher(scheduler)
    val parent: CoroutineScope = CoroutineScope(dispatcher + SupervisorJob())
    val repo = FakeRepository()
    val sink = InMemorySaveFailureSink()

    fun factory(config: AutosaveConfig = AutosaveConfig()): AutosaveCoordinatorFactory =
        AutosaveCoordinatorFactory(parent, dispatcher, repo, sink, config)
}

/** Records every save with its project id; [result] decides each call's outcome (default success). */
private class FakeRepository(
    var result: (String) -> DataResult<Unit> = { DataResult.Success(Unit) },
) : DocumentRepository {
    val saves = mutableListOf<Pair<String, ZineDocument>>()

    override suspend fun load(projectId: String): DataResult<ZineDocument> =
        error("load is not exercised by the factory")

    override suspend fun save(projectId: String, document: ZineDocument): DataResult<Unit> {
        saves += projectId to document
        return result(projectId)
    }
}
