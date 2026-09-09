package com.aritr.zinely.core.data.asset

import com.aritr.zinely.core.data.validation.Severity
import com.aritr.zinely.core.data.validation.ValidationIssue
import com.aritr.zinely.core.data.validation.ValidationResult
import com.aritr.zinely.core.model.CURRENT_SCHEMA_VERSION
import com.aritr.zinely.core.model.ZineCoverStamp
import com.aritr.zinely.core.model.ZineCoverSurface

/**
 * Pure fail-closed validation for a fully staged v2 library backup.
 *
 * The archive reader is responsible for counting bytes while extracting; this validator compares
 * that observed index with the manifest before any live project or asset path is touched. Hashing
 * the staged document/asset bytes and typed document decoding are the next restore layer: this class
 * deliberately validates only the package structure it can decide without I/O.
 */
public class ZineLibraryBackupValidator {
    public fun validate(
        manifest: ZineLibraryBackupManifest,
        entries: List<ZineArchiveEntry>,
    ): ValidationResult {
        val issues = mutableListOf<ValidationIssue>()

        validateHeader(manifest, issues)
        validateProjects(manifest, issues)
        validateAssets(manifest, issues)
        validateAssetClosure(manifest, issues)
        validateArchiveIndex(manifest, entries, issues)

        return ValidationResult(issues)
    }

    private fun validateHeader(
        manifest: ZineLibraryBackupManifest,
        issues: MutableList<ValidationIssue>,
    ) {
        if (manifest.packageVersion != CURRENT_LIBRARY_BACKUP_VERSION) {
            issues += error(
                "package.version.unsupported",
                "library package version ${manifest.packageVersion} is not $CURRENT_LIBRARY_BACKUP_VERSION",
                "packageVersion",
            )
        }
        if (manifest.kind != LIBRARY_BACKUP_KIND) {
            issues += error("package.kind.unsupported", "package kind '${manifest.kind}' is not a library backup", "kind")
        }
        if (manifest.appVersion.isBlank()) {
            issues += error("package.appVersion.blank", "app version is blank", "appVersion")
        }
        if (manifest.createdAtEpochMs < 0L) {
            issues += error("package.createdAt.invalid", "backup creation time is negative", "createdAtEpochMs")
        }
        if (manifest.projects.isEmpty()) {
            issues += error("projects.empty", "a library backup contains no projects", "projects")
        } else if (manifest.projects.size > MAX_BACKUP_PROJECTS) {
            issues += error("projects.tooMany", "library backup contains too many projects", "projects")
        }
        if (manifest.assets.size > MAX_BACKUP_ASSETS) {
            issues += error("assets.tooMany", "library backup contains too many assets", "assets")
        }
    }

    private fun validateProjects(
        manifest: ZineLibraryBackupManifest,
        issues: MutableList<ValidationIssue>,
    ) {
        val ids = HashSet<String>()
        val documentPaths = HashSet<String>()
        manifest.projects.forEachIndexed { index, project ->
            val path = "projects[$index]"
            if (project.sourceProjectId.isBlank()) {
                issues += error("project.id.blank", "source project id is blank", "$path.sourceProjectId")
            } else if (!ids.add(project.sourceProjectId)) {
                issues += error("project.id.duplicate", "duplicate source project id '${project.sourceProjectId}'", "$path.sourceProjectId")
            }
            if (project.createdAtEpochMs < 0L || project.updatedAtEpochMs < 0L) {
                issues += error("project.time.invalid", "project timestamps must not be negative", path)
            }
            if (project.documentSchemaVersion !in 1..CURRENT_SCHEMA_VERSION) {
                issues += error(
                    "package.documentSchema.unsupported",
                    "document schema ${project.documentSchemaVersion} is outside 1..$CURRENT_SCHEMA_VERSION",
                    "$path.documentSchemaVersion",
                )
            }
            if (!DOCUMENT_PATH.matches(project.documentPath)) {
                issues += error("project.documentPath.invalid", "document path is not canonical", "$path.documentPath")
            } else if (!documentPaths.add(project.documentPath)) {
                issues += error("project.documentPath.duplicate", "duplicate document path '${project.documentPath}'", "$path.documentPath")
            }
            if (!ContentHash.isValid(project.documentSha256)) {
                issues += error("project.documentHash.invalid", "document hash is not sha256", "$path.documentSha256")
            }
            if (project.documentByteCount <= 0L) {
                issues += error("project.documentByteCount.invalid", "document byte count must be positive", "$path.documentByteCount")
            } else if (project.documentByteCount > MAX_BACKUP_DOCUMENT_BYTES) {
                issues += error("project.documentByteCount.tooLarge", "document exceeds the restore limit", "$path.documentByteCount")
            }
            validateCover(project, path, issues)

            val projectAssets = HashSet<String>()
            project.assetHashes.forEachIndexed { assetIndex, hash ->
                if (!ContentHash.isValid(hash)) {
                    issues += error("project.assetHash.invalid", "asset reference is not sha256", "$path.assetHashes[$assetIndex]")
                } else if (!projectAssets.add(hash)) {
                    issues += error("project.assetHash.duplicate", "duplicate project asset reference '$hash'", "$path.assetHashes[$assetIndex]")
                }
            }
        }
    }

    private fun validateCover(
        project: ZineBackupProjectEntry,
        path: String,
        issues: MutableList<ValidationIssue>,
    ) {
        if ((project.coverSurface == null) != (project.coverStamp == null)) {
            issues += warning(
                "project.cover.incomplete",
                "incomplete cover metadata will be shown without a cover",
                "$path.cover",
            )
            return
        }
        project.coverSurface?.let { surface ->
            if (ZineCoverSurface.entries.none { it.name == surface }) {
                issues += warning(
                    "project.cover.unknown",
                    "unknown cover surface '$surface' will be shown without a cover",
                    "$path.coverSurface",
                )
            }
        }
        project.coverStamp?.let { stamp ->
            if (ZineCoverStamp.entries.none { it.name == stamp }) {
                issues += warning(
                    "project.cover.unknown",
                    "unknown cover stamp '$stamp' will be shown without a cover",
                    "$path.coverStamp",
                )
            }
        }
    }

    private fun validateAssets(
        manifest: ZineLibraryBackupManifest,
        issues: MutableList<ValidationIssue>,
    ) {
        val hashes = HashSet<String>()
        manifest.assets.forEachIndexed { index, asset ->
            val path = "assets[$index]"
            if (!ContentHash.isValid(asset.hash)) {
                issues += error("asset.hash.invalid", "asset hash is not sha256", "$path.hash")
            } else if (!hashes.add(asset.hash)) {
                issues += error("asset.hash.duplicate", "duplicate asset hash '${asset.hash}'", "$path.hash")
            }
            if (asset.mimeType !in SUPPORTED_ASSET_MIME_TYPES) {
                issues += error("asset.mime.unsupported", "unsupported asset mime type '${asset.mimeType}'", "$path.mimeType")
            }
            if (asset.widthPx !in 1..MAX_ASSET_DIMENSION_PX || asset.heightPx !in 1..MAX_ASSET_DIMENSION_PX) {
                issues += error("asset.dimensions.invalid", "asset dimensions are outside the import-master limit", path)
            }
            if (asset.byteCount <= 0L) {
                issues += error("asset.byteCount.invalid", "asset byte count must be positive", "$path.byteCount")
            } else if (asset.byteCount > MAX_BACKUP_ASSET_BYTES) {
                issues += error("asset.byteCount.tooLarge", "asset exceeds the restore limit", "$path.byteCount")
            }
        }
    }

    private fun validateAssetClosure(
        manifest: ZineLibraryBackupManifest,
        issues: MutableList<ValidationIssue>,
    ) {
        val referenced = manifest.projects.flatMapTo(linkedSetOf()) { it.assetHashes }
        val listed = manifest.assets.mapTo(linkedSetOf()) { it.hash }
        (referenced - listed).forEach { hash ->
            issues += error("asset.missing", "referenced asset '$hash' is absent from the manifest", "assets")
        }
        (listed - referenced).forEach { hash ->
            issues += error("asset.unreferenced", "manifest asset '$hash' is not referenced by any project", "assets")
        }
    }

    private fun validateArchiveIndex(
        manifest: ZineLibraryBackupManifest,
        entries: List<ZineArchiveEntry>,
        issues: MutableList<ValidationIssue>,
    ) {
        val observed = LinkedHashMap<String, ZineArchiveEntry>()
        entries.forEachIndexed { index, entry ->
            if (!isSafeArchivePath(entry.path)) {
                issues += error("archive.path.unsafe", "archive entry path is unsafe", "entries[$index].path")
            }
            if (entry.uncompressedByteCount <= 0L) {
                issues += error("archive.entry.sizeInvalid", "archive entry size must be positive", "entries[$index].uncompressedByteCount")
            }
            if (observed.putIfAbsent(entry.path, entry) != null) {
                issues += error("archive.entry.duplicate", "duplicate archive entry '${entry.path}'", "entries[$index].path")
            }
        }

        val expectedSizes = linkedMapOf<String, Long?>(MANIFEST_PATH to null)
        manifest.projects.forEach { expectedSizes[it.documentPath] = it.documentByteCount }
        manifest.assets.forEach { expectedSizes["assets/${it.hash}"] = it.byteCount }

        (expectedSizes.keys - observed.keys).forEach { path ->
            issues += error("archive.entry.missing", "required archive entry '$path' is missing", "entries")
        }
        (observed.keys - expectedSizes.keys).forEach { path ->
            issues += error("archive.entry.unexpected", "unexpected archive entry '$path'", "entries")
        }
        expectedSizes.forEach { (path, expected) ->
            val actual = observed[path]?.uncompressedByteCount ?: return@forEach
            if (path == MANIFEST_PATH && actual > MAX_BACKUP_MANIFEST_BYTES) {
                issues += error("archive.manifest.tooLarge", "manifest exceeds the restore limit", "entries")
            } else if (expected != null && actual != expected) {
                issues += error("archive.entry.sizeMismatch", "entry '$path' has $actual bytes, expected $expected", "entries")
            }
        }

        var total = 0L
        for (entry in entries) {
            total = try {
                Math.addExact(total, entry.uncompressedByteCount.coerceAtLeast(0L))
            } catch (_: ArithmeticException) {
                Long.MAX_VALUE
            }
        }
        if (total > MAX_BACKUP_TOTAL_BYTES) {
            issues += error("archive.total.tooLarge", "archive expands beyond the restore limit", "entries")
        }
    }

    private fun isSafeArchivePath(path: String): Boolean {
        if (path.isBlank() || path.startsWith('/') || '\\' in path || WINDOWS_DRIVE_PREFIX.matches(path)) return false
        val segments = path.split('/')
        return segments.none { it.isBlank() || it == "." || it == ".." }
    }

    private fun error(code: String, message: String, path: String): ValidationIssue =
        ValidationIssue(code, message, Severity.ERROR, path)

    private fun warning(code: String, message: String, path: String): ValidationIssue =
        ValidationIssue(code, message, Severity.WARNING, path)

    private companion object {
        val DOCUMENT_PATH: Regex = Regex("^projects/[A-Za-z0-9_-]{1,64}/document\\.json$")
        val SUPPORTED_ASSET_MIME_TYPES: Set<String> = setOf("image/jpeg")
        const val MAX_ASSET_DIMENSION_PX: Int = 4_096
        val WINDOWS_DRIVE_PREFIX: Regex = Regex("^[A-Za-z]:.*")
    }
}
