package com.aritr.zinely.core.data.storage

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

/**
 * Crash-safe document writer (ADR-021). A save is all-or-nothing and the prior good file always
 * survives a failure, because the target is replaced via [FileSystemOps.atomicReplace] of a
 * fully-written, fsync'd temp file — so at every instant the target holds either the complete old
 * bytes or the complete new bytes, never a torn write. A fsync'd `.bak` copy of the prior version is
 * kept as a recovery belt, and [read] performs the ADR-021 **open-time recovery** from it.
 *
 * The injected [FileSystemOps] must be able to atomically replace and fsync files; this is checked
 * at construction so a backend that cannot honour the all-or-nothing + durable-content invariant is
 * rejected up front rather than silently weakening durability. Directory fsync is a *best-effort*
 * dimension: the pure [NioFileSystemOps] reports `dirFsync = false` (java.nio cannot open a directory
 * channel portably), and the real `Os.fsync` directory flush is supplied by the `:data-android`
 * adapter behind the same seam.
 *
 * **Single-writer assumption:** this store uses a fixed `<name>.tmp` sidecar and a single `<name>.bak`
 * slot, so it assumes **one writer per [target]** — supplied by the ADR-021 autosave coordinator's
 * per-project single-writer mutex. Concurrent writes to the same target are the caller's
 * responsibility; they are not guarded here.
 *
 * Pure java.nio; scoped to app-private internal storage via the injected [FileSystemOps] seam
 * ([ADR-025](../../docs/DECISIONS.md#adr-025)).
 */
public class AtomicFileStore(
    private val fs: FileSystemOps = NioFileSystemOps,
) {
    init {
        val caps = fs.capabilities
        require(caps.atomicReplace && caps.fileFsync) {
            "AtomicFileStore needs a FileSystemOps that can atomically replace and fsync files " +
                "(the ADR-021 all-or-nothing + durable-content invariant); got $caps. Directory " +
                "fsync stays best-effort — NioFileSystemOps reports dirFsync=false and the " +
                ":data-android adapter supplies the real Os.fsync."
        }
    }

    /**
     * Durably replace [target] with [bytes]: snapshot the prior good version to a fsync'd
     * `<name>.bak`, then write a sibling temp, fsync it, atomically rename it over the target, and
     * fsync the directory. A failure anywhere leaves [target] holding its prior good content (and a
     * good `.bak`), and removes the temp. Relies on the caller having recovered a corrupt primary via
     * [read] first (single-writer), so the snapshot never captures known-bad bytes.
     */
    public fun write(target: Path, bytes: ByteArray) {
        // Snapshot the prior good file first (copy + fsync, so the recovery belt is durable and the
        // target never disappears). Skipped on the first write, when there is nothing to back up.
        if (Files.exists(target)) {
            val bak = backupOf(target)
            Files.copy(target, bak, StandardCopyOption.REPLACE_EXISTING)
            fs.fsyncFile(bak)
        }
        atomicPut(target, bytes)
    }

    /**
     * Read [target] with ADR-021 open-time crash recovery. Returns [target]'s bytes if present and
     * accepted by [isIntact]; otherwise recovers from the `<name>.bak` snapshot if it is present and
     * accepted, **repairing the primary on disk from the backup** before returning. Returns `null`
     * when neither a usable target nor a usable backup exists.
     *
     * The repair re-establishes the durability invariant (a good primary) so the *next* [write] does
     * not snapshot the corrupt primary over the only good copy. If the repair write itself fails, the
     * corrupt primary is **quarantined** (deleted) so it can never be snapshotted over the good
     * backup — leaving "primary is good or absent; backup is good" either way. Only in the
     * pathological case where both the repair write *and* the quarantine delete fail does this throw
     * (`IOException`, quarantine failure suppressed), rather than hand back bytes over an on-disk
     * state that still violates the invariant.
     *
     * [isIntact] keeps JSON/schema validity *above* this raw byte store (ADR-025 layering): the
     * repository can pass a predicate that deserializes/validates, so a structurally
     * corrupt-but-readable target is rejected and recovery tries the backup. A predicate that
     * **throws** (e.g. a parser on torn bytes) is treated as "not intact" so recovery still proceeds.
     * The default accepts any readable bytes, so a pure byte-level caller still gets
     * missing/unreadable-target → backup recovery.
     */
    public fun read(target: Path, isIntact: (ByteArray) -> Boolean = { true }): ByteArray? {
        readIfUsable(target, isIntact)?.let { return it }
        val recovered = readIfUsable(backupOf(target), isIntact) ?: return null
        // Repair the primary from the good backup without touching .bak, so a crash mid-repair still
        // leaves the backup as the good copy. If the repair write fails, quarantine the corrupt
        // primary: a surviving corrupt primary would be snapshotted over the good .bak by the next
        // write(), destroying the last good copy. Deleting it instead makes the next write() skip the
        // snapshot (target absent) and preserve the backup.
        try {
            atomicPut(target, recovered)
        } catch (repairFailure: IOException) {
            try {
                Files.deleteIfExists(target)
            } catch (quarantineFailure: IOException) {
                // Neither "primary good" (repair) nor "primary absent" (quarantine) holds: the on-disk
                // invariant is broken, so fail closed rather than hand back bytes over unstable state.
                repairFailure.addSuppressed(quarantineFailure)
                throw repairFailure
            }
        }
        return recovered
    }

    /** Atomically place [bytes] at [target] (no `.bak` handling). The all-or-nothing primitive. */
    private fun atomicPut(target: Path, bytes: ByteArray) {
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
            fs.atomicReplace(tmp, target)
            fs.fsyncDir(parent)
        } finally {
            // Never leave a partial temp behind, whether we succeeded or threw mid-write.
            Files.deleteIfExists(tmp)
        }
    }

    private fun readIfUsable(path: Path, isIntact: (ByteArray) -> Boolean): ByteArray? {
        if (!Files.exists(path)) return null
        val bytes = try {
            Files.readAllBytes(path)
        } catch (_: IOException) {
            // Unreadable target/backup — treat as absent so recovery can try the next candidate.
            return null
        }
        // A caller-supplied predicate that throws means "can't validate" → treat as corrupt so the
        // backup fallback still runs. Errors (not Exceptions) are left to propagate.
        val intact = try {
            isIntact(bytes)
        } catch (_: Exception) {
            false
        }
        return if (intact) bytes else null
    }

    private fun backupOf(target: Path): Path =
        target.resolveSibling(target.fileName.toString() + ".bak")
}
