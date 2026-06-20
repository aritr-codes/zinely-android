package com.aritr.zinely.core.data.storage

import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

/**
 * The low-level filesystem capability seam (ADR-025). The durability primitives an atomic save
 * relies on are not uniformly available across Android storage backends: app-private internal
 * storage supports atomic replace and (via `Os.fsync`) directory fsync; SAF / `DocumentsProvider`
 * / removable FAT/exFAT do not. Callers consult [capabilities] and route durability-sensitive
 * work only to a backend that supports it ([ADR-022 amendment](../../docs/DECISIONS.md)).
 *
 * The pure-JVM default ([NioFileSystemOps]) covers app-private paths; the real directory fsync
 * (`android.system.Os.open(dir, O_DIRECTORY) -> Os.fsync`) — which java.nio cannot do portably —
 * lives in the `:data-android` adapter behind this same interface.
 */
public interface FileSystemOps {
    public val capabilities: FsCapabilities

    /** Force the file's data + metadata to stable storage (fsync). No-op if unsupported. */
    public fun fsyncFile(path: Path)

    /** Force the directory entry to stable storage so a rename survives power loss. Best-effort. */
    public fun fsyncDir(dir: Path)

    /**
     * Move [source] onto [replacing], replacing any existing file. Atomic where the backend
     * supports it, so [replacing] is never observed half-written — it holds either the old bytes
     * or the new bytes, never a torn mix.
     */
    public fun atomicReplace(source: Path, replacing: Path)
}

/** What durability guarantees a [FileSystemOps] backend can actually honour. */
public data class FsCapabilities(
    val atomicReplace: Boolean,
    val fileFsync: Boolean,
    val dirFsync: Boolean,
)

/**
 * Pure java.nio [FileSystemOps] for **app-private internal storage** only. Directory fsync is
 * best-effort here (java.nio cannot open a directory as a channel on every platform — e.g.
 * Windows), so [FsCapabilities.dirFsync] is reported `false`; the `:data-android` adapter
 * supplies the real implementation. Do not use this for SAF/external URIs.
 */
public object NioFileSystemOps : FileSystemOps {
    override val capabilities: FsCapabilities =
        FsCapabilities(atomicReplace = true, fileFsync = true, dirFsync = false)

    override fun fsyncFile(path: Path) {
        FileChannel.open(path, StandardOpenOption.WRITE).use { it.force(true) }
    }

    override fun fsyncDir(dir: Path) {
        // Best-effort: opening a directory channel is unsupported on some platforms. The real
        // durable directory fsync is provided by the Android adapter via Os.fsync (ADR-025).
        try {
            FileChannel.open(dir, StandardOpenOption.READ).use { it.force(true) }
        } catch (_: IOException) {
            // unsupported on this platform — accept the weaker guarantee
        }
    }

    override fun atomicReplace(source: Path, replacing: Path) {
        try {
            Files.move(
                source,
                replacing,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            // Fall back to a non-atomic replace where the backend lacks atomic move.
            Files.move(source, replacing, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
