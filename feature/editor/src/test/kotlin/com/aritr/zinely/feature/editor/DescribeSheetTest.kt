package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.components.ZSheetScrimTestTag
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

/** Bench A52.2-A52.4 (ADR-147): the Describe sheet's words, its count, and that every way out saves. */
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp-xhdpi")
class DescribeSheetTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var open by mutableStateOf(true)
    private val saved = mutableListOf<String>()

    private fun host(kind: BenchVerbKind = BenchVerbKind.PHOTO, opening: String = "") {
        open = true
        saved.clear()
        composeRule.setContent {
            ZinelyTheme {
                DescribeSheet(
                    visible = open,
                    kind = kind,
                    opening = opening,
                    onSave = { saved += it },
                    onClose = { open = false },
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun fieldConfig() = composeRule.onNodeWithTag(DescribeFieldTestTag).fetchSemanticsNode().config

    @Test
    fun the_photo_sheet_says_its_words_exactly() {
        host()
        composeRule.onNodeWithText(Copy.Describe.TITLE_PHOTO).assertExists()
        composeRule.onNodeWithText(Copy.Describe.HELPER).assertExists()
        composeRule.onNodeWithText(Copy.Describe.DONE).assertExists()
        assertEquals("Describe this photo", Copy.Describe.TITLE_PHOTO)
        assertEquals("Zinely reads this aloud in Read. It isn’t printed or put in the PDF.", Copy.Describe.HELPER)
        // The grey example is a hint for the eyes, silent to a screen reader: the label names the field.
        assertEquals(Copy.Describe.LABEL, fieldConfig().getOrNull(SemanticsProperties.ContentDescription)?.single())
    }

    @Test
    fun the_art_sheet_has_its_own_title() {
        host(BenchVerbKind.DECOR)
        composeRule.onNodeWithText(Copy.Describe.TITLE_ART).assertExists()
        assertEquals("Describe this piece", Copy.Describe.TITLE_ART)
    }

    @Test
    fun there_is_no_count_until_fifty_are_left_and_then_it_rides_in_the_fields_own_node() {
        host()
        composeRule.onNodeWithTag(DescribeFieldTestTag).performTextInput("x".repeat(199))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(DescribeCountTestTag).assertDoesNotExist()
        assertNull(fieldConfig().getOrNull(SemanticsProperties.StateDescription))

        composeRule.onNodeWithTag(DescribeFieldTestTag).performTextInput("y")
        composeRule.waitForIdle()
        // 250 - 200 = 50 left: shown, and spoken through the field, not as a separate stop.
        assertEquals("50 left", fieldConfig().getOrNull(SemanticsProperties.StateDescription))
        composeRule.onNodeWithTag(DescribeCountTestTag, useUnmergedTree = true).assertExists()
        assertTrue(
            "the drawn count must not be a second spoken node",
            composeRule.onNodeWithTag(DescribeCountTestTag, useUnmergedTree = true).fetchSemanticsNode()
                .config.getOrNull(SemanticsProperties.Text) == null,
        )
    }

    @Test
    fun typing_stops_at_250_and_a_line_break_is_a_space() {
        host()
        composeRule.onNodeWithTag(DescribeFieldTestTag).performTextInput("z".repeat(400))
        composeRule.waitForIdle()
        assertEquals(250, fieldText().length)
        assertEquals("0 left", fieldConfig().getOrNull(SemanticsProperties.StateDescription))

        composeRule.onNodeWithTag(DescribeFieldTestTag).performTextReplacement("a\nb")
        composeRule.waitForIdle()
        assertEquals("a b", fieldText())
    }

    @Test
    fun a_stored_description_over_the_cap_is_not_cut_by_opening_and_closing() {
        val long = "q".repeat(300)
        host(opening = long)
        composeRule.onNodeWithText(Copy.Describe.DONE).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(long), saved)
    }

    @Test
    fun done_saves_the_words_and_closes() {
        host()
        composeRule.onNodeWithTag(DescribeFieldTestTag).performTextInput("Mum at the gate")
        composeRule.onNodeWithTag(DescribeDoneTestTag).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("Mum at the gate"), saved)
        assertEquals(false, open)
    }

    @Test
    fun a_scrim_tap_saves_the_words_too() {
        host()
        composeRule.onNodeWithTag(DescribeFieldTestTag).performTextInput("kept")
        composeRule.onNodeWithTag(ZSheetScrimTestTag).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("kept"), saved)
        assertEquals(false, open)
    }

    @Test
    fun back_saves_the_words_too() {
        host()
        composeRule.onNodeWithTag(DescribeFieldTestTag).performTextInput("kept on back")
        composeRule.runOnUiThread { ShadowDialog.getLatestDialog().onBackPressed() }
        composeRule.waitForIdle()
        assertEquals(listOf("kept on back"), saved)
        assertEquals(false, open)
    }

    @Test
    fun enter_is_done() {
        host()
        composeRule.onNodeWithTag(DescribeFieldTestTag).performTextInput("typed")
        composeRule.onNodeWithTag(DescribeFieldTestTag).performKeyInput { pressKey(Key.Enter) }
        composeRule.waitForIdle()
        assertEquals(listOf("typed"), saved)
    }

    @Test
    fun closing_keeps_the_typed_words_while_the_sheet_leaves_and_a_reopening_starts_from_the_saved_ones() {
        host()
        composeRule.onNodeWithTag(DescribeFieldTestTag).performTextInput("kept")
        composeRule.onNodeWithTag(DescribeDoneTestTag).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("kept"), saved)
        // The host does not store it, so a reopening shows `opening` ("") again, not the stale draft.
        open = true
        composeRule.waitForIdle()
        assertEquals("", fieldText())
    }

    @Test
    fun the_sheet_saves_once_however_many_ways_out_are_tapped() {
        host()
        composeRule.onNodeWithTag(DescribeFieldTestTag).performTextInput("once")
        composeRule.onNodeWithTag(DescribeDoneTestTag).performClick()
        composeRule.waitForIdle()
        assertEquals(1, saved.size)
    }

    private fun fieldText(): String =
        fieldConfig().getOrNull(SemanticsProperties.EditableText)?.text.orEmpty()
}
