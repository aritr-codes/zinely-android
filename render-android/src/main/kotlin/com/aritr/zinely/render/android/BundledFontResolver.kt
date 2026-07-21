package com.aritr.zinely.render.android

import android.content.res.AssetManager
import android.graphics.Typeface

/**
 * The golden-grade [FontResolver] (ADR-028 §4.2): resolves a document's family + bold/italic to a
 * **bundled** [Typeface] loaded from `assets/fonts/` ([ADR-010](../DECISIONS.md#adr-010) OFL;
 * [ADR-001](../DECISIONS.md#adr-001) reproducible vector PDF text), so a family resolves to the **same
 * glyphs and metrics** in preview, export, and goldens — the load-bearing condition for
 * `preview == export` wrapping (risk R2). Unlike [FontResolver.Default] (which calls `Typeface.create`
 * and so can pick a device-variable system font), this never reaches a system font for the MVP charset
 * ([MvpCharset], guarded by `FontCoverageTest`).
 *
 * The four faces per family are real TTFs — not synthesised bold/italic, and not a variable font whose
 * weight axis needs API 26+ (`minSdk` is 24, [ADR-024](../DECISIONS.md#adr-024)).
 *
 * **Family resolution is the [DocumentFontRegistry]'s (F3), not this class's.** This resolver owns only
 * the asset→[Typeface] step; *which* family a document's `fontFamily` means is the registry's single
 * source of truth, shared by every canvas provider. Previously this class hard-coded one family and
 * discarded `fontFamily` entirely, so every document rendered in Inter whatever it claimed.
 *
 * Faces are loaded **eagerly** for every registered family and reused. Eager is deliberate: a missing or
 * corrupt asset then fails at construction, where it is a wiring bug, rather than part-way through an
 * export where it would be a half-rendered zine.
 *
 * **S4/S5 wiring obligation (Codex):** the editor preview host and the export pipeline MUST inject this
 * resolver into their [CanvasReplayer]; falling back to [FontResolver.Default] would reintroduce the
 * exact cross-path drift this class removes.
 *
 * @param assets the app (or test) [AssetManager] whose merged assets contain the registry's font files.
 * @param registry the document-typography source of truth; defaults to what this build bundles.
 */
public class BundledFontResolver(
    assets: AssetManager,
    private val registry: DocumentFontRegistry = DocumentFontRegistry.Bundled,
) : FontResolver {

    /** One [Faces] per registered family, loaded once at construction (see the eager-loading note). */
    private val faces: Map<String, Faces> = registry.families.associate { family ->
        family.name to Faces(
            regular = Typeface.createFromAsset(assets, family.regularAsset),
            bold = Typeface.createFromAsset(assets, family.boldAsset),
            italic = Typeface.createFromAsset(assets, family.italicAsset),
            boldItalic = Typeface.createFromAsset(assets, family.boldItalicAsset),
        )
    }

    override fun resolve(fontFamily: String, bold: Boolean, italic: Boolean): Typeface {
        // The registry decides the family (falling back to its default for an unregistered name); this
        // class only picks the face. Keeping the two steps separate is what lets every surface agree on
        // family resolution while each still owns its own canvas.
        val family = registry.resolve(fontFamily)
        val f = faces.getValue(family.name)
        return when {
            bold && italic -> f.boldItalic
            bold -> f.bold
            italic -> f.italic
            else -> f.regular
        }
    }

    private class Faces(
        val regular: Typeface,
        val bold: Typeface,
        val italic: Typeface,
        val boldItalic: Typeface,
    )
}
