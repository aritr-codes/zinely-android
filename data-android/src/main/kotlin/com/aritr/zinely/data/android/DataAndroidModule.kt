package com.aritr.zinely.data.android

import com.aritr.zinely.core.data.storage.FileSystemOps

/**
 * Module marker for `:data-android` (ADR-026 PR-A, Build Order Step 1).
 *
 * This Android library will host the production adapters that bind the frozen, Android-free S2B
 * durability core ([com.aritr.zinely.core.data.storage]) to the real Android stack: the
 * `Os.fsync`-backed [FileSystemOps] (ADR-025, fail-closed directory fsync), the file-only
 * `DocumentRepository`, the autosave coordinator factory, and the lifecycle binder (ADR-026). None
 * of those exist yet — Step 1 lands only the gated module skeleton and dependency wiring.
 *
 * The reference to [FileSystemOps] below is a **compile-time assertion** that the pure-Kotlin core
 * seam is on this module's classpath in the intended one-way direction (ADR-025: `:data-android` ->
 * `:core:*`, never the reverse). It carries no behaviour and is removed once the real
 * `AndroidFileSystemOps` adapter lands in Step 2.
 */
internal object DataAndroidModule {
    /** Proves `:core:data-storage` is wired as a dependency; no runtime role. */
    internal val coreSeam: Class<FileSystemOps> = FileSystemOps::class.java
}
