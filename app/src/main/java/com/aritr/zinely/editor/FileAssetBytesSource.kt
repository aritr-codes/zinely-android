package com.aritr.zinely.editor

import com.aritr.zinely.core.data.asset.ContentHash
import com.aritr.zinely.render.android.AssetBytesSource
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

/**
 * The render read-path over the content-addressed master store (ADR-031 §3). A master is a plain local
 * file `assetsDir/<sha256>`, so `open` is a cheap **synchronous** `FileInputStream` — the
 * [ImageBlitter][com.aritr.zinely.render.android.ImageBlitter] calls it 2–3× per draw and needs a
 * fresh stream at byte 0 each time, which a new `FileInputStream` gives. This deliberately **bypasses**
 * the suspend [AssetStore][com.aritr.zinely.core.data.asset.AssetStore] (no `runBlocking` on the draw
 * thread, no cache — the [ADR-030] Codex-3 hazard avoided structurally).
 *
 * [assetId] is validated as a sha256 hex before any file access, so a malformed/hostile id (e.g. a
 * `../` traversal) can never escape [assetsDir]; a missing or unreadable file returns `null`, which the
 * blitter renders as its defined missing-asset placeholder.
 */
public class FileAssetBytesSource(private val assetsDir: File) : AssetBytesSource {
    override fun open(assetId: String): InputStream? {
        if (!ContentHash.isValid(assetId)) return null
        val file = File(assetsDir, assetId)
        return try {
            if (file.isFile) FileInputStream(file) else null
        } catch (_: IOException) {
            null // absent/unreadable ⇒ MISSING (placeholder), never a crash on the draw path
        }
    }
}
