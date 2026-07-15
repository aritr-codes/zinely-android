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
 * single-flight guard, and retry-reuses-last-destination are verified in pure logic. Robolectric only for
 * `android.net.Uri` in the [ExportReady] case. Given-When-Then.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ExportViewModelTest {

    private val doc = ZineDocument(format = ZineFormat.SINGLE_SHEET_8, paperSize = PaperSize.A4)
    private val size = PtSize(100.0, 100.0)
    private val bytes = AssetBytesSource { null }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    /** Returns [outcome], records the destination of every call, and can [gate] to simulate an in-flight render. */
    private class FakeExporter(private val outcome: ExportOutcome) : SheetExporter {
        val destinations = mutableListOf<ExportDestination>()
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun export(
            document: ZineDocument,
            pageSizePt: PtSize,
            imageBytes: AssetBytesSource,
            format: ExportFormat,
            destination: ExportDestination,
        ): ExportOutcome {
            destinations += destination
            gate?.await()
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
        val saved = ExportSaved("zine.pdf", "Downloads")
        val fake = FakeExporter(saved)
        val vm = ExportViewModel(fake)

        vm.export(doc, size, bytes, ExportFormat.PDF, ExportDestination.DOWNLOADS)

        assertEquals(saved, vm.outcomes.first())
        assertEquals(listOf(ExportDestination.DOWNLOADS), fake.destinations)
    }

    @Test
    fun singleFlightIgnoresTapsWhileWorking() = runTest {
        val fake = FakeExporter(ExportSaved("zine.pdf", "Downloads")).apply { gate = CompletableDeferred() }
        val vm = ExportViewModel(fake)

        vm.export(doc, size, bytes, ExportFormat.PDF, ExportDestination.DOWNLOADS) // enters Working, suspends
        vm.export(doc, size, bytes, ExportFormat.PDF, ExportDestination.TRANSPORT) // ignored while Working

        assertEquals(listOf(ExportDestination.DOWNLOADS), fake.destinations)
        fake.gate!!.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun retryReusesLastDestination() = runTest {
        val fake = FakeExporter(ExportSaved("zine.pdf", "Downloads"))
        val vm = ExportViewModel(fake)

        vm.export(doc, size, bytes, ExportFormat.PDF, ExportDestination.DOWNLOADS)
        advanceUntilIdle()
        vm.retry(doc, size, bytes, ExportFormat.PDF)
        advanceUntilIdle()

        assertEquals(listOf(ExportDestination.DOWNLOADS, ExportDestination.DOWNLOADS), fake.destinations)
    }

    @Test
    fun retryReusesTransportDestination() = runTest {
        // Pin that retry reuses the ACTUAL last destination, not a hardcoded one: a Share retries a Share.
        val fake = FakeExporter(ExportSaved("zine.pdf", "Downloads"))
        val vm = ExportViewModel(fake)

        vm.export(doc, size, bytes, ExportFormat.PDF, ExportDestination.TRANSPORT)
        advanceUntilIdle()
        vm.retry(doc, size, bytes, ExportFormat.PDF)
        advanceUntilIdle()

        assertEquals(listOf(ExportDestination.TRANSPORT, ExportDestination.TRANSPORT), fake.destinations)
    }
}
