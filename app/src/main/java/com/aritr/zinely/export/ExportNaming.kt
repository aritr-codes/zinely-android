package com.aritr.zinely.export

/**
 * Pure naming policy for durable ("keep a copy") exports — the single owner of the user-facing display
 * name (ADR-054 Decision 5): sanitisation, display-name generation, and the legacy collision suffix.
 *
 * Sanitisation mirrors Android's `FileUtils.buildValidFatFilename` (each FAT-invalid char → `_`) so the
 * name this computes for the API 24–28 File path is identical to the `DISPLAY_NAME` MediaStore derives on
 * API 29+, and the collision suffix uses MediaStore's own `" (N)"` convention — the "MediaStore parity"
 * invariant (ADR-054 Decision 6). Knows nothing about MediaStore, `ContentValues`, streams, or the
 * transport/cache filename (that opaque `zine-<millis>` token is owned by [ZineExporter], not here).
 */
internal object ExportNaming {

    /** Used when a title sanitises to nothing (empty, blank, or all-invalid). */
    const val FALLBACK_BASE = "zine"

    /** Cap the base so `base + " (99)" + ".pdf"` stays well under the 255-byte FAT/ext4 name limit. */
    private const val MAX_BASE = 200

    /** FAT-invalid chars (Android `FileUtils.isValidFatFilenameChar`) plus control chars (< 0x20) → `_`. */
    private fun isValidFatChar(c: Char): Boolean = when (c) {
        '"', '*', '/', ':', '<', '>', '?', '\\', '|' -> false
        else -> c.code >= 0x20
    }

    /** A user title reduced to a valid FAT base name (no extension). Empty result → [FALLBACK_BASE]. */
    fun sanitizeBase(title: String): String {
        val mapped = buildString(title.length) {
            for (c in title) append(if (isValidFatChar(c)) c else '_')
        }
        // FAT forbids trailing '.'/' ' and a leading '.'; trim to match FileUtils and keep a clean name.
        val cleaned = mapped.trim().trim('.', ' ').take(MAX_BASE).trim().trim('.', ' ')
        return cleaned.ifEmpty { FALLBACK_BASE }
    }

    /** The requested display name: sanitised base + `.` + [ext] (e.g. `"My Zine.pdf"`). */
    fun displayName(title: String, ext: String): String = "${sanitizeBase(title)}.$ext"

    /**
     * The first non-colliding display name for the legacy (API 24–28) File path, using MediaStore's
     * `" (N)"` convention: `name.ext`, then `name (1).ext`, `name (2).ext`, … [exists] is the sole I/O
     * seam (a directory probe), injected so this stays pure and unit-testable.
     */
    fun nextAvailableName(title: String, ext: String, exists: (String) -> Boolean): String {
        val base = sanitizeBase(title)
        val first = "$base.$ext"
        if (!exists(first)) return first
        var n = 1
        while (true) {
            val candidate = "$base ($n).$ext"
            if (!exists(candidate)) return candidate
            n++
        }
    }
}
