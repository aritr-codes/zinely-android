package com.aritr.zinely.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import androidx.compose.ui.unit.IntOffset
import com.aritr.zinely.ui.theme.ZinelyV21Press

/**
 * The **V2.1 printed shadow** — a zero-blur offset copy of the object's own shape, in full ink.
 *
 * ### Why this is not [zinelyV2Shadow] with an x offset
 *
 * [ZinelyV2ShadowLayer] models *soft* depth: blur, and spread in 20 of V2's 25 chrome shadows, with
 * `dy` only — its KDoc records that **no shadow in the V2 trilogy is offset horizontally**. V2.1's
 * shadow is the opposite object in every field: always offset on **both** axes, **never** blurred,
 * **never** spread, and always the flat `inkLine`. Routing it through the V2 layer would mean passing
 * zero for the two fields that file exists to model, and adding an `x` to a data class whose
 * documentation states the corpus has none. They are two materials, so they are two primitives — the
 * same call [ZinelyV2ShadowLayer] made about V1's ladder.
 *
 * ### It is a shadow, so it does not move with the press
 *
 * Pressing translates the object and *shortens* its shadow ([ZinelyV21Press]); the shadow is drawn
 * relative to the translated object, exactly as CSS does it, so its far edge creeps back toward the
 * surface rather than sliding with the finger.
 *
 * The outline is **filled**, not stroked, so the object's own opaque background is what makes this read
 * as a shadow rather than a slab — the same precondition [zinelyV2Shadow] carries. A surface with a
 * transparent interior gets a solid shape behind it, which is correct for a printed shadow and wrong
 * for anything expecting an outline.
 *
 * One corpus hard shadow is **not** expressible here: the Proof's fold sheet uses
 * `filter: drop-shadow(var(--hard) var(--hard) 0 …)`, which follows the drawn artwork's *alpha* rather
 * than the node's outline. That one needs its own draw, not this modifier with a clever shape.
 *
 * @param offset the down-right offset, on both axes. Zero draws nothing — the flush pressed state of
 *   [ZinelyV21Press.Flat] is a real state, not a missing shadow.
 * @param color **must be `inkLine`, never `ink`.** A shadow is the absence of light and stays darker
 *   than the surface; a drawn line is ink. Collapsing the two cost 61 near-invisible borders in dark
 *   ([ZinelyV21Colors]). Taken as a parameter rather than read from a theme so this file has no
 *   opinion about which theme is installed, matching every other draw modifier here.
 */
public fun Modifier.zinelyV21HardShadow(offset: Dp, color: Color, shape: Shape): Modifier =
    drawBehind {
        val d = offset.toPx()
        if (d <= 0f) return@drawBehind
        translate(d, d) {
            drawOutline(shape.createOutline(size, layoutDirection, this), color)
        }
    }

/**
 * A flat colour band drawn **outside** an object's bounds — the geometry behind every CSS
 * `box-shadow: 0 0 0 Npx COLOUR` in the corpus, at any width and in any colour.
 *
 * This is the mechanism, and it carries no policy. [zinelyV21Frame] is the `--frame: 5px`
 * misregistration ring built on it, and *that* is the one with a rule attached.
 *
 * ### Why the two are separate names
 *
 * They were one function, and the single name made a false promise. `zinelyV21Frame`'s KDoc reserved it
 * for "the one primary action on a screen" — while `BenchPageNav` routed the current page's **3dp berry
 * state ring** through it, giving the Bench two rings from a primitive documented as allowing one. The
 * pixels were right both times (`.fpage.on{box-shadow:0 0 0 3px var(--berry)}` is a real frozen rule);
 * what was wrong was that a contract lived on a function that could not enforce it, so the only thing
 * standing between the corpus and a screen full of rings was whether a reader noticed the paragraph.
 * A review noticed instead. Splitting the name puts the reservation where it can be counted: **grep for
 * `zinelyV21Frame` and every hit should be a screen's single primary action.**
 *
 * CSS writes this as `0 0 0 5px` — a third box-shadow idiom that is not depth at all, and
 * [ZinelyV2ShadowLayer] names the trap directly ("CSS reaches for a shadow because a real border would
 * change layout; Compose has no such constraint"). So this draws a ring, and it draws it **outside the
 * bounds**: the ring must not eat the object's own padding, and the object must not have to be laid
 * out 10dp larger to wear one.
 *
 * ### It is a stroked band, and the corner radii grow additively
 *
 * A first version grew the *size* and reused the *shape* — `shape.createOutline(size + 2w)` — then
 * filled it. A review caught two defects in one line, and the fix for both was already sitting in this
 * package. **Growing the size scales the radii proportionally, where CSS grows them additively** (CSS
 * Backgrounds §7.1.1): at `RoundedCornerShape(14.dp)` with a 5dp band the outer boundary kept radius 14
 * where CSS gives 19, pinching the band to less than its own width at every corner. It was invisible
 * only because all three corpus users are `--br-pill`, where proportional growth of `min/2` coincides
 * with `+w`. [spreadPath] does the additive thing, is unit-tested, and rejects a `Generic` outline
 * rather than silently mis-drawing one — so the ring is a **`w`-wide stroke of the outline spread by
 * `w/2`**, whose outer edge lands exactly `w` out with radius `r + w`.
 *
 * Ordered against [zinelyV21HardShadow] the way the corpus stacks them — `hard, then frame` in the CSS
 * list, and CSS paints the *first*-declared shadow layer on top. Of two chained `drawBehind`s the left
 * one paints first, i.e. underneath. **So apply the frame first in the chain** and the ring lands under
 * the hard shadow, as it does in the prototypes.
 */
public fun Modifier.zinelyV21OuterRing(width: Dp, color: Color, shape: Shape): Modifier =
    drawBehind {
        val w = width.toPx()
        if (w <= 0f) return@drawBehind
        drawPath(
            path = spreadPath(shape.createOutline(size, layoutDirection, this), w / 2f),
            color = color,
            style = Stroke(width = w),
        )
    }

/**
 * The `--frame: 5px` misregistration ring ([V21-SPEC §4.3](docs/design/V21-SPEC.md)) — [zinelyV21OuterRing]
 * with the width fixed, because the width is not the caller's to choose.
 *
 * **Reserved for the one primary action on a screen** (Save PDF, Add). Its whole job is to be the only
 * thing wearing it; a second ring on the same screen makes both of them decoration. A state ring, a
 * selection ring, or a ring at any other width is [zinelyV21OuterRing] — same paint, no claim.
 */
public fun Modifier.zinelyV21Frame(color: Color, shape: Shape): Modifier =
    zinelyV21OuterRing(ZinelyV21Dimens.frameRing, color, shape)

/**
 * The complete V2.1 press: the object travels down-right and its shadow shortens, together.
 *
 * This is the signature interaction of the whole language, and it is one modifier because the two
 * halves are one event — an object that translated without shedding shadow would read as sliding
 * across the desk rather than being pushed into it.
 *
 * The shadow is chained *inside* the travel, so it moves with the object and its offset shortens
 * within that moved frame — which is what CSS does, since `transform` carries an element's `box-shadow`
 * along with it. For [ZinelyV21Press.Hero] that puts the pressed shadow's far edge at `2 + 1 = 3dp`
 * against a resting `4dp`: the object approaches the surface, it does not slide across it.
 *
 * `prefers-reduced-motion` needs no branch here: this is not an animation. There is no transition to
 * downgrade — the object is in one of two places, and [V2-CONSTITUTION §III](docs/design/V2-CONSTITUTION.md)'s
 * requirement is that reduced motion never removes the state change motion was communicating. The
 * state change *is* the position.
 *
 * ### The chain contract — read this before writing a caller
 *
 * The resting shadow paints up to **4dp outside the node**, and nothing reserves space for it. Two
 * consequences bind every caller in Steps 4–6, and both fail as *"the shadow is missing"* on a device
 * rather than as anything a test would name:
 *
 * - **Nothing that clips may sit to the left of this modifier.** A `Modifier.clip`, a
 *   `graphicsLayer(clip = true)`, a `Surface`/`Card` shape, or a lazy-list viewport edge will cut the
 *   shadow off. Either clip further right, or give the row the padding the shadow needs.
 * - **Put `clickable` / `semantics` to the *right*** so the touch target and the platform
 *   `AccessibilityNodeInfo` bounds travel with the object. Placement offsets do propagate into
 *   `boundsInWindow`, and a ≤2dp transient shift is harmless — but ADR-058 and ADR-059 are both cases
 *   of Compose's semantics tree and the platform's disagreeing, so this is written down rather than
 *   rediscovered on a device.
 *
 * ### Two corpus press behaviours this deliberately cannot express
 *
 * `.act:active` presses by changing **background only**, with no travel and no shadow — it has
 * `border: none` and no depth to shed. And `.fnav:disabled` / `.icon-btn:disabled` drop to
 * `box-shadow: none`, a *disabled* depth rather than a pressed one. Neither is a fifth tier and neither
 * belongs here; a caller needing them states it at the call site.
 *
 * @param color the shadow ink — `inkLine`. See [zinelyV21HardShadow].
 */
public fun Modifier.zinelyV21Pressable(
    isPressed: Boolean,
    press: ZinelyV21Press,
    color: Color,
    shape: Shape,
): Modifier = this
    // Offset, not a smaller measure: the object does not shrink under a finger, it moves — its size,
    // and therefore its text metrics, must not change. The lambda form defers to the placement phase,
    // so a press does not re-measure the subtree. The shadow chains *inside* it and so moves with the
    // object, which is what CSS `transform` does to a `box-shadow`.
    .offset { if (isPressed) IntOffset(press.travel.roundToPx(), press.travel.roundToPx()) else IntOffset.Zero }
    .zinelyV21HardShadow(press.offset(isPressed), color, shape)
