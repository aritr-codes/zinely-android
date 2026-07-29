package com.aritr.zinely.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The **★ AA gate** for the V2 chrome palette.
 *
 * [V2-TOKENS.md](docs/design/V2-TOKENS.md) marks six pairings AA-critical with a ★ and states the
 * rule that travels with them: *"Calm is not low-contrast-everywhere — the ★ pairings must clear
 * WCAG AA (body ≥ 4.5:1) in CI."* [V2-CONSTITUTION.md](docs/design/V2-CONSTITUTION.md) §III makes
 * the same point in stronger terms: AA on the ★ pairings is **gated in CI, not eyeballed**.
 *
 * | ★ pairing | Role |
 * |---|---|
 * | `ink` / `paper` | body and headings on the sheet |
 * | `inkSoft` / `paper` | secondary text, captions |
 * | `matchaText` / `paper` | matcha as text/icon on the sheet |
 * | `onMatcha` / `matcha` | the primary button's label on its fill |
 * | `strawberryText` / `paper` | strawberry when it must carry text |
 * | `consequence` / `paper` | delete / error text |
 *
 * Both themes, every pairing. Sub-AA tokens that are *deliberately* below the floor are recorded
 * separately at the bottom of this file, with the same reasoning as the V1 harness: pin the ratio so
 * a drift upward (a decorative token quietly becoming body text) fails just as loudly as a drift down.
 */
class ZinelyV2ContrastTest {

    private fun assertStarPairing(
        name: String,
        fg: (ZinelyV2Colors) -> Color,
        bg: (ZinelyV2Colors) -> Color,
    ) {
        val light = zinelyV2LightColors().let { WcagContrast.contrastRatio(fg(it), bg(it)) }
        val dark = zinelyV2DarkColors().let { WcagContrast.contrastRatio(fg(it), bg(it)) }
        assertTrue(
            "★ $name — light ${"%.3f".format(light)}:1 is below the AA body floor " +
                "${WcagContrast.AA_NORMAL}:1",
            light >= WcagContrast.AA_NORMAL,
        )
        assertTrue(
            "★ $name — dark ${"%.3f".format(dark)}:1 is below the AA body floor " +
                "${WcagContrast.AA_NORMAL}:1",
            dark >= WcagContrast.AA_NORMAL,
        )
    }

    @Test
    fun `ink on paper clears AA in both themes`() {
        assertStarPairing("ink / paper", { it.ink }, { it.paper })
    }

    @Test
    fun `inkSoft on paper clears AA in both themes`() {
        assertStarPairing("inkSoft / paper", { it.inkSoft }, { it.paper })
    }

    @Test
    fun `matchaText on paper clears AA in both themes`() {
        assertStarPairing("matchaText / paper", { it.matchaText }, { it.paper })
    }

    @Test
    fun `the primary button label clears AA on the matcha fill in both themes`() {
        // V2-TOKENS.md marks `matcha` "★ w/ its on-text" — the pairing, not the fill alone.
        assertStarPairing("onMatcha / matcha", { it.onMatcha }, { it.matcha })
    }

    @Test
    fun `strawberryText on paper clears AA in both themes`() {
        assertStarPairing("strawberryText / paper", { it.strawberryText }, { it.paper })
    }

    @Test
    fun `consequence on paper clears AA in both themes`() {
        assertStarPairing("consequence / paper", { it.consequence }, { it.paper })
    }

    // ----- recorded-accepted: below the body floor by design ----------------------------------

    // `strawberry` is deliberately NOT asserted here, in either direction.
    //
    // An earlier draft of this file asserted that plain `strawberry` must stay *below* the AA body
    // floor, reasoning that `strawberryText` exists precisely so `strawberry` is never used for copy.
    // That assertion failed on the frozen palette: dark `strawberry #D98289` on dark `paper #2F2A22`
    // measures 5.087:1 (light measures 2.128:1). The failure was the test's fault, not the token's.
    //
    // V2-TOKENS.md marks `strawberry`'s AA-critical column "—", which means *no floor is required* —
    // it does not mean a ceiling is imposed. The spec's actual rule is "accents, never actions",
    // which is a statement about what strawberry may *mean*, not about what contrast it may reach.
    // Dark paper is a warm charcoal, so a mid-pink legitimately pops against it; there is nothing to
    // fix. Keeping `strawberry` out of running text is a call-site discipline, not a contrast fact, and
    // encoding it as one here would have made the frozen HTML fail its own gate — which is the exact
    // inversion of authority this workflow forbids.
    //
    // An earlier version of this comment promised that discipline as "the token-discipline lint in A8".
    // A8 did not build it, and the promise was wrong rather than merely unkept: `TokenDisciplineTest`
    // (CI-27) gates raw *literals* — `.dp`, `.sp`, `Color(`, `RoundedCornerShape(` — in enrolled
    // packages. "This token is used for the wrong job" is a different question, needs a call site to be
    // asked about, and there are none until Phase B. Whether it is answerable statically at all is open.

    @Test
    fun `inkFaint on paper stays decorative and never rises into body-text territory`() {
        // V2-TOKENS.md: "faint/decorative only — not for body text". It carries no ★. Unlike
        // `strawberry` above, the ceiling here IS stated by the spec, so it is asserted: pinned from
        // both sides so the token can neither vanish nor drift up into readable-copy territory
        // without that being a deliberate promotion.
        for ((theme, colors) in listOf("light" to zinelyV2LightColors(), "dark" to zinelyV2DarkColors())) {
            val ratio = WcagContrast.contrastRatio(colors.inkFaint, colors.paper)
            assertTrue(
                "inkFaint / paper — $theme ${"%.3f".format(ratio)}:1 dropped below 2.0:1 " +
                    "(token drifted; re-open the design decision)",
                ratio >= 2.0f,
            )
            assertTrue(
                "inkFaint / paper — $theme ${"%.3f".format(ratio)}:1 rose to/above the AA body " +
                    "floor ${WcagContrast.AA_NORMAL}:1; V2-TOKENS.md documents it as never body " +
                    "text. If it is now a text token, promote it deliberately and move it above.",
                ratio < WcagContrast.AA_NORMAL,
            )
        }
    }
}
