package com.aritr.zinely.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The dimensions the frozen **V2** trilogy states globally — which is very nearly nothing, and that
 * is the finding rather than an omission.
 *
 * ### There is no radius scale, and the one radius token is dead
 *
 * `--r:18px` is declared at `v2-bench.html:24` and `v2-proof.html:24` and is **referenced zero
 * times**. No border-radius of 18px exists anywhere in V2; every one of the sixteen distinct chrome
 * radii is a literal (50%, 22, 20, 16, 14, 13, 12, 11, 10, 9, 8, 6, 5, 4, 3, 2px, plus five
 * asymmetric ones such as the cover's `6px 9px 9px 6px`, whose tighter left corners are the spine of
 * a printed thing). Nor do the files agree: the Library's sheet is 20px, the Bench's is 22px, and the
 * Bench and Proof both put cards at 13px while the Library's cover is asymmetric. So `--r` is not
 * ported — a token that names a value nothing uses would invite Phase B to "restore" an 18px radius
 * the design never had. Logged as **D-006**.
 *
 * ### There is no spacing scale here either, and that one is not settled
 *
 * [V2-CONSTITUTION.md](docs/design/V2-CONSTITUTION.md) §III states *"An **8pt rhythm** governs
 * layout"* as an invariant, and [V2-RESEARCH.md](docs/design/V2-RESEARCH.md) §2.4 defines the scale as
 * 4/8/16/24/32/48. The frozen CSS does not keep it: **16.7%** of chrome spacing values are multiples
 * of 8 and **38.2%** are multiples of 4, with 2, 6, 7, 9, 10, 13 and 14px all common. Unlike the
 * radius question this is a conflict with the **highest** authority in the corpus, so it is not
 * implementation's to settle either way — see **D-007**, which is the one item of A4 held for an
 * owner ruling. Nothing is published here in the meantime, because both possible answers (transcribe
 * the frozen literals / snap to 8pt) produce a *different* spacing scale, and shipping either one now
 * would be choosing.
 *
 * ### What is left is genuinely global
 *
 * Two values, both of which every frozen surface agrees on.
 */
public object ZinelyV2Dimens {
    /**
     * The hairline — `1px`, used **40** times across the three files against **8** borders of any
     * other weight, and the default for every rule, divider, control outline and card edge. The heavier
     * (1.5px on the Bench's selection rectangle and selected swatch, 2px on its resize handle and
     * current page cell) are per-component and stay at their call sites.
     *
     * Drawn in `chromeLine` on Bench and Proof controls, `paperEdge` on paper-like surfaces, and
     * `hair` in the Library — see [ZinelyV2Colors].
     */
    public val Hairline: Dp = 1.dp

    /**
     * The focus indicator's stroke — `2px` in all four `:focus-visible` rules that exist.
     *
     * **Only the Library specifies focus at all.** The Bench and Proof contain no `:focus`,
     * `:focus-within` or `:focus-visible` rule between them, and the Bench additionally sets
     * `outline:none` on `.el` and `.search input`, both of which are keyboard-operable. So this
     * number is the *whole* of the frozen focus specification, and it covers one of three surfaces —
     * logged as **D-008**.
     *
     * There is deliberately no `FocusRingOffset` companion to this: the Library's three *product*
     * rules use three different offsets (6px on a zine card, 3px on the start button, 0 on the
     * overflow button) and its fourth rule — on the prototype-only theme control — uses a fourth
     * (2px). Four rules, four offsets: the stroke is shared, the offset is per-component.
     */
    public val FocusRingWidth: Dp = 2.dp

    /**
     * The minimum interactive target — **48dp**, the Android accessibility floor.
     *
     * This is the one value here that does **not** come from the frozen CSS, and the distinction is
     * the point: 48dp is not a design value, it is a **platform floor**. V1's [ZinelyDimens] carries
     * the same number because the V1 spec set `min-height:48px` and the two agreed; the V2 spec sets
     * no minimum on any control, and that silence is not a contradiction of the floor — it is the
     * absence of a statement about it.
     *
     * The frozen controls do not meet it. Most measure 26–46px, down to about 23×19 for the Bench's
     * tray fold, and **no selector in the trilogy declares a minimum touch target at all** — logged as
     * **D-009**. That defect is real and owner-owed, but its question is *how* the floor is reached:
     * grow the control, or extend the touch area past the drawn bounds. **Both answers presuppose
     * this number**, so stating it prejudges nothing and leaving it out would only mean Phase B builds
     * 26×26 controls with nothing in the foundation naming what they must clear.
     */
    public val MinTouchTarget: Dp = 48.dp
}
