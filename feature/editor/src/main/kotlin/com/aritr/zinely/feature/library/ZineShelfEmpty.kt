package com.aritr.zinely.feature.library

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
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
import com.aritr.zinely.ui.components.zinelyV2Shadow
import com.aritr.zinely.ui.theme.ZinelyMakerInkId
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV2Grain
import com.aritr.zinely.ui.theme.ZinelyV2ShadowLayer
import com.aritr.zinely.ui.theme.rememberZinelyV2GrainBrush
import java.util.Locale

/** The test handle on the empty state as a whole. */
internal const val ZineShelfEmptyTestTag = "shelf-empty"

/** The test handle on the loose sheet — the left half of the transformation. */
internal const val ZineSheetIllustrationTestTag = "empty-sheet"

/** The test handle on the little book — the right half. */
internal const val ZineBookIllustrationTestTag = "empty-book"

/**
 * The frozen Library's **empty state** — `v2-library.html:97-117`, `:157-166`.
 *
 * ```
 * .empty{position:absolute;inset:0;display:none;flex-direction:column;align-items:center;
 *   justify-content:center;text-align:center;padding:36px 40px 140px;gap:16px}
 * body.is-empty .shelf{display:none} body.is-empty .empty{display:flex}
 * ```
 *
 * The frozen file writes its own intent above the rule — *"empty state — teaches the concept by showing
 * the transformation"* (`:97`) — and that is the whole design: a loose sheet, an arrow, a little book, and
 * three lines of copy. There is no illustration of the *app*, no onboarding carousel and no sample zine.
 * Cross-read against the product principle, the Library's question is *"which zine do I want?"*, and when
 * the answer is *none yet* the screen answers the question the user actually now holds — *"what is this
 * thing going to make?"* — with a picture of the transformation rather than a tour.
 *
 * ### It **replaces** the shelf, and choosing between them is B5's
 *
 * `body.is-empty .shelf{display:none}` — the two are alternatives, not layers, so nothing here composes a
 * grid and nothing here is a slot inside one. Which of the two is shown depends on whether there are any
 * projects, and real project data is **B5**'s row on the roadmap. So B4 ships both halves and B5 chooses,
 * the same deferral shape B2 used when it shipped a shelf and left the desk to the screen. The dock is
 * *not* part of that choice: `.dock` sits outside `.empty` in the markup and has no `is-empty` rule, so it
 * stands in both states — which is the point, since the empty state's only exit is that button.
 *
 * ### The 140px of bottom padding is the dock again
 *
 * `padding:36px 40px 140px` against `justify-content:center` does not centre the content in the screen; it
 * centres it in the space **above the dock**, which is why the bottom number is five times the others.
 * Trimming it would centre the copy under the button. Same reservation as [ZineShelf]'s 152px, and made of
 * the same value in a different place, so the two are deliberately not shared: 152 clears the dock for a
 * *scrolling* region and 140 clears it for a *centred* one, and the frozen file states them separately.
 *
 * @param modifier the caller's. `position:absolute;inset:0` fills whatever it is placed in; B5 places it
 *   in the same [androidx.compose.foundation.layout.Box] as the shelf and the dock.
 */
@Composable
internal fun ZineShelfEmpty(modifier: Modifier = Modifier) {
    val colors = ZinelyTheme.v2Colors

    Column(
        modifier = modifier
            .testTag(ZineShelfEmptyTestTag)
            .fillMaxSize()
            .padding(
                start = EmptyPaddingHorizontal,
                end = EmptyPaddingHorizontal,
                top = EmptyPaddingTop,
                bottom = EmptyPaddingBottom,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        // `gap:16px` and `justify-content:center` in one arrangement.
        verticalArrangement = Arrangement.spacedBy(EmptyGap, Alignment.CenterVertically),
    ) {
        // `.tf{margin-bottom:6px}` — flex `gap` does not absorb margins, so this adds to the 16px.
        TransformationRow(Modifier.padding(bottom = TransformMarginBottom))

        // `.empty h2.serif{font-size:1.72rem;margin:8px 0 0;font-weight:600;letter-spacing:-.01em}`.
        // **Fraunces 500, not 600.** `.empty h2` is one of the three selectors the D-005 ruling names by
        // name (`.sh-ttl`, `.shelf-head h1`, `.empty h2`); the 600 is an artefact of the Iowan/Georgia
        // stack the Library was authored against, and the Constitution outranks the frozen file here.
        //
        // `heading()` for B2's reason, on the same evidence: the frozen markup is an `<h2>`, so dropping
        // the role costs TalkBack a landmark while looking identical on screen. Transcription of the
        // markup, not an accessibility addition of our own.
        Text(
            text = HeadlineText,
            style = TextStyle(
                fontFamily = ZinelyTheme.v2Typography.voice,
                fontWeight = FontWeight.Medium,
                fontSize = HeadlineSize,
                letterSpacing = HeadlineTracking,
                color = colors.ink,
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = HeadlineMarginTop)
                .semantics { heading() },
        )

        // `.empty p{margin:0;color:var(--ink-soft);max-width:28ch;line-height:1.55;font-size:.95rem}`.
        val bodyStyle = TextStyle(
            fontFamily = ZinelyTheme.v2Typography.work,
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

        // `.empty .pv{font-size:.76rem;color:var(--matcha-text);font-weight:600;margin-top:2px}` — the
        // privacy line, which is the one product promise this screen makes and the reason it is not
        // ink-soft like the sentence above it.
        //
        // **It is a `<p>`, so `.empty p` still applies to it** — `max-width:28ch` and `line-height:1.55`
        // are inherited, and `.pv` overrides only size, colour, weight and margin. That is easy to miss
        // twice over: the rule is written for the paragraph above and `ch` is relative to the element's
        // *own* font size, so the same 28 characters bind this line **much** narrower — 28 advances at
        // 12.16sp, not at 15.2sp. A first draft applied neither and let the line run to the full column;
        // the raster is what showed it, which is the one thing a raster is better at than a number.
        val privacyStyle = TextStyle(
            fontFamily = ZinelyTheme.v2Typography.work,
            fontWeight = FontWeight.SemiBold,
            fontSize = PrivacySize,
            lineHeight = BodyLineHeight,
            color = colors.matchaText,
        )
        Text(
            text = PrivacyText,
            style = privacyStyle,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = PrivacyMarginTop)
                .widthIn(max = measureCharacters(BodyMaxCharacters, privacyStyle)),
        )
    }
}

/**
 * `.tf{display:flex;align-items:center;gap:14px}` — sheet → arrow → book, `:158-162`.
 *
 * The two nudges are CSS margins on centred flex items, and a margin on a centred item moves it by **half**
 * its value: `.arrow{margin-bottom:18px}` lifts the arrow 9px above the row's centre line, and
 * `.lbl{margin-top:9px}` both spaces the caption and shifts its column. Compose padding inside a
 * `CenterVertically` row behaves identically, so these transcribe as padding rather than offset — an
 * `Modifier.offset` would move the glyph the full 18dp and be wrong by exactly a factor of two.
 */
@Composable
private fun TransformationRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TransformGap),
    ) {
        TransformColumn(SheetLabelText) { SheetIllustration() }

        // `.tf .arrow{color:var(--matcha-text);font-size:1.3rem;margin-bottom:18px}`, `:160`. U+2192, and
        // present in every bundled Inter weight — measured, unlike the `＋` in the dock. The frozen `.col`
        // wrapper around it is a single-child column, which is the child.
        Text(
            text = ArrowGlyph,
            style = TextStyle(
                fontFamily = ZinelyTheme.v2Typography.work,
                fontSize = ArrowSize,
                color = ZinelyTheme.v2Colors.matchaText,
            ),
            modifier = Modifier.padding(bottom = ArrowMarginBottom),
        )

        TransformColumn(BookLabelText) { BookIllustration() }
    }
}

/** `.tf .col{display:flex;flex-direction:column;align-items:center}` with its `.lbl` caption. */
@Composable
private fun TransformColumn(label: String, illustration: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        illustration()

        // `.tf .lbl{font-size:.66rem;letter-spacing:.1em;text-transform:uppercase;color:var(--ink-faint);
        //  font-weight:700;margin-top:9px}`.
        //
        // Compose has no `text-transform`, so the case change happens here. `Locale.ROOT` rather than the
        // default: a Turkish device would otherwise render "ONE SHEET" with a dotted İ, which is a
        // locale-dependent rendering of a design that has none. The frozen lowercase source text is kept
        // in the constant so what the DOM says and what CSS draws are both visible.
        Text(
            text = label.uppercase(Locale.ROOT),
            style = TextStyle(
                fontFamily = ZinelyTheme.v2Typography.work,
                fontWeight = FontWeight.Bold,
                fontSize = LabelSize,
                letterSpacing = LabelTracking,
                color = ZinelyTheme.v2Colors.inkFaint,
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = LabelMarginTop),
        )
    }
}

/**
 * `.sheet-ill` — one flat sheet with its fold guides showing, `:104-108`, `:159`.
 *
 * ```
 * .sheet-ill{width:92px;height:66px;border-radius:3px;background:var(--paper);
 *   box-shadow:0 8px 16px -10px var(--shadow),inset 0 0 0 1px var(--paper-edge);
 *   background-image:var(--grain);background-size:90px 90px;background-blend-mode:soft-light}
 * .sheet-ill i{position:absolute;background:var(--hair)}
 * .sheet-ill .v{top:6px;bottom:6px;width:1px} .v1{left:33%} .v3{left:67%}
 * .sheet-ill .v2{left:50%;background:repeating-linear-gradient(var(--ink-faint) 0 3px,transparent 3px 6px);opacity:.5}
 * .sheet-ill .h{left:6px;right:6px;height:1px;top:50%}
 * ```
 *
 * The four rules are the eight-page fold: three verticals and one horizontal, with the **centre** one
 * dashed because that is the cut, not a fold. Getting the dashed rule onto `.v1` or `.v3` would draw a
 * different set of folding instructions while looking entirely plausible, so
 * `the dashed rule is the middle one` asserts its position rather than its existence.
 *
 * Painted in **one draw scope**, for B1's reason: a blend mode composites against what is already in its
 * own layer, and a child node's backdrop is transparent, so grain drawn as a separate node degrades
 * silently to `src-over`. Below API 29 it draws **nothing at all** (**D-014** — soft-light does not exist
 * there and an approximation of a material is a second material), so the sheet is flat stock on those
 * devices, alongside the flat covers B1 already discloses.
 */
@Composable
private fun SheetIllustration() {
    val colors = ZinelyTheme.v2Colors
    val grain = rememberZinelyV2GrainBrush(SheetGrainTile)

    Canvas(
        modifier = Modifier
            .testTag(ZineSheetIllustrationTestTag)
            .size(width = SheetWidth, height = SheetHeight)
            .zinelyV2Shadow(sheetShadow(colors.shadow), SheetShape)
            .clip(SheetShape),
    ) {
        drawRect(colors.paper)
        drawGrain(grain)

        // `inset 0 0 0 1px var(--paper-edge)` — an inset shadow paints above the background and below the
        // children. Stroked at twice the width along the boundary, with the caller's clip cutting the
        // outer half: exact for a 1px inner hairline, which B1's cover established.
        drawRoundRect(
            color = colors.paperEdge,
            cornerRadius = CornerRadius(SheetRadius.toPx()),
            style = Stroke(width = 2 * RuleThickness.toPx()),
        )

        val inset = RuleInset.toPx()
        val thickness = RuleThickness.toPx()
        val ruleHeight = size.height - 2 * inset

        // `.v1{left:33%}` and `.v3{left:67%}` — fractions of the sheet's width, not of anything else.
        for (fraction in listOf(FirstFoldFraction, ThirdFoldFraction)) {
            drawRect(
                color = colors.hair,
                topLeft = Offset(fraction * size.width, inset),
                size = Size(thickness, ruleHeight),
            )
        }

        // `.v2` — `repeating-linear-gradient(var(--ink-faint) 0 3px,transparent 3px 6px)` at `opacity:.5`.
        // A 3px-on / 3px-off vertical repeat, so it is drawn as segments rather than as a gradient: a
        // Compose `Brush` has no repeat mode that reproduces a hard-stopped CSS repeating gradient without
        // tiling a shader, and a soft ramp would read as a smudge rather than a cut line.
        val dash = DashLength.toPx()
        val x = SecondFoldFraction * size.width
        var y = inset
        while (y < inset + ruleHeight) {
            drawRect(
                color = colors.inkFaint,
                topLeft = Offset(x, y),
                size = Size(thickness, minOf(dash, inset + ruleHeight - y)),
                alpha = DashAlpha,
            )
            y += 2 * dash
        }

        // `.h{left:6px;right:6px;height:1px;top:50%}` — the top edge sits at half the height.
        drawRect(
            color = colors.hair,
            topLeft = Offset(inset, HalfwayFraction * size.height),
            size = Size(size.width - 2 * inset, thickness),
        )
    }
}

/**
 * `.book-ill` — the same sheet, folded, `:109-113`, `:161`.
 *
 * ```
 * .book-ill{width:52px;height:68px;border-radius:3px 5px 5px 3px;background:#7C8A3F;
 *   box-shadow:0 10px 18px -10px var(--shadow);
 *   background-image:var(--grain);background-size:70px 70px;background-blend-mode:soft-light}
 * .book-ill::before{left:5px;top:5px;bottom:5px;width:1px;background:rgba(255,255,255,.25)}
 * .book-ill::after{right:-3px;top:4px;bottom:4px;width:3px;border-radius:0 4px 4px 0;
 *   background:repeating-linear-gradient(90deg,#F1EBDA,#F1EBDA 1px,#E3D9C2 1px,#E3D9C2 2px)}
 * ```
 *
 * ### It is not a [ZineCover], and must not become one
 *
 * The temptation is obvious — B1 already draws a little book — and it is wrong on the evidence. This is
 * 52×68 where a cover is 3:4; it has no ink band, no stamp, no title, no fore-edge *gradient*, a different
 * radius, a different grain tile and a hard-coded fill. It is an **illustration of the idea of a zine**,
 * not an instance of one, and reusing the cover would couple a diagram to the recipe system — so a future
 * change to how covers print would silently redraw a picture that is explaining folding.
 *
 * ### The fill is a content ink, and that is why it does not follow the theme
 *
 * `#7C8A3F` is not a chrome token; it is `ZinelyMakerInkId.Matcha` verbatim (`v2-bench.html:19`,
 * `v2-library.html:79`), and content inks are theme-invariant because a printed object does not re-tint at
 * night. Read from [com.aritr.zinely.ui.theme.ZinelyContentInks] rather than restated as a literal, so
 * there is one place the value lives. `#F1EBDA` on the fore-edge is the cover **stock** for the same
 * reason; `#E3D9C2`, its darker stripe, appears nowhere else and stays a per-component literal.
 *
 * ### The fore-edge is drawn outside the box, deliberately
 *
 * `right:-3px` puts the stacked leaves *beyond* the element, so `.col{align-items:center}` centres the
 * 52px book and the edge overhangs. Compose does not clip a `Canvas` to its bounds unless asked, so the
 * draw runs past `size.width` and the layout stays 52dp wide — which is what keeps the book optically
 * centred under its caption instead of shunted 1.5dp left.
 */
@Composable
private fun BookIllustration() {
    val colors = ZinelyTheme.v2Colors
    val fill = ZinelyTheme.contentInks[ZinelyMakerInkId.Matcha].value
    val grain = rememberZinelyV2GrainBrush(BookGrainTile)

    Canvas(
        modifier = Modifier
            .testTag(ZineBookIllustrationTestTag)
            .size(width = BookWidth, height = BookHeight)
            .zinelyV2Shadow(bookShadow(colors.shadow), BookShape),
    ) {
        // Clipped by hand rather than by `Modifier.clip`, because the fore-edge has to escape the bounds
        // the clip would impose. The cover's asymmetric radius, for the cover's reason (D-019): a printed
        // object keeps its binding edge in every locale.
        clipPath(shapePath(BookShape, Offset.Zero, size)) {
            drawRect(fill)
            drawGrain(grain)
        }

        // `::before` — the crease, a highlight rather than a drawn line.
        drawRect(
            color = BookSpine,
            topLeft = Offset(BookSpineInsetStart.toPx(), BookSpineInsetVertical.toPx()),
            size = Size(RuleThickness.toPx(), size.height - 2 * BookSpineInsetVertical.toPx()),
        )

        drawForeEdge()
    }
}

/**
 * `::after` — three 1px stripes of stacked paper hanging off the open side.
 *
 * `repeating-linear-gradient(90deg,#F1EBDA,#F1EBDA 1px,#E3D9C2 1px,#E3D9C2 2px)` over a 3px width is a
 * 2px period: cream, dark, cream.
 *
 * Its `border-radius:0 4px 4px 0` cannot be taken at face value — CSS scales a radius pair down when it
 * overflows its side, and 4+4 on a 3px width scales by 3/8, so the browser draws **1.5px**, which is what
 * is written here. **Skia applies the same overflow scaling**, so writing the literal 4 renders
 * identically: a mutation to `4.dp` is an equivalent mutant and no assertion can distinguish it. That was
 * measured, not assumed — an earlier draft of this comment claimed the literal 4 "would round a sliver
 * into a lozenge", which is true of the arithmetic and false of the renderer. The value stays 1.5 because
 * it is what the design computes to, not because the platform needs telling.
 */
private fun DrawScope.drawForeEdge() {
    val width = ForeEdgeWidth.toPx()
    val left = size.width
    val top = ForeEdgeInsetVertical.toPx()
    val height = size.height - 2 * top
    val stripe = RuleThickness.toPx()

    clipPath(shapePath(ForeEdgeShape, Offset(left, top), Size(width, height))) {
        var x = 0f
        var index = 0
        while (x < width) {
            drawRect(
                color = if (index % 2 == 0) ForeEdgeLight else ForeEdgeDark,
                topLeft = Offset(left + x, top),
                size = Size(minOf(stripe, width - x), height),
            )
            x += stripe
            index++
        }
    }
}

/**
 * `background-image:var(--grain);background-blend-mode:soft-light` at the illustrations' effective
 * strength of 1.00 — no CSS `opacity` and no baked alpha, exactly as B1's cover (**D-013**: the
 * per-surface strengths are the specification, do not normalise them against the Bench's 0.15–0.25).
 *
 * Draws nothing below API 29 (**D-014**). The predicate and the blend both come from [ZinelyV2Grain] so
 * these two illustrations cannot drift from the paper's own policy.
 */
private fun DrawScope.drawGrain(grain: ShaderBrush) {
    if (!ZinelyV2Grain.IsSupported) return
    drawRect(brush = grain, alpha = IllustrationGrainAlpha, blendMode = ZinelyV2Grain.Blend)
}

/**
 * A [Shape]'s boundary as a [Path], placed at [topLeft] — so a clip can follow an asymmetric radius that
 * no `clipRect` expresses, and so the fore-edge's rounded sliver can be clipped where it actually sits,
 * outside the book's own bounds. B1's cover carries the same helper for the same reason.
 */
private fun DrawScope.shapePath(shape: Shape, topLeft: Offset, size: Size): Path =
    Path().apply {
        when (val outline = shape.createOutline(size, layoutDirection, this@shapePath)) {
            is Outline.Rectangle -> addRect(outline.rect)
            is Outline.Rounded -> addRoundRect(outline.roundRect)
            is Outline.Generic -> addPath(outline.path)
        }
        translate(topLeft)
    }

/**
 * `max-width:28ch` — twenty-eight advances of "0" in the used font, which is what the CSS `ch` unit *is*.
 *
 * Measured rather than approximated as a fraction of the screen, for the same reason B1's 9ch cover title
 * is: `ch` depends on Inter's own zero advance, so a fraction that looked right at one size would be wrong
 * at another and wrong again under font scaling.
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

/** `box-shadow:0 8px 16px -10px var(--shadow)`. The inset layer is a border — see [SheetIllustration]. */
private fun sheetShadow(shadow: Color): List<ZinelyV2ShadowLayer> = listOf(
    ZinelyV2ShadowLayer(dy = 8.dp, blur = 16.dp, spread = (-10).dp, color = shadow),
)

/** `box-shadow:0 10px 18px -10px var(--shadow)`. */
private fun bookShadow(shadow: Color): List<ZinelyV2ShadowLayer> = listOf(
    ZinelyV2ShadowLayer(dy = 10.dp, blur = 18.dp, spread = (-10).dp, color = shadow),
)

// ---------------------------------------------------------------------------------------------
// The frozen values, transcribed from `v2-library.html` at the lines named against each.
//
// Per-component literals, as B1's cover and B2's shelf have them: V2 publishes no spacing scale (D-007,
// ADR-074). 36 · 40 · 140 · 16 · 14 · 9 · 18 · 6 · 8 · 2 puts three of ten on the 8pt grid — the same
// evidence B2 recorded, and the same reason there is no `EmptySpacing` object here.
// ---------------------------------------------------------------------------------------------

/** `.empty{padding:36px 40px 140px;gap:16px}` — the 140 clears the dock. */
private val EmptyPaddingTop = 36.dp
private val EmptyPaddingHorizontal = 40.dp
private val EmptyPaddingBottom = 140.dp
private val EmptyGap = 16.dp

/** `.tf{gap:14px;margin-bottom:6px}`. */
private val TransformGap = 14.dp
private val TransformMarginBottom = 6.dp

/** `.tf .arrow{font-size:1.3rem;margin-bottom:18px}` = 20.8px. U+2192 RIGHTWARDS ARROW. */
private const val ArrowGlyph = "→"
private val ArrowSize = 20.8.sp
private val ArrowMarginBottom = 18.dp

/** `.tf .lbl{font-size:.66rem;letter-spacing:.1em;font-weight:700;margin-top:9px}` = 10.56px. */
private val LabelSize = 10.56.sp
private val LabelTracking = 0.1.em
private val LabelMarginTop = 9.dp

/** The captions as the DOM holds them, `:159` and `:161`. CSS uppercases them — see [TransformColumn]. */
private const val SheetLabelText = "one sheet"
private const val BookLabelText = "a little book"

/** `.empty h2` — `1.72rem` = 27.52px, `letter-spacing:-.01em`, `margin:8px 0 0`. Text at `:163`. */
private const val HeadlineText = "Make your first little zine."
private val HeadlineSize = 27.52.sp
private val HeadlineTracking = (-0.01).em
private val HeadlineMarginTop = 8.dp

/**
 * `.empty p` — `.95rem` = 15.2px, `line-height:1.55`, `max-width:28ch`. Text at `:164`.
 *
 * The em dash and the right single quote are U+2014 and U+2019, both present in every bundled Inter
 * weight (measured against the `cmap`s). They are the frozen characters and stay as frozen, per D-021.
 */
private const val BodyText =
    "One sheet of paper becomes a little eight-page book you print and fold yourself — " +
        "we’ll show you each step."
private val BodySize = 15.2.sp
private val BodyLineHeight = 1.55.em
private const val BodyMaxCharacters = 28

/** `.empty .pv` — `.76rem` = 12.16px, `font-weight:600`, `margin-top:2px`. Text at `:165`. */
private const val PrivacyText =
    "Everything you make stays on your phone — no account, nothing uploaded."
private val PrivacySize = 12.16.sp
private val PrivacyMarginTop = 2.dp

/** `.sheet-ill{width:92px;height:66px;border-radius:3px;background-size:90px 90px}`. */
private val SheetWidth = 92.dp
private val SheetHeight = 66.dp
private val SheetRadius = 3.dp
private val SheetShape: Shape = RoundedCornerShape(SheetRadius)
private val SheetGrainTile = 90.dp

/** `.sheet-ill i` — `width:1px`, verticals inset `6px`, horizontal inset `6px` and at `top:50%`. */
private val RuleThickness = 1.dp
private val RuleInset = 6.dp
private const val FirstFoldFraction = 0.33f
private const val SecondFoldFraction = 0.50f
private const val ThirdFoldFraction = 0.67f
private const val HalfwayFraction = 0.50f

/** `.v2{background:repeating-linear-gradient(… 0 3px,transparent 3px 6px);opacity:.5}`. */
private val DashLength = 3.dp
private const val DashAlpha = 0.5f

/** `.book-ill{width:52px;height:68px;border-radius:3px 5px 5px 3px;background-size:70px 70px}`. */
private val BookWidth = 52.dp
private val BookHeight = 68.dp
private val BookGrainTile = 70.dp

/** Absolute, not logical: a printed object does not mirror (**D-019**), as B1's cover does not. */
private val BookShape: Shape = AbsoluteRoundedCornerShape(
    topLeft = 3.dp,
    topRight = 5.dp,
    bottomRight = 5.dp,
    bottomLeft = 3.dp,
)

/** `.book-ill::before{left:5px;top:5px;bottom:5px;background:rgba(255,255,255,.25)}`. */
private val BookSpineInsetStart = 5.dp
private val BookSpineInsetVertical = 5.dp
private val BookSpine = Color.White.copy(alpha = 0.25f)

/** `.book-ill::after{right:-3px;top:4px;bottom:4px;width:3px}`. The radius is CSS-clamped — see above. */
private val ForeEdgeWidth = 3.dp
private val ForeEdgeInsetVertical = 4.dp
private val ForeEdgeRadius = 1.5.dp
private val ForeEdgeShape: Shape = AbsoluteRoundedCornerShape(
    topLeft = 0.dp,
    topRight = ForeEdgeRadius,
    bottomRight = ForeEdgeRadius,
    bottomLeft = 0.dp,
)
private val ForeEdgeLight = Color(0xFFF1EBDA)
private val ForeEdgeDark = Color(0xFFE3D9C2)

/** Effective grain strength on both illustrations: no baked alpha, no CSS opacity. See D-013. */
private const val IllustrationGrainAlpha = 1.0f
