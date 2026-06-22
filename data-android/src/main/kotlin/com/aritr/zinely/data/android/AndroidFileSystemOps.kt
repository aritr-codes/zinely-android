package com.aritr.zinely.data.android

import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import com.aritr.zinely.core.data.storage.FileSystemOps
import com.aritr.zinely.core.data.storage.FsCapabilities
import com.aritr.zinely.core.data.storage.NioFileSystemOps
import java.io.FileDescriptor
import java.io.IOException
import java.nio.file.Path

/**
 * The Android production [FileSystemOps] for **app-private internal storage** (ADR-025 / ADR-026).
 *
 * Atomic replace and file fsync work portably through java.nio on an app-private path, so those are
 * delegated to [NioFileSystemOps]. What java.nio cannot do portably is fsync a *directory* — the
 * flush that makes a rename survive power loss — so this adapter supplies the real
 * `Os.open(O_RDONLY) → fstat/S_ISDIR check → Os.fsync → Os.close` and therefore advertises
 * [FsCapabilities.dirFsync] = `true`.
 *
 * **Fail-closed durability (ADR-026):** because it advertises `dirFsync = true`, a directory fsync
 * failure is a genuine durability failure and [fsyncDir] **throws** — it never silently degrades to
 * the weaker best-effort guarantee that [NioFileSystemOps] (which honestly reports `dirFsync = false`)
 * accepts. [AtomicFileStore][com.aritr.zinely.core.data.storage.AtomicFileStore] calls `fsyncDir`
 * inside its commit, so a throw here propagates as a failed save rather than a fake success.
 *
 * The real syscall lives behind the [DirFsync] seam so the fail-closed policy is unit-testable on a
 * plain JVM (`android.system.Os` is only callable on a device); the production constructor wires
 * [SyscallDirFsync] over the real [OsDirSyscalls].
 *
 * Scope: app-private internal storage only. SAF / `DocumentsProvider` / removable FAT/exFAT do not
 * support these guarantees and must not be routed here (ADR-022 amendment).
 */
public class AndroidFileSystemOps internal constructor(
    private val delegate: FileSystemOps,
    private val dirFsync: DirFsync,
) : FileSystemOps {

    /** Production wiring: java.nio for atomic-replace/file-fsync + the real `Os.fsync` directory flush. */
    public constructor() : this(NioFileSystemOps, SyscallDirFsync(OsDirSyscalls))

    // dirFsync is genuinely provided here, so flip the one capability the nio backend cannot honour.
    // atomicReplace / fileFsync are inherited honestly from the delegate (true for app-private paths).
    override val capabilities: FsCapabilities = delegate.capabilities.copy(dirFsync = true)

    override fun fsyncFile(path: Path): Unit = delegate.fsyncFile(path)

    override fun atomicReplace(source: Path, replacing: Path): Unit =
        delegate.atomicReplace(source, replacing)

    override fun fsyncDir(dir: Path): Unit = dirFsync.fsync(dir)
}

/**
 * Seam over the whole directory-fsync operation. Exists so [AndroidFileSystemOps]'s fail-closed
 * contract can be driven from plain-JVM unit tests; the production implementation is [SyscallDirFsync].
 */
internal fun interface DirFsync {
    /** Fsync the directory entry. Must throw [IOException] on failure (fail closed), never no-op. */
    fun fsync(dir: Path)
}

/**
 * The fail-closed directory-fsync **control flow** (ADR-026), independent of the actual syscalls so
 * it can be unit-tested on a plain JVM via a fake [DirSyscalls]:
 *
 * 1. open the directory (read-only);
 * 2. validate the **opened fd** is really a directory (`fstat`, not a path pre-check) — this closes
 *    the TOCTOU window where the path could be swapped to a regular file between check and open, and
 *    is itself part of failing closed (a non-directory fd is rejected, never fsync'd as a file);
 * 3. fsync the fd so a prior atomic rename is durable;
 * 4. always close the fd; a close failure *after* a successful fsync is benign (the data already
 *    reached stable storage) and is suppressed so it can never **mask** a real fsync failure.
 *
 * Any open/fstat/fsync failure surfaces as [IOException] so the caller fails closed.
 */
internal class SyscallDirFsync(private val sys: DirSyscalls) : DirFsync {
    override fun fsync(dir: Path) {
        val fd = sys.open(dir.toString())
        try {
            if (!sys.isDirectory(fd)) {
                throw IOException("not a directory, refusing to fsync: $dir")
            }
            sys.fsync(fd)
        } finally {
            try {
                sys.close(fd)
            } catch (_: IOException) {
                // Benign after a successful fsync; on the failure path it must not mask the fsync error.
            }
        }
    }
}

/**
 * Seam over the individual directory-fsync syscalls, so [SyscallDirFsync]'s fail-closed control flow
 * is JVM-testable. Each method must throw [IOException] on failure. The production binding is
 * [OsDirSyscalls]; the real kernel durability can only be proven by the instrumented test.
 */
internal interface DirSyscalls {
    /** Open [path] read-only, returning its descriptor. */
    fun open(path: String): FileDescriptor

    /** Whether [fd] refers to a directory (validated via `fstat` on the opened descriptor). */
    fun isDirectory(fd: FileDescriptor): Boolean

    /** Force [fd]'s directory entry to stable storage. */
    fun fsync(fd: FileDescriptor)

    /** Close [fd]. */
    fun close(fd: FileDescriptor)
}

/**
 * Real directory syscalls via `android.system.Os` (API 21+). `OsConstants` exposes no `O_DIRECTORY`,
 * so the directory check is an `fstat` + `S_ISDIR` on the already-opened fd (also TOCTOU-safe).
 * Every `ErrnoException` is mapped to [IOException] so the [SyscallDirFsync] control flow stays
 * platform-neutral and fails closed.
 */
internal object OsDirSyscalls : DirSyscalls {
    override fun open(path: String): FileDescriptor =
        try {
            Os.open(path, OsConstants.O_RDONLY, 0)
        } catch (e: ErrnoException) {
            throw IOException("open failed for directory $path", e)
        }

    override fun isDirectory(fd: FileDescriptor): Boolean =
        try {
            OsConstants.S_ISDIR(Os.fstat(fd).st_mode)
        } catch (e: ErrnoException) {
            throw IOException("fstat failed on directory fd", e)
        }

    override fun fsync(fd: FileDescriptor) {
        try {
            Os.fsync(fd)
        } catch (e: ErrnoException) {
            throw IOException("directory fsync failed", e)
        }
    }

    override fun close(fd: FileDescriptor) {
        try {
            Os.close(fd)
        } catch (e: ErrnoException) {
            throw IOException("close failed on directory fd", e)
        }
    }
}
