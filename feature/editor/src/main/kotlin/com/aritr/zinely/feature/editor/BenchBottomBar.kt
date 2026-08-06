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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import com.aritr.zinely.ui.theme.ZinelyV2IconPaint
import com.aritr.zinely.ui.theme.ZinelyV2Icons
import com.aritr.zinely.ui.theme.toImageVector

/** Test tag on the frozen bottom bar (`.bar`). */
public const val BenchBottomBarTestTag: String = "bench-bottom-bar"

/** Per-control test tags. Stable hooks for instrumentation as well as unit tests. */
public const val BenchBarUndoTag: String = "bench-bar-undo"
public const val BenchBarRedoTag: String = "bench-bar-redo"
public const val BenchBarAddTag: String = "bench-bar-add"
public const val BenchBarDoneTag: String = "bench-bar-done"

/**
 * The frozen `.add` label (`v2-bench.html:467`) — the literal word the button carries, and its spoken form.
 *
 * It is deliberately **not** `Add photo` or `Add words`: after
 * [OD-21](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-047-ruling) this control names no
 * medium, because choosing the medium is what the chooser it opens is for.
 */
public const val AddActionLabel: String = Copy.BenchBar.ADD

/**
 * The frozen `aria-label`s on the two history controls (`v2-bench.html:465`, `:466`).
 *
 * They outlived `EditorSupplyTray`, which declared them and which
 * [OD-21](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-047-ruling) retires: the *shelf* is gone,
 * the *capabilities* are not, and moving their labels here rather than re-typing them keeps the strings a
 * screen reader speaks identical across the change.
 */
public const val UndoActionLabel: String = Copy.BenchBar.UNDO
public const val RedoActionLabel: String = Copy.BenchBar.REDO

/** Frozen `.bar{height:66px}` (`v2-bench.html:267`). */
internal val BenchBarHeight = 66.dp

/** Frozen `.bar{gap:10px}` (`v2-bench.html:267`). */
internal val BenchBarGap = 10.dp

/**
 * Frozen `.bar{padding:0 16px 4px}` (`v2-bench.html:267`) — the bottom pad is **asymmetric**, and it is a
 * real 4dp rather than a rounding artefact. Under `align-items:center` only the *difference* between top and
 * bottom padding reaches layout, so the row's content sits 2dp above the box's centre; the height and the
 * pad are therefore asserted together, never one standing in for the other.
 */
internal val BenchBarPaddingH = 16.dp
internal val BenchBarPaddingBottom = 4.dp

/** Frozen `.icon-btn{width:44px;height:44px}` (`v2-bench.html:268`). */
internal val BenchIconBtnSize = 44.dp

/** Frozen `.icon-btn`/`.add{border-radius:12px}` (`v2-bench.html:268`, `:271`). */
internal val BenchBarRadius = 12.dp

/** Frozen `.icon-btn:disabled{opacity:.35}` (`v2-bench.html:269`). */
internal const val BenchIconBtnDisabledAlpha: Float = 0.35f

/** Frozen `.icon-btn:active{transform:scale(.94)}` (`v2-bench.html:269`). */
internal const val BenchIconBtnPressedScale: Float = 0.94f

/** Frozen `.add:active{transform:scale(.98)}` (`v2-bench.html:272`). */
internal const val BenchAddPressedScale: Float = 0.98f

/** Frozen `.icon-btn{transition:...,transform .1s}` / `.add{transition:transform .1s}` (`:268`, `:271`). */
internal const val BenchBarPressMillis: Int = 100

/** Frozen `.icon-btn svg{width:20px}` and its inherited `stroke-width:1.7` (`v2-bench.html:270`). */
internal val BenchIconBtnGlyphSize = 20.dp
internal const val BenchIconBtnStroke: Float = 1.7f

/** Frozen `.add{height:44px}` (`v2-bench.html:271`). */
internal val BenchAddHeight = 44.dp

/** Frozen `.add{font-size:14.5px;font-weight:600}` and `gap:8px` (`v2-bench.html:271`). */
internal val BenchAddTextSize = 14.5.sp
internal val BenchAddGap = 8.dp

/** Frozen `.add svg{width:18px}` and its stated `stroke-width:2` (`v2-bench.html:272`). */
internal val BenchAddGlyphSize = 18.dp
internal const val BenchAddStroke: Float = 2f

/**
 * The frozen bottom bar — `.bar` and its four controls (`v2-bench.html:267-272`, markup `:464-468`);
 * [ADR-094](../../../../../../../../docs/DECISIONS.md#adr-094) rows 4.1–4.8b.
 *
 * ### Why four controls and not the freeze's three
 *
 * The freeze drew `Undo · Add · Done`. Three accepted rulings each require something those three slots do
 * not hold: [OD-9](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-031-ruling) keeps **redo**,
 * which the frozen file omits entirely;
 * [OD-11](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-034-ruling) and
 * [OD-14](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-039-ruling) keep **both** shipped add
 * verbs; and `.add`'s own handler opens a chooser whose region OD-2 re-seated beyond Phase C.
 *
 * [OD-21](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-047-ruling) ruled the bar absorbs
 * everything, and **the frozen Bench was amended first** — one line of markup, `#redoBtn` at
 * `v2-bench.html:466`, and no CSS at all: `.icon-btn` already sizes it and `.add` still takes the residual
 * width. This composable implements the amended file, not a Compose-side departure from it.
 *
 * ### `Done` is two-state, and one of its states is *withheld*
 *
 * The frozen handler (`:653`) reads `if (editing) endEdit() else deselect()`. C3 already built the first
 * half as the style row's own `#doneEdit` chip, and a second visible `Done` while a session is open is
 * exactly the duplication OD-14 forbids. So during a session this control is **disabled**, drawn at the
 * frozen `.icon-btn:disabled` `.35` — OD-14's own withholding method, as C2b applied it, using a
 * presentation the frozen file already draws for `#undoBtn`. With no session it owns *clear selection*,
 * which is that capability's **first drawn control**: OD-13 gave it a gesture, and a gesture has no visible
 * presentation for OD-14 to count as a second one. Recorded at
 * [D-048](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-048-ruling).
 *
 * The Read hand-off is **not** here. It ships as `EditorScreen`'s top-end `Preview ›` and stays there —
 * OD-9's *reuse, don't invent* is satisfied by leaving it alone, and moving it would have been the
 * amendment that not moving it avoids.
 *
 * ### Touch targets
 *
 * The frozen controls are 44dp, under the 48dp floor
 * [D-009](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-009--no-control-in-the-frozen-trilogy-declares-a-minimum-touch-target-and-most-measure-well-under-48dp)
 * records. Its ruled remedy is *extend the target, keep the paint*, and the mechanism is Compose's own
 * pointer-input minimum: a `clickable` node smaller than 48dp already reports a 48dp
 * `touchBoundsInRoot`, so the finger reaches the floor while the drawn box stays 44.
 *
 * An explicit `minimumInteractiveComponentSize()` was tried first and **removed**, because here it does
 * not do what its name promises: it grows the *layout* slot to 48 and centres the 44dp paint inside it,
 * which pushed the first control 2dp in from the frozen 16dp padding and opened the frozen 10dp gap to
 * 14dp — measurable, and measured. That is *"modifying the visual design solely to satisfy"* the floor,
 * which D-009's ruling forbids in those words. The same modifier was removed from
 * [ZineOnShelf][com.aritr.zinely.feature.library.ZineOnShelf]'s seam on independent review for the same
 * reason, and its test stayed green. The floor is guarded where it is actually decided — on the platform
 * tree, by `BenchBottomBarPlatformA11yTest`.
 *
 * @param canUndo drives Undo's enabled state — and its *platform* enabled state, which is the one that
 *   matters. Compose can report `enabled` to the platform while `assertIsNotEnabled` passes
 *   ([ADR-058](../../../../../../../../docs/DECISIONS.md#adr-058) branch), so the semantics below are set
 *   explicitly rather than inferred from `clickable(enabled = …)` alone.
 * @param canRedo the same for Redo. The freeze draws it `disabled` at rest and so does this.
 * @param doneEnabled `false` for the whole of a text session — see *`Done` is two-state* above.
 */
@Composable
internal fun BenchBottomBar(
    canUndo: Boolean,
    canRedo: Boolean,
    doneEnabled: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onAdd: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ZinelyTheme.v2Colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(BenchBarHeight)
            // The tag sits ABOVE the ground and the padding, so the node's bounds are the whole 66dp bar
            // including its hairline. C3 learned this the hard way: below the padding, every geometric
            // assertion silently measures the inner content box, and a raster probe aimed at the top
            // hairline reads the controls instead.
            .testTag(BenchBottomBarTestTag)
            .background(colors.chrome)
            .benchBarHairline(colors.chromeLine)
            .padding(start = BenchBarPaddingH, end = BenchBarPaddingH, bottom = BenchBarPaddingBottom),
        horizontalArrangement = Arrangement.spacedBy(BenchBarGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BenchIconButton(
            icon = ZinelyV2Icons.Undo.toImageVector(
                BenchIconBtnGlyphSize,
                ZinelyV2IconPaint.Stroke(BenchIconBtnStroke),
            ),
            label = UndoActionLabel,
            enabled = canUndo,
            testTag = BenchBarUndoTag,
            onClick = onUndo,
        )
        BenchIconButton(
            icon = ZinelyV2Icons.Redo.toImageVector(
                BenchIconBtnGlyphSize,
                ZinelyV2IconPaint.Stroke(BenchIconBtnStroke),
            ),
            label = RedoActionLabel,
            enabled = canRedo,
            testTag = BenchBarRedoTag,
            onClick = onRedo,
        )
        BenchAddButton(onAdd, Modifier.weight(1f))
        BenchIconButton(
            icon = ZinelyV2Icons.Done.toImageVector(
                BenchIconBtnGlyphSize,
                ZinelyV2IconPaint.Stroke(BenchIconBtnStroke),
            ),
            label = Copy.EditText.DONE,
            enabled = doneEnabled,
            testTag = BenchBarDoneTag,
            onClick = onDone,
        )
    }
}

/**
 * Frozen `.icon-btn` — a 44dp square with a 1px `--chrome-line` outline over nothing, its glyph in
 * `--ink-soft`.
 *
 * A disabled instance carries **no `clickable`** rather than `clickable(enabled = false)`, so the platform
 * `AccessibilityNodeInfo` reports it non-clickable too. That distinction is not pedantry: it is the exact
 * defect [ADR-058](../../../../../../../../docs/DECISIONS.md#adr-058) shipped through a green Robolectric
 * suite, and C3's device pass re-verified the fix by dumping the real tree rather than the merged one.
 */
@Composable
private fun BenchIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    val colors = ZinelyTheme.v2Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) BenchIconBtnPressedScale else 1f,
        animationSpec = ZinelyTheme.v2Motion.standard(BenchBarPressMillis),
        label = "bench-icon-btn-press",
    )
    Box(
        modifier = Modifier
            .size(BenchIconBtnSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                // The frozen `opacity:.35` is on the **button**, not on its glyph: it fades the 1px
                // `--chrome-line` outline too. Fading only the tint left a full-strength outlined box with
                // a faint mark inside it — heavier chrome than the freeze draws, in the state the freeze
                // draws most carefully, and at the very first frame every user sees. Caught by independent
                // review; the golden probe could not see it because it measures glyph pixels only.
                alpha = if (enabled) 1f else BenchIconBtnDisabledAlpha
            }
            .clip(RoundedCornerShape(BenchBarRadius))
            .border(1.dp, colors.chromeLine, RoundedCornerShape(BenchBarRadius))
            .then(
                if (enabled) {
                    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .testTag(testTag)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                if (enabled) onClick { onClick(); true } else disabled()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            // No second alpha here: the layer above already carries the frozen `.35` for the whole
            // control, and multiplying them would draw the glyph at `.1225`.
            tint = colors.inkSoft,
            modifier = Modifier.size(BenchIconBtnGlyphSize),
        )
    }
}

/**
 * Frozen `.add` — the bar's one filled control, `--matcha` under `--on-matcha`.
 *
 * `flex:1` is `Modifier.weight(1f)`, passed in by the caller: it takes the residual width between the two
 * fixed 44s on its left and the one on its right, so the bar reflows correctly at every width and font
 * scale rather than at one tested one.
 */
@Composable
private fun BenchAddButton(onAdd: () -> Unit, modifier: Modifier) {
    val colors = ZinelyTheme.v2Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) BenchAddPressedScale else 1f,
        animationSpec = ZinelyTheme.v2Motion.standard(BenchBarPressMillis),
        label = "bench-add-press",
    )
    Row(
        modifier = modifier
            .height(BenchAddHeight)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(BenchBarRadius))
            .background(colors.matcha)
            .clickable(interactionSource = interaction, indication = null, onClick = onAdd)
            .testTag(BenchBarAddTag)
            .clearAndSetSemantics {
                contentDescription = AddActionLabel
                role = Role.Button
                onClick { onAdd(); true }
            },
        horizontalArrangement = Arrangement.spacedBy(BenchAddGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = ZinelyV2Icons.Add.toImageVector(
                BenchAddGlyphSize,
                ZinelyV2IconPaint.Stroke(BenchAddStroke),
            ),
            contentDescription = null,
            tint = colors.onMatcha,
            modifier = Modifier.size(BenchAddGlyphSize),
        )
        Text(
            text = AddActionLabel,
            color = colors.onMatcha,
            fontSize = BenchAddTextSize,
            fontWeight = FontWeight.SemiBold,
            fontFamily = ZinelyTheme.v2Typography.work,
        )
    }
}

/**
 * Frozen `.bar{border-top:1px solid var(--chrome-line)}`. Drawn rather than composed as a divider so the
 * bar stays one node in the semantics tree, and at `1.dp` so it is a hairline at every density.
 */
private fun Modifier.benchBarHairline(colour: Color): Modifier = drawBehind {
    drawRect(color = colour, size = Size(size.width, 1.dp.toPx()))
}
