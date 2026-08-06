package com.aritr.zinely.feature.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.ui.components.zinelyShadow
import com.aritr.zinely.ui.theme.ZinelyContentInks
import com.aritr.zinely.ui.theme.ZinelyMakerInkId
import com.aritr.zinely.ui.theme.ZinelyNeutralId
import com.aritr.zinely.ui.theme.ZinelyPaperTintId
import com.aritr.zinely.ui.theme.ZinelyShadowLayer
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV2Standard
import kotlin.math.roundToInt

internal const val BenchInkPopoverTestTag: String = "bench-ink-popover"
internal const val BenchInkSwatchTestTag: String = "bench-ink-swatch"
internal const val BenchInkPresetTestTag: String = "bench-ink-preset"
internal const val BenchInkBandLabelTestTag: String = "bench-ink-band-label"
internal const val BenchInkUseNoteTestTag: String = "bench-ink-use-note"
internal const val BenchInkDoneTestTag: String = "bench-ink-done"

/**
 * One swatch of the maker palette: the colour and the name the maker hears and reads.
 *
 * The pair is modelled here rather than passed as a bare [Color] because a swatch with no name is a
 * control a screen reader cannot announce, and the frozen arrays carry both (`['Matcha','#7C8A3F']`).
 */
internal data class BenchInkSwatch(val name: String, val value: Color)

/** One labelled band of the popover — `bandHTML(title, arr)` (`v2-bench.html:675-679`). */
internal data class BenchInkBand(val label: String, val swatches: List<BenchInkSwatch>)

/**
 * One ready-made palette. [dots] is the whole recipe, drawn as overlapping circles; [applied] is what a
 * tap actually sets.
 *
 * **[applied] is `dots.first()`, and that is [OD-24](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-028-ruling).**
 * The frozen prototype applied `PRESETS[i][1][1]` — the *second* colour — and measured on the paper this
 * product prints, that is the least legible colour available in all three recipes (2.75:1, 1.90:1,
 * 2.56:1) while every first colour clears AA (15.20, 5.17, 7.32). A preset is `[ink, accent, paper]`:
 * two of the three recipes end in a *paper tint* by value (`Cream`, `Sky`), which is what decoded it. The
 * frozen file was amended first, per the HTML-first rule.
 */
internal data class BenchInkPreset(val name: String, val dots: List<BenchInkSwatch>) {
    init {
        require(dots.isNotEmpty()) { "a preset with no colours cannot be applied" }
    }

    val applied: BenchInkSwatch get() = dots.first()
}

/**
 * The bands offered for one ink target — the pure half of the popover, and the whole of
 * [OD-24](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-028-ruling)'s fence.
 *
 * **A text element is offered `Inks` + `Neutrals` and no paper tints.** The tints are *paper* — by the
 * band's own frozen label (`Paper tints`), by the popover's own caption (*"riso spot-inks"*), and by the
 * presets' third slot, which in two of three recipes is a tint by value. Applying one as a text element's
 * single ink is what let `Cream #F1E9D6` reach a title at 1.21:1 against the paper `Background.None`
 * actually prints. The band is **fenced, not deleted**: [ZinelyContentInks.paperTints] is untouched, the
 * label still lives in `:core:copy`, and the band returns the day a paper target exists — which is not
 * Phase C ([OD-2](../../../../../../../../docs/DECISIONS.md#adr-089) re-seated `.decor` and the tray).
 * That is [OD-21](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-047-ruling)'s own distinction:
 * a fence reassignment, not a capability one.
 *
 * Pure, so the *sets* are asserted directly rather than through a composition — and asserted as
 * band-label order plus per-band swatch order, because a permutation satisfies "every colour is present"
 * and is still the wrong palette.
 */
internal fun benchInkBands(inks: ZinelyContentInks, kind: BenchVerbKind): List<BenchInkBand> {
    val band1 = BenchInkBand(
        Copy.BenchInk.INKS,
        inks.makerInks.map { BenchInkSwatch(benchInkName(it.id), it.value) },
    )
    val tints = BenchInkBand(
        Copy.BenchInk.PAPER_TINTS,
        inks.paperTints.map { BenchInkSwatch(benchInkName(it.id), it.value) },
    )
    val band3 = BenchInkBand(
        Copy.BenchInk.NEUTRALS,
        inks.neutrals.map { BenchInkSwatch(benchInkName(it.id), it.value) },
    )
    return when (kind) {
        BenchVerbKind.TEXT -> listOf(band1, band3)
        // Not `else`: the day a target that takes paper arrives, this must be a decision someone makes
        // rather than a default someone inherits.
        BenchVerbKind.PHOTO, BenchVerbKind.DECOR -> listOf(band1, tints, band3)
    }
}

/**
 * The three frozen recipes (`PRESETS`, `v2-bench.html:599`), resolved against the typed palette rather
 * than re-typed as hexes — so a recipe cannot drift from the band it is made of.
 */
internal fun benchInkPresets(inks: ZinelyContentInks): List<BenchInkPreset> = listOf(
    BenchInkPreset(
        Copy.BenchInk.PRESET_TWO_COLOUR,
        listOf(inks.swatch(ZinelyMakerInkId.Ink), inks.swatch(ZinelyMakerInkId.Strawberry)),
    ),
    BenchInkPreset(
        Copy.BenchInk.PRESET_WARM,
        listOf(
            inks.swatch(ZinelyMakerInkId.Brick),
            inks.swatch(ZinelyMakerInkId.Sunflower),
            inks.swatch(ZinelyPaperTintId.Cream),
        ),
    ),
    BenchInkPreset(
        Copy.BenchInk.PRESET_COOL,
        listOf(
            inks.swatch(ZinelyMakerInkId.Forest),
            inks.swatch(ZinelyMakerInkId.Aqua),
            inks.swatch(ZinelyPaperTintId.Sky),
        ),
    ),
)

private fun ZinelyContentInks.swatch(id: ZinelyMakerInkId) = BenchInkSwatch(benchInkName(id), this[id].value)

private fun ZinelyContentInks.swatch(id: ZinelyPaperTintId) = BenchInkSwatch(benchInkName(id), this[id].value)

/** Band 1's display names. A `when` rather than a map so a new ink is a compile error, not a blank chip. */
internal fun benchInkName(id: ZinelyMakerInkId): String = when (id) {
    ZinelyMakerInkId.Matcha -> Copy.BenchInk.MATCHA
    ZinelyMakerInkId.Forest -> Copy.BenchInk.FOREST
    ZinelyMakerInkId.Strawberry -> Copy.BenchInk.STRAWBERRY
    ZinelyMakerInkId.Brick -> Copy.BenchInk.BRICK
    ZinelyMakerInkId.Sunflower -> Copy.BenchInk.SUNFLOWER
    ZinelyMakerInkId.Ochre -> Copy.BenchInk.OCHRE
    ZinelyMakerInkId.Aqua -> Copy.BenchInk.AQUA
    ZinelyMakerInkId.Cornflower -> Copy.BenchInk.CORNFLOWER
    ZinelyMakerInkId.Plum -> Copy.BenchInk.PLUM
    ZinelyMakerInkId.Ink -> Copy.BenchInk.INK
}

/** Band 2's display names. Fenced for a text target — see [benchInkBands]. */
internal fun benchInkName(id: ZinelyPaperTintId): String = when (id) {
    ZinelyPaperTintId.Cream -> Copy.BenchInk.CREAM
    ZinelyPaperTintId.Blush -> Copy.BenchInk.BLUSH
    ZinelyPaperTintId.Sky -> Copy.BenchInk.SKY
    ZinelyPaperTintId.Sage -> Copy.BenchInk.SAGE
    ZinelyPaperTintId.Kraft -> Copy.BenchInk.KRAFT
}

/** Band 3's display names. `Ink` repeats band 1's, verbatim from the frozen source. */
internal fun benchInkName(id: ZinelyNeutralId): String = when (id) {
    ZinelyNeutralId.Ink -> Copy.BenchInk.INK
    ZinelyNeutralId.Slate -> Copy.BenchInk.SLATE
    ZinelyNeutralId.Stone -> Copy.BenchInk.STONE
    ZinelyNeutralId.Fog -> Copy.BenchInk.FOG
}

/**
 * The frozen ink popover — `.inkpop` (`v2-bench.html:377-390`, behaviour `:679-704`),
 * [ADR-096](../../../../../../../../docs/DECISIONS.md#adr-096).
 *
 * A card at the canvas's bottom edge, inset 12dp on three sides, exactly where [BenchContextBar] sits —
 * and it **replaces** that bar rather than stacking on it, which is the freeze's own behaviour
 * (`openInk` runs `ctx.classList.remove('show')`, `inkClose` restores it). The host owns that swap; this
 * composable owns only its own visibility.
 *
 * ### The selection ring reads the document, not the last tap
 *
 * The prototype sets `.sel` on whatever was clicked, because it has no document to ask. Here [selected]
 * is the element's **own** colour, so the ring survives undo, redo, a page change and a reselect — and an
 * element whose ink came from the shipped Type bar (`Coral`, `Teal`, `Blue`, which are in no frozen band)
 * correctly shows **no** ring rather than a stale one. This is the same call
 * [OD-22](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-053-ruling) made for the filmstrip:
 * where the prototype draws a stand-in because it has no artifact, the product draws the artifact. It
 * changes no frozen paint — `.sel::after` is transcribed exactly — only what "chosen" means.
 *
 * ### Touch targets
 *
 * `.sw2` is 26dp on a 33dp pitch, in a row that **wraps** — so this is
 * [D-009](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-009--no-control-in-the-frozen-trilogy-declares-a-minimum-touch-target-and-most-measure-well-under-48dp)'s
 * overlap case in *two* dimensions, where C5's filmstrip met it in one. D-009 rules *extend the target,
 * keep the paint*, and Compose's own pointer-input minimum does exactly that: a `selectable` node under
 * 48dp still reports 48dp of `touchBoundsInRoot`. No `minimumInteractiveComponentSize()` — it grows the
 * *layout* slot and would move the frozen 7dp gaps. Where expanded targets meet, the nearer centre wins;
 * that is inherent to a 26dp control on a 33dp pitch and is the honest reading of D-009 here.
 *
 * @param visible drives the frozen enter: a 14dp rise and a fade over `.22s var(--standard)`. Collapses
 *   to 0ms under reduced motion ([ADR-075](../../../../../../../../docs/DECISIONS.md#adr-075)).
 * @param bands the frozen bands for this ink target — [benchInkBands].
 * @param presets the three recipes — [benchInkPresets].
 * @param selected the element's current ink, or `null` when it matches no swatch in any offered band.
 * @param inkCount distinct inks in the whole zine, for the `.inkuse` note. Live, never a constant.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BenchInkPopover(
    visible: Boolean,
    bands: List<BenchInkBand>,
    presets: List<BenchInkPreset>,
    selected: Color?,
    inkCount: Int,
    onPick: (BenchInkSwatch) -> Unit,
    onPreset: (BenchInkPreset) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ZinelyTheme.v2Colors
    val type = ZinelyTheme.v2Typography
    val motion = if (ZinelyTheme.motion.reduceMotion) 0 else BenchInkPopoverEnterMillis
    val spec = tween<Float>(motion, easing = ZinelyV2Standard)
    val enterPx = with(LocalDensity.current) { BenchInkPopoverEnterOffsetDp.roundToPx() }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(tween(motion, easing = ZinelyV2Standard)) { enterPx } + fadeIn(spec),
        exit = slideOutVertically(tween(motion, easing = ZinelyV2Standard)) { enterPx } + fadeOut(spec),
    ) {
        val shape = RoundedCornerShape(BenchInkPopoverRadiusDp)
        Column(
            modifier = Modifier
                .padding(BenchInkPopoverInsetDp)
                // Tagged inside the inset, for the reason C2b recorded: tagged outside it the node's
                // bounds are the whole canvas and the frozen 12dp inset is unmeasurable.
                .testTag(BenchInkPopoverTestTag)
                // The card swallows taps that miss a swatch — the gaps between 26dp circles are holes,
                // and since OD-13 a tap that reaches the canvas deselects, which would take the popover
                // and the selection away together. C2b measured that failure on device; the same 7dp
                // gaps exist here, more of them.
                .pointerInput(Unit) { detectTapGestures { } }
                // `0 14px 34px -12px var(--frame-shadow)`.
                .zinelyShadow(
                    listOf(
                        ZinelyShadowLayer(
                            dy = 14.dp,
                            blur = 34.dp,
                            color = colors.frameShadow,
                            spread = (-12).dp,
                        ),
                    ),
                    shape,
                )
                .background(colors.sheet, shape)
                .border(1.dp, colors.chromeLine, shape)
                .padding(BenchInkPopoverPadding),
        ) {
            // `.inkpop h4{margin:0 0 8px;display:flex;justify-content:space-between;align-items:center}`
            //
            // `fillMaxWidth` is what makes `SpaceBetween` mean anything: without it the Row wraps its
            // two children and there is no free space to distribute, so `Done` sits hard against the
            // title and the header reads "InkDone". Measured on device at 411dp — title glyphs ended at
            // x=128px and `Done` ran 132..203px, where the frozen header puts it at the card's right
            // edge (~1007px). Neither the golden nor any semantics assertion could see it: the golden
            // was recorded from this layout, and both texts were present and displayed.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = BenchInkHeaderGapDp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = Copy.BenchInk.TITLE,
                    fontFamily = type.voice,
                    fontWeight = FontWeight.Medium,
                    fontSize = BenchInkTitleSp,
                    color = colors.ink,
                )
                // `.inkpop h4 button` — no background, no border, --ink-faint, 12px, sans.
                Text(
                    text = Copy.BenchInk.DONE,
                    fontFamily = type.work,
                    fontSize = BenchInkDoneSp,
                    color = colors.inkFaint,
                    modifier = Modifier
                        .testTag(BenchInkDoneTestTag)
                        .clickable(role = Role.Button, onClick = onDone),
                )
            }

            // The ring belongs to ONE swatch, and colour alone cannot identify one: `Ink #2A251E` is a
            // member of both the Inks band and the Neutrals band (`ZinelyContentInks.kt:219`, `:231` —
            // the freeze repeats it verbatim, `INKS` at `:596` and `NEUT` at `:598`), and for a text
            // target both bands are drawn. Ringing by `selected == swatch.value` therefore ringed the
            // element's ink TWICE and published `Selected` on two RadioButton nodes in one group. The
            // freeze rings exactly one (`:694` clears every `.sel` before setting one), and the OD-24
            // amendment log lists that exclusivity under NOTHING ELSE CHANGES.
            //
            // Resolved to the FIRST occurrence in band order, which is the Inks band: `Ink` is a riso
            // ink that the Neutrals band repeats, not the other way round.
            val ringed: Pair<Int, Int>? = selected?.let { value ->
                bands.withIndex().firstNotNullOfOrNull { (bandIndex, band) ->
                    band.swatches.indexOfFirst { it.value == value }
                        .takeIf { it >= 0 }
                        ?.let { bandIndex to it }
                }
            }
            for ((bandIndex, band) in bands.withIndex()) {
                Column(modifier = Modifier.padding(bottom = BenchInkBandGapDp)) {
                    BenchInkBandLabel(band.label)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(BenchInkSwatchGapDp),
                        verticalArrangement = Arrangement.spacedBy(BenchInkSwatchGapDp),
                    ) {
                        for ((swatchIndex, swatch) in band.swatches.withIndex()) {
                            BenchInkSwatchDot(
                                swatch = swatch,
                                selected = ringed == bandIndex to swatchIndex,
                                onClick = { onPick(swatch) },
                            )
                        }
                    }
                }
            }

            // The fourth band. `openInk` builds it with its own `.band` and `.bl`, so it is a band like
            // the others — ADR-089 row 6.3 said "three bands" and the freeze emits four (ADR-096 §2).
            //
            // No bottom gap on THIS band. `.band{margin-bottom:9px}` (`:382`) and `.inkuse{margin-top:9px}`
            // (`:389`) are adjacent block-level siblings inside `.inkpop`, which declares no `display`
            // (`:377`) and is therefore a block box — so the two margins COLLAPSE to 9px, not 18. Compose
            // has no margin collapsing, so the gap is applied once, by the note. Carrying both was an
            // 18dp gap the freeze does not specify, and it sat inside a block that rows 6.1h and 6.10c
            // close with a recorded frame — which, being recorded from this implementation, could never
            // have caught it.
            Column {
                BenchInkBandLabel(Copy.BenchInk.PRESETS)
                FlowRow(
                    modifier = Modifier.padding(top = BenchInkPresetsTopGapDp),
                    horizontalArrangement = Arrangement.spacedBy(BenchInkPresetGapDp),
                    verticalArrangement = Arrangement.spacedBy(BenchInkPresetGapDp),
                ) {
                    for (preset in presets) {
                        BenchInkPresetPill(preset = preset, onClick = { onPreset(preset) })
                    }
                }
            }

            // `.inkuse` — the ink-economy note, with a live count.
            Row(
                modifier = Modifier
                    .padding(top = BenchInkUseTopGapDp)
                    .testTag(BenchInkUseNoteTestTag)
                    // One node, one sentence: the shield is decorative and a reader that announced it
                    // separately would say "image" before the only words that matter.
                    .clearAndSetSemantics { contentDescription = Copy.BenchInk.useNote(inkCount) },
                horizontalArrangement = Arrangement.spacedBy(BenchInkUseGapDp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BenchInkShield(tint = colors.inkFaint)
                Text(
                    text = Copy.BenchInk.useNote(inkCount),
                    fontFamily = type.work,
                    fontSize = BenchInkUseSp,
                    color = colors.inkFaint,
                )
            }
        }
    }
}

/** `.band .bl` — 10px/600, `.12em` tracking, uppercase, `--ink-faint`, 5px under itself. */
@Composable
private fun BenchInkBandLabel(label: String) {
    Text(
        text = label.uppercase(),
        fontFamily = ZinelyTheme.v2Typography.work,
        fontSize = BenchInkBandLabelSp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = BenchInkBandLabelTracking,
        color = ZinelyTheme.v2Colors.inkFaint,
        modifier = Modifier
            .padding(bottom = BenchInkBandLabelGapDp)
            .testTag(BenchInkBandLabelTestTag),
    )
}

/**
 * One `.sw2`. Three rings, and each is a different frozen thing: the 2px translucent white *inside* the
 * 26dp box (`border`, under `box-sizing:border-box`), the 1px `--desk-edge` hairline *outside* it
 * (`box-shadow 0 0 0 1px`, which takes no layout space), and — when chosen — the 1.5px `--matcha` ring at
 * `inset:-5px`. The outer two are drawn rather than bordered because Compose's `border` is always inside
 * the layout bounds, and growing the box to hold them would move the frozen 7dp gaps.
 */
@Composable
private fun BenchInkSwatchDot(swatch: BenchInkSwatch, selected: Boolean, onClick: () -> Unit) {
    val colors = ZinelyTheme.v2Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // `.sw2{transition:transform .1s} .sw2:active{transform:scale(.9)}`
    val scale by animateFloatAsState(
        targetValue = if (pressed) BenchInkSwatchPressedScale else 1f,
        animationSpec = tween(if (ZinelyTheme.motion.reduceMotion) 0 else BenchInkSwatchPressMillis),
        label = "sw2-press",
    )
    Box(
        modifier = Modifier
            .size(BenchInkSwatchSizeDp)
            .scale(scale)
            .drawBehind {
                val half = size.minDimension / 2f
                // `box-shadow:0 0 0 1px var(--desk-edge)` — a spread ring outside the border box.
                val halo = BenchInkSwatchHaloDp.toPx()
                drawCircle(
                    color = colors.deskEdge,
                    radius = half + halo / 2f,
                    style = Stroke(width = halo),
                )
                if (selected) {
                    // `.sw2.sel::after{inset:-5px;border:1.5px solid var(--matcha)}`
                    val ring = BenchInkSelRingDp.toPx()
                    drawCircle(
                        color = colors.matcha,
                        radius = half + BenchInkSelInsetDp.toPx() + ring / 2f,
                        style = Stroke(width = ring),
                    )
                }
            }
            .background(swatch.value, CircleShape)
            .border(BenchInkSwatchBorderDp, BenchInkSwatchBorderColor, CircleShape)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                // A single-choice group of circles IS a radio group, and that is what the platform has
                // a state for. C5 learned that Compose routes `Selected` to `isChecked` for every role
                // but `Role.Tab`; a radio button is the one role for which "selected / not selected" is
                // Android's own wording rather than a wrong one borrowed from a checkbox.
                role = Role.RadioButton,
                onClick = onClick,
            )
            .testTag(BenchInkSwatchTestTag)
            .semantics { contentDescription = Copy.BenchInk.swatchLabel(swatch.name) },
    )
}

/**
 * One `.preset` pill: the overlapping dots, then the recipe's name. The dots are decorative — three
 * circles are not speakable — so the pill publishes one node whose name carries the recipe **and its
 * primary ink**, which under OD-24 is the colour the tap applies.
 */
@Composable
private fun BenchInkPresetPill(preset: BenchInkPreset, onClick: () -> Unit) {
    val colors = ZinelyTheme.v2Colors
    val shape = RoundedCornerShape(BenchInkPresetRadiusDp)
    Row(
        modifier = Modifier
            .border(1.dp, colors.chromeLine, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(BenchInkPresetPadding)
            .testTag(BenchInkPresetTestTag)
            .clearAndSetSemantics {
                contentDescription = Copy.BenchInk.presetLabel(preset.name, preset.applied.name)
                role = Role.Button
            },
        horizontalArrangement = Arrangement.spacedBy(BenchInkPresetGlyphGapDp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // `.preset .dots span{width:12px;height:12px;margin-right:-4px;border:1.5px solid var(--sheet)}`
        Row(horizontalArrangement = Arrangement.spacedBy(-BenchInkPresetDotOverlapDp)) {
            for (dot in preset.dots) {
                Box(
                    modifier = Modifier
                        .size(BenchInkPresetDotDp)
                        .background(dot.value, CircleShape)
                        .border(BenchInkPresetDotBorderDp, colors.sheet, CircleShape),
                )
            }
        }
        Text(
            text = preset.name,
            fontFamily = ZinelyTheme.v2Typography.work,
            fontSize = BenchInkPresetSp,
            color = colors.ink,
            textAlign = TextAlign.Start,
        )
    }
}

/**
 * `.inkuse svg` — the shield, authored on a 24-unit viewBox at `stroke-width:1.7`, `fill:none`, drawn in
 * the same 24-unit space scaled by `u = size / 24`, exactly as [ShelfGlyphs] does.
 */
@Composable
private fun BenchInkShield(tint: Color) {
    Canvas(modifier = Modifier.size(BenchInkUseGlyphDp)) {
        val u = size.minDimension / 24f
        // d="M12 3l7 3v6c0 4-3 7-7 9-4-2-7-5-7-9V6z"
        val path = Path().apply {
            moveTo(12f * u, 3f * u)
            lineTo(19f * u, 6f * u)
            lineTo(19f * u, 12f * u)
            cubicTo(19f * u, 16f * u, 16f * u, 19f * u, 12f * u, 21f * u)
            cubicTo(8f * u, 19f * u, 5f * u, 16f * u, 5f * u, 12f * u)
            lineTo(5f * u, 6f * u)
            close()
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = BenchInkUseGlyphStrokeDp.toPx(), join = StrokeJoin.Miter),
        )
    }
}

// — the frozen numbers (`v2-bench.html:377-390`) —

/** `.inkpop{left:12px;right:12px;bottom:12px}`. */
internal val BenchInkPopoverInsetDp = 12.dp

/** `.inkpop{border-radius:16px}`. */
internal val BenchInkPopoverRadiusDp = 16.dp

/** `.inkpop{padding:12px 14px 14px}` — CSS shorthand: top 12, sides 14, bottom 14. */
internal val BenchInkPopoverPadding = PaddingValues(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 14.dp)

/** `.inkpop{transition:transform .22s var(--standard),opacity .22s}`. */
internal const val BenchInkPopoverEnterMillis: Int = 220

/** `.inkpop{transform:translateY(14px)}` at rest. */
internal val BenchInkPopoverEnterOffsetDp = 14.dp

/** `.inkpop h4{margin:0 0 8px}`. */
internal val BenchInkHeaderGapDp = 8.dp

/** `.inkpop h4{font-size:15px}`, `--serif`, weight 500. */
internal val BenchInkTitleSp = 15.sp

/** `.inkpop h4 button{font-size:12px}`, `--sans`, `--ink-faint`. */
internal val BenchInkDoneSp = 12.sp

/** `.band{margin-bottom:9px}`. */
internal val BenchInkBandGapDp = 9.dp

/** `.band .bl{font-size:10px}`, weight 600, uppercase, `--ink-faint`. */
internal val BenchInkBandLabelSp = 10.sp

/** `.band .bl{letter-spacing:.12em}`. */
internal val BenchInkBandLabelTracking = 0.12.em

/** `.band .bl{margin-bottom:5px}`. */
internal val BenchInkBandLabelGapDp = 5.dp

/** `.swrow{gap:7px}`. */
internal val BenchInkSwatchGapDp = 7.dp

/** `.sw2{width:26px;height:26px}`, border-box. */
internal val BenchInkSwatchSizeDp = 26.dp

/** `.sw2{border:2px solid rgba(255,255,255,.6)}` — inside the 26dp box. */
internal val BenchInkSwatchBorderDp = 2.dp

/** `rgba(255,255,255,.6)`. A literal, not a token: the freeze writes it as one. */
internal val BenchInkSwatchBorderColor = Color.White.copy(alpha = 0.6f)

/** `.sw2{box-shadow:0 0 0 1px var(--desk-edge)}` — outside the box, no layout cost. */
internal val BenchInkSwatchHaloDp = 1.dp

/** `.sw2.sel::after{inset:-5px}`. */
internal val BenchInkSelInsetDp = 5.dp

/** `.sw2.sel::after{border:1.5px solid var(--matcha)}`. */
internal val BenchInkSelRingDp = 1.5.dp

/** `.sw2:active{transform:scale(.9)}`. */
internal const val BenchInkSwatchPressedScale: Float = 0.9f

/** `.sw2{transition:transform .1s}`. */
internal const val BenchInkSwatchPressMillis: Int = 100

/** `.presets{gap:8px}`. */
internal val BenchInkPresetGapDp = 8.dp

/** `.presets{margin-top:2px}`. */
internal val BenchInkPresetsTopGapDp = 2.dp

/** `.preset{border-radius:20px}`. */
internal val BenchInkPresetRadiusDp = 20.dp

/** `.preset{padding:5px 9px 5px 6px}` — top 5, right 9, bottom 5, left 6. */
internal val BenchInkPresetPadding = PaddingValues(start = 6.dp, top = 5.dp, end = 9.dp, bottom = 5.dp)

/** `.preset{gap:6px}` — between the dots and the name. */
internal val BenchInkPresetGlyphGapDp = 6.dp

/** `.preset{font-size:11.5px}`. */
internal val BenchInkPresetSp = 11.5.sp

/** `.preset .dots span{width:12px;height:12px}`. */
internal val BenchInkPresetDotDp = 12.dp

/** `.preset .dots span{margin-right:-4px}`. */
internal val BenchInkPresetDotOverlapDp = 4.dp

/** `.preset .dots span{border:1.5px solid var(--sheet)}`. */
internal val BenchInkPresetDotBorderDp = 1.5.dp

/** `.inkuse{margin-top:9px}`. */
internal val BenchInkUseTopGapDp = 9.dp

/** `.inkuse{gap:6px}`. */
internal val BenchInkUseGapDp = 6.dp

/** `.inkuse{font-size:11px}`, `--ink-faint`. */
internal val BenchInkUseSp = 11.sp

/** `.inkuse svg{width:13px;height:13px}`. */
internal val BenchInkUseGlyphDp = 13.dp

/** `.inkuse svg{stroke-width:1.7}`. */
internal val BenchInkUseGlyphStrokeDp = 1.7.dp

/**
 * The document's colour type and Compose's, both ways. `ColorRgba` is four 0..255 ints
 * (`Document.kt:125`) and carries no palette identity, which is why [OD-24](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-028-ruling)
 * needed no migration: every frozen swatch was already storable, and every already-stored ink still
 * renders. The round trip is exact — 8 bits in, 8 bits out — so a swatch that was applied is a swatch
 * that compares equal when the popover reopens.
 */
internal fun Color.toColorRgba(): ColorRgba = ColorRgba(
    r = (red * 255f).roundToInt(),
    g = (green * 255f).roundToInt(),
    b = (blue * 255f).roundToInt(),
    a = (alpha * 255f).roundToInt(),
)

/** @see toColorRgba */
internal fun ColorRgba.toComposeColor(): Color = Color(r, g, b, a)

/**
 * Distinct inks in the whole zine, for the `.inkuse` note — **live**, where the prototype hard-codes 2
 * because it has no document to count.
 *
 * Counted across every page rather than the open one: *"print cheapest"* is a per-**zine** cost, because
 * a riso or a copy shop charges by the inks on the job, not by the inks on one panel. A text element's
 * ink is its `TextStyle.color`; photos are not spot inks and are not counted.
 */
internal fun benchInkCount(pages: List<Page>): Int =
    pages.flatMap { it.elements }.filterIsInstance<TextElement>().map { it.style.color }.distinct().size
