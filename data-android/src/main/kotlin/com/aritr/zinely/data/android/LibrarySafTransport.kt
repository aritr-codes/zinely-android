package com.aritr.zinely.data.android

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.aritr.zinely.core.data.asset.MAX_BACKUP_ARCHIVE_BYTES
import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.DataResult
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Thin Android transport boundary between user-owned SAF documents and trusted private storage. */
public interface LibrarySafTransport {
    public suspend fun restoreFrom(source: Uri): DataResult<LibraryRestoreReceipt>
    public suspend fun backupTo(destination: Uri): DataResult<LibraryBackupReceipt>
}

internal interface SafStreams {
    fun openInput(uri: Uri): InputStream?
    fun openOutput(uri: Uri): OutputStream?
}

internal class ContentResolverSafStreams(
    private val resolver: ContentResolver,
) : SafStreams {
    override fun openInput(uri: Uri): InputStream? = resolver.openInputStream(uri)
    override fun openOutput(uri: Uri): OutputStream? = resolver.openOutputStream(uri, "w")
}

/**
 * Copies provider streams in bounded chunks. ZIP validation and library transactions remain in the
 * trusted restore repository; SAF never writes an authoritative project path directly.
 */
internal class ContentResolverLibrarySafTransport(
    private val transferRoot: Path,
    private val streams: SafStreams,
    private val restoreRepository: LibraryRestoreRepository,
    private val backupRepository: LibraryBackupRepository,
    private val io: CoroutineDispatcher,
    private val maximumArchiveBytes: Long = MAX_BACKUP_ARCHIVE_BYTES,
) : LibrarySafTransport {

    constructor(
        context: Context,
        restoreRepository: LibraryRestoreRepository,
        backupRepository: LibraryBackupRepository,
        io: CoroutineDispatcher,
    ) : this(
        transferRoot = context.cacheDir.toPath().resolve(TRANSFER_DIRECTORY),
        streams = ContentResolverSafStreams(context.contentResolver),
        restoreRepository = restoreRepository,
        backupRepository = backupRepository,
        io = io,
    )

    override suspend fun restoreFrom(source: Uri): DataResult<LibraryRestoreReceipt> = withContext(io) {
        val archive = privateArchivePath()
        try {
            Files.createDirectories(transferRoot)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            return@withContext classifyPrivateWriteFailure(archive, "couldn't prepare this backup", failure)
        }
        try {
            val input = try {
                streams.openInput(source)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                return@withContext readFailure(failure)
            } ?: return@withContext readFailure(null)

            try {
                input.use { sourceStream ->
                    copyBounded(sourceStream, Files.newOutputStream(archive))
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (tooLarge: ArchiveTransferLimitException) {
                return@withContext DataResult.Failure(DataError.Corrupt("backup is larger than Zinely can restore", tooLarge))
            } catch (failure: Exception) {
                return@withContext classifyPrivateWriteFailure(archive, "couldn't copy this backup", failure)
            }
            restoreRepository.restoreLibrary(archive)
        } finally {
            deletePrivateArchive(archive)
            deleteTransferRootIfEmpty()
        }
    }

    override suspend fun backupTo(destination: Uri): DataResult<LibraryBackupReceipt> = withContext(io) {
        val archive = privateArchivePath()
        try {
            Files.createDirectories(transferRoot)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            return@withContext classifyPrivateWriteFailure(archive, "couldn't prepare a backup", failure)
        }
        try {
            val receipt = when (val created = backupRepository.createLibraryBackup(archive)) {
                is DataResult.Failure -> return@withContext created
                is DataResult.Success -> created.value
            }
            val output = try {
                streams.openOutput(destination)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                return@withContext writeFailure(failure)
            } ?: return@withContext writeFailure(null)

            try {
                output.use { copyBounded(Files.newInputStream(archive), it) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                return@withContext writeFailure(failure)
            }
            DataResult.Success(receipt)
        } finally {
            deletePrivateArchive(archive)
            deleteTransferRootIfEmpty()
        }
    }

    private fun privateArchivePath(): Path = transferRoot.resolve("${UUID.randomUUID()}.zine")

    private suspend fun copyBounded(input: InputStream, output: OutputStream): Long {
        input.use { source ->
            output.use { sink ->
                val buffer = ByteArray(COPY_BUFFER_BYTES)
                var total = 0L
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = source.read(buffer)
                    if (read < 0) break
                    if (read == 0) continue
                    total = Math.addExact(total, read.toLong())
                    if (total > maximumArchiveBytes) throw ArchiveTransferLimitException()
                    sink.write(buffer, 0, read)
                }
                sink.flush()
                return total
            }
        }
    }

    private fun readFailure(cause: Throwable?): DataResult.Failure =
        DataResult.Failure(DataError.Io("couldn't read this backup", cause))

    private fun writeFailure(cause: Throwable?): DataResult.Failure =
        DataResult.Failure(DataError.Io("couldn't save the backup to that location", cause))

    private fun classifyPrivateWriteFailure(path: Path, message: String, cause: Throwable): DataResult.Failure {
        val usable = try {
            Files.getFileStore(path.parent).usableSpace
        } catch (_: Exception) {
            Long.MAX_VALUE
        }
        val error = if (usable < COPY_BUFFER_BYTES) DataError.OutOfSpace(message, cause) else DataError.Io(message, cause)
        return DataResult.Failure(error)
    }

    private fun deleteTransferRootIfEmpty() {
        try {
            Files.newDirectoryStream(transferRoot).use { entries ->
                if (!entries.iterator().hasNext()) Files.deleteIfExists(transferRoot)
            }
        } catch (_: IOException) {
            // No live data is present here. A later transfer can safely reuse and clean this folder.
        }
    }

    private fun deletePrivateArchive(path: Path) {
        try {
            Files.deleteIfExists(path)
        } catch (_: IOException) {
            // Private transfer residue is never authoritative and can be retried by a later janitor.
        }
    }

    private class ArchiveTransferLimitException : IOException("archive transfer limit exceeded")

    private companion object {
        const val TRANSFER_DIRECTORY: String = "zine-transfers"
        const val COPY_BUFFER_BYTES: Int = 64 * 1024
    }
}
