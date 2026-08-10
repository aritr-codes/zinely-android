package com.aritr.zinely.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
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
 * The `--frame: 5px` **stacked ring** — a flat colour band sitting outside the object, borrowed from
 * the reference sites as riso misregistration ([V21-SPEC §4.3](docs/design/V21-SPEC.md)).
 *
 * **Reserved for the one primary action on a screen** (Save PDF, Add). Its whole job is to be the only
 * thing wearing it; a second ring on the same screen makes both of them decoration.
 *
 * CSS writes this as `0 0 0 5px` — a third box-shadow idiom that is not depth at all, and
 * [ZinelyV2ShadowLayer] names the trap directly ("CSS reaches for a shadow because a real border would
 * change layout; Compose has no such constraint"). So this draws a ring, and it draws it **outside the
 * bounds**: the ring must not eat the object's own padding, and the object must not have to be laid
 * out 10dp larger to wear one.
 *
 * Ordered against [zinelyV21HardShadow] the way the corpus stacks them — `hard, then frame` in the CSS
 * list, meaning the ring paints *under* the hard shadow. Apply the frame first in the chain.
 */
public fun Modifier.zinelyV21Frame(width: Dp, color: Color, shape: Shape): Modifier =
    drawBehind {
        val w = width.toPx()
        if (w <= 0f) return@drawBehind
        translate(-w, -w) {
            val grown = Size(size.width + 2 * w, size.height + 2 * w)
            drawOutline(shape.createOutline(grown, layoutDirection, this), color)
        }
    }

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
 * @param color the shadow ink — `inkLine`. See [zinelyV21HardShadow].
 */
public fun Modifier.zinelyV21Pressable(
    isPressed: Boolean,
    press: ZinelyV21Press,
    color: Color,
    shape: Shape,
): Modifier = this
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val travel = if (isPressed) press.travel.roundToPx() else 0
        // Placed with an offset rather than measured smaller: the object does not shrink when pressed,
        // it moves. Its own size, and therefore its text metrics, must not change under a finger.
        layout(placeable.width, placeable.height) { placeable.place(travel, travel) }
    }
    .zinelyV21HardShadow(press.offset(isPressed), color, shape)
