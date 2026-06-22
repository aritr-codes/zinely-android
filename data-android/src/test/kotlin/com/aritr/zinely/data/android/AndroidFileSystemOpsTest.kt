package com.aritr.zinely.data.android

import com.aritr.zinely.core.data.storage.AtomicFileStore
import com.aritr.zinely.core.data.storage.FileSystemOps
import com.aritr.zinely.core.data.storage.FsCapabilities
import com.aritr.zinely.core.data.storage.NioFileSystemOps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Pure-JVM contract for [AndroidFileSystemOps] (ADR-025 / ADR-026). The real `android.system.Os`
 * directory fsync is only callable on a device, so it is injected behind the [DirFsync] seam here;
 * these tests pin the durability *policy* — honest capability reporting and **fail-closed** directory
 * fsync — without needing an emulator. The real syscall is exercised in the instrumented test.
 */
class AndroidFileSystemOpsTest {

    private fun tempDir(): Path = Files.createTempDirectory("zinely-android-fs")

    @Test
    fun `advertises directory fsync honestly on top of the nio backend`() {
        val ops = AndroidFileSystemOps(NioFileSystemOps, NoopDirFsync)

        // atomicReplace + fileFsync inherited from the nio backend; dirFsync now genuinely provided.
        assertEquals(
            FsCapabilities(atomicReplace = true, fileFsync = true, dirFsync = true),
            ops.capabilities,
        )
    }

    @Test
    fun `fsyncDir routes to the real directory fsync seam with the given directory`() {
        val recording = RecordingDirFsync()
        val ops = AndroidFileSystemOps(NioFileSystemOps, recording)
        val dir = tempDir()

        ops.fsyncDir(dir)

        assertEquals(dir, recording.synced)
    }

    @Test(expected = IOException::class)
    fun `fsyncDir fails closed when the directory fsync syscall fails`() {
        // Os.fsync failing on app-private storage is a genuine durability failure: we advertise
        // dirFsync=true, so we must propagate, never silently degrade to a weaker guarantee.
        val failing = DirFsync { throw IOException("simulated ErrnoException") }
        val ops = AndroidFileSystemOps(NioFileSystemOps, failing)

        ops.fsyncDir(tempDir())
    }

    @Test
    fun `fsyncFile and atomicReplace delegate to the injected backend`() {
        val recording = RecordingDelegate()
        val ops = AndroidFileSystemOps(recording, NoopDirFsync)
        val dir = tempDir()
        val a = Files.write(dir.resolve("a"), "a".toByteArray())
        val b = dir.resolve("b")

        ops.fsyncFile(a)
        ops.atomicReplace(a, b)

        assertTrue(recording.fsynced.contains(a))
        assertEquals(a to b, recording.replaced)
    }

    @Test
    fun `is accepted by AtomicFileStore because it satisfies the durability requirements`() {
        // Must not throw: atomicReplace=true and fileFsync=true are the hard requirements.
        AtomicFileStore(AndroidFileSystemOps(NioFileSystemOps, NoopDirFsync))
    }

    @Test
    fun `a write through AtomicFileStore drives the directory fsync`() {
        val recording = RecordingDirFsync()
        val store = AtomicFileStore(AndroidFileSystemOps(NioFileSystemOps, recording))
        val target = tempDir().resolve("doc.json")

        store.write(target, "zine".toByteArray())

        assertEquals(target.parent, recording.synced)
    }

    @Test(expected = IOException::class)
    fun `a write fails closed when the directory fsync fails`() {
        // The whole stack must surface the durability failure rather than report a fake success.
        val store = AtomicFileStore(
            AndroidFileSystemOps(NioFileSystemOps, DirFsync { throw IOException("dir fsync down") }),
        )
        val target = tempDir().resolve("doc.json")

        store.write(target, "zine".toByteArray())
    }

    private object NoopDirFsync : DirFsync {
        override fun fsync(dir: Path) = Unit
    }

    private class RecordingDirFsync : DirFsync {
        var synced: Path? = null
        override fun fsync(dir: Path) {
            synced = dir
        }
    }

    /** Records delegated calls while really performing the I/O via [NioFileSystemOps]. */
    private class RecordingDelegate(
        private val delegate: FileSystemOps = NioFileSystemOps,
    ) : FileSystemOps by delegate {
        val fsynced: MutableList<Path> = mutableListOf()
        var replaced: Pair<Path, Path>? = null

        override fun fsyncFile(path: Path) {
            fsynced.add(path)
            delegate.fsyncFile(path)
        }

        override fun atomicReplace(source: Path, replacing: Path) {
            replaced = source to replacing
            delegate.atomicReplace(source, replacing)
        }
    }
}
