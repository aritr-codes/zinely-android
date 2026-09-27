package com.aritr.zinely.feature.library

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.height
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.theme.LocalZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
class LibraryBackupRestoreSheetTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `a loaded library explains both safe choices and reports each action`() {
        var backups = 0
        var restores = 0
        setContent {
            KeepSafeSheet(
                visible = true,
                canBackup = true,
                lastBackup = null,
                onDismiss = {},
                onHidden = {},
                onSaveBackup = { backups++ },
                onRestoreBackup = { restores++ },
            )
        }

        composeRule.onNodeWithText(Copy.LibraryBackup.DESTINATION_NOTE).assertIsDisplayed()
        composeRule.onNodeWithTag(KeepSafeSaveActionTestTag).performClick()
        composeRule.onNodeWithTag(KeepSafeRestoreActionTestTag).performClick()

        assertEquals(1, backups)
        assertEquals(1, restores)
    }

    @Test
    fun `an empty library cannot create an empty backup but can restore one`() {
        setContent {
            KeepSafeSheet(
                visible = true,
                canBackup = false,
                lastBackup = null,
                onDismiss = {},
                onHidden = {},
                onSaveBackup = { error("empty shelf exposed backup") },
                onRestoreBackup = {},
            )
        }

        composeRule.onNodeWithText(Copy.LibraryBackup.EMPTY_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(KeepSafeSaveActionTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(KeepSafeRestoreActionTestTag).assertIsDisplayed()
    }

    @Test
    fun `running restore is indeterminate and cancel reports once`() {
        var cancellations = 0
        stateSheet(
            LibraryBackupRestoreUiState.Running(LibraryBackupRestoreMode.Restore),
            onCancel = { cancellations++ },
        )

        composeRule.onNodeWithTag(BackupRestoreRunningSheetTestTag)
            .assertIsDisplayed()
            .assert(
                androidx.compose.ui.test.SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo.Indeterminate,
                ),
            )
        composeRule.onNodeWithText(Copy.LibraryBackup.RESTORE_RUNNING_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(BackupRestoreCancelTestTag).performClick()
        assertEquals(1, cancellations)
    }

    @Test
    fun `system Back is the running sheet's Cancel action`() {
        var cancellations = 0
        stateSheet(
            LibraryBackupRestoreUiState.Running(LibraryBackupRestoreMode.Restore),
            onCancel = { cancellations++ },
        )

        composeRule.runOnUiThread { ShadowDialog.getLatestDialog()?.onBackPressed() }
        composeRule.waitForIdle()

        assertEquals(1, cancellations)
    }

    @Test
    fun `backup success reports its project count and closes from Done`() {
        var dismissals = 0
        stateSheet(
            LibraryBackupRestoreUiState.BackupSaved(projectCount = 3, assetCount = 8),
            onDismiss = { dismissals++ },
        )

        composeRule.onNodeWithText(Copy.LibraryBackup.backupSavedBody(3)).assertIsDisplayed()
        composeRule.onNodeWithTag(BackupRestoreDoneTestTag).performClick()
        assertEquals(1, dismissals)
    }

    @Test
    fun `restore success says projects were added rather than replacing the shelf`() {
        stateSheet(LibraryBackupRestoreUiState.RestoreAdded(restoredProjectCount = 2))

        composeRule.onNodeWithText(Copy.LibraryBackup.restoreAddedTitle(2)).assertIsDisplayed()
        composeRule.onNodeWithText(Copy.LibraryBackup.RESTORE_SUCCESS_BODY).assertIsDisplayed()
    }

    @Test
    fun `damaged restore offers another backup and leaves dismissal available`() {
        var retries = 0
        var dismissals = 0
        stateSheet(
            LibraryBackupRestoreUiState.Failed(
                mode = LibraryBackupRestoreMode.Restore,
                kind = LibraryBackupRestoreFailureKind.Damaged,
            ),
            onDismiss = { dismissals++ },
            onRetry = { retries++ },
        )

        composeRule.onNodeWithText(Copy.LibraryBackup.ERROR_DAMAGED_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText(Copy.LibraryBackup.ERROR_DAMAGED_BODY).assertIsDisplayed()
        composeRule.onNodeWithTag(BackupRestoreRetryTestTag).performClick()
        composeRule.onNodeWithTag(BackupRestoreDoneTestTag).performClick()
        assertEquals(1, retries)
        assertEquals(1, dismissals)
    }

    @Test
    fun `a newer backup gets a specific update message`() {
        stateSheet(
            LibraryBackupRestoreUiState.Failed(
                mode = LibraryBackupRestoreMode.Restore,
                kind = LibraryBackupRestoreFailureKind.NewerAppNeeded,
            ),
        )

        composeRule.onNodeWithText(Copy.LibraryBackup.ERROR_NEWER_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText(Copy.LibraryBackup.ERROR_NEWER_BODY).assertIsDisplayed()
    }

    @Test
    fun `newer-backup actions remain reachable and horizontally unclipped at 2x text`() {
        setContent(fontScale = 2f) {
            LibraryBackupRestoreStateSheet(
                state = LibraryBackupRestoreUiState.Failed(
                    mode = LibraryBackupRestoreMode.Restore,
                    kind = LibraryBackupRestoreFailureKind.NewerAppNeeded,
                ),
                onDismiss = {},
                onCancel = {},
                onRetry = {},
            )
        }

        val sheet = composeRule.onNodeWithTag(BackupRestoreErrorSheetTestTag).getUnclippedBoundsInRoot()
        val retry = composeRule.onNodeWithTag(BackupRestoreRetryTestTag)
            .performScrollTo()
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        val done = composeRule.onNodeWithTag(BackupRestoreDoneTestTag)
            .performScrollTo()
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()

        assertTrue("retry clipped left of the sheet: $retry outside $sheet", retry.left >= sheet.left)
        assertTrue("Done clipped right of the sheet: $done outside $sheet", done.right <= sheet.right)
    }

    @Test
    fun `backup actions survive large text and remain reachable`() {
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

        composeRule.onNodeWithTag(KeepSafeRestoreActionTestTag)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `restore action is named clickable and at least 48dp tall`() {
        setContent {
            KeepSafeSheet(
                visible = true,
                canBackup = false,
                lastBackup = null,
                onDismiss = {},
                onHidden = {},
                onSaveBackup = {},
                onRestoreBackup = {},
            )
        }

        val interaction = composeRule.onNodeWithTag(KeepSafeRestoreActionTestTag)
        val bounds = interaction.getUnclippedBoundsInRoot()

        assertTrue("restore touch target was only ${bounds.height}", bounds.height.value >= 48f)
        interaction
            .assertContentDescriptionEquals(Copy.LibraryBackup.RESTORE_ACTION)
            .assertHasClickAction()
    }

    // --- 1.x step 1: the frozen amendment 1a wording (backup-restore.html) ---

    @Test
    fun `the chooser keeps the frozen 1a wording and promises no zine count`() {
        chooser(lastBackup = null)

        composeRule.onNodeWithText("Keep your zines").assertIsDisplayed()
        composeRule.onNodeWithText(
            "The backup file holds the zines and photos on this shelf. Keep a copy somewhere other than this phone.",
        ).assertIsDisplayed()
        // The option merges its text under its own label, so read the unmerged node.
        composeRule.onNodeWithText("Choose where to keep the backup file.", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Your zines, kept safe", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("Choose where to keep a copy of this whole shelf.", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `with no backup saved the chooser states it before the note and the actions`() {
        chooser(lastBackup = null)

        val line = composeRule.onNodeWithTag(KeepSafeLastBackupTestTag)
            .assertTextEquals("No backup saved yet")
            .getUnclippedBoundsInRoot()
        val note = composeRule.onNodeWithText(Copy.LibraryBackup.DESTINATION_NOTE).getUnclippedBoundsInRoot()
        val save = composeRule.onNodeWithTag(KeepSafeSaveActionTestTag).getUnclippedBoundsInRoot()
        assertTrue("the fact must read before the note", line.bottom <= note.top)
        assertTrue("the fact must read before the action", line.bottom <= save.top)
    }

    @Test
    fun `a saved backup shows its medium date and the provider's file name`() {
        chooser(LibraryLastBackup(savedAtEpochMs = SAVED_AT, fileName = "zinely-backup-2026-09-12.zine"))

        composeRule.onNodeWithTag(KeepSafeLastBackupTestTag)
            .assertTextEquals("Last backup saved ${mediumDate(SAVED_AT)} · zinely-backup-2026-09-12.zine")
    }

    @Test
    fun `a saved backup without a reported name shows the date alone`() {
        chooser(LibraryLastBackup(savedAtEpochMs = SAVED_AT, fileName = null))

        composeRule.onNodeWithTag(KeepSafeLastBackupTestTag)
            .assertTextEquals("Last backup saved ${mediumDate(SAVED_AT)}")
    }

    @Test
    fun `an empty shelf shows no last-backup line`() {
        setContent {
            KeepSafeSheet(
                visible = true,
                canBackup = false,
                lastBackup = LibraryLastBackup(savedAtEpochMs = SAVED_AT, fileName = "a.zine"),
                onDismiss = {},
                onHidden = {},
                onSaveBackup = {},
                onRestoreBackup = {},
            )
        }

        composeRule.onNodeWithTag(KeepSafeLastBackupTestTag).assertDoesNotExist()
    }

    @Test
    fun `the running backup no longer promises every zine`() {
        stateSheet(LibraryBackupRestoreUiState.Running(LibraryBackupRestoreMode.Backup))

        composeRule.onNodeWithText("Putting zines together in one file.").assertIsDisplayed()
        composeRule.onNodeWithText("Keeping every zine together in one file.").assertDoesNotExist()
    }

    @Test
    fun `a destination failure asks for another location and offers Try again`() {
        backupFailure(LibraryBackupRestoreFailureKind.SaveFailed)

        composeRule.onNodeWithText("Couldn’t save the backup there").assertIsDisplayed()
        composeRule.onNodeWithText("Pick another location and try again.").assertIsDisplayed()
        composeRule.onNodeWithTag(BackupRestoreRetryTestTag).assertTextEquals("Try again")
        composeRule.onNodeWithTag(BackupRestoreDoneTestTag).assertTextEquals("Got it")
    }

    @Test
    fun `a private-archive failure is not finished and never blames the location`() {
        backupFailure(LibraryBackupRestoreFailureKind.Generic)

        composeRule.onNodeWithText("Couldn’t finish that backup").assertIsDisplayed()
        composeRule.onNodeWithText("Nothing about the zines on this shelf was changed.").assertIsDisplayed()
        composeRule.onNodeWithText("Couldn’t save the backup there").assertDoesNotExist()
        composeRule.onNodeWithTag(BackupRestoreRetryTestTag).assertTextEquals("Try again")
    }

    @Test
    fun `out of space offers Try again and Got it`() {
        backupFailure(LibraryBackupRestoreFailureKind.NotEnoughSpace)

        composeRule.onNodeWithText("Not enough space").assertIsDisplayed()
        composeRule.onNodeWithText("Free up some space, then try again.").assertIsDisplayed()
        composeRule.onNodeWithTag(BackupRestoreRetryTestTag).assertTextEquals("Try again")
        composeRule.onNodeWithTag(BackupRestoreDoneTestTag).assertTextEquals("Got it")
    }

    @Test
    fun `failures no retry can fix offer only Got it, in backup words`() {
        listOf(
            LibraryBackupRestoreFailureKind.BackupLimitReached to
                ("Couldn’t finish that backup" to "Nothing about the zines on this shelf was changed."),
            LibraryBackupRestoreFailureKind.BackupZineUnreadable to
                ("A zine here can’t be opened" to "No backup was saved. It may not appear on your shelf."),
            LibraryBackupRestoreFailureKind.BackupZineNewer to
                ("A zine here needs a newer Zinely" to "Update Zinely, then back up."),
        ).let { cases ->
            val state = mutableStateOf<LibraryBackupRestoreUiState?>(null)
            var dismissals = 0
            setContent {
                LibraryBackupRestoreStateSheet(state.value, onDismiss = { dismissals++ }, onCancel = {}, onRetry = {})
            }
            cases.forEach { (kind, copy) ->
                state.value = null
                composeRule.waitForIdle()
                state.value = LibraryBackupRestoreUiState.Failed(LibraryBackupRestoreMode.Backup, kind)
                composeRule.waitForIdle()

                composeRule.onNodeWithText(copy.first).assertIsDisplayed()
                composeRule.onNodeWithText(copy.second).assertIsDisplayed()
                composeRule.onNodeWithText(Copy.LibraryBackup.ERROR_DAMAGED_TITLE).assertDoesNotExist()
                composeRule.onNodeWithText(Copy.LibraryBackup.ERROR_NEWER_TITLE).assertDoesNotExist()
                composeRule.onNodeWithTag(BackupRestoreRetryTestTag).assertDoesNotExist()
                composeRule.onNodeWithTag(BackupRestoreDoneTestTag).assertTextEquals("Got it").performClick()
                assertEquals("$kind: Got it dismisses", 1, dismissals)
                dismissals = 0
            }
        }
    }

    private fun chooser(lastBackup: LibraryLastBackup?) = setContent {
        KeepSafeSheet(
            visible = true,
            canBackup = true,
            lastBackup = lastBackup,
            onDismiss = {},
            onHidden = {},
            onSaveBackup = {},
            onRestoreBackup = {},
        )
    }

    private fun backupFailure(kind: LibraryBackupRestoreFailureKind) =
        stateSheet(LibraryBackupRestoreUiState.Failed(LibraryBackupRestoreMode.Backup, kind))

    private fun mediumDate(epochMs: Long): String =
        java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(java.util.Date(epochMs))

    private fun stateSheet(
        state: LibraryBackupRestoreUiState,
        onDismiss: () -> Unit = {},
        onCancel: () -> Unit = {},
        onRetry: () -> Unit = {},
    ) = setContent {
        LibraryBackupRestoreStateSheet(
            state = state,
            onDismiss = onDismiss,
            onCancel = onCancel,
            onRetry = onRetry,
        )
    }

    private companion object {
        /** 2026-09-12 12:00 UTC: the same calendar day in every CI time zone. */
        const val SAVED_AT = 1_789_214_400_000L
    }

    private fun setContent(fontScale: Float = 1f, content: @Composable () -> Unit) =
        composeRule.setContent {
            ZinelyTheme {
                CompositionLocalProvider(
                    LocalZinelyMotion provides ZinelyMotion(reduceMotion = true),
                    LocalDensity provides Density(density = 1f, fontScale = fontScale),
                ) {
                    content()
                }
            }
        }
}
