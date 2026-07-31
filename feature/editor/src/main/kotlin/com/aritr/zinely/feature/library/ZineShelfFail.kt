package com.aritr.zinely.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.aritr.zinely.ui.a11y.zinelyV2Control
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV2Dimens

/** `.fail` — the whole error column, so a test can assert its presence and its absence. */
internal const val ZineShelfFailTestTag: String = "shelf-fail"

/** `.retry` — the one control this state offers. */
internal const val ZineRetryTestTag: String = "shelf-retry"

/**
 * The frozen Library's **error** state — `v2-library.html:138-150`, `:204-208`.
 *
 * ```css
 * .fail{position:absolute;inset:0;display:none;flex-direction:column;align-items:center;
 *       justify-content:center;text-align:center;padding:36px 40px 140px;gap:16px}
 * .fail h2{font-size:1.72rem;margin:8px 0 0;font-weight:600;letter-spacing:-.01em}
 * .fail p{margin:0;color:var(--ink-soft);max-width:28ch;line-height:1.55;font-size:.95rem}
 * .retry{margin-top:6px;background:var(--paper);color:var(--ink);border:1px solid var(--hair);
 *        border-radius:12px;font-size:.95rem;font-weight:600;padding:13px 22px}
 * body.is-error .shelf{display:none} body.is-error .empty{display:none} body.is-error .fail{display:flex}
 * ```
 *
 * ### This state is design, not an implementer's improvisation
 *
 * The Library was frozen with **six hard-coded zines**: a prototype never reads a store, so it never waits
 * and never fails. B5's planning table found that and raised it rather than filling the gap from V1
 * ([ADR-086](docs/DECISIONS.md#adr-086)), and the owner ruled that *Loading and Error are product states;
 * they belong in the canonical design* — so the **frozen HTML was amended**
 * ([D-024](docs/design/V2-SPEC-DEFECTS.md#d-024-ruling)) and this file transcribes the amendment. It is the
 * first V2 amendment that **adds** design rather than deleting dead specification.
 *
 * ### The empty state's own column, deliberately
 *
 * `.fail` repeats `.empty`'s box exactly — `inset:0`, centred column, `padding:36px 40px 140px`, `gap:16px`,
 * the same `1.72rem` serif headline and the same `.95rem`/`28ch`/`1.55` paragraph. That is the ruling
 * (*"Error reuses the Empty state's layout grammar"*, *"Do not introduce a second workspace grammar"*),
 * and the reason is that this is the same **kind** of moment: the shelf has nothing to show. A second
 * layout would be a second design for one situation.
 *
 * ### Reassurance precedes explanation
 *
 * The paragraph opens with *"Your zines are still on your phone"* and only then says what went wrong. On a
 * screen whose one question is *"which zine is mine?"*, a blank shelf reads as loss; the first clause is
 * the one that answers the fear, and the order is the ruling's, not a copy preference. That order is
 * asserted — a paragraph that says the same two things the other way round is a different screen.
 *
 * ### `--consequence` is deliberately unused
 *
 * The destructive ink is reserved for delete (`.act.danger`), and **nothing here was destroyed** — the
 * zines are files on this device and they are still there. Painting a failed *read* in the colour of a
 * deletion would tell the user the opposite of the sentence above it.
 *
 * ### Retry is a quiet control, not a second primary
 *
 * `.retry` takes `--paper` + a hairline border — the grammar the action sheet's rows already use — because
 * `.start` is the screen's one primary action and **stands in this state too** (the dock is workspace, not
 * loaded content). Two matcha buttons on one screen would make the recovery compete with the invitation.
 * Asserted as *no pixel of the retry is `--matcha`*, since "it looks quiet" is not checkable and the
 * mutation (style it as `.start`) is a one-line change that a screenshot ratifies.
 *
 * @param onRetry ask the store again. The shelf was not *lost*, so this simply re-reads — see
 *   `HomeViewModel.retry`, whose fresh subscription is what makes a re-ask meaningful.
 */
@Composable
internal fun ZineShelfFail(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ZinelyTheme.v2Colors

    Column(
        modifier = modifier
            .testTag(ZineShelfFailTestTag)
            .fillMaxSize()
            .padding(
                start = FailPaddingHorizontal,
                end = FailPaddingHorizontal,
                top = FailPaddingTop,
                bottom = FailPaddingBottom,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        // `gap:16px` and `justify-content:center`, as one arrangement — [ZineShelfEmpty]'s shape.
        verticalArrangement = Arrangement.spacedBy(FailGap, Alignment.CenterVertically),
    ) {
        // **Fraunces 500, not the file's 600** — `.fail h2` is `.empty h2`'s twin by construction, and
        // D-005 governs the register rather than the individual selector. See [ZineShelfEmpty].
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
            modifier = Modifier.widthIn(max = measureCharacterWidth(BodyMaxCharacters, bodyStyle)),
        )

        RetryButton(onRetry, Modifier.padding(top = RetryMarginTop))
    }
}

/**
 * `.retry` — `--paper` over a 1px `--hair` border at radius 12, `.95rem` semibold, `13px 22px`.
 *
 * The border is a real CSS `border`, which sits **inside** the element's box, so `Modifier.border` is
 * correct here — the opposite of the focus ring below, where an `outline` sits outside it. Getting these
 * two the same way round is why `.start`'s ring is stroked rather than bordered ([ZineDock]).
 */
@Composable
private fun RetryButton(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ZinelyTheme.v2Colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    Text(
        text = RetryLabelText,
        style = TextStyle(
            fontFamily = ZinelyTheme.v2Typography.work,
            fontWeight = FontWeight.SemiBold,
            fontSize = RetryLabelSize,
            color = colors.ink,
        ),
        modifier = modifier
            // Before `zinelyV2Control`, which ends in `clearAndSetSemantics` and would swallow a tag
            // chained after it — the seam's own KDoc, and the trap B4 documented.
            .testTag(ZineRetryTestTag)
            // `.retry:focus-visible{outline:2px solid var(--matcha-text);outline-offset:3px}` — matcha
            // text ink, not `--ink`: this control is not `.start` and does not borrow its ring either.
            .drawBehind { if (focused) drawRetryFocusRing(colors.matchaText) }
            .clip(RetryShape)
            .background(colors.paper)
            .border(RetryBorderWidth, colors.hair, RetryShape)
            .zinelyV2Control(
                label = RetryLabelText,
                onClick = onRetry,
                interactionSource = interaction,
            )
            .padding(horizontal = RetryPaddingHorizontal, vertical = RetryPaddingVertical),
    )
}

/** A CSS outline grows outward from its offset; see [ZineDock]'s ring for the full derivation. */
private fun DrawScope.drawRetryFocusRing(ink: Color) {
    val stroke = ZinelyV2Dimens.FocusRingWidth.toPx()
    val out = RetryFocusOffset.toPx() + stroke / 2f
    drawRoundRect(
        color = ink,
        topLeft = Offset(-out, -out),
        size = Size(size.width + 2 * out, size.height + 2 * out),
        cornerRadius = CornerRadius(RetryRadius.toPx() + out),
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
// The frozen values, transcribed from the amended `v2-library.html`. Per-component literals, as every
// other V2 component has them: V2 publishes no spacing scale (D-007, ADR-074).
// ---------------------------------------------------------------------------------------------

/** `.fail{padding:36px 40px 140px;gap:16px}` — identical to `.empty`'s, which is the point. */
private val FailPaddingTop = 36.dp
private val FailPaddingHorizontal = 40.dp
private val FailPaddingBottom = 140.dp
private val FailGap = 16.dp

/** `.fail h2` — `1.72rem` = 27.52px, `letter-spacing:-.01em`, `margin:8px 0 0`. Text at `:205`. */
private const val HeadlineText = "Your shelf didn’t open."
private val HeadlineSize = 27.52.sp
private val HeadlineTracking = (-0.01).em
private val HeadlineMarginTop = 8.dp

/**
 * `.fail p` — `.95rem` = 15.2px, `line-height:1.55`, `max-width:28ch`. Text at `:206`.
 *
 * The reassurance is the first clause and the explanation is the second; the em dash is U+2014 and the
 * apostrophe above is U+2019, both frozen characters kept exactly as frozen (D-021).
 */
private const val BodyText =
    "Your zines are still on your phone — something went wrong reading the shelf."
private val BodySize = 15.2.sp
private val BodyLineHeight = 1.55.em
private const val BodyMaxCharacters = 28

/**
 * The two halves the order assertion names, so a test states the claim in the frozen file's own words
 * rather than in a substring literal of its own invention.
 */
internal const val ZineShelfFailReassurance: String = "Your zines are still on your phone"
internal const val ZineShelfFailExplanation: String = "something went wrong reading the shelf"

/** `.retry` — `.95rem` = 15.2px, `padding:13px 22px`, `border-radius:12px`, `margin-top:6px`. */
private const val RetryLabelText = "Try again"
private val RetryLabelSize = 15.2.sp
private val RetryPaddingVertical = 13.dp
private val RetryPaddingHorizontal = 22.dp
private val RetryRadius = 12.dp
private val RetryShape: Shape = RoundedCornerShape(RetryRadius)
private val RetryBorderWidth = 1.dp
private val RetryMarginTop = 6.dp

/** `.retry:focus-visible{outline-offset:3px}`; the 2px width is [ZinelyV2Dimens.FocusRingWidth]. */
private val RetryFocusOffset = 3.dp
