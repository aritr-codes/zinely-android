package com.aritr.zinely.core.editor.ink

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InkColourTest {
    @Test fun `given every table hex when round tripped through hsv then unchanged`() {
        for (row in NAME_TABLE) {
            val hex = InkHex(row.first)
            val hsv = hexToHsv(hex, keepHue = null)
            assertEquals(hex, hsvToHex(hsv.h, hsv.s, hsv.v), row.first)
        }
    }

    @Test fun `given a grey and a kept hue then the hue is kept`() =
        assertEquals(150.0, hexToHsv(InkHex("808080"), keepHue = 150.0).h)

    @Test fun `given a grey and no kept hue then zero`() =
        assertEquals(0.0, hexToHsv(InkHex("808080"), keepHue = null).h)

    @Test fun `given zero chroma then no hue`() = assertNull(hueOf(0.5, 0.5, 0.5))

    @Test fun `given a red then hue zero and blue then 240`() {
        assertEquals(0.0, hueOf(1.0, 0.0, 0.0)!!, 1e-9)
        assertEquals(240.0, hueOf(0.0, 0.0, 1.0)!!, 1e-9)
    }

    @Test fun `contrast on white at the extremes`() {
        assertEquals(1.0, contrastOnWhite(InkHex("FFFFFF")), 1e-9)
        assertEquals(21.0, contrastOnWhite(InkHex("000000")), 1e-9)
    }

    @Test fun `given the pale prototype state then pale and a dark grey is not`() {
        assertTrue(isPale(InkHex("F2D7A6")))
        assertFalse(isPale(InkHex("505955")))
    }

    @Test fun `hue bands end at 360 and name by first limit above`() {
        assertEquals(13, HUE_BANDS.size)
        assertEquals(InkHueWord.RED, hueName(0.0))
        assertEquals(InkHueWord.ORANGE, hueName(12.0))
        assertEquals(InkHueWord.RED, hueName(359.9))
    }
}
