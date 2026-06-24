package com.aritr.zinely.render.android

import android.graphics.Typeface

/**
 * Resolves a [`TextStyle`][com.aritr.zinely.core.model.TextStyle]'s family + bold/italic to a single
 * [Typeface]. The **same** resolver instance must be used by every canvas provider and by the layout
 * tests, so a family resolves to the same glyphs and metrics in preview, export, and goldens — this is
 * what keeps wrapping identical across paths (ADR-028 §4.2, risk R2).
 *
 * The production resolver (G6) loads a fixed, self-covering bundled-font map from `assets/fonts/`
 * ([ADR-010](../DECISIONS.md#adr-010) / [ADR-001](../DECISIONS.md#adr-001)) so no device/system font is
 * ever reached for the MVP charset. [Default] is a deterministic system-family stand-in used until that
 * bundled map lands; it is sufficient for in-process layout-determinism tests but is **not** the
 * golden-grade resolver (system fonts vary by device — out of scope for G3, see §4.2).
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
