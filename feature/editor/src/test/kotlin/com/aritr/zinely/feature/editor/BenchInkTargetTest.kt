package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * [benchInkTargetOf] — the carried defect SUPPLIES-SPEC §10.1 names in its S7 row.
 *
 * `EditorScreen`'s `Ink` verb opened `.inkpop` unconditionally while the popover resolved its own target
 * with `ctxElement as? TextElement`. For a [DecorElement] the two disagreed, and the disagreement was not a
 * cosmetic one: `ctxVisible` carries `!inkPopoverOpen`, so the verb bar stood down, the popover never
 * appeared in its place, `Done` went disabled and the bottom bar was already captioned for an ink session.
 * Nothing on screen could act.
 *
 * It was reachable only through a verb that ships disabled — which is why §10.1 rules that **S7 fixes the
 * routing, not the verb**. Both halves are asserted here: the routing decision is a pure function with one
 * definition, and the verb is still off.
 */
class BenchInkTargetTest {

    private val transform = Transform(10.0, 10.0, 40.0, 20.0)

    @Test
    fun a_text_element_is_its_own_ink_target() {
        val text = TextElement(id = "t1", transform = transform, text = "Zine")
        assertSame(text, benchInkTargetOf(text))
    }

    @Test
    fun a_decor_element_has_no_ink_target_so_the_verb_cannot_open_an_empty_popover() {
        val decor = DecorElement(
            id = "d1",
            transform = transform,
            supplyId = "shape.rect",
            ink = ColorRgba(0x2A, 0x25, 0x1E),
        )
        // `.inkpop` is text-only by construction (`BenchInkPopover`'s own call site says so), and decor's
        // ink needs a band set and an `Intent` that recolours a DecorElement — neither exists yet.
        assertNull(benchInkTargetOf(decor))
    }

    @Test
    fun a_photo_has_no_ink_target_either() {
        val image = ImageElement(id = "i1", transform = transform, assetId = "a1")
        assertNull(benchInkTargetOf(image))
    }

    @Test
    fun nothing_selected_is_not_an_ink_target() {
        // The null case is the common one, and taking `Element?` is what stops each call site inventing its
        // own answer to it — which is how the three `as?` casts drifted apart.
        assertNull(benchInkTargetOf(null))
    }

    @Test
    fun the_decor_ink_verb_is_still_disabled_and_the_fix_did_not_enable_it() {
        val ink = benchContextVerbs(BenchVerbKind.DECOR).single { it.label == Copy.BenchVerbs.INK }
        assertFalse("S7 fixes the routing, not the verb — decor's Ink stays drawn and inert", ink.enabled)
        assertEquals(Copy.BenchVerbs.NOT_YET, ink.unavailableBecause)
    }
}
