package com.aritr.zinely.core.editor.ink

import com.aritr.zinely.core.model.ColorRgba

/**
 * Six upper-case hex digits, no `#`. The one colour currency of the ink mixer, My inks and palettes
 * (ADR-138). Alpha is not part of an ink: [toColorRgba] is opaque and [of] ignores alpha.
 */
@JvmInline
public value class InkHex(public val value: String) {
    init {
        require(HEX6.matches(value)) { "not six upper-case hex digits: $value" }
    }

    public fun toColorRgba(): ColorRgba = ColorRgba(channel(0), channel(2), channel(4), 255)

    /** Prototype `spokenHex`: "5 0 5 9 5 5", so TalkBack reads digits, not a number. */
    public fun spoken(): String = value.toList().joinToString(" ")

    private fun channel(i: Int): Int = value.substring(i, i + 2).toInt(16)

    public companion object {
        public fun of(c: ColorRgba): InkHex =
            InkHex("%02X%02X%02X".format(java.util.Locale.ROOT, c.r and 0xFF, c.g and 0xFF, c.b and 0xFF))
    }
}

private val HEX6 = Regex("^[0-9A-F]{6}$")
private val HEX_ANY6 = Regex("^[0-9a-fA-F]{6}$")
private val HEX_ANY3 = Regex("^[0-9a-fA-F]{3}$")
/**
 * Prototype `stripHex`: whitespace, zero-width characters and `#` removed. Kotlin's `isWhitespace` is Unicode-wide
 * (a no-break space from a web paste counts), matching JS `\s`. Not a regex on purpose: Android's ICU rejects the
 * Unicode-character-class flag that the JVM accepts, so a regex here passed every JVM test and crashed the editor on a phone.
 */
public fun stripHex(raw: String): String =
    raw.filterNot { it.isWhitespace() || it in '​'..'‍' || it == '﻿' || it == '#' }

/**
 * Prototype `parseHex(raw, final)`: six digits always; three digits only when [final] (Done / blur), so
 * "505" mid-typing is not read as a purple. Callers cap the live field to six characters after [stripHex].
 */
public fun parseHex(raw: String, final: Boolean): InkHex? {
    val t = stripHex(raw)
    if (HEX_ANY6.matches(t)) return InkHex(t.uppercase())
    if (final && HEX_ANY3.matches(t)) return InkHex(t.map { "$it$it" }.joinToString("").uppercase())
    return null
}
