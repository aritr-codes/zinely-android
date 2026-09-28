package com.aritr.zinely.core.data.storage

import com.aritr.zinely.core.data.asset.MANIFEST_PATH
import com.aritr.zinely.core.data.asset.MAX_BACKUP_ASSET_BYTES
import com.aritr.zinely.core.data.asset.MAX_BACKUP_ASSETS
import com.aritr.zinely.core.data.asset.MAX_BACKUP_DOCUMENT_BYTES
import com.aritr.zinely.core.data.asset.MAX_BACKUP_MANIFEST_BYTES
import com.aritr.zinely.core.data.asset.MAX_BACKUP_PROJECTS
import com.aritr.zinely.core.data.asset.MAX_BACKUP_TOTAL_BYTES
import com.aritr.zinely.core.data.asset.ZineArchiveEntry
import com.aritr.zinely.core.data.asset.ZineLibraryBackupManifest
import com.aritr.zinely.core.data.asset.ZineLibraryBackupValidator
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Resource limits applied while producing a v2 library backup. */
public data class ZineBackupWriteLimits(
    val maximumManifestBytes: Long = MAX_BACKUP_MANIFEST_BYTES,
    val maximumDocumentBytes: Long = MAX_BACKUP_DOCUMENT_BYTES,
    val maximumAssetBytes: Long = MAX_BACKUP_ASSET_BYTES,
    val maximumTotalBytes: Long = MAX_BACKUP_TOTAL_BYTES,
    val maximumProjects: Int = MAX_BACKUP_PROJECTS,
    val maximumAssets: Int = MAX_BACKUP_ASSETS,
    val copyBufferBytes: Int = 64 * 1024,
) {
    init {
        require(maximumManifestBytes > 0L)
        require(maximumDocumentBytes > 0L)
        require(maximumAssetBytes > 0L)
        require(maximumTotalBytes > 0L)
        require(maximumProjects > 0)
        require(maximumAssets > 0)
        require(copyBufferBytes > 0)
    }
}

/**
 * A stable failure family for invalid local inputs or an incomplete backup write.
 *
 * [entryPath] names the one archive entry (`projects/<id>/document.json` or `assets/<hash>`) a failure belongs to, or
 * is `null` when it belongs to none (the manifest, a library-wide limit, the destination). [readSide] is `true` only
 * for an [Reason.IO_FAILURE] raised while *reading* that entry's source; `false` means the private archive itself
 * failed to be written. ADR-122 §3 rebuilds without a named entry and fails the whole backup otherwise.
 */
public class ZineBackupWritingException(
    public val reason: Reason,
    message: String,
    cause: Throwable? = null,
    public val entryPath: String? = null,
    public val readSide: Boolean = false,
) : Exception(message, cause) {
    public enum class Reason {
        INVALID_MANIFEST,
        SOURCE_MISMATCH,
        SOURCE_UNAVAILABLE,
        LIMIT_EXCEEDED,
        INTEGRITY_MISMATCH,
        DESTINATION_EXISTS,
        IO_FAILURE,
    }
}

/**
 * Produces the seekable, private v2 archive that an Android SAF adapter may subsequently stream to a
 * user-owned destination. Manifest construction remains the caller's responsibility because project
 * metadata is owned by the Android repository; this class only proves that the supplied snapshot
 * matches that manifest byte-for-byte.
 *
 * [projectDocuments] is keyed by `sourceProjectId`; [assets] is keyed by SHA-256. Both key sets must
 * exactly match [manifest]. The destination must not already exist, so cleanup can never erase a
 * previous good backup. Every incomplete destination created by this writer is removed best-effort.
 */
public class ZineLibraryBackupWriter(
    private val limits: ZineBackupWriteLimits = ZineBackupWriteLimits(),
    private val json: Json = Json { encodeDefaults = true },
    /** Opens one source for reading; a test seam for a photo that changes or can't be read mid-backup. */
    private val openSource: (Path) -> InputStream = { Files.newInputStream(it) },
) {
    public suspend fun write(
        manifest: ZineLibraryBackupManifest,
        projectDocuments: Map<String, Path>,
        assets: Map<String, Path>,
        destination: Path,
    ): Long {
        currentCoroutineContext().ensureActive()

        val documentsSnapshot = projectDocuments.toMap()
        val assetsSnapshot = assets.toMap()
        val manifestBytes = json.encodeToString(manifest).encodeToByteArray()
        validateInputs(manifest, manifestBytes, documentsSnapshot, assetsSnapshot)

        val output = destination.toAbsolutePath().normalize()
        if (Files.exists(output)) {
            fail(ZineBackupWritingException.Reason.DESTINATION_EXISTS, "Backup destination already exists")
        }
        var created = false
        var complete = false
        var archiveByteCount = 0L
        try {
            output.parent?.let(Files::createDirectories)
            val raw = try {
                Files.newOutputStream(output, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
            } catch (existing: FileAlreadyExistsException) {
                throw ZineBackupWritingException(
                    ZineBackupWritingException.Reason.DESTINATION_EXISTS,
                    "Backup destination already exists",
                    existing,
                )
            }
            created = true
            raw.use { outputStream ->
                ZipOutputStream(BufferedOutputStream(outputStream, limits.copyBufferBytes)).use { zip ->
                    // Stored-like DEFLATE keeps the writer one-pass and prevents a valid, highly repetitive
                    // document from becoming an archive that the restore expansion-ratio gate rejects.
                    zip.setLevel(Deflater.NO_COMPRESSION)
                    var expandedBytes = 0L
                    expandedBytes = checkedAdd(expandedBytes, manifestBytes.size.toLong())
                    writeBytes(zip, MANIFEST_PATH, manifestBytes)

                    for (project in manifest.projects) {
                        currentCoroutineContext().ensureActive()
                        val source = documentsSnapshot.getValue(project.sourceProjectId)
                        val written = writeFile(zip, project.documentPath, source, limits.maximumDocumentBytes)
                        if (written.byteCount != project.documentByteCount || written.sha256 != project.documentSha256) {
                            fail(
                                ZineBackupWritingException.Reason.INTEGRITY_MISMATCH,
                                "Document '${project.documentPath}' does not match its declared byte count and SHA-256",
                                project.documentPath,
                            )
                        }
                        expandedBytes = checkedAdd(expandedBytes, written.byteCount)
                        enforceTotal(expandedBytes)
                    }

                    for (asset in manifest.assets) {
                        currentCoroutineContext().ensureActive()
                        val archivePath = assetPath(asset.hash)
                        val written = writeFile(zip, archivePath, assetsSnapshot.getValue(asset.hash), limits.maximumAssetBytes)
                        if (written.byteCount != asset.byteCount || written.sha256 != asset.hash) {
                            fail(
                                ZineBackupWritingException.Reason.INTEGRITY_MISMATCH,
                                "Asset '$archivePath' does not match its declared byte count and SHA-256",
                                archivePath,
                            )
                        }
                        expandedBytes = checkedAdd(expandedBytes, written.byteCount)
                        enforceTotal(expandedBytes)
                    }
                }
            }
            archiveByteCount = Files.size(output)
            complete = true
        } catch (known: ZineBackupWritingException) {
            throw known
        } catch (failure: IOException) {
            throw ZineBackupWritingException(
                ZineBackupWritingException.Reason.IO_FAILURE,
                "Library backup could not be written",
                failure,
            )
        } finally {
            if (created && !complete) {
                try {
                    Files.deleteIfExists(output)
                } catch (_: IOException) {
                    // Preserve the originating failure. The Android owner stages into a private transfer
                    // directory where a later janitor can retry cleanup; this path is never user-visible.
                }
            }
        }
        return archiveByteCount
    }

    private fun validateInputs(
        manifest: ZineLibraryBackupManifest,
        manifestBytes: ByteArray,
        projectDocuments: Map<String, Path>,
        assets: Map<String, Path>,
    ) {
        if (manifestBytes.isEmpty() || manifestBytes.size.toLong() > limits.maximumManifestBytes) {
            fail(ZineBackupWritingException.Reason.LIMIT_EXCEEDED, "Backup manifest exceeds the write limit")
        }
        if (manifest.projects.size > limits.maximumProjects || manifest.assets.size > limits.maximumAssets) {
            fail(ZineBackupWritingException.Reason.LIMIT_EXCEEDED, "Backup contains too many projects or assets")
        }

        val expectedDocuments = manifest.projects.mapTo(linkedSetOf()) { it.sourceProjectId }
        val expectedAssets = manifest.assets.mapTo(linkedSetOf()) { it.hash }
        if (projectDocuments.keys != expectedDocuments || assets.keys != expectedAssets) {
            fail(
                ZineBackupWritingException.Reason.SOURCE_MISMATCH,
                "Backup source files do not exactly match the manifest",
            )
        }

        val declaredEntries = buildList {
            add(ZineArchiveEntry(MANIFEST_PATH, manifestBytes.size.toLong()))
            manifest.projects.forEach { add(ZineArchiveEntry(it.documentPath, it.documentByteCount)) }
            manifest.assets.forEach { add(ZineArchiveEntry(assetPath(it.hash), it.byteCount)) }
        }
        val validation = ZineLibraryBackupValidator().validate(manifest, declaredEntries)
        if (!validation.isValid) {
            fail(
                ZineBackupWritingException.Reason.INVALID_MANIFEST,
                "Backup manifest is invalid: ${validation.errors.joinToString { it.code }}",
            )
        }

        val sources = manifest.projects.map { it.documentPath to projectDocuments.getValue(it.sourceProjectId) } +
            manifest.assets.map { assetPath(it.hash) to assets.getValue(it.hash) }
        sources.forEach { (entryPath, source) ->
            if (!Files.isRegularFile(source)) {
                fail(
                    ZineBackupWritingException.Reason.SOURCE_UNAVAILABLE,
                    "Backup source '$entryPath' is unavailable",
                    entryPath,
                )
            }
        }

        var declaredTotal = manifestBytes.size.toLong()
        manifest.projects.forEach { project ->
            if (project.documentByteCount > limits.maximumDocumentBytes) {
                fail(ZineBackupWritingException.Reason.LIMIT_EXCEEDED, "A document exceeds the write limit", project.documentPath)
            }
            declaredTotal = checkedAdd(declaredTotal, project.documentByteCount)
        }
        manifest.assets.forEach { asset ->
            if (asset.byteCount > limits.maximumAssetBytes) {
                fail(ZineBackupWritingException.Reason.LIMIT_EXCEEDED, "An asset exceeds the write limit", assetPath(asset.hash))
            }
            declaredTotal = checkedAdd(declaredTotal, asset.byteCount)
        }
        enforceTotal(declaredTotal)
    }

    private suspend fun writeFile(zip: ZipOutputStream, name: String, source: Path, entryLimit: Long): WrittenEntry {
        zip.putNextEntry(ZipEntry(name))
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(limits.copyBufferBytes)
        var count = 0L
        try {
            val input = readSource(name) { BufferedInputStream(openSource(source), limits.copyBufferBytes) }
            try {
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = readSource(name) { input.read(buffer) }
                    if (read < 0) break
                    count = checkedAdd(count, read.toLong())
                    if (count > entryLimit) {
                        fail(ZineBackupWritingException.Reason.LIMIT_EXCEEDED, "Backup entry '$name' exceeds its limit", name)
                    }
                    digest.update(buffer, 0, read)
                    zip.write(buffer, 0, read)
                }
            } finally {
                try {
                    input.close()
                } catch (_: IOException) {
                    // Closing a source is not writing the archive; every byte that mattered was already read.
                }
            }
        } finally {
            zip.closeEntry()
        }
        if (count <= 0L) {
            fail(ZineBackupWritingException.Reason.INTEGRITY_MISMATCH, "Backup entry '$name' is empty", name)
        }
        return WrittenEntry(count, digest.digest().toHex())
    }

    private suspend fun writeBytes(zip: ZipOutputStream, name: String, bytes: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        try {
            var offset = 0
            while (offset < bytes.size) {
                currentCoroutineContext().ensureActive()
                val count = minOf(limits.copyBufferBytes, bytes.size - offset)
                zip.write(bytes, offset, count)
                offset += count
            }
        } finally {
            zip.closeEntry()
        }
    }

    private fun checkedAdd(left: Long, right: Long): Long = try {
        Math.addExact(left, right)
    } catch (_: ArithmeticException) {
        fail(ZineBackupWritingException.Reason.LIMIT_EXCEEDED, "Backup byte count overflowed")
    }

    private fun enforceTotal(total: Long) {
        if (total > limits.maximumTotalBytes) {
            fail(ZineBackupWritingException.Reason.LIMIT_EXCEEDED, "Backup expands beyond the write limit")
        }
    }

    private fun ByteArray.toHex(): String {
        val alphabet = "0123456789abcdef"
        return buildString(size * 2) {
            for (byte in this@toHex) {
                val value = byte.toInt() and 0xff
                append(alphabet[value ushr 4])
                append(alphabet[value and 0x0f])
            }
        }
    }

    private data class WrittenEntry(val byteCount: Long, val sha256: String)

    private companion object {
        fun fail(reason: ZineBackupWritingException.Reason, message: String, entryPath: String? = null): Nothing =
            throw ZineBackupWritingException(reason, message, entryPath = entryPath)

        fun assetPath(hash: String): String = "assets/$hash"

        /** A failure opening or reading [entryPath]'s source, as opposed to writing the archive. */
        inline fun <T> readSource(entryPath: String, block: () -> T): T = try {
            block()
        } catch (failure: IOException) {
            throw ZineBackupWritingException(
                ZineBackupWritingException.Reason.IO_FAILURE,
                "Backup source '$entryPath' could not be read",
                failure,
                entryPath = entryPath,
                readSide = true,
            )
        }
    }
}
