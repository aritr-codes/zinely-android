package com.aritr.zinely.feature.editor

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.components.zinelyV21Pressable
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.ZinelyV21Press

/** Test tag on the frozen editing-state style row (`.styletb`). */
public const val BenchStyleRowTestTag: String = "bench-style-row"

/**
 * Frozen `.kbstack{transition:transform .26s cubic-bezier(.05,.7,.1,1)}` (`v21-bench.html:264-265`).
 *
 * V2 asked for 340ms. The curve is **byte-identical** to V2's `--settle`, so the V2 motion object is still
 * the right one to ask — what V2.1 changed is the duration, not the arrival. Routing it through
 * [ZinelyTheme.v2Motion] also keeps the reduced-motion downgrade, which is a
 * [V2-CONSTITUTION §III](../../../../../../../../docs/design/V2-CONSTITUTION.md) requirement and has no
 * V2.1 replacement.
 */
internal const val BenchKbStackMillis: Int = 260

/**
 * Frozen `.kbstack{transform:translateY(102%)}` (`v21-bench.html:264`) — the stack rests at **102 %** of its
 * own height below the fold, where V2 asked for 110 %.
 *
 * Still a fraction of the row's own height rather than a dp constant: it has to move with the font scale, or
 * a large text size leaves a sliver of the row on screen.
 */
internal const val BenchKbStackRestFraction: Float = 1.02f

/** Frozen `.styletb{padding:var(--gap-sm) var(--gap-md)}` (`v21-bench.html:267`) — unchanged from V2's 8/12. */
internal val BenchStyleRowPaddingH = ZinelyV21Dimens.gapMd
internal val BenchStyleRowPaddingV = ZinelyV21Dimens.gapSm

/**
 * Frozen `.styletb{gap:var(--gap-sm)}` — also the `.chip`'s own swatch/label gap (`v21-bench.html:267`,
 * `:271`). V2 set both to 6.
 */
internal val BenchStyleRowGap = ZinelyV21Dimens.gapSm

/**
 * ⚠ **V2.1's chips declare no height** (`v21-bench.html:269-272`), where V2 pinned `height:34px`. A chip is
 * now its padding plus its own text, exactly as [BenchBottomBar]'s `.bar` is its padding plus its tallest
 * child — and for the same reason: a pinned height makes the frozen padding decorative and silently
 * outvotes the label the day the font scale changes.
 */
internal val BenchStyleChipPaddingH = ZinelyV21Dimens.gapMd
internal val BenchStyleChipPaddingV = ZinelyV21Dimens.gapSm

/** Frozen `.doneEdit{padding:var(--gap-sm) var(--gap-lg)}` (`v21-bench.html:284`). */
internal val BenchStyleDonePaddingH = ZinelyV21Dimens.gapLg

/**
 * Frozen `.chip`/`.doneEdit{border-radius:var(--br-pill)}` — expressed as a percent shape rather than
 * [ZinelyV21Dimens.radiusPill]'s 999dp so the outline stays exact for the press shadow, which builds its
 * geometry from this shape. Same call [BenchBottomBar] makes.
 */
internal val BenchStyleChipShape: RoundedCornerShape = RoundedCornerShape(percent = 50)

/** Frozen `.chip .sw{width:15px;height:15px}` (`v21-bench.html:279`). V2 drew 14. */
internal val BenchStyleSwatchSize = 15.dp

/** Frozen `.chip{font-size:.78rem}` — 12.48sp at the prototype's 16px root. */
internal val BenchStyleChipTextSize = 12.48.sp

/** Frozen `.doneEdit{font-size:.82rem}` (`v21-bench.html:282`). */
internal val BenchStyleDoneTextSize = 13.12.sp

/**
 * Frozen `.styletb{border-top:1.5px solid var(--ink)}` / `.chip`,`.doneEdit`,`.sw{border:1.5px solid …}`.
 */
internal val BenchStyleRule = 1.5.dp

/**
 * Frozen `.styletb{border-bottom:1.5px dashed var(--hair)}` (`v21-bench.html:268`) — **new in V2.1**, and
 * the row's tell that what sits below it is the keyboard rather than more of the app.
 *
 * CSS derives a dash rhythm from the border width; at 1.5px it renders at roughly 3-on/3-off, which is what
 * this transcribes — the same approximation `ZineShelf`'s placeholder records, and recorded here rather than
 * presented as frozen.
 */
internal val BenchStyleDash = 3.dp

/**
 * The frozen editing-state style row — `.styletb` inside `.kbstack` (`v21-bench.html:263-284`, markup
 * `:521-528`); [ADR-093](../../../../../../../../docs/DECISIONS.md#adr-093) rows 3.4–3.7 and 3.9, re-skinned
 * to V2.1 by [ADR-102](../../../../../../../../docs/DECISIONS.md#adr-102) package P3.
 *
 * ### What P3 changed here
 *
 * The row's *ground* moved from `--sheet` to `--paper`: the style bar is now a torn-off strip of the page's
 * own material rather than a piece of chrome, which is why it also gains a dashed `--hair` rule along its
 * bottom edge — the perforation. Its chips became pills with printed shadows, and `Done` became `--leaf`
 * where it was `--matcha`.
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
 * [D-108](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-108) corrects a stale transcription:
 * the frozen HTML already wires `#editColour` to the Ink tray. The row therefore exposes that one honest
 * action, removes the fake `Font` / `Size` buttons, and quietly points to the complete style surface after
 * Done. The swatch remains the edited element's computed colour (`#editSw`).
 *
 * @param visible whether a text session is open — the frozen `.editing` class.
 * @param inkSwatch the edited element's own colour, for the `Ink` chip's dot (row 3.9).
 * @param onInk ends the edit cleanly and opens Ink after the draft commit.
 * @param onDone the frozen `#doneEdit` handler.
 * @param onDockedTopChanged the row's settled top edge in window coordinates, for the amended pan's
 *   clearance term ([benchEditPanMagnitudeDp], D-043 / OD-16). Reported on every layout, including at rest.
 */
@Composable
internal fun BenchStyleRow(
    visible: Boolean,
    inkSwatch: Color,
    onInk: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    onDockedTopChanged: (Float) -> Unit = {},
) {
    val colors = ZinelyTheme.v21Colors
    // A fraction of the row's OWN height, not a dp constant: `translateY(102%)` moves with the font scale,
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
            // `.styletb` (`:285`) is the prototype's stand-in for the system keyboard — `aria-hidden`,
            // purely scenery — so the real IME takes its place and the host is the ime inset, never a
            // ported key grid and never a fixed offset.
            .imePadding()
            // D-043 / OD-16: the amended pan needs to know where this row DOCKS, so it can lift the page
            // by only what the edited element needs to clear it. Reported here — ABOVE the graphicsLayer
            // below, deliberately: `positionInWindow()` applies layer transforms, so reading it inside or
            // after the layer would return the row's animated paint position and the pan would chase the
            // row's own entrance. Read here it is the settled layout position, which is what "docked"
            // means and the only value that makes the clamp a function of rest geometry.
            .onGloballyPositioned { onDockedTopChanged(it.positionInWindow().y) }
            .graphicsLayer {
                // `size` here is the layer's OWN measured size, so `translateY(102%)` is resolved against
                // the real height on the very first frame. An earlier cut measured it into state via
                // `onSizeChanged`, which is one frame late: the row painted fully docked once, then jumped
                // down and slid up. That flash is invisible to Robolectric and to a settled golden, and
                // plainly visible on a device.
                translationY = offsetFraction * size.height
            },
    ) {
        // Composed only while it is on screen or on its way off it. A `graphicsLayer` translation moves
        // pixels, not nodes: left permanently composed, the row's actions stayed in the platform
        // accessibility tree at rest, and TalkBack read editing controls on an editor with no
        // session open — caught by `SurfaceTraversalOrderTest`, which reads the merged tree in the design's
        // order. The outer Box keeps the animation state so the entrance still runs from the rest value.
        val docked = visible || offsetFraction < BenchKbStackRestFraction
        if (docked) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // The tag goes ABOVE the padding, so the node's bounds are the whole row — ground,
                    // rules and padding included — rather than its inner content box. It sat below the
                    // padding at first, and that is not a cosmetic difference: every geometric assertion
                    // written against this node was silently measuring a rect 8dp short at each end, which
                    // is how a raster probe aimed at the top rule ended up reading the chips instead and
                    // reported a deleted rule as present.
                    .testTag(BenchStyleRowTestTag)
                    .background(colors.surface)
                    .benchStyleRowRules(solid = colors.ink, dashed = colors.hair)
                    .padding(horizontal = BenchStyleRowPaddingH, vertical = BenchStyleRowPaddingV),
                horizontalArrangement = Arrangement.spacedBy(BenchStyleRowGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InkChip(swatch = inkSwatch, onInk = onInk)
                Text(
                    text = Copy.EditText.MORE_STYLES_AFTER_DONE,
                    color = colors.inkSoft,
                    fontSize = 10.88.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = ZinelyV21Fonts.Work,
                    lineHeight = 12.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("$BenchStyleRowTestTag-hint"),
                )
                // The hint owns the frozen flex space: Ink and Done are measured first, so neither can be
                // pushed off-screen when Android's font scale grows. At ordinary scale it still reads as
                // one quiet line; at maximum scale it may wrap to two before ellipsising.
                DoneChip(onDone)
            }
        }
    }
}

/**
 * D-108's live Ink chip. It uses the same flat printed press as Done, so touch and TalkBack both receive an
 * action that agrees with the control's full-strength paint.
 */
@Composable
private fun InkChip(swatch: Color, onInk: () -> Unit) {
    val colors = ZinelyTheme.v21Colors
    val ink = benchTap(action = onInk)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .zinelyV21Pressable(pressed, ZinelyV21Press.Flat, colors.inkLine, BenchStyleChipShape)
            .clip(BenchStyleChipShape)
            .background(colors.surface)
            .border(BenchStyleRule, colors.ink, BenchStyleChipShape)
            .clickable(interactionSource = interaction, indication = null, onClick = ink)
            .testTag("$BenchStyleRowTestTag-${Copy.BenchVerbs.INK}")
            .clearAndSetSemantics {
                contentDescription = Copy.BenchVerbs.INK
                role = Role.Button
                onClick { ink(); true }
            }
            .padding(horizontal = BenchStyleChipPaddingH, vertical = BenchStyleChipPaddingV),
        horizontalArrangement = Arrangement.spacedBy(BenchStyleRowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(BenchStyleSwatchSize)
                .clip(CircleShape)
                .background(swatch)
                .border(BenchStyleRule, colors.ink, CircleShape)
                .testTag("$BenchStyleRowTestTag-swatch"),
        )
        Text(
            text = Copy.BenchVerbs.INK,
            color = colors.inkSoft,
            fontSize = BenchStyleChipTextSize,
            // `.chip{font-weight:600}` — V2 asked for 500.
            fontWeight = FontWeight.SemiBold,
            fontFamily = ZinelyV21Fonts.Work,
            lineHeight = ZinelyV21Fonts.InheritedLineHeight,
        )
    }
}

/**
 * Frozen `.doneEdit` — `--leaf` under `--on-leaf`, the row's primary completion control.
 *
 * ⚠ Its press is an **amendment**, not a transcription: the frozen file gave `.doneEdit` a 2px resting
 * shadow and no `:active` rule. Amended in `v21-bench.html` on 2026-08-14 to the [ZinelyV21Press.Flat]
 * tier its neighbour `.chip` already wears — freeze first, then this file, in that order. `.pgc` and
 * `.dclose` turned out to be in the same state and P5 resolved them the same way, by rest depth; the
 * amendment's first wording called this control unique and was wrong.
 *
 * It wears **no** `--frame` ring. The ring is one-per-screen and the Bench spends it on `.add`; a second
 * one here would make both decoration ([zinelyV21Frame][com.aritr.zinely.ui.components.zinelyV21Frame]).
 */
@Composable
private fun DoneChip(onDone: () -> Unit) {
    val colors = ZinelyTheme.v21Colors
    val done = benchTap(action = onDone)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier
            // ⚠ Nothing that clips may sit to the LEFT of the press — the shadow paints outside the node's
            // own bounds, and a clip above it cuts the shadow off. The `clip` is deliberately downstream.
            .zinelyV21Pressable(pressed, ZinelyV21Press.Flat, colors.inkLine, BenchStyleChipShape)
            .clip(BenchStyleChipShape)
            .background(colors.leaf)
            .border(BenchStyleRule, colors.ink, BenchStyleChipShape)
            .clickable(interactionSource = interaction, indication = null, onClick = done)
            .padding(horizontal = BenchStyleDonePaddingH, vertical = BenchStyleChipPaddingV)
            .testTag("$BenchStyleRowTestTag-done")
            .clearAndSetSemantics {
                contentDescription = Copy.EditText.DONE
                role = Role.Button
                onClick { done(); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = Copy.EditText.DONE,
            color = colors.onLeaf,
            fontSize = BenchStyleDoneTextSize,
            // `.doneEdit{font-weight:700}` — a real Bold, where V2 asked for 600.
            fontWeight = FontWeight.Bold,
            fontFamily = ZinelyV21Fonts.Work,
            lineHeight = ZinelyV21Fonts.InheritedLineHeight,
        )
    }
}

/**
 * Frozen `.styletb{border-top:1.5px solid var(--ink);border-bottom:1.5px dashed var(--hair)}`.
 *
 * Both drawn rather than composed as dividers so the row stays a single node in the semantics tree, and in
 * one `drawBehind` rather than two chained modifiers because a chain's relative order is a thing to be
 * reasoned about rather than read.
 *
 * The strokes are centred on the node's own edges and the half that falls outside is left to the row's
 * bounds — CSS puts a border *inside* a border-box element, so each renders at its full 1.5dp only if the
 * outer half is cut. Drawn at double width for exactly that reason, the same device `ZineShelf`'s dashed
 * placeholder uses.
 */
private fun Modifier.benchStyleRowRules(solid: Color, dashed: Color): Modifier = drawBehind {
    val w = BenchStyleRule.toPx()
    drawLine(
        color = solid,
        start = Offset(0f, 0f),
        end = Offset(size.width, 0f),
        strokeWidth = w * 2f,
    )
    val dash = BenchStyleDash.toPx()
    drawLine(
        color = dashed,
        start = Offset(0f, size.height),
        end = Offset(size.width, size.height),
        strokeWidth = w * 2f,
        cap = StrokeCap.Butt,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
    )
}
