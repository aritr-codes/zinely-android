package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.Script
import com.aritr.zinely.core.model.TextCoverage
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The live unsupported-character notice ([EditorCoverageNotice], ADR-070; VOICE §Errors). Proves the
 * *static* state is always correct and motion-independent — the notice is present and names the script
 * whenever the coverage is not fully covered, absent when it is — and that a repeated script name is not
 * duplicated in the copy. The host gating (only while editing, yields to the save-failure banner) is
 * proven in [EditorScreenTest].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorCoverageNoticeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun coverage(vararg scripts: Script) =
        TextCoverage(scripts.toList(), scripts.map { it.displayName }, scripts.size)

    @Test
    fun renders_and_names_the_script_when_not_covered() {
        composeRule.setContent {
            ZinelyTheme { EditorCoverageNotice(coverage = coverage(Script.BENGALI), reduceMotion = false) }
        }
        composeRule.onNodeWithTag(EditorCoverageNoticeTestTag).assertIsDisplayed()
        composeRule.onNodeWithText(Copy.Coverage.unsupported(listOf("Bengali")), substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun renders_with_reduced_motion() {
        // Reduced-motion degrades the transition, never the content: the static state is identical.
        composeRule.setContent {
            ZinelyTheme { EditorCoverageNotice(coverage = coverage(Script.BENGALI), reduceMotion = true) }
        }
        composeRule.onNodeWithTag(EditorCoverageNoticeTestTag).assertIsDisplayed()
    }

    @Test
    fun is_absent_when_fully_covered() {
        composeRule.setContent {
            ZinelyTheme { EditorCoverageNotice(coverage = TextCoverage.Covered, reduceMotion = true) }
        }
        composeRule.onNodeWithTag(EditorCoverageNoticeTestTag).assertDoesNotExist()
    }

    @Test
    fun de_duplicates_scripts_that_share_a_display_name() {
        // HIRAGANA and KATAKANA both display "Japanese"; the copy must read "Japanese", never
        // "Japanese and Japanese".
        composeRule.setContent {
            ZinelyTheme {
                EditorCoverageNotice(coverage = coverage(Script.HIRAGANA, Script.KATAKANA), reduceMotion = true)
            }
        }
        composeRule.onNodeWithText(Copy.Coverage.unsupported(listOf("Japanese")), substring = true)
            .assertIsDisplayed()
    }

    // ── ADR-126: Greek and Cyrillic are only ever reported for a Book text ──────────────────────────

    @Test
    fun a_Book_text_holding_Greek_is_told_the_way_out_not_that_it_cannot_print() {
        assertEquals(
            "Book has no Greek letters. Switch this text to Plain to print them in a matching face.",
            coverageNoticeLine(coverage(Script.GREEK)),
        )
        assertEquals(
            "Book has no Greek and Cyrillic letters. Switch this text to Plain to print them in a matching face.",
            coverageNoticeLine(coverage(Script.CYRILLIC, Script.GREEK)),
        )
    }

    @Test
    fun a_script_no_font_sets_keeps_its_own_line_and_both_show_when_a_Book_text_holds_both() {
        assertEquals(Copy.Coverage.unsupported(listOf("Bengali")), coverageNoticeLine(coverage(Script.BENGALI)))
        assertEquals(
            Copy.Coverage.bookLacks("Cyrillic") + " " + Copy.Coverage.unsupported(listOf("Bengali")),
            coverageNoticeLine(coverage(Script.CYRILLIC, Script.BENGALI)),
        )
    }
}
