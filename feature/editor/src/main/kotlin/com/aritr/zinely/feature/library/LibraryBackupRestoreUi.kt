package com.aritr.zinely.feature.library

/** The library-wide backup / restore mode the shelf UI is currently handling. */
public enum class LibraryBackupRestoreMode {
    Backup,
    Restore,
}

/**
 * The product-level error families the shelf may show for backup / restore. [retryable] is `false`
 * where the same shelf would fail the same way, so the sheet offers only "Got it" (amendment 1a).
 */
public enum class LibraryBackupRestoreFailureKind(public val retryable: Boolean = true) {
    Damaged,
    NewerAppNeeded,
    ReadFailed,
    SaveFailed,
    NotEnoughSpace,
    Busy,
    Generic,

    /** Backup only, 0 of M saved (ADR-122 §4): "No zines could be saved", no file written. */
    BackupNoneSaved(retryable = false),

    /** [BackupNoneSaved], and a left-out zine has no shelf row. */
    BackupNoneSavedOffShelf(retryable = false),

    /** [BackupNoneSaved] where every zine was left out for a photo. */
    BackupNoneSavedPhoto(retryable = false),

    /** Backup only, 0 of M saved and every left-out zine needs a newer Zinely. */
    BackupZineNewer(retryable = false),

    /** Backup only: "Couldn't finish that backup" at a deterministic archive-wide or library-wide limit. */
    BackupLimitReached(retryable = false),
}

/** Why a backup left a zine out, in the order the sheet names reasons (amendment 1a). */
public enum class LibraryOmissionReason {
    Unreadable,
    Newer,
    Photo,
}

/** A zine a backup left out; [title] is null when it had no readable name (counted, not named). */
public data class LibraryOmission(
    val title: String?,
    val reason: LibraryOmissionReason,
)

/**
 * The last saved backup, for the chooser's fact line (formatted at the UI edge). The counts are set only for
 * a backup saved since step 1b; partial ⇔ [savedCount] < [totalCount].
 */
public data class LibraryLastBackup(
    val savedAtEpochMs: Long,
    val fileName: String?,
    val savedCount: Int? = null,
    val totalCount: Int? = null,
)

/**
 * The backup / restore surface currently standing over the shelf.
 *
 * The "Keep safe" choice sheet remains screen-local; this state only models flows owned by the host
 * view model and repository boundary.
 */
public sealed interface LibraryBackupRestoreUiState {
    /** [cancellable] is false once a restore's commit has started (ADR-122 R2): Cancel leaves the tree. */
    public data class Running(
        val mode: LibraryBackupRestoreMode,
        val cancellable: Boolean = true,
    ) : LibraryBackupRestoreUiState

    /**
     * Partial ⇔ [omitted] is non-empty; [projectCount] of [totalCount] zines are in the file. [offShelfReasons] are the
     * reasons with a left-out zine that has no shelf row: only their sentences say so.
     */
    public data class BackupSaved(
        val projectCount: Int,
        val assetCount: Int,
        val totalCount: Int = projectCount,
        val omitted: List<LibraryOmission> = emptyList(),
        val offShelfReasons: Set<LibraryOmissionReason> = emptySet(),
    ) : LibraryBackupRestoreUiState

    /**
     * A restore that reached its result (ADR-121 N2–N5, ADR-122 R3). [restoredProjectCount] 0 is "Nothing new to
     * add"; [omitted] is what the backup itself was saved without.
     */
    public data class RestoreAdded(
        val restoredProjectCount: Int,
        val alreadyHereCount: Int = 0,
        val shelfUpToDate: Boolean = true,
        val omitted: List<LibraryOmission> = emptyList(),
    ) : LibraryBackupRestoreUiState

    public data class Failed(
        val mode: LibraryBackupRestoreMode,
        val kind: LibraryBackupRestoreFailureKind,
    ) : LibraryBackupRestoreUiState
}
