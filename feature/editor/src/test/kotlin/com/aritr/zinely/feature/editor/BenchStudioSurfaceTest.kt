package com.aritr.zinely.feature.editor

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
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
import com.aritr.zinely.ui.theme.ZinelyV2Dimens
import com.aritr.zinely.ui.theme.zinelyV2LightColors
import com.aritr.zinely.ui.theme.ZinelyV21Colors
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Grain
import com.aritr.zinely.ui.theme.zinelyV21DarkColors
import com.aritr.zinely.ui.theme.zinelyV21LightColors
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **The studio surface**, pinned to the DESIGN-FROZEN Bench.
 *
 * Every constant this package transcribes is re-derived from the frozen mockup **at test time** rather
 * than compared against a second copy written here, following `ZinelyV2DimensTest`'s pattern and
 * [ADR-073](../../../../../../../../../docs/DECISIONS.md#adr-073)'s rule: compare against the declared
 * CSS, not against a rendering of it. A transcription test that hard-codes the frozen number on both
 * sides passes the day the design changes and the implementation does not, which is the one failure it
 * exists to catch.
 *
 * ### Two frozen files, on purpose
 *
 * [`v21-bench.html`](../../../../../../../../../docs/design/mockups/v21-bench.html) is the canonical
 * Bench ([ADR-099](../../../../../../../../../docs/DECISIONS.md#adr-099)) and [rule] reads it. **P2
 * landed on 2026-08-13** ([§12.9](../../../../../../../../../docs/DECISIONS.md#adr-102-p2-marks)), so
 * the keep-clear cue and the snap guide — the last two marks pinned to V2 — now read V2.1 like the rest.
 *
 * An earlier version of this note promised that *"when P2 lands, [ruleV2] and every caller of it are
 * deleted."* **That was too broad, and the remaining callers say why.** What survives is not V2 styling
 * waiting to be converted; it is the D-032 warn-trigger geometry (`:500-530`), which V2.1's freeze does
 * not specify **because the warn state is a departure the freeze does not contain**. Those rows have no
 * V2.1 address to move to, and pointing them at one would be inventing a specification. They stay on
 * [ruleV2] until the freeze grows a `.keepclear.warn` — which is exactly what
 * `the warn state is a documented departure…` asserts it currently has not.
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

        /** The CSS initial root font size, which `rem` in the frozen file resolves against. */
        const val CSS_ROOT_PX = 16.0
    }

    private fun css(name: String): String {
        val f = File("../../docs/design/mockups/$name")
        assertTrue("expected :feature:editor as the working directory — missing $f", f.isFile)
        return f.readText().substringBefore("</style>")
    }

    /** The canonical V2.1 Bench. */
    private val benchCss: String = css("v21-bench.html")

    /** The superseded V2 Bench, read **only** for the marks ADR-102 P2 still owns. See the class docs. */
    private val benchV2Css: String = css("v2-bench.html")

    /**
     * Every declaration block for [selector], joined — because a selector may legitimately carry more
     * than one.
     *
     * The first cut of this read only the *first* block, which was true of the frozen file until the
     * [D-035](../../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-035) amendment gave V2's
     * `.page` a second one holding the light-theme island's tokens. Five assertions then failed looking
     * for `width` in a block that declares only custom properties — the test was reading CSS's shape
     * wrong, and the spec was right. **V2.1's `.page` has no such second block** (see
     * [the island test][`the Compose sheet island lights exactly the tokens the frozen page paints`]),
     * but the joining stays, because it is the correct way to read CSS and not a workaround.
     */
    private fun rule(selector: String): String = ruleIn(benchCss, selector, "V2.1")

    private fun ruleV2(selector: String): String = ruleIn(benchV2Css, selector, "V2")

    private fun ruleIn(source: String, selector: String, which: String): String {
        val escaped = Regex.escape(selector)
        val blocks = Regex("""(?m)^\s*$escaped\s*\{([^}]*)}""").findAll(source)
            .map { it.groupValues[1] }.toList()
        assertTrue("frozen $which Bench has no `$selector` rule", blocks.isNotEmpty())
        return blocks.joinToString(";")
    }

    private fun px(selector: String, property: String): Double = pxIn(rule(selector), selector, property)

    private fun pxV2(selector: String, property: String): Double = pxIn(ruleV2(selector), selector, property)

    private fun pxIn(block: String, selector: String, property: String): Double {
        val m = Regex("""(?<![\w-])$property\s*:\s*(-?[\d.]+)px""").find(block)
        assertTrue("`$selector` declares no `$property` in px", m != null)
        return m!!.groupValues[1].toDouble()
    }

    /**
     * The frozen V2.1 page region — every rule from the `the canvas` section banner to the next banner.
     *
     * **This is the island's expected set, and it is derived rather than declared.** V2's `.page` carried
     * a second declaration block re-stating the on-paper tokens (the
     * [D-035](../../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-035) amendment), and the old
     * version of this test compared the Compose island against *that block*. **`v21-bench.html:177-182`
     * declares no custom properties at all** — the V2.1 corpus was frozen on 2026-08-09, before
     * [OD-31](../../../../../../../../../docs/DECISIONS.md#adr-098-od31) closed on 2026-08-12 — so
     * pointing the old test at V2.1 would derive an **empty** expected set and pass only if the island
     * changed nothing. Two reviews caught that before any P1 code was written.
     *
     * Amending the frozen file to add a block existing solely so a test could read it was rejected
     * ([§12.2](../../../../../../../../../docs/DECISIONS.md#adr-102-p1-test)). Deriving from what the
     * page **paints** is strictly stronger: it needs no amendment, and a token added to the page region
     * tomorrow fails the build without anyone editing a list.
     */
    private fun pageRegionCss(): String {
        val m = Regex("""(?ms)/\*[^*]*the canvas[^*]*\*/(.*?)/\* =""").find(benchCss)
        assertTrue("the frozen V2.1 Bench has no `the canvas` section banner", m != null)
        return m!!.groupValues[1]
    }

    /**
     * **The Compose island lights exactly the tokens the frozen page paints — no more, no fewer.**
     *
     * This is the assertion whose absence let C1 ship a defect. That cut provided `zinelyV2LightColors()`
     * wholesale, lightening all twenty-six tokens rather than the eight, and among the extras were
     * `pageShadow` / `pageContact` — so in dark the sheet cast a warm-brown shadow on a dark desk,
     * reinstating [D-010](../../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-010) inside the fix
     * for D-035. Every other test stayed green, and the re-recorded dark golden certified it.
     *
     * **The same trap is live in V2.1 with a different token.** The V2.1 page's shadow is
     * `var(--hard) var(--hard) 0 var(--ink-line)`, so the hazard is now `inkLine` — `#33261C` light,
     * `#120E0A` dark. ADR-102 §3 forbade `softShadow`/`contact` by name, which the V2.1 page does not use
     * at all; the warning was right and its cause was stale
     * ([§12.1](../../../../../../../../../docs/DECISIONS.md#adr-102-island-v21)). The explicit
     * exclusion below is what stops the third instance of D-010.
     *
     * Both directions are checked, because only the pair is a statement about *equality* of the sets: a
     * token the page paints and Kotlin misses is an un-applied amendment; a token Kotlin lights and the
     * page does not paint is Compose inventing spec. The comparison runs over the data class's own
     * `toString`, so a token added to [ZinelyV21Colors] tomorrow is covered without touching this test.
     */
    @Test
    fun `the Compose sheet island lights exactly the tokens the frozen page paints`() {
        fun fields(c: ZinelyV21Colors): Map<String, String> =
            Regex("""(\w+)=(Color\([^)]*\))""").findAll(c.toString())
                .associate { it.groupValues[1] to it.groupValues[2] }

        val room = zinelyV21DarkColors()
        val lit = fields(room).filter { (k, v) -> fields(BenchStudio.sheetIslandV21(room))[k] != v }.keys

        // Every `var(--x)` in the page region, less the non-colour ladders (`--br-*`, `--gap-*`, the
        // type roles, the grain texture and `--hard`, which is a distance) and less the shadow ink.
        val notColours = setOf("hard", "grain", "voice", "serif", "sans")
        val painted = Regex("""var\(--([a-z-]+)\)""").findAll(pageRegionCss())
            .map { it.groupValues[1] }
            .filterNot { it.startsWith("br-") || it.startsWith("gap-") || it in notColours }
            // `--ink-line` is the page's DROP SHADOW. It falls on the bench, not on the paper, so it is
            // the room's — lighting it would put `#33261C` under the sheet on a `#211B15` worktop.
            .filterNot { it == "ink-line" }
            .map { token ->
                token.split('-').mapIndexed { i, w -> if (i == 0) w else w.replaceFirstChar(Char::uppercase) }
                    .joinToString("")
            }.toSet()

        assertEquals(
            "the sheet island and the frozen page region must name the same tokens",
            painted.sorted(), lit.sorted(),
        )
        assertTrue(
            "the sheet's shadow belongs to the room, not the sheet — it must not be lightened at night",
            "inkLine" !in lit,
        )
        // The set is eight. Pinned as a number as well as a comparison, because both sides of the
        // comparison are derived and a regex that silently matched nothing would agree with an island
        // that lit nothing — which is exactly how the V2 version of this test would have passed on V2.1.
        assertEquals("the V2.1 island is eight tokens", 8, lit.size)
    }

    /**
     * The island restates the **light** theme's values and invents no colour.
     *
     * That the tokens are lit is the amendment; that they equal `zinelyV21LightColors()` is what makes it
     * an amendment rather than a second palette — the owner's ruling forbids inventing one.
     */
    @Test
    fun `the island restates the light palette rather than inventing a third one`() {
        val light = zinelyV21LightColors()
        val islanded = BenchStudio.sheetIslandV21(zinelyV21DarkColors())
        assertEquals(light.paper, islanded.paper)
        assertEquals(light.ink, islanded.ink)
        assertEquals(light.inkSoft, islanded.inkSoft)
        assertEquals(light.berry, islanded.berry)
        assertEquals(light.berryTint, islanded.berryTint)
        assertEquals(light.butter, islanded.butter)
        assertEquals(light.jam, islanded.jam)
        assertEquals(light.leaf, islanded.leaf)
        // And the room's own values survive where they must.
        assertEquals(zinelyV21DarkColors().inkLine, islanded.inkLine)
        assertEquals(zinelyV21DarkColors().bench, islanded.bench)
    }

    // -- the sheet (rows 1.5-1.7) ------------------------------------------------------------------

    /**
     * `.page{border-radius:var(--br-xs) var(--br-md) var(--br-md) var(--br-xs)}` — **the spine.**
     *
     * Four values, not one: V2 drew a uniform `5px`. The two leading corners are tight and the two
     * trailing ones generous, so the sheet reads as bound on one edge. Asserted as the resolved ladder
     * steps rather than as literals, because the frozen file writes the `--br-*` names and
     * [ZinelyV21Dimens] is where those live.
     */
    @Test
    fun `the page's radius is the frozen spine, and it is the shape the grain is clipped to`() {
        val decl = Regex("""border-radius\s*:\s*([^;}]*)""").find(rule(".page"))!!.groupValues[1].trim()
        val steps = Regex("""var\(--(br-[a-z]+)\)""").findAll(decl).map { it.groupValues[1] }.toList()
        assertEquals("the frozen page radius is four values", listOf("br-xs", "br-md", "br-md", "br-xs"), steps)
        assertEquals(ZinelyV21Dimens.radiusXs, BenchStudio.PageRadiusSpine)
        assertEquals(ZinelyV21Dimens.radiusMd, BenchStudio.PageRadiusFree)
        assertEquals(
            RoundedCornerShape(
                topStart = BenchStudio.PageRadiusSpine,
                topEnd = BenchStudio.PageRadiusFree,
                bottomEnd = BenchStudio.PageRadiusFree,
                bottomStart = BenchStudio.PageRadiusSpine,
            ),
            BenchStudio.PageShape,
        )
        assertNotEquals(
            "the spine is the point — a uniform radius is the V2 sheet",
            BenchStudio.PageRadiusSpine, BenchStudio.PageRadiusFree,
        )
    }

    /**
     * `.page{border:1.5px solid var(--ink)}` — and it is **`ink`, not `paperEdge`**.
     *
     * The token change is the half worth asserting: `paperEdge` survives in the palette with no use on
     * this page, which is why it left the island ([§12.1](../../../../../../../../../docs/DECISIONS.md#adr-102-island-v21)).
     */
    @Test
    fun `the page's edge is the frozen ink line, not the old paper-edge hairline`() {
        assertEquals(px(".page", "border"), BenchStudio.PageBorder.value.toDouble(), 1e-6)
        assertTrue("the frozen page border is --ink", rule(".page").contains("solid var(--ink)"))
        assertTrue(
            "`paperEdge` has no V2.1 use on the page and must not be reintroduced here",
            !rule(".page").contains("--paper-edge"),
        )
    }

    /**
     * The keep-clear cue's radius, **now V2.1's** — P2 converted the cue on 2026-08-13, so this moved
     * from [pxV2] to [rule] exactly as the note it replaced said it would.
     *
     * The freeze writes `var(--br-xs)` rather than a pixel count, so this resolves the custom property
     * instead of reading a number that is not there. Radius, colour and trigger all moved in the same
     * change, which was the whole point of §12.3's re-cut.
     */
    @Test
    fun `the keep-clear radius is the frozen --br-xs, converted with the rest of the cue in P2`() {
        val declared = Regex("""border-radius\s*:\s*var\(--br-xs\)""")
        assertTrue(
            "the frozen V2.1 cue names the radius token rather than a pixel value",
            declared.containsMatchIn(rule(".keepclear")),
        )
        assertEquals(ZinelyV21Dimens.radiusXs, BenchStudio.KeepClearRadius)
    }

    // -- the two marks P1 also re-cut: the selection ring and the handles --------------------------

    /**
     * `.el .ring{inset:-6px;border:1.6px dashed var(--ink);border-radius:var(--br-sm)}`
     * ([`v21-bench.html:188-189`](../../../../../../../../../docs/design/mockups/v21-bench.html)).
     *
     * [ADR-073](../../../../../../../../../docs/DECISIONS.md#adr-073)'s rule is not a rule about the
     * *sheet* — it is a rule about anything transcribed from the freeze. P1's first cut applied it to
     * the sheet and hand-copied the ring and the handles, so a typo in either would have shipped
     * green. These two tests close that.
     *
     * The dash *length* is deliberately not asserted against the freeze: CSS `dashed` names no
     * period, so [SelectionOutlineDashDp] is a Compose-side choice (one stroke on, one off) and is
     * asserted only as that relationship.
     *
     * ### The inset assertion is the one that was wrong, and it is worth reading twice
     *
     * The first version of this test asserted `SelectionOutlineInsetDp == -inset` — the CSS number,
     * straight across. That **passed while the ring was 0.8dp too far out**, because `inset:-6px` is
     * where the ring's *outer edge* goes and a CSS border paints inside its box, while Compose's
     * `Stroke` is centred on the path. The number Compose needs is `-6 + border/2 = -5.2`.
     *
     * ADR-073 says compare against the declared CSS rather than a rendering of it, and this test did
     * exactly that and was still wrong — **reading the declaration is not the same as reading the
     * geometry it declares.** So the assertion now derives the centre-line from the freeze's own two
     * numbers rather than transcribing one of them.
     */
    @Test
    fun `the selection ring restates the frozen dashed ink ring`() {
        val ring = rule(".el .ring")
        val outerEdge = pxIn(ring, ".el .ring", "inset")
        val stroke = pxIn(ring, ".el .ring", "border")
        assertEquals(
            "the ring's STROKE CENTRE is half a border inside the frozen outer edge — not the edge itself",
            -(outerEdge + stroke / 2),
            SelectionOutlineInsetDp.value.toDouble(),
            0.0001,
        )
        assertEquals(SelectionOutlineStrokeDp.value.toDouble(), stroke, 0.0001)
        assertTrue("the frozen ring is dashed, not solid", Regex("""border\s*:[^;}]*dashed""").containsMatchIn(ring))
        assertTrue("the frozen ring is drawn in `--ink`", Regex("""border\s*:[^;}]*var\(--ink\)""").containsMatchIn(ring))
        assertEquals(
            "the frozen ring radius is the `--br-sm` ladder step",
            "br-sm",
            Regex("""border-radius\s*:\s*var\(--(br-[a-z]+)\)""").find(ring)!!.groupValues[1],
        )
        assertEquals(ZinelyV21Dimens.radiusSm, SelectionOutlineRadiusDp)
        // …and the ladder step itself, which nothing else pins. Without this the radius chain reads
        // `frozen --br-sm → ZinelyV21Dimens.radiusSm → the constant`, with the first arrow unasserted:
        // there is no `ZinelyV21DimensTest`, so `radiusSm` is a hand-copy until something reads `:root`.
        assertEquals(pxIn(rule(":root"), ":root", "--br-sm").dp, ZinelyV21Dimens.radiusSm)
    }

    /**
     * `.hnd{width:9px;height:9px;background:var(--paper);border:1.6px solid var(--ink);border-radius:2px}`
     * ([`v21-bench.html:198-199`](../../../../../../../../../docs/design/mockups/v21-bench.html)).
     *
     * **The halo is not in the freeze and is asserted as such.** V2.1 drops the white ring V2 drew
     * behind the mark; Compose keeps it because the frozen mockup's handles never sit on a photo and
     * the app's do, and IA §C.4 wants 3:1 over *any* backdrop. That is a retained divergence, not a
     * transcription — so it is pinned here against the freeze's silence rather than to a number.
     */
    @Test
    fun `the handle mark restates the frozen rounded square`() {
        val hnd = rule(".hnd")
        assertEquals(
            "the frozen handle is square",
            pxIn(hnd, ".hnd", "width"),
            pxIn(hnd, ".hnd", "height"),
            0.0001,
        )
        assertEquals(HandleDiameterDp.value.toDouble(), pxIn(hnd, ".hnd", "width"), 0.0001)
        assertEquals(HandleBorderDp.value.toDouble(), pxIn(hnd, ".hnd", "border"), 0.0001)
        assertEquals(HandleRadiusDp.value.toDouble(), pxIn(hnd, ".hnd", "border-radius"), 0.0001)
        // The mark's displacement, derived from the freeze's two numbers rather than transcribed:
        // `left:-10px` on a 9px border-box mark ⇒ centre at -10 + 9/2 = -5.5px. Corrected 2026-08-12;
        // Compose drew these at 0 because ADR-091 row 2.6 called the offset "satisfied by construction"
        // on arithmetic that was already false in V2.
        val corner = rule(".hnd.tl")
        assertEquals(
            "the frozen mark's centre sits OUTSIDE the box by half its width less the offset",
            -(pxIn(corner, ".hnd.tl", "left") + pxIn(hnd, ".hnd", "width") / 2),
            HandleRingOffsetDp.value.toDouble(),
            0.0001,
        )
        // …and the edge handles land on the same line, which is what makes the eight one figure. The
        // freeze writes them as `calc(50% - 4.5px)`, i.e. the mark's centre on the edge's midpoint.
        assertTrue(
            "the frozen file must carry the four edge handles the app has always drawn (D-036)",
            Regex("""\.hnd\.t\s*\{""").containsMatchIn(benchCss) && Regex("""\.hnd\.r\s*\{""").containsMatchIn(benchCss),
        )
        assertEquals(
            "an edge handle's free axis is centred: calc(50% - width/2)",
            pxIn(hnd, ".hnd", "width") / 2,
            Regex("""calc\(50%\s*-\s*([\d.]+)px\)""").find(rule(".hnd.t"))!!.groupValues[1].toDouble(),
            0.0001,
        )
        // …and its BOUND axis carries the same 5.5px the corners do. Asserted separately because the free
        // axis above is the half-width and the bound axis is the standoff — a review found only the first
        // of the two here, which left the edge handles' actual displacement unpinned.
        assertEquals(
            "an edge handle's bound axis stands off exactly as far as a corner's",
            -(pxIn(rule(".hnd.t"), ".hnd.t", "top") + pxIn(hnd, ".hnd", "width") / 2),
            HandleRingOffsetDp.value.toDouble(),
            0.0001,
        )
        assertTrue("the frozen handle fills with `--paper`", hnd.contains("background:var(--paper)"))
        assertTrue("the frozen handle is bordered in `--ink`", Regex("""border\s*:[^;}]*var\(--ink\)""").containsMatchIn(hnd))
        assertTrue(
            "the frozen handle draws no halo — Compose's is a documented divergence, so if the freeze " +
                "ever grows one this test must be re-read rather than deleted",
            !hnd.contains("box-shadow") && !hnd.contains("outline"),
        )
        // The halo's own geometry is asserted where it can fail — on the raster, in
        // `BenchSelectionAppearanceTest`. Restating `HandleHaloDiameterDp == HandleDiameterDp + 2·width`
        // here would only restate its definition, and a test that cannot fail is worse than no test: it
        // reads as coverage.
    }

    /**
     * ⚠ **The page's and the screen's grain swapped alphas between V2 and V2.1**, and both tiles moved:
     * V2 was page `120px/.45`, screen `150px/.35`; V2.1 is page `130px/.35`, screen `160px/.45`. Reading
     * either number across from V2 gets it exactly wrong, which is why both are re-derived here.
     */
    @Test
    fun `the page grain is the frozen tile, alpha and paper blend`() {
        val tile = Regex("""background-size\s*:\s*([\d.]+)px""").find(rule(".page::after"))!!.groupValues[1]
        assertEquals(BenchStudio.PageGrainTile, tile.toDouble().dp)
        val opacity = Regex("""opacity\s*:\s*([\d.]+)""").find(rule(".page::after"))!!.groupValues[1].toFloat()
        // Effective strength = the tile's baked alpha × the rule's own opacity (ZinelyV21Grain's table).
        // The baked half is read from that file, not re-typed, so this cannot agree with a drifted tile.
        assertEquals(ZinelyV21Grain.BakedAlpha * opacity, BenchStudio.PAGE_GRAIN_ALPHA, 1e-6f)
        // `mix-blend-mode:multiply` — paper darkens like ink. The screen's is soft-light.
        assertTrue("the page's grain is the paper blend", rule(".page::after").contains("mix-blend-mode:multiply"))
    }

    @Test
    fun `the screen grain is the frozen tile, alpha and chrome blend`() {
        val tile = Regex("""background-size\s*:\s*([\d.]+)px""").find(rule(".phone::after"))!!.groupValues[1]
        assertEquals(BenchStudio.ScreenGrainTile, tile.toDouble().dp)
        val opacity = Regex("""opacity\s*:\s*([\d.]+)""").find(rule(".phone::after"))!!.groupValues[1].toFloat()
        assertEquals(ZinelyV21Grain.BakedAlpha * opacity, BenchStudio.SCREEN_GRAIN_ALPHA, 1e-6f)
        assertTrue("the screen's grain is the chrome blend", rule(".phone::after").contains("mix-blend-mode:soft-light"))
    }

    @Test
    fun `the ground is the worktop, not the paper it used to be`() {
        assertTrue("`.phone` is the bench", rule(".phone").contains("background:var(--bench)"))
        // The token that makes the island visible for the first time: while ground and sheet were both
        // `paper`, the boundary between room and artifact was nearly invisible.
        assertNotEquals(zinelyV21LightColors().bench, zinelyV21LightColors().paper)
        assertNotEquals(zinelyV21DarkColors().bench, zinelyV21DarkColors().paper)
    }

    // -- the page's hard shadow --------------------------------------------------------------------

    /**
     * `.page{box-shadow:var(--hard) var(--hard) 0 var(--ink-line)}` — one **printed** shadow, offset on
     * both axes, zero blur, flat ink. V2 cast two soft layers in `pageShadow`/`pageContact`; those tokens
     * have no V2.1 counterpart and `pageShadowLayers` was deleted rather than ported.
     */
    @Test
    fun `the page's shadow is the frozen hard offset in ink-line, and it is not blurred`() {
        val decl = Regex("""box-shadow\s*:\s*([^;}]*)""").find(rule(".page"))!!.groupValues[1]
        assertEquals(
            "the frozen page shadow is `--hard --hard 0 --ink-line`",
            "var(--hard) var(--hard) 0 var(--ink-line)", decl.trim(),
        )
        // `--hard` is the published offset, and the third value is a literal zero blur.
        assertEquals(4.dp, ZinelyV21Dimens.hardShadow)
    }

    /**
     * **The shadow ink stays the room's, and this is the D-010 guard restated in V2.1 terms.**
     *
     * `inkLine` is `#33261C` light and `#120E0A` dark. Lighting it inside the island would put a
     * `#33261C` shadow under the sheet on a `#211B15` worktop — brighter than the ground it falls on,
     * a glow where a contact shadow belongs. That is D-010, which this file has already caused once.
     */
    @Test
    fun `the shadow ink is theme-divergent and the island leaves it to the room`() {
        assertNotEquals(
            "inkLine must differ by theme, or this guard is asserting nothing",
            zinelyV21LightColors().inkLine, zinelyV21DarkColors().inkLine,
        )
        val dark = zinelyV21DarkColors()
        assertEquals(
            "the sheet's shadow belongs to the room",
            dark.inkLine, BenchStudio.sheetIslandV21(dark).inkLine,
        )
    }

    // -- the keep-clear, and why D-033 existed (rows 1.8, 1.9) -------------------------------------
    //
    // Pinned to **V2** throughout, for two reasons that happen to point the same way.
    //
    // 1. The cue is ADR-102 **P2**'s, like every other keep-clear assertion here.
    // 2. ⚠ The V2.1 freeze **re-drifted the depiction D-033 was raised to fix.** `v21-bench.html:177`
    //    draws `.page` at `aspect-ratio:3/4` (0.7500) against a real panel of 0.70714, and puts the cue
    //    at a uniform `inset:14px` on a `max-width:266px` sheet — 5.3% where the engine's safe area is
    //    8.1%. That is the same class of stylisation, at about half the magnitude, that the owner
    //    amended `v2-bench.html` to remove (229×324, 18.5px).
    //
    // The **Compose** side is unaffected and that is the whole point of D-033's ruling: `keepClearInsetPx`
    // derives from `Imposer.DEFAULT_SAFE_AREA_INSET_PT`, so it draws the engine's real boundary at any
    // page size and would keep drawing it if the prototype depicted a circle. What these tests assert is
    // the *join* between the transcription and the derivation, and V2 is where that join is currently
    // ruled. Re-pointing them at V2.1 would assert a depiction the owner has not amended.
    //
    // Raised for P2 rather than fixed here: P1 does not own the cue, and amending a frozen file is the
    // owner's.

    @Test
    fun `the frozen page carries the document's real panel aspect`() {
        val w = pxV2(".page", "width")
        val h = pxV2(".page", "height")
        val panel = SingleSheet8Panel()
        val frozenRatio = w / h
        val realRatio = panel.first / panel.second
        // The D-033 amendment. Before it this was 0.6503 against 0.7071 — eight percent out.
        assertEquals("the frozen page must depict the real panel's aspect", realRatio, frozenRatio, 0.001)
    }

    @Test
    fun `the derived keep-clear inset reproduces the frozen literal on the frozen page`() {
        val pageWidth = pxV2(".page", "width")
        val panelWidthPt = SingleSheet8Panel().first
        val derived = BenchStudio.keepClearInsetPx(pageWidth.toFloat(), panelWidthPt)
        val frozen = pxV2(".keepclear", "inset")

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
        val w = pxV2(".page", "width")
        val h = pxV2(".page", "height")
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

    /**
     * **P2: the resting form is absence, and both marks share one revealed alpha.**
     *
     * V2 gave the cue `.32` resting and `.9` warning — two states, both always painted. V2.1 rests both
     * the cue and the guide at `opacity:0` and reveals them together at `.85`. Derived from the freeze's
     * own combined selector rather than transcribed, so the two constants cannot drift apart from each
     * other or from the file that sets them.
     */
    @Test
    fun `the keep-clear and the guide rest at zero and share the one frozen revealed alpha`() {
        val reveal = Regex("""\.content\.focusing~\.keepclear\s*,\s*\.content\.focusing~\.guideV\s*\{[^}]*opacity\s*:\s*([\d.]+)""")
            .find(benchCss)!!.groupValues[1].toFloat()
        assertEquals("the cue takes the frozen revealed alpha", reveal, BenchStudio.KEEP_CLEAR_FOCUS_ALPHA, 1e-6f)
        assertEquals("so does the guide — it is one rule in the freeze", reveal, BenchStudio.GUIDE_ALPHA, 1e-6f)
        assertTrue("the cue's resting form is absence", rule(".keepclear").contains("opacity:0"))
        assertTrue("so is the guide's", rule(".guideV").contains("opacity:0"))
    }

    /**
     * **P2: the warn state is a departure the freeze does not contain — pinned as one.**
     *
     * Ruled by the owner on 2026-08-13 ([ADR-102 §12.9](../../../../../../../../docs/DECISIONS.md#adr-102-p2-marks)).
     * The measurement is the argument, so the measurement is the test: of the marks this surface can
     * draw, the warning is the **only** one that clears WCAG 1.4.11's 3:1, and both of the freeze's own
     * marks fail it. That is why the warning survived a freeze that has no warning.
     *
     * If the frozen file ever grows a `.keepclear.warn`, the first assertion fails — and the right
     * response is to re-read this ruling against it, not to delete the test.
     */
    @Test
    fun `the warn state is a documented departure, and it is the only mark on the sheet that clears 3 to 1`() {
        assertFalse(
            "the V2.1 freeze specifies no warn state; keeping one is the departure this test guards",
            benchCss.contains(".keepclear.warn"),
        )
        assertEquals(0.90f, BenchStudio.KEEP_CLEAR_WARN_ALPHA, 1e-6f)

        fun lum(x: Color): Double {
            fun ch(v: Float) = if (v <= 0.03928f) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
            return 0.2126 * ch(x.red) + 0.7152 * ch(x.green) + 0.0722 * ch(x.blue)
        }
        fun onPaper(c: ZinelyV21Colors, fg: Color, alpha: Float): Double {
            val bg = c.paper
            val mix = Color(
                red = fg.red * alpha + bg.red * (1 - alpha),
                green = fg.green * alpha + bg.green * (1 - alpha),
                blue = fg.blue * alpha + bg.blue * (1 - alpha),
            )
            val a = lum(mix)
            val b = lum(bg)
            return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
        }

        // **Both rooms, resolved through the island — not the light palette twice.** An earlier cut read
        // `zinelyV21LightColors()` alone, reasoning that the island makes the sheet's paper identical in
        // both themes. The paper, yes; a token the island does NOT light, no. `jamText` is not in the
        // island's set, so under that cut the warning painted the dark room's `#E4856D` on light paper at
        // 2.26:1 and the test said 4.96. This is why the loop runs over the room and not the palette.
        for (room in listOf(zinelyV21LightColors(), zinelyV21DarkColors())) {
            val c = BenchStudio.sheetIslandV21(room)
            assertTrue(
                "the warning must clear 1.4.11 in BOTH themes — it is the one mark carrying information " +
                    "available nowhere else",
                onPaper(c, c.jam, BenchStudio.KEEP_CLEAR_WARN_ALPHA) >= 3.0,
            )
            // The two accepted-as-decorative marks, pinned BELOW the floor so the acceptance stays
            // visible. Raising either above 3:1 is not a bug fix here — it would mean the D-064 question
            // was answered the other way, and this ruling has to be revisited rather than quietly
            // outgrown.
            assertTrue(
                "the frozen berry cue is accepted below the floor; if it clears 3:1 the ruling changed",
                onPaper(c, c.berry, BenchStudio.KEEP_CLEAR_FOCUS_ALPHA) < 3.0,
            )
            assertTrue(
                "the frozen butter guide is accepted below the floor; same",
                onPaper(c, c.butter, BenchStudio.GUIDE_ALPHA) < 3.0,
            )
        }
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

    /**
     * The guide's end-inset, now read from V2.1's `.guideV`. The value did not move — `8px` in both
     * freezes — but the *source* did, and leaving it pointed at V2 after P2 converted the line would
     * mean the one number still transcribed from a superseded file is the one nobody would think to
     * check. The revealed opacity is asserted with the cue's, in the shared-selector test above.
     */
    @Test
    fun `the guide stops the frozen 8px short at both ends`() {
        assertEquals(px(".guideV", "top").dp, BenchStudio.GuideEndInset)
        assertEquals(px(".guideV", "bottom").dp, BenchStudio.GuideEndInset)
    }

    /**
     * `.guideV{border-left:1.5px dashed var(--butter)}` — the three things P2 changed about the line,
     * asserted where they can fail. `butter`'s contrast is a ruled acceptance, not an oversight; the
     * measurement and the ruling live on [SnapGuides] and in ADR-102 §12.9.
     */
    @Test
    fun `the guide is butter, dashed, and the page's own border weight`() {
        val guide = rule(".guideV")
        assertTrue("V2.1 draws the guide in --butter (V2 was --matcha)", guide.contains("var(--butter)"))
        assertTrue("and dashes it, where V2 drew it solid", guide.contains("dashed"))
        assertEquals(
            "at the same 1.5px the page's own border takes",
            pxIn(guide, ".guideV", "border-left"),
            BenchStudio.PageBorder.value.toDouble(),
            0.0001,
        )
    }

    // -- the page number (row 1.11) ----------------------------------------------------------------

    @Test
    fun `the page number sits at the frozen offsets`() {
        // V2.1 seats the folio at the sheet's FOOT — V2 had it at `top:7px;right:10px`.
        assertEquals(px(".pagenum", "bottom").dp, BenchStudio.PageNumberBottomInset)
        assertEquals(px(".pagenum", "right").dp, BenchStudio.PageNumberEndInset)
    }

    /**
     * `.pagenum{font-size:.6rem;font-weight:700;font-variant-numeric:tabular-nums}`.
     *
     * The size is declared in **rem**, not px, so it is resolved against the prototype's root font size
     * rather than read as a pixel count: `.6rem × 16px = 9.6px`. Transcribing `.6` as `.6.sp` would be a
     * folio a sixteenth of its intended size, and the unit is the only thing that says so.
     *
     * V2's tracking is **gone**, replaced by weight and tabular figures — which is the change that
     * matters, since tabular figures are what stop the number shifting as the page count crosses a digit.
     */
    @Test
    fun `the page number is the frozen size, weight and figures`() {
        val rem = Regex("""font-size\s*:\s*([\d.]+)rem""").find(rule(".pagenum"))!!.groupValues[1].toDouble()
        assertEquals(rem * CSS_ROOT_PX, BenchStudio.PageNumberSize.value.toDouble(), 1e-6)
        assertTrue("the frozen folio is bold", rule(".pagenum").contains("font-weight:700"))
        assertTrue("the frozen folio is tabular", rule(".pagenum").contains("font-variant-numeric:tabular-nums"))
        assertTrue(
            "V2's `letter-spacing` is gone; do not reintroduce it",
            !rule(".pagenum").contains("letter-spacing"),
        )
    }

    /**
     * `.pagenum{color:var(--ink-soft)}` — **not `inkFaint`**, and that is a palette rule rather than a
     * preference. [ZinelyV21Colors.inkFaint] measures 3.04:1 on paper and sets **no text at all** in
     * V2.1; all 26 prototype uses of it as text, page numbers included, moved to `inkSoft`.
     */
    @Test
    fun `the page number takes ink-soft, because ink-faint sets no text in V2_1`() {
        assertTrue("the frozen folio is --ink-soft", rule(".pagenum").contains("var(--ink-soft)"))
        assertTrue("the folio must not be --ink-faint", !rule(".pagenum").contains("var(--ink-faint)"))
    }

    // The V2 assertion that used to live here pinned `.guide.v{width:1px}` against `ZinelyV2Dimens
    // .Hairline` and described it as "the stroke SnapGuides draws". P2 moved that stroke to
    // `BenchStudio.PageBorder` (1.5dp) and the assertion went on passing — it compared two V2 constants
    // to each other and had stopped describing the product. It is replaced by the width clause of
    // `the guide is butter, dashed, and the page's own border weight`, which reads V2.1's `.guideV`.

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
