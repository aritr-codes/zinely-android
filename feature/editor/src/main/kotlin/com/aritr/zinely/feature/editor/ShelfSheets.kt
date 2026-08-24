package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.ui.components.ZinelyV21FocusOffsetLibrary
import com.aritr.zinely.ui.components.zinelyFocusRing
import com.aritr.zinely.ui.components.ZMenuItem
import com.aritr.zinely.ui.components.ZSheet
import com.aritr.zinely.ui.components.zinelyV21HardShadow
import com.aritr.zinely.ui.components.zinelyV21Pressable
import com.aritr.zinely.ui.theme.ZinelyHaptic
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.ZinelyV21Press
import kotlin.math.roundToInt

/** Test tags on the two sheets the pre-reskin shelf had no equivalent of. */
internal const val ShelfActionSheetTestTag: String = "shelf-action-sheet"

/** The rename input on its own sheet — the V2 Library's route into the same rename flow. */
internal const val ShelfRenameSheetTestTag: String = "shelf-rename-sheet"
internal const val ShelfSortSheetTestTag: String = "shelf-sort-sheet"

/** The frozen `#sortMenu` options, in the spec's order. */
internal enum class ShelfSort(val menuLabel: String, val buttonLabel: String) {
    /** `data-sort="recent"` — the spec's default. */
    Recent(Copy.Shelf.SORT_RECENT_LONG, Copy.Shelf.SORT_RECENT_SHORT),
    Name(Copy.Shelf.SORT_NAME_LONG, Copy.Shelf.SORT_NAME_SHORT),
    Oldest(Copy.Shelf.SORT_OLDEST_LONG, Copy.Shelf.SORT_OLDEST_SHORT),
}

/** `#createSheet .paper[data-paper]` — A4 leads, as the frozen markup orders them. */
internal val ShelfPaperChoices: List<PaperSize> = listOf(PaperSize.A4, PaperSize.LETTER)

/**
 * `#createSheet` — the create-flow entry. Choosing the paper *is* the create action; there is no
 * confirm step, because the spec's `.paper` click closes the sheet and starts the zine.
 *
 * The two stocks are drawn at their real proportions (A4 ≈ 1:1.41, Letter ≈ 1:1.29) and at one
 * common scale, which is the whole point of the chooser: you pick the shape *and the size* of the
 * object you are about to fold, so the drawing has to be comparable between the two.
 *
 * ### V2.1 restyle: the contents, not the shell
 *
 * **No V2.1 prototype freezes a paper chooser on the Library**, but one of the three freezes a paper
 * chooser: the Proof's print drawer, `.paperseg` (`v21-proof.html:346-352`). That is the same control
 * answering the same question about the same two stocks, so the tiles take its paint (see
 * [PaperChoice]) and the line under them takes `.paperhint`, which is the sentence `.paperseg` itself is
 * captioned with.
 *
 * ⚠ **This paragraph used to say the frame around these objects was still V2** — that `ZSheet` was the V2
 * modal system and the seam was waiting on one conversion. That conversion has since happened: `ZSheet`
 * reads `v21Colors` and `ZinelyV21Fonts` throughout and makes **no** V2 token read at all. The note is
 * kept, inverted, because a KDoc describing finished work as outstanding is worse than none — the next
 * reader schedules a conversion that would find nothing to convert. (Caught by an audit that was told to
 * verify palette reads rather than believe the comment.)
 */
@Composable
internal fun ShelfCreateSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onChoosePaper: (PaperSize) -> Unit,
    preferredPaper: PaperSize = PaperSize.A4,
) {
    val haptics = ZinelyTheme.haptics
    ZSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = Copy.Common.START_A_ZINE,
        sub = Copy.Shelf.CHOOSE_PAPER_SUB,
        modifier = Modifier.testTag(HomePaperChooserTestTag),
    ) {
        // `.paperseg{display:flex;gap:var(--gap-sm)}` — 8dp, where V2's chooser wrote 12.
        Row(horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm)) {
            shelfPaperChoices(preferredPaper).forEach { paper ->
                PaperChoice(
                    paper = paper,
                    onClick = { haptics.perform(ZinelyHaptic.Snap); onChoosePaper(paper) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        // `.paperhint{font-size:.76rem;color:var(--ink-soft);margin:var(--gap-sm) var(--gap-hair) 0;
        // line-height:1.5}` — the frozen caption under the frozen paper chooser, carried across with it.
        BasicText(
            text = Copy.Shelf.EIGHT_PAGES_FROM_SHEET,
            modifier = Modifier
                .padding(top = ZinelyV21Dimens.gapSm, start = HintSideMargin, end = HintSideMargin)
                .fillMaxWidth(),
            style = TextStyle(
                color = ZinelyTheme.v21Colors.inkSoft,
                fontFamily = ZinelyV21Fonts.Work,
                fontSize = HintSize,
                lineHeight = HintLineHeight,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

internal fun shelfPaperChoices(preferredPaper: PaperSize): List<PaperSize> =
    buildList {
        add(preferredPaper)
        addAll(PaperSize.entries.filterNot { it == preferredPaper })
    }

/**
 * One stock, drawn as `.paperseg button` (`v21-proof.html:347-351`) — the corpus's own paper chooser.
 *
 * ```css
 * .paperseg button{flex:1;font-family:var(--sans);font-size:.9rem;font-weight:600;
 *   padding:var(--gap-md) var(--gap-sm);border-radius:var(--br-md);border:1.5px solid var(--ink);
 *   background:var(--paper);color:var(--ink-soft);box-shadow:3px 3px 0 var(--ink-line)}
 * .paperseg button:active{transform:translate(2px,2px);box-shadow:1px 1px 0 var(--ink-line)}
 * ```
 *
 * That is [ZinelyV21Press.Raised] exactly — 3 at rest, 2 of travel, 1 pressed — so the press is the
 * primitive rather than a hand-rolled pair of shadows. `.paperseg`'s selected state
 * (`[aria-pressed="true"]{background:var(--leaf)}`) has nothing to say here: this chooser has no
 * selection, because **choosing is the action** and the sheet is gone the moment one is chosen.
 *
 * ### The drawn stock is `.sheet-ill`, not a V2 field tile
 *
 * The little rectangle is a picture of a sheet of paper, and the corpus draws exactly that in the
 * Library's empty state: `.sheet-ill{background:var(--paper);border:1.5px solid var(--ink);
 * box-shadow:3px 3px 0 var(--ink-line);border-radius:var(--br-xs)}`. It replaces a two-layer blurred
 * shadow over a `--field` fill — V2 machinery for a rendering model V2.1 does not use. Its **geometry is
 * untouched**: both stocks stay derived from [PaperSize.portrait] at [StockDpPerPt], which is the one
 * thing about this tile that is a measurement rather than a paint.
 *
 * ### The second line is `.opt .tx span`
 *
 * `.paperseg button` is a one-line control and this tile has two, so the dimensions take the frozen
 * *secondary line inside a sheet option* (`v21-bench.html`, `.opt .tx span` — `.73rem`, `--ink-soft`)
 * rather than a size invented for the occasion.
 *
 * Selectors, not line numbers, throughout: `v21-bench.html` is itself under amendment, and a citation
 * that drifts is worse than one that has to be grepped.
 *
 * No frame ring: there are two of these and they are equals, and a ring on one of two peers says
 * "recommended" — a product claim this chooser does not make. The rule reserves it for a screen's single
 * primary action anyway.
 */
@Composable
private fun PaperChoice(paper: PaperSize, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ZinelyTheme.v21Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()

    Column(
        modifier = modifier
            // ⚠ Nothing that clips may sit LEFT of the press: its shadow paints outside the node's own
            // bounds, and the focus ring below paints further outside still. The `clip` is downstream.
            .zinelyV21Pressable(pressed, ZinelyV21Press.Raised, colors.inkLine, TileShape)
            .zinelyFocusRing(focused, ZinelyV21Dimens.radiusMd, ZinelyV21FocusOffsetLibrary)
            .clip(TileShape)
            .background(colors.paper)
            .border(TileBorder, colors.ink, TileShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .testTag(homePaperChoiceTestTag(paper))
            .padding(horizontal = ZinelyV21Dimens.gapSm, vertical = ZinelyV21Dimens.gapMd),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapMd),
    ) {
        // The stock sits in a fixed-height slot: the stocks differ in height, and without the slot the
        // name and dimension rows of the two tiles would sit at different heights.
        Box(Modifier.size(width = paper.stockWidth, height = StockSlotHeight)) {
            Box(
                Modifier
                    .testTag(homePaperStockTestTag(paper))
                    .zinelyV21HardShadow(StockShadow, colors.inkLine, StockShape)
                    .clip(StockShape)
                    .background(colors.paper)
                    .border(StockBorder, colors.ink, StockShape)
                    .size(width = paper.stockWidth, height = paper.stockHeight),
            )
        }
        BasicText(
            text = paper.choiceName,
            style = TextStyle(
                color = colors.inkSoft,
                fontFamily = ZinelyV21Fonts.Work,
                fontSize = TileNameSize,
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        BasicText(
            text = paper.choiceDimensions,
            style = TextStyle(
                color = colors.inkSoft,
                fontFamily = ZinelyV21Fonts.Work,
                fontSize = TileDimensionSize,
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
            ),
        )
    }
}

/**
 * `.paper .stock` — **one** scale for both stocks, taken from A4's long edge: `74dp ÷ 841.890pt`.
 *
 * Restating the two sizes as literals is what let them drift apart: Letter's frozen `56×72` was ~4%
 * oversized and inverted the relation it exists to show — Letter is the physically *smaller* sheet.
 * Deriving both from [PaperSize.portrait], the same dimensions the imposition and export use, means
 * a per-stock error is no longer expressible. Rounding to whole dp matches the spec's whole pixels.
 */
private const val StockDpPerPt: Double = 74.0 / 841.890

/**
 * The tallest stock. Both tiles reserve it, so their name and dimension rows stay level.
 *
 * Derived, not restated as `74.dp`: [Modifier.size] *caps* the child, so a literal that stopped
 * tracking the scale would silently clamp the taller stock rather than fail — and the tiles would
 * still be the same height, which is the only thing the slot's own test can see.
 */
private val StockSlotHeight: Dp = ShelfPaperChoices.maxOf { it.stockHeight }

/** `.paper.a4 .stock{52×74}` / `.paper.letter .stock{54×70}`, both derived, never restated. */
private val PaperSize.stockWidth: Dp get() = (portrait.width * StockDpPerPt).roundToInt().dp
private val PaperSize.stockHeight: Dp get() = (portrait.height * StockDpPerPt).roundToInt().dp
private val PaperSize.choiceName: String get() = if (this == PaperSize.A4) Copy.Paper.A4 else Copy.Paper.LETTER
private val PaperSize.choiceDimensions: String
    get() = if (this == PaperSize.A4) Copy.Paper.A4_DIMENSIONS else Copy.Paper.LETTER_DIMENSIONS

// ---------------------------------------------------------------------------------------------
// The frozen values these two sheets are drawn from. `.paperseg`/`.paperhint` are `v21-proof.html`;
// `.sheet-ill`, `.start` and the focus ring are `v21-library.html`; `.search` and `.opt .tx span` are
// `v21-bench.html`. Nothing below is a size chosen here.
// ---------------------------------------------------------------------------------------------

/** `.paperhint{font-size:.76rem;margin:var(--gap-sm) var(--gap-hair) 0;line-height:1.5}` = 12.16px. */
private val HintSize = 12.16.sp
private val HintLineHeight = 1.5.em
private val HintSideMargin = ZinelyV21Dimens.gapHair

/** `.paperseg button{border-radius:var(--br-md);border:1.5px solid var(--ink);font-size:.9rem}` */
private val TileShape: Shape = RoundedCornerShape(ZinelyV21Dimens.radiusMd)
private val TileBorder = 1.5.dp
private val TileNameSize = 14.4.sp

/** `.opt .tx span{font-size:.73rem;color:var(--ink-soft)}` = 11.68px — a sheet option's second line. */
private val TileDimensionSize = 11.68.sp

/** `.sheet-ill{border-radius:var(--br-xs);border:1.5px solid var(--ink);box-shadow:3px 3px 0}` */
private val StockShape: Shape = RoundedCornerShape(ZinelyV21Dimens.radiusXs)
private val StockBorder = 1.5.dp
private val StockShadow = 3.dp

/** `.search{border-radius:var(--br-pill);border:1.5px solid var(--ink);box-shadow:2px 2px 0}` */
private val FieldShape: Shape = RoundedCornerShape(ZinelyV21Dimens.radiusPill)
private val FieldBorder = 1.5.dp
private val FieldShadow = 2.dp

/** `.sh-ttl{font-size:1.22rem;line-height:1.15}` = 19.52/22.448px — a zine's name, in the voice face. */
private val FieldTextSize = 19.52.sp
private val FieldLineHeight = 22.448.sp

/** D-009's touch floor, which a text field can only meet with its drawn box. Not a frozen value. */
private val FieldMinHeight = 48.dp
private val SaveMinHeight = 48.dp

/** `.start{font-size:1rem;font-weight:700;border:1.5px solid var(--ink);border-radius:var(--br-pill)}` */
private val SaveShape: Shape = RoundedCornerShape(ZinelyV21Dimens.radiusPill)
private val SaveBorder = 1.5.dp
private val SaveTextSize = 16.sp

/** `.start:focus-visible{outline:2px solid var(--ink);outline-offset:5px}`. */

/**
 * `#actionSheet` — one zine's actions, titled with its name.
 *
 * Rename happens **inside** the sheet, as an inline field the Rename item reveals, never a second
 * dialog stacked on the first. The field owns its own draft text: it is scratch state that dies with
 * the sheet, and the spec resets it (`$("#rename").classList.remove("on")`) on every open.
 *
 * Deliberately absent: the spec's `Share…` item. `ProjectRepository` exposes no share action, and
 * inventing one is a product change rather than a reskin — a menu item that does nothing is worse
 * than an honest omission. Recorded as an M2 deferral alongside its glyph (see [ShelfGlyph]).
 */
@Composable
internal fun ShelfActionSheet(
    visible: Boolean,
    card: HomeZineCard?,
    onDismiss: () -> Unit,
    onOpen: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDuplicate: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    if (card == null) return
    val haptics = ZinelyTheme.haptics
    var renaming by remember(card.id, visible) { mutableStateOf(false) }
    var draft by remember(card.id, visible) { mutableStateOf(card.title) }

    ZSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = card.title,
        sub = card.editedLabel,
        modifier = Modifier.testTag(ShelfActionSheetTestTag),
    ) {
        if (renaming) {
            RenameField(
                draft = draft,
                onDraftChange = { draft = it },
                onSave = { trimmed -> onRename(card.id, trimmed) },
                onDone = onDismiss,
            )
        }

        Column {
            ZMenuItem(
                label = Copy.Shelf.OPEN_ON_THE_BENCH,
                onClick = { onDismiss(); onOpen(card.id) },
                icon = { OpenGlyph(it) },
            )
            ZMenuItem(
                label = Copy.Shelf.RENAME,
                onClick = { draft = card.title; renaming = true },
                icon = { RenameGlyph(it) },
            )
            ZMenuItem(
                label = Copy.Shelf.DUPLICATE,
                onClick = { haptics.perform(ZinelyHaptic.Snap); onDismiss(); onDuplicate(card.id) },
                icon = { DuplicateGlyph(it) },
            )
            ZMenuItem(
                label = Copy.Shelf.DELETE,
                onClick = { haptics.perform(ZinelyHaptic.Boundary); onDismiss(); onDelete(card.id) },
                icon = { DeleteGlyph(it) },
                danger = true,
            )
        }
    }
}

/**
 * The rename input itself — the field, its Done key and its Save button, as one thing.
 *
 * Extracted so the V2 Library can raise **this** surface rather than a second one of its own. The
 * [D-025 ruling](../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-025-ruling) is *"reuse existing
 * behaviour; invent no new product concept"*, and a re-implemented field would be a new concept wearing
 * the old one's name — same shape, different empty-name rule, different haptic, different test tags.
 *
 * @param onSave called only with a **non-blank, trimmed** title. A name emptied to nothing is not a
 *   rename: the surface still closes, the zine keeps the name it always had, and nothing buzzes, because
 *   no work happened.
 * @param onDone the surface should close, whether or not anything was saved.
 */
@Composable
private fun RenameField(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSave: (String) -> Unit,
    onDone: () -> Unit,
) {
    val haptics = ZinelyTheme.haptics
    val focusRequester = remember { FocusRequester() }
    // `inp.focus(); inp.select();` — the field is the reason the row appeared.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val save = {
        val trimmed = draft.trim()
        if (trimmed.isNotEmpty()) {
            haptics.perform(ZinelyHaptic.Snap)
            onSave(trimmed)
        }
        onDone()
    }
    Row(
        modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 6.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RenameInput(
            draft = draft,
            onDraftChange = onDraftChange,
            modifier = Modifier.weight(1f).focusRequester(focusRequester),
            onImeDone = save,
        )
        RenameSaveButton(onClick = save)
    }
}

/**
 * The field itself, in V2.1.
 *
 * **The corpus freezes exactly one text input**, the bench supply sheet's `.search`
 * (`v21-bench.html`), so that is the box:
 *
 * ```css
 * .search{display:flex;align-items:center;gap:var(--gap-sm);background:var(--paper);
 *   border:1.5px solid var(--ink);border-radius:var(--br-pill);
 *   padding:var(--gap-sm) var(--gap-md);box-shadow:2px 2px 0 var(--ink-line)}
 * .search input{border:0;background:none;font-family:var(--sans);color:var(--ink)}
 * ```
 *
 * ### The type is `.sh-ttl`'s, not `.search input`'s, and that is deliberate
 *
 * `.search input` is `--sans` at `.86rem`, because what you type into it is a *query*. What you type into
 * this one is a **zine's name**, and every zine name in the corpus — `.sh-ttl`, the shelf's `.nm`, the
 * cover's own mark — is set in the voice face. Taking the search field's 13.76sp sans here would be a
 * faithful transcription of the wrong selector, and would also shrink the V1 field it replaces (17sp
 * voice) at the one moment the user is checking their own spelling. So: `.search`'s box, `.sh-ttl`'s
 * type (Averia 700 at 19.52sp). Recorded as a composed reading rather than a single transcription.
 *
 * ### What is kept exactly
 *
 * `singleLine`, the Done IME action and its action, the focus requester, the test tag, and the 48dp
 * minimum height — the D-009 touch floor, which for a text field is the drawn box itself and so cannot be
 * met by Compose's pointer-input minimum the way a 44dp icon button's is. `.search` declares no height;
 * dropping the floor to match it would be a regression dressed as transcription.
 *
 * The 2dp shadow is [ZinelyV21Press.Flat]'s rest offset, taken as a plain hard shadow: a field is not
 * pressed, so nothing here moves.
 */
@Composable
private fun RenameInput(
    draft: String,
    onDraftChange: (String) -> Unit,
    onImeDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ZinelyTheme.v21Colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    BasicTextField(
        value = draft,
        onValueChange = onDraftChange,
        // The name is on the field, not on the sheet's heading above it: focus lands here, and a
        // `BasicTextField` with a `decorationBox` and no placeholder publishes nothing of its own. Set
        // through `semantics` rather than `clearAndSetSemantics` so the editable text, the selection and
        // the IME action all survive — clearing them would trade a missing label for a broken field.
        modifier = modifier
            .testTag(HomeRenameFieldTestTag)
            .semantics { contentDescription = Copy.Shelf.RENAME_FIELD },
        textStyle = TextStyle(
            color = colors.ink,
            fontFamily = ZinelyV21Fonts.Voice,
            fontSize = FieldTextSize,
            fontWeight = FontWeight.Bold,
            // `.sh-ttl` **declares** `line-height:1.15`, so it does not inherit — the field's own height
            // follows the type it borrowed, not the corpus's 1.55 running-text default. Left inherited,
            // this ran the box ~8dp taller than the reading it claims. Caught on review.
            lineHeight = FieldLineHeight,
        ),
        cursorBrush = SolidColor(colors.ink),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onImeDone() }),
        interactionSource = interaction,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .zinelyV21HardShadow(FieldShadow, colors.inkLine, FieldShape)
                    // A CSS `outline` grows outward; a `border` sits inside the box. Both are here, and
                    // they are not the same object — the ring appears only while the field holds focus,
                    // which is the state the V1 field signalled by re-colouring its border. Re-colouring
                    // this one would fight the language's single ink.
                    .zinelyFocusRing(focused, ZinelyV21Dimens.radiusPill, ZinelyV21FocusOffsetLibrary)
                    .clip(FieldShape)
                    .background(colors.paper)
                    .border(FieldBorder, colors.ink, FieldShape)
                    .defaultMinSize(minHeight = FieldMinHeight)
                    .padding(horizontal = ZinelyV21Dimens.gapMd, vertical = ZinelyV21Dimens.gapSm),
                contentAlignment = Alignment.CenterStart,
            ) {
                innerTextField()
            }
        },
    )
}

/**
 * The rename input on its own sheet — the V2 Library's rename, which is V1's rename.
 *
 * V1 reveals the field *inside* the action sheet, because that sheet's rows and the field are one
 * surface. V2's action sheet is B3's frozen `.sheet` and has no room the field could be revealed in
 * without redesigning a frozen component, so the same field arrives on its own sheet instead. **The flow
 * is unchanged** — [RenameField]'s rules, `HomeViewModel.rename`'s trimming, the same two test tags —
 * and the V1 chrome on a V2 screen is the seam [ADR-086](../../../../../../../docs/DECISIONS.md#adr-086)
 * records as an accepted Known Limitation rather than a defect.
 *
 * @param title the zine's current name, which is what the field opens with.
 * @param onRename the new, trimmed, non-blank title. The id is the caller's to remember.
 */
@Composable
internal fun ShelfRenameSheet(
    visible: Boolean,
    title: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
) {
    // Keyed on `title` as well as `visible` so reopening the sheet for a different zine — or for the same
    // one after a rename — starts from the name that zine actually has, never a stale draft.
    var draft by remember(visible, title) { mutableStateOf(title) }
    ZSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = Copy.Shelf.RENAME,
        sub = title,
        modifier = Modifier.testTag(ShelfRenameSheetTestTag),
    ) {
        RenameField(
            draft = draft,
            onDraftChange = { draft = it },
            onSave = onRename,
            onDone = onDismiss,
        )
    }
}

/**
 * `Save` — the rename's one committing action, drawn as `.start` (`v21-library.html:328-341`) minus its
 * ring.
 *
 * ```css
 * .start{font-family:var(--sans);font-size:1rem;font-weight:700;background:var(--leaf);
 *   color:var(--on-leaf);border:1.5px solid var(--ink);border-radius:var(--br-pill);
 *   padding:var(--gap-lg) var(--gap-xl);
 *   box-shadow:var(--hard) var(--hard) 0 var(--ink-line), 0 0 0 var(--frame) var(--butter)}
 * .start:active{transform:translate(2px,2px);box-shadow:1px 1px 0 var(--ink-line), …}
 * .start:focus-visible{outline:2px solid var(--ink);outline-offset:5px}
 * ```
 *
 * `4 / 2 / 1` is [ZinelyV21Press.Hero], the tier the corpus gives its filled primaries — where V2 gave
 * this button `coralStrong` under white and no depth at all.
 *
 * ### ⚠ No `--frame` ring, and the reason is the screen behind it
 *
 * The ring is reserved for **one** primary action per screen. This sheet is modal over the Library, whose
 * own `.start` is still standing in the dock a scrim's thickness away and is the ring's owner there; a
 * second one rising over it would leave two on the same display and make both of them decoration. The
 * leaf fill, the ink border and the Hero tier already say *primary* without spending the one mark that
 * only works while it is rare. Recorded, because "`.start` minus a shadow layer" is exactly the kind of
 * omission a later reader restores as a bug fix.
 *
 * `on-leaf` for the label — the pairing the contrast gate measured — where V2 wrote a flat white.
 *
 * ### The padding is `.start`'s, and the 48dp floor is a floor
 *
 * `padding:var(--gap-lg) var(--gap-xl)` is 16 vertical and **24 horizontal**; an earlier draft of this
 * button wrote 16 in both, which is the frozen vertical value quietly substituted into the horizontal
 * slot — caught on review. [SaveMinHeight] stays as well, and it is deliberately a floor rather than the
 * height: 16 + 16 + a ~19sp line already clears 48, so it binds only if a future type change would drop
 * this control under the D-009 target. That is the one value here the frozen file does not write.
 */
@Composable
private fun RenameSaveButton(onClick: () -> Unit) {
    val colors = ZinelyTheme.v21Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            // Nothing that clips may sit LEFT of the press or the ring — both paint outside the bounds.
            .zinelyV21Pressable(pressed, ZinelyV21Press.Hero, colors.inkLine, SaveShape)
            .zinelyFocusRing(focused, ZinelyV21Dimens.radiusPill, ZinelyV21FocusOffsetLibrary)
            .clip(SaveShape)
            .background(colors.leaf)
            .border(SaveBorder, colors.ink, SaveShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .testTag(HomeRenameConfirmTestTag)
            .defaultMinSize(minHeight = SaveMinHeight)
            .padding(horizontal = ZinelyV21Dimens.gapXl, vertical = ZinelyV21Dimens.gapLg),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = Copy.Shelf.SAVE,
            style = TextStyle(
                color = colors.onLeaf,
                fontFamily = ZinelyV21Fonts.Work,
                fontSize = SaveTextSize,
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}


/**
 * `#sortSheet` — three radios. Selection is carried by the check glyph and the label's weight, never
 * by coral: the spec is explicit that colour does not encode state here.
 */
@Composable
internal fun ShelfSortSheet(
    visible: Boolean,
    selected: ShelfSort,
    onDismiss: () -> Unit,
    onSelect: (ShelfSort) -> Unit,
) {
    val haptics = ZinelyTheme.haptics
    ZSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = Copy.Shelf.SORT,
        modifier = Modifier.testTag(ShelfSortSheetTestTag),
    ) {
        ShelfSort.entries.forEach { sort ->
            ZMenuItem(
                label = sort.menuLabel,
                onClick = {
                    haptics.perform(ZinelyHaptic.Tick)
                    onDismiss()
                    onSelect(sort)
                },
                selected = sort == selected,
            )
        }
    }
}
