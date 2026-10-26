package com.aritr.zinely.feature.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.aritr.zinely.core.editor.ink.PALETTE_CAP
import com.aritr.zinely.core.editor.ink.PALETTE_MIN
import com.aritr.zinely.core.editor.ink.PALETTE_NAME_MAX
import com.aritr.zinely.core.editor.ink.PaletteDraft
import com.aritr.zinely.core.editor.ink.PaletteResult
import com.aritr.zinely.core.editor.ink.ToggleResult
import com.aritr.zinely.ui.components.zinelyFocusRing
import kotlinx.coroutines.CoroutineScope
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
import com.aritr.zinely.ui.a11y.zinelyV2Control
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
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
/** Edit mode (INK task 7). */
internal const val BenchInkPaletteNameTestTag: String = "bench-ink-palette-name"
internal const val BenchInkPaletteNameHelpTestTag: String = "bench-ink-palette-name-help"
internal const val BenchInkDraftStripTestTag: String = "bench-ink-draft-strip"
internal const val BenchInkDraftPotTestTag: String = "bench-ink-draft-pot"
internal const val BenchInkCountTestTag: String = "bench-ink-count"
/** A kept or built-in ink as an edit-mode toggle. */
internal const val BenchInkToggleTestTag: String = "bench-ink-toggle"
/** A "Was in this palette" toggle. */
internal const val BenchInkLostTestTag: String = "bench-ink-lost"
internal const val BenchInkCancelTestTag: String = "bench-ink-cancel"
internal const val BenchInkSavePaletteTestTag: String = "bench-ink-save-palette"
/** A menu row, suffixed `-‹label›`. */
internal const val BenchInkMenuItemTestTag: String = "bench-ink-menu"

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
 * ### Palette edit mode (INK task 7, prototype `startEdit` / `renderEdit` / `peCancel` / `peSave`)
 *
 * While [draft] is non-null the same card edits a palette: the title reads `New palette` / `Edit palette`,
 * Done and Current ink hide, the body becomes [BenchInkEditBody] and a Cancel / Save palette bar is pinned
 * below the scroll. Back is Cancel. The draft is hoisted ([onDraftChange]) because the snack's Undo, which
 * the host owns, has to reopen it after the tray's own composition is gone.
 *
 * Every library write here is a transform through [onEditInkLibrary]; a store failure (`IOException`) is
 * "nothing changed": no snack, nothing closed, never a crash. A snack offers Undo (a [BenchInkTrayUndo],
 * which the host runs) only when the write changed the library or there is a draft to reopen.
 *
 * @param inkLibrary My inks and My palettes, or `null` while loading.
 * @param onEditInkLibrary every library write: the open strip, palettes, a kept ink's removal.
 * @param onMix the Mix pot, in the tray and in edit mode (the host tells them apart by the draft).
 * @param draft the palette being made or edited, or `null` for the tray.
 * @param onSnack a line for the host's snack, with what its Undo restores (`null`: no Undo).
 * @param focusRequest where focus goes once the control that had it is gone (A40 rev. 2 `keepFocus`
 *   fallbacks; INK task 9); hoisted because the host's Undo asks for it too. Consumed with
 *   [onFocusRequest]`(null)` once served.
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
    draft: PaletteDraft? = null,
    onDraftChange: (PaletteDraft?) -> Unit = {},
    onSnack: (String, BenchInkTrayUndo?) -> Unit = { _, _ -> },
    focusRequest: BenchInkFocusRequest? = null,
    onFocusRequest: (BenchInkFocusRequest?) -> Unit = {},
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
        val current = (selected ?: colors.ink).inkHex()
        val focus = remember { BenchInkFocusTargets() }
        val actions = rememberBenchInkTrayActions(inkLibrary, current, onEditInkLibrary, draft, onDraftChange, onSnack, onFocusRequest)
        // Served once the tray shows what the request is about: the right mode, and the library the write
        // returned (the screen's copy can trail the store by a frame).
        LaunchedEffect(focusRequest, inkLibrary, draft) {
            val r = focusRequest ?: return@LaunchedEffect
            if (inkLibrary == null || (r.library != null && inkLibrary != r.library)) return@LaunchedEffect
            val editing = draft != null
            if (editing != (r.target == BenchInkFocus.Count || r.target == BenchInkFocus.Toggle)) return@LaunchedEffect
            val target = when (r.target) {
                BenchInkFocus.Palettes -> if (inkLibrary.openPalette() != null) focus.more else focus.newPalette
                BenchInkFocus.MyInks -> if (inkLibrary.myInks.isNotEmpty()) focus.firstKept else focus.mix
                BenchInkFocus.Current -> focus.current
                BenchInkFocus.Count -> focus.count
                // The toggle asks for itself (see [BenchInkToggleChoice]) and clears the request.
                BenchInkFocus.Toggle -> return@LaunchedEffect
            }
            // A target not attached yet (the tray mid-enter) throws; focus then stays where it was.
            runCatching { target.requestFocus() }
            onFocusRequest(null)
        }
        // Composed after the host's tray handler, so it wins while editing: Back is Cancel (`peCancel`).
        BackHandler(enabled = draft != null) { actions.cancel() }
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
                    text = when {
                        draft == null -> Copy.BenchInk.TITLE
                        draft.id == null -> Copy.BenchInk.NEW_PALETTE
                        else -> Copy.BenchInk.EDIT_PALETTE
                    },
                    fontFamily = ZinelyV21Fonts.Voice,
                    fontWeight = FontWeight.Bold,
                    fontSize = BenchInkTitleSp,
                    lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                    color = colors.ink,
                    modifier = Modifier.testTag(BenchInkTitleTestTag),
                )
                if (draft == null) BenchInkDoneChip(onDone)
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
                if (draft != null && inkLibrary != null) {
                    BenchInkEditBody(draft, inkLibrary, kind, actions, onMix, focus, focusRequest, onFocusRequest)
                } else {
                    BenchInkTrayBody(
                        kind = kind,
                        selected = selected,
                        onPick = onPick,
                        inkLibrary = inkLibrary,
                        onMix = onMix,
                        actions = actions,
                        focus = focus,
                    )
                }
            }
            if (draft != null && inkLibrary != null) BenchInkEditBar(draft, actions)
        }
    }
}

@Composable
private fun BenchInkTrayBody(
    kind: BenchVerbKind,
    selected: Color?,
    onPick: (Color) -> Unit,
    inkLibrary: InkLibrary?,
    onMix: () -> Unit,
    actions: BenchInkTrayActions,
    focus: BenchInkFocusTargets,
) {
    val colors = ZinelyTheme.v21Colors
    val inks = ZinelyTheme.contentInks
    val shown = selected ?: colors.ink
    val current = shown.inkHex()
    val big = LocalDensity.current.fontScale >= BenchInkLargeTextFontScale

    BenchInkCurrent(colour = shown, name = inkName(current, inks, kind), focus = focus.current)

    if (inkLibrary != null) {
        // — My palettes —
        val palettes = inkLibrary.palettes
        BenchInkSectionLabel(
            label = Copy.BenchInk.MY_PALETTES,
            count = palettes.takeIf { it.isNotEmpty() }?.let { Copy.BenchInk.paletteCount(it.size) },
            countInline = true,
        ) { BenchInkNewPaletteAction(actions::newPalette, focus.newPalette) }
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
            ) {
                BenchInkPaletteMore(
                    focus = focus.more,
                    palette = open.name,
                    items = listOf(
                        Copy.BenchInk.EDIT to { actions.edit(open) },
                        Copy.BenchInk.DUPLICATE to { actions.duplicate(open) },
                        Copy.BenchInk.DELETE to { actions.delete(open) },
                    ),
                )
            }
            if (palettes.size >= 2) {
                BenchInkStrips(palettes = palettes, openId = open.id, big = big, onOpen = actions::openStrip)
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
            if (kept == null) {
                BenchInkMixChoice(onMix, m.focusRequester(focus.mix))
            } else {
                val first = kept == inkLibrary.myInks.first()
                BenchInkKeptChoice(kept, kept.hex == current, onPick, { actions.removeKept(kept.hex) }, if (first) m.focusRequester(focus.firstKept) else m)
            }
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
        onSaveStarterCopy = actions::saveCopy,
    )
}

/**
 * The current-ink row: pot + "Current ink" + name. Not a live region (A40 rev. 2: the snack is the one
 * announcement); a focusable node, so focus can land on it after Undo (INK task 9).
 */
@Composable
private fun BenchInkCurrent(colour: Color, name: String, focus: FocusRequester) {
    val colors = ZinelyTheme.v21Colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = BenchInkCurrentMinHeightDp)
            .background(colors.paper, RoundedCornerShape(BenchInkCurrentRadiusDp))
            .border(BenchInkPopoverBorderDp, colors.ink, RoundedCornerShape(BenchInkCurrentRadiusDp))
            .padding(horizontal = ZinelyV21Dimens.gapMd, vertical = ZinelyV21Dimens.gapSm)
            .testTag(BenchInkCurrentTestTag)
            // Focusable so focus can be put here after Undo (INK task 9). Before the clear, or the clear wipes
            // the focusable semantics and the platform never offers the node to a keyboard or to focus.
            .focusRequester(focus)
            .focusable()
            // One spoken node ("Current ink, ‹name›"), not its two Texts as well.
            .clearAndSetSemantics { contentDescription = Copy.BenchInk.currentInk(name) },
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
private fun BenchInkNewPaletteAction(onClick: () -> Unit, focus: FocusRequester) {
    val colors = ZinelyTheme.v21Colors
    val tap = benchTap(action = onClick)
    Box(
        modifier = Modifier
            .focusRequester(focus)
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

/** `.pal-more` ⋯ — 48dp, spoken "More for ‹palette›"; opens [items] as a [BenchInkMenu] under itself. */
@Composable
private fun BenchInkPaletteMore(focus: FocusRequester, palette: String, items: List<Pair<String, () -> Unit>>) {
    var open by remember { mutableStateOf(false) }
    val tap = benchTap { open = true }
    Box(
        modifier = Modifier
            .focusRequester(focus)
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
        if (open) BenchInkMenu(items) { open = false }
    }
}

/**
 * `.kept-menu` (prototype `menu`): a small popup under its anchor, right edges aligned, 48dp rows, the
 * first row focused. A row closes the menu, then acts; a tap outside or Back closes it.
 */
@Composable
private fun BenchInkMenu(items: List<Pair<String, () -> Unit>>, onDismiss: () -> Unit) {
    val colors = ZinelyTheme.v21Colors
    val shape = RoundedCornerShape(ZinelyV21Dimens.radiusMd)
    val first = remember { FocusRequester() }
    Popup(
        popupPositionProvider = BenchInkMenuPosition,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .padding(end = BenchInkMenuShadowDp, bottom = BenchInkMenuShadowDp)
                .zinelyV21HardShadow(BenchInkMenuShadowDp, colors.inkLine, shape)
                .background(colors.surface, shape)
                .border(BenchInkPopoverBorderDp, colors.ink, shape)
                .padding(ZinelyV21Dimens.gapXs)
                .width(IntrinsicSize.Max),
        ) {
            items.forEachIndexed { i, (label, act) ->
                val pick = benchTap { onDismiss(); act() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = BenchInkTouchDp)
                        .then(if (i == 0) Modifier.focusRequester(first) else Modifier)
                        .clickable(role = Role.Button, onClick = pick)
                        .padding(horizontal = ZinelyV21Dimens.gapMd)
                        .testTag("$BenchInkMenuItemTestTag-$label"),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = label,
                        fontFamily = ZinelyV21Fonts.Work,
                        fontSize = BenchInkMenuSp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.ink,
                    )
                }
            }
        }
        // Inside the popup's own composition, where the requester's node lives. A request that still finds
        // no attached window fails quietly and leaves focus where it was.
        LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    }
}

/** The prototype's `m.style.right = …; m.style.top = r.bottom`, kept inside the window. */
private object BenchInkMenuPosition : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset(
        x = (anchorBounds.right - popupContentSize.width).coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
        y = anchorBounds.bottom.coerceAtMost((windowSize.height - popupContentSize.height).coerceAtLeast(0)),
    )
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
private fun BenchInkKeptChoice(
    kept: MyInk,
    selected: Boolean,
    onPick: (Color) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier,
) {
    val colour = kept.hex.toColorRgba().toComposeColor()
    var menu by remember { mutableStateOf(false) }
    BenchInkChoice(
        colour = colour,
        spoken = Copy.BenchInk.myInkLabel(inkColourWords(colourName(kept.hex)), kept.hex.spoken()),
        selected = selected,
        onClick = { onPick(colour) },
        tag = BenchInkKeptTestTag,
        label = BenchInkHexPrefix + kept.hex.value,
        tabular = true,
        modifier = modifier,
        // A long press offers the one-item menu (`oncontextmenu`); a screen reader's custom action removes
        // directly — a one-item menu would only be an extra step for it. Either way it is undoable.
        onLongClick = { menu = true },
        removeAction = onRemove,
    ) {
        if (menu) BenchInkMenu(listOf(Copy.BenchInk.REMOVE_FROM_MY_INKS to onRemove)) { menu = false }
    }
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
    onLongClick: (() -> Unit)? = null,
    removeAction: (() -> Unit)? = null,
    anchored: @Composable () -> Unit = {},
) {
    val colors = ZinelyTheme.v21Colors
    val pick = benchTap(ZinelyHaptic.Snap, onClick)
    val press = if (onLongClick == null) {
        Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = pick)
    } else {
        Modifier.combinedClickable(
            role = Role.RadioButton,
            onLongClickLabel = Copy.BenchInk.REMOVE_FROM_MY_INKS,
            onLongClick = onLongClick,
            onClick = pick,
        )
    }
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .then(press)
            .testTag(tag)
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.RadioButton
                this.selected = selected
                onClick { pick(); true }
                if (onLongClick != null) onLongClick(Copy.BenchInk.REMOVE_FROM_MY_INKS) { onLongClick(); true }
                if (removeAction != null) {
                    customActions = listOf(CustomAccessibilityAction(Copy.BenchInk.REMOVE_FROM_MY_INKS) { removeAction(); true })
                }
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
        anchored()
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
            ) { BenchInkSaveCopy(starter.name) { onSaveStarterCopy(starter) } }
        }
    }
}

/**
 * `.pal-copy` — an underlined text button, 48dp tall. One named Button on the platform tree, spoken with its
 * palette's name: a plain `clickable` reached TalkBack as three identical unnamed Views (INK device pass F5).
 */
@Composable
private fun BenchInkSaveCopy(palette: String, onClick: () -> Unit) {
    val tap = benchTap(action = onClick)
    Box(
        modifier = Modifier
            // Tag before the control: its clearAndSetSemantics would drop a tag placed after it.
            .testTag(BenchInkSaveCopyTestTag)
            .zinelyV2Control(label = Copy.BenchInk.saveACopyOf(palette), onClick = tap)
            .defaultMinSize(minHeight = BenchInkTouchDp)
            .padding(horizontal = ZinelyV21Dimens.gapSm),
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

internal fun DrawScope.benchDashedLine(color: Color, width: Float, y: Float) {
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
internal fun Modifier.benchDashedBorder(width: Dp, color: Color, radius: Dp): Modifier = drawBehind {
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

// — palette edit mode and the tray's library actions (INK task 7) —

/**
 * What a tray snack's Undo restores; the host runs it, so it still works after the tray has closed.
 * [edit]'s `before` goes back to the store when it is non-null; [reopen] becomes the open draft again.
 */
internal data class BenchInkTrayUndo(val edit: InkLibraryEdit?, val reopen: PaletteDraft?)

/**
 * Every tray action that writes the library or the draft (prototype `wireTray`, `newPal`, `peSave`,
 * `peCancel`, `renderEdit`'s toggles). The rules, in one place:
 *  - a write is a transform through [onEditInkLibrary]; an `IOException` means "nothing changed": nothing
 *    closed (a draft stays open, so no typing is lost), no Undo, never a crash, and never silence — the
 *    snack says `Couldn't save that change.` (A40 rev. 2 amendment 2026-10-26);
 *  - focus that loses its control goes where `keepFocus`'s fallbacks say ([onFocus]): the open palette's
 *    ⋯ (else `+ New palette`) after Delete, Duplicate, Save a copy, Save and Cancel; the first My inks pot
 *    (else Mix) after Remove; the matching toggle (else the count line) after a strip pot's ×;
 *  - a snack offers Undo only when the write changed the library (`InkLibraryEdit.changed`) or there is a
 *    draft to reopen; a refusal (palettes full, six inks, too few) never does;
 *  - Undo after Save, and after Cancel of a changed draft, reopens the draft as it was.
 *
 * ponytail: the wait for a write runs in the tray's own scope. The write itself runs in the ViewModel's
 * scope and completes, but if the tray closes inside those milliseconds its snack (and Undo) is not
 * raised. Undo itself has no such ceiling: the host runs it.
 */
private class BenchInkTrayActions(
    private val library: InkLibrary?,
    private val current: InkHex,
    private val scope: CoroutineScope,
    private val onEditInkLibrary: suspend ((InkLibrary) -> InkLibrary) -> InkLibraryEdit,
    private val draft: PaletteDraft?,
    private val onDraftChange: (PaletteDraft?) -> Unit,
    private val onSnack: (String, BenchInkTrayUndo?) -> Unit,
    private val onFocus: (BenchInkFocusRequest?) -> Unit,
    val nameFocus: FocusRequester,
    /** A Save is being written; a second tap is ignored, or two taps would add the palette twice. */
    private val saving: MutableState<Boolean>,
) {
    fun newPalette() {
        val lib = library ?: return
        if (lib.palettes.size >= PALETTE_CAP) return onSnack(Copy.BenchInk.PALETTES_FULL, null)
        onDraftChange(PaletteDraft.newDraft(lib, current, Copy.BenchInk.DEFAULT_PALETTE_NAME))
    }

    fun edit(p: InkPalette) = onDraftChange(PaletteDraft.editDraft(p))

    fun duplicate(p: InkPalette) = palette(Copy.BenchInk.duplicated(p.name)) { it.duplicate(p.id, Copy.BenchInk.COPY_SUFFIX) }

    fun delete(p: InkPalette) = write(Copy.BenchInk.deleted(p.name), BenchInkFocus.Palettes) { it.deletePalette(p.id) }

    /** A strip opens its palette, persisted so the last opened stays open. Focus stays on the strip. */
    fun openStrip(id: Long) {
        scope.launch {
            try {
                onEditInkLibrary { it.open(id) }
            } catch (_: IOException) {
                onSnack(Copy.BenchInk.SAVE_FAILED, null)
            }
        }
    }

    fun saveCopy(starter: BenchStarterPalette) =
        palette(Copy.BenchInk.savedCopy(starter.name)) { lib ->
            lib.saveStarterCopy(starter.name, starter.inks.map { it.value.inkHex() })
        }

    fun removeKept(hex: InkHex) =
        write(Copy.BenchInk.removedInk(inkColourWords(colourName(hex))), BenchInkFocus.MyInks) { it.remove(hex) }

    fun toggle(hex: InkHex) {
        val d = draft ?: return
        when (val r = d.toggle(hex)) {
            is ToggleResult.Added -> onDraftChange(r.draft)
            is ToggleResult.Removed -> onDraftChange(r.draft)
            ToggleResult.Full -> onSnack(Copy.BenchInk.PALETTE_HOLDS_SIX, null)
        }
    }

    /** A strip pot's ×: focus goes to that ink's toggle below (prototype `renderEdit`), else the count line. */
    fun removeAt(index: Int) {
        val d = draft ?: return
        val hex = d.inks.getOrNull(index) ?: return
        onDraftChange(d.removeAt(index))
        onFocus(BenchInkFocusRequest(BenchInkFocus.Toggle, hex = hex))
    }

    /** The field's `maxlength="30"`. */
    fun rename(name: String) {
        draft?.let { onDraftChange(it.copy(name = name.take(PALETTE_NAME_MAX))) }
    }

    /** `peCancel`, and Back while editing. Silent when nothing changed. */
    fun cancel() {
        val d = draft ?: return
        onDraftChange(null)
        onFocus(BenchInkFocusRequest(BenchInkFocus.Palettes))
        if (d.changed(current)) onSnack(Copy.BenchInk.PALETTE_NOT_SAVED, BenchInkTrayUndo(null, d))
    }

    /** `peSave`: no name focuses the name; fewer than two says so; otherwise add or update. */
    fun save() {
        val d = draft ?: return
        val name = d.name.trim()
        when {
            name.isEmpty() -> runCatching { nameFocus.requestFocus() }
            d.inks.size < PALETTE_MIN -> onSnack(Copy.BenchInk.PICK_AT_LEAST_TWO, null)
            saving.value -> Unit
            else -> {
                saving.value = true
                palette(
                    message = if (d.id == null) Copy.BenchInk.newPalette(name) else Copy.BenchInk.saved(name),
                    reopen = d,
                    onDone = { onDraftChange(null) },
                    onEnd = { saving.value = false },
                ) { lib -> d.id?.let { lib.updatePalette(it, name, d.inks) } ?: lib.addPalette(name, d.inks) }
            }
        }
    }

    /** A plain library write that cannot be refused: Delete, Remove from My inks. */
    private fun write(message: String, then: BenchInkFocus, op: (InkLibrary) -> InkLibrary) {
        scope.launch {
            val edit = try {
                onEditInkLibrary(op)
            } catch (_: IOException) {
                return@launch onSnack(Copy.BenchInk.SAVE_FAILED, null)
            }
            onFocus(BenchInkFocusRequest(then, edit.after))
            onSnack(message, BenchInkTrayUndo(edit, null).takeIf { edit.changed })
        }
    }

    /** A palette write, which the library may refuse; a race to the cap answers `Full` here. */
    private fun palette(
        message: String,
        reopen: PaletteDraft? = null,
        onDone: () -> Unit = {},
        onEnd: () -> Unit = {},
        op: (InkLibrary) -> PaletteResult,
    ) {
        scope.launch {
            var result: PaletteResult? = null
            val edit = try {
                onEditInkLibrary { lib -> op(lib).also { result = it }.let { (it as? PaletteResult.Done)?.library ?: lib } }
            } catch (_: IOException) {
                return@launch onSnack(Copy.BenchInk.SAVE_FAILED, null)
            } finally {
                onEnd()
            }
            when (result) {
                is PaletteResult.Done -> {
                    onDone()
                    onFocus(BenchInkFocusRequest(BenchInkFocus.Palettes, edit.after))
                    onSnack(message, BenchInkTrayUndo(edit, reopen).takeIf { edit.changed })
                }
                PaletteResult.Full -> onSnack(Copy.BenchInk.PALETTES_FULL, null)
                PaletteResult.TooFew -> onSnack(Copy.BenchInk.PICK_AT_LEAST_TWO, null)
                // Unreachable from this UI (Save checks the name; a palette cannot vanish under its own
                // menu); the draft stays open rather than guessing.
                PaletteResult.NoName, PaletteResult.Missing, null -> Unit
            }
        }
    }
}

@Composable
private fun rememberBenchInkTrayActions(
    library: InkLibrary?,
    current: InkHex,
    onEditInkLibrary: suspend ((InkLibrary) -> InkLibrary) -> InkLibraryEdit,
    draft: PaletteDraft?,
    onDraftChange: (PaletteDraft?) -> Unit,
    onSnack: (String, BenchInkTrayUndo?) -> Unit,
    onFocus: (BenchInkFocusRequest?) -> Unit,
): BenchInkTrayActions {
    val scope = rememberCoroutineScope()
    val nameFocus = remember { FocusRequester() }
    val saving = remember { mutableStateOf(false) }
    return BenchInkTrayActions(library, current, scope, onEditInkLibrary, draft, onDraftChange, onSnack, onFocus, nameFocus, saving)
}

/** Where focus goes when the control that had it is gone (A40 rev. 2 `keepFocus` fallbacks; INK task 9). */
internal enum class BenchInkFocus { Palettes, MyInks, Current, Count, Toggle }

/**
 * A pending focus move: [target], once the tray shows [library] (`null`: whatever it shows); [hex] names the
 * toggle for [BenchInkFocus.Toggle].
 */
internal data class BenchInkFocusRequest(val target: BenchInkFocus, val library: InkLibrary? = null, val hex: InkHex? = null)

/** The tray's fixed focus targets, owned by the tray (one set per composition). */
internal class BenchInkFocusTargets {
    val current = FocusRequester()
    val more = FocusRequester()
    val newPalette = FocusRequester()
    val firstKept = FocusRequester()
    val mix = FocusRequester()
    val count = FocusRequester()
}

/**
 * `#editBody`: the name field and its help, the strip of chosen inks, the count line, "Was in this
 * palette" when an edit dropped an ink nothing else offers, then My inks (and Mix) and the built-in inks as
 * toggles. Focus opens on the count line, not the name (no keyboard springs up; prototype `startEdit`).
 */
@Composable
private fun BenchInkEditBody(
    draft: PaletteDraft,
    library: InkLibrary,
    kind: BenchVerbKind,
    actions: BenchInkTrayActions,
    onMix: () -> Unit,
    focus: BenchInkFocusTargets,
    focusRequest: BenchInkFocusRequest?,
    onFocusRequest: (BenchInkFocusRequest?) -> Unit,
) {
    val colors = ZinelyTheme.v21Colors
    val inks = ZinelyTheme.contentInks
    val big = LocalDensity.current.fontScale >= BenchInkLargeTextFontScale
    val countFocus = focus.count
    LaunchedEffect(Unit) { runCatching { countFocus.requestFocus() } }
    // A strip pot's ×: its toggle takes focus, the first one showing that ink; none (cannot happen while
    // "Was in this palette" lists a lost ink) falls back to the count line.
    val toggleFor = focusRequest?.takeIf { it.target == BenchInkFocus.Toggle }?.hex
    val focusToggle: (InkHex) -> Boolean = { it == toggleFor }
    LaunchedEffect(toggleFor) {
        if (toggleFor == null) return@LaunchedEffect
        val shown = toggleFor in library.myInks.map { it.hex } || toggleFor in benchBuiltInInks(inks, kind).map { it.value.inkHex() } ||
            toggleFor in draft.lost(library, benchBuiltInInks(inks, kind).map { it.value.inkHex() })
        if (!shown) {
            runCatching { countFocus.requestFocus() }
            onFocusRequest(null)
        }
    }

    BenchInkNameField(draft, library, actions)
    BenchInkDraftStrip(draft, inks, kind, actions)
    Text(
        text = Copy.BenchInk.pickedCount(draft.inks.size),
        fontFamily = ZinelyV21Fonts.Work,
        fontSize = BenchInkEmptySp,
        color = colors.inkSoft,
        modifier = Modifier
            .padding(top = ZinelyV21Dimens.gapXs, start = BenchInkHairInsetDp, end = BenchInkHairInsetDp)
            .focusRequester(countFocus)
            .focusable()
            .testTag(BenchInkCountTestTag)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )

    val builtIns = benchBuiltInInks(inks, kind)
    val columns = if (big) BenchInkLargeTextColumns else BenchInkGridColumns
    var claimed = false
    // The first toggle showing [hex] takes a pending strip-× focus; later ones with the same ink do not.
    val claim: (InkHex) -> (() -> Unit)? = { hex ->
        if (!claimed && focusToggle(hex)) {
            claimed = true
            { onFocusRequest(null) }
        } else {
            null
        }
    }
    val toggle: @Composable (InkHex, String, String, Modifier) -> Unit = { hex, spoken, tag, m ->
        BenchInkToggleChoice(
            colour = hex.toColorRgba().toComposeColor(),
            spoken = spoken,
            picked = hex in draft.inks,
            onToggle = { actions.toggle(hex) },
            tag = tag,
            label = BenchInkHexPrefix + hex.value,
            tabular = true,
            modifier = m,
            takeFocus = claim(hex),
        )
    }
    val lost = draft.lost(library, builtIns.map { it.value.inkHex() })
    if (lost.isNotEmpty()) {
        BenchInkSectionLabel(label = Copy.BenchInk.WAS_IN_PALETTE)
        BenchInkGrid(items = lost, columns = columns) { hex, m ->
            toggle(hex, Copy.BenchInk.pickInkLabel(inkColourWords(colourName(hex)), hex.spoken()), BenchInkLostTestTag, m)
        }
    }
    BenchInkSectionLabel(label = Copy.BenchInk.MY_INKS)
    BenchInkGrid(items = library.myInks + null, columns = columns) { kept, m ->
        if (kept == null) {
            BenchInkMixChoice(onMix, m)
        } else {
            toggle(kept.hex, Copy.BenchInk.pickInkLabel(inkColourWords(colourName(kept.hex)), kept.hex.spoken()), BenchInkToggleTestTag, m)
        }
    }
    BenchInkSectionLabel(label = Copy.BenchInk.BUILT_IN_INKS)
    val compactColumns = if (big) BenchInkGridColumns else BenchInkCompactColumns
    val compact: @Composable (BenchInkSwatch, Modifier) -> Unit = { swatch, m ->
        val hex = swatch.value.inkHex()
        BenchInkToggleChoice(
            colour = swatch.value,
            spoken = Copy.BenchInk.swatchLabel(swatch.name),
            picked = hex in draft.inks,
            onToggle = { actions.toggle(hex) },
            tag = BenchInkToggleTestTag,
            potSize = BenchInkCompactPotDp,
            minHeight = BenchInkCompactChoiceMinHeightDp,
            modifier = m,
            takeFocus = claim(hex),
        )
    }
    BenchInkGrid(items = builtIns.filterNot { it.tint }, columns = compactColumns, cell = compact)
    val tints = builtIns.filter { it.tint }
    if (tints.isNotEmpty()) {
        BenchInkDashedRule(Modifier.padding(vertical = ZinelyV21Dimens.gapHair))
        BenchInkGrid(items = tints, columns = compactColumns, cell = compact)
    }
}

/** `.pe-name`: a visible label, the 48dp field (`maxlength=30`, not autofocused) and its help line. */
@Composable
private fun BenchInkNameField(draft: PaletteDraft, library: InkLibrary, actions: BenchInkTrayActions) {
    val colors = ZinelyTheme.v21Colors
    val shape = RoundedCornerShape(ZinelyV21Dimens.radiusMd)
    var focused by remember { mutableStateOf(false) }
    Text(
        text = Copy.BenchInk.PALETTE_NAME.uppercase(),
        fontFamily = ZinelyV21Fonts.Work,
        fontSize = BenchInkLabelActionSp,
        fontWeight = FontWeight.Bold,
        letterSpacing = BenchInkCurrentLabelTracking,
        color = colors.inkSoft,
        // The field carries the name itself (`<label for>`), so the visible label is not a second node.
        modifier = Modifier
            .padding(top = ZinelyV21Dimens.gapXs, bottom = ZinelyV21Dimens.gapXs)
            .clearAndSetSemantics {},
    )
    BasicTextField(
        value = draft.name,
        onValueChange = actions::rename,
        singleLine = true,
        textStyle = TextStyle(
            fontFamily = ZinelyV21Fonts.Voice,
            fontWeight = FontWeight.Bold,
            fontSize = BenchInkNameSp,
            color = BenchInkPaperInk,
        ),
        cursorBrush = SolidColor(BenchInkPaperInk),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(actions.nameFocus)
            .onFocusChanged { focused = it.isFocused }
            // `.pe-name input:focus{outline:3px solid var(--berry);outline-offset:2px}`.
            .zinelyFocusRing(focused, ZinelyV21Dimens.radiusMd, offset = BenchInkNameRingOffsetDp, color = colors.berry)
            .testTag(BenchInkPaletteNameTestTag)
            .semantics { contentDescription = Copy.BenchInk.PALETTE_NAME },
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = BenchInkTouchDp)
                    .background(colors.paper, shape)
                    .border(BenchInkPopoverBorderDp, colors.ink, shape)
                    .padding(horizontal = ZinelyV21Dimens.gapMd),
                contentAlignment = Alignment.CenterStart,
            ) { inner() }
        },
    )
    val empty = draft.name.trim().isEmpty()
    val help = when {
        empty -> Copy.BenchInk.NAME_EMPTY_HELP
        draft.isDuplicateName(library) -> Copy.BenchInk.NAME_DUPLICATE_HELP
        else -> null
    }
    // `.hexhelp:empty{display:none}`; `.err` is `--jam-text`, 600.
    if (help != null) {
        Text(
            text = help,
            fontFamily = ZinelyV21Fonts.Work,
            fontSize = BenchInkSaveCopySp,
            fontWeight = if (empty) FontWeight.SemiBold else FontWeight.Normal,
            color = if (empty) colors.jamText else colors.inkSoft,
            modifier = Modifier
                .padding(top = ZinelyV21Dimens.gapXs, start = BenchInkHairInsetDp, end = BenchInkHairInsetDp)
                .testTag(BenchInkPaletteNameHelpTestTag)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/**
 * `.pe-strip`: the chosen inks in order on a dashed paper box, each a 48dp button with a × badge, spoken
 * "Remove ‹name› from palette". Empty, it says "Tap inks below to add them".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BenchInkDraftStrip(draft: PaletteDraft, inks: ZinelyContentInks, kind: BenchVerbKind, actions: BenchInkTrayActions) {
    val colors = ZinelyTheme.v21Colors
    val shape = RoundedCornerShape(ZinelyV21Dimens.radiusMd)
    FlowRow(
        modifier = Modifier
            .padding(top = ZinelyV21Dimens.gapSm)
            .fillMaxWidth()
            .defaultMinSize(minHeight = BenchInkStripBoxMinHeightDp)
            .background(colors.paper, shape)
            .benchDashedBorder(BenchInkPopoverBorderDp, colors.ink, ZinelyV21Dimens.radiusMd)
            .padding(ZinelyV21Dimens.gapXs)
            .testTag(BenchInkDraftStripTestTag)
            .semantics { contentDescription = Copy.BenchInk.INKS_IN_PALETTE },
        horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapXs),
    ) {
        if (draft.inks.isEmpty()) {
            Box(Modifier.height(BenchInkStripEmptyHeightDp), contentAlignment = Alignment.CenterStart) {
                Text(
                    text = Copy.BenchInk.TAP_INKS_TO_ADD,
                    fontFamily = ZinelyV21Fonts.Work,
                    fontSize = BenchInkEmptySp,
                    color = BenchInkPaperSoft,
                    modifier = Modifier.padding(start = ZinelyV21Dimens.gapSm),
                )
            }
        }
        draft.inks.forEachIndexed { i, hex ->
            val remove = benchTap { actions.removeAt(i) }
            Box(
                modifier = Modifier
                    .size(BenchInkTouchDp)
                    .clickable(role = Role.Button, onClick = remove)
                    .testTag(BenchInkDraftPotTestTag)
                    .clearAndSetSemantics {
                        contentDescription = Copy.BenchInk.removeFromPalette(inkName(hex, inks, kind))
                        role = Role.Button
                        onClick { remove(); true }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(BenchInkCompactPotDp)
                        .background(hex.toColorRgba().toComposeColor(), CircleShape)
                        .border(BenchInkSwatchBorderDp, BenchInkPaperInk, CircleShape),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = BenchInkRemoveBadgeInsetDp, end = BenchInkRemoveBadgeInsetDp)
                        .size(BenchInkRemoveBadgeDp)
                        .background(colors.paper, CircleShape)
                        .border(1.dp, BenchInkPaperInk, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = BenchInkRemoveGlyph,
                        fontFamily = ZinelyV21Fonts.Work,
                        fontSize = BenchInkRemoveGlyphSp,
                        lineHeight = BenchInkRemoveGlyphSp,
                        fontWeight = FontWeight.Bold,
                        color = BenchInkPaperInk,
                    )
                }
            }
        }
    }
}

/**
 * An ink as a toggle in edit mode (`.ink-choice[aria-pressed]`, `.picked`): a checkbox whose state is
 * spoken, with a ✓ badge at the pot's top right (`.ink-choice.picked .pot::before`) — not colour alone.
 */
@Composable
private fun BenchInkToggleChoice(
    colour: Color,
    spoken: String,
    picked: Boolean,
    onToggle: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
    potSize: Dp = BenchInkSwatchSizeDp,
    minHeight: Dp = BenchInkChoiceMinHeightDp,
    label: String? = null,
    tabular: Boolean = false,
    takeFocus: (() -> Unit)? = null,
) {
    val colors = ZinelyTheme.v21Colors
    val tap = benchTap(ZinelyHaptic.Snap, onToggle)
    val me = remember { FocusRequester() }
    if (takeFocus != null) {
        LaunchedEffect(Unit) {
            runCatching { me.requestFocus() }
            takeFocus()
        }
    }
    Column(
        modifier = modifier
            .focusRequester(me)
            .defaultMinSize(minHeight = minHeight)
            .toggleable(value = picked, role = Role.Checkbox, onValueChange = { tap() })
            .testTag(tag)
            // One node with no semantic children: Compose publishes a role's platform class (CheckBox) only
            // for a node whose children are all replaced, so the badge and label must not be nodes (ADR-059).
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Checkbox
                toggleableState = ToggleableState(picked)
                onClick { tap(); true }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BenchInkChoiceGapDp, Alignment.CenterVertically),
    ) {
        Box {
            BenchInkSwatchDot(colour, false, potSize, colors.ink)
            if (picked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = BenchInkPickedBadgeOverhangDp, y = -BenchInkPickedBadgeOverhangDp)
                        .size(BenchInkPickedBadgeDp)
                        .background(BenchInkPaperInk, CircleShape)
                        .border(BenchInkPopoverBorderDp, BenchInkBadgeInk, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = BenchInkPickedGlyph,
                        fontFamily = ZinelyV21Fonts.Work,
                        fontSize = BenchInkPickedGlyphSp,
                        lineHeight = BenchInkPickedGlyphSp,
                        fontWeight = FontWeight.Bold,
                        color = BenchInkBadgeInk,
                        modifier = Modifier.clearAndSetSemantics {},
                    )
                }
            }
        }
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
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}

/**
 * `.pe-bar`: Cancel and Save palette (`flex:1.6`), pinned under the scroll with a dashed `--hair` rule
 * above; stacked at largest text. Save looks disabled at 50% until it can save, but stays enabled and
 * focusable, so tapping it says why (`aria-disabled`, not `disabled`), and a screen reader hears the same
 * reason as its state ([benchInkSaveReason], A40 rev. 2 amendment 2026-10-26).
 */
@Composable
private fun BenchInkEditBar(draft: PaletteDraft, actions: BenchInkTrayActions) {
    val canSave = draft.canSave()
    val reason = benchInkSaveReason(draft)
    val colors = ZinelyTheme.v21Colors
    val big = LocalDensity.current.fontScale >= BenchInkLargeTextFontScale
    val bar = Modifier
        .fillMaxWidth()
        .padding(top = ZinelyV21Dimens.gapSm)
        .drawBehind { benchDashedLine(colors.hair, BenchInkPopoverBorderDp.toPx(), y = BenchInkPopoverBorderDp.toPx() / 2f) }
        .padding(vertical = ZinelyV21Dimens.gapMd)
    val cancel: @Composable (Modifier) -> Unit = {
        BenchInkBarButton(Copy.BenchInk.CANCEL, primary = false, dim = false, onClick = actions::cancel, tag = BenchInkCancelTestTag, modifier = it)
    }
    val save: @Composable (Modifier) -> Unit = {
        BenchInkBarButton(Copy.BenchInk.SAVE_PALETTE, primary = true, dim = !canSave, onClick = actions::save, tag = BenchInkSavePaletteTestTag, modifier = it, reason = reason)
    }
    if (big) {
        Column(bar, verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm)) {
            cancel(Modifier.fillMaxWidth())
            save(Modifier.fillMaxWidth())
        }
    } else {
        Row(bar, horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm)) {
            cancel(Modifier.weight(1f))
            save(Modifier.weight(BenchInkGoFlex))
        }
    }
}

/**
 * Why a dim Save cannot save yet (prototype `renderEdit`'s `aria-description`): too few inks first, then no
 * name, in words the edit already shows. `null` once it can save.
 */
internal fun benchInkSaveReason(draft: PaletteDraft): String? = when {
    draft.inks.size < PALETTE_MIN -> Copy.BenchInk.PICK_AT_LEAST_TWO
    draft.name.isBlank() -> Copy.BenchInk.NAME_EMPTY_HELP
    else -> null
}

/**
 * `.pe-bar button` / `.mixbar button`: a 48dp pill on a 2dp printed shadow; `.go` is leaf, `.ghost` surface.
 * [reason], when given, is the platform state description of a dim button (it stays enabled).
 */
@Composable
internal fun BenchInkBarButton(
    text: String,
    primary: Boolean,
    dim: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
    minHeight: Dp = BenchInkTouchDp,
    fontSize: TextUnit = BenchInkBarSp,
    reason: String? = null,
) {
    val colors = ZinelyTheme.v21Colors
    val tap = benchTap(action = onClick)
    Box(
        modifier = modifier
            .graphicsLayer { alpha = if (dim) BenchInkDimAlpha else 1f }
            .zinelyV21HardShadow(BenchInkDoneShadowDp, colors.inkLine, BenchInkPillShape)
            .clip(BenchInkPillShape)
            .background(if (primary) colors.leaf else colors.surface)
            .border(BenchInkPopoverBorderDp, colors.ink, BenchInkPillShape)
            .clickable(role = Role.Button, onClick = tap)
            .defaultMinSize(minHeight = minHeight)
            .padding(horizontal = ZinelyV21Dimens.gapSm)
            .testTag(tag)
            .then(if (reason != null) Modifier.semantics { stateDescription = reason } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontFamily = ZinelyV21Fonts.Work,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = if (primary) colors.onLeaf else colors.ink,
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

// — A40 rev. 2 edit mode and menus (`.pe-*`, `.kept-menu`, `.ink-choice.picked`) —

/** `.kept-menu{box-shadow:3px 3px 0}`; `button{font:600 .8rem}`. */
internal val BenchInkMenuShadowDp = 3.dp
internal val BenchInkMenuSp = 12.8.sp

/** `.pe-count`/`.hexhelp{margin:… 2px 0}`. */
internal val BenchInkHairInsetDp = 2.dp

/** `.pe-name input{font:700 .95rem var(--voice)}`; `:focus{outline-offset:2px}`. */
internal val BenchInkNameSp = 15.2.sp
internal val BenchInkNameRingOffsetDp = 2.dp

/** `.pe-strip{min-height:52px;padding:var(--gap-xs)}`: 44dp inside the padding. */
internal val BenchInkStripBoxMinHeightDp = 52.dp
internal val BenchInkStripEmptyHeightDp = 44.dp

/** `.pe-strip button::after` — the ×: 14dp at `right:3px;top:3px`, `font:700 .7rem/12px`. */
internal val BenchInkRemoveBadgeDp = 14.dp
internal val BenchInkRemoveBadgeInsetDp = 3.dp
internal val BenchInkRemoveGlyphSp = 11.2.sp
internal const val BenchInkRemoveGlyph: String = "×"

/** `.ink-choice.picked .pot::before` — the ✓: 16dp at `right:-6px;top:-6px`, `font:700 .58rem`, `#FFF9DB` on `#27270F`. */
internal val BenchInkPickedBadgeDp = 16.dp
internal val BenchInkPickedBadgeOverhangDp = 6.dp
internal val BenchInkPickedGlyphSp = 9.28.sp
internal val BenchInkBadgeInk = Color(0xFFFFF9DB)
internal const val BenchInkPickedGlyph: String = "✓"

/** `.pe-bar .go{flex:1.6}`, `[aria-disabled]{opacity:.5}`, `button{font:700 .88rem}`. */
internal const val BenchInkGoFlex: Float = 1.6f
internal const val BenchInkDimAlpha: Float = 0.5f
internal val BenchInkBarSp = 14.08.sp


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
