package com.aritr.zinely.data.android

import com.aritr.zinely.core.data.repository.DataResult
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
