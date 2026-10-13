package com.aritr.zinely.feature.library

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.feature.editor.HomeRenameConfirmTestTag
import com.aritr.zinely.feature.editor.HomeRenameFieldTestTag
import com.aritr.zinely.feature.editor.ShelfRenameSheet
import com.aritr.zinely.ui.theme.LocalZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.flow.emptyFlow
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
 * The Library on a small phone (360x640) with the system text size turned up. Each case is a control or
 * a line of copy that was cut off there with no way to reach it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp", sdk = [28])
class LibrarySmallPhoneTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the empty shelf's paragraph is whole and reachable at the largest text`() {
        setContent(fontScale = 2f) { ZineShelfEmpty() }

        val body = composeRule.onNodeWithText(EMPTY_BODY, substring = true)
        assertFalse("the paragraph is cut short", body.textLayout().didOverflowHeight)
        body.performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `the shelf that did not open keeps Try again whole and reachable at the largest text`() {
        setContent(fontScale = 2f) { ZineShelfFail(onRetry = {}) }

        val retry = composeRule.onNodeWithTag(ZineRetryTestTag)
        assertTrue("Try again is squeezed", retry.fetchSemanticsNode().size.height >= 48)
        retry.performScrollTo().assertIsDisplayed()
    }

    /**
     * The two tests above compose the surface alone, which proves it scrolls and nothing about the dock
     * that sits over it. The dock grows with the text size (and wraps its quiet actions onto a second
     * row), so the frozen clearance under each surface stops being enough; the measured dock has to win.
     */
    @Test
    fun `on the real screen the empty shelf's last line clears the dock at the largest text`() {
        setContent(fontScale = 2f) { Screen(LibraryShelfState.Empty) }

        assertEndClearsTheDock(ZineShelfEmptyTestTag)
    }

    @Test
    fun `on the real screen Try again clears the dock at the largest text`() {
        // A pin: without quiet actions the dock is shorter than the frozen 150dp even at 2x, so this
        // passed before the measured dock was used. The empty shelf above is the one that was red.
        setContent(fontScale = 2f) { Screen(LibraryShelfState.Error) }

        assertEndClearsTheDock(ZineShelfFailTestTag)
    }

    /** Scrolls the surface to its end, then asks that nothing in it is still under the dock. */
    private fun assertEndClearsTheDock(surfaceTag: String) {
        composeRule.onNodeWithTag(surfaceTag).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 100_000f) }
        composeRule.waitForIdle()
        val dockTop = composeRule.onNodeWithTag(ZineDockTestTag).fetchSemanticsNode().boundsInRoot.top
        // Unclipped: the dock is drawn over the shelf, so a clipped rectangle would not show the overlap.
        val lowest = composeRule.onAllNodes(hasAnyAncestor(hasTestTag(surfaceTag)), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .maxOf { it.positionInRoot.y + it.size.height }
        assertTrue("the surface's last content ends at $lowest, under the dock's top $dockTop", lowest <= dockTop + 1f)
    }

    @Composable
    private fun Screen(state: LibraryShelfState) {
        ZineLibraryScreen(
            state = state,
            events = emptyFlow(),
            backupRestoreState = null,
            lastBackup = null,
            onOpenZine = {},
            onShareExport = {},
            onStartZine = { _, _ -> },
            onRenameZine = { _, _ -> },
            onDuplicateZine = {},
            onDeleteZine = {},
            onDeleteUndo = {},
            onDeleteCommit = {},
            onRetry = {},
            onStartBackup = {},
            onStartRestore = {},
            onDismissBackupRestore = {},
            onCancelBackupRestore = {},
            onRetryBackupRestore = {},
            isDeleteWaiting = { false },
            checkFolderName = ::plainFolderNameVerdict,
            modifier = Modifier.fillMaxSize(),
        )
    }

    @Test
    fun `the zine sheet keeps Delete whole and reachable under a title that stays put`() {
        setContent(fontScale = 2f) {
            Box(Modifier.align(Alignment.BottomCenter)) {
                ZineActionSheetSurface(ZineActionTarget(LONG_TITLE, "A4 · today", inFolder = true), onAction = {})
            }
        }

        val open = composeRule.onNodeWithTag(zineActionTestTag(ZineAction.Open)).fetchSemanticsNode().size.height
        val delete = composeRule.onNodeWithTag(zineActionTestTag(ZineAction.Delete))
        assertEquals("Delete is squeezed", open, delete.fetchSemanticsNode().size.height)
        delete.performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(ZineActionTitleTestTag).assertIsDisplayed()
    }

    @Test
    fun `the Move sheet keeps its title while a long list of folders scrolls`() {
        val folders = (1..10).associate { "Folder $it" to it }
        setContent(fontScale = 1f) {
            Box(Modifier.align(Alignment.BottomCenter)) {
                FolderMoveSheetSurface(FolderMoveTarget("Letters home", "Folder 1", folders), onMove = {}, onNewFolder = {})
            }
        }

        composeRule.onNodeWithTag(FolderMoveNewTestTag).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(ZineActionTitleTestTag).assertIsDisplayed()
    }

    @Test
    fun `the backups sheet keeps its title while its choices scroll at the largest text`() {
        setContent(fontScale = 2f) {
            KeepSafeSheet(
                visible = true,
                canBackup = true,
                lastBackup = null,
                onDismiss = {},
                onHidden = {},
                onSaveBackup = {},
                onRestoreBackup = {},
            )
        }

        composeRule.onNodeWithTag(KeepSafeRestoreActionTestTag).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(Copy.LibraryBackup.TITLE).assertIsDisplayed()
        // Full height, its top was under the status bar.
        assertTrue(composeRule.onNodeWithTag(KeepSafeSheetTestTag).fetchSemanticsNode().boundsInRoot.top > 0f)
    }

    @Test
    fun `beside Try another backup, Got it keeps its words on one line at medium-large text`() {
        fun failed(kind: LibraryBackupRestoreFailureKind) = LibraryBackupRestoreUiState.Failed(LibraryBackupRestoreMode.Restore, kind)
        // A failure with nothing to retry shows Got it alone, which is the height its one line makes.
        var state by mutableStateOf(failed(LibraryBackupRestoreFailureKind.BackupNoneSaved))
        setContent(fontScale = 1.4f) { LibraryBackupRestoreStateSheet(state, onDismiss = {}, onCancel = {}, onRetry = {}) }
        val alone = composeRule.onNodeWithTag(BackupRestoreDoneTestTag).fetchSemanticsNode().size.height

        state = failed(LibraryBackupRestoreFailureKind.NewerAppNeeded)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(BackupRestoreRetryTestTag).assertIsDisplayed()
        assertEquals(alone, composeRule.onNodeWithTag(BackupRestoreDoneTestTag).fetchSemanticsNode().size.height)
    }

    @Test
    fun `renaming a zine with a very long name keeps the field and Save on screen`() {
        setContent(fontScale = 2f) {
            ShelfRenameSheet(visible = true, title = "Letters home ".repeat(30).trim(), onDismiss = {}, onRename = {})
        }

        composeRule.onNodeWithTag(HomeRenameFieldTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(HomeRenameConfirmTestTag).assertIsDisplayed()
    }

    @Test
    fun `the dock's quiet actions keep their words on one line at the largest text`() {
        setContent(fontScale = 2f) {
            ZineDock(
                onStart = {},
                modifier = Modifier.align(Alignment.BottomCenter),
                secondaryActions = listOf(
                    ZineDockSecondaryAction(Copy.LibraryBackup.BRING_BACK, onClick = {}),
                    ZineDockSecondaryAction(Copy.Colophon.ACTION, onClick = {}),
                ),
            )
        }

        // Neither is long enough to need two lines of a 360dp phone, so one that is taller has been broken.
        assertEquals(
            composeRule.onNodeWithTag(zineDockSecondaryActionTestTag(Copy.LibraryBackup.BRING_BACK)).fetchSemanticsNode().size.height,
            composeRule.onNodeWithTag(zineDockSecondaryActionTestTag(Copy.Colophon.ACTION)).fetchSemanticsNode().size.height,
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Harness
    // ---------------------------------------------------------------------------------------------

    private fun setContent(fontScale: Float, content: @Composable BoxScope.() -> Unit) {
        // A Dialog reads the text size from its own context, not from `LocalDensity`, so a sheet is only
        // at this size if the activity is.
        composeRule.activity.resources.run {
            configuration.fontScale = fontScale
            @Suppress("DEPRECATION")
            updateConfiguration(configuration, displayMetrics)
        }
        composeRule.setContent {
            ZinelyTheme {
                CompositionLocalProvider(
                    LocalZinelyMotion provides ZinelyMotion(reduceMotion = true),
                    LocalDensity provides Density(LocalDensity.current.density, fontScale),
                ) {
                    Box(Modifier.fillMaxSize(), content = content)
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        val got = fetchSemanticsNode().config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        assertTrue("no text layout to read", got == true && results.isNotEmpty())
        return results.first()
    }

    private companion object {
        const val EMPTY_BODY = "One sheet of paper becomes"
        const val LONG_TITLE = "Letters home from the long summer we spent at the lake"
    }
}
