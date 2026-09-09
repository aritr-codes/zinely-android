package com.aritr.zinely.feature.library

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.aritr.zinely.ui.theme.LocalZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Visual contract for the four production backup / restore sheets in both Zinely themes. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h812dp")
class LibraryBackupRestoreGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `backup chooser light`() = chooser(dark = false)

    @Test
    fun `backup chooser dark`() = chooser(dark = true)

    @Test
    fun `restore running light`() = state(
        name = "running_light",
        dark = false,
        tag = BackupRestoreRunningSheetTestTag,
        value = LibraryBackupRestoreUiState.Running(LibraryBackupRestoreMode.Restore),
    )

    @Test
    fun `restore running dark`() = state(
        name = "running_dark",
        dark = true,
        tag = BackupRestoreRunningSheetTestTag,
        value = LibraryBackupRestoreUiState.Running(LibraryBackupRestoreMode.Restore),
    )

    @Test
    fun `restore success light`() = state(
        name = "success_light",
        dark = false,
        tag = BackupRestoreSuccessSheetTestTag,
        value = LibraryBackupRestoreUiState.RestoreAdded(restoredProjectCount = 3),
    )

    @Test
    fun `restore success dark`() = state(
        name = "success_dark",
        dark = true,
        tag = BackupRestoreSuccessSheetTestTag,
        value = LibraryBackupRestoreUiState.RestoreAdded(restoredProjectCount = 3),
    )

    @Test
    fun `damaged restore error light`() = state(
        name = "error_light",
        dark = false,
        tag = BackupRestoreErrorSheetTestTag,
        value = LibraryBackupRestoreUiState.Failed(
            LibraryBackupRestoreMode.Restore,
            LibraryBackupRestoreFailureKind.Damaged,
        ),
    )

    @Test
    fun `damaged restore error dark`() = state(
        name = "error_dark",
        dark = true,
        tag = BackupRestoreErrorSheetTestTag,
        value = LibraryBackupRestoreUiState.Failed(
            LibraryBackupRestoreMode.Restore,
            LibraryBackupRestoreFailureKind.Damaged,
        ),
    )

    private fun chooser(dark: Boolean) {
        setContent(dark) {
            KeepSafeSheet(
                visible = true,
                canBackup = true,
                onDismiss = {},
                onHidden = {},
                onSaveBackup = {},
                onRestoreBackup = {},
            )
        }
        capture("chooser_${theme(dark)}", KeepSafeSheetTestTag)
    }

    private fun state(
        name: String,
        dark: Boolean,
        tag: String,
        value: LibraryBackupRestoreUiState,
    ) {
        setContent(dark) {
            LibraryBackupRestoreStateSheet(
                state = value,
                onDismiss = {},
                onCancel = {},
                onRetry = {},
            )
        }
        capture(name, tag)
    }

    private fun setContent(dark: Boolean, content: @Composable () -> Unit) {
        composeRule.setContent {
            ZinelyTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalZinelyMotion provides ZinelyMotion(reduceMotion = true)) {
                    content()
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun capture(name: String, tag: String) {
        composeRule.onNodeWithTag(tag).captureRoboImage("$GOLDEN_DIR/backup_restore_$name.png", aa())
    }

    private fun theme(dark: Boolean): String = if (dark) "dark" else "light"

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"

        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }
}
