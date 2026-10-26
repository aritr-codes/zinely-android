package com.aritr.zinely.feature.editor.a11y

import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.ink.InkHex
import com.aritr.zinely.core.editor.ink.InkLibrary
import com.aritr.zinely.core.editor.ink.MyInk
import com.aritr.zinely.feature.editor.BenchVerbKind
import com.aritr.zinely.feature.editor.InkMixerBody
import com.aritr.zinely.feature.editor.InkMixerFullNoteTestTag
import com.aritr.zinely.feature.editor.InkMixerHelpTestTag
import com.aritr.zinely.feature.editor.InkMixerHexFieldTestTag
import com.aritr.zinely.feature.editor.InkMixerHueTestTag
import com.aritr.zinely.feature.editor.InkMixerLegibClearTestTag
import com.aritr.zinely.feature.editor.InkMixerLegibPaleTestTag
import com.aritr.zinely.feature.editor.InkMixerLightTestTag
import com.aritr.zinely.feature.editor.InkMixerSquareTestTag
import com.aritr.zinely.feature.editor.InkMixerState
import com.aritr.zinely.feature.editor.InkMixerStrengthTestTag
import com.aritr.zinely.ui.a11y.PlatformA11yNode
import com.aritr.zinely.ui.a11y.platformNode
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Mix-an-ink body (A40 rule 6, INK task 9) on the platform tree: the three hidden sliders are real ranges
 * a service can set and step, the square has no node, and only the notes meant to speak are live regions.
 * The sheet's title row (with the colour name) lives in a `Dialog` window, which this harness does not reach.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp")
class InkMixerPlatformA11yTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var state by mutableStateOf(InkMixerState.open(InkHex("505955"), null))

    private fun host(start: InkHex, library: InkLibrary? = InkLibrary.EMPTY) {
        state = InkMixerState.open(start, null)
        composeRule.setContent {
            ZinelyTheme {
                Column(Modifier.width(360.dp)) {
                    InkMixerBody(state, { state = it }, BenchVerbKind.TEXT, Copy.InkMix.PREVIEW_SAMPLE, library)
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun node(tag: String): PlatformA11yNode = composeRule.onNodeWithTag(tag).platformNode(composeRule.activity)

    @Test
    fun `given the mixer then each slider is a range a service can set and step, with its value spoken`() {
        host(InkHex("505955"))
        val pct = (state.hsv.v * 100).roundToInt()

        for ((tag, label) in listOf(
            InkMixerStrengthTestTag to Copy.InkMix.COLOUR_STRENGTH,
            InkMixerLightTestTag to Copy.InkMix.LIGHTNESS,
            InkMixerHueTestTag to Copy.InkMix.HUE,
        )) {
            val slider = node(tag)
            assertEquals(label, slider.contentDescription)
            assertEquals("android.widget.SeekBar", slider.className)
            assertNotNull("$label has rangeInfo", slider.range)
            assertTrue("$label: ACTION_SET_PROGRESS", android.R.id.accessibilityActionSetProgress in slider.actionIds)
            assertTrue("$label: swipe up steps it", AccessibilityNodeInfo.ACTION_SCROLL_FORWARD in slider.actionIds)
            assertTrue("$label: swipe down steps it", AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD in slider.actionIds)
            assertTrue("$label: a keyboard can reach it", slider.isFocusable)
        }
        val light = node(InkMixerLightTestTag)
        assertEquals(Triple(0f, 100f, pct.toFloat()), light.range)
        assertEquals(Copy.InkMix.lightValue("dark grey", pct), light.stateDescription)
        assertEquals(355f, node(InkMixerHueTestTag).range!!.second)
    }

    @Test
    fun `given the square then it has no node of its own`() {
        host(InkHex("505955"))
        val square = composeRule.onAllNodesWithTag(InkMixerSquareTestTag).fetchSemanticsNodes().single()
        // `clearAndSetSemantics { testTag }` only: no description, no action, so TalkBack has nothing to land on.
        val p = composeRule.onNodeWithTag(InkMixerSquareTestTag).platformNode(composeRule.activity)
        assertEquals(null, p.contentDescription)
        assertTrue(!p.isClickable && !p.isFocusable && p.range == null)
        assertTrue(square.config.none { it.key == SemanticsActions.SetProgress })
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `given a focused slider when arrow keys are pressed then it moves in steps of five`() {
        // Given Lightness off the grid (5E = 37%), as a drag on the square can leave it
        host(InkHex("5E5E5E"))
        assertEquals(37, (state.hsv.v * 100).roundToInt())
        composeRule.onNodeWithTag(InkMixerLightTestTag).performSemanticsAction(SemanticsActions.RequestFocus)

        // When Right (one press a frame, as a keyboard sends them), then it snaps onto the grid: 40, not 42
        val press = { k: Key -> composeRule.onNodeWithTag(InkMixerLightTestTag).performKeyInput { pressKey(k) }; composeRule.waitForIdle() }
        press(Key.DirectionRight)
        assertEquals(40, (state.hsv.v * 100).roundToInt())
        // And Up and Down are left to focus (an Android SeekBar takes Left/Right only), so the value stays
        press(Key.DirectionUp)
        assertEquals(40, (state.hsv.v * 100).roundToInt())
        composeRule.onNodeWithTag(InkMixerLightTestTag).performSemanticsAction(SemanticsActions.RequestFocus)
        repeat(3) { press(Key.DirectionLeft) }
        assertEquals(25, (state.hsv.v * 100).roundToInt())

        // And Hue clamps at its far end
        composeRule.onNodeWithTag(InkMixerHueTestTag).performSemanticsAction(SemanticsActions.SetProgress) { it(355f) }
        composeRule.onNodeWithTag(InkMixerHueTestTag).performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.onNodeWithTag(InkMixerHueTestTag).performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.waitForIdle()
        assertEquals(355, state.hsv.h.roundToInt())
    }

    @Test
    fun `given a pale ink then the Pale in print note is a polite status`() {
        host(InkHex("F0E0D0"))
        assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE, node(InkMixerLegibPaleTestTag).liveRegion)
    }

    @Test
    fun `given a clear ink then Reads clearly is not a live region`() {
        host(InkHex("505955"))
        assertEquals(View.ACCESSIBILITY_LIVE_REGION_NONE, node(InkMixerLegibClearTestTag).liveRegion)
    }

    @Test
    fun `given a bad hex then its help is a polite live region, and the full note is polite too`() {
        val full = InkLibrary(
            myInks = (1..9).map { MyInk(InkHex("10203$it".padEnd(6, '0').take(6)), it.toLong()) },
            clock = 9,
        )
        host(InkHex("505955"), library = full)
        assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE, node(InkMixerFullNoteTestTag).liveRegion)

        composeRule.onNodeWithTag(InkMixerHexFieldTestTag).performTextReplacement("50G")
        composeRule.waitForIdle()
        assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE, node(InkMixerHelpTestTag).liveRegion)
    }
}
