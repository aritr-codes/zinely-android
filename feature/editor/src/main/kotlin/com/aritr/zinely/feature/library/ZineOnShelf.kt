package com.aritr.zinely.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.ui.a11y.zinelyV2Control
import com.aritr.zinely.ui.theme.ZinelyHaptic
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts

/** The `⋯` on one placed cover — `.more`, keyed by position like [zineShelfCoverTestTag]. */
internal fun zineShelfMoreTestTag(index: Int): String = "shelf-more-$index"

/**
 * `.zine` — one object on the shelf — `docs/design/mockups/v21-library.html`.
 *
 * ```css
 * .zine{appearance:none;border:0;background:none;padding:0;cursor:pointer;text-align:left;
 *       display:flex;flex-direction:column;gap:var(--gap-sm)}
 * .name{font-family:var(--voice);font-weight:700;font-size:1rem;line-height:1.2;margin:0 var(--gap-hair)}
 * .sub{font-size:.74rem;color:var(--ink-soft);margin:var(--gap-hair) var(--gap-hair) 0;font-weight:500}
 * ```
 *
 * ### The title moved off the cover, and that is the whole re-freeze in one component
 *
 * V2 printed the zine's name *on* the cover. V2.1 prints nothing on it — the cover is a physical object
 * with a mark, a spine, tape and a postmark ([ZineV21Cover]) — and puts the name **below** it, in the
 * editorial voice, with the date under that. So this file changed from "a wrapper around a cover" to "a
 * cover and its caption", and the press, which V2 put on the wrapper, now belongs to the cover alone:
 * `.zine:active .cover`, not `.zine:active`.
 *
 * ### The stamp and the sub are one field, split where the frozen markup splits it
 *
 * `data-sub="A4 · 2 days ago"` is the *sheet's* line. The shelf shows its two halves in two places: the
 * paper size becomes the cover's postmark and the date becomes `.sub`. That is [zineShelfStampLabel] and
 * [zineShelfDateLabel], which are pure and tested — a shelf that printed "A4 · 2 days ago" under a cover
 * carrying "A4" would be the same information twice, which is what the design moved it to avoid.
 *
 * ### ⚠️ The `⋯` is kept, and the frozen file does not draw it
 *
 * **This is a deliberate, declared departure from the freeze, and the owner should rule on it.** The V2.1
 * prototype's `.zine` has no actions affordance at all: its script opens the sheet on a plain `onclick`,
 * which is a demonstration shortcut (it is the only way to see the sheet in a browser), not a statement
 * that tap-opens-actions is the interaction. Read as a specification, the file therefore says nothing
 * about how actions are reached — it does not *remove* the affordance so much as never mention it.
 *
 * V2 did mention it, and gave the reason: the `⋯` is *"the path a screen reader, a keyboard and a switch
 * device can reach"*, and it is the frozen answer to that file's own **screen-reader path for long-press**
 * gate. Dropping it to match a prototype that is silent on the question would trade a stated
 * accessibility guarantee for an inference, so it is kept — under the freeze's own "accessibility
 * improvements are allowed after freeze" clause, at the smallest visible cost this layout permits.
 *
 * It has **moved**, because V2.1 leaves it nowhere to stand on the cover: `.stamp` occupies the
 * bottom-right corner and overhangs it, which is exactly where V2 put the `⋯`. It now sits at the
 * trailing edge of the caption row, opposite the name, where it collides with nothing and stays a
 * sibling — the seam ends in `clearAndSetSemantics`, so a nested `⋯` would be unreachable by the very
 * services it exists for.
 *
 * ### Focus, borrowed from the file's own sibling control
 *
 * `.zine` carries no `:focus-visible` rule in V2.1; `.start` does — `outline:2px solid var(--ink);
 * outline-offset:5px`. Transcribing that rather than V2's `matchaText`/6px ring keeps this file inside
 * the frozen corpus instead of inventing a focus appearance, which D-008 forbids.
 *
 * @param zine what to print and, for the sheet, what to disclose.
 * @param index the position this object stands at. The corpus keys tilt and tape placement off
 *   `:nth-child(3n+k)`, so the position is visual as well as identifying.
 * @param onOpen tap.
 * @param onActions long-press, or the `⋯`.
 */
@Composable
internal fun ZineOnShelf(
    zine: ZineShelfItem,
    index: Int,
    onOpen: (Int) -> Unit,
    onActions: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ZinelyTheme.v21Colors
    val haptics = ZinelyTheme.haptics

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()

    val fill = zine.recipe.surface.v21Fill(colors)
    val markInk = zine.recipe.surface.v21MarkInk(colors)

    Box(modifier) {
        Column(
            Modifier
                // `.start:focus-visible{outline:2px solid var(--ink);outline-offset:5px}`. A CSS outline
                // is drawn outside the box and grows outward, so the stroke's centre line sits one half
                // width beyond the offset — drawn rather than bordered, which would eat layout.
                .drawBehind {
                    if (!focused) return@drawBehind
                    val stroke = FocusRingWidth.toPx()
                    val out = FocusRingOffset.toPx() + stroke / 2f
                    drawRoundRect(
                        color = colors.ink,
                        topLeft = Offset(-out, -out),
                        size = Size(size.width + 2 * out, size.height + 2 * out),
                        cornerRadius = CornerRadius(FocusRingRadius.toPx()),
                        style = Stroke(width = stroke),
                    )
                }
                // Before the seam, never after: the seam ends in `clearAndSetSemantics`, and a tag
                // chained behind it leaves a node no test and no service can find.
                .testTag(zineShelfCoverTestTag(index))
                .zinelyV2Control(
                    // `.zine` carries no `aria-label` — it is a `<button>` whose accessible name is its
                    // own text content, which is the name and the date. The name alone is what a user
                    // calls the object; the date is disclosure, and it follows in `.sub`'s own node.
                    label = zine.title,
                    interactionSource = interaction,
                    onClick = { onOpen(index) },
                    onLongClick = {
                        haptics.perform(ZinelyHaptic.Boundary)
                        onActions(index)
                    },
                    onLongClickLabel = LongPressActionLabel,
                ),
            verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
        ) {
            ZineV21Cover(
                fill = fill,
                stampLabel = zineShelfStampLabel(zine.subtitle),
                index = index,
                pressed = pressed,
                modifier = Modifier.fillMaxWidth(),
            ) { markModifier ->
                Image(
                    imageVector = zine.recipe.stamp.v21Mark(),
                    // Decorative: the cover's meaning is its name, which is printed right below it.
                    // A reader announcing "envelope" over *Letters home* invents content.
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(markInk),
                    modifier = markModifier.aspectRatio(1f),
                )
            }

            // `.name` / `.sub`. The end padding is the `⋯`'s: it is a sibling, so it takes no layout
            // of its own, and without the reservation a long name would run under it.
            Column(Modifier.padding(end = MoreSize)) {
                Text(
                    text = zine.title,
                    style = NameStyle(colors.ink),
                    modifier = Modifier.padding(horizontal = ZinelyV21Dimens.gapHair),
                )
                Text(
                    text = zineShelfDateLabel(zine.subtitle),
                    style = SubStyle(colors.inkSoft),
                    modifier = Modifier.padding(
                        start = ZinelyV21Dimens.gapHair,
                        end = ZinelyV21Dimens.gapHair,
                        top = ZinelyV21Dimens.gapHair,
                    ),
                )
            }
        }

        MoreButton(
            title = zine.title,
            ink = colors.inkSoft,
            onClick = { onActions(index) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .testTag(zineShelfMoreTestTag(index)),
        )
    }
}

/**
 * The paper size, as the cover's postmark — the first half of `data-sub="A4 · 2 days ago"`.
 *
 * Pure, and split on the frozen separator rather than parsed: the subtitle is authored by whoever built
 * the item, and a subtitle that carries no `·` has no size to stamp, so it stamps nothing rather than
 * guessing. Both halves are trimmed because the frozen string spaces the separator.
 */
internal fun zineShelfStampLabel(subtitle: String): String =
    subtitle.substringBefore(SubtitleSeparator, missingDelimiterValue = "").trim()

/**
 * The date, as `.sub` — the second half of the same string, and the whole of it when there is no `·`.
 *
 * The fallback is the useful direction: a shelf that showed nothing under a cover because the subtitle
 * was unseparated would look like missing data, where showing the whole line looks like the line.
 */
internal fun zineShelfDateLabel(subtitle: String): String =
    subtitle.substringAfter(SubtitleSeparator, missingDelimiterValue = subtitle).trim()

private const val SubtitleSeparator = "·"

/**
 * `.more` — the quiet `⋯`, kept from V2 and moved. See the departure note on [ZineOnShelf].
 *
 * Inherits `ink-soft` rather than the cover's printed ink, because it no longer sits on the cover: it
 * sits on the desk beside the date, and it is the same weight of mark as that date.
 */
@Composable
private fun MoreButton(
    title: String,
    ink: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val focused by interaction.collectIsFocusedAsState()
    val lit = hovered || focused

    Box(
        modifier
            .size(MoreSize)
            .clip(CircleShape)
            .background(if (lit) MoreLitWash else Color.Transparent)
            .drawBehind {
                if (!focused) return@drawBehind
                val stroke = FocusRingWidth.toPx()
                drawCircle(
                    color = ink,
                    radius = (size.minDimension + stroke) / 2f,
                    style = Stroke(width = stroke),
                )
            }
            .zinelyV2Control(
                // `aria-label="Actions for Sunday market"` — V2's own string, kept with the affordance.
                label = "Actions for $title",
                interactionSource = interaction,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = MoreGlyph,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontFamily = ZinelyV21Fonts.Work,
                fontSize = MoreGlyphSize,
                color = ink.copy(alpha = if (lit) MoreLitOpacity else MoreRestOpacity),
            ),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// The frozen values — per component, per the D-007 ruling that V2.1's §3.3 did not overturn.
// ---------------------------------------------------------------------------------------------

/** `.name{font-family:var(--voice);font-weight:700;font-size:1rem;line-height:1.2}` */
private fun NameStyle(color: Color) = TextStyle(
    fontFamily = ZinelyV21Fonts.Voice,
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp,
    lineHeight = 19.2.sp,
    color = color,
)

/** `.sub{font-size:.74rem;font-weight:500}` against a 16px root — 11.84px, carried unrounded. */
private fun SubStyle(color: Color) = TextStyle(
    fontFamily = ZinelyV21Fonts.Work,
    fontWeight = FontWeight.Medium,
    fontSize = 11.84.sp,
    letterSpacing = 0.em,
    color = color,
)

/** `.start:focus-visible{outline:2px solid var(--ink);outline-offset:5px}` — the file's own ring. */
private val FocusRingWidth = 2.dp
private val FocusRingOffset = 5.dp
private val FocusRingRadius = ZinelyV21Dimens.radiusSm

/** V2's `.more{width:34px;height:34px;font-size:1.05rem}`, carried with the affordance. */
private val MoreSize = 34.dp
private val MoreGlyphSize = 16.8.sp

/**
 * `⋯` U+22EF — **not in the bundled Inter** (D-021), so the device's fallback draws it. Named here so
 * the ruling costs one line.
 */
private const val MoreGlyph = "⋯"

private const val MoreRestOpacity = 0.5f
private const val MoreLitOpacity = 0.95f

/** `background:rgba(0,0,0,.10)` — a literal, unchanged between themes, like the scrim's. */
private val MoreLitWash = Color.Black.copy(alpha = 0.10f)

/**
 * The name of the long-press gesture in the platform tree.
 *
 * Neither frozen file names the gesture, because a browser has no such concept. "Actions" is the
 * shortest name consistent with the sheet's own `aria-label="Zine actions"`.
 */
private const val LongPressActionLabel = "Actions"
