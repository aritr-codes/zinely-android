package com.aritr.zinely.core.editor.ink

import com.aritr.zinely.core.model.ColorRgba
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InkHexTest {
    @Test fun `given digits mid-typing and spaces when parsed not final then null`() =
        assertNull(parseHex(" #5 05​", final = false))

    @Test fun `given three digits when parsed final then doubled`() =
        assertEquals(InkHex("550055"), parseHex(" #5 05​", final = true))

    @Test fun `given lowercase six digits then upper`() =
        assertEquals(InkHex("ABCDEF"), parseHex("#abcdef", final = false))

    @Test fun `given a non hex digit then null`() = assertNull(parseHex("12345g", final = true))

    @Test fun `given seven digits then null`() = assertNull(parseHex("1234567", final = true))

    @Test fun `given fff not final then null`() = assertNull(parseHex("fff", final = false))

    @Test fun `given zero width characters then stripped`() {
        assertEquals("ABC", stripHex("A‌ B﻿#C‍"))
    }

    @Test fun `spoken spaces every digit`() = assertEquals("5 0 5 9 5 5", InkHex("505955").spoken())

    @Test fun `of ignores alpha`() = assertEquals(InkHex("010203"), InkHex.of(ColorRgba(1, 2, 3, 10)))

    @Test fun `toColorRgba is opaque`() = assertEquals(ColorRgba(255, 0, 16, 255), InkHex("FF0010").toColorRgba())

    @Test fun `given lowercase value then constructor rejects`() {
        assertThrows<IllegalArgumentException> { InkHex("abcdef") }
    }
}
