package com.aritr.zinely.core.data.storage

import com.aritr.zinely.core.data.asset.AssetEntry
import com.aritr.zinely.core.data.asset.CURRENT_LIBRARY_BACKUP_VERSION
import com.aritr.zinely.core.data.asset.LIBRARY_BACKUP_KIND
import com.aritr.zinely.core.data.asset.MAX_BACKUP_DOCUMENT_BYTES
import com.aritr.zinely.core.data.asset.ZineBackupProjectEntry
import com.aritr.zinely.core.data.asset.ZineLibraryBackupManifest
import com.aritr.zinely.core.data.serialization.JsonDocumentSerializer
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.CURRENT_SCHEMA_VERSION
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Random
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ZineLibraryBackupStagerTest {
    @TempDir
    lateinit var temp: Path

    @Test
    fun `stages one valid project without assets and close removes staging`() = runBlocking {
        val fixture = fixture(mapOf("one" to document()))
        val archive = writeArchive(fixture)

        val staged = ZineLibraryBackupStager().stage(archive, temp.resolve("stage"))

        assertEquals(listOf("one"), staged.projects.map { it.manifestEntry.sourceProjectId })
        assertTrue(staged.assets.isEmpty())
        assertTrue(Files.exists(staged.root))
        staged.close()
        assertFalse(Files.exists(staged.root))
    }

    @Test
    fun `stages multiple projects and one physical shared asset`() = runBlocking {
        val asset = ByteArray(4096).also { Random(7).nextBytes(it) }
        val hash = sha256(asset)
        val fixture = fixture(
            documents = mapOf("one" to document(hash), "two" to document(hash)),
            assets = mapOf(hash to asset),
        )

        ZineLibraryBackupStager().stage(writeArchive(fixture), temp.resolve("stage")).use { staged ->
            assertEquals(2, staged.projects.size)
            assertEquals(setOf(hash), staged.assets.keys)
            assertArrayEquals(asset, Files.readAllBytes(staged.assets.getValue(hash)))
            assertEquals(listOf(hash, hash), staged.projects.map { project ->
                (project.document.pages.first().elements.single() as ImageElement).assetId
            })
        }
    }

    @Test
    fun `rejects malformed and truncated ZIPs without leaving staging`() {
        val malformed = temp.resolve("broken.zine")
        Files.write(malformed, "not a zip".encodeToByteArray())
        val valid = writeArchive(fixture(mapOf("one" to document())))
        val truncated = temp.resolve("truncated.zine")
        Files.write(truncated, Files.readAllBytes(valid).copyOf(Files.size(valid).toInt() - 32))

        listOf(malformed, truncated).forEach { archive ->
            val failure = assertThrows(ZineBackupStagingException::class.java) {
                runBlocking { ZineLibraryBackupStager().stage(archive, temp.resolve("stage")) }
            }

            assertEquals(ZineBackupStagingException.Reason.MALFORMED_ARCHIVE, failure.reason)
            assertNoStagingChildren()
        }
    }

    @Test
    fun `rejects malformed manifest JSON and invalid manifest UTF-8`() {
        val fixture = fixture(mapOf("one" to document()))
        listOf(
            "not-json".encodeToByteArray(),
            byteArrayOf(0xC3.toByte(), 0x28),
        ).forEach { invalidManifest ->
            val archive = writeArchive(fixture, entryOverrides = mapOf("manifest.json" to invalidManifest))

            val failure = assertThrows(ZineBackupStagingException::class.java) {
                runBlocking { ZineLibraryBackupStager().stage(archive, temp.resolve("stage")) }
            }

            assertEquals(ZineBackupStagingException.Reason.INVALID_MANIFEST, failure.reason)
            assertNoStagingChildren()
        }
    }

    @Test
    fun `rejects unsupported package version`() {
        val original = fixture(mapOf("one" to document()))
        val fixture = original.copy(
            manifest = original.manifest.copy(packageVersion = CURRENT_LIBRARY_BACKUP_VERSION + 1),
        )

        val failure = assertThrows(ZineBackupStagingException::class.java) {
            runBlocking { ZineLibraryBackupStager().stage(writeArchive(fixture), temp.resolve("stage")) }
        }

        assertEquals(ZineBackupStagingException.Reason.FUTURE_VERSION, failure.reason)
        assertEquals(CURRENT_LIBRARY_BACKUP_VERSION + 1, failure.encounteredVersion)
        assertEquals(CURRENT_LIBRARY_BACKUP_VERSION, failure.supportedVersion)
        assertNoStagingChildren()
    }

    @Test
    fun `rejects traversal and absolute entry paths before extraction`() {
        listOf("../escape", "/absolute", "C:/drive", "nested\\windows").forEachIndexed { index, badPath ->
            val fixture = fixture(mapOf("one" to document()))
            val archive = writeArchive(fixture, additionalEntries = mapOf(badPath to byteArrayOf(index.toByte())))

            val failure = assertThrows(ZineBackupStagingException::class.java) {
                runBlocking { ZineLibraryBackupStager().stage(archive, temp.resolve("stage")) }
            }

            assertEquals(ZineBackupStagingException.Reason.UNSAFE_ENTRY, failure.reason, badPath)
            assertNoStagingChildren()
        }
    }

    @Test
    fun `rejects unexpected archive content and cleans staging`() {
        val fixture = fixture(mapOf("one" to document()))
        val archive = writeArchive(fixture, additionalEntries = mapOf("notes.txt" to "surprise".encodeToByteArray()))

        val failure = assertThrows(ZineBackupStagingException::class.java) {
            runBlocking { ZineLibraryBackupStager().stage(archive, temp.resolve("stage")) }
        }

        assertEquals(ZineBackupStagingException.Reason.INVALID_STRUCTURE, failure.reason)
        assertNoStagingChildren()
    }

    @Test
    fun `rejects an unexpected asset entry not declared by the manifest`() {
        val fixture = fixture(mapOf("one" to document()))
        val unexpectedHash = "f".repeat(64)
        val archive = writeArchive(
            fixture,
            additionalEntries = mapOf("assets/$unexpectedHash" to byteArrayOf(1, 2, 3)),
        )

        val failure = assertThrows(ZineBackupStagingException::class.java) {
            runBlocking { ZineLibraryBackupStager().stage(archive, temp.resolve("stage")) }
        }

        assertEquals(ZineBackupStagingException.Reason.INVALID_STRUCTURE, failure.reason)
        assertNoStagingChildren()
    }

    @Test
    fun `rejects duplicate ZIP entry names before manifest validation`() {
        val fixture = fixture(mapOf("one" to document()))
        val archive = writeArchive(
            fixture,
            additionalEntries = mapOf("extra-one" to byteArrayOf(1), "extra-two" to byteArrayOf(2)),
        )
        replaceAscii(archive, "extra-two", "extra-one")

        val failure = assertThrows(ZineBackupStagingException::class.java) {
            runBlocking { ZineLibraryBackupStager().stage(archive, temp.resolve("stage")) }
        }

        assertEquals(ZineBackupStagingException.Reason.DUPLICATE_ENTRY, failure.reason)
        assertNoStagingChildren()
    }

    @Test
    fun `rejects asset hash mismatch after streamed extraction`() {
        val declaredBytes = ByteArray(2048) { it.toByte() }
        val tamperedBytes = declaredBytes.copyOf().also { it[100] = (it[100] + 1).toByte() }
        val hash = sha256(declaredBytes)
        val fixture = fixture(mapOf("one" to document(hash)), mapOf(hash to declaredBytes))
        val archive = writeArchive(fixture, entryOverrides = mapOf("assets/$hash" to tamperedBytes))

        val failure = assertThrows(ZineBackupStagingException::class.java) {
            runBlocking { ZineLibraryBackupStager().stage(archive, temp.resolve("stage")) }
        }

        assertEquals(ZineBackupStagingException.Reason.INTEGRITY_MISMATCH, failure.reason)
        assertNoStagingChildren()
    }

    @Test
    fun `rejects document SHA mismatch independently of its byte count`() {
        val original = fixture(mapOf("one" to document()))
        val project = original.manifest.projects.single().copy(documentSha256 = "f".repeat(64))
        val fixture = original.copy(manifest = original.manifest.copy(projects = listOf(project)))

        val failure = assertThrows(ZineBackupStagingException::class.java) {
            runBlocking { ZineLibraryBackupStager().stage(writeArchive(fixture), temp.resolve("stage")) }
        }

        assertEquals(ZineBackupStagingException.Reason.INTEGRITY_MISMATCH, failure.reason)
        assertNoStagingChildren()
    }

    @Test
    fun `rejects document byte-count mismatch before extraction`() {
        val original = fixture(mapOf("one" to document()))
        val project = original.manifest.projects.single().let { it.copy(documentByteCount = it.documentByteCount + 1L) }
        val fixture = original.copy(manifest = original.manifest.copy(projects = listOf(project)))

        val failure = assertThrows(ZineBackupStagingException::class.java) {
            runBlocking { ZineLibraryBackupStager().stage(writeArchive(fixture), temp.resolve("stage")) }
        }

        assertEquals(ZineBackupStagingException.Reason.INVALID_STRUCTURE, failure.reason)
        assertNoStagingChildren()
    }

    @Test
    fun `rejects archive missing its declared document`() {
        val fixture = fixture(mapOf("one" to document()))

        val failure = assertThrows(ZineBackupStagingException::class.java) {
            runBlocking {
                ZineLibraryBackupStager().stage(
                    writeArchive(fixture, omittedEntries = setOf("projects/one/document.json")),
                    temp.resolve("stage"),
                )
            }
        }

        assertEquals(ZineBackupStagingException.Reason.INVALID_STRUCTURE, failure.reason)
        assertNoStagingChildren()
    }

    @Test
    fun `rejects archive missing an asset referenced by its document`() {
        val asset = ByteArray(1024) { it.toByte() }
        val hash = sha256(asset)
        val fixture = fixture(mapOf("one" to document(hash)), mapOf(hash to asset))

        val failure = assertThrows(ZineBackupStagingException::class.java) {
            runBlocking {
                ZineLibraryBackupStager().stage(
                    writeArchive(fixture, omittedEntries = setOf("assets/$hash")),
                    temp.resolve("stage"),
                )
            }
        }

        assertEquals(ZineBackupStagingException.Reason.INVALID_STRUCTURE, failure.reason)
        assertNoStagingChildren()
    }

    @Test
    fun `rejects document asset references that disagree with project closure`() {
        val asset = ByteArray(2048) { (it * 31).toByte() }
        val hash = sha256(asset)
        val document = document(hash)
        val fixture = fixture(mapOf("one" to document), mapOf(hash to asset))
        val project = fixture.manifest.projects.single().copy(assetHashes = emptyList())
        val invalid = fixture.copy(manifest = fixture.manifest.copy(projects = listOf(project), assets = emptyList()))

        val failure = assertThrows(ZineBackupStagingException::class.java) {
            runBlocking { ZineLibraryBackupStager().stage(writeArchive(invalid), temp.resolve("stage")) }
        }

        assertEquals(ZineBackupStagingException.Reason.INVALID_STRUCTURE, failure.reason)
        assertNoStagingChildren()
    }

    @Test
    fun `rejects malformed document after its declared hash and count verify`() {
        val original = fixture(mapOf("one" to document()))
        val malformed = "{\"schemaVersion\":$CURRENT_SCHEMA_VERSION,\"broken\":true}".encodeToByteArray()
        val project = original.manifest.projects.single().copy(
            documentSha256 = sha256(malformed),
            documentByteCount = malformed.size.toLong(),
        )
        val fixture = original.copy(
            manifest = original.manifest.copy(projects = listOf(project)),
            documents = mapOf("one" to malformed),
        )

        val failure = assertThrows(ZineBackupStagingException::class.java) {
            runBlocking { ZineLibraryBackupStager().stage(writeArchive(fixture), temp.resolve("stage")) }
        }

        assertEquals(ZineBackupStagingException.Reason.INVALID_DOCUMENT, failure.reason)
        assertNoStagingChildren()
    }

    @Test
    fun `rejects invalid UTF-8 document after integrity verification`() {
        val original = fixture(mapOf("one" to document()))
        val invalidUtf8 = byteArrayOf(0xC3.toByte(), 0x28)
        val project = original.manifest.projects.single().copy(
            documentSha256 = sha256(invalidUtf8),
            documentByteCount = invalidUtf8.size.toLong(),
        )
        val fixture = original.copy(
            manifest = original.manifest.copy(projects = listOf(project)),
            documents = mapOf("one" to invalidUtf8),
        )

        val failure = assertThrows(ZineBackupStagingException::class.java) {
            runBlocking { ZineLibraryBackupStager().stage(writeArchive(fixture), temp.resolve("stage")) }
        }

        assertEquals(ZineBackupStagingException.Reason.INVALID_DOCUMENT, failure.reason)
        assertNoStagingChildren()
    }

    @Test
    fun `partial and unknown cover metadata are non-blocking degradation`() = runBlocking {
        val original = fixture(mapOf("partial" to document(), "unknown" to document()))
        val projects = original.manifest.projects.map { project ->
            when (project.sourceProjectId) {
                "partial" -> project.copy(coverSurface = "PAPER", coverStamp = null)
                else -> project.copy(coverSurface = "FUTURE_SURFACE", coverStamp = "FUTURE_STAMP")
            }
        }
        val fixture = original.copy(manifest = original.manifest.copy(projects = projects))

        ZineLibraryBackupStager().stage(writeArchive(fixture), temp.resolve("stage")).use { staged ->
            assertEquals(setOf("partial", "unknown"), staged.projects.map { it.manifestEntry.sourceProjectId }.toSet())
        }
    }

    @Test
    fun `rejects unsupported future document schema before decoding`() {
        val original = fixture(mapOf("one" to document()))
        val project = original.manifest.projects.single().copy(documentSchemaVersion = CURRENT_SCHEMA_VERSION + 1)
        val fixture = original.copy(manifest = original.manifest.copy(projects = listOf(project)))

        val failure = assertThrows(ZineBackupStagingException::class.java) {
            runBlocking { ZineLibraryBackupStager().stage(writeArchive(fixture), temp.resolve("stage")) }
        }

        assertEquals(ZineBackupStagingException.Reason.FUTURE_VERSION, failure.reason)
        assertEquals(CURRENT_SCHEMA_VERSION + 1, failure.encounteredVersion)
        assertEquals(CURRENT_SCHEMA_VERSION, failure.supportedVersion)
        assertNoStagingChildren()
    }

    @Test
    fun `rejects excessive per-entry expansion ratio`() {
        val asset = ByteArray(128 * 1024)
        val hash = sha256(asset)
        val fixture = fixture(mapOf("one" to document(hash)), mapOf(hash to asset))

        val failure = assertThrows(ZineBackupStagingException::class.java) {
            runBlocking {
                ZineLibraryBackupStager(ZineArchiveLimits(maximumExpansionRatio = 20L))
                    .stage(writeArchive(fixture), temp.resolve("stage"))
            }
        }

        assertEquals(ZineBackupStagingException.Reason.LIMIT_EXCEEDED, failure.reason)
        assertNoStagingChildren()
    }

    @Test
    fun `rejects excessive physical entry count before decoding manifest`() {
        val fixture = fixture(mapOf("one" to document()))

        val failure = assertThrows(ZineBackupStagingException::class.java) {
            runBlocking {
                ZineLibraryBackupStager(ZineArchiveLimits(maximumEntries = 1))
                    .stage(writeArchive(fixture), temp.resolve("stage"))
            }
        }

        assertEquals(ZineBackupStagingException.Reason.LIMIT_EXCEEDED, failure.reason)
        assertNoStagingChildren()
    }

    @Test
    fun `rejects archive whose raw file size exceeds the configured limit`() {
        val archive = writeArchive(fixture(mapOf("one" to document())))

        val failure = assertThrows(ZineBackupStagingException::class.java) {
            runBlocking {
                ZineLibraryBackupStager(ZineArchiveLimits(maximumArchiveBytes = Files.size(archive) - 1))
                    .stage(archive, temp.resolve("stage"))
            }
        }

        assertEquals(ZineBackupStagingException.Reason.LIMIT_EXCEEDED, failure.reason)
        assertNoStagingChildren()
    }

    @Test
    fun `rejects an oversized document from ZIP metadata before extraction`() {
        val oversizedDocument = ByteArray((MAX_BACKUP_DOCUMENT_BYTES + 1L).toInt()).also {
            Random(71).nextBytes(it)
        }
        val archive = writeArchive(
            fixture(mapOf("one" to document())),
            entryOverrides = mapOf("projects/one/document.json" to oversizedDocument),
        )

        val failure = assertThrows(ZineBackupStagingException::class.java) {
            runBlocking { ZineLibraryBackupStager().stage(archive, temp.resolve("stage")) }
        }

        assertEquals(ZineBackupStagingException.Reason.LIMIT_EXCEEDED, failure.reason)
        assertNoStagingChildren()
    }

    @Test
    fun `cancellation during streamed extraction removes staging`() = runBlocking {
        val asset = ByteArray(8 * 1024 * 1024).also { Random(19).nextBytes(it) }
        val hash = sha256(asset)
        val fixture = fixture(mapOf("one" to document(hash)), mapOf(hash to asset))
        val archive = writeArchive(fixture)
        val stagingParent = temp.resolve("stage")

        val job = launch(Dispatchers.Default) {
            ZineLibraryBackupStager(ZineArchiveLimits(copyBufferBytes = 1))
                .stage(archive, stagingParent)
        }
        repeat(500) {
            if (Files.exists(stagingParent) && Files.list(stagingParent).use { it.findAny().isPresent }) return@repeat
            delay(2)
        }
        job.cancelAndJoin()

        assertNoStagingChildren()
    }

    private fun assertNoStagingChildren() {
        val parent = temp.resolve("stage")
        if (Files.exists(parent)) {
            assertFalse(Files.list(parent).use { it.findAny().isPresent })
        }
    }

    private fun document(assetHash: String? = null): ZineDocument {
        val pages = (0 until 8).map { index ->
            val role = when (index) {
                0 -> PageRole.FRONT_COVER
                7 -> PageRole.BACK_COVER
                else -> PageRole.INTERIOR
            }
            val elements = if (index == 0 && assetHash != null) {
                listOf(
                    ImageElement(
                        id = "image-$index",
                        transform = Transform(0.0, 0.0, 100.0, 100.0),
                        assetId = assetHash,
                    ),
                )
            } else {
                emptyList()
            }
            Page(index = index, role = role, elements = elements)
        }
        return ZineDocument(format = ZineFormat.SINGLE_SHEET_8, paperSize = PaperSize.LETTER, pages = pages)
    }

    private fun fixture(
        documents: Map<String, ZineDocument>,
        assets: Map<String, ByteArray> = emptyMap(),
    ): Fixture {
        val serializer = JsonDocumentSerializer()
        val documentBytes = documents.mapValues { serializer.serialize(it.value).encodeToByteArray() }
        val projectEntries = documentBytes.map { (id, bytes) ->
            val document = documents.getValue(id)
            ZineBackupProjectEntry(
                sourceProjectId = id,
                title = "Project $id",
                format = document.format,
                paperSize = document.paperSize,
                createdAtEpochMs = 1L,
                updatedAtEpochMs = 2L,
                documentSchemaVersion = document.schemaVersion,
                documentPath = "projects/$id/document.json",
                documentSha256 = sha256(bytes),
                documentByteCount = bytes.size.toLong(),
                assetHashes = document.pages.flatMap { page ->
                    page.elements.filterIsInstance<ImageElement>().map { it.assetId }
                }.distinct(),
                coverSurface = null,
                coverStamp = null,
            )
        }
        val manifest = ZineLibraryBackupManifest(
            packageVersion = CURRENT_LIBRARY_BACKUP_VERSION,
            kind = LIBRARY_BACKUP_KIND,
            appVersion = "test",
            createdAtEpochMs = 3L,
            projects = projectEntries,
            assets = assets.map { (hash, bytes) -> AssetEntry(hash, "image/jpeg", 32, 32, bytes.size.toLong()) },
        )
        return Fixture(manifest, documentBytes, assets)
    }

    private fun writeArchive(
        fixture: Fixture,
        entryOverrides: Map<String, ByteArray> = emptyMap(),
        additionalEntries: Map<String, ByteArray> = emptyMap(),
        omittedEntries: Set<String> = emptySet(),
    ): Path {
        val archive = Files.createTempFile(temp, "backup-", ".zine")
        val json = Json { encodeDefaults = true }
        val entries = linkedMapOf<String, ByteArray>()
        entries["manifest.json"] = json.encodeToString(ZineLibraryBackupManifest.serializer(), fixture.manifest).encodeToByteArray()
        entries.putAll(fixture.documents.mapKeys { (id, _) -> "projects/$id/document.json" })
        entries.putAll(fixture.assets.mapKeys { (hash, _) -> "assets/$hash" })
        entries.putAll(entryOverrides)
        entries.putAll(additionalEntries)
        ZipOutputStream(Files.newOutputStream(archive)).use { zip ->
            entries.filterKeys { it !in omittedEntries }.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return archive
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

    private fun replaceAscii(path: Path, from: String, to: String) {
        require(from.length == to.length)
        val bytes = Files.readAllBytes(path)
        val needle = from.encodeToByteArray()
        val replacement = to.encodeToByteArray()
        var replacements = 0
        for (index in 0..bytes.size - needle.size) {
            if (needle.indices.all { offset -> bytes[index + offset] == needle[offset] }) {
                replacement.copyInto(bytes, index)
                replacements++
            }
        }
        assertEquals(2, replacements, "ZIP name must occur in its local and central headers")
        Files.write(path, bytes)
    }

    private data class Fixture(
        val manifest: ZineLibraryBackupManifest,
        val documents: Map<String, ByteArray>,
        val assets: Map<String, ByteArray>,
    )
}
