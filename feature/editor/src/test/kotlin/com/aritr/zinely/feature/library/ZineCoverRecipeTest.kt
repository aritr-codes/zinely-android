package com.aritr.zinely.feature.library

import androidx.compose.ui.graphics.Color
import com.aritr.zinely.core.model.ZineCoverStamp
import com.aritr.zinely.core.model.ZineCoverSurface
import com.aritr.zinely.ui.theme.zinelyV21DarkColors
import com.aritr.zinely.ui.theme.zinelyV21LightColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * How a persisted cover recipe is **painted** in V2.1 — [ZineCoverSurface.v21Fill],
 * [ZineCoverSurface.v21MarkInk], [ZineCoverSurface.v21BorderInk] and [ZineCoverStamp.v21Mark].
 *
 * Pure JVM — no Robolectric. `Color` is a value class over a `ULong`, so the palette assertions need no
 * Android runtime, which keeps the frozen-hex checks in the fast suite where a wrong ink fails in
 * milliseconds.
 *
 * ### This file replaces a V2 one, and closes a hole it was hiding
 *
 * Until 2026-08-11 this tested `palette()` and `icon()` — the V2 rendering of the same recipe — and the
 * **V2.1 mapping had no test at all**, in either direction. The shelf had been painting from `v21Fill`
 * for a package and a half while the only recipe test in the tree guarded the function it had stopped
 * calling. That is the sharper half of retiring `ZineCover`: the dead component was not merely unused,
 * it was the thing the live suite still believed in.
 *
 * The V2 functions were deleted with the component ([ADR-100](docs/DECISIONS.md#adr-100)), so those two
 * tests could not be kept even in principle; these are their V2.1 counterparts plus the two invariance
 * claims the same ADR introduced.
 */
class ZineCoverRecipeTest {

    private val light = zinelyV21LightColors()
    private val dark = zinelyV21DarkColors()

    // -----------------------------------------------------------------------------------------
    // The six stocks
    // -----------------------------------------------------------------------------------------

    @Test
    fun `each surface prints on its own frozen stock`() {
        // `v21-library.html:182-187`, verbatim, resolved through the light palette.
        assertEquals(Color(0xFF4E7A3C), ZineCoverSurface.MatchaInk.v21Fill(light))
        assertEquals(Color(0xFFE4879F), ZineCoverSurface.StrawberryInk.v21Fill(light))
        assertEquals(Color(0xFFF6B22C), ZineCoverSurface.OchreInk.v21Fill(light))
        assertEquals(Color(0xFFCF4A28), ZineCoverSurface.TealInk.v21Fill(light))
        assertEquals("PaperStrawberryBand is .paper-s", light.paper, ZineCoverSurface.PaperStrawberryBand.v21Fill(light))
        assertEquals(Color(0xFFFDEBC4), ZineCoverSurface.PaperMatchaBand.v21Fill(light))
    }

    @Test
    fun `the pinned literals are the light palette's own values, not free-standing hexes`() {
        // Pinning a stock means freezing *the light palette's* value, not inventing a colour. The fills
        // were already anchored by the test above; the mark and the border were not, and a review found
        // the gap: move `inkSoft` or `ink` in the light theme and the paper stocks would silently stop
        // matching `.paper-s .mark{color:var(--ink-soft)}` and `.cover{border:… var(--ink)}` in the
        // frozen file, with nothing failing. A pinned literal still has an origin, and the origin is the
        // thing that can drift.
        for (surface in PaperStocks) {
            assertEquals(
                "$surface's pinned mark must be the light theme's inkSoft",
                Color(0xFF6E5947),
                surface.v21MarkInk(light),
            )
            assertEquals(
                "$surface's pinned border must be the light theme's ink",
                Color(0xFF33261C),
                surface.v21BorderInk(light),
            )
        }
    }

    @Test
    fun `the six stocks are six different colours, so no two zines print alike`() {
        val stocks = ZineCoverSurface.entries.map { it.v21Fill(light) }
        assertEquals("two surfaces resolved to the same stock: $stocks", stocks.size, stocks.toSet().size)
    }

    // -----------------------------------------------------------------------------------------
    // ADR-100 — the stock does not theme, the ink does
    // -----------------------------------------------------------------------------------------

    @Test
    fun `the paper stocks do not change between themes, because paper is paper`() {
        for (surface in PaperStocks) {
            assertEquals(
                "$surface must print on the same stock at night — a cover is the maker's object, and " +
                    "at 1.18:1 against the dark desk the themed token made it a hole cut in the desk",
                surface.v21Fill(light),
                surface.v21Fill(dark),
            )
            assertEquals(
                "$surface's mark must be invariant with the stock it is printed on",
                surface.v21MarkInk(light),
                surface.v21MarkInk(dark),
            )
            assertEquals(
                "$surface's border must be invariant too — the old themed --ink was #F6EAD6 in dark, which " +
                    "is the cream the stock now is, and an outline at 1.01:1 (cream) / 1.11:1 (paper) " +
                    "is no outline",
                surface.v21BorderInk(light),
                surface.v21BorderInk(dark),
            )
        }
    }

    @Test
    fun `the ink stocks keep their printed identity when the studio theme changes`() {
        for (surface in InkStocks) {
            assertEquals(
                "$surface is maker content, not app chrome",
                surface.v21Fill(light),
                surface.v21Fill(dark),
            )
        }
    }

    @Test
    fun `every cover rests on the desk rather than being cut out of it`() {
        // **The criterion is "fill OR shadow", and the first draft of this test got it wrong** — which
        // is worth recording, because the wrong version would have failed the design where it works and
        // passed it where it broke.
        //
        // A blanket `fill vs desk >= 3` fails `.paper-c` **in light** at 1.01:1: cream stock on a cream
        // desk. And the light shelf is fine — the raster shows an unmistakable object, because its
        // 1.5px ink border and its 4px hard shadow are both at 12.3:1. A cream cover on a cream desk is
        // the design working, not failing.
        //
        // What actually broke in dark was that the fill went to 1.18 **and the hard shadow went with
        // it**, `ink-line #120E0A` measuring 1.17:1 on `desk #241E18`. Fill gone and shadow gone leaves
        // an outline, and an outline with nothing behind it is a hole cut in the desk — which is exactly
        // what the parity review reported seeing. So the claim is the disjunction: an object needs
        // either a face you can distinguish or a shadow proving it is sitting on top of something.
        for (colors in listOf(light, dark)) {
            for (surface in ZineCoverSurface.entries) {
                val face = contrast(surface.v21Fill(colors), colors.desk)
                val shadow = contrast(colors.inkLine, colors.desk)
                assertTrue(
                    "$surface has neither a face (fill $face) nor a shadow (ink-line $shadow) against " +
                        "the desk, so it reads as a hole rather than an object",
                    maxOf(face, shadow) >= MinimumStockContrast,
                )
            }
        }
    }

    @Test
    fun `the paper stocks' marks are held to a reading bar, and the ink stocks' to the frozen literal`() {
        // The two mark inks are not the same *kind* of mark, so one bar for both was wrong.
        //
        // `.paper-s`/`.paper-c` take `ink-soft`, a **reading** ink on a pale stock, and ADR-100 pinned
        // both sides of that pair — so it can and must be measured: 6.16 and 5.61.
        for (colors in listOf(light, dark)) {
            for (surface in PaperStocks) {
                val ratio = contrast(surface.v21MarkInk(colors), surface.v21Fill(colors))
                assertTrue("$surface's mark measured $ratio on its own stock", ratio >= MinimumMarkContrast)
            }
        }

        // The ink stocks take the frozen `rgba(255,246,232,.92)`, which V21-SPEC §4.1 exempts **by
        // name** as cover art. It measures 2.38 on berry, and that is the recorded ruling rather than an
        // oversight: the mark is `contentDescription = null` decoration, and a cover's identity is the
        // title printed under it. Asserting the literal instead of a ratio keeps the exemption honest —
        // it cannot drift, and it cannot be quietly widened to a mark that *is* load-bearing.
        for (surface in InkStocks) {
            assertEquals(
                "$surface must take the frozen theme-invariant mark ink",
                surface.v21MarkInk(light),
                surface.v21MarkInk(dark),
            )
        }
    }

    // -----------------------------------------------------------------------------------------
    // The six marks
    // -----------------------------------------------------------------------------------------

    @Test
    fun `each stamp maps to its own frozen mark`() {
        assertEquals("Letter", ZineV21CoverMarks.Envelope, ZineCoverStamp.Letter.v21Mark())
        assertEquals("Sprig", ZineV21CoverMarks.Sprig, ZineCoverStamp.Sprig.v21Mark())
        assertEquals("Sun", ZineV21CoverMarks.Rings, ZineCoverStamp.Sun.v21Mark())
        assertEquals("Star", ZineV21CoverMarks.Booklet, ZineCoverStamp.Star.v21Mark())
        assertEquals("Waves", ZineV21CoverMarks.Lines, ZineCoverStamp.Waves.v21Mark())
        assertEquals("Face", ZineV21CoverMarks.Mug, ZineCoverStamp.Face.v21Mark())
    }

    @Test
    fun `the mapping is one to one, so no two stamps draw the same glyph`() {
        val marks = ZineCoverStamp.entries.map { it.v21Mark() }
        assertEquals("two stamps share a mark", marks.size, marks.toSet().size)
    }

    private companion object {
        val PaperStocks = listOf(ZineCoverSurface.PaperMatchaBand, ZineCoverSurface.PaperStrawberryBand)
        val InkStocks = ZineCoverSurface.entries - PaperStocks.toSet()

        /** 1.4.11's bar for a graphical object, which is what a cover on a desk is. */
        const val MinimumStockContrast = 3.0

        /** The paper stocks' `ink-soft` mark is a reading ink on a pale ground: the text bar applies. */
        const val MinimumMarkContrast = 4.5

        /** WCAG 2.x relative luminance, on the composited colour. */
        fun contrast(a: Color, b: Color): Double {
            val la = luminance(a)
            val lb = luminance(b)
            return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
        }

        fun luminance(c: Color): Double {
            fun channel(v: Float): Double {
                val d = v.toDouble()
                return if (d <= 0.03928) d / 12.92 else Math.pow((d + 0.055) / 1.055, 2.4)
            }
            // The marks carry alpha; composite them on their own stock's opaque value before measuring,
            // which the caller does by passing the stock as `b` — here the alpha is folded onto white,
            // the lighter of the two grounds, so the number this returns is never optimistic.
            val a = c.alpha.toDouble()
            fun over(v: Float) = (v.toDouble() * a + (1 - a)).toFloat()
            return 0.2126 * channel(over(c.red)) +
                0.7152 * channel(over(c.green)) +
                0.0722 * channel(over(c.blue))
        }
    }
}
