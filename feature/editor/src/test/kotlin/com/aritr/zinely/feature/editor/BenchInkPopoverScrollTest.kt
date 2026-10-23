package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.aritr.zinely.ui.theme.ZinelyTheme
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
     * A16 freezes a scrolling tray. Decor carries the extra tints row, so a phone-height editor cannot
     * expose the tray's last section without scrolling. A40 rev. 2: that section is now the Starter
     * palettes disclosure (it was A16's preset pills, whose "Apply Forest" line this used to reach).
     */
    @Test
    fun decor_tray_scrolls_to_the_starter_palettes_in_a_constrained_editor() {
        composeRule.setContent {
            ZinelyTheme {
                Box(Modifier.size(width = 360.dp, height = 430.dp)) {
                    BenchInkPopover(
                        visible = true,
                        kind = BenchVerbKind.DECOR,
                        selected = null,
                        onPick = {},
                        onDone = {},
                        onDockedTopChanged = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag(BenchInkStartersTestTag).performScrollTo().assertIsDisplayed()
    }

    /**
     * The header is pinned: scrolling to the last section must not carry `Ink` and `Done` under the
     * card's top edge. A tester's phone showed both cut in half there, with the only way out of the
     * tray half off the card.
     */
    @Test
    fun title_and_done_stay_whole_when_the_tray_is_scrolled_to_its_end() {
        composeRule.setContent {
            ZinelyTheme {
                Box(Modifier.size(width = 360.dp, height = 430.dp)) {
                    BenchInkPopover(
                        visible = true,
                        kind = BenchVerbKind.DECOR,
                        selected = null,
                        onPick = {},
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

        composeRule.onNodeWithTag(BenchInkStartersTestTag).performScrollTo()

        // Not vacuous: the tray really did scroll, so the header had the chance to leave.
        assertNotEquals(currentBefore, bounds(BenchInkCurrentTestTag))
        // `boundsInRoot` is clipped, so an equal rectangle means not moved and not cut.
        assertEquals(titleBefore, bounds(BenchInkTitleTestTag))
        assertEquals(doneBefore, bounds(BenchInkDoneTestTag))
    }
}
