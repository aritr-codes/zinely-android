package com.aritr.zinely.core.imposition

import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SvgProofSheetRendererTest {
    private val renderer = SvgProofSheetRenderer()
    private val layout = SingleSheet8Imposer().layout(ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER)
    private val proof = renderer.render(layout)
    private val svg = proof.svg

    private fun count(needle: String) = svg.split(needle).size - 1

    @Test
    fun `proof carries sheet dimensions and a well-formed svg root`() {
        assertEquals(792.0, proof.widthPt)
        assertEquals(612.0, proof.heightPt)
        assertTrue(svg.trimStart().startsWith("<svg"))
        assertTrue(svg.trimEnd().endsWith("</svg>"))
        assertTrue(svg.contains("xmlns=\"http://www.w3.org/2000/svg\""))
        assertTrue(svg.contains("viewBox=\"0 0 792 612\""))
    }

    @Test
    fun `renders one panel and one safe area per booklet page`() {
        assertEquals(8, count("class=\"panel\""))
        assertEquals(8, count("class=\"safe\""))
    }

    @Test
    fun `labels every booklet page and both covers`() {
        for (n in 1..8) assertTrue(svg.contains(">$n</text>"), "missing page label $n")
        assertTrue(svg.contains("FRONT_COVER"))
        assertTrue(svg.contains("BACK_COVER"))
    }

    @Test
    fun `places an upright panel via a pure-translate matrix`() {
        // page 1 is upright at cell (1,3) -> bounds origin (594,306)
        assertTrue(svg.contains("matrix(1 0 0 1 594 306)"), "upright transform missing")
    }

    @Test
    fun `places a rotated panel via a half-turn matrix`() {
        // page 5 is a 180-degree panel at the origin cell -> half-turn about (99,153)
        assertTrue(svg.contains("matrix(-1 0 0 -1 198 306)"), "half-turn transform missing")
    }

    @Test
    fun `draws four fold guides and one cut guide`() {
        assertEquals(4, count("class=\"fold\""))
        assertEquals(1, count("class=\"cut\""))
    }

    @Test
    fun `never emits NaN or Infinity`() {
        assertFalse(svg.contains("NaN"))
        assertFalse(svg.contains("Infinity"))
    }

    @Test
    fun `is deterministic`() {
        assertEquals(renderer.render(layout).svg, renderer.render(layout).svg)
    }

    @Test
    fun `formats A4 dimensions without a locale-dependent decimal separator`() {
        val a4 = renderer.render(SingleSheet8Imposer().layout(ZineFormat.SINGLE_SHEET_8, PaperSize.A4))
        assertTrue(a4.svg.contains("viewBox=\"0 0 841.89 595.276\""), "A4 viewBox wrong: not found")
    }
}
