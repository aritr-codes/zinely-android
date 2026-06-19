package com.aritr.zinely.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PaperSizeTest {
    private val eps = 1e-6

    @Test
    fun `letter portrait is 612 x 792 points`() {
        assertEquals(612.0, PaperSize.LETTER.portrait.width, eps)
        assertEquals(792.0, PaperSize.LETTER.portrait.height, eps)
    }

    @Test
    fun `a4 portrait is 595_276 x 841_890 points`() {
        assertEquals(595.276, PaperSize.A4.portrait.width, eps)
        assertEquals(841.890, PaperSize.A4.portrait.height, eps)
    }

    @Test
    fun `landscape swaps width and height`() {
        val l = PaperSize.LETTER.landscape()
        assertEquals(792.0, l.width, eps)
        assertEquals(612.0, l.height, eps)
    }
}
