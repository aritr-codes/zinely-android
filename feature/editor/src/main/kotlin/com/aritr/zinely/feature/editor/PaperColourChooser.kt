package com.aritr.zinely.feature.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.data.repository.PaperColour
import com.aritr.zinely.feature.library.LibrarySheetSurface
import com.aritr.zinely.ui.a11y.zinelyV2Control
import com.aritr.zinely.ui.components.zinelyFocusRing
import com.aritr.zinely.ui.components.zinelyV21HardShadow
import com.aritr.zinely.ui.components.zinelyV21Pressable
import com.aritr.zinely.ui.theme.ZinelyHaptic
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.ZinelyV21Press

/** The pile's radio group, one sheet of it, the sample page and Done, for tests. */
public const val PaperColourGroupTestTag: String = "paper-colour-group"
public fun paperColourOptionTestTag(colour: PaperColour): String = "paper-colour-${colour.id}"
public const val PaperColourSampleTestTag: String = "paper-colour-sample"
public const val PaperColourDoneTestTag: String = "paper-colour-done"
public const val PaperColourSampleLabelTestTag: String = "paper-colour-sample-label"

/** The chooser sheet (both doors), for tests. */
public const val PaperColourSheetTestTag: String = "paper-colour-sheet"

/**
 * A56, the paper pile (`v21-paper-pile.html`, [ADR-152](../../../../../../docs/DECISIONS.md#adr-152)): the Library's
 * own `.sheet` ([LibrarySheetSurface]) titled "Paper colour" (its pane title too), with no head rule; under the head the
 * pinned top row (the sample page, its label, Done); then the pile and the foot line, which scroll at large text.
 *
 * ONE sheet for both doors, the zine sheet and Print details (AW6.3), so the two cannot drift.
 *
 * **A tap does not close it (A56.5).** [onPick] runs for a sheet that is not the current paper; the door saves it and
 * the sheet stays open, so the sample page shows each paper in turn. [onDone] is the door's close: Done calls it, as
 * the door's Back and scrim do.
 *
 * **Fit (A56.2).** At font scale 1 the whole sheet fits a 360 × 640 dp window with no scrolling
 * (`PaperSheetFitTest`). The sheet never draws a scroll bar (Compose draws none).
 */
@Composable
internal fun PaperColourSheetSurface(
    selected: PaperColour,
    onPick: (PaperColour) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    doneFocus: FocusRequester? = null,
) {
    val haptics = ZinelyTheme.haptics
    LibrarySheetSurface(
        title = Copy.PaperColour.TITLE,
        subtitle = Copy.PaperColour.SUB,
        paneTitle = Copy.PaperColour.TITLE,
        modifier = modifier.testTag(PaperColourSheetTestTag),
        // `.head{padding:var(--gap-xs) var(--gap-xl) var(--gap-sm)}`: no `border-bottom`, unlike AW6's sheet.
        headDivider = false,
        pile = true,
        pinned = { PaperSampleRow(selected, onDone, doneFocus) },
    ) {
        PaperPile(selected = selected, onPick = { picked ->
            if (picked != selected) {
                haptics.perform(ZinelyHaptic.Tick)
                onPick(picked)
            }
        })
        // `.foot{font-size:.75em;text-align:center;margin:var(--gap-md) var(--gap-xl) 0;line-height:1.45}`
        BasicText(
            text = Copy.PaperColour.FOOT,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = ZinelyV21Dimens.gapMd, start = ZinelyV21Dimens.gapXl, end = ZinelyV21Dimens.gapXl),
            style = TextStyle(
                color = ZinelyTheme.v21Colors.inkSoft,
                fontFamily = ZinelyV21Fonts.Work,
                fontSize = 12.sp,
                lineHeight = 1.45.em,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

/**
 * A56.1/A56.6's `.top`: the sample page, then its label above Done. The page and the label are hidden from TalkBack
 * (the checked radio already says the choice), so the row reads as Done alone (A56.7).
 *
 * `.top{display:flex;align-items:center;gap:var(--gap-lg);padding:var(--gap-sm) var(--gap-xl) var(--gap-md)}`
 */
@Composable
private fun PaperSampleRow(selected: PaperColour, onDone: () -> Unit, doneFocus: FocusRequester?) {
    val colors = ZinelyTheme.v21Colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(
                start = ZinelyV21Dimens.gapXl, end = ZinelyV21Dimens.gapXl,
                top = ZinelyV21Dimens.gapSm, bottom = ZinelyV21Dimens.gapMd,
            ),
        horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SamplePage(selected)
        // `.side{display:flex;flex-direction:column;align-items:flex-start;gap:var(--gap-md)}`
        Column(verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapMd)) {
            // `.lab{font-size:.78em;font-weight:600;color:var(--ink-soft);line-height:1.35}`
            BasicText(
                text = sampleLabelFor(selected),
                modifier = Modifier.testTag(PaperColourSampleLabelTestTag).clearAndSetSemantics { },
                style = TextStyle(
                    color = colors.inkSoft, fontFamily = ZinelyV21Fonts.Work, fontSize = 12.48.sp,
                    fontWeight = FontWeight.SemiBold, lineHeight = 1.35.em,
                ),
            )
            DoneButton(onDone, doneFocus)
        }
    }
}

/** A56.6's label for [paper]'s sample page. */
internal fun sampleLabelFor(paper: PaperColour): String =
    Copy.PaperColour.sampleLabel(paper.sentenceName, isWhite = paper == PaperColour.WHITE)

/**
 * `.done`, the sheet's one action, in leaf. `min-height:48px;min-width:112px;padding:0 var(--gap-xl);
 * border-radius:var(--br-pill);border:1.5px solid #27270F;background:var(--leaf);color:var(--on-leaf);
 * font-size:.9em;font-weight:700;box-shadow:3px 3px 0 #27270F`, pressed `translate(2px,2px)` + 1px shadow.
 */
@Composable
private fun DoneButton(onDone: () -> Unit, focus: FocusRequester?) {
    val colors = ZinelyTheme.v21Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(ZinelyV21Dimens.radiusPill)
    Box(
        Modifier
            .zinelyV21Pressable(pressed, ZinelyV21Press.Raised, PileInk, shape)
            .zinelyFocusRing(focused, ZinelyV21Dimens.radiusPill)
            .clip(shape)
            .background(colors.leaf)
            .border(1.5.dp, PileInk, shape)
            .defaultMinSize(minWidth = 112.dp, minHeight = 48.dp)
            .testTag(PaperColourDoneTestTag)
            .then(if (focus != null) Modifier.focusRequester(focus) else Modifier)
            .zinelyV2Control(label = Copy.PaperColour.DONE, onClick = onDone, interactionSource = interaction)
            .padding(horizontal = ZinelyV21Dimens.gapXl),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            Copy.PaperColour.DONE,
            style = TextStyle(
                color = colors.onLeaf, fontFamily = ZinelyV21Fonts.Work, fontSize = 14.4.sp, fontWeight = FontWeight.Bold,
            ),
        )
    }
}

/**
 * A56.6: AW6's stand-in page (the prototype's `PAGE` SVG, viewBox 198 × 306), never the maker's cover (ADR-069, AW6.7).
 * Drawn on the export's white, then the paper laid over it once by [drawPaperLayer], as every AW6.7 surface does, so it
 * shows the inks on the paper exactly as the Bench and the Proof do. Square (it is the thing being changed, E.3), with
 * the 1.5dp ink edge and the 3dp hard shadow (`.page`), pinned in both themes (AW6.9). Hidden from TalkBack.
 */
@Composable
private fun SamplePage(paper: PaperColour) {
    val measurer = rememberTextMeasurer()
    Canvas(
        Modifier
            .testTag(PaperColourSampleTestTag)
            .clearAndSetSemantics { }
            .zinelyV21HardShadow(3.dp, PileInk, RectangleShape)
            .background(Color.White)
            .size(SampleWidth, SampleHeight),
    ) {
        val s = size.width / 198f
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        drawRect(
            Brush.linearGradient(listOf(Color.White, Color(0xFF57B0A9)), start = p(22f, 22f), end = p(176f, 118f)),
            topLeft = p(22f, 22f), size = Size(154f * s, 96f * s),
        )
        drawCircle(Color(0xFFB0503F), radius = 26f * s, center = p(150f, 150f))
        drawCircle(Color(0xFFE7B53C), radius = 20f * s, center = p(160f, 160f))
        // Type set in px of the page, never scaled with the reader's text size: it is a picture of a page.
        val head = TextStyle(color = Color(0xFF2A251E), fontFamily = ZinelyV21Fonts.Editorial,
            fontWeight = FontWeight.Bold, fontSize = (22f * s).toSp(), lineHeight = (22f * s).toSp())
        val body = TextStyle(color = Color(0xFF5B5347), fontFamily = ZinelyV21Fonts.Work,
            fontSize = (11f * s).toSp(), lineHeight = (11f * s).toSp())
        // SVG text sits on its baseline; the measured line's top is about one size above it.
        drawText(measurer, Copy.PaperColour.SAMPLE_TITLE_1, topLeft = p(22f, 172f - 20f), style = head, softWrap = false)
        drawText(measurer, Copy.PaperColour.SAMPLE_TITLE_2, topLeft = p(22f, 196f - 20f), style = head, softWrap = false)
        drawText(measurer, Copy.PaperColour.SAMPLE_BODY_1, topLeft = p(22f, 226f - 10f), style = body, softWrap = false)
        drawText(measurer, Copy.PaperColour.SAMPLE_BODY_2, topLeft = p(22f, 242f - 10f), style = body, softWrap = false)
        drawLine(
            Color(0xFF8C8269), p(22f, 270f), p(176f, 270f), strokeWidth = 1.5f * s,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f * s, 4f * s)),
        )
        drawPaperLayer(paper, Rect(Offset.Zero, size))
        val w = 1.5.dp.toPx()
        drawRect(PileInk, topLeft = Offset(w / 2f, w / 2f), size = Size(size.width - w, size.height - w), style = Stroke(w))
    }
}

/**
 * A56.3's `.pile`: ten sheets on a patch of desk, five to a row (two even rows), or three to a row when text is large.
 *
 * **Semantics (AW6.12, ADR-059's Role to View lesson):** the sheets sit in [Modifier.selectableGroup]; each is
 * [Modifier.selectable] with `role = Role.RadioButton`, never a Button carrying `selected` (which reaches the platform
 * as a checkable button). Its children are cleared so it collapses to ONE platform node named by the colour (and
 * White's note); `selectable` sits outside the clear, so its role and checked state survive the collapse. The group
 * is also a one-column collection, so TalkBack can say "n of 10" (A56.7).
 *
 * `.pile{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:var(--gap-sm) var(--gap-xs);
 *  padding:var(--gap-md) var(--gap-sm) var(--gap-sm);background:var(--desk);margin:0 var(--gap-lg);
 *  border-radius:var(--br-md);border:1.5px solid var(--hair)}`
 */
@Composable
private fun PaperPile(selected: PaperColour, onPick: (PaperColour) -> Unit) {
    val colors = ZinelyTheme.v21Colors
    val large = LocalDensity.current.fontScale >= LargeTextScale
    val columns = if (large) 3 else 5
    val shape = RoundedCornerShape(ZinelyV21Dimens.radiusMd)
    val papers = PaperColour.entries
    Column(
        Modifier
            .padding(horizontal = ZinelyV21Dimens.gapLg)
            .fillMaxWidth()
            .clip(shape)
            .background(colors.desk)
            .border(1.5.dp, colors.hair, shape)
            .padding(start = ZinelyV21Dimens.gapSm, end = ZinelyV21Dimens.gapSm, top = ZinelyV21Dimens.gapMd,
                bottom = ZinelyV21Dimens.gapSm)
            .testTag(PaperColourGroupTestTag)
            .selectableGroup()
            .semantics { collectionInfo = CollectionInfo(rowCount = papers.size, columnCount = 1) },
        verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
    ) {
        papers.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapXs)) {
                row.forEach { colour ->
                    PaperSheet(
                        colour = colour,
                        index = papers.indexOf(colour),
                        checked = colour == selected,
                        large = large,
                        onClick = { onPick(colour) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * One `.sh`: the sheet, its name under it, and "Chosen" (kept in the layout, hidden, when not chosen, so no cell moves
 * when the choice changes). The whole cell, at least 48dp tall, is the target.
 *
 * At rest the sheet tilts by its own fixed angle ([PileTilt], the prototype's list, never re-rolled); the chosen one is
 * square, taped down, and named "Chosen" in ink over a 3dp berry rule (A56.4: never colour alone, never the action
 * colour, E.5).
 */
@Composable
private fun PaperSheet(
    colour: PaperColour,
    index: Int,
    checked: Boolean,
    large: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val colors = ZinelyTheme.v21Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val note = colour.note
    val tilt = if (checked || pressed) 0f else PileTilt[index % PileTilt.size]
    Column(
        modifier
            .defaultMinSize(minHeight = 48.dp)
            .zinelyFocusRing(focused, ZinelyV21Dimens.radiusSm, 2.dp)
            .testTag(paperColourOptionTestTag(colour))
            .selectable(
                selected = checked,
                interactionSource = interaction,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            )
            // ONE node, named by the colour and its note. The clear sits inside `selectable`, so the role and the checked
            // state, which `selectable` carries, reach the platform.
            .clearAndSetSemantics {
                contentDescription = if (note == null) colour.displayName else "${colour.displayName}. $note"
                collectionItemInfo = CollectionItemInfo(rowIndex = index, rowSpan = 1, columnIndex = 0, columnSpan = 1)
            }
            .padding(top = ZinelyV21Dimens.gapSm, bottom = ZinelyV21Dimens.gapXs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapXs),
    ) {
        Box(
            Modifier
                .graphicsLayer { rotationZ = tilt }
                .zinelyV21Pressable(pressed, ZinelyV21Press.Raised, PileInk, RectangleShape)
                .background(colour.srgb.toComposeColor())
                .border(1.5.dp, PileInk, RectangleShape)
                .size(if (large) SheetLarge else SheetSmall)
                .drawWithContent {
                    drawContent()
                    if (checked) drawTape()
                },
        )
        // `.sh .nm{font-size:.75em;font-weight:600;line-height:1.2;text-align:center}`
        BasicText(
            colour.displayName,
            style = TextStyle(
                color = colors.ink, fontFamily = ZinelyV21Fonts.Work, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold, lineHeight = 1.2.em, textAlign = TextAlign.Center,
            ),
        )
        // `.sh .ck{font-size:.75em;font-weight:700;letter-spacing:.08em;text-transform:uppercase;color:var(--ink);
        //  border-bottom:3px solid var(--berry);padding-bottom:1px}`
        val berry = colors.berry
        BasicText(
            Copy.PaperColour.CHOSEN.uppercase(),
            modifier = Modifier
                .alpha(if (checked) 1f else 0f)
                .drawBehind {
                    val w = 3.dp.toPx()
                    drawRect(berry, topLeft = Offset(0f, size.height - w), size = Size(size.width, w))
                }
                .padding(bottom = 4.dp),
            style = TextStyle(
                color = colors.ink, fontFamily = ZinelyV21Fonts.Work, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 0.08.em, lineHeight = 1.2.em,
            ),
        )
    }
}

/**
 * The tape on the chosen sheet: `::after{top:-7px;left:50%;width:28px;height:11px;transform:translateX(-50%)
 * rotate(-3deg);background:rgba(233,226,155,.85);border:1.5px dashed rgba(39,39,15,.7)}`.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTape() {
    val w = 28.dp.toPx()
    val h = 11.dp.toPx()
    val topLeft = Offset((size.width - w) / 2f, -7.dp.toPx())
    rotate(-3f, pivot = Offset(size.width / 2f, topLeft.y + h / 2f)) {
        drawRect(TapeFill, topLeft, Size(w, h))
        val e = 1.5.dp.toPx()
        drawRect(
            TapeEdge, Offset(topLeft.x + e / 2f, topLeft.y + e / 2f), Size(w - e, h - e),
            style = Stroke(e, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))),
        )
    }
}

/**
 * `.aw6-sw`: the paper itself as a small swatch, 28dp, never scaled with text (AW6.11), pinned in both themes (AW6.9).
 * The zine sheet's row and Print details' row draw it. `border:1.5px solid #27270F;box-shadow:2px 2px 0 #27270F;
 * border-radius:var(--br-xs)`.
 */
@Composable
internal fun PaperSwatch(colour: PaperColour, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(ZinelyV21Dimens.radiusXs)
    Box(
        modifier
            .zinelyV21HardShadow(2.dp, PileInk, shape)
            .clip(shape)
            .background(colour.srgb.toComposeColor())
            .border(1.5.dp, PileInk, shape)
            .size(PaperSwatchSize),
    )
}

internal val PaperSwatchSize = 28.dp
private val PileInk = Color(0xFF27270F)
private val TapeFill = Color(0xD9E9E29B)
private val TapeEdge = Color(0xB327270F)

/** A56.6's `.page{width:64px;height:92px}`. */
private val SampleWidth: Dp = 64.dp
private val SampleHeight: Dp = 92.dp

/** A56.3: 42 × 58 dp at rest, 52 × 72 at large text. */
private val SheetSmall = androidx.compose.ui.unit.DpSize(42.dp, 58.dp)
private val SheetLarge = androidx.compose.ui.unit.DpSize(52.dp, 72.dp)

/**
 * The font scale from which the pile is three columns of the larger sheets (A56.3 draws it at font scale 2). 1.5 sits
 * between Android's largest common step (1.3, where five columns still hold "Pastel yellow" on two lines) and 2.
 */
internal const val LargeTextScale: Float = 1.5f

/** A56.3's `TILT`, in pile order: seeded per sheet, never re-rolled. Degrees. */
private val PileTilt = floatArrayOf(-1.6f, 1.1f, -0.7f, 1.8f, -1.2f, 0.8f, -1.9f, 1.4f, 0.6f, -0.9f)
