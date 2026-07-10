package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.ui.components.ZSheetScrimTestTag
import com.aritr.zinely.ui.theme.LocalZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Home · the frozen Shelf (`docs/design/v1/shelf.html`, DESIGN-FROZEN 2026-07-08).
 *
 * The shelf renders the objects it is given; a tap opens one, a long-press or the `⋯` affordance asks
 * for its actions; "Start a zine" is always reachable except on an unreadable shelf; delete is
 * confirm-less with a snackbar undo window driven by queued [HomeShelfEvent]s, and those events
 * *serialise* — a second prompt waits for the first to be answered.
 *
 * Every render pins reduced motion: the settle stagger and the sheet slide would otherwise decide
 * which frame an assertion sees.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val opened = mutableListOf<String>()
    private val started = mutableListOf<PaperSize>()
    private val renamed = mutableListOf<Pair<String, String>>()
    private val duplicated = mutableListOf<String>()
    private val deleted = mutableListOf<String>()
    private val undone = mutableListOf<String>()
    private val committed = mutableListOf<String>()
    private val retried = mutableListOf<Unit>()
    private val events = MutableSharedFlow<HomeShelfEvent>(extraBufferCapacity = 8)

    private val twoZines = listOf(
        HomeZineCard("zine-b", "Summer scraps", "8-page mini · Letter", "Edited just now"),
        HomeZineCard("zine-a", "Cat facts", "8-page mini · A4", "Edited 3 days ago"),
    )

    /** Enough objects to earn the tools row (`SHELF_TOOLS_THRESHOLD`). */
    private val manyZines = List(SHELF_TOOLS_THRESHOLD) { i ->
        HomeZineCard("z$i", "Zine $i", "8-page mini · A4", "Edited today")
    }

    private fun setHome(
        cards: List<HomeZineCard>,
        loading: Boolean = false,
        storeEmpty: Boolean = cards.isEmpty(),
        error: Boolean = false,
    ) {
        composeRule.setContent {
            ZinelyTheme {
                CompositionLocalProvider(LocalZinelyMotion provides ZinelyMotion(reduceMotion = true)) {
                    HomeScreen(
                        loading = loading,
                        storeEmpty = storeEmpty,
                        cards = cards,
                        events = events,
                        onOpenZine = { opened += it },
                        onStartZine = { started += it },
                        onRenameZine = { id, title -> renamed += id to title },
                        onDuplicateZine = { duplicated += it },
                        onDeleteZine = { deleted += it },
                        onDeleteUndo = { undone += it },
                        onDeleteCommit = { committed += it },
                        error = error,
                        onRetry = { retried += Unit },
                    )
                }
            }
        }
    }

    /** The `⋯` affordance and the long-press both open the same sheet; the tests use the button. */
    private fun openActions(id: String) {
        composeRule.onNodeWithTag(homeCardMenuTestTag(id)).performClick()
        composeRule.waitForIdle()
    }

    // --- the objects on the shelf ---

    /** The cover is one button; its printed face is not three things to announce. */
    @Test
    fun `every zine stands on the shelf as one announced object`() {
        setHome(twoZines)

        composeRule.onNodeWithTag(HomeShelfTestTag).assertExists()
        composeRule.onNodeWithContentDescription("Summer scraps, finished zine. Open on the bench.")
            .assertExists()
        composeRule.onNodeWithContentDescription("Cat facts, finished zine. Open on the bench.")
            .assertExists()
    }

    @Test
    fun `tapping a zine opens exactly that project`() {
        setHome(twoZines)
        opened.clear()

        composeRule.onNodeWithTag(homeCardTestTag("zine-a")).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("zine-a"), opened)
    }

    @Test
    fun `a long-press asks for a zine's actions without opening it`() {
        setHome(twoZines)
        opened.clear()

        composeRule.onNodeWithTag(homeCardTestTag("zine-a")).performTouchInput { longClick() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Open on the bench").assertExists()
        assertEquals(emptyList<String>(), opened)
    }

    @Test
    fun `each object has exactly two tap targets - the cover and its actions`() {
        setHome(listOf(twoZines[0]))

        composeRule.onNodeWithTag(homeCardTestTag("zine-b")).assert(hasClickAction())
        composeRule.onNodeWithTag(homeCardMenuTestTag("zine-b")).assert(hasClickAction())
    }

    // --- the four non-content states ---

    @Test
    fun `loading is the skeleton, never a flash of the empty invitation`() {
        setHome(emptyList(), loading = true)

        composeRule.onNodeWithTag(HomeLoadingTestTag).assertExists()
        composeRule.onNodeWithTag(HomeEmptyStateTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(HomeShelfTestTag).assertDoesNotExist()
    }

    @Test
    fun `the empty shelf invites, and its dock starts a zine`() {
        setHome(emptyList())

        composeRule.onNodeWithTag(HomeEmptyStateTestTag).assertExists()
        composeRule.onNodeWithText(HomeEmptyHeadline).assertExists()

        composeRule.onNodeWithTag(ShelfStartButtonTestTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(homePaperChoiceTestTag(PaperSize.LETTER)).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(PaperSize.LETTER), started)
    }

    /** `dock.classList.toggle("hide", err)` — no starting a zine on a shelf that cannot be read. */
    @Test
    fun `an unreadable shelf hides the dock and offers only a retry`() {
        setHome(emptyList(), storeEmpty = false, error = true)

        composeRule.onNodeWithTag(ShelfErrorStateTestTag).assertExists()
        composeRule.onNodeWithTag(HomeShelfTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(HomeEmptyStateTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(ShelfStartButtonTestTag).assertDoesNotExist()

        composeRule.onNodeWithTag(ShelfRetryButtonTestTag).performClick()
        assertEquals(1, retried.size)
    }

    @Test
    fun `a shelf filtered to zero by pending deletes never shows the empty invitation`() {
        // ADR-044 §3: zero VISIBLE cards over a non-empty store is a zero-card shelf — the invitation
        // would lie while a delete is still undoable.
        setHome(emptyList(), storeEmpty = false)

        composeRule.onNodeWithTag(HomeEmptyStateTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(HomeShelfTestTag).assertExists()
        composeRule.onNodeWithTag(ShelfStartButtonTestTag).assertIsDisplayed()
    }

    // --- search + sort, which a small shelf never earns ---

    @Test
    fun `a shelf you can see all of is offered no search`() {
        setHome(twoZines)
        composeRule.onNodeWithTag(ShelfToolsTestTag).assertDoesNotExist()
    }

    @Test
    fun `a large shelf earns its tools, and a search that misses says so`() {
        setHome(manyZines)

        composeRule.onNodeWithTag(ShelfToolsTestTag).assertExists()
        composeRule.onNodeWithTag(ShelfSearchFieldTestTag).performTextInput("Zine 3")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(homeCardTestTag("z3")).assertExists()
        composeRule.onNodeWithTag(homeCardTestTag("z4")).assertDoesNotExist()
        composeRule.onNodeWithTag(ShelfSearchMissTestTag).assertDoesNotExist()

        composeRule.onNodeWithTag(ShelfSearchFieldTestTag).performTextClearance()
        composeRule.onNodeWithTag(ShelfSearchFieldTestTag).performTextInput("nothing by this name")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ShelfSearchMissTestTag).assertExists()
        composeRule.onNodeWithTag(HomeEmptyStateTestTag).assertDoesNotExist()
    }

    @Test
    fun `sorting by name reorders the shelf and relabels its button`() {
        setHome(manyZines)

        composeRule.onNodeWithTag(ShelfSortButtonTestTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(ShelfSort.Name.menuLabel).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(ShelfSort.Name.buttonLabel).assertExists()
        assertEquals(
            manyZines.sortedBy { it.title.lowercase() },
            shelfVisibleCards(manyZines, query = "", sort = ShelfSort.Name),
        )
    }

    // --- one open sheet, ever ---

    @Test
    fun `opening a second sheet closes the first`() {
        setHome(manyZines)

        composeRule.onNodeWithTag(ShelfStartButtonTestTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(HomePaperChooserTestTag).assertExists()

        // The create sheet is modal, so its scrim must be dismissed before the sort button is reachable.
        composeRule.onNodeWithTag(ZSheetScrimTestTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ShelfSortButtonTestTag).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ShelfSortSheetTestTag).assertExists()
        composeRule.onNodeWithTag(HomePaperChooserTestTag).assertDoesNotExist()
    }

    // --- the per-zine actions ---

    @Test
    fun `Duplicate hands back the card's id`() {
        setHome(twoZines)
        openActions("zine-a")

        composeRule.onNodeWithText("Duplicate").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("zine-a"), duplicated)
    }

    @Test
    fun `Delete hands back the card's id - no confirm dialog`() {
        setHome(twoZines)
        openActions("zine-a")

        composeRule.onNodeWithText("Delete").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("zine-a"), deleted)
    }

    @Test
    fun `Rename reveals an inline field that delivers the new title`() {
        setHome(twoZines)
        openActions("zine-a")

        composeRule.onNodeWithText("Rename").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(HomeRenameFieldTestTag).performTextClearance()
        composeRule.onNodeWithTag(HomeRenameFieldTestTag).performTextInput("Dog facts")
        composeRule.onNodeWithTag(HomeRenameConfirmTestTag).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("zine-a" to "Dog facts"), renamed)
    }

    @Test
    fun `a blank name is never committed`() {
        setHome(twoZines)
        openActions("zine-a")

        composeRule.onNodeWithText("Rename").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(HomeRenameFieldTestTag).performTextClearance()
        composeRule.onNodeWithTag(HomeRenameConfirmTestTag).performClick()
        composeRule.waitForIdle()

        assertEquals(emptyList<Pair<String, String>>(), renamed)
    }

    // --- the undo window ---

    @Test
    fun `a DeletePrompt shows the undo snackbar and Undo restores the zine`() {
        setHome(twoZines)

        events.tryEmit(HomeShelfEvent.DeletePrompt(id = "zine-a", title = "Cat facts"))
        composeRule.waitForIdle()

        composeRule.onNodeWithText(homeDeletedMessage("Cat facts")).assertExists()
        composeRule.onNodeWithText("Undo").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("zine-a"), undone)
        assertEquals(emptyList<String>(), committed)
    }

    @Test
    fun `letting the snackbar pass commits the delete`() {
        setHome(twoZines)

        events.tryEmit(HomeShelfEvent.DeletePrompt(id = "zine-a", title = "Cat facts"))
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(6_000) // outlive the frozen 5s window
        composeRule.waitForIdle()

        assertEquals(listOf("zine-a"), committed)
        assertEquals(emptyList<String>(), undone)
    }

    /**
     * A rotation inside the undo window disposes the composition and cancels the collector mid-await.
     * The card is already hidden, so an unresolved outcome would strand the zine: gone from the shelf,
     * still in the store, and no window left to undo it. Leaving ends the window — ADR-046 §4.
     */
    @Test
    fun `a shelf torn down inside the undo window commits rather than stranding the zine`() {
        var mounted by mutableStateOf(true)
        composeRule.setContent {
            ZinelyTheme {
                CompositionLocalProvider(LocalZinelyMotion provides ZinelyMotion(reduceMotion = true)) {
                    if (mounted) {
                        HomeScreen(
                            loading = false,
                            storeEmpty = false,
                            cards = twoZines,
                            events = events,
                            onOpenZine = {},
                            onStartZine = {},
                            onRenameZine = { _, _ -> },
                            onDuplicateZine = {},
                            onDeleteZine = {},
                            onDeleteUndo = { undone += it },
                            onDeleteCommit = { committed += it },
                        )
                    }
                }
            }
        }

        events.tryEmit(HomeShelfEvent.DeletePrompt(id = "zine-a", title = "Cat facts"))
        composeRule.waitForIdle()
        composeRule.onNodeWithText(homeDeletedMessage("Cat facts")).assertExists()

        // When the shelf goes away with the window still open
        mounted = false
        composeRule.waitForIdle()

        assertEquals(listOf("zine-a"), committed)
        assertEquals(emptyList<String>(), undone)
    }

    /**
     * Two deletes in quick succession must queue. If the second prompt overwrote the first, the first
     * zine's undo window would close without anyone answering it — a silent, irreversible commit.
     */
    @Test
    fun `a second delete waits for the first undo window to close`() {
        setHome(twoZines)

        events.tryEmit(HomeShelfEvent.DeletePrompt(id = "zine-a", title = "Cat facts"))
        events.tryEmit(HomeShelfEvent.DeletePrompt(id = "zine-b", title = "Summer scraps"))
        composeRule.waitForIdle()

        // Only the first zine's snackbar is up.
        composeRule.onNodeWithText(homeDeletedMessage("Cat facts")).assertExists()
        composeRule.onAllNodesWithText(homeDeletedMessage("Summer scraps")).assertCountEquals(0)

        composeRule.onNodeWithText("Undo").performClick()
        composeRule.waitForIdle()

        // Answering the first releases the second.
        composeRule.onNodeWithText(homeDeletedMessage("Summer scraps")).assertExists()
        assertEquals(listOf("zine-a"), undone)
        assertEquals(emptyList<String>(), committed)
    }

    @Test
    fun `a Message event shows its warm text`() {
        setHome(twoZines)

        events.tryEmit(HomeShelfEvent.Message("That didn't work — try again?"))
        composeRule.waitForIdle()

        composeRule.onNodeWithText("That didn't work — try again?").assertExists()
    }

    // --- the pure view transform ---

    @Test
    fun `Recent passes the store's newest-first order through untouched, and Oldest reverses it`() {
        assertEquals(twoZines, shelfVisibleCards(twoZines, query = "", sort = ShelfSort.Recent))
        assertEquals(twoZines.reversed(), shelfVisibleCards(twoZines, query = "", sort = ShelfSort.Oldest))
    }

    /** A shelf too small to offer search is never filtered by a query it cannot have. */
    @Test
    fun `a small shelf ignores a stale query`() {
        assertEquals(twoZines, shelfVisibleCards(twoZines, query = "nothing", sort = ShelfSort.Recent))
    }
}
