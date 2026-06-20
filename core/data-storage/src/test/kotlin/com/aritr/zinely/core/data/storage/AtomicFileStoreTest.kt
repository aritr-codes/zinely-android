package com.aritr.zinely.core.data.storage

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * Durability contract for the atomic document file source (ADR-021): a save is all-or-nothing,
 * and the prior good file always survives a failure. All pure java.nio — runs on JVM temp dirs.
 */
class AtomicFileStoreTest {

    private val store = AtomicFileStore()

    @Test
    fun `write then read returns the written bytes`(@TempDir dir: Path) {
        val target = dir.resolve("doc.json")

        store.write(target, "hello zine".toByteArray())

        assertArrayEquals("hello zine".toByteArray(), store.read(target))
    }

    @Test
    fun `read of a missing file returns null`(@TempDir dir: Path) {
        assertNull(store.read(dir.resolve("absent.json")))
    }

    @Test
    fun `write replaces existing content`(@TempDir dir: Path) {
        val target = dir.resolve("doc.json")

        store.write(target, "v1".toByteArray())
        store.write(target, "v2".toByteArray())

        assertArrayEquals("v2".toByteArray(), store.read(target))
    }

    @Test
    fun `a failure during the atomic replace leaves the prior good file intact`(@TempDir dir: Path) {
        val target = dir.resolve("doc.json")
        store.write(target, "good v1".toByteArray())

        // Inject a filesystem that throws exactly when the new temp is renamed over the target.
        val failingReplace = object : FileSystemOps by NioFileSystemOps {
            override fun atomicReplace(source: Path, replacing: Path) {
                if (replacing == target) error("simulated crash mid-rename")
            }
        }
        val brittle = AtomicFileStore(failingReplace)

        runCatching { brittle.write(target, "torn v2".toByteArray()) }

        // The old document must still be fully readable — never lost, never half-written.
        assertArrayEquals("good v1".toByteArray(), AtomicFileStore().read(target))
        // No stray temp file left lying around in the directory.
        val strays = Files.list(dir).use { stream -> stream.map { it.fileName.toString() }.toList() }
        assert(strays.none { it.endsWith(".tmp") }) { "leftover temp files: $strays" }
    }
}
