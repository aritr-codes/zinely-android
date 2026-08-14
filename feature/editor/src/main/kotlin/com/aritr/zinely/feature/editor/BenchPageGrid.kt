package com.aritr.zinely.feature.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.ui.components.zinelyV21Pressable
import com.aritr.zinely.ui.theme.ZinelyHaptic
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Colors
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.ZinelyV21Press
import com.aritr.zinely.ui.theme.ZinelyV2IconPaint
import com.aritr.zinely.ui.theme.ZinelyV2Icons
import com.aritr.zinely.ui.theme.toImageVector
import com.aritr.zinely.ui.theme.zinelyV21LightColors
import kotlin.math.roundToInt

/** Test tag on the frozen `.pgrid` panel. It exists **only while summoned** (row 5.11a). */
public const val BenchPageGridTestTag: String = "bench-page-grid"

/**
 * Test tag on the frozen `.scrim` the grid raises with itself (`v21-bench.html:376-378`, `openGrid()`
 * at `:783`).
 */
public const val BenchPageGridScrimTestTag: String = "bench-page-grid-scrim"

/**
 * Test tag on the grid's `.dclose`.
 *
 * ⚠ **Renamed from `BenchPageGridDoneTag`.** V2's grid was dismissed by a `Done` *word*; V2.1's
 * `.pgrid` head carries the frozen `.dclose` cross instead (`v21-bench.html:462-465`, `openGrid()` at
 * `:780`). The control is the same control and the tag is the same tag, but a tag reading `done` on a
 * button labelled *Close* is the kind of small lie that survives three packages.
 */
public const val BenchPageGridCloseTag: String = "bench-page-grid-close"

/** Per-cell test tag, 1-based like the frozen `openGrid()`. */
public fun benchPageCellTag(pageNumber: Int): String = "bench-page-cell-$pageNumber"

/**
 * Frozen `.pgrid{border-radius:var(--br-xl) var(--br-xl) 0 0; border-top:2px solid var(--ink);
 * max-height:78%; padding:0 var(--gap-lg) var(--gap-xl)}` and its `translateY(103%) → 0` over
 * `.3s cubic-bezier(.05,.7,.1,1)` (`v21-bench.html:444-448`).
 *
 * ⚠ **This is the package's largest single change, and it is not a colour.** V2's grid was
 * `position:absolute; inset:0` — an opaque `--desk` overlay that *replaced* the canvas. V2.1's is the
 * same bottom sheet the supply drawer uses (`.sheet`, `:379-383`, byte-identical but for the max
 * height): a `--paper` panel that rises over a scrim with the page still visible above it. The 102%
 * of V2's slide is 103% here, and the two are transcribed rather than reconciled.
 */
internal val BenchGridPanelRadius = ZinelyV21Dimens.radiusXl
internal val BenchGridPanelRule = 2.dp
internal val BenchGridPanelPaddingH = ZinelyV21Dimens.gapLg
internal val BenchGridPanelPaddingBottom = ZinelyV21Dimens.gapXl
internal const val BenchGridMaxHeightFraction: Float = 0.78f
internal const val BenchGridEnterPercent: Float = 1.03f
internal const val BenchGridEnterMillis: Int = 300

/**
 * Frozen `.scrim{background:rgba(38,26,16,.44); transition:opacity .22s}` (`v21-bench.html:376-378`).
 *
 * A literal, because the frozen file writes a literal: no `--scrim` token exists in `:root`, so there
 * is nothing in [ZinelyV21Colors] to read it from and inventing one would be a token this corpus does
 * not have. `.44 × 255 = 112 = 0x70`.
 *
 * ⚠ The scrim is **shared chrome** — `.sheet` raises the same one (`showSheet()`, `:837`). It is drawn
 * here because P5's panel is the first V2.1 surface that needs it and the chooser is still on V1's
 * [com.aritr.zinely.ui.components.ZSheet], which brings its own. Whoever converts `.sheet` should hoist
 * this rather than write a second one.
 */
internal val BenchGridScrimColor = Color(0x70261A10)
internal const val BenchGridScrimMillis: Int = 220

/**
 * Frozen `.grip{44×5; border-radius:var(--br-pill); background:var(--ink-faint); opacity:.5;
 * margin:var(--gap-md) auto var(--gap-xs)}` (`v21-bench.html:384-385`).
 *
 * `--ink-faint` is legal here and only here on this surface: it sets **no text** in V2.1 (the two-tier
 * ramp is `ink` / `inkSoft`), and the grip is not text — it is a drawn bar at half opacity. The card's
 * number, which *was* `--ink-faint`, is [benchGridCardIsland]'s `inkSoft` now.
 */
internal val BenchGripWidth = 44.dp
internal val BenchGripHeight = 5.dp
internal val BenchGripPaddingTop = ZinelyV21Dimens.gapMd
internal val BenchGripPaddingBottom = ZinelyV21Dimens.gapXs
internal const val BenchGripOpacity: Float = 0.5f

/** Frozen `.pghead{margin:var(--gap-xs) var(--gap-hair) var(--gap-md)}` (`v21-bench.html:449`). */
internal val BenchGridHeadPaddingTop = ZinelyV21Dimens.gapXs
internal val BenchGridHeadPaddingH = ZinelyV21Dimens.gapHair
internal val BenchGridHeadPaddingBottom = ZinelyV21Dimens.gapMd

/**
 * The head's `<h3>` — `.sheet h3{font-family:var(--voice); font-weight:700; font-size:1.2rem}`
 * (`v21-bench.html:386`), 19.2sp at the prototype's 16px root.
 *
 * **A transcription with a gap in it, recorded rather than smoothed over.** `.pghead h3{margin:0}`
 * (`:450`) is the *only* rule the frozen file writes for this element, and it sets margin alone — so
 * the panel's title has no declared face, weight or size and would fall to the browser's default. That
 * rule exists to *neutralise* `.sheet h3`'s margins, which is only a sensible thing to write if its
 * author believed the rest of `.sheet h3` applied; `.pgrid` is otherwise a byte-for-byte copy of
 * `.sheet`. So the sheet title's type is adopted and the margin comes from `.pghead`. A browser
 * default is not a specification.
 *
 * ⚠ V2 set this at 17sp/500 in the voice; the weight is a real Bold now.
 */
internal val BenchGridTitleSize = 19.2.sp

/**
 * Frozen `.dclose{34×34; border-radius:var(--br-pill); border:1.5px solid var(--ink);
 * background:var(--paper); color:var(--ink-soft); box-shadow:2px 2px 0 var(--ink-line)}` and its
 * `svg{15px; stroke-width:2.2; stroke-linecap:round}` (`v21-bench.html:462-465`).
 *
 * Drawn at 34dp inside a 48dp target — [D-009](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-009)'s
 * *extend the target, keep the paint*, the same split `:core:ui`'s own `.dclose` uses. Visual parity
 * and a reachable control were only ever in tension because the two sizes were conflated.
 */
internal val BenchGridCloseSize = 34.dp
internal val BenchGridCloseTarget = 48.dp
internal val BenchGridCloseGlyphSize = 15.dp
internal const val BenchGridCloseGlyphStroke: Float = 2.2f

/** Frozen `.pgg{grid-template-columns:repeat(3,1fr); gap:var(--gap-md)}` (`v21-bench.html:451`). */
internal const val BenchGridColumns: Int = 3
internal val BenchGridGap = ZinelyV21Dimens.gapMd

/**
 * Frozen `.pgc{aspect-ratio:3/4; border-radius:var(--br-sm)}` (`v21-bench.html:439`).
 *
 * The aspect is width ÷ height, so **0.75** — a page-shaped card, where V2's `.66` was taller than any
 * paper the app imposes.
 */
internal const val BenchCellAspect: Float = 0.75f
internal val BenchCellRadius = ZinelyV21Dimens.radiusSm

/**
 * Frozen `.pgc{font-size:.72rem; font-weight:700; font-variant-numeric:tabular-nums}`
 * (`v21-bench.html:459-460`). `.72rem` against the prototype's 16px root is 11.52sp.
 *
 * ⚠ **Two defects close on this one line**, and they are different defects with different arguments —
 * see [ADR-102 §12.5](../../../../../../../../docs/DECISIONS.md#adr-102). V2 drew the number at **9sp**
 * in `--ink-faint`, which is 3.45:1 dark and 3.41:1 light: an AA failure at any reading of 1.4.3, since
 * 9sp is nowhere near the 18pt (or 14pt bold) large-scale threshold. The token was fixed on 2026-08-12
 * (`inkFaint → inkSoft`); the **size** is fixed here, by the freeze, and 11.52sp bold on a lit card
 * measures 6.78:1 in *both* themes. The card being lit is what makes that one number rather than two.
 */
internal val BenchCellNumberSize = 11.52.sp

/**
 * The card's palette — the room, with **exactly the six tokens `.pgc` re-declares** taken from the
 * light theme (`v21-bench.html:452-456`, the 2026-08-12 OD-47 amendment).
 *
 * ### Why the card is lit, and why the reason is *not* the one OD-47 was filed under
 *
 * OD-47 was filed under [OD-31](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md), *"the artifact
 * does not dim"*, and the amendment records that premise as **false here**: a card draws no page
 * content, so this surface has no artifact to dim — which is exactly why the V2 freeze lit the
 * filmstrip and deliberately left the grid dark. What changed the answer is a **consistency** defect.
 * One screen was rendering the same eight pages two ways, forty pixels apart: [benchThumbIsland]'s lit
 * sheets in the strip and a room-dark card in the grid, and the smaller rendering was the legible one.
 * A card that stands for a sheet of paper reads as paper on any desk.
 *
 * ### Six, not five, and not the whole scheme
 *
 * `.pthumb` restates five; this restates six, and the extra one is **`--ink-line`**. The amendment says
 * why in terms: the card's hard shadow falls on *the card's own plane*, unlike `.page`'s, which falls on
 * the bench and must follow the room. So the shadow is lit with everything else — and note that in the
 * light palette `inkLine` and `ink` are the same byte (`#33261C`), which does **not** collapse the two:
 * a drawn line is [ZinelyV21Colors.ink] and a shadow is [ZinelyV21Colors.inkLine] by name here, exactly
 * as they are everywhere else, because they diverge in dark and the island is read from `room`.
 *
 * Everything the amendment lists as unchanged is unchanged: the 3/4 aspect, `--br-sm`, the 1.5dp ink
 * border, the 3dp hard shadow, the bare tabular number, `.pgc.on`'s leaf tint. **The panel is chrome
 * and keeps the room** — only the cards are lit, which is why this takes `room` and returns a copy
 * instead of the panel simply installing the light scheme.
 *
 * Pure, so the set is asserted against the frozen `.pgc` block without a composition.
 */
internal fun benchGridCardIsland(room: ZinelyV21Colors): ZinelyV21Colors {
    val lit = zinelyV21LightColors()
    return room.copy(
        paper = lit.paper,
        ink = lit.ink,
        inkSoft = lit.inkSoft,
        inkLine = lit.inkLine,
        leafTint = lit.leafTint,
        leafText = lit.leafText,
    )
}

/**
 * The frozen page grid — `.scrim` + `.pgrid`, `.grip`, `.pghead`, `.pgg` and `.pgc`
 * (`v21-bench.html:376-378`, `:444-465`, script `:778-786`);
 * [ADR-095](../../../../../../../../docs/DECISIONS.md#adr-095) rows 5.11–5.15, re-skinned by
 * [ADR-102](../../../../../../../../docs/DECISIONS.md#adr-102) package P5.
 *
 * ### Summoned, never default
 *
 * This is the justified exception to *"a grid turns a desk into a database"*
 * ([V2-BENCH-REVIEW §E.2](../../../../../../../../docs/design/V2-BENCH-REVIEW.md)): it exists while the
 * user asks for it and not one frame longer. `visible = false` composes **nothing** — not a hidden node,
 * not a zero-alpha overlay — which is row 5.11a's assertion and the difference between a summoned
 * overlay and a permanent panel that happens to be off-screen.
 *
 * ⚠ **The early return below is load-bearing again.** In V2 it was not: a single [AnimatedVisibility]
 * enforced the invariant by itself, the mutation battery proved the guard could be deleted with no
 * observable change, and it was deleted. V2.1 needs a *host* for two children — the scrim and the panel
 * — and a host that outlived them would be a full-screen layout node standing over the canvas forever.
 * So the guard is back, and it is the thing enforcing the invariant rather than decoration in front of
 * something else that does.
 *
 * ### It is a bottom sheet now, not an overlay
 *
 * V2's `.pgrid` was `position:absolute; inset:0` — an opaque `--desk` panel that replaced the canvas.
 * V2.1's rises from the bottom edge over a scrim, in `--paper`, with a 36dp top radius, a 2dp ink top
 * rule and a 78% ceiling: the same object `.sheet` is, and the page stays visible above it. The scrim
 * dismisses on tap, which is the frozen `$('scrim').onclick → closeOverlays()` (`:845`) and a third
 * exit beside the cross and system Back.
 *
 * ⚠ **The host was re-homed to match, and this is the third answer to "where does this panel live".** The
 * frozen `.pgrid` is a child of `.phone` at `z-index:54` (markup `v21-bench.html:573`; `.canvasArea` closes
 * at `:530`), so it rises from the *screen's* bottom edge and covers the filmstrip and the bar, and the
 * scrim covers everything. `EditorScreen` mounted it inside `.canvasArea` until P5 — correctly, for V2,
 * whose rule was `position:absolute; inset:0` *inside* that element. V2.1 moved the element, not just its
 * paint: anchored to the canvas, a `translateY(103%)` sheet rises to a stop mid-screen with the filmstrip
 * and bar still lit beneath it, which reads as a panel that failed to open.
 *
 * The first two readings were both taken from the CSS rule; this one was taken from the **markup nesting**,
 * which is the thing that actually decides what `position:absolute` resolves against. The two earlier
 * mistakes are recorded in `EditorScreen`'s call site, because the file that gets it wrong is that one.
 *
 * ### The cells keep the frozen design, by the same ruling that changed the strip
 *
 * [OD-22](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-053-ruling) amended `.pthumb` and
 * said in terms that *"the page grid remains independent and continues to follow the frozen design."*
 * So a card is a sheet of paper with its number and **no miniature**. What it no longer carries is the
 * `COVER`/`BACK` badge: V2's `.pgcell b` is gone from the frozen markup, which draws `${i+1}` and
 * nothing else (`:782`). The cover distinction survives where it is actually *spoken*, in
 * [benchPageLabel] — it is no longer drawn.
 *
 * ### What Back is, and where it lives
 *
 * Back closing a summoned overlay is a **platform** contract the frozen HTML has no way to describe — a
 * prototype has no back button. The host supplies it explicitly (`EditorScreen.kt`,
 * `BackHandler(pageGridOpen)`) rather than this overlay covering more of the screen than the design does
 * in order to get it for free from a `Dialog`.
 *
 * @param pages the document's pages. `N` is `pages.size`, never a constant (row 5.16).
 * @param onSelectPage invoked with the tapped page index; the host navigates and dismisses.
 * @param onDismiss the frozen `.dclose`, and the scrim.
 */
@Composable
internal fun BenchPageGrid(
    visible: Boolean,
    pages: List<Page>,
    currentPageIndex: Int,
    onSelectPage: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleState = remember { MutableTransitionState(false) }
    visibleState.targetState = visible
    // Summoned, never default — and *gone* when stood down, not merely transparent.
    if (!visibleState.currentState && !visibleState.targetState) return

    val motion = ZinelyTheme.v2Motion
    // The scrim closes the same panel the cross does, so it must feel the same doing it.
    val dismissByScrim = benchTap(action = onDismiss)
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // `.scrim{transition:opacity .22s}` — the frozen rule names a duration and no easing token, so
        // this takes `--standard`, the corpus's non-arrival curve. The panel below takes `--settle`,
        // which its rule does name.
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(motion.standard(BenchGridScrimMillis)),
            exit = fadeOut(motion.standard(BenchGridScrimMillis)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(BenchPageGridScrimTestTag)
                    .background(BenchGridScrimColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClickLabel = Copy.Common.CLOSE_OVERLAY,
                        onClick = dismissByScrim,
                    )
                    // ⚠ The grid is drawn in the MAIN WINDOW, not a Dialog (see this file's KDoc), so this
                    // scrim is a screen-sized node in the same traversal order as the panel — and it
                    // published no name at all. TalkBack met an unlabelled full-screen button *before* the
                    // pages it covers. Named rather than hidden, because tapping outside is how the
                    // overlay closes and a sighted user has that affordance.
                    .semantics { contentDescription = Copy.Common.CLOSE_OVERLAY },
            )
        }
        AnimatedVisibility(
            visibleState = visibleState,
            modifier = Modifier.align(Alignment.BottomCenter),
            // Frozen enter: `translateY(103%)` → `0` at `.3s var(--settle)`. The 103 rather than 100 is
            // the freeze's own margin, so the panel clears the bottom edge before it starts;
            // transcribed, not rounded. Reduced motion collapses the tween to 0ms inside `settle`.
            enter = slideInVertically(motion.settle(BenchGridEnterMillis)) {
                (it * BenchGridEnterPercent).roundToInt()
            },
            exit = slideOutVertically(motion.settle(BenchGridEnterMillis)) {
                (it * BenchGridEnterPercent).roundToInt()
            },
        ) {
            BenchPageGridSurface(
                pages = pages,
                currentPageIndex = currentPageIndex,
                onSelectPage = onSelectPage,
                onDismiss = onDismiss,
                // `max-height:78%`, resolved against the box this overlay was given. See the deviation
                // above for why that box is the canvas rather than the phone.
                modifier = Modifier.heightIn(max = maxHeight * BenchGridMaxHeightFraction),
            )
        }
    }
}

/**
 * The `.pgrid` panel without its scrim or its enter/exit animation — split out so the goldens rasterize
 * a settled frame rather than whatever the slide happens to be showing.
 */
@Composable
internal fun BenchPageGridSurface(
    pages: List<Page>,
    currentPageIndex: Int,
    onSelectPage: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ZinelyTheme.v21Colors
    // The panel is CHROME and keeps the room (the OD-47 amendment says so in terms) — its ground is the
    // room's `--paper` and its rule the room's `--ink`. Only the cards below are lit.
    val shape = RoundedCornerShape(topStart = BenchGridPanelRadius, topEnd = BenchGridPanelRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.paper)
            // `border-top:2px solid var(--ink)` — the TOP only. `Modifier.border` would ink all four
            // sides, and three of them are not in the frozen rule: the panel's flanks and floor meet the
            // edges of the box it slides into. So the outline is stroked at twice the rule width (the
            // upstream `clip` discards the outer half, leaving 2dp inside, which is what CSS draws) and
            // clipped to the band the two top corners occupy. It ends square at the corners' tangent
            // point where CSS tapers; at 2dp that difference is sub-pixel.
            .drawBehind {
                val rule = BenchGridPanelRule.toPx()
                clipRect(bottom = BenchGridPanelRadius.toPx()) {
                    drawOutline(
                        outline = shape.createOutline(size, layoutDirection, this),
                        color = colors.ink,
                        style = Stroke(width = rule * 2f),
                    )
                }
            }
            // The canvas underneath keeps its drag, tap and pinch detectors. Without a consumer here a
            // gesture that starts on the panel would reach the page behind it — moving an element the
            // user cannot see. The panel is opaque, so it must be opaque to touch as well. (The scrim
            // takes the rest, and turns it into a dismissal.)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            }
            .testTag(BenchPageGridTestTag)
            // `padding:0 var(--gap-lg) var(--gap-xl)`, applied **inside** the scroll container.
            //
            // ⚠ This is the chain contract, and it is the one that fails as *"the shadow is missing"* on
            // a device rather than as anything a test would name. `verticalScroll` clips to its viewport
            // on both axes. Three cards and two 12dp gaps exactly fill the panel's inner width, so with
            // the inset applied *outside* the scroll the viewport edge lands flush against the right-hand
            // card and shears its 3dp printed shadow off. Inside, the viewport is the panel's full width
            // and the inset is content, so every card's shadow has 16dp to fall into. Nothing about the
            // frozen geometry changes: there is no horizontal scrolling for the padding to slide.
            .verticalScroll(rememberScrollState())
            .padding(
                start = BenchGridPanelPaddingH,
                end = BenchGridPanelPaddingH,
                bottom = BenchGridPanelPaddingBottom,
            ),
    ) {
        // `.grip` — decorative, exactly as it is in the frozen file: zero pointer handlers, and the
        // panel has no drag-to-dismiss to hint at.
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = BenchGripPaddingTop, bottom = BenchGripPaddingBottom)
                .width(BenchGripWidth)
                .height(BenchGripHeight)
                .clip(RoundedCornerShape(ZinelyV21Dimens.radiusPill))
                .alpha(BenchGripOpacity)
                .background(colors.inkFaint),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = BenchGridHeadPaddingTop,
                    start = BenchGridHeadPaddingH,
                    end = BenchGridHeadPaddingH,
                    bottom = BenchGridHeadPaddingBottom,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The frozen prototype's own `<h3>` reads "All pages" — the grid *button's* label, in a
            // document that has no document. The app has one, so it keeps `gridTitle`: row 5.16 says
            // `N` is the document's page count and never a constant, and a panel titled with the name
            // of the button that opened it tells the reader nothing they did not just do. Deviation
            // recorded rather than taken silently.
            Text(
                text = Copy.PageNav.gridTitle(pages.size),
                color = colors.ink,
                fontSize = BenchGridTitleSize,
                fontWeight = FontWeight.Bold,
                fontFamily = ZinelyV21Fonts.Voice,
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
            )
            BenchPageGridClose(onDismiss)
        }
        // `repeat(3, 1fr)` — the column width is the panel's inner width less two gaps, measured once
        // and handed to every card, so the last row's cards keep the grid's column width instead of
        // stretching to fill a short row.
        BoxWithConstraints {
            val cellWidth = (maxWidth - BenchGridGap * (BenchGridColumns - 1)) / BenchGridColumns
            Column {
                pages.chunked(BenchGridColumns).forEachIndexed { rowIndex, rowPages ->
                    if (rowIndex > 0) Spacer(Modifier.height(BenchGridGap))
                    Row(horizontalArrangement = Arrangement.spacedBy(BenchGridGap)) {
                        rowPages.forEachIndexed { columnIndex, _ ->
                            val index = rowIndex * BenchGridColumns + columnIndex
                            BenchPageCell(
                                pageNumber = index + 1,
                                pageCount = pages.size,
                                current = index == currentPageIndex,
                                width = cellWidth,
                                onClick = { onSelectPage(index) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The frozen `.dclose` — a 34dp `paper` pill, 1.5dp `ink` border, [ZinelyV21Press.Flat]'s 2dp printed
 * shadow, holding the 15dp cross in `inkSoft`.
 *
 * ⚠ **The freeze writes no `:active` rule for this button, and the press is applied anyway.** That is
 * the precedent the frozen file set for itself at `:285-292`, where P3 amended `.doneEdit` — *"a 2px
 * rest with a flush press is exactly the Flat tier the corpus already assigns"* — and the amendment's
 * claim that `.doneEdit` was the **only** shadowed control in the trilogy without an `:active` rule is
 * simply not true: `.dclose` and `.pgc` are two more. The tier follows the rest depth, as it does there.
 * The alternative is a shadowed control that does not move under a finger on a surface where six others
 * do, which reads as broken rather than as restrained.
 */
@Composable
private fun BenchPageGridClose(onDismiss: () -> Unit) {
    val colors = ZinelyTheme.v21Colors
    val dismiss = benchTap(action = onDismiss)
    val shape = RoundedCornerShape(ZinelyV21Dimens.radiusPill)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(BenchGridCloseTarget)
            .clickable(interactionSource = interaction, indication = null, onClick = dismiss)
            .testTag(BenchPageGridCloseTag)
            .semantics {
                contentDescription = Copy.Proof.CLOSE
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(BenchGridCloseSize)
                // Nothing that clips may precede the press — the shadow paints outside the node.
                .zinelyV21Pressable(pressed, ZinelyV21Press.Flat, colors.inkLine, shape)
                .clip(shape)
                .background(colors.paper)
                .border(BenchChromeBorder, colors.ink, shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ZinelyV2Icons.Close.toImageVector(
                    BenchGridCloseGlyphSize,
                    ZinelyV2IconPaint.Stroke(BenchGridCloseGlyphStroke),
                ),
                contentDescription = null,
                tint = colors.inkSoft,
                modifier = Modifier.size(BenchGridCloseGlyphSize),
            )
        }
    }
}

/**
 * One frozen `.pgc` — a **lit** sheet of paper carrying its number, centred.
 *
 * The card wears [ZinelyV21Press.Raised], its 3dp rest transcribed from
 * `box-shadow:3px 3px 0 var(--ink-line)` (`v21-bench.html:460`); see [BenchPageGridClose] for why a
 * press is applied to a control the freeze writes no `:active` rule for. V2's `scale(.96)` is gone with
 * everything else V2's cell did — a card does not shrink under a finger in this language, it moves.
 *
 * The cell keeps `Role.Button` with an explicit [stateDescription], not `Role.Tab`: see the note at the
 * semantics block, which is Device Pass 1's finding and is unchanged by the re-skin.
 */
@Composable
private fun BenchPageCell(
    pageNumber: Int,
    pageCount: Int,
    current: Boolean,
    width: Dp,
    onClick: () -> Unit,
) {
    // OD-47: the card is a light island on any desk. Read from the ROOM's scheme, so a token this
    // island does not restate still follows the theme.
    // `remember`ed on the room, not recomputed per item: the island builds a whole light scheme and then
    // copies the room over it, and this composable runs once per card. Unremembered that is 2N scheme
    // allocations per frame in a surface that scrolls. Cheap to hold, and the key is the only input.
    val room = ZinelyTheme.v21Colors
    val card = remember(room) { benchGridCardIsland(room) }
    // Snap — the grid and the strip are the same choice made two ways, so they must feel the same.
    val pick = benchTap(ZinelyHaptic.Snap, onClick)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(BenchCellRadius)
    Box(
        modifier = Modifier
            .width(width)
            // `aspect-ratio:3/4` is width ÷ height, so the height is the derived term.
            .height(width / BenchCellAspect)
            // Nothing that clips may sit to the LEFT of the press — the shadow paints outside the node.
            // The 12dp `.pgg` gap and the panel's 16dp flanks are what leave it the 3dp it needs; the
            // scroll viewport clips at the panel's padding edge, which is 13dp clear of any card.
            .zinelyV21Pressable(pressed, ZinelyV21Press.Raised, card.inkLine, shape)
            .clip(shape)
            // `.pgc.on{background:var(--leaf-tint)}` (`:461`) — the current page's whole mark. V2 drew a
            // 2dp `--matcha` border against every other cell's 1px; V2.1 says the *tint* carries the
            // state and every card keeps the same uniform 1.5dp ink edge.
            .background(if (current) card.leafTint else card.paper)
            .border(BenchChromeBorder, card.ink, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = pick)
            .testTag(benchPageCellTag(pageNumber))
            .semantics {
                // The same sentence the strip's thumb says, so the two page pickers never disagree —
                // including the cover naming the badge no longer draws.
                contentDescription = benchPageLabel(pageNumber, pageCount)
                this.role = Role.Button
                selected = current
                // …and the same *state*, said out loud rather than left to `selected` alone.
                //
                // Device Pass 1 read the platform tree and found every cell reporting `selected=false`,
                // the current one included: Compose maps `SemanticsProperties.Selected` through to
                // `AccessibilityNodeInfo.isSelected` for `Role.Tab` — which is why the strip's sheet
                // announces itself correctly — and does not for `Role.Button`. So the grid drew the
                // current page with a mark a screen reader could not see, and the two page pickers
                // disagreed about the one fact they both exist to tell you.
                //
                // `stateDescription` reaches the platform whatever the role, and it is the mechanism the
                // strip already uses (CI-29). Post-freeze accessibility work is allowed in terms.
                stateDescription =
                    if (current) Copy.PageStrip.CURRENT_PAGE else Copy.PageStrip.NOT_SELECTED
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$pageNumber",
            // 6.78:1 on the lit card, in both themes, at 11.52sp bold — the AA failure ADR-102 §12.5
            // booked against this cell, closed by the size the freeze gives it and the ground OD-47
            // gives the card. `card.inkSoft`, never the room's: the number sits on the card's paper.
            color = if (current) card.leafText else card.inkSoft,
            fontSize = BenchCellNumberSize,
            fontWeight = FontWeight.Bold,
            fontFamily = ZinelyV21Fonts.Work,
            lineHeight = ZinelyV21Fonts.InheritedLineHeight,
            // `font-variant-numeric:tabular-nums`. `Text` has no parameter for it, so it goes through
            // the style: a proportional `1` would leave page 1's number visibly off the card's centre
            // against page 8's.
            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
        )
    }
}
