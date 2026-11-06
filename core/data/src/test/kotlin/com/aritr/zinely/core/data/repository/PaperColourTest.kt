package com.aritr.zinely.core.data.repository

import com.aritr.zinely.core.model.ColorRgba
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** AW6.5, A56.3/A56.8 and ADR-150 decision 5: the closed set, in order, and the one colour the host multiplies. */
class PaperColourTest {

    @Test
    fun `given the closed set, then it is A56_3's ten papers in pile order, with R41's values`() {
        assertEquals(
            listOf(
                "white" to "#FFFFFF", "ivory" to "#F4EEDC", "pastel-yellow" to "#F7EC9B", "pastel-orange" to "#F5CFA8",
                "pastel-pink" to "#F4C6D0", "pastel-purple" to "#DCD0EC", "pastel-blue" to "#BCD6EA",
                "pastel-green" to "#CDE6C8", "light-brown" to "#E8D3A9", "grey" to "#CFCDC6",
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
        assertEquals(PaperColour.PASTEL_ORANGE, PaperColour.fromId("pastel-orange"))
        assertEquals(PaperColour.PASTEL_PURPLE, PaperColour.fromId("pastel-purple"))
        assertEquals(PaperColour.LIGHT_BROWN, PaperColour.fromId("light-brown"))
    }

    /**
     * AW6.6's gate, the same arithmetic as `v21-library.html` / `v21-paper-pile.html`: the house Ink keeps 7:1 on the
     * paper under multiply, and no ink that reaches 3:1 on White falls below 3:1 on it. A56.8's three papers had to pass.
     */
    @Test
    fun `given every paper, when the AW6_6 gate is run, then every paper passes`() {
        val inks = listOf(
            "#2A251E", "#3E5E3A", "#5B5347", "#B0503F", "#8A5A9B", "#7C8A3F", "#8C8269",
            "#6E86C9", "#E27F89", "#57B0A9", "#D19A3C", "#B7AD93", "#E7B53C",
        )
        val white = rgb("#FFFFFF")
        PaperColour.entries.forEach { paper ->
            val q = with(paper.srgb) { doubleArrayOf(r / 255.0, g / 255.0, b / 255.0) }
            assertTrue(onPaper(rgb("#2A251E"), q) >= 7.0, "${paper.id}: house Ink under 7:1")
            inks.map(::rgb).filter { onPaper(it, white) >= 3.0 }.forEach { ink ->
                assertTrue(onPaper(ink, q) >= 3.0, "${paper.id}: an ink falls under 3:1")
            }
        }
    }

    private fun rgb(h: String) = doubleArrayOf(
        h.substring(1, 3).toInt(16) / 255.0, h.substring(3, 5).toInt(16) / 255.0, h.substring(5, 7).toInt(16) / 255.0,
    )
    private fun lin(v: Double) = if (v <= 0.03928) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
    private fun lum(c: DoubleArray) = 0.2126 * lin(c[0]) + 0.7152 * lin(c[1]) + 0.0722 * lin(c[2])
    private fun onPaper(ink: DoubleArray, paper: DoubleArray): Double {
        val a = lum(DoubleArray(3) { ink[it] * paper[it] })
        val b = lum(paper)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    private fun ColorRgba.hex() = "#%02X%02X%02X".format(r, g, b)
}
