package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.zinelyContentInks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BenchInkPopoverScrollTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /**
     * A16 freezes a scrolling tray. Decor carries the extra Paper tints band, so a phone-height
     * editor cannot expose its final palette actions without scrolling the tray.
     */
    @Test
    fun decor_tray_scrolls_to_every_starting_palette_action_in_a_constrained_editor() {
        val inks = zinelyContentInks()
        composeRule.setContent {
            ZinelyTheme {
                Box(Modifier.size(width = 360.dp, height = 430.dp)) {
                    BenchInkPopover(
                        visible = true,
                        bands = benchInkBands(inks, BenchVerbKind.DECOR),
                        presets = benchInkPresets(inks),
                        selected = null,
                        onPick = {},
                        onPreset = {},
                        onDone = {},
                        onDockedTopChanged = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText(Copy.BenchInk.apply(Copy.BenchInk.FOREST), useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * The header is pinned: scrolling to the last palette must not carry `Ink` and `Done` under the
     * card's top edge. A tester's phone showed both cut in half there, with the only way out of the
     * tray half off the card.
     */
    @Test
    fun title_and_done_stay_whole_when_the_tray_is_scrolled_to_its_end() {
        val inks = zinelyContentInks()
        composeRule.setContent {
            ZinelyTheme {
                Box(Modifier.size(width = 360.dp, height = 430.dp)) {
                    BenchInkPopover(
                        visible = true,
                        bands = benchInkBands(inks, BenchVerbKind.DECOR),
                        presets = benchInkPresets(inks),
                        selected = null,
                        onPick = {},
                        onPreset = {},
                        onDone = {},
                        onDockedTopChanged = {},
                    )
                }
            }
        }
        fun bounds(tag: String) =
            composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val titleBefore = bounds(BenchInkTitleTestTag)
        val doneBefore = bounds(BenchInkDoneTestTag)
        val currentBefore = bounds(BenchInkCurrentTestTag)

        composeRule.onNodeWithText(Copy.BenchInk.apply(Copy.BenchInk.FOREST), useUnmergedTree = true)
            .performScrollTo()

        // Not vacuous: the tray really did scroll, so the header had the chance to leave.
        assertNotEquals(currentBefore, bounds(BenchInkCurrentTestTag))
        // `boundsInRoot` is clipped, so an equal rectangle means not moved and not cut.
        assertEquals(titleBefore, bounds(BenchInkTitleTestTag))
        assertEquals(doneBefore, bounds(BenchInkDoneTestTag))
    }
}
