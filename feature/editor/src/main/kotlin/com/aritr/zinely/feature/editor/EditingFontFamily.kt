package com.aritr.zinely.feature.editor

import android.content.res.AssetManager
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.aritr.zinely.render.android.DocumentFontFamily
import com.aritr.zinely.render.android.DocumentFontRegistry

/** One face of an editing family: the registry's own asset path, and the style it stands for. */
internal data class EditingFace(val assetPath: String, val bold: Boolean, val italic: Boolean)

/**
 * The four faces the editing surface draws a text in: **the same four asset files the registry names**
 * for the family (ADR-126 decision 6).
 *
 * Pure, so a test can hold every registered family to its own paths. Before this, the draft was drawn in
 * the interface's Inter, which has no italic file, so Compose slanted the upright face and an italic
 * draft differed from the page, the preview and the PDF. With a second voice it would have differed for
 * every Book text.
 */
internal fun editingFaces(family: DocumentFontFamily): List<EditingFace> = listOf(
    EditingFace(family.regularAsset, bold = false, italic = false),
    EditingFace(family.boldAsset, bold = true, italic = false),
    EditingFace(family.italicAsset, bold = false, italic = true),
    EditingFace(family.boldItalicAsset, bold = true, italic = true),
)

/**
 * The thin seam that loads [editingFaces] for the family [fontFamily] resolves to. Resolution is the
 * registry's, so an unknown family lands on Inter here exactly as it does on the page.
 */
internal fun editingFontFamily(
    assets: AssetManager,
    fontFamily: String,
    registry: DocumentFontRegistry = DocumentFontRegistry.Bundled,
): FontFamily = FontFamily(
    editingFaces(registry.resolve(fontFamily)).map { face ->
        Font(
            path = face.assetPath,
            assetManager = assets,
            weight = if (face.bold) FontWeight.Bold else FontWeight.Normal,
            style = if (face.italic) FontStyle.Italic else FontStyle.Normal,
        )
    },
)
