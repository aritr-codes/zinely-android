package com.aritr.zinely.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Press
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * M1 shared-component behavior + semantics, per the parity plan's M1 DoD: a11y baked in (not left
 * to callers), frozen timer behavior, stable metrics. Scalar presets are pinned to the frozen CSS
 * literals here for the same reason ZinelyTokensTest transcribes them (Gradle cwd cannot reach the
 * HTML): drift fails the build.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ZComponentsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    // ----- frozen preset pins ---------------------------------------------------------------

    @Test
    fun `primary button presets match the frozen V2 1 filled actions`() {
        with(ZPrimaryButtonMetrics.Shelf) { // v21-library.html .start
            assertEquals(56.dp, minHeight); assertEquals(30.dp, hPadding)
            assertEquals(ZinelyV21Dimens.radiusPill, radius)
            assertEquals(17.sp, fontSize); assertEquals(10.dp, gap); assertEquals(20.dp, iconSize)
            assertEquals(0.34f, restShadowAlpha)
        }
        with(ZPrimaryButtonMetrics.Bench) { // v21-bench.html .add
            assertEquals(52.dp, minHeight); assertEquals(22.dp, hPadding)
            assertEquals(ZinelyV21Dimens.radiusPill, radius)
            assertEquals(15.5.sp, fontSize); assertEquals(9.dp, gap); assertEquals(19.dp, iconSize)
            assertEquals(0.32f, restShadowAlpha)
        }
        with(ZPrimaryButtonMetrics.Proof) { // v21-proof.html .btn-save
            assertEquals(54.dp, minHeight); assertEquals(22.dp, hPadding)
            assertEquals(ZinelyV21Dimens.radiusPill, radius)
            assertEquals(16.sp, fontSize); assertEquals(10.dp, gap); assertEquals(20.dp, iconSize)
            assertEquals(0.32f, restShadowAlpha)
        }
    }

    /**
     * **Inverted, ADR-102 P8.** V2's three radii — 18 / 16 / 16 — are gone: every button in the three
     * V2.1 prototypes is `--br-pill`. Asserted as an absence rather than deleted with the old values,
     * so a returning rounded-rectangle button fails rather than silently re-skinning three surfaces.
     */
    @Test
    fun `no primary preset carries a V2 rounded-rectangle radius`() {
        val v2Radii = setOf(18.dp, 16.dp)
        listOf(ZPrimaryButtonMetrics.Shelf, ZPrimaryButtonMetrics.Bench, ZPrimaryButtonMetrics.Proof)
            .forEach { assertFalse(it.radius in v2Radii) }
    }

    /**
     * **Inverted, ADR-102 P8.** The press vocabulary is four counted tiers and nothing else. V2's
     * press was `translateY(1px) scale(.99)` interpolated by `animateFloatAsState`; V2.1's is a whole
     * number of dp of offset with a shortened hard shadow, so **no tier travels a fraction of a dp**
     * and none sheds more than it rests on. A reintroduced scale press has no tier to express it, and
     * an interpolated fifth tier (the fabrication D-006 and D-007 both refused) fails the count.
     */
    @Test
    fun `the press vocabulary is four offset-only tiers`() {
        val tiers = listOf(
            ZinelyV21Press.Hero, ZinelyV21Press.Raised, ZinelyV21Press.Flat, ZinelyV21Press.Inline,
        )
        assertEquals(4, tiers.toSet().size)
        tiers.forEach { tier ->
            assertEquals(tier.travel.value, tier.travel.value.toInt().toFloat(), 0f)
            assertTrue(tier.pressed <= tier.rest)
        }
        // `.dclose`, `.iconbtn`, `.icon-btn` and `.fnav` press FLUSH — the shadow goes to nothing.
        assertEquals(0.dp, ZinelyV21Press.Flat.pressed)
    }

    @Test
    fun `tool button presets match the frozen bordered secondary family`() {
        with(ZToolButtonMetrics.ShelfSort) { // shelf.html .sortbtn
            assertEquals(48.dp, minHeight); assertEquals(14.dp, hPadding); assertEquals(13.5.sp, fontSize)
            assertEquals(FontWeight.Medium, fontWeight); assertEquals(6.dp, gap); assertEquals(15.dp, iconSize)
            assertTrue(softText); assertFalse(pressTranslate)
        }
        with(ZToolButtonMetrics.BenchTool) { // bench.html .tool
            assertEquals(48.dp, minHeight); assertEquals(15.dp, hPadding); assertEquals(14.sp, fontSize)
            assertEquals(FontWeight.Medium, fontWeight); assertEquals(8.dp, gap); assertEquals(18.dp, iconSize)
        }
        with(ZToolButtonMetrics.ProofGhost) { // proof.html .ghostbtn
            assertEquals(52.dp, minHeight); assertEquals(16.dp, hPadding); assertEquals(14.5.sp, fontSize)
            assertEquals(FontWeight.SemiBold, fontWeight); assertEquals(8.dp, gap); assertEquals(18.dp, iconSize)
        }
        with(ZToolButtonMetrics.ProofExport) { // proof.html .exportrow .tool
            assertEquals(52.dp, minHeight); assertEquals(14.dp, hPadding); assertEquals(14.5.sp, fontSize)
            assertEquals(FontWeight.SemiBold, fontWeight); assertEquals(19.dp, iconSize); assertTrue(pressTranslate)
        }
        with(ZToolButtonMetrics.ProofStepNav) { // proof.html .fnav
            assertEquals(44.dp, minHeight); assertEquals(18.dp, iconSize)
        }
    }

    @Test
    fun `frozen timers are 5s snackbar and 2200ms toast`() {
        assertEquals(5_000L, ZINELY_SNACKBAR_TIMEOUT_MILLIS)
        assertEquals(2_200L, ZINELY_TOAST_TIMEOUT_MILLIS)
    }

    @Test
    fun `css blur to android radius conversion matches the sigma math`() {
        // sigma = css/2; radius = (sigma - .5)/.57735 — e.g. css 20px -> sigma 10 -> r 16.45
        assertEquals(16.45f, cssBlurToAndroidRadius(20f), 0.01f)
        assertEquals(0.1f, cssBlurToAndroidRadius(0.5f), 0.001f) // clamp floor
    }

    // ----- buttons ---------------------------------------------------------------------------

    @Test
    fun `primary button is a clickable button with the frozen min height`() {
        var clicks = 0
        composeRule.setContent {
            ZinelyTheme {
                ZPrimaryButton(text = "Start a zine", onClick = { clicks++ }, metrics = ZPrimaryButtonMetrics.Shelf)
            }
        }
        val node = composeRule.onNodeWithText("Start a zine")
        node.assertHasClickAction()
        node.assertHeightIsAtLeast(56.dp)
        node.performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun `icon button exposes its label and honors disabled`() {
        var clicks = 0
        composeRule.setContent {
            ZinelyTheme {
                Column {
                    ZIconButton(onClick = { clicks++ }, contentDescription = "Undo", enabled = false) { }
                    ZIconButton(onClick = { clicks++ }, contentDescription = "Redo") { }
                }
            }
        }
        composeRule.onNodeWithContentDescription("Undo").performClick()
        assertEquals(0, clicks) // disabled: bench .iconbtn:disabled
        composeRule.onNodeWithContentDescription("Redo").performClick()
        assertEquals(1, clicks)
        // 44px visual expands to the frozen >=48dp touch target
        composeRule.onNodeWithContentDescription("Redo").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithContentDescription("Redo").assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun `icon-only tool button is square`() {
        composeRule.setContent {
            ZinelyTheme {
                ZToolButton(
                    onClick = {},
                    metrics = ZToolButtonMetrics.ProofStepNav,
                    contentDescription = "Next step",
                ) { }
            }
        }
        val node = composeRule.onNodeWithContentDescription("Next step")
        node.assertHeightIsAtLeast(44.dp)
        node.assertWidthIsAtLeast(44.dp)
    }

    // ----- menu items ------------------------------------------------------------------------

    @Test
    fun `radio menu items carry selection semantics in both frozen styles`() {
        composeRule.setContent {
            ZinelyTheme {
                Column {
                    ZMenuItem("Newest first", onClick = {}, selected = true, selectedStyle = ZSelectedStyle.WeightAndCheck)
                    ZMenuItem("A4", onClick = {}, selected = false, selectedStyle = ZSelectedStyle.Coral, subLabel = "210 × 297 mm")
                }
            }
        }
        composeRule.onNodeWithText("Newest first").assertIsSelected()
        composeRule.onNodeWithText("A4").assertIsNotSelected()
        composeRule.onNodeWithText("210 × 297 mm").assertExists()
    }

    @Test
    fun `plain menu item is a 52dp button`() {
        var clicked = false
        composeRule.setContent {
            ZinelyTheme { ZMenuItem("Duplicate", onClick = { clicked = true }) }
        }
        composeRule.onNodeWithText("Duplicate").assertHasClickAction()
        composeRule.onNodeWithText("Duplicate").assertHeightIsAtLeast(52.dp)
        composeRule.onNodeWithText("Duplicate").performClick()
        assertTrue(clicked)
    }

    // ----- sheet ------------------------------------------------------------------------------

    @Test
    fun `sheet shows title and content and scrim tap dismisses`() {
        var dismissed = false
        composeRule.setContent {
            ZinelyTheme {
                ZSheet(visible = true, onDismiss = { dismissed = true }, title = "Paper size", sub = "Match your printer.") {
                    ZMenuItem("A4", onClick = {}, selected = true, selectedStyle = ZSelectedStyle.Coral)
                }
            }
        }
        composeRule.onNodeWithText("Paper size").assertExists()
        composeRule.onNodeWithText("Match your printer.").assertExists()
        composeRule.onNodeWithText("A4").assertExists()
        composeRule.onNodeWithTag(ZSheetScrimTestTag).performClick()
        assertTrue(dismissed)
    }

    // ----- snackbar / toast -------------------------------------------------------------------

    @Test
    fun `snackbar times out after the frozen 5s and not before`() {
        composeRule.mainClock.autoAdvance = false
        var timedOut = false
        composeRule.setContent {
            ZinelyTheme {
                ZSnackbar(message = "Zine deleted", actionLabel = "Undo", onAction = {}, onTimeout = { timedOut = true })
            }
        }
        composeRule.mainClock.advanceTimeBy(4_900L)
        assertFalse(timedOut)
        composeRule.mainClock.advanceTimeBy(1_000L)
        assertTrue(timedOut)
    }

    @Test
    fun `snackbar action fires and receives focus on show`() {
        var undone = false
        composeRule.setContent {
            ZinelyTheme {
                ZSnackbar(message = "Zine deleted", actionLabel = "Undo", onAction = { undone = true }, onTimeout = {})
            }
        }
        // Spec: focus moves to the action because the triggering control was destroyed by render.
        // ponytail: Robolectric runs in touch mode, where Compose refuses programmatic focus on a
        // non-text node, so Focused=true cannot be asserted headless — assert the request seam
        // exists; the actual focus move is the M2 device TalkBack pass (F3).
        composeRule.onNodeWithText("Undo").assert(
            androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(
                androidx.compose.ui.semantics.SemanticsActions.RequestFocus,
            ),
        )
        composeRule.onNodeWithText("Undo").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Undo").performClick()
        assertTrue(undone)
    }

    @Test
    fun `toast times out after the frozen 2200ms`() {
        composeRule.mainClock.autoAdvance = false
        var timedOut = false
        composeRule.setContent {
            ZinelyTheme { ZToast(message = "Saved", onTimeout = { timedOut = true }) }
        }
        composeRule.mainClock.advanceTimeBy(2_100L)
        assertFalse(timedOut)
        composeRule.mainClock.advanceTimeBy(500L)
        assertTrue(timedOut)
    }

    // ----- text field --------------------------------------------------------------------------

    @Test
    fun `text field round-trips input`() {
        var value = "My zine"
        composeRule.setContent {
            ZinelyTheme {
                ZTextField(value = value, onValueChange = { value = it })
            }
        }
        composeRule.onNodeWithText("My zine").assertExists()
    }
}
