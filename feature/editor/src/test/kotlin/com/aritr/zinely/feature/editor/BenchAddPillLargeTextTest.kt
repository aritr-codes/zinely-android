package com.aritr.zinely.feature.editor

import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasText
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** W1 task 6 / #146: at the largest text the "Add" pill grows with its label instead of clipping it. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp-xhdpi", fontScale = 2f)
class BenchAddPillLargeTextTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun given_the_bench_at_text_size_2_then_the_Add_label_lies_inside_the_pill() {
        composeRule.setContent {
            ZinelyTheme {
                BenchBottomBar(
                    canUndo = false, canRedo = false, doneEnabled = true,
                    onUndo = {}, onRedo = {}, onAdd = {}, onDone = {},
                )
            }
        }
        val pill = composeRule.onNodeWithTag(BenchBarAddTag).getBoundsInRoot()
        val label = composeRule.onNode(hasText("Add", substring = false) and hasAnyAncestor(hasTestTag(BenchBarAddTag)), useUnmergedTree = true)
            .getBoundsInRoot()
        // Inside the pill's ink border, not merely inside its outer bounds: a label that touches the ring is cut by it.
        val b = BenchChromeBorder
        assertTrue(
            "label $label must clear the ${b} ring of pill $pill",
            label.top >= pill.top + b && label.bottom <= pill.bottom - b &&
                label.left >= pill.left + b && label.right <= pill.right - b,
        )
    }
}
