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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.ink.InkHex
import com.aritr.zinely.core.editor.ink.InkLibrary
import com.aritr.zinely.core.editor.ink.InkLibraryEdit
import com.aritr.zinely.core.editor.ink.InkPalette
import com.aritr.zinely.core.editor.ink.MyInk
import com.aritr.zinely.core.editor.ink.colourName
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
import java.io.IOException
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

internal const val BenchInkPopoverTestTag: String = "bench-ink-popover"
/** A built-in ink choice (maker inks, the three neutrals, and for Art the five tints). */
internal const val BenchInkSwatchTestTag: String = "bench-ink-swatch"
internal const val BenchInkBandLabelTestTag: String = "bench-ink-band-label"
internal const val BenchInkDoneTestTag: String = "bench-ink-done"
internal const val BenchInkCurrentTestTag: String = "bench-ink-current"
internal const val BenchInkTitleTestTag: String = "bench-ink-title"
internal const val BenchInkPotTestTag: String = "bench-ink-pot"
/** A kept ink in My inks. */
internal const val BenchInkKeptTestTag: String = "bench-ink-kept"
internal const val BenchInkMixTestTag: String = "bench-ink-mix"
internal const val BenchInkNewPaletteTestTag: String = "bench-ink-new-palette"
internal const val BenchInkPalettesEmptyTestTag: String = "bench-ink-palettes-empty"
/** The open palette's card (`.pal`). */
internal const val BenchInkPaletteTestTag: String = "bench-ink-palette"
internal const val BenchInkPalettePotTestTag: String = "bench-ink-palette-pot"
internal const val BenchInkPaletteMoreTestTag: String = "bench-ink-palette-more"
/** One palette strip (`.pal-mini`), shown when two or more palettes exist. */
internal const val BenchInkStripTestTag: String = "bench-ink-strip"
internal const val BenchInkTintSeparatorTestTag: String = "bench-ink-tint-sep"
/** The Starter palettes disclosure row (`.disclose`). */
internal const val BenchInkStartersTestTag: String = "bench-ink-starters"
/** One starter palette's card. */
internal const val BenchInkStarterTestTag: String = "bench-ink-starter"
internal const val BenchInkStarterPotTestTag: String = "bench-ink-starter-pot"
internal const val BenchInkSaveCopyTestTag: String = "bench-ink-save-copy"

/**
 * One built-in ink: the colour and the name the maker hears. [tint] marks the five paper tints, which the
 * tray draws after a dashed separator and offers to Art only (§0 O-A).
 */
internal data class BenchInkSwatch(val name: String, val value: Color, val tint: Boolean = false)

/** One starter palette (prototype `STARTERS`): a name and its inks, every one of which applies itself. */
internal data class BenchStarterPalette(val name: String, val inks: List<BenchInkSwatch>)

/**
 * Built-in inks, one list (prototype `BUILT`, A40 rev. 2): the ten maker inks, then `Slate`, `Stone`, `Fog`
 * — the frozen Spot inks then Neutrals with the duplicate `Ink` shown **once**, so D-083's two spoken names
 * for one pigment are gone. For a target that takes paper (Art) the five tints follow (prototype `builtFor`);
 * a text target is not offered them (OD-24's fence, which A40 keeps).
 *
 * `PHOTO` is unreachable (no photo `Ink` verb) and grouped with `DECOR` so the `when` stays exhaustive.
 */
internal fun benchBuiltInInks(inks: ZinelyContentInks, kind: BenchVerbKind): List<BenchInkSwatch> {
    val built = inks.makerInks.map { BenchInkSwatch(benchInkName(it.id), it.value) } +
        inks.neutrals.filter { it.id != ZinelyNeutralId.Ink }.map { BenchInkSwatch(benchInkName(it.id), it.value) }
    return when (kind) {
        BenchVerbKind.TEXT -> built
        BenchVerbKind.PHOTO, BenchVerbKind.DECOR ->
            built + inks.paperTints.map { BenchInkSwatch(benchInkName(it.id), it.value, tint = true) }
    }
}

/**
 * The three frozen recipes (prototype `STARTERS`), resolved against the typed palette rather than re-typed
 * as hexes, so a recipe cannot drift from the inks it is made of. Two end in a paper tint by value.
 */
internal fun benchStarterPalettes(inks: ZinelyContentInks): List<BenchStarterPalette> = listOf(
    BenchStarterPalette(
        Copy.BenchInk.PRESET_TWO_COLOUR,
        listOf(inks.swatch(ZinelyMakerInkId.Ink), inks.swatch(ZinelyMakerInkId.Strawberry)),
    ),
    BenchStarterPalette(
        Copy.BenchInk.PRESET_WARM,
        listOf(
            inks.swatch(ZinelyMakerInkId.Brick),
            inks.swatch(ZinelyMakerInkId.Sunflower),
            inks.swatch(ZinelyPaperTintId.Cream),
        ),
    ),
    BenchStarterPalette(
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

/** Maker ink display names. A `when` rather than a map so a new ink is a compile error, not a blank chip. */
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

/** Paper tint display names. Offered to Art only — see [benchBuiltInInks]. */
internal fun benchInkName(id: ZinelyPaperTintId): String = when (id) {
    ZinelyPaperTintId.Cream -> Copy.BenchInk.CREAM
    ZinelyPaperTintId.Blush -> Copy.BenchInk.BLUSH
    ZinelyPaperTintId.Sky -> Copy.BenchInk.SKY
    ZinelyPaperTintId.Sage -> Copy.BenchInk.SAGE
    ZinelyPaperTintId.Kraft -> Copy.BenchInk.KRAFT
}

/** Neutral display names. `Ink` repeats the maker ink's, verbatim from the frozen source. */
internal fun benchInkName(id: ZinelyNeutralId): String = when (id) {
    ZinelyNeutralId.Ink -> Copy.BenchInk.INK
    ZinelyNeutralId.Slate -> Copy.BenchInk.SLATE
    ZinelyNeutralId.Stone -> Copy.BenchInk.STONE
    ZinelyNeutralId.Fog -> Copy.BenchInk.FOG
}

/**
 * The Ink tray, A40 rev. 2 (`v21-ink-mixer.html` `renderTray`, [ADR-138](../../../../../../../../docs/DECISIONS.md#adr-138)):
 * the maker's own work first, the built-in inks compact below.
 *
 * Top to bottom: the header (`Ink` + `Done`, outside the scroll), Current ink, **My palettes** (the open
 * palette's card, then a strip per palette once there are two), **My inks** (kept inks, then Mix),
 * **Built-in inks** (one compact grid, no visible names), and **Starter palettes** behind a disclosure that
 * starts closed. My palettes and My inks are drawn only once [inkLibrary] has loaded (non-null), so nothing
 * loaded late is shown as empty and then overwritten.
 *
 * Every pot applies its **own** ink through [onPick] — a palette's, a kept one, a built-in, a starter's.
 * OD-24's "a preset applies its first colour" is retired (ADR-138 Amends). The host owns what a pick does
 * (one entry point, `applyInk`), so this composable never writes the library itself except to remember
 * which palette is open ([InkLibrary.open], through [onEditInkLibrary]).
 *
 * ### The selection ring reads the document, not the last tap
 *
 * [selected] is the element's **own** colour, so the ring survives undo, a page change and a reselect.
 * One ring per **group**: the pot whose hex equals the piece's ink, in every group where it occurs (the
 * open palette, My inks, built-in, a starter). Within a group there is at most one, by construction
 * (palettes and My inks hold distinct hexes, and built-in `Ink` is listed once).
 *
 * The card's paint (paper ground, printed 4dp shadow, 1.5dp ink outline, dashed chosen ring) is P4's and
 * unchanged; see [BenchInkSwatchDot] and [BenchInkDoneChip].
 *
 * @param visible drives the frozen enter: a 10dp rise and a fade over `.18s`. 0ms under reduced motion.
 * @param kind the ink target; chooses the built-in list ([benchBuiltInInks]).
 * @param selected the element's current ink.
 * @param onPick a pot was tapped; the colour is the pot's own.
 * @param onDockedTopChanged the card's settled top edge in window coordinates (F-5 clearance). ⚠ Nothing is
 *   reported while hidden, so the consumer gates on its own open flag.
 * @param inkLibrary My inks and My palettes, or `null` while loading.
 * @param onEditInkLibrary used here only to persist which palette strip is open. A store failure
 *   (`IOException`) leaves the strip where it was; the tray never crashes on storage.
 * @param onMix the Mix pot (the mixer is hosted in INK task 8b).
 * @param onNewPalette `+ New palette` (edit mode is INK task 7).
 * @param onPaletteMore the open palette's ⋯ (its menu is INK task 7).
 * @param onSaveStarterCopy a starter's `Save a copy` (INK task 7).
 */
@Composable
internal fun BenchInkPopover(
    visible: Boolean,
    kind: BenchVerbKind,
    selected: Color?,
    onPick: (Color) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    onDockedTopChanged: (Float) -> Unit = {},
    inkLibrary: InkLibrary? = InkLibrary.EMPTY,
    onEditInkLibrary: suspend ((InkLibrary) -> InkLibrary) -> InkLibraryEdit =
        { InkLibraryEdit(InkLibrary.EMPTY, InkLibrary.EMPTY) },
    onMix: () -> Unit = {},
    onNewPalette: () -> Unit = {},
    onPaletteMore: (InkPalette) -> Unit = {},
    onSaveStarterCopy: (BenchStarterPalette) -> Unit = {},
) {
    val colors = ZinelyTheme.v21Colors
    val motion = if (ZinelyTheme.motion.reduceMotion) 0 else BenchInkPopoverEnterMillis
    val spec = tween<Float>(motion, easing = ZinelyV2Standard)
    val enterPx = with(LocalDensity.current) { BenchInkPopoverEnterOffsetDp.roundToPx() }
    AnimatedVisibility(
        visible = visible,
        // F-5: read on the OUTER node, above `AnimatedVisibility`'s own slide, so the pan follows the docked
        // position and not the entrance.
        modifier = modifier.onGloballyPositioned { onDockedTopChanged(it.positionInWindow().y) },
        enter = slideInVertically(tween(motion, easing = ZinelyV2Standard)) { enterPx } + fadeIn(spec),
        exit = slideOutVertically(tween(motion, easing = ZinelyV2Standard)) { enterPx } + fadeOut(spec),
    ) {
        val shape = RoundedCornerShape(BenchInkPopoverRadiusDp)
        Column(
            modifier = Modifier
                .padding(BenchInkPopoverInsetDp)
                // Tagged inside the inset, so the frozen 12dp inset is measurable.
                .testTag(BenchInkPopoverTestTag)
                // The card swallows taps that miss a pot: since OD-13 a tap that reaches the canvas deselects,
                // which would take the tray and the selection away together.
                .pointerInput(Unit) { detectTapGestures { } }
                // A resting printed shadow; ⚠ it paints 4dp outside the card, so nothing that clips may sit
                // to its left.
                .zinelyV21HardShadow(BenchInkPopoverShadowDp, colors.inkLine, shape)
                .background(colors.surface, shape)
                .border(BenchInkPopoverBorderDp, colors.ink, shape)
                // The bottom padding lives inside the scrolling body, so the last row scrolls clear of the edge.
                .padding(start = ZinelyV21Dimens.gapMd, top = ZinelyV21Dimens.gapMd, end = ZinelyV21Dimens.gapMd),
        ) {
            // `fillMaxWidth` is what makes `SpaceBetween` mean anything (Device Pass 1: "InkDone").
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
                    fontWeight = FontWeight.Bold,
                    fontSize = BenchInkTitleSp,
                    lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                    color = colors.ink,
                    modifier = Modifier.testTag(BenchInkTitleTestTag),
                )
                BenchInkDoneChip(onDone)
            }

            // ⚠ Everything BELOW the header scrolls; the header does not (a tester's 891dp phone cut `Ink`
            // and `Done` in half when the whole card scrolled). `weight(fill = false)` keeps a tray that fits
            // at its own height, and needs a bounded host — the canvas Box and every test host are.
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = ZinelyV21Dimens.gapMd),
            ) {
                BenchInkTrayBody(
                    kind = kind,
                    selected = selected,
                    onPick = onPick,
                    inkLibrary = inkLibrary,
                    onEditInkLibrary = onEditInkLibrary,
                    onMix = onMix,
                    onNewPalette = onNewPalette,
                    onPaletteMore = onPaletteMore,
                    onSaveStarterCopy = onSaveStarterCopy,
                )
            }
        }
    }
}

@Composable
private fun BenchInkTrayBody(
    kind: BenchVerbKind,
    selected: Color?,
    onPick: (Color) -> Unit,
    inkLibrary: InkLibrary?,
    onEditInkLibrary: suspend ((InkLibrary) -> InkLibrary) -> InkLibraryEdit,
    onMix: () -> Unit,
    onNewPalette: () -> Unit,
    onPaletteMore: (InkPalette) -> Unit,
    onSaveStarterCopy: (BenchStarterPalette) -> Unit,
) {
    val colors = ZinelyTheme.v21Colors
    val inks = ZinelyTheme.contentInks
    val shown = selected ?: colors.ink
    val current = shown.inkHex()
    val big = LocalDensity.current.fontScale >= BenchInkLargeTextFontScale
    val scope = rememberCoroutineScope()

    BenchInkCurrent(colour = shown, name = inkName(current, inks, kind))

    if (inkLibrary != null) {
        // — My palettes —
        val palettes = inkLibrary.palettes
        BenchInkSectionLabel(
            label = Copy.BenchInk.MY_PALETTES,
            count = palettes.takeIf { it.isNotEmpty() }?.let { Copy.BenchInk.paletteCount(it.size) },
            countInline = true,
        ) { BenchInkNewPaletteAction(onNewPalette) }
        val open = inkLibrary.openPalette()
        if (open == null) {
            BenchInkPalettesEmpty()
        } else {
            BenchInkPaletteCard(
                name = open.name,
                hexes = open.inks,
                current = current,
                spoken = { inkName(it, inks, kind) },
                big = big,
                onPick = onPick,
                tag = BenchInkPaletteTestTag,
                potTag = BenchInkPalettePotTestTag,
            ) { BenchInkPaletteMore(open.name) { onPaletteMore(open) } }
            if (palettes.size >= 2) {
                BenchInkStrips(palettes = palettes, openId = open.id, big = big) { id ->
                    // Persisted, so the last opened stays open. A storage failure leaves the strip where it
                    // was: nothing the maker made is lost, and the tray must not crash on a write.
                    scope.launch {
                        try {
                            onEditInkLibrary { it.open(id) }
                        } catch (_: IOException) {
                        }
                    }
                }
            }
        }

        // — My inks —
        BenchInkSectionLabel(
            label = Copy.BenchInk.MY_INKS,
            count = inkLibrary.myInks.takeIf { it.isNotEmpty() }?.let { Copy.BenchInk.inkCount(it.size) },
        )
        // `null` is the Mix choice, which always closes the grid (prototype `mineHtml`).
        BenchInkGrid(
            items = inkLibrary.myInks + null,
            columns = if (big) BenchInkLargeTextColumns else BenchInkGridColumns,
            modifier = Modifier.selectableGroup(),
        ) { kept, m ->
            if (kept == null) BenchInkMixChoice(onMix, m) else BenchInkKeptChoice(kept, kept.hex == current, onPick, m)
        }
    }

    // — Built-in inks —
    BenchInkSectionLabel(label = Copy.BenchInk.BUILT_IN_INKS)
    val builtIns = benchBuiltInInks(inks, kind)
    val columns = if (big) BenchInkGridColumns else BenchInkCompactColumns
    val compact: @Composable (BenchInkSwatch, Modifier) -> Unit = { swatch, m ->
        BenchInkChoice(
            colour = swatch.value,
            spoken = Copy.BenchInk.swatchLabel(swatch.name),
            selected = swatch.value.inkHex() == current,
            onClick = { onPick(swatch.value) },
            tag = BenchInkSwatchTestTag,
            potSize = BenchInkCompactPotDp,
            minHeight = BenchInkCompactChoiceMinHeightDp,
            modifier = m,
        )
    }
    BenchInkGrid(items = builtIns.filterNot { it.tint }, columns = columns, modifier = Modifier.selectableGroup(), cell = compact)
    val tints = builtIns.filter { it.tint }
    if (tints.isNotEmpty()) {
        // `.tint-sep{border-top:1.5px dashed var(--hair);margin:2px 0}` — decorative, so no semantics.
        BenchInkDashedRule(
            Modifier
                .padding(vertical = ZinelyV21Dimens.gapHair)
                .testTag(BenchInkTintSeparatorTestTag),
        )
        BenchInkGrid(items = tints, columns = columns, modifier = Modifier.selectableGroup(), cell = compact)
    }

    // — Starter palettes —
    BenchInkStarters(
        starters = benchStarterPalettes(inks),
        current = current,
        spoken = { inkName(it, inks, kind) },
        big = big,
        onPick = onPick,
        onSaveStarterCopy = onSaveStarterCopy,
    )
}

/**
 * The current-ink row: pot + "Current ink" + name. Not a live region (A40 rev. 2: the snack is the one
 * announcement); a focusable node, so focus can land on it after Undo (INK task 9).
 */
@Composable
private fun BenchInkCurrent(colour: Color, name: String) {
    val colors = ZinelyTheme.v21Colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = BenchInkCurrentMinHeightDp)
            .background(colors.paper, RoundedCornerShape(BenchInkCurrentRadiusDp))
            .border(BenchInkPopoverBorderDp, colors.ink, RoundedCornerShape(BenchInkCurrentRadiusDp))
            .padding(horizontal = ZinelyV21Dimens.gapMd, vertical = ZinelyV21Dimens.gapSm)
            .testTag(BenchInkCurrentTestTag)
            // One spoken node ("Current ink, ‹name›"), not its two Texts as well; focusable so focus can be
            // put here after Undo (INK task 9).
            .clearAndSetSemantics { contentDescription = Copy.BenchInk.currentInk(name) }
            .focusable(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(BenchInkSwatchSizeDp)
                    .background(colour, CircleShape)
                    .border(BenchInkSwatchBorderDp, BenchInkPaperInk, CircleShape),
            )
            Column {
                Text(
                    text = Copy.BenchInk.CURRENT_INK.uppercase(),
                    fontFamily = ZinelyV21Fonts.Work,
                    fontSize = BenchInkCurrentLabelSp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = BenchInkCurrentLabelTracking,
                    color = BenchInkPaperSoft,
                )
                Text(
                    text = name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = ZinelyV21Fonts.Voice,
                    fontSize = BenchInkCurrentNameSp,
                    fontWeight = FontWeight.Bold,
                    color = BenchInkPaperInk,
                )
            }
        }
    }
}

/**
 * `.inkpop.a40 .inklbl` — the uppercase section label, `min-height:28px`, with an optional count (`small`,
 * `.6rem/600`, not uppercase) beside it ([countInline], My palettes) or at the far end (My inks), and an
 * optional [trailing] action at the far end.
 */
@Composable
private fun BenchInkSectionLabel(
    label: String,
    count: String? = null,
    countInline: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = ZinelyTheme.v21Colors
    val small: @Composable (String) -> Unit = {
        Text(
            text = it,
            fontFamily = ZinelyV21Fonts.Work,
            fontSize = BenchInkBandLabelSp,
            fontWeight = FontWeight.SemiBold,
            color = colors.inkSoft,
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = BenchInkBandLabelTopGapDp, bottom = BenchInkBandLabelGapDp)
            .defaultMinSize(minHeight = BenchInkSectionLabelMinHeightDp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(BenchInkCountGapDp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label.uppercase(),
                fontFamily = ZinelyV21Fonts.Work,
                fontSize = BenchInkBandLabelSp,
                fontWeight = FontWeight.Bold,
                letterSpacing = BenchInkBandLabelTracking,
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                color = colors.inkSoft,
                modifier = Modifier.testTag(BenchInkBandLabelTestTag),
            )
            if (countInline && count != null) small(count)
        }
        if (!countInline && count != null) small(count)
        trailing?.invoke()
    }
}

/**
 * `.lbl-act` `+ New palette`: a 32dp paper pill whose touch area reaches 48dp (`::after{inset:-8px -2px}`).
 * Compose extends a smaller pointer target to the 48dp minimum itself, so the paint stays 32dp.
 */
@Composable
private fun BenchInkNewPaletteAction(onClick: () -> Unit) {
    val colors = ZinelyTheme.v21Colors
    val tap = benchTap(action = onClick)
    Box(
        modifier = Modifier
            .clip(BenchInkPillShape)
            .background(colors.paper)
            .border(BenchInkPopoverBorderDp, colors.ink, BenchInkPillShape)
            .clickable(role = Role.Button, onClick = tap)
            .defaultMinSize(minHeight = BenchInkLabelActionMinHeightDp)
            .padding(horizontal = ZinelyV21Dimens.gapMd)
            .testTag(BenchInkNewPaletteTestTag)
            .clearAndSetSemantics {
                contentDescription = Copy.BenchInk.NEW_PALETTE
                role = Role.Button
                onClick { tap(); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = Copy.BenchInk.NEW_PALETTE_ACTION,
            fontFamily = ZinelyV21Fonts.Work,
            fontSize = BenchInkLabelActionSp,
            fontWeight = FontWeight.Bold,
            color = BenchInkPaperInk,
        )
    }
}

/** `.pal-empty`: dashed ink box, the lead in bold ink, the rest in `--ink-soft`. */
@Composable
private fun BenchInkPalettesEmpty() {
    val colors = ZinelyTheme.v21Colors
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.ink)) {
                append(Copy.BenchInk.PALETTES_EMPTY_LEAD)
            }
            append(Copy.BenchInk.PALETTES_EMPTY_REST)
        },
        fontFamily = ZinelyV21Fonts.Work,
        fontSize = BenchInkEmptySp,
        lineHeight = 1.4.em,
        color = colors.inkSoft,
        modifier = Modifier
            .padding(bottom = ZinelyV21Dimens.gapSm)
            .fillMaxWidth()
            .benchDashedBorder(BenchInkPopoverBorderDp, colors.ink, ZinelyV21Dimens.radiusMd)
            .padding(horizontal = ZinelyV21Dimens.gapMd, vertical = ZinelyV21Dimens.gapSm)
            .testTag(BenchInkPalettesEmptyTestTag),
    )
}

/**
 * `.pal`: a paper card, the palette's name in Voice 700, a [trailing] control (⋯ for the maker's own,
 * `Save a copy` for a starter), then its pots — 26dp in 40×48 targets, ringed when equal to [current].
 * The card is a named group (`role=group`, "‹name›, ‹n› inks").
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BenchInkPaletteCard(
    name: String,
    hexes: List<InkHex>,
    current: InkHex,
    spoken: (InkHex) -> String,
    big: Boolean,
    onPick: (Color) -> Unit,
    tag: String,
    potTag: String,
    trailing: @Composable () -> Unit,
) {
    val colors = ZinelyTheme.v21Colors
    val shape = RoundedCornerShape(ZinelyV21Dimens.radiusMd)
    Column(
        modifier = Modifier
            .padding(bottom = ZinelyV21Dimens.gapSm)
            .fillMaxWidth()
            .background(colors.paper, shape)
            .border(BenchInkPopoverBorderDp, colors.ink, shape)
            .padding(
                start = ZinelyV21Dimens.gapMd,
                top = ZinelyV21Dimens.gapXs,
                end = ZinelyV21Dimens.gapXs,
                bottom = ZinelyV21Dimens.gapXs,
            )
            .testTag(tag)
            .semantics { contentDescription = Copy.BenchInk.paletteGroup(name, hexes.size) },
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = name,
                maxLines = if (big) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = ZinelyV21Fonts.Voice,
                fontSize = BenchInkPaletteNameSp,
                fontWeight = FontWeight.Bold,
                color = BenchInkPaperInk,
                modifier = Modifier.weight(1f),
            )
            trailing()
        }
        // `.pal-pots{margin:-6px 0 0 -7px}`: the pots' own 40×48 cells overhang the card's padding.
        FlowRow(
            modifier = Modifier
                .benchNegativeMargin(start = BenchInkPalettePotsPullStartDp, top = BenchInkPalettePotsPullTopDp)
                .selectableGroup(),
        ) {
            for (hex in hexes) {
                val colour = hex.toColorRgba().toComposeColor()
                val on = hex == current
                val pick = benchTap(ZinelyHaptic.Snap) { onPick(colour) }
                Box(
                    modifier = Modifier
                        .size(BenchInkPalettePotTargetWidthDp, BenchInkPalettePotTargetHeightDp)
                        .selectable(selected = on, role = Role.RadioButton, onClick = pick)
                        .testTag(potTag)
                        .clearAndSetSemantics {
                            contentDescription = Copy.BenchInk.palettePot(spoken(hex), name)
                            role = Role.RadioButton
                            this.selected = on
                            onClick { pick(); true }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    BenchInkSwatchDot(colour, on, BenchInkPalettePotDp, BenchInkPaperInk)
                }
            }
        }
    }
}

/** `.pal-more` ⋯ — 48dp, spoken "More for ‹palette›". */
@Composable
private fun BenchInkPaletteMore(palette: String, onClick: () -> Unit) {
    val tap = benchTap(action = onClick)
    Box(
        modifier = Modifier
            .size(BenchInkTouchDp)
            .clip(RoundedCornerShape(ZinelyV21Dimens.radiusSm))
            .clickable(role = Role.Button, onClick = tap)
            .testTag(BenchInkPaletteMoreTestTag)
            .clearAndSetSemantics {
                contentDescription = Copy.BenchInk.moreFor(palette)
                role = Role.Button
                onClick { tap(); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = BenchInkMoreGlyph,
            fontFamily = ZinelyV21Fonts.Work,
            fontSize = BenchInkMoreSp,
            fontWeight = FontWeight.Bold,
            color = BenchInkPaperInk,
        )
    }
}

/**
 * `.pal-pick` / `.pal-mini`: one strip per palette, its inks as equal segments, 28dp tall in a 48dp target
 * (`margin:10px 0`), five a row (three at largest text). A radio group, "Your palettes"; the open one carries
 * a 2dp dashed outline at a 3dp offset.
 */
@Composable
private fun BenchInkStrips(palettes: List<InkPalette>, openId: Long, big: Boolean, onOpen: (Long) -> Unit) {
    val colors = ZinelyTheme.v21Colors
    BenchInkGrid(
        items = palettes,
        columns = if (big) BenchInkLargeTextColumns else BenchInkGridColumns,
        gap = BenchInkStripGapDp,
        modifier = Modifier
            .padding(bottom = ZinelyV21Dimens.gapSm)
            .selectableGroup()
            .semantics { contentDescription = Copy.BenchInk.YOUR_PALETTES },
    ) { palette, m ->
        val on = palette.id == openId
        val open = benchTap(ZinelyHaptic.Snap) { onOpen(palette.id) }
        Box(
            modifier = m
                .height(BenchInkTouchDp)
                .selectable(selected = on, role = Role.RadioButton, onClick = open)
                .testTag(BenchInkStripTestTag)
                .clearAndSetSemantics {
                    contentDescription = Copy.BenchInk.paletteGroup(palette.name, palette.inks.size)
                    role = Role.RadioButton
                    this.selected = on
                    onClick { open(); true }
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BenchInkStripHeightDp)
                    // `aria-checked=true{outline:2px dashed var(--ink);outline-offset:3px}` — outside the clip.
                    .drawBehind {
                        if (!on) return@drawBehind
                        val w = BenchInkStripOutlineDp.toPx()
                        val o = BenchInkStripOutlineOffsetDp.toPx() + w / 2f
                        benchDashedRoundRect(colors.ink, w, Offset(-o, -o), Size(size.width + 2 * o, size.height + 2 * o), size.height / 2f + o)
                    }
                    .clip(BenchInkPillShape)
                    .background(colors.paper)
                    .border(BenchInkPopoverBorderDp, colors.ink, BenchInkPillShape),
            ) {
                for (hex in palette.inks) {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(hex.toColorRgba().toComposeColor()),
                    )
                }
            }
        }
    }
}

/**
 * A kept ink: a 30dp pot in a 56dp choice, the visible name `#RRGGBB` in tabular figures, spoken
 * "My ink, ‹colour words›, ‹spoken hex›" (prototype `mineHtml`: always `colourName`, even for a built-in hex).
 */
@Composable
private fun BenchInkKeptChoice(kept: MyInk, selected: Boolean, onPick: (Color) -> Unit, modifier: Modifier) {
    val colour = kept.hex.toColorRgba().toComposeColor()
    BenchInkChoice(
        colour = colour,
        spoken = Copy.BenchInk.myInkLabel(inkColourWords(colourName(kept.hex)), kept.hex.spoken()),
        selected = selected,
        onClick = { onPick(colour) },
        tag = BenchInkKeptTestTag,
        label = BenchInkHexPrefix + kept.hex.value,
        tabular = true,
        modifier = modifier,
    )
}

/**
 * One ink choice (`.ink-choice`): a pot in a named radio target. The paint stays at [potSize]; the target is
 * at least [minHeight] high. The label, when drawn, is decorative ([spoken] is the node's whole name).
 */
@Composable
private fun BenchInkChoice(
    colour: Color,
    spoken: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
    potSize: Dp = BenchInkSwatchSizeDp,
    minHeight: Dp = BenchInkChoiceMinHeightDp,
    label: String? = null,
    tabular: Boolean = false,
) {
    val colors = ZinelyTheme.v21Colors
    val pick = benchTap(ZinelyHaptic.Snap, onClick)
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .selectable(selected = selected, role = Role.RadioButton, onClick = pick)
            .testTag(tag)
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.RadioButton
                this.selected = selected
                onClick { pick(); true }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BenchInkChoiceGapDp, Alignment.CenterVertically),
    ) {
        BenchInkSwatchDot(colour, selected, potSize, colors.ink)
        if (label != null) {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = ZinelyV21Fonts.Work,
                fontSize = BenchInkChoiceNameSp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = if (tabular) BenchInkHexTracking else 0.em,
                style = if (tabular) LocalTextStyle.current.copy(fontFeatureSettings = "tnum") else LocalTextStyle.current,
                color = colors.inkSoft,
            )
        }
    }
}

/**
 * `.ink-choice.mix`: a dashed, empty paper pot with a plus — "an ink you haven't made yet". Label "Mix",
 * spoken "Mix an ink". A button, not a radio: it opens the mixer rather than choosing an ink.
 */
@Composable
private fun BenchInkMixChoice(onClick: () -> Unit, modifier: Modifier) {
    val colors = ZinelyTheme.v21Colors
    val tap = benchTap(action = onClick)
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = BenchInkChoiceMinHeightDp)
            .clickable(role = Role.Button, onClick = tap)
            .testTag(BenchInkMixTestTag)
            .clearAndSetSemantics {
                contentDescription = Copy.BenchInk.MIX_AN_INK
                role = Role.Button
                onClick { tap(); true }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BenchInkChoiceGapDp, Alignment.CenterVertically),
    ) {
        Canvas(
            modifier = Modifier
                .size(BenchInkSwatchSizeDp)
                .background(colors.paper, CircleShape)
                .benchDashedBorder(BenchInkSwatchBorderDp, colors.ink, BenchInkSwatchSizeDp / 2),
        ) {
            // `<path d="M12 6v12M6 12h12"/>` at 16dp, `stroke-width:2.4`, round caps, in #27270F.
            val u = BenchInkMixGlyphDp.toPx() / 24f
            val o = (size.minDimension - BenchInkMixGlyphDp.toPx()) / 2f
            val w = BenchInkMixGlyphStroke * u
            drawLine(BenchInkPaperInk, Offset(o + 12 * u, o + 6 * u), Offset(o + 12 * u, o + 18 * u), w, StrokeCap.Round)
            drawLine(BenchInkPaperInk, Offset(o + 6 * u, o + 12 * u), Offset(o + 18 * u, o + 12 * u), w, StrokeCap.Round)
        }
        Text(
            text = Copy.BenchInk.MIX,
            maxLines = 1,
            fontFamily = ZinelyV21Fonts.Work,
            fontSize = BenchInkChoiceNameSp,
            fontWeight = FontWeight.Bold,
            color = colors.ink,
        )
    }
}

/**
 * `.disclose` + `#starterList`: a 48dp row ("Starter palettes", "3 to copy", a chevron that turns 180° when
 * open), closed by default each time the tray opens. Open, it shows the three starters as `.pal` cards
 * with `Save a copy` in place of ⋯. A button with expand/collapse state.
 */
@Composable
private fun BenchInkStarters(
    starters: List<BenchStarterPalette>,
    current: InkHex,
    spoken: (InkHex) -> String,
    big: Boolean,
    onPick: (Color) -> Unit,
    onSaveStarterCopy: (BenchStarterPalette) -> Unit,
) {
    val colors = ZinelyTheme.v21Colors
    var open by remember { mutableStateOf(false) }
    val turnMillis = if (ZinelyTheme.motion.reduceMotion) 0 else BenchInkChevronMillis
    val turn by animateFloatAsState(if (open) 180f else 0f, tween(turnMillis), label = "starterChevron")
    val toggle = benchTap { open = !open }
    Row(
        modifier = Modifier
            .padding(top = ZinelyV21Dimens.gapXs)
            .fillMaxWidth()
            .drawBehind {
                val w = BenchInkPopoverBorderDp.toPx()
                benchDashedLine(colors.hair, w, y = w / 2f)
            }
            .clickable(role = Role.Button, onClick = toggle)
            .defaultMinSize(minHeight = BenchInkTouchDp)
            .testTag(BenchInkStartersTestTag)
            .semantics {
                if (open) collapse { toggle(); true } else expand { toggle(); true }
            },
        horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = Copy.BenchInk.STARTER_PALETTES.uppercase(),
            fontFamily = ZinelyV21Fonts.Work,
            fontSize = BenchInkBandLabelSp,
            fontWeight = FontWeight.Bold,
            letterSpacing = BenchInkBandLabelTracking,
            color = colors.ink,
        )
        Text(
            text = Copy.BenchInk.STARTER_COUNT,
            fontFamily = ZinelyV21Fonts.Work,
            fontSize = BenchInkLabelActionSp,
            fontWeight = FontWeight.SemiBold,
            color = colors.inkSoft,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = BenchInkChevronGlyph,
            fontFamily = ZinelyV21Fonts.Work,
            fontSize = BenchInkBandLabelSp,
            color = colors.ink,
            modifier = Modifier
                .rotate(turn)
                .clearAndSetSemantics {},
        )
    }
    if (open) {
        for (starter in starters) {
            BenchInkPaletteCard(
                name = starter.name,
                hexes = starter.inks.map { it.value.inkHex() },
                current = current,
                spoken = spoken,
                big = big,
                onPick = onPick,
                tag = BenchInkStarterTestTag,
                potTag = BenchInkStarterPotTestTag,
            ) { BenchInkSaveCopy { onSaveStarterCopy(starter) } }
        }
    }
}

/** `.pal-copy` — an underlined text button, 48dp tall. */
@Composable
private fun BenchInkSaveCopy(onClick: () -> Unit) {
    val tap = benchTap(action = onClick)
    Box(
        modifier = Modifier
            .clickable(role = Role.Button, onClick = tap)
            .defaultMinSize(minHeight = BenchInkTouchDp)
            .padding(horizontal = ZinelyV21Dimens.gapSm)
            .testTag(BenchInkSaveCopyTestTag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = Copy.BenchInk.SAVE_A_COPY,
            fontFamily = ZinelyV21Fonts.Work,
            fontSize = BenchInkSaveCopySp,
            fontWeight = FontWeight.Bold,
            textDecoration = TextDecoration.Underline,
            color = BenchInkSaveCopyInk,
        )
    }
}

/** Equal columns with trailing empty cells, so a short last row keeps the grid's column widths. */
@Composable
private fun <T> BenchInkGrid(
    items: List<T>,
    columns: Int,
    modifier: Modifier = Modifier,
    gap: Dp = 0.dp,
    cell: @Composable (T, Modifier) -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(gap)) {
        for (row in items.chunked(columns)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (item in row) cell(item, Modifier.weight(1f))
                repeat(columns - row.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

/** A full-width dashed `--hair` rule, 1.5dp. */
@Composable
private fun BenchInkDashedRule(modifier: Modifier = Modifier) {
    val hair = ZinelyTheme.v21Colors.hair
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(BenchInkPopoverBorderDp)
            .drawBehind { benchDashedLine(hair, size.height, y = size.height / 2f) },
    )
}

private fun DrawScope.benchDashedLine(color: Color, width: Float, y: Float) {
    val dash = BenchInkSelDashDp.toPx()
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = width,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
    )
}

private fun DrawScope.benchDashedRoundRect(color: Color, width: Float, topLeft: Offset, size: Size, radius: Float) {
    val dash = BenchInkSelDashDp.toPx()
    drawRoundRect(
        color = color,
        topLeft = topLeft,
        size = size,
        cornerRadius = CornerRadius(radius),
        style = Stroke(width = width, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
    )
}

/** A dashed border inside the bounds, as CSS's `border-style:dashed` (Compose's `border` is solid only). */
private fun Modifier.benchDashedBorder(width: Dp, color: Color, radius: Dp): Modifier = drawBehind {
    val w = width.toPx()
    benchDashedRoundRect(color, w, Offset(w / 2f, w / 2f), Size(size.width - w, size.height - w), radius.toPx() - w / 2f)
}

/** CSS negative margins: the content overhangs [start]/[top] without taking that space in the layout. */
private fun Modifier.benchNegativeMargin(start: Dp, top: Dp): Modifier = layout { measurable, constraints ->
    val dx = start.roundToPx()
    val dy = top.roundToPx()
    // An unbounded width stays unbounded: `Infinity + dx` would overflow the constraint.
    val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + dx else constraints.maxWidth
    val p = measurable.measure(constraints.copy(maxWidth = maxWidth))
    layout((p.width - dx).coerceAtLeast(0), (p.height - dy).coerceAtLeast(0)) { p.place(-dx, -dy) }
}

/**
 * Frozen `.inkpop h4 button` — a `leaf` pill outlined in ink on a 2dp printed shadow, at least 64×48dp
 * (`.inkpop.a40 h4 button`). ⚠ It rests and does not press: the freeze declares no `:active`.
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
            .defaultMinSize(minWidth = BenchInkDoneMinWidthDp, minHeight = BenchInkDoneMinHeightDp)
            .padding(horizontal = BenchInkDonePaddingH, vertical = BenchInkDonePaddingV),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = Copy.BenchInk.DONE,
            fontFamily = ZinelyV21Fonts.Work,
            fontSize = BenchInkDoneSp,
            fontWeight = FontWeight.Bold,
            lineHeight = ZinelyV21Fonts.InheritedLineHeight,
            color = colors.onLeaf,
        )
    }
}

/**
 * One `.pot` — a circle of ink outlined at 1.5dp in [border], with the chosen mark `1.6px dashed` at
 * `inset:-5px`. Drawn rather than bordered because Compose's `border` is inside the layout bounds, and
 * growing the box to hold the ring would move the grid.
 */
@Composable
private fun BenchInkSwatchDot(colour: Color, selected: Boolean, size: Dp, border: Color) {
    Box(
        modifier = Modifier
            .size(size)
            // Outside the clip below, deliberately: the chosen ring sits 5dp beyond the pot's own bounds.
            .drawBehind {
                if (!selected) return@drawBehind
                val half = this.size.minDimension / 2f
                val ring = BenchInkSelRingDp.toPx()
                val radius = half + BenchInkSelInsetDp.toPx() + ring / 2f
                val dash = BenchInkSelDashDp.toPx()
                drawCircle(
                    color = border,
                    radius = radius,
                    style = Stroke(width = ring, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
                )
            }
            .clip(CircleShape)
            .background(colour, CircleShape)
            .border(BenchInkSwatchBorderDp, border, CircleShape)
            .testTag(BenchInkPotTestTag),
    )
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
internal val BenchInkHeaderGapDp = ZinelyV21Dimens.gapSm

/** `.inkpop h4{font-size:1.02rem}` (`:242`) — 16.32sp in `--voice` at weight 700, where V2 drew 15/500. */
internal val BenchInkTitleSp = 16.32.sp

/** `.inkpop h4 button{font-size:.78rem}` (`:244`) — 12.48sp, weight 700, `--on-leaf` on `--leaf`. */
internal val BenchInkDoneSp = 12.48.sp

/** `.inkpop h4 button{border-radius:var(--br-pill)}` (`:245`). A percent shape, per `BenchBarShape`'s note. */
internal val BenchInkDoneShape: RoundedCornerShape = RoundedCornerShape(percent = 50)

/** `.inkpop h4 button{padding:var(--gap-xs) var(--gap-md)}` (`:246`). */
internal val BenchInkDonePaddingH = ZinelyV21Dimens.gapMd
internal val BenchInkDonePaddingV = ZinelyV21Dimens.gapXs
internal val BenchInkDoneMinWidthDp = 64.dp
internal val BenchInkDoneMinHeightDp = 48.dp

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
internal val BenchInkBandLabelTopGapDp = ZinelyV21Dimens.gapSm
internal val BenchInkBandLabelGapDp = ZinelyV21Dimens.gapHair

internal const val BenchInkGridColumns: Int = 5
internal val BenchInkChoiceMinHeightDp = 56.dp
internal val BenchInkChoiceNameSp = 8.96.sp

internal val BenchInkCurrentMinHeightDp = 52.dp
internal val BenchInkCurrentRadiusDp = ZinelyV21Dimens.radiusMd
internal val BenchInkCurrentLabelSp = 9.76.sp
internal val BenchInkCurrentLabelTracking = 0.1.em
internal val BenchInkCurrentNameSp = 14.08.sp
internal val BenchInkPaperInk = Color(0xFF27270F)
internal val BenchInkPaperSoft = Color(0xFF6A452F)

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
// — A40 rev. 2 (`v21-ink-mixer.html` `.inkpop.a40`, `.pal`, `.pal-mini`, `.pots.compact`, `.disclose`) —

/**
 * Largest text: at or above this font scale the grids drop to fewer columns (`:root[data-big="on"]`:
 * built-in 7 → 5, My inks 5 → 3, strips 5 → 3). The same threshold the Flip tray stacks at
 * (`FlipTray.kt:159`, `fontScale > 1.3f`); inclusive here so a phone set to exactly 130% gets the roomy grid.
 */
internal const val BenchInkLargeTextFontScale: Float = 1.3f

/** `.inkpop.a40 .pots.compact{grid-template-columns:repeat(7,…)}` — the built-in grid. */
internal const val BenchInkCompactColumns: Int = 7

/** `:root[data-big] .inkpop.a40 .pots` / `.pal-pick{repeat(3,…)}` — My inks and strips at largest text. */
internal const val BenchInkLargeTextColumns: Int = 3

/** `.pots.compact .ink-choice{min-height:48px}`, `.pot{width:28px;height:28px}`. */
internal val BenchInkCompactChoiceMinHeightDp = 48.dp
internal val BenchInkCompactPotDp = 28.dp

/** `.ink-choice{gap:3px}`. */
internal val BenchInkChoiceGapDp = 3.dp

/** `.inkpop.a40 .inklbl{min-height:28px}`; `#palCount{margin-left:6px}`. */
internal val BenchInkSectionLabelMinHeightDp = 28.dp
internal val BenchInkCountGapDp = 6.dp

/** `.lbl-act{font:700 .66rem;min-height:32px}` — the paint; the touch area is 48dp. */
internal val BenchInkLabelActionSp = 10.56.sp
internal val BenchInkLabelActionMinHeightDp = 32.dp
internal val BenchInkPillShape: RoundedCornerShape = RoundedCornerShape(percent = 50)

/** A 48dp control: `.pal-more`, `.pal-copy`, `.disclose`, the strip's target. */
internal val BenchInkTouchDp = 48.dp

/** `.pal .pal-name{font:700 .84rem var(--voice)}`. */
internal val BenchInkPaletteNameSp = 13.44.sp

/** `.pal .pal-pot{width:40px;height:48px}` around a 26dp pot (`span{width:26px;height:26px}`). */
internal val BenchInkPalettePotTargetWidthDp = 40.dp
internal val BenchInkPalettePotTargetHeightDp = 48.dp
internal val BenchInkPalettePotDp = 26.dp

/** `.pal .pal-pots{margin:-6px 0 0 -7px}`. */
internal val BenchInkPalettePotsPullStartDp = 7.dp
internal val BenchInkPalettePotsPullTopDp = 6.dp

/** `.pal .pal-more{font:700 1.2rem}` — the ⋯ glyph (`&#8943;`), not a word. */
internal val BenchInkMoreSp = 19.2.sp
internal const val BenchInkMoreGlyph: String = "⋯"

/** `.pal .pal-copy{color:#555A1B;font:700 .72rem;text-decoration:underline}`. */
internal val BenchInkSaveCopySp = 11.52.sp
internal val BenchInkSaveCopyInk = Color(0xFF555A1B)

/** `.pal-empty{font-size:.74rem}`. */
internal val BenchInkEmptySp = 11.84.sp

/** `.pal-pick{gap:6px}`; `.pal-mini{height:28px}`; `[aria-checked=true]{outline:2px dashed;outline-offset:3px}`. */
internal val BenchInkStripGapDp = 6.dp
internal val BenchInkStripHeightDp = 28.dp
internal val BenchInkStripOutlineDp = 2.dp
internal val BenchInkStripOutlineOffsetDp = 3.dp

/** `.ink-choice .ink-name.hex{font-variant-numeric:tabular-nums;letter-spacing:-.01em}`; the visible `#`. */
internal val BenchInkHexTracking = (-0.01).em
internal const val BenchInkHexPrefix: String = "#"

/** `.ink-choice.mix .pot svg{width:16px;height:16px;stroke-width:2.4}`. */
internal val BenchInkMixGlyphDp = 16.dp
internal const val BenchInkMixGlyphStroke: Float = 2.4f

/** `.disclose i{transition:transform .15s}` and its `&#9662;` chevron. */
internal const val BenchInkChevronMillis: Int = 150
internal const val BenchInkChevronGlyph: String = "▾"


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
