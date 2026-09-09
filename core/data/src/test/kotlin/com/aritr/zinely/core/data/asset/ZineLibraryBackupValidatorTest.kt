package com.aritr.zinely.core.data.asset

import com.aritr.zinely.core.model.CURRENT_SCHEMA_VERSION
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineFormat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ZineLibraryBackupValidatorTest {
    private val validator = ZineLibraryBackupValidator()
    private val assetHash = "a".repeat(64)

    @Test
    fun `a complete canonical library backup is valid`() {
        assertTrue(validator.validate(manifest(), entries()).isValid)
    }

    @Test
    fun `v1 is refused by the v2 validator without changing the legacy contract`() {
        assertCode(manifest().copy(packageVersion = 1), entries(), "package.version.unsupported")
    }

    @Test
    fun `a newer package and a newer document are refused honestly`() {
        assertCode(
            manifest().copy(packageVersion = CURRENT_LIBRARY_BACKUP_VERSION + 1),
            entries(),
            "package.version.unsupported",
        )
        assertCode(
            manifest(project = project().copy(documentSchemaVersion = CURRENT_SCHEMA_VERSION + 1)),
            entries(),
            "package.documentSchema.unsupported",
        )
        assertCode(manifest().copy(kind = "single-project"), entries(), "package.kind.unsupported")
    }

    @Test
    fun `an empty backup and duplicate source ids are rejected`() {
        assertCode(manifest().copy(projects = emptyList()), listOf(manifestEntry()), "projects.empty")
        assertCode(
            manifest().copy(projects = listOf(project(), project().copy(documentPath = "projects/copy/document.json"))),
            entries() + ZineArchiveEntry("projects/copy/document.json", project().documentByteCount),
            "project.id.duplicate",
        )
    }

    @Test
    fun `cover metadata drift degrades without blocking healthy work`() {
        assertWarning(manifest(project = project().copy(coverStamp = null)), entries(), "project.cover.incomplete")
        assertWarning(manifest(project = project().copy(coverSurface = "Unknown")), entries(), "project.cover.unknown")
    }

    @Test
    fun `a blank title is preserved rather than making the whole library unbackuppable`() {
        assertTrue(validator.validate(manifest(project = project().copy(title = "")), entries()).isValid)
    }

    @Test
    fun `documents must use unique canonical safe paths`() {
        val unsafe = project().copy(documentPath = "../document.json")
        assertCode(manifest(project = unsafe), entries(project = unsafe), "project.documentPath.invalid")

        val second = project().copy(sourceProjectId = "project-2")
        assertCode(
            manifest().copy(projects = listOf(project(), second)),
            entries(),
            "project.documentPath.duplicate",
        )
    }

    @Test
    fun `asset references and the manifest asset table must match exactly`() {
        assertCode(manifest(project = project().copy(assetHashes = emptyList())), entries(), "asset.unreferenced")
        assertCode(manifest().copy(assets = emptyList()), entries().filterNot { it.path.startsWith("assets/") }, "asset.missing")
    }

    @Test
    fun `archive entries cannot traverse duplicate disappear or appear unexpectedly`() {
        listOf(
            "../escape",
            "/absolute",
            "C:/windows-drive",
            "projects\\backslash\\document.json",
            "projects//document.json",
            "projects/./document.json",
        ).forEach { unsafePath ->
            assertCode(manifest(), entries() + ZineArchiveEntry(unsafePath, 1L), "archive.path.unsafe")
        }
        assertCode(manifest(), entries() + manifestEntry(), "archive.entry.duplicate")
        assertCode(manifest(), entries().filterNot { it.path == project().documentPath }, "archive.entry.missing")
        assertCode(manifest(), entries() + ZineArchiveEntry("notes.txt", 3L), "archive.entry.unexpected")
    }

    @Test
    fun `declared and staged byte counts must agree`() {
        val wrongDocument = entries().map {
            if (it.path == project().documentPath) it.copy(uncompressedByteCount = it.uncompressedByteCount + 1L) else it
        }
        assertCode(manifest(), wrongDocument, "archive.entry.sizeMismatch")

        val wrongAsset = entries().map {
            if (it.path == "assets/$assetHash") it.copy(uncompressedByteCount = it.uncompressedByteCount + 1L) else it
        }
        assertCode(manifest(), wrongAsset, "archive.entry.sizeMismatch")
    }

    @Test
    fun `per-entry and total expansion limits reject hostile archives`() {
        val hugeDocument = project().copy(documentByteCount = MAX_BACKUP_DOCUMENT_BYTES + 1L)
        assertCode(
            manifest(project = hugeDocument),
            entries(project = hugeDocument),
            "project.documentByteCount.tooLarge",
        )

        val hugeAsset = asset().copy(byteCount = MAX_BACKUP_ASSET_BYTES + 1L)
        val hugeManifest = manifest().copy(assets = listOf(hugeAsset))
        val hugeEntries = entries().map {
            if (it.path.startsWith("assets/")) it.copy(uncompressedByteCount = hugeAsset.byteCount) else it
        }
        assertCode(hugeManifest, hugeEntries, "asset.byteCount.tooLarge")

        assertCode(
            manifest(),
            entries().map { if (it.path == MANIFEST_PATH) it.copy(uncompressedByteCount = MAX_BACKUP_MANIFEST_BYTES + 1L) else it },
            "archive.manifest.tooLarge",
        )

        val assetCount = (MAX_BACKUP_TOTAL_BYTES / MAX_BACKUP_ASSET_BYTES).toInt() + 1
        val largeAssets = (0 until assetCount).map { index ->
            asset().copy(hash = index.toString(16).padStart(64, '0'), byteCount = MAX_BACKUP_ASSET_BYTES)
        }
        val largeProject = project().copy(assetHashes = largeAssets.map(AssetEntry::hash))
        val largeArchive = listOf(
            manifestEntry(),
            ZineArchiveEntry(largeProject.documentPath, largeProject.documentByteCount),
        ) + largeAssets.map { ZineArchiveEntry("assets/${it.hash}", it.byteCount) }
        assertCode(
            manifest(project = largeProject).copy(assets = largeAssets),
            largeArchive,
            "archive.total.tooLarge",
        )
    }

    private fun assertCode(
        manifest: ZineLibraryBackupManifest,
        entries: List<ZineArchiveEntry>,
        expected: String,
    ) {
        val result = validator.validate(manifest, entries)
        assertFalse(result.isValid, "expected $expected but validation passed")
        assertTrue(result.issues.any { it.code == expected }, result.issues.toString())
    }

    private fun assertWarning(
        manifest: ZineLibraryBackupManifest,
        entries: List<ZineArchiveEntry>,
        expected: String,
    ) {
        val result = validator.validate(manifest, entries)
        assertTrue(result.isValid, result.issues.toString())
        assertTrue(result.warnings.any { it.code == expected }, result.issues.toString())
    }

    private fun manifest(project: ZineBackupProjectEntry = project()): ZineLibraryBackupManifest =
        ZineLibraryBackupManifest(
            packageVersion = CURRENT_LIBRARY_BACKUP_VERSION,
            kind = LIBRARY_BACKUP_KIND,
            appVersion = "0.9.0",
            createdAtEpochMs = 1_000L,
            projects = listOf(project),
            assets = listOf(asset()),
        )

    private fun project(): ZineBackupProjectEntry = ZineBackupProjectEntry(
        sourceProjectId = "project-1",
        title = "Pocket poems",
        format = ZineFormat.SINGLE_SHEET_8,
        paperSize = PaperSize.A4,
        createdAtEpochMs = 100L,
        updatedAtEpochMs = 900L,
        documentSchemaVersion = CURRENT_SCHEMA_VERSION,
        documentPath = "projects/project-1/document.json",
        documentSha256 = "d".repeat(64),
        documentByteCount = 4_096L,
        assetHashes = listOf(assetHash),
        coverSurface = "MatchaInk",
        coverStamp = "Star",
    )

    private fun asset(): AssetEntry = AssetEntry(
        hash = assetHash,
        mimeType = "image/jpeg",
        widthPx = 2_048,
        heightPx = 1_365,
        byteCount = 12_345L,
    )

    private fun entries(project: ZineBackupProjectEntry = project()): List<ZineArchiveEntry> = listOf(
        manifestEntry(),
        ZineArchiveEntry(project.documentPath, project.documentByteCount),
        ZineArchiveEntry("assets/$assetHash", asset().byteCount),
    )

    private fun manifestEntry(): ZineArchiveEntry = ZineArchiveEntry(MANIFEST_PATH, 1_024L)
}
