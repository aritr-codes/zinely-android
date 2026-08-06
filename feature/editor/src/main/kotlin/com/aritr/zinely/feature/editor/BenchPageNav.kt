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
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.render.SceneRenderer
import com.aritr.zinely.render.android.AssetBytesSource
import com.aritr.zinely.ui.theme.LocalZinelyV2Colors
import com.aritr.zinely.ui.theme.ZinelyV2Colors
import com.aritr.zinely.ui.theme.zinelyV2LightColors
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

/** Frozen `.navrow{height:56px; gap:8px; padding:0 10px}` (`v2-bench.html:275`). */
internal val BenchNavRowHeight = 56.dp
internal val BenchNavRowGap = 8.dp
internal val BenchNavRowPaddingH = 10.dp

/** Frozen `.gridbtn{34×34; border-radius:9px}` and `svg{17px; stroke-width:1.8}` (`:277-278`). */
internal val BenchGridBtnSize = 34.dp
internal val BenchGridBtnRadius = 9.dp
internal val BenchGridGlyphSize = 17.dp
internal const val BenchGridGlyphStroke: Float = 1.8f

/** Frozen `.filmstrip{gap:7px; padding:9px 4px}` (`:279`). */
internal val BenchStripGap = 7.dp
internal val BenchStripPaddingH = 4.dp
internal val BenchStripPaddingV = 9.dp

/** Frozen `.pthumb{width:26px;height:34px}` (`:282`). */
internal val BenchThumbWidth = 26.dp
internal val BenchThumbHeight = 34.dp

/**
 * Frozen `.pthumb{border-radius:1.5px 3px 3px 1.5px}` (`:282`) — **asymmetric on purpose**. The two
 * left corners are squarer because that edge is the sheet's *spine*; a uniform radius would draw a
 * rounded card, which is the "slider pip" the freeze's physicality audit replaced.
 */
internal val BenchThumbRadiusSpine = 1.5.dp
internal val BenchThumbRadiusOuter = 3.dp

/** Frozen `.pthumb::before{width:2px}` — the spine edge (`:284`), `--matcha` on cover and back (`:285`). */
internal val BenchThumbSpineWidth = 2.dp

/** Frozen `.pthumb{transition:transform .2s var(--settle),box-shadow .2s}` (`:283`). */
internal const val BenchThumbMillis: Int = 200

/** Frozen `.pthumb.cur{transform:scale(1.16) translateY(-2px)}` (`:288`). */
internal const val BenchThumbCurScale: Float = 1.16f
internal val BenchThumbCurLift = 2.dp

/**
 * Frozen `.pthumb{box-shadow:0 2px 5px -2px}` (`:283`) rising to `.cur{box-shadow:0 9px 16px -6px}`
 * (`:288`). Compose's single-`elevation` shadow cannot transcribe a three-part CSS shadow literally; the
 * *blur* is the part that carries the read, so the elevation is set to the frozen blur-ish y-offset and
 * animated over [BenchThumbMillis] like the transform beside it.
 */
internal val BenchThumbShadow = 2.dp
internal val BenchThumbCurShadow = 9.dp

/** Frozen `.pthumb.cur::after` — a 4px `--strawberry` dot, 7px above the sheet (`:289`). */
internal val BenchThumbDotSize = 4.dp
internal val BenchThumbDotGap = 7.dp

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
    val colors = ZinelyTheme.v2Colors
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
            .height(BenchNavRowHeight)
            .testTag(BenchNavRowTestTag)
            .background(colors.chrome)
            // Frozen `border-top:1px solid var(--chrome-line)`, drawn rather than composed so the row
            // stays one node — the same choice, for the same reason, as C4's bar hairline.
            .drawBehind { drawRect(color = colors.chromeLine, size = Size(size.width, 1.dp.toPx())) }
            .padding(horizontal = BenchNavRowPaddingH),
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
                .padding(horizontal = BenchStripPaddingH, vertical = BenchStripPaddingV),
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

/** Frozen `.gridbtn` — a 34dp outlined square holding the 17dp four-pane glyph (`:277-278`, `:482-484`). */
@Composable
private fun BenchGridButton(onOpenGrid: () -> Unit) {
    val colors = ZinelyTheme.v2Colors
    Box(
        modifier = Modifier
            .size(BenchGridBtnSize)
            .clip(RoundedCornerShape(BenchGridBtnRadius))
            .border(1.dp, colors.chromeLine, RoundedCornerShape(BenchGridBtnRadius))
            .clickable(onClick = onOpenGrid)
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
 */
internal fun benchThumbIsland(room: ZinelyV2Colors): ZinelyV2Colors {
    val light = zinelyV2LightColors()
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
    val colors = ZinelyTheme.v2Colors
    val motion = ZinelyTheme.v2Motion
    val scale by animateFloatAsState(
        targetValue = if (current) BenchThumbCurScale else 1f,
        animationSpec = motion.settle(BenchThumbMillis),
        label = "bench-thumb-scale",
    )
    val lift by animateDpAsState(
        targetValue = if (current) -BenchThumbCurLift else 0.dp,
        animationSpec = motion.settle(BenchThumbMillis),
        label = "bench-thumb-lift",
    )
    // The frozen `.pthumb{transition:transform .2s var(--settle),box-shadow .2s}` (`v2-bench.html:283`)
    // names the shadow alongside the transform. Stepping it while the lift eases is what makes a lifted
    // sheet look like it snapped rather than rose. The freeze gives the shadow **no easing function** —
    // the settle curve is on the transform only — so this runs it on the settle curve anyway, which is a
    // 200ms difference in shape nobody can see on a 9dp shadow and keeps one spec for one gesture. An
    // earlier version of this comment misquoted the rule as easing both; the file is quoted verbatim now.
    val elevation by animateDpAsState(
        targetValue = if (current) BenchThumbCurShadow else BenchThumbShadow,
        animationSpec = motion.settle(BenchThumbMillis),
        label = "bench-thumb-shadow",
    )
    val shape = RoundedCornerShape(
        topStart = BenchThumbRadiusSpine,
        topEnd = BenchThumbRadiusOuter,
        bottomEnd = BenchThumbRadiusOuter,
        bottomStart = BenchThumbRadiusSpine,
    )
    // Frozen `.pthumb[data-cover]::before{--matcha}` — set by `buildFilm()` on the first and last sheet.
    val cover = benchCoverAt(pageNumber, pageCount) != BenchCover.NONE
    // OD-23's island. Read from the ROOM's scheme, so the spine, the current border and the dot below can
    // still be taken from `colors` — they are the row's marks on the sheet, not the page's ink.
    val sheet = benchThumbIsland(colors)

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
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = lift.toPx()
            }
            .selectable(selected = current, onClick = onClick, role = Role.Tab)
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
                .shadow(
                    elevation = elevation,
                    shape = shape,
                    ambientColor = colors.frameShadow,
                    spotColor = colors.frameShadow,
                )
                .clip(shape)
                // OD-23: the sheet's own paper, from the island — not the room's. See [benchThumbIsland].
                .background(sheet.paper)
                .border(1.dp, if (current) colors.matcha else sheet.paperEdge, shape),
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
            Box(
                modifier = Modifier
                    .size(width = BenchThumbSpineWidth, height = BenchThumbHeight)
                    .background(if (cover) colors.matcha else colors.deskEdge),
            )
        }
        if (current) {
            // Frozen `.cur::after` — 4px, 7px *above* the sheet, centred. It is drawn with a negative
            // offset rather than inside the sheet, because the freeze puts it outside: `top:-7px` on a
            // box the strip does not clip (`overflow-y:visible`, `:279`).
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    // `top:-7px` places the dot's TOP edge 7px above the sheet — so the offset is the gap
                    // itself, not the gap plus the dot. Getting that wrong puts it 4px too high, which at
                    // this size is the difference between "a berry on the sheet" and "a speck in the row".
                    .graphicsLayer { translationY = -BenchThumbDotGap.toPx() }
                    .size(BenchThumbDotSize)
                    .clip(CircleShape)
                    .background(colors.strawberry),
            )
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
