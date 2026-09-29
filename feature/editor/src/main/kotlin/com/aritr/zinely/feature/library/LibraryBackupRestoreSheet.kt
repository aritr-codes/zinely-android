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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.liveRegion
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
import com.aritr.zinely.ui.components.ZSheetParagraphBreak
import com.aritr.zinely.ui.components.ZStampButton
import com.aritr.zinely.ui.components.zinelyV21Frame
import com.aritr.zinely.ui.components.zinelyV21HardShadow
import com.aritr.zinely.ui.components.zinelySweep
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import java.text.DateFormat
import java.util.Date

internal const val KeepSafeSheetTestTag = "keep-safe-sheet"
internal const val KeepSafeSaveActionTestTag = "keep-safe-save"
internal const val KeepSafeRestoreActionTestTag = "keep-safe-restore"
internal const val BackupRestoreRunningSheetTestTag = "backup-restore-running"
internal const val BackupRestoreSuccessSheetTestTag = "backup-restore-success"
internal const val BackupRestoreErrorSheetTestTag = "backup-restore-error"
internal const val BackupRestoreCancelTestTag = "backup-restore-cancel"
internal const val BackupRestoreDoneTestTag = "backup-restore-done"
internal const val BackupRestoreRetryTestTag = "backup-restore-retry"
internal const val KeepSafeLastBackupTestTag = "keep-safe-last-backup"

/** The device locale's medium date and the device time zone, never a relative time (amendment 1a item 1). */
internal fun lastBackupLine(lastBackup: LibraryLastBackup?): String {
    if (lastBackup == null) return Copy.LibraryBackup.NO_BACKUP_YET
    val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(lastBackup.savedAtEpochMs))
    val saved = lastBackup.savedCount
    val total = lastBackup.totalCount
    return if (saved != null && total != null && saved < total) {
        Copy.LibraryBackup.lastBackupSavedPartial(date, saved, total)
    } else {
        Copy.LibraryBackup.lastBackupSaved(date)
    }
}

/** A result sheet's words: its title, then one paragraph per line of the frozen state, in order. */
internal data class ResultCopy(val title: String, val paragraphs: List<String>)

/**
 * A saved backup (amendment 1a items 5 and 10): complete, or "N of M zines saved" with one paragraph per reason in
 * the order couldn't open · newer Zinely · couldn't read a photo. The off-shelf sentence closes only the paragraph of
 * a reason that has an off-shelf zine, so it is never said of zines that are on the shelf.
 */
internal fun backupSavedCopy(state: LibraryBackupRestoreUiState.BackupSaved): ResultCopy {
    if (state.omitted.isEmpty()) {
        return ResultCopy(
            Copy.LibraryBackup.backupSavedTitle(),
            listOf(Copy.LibraryBackup.backupSavedBody(state.projectCount)),
        )
    }
    val paragraphs = LibraryOmissionReason.entries.mapNotNull { reason ->
        val group = state.omitted.filter { it.reason == reason }
        val names = group.mapNotNull { it.title }
        when {
            group.isEmpty() -> null
            reason == LibraryOmissionReason.Unreadable -> Copy.LibraryBackup.backupCouldntOpen(group.size, names)
            reason == LibraryOmissionReason.Newer -> Copy.LibraryBackup.backupNewer(group.size, names)
            else -> Copy.LibraryBackup.backupCouldntReadPhoto(group.size, names)
        }?.let { sentence ->
            if (reason in state.offShelfReasons) "$sentence ${Copy.LibraryBackup.BACKUP_SOME_OFF_SHELF}" else sentence
        }
    }
    return ResultCopy(Copy.LibraryBackup.backupPartialTitle(state.projectCount, state.totalCount), paragraphs)
}

/**
 * A restore's result in Amendment N5's order: title → already here → what the backup was saved without → the
 * lagging line (ADR-122 R3) or "stayed put". Nothing new (N4) says neither of the last two: nothing was added.
 */
internal fun restoreAddedCopy(state: LibraryBackupRestoreUiState.RestoreAdded): ResultCopy {
    val savedWithout = savedWithoutNotice(state.omitted)
    if (state.restoredProjectCount == 0) {
        return ResultCopy(
            Copy.LibraryBackup.RESTORE_NOTHING_NEW_TITLE,
            listOfNotNull(Copy.LibraryBackup.restoreNothingNewBody(state.alreadyHereCount), savedWithout),
        )
    }
    return ResultCopy(
        Copy.LibraryBackup.restoreAddedTitle(state.restoredProjectCount),
        listOfNotNull(
            state.alreadyHereCount.takeIf { it > 0 }?.let(Copy.LibraryBackup::restoreAlreadyHere),
            savedWithout,
            if (state.shelfUpToDate) Copy.LibraryBackup.RESTORE_SUCCESS_BODY else Copy.LibraryBackup.RESTORE_LAGGING,
        ),
    )
}

/**
 * One paragraph (N5 has room for one): "couldn't be opened then" (unreadable and newer; a newer-only notice isn't
 * drawn), then "whose photo couldn't be read then"; a reason with no readable name joins the unnamed group. One
 * reason keeps its own sentence; several share one sentence that gives the total once (owner ruling (g), Option 3).
 */
private fun savedWithoutNotice(omitted: List<LibraryOmission>): String? {
    val (photo, opened) = omitted.partition { it.reason == LibraryOmissionReason.Photo }
    var unnamed = 0
    // Each reason as (its own sentence, its clause in a shared one).
    val reasons = buildList {
        listOf(
            Triple(opened, Copy.LibraryBackup::restoreSavedWithoutOpened, Copy.LibraryBackup::restoreSavedWithoutOpenedClause),
            Triple(photo, Copy.LibraryBackup::restoreSavedWithoutPhoto, Copy.LibraryBackup::restoreSavedWithoutPhotoClause),
        ).forEach { (group, sentence, clause) ->
            val names = group.mapNotNull { it.title }
            if (names.isEmpty()) unnamed += group.size else add(sentence(group.size, names) to clause(group.size, names))
        }
        if (unnamed > 0) {
            add(Copy.LibraryBackup.restoreSavedWithoutUnnamed(unnamed) to Copy.LibraryBackup.restoreSavedWithoutUnnamedClause(unnamed))
        }
    }
    return when (reasons.size) {
        0 -> null
        1 -> reasons.single().first
        else -> Copy.LibraryBackup.restoreSavedWithoutSeveral(omitted.size, reasons.map { it.second })
    }
}

@Composable
internal fun KeepSafeSheet(
    visible: Boolean,
    canBackup: Boolean,
    lastBackup: LibraryLastBackup?,
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
        // The last-backup fact sits after the body, before the action it explains; an empty shelf doesn't
        // show it (amendment 1a item 1). No note follows it on either shelf (backup-sheet polish: removed, not
        // replaced). The file name is its own line under the date, whole, with no separator. The two lines are
        // one TalkBack stop: merged, their texts are announced in order with a pause between, never fused.
        if (canBackup) {
            val lineStyle = TextStyle(
                color = colors.inkSoft,
                fontFamily = ZinelyV21Fonts.Work,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            Column(
                Modifier
                    .padding(start = ZinelyV21Dimens.gapHair)
                    .testTag(KeepSafeLastBackupTestTag)
                    .semantics(mergeDescendants = true) {},
            ) {
                Text(text = lastBackupLine(lastBackup), style = lineStyle)
                lastBackup?.fileName?.takeIf { it.isNotBlank() }?.let { Text(text = it, style = lineStyle) }
            }
        }

        if (canBackup) {
            KeepSafeOption(
                testTag = KeepSafeSaveActionTestTag,
                label = Copy.LibraryBackup.SAVE_ACTION,
                body = Copy.LibraryBackup.SAVE_BODY,
                glyph = "⤓",
                onClick = onSaveBackup,
                focusRequester = firstActionFocus,
            )
        }
        KeepSafeOption(
            testTag = KeepSafeRestoreActionTestTag,
            label = Copy.LibraryBackup.RESTORE_ACTION,
            body = if (canBackup) Copy.LibraryBackup.RESTORE_BODY else Copy.LibraryBackup.EMPTY_RESTORE_BODY,
            glyph = "↺",
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
        is LibraryBackupRestoreUiState.Running -> RunningSheet(state.mode, state.cancellable, onCancel)
        is LibraryBackupRestoreUiState.BackupSaved -> SuccessSheet(backupSavedCopy(state), onDismiss)
        is LibraryBackupRestoreUiState.RestoreAdded -> SuccessSheet(restoreAddedCopy(state), onDismiss)
        is LibraryBackupRestoreUiState.Failed -> ErrorSheet(state, onDismiss, onRetry)
    }
}

@Composable
private fun KeepSafeOption(
    testTag: String,
    label: String,
    body: String,
    glyph: String,
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
        // One tile for both actions, so they read as peers (backup-sheet polish). Backup's old butterTint tile
        // was the sheet surface colour in both themes, so it didn't show and Restore read as primary.
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(ZinelyV21Dimens.radiusSm))
                .background(colors.leafTint),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = glyph,
                style = TextStyle(
                    color = colors.onLeaf,
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
                    color = if (pressed) colors.onLeaf else colors.ink,
                    fontFamily = ZinelyV21Fonts.Work,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                ),
            )
            Text(
                text = body,
                style = TextStyle(
                    color = if (pressed) colors.onLeaf else colors.inkSoft,
                    fontFamily = ZinelyV21Fonts.Work,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                ),
            )
        }
    }
}

@Composable
private fun RunningSheet(mode: LibraryBackupRestoreMode, cancellable: Boolean, onCancel: () -> Unit) {
    val colors = ZinelyTheme.v21Colors
    val cancelFocus = remember { FocusRequester() }
    // ADR-122 R2: once a restore's commit starts, Cancel leaves the tree and neither Back nor a scrim tap
    // cancels. The view model's latch refuses a Cancel that raced the change, so this is presentation only.
    val committing = mode == LibraryBackupRestoreMode.Restore && !cancellable
    ZSheet(
        visible = true,
        onDismiss = if (committing) ({}) else onCancel,
        onShown = { if (!committing) cancelFocus.requestFocus() },
        title = if (mode == LibraryBackupRestoreMode.Backup) {
            Copy.LibraryBackup.BACKUP_RUNNING_TITLE
        } else {
            Copy.LibraryBackup.RESTORE_RUNNING_TITLE
        },
        sub = when {
            mode == LibraryBackupRestoreMode.Backup -> Copy.LibraryBackup.BACKUP_RUNNING_BODY
            committing -> Copy.LibraryBackup.RESTORE_COMMIT_BODY
            else -> Copy.LibraryBackup.RESTORE_RUNNING_BODY
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
                .background(colors.surface)
                .border(1.5.dp, colors.hair, RoundedCornerShape(14.dp))
                .zinelySweep(),
        )
        Text(
            text = when {
                mode == LibraryBackupRestoreMode.Backup -> Copy.LibraryBackup.BACKUP_RUNNING_HINT
                committing -> Copy.LibraryBackup.RESTORE_COMMIT_HINT
                else -> Copy.LibraryBackup.RESTORE_RUNNING_HINT
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = ZinelyV21Dimens.gapSm)
                // When the commit starts, the focused Cancel leaves the tree; the hint changing to "This part
                // can't be stopped." is then announced rather than silent (accessibility, not a redesign).
                .semantics { liveRegion = LiveRegionMode.Polite },
            textAlign = TextAlign.Center,
            style = TextStyle(
                color = colors.inkSoft,
                fontFamily = ZinelyV21Fonts.Work,
                fontSize = 12.sp,
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
            ),
        )
        if (!committing) {
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
}

@Composable
private fun SuccessSheet(
    copy: ResultCopy,
    onDismiss: () -> Unit,
) {
    val colors = ZinelyTheme.v21Colors
    val doneFocus = remember { FocusRequester() }
    ZSheet(
        visible = true,
        onDismiss = onDismiss,
        onShown = doneFocus::requestFocus,
        title = copy.title,
        // The frozen body / note / more paragraphs, drawn 8dp apart and read by TalkBack in order as one stop.
        sub = copy.paragraphs.joinToString(ZSheetParagraphBreak),
        modifier = Modifier.testTag(BackupRestoreSuccessSheetTestTag).verticalScroll(rememberScrollState()),
    ) {
        Mark("✓", colors.paper, colors.leaf, colors.onLeaf)
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
    val isBackup = state.mode == LibraryBackupRestoreMode.Backup
    val title = when (state.kind) {
        LibraryBackupRestoreFailureKind.Damaged -> Copy.LibraryBackup.ERROR_DAMAGED_TITLE
        LibraryBackupRestoreFailureKind.NewerAppNeeded -> Copy.LibraryBackup.ERROR_NEWER_TITLE
        LibraryBackupRestoreFailureKind.ReadFailed -> Copy.LibraryBackup.ERROR_READ_TITLE
        LibraryBackupRestoreFailureKind.SaveFailed -> Copy.LibraryBackup.ERROR_SAVE_TITLE
        LibraryBackupRestoreFailureKind.NotEnoughSpace -> Copy.LibraryBackup.ERROR_SPACE_TITLE
        LibraryBackupRestoreFailureKind.Busy -> Copy.LibraryBackup.ERROR_BUSY_TITLE
        LibraryBackupRestoreFailureKind.Generic,
        LibraryBackupRestoreFailureKind.BackupLimitReached,
        -> Copy.LibraryBackup.errorGenericTitle(isBackup)
        LibraryBackupRestoreFailureKind.BackupNoneSaved,
        LibraryBackupRestoreFailureKind.BackupNoneSavedOffShelf,
        LibraryBackupRestoreFailureKind.BackupNoneSavedPhoto,
        -> Copy.LibraryBackup.BACKUP_NONE_TITLE
        LibraryBackupRestoreFailureKind.BackupZineNewer -> Copy.LibraryBackup.BACKUP_ZINE_NEWER_TITLE
    }
    val body = when (state.kind) {
        LibraryBackupRestoreFailureKind.Damaged -> Copy.LibraryBackup.ERROR_DAMAGED_BODY
        LibraryBackupRestoreFailureKind.NewerAppNeeded -> Copy.LibraryBackup.ERROR_NEWER_BODY
        LibraryBackupRestoreFailureKind.ReadFailed -> Copy.LibraryBackup.ERROR_READ_BODY
        LibraryBackupRestoreFailureKind.SaveFailed -> Copy.LibraryBackup.ERROR_SAVE_BODY
        LibraryBackupRestoreFailureKind.NotEnoughSpace -> Copy.LibraryBackup.ERROR_SPACE_BODY
        LibraryBackupRestoreFailureKind.Busy -> Copy.LibraryBackup.ERROR_BUSY_BODY
        LibraryBackupRestoreFailureKind.Generic,
        LibraryBackupRestoreFailureKind.BackupLimitReached,
        -> Copy.LibraryBackup.errorGenericBody(isBackup)
        LibraryBackupRestoreFailureKind.BackupNoneSaved -> Copy.LibraryBackup.BACKUP_NONE_BODY
        LibraryBackupRestoreFailureKind.BackupNoneSavedOffShelf -> Copy.LibraryBackup.BACKUP_NONE_OFF_SHELF_BODY
        LibraryBackupRestoreFailureKind.BackupNoneSavedPhoto -> Copy.LibraryBackup.BACKUP_NONE_PHOTO_BODY
        LibraryBackupRestoreFailureKind.BackupZineNewer -> Copy.LibraryBackup.BACKUP_ZINE_NEWER_BODY
    }
    // A failure no retry can fix offers only "Got it" (amendment 1a); focus then lands on it.
    val retry = when {
        !state.kind.retryable -> null
        isBackup -> Copy.LibraryBackup.TRY_AGAIN
        else -> Copy.LibraryBackup.TRY_ANOTHER_BACKUP
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
        Mark("!", colors.paper, colors.jam, colors.onLeaf)
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
    retry: String?,
    firstActionFocus: FocusRequester,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (retry != null) {
        ZStampButton(
            text = retry,
            onClick = onRetry,
            modifier = Modifier
                .focusRequester(firstActionFocus)
                .testTag(BackupRestoreRetryTestTag),
        )
    }
    ZPrimaryButton(
        text = Copy.LibraryBackup.GOT_IT,
        onClick = onDismiss,
        metrics = ZPrimaryButtonMetrics.Shelf,
        modifier = Modifier
            .then(if (retry == null) Modifier.focusRequester(firstActionFocus) else Modifier)
            .testTag(BackupRestoreDoneTestTag),
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
