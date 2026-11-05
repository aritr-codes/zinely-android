package com.aritr.zinely.core.data.repository

import com.aritr.zinely.core.model.ColorRgba
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** AW6.5 and ADR-150 decision 5: the closed set, in order, and the one colour the host multiplies. */
class PaperColourTest {

    @Test
    fun `the set is AW6_5's seven papers, light to dark, with R41's values`() {
        assertEquals(
            listOf(
                "white" to "#FFFFFF", "ivory" to "#F4EEDC", "pastel-yellow" to "#F7EC9B", "pastel-pink" to "#F4C6D0",
                "pastel-blue" to "#BCD6EA", "pastel-green" to "#CDE6C8", "grey" to "#CFCDC6",
            ),
            PaperColour.entries.map { it.id to it.srgb.hex() },
        )
    }

    @Test
    fun `White draws no layer and every other paper draws its own colour`() {
        assertNull(PaperColour.WHITE.overlay)
        PaperColour.entries.filter { it != PaperColour.WHITE }.forEach { assertEquals(it.srgb, it.overlay) }
    }

    @Test
    fun `an unknown or absent id is drawn as White, so it draws nothing`() {
        assertNull(PaperColour.fromId("brown").overlay)
        assertNull(PaperColour.fromId(null).overlay)
        assertEquals(PaperColour.PASTEL_BLUE, PaperColour.fromId("pastel-blue"))
    }

    private fun ColorRgba.hex() = "#%02X%02X%02X".format(r, g, b)
}
