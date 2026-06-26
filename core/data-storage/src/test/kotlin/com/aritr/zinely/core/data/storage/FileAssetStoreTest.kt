package com.aritr.zinely.core.data.storage

import com.aritr.zinely.core.data.asset.ContentHasher
import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.DataResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Unit tests for the content-addressed [FileAssetStore] (ADR-022/ADR-031). Given-When-Then; a real
 * temp dir + [NioFileSystemOps] exercise the atomic temp→rename write with no Android.
 */
class FileAssetStoreTest {

    @TempDir
    lateinit var root: Path

    private fun store() = FileAssetStore(rootDir = root)

    private val bytes = "the import master bytes".toByteArray()
    private val expectedHash = ContentHasher.sha256(bytes)

    @Test
    fun `store content-addresses, persists the master at rootDir hex, and returns its hash`() = runTest {
        val result = store().store(bytes)

        assertEquals(expectedHash, (result as DataResult.Success).value)
        val blob = root.resolve(expectedHash.hex)
        assertTrue(Files.exists(blob), "blob must exist at rootDir/<hex>")
        assertArrayEquals(bytes, Files.readAllBytes(blob))
    }

    @Test
    fun `store is idempotent — re-storing identical bytes dedupes to one file and the same hash`() = runTest {
        val s = store()
        val first = s.store(bytes)
        val second = s.store(bytes)

        assertEquals((first as DataResult.Success).value, (second as DataResult.Success).value)
        // Exactly one blob (+ the .tmp dir) under root — no duplicate file for the same content.
        val blobs = Files.list(root).use { stream ->
            stream.filter { Files.isRegularFile(it) }.toList()
        }
        assertEquals(1, blobs.size, "identical content must not produce a second blob")
    }

    @Test
    fun `contains reflects presence`() = runTest {
        val s = store()
        assertFalse(s.contains(expectedHash))
        s.store(bytes)
        assertTrue(s.contains(expectedHash))
    }

    @Test
    fun `read returns the stored master bytes`() = runTest {
        val s = store()
        s.store(bytes)

        val read = s.read(expectedHash)

        assertArrayEquals(bytes, (read as DataResult.Success).value)
    }

    @Test
    fun `read of an absent hash is NotFound, never a crash`() = runTest {
        val read = store().read(expectedHash)

        val error = (read as DataResult.Failure).error
        assertTrue(error is DataError.NotFound)
        assertEquals(expectedHash.hex, (error as DataError.NotFound).id)
    }

    @Test
    fun `concurrent imports of identical bytes both succeed and dedupe to one blob`() = runTest {
        val s = store()

        val results = withContext(Dispatchers.Default) {
            (1..8).map { async { s.store(bytes) } }.awaitAll()
        }

        results.forEach { assertEquals(expectedHash, (it as DataResult.Success).value) }
        val blobs = Files.list(root).use { stream ->
            stream.filter { Files.isRegularFile(it) }.toList()
        }
        assertEquals(1, blobs.size, "concurrent identical imports must converge on one blob")
        assertArrayEquals(bytes, Files.readAllBytes(root.resolve(expectedHash.hex)))
    }

    @Test
    fun `a write failure returns Failure and leaves no temp residue`() = runTest {
        // A FileSystemOps whose atomicReplace fails closed (e.g. a backend that cannot atomically move).
        val failing = object : FileSystemOps {
            override val capabilities = FsCapabilities(atomicReplace = true, fileFsync = true, dirFsync = false)
            override fun fsyncFile(path: Path) = Unit
            override fun fsyncDir(dir: Path) = Unit
            override fun atomicReplace(source: Path, replacing: Path): Unit =
                throw IOException("atomic replace unsupported")
        }
        val s = FileAssetStore(rootDir = root, fs = failing)

        val result = s.store(bytes)

        assertTrue((result as DataResult.Failure).error is DataError.Io)
        assertFalse(Files.isRegularFile(root.resolve(expectedHash.hex)), "no blob on failure")
        val tmp = root.resolve(".tmp")
        if (Files.exists(tmp)) {
            val leftovers = Files.list(tmp).use { it.toList() }
            assertTrue(leftovers.isEmpty(), "temp must be cleaned up on failure, found $leftovers")
        }
    }

    @Test
    fun `a directory squatting at the blob path is not mistaken for a stored master`() = runTest {
        // Codex RF1: a non-regular node at assets/<hex> must not count as a blob.
        Files.createDirectories(root.resolve(expectedHash.hex))
        val s = store()

        assertFalse(s.contains(expectedHash), "a directory must not satisfy contains()")
        assertTrue(s.read(expectedHash).let { it is DataResult.Failure && it.error is DataError.NotFound })
    }

    @Test
    fun `store leaves no temp residue and the temp dir never masquerades as a blob`() = runTest {
        store().store(bytes)

        // No leftover temp files; the only regular file under root is the valid-hex blob.
        val tmp = root.resolve(".tmp")
        if (Files.exists(tmp)) {
            val leftovers = Files.list(tmp).use { it.toList() }
            assertTrue(leftovers.isEmpty(), "no temp residue expected, found $leftovers")
        }
        val rootFiles = Files.list(root).use { stream ->
            stream.filter { Files.isRegularFile(it) }.map { it.fileName.toString() }.toList()
        }
        assertEquals(listOf(expectedHash.hex), rootFiles)
    }
}
