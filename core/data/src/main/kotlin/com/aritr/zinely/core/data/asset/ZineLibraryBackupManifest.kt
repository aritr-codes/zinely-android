package com.aritr.zinely.core.data.asset

import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineFormat
import kotlinx.serialization.Serializable

/** The package version written by the all-project, user-held library backup. */
public const val CURRENT_LIBRARY_BACKUP_VERSION: Int = 2

/** Discriminator for the v2 all-project package; v1 remains the legacy single-project shape. */
public const val LIBRARY_BACKUP_KIND: String = "library"

/** The one manifest entry at the archive root. */
public const val MANIFEST_PATH: String = "manifest.json"

/** Hard refusal limits for hostile or accidentally explosive archives. */
public const val MAX_BACKUP_MANIFEST_BYTES: Long = 4L * 1024L * 1024L
public const val MAX_BACKUP_DOCUMENT_BYTES: Long = 16L * 1024L * 1024L
public const val MAX_BACKUP_ASSET_BYTES: Long = 128L * 1024L * 1024L
public const val MAX_BACKUP_TOTAL_BYTES: Long = 8L * 1024L * 1024L * 1024L
public const val MAX_BACKUP_PROJECTS: Int = 10_000
public const val MAX_BACKUP_ASSETS: Int = 100_000

/**
 * One project inside a v2 library backup.
 *
 * [sourceProjectId] is identity metadata, not a restore target: restore may retain it when free or
 * mint a new local id on collision. [documentPath] is validated as a canonical archive-relative
 * path before any bytes are staged. Cover fields use their persisted enum names so the backup
 * preserves the shelf identity owned by `meta.json` without copying that private sidecar format.
 */
@Serializable
public data class ZineBackupProjectEntry(
    val sourceProjectId: String,
    val title: String,
    val format: ZineFormat,
    val paperSize: PaperSize,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val documentSchemaVersion: Int,
    val documentPath: String,
    val documentSha256: String,
    val documentByteCount: Long,
    val assetHashes: List<String>,
    val coverSurface: String?,
    val coverStamp: String?,
)

/**
 * The v2 `.zine` package: one user-created file containing the whole library and its deduplicated
 * import masters. This is additive beside [ZinePackageManifest], the readable v1 single-project
 * shape; changing that type's default version would create a v2-labelled v1 payload and is forbidden.
 */
@Serializable
public data class ZineLibraryBackupManifest(
    val packageVersion: Int,
    val kind: String,
    val appVersion: String,
    val createdAtEpochMs: Long,
    val projects: List<ZineBackupProjectEntry>,
    val assets: List<AssetEntry> = emptyList(),
)

/** One fully staged archive entry and its actual uncompressed byte count. */
public data class ZineArchiveEntry(
    val path: String,
    val uncompressedByteCount: Long,
)
