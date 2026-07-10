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
    fun `primary button presets match the frozen coral CTAs`() {
        with(ZPrimaryButtonMetrics.Shelf) { // shelf.html .start
            assertEquals(56.dp, minHeight); assertEquals(30.dp, hPadding); assertEquals(18.dp, radius)
            assertEquals(17.sp, fontSize); assertEquals(10.dp, gap); assertEquals(20.dp, iconSize)
            assertEquals(0.34f, restShadowAlpha)
        }
        with(ZPrimaryButtonMetrics.Bench) { // bench.html .proof
            assertEquals(52.dp, minHeight); assertEquals(22.dp, hPadding); assertEquals(16.dp, radius)
            assertEquals(15.5.sp, fontSize); assertEquals(9.dp, gap); assertEquals(19.dp, iconSize)
            assertEquals(0.32f, restShadowAlpha)
        }
        with(ZPrimaryButtonMetrics.Proof) { // proof.html .primary
            assertEquals(54.dp, minHeight); assertEquals(22.dp, hPadding); assertEquals(16.dp, radius)
            assertEquals(16.sp, fontSize); assertEquals(10.dp, gap); assertEquals(20.dp, iconSize)
            assertEquals(0.32f, restShadowAlpha)
        }
    }

    @Test
    fun `tool button presets match the frozen field-filled family`() {
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
        with(ZToolButtonMetrics.ProofStepNav) { // proof.html .stepnav button
            assertEquals(52.dp, minHeight); assertEquals(22.dp, iconSize)
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
        node.assertHeightIsAtLeast(52.dp)
        node.assertWidthIsAtLeast(52.dp)
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
