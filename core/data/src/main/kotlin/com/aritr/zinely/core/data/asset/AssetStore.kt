package com.aritr.zinely.core.data.asset

import com.aritr.zinely.core.data.repository.DataResult

/**
 * Contract for the content-addressed asset store ([ADR-022]). The store persists **import-master
 * bytes** — already downscaled to ≤4096 px and EXIF-normalised ([ADR-023]); that step is Android
 * (Bitmap) work done in S2B before [store] is called, so this contract stays pure.
 *
 * Liveness/GC (mark-and-sweep over document/undo/in-flight roots) and the deferred WorkManager sweep
 * are S2B; the store itself is just put/get/exists keyed by [ContentHash]. There is **no delete on
 * this contract** — deletion is the GC's job, never a per-call concern (refcounting was rejected).
 */
public interface AssetStore {
    /** Whether a blob for [hash] already exists (the dedupe fast-path). */
    public suspend fun contains(hash: ContentHash): Boolean

    /** Content-address [masterBytes], persist them if new, and return their [ContentHash]. */
    public suspend fun store(masterBytes: ByteArray): DataResult<ContentHash>

    /** Read the master bytes for [hash], or a failure if the blob is missing/unreadable. */
    public suspend fun read(hash: ContentHash): DataResult<ByteArray>
}
