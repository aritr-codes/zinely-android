package com.aritr.zinely.render.android

import android.graphics.Typeface

/**
 * Resolves a [`TextStyle`][com.aritr.zinely.core.model.TextStyle]'s family + bold/italic to a single
 * [Typeface]. The **same** resolver instance must be used by every canvas provider and by the layout
 * tests, so a family resolves to the same glyphs and metrics in preview, export, and goldens — this is
 * what keeps wrapping identical across paths (ADR-028 §4.2, risk R2).
 *
 * The production resolver is [BundledFontResolver] (G6b): it loads a fixed, self-covering bundled-font
 * map from `assets/fonts/` ([ADR-010](../DECISIONS.md#adr-010) / [ADR-001](../DECISIONS.md#adr-001)) so
 * no device/system font is ever reached for the MVP charset. [Default] is a deterministic system-family
 * stand-in: sufficient for in-process layout-determinism tests but **not** golden-grade (system fonts
 * vary by device). S4/S5 wiring MUST inject [BundledFontResolver], never [Default] (§4.2, risk R2).
 */
public fun interface FontResolver {
    public fun resolve(fontFamily: String, bold: Boolean, italic: Boolean): Typeface

    public companion object {
        /** Deterministic system-family resolver (pre-bundled-font stand-in). */
        public val Default: FontResolver = FontResolver { fontFamily, bold, italic ->
            val style = when {
                bold && italic -> Typeface.BOLD_ITALIC
                bold -> Typeface.BOLD
                italic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
            Typeface.create(fontFamily, style)
        }
    }
}
