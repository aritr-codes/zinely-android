package com.aritr.zinely.core.data.asset

import com.aritr.zinely.core.data.repository.DataResult

/**
 * Contract for the content-addressed asset store ([ADR-022]). The store persists **import-master
 * bytes** — already downscaled to ≤4096 px and EXIF-normalised ([ADR-023]); that step is Android
 * (Bitmap) work done in S2B before [store] is called, so this contract stays pure.
 *
 * Liveness/GC (mark-and-sweep over document/undo roots) is **deferred and not yet shipped**; the
 * store itself is just put/get/exists keyed by [ContentHash]. There is **no delete on this contract**
 * — deletion is the GC's job, never a per-call concern (refcounting was rejected).
 *
 * **The import↔GC race closure is a future concern, not part of the shipped store.** The shipped
 * implementation ([FileAssetStore][com.aritr.zinely.core.data.storage.FileAssetStore]) content-
 * addresses and atomically writes blobs, but does **not** yet create the [ADR-022]-amendment pin
 * files / generation counter. That is safe only because **no sweeper exists yet** ([ADR-031] §2):
 * no asset GC may ship until the import path pins a hash *before* the document reference commits,
 * else a sweep could reclaim a blob in the window between [store] and the editor's commit. When the
 * GC lands it will own that pin/generation race closure; it is deliberately *not* surfaced on this
 * pure put/get/exists API because it is internal store/sweep coordination, not a caller concern.
 * See [ADR-022], [ADR-031], and S2 spike §5/§11 for the planned transaction contract.
 */
public interface AssetStore {
    /** Whether a blob for [hash] already exists (the dedupe fast-path). */
    public suspend fun contains(hash: ContentHash): Boolean

    /**
     * Content-address [masterBytes], persist them if new, and return their [ContentHash]. The shipped
     * implementation writes atomically and dedupes on the hash; pin/generation protection against a
     * concurrent sweep is deferred until the GC lands (no sweeper exists yet — [ADR-031] §2).
     */
    public suspend fun store(masterBytes: ByteArray): DataResult<ContentHash>

    /** Read the master bytes for [hash], or a failure if the blob is missing/unreadable. */
    public suspend fun read(hash: ContentHash): DataResult<ByteArray>
}
