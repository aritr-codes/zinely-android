package com.aritr.zinely.feature.editor

import com.aritr.zinely.render.android.DocumentFontRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **Every registered family is edited in its own four files (ADR-126 decision 6).** A family added to the
 * registry is held to this without touching the test: the draft a maker types into must be the faces the
 * page, the preview and the PDF draw. Plain JVM.
 */
class EditingFontFamilyTest {

    @Test
    fun `each registered family's editing faces are the registry's own asset paths, one per style`() {
        val families = DocumentFontRegistry.Bundled.families
        assertTrue("the registry holds Book as well as Plain", families.size >= 2)
        for (family in families) {
            assertEquals(
                family.name,
                listOf(
                    EditingFace(family.regularAsset, bold = false, italic = false),
                    EditingFace(family.boldAsset, bold = true, italic = false),
                    EditingFace(family.italicAsset, bold = false, italic = true),
                    EditingFace(family.boldItalicAsset, bold = true, italic = true),
                ),
                editingFaces(family),
            )
            // Four different files that exist: no style quietly falls back to a synthesised face.
            assertEquals(family.name, 4, editingFaces(family).map { it.assetPath }.distinct().size)
            editingFaces(family).forEach {
                assertTrue("${it.assetPath} is not bundled", File("../../render-android/src/main/assets/${it.assetPath}").isFile)
            }
        }
    }
}
