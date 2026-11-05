package com.aritr.zinely.core.data.storage

import com.aritr.zinely.core.data.asset.ZineLibraryBackupManifest
import com.aritr.zinely.core.data.asset.effectivePaperSize
import com.aritr.zinely.core.model.PaperSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.security.MessageDigest

/**
 * ADR-150 decision 2's older-build check: what a shipped build (`v0.9.0-beta.4-r3` to `v0.9.0-beta.9`) does with the
 * manifest a v5 build writes for a US Legal zine.
 *
 * `fixtures/manifest-v5-legal.json` is frozen: the bytes this build's backup writer `Json` (`encodeDefaults = true`)
 * writes for one Legal zine (schema 5, `paperSize` `letter`, `sheet` `legal`, a paper colour) and one plain A4 zine.
 * The older build is modelled by [FrozenEntry], a copy of the entry type those builds shipped with its **own frozen
 * two-value enum** ([FrozenPaper]; never the live [PaperSize], which now has four values and would hide the hazard),
 * decoded under `ignoreUnknownKeys = true` as their stager does, then their version check (`packageVersion > 2` or
 * any `documentSchemaVersion > 4` is FUTURE_VERSION).
 */
class OlderBuildManifestTest {

    private val olderBuildJson = Json { ignoreUnknownKeys = true }

    @Test
    fun `the frozen manifest bytes are what this build writes`() {
        val text = fixture()
        assertEquals(FIXTURE_SHA256, sha256(text.encodeToByteArray()), "manifest-v5-legal.json is frozen")
        val writer = Json { encodeDefaults = true }
        val reader = Json { ignoreUnknownKeys = true }
        val decoded = reader.decodeFromString(ZineLibraryBackupManifest.serializer(), text)
        assertEquals(text, writer.encodeToString(ZineLibraryBackupManifest.serializer(), decoded))
        assertEquals(listOf(PaperSize.LEGAL, PaperSize.A4), decoded.projects.map { it.effectivePaperSize() })
    }

    @Test
    fun `an older build decodes the v5 manifest and says FUTURE_VERSION, never INVALID_MANIFEST`() {
        assertEquals("FUTURE_VERSION", olderBuildVerdict(fixture()))
    }

    @Test
    fun `the check has teeth - legal written straight into paperSize is INVALID_MANIFEST on an older build`() {
        val straight = fixture().replace("\"paperSize\":\"letter\"", "\"paperSize\":\"legal\"")
        assertEquals("INVALID_MANIFEST", olderBuildVerdict(straight))
    }

    /** The older build's stager up to and including its version check. */
    private fun olderBuildVerdict(text: String): String {
        val manifest = try {
            olderBuildJson.decodeFromString(FrozenManifest.serializer(), text)
        } catch (_: SerializationException) {
            return "INVALID_MANIFEST"
        } catch (_: IllegalArgumentException) {
            return "INVALID_MANIFEST"
        }
        if (manifest.packageVersion > 2) return "FUTURE_VERSION"
        if (manifest.projects.maxOf { it.documentSchemaVersion } > OLDER_BUILD_SCHEMA) return "FUTURE_VERSION"
        return "PASSES_VERSION_CHECK"
    }

    private fun fixture(): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/manifest-v5-legal.json")).use { it.readBytes() }.decodeToString()

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    @Serializable
    private enum class FrozenPaper {
        @SerialName("letter") LETTER,
        @SerialName("a4") A4,
    }

    @Serializable
    private enum class FrozenFormat {
        @SerialName("single_sheet_8") SINGLE_SHEET_8,
    }

    /** `ZineBackupProjectEntry` as `v0.9.0-beta.9` shipped it. */
    @Serializable
    private data class FrozenEntry(
        val sourceProjectId: String,
        val title: String,
        val format: FrozenFormat,
        val paperSize: FrozenPaper,
        val createdAtEpochMs: Long,
        val updatedAtEpochMs: Long,
        val documentSchemaVersion: Int,
        val documentPath: String,
        val documentSha256: String,
        val documentByteCount: Long,
        val assetHashes: List<String>,
        val coverSurface: String?,
        val coverStamp: String?,
        val folder: String? = null,
    )

    @Serializable
    private data class FrozenManifest(
        val packageVersion: Int,
        val kind: String,
        val appVersion: String,
        val createdAtEpochMs: Long,
        val projects: List<FrozenEntry>,
        val assets: List<JsonObject> = emptyList(),
        val omitted: JsonElement? = null,
    )

    private companion object {
        /** CURRENT_SCHEMA_VERSION in `v0.9.0-beta.9`, the newest shipped build. */
        const val OLDER_BUILD_SCHEMA = 4
        const val FIXTURE_SHA256 = "69fb7b4a197cf5705aee4477668fa217b11fb179c9696428d9b895a21914d2e5"
    }
}
