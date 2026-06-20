package com.aritr.zinely.core.data.asset

import com.aritr.zinely.core.data.validation.Severity
import com.aritr.zinely.core.data.validation.ValidationIssue
import com.aritr.zinely.core.data.validation.ValidationResult
import com.aritr.zinely.core.model.CURRENT_SCHEMA_VERSION

/**
 * Validates a `.zine` [ZinePackageManifest] before restore so a tampered/truncated package is
 * rejected with a clear error rather than partially imported (S2 spike §7, risk R7). Pure and
 * structural: it checks versions, ids, and the asset table. The **byte-level** integrity check
 * (each blob's bytes hash to its [AssetEntry.hash] and match [AssetEntry.byteCount]) needs the
 * actual zip and is performed at restore time in S2B, using [ContentHasher].
 */
public class ZinePackageManifestValidator {

    public fun validate(manifest: ZinePackageManifest): ValidationResult {
        val issues = mutableListOf<ValidationIssue>()

        if (manifest.packageVersion < 1 || manifest.packageVersion > CURRENT_PACKAGE_VERSION) {
            issues += error(
                "package.version.unsupported",
                "package version ${manifest.packageVersion} is outside 1..$CURRENT_PACKAGE_VERSION",
                "packageVersion",
            )
        }
        if (manifest.documentSchemaVersion < 1 || manifest.documentSchemaVersion > CURRENT_SCHEMA_VERSION) {
            issues += error(
                "package.documentSchema.unsupported",
                "document schema version ${manifest.documentSchemaVersion} is outside 1..$CURRENT_SCHEMA_VERSION",
                "documentSchemaVersion",
            )
        }
        if (manifest.project.id.isBlank()) {
            issues += error("project.id.blank", "project id is blank", "project.id")
        }

        val seenHashes = HashSet<String>()
        manifest.assets.forEachIndexed { i, asset ->
            val path = "assets[$i]"
            if (!ContentHash.isValid(asset.hash)) {
                issues += error("asset.hash.invalid", "asset hash '${asset.hash}' is not a sha256 hash", "$path.hash")
            } else if (!seenHashes.add(asset.hash)) {
                issues += error("asset.hash.duplicate", "duplicate asset hash '${asset.hash}'", "$path.hash")
            }
            if (asset.widthPx <= 0 || asset.heightPx <= 0) {
                issues += error("asset.dimensions.invalid", "asset pixel dimensions must be positive", "$path")
            }
            if (asset.byteCount <= 0L) {
                issues += error("asset.byteCount.invalid", "asset byte count must be positive", "$path.byteCount")
            }
            if (asset.mimeType.isBlank()) {
                issues += error("asset.mime.blank", "asset mime type is blank", "$path.mimeType")
            }
        }

        return ValidationResult(issues)
    }

    private fun error(code: String, message: String, path: String): ValidationIssue =
        ValidationIssue(code, message, Severity.ERROR, path)
}
