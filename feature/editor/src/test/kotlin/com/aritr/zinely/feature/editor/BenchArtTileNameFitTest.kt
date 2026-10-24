package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.TextLayoutResult
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * D-132: every Art name is read whole on its A16 tile. A16 (tile size, grid, two lines, clip) is frozen,
 * so the names fit the tile rather than the tile the names. Measured on the real sheet at the narrowest
 * supported phone width and the default font scale, from the label's own laid-out text.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xhdpi")
class BenchArtTileNameFitTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun every_art_name_lays_out_whole_in_two_lines_without_breaking_a_word() {
        // Given the real Art sheet on a 360dp phone at font scale 1.0
        assertEquals(1f, composeRule.activity.resources.configuration.fontScale)
        composeRule.setContent { ZinelyTheme { BenchArtSheet(visible = true, onDismiss = {}, onPick = {}) } }
        composeRule.waitForIdle()

        // When each of the 124 tiles is scrolled into view and its name label is laid out
        val failures = Copy.Supplies.NAMES.mapNotNull { (id, name) ->
            val tag = benchArtTileNameTestTag(id)
            composeRule.onNodeWithTag(BenchArtSheetTestTag).performScrollToNode(hasTestTag(tag))
            val layout = layoutOf(tag)
            fitProblem(layout)?.let { "$id \"$name\": $it [${lines(layout).joinToString(" / ")}]" }
        }

        // Then none clips, none needs a third line, and every line ends between words
        assertTrue("Art names that do not fit the A16 tile:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun every_art_name_is_unique_so_no_two_tiles_or_spoken_names_read_alike() {
        // Given the 124 Art names; When they are compared ignoring case; Then no two are the same
        val names = Copy.Supplies.NAMES.values.map { it.lowercase() }
        val repeated = names.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        assertTrue("repeated Art names: $repeated", repeated.isEmpty())
    }

    private fun layoutOf(tag: String): TextLayoutResult {
        val node = composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
        return results.single()
    }

    private fun lines(layout: TextLayoutResult): List<String> {
        val text = layout.layoutInput.text.text
        return (0 until layout.lineCount).map { text.substring(layout.getLineStart(it), layout.getLineEnd(it)).trim() }
    }

    private fun fitProblem(layout: TextLayoutResult): String? {
        val text = layout.layoutInput.text.text
        if (layout.hasVisualOverflow) return "visual overflow"
        if (layout.didOverflowWidth) return "wider than the tile"
        if (layout.lineCount > 2) return "${layout.lineCount} lines"
        if (layout.getLineEnd(layout.lineCount - 1) != text.length) return "clipped after line ${layout.lineCount}"
        for (line in 0 until layout.lineCount - 1) {
            val end = layout.getLineEnd(line)
            // A break between words leaves whitespace (the forced '\n' or a wrapped space) on one side.
            if (!text[end - 1].isWhitespace() && !text[end].isWhitespace()) return "breaks inside a word at $end"
        }
        return null
    }
}
