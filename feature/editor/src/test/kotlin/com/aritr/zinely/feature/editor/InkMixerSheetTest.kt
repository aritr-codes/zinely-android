package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.ink.InkHex
import com.aritr.zinely.core.editor.ink.InkLibrary
import com.aritr.zinely.core.editor.ink.MyInk
import com.aritr.zinely.core.editor.ink.PaletteDraft
import com.aritr.zinely.core.editor.ink.colourName
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Mix-an-ink sheet (INK task 8a, A40 rules 2–6, prototype `paint` / `openMixer` / the hex handlers). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class InkMixerSheetTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var state by mutableStateOf(InkMixerState.open(InkHex("2A251E"), null))

    private fun words(hex: InkHex) = inkColourWords(colourName(hex))

    /** The body and foot in a plain host: a `Dialog` window would drop a test's font scale. */
    private fun host(
        start: InkHex,
        kind: BenchVerbKind = BenchVerbKind.TEXT,
        library: InkLibrary? = InkLibrary.EMPTY,
        fontScale: Float = 1f,
    ) {
        state = InkMixerState.open(start, null)
        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                ZinelyTheme {
                    Column(Modifier.width(360.dp)) {
                        InkMixerBody(state, { state = it }, kind, Copy.InkMix.PREVIEW_SAMPLE, library)
                        InkMixerFoot(Copy.InkMix.USE_THIS_INK, onCancel = {}, onUse = {})
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun field() = composeRule.onNodeWithTag(InkMixerHexFieldTestTag)

    @Test
    fun `given 505955 typed one character at a time then the colour changes only at the sixth`() {
        val start = InkHex("2A251E")
        host(start)
        field().performTextClearance()
        for ((i, c) in "505955".withIndex()) {
            field().performTextInput(c.toString())
            composeRule.waitForIdle()
            if (i < 5) assertEquals("after ${i + 1} characters", start, state.hex)
        }
        assertEquals(InkHex("505955"), state.hex)
    }

    @Test
    fun `given 505 when IME Done then the three-digit code counts`() {
        host(InkHex("2A251E"))
        field().performTextClearance()
        field().performTextInput("505")
        composeRule.waitForIdle()
        field().performImeAction()
        composeRule.waitForIdle()
        assertEquals(InkHex("550055"), state.hex)
        assertNull(state.help)
    }

    @Test
    fun `given 12z then the error names the colour still showing, and the colour is unchanged`() {
        val start = InkHex("2A251E")
        host(start)
        field().performTextClearance()
        field().performTextInput("12z")
        composeRule.waitForIdle()
        assertEquals(start, state.hex)
        composeRule.onNodeWithTag(InkMixerHelpTestTag).fetchSemanticsNode()
            .config[SemanticsProperties.Text].single().text.let { assertEquals(Copy.InkMix.badHex(start.value), it) }
    }

    @Test
    fun `given input longer than six then the field keeps six after stripping (R-I4)`() {
        val s = InkMixerState.open(InkHex("2A251E"), null).onHex(TextFieldValue("# 50 59 55 77"))
        assertEquals("505955", s.hexField.text)
        assertEquals(InkHex("505955"), s.hex)
        assertEquals(TextRange(6), s.hexField.selection)
    }

    @Test
    fun `given a pale ink then the footer reads the pale line for the target, in the same slot height`() {
        host(InkHex("F2D7A6"), BenchVerbKind.TEXT)
        composeRule.onNodeWithText(Copy.InkMix.PALE_LEAD + Copy.InkMix.PALE_TEXT).assertExistsAsOne()
        composeRule.onNodeWithText(Copy.InkMix.READS_CLEARLY).assertDoesNotExist()
        val paleHeight = slotHeight()

        state = InkMixerState.open(InkHex("505955"), null)
        composeRule.waitForIdle()
        composeRule.onNodeWithText(Copy.InkMix.READS_CLEARLY).assertExistsAsOne()
        composeRule.onNodeWithText(Copy.InkMix.PALE_LEAD + Copy.InkMix.PALE_TEXT).assertDoesNotExist()
        assertEquals("one fixed slot: no layout jump", paleHeight, slotHeight(), 0.5f)
    }

    @Test
    fun `given a pale ink on decor then the footer says it is fine for a soft mark`() {
        host(InkHex("F2D7A6"), BenchVerbKind.DECOR)
        composeRule.onNodeWithText(Copy.InkMix.PALE_LEAD + Copy.InkMix.PALE_ART).assertExistsAsOne()
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertExistsAsOne() {
        fetchSemanticsNode()
    }

    private fun slotHeight() = composeRule.onNodeWithTag(InkMixerLegibTestTag).fetchSemanticsNode().size.height.toFloat()

    @Test
    fun `given My inks is full and a new hex then the note names the least recently used ink, and a kept hex has none`() {
        val list = listOf("505955", "C0392B", "2E6F95", "E8A87C", "6B4E71", "1F7A5C", "D4B000", "8C1C13", "3D5A80")
        val full = InkLibrary(myInks = list.mapIndexed { i, h -> MyInk(InkHex(h), (list.size - i).toLong()) }, clock = 9)
        host(InkHex("7A3B69"), library = full)
        composeRule.onNodeWithTag(InkMixerFullNoteTestTag).fetchSemanticsNode()
            .config[SemanticsProperties.Text].single().text
            .let { assertEquals(Copy.InkMix.fullNote(words(InkHex("3D5A80"))), it) }

        state = InkMixerState.open(InkHex("505955"), null)
        composeRule.waitForIdle()
        assertTrue(composeRule.onAllNodesWithTag(InkMixerFullNoteTestTag).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `given the strength slider set to 40 then s is 0_40 and its value reads the colour and 40 percent`() {
        host(InkHex("505955"))
        composeRule.onNodeWithTag(InkMixerStrengthTestTag).performSemanticsAction(SemanticsActions.SetProgress) { it(40f) }
        composeRule.waitForIdle()
        assertEquals(0.40, state.hsv.s, 1e-9)
        val node = composeRule.onNodeWithTag(InkMixerStrengthTestTag).fetchSemanticsNode()
        assertEquals(Copy.InkMix.strengthValue(words(state.hex), 40), node.config[SemanticsProperties.StateDescription])
        assertEquals(Copy.InkMix.COLOUR_STRENGTH, node.config[SemanticsProperties.ContentDescription].single())
        assertEquals(40f, node.config[SemanticsProperties.ProgressBarRangeInfo].current)
    }

    @Test
    fun `given the hue slider then it is clamped to 0 to 355 in steps of 5 and reads the hue name`() {
        host(InkHex("505955"))
        composeRule.onNodeWithTag(InkMixerHueTestTag).performSemanticsAction(SemanticsActions.SetProgress) { it(359f) }
        composeRule.waitForIdle()
        assertEquals(355.0, state.hsv.h, 1e-9)
        val node = composeRule.onNodeWithTag(InkMixerHueTestTag).fetchSemanticsNode()
        assertEquals(hueWord(com.aritr.zinely.core.editor.ink.hueName(355.0)), node.config[SemanticsProperties.StateDescription])
        assertEquals(0f..355f, node.config[SemanticsProperties.ProgressBarRangeInfo].range)
    }

    @Test
    fun `given the square then TalkBack does not see it`() {
        host(InkHex("505955"))
        val square = composeRule.onNodeWithTag(InkMixerSquareTestTag, useUnmergedTree = true).fetchSemanticsNode()
        assertNull(square.config.getOrNull(SemanticsProperties.ContentDescription))
        assertNull(square.config.getOrNull(SemanticsActions.OnClick))
        assertTrue("nothing inside it is a node either", square.children.isEmpty())
    }

    @Test
    fun `given largest text then the buttons stack and the square is 120dp`() {
        host(InkHex("505955"), fontScale = 2f)
        val cancel = composeRule.onNodeWithTag(InkMixerCancelTestTag).fetchSemanticsNode().boundsInRoot
        val use = composeRule.onNodeWithTag(InkMixerUseTestTag).fetchSemanticsNode().boundsInRoot
        assertTrue("Use sits below Cancel", use.top >= cancel.bottom)
        val square = composeRule.onNodeWithTag(InkMixerSquareTestTag, useUnmergedTree = true).fetchSemanticsNode()
        val density = composeRule.density.density
        assertEquals(120f, square.size.height / density, 1f)
        assertTrue("56dp buttons at largest text", use.height / density >= 55.5f)
    }

    @Test
    fun `given a draft then the Use label follows rev 2`() {
        assertEquals(Copy.InkMix.USE_THIS_INK, inkMixerUseLabel(null))
        val five = PaletteDraft(1, "P", "P", List(5) { InkHex("00000$it") }, emptyList())
        assertEquals(Copy.InkMix.ADD_TO_PALETTE, inkMixerUseLabel(five))
        assertEquals(Copy.InkMix.SAVE_TO_MY_INKS, inkMixerUseLabel(five.copy(inks = five.inks + InkHex("000009"))))
    }

    @Test
    fun `given the square and hue moves then s, v and h are clamped and the field follows`() {
        val s = InkMixerState.open(InkHex("505955"), null).onSquare(1.4, -0.2).onHue(-10.0)
        assertEquals(1.0, s.hsv.s, 0.0)
        assertEquals(0.0, s.hsv.v, 0.0)
        assertEquals(0.0, s.hsv.h, 0.0)
        assertEquals(s.hex.value, s.hexField.text)
    }

    @Test
    fun `given 505 typed in the sheet when Use then the three-digit code is committed and handed over`() {
        var used: InkHex? = null
        state = InkMixerState.open(InkHex("2A251E"), null)
        composeRule.setContent {
            ZinelyTheme {
                InkMixerSheet(
                    visible = true, state = state, onState = { state = it }, kind = BenchVerbKind.TEXT,
                    sample = Copy.InkMix.PREVIEW_SAMPLE, library = InkLibrary.EMPTY,
                    useLabel = Copy.InkMix.USE_THIS_INK, onCancel = {}, onUse = { used = it },
                )
            }
        }
        composeRule.waitForIdle()
        field().performTextClearance()
        field().performTextInput("505")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(InkMixerUseTestTag).performClick()
        composeRule.waitForIdle()
        assertEquals(InkHex("550055"), used)
        assertEquals("550055", state.hexField.text)
    }
}
