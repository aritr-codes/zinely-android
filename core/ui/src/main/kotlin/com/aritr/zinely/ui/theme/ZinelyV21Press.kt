package com.aritr.zinely.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A V2.1 **press tier** — how deep an object sits, and what happens to it under a finger.
 *
 * The hard shadow is not elevation, so pressing is not a state change to animate: the object
 * *physically moves*, down-right, and its shadow shortens because it is now closer to the surface it
 * is printed on ([V21-SPEC §4.3](docs/design/V21-SPEC.md)). Both halves have to move together or the
 * illusion breaks — an object that translates without its shadow shortening reads as sliding, not
 * pressing.
 *
 * ### There is no single press rule, and the spec implied there was one
 *
 * [V21-SPEC §4.3](docs/design/V21-SPEC.md) states the hero case — *"a pressed control translates
 * `2px, 2px` and drops its shadow to `1px`"* — and a first draft of [ZinelyV21Dimens] published
 * exactly that as `hardShadowPressed` and `pressTravel`, as though it governed everything. Counting
 * the corpus says otherwise: **18 `:active` rule blocks / 19 selectors**, of which **17 carry depth**.
 * The one excluded is `.act:active { background: var(--leaf-tint) }` — `.act` has `border: none` and
 * no shadow at rest, so it has no depth to tier. Three rest depths, **four** behaviours:
 *
 * | Tier | Rest | Travel | Pressed | Corpus |
 * |---|---|---|---|---|
 * | [Hero] | `--hard` 4 | 2 | **1** | `.cover` (every Library cover), `.start`, `.btn-save`, `.btn-share`, `.add` |
 * | [Raised] | 3 | 2 | 1 | `.retry`, `.foldit`, `.shelf`, `.opt`, `.paperseg button` |
 * | [Flat] | 2 | 2 | **0** | `.chip`, `.tile`, `.iconbtn`, `.icon-btn`, `.fnav` — presses flush to the surface |
 * | [Inline] | 2 | 1 | 1 | `.ctl` only, in all three prototypes — a control *inside* another surface |
 *
 * ### What the corpus shows, and what it does not
 *
 * Three tiers follow *pressed = rest − travel*; [Hero] travels 2 and sheds 3. An earlier version of
 * this file called that a deliberate expressive choice — *"the one primary action should feel like it
 * goes further down"* — and **a review was right that the corpus does not support the claim.** Every
 * Hero *and* Raised `:active` rule writes the byte-identical `translate(2px,2px)` +
 * `box-shadow: 1px 1px 0`, across nine selectors and three files. What that shows is **one pressed
 * value of 1dp, reused regardless of rest depth**; under that reading Raised's agreement with the
 * subtraction is coincidence and Hero's "exception" is an artifact of it. The numbers are transcribed
 * because the HTML is canonical; the *reason* is not known and is no longer asserted.
 *
 * That also killed a second claim. Hero was described as *"the one primary action on a screen"*, and
 * it is not: `.cover` wears Hero depth on **every tile in the Library grid**, and the Proof puts
 * `.btn-save` and `.btn-share` at Hero on one screen. One-per-screen is a property of the **`--frame`
 * ring** — used exactly twice per file, a base rule and its `:active` — not of this depth.
 *
 * ⚠️ **Do not add a tier by interpolation.** Each of these four is a count of frozen rules; a fifth
 * would have no corpus behind it, which is the fabrication D-006 and D-007 both refused.
 */
@Immutable
public data class ZinelyV21Press(
    /** The resting hard-shadow offset, on both axes. */
    val rest: Dp,
    /** How far the object itself travels down-right under a finger. */
    val travel: Dp,
    /** The hard-shadow offset while pressed. Zero means the object is flush to the surface. */
    val pressed: Dp,
) {
    /** The offset to draw for a given press state — the only thing a caller normally needs. */
    public fun offset(isPressed: Boolean): Dp = if (isPressed) pressed else rest

    public companion object {
        /** `--hard`. The deepest tier — Library covers and the screen's headline buttons — and the only one that sheds more than it travels. */
        public val Hero: ZinelyV21Press = ZinelyV21Press(rest = 4.dp, travel = 2.dp, pressed = 1.dp)

        /** Secondary actions and shelves — still lifted off the surface when pressed. */
        public val Raised: ZinelyV21Press = ZinelyV21Press(rest = 3.dp, travel = 2.dp, pressed = 1.dp)

        /** Chips, tiles and icon buttons. Presses **flush**: no shadow at all while held. */
        public val Flat: ZinelyV21Press = ZinelyV21Press(rest = 2.dp, travel = 2.dp, pressed = 0.dp)

        /** `.ctl` — a control sitting inside another surface. Halves the travel so it cannot outshout its host. */
        public val Inline: ZinelyV21Press = ZinelyV21Press(rest = 2.dp, travel = 1.dp, pressed = 1.dp)
    }
}
