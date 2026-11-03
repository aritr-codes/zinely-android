package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.Element
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** ADR-135 decisions 2-3: what each Read stop says, which pieces get none, and the two empty-page stops. */
class ReadStopsTest {
    private val size = PtSize(200.0, 300.0)

    private fun page(vararg e: Element) = Page(1, PageRole.INTERIOR, elements = e.toList())
    private fun at(x: Double, y: Double, w: Double = 40.0, h: Double = 30.0) = Transform(x, y, w, h)
    private fun text(id: String, t: String, x: Double, y: Double) = TextElement(id, at(x, y), text = t)
    private fun photo(id: String, x: Double, y: Double, d: String? = null, h: Double = 30.0) =
        ImageElement(id, at(x, y, h = h), assetId = "a".repeat(64), description = d)
    private fun art(id: String, x: Double, y: Double, d: String? = null) =
        DecorElement(id, at(x, y), supplyId = "tape.torn", ink = ColorRgba.BLACK, description = d)

    private fun labels(vararg e: Element) = readStops(page(*e), size).map { it.label }

    @Test
    fun text_is_its_words_and_a_photo_says_Photo_in_the_Benchs_own_order() {
        assertEquals(
            listOf("first", "Photo", "last"),
            labels(text("c", "last", 20.0, 200.0), photo("b", 100.0, 20.0), text("a", "first", 20.0, 20.0)),
        )
        assertTrue(readStops(page(text("a", "x", 0.0, 0.0)), size).single().isText)
        assertTrue(!readStops(page(photo("p", 0.0, 0.0)), size).single().isText)
    }

    @Test
    fun a_described_photo_and_described_Art_say_what_the_Bench_says() {
        assertEquals(
            listOf("Photo: Mum at the gate", "Torn tape: this way"),
            labels(photo("p", 20.0, 20.0, "Mum at the gate"), art("d", 100.0, 20.0, "this way")),
        )
    }

    @Test
    fun blank_text_undescribed_Art_and_a_blank_description_get_no_stop() {
        assertEquals(
            listOf("Photo"),
            labels(text("t", "   ", 20.0, 20.0), art("d", 100.0, 20.0), art("e", 100.0, 80.0, "  "), photo("p", 20.0, 120.0, "  ")),
        )
    }

    @Test
    fun a_piece_taller_than_six_tenths_of_the_page_is_read_first() {
        assertEquals(
            listOf("Photo", "top"),
            labels(text("t", "top", 20.0, 5.0), photo("big", 100.0, 40.0, h = 200.0)),
        )
    }

    @Test
    fun pieces_with_no_part_on_the_page_get_no_stop() {
        assertEquals(listOf("Photo"), labels(photo("off", 500.0, 20.0), photo("on", 20.0, 20.0), text("o", "gone", 20.0, -100.0)))
        // Partly on the page still counts.
        assertEquals(listOf("hang"), labels(text("h", "hang", 180.0, 20.0)))
    }

    @Test
    fun an_empty_page_is_Blank_page_or_Only_Art_when_Art_is_drawn_on_it() {
        assertEquals(listOf(Copy.ProofRead.BLANK_PAGE), labels())
        assertEquals(listOf(Copy.ProofRead.BLANK_PAGE), labels(text("t", " ", 20.0, 20.0)))
        assertEquals(listOf(Copy.ProofRead.ONLY_ART), labels(art("d", 20.0, 20.0), text("t", "", 20.0, 120.0)))
    }

    @Test
    fun Art_wholly_off_the_page_is_left_out_before_the_empty_page_test() {
        assertEquals(listOf(Copy.ProofRead.BLANK_PAGE), labels(art("d", 900.0, 20.0)))
        val stop = readStops(page(art("d", 900.0, 20.0)), size).single()
        assertEquals(READ_EMPTY_PAGE_STOP_ID, stop.id)
        assertEquals(200.0, stop.widthPt, 0.0)
    }
}
