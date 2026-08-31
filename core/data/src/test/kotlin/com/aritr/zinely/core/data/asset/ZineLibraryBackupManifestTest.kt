package com.aritr.zinely.core.data.asset

import com.aritr.zinely.core.model.CURRENT_SCHEMA_VERSION
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineFormat
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ZineLibraryBackupManifestTest {
    private val json = Json

    @Test
    fun `the v2 library manifest round-trips without losing project identity`() {
        val manifest = sampleLibraryBackupManifest()

        val encoded = json.encodeToString(ZineLibraryBackupManifest.serializer(), manifest)
        val decoded = json.decodeFromString(ZineLibraryBackupManifest.serializer(), encoded)

        assertEquals(manifest, decoded)
        assertTrue(encoded.contains("\"packageVersion\":$CURRENT_LIBRARY_BACKUP_VERSION"), encoded)
        assertTrue(encoded.contains("\"kind\":\"$LIBRARY_BACKUP_KIND\""), encoded)
        assertTrue(encoded.contains("\"coverSurface\":\"MatchaInk\""), encoded)
        assertTrue(encoded.contains("\"coverStamp\":\"Star\""), encoded)
    }

    private fun sampleLibraryBackupManifest(): ZineLibraryBackupManifest {
        val assetHash = "a".repeat(64)
        return ZineLibraryBackupManifest(
            packageVersion = CURRENT_LIBRARY_BACKUP_VERSION,
            kind = LIBRARY_BACKUP_KIND,
            appVersion = "0.9.0",
            createdAtEpochMs = 1_000L,
            projects = listOf(
                ZineBackupProjectEntry(
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
                ),
                ZineBackupProjectEntry(
                    sourceProjectId = "project-2",
                    title = "Shared light",
                    format = ZineFormat.SINGLE_SHEET_8,
                    paperSize = PaperSize.LETTER,
                    createdAtEpochMs = 200L,
                    updatedAtEpochMs = 950L,
                    documentSchemaVersion = CURRENT_SCHEMA_VERSION,
                    documentPath = "projects/project-2/document.json",
                    documentSha256 = "e".repeat(64),
                    documentByteCount = 2_048L,
                    assetHashes = listOf(assetHash),
                    coverSurface = null,
                    coverStamp = null,
                ),
            ),
            assets = listOf(
                AssetEntry(
                    hash = assetHash,
                    mimeType = "image/jpeg",
                    widthPx = 2_048,
                    heightPx = 1_365,
                    byteCount = 12_345L,
                ),
            ),
        )
    }
}
