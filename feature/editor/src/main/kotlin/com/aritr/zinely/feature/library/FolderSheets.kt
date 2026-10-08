package com.aritr.zinely.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.a11y.zinelyV2Control
import com.aritr.zinely.ui.components.zinelyFocusRing
import com.aritr.zinely.ui.components.zinelyV21Pressable
import com.aritr.zinely.ui.theme.ZinelyHaptic
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.ZinelyV21Press
import java.util.Locale

// The three sheets the A28 folders amendment adds to the Library (`v21-library.html`,
// [ADR-125](docs/DECISIONS.md#adr-125)): the chooser, the name sheet and the folder's own sheet. All three
// are `.sheet` with `.act` rows, in [LibrarySheetHost]; what is new is in this file. Each reports the
// choice and holds still, as the zine sheet does: closing is the screen's.

/** `#moveSheet`, `#nameSheet`, `#pileSheet`. */
internal const val FolderMoveSheetTestTag: String = "folder-move-sheet"
internal const val FolderNameSheetTestTag: String = "folder-name-sheet"
internal const val FolderActionsSheetTestTag: String = "folder-actions-sheet"

/** The chooser's rows: My Shelf, one per folder by its name, and New folder. */
internal const val FolderMoveToShelfTestTag: String = "folder-move-to-shelf"
internal fun folderMoveRowTestTag(folder: String): String = "folder-move-to-$folder"
internal const val FolderMoveNewTestTag: String = "folder-move-new"

/** The name sheet's field, the line under it, and its two buttons. */
internal const val FolderNameFieldTestTag: String = "folder-name-field"
internal const val FolderNameHintTestTag: String = "folder-name-hint"
internal const val FolderNameGoTestTag: String = "folder-name-go"

/** The folder sheet's three rows. */
internal const val FolderOpenTestTag: String = "folder-open"
internal const val FolderRenameTestTag: String = "folder-rename"
internal const val FolderUnpackTestTag: String = "folder-unpack"

/**
 * Who the chooser is for.
 *
 * @property current the folder the zine is in now, or `null` on My Shelf.
 * @property folders every folder on the Shelf with its count, in the order their piles stand.
 */
internal data class FolderMoveTarget(val title: String, val current: String?, val folders: Map<String, Int>)

/**
 * `#moveSheet` — where a zine can go.
 *
 * *My Shelf* is offered only to a zine that is in a folder, where it is the way out. The folder the zine is
 * already in is listed, because leaving it out would make the list look as if that folder had gone, but it
 * is not a choice ([SheetRow]'s `here`). *New folder* is always last.
 *
 * @param onMove a row was chosen: a folder's name, or `null` for My Shelf.
 */
@Composable
internal fun FolderMoveSheet(
    target: FolderMoveTarget?,
    onMove: (String?) -> Unit,
    onNewFolder: () -> Unit,
    onDismiss: () -> Unit,
) {
    LibrarySheetHost(target, onDismiss) { drawn -> FolderMoveSheetSurface(drawn, onMove, onNewFolder) }
}

/** [FolderMoveSheet]'s body, without the window that makes it modal: what a parity raster composes. */
@Composable
internal fun FolderMoveSheetSurface(drawn: FolderMoveTarget, onMove: (String?) -> Unit, onNewFolder: () -> Unit) {
    LibrarySheetSurface(
        title = drawn.title,
        subtitle = drawn.current?.let(Copy.Folders::inFolderMoveTo) ?: Copy.Folders.ON_MY_SHELF_MOVE_TO,
        paneTitle = Copy.Folders.MOVE_PANE,
        modifier = Modifier.testTag(FolderMoveSheetTestTag),
        scrolls = true,
    ) {
        if (drawn.current != null) {
            SheetRow(
                label = Copy.Folders.MY_SHELF,
                onClick = { onMove(null) },
                modifier = Modifier.testTag(FolderMoveToShelfTestTag),
                icon = SheetIcon.Shelf,
                note = Copy.Folders.OUT_OF_THE_FOLDER,
            )
        }
        drawn.folders.forEach { (folder, count) ->
            val here = folder == drawn.current
            SheetRow(
                label = folder,
                onClick = { onMove(folder) },
                modifier = Modifier.testTag(folderMoveRowTestTag(folder)),
                icon = SheetIcon.Pile,
                note = if (here) Copy.Folders.ITS_HERE else pluralZineCount(count),
                here = here,
            )
        }
        SheetRow(
            label = Copy.Folders.NEW_FOLDER,
            onClick = onNewFolder,
            modifier = Modifier.testTag(FolderMoveNewTestTag),
            glyph = StartPlusGlyph,
            note = "",
        )
    }
}

/**
 * What the name sheet is naming: a new folder for one zine, or an existing folder.
 *
 * @property renaming the folder being renamed, or `null` for *New folder*.
 * @property zineTitle *New folder* only: the zine it is for.
 * @property zineFolder *New folder* only: the folder that zine is in now.
 * @property count *Rename folder* only: how many zines it holds.
 */
internal data class FolderNameTarget(
    val renaming: String? = null,
    val zineTitle: String = "",
    val zineFolder: String? = null,
    val count: Int = 0,
)

/**
 * `#nameSheet` — *New folder* and *Rename folder* (A28.10).
 *
 * ```css
 * .field{padding:var(--gap-lg) var(--gap-xl) 0;display:flex;flex-direction:column;gap:var(--gap-sm)}
 * .field label{font:700 .7rem/1.2 var(--sans);letter-spacing:.1em;text-transform:uppercase;color:var(--ink-soft)}
 * .field input{font:500 1rem/1.3 var(--sans);color:#27270F;background:#FFF6E8;
 *   border:1.5px solid var(--ink);border-radius:var(--br-sm);padding:var(--gap-md);min-height:48px;width:100%}
 * .field input:focus-visible{outline:2px solid var(--ink);outline-offset:2px}
 * .field .hint{min-height:1.3em;margin:0;font-size:.78rem;color:var(--ink-soft);font-weight:500}
 * .sheet-btns{display:flex;flex-wrap:wrap;justify-content:flex-end;align-items:center;
 *   gap:var(--gap-sm) var(--gap-lg);padding:var(--gap-md) var(--gap-xl) 0}
 * ```
 *
 * The sheet decides nothing about names. [check] says what the typed text stands for and
 * [newFolderAnswer] / [renameFolderAnswer] turn that into the button and the line under the field, so the
 * rules have one home each and both are tested without a screen.
 *
 * **The field is paper**, pinned with its ink, so typed words read the same in both themes.
 *
 * **Forty characters, as a reader counts them.** The frozen field says `maxlength="40"`, which counts
 * UTF-16 units; the app counts what [check] counts (A28.10, stated). Text past the limit is not kept.
 *
 * **Enter does what the button does, and nothing more:** while the button cannot be pressed, Enter does
 * nothing.
 *
 * @param keys each folder on the Shelf by its key ([FolderNameVerdict.Name.key]).
 * @param onGo the button was pressed, with the name to use: the folder's existing spelling when the zine
 *   is joining one.
 * @param onCancel *Cancel*. Not [onDismiss]: from the chooser, Cancel goes back to the chooser (A28.14).
 */
@Composable
internal fun FolderNameSheet(
    target: FolderNameTarget?,
    keys: Map<String, String>,
    check: (String) -> FolderNameVerdict,
    onGo: (String) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    LibrarySheetHost(target, onDismiss) { drawn -> FolderNameSheetSurface(drawn, keys, check, onGo, onCancel) }
}

/**
 * [FolderNameSheet]'s body, without the window that makes it modal.
 *
 * @param initial what the field opens holding, when it is not the folder's own name: a parity raster's way
 *   to draw the sheet with something typed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FolderNameSheetSurface(
    drawn: FolderNameTarget,
    keys: Map<String, String>,
    check: (String) -> FolderNameVerdict,
    onGo: (String) -> Unit,
    onCancel: () -> Unit,
    initial: String = drawn.renaming.orEmpty(),
) {
    val colors = ZinelyTheme.v21Colors
    val haptics = ZinelyTheme.haptics
    val renaming = drawn.renaming
    val title = if (renaming == null) Copy.Folders.NEW_FOLDER else Copy.Folders.RENAME_FOLDER

    var draft by remember(drawn) {
        mutableStateOf(TextFieldValue(initial, TextRange(initial.length)))
    }
    val verdict = check(draft.text)
    val answer = if (renaming == null) {
        newFolderAnswer(verdict, keys, drawn.zineFolder)
    } else {
        renameFolderAnswer(verdict, keys, renaming)
    }
    val go = {
        val name = answer.name
        if (answer.enabled && name != null) {
            haptics.perform(ZinelyHaptic.Snap)
            onGo(name)
        }
    }

    LibrarySheetSurface(
        title = title,
        subtitle = if (renaming == null) Copy.Folders.forZine(drawn.zineTitle) else pluralZineCount(drawn.count),
        paneTitle = title,
        modifier = Modifier.testTag(FolderNameSheetTestTag),
        scrolls = true,
    ) {
        Column(
            Modifier.padding(
                start = ZinelyV21Dimens.gapXl,
                top = ZinelyV21Dimens.gapLg,
                end = ZinelyV21Dimens.gapXl,
            ),
            verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
        ) {
            Text(
                text = Copy.Folders.FOLDER_NAME.uppercase(Locale.ROOT),
                style = TextStyle(
                    fontFamily = ZinelyV21Fonts.Work,
                    fontWeight = FontWeight.Bold,
                    fontSize = FieldLabelSize,
                    lineHeight = FieldLabelSize * 1.2f,
                    letterSpacing = 0.1.em,
                    color = colors.inkSoft,
                ),
                // The `<label for>`: the field carries these words as its name, so the capitals above
                // it are not a second thing to hear.
                modifier = Modifier.clearAndSetSemantics { },
            )
            val interaction = remember { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            val focusRequester = remember { FocusRequester() }
            // `show()` puts focus in the field: it is the reason the sheet rose.
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            BasicTextField(
                value = draft,
                onValueChange = { typed ->
                    val stands = check(typed.text)
                    draft = if (stands is FolderNameVerdict.Name && stands.cut) {
                        TextFieldValue(stands.name, TextRange(stands.name.length))
                    } else {
                        typed
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag(FolderNameFieldTestTag)
                    .semantics { contentDescription = Copy.Folders.FOLDER_NAME },
                textStyle = TextStyle(
                    fontFamily = ZinelyV21Fonts.Work,
                    fontWeight = FontWeight.Medium,
                    fontSize = FieldTextSize,
                    lineHeight = FieldTextSize * 1.3f,
                    color = FieldInk,
                ),
                cursorBrush = SolidColor(FieldInk),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { go() }),
                interactionSource = interaction,
                decorationBox = { innerTextField ->
                    Box(
                        Modifier
                            .zinelyFocusRing(focused, ZinelyV21Dimens.radiusSm, FieldFocusOffset)
                            .clip(FieldShape)
                            .background(FieldPaper)
                            .border(FieldBorder, colors.ink, FieldShape)
                            .defaultMinSize(minHeight = FieldMinHeight)
                            .padding(ZinelyV21Dimens.gapMd),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        innerTextField()
                    }
                },
            )
            Text(
                text = answer.hint,
                style = TextStyle(
                    fontFamily = ZinelyV21Fonts.Work,
                    fontWeight = FontWeight.Medium,
                    fontSize = HintSize,
                    lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                    color = colors.inkSoft,
                ),
                modifier = Modifier
                    .testTag(FolderNameHintTestTag)
                    // `aria-live="polite"`: spoken when it changes. The description is absent, not
                    // empty, while there is nothing to say, or the empty line would be a place for
                    // TalkBack to stop (the Bench's snack found this out).
                    .clearAndSetSemantics {
                        liveRegion = LiveRegionMode.Polite
                        if (answer.hint.isNotEmpty()) contentDescription = answer.hint
                    },
            )
        }
        FlowRow(
            Modifier
                .fillMaxWidth()
                .padding(start = ZinelyV21Dimens.gapXl, top = ZinelyV21Dimens.gapMd, end = ZinelyV21Dimens.gapXl),
            horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapLg, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            QuietAction(
                ZineDockSecondaryAction(
                    label = Copy.Folders.CANCEL,
                    onClick = { haptics.perform(ZinelyHaptic.Tick); onCancel() },
                ),
            )
            GoButton(label = answer.button, enabled = answer.enabled, onClick = go)
        }
    }
}

/**
 * `.go` — the name sheet's one committing button.
 *
 * ```css
 * .go{font:700 .94rem/1.2 var(--sans);background:var(--leaf);color:var(--on-leaf);border:1.5px solid var(--ink);
 *   border-radius:var(--br-lg);padding:var(--gap-md) var(--gap-xl);min-height:48px;
 *   max-width:100%;overflow-wrap:anywhere;text-align:center;box-shadow:3px 3px 0 var(--ink-line)}
 * .go:active{transform:translate(2px,2px);box-shadow:1px 1px 0 var(--ink-line)}
 * .go:disabled{opacity:.45;cursor:default;transform:none;box-shadow:3px 3px 0 var(--ink-line)}
 * .go:focus-visible{outline:2px solid var(--ink);outline-offset:3px}
 * ```
 *
 * `3 / 2 / 1` is [ZinelyV21Press.Raised]. Its words change with what is typed (*Make folder*, *Move to
 * "Family"*), and a folder's name can be forty characters, so the label wraps inside the button.
 */
@Composable
private fun GoButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = ZinelyTheme.v21Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    Box(
        Modifier
            .alpha(if (enabled) 1f else GoDisabledOpacity)
            // Nothing that clips may sit before the press or the ring: both paint outside the bounds.
            .zinelyV21Pressable(pressed && enabled, ZinelyV21Press.Raised, colors.inkLine, GoShape)
            .zinelyFocusRing(focused, ZinelyV21Dimens.radiusLg, GoFocusOffset)
            .clip(GoShape)
            .background(colors.leaf)
            .border(FieldBorder, colors.ink, GoShape)
            .testTag(FolderNameGoTestTag)
            .zinelyV2Control(label = label, enabled = enabled, interactionSource = interaction, onClick = onClick)
            .defaultMinSize(minHeight = FieldMinHeight)
            .padding(horizontal = ZinelyV21Dimens.gapXl, vertical = ZinelyV21Dimens.gapMd),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontFamily = ZinelyV21Fonts.Work,
                fontWeight = FontWeight.Bold,
                fontSize = GoTextSize,
                lineHeight = GoTextSize * 1.2f,
                color = colors.onLeaf,
            ),
        )
    }
}

/** Which folder its sheet is open for, and how many zines it holds. */
internal data class FolderActionsTarget(val name: String, val count: Int)

/**
 * `#pileSheet` — what can be done to a folder: look inside, rename it, or take its zines out.
 *
 * There is no "delete folder": a folder exists only while it holds a zine, so taking the zines out is how
 * one goes away, and the row's second line says where they go.
 */
@Composable
internal fun FolderActionsSheet(
    target: FolderActionsTarget?,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onUnpack: () -> Unit,
    onDismiss: () -> Unit,
) {
    LibrarySheetHost(target, onDismiss) { drawn -> FolderActionsSheetSurface(drawn, onOpen, onRename, onUnpack) }
}

/** [FolderActionsSheet]'s body, without the window that makes it modal. */
@Composable
internal fun FolderActionsSheetSurface(
    drawn: FolderActionsTarget,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onUnpack: () -> Unit,
) {
    LibrarySheetSurface(
        title = drawn.name,
        subtitle = Copy.Folders.folderSubtitle(pluralZineCount(drawn.count)),
        paneTitle = Copy.Folders.FOLDER_ACTIONS,
        modifier = Modifier.testTag(FolderActionsSheetTestTag),
        scrolls = true,
    ) {
        SheetRow(
            label = Copy.Folders.OPEN_FOLDER,
            onClick = onOpen,
            modifier = Modifier.testTag(FolderOpenTestTag),
            glyph = ZineAction.Open.glyph,
        )
        SheetRow(
            label = Copy.Folders.RENAME_FOLDER,
            onClick = onRename,
            modifier = Modifier.testTag(FolderRenameTestTag),
            glyph = ZineAction.Rename.glyph,
        )
        SheetRow(
            label = Copy.Folders.TAKE_THE_ZINES_OUT,
            onClick = onUnpack,
            modifier = Modifier.testTag(FolderUnpackTestTag),
            icon = SheetIcon.Pile,
            note = Copy.Folders.TAKE_OUT_NOTE,
        )
    }
}

/** `.field label{font:700 .7rem/1.2}`, `.field input{font:500 1rem/1.3}`, `.hint{font-size:.78rem}`. */
private val FieldLabelSize = 11.2.sp
private val FieldTextSize = 16.sp
private val HintSize = 12.48.sp

/** `#FFF6E8` under `#27270F`: paper and its ink, the same in both themes. */
private val FieldPaper = Color(0xFFFFF6E8)
private val FieldInk = Color(0xFF27270F)
private val FieldShape = RoundedCornerShape(ZinelyV21Dimens.radiusSm)
private val FieldBorder = 1.5.dp
private val FieldMinHeight = 48.dp
private val FieldFocusOffset = 2.dp

/** `.go{font:700 .94rem/1.2;border-radius:var(--br-lg)}`, `:disabled{opacity:.45}`, `outline-offset:3px`. */
private val GoTextSize = 15.04.sp
private val GoShape = RoundedCornerShape(ZinelyV21Dimens.radiusLg)
private const val GoDisabledOpacity = 0.45f
private val GoFocusOffset = 3.dp
