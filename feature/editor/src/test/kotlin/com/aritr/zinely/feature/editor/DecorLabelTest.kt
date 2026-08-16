package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.copy.Copy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What TalkBack says when focus lands on a placed supply.
 *
 * These exist because the previous implementation derived the label from the `supplyId` by splitting
 * it on the dot and speaking the halves — *"Rect shape"*, *"Corner fix"* — and **nothing failed**. It
 * was pure, total, non-empty, and wrong in every one of sixteen cases. A test that only asserts "some
 * non-empty string" would have passed then and passes now, so these assert against
 * [Copy.Supplies.NAMES] itself: the day someone reintroduces a derivation, the id and the name diverge
 * and this breaks.
 */
class DecorLabelTest {

    @Test
    fun `every authored supply is spoken by its authored name`() {
        // Given the sixteen supplies in :core:copy — When each id is labelled — Then the name is the copy's,
        // not a rearrangement of the id.
        Copy.Supplies.NAMES.forEach { (supplyId, name) ->
            assertEquals(
                "$supplyId must be spoken as the name Copy.Supplies gives it",
                name,
                EditorA11y.decorLabel(supplyId),
            )
        }
    }

    @Test
    fun `the two worst cases of the old derivation are named, as readable examples`() {
        // The assertion above is the real guard — it is exact, and any derivation breaks it. These two
        // exist so the next reader can see what the bug SOUNDED like without reconstructing it.
        assertEquals("Rectangle", EditorA11y.decorLabel("shape.rect")) // was "Rect shape"
        assertEquals("Photo corner", EditorA11y.decorLabel("fix.corner")) // was "Corner fix"
    }

    // Two guards were drafted here and deleted, because they were wrong rather than strict, and the way
    // they failed is worth more than they were:
    //
    //  · "no supply is spoken as a rearrangement of its id" failed on `tape.torn`, whose authored name
    //    genuinely IS "Torn tape" — the derivation happened to land on the right words for one of the
    //    sixteen. A rule banning a *coincidence* fails on the coincidence.
    //  · "the id prefix is never spoken" failed for the same reason: "Torn tape" contains "tape", and
    //    "Photo corner" would have been fine only by luck.
    //
    // Both were trying to re-derive the correct answer from the id in order to check it, which is the
    // exact mistake they were written to catch. Equality against [Copy.Supplies.NAMES] needs no such
    // reasoning: there is one authored source, and either the label came from it or it did not.

    @Test
    fun `an id outside the sixteen still says something rather than nothing`() {
        // Given a document the validator should have rejected — When it is labelled anyway — Then the
        // node is vague, not silent: an unlabelled semantic node is the worse failure.
        assertEquals(EditorA11y.DECOR_LABEL_FALLBACK, EditorA11y.decorLabel("not.a.supply"))
        assertEquals(EditorA11y.DECOR_LABEL_FALLBACK, EditorA11y.decorLabel(""))
        assertTrue(EditorA11y.decorLabel("").isNotBlank())
    }
}
