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
 * every `:active` rule in the corpus (17 of them) says otherwise. Three rest depths and **four**
 * behaviours:
 *
 * | Tier | Rest | Travel | Pressed | Corpus |
 * |---|---|---|---|---|
 * | [Hero] | `--hard` 4 | 2 | **1** | `.start`, `.btn-save`, `.add`, `.btn-share` — the one primary per screen |
 * | [Raised] | 3 | 2 | 1 | `.retry`, `.foldit`, `.shelf`, `.opt`, `.paperseg button` |
 * | [Flat] | 2 | 2 | **0** | `.chip`, `.tile`, `.iconbtn`, `.icon-btn`, `.fnav` — presses flush to the surface |
 * | [Inline] | 2 | 1 | 1 | `.ctl` only, in all three prototypes — a control *inside* another surface |
 *
 * Three of the four follow *pressed = rest − travel*, which is what physically ought to happen.
 * **[Hero] does not**: it travels 2 and sheds 3. That is deliberate rather than a transcription slip —
 * the one primary action on a screen is also the one that should feel like it goes *further* down —
 * and it is exactly why this is a table rather than a formula. A formula would have quietly corrected
 * the design's most expressive control into its least.
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
        /** `--hard`. **The one primary action on a screen**, and the only tier that sheds more than it travels. */
        public val Hero: ZinelyV21Press = ZinelyV21Press(rest = 4.dp, travel = 2.dp, pressed = 1.dp)

        /** Secondary actions and shelves — still lifted off the surface when pressed. */
        public val Raised: ZinelyV21Press = ZinelyV21Press(rest = 3.dp, travel = 2.dp, pressed = 1.dp)

        /** Chips, tiles and icon buttons. Presses **flush**: no shadow at all while held. */
        public val Flat: ZinelyV21Press = ZinelyV21Press(rest = 2.dp, travel = 2.dp, pressed = 0.dp)

        /** `.ctl` — a control sitting inside another surface. Halves the travel so it cannot outshout its host. */
        public val Inline: ZinelyV21Press = ZinelyV21Press(rest = 2.dp, travel = 1.dp, pressed = 1.dp)
    }
}
