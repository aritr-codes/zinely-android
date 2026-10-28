package com.aritr.zinely.feature.library

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import android.view.accessibility.AccessibilityNodeInfo
import com.aritr.zinely.core.model.ZineCoverRecipe
import com.aritr.zinely.core.model.ZineCoverStamp
import com.aritr.zinely.core.model.ZineCoverSurface
import com.aritr.zinely.feature.editor.BenchSnackActionTestTag
import com.aritr.zinely.feature.editor.BenchSnackVoiceTestTag
import com.aritr.zinely.feature.editor.HomeShelfEvent
import com.aritr.zinely.ui.a11y.platformNode
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Choosing several zines on the Library screen — `v21-library.html` A47, [ADR-142](docs/DECISIONS.md#adr-142).
 *
 * The words are the frozen page's, written out as literals rather than read back from `Copy`. What a batch then
 * does to the zines is the host's (Lane A) and is tested there; this asserts what the screen hands it.
 *
 * SDK 33 so the platform tree carries a state description (API 30+).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Tall enough that all five tiles stand clear of the dock, so a tap lands on the tile it names.
@Config(qualifiers = "w480dp-h1600dp", sdk = [33])
class ZineLibrarySelectTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val opened = mutableListOf<String>()
    private val moved = mutableListOf<Pair<String, String?>>()
    private val movedMany = mutableListOf<Pair<List<String>, String?>>()
    private val deleted = mutableListOf<String>()
    private val deletedMany = mutableListOf<List<String>>()
    private val undoneMany = mutableListOf<List<String>>()
    private val committedMany = mutableListOf<List<String>>()
    private val events = Channel<HomeShelfEvent>(Channel.BUFFERED)
    private var zines by mutableStateOf(SEEDED)
    private var loading by mutableStateOf(false)
    private lateinit var inputMode: InputModeManager

    // ---------------------------------------------------------------------------------------------
    // A47.1: the ways in
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `a long press on a zine starts choosing with that zine`() {
        shelf()
        composeRule.onNodeWithContentDescription("Riso tests").performTouchInput { longClick() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ShelfChoosingBarTestTag).assertExists()
        composeRule.onNodeWithTag(ShelfChoosingCountTestTag).assert(hasContentDescription("1 selected"))
        composeRule.onNodeWithContentDescription("Riso tests").assert(selected(true))
        composeRule.onNodeWithContentDescription("Coffee log").assert(selected(false))
        // The heading gives way to the bar; the sheet did not open.
        composeRule.onNode(isHeading()).assertDoesNotExist()
        composeRule.onNodeWithTag(ZineActionSheetTestTag).assertDoesNotExist()
        assertTrue(opened.isEmpty())
    }

    @Test
    fun `a folder's long press still opens its sheet`() {
        shelf()
        composeRule.onNodeWithContentDescription("Family, folder, 2 zines").performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(FolderActionsSheetTestTag).assertExists()
        composeRule.onNodeWithTag(ShelfChoosingBarTestTag).assertDoesNotExist()
    }

    @Test
    fun `the zine sheet's Select several starts choosing with that zine`() {
        shelf()
        composeRule.onNodeWithContentDescription("Actions for Coffee log").performClick()
        composeRule.onNodeWithTag(zineActionTestTag(ZineAction.Select)).assert(hasContentDescription("Select several")).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ZineActionSheetTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(ShelfChoosingCountTestTag).assert(hasContentDescription("1 selected"))
        composeRule.onNodeWithContentDescription("Coffee log").assert(selected(true))
    }

    // ---------------------------------------------------------------------------------------------
    // A47.2 – A47.4: choosing and leaving
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `while choosing a tap chooses or lets go, opens nothing, and the dots are gone`() {
        choose("Riso tests")
        composeRule.onNodeWithContentDescription("Actions for Coffee log").assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Coffee log").performClick()
        composeRule.onNodeWithTag(ShelfChoosingCountTestTag).assert(hasContentDescription("2 selected"))
        composeRule.onNodeWithContentDescription("Riso tests").performClick()
        composeRule.onNodeWithTag(ShelfChoosingCountTestTag).assert(hasContentDescription("1 selected"))
        composeRule.onNodeWithContentDescription("Riso tests").assert(selected(false))
        assertTrue("nothing opens while choosing", opened.isEmpty())
    }

    @Test
    fun `letting go of the last zine stops choosing, never 0 selected`() {
        choose("Riso tests")
        composeRule.onNodeWithContentDescription("Riso tests").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ShelfChoosingBarTestTag).assertDoesNotExist()
        composeRule.onNode(isHeading()).assertTextEquals("My Shelf")
        composeRule.onNodeWithText("0 selected").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Actions for Riso tests").assertExists()
    }

    @Test
    fun `the cross stops choosing and changes nothing`() {
        choose("Riso tests", "Coffee log")
        composeRule.onNodeWithContentDescription("Stop selecting").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ShelfChoosingBarTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(zineDockSecondaryActionTestTag("Backups")).assertExists()
        assertTrue(deleted.isEmpty() && deletedMany.isEmpty() && moved.isEmpty() && movedMany.isEmpty())
    }

    @Test
    fun `inside a folder Back first stops choosing, then leaves the folder`() {
        shelf()
        composeRule.onNodeWithContentDescription("Family, folder, 2 zines").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Letters home").performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ShelfChoosingBarTestTag).assertExists()

        back()
        composeRule.onNodeWithTag(ShelfChoosingBarTestTag).assertDoesNotExist()
        composeRule.onNode(isHeading()).assertTextEquals("Family")

        back()
        composeRule.onNode(isHeading()).assertTextEquals("My Shelf")
    }

    @Test
    fun `a folder cannot be chosen, and a tap on it only says so`() {
        choose("Riso tests")
        composeRule.onNodeWithContentDescription("Family, folder, 2 zines").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(BenchSnackVoiceTestTag, useUnmergedTree = true)
            .assert(hasContentDescription("To choose zines in a folder, open it first"))
        composeRule.onNodeWithTag(BenchSnackActionTestTag, useUnmergedTree = true).assertDoesNotExist()
        // Still choosing, still on My Shelf.
        composeRule.onNodeWithTag(ShelfChoosingCountTestTag).assert(hasContentDescription("1 selected"))
        composeRule.onNodeWithContentDescription("Coffee log").assertExists()
    }

    @Test
    fun `a chosen zine that is gone is dropped, and none left stops choosing`() {
        choose("Riso tests", "Coffee log")
        zines = SEEDED.filter { it.id != "Riso tests" }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ShelfChoosingCountTestTag).assert(hasContentDescription("1 selected"))

        zines = SEEDED.filter { it.id != "Riso tests" && it.id != "Coffee log" }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ShelfChoosingBarTestTag).assertDoesNotExist()
    }

    @Test
    fun `the choice survives recreation`() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent { Screen() }
        composeRule.onNodeWithContentDescription("Riso tests").performTouchInput { longClick() }
        composeRule.onNodeWithContentDescription("Tiny poems").performClick()
        composeRule.waitForIdle()

        restoration.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ShelfChoosingCountTestTag).assert(hasContentDescription("2 selected"))
        composeRule.onNodeWithContentDescription("Tiny poems").assert(selected(true))
    }

    @Test
    fun `the choice is kept while the Shelf reloads`() {
        // A47.10: not judged while loading, when there are no zines to judge by.
        choose("Riso tests", "Coffee log")
        loading = true
        composeRule.waitForIdle()
        loading = false
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ShelfChoosingCountTestTag).assert(hasContentDescription("2 selected"))
    }

    @Test
    fun `the chooser for a set is still up after recreation`() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent { Screen() }
        composeRule.onNodeWithContentDescription("Riso tests").performTouchInput { longClick() }
        composeRule.onNodeWithContentDescription("Coffee log").performClick()
        composeRule.onNodeWithTag(ZineDockMoveTestTag).performClick()
        composeRule.waitForIdle()

        restoration.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(FolderMoveSheetTestTag).assert(paneTitled("Move 2 zines"))
    }

    // ---------------------------------------------------------------------------------------------
    // A47.12: focus, with a keyboard (in touch mode a tappable control takes no focus by request; see
    // ZineLibraryFoldersTest)
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `with a keyboard, Select several puts focus on the zine just chosen`() {
        shelf()
        keyboard()
        composeRule.onNodeWithContentDescription("Actions for Coffee log").performClick()
        composeRule.onNodeWithTag(zineActionTestTag(ZineAction.Select)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Coffee log").assertIsFocused()
    }

    @Test
    fun `with a keyboard, the cross puts focus on the zine touched last`() {
        choose("Riso tests", "Coffee log")
        keyboard()
        composeRule.onNodeWithContentDescription("Stop selecting").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Coffee log").assertIsFocused()
    }

    @Test
    fun `with a keyboard, after Delete focus goes to the heading`() {
        choose("Riso tests", "Coffee log")
        keyboard()
        composeRule.onNodeWithTag(ZineDockDeleteTestTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNode(isHeading()).assertIsFocused()
    }

    // ---------------------------------------------------------------------------------------------
    // A47.5: the dock
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the dock gives way to Move and Delete, named with the count`() {
        choose("Riso tests", "Coffee log", "Tiny poems")
        composeRule.onNodeWithTag(ZineStartTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(zineDockSecondaryActionTestTag("Backups")).assertDoesNotExist()
        composeRule.onNodeWithTag(ZineDockMoveTestTag).assert(hasContentDescription("Move 3 zines to a folder"))
        composeRule.onNodeWithTag(ZineDockDeleteTestTag).assert(hasContentDescription("Delete 3 zines"))
        composeRule.onNodeWithText("Move to a folder", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `inside a folder Move reads Move somewhere else`() {
        shelf()
        composeRule.onNodeWithContentDescription("Family, folder, 2 zines").performClick()
        composeRule.onNodeWithContentDescription("Letters home").performTouchInput { longClick() }
        composeRule.onNodeWithContentDescription("Mum's garden").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ZineDockMoveTestTag).assert(hasContentDescription("Move 2 zines somewhere else"))
        composeRule.onNodeWithText("Move somewhere else", useUnmergedTree = true).assertExists()
    }

    // ---------------------------------------------------------------------------------------------
    // A47.7, A47.8: acting on the set
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `Delete hands the set over at once and stops choosing`() {
        choose("Riso tests", "Tiny poems")
        composeRule.onNodeWithTag(ZineDockDeleteTestTag).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(listOf("Riso tests", "Tiny poems")), deletedMany)
        assertTrue(deleted.isEmpty())
        composeRule.onNodeWithTag(ShelfChoosingBarTestTag).assertDoesNotExist()
    }

    @Test
    fun `a set of one deletes as one zine does`() {
        choose("Riso tests")
        composeRule.onNodeWithTag(ZineDockDeleteTestTag).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("Riso tests"), deleted)
        assertTrue(deletedMany.isEmpty())
    }

    @Test
    fun `one Undo covers a batch delete`() {
        shelf()
        events.trySend(HomeShelfEvent.DeleteBatchPrompt(listOf("Riso tests", "Tiny poems"), "Deleted 2 zines"))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Deleted 2 zines").assertExists()
        composeRule.onNodeWithText("Undo").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(listOf("Riso tests", "Tiny poems")), undoneMany)
        assertTrue(committedMany.isEmpty())
    }

    @Test
    fun `Move raises the chooser for the set, titled by its count`() {
        choose("Riso tests", "Coffee log")
        composeRule.onNodeWithTag(ZineDockMoveTestTag).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(FolderMoveSheetTestTag).assert(paneTitled("Move 2 zines"))
        composeRule.onNodeWithTag(ZineActionTitleTestTag).assertTextEquals("2 zines")
        composeRule.onNodeWithTag(ZineActionSubtitleTestTag).assertTextEquals("On My Shelf · move to…")
        composeRule.onNodeWithTag(folderMoveRowTestTag("Family")).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(listOf("Riso tests", "Coffee log") to "Family"), movedMany)
        assertTrue(moved.isEmpty())
        composeRule.onNodeWithTag(ShelfChoosingBarTestTag).assertDoesNotExist()
    }

    @Test
    fun `a set of one moves as one zine does, in its words`() {
        choose("Riso tests")
        composeRule.onNodeWithTag(ZineDockMoveTestTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(FolderMoveSheetTestTag).assert(paneTitled("Move zine"))
        composeRule.onNodeWithTag(ZineActionTitleTestTag).assertTextEquals("Riso tests")
        composeRule.onNodeWithTag(folderMoveRowTestTag("Family")).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf<Pair<String, String?>>("Riso tests" to "Family"), moved)
        assertTrue(movedMany.isEmpty())
    }

    @Test
    fun `a new folder for the set names the set, and Cancel goes back to the chooser`() {
        choose("Riso tests", "Coffee log")
        composeRule.onNodeWithTag(ZineDockMoveTestTag).performClick()
        composeRule.onNodeWithTag(FolderMoveNewTestTag).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ZineActionSubtitleTestTag).assertTextEquals("For 2 zines")
        composeRule.onNodeWithTag(FolderNameFieldTestTag).performTextInput("family")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(FolderNameHintTestTag).assert(hasContentDescription("You already have “Family”. These zines will join it."))

        composeRule.onNodeWithContentDescription("Cancel").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(FolderMoveSheetTestTag).assert(paneTitled("Move 2 zines"))
    }

    @Test
    fun `naming a new folder for the set moves the set into it`() {
        choose("Riso tests", "Coffee log")
        composeRule.onNodeWithTag(ZineDockMoveTestTag).performClick()
        composeRule.onNodeWithTag(FolderMoveNewTestTag).performClick()
        composeRule.onNodeWithTag(FolderNameFieldTestTag).performTextInput("Trips")
        composeRule.onNodeWithTag(FolderNameGoTestTag).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(listOf("Riso tests", "Coffee log") to "Trips"), movedMany)
    }

    @Test
    fun `a sheet dismissed without acting keeps the choice`() {
        choose("Riso tests", "Coffee log")
        composeRule.onNodeWithTag(ZineDockMoveTestTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ZineActionScrimTestTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(FolderMoveSheetTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(ShelfChoosingCountTestTag).assert(hasContentDescription("2 selected"))
    }

    // ---------------------------------------------------------------------------------------------
    // A47.11: what the platform hears
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `a zine while choosing is one platform button with its state after its name`() {
        choose("Riso tests")
        val chosen = composeRule.onNodeWithContentDescription("Riso tests").platformNode(composeRule.activity)
        assertEquals("android.widget.Button", chosen.className)
        assertEquals("Riso tests", chosen.contentDescription)
        // Compose reports `selected` on a non-Tab role as a checkable, checked node; the state TalkBack speaks is
        // the stateDescription, which replaces "checked".
        assertTrue(chosen.isCheckable && chosen.isChecked)
        assertEquals("Selected", chosen.stateDescription)
        assertTrue(chosen.isClickable)
        assertTrue("its tap is named Unselect", "Unselect" in chosen.actionLabels)
        assertFalse("no long press while choosing", AccessibilityNodeInfo.ACTION_LONG_CLICK in chosen.actionIds)

        val other = composeRule.onNodeWithContentDescription("Coffee log").platformNode(composeRule.activity)
        assertFalse(other.isChecked)
        assertEquals("Not selected", other.stateDescription)
        assertTrue("its tap is named Select", "Select" in other.actionLabels)
    }

    @Test
    fun `at rest a zine's long press is named Select, a folder's Actions`() {
        shelf()
        assertEquals("Select", composeRule.onNodeWithContentDescription("Riso tests").platformNode(composeRule.activity).longClickLabel)
        assertEquals(
            "Actions",
            composeRule.onNodeWithContentDescription("Family, folder, 2 zines").platformNode(composeRule.activity).longClickLabel,
        )
    }

    @Test
    fun `a folder while choosing is dimmed to the platform`() {
        choose("Riso tests")
        val pile = composeRule.onNodeWithContentDescription("Family, folder, 2 zines").platformNode(composeRule.activity)
        assertEquals("android.widget.Button", pile.className)
        assertFalse("TalkBack says dimmed", pile.isEnabled)
    }

    @Test
    fun `the bar's cross, count and the dock are named for the platform`() {
        choose("Riso tests", "Coffee log")
        val stop = composeRule.onNodeWithTag(ShelfStopChoosingTestTag).platformNode(composeRule.activity)
        assertEquals("android.widget.Button", stop.className)
        assertEquals("Stop selecting", stop.contentDescription)
        val count = composeRule.onNodeWithTag(ShelfChoosingCountTestTag).platformNode(composeRule.activity)
        assertEquals(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE, count.liveRegion)
        val move = composeRule.onNodeWithTag(ZineDockMoveTestTag).platformNode(composeRule.activity)
        assertEquals("android.widget.Button", move.className)
        assertEquals("Move 2 zines to a folder", move.contentDescription)
        val delete = composeRule.onNodeWithTag(ZineDockDeleteTestTag).platformNode(composeRule.activity)
        assertEquals("android.widget.Button", delete.className)
        assertEquals("Delete 2 zines", delete.contentDescription)
    }

    // ---------------------------------------------------------------------------------------------
    // Harness
    // ---------------------------------------------------------------------------------------------

    private fun selected(on: Boolean) = SemanticsMatcher.expectValue(SemanticsProperties.Selected, on)

    private fun paneTitled(title: String) = SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, title)

    private fun keyboard() {
        composeRule.runOnUiThread { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
        composeRule.waitForIdle()
    }

    private fun back() {
        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    /** My Shelf, choosing [first] by a long press and then the rest by taps. */
    private fun choose(first: String, vararg more: String) {
        shelf()
        composeRule.onNodeWithContentDescription(first).performTouchInput { longClick() }
        composeRule.waitForIdle()
        more.forEach { composeRule.onNodeWithContentDescription(it).performClick() }
        composeRule.waitForIdle()
    }

    private fun shelf() {
        composeRule.setContent { Screen() }
        composeRule.waitForIdle()
    }

    @Composable
    private fun Screen() {
        ZinelyTheme {
            inputMode = LocalInputModeManager.current
            ZineLibraryScreen(
                state = if (loading) LibraryShelfState.Loading else LibraryShelfState.Content(zines),
                events = events.receiveAsFlow(),
                backupRestoreState = null,
                lastBackup = null,
                onOpenZine = { opened += it },
                onShareExport = {},
                onStartZine = { _, _ -> },
                onRenameZine = { _, _ -> },
                onDuplicateZine = {},
                onDeleteZine = { deleted += it },
                onDeleteUndo = {},
                onDeleteCommit = {},
                onRetry = {},
                onStartBackup = {},
                onStartRestore = {},
                onDismissBackupRestore = {},
                onCancelBackupRestore = {},
                onRetryBackupRestore = {},
                onMoveZine = { id, folder -> moved += id to folder },
                isDeleteWaiting = { true },
                checkFolderName = ::plainFolderNameVerdict,
                modifier = Modifier.fillMaxSize(),
                onDeleteZines = { deletedMany += it },
                onDeleteUndoMany = { undoneMany += it },
                onDeleteCommitMany = { committedMany += it },
                onMoveZines = { ids, folder -> movedMany += ids to folder },
            )
        }
    }

    private companion object {
        private fun zine(title: String, folder: String?) = LibraryZine(
            id = title,
            title = title,
            subtitle = "A4 · today",
            cover = ZineCoverRecipe(ZineCoverSurface.MatchaInk, ZineCoverStamp.Sun),
            folder = folder,
        )

        /** Newest first: My Shelf shows For the stall, Riso tests, Coffee log, Family, Tiny poems. */
        val SEEDED = listOf(
            zine("Sunday market", "For the stall"),
            zine("Riso tests", null),
            zine("Coffee log", null),
            zine("Letters home", "Family"),
            zine("Tiny poems", null),
            zine("Mum's garden", "Family"),
        )
    }
}
