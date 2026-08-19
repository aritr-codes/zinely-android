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
import org.junit.Assert.assertTrue
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
 * It was reachable only through a verb that shipped disabled — which is why §10.1 ruled that **S7 fixes the
 * routing, not the verb**.
 *
 * ✅ **The verb is now on**, and this file is the record of what that cost: one function widened, one colour
 * accessor added beside it, and *nothing else changed at any call site* — because the router, the popover's
 * visibility and the F-5 clearance term all read the binding rather than casting for themselves. That was
 * the explicit promise of the original fix, and enabling the verb is the experiment that tested it.
 *
 * The assertions below therefore come in pairs: what a supply gained, and what a **photo** did not. The
 * widening's real risk is not that decor was missed — it is that something else was swept in with it.
 */
class BenchInkTargetTest {

    private val transform = Transform(10.0, 10.0, 40.0, 20.0)

    @Test
    fun a_text_element_is_its_own_ink_target() {
        val text = TextElement(id = "t1", transform = transform, text = "Zine")
        assertSame(text, benchInkTargetOf(text))
    }

    @Test
    fun a_decor_element_is_its_own_ink_target_now_that_the_verb_is_live() {
        val decor = DecorElement(
            id = "d1",
            transform = transform,
            supplyId = "shape.rect",
            ink = ColorRgba(0x2A, 0x25, 0x1E),
        )
        // ⚠ **This assertion is inverted from the one it replaces**, which read `assertNull` because
        // `.inkpop` was text-only and no `Intent` recoloured a supply. Both of those are now false, so a
        // supply IS an ink target. The test kept its place in the file rather than being deleted because
        // the *shape* of the guard is unchanged — one function decides, and every call site obeys it.
        assertSame(decor, benchInkTargetOf(decor))
        // The colour half has to agree with the target half, or the popover opens ringing nothing.
        assertEquals(ColorRgba(0x2A, 0x25, 0x1E), benchInkColorOf(decor))
    }

    @Test
    fun nothing_selected_is_not_an_ink_target() {
        // The null case is the common one, and taking `Element?` is what stops each call site inventing its
        // own answer to it — which is how the three `as?` casts drifted apart.
        assertNull(benchInkTargetOf(null))
    }

    @Test
    fun the_decor_ink_verb_is_live_and_so_is_replace_now_that_it_has_a_flow() {
        val verbs = benchContextVerbs(BenchVerbKind.DECOR)

        val ink = verbs.single { it.label == Copy.BenchVerbs.INK }
        assertTrue("decor's Ink is live: Intent.InkSupply recolours a supply", ink.enabled)
        assertNull("a live verb must not claim a reason for being unavailable", ink.unavailableBecause)

        // ⚠ This half was `assertFalse` one package ago, and the flip was earned rather than assumed:
        // Replace is live **because** `Intent.ReplaceSupply` and the Art sheet's picker purpose now exist.
        // The `unavailableBecause` assertion is the one that matters — a verb enabled while still carrying
        // "not yet" would speak that reason to TalkBack over a control that works.
        val replace = verbs.single { it.label == Copy.BenchVerbs.REPLACE }
        assertTrue("Replace is live: the Art sheet re-opens as a picker", replace.enabled)
        assertNull("a live verb must not claim a reason for being unavailable", replace.unavailableBecause)
    }

    @Test
    fun a_photo_is_not_an_ink_target_and_has_no_ink_colour() {
        // The boundary that matters most after the widening: exactly ONE new kind became inkable. A change
        // that widened `benchInkTargetOf` to "anything non-null" would pass every decor assertion above.
        val image = ImageElement(id = "i1", transform = transform, assetId = "a1")
        assertNull(benchInkTargetOf(image))
        assertNull(benchInkColorOf(image))
        assertNull(benchInkColorOf(null))
    }
}
