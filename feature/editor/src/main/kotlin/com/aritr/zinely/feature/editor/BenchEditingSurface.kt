package com.aritr.zinely.feature.editor

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.TextCoverage
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlin.math.roundToInt
import com.aritr.zinely.core.model.TextAlign as ModelTextAlign

/**
 * Frozen `edit()`: `pageWrap.style.transform='translateY(-96px)'` (`v2-bench.html:551`), settled back to
 * `translateY(0)` by `endEdit()` (`:558`) — [ADR-093](../../../../../../../../docs/DECISIONS.md#adr-093)
 * rows 3.1 and 3.2.
 *
 * **Ported as the literal frozen value, with its scale caveat recorded rather than silently corrected**
 * (row 3.1a). In the prototype −96px lifts a 324px-tall page inside a 744px-tall phone — 29.6 % of the
 * page. On a real device the page is far taller relative to the same 96dp, so the same constant clears
 * proportionally less. The freeze specifies a distance, not a fraction, so the distance is what ships.
 *
 * ⚠ **Implementing it surfaced the failure row 3.1a predicted, in the direction it did not** — see
 * [D-043](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-043). The frozen `.pageWrap` has slack
 * above the page to lift into (a 324px page inside a taller canvas); the shipped page is *contained* in
 * the canvas and typically height-bound, so its slack is near zero. An element in the **top 96dp** of the
 * page is therefore lifted clean out of view by its own edit gesture — reproduced deterministically, not
 * inferred: with the pan neutralised the two `EditorScreenTest` session tests pass, and with the literal
 * in place the field lands above the canvas.
 *
 * ✅ **Resolved 2026-08-03 by owner ruling OD-16, and the frozen file was amended first.** This constant is
 * now the **maximum** lift rather than the lift: `v2-bench.html`'s `edit()` computes
 * `min(96, slack + clearance)` and its header records the amendment. The value is unchanged — see
 * [benchEditPanMagnitudeDp] for the rule that consumes it.
 */
internal val BenchEditPanDp = (-96).dp

/**
 * How far the page actually leans up when a text session opens — frozen `edit()` as amended by
 * [OD-16](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-043-ruling), 2026-08-03.
 *
 * ```
 * lift = min(96dp, slackAbovePage + clearanceNeeded)
 * ```
 *
 * **Why the frozen literal could not stay unconditional.** −96px is affordable in the prototype because a
 * 229×324 `.page` sits inside a 344×744 `.phone` with a band of empty canvas above it. The shipped Bench
 * *contains* the page (`scale = min(w/pageW, h/pageH)`, height-bound on a portrait page), so that band is
 * ≈ 0 — measured 4.2dp on `SM-A176B` in the pre-amendment round, and 0 within a pixel on the amended
 * build; the two readings are reconciled in ADR-093 §8 and bracket this device at 0–4dp against a 96dp demand. The lift came out of the page, and on device
 * out of the window.
 *
 * **The two terms.**
 * - [slackAboveDp] — the empty canvas band above the sheet. Spending it is free: the page cannot leave a
 *   canvas it has not yet reached the top of.
 * - `elementBottomDp - occluderTopDp` — how far the edited element's **bottom** currently sits below the
 *   docked `.kbstack` + IME. Zero when the element is already clear, which is the top-of-page case: there
 *   the lift collapses to the slack alone and the page stays inside its canvas.
 *
 * Both are measured at **rest**, from the un-panned geometry, so the result cannot chase itself: this is a
 * function of where things are before the gesture, not of where the gesture puts them.
 *
 * **What it deliberately does not do.** It never lifts *more* than 96dp. An element deep at the page bottom
 * can need more clearance than the ceiling allows and keeps part of its box behind the row — the typed line
 * still clears it. That is the priced cost of the ruling's word *maximum*, and it is recorded on
 * [D-043](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-043) rather than fixed by inference.
 *
 * Returns a non-negative **magnitude**; the caller applies the sign.
 */
internal fun benchEditPanMagnitudeDp(
    maxPanDp: Float,
    slackAboveDp: Float,
    elementBottomDp: Float,
    occluderTopDp: Float,
): Float {
    if (maxPanDp <= 0f) return 0f
    val slack = slackAboveDp.coerceAtLeast(0f)
    val clearance = (elementBottomDp - occluderTopDp).coerceAtLeast(0f)
    return (slack + clearance).coerceAtMost(maxPanDp)
}

/**
 * Frozen `.pageWrap{transition:transform .34s var(--settle)}` (`v2-bench.html:172-173`) — the same
 * `.34s` settle the keyboard stack rides in on, so the page and the row move as one gesture.
 */
internal const val BenchEditPanMillis: Int = 340

/** Frozen `.caret{width:1.5px}` (`v2-bench.html:204`). */
internal val BenchCaretWidth = 1.5.dp

/** Frozen `.caret{height:1.05em}` (`v2-bench.html:204`) — a multiple of the element's own font size. */
internal const val BenchCaretHeightEm: Float = 1.05f

/** Frozen `@keyframes blink{50%{opacity:0}}` at `1.05s steps(1)` (`v2-bench.html:204,206`). */
internal const val BenchCaretBlinkMillis: Int = 1050

/**
 * The frozen caret's opacity at a moment in the blink — `blink 1.05s steps(1)`, i.e. a **square wave**:
 * fully on for the first half of the period, fully off for the second, with no fade between (that is what
 * `steps(1)` means, and a linear interpolation would be a different caret).
 *
 * **Reduced motion holds it on.** The blink is the frozen trilogy's only looping animation and the whole
 * subject of [D-012](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-012), which is open and
 * **C9's** — so C3 takes the one reading all three frozen files agree on: it does not run
 * ([ADR-093](../../../../../../../../docs/DECISIONS.md#adr-093) row 3.8). A caret that stops blinking is
 * still a caret; one that strobes at 0.95 Hz next to a user who asked for less motion is a defect.
 *
 * Pure so the property is testable without a frame clock — the composable feeds it elapsed animation time
 * and does nothing else.
 */
internal fun benchCaretAlphaAt(elapsedMillis: Long, reduceMotion: Boolean): Float = when {
    reduceMotion -> 1f
    elapsedMillis.mod(BenchCaretBlinkMillis.toLong()) < BenchCaretBlinkMillis / 2 -> 1f
    else -> 0f
}

/**
 * The frozen in-place editing surface — the text is edited **on the page, at its own box**, rather than in
 * a detached sheet at the bottom of the screen ([ADR-093](../../../../../../../../docs/DECISIONS.md#adr-093)
 * rows 3.8 and 3.11; frozen `.block.text .surface[contenteditable]` under `.editing`).
 *
 * ### Why the field must replace the rendered element, not sit over it
 *
 * The page is painted by [PagePreview] — a `drawIntoCanvas` replay of the export tape through
 * [com.aritr.zinely.render.android.SharedTextLayout]. Composing an editable field over a box the tape is
 * still painting would show the text **twice**, offset by whatever the two layout engines disagree about,
 * which reads as the artifact having duplicated itself. So the host suppresses this element from the tape
 * for the life of the session (`EditorPagePreview(hiddenElementId = …)`) and this field is the only
 * drawing of it. One text on screen, always.
 *
 * ### The parity this can and cannot promise (row 3.11)
 *
 * Everything the two engines *both* take from the model is matched exactly here: the box rect, the point
 * size scaled by the live `screenPxPerPt`, the ink, the alignment, bold and italic. Line **breaking** is
 * matched as closely as the two APIs allow — `includeFontPadding = false` and a 1.0 line-height mirror the
 * renderer's `setIncludePad(false)` / `setLineSpacing(0f, 1f)`, and Compose's default simple break strategy
 * matches its `BREAK_STRATEGY_SIMPLE` — but they are not the same code path, so a long line may wrap one
 * word differently and shift on commit. That residue is a **device-verification** item, not something a
 * unit test can settle, and it is the honest limit of this approach: the alternative is editing through the
 * export replayer, which would mean building a caret and hit-testing on a canvas, i.e. a text engine.
 *
 * ### The caret (row 3.8)
 *
 * Frozen `.caret{width:1.5px;height:1.05em;background:var(--matcha);animation:blink 1.05s steps(1)}`
 * (`:204-205`). The platform cursor is **suppressed** (`cursorColor = Transparent`) and the caret drawn
 * here instead, because `BasicTextField` exposes only a brush — width, height and blink period are not
 * settable on it, and row 3.8 additionally requires the blink **not to run** under reduced motion, which
 * the platform cursor will not honour. Drawing it costs the field's layout and selection, which is the
 * only reason [EditTextSession] gained `onTextLayout`/`onDraftChanged`; placement still comes from
 * `TextLayoutResult.getCursorRect`, so this owns no text metrics of its own.
 *
 * The colour is the change this row actually makes: the shipped caret was `coralStrong`, cited to **V1**'s
 * `bench.html`. V2's Bench says `--matcha` — a canonical amendment either way, recorded on row 3.8.
 *
 * @param session the open edit session — its `id`/`token` scope the commit.
 * @param element the [TextElement] being edited; supplies both the text and the geometry/style to match.
 * @param screenPxPerPt the live viewport scale, so the field tracks a canvas resize like every other layer.
 * @param pageOffset the live page offset (centring/pan), same seam every other layer maps through.
 * @param modifier placement from the host; the offset and size are applied on top of it.
 */
@Composable
internal fun BenchEditingSurface(
    session: Interaction.EditingText,
    element: TextElement,
    dispatch: (Intent) -> Unit,
    screenPxPerPt: Float,
    pageOffset: PtPoint,
    modifier: Modifier = Modifier,
    onCoverageChanged: (TextCoverage) -> Unit = {},
) {
    val density = LocalDensity.current
    val t = element.transform
    // The same page→device mapping every sibling layer uses (`devicePx = (pagePt + pageOffset) × scale`),
    // written out rather than routed through ExportScale because only the rect is needed, not a matrix.
    val xPx = ((t.xPt + pageOffset.x) * screenPxPerPt).toFloat()
    val yPx = ((t.yPt + pageOffset.y) * screenPxPerPt).toFloat()
    val wPx = (t.widthPt * screenPxPerPt).toFloat()
    val hPx = (t.heightPt * screenPxPerPt).toFloat()

    val style = element.style
    // Point size × the live scale, in px — NOT scaled by the user's font-scale preference, because the
    // artifact is not. A zine set at 14pt prints at 14pt whatever the phone's text size is set to.
    val fontSizePx = (style.sizePt * screenPxPerPt).toFloat()
    val ink = Color(style.color.r, style.color.g, style.color.b, style.color.a)

    val textStyle = TextStyle(
        // The bundled Inter the export/preview path resolves to, so the draft and the baked artifact share
        // glyphs and metrics. Deliberately the V2 face rather than V1's `typography.shell`, which is the
        // same four resources under a family this phase retires.
        fontFamily = ZinelyTheme.v2Typography.work,
        fontSize = with(density) { fontSizePx.toSp() },
        color = ink,
        fontWeight = if (style.bold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal,
        textAlign = when (style.align) {
            ModelTextAlign.START -> TextAlign.Start
            ModelTextAlign.CENTER -> TextAlign.Center
            ModelTextAlign.END -> TextAlign.End
        },
        // Mirrors SharedTextLayout's `setLineSpacing(0f, 1f)` + `setIncludePad(false)`: unit line spacing
        // with no font padding, and the trim that stops Compose re-adding it at the first and last line.
        lineHeight = 1.em,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Proportional,
            trim = LineHeightStyle.Trim.Both,
        ),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    // The frozen caret is drawn here rather than tinted on the platform cursor, because three of its four
    // properties are not settable on that cursor — see [benchCaretAlphaAt]. Tracking it needs the field's
    // measured layout and the live selection, which is why they escape [EditTextSession].
    var layout by remember(session.token) { mutableStateOf<TextLayoutResult?>(null) }
    var cursor by remember(session.token) { mutableIntStateOf(0) }
    val reduceMotion = ZinelyTheme.motion.reduceMotion
    // `withInfiniteAnimationFrameMillis` honours the ambient InfiniteAnimationPolicy, which the Compose
    // test rule installs — so this ticks on a device and suspends under test rather than blocking idle.
    // Not composed at all under reduced motion, so "it does not run" is structural, not a branch on alpha.
    //
    // It yields the SYSTEM frame clock, so the elapsed time is measured from a baseline taken on the first
    // frame of this session. Without that subtraction the phase would be arbitrary and a session could
    // open in the blink's off half — up to 525ms with no caret at the moment the user taps to type. The
    // freeze does not have that problem because `display:none → inline-block` restarts the animation, i.e.
    // the frozen caret always starts **on**; the baseline is what reproduces that.
    val elapsed by if (reduceMotion) {
        remember { mutableStateOf(0L) }
    } else {
        produceState(0L, session.token) {
            var origin = -1L
            while (true) {
                withInfiniteAnimationFrameMillis { now ->
                    if (origin < 0) origin = now
                    value = now - origin
                }
            }
        }
    }
    val caretAlpha = benchCaretAlphaAt(elapsed, reduceMotion)
    val caretColour = ZinelyTheme.v2Colors.matcha
    val caretWidthPx = with(density) { BenchCaretWidth.toPx() }
    val caretHeightPx = fontSizePx * BenchCaretHeightEm

    EditTextSession(
        session = session,
        element = element,
        dispatch = dispatch,
        modifier = modifier
            .offset { IntOffset(xPx.roundToInt(), yPx.roundToInt()) }
            .size(with(density) { wPx.toDp() }, with(density) { hPx.toDp() })
            .drawWithContent {
                drawContent()
                val measured = layout ?: return@drawWithContent
                if (caretAlpha <= 0f) return@drawWithContent
                // Frozen `.caret{display:none}` / `.editing .caret{display:inline-block}` (`:204-205`):
                // the caret exists only while a session is open, which this whole composable already is.
                val rect = measured.getCursorRect(cursor.coerceIn(0, measured.layoutInput.text.length))
                drawRect(
                    color = caretColour.copy(alpha = caretAlpha),
                    topLeft = Offset(rect.left, rect.bottom - caretHeightPx),
                    size = Size(caretWidthPx, caretHeightPx),
                )
            },
        onCoverageChanged = onCoverageChanged,
        textStyle = textStyle,
        // The platform cursor is suppressed, not recoloured — two carets would be two carets.
        cursorColor = Color.Transparent,
        onTextLayout = { layout = it },
        // `end`, not `start`: for a collapsed selection they are the same, and for a range the caret
        // belongs at the edge the user is moving — after a select-all, `start` draws it at the beginning
        // of the text, which is the one place the insertion point is not.
        onDraftChanged = { cursor = it.selection.end },
    )
}
