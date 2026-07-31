package com.aritr.zinely.feature.library

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
 * Parity rasters of the frozen empty state and the dock, both themes — the artifact B4's review gate lays
 * beside `docs/design/mockups/v2-library.html` with its **Show empty state** toggle on.
 *
 * **What a green run here proves, and what it does not.** Without `-Proborazzi.test.record` or
 * `-Proborazzi.test.verify` nothing is written and nothing is compared, so a plain run only proves the
 * screen composes. **A recorded golden is not evidence until it has passed `verify`** — the principle B3
 * paid for, when a raster committed in B2 turned out to have been stale at HEAD. The measurable claims are
 * in [ZineShelfEmptyTest] and [ZineDockTest], which hold under a plain run.
 *
 * **The window is the frozen phone**, `392dp × 812dp` (`.phone`, `:41`), and the SDK is the module default
 * rather than [ZineShelfEmptyTest]'s API 28 — so this is the one place B4's grain is actually drawn. The
 * two illustrations are grained at soft-light and flat below API 29 (**D-014**), and only these rasters
 * show the first case.
 *
 * ### This is the whole `is-empty` screen, which no single composable is
 *
 * `body.is-empty` hides the shelf and shows `.empty`, while `.dock` stands in **both** states. So the
 * capture composes the desk, the empty state and the dock together — a picture of a screen B5 will
 * assemble, taken here because that is the arrangement the frozen file draws and the only one worth
 * comparing against it. B4 ships the parts; it does not ship the choice between them.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h812dp")
class ZineEmptyGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val TAG = "empty-viewport"

        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }

    @Test
    fun `the frozen empty state light`() = viewport("light", dark = false)

    @Test
    fun `the frozen empty state dark`() = viewport("dark", dark = true)

    private fun viewport(name: String, dark: Boolean) {
        composeRule.setContent { Desk(dark) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TAG).captureRoboImage("$GOLDEN_DIR/v2_empty_$name.png", aa())
    }

    @Composable
    private fun Desk(dark: Boolean) {
        ZinelyTheme(darkTheme = dark) {
            Box(
                Modifier
                    .testTag(TAG)
                    .fillMaxSize()
                    .background(ZinelyTheme.v2Colors.desk),
            ) {
                ZineShelfEmpty()
                ZineDock(onStart = {}, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}
