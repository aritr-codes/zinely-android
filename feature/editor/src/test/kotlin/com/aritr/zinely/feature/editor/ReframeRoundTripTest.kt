package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.FramingMath
import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.Fit
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Transform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **Opening Reframe and committing without touching anything must not count as a change** —
 * [D-097](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-097), and the table
 * [ADR-109](../../../../../../../../docs/DECISIONS.md#adr-109) Consequence 5 mandates.
 *
 * ### Why this file is here and not in `core:editor`
 *
 * The predicate under test ([FramingMath.sameFraming]) lives in `core:editor`; the round trip that
 * produces the drift ([Framing.seedDraft] → [Framing.toImage]) lives here. A first version of this table
 * sat in `core:editor` and **re-implemented the round trip locally**, which made it a test of a copy: a
 * later edit to [Framing] could re-open D-097 with the table still green. It walks the real functions
 * instead, and that is the whole reason for the module it is in.
 *
 * ### The defect being defended against
 *
 * `seedDraft` recovers a *zoom* from a persisted crop (`cw0 / (right − left)`) and `resolveCrop` recovers
 * the *width* back from that zoom (`cw0 / zoom`). Dividing by a quotient does not return the numerator, so
 * an untouched session commits a crop 1–7 ULP from the one on disk. Both deciders compared [Crop]'s
 * `Double`s with `==`, so **56 % of untouched sessions** announced *"Framing saved."*, pushed an undo step
 * and autosaved.
 */
class ReframeRoundTripTest {

    private val page = 105.0 / 148.0

    private fun image(crop: Crop, fit: Fit) =
        ImageElement(id = "img", transform = Transform(0.0, 0.0, 100.0, 100.0), assetId = "a", crop = crop, fit = fit)

    @Test
    fun `given a panned or zoomed photo, when reframe is opened and committed untouched, then it is the same framing`() {
        // 3:2 and 16:9 are named on purpose: they are the aspects that FAIL under `==`. 4:3, 1:1 and 5:4
        // happen to round exactly, which is why D-097 survived a green suite — whether you see it depends
        // entirely on the aspect of the photo you happened to test with.
        val aspects = linkedMapOf(
            "3:2" to 3.0 / 2, "16:9" to 16.0 / 9, "4:3" to 4.0 / 3, "1:1" to 1.0,
            "2:3" to 2.0 / 3, "9:16" to 9.0 / 16, "5:4" to 5.0 / 4, "2:1" to 2.0, "10:1" to 10.0,
        )
        var checked = 0
        var drifted = 0
        for ((name, p) in aspects) {
            for (zoom in doubleArrayOf(1.0, 1.15, 1.3225, 1.5, 2.0, 2.5, 3.2, 4.0)) {
                val (mx, my) = Framing.panRange(FramingDraft(FrameFit.FILL, zoom, 0.0, 0.0), p, page)
                for (f in doubleArrayOf(0.0, 0.5, 1.0, -0.35)) {
                    // What the maker did, and what it persisted as.
                    val seeded = Framing.clampPan(FramingDraft(FrameFit.FILL, zoom, mx * f, my * f), p, page)
                    val persisted = Framing.toImage(image(Crop.FULL, Fit.FILL), seeded, p, page)
                    // The untouched-baseline case commits as `Crop.FULL`/`Fit.FILL` through `isBaseline`
                    // and is exact by construction — it is not the path D-097 lives on.
                    if (persisted.crop == Crop.FULL) continue

                    // Open Reframe. Touch nothing. Tap Done.
                    val after = Framing.toImage(persisted, Framing.seedDraft(persisted, p, page), p, page)
                    checked++
                    if (after.crop != persisted.crop || after.fit != persisted.fit) drifted++

                    assertTrue(
                        "$name at zoom $zoom pan-factor $f: an untouched reframe must not count as a " +
                            "change — was ${persisted.crop}, came back ${after.crop}",
                        FramingMath.sameFraming(after.crop, after.fit, persisted.crop, persisted.fit),
                    )
                }
            }
        }
        assertTrue("the table must actually be walked; only $checked cases ran", checked >= 250)
        // Non-vacuity, and the sharpest assertion in the file. The table above passes because
        // `sameFraming` absorbs the drift — NOT because the drift stopped existing. If this ever goes to
        // zero the round trip became exact, and every assertion above has been passing for a reason
        // nobody re-checked. That is exactly how D-097 hid inside a green suite in the first place.
        assertTrue(
            "expected the ULP drift to still be present in most cases; only $drifted of $checked drifted",
            drifted > checked / 4,
        )
    }

    @Test
    fun `given a real adjustment, when it is committed, then it is NOT the same framing`() {
        // The ceiling. Without this the test above could pass with a predicate that returns `true` always.
        val p = 3.0 / 2
        val a = Framing.toImage(image(Crop.FULL, Fit.FILL), FramingDraft(FrameFit.FILL, 1.5, 0.0, 0.0), p, page)
        val b = Framing.toImage(image(Crop.FULL, Fit.FILL), Framing.nudged(FramingDraft(FrameFit.FILL, 1.5, 0.0, 0.0), 1, 0, p, page), p, page)
        assertFalse(
            "one a11y nudge is a real change and must be recorded",
            FramingMath.sameFraming(a.crop, a.fit, b.crop, b.fit),
        )
    }

    @Test
    fun `given Whole photo chosen on a cropped image, when committed, then it IS a change`() {
        // The one path that legitimately resets a crop. `Fit` is compared exactly, so this must survive
        // the tolerance — if it ever did not, "Whole photo" would silently do nothing and say nothing.
        val p = 3.0 / 2
        val cropped = Framing.toImage(image(Crop.FULL, Fit.FILL), FramingDraft(FrameFit.FILL, 2.0, 0.1, 0.0), p, page)
        val whole = Framing.toImage(cropped, FramingDraft(FrameFit.WHOLE, 1.0, 0.0, 0.0), p, page)
        assertEquals(Crop.FULL, whole.crop)
        assertFalse(FramingMath.sameFraming(whole.crop, whole.fit, cropped.crop, cropped.fit))
    }

    // — the spoken outcome, whose polarity nothing else can reach —

    @Test
    fun `given an untouched reframe, when the outcome is spoken, then it says unchanged`() {
        // D-097's other half. The predicate is shared with the reducer and tested; this pins WHICH
        // sentence each branch produces. A flipped `if` here tells a TalkBack user their framing was saved
        // when it was not — and a sighted maker never sees this string, so nothing else would notice.
        val p = 3.0 / 2
        val persisted = Framing.toImage(image(Crop.FULL, Fit.FILL), FramingDraft(FrameFit.FILL, 1.15, 0.0, 0.0), p, page)
        val after = Framing.toImage(persisted, Framing.seedDraft(persisted, p, page), p, page)
        assertTrue("the fixture must exercise the drift", after.crop != persisted.crop)
        assertEquals(Copy.Editor.FRAMING_UNCHANGED, reframeOutcomeLine(after, persisted))
    }

    @Test
    fun `given a real adjustment, when the outcome is spoken, then it says saved`() {
        val p = 3.0 / 2
        val before = Framing.toImage(image(Crop.FULL, Fit.FILL), FramingDraft(FrameFit.FILL, 1.5, 0.0, 0.0), p, page)
        val nudged = Framing.nudged(FramingDraft(FrameFit.FILL, 1.5, 0.0, 0.0), 1, 0, p, page)
        val after = Framing.toImage(before, nudged, p, page)
        assertEquals(Copy.Editor.FRAMING_SAVED, reframeOutcomeLine(after, before))
    }

    @Test
    fun `given the two outcome lines, then they are not the same sentence`() {
        // Guards the degenerate pass: if the two Copy constants ever converged, both tests above would go
        // green while the app said one thing in both cases.
        assertTrue(Copy.Editor.FRAMING_SAVED != Copy.Editor.FRAMING_UNCHANGED)
    }
}
