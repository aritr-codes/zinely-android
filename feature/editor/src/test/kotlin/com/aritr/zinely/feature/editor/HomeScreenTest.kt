package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextReplacement
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Home · "My zines" with the S6.3 shelf actions (ADR-043 read shelf + ADR-044 actions;
 * docs/design/SCREEN-INVENTORY.md#home--my-zines): renders the cards it is given, taps open a
 * zine, "Start a zine" is always reachable (empty-state CTA + content FAB — the ADR-043 §5
 * deviation is over), each card carries an overflow menu (Rename/Duplicate/Delete), rename is a
 * gentle dialog, and delete is confirm-less with a snackbar undo window driven by queued
 * [HomeShelfEvent]s. Robolectric NATIVE to match the sibling screen tests.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val opened = mutableListOf<String>()
    private var startCount = 0
    private val renamed = mutableListOf<Pair<String, String>>()
    private val duplicated = mutableListOf<String>()
    private val deleted = mutableListOf<String>()
    private val undone = mutableListOf<String>()
    private val committed = mutableListOf<String>()
    private val events = MutableSharedFlow<HomeShelfEvent>(extraBufferCapacity = 8)

    private val twoZines = listOf(
        HomeZineCard(
            id = "zine-b",
            title = "Summer scraps",
            formatLabel = "8-page mini · Letter",
            editedLabel = "Edited just now",
        ),
        HomeZineCard(
            id = "zine-a",
            title = "Cat facts",
            formatLabel = "8-page mini · A4",
            editedLabel = "Edited 3 days ago",
        ),
    )

    private fun setHome(cards: List<HomeZineCard>, loading: Boolean = false) {
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    loading = loading,
                    cards = cards,
                    events = events,
                    onOpenZine = { opened += it },
                    onStartZine = { startCount++ },
                    onRenameZine = { id, title -> renamed += id to title },
                    onDuplicateZine = { duplicated += it },
                    onDeleteZine = { deleted += it },
                    onDeleteUndo = { undone += it },
                    onDeleteCommit = { committed += it },
                )
            }
        }
    }

    private fun openCardMenu(id: String) {
        composeRule.onNodeWithTag(homeCardMenuTestTag(id)).performClick()
        composeRule.waitForIdle()
    }

    // --- the read shelf (unchanged S6.2 contract) ---

    @Test
    fun `shelf shows every zine with its title, format, and recency`() {
        setHome(twoZines)

        composeRule.onNodeWithTag(HomeShelfTestTag).assertExists()
        composeRule.onNodeWithText("Summer scraps").assertExists()
        composeRule.onNodeWithText("8-page mini · Letter").assertExists()
        composeRule.onNodeWithText("Edited just now").assertExists()
        composeRule.onNodeWithText("Cat facts").assertExists()
        composeRule.onNodeWithText("8-page mini · A4").assertExists()
        composeRule.onNodeWithText("Edited 3 days ago").assertExists()
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
    fun `loading is a quiet spinner, never a flash of the empty invitation`() {
        setHome(emptyList(), loading = true)

        composeRule.onNodeWithTag(HomeLoadingTestTag).assertExists()
        composeRule.onNodeWithTag(HomeEmptyStateTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(HomeShelfTestTag).assertDoesNotExist()
    }

    // --- Start a zine (ADR-044 §4: the ADR-043 §5 CTA deviation ends) ---

    @Test
    fun `the empty shelf pairs the invitation with its Start a zine CTA`() {
        setHome(emptyList())

        composeRule.onNodeWithTag(HomeEmptyStateTestTag).assertExists()
        composeRule.onNodeWithText(HomeEmptyHeadline).assertExists()
        composeRule.onNodeWithText("works offline · stays on your phone").assertExists()

        composeRule.onNodeWithText("Start a zine").performClick()
        composeRule.waitForIdle()
        assertEquals(1, startCount)
    }

    @Test
    fun `the content shelf keeps Start a zine reachable`() {
        setHome(twoZines)

        composeRule.onNodeWithText("Start a zine").performClick()
        composeRule.waitForIdle()

        assertEquals(1, startCount)
    }

    // --- per-card actions (overflow menu) ---

    @Test
    fun `each card has exactly two tap targets - open and its menu`() {
        // The structural successor of the S6.2 no-mutation assertion: mutations exist now, but
        // only behind one menu per card (plus the shelf-level Start-a-zine FAB).
        setHome(twoZines)

        composeRule.onAllNodes(hasClickAction()).assertCountEquals(twoZines.size * 2 + 1)
    }

    @Test
    fun `Duplicate hands back the card's id`() {
        setHome(twoZines)

        openCardMenu("zine-a")
        composeRule.onNodeWithText("Duplicate").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("zine-a"), duplicated)
    }

    @Test
    fun `Delete hands back the card's id - no confirm dialog`() {
        setHome(twoZines)

        openCardMenu("zine-b")
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("zine-b"), deleted)
        composeRule.onNodeWithTag(HomeRenameDialogTestTag).assertDoesNotExist()
    }

    // --- rename dialog ---

    @Test
    fun `Rename opens a dialog that delivers the new title`() {
        setHome(twoZines)

        openCardMenu("zine-a")
        composeRule.onNodeWithText("Rename").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(HomeRenameFieldTestTag).performTextReplacement("Dog facts")
        composeRule.onNodeWithTag(HomeRenameConfirmTestTag).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("zine-a" to "Dog facts"), renamed)
        composeRule.onNodeWithTag(HomeRenameDialogTestTag).assertDoesNotExist()
    }

    @Test
    fun `a blank title disables Rename - you cannot save a nameless zine`() {
        setHome(twoZines)

        openCardMenu("zine-a")
        composeRule.onNodeWithText("Rename").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(HomeRenameConfirmTestTag).assertIsEnabled() // pre-filled title
        composeRule.onNodeWithTag(HomeRenameFieldTestTag).performTextClearance()

        composeRule.onNodeWithTag(HomeRenameConfirmTestTag).assertIsNotEnabled()
    }

    @Test
    fun `Keep name closes the dialog without renaming`() {
        setHome(twoZines)

        openCardMenu("zine-a")
        composeRule.onNodeWithText("Rename").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Keep name").performClick()
        composeRule.waitForIdle()

        assertEquals(emptyList<Pair<String, String>>(), renamed)
        composeRule.onNodeWithTag(HomeRenameDialogTestTag).assertDoesNotExist()
    }

    // --- delete undo window (HomeShelfEvent-driven snackbar) ---

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
        composeRule.mainClock.advanceTimeBy(15_000) // outlive SnackbarDuration.Long
        composeRule.waitForIdle()

        assertEquals(listOf("zine-a"), committed)
        assertEquals(emptyList<String>(), undone)
    }

    @Test
    fun `a Message event shows its warm text`() {
        setHome(twoZines)

        events.tryEmit(HomeShelfEvent.Message("That didn't work — try again?"))
        composeRule.waitForIdle()

        composeRule.onNodeWithText("That didn't work — try again?").assertExists()
    }

    // --- S6.4 card thumbnails (ADR-045) ---

    @Test
    fun `a card with a thumbnail shows the page-1 image, not the placeholder`() {
        // Given a card carrying a decoded thumbnail
        setHome(listOf(twoZines[0].copy(thumbnail = ImageBitmap(4, 8)), twoZines[1]))

        // Then that card renders the image and drops its placeholder
        composeRule.onNodeWithTag(homeCardThumbnailTestTag("zine-b"), useUnmergedTree = true)
            .assertExists()
        composeRule
            .onNodeWithTag(homeCardThumbnailPlaceholderTestTag("zine-b"), useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `a card without a thumbnail shows the warm paper placeholder, never a broken slot`() {
        // Given cards with no thumbnail yet (still rendering, or the document was unreadable)
        setHome(twoZines)

        // Then every card shows the placeholder and no image node exists
        composeRule
            .onNodeWithTag(homeCardThumbnailPlaceholderTestTag("zine-b"), useUnmergedTree = true)
            .assertExists()
        composeRule
            .onNodeWithTag(homeCardThumbnailPlaceholderTestTag("zine-a"), useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(homeCardThumbnailTestTag("zine-b"), useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `the thumbnail is decorative - it adds no tap target to the card`() {
        // Given one card with and one without a thumbnail
        setHome(listOf(twoZines[0].copy(thumbnail = ImageBitmap(4, 8)), twoZines[1]))

        // Then the S6.3 structural contract is unchanged: open + menu per card, plus the FAB
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(twoZines.size * 2 + 1)
    }
}
