package com.aritr.zinely.core.data.storage

import com.aritr.zinely.core.data.asset.MANIFEST_PATH
import com.aritr.zinely.core.data.asset.CURRENT_LIBRARY_BACKUP_VERSION
import com.aritr.zinely.core.data.asset.MAX_BACKUP_ASSET_BYTES
import com.aritr.zinely.core.data.asset.MAX_BACKUP_DOCUMENT_BYTES
import com.aritr.zinely.core.data.asset.MAX_BACKUP_MANIFEST_BYTES
import com.aritr.zinely.core.data.asset.MAX_BACKUP_ASSETS
import com.aritr.zinely.core.data.asset.MAX_BACKUP_PROJECTS
import com.aritr.zinely.core.data.asset.MAX_BACKUP_ARCHIVE_BYTES
import com.aritr.zinely.core.data.asset.MAX_BACKUP_TOTAL_BYTES
import com.aritr.zinely.core.data.asset.ZineArchiveEntry
import com.aritr.zinely.core.data.asset.ZineBackupProjectEntry
import com.aritr.zinely.core.data.asset.ZineLibraryBackupManifest
import com.aritr.zinely.core.data.asset.ZineLibraryBackupValidator
import com.aritr.zinely.core.data.serialization.JsonDocumentSerializer
import com.aritr.zinely.core.data.validation.DefaultDocumentValidator
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.CURRENT_SCHEMA_VERSION
import com.aritr.zinely.core.model.ZineDocument
import java.io.BufferedInputStream
import java.io.IOException
import java.nio.charset.CharacterCodingException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.Comparator
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Additional codec limits which complement the v2 package-contract limits. */
public data class ZineArchiveLimits(
    val maximumArchiveBytes: Long = MAX_BACKUP_ARCHIVE_BYTES,
    val maximumExpansionRatio: Long = 200L,
    val maximumEntries: Int = MAX_BACKUP_PROJECTS + MAX_BACKUP_ASSETS + 1,
    val copyBufferBytes: Int = 64 * 1024,
) {
    init {
        require(maximumArchiveBytes > 0L)
        require(maximumExpansionRatio > 0L)
        require(maximumEntries > 0)
        require(copyBufferBytes > 0)
    }
}

/** A stable failure family for hostile, corrupt, incompatible, or invalid backup input. */
public class ZineBackupStagingException(
    public val reason: Reason,
    message: String,
    cause: Throwable? = null,
    public val encounteredVersion: Int? = null,
    public val supportedVersion: Int? = null,
) : Exception(message, cause) {
    public enum class Reason {
        MALFORMED_ARCHIVE,
        UNSAFE_ENTRY,
        DUPLICATE_ENTRY,
        LIMIT_EXCEEDED,
        INVALID_MANIFEST,
        INVALID_STRUCTURE,
        INTEGRITY_MISMATCH,
        INVALID_DOCUMENT,
        FUTURE_VERSION,
    }
}

/** One decoded project whose original bytes remain isolated under [StagedZineLibraryBackup.root]. */
public data class StagedZineProject(
    val manifestEntry: ZineBackupProjectEntry,
    val document: ZineDocument,
    val documentPath: Path,
)

/**
 * A fully verified v2 library backup. Closing it removes the private staging tree.
 *
 * This value deliberately has no commit method: staging cannot write to live project or asset paths.
 */
public class StagedZineLibraryBackup internal constructor(
    public val root: Path,
    public val manifest: ZineLibraryBackupManifest,
    public val projects: List<StagedZineProject>,
    public val assets: Map<String, Path>,
) : AutoCloseable {
    override fun close(): Unit = deleteTree(root)
}

/**
 * Pure-JVM, fail-closed reader for v2 whole-library `.zine` archives.
 *
 * The archive is never expanded into the live library. Central-directory sizes are preflight hints,
 * then every actual byte is counted and hashed while it is copied into a unique staging directory.
 */
public class ZineLibraryBackupStager(
    private val limits: ZineArchiveLimits = ZineArchiveLimits(),
    private val manifestJson: Json = Json { ignoreUnknownKeys = true },
) {
    public suspend fun stage(archive: Path, stagingParent: Path): StagedZineLibraryBackup {
        currentCoroutineContext().ensureActive()
        val archiveBytes = checkedArchiveSize(archive)
        Files.createDirectories(stagingParent)
        val stagingRoot = Files.createTempDirectory(stagingParent, STAGING_PREFIX)
        try {
            return openZip(archive).use { zip -> stageOpenArchive(zip, archiveBytes, stagingRoot) }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            deleteTree(stagingRoot)
            throw cancelled
        } catch (known: ZineBackupStagingException) {
            deleteTree(stagingRoot)
            throw known
        } catch (failure: ZipException) {
            deleteTree(stagingRoot)
            throw ZineBackupStagingException(
                ZineBackupStagingException.Reason.MALFORMED_ARCHIVE,
                "Backup is not a readable ZIP archive",
                failure,
            )
        } catch (failure: IOException) {
            deleteTree(stagingRoot)
            throw ZineBackupStagingException(
                ZineBackupStagingException.Reason.MALFORMED_ARCHIVE,
                "Backup could not be read completely",
                failure,
            )
        } catch (failure: SerializationException) {
            deleteTree(stagingRoot)
            throw ZineBackupStagingException(
                ZineBackupStagingException.Reason.INVALID_MANIFEST,
                "Backup manifest is not valid v2 JSON",
                failure,
            )
        } catch (failure: IllegalArgumentException) {
            deleteTree(stagingRoot)
            throw ZineBackupStagingException(
                ZineBackupStagingException.Reason.INVALID_MANIFEST,
                "Backup manifest is not valid v2 JSON",
                failure,
            )
        }
    }

    private fun checkedArchiveSize(archive: Path): Long {
        val size = try {
            Files.size(archive)
        } catch (failure: IOException) {
            throw ZineBackupStagingException(
                ZineBackupStagingException.Reason.MALFORMED_ARCHIVE,
                "Backup file is unavailable",
                failure,
            )
        }
        if (size <= 0L || size > limits.maximumArchiveBytes) {
            throw ZineBackupStagingException(
                ZineBackupStagingException.Reason.LIMIT_EXCEEDED,
                "Backup file size is outside the restore limit",
            )
        }
        return size
    }

    private fun openZip(archive: Path): ZipFile = try {
        ZipFile(archive.toFile())
    } catch (failure: IOException) {
        throw ZineBackupStagingException(
            ZineBackupStagingException.Reason.MALFORMED_ARCHIVE,
            "Backup is not a readable ZIP archive",
            failure,
        )
    }

    private suspend fun stageOpenArchive(
        zip: ZipFile,
        archiveBytes: Long,
        root: Path,
    ): StagedZineLibraryBackup {
        val entries = preflight(zip, archiveBytes)
        val manifestEntry = entries[MANIFEST_PATH]
            ?: fail(ZineBackupStagingException.Reason.INVALID_STRUCTURE, "Backup has no manifest.json")
        if (manifestEntry.size > MAX_BACKUP_MANIFEST_BYTES) {
            fail(ZineBackupStagingException.Reason.LIMIT_EXCEEDED, "Backup manifest exceeds the restore limit")
        }

        val manifestCopy = copyEntry(zip, manifestEntry, root.resolve(MANIFEST_PATH), MAX_BACKUP_MANIFEST_BYTES)
        val manifestText = strictUtf8(Files.readAllBytes(manifestCopy.path), "manifest")
        val manifest = try {
            manifestJson.decodeFromString(ZineLibraryBackupManifest.serializer(), manifestText)
        } catch (failure: SerializationException) {
            throw ZineBackupStagingException(
                ZineBackupStagingException.Reason.INVALID_MANIFEST,
                "Backup manifest is not valid v2 JSON",
                failure,
            )
        } catch (failure: IllegalArgumentException) {
            throw ZineBackupStagingException(
                ZineBackupStagingException.Reason.INVALID_MANIFEST,
                "Backup manifest is not valid v2 JSON",
                failure,
            )
        }

        validateStructure(
            manifest,
            entries.values.map { ZineArchiveEntry(it.name, it.size) },
        )

        val actualEntries = mutableListOf(ZineArchiveEntry(MANIFEST_PATH, manifestCopy.byteCount))
        val stagedAssets = linkedMapOf<String, Path>()
        for (asset in manifest.assets) {
            currentCoroutineContext().ensureActive()
            val path = "assets/${asset.hash}"
            val entry = entries.getValue(path)
            val copied = copyEntry(zip, entry, root.resolve(path), MAX_BACKUP_ASSET_BYTES)
            actualEntries += ZineArchiveEntry(path, copied.byteCount)
            if (copied.byteCount != asset.byteCount || copied.sha256 != asset.hash) {
                fail(
                    ZineBackupStagingException.Reason.INTEGRITY_MISMATCH,
                    "Asset '$path' does not match its declared byte count and SHA-256",
                )
            }
            stagedAssets[asset.hash] = copied.path
        }

        val serializer = JsonDocumentSerializer()
        val documentValidator = DefaultDocumentValidator()
        val stagedProjects = ArrayList<StagedZineProject>(manifest.projects.size)
        for (project in manifest.projects) {
            currentCoroutineContext().ensureActive()
            val entry = entries.getValue(project.documentPath)
            val copied = copyEntry(zip, entry, root.resolve(project.documentPath), MAX_BACKUP_DOCUMENT_BYTES)
            actualEntries += ZineArchiveEntry(project.documentPath, copied.byteCount)
            if (copied.byteCount != project.documentByteCount || copied.sha256 != project.documentSha256) {
                fail(
                    ZineBackupStagingException.Reason.INTEGRITY_MISMATCH,
                    "Document '${project.documentPath}' does not match its declared byte count and SHA-256",
                )
            }
            val documentBytes = Files.readAllBytes(copied.path)
            val documentText = strictUtf8(documentBytes, "document '${project.documentPath}'")
            val rawSchema = rawSchemaVersion(documentText, project.documentPath)
            val document = try {
                serializer.deserialize(documentText)
            } catch (failure: Exception) {
                throw ZineBackupStagingException(
                    ZineBackupStagingException.Reason.INVALID_DOCUMENT,
                    "Document '${project.documentPath}' could not be decoded",
                    failure,
                )
            }
            if (rawSchema != project.documentSchemaVersion ||
                document.format != project.format ||
                document.paperSize != project.paperSize
            ) {
                fail(
                    ZineBackupStagingException.Reason.INVALID_DOCUMENT,
                    "Document '${project.documentPath}' disagrees with its manifest metadata",
                )
            }
            val validation = documentValidator.validate(document)
            if (!validation.isValid) {
                fail(
                    ZineBackupStagingException.Reason.INVALID_DOCUMENT,
                    "Document '${project.documentPath}' violates document invariants: " +
                        validation.errors.joinToString { it.code },
                )
            }
            val referenced = document.pages.asSequence()
                .flatMap { it.elements.asSequence() }
                .filterIsInstance<ImageElement>()
                .mapTo(linkedSetOf()) { it.assetId }
            if (referenced != project.assetHashes.toSet()) {
                fail(
                    ZineBackupStagingException.Reason.INVALID_DOCUMENT,
                    "Document '${project.documentPath}' asset references do not match its manifest closure",
                )
            }
            stagedProjects += StagedZineProject(project, document, copied.path)
        }

        validateStructure(manifest, actualEntries)
        return StagedZineLibraryBackup(root, manifest, stagedProjects, stagedAssets)
    }

    private fun preflight(zip: ZipFile, archiveBytes: Long): LinkedHashMap<String, ZipEntry> {
        val result = linkedMapOf<String, ZipEntry>()
        var expandedTotal = 0L
        val enumeration = zip.entries()
        while (enumeration.hasMoreElements()) {
            val entry = enumeration.nextElement()
            if (entry.isDirectory || !isSafeArchivePath(entry.name)) {
                fail(ZineBackupStagingException.Reason.UNSAFE_ENTRY, "Unsafe ZIP entry '${entry.name}'")
            }
            if (result.putIfAbsent(entry.name, entry) != null) {
                fail(ZineBackupStagingException.Reason.DUPLICATE_ENTRY, "Duplicate ZIP entry '${entry.name}'")
            }
            if (result.size > limits.maximumEntries) {
                fail(ZineBackupStagingException.Reason.LIMIT_EXCEEDED, "Backup contains too many ZIP entries")
            }
            val size = entry.size
            val compressed = entry.compressedSize
            if (size <= 0L || compressed < 0L) {
                fail(ZineBackupStagingException.Reason.MALFORMED_ARCHIVE, "ZIP entry '${entry.name}' has invalid sizes")
            }
            if (size > preflightEntryLimit(entry.name)) {
                fail(ZineBackupStagingException.Reason.LIMIT_EXCEEDED, "ZIP entry '${entry.name}' exceeds the restore limit")
            }
            if (compressed == 0L || exceedsRatio(size, compressed)) {
                fail(ZineBackupStagingException.Reason.LIMIT_EXCEEDED, "ZIP entry '${entry.name}' expands excessively")
            }
            expandedTotal = checkedAdd(expandedTotal, size)
            if (expandedTotal > MAX_BACKUP_TOTAL_BYTES) {
                fail(ZineBackupStagingException.Reason.LIMIT_EXCEEDED, "Backup expands beyond the restore limit")
            }
        }
        if (result.isEmpty()) {
            fail(ZineBackupStagingException.Reason.MALFORMED_ARCHIVE, "Backup archive is empty")
        }
        if (exceedsRatio(expandedTotal, archiveBytes)) {
            fail(ZineBackupStagingException.Reason.LIMIT_EXCEEDED, "Backup expands excessively")
        }
        return result
    }

    private suspend fun copyEntry(zip: ZipFile, entry: ZipEntry, target: Path, limit: Long): CopiedEntry {
        Files.createDirectories(target.parent)
        val digest = MessageDigest.getInstance("SHA-256")
        var count = 0L
        val buffer = ByteArray(limits.copyBufferBytes)
        BufferedInputStream(zip.getInputStream(entry), limits.copyBufferBytes).use { input ->
            Files.newOutputStream(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use { output ->
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = input.read(buffer)
                    if (read < 0) break
                    count = checkedAdd(count, read.toLong())
                    if (count > limit || count > entry.size) {
                        fail(ZineBackupStagingException.Reason.LIMIT_EXCEEDED, "ZIP entry '${entry.name}' exceeds its limit")
                    }
                    digest.update(buffer, 0, read)
                    output.write(buffer, 0, read)
                }
            }
        }
        if (count != entry.size) {
            fail(ZineBackupStagingException.Reason.INTEGRITY_MISMATCH, "ZIP entry '${entry.name}' was truncated")
        }
        return CopiedEntry(target, count, digest.digest().toHex())
    }

    private fun validateStructure(manifest: ZineLibraryBackupManifest, entries: List<ZineArchiveEntry>) {
        if (manifest.packageVersion > CURRENT_LIBRARY_BACKUP_VERSION) {
            throw ZineBackupStagingException(
                reason = ZineBackupStagingException.Reason.FUTURE_VERSION,
                message = "Backup package version ${manifest.packageVersion} is newer than $CURRENT_LIBRARY_BACKUP_VERSION",
                encounteredVersion = manifest.packageVersion,
                supportedVersion = CURRENT_LIBRARY_BACKUP_VERSION,
            )
        }
        manifest.projects.maxOfOrNull { it.documentSchemaVersion }
            ?.takeIf { it > CURRENT_SCHEMA_VERSION }
            ?.let { futureVersion ->
                throw ZineBackupStagingException(
                    reason = ZineBackupStagingException.Reason.FUTURE_VERSION,
                    message = "Backup document schema $futureVersion is newer than $CURRENT_SCHEMA_VERSION",
                    encounteredVersion = futureVersion,
                    supportedVersion = CURRENT_SCHEMA_VERSION,
                )
            }
        val validation = ZineLibraryBackupValidator().validate(manifest, entries)
        if (!validation.isValid) {
            fail(
                ZineBackupStagingException.Reason.INVALID_STRUCTURE,
                "Backup structure is invalid: ${validation.errors.joinToString { it.code }}",
            )
        }
    }

    private fun rawSchemaVersion(text: String, path: String): Int = try {
        manifestJson.parseToJsonElement(text).jsonObject["schemaVersion"]?.jsonPrimitive?.intOrNull
            ?: fail(ZineBackupStagingException.Reason.INVALID_DOCUMENT, "Document '$path' has no integer schemaVersion")
    } catch (known: ZineBackupStagingException) {
        throw known
    } catch (failure: Exception) {
        throw ZineBackupStagingException(
            ZineBackupStagingException.Reason.INVALID_DOCUMENT,
            "Document '$path' is not a JSON object",
            failure,
        )
    }

    private fun strictUtf8(bytes: ByteArray, label: String): String = try {
        bytes.decodeToString(throwOnInvalidSequence = true)
    } catch (failure: CharacterCodingException) {
        throw ZineBackupStagingException(
            if (label == "manifest") ZineBackupStagingException.Reason.INVALID_MANIFEST
            else ZineBackupStagingException.Reason.INVALID_DOCUMENT,
            "$label is not valid UTF-8",
            failure,
        )
    }

    private fun isSafeArchivePath(path: String): Boolean {
        if (path.isBlank() || path.startsWith('/') || '\\' in path || WINDOWS_DRIVE_PREFIX.matches(path)) return false
        return path.split('/').none { it.isBlank() || it == "." || it == ".." }
    }

    private fun preflightEntryLimit(path: String): Long = when {
        path == MANIFEST_PATH -> MAX_BACKUP_MANIFEST_BYTES
        DOCUMENT_PATH.matches(path) -> MAX_BACKUP_DOCUMENT_BYTES
        else -> MAX_BACKUP_ASSET_BYTES
    }

    private fun checkedAdd(left: Long, right: Long): Long = try {
        Math.addExact(left, right)
    } catch (_: ArithmeticException) {
        fail(ZineBackupStagingException.Reason.LIMIT_EXCEEDED, "Backup byte count overflowed")
    }

    private fun exceedsRatio(expanded: Long, compressed: Long): Boolean {
        if (compressed <= 0L) return expanded > 0L
        val quotient = expanded / compressed
        return quotient > limits.maximumExpansionRatio ||
            (quotient == limits.maximumExpansionRatio && expanded % compressed != 0L)
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { byte -> "%02x".format(byte) }

    private data class CopiedEntry(val path: Path, val byteCount: Long, val sha256: String)

    private companion object {
        const val STAGING_PREFIX: String = ".zine-restore-"
        val WINDOWS_DRIVE_PREFIX: Regex = Regex("^[A-Za-z]:.*")
        val DOCUMENT_PATH: Regex = Regex("^projects/[A-Za-z0-9_-]{1,64}/document\\.json$")

        fun fail(reason: ZineBackupStagingException.Reason, message: String): Nothing =
            throw ZineBackupStagingException(reason, message)
    }
}

private fun deleteTree(root: Path) {
    if (!Files.exists(root)) return
    try {
        Files.walk(root).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach { path ->
                try {
                    Files.deleteIfExists(path)
                } catch (_: IOException) {
                    // A failed cleanup never turns staged data into live data. A later janitor may retry.
                }
            }
        }
    } catch (_: IOException) {
        // Preserve the original staging failure/cancellation if even enumerating the tree fails.
    }
}
