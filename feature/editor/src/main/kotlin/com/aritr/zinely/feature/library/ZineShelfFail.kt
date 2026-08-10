package com.aritr.zinely.feature.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import com.aritr.zinely.ui.a11y.zinelyV2Control
import com.aritr.zinely.ui.components.zinelyV21HardShadow
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.ZinelyV21Press
import com.aritr.zinely.ui.theme.ZinelyV2Standard

/** `.fail` — the whole error column, so a test can assert its presence and its absence. */
internal const val ZineShelfFailTestTag: String = "shelf-fail"

/** `.retry` — the one control this state offers. */
internal const val ZineRetryTestTag: String = "shelf-retry"

/**
 * The frozen Library's **error** state — `docs/design/mockups/v21-library.html`.
 *
 * ```css
 * .fail{position:absolute;inset:0;flex-direction:column;align-items:center;justify-content:center;
 *       text-align:center;padding:var(--gap-2xl) var(--gap-2xl) 150px;gap:var(--gap-md)}
 * .fail h2{font-family:var(--voice);font-size:1.6rem;font-weight:700;margin:var(--gap-sm) 0 0}
 * .fail p{margin:0;color:var(--ink-soft);max-width:28ch;font-size:.94rem}
 * ```
 *
 * ### This state is design, not an implementer's improvisation
 *
 * The V2 Library was frozen with six hard-coded zines: a prototype never reads a store, so it never
 * waits and never fails. The owner ruled that *Loading and Error are product states; they belong in the
 * canonical design* ([D-024](docs/design/V2-SPEC-DEFECTS.md#d-024-ruling)), the frozen HTML was amended,
 * and **V2.1 was authored with all four states from the start** — which is the amendment holding.
 *
 * ### The empty state's own column, deliberately
 *
 * `.fail` repeats `.empty`'s box exactly — `inset:0`, centred column, `padding:36px 36px 150px`,
 * `gap:12px`, the same `28ch` paragraph. That is the ruling (*"Error reuses the Empty state's layout
 * grammar"*), and the reason is that this is the same **kind** of moment: the shelf has nothing to show.
 *
 * ### Reassurance precedes explanation
 *
 * The paragraph opens with *"Your zines are still on your phone"* and only then says what went wrong. On
 * a screen whose one question is *"which zine is mine?"*, a blank shelf reads as loss; the first clause
 * answers the fear. That order is asserted — a paragraph that says the same two things the other way
 * round is a different screen.
 *
 * ### The `!` mark is new in V2.1, and it is jam
 *
 * V2's error column opened with the headline. V2.1 opens with a rotated `!` badge in `jam` — the
 * destructive ink — which is the one place the re-freeze appears to contradict V2's own reasoning that
 * *"nothing here was destroyed"*. It does not: the badge is `jam` **outline and text on paper**, not a
 * jam fill, so it reads as a warning mark rather than as a deletion. `jamText` is what the contrast gate
 * requires for jam **as text**, and that is what [MarkText] uses; the border stays the `jam` the file
 * writes, because a 2px rule is not text.
 *
 * ### Retry is a quiet control, not a second primary
 *
 * `.retry` takes `--paper` with an `--ink` border and the **Raised** press tier, where `.start` takes a
 * `--leaf` fill, the **Hero** tier and the frame ring. `.start` is the screen's one primary action and
 * stands in this state too, so two filled buttons would make the recovery compete with the invitation.
 *
 * @param onRetry ask the store again. The shelf was not *lost*, so this simply re-reads.
 */
@Composable
internal fun ZineShelfFail(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ZinelyTheme.v21Colors

    Column(
        modifier = modifier
            .testTag(ZineShelfFailTestTag)
            .fillMaxSize()
            .padding(
                start = FailPaddingHorizontal,
                end = FailPaddingHorizontal,
                top = FailPaddingTop,
                bottom = zineDockClearance(FailPaddingBottom),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        // `gap:var(--gap-md)` and `justify-content:center`, as one arrangement.
        verticalArrangement = Arrangement.spacedBy(FailGap, Alignment.CenterVertically),
    ) {
        FailMark()

        // Averia 700 — the only weight the bundled voice face has above 400, and the file's own value.
        // D-005's Fraunces-500 ruling was about a typeface V2.1 does not use; §III Amendment 1 replaced
        // it, so this is transcription with nothing to override.
        Text(
            text = HeadlineText,
            style = TextStyle(
                fontFamily = ZinelyV21Fonts.Voice,
                fontWeight = FontWeight.Bold,
                fontSize = HeadlineSize,
                // `.fail h2` declares no line-height, unlike `.empty h2`'s 1.12 — so it inherits.
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                color = colors.ink,
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = HeadlineMarginTop)
                .semantics { heading() },
        )

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
            modifier = Modifier.widthIn(max = measureCharacterWidth(BodyMaxCharacters, bodyStyle)),
        )

        RetryButton(onRetry, Modifier.padding(top = RetryMarginTop))
    }
}

/**
 * `.fail .mk` — a 60px rotated disc with a `!` in it.
 *
 * ```css
 * .fail .mk{width:60px;height:60px;border-radius:var(--br-pill);background:var(--paper);
 *   border:2px solid var(--jam);color:var(--jam);font-family:var(--voice);font-size:1.8rem;
 *   font-weight:700;transform:rotate(-4deg);box-shadow:3px 3px 0 var(--jam)}
 * ```
 *
 * **Silent to TalkBack.** The headline immediately below says what the mark means, in words; a reader
 * announcing "!" first would spend a whole utterance on punctuation. `clearAndSetSemantics {}` rather
 * than a null description, because the `!` is a `Text` node and would otherwise be read as content.
 *
 * The shadow is `jam`, not `inkLine` — the one hard shadow in the corpus that is not the shadow colour,
 * and the reason V21-SPEC §4.3 records `inkLine` as **56** uses rather than "all of them".
 */
@Composable
private fun FailMark() {
    val colors = ZinelyTheme.v21Colors
    Box(
        Modifier
            .size(MarkSize)
            .graphicsLayer { rotationZ = MarkRotation }
            .zinelyV21HardShadow(MarkShadow, colors.jam, MarkShape)
            .clip(MarkShape)
            .background(colors.paper)
            .border(MarkBorder, colors.jam, MarkShape)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = MarkText,
            style = TextStyle(
                fontFamily = ZinelyV21Fonts.Voice,
                fontWeight = FontWeight.Bold,
                fontSize = MarkSize2,
                // `.fail .mk` declares no `line-height` — inherited, as everywhere else.
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                // `color:var(--jam)` as TEXT, which is what `jamText` exists for: plain `jam` on paper
                // measures 4.20:1 light / 4.55:1 dark, and V21-SPEC §5 ruled the text case onto the
                // darker token. The border above keeps `jam`, because a rule is not text.
                color = colors.jamText,
            ),
        )
    }
}

/**
 * `.retry` — `--paper` under an `--ink` border at pill radius, `.94rem` semibold, `12px 24px`, on the
 * **Raised** tier: `3px 3px 0` at rest, `translate(2px,2px)` and `1px 1px 0` pressed.
 *
 * The border is a real CSS `border`, which sits **inside** the element's box, so `Modifier.border` is
 * correct here — the opposite of the focus ring below, where an `outline` sits outside it.
 *
 * Its focus ring is `.start`'s. `.retry` declares none in V2.1, and borrowing the one ring the file does
 * author is transcription; inventing a second appearance is what D-008 forbids.
 */
@Composable
private fun RetryButton(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ZinelyTheme.v21Colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val pressed by interaction.collectIsPressedAsState()

    val press = ZinelyV21Press.Raised
    val duration = if (ZinelyTheme.v2Motion.reduceMotion) 0 else RetryPressDurationMillis
    val travel by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(durationMillis = duration, easing = ZinelyV2Standard),
        label = "zineRetryPress",
    )

    Text(
        text = RetryLabelText,
        style = TextStyle(
            fontFamily = ZinelyV21Fonts.Work,
            fontWeight = FontWeight.SemiBold,
            fontSize = RetryLabelSize,
            lineHeight = ZinelyV21Fonts.InheritedLineHeight,
            color = colors.ink,
        ),
        modifier = modifier
            // Before `zinelyV2Control`, which ends in `clearAndSetSemantics` and would swallow a tag
            // chained after it — the seam's own KDoc.
            .testTag(ZineRetryTestTag)
            .graphicsLayer {
                val t = press.travel.toPx() * travel
                translationX = t
                translationY = t
            }
            .zinelyV21HardShadow(
                offset = press.rest - (press.rest - press.pressed) * travel,
                color = colors.inkLine,
                shape = RetryShape,
            )
            .drawBehind { if (focused) drawRetryFocusRing(colors.ink) }
            .clip(RetryShape)
            .background(colors.paper)
            .border(RetryBorderWidth, colors.ink, RetryShape)
            .zinelyV2Control(
                label = RetryLabelText,
                onClick = onRetry,
                interactionSource = interaction,
            )
            .padding(
                horizontal = ZinelyV21Dimens.gapXl,
                vertical = ZinelyV21Dimens.gapMd,
            ),
    )
}

/** A CSS outline grows outward from its offset; see [ZineDock]'s ring for the full derivation. */
private fun DrawScope.drawRetryFocusRing(ink: Color) {
    val stroke = RetryFocusWidth.toPx()
    val out = RetryFocusOffset.toPx() + stroke / 2f
    drawRoundRect(
        color = ink,
        topLeft = Offset(-out, -out),
        size = Size(size.width + 2 * out, size.height + 2 * out),
        cornerRadius = CornerRadius(ZinelyV21Dimens.radiusPill.toPx() + out),
        style = Stroke(width = stroke),
    )
}

/**
 * `max-width:28ch` — the advance width of 28 `0` glyphs in the paragraph's own style, which is what the
 * CSS `ch` unit measures. [ZineShelfEmpty] measures its own the same way and for the same reason.
 */
@Composable
private fun measureCharacterWidth(count: Int, style: TextStyle): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(measurer, density, style, count) {
        val zeros = measurer.measure("0".repeat(count), style, softWrap = false)
        with(density) { zeros.size.width.toDp() }
    }
}

// ---------------------------------------------------------------------------------------------
// The frozen values, transcribed from `v21-library.html`.
// ---------------------------------------------------------------------------------------------

/** `.fail{padding:var(--gap-2xl) var(--gap-2xl) 150px;gap:var(--gap-md)}` — `.empty`'s box exactly. */
private val FailPaddingTop = ZinelyV21Dimens.gap2Xl
private val FailPaddingHorizontal = ZinelyV21Dimens.gap2Xl
private val FailPaddingBottom = 150.dp
private val FailGap = ZinelyV21Dimens.gapMd

/** `.fail .mk{width:60px;height:60px;border:2px;font-size:1.8rem;rotate(-4deg);box-shadow:3px 3px 0}` */
private val MarkSize = 60.dp
private val MarkSize2 = 28.8.sp
private val MarkBorder = 2.dp
private val MarkShadow = 3.dp
private const val MarkRotation = -4f
private val MarkShape: Shape = RoundedCornerShape(ZinelyV21Dimens.radiusPill)
private const val MarkText = "!"

/** `.fail h2{font-size:1.6rem;margin:var(--gap-sm) 0 0}` = 25.6px. */
private const val HeadlineText = "Your shelf didn’t open."
private val HeadlineSize = 25.6.sp
private val HeadlineMarginTop = ZinelyV21Dimens.gapSm

/**
 * `.fail p{font-size:.94rem;max-width:28ch}` = 15.04px, over the body's own `line-height:1.55`.
 *
 * The reassurance is the first clause and the explanation is the second; the em dash is U+2014 and the
 * apostrophe above is U+2019, both frozen characters kept exactly as frozen (D-021).
 */
private const val BodyText =
    "Your zines are still on your phone — something went wrong reading the shelf."
private val BodySize = 15.04.sp
private val BodyLineHeight = 1.55.em
private const val BodyMaxCharacters = 28

/**
 * The two halves the order assertion names, so a test states the claim in the frozen file's own words
 * rather than in a substring literal of its own invention.
 */
internal const val ZineShelfFailReassurance: String = "Your zines are still on your phone"
internal const val ZineShelfFailExplanation: String = "something went wrong reading the shelf"

/** `.retry{font-size:.94rem;padding:var(--gap-md) var(--gap-xl);margin-top:var(--gap-sm)}`. */
private const val RetryLabelText = "Try again"
private val RetryLabelSize = 15.04.sp
private val RetryShape: Shape = RoundedCornerShape(ZinelyV21Dimens.radiusPill)
private val RetryBorderWidth = 1.5.dp
private val RetryMarginTop = ZinelyV21Dimens.gapSm

/** No `transition` is declared on `.retry`; `.start`'s .14s is the corpus's own press duration. */
private const val RetryPressDurationMillis = 140

/** `.start:focus-visible{outline:2px solid var(--ink);outline-offset:5px}`, borrowed. */
private val RetryFocusWidth = 2.dp
private val RetryFocusOffset = 5.dp
