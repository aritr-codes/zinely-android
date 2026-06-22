package com.aritr.zinely.data.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.FileDescriptor
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Fail-closed control flow of [SyscallDirFsync] (ADR-026), driven on a plain JVM via a fake
 * [DirSyscalls]. This is the durability-critical logic — validate the opened fd is really a
 * directory (no TOCTOU), fsync, always close, and never let a close failure mask a fsync failure.
 * The real `android.system.Os` binding ([OsDirSyscalls]) can only be exercised on a device, but its
 * *policy* is pinned here without an emulator.
 */
class SyscallDirFsyncTest {

    private val dir: Path = Paths.get("/some/dir")

    @Test
    fun `happy path opens validates fsyncs then closes in order`() {
        val sys = FakeSyscalls()

        SyscallDirFsync(sys).fsync(dir)

        assertEquals(listOf("open", "isDirectory", "fsync", "close"), sys.calls)
    }

    @Test
    fun `a non-directory fd fails closed and never fsyncs but still closes`() {
        val sys = FakeSyscalls(isDir = false)

        assertThrows(IOException::class.java) { SyscallDirFsync(sys).fsync(dir) }

        assertEquals(listOf("open", "isDirectory", "close"), sys.calls)
    }

    @Test
    fun `a fsync failure propagates and the fd is still closed`() {
        val sys = FakeSyscalls(fsyncError = IOException("EIO"))

        assertThrows(IOException::class.java) { SyscallDirFsync(sys).fsync(dir) }

        assertEquals(listOf("open", "isDirectory", "fsync", "close"), sys.calls)
    }

    @Test
    fun `a close failure after a successful fsync is suppressed`() {
        val sys = FakeSyscalls(closeError = IOException("EBADF"))

        // Must not throw: the durability boundary (fsync) was already crossed.
        SyscallDirFsync(sys).fsync(dir)

        assertEquals(listOf("open", "isDirectory", "fsync", "close"), sys.calls)
    }

    @Test
    fun `a fsync failure is preserved even when close also fails`() {
        val sys = FakeSyscalls(fsyncError = IOException("EIO"), closeError = IOException("EBADF"))

        val thrown = assertThrows(IOException::class.java) { SyscallDirFsync(sys).fsync(dir) }

        // The real durability failure must surface, not the benign close failure.
        assertEquals("EIO", thrown.message)
    }

    @Test
    fun `an open failure propagates with no fd to close`() {
        val sys = FakeSyscalls(openError = IOException("ENOENT"))

        assertThrows(IOException::class.java) { SyscallDirFsync(sys).fsync(dir) }

        assertEquals(listOf("open"), sys.calls)
    }

    private class FakeSyscalls(
        private val isDir: Boolean = true,
        private val openError: IOException? = null,
        private val fsyncError: IOException? = null,
        private val closeError: IOException? = null,
    ) : DirSyscalls {
        val calls: MutableList<String> = mutableListOf()

        override fun open(path: String): FileDescriptor {
            calls.add("open")
            openError?.let { throw it }
            return FileDescriptor()
        }

        override fun isDirectory(fd: FileDescriptor): Boolean {
            calls.add("isDirectory")
            return isDir
        }

        override fun fsync(fd: FileDescriptor) {
            calls.add("fsync")
            fsyncError?.let { throw it }
        }

        override fun close(fd: FileDescriptor) {
            calls.add("close")
            closeError?.let { throw it }
        }
    }
}
