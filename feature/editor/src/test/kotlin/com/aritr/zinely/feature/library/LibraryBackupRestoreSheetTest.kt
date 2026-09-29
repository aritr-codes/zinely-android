package com.aritr.zinely.feature.library

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
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
import org.junit.Assert.assertFalse
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
    fun `a loaded library offers both choices and reports each action`() {
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

        composeRule.runOnUiThread { checkNotNull(ShadowDialog.getLatestDialog()).onBackPressed() }
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
    fun `with no backup saved the chooser states it before the actions`() {
        chooser(lastBackup = null)

        val line = composeRule.onNodeWithTag(KeepSafeLastBackupTestTag)
            .assertTextEquals("No backup saved yet")
            .getUnclippedBoundsInRoot()
        val save = composeRule.onNodeWithTag(KeepSafeSaveActionTestTag).getUnclippedBoundsInRoot()
        assertTrue("the fact must read before the action", line.bottom <= save.top)
    }

    @Test
    fun `a saved backup shows its date, then the provider's whole file name on its own line`() {
        val name = "zinely-backup-2026-09-12-a-long-name-the-provider-chose-for-this-shelf.zine"
        chooser(LibraryLastBackup(savedAtEpochMs = SAVED_AT, fileName = name))

        // One TalkBack stop, two texts in reading order: announced with a pause between, never fused.
        composeRule.onNodeWithTag(KeepSafeLastBackupTestTag)
            .assertTextEquals("Last backup saved ${mediumDate(SAVED_AT)}", name)
        val date = composeRule.onNodeWithText("Last backup saved ${mediumDate(SAVED_AT)}", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val file = composeRule.onNodeWithText(name, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val save = composeRule.onNodeWithTag(KeepSafeSaveActionTestTag).getUnclippedBoundsInRoot()
        assertTrue("the file name sits under the date", date.bottom <= file.top)
        assertTrue("both read before the action", file.bottom <= save.top)
        composeRule.onNodeWithText("·", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `a saved backup without a reported name shows the date alone`() {
        chooser(LibraryLastBackup(savedAtEpochMs = SAVED_AT, fileName = null))

        composeRule.onNodeWithTag(KeepSafeLastBackupTestTag)
            .assertTextEquals("Last backup saved ${mediumDate(SAVED_AT)}")
    }

    @Test
    fun `the content shelf has no explanatory note`() {
        chooser(LibraryLastBackup(savedAtEpochMs = SAVED_AT, fileName = "a.zine"))

        composeRule.onNodeWithText("Backups save as a file you choose", substring = true, useUnmergedTree = true)
            .assertDoesNotExist()
        // Removed, not replaced: no text of any wording between the last-backup line and the first action.
        assertNoTextBetween(
            composeRule.onNodeWithTag(KeepSafeLastBackupTestTag).getUnclippedBoundsInRoot().bottom,
            composeRule.onNodeWithTag(KeepSafeSaveActionTestTag).getUnclippedBoundsInRoot().top,
        )
    }

    @Test
    fun `the empty shelf has no explanatory note`() {
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
        composeRule.onNodeWithText("Restoring adds zines to this shelf", substring = true, useUnmergedTree = true)
            .assertDoesNotExist()
        assertNoTextBetween(
            composeRule.onNodeWithText(Copy.LibraryBackup.EMPTY_BODY).getUnclippedBoundsInRoot().bottom,
            composeRule.onNodeWithTag(KeepSafeRestoreActionTestTag).getUnclippedBoundsInRoot().top,
        )
    }

    @Test
    fun `a long file name wraps whole at 200 percent text, never cut off`() {
        val name = "zinely-backup-2026-09-12-a-long-name-the-provider-chose-for-this-whole-shelf-of-zines.zine"
        setContent(fontScale = 2f) {
            KeepSafeSheet(
                visible = true,
                canBackup = true,
                lastBackup = LibraryLastBackup(savedAtEpochMs = SAVED_AT, fileName = name),
                onDismiss = {},
                onHidden = {},
                onSaveBackup = {},
                onRestoreBackup = {},
            )
        }

        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(name, useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
        val layout = layouts.single()
        assertTrue("the name wraps rather than running off", layout.lineCount > 1)
        assertFalse("no line of the name is clipped or ellipsised", layout.hasVisualOverflow)
        assertEquals(name, layout.layoutInput.text.text)
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
            LibraryBackupRestoreFailureKind.BackupNoneSaved to
                ("No zines could be saved" to "Zinely couldn’t read the zines on this phone, so no backup was saved."),
            LibraryBackupRestoreFailureKind.BackupNoneSavedOffShelf to (
                "No zines could be saved" to
                    "Zinely couldn’t read the zines on this phone, so no backup was saved. They may not appear on your shelf."
                ),
            LibraryBackupRestoreFailureKind.BackupNoneSavedPhoto to (
                "No zines could be saved" to
                    "Zinely couldn’t read the photos in the zines on this phone, so no backup was saved."
                ),
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

    // --- 1.x step 1b (ADR-122 R2 / R3, ADR-121 Amendment N) ---

    @Test
    fun `once a restore commits, Cancel leaves the tree and Back does nothing (R2)`() {
        var cancellations = 0
        stateSheet(
            LibraryBackupRestoreUiState.Running(LibraryBackupRestoreMode.Restore, cancellable = false),
            onCancel = { cancellations++ },
        )

        composeRule.onNodeWithText(Copy.LibraryBackup.RESTORE_RUNNING_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText("Adding zines to your shelf.").assertIsDisplayed()
        composeRule.onNodeWithText("This part can’t be stopped.").assertIsDisplayed()
        composeRule.onNodeWithText(Copy.LibraryBackup.RESTORE_RUNNING_BODY).assertDoesNotExist()
        composeRule.onNodeWithTag(BackupRestoreCancelTestTag).assertDoesNotExist()

        composeRule.runOnUiThread { checkNotNull(ShadowDialog.getLatestDialog()).onBackPressed() }
        composeRule.waitForIdle()
        assertEquals(0, cancellations)
    }

    @Test
    fun `when the commit starts mid-sheet, Cancel leaves and the new hint is announced`() {
        val state = mutableStateOf<LibraryBackupRestoreUiState?>(
            LibraryBackupRestoreUiState.Running(LibraryBackupRestoreMode.Restore),
        )
        setContent { LibraryBackupRestoreStateSheet(state.value, onDismiss = {}, onCancel = {}, onRetry = {}) }
        composeRule.onNodeWithTag(BackupRestoreCancelTestTag).assertIsDisplayed()

        state.value = LibraryBackupRestoreUiState.Running(LibraryBackupRestoreMode.Restore, cancellable = false)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(BackupRestoreCancelTestTag).assertDoesNotExist()
        composeRule.onNodeWithText("This part can’t be stopped.")
            .assertIsDisplayed()
            .assert(
                androidx.compose.ui.test.SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    androidx.compose.ui.semantics.LiveRegionMode.Polite,
                ),
            )
    }

    @Test
    fun `a running backup keeps its Cancel whatever the flag says`() {
        stateSheet(LibraryBackupRestoreUiState.Running(LibraryBackupRestoreMode.Backup, cancellable = false))

        composeRule.onNodeWithTag(BackupRestoreCancelTestTag).assertIsDisplayed()
    }

    @Test
    fun `a partial backup is a success titled N of M that names what it left out`() {
        stateSheet(
            LibraryBackupRestoreUiState.BackupSaved(
                projectCount = 4,
                assetCount = 1,
                totalCount = 6,
                omitted = listOf(
                    LibraryOmission("Letters home", LibraryOmissionReason.Unreadable),
                    LibraryOmission("Moth Club Bulletin", LibraryOmissionReason.Photo),
                ),
            ),
        )

        composeRule.onNodeWithText("4 of 6 zines saved").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Zinely couldn’t open 1 zine, so it isn’t in this backup: “Letters home”.\n\n" +
                "Zinely couldn’t read a photo in 1 zine, so that zine isn’t in this backup: “Moth Club Bulletin”.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("✓").assertIsDisplayed()
        composeRule.onNodeWithTag(BackupRestoreDoneTestTag).assertIsDisplayed()
    }

    @Test
    fun `a restore that adds some says what was already here, then stayed put (N3)`() {
        stateSheet(LibraryBackupRestoreUiState.RestoreAdded(restoredProjectCount = 2, alreadyHereCount = 5))

        composeRule.onNodeWithText("2 zines added to your shelf").assertIsDisplayed()
        composeRule.onNodeWithText(
            "The other 5 were already here, so they weren’t added again.\n\nWhat was already on this shelf stayed put.",
        ).assertIsDisplayed()
    }

    @Test
    fun `nothing new is a success with Done, not an error (N4)`() {
        stateSheet(LibraryBackupRestoreUiState.RestoreAdded(restoredProjectCount = 0, alreadyHereCount = 7))

        composeRule.onNodeWithText("Nothing new to add").assertIsDisplayed()
        composeRule.onNodeWithText("The zines from this backup are already on your shelf.").assertIsDisplayed()
        composeRule.onNodeWithTag(BackupRestoreSuccessSheetTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(BackupRestoreRetryTestTag).assertDoesNotExist()
    }

    @Test
    fun `the chooser's restore line says a zine already here isn't added again (N1)`() {
        chooser(lastBackup = null)

        composeRule.onNodeWithText(
            "Add zines from a Zinely backup. Zines already on this shelf aren’t added again. " +
                "If a zine has changed since the backup, the changed one is added too.",
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun `a partial last backup says how many of how many it holds`() {
        chooser(LibraryLastBackup(savedAtEpochMs = SAVED_AT, fileName = null, savedCount = 5, totalCount = 6))

        composeRule.onNodeWithText("— 5 of 6 zines.", substring = true).assertExists()
    }

    private fun assertNoTextBetween(top: androidx.compose.ui.unit.Dp, bottom: androidx.compose.ui.unit.Dp) {
        val between = composeRule.onAllNodes(hasText("", substring = true), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .filter { node ->
                with(composeRule.density) { node.boundsInRoot.top.toDp() >= top && node.boundsInRoot.bottom.toDp() <= bottom }
            }
        assertTrue("nothing sits here, found: ${between.map { it.config.getOrNull(SemanticsProperties.Text) }}", between.isEmpty())
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
