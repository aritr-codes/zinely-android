package com.aritr.zinely.feature.editor

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.components.zinelyV21Frame
import com.aritr.zinely.ui.components.zinelyV21Pressable
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.ZinelyV21Press
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

/**
 * ⚠ **V2.1's `.bar` declares no height** (`v21-bench.html:341`), where V2 pinned `height:66px`. The bar is
 * now exactly its padding plus its tallest child — the 48dp `.add` — and that is transcribed rather than
 * re-pinned: writing a height back would make the freeze's padding decorative, and would silently outvote
 * `.add` the day it changes. ADR-102 does not say whether the omission was deliberate; the frozen file is
 * canonical either way, so this follows it and flags it.
 *
 * The intrinsic height is `8 + 48 + 16 = 72dp` at rest, against V2's 66.
 */
internal val BenchBarGap = ZinelyV21Dimens.gapSm

/**
 * Frozen `.bar{padding:var(--gap-sm) var(--gap-lg) var(--gap-lg)}` (`v21-bench.html:341`) — 8 top, 16 sides,
 * 16 bottom.
 *
 * V2's pad was `0 16 4`, which under `align-items:center` sat the content 2dp *above* centre. V2.1's is
 * generous and asymmetric the other way: the bar sits **off the foot of the phone**, not flush against it.
 */
internal val BenchBarPaddingTop = ZinelyV21Dimens.gapSm
internal val BenchBarPaddingH = ZinelyV21Dimens.gapLg
internal val BenchBarPaddingBottom = ZinelyV21Dimens.gapLg

/** Frozen `.icon-btn{width:44px;height:44px}` (`v21-bench.html:342`) — unchanged from V2. */
internal val BenchIconBtnSize = 44.dp

/**
 * Frozen `.icon-btn`/`.add{border-radius:var(--br-pill)}` (`v21-bench.html:342`, `:349`).
 *
 * V2 drew both at a 12dp rounded rectangle. V2.1's chrome controls are **pills**, expressed as a percent
 * shape rather than [ZinelyV21Dimens.radiusPill]'s 999dp so a `RoundedCornerShape` outline stays exact for
 * the press shadow and the frame ring, both of which build their geometry from this shape.
 */
internal val BenchBarShape: RoundedCornerShape = RoundedCornerShape(percent = 50)

/** Frozen `.icon-btn`/`.add`/`.chip`/`.gridbtn`/`.fpage{border:1.5px solid var(--ink)}`. */
internal val BenchChromeBorder = 1.5.dp

/**
 * Frozen `.icon-btn:disabled{opacity:.35; box-shadow:none}` (`v21-bench.html:345`).
 *
 * ⚠ The **shadow half is not an alpha**. A disabled control sheds its depth entirely rather than fading it,
 * which is a different statement from a pressed one: pressed means *"you are pushing this"*, disabled means
 * *"there is nothing here to push"*. [ZinelyV21Pressable][com.aritr.zinely.ui.components.zinelyV21Pressable]
 * deliberately cannot express it — its KDoc names this exact case — so the call site states it.
 */
internal const val BenchIconBtnDisabledAlpha: Float = 0.35f

/**
 * Frozen `.icon-btn svg{width:20px;height:20px;stroke-width:1.9}` (`v21-bench.html:347-348`). V2's stroke
 * was 1.7: the whole language got a heavier pen.
 */
internal val BenchIconBtnGlyphSize = 20.dp
internal const val BenchIconBtnStroke: Float = 1.9f

/** Frozen `.add{height:48px}` (`v21-bench.html:349`), up from V2's 44. */
internal val BenchAddHeight = 48.dp

/**
 * Frozen `.add{font-size:1rem;font-weight:700;gap:var(--gap-sm)}` (`v21-bench.html:349-352`).
 *
 * `1rem` is the prototype's root size, 16px, and the weight is a real 700 where V2 asked for 600.
 */
internal val BenchAddTextSize = 16.sp
internal val BenchAddGap = ZinelyV21Dimens.gapSm

/** Frozen `.add svg{width:20px;height:20px;stroke-width:2.4}` (`v21-bench.html:355`). */
internal val BenchAddGlyphSize = 20.dp
internal const val BenchAddStroke: Float = 2.4f

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
 * @param doneEnabled `false` for the whole of a text session — see *`Done` is two-state* above — and,
 *   since F-6, for the whole of an ink session too. Both are the same rule (OD-14: never two live `Done`s),
 *   not two rules: whatever panel owns "finish" right now is the only control allowed to say it.
 * @param doneUnavailableBecause why it is dim, spoken as `stateDescription` and read only when
 *   [doneEnabled] is `false` — the two states withhold it for different reasons and a screen-reader user
 *   otherwise hears "Done, disabled" and stops. Null is honest for a caller that has no reason to give.
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
    doneUnavailableBecause: String? = null,
) {
    val colors = ZinelyTheme.v21Colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            // The tag sits ABOVE the ground and the padding, so the node's bounds are the whole bar. C3
            // learned this the hard way: below the padding, every geometric assertion silently measures the
            // inner content box, and a raster probe aimed at the top edge reads the controls instead.
            .testTag(BenchBottomBarTestTag)
            // ⚠ **No hairline.** V2's `.bar` carried a `--chrome-line` rule along its top edge; V2.1's
            // (`v21-bench.html:341`) declares `background` and nothing else. The bar and the room are the
            // same `desk`, so the boundary the hairline used to draw is now drawn by the controls' own ink
            // borders — which is the language's whole move: marks, not surfaces.
            .background(colors.desk)
            .padding(
                top = BenchBarPaddingTop,
                start = BenchBarPaddingH,
                end = BenchBarPaddingH,
                bottom = BenchBarPaddingBottom,
            ),
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
            unavailableBecause = doneUnavailableBecause,
        )
    }
}

/**
 * Frozen `.icon-btn` (`v21-bench.html:342-348`) — a 44dp **pill** in `paper`, drawn with a 1.5dp `ink`
 * border, its glyph in `inkSoft`, sitting on [ZinelyV21Press.Flat]'s 2dp printed shadow.
 *
 * ### Three V2 habits this sheds
 *
 * The V2 control was a 12dp rounded rectangle with a 1dp `--chrome-line` outline **over nothing** — the
 * chrome ground showed through it — and it pressed by scaling to `.94`. All three are gone: the pill has its
 * own `paper` ground, the outline is real ink at the language's 1.5dp pen, and the press is a translate with
 * the shadow collapsing under it. Nothing here animates: a V2.1 press is a position, not a transition, which
 * is why [zinelyV21Pressable]'s KDoc notes `prefers-reduced-motion` needs no branch.
 *
 * ### Disabled sheds its depth, and that is not an alpha
 *
 * `.icon-btn:disabled` is `opacity:.35` **and** `box-shadow:none`. The alpha fades the whole control (see
 * below); the shadow is removed outright by dropping [zinelyV21Pressable] from the chain. A faded shadow
 * would still say *"this object stands off the desk"* about a control that cannot be pushed.
 *
 * A disabled instance also carries **no `clickable`** rather than `clickable(enabled = false)`, so the
 * platform `AccessibilityNodeInfo` reports it non-clickable too. That distinction is not pedantry: it is the
 * exact defect [ADR-058](../../../../../../../../docs/DECISIONS.md#adr-058) shipped through a green
 * Robolectric suite, and C3's device pass re-verified the fix by dumping the real tree rather than the merged
 * one.
 */
@Composable
private fun BenchIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit,
    unavailableBecause: String? = null,
) {
    val colors = ZinelyTheme.v21Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // Through [benchTap], like every other Bench control — this button IS Undo, Redo and Done, so one
    // seam serves three and cannot drift between them.
    val activate = benchTap(action = onClick)
    Box(
        modifier = Modifier
            .size(BenchIconBtnSize)
            .graphicsLayer {
                // The frozen `opacity:.35` is on the **button**, not on its glyph: it fades the border too.
                // Fading only the tint left a full-strength outlined box with a faint mark inside it —
                // heavier chrome than the freeze draws, in the state the freeze draws most carefully, and at
                // the very first frame every user sees. Caught by independent review; the golden probe could
                // not see it because it measures glyph pixels only.
                alpha = if (enabled) 1f else BenchIconBtnDisabledAlpha
            }
            // ⚠ Nothing that clips may sit to the LEFT of the press — the shadow paints outside the node's
            // own bounds, and a clip above it cuts the shadow off. The `clip` is deliberately downstream.
            .then(
                if (enabled) {
                    Modifier.zinelyV21Pressable(pressed, ZinelyV21Press.Flat, colors.inkLine, BenchBarShape)
                } else {
                    Modifier
                },
            )
            .clip(BenchBarShape)
            .background(colors.surface)
            .border(BenchChromeBorder, colors.ink, BenchBarShape)
            .then(
                if (enabled) {
                    Modifier.clickable(interactionSource = interaction, indication = null, onClick = activate)
                } else {
                    Modifier
                },
            )
            .testTag(testTag)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                if (enabled) {
                    onClick { activate(); true }
                } else {
                    disabled()
                    // F-1's rule, applied where F-6 created a second reason to be dim: a drawn control that
                    // is disabled says why. State, not name — "Done" is still what this button is called.
                    // Undo and Redo pass nothing and correctly stay silent: nothing revives them but doing
                    // something, which is not an instruction anyone needs.
                    unavailableBecause?.let { stateDescription = it }
                }
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
 * Frozen `.add` (`v21-bench.html:349-355`) — the bar's one filled control, `leaf` under `onLeaf`, and the
 * **only object on this screen that wears the misregistration ring**.
 *
 * ### The ring is the point, and it is reserved
 *
 * `.add` stacks two shadows: [ZinelyV21Press.Hero]'s 4dp printed shadow *and* `0 0 0 var(--frame)` in
 * `butterTint` — a flat band riso-printing borrowed from the reference sites as a plate that missed
 * ([V21-SPEC §4.3](../../../../../../../../docs/design/V21-SPEC.md)). [zinelyV21Frame]'s KDoc states the
 * rule this call site has to keep: it is for **the one primary action on a screen**, because a second ring
 * makes both of them decoration.
 *
 * Applied *before* the press in the chain, which is what puts it underneath: CSS paints the first-declared
 * shadow layer on top, and of two chained `drawBehind`s the left one paints first. Both travel with the
 * press, because `Modifier.offset` moves the whole node — under a finger the object approaches the desk and
 * the ring goes with it, exactly as `transform` carries a `box-shadow`.
 *
 * `flex:1` is `Modifier.weight(1f)`, passed in by the caller: it takes the residual width between the two
 * fixed 44s on its left and the one on its right, so the bar reflows correctly at every width and font
 * scale rather than at one tested one.
 */
@Composable
private fun BenchAddButton(onAdd: () -> Unit, modifier: Modifier) {
    val colors = ZinelyTheme.v21Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val addWithHaptic = benchTap(action = onAdd)
    Row(
        modifier = modifier
            .height(BenchAddHeight)
            // Ring first so it lands under the shadow; neither may sit right of a clip.
            .zinelyV21Frame(colors.butterTint, BenchBarShape)
            .zinelyV21Pressable(pressed, ZinelyV21Press.Hero, colors.inkLine, BenchBarShape)
            .clip(BenchBarShape)
            .background(colors.leaf)
            .border(BenchChromeBorder, colors.ink, BenchBarShape)
            .clickable(interactionSource = interaction, indication = null, onClick = addWithHaptic)
            .testTag(BenchBarAddTag)
            .clearAndSetSemantics {
                contentDescription = AddActionLabel
                role = Role.Button
                onClick { addWithHaptic(); true }
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
            tint = colors.onLeaf,
            modifier = Modifier.size(BenchAddGlyphSize),
        )
        Text(
            text = AddActionLabel,
            color = colors.onLeaf,
            fontSize = BenchAddTextSize,
            // `.add{font-weight:700}` — a real Bold, where V2 asked for 600.
            fontWeight = FontWeight.Bold,
            fontFamily = ZinelyV21Fonts.Work,
            lineHeight = ZinelyV21Fonts.InheritedLineHeight,
        )
    }
}
