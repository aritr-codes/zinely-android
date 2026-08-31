package com.aritr.zinely.feature.library

/** The library-wide backup / restore mode the shelf UI is currently handling. */
public enum class LibraryBackupRestoreMode {
    Backup,
    Restore,
}

/** The product-level error families the shelf may show for backup / restore. */
public enum class LibraryBackupRestoreFailureKind {
    Damaged,
    NewerAppNeeded,
    ReadFailed,
    SaveFailed,
    NotEnoughSpace,
    Busy,
    Generic,
}

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
