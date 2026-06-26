package com.aritr.zinely.core.data.storage

import com.aritr.zinely.core.data.asset.AssetStore
import com.aritr.zinely.core.data.asset.ContentHash
import com.aritr.zinely.core.data.asset.ContentHasher
import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.DataResult
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Pure-java.nio [AssetStore] over the global content-addressed store `rootDir/<sha256>` (ADR-022,
 * ADR-031 §1). A blob is written to a unique temp under `rootDir/.tmp`, fsync'd, then **atomically
 * renamed** into place through the [FileSystemOps] capability seam — so `rootDir/<hex>` is never
 * observed half-written, and the render read-path ([open][com.aritr.zinely…]) only ever sees a
 * complete master.
 *
 * **Dedupe.** [store] content-addresses first; if the destination already exists it returns success
 * **without rewriting** (the same photo placed twice ⇒ one file). Two concurrent imports of identical
 * bytes each write their own temp and atomically rename over the same target — last writer wins with
 * identical bytes, so both callers get the same [ContentHash] (ADR-031 §1, Codex Rec).
 *
 * **GC is deferred — and a sweeper is BLOCKED until import is pin-safe (ADR-031 §2, Codex RF3).** This
 * store writes atomically but does **not** create the [ADR-022]-amendment pin files / generation
 * counter. That is safe only because no `GcWorker` exists yet; **no asset sweep may ship until the
 * import path pins a hash before the document reference commits**, else a sweep could reclaim a blob
 * in the window between [store] and the editor's `CommitAddImage`. The pin/generation work lands with
 * the GC, not here.
 *
 * Android-free; scoped to app-private internal storage via the injected [FileSystemOps] (ADR-025).
 *
 * @param rootDir the assets directory (e.g. `filesDir/assets`); shared with the render read-path.
 */
public class FileAssetStore(
    private val rootDir: Path,
    private val fs: FileSystemOps = NioFileSystemOps,
) : AssetStore {

    init {
        val caps = fs.capabilities
        require(caps.atomicReplace && caps.fileFsync) {
            "FileAssetStore needs a FileSystemOps that can atomically replace and fsync files " +
                "(ADR-022 temp→rename invariant); got $caps."
        }
    }

    private val tmpDir: Path get() = rootDir.resolve(".tmp")

    // A blob is a *regular file* at rootDir/<hex>. Using isRegularFile (not Files.exists) means a stray
    // directory/symlink at that name is never mistaken for a stored master (Codex RF1) — store() would
    // otherwise short-circuit to Success with no readable bytes, and contains() would lie.
    override suspend fun contains(hash: ContentHash): Boolean =
        Files.isRegularFile(rootDir.resolve(hash.hex))

    override suspend fun store(masterBytes: ByteArray): DataResult<ContentHash> {
        val hash = ContentHasher.sha256(masterBytes)
        val target = rootDir.resolve(hash.hex)
        return try {
            if (!Files.isRegularFile(target)) {
                Files.createDirectories(tmpDir)
                // A unique temp (not "<hex>.tmp") so concurrent imports of identical bytes don't collide
                // on the temp, and a leaked temp can never pass ContentHash.isValid → the reader skips it.
                val tmp = Files.createTempFile(tmpDir, "import", ".tmp")
                try {
                    Files.write(
                        tmp,
                        masterBytes,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE,
                    )
                    fs.fsyncFile(tmp)
                    try {
                        fs.atomicReplace(tmp, target)
                    } catch (e: IOException) {
                        // Lost a concurrent identical-import race: if another writer already completed the
                        // blob, that IS success (dedupe — Codex Rec). Some backends (e.g. Windows ATOMIC_MOVE
                        // + REPLACE_EXISTING) reject replacing an extant target; the winner's bytes are
                        // identical, so re-throw only if the target is still not a complete blob.
                        if (!Files.isRegularFile(target)) throw e
                    }
                    fs.fsyncDir(rootDir)
                } finally {
                    Files.deleteIfExists(tmp)
                }
            }
            DataResult.Success(hash)
        } catch (e: IOException) {
            DataResult.Failure(DataError.Io("asset store write failed for ${hash.hex}", e))
        }
    }

    override suspend fun read(hash: ContentHash): DataResult<ByteArray> {
        val target = rootDir.resolve(hash.hex)
        if (!Files.isRegularFile(target)) return DataResult.Failure(DataError.NotFound(hash.hex))
        return try {
            DataResult.Success(Files.readAllBytes(target))
        } catch (e: IOException) {
            DataResult.Failure(DataError.Io("asset read failed for ${hash.hex}", e))
        }
    }
}
