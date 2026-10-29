package com.aritr.zinely.editor

import android.content.Context
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.aritr.zinely.core.data.asset.AssetStore
import com.aritr.zinely.core.data.asset.ContentHash
import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.DocumentRepository
import com.aritr.zinely.core.editor.ink.InkHex
import com.aritr.zinely.core.editor.ink.InkLibrary
import com.aritr.zinely.core.editor.ink.InkLibraryEdit
import com.aritr.zinely.core.imposition.SingleSheet8Imposer
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.data.android.AutosaveCoordinatorFactory
import com.aritr.zinely.data.android.InMemorySaveFailureSink
import com.aritr.zinely.data.android.di.EditorAutosaveBinderFactory
import com.aritr.zinely.data.android.prefs.EditorOnboardingStore
import com.aritr.zinely.data.android.prefs.InkLibraryStore
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * ADR-138's ViewModel seam for My inks and My palettes: the library is exposed load-aware (`null` until the
 * store's first value), and every write is a transform run in the ViewModel's scope, so a screen that leaves
 * mid-write cancels only its own wait, never the write.
 */
@RunWith(RobolectricTestRunner::class)
class EditorInkLibraryTest {

    private val stored = InkLibrary.EMPTY.use(InkHex("1A2B3C")).library

    @Test
    fun `given a store holding a library, when the ViewModel starts, then inkLibrary emits it`() {
        val vm = viewModel(FakeInkLibraryStore(stored))
        idleMain()

        assertEquals(stored, vm.inkLibrary.value)
    }

    @Test
    fun `given a store that has not loaded, when the ViewModel starts, then inkLibrary is null, not empty`() {
        val unloaded = object : InkLibraryStore {
            override val library = emptyFlow<InkLibrary>()
            override suspend fun update(transform: (InkLibrary) -> InkLibrary) = error("not called")
        }
        val vm = viewModel(unloaded)
        idleMain()

        assertNull(vm.inkLibrary.value)
    }

    @Test
    fun `when editInkLibrary runs, then the store holds the transformed value and the call returns before and after`() {
        val store = FakeInkLibraryStore(stored)
        val vm = viewModel(store)
        var edit: InkLibraryEdit? = null

        CoroutineScope(UnconfinedTestDispatcher()).launch {
            edit = vm.editInkLibrary { it.use(InkHex("FF0000")).library }
        }
        idleMain()

        val after = stored.use(InkHex("FF0000")).library
        assertEquals(after, store.state.value)
        assertEquals(InkLibraryEdit(before = stored, after = after), edit)
        assertEquals(after, vm.inkLibrary.value)
    }

    @Test
    fun `given the caller is cancelled during the write, then the write still lands`() {
        val store = FakeInkLibraryStore(stored).apply { gate = CompletableDeferred() }
        val vm = viewModel(store)
        val caller = CoroutineScope(UnconfinedTestDispatcher()).launch {
            vm.editInkLibrary { it.use(InkHex("00FF00")).library }
        }
        idleMain()

        caller.cancel() // the screen leaves while the store is still writing
        store.gate!!.complete(Unit)
        idleMain()

        assertTrue(caller.isCancelled)
        assertEquals(stored.use(InkHex("00FF00")).library, store.state.value)
    }

    private fun idleMain() = shadowOf(Looper.getMainLooper()).idle()

    private fun viewModel(inkStore: InkLibraryStore): EditorViewModel {
        val dispatcher = UnconfinedTestDispatcher()
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val repository = InMemoryRepository()
        val sink = InMemorySaveFailureSink()
        val context: Context = ApplicationProvider.getApplicationContext()
        return EditorViewModel(
            savedStateHandle = SavedStateHandle(mapOf("projectId" to "ink-test")),
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
            shareInbox = ShareInbox(),
            inkLibraryStore = inkStore,
            artShortcutsStore = FakeArtShortcutsStore(),
            assetsDir = File(context.cacheDir, "assets").apply { mkdirs() },
            mainDispatcher = dispatcher,
            ioDispatcher = dispatcher,
        )
    }

    /** A store whose [update] can be held at [gate], to stand for a write still in flight. */
    private class FakeInkLibraryStore(initial: InkLibrary) : InkLibraryStore {
        val state = MutableStateFlow(initial)
        var gate: CompletableDeferred<Unit>? = null
        override val library = state
        override suspend fun update(transform: (InkLibrary) -> InkLibrary): InkLibraryEdit {
            gate?.await()
            val before = state.value
            val after = transform(before)
            state.value = after
            return InkLibraryEdit(before, after)
        }
    }

    private class InMemoryRepository : DocumentRepository {
        private var doc: ZineDocument = blankDocument()
        override suspend fun load(projectId: String) = DataResult.Success(doc)
        override suspend fun save(projectId: String, document: ZineDocument): DataResult<Unit> {
            doc = document
            return DataResult.Success(Unit)
        }
    }

    private class NoAssetStore : AssetStore {
        override suspend fun contains(hash: ContentHash) = false
        override suspend fun store(masterBytes: ByteArray): DataResult<ContentHash> =
            DataResult.Failure(DataError.NotFound("no asset store in this test"))
        override suspend fun read(hash: ContentHash): DataResult<ByteArray> =
            DataResult.Failure(DataError.NotFound(hash.hex))
    }

    private class SeenOnboardingStore : EditorOnboardingStore {
        override val moveResizeHintSeen = flowOf(true)
        override val reframeCoachSeen = flowOf(true)
        override suspend fun markMoveResizeHintSeen() = Unit
        override suspend fun markReframeCoachSeen() = Unit
    }
}
