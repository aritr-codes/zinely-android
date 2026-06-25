package com.aritr.zinely.render.android

import android.content.res.AssetManager
import android.graphics.Typeface

/**
 * The golden-grade [FontResolver] (ADR-028 §4.2): resolves every family to a **bundled** Inter
 * [Typeface] loaded from `assets/fonts/` ([ADR-010](../DECISIONS.md#adr-010) OFL; [ADR-001](../DECISIONS.md#adr-001)
 * reproducible vector PDF text), so a family resolves to the **same glyphs and metrics** in preview,
 * export, and goldens — the load-bearing condition for `preview == export` wrapping (risk R2). Unlike
 * [FontResolver.Default] (which calls `Typeface.create` and so can pick a device-variable system font),
 * this never reaches a system font for the MVP charset ([MvpCharset], guarded by `FontCoverageTest`).
 *
 * The four static styles are real TTFs (not synthesised bold/italic, and not a variable font whose
 * weight axis needs API 26+ — minSdk is 24, [ADR-024](../DECISIONS.md#adr-024)), each loaded once and
 * reused. MVP bundles a single family, so every [fontFamily] (known or unknown) maps to Inter; when
 * more families are bundled (ROADMAP V1 typography), this becomes a real name→family map with Inter as
 * the unknown-family default.
 *
 * **S4/S5 wiring obligation (Codex):** the editor preview host and the export pipeline MUST inject this
 * resolver into their [CanvasReplayer]; falling back to [FontResolver.Default] would reintroduce the
 * exact cross-path drift this class removes.
 *
 * @param assets the app (or test) [AssetManager] whose merged assets contain `fonts/Inter-*.ttf`.
 */
public class BundledFontResolver(assets: AssetManager) : FontResolver {

    private val regular = Typeface.createFromAsset(assets, "fonts/Inter-Regular.ttf")
    private val bold = Typeface.createFromAsset(assets, "fonts/Inter-Bold.ttf")
    private val italic = Typeface.createFromAsset(assets, "fonts/Inter-Italic.ttf")
    private val boldItalic = Typeface.createFromAsset(assets, "fonts/Inter-BoldItalic.ttf")

    override fun resolve(fontFamily: String, bold: Boolean, italic: Boolean): Typeface = when {
        bold && italic -> boldItalic
        bold -> this.bold
        italic -> this.italic
        else -> regular
    }
}
