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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.feature.library.LibrarySheetHost
import com.aritr.zinely.feature.library.LibrarySheetSurface
import com.aritr.zinely.ui.a11y.zinelyV2Control
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

/** A35.2: the four papers in the fixed order, written out here and never taken from `PaperSize.entries`. */
internal val ShelfPaperChoices: List<PaperSize> = listOf(PaperSize.A4, PaperSize.LETTER, PaperSize.LEGAL, PaperSize.A5)

/**
 * A35, *Start a zine*: the only place a zine's paper is chosen (A35.1). Four rows, the paper set in About first and
 * then the others in the fixed order (A35.2). **Choosing is the action** (A35.3): one tap starts the zine on that
 * paper; no selected state, no ring, no confirm, no "recommended". Scrim and Back close it and start nothing.
 *
 * Drawn as the Library's own `.sheet` ([LibrarySheetHost] / [LibrarySheetSurface]), which is the paint the frozen
 * `.a35-sheet` restates: the head, its dashed divider, then full-bleed rows with a dashed rule under each.
 *
 * Each row ([StartPaperRow]) is the sheet drawn upright at **one** scale for all four, then the name, the size and
 * one line. The scale was re-based from A4's 74dp on 841.890pt to 56dp on US Legal's 1008pt so the tallest fits
 * (A35.4), so the shipped stocks shrank (A4 52x74 to 33x47).
 *
 * ponytail: the head stays pinned above the scrolling rows, as every Library sheet's does ([LibrarySheetSurface]);
 * the frozen `.a35-sheet` scrolls its head with the rows. At font scale 2 on 360dp the four rows still scroll.
 */
@Composable
internal fun ShelfCreateSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onChoosePaper: (PaperSize) -> Unit,
    preferredPaper: PaperSize = PaperSize.A4,
) {
    LibrarySheetHost(target = Unit.takeIf { visible }, onDismiss = onDismiss) {
        ShelfCreateSheetSurface(onChoosePaper = onChoosePaper, preferredPaper = preferredPaper)
    }
}

/** The sheet's body without its window, for the goldens (a `Dialog`'s window is invisible to the raster). */
@Composable
internal fun ShelfCreateSheetSurface(
    onChoosePaper: (PaperSize) -> Unit,
    preferredPaper: PaperSize = PaperSize.A4,
    modifier: Modifier = Modifier,
) {
    val haptics = ZinelyTheme.haptics
    LibrarySheetSurface(
        title = Copy.Common.START_A_ZINE,
        subtitle = Copy.Shelf.CHOOSE_PAPER_SUB,
        // A35.11: the sheet is a dialog whose title is "Start a zine".
        paneTitle = Copy.Common.START_A_ZINE,
        modifier = modifier.testTag(HomePaperChooserTestTag),
    ) {
        shelfPaperChoices(preferredPaper).forEach { paper ->
            StartPaperRow(paper = paper, onClick = { haptics.perform(ZinelyHaptic.Snap); onChoosePaper(paper) })
        }
        // `.a35-foot{font-size:.76em;text-align:center;margin:var(--gap-md) var(--gap-xl) 0;line-height:1.5}`
        BasicText(
            text = Copy.Shelf.EIGHT_PAGES_FROM_SHEET,
            modifier = Modifier
                .padding(top = ZinelyV21Dimens.gapMd, start = ZinelyV21Dimens.gapXl, end = ZinelyV21Dimens.gapXl)
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

/** The preferred stock first, then the rest of [ShelfPaperChoices] in their fixed order (A35.2). */
internal fun shelfPaperChoices(preferredPaper: PaperSize): List<PaperSize> =
    listOf(preferredPaper) + ShelfPaperChoices.filter { it != preferredPaper }

/**
 * One `.a35-row`: one button, the drawn sheet, then three lines.
 *
 * ```css
 * .a35-row{display:flex;align-items:flex-start;gap:var(--gap-lg);min-height:48px;
 *   border-bottom:1.5px dashed var(--hair);padding:var(--gap-md) var(--gap-xl)}
 * .a35-row:active{background:var(--leaf-tint);color:var(--on-leaf)}
 * ```
 *
 * ONE node, role button, its drawing hidden, its name the row's words with units said in full (A35.11), through
 * the [zinelyV2Control] seam, so the platform gets a named `android.widget.Button` and nothing selected.
 */
@Composable
private fun StartPaperRow(paper: PaperSize, onClick: () -> Unit) {
    val colors = ZinelyTheme.v21Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val hair = colors.hair
    val ink = if (pressed) colors.onLeaf else colors.ink
    val soft = if (pressed) colors.onLeaf else colors.inkSoft
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = RowMinHeight)
            .zinelyFocusRing(focused, ZinelyV21Dimens.radiusSm, (-4).dp)
            .background(if (pressed) colors.leafTint else Color.Transparent)
            .drawBehind {
                val w = RowRule.toPx()
                drawLine(
                    color = hair,
                    start = Offset(0f, size.height - w / 2f),
                    end = Offset(size.width, size.height - w / 2f),
                    strokeWidth = w,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(RowRuleDash.toPx(), RowRuleDash.toPx())),
                )
            }
            .testTag(homePaperChoiceTestTag(paper))
            .zinelyV2Control(label = paper.startSpoken, onClick = onClick, interactionSource = interaction)
            .padding(horizontal = ZinelyV21Dimens.gapXl, vertical = ZinelyV21Dimens.gapMd),
        horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapLg),
        verticalAlignment = Alignment.Top,
    ) {
        // `.a35-slot{width:36px;height:56px;align-items:flex-end;justify-content:center}`: heights compare at a glance.
        Box(Modifier.size(width = StockSlotWidth, height = StockSlotHeight), contentAlignment = Alignment.BottomCenter) {
            PaperStock(paper)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapHair)) {
            BasicText(
                text = paper.choiceName,
                style = TextStyle(
                    color = ink, fontFamily = ZinelyV21Fonts.Work, fontSize = RowNameSize,
                    fontWeight = FontWeight.SemiBold, lineHeight = 1.3.em,
                ),
            )
            BasicText(
                text = paper.choiceDimensions,
                style = TextStyle(
                    color = soft, fontFamily = ZinelyV21Fonts.Work, fontSize = RowLineSize,
                    fontWeight = FontWeight.SemiBold, lineHeight = 1.4.em,
                ),
            )
            BasicText(
                text = paper.startNote,
                style = TextStyle(
                    color = soft, fontFamily = ZinelyV21Fonts.Work, fontSize = RowLineSize,
                    fontWeight = FontWeight.Medium, lineHeight = 1.45.em,
                ),
            )
        }
    }
}

/**
 * `.a35-stock`: the sheet itself, pinned paint in both themes (it is paper, not chrome), its eight pages ruled on it
 * in two columns and four rows. Shared with A35.8's fixed paper at Print.
 */
@Composable
internal fun PaperStock(paper: PaperSize) {
    Box(
        Modifier
            .testTag(homePaperStockTestTag(paper))
            .zinelyV21HardShadow(StockShadow, StockInk, StockShape)
            .clip(StockShape)
            .background(StockPaper)
            .border(StockBorder, StockInk, StockShape)
            .size(width = paper.stockWidth, height = paper.stockHeight)
            .drawBehind {
                val inset = 2.dp.toPx()
                val line = 1.dp.toPx()
                val left = inset
                val right = size.width - inset
                val top = inset
                val bottom = size.height - inset
                drawLine(StockRule, Offset(size.width / 2f, top), Offset(size.width / 2f, bottom), line)
                listOf(0.25f, 0.5f, 0.75f).forEach { f ->
                    val y = top + (bottom - top) * f - line / 2f
                    drawLine(StockRule, Offset(left, y), Offset(right, y), line)
                }
            },
    )
}

/** `.a35-stock`: one scale for all four, 56dp on US Legal's 1008pt (A35.4). Derived, never restated. */
private const val StockDpPerPt: Double = 56.0 / 1008.0

private val StockSlotWidth = 36.dp

/** The tallest stock, derived: [Modifier.size] caps, so a literal that stopped tracking the scale would clamp it. */
private val StockSlotHeight: Dp = ShelfPaperChoices.maxOf { it.stockHeight }

private val PaperSize.stockWidth: Dp get() = (portrait.width * StockDpPerPt).roundToInt().dp
private val PaperSize.stockHeight: Dp get() = (portrait.height * StockDpPerPt).roundToInt().dp

/** The paper's name is [displayName] (`ProofPrint.kt`), one exhaustive `when` for every surface. */
internal val PaperSize.choiceName: String get() = displayName
internal val PaperSize.choiceDimensions: String
    get() = when (this) {
        PaperSize.A4 -> Copy.Paper.A4_DIMENSIONS
        PaperSize.LETTER -> Copy.Paper.LETTER_DIMENSIONS
        PaperSize.LEGAL -> Copy.Paper.LEGAL_DIMENSIONS
        PaperSize.A5 -> Copy.Paper.A5_DIMENSIONS
    }
private val PaperSize.startNote: String
    get() = when (this) {
        PaperSize.A4 -> Copy.Paper.A4_NOTE
        PaperSize.LETTER -> Copy.Paper.LETTER_NOTE
        PaperSize.LEGAL -> Copy.Paper.LEGAL_NOTE
        PaperSize.A5 -> Copy.Paper.A5_NOTE
    }
private val PaperSize.startSpoken: String
    get() = when (this) {
        PaperSize.A4 -> Copy.Paper.A4_SPOKEN
        PaperSize.LETTER -> Copy.Paper.LETTER_SPOKEN
        PaperSize.LEGAL -> Copy.Paper.LEGAL_SPOKEN
        PaperSize.A5 -> Copy.Paper.A5_SPOKEN
    }

// `.a35-row` and `.a35-stock`, transcribed. Sizes in em are the phone's 16px, so sp here.
private val RowMinHeight = 48.dp
private val RowRule = 1.5.dp
private val RowRuleDash = 3.dp

/** `.a35-nm{font-size:.9em}` = 14.4px; `.a35-dm`, `.a35-nt{font-size:.73em}` = 11.68px. */
private val RowNameSize = 14.4.sp
private val RowLineSize = 11.68.sp

/** `.a35-stock{background:#FFF6E8;border:1.5px solid #27270F;box-shadow:2px 2px 0 #27270F}`: px, pinned. */
private val StockPaper = Color(0xFFFFF6E8)
private val StockInk = Color(0xFF27270F)
private val StockRule = Color(0x4727270F)

// ---------------------------------------------------------------------------------------------
// The frozen values these two sheets are drawn from. `.paperseg`/`.paperhint` are `v21-proof.html`;
// `.sheet-ill`, `.start` and the focus ring are `v21-library.html`; `.search` and `.opt .tx span` are
// `v21-bench.html`. Nothing below is a size chosen here.
// ---------------------------------------------------------------------------------------------

/** `.a35-foot{font-size:.76em;line-height:1.5}` = 12.16px. */
private val HintSize = 12.16.sp
private val HintLineHeight = 1.5.em

/** `.a35-stock{border-radius:var(--br-xs);border:1.5px solid #27270F;box-shadow:2px 2px 0 #27270F}` */
private val StockShape: Shape = RoundedCornerShape(ZinelyV21Dimens.radiusXs)
private val StockBorder = 1.5.dp
private val StockShadow = 2.dp

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
    var draft by remember(card.id, visible) { mutableStateOf(titleDraft(card.title)) }

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
                onDraftChange = { draft = it.capped(card.title) },
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
                onClick = { draft = titleDraft(card.title); renaming = true },
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
    draft: TextFieldValue,
    onDraftChange: (TextFieldValue) -> Unit,
    onSave: (String) -> Unit,
    onDone: () -> Unit,
) {
    val haptics = ZinelyTheme.haptics
    val focusRequester = remember { FocusRequester() }
    // `inp.focus(); inp.select();` — the field is the reason the row appeared.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val save = {
        val trimmed = draft.text.trim()
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
 * The bench supply sheet originally froze the corpus's text-input box as `.search`. The 37596 freeze
 * removes that search surface, while this naming field keeps its established box grammar on themed chrome:
 *
 * ```css
 * .search{display:flex;align-items:center;gap:var(--gap-sm);background:var(--surface);
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
    draft: TextFieldValue,
    onDraftChange: (TextFieldValue) -> Unit,
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
                    .background(colors.surface)
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
    var draft by rememberSaveable(visible, title, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(titleDraft(title))
    }
    ZSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = Copy.Shelf.RENAME,
        sub = title,
        // A very long name at large text, with the keyboard up, pushed the field and Save off the sheet.
        subMaxLines = 2,
        modifier = Modifier.testTag(ShelfRenameSheetTestTag),
    ) {
        RenameField(
            draft = draft,
            onDraftChange = { draft = it.capped(title) },
            onSave = onRename,
            onDone = onDismiss,
        )
    }
}

/**
 * `Save` — the rename's one committing action, drawn as `.start` (`v21-library.html:354-366`) minus its
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

/** The longest zine title a person can type, in code points (#157). */
internal const val MaxTitleCodePoints = 60

/**
 * [s] cut to [MaxTitleCodePoints] **code points**, so a surrogate pair (an emoji) is kept or dropped
 * whole, never split into a lone surrogate.
 *
 * The cap governs **user typing only**. `RoomProjectRepository.duplicate` appends its " copy" suffix to
 * an already-capped title and is allowed to exceed 60: truncating there would eat the name the person
 * chose, and nothing downstream assumes a bound (titles are free text, the shelf ellipsises).
 */
internal fun capTitle(s: String, limit: Int = MaxTitleCodePoints): String {
    if (s.codePointCount(0, s.length) <= limit) return s
    // Cut on a grapheme-cluster boundary at or below [limit] code points, so a ZWJ emoji, a flag or a
    // base + combining mark is kept or dropped whole.
    val raw = s.offsetByCodePoints(0, limit)
    val bi = java.text.BreakIterator.getCharacterInstance().apply { setText(s) }
    val end = if (bi.isBoundary(raw)) raw else bi.preceding(raw)
    return s.substring(0, end)
}

/** The rename field's state: the old name, all of it selected, so typing replaces it (#157). */
private fun titleDraft(title: String) = TextFieldValue(title, TextRange(0, title.length))

private fun TextFieldValue.capped(opening: String): TextFieldValue = titleEdit(opening, this)

/**
 * Applies an edit to the title draft. [opening] is the title the sheet **opened with** (from its
 * `title` param, never the draft, so it is constant for the session and survives rotation). The limit
 * is the larger of [MaxTitleCodePoints] and its length, because a title can already be over 60 (a
 * duplicate's " copy"): editing it must not eat its tail, and delete-then-retype must not either — a
 * limit based on the previous draft shrank after the delete and cut the retyped letter. Only growth
 * past the opening length is cut. When a cut happens the selection is clamped to the new length.
 */
internal fun titleEdit(opening: String, next: TextFieldValue): TextFieldValue {
    val limit = maxOf(MaxTitleCodePoints, opening.codePointCount(0, opening.length))
    val c = capTitle(next.text, limit)
    if (c == next.text) return next
    return TextFieldValue(
        c,
        TextRange(next.selection.start.coerceAtMost(c.length), next.selection.end.coerceAtMost(c.length)),
    )
}
