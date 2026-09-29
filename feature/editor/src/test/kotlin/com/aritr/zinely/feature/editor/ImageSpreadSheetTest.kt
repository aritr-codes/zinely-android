package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.Fit
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.EditorReducer
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageSpreadSheetTest {
    private val size = PtSize(100.0, 140.0)
    private val full = Transform(0.0, 0.0, 100.0, 140.0)

    @Test
    fun fixed_physical_pairs_have_human_page_numbers() {
        assertEquals(8 to 1, imageSpreadPageNumbers(0))
        assertEquals(2 to 3, imageSpreadPageNumbers(2))
        assertEquals(4 to 5, imageSpreadPageNumbers(3))
        assertEquals(6 to 7, imageSpreadPageNumbers(6))
        assertNull(imageSpreadPageNumbers(8))
    }

    @Test
    fun recognises_both_sides_of_a_complementary_shared_asset_pair() {
        val left = ImageElement("left", full, assetId = "asset", crop = Crop(0.0, 0.1, 0.5, 0.9), fit = Fit.FIT)
        val right = ImageElement("right", full, assetId = "asset", crop = Crop(0.5, 0.1, 1.0, 0.9), fit = Fit.FIT)
        val pages = pages(page1 = left, page2 = right)

        assertEquals(SpreadInnerEdge.RIGHT, imageSpreadInnerEdge(pages, 1, left, size))
        assertEquals(SpreadInnerEdge.LEFT, imageSpreadInnerEdge(pages, 2, right, size))
    }

    @Test
    fun refuses_to_hide_a_keep_clear_edge_for_an_unrelated_photo() {
        val left = ImageElement("left", full, assetId = "asset-a", crop = Crop(0.0, 0.0, 0.5, 1.0), fit = Fit.FIT)
        val right = ImageElement("right", full, assetId = "asset-b", crop = Crop(0.5, 0.0, 1.0, 1.0), fit = Fit.FIT)

        assertNull(imageSpreadInnerEdge(pages(page1 = left, page2 = right), 1, left, size))
    }

    @Test
    fun recognises_the_exact_pair_written_by_the_reducer() {
        val source = ImageElement("photo", Transform(10.0, 10.0, 40.0, 50.0), assetId = "asset")
        val document = ZineDocument(
            format = ZineFormat.SINGLE_SHEET_8,
            paperSize = PaperSize.LETTER,
            pages = List(8) { index ->
                Page(
                    index = index,
                    role = PageRole.INTERIOR,
                    elements = if (index == 0) listOf(source) else emptyList(),
                )
            },
        )
        val after = EditorReducer.reduce(
            EditorModel(document = document, selection = setOf(source.id)),
            Intent.MakeImageSpread(source.id, photoAspect = 1080.0 / 2340.0, pageSizePt = size),
        ).model
        val right = after.document.pages[0].elements.single() as ImageElement
        val left = after.document.pages[7].elements.single() as ImageElement

        assertEquals(SpreadInnerEdge.LEFT, imageSpreadInnerEdge(after.document.pages, 0, right, size))
        assertEquals(SpreadInnerEdge.RIGHT, imageSpreadInnerEdge(after.document.pages, 7, left, size))
    }

    /** A flipped spread swaps which half each page holds (ADR-113); the fold cue must still recognise it. */
    @Test
    fun recognises_the_pair_the_reducer_writes_for_a_flipped_photo() {
        val source = ImageElement(
            "photo", Transform(10.0, 10.0, 40.0, 50.0), assetId = "asset", flippedHorizontally = true,
        )
        val document = ZineDocument(
            format = ZineFormat.SINGLE_SHEET_8,
            paperSize = PaperSize.LETTER,
            pages = List(8) { index ->
                Page(index = index, role = PageRole.INTERIOR, elements = if (index == 1) listOf(source) else emptyList())
            },
        )
        val after = EditorReducer.reduce(
            EditorModel(document = document, currentPageIndex = 1, selection = setOf(source.id)),
            Intent.MakeImageSpread(source.id, photoAspect = 1080.0 / 2340.0, pageSizePt = size),
        ).model
        val left = after.document.pages[1].elements.single() as ImageElement
        val right = after.document.pages[2].elements.single() as ImageElement

        assertEquals(SpreadInnerEdge.RIGHT, imageSpreadInnerEdge(after.document.pages, 1, left, size))
        assertEquals(SpreadInnerEdge.LEFT, imageSpreadInnerEdge(after.document.pages, 2, right, size))
    }

    /** Turning one half upside down afterwards breaks the seam, so the cue must come back. */
    @Test
    fun refuses_a_pair_whose_halves_disagree_on_the_top_bottom_flip() {
        val left = ImageElement("left", full, assetId = "asset", crop = Crop(0.0, 0.1, 0.5, 0.9), fit = Fit.FIT)
        val right = ImageElement(
            "right", full, assetId = "asset", crop = Crop(0.5, 0.1, 1.0, 0.9), fit = Fit.FIT,
            flippedVertically = true,
        )
        val pages = pages(page1 = left, page2 = right)

        assertNull(imageSpreadInnerEdge(pages, 1, left, size))
        assertNull(imageSpreadInnerEdge(pages, 2, right, size))
    }

    private fun pages(page1: ImageElement, page2: ImageElement): List<Page> =
        List(8) { index ->
            val elements = when (index) {
                1 -> listOf(page1)
                2 -> listOf(page2)
                else -> emptyList()
            }
            Page(index = index, role = PageRole.INTERIOR, elements = elements)
        }
}
