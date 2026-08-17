package com.aritr.zinely.feature.editor

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.render.SceneRenderer
import com.aritr.zinely.render.android.AssetBytesSource
import com.aritr.zinely.ui.components.zinelyV21OuterRing
import com.aritr.zinely.ui.components.zinelyV21Pressable
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Press
import com.aritr.zinely.ui.theme.LocalZinelyV2Colors
import com.aritr.zinely.ui.theme.ZinelyV2Colors
import com.aritr.zinely.ui.theme.zinelyV21LightColors
import com.aritr.zinely.ui.theme.ZinelyHaptic
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV2IconPaint
import com.aritr.zinely.ui.theme.ZinelyV2Icons
import com.aritr.zinely.ui.theme.toImageVector
import kotlin.math.min

/** Test tag on the frozen `.navrow`. */
public const val BenchNavRowTestTag: String = "bench-nav-row"

/** Test tag on the frozen `.gridbtn`. */
public const val BenchGridButtonTestTag: String = "bench-grid-button"

/** Test tag on the frozen `.filmstrip`. */
public const val BenchFilmstripTestTag: String = "bench-filmstrip"

/** Per-thumb test tag, 1-based to match the frozen `buildFilm()` and the spoken label. */
public fun benchThumbTag(pageNumber: Int): String = "bench-thumb-$pageNumber"

/** Per-thumb miniature test tag — the interior OD-22 made the real page. */
public fun benchThumbPageTag(pageNumber: Int): String = "bench-thumb-page-$pageNumber"

/**
 * Frozen `.navrow{gap:var(--gap-sm); padding:var(--gap-sm) var(--gap-md) var(--gap-xs)}`
 * (`v21-bench.html:328`).
 *
 * ⚠ **V2.1 declares no `height`**, where V2 pinned 56. Like `.bar`, the row is now its padding plus its
 * tallest child (the 38dp `.gridbtn`), which comes to `8 + 38 + 4 = 50`. The padding is also no longer
 * symmetric: 8 above, 4 below, so the row sits closer to the bar beneath it than to the sheet above.
 */
internal val BenchNavRowGap = ZinelyV21Dimens.gapSm
internal val BenchNavRowPaddingTop = ZinelyV21Dimens.gapSm
internal val BenchNavRowPaddingH = ZinelyV21Dimens.gapMd
internal val BenchNavRowPaddingBottom = ZinelyV21Dimens.gapXs

/**
 * Frozen `.gridbtn{38×38; border-radius:var(--br-sm); border:1.5px solid var(--ink); background:var(--paper);
 * color:var(--ink-soft); box-shadow:2px 2px 0 var(--ink-line)}` and `svg{17px; stroke-width:1.8}`
 * (`v21-bench.html:329-332`).
 *
 * ⚠ **This one is `--br-sm`, not a pill** — the only V2.1 chrome button on the Bench that is not. Its
 * neighbours in `.bar` are pills; the grid button is a small square with softened corners, which is what
 * the frozen file says and is transcribed rather than regularised.
 */
internal val BenchGridBtnSize = 38.dp
internal val BenchGridBtnRadius = ZinelyV21Dimens.radiusSm
internal val BenchGridGlyphSize = 17.dp
internal const val BenchGridGlyphStroke: Float = 1.8f

/** Frozen `.filmstrip{gap:var(--gap-sm); padding:var(--gap-hair) 0 var(--gap-xs)}` (`v21-bench.html:333`). */
internal val BenchStripGap = ZinelyV21Dimens.gapSm
internal val BenchStripPaddingH = 0.dp
internal val BenchStripPaddingTop = ZinelyV21Dimens.gapHair
internal val BenchStripPaddingBottom = ZinelyV21Dimens.gapXs

/** Frozen `.fpage{width:29px;height:38px}` (`v21-bench.html:335`), up from V2's 26×34. */
internal val BenchThumbWidth = 29.dp
internal val BenchThumbHeight = 38.dp

/**
 * Frozen `.fpage{border-radius:var(--br-xs)}` (`v21-bench.html:335`) — **uniform**, where V2 drew an
 * asymmetric `1.5 3 3 1.5`.
 *
 * ⚠ The spine goes with it. V2's thumbnail carried a 2dp `--matcha` edge and squarer left corners so the
 * sheet read as *bound*; V2.1's `.fpage` declares neither. Deleting a detail this deliberate is the kind
 * of thing a later reader assumes was an oversight, so it is recorded here rather than only in the diff.
 *
 * ⚠ **This paragraph used to explain the deletion by saying the strip "is a row of page numbers, and the
 * number is what identifies the page now". That was true for about a day.** The 2026-08-13 owner ruling
 * (AMENDMENT LOG A2) struck the number too, and the interior became a live miniature — so the sentence
 * survived as an explanation of a design that had already been replaced, sitting *above* the ⚠ block that
 * replaced it, which is the order a reader meets them in. Corrected in place, and left visible: KDoc that
 * narrates a superseded intent is worse than none, because it reads as current.
 */
internal val BenchThumbRadius = ZinelyV21Dimens.radiusXs

// ⚠ `BenchThumbNumberSize = 9.6.sp` stood here, citing `.fpage{font-size:.6rem;font-variant-numeric:
// tabular-nums}`. The 2026-08-13 owner ruling (AMENDMENT LOG A2) struck those properties from the frozen
// file — the sheet's interior is the page, not its number — so the constant had no rule behind it and no
// reference in front of it. Deleted rather than left: a constant quoting a declaration that no longer
// exists is how a later reader reinstates a number nobody wants.

/**
 * Frozen `.fpage.on{box-shadow:0 0 0 3px var(--berry)}` (`v21-bench.html:338-340`). The rule's `color`
 * half fell with the page number (AMENDMENT LOG A2); the ring is the whole state now.
 *
 * ⚠ **The current page no longer moves.** V2 scaled it 1.16×, lifted it 2dp, raised its shadow from 2 to 9
 * and hung a 4dp strawberry dot above it. V2.1 draws a flat 3dp `berry` ring and darkens the number — no
 * transform, no elevation, no dot, and therefore no `.2s` settle to animate.
 *
 * The colour is ruled, not chosen: the frozen file carries the reasoning inline (`:338-339`) and
 * [V21-SPEC §163-164](../../../../../../../../docs/design/V21-SPEC.md) states it — *"the current page in the
 * filmstrip was ringed butter. That is a state, and V2-TOKENS assigns the current-page dot to strawberry, so
 * it is now berry."* **Butter is material, never a state**; a token sweep that "corrects" this ring to butter
 * is reintroducing the defect the ruling closed.
 */
internal val BenchThumbCurrentRing = 3.dp

/**
 * The frozen page navigator — `.navrow`, `.gridbtn`, `.filmstrip` and the `.pthumb` sheets
 * (`v2-bench.html:275-289`, markup `:481-485`); [ADR-095](../../../../../../../../docs/DECISIONS.md#adr-095)
 * rows 5.1–5.10.
 *
 * ### The thumb's interior is the page, by ruling
 *
 * The frozen file drew three faint rules inside each interior thumb — a *drawing of text*, which was
 * honest in a prototype with no document and false in a product with one. The shipped strip has always
 * rendered a **live miniature** through the canvas's own [SceneRenderer] tape, and
 * [OD-22](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-053-ruling) ruled that the *specification*
 * moves rather than the implementation: `.pthumb i` and the `<i>` that carried it are **deleted from the
 * frozen file**, and everything else about the sheet — its size, its asymmetric radius, its spine, its
 * contact shadow, its `.2s` settle, the current sheet's lift and its strawberry dot — is transcribed here
 * exactly. The accepted price is recorded with the ruling: at 26×34dp the miniature reads as *"something is
 * on this page, and roughly where"*, not as *"which photo"*, and this composable does not enlarge the sheet
 * to compensate.
 *
 * ### What replaces `EditorPageStrip`
 *
 * All of it. The retired strip drew 48×66dp tilted cards with a yellow tape marker, and its own KDoc
 * recorded that marker as *"a conflict between the frozen Bench spec and editor-visual-direction.md's tape
 * metaphor, left for a design decision."* The freeze **is** that decision: the current sheet lifts, scales,
 * takes a `--matcha` border and carries the one strawberry dot in the whole Bench.
 *
 * ### Cover and back come from the position, as the freeze says
 *
 * The frozen `buildFilm()` marks covers with `i===1||i===NP`, and so does this — see [benchCoverAt] for why
 * the role-based first cut was reverted after Device Pass 1.
 *
 * ### Touch targets
 *
 * The sheets are 26×34dp and the grid button is 34dp, all far under the 48dp floor, and
 * [D-009](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-009--no-control-in-the-frozen-trilogy-declares-a-minimum-touch-target-and-most-measure-well-under-48dp)
 * names *this exact surface* — *"the Bench's filmstrip and swatch grid keep their frozen 26×34"* — and rules
 * *extend the target, keep the paint*. Compose's own pointer-input minimum does that: a `selectable` node
 * smaller than 48dp still reports a 48dp `touchBoundsInRoot`. No `minimumInteractiveComponentSize()` is used,
 * for the reason C4 recorded — it grows the *layout* slot and moves the frozen paint.
 *
 * Device Pass 1 confirmed this on the platform tree, at 420dpi: a non-current sheet's real node measures
 * 86×126px = 33×48dp. The vertical floor is met in full; horizontally the expansion is clipped by the
 * neighbouring sheets, which is inherent to a 26dp sheet on a 7dp pitch and is the honest reading of D-009
 * on this surface.
 *
 * ### What the platform publishes for a sheet, and why it looks wrong at first
 *
 * A `selectable` merges its descendants, so Compose publishes each sheet the way it publishes every merged
 * control: a focusable, clickable parent whose **name lives on a synthetic `contentDescription` child**.
 * Dumping the tree with `uiautomator` and grepping for the label therefore finds a node reporting
 * `clickable="false" focusable="false" selected="false"` — the synthetic child — while the real sheet node
 * sits beside it carrying the click, the `Role.Tab` selected state and the 48dp bounds. That is the
 * documented mechanism, not a defect: the ancestor rule means a service still lands on exactly **one** stop
 * per sheet, named from the synthetic child (`PlatformAccessibilityTree`, `:core:ui` test fixtures).
 *
 * Device Pass 1 misread it as a defect, and the fixes attempted for it — deleting the miniature's test tag,
 * then clearing the subtree's semantics — were reverted once the traversal was measured rather than
 * inferred. `BenchPageNavA11yTest` now asserts the traversal directly, which is the claim worth holding.
 *
 * @param pages the document's pages, rendered in order. `N` is `pages.size`, never a constant (row 5.16).
 * @param currentPageIndex the page on the canvas; its sheet lifts and takes the dot.
 * @param onSelectPage invoked with the tapped page index — the host dispatches `Intent.GoToPage`.
 * @param onOpenGrid summons [BenchPageGrid]; the grid does not exist until it is called (row 5.11a).
 */
@Composable
internal fun BenchPageNav(
    pages: List<Page>,
    currentPageIndex: Int,
    pageSizePt: PtSize,
    defaults: DocumentDefaults,
    onSelectPage: (Int) -> Unit,
    onOpenGrid: () -> Unit,
    modifier: Modifier = Modifier,
    imageBytes: AssetBytesSource = EmptyAssetBytes,
) {
    if (pages.isEmpty()) return
    val colors = ZinelyTheme.v21Colors
    val scroll = rememberScrollState()
    // Row 5.10: the frozen `setPage()` scrolls the selected sheet to the centre of the strip
    // (`scrollIntoView({inline:'center'})`, `:727`).
    //
    // The thumb's centre is *computed* from the strip's own frozen geometry rather than read back from
    // layout: every sheet is `BenchThumbWidth` wide with `BenchStripGap` between them, so the i-th centre
    // is arithmetic that is exact at any page count and any screen width — and, unlike a measurement, is
    // already correct on the first frame the current page changes. The one thing that cannot be computed
    // is the viewport, so that alone is measured.
    val density = LocalDensity.current
    var viewportPx by remember { mutableIntStateOf(0) }
    // The frozen `setPage()` scrolls when the page *changes*. Arriving at the editor is not a change, so
    // the first placement snaps: a strip that visibly slides itself into position on entry announces the
    // navigator instead of the page, which is the opposite of what row 5.10 is for.
    var placed by remember { mutableStateOf(false) }
    LaunchedEffect(currentPageIndex, viewportPx, pages.size) {
        if (viewportPx <= 0) return@LaunchedEffect
        // `BenchStripPaddingH` is 0 in V2.1 (`.filmstrip{padding:2px 0 4px}`); it stays in the arithmetic
        // rather than being folded away, because the term is what makes this expression the strip's geometry
        // instead of a coincidence that happens to hold while one value is zero.
        val centre = with(density) {
            (BenchStripPaddingH + BenchThumbWidth / 2 + (BenchThumbWidth + BenchStripGap) * currentPageIndex)
                .roundToPx()
        }
        val target = (centre - viewportPx / 2).coerceIn(0, scroll.maxValue)
        if (placed) scroll.animateScrollTo(target) else scroll.scrollTo(target)
        placed = true
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(BenchNavRowTestTag)
            // P3: `desk`, and **no top rule**. V2's `.navrow` carried a `--chrome-line` border-top; V2.1's
            // (`v21-bench.html:328`) declares only a background, exactly as `.bar` does. The room runs
            // continuously from the sheet down to the foot of the phone, and the controls' own ink borders
            // are what separate them from it.
            .background(colors.desk)
            .padding(
                top = BenchNavRowPaddingTop,
                start = BenchNavRowPaddingH,
                end = BenchNavRowPaddingH,
                bottom = BenchNavRowPaddingBottom,
            ),
        horizontalArrangement = Arrangement.spacedBy(BenchNavRowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BenchGridButton(onOpenGrid)
        Row(
            modifier = Modifier
                .testTag(BenchFilmstripTestTag)
                // Measured BEFORE `horizontalScroll`, which is what makes it the viewport: anything after
                // it is inside the scrolling content and would report the full strip width instead.
                .onGloballyPositioned { viewportPx = it.size.width }
                .horizontalScroll(scroll)
                // AFTER `horizontalScroll`, deliberately. The scroll modifier contributes a semantics node
                // of its own, so a traversal group declared above it is not the sheets' immediate semantic
                // parent and their `traversalIndex` is never consulted. Declared here, the group and the
                // scroll container are one node and the ordering applies.
                .semantics { isTraversalGroup = true }
                .padding(
                    start = BenchStripPaddingH,
                    end = BenchStripPaddingH,
                    top = BenchStripPaddingTop,
                    bottom = BenchStripPaddingBottom,
                ),
            horizontalArrangement = Arrangement.spacedBy(BenchStripGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            pages.forEachIndexed { i, page ->
                BenchPageThumb(
                    pageNumber = i + 1,
                    pageCount = pages.size,
                    page = page,
                    current = i == currentPageIndex,
                    pageSizePt = pageSizePt,
                    defaults = defaults,
                    imageBytes = imageBytes,
                    onClick = { onSelectPage(i) },
                )
            }
        }
    }
}

/**
 * Frozen `.gridbtn` (`v21-bench.html:329-332`) — a 38dp `paper` square, `--br-sm` corners, a 1.5dp `ink`
 * border and [ZinelyV21Press.Flat]'s 2dp printed shadow, holding the 17dp four-pane glyph in `inkSoft`.
 *
 * The press is the language's own: translate down-right, shadow to nothing. Nothing here animates.
 */
@Composable
private fun BenchGridButton(onOpenGrid: () -> Unit) {
    val colors = ZinelyTheme.v21Colors
    val openGrid = benchTap(action = onOpenGrid)
    val shape = RoundedCornerShape(BenchGridBtnRadius)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(BenchGridBtnSize)
            // Nothing that clips may precede the press — the shadow paints outside the node.
            .zinelyV21Pressable(pressed, ZinelyV21Press.Flat, colors.inkLine, shape)
            .clip(shape)
            .background(colors.paper)
            .border(BenchChromeBorder, colors.ink, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = openGrid)
            .testTag(BenchGridButtonTestTag)
            .semantics {
                contentDescription = Copy.PageNav.ALL_PAGES
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ZinelyV2Icons.Grid.toImageVector(
                BenchGridGlyphSize,
                ZinelyV2IconPaint.Stroke(BenchGridGlyphStroke),
            ),
            contentDescription = null,
            tint = colors.inkSoft,
            modifier = Modifier.size(BenchGridGlyphSize),
        )
    }
}

/**
 * Is this the front cover, the back, or an interior page? **By position, as the freeze says.**
 *
 * The frozen `buildFilm()` marks covers with `i===1||i===NP` (`v2-bench.html:697`). C5's first cut read
 * `PageRole` instead and [ADR-095 §3](../../../../../../../../docs/DECISIONS.md#adr-095-blockers) called that
 * *"strictly better"* on the grounds that the model already carried the roles. **Device Pass 1 falsified
 * that claim:** `EditorBootstrap.kt:26` and `RoomProjectRepository.kt:475` create *every* page as
 * `PageRole.INTERIOR`, and `FRONT_COVER`/`BACK_COVER` exist only in `:core:imposition`'s print-time *panel*
 * convention. So the role branch never fired on a real document — no cover label, no matcha spine, no
 * `COVER`/`BACK` badge — while every test that proved those rows fabricated roles the product never
 * produces. Reverting to the frozen index rule is literal parity, and it agrees with the imposition
 * convention that already maps panel 1 to the front cover and panel 8 to the back.
 */
/**
 * The sheet's palette inside a filmstrip thumb — [room] with **exactly the five tokens `.pthumb` re-declares**
 * taken from the light theme, and nothing else touched
 * ([OD-23](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-059-ruling), `v2-bench.html:282`).
 *
 * **Five, not eight, and not the whole scheme.** `.page` restates eight because it draws `--matcha` chrome of
 * its own; a thumb does not. Its `--matcha` appears only as the cover spine and the current sheet's border,
 * and its `--strawberry` only as the dot 7dp *above* the sheet — all three are marks the **row** puts on the
 * sheet, and all three have to keep reading against the chrome around them rather than against the paper. C1
 * drew this same line when it left `.page`'s shadow to the room ([D-010](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-010)'s
 * lesson, reinstated once by a fix for a different defect and caught by review); this is that lesson applied
 * before the fact rather than after it.
 *
 * Why it exists at all: [OD-22](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-053-ruling) made the
 * thumb's interior the real page, and the ground under it stayed the room's `--paper`. In dark theme that
 * measured **1.21:1** for a page's own words against its own sheet, on a real document, on hardware — the
 * same failure [D-035](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-035) found on the canvas.
 *
 * Pure, so the set is asserted against the frozen `.pthumb` block without a composition.
 *
 * ### ⚠ The lit values are V2.1's, and the scheme type is still V2's
 *
 * The five tokens are lifted from **`zinelyV21LightColors()`**, not `zinelyV2LightColors()`, while the
 * returned scheme stays a [ZinelyV2Colors] because that is the type `LocalZinelyV2Colors` publishes and
 * what the miniature renderer reads. The type is a seam; the *values* are the specification.
 *
 * That distinction was missed once, and it was visible: the strip kept V2's `#F7F2E7` paper while
 * [benchGridCardIsland] had already moved to V2.1's `#FFF6E8`, so **one screen drew the same eight pages
 * on two different papers** — a filmstrip sheet and a grid card differing by a byte the eye reads as two
 * kinds of paper on one desk. That is precisely the defect OD-47 assigns to P5 and that
 * [BenchPageGrid]'s own KDoc claims to be closing; the strip simply was not converted with it. Found by
 * review, by opening both islands rather than by trusting either file's account of itself.
 */
internal fun benchThumbIsland(room: ZinelyV2Colors): ZinelyV2Colors {
    val light = zinelyV21LightColors()
    return room.copy(
        paper = light.paper,
        paperEdge = light.paperEdge,
        ink = light.ink,
        inkSoft = light.inkSoft,
        inkFaint = light.inkFaint,
    )
}

internal enum class BenchCover { FRONT, BACK, NONE }

internal fun benchCoverAt(pageNumber: Int, pageCount: Int): BenchCover = when {
    pageNumber == 1 -> BenchCover.FRONT
    pageNumber == pageCount -> BenchCover.BACK
    else -> BenchCover.NONE
}

/**
 * The frozen thumb label (row 5.9), assembled from [Copy.PageNav] over [benchCoverAt].
 *
 * The case analysis lives here rather than in `:core:copy` because that module has **zero dependencies**;
 * `Copy` keeps the wording, this keeps the analysis.
 */
internal fun benchPageLabel(pageNumber: Int, pageCount: Int): String =
    when (benchCoverAt(pageNumber, pageCount)) {
        BenchCover.FRONT -> Copy.PageNav.frontCoverLabel(pageNumber, pageCount)
        BenchCover.BACK -> Copy.PageNav.backCoverLabel(pageNumber, pageCount)
        BenchCover.NONE -> Copy.PageNav.pageLabel(pageNumber, pageCount)
    }

/**
 * One frozen `.pthumb` — a 26×34 sheet of paper with the page drawn inside it.
 *
 * The sheet keeps a `Role.Tab` `selectable` with a selected state rather than the frozen `role=button`.
 * That is deliberate and recorded in [ADR-095 §3](../../../../../../../../docs/DECISIONS.md#adr-095-blockers):
 * the strip *is* a page picker, CI-29/CI-30 already assert the stronger contract, and a parity phase does not
 * weaken a conformance path (C2b's rule). The frozen **label** is adopted in full.
 */
@Composable
private fun BenchPageThumb(
    pageNumber: Int,
    pageCount: Int,
    page: Page,
    current: Boolean,
    pageSizePt: PtSize,
    defaults: DocumentDefaults,
    imageBytes: AssetBytesSource,
    onClick: () -> Unit,
) {
    val colors = ZinelyTheme.v21Colors
    val shape = RoundedCornerShape(BenchThumbRadius)
    // Snap, not Tick: moving the strip is a selection landing, not a button firing.
    val pick = benchTap(ZinelyHaptic.Snap, onClick)
    // OD-23's island, still read from the ROOM's scheme so the ring below is taken from `colors` — it is
    // the row's mark on the sheet, not the page's ink.
    // `remember`ed on the room, not recomputed per item: the island builds a whole light scheme and then
    // copies the room over it, and this composable runs once per sheet. Unremembered that is 2N scheme
    // allocations per frame in a surface that scrolls. Cheap to hold, and the key is the only input.
    val room = ZinelyTheme.v2Colors
    val sheet = remember(room) { benchThumbIsland(room) }

    Box(
        modifier = Modifier
            .size(width = BenchThumbWidth, height = BenchThumbHeight)
            // The frozen `.cur{z-index:2}` (`:288`) is NOT transcribed, and this is a deliberate,
            // recorded deviation rather than an omission.
            //
            // `Modifier.zIndex` reorders the platform accessibility tree's children, not only the paint:
            // with it, the current sheet is published LAST, so a screen reader met page 1 after pages 2, 3
            // and 4 — caught by `BenchPageNavA11yTest.every_sheet_is_one_named_traversal_stop` and by
            // CI-31's `SurfaceTraversalOrderTest`. `traversalIndex` did not override it. The freeze's own
            // geometry says what the z-index was worth: at `scale(1.16)` a 26dp sheet overhangs 2.08dp a
            // side into a 7dp gap, so no two sheets ever overlap — the only thing the raise protected was
            // the current sheet's shadow tail against a neighbour 7dp away. Reading order is a conformance
            // path (CI-29/30/31); a shadow tail is not. Post-freeze accessibility improvements are allowed
            // in terms (CLAUDE.md, DESIGN FREEZE), so the order is kept and the tail is given up.
            //
            // ⚠ **P3 removed the transform that argument was about.** V2's current sheet scaled 1.16× and
            // lifted 2dp; V2.1's does neither, so there is no overhang, no z-order question and nothing to
            // animate. The paragraph above is kept because the *conclusion* it reached — reading order is a
            // conformance path — is what still holds the `traversalIndex` below in place.
            .selectable(selected = current, onClick = pick, role = Role.Tab)
            .semantics {
                contentDescription = benchPageLabel(pageNumber, pageCount)
                stateDescription =
                    if (current) Copy.PageStrip.CURRENT_PAGE else Copy.PageStrip.NOT_SELECTED
                // Reading order is page order, stated rather than inferred. It is belt-and-braces beside
                // the dropped `zIndex` above: an explicit index cannot be undone by a later paint change.
                traversalIndex = pageNumber.toFloat()
            }
            .testTag(benchThumbTag(pageNumber)),
    ) {
        Box(
            modifier = Modifier
                .size(width = BenchThumbWidth, height = BenchThumbHeight)
                // `.fpage.on{box-shadow:0 0 0 3px var(--berry)}` — the current page's whole signal, and a
                // ring **outside** the sheet: it must not eat the 29×38 the miniature is drawn into, and
                // the strip's 8dp gap is what gives it room. Drawn for the current sheet only, so a
                // non-current one is a plain bordered sheet with no depth at all — V2.1's filmstrip does
                // not float its sheets off the desk.
                .then(
                    if (current) {
                        Modifier.zinelyV21OuterRing(BenchThumbCurrentRing, colors.berry, shape)
                    } else {
                        Modifier
                    },
                )
                .clip(shape)
                // OD-23: the sheet's own paper, from the island — not the room's. See [benchThumbIsland].
                .background(sheet.paper)
                // Uniform 1.5dp `ink` on every sheet, current or not. V2 switched this border to `matcha`
                // for the current page; V2.1 says the *ring* carries that state, and doubling it into the
                // border too would make the current sheet differ from its neighbours in two ways at once.
                .border(BenchChromeBorder, colors.ink, shape),
        ) {
            // The interior: the page itself (OD-22). Drawn first, so the spine edge below stays on top of
            // it — a sheet's edge is in front of its ink, not behind it.
            //
            // The island is provided around it the way CSS inherits: anything the miniature draws from the
            // scheme reads the page's tokens, not the room's, exactly as `.pthumb`'s five restated custom
            // properties cascade to its contents.
            CompositionLocalProvider(LocalZinelyV2Colors provides sheet) {
                BenchThumbPage(
                    page = page,
                    pageSizePt = pageSizePt,
                    defaults = defaults,
                    imageBytes = imageBytes,
                    modifier = Modifier
                        .testTag(benchThumbPageTag(pageNumber))
                        .size(width = BenchThumbWidth, height = BenchThumbHeight),
                )
            }
            // ⚠ **The spine and the strawberry dot are gone**, and both were deliberate in V2: a 2dp
            // `--matcha` edge that made covers read as *bound*, and a 4dp dot 7dp above the current sheet.
            // V2.1's `.fpage` declares neither — the ring is the only state and the border is uniform. The
            // cover distinction survives where it is actually spoken, in [benchPageLabel]; it is no longer
            // drawn. Recorded here because a later reader will otherwise take the absence for an oversight.
        }
    }
}

/**
 * A live miniature of one [page] — the page's [SceneRenderer] tape replayed through [PagePreview], the same
 * render path the canvas uses, so a sheet is a faithful scaled-down twin rather than a second drawing model.
 *
 * Lifted verbatim from the retired `EditorPageStrip.PageThumbnail`: [OD-22](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-053-ruling)
 * kept the capability, so C5 keeps the code that provides it rather than writing a second one.
 */
@Composable
private fun BenchThumbPage(
    page: Page,
    pageSizePt: PtSize,
    defaults: DocumentDefaults,
    imageBytes: AssetBytesSource,
    modifier: Modifier,
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        if (pageSizePt.width <= 0.0 || pageSizePt.height <= 0.0 || wPx <= 0f || hPx <= 0f) {
            return@BoxWithConstraints
        }
        val scale = min(wPx / pageSizePt.width, hPx / pageSizePt.height).toFloat()
        val tape = remember(page, pageSizePt, defaults) {
            SceneRenderer.render(page, pageSizePt, defaults)
        }
        val density = LocalDensity.current
        val outW = with(density) { (pageSizePt.width * scale).toFloat().toDp() }
        val outH = with(density) { (pageSizePt.height * scale).toFloat().toDp() }
        PagePreview(
            tape = tape,
            sheet = pageSizePt,
            screenPxPerPt = scale,
            modifier = Modifier.size(width = outW, height = outH),
            imageBytes = imageBytes,
        )
    }
}
