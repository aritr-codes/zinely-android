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
import com.aritr.zinely.ui.theme.zinelyV2LightColors
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

        // Row 5.1 — the strip stands on `--chrome`, not on the desk.
        assertTrue("the navigation row did not paint its --chrome ground ($name)",
            row.pixelCountOf(chromeArgb) > 500)

        // Row 5.3 — `border-top:1px solid var(--chrome-line)`. Compared against the ground **as actually
        // painted**, sampled from the row's middle: the token itself is composited and equals neither
        // side, and a re-cropped bitmap's rounded top edge can land on the desk and "differ" whether or
        // not a hairline was ever drawn (the two shapes of this assertion that failed to bite in C3).
        val rect = composeRule.onNodeWithTag(BenchNavRowTestTag).fetchSemanticsNode().boundsInRoot
        val left = rect.left.toInt() + 2
        val right = rect.right.toInt() - 2
        val ground = full.getPixel(left, (rect.top + rect.height / 2f).toInt())
        var hairline = 0
        for (x in left until right) if (full.getPixel(x, rect.top.toInt() + 1) != ground) hairline++
        assertTrue("the top hairline is missing ($name): only $hairline/${right - left} px on the row's " +
            "first scanline differ from its own ground", hairline > (right - left) / 2)

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
        // is a transparent 34dp box over the row's chrome carrying a 1px `--chrome-line` outline on a 9dp
        // radius, and the outline's antialiased arc reaches far enough inside any fixed inset to pin the
        // bounding box to the button's own edges. The first cut measured exactly that and reported the
        // glyph as 62px wide — the button, not the glyph.
        var minX = button.width; var maxX = -1; var minY = button.height; var maxY = -1; var ink = 0
        for (y in 0 until button.height) for (x in 0 until button.width) {
            if (button.getPixel(x, y) == inkSoftArgb) {
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

        // Rows 5.6-5.7 — the current sheet takes a `--matcha` border and the two covers a `--matcha`
        // spine, so matcha must be on the row. A 2px spine on a 34dp sheet is ~140px at xhdpi.
        assertTrue("no --matcha on the strip: neither the current border nor the cover spines painted " +
            "($name)", row.pixelCountOf(matchaArgb) > 60)

        // Row 5.5a — `.pthumb[data-cover]::before{background:var(--matcha)}` applies to **both** covers.
        // Counted per sheet, because the row-wide count above is satisfied by the current sheet's border
        // alone: the mutation that greened the back cover's spine survived it. Sheet 6 is the back cover
        // and is NOT the current sheet, so every matcha pixel inside its crop is its spine.
        val backCover = crop(benchThumbTag(6), full)
        assertTrue(
            "the back cover has no --matcha spine ($name) — only ${backCover.pixelCountOf(matchaArgb)}px",
            backCover.pixelCountOf(matchaArgb) > 40,
        )
        val frontCover = crop(benchThumbTag(1), full)
        assertTrue(
            "the front cover has no --matcha spine ($name)",
            frontCover.pixelCountOf(matchaArgb) > 40,
        )
        // …and an interior sheet has none, so the assertion above cannot pass by painting every spine.
        val interior = crop(benchThumbTag(3), full)
        assertTrue(
            "an interior sheet drew a cover spine ($name)",
            interior.pixelCountOf(matchaArgb) < 10,
        )

        // Row 5.6a — the CURRENT sheet's border is `--matcha` (`.pthumb.cur{border-color:var(--matcha)}`,
        // `:288`). The row-wide matcha count above does NOT cover this: the two cover spines satisfy it on
        // their own, which is exactly how a build that stopped ringing the current sheet would have passed.
        // Sheet 2 is the current sheet here (`currentPageIndex = 1`) and is **not** a cover, so every matcha
        // pixel in its crop is its border — and sheet 3, asserted above to carry fewer than 10, is the
        // counter-case that stops this from being satisfied by painting every sheet's border matcha.
        val currentSheet = crop(benchThumbTag(2), full)
        assertTrue(
            "the current sheet has no --matcha border ($name) — only " +
                "${currentSheet.pixelCountOf(matchaArgb)}px, against an interior sheet's " +
                "${interior.pixelCountOf(matchaArgb)}px",
            currentSheet.pixelCountOf(matchaArgb) > 40,
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
        assertEquals(
            "the sheet's interior is #%06X ($name) — the frozen island is `--paper:#F7F2E7` (`:282`), and " +
                "a sheet that paints the room's paper instead loses the page in dark theme"
                .format(interiorPixel and 0xFFFFFF),
            zinelyV2LightColors().paper.toArgb(),
            interiorPixel,
        )
        // …and the row it stands on did NOT get lightened with it: the island is the sheet, not the strip.
        // Without this, "make the dark thumb readable" is satisfied by lightening the whole navigation row,
        // which is the mistake C1 made once with the page's shadow and review caught (D-010).
        assertEquals(
            "the navigation row's own ground moved with the sheet's ($name)",
            chromeArgb,
            full.getPixel(rect.left.toInt() + 4, (rect.top + rect.height / 2f).toInt()),
        )

        // Row 5.5 — the spine ITSELF: `.pthumb::before` is 2px of `--desk-edge` on every ordinary sheet
        // (`v2-bench.html:284`), and only the *cover* half of that rule was asserted. A build that drew no
        // spine at all passed everything above, because the matcha probes only ever look at the covers and
        // the interior probe only asserts matcha's *absence*. Run length inward from the left edge: the
        // 1px `--paper-edge` border sits on top of the spine's first column, so the scan skips it and
        // counts what is left of the frozen 2px.
        val spine = interior.leftBorderThickness(deskEdgeArgb)
        assertTrue("the interior sheet has no --desk-edge spine ($name)", spine >= 1)

        // Row 5.4b — the frozen radius is ASYMMETRIC: `1.5px 3px 3px 1.5px`, squarer on the spine side
        // because that edge is the sheet's spine. A uniform radius is the mutation, and it is invisible to
        // every size assertion — the box is 26×34 either way. Measured as *fill*: in a 6×6 corner box the
        // squarer left corner keeps more of its paper than the rounder right one, and under a uniform
        // radius the two counts converge.
        val sheet = crop(benchThumbTag(3), full)
        val chromeInCorner = { x0: Int, y0: Int ->
            var n = 0
            for (y in y0 until y0 + 6) for (x in x0 until x0 + 6) {
                if (sheet.getPixel(x, y) == chromeArgb) n++
            }
            n
        }
        val leftCut = chromeInCorner(0, 0)
        val rightCut = chromeInCorner(sheet.width - 6, 0)
        assertTrue(
            "the sheet's corners are equally round ($name): the spine-side corner loses ${leftCut}px to " +
                "the row's ground and the outer corner ${rightCut}px — the frozen radius is 1.5/3/3/1.5",
            rightCut > leftCut,
        )

        // Row 5.6 — the frozen `.cur::after`, the one strawberry dot in the whole Bench. A 4dp disc at
        // xhdpi is ~50px before its antialiased rim is discounted, so 8 exact-colour pixels is a floor
        // that only a missing (or mispositioned-into-clipping) dot can fall below.
        assertTrue("the current sheet has no strawberry dot ($name)",
            row.pixelCountOf(strawberryArgb) > 8)

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
                onSelectPage = {},
                onDismiss = {},
            )
        }
        composeRule.onNodeWithTag(BenchPageGridTestTag).assertExists()
        val full = composeRule.activity.window.decorView.rasterizeToBitmap()
        val grid = crop(BenchPageGridTestTag, full)

        // Row 5.11 — the grid fills its box in `--desk`, which is what makes it an overlay rather than a
        // panel with a gap around it. WHICH box it fills is asserted at the screen, below.
        assertTrue("the grid did not paint its --desk ground ($name)", grid.pixelCountOf(deskArgb) > 5000)

        // Row 5.13 — the current cell's border is `--matcha` **and 2px wide**, against every other cell's
        // 1px. A perimeter-relative *count* was the first attempt and it did not survive its own mutation:
        // at this density a 1px border still cleared the ratio floor, so `BenchCellBorderCurrent = 1.dp`
        // passed. Measured now as a run length, which is the width itself rather than a proxy for it.
        val cell = crop(benchPageCellTag(3), full)
        val perimeter = 2 * (cell.width + cell.height)
        // The frozen 2px and 1px are quoted as literals, never as the production constants: an assertion
        // written against `BenchCellBorderCurrent` moves with the mutation and can only ever pass.
        val thickness = cell.leftBorderThickness(matchaArgb)
        val expected = with(composeRule.density) { 2.dp.toPx() }
        val ordinary = with(composeRule.density) { 1.dp.toPx() }
        assertTrue(
            "the current cell's --matcha border is ${thickness}px thick ($name), and the freeze draws " +
                "2px = ${expected}px at this density",
            thickness >= expected - 1f && thickness <= expected + 1f,
        )
        assertTrue(
            "the current cell's border (${thickness}px) is no thicker than the 1px ($ordinary px) every " +
                "other cell wears ($name)",
            thickness > ordinary,
        )
        // And no other cell carries it, so the ring above is the *current* marker rather than a border
        // every cell wears.
        assertTrue(
            "a cell that is not current drew the --matcha border ($name)",
            crop(benchPageCellTag(2), full).pixelCountOf(matchaArgb) < perimeter / 4,
        )

        // Row 5.15 — the header's title is set in `--serif` (`v2-bench.html:376`), the Bench's *voice*, while
        // `Done` beside it is the sans *work* face. Nothing asserted either: `fontFamily` reaches no
        // semantics property, so the only instrument that can see it is the raster.
        //
        // Read as the title's ink: its bounding box and how much of the box is filled. Counting only exact
        // `--ink` keeps `Done` out of the sample entirely, since that word is painted `--matcha-text`.
        // Both numbers move under a face change — a sans cut of the same string at the same 17px sets to a
        // different width AND a different stem weight — and neither moves under anything else this surface
        // does, because the string, the size and the weight are all asserted elsewhere.
        var tMinX = Int.MAX_VALUE; var tMaxX = Int.MIN_VALUE
        var tMinY = Int.MAX_VALUE; var tMaxY = Int.MIN_VALUE
        var titleInk = 0
        val headerBand = (grid.height * 0.12f).toInt()
        for (y in 0 until headerBand) for (x in 0 until grid.width) {
            if (grid.getPixel(x, y) == inkArgb) {
                titleInk++
                if (x < tMinX) tMinX = x; if (x > tMaxX) tMaxX = x
                if (y < tMinY) tMinY = y; if (y > tMaxY) tMaxY = y
            }
        }
        assertTrue("no --ink found in the grid header at all ($name)", titleInk > 0)
        val titleW = (tMaxX - tMinX + 1).toFloat()
        assertTrue("the grid header's title is ${titleW}px wide ($name) — it did not render", titleW > 100f)
        // The band is set from BOTH measurements rather than around one of them: the serif voice sets this
        // string in 1302px of ink and the sans work face in 1620px, so ±12 % of the serif reading
        // (1146…1458) admits the frozen face and excludes the mutation by a clear margin. The bounding box
        // is deliberately NOT asserted — the two faces differ by 315px vs 323px there, 2.5 %, which is
        // inside any honest tolerance. Stem weight is what separates a serif from a sans at 17px; width is
        // not, and asserting the measure that cannot discriminate is how a row gets a test that never bites.
        assertTrue(
            "the grid header's title carries ${titleInk}px of --ink ($name), outside the 1146…1458 band the " +
                "frozen `--serif` voice sets this string in — a sans cut of the same string at 17px/500 " +
                "measures ~1620px",
            titleInk in 1146..1458,
        )

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
     * The frozen `.pgrid` is `position:absolute; inset:0` on markup inside `.canvasArea`
     * (`v2-bench.html:374`, `:470`), so it covers the canvas and leaves the status strip above it and the
     * filmstrip and bar below it standing. C5's first cut hosted it in a full-screen `Dialog` instead —
     * and every component golden passed, because a component golden renders the component, not its
     * placement. Independent review caught it by reading the HTML. This frame is the picture of the fix,
     * and a Dialog would take the strip and bar out of it.
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
        assertTrue(
            "the grid (bottom ${grid.bottom}) covers the navigation row (top ${nav.top}) — it is not " +
                "scoped to the canvas",
            grid.bottom <= nav.top + 1f,
        )
        cropToBounds(full, composeRule.onNodeWithTag(HOST_TAG).fetchSemanticsNode().boundsInRoot)
            .captureRoboImage("$GOLDEN_DIR/bench_page_grid_open_light.png", aa())
    }
}
