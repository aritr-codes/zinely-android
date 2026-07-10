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
internal const val ShelfSortSheetTestTag: String = "shelf-sort-sheet"

/** The frozen `#sortMenu` options, in the spec's order. */
internal enum class ShelfSort(val menuLabel: String, val buttonLabel: String) {
    /** `data-sort="recent"` — the spec's default. */
    Recent("Recently opened", "Recent"),
    Name("Name (A–Z)", "Name"),
    Oldest("Oldest first", "Oldest"),
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
        title = "Start a zine",
        sub = "Choose your paper. You can print it at home on either.",
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
            text = "Eight pages from one folded sheet.",
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
private val PaperSize.choiceName: String get() = if (this == PaperSize.A4) "A4" else "Letter"
private val PaperSize.choiceDimensions: String
    get() = if (this == PaperSize.A4) "210 × 297 mm" else "8.5 × 11 in"

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
            val focusRequester = remember { FocusRequester() }
            // `inp.focus(); inp.select();` — the field is the reason the row appeared.
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            // `if(z && v){ z.t=v; buzz("snap"); } closeSheets(false);` — a name emptied to nothing is
            // not a rename. The sheet still closes, the zine still has the name it always had, and
            // nothing buzzes: no work happened.
            val save = {
                val trimmed = draft.trim()
                if (trimmed.isNotEmpty()) {
                    haptics.perform(ZinelyHaptic.Snap)
                    onRename(card.id, trimmed)
                }
                onDismiss()
            }
            Row(
                modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 6.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ZTextField(
                    value = draft,
                    onValueChange = { draft = it },
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

        Column {
            ZMenuItem(
                label = "Open on the bench",
                onClick = { onDismiss(); onOpen(card.id) },
                icon = { OpenGlyph(it) },
            )
            ZMenuItem(
                label = "Rename",
                onClick = { draft = card.title; renaming = true },
                icon = { RenameGlyph(it) },
            )
            ZMenuItem(
                label = "Duplicate",
                onClick = { haptics.perform(ZinelyHaptic.Snap); onDismiss(); onDuplicate(card.id) },
                icon = { DuplicateGlyph(it) },
            )
            ZMenuItem(
                label = "Delete",
                onClick = { haptics.perform(ZinelyHaptic.Boundary); onDismiss(); onDelete(card.id) },
                icon = { DeleteGlyph(it) },
                danger = true,
            )
        }
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
            text = "Save",
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
        title = "Sort",
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
