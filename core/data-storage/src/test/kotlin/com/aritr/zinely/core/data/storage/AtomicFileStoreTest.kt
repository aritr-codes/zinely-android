package com.aritr.zinely.core.data.storage

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
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

    @Test
    fun `a successful overwrite keeps a backup of the prior version`(@TempDir dir: Path) {
        val target = dir.resolve("doc.json")

        store.write(target, "v1".toByteArray())
        store.write(target, "v2".toByteArray())

        assertArrayEquals("v1".toByteArray(), Files.readAllBytes(dir.resolve("doc.json.bak")))
    }

    @Test
    fun `open-time recovery falls back to the backup when the target is missing`(@TempDir dir: Path) {
        val target = dir.resolve("doc.json")
        store.write(target, "v1".toByteArray())
        store.write(target, "v2".toByteArray()) // .bak now holds v1
        // Simulate an interrupted rename / power loss that left the target gone but the backup good.
        Files.delete(target)

        assertArrayEquals("v1".toByteArray(), store.read(target))
    }

    @Test
    fun `open-time recovery falls back to the backup when the target is corrupt`(@TempDir dir: Path) {
        val target = dir.resolve("doc.json")
        store.write(target, "good".toByteArray())
        store.write(target, "good again".toByteArray()) // .bak now holds "good"
        // A torn/foreign target the caller's integrity check will reject.
        Files.write(target, "tornXX".toByteArray())

        // JSON/schema validity lives above the byte store: the repository passes the integrity check.
        val recovered = store.read(target) { bytes -> bytes.decodeToString().endsWith("good") }

        assertArrayEquals("good".toByteArray(), recovered)
    }

    @Test
    fun `open-time recovery returns null when both target and backup are unusable`(@TempDir dir: Path) {
        val target = dir.resolve("doc.json")
        store.write(target, "v1".toByteArray())
        store.write(target, "v2".toByteArray()) // target=v2, .bak=v1

        // Neither candidate passes the integrity predicate — corruption on both sides.
        assertNull(store.read(target) { false })
    }

    @Test
    fun `construction rejects a backend that cannot atomically replace`() {
        val noAtomicReplace = object : FileSystemOps by NioFileSystemOps {
            override val capabilities = NioFileSystemOps.capabilities.copy(atomicReplace = false)
        }

        assertThrows(IllegalArgumentException::class.java) { AtomicFileStore(noAtomicReplace) }
    }

    @Test
    fun `construction rejects a backend that cannot fsync files`() {
        val noFileFsync = object : FileSystemOps by NioFileSystemOps {
            override val capabilities = NioFileSystemOps.capabilities.copy(fileFsync = false)
        }

        assertThrows(IllegalArgumentException::class.java) { AtomicFileStore(noFileFsync) }
    }

    @Test
    fun `a backend with best-effort directory fsync is still accepted`() {
        // NioFileSystemOps reports dirFsync=false by design; durability-sensitive writes must still
        // construct (the :data-android adapter supplies the real Os.fsync) — only atomicReplace and
        // fileFsync are hard requirements.
        AtomicFileStore(NioFileSystemOps)
    }

    @Test
    fun `recovery repairs the primary so a later failed write cannot destroy the last good copy`(
        @TempDir dir: Path,
    ) {
        val target = dir.resolve("doc.json")
        val isGood = { bytes: ByteArray -> bytes.decodeToString().startsWith("good") }
        store.write(target, "good".toByteArray())
        store.write(target, "good two".toByteArray()) // .bak now holds "good"
        // External/power-loss corruption leaves a readable-but-invalid primary, good backup behind.
        Files.write(target, "corruptXX".toByteArray())

        // Open-time recovery returns the good backup AND repairs the primary on disk.
        assertArrayEquals("good".toByteArray(), store.read(target, isGood))

        // A subsequent save that fails mid-rename must still leave the good content recoverable —
        // it would not if the primary were still corrupt (the snapshot would overwrite the good .bak).
        val failingReplace = object : FileSystemOps by NioFileSystemOps {
            override fun atomicReplace(source: Path, replacing: Path) {
                if (replacing == target) error("simulated crash mid-rename")
            }
        }
        runCatching { AtomicFileStore(failingReplace).write(target, "good three".toByteArray()) }

        assertArrayEquals("good".toByteArray(), store.read(target, isGood))
    }

    @Test
    fun `recovery quarantines the corrupt primary when the repair write itself fails`(
        @TempDir dir: Path,
    ) {
        val target = dir.resolve("doc.json")
        val isGood = { bytes: ByteArray -> bytes.decodeToString().startsWith("good") }
        store.write(target, "good".toByteArray())
        store.write(target, "good two".toByteArray()) // .bak now holds "good"
        Files.write(target, "corruptXX".toByteArray())

        // A backend whose repair write (atomicReplace onto target) fails with an IOException.
        val repairFails = object : FileSystemOps by NioFileSystemOps {
            override fun atomicReplace(source: Path, replacing: Path) {
                if (replacing == target) throw IOException("repair write fails")
                NioFileSystemOps.atomicReplace(source, replacing)
            }
        }

        val recovered = AtomicFileStore(repairFails).read(target, isGood)

        assertArrayEquals("good".toByteArray(), recovered)
        // The corrupt primary must be gone so the next write cannot snapshot it over the good backup.
        assertFalse(Files.exists(target)) { "corrupt primary was not quarantined" }
        // A normal subsequent write keeps the good content recoverable.
        store.write(target, "good three".toByteArray())
        assertArrayEquals("good three".toByteArray(), store.read(target, isGood))
    }

    @Test
    fun `a throwing integrity predicate falls back to the backup`(@TempDir dir: Path) {
        val target = dir.resolve("doc.json")
        store.write(target, "good".toByteArray())
        store.write(target, "good again".toByteArray()) // .bak now holds "good"
        Files.write(target, "{ torn".toByteArray())

        // A real parser throws on torn bytes rather than returning false; recovery must still run.
        val parsing = { bytes: ByteArray ->
            check(bytes.decodeToString().startsWith("good")) { "parse error" }
            true
        }

        assertArrayEquals("good".toByteArray(), store.read(target, parsing))
    }

    @Test
    fun `an overwrite fsyncs the backup before replacing the primary`(@TempDir dir: Path) {
        val recording = RecordingFileSystemOps()
        val recordingStore = AtomicFileStore(recording)
        val target = dir.resolve("doc.json")

        recordingStore.write(target, "v1".toByteArray())
        recordingStore.write(target, "v2".toByteArray())

        assert(recording.fsyncedFiles.any { it.fileName.toString() == "doc.json.bak" }) {
            "backup was not fsynced before replace: ${recording.fsyncedFiles}"
        }
    }

    /** Records fsync'd file paths while delegating real I/O to [NioFileSystemOps]. */
    private class RecordingFileSystemOps(
        private val delegate: FileSystemOps = NioFileSystemOps,
    ) : FileSystemOps by delegate {
        val fsyncedFiles: MutableList<Path> = mutableListOf()

        override fun fsyncFile(path: Path) {
            fsyncedFiles.add(path)
            delegate.fsyncFile(path)
        }
    }
}
