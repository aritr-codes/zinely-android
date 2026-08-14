package com.aritr.zinely.ui.components

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritr.zinely.ui.theme.ZinelyDimens
import com.aritr.zinely.ui.theme.ZinelyTheme

/**
 * The frozen `:focus-visible` ring. V2.1 — ADR-102 P8.
 *
 * ```css
 * v21-bench.html    button:focus-visible,.el:focus-visible{outline:2px solid var(--ink);outline-offset:3px}
 * v21-proof.html    .btn:focus-visible,.iconbtn:focus-visible,.tapz:focus-visible{… outline-offset:4px}
 * v21-library.html  .start:focus-visible{outline:2px solid var(--ink);outline-offset:5px}
 * ```
 *
 * **The behaviour is frozen and unchanged; only the paint moves.** V2 drew `3px solid --coral-strong`
 * at `outline-offset:2px`; V2.1 has no coral at all, and all three `:focus-visible` rules in the three
 * prototypes write the same `2px solid var(--ink)`.
 *
 * ### The offset is 3px, and the corpus gives three answers
 *
 * The width is unanimous; the offset is **3 / 4 / 5**, one value per file, and a shared primitive has
 * to pick one. The Bench's is taken because it is the only rule written against an **element**
 * selector — `button:focus-visible` governs every button on that surface that has not said otherwise,
 * which is precisely what this modifier is. The other two are class rules naming four selectors
 * between them, and the widest of those, the Library's `.start`, is transcribed locally by
 * [ZineShelfFail]'s retry ring because that file *is* that selector.
 *
 * An earlier draft of this KDoc surveyed only the Proof and the Library and called the result complete.
 * A review found the Bench rule it had missed. The disagreement is recorded rather than averaged.
 *
 * A drawn line is `ink`, never `inkLine` — `inkLine` is the shadow ink, and collapsing the two cost
 * 61 near-invisible borders in dark once already ([zinelyV21HardShadow]).
 *
 * The CSS rule's global `border-radius:6px` is a lowest-common-denominator wart of a universal
 * selector; in Compose the ring follows each component's own [cornerRadius] (pass the component's
 * radius; the frozen 6px [ZinelyDimens.FocusRingRadius] is the fallback for unshaped targets).
 *
 * Driven by the component's own focus [interactionSource] — on Android, focus interactions surface
 * for keyboard/d-pad navigation, which is exactly the `:focus-visible` (not `:focus`) distinction.
 */
@Composable
public fun Modifier.zinelyFocusRing(
    interactionSource: InteractionSource,
    cornerRadius: Dp = ZinelyDimens.FocusRingRadius,
): Modifier {
    val focused by interactionSource.collectIsFocusedAsState()
    return zinelyFocusRing(focused, cornerRadius)
}

/**
 * The same ring, for a caller that already holds its own focus flag.
 *
 * Some controls read focus from `Modifier.onFocusChanged` into their own state rather than from an
 * [InteractionSource] — a text field wrapping a `BasicTextField`, a control whose focus drives more than
 * its ring. Before this overload existed, **five** call sites answered that by hand-rolling the draw:
 * `ZineShelfFail`, `ZineDock`, `ZineOnShelf`, `ShelfSheets`, and one copy that had crossed the module
 * boundary into `:app`'s nav host. Five copies of a two-number accessibility affordance is five places
 * for it to drift, and the ring is exactly the kind of thing nobody re-measures once it looks right.
 *
 * Same paint, one definition. The `InteractionSource` overload above now delegates here.
 *
 * ⚠ **This paragraph used to end "…so there is one ring in the app and not two that agree today", and
 * that sentence was false on the day it was written.** Only `ZineShelfFail` had actually converged;
 * `ShelfSheets` still held its own `drawV21FocusRing` with its own `2.dp`/`5.dp` pair, and the nav host's
 * copy was **added by the very change that wrote the sentence** — three implementations, in the same diff
 * whose comment warned that duplicated rings drift. A review found it by opening the two files instead of
 * reading this KDoc, which is the whole argument for treating a KDoc as a claim and not as evidence.
 * All three are converged now, and the count is asserted nowhere — so the honest form of the promise is
 * a command rather than a sentence: grep the main source sets for `drawRoundRect` and this file should be
 * the only ring that comes back.
 *
 * ### Why [offset] is a parameter and not a constant
 *
 * Converging those five copies onto a single 3dp would have been wrong, and reading them carefully is
 * what showed it: four of them write **5**, because they are Library controls and `v21-library.html`'s
 * `.start:focus-visible` writes `outline-offset:5px`. They were not drifting — they were transcribing a
 * different rule. The corpus really does give three offsets, one per prototype, so the duplication worth
 * removing is the *draw*, not the number. Callers pass their own surface's offset; the default is the
 * Bench's element-selector rule, which is what an unshaped shared control is.
 */
@Composable
public fun Modifier.zinelyFocusRing(
    focused: Boolean,
    cornerRadius: Dp = ZinelyDimens.FocusRingRadius,
    offset: Dp = FocusRingOffset,
): Modifier {
    if (!focused) return this
    val color = ZinelyTheme.v21Colors.ink
    return drawBehind {
        val width = FocusRingWidth.toPx()
        val offsetPx = offset.toPx()
        // Ring sits outside the bounds: outline-offset, stroke centred on the outline path.
        val inflate = offsetPx + width / 2f
        drawRoundRect(
            color = color,
            topLeft = Offset(-inflate, -inflate),
            size = Size(size.width + 2 * inflate, size.height + 2 * inflate),
            cornerRadius = CornerRadius(cornerRadius.toPx() + inflate),
            style = Stroke(width = width),
        )
    }
}

/**
 * `outline:2px solid var(--ink);outline-offset:3px` — `v21-bench.html button:focus-visible`.
 *
 * Local to this file rather than on [ZinelyDimens], which is the **V2** token set and is still read by
 * unconverted callers — moving its numbers would re-skin them from underneath.
 *
 * The width is unanimous across all three prototypes, so it stays private. The offset is not, so it is a
 * parameter; [ZinelyV21FocusOffsetLibrary] publishes the Library's.
 */
private val FocusRingWidth = 2.dp
private val FocusRingOffset = 3.dp

/**
 * `v21-library.html .start:focus-visible{outline-offset:5px}` — the Library surface's own offset, worn by
 * every focusable object on the shelf and by the boot states modelled on `.fail`.
 *
 * Published so the four call sites that need it name the rule rather than re-typing a `5.dp` each.
 */
public val ZinelyV21FocusOffsetLibrary: Dp = 5.dp
