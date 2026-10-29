package com.aritr.zinely.export

import android.net.Uri
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.render.android.AssetBytesSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for the ADR-054 delivery flow in [ExportViewModel]. A hand [FakeExporter] stands in for the
 * [SheetExporter] seam (no render, no Android IO), so the destination→[ExportOutcome] subtype forwarding,
 * single-flight guard, and retry-reuses-the-FAILED-destination are verified in pure logic. (It said
 * "retry-reuses-last-destination" until ADR-102 §12.14 deleted the `lastDestination` field it named:
 * retry now reads the destination off the `Error` state it is offered against.) Robolectric only for
 * `android.net.Uri` in the [ExportReady] case. Given-When-Then.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ExportViewModelTest {

    private val doc = ZineDocument(format = ZineFormat.SINGLE_SHEET_8, paperSize = PaperSize.A4)
    private val size = PtSize(100.0, 100.0)
    private val bytes = AssetBytesSource { null }
    private fun saved(): ExportSaved = ExportSaved(
        uri = Uri.parse("content://zinely/downloads/1"),
        mime = "application/pdf",
        displayName = "zine.pdf",
        location = "Downloads",
    )

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    /**
     * Returns [outcome], records the destination of every call, and can [gate] to simulate an in-flight
     * render. [failFirst] makes the first N calls throw, which is the only way to reach the `Error` state
     * — and therefore the only way to reach `retry` at all, since ADR-102 stopped retry reading a
     * remembered field and made it read the error it is offered against.
     */
    private class FakeExporter(
        private val outcome: ExportOutcome,
        private val failFirst: Int = 0,
    ) : SheetExporter {
        val destinations = mutableListOf<ExportDestination>()
        val titles = mutableListOf<String>()
        var gate: CompletableDeferred<Unit>? = null
        private var calls = 0
        override suspend fun export(
            document: ZineDocument,
            pageSizePt: PtSize,
            imageBytes: AssetBytesSource,
            format: ExportFormat,
            destination: ExportDestination,
            title: String,
        ): ExportOutcome {
            destinations += destination
            titles += title
            gate?.await()
            if (calls++ < failFirst) throw java.io.IOException("render failed")
            return outcome
        }
    }

    @Test
    fun transportEmitsExportReady() = runTest {
        val ready = ExportReady(Uri.parse("content://zinely/1"), "application/pdf")
        val fake = FakeExporter(ready)
        val vm = ExportViewModel(fake)

        // The eager export buffers its outcome; first() drains the one emission deterministically.
        vm.export(doc, size, bytes, ExportFormat.PDF, ExportDestination.TRANSPORT)

        assertEquals(ready, vm.outcomes.first())
        assertEquals(listOf(ExportDestination.TRANSPORT), fake.destinations)
    }

    @Test
    fun downloadsEmitsExportSaved() = runTest {
        val saved = saved()
        val fake = FakeExporter(saved)
        val vm = ExportViewModel(fake)

        vm.export(doc, size, bytes, ExportFormat.PDF, ExportDestination.DOWNLOADS)

        assertEquals(saved, vm.outcomes.first())
        assertEquals(listOf(ExportDestination.DOWNLOADS), fake.destinations)
    }

    @Test
    fun exportHandsTheProjectTitleToTheExporter() = runTest {
        val fake = FakeExporter(saved())
        val vm = ExportViewModel(fake, freeBytes = { Long.MAX_VALUE }, titleOf = { "My Zine" })

        vm.export(doc, size, bytes, ExportFormat.PDF, ExportDestination.DOWNLOADS)

        assertEquals(listOf("My Zine"), fake.titles)
    }

    @Test
    fun singleFlightIgnoresTapsWhileWorking() = runTest {
        val fake = FakeExporter(saved()).apply { gate = CompletableDeferred() }
        val vm = ExportViewModel(fake)

        vm.export(doc, size, bytes, ExportFormat.PDF, ExportDestination.DOWNLOADS) // enters Working, suspends
        vm.export(doc, size, bytes, ExportFormat.PDF, ExportDestination.TRANSPORT) // ignored while Working

        assertEquals(listOf(ExportDestination.DOWNLOADS), fake.destinations)
        fake.gate!!.complete(Unit)
        advanceUntilIdle()
    }

    /**
     * Retry reproduces the failed attempt — and reads which attempt that was **off the error being
     * shown**, not off a remembered field.
     *
     * These two cases used to retry after a *success*, which no UI can do: "Try again" exists only on the
     * error overlay. That made them pass against `lastDestination`, a field that could outlive the error it
     * described. Both now go through the real path: a failure, an error carrying its destination, a retry.
     */
    @Test
    fun retryReusesTheFailedSaveDestination() = runTest {
        val fake = FakeExporter(saved(), failFirst = 1)
        val vm = ExportViewModel(fake)

        vm.export(doc, size, bytes, ExportFormat.PDF, ExportDestination.DOWNLOADS)
        advanceUntilIdle()
        assertEquals(ExportDestination.DOWNLOADS, (vm.state.value as ExportUiState.Error).destination)

        vm.retry(doc, size, bytes, ExportFormat.PDF)
        advanceUntilIdle()

        assertEquals(listOf(ExportDestination.DOWNLOADS, ExportDestination.DOWNLOADS), fake.destinations)
    }

    @Test
    fun retryReusesTheFailedShareDestination() = runTest {
        // A Share retries a Share — the destination is the ACTUAL failed one, never a hardcoded default.
        val fake = FakeExporter(saved(), failFirst = 1)
        val vm = ExportViewModel(fake)

        vm.export(doc, size, bytes, ExportFormat.PDF, ExportDestination.TRANSPORT)
        advanceUntilIdle()
        vm.retry(doc, size, bytes, ExportFormat.PDF)
        advanceUntilIdle()

        assertEquals(listOf(ExportDestination.TRANSPORT, ExportDestination.TRANSPORT), fake.destinations)
    }

    /** With nothing failed, there is nothing to try again — and no stale field to try it with. */
    @Test
    fun retryWithoutAFailureDoesNothing() = runTest {
        val fake = FakeExporter(saved())
        val vm = ExportViewModel(fake)

        vm.export(doc, size, bytes, ExportFormat.PDF, ExportDestination.DOWNLOADS)
        advanceUntilIdle()
        vm.retry(doc, size, bytes, ExportFormat.PDF)
        advanceUntilIdle()

        assertEquals(listOf(ExportDestination.DOWNLOADS), fake.destinations)
    }

    /** The state names the running action, which is what lets the two commit buttons stop sharing one. */
    @Test
    fun workingStateNamesItsDestination() = runTest {
        val fake = FakeExporter(saved()).apply { gate = CompletableDeferred() }
        val vm = ExportViewModel(fake)

        vm.export(doc, size, bytes, ExportFormat.PDF, ExportDestination.TRANSPORT)

        assertEquals(ExportDestination.TRANSPORT, (vm.state.value as ExportUiState.Working).destination)
        fake.gate!!.complete(Unit)
        advanceUntilIdle()
    }

    /**
     * A Save PDF the host could not even start (Android 7–9 storage permission denied, ADR-054 §8) shows
     * the same recoverable error a failed save shows, names Save as the failed action, and renders nothing.
     */
    @Test
    fun couldNotStartShowsTheSaveErrorWithoutRendering() = runTest {
        val fake = FakeExporter(saved())
        val vm = ExportViewModel(fake)

        vm.couldNotStart(ExportDestination.DOWNLOADS)

        val error = vm.state.value as ExportUiState.Error
        assertEquals(ExportDestination.DOWNLOADS, error.destination)
        assertEquals("Couldn’t make your file just now. Please try again.", error.message)
        assertEquals(emptyList<ExportDestination>(), fake.destinations)
    }

    // ---- A43 / D-142: the error names its cause -------------------------------------------------

    /** An exporter that always throws [failure], to reach each cause through the real catch blocks. */
    private class ThrowingExporter(private val failure: Throwable) : SheetExporter {
        override suspend fun export(
            document: ZineDocument,
            pageSizePt: PtSize,
            imageBytes: AssetBytesSource,
            format: ExportFormat,
            destination: ExportDestination,
            title: String,
        ): ExportOutcome = throw failure
    }

    @Test
    fun oom_is_reported_as_out_of_memory() = runTest {
        // Given the sheet render runs out of memory
        val vm = ExportViewModel(ThrowingExporter(OutOfMemoryError("sheet")), freeBytes = { Long.MAX_VALUE })

        // When Save PDF runs
        vm.export(doc, size, bytes, ExportFormat.PDF, ExportDestination.DOWNLOADS)
        advanceUntilIdle()

        // Then the error says memory
        assertEquals(ExportFailure.OutOfMemory, (vm.state.value as ExportUiState.Error).failure)
    }

    @Test
    fun io_failure_with_low_space_is_out_of_space() = runTest {
        // Given the write fails and the phone has no usable space left
        val vm = ExportViewModel(ThrowingExporter(java.io.IOException("write failed")), freeBytes = { 0L })

        vm.export(doc, size, bytes, ExportFormat.PDF, ExportDestination.DOWNLOADS)
        advanceUntilIdle()

        assertEquals(ExportFailure.OutOfSpace, (vm.state.value as ExportUiState.Error).failure)
    }

    /** The same failure with room to spare is not a space problem: never claim one the disk disproves. */
    @Test
    fun io_failure_with_room_to_spare_stays_generic() = runTest {
        val vm = ExportViewModel(
            ThrowingExporter(java.io.IOException("write failed")),
            freeBytes = { 10L * 1024 * 1024 * 1024 },
        )

        vm.export(doc, size, bytes, ExportFormat.PDF, ExportDestination.DOWNLOADS)
        advanceUntilIdle()

        assertEquals(ExportFailure.Other, (vm.state.value as ExportUiState.Error).failure)
    }

    /** A probe that cannot answer must not invent a storage-full claim. */
    @Test
    fun io_failure_with_a_broken_probe_stays_generic() = runTest {
        val vm = ExportViewModel(
            ThrowingExporter(java.io.IOException("write failed")),
            freeBytes = { throw SecurityException("no statfs") },
        )

        vm.export(doc, size, bytes, ExportFormat.PDF, ExportDestination.DOWNLOADS)
        advanceUntilIdle()

        assertEquals(ExportFailure.Other, (vm.state.value as ExportUiState.Error).failure)
    }

    /** Low space with a non-IO failure is not a space problem either: only a failed write is. */
    @Test
    fun non_io_failure_with_low_space_stays_generic() = runTest {
        val vm = ExportViewModel(ThrowingExporter(IllegalStateException("bad layout")), freeBytes = { 0L })

        vm.export(doc, size, bytes, ExportFormat.PDF, ExportDestination.DOWNLOADS)
        advanceUntilIdle()

        assertEquals(ExportFailure.Other, (vm.state.value as ExportUiState.Error).failure)
    }

    @Test
    fun denied_permission_is_permission_denied() = runTest {
        // Given Android 7–9 refused Save PDF's storage permission
        val vm = ExportViewModel(FakeExporter(saved()), freeBytes = { Long.MAX_VALUE })

        // When the host reports it could not start the save
        vm.couldNotStart(ExportDestination.DOWNLOADS)

        // Then the error says permission
        assertEquals(ExportFailure.PermissionDenied, (vm.state.value as ExportUiState.Error).failure)
    }

    /** Like a second tap, it never interrupts a render already in flight. */
    @Test
    fun couldNotStartLeavesARenderInFlightAlone() = runTest {
        val fake = FakeExporter(saved()).apply { gate = CompletableDeferred() }
        val vm = ExportViewModel(fake)

        vm.export(doc, size, bytes, ExportFormat.PDF, ExportDestination.TRANSPORT)
        vm.couldNotStart(ExportDestination.DOWNLOADS)

        assertEquals(ExportDestination.TRANSPORT, (vm.state.value as ExportUiState.Working).destination)
        fake.gate!!.complete(Unit)
        advanceUntilIdle()
    }
}
