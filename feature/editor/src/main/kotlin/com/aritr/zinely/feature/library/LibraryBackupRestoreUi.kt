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

    /** Backup only: a zine on this phone couldn't be read, so no backup was saved (part 1's interim). */
    BackupZineUnreadable(retryable = false),

    /** Backup only: a zine on this phone needs a newer Zinely. */
    BackupZineNewer(retryable = false),

    /** Backup only: "Couldn't finish that backup" at a deterministic archive-wide or library-wide limit. */
    BackupLimitReached(retryable = false),
}

/** The last saved backup, for the chooser's fact line (formatted at the UI edge). */
public data class LibraryLastBackup(
    val savedAtEpochMs: Long,
    val fileName: String?,
)

/**
 * The backup / restore surface currently standing over the shelf.
 *
 * The "Keep safe" choice sheet remains screen-local; this state only models flows owned by the host
 * view model and repository boundary.
 */
public sealed interface LibraryBackupRestoreUiState {
    public data class Running(val mode: LibraryBackupRestoreMode) : LibraryBackupRestoreUiState

    public data class BackupSaved(
        val projectCount: Int,
        val assetCount: Int,
    ) : LibraryBackupRestoreUiState

    public data class RestoreAdded(val restoredProjectCount: Int) : LibraryBackupRestoreUiState

    public data class Failed(
        val mode: LibraryBackupRestoreMode,
        val kind: LibraryBackupRestoreFailureKind,
    ) : LibraryBackupRestoreUiState
}
