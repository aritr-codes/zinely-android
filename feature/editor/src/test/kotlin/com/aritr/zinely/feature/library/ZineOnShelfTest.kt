package com.aritr.zinely.feature.library

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.model.ZineCoverRecipe
import com.aritr.zinely.core.model.ZineCoverStamp
import com.aritr.zinely.core.model.ZineCoverSurface
import com.aritr.zinely.ui.a11y.PlatformA11yNode
import com.aritr.zinely.ui.a11y.platformNode
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV2Dimens
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * `.zine` — what the two gestures do, what a press looks like, and what the platform hears.
 *
 * **Every assertion here is a behaviour or a placed bound.** The gestures are driven through the pointer
 * API rather than by calling the callbacks, the press is measured off the transformed node rather than read
 * off the source, and the roles are read out of the **platform** `AccessibilityNodeInfo` tree rather than
 * Compose's merged one — the split ADR-058 records, and the reason B2's own a11y claim needed two tests.
 *
 * `w480dp` gives the object the 208dp column width the frozen shelf produces, so the numbers here are the
 * numbers a real cell sees.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w480dp-h960dp", sdk = [28])
class ZineOnShelfTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val INDEX = 3
        const val TITLE = "Sunday market"
        const val SUBTITLE = "A4 · 2 days ago"

        /** `.zine:active{transform:translateY(2px) scale(.985)}` */
        const val PRESS_DROP = 2f
        const val PRESS_SCALE = 0.985f

        /** `.zine:focus-visible{outline:2px solid var(--matcha-text);outline-offset:6px;border-radius:9px}` */
        const val RING_OFFSET = 6
        const val RING_RADIUS = 9

        /** The two columns the 2px stroke actually lands on, measured off the raster. */
        const val RING_INNER = 7
        const val RING_OUTER = 8

        /** Where the corner arc crosses its own diagonal — the probe that can see [RING_RADIUS]. */
        const val RING_DIAGONAL = 5

        /** A ground no token carries, so "something was painted here" is unambiguous. */
        val PROBE_GROUND = Color(0xFF00FF00)

        /** `.more{width:34px;height:34px;bottom:8px;right:8px}` */
        const val MORE_SIZE = 34
        const val MORE_INSET = 8

        /** The column width `w480dp` produces on the frozen shelf: `(480 − 22 − 22 − 20) / 2`. */
        val COLUMN = 208.dp

        /** Half a pixel, for the reason `ZineShelfTest.HALF_PIXEL` states: `assertEquals` passes at `|Δ| == delta`. */
        const val HALF_PIXEL = 0.5f

        val ITEM = ZineShelfItem(
            title = TITLE,
            recipe = ZineCoverRecipe(ZineCoverSurface.MatchaInk, ZineCoverStamp.Sun),
            subtitle = SUBTITLE,
        )
    }

    private val opened = mutableListOf<Int>()
    private val actioned = mutableListOf<Int>()

    // ---------------------------------------------------------------------------------------------
    // The two ways in, and the third — `:199`: "tap = open zine · long-press = actions · ... = actions"
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `a tap opens the zine and asks for nothing else`() {
        item()
        composeRule.onNodeWithTag(zineShelfCoverTestTag(INDEX)).performClick()

        assertEquals("a tap must open exactly this object", listOf(INDEX), opened)
        assertEquals("and must not open the actions", emptyList<Int>(), actioned)
    }

    @Test
    fun `a long press asks for the actions, and does not also open the zine`() {
        item()
        composeRule.onNodeWithTag(zineShelfCoverTestTag(INDEX)).performTouchInput { longClick() }

        // Both halves matter. A `combinedClickable` whose `onLongClick` is null delivers a long press as a
        // *click*, so a test that only checked `actioned` would pass while the shelf opened the zine.
        assertEquals("a long press must ask for the actions", listOf(INDEX), actioned)
        assertEquals("and must not also open it", emptyList<Int>(), opened)
    }

    @Test
    fun `the ⋯ asks for the same actions without opening the zine`() {
        item()
        composeRule.onNodeWithTag(zineShelfMoreTestTag(INDEX)).performClick()

        assertEquals("the fallback must reach the same place as the gesture", listOf(INDEX), actioned)
        assertEquals("and must not open the zine underneath it", emptyList<Int>(), opened)
    }

    @Test
    fun `the ⋯ is drawn at rest, not revealed by hovering or by discovering the gesture`() {
        item()
        // `.more{display:flex;opacity:.5}` unconditionally — no hover gate anywhere in the frozen rule
        // (`:73-77`). It is *"the fallback; long-press is the accelerator"* (`:72`), and a fallback that
        // appears only after you have found the accelerator is not one.
        composeRule.onNodeWithTag(zineShelfMoreTestTag(INDEX)).assertIsDisplayed()
    }

    // ---------------------------------------------------------------------------------------------
    // What a press looks like — `.zine:active`
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `a held cover settles two pixels down and a percent and a half smaller`() {
        item()
        val rest = coverBounds()

        composeRule.onNodeWithTag(zineShelfCoverTestTag(INDEX)).performTouchInput { down(center) }
        composeRule.waitForIdle()
        val held = coverBounds()

        // Both numbers, because either alone is satisfied by the wrong transform: a pure translate keeps
        // the width, and a pure scale about the centre moves the top edge down on its own.
        assertEquals(
            "the object must scale to $PRESS_SCALE of its width",
            rest.width * PRESS_SCALE,
            held.width,
            HALF_PIXEL,
        )
        val centreDrop = held.center.y - rest.center.y
        assertEquals(
            "and its centre must drop ${PRESS_DROP}px — the translate, which the scale cannot produce",
            PRESS_DROP,
            centreDrop,
            HALF_PIXEL,
        )
    }

    @Test
    fun `a cover at rest carries no transform at all`() {
        // The frozen `.zine` is a bare positioned box until `:active`. This is the control for the test
        // above: without it, a permanently-shrunken cover would satisfy every relative measurement there.
        item()
        val cover = coverBounds()
        assertEquals("an untouched cover is the full column wide", 208f, cover.width, HALF_PIXEL)
    }

    // ---------------------------------------------------------------------------------------------
    // The focus ring — `.zine:focus-visible`
    // ---------------------------------------------------------------------------------------------

    // **These three tests exist because independent review proved there were none.** Offset 6dp -> 60dp and
    // radius 9dp -> 0dp both survived the entire library suite, goldens included: the ring is painted
    // *outside* the node's bounds, so no bound can see it, and no raster in this package captures a focused
    // state. It is read off the pixels or it is not read at all.
    //
    // Two things had to be measured rather than assumed, and both were wrong on the first attempt. The ring
    // is requested through `InputMode.Keyboard`, because Compose refuses focus in touch mode and
    // `requestFocus()` silently did nothing — which is also the honest transcription, since `:focus-visible`
    // *is* keyboard focus. And the ground outside a cover is **not** clean: B1's grounded shadow tints it for
    // ten pixels out, so "no ring here" is asserted as *not the ring's ink* rather than as bare ground.

    @Test
    fun `a focused cover is ringed, clear of its own edge, with a rounded corner`() {
        item()
        focusTheCover()

        val raster = decorRaster()
        val cover = coverBounds()
        val left = cover.left.roundToInt()
        val midY = cover.center.y.roundToInt()

        // A CSS outline is drawn outside the box: its inner edge sits `outline-offset` out and its 2px
        // thickness grows outward from there. Measured, the stroke lands on the two columns 7 and 8 out.
        for (out in RING_INNER..RING_OUTER) {
            assertTrue(
                "the ring must be painted ${out}px out from the cover's left edge, in matchaText " +
                    "(found ${raster.colourAt(left - out, midY)})",
                raster.colourAt(left - out, midY).closeTo(capturedMatchaText),
            )
        }
        // The `outline-offset` gap must hold no ring, or a ring drawn flush against the edge — or a 2px
        // border eating into the layout — would satisfy the loop above.
        assertFalse(
            "the ${RING_OFFSET}px offset must be clear of the ring",
            raster.colourAt(left - 3, midY).closeTo(capturedMatchaText),
        )
        assertFalse(
            "and there must be no second ring beyond it",
            raster.colourAt(left - (RING_OUTER + 4), midY).closeTo(capturedMatchaText),
        )

        // **The radius, which a straight edge cannot see.** The ring rect's own corner sits 8px out; with
        // `border-radius:9px` the arc cuts that corner and crosses its diagonal about 5px out. Squared off,
        // that point is inside the ring and nothing is painted there — so this is what pins the 9px.
        assertTrue(
            "the ring's corner must be rounded to ${RING_RADIUS}px — a square corner leaves the " +
                "diagonal ${RING_DIAGONAL}px out unpainted",
            raster.anyPixelNear(left - RING_DIAGONAL, cover.top.roundToInt() - RING_DIAGONAL, capturedMatchaText),
        )
    }

    @Test
    fun `an unfocused cover has no ring at all`() {
        // The control: without it, a ring painted unconditionally would satisfy every assertion above while
        // showing on all six covers of a shelf at once.
        item()
        val raster = decorRaster()
        val cover = coverBounds()
        val midY = cover.center.y.roundToInt()
        for (out in RING_INNER..RING_OUTER) {
            assertFalse(
                "nothing may be painted ${out}px outside an unfocused cover",
                raster.colourAt(cover.left.roundToInt() - out, midY).closeTo(capturedMatchaText),
            )
        }
    }

    @Test
    fun `the ring does not shrink when the cover is pressed`() {
        // `drawBehind` is chained *before* `graphicsLayer`, so the press transform never reaches the ring.
        // Chaining them the other way composes perfectly and reads as deliberate — and would drag the focus
        // indicator inward every time the object was touched.
        item()
        focusTheCover()
        val rest = coverBounds()

        composeRule.onNodeWithTag(zineShelfCoverTestTag(INDEX)).performTouchInput { down(center) }
        composeRule.waitForIdle()

        assertTrue(
            "the cover must actually have shrunk, or this test proves nothing",
            coverBounds().width < rest.width,
        )
        assertTrue(
            "but the ring must still stand where it did, measured from the cover's resting edge",
            decorRaster()
                .colourAt(rest.left.roundToInt() - RING_INNER, rest.center.y.roundToInt())
                .closeTo(capturedMatchaText),
        )
    }

    // ---------------------------------------------------------------------------------------------
    // D-009 — the target grows, the mark does not
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the ⋯ mark stays 34dp while what a finger hits reaches the platform floor`() {
        // **Only the first half of this name discriminates, and saying so is the point.** Independent review
        // removed `minimumInteractiveComponentSize()` from the seam and this test stayed green: Compose's
        // own pointer-input minimum already lifts `touchBoundsInRoot` to 48dp regardless. The seam's floor is
        // guarded where it lives, by `ZinelyV2ControlPlatformA11yTest`; what is guarded *here* is that
        // satisfying D-009 did not grow the printed mark, which is what D-009's ruling actually forbids.
        item()
        val node = composeRule.onNodeWithTag(zineShelfMoreTestTag(INDEX)).fetchSemanticsNode()
        val drawn = node.boundsInRoot
        val touch = node.touchBoundsInRoot
        val floor = ZinelyV2Dimens.MinTouchTarget.value

        // The D-009 ruling is two claims, and a test that checked only the floor would pass a control that
        // met it by being drawn bigger — *"do not modify the visual design solely to satisfy these
        // requirements"*. So the mark is pinned at its frozen size first.
        assertEquals("the drawn mark is ${MORE_SIZE}dp wide", MORE_SIZE.toFloat(), drawn.width, HALF_PIXEL)
        assertEquals("and ${MORE_SIZE}dp tall", MORE_SIZE.toFloat(), drawn.height, HALF_PIXEL)
        assertTrue(
            "the touch target must reach ${floor}dp (found ${touch.width} × ${touch.height})",
            touch.width >= floor - HALF_PIXEL && touch.height >= floor - HALF_PIXEL,
        )
    }

    @Test
    fun `the ⋯ sits eight pixels in from the cover's bottom-right corner`() {
        item()
        val cover = coverBounds()
        val more = composeRule.onNodeWithTag(zineShelfMoreTestTag(INDEX))
            .fetchSemanticsNode().boundsInRoot

        // `.more{bottom:8px;right:8px}` measured against the *cover*, which is the box CSS positions it
        // against — and the check that survives the affordance moving out of the cover's own subtree.
        assertEquals(
            "${MORE_INSET}px in from the right edge",
            MORE_INSET.toFloat(),
            cover.right - more.right,
            HALF_PIXEL,
        )
        assertEquals(
            "${MORE_INSET}px up from the bottom edge",
            MORE_INSET.toFloat(),
            cover.bottom - more.bottom,
            HALF_PIXEL,
        )
    }

    // ---------------------------------------------------------------------------------------------
    // What the platform hears
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the cover is a button in the platform's own tree, named by the zine`() {
        item()
        val node = platformNode(zineShelfCoverTestTag(INDEX))
        assertEquals(
            "TalkBack must land on a button, not a bare View — the ADR-059 defect V2 exists not to inherit",
            "android.widget.Button",
            node.className,
        )
        assertEquals("and must hear the zine's own name", TITLE, node.contentDescription)
    }

    @Test
    fun `the long press has a named twin in the platform tree, not an anonymous one`() {
        item()
        val node = platformNode(zineShelfCoverTestTag(INDEX))
        // The implementation guide's rule is that *"every gesture has a named custom action twin"*. An
        // unnamed ACTION_LONG_CLICK is present but undiscoverable, so the label is the assertion.
        assertTrue("the platform node must offer a long-press action", node.isLongClickable)
        assertEquals("and must name it", "Actions", node.longClickLabel)
    }

    @Test
    fun `the ⋯ survives into the platform tree as its own named button`() {
        item()
        // This is the assertion that would have caught the shape B1 anticipated and B3 deleted: nested
        // inside the cover, the `⋯` sat under a `clearAndSetSemantics` and reached no service at all.
        val node = platformNode(zineShelfMoreTestTag(INDEX))
        assertEquals("android.widget.Button", node.className)
        assertEquals("the frozen aria-label, verbatim", "Actions for $TITLE", node.contentDescription)
        assertFalse(
            "and the fallback itself takes no long press — it *is* the non-gesture path",
            node.isLongClickable,
        )
    }

    @Test
    fun `the object discloses no metadata of its own`() {
        item()
        // `data-sub` exists on every `.zine` and the shelf shows none of it: *"Format & date are disclosed
        // there, not stamped on every card"* (`:142-144`). "There" is the sheet.
        composeRule.onNodeWithText(SUBTITLE).assertDoesNotExist()
        assertNull(
            "and it must not reach a screen reader either",
            platformNode(zineShelfCoverTestTag(INDEX)).contentDescription
                ?.takeIf { it.contains(SUBTITLE) },
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Harness
    // ---------------------------------------------------------------------------------------------

    private fun item() {
        composeRule.setContent {
            Host {
                ZineOnShelf(
                    zine = ITEM,
                    index = INDEX,
                    onOpen = { opened += it },
                    onActions = { actioned += it },
                    modifier = Modifier.width(COLUMN),
                )
            }
        }
        composeRule.waitForIdle()
    }

    private var capturedMatchaText: Color = Color.Unspecified
    private lateinit var inputMode: InputModeManager

    /**
     * Focus the cover the way a keyboard would.
     *
     * `requestFocus()` alone does nothing here and fails silently: Compose declines focus while the input
     * mode is `Touch`, which Robolectric starts in. Requesting `Keyboard` first is also the faithful reading
     * of `:focus-visible`, which is the keyboard-focus selector rather than the focus one.
     */
    private fun focusTheCover() {
        composeRule.runOnUiThread {
            assertTrue(
                "the host must be able to enter keyboard input mode, or no focus test here means anything",
                inputMode.requestInputMode(InputMode.Keyboard),
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(zineShelfCoverTestTag(INDEX)).requestFocus()
        composeRule.waitForIdle()
    }

    @Composable
    private fun Host(content: @Composable () -> Unit) {
        ZinelyTheme(darkTheme = false) {
            capturedMatchaText = ZinelyTheme.v2Colors.matchaText
            inputMode = LocalInputModeManager.current
            // The probe ground rather than the desk: the ring is painted *outside* the cover, so the pixels
            // it lands on must be a colour no token carries, or "ring" and "room" could be the same value.
            //
            // Centred, not at the origin, and that is load-bearing for the ring tests: the frozen ring is
            // drawn *outside* the object's bounds, so a cover flush against x=0 puts its own ring off the
            // raster entirely. Centring leaves room on every side to sample.
            Box(
                Modifier.fillMaxSize().background(PROBE_GROUND),
                contentAlignment = Alignment.Center,
            ) { content() }
        }
    }

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

    /** The arc crosses its diagonal at a sub-pixel point, so the probe accepts it within one pixel. */
    private fun Bitmap.anyPixelNear(x: Int, y: Int, expected: Color): Boolean =
        (-1..1).any { dy -> (-1..1).any { dx -> colourAt(x + dx, y + dy).closeTo(expected) } }

    private fun Color.closeTo(other: Color): Boolean {
        val tolerance = 1.5f / 255f
        return abs(red - other.red) <= tolerance &&
            abs(green - other.green) <= tolerance &&
            abs(blue - other.blue) <= tolerance
    }

    private fun coverBounds(): Rect =
        composeRule.onNodeWithTag(zineShelfCoverTestTag(INDEX)).fetchSemanticsNode().boundsInRoot

    /**
     * The **platform** node for a tagged composable, through the published CI-26 harness.
     *
     * Not a local walk of the view tree: `platformNode` in `:core:ui`'s test fixtures is the same reader
     * `ZinelyV2ControlPlatformA11yTest` uses, so a change in how the platform tree is read cannot leave one
     * of the two suites measuring something else. It grew `isLongClickable`/`longClickLabel` for B3.
     */
    private fun platformNode(tag: String): PlatformA11yNode =
        composeRule.onNodeWithTag(tag).platformNode(composeRule.activity)
}
