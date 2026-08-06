package com.aritr.zinely.feature.editor

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.theme.ZinelyTheme

/** Test tag on the frozen editing-state style row (`.styletb`). */
public const val BenchStyleRowTestTag: String = "bench-style-row"

/** Frozen `.kbstack{transition:transform .34s var(--settle)}` (`v2-bench.html:259`). */
internal const val BenchKbStackMillis: Int = 340

/**
 * Frozen `.kbstack{transform:translateY(110%)}` (`v2-bench.html:259`) — the stack rests at **110 %** of
 * its own height below the fold, not 100 %.
 */
internal const val BenchKbStackRestFraction: Float = 1.10f

/** Frozen `.styletb{padding:8px 12px}` (`v2-bench.html:261`). */
internal val BenchStyleRowPaddingH = 12.dp
internal val BenchStyleRowPaddingV = 8.dp

/** Frozen `.styletb{gap:6px}` — also the `.chip`'s own icon/label gap (`v2-bench.html:261-262`). */
internal val BenchStyleRowGap = 6.dp

/** Frozen `.styletb .chip` / `.done` height (`v2-bench.html:262`, `:264`). */
internal val BenchStyleChipHeight = 34.dp

/** Frozen `.styletb .chip` / `.done` radius (`v2-bench.html:262`, `:264`). */
internal val BenchStyleChipRadius = 9.dp

/** Frozen `.styletb .chip{padding:0 12px}` (`v2-bench.html:262`). */
internal val BenchStyleChipPaddingH = 12.dp

/** Frozen `.styletb .done{padding:0 16px}` (`v2-bench.html:264`). */
internal val BenchStyleDonePaddingH = 16.dp

/** Frozen `.styletb .chip .sw{width:14px;height:14px;border-radius:50%}` (`v2-bench.html:263`). */
internal val BenchStyleSwatchSize = 14.dp

/** Frozen `.styletb .chip{font-size:12.5px;font-weight:500}` (`v2-bench.html:262`). */
internal val BenchStyleChipTextSize = 12.5.sp

/** Frozen `.styletb .done{font-size:13px;font-weight:600}` (`v2-bench.html:264`). */
internal val BenchStyleDoneTextSize = 13.sp

/**
 * The frozen editing-state style row — `.styletb` inside `.kbstack` (`v2-bench.html:259-264`, markup
 * `:408-410`); [ADR-093](../../../../../../../../docs/DECISIONS.md#adr-093) rows 3.4–3.7 and 3.9.
 *
 * ### Why this is a new row and not a re-skinned [TypeBar]
 *
 * [ADR-089](../../../../../../../../docs/DECISIONS.md#adr-089) row 3.5 named `TypeBar.kt` as the thing to
 * re-skin into this row. Taken literally that deletes a ten-value size stepper, three alignments, bold,
 * italic and five inks in a **parity** phase.
 * [OD-11](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-034-ruling) forbids exactly that in one
 * unconditional sentence — *"no existing editor capability is removed"* — and
 * [OD-14](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-039-ruling) restates it. So the plan row
 * is what gets corrected, not the product: this ships as the **editing state's** toolbar, a state that has
 * no style control today, and [TypeBar] is untouched and keeps the selected state.
 *
 * The two cannot collide, and that is structural rather than conventional: `EditorScreen`'s `styleTarget`
 * is gated on `interaction !is Interaction.EditingText`, so the Type bar is unreachable for the whole of a
 * session. Recorded at [D-042](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-042).
 *
 * ### Why three of its four controls are inert
 *
 * That is the freeze's own arithmetic, not a shortcut. Enumerating every listener in the frozen script:
 * the `Fraunces` and `A 23` chips have no handler, and **`#editColour` has none either** — `openInk` is
 * bound in `toolsFor` (`:495`, `:497`) and dispatched by `buildCtx` (`:499`, `:504`), to the **`.ctx`
 * bar's** `Ink` verb, never to this chip.
 * All three therefore ship drawn-and-disabled under
 * [OD-9](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-031-ruling), exactly as `Font` and
 * `Replace` already do on the context bar — which is also why C6's `.inkpop` is never reached from here.
 *
 * Note that `Size` being inert here does **not** contradict OD-9 routing the context bar's `Size` to the
 * Type bar: that route exists because the Type bar is reachable from the *selected* state. Reaching it
 * from the *editing* state would mean lifting the `styleTarget` gate above, which is new capability and
 * therefore C4's, not C3's.
 *
 * ### Deviation: the chips are labelled by verb, not by value
 *
 * The freeze draws its chips as *values* — a family name, `A 23`, a swatch. A value display that cannot
 * change the value is still a display, so it must be either true or absent: hard-coding `Fraunces`/`A 23`
 * would misreport every element that is neither. The chips therefore carry the same verb labels the
 * context bar already uses for these controls ([Copy.BenchVerbs]), which invents nothing and reports
 * nothing false. **The swatch is the exception and stays a value** — row 3.9 requires it to seed from the
 * element's own computed colour (`v2-bench.html:553`), so a coral heading shows coral.
 *
 * @param visible whether a text session is open — the frozen `.editing` class.
 * @param inkSwatch the edited element's own colour, for the `Ink` chip's dot (row 3.9).
 * @param onDone the frozen `#doneEdit` handler — the one live control in the row.
 * @param onDockedTopChanged the row's settled top edge in window coordinates, for the amended pan's
 *   clearance term ([benchEditPanMagnitudeDp], D-043 / OD-16). Reported on every layout, including at rest.
 */
@Composable
internal fun BenchStyleRow(
    visible: Boolean,
    inkSwatch: Color,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    onDockedTopChanged: (Float) -> Unit = {},
) {
    val colors = ZinelyTheme.v2Colors
    // A fraction of the row's OWN height, not a dp constant: `translateY(110%)` moves with the font scale,
    // and a fixed offset would leave a sliver of the row on screen at large text sizes.
    val offsetFraction by animateFloatAsState(
        targetValue = if (visible) 0f else BenchKbStackRestFraction,
        animationSpec = ZinelyTheme.v2Motion.settle(BenchKbStackMillis),
        label = "bench-kbstack",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            // The stack is bottom-anchored and rides ABOVE the IME. The frozen `.kb` drawn beneath
            // `.styletb` (`:265`, markup `:412-417`) is the prototype's stand-in for the system keyboard —
            // `aria-hidden`, purely scenery — so the real IME takes its place and the host is the ime
            // inset, never a ported key grid and never a fixed offset.
            .imePadding()
            // D-043 / OD-16: the amended pan needs to know where this row DOCKS, so it can lift the page
            // by only what the edited element needs to clear it. Reported here — ABOVE the graphicsLayer
            // below, deliberately: `positionInWindow()` applies layer transforms, so reading it inside or
            // after the layer would return the row's animated paint position and the pan would chase the
            // row's own entrance. Read here it is the settled layout position, which is what "docked"
            // means and the only value that makes the clamp a function of rest geometry.
            .onGloballyPositioned { onDockedTopChanged(it.positionInWindow().y) }
            .graphicsLayer {
                // `size` here is the layer's OWN measured size, so `translateY(110%)` is resolved against
                // the real height on the very first frame. An earlier cut measured it into state via
                // `onSizeChanged`, which is one frame late: the row painted fully docked once, then jumped
                // down and slid up. That flash is invisible to Robolectric and to a settled golden, and
                // plainly visible on a device.
                translationY = offsetFraction * size.height
            },
    ) {
        // Composed only while it is on screen or on its way off it. A `graphicsLayer` translation moves
        // pixels, not nodes: left permanently composed, the row's four controls stayed in the platform
        // accessibility tree at rest, and TalkBack read "Font, Size, Ink, Done" on an editor with no
        // session open — caught by `SurfaceTraversalOrderTest`, which reads the merged tree in the design's
        // order. The outer Box keeps the animation state so the entrance still runs from the rest value.
        val docked = visible || offsetFraction < BenchKbStackRestFraction
        if (docked) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // The tag goes ABOVE the padding, so the node's bounds are the whole row — ground,
                    // hairline and padding included — rather than its inner content box. It sat below the
                    // padding at first, and that is not a cosmetic difference: every geometric assertion
                    // written against this node was silently measuring a rect 8dp short at each end, which
                    // is how a raster probe aimed at the top hairline ended up reading the chips instead
                    // and reported a deleted hairline as present.
                    .testTag(BenchStyleRowTestTag)
                    .background(colors.sheet)
                    .benchStyleRowHairline(colors.chromeLine)
                    .padding(horizontal = BenchStyleRowPaddingH, vertical = BenchStyleRowPaddingV),
                horizontalArrangement = Arrangement.spacedBy(BenchStyleRowGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InertChip(Copy.BenchVerbs.FONT)
                InertChip(Copy.BenchVerbs.SIZE)
                InertChip(Copy.BenchVerbs.INK, swatch = inkSwatch)
                // Frozen `.styletb .grow{flex:1}` — the chips pack left, Done anchors right.
                Box(Modifier.weight(1f))
                DoneChip(onDone)
            }
        }
    }
}

/**
 * A chip the freeze draws and wires to nothing.
 *
 * Announced **disabled** and drawn dim, and the two must agree:
 * [ADR-092](../../../../../../../../docs/DECISIONS.md#adr-092) row 2.13c-i records why — a control that
 * says *disabled* to TalkBack while looking tappable is a defect this programme has already shipped once.
 * It carries no `clickable`, so the platform `AccessibilityNodeInfo` reports it non-clickable too, rather
 * than only the merged Compose tree saying so.
 */
@Composable
private fun InertChip(label: String, swatch: Color? = null) {
    val colors = ZinelyTheme.v2Colors
    Row(
        modifier = Modifier
            .height(BenchStyleChipHeight)
            .clip(RoundedCornerShape(BenchStyleChipRadius))
            .border(1.dp, colors.chromeLine, RoundedCornerShape(BenchStyleChipRadius))
            // Tag and semantics ABOVE the padding, for the same reason the row itself does it: below it,
            // the node is the inner content box, so the chip reported 28dp wide where it is drawn 52dp —
            // and TalkBack's focus rectangle would have been 24dp narrower than the control it outlines.
            .testTag("$BenchStyleRowTestTag-$label")
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                disabled()
            }
            .padding(horizontal = BenchStyleChipPaddingH),
        horizontalArrangement = Arrangement.spacedBy(BenchStyleRowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (swatch != null) {
            // Row 3.9: the dot is the element's own ink, so the chip still reports even though it is inert.
            // Painted at full strength — the 0.35 dim belongs to the chip's own label, not to the artifact's
            // colour, which would otherwise be reported wrong.
            Box(
                Modifier
                    .size(BenchStyleSwatchSize)
                    .clip(CircleShape)
                    .background(swatch)
                    .testTag("$BenchStyleRowTestTag-swatch"),
            )
        }
        Text(
            text = label,
            color = colors.ink.copy(alpha = BenchContextBarDisabledAlpha),
            fontSize = BenchStyleChipTextSize,
            fontWeight = FontWeight.Medium,
            fontFamily = ZinelyTheme.v2Typography.work,
        )
    }
}

/** Frozen `.styletb .done` — `--matcha` under `--on-matcha`, and the row's only live control. */
@Composable
private fun DoneChip(onDone: () -> Unit) {
    val colors = ZinelyTheme.v2Colors
    Box(
        modifier = Modifier
            .height(BenchStyleChipHeight)
            .clip(RoundedCornerShape(BenchStyleChipRadius))
            .background(colors.matcha)
            .clickable(onClick = onDone)
            .padding(horizontal = BenchStyleDonePaddingH)
            .testTag("$BenchStyleRowTestTag-done")
            .clearAndSetSemantics {
                contentDescription = Copy.EditText.DONE
                role = Role.Button
                onClick { onDone(); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = Copy.EditText.DONE,
            color = colors.onMatcha,
            fontSize = BenchStyleDoneTextSize,
            fontWeight = FontWeight.SemiBold,
            fontFamily = ZinelyTheme.v2Typography.work,
        )
    }
}

/**
 * Frozen `.styletb{border-top:1px solid var(--chrome-line)}`. Drawn rather than composed as a divider so
 * the row stays a single node in the semantics tree, and at `1.dp` so it is a hairline at every density.
 */
private fun Modifier.benchStyleRowHairline(colour: Color): Modifier = drawBehind {
    drawRect(color = colour, size = Size(size.width, 1.dp.toPx()))
}
