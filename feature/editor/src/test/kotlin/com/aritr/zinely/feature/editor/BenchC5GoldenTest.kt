package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.TextStyle
import com.aritr.zinely.core.model.Transform
import androidx.compose.foundation.layout.size
import androidx.compose.ui.test.performClick
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.aritr.zinely.ui.golden.cropToBounds
import com.aritr.zinely.ui.golden.pixelCountOf
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.zinelyV21LightColors
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The golden net for [BenchPageNav] and [BenchPageGridSurface] — the frozen `.navrow` / `.filmstrip` /
 * `.pthumb` / `.pgrid` (ADR-095 rows 5.1–5.8, 5.11–5.15), light + dark.
 *
 * **This is where C5's painted properties are asserted, and the raster alone does not assert them.**
 * Roborazzi compares at `changeThreshold = 0.02f`: a 1px hairline across the row, or a 2px spine on a
 * 26dp sheet, moves far less than 2 % of the frame and would survive its own deletion — the lesson C3 and
 * C4 both had to learn the hard way, recorded in ADR-094 §6.13. So the properties small enough to hide
 * under the threshold are counted in pixels here, and the recorded image keeps its real job: catching
 * what nobody thought to assert.
 *
 * The sharpest of those counts is [OD-22]'s: a sheet holding a page with content must differ from a sheet
 * holding an empty one. That single comparison is the difference between the ruling as implemented and the
 * abstraction it replaced — placeholder rules would paint every sheet identically, and pass any golden
 * recorded from them.
 *
 * **What these frames are not.** All but [bench_page_grid_open_light] host the composables *directly*,
 * with hand-built pages and a plain `Box` around them. That is deliberate — it is the only way to raster a
 * 56dp row at a size where a 1px hairline is countable — but it means they bypass the shipped path
 * entirely: `EditorScreen`'s colour scoping, its `BoxWithConstraints`, and the room palette the nav row
 * actually stands in are all absent. A property that is correct here and wrong in the app would go
 * unseen by every test in this file except the screen-level frame, which exists for exactly that reason.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class BenchC5GoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val HOST_TAG = "benchPageNavGoldenHost"

        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }

    private val pageSizePt = PtSize(100.0, 130.0)

    private var deskArgb = 0
    private var chromeArgb = 0
    // P3's own yardsticks, kept beside the V2 ones rather than replacing them: this class also records
    // goldens for surfaces P3 does not touch, and repointing a shared field turned four unrelated
    // assertions red in C4 before this rule was learned.
    private var v21DeskArgb = 0
    private var v21BerryArgb = 0
    private var v21InkSoftArgb = 0
    // P5's: the `.pgrid` panel's ground and its title's ink, both the ROOM's — the panel is chrome and
    // keeps the room, and only the cards are lit.
    private var v21PaperArgb = 0
    private var v21InkArgb = 0
    private var matchaArgb = 0
    private var strawberryArgb = 0
    private var deskEdgeArgb = 0
    private var inkSoftArgb = 0
    private var inkArgb = 0

    /**
     * Six sheets: cover, an interior carrying a large block of text, three empty interiors, back.
     *
     * Page 2's text is what makes row 5.8 assertable — it is the only difference between that sheet and
     * its empty neighbours, so any pixel difference between them can only be the page being drawn.
     */
    private fun pages(): List<Page> = (0 until 6).map { i ->
        Page(
            index = i,
            // Every page is INTERIOR, exactly as the product builds them (`EditorBootstrap.kt:26`,
            // `RoomProjectRepository.kt:475`). These fixtures used to fabricate cover roles, which is
            // why the suite proved three frozen rows that never fired on a real document until Device
            // Pass 1 found them dead. Covers are a matter of POSITION now, per the freeze.
            role = PageRole.INTERIOR,
            elements = if (i == 1) {
                listOf(
                    TextElement(
                        id = "t",
                        transform = Transform(8.0, 8.0, 84.0, 110.0),
                        text = "zine zine zine zine zine zine",
                        style = TextStyle(sizePt = 18.0, color = ColorRgba(0x11, 0x11, 0x11)),
                    ),
                )
            } else {
                emptyList()
            },
        )
    }

    private fun host(darkTheme: Boolean, content: @Composable () -> Unit) {
        composeRule.setContent {
            ZinelyTheme(darkTheme = darkTheme) {
                // The V2 desk, which is what the grid paints — not `colors.desk`, the V1 token the host
                // box happens to stand on. They are different colours and the probe must read the one
                // under test.
                deskArgb = ZinelyTheme.v2Colors.desk.toArgb()
                chromeArgb = ZinelyTheme.v2Colors.chrome.toArgb()
                v21DeskArgb = ZinelyTheme.v21Colors.desk.toArgb()
                v21BerryArgb = ZinelyTheme.v21Colors.berry.toArgb()
                v21InkSoftArgb = ZinelyTheme.v21Colors.inkSoft.toArgb()
                v21PaperArgb = ZinelyTheme.v21Colors.paper.toArgb()
                v21InkArgb = ZinelyTheme.v21Colors.ink.toArgb()
                matchaArgb = ZinelyTheme.v2Colors.matcha.toArgb()
                strawberryArgb = ZinelyTheme.v2Colors.strawberry.toArgb()
                deskEdgeArgb = ZinelyTheme.v2Colors.deskEdge.toArgb()
                inkSoftArgb = ZinelyTheme.v2Colors.inkSoft.toArgb()
                inkArgb = ZinelyTheme.v2Colors.ink.toArgb()
                Box(
                    Modifier
                        .testTag(HOST_TAG)
                        .fillMaxWidth()
                        .background(ZinelyTheme.v2Colors.desk),
                ) { content() }
            }
        }
        composeRule.waitForIdle()
    }

    /**
     * Unmerged, always: the miniature inside a thumb is merged away by the thumb's own `selectable`, so
     * the merged tree cannot see the node this test crops to.
     */
    private fun crop(tag: String, full: Bitmap): Bitmap = cropToBounds(
        full,
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot,
    )

    /**
     * The drawn thickness, in px, of a border of [argb] on this bitmap's left edge — counted as the run of
     * that colour running inward at mid-height, where the 6dp corner radius is not in the way.
     *
     * A *count* of border-coloured pixels cannot tell 1px from 2px without knowing the density; a run
     * length can, and it is the property row 5.13 actually freezes.
     */
    private fun Bitmap.leftBorderThickness(argb: Int): Int {
        val y = height / 2
        var x = 0
        while (x < width && getPixel(x, y) != argb) x++
        var n = 0
        while (x < width && getPixel(x, y) == argb) { x++; n++ }
        return n
    }

    /** How many pixels of [a] differ from the pixel at the same place in [b]. Both must be the same size. */
    private fun differingPixels(a: Bitmap, b: Bitmap): Int {
        var n = 0
        for (y in 0 until minOf(a.height, b.height)) {
            for (x in 0 until minOf(a.width, b.width)) {
                if (a.getPixel(x, y) != b.getPixel(x, y)) n++
            }
        }
        return n
    }

    private fun captureNav(name: String, darkTheme: Boolean, currentPageIndex: Int = 1) {
        host(darkTheme) {
            BenchPageNav(
                pages = pages(),
                currentPageIndex = currentPageIndex,
                pageSizePt = pageSizePt,
                defaults = DocumentDefaults(),
                onSelectPage = {},
                onOpenGrid = {},
            )
        }
        // Non-vacuity: the row's own node must be present, so a blanked re-record fails here rather than
        // passing on a count of desk pixels.
        composeRule.onNodeWithTag(BenchNavRowTestTag).assertExists()
        val full = composeRule.activity.window.decorView.rasterizeToBitmap()
        val row = crop(BenchNavRowTestTag, full)

        // P3 — the row stands on `--desk`: it *is* the desk, where V2 gave it a distinct `--chrome`.
        assertTrue("the navigation row did not paint its --desk ground ($name)",
            row.pixelCountOf(v21DeskArgb) > 500)

        // P3 — **and no top hairline.** V2's `.navrow` carried `border-top:1px solid var(--chrome-line)`;
        // V2.1's (`v21-bench.html:328`) declares only a background. Inverted rather than deleted, for the
        // reason C4's twin records: the room and the chrome merging into one continuous surface is a
        // decision, and a deleted assertion would let a rule reappear unnoticed. Sampled against the ground
        // **as actually painted** from the row's middle, because a re-cropped bitmap's top edge can land on
        // the desk and "differ" whether or not a hairline exists (the two shapes that failed to bite in C3).
        val rect = composeRule.onNodeWithTag(BenchNavRowTestTag).fetchSemanticsNode().boundsInRoot
        val left = rect.left.toInt() + 2
        val right = rect.right.toInt() - 2
        val ground = full.getPixel(left, (rect.top + rect.height / 2f).toInt())
        var hairline = 0
        for (x in left until right) if (full.getPixel(x, rect.top.toInt() + 1) != ground) hairline++
        assertTrue("the row drew a top hairline ($name): $hairline/${right - left} px on its first " +
            "scanline differ from its own ground, and V2.1's `.navrow` has no border", hairline == 0)

        // P3 — the current page's whole signal: a 3dp `--berry` ring, and **the only berry in the row**.
        // Asserted here because it replaced a scale, a lift, a raised shadow and a strawberry dot, all of
        // which BenchC5Test measured geometrically; if the ring were missing, that suite would now pass in
        // silence, since "nothing moves" is exactly what it asserts.
        assertTrue("the current sheet is not ringed in --berry ($name)", row.pixelCountOf(v21BerryArgb) > 60)

        // Row 5.2a — the frozen `.gridbtn svg{width:17px; height:17px; stroke-width:1.8}` (`:278`).
        // Neither number was asserted anywhere: the button's own 34dp box is measured in [BenchC5Test],
        // and the glyph inside it could have been any size or weight. Two readings, because they fail
        // differently — the ink's bounding box is the *size*, and how much ink fills it is the *stroke*.
        //
        // Both expectations are computed from the frozen file rather than from the production constants:
        // the four `<rect>`s span `x 3..21`, `y 3..22` of a 24-unit viewBox (`v2-bench.html:483`), the
        // stroke straddles the path by half its width each side, and the whole thing is scaled to 17px.
        val button = crop(BenchGridButtonTestTag, full)
        // Counted as pixels of the glyph's own tint, not as "anything that is not the ground": the button
        // carries a 1.5dp `ink` border on an 8dp radius plus a 2dp printed shadow, and their antialiased
        // edges reach far enough inside any fixed inset to pin the bounding box to the button's own edges.
        // The first cut measured exactly that and reported the glyph as 62px wide — the button, not the
        // glyph. ⚠ P3: the tint is now **V2.1's** `inkSoft`, a different value; comparing against V2's
        // matched nothing and the bounding box came back inverted, reporting a width of −76px. A negative
        // width is the probe saying it found no ink at all, and it is worth reading as that rather than as
        // a glyph that shrank.
        var minX = button.width; var maxX = -1; var minY = button.height; var maxY = -1; var ink = 0
        for (y in 0 until button.height) for (x in 0 until button.width) {
            if (button.getPixel(x, y) == v21InkSoftArgb) {
                ink++
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
        val unit = with(composeRule.density) { 17.dp.toPx() } / 24f
        assertEquals(
            "the grid glyph's ink is ${maxX - minX + 1}px wide ($name); the freeze draws rects spanning " +
                "18 units of a 24-unit viewBox at 17px, plus a 1.8 stroke straddling them [ink=$ink]",
            (18f + 1.8f) * unit,
            (maxX - minX + 1).toFloat(),
            2.5f,
        )
        assertEquals(
            "the grid glyph's ink is ${maxY - minY + 1}px tall ($name)",
            (19f + 1.8f) * unit,
            (maxY - minY + 1).toFloat(),
            2.5f,
        )
        // The stroke's WEIGHT, as ink mass — the half the bounding box cannot see, since a heavier stroke
        // on the same rects grows the box by a fraction of a pixel and fills it far more.
        //
        // 4 rects of 7×8 units = 120 units of path at 1.8 wide, scaled by the same unit. Counting only
        // *exact* tint erodes each stroke by its antialiased rim — about 0.9px of a 2.55px stroke here —
        // so the geometric area is scaled by a measured 0.65 rather than pretended to be exact. The band
        // is ±20 % of that, which the two mutations this exists for clear easily: `stroke 1.8 → 2.6`
        // lands near +68 % (the rim is a fixed cost on a thicker core) and `17 → 20` near +38 %.
        val expectedInk = 120f * 1.8f * unit * unit * 0.65f
        assertTrue(
            "the grid glyph carries ${ink}px of ink ($name) against ~${expectedInk.toInt()}px for the " +
                "frozen 17px/1.8 geometry — the stroke weight is not the frozen one",
            ink > expectedInk * 0.8f && ink < expectedInk * 1.2f,
        )

        // ⚠ **P3 removed every `--matcha` mark this section used to assert**, and the replacements are not
        // one-for-one, so the whole block is rewritten rather than renumbered:
        //
        // - the **cover spine** is gone. V2 gave the first and last sheets a 2dp `--matcha` edge so they
        //   read as bound; V2.1's `.fpage` declares no spine at all, and the cover distinction now lives
        //   only where it is spoken, in `benchPageLabel`. There is nothing left to see, so nothing to probe.
        // - the **current sheet's border** is gone. It is a uniform 1.5dp `ink` on every sheet now, and the
        //   state moved out to the `--berry` ring asserted above.
        //
        // What survives is the *shape* of the old assertions, which was their real value: the current sheet
        // must differ from its neighbours, and it must differ **only** where the freeze says. So the ring is
        // counted on the current sheet and denied on an interior one — the same current-vs-neighbour pairing
        // that caught a build which stopped ringing the current sheet, applied to the mark that replaced it.
        // ⚠ Counted over the sheet's bounds **inflated by the ring**, not over the sheet's own crop. The
        // ring is `box-shadow:0 0 0 3px`, which draws entirely *outside* the element — cropping to the node
        // caught only its antialiased inner edge and read 32px against a threshold of 40, which is the probe
        // missing the mark rather than the mark missing. The row-wide count above saw it the whole time.
        val ring = with(composeRule.density) { 3.dp.toPx() }.toInt() + 1
        fun ringPixelsAround(tag: String): Int {
            val r = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            var n = 0
            for (y in (r.top.toInt() - ring).coerceAtLeast(0)..(r.bottom.toInt() + ring).coerceAtMost(full.height - 1)) {
                for (x in (r.left.toInt() - ring).coerceAtLeast(0)..(r.right.toInt() + ring).coerceAtMost(full.width - 1)) {
                    if (full.getPixel(x, y) == v21BerryArgb) n++
                }
            }
            return n
        }
        assertTrue(
            "the current sheet is not ringed in --berry ($name) — only ${ringPixelsAround(benchThumbTag(2))}px",
            ringPixelsAround(benchThumbTag(2)) > 40,
        )
        assertTrue(
            "an interior sheet drew the current-page ring ($name) — ${ringPixelsAround(benchThumbTag(4))}px",
            // Sheet 4, not 3: sheet 3 abuts the current sheet, and its inflated box overlaps the current
            // sheet's ring by design. Choosing a neighbour that cannot overlap is the difference between
            // asserting "only the current sheet is ringed" and asserting the strip's gap arithmetic.
            ringPixelsAround(benchThumbTag(4)) < 10,
        )
        val interior = crop(benchThumbTag(3), full)
        // The covers are now indistinguishable from interiors in paint. Asserted, because "they look the
        // same" is the deliberate outcome and a spine creeping back would otherwise pass unnoticed.
        assertTrue(
            "the front cover still draws a spine ($name)",
            crop(benchThumbTag(1), full).pixelCountOf(matchaArgb) < 10,
        )
        assertTrue(
            "the back cover still draws a spine ($name)",
            crop(benchThumbTag(6), full).pixelCountOf(matchaArgb) < 10,
        )

        // Row 5.4d / OD-23 — **the sheet's ground is the PAGE's paper, in both themes.**
        //
        // This is D-059's assertion, and it is the one the whole suite was missing: until it existed, the
        // dark strip drew the user's own words at 1.21:1 against their own sheet and every test, probe and
        // golden passed. Read from a sheet's interior, away from its 1px border and its 2px spine, and
        // compared against the LIGHT theme's `--paper` literal — which in dark is the whole point, and in
        // light is the unchanged-byte-for-byte half of the amendment.
        val island = crop(benchThumbTag(4), full)
        val interiorPixel = island.getPixel(island.width / 2, island.height / 2)
        // ⚠ The literal moved from V2's `#F7F2E7` to **V2.1's `#FFF6E8`**, and the move is the finding, not
        // a re-baseline. This assertion held the strip to the V2 light paper while `benchGridCardIsland`
        // had already gone to V2.1's — so it was actively *defending* one screen drawing the same eight
        // pages on two different papers, which is the OD-47 defect P5 exists to close. A review found it by
        // comparing the two islands; no probe could, because each was self-consistent. The assertion is
        // still against the LIGHT literal in both themes, which is the half of D-059 that has not changed.
        assertEquals(
            "the sheet's interior is #%06X ($name) — the island is V2.1's `--paper:#FFF6E8`, the same paper " +
                "the page grid's cards use; a sheet that paints the room's paper loses the page in dark theme"
                .format(interiorPixel and 0xFFFFFF),
            zinelyV21LightColors().paper.toArgb(),
            interiorPixel,
        )
        // …and the row it stands on did NOT get lightened with it: the island is the sheet, not the strip.
        // Without this, "make the dark thumb readable" is satisfied by lightening the whole navigation row,
        // which is the mistake C1 made once with the page's shadow and review caught (D-010).
        assertEquals(
            "the navigation row's own ground moved with the sheet's ($name)",
            // P3: the row's ground is V2.1's `desk`. The guard is unchanged in substance — the island must
            // stay the sheet's, not the strip's — only the colour it is measured against moved.
            v21DeskArgb,
            full.getPixel(rect.left.toInt() + 4, (rect.top + rect.height / 2f).toInt()),
        )

        // ⚠ **P3 deleted the spine**, so this asserts its absence. V2's `.pthumb::before` laid 2px of
        // `--desk-edge` down the left of *every* sheet; V2.1's `.fpage` declares no `::before` at all, and
        // the sheet is bordered uniformly in `ink`.
        //
        // Inverted rather than deleted, and this one earns it: the original assertion exists because a build
        // that drew **no spine at all** passed everything around it — the matcha probes only ever looked at
        // covers, and the interior probe only asserted matcha's *absence*. That blind spot is exactly what
        // an inverted assertion re-opens if it is thrown away instead of turned around.
        val spine = interior.leftBorderThickness(deskEdgeArgb)
        assertTrue("the interior sheet still draws a --desk-edge spine ($name): ${spine}px", spine == 0)

        // ⚠ **P3 made the radius uniform**, so this now asserts the two corners match.
        //
        // V2's `.pthumb` was `1.5px 3px 3px 1.5px` — squarer on the spine side, because that edge was the
        // sheet's spine — and this test measured the difference, since a uniform radius is invisible to
        // every size assertion. V2.1's `.fpage` is `var(--br-xs)` on all four, of a piece with dropping the
        // spine: the strip is no longer a row of little bound sheets.
        //
        // The measurement is unchanged and only the comparison is inverted, which keeps its original virtue
        // — it reads the drawn corners rather than the shape constant, so it still fails if someone restores
        // an asymmetric shape without touching the sizes.
        val sheet = crop(benchThumbTag(3), full)
        val groundInCorner = { x0: Int, y0: Int ->
            var n = 0
            for (y in y0 until y0 + 6) for (x in x0 until x0 + 6) {
                if (sheet.getPixel(x, y) == v21DeskArgb) n++
            }
            n
        }
        val leftCut = groundInCorner(0, 0)
        val rightCut = groundInCorner(sheet.width - 6, 0)
        assertTrue(
            "the sheet's corners are not equally round ($name): the leading corner loses ${leftCut}px to " +
                "the row's ground and the trailing corner ${rightCut}px — V2.1's radius is a uniform --br-xs",
            kotlin.math.abs(rightCut - leftCut) <= 2,
        )

        // ⚠ **P3 removed the strawberry dot.** V2's `.cur::after` hung a 4dp disc 7dp above the current
        // sheet — the one strawberry mark in the whole Bench. V2.1's `.fpage.on` is the `berry` ring and
        // nothing else, so the current page is now said once instead of three times (dot, border, lift).
        //
        // Inverted, not deleted, and the count is the same one: **zero** strawberry pixels in the row. A
        // conversion that kept the dot alongside the new ring would double the signal and pass every other
        // assertion here, since each of them checks for the presence of its own mark.
        assertTrue("the current sheet still draws the strawberry dot ($name): " +
            "${row.pixelCountOf(strawberryArgb)}px", row.pixelCountOf(strawberryArgb) == 0)

        // Row 5.8 / OD-22 — the sheet's interior is the REAL PAGE. Page 2 carries text and pages 3 and 4
        // are empty; if the interiors were the frozen placeholder (or any other page-independent paint)
        // these two crops would be pixel-identical. They are not, and that difference IS the ruling.
        //
        // Compared against a *pair of empty* sheets first, so the number below is calibrated against this
        // renderer's own noise rather than an invented constant: 3 and 4 differ only by scale-free
        // position, so whatever they differ by is the floor.
        val withText = crop(benchThumbPageTag(2), full)
        val emptyA = crop(benchThumbPageTag(3), full)
        val emptyB = crop(benchThumbPageTag(4), full)
        val noise = differingPixels(emptyA, emptyB)
        val signal = differingPixels(withText, emptyA)
        assertTrue(
            "the sheet does not draw its page ($name): a page with text differs from an empty one by " +
                "$signal px, while two empty ones differ by $noise",
            signal > noise + 20,
        )

        cropToBounds(full, composeRule.onNodeWithTag(HOST_TAG).fetchSemanticsNode().boundsInRoot)
            .captureRoboImage("$GOLDEN_DIR/$name.png", aa())
    }

    @Test
    fun bench_page_nav_light() = captureNav("bench_page_nav_light", darkTheme = false)

    @Test
    fun bench_page_nav_dark() = captureNav("bench_page_nav_dark", darkTheme = true)

    private fun captureGrid(name: String, darkTheme: Boolean) {
        host(darkTheme) {
            BenchPageGridSurface(
                pages = pages(),
                currentPageIndex = 2,
                pageSizePt = pageSizePt,
                defaults = DocumentDefaults(),
                onSelectPage = {},
                onDismiss = {},
            )
        }
        composeRule.onNodeWithTag(BenchPageGridTestTag).assertExists()
        val full = composeRule.activity.window.decorView.rasterizeToBitmap()
        val grid = crop(BenchPageGridTestTag, full)

        // Row 5.11, **inverted by P5** — the panel's ground is `--paper` and no longer `--desk`
        // (`v21-bench.html:444`). V2's `.pgrid` was an opaque desk-coloured overlay; V2.1's is a paper
        // sheet, and the panel *keeps the room* while the cards below are lit. Both halves asserted: a
        // conversion that repainted the panel and left the desk showing anywhere would pass a
        // presence-only check.
        assertTrue(
            "the panel did not paint its --paper ground ($name)",
            grid.pixelCountOf(v21PaperArgb) > 5000,
        )
        // `onLeaf` intentionally equals the dark desk ink, so a whole-panel zero-count assertion became
        // invalid once the current page number moved to accessible dark-on-bright ink. The positive
        // surface count above and the bounded lit-card checks below pin the actual grounds.

        // **OD-47, in pixels, and this is the assertion the whole amendment comes down to.** The card
        // paints the LIGHT `--paper` in *both* themes (`.pgc`'s six restatements, `:456-457`). The
        // expected colour is read from `zinelyV21LightColors()` rather than from the installed theme, so
        // in the dark frame the two differ and a card that followed the room fails here — which is what
        // "one screen rendering the same eight pages two ways" meant.
        val litPaper = zinelyV21LightColors().paper.toArgb()
        val ordinaryCell = crop(benchPageCellTag(2), full)
        assertTrue(
            "the card is not lit ($name): ${ordinaryCell.pixelCountOf(litPaper)}px of the light --paper " +
                "in a ${ordinaryCell.width}×${ordinaryCell.height} card",
            ordinaryCell.pixelCountOf(litPaper) > ordinaryCell.width * ordinaryCell.height / 2,
        )

        // A18: the current page uses the same berry OUTER ring as the strip. It must not tint the page:
        // selection chrome belongs outside the artifact, where it cannot alter the maker's work.
        val currentCell = crop(benchPageCellTag(3), full)
        val currentBounds = composeRule.onNodeWithTag(benchPageCellTag(3)).fetchSemanticsNode().boundsInRoot
        val currentWithRing = cropToBounds(full, currentBounds.inflate(8f))
        assertTrue(
            "the current card has no berry outer ring ($name)",
            currentWithRing.pixelCountOf(v21BerryArgb) > 60,
        )
        assertTrue(
            "the current card still wears a --matcha border ($name)",
            currentCell.leftBorderThickness(matchaArgb) == 0,
        )

        // D-065's root regression: the larger chooser must preserve page identity. Page 2 contains text
        // and page 3 is empty, so their miniature rasters must differ even after excluding the badges.
        val contentPage = crop(benchPageGridPageTag(2), full)
        val emptyPage = crop(benchPageGridPageTag(3), full)
        assertTrue(
            "the grid replaced both live pages with the same blank stand-in ($name)",
            differingPixels(contentPage, emptyPage) > 100,
        )

        // Row 5.15 — the header's title is set in the voice (`.sheet h3`, `v21-bench.html:386`), while the
        // `.dclose` beside it draws no text at all. Nothing else can see `fontFamily`: it reaches no
        // semantics property, so the raster is the only instrument.
        //
        // Read as the title's ink: its bounding box and how much of the box is filled.
        var tMinX = Int.MAX_VALUE; var tMaxX = Int.MIN_VALUE
        var tMinY = Int.MAX_VALUE; var tMaxY = Int.MIN_VALUE
        var titleInk = 0
        val headerBand = (grid.height * 0.16f).toInt()
        for (y in 0 until headerBand) for (x in 0 until grid.width) {
            if (grid.getPixel(x, y) == v21InkArgb) {
                titleInk++
                if (x < tMinX) tMinX = x; if (x > tMaxX) tMaxX = x
                if (y < tMinY) tMinY = y; if (y > tMaxY) tMaxY = y
            }
        }
        assertTrue("no --ink found in the grid header at all ($name)", titleInk > 0)
        val titleW = (tMaxX - tMinX + 1).toFloat()
        assertTrue("the grid header's title is ${titleW}px wide ($name) — it did not render", titleW > 100f)
        // ⏳ **The stem-weight band is OWED, and is deliberately absent rather than guessed.** V2's version
        // of this assertion pinned the title's ink between 1146 and 1458 px — a band measured from two
        // real rasters, the serif voice at 17px/500 (1302px) against the sans work face (1620px), and it
        // is what made "the title is in the voice" a real claim rather than a hope. P5 changes the size
        // and the weight (17sp/500 → 19.2sp/700, `.sheet h3`), so BOTH numbers are invalid, and P5 may not
        // run the raster: a band derived by arithmetic from the old one would be a fabricated measurement
        // of the exact kind D-006 and D-007 refused. Re-measure it when the goldens for this surface are
        // re-recorded, and restore the assertion then. Until it is restored, the frozen face on this
        // surface is asserted by nothing but the recorded image.

        grid.captureRoboImage("$GOLDEN_DIR/$name.png", aa())
    }

    @Test
    fun bench_page_grid_light() = captureGrid("bench_page_grid_light", darkTheme = false)

    @Test
    fun bench_page_grid_dark() = captureGrid("bench_page_grid_dark", darkTheme = true)

    /**
     * The summoned grid **in the screen it actually lives in** — the frame that shows what the two
     * component goldens above structurally cannot.
     *
     * The frozen `.pgrid` is a bottom sheet over a scrim (`v21-bench.html:444-448`, `openGrid()` at
     * `:783`), and the component goldens above render the panel alone — they cannot show the scrim, the
     * page still visible above it, or where the panel's floor lands. C5's first cut hosted this in a
     * full-screen `Dialog` and every component golden passed, because a component golden renders the
     * component and not its placement; independent review caught it by reading the HTML.
     *
     * ⚠ **This frame is also the picture of P5's recorded deviation.** The frozen panel is a child of
     * `.phone` (markup `:568`) and covers the navigation row and the bar; the host still mounts it inside
     * `.canvasArea`, so in this frame it stops at the navigation row and the scrim stops with it.
     * Re-homing the call site is `EditorScreen.kt`'s, which P5 does not own — and until it happens, this
     * image is what the disagreement looks like.
     */
    @Test
    fun bench_page_grid_open_light() {
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        val store = EditorStore(
            EditorModel(
                document = ZineDocument(
                    format = ZineFormat.SINGLE_SHEET_8,
                    paperSize = PaperSize.LETTER,
                    pages = pages(),
                ),
            ),
            CoroutineScope(Dispatchers.Unconfined), Dispatchers.Unconfined, runner,
        )
        composeRule.setContent {
            ZinelyTheme(darkTheme = false) {
                deskArgb = ZinelyTheme.v2Colors.desk.toArgb()
                Box(Modifier.testTag(HOST_TAG).background(ZinelyTheme.v2Colors.desk)) {
                    EditorScreen(
                        store = store,
                        pageSizePt = pageSizePt,
                        modifier = Modifier.size(360.dp, 720.dp),
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchGridButtonTestTag).performClick()
        composeRule.waitForIdle()

        val full = composeRule.activity.window.decorView.rasterizeToBitmap()
        val grid = composeRule.onNodeWithTag(BenchPageGridTestTag).fetchSemanticsNode().boundsInRoot
        val nav = composeRule.onNodeWithTag(BenchNavRowTestTag).fetchSemanticsNode().boundsInRoot
        val scrim = composeRule.onNodeWithTag(BenchPageGridScrimTestTag).fetchSemanticsNode().boundsInRoot
        // ⚠ **Inverted with the re-home.** The frozen `.pgrid` is a child of `.phone` (`v21-bench.html:585`;
        // `.canvasArea` closes at `:530`), so the sheet rises from the screen's bottom edge and the
        // filmstrip and bar go under it. This line used to demand the opposite, because the host mounted
        // the overlay inside the canvas — where V2's `inset:0` correctly put it and V2.1's rule does not.
        assertTrue(
            "the grid (bottom ${grid.bottom}) stops above the navigation row (top ${nav.top}) — it is " +
                "scoped to the canvas again",
            grid.bottom > nav.top,
        )
        // The frame's other half, and the one the panel shape is about: the page is still visible above
        // the sheet, behind the scrim, rather than replaced by it.
        assertTrue(
            "the panel (top ${grid.top}) reaches the top of the scrim (${scrim.top}) — it is an overlay " +
                "again, not the frozen bottom sheet",
            grid.top > scrim.top + 1f,
        )
        cropToBounds(full, composeRule.onNodeWithTag(HOST_TAG).fetchSemanticsNode().boundsInRoot)
            .captureRoboImage("$GOLDEN_DIR/bench_page_grid_open_light.png", aa())
    }
}
