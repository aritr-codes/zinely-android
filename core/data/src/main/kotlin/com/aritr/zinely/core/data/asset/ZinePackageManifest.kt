package com.aritr.zinely.core.data.asset

import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineFormat
import kotlinx.serialization.Serializable

/**
 * The `.zine` package version this build writes. The package is a self-contained zip
 * (`manifest.json` + `document.json` + `assets/` + `thumbnail.png`) designed now for forward
 * compatibility; SAF wiring lands in V1 (S2 spike §6, [ADR-009]).
 */
public const val CURRENT_PACKAGE_VERSION: Int = 1

/**
 * One content-addressed asset recorded in a `.zine` manifest. [hash] is the sha256 hex that resolves
 * to `assets/<hash>`; the pixel dimensions and byte count let restore validate integrity before
 * importing.
 */
@Serializable
public data class AssetEntry(
    val hash: String,
    val mimeType: String,
    val widthPx: Int,
    val heightPx: Int,
    val byteCount: Long,
) {
    /** The validated [ContentHash] for this entry (throws if the stored hash is malformed). */
    public fun contentHash(): ContentHash = ContentHash.of(hash)
}

/** The project metadata embedded in a `.zine` manifest (a snapshot, not the live Room cache). */
@Serializable
public data class ZineProjectMetadata(
    val id: String,
    val title: String,
    val format: ZineFormat,
    val paperSize: PaperSize,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

/**
 * The manifest at the root of a `.zine` backup package. Carries the **package** version and the
 * embedded **document** schema version (two independent axes, S2 spike §6) so restore can gate on
 * compatibility, plus the project metadata and the asset table to verify on import.
 */
@Serializable
public data class ZinePackageManifest(
    val packageVersion: Int = CURRENT_PACKAGE_VERSION,
    val appVersion: String,
    val documentSchemaVersion: Int,
    val project: ZineProjectMetadata,
    val assets: List<AssetEntry> = emptyList(),
)
