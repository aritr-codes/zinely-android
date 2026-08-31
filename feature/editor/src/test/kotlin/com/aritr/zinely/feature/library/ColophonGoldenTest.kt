package com.aritr.zinely.feature.library

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.ui.theme.LocalZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Visual baseline for Colophon, in both themes and maximum supported font scale.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h812dp")
class ColophonGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the frozen colophon light`() = viewport("light", dark = false)

    @Test
    fun `the frozen colophon dark`() = viewport("dark", dark = true)

    @Test
    fun `the frozen colophon max-font-scale light`() = viewport("max_font_scale_light", dark = false, fontScale = 1.8f)

    @Test
    fun `the frozen colophon max-font-scale dark`() = viewport("max_font_scale_dark", dark = true, fontScale = 1.8f)

    private fun viewport(name: String, dark: Boolean, fontScale: Float = 1f) {
        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(base.density, fontScale),
                LocalZinelyMotion provides ZinelyMotion(reduceMotion = true),
            ) {
                ZinelyTheme(darkTheme = dark) {
                    ColophonScreen(
                        preferredPaper = PaperSize.A4,
                        appVersion = "v0.0.0",
                        onPreferredPaperChange = {},
                        onBackToShelf = {},
                        modifier = Modifier.fillMaxSize().testTag(ColophonScreenTestTag),
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ColophonScreenTestTag)
            .captureRoboImage("$GOLDEN_DIR/colophon_$name.png", aa())
    }

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"

        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }
}
