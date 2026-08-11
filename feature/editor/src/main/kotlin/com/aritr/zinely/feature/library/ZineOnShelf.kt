package com.aritr.zinely.feature.library

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.text.style.TextOverflow
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
 * [zineShelfDateLabel], pure and covered by [ZineShelfLabelsTest] — a shelf that printed "A4 · 2 days ago" under a cover
 * carrying "A4" would be the same information twice, which is what the design moved it to avoid.
 *
 * ### The overflow affordance is kept, and the frozen file does not draw it — ruled, ADR-100
 *
 * **Ruled 2026-08-11: keep it, and redraw it.** It stays for the reason below; it is now three stacked
 * dots at full `ink-soft` rather than a half-strength horizontal `⋯`, because the form the departure
 * originally took failed contrast at 2.90:1 and read as a truncation ellipsis. [MoreDotDiameter] carries
 * the whole argument. The paragraphs below are why the *affordance* exists at all, which the redraw does
 * not change.
 *
 * The V2.1
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
 * The second reason, which the redraw makes load-bearing: **long-press cannot be the only route.** It is
 * undiscoverable by inspection — a first-time user has no way to learn it exists — so removing the visible
 * affordance would not simplify the shelf, it would hide half of what the shelf can do behind a gesture
 * nobody is told about.
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
                    // own text content, which is the name *and* the date. This seam ends in
                    // `clearAndSetSemantics`, so the caption's two `Text`s have no nodes of their own and
                    // the label here is the whole of what the tile says. Announcing the title alone is
                    // therefore a deliberate narrowing, not the browser's behaviour: the date is
                    // disclosure and the sheet's header states it, whereas a shelf that read
                    // "Camping trip, A4, 2 days ago" six times over is a list nobody can skim by ear.
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
                borderInk = zine.recipe.surface.v21BorderInk(colors),
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
                    // **Two lines, then an ellipsis — ADR-100, an amendment to the freeze.**
                    //
                    // `.name` declares no `max-lines` and no overflow, and every title in the frozen
                    // markup is 14 characters or shorter, so the prototype never rendered the case. The
                    // long-title raster does: "Notes from the Sunday market, volume three" wraps to
                    // three lines, and because the grid sizes a row to its tallest cell, one long name
                    // pushes its neighbour's *cover* down by a line-and-a-half of empty desk. The shelf
                    // stops being a grid of objects and becomes a ragged list.
                    //
                    // Two lines is the cap because the cover — not the caption — is what the user
                    // recognises a zine by, and because nothing is lost: the full title is the action
                    // sheet's header, one tap away, and TalkBack reads the whole string regardless
                    // (`zinelyV21Control(label = zine.title)` on the tile, which this `Text` does not
                    // supply). One line was too few — plenty of ordinary titles are two.
                    //
                    // **`TextOverflow.MiddleEllipsis` was tried and is not available here — measured,
                    // not assumed.** A review made the good point that makers distinguish zines by
                    // *suffix* ("… volume two" / "… volume three"), which end-truncation destroys
                    // exactly when it matters. But a probe against this Compose version showed
                    // `MiddleEllipsis` at `maxLines = 2` produces `isLineEllipsized == false` on **both**
                    // lines and simply cuts the string dead — no mark of any kind. It is a single-line
                    // overflow mode. The choice was therefore end-ellipsis at two lines versus
                    // middle-ellipsis at one, and two lines with a visible truncation mark beats one
                    // line with a cleverer one. Revisit if multiline middle-ellipsis ever lands.
                    //
                    // **Font scale is deliberately not special-cased.** At `fontScale 2.0` two lines
                    // hold roughly half the characters, which is a real cost — but the cap is uniform
                    // across every tile, so the grid stays rigid at any scale, and a scale-dependent cap
                    // would make the shelf's layout unpredictable to the users least able to absorb
                    // surprise. The sighted escape hatch is the same one at every scale: the ⋮ sheet's
                    // header, which sets no `maxLines` at all.
                    maxLines = NameMaxLines,
                    overflow = TextOverflow.Ellipsis,
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
        // Three dots stacked, **drawn rather than set** — see [MoreDotDiameter] for both halves of why.
        Canvas(Modifier.size(width = MoreDotDiameter, height = MoreMarkHeight)) {
            val r = MoreDotDiameter.toPx() / 2f
            val step = MoreDotDiameter.toPx() + MoreDotGap.toPx()
            repeat(MoreDotCount) { dot ->
                drawCircle(color = ink, radius = r, center = Offset(r, r + dot * step))
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// The frozen values — per component, per the D-007 ruling that V2.1's §3.3 did not overturn.
// ---------------------------------------------------------------------------------------------

/** Two lines then an ellipsis — an amendment, not a transcription. See the call site. */
private const val NameMaxLines = 2

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
    // `body{line-height:1.55}`, inherited — `.sub` declares none.
    lineHeight = ZinelyV21Fonts.InheritedLineHeight,
    color = color,
)

/**
 * `.start:focus-visible{outline:2px solid var(--ink);outline-offset:5px}` — the corpus's **only**
 * `focus-visible` rule, borrowed for the tile.
 *
 * There is no `.zine:focus-visible`: a browser gives every `<button>` a focus ring for free, so the
 * prototype never had to write one, and Compose gives nothing. The ring is therefore an **addition** to
 * the freeze, taking the one declaration the file does make so the two rings match.
 *
 * **The radius is 0 because `.zine` has none.** A CSS `outline` follows the element's own `border-radius`,
 * and the tile — unlike `.start`, which is a pill — declares none, so its ring is square. An earlier draft
 * rounded it to `radiusSm` by analogy with the cover inside it; that is the cover's radius, not the tile's,
 * and the ring is drawn around the caption too.
 */
private val FocusRingWidth = 2.dp
private val FocusRingOffset = 5.dp
private val FocusRingRadius = 0.dp

/** V2's `.more{width:34px;height:34px}`, carried with the affordance. */
private val MoreSize = 34.dp

/**
 * The overflow mark: **three dots stacked vertically, drawn, at full `ink-soft`** — ADR-100.
 *
 * Three things were wrong with the `⋯` this replaces, and they compound.
 *
 * 1. **It measured 2.90:1 against the desk.** `ink` at `alpha = .5` composites to `#978875` on
 *    `--desk`, and less than that over a cover's cast shadow — failing 4.5:1 as text
 *    and 3:1 as a non-text control indicator, in *both* themes, in the only state a user sees before
 *    touching it. Full `ink-soft` is **5.54:1 light / 7.49:1 dark**, which is the same weight of mark as
 *    the date beside it — which is what the affordance was always documented to be.
 * 2. **Horizontal, inline, immediately after the title, it read as a truncation ellipsis.** The parity
 *    review's first-time reading of `Sunday market ⋯` was *"the name is too long"* — the precise
 *    opposite of "there are more actions here", and a Pass-2 failure however correct the code. Stacking
 *    the dots removes the reading entirely: a vertical kebab cannot be mistaken for elided text, and it
 *    is the overflow idiom every Android user already knows. That the frozen file draws no affordance at
 *    all leaves the *form* free; it never argued for this one.
 * 3. **`⋯` U+22EF is not in the bundled Inter** (D-021), so it was drawn by whatever the device fell
 *    back to — a glyph whose size, weight and centring we did not control, and could lose to tofu. The
 *    empty state's arrow was a `Text` for the same reason until device Pass 1 found emoji2 substituting
 *    a colour emoji for U+2192. Three circles are three circles.
 *
 * **The three do not all prove the same thing, and an earlier draft claimed they did.** 1 and 3 force
 * *drawn rather than set* — a drawn horizontal mark would satisfy both and still be the wrong mark. Only
 * 2 forces *vertical*, and 2 is the product argument, so it is the one carrying the decision. The other
 * two only say the app must own the pixels either way.
 *
 * **The trade this makes, stated rather than assumed:** a vertical kebab is the most recognisably *stock
 * Android* mark on the screen, and V2.1 exists to not read as stock Android. That is a real concession of
 * identity, made deliberately — learnability outranks identity for a control with no label, and this
 * control has no label.
 *
 * 15dp of mark (3 · 3dp dots, 3dp apart) centred in the 34dp box, which
 * `minimumInteractiveComponentSize()` still expands to a 48dp target.
 */
private val MoreDotDiameter = 3.dp
private val MoreDotGap = 3.dp
private const val MoreDotCount = 3
private val MoreMarkHeight = MoreDotDiameter * MoreDotCount + MoreDotGap * (MoreDotCount - 1)

/**
 * The `⋯`'s hover/focus wash.
 *
 * **Not transcribed — carried.** `v21-library.html` has no `.more` at all; the affordance is a V2 carry-over
 * kept because removing the only pointer-and-keyboard route to the action sheet would leave long-press as
 * the sole one. So this literal is V2's `background:rgba(0,0,0,.10)`, held unchanged between themes as the
 * scrim's is, and it is an addition to the freeze rather than a reading of it.
 */
private val MoreLitWash = Color.Black.copy(alpha = 0.10f)

/**
 * The name of the long-press gesture in the platform tree.
 *
 * Neither frozen file names the gesture, because a browser has no such concept. "Actions" is the
 * shortest name consistent with the sheet's own `aria-label="Zine actions"`.
 */
private const val LongPressActionLabel = "Actions"
