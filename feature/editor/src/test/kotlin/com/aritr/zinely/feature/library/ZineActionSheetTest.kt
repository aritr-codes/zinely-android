package com.aritr.zinely.feature.library

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The frozen action sheet — its geometry, its type, its inks, its five rows, and its dismissal paths.
 *
 * **Two hosts, deliberately.** The dismissal and modality claims need the real [ZineActionSheet], window and
 * all; the geometry and pixel claims need a surface the decor-view raster can see, which a `Dialog`'s own
 * window is not — so those use [ZineActionSheetSurface] directly, exactly as V1's golden tests use
 * `ZSheetSurface`. Each test says which host it is on and why.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w480dp-h960dp", sdk = [28])
class ZineActionSheetTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val TITLE = "Sunday market"
        const val SUBTITLE = "A4 · 2 days ago"
        val TARGET = ZineActionTarget(TITLE, SUBTITLE)

        /** `.sheet{left:10px;right:10px;bottom:10px}` */
        const val INSET = 10

        /** `.sh-head{padding:17px 20px 13px}` · `.act{padding:15px 20px}` */
        const val HEAD_TOP = 17
        const val HEAD_SIDE = 20
        const val ROW_SIDE = 20

        /** `border-top:1px solid var(--hair)` · `.danger{border-top:8px solid var(--desk)}` */
        const val HAIRLINE = 1
        const val DANGER_BAND = 8

        /** `.sh-ttl{font-size:1.12rem}` — and **D-005**'s weight, not the file's 600. */
        val TITLE_SIZE = 17.92.sp

        /** `.sheet{border-radius:20px}` */
        const val CARD_RADIUS = 20

        const val HALF_PIXEL = 0.5f

        /** A ground no token carries, so "the sheet painted here" is unambiguous. */
        val PROBE_GROUND = Color(0xFF00FF00)

        /** Reference renderings for the type test. */
        const val REF_FROZEN = "ref-frozen"
        const val REF_HEAVY = "ref-600"
        const val REF_SMALL = "ref-12sp"

        /**
         * **Two** tofu controls, and the pair is the point.
         *
         * The first version of this used `U+E000`, on the reasoning that a Private Use Area codepoint is one
         * no font carries. That reasoning was wrong, and independent review proved it by rendering a
         * guaranteed-tofu glyph through the passing test: **the bundled Inter maps `U+E000` to a real glyph**
         * (id 1863, in all four weights). The control was a normal character, so `assertNotEquals` was
         * satisfied by any two glyphs that merely differ from each other.
         *
         * Both codepoints below were checked against every `cmap` subtable of all four bundled Inter weights
         * and are absent from every one, so each falls through to the platform's notdef. **Two are used
         * rather than one because the identity is the evidence:** distinct codepoints that render
         * *identically* can only be doing so because neither has a glyph, so that shared raster **is** the
         * tofu box — established by measurement rather than by assumption. One control can only be assumed.
         */
        const val TOFU_A = "\uE001"
        const val TOFU_B = "\uE05F"
    }

    private val chosen = mutableListOf<ZineAction>()
    private var dismissals = 0

    // ---------------------------------------------------------------------------------------------
    // Presence and dismissal — on the real sheet, window and all
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `there is no sheet until a zine is chosen`() {
        sheet(target = null)
        composeRule.onNodeWithTag(ZineActionSheetTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(ZineActionScrimTestTag).assertDoesNotExist()
    }

    @Test
    fun `tapping the scrim dismisses the sheet`() {
        sheet(TARGET)
        composeRule.onNodeWithTag(ZineActionScrimTestTag).performClick()
        // `document.getElementById('scrim').addEventListener('click',close)` — `:197`.
        assertEquals("the scrim is a dismissal path", 1, dismissals)
        assertEquals("and dismissing chooses nothing", emptyList<ZineAction>(), chosen)
    }

    @Test
    fun `the system back dismisses the sheet`() {
        sheet(TARGET)
        // The frozen file's `aria-modal="true"` (`:171`) is a behaviour, and Escape is its browser half.
        // Android's is system back, which the implementation guide authorises as platform truth the HTML
        // cannot express. A `Dialog` provides it — this asserts that it actually reaches `onDismiss`.
        composeRule.runOnUiThread { ShadowDialog.getLatestDialog()?.onBackPressed() }
        composeRule.waitForIdle()
        assertEquals("back must dismiss", 1, dismissals)
    }

    @Test
    fun `choosing an action reports it once and leaves the sheet standing`() {
        sheet(TARGET)
        composeRule.onNodeWithTag(zineActionTestTag(ZineAction.Rename)).performClick()

        assertEquals("the choice is reported", listOf(ZineAction.Rename), chosen)
        // **Deliberate, and the frozen file's own doing.** The `.act` buttons carry no handler at all
        // (`:195-209` wires the scrim and the `⋯`, nothing else), so what follows Rename is undesigned
        // here and every one of the five leads somewhere B5 owns. The sheet reports and holds still; a
        // self-dismissing sheet would be inventing the flow.
        assertEquals("and the sheet does not dismiss itself", 0, dismissals)
        composeRule.onNodeWithTag(ZineActionSheetTestTag).assertExists()
    }

    @Test
    fun `the sheet names itself to a screen reader`() {
        sheet(TARGET)
        composeRule.onNodeWithTag(ZineActionSheetTestTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, ZineActionSheetPaneTitle))
    }

    @Test
    fun `the sheet floats ten pixels clear of the two sides and the bottom`() {
        sheet(TARGET)
        val window = composeRule.onNodeWithTag(ZineActionScrimTestTag).fetchSemanticsNode().boundsInRoot
        val card = composeRule.onNodeWithTag(ZineActionSheetTestTag).fetchSemanticsNode().boundsInRoot

        // Measured against the scrim, which is `inset:0` — the frozen sheet's own coordinate space.
        assertEquals("${INSET}px in from the left", INSET.toFloat(), card.left - window.left, HALF_PIXEL)
        assertEquals("${INSET}px in from the right", INSET.toFloat(), window.right - card.right, HALF_PIXEL)
        assertEquals("${INSET}px up from the bottom", INSET.toFloat(), window.bottom - card.bottom, HALF_PIXEL)
        assertTrue(
            "and it is a card, not a full-height panel (measured ${card.height} of ${window.height})",
            card.height < window.height / 2f,
        )
    }

    @Test
    fun `the card's top corners are rounded away, not square`() {
        // `.sheet{border-radius:20px}`. No bound can see this — the node's rectangle is identical either
        // way — so it is read off the pixels: inside the arc there is no paper, and a few rows further down
        // the very same column is paper. The second half is the discriminator: without it, a sheet drawn
        // somewhere else entirely would pass the first.
        surface()
        val raster = decorRaster()
        val card = tagBounds(ZineActionSheetTestTag)
        val left = card.left.roundToInt()
        val top = card.top.roundToInt()

        // (3,3) from the corner is outside a 20px arc by six pixels, and outside the 1px hairline ring too.
        assertTrue(
            "the top-left corner must be cut away by the ${CARD_RADIUS}px radius",
            !raster.colourAt(left + 3, top + 3).closeTo(capturedPaper),
        )
        assertTrue(
            "and so must the top-right",
            !raster.colourAt(card.right.roundToInt() - 4, top + 3).closeTo(capturedPaper),
        )
        // The same column, below the arc — paper. This is what fails if the sheet simply is not there.
        assertTrue(
            "three pixels in, well below the corner, must be the sheet's own paper",
            raster.colourAt(left + 3, top + CARD_RADIUS * 2).closeTo(capturedPaper),
        )
        assertTrue(
            "and the corner's own diagonal must be paper once past the arc",
            raster.colourAt(left + CARD_RADIUS, top + CARD_RADIUS).closeTo(capturedPaper),
        )
    }

    // ---------------------------------------------------------------------------------------------
    // The five rows — on the surface, where they can be measured and sampled
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the five actions stand in the frozen order`() {
        surface()
        // `:173-177`. The order is the design's argument about consequence — Open first, Delete last and
        // apart — so it is asserted by placement rather than by the enum's own declaration order, which
        // would be the test asserting itself.
        val tops = ZineAction.entries.map { action ->
            action to composeRule.onNodeWithTag(zineActionTestTag(action))
                .fetchSemanticsNode().boundsInRoot.top
        }
        val expected = listOf(
            ZineAction.Open,
            ZineAction.ShareExport,
            ZineAction.Rename,
            ZineAction.Duplicate,
            ZineAction.Delete,
        )
        assertEquals(
            "the rows must be placed in the frozen order",
            expected,
            tops.sortedBy { it.second }.map { it.first },
        )
        // And each row *says* its frozen label — asserted against the file's own strings rather than
        // against `ZineAction.label`, which would be the enum agreeing with itself. Read as the control's
        // spoken name, because the row collapses to one node: the same string a screen reader hears is the
        // string that is printed, which is exactly the guarantee wanted here.
        listOf(
            "Open on the bench",
            "Share & export",
            "Rename",
            "Duplicate",
            "Delete",
        ).forEach { label ->
            // Twice over, because the two are different claims and either can fail alone: the row *says*
            // the label to a screen reader, and the row *prints* it.
            composeRule.onNode(hasContentDescription(label)).assertExists()
            composeRule.onNodeWithText(label, useUnmergedTree = true).assertExists()
        }
    }

    @Test
    fun `Delete is set apart by a band of the desk, and the others by a hairline`() {
        surface()
        val hairlines = ZineAction.entries.filterNot { it.danger }
        hairlines.forEach { action ->
            val band = composeRule.onNodeWithTag(zineActionSeparatorTestTag(action))
                .fetchSemanticsNode().boundsInRoot
            assertEquals(
                "${action.label} is separated by a ${HAIRLINE}px hairline",
                HAIRLINE.toFloat(),
                band.height,
                HALF_PIXEL,
            )
        }
        val danger = composeRule.onNodeWithTag(zineActionSeparatorTestTag(ZineAction.Delete))
            .fetchSemanticsNode().boundsInRoot
        assertEquals(
            "Delete is separated by the ${DANGER_BAND}px desk band, which is what makes it *apart* " +
                "rather than merely last",
            DANGER_BAND.toFloat(),
            danger.height,
            HALF_PIXEL,
        )
        // And it is the desk showing through, not a darker hairline: sampled in the middle of the band.
        val raster = decorRaster()
        assertTrue(
            "the band must be painted in the desk colour",
            raster.colourAt(danger.center.x.roundToInt(), danger.center.y.roundToInt())
                .closeTo(capturedDesk),
        )
    }

    @Test
    fun `every row is inset twenty pixels, header included`() {
        surface()
        val card = composeRule.onNodeWithTag(ZineActionSheetTestTag).fetchSemanticsNode().boundsInRoot
        val title = composeRule.onNodeWithTag(ZineActionTitleTestTag).fetchSemanticsNode().boundsInRoot

        assertEquals(
            "`.sh-head` is inset ${HEAD_SIDE}px",
            HEAD_SIDE.toFloat(),
            title.left - card.left,
            HALF_PIXEL,
        )
        assertEquals(
            "and opens ${HEAD_TOP}px down",
            HEAD_TOP.toFloat(),
            title.top - card.top,
            HALF_PIXEL,
        )

        // **The row's own parts are found in the unmerged tree, and that is not a workaround.** A row
        // collapses to one node in the merged tree — the shape that reaches TalkBack as a `Button` named by
        // its action, without announcing the decorative glyph beside it. The parts still exist underneath,
        // which is where a *geometry* question belongs anyway: a11y asks what a service hears, layout asks
        // where the ink went, and they are different trees on purpose.
        val row = composeRule.onNodeWithTag(zineActionTestTag(ZineAction.Open))
            .fetchSemanticsNode().boundsInRoot
        val glyph = composeRule
            .onNodeWithText(ZineAction.Open.glyph, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val label = composeRule
            .onNodeWithText(ZineAction.Open.label, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

        assertEquals(
            "`.act{padding:15px ${ROW_SIDE}px}` — the glyph slot starts ${ROW_SIDE}px in",
            ROW_SIDE.toFloat(),
            glyph.left - row.left,
            HALF_PIXEL,
        )
        assertEquals("`.ic{width:20px}`", 20f, glyph.width, HALF_PIXEL)
        assertEquals(
            "`gap:14px` between the icon slot and the label",
            14f,
            label.left - glyph.right,
            HALF_PIXEL,
        )
        // `padding:15px 20px` is symmetric top and bottom, so the label sits centred. An asymmetric
        // transcription — the header's own 17/13, say — passes every horizontal check above.
        assertEquals(
            "the label must sit centred between equal 15px paddings",
            row.center.y,
            label.center.y,
            1.5f,
        )
    }

    @Test
    fun `the header discloses exactly what the shelf withheld`() {
        surface()
        composeRule.onNodeWithText(TITLE).assertExists()
        composeRule.onNodeWithText(SUBTITLE).assertExists()
    }

    // ---------------------------------------------------------------------------------------------
    // Type and ink
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the sheet title is set in the voice face at the frozen size and D-005's weight`() {
        // Ink coverage, for the reason `ZineShelfTest` had to adopt it: `.sh-ttl` at 500 and at the file's
        // stale 600 differ by about a pixel of advance, so nothing about the node's width or height can
        // tell them apart — and the weight is precisely what **D-005** ruled, naming this selector.
        surfaceWithReferences()
        val raster = decorRaster()
        val threshold = inkThreshold(capturedPaper)

        val frozen = raster.inkCoverage(tagBounds(REF_FROZEN), threshold)
        val heavier = raster.inkCoverage(tagBounds(REF_HEAVY), threshold)
        val smaller = raster.inkCoverage(tagBounds(REF_SMALL), threshold)
        val actual = raster.inkCoverage(tagBounds(ZineActionTitleTestTag), threshold)

        assertTrue("nothing was drawn at the frozen style", frozen > 0)
        assertTrue(
            "this host cannot tell Fraunces 500 from the file's stale 600 ($frozen vs $heavier), " +
                "so D-005 would be unguarded here",
            relativeGap(frozen, heavier) > 0.03f,
        )
        assertTrue(
            "this host cannot tell ${TITLE_SIZE} from the subtitle's 12.48sp ($frozen vs $smaller)",
            relativeGap(frozen, smaller) > 0.03f,
        )
        assertTrue(
            "the title must render at Fraunces 500 / $TITLE_SIZE — coverage $actual against $frozen " +
                "frozen, $heavier at 600, $smaller at 12.48sp",
            relativeGap(actual, frozen) <= 0.03f,
        )
    }

    @Test
    fun `the subtitle is printed in the faintest ink and the title in the full one`() {
        surface()
        val raster = decorRaster()
        // Three tokens are plausible here and two of them are wrong: `.sh-sub{color:var(--ink-faint)}`
        // while the title takes plain `--ink`. Swapping either composes perfectly and reads as a choice.
        assertTrue(
            "some pixel of the subtitle must be exactly inkFaint",
            raster.anyPixel(tagBounds(ZineActionSubtitleTestTag), capturedInkFaint),
        )
        assertTrue(
            "and some pixel of the title exactly ink",
            raster.anyPixel(tagBounds(ZineActionTitleTestTag), capturedInk),
        )
        assertNotEquals("the two inks must differ, or neither assertion discriminates", capturedInk, capturedInkFaint)
    }

    @Test
    fun `Delete is printed in the consequence ink, and the other four are not`() {
        surface()
        val raster = decorRaster()
        assertTrue(
            "`.act.danger{color:var(--consequence)}`",
            raster.anyPixel(tagBounds(zineActionTestTag(ZineAction.Delete)), capturedConsequence),
        )
        ZineAction.entries.filterNot { it.danger }.forEach { action ->
            assertTrue(
                "${action.label} must not be printed in the consequence ink",
                !raster.anyPixel(tagBounds(zineActionTestTag(action)), capturedConsequence),
            )
        }
    }

    // ---------------------------------------------------------------------------------------------
    // D-021 — the icons are characters, and half of them are not in the app's own font
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `every frozen glyph draws a real mark, not a tofu box`() {
        surfaceWithReferences()
        val raster = decorRaster()

        // **This is the register entry, measured.** `.ic` holds a literal character, and of the six the
        // design uses — the five action icons plus the shelf's `\u22EF` — only `\u2197`, `\u21EA` and `\u232B` are in
        // the bundled Inter. `\u270E`, `\u29C9` and `\u22EF` come from whatever the platform falls back to, and a
        // platform with no glyph at all draws the tofu box. Ink alone cannot tell the two apart: the tofu
        // box is a rectangle and paints *more* ink than a thin ellipsis does. So each glyph is compared
        // against the tofu box pixel for pixel.
        val threshold = inkThreshold(capturedPaper)
        val tofuA = raster.signature(tagBounds("glyph-$TOFU_A"), threshold)
        val tofuB = raster.signature(tagBounds("glyph-$TOFU_B"), threshold)

        // **The positive control, and the assertion this test previously lacked.** Two codepoints with no
        // glyph in any bundled weight must render *the same thing*, because that thing is the notdef. If
        // they differ, at least one resolved to a real glyph and the comparison below would be meaningless
        // — which is exactly the state review found this test shipped in.
        assertTrue("the tofu control must draw something, or it discriminates nothing", tofuA.any { it })
        assertEquals(
            "two codepoints absent from every bundled weight must render identically — if they do not, " +
                "one of them has a glyph and is not a tofu control at all",
            tofuA,
            tofuB,
        )
        assertTrue(
            "and the tofu box must not be a solid block — a threshold that called the whole cell ink " +
                "would make every glyph equal to it (which is what the first run of this test did)",
            tofuA.count { it } < tofuA.size / 2,
        )

        val glyphs = ZineAction.entries.map { it.glyph } + listOf("\u22EF")
        glyphs.forEach { glyph ->
            val drawn = raster.signature(tagBounds("glyph-$glyph"), threshold)
            assertTrue("'$glyph' drew nothing at all", drawn.any { it })
            assertNotEquals(
                "'$glyph' rendered as the tofu box — the platform has no glyph for it (D-021)",
                tofuA,
                drawn,
            )
        }
    }

    @Test
    fun `the scrim dims a dark shelf harder than a light one, which is the D-022 ruling`() {
        // **D-022, ruled: the corpus wins.** `v2-library.html:119` writes `rgba(30,25,18,.36)` as a hard
        // literal outside its own `:root`, so the frozen dark block could never reach it and both themes
        // dimmed identically — over a dark desk that is already close to that colour. The owner ruled the
        // canonical `--scrim` authoritative, as for the serif (D-005) and the easings (D-011). So this test
        // is the inverse of the one B3 first shipped: what was pinned as sameness is now asserted as
        // difference, and the earlier version is what the ruling flipped.
        val (light, dark) = scrimPixels()

        assertNotEquals(
            "the ruled scrim is theme-aware — if these are equal, the Library's stale literal is back",
            light,
            dark,
        )
        // Not merely different: the published values, composited the way the raster composited them. The
        // dark half is deliberately the *stronger* wash, which is the whole substance of the ruling.
        assertTrue(
            "light must be the corpus ink at .34 over white — expected ${LIGHT_SCRIM.compositeOverWhite()}, found $light",
            light.closeTo(LIGHT_SCRIM.compositeOverWhite()),
        )
        assertTrue(
            "dark must be the corpus black at .50 over white — expected ${DARK_SCRIM.compositeOverWhite()}, found $dark",
            dark.closeTo(DARK_SCRIM.compositeOverWhite()),
        )
        assertTrue(
            "and the dark scrim must dim harder than the light one, or the ruling's point is lost " +
                "(light luminance ${light.luminance()}, dark ${dark.luminance()})",
            dark.luminance() < light.luminance(),
        )
    }

    @Test
    fun `the stale Library literal is not what the sheet paints`() {
        // The control for the ruling. Without it, any theme-aware pair of colours would satisfy the test
        // above; this names the specific value that was rejected, so a revert to it fails loudly rather
        // than quietly re-opening a closed defect.
        val (light, _) = scrimPixels()
        assertFalse(
            "the frozen Library's rgba(30,25,18,.36) was ruled stale and must not be painted",
            light.closeTo(STALE_LIBRARY_SCRIM.compositeOverWhite()),
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Harness
    // ---------------------------------------------------------------------------------------------

    /** The real component, `Dialog` and all — for behaviour, dismissal and modality. */
    private fun sheet(target: ZineActionTarget?) {
        composeRule.setContent {
            Host(dark = false) {
                ZineActionSheet(
                    target = target,
                    onAction = { chosen += it },
                    onDismiss = { dismissals++ },
                )
            }
        }
        composeRule.waitForIdle()
    }

    /** The sheet's body in a plain host — for geometry and pixels the decor raster must be able to see. */
    private fun surface() {
        composeRule.setContent {
            Host(dark = false) {
                Box(Modifier.align(Alignment.BottomCenter)) {
                    ZineActionSheetSurface(target = TARGET, onAction = { chosen += it })
                }
            }
        }
        composeRule.waitForIdle()
    }

    /** The surface plus the yardsticks the type and glyph tests measure against. */
    private fun surfaceWithReferences() {
        composeRule.setContent {
            Host(dark = false) {
                Box(Modifier.align(Alignment.BottomCenter)) {
                    ZineActionSheetSurface(target = TARGET, onAction = { chosen += it })
                }
                // **On the sheet's own paper, not on the probe ground.** Ink coverage counts pixels darker
                // than a threshold, and how many a glyph's antialiased edge contributes depends on what is
                // behind it — references measured over a mid-luminance green and a title measured over
                // near-white paper differ by a fifth for that reason alone, which is what the first run of
                // this test reported.
                Column(
                    Modifier
                        .align(Alignment.TopStart)
                        .background(ZinelyTheme.v2Colors.paper),
                ) {
                    Reference(REF_FROZEN, FontWeight.Medium, TITLE_SIZE, TITLE)
                    Reference(REF_HEAVY, FontWeight.SemiBold, TITLE_SIZE, TITLE)
                    Reference(REF_SMALL, FontWeight.Medium, 12.48.sp, TITLE)
                }
                Column(
                    Modifier
                        .align(Alignment.TopEnd)
                        .background(ZinelyTheme.v2Colors.paper),
                ) {
                    (ZineAction.entries.map { it.glyph } + listOf("⋯", TOFU_A, TOFU_B)).forEach { glyph ->
                        GlyphCell(glyph)
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Composable
    private fun Reference(tag: String, weight: FontWeight, size: TextUnit, text: String) {
        Text(
            text = text,
            modifier = Modifier.testTag(tag),
            style = TextStyle(
                fontFamily = ZinelyTheme.v2Typography.voice,
                fontWeight = weight,
                fontSize = size,
                color = ZinelyTheme.v2Colors.ink,
            ),
        )
    }

    /** One glyph in a fixed cell, so every signature is compared over the same box. */
    @Composable
    private fun GlyphCell(glyph: String) {
        Box(Modifier.size(GLYPH_CELL).testTag("glyph-$glyph")) {
            Text(
                text = glyph,
                style = TextStyle(
                    fontFamily = ZinelyTheme.v2Typography.work,
                    fontSize = 16.sp,
                    color = ZinelyTheme.v2Colors.ink,
                ),
            )
        }
    }

    private var capturedInk: Color = Color.Unspecified
    private var capturedInkFaint: Color = Color.Unspecified
    private var capturedConsequence: Color = Color.Unspecified
    private var capturedDesk: Color = Color.Unspecified
    private var capturedPaper: Color = Color.Unspecified

    @Composable
    private fun Host(dark: Boolean, content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
        ZinelyTheme(darkTheme = dark) {
            capturedInk = ZinelyTheme.v2Colors.ink
            capturedInkFaint = ZinelyTheme.v2Colors.inkFaint
            capturedConsequence = ZinelyTheme.v2Colors.consequence
            capturedDesk = ZinelyTheme.v2Colors.desk
            capturedPaper = ZinelyTheme.v2Colors.paper
            Box(Modifier.fillMaxSize().background(PROBE_GROUND)) { content() }
        }
    }

    /**
     * Both themes' scrims in **one** composition, over the same white ground.
     *
     * `setContent` may be called once per test, and the claim needs the two side by side anyway: the
     * question is whether they differ, and a single composition removes any doubt that they were rendered
     * under identical conditions.
     */
    private fun scrimPixels(): Pair<Color, Color> {
        composeRule.setContent {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                ZinelyTheme(darkTheme = false) {
                    ScrimProbe("scrim-light", Alignment.TopStart)
                }
                ZinelyTheme(darkTheme = true) {
                    ScrimProbe("scrim-dark", Alignment.BottomEnd)
                }
            }
        }
        composeRule.waitForIdle()
        val raster = decorRaster()
        fun probe(tag: String): Color {
            val at = tagBounds(tag)
            return raster.colourAt(at.center.x.roundToInt(), at.center.y.roundToInt())
        }
        return probe("scrim-light") to probe("scrim-dark")
    }

    /**
     * A patch of the **production** scrim.
     *
     * It composes [ZineActionScrim] rather than a `Box` filled from a local copy of the literal, and that is
     * the whole point of the shape: the first version of this test painted its own patch, so swapping the
     * sheet's paint for a theme-aware token left it green — a test agreeing with a constant while production
     * did something else. The clipping `Box` is only there to give the fullscreen scrim a samplable patch.
     */
    @Composable
    private fun ScrimProbe(tag: String, corner: Alignment) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.align(corner).size(PROBE_PATCH).testTag(tag)) {
                ZineActionScrim(onDismiss = {})
            }
        }
    }

    private fun tagBounds(tag: String): Rect =
        composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private fun decorRaster(): Bitmap {
        assertEquals(
            "these pixel offsets assume dp == px; density was ${composeRule.density.density}",
            1.0f,
            composeRule.density.density,
            0.0001f,
        )
        return composeRule.activity.window.decorView.rasterizeToBitmap()
    }

    private fun Bitmap.colourAt(x: Int, y: Int): Color = Color(getPixel(x, y))

    private fun Bitmap.signature(region: Rect, threshold: Float): List<Boolean> {
        val out = mutableListOf<Boolean>()
        for (dy in 0 until GLYPH_CELL_PX) {
            for (dx in 0 until GLYPH_CELL_PX) {
                val x = region.left.roundToInt() + dx
                val y = region.top.roundToInt() + dy
                out += if (x in 0 until width && y in 0 until height) {
                    colourAt(x, y).luminance() < threshold
                } else {
                    false
                }
            }
        }
        return out
    }

    /** Halfway between the ground and the ink — the only threshold that means the same thing on any ground. */
    private fun inkThreshold(ground: Color): Float = (ground.luminance() + capturedInk.luminance()) / 2f

    private fun Bitmap.inkCoverage(region: Rect, threshold: Float): Int {
        var covered = 0
        for (y in region.top.roundToInt() until region.bottom.roundToInt().coerceAtMost(height)) {
            for (x in region.left.roundToInt() until region.right.roundToInt().coerceAtMost(width)) {
                if (x >= 0 && y >= 0 && colourAt(x, y).luminance() < threshold) covered++
            }
        }
        return covered
    }

    private fun Bitmap.anyPixel(region: Rect, expected: Color): Boolean {
        for (y in region.top.roundToInt() until region.bottom.roundToInt().coerceAtMost(height)) {
            for (x in region.left.roundToInt() until region.right.roundToInt().coerceAtMost(width)) {
                if (x >= 0 && y >= 0 && colourAt(x, y).closeTo(expected)) return true
            }
        }
        return false
    }

    private fun Color.closeTo(other: Color): Boolean {
        val tolerance = 1.5f / 255f
        return abs(red - other.red) <= tolerance &&
            abs(green - other.green) <= tolerance &&
            abs(blue - other.blue) <= tolerance
    }

    /** The scrim's literal has alpha; the raster does not. Composite it the way the raster did. */
    private fun Color.compositeOverWhite(): Color = Color(
        red = red * alpha + (1f - alpha),
        green = green * alpha + (1f - alpha),
        blue = blue * alpha + (1f - alpha),
    )

    private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

    private fun relativeGap(a: Int, b: Int): Float {
        val larger = maxOf(a, b)
        return if (larger == 0) 0f else abs(a - b).toFloat() / larger
    }
}

/**
 * The **corpus** scrim, `--scrim` — `rgba(42,37,30,.34)` light and `rgba(0,0,0,.5)` dark
 * ([`v2-bench.html:21,35`](docs/design/mockups/v2-bench.html)), which the **D-022** ruling made authoritative
 * over the Library's own literal.
 *
 * Written out here rather than read from `ZinelyV2Colors` on purpose: a test that took the token would agree
 * with whatever the token said, including a wrong value. These are the bytes the ruling names.
 */
private val LIGHT_SCRIM = Color(0xFF2A251E).copy(alpha = 0.34f)
private val DARK_SCRIM = Color(0xFF000000).copy(alpha = 0.50f)

/** What the ruling rejected — pinned so a revert to it is a failure rather than a silence. */
private val STALE_LIBRARY_SCRIM = Color(0xFF1E1912).copy(alpha = 0.36f)

/** A patch big enough to sample away from any edge. */
private val PROBE_PATCH = 40.dp

/** The glyph probe's cell, in dp and in px — density is pinned to 1, and asserted to be. */
private val GLYPH_CELL = 28.dp
private const val GLYPH_CELL_PX = 28
