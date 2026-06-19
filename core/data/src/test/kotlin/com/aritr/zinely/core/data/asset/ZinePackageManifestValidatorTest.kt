package com.aritr.zinely.core.data.asset

import com.aritr.zinely.core.model.CURRENT_SCHEMA_VERSION
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineFormat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * `.zine` restore must never partially import a tampered package (S2 spike §7, risk R7). This
 * validator covers the **structural** manifest rules that need no bytes; the byte-level
 * "blob matches hash and byteCount" check is done at restore time in S2B against the actual zip.
 */
class ZinePackageManifestValidatorTest {

    private val validator = ZinePackageManifestValidator()

    private fun entry(hash: String = "f".repeat(64)) =
        AssetEntry(hash = hash, mimeType = "image/jpeg", widthPx = 4096, heightPx = 2731, byteCount = 1000L)

    private fun manifest(
        packageVersion: Int = CURRENT_PACKAGE_VERSION,
        documentSchemaVersion: Int = CURRENT_SCHEMA_VERSION,
        projectId: String = "p1",
        assets: List<AssetEntry> = listOf(entry()),
    ) = ZinePackageManifest(
        packageVersion = packageVersion,
        appVersion = "0.2.0",
        documentSchemaVersion = documentSchemaVersion,
        project = ZineProjectMetadata("", "t", ZineFormat.SINGLE_SHEET_8, PaperSize.A4, 0L, 0L).copy(id = projectId),
        assets = assets,
    )

    private fun codes(m: ZinePackageManifest) = validator.validate(m).issues.map { it.code }

    @Test
    fun `a well-formed manifest is valid`() {
        val result = validator.validate(manifest())
        assertTrue(result.isValid, result.issues.toString())
    }

    @Test
    fun `a newer package version is refused`() {
        val m = manifest(packageVersion = CURRENT_PACKAGE_VERSION + 1)
        assertTrue(codes(m).contains("package.version.unsupported"), codes(m).toString())
        assertFalse(validator.validate(m).isValid)
    }

    @Test
    fun `a newer document schema version is refused`() {
        assertTrue(codes(manifest(documentSchemaVersion = CURRENT_SCHEMA_VERSION + 1)).contains("package.documentSchema.unsupported"))
    }

    @Test
    fun `a blank project id is an error`() {
        assertTrue(codes(manifest(projectId = " ")).contains("project.id.blank"))
    }

    @Test
    fun `an invalid asset hash is an error`() {
        assertTrue(codes(manifest(assets = listOf(entry(hash = "nope")))).contains("asset.hash.invalid"))
    }

    @Test
    fun `non-positive asset dimensions are an error`() {
        assertTrue(codes(manifest(assets = listOf(entry().copy(widthPx = 0)))).contains("asset.dimensions.invalid"))
    }

    @Test
    fun `a non-positive byte count is an error`() {
        assertTrue(codes(manifest(assets = listOf(entry().copy(byteCount = 0L)))).contains("asset.byteCount.invalid"))
    }

    @Test
    fun `a blank mime type is an error`() {
        assertTrue(codes(manifest(assets = listOf(entry().copy(mimeType = "")))).contains("asset.mime.blank"))
    }

    @Test
    fun `duplicate asset hashes are an error`() {
        val dup = "a".repeat(64)
        assertTrue(codes(manifest(assets = listOf(entry(dup), entry(dup)))).contains("asset.hash.duplicate"))
    }
}
