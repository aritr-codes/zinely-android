package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import com.aritr.zinely.ui.golden.cropToBounds
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The named-undo snack (ADR-123, frozen `v21-bench.html` A26), in the states A26 stages: undo, redo, a page
 * clause, Across fold, and the longest line, light and dark, plus the longest at font scale 2 on a 360dp
 * phone. The pill is [BenchSnack] unchanged, so what these guard is that the new words sit in the frozen
 * geometry: no button, and at 2x the line wraps inside the pill instead of being clipped or shrunk.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h844dp-xhdpi")
class NamedUndoSnackGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val HOST_TAG = "namedUndoGoldenHost"
        const val LONGEST = "Top-bottom flip taken off, page 8"

        fun aa() = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f))
    }

    private fun capture(name: String, line: String, darkTheme: Boolean = false, fontScale: Float = 1f) {
        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                ZinelyTheme(darkTheme = darkTheme) {
                    // The production nesting: laid out in the paper island, painted in the room palette.
                    val roomColors = ZinelyTheme.v21Colors
                    Box(Modifier.testTag(HOST_TAG).fillMaxWidth().background(ZinelyTheme.colors.desk)) {
                        BenchSheetIsland {
                            BenchSnack(visible = true, message = line, actionLabel = null, onAction = {}, colors = roomColors)
                        }
                    }
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(BenchSnackMillis + 100L)
        composeRule.waitForIdle()

        // No button, and the whole line inside the pill: a clipped or hidden line is the 2x failure A26 rules out.
        composeRule.onNodeWithTag(BenchSnackActionTestTag).assertDoesNotExist()
        val pill = composeRule.onNodeWithTag(BenchSnackTestTag).fetchSemanticsNode().boundsInRoot
        val text = composeRule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(line)),
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        assertTrue("the line overflows the pill ($name): $text in $pill", pill.contains(text.topLeft) && pill.contains(text.bottomRight))

        val full = composeRule.activity.window.decorView.rasterizeToBitmap()
        cropToBounds(full, composeRule.onNodeWithTag(HOST_TAG).fetchSemanticsNode().boundsInRoot)
            .captureRoboImage("$GOLDEN_DIR/$name.png", aa())
    }

    @Test fun named_undo_snack_undo_light() = capture("named_undo_snack_undo_light", "Photo put back")

    @Test fun named_undo_snack_undo_dark() = capture("named_undo_snack_undo_dark", "Photo put back", darkTheme = true)

    @Test fun named_undo_snack_redo_light() = capture("named_undo_snack_redo_light", "Photo removed")

    @Test fun named_undo_snack_page_light() = capture("named_undo_snack_page_light", "Photo put back, page 3")

    @Test fun named_undo_snack_across_fold_light() = capture("named_undo_snack_across_fold_light", "Photo runs across pages 2 & 3")

    @Test fun named_undo_snack_longest_light() = capture("named_undo_snack_longest_light", LONGEST)

    @Test fun named_undo_snack_longest_dark() = capture("named_undo_snack_longest_dark", LONGEST, darkTheme = true)

    @Test
    @Config(qualifiers = "w360dp-h800dp-xhdpi")
    fun named_undo_snack_longest_text2x_light() = capture("named_undo_snack_longest_text2x_light", LONGEST, fontScale = 2f)
}
