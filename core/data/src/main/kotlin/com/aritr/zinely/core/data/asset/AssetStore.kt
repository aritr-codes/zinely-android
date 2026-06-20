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
 *
 * **The S2B implementation owns the ADR-022 import↔GC race closure** — it is deliberately *not*
 * surfaced on this pure put/get/exists API because it is internal store/sweep coordination, not a
 * caller concern. That implementation must: (1) register a hash in the **in-flight-import root set**
 * and (2) **touch the blob's mtime** *before* the document reference is committed; (3) serialize
 * writes and the sweep's unlink under a **store mutex**; and (4) **re-check mtime at unlink** so a
 * blob revived by a concurrent import past the grace window is never deleted. The deferred sweep
 * reconciles `assets/` against the document-derived live set (disk-vs-documents wins over the Room
 * index). See [ADR-022] and S2 spike §5/§11 for the full transaction contract.
 */
public interface AssetStore {
    /** Whether a blob for [hash] already exists (the dedupe fast-path). */
    public suspend fun contains(hash: ContentHash): Boolean

    /**
     * Content-address [masterBytes], persist them if new, and return their [ContentHash]. The S2B
     * implementation registers the hash as an in-flight root and refreshes the blob mtime before
     * returning, so a concurrent sweep cannot reclaim a blob this import is about to reference.
     */
    public suspend fun store(masterBytes: ByteArray): DataResult<ContentHash>

    /** Read the master bytes for [hash], or a failure if the blob is missing/unreadable. */
    public suspend fun read(hash: ContentHash): DataResult<ByteArray>
}
