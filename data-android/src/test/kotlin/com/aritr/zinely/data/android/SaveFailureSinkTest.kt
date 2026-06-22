package com.aritr.zinely.data.android

import com.aritr.zinely.core.data.repository.DataError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Contract for the ADR-026 in-memory [SaveFailureSink]: an application-scoped, per-project keyed
 * holder of background autosave failures. Failures are reported in (a later step collects the
 * coordinator's `failures` flow here), observed via a [StateFlow], and cleared explicitly. Pure
 * JVM — no Android, no coordinator wiring.
 */
class SaveFailureSinkTest {

    private val sink = InMemorySaveFailureSink()

    @Test
    fun `starts empty`() {
        assertTrue(sink.failures.value.isEmpty())
    }

    @Test
    fun `report records the failure for its project with count one`() {
        val error = DataError.Io("disk full")

        sink.report("proj1", error)

        val failure = sink.failures.value.getValue("proj1")
        assertEquals("proj1", failure.projectId)
        assertEquals(error, failure.error)
        assertEquals(1, failure.failureCount)
    }

    @Test
    fun `repeated reports bump the count and keep the latest error`() {
        sink.report("proj1", DataError.Io("first"))
        sink.report("proj1", DataError.Corrupt("second"))

        val failure = sink.failures.value.getValue("proj1")
        assertEquals(DataError.Corrupt("second"), failure.error)
        assertEquals(2, failure.failureCount)
    }

    @Test
    fun `failures are keyed independently per project`() {
        sink.report("proj1", DataError.Io("a"))
        sink.report("proj2", DataError.Unknown("b"))
        sink.report("proj2", DataError.Unknown("c"))

        assertEquals(1, sink.failures.value.getValue("proj1").failureCount)
        assertEquals(2, sink.failures.value.getValue("proj2").failureCount)
    }

    @Test
    fun `clear removes only the given project and resets its count`() {
        sink.report("proj1", DataError.Io("a"))
        sink.report("proj2", DataError.Io("b"))

        sink.clear("proj1")

        assertNull(sink.failures.value["proj1"])
        assertEquals(1, sink.failures.value.getValue("proj2").failureCount)
        // A failure reported after a clear starts counting from one again.
        sink.report("proj1", DataError.Io("c"))
        assertEquals(1, sink.failures.value.getValue("proj1").failureCount)
    }

    @Test
    fun `clear of an unknown project does not emit a new state`() {
        sink.report("proj1", DataError.Io("a"))
        val before = sink.failures.value

        sink.clear("ghost")

        // No change => the StateFlow holds the very same map instance (no spurious recomposition).
        assertSame(before, sink.failures.value)
    }

    @Test
    fun `clearAll discards every recorded failure`() {
        sink.report("proj1", DataError.Io("a"))
        sink.report("proj2", DataError.Io("b"))

        sink.clearAll()

        assertTrue(sink.failures.value.isEmpty())
    }

    @Test
    fun `concurrent reports never lose an update`() {
        // The sink is reported into from the coordinator's IO dispatcher and read on the UI thread;
        // MutableStateFlow.update is lock-free CAS, so concurrent reports must not drop increments.
        val threads = 8
        val perThread = 500
        val pool = Executors.newFixedThreadPool(threads)
        val start = CountDownLatch(1)
        val done = CountDownLatch(threads)
        repeat(threads) {
            pool.execute {
                start.await()
                repeat(perThread) { sink.report("proj1", DataError.Io("x")) }
                done.countDown()
            }
        }
        start.countDown()
        assertTrue(done.await(30, TimeUnit.SECONDS))
        pool.shutdown()

        assertEquals(threads * perThread, sink.failures.value.getValue("proj1").failureCount)
    }
}
