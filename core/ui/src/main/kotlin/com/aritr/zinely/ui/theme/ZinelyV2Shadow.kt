package com.aritr.zinely.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One CSS `box-shadow` layer as the frozen V2 trilogy actually writes them: `0 <dy> <blur> <spread>
 * <colour>`.
 *
 * ### Why this is a primitive and not a ladder
 *
 * V1 has a three-tier depth ladder ([ZinelyElevation] — `shadow1` / `shadow2` / `shadowLift`) because
 * the V1 trilogy declared one as tokens. **V2 declares no shadow tokens at all.** Every `box-shadow`
 * in the three frozen files is written out in full at its use site.
 *
 * Counted across the three `<style>` blocks: **27** `box-shadow` declarations (plus one `none`), of
 * which **26** are distinct. The single duplicated pair is the `.phone` prototype bezel, which is
 * scaffolding — so among the **25 chrome** declarations **no two are alike at all**, and **20** of
 * them carry spread. There is no tier to belong to, so modelling tiers would be inventing the ladder
 * rather than porting it — the same call [ZinelyV2Typography] made about the type scale, for the same
 * reason.
 *
 * What *is* shared is the shape of a layer, and V2's shape differs from V1's in two ways that matter:
 *
 * - **Spread is required.** V1's `ZinelyShadowLayer` states the frozen spec "never uses spread, so
 *   neither is modelled". V2 uses it in **20 of its 25** chrome shadows, and **never once positively**
 *   — the `0 Ypx Bpx -Spx` idiom that pulls a soft shadow back under its object instead of letting it
 *   halo. It is the dominant idiom of the whole system, not an exception.
 * - **`dy` may be negative.** Three chrome surfaces cast *upward* — the Bench's bottom sheet, and the
 *   Proof's ready band and drawer. They sit at the bottom of the screen and throw their shadow onto
 *   the content above, which is the correct physical reading and is easy to lose in transcription.
 *
 * ### Four idioms in the frozen CSS that look like shadows and are not
 *
 * Named here because each will otherwise be transcribed as elevation, and each would be wrong:
 *
 * 1. **`inset 0 0 0 1px <colour>`** (Library `.cover`, `.zine:active .cover`, `.sheet-ill`) is a 1px
 *    *inner* hairline. In Compose that is a border drawn inside the bounds, not a shadow — so `inset`
 *    is deliberately not modelled here.
 * 2. **`0 0 0 <n>px <colour>`** with no offset or blur (Library `.sheet` and Bench `.sw2` at 1px, Bench
 *    `.handle` at 1.5px) is an *outer* ring. CSS reaches for a shadow because a real border would
 *    change layout; Compose has no such constraint, so this is an outline/border too.
 * 3. **`0 1px 0 <colour>`** with zero blur (Bench `.kb .key`) is a keycap bevel — a hard edge, not depth.
 * 4. **`0 3px 0 / 0 6px 0 / 0 9px 0`** (Bench `.pthumb i`) is a *drawing trick*: three zero-blur
 *    shadows fake four lines of text from one 1px div in a page thumbnail. It must be drawn as rects.
 *
 * There is no `x` offset field because **no shadow in the trilogy is offset horizontally**, and no
 * `inset` field because the only chrome uses of `inset` are the three 1px hairlines above. (`inset`
 * does appear twice more, as the `0 4px 0 rgba(255,255,255,.15) inset` highlight on the `.phone`
 * bezel — but that is prototype scaffolding, not product UI, and it is the one duplicated shadow in
 * the corpus.) Both absences are stated so a later reader takes them as findings rather than gaps.
 *
 * ### Not a Modifier
 *
 * These are **data**. Compose has no multi-layer coloured shadow with spread, so the modifier that
 * draws them lands with the first component that needs it (Phase B). Shipping draw code here with no
 * caller would be the speculative abstraction V1 already declined to ship.
 */
@Immutable
public data class ZinelyV2ShadowLayer(
    /** Vertical offset. **Negative casts upward** — the bottom sheets and the ready band do. */
    val dy: Dp,
    /** Blur radius. */
    val blur: Dp,
    /**
     * Spread. Almost always **negative** in this system, shrinking the shadow so a large blur reads as
     * a soft contact rather than a halo. Defaulted to zero for the handful of layers that omit it.
     */
    val spread: Dp = 0.dp,
    /**
     * The shadow's colour, alpha included. The Library composes depth from two colour tokens
     * (`shadow` for the cast, `contact` for the tight layer beneath); the Bench and Proof use the
     * single `frameShadow`. Both are chrome colours and live in [ZinelyV2Colors].
     */
    val color: Color,
)
