package com.aritr.zinely.data.android

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.system.ErrnoException
import android.system.OsConstants
import com.aritr.zinely.core.data.asset.MAX_BACKUP_ARCHIVE_BYTES
import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.DataResult
import java.io.FilterInputStream
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

    /**
     * Builds the private archive, then copies it to [destination]. [latch] decides a Cancel that races
     * the end of the copy: once the provider has accepted the whole stream the backup is saved and a
     * later Cancel is a no-op.
     */
    public suspend fun backupTo(destination: Uri, latch: OutcomeLatch): LibraryBackupResult
}

/** Where a whole backup stopped (owner ruling F1, ADR-120). */
public enum class LibraryBackupStage {
    /** Zinely building, validating or cleaning up its private archive; the destination was never written. */
    PrivateArchive,

    /** Writing the completed archive to the maker's chosen destination. */
    Destination,
}

public sealed interface LibraryBackupResult {
    /** The provider accepted the whole archive. [fileName] only when the provider reports one. */
    public data class Saved(val receipt: LibraryBackupReceipt, val fileName: String?) : LibraryBackupResult

    /** No backup was saved; the maker's state is chosen from [stage] and [error]. */
    public data class Failed(val stage: LibraryBackupStage, val error: DataError) : LibraryBackupResult
}

internal interface SafStreams {
    fun openInput(uri: Uri): InputStream?
    fun openOutput(uri: Uri): OutputStream?

    /** The provider's display name, or null when it reports none. */
    fun displayName(uri: Uri): String?

    /** The provider's byte size, or null when it reports none. */
    fun size(uri: Uri): Long?

    /** Removes the document; `false` (or a throw) when the provider won't. */
    fun delete(uri: Uri): Boolean
}

internal class ContentResolverSafStreams(
    private val resolver: ContentResolver,
) : SafStreams {
    override fun openInput(uri: Uri): InputStream? = resolver.openInputStream(uri)
    override fun openOutput(uri: Uri): OutputStream? = resolver.openOutputStream(uri, "w")

    override fun displayName(uri: Uri): String? =
        queryColumn(uri, OpenableColumns.DISPLAY_NAME) { cursor, index -> cursor.getString(index) }
            ?.takeIf { it.isNotBlank() }

    override fun size(uri: Uri): Long? =
        queryColumn(uri, OpenableColumns.SIZE) { cursor, index -> cursor.getLong(index) }

    override fun delete(uri: Uri): Boolean = DocumentsContract.deleteDocument(resolver, uri)

    private fun <T> queryColumn(uri: Uri, column: String, read: (Cursor, Int) -> T): T? =
        resolver.query(uri, arrayOf(column), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(column)
            if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) read(cursor, index) else null
        }
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

    /**
     * Two phases (ADR-120): the repository builds a complete **private** archive, then it is copied to
     * [destination]. A failure is reported with the phase it happened in, so a failure inside Zinely is
     * never blamed on the maker's chosen location.
     *
     * Invariant: unless [LibraryBackupResult.Saved] is returned, the destination is discarded best-effort —
     * always once Zinely has opened it for writing (its old bytes are gone), otherwise only while it is still
     * empty (the picker's fresh file; a file the maker chose to replace is never deleted for a failure that
     * never touched it). The file is never deleted after [OutcomeLatch.markDone] wins.
     */
    override suspend fun backupTo(destination: Uri, latch: OutcomeLatch): LibraryBackupResult = withContext(io) {
        val archive = privateArchivePath()
        var opened = false
        var saved = false
        var input: InputStream? = null
        try {
            try {
                Files.createDirectories(transferRoot)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                return@withContext privateFailure(latch, privateWriteError("couldn't prepare a backup", failure))
            }
            val receipt = when (val created = backupRepository.createLibraryBackup(archive)) {
                is DataResult.Failure -> return@withContext privateFailure(latch, created.error)
                is DataResult.Success -> created.value
            }
            // Zinely's own archive is opened first, and its reads are tagged, so a failure to read it is
            // never blamed on the maker's location.
            val archiveInput = try {
                PrivateArchiveInput(Files.newInputStream(archive))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                return@withContext privateFailure(latch, DataError.Io("couldn't read the private backup", failure))
            }
            input = archiveInput
            val output = try {
                streams.openOutput(destination)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                return@withContext destinationFailure(latch, failure)
            } ?: return@withContext destinationFailure(latch, null)

            opened = true
            try {
                output.use { copyBounded(archiveInput, it) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (tooLarge: ArchiveTransferLimitException) {
                return@withContext privateFailure(latch, DataError.LimitExceeded("backup is larger than Zinely can save", tooLarge))
            } catch (unreadable: PrivateArchiveReadException) {
                return@withContext privateFailure(latch, DataError.Io("couldn't read the private backup", unreadable.cause))
            } catch (failure: Exception) {
                return@withContext destinationFailure(latch, failure)
            }
            // The provider has the whole stream. Whoever claims the latch first decides: a Cancel that
            // already won makes this an honest cancel (the file is discarded below).
            if (!latch.markDone()) throw CancellationException("backup cancelled before it completed")
            saved = true
            LibraryBackupResult.Saved(receipt, fileName = bestEffort { streams.displayName(destination) })
        } finally {
            bestEffort { input?.close() }
            if (!saved) discardDestination(destination, opened)
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

    // Every backup failure leaves through these two. Invariant (ADR-120): once a Cancel has won the latch,
    // whatever the work throws afterwards — a provider's ordinary IOException as the stream is torn down —
    // is the cancel's echo, never a failure of the backup, so it resolves as cancellation.
    private fun privateFailure(latch: OutcomeLatch, error: DataError): LibraryBackupResult {
        throwIfCancelled(latch)
        return LibraryBackupResult.Failed(LibraryBackupStage.PrivateArchive, error)
    }

    // The provider's free space can't be probed, so "no space" at the destination (F1: out of space
    // *anywhere*) is claimed only when the write itself says ENOSPC, never guessed.
    private fun destinationFailure(latch: OutcomeLatch, cause: Throwable?): LibraryBackupResult {
        throwIfCancelled(latch)
        return LibraryBackupResult.Failed(
            LibraryBackupStage.Destination,
            if (isOutOfSpace(cause)) {
                DataError.OutOfSpace("the chosen location is full", cause)
            } else {
                DataError.Io("couldn't save the backup to that location", cause)
            },
        )
    }

    private fun throwIfCancelled(latch: OutcomeLatch) {
        if (latch.isCancelled) throw CancellationException("backup cancelled before it completed")
    }

    private fun discardDestination(destination: Uri, opened: Boolean) {
        bestEffort {
            if (opened || streams.size(destination) == 0L) streams.delete(destination)
        }
    }

    // Provider metadata and cleanup are best-effort: they never replace the outcome the maker is shown.
    private inline fun <T> bestEffort(block: () -> T): T? = try {
        block()
    } catch (_: Exception) {
        null
    }

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

/**
 * A write failed because the device is full: `ENOSPC` anywhere in the cause chain — as a provider's
 * `ErrnoException`, its message, or java.nio's `FileSystemException` reason (bionic's `strerror`, not localized).
 */
internal fun isOutOfSpace(failure: Throwable?): Boolean = generateSequence(failure) { it.cause }
    .take(MAX_CAUSE_DEPTH)
    .any { cause ->
        val message = cause.message.orEmpty()
        "ENOSPC" in message || "No space left on device" in message ||
            (cause is ErrnoException && cause.errno == OsConstants.ENOSPC)
    }

/**
 * A backup's private-disk failure outside the writer (ADR-120 §3, F1 "genuine out of space"). No payload size is
 * known here, so there is no ADR-036 required-bytes comparison to make: only the failure's own `ENOSPC` proves the
 * disk is full. Low free space alone never turns an unrelated failure into "Not enough space".
 */
internal fun privateWriteError(message: String, failure: Throwable): DataError =
    if (isOutOfSpace(failure)) DataError.OutOfSpace(message, failure) else DataError.Io(message, failure)

private const val MAX_CAUSE_DEPTH = 8

private class PrivateArchiveReadException(cause: IOException) : IOException(cause)

/** Tags read failures on Zinely's own archive, so the copy can tell them from the provider's write failures. */
private class PrivateArchiveInput(input: InputStream) : FilterInputStream(input) {
    override fun read(b: ByteArray, off: Int, len: Int): Int = try {
        super.read(b, off, len)
    } catch (failure: IOException) {
        throw PrivateArchiveReadException(failure)
    }
}
