package com.aritr.zinely.feature.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.ui.components.zinelyV21HardShadow
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.zinelyV21LightColors
import java.util.Locale

/** The test handle on the empty state as a whole. */
internal const val ZineShelfEmptyTestTag = "shelf-empty"

/** The test handle on the loose sheet — the left half of the transformation. */
internal const val ZineSheetIllustrationTestTag = "empty-sheet"

/** The test handle on the little book — the right half. */
internal const val ZineBookIllustrationTestTag = "empty-book"

/**
 * `.tf .arrow` — the mark between the two illustrations.
 *
 * It was a `Text` until device Pass 1 turned it into a `Canvas` (see the note at its call site), and a
 * canvas has no text for a test to find. The tag is what the geometry assertions hold on to now.
 *
 * A bare `testTag` adds a semantics node carrying no name, no description and no action, which services
 * skip — so the decorative reading the `Canvas` was chosen for is unchanged.
 */
internal const val ZineArrowTestTag = "empty-arrow"

/**
 * The frozen Library's **empty state** — `docs/design/mockups/v21-library.html`.
 *
 * ```css
 * .empty{position:absolute;inset:0;flex-direction:column;align-items:center;justify-content:center;
 *        text-align:center;padding:var(--gap-2xl) var(--gap-2xl) 150px;gap:var(--gap-md)}
 * ```
 *
 * The design is unchanged in intent — a loose sheet, an arrow, a little book, and three lines of copy,
 * *teaching the concept by showing the transformation*. There is no illustration of the app, no
 * onboarding carousel and no sample zine. Cross-read against the product principle, the Library's
 * question is *"which zine do I want?"*, and when the answer is *none yet* the screen answers the
 * question the user now holds — *"what is this thing going to make?"*
 *
 * ### It **replaces** the shelf
 *
 * The two are alternatives, not layers, so nothing here composes a grid and nothing here is a slot
 * inside one. The dock is not part of that choice: `.dock` sits outside `.empty` and stands in both
 * states — which is the point, since the empty state's only exit is that button.
 *
 * ### The 150px of bottom padding is the dock again
 *
 * `padding:36px 36px 150px` against `justify-content:center` does not centre the content in the screen;
 * it centres it in the space **above the dock**, which is why the bottom number is four times the
 * others. Trimming it would centre the copy under the button.
 *
 * @param modifier the caller's. `position:absolute;inset:0` fills whatever it is placed in.
 */
@Composable
internal fun ZineShelfEmpty(modifier: Modifier = Modifier) {
    val colors = ZinelyTheme.v21Colors

    Column(
        modifier = modifier
            .testTag(ZineShelfEmptyTestTag)
            .fillMaxSize()
            .padding(
                start = EmptyPaddingHorizontal,
                end = EmptyPaddingHorizontal,
                top = EmptyPaddingTop,
                bottom = zineDockClearance(EmptyPaddingBottom),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        // `gap:var(--gap-md)` and `justify-content:center` in one arrangement.
        verticalArrangement = Arrangement.spacedBy(EmptyGap, Alignment.CenterVertically),
    ) {
        // `.tf{margin-bottom:var(--gap-xs)}` — flex `gap` does not absorb margins, so this adds to it.
        TransformationRow(Modifier.padding(bottom = TransformMarginBottom))

        // `.empty h2{font-family:var(--voice);font-size:1.75rem;font-weight:700;line-height:1.12}`.
        //
        // Averia 700. D-005's Fraunces-500 ruling named this selector, and §III Amendment 1 replaced the
        // typeface it ruled on: the bundled voice face has 400 and 700 and nothing between, so there is
        // no lighter weight to prefer and the file's own value is the only one available.
        //
        // `heading()` because the frozen markup is an `<h2>` — dropping the role costs TalkBack a
        // landmark while looking identical on screen. Transcription, not an addition of our own.
        Text(
            text = HeadlineText,
            style = TextStyle(
                fontFamily = ZinelyV21Fonts.Voice,
                fontWeight = FontWeight.Bold,
                fontSize = HeadlineSize,
                lineHeight = HeadlineLineHeight,
                color = colors.ink,
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = HeadlineMarginTop)
                .semantics { heading() },
        )

        // `.empty p{color:var(--ink-soft);max-width:29ch;line-height:1.55;font-size:.94rem}`.
        val bodyStyle = TextStyle(
            fontFamily = ZinelyV21Fonts.Work,
            fontSize = BodySize,
            lineHeight = BodyLineHeight,
            color = colors.inkSoft,
        )
        Text(
            text = BodyText,
            style = bodyStyle,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = measureCharacters(BodyMaxCharacters, bodyStyle)),
        )

    }
}

/**
 * `.tf{display:flex;align-items:center;gap:var(--gap-lg)}` — sheet → arrow → book.
 *
 * The arrow's nudge is a CSS margin on a centred flex item, and a margin on a centred item moves it by
 * **half** its value: `.arrow{margin-bottom:16px}` lifts the arrow 8px above the row's centre line.
 * Compose padding inside a `CenterVertically` row behaves identically, so it transcribes as padding
 * rather than offset — an `Modifier.offset` would move the glyph the full 16dp and be wrong by exactly a
 * factor of two.
 *
 * V2 had a second such margin, `.lbl{margin-top:9px}`, which had to be reasoned about the same way.
 * V2.1 replaced it with `.col{gap:var(--gap-sm)}`, which is an ordinary gap and needs no halving —
 * recorded because the two look interchangeable and are not.
 */
@Composable
private fun TransformationRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TransformGap),
    ) {
        TransformColumn(SheetLabelText) { SheetIllustration() }

        // `.tf .arrow{color:var(--jam);font-size:1.5rem;font-family:var(--voice);font-weight:700;
        // margin-bottom:var(--gap-lg)}` — **drawn, not typed**, and that is a device-Pass-1 finding.
        //
        // Typed, this arrow came out as a **blue rounded square with an orange arrow inside it** on
        // `SM-A176B`: colour emoji, ignoring `color:var(--jam)`, sitting between two hand-drawn
        // illustrations and reading as a broken image. Two fixes were tried on the device and both failed:
        //
        // 1. **Set it in the sans.** Averia carries no U+2192 (cmap read straight out of
        //    `averia_sans_libre_{regular,bold}.ttf` — absent from both) whereas all four bundled Inter
        //    weights do, so the fallback looked like the cause. It was not: still blue, measured
        //    `#1A5CE5` in the raster.
        // 2. **Append U+FE0E**, the text-presentation selector — the documented way to say *"this
        //    codepoint is text"*. Still `#1A5CE5`.
        //
        // U+2192 is `Emoji=Yes` (text-default) in UTR #51, and the substitution happens **above the font
        // layer**, so no `fontFamily` and no variation selector reaches it. A glyph we cannot control is
        // not a glyph we can transcribe, so the arrow is a path: 24dp square, 2dp round-capped strokes,
        // `jam`, which is what `font-size:1.5rem` in `--jam` renders as anywhere the substitution does not
        // fire. Nothing else on this screen is drawn from a font we do not ship.
        //
        // The three other D-021 orphans (`⋯` U+22EF, `✎` U+270E, `⧉` U+29C9) are absent from Inter too and
        // still fall back — but none is emoji-eligible, and on this device all three landed on plain
        // monochrome text glyphs. Left alone, and recorded.
        val jam = ZinelyTheme.v21Colors.jam
        Canvas(
            // No semantics: `<span class="arrow">→</span>` carries no `aria-*` and names nothing — it
            // joins two already-labelled illustrations. A `Canvas` contributes no node, which is the
            // decorative reading. The `Text` it replaces did contribute one, spoken as "right arrow".
            Modifier
                .testTag(ZineArrowTestTag)
                .padding(bottom = ArrowMarginBottom)
                .size(ArrowBox),
        ) {
            val weight = ArrowStroke.toPx()
            val mid = size.height / 2f
            val start = ArrowInset.toPx()
            val end = size.width - start
            val head = ArrowHead.toPx()
            drawLine(jam, Offset(start, mid), Offset(end, mid), weight, StrokeCap.Round)
            drawPath(
                Path().apply {
                    moveTo(end - head, mid - head)
                    lineTo(end, mid)
                    lineTo(end - head, mid + head)
                },
                color = jam,
                style = Stroke(width = weight, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }

        TransformColumn(BookLabelText) { BookIllustration() }
    }
}

/** `.tf .col{display:flex;flex-direction:column;align-items:center;gap:var(--gap-sm)}`. */
@Composable
private fun TransformColumn(label: String, illustration: @Composable () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
    ) {
        illustration()

        // `.tf .lbl{font-size:.62rem;letter-spacing:.12em;text-transform:uppercase;
        //  color:var(--ink-soft);font-weight:700}`.
        //
        // Compose has no `text-transform`, so the case change happens here. `Locale.ROOT` rather than the
        // default: a Turkish device would otherwise render "ONE SHEET" with a dotted İ, which is a
        // locale-dependent rendering of a design that has none. The frozen lowercase source text stays
        // in the constant so what the DOM says and what CSS draws are both visible.
        //
        // The colour moved up a step in V2.1 — `ink-soft`, where V2 used `ink-faint`. These captions are
        // the only words in the diagram, and the re-freeze decided they should be readable rather than
        // atmospheric.
        Text(
            text = label.uppercase(Locale.ROOT),
            style = TextStyle(
                fontFamily = ZinelyV21Fonts.Work,
                fontWeight = FontWeight.Bold,
                fontSize = LabelSize,
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                letterSpacing = LabelTracking,
                color = ZinelyTheme.v21Colors.inkSoft,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * `.sheet-ill` — one flat sheet with its fold guides showing.
 *
 * ```css
 * .sheet-ill{width:94px;height:68px;border-radius:var(--br-xs);background:var(--paper);
 *   border:1.5px solid var(--ink);box-shadow:3px 3px 0 var(--ink-line);transform:rotate(-2deg)}
 * .sheet-ill .v{top:7px;bottom:7px;width:1px} .v1{left:33%} .v3{left:67%}
 * .sheet-ill .v2{left:50%;background:repeating-linear-gradient(var(--ink-faint) 0 3px,transparent 3px 6px);
 *                opacity:.55}
 * .sheet-ill .h{left:7px;right:7px;height:1px;top:50%}
 * ```
 *
 * The four rules are the eight-page fold: three verticals and one horizontal, with the **centre** one
 * dashed because that is the cut, not a fold. Getting the dashed rule onto `.v1` or `.v3` would draw a
 * different set of folding instructions while looking entirely plausible, so a test asserts its
 * position rather than its existence.
 *
 * ### The grain and the soft shadow are gone, and that is the re-freeze
 *
 * V2's sheet carried `background-image:var(--grain)` at soft-light and a blurred `0 8px 16px -10px`
 * shadow, plus a 1px inset edge that had to be stroked at double width against a clip. V2.1 writes a
 * plain paper fill, an ink border and a hard `3px 3px 0` offset. All of that machinery is **deleted**
 * rather than carried — it belonged to a rendering model this design does not use, and keeping it
 * "in case" is how a re-skin ends up shipping both.
 */
@Composable
private fun SheetIllustration() {
    val colors = ZinelyTheme.v21Colors
    val printed = remember { zinelyV21LightColors() }

    Canvas(
        modifier = Modifier
            .testTag(ZineSheetIllustrationTestTag)
            .size(width = SheetWidth, height = SheetHeight)
            .graphicsLayer { rotationZ = SheetRotation }
            .zinelyV21HardShadow(IllustrationShadow, printed.inkLine, SheetShape)
            .clip(SheetShape)
            .background(printed.paper)
            .border(IllustrationBorder, printed.ink, SheetShape),
    ) {
        // Everything below is positioned in the **padding box**, not the border box: the frozen rules are
        // absolutely-positioned children of a bordered element, and CSS resolves both their offsets and
        // their percentages against the padding box — which `border:1.5px` insets on every side. Drawing
        // them against `size` instead puts each rule 1.5px too far up and left and makes them 3px too
        // long, which is invisible in a review and wrong in a diff. A review found it.
        val border = IllustrationBorder.toPx()
        val boxWidth = size.width - 2 * border
        val boxHeight = size.height - 2 * border
        val inset = border + RuleInset.toPx()
        val thickness = RuleThickness.toPx()
        val ruleHeight = boxHeight - 2 * RuleInset.toPx()

        // `.v1{left:33%}` and `.v3{left:67%}` — fractions of the sheet's padding box, not of anything else.
        for (fraction in listOf(FirstFoldFraction, ThirdFoldFraction)) {
            drawRect(
                color = printed.hair,
                topLeft = Offset(border + fraction * boxWidth, inset),
                size = Size(thickness, ruleHeight),
            )
        }

        // `.v2` — `repeating-linear-gradient(var(--ink-faint) 0 3px,transparent 3px 6px)` at `.55`.
        // A 3px-on / 3px-off vertical repeat, drawn as segments rather than as a gradient: a Compose
        // `Brush` has no repeat mode that reproduces a hard-stopped CSS repeating gradient without
        // tiling a shader, and a soft ramp would read as a smudge rather than a cut line.
        val dash = DashLength.toPx()
        val x = border + SecondFoldFraction * boxWidth
        var y = inset
        while (y < inset + ruleHeight) {
            drawRect(
                color = printed.inkFaint,
                topLeft = Offset(x, y),
                size = Size(thickness, minOf(dash, inset + ruleHeight - y)),
                alpha = DashAlpha,
            )
            y += 2 * dash
        }

        // `.h{left:7px;right:7px;height:1px;top:50%}` — 7px in from the padding box on both sides, and
        // its top edge at half that box's height.
        drawRect(
            color = printed.hair,
            topLeft = Offset(inset, border + HalfwayFraction * boxHeight),
            size = Size(boxWidth - 2 * RuleInset.toPx(), thickness),
        )
    }
}

/**
 * `.book-ill` — the same sheet, folded.
 *
 * ```css
 * .book-ill{width:54px;height:70px;border-radius:var(--br-xs) 6px 6px var(--br-xs);
 *   background:var(--leaf);border:1.5px solid var(--ink);box-shadow:3px 3px 0 var(--ink-line);
 *   transform:rotate(2deg)}
 * .book-ill::before{left:6px;top:6px;bottom:6px;width:1px;background:rgba(255,255,255,.3)}
 * ```
 *
 * ### It is not a [ZineV21Cover], and must not become one
 *
 * The temptation is stronger in V2.1 than it was in V2, because the two now share a border, a hard
 * shadow, a crease and a tilt — and it is still wrong. This is 54×70 where a cover is 3:4; it has no
 * mark, no tape, no stamp, no grain, and a radius of its own. It is an **illustration of the idea of a
 * zine**, not an instance of one, and reusing the cover would couple a diagram to the recipe system, so
 * a future change to how covers print would silently redraw a picture that is explaining folding.
 *
 * ### The fill is `--leaf`, and it is a theme token now
 *
 * V2 hard-coded `#7C8A3F` — a *content* ink, theme-invariant because a printed object does not re-tint
 * at night — and this file read it from `ZinelyContentInks` so the value lived in one place. V2.1 writes
 * `var(--leaf)`, which **is** themed. That is a real change of reading, not a transcription slip: the
 * little book is a diagram drawn in the app's own green, not a photograph of a printed object.
 *
 * ### The fore-edge is gone
 *
 * V2's `::after` drew three 1px stripes of stacked paper hanging 3px past the right edge, with a radius
 * CSS clamped from 4 to 1.5 — the most intricate thing in this file and the subject of two corrections.
 * V2.1 deletes it. The whole helper it needed goes with it.
 */
@Composable
private fun BookIllustration() {
    val colors = ZinelyTheme.v21Colors

    Canvas(
        modifier = Modifier
            .testTag(ZineBookIllustrationTestTag)
            .size(width = BookWidth, height = BookHeight)
            .graphicsLayer { rotationZ = BookRotation }
            .zinelyV21HardShadow(IllustrationShadow, colors.onLeaf, BookShape)
            .clip(BookShape)
            .background(colors.leaf)
            .border(IllustrationBorder, colors.onLeaf, BookShape),
    ) {
        // `::before{left:6px;top:6px;bottom:6px}` — the crease, a highlight rather than a drawn line.
        // Padding box again: 6px in from the border, not from the node's edge. See [SheetIllustration].
        val border = IllustrationBorder.toPx()
        val spine = border + BookSpineInset.toPx()
        drawRect(
            color = BookSpine,
            topLeft = Offset(spine, spine),
            size = Size(RuleThickness.toPx(), size.height - 2 * spine),
        )
    }
}

/**
 * `max-width:29ch` — twenty-nine advances of "0" in the used font, which is what the CSS `ch` unit *is*.
 *
 * Measured rather than approximated as a fraction of the screen: `ch` depends on Inter's own zero
 * advance, so a fraction that looked right at one size would be wrong at another and wrong again under
 * font scaling.
 */
@Composable
private fun measureCharacters(count: Int, style: TextStyle): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(measurer, density, style, count) {
        val zeros = measurer.measure("0".repeat(count), style, softWrap = false)
        with(density) { zeros.size.width.toDp() }
    }
}

// ---------------------------------------------------------------------------------------------
// The frozen values, transcribed from `v21-library.html`. Spacing is on the published scale; the
// illustrations' own geometry is not, because it is a drawing rather than a layout.
// ---------------------------------------------------------------------------------------------

/** The original 150dp dock clearance plus the frozen 56dp `Restore a backup` action row. */
private val EmptyPaddingTop = ZinelyV21Dimens.gap2Xl
private val EmptyPaddingHorizontal = ZinelyV21Dimens.gap2Xl
private val EmptyPaddingBottom = 206.dp
private val EmptyGap = ZinelyV21Dimens.gapMd

/** `.tf{gap:var(--gap-lg);margin-bottom:var(--gap-xs)}`. */
private val TransformGap = ZinelyV21Dimens.gapLg
private val TransformMarginBottom = ZinelyV21Dimens.gapXs

/** `.tf .arrow{font-size:1.5rem;margin-bottom:var(--gap-lg)}` = 24px. U+2192 RIGHTWARDS ARROW. */
/** `font-size:1.5rem` — the box the drawn arrow occupies, matching the glyph's 24px em. */
private val ArrowBox = 24.dp

/** Inter's `→` at 24px measures ~1.9px on the shaft; 2dp, round-capped, is that line. */
private val ArrowStroke = 2.dp

/** The glyph's side bearings — a typed `→` does not touch its own advance edges. */
private val ArrowInset = 2.5.dp

/** Half the head's height, and its depth: a 45° chevron, as Inter draws it. */
private val ArrowHead = 6.dp

private val ArrowMarginBottom = ZinelyV21Dimens.gapLg

/** `.tf .lbl{font-size:.62rem;letter-spacing:.12em;font-weight:700}` = 9.92px. */
private val LabelSize = 9.92.sp
private val LabelTracking = 0.12.em

/** The captions as the DOM holds them. CSS uppercases them — see [TransformColumn]. */
private const val SheetLabelText = "one sheet"
private const val BookLabelText = "a little book"

/** `.empty h2{font-size:1.75rem;line-height:1.12;margin:var(--gap-xs) 0 0}` = 28px over 31.36px. */
private const val HeadlineText = "Make your first little zine."
private val HeadlineSize = 28.sp
private val HeadlineLineHeight = 31.36.sp
private val HeadlineMarginTop = ZinelyV21Dimens.gapXs

/**
 * `.empty p{font-size:.94rem;line-height:1.55;max-width:29ch}` = 15.04px.
 *
 * The em dash and the right single quote are U+2014 and U+2019, both present in every bundled Inter
 * weight (measured against the `cmap`s). They are the frozen characters and stay as frozen, per D-021.
 *
 * **29ch, not V2's 28.** One character, and it changes where two lines break.
 */
private const val BodyText =
    "One sheet of paper becomes a little eight-page book you print and fold yourself — " +
        "we’ll show you each step."
private val BodySize = 15.04.sp
private val BodyLineHeight = 1.55.em
private const val BodyMaxCharacters = 29

/**
 * `.empty .pv{font-size:.75rem;font-weight:600;margin-top:var(--gap-xs)}` = 12px.
 *
 * **The wording lost two words.** V2 said *"Everything you make stays on your phone"*; V2.1 says
 * *"Everything stays on your phone"*. Transcribed as frozen — the shorter line is the one the design
 * shows, and a promise is a thing to quote rather than to improve.
 */

/** `box-shadow:3px 3px 0 var(--ink-line)` and `border:1.5px solid var(--ink)` on both illustrations. */
private val IllustrationShadow = 3.dp
private val IllustrationBorder = 1.5.dp

/** `.sheet-ill{width:94px;height:68px;border-radius:var(--br-xs);transform:rotate(-2deg)}`. */
private val SheetWidth = 94.dp
private val SheetHeight = 68.dp
private val SheetShape: Shape = RoundedCornerShape(ZinelyV21Dimens.radiusXs)
private const val SheetRotation = -2f

/** `.sheet-ill i` — `width:1px`, verticals inset `7px`, horizontal inset `7px` and at `top:50%`. */
private val RuleThickness = 1.dp
private val RuleInset = 7.dp
private const val FirstFoldFraction = 0.33f
private const val SecondFoldFraction = 0.50f
private const val ThirdFoldFraction = 0.67f
private const val HalfwayFraction = 0.50f

/** `.v2{background:repeating-linear-gradient(… 0 3px,transparent 3px 6px);opacity:.55}`. */
private val DashLength = 3.dp
private const val DashAlpha = 0.55f

/** `.book-ill{width:54px;height:70px;transform:rotate(2deg)}`. */
private val BookWidth = 54.dp
private val BookHeight = 70.dp
private const val BookRotation = 2f

/**
 * `border-radius:var(--br-xs) 6px 6px var(--br-xs)`.
 *
 * Absolute, not logical: a printed object does not mirror (**D-019**), as the cover does not.
 */
private val BookShape: Shape = AbsoluteRoundedCornerShape(
    topLeft = ZinelyV21Dimens.radiusXs,
    topRight = 6.dp,
    bottomRight = 6.dp,
    bottomLeft = ZinelyV21Dimens.radiusXs,
)

/** `.book-ill::before{left:6px;top:6px;bottom:6px;background:rgba(255,255,255,.3)}`. */
private val BookSpineInset = 6.dp
private val BookSpine = Color.White.copy(alpha = 0.3f)
