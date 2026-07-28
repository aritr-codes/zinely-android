package com.aritr.zinely.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `every cover title clears the contested 3-to-1 floor on its own ink — see D-002`() {
        // COMPOSE-V2-ROADMAP Phase B lists "AA contrast per ink" as an impl-gate. The values live
        // here, so the measurement lives here too and Phase B inherits a gate rather than a promise.
        //
        // Measured (onFill on fill): Matcha 3.380 · Teal 3.832 · Strawberry 4.992 · Ochre 5.535.
        //
        // The floor asserted is AA_LARGE (3.0:1), and that choice is **contested rather than
        // settled** — it is logged as D-002 in V2-SPEC-DEFECTS.md for an owner ruling, and this
        // comment must not be read as having decided it:
        //
        //  - `.ct` is `font-size:1.16rem; font-weight:600` (v2-library.html:68) = ~18.56px semibold.
        //    WCAG's large-text threshold is 18.66px **bold**. 18.56 < 18.66, and 600 is not
        //    conventionally bold (700). So the cover title arguably does NOT qualify as large text,
        //    in which case the floor is 4.5:1 and **Matcha and Teal fail it**.
        //  - Cover inks carry no ★ in V2-TOKENS.md, and the Constitution gates AA on ★ pairings —
        //    so there is a defensible reading in which no CI floor is owed here at all.
        //
        // Asserting 3.0 is therefore the *minimum defensible* gate: it is strictly better than no
        // gate (a real regression still fails), and it does not silently pass off a value the owner
        // may want to change as though implementation had approved it. If the owner rules that the
        // stricter floor applies, this becomes 4.5 and the two failing inks become a spec change —
        // which is the owner's to make, in the HTML, not ours to make here.
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
    fun `D-002 is still open — matcha and teal cover titles remain below the AA body floor`() {
        // This test exists to make D-002 impossible to resolve *silently*, in either direction.
        //
        // A logged defect that nothing enforces tends to evaporate: the suite stays green, a later
        // session reads the green suite as "accessibility is fine", and the open question is quietly
        // lost. So the current, known-imperfect state is pinned as a fact. The moment the owner rules
        // — whether by darkening the fills, lightening the titles, or raising the title's
        // size/weight — this test FAILS, forcing whoever makes that change to come back here, delete
        // this test, raise the floor in the test above, and close D-002 in V2-SPEC-DEFECTS.md.
        //
        // A failure here is therefore not a regression. It is D-002 being answered, and the failure
        // message says so.
        val belowBodyFloor = inks.coverInks
            .filter { WcagContrast.contrastRatio(it.onFill, it.fill) < WcagContrast.AA_NORMAL }
            .map { it.id }

        assertEquals(
            "The set of cover inks below the AA body floor has changed. If a frozen ink or title " +
                "colour was deliberately amended to resolve D-002, that is the intended outcome: " +
                "close D-002 in V2-SPEC-DEFECTS.md, raise the floor in the test above to " +
                "${WcagContrast.AA_NORMAL}, and delete this test. If nothing was amended on purpose, " +
                "a frozen value has drifted and must be restored.",
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
