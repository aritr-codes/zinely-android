package com.aritr.zinely.core.data.storage

import com.aritr.zinely.core.data.asset.AssetEntry
import com.aritr.zinely.core.data.asset.CURRENT_LIBRARY_BACKUP_VERSION
import com.aritr.zinely.core.data.asset.LIBRARY_BACKUP_KIND
import com.aritr.zinely.core.data.asset.MAX_BACKUP_ARCHIVE_BYTES
import com.aritr.zinely.core.data.asset.MAX_BACKUP_TOTAL_BYTES
import com.aritr.zinely.core.data.asset.ZineBackupProjectEntry
import com.aritr.zinely.core.data.asset.ZineLibraryBackupManifest
import com.aritr.zinely.core.data.serialization.JsonDocumentSerializer
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.ZipFile

class ZineLibraryBackupWriterTest {
    @TempDir
    lateinit var temp: Path
    private val backupJson = Json { encodeDefaults = true }

    @Test
    fun `writer produces a manifest-first archive accepted by the trusted stager with one shared asset`() = runBlocking {
        val assetBytes = "shared-jpeg-master".encodeToByteArray()
        val assetHash = sha256(assetBytes)
        val fixture = fixture(
            documents = linkedMapOf(
                "first" to document(assetHash),
                "second" to document(assetHash),
            ),
            assets = mapOf(assetHash to assetBytes),
        )
        val destination = temp.resolve("library.zine")

        ZineLibraryBackupWriter().write(
            fixture.manifest,
            fixture.documentPaths,
            fixture.assetPaths,
            destination,
        )

        ZipFile(destination.toFile()).use { zip ->
            val names = zip.entries().asSequence().map { it.name }.toList()
            assertEquals("manifest.json", names.first())
            assertEquals(1, names.count { it == "assets/$assetHash" })
        }
        ZineLibraryBackupStager().stage(destination, temp.resolve("staging")).use { staged ->
            assertEquals(listOf("first", "second"), staged.projects.map { it.manifestEntry.sourceProjectId })
            assertEquals(setOf(assetHash), staged.assets.keys)
        }
    }

    @Test
    fun `encoded archive overhead remains inside the separate restore envelope`() = runBlocking {
        val fixture = fixture(mapOf("project" to document()))
        val manifestBytes = backupJson.encodeToString(fixture.manifest)
            .encodeToByteArray()
        val expandedBytes = manifestBytes.size.toLong() + fixture.manifest.projects.single().documentByteCount
        val destination = temp.resolve("archive-envelope.zine")

        val archiveByteCount = ZineLibraryBackupWriter(
            ZineBackupWriteLimits(maximumTotalBytes = expandedBytes),
        ).write(fixture.manifest, fixture.documentPaths, fixture.assetPaths, destination)

        assertTrue(archiveByteCount > expandedBytes)
        assertEquals(Files.size(destination), archiveByteCount)
        assertTrue(MAX_BACKUP_ARCHIVE_BYTES > MAX_BACKUP_TOTAL_BYTES)
        assertTrue(archiveByteCount <= MAX_BACKUP_ARCHIVE_BYTES)
        ZineLibraryBackupStager().stage(destination, temp.resolve("envelope-staging")).close()
    }

    @Test
    fun `declared document hash mismatch deletes the incomplete destination`() {
        val fixture = fixture(mapOf("project" to document()))
        val invalid = fixture.manifest.copy(
            projects = fixture.manifest.projects.map { it.copy(documentSha256 = "0".repeat(64)) },
        )
        val destination = temp.resolve("mismatch.zine")

        val error = assertThrows(ZineBackupWritingException::class.java) {
            runBlocking {
                ZineLibraryBackupWriter().write(invalid, fixture.documentPaths, fixture.assetPaths, destination)
            }
        }

        assertEquals(ZineBackupWritingException.Reason.INTEGRITY_MISMATCH, error.reason)
        assertFalse(Files.exists(destination))
    }

    @Test
    fun `changed asset bytes are rejected and the incomplete destination is deleted`() {
        val declaredAsset = "declared-jpeg-master".encodeToByteArray()
        val hash = sha256(declaredAsset)
        val fixture = fixture(mapOf("project" to document(hash)), mapOf(hash to declaredAsset))
        Files.write(fixture.assetPaths.getValue(hash), "tampered-jpeg-master".encodeToByteArray())
        val destination = temp.resolve("tampered-asset.zine")

        val error = assertThrows(ZineBackupWritingException::class.java) {
            runBlocking {
                ZineLibraryBackupWriter().write(
                    fixture.manifest,
                    fixture.documentPaths,
                    fixture.assetPaths,
                    destination,
                )
            }
        }

        assertEquals(ZineBackupWritingException.Reason.INTEGRITY_MISMATCH, error.reason)
        assertFalse(Files.exists(destination))
    }

    @Test
    fun `source maps must exactly match the manifest before an output is created`() {
        val fixture = fixture(mapOf("project" to document()))
        val destination = temp.resolve("missing-source.zine")

        val error = assertThrows(ZineBackupWritingException::class.java) {
            runBlocking {
                ZineLibraryBackupWriter().write(fixture.manifest, emptyMap(), fixture.assetPaths, destination)
            }
        }

        assertEquals(ZineBackupWritingException.Reason.SOURCE_MISMATCH, error.reason)
        assertFalse(Files.exists(destination))
    }

    @Test
    fun `actual bytes enforce an injected entry limit and delete the incomplete destination`() {
        val fixture = fixture(mapOf("project" to document()))
        val destination = temp.resolve("limited.zine")
        val writer = ZineLibraryBackupWriter(
            ZineBackupWriteLimits(
                maximumDocumentBytes = fixture.manifest.projects.single().documentByteCount - 1L,
            ),
        )

        val error = assertThrows(ZineBackupWritingException::class.java) {
            runBlocking { writer.write(fixture.manifest, fixture.documentPaths, fixture.assetPaths, destination) }
        }

        assertEquals(ZineBackupWritingException.Reason.LIMIT_EXCEEDED, error.reason)
        assertFalse(Files.exists(destination))
    }

    @Test
    fun `an existing destination is never overwritten or deleted`() {
        val fixture = fixture(mapOf("project" to document()))
        val destination = temp.resolve("existing.zine")
        val original = "previous-good-backup".encodeToByteArray()
        Files.write(destination, original)

        val error = assertThrows(ZineBackupWritingException::class.java) {
            runBlocking {
                ZineLibraryBackupWriter().write(fixture.manifest, fixture.documentPaths, fixture.assetPaths, destination)
            }
        }

        assertEquals(ZineBackupWritingException.Reason.DESTINATION_EXISTS, error.reason)
        assertTrue(original.contentEquals(Files.readAllBytes(destination)))
    }

    @Test
    fun `cancellation during streaming removes the incomplete destination`() = runBlocking {
        val sourceId = "large"
        val bytes = ByteArray(8 * 1024 * 1024) { 0x31 }
        val source = temp.resolve("large-document.json")
        Files.write(source, bytes)
        val manifest = manifestForRawDocument(sourceId, bytes)
        val destination = temp.resolve("cancelled.zine")
        val writer = ZineLibraryBackupWriter(ZineBackupWriteLimits(copyBufferBytes = 1))

        val job = launch(Dispatchers.Default) {
            writer.write(manifest, mapOf(sourceId to source), emptyMap(), destination)
        }
        while (!Files.exists(destination) && job.isActive) yield()
        job.cancelAndJoin()

        assertFalse(Files.exists(destination))
    }

    private fun fixture(
        documents: Map<String, ZineDocument>,
        assets: Map<String, ByteArray> = emptyMap(),
    ): Fixture {
        val serializer = JsonDocumentSerializer()
        val documentBytes = documents.mapValues { serializer.serialize(it.value).encodeToByteArray() }
        val documentPaths = documentBytes.mapValues { (id, bytes) ->
            temp.resolve("source-$id.json").also { Files.write(it, bytes) }
        }
        val assetPaths = assets.mapValues { (hash, bytes) ->
            temp.resolve("source-$hash").also { Files.write(it, bytes) }
        }
        val projects = documentBytes.map { (id, bytes) ->
            val source = documents.getValue(id)
            ZineBackupProjectEntry(
                sourceProjectId = id,
                title = "Project $id",
                format = source.format,
                paperSize = source.paperSize,
                createdAtEpochMs = 1L,
                updatedAtEpochMs = 2L,
                documentSchemaVersion = source.schemaVersion,
                documentPath = "projects/$id/document.json",
                documentSha256 = sha256(bytes),
                documentByteCount = bytes.size.toLong(),
                assetHashes = source.pages.flatMap { page ->
                    page.elements.filterIsInstance<ImageElement>().map { it.assetId }
                }.distinct(),
                coverSurface = null,
                coverStamp = null,
            )
        }
        val manifest = ZineLibraryBackupManifest(
            packageVersion = CURRENT_LIBRARY_BACKUP_VERSION,
            kind = LIBRARY_BACKUP_KIND,
            appVersion = "writer-test",
            createdAtEpochMs = 3L,
            projects = projects,
            assets = assets.map { (hash, bytes) -> AssetEntry(hash, "image/jpeg", 32, 32, bytes.size.toLong()) },
        )
        return Fixture(manifest, documentPaths, assetPaths)
    }

    private fun manifestForRawDocument(id: String, bytes: ByteArray): ZineLibraryBackupManifest =
        ZineLibraryBackupManifest(
            packageVersion = CURRENT_LIBRARY_BACKUP_VERSION,
            kind = LIBRARY_BACKUP_KIND,
            appVersion = "writer-test",
            createdAtEpochMs = 1L,
            projects = listOf(
                ZineBackupProjectEntry(
                    sourceProjectId = id,
                    title = "Large",
                    format = ZineFormat.SINGLE_SHEET_8,
                    paperSize = PaperSize.LETTER,
                    createdAtEpochMs = 1L,
                    updatedAtEpochMs = 1L,
                    documentSchemaVersion = 2,
                    documentPath = "projects/$id/document.json",
                    documentSha256 = sha256(bytes),
                    documentByteCount = bytes.size.toLong(),
                    assetHashes = emptyList(),
                    coverSurface = null,
                    coverStamp = null,
                ),
            ),
        )

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

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

    private data class Fixture(
        val manifest: ZineLibraryBackupManifest,
        val documentPaths: Map<String, Path>,
        val assetPaths: Map<String, Path>,
    )
}
