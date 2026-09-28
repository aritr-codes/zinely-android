package com.aritr.zinely.data.android

import com.aritr.zinely.core.data.asset.MAX_BACKUP_ASSET_BYTES
import com.aritr.zinely.core.data.asset.MAX_BACKUP_DOCUMENT_BYTES
import com.aritr.zinely.core.data.asset.MAX_BACKUP_TOTAL_BYTES
import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.storage.ZineBackupWritingException
import java.nio.file.Path

/** Creates one complete, validated library backup at an app-private [destination]. */
public interface LibraryBackupRepository {
    public suspend fun createLibraryBackup(destination: Path): DataResult<LibraryBackupReceipt>
}

/** Summary of the private archive that is ready to be delivered to user-owned storage. */
public data class LibraryBackupReceipt(
    val projectCount: Int,
    val assetCount: Int,
    val archiveByteCount: Long,
)

/**
 * Maps a writer failure on the **private** archive to the error the maker's state is chosen from
 * (owner ruling F1, ADR-120). None of these is a destination failure: the maker's file isn't touched
 * until the archive is complete.
 *
 * - A write-side I/O failure is [DataError.OutOfSpace] only when the private disk verifiably can't hold
 *   the declared bytes (the ADR-036 probe; a probe that can't answer never invents a full disk), else [DataError.Io].
 * - [LIMIT_EXCEEDED][ZineBackupWritingException.Reason.LIMIT_EXCEEDED] is deterministic:
 *   [DataError.LimitExceeded]. Part 1 treats a per-entry limit the same way; 1b moves the per-photo case to
 *   skip-and-list once the writer names the entry.
 * - Invariant: the writer's manifest validation enforces the document, photo and total size limits *before*
 *   its own limit checks, so those limits arrive as INVALID_MANIFEST. Declared sizes over a limit are
 *   therefore [DataError.LimitExceeded] whatever the reason says — never "a zine here can't be opened".
 * - Otherwise a mismatch or invalid manifest stays [DataError.Corrupt], as before.
 */
internal fun backupWriteError(
    failure: ZineBackupWritingException,
    documentBytes: List<Long>,
    assetBytes: List<Long>,
    usableBytes: () -> Long,
): DataError {
    val requiredBytes = (documentBytes + assetBytes).fold(0L) { total, bytes ->
        if (total > Long.MAX_VALUE - bytes) Long.MAX_VALUE else total + bytes
    }
    val overLimit = documentBytes.any { it > MAX_BACKUP_DOCUMENT_BYTES } ||
        assetBytes.any { it > MAX_BACKUP_ASSET_BYTES } ||
        requiredBytes > MAX_BACKUP_TOTAL_BYTES
    if (overLimit) return DataError.LimitExceeded("the library is beyond what one backup can hold", failure)
    return classifyWriteReason(failure, requiredBytes, usableBytes)
}

private fun classifyWriteReason(
    failure: ZineBackupWritingException,
    requiredBytes: Long,
    usableBytes: () -> Long,
): DataError = when (failure.reason) {
    ZineBackupWritingException.Reason.IO_FAILURE -> {
        val full = try {
            usableBytes() < requiredBytes
        } catch (_: Exception) {
            false
        }
        if (full) {
            DataError.OutOfSpace("not enough space for the private library backup", failure)
        } else {
            DataError.Io("couldn't create the private library backup", failure)
        }
    }
    ZineBackupWritingException.Reason.DESTINATION_EXISTS,
    ZineBackupWritingException.Reason.SOURCE_UNAVAILABLE,
    -> DataError.Io("couldn't create the private library backup", failure)
    ZineBackupWritingException.Reason.LIMIT_EXCEEDED ->
        DataError.LimitExceeded("the library is beyond what one backup can hold", failure)
    ZineBackupWritingException.Reason.INVALID_MANIFEST,
    ZineBackupWritingException.Reason.SOURCE_MISMATCH,
    ZineBackupWritingException.Reason.INTEGRITY_MISMATCH,
    -> DataError.Corrupt("the local library could not be backed up safely", failure)
}
