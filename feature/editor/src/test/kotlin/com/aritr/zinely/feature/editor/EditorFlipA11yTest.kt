package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.FlipAxis
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorFlipA11yTest {
    private val transform = Transform(0.0, 0.0, 20.0, 20.0)

    @Test
    fun `photo actions describe the current consequence and dispatch the shared reducer intent`() {
        val intents = mutableListOf<Intent>()
        val photo = ImageElement(
            id = "photo",
            transform = transform,
            assetId = "a".repeat(64),
            flippedHorizontally = true,
        )
        val actions = EditorA11y.elementCustomActions(photo, intents::add)
        val labels = actions.map { it.label }
        assertTrue(Copy.A11y.REMOVE_LEFT_RIGHT_FLIP in labels)
        assertTrue(Copy.A11y.FLIP_TOP_BOTTOM in labels)

        actions.single { it.label == Copy.A11y.FLIP_TOP_BOTTOM }.action()
        assertEquals(Intent.Select("photo"), intents[0])
        assertEquals(Intent.ToggleFlip("photo", FlipAxis.VERTICAL), intents[1])
    }

    @Test
    fun `art gets both flip actions while text and multi-selection mode get neither`() {
        val art = DecorElement("art", transform, supplyId = "shape.star", ink = ColorRgba.BLACK)
        val artLabels = EditorA11y.elementCustomActions(art, {}).map { it.label }
        assertTrue(Copy.A11y.FLIP_LEFT_RIGHT in artLabels)
        assertTrue(Copy.A11y.FLIP_TOP_BOTTOM in artLabels)

        val textLabels = EditorA11y.elementCustomActions(
            TextElement("text", transform, text = "words"),
            {},
        ).map { it.label }
        assertFalse(textLabels.any { "flip" in it.lowercase() })

        val multiLabels = EditorA11y.elementCustomActions(
            art,
            {},
            flipActionsEnabled = false,
        ).map { it.label }
        assertFalse(multiLabels.any { "flip" in it.lowercase() })
    }
}
