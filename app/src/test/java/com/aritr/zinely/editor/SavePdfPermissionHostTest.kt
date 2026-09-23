package com.aritr.zinely.editor

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.aritr.zinely.HiltTestActivity
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.storage.AtomicFileStore
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.data.android.DocumentRepositoryImpl
import com.aritr.zinely.export.ExportDestination
import com.aritr.zinely.export.ExportFormat
import com.aritr.zinely.export.ExportModule
import com.aritr.zinely.export.ExportOutcome
import com.aritr.zinely.export.ExportReady
import com.aritr.zinely.export.ExportSaved
import com.aritr.zinely.export.SheetExporter
import com.aritr.zinely.feature.editor.BenchBottomBarTestTag
import com.aritr.zinely.feature.editor.ProofErrorPaneTestTag
import com.aritr.zinely.feature.editor.ProofSavePdfTestTag
import com.aritr.zinely.feature.editor.ProofShareTestTag
import com.aritr.zinely.render.android.AssetBytesSource
import com.aritr.zinely.ui.theme.ZinelyTheme
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowActivity
import java.util.Collections
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Save PDF's storage permission on Android 7–9 (ADR-054 §8), driven through the **real** [ZinelyNavHost]
 * and Proof screen: the request is made by the host, and only a tap on the real button reaches it.
 *
 * Only the [SheetExporter] is faked. It records every export it is asked for, so each test asserts what
 * actually happened: whether a save ran, how many times, to which destination. That is what a maker sees.
 * The permission dialog is Robolectric's: [ShadowActivity.getLastRequestedPermission] is the request the
 * host made, and [HiltTestActivity.onRequestPermissionsResult] delivers the answer back through the
 * Activity Result registry, which is how the real system answers too.
 *
 * Why the platform tree's default holds: Robolectric denies every runtime permission until one is granted,
 * which is a fresh install on Android 7–9.
 */
@HiltAndroidTest
@UninstallModules(ExportModule::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [26])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SavePdfPermissionHostTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltTestActivity>()

    @Inject
    internal lateinit var exporter: RecordingExporter

    private lateinit var navController: TestNavHostController

    @Test
    fun `Save PDF on Android 7 to 9 asks for storage first and saves nothing yet`() {
        openProofOnFreshZine()

        composeRule.onNodeWithTag(ProofSavePdfTestTag).performClick()
        composeRule.waitForIdle()

        val request = shadowOf(composeRule.activity).lastRequestedPermission
        assertTrue(
            "Save PDF must ask for WRITE_EXTERNAL_STORAGE",
            request?.requestedPermissions?.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE) == true,
        )
        settle(300)
        assertEquals("nothing is saved before the answer", emptyList<ExportDestination>(), exporter.destinations)
    }

    @Test
    fun `granting continues that one save, exactly once`() {
        openProofOnFreshZine()

        composeRule.onNodeWithTag(ProofSavePdfTestTag).performClick()
        answerStoragePermission(granted = true)

        awaitRealWork("the granted save") { exporter.destinations.isNotEmpty() }
        settle(300)
        assertEquals(listOf(ExportDestination.DOWNLOADS), exporter.destinations)
    }

    @Test
    fun `denying shows the save error, saves nothing, and does not ask again by itself`() {
        openProofOnFreshZine()

        composeRule.onNodeWithTag(ProofSavePdfTestTag).performClick()
        val request = answerStoragePermission(granted = false)

        awaitRealWork("the save error") {
            composeRule.onAllNodesWithTag(ProofErrorPaneTestTag).fetchSemanticsNodes().isNotEmpty()
        }
        settle(300)
        assertEquals(emptyList<ExportDestination>(), exporter.destinations)
        assertSame("no second request without a tap", request, shadowOf(composeRule.activity).lastRequestedPermission)
    }

    @Test
    fun `after a grant, the next Save PDF does not ask again`() {
        openProofOnFreshZine()
        composeRule.onNodeWithTag(ProofSavePdfTestTag).performClick()
        val first = answerStoragePermission(granted = true)
        awaitRealWork("the first save") { exporter.destinations.size == 1 }

        // The finished save replaces the commit row with `.done`, so the next save is a fresh visit.
        composeRule.runOnUiThread { navController.popBackStack() }
        openProof(zineId)
        composeRule.onNodeWithTag(ProofSavePdfTestTag).performClick()

        awaitRealWork("the second save") { exporter.destinations.size == 2 }
        assertEquals(listOf(ExportDestination.DOWNLOADS, ExportDestination.DOWNLOADS), exporter.destinations)
        assertSame("a granted save must not ask again", first, shadowOf(composeRule.activity).lastRequestedPermission)
    }

    @Test
    @Config(sdk = [29])
    fun `Save PDF on Android 10 and later never asks`() {
        openProofOnFreshZine()

        composeRule.onNodeWithTag(ProofSavePdfTestTag).performClick()

        awaitRealWork("the save") { exporter.destinations.isNotEmpty() }
        assertEquals(listOf(ExportDestination.DOWNLOADS), exporter.destinations)
        assertNull(shadowOf(composeRule.activity).lastRequestedPermission)
    }

    @Test
    fun `Share never asks for storage, even on Android 7 to 9`() {
        openProofOnFreshZine()

        composeRule.onNodeWithTag(ProofShareTestTag).performClick()

        awaitRealWork("the share export") { exporter.destinations.isNotEmpty() }
        assertEquals(listOf(ExportDestination.TRANSPORT), exporter.destinations)
        assertNull(shadowOf(composeRule.activity).lastRequestedPermission)
    }

    // ---- harness (the ZinelyNavHostTest / ShareInHostTest idiom) ----

    private lateinit var zineId: String

    private fun openProofOnFreshZine() {
        hiltRule.inject()
        zineId = seedZine()
        composeRule.setContent {
            navController = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            ZinelyTheme { ZinelyNavHost(navController = navController) }
        }
        composeRule.runOnUiThread { navController.navigate(EditorRoute(zineId)) }
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag(BenchBottomBarTestTag).fetchSemanticsNodes().isNotEmpty()
        }
        openProof(zineId)
    }

    private fun openProof(id: String) {
        composeRule.runOnUiThread { navController.navigate(ProofRoute(id)) }
        composeRule.waitUntil(10_000) {
            navController.currentDestination?.hasRoute<ProofRoute>() == true &&
                composeRule.onAllNodesWithTag(ProofSavePdfTestTag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Answers the host's pending request as the system dialog would, and returns that request. */
    private fun answerStoragePermission(granted: Boolean): ShadowActivity.PermissionsRequest {
        composeRule.waitForIdle()
        val request = checkNotNull(shadowOf(composeRule.activity).lastRequestedPermission) {
            "the host never asked for the storage permission"
        }
        composeRule.runOnUiThread {
            if (granted) {
                shadowOf(composeRule.activity.application).grantPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            val result = if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
            @Suppress("DEPRECATION")
            composeRule.activity.onRequestPermissionsResult(
                request.requestCode,
                request.requestedPermissions,
                IntArray(request.requestedPermissions.size) { result },
            )
        }
        return request
    }

    /** Seeded as on-disk files; the real store adopts it (the [ZinelyNavHostTest.seedZine] recipe). */
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

    /** Real time plus an idle main looper: the export runs on a coroutine and posts back to Main. */
    private fun awaitRealWork(what: String, timeoutMs: Long = 30_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            composeRule.waitForIdle()
            if (condition()) return
            Thread.sleep(25)
        }
        throw AssertionError("timed out after ${timeoutMs}ms waiting for $what")
    }

    /** Keep real time and the main looper running — for asserting something does NOT happen. */
    private fun settle(millis: Long) {
        val deadline = System.currentTimeMillis() + millis
        while (System.currentTimeMillis() < deadline) {
            composeRule.waitForIdle()
            Thread.sleep(25)
        }
    }

    @Module
    @InstallIn(SingletonComponent::class)
    internal object FakeExportModule {
        @Provides
        @Singleton
        fun recorder(): RecordingExporter = RecordingExporter()

        @Provides
        fun exporter(recorder: RecordingExporter): SheetExporter = recorder
    }
}

/** Records each export's destination and answers without rendering: Save gets a saved copy, Share a file. */
internal class RecordingExporter : SheetExporter {
    val destinations: MutableList<ExportDestination> = Collections.synchronizedList(mutableListOf())

    override suspend fun export(
        document: ZineDocument,
        pageSizePt: PtSize,
        imageBytes: AssetBytesSource,
        format: ExportFormat,
        destination: ExportDestination,
    ): ExportOutcome {
        destinations += destination
        val uri = Uri.parse("content://zinely.test/export.pdf")
        return when (destination) {
            ExportDestination.DOWNLOADS -> ExportSaved(uri, "application/pdf", "zine.pdf", "Downloads")
            ExportDestination.TRANSPORT -> ExportReady(uri, "application/pdf")
        }
    }
}
