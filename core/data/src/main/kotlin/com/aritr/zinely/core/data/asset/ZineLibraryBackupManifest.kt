package com.aritr.zinely.core.data.asset

import com.aritr.zinely.core.data.repository.LenientFolderNameSerializer
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
 *
 * [folder] is the Shelf folder the zine was in ([ADR-125](docs/DECISIONS.md#adr-125) rule 15). Additive and
 * defaulted, so `packageVersion` stays 2: an entry without the key is a zine on My Shelf, and builds that do
 * not know the key ignore it and restore the zine to My Shelf. It is arrangement, not work, so it is read
 * leniently: a value of the wrong type is no folder and never refuses a restore.
 *
 * ### The zine's paper is [sheet], else [paperSize] ([ADR-150](docs/DECISIONS.md#adr-150) decision 2)
 *
 * [paperSize] no longer means "this zine's size" on its own: it is **the size an older build should believe**,
 * always `letter` or `a4`. A US Legal zine writes `letter` and an A5 zine `a4`, plus [sheet] (`"legal"` / `"a5"`,
 * the document's serial name), which overrides it. Every shipped build decodes this manifest before any version
 * check, with unknown keys ignored, so it ignores [sheet], reaches the version check, sees schema 5 and says
 * "needs a newer Zinely" instead of "damaged". Read the size only through [effectivePaperSize], and only after
 * the version check; an unknown or inconsistent pair is `INVALID_STRUCTURE` (`project.sheet.invalid`).
 *
 * **Rule for every manifest field, now and later: no typed enum, ever.** The manifest is decoded before any
 * version check, so a typed enum makes the build that decodes it call a later build's backup damaged the day
 * a value is added. [sheet] is a `String` for that reason; [format] and [paperSize] are typed only because
 * shipped builds already read them, and their values are frozen.
 *
 * [paperColour] is the zine's paper colour id from `meta.json` (ADR-150 decision 3), or `null` for White. Additive
 * and read leniently like [folder]; the raw id is kept, so a later build's paper survives a restore here.
 */
@Serializable
public data class ZineBackupProjectEntry(
    val sourceProjectId: String,
    val title: String,
    val format: ZineFormat,
    /** Always LETTER or A4: the family size of the zine's paper. Use [effectivePaperSize]. */
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
    @Serializable(with = LenientFolderNameSerializer::class)
    val folder: String? = null,
    /** The paper's serial name when [paperSize] alone would be wrong (`"legal"`, `"a5"`), else `null`. A string, never an enum. */
    val sheet: String? = null,
    /** The same lenient string read as [folder]: a non-string is White and never refuses a restore. */
    @Serializable(with = LenientFolderNameSerializer::class)
    val paperColour: String? = null,
)

/**
 * The size an older build should believe for a zine on this paper: its family size, LETTER or A4 (ADR-150
 * decision 2). The `when` is exhaustive, so a later size must name its family.
 */
public fun PaperSize.backupPaperSize(): PaperSize = when (this) {
    PaperSize.LETTER, PaperSize.LEGAL -> PaperSize.LETTER
    PaperSize.A4, PaperSize.A5 -> PaperSize.A4
}

/** The manifest's [ZineBackupProjectEntry.sheet] for this paper: its serial name, or `null` for LETTER and A4. */
public fun PaperSize.backupSheet(): String? = if (backupPaperSize() == this) null else serialName()

/**
 * The zine's paper: the parsed [ZineBackupProjectEntry.sheet], else [ZineBackupProjectEntry.paperSize]. `null`
 * when the pair is not one a writer produces: an unknown sheet, a sheet of `letter` or `a4`, a sheet whose
 * family is not [ZineBackupProjectEntry.paperSize], or a bare LEGAL / A5 `paperSize`. The validator refuses
 * those as `project.sheet.invalid`, which the stager reports as `INVALID_STRUCTURE` after its version check.
 */
public fun ZineBackupProjectEntry.effectivePaperSize(): PaperSize? {
    val paper = sheet?.let { name -> PaperSize.entries.firstOrNull { it.serialName() == name } ?: return null }
        ?: return paperSize.takeIf { it.backupSheet() == null }
    return paper.takeIf { it.backupSheet() != null && it.backupPaperSize() == paperSize }
}

private fun PaperSize.serialName(): String = PaperSize.serializer().descriptor.getElementName(ordinal)

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
     * The zines this backup was saved without (ADR-122 §5). Partial ⇔ non-empty; the count is its size, at most
     * [MAX_BACKUP_PROJECTS] (the writer refuses more). Additive and
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
        // A hostile 4 MiB manifest can list ~1M tiny entries; keep no more than a real backup can hold (the writer
        // refuses more than MAX_BACKUP_PROJECTS, ADR-122 §5), so this never shortens a genuine list and the display
        // copies downstream stay bounded. The JSON tree itself is bounded by MAX_BACKUP_MANIFEST_BYTES.
        return (element as? JsonArray)?.asSequence()?.take(MAX_BACKUP_PROJECTS)?.map { item ->
            val fields = item as? JsonObject
            ZineBackupOmission(
                title = fields?.stringOrNull("title"),
                reason = ZineBackupOmission.normalizedReason(fields?.stringOrNull("reason")),
            )
        }?.toList().orEmpty()
    }

    private fun JsonObject.stringOrNull(key: String): String? =
        (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content
}

/** One fully staged archive entry and its actual uncompressed byte count. */
public data class ZineArchiveEntry(
    val path: String,
    val uncompressedByteCount: Long,
)
