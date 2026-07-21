package com.aritr.zinely.render.android

/**
 * Test-facing alias for the production [CmapCoverage] reader.
 *
 * The parser moved into `src/main` at F3 Increment 2 so [FontCoverage] could use it. This alias is kept
 * so existing tests read unchanged, and it **delegates** rather than carrying its own copy — two
 * implementations of a binary format is precisely the divergence this module's font work exists to
 * remove.
 */
internal object CmapReader {
    fun coveredCodePoints(data: ByteArray): Set<Int> = CmapCoverage.coveredCodePoints(data)
}
