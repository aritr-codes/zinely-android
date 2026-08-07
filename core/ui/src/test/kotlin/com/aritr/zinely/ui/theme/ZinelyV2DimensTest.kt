package com.aritr.zinely.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins A4's shape and elevation work to the DESIGN-FROZEN V2 trilogy.
 *
 * Most of this class reads the frozen HTML directly rather than asserting against transcribed
 * constants, which is unusual and deliberate: A4's central claims are all claims about **absence** —
 * no radius scale, no spacing scale, no shadow ladder, a token that is never used. An absence cannot
 * be pinned by transcribing it, only by re-measuring the source. So these tests re-derive from the
 * HTML on every run, and they fail the moment the design corpus changes underneath a decision that
 * was made because of it.
 *
 * That also makes them the companions to **D-006** and **D-007** — both now **resolved by owner ruling**
 * (D-007 on 2026-07-28, D-006 at the Phase A closeout on 2026-07-30). Each is still pinned by a test, but
 * the pin has inverted with the ruling: it used to break the build the day the design answered the
 * question, and now breaks it the day someone undoes the answer.
 */
class ZinelyV2DimensTest {

    private val mockups = File("../../docs/design/mockups")

    private fun frozen(name: String): String {
        val f = File(mockups, name)
        assertTrue("expected to run with :core:ui as the working directory — missing $f", f.isFile)
        assertTrue("$name should contain a <style> block", f.readText().contains("</style>"))
        // CSS only. The JS below `</style>` contains geometry too, but it is behaviour rather than
        // shape, and mixing the two would make every count in this class unreproducible by hand.
        return f.readText().substringBefore("</style>")
    }

    private val library = frozen("v2-library.html")
    private val bench = frozen("v2-bench.html")
    private val proof = frozen("v2-proof.html")
    private val all = listOf(library, bench, proof)

    // -- the two globals --------------------------------------------------------------------------

    @Test
    fun `the hairline is 1dp and is by far the most common border weight`() {
        assertEquals(1.dp, ZinelyV2Dimens.Hairline)

        val widths = all.flatMap { css ->
            Regex("""border(?:-top|-bottom|-left|-right)?\s*:\s*([\d.]+)px""").findAll(css)
                .map { it.groupValues[1] }.toList()
        }.groupingBy { it }.eachCount()

        val hairlines = widths["1"] ?: 0
        val others = widths.filterKeys { it != "1" }.values.sum()
        assertTrue("expected 1px to be the dominant border weight, got $hairlines vs $others", hairlines > others)
    }

    @Test
    fun `the touch-target floor is the platform's, and the frozen controls do not meet it`() {
        // 48dp is the only value in ZinelyV2Dimens that is NOT transcribed from the frozen CSS —
        // it is the Android accessibility floor. The spec's silence about it is what D-009 records.
        assertEquals(48.dp, ZinelyV2Dimens.MinTouchTarget)

        // Every px `min-*` in the trilogy, with the selector it sits on. There is exactly one, and it
        // is the Bench's prototype caption reserving vertical space so its text cannot reflow the
        // page — scaffolding, not a control. Asserting the whole set rather than "zero" is what makes
        // this readable: a new entry appearing here is the notification that D-009 has been answered,
        // and the message will name the selector that answered it.
        val pxMinimums = all.flatMap { css ->
            Regex("""\.([\w-]+)[^{}]*\{[^{}]*?min-(?:width|height)\s*:\s*([\d.]+px)""")
                .findAll(css).map { "${it.groupValues[1]}=${it.groupValues[2]}" }.toList()
        }
        assertEquals(
            "the only px minimum in the trilogy is the prototype caption; no control declares one",
            listOf("caption=40px"),
            pxMinimums,
        )
    }

    @Test
    fun `the focus ring is 2dp and every focus rule in the trilogy agrees on that`() {
        assertEquals(2.dp, ZinelyV2Dimens.FocusRingWidth)

        val outlines = all.flatMap { css ->
            Regex("""outline\s*:\s*([\d.]+)px""").findAll(css).map { it.groupValues[1] }.toList()
        }
        assertTrue("the trilogy should specify at least one focus outline", outlines.isNotEmpty())
        assertEquals("every declared outline width is 2px", setOf("2"), outlines.toSet())
    }

    // -- D-006: the dead radius token, deleted from the corpus by owner ruling --------------------

    @Test
    fun `D-006 is resolved — the dead radius token is gone from the corpus and from here`() {
        // Ruled 2026-07-30: --r:18px was dead specification. The owner ordered it deleted from the
        // frozen Bench and Proof, and forbade an 18px radius token in the implementation. This test
        // inverted with the ruling: it used to pin the two declarations in place, and now pins their
        // absence, so a re-introduction fails the build instead of passing it.
        val declarations = all.count { it.contains("--r:18px") }
        assertEquals("--r was deleted from the trilogy by the D-006 ruling", 0, declarations)

        val references = all.sumOf { css -> Regex("""var\(\s*--r\s*\)""").findAll(css).count() }
        assertEquals(
            "--r is referenced nowhere; if this ever becomes non-zero the deletion has been reverted, " +
                "and reverting it is an owner act — not a licence for ZinelyV2Dimens to grow a radius",
            0,
            references,
        )

        val eighteens = all.sumOf { css ->
            Regex("""border-radius\s*:[^;}]*\b18px""").findAll(css).count()
        }
        assertEquals("no 18px radius exists in V2 even as a literal", 0, eighteens)
    }

    @Test
    fun `there is no radius scale to publish — the trilogy uses many radii on no ladder`() {
        val radii = all.flatMap { css ->
            Regex("""border-radius\s*:\s*([^;}]+)""").findAll(css).map { it.groupValues[1].trim() }.toList()
        }
        val distinct = radii.toSet()
        assertTrue(
            "expected a long tail of bespoke radii, found ${distinct.size}: $distinct",
            distinct.size >= 15,
        )
        // The specific disagreement that makes a shared "sheet radius" impossible: the Library's
        // bottom sheet and the Bench's are two different numbers for the same kind of object.
        assertTrue("library sheet radius is 20px", library.contains("border-radius:20px"))
        assertTrue("bench sheet radius is 22px on the top corners", bench.contains("border-radius:22px 22px 0 0"))
    }

    // -- D-007: the 8pt rhythm --------------------------------------------------------------------

    @Test
    fun `the frozen CSS does not keep the constitutional 8pt rhythm — and by ruling, never has to`() {
        val values = all.flatMap { css ->
            Regex("""(?:padding|margin|gap|row-gap|column-gap)(?:-top|-bottom|-left|-right)?\s*:\s*([^;}]+)""")
                .findAll(css)
                .flatMap { Regex("""(-?[\d.]+)px""").findAll(it.groupValues[1]).map { m -> m.groupValues[1].toDouble() } }
                .toList()
        }
        assertTrue("expected a large spacing sample, got ${values.size}", values.size > 200)

        val onEight = values.count { it != 0.0 && it % 8.0 == 0.0 }
        val fraction = onEight.toDouble() / values.size

        // Measured 17.1% over the whole CSS (16.7% over chrome alone, classified by hand). The
        // assertion pins the *fact* that an 8pt grid is not observable, not a precise ratio.
        //
        // D-007 is CLOSED: the owner ruled that §III is an implementation aspiration rather than a
        // token inventory, so no spacing scale is published and spacing stays per-component exactly as
        // frozen. This test therefore no longer guards an open question — it records the measurement
        // the ruling was made on, so that a later corpus edit toward a real grid is visible rather
        // than silent. The threshold is tighter than it looks: snapping the Bench alone reaches ~0.54
        // and the Proof ~0.49, but snapping only the Library lands at ~0.31 and clears 0.30 by a point.
        assertTrue(
            "an 8pt grid is still not observable in the frozen CSS (%.1f%% on-grid) — if this has " +
                "risen sharply, re-read D-007 before touching ZinelyV2Dimens".format(fraction * 100),
            fraction < 0.30,
        )
    }

    // -- the shadow primitive ---------------------------------------------------------------------

    @Test
    fun `a shadow layer carries spread, because V2 uses it in most of its shadows`() {
        // V1's ZinelyShadowLayer has no spread field: its spec never used one. V2's does, negatively,
        // which is the whole reason this is a separate type rather than a reuse.
        val shadows = all.flatMap { css ->
            Regex("""box-shadow\s*:\s*([^;}]+)""").findAll(css).map { it.groupValues[1].trim() }.toList()
        }.filter { it != "none" }
        val negativeSpread = shadows.count { Regex("""\s-[\d.]+px""").containsMatchIn(it) }
        // 27 until 2026-08-04, when [OD-22](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-053-ruling)
        // deleted `.pthumb i` from the frozen Bench. That rule drew its three placeholder page-lines *with*
        // a stacked `box-shadow` (`0 3px 0, 0 6px 0, 0 9px 0`) rather than with borders, so removing the
        // abstraction removed a shadow declaration with it. Zero-spread, which is why the count below is
        // unmoved. The drop is the amendment's arithmetic, not a corpus edit anyone made by hand.
        assertEquals("26 real box-shadow declarations across the trilogy", 26, shadows.size)
        assertEquals("22 of them carry spread", 22, negativeSpread)
        // Never once positive, and never offset horizontally — both absences justify the field list.
        assertEquals(
            "no shadow uses positive spread",
            0,
            shadows.count { Regex("""(?:^|,)\s*[-\d.]+px\s+[-\d.]+px\s+[-\d.]+px\s+[\d.]+px""").containsMatchIn(it) },
        )

        val layer = ZinelyV2ShadowLayer(dy = 20.dp, blur = 24.dp, spread = (-16).dp, color = Color.Black)
        assertEquals((-16).dp, layer.spread)
    }

    @Test
    fun `spread defaults to zero for the layers that omit it`() {
        assertEquals(0.dp, ZinelyV2ShadowLayer(dy = 2.dp, blur = 5.dp, color = Color.Black).spread)
    }

    @Test
    fun `a shadow layer may cast upward, because three frozen surfaces do`() {
        // Bench `.sheet`, Proof `.band`, Proof `.drawer` — all sit at the bottom of the screen and
        // throw their shadow onto the content above. A Dp is signed, so this is really a check that
        // nothing in the type or its docs treats upward as impossible.
        val upward = all.sumOf { css ->
            Regex("""box-shadow\s*:\s*0\s+-\d""").findAll(css).count()
        }
        assertEquals("bench .sheet, proof .band, proof .drawer", 3, upward)
        assertEquals((-14).dp, ZinelyV2ShadowLayer(dy = (-14).dp, blur = 40.dp, color = Color.Black).dy)
    }

    @Test
    fun `there is no shadow ladder to publish — every V2 shadow is written at its use site`() {
        // V1 declared --shadow-1/-2/-lift in :root. V2 declares no shadow definition token at all;
        // its shadow-ish tokens (shadow, contact, hair, frameShadow) are colours, and they are
        // already owned by ZinelyV2Colors. If a ladder ever appears in :root, this fails and the
        // elevation model must be revisited rather than extended.
        val ladderTokens = all.sumOf { css ->
            Regex("""--shadow-(?:1|2|lift)\s*:""").findAll(css).count()
        }
        assertEquals("V2 declares no shadow-ladder tokens", 0, ladderTokens)
    }
}
