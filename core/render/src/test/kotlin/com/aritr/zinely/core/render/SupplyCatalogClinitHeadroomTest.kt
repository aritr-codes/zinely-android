package com.aritr.zinely.core.render

import com.aritr.zinely.core.copy.Copy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ART-1 guards for the family split. A JVM method (so `<clinit>`) is capped at 64 KB of bytecode; a
 * family object that outgrows it fails at class-load, on a phone, not at compile time. The class file is a
 * safe upper bound on `<clinit>`, so each stays under 48 KB, leaving 16 KB of headroom (R-A5).
 */
class SupplyCatalogClinitHeadroomTest {
    @Test
    fun `given each family art object, when its class file is read, then it is under 48 KB`() {
        for (name in listOf("ShapeArt", "MarkArt", "MarkArtPart2", "PaperArt", "PaperArtPart2", "TapeArt")) {
            val bytes = checkNotNull(javaClass.getResourceAsStream("/com/aritr/zinely/core/render/$name.class")) { "$name.class not on the classpath; update this list when a family object is added or renamed" }.use { it.readBytes() }
            assertTrue(bytes.size <= 48 * 1024, "$name.class is ${bytes.size} bytes; split the family before it nears 64 KB")
        }
    }

    @Test
    fun `given the split catalogue, when OUTLINES is listed, then ids follow Copy BY_FAMILY order`() {
        assertEquals(
            Copy.Supplies.BY_FAMILY.values.flatMap { it.keys },
            SupplyCatalog.OUTLINES.keys.toList(),
        )
    }
}
