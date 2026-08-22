package com.aritr.zinely.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.a11y.zinelyV2Control
import com.aritr.zinely.ui.components.ZPrimaryButton
import com.aritr.zinely.ui.components.ZPrimaryButtonMetrics
import com.aritr.zinely.ui.components.ZSheet
import com.aritr.zinely.ui.components.ZStampButton
import com.aritr.zinely.ui.components.zinelyV21Frame
import com.aritr.zinely.ui.components.zinelyV21HardShadow
import com.aritr.zinely.ui.components.zinelySweep
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts

internal const val KeepSafeSheetTestTag = "keep-safe-sheet"
internal const val KeepSafeSaveActionTestTag = "keep-safe-save"
internal const val KeepSafeRestoreActionTestTag = "keep-safe-restore"
internal const val BackupRestoreRunningSheetTestTag = "backup-restore-running"
internal const val BackupRestoreSuccessSheetTestTag = "backup-restore-success"
internal const val BackupRestoreErrorSheetTestTag = "backup-restore-error"
internal const val BackupRestoreCancelTestTag = "backup-restore-cancel"
internal const val BackupRestoreDoneTestTag = "backup-restore-done"
internal const val BackupRestoreRetryTestTag = "backup-restore-retry"

@Composable
internal fun KeepSafeSheet(
    visible: Boolean,
    canBackup: Boolean,
    onDismiss: () -> Unit,
    onHidden: () -> Unit,
    onSaveBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
) {
    val colors = ZinelyTheme.v21Colors
    val firstActionFocus = remember { FocusRequester() }
    ZSheet(
        visible = visible,
        onDismiss = onDismiss,
        onShown = firstActionFocus::requestFocus,
        onHidden = onHidden,
        title = if (canBackup) Copy.LibraryBackup.TITLE else Copy.LibraryBackup.EMPTY_TITLE,
        sub = if (canBackup) Copy.LibraryBackup.SHEET_BODY else Copy.LibraryBackup.EMPTY_BODY,
        modifier = Modifier
            .testTag(KeepSafeSheetTestTag)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = if (canBackup) Copy.LibraryBackup.PRIVACY_NOTE else Copy.LibraryBackup.EMPTY_PRIVACY_NOTE,
            modifier = Modifier
                .padding(start = ZinelyV21Dimens.gapHair, top = ZinelyV21Dimens.gapXs)
                .clip(RoundedCornerShape(ZinelyV21Dimens.radiusPill))
                .background(colors.leafTint)
                .padding(horizontal = ZinelyV21Dimens.gapMd, vertical = ZinelyV21Dimens.gapSm),
            style = TextStyle(
                color = colors.leafText,
                fontFamily = ZinelyV21Fonts.Work,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
            ),
        )

        if (canBackup) {
            KeepSafeOption(
                testTag = KeepSafeSaveActionTestTag,
                label = Copy.LibraryBackup.SAVE_ACTION,
                body = Copy.LibraryBackup.SAVE_BODY,
                glyph = "⤓",
                tint = colors.butterTint,
                glyphTint = colors.inkSoft,
                onClick = onSaveBackup,
                focusRequester = firstActionFocus,
            )
        }
        KeepSafeOption(
            testTag = KeepSafeRestoreActionTestTag,
            label = Copy.LibraryBackup.RESTORE_ACTION,
            body = if (canBackup) Copy.LibraryBackup.RESTORE_BODY else Copy.LibraryBackup.EMPTY_RESTORE_BODY,
            glyph = "↺",
            tint = colors.leafTint,
            glyphTint = colors.leafText,
            onClick = onRestoreBackup,
            focusRequester = if (canBackup) null else firstActionFocus,
        )
    }
}

@Composable
internal fun LibraryBackupRestoreStateSheet(
    state: LibraryBackupRestoreUiState?,
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        null -> Unit
        is LibraryBackupRestoreUiState.Running -> RunningSheet(state.mode, onCancel)
        is LibraryBackupRestoreUiState.BackupSaved -> SuccessSheet(
            title = Copy.LibraryBackup.backupSavedTitle(),
            body = Copy.LibraryBackup.backupSavedBody(state.projectCount),
            testTag = BackupRestoreSuccessSheetTestTag,
            onDismiss = onDismiss,
        )
        is LibraryBackupRestoreUiState.RestoreAdded -> SuccessSheet(
            title = Copy.LibraryBackup.restoreAddedTitle(state.restoredProjectCount),
            body = Copy.LibraryBackup.RESTORE_SUCCESS_BODY,
            testTag = BackupRestoreSuccessSheetTestTag,
            onDismiss = onDismiss,
        )
        is LibraryBackupRestoreUiState.Failed -> ErrorSheet(state, onDismiss, onRetry)
    }
}

@Composable
private fun KeepSafeOption(
    testTag: String,
    label: String,
    body: String,
    glyph: String,
    tint: androidx.compose.ui.graphics.Color,
    glyphTint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    val colors = ZinelyTheme.v21Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .testTag(testTag)
            .zinelyV2Control(label = label, interactionSource = interaction, onClick = onClick)
            .background(if (pressed) colors.leafTint else androidx.compose.ui.graphics.Color.Transparent)
            .padding(horizontal = ZinelyV21Dimens.gapXl, vertical = ZinelyV21Dimens.gapLg),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapLg),
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(ZinelyV21Dimens.radiusSm))
                .background(tint),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = glyph,
                style = TextStyle(
                    color = glyphTint,
                    fontFamily = ZinelyV21Fonts.Work,
                    fontSize = 15.sp,
                    lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                ),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = TextStyle(
                    color = colors.ink,
                    fontFamily = ZinelyV21Fonts.Work,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                ),
            )
            Text(
                text = body,
                style = TextStyle(
                    color = colors.inkSoft,
                    fontFamily = ZinelyV21Fonts.Work,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                ),
            )
        }
    }
}

@Composable
private fun RunningSheet(mode: LibraryBackupRestoreMode, onCancel: () -> Unit) {
    val colors = ZinelyTheme.v21Colors
    val cancelFocus = remember { FocusRequester() }
    ZSheet(
        visible = true,
        onDismiss = onCancel,
        onShown = cancelFocus::requestFocus,
        title = if (mode == LibraryBackupRestoreMode.Backup) {
            Copy.LibraryBackup.BACKUP_RUNNING_TITLE
        } else {
            Copy.LibraryBackup.RESTORE_RUNNING_TITLE
        },
        sub = if (mode == LibraryBackupRestoreMode.Backup) {
            Copy.LibraryBackup.BACKUP_RUNNING_BODY
        } else {
            Copy.LibraryBackup.RESTORE_RUNNING_BODY
        },
        modifier = Modifier
            .testTag(BackupRestoreRunningSheetTestTag)
            .verticalScroll(rememberScrollState())
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                stateDescription = Copy.LibraryBackup.RUNNING_STATE_DESCRIPTION
            },
    ) {
        Box(
            Modifier
                .padding(top = ZinelyV21Dimens.gapXs)
                .align(Alignment.CenterHorizontally)
                .size(width = 86.dp, height = 114.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.paper)
                .border(1.5.dp, colors.hair, RoundedCornerShape(14.dp))
                .zinelySweep(),
        )
        Text(
            text = if (mode == LibraryBackupRestoreMode.Backup) {
                Copy.LibraryBackup.BACKUP_RUNNING_HINT
            } else {
                Copy.LibraryBackup.RESTORE_RUNNING_HINT
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = ZinelyV21Dimens.gapSm),
            textAlign = TextAlign.Center,
            style = TextStyle(
                color = colors.inkFaint,
                fontFamily = ZinelyV21Fonts.Work,
                fontSize = 12.sp,
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
            ),
        )
        ZStampButton(
            text = Copy.LibraryBackup.CANCEL,
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = ZinelyV21Dimens.gapLg)
                .focusRequester(cancelFocus)
                .testTag(BackupRestoreCancelTestTag),
        )
    }
}

@Composable
private fun SuccessSheet(
    title: String,
    body: String,
    testTag: String,
    onDismiss: () -> Unit,
) {
    val colors = ZinelyTheme.v21Colors
    val doneFocus = remember { FocusRequester() }
    ZSheet(
        visible = true,
        onDismiss = onDismiss,
        onShown = doneFocus::requestFocus,
        title = title,
        sub = body,
        modifier = Modifier.testTag(testTag).verticalScroll(rememberScrollState()),
    ) {
        Mark("✓", colors.paper, colors.leaf, colors.leafText)
        ZPrimaryButton(
            text = Copy.LibraryBackup.DONE,
            onClick = onDismiss,
            metrics = ZPrimaryButtonMetrics.Shelf,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = ZinelyV21Dimens.gapLg)
                .focusRequester(doneFocus)
                .testTag(BackupRestoreDoneTestTag),
        )
    }
}

@Composable
private fun ErrorSheet(
    state: LibraryBackupRestoreUiState.Failed,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    val retryFocus = remember { FocusRequester() }
    val title = when (state.kind) {
        LibraryBackupRestoreFailureKind.Damaged -> Copy.LibraryBackup.ERROR_DAMAGED_TITLE
        LibraryBackupRestoreFailureKind.NewerAppNeeded -> Copy.LibraryBackup.ERROR_NEWER_TITLE
        LibraryBackupRestoreFailureKind.ReadFailed -> Copy.LibraryBackup.ERROR_READ_TITLE
        LibraryBackupRestoreFailureKind.SaveFailed -> Copy.LibraryBackup.ERROR_SAVE_TITLE
        LibraryBackupRestoreFailureKind.NotEnoughSpace -> Copy.LibraryBackup.ERROR_SPACE_TITLE
        LibraryBackupRestoreFailureKind.Busy -> Copy.LibraryBackup.ERROR_BUSY_TITLE
        LibraryBackupRestoreFailureKind.Generic ->
            Copy.LibraryBackup.errorGenericTitle(state.mode == LibraryBackupRestoreMode.Backup)
    }
    val body = when (state.kind) {
        LibraryBackupRestoreFailureKind.Damaged -> Copy.LibraryBackup.ERROR_DAMAGED_BODY
        LibraryBackupRestoreFailureKind.NewerAppNeeded -> Copy.LibraryBackup.ERROR_NEWER_BODY
        LibraryBackupRestoreFailureKind.ReadFailed -> Copy.LibraryBackup.ERROR_READ_BODY
        LibraryBackupRestoreFailureKind.SaveFailed -> Copy.LibraryBackup.ERROR_SAVE_BODY
        LibraryBackupRestoreFailureKind.NotEnoughSpace -> Copy.LibraryBackup.ERROR_SPACE_BODY
        LibraryBackupRestoreFailureKind.Busy -> Copy.LibraryBackup.ERROR_BUSY_BODY
        LibraryBackupRestoreFailureKind.Generic ->
            Copy.LibraryBackup.errorGenericBody(state.mode == LibraryBackupRestoreMode.Backup)
    }
    val retry = if (state.mode == LibraryBackupRestoreMode.Backup) {
        Copy.LibraryBackup.TRY_AGAIN
    } else {
        Copy.LibraryBackup.TRY_ANOTHER_BACKUP
    }
    val stackActions = LocalDensity.current.fontScale >= 1.5f

    ZSheet(
        visible = true,
        onDismiss = onDismiss,
        onShown = retryFocus::requestFocus,
        title = title,
        sub = body,
        modifier = Modifier
            .testTag(BackupRestoreErrorSheetTestTag)
            .verticalScroll(rememberScrollState()),
    ) {
        val colors = ZinelyTheme.v21Colors
        Mark("!", colors.paper, colors.jam, colors.jam)
        val actionsModifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(top = ZinelyV21Dimens.gapLg)
        if (stackActions) {
            Column(
                modifier = actionsModifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapMd),
            ) {
                ErrorActions(retry, retryFocus, onRetry, onDismiss)
            }
        } else {
            Row(
                modifier = actionsModifier,
                horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapMd),
            ) {
                ErrorActions(retry, retryFocus, onRetry, onDismiss)
            }
        }
    }
}

@Composable
private fun ErrorActions(
    retry: String,
    retryFocus: FocusRequester,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    ZStampButton(
        text = retry,
        onClick = onRetry,
        modifier = Modifier
            .focusRequester(retryFocus)
            .testTag(BackupRestoreRetryTestTag),
    )
    ZPrimaryButton(
        text = Copy.LibraryBackup.GOT_IT,
        onClick = onDismiss,
        metrics = ZPrimaryButtonMetrics.Shelf,
        modifier = Modifier.testTag(BackupRestoreDoneTestTag),
    )
}

@Composable
private fun ColumnScope.Mark(
    glyph: String,
    paper: androidx.compose.ui.graphics.Color,
    border: androidx.compose.ui.graphics.Color,
    ink: androidx.compose.ui.graphics.Color,
) {
    Box(
        Modifier
            .align(Alignment.CenterHorizontally)
            .padding(top = ZinelyV21Dimens.gapXs)
            .size(60.dp)
            .zinelyV21HardShadow(3.dp, ZinelyTheme.v21Colors.inkLine.copy(alpha = 0.18f), RoundedCornerShape(ZinelyV21Dimens.radiusPill))
            .clip(RoundedCornerShape(ZinelyV21Dimens.radiusPill))
            .background(paper)
            .border(2.dp, border, RoundedCornerShape(ZinelyV21Dimens.radiusPill)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            style = TextStyle(
                color = ink,
                fontFamily = ZinelyV21Fonts.Voice,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 28.sp,
            ),
        )
    }
}
