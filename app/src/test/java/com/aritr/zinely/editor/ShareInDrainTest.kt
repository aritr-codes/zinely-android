package com.aritr.zinely.editor

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.aritr.zinely.core.data.asset.AssetStore
import com.aritr.zinely.core.data.asset.ContentHash
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.DocumentRepository
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.Intent as EditorIntent
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.data.android.AutosaveCoordinatorFactory
import com.aritr.zinely.data.android.InMemorySaveFailureSink
import com.aritr.zinely.data.android.di.EditorAutosaveBinderFactory
import com.aritr.zinely.data.android.prefs.EditorOnboardingStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest

/**
 * **The share-in drain** ([ShareInbox] → [EditorViewModel] → the shipped ADR-031 §5 import path).
 *
 * The pure acceptance rule is covered by [ShareInboxTest]; this suite exists because independent review
 * observed that *"the half that can lose photos is untested"* — and it was right. Everything here drives
 * the real [EditorViewModel] against fakes, with a real Skia decode behind a shadow `ContentResolver`
 * (the [ImportMasterDecoderTest] recipe), and asserts on the **document**, never on the inbox: an
 * assertion that the queue emptied would pass with the whole import removed.
 *
 * Given-When-Then.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShareInDrainTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Given one photo shared into Zinely, when a zine opens, then it is on the page. */
    @Test
    fun `a shared photo becomes an element of the zine that opens`() {
        val inbox = ShareInbox()
        inbox.offer(listOf(registerPng()))

        val ready = viewModel(inbox).bootState.value as EditorBootState.Ready

        assertEquals(1, ready.imagesOnCurrentPage())
    }

    /** Given three photos in one share, when a zine opens, then all three arrive — none dropped. */
    @Test
    fun `a multi-image share imports every photo`() {
        val inbox = ShareInbox()
        inbox.offer(listOf(registerPng(), registerPng(), registerPng()))

        val ready = viewModel(inbox).bootState.value as EditorBootState.Ready

        assertEquals(3, ready.imagesOnCurrentPage())
    }

    /**
     * Given a photo shared **into an already-open zine**, when it arrives, then it lands there — the
     * collector is live for the ViewModel's whole life, not only at boot.
     */
    @Test
    fun `a share arriving mid-session lands in the open zine`() {
        val inbox = ShareInbox()
        val ready = viewModel(inbox).bootState.value as EditorBootState.Ready
        assertEquals(0, ready.imagesOnCurrentPage())

        inbox.offer(listOf(registerPng()))

        assertEquals(1, ready.imagesOnCurrentPage())
    }

    /**
     * Given a photo shared **while an earlier share is still importing**, when the import finishes, then
     * the newcomer is imported too rather than stranded.
     *
     * ⚠ **This is the `StateFlow` conflation window, and it is the whole reason the drain loops.** A
     * `StateFlow` re-emits only when the current value differs from the one *that collector* last observed.
     * The re-entrant share below restores the value to a list `equals` to the one already seen while the
     * body is mid-import, so when the body returns there is nothing to emit and those URIs sit in the inbox
     * forever with nothing said about them. Replace the `while` loop with a single `takeAll()` per emission
     * and this test — and only this test — fails.
     *
     * The re-entry is staged through the asset store because that is a real suspension point *inside* the
     * import, which is exactly where a share from another app can land. It re-shares **the same photo** on
     * purpose: that is what restores the flow's value to one `equals` to the last observed, which is the
     * only shape conflation can swallow. Sharing the same picture into the same zine twice is also an
     * entirely ordinary thing for a person to do.
     */
    @Test
    fun `a share arriving mid-import is not conflated away`() {
        val inbox = ShareInbox()
        val first = registerPng()
        // Fires once, while the first photo is being written — i.e. the collector is inside its own body,
        // which is the only state in which conflation can swallow an offer.
        val store = InMemoryAssetStore(onFirstStore = { inbox.offer(listOf(first)) })
        val ready = viewModel(inbox, store).bootState.value as EditorBootState.Ready

        inbox.offer(listOf(first))

        val placed = ready.imageTransforms()
        assertEquals(2, placed.size)
        assertOneCascadeStep(placed[0], placed[1])
    }

    /** D-081 Q10: separate share events continue the page's photo cascade instead of restarting it. */
    @Test
    fun `two consecutive single-photo shares continue one page cascade`() {
        val inbox = ShareInbox()
        val ready = viewModel(inbox).bootState.value as EditorBootState.Ready

        inbox.offer(listOf(registerPng()))
        val first = ready.imageTransforms().single()
        inbox.offer(listOf(registerPng()))

        val placed = ready.imageTransforms()
        assertEquals(2, placed.size)
        assertEquals(first, placed[0])
        assertOneCascadeStep(placed[0], placed[1])
        assertWhollyOnPage(placed, ready.pageSizePt)
    }

    /** A13 is photo-specific: existing text must not push the first shared photo off its centred default. */
    @Test
    fun `non-image elements do not seed the shared-photo cascade`() {
        val inbox = ShareInbox()
        val ready = viewModel(inbox).bootState.value as EditorBootState.Ready
        ready.store.dispatch(
            EditorIntent.PlaceText(
                transform = Transform(xPt = 20.0, yPt = 20.0, widthPt = 120.0, heightPt = 40.0),
                text = "Already here",
            ),
        )

        inbox.offer(listOf(registerPng()))

        assertEquals(
            defaultImagePlacement(masterWidthPx = 48, masterHeightPx = 32, pageSizePt = ready.pageSizePt),
            ready.imageTransforms().single(),
        )
    }

    /** Given a Uri nothing can read, when it is shared, then the zine is unchanged and nothing crashes. */
    @Test
    fun `an unreadable uri fails the import instead of crashing`() {
        val inbox = ShareInbox()
        inbox.offer(listOf(Uri.parse("content://zinely.test/revoked")))

        val ready = viewModel(inbox).bootState.value as EditorBootState.Ready

        assertEquals(0, ready.imagesOnCurrentPage())
    }

    /**
     * Given five photos in one share, when they land, then they are five *visible* photos — five distinct
     * origins, each still wholly on the page (D-081 ruling #2, [cascadedPlacement]).
     *
     * The count assertion above already passes with every photo at the same centred default, because five
     * elements exist either way. This is the assertion that can tell "five photos" from "one photo with four
     * hidden underneath" — which is the reading a maker would give the stacked version, and they would call
     * it lost work.
     */
    @Test
    fun `a five-photo share cascades instead of landing as one stack`() {
        val inbox = ShareInbox()
        inbox.offer(List(5) { registerPng() })

        val ready = viewModel(inbox).bootState.value as EditorBootState.Ready
        val placed = ready.imageTransforms()
        val page = ready.pageSizePt

        assertEquals(5, placed.size)
        assertEquals("five photos must land on five distinct origins", 5, placed.map { it.xPt to it.yPt }.toSet().size)
        assertWhollyOnPage(placed, page)
    }

    private fun assertOneCascadeStep(first: Transform, second: Transform) {
        assertEquals(first.xPt + 12.0, second.xPt, EPSILON_PT)
        assertEquals(first.yPt + 12.0, second.yPt, EPSILON_PT)
    }

    private fun assertWhollyOnPage(placed: List<Transform>, page: PtSize) {
        placed.forEach { t ->
            assertTrue(
                "photo at (${t.xPt}, ${t.yPt}) size ${t.widthPt}x${t.heightPt} escapes the ${page.width}x${page.height} page",
                t.xPt >= 0.0 && t.yPt >= 0.0 &&
                    t.xPt + t.widthPt <= page.width + EPSILON_PT &&
                    t.yPt + t.heightPt <= page.height + EPSILON_PT,
            )
        }
    }

    /**
     * Given an import where some photos fail, when it finishes, then the summary reaches the **visible**
     * channel — not only the a11y live region (D-081 ruling #3, WCAG 3.3.1).
     *
     * The host turns this into a `Toast`. Asserted on the flow rather than on a toast because the emission is
     * what the ViewModel owns; a maker who cannot see the failure sentence has no idea two photos are missing,
     * and before this the sentence existed for screen-reader users only.
     */
    @Test
    fun `an import reports its summary as text, not only to the a11y live region`() {
        val inbox = ShareInbox()
        val viewModel = viewModel(inbox)
        val seen = mutableListOf<String>()
        // Subscribed BEFORE the share arrives: the flow is replay-free, exactly like the `saved` signal.
        CoroutineScope(UnconfinedTestDispatcher() + SupervisorJob())
            .launch { viewModel.importSummaries.collect { seen += it } }

        inbox.offer(listOf(registerPng(), Uri.parse("content://zinely.test/revoked")))

        assertEquals(listOf(Copy.ShareIn.importSummary(added = 1, failed = 1)), seen)
    }

    /** A live collector is what tells the receiving Activity a zine is open. */
    @Test
    fun `an open editor makes the inbox report an open zine`() {
        val inbox = ShareInbox()
        viewModel(inbox)

        assertEquals(true, inbox.hasOpenZine)
    }

    // =================================================================================================
    // Harness
    // =================================================================================================

    private fun EditorBootState.Ready.imagesOnCurrentPage(): Int = imageTransforms().size

    private fun EditorBootState.Ready.imageTransforms(): List<Transform> {
        val state = store.uiState.value
        return state.document.pages[state.currentPageIndex].elements
            .filterIsInstance<ImageElement>()
            .map { it.transform }
    }

    private fun viewModel(
        inbox: ShareInbox,
        assets: AssetStore = InMemoryAssetStore(),
    ): EditorViewModel {
        val dispatcher = UnconfinedTestDispatcher()
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val repository = InMemoryRepository()
        val sink = InMemorySaveFailureSink()
        return EditorViewModel(
            savedStateHandle = SavedStateHandle(mapOf("projectId" to "share-in-test")),
            repository = repository,
            binderFactory = EditorAutosaveBinderFactory(
                AutosaveCoordinatorFactory(scope, dispatcher, repository, sink),
                scope,
            ),
            saveFailureSink = sink,
            imposer = com.aritr.zinely.core.imposition.SingleSheet8Imposer(),
            assetStore = assets,
            imageDecoder = ImportMasterDecoder(context.contentResolver),
            onboardingStore = SeenOnboardingStore(),
            shareInbox = inbox,
            assetsDir = File(context.cacheDir, "assets").apply { mkdirs() },
            mainDispatcher = dispatcher,
            ioDispatcher = dispatcher,
        )
    }

    /**
     * A real PNG served as a fresh stream per open — the decoder opens the Uri three times (bounds,
     * sampled decode, EXIF), exactly as a real provider is asked to.
     */
    private fun registerPng(): Uri {
        val bitmap = Bitmap.createBitmap(48, 32, Bitmap.Config.ARGB_8888)
        val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            .toByteArray()
        bitmap.recycle()
        val uri = Uri.parse("content://zinely.test/${pngCount++}")
        shadowOf(context.contentResolver).registerInputStreamSupplier(uri) {
            ByteArrayInputStream(bytes)
        }
        return uri
    }

    private var pngCount = 0

    private class InMemoryRepository : DocumentRepository {
        private var stored: ZineDocument = blankDocument()
        override suspend fun load(projectId: String) = DataResult.Success(stored)
        override suspend fun save(projectId: String, document: ZineDocument): DataResult<Unit> {
            stored = document
            return DataResult.Success(Unit)
        }
    }

    /** Content-addressed enough for this suite: the real sha256, no disk. */
    private class InMemoryAssetStore(private val onFirstStore: (() -> Unit)? = null) : AssetStore {
        private val bytesByHex = mutableMapOf<String, ByteArray>()
        private var stores = 0
        override suspend fun contains(hash: ContentHash) = bytesByHex.containsKey(hash.hex)
        override suspend fun store(masterBytes: ByteArray): DataResult<ContentHash> {
            if (stores++ == 0) onFirstStore?.invoke()
            val hex = MessageDigest.getInstance("SHA-256").digest(masterBytes)
                .joinToString("") { "%02x".format(it) }
            bytesByHex[hex] = masterBytes
            return DataResult.Success(ContentHash.of(hex))
        }
        override suspend fun read(hash: ContentHash): DataResult<ByteArray> =
            bytesByHex[hash.hex]?.let { DataResult.Success(it) }
                ?: DataResult.Failure(
                    com.aritr.zinely.core.data.repository.DataError.NotFound(hash.hex),
                )
    }

    /** Page-bounds slack for double arithmetic — a hundredth of a point is not an escaped photo. */
    private companion object {
        const val EPSILON_PT = 0.01
    }

    private class SeenOnboardingStore : EditorOnboardingStore {
        override val moveResizeHintSeen = flowOf(true)
        override val reframeCoachSeen = flowOf(true)
        override suspend fun markMoveResizeHintSeen() = Unit
        override suspend fun markReframeCoachSeen() = Unit
    }
}
