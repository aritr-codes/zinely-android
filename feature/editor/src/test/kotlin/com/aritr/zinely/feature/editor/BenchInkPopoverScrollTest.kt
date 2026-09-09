package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.zinelyContentInks
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
     * editor cannot expose its final palette actions without scrolling the whole popover.
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
}
