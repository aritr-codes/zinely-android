package com.aritr.zinely.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The **AA gate** for the V2.1 palette — this session's measurement, made permanent.
 *
 * [ADR-099](docs/DECISIONS.md#adr-099) opened `Proposed` with no contrast evidence, and §5 named the
 * absence a blocker rather than a follow-up. Measuring it found **eight failing pairings**
 * ([V21-SPEC.md §4.1](docs/design/V21-SPEC.md)). This file exists so that number can never again be
 * unknown, and so the eight fixes cannot silently regress.
 *
 * ### Why this test asserts more than the ★ pairings
 *
 * [ZinelyV2ContrastTest] gates six ★ pairings, which is what V2-TOKENS marked. V2.1's failures did
 * **not** arrive through the pairings anyone had marked:
 *
 * - three of the eight came from a sweep that read CSS `color` against CSS `background` **and
 *   nothing else** — it never looked at hardcoded literals or SVG `fill`/`stroke`, and so it reported
 *   "zero failing pairings" while `.seal` sat at 2.86:1 forty lines from a bug it had just caught;
 * - one was inside the single exception the spec had granted itself in writing;
 * - one was a *state* colour, and one was the fold guide's own outline.
 *
 * So the gate is deliberately wider than a starred list: **every pairing the corpus actually forms**,
 * both themes, plus pinned ratios for the tokens that are deliberately sub-AA. A decorative token
 * quietly becoming body text must fail as loudly as a token drifting darker.
 */
class ZinelyV21ContrastTest {

    private fun ratio(theme: (ZinelyV21Colors) -> Pair<Color, Color>, dark: Boolean): Float {
        val colors = if (dark) zinelyV21DarkColors() else zinelyV21LightColors()
        val (fg, bg) = theme(colors)
        return WcagContrast.contrastRatio(fg, bg)
    }

    private fun assertPairing(
        name: String,
        floor: Float,
        pair: (ZinelyV21Colors) -> Pair<Color, Color>,
    ) {
        listOf(false to "light", true to "dark").forEach { (dark, label) ->
            val r = ratio(pair, dark)
            assertTrue(
                "$name — $label ${"%.2f".format(r)}:1 is below the $floor:1 floor",
                r >= floor,
            )
        }
    }

    // ----- text on the grounds it actually lands on -----------------------------------------

    @Test
    fun `ink clears AA on every ground it is used on`() {
        assertPairing("ink / paper", WcagContrast.AA_NORMAL) { it.ink to it.paper }
        assertPairing("ink / desk", WcagContrast.AA_NORMAL) { it.ink to it.desk }
        assertPairing("ink / deskEdge", WcagContrast.AA_NORMAL) { it.ink to it.deskEdge }
        assertPairing("ink / paperEdge", WcagContrast.AA_NORMAL) { it.ink to it.paperEdge }
        assertPairing("ink / bench", WcagContrast.AA_NORMAL) { it.ink to it.bench }
        assertPairing("ink / butterTint", WcagContrast.AA_NORMAL) { it.ink to it.butterTint }
    }

    @Test
    fun `inkSoft clears AA as the corpus's secondary text tier`() {
        // 26 text rules moved here from inkFaint when inkFaint measured 3.04:1. See the class docs
        // on ZinelyV21Colors: the text ramp is two tiers, and this is the lower one.
        assertPairing("inkSoft / paper", WcagContrast.AA_NORMAL) { it.inkSoft to it.paper }
        assertPairing("inkSoft / desk", WcagContrast.AA_NORMAL) { it.inkSoft to it.desk }
        assertPairing("inkSoft / bench", WcagContrast.AA_NORMAL) { it.inkSoft to it.bench }
        assertPairing("inkSoft / butterTint", WcagContrast.AA_NORMAL) { it.inkSoft to it.butterTint }
    }

    @Test
    fun `the action colour clears AA as text and as a label on its own fill`() {
        assertPairing("leafText / paper", WcagContrast.AA_NORMAL) { it.leafText to it.paper }
        assertPairing("leafText / desk", WcagContrast.AA_NORMAL) { it.leafText to it.desk }
        assertPairing("leafText / bench", WcagContrast.AA_NORMAL) { it.leafText to it.bench }
        assertPairing("leafText / leafTint", WcagContrast.AA_NORMAL) { it.leafText to it.leafTint }
        assertPairing("onLeaf / leaf", WcagContrast.AA_NORMAL) { it.onLeaf to it.leaf }
    }

    @Test
    fun `jamText carries the consequence colour wherever jam itself cannot`() {
        // jam / paper is 4.20:1 — under the body floor. This trio is why jamText exists.
        assertPairing("jamText / paper", WcagContrast.AA_NORMAL) { it.jamText to it.paper }
        assertPairing("jamText / desk", WcagContrast.AA_NORMAL) { it.jamText to it.desk }
        assertPairing("jamText / bench", WcagContrast.AA_NORMAL) { it.jamText to it.bench }
        assertPairing("jamText / berryTint", WcagContrast.AA_NORMAL) { it.jamText to it.berryTint }
        assertPairing("onJam / jamText", WcagContrast.AA_NORMAL) { it.onJam to it.jamText }
    }

    @Test
    fun `onButter carries a label on the one hue that stays bright in both themes`() {
        // The gap ADR-100 left open and a review found: `onButter` was declared "★ AA-critical" in its own
        // KDoc, and this file — which that KDoc names as the thing pinning it — did not mention the token.
        // A cream label mirroring `onLeaf` would measure 1.6:1 on butter, so this pairing is the whole
        // reason the token exists and it was the one AA-critical pairing nothing checked.
        assertPairing("onButter / butter", WcagContrast.AA_NORMAL) { it.onButter to it.butter }
    }

    @Test
    fun `the snackbar reads in both themes`() {
        // Its ground is `ink`, which inverts. The Undo used to be butter here and measured 1.59:1 in
        // dark — V21-SPEC §3.2's one recorded exception, retired rather than lived with.
        assertPairing("paper / ink", WcagContrast.AA_NORMAL) { it.paper to it.ink }
    }

    // ----- meaning-bearing graphics (WCAG 1.4.11, 3:1) ---------------------------------------

    @Test
    fun `drawn lines and filled blocks clear the non-text floor`() {
        assertPairing("leaf / paper", WcagContrast.AA_LARGE) { it.leaf to it.paper }
        assertPairing("jam / paper", WcagContrast.AA_LARGE) { it.jam to it.paper }
        // A border is a drawn line and follows `ink`, never `inkLine`. inkLine as a border measured
        // 1.38:1 in dark across 61 borders; see ZinelyV21Colors. This asserts the corrected rule.
        assertPairing("ink border / paper", WcagContrast.AA_LARGE) { it.ink to it.paper }
        assertPairing("ink border / desk", WcagContrast.AA_LARGE) { it.ink to it.desk }
        assertPairing("ink border / bench", WcagContrast.AA_LARGE) { it.ink to it.bench }
    }

    // ----- deliberately sub-AA: pinned so a drift in EITHER direction fails --------------------

    private fun assertPinned(
        name: String,
        expectedLight: Float,
        expectedDark: Float,
        pair: (ZinelyV21Colors) -> Pair<Color, Color>,
    ) {
        assertEquals("$name (light) drifted", expectedLight, ratio(pair, dark = false), 0.01f)
        assertEquals("$name (dark) drifted", expectedDark, ratio(pair, dark = true), 0.01f)
    }

    @Test
    fun `inkFaint stays decorative and stays exactly where it was measured`() {
        // 3.04 clears 1.4.11 by 0.04. That margin is the reason `.search svg` may not move onto the
        // Bench worktop (it would be 2.30). Pinned in both directions: darkening it would silently
        // license it as text again, which is the defect this palette already had once.
        assertPinned("inkFaint / paper", 3.04f, 3.40f) { it.inkFaint to it.paper }
    }

    @Test
    fun `butter is material and is pinned below the text floor to keep it that way`() {
        // 1.73:1 in light. butter carries no action, no text and no state alone (V21-SPEC §3.2).
        // The Bench's favourite star is a butter fill inside an INK OUTLINE — the outline carries the
        // state and clears 1.4.11; butter alone never could.
        assertPinned("butter / paper", 1.73f, 7.35f) { it.butter to it.paper }
    }

    @Test
    fun `jam as text is pinned under the floor, which is why jamText exists`() {
        assertPinned("jam / paper", 4.20f, 4.55f) { it.jam to it.paper }
    }

    @Test
    fun `berry is punctuation, not text`() {
        assertPinned("berry / paper", 2.37f, 5.18f) { it.berry to it.paper }
    }

    @Test
    fun `inkLine is the shadow colour and is pinned as unusable for a border in dark`() {
        // This pair IS the trap, stated as numbers: in light, inkLine on paper is 13.66 — identical
        // to ink, because the two tokens hold the same hex there. Read light-only, a border drawn in
        // inkLine looks perfectly correct. In dark it collapses to 1.38, and 61 borders went with it.
        //
        // 1.38 is the RIGHT answer for a shadow and a disqualifying one for a line. If a future
        // change "fixes" this ratio, it has redefined what a printed shadow is; if it changes the
        // light value, the two tokens have stopped agreeing where they must.
        assertPinned("inkLine / paper", 13.66f, 1.38f) { it.inkLine to it.paper }
    }
}
