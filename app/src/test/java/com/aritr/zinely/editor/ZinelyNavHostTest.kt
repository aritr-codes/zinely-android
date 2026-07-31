package com.aritr.zinely.editor

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aritr.zinely.feature.editor.HomeEmptyHeadline
import com.aritr.zinely.feature.editor.ProofBackTestTag
import com.aritr.zinely.feature.editor.ProofScreenTestTag
import com.aritr.zinely.ui.theme.ZinelyTheme
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
            // `MainActivity` hosts the graph inside `ZinelyTheme`, and the Shelf now reads that
            // theme's haptics and tokens. A bare host is not the tree the app actually runs.
            ZinelyTheme {
                ZinelyNavHost(navController = navController)
            }
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

    /**
     * A zine on the shelf prints its title inside `clearAndSetSemantics{}` — one node, one
     * announcement — so the title is reachable as the object's label, never as a text node. Tests
     * address the object the way a screen reader does.
     *
     * **B5 changed what this label says**, because the route now hosts the V2 Library
     * ([ADR-086](../../../../../../../docs/DECISIONS.md#adr-086)). V1's card announced *"X, finished
     * zine. Open on the bench."*; the V2 cover announces the zine's name and nothing else — the whole
     * design is *"covers only, no metadata line"*, and the spoken name follows the drawn one.
     */
    private fun cardLabel(title: String) = title

    private fun waitForCard(title: String, timeoutMs: Long = 10_000) {
        composeRule.waitUntil(timeoutMs) {
            composeRule.onAllNodesWithContentDescription(cardLabel(title))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun tapCard(title: String) {
        composeRule.onNodeWithContentDescription(cardLabel(title)).performClick()
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
        // Nothing is seeded, so the shelf that greets a first run is the empty one — after the
        // skeleton, since the store read is asynchronous. Its head, and its count, appear only once
        // there is something to count.
        waitForText(HomeEmptyHeadline)
    }

    @Test
    fun `tapping a card pushes that project's editor and back returns to the shelf`() {
        // Given a zine on the shelf
        val id = seedZine()
        setHost()
        waitForCard(SEEDED_TITLE)

        // When the card is tapped
        tapCard(SEEDED_TITLE)

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
        waitForCard(SEEDED_TITLE)
        tapCard(SEEDED_TITLE)
        waitForEditor()
        waitForText("Add a photo") // Ready: the supply tray is up
        composeRule.runOnUiThread { navController.popBackStack() }
        waitForHome()

        // When the same card is reopened immediately
        waitForCard(SEEDED_TITLE)
        tapCard(SEEDED_TITLE)
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
    fun `the single Proof surface stacks above the editor and loss-safe back returns to it`() {
        // Given the M5 collapse (ADR-051): one ProofRoute above the editor, not the Preview/Export/
        // Completion triad. Home / Editor / Proof.
        val id = seedZine()
        setHost()
        waitForCard(SEEDED_TITLE)
        tapCard(SEEDED_TITLE)
        waitForEditor()
        waitForText("Add a photo")
        composeRule.runOnUiThread { navController.navigate(ProofRoute(id)) }

        // Then the Proof surface is up (one destination, not three)
        composeRule.waitUntil(10_000) {
            navController.currentDestination?.hasRoute<ProofRoute>() == true
        }
        composeRule.onNodeWithTag(ProofScreenTestTag).assertIsDisplayed()

        // When loss-safe back is tapped, the pop lands on the EXISTING editor entry (shared-VM seam intact)…
        composeRule.onNodeWithTag(ProofBackTestTag).performClick()
        waitForEditor()
        assertEquals(id, navController.currentBackStackEntry?.toRoute<EditorRoute>()?.projectId)

        // …with the Proof popped: one more back is the shelf
        composeRule.runOnUiThread { navController.popBackStack() }
        waitForHome()
    }

    @Test
    fun `Share and export from the shelf stacks Home then Editor then Proof`() {
        // Given the V2 Library's sheet (B5) — `Share & export` routes into the EXISTING Proof flow
        // (D-025), and there is no shelf-level export concept.
        val id = seedZine()
        setHost()
        waitForCard(SEEDED_TITLE)

        // When the row is chosen
        // Addressed by their spoken names rather than by test tags: the Library's tags are `internal`
        // to `:feature:editor`, and a host-level test should reach the surfaces the way the platform
        // does anyway — the `⋯` by its `aria-label`, the row by the words on it.
        composeRule.onNodeWithContentDescription("Actions for $SEEDED_TITLE").performClick()
        composeRule.onNodeWithContentDescription(SHARE_AND_EXPORT).performClick()

        // Then the Proof is up over an editor that actually exists on the stack.
        //
        // **This is the assertion, and it is on the stack rather than on a rendered screen.**
        // `ProofRoute` resolves the *shared* editor ViewModel via `getBackStackEntry(EditorRoute(id))`
        // — the ADR-026 single-writer seam — so a direct `navigate(ProofRoute)` finds no such entry and
        // **throws at runtime**. The mutation is a crash, not a wrong pixel, and only the stack shows it.
        composeRule.waitUntil(10_000) {
            navController.currentDestination?.hasRoute<ProofRoute>() == true
        }
        assertEquals(id, navController.currentBackStackEntry?.toRoute<ProofRoute>()?.projectId)

        // and back lands on the bench, not the shelf — "reuse the flow" means its behaviour too.
        composeRule.runOnUiThread { navController.popBackStack() }
        waitForEditor()
        assertEquals(id, navController.currentBackStackEntry?.toRoute<EditorRoute>()?.projectId)
    }
}

/** The ADR-042 §4 adoption fallback title every on-disk-seeded test project carries. */
private const val SEEDED_TITLE = "My zine"

/** `.act` — the frozen row's own words (`v2-library.html:127`), which are also its spoken name. */
private const val SHARE_AND_EXPORT = "Share & export"
