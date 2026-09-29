package com.aritr.zinely.core.data.asset

import com.aritr.zinely.core.model.CURRENT_SCHEMA_VERSION
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineFormat
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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

    @Test
    fun `a partial manifest round-trips its omissions`() {
        val manifest = sampleLibraryBackupManifest().copy(
            omitted = listOf(
                ZineBackupOmission("Moth Club Bulletin", ZineBackupOmission.UNREADABLE),
                ZineBackupOmission(null, ZineBackupOmission.PHOTO),
            ),
        )

        val decoded = lenient.decodeFromString(
            ZineLibraryBackupManifest.serializer(),
            lenient.encodeToString(ZineLibraryBackupManifest.serializer(), manifest),
        )

        assertEquals(manifest, decoded)
    }

    @Test
    fun `a manifest without the omitted key decodes as a complete backup`() {
        val withoutKey = json.encodeToString(ZineLibraryBackupManifest.serializer(), sampleLibraryBackupManifest())
            .also { assertFalse(it.contains("omitted"), it) } // default Json drops defaults: the pre-1b shape

        assertEquals(emptyList<ZineBackupOmission>(), lenient.decodeFromString(ZineLibraryBackupManifest.serializer(), withoutKey).omitted)
    }

    @Test
    fun `a complete backup written with encodeDefaults carries an empty omitted list`() {
        val encoded = writer.encodeToString(ZineLibraryBackupManifest.serializer(), sampleLibraryBackupManifest())

        assertTrue(encoded.contains("\"omitted\":[]"), encoded)
    }

    @Test
    fun `a malformed omitted value never refuses the manifest`() {
        val cases = mapOf<String, List<ZineBackupOmission>>(
            "5" to emptyList(),
            "null" to emptyList(),
            "\"x\"" to emptyList(),
            "{}" to emptyList(),
            "[5]" to listOf(ZineBackupOmission(null, ZineBackupOmission.UNREADABLE)),
            "[{\"title\":7,\"reason\":true}]" to listOf(ZineBackupOmission(null, ZineBackupOmission.UNREADABLE)),
            "[{\"title\":\"A\",\"reason\":\"from_the_future\"}]" to listOf(ZineBackupOmission("A", ZineBackupOmission.UNREADABLE)),
            "[{\"title\":\"B\",\"reason\":\"newer_version\"}]" to listOf(ZineBackupOmission("B", ZineBackupOmission.NEWER_VERSION)),
        )
        val base = json.encodeToString(ZineLibraryBackupManifest.serializer(), sampleLibraryBackupManifest())

        cases.forEach { (value, expected) ->
            val text = base.dropLast(1) + ",\"omitted\":$value}"
            assertEquals(expected, lenient.decodeFromString(ZineLibraryBackupManifest.serializer(), text).omitted, value)
        }
    }

    @Test
    fun `a hostile omitted list is cut to what a real backup could hold`() {
        val base = json.encodeToString(ZineLibraryBackupManifest.serializer(), sampleLibraryBackupManifest())
        val text = base.dropLast(1) + ",\"omitted\":[" + List(MAX_BACKUP_PROJECTS + 5) { "1" }.joinToString(",") + "]}"

        assertEquals(MAX_BACKUP_PROJECTS, lenient.decodeFromString(ZineLibraryBackupManifest.serializer(), text).omitted.size)
    }

    @Test
    fun `the decode cap keeps every entry of the largest list the writer allows`() {
        // ADR-122 §5: the writer refuses more than MAX_BACKUP_PROJECTS, so exactly that many must all come back.
        val omitted = List(MAX_BACKUP_PROJECTS) { ZineBackupOmission("Zine $it", ZineBackupOmission.PHOTO) }
        val encoded = writer.encodeToString(
            ZineLibraryBackupManifest.serializer(),
            sampleLibraryBackupManifest().copy(omitted = omitted),
        )

        assertEquals(omitted, lenient.decodeFromString(ZineLibraryBackupManifest.serializer(), encoded).omitted)
    }

    /** The restore reader's configuration (`ZineLibraryBackupStager`): unknown keys are ignored. */
    private val lenient = Json { ignoreUnknownKeys = true }

    /** The backup writer's configuration: defaults are written. */
    private val writer = Json { encodeDefaults = true }

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
