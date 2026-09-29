package com.aritr.zinely.data.android

import android.net.Uri
import com.aritr.zinely.core.data.asset.ZineBackupOmission
import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.DataResult
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LibrarySafTransportTest {
    @get:Rule val temporary = TemporaryFolder()

    private lateinit var root: Path
    private val uri = Uri.parse("content://zinely.test/library.zine")

    @Before fun setUp() {
        root = temporary.root.toPath().resolve("transfers")
    }

    @Test fun `restore streams provider bytes to repository and removes private archive`() = runTest {
        val expected = ByteArray(256 * 1024) { (it % 251).toByte() }
        var restoredBytes: ByteArray? = null
        val transport = transport(
            streams = streams(input = { ByteArrayInputStream(expected) }),
            restore = { archive ->
                restoredBytes = Files.readAllBytes(archive)
                DataResult.Success(LibraryRestoreReceipt(emptyList()))
            },
        )

        val result = transport.restoreFrom(uri)

        assertTrue(result is DataResult.Success)
        assertArrayEquals(expected, restoredBytes)
        assertTransferRootClean()
    }

    @Test fun `restore refuses a provider stream beyond the transfer limit without repository writes`() = runTest {
        var restoreCalls = 0
        val transport = transport(
            streams = streams(input = { ByteArrayInputStream(ByteArray(33)) }),
            restore = {
                restoreCalls++
                DataResult.Success(LibraryRestoreReceipt(emptyList()))
            },
            maximumBytes = 32,
        )

        val result = transport.restoreFrom(uri)

        assertTrue((result as DataResult.Failure).error is DataError.Corrupt)
        assertEquals(0, restoreCalls)
        assertTransferRootClean()
    }

    @Test fun `restore maps unavailable and failing providers without repository writes`() = runTest {
        listOf<SafStreams>(
            streams(input = { null }),
            streams(input = { throw SecurityException("denied") }),
            streams(input = { object : InputStream() { override fun read(): Int = throw IOException("gone") } }),
        ).forEach { provider ->
            var restoreCalls = 0
            val result = transport(provider, restore = {
                restoreCalls++
                DataResult.Success(LibraryRestoreReceipt(emptyList()))
            }).restoreFrom(uri)
            assertTrue((result as DataResult.Failure).error is DataError.Io)
            assertEquals(0, restoreCalls)
            assertTransferRootClean()
        }
    }

    @Test fun `private transfer directory failure is contained before provider or repository access`() = runTest {
        Files.write(root, byteArrayOf(1))
        var providerCalls = 0
        var restoreCalls = 0
        var backupCalls = 0
        val transport = transport(
            streams = streams(
                input = {
                    providerCalls++
                    ByteArrayInputStream(byteArrayOf(1))
                },
                output = {
                    providerCalls++
                    ByteArrayOutputStream()
                },
            ),
            restore = {
                restoreCalls++
                DataResult.Success(LibraryRestoreReceipt(emptyList()))
            },
            backup = {
                backupCalls++
                DataResult.Success(LibraryBackupReceipt(0, 0, 0))
            },
        )

        val restoreResult = transport.restoreFrom(uri)
        val backupResult = transport.backupTo(uri, OutcomeLatch())

        assertTrue((restoreResult as DataResult.Failure).error is DataError.Io)
        assertEquals(LibraryBackupStage.PrivateArchive, (backupResult as LibraryBackupResult.Failed).stage)
        assertTrue(backupResult.error is DataError.Io)
        assertEquals(0, providerCalls)
        assertEquals(0, restoreCalls)
        assertEquals(0, backupCalls)
        assertTrue(Files.isRegularFile(root))
    }

    @Test fun `restore preserves repository error classification and cleans temp`() = runTest {
        val expected = DataError.Busy("editor is open")
        val result = transport(
            streams = streams(input = { ByteArrayInputStream(byteArrayOf(1)) }),
            restore = { DataResult.Failure(expected) },
        ).restoreFrom(uri)

        assertEquals(expected, (result as DataResult.Failure).error)
        assertTransferRootClean()
    }

    @Test fun `restore cancellation propagates and cleans temp`() = runTest {
        val cancelling = object : InputStream() {
            override fun read(): Int = throw CancellationException("cancelled")
        }
        val transport = transport(streams(input = { cancelling }))

        var cancelled = false
        try {
            transport.restoreFrom(uri)
        } catch (_: CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
        assertTransferRootClean()
    }

    @Test fun `backup writes the complete private archive to the provider and cleans temp`() = runTest {
        val expected = ByteArray(512 * 1024) { (it % 239).toByte() }
        val destination = ByteArrayOutputStream()
        val receipt = LibraryBackupReceipt(3, 2, expected.size.toLong())
        val result = transport(
            streams = streams(output = { destination }),
            backup = { path ->
                Files.write(path, expected)
                DataResult.Success(receipt)
            },
        ).backupTo(uri, OutcomeLatch())

        assertEquals(LibraryBackupResult.Saved(receipt, fileName = null), result)
        assertArrayEquals(expected, destination.toByteArray())
        assertTransferRootClean()
    }

    @Test fun `backup provider failure is contained after private archive creation`() = runTest {
        val result = transport(
            streams = streams(output = { throw IOException("provider failed") }),
            backup = { path ->
                Files.write(path, byteArrayOf(1, 2, 3))
                DataResult.Success(LibraryBackupReceipt(1, 0, 3))
            },
        ).backupTo(uri, OutcomeLatch())

        assertEquals(LibraryBackupStage.Destination, (result as LibraryBackupResult.Failed).stage)
        assertTrue(result.error is DataError.Io)
        assertTransferRootClean()
    }

    @Test fun `backup cancellation propagates and removes private archive`() = runTest {
        val cancelling = object : OutputStream() {
            override fun write(value: Int): Unit = throw CancellationException("cancelled")
        }
        val transport = transport(
            streams = streams(output = { cancelling }),
            backup = { path ->
                Files.write(path, byteArrayOf(1, 2, 3))
                DataResult.Success(LibraryBackupReceipt(1, 0, 3))
            },
        )

        var cancelled = false
        try {
            transport.backupTo(uri, OutcomeLatch())
        } catch (_: CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
        assertTransferRootClean()
    }

    // --- 1.x step 1 (ADR-120): two phases, honest clean-up, the late Cancel ---

    @Test fun `a saved backup keeps its file and reports the provider's name`() = runTest {
        val provider = streams(output = { ByteArrayOutputStream() }, displayName = "zinely-backup-2026-09-12.zine")

        val result = transport(provider, backup = completeArchive()).backupTo(uri, OutcomeLatch())

        assertEquals("zinely-backup-2026-09-12.zine", (result as LibraryBackupResult.Saved).fileName)
        assertEquals(0, provider.deletes)
        assertTransferRootClean()
    }

    @Test fun `a provider that reports no name saves with no name`() = runTest {
        val provider = streams(output = { ByteArrayOutputStream() }, displayName = null)

        val result = transport(provider, backup = completeArchive()).backupTo(uri, OutcomeLatch())

        assertEquals(null, (result as LibraryBackupResult.Saved).fileName)
    }

    @Test fun `a private-archive failure keeps its error, is never the destination, and removes the empty file`() = runTest {
        val expected = DataError.Io("couldn't read project 'a' for backup")
        var opens = 0
        val provider = streams(output = { opens++; ByteArrayOutputStream() }, size = 0L)

        val result = transport(provider, backup = { DataResult.Failure(expected) }).backupTo(uri, OutcomeLatch())

        assertEquals(LibraryBackupResult.Failed(LibraryBackupStage.PrivateArchive, expected), result)
        assertEquals("the destination is never opened for a private failure", 0, opens)
        assertEquals("the picker's empty file is removed", 1, provider.deletes)
        assertTransferRootClean()
    }

    @Test fun `a private-archive failure never deletes a destination that already had bytes`() = runTest {
        // The maker may have chosen to replace an older backup; nothing of it was touched, so nothing goes.
        val provider = streams(size = 4096L)

        transport(provider, backup = { DataResult.Failure(DataError.Io("x")) }).backupTo(uri, OutcomeLatch())

        assertEquals(0, provider.deletes)
    }

    @Test fun `a destination that can't be opened is the destination's failure`() = runTest {
        val provider = streams(output = { null }, size = 0L)

        val result = transport(provider, backup = completeArchive()).backupTo(uri, OutcomeLatch())

        assertEquals(LibraryBackupStage.Destination, (result as LibraryBackupResult.Failed).stage)
        assertEquals(1, provider.deletes)
        assertTransferRootClean()
    }

    @Test fun `a write that fails part-way deletes the partial file`() = runTest {
        val failing = object : OutputStream() {
            override fun write(value: Int): Unit = throw IOException("provider went away")
        }
        // A size the provider still reports as non-zero: once Zinely has written, the file goes anyway.
        val provider = streams(output = { failing }, size = 1L)

        val result = transport(provider, backup = completeArchive()).backupTo(uri, OutcomeLatch())

        assertEquals(LibraryBackupStage.Destination, (result as LibraryBackupResult.Failed).stage)
        assertEquals(1, provider.deletes)
        assertTransferRootClean()
    }

    @Test fun `a full destination is not enough space, not the location's fault`() = runTest {
        // How ExternalStorageProvider reports a full disk; the errno name is fixed, not localized.
        val full = object : OutputStream() {
            override fun write(value: Int): Unit =
                throw IOException("write failed", IOException("write failed: ENOSPC (No space left on device)"))
        }
        val provider = streams(output = { full }, size = 1L)

        val result = transport(provider, backup = completeArchive()).backupTo(uri, OutcomeLatch())

        assertEquals(LibraryBackupStage.Destination, (result as LibraryBackupResult.Failed).stage)
        assertTrue(result.error is DataError.OutOfSpace)
        assertEquals(1, provider.deletes)
        assertTransferRootClean()
    }

    @Test fun `a private archive Zinely can't read back is never blamed on the location`() = runTest {
        var opens = 0
        val provider = streams(output = { opens++; ByteArrayOutputStream() }, size = 0L)
        // The repository reports success but the archive isn't there to read.
        val missing: suspend (Path) -> DataResult<LibraryBackupReceipt> = { DataResult.Success(LibraryBackupReceipt(1, 0, 3)) }

        val result = transport(provider, backup = missing).backupTo(uri, OutcomeLatch())

        assertEquals(LibraryBackupStage.PrivateArchive, (result as LibraryBackupResult.Failed).stage)
        assertEquals("the destination is never opened", 0, opens)
        assertTransferRootClean()
    }

    @Test fun `a failing clean-up never changes the failure the maker sees`() = runTest {
        val expected = DataError.Io("private archive")
        val provider = streams(size = 0L, delete = { throw SecurityException("provider refuses") })

        val result = transport(provider, backup = { DataResult.Failure(expected) }).backupTo(uri, OutcomeLatch())

        assertEquals(LibraryBackupResult.Failed(LibraryBackupStage.PrivateArchive, expected), result)
        assertEquals(1, provider.deletes)
        assertTransferRootClean()
    }

    @Test fun `an archive over the transfer limit is a deterministic limit, not the destination`() = runTest {
        val provider = streams(output = { ByteArrayOutputStream() })

        val result = transport(provider, backup = completeArchive(ByteArray(64)), maximumBytes = 32)
            .backupTo(uri, OutcomeLatch())

        assertEquals(LibraryBackupStage.PrivateArchive, (result as LibraryBackupResult.Failed).stage)
        assertTrue(result.error is DataError.LimitExceeded)
        assertEquals(1, provider.deletes)
    }

    @Test fun `a Cancel during the copy deletes the partial file and propagates`() = runTest {
        val cancelling = object : OutputStream() {
            override fun write(value: Int): Unit = throw CancellationException("cancelled")
        }
        val provider = streams(output = { cancelling }, size = 1L)

        var cancelled = false
        try {
            transport(provider, backup = completeArchive()).backupTo(uri, OutcomeLatch())
        } catch (_: CancellationException) {
            cancelled = true
        }

        assertTrue(cancelled)
        assertEquals(1, provider.deletes)
        assertTransferRootClean()
    }

    @Test fun `a Cancel that won the latch before the close is an honest cancel even with every byte written`() = runTest {
        val latch = OutcomeLatch()
        var cancelWon = false
        val sink = object : ByteArrayOutputStream() {
            override fun close() {
                super.close()
                cancelWon = cancelWon || latch.requestCancel() // the stream may be closed twice
            }
        }
        val provider = streams(output = { sink }, size = 3L)

        var cancelled = false
        try {
            transport(provider, backup = completeArchive()).backupTo(uri, latch)
        } catch (_: CancellationException) {
            cancelled = true
        }

        assertTrue("Cancel claimed the latch while the stream closed", cancelWon)
        assertTrue(cancelled)
        assertEquals("a cancelled backup's file is not left looking like a backup", 1, provider.deletes)
    }

    @Test fun `a Cancel that won the latch is not turned into a destination failure by a late IOException`() = runTest {
        // Cancel wins, then the provider's stream fails with an ordinary IOException as it is torn down.
        val latch = OutcomeLatch()
        val sink = object : OutputStream() {
            override fun write(value: Int) {
                assertTrue("Cancel wins the latch before the provider fails", latch.requestCancel())
                throw IOException("provider stream closed under us")
            }
        }
        val provider = streams(output = { sink }, size = 1L)

        val outcome = try {
            transport(provider, backup = completeArchive()).backupTo(uri, latch)
        } catch (cancelled: CancellationException) {
            cancelled
        }

        assertTrue("resolves as cancellation, not $outcome", outcome is CancellationException)
        assertEquals("the partial file is removed", 1, provider.deletes)
        assertTransferRootClean()
    }

    @Test fun `a Cancel that won the latch is not turned into a private failure either`() = runTest {
        val latch = OutcomeLatch()
        val provider = streams(size = 0L)
        val failsAfterCancel: suspend (Path) -> DataResult<LibraryBackupReceipt> = {
            latch.requestCancel()
            DataResult.Failure(DataError.Io("writer interrupted"))
        }

        val outcome = try {
            transport(provider, backup = failsAfterCancel).backupTo(uri, latch)
        } catch (cancelled: CancellationException) {
            cancelled
        }

        assertTrue("resolves as cancellation, not $outcome", outcome is CancellationException)
        assertEquals("the picker's empty file is removed", 1, provider.deletes)
        assertTransferRootClean()
    }

    @Test fun `without a Cancel the same late IOException stays the destination's failure`() = runTest {
        val latch = OutcomeLatch()
        val sink = object : OutputStream() {
            override fun write(value: Int): Unit = throw IOException("provider stream closed under us")
        }

        val result = transport(streams(output = { sink }, size = 1L), backup = completeArchive()).backupTo(uri, latch)

        assertEquals(LibraryBackupStage.Destination, (result as LibraryBackupResult.Failed).stage)
        assertTrue("the latch is still open for the view model", latch.requestCancel())
    }

    @Test fun `once the file is complete a later Cancel loses the latch and the file stays`() = runTest {
        val latch = OutcomeLatch()
        val provider = streams(output = { ByteArrayOutputStream() })

        val result = transport(provider, backup = completeArchive()).backupTo(uri, latch)

        assertTrue(result is LibraryBackupResult.Saved)
        assertFalse("a Cancel after completion is a no-op", latch.requestCancel())
        assertEquals(0, provider.deletes)
    }

    @Test fun `restore hands the commit-start hook to the repository`() = runTest {
        var handed: (() -> Boolean)? = null
        val hook = { false }
        val transport = transport(
            streams = streams(input = { ByteArrayInputStream(byteArrayOf(1)) }),
            onRestoreCommitStart = { handed = it },
        )

        transport.restoreFrom(uri, hook)

        assertTrue(handed === hook)
    }

    @Test fun `a backup that saved no zine is nothing saved - the provider is never opened and the empty file goes`() = runTest {
        val receipt = LibraryBackupReceipt(
            projectCount = 0,
            assetCount = 0,
            archiveByteCount = 0,
            totalCount = 2,
            omitted = listOf(ZineBackupOmission("A", ZineBackupOmission.PHOTO), ZineBackupOmission(null, ZineBackupOmission.PHOTO)),
        )
        var opened = 0
        val streams = streams(output = { opened++; ByteArrayOutputStream() }, size = 0L)
        val transport = transport(streams = streams, backup = { DataResult.Success(receipt) })

        val result = transport.backupTo(uri, OutcomeLatch())

        assertEquals(LibraryBackupResult.NothingSaved(receipt), result)
        assertEquals(0, opened)
        assertEquals(1, streams.deletes)
        assertTransferRootClean()
    }

    @Test fun `nothing saved never deletes a destination that already had bytes`() = runTest {
        val streams = streams(size = 12L)
        val transport = transport(streams = streams, backup = { DataResult.Success(LibraryBackupReceipt(0, 0, 0, totalCount = 1)) })

        transport.backupTo(uri, OutcomeLatch())

        assertEquals(0, streams.deletes)
    }

    private fun transport(
        streams: SafStreams = streams(),
        restore: suspend (Path) -> DataResult<LibraryRestoreReceipt> = {
            DataResult.Success(LibraryRestoreReceipt(emptyList()))
        },
        onRestoreCommitStart: (() -> Boolean) -> Unit = {},
        backup: suspend (Path) -> DataResult<LibraryBackupReceipt> = {
            DataResult.Failure(DataError.Io("unused"))
        },
        maximumBytes: Long = 1024 * 1024,
    ): LibrarySafTransport = ContentResolverLibrarySafTransport(
        transferRoot = root,
        streams = streams,
        restoreRepository = object : LibraryRestoreRepository {
            override suspend fun restoreLibrary(
                archive: Path,
                onCommitStart: () -> Boolean,
            ): DataResult<LibraryRestoreReceipt> {
                onRestoreCommitStart(onCommitStart)
                return restore(archive)
            }
        },
        backupRepository = object : LibraryBackupRepository {
            override suspend fun createLibraryBackup(destination: Path): DataResult<LibraryBackupReceipt> = backup(destination)
        },
        io = Dispatchers.Unconfined,
        maximumArchiveBytes = maximumBytes,
    )

    private fun streams(
        input: () -> InputStream? = { null },
        output: () -> OutputStream? = { null },
        displayName: String? = null,
        size: Long? = 0L,
        delete: () -> Boolean = { true },
    ): RecordingStreams = RecordingStreams(input, output, displayName, size, delete)

    private class RecordingStreams(
        private val input: () -> InputStream?,
        private val output: () -> OutputStream?,
        private val name: String?,
        private val reportedSize: Long?,
        private val onDelete: () -> Boolean,
    ) : SafStreams {
        var deletes = 0

        override fun openInput(uri: Uri): InputStream? = input()
        override fun openOutput(uri: Uri): OutputStream? = output()
        override fun displayName(uri: Uri): String? = name
        override fun size(uri: Uri): Long? = reportedSize
        override fun delete(uri: Uri): Boolean {
            deletes++
            return onDelete()
        }
    }

    private fun completeArchive(bytes: ByteArray = byteArrayOf(1, 2, 3)): suspend (Path) -> DataResult<LibraryBackupReceipt> =
        { path ->
            Files.write(path, bytes)
            DataResult.Success(LibraryBackupReceipt(1, 0, bytes.size.toLong()))
        }

    private fun assertTransferRootClean() {
        if (!Files.exists(root)) return
        Files.newDirectoryStream(root).use { assertFalse(it.iterator().hasNext()) }
    }
}
