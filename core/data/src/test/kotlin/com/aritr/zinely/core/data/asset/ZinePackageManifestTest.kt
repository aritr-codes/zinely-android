package com.aritr.zinely.core.data.asset

import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineFormat
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The `.zine` backup/restore package manifest is designed now so the format is forward-compatible
 * (S2 spike §6, [ADR-009]). It must round-trip and carry both a package version and the embedded
 * document schema version so restore can gate on compatibility.
 */
class ZinePackageManifestTest {

    private val json = Json { encodeDefaults = true; prettyPrint = false }

    private fun sample() = ZinePackageManifest(
        appVersion = "0.2.0",
        documentSchemaVersion = 1,
        project = ZineProjectMetadata(
            id = "p1",
            title = "My Zine",
            format = ZineFormat.SINGLE_SHEET_8,
            paperSize = PaperSize.A4,
            createdAtEpochMs = 100L,
            updatedAtEpochMs = 200L,
        ),
        assets = listOf(
            AssetEntry(hash = "f".repeat(64), mimeType = "image/jpeg", widthPx = 4096, heightPx = 2731, byteCount = 1_234_567L),
        ),
    )

    @Test
    fun `manifest round-trips through json unchanged`() {
        val manifest = sample()
        val decoded = json.decodeFromString(ZinePackageManifest.serializer(), json.encodeToString(ZinePackageManifest.serializer(), manifest))
        assertEquals(manifest, decoded)
    }

    @Test
    fun `package version defaults to the current package version and is emitted`() {
        val manifest = sample()
        assertEquals(CURRENT_PACKAGE_VERSION, manifest.packageVersion)
        val encoded = json.encodeToString(ZinePackageManifest.serializer(), manifest)
        assertTrue(encoded.contains("\"packageVersion\":$CURRENT_PACKAGE_VERSION"), encoded)
        assertTrue(encoded.contains("\"documentSchemaVersion\":1"), encoded)
    }

    @Test
    fun `asset entry hash resolves to a content hash`() {
        val entry = sample().assets.first()
        assertEquals(entry.hash, entry.contentHash().hex)
    }
}
