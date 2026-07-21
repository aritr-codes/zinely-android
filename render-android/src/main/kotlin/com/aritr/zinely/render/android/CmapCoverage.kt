package com.aritr.zinely.render.android

/**
 * A minimal, dependency-free TrueType/OpenType `cmap` reader: the set of Unicode code points a font file
 * **itself** maps to a non-`.notdef` glyph (ADR-028 §4.2).
 *
 * Promoted to production at F3 Increment 2 so [FontCoverage] can verify at build/test time that the
 * bundled fonts keep the promise `SupportedScripts.BUNDLED_SCRIPTS` makes. It was previously a test-only
 * helper (`CmapReader`); the parsing logic is unchanged, because a second implementation of a binary
 * format is a divergence waiting to happen.
 *
 * `Paint.hasGlyph` is the wrong tool here: it walks the **whole system fallback chain**, so it would
 * falsely pass a glyph the bundled font lacks but a device font supplies — exactly the preview-vs-export
 * drift the bundled-font policy removes. Parsing the file answers "does *this* TTF cover it?".
 *
 * Handles the two Unicode subtable formats real fonts use: format 4 (BMP segment mapping) and format 12
 * (full UCS-4 groups).
 */
public object CmapCoverage {

    public fun coveredCodePoints(data: ByteArray): Set<Int> {
        val numTables = u16(data, 4)
        var off = 12
        var cmapOff = -1
        repeat(numTables) {
            val tag = String(data, off, 4, Charsets.US_ASCII)
            if (tag == "cmap") cmapOff = u32(data, off + 8)
            off += 16
        }
        require(cmapOff >= 0) { "no cmap table" }

        val subtableCount = u16(data, cmapOff + 2)
        val out = HashSet<Int>()
        for (i in 0 until subtableCount) {
            val rec = cmapOff + 4 + i * 8
            val platform = u16(data, rec)
            val encoding = u16(data, rec + 2)
            val sub = cmapOff + u32(data, rec + 4)
            when (u16(data, sub)) {
                4 -> if (platform to encoding in UNICODE_BMP) parseFormat4(data, sub, out)
                12 -> if (platform to encoding in UNICODE_FULL) parseFormat12(data, sub, out)
            }
        }
        return out
    }

    private fun parseFormat4(data: ByteArray, sub: Int, out: MutableSet<Int>) {
        val segX2 = u16(data, sub + 6)
        val segCount = segX2 / 2
        var p = sub + 14
        val end = IntArray(segCount) { u16(data, p + it * 2) }; p += segX2 + 2 // +2 reservedPad
        val start = IntArray(segCount) { u16(data, p + it * 2) }; p += segX2
        val delta = IntArray(segCount) { s16(data, p + it * 2) }; p += segX2
        val rangeOffsetsAt = p
        val rangeOffset = IntArray(segCount) { u16(data, p + it * 2) }
        for (i in 0 until segCount) {
            val s = start[i]
            val e = end[i]
            if (s == 0xFFFF && e == 0xFFFF) continue // the mandatory final sentinel segment
            for (c in s..e) {
                val glyph = if (rangeOffset[i] == 0) {
                    (c + delta[i]) and 0xFFFF
                } else {
                    val gIndexAt = rangeOffsetsAt + i * 2 + rangeOffset[i] + (c - s) * 2
                    val g = u16(data, gIndexAt)
                    if (g == 0) 0 else (g + delta[i]) and 0xFFFF
                }
                if (glyph != 0) out.add(c)
            }
        }
    }

    private fun parseFormat12(data: ByteArray, sub: Int, out: MutableSet<Int>) {
        val groups = u32(data, sub + 12)
        var p = sub + 16
        repeat(groups) {
            val startChar = u32(data, p)
            val endChar = u32(data, p + 4)
            val startGlyph = u32(data, p + 8)
            p += 12
            // Skip any code point that maps to glyph 0 (.notdef) — not real coverage.
            for (c in startChar..endChar) if (startGlyph + (c - startChar) != 0) out.add(c)
        }
    }

    private val UNICODE_BMP = setOf(3 to 1, 0 to 3, 0 to 4)
    private val UNICODE_FULL = setOf(3 to 10, 0 to 4, 0 to 6)

    private fun u16(b: ByteArray, i: Int): Int =
        ((b[i].toInt() and 0xFF) shl 8) or (b[i + 1].toInt() and 0xFF)

    private fun s16(b: ByteArray, i: Int): Int = u16(b, i).toShort().toInt()

    private fun u32(b: ByteArray, i: Int): Int =
        ((b[i].toInt() and 0xFF) shl 24) or ((b[i + 1].toInt() and 0xFF) shl 16) or
            ((b[i + 2].toInt() and 0xFF) shl 8) or (b[i + 3].toInt() and 0xFF)
}
