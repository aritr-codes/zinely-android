package com.aritr.zinely.data.android.di

import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.DocumentRepository
import com.aritr.zinely.core.data.storage.AutosaveConfig
import com.aritr.zinely.core.data.storage.DocumentSnapshotProvider
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.data.android.AutosaveCoordinatorFactory
import com.aritr.zinely.data.android.InMemorySaveFailureSink
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract for the hand-written [EditorAutosaveBinderFactory] (PR-A Step 7, design §10). The factory
 * adds no behavior — it only forwards its injected collaborators into the frozen
 * [com.aritr.zinely.data.android.EditorAutosaveBinder] constructor — so the test proves the wiring,
 * not autosave semantics (those are covered by the Step 5/6 suites). Runs on the virtual clock; no
 * Hilt runtime needed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditorAutosaveBinderFactoryTest {

    @Test
    fun `create wires the binder to the injected coordinator factory`() = runTest {
        val dispatcher: CoroutineDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val coordinatorFactory =
            AutosaveCoordinatorFactory(scope, dispatcher, RecordingRepository(), InMemorySaveFailureSink(), AutosaveConfig())
        val binderFactory = EditorAutosaveBinderFactory(coordinatorFactory, scope)

        val binder = binderFactory.create("proj1", DocumentSnapshotProvider { doc(1) })
        assertNotNull(binder)

        // The binder eagerly registered proj1 with THIS coordinator factory (proof of correct wiring):
        // a duplicate create for the same id must now fail fast (ADR-026 §2 single-writer guard).
        assertThrows(IllegalStateException::class.java) {
            coordinatorFactory.create("proj1", DocumentSnapshotProvider { doc(1) })
        }

        binder.dispose() // release the eagerly-registered project so the test scope drains cleanly
        advanceUntilIdle()
    }

    // ADR-046 §2: the editor's await-release-before-reopen (ADR-030 Rec1) goes through the SAME
    // AutosaveSessionGate policy the repository's mutation gate uses — one 5 s bound, no drift.

    @Test
    fun `awaitNoSession returns true immediately when the project has no live session`() = runTest {
        val dispatcher: CoroutineDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val coordinatorFactory =
            AutosaveCoordinatorFactory(scope, dispatcher, RecordingRepository(), InMemorySaveFailureSink(), AutosaveConfig())
        val binderFactory = EditorAutosaveBinderFactory(coordinatorFactory, scope)

        assertTrue(binderFactory.awaitNoSession("never-opened"))
    }

    @Test
    fun `awaitNoSession reports busy at the bound while a session stays live`() = runTest {
        val dispatcher: CoroutineDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val coordinatorFactory =
            AutosaveCoordinatorFactory(scope, dispatcher, RecordingRepository(), InMemorySaveFailureSink(), AutosaveConfig())
        val binderFactory = EditorAutosaveBinderFactory(coordinatorFactory, scope)
        val binder = binderFactory.create("proj1", DocumentSnapshotProvider { doc(1) })

        // The session is never closed, so the (virtual) timeout is what answers — false, not a hang.
        assertFalse(binderFactory.awaitNoSession("proj1"))

        binder.dispose()
        advanceUntilIdle()
    }

    @Test
    fun `awaitNoSession rides out an in-flight asynchronous release`() = runTest {
        val dispatcher: CoroutineDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val coordinatorFactory =
            AutosaveCoordinatorFactory(scope, dispatcher, RecordingRepository(), InMemorySaveFailureSink(), AutosaveConfig())
        val binderFactory = EditorAutosaveBinderFactory(coordinatorFactory, scope)
        val binder = binderFactory.create("proj1", DocumentSnapshotProvider { doc(1) })

        // Close the project (the reopen scenario): release is asynchronous — the awaiting reopen must
        // resume true once the teardown completes, well inside the bound.
        binder.dispose()
        assertTrue(binderFactory.awaitNoSession("proj1"))
    }
}

private fun doc(marker: Int): ZineDocument =
    ZineDocument(
        format = ZineFormat.SINGLE_SHEET_8,
        paperSize = PaperSize.LETTER,
        pages = listOf(Page(index = marker, role = PageRole.INTERIOR)),
    )

private class RecordingRepository : DocumentRepository {
    override suspend fun load(projectId: String): DataResult<ZineDocument> =
        error("load is not exercised by the binder factory")

    override suspend fun save(projectId: String, document: ZineDocument): DataResult<Unit> =
        DataResult.Success(Unit)
}
