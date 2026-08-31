package com.aritr.zinely.feature.library

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.ZineCoverRecipe
import com.aritr.zinely.ui.components.zinelyV21HardShadow
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts

/**
 * One object standing on the shelf: the title it is called, the cover it is printed on, and the line the
 * shelf deliberately does not show.
 *
 * **Three fields now, and the third is drawn nowhere on this screen.** The frozen markup carries a
 * `data-sub` on every `.zine` (`"A4 · 2 days ago"`, `v2-library.html:149-154`) and the frozen design's whole
 * point is that the shelf **withholds** it: *"Covers only — no metadata line … Format & date are disclosed
 * there, not stamped on every card"* (`:142-144`). B2 left the field out because nothing read it; **B3**
 * adds it with the reader — [ZineActionTarget], the sheet's header. A test asserts the withholding directly
 * (`the shelf shows no metadata under a cover`), because a subtitle that leaks onto the shelf is a
 * one-line mistake that looks like a feature.
 *
 * No identity field even so. [ZineShelf] leaves its grid keyed by position, which is correct for a list
 * that cannot yet reorder; **B5** brings real project data and, with it, the stable key.
 */
internal data class ZineShelfItem(
    val title: String,
    val recipe: ZineCoverRecipe,
    val subtitle: String,
)

/**
 * The test handle on one placed cover, keyed by its position on the shelf.
 *
 * Published from production for the reason V1's `homeCardTestTag(id)` is: the shelf's geometry is only
 * checkable at the **cover's** own bounds, and a cover is not otherwise addressable — searching for its
 * title finds the `Text` *inside* it, whose bounds are inset by the cover's own `padding:15px 15px 18px`.
 * Measuring that node instead reports the column gap as 177px and the side padding as 37px, which is how
 * this constant came to exist: those were the first numbers [ZineShelfTest] produced.
 *
 * **Position, not identity.** Matching the position-keyed grid — B5 brings the project id and this
 * becomes a function of that.
 */
internal fun zineShelfCoverTestTag(index: Int): String = "shelf-cover-$index"

/** `.ph` — one loading placeholder cell. Unnumbered: they are interchangeable, and counting them is the
 * only assertion worth making about them. */
internal const val ZineShelfPlaceholderTestTag: String = "shelf-placeholder"

/**
 * `.shelf-head` — the whole head **row**, not the `<h1>` inside it.
 *
 * The distinction is the reason this tag exists. `grid-column:1/-1` is a claim about the row, and in V2
 * the row's only child was the heading, so measuring the heading measured the span. V2.1's head is
 * `display:flex;justify-content:space-between` with the count chip on the other end, so the `<h1>` is
 * now content-sized: it measures ~146px on a 432px line, and a test that reads it can no longer tell a
 * spanning head from a one-column one.
 */
internal const val ZineShelfHeadTestTag: String = "shelf-head"

/**
 * The frozen Library's shelf — `v2-library.html:46-49`, `:147-155`.
 *
 * ```
 * .shelf{flex:1 1 auto;overflow-y:auto;padding:30px 22px 152px;
 *        display:grid;grid-template-columns:1fr 1fr;gap:28px 20px;align-content:start}
 * .shelf::-webkit-scrollbar{width:0}
 * .shelf-head{grid-column:1 / -1;padding:2px 2px 0}
 * ```
 *
 * Two columns of [ZineCover] under one quiet heading, scrolling as one region. The frozen file's own
 * comment states what this screen is: *"no header chrome, no metadata line — the covers ARE the
 * screen"* (`:45`). There is no wordmark, no count, no search, no sort and no toolbar, and those
 * absences are the design rather than an unfinished state — V1's search and sort were dropped by owner
 * ruling, not overlooked ([ADR-081](docs/DECISIONS.md#adr-081), ruling 2).
 *
 * ### The heading scrolls away, because it is a cell in the grid
 *
 * `.shelf-head` is a `grid-column:1 / -1` **item inside the scrolling `.shelf`**, not a bar above it.
 * So "My Shelf" travels up with the covers and leaves the viewport. That is the kind of tiny copy change
 * to write and an invisible one to review — a pinned header composes perfectly, looks deliberate, and
 * is a different screen — so it is asserted (`the heading scrolls away with the covers`).
 *
 * ### This shelf paints no ground, and that is the frozen reading
 *
 * `.shelf` declares no `background`; the desk it sits on belongs to `.phone{background:var(--desk)}`
 * (`:41-43`) — the app window, which is **B5**'s screen. So this composable is transparent and B5 owes
 * the desk fill, exactly as B1's cover owes its own desk to whatever places it. B2's own rasters supply
 * a desk the way [ZineCoverGoldenTest] does, for the same reason.
 *
 * ### Two columns, always — and that is the ruling, not an omission
 *
 * `grid-template-columns:1fr 1fr` carries **no media query anywhere in the frozen file**, so the literal
 * transcription is a fixed two columns at every width. V1's shelf is responsive by contrast
 * (`shelfColumns`: 2 · 3 · 4 · 5), and Phase B's device verification includes **foldables**
 * ([COMPOSE-V2-ROADMAP.md](docs/COMPOSE-V2-ROADMAP.md)), where two columns means two very large covers.
 * Inventing a breakpoint here would have been inventing design, so B2 transcribed the freeze and raised
 * the gap instead — and **[D-020](docs/design/V2-SPEC-DEFECTS.md#d-020-ruling)** was then **ruled** in
 * favour of exactly that: *"The frozen design defines a two-column shelf. No breakpoint exists. No
 * responsive behaviour exists. No maximum cover width exists. Do not invent any of them."* An adaptive
 * shelf requires a **future frozen design**, never an inference made here — so this file holds no
 * `BoxWithConstraints`, no window-size class and no width branch, deliberately.
 *
 * ### The bottom padding is 152px with nothing under it yet
 *
 * `padding-bottom:152px` exists to clear the `.dock` and its "Make a zine" button (`:88-90`), which is
 * **B4**. Trimming it to what B2 alone appears to need would be a silent deviation that B4 then has to
 * discover, so the frozen value is transcribed whole and the space below the last cover is simply empty
 * until B4 fills it.
 *
 * ### Each cell is a `.zine`, and B3 filled it
 *
 * B2 placed a bare [ZineCover] in every cell and recorded that `.zine` — the wrapper carrying the press
 * transform, the focus ring, `cursor:pointer` and the tap-highlight suppression (`:51-54`) — was **B3**'s,
 * because every one of those is *interaction*. **B3 landed it as [ZineOnShelf]**, together with the tap, the
 * long-press, the `⋯` and the haptic that the states exist to announce. B2's claim that the deferral cost
 * no parity held: at rest `.zine` paints nothing of its own, and the only thing these cells gained visually
 * is the `⋯` — which the frozen file draws at rest on every cover, and which the B2 rasters therefore
 * omitted and said so.
 *
 * The shelf still holds no sheet and no press state. Both belong to the objects and the screen, which is
 * why the two callbacks below report a position rather than opening anything.
 *
 * @param zines the objects on the shelf, in the order they are given. The frozen file states no sort —
 *   V1's sort control was dropped, so ordering is the caller's, and B5's data layer answers for it.
 * @param onOpen a cover was tapped, by its position. **B3** brought the gesture; where it leads is B5's.
 * @param onActions a cover was long-pressed, or its `⋯` was used. The caller opens [ZineActionSheet] for
 *   `zines[index]` — the shelf holds no sheet of its own, for the same reason it paints no desk: the sheet
 *   is a `.phone` child in the frozen file, which is the screen.
 * @param modifier the caller's. `.shelf{flex:1 1 auto}` means **the shelf takes the space left over**,
 *   so a caller that gives it none gets a grid sized to its content and no scrolling; B5's screen passes
 *   `fillMaxSize()`.
 * @param placeholders how many `.ph` cells to stand in for zines that have not arrived yet — **B5**, from
 *   the [D-024 amendment](docs/design/V2-SPEC-DEFECTS.md#d-024-amendment). Zero at rest. They live inside
 *   this grid because the frozen markup puts them there (`:184`), under the same heading and on the same
 *   two columns, so nothing about the screen moves when the real covers land.
 */
@Composable
internal fun ZineShelf(
    zines: List<ZineShelfItem>,
    onOpen: (Int) -> Unit,
    onActions: (Int) -> Unit,
    modifier: Modifier = Modifier,
    placeholders: Int = 0,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(ShelfColumns),
        modifier = modifier,
        contentPadding = PaddingValues(
            start = ZinelyV21Dimens.gapLg,
            top = ZinelyV21Dimens.gapXl,
            end = ZinelyV21Dimens.gapLg,
            bottom = zineDockClearance(ShelfDockClearance),
        ),
        verticalArrangement = Arrangement.spacedBy(ShelfRowGap),
        horizontalArrangement = Arrangement.spacedBy(ShelfColumnGap),
    ) {
        // `grid-column:1 / -1` — a full-width cell, so the row gap below it is the same 24px that
        // separates the cover rows. Nothing about this is a header component; it is a wide cell.
        // `state('loading')` sets `.shelf-head{visibility:hidden}` — **hidden, which keeps its space.**
        //
        // A first version wrote `if (placeholders == 0) item { … }`, which removes the cell entirely,
        // under a comment claiming the head keeps its space so the grid does not restructure. Both
        // reviewers caught it independently: the placeholders started ~60dp higher and every cover
        // jumped down when the data landed — the exact restructure the comment said it prevented, and
        // the exact thing `visibility:hidden` exists to avoid.
        //
        // The head is hidden rather than shown-with-a-zero because it carries a **count**, and a count
        // rendered against an empty list during a slow read announces "0 zines" to a user who has
        // twelve — the failure the D-024 amendment introduced placeholders to prevent.
        //
        // `alpha(0f)` and not `alpha(0.01f)` or a transparent colour: `visibility:hidden` is also
        // removed from the accessibility tree, which is what `clearAndSetSemantics {}` transcribes.
        //
        // While hidden this cell is not silent — it SAYS SO. `clearAndSetSemantics` drops the count
        // (which would announce "0 zines" to someone who has twelve) and puts one spoken node in its
        // place. Without it the whole loading shelf contributes no accessibility node at all: the
        // heading is cleared here and every placeholder clears itself, so a TalkBack user met a screen
        // containing only "Make a zine" — **indistinguishable from the empty state**, which is the exact
        // confusion the placeholders were added to prevent. A sighted user gets the sweep; this is its
        // spoken equal. `visibility:hidden` removes a thing that is *decorative*, and a load is not.
        item(span = { GridItemSpan(maxLineSpan) }) {
            val hidden = placeholders > 0
            ShelfHeading(
                count = zines.size,
                modifier = if (hidden) {
                    Modifier.alpha(0f).clearAndSetSemantics {
                        contentDescription = Copy.Shelf.LOADING_YOUR_ZINES
                        liveRegion = LiveRegionMode.Polite
                    }
                } else {
                    Modifier
                },
            )
        }

        // `.ph` — the loading placeholders, which the amended freeze puts INSIDE this grid, after the
        // heading, as ordinary cells (`:184`). They are cells rather than a separate skeleton screen for
        // the reason the amendment states: *the head's cell stays up, so the screen does not restructure
        // when the data lands*. The **cell**, not the heading — `visibility:hidden` keeps the space and
        // drops the text, which is what the block above transcribes; an earlier version of this line
        // quoted the amendment as "the heading stays up" and so read as the opposite of what ships.
        // See [ZineLibraryScreen] for why they are never shown beside real zines.
        items(placeholders) { ShelfPlaceholder() }

        // No `key`: position is the identity a list that cannot reorder actually has. B5 supplies the
        // stable one with real projects — asserting a key here would be asserting B5's data model.
        itemsIndexed(zines) { index, zine ->
            ZineOnShelf(zine = zine, index = index, onOpen = onOpen, onActions = onActions)
        }
    }
}

/**
 * `.shelf-head` — *"My Shelf"*, its hand-drawn swipe, and the count.
 *
 * ```css
 * .shelf-head{grid-column:1/-1;display:flex;align-items:flex-end;justify-content:space-between;
 *             margin-bottom:var(--gap-hair)}
 * .shelf-head h1{font-family:var(--voice);font-weight:700;font-size:2rem;letter-spacing:-.005em;
 *                line-height:1.05;margin:0}
 * .shelf-head h1 svg{width:132px;height:9px;margin-top:var(--gap-hair);color:var(--butter)}
 * .count{font-size:.78rem;font-weight:600;color:var(--ink-soft);background:var(--butter-tint);
 *        border-radius:var(--br-pill);padding:var(--gap-xs) var(--gap-md);transform:rotate(1.5deg)}
 * ```
 *
 * ### Averia 700, and this is the amendment, not a deviation
 *
 * V2's `.shelf-head h1` was Fraunces **500** by the D-005 ruling, which overrode the file's own 600 as a
 * stale artefact of the stack it was authored against. V2.1 bundles Averia Serif Libre, whose only
 * weights are 400 and 700, and Constitution §III **Amendment 1** admits it as the voice role. So 700 is
 * the transcription *and* the only weight available: there is no 500 to prefer.
 *
 * ### The swipe is a drawing, not an underline
 *
 * A `border-bottom` would be chrome. This is a stroked path with round caps that misses the text's left
 * and right edges and sags in the middle — a pen mark, in `butter`, `aria-hidden`. §5.1's ruling that
 * craft belongs to the material rather than to tools is what makes it a *drawn* line and not a styled
 * rule, and `aria-hidden="true"` is the file's own attribute, so it is silent here too.
 *
 * ### The count is disclosure, and it says so out loud
 *
 * `6 zines` is a rotated pill, which is decoration — but the number is real information, so it stays a
 * text node with its own semantics rather than being folded into the heading. It is **not** a heading:
 * the `<h1>` is, and `heading()` stays on the title alone so TalkBack's heading navigation lands on one
 * node per screen rather than two.
 */
@Composable
private fun ShelfHeading(count: Int, modifier: Modifier = Modifier) {
    val colors = ZinelyTheme.v21Colors
    Row(
        modifier
            .testTag(ZineShelfHeadTestTag)
            .fillMaxWidth()
            .padding(bottom = ZinelyV21Dimens.gapHair),
        horizontalArrangement = Arrangement.SpaceBetween,
        // `align-items:flex-end` — the count's baseline sits with the swipe, not with the cap height.
        verticalAlignment = Alignment.Bottom,
    ) {
        Column {
            Text(
                text = ShelfHeadingText,
                style = TextStyle(
                    fontFamily = ZinelyV21Fonts.Voice,
                    fontWeight = FontWeight.Bold,
                    fontSize = ShelfHeadingSize,
                    lineHeight = ShelfHeadingLineHeight,
                    letterSpacing = ShelfHeadingTracking,
                    color = colors.ink,
                ),
                modifier = Modifier.semantics { heading() },
            )
            val swipe = rememberShelfSwipePath()
            Canvas(
                Modifier
                    .padding(top = ZinelyV21Dimens.gapHair)
                    .size(width = SwipeWidth, height = SwipeHeight),
            ) {
                // `viewBox="0 0 132 9"` drawn at 132x9, so the viewport units are dp one for one and
                // the path needs no scale — the one case where transcribing an SVG costs no arithmetic.
                drawPath(
                    path = swipe,
                    color = colors.butter,
                    style = Stroke(
                        width = SwipeStroke.toPx(),
                        cap = StrokeCap.Round,
                    ),
                )
            }
        }
        Text(
            text = pluralZineCount(count),
            style = TextStyle(
                fontFamily = ZinelyV21Fonts.Work,
                fontWeight = FontWeight.SemiBold,
                fontSize = CountSize,
                // `body{line-height:1.55}`, inherited — `.count` declares none. See
                // [ZinelyV21Fonts.InheritedLineHeight].
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                // **`on-butter` on `butter`, where the freeze wrote `ink-soft` on `butter-tint`** —
                // ADR-100. See the ground below for the defect; the label follows it because
                // `ink-soft` on `butter` is 3.56:1 and would fail where it used to pass.
                color = colors.onButter,
            ),
            modifier = Modifier
                .graphicsLayer { rotationZ = CountRotation }
                // `.count{background:var(--butter-tint)}` — **amended to `--butter`, ADR-100.**
                //
                // In light, `butter-tint #FDEBC4` on `desk #FBE9CE` measures **1.01:1**. Not "subtle":
                // the same colour. The chip does not exist in the light theme — "6 zines" renders as
                // bare text — while in dark it measures 1.33 and is faintly there, so the one element
                // that tells you how much work you have looks like two different designs depending on
                // the time of day. Nothing in the freeze intends that; the two tokens simply collide on
                // the warm cream desk V2.1 introduced, and `butter-tint` was chosen against the older,
                // near-white one.
                //
                // `--butter` is the palette's own answer: *"highlight / tape / stamp — punctuation,
                // never action"*. A count is punctuation, and this is the third butter mark on the
                // screen after the heading's swipe and every cover's tape, so the shelf gains a rhyme
                // rather than a new colour. 1.56:1 against the desk as a ground, with a 7.89:1 label on
                // it — the pill is unmistakably there in both themes now, and it still is not an action,
                // because §3.2 reserves that reading for leaf.
                .background(colors.butter, RoundedCornerShape(ZinelyV21Dimens.radiusPill))
                .padding(
                    horizontal = ZinelyV21Dimens.gapMd,
                    vertical = ZinelyV21Dimens.gapXs,
                ),
        )
    }
}

/**
 * `6 zines` — and `1 zine`, which the frozen file never had to render.
 *
 * A prototype shows one number, so it can write the plural in by hand. A shelf holds whatever it holds.
 * Pure, so the one case the corpus does not demonstrate is nonetheless tested rather than trusted.
 */
internal fun pluralZineCount(count: Int): String = if (count == 1) "1 zine" else "$count zines"

/**
 * `.ph` — the same object, unprinted.
 *
 * ```css
 * .ph{aspect-ratio:3/4;border-radius:var(--br-xs) var(--br-md) var(--br-md) var(--br-xs);
 *     background:var(--paper);border:1.5px dashed var(--hair);
 *     box-shadow:var(--hard) var(--hard) 0 var(--hair);overflow:hidden}
 * .ph::after{background:linear-gradient(100deg,transparent 20%,rgba(255,255,255,.5) 50%,transparent 80%);
 *            transform:translateX(-100%);animation:sweep 1.5s cubic-bezier(.2,0,0,1) infinite}
 * ```
 *
 * V2's placeholder was a **hollow** — a cover-shaped hole in the desk's own edge tone. V2.1's is the
 * opposite reading: a sheet of paper that is standing there already, dashed rather than drawn, carrying
 * the same hard shadow every other object carries. The amendment's argument survives either way —
 * *"loading must never be mistaken for the empty state"* — but the answer got warmer.
 *
 * **The sweep is animation, so it obeys the motion policy.** `animation:sweep 1.5s infinite` is the one
 * looping animation in the whole corpus, and a loop is exactly what the reduced-motion setting exists
 * to stop. Under reduce-motion the sheet simply sits there, which still answers *"your shelf is
 * coming"* — the dashed border and the empty cover shape carry that on their own.
 *
 * **Silent to TalkBack.** It has no name because it is not a thing.
 */
@Composable
private fun ShelfPlaceholder() {
    val colors = ZinelyTheme.v21Colors
    val motion = ZinelyTheme.v2Motion

    // **`allowsContinuousMotion`, not a collapsed duration.** D-012's whole point: a one-shot under
    // reduced motion collapses to zero and still *arrives*; a loop must not run **at all**, because
    // collapsing an infinite animation's duration makes it strobe. A first version gated on
    // `durationMillis(…) == 0`, which reaches the same place by the wrong reasoning and would have
    // shipped the strobe the day someone made the policy return 1 instead of 0.
    // Held as `State`, not read here. Reading `.value` in the composable body would recompose all four
    // cells ~60x a second during a load — re-running the whole `ShelfPlaceholder`, re-allocating the
    // draw lambdas and re-resolving the shape outline. Read inside `drawBehind` it is a **draw-phase**
    // read: the layer redraws and nothing recomposes.
    val sweep: State<Float>? = if (!motion.allowsContinuousMotion) {
        null
    } else {
        rememberInfiniteTransition(label = SweepLabel).animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(motion.durationMillis(SweepDurationMillis), easing = SweepEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = SweepLabel,
        )
    }

    Box(
        Modifier
            .testTag(ZineShelfPlaceholderTestTag)
            .fillMaxWidth()
            .aspectRatio(PlaceholderAspectRatio)
            .zinelyV21HardShadow(ZinelyV21Dimens.hardShadow, colors.hair, PlaceholderShape)
            .clip(PlaceholderShape)
            .background(colors.paper)
            // One `drawBehind`, two paints, **border first and sweep over it** — CSS paint order is
            // background, then border, then positioned descendants, and `.ph::after` is a positioned
            // descendant. A first version had them the other way round and commented that they were
            // "in the order CSS paints them"; a review checked the order rather than the comment.
            //
            // One block rather than two chained modifiers because a chain's relative order is a thing
            // to be reasoned about rather than read — which is precisely how the cover's draw-order
            // defect stayed hidden through two reviews.
            .drawBehind {
                // `border:1.5px dashed var(--hair)` on a border-box element — so the stroke is INSIDE
                // the shape. Stroked at double width along the shape's own outline and left to the
                // clip already on this chain, which cuts the outer half away: that is the same 1.5dp
                // CSS draws, without an inset path whose corner radii would have to be recomputed
                // (the arithmetic that was wrong in `zinelyV21Frame` until a review caught it).
                val w = PlaceholderBorder.toPx()
                drawOutline(
                    outline = PlaceholderShape.createOutline(size, layoutDirection, this),
                    color = colors.hair,
                    style = Stroke(
                        width = w * 2f,
                        // CSS derives a dash rhythm from the border width; at 1.5px it renders at
                        // roughly 3on/3off, which is what this transcribes — the one approximated
                        // value on this component, recorded rather than presented as frozen.
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(PlaceholderDash.toPx(), PlaceholderDash.toPx()),
                        ),
                    ),
                )

                val offset = sweep?.value
                if (offset != null) {
                    // `.ph::after` — `linear-gradient(100deg, …)` translated across the cell. 100deg in
                    // CSS is measured clockwise from "to top", so it leans right; near-vertical is the
                    // wrong read. Approximated as a bottom-left to top-right sweep of the cell's width.
                    val x = offset * size.width
                    drawRect(
                        brush = Brush.linearGradient(
                            colorStops = SweepStops,
                            start = Offset(x, size.height),
                            end = Offset(x + size.width, 0f),
                        ),
                    )
                }
            }
            .clearAndSetSemantics {},
    )
}

// ---------------------------------------------------------------------------------------------
// The frozen values, transcribed from `v21-library.html`.
//
// V2.1 publishes a spacing scale (§3.3), so the shelf's own numbers are now token references rather
// than literals — `padding:var(--gap-xl) var(--gap-lg) 132px` and `gap:var(--gap-xl) var(--gap-lg)`.
// The 132px is the exception and stays a literal, because it is not on the scale: it is the height of
// the dock plus its clearance, which is a measurement of another component, not a spacing choice.
// ---------------------------------------------------------------------------------------------

/** `grid-template-columns:1fr 1fr` — still no breakpoint anywhere in the corpus. See D-020. */
private const val ShelfColumns = 2

/**
 * The original 132dp dock clearance plus the frozen Backup companion's 56dp quiet action row.
 * Loading temporarily keeps the same clearance so the grid does not jump when content arrives.
 */
private val ShelfDockClearance = 188.dp

/**
 * `.shelf{gap:var(--gap-xl) var(--gap-lg)}` — CSS `gap` is **row-gap first**, then column-gap.
 *
 * Named apart rather than carried as one value because they are not one value, and transposing them is
 * the defect this package is most likely to ship: 24 between the rows and 16 between the columns looks
 * plausible either way round on a screenshot.
 */
private val ShelfRowGap = ZinelyV21Dimens.gapXl
private val ShelfColumnGap = ZinelyV21Dimens.gapLg

/** `.shelf-head h1{margin:0}` — the heading's own text. */
private const val ShelfHeadingText = "My Shelf"

/** `font-size:2rem` against the browser's 16px root, and `line-height:1.05` of that. */
private val ShelfHeadingSize = 32.sp
private val ShelfHeadingLineHeight = 33.6.sp

/** `letter-spacing:-.005em` — kept in `em`, the unit the CSS states, so it tracks the size. */
private val ShelfHeadingTracking = (-0.005).em

/**
 * `.shelf-head h1 svg` — the hand-drawn swipe, `viewBox="0 0 132 9"` drawn at 132×9.
 *
 * Transcribed verbatim as path data for [ZineV21CoverMarks]' reason: a copy is checkable against the
 * source by eye, and a sequence of `moveTo`/`cubicTo` calls is not.
 */
private val SwipeWidth = 132.dp
private val SwipeHeight = 9.dp
private val SwipeStroke = 3.4.dp
private const val SwipePathData =
    "M2 6.2c22-3.4 46-4.6 74-3.2 18 .9 33 2.4 54 1.1"

/**
 * Parse the swipe once, into the composition — **not per draw, and not into a file-level `val`**.
 *
 * It used to be `private fun ShelfSwipePath(): Path` called from *inside* the `Canvas` draw lambda, so a
 * [PathParser] was allocated and an SVG string re-parsed every frame, on the UI thread, for a
 * compile-time constant — in a cell of a scrolling `LazyVerticalGrid`, the worst place in the app for it.
 *
 * ⚠ **The obvious fix — a file-level `private val` — is wrong here, and the test suite caught it.**
 * [Path] and [PathParser] are Android types, so constructing one in a top-level property runs it in
 * `ZineShelfKt`'s **static initialiser**. `pluralZineCount` and the two label helpers also live in this
 * file and are covered by `ZineShelfLabelsTest`, which is deliberately **pure JVM — no Robolectric**
 * (*"`String` in, `String` out"*). Loading the class to reach a pure string function then dragged in the
 * Android graphics stack and threw `ExceptionInInitializerError`. A pure function's test should not need
 * an Android runtime because something unrelated in the same file draws.
 *
 * `remember` gives the same once-per-composition parse with none of that: the initialiser runs during
 * composition, where Android already exists.
 */
@Composable
private fun rememberShelfSwipePath(): Path =
    remember { PathParser().parsePathString(SwipePathData).toPath() }

/** `.count{font-size:.78rem}` against a 16px root — 12.48px, carried unrounded. */
private val CountSize = 12.48.sp

/** `.count{transform:rotate(1.5deg)}` — the pill leans, the number does not move. */
private const val CountRotation = 1.5f

/** `.ph{aspect-ratio:3/4}` — width ÷ height, which is the order Compose's `aspectRatio` takes. */
private const val PlaceholderAspectRatio = 3f / 4f

/** `.ph{border-radius:var(--br-xs) var(--br-md) var(--br-md) var(--br-xs)}` — the cover's own profile. */
private val PlaceholderShape = RoundedCornerShape(
    topStart = ZinelyV21Dimens.radiusXs,
    topEnd = ZinelyV21Dimens.radiusMd,
    bottomEnd = ZinelyV21Dimens.radiusMd,
    bottomStart = ZinelyV21Dimens.radiusXs,
)

/** `.ph{border:1.5px dashed var(--hair)}` — the same 1.5px every drawn line in V2.1 uses. */
private val PlaceholderBorder = 1.5.dp
private val PlaceholderDash = 3.dp

/**
 * `.ph::after{background:linear-gradient(100deg,transparent 20%,rgba(255,255,255,.5) 50%,transparent 80%)}`
 *
 * White at half alpha in both themes — the frozen file does not theme it, and a sweep is a highlight
 * moving across a surface rather than a colour the surface has.
 */
private val SweepStops = arrayOf(
    0.2f to Color.Transparent,
    0.5f to Color.White.copy(alpha = 0.5f),
    0.8f to Color.Transparent,
)

/** `animation:sweep 1.5s cubic-bezier(.2,0,0,1) infinite`. */
private const val SweepDurationMillis = 1500
private const val SweepLabel = "shelfPlaceholderSweep"
private val SweepEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
