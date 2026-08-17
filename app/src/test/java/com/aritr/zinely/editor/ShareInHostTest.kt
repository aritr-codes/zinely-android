package com.aritr.zinely.editor

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.aritr.zinely.HiltTestActivity
import com.aritr.zinely.MainActivity
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.data.asset.AssetStore
import com.aritr.zinely.core.data.asset.ContentHash
import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.storage.AtomicFileStore
import com.aritr.zinely.core.imposition.Imposer
import com.aritr.zinely.core.imposition.SingleSheet8Imposer
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.data.android.DocumentRepositoryImpl
import com.aritr.zinely.export.ExportDestination
import com.aritr.zinely.export.ExportFormat
import com.aritr.zinely.export.ExportModule
import com.aritr.zinely.export.ExportOutcome
import com.aritr.zinely.export.ExportReady
import com.aritr.zinely.export.SheetExporter
import com.aritr.zinely.feature.editor.BenchBottomBarTestTag
import com.aritr.zinely.feature.editor.ProofShareTestTag
import com.aritr.zinely.render.android.AssetBytesSource
import com.aritr.zinely.ui.theme.ZinelyTheme
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowToast
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Singleton

/**
 * The **host** half of share-in and share-out — the two behaviours that live in [ZinelyNavHost] itself and
 * that no ViewModel test can see.
 *
 * Both were added by review, and both were chosen because deleting the production code left the suite
 * green:
 *
 *  1. `ShareInDrainTest` asserts the import summary reaches `EditorViewModel.importSummaries`. That is the
 *     ViewModel's half. The **visible** half — the `Toast` the host raises from that flow (D-081 ruling #3,
 *     WCAG 3.3.1) — had no assertion anywhere, so an assistive-technology-only error report could come back
 *     silently.
 *  2. `EXTRA_EXCLUDE_COMPONENTS` keeps Zinely out of its own share sheet. It must sit on the **chooser**,
 *     not on the inner send Intent (only the chooser Activity reads it), and that ordering is exactly what
 *     a refactor can invert without changing behaviour any other test observes.
 *
 * Real [ZinelyNavHost] over the real Hilt graph, the [ZinelyNavHostTest] idiom, Given-When-Then. Only the
 * [SheetExporter] is faked: the assertion is about the Intent the host builds from an [ExportReady], not
 * about rendering a PDF, and a real render here would make the test a slow renderer test wearing an Intent
 * assertion.
 */
@HiltAndroidTest
@UninstallModules(ExportModule::class, EditorAppModule::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShareInHostTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltTestActivity>()

    /** The same singleton [MainActivity] hands received URIs to — this is the app's real inbox. */
    @javax.inject.Inject
    lateinit var shareInbox: ShareInbox

    private lateinit var navController: TestNavHostController

    private fun setHost() {
        hiltRule.inject()
        composeRule.setContent {
            navController = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            ZinelyTheme {
                ZinelyNavHost(navController = navController)
            }
        }
    }

    /** Seeded on disk and adopted by the real store — the [ZinelyNavHostTest.seedZine] recipe. */
    private fun seedZine(): String {
        val root = composeRule.activity.filesDir.toPath()
        val documents = DocumentRepositoryImpl(rootDir = root, store = AtomicFileStore())
        val id = UUID.randomUUID().toString()
        runBlocking {
            val saved = documents.save(id, blankDocument())
            check(saved is DataResult.Success) { "seed save failed: $saved" }
        }
        return id
    }

    private fun openEditor(id: String) {
        composeRule.runOnUiThread { navController.navigate(EditorRoute(id)) }
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag(BenchBottomBarTestTag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** The Proof stacked above its editor — the ADR-051 shared-VM seam, and D-081 Q9's surface. */
    private fun openProof(id: String) {
        composeRule.runOnUiThread { navController.navigate(ProofRoute(id)) }
        composeRule.waitUntil(10_000) {
            navController.currentDestination?.hasRoute<ProofRoute>() == true &&
                composeRule.onAllNodesWithTag(ProofShareTestTag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Given a share where one photo lands and one cannot be decoded, when the import finishes, then the
     * summary is **shown**, not only spoken.
     *
     * The partial batch is the case that matters: a maker without TalkBack watches one of two photos appear
     * and, before this Toast existed, was told nothing at all about the other. Asserted on [ShadowToast]
     * because the Toast is the whole finding — delete the host's `importSummaries` `LaunchedEffect` and
     * `ShareInDrainTest` still passes.
     *
     * The failing Uri serves **undecodable bytes** rather than being left unregistered: Robolectric's
     * unregistered-Uri stream throws `UnsupportedOperationException` out of `BitmapFactory`, which kills the
     * import coroutine instead of exercising the pipeline's own failure path. Junk bytes are what a corrupt
     * photo actually looks like — `BitmapFactory` returns null and the pipeline counts a failure.
     */
    @Test
    fun `an import summary is shown to the eye, not only to the live region`() {
        // Given an open zine
        val id = seedZine()
        setHost()
        openEditor(id)

        // When two photos are shared into it and only one decodes
        composeRule.runOnUiThread { shareInbox.offer(listOf(registerPng(), registerJunk())) }

        // Then the summary — both halves — is on screen as text
        val expected = Copy.ShareIn.importSummary(added = 1, failed = 1)
        awaitRealWork("the import summary Toast") { ShadowToast.getTextOfLatestToast() == expected }
        assertEquals(expected, ShadowToast.getTextOfLatestToast())
    }

    /**
     * Given a share that lands **while the Proof is on top**, when the import finishes, then it is still
     * reported — D-081 Q9.
     *
     * The Proof stacks above the editor over the *same* ViewModel, so the import runs exactly as it always
     * does; only the reporting moved out from under it. Before `ImportReportSink` was called from
     * `ProofDestination` the editor's collectors were the only ones, and they are not RESUMED while the
     * Proof is up — so a maker who shared photos from this screen was told nothing, by either channel, and
     * the app looked like it had ignored them.
     *
     * Sharing into the Proof is not an exotic path: the Proof is where a maker goes to send the zine to
     * someone, which is precisely the moment another app's share sheet is the thing they were just in.
     */
    @Test
    fun `a share landing while the Proof is on top is still reported`() {
        // Given a zine whose Proof is the surface on screen
        val id = seedZine()
        setHost()
        openEditor(id)
        openProof(id)

        // When two photos are shared in and only one decodes
        composeRule.runOnUiThread { shareInbox.offer(listOf(registerPng(), registerJunk())) }

        // Then the summary is still shown — the Proof reports what the editor beneath it cannot
        val expected = Copy.ShareIn.importSummary(added = 1, failed = 1)
        awaitRealWork("the import summary Toast on the Proof") {
            ShadowToast.getTextOfLatestToast() == expected
        }
        assertEquals(expected, ShadowToast.getTextOfLatestToast())
    }

    /**
     * Given the same share, when both destinations are composed, then it is reported **once**.
     *
     * Two calls to `ImportReportSink` over one ViewModel means two collectors on one `SharedFlow`, and a
     * `SharedFlow` delivers to every subscriber — so an emission arriving while both are live is two
     * toasts and two announcements for one import. The `repeatOnLifecycle(RESUMED)` gate is what makes
     * "both composed" harmless: navigation-compose keeps the outgoing destination composed through the
     * transition, but only one entry is RESUMED.
     *
     * ⚠ **Honest limits of this assertion, measured rather than assumed.** Widening the production gate
     * from `RESUMED` to `CREATED` — i.e. letting a back-stack entry's collector run too — leaves this test
     * **green**. By the time the share arrives the transition has settled and the editor destination is no
     * longer composed at all, so there is no second collector for a wider gate to admit.
     *
     * So this pins the *outcome* (one import, one report) and would catch a duplicate raised by a second
     * sink, a replayed flow, a collector that never stops, or a `Toast` moved somewhere it fires per
     * recomposition. It does **not** prove the `RESUMED` gate: the window that gate closes is the few
     * hundred milliseconds *during* the transition, when both destinations are composed, and this harness
     * cannot schedule an emission inside that window deterministically. Pinning it would need a test that
     * offers to the inbox mid-transition, which is a device pass, not a Robolectric one.
     */
    @Test
    fun `one import is reported once, not once per composed destination`() {
        // Given both destinations composed over the same ViewModel
        val id = seedZine()
        setHost()
        openEditor(id)
        openProof(id)

        // When one share is imported
        composeRule.runOnUiThread { shareInbox.offer(listOf(registerPng())) }
        val expected = Copy.ShareIn.importSummary(added = 1, failed = 0)
        awaitRealWork("the import summary Toast on the Proof") {
            ShadowToast.getTextOfLatestToast() == expected
        }

        // Then exactly one toast — kept running afterwards, because a second collector's toast would
        // arrive *after* the first and an immediate count would pass without ever looking for it.
        settle(500)
        assertEquals("one import must not be reported twice", 1, ShadowToast.shownToastCount())
    }

    /**
     * Given a finished share export, when the chooser is launched, then Zinely is excluded from it — and
     * the exclusion is on the **chooser**, not on the payload.
     *
     * Zinely's own PNG export shares `image/png`, which matches the image-wildcard filter share-in
     * (ADR-105) registered on [MainActivity]: without this extra the chooser offers "send this zine to Zinely", a loop
     * that re-imports the export as a photo. Only `ACTION_CHOOSER`'s own Activity reads
     * `EXTRA_EXCLUDE_COMPONENTS`; on the inner send Intent it is an unread extra handed to whichever app the
     * maker picks, so the last two assertions are the ones that catch a refactor moving it inward.
     */
    @Test
    fun `the share chooser excludes Zinely itself, and excludes it on the chooser`() {
        // Given the Proof surface over its editor (the ADR-051 shared-VM seam)
        val id = seedZine()
        setHost()
        openEditor(id)
        openProof(id)

        // When Share finishes and the host launches the chooser
        composeRule.onNodeWithTag(ProofShareTestTag).performClick()
        val launched = composeRule.run {
            waitUntil(10_000) { shadowOf(activity).peekNextStartedActivity() != null }
            shadowOf(activity).getNextStartedActivity()
        }

        // Then it is a chooser…
        assertEquals(Intent.ACTION_CHOOSER, launched.action)
        // …that excludes Zinely's own component…
        val excluded = launched.getParcelableArrayExtra(Intent.EXTRA_EXCLUDE_COMPONENTS)
        assertNotNull("EXTRA_EXCLUDE_COMPONENTS missing from the chooser", excluded)
        assertEquals(
            listOf(ComponentName(composeRule.activity, MainActivity::class.java)),
            excluded!!.map { it as ComponentName },
        )
        // …and carries it itself, rather than burying it in the payload where nothing reads it.
        val inner = launched.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull("the chooser must wrap the send Intent", inner)
        assertEquals(Intent.ACTION_SEND, inner!!.action)
        assertFalse(
            "EXTRA_EXCLUDE_COMPONENTS is on the inner send Intent, where the chooser never reads it",
            inner.hasExtra(Intent.EXTRA_EXCLUDE_COMPONENTS),
        )
        assertTrue("the payload Uri must still ride the send Intent", inner.hasExtra(Intent.EXTRA_STREAM))
    }

    /**
     * Wait on work that happens on a **real** background thread, in **real** time.
     *
     * `composeRule.waitUntil` cannot do this and the difference is not academic: it spins the Compose
     * clock, so its timeout is virtual and a 20 000 ms budget elapsed in a few real milliseconds — long
     * enough for a
     * recomposition, nowhere near long enough for `withContext(Dispatchers.IO) { decode }` to return. The
     * share-in import spent that whole budget mid-decode and this suite read the silence as "no Toast".
     * So: sleep for real, and idle the main looper each turn so the continuations posted back to Main
     * (the import loop, the `LaunchedEffect` collector, the Toast) actually run.
     */
    private fun awaitRealWork(what: String, timeoutMs: Long = 30_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            composeRule.waitForIdle()
            if (condition()) return
            Thread.sleep(25)
        }
        throw AssertionError("timed out after ${timeoutMs}ms waiting for $what")
    }

    /** Keep real time and the main looper running for [millis] — for asserting something does NOT happen. */
    private fun settle(millis: Long) {
        val deadline = System.currentTimeMillis() + millis
        while (System.currentTimeMillis() < deadline) {
            composeRule.waitForIdle()
            Thread.sleep(25)
        }
    }

    /**
     * A real PNG served as a fresh stream per open — the decoder opens the Uri three times (bounds, sampled
     * decode, EXIF), exactly as a real provider is asked to. The [ShareInDrainTest] recipe.
     */
    private fun registerPng(): Uri {
        val bitmap = Bitmap.createBitmap(48, 32, Bitmap.Config.ARGB_8888)
        val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            .toByteArray()
        bitmap.recycle()
        return registerStream(bytes)
    }

    /** A Uri that opens but is not an image — the corrupt-photo half of a partial import. */
    private fun registerJunk(): Uri = registerStream("not a photo".toByteArray())

    private fun registerStream(bytes: ByteArray): Uri {
        val uri = Uri.parse("content://zinely.test/${sharedCount++}")
        // The **application** resolver, not the Activity's: every `ContextImpl` owns its own
        // `ApplicationContentResolver`, registrations live on that instance's shadow, and the import
        // pipeline resolves its resolver from the `@ApplicationContext`. Registering on the Activity's
        // leaves the Uri unregistered where it is actually opened.
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        shadowOf(resolver).registerInputStreamSupplier(uri) { ByteArrayInputStream(bytes) }
        return uri
    }

    private var sharedCount = 0

    /**
     * Returns an [ExportReady] without rendering anything. The share Intent's *contents* are the exporter's
     * and `shareIntent`'s business; this suite is only about the chooser wrapper the host puts around them.
     */
    @Module
    @InstallIn(SingletonComponent::class)
    internal object FakeExportModule {
        @Provides
        @Singleton
        fun exporter(): SheetExporter = object : SheetExporter {
            override suspend fun export(
                document: ZineDocument,
                pageSizePt: PtSize,
                imageBytes: AssetBytesSource,
                format: ExportFormat,
                destination: ExportDestination,
            ): ExportOutcome = ExportReady(Uri.parse("content://zinely.test/export.pdf"), "application/pdf")
        }
    }

    /**
     * [EditorAppModule] re-provided with **one** substitution: the content-addressed [AssetStore] is
     * in-memory instead of `FileAssetStore` over the production `AndroidFileSystemOps`.
     *
     * Measured, not assumed: with the real store the import writes its temp file and then never returns —
     * `filesDir` was left holding `assets/.tmp/importNNNN.tmp` with no blob beside it and no summary ever
     * emitted, because that path ends in a directory fsync through `android.system.Os` which this Windows
     * JVM cannot serve. `ZinelyNavHostTest.seedZine` documents the same limitation for the document store
     * and works around it the same way. The hash is real (`sha256` over the master bytes); only the sink
     * is memory, which is exactly the [ShareInDrainTest] double.
     */
    @Module
    @InstallIn(SingletonComponent::class)
    internal object FakeEditorAppModule {
        @Provides
        @MainDispatcher
        fun mainDispatcher(): CoroutineDispatcher = Dispatchers.Main.immediate

        @Provides
        @Singleton
        fun imposer(): Imposer = SingleSheet8Imposer()

        @Provides
        @Singleton
        @AssetsDir
        fun assetsDir(@ApplicationContext context: Context): File = File(context.filesDir, "assets")

        @Provides
        @Singleton
        fun decoder(@ApplicationContext context: Context): ImportMasterDecoder =
            ImportMasterDecoder(context.contentResolver)

        @Provides
        @Singleton
        fun assetStore(): AssetStore = object : AssetStore {
            private val bytesByHex = mutableMapOf<String, ByteArray>()
            override suspend fun contains(hash: ContentHash) = bytesByHex.containsKey(hash.hex)
            override suspend fun store(masterBytes: ByteArray): DataResult<ContentHash> {
                val hex = MessageDigest.getInstance("SHA-256").digest(masterBytes)
                    .joinToString("") { "%02x".format(it) }
                bytesByHex[hex] = masterBytes
                return DataResult.Success(ContentHash.of(hex))
            }
            override suspend fun read(hash: ContentHash): DataResult<ByteArray> =
                bytesByHex[hash.hex]?.let { DataResult.Success(it) }
                    ?: DataResult.Failure(DataError.NotFound(hash.hex))
        }
    }
}
