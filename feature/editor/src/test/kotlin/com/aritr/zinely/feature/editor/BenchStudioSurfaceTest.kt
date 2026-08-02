package com.aritr.zinely.feature.editor

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.editor.LiveTransform
import com.aritr.zinely.core.imposition.Imposer
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.ui.theme.ZinelyV2Grain
import com.aritr.zinely.ui.theme.zinelyV2DarkColors
import com.aritr.zinely.ui.theme.ZinelyV2Dimens
import com.aritr.zinely.ui.theme.zinelyV2LightColors
import java.io.File
import com.aritr.zinely.ui.theme.ZinelyV2Colors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **C1 — the studio surface**, pinned to the DESIGN-FROZEN Bench.
 *
 * Every constant this package transcribes is re-derived from
 * [`v2-bench.html`](../../../../../../../../../docs/design/mockups/v2-bench.html) **at test time**
 * rather than compared against a second copy written here, following `ZinelyV2DimensTest`'s pattern and
 * [ADR-073](../../../../../../../../../docs/DECISIONS.md#adr-073)'s rule: compare against the declared
 * CSS, not against a rendering of it. A transcription test that hard-codes the frozen number on both
 * sides passes the day the design changes and the implementation does not, which is the one failure it
 * exists to catch.
 *
 * The keep-clear tests are the ones to read first. They are the reason
 * [D-033](../../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-033) existed, and they assert the
 * *join* between the two halves of its amendment: that the frozen `18.5px` and the engine's
 * `safeAreaInsetPt` are the same boundary expressed twice.
 */
class BenchStudioSurfaceTest {

    private companion object {
        /** A page big enough that the 17pt keep-clear margin is comfortably inside it. */
        val PAGE = PtSize(210.4725, 297.638)
        const val SCALE = 2.5f
    }

    private val benchCss: String = run {
        val f = File("../../docs/design/mockups/v2-bench.html")
        assertTrue("expected :feature:editor as the working directory — missing $f", f.isFile)
        f.readText().substringBefore("</style>")
    }

    /**
     * Every declaration block for [selector], joined — because a selector may legitimately carry more
     * than one, and `.page` now does.
     *
     * The first cut of this read only the *first* block, which was true of the frozen file until the
     * [D-035](../../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-035) amendment gave `.page` a
     * second one holding the light-theme island's tokens. Five assertions then failed looking for `width`
     * in a block that declares only custom properties — the test was reading CSS's shape wrong, and the
     * spec was right.
     */
    private fun rule(selector: String): String {
        val escaped = Regex.escape(selector)
        val blocks = Regex("""(?m)^\s*$escaped\s*\{([^}]*)}""").findAll(benchCss)
            .map { it.groupValues[1] }.toList()
        assertTrue("frozen Bench has no `$selector` rule", blocks.isNotEmpty())
        return blocks.joinToString(";")
    }

    private fun px(selector: String, property: String): Double {
        val m = Regex("""(?<![\w-])$property\s*:\s*(-?[\d.]+)px""").find(rule(selector))
        assertTrue("`$selector` declares no `$property` in px", m != null)
        return m!!.groupValues[1].toDouble()
    }

    /**
     * The sheet is a **light-theme island** ([D-035](../../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-035)):
     * `.page` re-declares the on-paper tokens, and each one restates the **light** theme's value.
     *
     * Both halves matter. That the tokens are re-declared is the amendment; that they equal `:root`'s
     * light values is what makes it an amendment rather than a new palette — the owner's ruling forbids
     * inventing a second one. Reading the light block out of the frozen file rather than transcribing
     * hex here means this fails if either side is edited alone.
     */
    @Test
    fun `the frozen page is a light-theme island, restating the light values and inventing no colour`() {
        val lightRoot = Regex("""(?ms)^\s*:root\{(.*?)}""").find(benchCss)!!.groupValues[1]
        val island = rule(".page")
        val declared = Regex("""(--[a-z-]+)\s*:\s*(#[0-9A-Fa-f]{3,8})""").findAll(island)
            .associate { it.groupValues[1] to it.groupValues[2].lowercase() }

        assertTrue(
            "the sheet must re-declare the on-paper tokens; found ${declared.keys}",
            declared.keys.containsAll(
                listOf("--paper", "--paper-edge", "--ink", "--ink-soft", "--ink-faint", "--matcha", "--strawberry-text"),
            ),
        )
        declared.forEach { (token, value) ->
            val light = Regex("""(?<![\w-])${Regex.escape(token)}\s*:\s*(#[0-9A-Fa-f]{3,8})""")
                .find(lightRoot)?.groupValues?.get(1)?.lowercase()
            assertEquals("`$token` on .page must restate the light theme's value, not invent one", light, value)
        }
    }

    /**
     * **The Compose island overrides exactly the tokens `.page` declares — no more.**
     *
     * This is the assertion whose absence let the first cut ship a defect. That cut provided
     * `zinelyV2LightColors()` wholesale, lightening all twenty-six tokens rather than the spec's eight,
     * and among the eighteen extras were `pageShadow` / `pageContact` — so in dark the sheet cast a
     * warm-brown shadow on a dark desk, reinstating [D-010](../../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-010)
     * inside the fix for [D-035](../../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-035). Every
     * other test stayed green, and the re-recorded dark golden certified it.
     *
     * Both directions are checked, because only the pair is a statement about *equality* of the sets:
     * a token the CSS declares and Kotlin misses is an un-applied amendment; a token Kotlin changes and
     * the CSS does not is Compose inventing spec. The comparison runs over the data class's own
     * `toString`, so a token added to [ZinelyV2Colors] tomorrow is covered without touching this test.
     */
    @Test
    fun `the Compose sheet island overrides exactly the tokens the frozen page declares`() {
        fun fields(c: ZinelyV2Colors): Map<String, String> =
            Regex("""(\w+)=(Color\([^)]*\))""").findAll(c.toString())
                .associate { it.groupValues[1] to it.groupValues[2] }

        val room = zinelyV2DarkColors()
        val changed = fields(room).filter { (k, v) -> fields(BenchStudio.sheetIsland(room))[k] != v }.keys

        val declared = Regex("""(--[a-z-]+)\s*:\s*#""").findAll(rule(".page"))
            .map { m ->
                m.groupValues[1].removePrefix("--").split('-')
                    .mapIndexed { i, w -> if (i == 0) w else w.replaceFirstChar(Char::uppercase) }.joinToString("")
            }.toSet()

        assertEquals(
            "the sheet island and the frozen `.page` block must name the same tokens",
            declared.sorted(), changed.sorted(),
        )
        assertTrue(
            "the sheet's shadow belongs to the room, not the sheet — it must not be lightened at night",
            "pageShadow" !in changed && "pageContact" !in changed,
        )
    }

    // -- the sheet (rows 1.5-1.7) ------------------------------------------------------------------

    @Test
    fun `the page's radius is the frozen one, and it is the shape the grain is clipped to`() {
        assertEquals(px(".page", "border-radius").dp, BenchStudio.PageRadius)
        assertEquals(RoundedCornerShape(BenchStudio.PageRadius), BenchStudio.PageShape)
    }

    @Test
    fun `the keep-clear radius is the frozen one, and is tighter than the page's`() {
        assertEquals(px(".keepclear", "border-radius").dp, BenchStudio.KeepClearRadius)
        assertTrue(
            "the cue's corner is deliberately tighter than the sheet's",
            BenchStudio.KeepClearRadius < BenchStudio.PageRadius,
        )
    }

    @Test
    fun `the page grain is the frozen tile and the effective alpha Phase A published`() {
        val tile = Regex("""background-size\s*:\s*([\d.]+)px""").find(rule(".page::after"))!!.groupValues[1]
        assertEquals(BenchStudio.PageGrainTile, tile.toDouble().dp)
        val opacity = Regex("""opacity\s*:\s*([\d.]+)""").find(rule(".page::after"))!!.groupValues[1].toFloat()
        // Effective strength = the tile's baked alpha × the rule's own opacity (ZinelyV2Grain's table).
        // The baked half is read from ZinelyV2Grain, not re-typed, so this cannot agree with a drifted tile.
        assertEquals(ZinelyV2Grain.BakedAlpha * opacity, BenchStudio.PAGE_GRAIN_ALPHA, 1e-6f)
    }

    @Test
    fun `the screen grain is the frozen tile and alpha`() {
        val tile = Regex("""background-size\s*:\s*([\d.]+)px""").find(rule(".phone::after"))!!.groupValues[1]
        assertEquals(BenchStudio.ScreenGrainTile, tile.toDouble().dp)
        val opacity = Regex("""opacity\s*:\s*([\d.]+)""").find(rule(".phone::after"))!!.groupValues[1].toFloat()
        assertEquals(ZinelyV2Grain.BakedAlpha * opacity, BenchStudio.SCREEN_GRAIN_ALPHA, 1e-6f)
    }

    // -- the page's two-layer shadow (row 1.6) -----------------------------------------------------

    @Test
    fun `the page shadow is two layers, and its two tokens are distinct in both themes`() {
        val light = BenchStudio.pageShadowLayers(zinelyV2LightColors())
        val dark = BenchStudio.pageShadowLayers(zinelyV2DarkColors())
        assertEquals(2, light.size)
        assertEquals(2, dark.size)
        // The mutation this guards: collapsing both layers onto one token renders almost right in light
        // and wrong in dark, which is the bug the D-010 amendment exists to prevent.
        assertNotEquals(light[0].color, light[1].color)
        assertNotEquals(dark[0].color, dark[1].color)
        // And the two themes must not share a value, or the page keeps a warm shadow on a dark desk.
        assertNotEquals(light[0].color, dark[0].color)
        assertNotEquals(light[1].color, dark[1].color)
    }

    @Test
    fun `the page shadow's geometry is the frozen declaration, negative spread included`() {
        val decl = Regex("""box-shadow\s*:\s*([^;]*);""").find(rule(".page"))!!.groupValues[1]
        val layers = Regex("""(-?[\d.]+)px\s+(-?[\d.]+)px(?:\s+(-?[\d.]+)px)?""").findAll(decl).toList()
        assertEquals("the frozen `.page` shadow is two layers", 2, layers.size)

        val built = BenchStudio.pageShadowLayers(zinelyV2LightColors())
        assertEquals(layers[0].groupValues[1].toDouble().dp, built[0].dy)
        assertEquals(layers[0].groupValues[2].toDouble().dp, built[0].blur)
        assertEquals(layers[0].groupValues[3].toDouble().dp, built[0].spread)
        assertEquals(layers[1].groupValues[1].toDouble().dp, built[1].dy)
        assertEquals(layers[1].groupValues[2].toDouble().dp, built[1].blur)

        assertTrue("the cast layer pulls back under the sheet", built[0].spread.value < 0f)
        assertEquals("the contact layer has no spread", 0.dp, built[1].spread)
    }

    // -- the keep-clear, and why D-033 existed (rows 1.8, 1.9) -------------------------------------

    @Test
    fun `the frozen page carries the document's real panel aspect`() {
        val w = px(".page", "width")
        val h = px(".page", "height")
        val panel = SingleSheet8Panel()
        val frozenRatio = w / h
        val realRatio = panel.first / panel.second
        // The D-033 amendment. Before it this was 0.6503 against 0.7071 — eight percent out.
        assertEquals("the frozen page must depict the real panel's aspect", realRatio, frozenRatio, 0.001)
    }

    @Test
    fun `the derived keep-clear inset reproduces the frozen literal on the frozen page`() {
        val pageWidth = px(".page", "width")
        val panelWidthPt = SingleSheet8Panel().first
        val derived = BenchStudio.keepClearInsetPx(pageWidth.toFloat(), panelWidthPt)
        val frozen = px(".keepclear", "inset")

        // THE test of this package. The frozen number and the engine's safe area are the same boundary
        // expressed twice; if they ever stop agreeing, the cue is decorative again and D-033 is back.
        assertEquals(
            "the frozen `.keepclear` inset must be `safeAreaInsetPt` scaled to the frozen page",
            frozen,
            derived.toDouble(),
            0.01,
        )
    }

    @Test
    fun `the keep-clear inset is uniform because the page shares the panel's aspect`() {
        val panel = SingleSheet8Panel()
        val w = px(".page", "width")
        val h = px(".page", "height")
        val horizontal = Imposer.DEFAULT_SAFE_AREA_INSET_PT * w / panel.first
        val vertical = Imposer.DEFAULT_SAFE_AREA_INSET_PT * h / panel.second
        // A uniform inset is only honest when these agree. On the pre-amendment 212×326 page they were
        // 17.1 and 18.6 — which is why one number could not be truthful, and why the aspect had to move
        // before the inset could.
        assertEquals("one uniform inset requires both axes to agree", horizontal, vertical, 0.02)
    }

    @Test
    fun `the keep-clear inset scales with the page rather than being a fixed dp`() {
        val panelWidthPt = SingleSheet8Panel().first
        val small = BenchStudio.keepClearInsetPx(229f, panelWidthPt)
        val large = BenchStudio.keepClearInsetPx(458f, panelWidthPt)
        assertEquals("twice the page, twice the inset", 2f * small, large, 1e-3f)
    }

    @Test
    fun `a degenerate page draws no keep-clear rather than an inverted one`() {
        assertEquals(0f, BenchStudio.keepClearInsetPx(0f, 210.0), 0f)
        assertEquals(0f, BenchStudio.keepClearInsetPx(229f, 0.0), 0f)
    }

    @Test
    fun `the keep-clear's two states are the frozen opacities and are far apart`() {
        assertEquals(0.32f, BenchStudio.KEEP_CLEAR_REST_ALPHA, 1e-6f)
        assertEquals(0.90f, BenchStudio.KEEP_CLEAR_WARN_ALPHA, 1e-6f)
        val rest = Regex("""opacity\s*:\s*([\d.]+)""").find(rule(".keepclear"))!!.groupValues[1].toFloat()
        val warn = Regex("""opacity\s*:\s*([\d.]+)""").find(rule(".keepclear.warn"))!!.groupValues[1].toFloat()
        assertEquals(rest, BenchStudio.KEEP_CLEAR_REST_ALPHA, 1e-6f)
        assertEquals(warn, BenchStudio.KEEP_CLEAR_WARN_ALPHA, 1e-6f)
    }

    @Test
    fun `the warn state recolours to strawberry-text and is not merely brighter ink`() {
        assertTrue(rule(".keepclear.warn").contains("--strawberry-text"))
        assertTrue(rule(".keepclear").contains("--ink-faint"))
        assertNotEquals(zinelyV2LightColors().strawberryText, zinelyV2LightColors().inkFaint)
    }

    // -- the warn trigger, D-032's ruling (row 1.9) -------------------------------------------------
    //
    // The ruling is a behaviour, so these are the package's only tests that assert something the CSS
    // cannot state. They are written as the four bullets the owner wrote, one test each, because the
    // failure that matters is not "the wrong pixels" but "the warning outlived the gesture".

    /** An element whose box is well inside the keep-clear rect on a [PAGE] page. */
    private fun inside() = Transform(xPt = 60.0, yPt = 60.0, widthPt = 40.0, heightPt = 40.0)

    /** An element whose box crosses the left keep-clear boundary (17pt). */
    private fun crossing() = Transform(xPt = 2.0, yPt = 60.0, widthPt = 40.0, heightPt = 40.0)

    private fun pageOf(t: Transform) = Page(
        index = 0,
        role = PageRole.INTERIOR,
        elements = listOf(TextElement(id = "a", transform = t, text = "x")),
    )

    private fun transforming(t: Transform) =
        Interaction.Transforming(pageIndex = 0, ids = setOf("a"), before = mapOf("a" to t), token = 1L)

    @Test
    fun `idle never warns, even with content already inside the keep-clear area`() {
        // The bullet the ruling spends most of its words on: this is NOT a validity indicator. A zine
        // that has been left with a photo in the margin is not a zine the Bench nags about.
        assertEquals(
            false,
            BenchStudio.keepClearWarn(
                page = pageOf(crossing()),
                interaction = Interaction.Idle,
                live = null,
                resizeOverride = null,
                screenPxPerPt = SCALE,
                pageSizePt = PAGE,
            ),
        )
    }

    @Test
    fun `a drag that carries content into the keep-clear area warns while it runs`() {
        assertEquals(
            true,
            BenchStudio.keepClearWarn(
                page = pageOf(crossing()),
                interaction = transforming(crossing()),
                live = LiveTransform(),
                resizeOverride = null,
                screenPxPerPt = SCALE,
                pageSizePt = PAGE,
            ),
        )
    }

    /**
     * The ruling's **central** case, and the one the first cut of this file missed: a gesture that
     * *carries* content across the boundary, rather than one that starts across it.
     *
     * The review proved the gap by mutation — replacing the live gesture with the identity
     * `LiveTransform()` in `EditorScreen` killed no test, because every warn test here passed the
     * identity and set `before` to the final geometry. So `LiveSnap.resolve` could have been handed a
     * constant and the suite would have stayed green while row 1.9's whole behaviour was gone.
     *
     * These two tests are the fix: the element starts clear of the margin and the **pan** is what puts
     * it inside, so the assertion depends on `live` being read. 150px at 2.5 px/pt is −60pt, which
     * takes `inside()`'s left edge from 60 to 0 — well inside the 17pt margin.
     */
    @Test
    fun `a drag that carries clear content into the margin warns because of the gesture, not the start`() {
        val start = inside()
        assertEquals(
            "precondition: it does not warn where the drag began",
            false,
            BenchStudio.keepClearWarn(
                pageOf(start), transforming(start), LiveTransform(), null, SCALE, PAGE,
            ),
        )
        assertEquals(
            "the pan carried it across the boundary",
            true,
            BenchStudio.keepClearWarn(
                page = pageOf(start),
                interaction = transforming(start),
                live = LiveTransform(panXpx = -150.0),
                resizeOverride = null,
                screenPxPerPt = SCALE,
                pageSizePt = PAGE,
            ),
        )
    }

    @Test
    fun `a drag that keeps content clear does not warn, however far it travels`() {
        // The mirror, so the test above cannot be satisfied by "any non-identity gesture warns". 25px
        // is 10pt: from x=60 to x=50, still outside the 17pt margin.
        val start = inside()
        assertEquals(
            false,
            BenchStudio.keepClearWarn(
                page = pageOf(start),
                interaction = transforming(start),
                live = LiveTransform(panXpx = -25.0),
                resizeOverride = null,
                screenPxPerPt = SCALE,
                pageSizePt = PAGE,
            ),
        )
    }

    @Test
    fun `the same drag over content that clears the boundary does not warn`() {
        // Guards the mutant that warns for *any* in-flight gesture: the geometry has to matter, or the
        // cue means "you are touching something" rather than "this will not print".
        assertEquals(
            false,
            BenchStudio.keepClearWarn(
                page = pageOf(inside()),
                interaction = transforming(inside()),
                live = LiveTransform(),
                resizeOverride = null,
                screenPxPerPt = SCALE,
                pageSizePt = PAGE,
            ),
        )
    }

    @Test
    fun `the warning does not survive the end of the interaction`() {
        // THE mutation of row 1.9. Same document, same offending geometry, gesture over: `live` is null
        // and the reducer is back to Idle, and the answer must flip to false. If this ever passes with a
        // persisted flag behind it, the warning has become document state and the ruling is broken.
        val offending = pageOf(crossing())
        assertEquals(
            true,
            BenchStudio.keepClearWarn(
                offending, transforming(crossing()), LiveTransform(), null, SCALE, PAGE,
            ),
        )
        assertEquals(
            false,
            BenchStudio.keepClearWarn(offending, Interaction.Idle, null, null, SCALE, PAGE),
        )
    }

    @Test
    fun `a handle resize warns on its own override, without a LiveTransform`() {
        // The resize path wins over `live` in EditorPagePreview; the cue must follow the same branch or
        // it goes quiet during exactly the gesture most likely to push content off the printable area.
        assertEquals(
            true,
            BenchStudio.keepClearWarn(
                page = pageOf(inside()),
                interaction = Interaction.Idle,
                live = null,
                resizeOverride = mapOf("a" to crossing()),
                screenPxPerPt = SCALE,
                pageSizePt = PAGE,
            ),
        )
    }

    @Test
    fun `rotation is expanded, so a turned element's corner is seen entering the margin`() {
        // A 40×40 box at x=20 clears a 17pt margin unrotated (its left edge is at 20). Turned 45° about
        // its centre the diagonal spans 56.6pt, putting the left corner at 11.7 — inside the margin and
        // visibly so. Reporting "no warning" here is the defect this test exists to prevent.
        val square = Transform(xPt = 20.0, yPt = 100.0, widthPt = 40.0, heightPt = 40.0)
        val turned = square.copy(rotationDegrees = 45.0)
        assertEquals(
            "unrotated, this box clears the boundary",
            false,
            BenchStudio.keepClearWarn(
                pageOf(square), transforming(square), LiveTransform(), null, SCALE, PAGE,
            ),
        )
        assertEquals(
            "rotated, its corner does not",
            true,
            BenchStudio.keepClearWarn(
                pageOf(turned), transforming(turned), LiveTransform(), null, SCALE, PAGE,
            ),
        )
    }

    // -- the centre guide (row 1.10) ---------------------------------------------------------------

    @Test
    fun `the guide stops the frozen 8px short at both ends and shows at the frozen opacity`() {
        assertEquals(px(".guide.v", "top").dp, BenchStudio.GuideEndInset)
        assertEquals(px(".guide.v", "bottom").dp, BenchStudio.GuideEndInset)
        val show = Regex("""opacity\s*:\s*([\d.]+)""").find(rule(".guide.show"))!!.groupValues[1].toFloat()
        assertEquals(show, BenchStudio.GUIDE_ALPHA, 1e-6f)
    }

    @Test
    fun `the guide is matcha, not the V1 teal it used to be`() {
        assertTrue("the frozen guide is --matcha", rule(".guide").contains("var(--matcha)"))
    }

    // -- the page number (row 1.11) ----------------------------------------------------------------

    @Test
    fun `the page number sits at the frozen offsets`() {
        assertEquals(px(".pagenum", "top").dp, BenchStudio.PageNumberTopInset)
        assertEquals(px(".pagenum", "right").dp, BenchStudio.PageNumberEndInset)
    }

    @Test
    fun `the page number is the frozen size and tracking`() {
        // Row 1.11's planned mutation is `9px→11px`; until these two were extracted from the composable
        // they were inline literals with nothing asserting them, so that mutation had nothing to kill.
        assertEquals(px(".pagenum", "font-size"), BenchStudio.PageNumberSize.value.toDouble(), 1e-6)
        val tracking = Regex("""letter-spacing\s*:\s*([\d.]+)em""").find(rule(".pagenum"))!!.groupValues[1]
        assertEquals(tracking.toDouble(), BenchStudio.PageNumberTracking.value.toDouble(), 1e-6)
    }

    @Test
    fun `the guide is the frozen hairline, which is row 1_10's other mutation`() {
        // `.guide.v{width:1px}` — the stroke SnapGuides draws. Same reason as above: row 1.10 plans a
        // `width 1→2` mutation, and nothing here was checking the width.
        assertEquals(px(".guide.v", "width"), ZinelyV2Dimens.Hairline.value.toDouble(), 1e-6)
    }

    /**
     * The real panel: `A4.landscape()` tiled by [ZineFormat.SINGLE_SHEET_8]'s grid — the same derivation
     * the imposer makes, taken from the imposer rather than restated, so this test cannot certify a
     * page against a panel the engine has stopped producing.
     */
    private fun SingleSheet8Panel(): Pair<Double, Double> {
        val layout = com.aritr.zinely.core.imposition.SingleSheet8Imposer()
            .layout(ZineFormat.SINGLE_SHEET_8, PaperSize.A4)
        val panel = layout.panels.first().panelLocalBounds
        return panel.width to panel.height
    }
}
