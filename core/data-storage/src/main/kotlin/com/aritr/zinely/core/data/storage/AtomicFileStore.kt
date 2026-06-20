package com.aritr.zinely.core.data.storage

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

/**
 * Crash-safe document writer (ADR-021). A save is all-or-nothing and the prior good file always
 * survives a failure, because the target is replaced via [FileSystemOps.atomicReplace] of a
 * fully-written, fsync'd temp file — so at every instant the target holds either the complete old
 * bytes or the complete new bytes, never a torn write. A `.bak` copy of the prior version is kept
 * as a recovery belt.
 *
 * Pure java.nio; scoped to app-private internal storage via the injected [FileSystemOps] seam
 * ([ADR-025](../../docs/DECISIONS.md)).
 */
public class AtomicFileStore(
    private val fs: FileSystemOps = NioFileSystemOps,
) {
    /**
     * Durably replace [target] with [bytes]: write a sibling temp, fsync it, snapshot the prior
     * version to `<name>.bak`, then atomically rename the temp over the target and fsync the
     * directory. A failure anywhere leaves [target] holding its prior good content and removes the
     * temp.
     */
    public fun write(target: Path, bytes: ByteArray) {
        val parent = requireNotNull(target.parent) { "target must have a parent directory: $target" }
        Files.createDirectories(parent)
        val tmp = parent.resolve(target.fileName.toString() + ".tmp")
        try {
            Files.write(
                tmp,
                bytes,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
            )
            fs.fsyncFile(tmp)

            // Snapshot the prior good file first (copy, so target never disappears).
            if (Files.exists(target)) {
                val bak = parent.resolve(target.fileName.toString() + ".bak")
                Files.copy(target, bak, StandardCopyOption.REPLACE_EXISTING)
            }

            fs.atomicReplace(tmp, target)
            fs.fsyncDir(parent)
        } finally {
            // Never leave a partial temp behind, whether we succeeded or threw mid-write.
            Files.deleteIfExists(tmp)
        }
    }

    /** Read the current bytes of [target], or `null` if it does not exist. */
    public fun read(target: Path): ByteArray? =
        if (Files.exists(target)) Files.readAllBytes(target) else null
}
