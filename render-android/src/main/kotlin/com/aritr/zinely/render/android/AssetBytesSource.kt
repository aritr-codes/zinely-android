package com.aritr.zinely.render.android

import java.io.InputStream

/**
 * The seam through which [ImageBlitter] reads the **canonical import-master bytes** ([ADR-023]) for an
 * asset — the same bytes in preview, export, and tests, so `inJustDecodeBounds` reports the same
 * intrinsic everywhere and parity cannot silently break (ADR-028 §5.3). `:render-android` never depends
 * on Room / `AssetStore` / Coil; `:data-android` implements this in app wiring (a content-addressed
 * master is a file, so re-opening is cheap and reliable).
 *
 * **Fresh stream per call (Codex Required-fix D).** The blitter calls [open] **more than once** per
 * draw — once for the bounds pass (`inJustDecodeBounds`, which *consumes* the stream) and again for the
 * pixel decode. Many asset/content streams cannot `reset()`/`mark()`, so every call MUST return a new,
 * fully independent stream positioned at byte 0. The blitter owns and closes each returned stream
 * (`use { }`). A `null` on **any** call (including the second open) ⇒ the asset is treated as MISSING
 * → placeholder (§5.4); the blitter never emits a half-decoded draw.
 */
public fun interface AssetBytesSource {
    /** A fresh, independent stream at byte 0 of [assetId]'s import-master, or `null` if absent. */
    public fun open(assetId: String): InputStream?
}
