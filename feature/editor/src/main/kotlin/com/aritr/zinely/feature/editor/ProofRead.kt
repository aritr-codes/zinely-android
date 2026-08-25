package com.aritr.zinely.feature.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.render.SceneRenderer
import com.aritr.zinely.render.android.AssetBytesSource
import com.aritr.zinely.ui.components.zinelyV21HardShadow
import com.aritr.zinely.ui.theme.LocalZinelyV21Colors
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Colors
import com.aritr.zinely.ui.theme.ZinelyV21Grain
import com.aritr.zinely.ui.theme.rememberZinelyV21GrainBrush
import com.aritr.zinely.ui.theme.zinelyV21LightColors
import com.aritr.zinely.ui.theme.zinelyV21Grain
import kotlin.math.min

/** Test tag on the Read act's booklet stage — the leaf, its stack edge, and the two tap edges. */
public const val ProofReadBookTestTag: String = "proof-read-book"

/** Test tag on the left `.tapz`. */
public const val ProofReadPrevTestTag: String = "proof-read-prev"

/** Test tag on the right `.tapz`. */
public const val ProofReadNextTestTag: String = "proof-read-next"

/** Test tag on the stack edge — the closed remainder of the booklet, seen edge-on. */
public const val ProofReadStackTestTag: String = "proof-read-stack"

/** Per-page test tag: `"proof-read-page-<pageNumber>"` (1-based, reading order). */
public fun proofReadPageTag(pageNumber: Int): String = "proof-read-page-$pageNumber"

/**
 * **One leaf of the booklet** — which page, which side of the spine it is bound on, and whether it faces
 * anything.
 *
 * @property pageNumber 1-based, in reading order.
 * @property spineOnLeft the bound edge is the leaf's left edge, i.e. this is a right-hand page. The gutter
 *   shade, the corner radii and the turn's pivot all follow this.
 * @property solo the leaf faces nothing — the cover and the back cover are free single sheets, not halves
 *   of a spread. They draw no gutter and no stack edge.
 */
internal data class ReadLeaf(
    val pageNumber: Int,
    val spineOnLeft: Boolean,
    val solo: Boolean,
)

/**
 * **What an N-page one-sheet zine physically opens to** — the frozen `SPREADS` table, derived rather than
 * written down.
 *
 * The prototype hardcodes `[[null,1],[2,3],[4,5],[6,7],[8,null]]` for its eight-page sample. Written out,
 * the rule is: *the cover is alone on the right, the back cover is alone on the left, and everything
 * between them pairs up left-to-right.* That is a property of a folded sheet, not of the number eight, so
 * it is computed — the imposition engine already refuses to assume eight pages
 * ([ADR-028](../../../../../../docs/DECISIONS.md#adr-028)) and the reader has no business doing it either.
 *
 * The result is **flat and in reading order**, which is the simplification the prototype's
 * `(spread, side)` pair does not have: turning a page is `index ± 1`, and the model cannot represent a
 * position that is not a page. Each entry still knows its side, because the physical fact the frozen
 * design insists on — [V21-SPEC §5.2](../../../../../../docs/design/V21-SPEC.md), *the spine lands on the
 * correct side* — is a property of the leaf, not of the navigation.
 *
 * Odd page counts cannot reach a printed zine (every format seeds an even sheet), but they can reach a
 * unit test and an in-progress document, so the middle pairs simply run out: the last interior leaf faces
 * nothing and is drawn `solo`.
 */
internal fun bookletLeaves(pageCount: Int): List<ReadLeaf> {
    if (pageCount <= 0) return emptyList()
    if (pageCount == 1) return listOf(ReadLeaf(1, spineOnLeft = true, solo = true))
    val leaves = ArrayList<ReadLeaf>(pageCount)
    // The cover: alone, and bound on its left, because the book opens to its right.
    leaves += ReadLeaf(1, spineOnLeft = true, solo = true)
    // The interior: 2|3, 4|5, … Even page numbers are left-hand leaves, odd ones right-hand.
    var n = 2
    while (n < pageCount) {
        val facing = n + 1 < pageCount
        leaves += ReadLeaf(n, spineOnLeft = false, solo = !facing)
        if (facing) leaves += ReadLeaf(n + 1, spineOnLeft = true, solo = false)
        n += 2
    }
    // The back cover: alone, and bound on its right — it is the last thing you close.
    leaves += ReadLeaf(pageCount, spineOnLeft = false, solo = true)
    return leaves
}

/**
 * **Act 0 — Read** ([ADR-058](../../../../../../docs/DECISIONS.md#adr-058)): the finished zine. It answers
 * the only question the Proof surface did not: *what did I make?*
 *
 * ### P5 rewrote this, and the reason is physical honesty
 *
 * It was a `HorizontalPager` — eight equal pages on a carousel, swiped, with the neighbours peeking in at
 * the edges. That reads as a slide deck of a zine. [ADR-101](../../../../../../docs/DECISIONS.md#adr-101)
 * P5 replaces it with the frozen `.book`: **one leaf at a time**, each bound on the side it is physically
 * bound on, with the closed remainder of the booklet showing as a stack edge beside it, turned by tapping
 * the left or right edge. A user who has just folded eight pages out of one sheet is holding the object
 * this is a picture of, and the two should agree.
 *
 * **No spread view, deliberately.** The frozen CSS carries the ruling in a comment and this implementation
 * inherits it: two mini-zine pages side by side on a phone are two pages you cannot read. A tablet spread
 * is a scope decision, not a free consequence of the booklet model.
 *
 * ### What did not change, and must not
 *
 * Document order **is** reading order — imposition rearranges pages for the printer and stays confined to
 * the print acts, so this walks `pages` as they are and shows none of the printer's furniture. Each page is
 * still `SceneRenderer.render(page, …)` replayed through [PagePreview], the identical path the Bench's
 * thumbs, the main canvas and the PDF export all use
 * ([ADR-028](../../../../../../docs/DECISIONS.md#adr-028)). `read == preview == export` stays structural
 * rather than promised: there is no second way to draw a page for the two to disagree about.
 *
 * ### Accessibility
 *
 * The pager was one swipeable stop; a booklet is a leaf plus two buttons, which is a **better** shape for
 * TalkBack than a swipe surface — the two turns are named, discoverable controls that disable at the ends
 * rather than a gesture with no announcement. The rendered leaf is a Canvas with no text nodes, so the
 * position is carried by a readout — and **P6 moved that readout out of this composable**. It used to be a
 * caption under the book, which P5 added because the top bar had no page readout to be the one. The frozen
 * top bar does: `.pcount`. Two live regions naming the same leaf would make TalkBack say the page twice on
 * every turn, so there is one, it is in the frozen place, and [onLeafChange] is how it gets there.
 *
 * @param pages the document's pages, in document (= reading) order.
 * @param onLeafChange the leaf now **on screen** changed — its 1-based page number. Fires when the turn
 *   lands, not when it starts, because the ticket names what you can see. [ProofScreen] renders it as the
 *   top bar's `.pcount`.
 * @param pageSizePt the page size in points — the same hoisted size the editor canvas renders at.
 * @param defaults document defaults the renderer folds in (background); same value the canvas uses.
 * @param reduceMotion the turn degrades to a short cross-fade, per the frozen
 *   `@media (prefers-reduced-motion)` block.
 * @param imageBytes import-master byte source for image elements; the default renders the missing-asset
 *   placeholder, exactly as [PagePreview] documents.
 * @param modifier sizing/placement applied by [ProofScreen].
 */
@Composable
public fun ProofReadAct(
    pages: List<Page>,
    pageSizePt: PtSize,
    defaults: DocumentDefaults,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
    imageBytes: AssetBytesSource = EmptyAssetBytes,
    onLeafChange: (Int) -> Unit = {},
) {
    // A zero-page document cannot happen through any shipping path (every format seeds its pages). The
    // guard is structural: with no leaves there is nothing to bind, and the readout would have no subject.
    if (pages.isEmpty()) return

    val leaves = remember(pages.size) { bookletLeaves(pages.size) }
    if (leaves.isEmpty()) return
    var index by rememberSaveable { mutableIntStateOf(0) }
    // Which leaf is actually on screen. It lags `index` for the length of the swing-out, because the leaf
    // that moves is the one you are leaving.
    //
    // **Saveable, and the review that caught it needed two interactions rather than an argument.** As a
    // plain `remember`, a rotation at page 4 restored `index = 3` and `shown = 0`, so the effect below
    // found them unequal on first composition and played a full swing — showing the *cover* for 180ms and
    // an animation nobody asked for before landing back where the user already was. `rememberPagerState`
    // did not have that hole; the rewrite reintroduced it.
    var shown by rememberSaveable { mutableIntStateOf(0) }
    var leaving by remember { mutableIntStateOf(0) } // -1 back, +1 forward, 0 at rest
    val turn = remember { Animatable(0f) } // 0 = seated, 1 = swung fully out

    // **One long-lived collector, not `LaunchedEffect(index)`** — and the difference is a real defect, not
    // a style preference. Keyed on `index`, a second tap *cancels* the running coroutine mid-swing: the
    // body restarts, returns early because it has already been overtaken, and leaves `turn` frozen
    // part-way with `leaving` still set. The leaf then sits tilted and half-transparent until some later
    // turn happens to animate it back. Two taps on two enabled controls, reachable within 180ms — Previous
    // enables the instant `index` moves, so tapping forward then back does it.
    //
    // The prototype carries the same invariant as a `if (turning) return` guard, which drops the second
    // input. Collecting sequentially is the better half of that trade: the turn always finishes seated,
    // and a second tap is honoured rather than swallowed.
    // A saved `index` can outlive the page count that made it valid (a document shrinks, the state is
    // restored). Clamping here rather than at the two tap sites keeps one definition of "a position that
    // exists" — and without it the first few Previous taps are silent no-ops, which is the defect this
    // package spent a review round removing.
    LaunchedEffect(leaves.lastIndex) { index = index.coerceIn(leaves.indices) }

    LaunchedEffect(Unit) {
        snapshotFlow { index }.collect { target ->
            if (target == shown) return@collect
            leaving = if (target > shown) 1 else -1
            turn.animateTo(1f, tween(if (reduceMotion) TURN_FADE_MS else TURN_OUT_MS, easing = TurnEasing))
            shown = target
            // The arriving leaf does not swing. Physically it was already there, under the one that just
            // lifted; it is *revealed*, not thrown in. The frozen prototype does the same thing by
            // accident — it replaces the node, so the incoming leaf has no transform to animate from — and
            // the accident is right, so it is kept on purpose.
            leaving = 0
            turn.animateTo(0f, tween(if (reduceMotion) TURN_FADE_MS else TURN_IN_MS, easing = TurnEasing))
        }
    }

    val leaf = leaves[shown.coerceIn(leaves.indices)]
    LaunchedEffect(leaf.pageNumber) { onLeafChange(leaf.pageNumber) }

    // `.stagewrap::before` — the desk's grain, and it has to sit **behind** the book. In the frozen file
    // it is a `::before` with no z-index while `.book` is positioned, so the book paints over it. The
    // shared [zinelyV21Grain] draws *over* its content, which here would grain the user's page a second
    // time — the leaf carries its own, at a different tile size and the opposite blend. `drawBehind` is
    // `::before`: over this surface's background, under everything it contains.
    //
    // `.phone::after`, a second and stronger grain over the whole device, is **not** ported — the ruling
    // the Library's desk already made, that it grains the simulated phone rather than the app.
    val stageGrain = rememberZinelyV21GrainBrush()
    Column(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                if (ZinelyV21Grain.IsSupported) {
                    drawRect(stageGrain, alpha = STAGE_GRAIN_ALPHA, blendMode = ZinelyV21Grain.ChromeBlend)
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val thresholdPx = with(LocalDensity.current) { DRAG_TURN_THRESHOLD.toPx() }
        Box(
            modifier = Modifier
                .weight(1f)
                // **The turn edges have to belong to the book.** The frozen design is a 390px phone frame,
                // where 30%-wide edges sit right beside the leaf. Unbounded, they follow the *window*: a
                // device pass in landscape found both chevrons marooned ~800px out in empty desk, pointing
                // at a leaf they no longer touch, and the app is not orientation-locked so any user who
                // rotates gets that. Bounding the stage keeps leaf and controls one object on every width;
                // the zones stay generous (30% of 480dp is still 144dp).
                .widthIn(max = STAGE_MAX_WIDTH)
                .fillMaxWidth()
                .testTag(ProofReadBookTestTag)
                // **A drag turns a leaf, because it is the first thing a hand does.** The booklet model
                // never needed the swipe deleted; it needed the tap *added*. Every reading surface on this
                // phone turns on a drag, so a reader that ignores one makes its single most probable first
                // input a silent no-op — and a silent no-op on arrival is indistinguishable from a frozen
                // screen. The tap edges remain the discoverable, screen-reader-reachable control; this is
                // the one the hand already knows, and both run through the same `index ± 1`.
                .pointerInput(leaves.lastIndex) {
                    var travelled = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { travelled = 0f },
                        onDragEnd = {
                            if (travelled <= -thresholdPx && index < leaves.lastIndex) index += 1
                            if (travelled >= thresholdPx && index > 0) index -= 1
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        travelled += dragAmount
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            // The two `.tapz` edges: 30% of the width each, full height, invisible, and *disabled at the
            // ends* rather than silently doing nothing — a control that accepts a tap and produces no
            // change is how a screen teaches a user that it is broken.
            //
            // Declared left, leaf, right — not leaf-then-edges. The leaf draws over them either way, but
            // traversal follows declaration, and V2-CONSTITUTION §4.5 requires it to follow the *visual*
            // row: a "Previous page" reached after a control drawn to its right is the exact divergence
            // `SurfaceTraversalOrderTest` exists to catch.
            TapEdge(
                label = Copy.ProofRead.PREVIOUS_PAGE,
                tag = ProofReadPrevTestTag,
                forward = false,
                enabled = index > 0,
                onTurn = { index -= 1 },
                modifier = Modifier.align(Alignment.CenterStart),
            )
            BookLeaf(
                page = pages[leaf.pageNumber - 1],
                leaf = leaf,
                totalPages = pages.size,
                pageSizePt = pageSizePt,
                defaults = defaults,
                imageBytes = imageBytes,
                swing = turn.value,
                leaving = leaving,
                reduceMotion = reduceMotion,
            )
            TapEdge(
                label = Copy.ProofRead.NEXT_PAGE,
                tag = ProofReadNextTestTag,
                forward = true,
                enabled = index < leaves.lastIndex,
                onTurn = { index += 1 },
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

/**
 * **The sheet is lit.** The V2.1 palette as it is in light theme, handed to the leaf and everything bound to
 * it in *both* themes.
 *
 * See [BookLeaf] for the measurement that forced this. In one sentence: the reader draws the user's real
 * page, that page fills no background of its own, and its default ink is black — so a leaf painted in the
 * room's palette turns the artwork invisible the moment the room goes dark, while still claiming to show
 * what a printer will put on white paper.
 *
 * Kept as one function rather than four constants so that no future caller can take the paper from here and
 * the edge from the theme, which would be the same defect with better manners.
 *
 * `internal`, not private, because the rule is not the reader's: [ProofLitPaper] hands it to the print
 * drawer's imposed sheet too, after device verification found that sheet dark while the two cover cards
 * directly beneath it were lit — one drawer showing the same piece of paper two ways.
 */
@Composable
internal fun rememberLitSheetPalette(): ZinelyV21Colors = remember { zinelyV21LightColors() }

/**
 * Runs [content] under the [rememberLitSheetPalette].
 *
 * **The test for whether a subtree belongs here is what it claims, not what it is made of** — see [BookLeaf]
 * for the two surfaces that failed it and the one that legitimately does not. A subtree answering *"this is
 * what the printer will produce"* is making a statement about a physical object and takes paper's palette in
 * both themes; a subtree answering *"here is what to do"* is an instruction and takes the room's.
 *
 * Provided through [LocalZinelyV21Colors] so every descendant follows without each one having to remember
 * to; a subtree where only the ground was swapped is how the ink goes invisible instead. Note that
 * `ZinelyTheme.elevation` is a **separate** local and is deliberately not overridden — a lit sheet on a dark
 * desk still casts the desk's shadow.
 */
@Composable
internal fun ProofLitPaper(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalZinelyV21Colors provides rememberLitSheetPalette(), content = content)
}

/** `.stagewrap::before{opacity:.35}` against the tile's own baked strength. */
private const val STAGE_GRAIN_ALPHA = ZinelyV21Grain.BakedAlpha * 0.35f

/** `.leaf::after{opacity:.4}`, multiplied into the page — paper takes ink, it does not glow. */
private const val LEAF_GRAIN_ALPHA = ZinelyV21Grain.BakedAlpha * 0.4f

/** `.leaf::after{background-size:130px}` — a finer tile than the desk's 160, because it is a finer paper. */
private val LEAF_GRAIN_TILE = 130.dp

/** The out-swing: the leaf you are leaving lifts off its spine. */
private const val TURN_OUT_MS = 180

/** The in-fade: the leaf underneath is revealed, without moving. */
private const val TURN_IN_MS = 140

/** Reduced motion — the frozen `.leaf{transition:opacity .12s linear}`, no rotation at all. */
private const val TURN_FADE_MS = 120

/** The frozen `.leaf{transition:transform .3s cubic-bezier(.22,.61,.24,1)}`. A linear swing is a machine. */
private val TurnEasing = CubicBezierEasing(0.22f, 0.61f, 0.24f, 1f)

/** The frozen `.book.turn-fwd .leaf{transform:rotateY(-26deg) translateX(-3%)}`. */
private const val TURN_DEGREES = 26f

/** `:root{--tiltP:-.9deg}` — the book's resting tilt. */
private const val BOOK_TILT_DEGREES = -0.9f
private const val TURN_SHIFT_FRACTION = 0.03f

/**
 * One `.tapz` — a third of the stage's width, the full height, and nothing to see.
 *
 * Invisible controls are usually a defect, and the chevron is what makes this one honest to a sighted user
 * — it is the whole affordance now that P6 has retired the status line that used to name the gesture. Both
 * edges are also real, labelled, focusable buttons in the semantics tree, so TalkBack and a keyboard get a
 * named control where a hand gets a big soft target.
 */
@Composable
private fun TapEdge(
    label: String,
    tag: String,
    forward: Boolean,
    enabled: Boolean,
    onTurn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxHeight()) {
        Box(
            modifier = Modifier
                .width(maxWidth * TAP_EDGE_FRACTION)
                .fillMaxHeight()
                .testTag(tag)
                // **No ripple**, and this is a fix rather than a preference. The zone is 30% of the stage
                // wide and full height, and the right edge is declared *after* the leaf — so the stock
                // ripple washed a grey circle across the user's artwork on every forward turn and, because
                // the left edge is painted over by the leaf, on no backward one. A press feedback that
                // depends on which way you are reading is not feedback. The frozen `.tapz:active` brightens
                // the 26px glyph and touches nothing else.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                ) { onTurn() }
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            // The hint that stops the first tap being a silent no-op — and, by *disappearing*, the
            // end-of-book signal the design had none of.
            //
            // **`inkSoft`, not the frozen `inkFaint` at 55% opacity.** The chevron is drawn on `desk`,
            // never on paper — the zones sit outside the leaf — and `inkFaint` measures **2.74:1** there in
            // light, under 1.4.11's 3:1 floor for a meaning-bearing graphic; the frozen opacity would take
            // it to 1.75. `ZinelyV21Colors.inkFaint`'s own KDoc bounds it to grounds it was measured on, and
            // `desk` is not one of them. This is also now the surface's *whole* affordance, since P6 retired
            // the status line that used to name the gesture — the one control a previous review already
            // returned NO-GO over.
            if (enabled) {
                Box(Modifier.size(26.dp)) {
                    ProofVectorIcon(
                        if (forward) ICON_CHEVRON_RIGHT else ICON_CHEVRON_LEFT,
                        ZinelyTheme.v21Colors.inkSoft,
                    )
                }
            }
        }
    }
}

private const val TAP_EDGE_FRACTION = 0.30f

/** The frozen `.tapz` chevrons — the same 24-unit paths the fold guide's nav arrows use. */
private const val ICON_CHEVRON_LEFT = "M15 5l-7 7 7 7"
private const val ICON_CHEVRON_RIGHT = "M9 5l7 7-7 7"

/**
 * How far a finger must travel before it counts as a page turn rather than a tap that wandered.
 *
 * Deliberately short. This is not a pager's snap threshold — nothing follows the finger, so there is no
 * half-turned state to commit or abandon, and the old pager's calibration between "a flick that carries
 * two pages" and "a nudge that springs back" is exactly the tuning a page turn should not need.
 */
private val DRAG_TURN_THRESHOLD = 40.dp

/**
 * The leaf itself: the user's page, on paper, bound on one edge.
 *
 * **P6 paints it — from the light palette, in both themes, and that is the package's sharpest finding.**
 *
 * The obvious sweep is `ZinelyTheme.v21Colors`, and it was built that way and put on a phone. In dark theme
 * the leaf came back **dark brown**, and the reason is three facts that are individually correct:
 *
 * - a page's default background is `Background.None`, and `None` over a `None` document default means
 *   [SceneRenderer] fills **nothing** — so this composable's own `.background(…)` *is* the paper the user
 *   sees, not a backdrop behind a rendered sheet;
 * - the default text colour is `ColorRgba.BLACK`;
 * - before the 37596 palette amendment, V2.1 `paper` flipped to `#332B22` in dark.
 *
 * Black ink on that old `#332B22` paper was **1.2:1**. The user's writing disappeared, on the one screen whose whole job is
 * to show them what they made — and the sheet it disappears on is a picture of something that will be
 * printed on **white paper** either way. The reader is not chrome; it is the artefact.
 *
 * So the leaf, its cut edge, its gutter and its stack all take [zinelyV21LightColors] regardless of theme.
 * **This is not a departure from V2.1; it is the rule V1 already encoded** — that palette pins `paper` and
 * `ink` to their light values in dark theme, with a comment saying the sheet stays lit so the ink on it
 * stays dark. The amended V2.1 palette now expresses this directly: the room gets dark, while paper remains
 * lit because a lamp does not change what a printer will do.
 *
 * The rest of the frozen recipe is transcribed as written: a `1.5dp ink` cut edge and the printed `--hard`
 * shadow in `inkLine` in place of the elevation blur.
 *
 * The **fold guide's** diagram is deliberately not held to this rule — and the reason it is exempt is not
 * the one this comment first gave. *"It is only a schematic"* does not separate them: the imposed sheet's
 * cells are schematic stand-ins too, with no user artwork on them anywhere, and `ProofFold.sheet()` fills a
 * `paper` ground exactly as this leaf does. A review found that criterion false against the code.
 *
 * The line that actually holds is **what the surface claims**. The leaf and the imposed sheet answer *"what
 * will come out of the printer"* — so their ground is a statement about a physical object, and a statement
 * about a physical object may not change because a lamp went off. The fold diagram answers *"what do my
 * hands do next"*: it is an instruction, drawn in the room, and its marks are keyed by a legend rather than
 * read as output. That is why it follows the room and passed both P4 device passes doing so.
 *
 * The cost of the line is real and is not hidden: in dark theme the print drawer's sheet is now lit and the
 * fold drawer's sheet, one tap away, is not. Recorded in
 * [ADR-101 §6.11](../../../../../../../docs/DECISIONS.md#adr-101-p6-device) rather than resolved here —
 * re-palletting the fold diagram means redoing the eight-step contrast work that took P4 a NO-GO round, and
 * that is a package, not a comment fix.
 *
 * **The shadow is the leaf's, not the book's.** The frozen `filter:drop-shadow` sits on `.book`, so the
 * stack edge casts one too. [zinelyV21HardShadow] takes a [Shape], and leaf-plus-stack is not one — so the
 * leaf carries it and the stack edge, 7dp at its widest and tucked against the spine, does not. A single
 * shape for the pair would have to be built to make that difference visible.
 *
 * The paper card is sized to the **page**, not to the slot — the same rule the editor canvas follows since
 * the two-scales defect: the backing and the render take one scale, so they cannot disagree about where the
 * sheet is.
 */
@Composable
private fun BookLeaf(
    page: Page,
    leaf: ReadLeaf,
    totalPages: Int,
    pageSizePt: PtSize,
    defaults: DocumentDefaults,
    imageBytes: AssetBytesSource,
    swing: Float,
    leaving: Int,
    reduceMotion: Boolean,
) {
    val colors = rememberLitSheetPalette()
    val leafGrain = rememberZinelyV21GrainBrush(LEAF_GRAIN_TILE)
    BoxWithConstraints(
        // `.stagewrap{padding: --gap-xs --gap-xl --gap-md}` — the padding the pager's `contentPadding`
        // used to supply and the rewrite briefly dropped, which put the sheet edge-to-edge against the
        // screen. A booklet that touches both bezels is not an object resting on a desk; it is a
        // background, and the whole reason for this package is that the reader should read as an object.
        modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        if (pageSizePt.width <= 0.0 || pageSizePt.height <= 0.0 || wPx <= 0f || hPx <= 0f) {
            return@BoxWithConstraints
        }

        // `LEAF_WIDTH_FRACTION` is the frozen `.leaf{width:244px}` inside a 390px frame, restated as a
        // proportion because Compose has no 390px frame. Without it the leaf is fitted to whichever axis
        // binds first, which on a tall page means "as large as the stage allows" — the frozen design picks
        // a size instead, and the air around the book is what makes it one.
        //
        // It is also what keeps the book from growing and shrinking as you turn: the leaf is the same
        // width whether or not a stack edge sits beside it, because the fraction does not know about the
        // stack. An earlier version subtracted the stack as well, which was dead arithmetic — it could
        // only bind below ~24dp of stage — under a comment claiming it was the thing doing this job.
        val availW = (wPx * LEAF_WIDTH_FRACTION).coerceAtLeast(1f)
        val scale = min(availW / pageSizePt.width, hPx / pageSizePt.height).toFloat()
        val tape = remember(page, pageSizePt, defaults) {
            SceneRenderer.render(page, pageSizePt, defaults)
        }
        val density = LocalDensity.current
        val outW = with(density) { (pageSizePt.width * scale).toFloat().toDp() }
        val outH = with(density) { (pageSizePt.height * scale).toFloat().toDp() }

        // `.leaf.R{border-radius:0 md md 0}` / `.leaf.L{border-radius:md 0 0 md}` — the bound edge is
        // square because it is a fold, and only the outer corners are rounded. A `solo` leaf is a free
        // sheet, so `.leaf.solo` softens the bound edge too.
        val bound = if (leaf.solo) RADIUS_FREE else RADIUS_BOUND
        val shape = if (leaf.spineOnLeft) {
            RoundedCornerShape(topStart = bound, bottomStart = bound, topEnd = RADIUS_OUTER, bottomEnd = RADIUS_OUTER)
        } else {
            RoundedCornerShape(topStart = RADIUS_OUTER, bottomStart = RADIUS_OUTER, topEnd = bound, bottomEnd = bound)
        }

        Row(
            modifier = Modifier.graphicsLayer {
                alpha = 1f - swing
                // `.book{transform:rotate(var(--tiltP))}` — a nine-tenths of a degree of hand. It is the
                // same device the Library's covers use, and it is what stops a rectangle of paper reading
                // as a rectangle of UI. Static, not motion: nothing animates it, so reduced motion has no
                // opinion about it.
                rotationZ = BOOK_TILT_DEGREES
                if (leaving != 0 && !reduceMotion) {
                    // The pivot is the spine. Rotating about the leaf's centre would be a card flipping,
                    // which is the one thing V21-SPEC §5.2 says this must not look like.
                    transformOrigin = TransformOrigin(if (leaf.spineOnLeft) 0f else 1f, 0.5f)
                    rotationY = -TURN_DEGREES * leaving * swing
                    translationX = -size.width * TURN_SHIFT_FRACTION * leaving * swing
                }
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Behind a right-hand leaf is everything already read; behind a left-hand one, everything
            // left. The stack is drawn to that depth, which is what makes it worth drawing at all.
            val behind = if (leaf.spineOnLeft) leaf.pageNumber - 1 else totalPages - leaf.pageNumber
            if (!leaf.solo && leaf.spineOnLeft) StackEdge(outH, behind, atStart = true)
            Box(
                modifier = Modifier
                    .testTag(proofReadPageTag(leaf.pageNumber))
                    .size(outW, outH)
                    // The printed shadow: an offset copy of the sheet, no blur. See [zinelyV21HardShadow]
                    // for why V2.1 draws depth this way rather than with elevation.
                    .zinelyV21HardShadow(ZinelyV21Dimens.hardShadow, colors.inkLine, shape)
                    .clip(shape)
                    .background(colors.paper)
                    .border(1.5.dp, colors.ink, shape)
                    // `.leaf::after` — the paper's own tooth, multiplied into the user's page. Placed here
                    // rather than as a sibling because it must fall ON the printing: paper takes ink.
                    .zinelyV21Grain(leafGrain, LEAF_GRAIN_ALPHA, ZinelyV21Grain.PaperBlend)
                    // The card is decoration around the render; the act's own label and the live readout
                    // carry the a11y meaning, so this adds no traversable stop per page.
                    .clearAndSetSemantics { },
            ) {
                PagePreview(
                    tape = tape,
                    sheet = pageSizePt,
                    screenPxPerPt = scale,
                    modifier = Modifier.fillMaxSize(),
                    imageBytes = imageBytes,
                )
                if (!leaf.solo) Gutter(spineOnLeft = leaf.spineOnLeft)
            }
            if (!leaf.solo && !leaf.spineOnLeft) StackEdge(outH, behind, atStart = false)
        }
    }
}

// The published V2.1 radius scale, not hand-picked neighbours of it: `--br-md:14px` on the outer corners,
// `--br-xs:4px` on a free sheet's bound edge, `0` on a real fold. The first build used 10/3 — close enough
// to look right and wrong enough to be a fourth source of truth for a scale ADR-101 §3.3 exists to publish.
private val RADIUS_OUTER = 14.dp
private val RADIUS_BOUND = 0.dp
private val RADIUS_FREE = 4.dp
/** The frozen `.stack{width:7px}` — now the width at the *thickest*, i.e. the middle of the book. */
private val STACK_WIDTH = 7.dp
private val STACK_PER_LEAF = 1.2.dp
private val STACK_MIN = 2.dp

/** The frozen `.leaf{width:244px}` in a 390px frame, less `.stagewrap`'s 24px sides: 244 / 342. */
private const val LEAF_WIDTH_FRACTION = 0.71f

/**
 * The widest the book and its two turn edges are allowed to spread. Comfortably above every phone's portrait
 * width, so portrait is untouched; it only binds on a landscape phone or a tablet, where the frozen
 * proportions would otherwise stretch to the window and leave the controls nowhere near the object.
 */
private val STAGE_MAX_WIDTH = 480.dp

/**
 * `.leaf::before` — the gutter: a soft shade falling away from the bound edge, and a hairline on the fold
 * itself. It is what tells you which side this leaf is attached on without a label, and it is drawn over
 * the user's page because a real gutter shades whatever is printed there.
 */
@Composable
private fun Gutter(spineOnLeft: Boolean) {
    // `--shade` is its own token in both themes, and P6 replaces a derivation with it. P5 mixed the wash
    // by hand from `ink` at 7% — correct in V1 light, and the reason the comment here used to be an
    // argument about which token stays dark. The 37596 amendment makes the answer physical and
    // theme-invariant: `--shade` is the lit ink at 7%, `rgba(39,39,15,.07)`.
    // The lit palette, for the reason [BookLeaf] gives: this shades the user's page, so it belongs to the
    // sheet and not to the room.
    val colors = rememberLitSheetPalette()
    val shade = colors.shade
    val hair = colors.hair
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                val w = GUTTER_WIDTH.toPx()
                val x0 = if (spineOnLeft) 0f else size.width - w
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = if (spineOnLeft) {
                            listOf(shade, androidx.compose.ui.graphics.Color.Transparent)
                        } else {
                            listOf(androidx.compose.ui.graphics.Color.Transparent, shade)
                        },
                        startX = x0,
                        endX = x0 + w,
                    ),
                    topLeft = Offset(x0, 0f),
                    size = Size(w, size.height),
                )
                val hx = if (spineOnLeft) 0.5f else size.width - 0.5f
                drawLine(hair, Offset(hx, 0f), Offset(hx, size.height), strokeWidth = 1f)
            }
            .clearAndSetSemantics { },
    )
}

private val GUTTER_WIDTH = 22.dp

/**
 * `.stack` — the closed remainder of the booklet, seen edge-on, so you can feel where you are in it
 * without counting. It sits on the bound side of the leaf: behind a right-hand page is everything you have
 * already read, behind a left-hand one is everything left.
 *
 * **Its width is [behind] pages thick, and it was not.** The frozen CSS declares a flat `7px`, and built
 * that way the sentence above is false — 7dp at page 2 and 7dp at page 7 says only *which side is bound*,
 * which the gutter already says, so what is left is a grey stripe a reader stops seeing by the second
 * turn. A review read it cold and called it exactly that. Drawn to depth it earns the claim and answers
 * *how much is left* for free. The floor keeps a single remaining leaf an edge rather than nothing.
 */
@Composable
private fun StackEdge(height: androidx.compose.ui.unit.Dp, behind: Int, atStart: Boolean) {
    // The lit palette: this is the edge of the same stack of paper the leaf is the top of.
    val colors = rememberLitSheetPalette()
    // `.stack.L{border-radius: --br-sm 0 0 --br-sm}` — 8px, the scale's own value.
    val shape = if (atStart) {
        RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
    } else {
        RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
    }
    Box(
        modifier = Modifier
            .testTag(ProofReadStackTestTag)
            .height(height - 14.dp)
            .width((STACK_PER_LEAF * behind).coerceIn(STACK_MIN, STACK_WIDTH))
            .clip(shape)
            .background(colors.paperEdge)
            .drawWithContent {
                drawContent()
                // The pages inside it, as ruled hairlines — the frozen repeating-linear-gradient. `hair`,
                // not `paper`: the rules are the gaps between sheets seen edge-on, so they are darker than
                // the stack, not lighter. Built the other way round in P5 they read as a highlight.
                var y = 0f
                while (y < size.height) {
                    drawLine(colors.hair, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += 4.dp.toPx()
                }
            }
            .clearAndSetSemantics { },
    )
}
