package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.data.repository.PaperColour
import com.aritr.zinely.feature.library.LibrarySheetSurface
import com.aritr.zinely.ui.components.zinelyFocusRing
import com.aritr.zinely.ui.components.zinelyV21HardShadow
import com.aritr.zinely.ui.theme.ZinelyHaptic
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts

/** The chooser's radio group, and one row of it, for tests. */
public const val PaperColourGroupTestTag: String = "paper-colour-group"
public fun paperColourOptionTestTag(colour: PaperColour): String = "paper-colour-${colour.id}"

/**
 * The AW6.4 chooser's body: seven rows in the AW6.5 order, then the foot line. One body for both doors (the zine
 * sheet and Print details, AW6.3), so the two cannot drift; each door supplies its own sheet around it.
 *
 * **Semantics (AW6.12, ADR-059's Role to View lesson):** the rows sit in [Modifier.selectableGroup]; each row is
 * [Modifier.selectable] with `role = Role.RadioButton`, never a Button carrying `selected` (which reaches the
 * platform as a checkable button). The row's children are cleared so it collapses to ONE platform node named by the
 * colour (and its note); `selectable` sits outside the clear, so its role and checked state survive the collapse.
 *
 * The current paper is checked by a ring on its swatch **and** a tick, never by colour alone. One tap picks; the
 * door closes the chooser ([onPick] is called even for the current paper, which changes nothing).
 *
 * @param rowPaddingH the row's side padding: the Library sheet's `.aw6-opt` is full-bleed at `--gap-xl`; inside
 *   Print details' drawer, which already pads, it is `--gap-xs` (`.aw6p-opt`).
 */
@Composable
internal fun PaperColourOptions(
    selected: PaperColour,
    onPick: (PaperColour) -> Unit,
    modifier: Modifier = Modifier,
    rowPaddingH: Dp = ZinelyV21Dimens.gapXl,
) {
    val haptics = ZinelyTheme.haptics
    Column(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().testTag(PaperColourGroupTestTag).selectableGroup()) {
            PaperColour.entries.forEach { colour ->
                PaperColourOption(
                    colour = colour,
                    checked = colour == selected,
                    onClick = { haptics.perform(ZinelyHaptic.Tick); onPick(colour) },
                    paddingH = rowPaddingH,
                )
            }
        }
        // `.aw6-foot{font-size:.76em;text-align:center;margin:var(--gap-md) var(--gap-xl) 0;line-height:1.5}`
        BasicText(
            text = Copy.PaperColour.FOOT,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = ZinelyV21Dimens.gapMd, start = ZinelyV21Dimens.gapXl, end = ZinelyV21Dimens.gapXl),
            style = TextStyle(
                color = ZinelyTheme.v21Colors.inkSoft,
                fontFamily = ZinelyV21Fonts.Work,
                fontSize = 12.16.sp,
                lineHeight = 1.5.em,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

/**
 * One `.aw6-opt`.
 *
 * ```css
 * .aw6-opt{display:flex;align-items:flex-start;gap:var(--gap-lg);min-height:48px;
 *   border-bottom:1.5px dashed var(--hair);padding:var(--gap-md) var(--gap-xl)}
 * .aw6-opt .nm{font-size:.9em;font-weight:600;line-height:1.3}
 * .aw6-opt .nt{font-size:.73em;font-weight:500;color:var(--ink-soft);line-height:1.45}
 * .aw6-opt .tick{width:1.1em;font-weight:700;color:var(--leaf-text)}
 * .aw6-opt[aria-checked="true"] .aw6-sw{outline:3px solid var(--leaf);outline-offset:2px}
 * ```
 */
@Composable
private fun PaperColourOption(colour: PaperColour, checked: Boolean, onClick: () -> Unit, paddingH: Dp) {
    val colors = ZinelyTheme.v21Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val hair = colors.hair
    val note = colour.note
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .zinelyFocusRing(focused, ZinelyV21Dimens.radiusSm, (-4).dp)
            .background(if (pressed) colors.leafTint else Color.Transparent)
            .drawBehind {
                val w = 1.5.dp.toPx()
                drawLine(
                    hair, Offset(0f, size.height - w / 2f), Offset(size.width, size.height - w / 2f), w,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
                )
            }
            .testTag(paperColourOptionTestTag(colour))
            .selectable(
                selected = checked,
                interactionSource = interaction,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            )
            // AW6.12: ONE node, named by the colour and its note. The clear sits inside `selectable`, so the role and
            // the checked state, which `selectable` carries, reach the platform.
            .clearAndSetSemantics { contentDescription = if (note == null) colour.displayName else "${colour.displayName}. $note" }
            .padding(horizontal = paddingH, vertical = ZinelyV21Dimens.gapMd),
        horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapLg),
        verticalAlignment = Alignment.Top,
    ) {
        PaperSwatch(colour, ringed = checked)
        val ink = if (pressed) colors.onLeaf else colors.ink
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapHair)) {
            BasicText(
                text = colour.displayName,
                style = TextStyle(
                    color = ink, fontFamily = ZinelyV21Fonts.Work, fontSize = 14.4.sp,
                    fontWeight = FontWeight.SemiBold, lineHeight = 1.3.em,
                ),
            )
            if (note != null) {
                BasicText(
                    text = note,
                    style = TextStyle(
                        color = if (pressed) colors.onLeaf else colors.inkSoft, fontFamily = ZinelyV21Fonts.Work,
                        fontSize = 11.68.sp, fontWeight = FontWeight.Medium, lineHeight = 1.45.em,
                    ),
                )
            }
        }
        BasicText(
            text = "✓",
            modifier = Modifier.width(TickWidth).alpha(if (checked) 1f else 0f),
            style = TextStyle(
                color = colors.leafText, fontFamily = ZinelyV21Fonts.Work, fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

/**
 * `.aw6-sw`: the paper itself, 28dp, never scaled with text (AW6.11), pinned in both themes (AW6.9).
 * `border:1.5px solid #27270F;box-shadow:2px 2px 0 #27270F;border-radius:var(--br-xs)`; checked adds
 * `outline:3px solid var(--leaf);outline-offset:2px`.
 */
@Composable
internal fun PaperSwatch(colour: PaperColour, ringed: Boolean = false, modifier: Modifier = Modifier) {
    val leaf = ZinelyTheme.v21Colors.leaf
    val shape = RoundedCornerShape(ZinelyV21Dimens.radiusXs)
    Box(
        modifier
            .drawBehind {
                if (!ringed) return@drawBehind
                val w = 3.dp.toPx()
                val inflate = 2.dp.toPx() + w / 2f
                drawRoundRect(
                    leaf,
                    topLeft = Offset(-inflate, -inflate),
                    size = Size(size.width + 2 * inflate, size.height + 2 * inflate),
                    cornerRadius = CornerRadius(ZinelyV21Dimens.radiusXs.toPx() + inflate),
                    style = Stroke(w),
                )
            }
            .zinelyV21HardShadow(2.dp, SwatchInk, shape)
            .clip(shape)
            .background(colour.srgb.toComposeColor())
            .border(1.5.dp, SwatchInk, shape)
            .size(PaperSwatchSize),
    )
}

internal val PaperSwatchSize = 28.dp
private val SwatchInk = Color(0xFF27270F)

/** `.tick{width:1.1em}` of the row's 16px. */
private val TickWidth = 17.6.dp

/** The Library door's chooser sheet (AW6.3a), for tests. */
public const val PaperColourSheetTestTag: String = "paper-colour-sheet"

/**
 * AW6.4 from the zine sheet: the Library's own `.sheet` ([LibrarySheetSurface], as `.aw6-sheet` restates it),
 * titled "Paper colour" (its pane title too, AW6.12), holding [PaperColourOptions] full-bleed.
 */
@Composable
internal fun PaperColourSheetSurface(
    selected: PaperColour,
    onPick: (PaperColour) -> Unit,
    modifier: Modifier = Modifier,
) {
    LibrarySheetSurface(
        title = Copy.PaperColour.TITLE,
        subtitle = Copy.PaperColour.SUB,
        paneTitle = Copy.PaperColour.TITLE,
        modifier = modifier.testTag(PaperColourSheetTestTag),
    ) {
        PaperColourOptions(selected = selected, onPick = onPick)
    }
}
