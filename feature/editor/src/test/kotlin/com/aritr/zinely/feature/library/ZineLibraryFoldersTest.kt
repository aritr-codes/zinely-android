package com.aritr.zinely.feature.library

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineCoverRecipe
import com.aritr.zinely.core.model.ZineCoverStamp
import com.aritr.zinely.core.model.ZineCoverSurface
import com.aritr.zinely.feature.editor.BenchSnackActionTestTag
import com.aritr.zinely.feature.editor.BenchSnackTestTag
import com.aritr.zinely.feature.editor.BenchSnackVoiceTestTag
import com.aritr.zinely.feature.editor.FolderSnackAction
import com.aritr.zinely.feature.editor.HomeShelfEvent
import com.aritr.zinely.feature.editor.homeDeletedMessage
import com.aritr.zinely.feature.editor.homePaperChoiceTestTag
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Shelf folders on the Library screen — `v21-library.html` A28, [ADR-125](docs/DECISIONS.md#adr-125).
 *
 * What is asserted here is what the frozen page **says** and **does**: the names TalkBack hears (its *What
 * TalkBack hears* table, written out below as literals and not read back from `Copy`), which sheet a row
 * raises, what each confirmation hands to the host, and the two things the screen itself owns, the open
 * folder and the snack. What a folder action then *does* to the zines is the host's and the store's, and is
 * tested there.
 *
 * The Shelf is the frozen page's own seeded one (`seed(true)`): six zines, two folders, one loose zine.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w480dp-h960dp", sdk = [28])
class ZineLibraryFoldersTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val opened = mutableListOf<String>()
    private val started = mutableListOf<Pair<PaperSize, String?>>()
    private val moved = mutableListOf<Pair<String, String?>>()
    private val renamed = mutableListOf<Pair<String, String>>()
    private val unpacked = mutableListOf<String>()
    private val undone = mutableListOf<String>()
    private val committed = mutableListOf<String>()
    private var snackActions = 0
    private val events = Channel<HomeShelfEvent>(Channel.BUFFERED)
    private var zines by mutableStateOf(SEEDED)
    private lateinit var inputMode: InputModeManager

    // ---------------------------------------------------------------------------------------------
    // The pile
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `a folder is one tile that says what it is and how many zines it holds`() {
        shelf()
        composeRule.onNodeWithContentDescription("For the stall, folder, 3 zines").assertExists()
        composeRule.onNodeWithContentDescription("Actions for folder For the stall").assertExists()
        composeRule.onNodeWithContentDescription("Family, folder, 2 zines").assertExists()
        // The loose zine is still a zine.
        composeRule.onNodeWithContentDescription("Riso tests").assertExists()
        // A zine in a folder is not also on My Shelf (A28.2).
        composeRule.onNodeWithContentDescription("Sunday market").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Letters home").assertDoesNotExist()
    }

    @Test
    fun `the count beside My Shelf counts every zine, in a folder or not`() {
        // A28.5, and F-12's ruling: six zines beside three tiles.
        shelf()
        composeRule.onNode(isHeading()).assertTextEquals("My Shelf")
        composeRule.onNodeWithText("6 zines").assertExists()
    }

    @Test
    fun `tapping a pile opens the folder, and the back control returns to My Shelf`() {
        shelf()
        composeRule.onNodeWithContentDescription("Family, folder, 2 zines").performClick()
        composeRule.waitForIdle()

        // Inside: the heading is the folder's name, the count is the folder's, and the tiles are its zines.
        composeRule.onNode(isHeading()).assertTextEquals("Family")
        composeRule.onNodeWithText("2 zines").assertExists()
        composeRule.onNodeWithContentDescription("Letters home").assertExists()
        composeRule.onNodeWithContentDescription("Mum's garden").assertExists()
        composeRule.onNodeWithContentDescription("Riso tests").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("For the stall, folder, 3 zines").assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Back to My Shelf").performClick()
        composeRule.waitForIdle()
        composeRule.onNode(isHeading()).assertTextEquals("My Shelf")
        composeRule.onNodeWithTag(ZineShelfBackTestTag).assertDoesNotExist()
        composeRule.onNodeWithText("6 zines").assertExists()
    }

    @Test
    fun `a zine inside a folder still opens when tapped`() {
        shelf()
        composeRule.onNodeWithContentDescription("Family, folder, 2 zines").performClick()
        composeRule.onNodeWithContentDescription("Mum's garden").performClick()
        assertEquals(listOf("Mum's garden"), opened)
    }

    @Test
    fun `system Back inside a folder returns to My Shelf`() {
        // A28.9.
        shelf()
        composeRule.onNodeWithContentDescription("Family, folder, 2 zines").performClick()
        composeRule.waitForIdle()
        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
        composeRule.onNode(isHeading()).assertTextEquals("My Shelf")
    }

    @Test
    fun `an open folder whose last zine leaves returns to My Shelf`() {
        // A28.3: a folder exists only while it holds a zine.
        shelf()
        composeRule.onNodeWithContentDescription("Family, folder, 2 zines").performClick()
        composeRule.waitForIdle()
        zines = SEEDED.map { if (it.folder == "Family") it.copy(folder = null) else it }
        composeRule.waitForIdle()
        composeRule.onNode(isHeading()).assertTextEquals("My Shelf")
    }

    @Test
    fun `opening a folder puts focus on the back control, and going back puts it on the pile`() {
        // A28, *Focus*. Keyboard mode: a control takes focus by request only outside touch mode.
        shelf()
        composeRule.runOnUiThread { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
        composeRule.onNodeWithContentDescription("Family, folder, 2 zines").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ZineShelfBackTestTag).assertIsFocused()

        composeRule.onNodeWithTag(ZineShelfBackTestTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Family, folder, 2 zines").assertIsFocused()
    }

    // ---------------------------------------------------------------------------------------------
    // Move
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `with no folder to choose, Move to a folder goes straight to naming one`() {
        // A28.14: the first folder skips the chooser.
        zines = SEEDED.map { it.copy(folder = null) }
        shelf()
        composeRule.onNodeWithContentDescription("Actions for Riso tests").performClick()
        composeRule.onNodeWithTag(zineActionTestTag(ZineAction.Move)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(FolderMoveSheetTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(FolderNameSheetTestTag).assertExists()
        composeRule.onNodeWithTag(ZineActionTitleTestTag).assertTextEquals("New folder")
        composeRule.onNodeWithTag(ZineActionSubtitleTestTag).assertTextEquals("For “Riso tests”")

        // Nothing typed: the button is there and cannot be pressed.
        composeRule.onNodeWithTag(FolderNameGoTestTag).assert(hasContentDescription("Make folder")).assertIsNotEnabled()
        composeRule.onNodeWithTag(FolderNameFieldTestTag).performTextInput("Trips")
        composeRule.onNodeWithTag(FolderNameGoTestTag).assertIsEnabled().performClick()
        composeRule.waitForIdle()

        assertEquals(listOf<Pair<String, String?>>("Riso tests" to "Trips"), moved)
        composeRule.onNodeWithTag(FolderNameSheetTestTag).assertDoesNotExist()
    }

    @Test
    fun `the chooser lists where the zine can go, and says where it already is`() {
        // A28, *What TalkBack hears*: "My Shelf, Out of the folder" · "Family, 2 zines" ·
        // "Family, It's here" (not a choice) · "New folder".
        shelf()
        composeRule.onNodeWithContentDescription("Family, folder, 2 zines").performClick()
        composeRule.onNodeWithContentDescription("Actions for Letters home").performClick()
        // A28.15: for a zine that is in a folder the row is also the way out.
        composeRule.onNodeWithTag(zineActionTestTag(ZineAction.Move))
            .assert(hasContentDescription("Move somewhere else"))
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ZineActionSubtitleTestTag).assertTextEquals("In “Family” · move to…")
        composeRule.onNodeWithTag(FolderMoveToShelfTestTag)
            .assert(hasContentDescription("My Shelf, Out of the folder")).assertIsEnabled()
        composeRule.onNodeWithTag(folderMoveRowTestTag("For the stall"))
            .assert(hasContentDescription("For the stall, 3 zines")).assertIsEnabled()
        composeRule.onNodeWithTag(folderMoveRowTestTag("Family"))
            .assert(hasContentDescription("Family, It’s here")).assertIsNotEnabled()
        composeRule.onNodeWithTag(FolderMoveNewTestTag).assert(hasContentDescription("New folder")).assertIsEnabled()

        composeRule.onNodeWithTag(folderMoveRowTestTag("For the stall")).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf<Pair<String, String?>>("Letters home" to "For the stall"), moved)
        composeRule.onNodeWithTag(FolderMoveSheetTestTag).assertDoesNotExist()
    }

    @Test
    fun `My Shelf is offered only to a zine that is in a folder, and takes it out`() {
        shelf()
        composeRule.onNodeWithContentDescription("Actions for Riso tests").performClick()
        composeRule.onNodeWithTag(zineActionTestTag(ZineAction.Move))
            .assert(hasContentDescription("Move to a folder"))
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ZineActionSubtitleTestTag).assertTextEquals("On My Shelf · move to…")
        composeRule.onNodeWithTag(FolderMoveToShelfTestTag).assertDoesNotExist()
        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Family, folder, 2 zines").performClick()
        composeRule.onNodeWithContentDescription("Actions for Mum's garden").performClick()
        composeRule.onNodeWithTag(zineActionTestTag(ZineAction.Move)).performClick()
        composeRule.onNodeWithTag(FolderMoveToShelfTestTag).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf<Pair<String, String?>>("Mum's garden" to null), moved)
    }

    @Test
    fun `Cancel on a name sheet that came from the chooser returns to the chooser`() {
        // A28.14.
        shelf()
        composeRule.onNodeWithContentDescription("Actions for Riso tests").performClick()
        composeRule.onNodeWithTag(zineActionTestTag(ZineAction.Move)).performClick()
        composeRule.onNodeWithTag(FolderMoveNewTestTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(FolderNameSheetTestTag).assertExists()

        composeRule.onNodeWithContentDescription("Cancel").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(FolderNameSheetTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(FolderMoveSheetTestTag).assertExists()
        assertTrue(moved.isEmpty())
    }

    @Test
    fun `typing a name that exists joins that folder, and Enter does what the button does`() {
        // A28.10.
        shelf()
        composeRule.onNodeWithContentDescription("Actions for Riso tests").performClick()
        composeRule.onNodeWithTag(zineActionTestTag(ZineAction.Move)).performClick()
        composeRule.onNodeWithTag(FolderMoveNewTestTag).performClick()
        composeRule.waitForIdle()

        // The field is named, and the line under it is a polite live region.
        composeRule.onNodeWithTag(FolderNameFieldTestTag).assert(hasContentDescription("Folder name"))
        composeRule.onNodeWithTag(FolderNameHintTestTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))

        // My Shelf is not a name; Enter then does nothing, because the button would do nothing.
        composeRule.onNodeWithTag(FolderNameFieldTestTag).performTextInput("my shelf")
        composeRule.onNodeWithTag(FolderNameHintTestTag)
            .assert(hasContentDescription("“My Shelf” is where loose zines sit. Try another name."))
        composeRule.onNodeWithTag(FolderNameGoTestTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(FolderNameFieldTestTag).performImeAction()
        composeRule.waitForIdle()
        assertTrue(moved.isEmpty())
        composeRule.onNodeWithTag(FolderNameSheetTestTag).assertExists()

        composeRule.onNodeWithTag(FolderNameFieldTestTag).performTextReplacement("FAMILY")
        composeRule.onNodeWithTag(FolderNameHintTestTag)
            .assert(hasContentDescription("You already have “Family”. This zine will join it."))
        composeRule.onNodeWithTag(FolderNameGoTestTag).assert(hasContentDescription("Move to “Family”"))
        composeRule.onNodeWithTag(FolderNameFieldTestTag).performImeAction()
        composeRule.waitForIdle()
        // The folder's own spelling, not the typed one.
        assertEquals(listOf<Pair<String, String?>>("Riso tests" to "Family"), moved)
    }

    @Test
    fun `the field keeps forty characters and no more`() {
        zines = SEEDED.map { it.copy(folder = null) }
        shelf()
        composeRule.onNodeWithContentDescription("Actions for Riso tests").performClick()
        composeRule.onNodeWithTag(zineActionTestTag(ZineAction.Move)).performClick()
        composeRule.onNodeWithTag(FolderNameFieldTestTag).performTextInput("a".repeat(45))
        composeRule.onNodeWithTag(FolderNameGoTestTag).performClick()
        assertEquals(listOf<Pair<String, String?>>("Riso tests" to "a".repeat(40)), moved)
    }

    // ---------------------------------------------------------------------------------------------
    // The folder's own sheet
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `a folder's three dots raise its sheet, which opens, renames and takes the zines out`() {
        shelf()
        composeRule.onNodeWithContentDescription("Actions for folder Family").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ZineActionTitleTestTag).assertTextEquals("Family")
        composeRule.onNodeWithTag(ZineActionSubtitleTestTag).assertTextEquals("Folder · 2 zines")
        composeRule.onNodeWithTag(FolderOpenTestTag).assert(hasContentDescription("Open folder"))
        composeRule.onNodeWithTag(FolderRenameTestTag).assert(hasContentDescription("Rename folder"))
        composeRule.onNodeWithTag(FolderUnpackTestTag)
            .assert(hasContentDescription("Take the zines out, They go back on My Shelf. The folder goes away."))

        composeRule.onNodeWithTag(FolderOpenTestTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNode(isHeading()).assertTextEquals("Family")
        composeRule.onNodeWithTag(ZineShelfBackTestTag).performClick()

        composeRule.onNodeWithContentDescription("Actions for folder Family").performClick()
        composeRule.onNodeWithTag(FolderUnpackTestTag).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("Family"), unpacked)
        composeRule.onNodeWithTag(FolderActionsSheetTestTag).assertDoesNotExist()
    }

    @Test
    fun `Rename folder opens holding the name, refuses another folder's, and renames`() {
        shelf()
        composeRule.onNodeWithContentDescription("Actions for folder Family").performClick()
        composeRule.onNodeWithTag(FolderRenameTestTag).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ZineActionTitleTestTag).assertTextEquals("Rename folder")
        composeRule.onNodeWithTag(ZineActionSubtitleTestTag).assertTextEquals("2 zines")
        composeRule.onNodeWithTag(FolderNameFieldTestTag).assertTextEquals("Family")
        // Its own name, unchanged, is nothing to do.
        composeRule.onNodeWithTag(FolderNameGoTestTag).assert(hasContentDescription("Rename")).assertIsNotEnabled()

        composeRule.onNodeWithTag(FolderNameFieldTestTag).performTextReplacement("for the stall")
        composeRule.onNodeWithTag(FolderNameHintTestTag)
            .assert(hasContentDescription("You already have a folder called “For the stall”."))
        composeRule.onNodeWithTag(FolderNameGoTestTag).assertIsNotEnabled()

        composeRule.onNodeWithTag(FolderNameFieldTestTag).performTextReplacement("Home")
        composeRule.onNodeWithTag(FolderNameGoTestTag).assertIsEnabled().performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("Family" to "Home"), renamed)
        // Cancel here would have closed the sheet, not gone to a chooser it did not come from.
        composeRule.onNodeWithTag(FolderMoveSheetTestTag).assertDoesNotExist()
    }

    // ---------------------------------------------------------------------------------------------
    // What the screen owns: where a new zine lands, a waiting delete, and the snack
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `Make a zine inside a folder starts it in that folder, and on My Shelf in none`() {
        // A28.6.
        shelf()
        composeRule.onNodeWithTag(ZineStartTestTag).performClick()
        composeRule.onNodeWithTag(homePaperChoiceTestTag(PaperSize.A4)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Family, folder, 2 zines").performClick()
        composeRule.onNodeWithTag(ZineStartTestTag).performClick()
        composeRule.onNodeWithTag(homePaperChoiceTestTag(PaperSize.A4)).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf<Pair<PaperSize, String?>>(PaperSize.A4 to null, PaperSize.A4 to "Family"), started)
    }

    @Test
    fun `confirming a folder action closes a waiting delete's Undo`() {
        // ADR-125 rule 11: the folder action finishes a waiting delete first, so its Undo is gone by the
        // time the folder snack shows. Raising a sheet does not; confirming does.
        shelf()
        events.trySend(HomeShelfEvent.DeletePrompt("Coffee log", "Coffee log"))
        composeRule.waitForIdle()
        composeRule.onNodeWithText(homeDeletedMessage("Coffee log")).assertExists()

        composeRule.onNodeWithContentDescription("Actions for folder Family").performClick()
        composeRule.waitForIdle()
        assertTrue("raising a sheet must not end the delete's Undo", committed.isEmpty())
        composeRule.onNodeWithText(homeDeletedMessage("Coffee log")).assertExists()

        composeRule.onNodeWithTag(FolderUnpackTestTag).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("Coffee log"), committed)
        assertTrue(undone.isEmpty())
        composeRule.onNodeWithText(homeDeletedMessage("Coffee log")).assertDoesNotExist()
        assertEquals(listOf("Family"), unpacked)
    }

    @Test
    fun `the folder snack says what happened and offers Undo, which reports back and takes it down`() {
        // A28.12.
        shelf()
        composeRule.onNodeWithTag(BenchSnackTestTag).assertDoesNotExist()
        events.trySend(HomeShelfEvent.FolderSnack("Moved to “Family”", FolderSnackAction.Undo))
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(BenchSnackTestTag).assertExists()
        // The line is the one polite announcement; Undo is its own control beside it, not inside it.
        composeRule.onNodeWithTag(BenchSnackVoiceTestTag, useUnmergedTree = true)
            .assert(hasContentDescription("Moved to “Family”"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        composeRule.onNodeWithTag(BenchSnackActionTestTag, useUnmergedTree = true)
            .assert(hasContentDescription("Undo"))
            .performClick()
        composeRule.waitForIdle()

        assertEquals(1, snackActions)
        composeRule.onNodeWithTag(BenchSnackTestTag).assertDoesNotExist()
    }

    @Test
    fun `a rename's snack offers nothing, and a stopped one offers Try again`() {
        shelf()
        events.trySend(HomeShelfEvent.FolderSnack("Renamed to “Home”"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchSnackTestTag).assertExists()
        composeRule.onNodeWithTag(BenchSnackActionTestTag, useUnmergedTree = true).assertDoesNotExist()

        // A newer snack replaces the standing one; it does not wait behind it.
        events.trySend(HomeShelfEvent.FolderSnack("Some zines are still in “Family”.", FolderSnackAction.TryAgain))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchSnackVoiceTestTag, useUnmergedTree = true)
            .assert(hasContentDescription("Some zines are still in “Family”."))
        composeRule.onNodeWithTag(BenchSnackActionTestTag, useUnmergedTree = true)
            .assert(hasContentDescription("Try again"))
    }

    @Test
    fun `raising a sheet takes the snack down, and its Undo with it`() {
        // A28.13.
        shelf()
        events.trySend(HomeShelfEvent.FolderSnack("Back on My Shelf", FolderSnackAction.Undo))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchSnackTestTag).assertExists()

        // A tile in the top row: the snack lies over the bottom of the Shelf, and takes the taps there.
        composeRule.onNodeWithContentDescription("Actions for folder For the stall").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(FolderActionsSheetTestTag).assertExists()
        composeRule.onNodeWithTag(BenchSnackTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(BenchSnackActionTestTag, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `the snack stays four seconds, then goes on its own`() {
        // A28.13.
        shelf()
        composeRule.mainClock.autoAdvance = false
        events.trySend(HomeShelfEvent.FolderSnack("Back on My Shelf", FolderSnackAction.Undo))
        composeRule.mainClock.advanceTimeBy(3_500)
        composeRule.onNodeWithTag(BenchSnackTestTag).assertExists()
        composeRule.mainClock.advanceTimeBy(1_500)
        composeRule.onNodeWithTag(BenchSnackTestTag).assertDoesNotExist()
    }

    @Test
    fun `after a move focus goes to the pile that now holds the zine, once the Shelf shows it there`() {
        // A28, *Focus*. The snack can arrive before the Shelf has listed again; focus must not land on
        // whatever tile is standing in the zine's old place.
        shelf()
        composeRule.runOnUiThread { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
        events.trySend(HomeShelfEvent.FolderSnack("Moved to “Family”", FolderSnackAction.Undo, zineId = "Riso tests", folder = "Family"))
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Riso tests").assert(SemanticsMatcher.expectValue(SemanticsProperties.Focused, false))

        zines = SEEDED.map { if (it.id == "Riso tests") it.copy(folder = "Family") else it }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Family, folder, 3 zines").assertIsFocused()
    }

    // ---------------------------------------------------------------------------------------------
    // Harness
    // ---------------------------------------------------------------------------------------------

    private fun shelf() {
        composeRule.setContent {
            ZinelyTheme {
                inputMode = LocalInputModeManager.current
                ZineLibraryScreen(
                    state = LibraryShelfState.Content(zines),
                    events = events.receiveAsFlow(),
                    backupRestoreState = null,
                    lastBackup = null,
                    onOpenZine = { opened += it },
                    onShareExport = {},
                    onStartZine = { paper, folder -> started += paper to folder },
                    onRenameZine = { _, _ -> },
                    onDuplicateZine = {},
                    onDeleteZine = {},
                    onDeleteUndo = { undone += it },
                    onDeleteCommit = { committed += it },
                    onRetry = {},
                    onStartBackup = {},
                    onStartRestore = {},
                    onDismissBackupRestore = {},
                    onCancelBackupRestore = {},
                    onRetryBackupRestore = {},
                    onMoveZine = { id, folder -> moved += id to folder },
                    onRenameFolder = { from, to -> renamed += from to to },
                    onUnpackFolder = { unpacked += it },
                    onFolderSnackAction = { snackActions++ },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.waitForIdle()
    }

    private companion object {
        private fun zine(title: String, folder: String?) = LibraryZine(
            id = title,
            title = title,
            subtitle = "A4 · today",
            cover = ZineCoverRecipe(ZineCoverSurface.MatchaInk, ZineCoverStamp.Sun),
            folder = folder,
        )

        /** `seed(true)` in the frozen page: newest first. */
        val SEEDED = listOf(
            zine("Sunday market", "For the stall"),
            zine("Letters home", "Family"),
            zine("Riso tests", null),
            zine("Mum's garden", "Family"),
            zine("Tiny poems", "For the stall"),
            zine("Coffee log", "For the stall"),
        )
    }
}
