package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.ui.components.ZMenuItem
import com.aritr.zinely.ui.components.ZSheet
import com.aritr.zinely.ui.components.ZTextField
import com.aritr.zinely.ui.components.zinelyFocusRing
import com.aritr.zinely.ui.components.zinelyShadow
import com.aritr.zinely.ui.theme.ZinelyHaptic
import com.aritr.zinely.ui.theme.ZinelyShadowLayer
import com.aritr.zinely.ui.theme.ZinelyTheme

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
 * The two stocks are drawn at their real-ish proportions (A4 ≈ 1:1.41, Letter ≈ 1:1.29), which is
 * the whole point of the chooser: you pick the shape of the object you are about to fold.
 */
@Composable
internal fun ShelfCreateSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onChoosePaper: (PaperSize) -> Unit,
) {
    val haptics = ZinelyTheme.haptics
    ZSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = Copy.Common.START_A_ZINE,
        sub = Copy.Shelf.CHOOSE_PAPER_SUB,
        modifier = Modifier.testTag(HomePaperChooserTestTag),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShelfPaperChoices.forEach { paper ->
                PaperChoice(
                    paper = paper,
                    onClick = { haptics.perform(ZinelyHaptic.Snap); onChoosePaper(paper) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        BasicText(
            text = Copy.Shelf.EIGHT_PAGES_FROM_SHEET,
            modifier = Modifier.padding(top = 14.dp).fillMaxWidth(),
            style = TextStyle(
                color = ZinelyTheme.colors.onDeskSoft,
                fontFamily = ZinelyTheme.typography.shell,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

/** `.paper` — a bordered field tile holding a paper stock, its name, and its real dimensions. */
@Composable
private fun PaperChoice(paper: PaperSize, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ZinelyTheme.colors
    val type = ZinelyTheme.typography
    val shape = RoundedCornerShape(14.dp)
    val interaction = remember { MutableInteractionSource() }
    val stockShadow = remember(colors) {
        listOf(
            ZinelyShadowLayer(dy = 1.dp, blur = 2.dp, color = colors.stamp.copy(alpha = 0.10f)),
            ZinelyShadowLayer(dy = 1.dp, blur = 0.dp, color = colors.paperEdge),
        )
    }
    Column(
        modifier = modifier
            .zinelyFocusRing(interaction, cornerRadius = 14.dp)
            .clip(shape)
            .background(colors.field)
            .border(1.dp, colors.fieldEdge, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .testTag(homePaperChoiceTestTag(paper))
            .padding(horizontal = 14.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .zinelyShadow(stockShadow, RoundedCornerShape(2.dp))
                .clip(RoundedCornerShape(2.dp))
                .background(colors.paper)
                .size(width = paper.stockWidth, height = paper.stockHeight),
        )
        BasicText(
            text = paper.choiceName,
            style = TextStyle(
                color = colors.onDesk,
                fontFamily = type.shell,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        BasicText(
            text = paper.choiceDimensions,
            style = TextStyle(color = colors.onDeskSoft, fontFamily = type.shell, fontSize = 11.5.sp),
        )
    }
}

/** `.paper.a4 .stock{52×74}` / `.paper.letter .stock{56×72}`. */
private val PaperSize.stockWidth: Dp get() = if (this == PaperSize.A4) 52.dp else 56.dp
private val PaperSize.stockHeight: Dp get() = if (this == PaperSize.A4) 74.dp else 72.dp
private val PaperSize.choiceName: String get() = if (this == PaperSize.A4) Copy.Paper.A4 else Copy.Paper.LETTER
private val PaperSize.choiceDimensions: String
    get() = if (this == PaperSize.A4) Copy.Paper.A4_DIMENSIONS else Copy.Paper.LETTER_DIMENSIONS

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
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ZTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .testTag(HomeRenameFieldTestTag),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { save() }),
        )
        RenameSaveButton(onClick = save)
    }
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

/** `.rename .save` — coral-strong, radius 12, min-height 48. Not `.start`: no icon, no lift. */
@Composable
private fun RenameSaveButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .zinelyFocusRing(interaction, cornerRadius = 12.dp)
            .clip(shape)
            .background(ZinelyTheme.colors.coralStrong)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .testTag(HomeRenameConfirmTestTag)
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = Copy.Shelf.SAVE,
            style = TextStyle(
                color = Color.White,
                fontFamily = ZinelyTheme.typography.shell,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
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
