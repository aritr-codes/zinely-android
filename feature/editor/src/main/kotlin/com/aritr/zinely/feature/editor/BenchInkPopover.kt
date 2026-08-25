package com.aritr.zinely.feature.editor

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
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
import com.aritr.zinely.ui.components.zinelyV21HardShadow
import com.aritr.zinely.ui.theme.ZinelyContentInks
import com.aritr.zinely.ui.theme.ZinelyHaptic
import com.aritr.zinely.ui.theme.ZinelyMakerInkId
import com.aritr.zinely.ui.theme.ZinelyNeutralId
import com.aritr.zinely.ui.theme.ZinelyPaperTintId
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
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
internal data class BenchInkSwatch(
    val name: String,
    val value: Color,
    val spokenName: String = name,
)

/** One labelled band of the popover — an `.inklbl` over a `.pots` row (`v21-bench.html:705-713`). */
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
 * **A decor element is offered all three bands, and that is a ruling, not an oversight.**
 * [SUPPLIES-SPEC §0 O-A](../../../../../../../../docs/design/SUPPLIES-SPEC.md#o-a--decor-tints-from-the-content-palette--all-three-bands)
 * states and rejects the objection that fences text: *"a paper tint laid as a mark reads as nothing — is
 * true and is the maker's call."* The two targets differ in what is at stake, not in the palette's honesty:
 * a title nobody can read is a failure of the page, a supply nobody can see is a pale-on-pale riso result
 * undone in one tap. The reason text is fenced is legibility (§II.9 is satisfied for decor by the
 * single-coverage rule instead), so the fence does not generalise from one to the other.
 *
 * Pure, so the *sets* are asserted directly rather than through a composition — and asserted as
 * band-label order plus per-band swatch order, because a permutation satisfies "every colour is present"
 * and is still the wrong palette.
 */
internal fun benchInkBands(inks: ZinelyContentInks, kind: BenchVerbKind): List<BenchInkBand> {
    val band1 = BenchInkBand(
        Copy.BenchInk.INKS,
        inks.makerInks.map {
            val name = benchInkName(it.id)
            BenchInkSwatch(
                name = name,
                value = it.value,
                spokenName = if (it.id == ZinelyMakerInkId.Ink) Copy.BenchInk.SPOT_INK_SPOKEN else name,
            )
        },
    )
    val tints = BenchInkBand(
        Copy.BenchInk.PAPER_TINTS,
        inks.paperTints.map { BenchInkSwatch(benchInkName(it.id), it.value) },
    )
    val band3 = BenchInkBand(
        Copy.BenchInk.NEUTRALS,
        inks.neutrals.map {
            val name = benchInkName(it.id)
            BenchInkSwatch(
                name = name,
                value = it.value,
                spokenName = if (it.id == ZinelyNeutralId.Ink) Copy.BenchInk.NEUTRAL_INK_SPOKEN else name,
            )
        },
    )
    return when (kind) {
        BenchVerbKind.TEXT -> listOf(band1, band3)
        // Not `else`, and the reason held: a target that takes paper arrived (decor, once `Ink` went live)
        // and the grant was made deliberately, by §0 O-A, rather than inherited from this line.
        //
        // ⚠ `PHOTO` is the unreachable half now, not `DECOR`. `BenchContextBar.kt:151` ships no photo
        // `Ink` verb at all — Reframe / Copier / Replace / Delete — so decor is the sole consumer of
        // `tints`. Against the only paper a page can carry (nothing constructs `Background.Solid`), those
        // five run 1.03:1–1.28:1. O-A accepted exactly that; it is recorded here so the next reader meets
        // the number beside the ruling rather than discovering it as a surprise.
        BenchVerbKind.PHOTO, BenchVerbKind.DECOR -> listOf(band1, tints, band3)
    }
}

/**
 * The three frozen recipes (`PRESETS`, `v21-bench.html:714-715`), resolved against the typed palette rather
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
 * The frozen ink popover — `.inkpop` (`v21-bench.html:236-261`, behaviour `:693-720`),
 * [ADR-096](../../../../../../../../docs/DECISIONS.md#adr-096), re-skinned to V2.1 by
 * [ADR-102](../../../../../../../../docs/DECISIONS.md#adr-102) package P4.
 *
 * ### What P4 changed here
 *
 * The card's ground moved from `--sheet` to `--paper` and its soft `0 14px 34px -12px` shadow became the
 * language's **printed** one — `var(--hard) var(--hard) 0 var(--ink-line)`, a 4dp zero-blur offset copy of
 * its own outline. It is a *resting* depth with no press: `.inkpop` is a surface, not a control.
 *
 * The pots grew from 26dp to 30dp and lost two rings and a gesture. V2's translucent white inner border and
 * `--desk-edge` halo are both gone — a pot is now a plain circle outlined in ink — and `.pot` declares no
 * `:active`, so the `scale(.9)` press is gone with them. What replaces the selection ring is not a colour
 * change but a **dashed** one: `1.6px dashed var(--ink)` at `inset:-5px`, the same tear-line mark the
 * canvas draws around a selected element (`v21-bench.html:195`). The language stopped saying *chosen* with
 * a hue.
 *
 * `Done` became a `leaf` pill and the band labels moved from `--ink-faint` to `--ink-soft`.
 *
 * ### Deviation: the ink-economy note is kept, and V2.1 does not draw it
 *
 * `openInk()` emits four bands and stops (`v21-bench.html:709-716`); the `.inkuse` shield-and-count line V2
 * closed the card with is **absent from the freeze**. It is retained here rather than deleted, because it
 * is the only place the product tells a maker what a second ink costs to print and removing it is a
 * *product* decision rather than a re-skin. Its geometry is therefore no longer frozen anywhere — the
 * numbers below are V2's, carried forward, and flagged as such. Owner call.
 *
 * ### Two more places the freeze and the shipped copy disagree
 *
 * The frozen labels read *"Spot inks"* and *"Ready-made pairs"* where `Copy.BenchInk` ships *"Inks"* and
 * *"Ready-made palettes"*. `:core:copy` is outside P4's blast radius, so the strings are left alone and the
 * divergence is recorded here rather than silently resolved in either direction.
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
 * `.pot` is 30dp on a 38dp pitch, in a row that **wraps** — so this is
 * [D-009](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-009--no-control-in-the-frozen-trilogy-declares-a-minimum-touch-target-and-most-measure-well-under-48dp)'s
 * overlap case in *two* dimensions, where C5's filmstrip met it in one. D-009 rules *extend the target,
 * keep the paint*, and Compose's own pointer-input minimum does exactly that: a `selectable` node under
 * 48dp still reports 48dp of `touchBoundsInRoot`. No `minimumInteractiveComponentSize()` — it grows the
 * *layout* slot and would move the frozen 8dp gaps. Where expanded targets meet, the nearer centre wins;
 * that is inherent to a 30dp control on a 38dp pitch and is the honest reading of D-009 here.
 *
 * @param visible drives the frozen enter: a 10dp rise and a fade over `.18s` (`v21-bench.html:240`).
 *   Collapses to 0ms under reduced motion ([ADR-075](../../../../../../../../docs/DECISIONS.md#adr-075)).
 * @param bands the frozen bands for this ink target — [benchInkBands].
 * @param presets the three recipes — [benchInkPresets].
 * @param selected the element's current ink, or `null` when it matches no swatch in any offered band.
 * @param inkCount distinct inks in the whole zine, for the `.inkuse` note. Live, never a constant.
 * @param onDockedTopChanged the card's settled top edge in window coordinates, for the same clearance term
 *   the editing row feeds ([benchEditPanMagnitudeDp], D-043 / OD-16). F-5: a maker was being asked to pick
 *   ink for type this card was covering. ⚠ Nothing is reported while hidden — `AnimatedVisibility` composes
 *   no layout at all in that state — so the last value goes **stale** the moment the card leaves. The
 *   consumer must therefore gate on its own open flag rather than trust this edge, and does.
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
    onDockedTopChanged: (Float) -> Unit = {},
) {
    val colors = ZinelyTheme.v21Colors
    val motion = if (ZinelyTheme.motion.reduceMotion) 0 else BenchInkPopoverEnterMillis
    val spec = tween<Float>(motion, easing = ZinelyV2Standard)
    val enterPx = with(LocalDensity.current) { BenchInkPopoverEnterOffsetDp.roundToPx() }
    AnimatedVisibility(
        visible = visible,
        // F-5: this card occludes the page exactly as the editing row does, so it owes the page the same
        // clearance the row already pays ([benchEditPanMagnitudeDp], D-043 / OD-16). Read on the OUTER
        // node, above `AnimatedVisibility`'s own slide, for the reason `BenchStyleRow` records: measured
        // inside the animation the pan would chase the card's entrance instead of its docked position.
        modifier = modifier.onGloballyPositioned { onDockedTopChanged(it.positionInWindow().y) },
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
                // The card swallows taps that miss a pot — the gaps between 30dp circles are holes,
                // and since OD-13 a tap that reaches the canvas deselects, which would take the popover
                // and the selection away together. C2b measured that failure on device; the same 8dp
                // gaps exist here, more of them.
                .pointerInput(Unit) { detectTapGestures { } }
                // `box-shadow:var(--hard) var(--hard) 0 var(--ink-line)` — a resting printed shadow, and
                // ⚠ nothing that clips may sit to its LEFT: it paints 4dp outside the card's own bounds.
                // No press tier: `.inkpop` declares no `:active` because a surface is not a control.
                .zinelyV21HardShadow(BenchInkPopoverShadowDp, colors.inkLine, shape)
                .background(colors.surface, shape)
                .border(BenchInkPopoverBorderDp, colors.ink, shape)
                .padding(BenchInkPopoverPadding),
        ) {
            // `.inkpop h4{margin:0 0 var(--gap-hair);display:flex;justify-content:space-between;
            // align-items:center}` (`v21-bench.html:242-243`).
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
                    fontFamily = ZinelyV21Fonts.Voice,
                    // `.inkpop h4{font-weight:700}` — a real Bold, where V2 asked for 500.
                    fontWeight = FontWeight.Bold,
                    fontSize = BenchInkTitleSp,
                    lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                    color = colors.ink,
                )
                BenchInkDoneChip(onDone)
            }

            // The ring belongs to ONE swatch, and colour alone cannot identify one: `Ink #2A251E` is a
            // member of both the Inks band and the Neutrals band (`ZinelyContentInks.kt:219`, `:231` —
            // the freeze repeats it verbatim in both `INKS` and `NEUT`), and for a text target both bands
            // are drawn. Ringing by `selected == swatch.value` therefore ringed the element's ink TWICE
            // and published `Selected` on two RadioButton nodes in one group. The freeze rings exactly
            // one (`v21-bench.html:722` clears every `.on` before setting one), and the OD-24
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
                // No bottom margin on the band itself in V2.1: `.pots` declares none and `.inklbl` owns
                // the whole rhythm through its own `margin:var(--gap-md) 0 var(--gap-xs)`. Carrying a
                // band gap as well would double every space between a row of pots and the next label.
                Column {
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

            // The fourth band. `openInk` gives it the same `.inklbl` the other three get, so it is a band
            // like the others — ADR-089 row 6.3 said "three bands" and the freeze emits four (ADR-096 §2).
            Column {
                BenchInkBandLabel(Copy.BenchInk.PRESETS)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(BenchInkPresetGapDp),
                    verticalArrangement = Arrangement.spacedBy(BenchInkPresetGapDp),
                ) {
                    for (preset in presets) {
                        BenchInkPresetPill(preset = preset, onClick = { onPreset(preset) })
                    }
                }
            }

            // `.inkuse` — the ink-economy note, with a live count.
            //
            // ⚠ **Not in the V2.1 freeze.** `openInk()` stops after the fourth band; this line is V2's,
            // kept because it is the only place the product prices a second ink, and its numbers below are
            // V2's carried forward rather than transcribed. See the class KDoc.
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
                BenchInkShield(tint = colors.inkSoft)
                Text(
                    text = Copy.BenchInk.useNote(inkCount),
                    fontFamily = ZinelyV21Fonts.Work,
                    fontSize = BenchInkUseSp,
                    lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                    color = colors.inkSoft,
                )
            }
        }
    }
}

/**
 * Frozen `.inkpop h4 button` (`v21-bench.html:244-246`) — a `leaf` pill under `onLeaf`, outlined in ink,
 * standing on a 2dp printed shadow. V2 drew this as bare `--ink-faint` text with no ground at all.
 *
 * ⚠ **It rests at 2dp and does not press**, because `.inkpop h4 button` declares no `:active` — the same
 * omission P3 found on `.doneEdit` and amended the frozen file to fix (`v21-bench.html:285-292`). This one
 * is transcribed as written rather than amended by analogy: changing it is a freeze edit, and a freeze edit
 * is the owner's, not P4's. Recorded so the resemblance is not mistaken for an oversight here.
 */
@Composable
private fun BenchInkDoneChip(onDone: () -> Unit) {
    val colors = ZinelyTheme.v21Colors
    val done = benchTap(action = onDone)
    Box(
        modifier = Modifier
            // ⚠ The resting shadow paints 2dp outside the node — nothing that clips may sit to its left.
            .zinelyV21HardShadow(BenchInkDoneShadowDp, colors.inkLine, BenchInkDoneShape)
            .clip(BenchInkDoneShape)
            .background(colors.leaf)
            .border(BenchInkPopoverBorderDp, colors.ink, BenchInkDoneShape)
            .testTag(BenchInkDoneTestTag)
            .clickable(role = Role.Button, onClick = done)
            .padding(horizontal = BenchInkDonePaddingH, vertical = BenchInkDonePaddingV),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = Copy.BenchInk.DONE,
            fontFamily = ZinelyV21Fonts.Work,
            fontSize = BenchInkDoneSp,
            // `.inkpop h4 button{font-weight:700}`.
            fontWeight = FontWeight.Bold,
            lineHeight = ZinelyV21Fonts.InheritedLineHeight,
            color = colors.onLeaf,
        )
    }
}

/** `.inklbl` — 9.6sp/700, `.13em` tracking, uppercase, `--ink-soft`, 12dp above itself and 4dp below. */
@Composable
private fun BenchInkBandLabel(label: String) {
    Text(
        text = label.uppercase(),
        fontFamily = ZinelyV21Fonts.Work,
        fontSize = BenchInkBandLabelSp,
        // `.inklbl{font-weight:700}` — V2 asked for 600.
        fontWeight = FontWeight.Bold,
        letterSpacing = BenchInkBandLabelTracking,
        lineHeight = ZinelyV21Fonts.InheritedLineHeight,
        color = ZinelyTheme.v21Colors.inkSoft,
        modifier = Modifier
            .padding(top = BenchInkBandLabelTopGapDp, bottom = BenchInkBandLabelGapDp)
            .testTag(BenchInkBandLabelTestTag),
    )
}

/**
 * One `.pot` (`v21-bench.html:250-253`) — a 30dp circle of ink, outlined at the language's 1.5dp pen, and
 * **one** ring rather than V2's three.
 *
 * V2's translucent white inner border and its 1dp `--desk-edge` halo are both gone; so is `scale(.9)`,
 * because `.pot` declares no `:active`. The chosen mark is `1.6px dashed var(--ink)` at `inset:-5px` — a
 * *dashed* ring, the same tear-line the canvas draws around a selected element, and the reason it can be
 * ink rather than a hue: a dash reads as chosen without competing with the colour it is ringing, which is
 * the whole difficulty of putting a selection mark on a swatch.
 *
 * It is drawn rather than bordered because Compose's `border` is always inside the layout bounds, and
 * growing the box to hold a ring at `inset:-5px` would move the frozen 8dp gaps.
 */
@Composable
private fun BenchInkSwatchDot(swatch: BenchInkSwatch, selected: Boolean, onClick: () -> Unit) {
    val colors = ZinelyTheme.v21Colors
    // Snap — a pot is a selection, and it is the same choice the preset pill makes.
    val pick = benchTap(ZinelyHaptic.Snap, onClick)
    Box(
        modifier = Modifier
            .size(BenchInkSwatchSizeDp)
            // Outside the clip below, deliberately: the chosen ring sits 5dp beyond the pot's own bounds.
            .drawBehind {
                if (!selected) return@drawBehind
                // `.pot.on::after{inset:-5px;border:1.6px dashed var(--ink);border-radius:pill}`
                val half = size.minDimension / 2f
                val ring = BenchInkSelRingDp.toPx()
                val radius = half + BenchInkSelInsetDp.toPx() + ring / 2f
                val dash = BenchInkSelDashDp.toPx()
                drawCircle(
                    color = colors.ink,
                    radius = radius,
                    style = Stroke(
                        width = ring,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
                    ),
                )
            }
            .clip(CircleShape)
            .background(swatch.value, CircleShape)
            // `.pot{border:1.5px solid var(--ink)}` — real ink now, where V2 drew translucent white. It is
            // what separates the pot from the card's `paper` ground whatever the fill becomes.
            .border(BenchInkSwatchBorderDp, colors.ink, CircleShape)
            .selectable(
                selected = selected,
                // A single-choice group of circles IS a radio group, and that is what the platform has
                // a state for. C5 learned that Compose routes `Selected` to `isChecked` for every role
                // but `Role.Tab`; a radio button is the one role for which "selected / not selected" is
                // Android's own wording rather than a wrong one borrowed from a checkbox.
                role = Role.RadioButton,
                onClick = pick,
            )
            .testTag(BenchInkSwatchTestTag)
            .semantics { contentDescription = Copy.BenchInk.swatchLabel(swatch.spokenName) },
    )
}

/**
 * One `.preset` pill (`v21-bench.html:255-261`): the overlapping dots, then the recipe's name. The dots are
 * decorative — three circles are not speakable — so the pill publishes one node whose name carries the
 * recipe **and its primary ink**, which under OD-24 is the colour the tap applies.
 *
 * It gains its own `paper` ground, an ink outline and a 2dp printed shadow, where V2 drew a bare
 * `--chrome-line` outline over the card. Like `.inkpop h4 button` above it declares no `:active`, so the
 * shadow rests and does not shorten — transcribed, not amended.
 */
@Composable
private fun BenchInkPresetPill(preset: BenchInkPreset, onClick: () -> Unit) {
    val colors = ZinelyTheme.v21Colors
    // Snap — a recipe is chosen, not fired.
    val pick = benchTap(ZinelyHaptic.Snap, onClick)
    Row(
        modifier = Modifier
            // ⚠ Resting shadow, 2dp outside the node — nothing that clips may sit to its left.
            .zinelyV21HardShadow(BenchInkPresetShadowDp, colors.inkLine, BenchInkPresetShape)
            .clip(BenchInkPresetShape)
            .background(colors.surface)
            .border(BenchInkPopoverBorderDp, colors.ink, BenchInkPresetShape)
            .clickable(role = Role.Button, onClick = pick)
            .padding(BenchInkPresetPadding)
            .testTag(BenchInkPresetTestTag)
            .clearAndSetSemantics {
                contentDescription = Copy.BenchInk.presetLabel(preset.name, preset.applied.name)
                role = Role.Button
                // Declared, though **measured to be redundant** — and recorded as such rather than
                // repeated as folklore. A `clearAndSetSemantics` that sits *below* a `clickable` was
                // assumed here (and by the review that asked for this line) to wipe the platform's
                // ACTION_CLICK, the way [BenchContextBar]'s note describes. It does not: with this line
                // deleted, `BenchInkPresetPlatformA11yTest` still reads `isClickable = true` off the real
                // `AccessibilityNodeInfo`, and only deleting the `clickable` itself turns it red. The line
                // stays because it pins what the action *is* if the chain is ever reordered — not because
                // the pill was broken without it. (The two siblings in this file declare none.)
                onClick { pick(); true }
            },
        horizontalArrangement = Arrangement.spacedBy(BenchInkPresetGlyphGapDp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // `.preset i b{width:13px;height:13px;margin-left:-5px;border:1.5px solid var(--ink)}` — the dots
        // overlap leftward now, and their separating ring is ink rather than the card's own ground.
        Row(horizontalArrangement = Arrangement.spacedBy(-BenchInkPresetDotOverlapDp)) {
            for (dot in preset.dots) {
                Box(
                    modifier = Modifier
                        .size(BenchInkPresetDotDp)
                        .background(dot.value, CircleShape)
                        .border(BenchInkPresetDotBorderDp, colors.ink, CircleShape),
                )
            }
        }
        Text(
            text = preset.name,
            fontFamily = ZinelyV21Fonts.Work,
            fontSize = BenchInkPresetSp,
            // `.preset{font-weight:600;color:var(--ink-soft)}`.
            fontWeight = FontWeight.SemiBold,
            lineHeight = ZinelyV21Fonts.InheritedLineHeight,
            color = colors.inkSoft,
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

// — the frozen numbers (`v21-bench.html:236-261`) —

/** `.inkpop{left:12px;right:12px;bottom:12px}` (`:237`) — unchanged from V2. */
internal val BenchInkPopoverInsetDp = 12.dp

/** `.inkpop{border-radius:var(--br-lg)}` (`:238`) — 22, where V2 drew 16. */
internal val BenchInkPopoverRadiusDp = ZinelyV21Dimens.radiusLg

/** `.inkpop`/`.pot`/`.preset`/`.inkpop h4 button{border:1.5px solid var(--ink)}`. V2 drew a 1dp `--chrome-line`. */
internal val BenchInkPopoverBorderDp = 1.5.dp

/**
 * `.inkpop{box-shadow:var(--hard) var(--hard) 0 var(--ink-line)}` (`:239`) — a **resting** printed shadow
 * at [ZinelyV21Dimens.hardShadow], where V2 carried a soft `0 14px 34px -12px`. Not a press tier: the card
 * declares no `:active`.
 */
internal val BenchInkPopoverShadowDp = ZinelyV21Dimens.hardShadow

/**
 * `.inkpop{padding:var(--gap-md) var(--gap-md) var(--gap-lg)}` (`:238`) — CSS shorthand: top 12, sides 12,
 * bottom 16. V2 asked for 12/14/14.
 */
internal val BenchInkPopoverPadding = PaddingValues(
    start = ZinelyV21Dimens.gapMd,
    top = ZinelyV21Dimens.gapMd,
    end = ZinelyV21Dimens.gapMd,
    bottom = ZinelyV21Dimens.gapLg,
)

/**
 * `.inkpop{transition:opacity .18s,transform .18s}` (`:240`). V2 asked for 220ms.
 *
 * The freeze names no easing, so CSS's own `ease` applies. This still runs on `ZinelyV2Standard` — an
 * approximation, kept because it is where the reduced-motion downgrade lives and because a `.18s` opacity
 * ramp is below the threshold at which the two curves are distinguishable. Recorded, not hidden.
 */
internal const val BenchInkPopoverEnterMillis: Int = 180

/** `.inkpop{transform:translateY(10px)}` at rest (`:240`). V2 rose 14. */
internal val BenchInkPopoverEnterOffsetDp = 10.dp

/** `.inkpop h4{margin:0 0 var(--gap-hair)}` (`:242`) — 2, where V2 gapped 8. */
internal val BenchInkHeaderGapDp = ZinelyV21Dimens.gapHair

/** `.inkpop h4{font-size:1.02rem}` (`:242`) — 16.32sp in `--voice` at weight 700, where V2 drew 15/500. */
internal val BenchInkTitleSp = 16.32.sp

/** `.inkpop h4 button{font-size:.78rem}` (`:244`) — 12.48sp, weight 700, `--on-leaf` on `--leaf`. */
internal val BenchInkDoneSp = 12.48.sp

/** `.inkpop h4 button{border-radius:var(--br-pill)}` (`:245`). A percent shape, per `BenchBarShape`'s note. */
internal val BenchInkDoneShape: RoundedCornerShape = RoundedCornerShape(percent = 50)

/** `.inkpop h4 button{padding:var(--gap-xs) var(--gap-md)}` (`:246`). */
internal val BenchInkDonePaddingH = ZinelyV21Dimens.gapMd
internal val BenchInkDonePaddingV = ZinelyV21Dimens.gapXs

/** `.inkpop h4 button{box-shadow:2px 2px 0 var(--ink-line)}` (`:246`) — a rest with no `:active`; see [BenchInkDoneChip]. */
internal val BenchInkDoneShadowDp = 2.dp

/** `.inklbl{font-size:.6rem}` (`:247`) — 9.6sp, weight 700, uppercase, `--ink-soft`. V2 drew 10sp/600 faint. */
internal val BenchInkBandLabelSp = 9.6.sp

/** `.inklbl{letter-spacing:.13em}` (`:247`). V2 tracked .12em. */
internal val BenchInkBandLabelTracking = 0.13.em

/**
 * `.inklbl{margin:var(--gap-md) 0 var(--gap-xs)}` (`:248`) — 12 above, 4 below.
 *
 * The label owns the whole rhythm in V2.1. V2 split it between a `.band{margin-bottom:9px}` and a 5dp label
 * gap; the band's margin is gone, so carrying one here would double every space in the card.
 */
internal val BenchInkBandLabelTopGapDp = ZinelyV21Dimens.gapMd
internal val BenchInkBandLabelGapDp = ZinelyV21Dimens.gapXs

/** `.pots{gap:var(--gap-sm)}` (`:249`) — 8, where V2 gapped 7. */
internal val BenchInkSwatchGapDp = ZinelyV21Dimens.gapSm

/** `.pot{width:30px;height:30px}` (`:250`), border-box. V2 drew 26. */
internal val BenchInkSwatchSizeDp = 30.dp

/** `.pot{border:1.5px solid var(--ink)}` (`:250`) — inside the 30dp box. V2 drew 2dp of translucent white. */
internal val BenchInkSwatchBorderDp = 1.5.dp

/** `.pot.on::after{inset:-5px}` (`:252`) — unchanged from V2's chosen ring. */
internal val BenchInkSelInsetDp = 5.dp

/** `.pot.on::after{border:1.6px dashed var(--ink)}` (`:252`). V2 drew 1.5px **solid** `--matcha`. */
internal val BenchInkSelRingDp = 1.6.dp

/**
 * The dash rhythm of the chosen ring.
 *
 * CSS derives it from the border width and renders roughly 3-on/3-off at this weight; that is what this
 * transcribes, and it is **recorded rather than presented as frozen** — the same approximation
 * `BenchStyleRow`'s perforation and `ZineShelf`'s placeholder both record.
 */
internal val BenchInkSelDashDp = 3.dp

/** `.presets{gap:var(--gap-sm)}` (`:254`) — 8, unchanged from V2. */
internal val BenchInkPresetGapDp = ZinelyV21Dimens.gapSm

/** `.preset{border-radius:var(--br-pill)}` (`:256`), where V2 drew a 20dp radius. */
internal val BenchInkPresetShape: RoundedCornerShape = RoundedCornerShape(percent = 50)

/**
 * `.preset{padding:var(--gap-xs) var(--gap-md) var(--gap-xs) var(--gap-xs)}` (`:256`) — top 4, right 12,
 * bottom 4, left 4. The left is tight because the dots start there; the right holds the name off the edge.
 */
internal val BenchInkPresetPadding = PaddingValues(
    start = ZinelyV21Dimens.gapXs,
    top = ZinelyV21Dimens.gapXs,
    end = ZinelyV21Dimens.gapMd,
    bottom = ZinelyV21Dimens.gapXs,
)

/** `.preset{gap:var(--gap-sm)}` (`:255`) — 8 between the dots and the name, where V2 gapped 6. */
internal val BenchInkPresetGlyphGapDp = ZinelyV21Dimens.gapSm

/** `.preset{font-size:.72rem}` (`:256`) — 11.52sp at weight 600 in `--ink-soft`. V2 drew 11.5sp in `--ink`. */
internal val BenchInkPresetSp = 11.52.sp

/** `.preset{box-shadow:2px 2px 0 var(--ink-line)}` (`:257`) — a rest with no `:active`; see [BenchInkPresetPill]. */
internal val BenchInkPresetShadowDp = 2.dp

/** `.preset i b{width:13px;height:13px}` (`:259`). V2 drew 12. */
internal val BenchInkPresetDotDp = 13.dp

/** `.preset i b{margin-left:-5px}` (`:260`) — the dots overlap **leftward** now; V2 overlapped right by 4. */
internal val BenchInkPresetDotOverlapDp = 5.dp

/** `.preset i b{border:1.5px solid var(--ink)}` (`:259`), where V2 separated them with `--sheet`. */
internal val BenchInkPresetDotBorderDp = 1.5.dp

// — `.inkuse`: V2's numbers, carried forward. V2.1 does not draw this line at all — see [BenchInkPopover]. —

/** `.inkuse{margin-top:9px}` (`v2-bench.html:389`). */
internal val BenchInkUseTopGapDp = 9.dp

/** `.inkuse{gap:6px}` (`v2-bench.html:389`). */
internal val BenchInkUseGapDp = 6.dp

/** `.inkuse{font-size:11px}` (`v2-bench.html:389`), re-tinted to `--ink-soft`: V2.1 has no `--ink-faint` text. */
internal val BenchInkUseSp = 11.sp

/** `.inkuse svg{width:13px;height:13px}` (`v2-bench.html:390`). */
internal val BenchInkUseGlyphDp = 13.dp

/** `.inkuse svg{stroke-width:1.7}` (`v2-bench.html:390`). */
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
 * a riso or a copy shop charges by the inks on the job, not by the inks on one panel.
 *
 * **What counts as a spot ink**, exhaustively over [com.aritr.zinely.core.model.Element]:
 *  - a [TextElement]'s `style.color` — the maker chose it from the ink popover;
 *  - a [com.aritr.zinely.core.model.DecorElement]'s `ink` — SUPPLIES-SPEC §2 makes a supply an outline
 *    laid down in **one** colour *precisely so* it is a spot ink, and `SceneRenderer` draws it as one;
 *  - an `ImageElement` — **not** counted. A photo is continuous tone, not a spot colour; that is the
 *    original exclusion and it stands.
 *
 * ⚠ The decor arm arrived with ADR-105 S7 (independent review). Before it, black text plus three berry
 * stars reported *"1 ink"* while the print shop charged for two — an under-report that shipped harmless
 * only because no supply could be placed. Written as a `when` rather than a filter so the fourth element
 * kind has to answer the question instead of inheriting `false`.
 */
internal fun benchInkCount(pages: List<Page>): Int =
    pages.asSequence()
        .flatMap { it.elements.asSequence() }
        .mapNotNull { element ->
            when (element) {
                is TextElement -> element.style.color
                is com.aritr.zinely.core.model.DecorElement -> element.ink
                is com.aritr.zinely.core.model.ImageElement -> null
            }
        }
        .distinct()
        .count()
