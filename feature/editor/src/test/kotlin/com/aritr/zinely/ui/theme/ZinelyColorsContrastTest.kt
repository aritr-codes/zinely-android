package com.aritr.zinely.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * CI-32 — the contrast harness DESIGN-RULES R8 requires ("Contrast meets AA, including over texture";
 * [ZINELY-DESIGN-SYSTEM §7.3](../../../../../../../../docs/ZINELY-DESIGN-SYSTEM.md)). One WCAG 2.x
 * contrast-ratio assertion per foreground/background token pair in [ZinelyColors], **light and dark**.
 *
 * The palette is riso-warm and low-contrast by design, so this file has two jobs, not one:
 *
 *  - **body-text pairs must clear the AA normal-text floor (4.5:1)** in *both* rooms and *both* themes;
 *  - the **decorative and authorial pairs that sit below that floor on purpose** are recorded here with
 *    their measured ratio, so the floor they *do* live above is asserted and any future token change
 *    that moves the ratio fails the build and forces a fresh design decision. A token value is a design
 *    decision (C3/ADR territory); this test never proposes changing one — it only pins the consequence.
 *
 * ### Why these pairs
 *
 * [ZinelyColors] splits two "rooms": ink* sits **on a sheet of paper** (`paper`/`paper2`/`field`/`menu`
 * — light surfaces in both themes), onDesk* sits **on the desk behind it** (`desk` — which flips dark).
 * ink* is deliberately shared across themes; onDesk* and the shell surfaces diverge. The pairs below are
 * the foreground text tokens on the surfaces their own KDoc assigns them to:
 *
 * | Foreground     | Background(s)        | Role                                   | Floor        |
 * |----------------|----------------------|----------------------------------------|--------------|
 * | ink            | paper, paper2        | primary body text on a sheet           | AA 4.5:1     |
 * | inkSoft        | paper                | secondary body text on a sheet         | AA 4.5:1     |
 * | onDesk         | desk                 | primary body text on the desk          | AA 4.5:1     |
 * | onDeskSoft     | desk                 | secondary body text on the desk        | AA 4.5:1     |
 * | coralText      | field, menu          | coral as text on the input/menu fills  | AA 4.5:1     |
 * | white          | coralStrong          | the primary-button label               | AA 4.5:1     |
 * | inkFaint       | paper                | tertiary/decorative — *never text*     | recorded     |
 * | onDeskFaint    | desk                 | tertiary/decorative — *never text*     | recorded     |
 * | teal           | paper                | an authorial ink — *not chrome*        | recorded     |
 *
 * Deliberately **not** asserted as chrome pairs, with reasons (see the class report / CI-32 notes):
 *  - `coralText`/`paper`: `coralText`'s KDoc lists paper as a light-shell surface (light = 5.57:1, a
 *    pass), but the dark block re-points `coralText` at the *dark menu* (`0xFFE76F51`), and coral text
 *    is never drawn on the paper *sheet* in dark — on paper in dark the coral is the user's authorial
 *    ink (same category as `teal`), not chrome. Asserting a pairing the spec repurposes away would test
 *    a surface that never renders.
 *  - `yellow`/`stamp` on paper: authorial inks (the user's drawing colours, §7.4 artifact colour), never
 *    chrome text and assigned no chrome surface; out of scope exactly as `teal`-as-text would be but for
 *    the single documented ratio `teal` already carries.
 */
class ZinelyColorsContrastTest {

    // ----- WCAG 2.x relative luminance & contrast ratio (pure; no dependency) ----------------
    //
    // Color.red/green/blue are the sRGB (gamma-encoded) components in [0,1] — identical to the 8-bit
    // channel / 255. WCAG linearises each, weights them, and forms (Llighter+.05)/(Ldarker+.05). Every
    // token in the pairs below is fully opaque, so no alpha compositing is needed.

    private fun linearize(channel: Float): Float =
        if (channel <= 0.03928f) channel / 12.92f
        else ((channel + 0.055f) / 1.055f).pow(2.4f)

    private fun relativeLuminance(c: Color): Float =
        0.2126f * linearize(c.red) + 0.7152f * linearize(c.green) + 0.0722f * linearize(c.blue)

    private fun contrastRatio(fg: Color, bg: Color): Float {
        val a = relativeLuminance(fg) + 0.05f
        val b = relativeLuminance(bg) + 0.05f
        return if (a >= b) a / b else b / a
    }

    private companion object {
        const val AA_NORMAL = 4.5f // WCAG 1.4.3 — text below 18pt / 14pt-bold
        const val AA_LARGE = 3.0f  // WCAG 1.4.3 (large text) / 1.4.11 (non-text) — the decorative floor
    }

    /** Assert a pair clears [floor] in both themes; the message always states both measured ratios. */
    private fun assertMeets(
        name: String,
        floor: Float,
        fg: (ZinelyColors) -> Color,
        bg: (ZinelyColors) -> Color,
    ) {
        val light = zinelyLightColors().let { contrastRatio(fg(it), bg(it)) }
        val dark = zinelyDarkColors().let { contrastRatio(fg(it), bg(it)) }
        assertTrue(
            "$name — light ${"%.3f".format(light)}:1 below AA floor ${"%.1f".format(floor)}:1",
            light >= floor,
        )
        assertTrue(
            "$name — dark ${"%.3f".format(dark)}:1 below AA floor ${"%.1f".format(floor)}:1",
            dark >= floor,
        )
    }

    /**
     * Assert a pair the palette places **below** the body-text floor on purpose: it must land inside
     * `[lowerFloor, AA_NORMAL)` in both themes — proving it still clears whatever floor it is allowed to
     * (the large/graphical floor, or simply "measurable"), and confirming it has NOT silently drifted up
     * into body-text territory where callers might mistake it for readable copy. The measured ratios are
     * in the failure message so a drift review reads them without re-deriving.
     */
    private fun assertRecordedAccepted(
        name: String,
        lowerFloor: Float,
        fg: (ZinelyColors) -> Color,
        bg: (ZinelyColors) -> Color,
    ) {
        val light = zinelyLightColors().let { contrastRatio(fg(it), bg(it)) }
        val dark = zinelyDarkColors().let { contrastRatio(fg(it), bg(it)) }
        for ((theme, ratio) in listOf("light" to light, "dark" to dark)) {
            assertTrue(
                "$name — $theme ${"%.3f".format(ratio)}:1 fell below its recorded floor " +
                    "${"%.1f".format(lowerFloor)}:1 (token drifted; re-open the design decision)",
                ratio >= lowerFloor,
            )
            assertTrue(
                "$name — $theme ${"%.3f".format(ratio)}:1 rose to/above the AA normal-text floor " +
                    "$AA_NORMAL:1; it is documented as never-load-bearing/authorial. If it is now a " +
                    "body-text token, promote it deliberately and move it to the AA-normal set",
                ratio < AA_NORMAL,
            )
        }
    }

    // ----- body text: must clear AA normal (4.5:1), both rooms, both themes ------------------

    @Test
    fun `primary body text clears AA normal on every sheet surface`() {
        // ink/paper   light 14.161 · dark 13.068
        assertMeets("ink / paper", AA_NORMAL, { it.ink }, { it.paper })
        // ink/paper2  light 13.303 · dark 11.811  (stacked cards, inner faces)
        assertMeets("ink / paper2", AA_NORMAL, { it.ink }, { it.paper2 })
    }

    @Test
    fun `secondary body text on a sheet clears AA normal`() {
        // inkSoft/paper  light 5.164 · dark 4.766
        assertMeets("inkSoft / paper", AA_NORMAL, { it.inkSoft }, { it.paper })
    }

    @Test
    fun `body text on the desk clears AA normal in both themes`() {
        // onDesk/desk      light 12.154 · dark 13.611
        assertMeets("onDesk / desk", AA_NORMAL, { it.onDesk }, { it.desk })
        // onDeskSoft/desk  light  5.346 · dark  8.639
        assertMeets("onDeskSoft / desk", AA_NORMAL, { it.onDeskSoft }, { it.desk })
    }

    @Test
    fun `coral text clears AA normal on the input and menu fills in both themes`() {
        // The chrome home of coralText: the field/menu fills (identical value). field==menu, so testing
        // both documents intent even though the ratio is shared.
        // coralText/field  light 6.016 · dark 4.637     coralText/menu  light 6.016 · dark 4.637
        assertMeets("coralText / field", AA_NORMAL, { it.coralText }, { it.field })
        assertMeets("coralText / menu", AA_NORMAL, { it.coralText }, { it.menu })
    }

    @Test
    fun `the primary button label clears AA normal on the coral fill`() {
        // white/coralStrong  4.625 in both themes (coralStrong is theme-invariant). KDoc claims "AA 4.6:1".
        assertMeets("white / coralStrong", AA_NORMAL, { Color.White }, { it.coralStrong })
    }

    // ----- recorded-accepted: sub-AA by design (never load-bearing / authorial) --------------

    @Test
    fun `inkFaint on paper is recorded accepted below the decorative floor`() {
        // inkFaint/paper  light 2.647 · dark 2.443. KDoc: "tertiary/decorative on paper. Never load-
        // bearing text." Sits below even the 3.0 large/graphical floor — accepted ONLY because it is
        // never text nor a perceivable-boundary UI element; it is grain/decoration. Floor asserted: it
        // must stay a measurable, non-body ratio (>2.0 and <4.5).
        assertRecordedAccepted("inkFaint / paper", lowerFloor = 2.0f, { it.inkFaint }, { it.paper })
    }

    @Test
    fun `onDeskFaint on the desk is recorded accepted at the large-text floor, below body`() {
        // onDeskFaint/desk  light 4.003 · dark 4.495. KDoc: "tertiary/decorative on the desk. Never
        // load-bearing text." It clears the AA large/graphical floor (3.0:1) in both themes but is BELOW
        // the AA normal-text floor (4.5:1) by design — this is the token CI-32 flags as the one used for
        // exactly the small text a contrast bug hides in, so the band is pinned tightly.
        assertRecordedAccepted("onDeskFaint / desk", lowerFloor = AA_LARGE, { it.onDeskFaint }, { it.desk })
    }

    @Test
    fun `teal as text on paper is recorded accepted as a documented artist choice`() {
        // teal/paper  light 2.902 · dark 2.678. ZinelyColors' own KDoc: "Sub-AA as text (2.9:1); a
        // documented artist choice, not a default." Authorial ink, not chrome — recorded, never promoted.
        assertRecordedAccepted("teal / paper", lowerFloor = 2.0f, { it.teal }, { it.paper })
    }
}
