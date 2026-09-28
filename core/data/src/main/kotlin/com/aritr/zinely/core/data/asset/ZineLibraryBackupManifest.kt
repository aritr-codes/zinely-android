package com.aritr.zinely.core.data.asset

import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineFormat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

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
/**
 * Maximum encoded ZIP size. The 64 MiB envelope is larger than the worst-case ZIP64 framing and
 * no-compression DEFLATE overhead for the bounded project/asset counts and canonical path lengths,
 * so every payload accepted at [MAX_BACKUP_TOTAL_BYTES] remains transportable and restorable.
 */
public const val MAX_BACKUP_ARCHIVE_BYTES: Long = MAX_BACKUP_TOTAL_BYTES + 64L * 1024L * 1024L
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
    /**
     * The zines this backup was saved without (ADR-122 §5). Partial ⇔ non-empty; the count is its size. Additive and
     * defaulted, so `packageVersion` stays 2: an archive without the key is complete, and older builds ignore it.
     * Display-only and untrusted, so it is read leniently: a malformed value never makes a valid archive unrestorable.
     */
    @Serializable(with = LenientOmissionsSerializer::class)
    val omitted: List<ZineBackupOmission> = emptyList(),
)

/** One zine left out of a backup: only what the maker was already told, never its id, path or bytes. */
@Serializable
public data class ZineBackupOmission(
    val title: String? = null,
    val reason: String,
) {
    public companion object {
        public const val UNREADABLE: String = "unreadable"
        public const val NEWER_VERSION: String = "newer_version"
        public const val PHOTO: String = "photo"
        private val KNOWN = setOf(UNREADABLE, NEWER_VERSION, PHOTO)

        /** An unknown reason reads as [UNREADABLE], so a newer build can add reasons without a version change. */
        public fun normalizedReason(reason: String?): String = reason?.takeIf { it in KNOWN } ?: UNREADABLE
    }
}

/**
 * Invariant: decoding `omitted` never throws. A non-array is no omissions; each array element counts as one left-out
 * zine, keeping a title only when it is a string and a reason only when it is a known one. Encoding is plain.
 */
internal object LenientOmissionsSerializer : KSerializer<List<ZineBackupOmission>> {
    private val delegate = ListSerializer(ZineBackupOmission.serializer())
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: List<ZineBackupOmission>): Unit =
        delegate.serialize(encoder, value)

    override fun deserialize(decoder: Decoder): List<ZineBackupOmission> {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement() ?: return delegate.deserialize(decoder)
        return (element as? JsonArray)?.map { item ->
            val fields = item as? JsonObject
            ZineBackupOmission(
                title = fields?.stringOrNull("title"),
                reason = ZineBackupOmission.normalizedReason(fields?.stringOrNull("reason")),
            )
        }.orEmpty()
    }

    private fun JsonObject.stringOrNull(key: String): String? =
        (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content
}

/** One fully staged archive entry and its actual uncompressed byte count. */
public data class ZineArchiveEntry(
    val path: String,
    val uncompressedByteCount: Long,
)
