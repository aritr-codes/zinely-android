package com.aritr.zinely.editor

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.aritr.zinely.core.data.asset.AssetStore
import com.aritr.zinely.core.data.asset.ContentHash
import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.DocumentRepository
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.imposition.SingleSheet8Imposer
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.data.android.AutosaveCoordinatorFactory
import com.aritr.zinely.data.android.InMemorySaveFailureSink
import com.aritr.zinely.data.android.di.EditorAutosaveBinderFactory
import com.aritr.zinely.data.android.prefs.EditorOnboardingStore
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * C9 row 9.3 — **persistence of place, page half**
 * ([ADR-097](../../../../../../docs/DECISIONS.md#adr-097);
 * [§E.4](../../../../../../docs/design/V2-BENCH-REVIEW.md) calls it *"a build invariant, freeze-blocking
 * for the Compose build"*).
 *
 * Two things have to be true, and they fail in different ways, so they are asserted separately:
 *
 * 1. **The index survives process death.** Exercised through a real `SavedStateHandle` round-tripped
 *    across its own saved-state provider — which is precisely what the platform does — rather than
 *    through a mock that would only prove the key was spelled consistently.
 * 2. **A remembered index cannot outlive the pages it referred to.** [restoredPageIndex]'s clamp.
 *
 * The **shelf** half is excluded: [OD-2](../../../../../../docs/DECISIONS.md#adr-089) re-seated H1, and
 * §E.4's invariant carries forward to the package that builds the shelf, unweakened.
 */
@RunWith(RobolectricTestRunner::class)
class EditorPagePersistenceTest {

    /**
     * Given the maker is on page 4, when the process dies and the handle is restored from its own saved
     * bundle, then the index is still 3.
     *
     * The mutation this exists to catch — *always restore page 1* — turns this into `0`.
     */
    @Test
    fun `the page index survives process death`() {
        val before = SavedStateHandle()
        before[KEY_PAGE_INDEX] = 3

        // What the platform does: the handle writes itself into a bundle, the process dies, a new handle
        // is built from that bundle. Nothing of the old one survives except what it saved.
        val after = SavedStateHandle.createHandle(before.savedStateProvider().saveState(), null)

        assertEquals(3, after.get<Int>(KEY_PAGE_INDEX))
    }

    /** A first open has nothing remembered, and lands where it always did. */
    @Test
    fun `a project opened for the first time lands on page 1`() {
        assertEquals(0, restoredPageIndex(saved = null, pageCount = 8))
    }

    /** Given a remembered page 8, when the document now has 4 pages, then the index clamps to the last. */
    @Test
    fun `a remembered index cannot outlive the pages it referred to`() {
        assertEquals(3, restoredPageIndex(saved = 7, pageCount = 4))
        assertEquals(0, restoredPageIndex(saved = 7, pageCount = 1))
        // An empty document is defended rather than assumed impossible: the reversed range a bare
        // coerceIn would build here throws, and a missing page is not worth a crash.
        assertEquals(0, restoredPageIndex(saved = 7, pageCount = 0))
        // Negative is not reachable through the store, but the clamp is the guard, so it is the guard.
        assertEquals(0, restoredPageIndex(saved = -2, pageCount = 8))
    }

    /** The index the maker actually left is returned untouched — the clamp is a guard, not a policy. */
    @Test
    fun `an index inside the document is restored exactly`() {
        assertEquals(5, restoredPageIndex(saved = 5, pageCount = 8))
    }

    // =================================================================================================
    // The wiring — the row itself, not its helper
    // =================================================================================================

    /**
     * The tests above prove the *pieces*. Independent review pointed out that they prove nothing about
     * the **row**: reverting `ready(EditorModel(doc, currentPageIndex = restored))` to
     * `ready(EditorModel(doc))`, or deleting the write-back collector outright, left all four green.
     * A helper with no caller is not persistence of place.
     *
     * So these two drive the real [EditorViewModel] against fakes and assert the two directions
     * separately — reading at boot, and writing on navigation.
     */
    private fun viewModel(handle: SavedStateHandle): EditorViewModel {
        val dispatcher = UnconfinedTestDispatcher()
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val repository = InMemoryRepository()
        val sink = InMemorySaveFailureSink()
        val context: Context = ApplicationProvider.getApplicationContext()
        return EditorViewModel(
            savedStateHandle = handle,
            repository = repository,
            binderFactory = EditorAutosaveBinderFactory(
                AutosaveCoordinatorFactory(scope, dispatcher, repository, sink),
                scope,
            ),
            saveFailureSink = sink,
            imposer = SingleSheet8Imposer(),
            assetStore = NoAssetStore(),
            imageDecoder = ImportMasterDecoder(context.contentResolver),
            onboardingStore = SeenOnboardingStore(),
            assetsDir = File(context.cacheDir, "assets").apply { mkdirs() },
            mainDispatcher = dispatcher,
            ioDispatcher = dispatcher,
        )
    }

    /**
     * Given the handle remembers page 4 — which is all that survives process death — when the editor
     * boots, then the **store** opens on page 4.
     *
     * Asserted through `store.uiState`, never through the handle, because [ADR-005](../../../../../../docs/DECISIONS.md#adr-005)
     * makes the store the single state owner: the handle is a carrier, and a test that read the answer
     * back out of the carrier would pass with the wiring removed.
     */
    @Test
    fun `the editor boots on the page the handle remembers`() {
        val handle = routeHandle()
        handle[KEY_PAGE_INDEX] = 3

        val ready = viewModel(handle).bootState.value as EditorBootState.Ready

        assertEquals(3, ready.store.uiState.value.currentPageIndex)
    }

    /** And with nothing remembered, page 1 — so the test above is not passing on a coincidence. */
    @Test
    fun `the editor boots on page 1 when nothing is remembered`() {
        val ready = viewModel(routeHandle()).bootState.value as EditorBootState.Ready

        assertEquals(0, ready.store.uiState.value.currentPageIndex)
    }

    /** Given the maker turns to page 3, when they do, then the handle carries 2 — the write half. */
    @Test
    fun `turning the page writes the maker's place into the handle`() {
        val handle = routeHandle()
        val ready = viewModel(handle).bootState.value as EditorBootState.Ready

        ready.store.dispatch(Intent.GoToPage(2))

        assertEquals(2, handle.get<Int>(KEY_PAGE_INDEX))
    }

    private fun routeHandle() = SavedStateHandle(mapOf("projectId" to "c9-test"))

    private class InMemoryRepository : DocumentRepository {
        private var stored: ZineDocument = blankDocument()
        override suspend fun load(projectId: String) = DataResult.Success(stored)
        override suspend fun save(projectId: String, document: ZineDocument): DataResult<Unit> {
            stored = document
            return DataResult.Success(Unit)
        }
    }

    /** No import happens in these tests; the pipeline is constructed, never driven. */
    private class NoAssetStore : AssetStore {
        override suspend fun contains(hash: ContentHash) = false
        override suspend fun store(masterBytes: ByteArray): DataResult<ContentHash> =
            DataResult.Failure(DataError.NotFound("no asset store in this test"))
        override suspend fun read(hash: ContentHash): DataResult<ByteArray> =
            DataResult.Failure(DataError.NotFound(hash.hex))
    }

    /** Everything already taught, so no coach-mark work runs during boot. */
    private class SeenOnboardingStore : EditorOnboardingStore {
        override val moveResizeHintSeen = flowOf(true)
        override val reframeCoachSeen = flowOf(true)
        override suspend fun markMoveResizeHintSeen() = Unit
        override suspend fun markReframeCoachSeen() = Unit
    }
}
