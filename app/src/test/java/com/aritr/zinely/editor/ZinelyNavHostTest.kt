package com.aritr.zinely.editor

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.aritr.zinely.feature.editor.CompletionKeepEditingTestTag
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.navigation.toRoute
import com.aritr.zinely.HiltTestActivity
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.storage.AtomicFileStore
import com.aritr.zinely.data.android.DocumentRepositoryImpl
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.UUID

/**
 * The graph's first host-level tests (ADR-046 §Testing) — the REAL [ZinelyNavHost] over the real
 * Hilt graph (Room + files in Robolectric's per-test dirs), driven by a [TestNavHostController]:
 * the S6.5 back-stack policy is what changed, so the policy itself is what's asserted. Given-When-
 * Then; Robolectric NATIVE to match the sibling screen suites.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ZinelyNavHostTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltTestActivity>()

    private lateinit var navController: TestNavHostController

    private fun setHost() {
        composeRule.setContent {
            navController = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            ZinelyNavHost(navController = navController)
        }
    }

    /**
     * Seed a project as on-disk files (pure-JVM nio write — the production `AndroidFileSystemOps`
     * dir-fsync can't open a directory handle on the Windows JVM); the real store then ADOPTS it via
     * the ADR-042 §4 reconcile on first shelf subscription, with the fallback title [SEEDED_TITLE].
     * The production write path itself is covered by the `:data-android` Robolectric suite.
     */
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

    private fun waitForText(text: String, timeoutMs: Long = 10_000) {
        composeRule.waitUntil(timeoutMs) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForHome() {
        composeRule.waitUntil(10_000) {
            navController.currentDestination?.hasRoute<HomeRoute>() == true
        }
    }

    private fun waitForEditor() {
        composeRule.waitUntil(10_000) {
            navController.currentDestination?.hasRoute<EditorRoute>() == true
        }
    }

    @Test
    fun `the app starts on the Home shelf`() {
        // When
        setHost()
        composeRule.waitForIdle()

        // Then — HomeRoute is the start destination and single root (ADR-046 §1)
        assertTrue(navController.currentDestination?.hasRoute<HomeRoute>() == true)
        composeRule.onNodeWithText("My zines").assertExists()
    }

    @Test
    fun `tapping a card pushes that project's editor and back returns to the shelf`() {
        // Given a zine on the shelf
        val id = seedZine()
        setHost()
        waitForText(SEEDED_TITLE)

        // When the card is tapped
        composeRule.onNodeWithText(SEEDED_TITLE).performClick()

        // Then the editor for exactly that project is pushed above Home
        waitForEditor()
        assertEquals(id, navController.currentBackStackEntry?.toRoute<EditorRoute>()?.projectId)

        // and back is a pop straight to the shelf (nothing between)
        composeRule.runOnUiThread { navController.popBackStack() }
        waitForHome()
    }

    @Test
    fun `a fast reopen of the just-closed project boots Ready - never the busy error`() {
        // Given an editor that was just left (its binder release is asynchronous — ADR-046 §2)
        val id = seedZine()
        setHost()
        waitForText(SEEDED_TITLE)
        composeRule.onNodeWithText(SEEDED_TITLE).performClick()
        waitForEditor()
        waitForText("Add a photo") // Ready: the supply tray is up
        composeRule.runOnUiThread { navController.popBackStack() }
        waitForHome()

        // When the same card is reopened immediately
        waitForText(SEEDED_TITLE)
        composeRule.onNodeWithText(SEEDED_TITLE).performClick()
        waitForEditor()

        // Then the editor awaits the single-writer slot and boots Ready — no spurious boot error
        waitForText("Add a photo")
        assertEquals(id, navController.currentBackStackEntry?.toRoute<EditorRoute>()?.projectId)
    }

    @Test
    fun `opening the same project twice keeps one editor entry - launchSingleTop`() {
        // Given (mirrors the exact navigate call HomeDestination's open path makes — a double-tap
        // delivers the same route twice before the first push settles)
        val id = seedZine()
        setHost()
        composeRule.waitForIdle()

        // When
        composeRule.runOnUiThread {
            navController.navigate(EditorRoute(id)) { launchSingleTop = true }
            navController.navigate(EditorRoute(id)) { launchSingleTop = true }
        }
        waitForEditor()

        // Then one back pop lands on Home — there was exactly one editor entry
        composeRule.runOnUiThread { navController.popBackStack() }
        waitForHome()
    }

    @Test
    fun `a missing project is an honest error with a way back to the shelf`() {
        // Given the seed-on-miss is retired (ADR-046 §3): a ghost id is a real error, not a re-seed
        setHost()
        composeRule.waitForIdle()

        // When
        composeRule.runOnUiThread { navController.navigate(EditorRoute("ghost")) }
        waitForText("Couldn’t open this project.")

        // Then the error is not a dead end (Codex RF2)
        composeRule.onNodeWithText("‹  Back to your shelf").performClick()
        waitForHome()
    }

    @Test
    fun `Keep editing pops to the existing editor entry`() {
        // Given the full chain above one editor: Home / Editor / Preview / Export / Completion
        val id = seedZine()
        setHost()
        waitForText(SEEDED_TITLE)
        composeRule.onNodeWithText(SEEDED_TITLE).performClick()
        waitForEditor()
        waitForText("Add a photo")
        composeRule.runOnUiThread {
            navController.navigate(PreviewRoute(id))
            navController.navigate(ExportRoute(id))
            navController.navigate(CompletionRoute(id))
        }
        waitForText("Keep editing")

        // When — scroll first: the action sits below the fold steps on the small test display
        composeRule.onNodeWithTag(CompletionKeepEditingTestTag).performScrollTo().performClick()

        // Then the pop lands on the EXISTING editor entry (shared-VM seam intact)…
        waitForEditor()
        assertEquals(id, navController.currentBackStackEntry?.toRoute<EditorRoute>()?.projectId)

        // …with Preview/Export/Completion all popped: one more back is the shelf
        composeRule.runOnUiThread { navController.popBackStack() }
        waitForHome()
    }
}

/** The ADR-042 §4 adoption fallback title every on-disk-seeded test project carries. */
private const val SEEDED_TITLE = "My zine"
