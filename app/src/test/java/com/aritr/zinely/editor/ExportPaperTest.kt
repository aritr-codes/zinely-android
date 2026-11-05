package com.aritr.zinely.editor

import com.aritr.zinely.core.data.repository.PaperColour
import com.aritr.zinely.core.data.repository.ProjectRepository
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.export.SheetExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** ADR-150 decision 8 / A35.8: a US Legal or A5 zine is exported on its own paper, whatever the switch holds. */
class ExportPaperTest {

    @Test
    fun a_legal_or_a5_zine_is_never_reimposed_onto_another_paper() {
        for (own in listOf(PaperSize.LEGAL, PaperSize.A5)) {
            for (chosen in PaperSize.entries) {
                assertEquals("$own chosen $chosen", own, exportPaper(own, chosen))
            }
        }
    }

    @Test
    fun an_a4_or_letter_zine_follows_the_two_way_switch() {
        for (own in listOf(PaperSize.A4, PaperSize.LETTER)) {
            assertEquals(PaperSize.A4, exportPaper(own, PaperSize.A4))
            assertEquals(PaperSize.LETTER, exportPaper(own, PaperSize.LETTER))
        }
    }

    @Test
    fun the_export_seam_never_receives_a_paper_colour() {
        // ADR-150 decision 5: the export takes a document, a page size, image bytes, a format and a destination.
        // None of them is or holds the paper colour, so the PDF and PNG are the same bytes for every colour.
        val seams = listOf(SheetExporter::class.java, com.aritr.zinely.export.ExportViewModel::class.java)
        val exports = seams.flatMap { seam -> seam.declaredMethods.filter { it.name == "export" || it.name == "retry" } }
        assertTrue(exports.isNotEmpty())
        val forbidden = setOf(PaperColour::class.java, ProjectRepository::class.java)
        exports.flatMap { it.parameterTypes.toList() }.forEach { type ->
            assertFalse("$type reaches the export", type in forbidden)
        }
        assertFalse(
            "the document must not hold the paper colour",
            (0 until ZineDocument.serializer().descriptor.elementsCount)
                .any { ZineDocument.serializer().descriptor.getElementName(it) == "paperColour" },
        )
    }

    @Test
    fun an_a4_or_letter_zine_is_never_moved_onto_legal_or_a5() {
        for (own in listOf(PaperSize.A4, PaperSize.LETTER)) {
            assertEquals(own, exportPaper(own, PaperSize.LEGAL))
            assertEquals(own, exportPaper(own, PaperSize.A5))
        }
    }
}
