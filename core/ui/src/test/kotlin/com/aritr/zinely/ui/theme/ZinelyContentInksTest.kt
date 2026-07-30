package com.aritr.zinely.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the `content.*` namespace to the frozen V2 specs and guards the chrome/content separation.
 *
 * Literals are **transcribed, not parsed**, for the same reason as `ZinelyV2ColorsTest`: a test that
 * re-read the HTML would share any misreading with the implementation and would silently follow a
 * spec edit nobody noticed.
 */
class ZinelyContentInksTest {

    private val inks = zinelyContentInks()

    // ----- the four cover inks (ADR-069), v2-library.html:79-82 -------------------------------

    @Test
    fun `the cover ink set is exactly the four frozen inks, in the frozen order`() {
        assertEquals(
            listOf(
                ZinelyCoverInkId.Matcha,
                ZinelyCoverInkId.Teal,
                ZinelyCoverInkId.Strawberry,
                ZinelyCoverInkId.Ochre,
            ),
            inks.coverInks.map { it.id },
        )
    }

    @Test
    fun `each cover ink carries its frozen fill, title colour and band`() {
        fun ink(id: ZinelyCoverInkId) = inks.coverInks.single { it.id == id }

        ink(ZinelyCoverInkId.Matcha).let {
            assertEquals(Color(0xFF7C8A3F), it.fill)
            assertEquals(Color(0xFFF7F2E7), it.onFill)
            assertEquals(Color(0xFF4E5A26), it.band)
        }
        ink(ZinelyCoverInkId.Teal).let {
            assertEquals(Color(0xFF47857B), it.fill)
            assertEquals(Color(0xFFF7F2E7), it.onFill)
            assertEquals(Color(0xFF2E574E), it.band)
        }
        ink(ZinelyCoverInkId.Strawberry).let {
            assertEquals(Color(0xFFE27F89), it.fill)
            assertEquals(Color(0xFF4A211F), it.onFill)
            assertEquals(Color(0xFFC05863), it.band)
        }
        ink(ZinelyCoverInkId.Ochre).let {
            assertEquals(Color(0xFFD19A3C), it.fill)
            assertEquals(Color(0xFF3A2A0E), it.onFill)
            assertEquals(Color(0xFFA9741F), it.band)
        }
    }

    @Test
    fun `the paper cover stock matches the frozen block`() {
        assertEquals(Color(0xFFF1EBDA), inks.coverStock.fill)
        assertEquals(Color(0xFF2A251E), inks.coverStock.onFill)
    }

    // ----- the ten in-page maker inks (Bench H4), v2-bench.html:391 ---------------------------

    @Test
    fun `the maker set is exactly the ten frozen inks, in the frozen order`() {
        assertEquals(
            listOf(
                ZinelyMakerInkId.Matcha,
                ZinelyMakerInkId.Forest,
                ZinelyMakerInkId.Strawberry,
                ZinelyMakerInkId.Brick,
                ZinelyMakerInkId.Sunflower,
                ZinelyMakerInkId.Ochre,
                ZinelyMakerInkId.Aqua,
                ZinelyMakerInkId.Cornflower,
                ZinelyMakerInkId.Plum,
                ZinelyMakerInkId.Ink,
            ),
            inks.makerInks.map { it.id },
        )
    }

    @Test
    fun `each maker ink carries its frozen value`() {
        fun ink(id: ZinelyMakerInkId) = inks.makerInks.single { it.id == id }.value

        assertEquals(Color(0xFF7C8A3F), ink(ZinelyMakerInkId.Matcha))
        assertEquals(Color(0xFF3E5E3A), ink(ZinelyMakerInkId.Forest))
        assertEquals(Color(0xFFE27F89), ink(ZinelyMakerInkId.Strawberry))
        assertEquals(Color(0xFFB0503F), ink(ZinelyMakerInkId.Brick))
        assertEquals(Color(0xFFE7B53C), ink(ZinelyMakerInkId.Sunflower))
        assertEquals(Color(0xFFD19A3C), ink(ZinelyMakerInkId.Ochre))
        assertEquals(Color(0xFF57B0A9), ink(ZinelyMakerInkId.Aqua))
        assertEquals(Color(0xFF6E86C9), ink(ZinelyMakerInkId.Cornflower))
        assertEquals(Color(0xFF8A5A9B), ink(ZinelyMakerInkId.Plum))
        assertEquals(Color(0xFF2A251E), ink(ZinelyMakerInkId.Ink))
    }

    // ----- band 2: paper tints, v2-bench.html:392 ---------------------------------------------

    @Test
    fun `the paper tint band is exactly the five frozen tints, in the frozen order`() {
        assertEquals(
            listOf(
                ZinelyPaperTintId.Cream,
                ZinelyPaperTintId.Blush,
                ZinelyPaperTintId.Sky,
                ZinelyPaperTintId.Sage,
                ZinelyPaperTintId.Kraft,
            ),
            inks.paperTints.map { it.id },
        )
        assertEquals(Color(0xFFF1E9D6), inks[ZinelyPaperTintId.Cream].value)
        assertEquals(Color(0xFFF0DED9), inks[ZinelyPaperTintId.Blush].value)
        assertEquals(Color(0xFFDDE9EE), inks[ZinelyPaperTintId.Sky].value)
        assertEquals(Color(0xFFE1E9D2), inks[ZinelyPaperTintId.Sage].value)
        assertEquals(Color(0xFFE4D3B4), inks[ZinelyPaperTintId.Kraft].value)
    }

    @Test
    fun `the Cream paper tint is not the cover stock, near as they look`() {
        // Cream #F1E9D6 (an in-page tint) and the cover stock #F1EBDA differ by two channels. They
        // are separate values for separate jobs, and a near-miss like this is exactly the kind of
        // thing a transcription pass "corrects" into a single token by accident.
        assertNotEquals(inks.coverStock.fill, inks[ZinelyPaperTintId.Cream].value)
    }

    // ----- band 3: neutrals, v2-bench.html:393 ------------------------------------------------

    @Test
    fun `the neutral band is exactly the four frozen neutrals, in the frozen order`() {
        assertEquals(
            listOf(
                ZinelyNeutralId.Ink,
                ZinelyNeutralId.Slate,
                ZinelyNeutralId.Stone,
                ZinelyNeutralId.Fog,
            ),
            inks.neutrals.map { it.id },
        )
        assertEquals(Color(0xFF2A251E), inks[ZinelyNeutralId.Ink].value)
        assertEquals(Color(0xFF5B5347), inks[ZinelyNeutralId.Slate].value)
        assertEquals(Color(0xFF8C8269), inks[ZinelyNeutralId.Stone].value)
        assertEquals(Color(0xFFB7AD93), inks[ZinelyNeutralId.Fog].value)
    }

    @Test
    fun `the three sanctioned chrome-content value coincidences are pinned as intentional`() {
        // These are the values that would break a naive "no content value equals a chrome value"
        // lint. They are verbatim in the frozen source and are pinned here so that a future author
        // writing such a lint finds the exception list already documented and tested, rather than
        // discovering it as three mysterious failures and "fixing" a frozen value to make them go
        // away. See the D-003 ruling in the ZinelyContentInks KDoc.
        val chrome = zinelyV2LightColors()
        assertEquals(chrome.ink, inks[ZinelyNeutralId.Ink].value)          // #2A251E
        assertEquals(chrome.inkSoft, inks[ZinelyNeutralId.Slate].value)    // #5B5347
        assertEquals(chrome.inkFaint, inks[ZinelyNeutralId.Stone].value)   // #8C8269
    }

    @Test
    fun `Ink appears in both the ink and neutral bands, verbatim from the frozen source`() {
        // The frozen popover lists 'Ink' twice — once as a spot ink, once as a neutral. Not a
        // transcription slip; pinned so nobody de-duplicates it into one band and quietly changes
        // what the popover offers.
        assertEquals(inks[ZinelyMakerInkId.Ink].value, inks[ZinelyNeutralId.Ink].value)
    }

    // ----- the two sets are genuinely distinct axes, not one set drifting apart ---------------

    @Test
    fun `the cover and maker sets stay the distinct axes the frozen record describes`() {
        // V2-BENCH-REVIEW §8 / V2-BENCH-IA-INTERACTION: "The 4 cover inks stay for cover identity
        // (ADR-069); the in-page maker set is distinct and larger." The two specific values that
        // prove the sets are not converging: cover `teal` is not a maker ink, and maker `Aqua` is
        // not a cover ink. If either ever crossed over, the "distinct axes" reading — which is what
        // dissolves the apparent 4-vs-10 contradiction between V2-TOKENS.md and the Constitution —
        // would no longer be true, and the namespace design would need re-opening rather than
        // quietly becoming one set.
        val makerValues = inks.makerInks.map { it.value }.toSet()
        val coverFills = inks.coverInks.map { it.fill }.toSet()

        assertFalse(
            "cover teal #47857B has appeared in the maker set; the two ink axes are converging",
            Color(0xFF47857B) in makerValues,
        )
        assertFalse(
            "maker Aqua #57B0A9 has appeared among the cover fills; the two ink axes are converging",
            Color(0xFF57B0A9) in coverFills,
        )
        assertEquals("the cover set is frozen at four", 4, inks.coverInks.size)
        assertEquals("the maker set is frozen at ten", 10, inks.makerInks.size)
    }

    // ----- the chrome/content wall -------------------------------------------------------------

    @Test
    fun `the consequence red never enters the maker's supplies`() {
        // V2-BENCH-REVIEW §8: "`consequence` red never enters the maker set." A destructive/error
        // colour offered as an art supply would let a zine be painted in the one hue the interface
        // reserves for "this will remove". Asserted against BOTH themes' consequence values, since
        // either could be the one that leaks.
        //
        // Note this is deliberately narrow. A blanket "no content value equals any chrome value"
        // assertion would be wrong and would fail immediately: maker `Ink #2A251E` is *identical* to
        // light-theme chrome `ink`, and legitimately so — black ink is both what the interface writes
        // in and what a maker draws with. The rule the specs state is about `consequence`, and that
        // is the rule asserted.
        val contentValues: Set<Color> =
            inks.makerInks.map { it.value }.toSet() +
                inks.coverInks.flatMap { listOf(it.fill, it.onFill, it.band) } +
                setOf(inks.coverStock.fill, inks.coverStock.onFill)

        for ((theme, chrome) in listOf("light" to zinelyV2LightColors(), "dark" to zinelyV2DarkColors())) {
            assertFalse(
                "the $theme consequence red (${chrome.consequence}) is being offered as a maker ink",
                chrome.consequence in contentValues,
            )
        }
    }

    @Test
    fun `content inks are theme-invariant`() {
        // A printed cover does not restyle itself when the room goes dark. `zinelyContentInks()`
        // takes no theme parameter, so this cannot regress by a value edit — only by someone adding
        // a parameter. Asserting equality of two independent calls documents the intent and fails
        // loudly if a theme-dependent overload is ever introduced alongside it.
        assertEquals(zinelyContentInks(), zinelyContentInks())
    }

    // ----- contrast: the roadmap's Phase B "AA contrast per ink" gate, measured here -----------

    @Test
    fun `every cover title clears the ruled 3-to-1 floor on its own ink — see D-002`() {
        // COMPOSE-V2-ROADMAP Phase B lists contrast per ink as an impl-gate. The values live here, so
        // the measurement lives here too and Phase B inherits a gate rather than a promise.
        //
        // Measured (onFill on fill): Matcha 3.380 · Teal 3.832 · Strawberry 4.992 · Ochre 5.535.
        //
        // The floor is AA_LARGE (3.0:1), and it is now **settled by owner ruling** — D-002,
        // 2026-07-30: "The governing floor for cover titles is 3.0:1. No frozen colours change. No
        // HTML changes. No design amendment." So this assertion is the gate, not a placeholder for
        // one. What made it contested, kept because it explains why the ruling was needed:
        //
        //  - `.ct` is `font-size:1.16rem; font-weight:600` (v2-library.html:68) = ~18.56px semibold.
        //    WCAG's large-text threshold is 18.66px **bold**. 18.56 < 18.66, and 600 is not
        //    conventionally bold (700), so reading the cover title as large text was a stretch — on
        //    the 4.5:1 reading, Matcha and Teal would have failed.
        //  - Cover inks carry no ★ in V2-TOKENS.md, and the Constitution gates AA on ★ pairings.
        //    That is the reading the ruling followed.
        //
        // Raising this floor is an owner act, and it would make Matcha and Teal a spec change — in
        // the HTML, not here.
        for (ink in inks.coverInks) {
            val ratio = WcagContrast.contrastRatio(ink.onFill, ink.fill)
            assertTrue(
                "cover title on ${ink.id} — ${"%.3f".format(ratio)}:1 is below the AA large-text " +
                    "floor ${WcagContrast.AA_LARGE}:1",
                ratio >= WcagContrast.AA_LARGE,
            )
        }
    }

    @Test
    fun `D-002 is ruled — matcha and teal sit below the AA body floor and were deliberately left there`() {
        // D-002 is RESOLVED (owner ruling, 2026-07-30): the governing floor for cover titles is
        // 3.0:1, "no frozen colours change. No HTML changes. No design amendment." Matcha (3.380)
        // and Teal (3.832) clear that floor and were deliberately NOT amended.
        //
        // So this test's job changed with the ruling. It used to pin an open question so it could not
        // evaporate; it now pins the ruling's outcome so it cannot be undone by drift. The exact set
        // of inks sitting between 3.0 and 4.5 is a fact the owner chose — if it changes, either a
        // frozen value drifted (restore it) or someone amended the frozen Library without a new
        // ruling (which is an owner act, not an implementation one).
        //
        // Raising the floor here to AA_NORMAL is NOT the fix for a failure. That would overturn the
        // ruling from inside a test.
        val belowBodyFloor = inks.coverInks
            .filter { WcagContrast.contrastRatio(it.onFill, it.fill) < WcagContrast.AA_NORMAL }
            .map { it.id }

        assertEquals(
            "The set of cover inks below the AA body floor has changed. The D-002 ruling of " +
                "2026-07-30 fixed the cover-title floor at ${WcagContrast.AA_LARGE} and left these " +
                "two inks exactly as frozen, so this set is a ruled outcome, not a pending question. " +
                "Either a frozen value has drifted and must be restored, or the frozen Library was " +
                "amended — which requires a NEW owner ruling recorded in V2-SPEC-DEFECTS.md. Do not " +
                "raise the floor in the test above to make this pass.",
            listOf(ZinelyCoverInkId.Matcha, ZinelyCoverInkId.Teal),
            belowBodyFloor,
        )
    }

    @Test
    fun `the paper cover title clears the AA body floor on the stock`() {
        // Ink on an uninked cream stock is ordinary dark-on-light and should comfortably clear the
        // stricter body floor; if it ever does not, something is badly wrong with the stock value.
        val ratio = WcagContrast.contrastRatio(inks.coverStock.onFill, inks.coverStock.fill)
        assertTrue(
            "paper cover title — ${"%.3f".format(ratio)}:1 is below the AA body floor " +
                "${WcagContrast.AA_NORMAL}:1",
            ratio >= WcagContrast.AA_NORMAL,
        )
    }
}
