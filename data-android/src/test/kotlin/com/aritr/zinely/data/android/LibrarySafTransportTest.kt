package com.aritr.zinely.data.android

import android.net.Uri
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
        val backupResult = transport.backupTo(uri)

        assertTrue((restoreResult as DataResult.Failure).error is DataError.Io)
        assertTrue((backupResult as DataResult.Failure).error is DataError.Io)
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
        ).backupTo(uri)

        assertEquals(DataResult.Success(receipt), result)
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
        ).backupTo(uri)

        assertTrue((result as DataResult.Failure).error is DataError.Io)
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
            transport.backupTo(uri)
        } catch (_: CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
        assertTransferRootClean()
    }

    private fun transport(
        streams: SafStreams = streams(),
        restore: suspend (Path) -> DataResult<LibraryRestoreReceipt> = {
            DataResult.Success(LibraryRestoreReceipt(emptyList()))
        },
        backup: suspend (Path) -> DataResult<LibraryBackupReceipt> = {
            DataResult.Failure(DataError.Io("unused"))
        },
        maximumBytes: Long = 1024 * 1024,
    ): LibrarySafTransport = ContentResolverLibrarySafTransport(
        transferRoot = root,
        streams = streams,
        restoreRepository = object : LibraryRestoreRepository {
            override suspend fun restoreLibrary(archive: Path): DataResult<LibraryRestoreReceipt> = restore(archive)
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
    ): SafStreams = object : SafStreams {
        override fun openInput(uri: Uri): InputStream? = input()
        override fun openOutput(uri: Uri): OutputStream? = output()
    }

    private fun assertTransferRootClean() {
        if (!Files.exists(root)) return
        Files.newDirectoryStream(root).use { assertFalse(it.iterator().hasNext()) }
    }
}
