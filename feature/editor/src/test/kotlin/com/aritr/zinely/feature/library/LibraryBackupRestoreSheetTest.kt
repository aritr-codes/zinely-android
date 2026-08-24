package com.aritr.zinely.feature.library

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
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
