package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.golden.cropToBounds
import com.aritr.zinely.ui.golden.pixelCountOf
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A24/ADR-115: observe the selected-text row itself, not an unrelated editor surface. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class BenchTextGoldenTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun capture(dark: Boolean) {
        val verbs = benchContextVerbs(BenchVerbKind.TEXT)
        val expected = listOf(
            Copy.BenchVerbs.EDIT, Copy.BenchVerbs.SIZE, Copy.BenchVerbs.INK,
            Copy.BenchVerbs.DUPLICATE, Copy.BenchVerbs.DELETE,
        )
        assertEquals(expected, verbs.map { it.label })
        var ink = 0
        var danger = 0
        composeRule.setContent {
            ZinelyTheme(darkTheme = dark) {
                ink = ZinelyTheme.v21Colors.inkSoft.toArgb()
                danger = ZinelyTheme.v21Colors.jamText.toArgb()
                Box(Modifier.fillMaxWidth().background(ZinelyTheme.v21Colors.desk).testTag("textGoldenHost")) {
                    BenchContextBar(visible = true, verbs = verbs, onVerb = {}, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.FONT}").assertDoesNotExist()
        expected.forEach { composeRule.onNodeWithTag("$BenchContextBarTestTag-$it").assertIsEnabled() }
        val positions = expected.map {
            composeRule.onNodeWithTag("$BenchContextBarTestTag-$it").fetchSemanticsNode().boundsInRoot.left
        }
        assertTrue("Rendered controls retain a strict left-to-right order", positions.zipWithNext().all { (a, b) -> a < b })
        val raster = composeRule.activity.window.decorView.rasterizeToBitmap()
        expected.forEach { label ->
            val bounds = composeRule.onNodeWithTag("$BenchContextBarTestTag-$label")
                .fetchSemanticsNode().boundsInRoot
            val tint = if (label == Copy.BenchVerbs.DELETE) danger else ink
            assertTrue("$label paints its full-strength tint in the ${if (dark) "dark" else "light"} row",
                cropToBounds(raster, bounds).pixelCountOf(tint) > 50)
        }
        composeRule.onNodeWithTag("textGoldenHost").captureRoboImage(
            "src/test/roborazzi/bench_context_bar_text_${if (dark) "dark" else "light"}.png",
            roborazziOptions = RoborazziOptions(
                compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
            ),
        )
    }

    @Test fun selected_text_light() = capture(dark = false)
    @Test fun selected_text_dark() = capture(dark = true)
}
