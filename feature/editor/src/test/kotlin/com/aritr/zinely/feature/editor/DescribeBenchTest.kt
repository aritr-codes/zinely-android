package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Bench A52.1 and A52.5 (ADR-147): where Describe sits on each bar, and what the Bench node says. */
class DescribeBenchTest {
    private val box = Transform(0.0, 0.0, 50.0, 40.0)
    private val photo = ImageElement("p", box, assetId = "a".repeat(64))
    private val art = DecorElement("d", box, supplyId = "tape.torn", ink = ColorRgba.BLACK)

    @Test
    fun the_photo_bar_gains_Describe_after_Replace_and_before_Duplicate() {
        assertEquals(
            listOf("Reframe", "Arrange", "Flip", "Across fold", "Copier", "Replace", "Describe", "Duplicate", "Delete"),
            benchContextVerbs(BenchVerbKind.PHOTO).map { it.label },
        )
    }

    @Test
    fun the_Art_bar_gains_Describe_after_Flip_and_before_Duplicate() {
        assertEquals(
            listOf("Replace", "Arrange", "Ink", "Flip", "Describe", "Duplicate", "Delete"),
            benchContextVerbs(BenchVerbKind.DECOR).map { it.label },
        )
    }

    @Test
    fun text_has_no_Describe() {
        assertTrue(benchContextVerbs(BenchVerbKind.TEXT).none { it.label == Copy.BenchVerbs.DESCRIBE })
    }

    @Test
    fun the_bench_node_says_what_Read_says() {
        assertEquals("Photo", EditorA11y.label(photo))
        assertEquals("Photo: Mum at the gate", EditorA11y.label(photo.copy(description = "Mum at the gate")))
        assertEquals("Torn tape", EditorA11y.label(art))
        val name = EditorA11y.label(art)
        assertEquals("$name: this way", EditorA11y.label(art.copy(description = "this way")))
    }

    @Test
    fun a_blank_description_is_none() {
        assertEquals("Photo", EditorA11y.label(photo.copy(description = "   ")))
        assertEquals(EditorA11y.label(art), EditorA11y.label(art.copy(description = "")))
    }

    @Test
    fun the_Describe_action_is_added_only_with_a_handler_and_never_on_text() {
        val none = EditorA11y.elementCustomActions(photo, dispatch = {})
        assertTrue(none.none { it.label == Copy.A11y.DESCRIBE })

        val opened = mutableListOf<String>()
        val dispatched = mutableListOf<Intent>()
        val withHandler = EditorA11y.elementCustomActions(photo, dispatch = { dispatched += it }, onDescribe = { opened += it })
        assertEquals(1, withHandler.count { it.label == "Describe" })
        withHandler.single { it.label == "Describe" }.action()
        assertEquals(listOf("p"), opened)
        // It selects the piece first, as the other actions do.
        assertEquals(listOf<Intent>(Intent.Select("p")), dispatched)

        val artActions = EditorA11y.elementCustomActions(art, dispatch = {}, onDescribe = {})
        assertEquals(1, artActions.count { it.label == "Describe" })
        val text = TextElement("t", box, text = "words")
        assertTrue(EditorA11y.elementCustomActions(text, dispatch = {}, onDescribe = {}).none { it.label == "Describe" })
    }
}
