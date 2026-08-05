package com.aritr.zinely.feature.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlin.math.roundToInt

/** Test tag on the frozen `.pgrid` overlay. It exists **only while summoned** (row 5.11a). */
public const val BenchPageGridTestTag: String = "bench-page-grid"

/** Test tag on the grid's `Done`. */
public const val BenchPageGridDoneTag: String = "bench-page-grid-done"

/** Per-cell test tag, 1-based like the frozen `openGrid()`. */
public fun benchPageCellTag(pageNumber: Int): String = "bench-page-cell-$pageNumber"

/** Frozen `.pgrid{padding:16px}` and its `translateY(102%) → 0` at `.3s var(--settle)` (`:374-375`). */
internal val BenchGridPadding = 16.dp
internal const val BenchGridEnterPercent: Float = 1.02f
internal const val BenchGridEnterMillis: Int = 300

/** Frozen `.pgg{grid-template-columns:repeat(3,1fr); gap:12px}` (`:379`). */
internal const val BenchGridColumns: Int = 3
internal val BenchGridGap = 12.dp

/** Frozen `.pgcell{aspect-ratio:.66; border-radius:6px; shadow 0 4px 10px -6px}` (`:380`). */
internal const val BenchCellAspect: Float = 0.66f
internal val BenchCellRadius = 6.dp
internal val BenchCellShadow = 4.dp

/** Frozen `.pgcell.cur{border-width:2px}` against every other cell's 1px (`:381`). */
internal val BenchCellBorder = 1.dp
internal val BenchCellBorderCurrent = 2.dp

/** Frozen `.pgcell:active{transform:scale(.96)}` and its `transition:transform .1s` (`:380-381`). */
internal const val BenchCellPressedScale: Float = 0.96f
internal const val BenchCellPressMillis: Int = 100

/** Frozen `.pgcell b` / `.pgcell span` type (`:382-383`). */
internal val BenchCellLabelSize = 8.sp

/** Frozen `.pgcell b{letter-spacing:.1em}` — `em` is relative to the 8sp above, as CSS's is to 8px. */
internal val BenchCellLabelTracking = 0.1.em

internal val BenchCellNumberSize = 9.sp
internal val BenchCellLabelInsetTop = 5.dp
internal val BenchCellLabelInsetStart = 6.dp
internal val BenchCellNumberInsetBottom = 3.dp
internal val BenchCellNumberInsetEnd = 5.dp

/** Frozen `.pgh h3` 17px/500 serif and its `Done` at 13px/600 `--matcha-text` (`:377-378`). */
internal val BenchGridTitleSize = 17.sp
internal val BenchGridDoneSize = 13.sp
internal val BenchGridHeaderGap = 14.dp

/**
 * The frozen page grid — `.pgrid`, `.pgh`, `.pgg` and `.pgcell` (`v2-bench.html:374-383`, script `:730-739`);
 * [ADR-095](../../../../../../../../docs/DECISIONS.md#adr-095) rows 5.11–5.15.
 *
 * ### Summoned, never default
 *
 * This is the justified exception to *"a grid turns a desk into a database"*
 * ([V2-BENCH-REVIEW §E.2](../../../../../../../../docs/design/V2-BENCH-REVIEW.md)): it exists while the user
 * asks for it and not one frame longer. `visible = false` composes **nothing** — not a hidden node, not a
 * zero-alpha overlay — which is row 5.11a's assertion and the difference between a summoned overlay and a
 * permanent panel that happens to be off-screen. That is enforced by [AnimatedVisibility] itself, which
 * emits no layout node while both its `currentState` and `targetState` are false. An earlier cut guarded it
 * with an explicit early return as well; the mutation battery proved that line could be deleted without any
 * observable change, so it was — a guard that enforces nothing reads as though it enforces the invariant.
 *
 * ### The cells keep the frozen design, by the same ruling that changed the strip
 *
 * [OD-22](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-053-ruling) amended `.pthumb` and said in
 * terms that *"the page grid remains independent and continues to follow the frozen design."* So a cell is a
 * sheet of paper with its number and, on the two covers, its name — **no miniature**. C5 does not infer a
 * grid interior from the strip's, and the asymmetry is the ruling's, not an oversight.
 *
 * ### It covers the canvas, and only the canvas
 *
 * The frozen rule is `position:absolute; inset:0` (`:374`) on markup that sits **inside `.canvasArea`**
 * (opened `:416`, closed `:471`) — so the status strip above and the filmstrip and bar below stay visible
 * and live while the grid is open. That is deliberate: you can see the strip you are jumping *from*.
 *
 * The first cut of C5 hosted this in a full-screen `Dialog` to get system Back for free, and recorded — here
 * and in ADR-095 row 5.11 — that the freeze said `fixed`. It does not. Independent review caught it by
 * reading the HTML rather than the ADR, which is the same way C4's `.snack` anchoring was caught in this same
 * file.
 *
 * Losing the `Dialog` lost Back with it, and Back closing a summoned overlay is a **platform** contract that
 * the frozen HTML has no way to describe — a prototype has no back button. The host supplies it explicitly
 * (`EditorScreen.kt`, `BackHandler(pageGridOpen)`) rather than the overlay covering more of the screen than
 * the design does to get it back for free.
 *
 * @param pages the document's pages. `N` is `pages.size`, never a constant (row 5.16).
 * @param onSelectPage invoked with the tapped page index; the host navigates and dismisses.
 * @param onDismiss the frozen `Done`.
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
    // Summoned, never default — and *gone* when stood down, not merely transparent. `AnimatedVisibility`
    // emits no node at all once the exit slide has finished, which is row 5.11a.
    val visibleState = remember { MutableTransitionState(false) }
    visibleState.targetState = visible

    val motion = ZinelyTheme.v2Motion
    AnimatedVisibility(
        visibleState = visibleState,
        modifier = modifier.fillMaxSize(),
        // Frozen enter: `translateY(102%)` → `0` at `.3s var(--settle)`. The 102 rather than 100 is the
        // freeze's own margin, so the overlay clears the bottom edge before it starts; transcribed, not
        // rounded. It is 102% of the CANVAS, because that is the box the frozen rule resolves against.
        // Reduced motion collapses the tween to 0ms inside `settle`.
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
        )
    }
}

/**
 * The grid body without its enter/exit animation — split out so the goldens rasterize a settled frame
 * rather than whatever the slide happens to be showing.
 */
@Composable
internal fun BenchPageGridSurface(
    pages: List<Page>,
    currentPageIndex: Int,
    onSelectPage: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ZinelyTheme.v2Colors
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.desk)
            // The canvas underneath keeps its drag, tap and pinch detectors. Without a consumer here a
            // gesture that starts on the grid would reach the page hidden behind it — moving an element the
            // user cannot see. The overlay is opaque, so it must be opaque to touch as well.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            }
            .testTag(BenchPageGridTestTag)
            .padding(BenchGridPadding),
    ) {
        val cellWidth = (maxWidth - BenchGridGap * (BenchGridColumns - 1)) / BenchGridColumns
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = Copy.PageNav.gridTitle(pages.size),
                    color = colors.ink,
                    fontSize = BenchGridTitleSize,
                    fontWeight = FontWeight.Medium,
                    fontFamily = ZinelyTheme.v2Typography.voice,
                )
                Text(
                    text = Copy.EditText.DONE,
                    color = colors.matchaText,
                    fontSize = BenchGridDoneSize,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = ZinelyTheme.v2Typography.work,
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .testTag(BenchPageGridDoneTag)
                        .semantics {
                            contentDescription = Copy.EditText.DONE
                            role = Role.Button
                        },
                )
            }
            Spacer(Modifier.height(BenchGridHeaderGap))
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

/** One frozen `.pgcell` — a sheet of paper carrying its number, and its name if it is a cover. */
@Composable
private fun BenchPageCell(
    pageNumber: Int,
    pageCount: Int,
    current: Boolean,
    width: Dp,
    onClick: () -> Unit,
) {
    val colors = ZinelyTheme.v2Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) BenchCellPressedScale else 1f,
        animationSpec = ZinelyTheme.v2Motion.standard(BenchCellPressMillis),
        label = "bench-page-cell-press",
    )
    val shape = RoundedCornerShape(BenchCellRadius)
    Box(
        modifier = Modifier
            .width(width)
            // `aspect-ratio:.66` is width ÷ height, so the height is the derived term.
            .height(width / BenchCellAspect)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(BenchCellShadow, shape, ambientColor = colors.frameShadow, spotColor = colors.frameShadow)
            .clip(shape)
            .background(colors.paper)
            .border(
                width = if (current) BenchCellBorderCurrent else BenchCellBorder,
                color = if (current) colors.matcha else colors.paperEdge,
                shape = shape,
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .testTag(benchPageCellTag(pageNumber))
            .semantics {
                // The same sentence the strip's thumb says, so the two page pickers never disagree.
                contentDescription = benchPageLabel(pageNumber, pageCount)
                this.role = Role.Button
                selected = current
                // …and the same *state*, said out loud rather than left to `selected` alone.
                //
                // Device Pass 1 read the platform tree and found every cell reporting `selected=false`,
                // the current one included: Compose maps `SemanticsProperties.Selected` through to
                // `AccessibilityNodeInfo.isSelected` for `Role.Tab` — which is why the strip's sheet
                // announces itself correctly — and does not for `Role.Button`. So the grid drew the
                // current page with a 2dp matcha ring that a screen reader could not see, and the two
                // page pickers disagreed about the one fact they both exist to tell you.
                //
                // `stateDescription` reaches the platform whatever the role, and it is the mechanism the
                // strip already uses (CI-29). Post-freeze accessibility work is allowed in terms.
                stateDescription =
                    if (current) Copy.PageStrip.CURRENT_PAGE else Copy.PageStrip.NOT_SELECTED
            },
    ) {
        // Frozen `buildFilm()`/`openGrid()` name the first and last sheet — by position, per
        // [benchCoverAt]. The role-based first cut named nothing at all on a real document.
        val name = when (benchCoverAt(pageNumber, pageCount)) {
            BenchCover.FRONT -> Copy.PageNav.COVER
            BenchCover.BACK -> Copy.PageNav.BACK
            BenchCover.NONE -> null
        }
        if (name != null) {
            Text(
                // Frozen `.pgcell b{text-transform:uppercase; letter-spacing:.1em}` (`v2-bench.html:383`).
                // The transform is presentation, so it is applied here rather than stored uppercase in
                // `Copy` — the cell's spoken label is `benchPageLabel` above and is deliberately not
                // shouted. At 8sp the tracking is what stops `COVER` reading as a smudge.
                text = name.uppercase(),
                color = colors.matchaText,
                fontSize = BenchCellLabelSize,
                letterSpacing = BenchCellLabelTracking,
                fontWeight = FontWeight.Bold,
                fontFamily = ZinelyTheme.v2Typography.work,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = BenchCellLabelInsetStart, top = BenchCellLabelInsetTop),
            )
        }
        Text(
            text = "$pageNumber",
            color = colors.inkFaint,
            fontSize = BenchCellNumberSize,
            fontFamily = ZinelyTheme.v2Typography.work,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = BenchCellNumberInsetEnd, bottom = BenchCellNumberInsetBottom),
        )
    }
}
