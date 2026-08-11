package com.aritr.zinely.feature.library

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.down
import androidx.compose.ui.test.up
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.ui.a11y.PlatformA11yNode
import com.aritr.zinely.ui.a11y.platformNode
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.ZinelyV2Dimens
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The dock — the band, what it lets through, and the one button standing in it.
 *
 * **The band is the part that fails silently.** A dock is four values (two paddings, a gradient stop and a
 * direction) and three of the four wrong readings compose perfectly and screenshot plausibly: a gradient
 * run the other way looks like a deliberate scrim, an 80% stop read as 20% looks like a softer fade, and a
 * band that eats touches looks exactly like a band that does not until a user tries to reach the bottom
 * row of covers. So each is a **rasterised pixel** or a **driven gesture**, never a read of the source.
 *
 * `sdk = [28]` is the same choice [ZineShelfTest] makes and for the same reason: the dock's own colours are
 * flat, and pinning the SDK keeps the platform's font and blend behaviour out of assertions that are about
 * geometry. The dock draws no grain, so nothing here is dimmed by that choice — the illustrations that do
 * are covered in [ZineShelfEmptyTest] and by the goldens.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w480dp-h960dp", sdk = [28])
class ZineDockTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val UNDER = "under-the-dock"
        const val REF_LABEL = "ref-label"
        const val REF_ROW = "ref-row"
        const val REF_PLUS = "ref-plus"

        const val LABEL = "Make a zine"

        /** `.dock{padding:var(--gap-lg) var(--gap-lg) var(--gap-xl)}` — 16 / 16 / 24. */
        const val PAD_TOP = 16
        const val PAD_SIDE = 16
        const val PAD_BOTTOM = 24

        /** `.start{padding:var(--gap-lg) var(--gap-xl);border-radius:var(--br-pill)}`. */
        const val START_PAD_VERTICAL = 16
        const val START_PAD_HORIZONTAL = 24

        /**
         * `.start{border:1.5px solid var(--ink)}` — V2's button had **no border at all**, and that is
         * why three raster probes in this file had to move rather than be re-baselined. A probe one
         * pixel inside the bounds used to land on the fill; it now lands on the ink edge, and reads as
         * "the fill is missing" when the fill is exactly where it should be. The probes step past the
         * border and one antialiased pixel beyond it. `Modifier.border` paints *inside* the bounds, so
         * this adds nothing to the button's measured size — only to where its fill starts.
         */
        const val START_BORDER_PROBE = 3

        /**
         * `linear-gradient(to top,var(--desk) 58%,transparent)`, as a fraction measured **downward**.
         *
         * CSS measures `to top` from the bottom edge, so the frozen stop 58% up is 42% down. V2's band
         * stopped at 80% up (20% down); re-baselining the number without inverting it would put the solid
         * in the wrong half and still read as a plausible band.
         */
        const val FADE_STOP_DOWNWARD = 0.42f

        /** `.start{font-size:1rem}` and `.start .plus{font-size:1.15rem;line-height:1}`. */
        const val LABEL_SIZE = 16f
        const val PLUS_SIZE = 18.4f

        /** `.start:active{transform:translate(2px,2px)}` on `transition:transform .14s`. */
        const val PRESS_TRANSLATION = 2
        const val PRESS_DURATION = 140L

        /** V2's `.start{border-radius:16px}`, kept only as the foil the pill test measures against. */
        const val START_RADIUS_RECTANGLE = 16

        /** `.start:focus-visible{outline:2px solid var(--ink);outline-offset:5px}`. */
        const val RING_OFFSET = 5

        /**
         * How far left of the button's own box to sweep for the decorative `--frame` ring.
         *
         * `ZinelyV21Dimens.frameRing` is 5dp and it stacks outside a 4dp hard shadow, so the ring's
         * band starts within ~9px of the edge. Swept rather than probed at one x: the ring is drawn on
         * a rounded shape and the exact first painted column depends on antialiasing at the corner
         * radius, which is not what this test is about.
         */
        const val RING_SEARCH = 12

        /** Narrower than the button wants, so the frozen side padding becomes the binding constraint. */
        const val NARROW_BAND = 120

        /** Room enough that the reference plus cannot be clipped by its own box. */
        const val REF_PLUS_BOX = 48

        /** Half a pixel, not one — see [ZineShelfTest.HALF_PIXEL]'s note. */
        const val HALF_PIXEL = 0.5f

        /** Tighter than half a pixel, for the one claim here whose whole effect *is* a pixel. */
        const val SUB_PIXEL = 0.33f

        /** Rows to skip at a fill's own edge, where antialiasing sits — see [Bitmap.inkCentroidY]. */
        const val EDGE_INSET = 6

        /** A ground no token carries, so "the dock painted over it" is unambiguous. */
        val PROBE_GROUND = Color(0xFF00FF00)
    }

    // ---------------------------------------------------------------------------------------------
    // The band — `linear-gradient(to top,var(--desk) 80%,transparent)` and `pointer-events:none`
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the fade is above the solid, not below it`() {
        dock()
        val band = bounds(ZineDockTestTag)
        val raster = decorRaster()
        // Sampled near the band's edge, **not** down its centre: the button's own
        // `0 16px 30px -12px` shadow tints the desk directly below it, so a centre column reads #E5DDCB
        // where the gradient alone would give #ECE3D1. A first draft sampled the centre and failed on a
        // shadow it had itself specified — B3's "ground outside an object is not clean", one package on.
        // The gradient is uniform horizontally, so an edge column is the same band with nothing over it.
        val x = band.left.roundToInt() + 5

        val top = raster.colourAt(x, band.top.roundToInt() + 1)
        val bottom = raster.colourAt(x, band.bottom.roundToInt() - 1)

        // CSS measures `to top` from the bottom edge and `Brush.verticalGradient` from the top, so the
        // frozen stop at 80% *up* is a stop at 20% *down*. Written the CSS way round the band dissolves at
        // the button and is opaque where the covers scroll past — the exact inverse of the frozen
        // comment's intent, and symmetrical enough to look deliberate in a screenshot.
        //
        // The top row is *nearly* ground rather than exactly it, and that is the gradient working: one
        // pixel into a 152px band is 3% of the way to the 20% stop, so ~5% of the desk is already there.
        // Asserted as "far closer to the ground than to the desk", which is the claim — a first draft
        // demanded an exact ground match and failed on the ramp it was written to prove existed.
        assertTrue(
            "the top of the dock must still be mostly ground (found $top against desk $capturedDesk)",
            distance(top, PROBE_GROUND) < distance(top, capturedDesk) / 4f,
        )
        assertTrue(
            "the foot of the dock must be solid desk (found $bottom)",
            raster.matches(x, band.bottom.roundToInt() - 1, capturedDesk),
        )
        assertNotEquals("a band with no gradient at all would pass neither edge", top, bottom)
    }

    @Test
    fun `the desk is solid from 58 percent of the way up`() {
        dock()
        val band = bounds(ZineDockTestTag)
        val raster = decorRaster()
        // The edge column again, clear of the button's shadow — see the test above.
        val x = band.left.roundToInt() + 5

        // The stop, read as a boundary rather than as a colour: the first row from the top that is fully
        // desk. V2 stopped at 80% up; V2.1 stops at **58%** up, which is [FADE_STOP_DOWNWARD] measured
        // downward — and inverting the fraction is the half of this re-baseline that is easy to miss,
        // because 0.58 downward is also a number and also produces a band.
        val firstSolid = (band.top.roundToInt()..band.bottom.roundToInt() - 1)
            .first { raster.matches(x, it, capturedDesk) }
        val expected = band.top + FADE_STOP_DOWNWARD * band.height

        assertEquals(
            "the solid must begin ${FADE_STOP_DOWNWARD * 100}% down the band, which is the frozen 58% up",
            expected,
            firstSolid.toFloat(),
            1f,
        )
        // And the fade above it must actually be a fade — a two-stop step at the same place would satisfy
        // the boundary above while looking nothing like the frozen band.
        val quarterWay = (band.top + FADE_STOP_DOWNWARD / 2f * band.height).roundToInt()
        assertFalse(
            "the region above the stop must be partly transparent, not a hard edge",
            raster.matches(x, quarterWay, capturedDesk) ||
                raster.matches(x, quarterWay, PROBE_GROUND),
        )
    }

    @Test
    fun `the dock does not swallow touches meant for the shelf`() {
        var underneath = 0
        composeRule.setContent {
            Host {
                Box(
                    Modifier
                        .testTag(UNDER)
                        .fillMaxSize()
                        .clickable { underneath++ },
                )
                ZineDock(onStart = {}, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
        composeRule.waitForIdle()

        // `pointer-events:none` on `.dock`, `pointer-events:auto` on `.start` only. The band covers the
        // bottom ~150px of the shelf, so a dock that consumed touches would make the last row of covers
        // unreachable through a region that looks like empty desk — a defect no raster shows and no
        // callback test finds, because the callback that never fires is the *other* component's.
        val band = bounds(ZineDockTestTag)
        val point = Offset(band.center.x, band.top + 4f)
        composeRule.onNodeWithTag(UNDER).performTouchInput { click(point) }
        composeRule.waitForIdle()

        assertEquals("the touch must reach what is under the band", 1, underneath)
    }

    // ---------------------------------------------------------------------------------------------
    // Placement — the button in the band
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the button stands in the frozen padding, and the fade is above it`() {
        dock()
        val band = bounds(ZineDockTestTag)
        val button = bounds(ZineStartTestTag)

        assertEquals(
            "the band must open ${PAD_TOP}px above the button",
            PAD_TOP.toFloat(),
            button.top - band.top,
            HALF_PIXEL,
        )
        assertEquals(
            "and close ${PAD_BOTTOM}px below it",
            PAD_BOTTOM.toFloat(),
            band.bottom - button.bottom,
            HALF_PIXEL,
        )
        // `justify-content:center`.
        assertEquals("the button must be centred in the band", band.center.x, button.center.x, HALF_PIXEL)
    }

    @Test
    fun `the side padding bounds the button when the band is too narrow for it`() {
        // The frozen `20px` is unobservable at any width the button fits in — it is the *limit* on a
        // centred child, not a position. A first version asserted `button.left >= band.left + 20`, which
        // at 480dp reduces to `165 >= 19.5`: true for a side padding anywhere from 0 to ~160dp, so the one
        // named test of this value gated nothing. Narrowing the band until the padding binds is what makes
        // it a measurement — the button then sits exactly 20px in, and 0px in if the padding is dropped.
        composeRule.setContent {
            Host {
                ZineDock(
                    onStart = {},
                    modifier = Modifier.align(Alignment.BottomCenter).width(NARROW_BAND.dp),
                )
            }
        }
        composeRule.waitForIdle()

        val band = bounds(ZineDockTestTag)
        val button = bounds(ZineStartTestTag)
        assertEquals(
            "a band too narrow for the button must still hold the frozen ${PAD_SIDE}px at the left",
            PAD_SIDE.toFloat(),
            button.left - band.left,
            HALF_PIXEL,
        )
        assertEquals(
            "and at the right",
            PAD_SIDE.toFloat(),
            band.right - button.right,
            HALF_PIXEL,
        )
        // The discrimination, measured rather than argued: the padding is only binding if the button would
        // otherwise have been wider than the room left for it.
        assertTrue(
            "the band must actually be narrower than the button wants, or the padding never binds",
            button.width >= NARROW_BAND - 2 * PAD_SIDE - HALF_PIXEL,
        )
    }

    @Test
    fun `the button is its tallest child plus the frozen padding`() {
        dockWithReferences()
        val button = bounds(ZineStartTestTag)
        val label = bounds(REF_LABEL)
        val plus = bounds(REF_PLUS)

        // **The V2 test asked a question this harness cannot answer, and re-baselining its number would
        // have hidden that.** V2 wrote `.plus{line-height:0}`, so its row was the label alone and the
        // test asserted `label + 15 + 15`. V2.1 writes `.plus{line-height:1}` against a label inheriting
        // `body{line-height:1.55}` — 18.4px under 24.8px — so on a device the row is still the label's
        // line box, for the opposite reason.
        //
        // ⚠️ **`lineHeight` is not observable here.** Measured, not assumed: the reference was run at
        // `lineHeight = 40.sp` and laid out 19px, because Robolectric's text layout ignores the
        // line-height span. So in this harness the plus — in the Voice face, whose metrics are deeper —
        // is the taller child, and no JVM assertion can gate the frozen line boxes at all.
        //
        // What *is* gated is the padding, which is the value that actually moved: `var(--gap-lg)` where
        // V2 wrote 15px. It is gated against whichever child the row resolves to, so this assertion
        // states the same thing on a device as it does here. The line boxes are a **golden and device
        // parity** claim; that is said rather than left to look like coverage.
        assertEquals(
            "the button must be its tallest child (label ${label.height}px, plus ${plus.height}px) plus " +
                "the frozen ${START_PAD_VERTICAL}px top and bottom — the 1.5px border paints inside the " +
                "bounds and adds nothing to the measured height",
            maxOf(label.height, plus.height) + 2 * START_PAD_VERTICAL,
            button.height,
            HALF_PIXEL,
        )
    }

    @Test
    fun `the plus sits on the row's centre, because V2_1 gave it no margin to lift it`() {
        dockWithReferences()
        // **This test asserts the opposite of what it asserted in V2, and that is the whole point.**
        //
        // V2 wrote `.start .plus{line-height:0;margin-top:-2px}`, and because a flex item is centred on its
        // *margin* box, the frozen −2px lifted the glyph by exactly **1px**. V2.1 writes
        // `.start .plus{font-family:var(--voice);font-size:1.15rem;font-weight:700;line-height:1}` — no
        // margin at all. So the plus must now sit level with an unmargined reference, and re-baselining the
        // old expectation to a new number would have kept a lift the corpus no longer asks for.
        //
        // Measured as an offset from each container's own centre, so the glyph's metrics cancel. A
        // fullwidth plus is not centred on its em box — it sits on the maths axis — so no absolute
        // prediction of its ink is safe. The reference renders the same glyph at the same size with no
        // margin; production must sit on top of it. Nothing inside the button can carry a test tag (the A8
        // seam clears descendant semantics), so the live plus is found by pixel: the first ink run inside
        // the button, left of the label.
        val raster = decorRaster()
        val button = bounds(ZineStartTestTag)
        val reference = bounds(REF_ROW)

        // Each row is measured against **itself**: the plus's centroid less the label's, in the same row.
        // The label carries no margin in either, so the difference between the two differences is any
        // remaining lift and nothing else — no container geometry survives into the comparison.
        val live = raster.plusOverLabel(button)
        val flat = raster.plusOverLabel(reference)

        assertEquals(
            "the production plus must sit level with an unmargined one (live $live against reference $flat)",
            flat,
            live,
            SUB_PIXEL,
        )
        // The discrimination: the tolerance is a third of a pixel, so re-introducing V2's `margin-top:-2px`
        // — a 1px lift — fails this. Without that, "level" would be satisfied by any margin small enough to
        // round away, and the assertion would certify nothing.
        assertTrue(
            "a tolerance of $SUB_PIXEL must be able to see V2's 1px lift",
            SUB_PIXEL < 1f,
        )
    }

    @Test
    fun `the button clears the 48dp touch floor`() {
        dock()
        // D-009 — the register's open touch-target entry, whose ruling is explicit that the *visual* design
        // must not be changed to satisfy it. The claim worth making is therefore that the frozen padding
        // clears the floor **on its own**, and the touch bounds cannot carry that claim: `zinelyV2Control`
        // opens with `minimumInteractiveComponentSize()`, so `touchBoundsInRoot` is >= 48dp for any control
        // through the seam whatever its padding. Review's finding — the first version asserted the touch
        // box and the ADR read it as evidence about the design. The *layout* bounds are the honest measure.
        val floor = ZinelyV2Dimens.MinTouchTarget.value
        val laidOut = bounds(ZineStartTestTag)
        assertTrue(
            "the frozen 15px padding around 16px type must clear ${floor}dp unaided; it drew " +
                "${laidOut.height}px",
            laidOut.height >= floor,
        )
        assertTrue(
            "and the frozen 26px padding around the label must too; it drew ${laidOut.width}px",
            laidOut.width >= floor,
        )
        // The seam's own floor is still asserted, because it is the guarantee that survives a design change.
        val touch = composeRule.onNodeWithTag(ZineStartTestTag).fetchSemanticsNode().touchBoundsInRoot
        assertTrue("the touch height was ${touch.height}, below $floor", touch.height >= floor)
        assertTrue("the touch width was ${touch.width}, below $floor", touch.width >= floor)
    }

    // ---------------------------------------------------------------------------------------------
    // Ink and glyph
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the button is leaf and its label is on-leaf, by day`() = assertButtonInk(dark = false)

    @Test
    fun `the button is leaf and its label is on-leaf, by night`() = assertButtonInk(dark = true)

    /**
     * `.start{background:var(--leaf);color:var(--on-leaf)}`, read off the raster.
     *
     * **This is the assertion D-023 turned on, and it now points the other way.** V2's Library wrote
     * `color:var(--paper)` on a matcha fill while the Bench and the Proof wrote `--on-matcha`, and this
     * test held the Library's reading by requiring the corpus's token to be *absent*. V2.1 writes
     * `--on-leaf`, so the defect is closed by the re-freeze rather than by a ruling, and the forbidden
     * colour is V2's `--paper`.
     *
     * Both tokens are theme-aware — so both are read from `ZinelyTheme.v21Colors` rather than written as
     * literals. A first draft of the V2 test asserted white in both themes and would have been inert in
     * dark, where the corpus's own label is nearly black: it would have passed on the exact substitution
     * it existed to catch. The same trap applies inverted, which is why the two are also asserted to
     * differ in each theme before the exclusion is trusted.
     *
     * Two tests rather than a loop over both themes: the Compose rule accepts one `setContent` per test,
     * which a first draft learned by throwing on the second call.
     */
    private fun assertButtonInk(dark: Boolean) {
        dock(dark = dark)
        val button = bounds(ZineStartTestTag)
        val raster = decorRaster()
        val found = raster.coloursIn(button)

        assertTrue(
            "the fill must be --leaf ($capturedLeaf) in ${theme(dark)}",
            found.any { it.closeTo(capturedLeaf) },
        )
        assertTrue(
            "the label must be --on-leaf ($capturedOnLeaf) in ${theme(dark)}",
            found.any { it.closeTo(capturedOnLeaf) },
        )
        // **D-023 is closed by the re-freeze, and the exclusion that used to guard it can only run in
        // one theme now.** V2's Library wrote `color:var(--paper)` on matcha while the Bench and Proof
        // wrote `--on-matcha`; V2.1 writes `--on-leaf` here, so the substitution to forbid is V2's
        // `--paper`. But the light palette declares `--paper:#FFF6E8` and `--on-leaf:#FFF6E8` — the same
        // colour — so in light no raster can tell the two apart, and an exclusion written there would
        // fail on a **correct** implementation. It is asserted where it discriminates, and the reason it
        // cannot be asserted in light is stated rather than left as a missing case.
        if (capturedPaper.closeTo(capturedOnLeaf)) {
            assertFalse(
                "the two tokens may only coincide in light — in dark, --paper (${capturedPaper}) and " +
                    "--on-leaf ($capturedOnLeaf) are different colours and the exclusion must run",
                dark,
            )
        } else {
            assertFalse(
                "no pixel may be --paper ($capturedPaper) in ${theme(dark)} — that is V2's " +
                    "transcription of this label, and the re-freeze replaced it",
                found.any { it.closeTo(capturedPaper) },
            )
        }
        // The reason the transcription is safe, measured on the rendered pair rather than asserted from
        // the tokens: 4.70:1 light and 6.95:1 dark.
        val ratio = contrast(capturedOnLeaf, capturedLeaf)
        assertTrue(
            "the label must clear AA on its own fill in ${theme(dark)} (found $ratio:1)",
            ratio >= 4.5f,
        )
    }

    @Test
    fun `the plus is the frozen fullwidth character, not the ASCII one`() {
        // D-021, ruled: *"Keep the literal characters exactly as defined by the frozen HTML… Bundled-font
        // coverage does not justify changing the design. Platform fallback is acceptable."* U+FF0B is
        // absent from all seven bundled faces — measured against their `cmap`s — so the platform supplies
        // it, and no rendered assertion on a Robolectric host could tell it from a tofu box. The codepoint
        // is therefore pinned directly: "＋" and "+" are indistinguishable in a diff and not on screen.
        assertEquals("the plus must be U+FF0B FULLWIDTH PLUS SIGN", "＋", StartPlusGlyph)
        assertNotEquals("and must not be the ASCII plus", "+", StartPlusGlyph)
    }

    // ---------------------------------------------------------------------------------------------
    // Behaviour, focus and the platform tree
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `pressing the button reports it once and navigates nowhere`() {
        var starts = 0
        composeRule.setContent {
            Host { ZineDock(onStart = { starts++ }, modifier = Modifier.align(Alignment.BottomCenter)) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ZineStartTestTag).performClick()
        composeRule.waitForIdle()

        // The frozen file wires no handler to `.start` at all — the only scripted buttons are the two
        // prototype controls. So the roadmap's "the CTA into the existing paper chooser" is route
        // hand-over, which is B5's, and this reports rather than navigates.
        assertEquals("one press must report exactly once", 1, starts)
    }

    @Test
    fun `the button reaches the platform tree as a button carrying its own name`() {
        dock()
        // The ADR-058/059 split, which this project has the scar for: a Compose semantics assertion reads
        // the *merged* tree and TalkBack reads the platform `AccessibilityNodeInfo` tree. A container
        // wrapping a `Text` collapses to `android.view.View` with no name at all unless it goes through
        // the A8 seam, so the seam's effect is read back off the real provider.
        val node = platformNode(ZineStartTestTag)
        assertEquals("the platform class must be a button", "android.widget.Button", node.className)
        assertEquals("and it must speak its own words", LABEL, node.contentDescription)
        assertTrue("and be clickable", node.isClickable)
    }

    @Test
    fun `the decorative misregistration ring is butter, and is not the focus ring`() {
        // **ADR-100 §4's second half, guarded only by a raster this pass re-recorded until a review
        // said so.** The ring was `butterTint`, 1.01:1 against the light desk — an invisible ring.
        //
        // The two assertions here are one finding: the *decorative* ring moved to `butter` and the
        // *focus* ring did not. They sit 5dp apart, they look like the same object, and only one of them
        // is load-bearing for 2.4.7/1.4.11. A revert that repainted both, or a "tidy-up" that unified
        // them, is the failure this catches — and the focus half is already asserted at `capturedInk` by
        // the test below, so what is added here is the decorative half plus the explicit non-equality.
        dock()
        val button = bounds(ZineStartTestTag)
        val raster = decorRaster()

        // The ring stacks OUTSIDE the hard shadow, so probe beyond the button's own box on the centre
        // row — the same row the focus-ring test uses, and the row a rounded shape displaces least.
        val y = button.center.y.roundToInt()
        val ringBand = (button.left.roundToInt() - RING_SEARCH..button.left.roundToInt())
        assertTrue(
            "no pixel left of the start button is --butter ($capturedButter) — the misregistration " +
                "ring is invisible on the light desk",
            ringBand.any { raster.matches(it, y, capturedButter) },
        )
        assertFalse(
            "--butter and --ink must stay different colours, or this test cannot tell the decorative " +
                "ring from the focus ring",
            capturedButter.closeTo(capturedInk),
        )
    }

    @Test
    fun `the focus ring surrounds the button instead of eating into it`() {
        dock()
        focusTheButton()

        val button = bounds(ZineStartTestTag)
        val raster = decorRaster()
        val y = button.center.y.roundToInt()

        // A CSS outline starts at the 3px offset *outside* the border box and grows outward, so a 2px
        // stroke centres 4px out. `Modifier.border` paints inside instead — the mutation B3's review
        // caught the absence of on `.more`, where a ring that eats into the object reads as the indicator
        // breaking rather than as focus.
        val stroke = ZinelyV2Dimens.FocusRingWidth.value.roundToInt()
        val ringX = button.left.roundToInt() - (RING_OFFSET + stroke / 2)
        assertTrue(
            "the ring must be --ink ${RING_OFFSET + stroke / 2}px outside the button's left edge " +
                "(found ${raster.colourAt(ringX, y)})",
            raster.matches(ringX, y, capturedInk),
        )
        assertTrue(
            "and the button's own fill must be untouched just inside its ink border " +
                "(found ${raster.colourAt(button.left.roundToInt() + START_BORDER_PROBE, y)})",
            raster.matches(button.left.roundToInt() + START_BORDER_PROBE, y, capturedLeaf),
        )

        // `outline-offset:3px` gated on **both** sides. The probe above only fails if the ring moves
        // inward: at an offset of 4 the ring spans 4-6px out and a probe at 4px out still lands on stroke,
        // so 3 -> 4 survived it. One pixel beyond the ring's outer limit must be clear.
        val beyond = button.left.roundToInt() - (RING_OFFSET + stroke + 1)
        assertFalse(
            "nothing may be --ink beyond the ring's ${RING_OFFSET + stroke}px outer limit " +
                "(found ${raster.colourAt(beyond, y)})",
            raster.matches(beyond, y, capturedInk),
        )

        // And the ring's **shape**. A single mid-height probe on one edge is satisfied by a square ring, so
        // `border-radius:16px -> 0` survived the whole suite. A CSS outline follows the border radius,
        // growing it by the offset: the ring's corner arc has radius 16+4=20 about the button's own corner
        // centre, so the 45-degree point 4px out from the corner is 28px from that centre — well outside a
        // rounded ring, and exactly on the stroke of a square one.
        val corner = button.left.roundToInt() - (RING_OFFSET + stroke / 2)
        val cornerY = button.top.roundToInt() - (RING_OFFSET + stroke / 2)
        assertFalse(
            "the ring must be rounded, so its corner must be empty at 45 degrees " +
                "(found ${raster.colourAt(corner, cornerY)})",
            raster.matches(corner, cornerY, capturedInk),
        )
        // ...and that probe only means something if the ring is present at all on those two edges.
        assertTrue(
            "the ring must run along the top edge, or the corner probe proves nothing",
            raster.matches(button.center.x.roundToInt(), cornerY, capturedInk),
        )
    }

    @Test
    fun `the button is a pill, not a rounded rectangle`() {
        dock()
        val button = bounds(ZineStartTestTag)
        val raster = decorRaster()

        // `.start{border-radius:var(--br-pill)}` — 999px, i.e. **half the height**, where V2 wrote a 16px
        // rectangle. Re-baselining 16 to 999 would assert nothing: any radius past half the height clips
        // identically, so the test has to change shape rather than change number. What separates a pill
        // from a large rounded rectangle is that the arc consumes the button's **whole** vertical extent:
        // at the exact vertical centre the fill must reach the outermost column, and one pixel further out
        // than half the height at the top row it must not.
        val r = button.height / 2f
        val midY = button.center.y.roundToInt()
        val left = button.left.roundToInt()

        assertTrue(
            "a pill's widest point is its vertical centre, so the leftmost column must be leaf there, " +
                "just inside the ink border (found ${raster.colourAt(left + START_BORDER_PROBE, midY)})",
            raster.matches(left + START_BORDER_PROBE, midY, capturedLeaf),
        )
        // The corner pixel of the bounding box is outside the arc of any radius worth the name — this is
        // the probe that fails on `RoundedCornerShape(0)`.
        assertFalse(
            "the button's top-left corner pixel must be clipped away, not leaf",
            raster.matches(left + 1, button.top.roundToInt() + 1, capturedLeaf),
        )
        // And the pill claim itself: a 16px rectangle has closed its arc ~5px in and would be fill at the
        // top row well before `r`. A pill is still empty there, because its arc runs the full half-height.
        val insideRectangleArc = left + (r / 2f).roundToInt()
        assertFalse(
            "at ${(r / 2f).roundToInt()}px in, the top row must still be outside a pill's arc — " +
                "a ${START_RADIUS_RECTANGLE}px rectangle would already be fill here " +
                "(found ${raster.colourAt(insideRectangleArc, button.top.roundToInt() + 1)})",
            raster.matches(insideRectangleArc, button.top.roundToInt() + 1, capturedLeaf),
        )
        // The arc must close by the time the corner's own 45-degree point is reached, or this is not a
        // radius at all but a clipped rectangle.
        val diagonal = (r - r / 1.41421f).roundToInt() + START_BORDER_PROBE
        assertTrue(
            "and the arc must have closed by ${diagonal}px in from that corner",
            raster.matches(left + diagonal, button.top.roundToInt() + diagonal, capturedLeaf),
        )
    }

    @Test
    fun `pressing the button settles it toward the desk`() {
        dock()
        val raster = decorRaster()
        val button = bounds(ZineStartTestTag)
        val x = button.center.x.roundToInt()
        val atRest = raster.firstRowOf(capturedLeaf, x, button.top.roundToInt() - 8)

        // `.start:active{transform:translateY(2px)}` on `transition:transform .14s`, which had no assertion
        // at all — deleting the whole `graphicsLayer`/`animateFloatAsState` block survived the suite, as did
        // swapping D-011's easing. A `transform` does not change layout, so this is only visible in pixels.
        composeRule.onNodeWithTag(ZineStartTestTag).performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(PRESS_DURATION * 2)
        composeRule.waitForIdle()

        val pressedRaster = decorRaster()
        val pressed = pressedRaster.firstRowOf(capturedLeaf, x, button.top.roundToInt() - 8)
        composeRule.onNodeWithTag(ZineStartTestTag).performTouchInput { up() }

        assertEquals(
            "the pressed button must sit ${PRESS_TRANSLATION}px lower than at rest",
            PRESS_TRANSLATION.toFloat(),
            (pressed - atRest).toFloat(),
            HALF_PIXEL,
        )
        // The direction is the claim, not just the distance: `translateY(+2px)` presses the button *into*
        // the desk. A sign error lifts it toward the reader, which is the opposite physical statement and
        // would satisfy an assertion written on the magnitude alone.
        assertTrue("and lower means a greater y, not a smaller one", pressed > atRest)
    }

    @Test
    fun `an unfocused button draws no ring`() {
        // The positive test above is worth nothing unless the same pixel is *not* ink at rest. It is not
        // clean ground either: the button's own `0 16px 30px -12px` shadow tints the region, which is the
        // B3 lesson that a probe outside an object must assert "not the expected ink" rather than "bare
        // ground".
        dock()
        val button = bounds(ZineStartTestTag)
        val raster = decorRaster()
        val stroke = ZinelyV2Dimens.FocusRingWidth.value.roundToInt()
        val ringX = button.left.roundToInt() - (RING_OFFSET + stroke / 2)
        val y = button.center.y.roundToInt()

        assertFalse(
            "an unfocused button must not paint --ink outside itself (found ${raster.colourAt(ringX, y)})",
            raster.matches(ringX, y, capturedInk),
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Harness
    // ---------------------------------------------------------------------------------------------

    private fun dock(dark: Boolean = false) {
        composeRule.setContent {
            Host(dark) { ZineDock(onStart = {}, modifier = Modifier.align(Alignment.BottomCenter)) }
        }
        composeRule.waitForIdle()
    }

    /**
     * The dock, plus two yardsticks rendered at the production styles in the same composition.
     *
     * [REF_LABEL] and [REF_PLUS] are the button's two children, each alone and each at the production
     * style — the two candidates for the row's height. [REF_ROW] is the whole row rebuilt with **no
     * margin** on the plus, which is the one value the alignment test measures. All three are references
     * rather than copies of an assertion: they measure the host, which is what the three tautological
     * "discrimination" guards this file first shipped did not.
     *
     * One composition, because the Compose rule accepts a single `setContent` per test.
     */
    private fun dockWithReferences() {
        composeRule.setContent {
            Host {
                ZineDock(onStart = {}, modifier = Modifier.align(Alignment.BottomCenter))
                // Both references at the **production** styles, so the button's height can be measured
                // against what it actually contains rather than against a remembered number.
                //
                // ⚠️ `lineHeight` is **not observable in this harness** — measured, not assumed: this
                // reference was run at `lineHeight = 40.sp` and still laid out 19px. Robolectric's text
                // layout does not honour the line-height span, so the frozen `body{line-height:1.55}`
                // (24.8px against the plus's 18.4px) cannot be gated from the JVM at all. It is written
                // here regardless, because the reference must not silently diverge from production the
                // day the harness gains it.
                Text(
                    text = LABEL,
                    style = TextStyle(
                        fontFamily = ZinelyV21Fonts.Work,
                        fontWeight = FontWeight.Bold,
                        fontSize = LABEL_SIZE.sp,
                        lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                    ),
                    modifier = Modifier.testTag(REF_LABEL).align(Alignment.TopStart),
                )
                Text(
                    text = StartPlusGlyph,
                    style = TextStyle(
                        fontFamily = ZinelyV21Fonts.Voice,
                        fontWeight = FontWeight.Bold,
                        fontSize = PLUS_SIZE.sp,
                        lineHeight = PLUS_SIZE.sp,
                    ),
                    modifier = Modifier.testTag(REF_PLUS).align(Alignment.BottomStart),
                )
                // The button's own row, rebuilt at the production styles with **no `margin-top`** on the
                // plus — the one value under test. Everything else is identical, deliberately: the plus is
                // compared against the label *within each row*, so the row's own height, its parity and
                // its position all cancel. Anchoring on the container's centre instead does not cancel
                // them: the button's height is odd, so its centre falls on a half-pixel and the reference
                // box's does not, and that alone accounted for exactly half the displacement this test
                // exists to measure.
                Row(
                    modifier = Modifier
                        .testTag(REF_ROW)
                        .align(Alignment.TopEnd)
                        .background(ZinelyTheme.v21Colors.leaf)
                        .padding(horizontal = 26.dp, vertical = START_PAD_VERTICAL.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = StartPlusGlyph,
                        style = TextStyle(
                            fontFamily = ZinelyTheme.v2Typography.work,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = PLUS_SIZE.sp,
                            color = ZinelyTheme.v21Colors.paper,
                        ),
                        modifier = Modifier.layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, 0) {
                                placeable.place(x = 0, y = -placeable.height / 2)
                            }
                        },
                    )
                    Text(
                        text = LABEL,
                        style = TextStyle(
                            fontFamily = ZinelyTheme.v2Typography.work,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = LABEL_SIZE.sp,
                            color = ZinelyTheme.v21Colors.paper,
                        ),
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    /**
     * Focus, through the keyboard input mode.
     *
     * **Compose declines focus in touch mode and `requestFocus()` fails silently**, which cost B3 a
     * diagnostic run to discover: the focused and unfocused probes contradicted each other because nothing
     * was ever focused. Requesting keyboard mode first is also the honest reading of `:focus-visible`,
     * which is a keyboard-navigation selector rather than a pointer one.
     */
    private fun focusTheButton() {
        composeRule.runOnUiThread {
            assertTrue(
                "the host must be able to enter keyboard input mode, or no focus test here means anything",
                inputMode.requestInputMode(InputMode.Keyboard),
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ZineStartTestTag).requestFocus()
        composeRule.waitForIdle()
    }

    private lateinit var inputMode: InputModeManager
    private var capturedInk: Color = Color.Unspecified
    private var capturedDesk: Color = Color.Unspecified
    private var capturedLeaf: Color = Color.Unspecified
    private var capturedPaper: Color = Color.Unspecified
    private var capturedOnLeaf: Color = Color.Unspecified

    /** `--butter` — the decorative `--frame` ring's colour since ADR-100 §4. Not the focus ring. */
    private var capturedButter: Color = Color.Unspecified

    @Composable
    private fun Host(dark: Boolean = false, content: @Composable BoxScope.() -> Unit) {
        ZinelyTheme(darkTheme = dark) {
            inputMode = LocalInputModeManager.current
            capturedButter = ZinelyTheme.v21Colors.butter
            capturedInk = ZinelyTheme.v21Colors.ink
            capturedDesk = ZinelyTheme.v21Colors.desk
            capturedLeaf = ZinelyTheme.v21Colors.leaf
            capturedPaper = ZinelyTheme.v21Colors.paper
            capturedOnLeaf = ZinelyTheme.v21Colors.onLeaf
            // The probe ground rather than the desk: the band's fade is only visible against something the
            // desk is not, and `.dock` is the only thing in this host that paints desk at all.
            Box(Modifier.fillMaxSize().background(PROBE_GROUND)) { content() }
        }
    }

    private fun theme(dark: Boolean) = if (dark) "dark" else "light"

    private fun bounds(tag: String): Rect =
        composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private fun platformNode(tag: String): PlatformA11yNode =
        composeRule.onNodeWithTag(tag).platformNode(composeRule.activity)

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

    private fun Bitmap.coloursIn(region: Rect): Set<Color> {
        val found = mutableSetOf<Color>()
        for (y in region.top.roundToInt() until region.bottom.roundToInt().coerceAtMost(height)) {
            for (x in region.left.roundToInt() until region.right.roundToInt().coerceAtMost(width)) {
                if (x >= 0 && y >= 0) found += colourAt(x, y)
            }
        }
        return found
    }

    /**
     * How far the `＋` rides above the label beside it, within one matcha row.
     *
     * Both glyphs sit in the same row, so the row's height, its parity and its position on screen cancel
     * out — leaving only what the frozen `margin-top` does to one of them. The two windows are cut from
     * the row's own left edge at the frozen padding: 26px in is the plus, and the label follows it after
     * the plus's width and the 10px gap.
     */
    private fun Bitmap.plusOverLabel(row: Rect): Float {
        val plus = inkCentroidY(row, row.left + 20f, row.left + 52f)
        val label = inkCentroidY(row, row.left + 56f, row.right - 20f)
        return plus - label
    }

    /**
     * The **centroid** of the ink inside [region] between [fromX] and [toX], weighted by how far each pixel
     * departs from the matcha it is drawn on.
     *
     * Used to find the `＋`, which cannot be found any other way: the A8 control seam ends in
     * `clearAndSetSemantics`, so nothing inside the button carries a findable node.
     *
     * **Weighted, because a bounding box cannot resolve one pixel.** The first version took
     * `(firstInkRow + lastInkRow) / 2`, which quantises: a 1px shift moves the first row by one and the
     * last by zero once antialiasing is counted, so it reported **half** the displacement. Against a
     * half-pixel tolerance that put the correct rendering exactly *on* the boundary — where `assertEquals`
     * passes — and left the wrong one passing too. The mutation that proved it is the very defect this
     * file's own Required Fix was written for, which is as clear a case as the programme has produced of
     * an assertion agreeing with the thing it was built to reject. A centroid moves continuously and by
     * the full amount.
     */
    private fun Bitmap.inkCentroidY(region: Rect, fromX: Float, toX: Float): Float {
        var weighted = 0.0
        var total = 0.0
        // The region's own top and bottom rows are the fill's antialiased edge against whatever is behind
        // it, and they carry weight without ever moving — so including them anchors the centroid and
        // reports a fraction of the glyph's displacement. Inset past them, still well clear of the glyph.
        val from = region.top.roundToInt() + EDGE_INSET
        val to = (region.bottom.roundToInt() - EDGE_INSET).coerceAtMost(height)
        for (y in from until to) {
            for (x in fromX.roundToInt() until toX.roundToInt().coerceAtMost(width)) {
                if (x < 0 || y < 0) continue
                val weight = distance(colourAt(x, y), capturedLeaf).toDouble()
                weighted += weight * y
                total += weight
            }
        }
        assertTrue("no ink was found to measure between $fromX and $toX in $region", total > 0.0)
        return (weighted / total).toFloat()
    }

    /** The first row at or below [from], in column [x], painted [expected] — the top edge of a fill. */
    private fun Bitmap.firstRowOf(expected: Color, x: Int, from: Int): Int {
        for (y in max(from, 0) until height) if (matches(x, y, expected)) return y
        throw AssertionError("no row in column $x below $from was $expected")
    }

    private fun Bitmap.matches(x: Int, y: Int, expected: Color): Boolean {
        if (x < 0 || y < 0 || x >= width || y >= height) return false
        return colourAt(x, y).closeTo(expected)
    }

    /** One 8-bit step of tolerance: an exact value still rounds through the bitmap's own channels. */
    private fun Color.closeTo(expected: Color): Boolean {
        val tolerance = 1.5f / 255f
        return abs(red - expected.red) <= tolerance &&
            abs(green - expected.green) <= tolerance &&
            abs(blue - expected.blue) <= tolerance
    }

    /** Straight-line distance in RGB, for claims about which of two colours a pixel is nearer. */
    private fun distance(a: Color, b: Color): Float =
        abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue)

    /** WCAG relative luminance, so the AA claim above is the ratio the standard defines. */
    private fun Color.relativeLuminance(): Float {
        fun channel(c: Float) = if (c <= 0.03928f) c / 12.92f else Math.pow(
            ((c + 0.055f) / 1.055f).toDouble(),
            2.4,
        ).toFloat()
        return 0.2126f * channel(red) + 0.7152f * channel(green) + 0.0722f * channel(blue)
    }

    private fun contrast(a: Color, b: Color): Float {
        val la = a.relativeLuminance()
        val lb = b.relativeLuminance()
        return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
    }
}
